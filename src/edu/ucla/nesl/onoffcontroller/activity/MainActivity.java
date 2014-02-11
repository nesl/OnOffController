package edu.ucla.nesl.onoffcontroller.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import edu.ucla.nesl.onoffcontroller.Const;
import edu.ucla.nesl.onoffcontroller.R;
import edu.ucla.nesl.onoffcontroller.TimerService;
import edu.ucla.nesl.onoffcontroller.tools.Tools;
import edu.ucla.nesl.onoffcontroller.ui.SetupUserDialog;
import edu.ucla.nesl.onoffcontroller.ui.SetupUserDialog.OnFinishListener;

public class MainActivity extends Activity {

	private final int DIALOG_SETUP_USERNAME_PASSWORD = 1;
	private final Context context = this;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		SharedPreferences settings = getSharedPreferences(Const.PREFS_NAME, 0);
		boolean isFirst = settings.getBoolean(Const.PREFS_IS_FIRST, true);

		if (isFirst) {
			// Start initial setup process
			Tools.showAlertDialog(this, "Welcome", "Welcome to On/Off Controller! You've launched On/Off Controller for the first time, so let's go through inital setup process.", welcomeListener);
		} else {
			// Start timer initial clean up 
			Intent intent = new Intent(this, TimerService.class);
			intent.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_INIT);
			startService(intent);

			// Start login activity
			intent = new Intent(this, LoginActivity.class);
			startActivityForResult(intent, Const.REQUEST_CODE_NORMAL);			
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case Const.REQUEST_CODE_NORMAL:
			finish();
			break;
		}
	}

	private OnClickListener welcomeListener = new OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
			showDialog(DIALOG_SETUP_USERNAME_PASSWORD);
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_SETUP_USERNAME_PASSWORD:
			return new SetupUserDialog(this, onFinishListener);
		}
		return null;
	}

	private OnFinishListener onFinishListener = new OnFinishListener() {
		@Override
		public void onFinish() {
			Tools.showAlertDialog(context, "Congratulations!", "Now you're ready to use On/Off Controller. Please login in the following screen.", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					
					SharedPreferences settings = context.getSharedPreferences(Const.PREFS_NAME, 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean(Const.PREFS_IS_FIRST, false);
					editor.commit();
					
					Intent intent = new Intent(context, LoginActivity.class);
					startActivityForResult(intent, Const.REQUEST_CODE_NORMAL);
				}
			});
		}
	};
}
