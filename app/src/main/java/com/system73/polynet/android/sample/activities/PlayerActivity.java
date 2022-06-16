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
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoSize;
import com.system73.polynet.android.sample.R;
import com.system73.polynet.android.sdk.PolyNet;
import com.system73.polynet.android.sdk.PolyNetAnalyticsListener;
import com.system73.polynet.android.sdk.PolyNetConfiguration;
import com.system73.polynet.android.sdk.PolyNetExoPlayerWrapper;
import com.system73.polynet.android.sdk.PolyNetPlayerListener;
import com.system73.polynet.android.sdk.core.metrics.PlayerState;
import com.system73.polynet.android.sdk.core.metrics.PolyNetMetrics;
import com.system73.polynet.android.sdk.exception.PolyNetException;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

/**
 * An activity that plays media using {@link ExoPlayer}.
 */
public class PlayerActivity extends Activity {

    public static boolean active = false;

    public static final String TAG = "PlayerActivity";

    public static final String CHANNEL_ID = "channel_id";
    public static final String API_KEY = "api_key";

    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private StyledPlayerView playerView;
    private ExoPlayer player = null;
    private boolean shouldAutoPlay;
    private PolyNet polyNet;

    public static boolean isActive() {
        return active;
    }

     // Listeners

    /**
     * Listener for analytics reported by the SDK
     */
    private final PolyNetAnalyticsListener polyNetAnalyticsListener = new PolyNetAnalyticsListener() {
        @Override
        public void onError(PolyNet polyNet, PolyNetException e) {
            Log.e(TAG, "PolyNet error", e);
        }

        @Override
        public void onMetrics(PolyNetMetrics polyNetMetrics) {
            // Public metrics
        }
    };

    /**
     * Listener for metric requests by the SDK
     */
    private final PolyNetPlayerListener polyNetPlayerListener = new PolyNetPlayerListener() {
        @Override
        public void onPlayerStateRequest(PolyNet polyNet, final PlayerState oldPlayerState) {
            runOnUiThread(() -> {
                if (player != null) {
                    boolean isPlaying = player.isPlaying();
                    int playbackState = player.getPlaybackState();
                    PlayerState currentPlayerState;

                    if ((oldPlayerState == PlayerState.UNKNOWN || oldPlayerState == PlayerState.STARTING) && playbackState == Player.STATE_BUFFERING) {
                        currentPlayerState = PlayerState.STARTING;
                    } else if (isPlaying) {
                        currentPlayerState = PlayerState.PLAYING;
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        currentPlayerState = PlayerState.BUFFERING;
                    } else if (playbackState == Player.STATE_READY) {
                        currentPlayerState = PlayerState.PAUSE;
                    } else {
                        currentPlayerState = PlayerState.UNKNOWN;
                    }

                    polyNet.reportPlayerState(currentPlayerState);
                }
            });
        }

        @Override
        public void onBufferHealthRequest(PolyNet polyNet) {
            runOnUiThread(() -> {
                if (player != null) {
                    // Report the buffer health only if we can compute it
                    long bufferHealthInMs = player.getTotalBufferedDuration();
                    polyNet.reportBufferHealth(bufferHealthInMs);
                }
            });
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

    /**
     * Listener for analytics/events reported by ExoPlayer
     */
    private final AnalyticsListener analyticsListener = new AnalyticsListener() {
        @Override
        public void onPlaybackStateChanged(@NonNull EventTime eventTime, int state) {
            if (state == Player.STATE_READY) {
                if (polyNet != null) {
                    polyNet.reportPlayBackStarted();
                }
            }
        }

        @Override
        public void onDroppedVideoFrames(@NonNull EventTime eventTime, int count, long elapsed) {
            for (int n = 0; n < count; n++) {
                if (polyNet != null) {
                    polyNet.reportDroppedFrame();
                }
            }
        }

        @Override
        public void onSurfaceSizeChanged(@NonNull EventTime eventTime, int width, int height) {
            polyNet.reportViewportSize(width, height);
        }

        @Override
        public void onVideoSizeChanged(@NonNull EventTime eventTime, VideoSize videoSize) {
            polyNet.reportVideoResolution(videoSize.width, videoSize.height);
        }
    };

    /**
     * Listener for errors reported by ExoPlayer
     */
    AnalyticsListener errorListener = new AnalyticsListener() {
        @Override
        public void onPlayerError(@NonNull EventTime eventTime, @NonNull PlaybackException error) {
        if (error instanceof ExoPlaybackException &&
                ((ExoPlaybackException) error).type == ExoPlaybackException.TYPE_SOURCE) {
                // Restart the player
                releasePlayer();
                active = true;
                onShown();
            }
        }
    };

    // Activity lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shouldAutoPlay = true;
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        setContentView(R.layout.player_activity);

        playerView = findViewById(R.id.player_view);
        playerView.setUseController(false);
        playerView.requestFocus();
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
                PolyNetConfiguration.Builder configurationBuilder = PolyNetConfiguration.builder()
                        .setManifestUrl(manifestUri.toString().trim())
                        .setChannelId(channelId.trim())
                        .setApiKey(apiKey.trim())
                        .setContext(this);

                polyNet = new PolyNet(configurationBuilder.build());

                initializePlayer(polyNet.getLocalManifestUrl());

                integratePlayerUsingPlugin();
//                integratePlayerManually();

                polyNet.setAnalyticsListener(polyNetAnalyticsListener);
            } catch (Exception e) {
                Log.e(TAG, "Error detected in the input parameters.", e);
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }

    /**
     * Passing the player to the SDK using the plugin.
     */
    void integratePlayerUsingPlugin(){
        polyNet.setPlayer(new PolyNetExoPlayerWrapper(player));
    }

    /**
     * Manually passing the player metrics and info to the SDK
     */
    void integratePlayerManually(){
        polyNet.reportPlayerName("exoplayer");
        polyNet.reportPlayerVersion(ExoPlayerLibraryInfo.VERSION);
        polyNet.setPlayerListener(polyNetPlayerListener);

        player.addAnalyticsListener(analyticsListener);
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
        playerView.showController();
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || playerView.dispatchMediaKeyEvent(event);
    }

    // Internal methods

    private void initializePlayer(String contentUri) {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            MediaItem mediaItem = MediaItem.fromUri(contentUri);
            player.setMediaItem(mediaItem);
            player.prepare();
            playerView.setPlayer(player);
            player.setPlayWhenReady(shouldAutoPlay);
            player.addAnalyticsListener(errorListener);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            active = false;
            shouldAutoPlay = player.getPlayWhenReady();
            player.removeAnalyticsListener(analyticsListener);
            player.removeAnalyticsListener(errorListener);
            player.release();
            player = null;
            playerView.setPlayer(null);
        }
        if (polyNet != null) {
            polyNet.dispose();
            polyNet = null;
        }
    }
}