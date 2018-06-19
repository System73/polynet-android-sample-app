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

import android.app.Activity;
import android.content.Intent;
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
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
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
import com.system73.polynet.android.sdk.PolyNetListener;
import com.system73.polynet.android.sdk.PolyNetMetrics;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class PlayerActivity extends Activity {

    public static boolean active = false;

    public static final String TAG = "PlayerActivity";

    public static final String CHANNEL_ID = "channel_id";
    public static final String API_KEY = "api_key";

    public static final int BUFFER_MIN = 30000;
    public static final int BUFFER_MAX = 30000;

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private Handler mainHandler;
    private SimpleExoPlayerView simpleExoPlayerView;

    private SimpleExoPlayer player = null;
    private DefaultTrackSelector trackSelector;

    private boolean shouldAutoPlay;

    private Uri contentUri;

    private String userAgent = "ExoPlayerPolyNetSample";

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

    @Override
    public void onNewIntent(Intent intent) {
        releasePlayer();
        shouldAutoPlay = true;
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
        String channelId = intent.getStringExtra(CHANNEL_ID);
        String apiKey = intent.getStringExtra(API_KEY);
        if (polyNet == null) {
            try {
                // Connect to PolyNet
                PolyNetConfiguration configuration = PolyNetConfiguration.builder()
                    .setManifestUrl(manifestUri.toString().trim())
                    .setChannelId(channelId.trim())
                    .setApiKey(apiKey.trim())
                    .setContext(this)
                    .build();

                polyNet = new PolyNet(configuration);

                contentUri = Uri.parse(polyNet.getLocalManifestUrl());

                polyNet.setDebugMode(true);

                polyNet.setListener(polyNetListener);
                // New integration flow: Player can be initialized here, waiting for polyNet
                // connection is no longer needed.
                initializePlayer(contentUri);
                addPlayerErrorListener();

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

    private void handleConnectSuccess(final String polyNetManifestUrl) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // When PolyNet is connected, use the PolyNetManifestUrl to initialize the player
                Uri contentUri = Uri.parse(polyNetManifestUrl);
                initializePlayer(contentUri);
            }
        });
    }

    private void initializePlayer(final Uri contentUri) {
        if (player == null) {
            TrackSelection.Factory adaptiveTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
            trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
            DefaultLoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE), BUFFER_MIN, BUFFER_MAX,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);
            player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this), trackSelector, loadControl);
            addPlayBackStartedListener();
            setDroppedFramesListener();
            addPlayerErrorListener();
            MediaSource mediaSource = new HlsMediaSource(contentUri, buildDataSourceFactory(true), mainHandler, new AdaptiveMediaSourceEventAdapter());
            player.prepare(mediaSource);
            simpleExoPlayerView.setPlayer(player);
            player.setPlayWhenReady(shouldAutoPlay);
        }
    }

    private void addPlayBackStartedListener() {
        player.addListener(new PlayerEventAdapter() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int state) {
                if (state == Player.STATE_READY) {
                    if (polyNet != null) {
                        polyNet.reportPlayBackStarted();
                    }
                }
            }
        });
    }

    private void setDroppedFramesListener() {
        player.setVideoDebugListener(new VideoRendererEventAdapter() {
            @Override
            public void onDroppedFrames(int count, long elapsed) {
                for (int n = 0; n < count; n++) {
                    if (polyNet != null) {
                        polyNet.reportDroppedFrame();
                    }
                }
            }
        });
    }

    private void addPlayerErrorListener() {
        player.addListener(new PlayerEventAdapter() {
            @Override
            public void onPlayerError(ExoPlaybackException error) {
                switch (error.type) {
                    case ExoPlaybackException.TYPE_SOURCE:
                        // Restart the player
                        releasePlayer();
                        active = true;
                        onShown();
                        break;
                }
            }
        });
    }

    private void releasePlayer() {
        if (player != null) {
            active = false;
            shouldAutoPlay = player.getPlayWhenReady();
            player.release();
            player = null;
            trackSelector = null;
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

    private final PolyNetListener polyNetListener = new PolyNetListener() {
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
        public void onMetrics(PolyNetMetrics polyNetMetrics) {
            // Public metrics
        }

        @Override
        public void onDroppedFramesRequest(PolyNet polyNet) {
            // No need to implement it for ExoPlayer.
        }

        @Override
        public void onPlayBackStartedRequest(PolyNet polyNet) {
            // No need to implement it for ExoPlayer.
        }

        @Override
        public void onError(PolyNet polyNet, Throwable throwable) {
            Log.e(TAG, "PolyNet error", throwable.getCause());
        }

    };
}
