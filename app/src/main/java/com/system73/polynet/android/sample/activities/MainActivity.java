package com.system73.polynet.android.sample.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.system73.polynet.android.sample.BuildConfig;
import com.system73.polynet.android.sample.R;
import com.system73.polynet.android.sample.model.Channel;
import com.system73.polynet.android.sample.persistence.ChannelDB;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Channel channel;

    private EditText manifestUrlText;
    private EditText channelIdText;
    private EditText backendUrlText;
    private EditText backendMetricsUrlText;
    private EditText stunServerUrlText;

    private ChannelDB channelDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView sampleAppVersionTextView = (TextView) findViewById(R.id.polynet_sample_app_version);
        sampleAppVersionTextView.setText(getString(R.string.sample_app_version_base_text) + BuildConfig.VERSION_NAME);

        TextView polyNetSdkVersionTextView = (TextView) findViewById(R.id.polynet_sdk_version);
        polyNetSdkVersionTextView.setText(getString(R.string.polynet_sdk_version_base_text) + com.system73.polynet.android.sdk.BuildConfig.VERSION_NAME);

        TextView companyUrlTextView =(TextView)findViewById(R.id.company_url_text_view);
        companyUrlTextView.setClickable(true);
        companyUrlTextView.setMovementMethod(LinkMovementMethod.getInstance());
        String text = "<a href='http://" + getResources().getString(R.string.company_url) + "'> " + getResources().getString(R.string.company_url_label) + " </a>";
        companyUrlTextView.setText(Html.fromHtml(text));

        manifestUrlText = (EditText) findViewById(R.id.manifest_url);
        channelIdText = (EditText) findViewById(R.id.channel_id);
        backendUrlText = (EditText) findViewById(R.id.backend_url);
        backendMetricsUrlText = (EditText) findViewById(R.id.backend_metrics_url);
        stunServerUrlText = (EditText) findViewById(R.id.stun_server_url);

        channelDB = new ChannelDB(this);

        channel = channelDB.getChannel();
        if (channel != null) {
            setParametersFromStoredChannel();
        }
    }

    public void play (View view) {
        String manifestUrl = manifestUrlText.getText().toString().trim();
        String channelId = channelIdText.getText().toString().trim();
        String backendUrl = backendUrlText.getText().toString().trim();
        String backendMetricsUrl = backendMetricsUrlText.getText().toString().trim();
        String stunServerUrl = stunServerUrlText.getText().toString().trim();

        if (checkFieldsAreValid(manifestUrl, channelId, backendUrl, backendMetricsUrl, stunServerUrl)) {
            channelDB.setChannel(channel, Integer.parseInt(channelId), manifestUrl, backendUrl, backendMetricsUrl, stunServerUrl);
            launchVideoActivity(manifestUrl, channelId, backendUrl, backendMetricsUrl, stunServerUrl);
        }
    }

    public void launchVideoActivity(String manifestUrl, String channelId, String backendUrl, String backendMetricsUrl, String stunServerUrl) {
        if (PlayerActivity.isActive()) {
            return;
        }

        Intent mpdIntent = new Intent(this, PlayerActivity.class)
            .setData(Uri.parse(manifestUrl))
            .putExtra(PlayerActivity.CHANNEL_ID, channelId)
            .putExtra(PlayerActivity.BACKEND_URL, backendUrl)
            .putExtra(PlayerActivity.BACKEND_METRICS_URL, backendMetricsUrl)
            .putExtra(PlayerActivity.STUN_SERVER_URL, stunServerUrl);
        startActivity(mpdIntent);
    }


    private void setParametersFromStoredChannel() {
        manifestUrlText.setText(channel.getManifestUrl());
        channelIdText.setText(String.valueOf(channel.getChannelId()));
        backendUrlText.setText(channel.getBackendUrl());
        backendMetricsUrlText.setText(channel.getBackendMetricsUrl());
        stunServerUrlText.setText(channel.getStunServerUrl());
    }

    @Override
    protected void onDestroy() {
        channelDB.close();
        super.onDestroy();
    }

    private boolean checkFieldsAreValid(String manifestUrl, String channelId, String backendUrl, String backendMetricsUrl, String stunServerUrl) {
        boolean valid = true;

        if (manifestUrl.isEmpty()) {
            Log.e(TAG, "playerFailed [Empty field: MANIFEST URL]");
            valid = false;
        }

        if (channelId.isEmpty()) {
            Log.e(TAG, "playerFailed [Empty field: CHANNEL ID]");
            valid = false;
        }

        try {
            if (Integer.parseInt(channelId) < 0) {
                Log.e(TAG, "playerFailed [CHANNEL ID must be a positive integer number]");
                valid = false;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "playerFailed [CHANNEL ID must be an integer number]");
            valid = false;
        }

        if (backendUrl.isEmpty()) {
            Log.e(TAG, "playerFailed [Empty field: BACKEND URL]");
            valid = false;
        }

        if (backendMetricsUrl.isEmpty()) {
            Log.e(TAG, "playerFailed [Empty field: BACKEND METRICS URL]");
            valid = false;
        }

        if (stunServerUrl.isEmpty()) {
            Log.e(TAG, "playerFailed [Empty field: STUN SERVER URL]");
            valid = false;
        }

        return valid;
    }
}
