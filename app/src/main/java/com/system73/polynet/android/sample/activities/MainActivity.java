package com.system73.polynet.android.sample.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.system73.polynet.android.sample.BuildConfig;
import com.system73.polynet.android.sample.R;
import com.system73.polynet.android.sample.model.Channel;
import com.system73.polynet.android.sample.persistence.ChannelDB;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Channel channel;

    private EditText manifestUrlText;
    private EditText channelIdText;
    private EditText apiKeyText;

    private ChannelDB channelDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView sampleAppVersionTextView = findViewById(R.id.polynet_sample_app_version);
        sampleAppVersionTextView.setText(getString(R.string.sample_app_version_base_text) + BuildConfig.VERSION_NAME);

        TextView polyNetSdkVersionTextView = findViewById(R.id.polynet_sdk_version);
        polyNetSdkVersionTextView.setText(getString(R.string.polynet_sdk_version_base_text) + com.system73.polynet.android.sdk.BuildConfig.VERSION_NAME);

        TextView companyUrlTextView = findViewById(R.id.company_url_text_view);
        companyUrlTextView.setClickable(true);
        companyUrlTextView.setMovementMethod(LinkMovementMethod.getInstance());
        String text = "<a href='http://" + getResources().getString(R.string.company_url) + "'> " + getResources().getString(R.string.company_url_label) + " </a>";
        companyUrlTextView.setText(Html.fromHtml(text));

        manifestUrlText = findViewById(R.id.manifest_url);
        channelIdText = findViewById(R.id.channel_id);
        apiKeyText = findViewById(R.id.api_key);

        channelDB = new ChannelDB(this);

        channel = channelDB.getChannel();
        if (channel != null) {
            setParametersFromStoredChannel();
        }
    }

    public void play (View view) {
        String manifestUrl = manifestUrlText.getText().toString().trim();
        String channelId = channelIdText.getText().toString().trim();
        String apiKey = apiKeyText.getText().toString().trim();

        if (checkFieldsAreValid(manifestUrl, channelId, apiKey)) {
            channelDB.setChannel(channel, channelId, manifestUrl, apiKey);
            launchVideoActivity(manifestUrl, channelId, apiKey);
        }
    }

    public void launchVideoActivity(String manifestUrl, String channelId, String apiKey) {
        if (PlayerActivity.isActive()) {
            return;
        }

        Intent mpdIntent = new Intent(this, PlayerActivity.class)
            .setData(Uri.parse(manifestUrl))
            .putExtra(PlayerActivity.CHANNEL_ID, channelId)
            .putExtra(PlayerActivity.API_KEY, apiKey);
        startActivity(mpdIntent);
    }


    private void setParametersFromStoredChannel() {
        manifestUrlText.setText(channel.getManifestUrl());
        channelIdText.setText(String.valueOf(channel.getChannelId()));
        apiKeyText.setText(channel.getApiKey());
    }

    @Override
    protected void onDestroy() {
        channelDB.close();
        super.onDestroy();
    }

    private boolean checkFieldsAreValid(String manifestUrl, String channelId, String apiKey) {
        boolean valid = true;

        if (manifestUrl.isEmpty()) {
            Log.e(TAG, "playerFailed [Empty field: MANIFEST URL]");
            valid = false;
        }

        if (channelId.isEmpty()) {
            Log.e(TAG, "playerFailed [Empty field: CHANNEL ID]");
            valid = false;
        }

        if (apiKey.isEmpty()) {
            Log.e(TAG, "playerFailed [Empty field: API KEY]");
            valid = false;
        }

        return valid;
    }
}
