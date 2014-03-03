package com.zsoft.SignalA.Hubs;

import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

public class HubInvocationMessage
{
	private String mHubName;
	private String mMethod;
	private JSONArray mArgs;

	public HubInvocationMessage(JSONObject message)
	{
		this.mHubName = message.optString("H");
		this.mHubName = this.mHubName.toLowerCase(Locale.US);
		this.mMethod = message.optString("M");
		this.mMethod = this.mMethod.toLowerCase(Locale.US);
		this.mArgs = message.optJSONArray("A");
		// [{"H":"CalculatorHub","M":"newCalculation","A":["4/7/2013 12:42:23 PM : 10 + 5 = 15"]}]}" +
	}

	public String getHubName()
	{
		return this.mHubName;
	}

	public JSONArray getArgs()
	{
		return this.mArgs;
	}

	public String getMethod()
	{
		return this.mMethod;
	}

}
