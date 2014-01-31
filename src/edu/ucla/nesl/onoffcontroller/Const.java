package edu.ucla.nesl.onoffcontroller;

import edu.ucla.nesl.onoffcontroller.db.SQLiteHelper;

public class Const {
	public static final String TAG = "OnOffController";

	public static final String RULE_TAG = "onoffcontroller";
	
	public static final int REQUEST_CODE_NORMAL = 1;

	public static final String PREFS_NAME = "onoffcontroller_prefs";
	public static final String PREFS_IS_FIRST = "is_first";
	public static final String PREFS_USERNAME = "username";
	public static final String PREFS_PASSWORD = "password";
	public static final String PREFS_SERVER_IP = "server_ip";

	public static final int SENSOR_TYPE_START_NUM = 1;
	public static final int SENSOR_TYPE_ALL = 1;
	public static final int SENSOR_TYPE_LOCATION = 2;
	public static final int SENSOR_TYPE_ACCELEROMETER = 3;
	public static final int SENSOR_TYPE_ECG = 4;
	public static final int SENSOR_TYPE_RESPIRATION = 5;
	public static final int SENSOR_TYPE_ACTIVITY = 6;
	public static final int SENSOR_TYPE_STRESS = 7;
	public static final int SENSOR_TYPE_CONVERSATION = 8;
	public static final int SENSOR_TYPE_END_NUM = 8;

	public static final int TIMER_INIT = 0;
	public static final int TIMER_START = 1;
	public static final int TIMER_REMINDER = 2;
	public static final int TIMER_STOP = 3;
	public static final int TIMER_STOP_BY_USER = 4;
	public static final int TIMER_EXTEND = 5;
	
	public static final String BUNDLE_SENSOR_TYPE = "sensor_type";
	public static final String BUNDLE_TIMER_OPERATION = "timer_stat";
	public static final String BUNDLE_DURATION = "duration";

	public static String getSensorString(int sensorType) {
		switch (sensorType) {
		case Const.SENSOR_TYPE_ALL:
			return "All sensors";
		case Const.SENSOR_TYPE_LOCATION:
			return "Location";
		case Const.SENSOR_TYPE_ACCELEROMETER:
			return "Accelerometer";
		case Const.SENSOR_TYPE_ECG:
			return "ECG";
		case Const.SENSOR_TYPE_RESPIRATION:
			return "Respiration";
		case Const.SENSOR_TYPE_ACTIVITY:
			return "Activity";
		case Const.SENSOR_TYPE_STRESS:
			return "Stress";
		case Const.SENSOR_TYPE_CONVERSATION:
			return "Conversation";
		}
		return null;
	}
	
	public static String convertSensorTypeNumToDbColName(int sensorType) {
		switch (sensorType) {
		case Const.SENSOR_TYPE_ALL:
			return SQLiteHelper.SENSOR_ALL;
		case Const.SENSOR_TYPE_LOCATION:
			return SQLiteHelper.SENSOR_LOCATION;
		case Const.SENSOR_TYPE_ACCELEROMETER:
			return SQLiteHelper.SENSOR_ACCELEROMETER;
		case Const.SENSOR_TYPE_ECG:
			return SQLiteHelper.SENSOR_ECG;
		case Const.SENSOR_TYPE_RESPIRATION:
			return SQLiteHelper.SENSOR_RESPIRATION;
		case Const.SENSOR_TYPE_ACTIVITY:
			return SQLiteHelper.SENSOR_ACTIVITY;
		case Const.SENSOR_TYPE_STRESS:
			return SQLiteHelper.SENSOR_STRESS;
		case Const.SENSOR_TYPE_CONVERSATION:
			return SQLiteHelper.SENSOR_CONVERSATION;
		}
		return null;
	}
}
