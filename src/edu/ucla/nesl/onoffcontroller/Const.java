package edu.ucla.nesl.onoffcontroller;

public class Const {
	public static final String TAG = "OnOffController";
	
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
}
