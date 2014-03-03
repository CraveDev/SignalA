package com.zsoft.SignalA.Transport.longpolling;

import java.util.Map;

import org.json.JSONObject;

import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.zsoft.SignalA.ConnectionBase;
import com.zsoft.SignalA.ConnectionState;
import com.zsoft.SignalA.SendCallback;
import com.zsoft.SignalA.SignalAUtils;
import com.zsoft.SignalA.Transport.ProcessResult;
import com.zsoft.SignalA.Transport.TransportHelper;
import com.zsoft.parallelhttpclient.ParallelHttpClient;

public class ConnectingState extends StopableStateWithCallback
{
	public ConnectingState(ConnectionBase connection)
	{
		super(connection);
	}

	@Override
	public ConnectionState getState()
	{
		return ConnectionState.Connecting;
	}

	@Override
	public void Start()
	{
	}

	@Override
	public void Send(CharSequence text, SendCallback callback)
	{
		callback.OnError(new Exception("Not connected"));
	}

	@Override
	protected void OnRun()
	{
		if (DoStop())
			return;

		// connect
		String url = SignalAUtils.EnsureEndsWith(this.mConnection.getUrl(), "/") + "connect";
		String connectionData = this.mConnection.OnSending();
		url += TransportHelper.GetReceiveQueryString(this.mConnection, connectionData, TRANSPORT_NAME);
		TransportHelper.AppendCustomQueryString(this.mConnection, url);

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
							ProcessResult result = TransportHelper.ProcessResponse(ConnectingState.this.mConnection, json);

							if (result.processingFailed)
							{
								ConnectingState.this.mConnection.setException(new Exception("Error while processing response."));
								ConnectingState.this.mConnection.SetNewState(new ReconnectingState(ConnectingState.this.mConnection));
							}
							else if (result.disconnected)
							{
								ConnectingState.this.mConnection.setException(new Exception("Disconnected by server."));
								ConnectingState.this.mConnection.SetNewState(new DisconnectedState(ConnectingState.this.mConnection));
							}
							else if (result.initialized)
							{
								ConnectingState.this.mConnection.SetNewState(new ConnectedState(ConnectingState.this.mConnection));
							}
							else
							{
								ConnectingState.this.mConnection.SetNewState(new DisconnectedState(ConnectingState.this.mConnection));
							}
						}
						else
						{
							ConnectingState.this.mConnection.setException(new Exception("Error when calling endpoint. Return code: " + httpResponse.getStatus()));
							ConnectingState.this.mConnection.SetNewState(new DisconnectedState(ConnectingState.this.mConnection));
						}
					}
					else
					{
						ConnectingState.this.mConnection.setException(new Exception("Error when calling endpoint."));
						ConnectingState.this.mConnection.SetNewState(new DisconnectedState(ConnectingState.this.mConnection));
					}
				}
				finally
				{
					ConnectingState.this.mIsRunning.set(false);
				}
			}

			@Override
			public void onError(Exception ex)
			{
				ConnectingState.this.mConnection.setException(ex);
				ConnectingState.this.mConnection.SetNewState(new DisconnectedState(ConnectingState.this.mConnection));
			}
		};

		synchronized (this.mCallbackLock)
		{
			// mCurrentCallback = cb;
		}

		ParallelHttpClient httpClient = new ParallelHttpClient();
		httpClient.setMaxRetries(1);
		for (Map.Entry<String, String> entry : this.mConnection.getHeaders().entrySet())
		{
			httpClient.addHeader(entry.getKey(), entry.getValue());
		}
		httpClient.get(url, null, cb);
	}

}
