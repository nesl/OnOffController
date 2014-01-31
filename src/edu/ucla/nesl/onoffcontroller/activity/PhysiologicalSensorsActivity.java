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
import edu.ucla.nesl.onoffcontroller.tools.Tools;
import edu.ucla.nesl.onoffcontroller.ui.TimerSetDialog;
import edu.ucla.nesl.onoffcontroller.ui.TimerSetDialog.OnFinishListener;

public class PhysiologicalSensorsActivity extends Activity {

	private Context context = this;
	private TimerDataSource timerDataSource = null;

	private final int DIALOG_ECG_TIMER_SETUP = 1;
	private final int DIALOG_RESPIRATION_TIMER_SETUP = 2;
	private final int DIALOG_ECG_TIMER_EXTEND = 3;
	private final int DIALOG_RESPIRATION_TIMER_EXTEND = 4;

	private ToggleButton ecgButton;
	private ToggleButton respirationButton;
	private Button ecgCountdownButton;
	private Button respirationCountdownButton;
	private CountDownTimer ecgCountdownTimer;
	private CountDownTimer respirationCountdownTimer;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_physiological_sensors);

		timerDataSource = new TimerDataSource(this);
		ecgButton = (ToggleButton)findViewById(R.id.toggle_ecg);
		respirationButton = (ToggleButton)findViewById(R.id.toggle_respiration);
		ecgCountdownButton = (Button)findViewById(R.id.countdown_button_ecg);
		respirationCountdownButton = (Button)findViewById(R.id.countdown_button_respiration);
		ecgCountdownButton.setOnClickListener(ecgCountdownOnClickListener);
		respirationCountdownButton.setOnClickListener(respirationCountdownOnClickListener);
	}

	private OnClickListener ecgCountdownOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			showDialog(DIALOG_ECG_TIMER_EXTEND);
		}
	};

	private OnClickListener respirationCountdownOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			showDialog(DIALOG_RESPIRATION_TIMER_EXTEND);
		}
	};

	@Override
	protected void onResume() {
		if (timerDataSource != null) {
			timerDataSource.open();
			boolean ecgSensorStatus = !timerDataSource.getTimerStatus(SQLiteHelper.SENSOR_ECG);
			ecgButton.setChecked(ecgSensorStatus);
			if (!ecgSensorStatus) {
				long startTime = timerDataSource.getStartTime(SQLiteHelper.SENSOR_ECG);
				long duration = timerDataSource.getDuration(SQLiteHelper.SENSOR_ECG);
				startCountDown(startTime, duration, Const.SENSOR_TYPE_ECG);
			}
			boolean respirationSensorStatus = !timerDataSource.getTimerStatus(SQLiteHelper.SENSOR_RESPIRATION);
			respirationButton.setChecked(respirationSensorStatus);
			if (!respirationSensorStatus) {
				long startTime = timerDataSource.getStartTime(SQLiteHelper.SENSOR_RESPIRATION);
				long duration = timerDataSource.getDuration(SQLiteHelper.SENSOR_RESPIRATION);
				startCountDown(startTime, duration, Const.SENSOR_TYPE_RESPIRATION);
			}
		}
		registerReceiver(receiver, new IntentFilter(TimerService.BROADCAST_INTENT_MESSAGE));
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		if (timerDataSource != null) {
			timerDataSource.close();
		}
		unregisterReceiver(receiver);
		if (ecgCountdownTimer != null) { 
			ecgCountdownTimer.cancel();
		}
		if (respirationCountdownTimer != null) { 
			respirationCountdownTimer.cancel();
		}
		super.onPause();
	}

	private void stopCountDown(int sensorType) {
		if (sensorType == Const.SENSOR_TYPE_ECG) {
			if (ecgCountdownTimer != null) {
				ecgCountdownTimer.cancel();
				ecgCountdownTimer = null;
			}
			ecgCountdownButton.setVisibility(View.INVISIBLE);
		} else if (sensorType == Const.SENSOR_TYPE_RESPIRATION) {
			if (respirationCountdownTimer != null) {
				respirationCountdownTimer.cancel();
				respirationCountdownTimer = null;
			}
			respirationCountdownButton.setVisibility(View.INVISIBLE);
		}
	}

	private void startCountDown(long startTime, long duration, int sensorType) {
		CountDownTimer countdownTimer;
		final Button countdownButton;
		
		if (sensorType == Const.SENSOR_TYPE_ECG) {
			countdownTimer = ecgCountdownTimer;
			countdownButton = ecgCountdownButton;
		} else if (sensorType == Const.SENSOR_TYPE_RESPIRATION) {
			countdownTimer = respirationCountdownTimer;
			countdownButton = respirationCountdownButton;
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
		
		if (sensorType == Const.SENSOR_TYPE_ECG) {
			ecgCountdownTimer = countdownTimer;
		} else if (sensorType == Const.SENSOR_TYPE_RESPIRATION) {
			respirationCountdownTimer = countdownTimer;
		} 
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			int requestCode = bundle.getInt(Const.BUNDLE_SENSOR_TYPE);
			if (requestCode == Const.SENSOR_TYPE_ECG) {
				ecgButton.setChecked(true);
				ecgCountdownButton.setVisibility(View.INVISIBLE);
				if (ecgCountdownTimer != null) {
					ecgCountdownTimer.cancel();
					ecgCountdownTimer = null;
				}
			} else if (requestCode == Const.SENSOR_TYPE_RESPIRATION) {
				respirationButton.setChecked(true);
				respirationCountdownButton.setVisibility(View.INVISIBLE);
				if (respirationCountdownTimer != null) {
					respirationCountdownTimer.cancel();
					respirationCountdownTimer = null;
				}
			}
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ECG_TIMER_EXTEND:
			return new TimerSetDialog(this, "Select time to add.", onLocationTimerExtendFinishListener);
		case DIALOG_ECG_TIMER_SETUP:
			return new TimerSetDialog(this, "Select OFF duration.", onLocationTimerSetFinishListener);
		case DIALOG_RESPIRATION_TIMER_EXTEND:
			return new TimerSetDialog(this, "Select time to add.", onAccelTimerExtendFinishListener);
		case DIALOG_RESPIRATION_TIMER_SETUP:
			return new TimerSetDialog(this, "Select OFF duration.", onAccelTimerSetFinishListener);
		}
		return null;
	}

	private OnFinishListener onLocationTimerExtendFinishListener = new OnFinishListener() {
		
		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				long startTime = timerDataSource.getStartTime(SQLiteHelper.SENSOR_ECG);
				if (startTime == 0) {
					return;
				}
				
				long newDuration = timerDataSource.getDuration(SQLiteHelper.SENSOR_ECG) + duration;
				startCountDown(startTime, newDuration, Const.SENSOR_TYPE_ECG);
				
				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_ECG);
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
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_ECG);
				intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_START);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
				startCountDown(0, duration, Const.SENSOR_TYPE_ECG);
			} else {
				ecgButton.toggle();				
			}
		}
	};

	private OnFinishListener onAccelTimerExtendFinishListener = new OnFinishListener() {
		
		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				long startTime = timerDataSource.getStartTime(SQLiteHelper.SENSOR_RESPIRATION);
				if (startTime == 0) {
					return;
				}
				
				long newDuration = timerDataSource.getDuration(SQLiteHelper.SENSOR_RESPIRATION) + duration;
				startCountDown(startTime, newDuration, Const.SENSOR_TYPE_RESPIRATION);
				
				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_RESPIRATION);
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
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_RESPIRATION);
				intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_START);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
				startCountDown(0, duration, Const.SENSOR_TYPE_RESPIRATION);
			} else {
				respirationButton.toggle();				
			}
		}
	};
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.physio_sensor_control_menu, menu);
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
		case R.id.all_sensors:
			if (Tools.isIndividualSensors(context, timerDataSource)) {
				return true;
			}
			intent = new Intent(this, OnOffAllControlActivity.class);
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

	public void onToggleECGClicked(View view) {
		boolean ecgStatus = ecgButton.isChecked();
		
		if (!ecgStatus) {
			showDialog(DIALOG_ECG_TIMER_SETUP);
		} else {
			Intent intent = new Intent(this, TimerService.class);
			intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_ECG);
			intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_STOP_BY_USER);
			startService(intent);
			stopCountDown(Const.SENSOR_TYPE_ECG);
		}
	}

	public void onToggleRespirationClicked(View view) {
		boolean respirationStatus = respirationButton.isChecked();
		
		if (!respirationStatus) {
			showDialog(DIALOG_RESPIRATION_TIMER_SETUP);
		} else {
			Intent intent = new Intent(this, TimerService.class);
			intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_RESPIRATION);
			intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_STOP_BY_USER);
			startService(intent);
			stopCountDown(Const.SENSOR_TYPE_RESPIRATION);
		}
	}
}
