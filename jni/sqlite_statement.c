#include "sqlite.h"
#include <jni.h>
jfieldID queryArgsCountField;
int  argsNum=0;
jint sqliteOnJNILoad(JavaVM *vm, void *reserved, JNIEnv *env) {
	jclass class = (*env)->FindClass(env, "blue/stack/sqlite/SQLitePreparedStatement");
	queryArgsCountField = (*env)->GetFieldID(env, class, "mNumParameters", "I");
	return JNI_VERSION_1_4;
}

JNIEXPORT int Java_blue_stack_sqlite_SQLitePreparedStatement_step(JNIEnv* env, jobject object, int statementHandle) {
	sqlite3_stmt *handle = (sqlite3_stmt *)statementHandle;

	int errcode = sqlite3_step(handle);
	if (errcode == SQLITE_ROW)  {
		return 0;
	} else if(errcode == SQLITE_DONE) {
		return 1;
	}  else if(errcode == SQLITE_BUSY) {
		return -1;
	}
	throw_sqlite3_exception(env, sqlite3_db_handle(handle), errcode);
}

JNIEXPORT int Java_blue_stack_sqlite_SQLitePreparedStatement_prepare(JNIEnv *env, jobject object, int sqliteHandle, jstring sql) {
	sqlite3* handle = (sqlite3 *)sqliteHandle;

	char const *sqlStr = (*env)->GetStringUTFChars(env, sql, 0);

	sqlite3_stmt *stmt_handle;

	int errcode = sqlite3_prepare_v2(handle, sqlStr, -1, &stmt_handle, 0);
	if (SQLITE_OK != errcode) {
		throw_sqlite3_exception(env, handle, errcode);
	} else {
		int argsCount = sqlite3_bind_parameter_count(stmt_handle);
		(*env)->SetIntField(env, object, queryArgsCountField, argsCount);
		argsNum=argsCount;
	}

	if (sqlStr != 0) {
		(*env)->ReleaseStringUTFChars(env, sql, sqlStr);
	}

	return (int)stmt_handle;
}


static inline int executeNonQuery(JNIEnv* env, sqlite3* db, sqlite3_stmt* statement) {
	int err = sqlite3_step(statement);
	if (err == SQLITE_ROW) {
		throw_sqlite3_exception(env, sqlite3_db_handle(statement), err);
//		throw_sqlite3_exception(env,
//				"Queries can be performed using SQLiteDatabase query or rawQuery methods only.");
	} else if (err != SQLITE_DONE) {
		throw_sqlite3_exception(env, sqlite3_db_handle(statement), err);
	//	throw_sqlite3_exception(env, connection);
	}
	return err;
}

JNIEXPORT jlong JNICALL Java_blue_stack_sqlite_SQLitePreparedStatement_nativeExecuteForLastInsertedRowId(JNIEnv* env, jclass clazz,
		int connectionPtr, int statementPtr) {
	sqlite3* db = (sqlite3*)(connectionPtr);
	sqlite3_stmt* statement = (sqlite3_stmt*)(statementPtr);

	int err = executeNonQuery(env, db, statement);
	return err == SQLITE_DONE && sqlite3_changes(db) > 0
			? sqlite3_last_insert_rowid(db) : -1;
}


JNIEXPORT jint JNICALL Java_blue_stack_sqlite_SQLitePreparedStatement_nativeExecuteForChangedRowCount
(JNIEnv* env, jclass clazz,int connectionPtr, int statementPtr) {
	sqlite3* db = (sqlite3*)(connectionPtr);
	sqlite3_stmt* statement = (sqlite3_stmt*)(statementPtr);

	int err = executeNonQuery(env, db, statement);
	return err == SQLITE_DONE ? sqlite3_changes(db) : -1;
}





JNIEXPORT void Java_blue_stack_sqlite_SQLitePreparedStatement_reset(JNIEnv *env, jobject object, int statementHandle) {
	sqlite3_stmt *handle = (sqlite3_stmt *)statementHandle;

	int errcode = sqlite3_reset(handle);
	if (SQLITE_OK != errcode) {
		throw_sqlite3_exception(env, sqlite3_db_handle(handle), errcode);
	}
}

JNIEXPORT void Java_blue_stack_sqlite_SQLitePreparedStatement_finalize(JNIEnv *env, jobject object, int statementHandle) {
	sqlite3_stmt *handle = (sqlite3_stmt *)statementHandle;

	int errcode = sqlite3_finalize (handle);
	if (SQLITE_OK != errcode) {
		throw_sqlite3_exception(env, sqlite3_db_handle(handle), errcode);
	}
}

JNIEXPORT void Java_blue_stack_sqlite_SQLitePreparedStatement_bindByteBuffer(JNIEnv *env, jobject object, int statementHandle, int index, jobject value, int length) {
	sqlite3_stmt *handle = (sqlite3_stmt *)statementHandle;
	jbyte *buf = (*env)->GetDirectBufferAddress(env, value);

	int errcode = sqlite3_bind_blob(handle, index, buf, length, SQLITE_STATIC);
	if (SQLITE_OK != errcode) {
		throw_sqlite3_exception(env, sqlite3_db_handle(handle), errcode);
	}
}

JNIEXPORT void Java_blue_stack_sqlite_SQLitePreparedStatement_bindString(JNIEnv *env, jobject object, int statementHandle, int index, jstring value) {
	sqlite3_stmt *handle = (sqlite3_stmt*)statementHandle;

	char const *valueStr = (*env)->GetStringUTFChars(env, value, 0);

	int errcode = sqlite3_bind_text(handle, index, valueStr, -1, SQLITE_TRANSIENT);
	if (SQLITE_OK != errcode) {
		throw_sqlite3_exception(env, sqlite3_db_handle(handle), errcode);
	}

	if (valueStr != 0) {
		(*env)->ReleaseStringUTFChars(env, value, valueStr);
	}
}

JNIEXPORT void Java_blue_stack_sqlite_SQLitePreparedStatement_bindInt(JNIEnv *env, jobject object, int statementHandle, int index, int value) {
	sqlite3_stmt *handle = (sqlite3_stmt*)statementHandle;

	int errcode = sqlite3_bind_int(handle, index, value);
	if (SQLITE_OK != errcode) {
		throw_sqlite3_exception(env, sqlite3_db_handle(handle), errcode);
	}
}

JNIEXPORT void Java_blue_stack_sqlite_SQLitePreparedStatement_bindLong(JNIEnv *env, jobject object, int statementHandle, int index, long long value) {
	sqlite3_stmt *handle = (sqlite3_stmt*)statementHandle;

	int errcode = sqlite3_bind_int64(handle, index, value);
	if (SQLITE_OK != errcode) {
		throw_sqlite3_exception(env, sqlite3_db_handle(handle), errcode);
	}
}

JNIEXPORT void Java_blue_stack_sqlite_SQLitePreparedStatement_bindDouble(JNIEnv* env, jobject object, int statementHandle, int index, double value) {
	sqlite3_stmt *handle = (sqlite3_stmt*)statementHandle;

	int errcode = sqlite3_bind_double(handle, index, value);
	if (SQLITE_OK != errcode) {
		throw_sqlite3_exception(env, sqlite3_db_handle(handle), errcode);
	}
}

JNIEXPORT void Java_blue_stack_sqlite_SQLitePreparedStatement_bindNull(JNIEnv* env, jobject object, int statementHandle, int index) {
	sqlite3_stmt *handle = (sqlite3_stmt*)statementHandle;

	int errcode = sqlite3_bind_null(handle, index);
	if (SQLITE_OK != errcode) {
		throw_sqlite3_exception(env, sqlite3_db_handle(handle), errcode);
	}
}

