package com.zsoft.SignalA.Transport.longpolling;

import java.util.concurrent.atomic.AtomicBoolean;

import com.zsoft.SignalA.ConnectionBase;
import com.zsoft.SignalA.Transport.StateBase;

public abstract class StopableState extends StateBase
{
	protected AtomicBoolean requestStop = new AtomicBoolean(false);
	protected static final String TRANSPORT_NAME = "LongPolling";

	public StopableState(ConnectionBase connection)
	{
		super(connection);
	}

	protected boolean DoStop()
	{
		if (this.requestStop.get())
		{
			this.mConnection.SetNewState(new DisconnectedState(this.mConnection));
			return true;
		}
		return false;
	}

}
