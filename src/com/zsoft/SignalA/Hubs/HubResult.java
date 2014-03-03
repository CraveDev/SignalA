package com.zsoft.SignalA.Hubs;

import org.json.JSONObject;

public class HubResult
{
	private String mId;
	private String mResult;

	public HubResult(JSONObject message)
	{
		this.mId = message.optString("I");
		this.mResult = message.optString("R");
	}

	public String getId()
	{
		return this.mId;
	}

	public String getResult()
	{
		return this.mResult;
	}
}
