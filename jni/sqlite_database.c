#include "sqlite.h"
#include "utils.h"
#include <jni.h>
JNIEXPORT void Java_blue_stack_sqlite_SQLiteDatabase_closedb(JNIEnv *env, jobject object, int sqliteHandle) {
	sqlite3 *handle = (sqlite3 *)sqliteHandle;
	int err = sqlite3_close(handle);
	if (SQLITE_OK != err) {
		throw_sqlite3_exception(env, handle, err);
	}
}

JNIEXPORT void Java_blue_stack_sqlite_SQLiteDatabase_beginTransaction(JNIEnv *env, jobject object, int sqliteHandle) {
    sqlite3 *handle = (sqlite3 *)sqliteHandle;
    sqlite3_exec(handle, "BEGIN", 0, 0, 0);
}

JNIEXPORT void Java_blue_stack_sqlite_SQLiteDatabase_commitTransaction(JNIEnv *env, jobject object, int sqliteHandle) {
    sqlite3 *handle = (sqlite3 *)sqliteHandle;
    sqlite3_exec(handle, "COMMIT", 0, 0, 0);
}

JNIEXPORT jint JNICALL Java_blue_stack_sqlite_SQLiteDatabase_opendb(JNIEnv *env, jobject object, jstring fileName, jstring tempDir) {
    char const *fileNameStr = (*env)->GetStringUTFChars(env, fileName, 0);
    char const *tempDirStr = (*env)->GetStringUTFChars(env, tempDir, 0);
    
    if (sqlite3_temp_directory != 0) {
        sqlite3_free(sqlite3_temp_directory);
    }
    sqlite3_temp_directory = sqlite3_mprintf("%s", tempDirStr);
    
    sqlite3 *handle = 0;
    int err = sqlite3_open(fileNameStr, &handle);

    if (SQLITE_OK != err) {
    	throw_sqlite3_exception(env, handle, err);
    }
    if (fileNameStr != 0) {
        (*env)->ReleaseStringUTFChars(env, fileName, fileNameStr);
    }
    if (tempDirStr != 0) {
        (*env)->ReleaseStringUTFChars(env, tempDir, tempDirStr);
    }
 //int ret= sqlite3_key(handle, "12345678", 8);
// LOGI("sqlite3_key %d",ret);
//sqlite3_rekey(handle,"",0);
    return (int)handle;
}
