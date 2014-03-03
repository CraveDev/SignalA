package com.zsoft.SignalA.Transport.longpolling;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;
import com.zsoft.SignalA.ConnectionBase;
import com.zsoft.SignalA.ConnectionState;
import com.zsoft.SignalA.LogLevel;
import com.zsoft.SignalA.SendCallback;
import com.zsoft.SignalA.SignalAUtils;
import com.zsoft.SignalA.Transport.StateBase;
import com.zsoft.SignalA.Transport.TransportHelper;
import com.zsoft.parallelhttpclient.ParallelHttpClient;

public class DisconnectingState extends StateBase
{

	protected static final String TAG = "DisconnectingState";

	public DisconnectingState(ConnectionBase connection)
	{
		super(connection);
	}

	@Override
	public ConnectionState getState()
	{
		return ConnectionState.Disconnecting;
	}

	@Override
	public void Start()
	{
	}

	@Override
	public void Stop()
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
		String url = SignalAUtils.EnsureEndsWith(this.mConnection.getUrl(), "/");
		try
		{
			url += "abort?transport=LongPolling&connectionToken=" + URLEncoder.encode(this.mConnection.getConnectionToken(), "utf-8");
		}
		catch (UnsupportedEncodingException e)
		{
			this.mConnection.OnLog(LogLevel.Error, "Unsupported message encoding error, when encoding connectionToken.");
		}
		TransportHelper.AppendCustomQueryString(this.mConnection, url);

		AsyncCallback cb = new AsyncCallback()
		{
			@Override
			public void onComplete(HttpResponse httpResponse)
			{
				if (httpResponse.getStatus() != 200
						|| httpResponse.getBodyAsString() == null
						|| httpResponse.getBodyAsString().isEmpty())
				{
					DisconnectingState.this.mConnection.OnLog(LogLevel.Error, "Clean disconnect failed. Status: %s", httpResponse.getStatus());
				}

				DisconnectingState.this.mConnection.SetNewState(new DisconnectedState(DisconnectingState.this.mConnection));
			}

			@Override
			public void onError(Exception ex)
			{
				DisconnectingState.this.mConnection.setException(ex);
				DisconnectingState.this.mConnection.SetNewState(new DisconnectedState(DisconnectingState.this.mConnection));
			}
		};

		ParallelHttpClient httpClient = new ParallelHttpClient();
		httpClient.setMaxRetries(1);
		for (Map.Entry<String, String> entry : this.mConnection.getHeaders().entrySet())
		{
			httpClient.addHeader(entry.getKey(), entry.getValue());
		}
		ParameterMap params = httpClient.newParams();
		httpClient.post(url, params, cb);
	}

}
