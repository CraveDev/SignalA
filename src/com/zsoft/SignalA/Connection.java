package com.zsoft.SignalA;

import android.content.Context;

import com.zsoft.SignalA.Transport.ITransport;

public class Connection extends ConnectionBase {

	public Connection(String url, Context context, ITransport transport) {
		super(url, context, transport);
	}

}
