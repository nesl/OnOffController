package edu.ucla.nesl.onoffcontroller.db;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Rule {

	private long id;
	private String sensor;
	private long startTime;
	private long endTime;
	
	public Rule(long id, String sensor, long startTime, long endTime) {
		this.id = id;
		this.sensor = sensor;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		try {
			json.put("id", id);
			json.put("action", "deny");
			if (!sensor.equals(SQLiteHelper.SENSOR_ALL)) {
				JSONArray arr = new JSONArray();
				arr.put(sensor);
				json.put("target_streams", arr);
			}
			json.put("condition", "timestamp BETWEEN '" + getDateStr(startTime) + "' AND '" + getDateStr(endTime) + "'");
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		return json;
	}
	
	private String getDateStr(long time) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		return sdf.format(new Date(time * 1000));
	}

	public String toJsonString() {
		return toJson().toString();
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getSensor() {
		return sensor;
	}
	public void setSensor(String sensor) {
		this.sensor = sensor;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

}
