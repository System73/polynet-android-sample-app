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

import com.system73.polynet.android.sample.R;
import com.system73.polynet.android.sample.model.Channel;
import com.system73.polynet.android.sample.persistence.ChannelDB;
import com.system73.polynet.android.sample.persistence.ChannelSQLiteHelper;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private final int SQL_DATABASE_VERSION = 1;

    private Channel channel;

    private EditText channelIdText;
    private EditText manifestUrlText;
    private EditText backendUrlText;
    private EditText stunServerUrlText;

    private ChannelSQLiteHelper channelHelper;
    private ChannelDB channelDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView companyUrlTextView =(TextView)findViewById(R.id.company_url_text_view);
        companyUrlTextView.setClickable(true);
        companyUrlTextView.setMovementMethod(LinkMovementMethod.getInstance());
        String text = "<a href='http://" + getResources().getString(R.string.company_url) + "'> " + getResources().getString(R.string.company_url_label) + " </a>";
        companyUrlTextView.setText(Html.fromHtml(text));

        channelIdText = (EditText) findViewById(R.id.channel_id);
        manifestUrlText = (EditText) findViewById(R.id.manifest_url_text);
        backendUrlText = (EditText) findViewById(R.id.infrastructure_ws_url_text);
        stunServerUrlText = (EditText) findViewById(R.id.stun_server_uri);

        channelHelper = new ChannelSQLiteHelper(this, "ChannelPersistence", null, SQL_DATABASE_VERSION);
        channelDB = new ChannelDB(channelHelper);

        channel = channelDB.getChannel();
        if (channel != null) {
            setTextToView();
        }
    }

    public void play (View view) {

        String channelId = channelIdText.getText().toString();
        String manifestUrl = manifestUrlText.getText().toString();
        String backendUrl = backendUrlText.getText().toString();
        String stunServerUrl = stunServerUrlText.getText().toString();

        if (checkFieldsAreValid(channelId, manifestUrl, backendUrl, stunServerUrl)) {
            channelDB.setChannel(channel, Integer.parseInt(channelId), manifestUrl, backendUrl, stunServerUrl);
            launchVideoActivity(backendUrl, manifestUrl, channelId, stunServerUrl);
        }
    }

    public void launchVideoActivity(String backendUrl, String manifestUrl, String channelId, String stunServerUrl) {
        if (PlayerActivity.isActive()) {
            return;
        }

        Intent mpdIntent = new Intent(this,PlayerActivity.class)
                .setData(Uri.parse(manifestUrl))
                .putExtra(PlayerActivity.STUN_SERVER_URI, stunServerUrl)
                .putExtra(PlayerActivity.CHANNEL_ID, channelId)
                .putExtra(PlayerActivity.BACKEND_URL, backendUrl);
        startActivity(mpdIntent);
    }


    private void setTextToView() {
        channelIdText.setText(String.valueOf(channel.getChannelId()));
        manifestUrlText.setText(channel.getManifestUrl());
        backendUrlText.setText(channel.getBackendUrl());
        stunServerUrlText.setText(channel.getStunServerUrl());
    }

    @Override
    protected void onDestroy() {
        channelDB.close();
        super.onDestroy();
    }

    private boolean checkFieldsAreValid(String channelId, String manifestUrl, String backendUrl, String stunServerUrl) {
        boolean valid = true;

        if (channelId.isEmpty()) {
            Log.e(TAG, "playerFailed [Empty field: CHANNEL ID]");
            valid = false;
        }
        if (manifestUrl.isEmpty()) {
            Log.e(TAG, "playerFailed [Empty field: MANIFEST URL]");
            valid = false;
        }
        if (backendUrl.isEmpty()) {
            Log.e(TAG, "playerFailed [Empty field: BACKEND URL]");
            valid = false;
        }
        if (stunServerUrl.isEmpty()) {
            Log.e(TAG, "playerFailed [Empty field: STUN SERVER URI]");
            valid = false;
        }
        return valid;
    }
}
