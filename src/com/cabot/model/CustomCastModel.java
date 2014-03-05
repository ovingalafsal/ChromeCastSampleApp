package com.cabot.model;

import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;

import com.cabot.chromecastsampleapp.CastMediaActivity.ConnectionCallbacks;
import com.cabot.chromecastsampleapp.CastMediaActivity.ConnectionFailedListener;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.api.GoogleApiClient;

public class CustomCastModel {
	
	private CastDevice mSelectedDevice;
	private GoogleApiClient mApiClient;
	private Cast.Listener mCastListener;
	private ConnectionCallbacks mConnectionCallbacks;
	private ConnectionFailedListener mConnectionFailedListener;
	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private MediaRouter.Callback mMediaRouterCallback;
	private RemoteMediaPlayer mRemoteMediaPlayer;

}
