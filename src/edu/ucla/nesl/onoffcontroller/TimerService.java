package edu.ucla.nesl.onoffcontroller;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import edu.ucla.nesl.onoffcontroller.activity.InferencesActivity;
import edu.ucla.nesl.onoffcontroller.activity.LocationAccelSensorsActivity;
import edu.ucla.nesl.onoffcontroller.activity.OnOffAllControlActivity;
import edu.ucla.nesl.onoffcontroller.activity.PhysiologicalSensorsActivity;
import edu.ucla.nesl.onoffcontroller.db.SQLiteHelper;
import edu.ucla.nesl.onoffcontroller.db.TimerDataSource;

public class TimerService extends IntentService {

	private static final int REMINDER_TIMES[] = { 5*60, 2*60 }; // seconds;

	public static final String BROADCAST_INTENT_MESSAGE = "edu.ucla.nesl.onoffcontroller.TimerService"; 

	private static final String NOTIFICATION_TITLE = "On/Off Controller";

	private TimerDataSource tds;

	private Handler handler;

	public TimerService() {
		super("TimerService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		handler = new Handler();
	}

	private void postNotifyUser(final String msg) {
		handler.post(new Runnable() {            
			@Override
			public void run() {
				Toast.makeText(TimerService.this, msg, Toast.LENGTH_LONG).show();

				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(1000);

				Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
				r.play();
			}
		});
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle bundle = intent.getExtras();
		int timerStat = bundle.getInt(Const.BUNDLE_TIMER_OPERATION);
		int sensorType = bundle.getInt(Const.BUNDLE_SENSOR_TYPE);

		Log.d(Const.TAG, "onHandleIntent() timerStat = " + timerStat);

		tds = new TimerDataSource(this);
		tds.open();

		switch (timerStat) {		
		case Const.TIMER_INIT:
			handleTimerInit();
			break;
		case Const.TIMER_START:
			handleTimerStart(sensorType, bundle.getLong(Const.BUNDLE_DURATION));
			break;
		case Const.TIMER_REMINDER:
			handleTimerReminder(sensorType);
			break;
		case Const.TIMER_STOP:
			handleTimerStop(sensorType);
			break;
		case Const.TIMER_STOP_BY_USER:
			handleTimerStopByUser(sensorType);
			break;
		case Const.TIMER_EXTEND:
			handleTimerExtend(sensorType, bundle.getLong(Const.BUNDLE_DURATION));
			break;
		}
		
		tds.close();
	}

	private void handleTimerInit() {
		Log.d(Const.TAG, "TimerInit()");
		for (int sensorType = Const.SENSOR_TYPE_START_NUM; sensorType <= Const.SENSOR_TYPE_END_NUM; sensorType++) {
			if (tds.getTimerStatus(getDbCol(sensorType))) {
				long startTime = tds.getStartTime(getDbCol(sensorType));
				long duration = tds.getDuration(getDbCol(sensorType));
				long expireTime = startTime + duration;
				long curTime = Calendar.getInstance().getTimeInMillis() / 1000;

				if (expireTime <= curTime) {
					handleTimerStop(sensorType);
				} else {
					if (!isServiceScheduled(sensorType)) {
						scheduleNextAlarm(startTime, duration, curTime, sensorType);
					}
				}
			}
		}
	}

	private String getDbCol(int sensorType) {
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

	private void handleTimerExtend(int sensorType, long duration) {
		tds.extendTimer(getDbCol(sensorType), duration);
		long newDuration = tds.getDuration(getDbCol(sensorType));
		long startTime = tds.getStartTime(getDbCol(sensorType));

		if (isServiceScheduled(sensorType)) {
			cancelServiceSchedule(sensorType);
			Log.d(Const.TAG, "canceld.");
		}
		cancelNotification(sensorType);
		scheduleNextAlarm(startTime, newDuration, Calendar.getInstance().getTimeInMillis() / 1000, sensorType);
	}

	private void handleTimerStopByUser(int sensorType) {
		Calendar cal = Calendar.getInstance();
		long curTime = cal.getTimeInMillis() / 1000;

		createRule(getDbCol(sensorType), curTime);
		tds.unregisterTimer(getDbCol(sensorType));

		if (isServiceScheduled(sensorType)) {
			cancelServiceSchedule(sensorType);
		}
	}

	private void handleTimerStop(int sensorType) {
		createRule(getDbCol(sensorType));
		tds.unregisterTimer(getDbCol(sensorType));

		String sensorStr = getSensorString(sensorType);
		String message;
		if (sensorType == Const.SENSOR_TYPE_ALL) {
			message = sensorStr + " are active now.";
		} else {
			message = sensorStr + " is active now.";
		}
		notifyUser(message, sensorType);

		notifyActivity(sensorType);
	}

	private String getSensorString(int sensorType) {
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

	private boolean isServiceScheduled(int sensorType) {
		return PendingIntent.getService(this, sensorType, new Intent(this, TimerService.class), PendingIntent.FLAG_NO_CREATE) != null;
	}

	private void cancelServiceSchedule(int sensorType) {
		Intent intent = new Intent(this, TimerService.class);
		PendingIntent pintent = PendingIntent.getService(this, sensorType, intent, 0);
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pintent);
	}

	private void createRule(String sensor) {
		long startTime = tds.getStartTime(sensor);
		long duration = tds.getDuration(sensor);
		tds.insertRule(sensor, startTime, startTime + duration);
	}

	private void createRule(String sensor, long curTimeInSecs) {
		long startTime = tds.getStartTime(sensor);
		tds.insertRule(sensor, startTime, curTimeInSecs);
	}

	private void handleTimerReminder(int sensorType) {
		Calendar cal = Calendar.getInstance();
		long curTime = cal.getTimeInMillis() / 1000;
		long startTime;
		long duration;

		startTime = tds.getStartTime(getDbCol(sensorType));
		duration = tds.getDuration(getDbCol(sensorType));

		long remainingTime = startTime + duration - curTime;
		String sensorStr = getSensorString(sensorType);
		String message = sensorStr + " will be active in " + getTimeStr(remainingTime);
		notifyUser(message, sensorType);			
		scheduleNextAlarm(startTime, duration, curTime, sensorType);
	}

	private void notifyUser(String message, int sensorType) {
		createNotification(NOTIFICATION_TITLE, message, sensorType);
		postNotifyUser(message);
	}

	private String getTimeStr(long time) {
		if (time < 60) {
			return "" + time + " seconds.";
		} else if (time < 3600){
			return "" + time/60 + " minutes.";
		} else {
			return "" + time/3600 + " hours.";
		}
	}

	private void handleTimerStart(int sensorType, long duration) {

		Calendar cal = Calendar.getInstance();
		long epoch = cal.getTimeInMillis() / 1000;
		tds.registerTimer(getDbCol(sensorType), epoch, duration);
		cancelNotification(sensorType);

		long startTime = epoch;

		scheduleNextAlarm(startTime, duration, startTime, sensorType);
	}

	private void scheduleNextAlarm(long startTimeInSecs, long durationInSecs, long curTimeInSecs, int sensorType) {
		long nextReminderTime = getNextReminderTime(startTimeInSecs, durationInSecs, curTimeInSecs);
		long expireTime;
		int timerStat;
		if (nextReminderTime <= 0) {
			timerStat = Const.TIMER_STOP;
			expireTime = startTimeInSecs + durationInSecs;
		} else {
			timerStat = Const.TIMER_REMINDER;
			expireTime = nextReminderTime;
		}

		Log.d(Const.TAG, "curTime = " + curTimeInSecs + ", expireTime = " + expireTime + ", timerStat = " + timerStat);

		if (expireTime <= curTimeInSecs) {
			return;
		}

		// Schedule alarm
		Intent intent = new Intent(this, TimerService.class);
		intent.putExtra(Const.BUNDLE_SENSOR_TYPE, sensorType);
		intent.putExtra(Const.BUNDLE_TIMER_OPERATION, timerStat);
		PendingIntent pintent = PendingIntent.getService(this, sensorType, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarm.set(AlarmManager.RTC_WAKEUP, expireTime * 1000, pintent);
	}

	private long getNextReminderTime(long startTimeInSecs, long durationInSecs, long curTimeInSecs) {
		long remainingTime = durationInSecs - (curTimeInSecs - startTimeInSecs);
		int selectedRemindTime = -1;
		for (int remindTime : REMINDER_TIMES) {
			if (remindTime < remainingTime) {
				if (remindTime > selectedRemindTime) {
					selectedRemindTime = remindTime;
				}
			}
		}
		if (selectedRemindTime > 0) {
			return startTimeInSecs + (durationInSecs - selectedRemindTime);
		}
		return 0;
	}

	private void notifyActivity(int sensorType) {
		Intent intent = new Intent(BROADCAST_INTENT_MESSAGE);
		intent.putExtra(Const.BUNDLE_SENSOR_TYPE, sensorType);
		sendBroadcast(intent);
	}

	private void cancelNotification(int sensorType) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(sensorType);
	}

	private void createNotification(String title, String message, int sensorType) {
		Intent intent = null;
		switch (sensorType) {
		case Const.SENSOR_TYPE_ALL:
			intent = new Intent(getApplicationContext(), OnOffAllControlActivity.class);
			break;
		case Const.SENSOR_TYPE_LOCATION:
		case Const.SENSOR_TYPE_ACCELEROMETER:
			intent = new Intent(getApplicationContext(), LocationAccelSensorsActivity.class);
			break;
		case Const.SENSOR_TYPE_ECG:
		case Const.SENSOR_TYPE_RESPIRATION:
			intent = new Intent(getApplicationContext(), PhysiologicalSensorsActivity.class);
			break;
		case Const.SENSOR_TYPE_ACTIVITY:
		case Const.SENSOR_TYPE_STRESS:
		case Const.SENSOR_TYPE_CONVERSATION:
			intent = new Intent(getApplicationContext(), InferencesActivity.class);
			break;
		default:
			return;
		}
		intent.setAction("android.intent.action.MAIN");

		PendingIntent pintent = PendingIntent.getActivity(
				getApplicationContext(),
				0,
				intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		Notification noti = new NotificationCompat.Builder(this)
		.setContentTitle(title)
		.setContentText(message)
		.setContentIntent(pintent)
		.setSmallIcon(R.drawable.ic_launcher)
		.build();

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		noti.flags |= Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(sensorType, noti);
	}
}
