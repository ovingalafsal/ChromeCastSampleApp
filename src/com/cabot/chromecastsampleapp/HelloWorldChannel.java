package com.cabot.chromecastsampleapp;

import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class HelloWorldChannel implements MessageReceivedCallback{
	
	private static final String NAMESPACE = "urn:x-cast:com.google.cast.sample.helloworld";

	public String getNamespace() {
	    return NAMESPACE;
	}

	  @Override
	  public void onMessageReceived(CastDevice castDevice, String namespace,
	        String message) {
	    Log.d("", "onMessageReceived: " + message);
	  }
	  
	  public void sendMessage(GoogleApiClient apiClient, String message) {
	        Cast.CastApi.sendMessage(apiClient, NAMESPACE, message).setResultCallback(
	                new SendMessageResultCallback(message));
	    }

	    private class SendMessageResultCallback implements ResultCallback<Status> {
	        String mMessage;

	        SendMessageResultCallback(String message) {
	            mMessage = message;
	        }

	        @Override
	        public void onResult(Status result) {
	            if (!result.isSuccess()) {
	                Log.d("", "Failed to send message. statusCode: " + result.getStatusCode()
	                        + " message: " + mMessage);
	            }
	        }
	    }
	  
}
