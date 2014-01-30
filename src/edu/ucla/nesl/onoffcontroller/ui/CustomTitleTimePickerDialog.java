package edu.ucla.nesl.onoffcontroller.ui;

import java.util.Calendar;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.widget.TimePicker;

public class CustomTitleTimePickerDialog extends TimePickerDialog {

	boolean mIs24HourView;
	private final Calendar mCalendar;
	private final java.text.DateFormat mDateFormat;

	public CustomTitleTimePickerDialog(Context context, OnTimeSetListener callBack, int hourOfDay, int minute, boolean is24HourView) {
		super(context, callBack, hourOfDay, minute, is24HourView);

		mIs24HourView = is24HourView;

		mDateFormat = DateFormat.getTimeFormat(context);
		mCalendar = Calendar.getInstance();
		updateTitle(hourOfDay, minute);
	}

	@Override
	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {			
		updateTitle(hourOfDay, minute);
	}

	private void updateTitle(int hour, int minute) {
		if (mIs24HourView) {
			String hourStr;
			if (hour > 1) {
				hourStr = " hours ";
			} else {
				hourStr = " hour ";
			}
			String minuteStr;
			if (minute > 1) {
				minuteStr = " minutes";
			} else {
				minuteStr = " minute";
			}
			setTitle(hour + hourStr + minute + minuteStr);
		}
		else {
			mCalendar.set(Calendar.HOUR_OF_DAY, hour);
			mCalendar.set(Calendar.MINUTE, minute);
			setTitle(mDateFormat.format(mCalendar.getTime()));
		}
	}
}

