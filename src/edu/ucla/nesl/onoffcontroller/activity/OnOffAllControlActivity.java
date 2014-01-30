package edu.ucla.nesl.onoffcontroller.activity;

import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ToggleButton;
import edu.ucla.nesl.onoffcontroller.Const;
import edu.ucla.nesl.onoffcontroller.R;
import edu.ucla.nesl.onoffcontroller.TimerService;
import edu.ucla.nesl.onoffcontroller.db.SQLiteHelper;
import edu.ucla.nesl.onoffcontroller.db.TimerDataSource;
import edu.ucla.nesl.onoffcontroller.ui.TimerSetDialog;
import edu.ucla.nesl.onoffcontroller.ui.TimerSetDialog.OnFinishListener;

public class OnOffAllControlActivity extends Activity {

	private Context context = this;
	private TimerDataSource timerDataSource = null;
	private final int DIALOG_TIMER_SETUP = 1;
	private final int DIALOG_TIMER_EXTEND = 2;
	
	private ToggleButton allSensorButton;
	private Button countdownButton;
	private CountDownTimer countdownTimer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_onoffcontrol);
		timerDataSource = new TimerDataSource(this);
		allSensorButton = (ToggleButton)findViewById(R.id.toggle_all_sensors);
		countdownButton = (Button)findViewById(R.id.countdown_button);
		countdownButton.setOnClickListener(countdownOnClickListener);
	}

	private OnClickListener countdownOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			showDialog(DIALOG_TIMER_EXTEND);
		}
	};
	
	@Override
	protected void onResume() {
		if (timerDataSource != null) {
			timerDataSource.open();
			boolean allSensorStatus = !timerDataSource.getTimerStatus(SQLiteHelper.SENSOR_ALL);
			allSensorButton.setChecked(allSensorStatus);
			if (!allSensorStatus) {
				long startTime = timerDataSource.getStartTime(SQLiteHelper.SENSOR_ALL);
				long duration = timerDataSource.getDuration(SQLiteHelper.SENSOR_ALL);
				startCountDown(startTime, duration);
			}
		}
		registerReceiver(receiver, new IntentFilter(TimerService.BROADCAST_INTENT_MESSAGE));
		super.onResume();
	}
	
	private void stopCountDown() {
		countdownTimer.cancel();
		countdownTimer = null;
		countdownButton.setVisibility(View.INVISIBLE);
	}

	private void startCountDown(long startTime, long duration) {
		if (countdownTimer != null) {
			countdownTimer.cancel();
		}
		
		long millisInFuture;
		
		if (startTime <= 0) {
			millisInFuture = duration * 1000;
		} else {
			Calendar cal = Calendar.getInstance();
			long curTime = cal.getTimeInMillis();
			millisInFuture = (startTime+duration)*1000 - curTime;
		}
		countdownButton.setVisibility(View.VISIBLE);
		countdownTimer = new CountDownTimer(millisInFuture, 1000) {
			
			@Override
			public void onTick(long millisUntilFinished) {
				long remain = millisUntilFinished / 1000;
				long hours = remain / 3600;
				remain -= (hours * 3600);
				long minutes = remain / 60;
				remain -= (minutes * 60);
				long seconds = remain;
				countdownButton.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
			}
			
			@Override
			public void onFinish() {
				countdownButton.setVisibility(View.INVISIBLE);
			}
		};
		countdownTimer.start();
	}
	
	@Override
	protected void onPause() {
		if (timerDataSource != null) {
			timerDataSource.close();
		}
		unregisterReceiver(receiver);
		if (countdownTimer != null) { 
			countdownTimer.cancel();
		}
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.all_control_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		Intent intent;
		switch (item.getItemId()) {
		case R.id.location_accel_sensors:
			intent = new Intent(this, LocationAccelSensorsActivity.class);
			startActivity(intent);
			return true;
		case R.id.physiological_sensors:
			intent = new Intent(this, PhysiologicalSensorsActivity.class);
			startActivity(intent);
			return true;
		case R.id.inferences:
			intent = new Intent(this, InferencesActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			int requestCode = bundle.getInt(Const.BUNDLE_SENSOR_TYPE);
			if (requestCode == Const.SENSOR_TYPE_ALL) {
				allSensorButton.setChecked(true);
				countdownButton.setVisibility(View.INVISIBLE);
				if (countdownTimer != null) {
					countdownTimer.cancel();
					countdownTimer = null;
				}
			}
		}
	};
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_TIMER_EXTEND:
			return new TimerSetDialog(this, "Select time to add.", onTimerExtendFinishListener);
		case DIALOG_TIMER_SETUP:
			return new TimerSetDialog(this, "Select OFF duration.", onTimerSetFinishListener);
		}
		return null;
	}

	private OnFinishListener onTimerExtendFinishListener = new OnFinishListener() {
		
		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				long startTime = timerDataSource.getStartTime(SQLiteHelper.SENSOR_ALL);
				if (startTime == 0) {
					return;
				}
				
				long newDuration = timerDataSource.getDuration(SQLiteHelper.SENSOR_ALL) + duration;
				startCountDown(startTime, newDuration);
				
				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_ALL);
				intent.putExtra(Const.BUNDLE_TIMER_STAT, Const.TIMER_EXTEND);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
			} 
		}
	};

	private OnFinishListener onTimerSetFinishListener = new OnFinishListener() {
		
		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_ALL);
				intent.putExtra(Const.BUNDLE_TIMER_STAT, Const.TIMER_START);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
				startCountDown(0, duration);
			} else {
				allSensorButton.toggle();				
			}
		}
	};

	public void onToggleAllSensorsClicked(View view) {
		boolean allSensorStatus = allSensorButton.isChecked();
		
		if (!allSensorStatus) {
			showDialog(DIALOG_TIMER_SETUP);
		} else {
			Intent intent = new Intent(this, TimerService.class);
			intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_ALL);
			intent.putExtra(Const.BUNDLE_TIMER_STAT, Const.TIMER_STOP_BY_USER);
			startService(intent);
			stopCountDown();
		}
		
	}
}
