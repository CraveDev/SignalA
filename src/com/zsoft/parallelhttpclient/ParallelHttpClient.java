package com.zsoft.parallelhttpclient;

import java.io.IOException;
import java.net.HttpURLConnection;

import android.os.Build;
import android.util.Log;

import com.turbomanage.httpclient.AsyncHttpClient;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.RequestHandler;
import com.turbomanage.httpclient.RequestLogger;

public class ParallelHttpClient extends AsyncHttpClient implements RequestLogger
{
	static
	{
		disableConnectionReuseIfNecessary();
		// See http://code.google.com/p/basic-http-client/issues/detail?id=8
		if (Build.VERSION.SDK_INT > 8)
			ensureCookieManager();
	}

	/**
	 * Constructs a new client with empty baseUrl. When used this way, the path
	 * passed to a request method must be the complete URL.
	 */
	public ParallelHttpClient()
	{
		this("");
	}

	/**
	 * Constructs a new client using the default {@link RequestHandler} and {@link RequestLogger}.
	 */
	public ParallelHttpClient(String baseUrl)
	{
		super(new ParallelAsyncTaskFactory(), baseUrl);

		setConnectionTimeout(5000);
		setReadTimeout(5000);
		setRequestLogger(this);
	}

	/**
	 * Constructs a client with baseUrl and custom {@link RequestHandler}.
	 * 
	 * @param baseUrl
	 * @param requestHandler
	 */
	public ParallelHttpClient(String baseUrl, RequestHandler requestHandler)
	{
		super(new ParallelAsyncTaskFactory(), baseUrl, requestHandler);

		setConnectionTimeout(5000);
		setReadTimeout(5000);
		setRequestLogger(this);
	}

	/**
	 * Work around bug in {@link HttpURLConnection} on older versions of
	 * Android.
	 * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
	 */
	private static void disableConnectionReuseIfNecessary()
	{
		// HTTP connection reuse which was buggy pre-froyo
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO)
		{
			System.setProperty("http.keepAlive", "false");
		}
	}

	@Override
	public boolean isLoggingEnabled()
	{
		return false;
	}

	@Override
	public void log(String msg)
	{
		if (isLoggingEnabled())
			Log.d("SignalR", msg);
	}

	@Override
	public void logRequest(HttpURLConnection uc, Object content) throws IOException
	{
		log("=== HTTP Request ===");
		log(uc.getRequestMethod() + " " + uc.getURL().toString());
		if (content instanceof String)
		{
			log("Content: " + (String)content);
		}
	}

	@Override
	public void logResponse(HttpResponse res)
	{
		if (res != null)
		{
			log("=== HTTP Response ===");
			log("Receive url: " + res.getUrl());
			log("Status: " + res.getStatus());
			log("Content:\n" + res.getBodyAsString());
		}
	}
}
