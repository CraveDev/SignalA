package com.zsoft.SignalA.Transport.longpolling;

import java.util.Map;

import org.json.JSONObject;

import android.os.Handler;

import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.zsoft.SignalA.ConnectionBase;
import com.zsoft.SignalA.ConnectionState;
import com.zsoft.SignalA.SendCallback;
import com.zsoft.SignalA.SignalAUtils;
import com.zsoft.SignalA.Transport.ProcessResult;
import com.zsoft.SignalA.Transport.TransportHelper;
import com.zsoft.parallelhttpclient.ParallelHttpClient;

public class ReconnectingState extends StopableStateWithCallback
{

	public ReconnectingState(ConnectionBase connection)
	{
		super(connection);
	}

	@Override
	public ConnectionState getState()
	{
		return ConnectionState.Reconnecting;
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

		if (this.mConnection.getMessageId() == null)
		{
			// No message received yet....connect instead of reconnect
			this.mConnection.SetNewState(new ConnectingState(this.mConnection));
			return;
		}

		String url = SignalAUtils.EnsureEndsWith(this.mConnection.getUrl(), "/");
		url += "reconnect";
		url += TransportHelper.GetReceiveQueryString(this.mConnection, null, TRANSPORT_NAME);

		AsyncCallback cb = new AsyncCallback()
		{

			@Override
			public void onComplete(HttpResponse httpResponse)
			{
				if (DoStop())
					return;

				try
				{
					if (httpResponse.getStatus() == 200)
					{
						JSONObject json = JSONHelper.ToJSONObject(httpResponse.getBodyAsString());
						if (json != null)
						{
							ProcessResult result = TransportHelper.ProcessResponse(ReconnectingState.this.mConnection, json);

							if (result.processingFailed)
							{
								ReconnectingState.this.mConnection.setException(new Exception("Error while proccessing response."));
							}
							else if (result.disconnected)
							{
								ReconnectingState.this.mConnection.SetNewState(new DisconnectedState(ReconnectingState.this.mConnection));
								return;
							}
						}
						else
						{
							ReconnectingState.this.mConnection.setException(new Exception("Error when parsing response to JSONObject."));
						}
					}
					else
					{
						ReconnectingState.this.mConnection.setException(new Exception("Error when calling endpoint. Returncode: " + httpResponse.getStatus()));
					}
				}
				finally
				{
					if (ReconnectingState.this.mConnection.getCurrentState() == ReconnectingState.this)
					{
						// Delay before reconnecting
						Delay(2000, new DelayCallback()
						{
							@Override
							public void OnStopedBeforeElapsed()
							{
								ReconnectingState.this.mIsRunning.set(false);
								ReconnectingState.this.mConnection.SetNewState(new DisconnectedState(ReconnectingState.this.mConnection));
							}

							@Override
							public void OnDelayElapsed()
							{
								ReconnectingState.this.mIsRunning.set(false);
								// Loop if we are still reconnecting
								Run();
							}
						});
					}
				}
			}

			@Override
			public void onError(Exception ex)
			{
				ReconnectingState.this.mConnection.setException(ex);

			}
		};

		synchronized (this.mCallbackLock)
		{
			// mCurrentCallback = cb;
		}

		ParallelHttpClient httpClient = new ParallelHttpClient();
		httpClient.setMaxRetries(1);
		httpClient.setConnectionTimeout(15000);
		httpClient.setReadTimeout(15000);
		for (Map.Entry<String, String> entry : this.mConnection.getHeaders().entrySet())
		{
			httpClient.addHeader(entry.getKey(), entry.getValue());
		}
		httpClient.post(url, null, cb);
	}

	protected void Delay(final long milliSeconds, final DelayCallback cb)
	{
		final long startTime = System.currentTimeMillis();
		final Handler handler = new Handler();
		final Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				if (DoStop())
				{
					cb.OnStopedBeforeElapsed();
				}
				else
				{
					long difference = System.currentTimeMillis() - startTime;
					if (difference < milliSeconds)
					{
						handler.postDelayed(this, 500);
					}
					else
					{
						cb.OnDelayElapsed();
					}
				}

			}
		};

		handler.postDelayed(runnable, 500);
	}

	private abstract class DelayCallback
	{
		public abstract void OnDelayElapsed();

		public abstract void OnStopedBeforeElapsed();
	}

}
