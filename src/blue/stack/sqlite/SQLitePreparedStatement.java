package blue.stack.sqlite;

import java.nio.ByteBuffer;

import android.database.sqlite.SQLiteBindOrColumnIndexOutOfRangeException;

public class SQLitePreparedStatement {
	private boolean isFinalized = false;
	private int sqliteStatementHandle;

	private int mNumParameters;
	private boolean finalizeAfterQuery = false;
	Object[] bindArgs = null;

	public int getStatementHandle() {
		return sqliteStatementHandle;
	}

	public SQLitePreparedStatement(SQLiteDatabase db, String sql, boolean finalize) throws SQLiteException {
		finalizeAfterQuery = finalize;
		sqliteStatementHandle = prepare(db.getSQLiteHandle(), sql);
	}

	/**
	 * @param db
	 * @param string
	 * @param bindArgs
	 * @throws SQLiteException
	 */
	public SQLitePreparedStatement(SQLiteDatabase db, String sql, Object[] bindArgs) throws SQLiteException {
		sqliteStatementHandle = prepare(db.getSQLiteHandle(), sql);
		finalizeAfterQuery = true;
		this.bindArgs = bindArgs;

	}

	protected void bindArguments(Object[] bindArgs) throws SQLiteException {
		final int count = bindArgs != null ? bindArgs.length : 0;
		if (count != mNumParameters) {
			throw new SQLiteBindOrColumnIndexOutOfRangeException(
					"Expected " + mNumParameters + " bind arguments but "
							+ count + " were provided.");
		}
		if (count == 0) {
			return;
		}

		// final int statementPtr = statement.mStatementPtr;
		for (int i = 0; i < count; i++) {
			final Object arg = bindArgs[i];
			switch (DatabaseUtils.getTypeOfObject(arg)) {
			case SQLiteCursor.FIELD_TYPE_NULL:

				bindNull(sqliteStatementHandle, i + 1);
				break;
			case SQLiteCursor.FIELD_TYPE_INT:
				bindLong(sqliteStatementHandle, i + 1, ((Number) arg).longValue());

				break;
			case SQLiteCursor.FIELD_TYPE_FLOAT:
				bindDouble(sqliteStatementHandle, i + 1, ((Number) arg).doubleValue());

				break;
			case SQLiteCursor.FIELD_TYPE_BYTEARRAY:
				ByteBuffer buffer = ByteBuffer.wrap((byte[]) arg);
				bindByteBuffer(sqliteStatementHandle, i + 1, buffer, buffer.limit());
				// nativeBindBlob(mConnectionPtr, statementPtr, i + 1, (byte[])
				// arg);
				break;
			case SQLiteCursor.FIELD_TYPE_STRING:
			default:
				if (arg instanceof Boolean) {
					// Provide compatibility with legacy applications which may
					// pass
					// Boolean values in bind args.
					bindLong(sqliteStatementHandle, i + 1, ((Boolean) arg).booleanValue() ? 1 : 0);

				} else {
					bindString(sqliteStatementHandle, i + 1, arg.toString());
					// nativeBindString(mConnectionPtr, statementPtr, i + 1,
					// arg.toString());
				}
				break;
			}
		}
	}

	public SQLiteCursor query(Object[] args) throws SQLiteException {
		if (args == null || args.length != mNumParameters) {
			throw new IllegalArgumentException();
		}

		checkFinalized();

		reset(sqliteStatementHandle);

		int i = 1;
		for (Object obj : args) {
			if (obj == null) {
				bindNull(sqliteStatementHandle, i);
			} else if (obj instanceof Integer) {
				bindInt(sqliteStatementHandle, i, (Integer) obj);
			} else if (obj instanceof Double) {
				bindDouble(sqliteStatementHandle, i, (Double) obj);
			} else if (obj instanceof String) {
				bindString(sqliteStatementHandle, i, (String) obj);
			} else {
				throw new IllegalArgumentException();
			}
			i++;
		}

		return new SQLiteCursor(this);
	}

	public int step() throws SQLiteException {
		return step(sqliteStatementHandle);
	}

	public SQLitePreparedStatement stepThis() throws SQLiteException {
		step(sqliteStatementHandle);
		return this;
	}

	public void requery() throws SQLiteException {
		checkFinalized();
		reset(sqliteStatementHandle);
	}

	public void dispose() {
		if (finalizeAfterQuery) {
			finalizeQuery();
		}
	}

	void checkFinalized() throws SQLiteException {
		if (isFinalized) {
			throw new SQLiteException("Prepared query finalized");
		}
	}

	public void finalizeQuery() {
		if (isFinalized) {
			return;
		}
		try {
			isFinalized = true;
			finalize(sqliteStatementHandle);
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
	}

	public void bindInteger(int index, int value) throws SQLiteException {
		bindInt(sqliteStatementHandle, index, value);
	}

	public void bindDouble(int index, double value) throws SQLiteException {
		bindDouble(sqliteStatementHandle, index, value);
	}

	public void bindByteBuffer(int index, ByteBuffer value) throws SQLiteException {
		bindByteBuffer(sqliteStatementHandle, index, value, value.limit());
	}

	public void bindString(int index, String value) throws SQLiteException {
		bindString(sqliteStatementHandle, index, value);
	}

	public void bindLong(int index, long value) throws SQLiteException {
		bindLong(sqliteStatementHandle, index, value);
	}

	native void bindByteBuffer(int statementHandle, int index, ByteBuffer value, int length) throws SQLiteException;

	native void bindString(int statementHandle, int index, String value) throws SQLiteException;

	native void bindInt(int statementHandle, int index, int value) throws SQLiteException;

	native void bindLong(int statementHandle, int index, long value) throws SQLiteException;

	native void bindDouble(int statementHandle, int index, double value) throws SQLiteException;

	native void bindNull(int statementHandle, int index) throws SQLiteException;

	native void reset(int statementHandle) throws SQLiteException;

	native int prepare(int sqliteHandle, String sql) throws SQLiteException;

	@Deprecated
	native int nativeGetParameterCount(int sqliteHandle) throws SQLiteException;

	native void finalize(int statementHandle) throws SQLiteException;

	native int step(int statementHandle) throws SQLiteException;
}
