package com.example.adtest;

import tv.matchstick.flint.ApplicationMetadata;
import tv.matchstick.flint.ConnectionResult;
import tv.matchstick.flint.Flint;
import tv.matchstick.flint.Flint.ApplicationConnectionResult;
import tv.matchstick.flint.FlintDevice;
import tv.matchstick.flint.FlintManager;
import tv.matchstick.flint.FlintMediaControlIntent;
import tv.matchstick.flint.ResultCallback;
import tv.matchstick.flint.Status;
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
import android.widget.EditText;

public class AdTestMainActivity extends ActionBarActivity {
    private static final String TAG = "AdTestMainActivity";

    private static final String APP_URL = "http://www.baidu.com";

    private FlintDevice mSelectedDevice;
    private FlintManager mApiClient;
    private Flint.Listener mFlingListener;
    private ConnectionCallbacks mConnectionCallbacks;

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;

    private EditText mLaunchUrlEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_test_main);

        String APPLICATION_ID = "~AdTest";
        Flint.FlintApi.setApplicationId(APPLICATION_ID);

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(
                        FlintMediaControlIntent
                                .categoryForFlint(APPLICATION_ID)).build();

        mMediaRouterCallback = new MediaRouterCallback();
        mFlingListener = new FlingListener();
        mConnectionCallbacks = new ConnectionCallbacks();

        mLaunchUrlEditText = (EditText) findViewById(R.id.launch_url);
        mLaunchUrlEditText.setText(APP_URL);
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        mMediaRouter.removeCallback(mMediaRouterCallback);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Called when the options menu is first created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat
                .getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_stop:
            stopApplication();
            
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
            break;
        }

        return true;
    }

    private class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteSelected: " + route);
            AdTestMainActivity.this.onRouteSelected(route);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteUnselected: " + route);
            AdTestMainActivity.this.onRouteUnselected(route);
        }
    }

    private class FlingListener extends Flint.Listener {
        @Override
        public void onApplicationDisconnected(int statusCode) {
            Log.d(TAG, "Flint.Listener.onApplicationDisconnected: "
                    + statusCode);

            mSelectedDevice = null;
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());

            if (mApiClient == null) {
                return;
            }
        }
    }

    private class ConnectionCallbacks implements
            FlintManager.ConnectionCallbacks {
        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "ConnectionCallbacks.onConnectionSuspended");
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "ConnectionCallbacks.onConnected");
            Flint.FlintApi.launchApplication(mApiClient, APP_URL, true, false)
                    .setResultCallback(new ConnectionResultCallback());
        }

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

        }
    }

    /**
     * Stop receiver application.
     */
    public void stopApplication() {
        if (mApiClient == null || !mApiClient.isConnected()) {
            return;
        }

        Flint.FlintApi.stopApplication(mApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status result) {
                        if (result.isSuccess()) {
                            //
                        }
                    }
                });
    }

    private void setSelectedDevice(FlintDevice device) {
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
                stopApplication();
                
                disconnectApiClient();
            }

            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        }
    }

    private void connectApiClient() {
        Flint.FlintOptions apiOptions = Flint.FlintOptions.builder(
                mSelectedDevice, mFlingListener).build();
        mApiClient = new FlintManager.Builder(this)
                .addApi(Flint.API, apiOptions)
                .addConnectionCallbacks(mConnectionCallbacks).build();
        mApiClient.connect();
    }

    private void disconnectApiClient() {
        if (mApiClient != null) {
            mApiClient.disconnect();
            mApiClient = null;
        }
    }

    /**
     * Called when a user selects a route.
     */
    private void onRouteSelected(RouteInfo route) {
        Log.d(TAG, "onRouteSelected: " + route.getName());

        FlintDevice device = FlintDevice.getFromBundle(route.getExtras());
        setSelectedDevice(device);
    }

    /**
     * Called when a user unselects a route.
     */
    private void onRouteUnselected(RouteInfo route) {
        if (route != null) {
            Log.d(TAG, "onRouteUnselected: " + route.getName());
        }
        setSelectedDevice(null);
    }

}
