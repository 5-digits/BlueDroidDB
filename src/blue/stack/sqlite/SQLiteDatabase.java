package blue.stack.sqlite;

public class SQLiteDatabase {
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
				commitTransaction();
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

	public void commitTransaction() {
		if (!inTransaction) {
			return;
		}
		inTransaction = false;
		commitTransaction(sqliteHandle);
	}

	native int opendb(String fileName, String tempDir) throws SQLiteException;

	native void closedb(int sqliteHandle) throws SQLiteException;

	native void beginTransaction(int sqliteHandle);

	native void commitTransaction(int sqliteHandle);
}
