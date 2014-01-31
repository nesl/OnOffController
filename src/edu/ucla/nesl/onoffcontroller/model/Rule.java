package edu.ucla.nesl.onoffcontroller.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ucla.nesl.onoffcontroller.Const;
import edu.ucla.nesl.onoffcontroller.db.SQLiteHelper;

public class Rule {

	private long id;
	private String sensor;
	private long startTime;
	private long endTime;
	private String tags;
	
	public Rule(long id, String sensor, long startTime, long endTime) {
		this.id = id;
		this.sensor = sensor;
		this.startTime = startTime;
		this.endTime = endTime;
		this.tags = Const.RULE_TAG;
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
			json.put("tags", tags);
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

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}
}
