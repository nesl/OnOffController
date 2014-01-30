package edu.ucla.nesl.onoffcontroller.db;

import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class TimerDataSource extends DataSource {

	public TimerDataSource(Context context) {
		super(context);
	}

	public boolean getTimerStatus(String sensor) {
		Cursor c = database.query(SQLiteHelper.TABLE_TIMERS, null, SQLiteHelper.COL_SENSOR + " = ?", new String[] { sensor }, null, null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			if (!c.isNull(1)) {
				return true;
			} else {
				return false;
			}
		}
		assert false;
		return false;
	}

	public int registerTimer(String sensor, long startEpochTime, long duration) {
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.COL_START_TIME, startEpochTime);
		values.put(SQLiteHelper.COL_DURATION, duration);
		return database.update(SQLiteHelper.TABLE_TIMERS, values, SQLiteHelper.COL_SENSOR + " = ?", new String[] { sensor });
	}

	public int unregisterTimer(String sensor) {
		ContentValues values = new ContentValues();
		values.putNull(SQLiteHelper.COL_START_TIME);
		values.putNull(SQLiteHelper.COL_DURATION);
		return database.update(SQLiteHelper.TABLE_TIMERS, values, SQLiteHelper.COL_SENSOR + " = ?", new String[] { sensor });
	}

	public long getStartTime(String sensor) {
		Cursor c = database.query(SQLiteHelper.TABLE_TIMERS
				, new String[] { SQLiteHelper.COL_START_TIME }
				, SQLiteHelper.COL_SENSOR + " = ?", new String[] { sensor }
				, null, null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			return c.getLong(0);
		}
		assert false;
		return -1;
	}

	public long getDuration(String sensor) {
		Cursor c = database.query(SQLiteHelper.TABLE_TIMERS
				, new String[] { SQLiteHelper.COL_DURATION }
				, SQLiteHelper.COL_SENSOR + " = ?", new String[] { sensor }
				, null, null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			return c.getLong(0);
		}
		assert false;
		return -1;
	}

	public long insertRule(String sensor, long startTime, long endTime) {
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.COL_SENSOR, sensor);
		values.put(SQLiteHelper.COL_START_TIME, startTime);
		values.put(SQLiteHelper.COL_END_TIME, endTime);
		return database.insert(SQLiteHelper.TABLE_RULES, null, values);
	}

	public void extendTimer(String sensor, long duration) {
		database.execSQL("UPDATE " + SQLiteHelper.TABLE_TIMERS 
				+ " SET " + SQLiteHelper.COL_DURATION + " = " + SQLiteHelper.COL_DURATION + " + " + duration
				+ " WHERE " + SQLiteHelper.COL_SENSOR + " = '" + sensor + "';");
	}
}