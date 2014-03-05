/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.cabot.chromecastsampleapp;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;

/**
 * Sample activity to demonstrate discovering Cast devices using the Media
 * Router. The discovered devices are displayed as a list.
 * 
 * @see http://developer.android.com/guide/topics/media/mediarouter.html
 */
public class MediaRouterDiscoveryActivity extends Activity {

	private static final String TAG = MediaRouterDiscoveryActivity.class
			.getSimpleName();
	private static final String APP_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;

	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private MediaRouter.Callback mMediaRouterCallback;
	private CastDevice mSelectedDevice;

	private ArrayList<String> mRouteNames = new ArrayList<String>();
	private ArrayAdapter<String> mAdapter;
	private final ArrayList<MediaRouter.RouteInfo> mRouteInfos = new ArrayList<MediaRouter.RouteInfo>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.media_router_discovery);

		// Create a list control for displaying the names of the
		// devices discovered by the MediaRouter
		final ListView listview = (ListView) findViewById(R.id.listview);
		mRouteNames = new ArrayList<String>();
		mAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mRouteNames);
		listview.setAdapter(mAdapter);
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {
				Log.d(TAG, "onItemClick: position=" + position);

				MediaRouter.RouteInfo info = mRouteInfos.get(position);
				mMediaRouter.selectRoute(info);
			}

		});

		mMediaRouter = MediaRouter.getInstance(getApplicationContext());
		// Create a MediaRouteSelector for the type of routes your app supports
		mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(
						CastMediaControlIntent.categoryForCast(APP_ID)).build();
		// Create a MediaRouter callback for discovery events
		mMediaRouterCallback = new MyMediaRouterCallback();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Add the callback to start device discovery
		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
				MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
	}

	@Override
	protected void onPause() {
		// Remove the callback to stop device discovery
		mMediaRouter.removeCallback(mMediaRouterCallback);
		super.onPause();
	}

	private class MyMediaRouterCallback extends MediaRouter.Callback {

		@Override
		public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
			Log.d(TAG, "onRouteAdded: info=" + info);

			// Add route to list of discovered routes
			synchronized (this) {
				mRouteInfos.add(info);
				mRouteNames.add(info.getName() + " (" + info.getDescription()
						+ ")");
				mAdapter.notifyDataSetChanged();
			}
		}

		@Override
		public void onRouteRemoved(MediaRouter router,
				MediaRouter.RouteInfo info) {
			Log.d(TAG, "onRouteRemoved: info=" + info);

			// Remove route from list of routes
			synchronized (this) {
				for (int i = 0; i < mRouteInfos.size(); i++) {
					MediaRouter.RouteInfo routeInfo = mRouteInfos.get(i);
					if (routeInfo.equals(info)) {
						mRouteInfos.remove(i);
						mRouteNames.remove(i);
						mAdapter.notifyDataSetChanged();
						return;
					}
				}
			}
		}

		@Override
		public void onRouteSelected(MediaRouter router, RouteInfo info) {
			Log.d(TAG, "onRouteSelected: info=" + info);
			mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
		}

		@Override
		public void onRouteUnselected(MediaRouter router, RouteInfo info) {
			Log.d(TAG, "onRouteUnselected: info=" + info);
			mSelectedDevice = null;
		}

	}

}
