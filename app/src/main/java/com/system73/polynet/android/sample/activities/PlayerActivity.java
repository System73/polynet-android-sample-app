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
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Util;
import com.system73.polynet.android.sample.R;
import com.system73.polynet.android.sdk.PolyNet;
import com.system73.polynet.android.sdk.PolyNetConfiguration;
import com.system73.polynet.android.sdk.PolyNetListener;
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

    private PlayerView playerView;

    private ExoPlayer player = null;

    private boolean shouldAutoPlay;

    private Uri contentUri;

    private PolyNet polyNet;

    public static boolean isActive() {
        return active;
    }

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

                contentUri = Uri.parse(polyNet.getLocalManifestUrl());

                polyNet.setListener(polyNetListener);
                // New integration flow: Player can be initialized here, waiting for polyNet
                // connection is no longer needed.
                initializePlayer();

            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error detected in the input parameters.", e);
                Toast.makeText(this,e.getMessage(), Toast.LENGTH_LONG).show();
                this.finish();
            } catch (Exception e) {
                Log.e(TAG, "Error detected in the input parameters.", e);
                Toast.makeText(this,e.getMessage(), Toast.LENGTH_LONG).show();
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
        playerView.showController();
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || playerView.dispatchMediaKeyEvent(event);
    }

    // Internal methods

    private void initializePlayer() {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            MediaItem mediaItem = MediaItem.fromUri(contentUri);
            player.setMediaItem(mediaItem);
            addAnalyticsListener();
            player.prepare();
            playerView.setPlayer(player);
            player.setPlayWhenReady(shouldAutoPlay);
        }
    }

    private void addAnalyticsListener() {
        player.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onPlaybackStateChanged(EventTime eventTime, int state) {
                if (state == Player.STATE_READY) {
                    if (polyNet != null) {
                        polyNet.reportPlayBackStarted();
                    }
                }
            }

            @Override
            public void onDroppedVideoFrames(EventTime eventTime, int count, long elapsed) {
                for (int n = 0; n < count; n++) {
                    if (polyNet != null) {
                        polyNet.reportDroppedFrame();
                    }
                }
            }

            @Override
            public void onPlayerError(EventTime eventTime, PlaybackException error) {
                if (error instanceof ExoPlaybackException && ((ExoPlaybackException) error).type == ExoPlaybackException.TYPE_SOURCE) {
                    // Restart the player
                    releasePlayer();
                    active = true;
                    onShown();
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
        }
        if (polyNet != null) {
            polyNet.dispose();
            polyNet = null;
        }
    }

    private final PolyNetListener polyNetListener = new PolyNetListener() {
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
        public void onError(PolyNet polyNet, PolyNetException e) {
            Log.e(TAG, "PolyNet error", e);
        }
    };
}