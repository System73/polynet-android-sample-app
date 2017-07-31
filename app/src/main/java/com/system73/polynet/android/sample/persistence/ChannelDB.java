package com.system73.polynet.android.sample.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.system73.polynet.android.sample.model.Channel;

public class ChannelDB {

    private final int ID_DATABASE = 1;

    private ChannelSQLiteHelper channelHelper;
    private SQLiteDatabase db;

    public ChannelDB(ChannelSQLiteHelper channelHelper) {
        this.channelHelper = channelHelper;
        db = this.channelHelper.getWritableDatabase();
    }

    public void setChannel(Channel channel, int channelId, String manifestUrl, String backendUrl, String stunServerUrl) {
        if (db != null) {
            ContentValues newChannel = new ContentValues();
            newChannel.put("channelId", channelId);
            newChannel.put("manifestUrl", manifestUrl);
            newChannel.put("backendUrl", backendUrl);
            newChannel.put("stunServerUrl", stunServerUrl);

            if (channel == null) {
                newChannel.put("id", ID_DATABASE);
                db.insert("Channels", null, newChannel);
            } else {
                db.update("Channels", newChannel, "id=" + channel.getId(), null);
            }
        }
    }

    public Channel getChannel() {
        Cursor cursor = db.rawQuery("select * from Channels", null);
        Channel channel = null;
        if (cursor.moveToFirst()) {
            channel = new Channel();
            channel.setId(cursor.getInt(cursor.getColumnIndex("id")));
            channel.setChannelId(cursor.getInt(cursor.getColumnIndex("channelId")));
            channel.setManifestUrl(cursor.getString(cursor.getColumnIndex("manifestUrl")));
            channel.setBackendUrl(cursor.getString(cursor.getColumnIndex("backendUrl")));
            channel.setStunServerUrl(cursor.getString(cursor.getColumnIndex("stunServerUrl")));
        }
        return channel;
    }

    public void close() {
        db.close();
    }
}
