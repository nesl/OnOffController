package edu.ucla.nesl.onoffcontroller;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import edu.ucla.nesl.onoffcontroller.db.DataSource;
import edu.ucla.nesl.onoffcontroller.model.Rule;
import edu.ucla.nesl.onoffcontroller.tools.MySSLSocketFactory;
import edu.ucla.nesl.onoffcontroller.tools.NetworkUtils;
import edu.ucla.nesl.onoffcontroller.ui.Base64;

public class SyncService extends IntentService {

	private static final String PORT = "8443";
	private static int SERVICE_RESTART_INTERVAL = 5 * 60; // seconds

	private String serverip;
	private String username;
	private String password;

	private PowerManager.WakeLock mWakeLock;

	private Handler handler;

	public SyncService() {
		super("SyncService");
	}

	@Override
	public void onCreate() {
		setIntentRedelivery(true);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
		mWakeLock.setReferenceCounted(false);

		handler = new Handler();

		super.onCreate();
	}

	public static void startSyncService(Context context) {
		// Start upload service
		Intent i = new Intent(context, SyncService.class);
		context.startService(i); 
	}

	private void postToast(final String msg) {
		handler.post(new Runnable() {            
			@Override
			public void run() {
				Toast.makeText(SyncService.this, msg, Toast.LENGTH_LONG).show();                
			}
		});
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		CharSequence text = getText(R.string.foreground_service_started);
		Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getBroadcast(this, 0, new Intent(), 0);
		notification.setLatestEventInfo(this, text, text, contentIntent);
		startForeground(R.string.foreground_service_started, notification);

		acquireWakeLock();

		Context context = getApplicationContext();
		int status = NetworkUtils.getConnectivityStatus(context);

		if (status == NetworkUtils.TYPE_WIFI) {
			SharedPreferences settings = context.getSharedPreferences(Const.PREFS_NAME, 0);
			serverip = settings.getString(Const.PREFS_SERVER_IP, null);
			username = settings.getString(Const.PREFS_USERNAME, null);
			password = settings.getString(Const.PREFS_PASSWORD, null);

			if (serverip != null && username != null && password != null) {
				try {
					startUploadRules();
					cancelNotification();
					cancelServiceSchedule();
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					createNotification("Server Authentication Problem.");
				} catch (IOException e) {
					e.printStackTrace();
					createNotification("Server Connection Problem.");
					// Schedule next check.
					if (NetworkUtils.getConnectivityStatus(context) == NetworkUtils.TYPE_WIFI 
							&& !isServiceScheduled()) {
						scheduleStartService();
					}
				} catch (JSONException e) {
					e.printStackTrace();
					createNotification("JSON Exception.");
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					createNotification("Server Response Problem: " + e);
				}
			}
		} else {
			cancelNotification();
			cancelServiceSchedule();
		}

		releaseWakeLock();

		stopForeground(true);
	}

	private void cancelNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(0);
	}

	private void createNotification(String message) {
		PendingIntent pintent = PendingIntent.getActivity(
				getApplicationContext(),
				0,
				new Intent(),
				PendingIntent.FLAG_UPDATE_CURRENT);

		Notification noti = new NotificationCompat.Builder(this)
		.setContentTitle("On/OffController Error")
		.setContentText(message)
		.setContentIntent(pintent)
		.setSmallIcon(R.drawable.ic_launcher)
		.build();

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		noti.flags |= Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(0, noti);
	}

	private boolean isServiceScheduled() {
		return PendingIntent.getBroadcast(this, 0, new Intent(this, SyncService.class), PendingIntent.FLAG_NO_CREATE) != null;
	}

	private void scheduleStartService() {
		Calendar cal = Calendar.getInstance();

		Intent intent = new Intent(this, SyncService.class);
		PendingIntent pintent = PendingIntent.getService(this, 0, intent, 0);

		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarm.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis() + SERVICE_RESTART_INTERVAL*1000, pintent);
	}

	private void cancelServiceSchedule() {
		Intent intent = new Intent(this, SyncService.class);
		PendingIntent pintent = PendingIntent.getService(this, 0, intent, 0);
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pintent);
	}

	private HttpClient getNewHttpClient() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

			return new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			return new DefaultHttpClient();
		}
	}

	private void uploadJson(String apiend, JSONObject json) throws ClientProtocolException, IOException, IllegalAccessException {
		final String url = "https://" + serverip + ":" + PORT + "/api/" + apiend;

		HttpClient httpClient = getNewHttpClient();
		HttpPost httpPost = new HttpPost(url);

		// Add authorization
		httpPost.setHeader("Authorization", "basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));

		httpPost.setHeader("Content-Type", "application/json");		
		try {
			httpPost.setEntity(new StringEntity(json.toString()));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return;
		}

		HttpResponse response = httpClient.execute(httpPost);
		InputStream is = response.getEntity().getContent();
		long length = response.getEntity().getContentLength();
		byte[] buffer = new byte[(int)length];
		is.read(buffer);
		String content = new String(buffer);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new IllegalAccessException("HTTP Server Error: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase() + "(" + content + ")");			}

		Log.i(Const.TAG, "Server Content: " + content);
	}

	private void startUploadRules() throws ClientProtocolException, IOException, JSONException, IllegalAccessException {
		DataSource tds = new DataSource(getApplicationContext());
		try {
			tds.open();
			List<Rule> rules = tds.getNotUploadedRules();
			for (Rule rule : rules) {
				JSONObject json = rule.toJson();
				json.remove("id");
				uploadRule(json);
				tds.markRuleUploaded(rule.getId());
			} 
		} finally {
			tds.close();
		}
	}

	private void uploadRule(JSONObject json) throws ClientProtocolException, IOException, IllegalAccessException {
		uploadJson("rules", json);
	}

	private void acquireWakeLock() {
		if (!mWakeLock.isHeld()) {
			mWakeLock.acquire();
		}
	}

	private void releaseWakeLock() {
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}
}
