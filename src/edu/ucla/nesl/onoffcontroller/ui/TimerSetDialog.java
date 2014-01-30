package edu.ucla.nesl.onoffcontroller.ui;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TimePicker;
import edu.ucla.nesl.onoffcontroller.R;
import edu.ucla.nesl.onoffcontroller.tools.Tools;

public class TimerSetDialog extends Dialog {

	private Context context;
	private Dialog dialog = this;

	private OnFinishListener onFinishListener;

	private RadioGroup radioGroup;
	private RadioButton customTimeButton;
	private long duration;

	private String title;
	
	public interface OnFinishListener {
		public void onFinish(int result, long duration);
	}

	public TimerSetDialog(Context context, String title, OnFinishListener onFinishListener) {
		super(context);
		this.context = context;
		this.onFinishListener = onFinishListener;
		this.title = title;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_timer_set);

		setCancelable(false);
		setTitle(title);

		Button doneButton = (Button) findViewById(R.id.timer_set_done);
		doneButton.setOnClickListener(onClickDoneButtonListener);

		Button cancelButton = (Button) findViewById(R.id.timer_set_cancel);
		cancelButton.setOnClickListener(onClickCancelButtonListener);

		radioGroup = (RadioGroup)findViewById(R.id.radio_group);
		radioGroup.setOnCheckedChangeListener(onCheckedChangeListener);
		radioGroup.check(R.id.radio_30_min);
		duration = 30 * 60;

		customTimeButton = (RadioButton)findViewById(R.id.radio_custom);
		customTimeButton.setOnClickListener(customTimeOnClickListener);
	}

	private android.view.View.OnClickListener customTimeOnClickListener = new android.view.View.OnClickListener() {

		@Override
		public void onClick(View arg0) {
			CustomTitleTimePickerDialog timeDialog = new CustomTitleTimePickerDialog(context, timePickerListener, (int)(duration/3600), (int)((duration%3600)/60), true);
			timeDialog.show();
		}
	};

	private android.view.View.OnClickListener onClickDoneButtonListener = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View v) {
			dialog.dismiss();
			onFinishListener.onFinish(1, duration);
		}
	};

	private android.view.View.OnClickListener onClickCancelButtonListener = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View v) {
			dialog.dismiss();
			onFinishListener.onFinish(0, -1);
		}
	};

	private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			switch(checkedId) {
			case R.id.radio_10_min:
				//duration = 10*60;
				duration = 5;
				break;
			case R.id.radio_30_min:
				//duration = 30*60;
				duration = 10;
				break;
			case R.id.radio_1_hour:
				//duration = 60*60;
				duration = 15;
				break;
			case R.id.radio_2_hours:
				//duration = 120*60;
				duration = 20;
				break;			
			}
		}
	};	

	private TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
			duration = selectedHour * 3600 + selectedMinute * 60;
			if (duration == 0) {
				Tools.showAlertDialog(context, "Error", "Please enter a valid duration.", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						CustomTitleTimePickerDialog timeDialog = new CustomTitleTimePickerDialog(context, timePickerListener, (int)(duration/3600), (int)((duration%3600)/60), true);
						timeDialog.show();
					}
				});
			}
		}
	};
}
