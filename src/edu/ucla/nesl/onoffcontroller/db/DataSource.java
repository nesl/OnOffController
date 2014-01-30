package edu.ucla.nesl.onoffcontroller.db;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DataSource {
	protected SQLiteDatabase database;
	protected SQLiteHelper dbHelper;

	public DataSource(Context context) {
		dbHelper = new SQLiteHelper(context);
	}
	
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}
	
	public void close() {
		dbHelper.close();
	}
}
