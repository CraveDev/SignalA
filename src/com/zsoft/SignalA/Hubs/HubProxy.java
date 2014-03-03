package com.zsoft.SignalA.Hubs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;

import com.zsoft.SignalA.LogLevel;
import com.zsoft.SignalA.SendCallback;

public class HubProxy implements IHubProxy
{
	protected static final String TAG = "HubProxy";
	private String mHubName;
	private HubConnection mConnection;
	// private Map<string, JSONObject> _state = new Dictionary<string,
	// JToken>(StringComparer.OrdinalIgnoreCase);
	private Map<String, HubOnDataCallback> mSubscriptions = new HashMap<String, HubOnDataCallback>();

	public HubProxy(HubConnection hubConnection, String hubName)
	{
		this.mConnection = hubConnection;
		this.mHubName = hubName;
	}

	public void Invoke(final String method, Collection<?> args,
			HubInvokeCallback callback)
	{

		Invoke(method, new JSONArray(args), callback);
	}

	// Executes a method on the server asynchronously
	@Override
	public void Invoke(final String method, JSONArray args, HubInvokeCallback callback)
	{

		if (method == null)
		{
			throw new IllegalArgumentException("method");
		}

		if (args == null)
		{
			throw new IllegalArgumentException("args");
		}

		final String callbackId = this.mConnection.RegisterCallback(callback);

		HubInvocation hubData = new HubInvocation(this.mHubName, method, args, callbackId);

		String value = hubData.Serialize();

		this.mConnection.Send(value, new SendCallback()
		{
			@Override
			public void OnSent(CharSequence messageSent)
			{
				HubProxy.this.mConnection.OnLog(LogLevel.Debug, "Invoke of %s send to %s", method, HubProxy.this.mHubName);
			}

			@Override
			public void OnError(Exception ex)
			{
				if (HubProxy.this.mConnection != null)
				{
					HubProxy.this.mConnection.OnLog(LogLevel.Debug, "Failed to invoke %s send to %s", method, HubProxy.this.mHubName);
					HubProxy.this.mConnection.RemoveCallback(callbackId);
				}
			}
		});
	}

	public void On(String eventName, HubOnDataCallback callback)
	{
		Subscribe(eventName, callback);
	}

	public void Subscribe(String eventName, HubOnDataCallback callback)
	{
		if (eventName == null)
			throw new IllegalArgumentException("eventName can not be null");
		if (callback == null)
			throw new IllegalArgumentException("callback can not be null");

		eventName = eventName.toLowerCase(Locale.US);

		if (!this.mSubscriptions.containsKey(eventName))
		{
			this.mSubscriptions.put(eventName, callback);
		}
	}

	// Run event locally that is called from the server and registered in ON
	// method.
	public void InvokeEvent(String eventName, JSONArray args)
	{
		HubOnDataCallback subscription;
		if (this.mSubscriptions.containsKey(eventName))
		{
			subscription = this.mSubscriptions.get(eventName);
			subscription.OnReceived(args);
		}
	}

	public void dispose()
	{
		this.mSubscriptions.clear();

		this.mConnection = null;
	}

}
