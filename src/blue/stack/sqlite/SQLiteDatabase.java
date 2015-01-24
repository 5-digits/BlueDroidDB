package blue.stack.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.text.TextUtils;
import blue.stack.bluedroiddb.cvTest.BuildTimeCounter;

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
	 *
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
	 * @throws SQLiteException
	 */
	@Deprecated
	public long insert(String table, String nullColumnHack, ContentValues values) throws SQLiteException {

		return insertWithOnConflict(table, nullColumnHack, values, 0);

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
		// try {
		BuildTimeCounter.start();
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
		BuildTimeCounter.end();

		SQLitePreparedStatement sqLitePreparedStatement = new SQLitePreparedStatement(this, sql.toString(), bindArgs);

		sqLitePreparedStatement.bindArguments(bindArgs);

		sqLitePreparedStatement.exeInsertWithDispose();
		// TimeCounter.end();

		// }
		// } finally {
		// // releaseReference();
		// }
		return conflictAlgorithm;
	}

	/**
	 * Convenience method for updating rows in the database.
	 *
	 * @param table
	 *            the table to update in
	 * @param values
	 *            a map from column names to new column values. null is a valid
	 *            value that will be translated to NULL.
	 * @param whereClause
	 *            the optional WHERE clause to apply when updating. Passing null
	 *            will update all rows.
	 * @param whereArgs
	 *            You may include ?s in the where clause, which will be replaced
	 *            by the values from whereArgs. The values will be bound as
	 *            Strings.
	 * @return the number of rows affected
	 */
	public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
		return updateWithOnConflict(table, values, whereClause, whereArgs, CONFLICT_NONE);
	}

	/**
	 * Convenience method for updating rows in the database.
	 *
	 * @param table
	 *            the table to update in
	 * @param values
	 *            a map from column names to new column values. null is a valid
	 *            value that will be translated to NULL.
	 * @param whereClause
	 *            the optional WHERE clause to apply when updating. Passing null
	 *            will update all rows.
	 * @param whereArgs
	 *            You may include ?s in the where clause, which will be replaced
	 *            by the values from whereArgs. The values will be bound as
	 *            Strings.
	 * @param conflictAlgorithm
	 *            for update conflict resolver
	 * @return the number of rows affected
	 */
	public int updateWithOnConflict(String table, ContentValues values,
			String whereClause, String[] whereArgs, int conflictAlgorithm) {
		if (values == null || values.size() == 0) {
			throw new IllegalArgumentException("Empty values");
		}

		try {
			StringBuilder sql = new StringBuilder(120);
			sql.append("UPDATE ");
			sql.append(CONFLICT_VALUES[conflictAlgorithm]);
			sql.append(table);
			sql.append(" SET ");

			// move all bind args to one array
			int setValuesSize = values.size();
			int bindArgsSize = (whereArgs == null) ? setValuesSize : (setValuesSize + whereArgs.length);
			Object[] bindArgs = new Object[bindArgsSize];
			int i = 0;
			for (String colName : values.keySet()) {
				sql.append((i > 0) ? "," : "");
				sql.append(colName);
				bindArgs[i++] = values.get(colName);
				sql.append("=?");
			}
			if (whereArgs != null) {
				for (i = setValuesSize; i < bindArgsSize; i++) {
					bindArgs[i] = whereArgs[i - setValuesSize];
				}
			}
			if (!TextUtils.isEmpty(whereClause)) {
				sql.append(" WHERE ");
				sql.append(whereClause);
			}
			SQLitePreparedStatement sqLitePreparedStatement;
			try {
				sqLitePreparedStatement = new SQLitePreparedStatement(this, sql.toString(),
						bindArgs);
				sqLitePreparedStatement.bindArguments(bindArgs);
				return sqLitePreparedStatement.executeUpdateWithDispose();
				// return //return 1;
			} catch (SQLiteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -1;
			}

		} finally {

		}
	}

	/**
	 * Convenience method for deleting rows in the database.
	 *
	 * @param table
	 *            the table to delete from
	 * @param whereClause
	 *            the optional WHERE clause to apply when deleting. Passing null
	 *            will delete all rows.
	 * @param whereArgs
	 *            You may include ?s in the where clause, which will be replaced
	 *            by the values from whereArgs. The values will be bound as
	 *            Strings.
	 * @return the number of rows affected if a whereClause is passed in, 0
	 *         otherwise. To remove all rows and get a count pass "1" as the
	 *         whereClause.
	 * @throws SQLiteException
	 */
	public int delete(String table, String whereClause, String[] whereArgs) throws SQLiteException {
		SQLitePreparedStatement sqLitePreparedStatement = new SQLitePreparedStatement(this, "DELETE FROM " + table +
				(!TextUtils.isEmpty(whereClause) ? " WHERE " + whereClause : ""), whereArgs);
		sqLitePreparedStatement.bindArguments(whereArgs);
		return sqLitePreparedStatement.executeUpdateWithDispose();

	}

	public SQLiteCursor query(String table, String[] columns, String selection,
			String[] selectionArgs) {

		try {
			return query(false, table, columns, selection, selectionArgs, null,
					null, null, null /* limit */);
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Query the given table, returning a {@link Cursor} over the result set.
	 *
	 * @param table
	 *            The table name to compile the query against.
	 * @param columns
	 *            A list of which columns to return. Passing null will return
	 *            all columns, which is discouraged to prevent reading data from
	 *            storage that isn't going to be used.
	 * @param selection
	 *            A filter declaring which rows to return, formatted as an SQL
	 *            WHERE clause (excluding the WHERE itself). Passing null will
	 *            return all rows for the given table.
	 * @param selectionArgs
	 *            You may include ?s in selection, which will be replaced by the
	 *            values from selectionArgs, in order that they appear in the
	 *            selection. The values will be bound as Strings.
	 * @param groupBy
	 *            A filter declaring how to group rows, formatted as an SQL
	 *            GROUP BY clause (excluding the GROUP BY itself). Passing null
	 *            will cause the rows to not be grouped.
	 * @param having
	 *            A filter declare which row groups to include in the cursor, if
	 *            row grouping is being used, formatted as an SQL HAVING clause
	 *            (excluding the HAVING itself). Passing null will cause all row
	 *            groups to be included, and is required when row grouping is
	 *            not being used.
	 * @param orderBy
	 *            How to order the rows, formatted as an SQL ORDER BY clause
	 *            (excluding the ORDER BY itself). Passing null will use the
	 *            default sort order, which may be unordered.
	 * @return A {@link Cursor} object, which is positioned before the first
	 *         entry. Note that {@link Cursor}s are not synchronized, see the
	 *         documentation for more details.
	 * @see Cursor
	 */
	public SQLiteCursor query(String table, String[] columns, String selection,
			String[] selectionArgs, String groupBy, String having,
			String orderBy) {

		try {
			return query(false, table, columns, selection, selectionArgs, groupBy,
					having, orderBy, null /* limit */);
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Query the given URL, returning a {@link Cursor} over the result set.
	 *
	 * @param distinct
	 *            true if you want each row to be unique, false otherwise.
	 * @param table
	 *            The table name to compile the query against.
	 * @param columns
	 *            A list of which columns to return. Passing null will return
	 *            all columns, which is discouraged to prevent reading data from
	 *            storage that isn't going to be used.
	 * @param selection
	 *            A filter declaring which rows to return, formatted as an SQL
	 *            WHERE clause (excluding the WHERE itself). Passing null will
	 *            return all rows for the given table.
	 * @param selectionArgs
	 *            You may include ?s in selection, which will be replaced by the
	 *            values from selectionArgs, in order that they appear in the
	 *            selection. The values will be bound as Strings.
	 * @param groupBy
	 *            A filter declaring how to group rows, formatted as an SQL
	 *            GROUP BY clause (excluding the GROUP BY itself). Passing null
	 *            will cause the rows to not be grouped.
	 * @param having
	 *            A filter declare which row groups to include in the cursor, if
	 *            row grouping is being used, formatted as an SQL HAVING clause
	 *            (excluding the HAVING itself). Passing null will cause all row
	 *            groups to be included, and is required when row grouping is
	 *            not being used.
	 * @param orderBy
	 *            How to order the rows, formatted as an SQL ORDER BY clause
	 *            (excluding the ORDER BY itself). Passing null will use the
	 *            default sort order, which may be unordered.
	 * @param limit
	 *            Limits the number of rows returned by the query, formatted as
	 *            LIMIT clause. Passing null denotes no LIMIT clause.
	 * @return A {@link Cursor} object, which is positioned before the first
	 *         entry. Note that {@link Cursor}s are not synchronized, see the
	 *         documentation for more details.
	 * @throws SQLiteException
	 * @see Cursor
	 */
	public SQLiteCursor query(boolean distinct, String table, String[] columns,
			String selection, String[] selectionArgs, String groupBy,
			String having, String orderBy, String limit) throws SQLiteException {
		return queryWithFactory(null, distinct, table, columns, selection, selectionArgs,
				groupBy, having, orderBy, limit, null);
	}

	/**
	 * Query the given URL, returning a {@link Cursor} over the result set.
	 *
	 * @param cursorFactory
	 *            the cursor factory to use, or null for the default factory
	 * @param distinct
	 *            true if you want each row to be unique, false otherwise.
	 * @param table
	 *            The table name to compile the query against.
	 * @param columns
	 *            A list of which columns to return. Passing null will return
	 *            all columns, which is discouraged to prevent reading data from
	 *            storage that isn't going to be used.
	 * @param selection
	 *            A filter declaring which rows to return, formatted as an SQL
	 *            WHERE clause (excluding the WHERE itself). Passing null will
	 *            return all rows for the given table.
	 * @param selectionArgs
	 *            You may include ?s in selection, which will be replaced by the
	 *            values from selectionArgs, in order that they appear in the
	 *            selection. The values will be bound as Strings.
	 * @param groupBy
	 *            A filter declaring how to group rows, formatted as an SQL
	 *            GROUP BY clause (excluding the GROUP BY itself). Passing null
	 *            will cause the rows to not be grouped.
	 * @param having
	 *            A filter declare which row groups to include in the cursor, if
	 *            row grouping is being used, formatted as an SQL HAVING clause
	 *            (excluding the HAVING itself). Passing null will cause all row
	 *            groups to be included, and is required when row grouping is
	 *            not being used.
	 * @param orderBy
	 *            How to order the rows, formatted as an SQL ORDER BY clause
	 *            (excluding the ORDER BY itself). Passing null will use the
	 *            default sort order, which may be unordered.
	 * @param limit
	 *            Limits the number of rows returned by the query, formatted as
	 *            LIMIT clause. Passing null denotes no LIMIT clause.
	 * @param cancellationSignal
	 *            A signal to cancel the operation in progress, or null if none.
	 *            If the operation is canceled, then
	 *            {@link OperationCanceledException} will be thrown when the
	 *            query is executed.
	 * @return A {@link Cursor} object, which is positioned before the first
	 *         entry. Note that {@link Cursor}s are not synchronized, see the
	 *         documentation for more details.
	 * @throws SQLiteException
	 * @see Cursor
	 */
	public SQLiteCursor queryWithFactory(CursorFactory cursorFactory,
			boolean distinct, String table, String[] columns,
			String selection, String[] selectionArgs, String groupBy,
			String having, String orderBy, String limit, CancellationSignal cancellationSignal) throws SQLiteException {

		try {
			String sql = SQLiteQueryBuilder.buildQueryString(
					distinct, table, columns, selection, groupBy, having, orderBy, limit);
			// SQLitePreparedStatement sqLitePreparedStatement = new
			// SQLitePreparedStatement(this, sql, selectionArgs);

			return queryFinalized(sql, selectionArgs);

		} finally {

		}
	}

	/************** native function implementation ************/
	native int opendb(String fileName, String tempDir) throws SQLiteException;

	native void closedb(int sqliteHandle) throws SQLiteException;

	/**********/
	public native boolean keyDB(int sqliteHandle, String key);

	public native void reKeyDB(int sqliteHandle, String oldKey, String newKey);

	native void beginTransaction(int sqliteHandle);

	native void commitTransaction(int sqliteHandle);
}
