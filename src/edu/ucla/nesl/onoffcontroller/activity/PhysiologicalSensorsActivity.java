package edu.ucla.nesl.onoffcontroller.activity;

import edu.ucla.nesl.onoffcontroller.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class PhysiologicalSensorsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_physiological_sensors);
	}

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

	}

	public void onToggleRespirationClicked(View view) {

	}
}
