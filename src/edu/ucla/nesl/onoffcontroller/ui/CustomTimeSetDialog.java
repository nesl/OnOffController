package edu.ucla.nesl.onoffcontroller.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import edu.ucla.nesl.onoffcontroller.R;
import edu.ucla.nesl.onoffcontroller.tools.Tools;

public class CustomTimeSetDialog extends Dialog {

	private Context context;
	private Dialog dialog = this;

	private OnFinishListener onFinishListener;

	private long duration;
	
	private EditText editHours;
	private EditText editMins;
	
	public interface OnFinishListener {
		public void onFinish(long duration);
	}

	public CustomTimeSetDialog(Context context, OnFinishListener onFinishListener) {
		super(context);
		this.context = context;
		this.onFinishListener = onFinishListener;		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_custom_time_set);

		setCancelable(false);
		setTitle("Enter OFF duration");

		Button doneButton = (Button) findViewById(R.id.custom_time_set_done);
		doneButton.setOnClickListener(onClickDoneButtonListener);
		
		editHours = (EditText) findViewById(R.id.timer_hour);
		editMins = (EditText) findViewById(R.id.timer_min);
		
		duration = 30*60;
	}

	private android.view.View.OnClickListener onClickDoneButtonListener = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View v) {
			
			int hours;
			String hoursStr = editHours.getText().toString();
			if (hoursStr.length() == 0) {
				hours = 0;
			} else {
				hours = Integer.valueOf(hoursStr);
			}
			
			int mins;
			String minsStr = editMins.getText().toString();
			if (minsStr.length() == 0) {
				mins = 0;
			} else {
				mins = Integer.valueOf(minsStr);
			}
			
			duration = hours * 3600 + mins * 60;
			
			if (duration <= 0) {
				Tools.showAlertDialog(context, "Error", "Please enter a valid duration.");
				return;
			}
			
			dialog.dismiss();
			onFinishListener.onFinish(duration);
		}
	};
}
