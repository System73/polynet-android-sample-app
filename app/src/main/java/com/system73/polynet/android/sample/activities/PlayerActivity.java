/*
 * Copyright (C) 2016 The Android Open Source Project
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

/*
 * Copyright (C) 2017 System73 (R) Europe, SL
 * Refer to the LICENSE file for the detailed License of this software.
 *
 * This file has been modified from the original file of ExoPlayer's demo, available at:
 * https://github.com/google/ExoPlayer
 */
package com.system73.polynet.android.sample.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.system73.polynet.android.sample.R;
import com.system73.polynet.android.sdk.PolyNet;
import com.system73.polynet.android.sdk.PolyNetConfiguration;
import com.system73.polynet.android.sdk.PolyNetConnectionListener;
import com.system73.polynet.android.sdk.PolyNetMetricsRequestListener;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class PlayerActivity extends Activity {

    public static boolean active = false;

    public static final String TAG = "PlayerActivity";

    // For use within demo app code.
    public static final String CHANNEL_ID = "channel_id";
    public static final String STUN_SERVER_URL = "stun_server_url";
    public static final String BACKEND_URL = "backend_url";

    public static final int BUFFER_MIN = 30000;
    public static final int BUFFER_MAX = 30000;

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private Handler mainHandler;
    private EventLogger eventLogger;
    private SimpleExoPlayerView simpleExoPlayerView;

    private SimpleExoPlayer player = null;
    private DefaultTrackSelector trackSelector;

    private boolean shouldAutoPlay;

    private Uri contentUri;

    private long playerPosition;

    private String userAgent = "ExoPlayerPolyNetDemo";

    private PolyNet polyNet;

    public static boolean isActive() {
        return active;
    }

    // Activity lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shouldAutoPlay = true;
        mainHandler = new Handler();
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        setContentView(R.layout.player_activity);

        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.player_view);
        simpleExoPlayerView.setUseController(false);
        simpleExoPlayerView.requestFocus();
    }

    // Permission management methods

    /**
     * Checks whether it is necessary to ask for permission to read storage. If necessary, it also
     * requests permission.
     *
     * @return true if a permission request is made. False if it is not necessary.
     */
    @TargetApi(23)
    private boolean maybeRequestPermission() {
        if (requiresPermission(contentUri)) {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            return true;
        } else {
            return false;
        }
    }

    @TargetApi(23)
    private boolean requiresPermission(Uri uri) {
        return Util.SDK_INT >= 23
                && Util.isLocalFileUri(uri)
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onNewIntent(Intent intent) {
        releasePlayer();
        shouldAutoPlay = true;
        playerPosition = 0;
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        active = true;
        if (Util.SDK_INT > 23) {
            onShown();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || player == null)) {
            onShown();
        }
    }

    private void onShown() {
        Intent intent = getIntent();
        Uri manifestUri = intent.getData();
        String backendUrl = intent.getStringExtra(BACKEND_URL);
        String channelId = intent.getStringExtra(CHANNEL_ID);
        String stunServerUrl = intent.getStringExtra(STUN_SERVER_URL);
        if (polyNet == null) {
            try {
                // Connect to PolyNet
                PolyNetConfiguration configuration = PolyNetConfiguration.builder()
                    .setManifestUrl(manifestUri.toString().trim())
                    .setPolyNetBackendUrl(backendUrl.trim())
                    .setChannelId(Integer.parseInt(channelId.trim()))
                    .addStunServerUrl(stunServerUrl.trim())
                    .setContext(this)
                    .build();

                polyNet = new PolyNet(configuration);
                polyNet.setConnectionListener(polyNetConnectionListener);
                polyNet.setMetricsRequestListener(metricsRequestListener);
                polyNet.setDebugMode(true);
                polyNet.connect();
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error detected in the input parameters.", e);
                this.finish();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        //Do nothing.
    }

    // Activity input

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Show the controls on any key event.
        simpleExoPlayerView.showController();
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event);
    }

    // Internal methods

    private void handleConnectSuccess() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // When PolyNet is connected, use polyNet.getPolyNetManifestUrl() to initialize the player
                contentUri = Uri.parse(polyNet.getPolyNetManifestUrl());
                if (!maybeRequestPermission()) {
                    initializePlayer();
                    playerAddListener();
                }
            }
        });
    }

    private void initializePlayer() {
        if (player == null) {
            TrackSelection.Factory adaptiveTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
            trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
            eventLogger = new EventLogger(trackSelector, polyNet);
            DefaultLoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE), BUFFER_MIN, BUFFER_MAX,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);
            player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this), trackSelector, loadControl);
            player.addListener(eventLogger);
            MediaSource mediaSource = new HlsMediaSource(contentUri, buildDataSourceFactory(true), mainHandler, eventLogger);
            player.prepare(mediaSource);
            simpleExoPlayerView.setPlayer(player);
            player.setPlayWhenReady(shouldAutoPlay);
        }
    }

    private void playerAddListener() {
        player.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {

            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            }

            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                switch (error.type) {
                    case ExoPlaybackException.TYPE_SOURCE:
                        //Restart the player
                        releasePlayer();
                        active = true;
                        onShown();
                        break;
                }
            }

            @Override
            public void onPositionDiscontinuity() {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }
        });
    }

    private void releasePlayer() {
        if (player != null) {
            active = false;
            shouldAutoPlay = player.getPlayWhenReady();
            playerPosition = player.getCurrentPosition();
            player.release();
            player = null;
            trackSelector = null;
            eventLogger = null;
        }
        if (polyNet != null) {
            polyNet.dispose();
            polyNet = null;
        }
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    private DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);
    }

    private final PolyNetConnectionListener polyNetConnectionListener = new PolyNetConnectionListener() {

        @Override
        public void onConnected(PolyNet polyNet, String polyNetManifestUrl) {
            // When PolyNet is connected, we can initialize the player with polyNetManifestUrl or with polyNet.getPolyNetManifestUrl()
            handleConnectSuccess();
        }

        @Override
        public void onError(PolyNet polyNet, Throwable throwable) {
            Log.e(TAG, "PolyNet error", throwable.getCause());
        }

    };

    private final PolyNetMetricsRequestListener metricsRequestListener = new PolyNetMetricsRequestListener() {

        @Override
        public void onBufferHealthRequest(PolyNet polyNet) {
            if (player != null) {
                // Report the buffer health only if we can compute it
                long bufferPos = player.getBufferedPosition();
                long currentPos = player.getCurrentPosition();
                long bufferHealthInMs = bufferPos - currentPos;
                polyNet.reportBufferHealth(bufferHealthInMs);
            }
        }

        @Override
        public void onDroppedFramesRequest(PolyNet polyNet) {
            // No need to implement it for ExoPlayer.
        }

        @Override
        public void onPlayBackStartedRequest(PolyNet polyNet) {
            // No need to implement it for ExoPlayer.
        }
    };
}
