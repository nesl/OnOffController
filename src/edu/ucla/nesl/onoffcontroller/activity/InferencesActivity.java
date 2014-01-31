package edu.ucla.nesl.onoffcontroller.activity;

import java.util.Calendar;

import edu.ucla.nesl.onoffcontroller.Const;
import edu.ucla.nesl.onoffcontroller.R;
import edu.ucla.nesl.onoffcontroller.TimerService;
import edu.ucla.nesl.onoffcontroller.db.SQLiteHelper;
import edu.ucla.nesl.onoffcontroller.db.TimerDataSource;
import edu.ucla.nesl.onoffcontroller.ui.TimerSetDialog;
import edu.ucla.nesl.onoffcontroller.ui.TimerSetDialog.OnFinishListener;
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

public class InferencesActivity extends Activity {

	private Context context = this;
	private TimerDataSource timerDataSource = null;

	private final int DIALOG_ACTIVITY_TIMER_SETUP = 1;
	private final int DIALOG_STRESS_TIMER_SETUP = 2;
	private final int DIALOG_CONVERSATION_TIMER_SETUP = 3;
	private final int DIALOG_ACTIVITY_TIMER_EXTEND = 4;
	private final int DIALOG_STRESS_TIMER_EXTEND = 5;
	private final int DIALOG_CONVERSATION_TIMER_EXTEND = 6;

	private ToggleButton activityButton;
	private ToggleButton stressButton;
	private ToggleButton conversationButton;
	private Button activityCountdownButton;
	private Button stressCountdownButton;
	private Button conversationCountdownButton;
	private CountDownTimer activityCountdownTimer;
	private CountDownTimer stressCountdownTimer;
	private CountDownTimer conversationCountdownTimer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_inferences);

		timerDataSource = new TimerDataSource(this);
		activityButton = (ToggleButton)findViewById(R.id.toggle_activity);
		stressButton = (ToggleButton)findViewById(R.id.toggle_stress);
		conversationButton = (ToggleButton)findViewById(R.id.toggle_conversation);
		activityCountdownButton = (Button)findViewById(R.id.countdown_button_activity);
		stressCountdownButton = (Button)findViewById(R.id.countdown_button_stress);
		conversationCountdownButton = (Button)findViewById(R.id.countdown_button_conversation);
		activityCountdownButton.setOnClickListener(activityCountdownOnClickListener);
		stressCountdownButton.setOnClickListener(stressCountdownOnClickListener);
		conversationCountdownButton.setOnClickListener(conversationCountdownOnClickListener);
	}

	private OnClickListener activityCountdownOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			showDialog(DIALOG_ACTIVITY_TIMER_EXTEND);
		}
	};

	private OnClickListener stressCountdownOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			showDialog(DIALOG_STRESS_TIMER_EXTEND);
		}
	};

	private OnClickListener conversationCountdownOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			showDialog(DIALOG_CONVERSATION_TIMER_EXTEND);
		}
	};

	@Override
	protected void onResume() {
		if (timerDataSource != null) {
			timerDataSource.open();

			boolean activitySensorStatus = !timerDataSource.getTimerStatus(SQLiteHelper.SENSOR_ACTIVITY);
			activityButton.setChecked(activitySensorStatus);
			if (!activitySensorStatus) {
				long startTime = timerDataSource.getStartTime(SQLiteHelper.SENSOR_ACTIVITY);
				long duration = timerDataSource.getDuration(SQLiteHelper.SENSOR_ACTIVITY);
				startCountDown(startTime, duration, Const.SENSOR_TYPE_ACTIVITY);
			} else {
				stopCountDown(Const.SENSOR_TYPE_ACTIVITY);
			}

			boolean stressSensorStatus = !timerDataSource.getTimerStatus(SQLiteHelper.SENSOR_STRESS);
			stressButton.setChecked(stressSensorStatus);
			if (!stressSensorStatus) {
				long startTime = timerDataSource.getStartTime(SQLiteHelper.SENSOR_STRESS);
				long duration = timerDataSource.getDuration(SQLiteHelper.SENSOR_STRESS);
				startCountDown(startTime, duration, Const.SENSOR_TYPE_STRESS);
			} else {
				stopCountDown(Const.SENSOR_TYPE_STRESS);
			}

			boolean conversationSensorStatus = !timerDataSource.getTimerStatus(SQLiteHelper.SENSOR_CONVERSATION);
			conversationButton.setChecked(conversationSensorStatus);
			if (!conversationSensorStatus) {
				long startTime = timerDataSource.getStartTime(SQLiteHelper.SENSOR_CONVERSATION);
				long duration = timerDataSource.getDuration(SQLiteHelper.SENSOR_CONVERSATION);
				startCountDown(startTime, duration, Const.SENSOR_TYPE_CONVERSATION);
			} else {
				stopCountDown(Const.SENSOR_TYPE_CONVERSATION);
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
		if (activityCountdownTimer != null) { 
			activityCountdownTimer.cancel();
		}
		if (stressCountdownTimer != null) { 
			stressCountdownTimer.cancel();
		}
		if (conversationCountdownTimer != null) { 
			conversationCountdownTimer.cancel();
		}
		super.onPause();
	}

	private void stopCountDown(int sensorType) {
		if (sensorType == Const.SENSOR_TYPE_ACTIVITY) {
			if (activityCountdownTimer != null) {
				activityCountdownTimer.cancel();
				activityCountdownTimer = null;
			}
			activityCountdownButton.setVisibility(View.INVISIBLE);
		} else if (sensorType == Const.SENSOR_TYPE_STRESS) {
			if (stressCountdownTimer != null) {
				stressCountdownTimer.cancel();
				stressCountdownTimer = null;
			}
			stressCountdownButton.setVisibility(View.INVISIBLE);
		} else if (sensorType == Const.SENSOR_TYPE_CONVERSATION) {
			if (conversationCountdownTimer != null) {
				conversationCountdownTimer.cancel();
				conversationCountdownTimer = null;
			}
			conversationCountdownButton.setVisibility(View.INVISIBLE);
		}
	}

	private void startCountDown(long startTime, long duration, int sensorType) {
		CountDownTimer countdownTimer;
		final Button countdownButton;

		if (sensorType == Const.SENSOR_TYPE_ACTIVITY) {
			countdownTimer = activityCountdownTimer;
			countdownButton = activityCountdownButton;
		} else if (sensorType == Const.SENSOR_TYPE_STRESS) {
			countdownTimer = stressCountdownTimer;
			countdownButton = stressCountdownButton;
		} else if (sensorType == Const.SENSOR_TYPE_CONVERSATION) {
			countdownTimer = conversationCountdownTimer;
			countdownButton = conversationCountdownButton;
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

		if (sensorType == Const.SENSOR_TYPE_ACTIVITY) {
			activityCountdownTimer = countdownTimer;
		} else if (sensorType == Const.SENSOR_TYPE_STRESS) {
			stressCountdownTimer = countdownTimer;
		} else if (sensorType == Const.SENSOR_TYPE_CONVERSATION) {
			conversationCountdownTimer = countdownTimer;
		} 
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			int requestCode = bundle.getInt(Const.BUNDLE_SENSOR_TYPE);
			if (requestCode == Const.SENSOR_TYPE_ACTIVITY) {
				activityButton.setChecked(true);
				activityCountdownButton.setVisibility(View.INVISIBLE);
				if (activityCountdownTimer != null) {
					activityCountdownTimer.cancel();
					activityCountdownTimer = null;
				}
			} else if (requestCode == Const.SENSOR_TYPE_STRESS) {
				stressButton.setChecked(true);
				stressCountdownButton.setVisibility(View.INVISIBLE);
				if (stressCountdownTimer != null) {
					stressCountdownTimer.cancel();
					stressCountdownTimer = null;
				}
			} else if (requestCode == Const.SENSOR_TYPE_CONVERSATION) {
				conversationButton.setChecked(true);
				conversationCountdownButton.setVisibility(View.INVISIBLE);
				if (conversationCountdownTimer != null) {
					conversationCountdownTimer.cancel();
					conversationCountdownTimer = null;
				}
			}
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ACTIVITY_TIMER_EXTEND:
			return new TimerSetDialog(this, "Select time to add.", onActivityTimerExtendFinishListener);
		case DIALOG_ACTIVITY_TIMER_SETUP:
			return new TimerSetDialog(this, "Select OFF duration.", onActivityTimerSetFinishListener);
		case DIALOG_STRESS_TIMER_EXTEND:
			return new TimerSetDialog(this, "Select time to add.", onStressTimerExtendFinishListener);
		case DIALOG_STRESS_TIMER_SETUP:
			return new TimerSetDialog(this, "Select OFF duration.", onStressTimerSetFinishListener);
		case DIALOG_CONVERSATION_TIMER_EXTEND:
			return new TimerSetDialog(this, "Select time to add.", onConversationTimerExtendFinishListener);
		case DIALOG_CONVERSATION_TIMER_SETUP:
			return new TimerSetDialog(this, "Select OFF duration.", onConversationTimerSetFinishListener);
		}
		return null;
	}

	private OnFinishListener onActivityTimerExtendFinishListener = new OnFinishListener() {

		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				long startTime = timerDataSource.getStartTime(SQLiteHelper.SENSOR_ACTIVITY);
				if (startTime == 0) {
					return;
				}

				long newDuration = timerDataSource.getDuration(SQLiteHelper.SENSOR_ACTIVITY) + duration;
				startCountDown(startTime, newDuration, Const.SENSOR_TYPE_ACTIVITY);

				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_ACTIVITY);
				intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_EXTEND);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
			} 
		}
	};

	private OnFinishListener onActivityTimerSetFinishListener = new OnFinishListener() {

		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_ACTIVITY);
				intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_START);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
				startCountDown(0, duration, Const.SENSOR_TYPE_ACTIVITY);
			} else {
				activityButton.toggle();				
			}
		}
	};

	private OnFinishListener onStressTimerExtendFinishListener = new OnFinishListener() {

		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				long startTime = timerDataSource.getStartTime(SQLiteHelper.SENSOR_STRESS);
				if (startTime == 0) {
					return;
				}

				long newDuration = timerDataSource.getDuration(SQLiteHelper.SENSOR_STRESS) + duration;
				startCountDown(startTime, newDuration, Const.SENSOR_TYPE_STRESS);

				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_STRESS);
				intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_EXTEND);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
			} 
		}
	};

	private OnFinishListener onStressTimerSetFinishListener = new OnFinishListener() {

		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_STRESS);
				intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_START);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
				startCountDown(0, duration, Const.SENSOR_TYPE_STRESS);
			} else {
				stressButton.toggle();				
			}
		}
	};

	private OnFinishListener onConversationTimerExtendFinishListener = new OnFinishListener() {

		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				long startTime = timerDataSource.getStartTime(SQLiteHelper.SENSOR_CONVERSATION);
				if (startTime == 0) {
					return;
				}

				long newDuration = timerDataSource.getDuration(SQLiteHelper.SENSOR_CONVERSATION) + duration;
				startCountDown(startTime, newDuration, Const.SENSOR_TYPE_CONVERSATION);

				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_CONVERSATION);
				intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_EXTEND);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
			} 
		}
	};

	private OnFinishListener onConversationTimerSetFinishListener = new OnFinishListener() {

		@Override
		public void onFinish(int result, long duration) {
			if (result == 1) {
				Intent intent = new Intent(context, TimerService.class);
				intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_CONVERSATION);
				intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_START);
				intent.putExtra(Const.BUNDLE_DURATION, duration);
				startService(intent);
				startCountDown(0, duration, Const.SENSOR_TYPE_CONVERSATION);
			} else {
				conversationButton.toggle();				
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.inference_control_menu, menu);
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
		case R.id.all_sensors:
			intent = new Intent(this, OnOffAllControlActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onToggleActivityClicked(View view) {
		boolean activityStatus = activityButton.isChecked();

		if (!activityStatus) {
			showDialog(DIALOG_ACTIVITY_TIMER_SETUP);
		} else {
			Intent intent = new Intent(this, TimerService.class);
			intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_ACTIVITY);
			intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_STOP_BY_USER);
			startService(intent);
			stopCountDown(Const.SENSOR_TYPE_ACTIVITY);
		}
	}

	public void onToggleStressClicked(View view) {
		boolean stressStatus = stressButton.isChecked();

		if (!stressStatus) {
			showDialog(DIALOG_STRESS_TIMER_SETUP);
		} else {
			Intent intent = new Intent(this, TimerService.class);
			intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_STRESS);
			intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_STOP_BY_USER);
			startService(intent);
			stopCountDown(Const.SENSOR_TYPE_STRESS);
		}
	}

	public void onToggleConversationClicked(View view) {
		boolean conversationStatus = conversationButton.isChecked();

		if (!conversationStatus) {
			showDialog(DIALOG_CONVERSATION_TIMER_SETUP);
		} else {
			Intent intent = new Intent(this, TimerService.class);
			intent.putExtra(Const.BUNDLE_SENSOR_TYPE, Const.SENSOR_TYPE_CONVERSATION);
			intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_STOP_BY_USER);
			startService(intent);
			stopCountDown(Const.SENSOR_TYPE_CONVERSATION);
		}
	}
}
