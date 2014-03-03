package com.zsoft.SignalA.Transport.longpolling;

import java.util.Map;

import org.json.JSONObject;

import android.util.Log;

import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;
import com.zsoft.SignalA.ConnectionBase;
import com.zsoft.SignalA.ConnectionState;
import com.zsoft.SignalA.SendCallback;
import com.zsoft.SignalA.SignalAUtils;
import com.zsoft.SignalA.Transport.ProcessResult;
import com.zsoft.SignalA.Transport.TransportHelper;
import com.zsoft.parallelhttpclient.ParallelHttpClient;

public class ConnectedState extends StopableStateWithCallback
{
	protected static final String TAG = "ConnectedState";

	public ConnectedState(ConnectionBase connection)
	{
		super(connection);
	}

	@Override
	public ConnectionState getState()
	{
		return ConnectionState.Connected;
	}

	@Override
	public void Start()
	{
	}

	@Override
	public void Stop()
	{
		this.mConnection.SetNewState(new DisconnectingState(this.mConnection));
		super.Stop();
	}

	@Override
	public void Send(final CharSequence text, final SendCallback sendCb)
	{
		if (DoStop())
		{
			sendCb.OnError(new Exception("Connection is about to close"));
			return;
		}

		String url = SignalAUtils.EnsureEndsWith(this.mConnection.getUrl(), "/") + "send";
		String connectionData = this.mConnection.OnSending();
		url += TransportHelper.GetSendQueryString(this.mConnection, connectionData, TRANSPORT_NAME);
		TransportHelper.AppendCustomQueryString(this.mConnection, url);

		AsyncCallback cb = new AsyncCallback()
		{
			@Override
			public void onComplete(HttpResponse httpResponse)
			{
				if (httpResponse.getStatus() == 200)
				{
					Log.d(TAG, "Message sent: " + text);

					sendCb.OnSent(text);

					JSONObject json = JSONHelper.ToJSONObject(httpResponse.getBodyAsString());

					if (json != null && json.length() > 0)
					{
						ConnectedState.this.mConnection.setMessage(json);
					}
				}
				else
				{
					Exception ex = new Exception("Error sending message");
					ConnectedState.this.mConnection.setException(ex);
					sendCb.OnError(ex);
				}
			}

			@Override
			public void onError(Exception ex)
			{
				ConnectedState.this.mConnection.setException(ex);
				sendCb.OnError(ex);
			}
		};

		ParallelHttpClient httpClient = new ParallelHttpClient();
		for (Map.Entry<String, String> entry : this.mConnection.getHeaders().entrySet())
		{
			httpClient.addHeader(entry.getKey(), entry.getValue());
		}
		ParameterMap params = httpClient.newParams().add("data", text.toString());
		httpClient.setMaxRetries(1);
		httpClient.post(url, params, cb);
	}

	@Override
	protected void OnRun()
	{
		if (DoStop())
			return;

		String url = SignalAUtils.EnsureEndsWith(this.mConnection.getUrl(), "/") + "poll";
		String connectionData = this.mConnection.OnSending();
		url += TransportHelper.GetReceiveQueryString(this.mConnection, connectionData, TRANSPORT_NAME);

		AsyncCallback cb = new AsyncCallback()
		{
			@Override
			public void onComplete(HttpResponse httpResponse)
			{
				if (DoStop())
					return;

				try
				{
					if (httpResponse != null && httpResponse.getStatus() == 200)
					{
						JSONObject json = JSONHelper.ToJSONObject(httpResponse.getBodyAsString());
						if (json != null)
						{
							ProcessResult result = TransportHelper.ProcessResponse(ConnectedState.this.mConnection, json);

							if (result.processingFailed)
							{
								ConnectedState.this.mConnection.setException(new Exception("Error while processing response."));
								ConnectedState.this.mConnection.SetNewState(new ReconnectingState(ConnectedState.this.mConnection));
							}
							else if (result.disconnected)
							{
								ConnectedState.this.mConnection.SetNewState(new DisconnectedState(ConnectedState.this.mConnection));
								return;
							}
						}
						else
						{
							ConnectedState.this.mConnection.setException(new Exception("Error when calling endpoint. Returncode: " + httpResponse.getStatus()));
							ConnectedState.this.mConnection.SetNewState(new ReconnectingState(ConnectedState.this.mConnection));
						}
					}
					else
					{
						ConnectedState.this.mConnection.setException(new Exception("Error when calling endpoint."));
						ConnectedState.this.mConnection.SetNewState(new ReconnectingState(ConnectedState.this.mConnection));
					}
				}
				finally
				{
					ConnectedState.this.mIsRunning.set(false);

					// Loop if we are still connected
					if (ConnectedState.this.mConnection.getCurrentState() == ConnectedState.this)
						Run();
				}
			}

			@Override
			public void onError(Exception ex)
			{
				ConnectedState.this.mConnection.setException(ex);
				ConnectedState.this.mConnection.SetNewState(new ReconnectingState(ConnectedState.this.mConnection));
			}
		};

		ParallelHttpClient httpClient = new ParallelHttpClient();
		httpClient.setMaxRetries(1);
		httpClient.setConnectionTimeout(5000);
		httpClient.setReadTimeout(115000);
		for (Map.Entry<String, String> entry : this.mConnection.getHeaders().entrySet())
		{
			httpClient.addHeader(entry.getKey(), entry.getValue());
		}
		ParameterMap params = httpClient.newParams();
		httpClient.post(url, params, cb);
	}

}
