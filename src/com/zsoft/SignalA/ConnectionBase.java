package com.zsoft.SignalA;

import java.util.Map;
import java.util.TreeMap;

import org.json.JSONObject;

import android.content.Context;

import com.zsoft.SignalA.Transport.ITransport;
import com.zsoft.SignalA.Transport.StateBase;

public abstract class ConnectionBase
{
	private Object mStateLock = new Object();
	private StateBase mCurrentState = null;
	private String mUrl = "";
	private String mConnectionId = null;
	private String mConnectionToken = null;
	private Context mContext;
	private String mMessageId = null;
	private String mGroupsToken = null;
	private String mQueryString = null;
	private Map<String, String> mHeaders = new TreeMap<String, String>();

	protected ITransport mTransport;

	public ConnectionBase(String url, Context context, ITransport transport, String queryString)
	{
		this(url, context, transport);
		setQueryString(queryString);
	}

	public ConnectionBase(String url, Context context, ITransport transport)
	{
		this.mContext = context;
		this.mTransport = transport;
		this.mCurrentState = this.mTransport.CreateInitialState(this);
		setUrl(url);
	}

	public void SetNewState(StateBase state)
	{
		StateBase oldState;
		synchronized (this.mStateLock)
		{
			oldState = this.mCurrentState;
			this.mCurrentState = state;

			if (state.getState() == ConnectionState.Disconnected)
			{
				setConnectionId(null);
				setConnectionToken(null);
			}
		}

		state.Run();

		// Fire event
		OnStateChanged(oldState, state);
	}

	public String getUrl()
	{
		return this.mUrl;
	}

	public void setUrl(String url)
	{
		this.mUrl = url;
	}

	public Context getContext()
	{
		return this.mContext;
	}

	private void setQueryString(String queryString)
	{
		this.mQueryString = queryString;
	}

	public String getQueryString()
	{
		return this.mQueryString;
	}

	public String getConnectionId()
	{
		return this.mConnectionId;
	}

	public void setConnectionId(String connectionId)
	{
		this.mConnectionId = connectionId;
	}

	public String getConnectionToken()
	{
		return this.mConnectionToken;
	}

	public void setConnectionToken(String connectionToken)
	{
		this.mConnectionToken = connectionToken;
	}

	public StateBase getCurrentState()
	{
		synchronized (this.mStateLock)
		{
			return this.mCurrentState;
		}
	}

	public boolean VerifyProtocolVersion(String protocolVersion)
	{
		return protocolVersion.compareTo(getProtocolVersion()) == 0;
	}

	public String getProtocolVersion()
	{
		return "1.3";
	}

	public String getMessageId()
	{
		return this.mMessageId;
	}

	public void setMessageId(String messageId)
	{
		this.mMessageId = messageId;
	}

	public String getGroupsToken()
	{
		return this.mGroupsToken;
	}

	public void setGroupsToken(String groupsToken)
	{
		this.mGroupsToken = groupsToken;
	}

	public void setMessage(JSONObject response)
	{
		OnMessage(response.toString());
	}

	public void setException(Exception exception)
	{
		OnException(exception);
	}

	public void addHeader(String header, String value)
	{
		this.mHeaders.put(header, value);
	}

	public Map<String, String> getHeaders()
	{
		return this.mHeaders;
	}

	// Methods for the user to implement

	protected void OnException(Exception exception)
	{
		this.mTransport.OnException(exception);
	}

	protected void OnMessage(String message)
	{
		this.mTransport.OnMessage(message);
	}

	protected void OnStateChanged(StateBase oldState, StateBase newState)
	{
		this.mTransport.OnStateChanged(oldState, newState);
	}

	public void OnLog(LogLevel logLevel, String message, Object... args)
	{
		this.mTransport.OnLog(logLevel, String.format(message, args));
	}

	public String OnSending()
	{
		return null;
	}

	public void Start()
	{
		getCurrentState().Start();
	}

	public void Stop()
	{
		getCurrentState().Stop();
	}

	public void Send(CharSequence text, SendCallback callback)
	{
		getCurrentState().Send(text, callback);
	}

}
