package edu.ucla.nesl.onoffcontroller.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "onoffcontroller.db";
	private static final int DATABASE_VERSION = 5;

	public static final String SENSOR_ALL = "all";
	public static final String SENSOR_LOCATION = "PhoneGPS";
	public static final String SENSOR_ACCELEROMETER = "PhoneAccelerometer";
	public static final String SENSOR_ECG = "ECG";
	public static final String SENSOR_RESPIRATION = "RIP";
	public static final String SENSOR_ACTIVITY = "Activity";
	public static final String SENSOR_STRESS = "Stress";
	public static final String SENSOR_CONVERSATION = "Conversation";

	public static final String TABLE_TIMERS = "timers";
	public static final String COL_SENSOR = "sensor";
	public static final String COL_START_TIME = "start_time";
	public static final String COL_DURATION = "duration";

	public static final String TABLE_RULES = "rules";
	public static final String COL_ID = "id";
	//public static final String COL_SENSOR = "sensor";
	//public static final String COL_START_TIME = "start_time";
	public static final String COL_END_TIME = "end_time";
	public static final String COL_IS_UPLOAD = "is_upload";

	private static final String CREATE_TABLE_TIMERS = "CREATE TABLE " + TABLE_TIMERS
			+ "( " + COL_SENSOR + " TEXT UNIQUE NOT NULL, "
			+ COL_START_TIME + " INTEGER, "
			+ COL_DURATION + " INTEGER);";

	private static final String CREATE_TABLE_RULES = "CREATE TABLE " + TABLE_RULES 
			+ "( " + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ COL_SENSOR + " TEXT NOT NULL, "
			+ COL_START_TIME + " INTEGER NOT NULL, "
			+ COL_END_TIME + " INTEGER NOT NULL, "
			+ COL_IS_UPLOAD + " INTEGER NOT NULL DEFAULT 0);";

	public SQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_TIMERS);
		db.execSQL(CREATE_TABLE_RULES);

		ContentValues values = new ContentValues();
		values.put(COL_SENSOR, SENSOR_ALL);
		db.insert(TABLE_TIMERS, null, values);
		values.put(COL_SENSOR, SENSOR_LOCATION);
		db.insert(TABLE_TIMERS, null, values);
		values.put(COL_SENSOR, SENSOR_ACCELEROMETER);
		db.insert(TABLE_TIMERS, null, values);
		values.put(COL_SENSOR, SENSOR_ECG);
		db.insert(TABLE_TIMERS, null, values);
		values.put(COL_SENSOR, SENSOR_RESPIRATION);
		db.insert(TABLE_TIMERS, null, values);
		values.put(COL_SENSOR, SENSOR_ACTIVITY);
		db.insert(TABLE_TIMERS, null, values);
		values.put(COL_SENSOR, SENSOR_STRESS);
		db.insert(TABLE_TIMERS, null, values);
		values.put(COL_SENSOR, SENSOR_CONVERSATION);
		db.insert(TABLE_TIMERS, null, values);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIMERS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_RULES);
		onCreate(db);
	}
}
