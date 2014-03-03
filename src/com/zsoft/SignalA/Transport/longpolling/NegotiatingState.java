package com.zsoft.SignalA.Transport.longpolling;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.zsoft.SignalA.ConnectionBase;
import com.zsoft.SignalA.ConnectionState;
import com.zsoft.SignalA.SendCallback;
import com.zsoft.SignalA.SignalAUtils;
import com.zsoft.SignalA.Transport.TransportHelper;
import com.zsoft.parallelhttpclient.ParallelHttpClient;

/**
 * Created by Erik on 2014-02-20.
 */
public class NegotiatingState extends StopableStateWithCallback
{
	public NegotiatingState(ConnectionBase connection)
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

		// negotiate
		String url = SignalAUtils.EnsureEndsWith(this.mConnection.getUrl(), "/") + "negotiate";
		String connectionData = this.mConnection.OnSending();
		url += TransportHelper.GetNegotiationQueryString(this.mConnection, connectionData);
		TransportHelper.AppendCustomQueryString(this.mConnection, url);
		AsyncCallback cb = new AsyncCallback()
		{

			@Override
			public void onComplete(HttpResponse httpResponse)
			{
				try
				{
					if (DoStop())
						return;


					if (httpResponse != null && httpResponse.getStatus() == 200 && !httpResponse.getBodyAsString().isEmpty())
					{
						JSONObject json = JSONHelper.ToJSONObject(httpResponse.getBodyAsString());
						String connectionId = "";
						String connectionToken = "";
						String protocolVersion = "";
						try
						{
							connectionId = json.getString("ConnectionId");
							connectionToken = json.getString("ConnectionToken");
							protocolVersion = json.getString("ProtocolVersion");

							if (NegotiatingState.this.mConnection.VerifyProtocolVersion(protocolVersion))
							{
								NegotiatingState.this.mConnection.setConnectionId(connectionId);
								NegotiatingState.this.mConnection.setConnectionToken(connectionToken);
								NegotiatingState.this.mConnection.SetNewState(new ConnectingState(NegotiatingState.this.mConnection));
								return;
							}
							else
							{
								NegotiatingState.this.mConnection.setException(new Exception("Not supported protocol version."));
								NegotiatingState.this.mConnection.SetNewState(new DisconnectedState(NegotiatingState.this.mConnection));
								return;
							}

						}
						catch (JSONException e)
						{
							NegotiatingState.this.mConnection.setException(new Exception("Unable to parse negotiation response."));
							return;
						}
					}
					else
					{
						NegotiatingState.this.mConnection.SetNewState(new DisconnectedState(NegotiatingState.this.mConnection));
					}
				}
				finally
				{
					NegotiatingState.this.mIsRunning.set(false);
				}
			}

			@Override
			public void onError(Exception ex)
			{
				NegotiatingState.this.mConnection.setException(ex);
				NegotiatingState.this.mConnection.SetNewState(new DisconnectedState(NegotiatingState.this.mConnection));
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