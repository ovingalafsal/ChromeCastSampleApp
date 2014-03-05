package com.cabot.chromecastsampleapp;

import java.io.IOException;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.cast.RemoteMediaPlayer.MediaChannelResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class CastActivity extends ActionBarActivity implements OnClickListener{

	private static final String APP_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;

	private MediaRouter mRouter;
	private MediaRouter.Callback mCallback;
	private MediaRouteSelector mSelector;
	private CastDevice mSelectedDevice;
	private GoogleApiClient mApiClient;
	private Cast.Listener mCastListener;
	private ConnectionCallbacks mConnectionCallbacks;
	boolean mWaitingForReconnect = false;
	private ConnectionFailedListener mConnectionFailedListener;
	HelloWorldChannel mHelloWorldChannel = new HelloWorldChannel();
	RemoteMediaPlayer mRemoteMediaPlayer = new RemoteMediaPlayer();
	Button play;
	Button play2;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);
		play = (Button)findViewById(R.id.play);
		play.setOnClickListener(this);
		mRouter = MediaRouter.getInstance(getApplicationContext());
		mSelector = new MediaRouteSelector.Builder()
				.addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
				.addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
				.build();
		mCallback = new MyCallback();
		mCastListener = new CastListener();
		mConnectionCallbacks = new ConnectionCallbacks();
		mConnectionFailedListener = new ConnectionFailedListener();

		mRemoteMediaPlayer = new RemoteMediaPlayer();
		mRemoteMediaPlayer
				.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {
					@Override
					public void onStatusUpdated() {
						MediaStatus mediaStatus = mRemoteMediaPlayer
								.getMediaStatus();
						boolean isPlaying = mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING;
					}
				});

		mRemoteMediaPlayer
				.setOnMetadataUpdatedListener(new RemoteMediaPlayer.OnMetadataUpdatedListener() {
					@Override
					public void onMetadataUpdated() {
						MediaInfo mediaInfo = mRemoteMediaPlayer.getMediaInfo();
						MediaMetadata metadata = mediaInfo.getMetadata();
					}
				});
	}

	// Add the callback on start to tell the media router what kinds of routes
	// the application is interested in so that it can try to discover suitable
	// ones.
	public void onStart() {
		super.onStart();

		mRouter.addCallback(mSelector, mCallback,
				MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);

		MediaRouter.RouteInfo route = mRouter.updateSelectedRoute(mSelector);
		// do something with the route...
	}

	// Remove the selector on stop to tell the media router that it no longer
	// needs to invest effort trying to discover routes of these kinds for now.
	public void onStop() {
		 setSelectedDevice(null);
		 mRouter.removeCallback(mCallback);
	     super.onStop();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.cast, menu);

		MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
		MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat
				.getActionProvider(mediaRouteMenuItem);
		mediaRouteActionProvider.setRouteSelector(mSelector);
		return true;
	}

	private final class MyCallback extends MediaRouter.Callback {

		@Override
		public void onRouteSelected(MediaRouter router, RouteInfo route) {
			super.onRouteSelected(router, route);
			CastDevice device = CastDevice.getFromBundle(route.getExtras());
			setSelectedDevice(device);
			String routeId = route.getId();
			Toast.makeText(CastActivity.this, "RoutedId " + routeId, Toast.LENGTH_SHORT).show();

		}

		@Override
		public void onRouteUnselected(MediaRouter router, RouteInfo route) {
			super.onRouteUnselected(router, route);
			mSelectedDevice = null;
			setSelectedDevice(null);
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		int errorCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (errorCode != ConnectionResult.SUCCESS) {
			GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0).show();
		}
	}

	private void setSelectedDevice(CastDevice device) {
		Log.d("setSelectedDevice 1", "setSelectedDevice: " + device);
		mSelectedDevice = device;

		if (mSelectedDevice != null) {
			Log.e("Testing  ", "selcted" + mSelectedDevice);
			try {
				disconnectApiClient();
				connectApiClient();
			} catch (IllegalStateException e) {
				Log.w("", "Exception while connecting API client", e);
				disconnectApiClient();
			}
		} else {
			if (mApiClient != null) {
				if (mApiClient.isConnected()) {
				}
				disconnectApiClient();
			}
			mRouter.selectRoute(mRouter.getDefaultRoute());
		}
	}

	private void connectApiClient() {
		Log.e("Connection checking", " Inside Connect Status Before");
		Cast.CastOptions apiOptions = Cast.CastOptions.builder(mSelectedDevice,
				mCastListener).build();
		mApiClient = new GoogleApiClient.Builder(this)
				.addApi(Cast.API, apiOptions)
				.addConnectionCallbacks(mConnectionCallbacks)
				.addOnConnectionFailedListener(mConnectionFailedListener)
				.build();
		mApiClient.connect();
		Log.e("Connection checking", mApiClient.isConnected() + "Status");
	}

	private void disconnectApiClient() {
		if (mApiClient != null) {
			mApiClient.disconnect();
			mApiClient = null;
		}
	}

	private class CastListener extends Cast.Listener {

		@Override
		public void onApplicationStatusChanged() {
//			Toast.makeText(CastActivity.this, "testing", Toast.LENGTH_SHORT).show();
			if (mApiClient != null) {
				Log.e("",
						"onApplicationStatusChanged: "
								+ Cast.CastApi.getApplicationStatus(mApiClient));
			}
		}

		@Override
		public void onVolumeChanged() {
			if (mApiClient != null) {
				Log.e("",
						"onVolumeChanged: "
								+ Cast.CastApi.getVolume(mApiClient));
			}
		}

		@Override
		public void onApplicationDisconnected(int statusCode) {
			Log.e("", "Cast.Listener.onApplicationDisconnected: " + statusCode);
			try {
				Cast.CastApi.removeMessageReceivedCallbacks(mApiClient,
						mRemoteMediaPlayer.getNamespace());
			} catch (IOException e) {
				Log.w("", "Exception while launching application", e);
			}
		}
	}

	private class ConnectionFailedListener implements
			GoogleApiClient.OnConnectionFailedListener {
		@Override
		public void onConnectionFailed(ConnectionResult result) {
			Log.e("Testing 8", "onConnectionFailed  Connection Call back");
			setSelectedDevice(null);
		}
	}

	private class ConnectionCallbacks implements
			GoogleApiClient.ConnectionCallbacks {

		@Override
		public void onConnected(Bundle connectionHint) {
			Log.e("Testing 2", "onConnected Connection Call back");
			Cast.CastApi.launchApplication(mApiClient, APP_ID)
						.setResultCallback(new ConnectionResultCallback());
		}

		@Override
		public void onConnectionSuspended(int cause) {
			Log.e("Testing 2", "onConnectionSuspended");
			mWaitingForReconnect = true;
		}

	}

	private final class ConnectionResultCallback implements
			ResultCallback<ApplicationConnectionResult> {
		@Override
		public void onResult(ApplicationConnectionResult result) {
			Status status = result.getStatus();
			ApplicationMetadata appMetaData = result.getApplicationMetadata();

			if (status.isSuccess()) {
				Log.e("Testing 3", "ConnectionResultCallback");
				ApplicationMetadata applicationMetadata = result
						.getApplicationMetadata();
				String sessionId = result.getSessionId();
				String applicationStatus = result.getApplicationStatus();
				boolean wasLaunched = result.getWasLaunched();

				try {
					Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
							mRemoteMediaPlayer.getNamespace(),
							mRemoteMediaPlayer);
				} catch (IOException e) {
					Log.e("Testing Exception", "ConnectionResultCallback");
				}

				// mApplicationStarted = true;//TODO

				/*
				 * mHelloWorldChannel = new HelloWorldChannel(); try {
				 * Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
				 * mHelloWorldChannel.getNamespace(), mHelloWorldChannel); }
				 * catch (IOException e) { Log.e("",
				 * "Exception while creating channel", e); }
				 */

			}
		}
	}

	private void sendMessage(String message) {
		if (mApiClient != null && mHelloWorldChannel != null) {
			try {
				Cast.CastApi.sendMessage(mApiClient,
						mHelloWorldChannel.getNamespace(), message)
						.setResultCallback(new ResultCallback<Status>() {
							@Override
							public void onResult(Status result) {
								if (!result.isSuccess()) {
								}
							}
						});
			} catch (Exception e) {
			}
		}
	}

	private void Play() {
		try {
			if (mRemoteMediaPlayer != null) {
				mRemoteMediaPlayer.pause(mApiClient);

			} else {
				play.setText("PAUSE");
				MediaMetadata mediaMetadata = new MediaMetadata(
						MediaMetadata.MEDIA_TYPE_MOVIE);
				mediaMetadata.putString(MediaMetadata.KEY_TITLE, "My video");
				MediaInfo mediaInfo = new MediaInfo.Builder(
						"http://www.youtube.com/watch?feature=player_embedded&v=uK4_x_qq_E8")
						.setContentType("video/mp4")
						.setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
						.setMetadata(mediaMetadata).build();
				mRemoteMediaPlayer
						.load(mApiClient, mediaInfo, true)
						.setResultCallback(
								new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {

									@Override
									public void onResult(
											MediaChannelResult result) {
										if (result.getStatus().isSuccess()) {
										}
									}
								});
			}
		} catch (IllegalStateException e) {
		} catch (Exception e) {
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.play:
			Play();
			break;

		default:
			break;
		}
	}
}
