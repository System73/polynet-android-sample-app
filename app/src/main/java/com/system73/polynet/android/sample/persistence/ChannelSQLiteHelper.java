package com.system73.polynet.android.sample.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ChannelSQLiteHelper extends SQLiteOpenHelper {

    private static final String channelSQL = "CREATE TABLE " + ChannelDB.CHANNELS_TABLE_NAME +
            " (" + ChannelDB.ID_COLUMN_NAME + " INTEGER PRIMARY KEY NOT NULL, " +
            ChannelDB.CHANNEL_ID_COLUMN_NAME + " INTEGER NOT NULL, " +
            ChannelDB.MANIFEST_URL_COLUMN_NAME + " TEXT NOT NULL, " +
            ChannelDB.BACKEND_URL_COLUMN_NAME + " TEXT NOT NULL, " +
            ChannelDB.BACKEND_METRICS_URL_COLUMN_NAME + " TEXT NOT NULL, " +
            ChannelDB.STUN_SERVER_URL_COLUMN_NAME + " TEXT NOT NULL)";

    public ChannelSQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(channelSQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ChannelDB.CHANNELS_TABLE_NAME);
        db.execSQL(channelSQL);
    }
}
