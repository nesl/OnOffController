package edu.ucla.nesl.onoffcontroller.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import edu.ucla.nesl.onoffcontroller.R;

public class LocationAccelSensorsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location_accel_sensors);
	}
	
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
		
	}

	public void onToggleAccelerometerClicked(View view) {
		
	}
}
