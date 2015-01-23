package blue.stack.bluedroiddb;

import java.io.File;

import android.app.Activity;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import blue.stack.bluedroiddb.systest.SysSQLiteOpenHelper;
import blue.stack.sqlite.SQLiteDatabase;
import blue.stack.sqlite.SQLiteException;
import blue.stack.sqlite.SQLitePreparedStatement;

public class MainActivity extends Activity {
	static {
		System.loadLibrary("bluedb");
	}
	SQLiteDatabase database;
	SysSQLiteOpenHelper mDbHelper;
	android.database.sqlite.SQLiteDatabase msq;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		try {
			File file = new File(getFilesDir().getAbsolutePath() + File.separator + "db");
			file.mkdirs();

			mDbHelper = new SysSQLiteOpenHelper(getApplicationContext());

			database = new SQLiteDatabase(file.getAbsolutePath() + File.separator + "crypt.db",
					file.getAbsolutePath());

			database.executeFast("PRAGMA secure_delete = ON").stepThis().dispose();
			database.executeFast("PRAGMA temp_store = 1").stepThis().dispose();

			database.executeFast("CREATE TABLE users(uid INTEGER  , name TEXT, status INTEGER)")
					.stepThis().dispose();
			msq = mDbHelper.getWritableDatabase();
			msq.execSQL("CREATE TABLE users(uid INTEGER  , name TEXT, status INTEGER)");

			// database.executeFast(
			// "CREATE TABLE messages(mid INTEGER PRIMARY KEY, uid INTEGER, read_state INTEGER, send_state INTEGER, date INTEGER, data BLOB, out INTEGER, ttl INTEGER, media INTEGER)")
			// .stepThis().dispose();
			database.beginTransaction();
			long start = System.currentTimeMillis();
			for (int i = 0; i < 10000; i++) {

				SQLitePreparedStatement state = database.executeFast("insert INTO users VALUES(?,?,?)");

				state.requery();
				state.bindInteger(1, i);
				state.bindString(2, i + "user");
				state.bindInteger(3, i);

				// state.bindByteBuffer(4, ByteBuffer.wrap("user".getBytes()));
				state.step();

				state.dispose();

			}
			long end = System.currentTimeMillis();
			System.out.println("MainActivity.onCreate()" + (end - start));
			database.commitTransaction();

			// msq.beginTransaction();
			start = System.currentTimeMillis();
			for (long i = 0; i < 10000; i++) {
				// msq.execSQL("insert INTO users VALUES(?,?,?)", new Object[] {
				// i, i + "user", i });
				SQLiteStatement state = msq.compileStatement("insert into users VALUES(?,?,?);");

				state.bindLong(1, i);
				state.bindString(2, i + "user");
				state.bindLong(3, i);
				state.execute();
				// System.out.println(ret);
			}
			end = System.currentTimeMillis();
			System.out.println("MainActivity.onCreate(system)" + (end - start));
			// msq.endTransaction();

			// database.executeFast("CREATE TABLE chats(uid INTEGER PRIMARY KEY, name TEXT, data BLOB)").stepThis()
			// .dispose();
			// database.executeFast(
			// "CREATE TABLE enc_chats(uid INTEGER PRIMARY KEY, user INTEGER, name TEXT, data BLOB, g BLOB, authkey BLOB, ttl INTEGER, layer INTEGER, seq_in INTEGER, seq_out INTEGER, use_count INTEGER, exchange_id INTEGER, key_date INTEGER, fprint INTEGER, fauthkey BLOB, khash BLOB)")
			// .stepThis().dispose();
			// database.executeFast(
			// "CREATE TABLE dialogs(did INTEGER PRIMARY KEY, date INTEGER, unread_count INTEGER, last_mid INTEGER)")
			// .stepThis().dispose();
			// database.executeFast("CREATE TABLE chat_settings(uid INTEGER PRIMARY KEY, participants BLOB)").stepThis()
			// .dispose();
			// database.executeFast("CREATE TABLE contacts(uid INTEGER PRIMARY KEY, mutual INTEGER)").stepThis().dispose();
			// database.executeFast("CREATE TABLE pending_read(uid INTEGER PRIMARY KEY, max_id INTEGER)").stepThis()
			// .dispose();
			// database.executeFast("CREATE TABLE media(mid INTEGER PRIMARY KEY, uid INTEGER, date INTEGER, data BLOB)")
			// .stepThis().dispose();
			// database.executeFast("CREATE TABLE media_counts(uid INTEGER PRIMARY KEY, count INTEGER)").stepThis()
			// .dispose();
			// database.executeFast("CREATE TABLE wallpapers(uid INTEGER PRIMARY KEY, data BLOB)").stepThis().dispose();
			// database.executeFast("CREATE TABLE randoms(random_id INTEGER PRIMARY KEY, mid INTEGER)").stepThis()
			// .dispose();
			// database.executeFast("CREATE TABLE enc_tasks_v2(mid INTEGER PRIMARY KEY, date INTEGER)").stepThis()
			// .dispose();
			// database.executeFast(
			// "CREATE TABLE params(id INTEGER PRIMARY KEY, seq INTEGER, pts INTEGER, date INTEGER, qts INTEGER, lsv INTEGER, sg INTEGER, pbytes BLOB)")
			// .stepThis().dispose();
			// database.executeFast("INSERT INTO params VALUES(1, 0, 0, 0, 0, 0, 0, NULL)").stepThis().dispose();
			// database.executeFast("CREATE TABLE user_photos(uid INTEGER, id INTEGER, data BLOB, PRIMARY KEY (uid, id))")
			// .stepThis().dispose();
			// database.executeFast("CREATE TABLE blocked_users(uid INTEGER PRIMARY KEY)").stepThis().dispose();
			// database.executeFast(
			// "CREATE TABLE download_queue(uid INTEGER, type INTEGER, date INTEGER, data BLOB, PRIMARY KEY (uid, type));")
			// .stepThis().dispose();
			// database.executeFast("CREATE TABLE dialog_settings(did INTEGER PRIMARY KEY, flags INTEGER);").stepThis()
			// .dispose();
			// database.executeFast("CREATE TABLE messages_seq(mid INTEGER PRIMARY KEY, seq_in INTEGER, seq_out INTEGER);")
			// .stepThis().dispose();
			// //
			// database.executeFast("CREATE TABLE secret_holes(uid INTEGER, seq_in INTEGER, seq_out INTEGER, data BLOB, PRIMARY KEY (uid, seq_in, seq_out));").stepThis().dispose();
			//
			// //
			// database.executeFast("CREATE TABLE attach_data(uid INTEGER, id INTEGER, data BLOB, PRIMARY KEY (uid, id))").stepThis().dispose();
			//
			// database.executeFast("CREATE TABLE user_contacts_v6(uid INTEGER PRIMARY KEY, fname TEXT, sname TEXT)")
			// .stepThis().dispose();
			// database.executeFast(
			// "CREATE TABLE user_phones_v6(uid INTEGER, phone TEXT, sphone TEXT, deleted INTEGER, PRIMARY KEY (uid, phone))")
			// .stepThis().dispose();
			//
			// database.executeFast(
			// "CREATE TABLE sent_files_v2(uid TEXT, type INTEGER, data BLOB, PRIMARY KEY (uid, type))")
			// .stepThis().dispose();
			// sqLiteDatabase.finalize();
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Toast.makeText(this, "End", 1).show();
		finish();
	}
}
