package com.zsoft.SignalA.Hubs;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.OperationApplicationException;

import com.zsoft.SignalA.ConnectionState;
import com.zsoft.SignalA.LogLevel;
import com.zsoft.SignalA.Transport.ITransport;

public class HubConnection extends com.zsoft.SignalA.ConnectionBase
{
	private final Map<String, HubProxy> mHubs = new HashMap<String, HubProxy>();
	private final Map<String, HubInvokeCallback> mCallbacks = new HashMap<String, HubInvokeCallback>();
	private int mCallbackId = 0;

	public HubConnection(String url, Context context, ITransport transport)
	{
		super(url, context, transport);
		setUrl(GetUrl(url, true));
	}

	@Override
	public void setMessage(JSONObject message)
	{
		String info = message.optString("I", null);

		if (info != null)
		{
			HubResult result = new HubResult(message);
			HubInvokeCallback callback = null;

			if (!result.getId().equals("-1"))
			{
				synchronized (this.mCallbacks)
				{
					if (this.mCallbacks.containsKey(result.getId()))
					{
						callback = this.mCallbacks.remove(result.getId());
					}
					else
					{
						OnLog(LogLevel.Warning, "Callback with id %s not found!", result.getId());
					}
				}
			}

			if (callback != null)
			{
				try
				{
					callback.OnResult(true, result.getResult());
				}
				catch (Exception ex)
				{
					OnLog(LogLevel.Error, "Exception in callback: %s", ex);
				}
			}
		}
		else
		{
			HubInvocationMessage invokeMessage = new HubInvocationMessage(message);
			HubProxy hubProxy;
			if (this.mHubs.containsKey(invokeMessage.getHubName()))
			{
				/*
				 * ToDo. Handle state if (invocation.State != null) { foreach
				 * (var state in invocation.State) { hubProxy[state.Key] =
				 * state.Value; } }
				 */
				hubProxy = this.mHubs.get(invokeMessage.getHubName());
				hubProxy.InvokeEvent(invokeMessage.getMethod(), invokeMessage.getArgs());
			}

			super.setMessage(message);
		}

	}

	public IHubProxy CreateHubProxy(String hubName) throws OperationApplicationException
	{
		if (getCurrentState().getState() != ConnectionState.Disconnected)
		{
			throw new OperationApplicationException("Proxies cannot be added when connection is started");
		}

		hubName = hubName.toLowerCase();
		HubProxy hubProxy;
		if (this.mHubs.containsKey(hubName))
			hubProxy = this.mHubs.get(hubName);
		else
		{
			hubProxy = new HubProxy(this, hubName);
			this.mHubs.put(hubName, hubProxy);
		}
		return hubProxy;
	}

	public String RegisterCallback(HubInvokeCallback callback)
	{
		if (callback == null)
			return "-1";

		synchronized (this.mCallbacks)
		{
			String id = Integer.toString(this.mCallbackId);
			this.mCallbacks.put(id, callback);
			this.mCallbackId++;
			return id;
		}
	}

	public boolean RemoveCallback(final String callbackId)
	{
		if (callbackId.equals("-1"))
			return true;

		synchronized (this.mCallbacks)
		{
			if (this.mCallbacks.containsKey(callbackId))
			{
				this.mCallbacks.remove(callbackId);
				return true;
			}
			else
			{

				OnLog(LogLevel.Warning, "Callback with id %s not found!", callbackId);
			}
		}
		return false;
	}

	public String GetUrl(String url, boolean useDefaultUrl)
	{
		if (!url.endsWith("/"))
		{
			url += "/";
		}

		if (useDefaultUrl)
		{
			return url + "signalr";
		}

		return url;

	}

	// return the connectiondata....[{"Name":"chat"}]
	@Override
	public String OnSending()
	{
		JSONArray arr = new JSONArray();
		for (Entry<String, HubProxy> entry : this.mHubs.entrySet())
		{
			JSONObject jsonObj = new JSONObject();
			try
			{
				jsonObj.put("name", entry.getKey());
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			arr.put(jsonObj);
		}

		return arr.toString();
	}
}
