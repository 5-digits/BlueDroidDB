#include "sqlite.h"
#include <jni.h>
JNIEXPORT int Java_blue_stack_sqlite_SQLiteCursor_columnType(JNIEnv *env, jobject object, int statementHandle, int columnIndex) {
	sqlite3_stmt *handle = (sqlite3_stmt *)statementHandle;
	return sqlite3_column_type(handle, columnIndex);
}

JNIEXPORT int Java_blue_stack_sqlite_SQLiteCursor_columnIsNull(JNIEnv *env, jobject object, int statementHandle, int columnIndex) {
	sqlite3_stmt *handle = (sqlite3_stmt *)statementHandle;
	int valType = sqlite3_column_type(handle, columnIndex);
	return SQLITE_NULL == valType;
}

JNIEXPORT int Java_blue_stack_sqlite_SQLiteCursor_columnIntValue(JNIEnv *env, jobject object, int statementHandle, int columnIndex) {
	sqlite3_stmt *handle = (sqlite3_stmt *)statementHandle;
	int valType = sqlite3_column_type(handle, columnIndex);
	if (SQLITE_NULL == valType) {
		return 0;
	}
	return sqlite3_column_int(handle, columnIndex);
}

JNIEXPORT long long Java_blue_stack_sqlite_SQLiteCursor_columnLongValue(JNIEnv *env, jobject object, int statementHandle, int columnIndex) {
	sqlite3_stmt *handle = (sqlite3_stmt *)statementHandle;
	int valType = sqlite3_column_type(handle, columnIndex);
	if (SQLITE_NULL == valType) {
		return 0;
	}
	return sqlite3_column_int64(handle, columnIndex);
}

JNIEXPORT double Java_blue_stack_sqlite_SQLiteCursor_columnDoubleValue(JNIEnv *env, jobject object, int statementHandle, int columnIndex) {
	sqlite3_stmt *handle = (sqlite3_stmt *)statementHandle;
	int valType = sqlite3_column_type(handle, columnIndex);
	if (SQLITE_NULL == valType) {
		return 0;
	}
	return sqlite3_column_double(handle, columnIndex);
}

JNIEXPORT jstring Java_blue_stack_sqlite_SQLiteCursor_columnStringValue(JNIEnv *env, jobject object, int statementHandle, int columnIndex) {
	sqlite3_stmt *handle = (sqlite3_stmt *)statementHandle;
	const char *str = sqlite3_column_text(handle, columnIndex);
	if (str != 0) {
		return (*env)->NewStringUTF(env, str);
	}
	return 0;
}

JNIEXPORT jbyteArray Java_blue_stack_sqlite_SQLiteCursor_columnByteArrayValue(JNIEnv *env, jobject object, int statementHandle, int columnIndex) {
    sqlite3_stmt *handle = (sqlite3_stmt *)statementHandle;
	void *buf = sqlite3_column_blob(handle, columnIndex);
	int length = sqlite3_column_bytes(handle, columnIndex);
	if (buf != 0 && length > 0) {
		jbyteArray result = (*env)->NewByteArray(env, length);
        (*env)->SetByteArrayRegion(env, result, 0, length, buf);
        return result;
	}
	return 0;
}

JNIEXPORT int Java_blue_stack_sqlite_SQLiteCursor_columnByteArrayLength(JNIEnv *env, jobject object, int statementHandle, int columnIndex) {
	return sqlite3_column_bytes((sqlite3_stmt *)statementHandle, columnIndex);
}

JNIEXPORT int Java_blue_stack_sqlite_SQLiteCursor_columnByteBufferValue(JNIEnv *env, jobject object, int statementHandle, int columnIndex, jobject buffer) {
    if (!buffer) {
        return 0;
    }
	sqlite3_stmt *handle = (sqlite3_stmt *)statementHandle;
	void *buf = sqlite3_column_blob(handle, columnIndex);
	int length = sqlite3_column_bytes(handle, columnIndex);
	if (buf != 0 && length > 0) {
        jbyte *byteBuff = (*env)->GetDirectBufferAddress(env, buffer);
        memcpy(byteBuff, buf, length);
        return length;
	}
	return 0;
}
