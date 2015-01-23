LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE 	:= bluedb
LOCAL_CFLAGS 	:= -DCODEC_TYPE=CODEC_TYPE_AES128 -DSQLITE_HAS_CODEC 
#LOCAL_LDLIBS 	:= -llog
LOCAL_LDLIBS 	:=  -llog

LOCAL_SRC_FILES     += \
./sqlite/sqlite3secure.c \
./jni.c \
./sqlite_cursor.c \
./sqlite_database.c \
./sqlite_statement.c \
./sqlite.c \
./utils.c 

include $(BUILD_SHARED_LIBRARY)