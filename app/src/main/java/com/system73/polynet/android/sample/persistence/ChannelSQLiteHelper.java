package com.system73.polynet.android.sample.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ChannelSQLiteHelper extends SQLiteOpenHelper {

    String channelSQL = "CREATE TABLE Channels (id INTEGER PRIMARY KEY NOT NULL, channelId INTEGER NOT NULL, manifestUrl TEXT NOT NULL, backendUrl TEXT NOT NULL, stunServerUrl TEXT NOT NULL)";

    public ChannelSQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(channelSQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Channels");
        db.execSQL(channelSQL);
    }
}
