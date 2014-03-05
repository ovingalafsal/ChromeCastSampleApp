package com.cabot.chromecastsampleapp;

import java.io.IOException;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

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

/**
 * An activity which both presents a UI on the first screen and casts the
 * TicTacToe game board to the selected Cast device and its attached second
 * screen.
 */
public class CastMediaActivity extends ActionBarActivity {
	private static final String TAG = CastMediaActivity.class.getSimpleName();
	private static final int REQUEST_GMS_ERROR = 0;

//	private static final String APP_ID = "5001E5A6";
	private static final String APP_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;

	private CastDevice mSelectedDevice;
	private GoogleApiClient mApiClient;
	private Cast.Listener mCastListener;
	private ConnectionCallbacks mConnectionCallbacks;
	private ConnectionFailedListener mConnectionFailedListener;
	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private MediaRouter.Callback mMediaRouterCallback;
	private RemoteMediaPlayer mRemoteMediaPlayer;
	private Button play,play2;
	boolean isPlaying = false;

	/**
	 * Called when the activity is first created. Initializes the game with
	 * necessary listeners for player interaction, and creates a new cast
	 * channel.
	 */
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.test);

		mMediaRouter = MediaRouter.getInstance(getApplicationContext());
		mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(
						CastMediaControlIntent.categoryForCast(APP_ID)).build();

		mMediaRouterCallback = new MediaRouterCallback();
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
						isPlaying = mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING;
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

		play = (Button) findViewById(R.id.play);
		play.setEnabled(false);
		play.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Play(play,"http://dev2.cabotprojects.com/myl_dev/mylfiles/ads/video/1388750465NikeLebron.mp4");
			}
		});
		
		play2 = (Button) findViewById(R.id.play2);
		play2.setEnabled(false);
		play2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Play(play2,"http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
			}
		});

	}

	/**
	 * Called when the options menu is first created.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.cast, menu);
		MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
		MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat
				.getActionProvider(mediaRouteMenuItem);
		mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
		return true;
	}

	/**
	 * Called on application start. Using the previously selected Cast device,
	 * attempts to begin a session using the application name TicTacToe.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
				MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
	}

	@Override
	protected void onResume() {
		super.onResume();
		int errorCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (errorCode != ConnectionResult.SUCCESS) {
			GooglePlayServicesUtil.getErrorDialog(errorCode, this,
					REQUEST_GMS_ERROR).show();
		}
	}

	/**
	 * Removes the activity from memory when the activity is paused.
	 */
	@Override
	protected void onPause() {
		finish();
		super.onPause();
	}

	/**
	 * Attempts to end the current game session when the activity stops.
	 */
	@Override
	protected void onStop() {
		setSelectedDevice(null);
		mMediaRouter.removeCallback(mMediaRouterCallback);
		super.onStop();
	}

	/**
	 * Returns the screen configuration to portrait mode whenever changed.
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	private void setSelectedDevice(CastDevice device) {
		Log.d(TAG, "setSelectedDevice: " + device);
		mSelectedDevice = device;

		if (mSelectedDevice != null) {
			try {
				disconnectApiClient();
				connectApiClient();
			} catch (IllegalStateException e) {
				Log.w(TAG, "Exception while connecting API client", e);
				disconnectApiClient();
			}
		} else {
			if (mApiClient != null) {
				if (mApiClient.isConnected()) {
					/*if (mRemoteMediaPlayer != null && isPlaying) {
						try {
							mRemoteMediaPlayer.pause(mApiClient);
						} catch (IOException e) {
							e.printStackTrace();
						}

					}*/
				}
				disconnectApiClient();
			}
			play.setEnabled(false);
			play2.setEnabled(false);
			mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
		}
	}

	private void connectApiClient() {
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
		play.setText("START");
		play2.setText("START ANIMATION VIDEO");
		if (mApiClient != null && mApiClient.isConnected()) {
			/*if (mRemoteMediaPlayer != null && isPlaying) {
				try {
					mRemoteMediaPlayer.stop(mApiClient);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}*/
				mApiClient.disconnect();
				mApiClient = null;
		}
	}

	/**
	 * Called when a user selects a route.
	 */
	private void onRouteSelected(RouteInfo route) {
		Log.d(TAG, "onRouteSelected: " + route.getName());

		CastDevice device = CastDevice.getFromBundle(route.getExtras());
		setSelectedDevice(device);
	}

	/**
	 * Called when a user unselects a route.
	 */
	private void onRouteUnselected(RouteInfo route) {
		Log.d(TAG, "onRouteUnselected: " + route.getName());
		setSelectedDevice(null);
	}

	/**
	 * An extension of the MediaRoute.Callback specifically for the TicTacToe
	 * game.
	 */
	private class MediaRouterCallback extends MediaRouter.Callback {
		@Override
		public void onRouteSelected(MediaRouter router, RouteInfo route) {
			Log.d(TAG, "onRouteSelected: " + route);
			CastMediaActivity.this.onRouteSelected(route);
		}

		@Override
		public void onRouteUnselected(MediaRouter router, RouteInfo route) {
			Log.d(TAG, "onRouteUnselected: " + route);
			CastMediaActivity.this.onRouteUnselected(route);
		}
	}

	private class CastListener extends Cast.Listener {
		@Override
		public void onApplicationDisconnected(int statusCode) {
			Log.e(TAG, "Cast.Listener.onApplicationDisconnected: " + statusCode);
			try {
				Cast.CastApi.removeMessageReceivedCallbacks(mApiClient,
						mRemoteMediaPlayer.getNamespace());
			} catch (IOException e) {
				Log.e(TAG, "Exception while launching application", e);
			}
		}
	}

	public class ConnectionCallbacks implements
			GoogleApiClient.ConnectionCallbacks {
		@Override
		public void onConnectionSuspended(int cause) {
			Log.d(TAG, "ConnectionCallbacks.onConnectionSuspended");
		}

		@Override
		public void onConnected(Bundle connectionHint) {
			Log.d(TAG, "ConnectionCallbacks.onConnected");
			Cast.CastApi.launchApplication(mApiClient, APP_ID)
					.setResultCallback(new ConnectionResultCallback());
		}
	}

	public class ConnectionFailedListener implements
			GoogleApiClient.OnConnectionFailedListener {
		@Override
		public void onConnectionFailed(ConnectionResult result) {
			Log.d(TAG, "ConnectionFailedListener.onConnectionFailed");
			setSelectedDevice(null);
		}
	}

	private final class ConnectionResultCallback implements
			ResultCallback<ApplicationConnectionResult> {
		@Override
		public void onResult(ApplicationConnectionResult result) {
			Status status = result.getStatus();
			ApplicationMetadata appMetaData = result.getApplicationMetadata();

			if (status.isSuccess()) {
				Log.e(TAG, "ConnectionResultCallback: " + appMetaData.getName());
				play.setEnabled(true);
				play2.setEnabled(true);
				try {
					Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
							mRemoteMediaPlayer.getNamespace(),
							mRemoteMediaPlayer);
				} catch (IOException e) {
					Log.w(TAG, "Exception while launching application", e);
				}
			} else {
				play.setEnabled(false);
				Log.e(TAG,
						"ConnectionResultCallback. Unable to launch the game. statusCode: "
								+ status.getStatusCode());
			}
		}
	}

	private void Play(Button play,String file) {
		Log.e("Play **********", mApiClient.isConnected() + " ++++++++Status");
		if (play.getText().toString().equalsIgnoreCase("PAUSE")) {
			if (mApiClient != null) {
				if (mApiClient.isConnected()) {
					if (mRemoteMediaPlayer != null) {
						try {
							mRemoteMediaPlayer.pause(mApiClient);
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				}
			}
			play.setText("PLAY");
		} else if (play.getText().toString().equalsIgnoreCase("PLAY")) {
			if (mApiClient != null) {
				if (mApiClient.isConnected()) {
					if (mRemoteMediaPlayer != null) {
						try {
							mRemoteMediaPlayer.play(mApiClient);
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				}
			}
			play.setText("PAUSE");
		} else {
			try {
				Log.e("Play Testing", "mRemoteMediaPlayer is play");
				play.setText("PAUSE");
				MediaMetadata mediaMetadata = new MediaMetadata(
						MediaMetadata.MEDIA_TYPE_MOVIE);
				mediaMetadata.putString(MediaMetadata.KEY_TITLE, "My video");
				MediaInfo mediaInfo = new MediaInfo.Builder(
						 file)
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
			} catch (IllegalStateException e) {
				Log.e(TAG, e.getMessage());
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}

}
