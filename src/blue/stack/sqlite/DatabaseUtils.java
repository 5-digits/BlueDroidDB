/**
 * 
 */
package blue.stack.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteProgram;

/**
 * @author BunnyBlue
 *
 */
public class DatabaseUtils {

	/**
	 * Binds the given Object to the given SQLiteProgram using the proper
	 * typing. For example, bind numbers as longs/doubles, and everything else
	 * as a string by call toString() on it.
	 *
	 * @param prog
	 *            the program to bind the object to
	 * @param index
	 *            the 1-based index to bind at
	 * @param value
	 *            the value to bind
	 */
	public static void bindObjectToProgram(SQLiteProgram prog, int index,
			Object value) {
		if (value == null) {
			prog.bindNull(index);
		} else if (value instanceof Double || value instanceof Float) {
			prog.bindDouble(index, ((Number) value).doubleValue());
		} else if (value instanceof Number) {
			prog.bindLong(index, ((Number) value).longValue());
		} else if (value instanceof Boolean) {
			Boolean bool = (Boolean) value;
			if (bool) {
				prog.bindLong(index, 1);
			} else {
				prog.bindLong(index, 0);
			}
		} else if (value instanceof byte[]) {
			prog.bindBlob(index, (byte[]) value);
		} else {
			prog.bindString(index, value.toString());
		}
	}

	/**
	 * Returns data type of the given object's value.
	 * <p>
	 * Returned values are
	 * <ul>
	 * <li>{@link Cursor#FIELD_TYPE_NULL}</li>
	 * <li>{@link Cursor#FIELD_TYPE_INTEGER}</li>
	 * <li>{@link Cursor#FIELD_TYPE_FLOAT}</li>
	 * <li>{@link Cursor#FIELD_TYPE_STRING}</li>
	 * <li>{@link Cursor#FIELD_TYPE_BLOB}</li>
	 * </ul>
	 * </p>
	 *
	 * @param obj
	 *            the object whose value type is to be returned
	 * @return object value type
	 * @hide
	 */
	public static int getTypeOfObject(Object obj) {
		if (obj == null) {
			return SQLiteCursor.FIELD_TYPE_NULL;
		} else if (obj instanceof byte[]) {
			return SQLiteCursor.FIELD_TYPE_BYTEARRAY;
		} else if (obj instanceof Float || obj instanceof Double) {
			return SQLiteCursor.FIELD_TYPE_FLOAT;
		} else if (obj instanceof Long || obj instanceof Integer
				|| obj instanceof Short || obj instanceof Byte) {
			return SQLiteCursor.FIELD_TYPE_INT;
		} else {
			return SQLiteCursor.FIELD_TYPE_STRING;
		}
	}

}
