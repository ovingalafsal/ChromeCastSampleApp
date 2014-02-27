package com.cabot.chromecastsampleapp;

import android.util.Log;

import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;

public class HelloWorldChannel implements MessageReceivedCallback{

	public String getNamespace() {
	    return "urn:x-cast:com.cabot.custom";
	}

	  @Override
	  public void onMessageReceived(CastDevice castDevice, String namespace,
	        String message) {
	    Log.d("", "onMessageReceived: " + message);
	  }
	  
}
