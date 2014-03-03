package com.zsoft.SignalA.Transport.longpolling;

import com.zsoft.SignalA.ConnectionBase;

public abstract class StopableStateWithCallback extends StopableState
{
	protected Object mCallbackLock = new Object();

	// protected AjaxCallback<JSONObject> mCurrentCallback = null;

	public StopableStateWithCallback(ConnectionBase connection)
	{
		super(connection);
	}

	@Override
	public void Stop()
	{
		this.requestStop.set(true);
		synchronized (this.mCallbackLock)
		{
			// if(mCurrentCallback!=null)
			// mCurrentCallback.abort();
		}
		Run();
	}

	@Override
	public void Run()
	{
		if (this.mIsRunning.compareAndSet(false, true))
		{
			try
			{
				OnRun();
			}
			finally
			{
				// mIsRunning.set(false); Do this in the callback instead
			}
		}
	}

}
