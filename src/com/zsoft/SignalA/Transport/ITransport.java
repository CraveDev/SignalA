package com.zsoft.SignalA.Transport;

import com.zsoft.SignalA.ConnectionBase;
import com.zsoft.SignalA.LogLevel;

public interface ITransport
{
	StateBase CreateInitialState(ConnectionBase connectionBase);

	abstract void OnException(Exception exception);

	abstract void OnMessage(String message);

	abstract void OnStateChanged(StateBase oldState, StateBase newState);

	abstract void OnLog(LogLevel level, String message);
}
