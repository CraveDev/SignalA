package com.zsoft.SignalA.Transport.longpolling;

import java.util.concurrent.atomic.AtomicBoolean;

import com.zsoft.SignalA.ConnectionBase;
import com.zsoft.SignalA.ConnectionState;
import com.zsoft.SignalA.SendCallback;
import com.zsoft.SignalA.Transport.StateBase;

public class DisconnectedState extends StateBase
{
	private AtomicBoolean requestStart = new AtomicBoolean(false);

	public DisconnectedState(ConnectionBase connection)
	{
		super(connection);
	}

	@Override
	public ConnectionState getState()
	{
		return ConnectionState.Disconnected;
	}

	@Override
	public void Start()
	{
		this.requestStart.set(true);
		Run();
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
		if (this.requestStart.get())
		{
			NegotiatingState s = new NegotiatingState(this.mConnection);
			this.mConnection.SetNewState(s);
		}
	}

}
