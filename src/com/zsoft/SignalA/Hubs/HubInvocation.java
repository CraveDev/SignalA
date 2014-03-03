package com.zsoft.SignalA.Hubs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HubInvocation
{

	private String mHubName;
	private String mMethod;
	private String mCallbackId;
	private JSONArray mArgs;

	public HubInvocation(String hubName, String method, JSONArray args, String callbackId)
	{
		this.mHubName = hubName;
		this.mMethod = method;
		this.mArgs = args;
		this.mCallbackId = callbackId;

	}

	public String getHubName()
	{
		return this.mHubName;
	}

	public String getMethod()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public JSONObject getArgs()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String Serialize()
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put("I", this.mCallbackId);
			json.put("M", this.mMethod);
			json.put("A", this.mArgs);
			json.put("H", this.mHubName);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		return json.toString();
	}

}
