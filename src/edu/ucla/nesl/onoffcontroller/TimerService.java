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
import edu.ucla.nesl.onoffcontroller.activity.OnOffAllControlActivity;
import edu.ucla.nesl.onoffcontroller.db.SQLiteHelper;
import edu.ucla.nesl.onoffcontroller.db.TimerDataSource;

public class TimerService extends IntentService {

	private static final int REMINDER_TIMES[] = { 5, 2 }; // seconds;

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
		int timerStat = bundle.getInt(Const.BUNDLE_TIMER_STAT);
		int sensorType = bundle.getInt(Const.BUNDLE_SENSOR_TYPE);

		Log.d(Const.TAG, "onHandleIntent() timerStat = " + timerStat);

		switch (timerStat) {
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
	}

	private String getDbCol(int sensorType) {
		switch (sensorType) {
		case Const.SENSOR_TYPE_ALL:
			return SQLiteHelper.SENSOR_ALL;
		}
		return null;
	}

	private void handleTimerExtend(int sensorType, long duration) {
		tds = new TimerDataSource(this);
		tds.open();
		tds.extendTimer(getDbCol(sensorType), duration);
		long newDuration = tds.getDuration(getDbCol(sensorType));
		long startTime = tds.getStartTime(getDbCol(sensorType));
		tds.close();

		if (isServiceScheduled(sensorType)) {
			cancelServiceSchedule(sensorType);
			Log.d(Const.TAG, "canceld.");
		}
		cancelNotification();
		scheduleNextAlarm(startTime, newDuration, Calendar.getInstance().getTimeInMillis() / 1000, sensorType);
	}

	private void handleTimerStopByUser(int sensorType) {
		Calendar cal = Calendar.getInstance();
		long curTime = cal.getTimeInMillis() / 1000;

		tds = new TimerDataSource(this);
		tds.open();
		createRule(getDbCol(sensorType), curTime);
		tds.unregisterTimer(getDbCol(sensorType));
		tds.close();

		if (isServiceScheduled(sensorType)) {
			cancelServiceSchedule(sensorType);
		}
	}

	private void handleTimerStop(int sensorType) {
		Calendar cal = Calendar.getInstance();
		long curTime = cal.getTimeInMillis() / 1000;

		tds = new TimerDataSource(this);
		tds.open();
		createRule(getDbCol(sensorType), curTime);
		tds.unregisterTimer(getDbCol(sensorType));
		tds.close();

		String message = "All Sensors are active now.";
		notifyUser(message, sensorType);

		notifyActivity(sensorType);
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

	private void createRule(String sensor, long curTime) {
		long startTime = tds.getStartTime(sensor);
		tds.insertRule(sensor, startTime, curTime);
	}

	private void handleTimerReminder(int sensorType) {
		Calendar cal = Calendar.getInstance();
		long curTime = cal.getTimeInMillis() / 1000;
		long startTime;
		long duration;

		tds = new TimerDataSource(this);
		tds.open();
		startTime = tds.getStartTime(getDbCol(sensorType));
		duration = tds.getDuration(getDbCol(sensorType));
		tds.close();

		long remainingTime = startTime + duration - curTime;
		String message = "All sensors will be active in " + getTimeStr(remainingTime);
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

		tds = new TimerDataSource(this);
		tds.open();

		Calendar cal = Calendar.getInstance();
		long epoch = cal.getTimeInMillis() / 1000;

		switch(sensorType) {
		case Const.SENSOR_TYPE_ALL:
			tds.registerTimer(SQLiteHelper.SENSOR_ALL, epoch, duration);
			cancelNotification();
			break;
		}
		tds.close();

		long startTime = epoch;

		scheduleNextAlarm(startTime, duration, startTime, sensorType);
	}

	private void scheduleNextAlarm(long startTime, long duration, long curTime, int sensorType) {
		long nextReminderTime = getNextReminderTime(startTime, duration, curTime);
		long expireTime;
		int timerStat;
		if (nextReminderTime <= 0) {
			timerStat = Const.TIMER_STOP;
			expireTime = startTime + duration;
		} else {
			timerStat = Const.TIMER_REMINDER;
			expireTime = nextReminderTime;
		}

		Log.d(Const.TAG, "curTime = " + curTime + ", expireTime = " + expireTime + ", timerStat = " + timerStat);

		if (expireTime <= curTime) {
			return;
		}

		// Schedule alarm
		Intent intent = new Intent(this, TimerService.class);
		intent.putExtra(Const.BUNDLE_SENSOR_TYPE, sensorType);
		intent.putExtra(Const.BUNDLE_TIMER_STAT, timerStat);
		PendingIntent pintent = PendingIntent.getService(this, sensorType, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarm.set(AlarmManager.RTC_WAKEUP, expireTime * 1000, pintent);
	}

	private long getNextReminderTime(long startTime, long duration, long curTime) {
		long remainingTime = duration - (curTime - startTime);
		int selectedRemindTime = -1;
		for (int remindTime : REMINDER_TIMES) {
			if (remindTime < remainingTime) {
				if (remindTime > selectedRemindTime) {
					selectedRemindTime = remindTime;
				}
			}
		}
		if (selectedRemindTime > 0) {
			return startTime + (duration - selectedRemindTime);
		}
		return 0;
	}

	private void notifyActivity(int requestCode) {
		Intent intent = new Intent(BROADCAST_INTENT_MESSAGE);
		intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_ALL);
		sendBroadcast(intent);
	}

	private void cancelNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(0);
	}

	private void createNotification(String title, String message, int sensorType) {
		Intent intent = null;
		switch (sensorType) {
		case Const.SENSOR_TYPE_ALL:
			intent = new Intent(getApplicationContext(), OnOffAllControlActivity.class);
			break;
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
		notificationManager.notify(0, noti);
	}
}
