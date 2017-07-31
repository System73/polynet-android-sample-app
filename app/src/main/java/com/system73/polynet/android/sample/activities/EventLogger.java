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

import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.system73.polynet.android.sdk.PolyNet;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Logs player events using {@link Log}.
 */
/* package */ final class EventLogger implements ExoPlayer.EventListener, VideoRendererEventListener, AdaptiveMediaSourceEventListener {

    private static final String TAG = "EventLogger";
    private static final int MAX_TIMELINE_ITEM_LINES = 3;
    private static final NumberFormat TIME_FORMAT;

    static {
        TIME_FORMAT = NumberFormat.getInstance(Locale.US);
        TIME_FORMAT.setMinimumFractionDigits(2);
        TIME_FORMAT.setMaximumFractionDigits(2);
        TIME_FORMAT.setGroupingUsed(false);
    }

    private final MappingTrackSelector trackSelector;
    private final Timeline.Window window;
    private final Timeline.Period period;
    private final long startTimeMs;

    private PolyNet polyNet;

    public EventLogger(MappingTrackSelector trackSelector, PolyNet polyNet) {
        this.trackSelector = trackSelector;
        window = new Timeline.Window();
        period = new Timeline.Period();
        startTimeMs = SystemClock.elapsedRealtime();
        this.polyNet = polyNet;
    }

    // ExoPlayer.EventListener

    @Override
    public void onLoadingChanged(boolean isLoading) {
        //Do nothing.
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        if (state == ExoPlayer.STATE_READY) {
            polyNet.reportPlayBackStarted();
        }
    }

    @Override
    public void onPositionDiscontinuity() {
        //Do nothing.
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        //Do nothing.
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        //Do nothing.
    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {
        //Log.e(TAG, "playerFailed [" + getSessionTimeString() + "]", e);
    }

    @Override
    public void onTracksChanged(TrackGroupArray ignored, TrackSelectionArray trackSelections) {
        //Do nothing.
    }

    // VideoRendererEventListener

    @Override
    public void onVideoEnabled(DecoderCounters counters) {
        //Do nothing.
    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                                          long initializationDurationMs) {
        //Do nothing.
    }

    @Override
    public void onVideoInputFormatChanged(Format format) {
        //Do nothing.
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {
        //Do nothing.
    }

    @Override
    public void onDroppedFrames(int count, long elapsed) {
        for (int n = 0; n < count; n++) {
            polyNet.reportDroppedFrame();
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                   float pixelWidthHeightRatio) {
        // Do nothing.
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
        //Do nothing.
    }

    @Override
    public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs) {
        //Do nothing.
    }

    @Override
    public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
        //Do nothing.
    }

    @Override
    public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
        //Do nothing.
    }

    @Override
    public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {
        //Do nothing.
    }

    @Override
    public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {
        //Do nothing.
    }

    @Override
    public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeMs) {
        //Do nothing.
    }
}
