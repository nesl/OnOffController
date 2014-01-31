package edu.ucla.nesl.onoffcontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BroadcastEventReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		
		if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
			// Start timer initial clean up 
			Intent i = new Intent(context, TimerService.class);
			i.putExtra(Const.BUNDLE_TIMER_OPERATION, Const.TIMER_INIT);
			context.startService(i);
		} else {
			SyncService.startSyncService(context);
		}
	}
}
