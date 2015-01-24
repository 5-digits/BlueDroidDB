package blue.stack.sqlite;

import android.content.ContentValues;

public class SQLiteDatabase {
	private static final String TAG = "SQLiteDatabase";

	/**
	 * When a constraint violation occurs, an immediate ROLLBACK occurs, thus
	 * ending the current transaction, and the command aborts with a return code
	 * of SQLITE_CONSTRAINT. If no transaction is active (other than the implied
	 * transaction that is created on every command) then this algorithm works
	 * the same as ABORT.
	 */
	public static final int CONFLICT_ROLLBACK = 1;

	/**
	 * When a constraint violation occurs,no ROLLBACK is executed so changes
	 * from prior commands within the same transaction are preserved. This is
	 * the default behavior.
	 */
	public static final int CONFLICT_ABORT = 2;

	/**
	 * When a constraint violation occurs, the command aborts with a return code
	 * SQLITE_CONSTRAINT. But any changes to the database that the command made
	 * prior to encountering the constraint violation are preserved and are not
	 * backed out.
	 */
	public static final int CONFLICT_FAIL = 3;

	/**
	 * When a constraint violation occurs, the one row that contains the
	 * constraint violation is not inserted or changed. But the command
	 * continues executing normally. Other rows before and after the row that
	 * contained the constraint violation continue to be inserted or updated
	 * normally. No error is returned.
	 */
	public static final int CONFLICT_IGNORE = 4;

	/**
	 * When a UNIQUE constraint violation occurs, the pre-existing rows that are
	 * causing the constraint violation are removed prior to inserting or
	 * updating the current row. Thus the insert or update always occurs. The
	 * command continues executing normally. No error is returned. If a NOT NULL
	 * constraint violation occurs, the NULL value is replaced by the default
	 * value for that column. If the column has no default value, then the ABORT
	 * algorithm is used. If a CHECK constraint violation occurs then the IGNORE
	 * algorithm is used. When this conflict resolution strategy deletes rows in
	 * order to satisfy a constraint, it does not invoke delete triggers on
	 * those rows. This behavior might change in a future release.
	 */
	public static final int CONFLICT_REPLACE = 5;

	/**
	 * Use the following when no conflict action is specified.
	 */
	public static final int CONFLICT_NONE = 0;

	private static final String[] CONFLICT_VALUES = new String[]
	{ "", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE " };

	private final int sqliteHandle;

	private boolean isOpen = false;
	private boolean inTransaction = false;

	public int getSQLiteHandle() {
		return sqliteHandle;
	}

	public SQLiteDatabase(String fileName, String path) throws SQLiteException {

		sqliteHandle = opendb(fileName, path);
		isOpen = true;
	}

	/**
	 * @return the isOpen
	 */
	public boolean isOpen() {
		return isOpen;
	}

	public boolean tableExists(String tableName) throws SQLiteException {
		checkOpened();
		String s = "SELECT rowid FROM sqlite_master WHERE type='table' AND name=?;";
		return executeInt(s, tableName) != null;
	}

	public SQLitePreparedStatement executeFast(String sql) throws SQLiteException {
		return new SQLitePreparedStatement(this, sql, true);
	}

	public Integer executeInt(String sql, Object... args) throws SQLiteException {
		checkOpened();
		SQLiteCursor cursor = queryFinalized(sql, args);
		try {
			if (!cursor.next()) {
				return null;
			}
			return cursor.intValue(0);
		} finally {
			cursor.dispose();
		}
	}

	public SQLiteCursor queryFinalized(String sql, Object... args) throws SQLiteException {
		checkOpened();
		return new SQLitePreparedStatement(this, sql, true).query(args);
	}

	public void close() {
		if (isOpen) {
			try {
				endTransaction();
				closedb(sqliteHandle);
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			isOpen = false;
		}
	}

	void checkOpened() throws SQLiteException {
		if (!isOpen) {
			throw new SQLiteException("Database closed");
		}
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
		close();
	}

	private StackTraceElement[] temp;

	public void beginTransaction() throws SQLiteException {
		if (inTransaction) {
			throw new SQLiteException("database already in transaction");
		}
		inTransaction = true;
		beginTransaction(sqliteHandle);
	}

	public void endTransaction() {
		if (!inTransaction) {
			return;
		}
		inTransaction = false;
		commitTransaction(sqliteHandle);
	}

	/**
	 * Convenience method for inserting a row into the database.
	 *
	 * @param table
	 *            the table to insert the row into
	 * @param nullColumnHack
	 *            optional; may be <code>null</code>. SQL doesn't allow
	 *            inserting a completely empty row without naming at least one
	 *            column name. If your provided <code>values</code> is empty, no
	 *            column names are known and an empty row can't be inserted. If
	 *            not set to null, the <code>nullColumnHack</code> parameter
	 *            provides the name of nullable column name to explicitly insert
	 *            a NULL into in the case where your <code>values</code> is
	 *            empty.
	 * @param values
	 *            this map contains the initial column values for the row. The
	 *            keys should be the column names and the values the column
	 *            values
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public long insert(String table, String nullColumnHack, ContentValues values) {
		try {
			return insertWithOnConflict(table, nullColumnHack, values, 0);
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sqliteHandle;
	}

	/**
	 * General method for inserting a row into the database.
	 *
	 * @param table
	 *            the table to insert the row into
	 * @param nullColumnHack
	 *            optional; may be <code>null</code>. SQL doesn't allow
	 *            inserting a completely empty row without naming at least one
	 *            column name. If your provided <code>initialValues</code> is
	 *            empty, no column names are known and an empty row can't be
	 *            inserted. If not set to null, the <code>nullColumnHack</code>
	 *            parameter provides the name of nullable column name to
	 *            explicitly insert a NULL into in the case where your
	 *            <code>initialValues</code> is empty.
	 * @param initialValues
	 *            this map contains the initial column values for the row. The
	 *            keys should be the column names and the values the column
	 *            values
	 * @param conflictAlgorithm
	 *            for insert conflict resolver
	 * @return the row ID of the newly inserted row OR the primary key of the
	 *         existing row if the input param 'conflictAlgorithm' =
	 *         {@link #CONFLICT_IGNORE} OR -1 if any error
	 * @throws SQLiteException
	 */
	public long insertWithOnConflict(String table, String nullColumnHack,
			ContentValues initialValues, int conflictAlgorithm) throws SQLiteException {
		// acquireReference();
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("INSERT");
			sql.append(CONFLICT_VALUES[conflictAlgorithm]);
			sql.append(" INTO ");
			sql.append(table);
			sql.append('(');

			Object[] bindArgs = null;
			int size = (initialValues != null && initialValues.size() > 0)
					? initialValues.size() : 0;
			if (size > 0) {
				bindArgs = new Object[size];
				int i = 0;
				for (String colName : initialValues.keySet()) {
					sql.append((i > 0) ? "," : "");
					sql.append(colName);
					bindArgs[i++] = initialValues.get(colName);
				}
				sql.append(')');
				sql.append(" VALUES (");
				for (i = 0; i < size; i++) {
					sql.append((i > 0) ? ",?" : "?");
				}
			} else {
				sql.append(nullColumnHack + ") VALUES (NULL");
			}
			sql.append(')');
			SQLitePreparedStatement sqLitePreparedStatement = new SQLitePreparedStatement(this, sql.toString(), true);
			sqLitePreparedStatement.bindArguments(bindArgs);
			sqLitePreparedStatement.stepThis().dispose();
			// SQLitePreparedStatement statement = new
			// SQLitePreparedStatement(this, sql.toString(), bindArgs);
			// try {
			// return statement.executeInsert();
			// } finally {
			// statement.close();
			// }
		} finally {
			// releaseReference();
		}
		return conflictAlgorithm;
	}

	native int opendb(String fileName, String tempDir) throws SQLiteException;

	native void closedb(int sqliteHandle) throws SQLiteException;

	/**********/
	public native boolean keyDB(int sqliteHandle, String key);

	public native void reKeyDB(int sqliteHandle, String oldKey, String newKey);

	native void beginTransaction(int sqliteHandle);

	native void commitTransaction(int sqliteHandle);
}
