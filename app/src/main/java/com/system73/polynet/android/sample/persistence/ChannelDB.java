package com.system73.polynet.android.sample.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.system73.polynet.android.sample.model.Channel;

public class ChannelDB {

    private static final String DATABASE_FILE_NAME = "ChannelPersistence";

    private static final int DATABASE_VERSION = 2;

    private static final int DEFAULT_ID = 1;

    static final String CHANNELS_TABLE_NAME = "Channels";

    static final String ID_COLUMN_NAME = "id";
    static final String CHANNEL_ID_COLUMN_NAME = "channelId";
    static final String MANIFEST_URL_COLUMN_NAME = "manifestUrl";
    static final String BACKEND_URL_COLUMN_NAME = "backendUrl";
    static final String BACKEND_METRICS_URL_COLUMN_NAME = "backendMetricsUrl";
    static final String STUN_SERVER_URL_COLUMN_NAME = "stunServerUrl";

    private SQLiteDatabase db;

    public ChannelDB(Context context) {
        ChannelSQLiteHelper channelHelper = new ChannelSQLiteHelper(context, DATABASE_FILE_NAME, null, DATABASE_VERSION);
        db = channelHelper.getWritableDatabase();
    }

    public void setChannel(Channel channel, int channelId, String manifestUrl, String backendUrl, String backendMetricsUrl, String stunServerUrl) {
        if (db != null) {
            ContentValues newChannel = new ContentValues();
            newChannel.put(CHANNEL_ID_COLUMN_NAME, channelId);
            newChannel.put(MANIFEST_URL_COLUMN_NAME, manifestUrl);
            newChannel.put(BACKEND_URL_COLUMN_NAME, backendUrl);
            newChannel.put(BACKEND_METRICS_URL_COLUMN_NAME, backendMetricsUrl);
            newChannel.put(STUN_SERVER_URL_COLUMN_NAME, stunServerUrl);

            if (channel == null) {
                newChannel.put(ID_COLUMN_NAME, DEFAULT_ID);
                db.insert(CHANNELS_TABLE_NAME, null, newChannel);
            } else {
                db.update(CHANNELS_TABLE_NAME, newChannel, ID_COLUMN_NAME + "=" + channel.getId(), null);
            }
        }
    }

    public Channel getChannel() {
        Cursor cursor = db.rawQuery("select * from " + CHANNELS_TABLE_NAME, null);
        Channel channel = null;
        if (cursor.moveToFirst()) {
            channel = new Channel();
            channel.setId(cursor.getInt(cursor.getColumnIndex(ID_COLUMN_NAME)));
            channel.setChannelId(cursor.getInt(cursor.getColumnIndex(CHANNEL_ID_COLUMN_NAME)));
            channel.setManifestUrl(cursor.getString(cursor.getColumnIndex(MANIFEST_URL_COLUMN_NAME)));
            channel.setBackendUrl(cursor.getString(cursor.getColumnIndex(BACKEND_URL_COLUMN_NAME)));
            channel.setBackendMetricsUrl(cursor.getString(cursor.getColumnIndex(BACKEND_METRICS_URL_COLUMN_NAME)));
            channel.setStunServerUrl(cursor.getString(cursor.getColumnIndex(STUN_SERVER_URL_COLUMN_NAME)));
        }
        return channel;
    }

    public void close() {
        db.close();
    }
}
