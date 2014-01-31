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
import edu.ucla.nesl.onoffcontroller.db.DataSource;
import edu.ucla.nesl.onoffcontroller.tools.Tools;
import edu.ucla.nesl.onoffcontroller.ui.TimerSetDialog;
import edu.ucla.nesl.onoffcontroller.ui.TimerSetDialog.OnFinishListener;

public class LocationAccelSensorsActivity extends Activity {

	private Context context = this;
	private DataSource dataSource = null;

	private final int DIALOG_LOCATION_TIMER_SETUP = 1;
	private final int DIALOG_ACCEL_TIMER_SETUP = 2;
	private final int DIALOG_LOCATION_TIMER_EXTEND = 3;
	private final int DIALOG_ACCEL_TIMER_EXTEND = 4;

	private ToggleButton locationButton;
	private ToggleButton accelButton;
	private Button locationCountdownButton;
	private Button accelCountdownButton;
	private CountDownTimer locationCountdownTimer;
	private CountDownTimer accelCountdownTimer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location_accel_sensors);

		dataSource = new DataSource(this);
		locationButton = (ToggleButton)findViewById(R.id.toggle_location);
		accelButton = (ToggleButton)findViewById(R.id.toggle_accel);
		locationCountdownButton = (Button)findViewById(R.id.countdown_button_location);
		accelCountdownButton = (Button)findViewById(R.id.countdown_button_accel);
		locationCountdownButton.setOnClickListener(locationCountdownOnClickListener);
		accelCountdownButton.setOnClickListener(accelCountdownOnClickListener);
	}

	private OnClickListener locationCountdownOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			showDialog(DIALOG_LOCATION_TIMER_EXTEND);
		}
	};

	private OnClickListener accelCountdownOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			showDialog(DIALOG_ACCEL_TIMER_EXTEND);
		}
	};

	@Override
	protected void onResume() {
		if (dataSource != null) {
			dataSource.open();
			boolean locationSensorStatus = !dataSource.getTimerStatus(SQLiteHelper.SENSOR_LOCATION);
			locationButton.setChecked(locationSensorStatus);
			if (!locationSensorStatus) {
				long startTime = dataSource.getStartTime(SQLiteHelper.SENSOR_LOCATION);
				long duration = dataSource.getDuration(SQLiteHelper.SENSOR_LOCATION);
				startCountDown(startTime, duration, Const.SENSOR_TYPE_LOCATION);
			} else {
				stopCountDown(Const.SENSOR_TYPE_LOCATION);
			}

			boolean accelSensorStatus = !dataSource.getTimerStatus(SQLiteHelper.SENSOR_ACCELEROMETER);
			accelButton.setChecked(accelSensorStatus);
			if (!accelSensorStatus) {
				long startTime = dataSource.getStartTime(SQLiteHelper.SENSOR_ACCELEROMETER);
				long duration = dataSource.getDuration(SQLiteHelper.SENSOR_ACCELEROMETER);
				startCountDown(startTime, duration, Const.SENSOR_TYPE_ACCELEROMETER);
			} else {
				stopCountDown(Const.SENSOR_TYPE_ACCELEROMETER);
			}
		}
		registerReceiver(receiver, new IntentFilter(TimerService.BROADCAST_INTENT_MESSAGE));
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (dataSource != null) {
			dataSource.close();
		}
		unregisterReceiver(receiver);
		if (locationCountdownTimer != null) { 
			locationCountdownTimer.cancel();
		}
		if (accelCountdownTimer != null) { 
			accelCountdownTimer.cancel();
		}
		super.onPause();
	}

	private void stopCountDown(int sensorType) {
		if (sensorType == Const.SENSOR_TYPE_LOCATION) {
			if (locationCountdownTimer != null) {
				locationCountdownTimer.cancel();
				locationCountdownTimer = null;
			}
			locationCountdownButton.setVisibility(View.INVISIBLE);
		} else if (sensorType == Const.SENSOR_TYPE_ACCELEROMETER) {
			if (accelCountdownTimer != null) {
				accelCountdownTimer.cancel();
				accelCountdownTimer = null;
			}
			accelCountdownButton.setVisibility(View.INVISIBLE);
		}
	}

	private void startCountDown(long startTime, long duration, int sensorType) {
		CountDownTimer countdownTimer;
		final Button countdownButton;

		if (sensorType == Const.SENSOR_TYPE_LOCATION) {
			countdownTimer = locationCountdownTimer;
			countdownButton = locationCountdownButton;
		} else if (sensorType == Const.SENSOR_TYPE_ACCELEROMETER) {
			countdownTimer = accelCountdownTimer;
			countdownButton = accelCountdownButton;
		} else {
			return;
		}

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

		if (sensorType == Const.SENSOR_TYPE_LOCATION) {
			locationCountdownTimer = countdownTimer;
		} else if (sensorType == Const.SENSOR_TYPE_ACCELEROMETER) {
			accelCountdownTimer = countdownTimer;
		} 
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			int requestCode = bundle.getInt(Const.BUNDLE_SENSOR_TYPE);
			if (requestCode == Const.SENSOR_TYPE_LOCATION) {
				locationButton.setChecked(true);
				locationCountdownButton.setVisibility(View.INVISIBLE);
				if (locationCountdownTimer != null) {
					locationCountdownTimer.cancel();
					locationCountdownTimer = null;
				}
			} else if (requestCode == Const.SENSOR_TYPE_ACCELEROMETER) {
				accelButton.setChecked(true);
				accelCountdownButton.setVisibility(View.INVISIBLE);
				if (accelCountdownTimer != null) {
					accelCountdownTimer.cancel();
					accelCountdownTimer = null;
				}
			}
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_LOCATION_TIMER_EXTEND:
			return new TimerSetDialog(this, "Select time to add.", onLocationTimerExtendFinishListener);
		case DIALOG_LOCATION_TIMER_SETUP:
			return new TimerSetDialog(this, "Select OFF duration.", onLocationTimerSetFinishListener);
		case DIALOG_ACCEL_TIMER_EXTEND:
			return new TimerSetDialog(this, "Select time to add.", onAccelTimerExtendFinishListener);
		case DIALOG_ACCEL_TIMER_SETUP:
			return new TimerSetDialog(this, "Select OFF duration.", onAccelTimerSetFinishListener);
		}
		return null;
	}

	private OnFinishListener onLocationTimerExtendFinishListener = new OnFinishListener() {

		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				long startTime = dataSource.getStartTime(SQLiteHelper.SENSOR_LOCATION);
				if (startTime == 0) {
					return;
				}

				long newDuration = dataSource.getDuration(SQLiteHelper.SENSOR_LOCATION) + duration;
				startCountDown(startTime, newDuration, Const.SENSOR_TYPE_LOCATION);

				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_LOCATION);
				intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_EXTEND);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
			} 
		}
	};

	private OnFinishListener onLocationTimerSetFinishListener = new OnFinishListener() {

		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_LOCATION);
				intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_START);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
				startCountDown(0, duration, Const.SENSOR_TYPE_LOCATION);
			} else {
				locationButton.toggle();				
			}
		}
	};

	private OnFinishListener onAccelTimerExtendFinishListener = new OnFinishListener() {

		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				long startTime = dataSource.getStartTime(SQLiteHelper.SENSOR_ACCELEROMETER);
				if (startTime == 0) {
					return;
				}

				long newDuration = dataSource.getDuration(SQLiteHelper.SENSOR_ACCELEROMETER) + duration;
				startCountDown(startTime, newDuration, Const.SENSOR_TYPE_ACCELEROMETER);

				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_ACCELEROMETER);
				intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_EXTEND);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
			} 
		}
	};

	private OnFinishListener onAccelTimerSetFinishListener = new OnFinishListener() {

		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_ACCELEROMETER);
				intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_START);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
				startCountDown(0, duration, Const.SENSOR_TYPE_ACCELEROMETER);
			} else {
				accelButton.toggle();				
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.location_accel_control_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		// Handle item selection
		Intent intent;
		switch (item.getItemId()) {
		case R.id.all_sensors:
			if (Tools.isIndividualSensors(context, dataSource)) {
				return true;
			}
			intent = new Intent(this, OnOffAllControlActivity.class);
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

	public void onToggleLocationClicked(View view) {
		boolean locationStatus = locationButton.isChecked();

		if (!locationStatus) {
			showDialog(DIALOG_LOCATION_TIMER_SETUP);
		} else {
			Intent intent = new Intent(this, TimerService.class);
			intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_LOCATION);
			intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_STOP_BY_USER);
			startService(intent);
			stopCountDown(Const.SENSOR_TYPE_LOCATION);
		}
	}

	public void onToggleAccelerometerClicked(View view) {
		boolean accelStatus = accelButton.isChecked();

		if (!accelStatus) {
			showDialog(DIALOG_ACCEL_TIMER_SETUP);
		} else {
			Intent intent = new Intent(this, TimerService.class);
			intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_ACCELEROMETER);
			intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_STOP_BY_USER);
			startService(intent);
			stopCountDown(Const.SENSOR_TYPE_ACCELEROMETER);
		}
	}
}
