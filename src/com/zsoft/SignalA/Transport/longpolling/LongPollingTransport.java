package com.zsoft.SignalA.Transport.longpolling;

import com.zsoft.SignalA.ConnectionBase;
import com.zsoft.SignalA.LogLevel;
import com.zsoft.SignalA.Transport.ITransport;
import com.zsoft.SignalA.Transport.StateBase;

public class LongPollingTransport implements ITransport
{
	@Override
	public StateBase CreateInitialState(ConnectionBase connection)
	{
		return new DisconnectedState(connection);
	}

	@Override
	public void OnException(Exception exception)
	{

	}

	@Override
	public void OnMessage(String message)
	{

	}

	@Override
	public void OnStateChanged(StateBase oldState, StateBase newState)
	{

	}

	@Override
	public void OnLog(LogLevel level, String message)
	{

	}
}
