/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.organforce.futuretext;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TextDbAdapter {

    public static final String KEY_CONTENT = "content";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_TIME = "time";
    public static final String KEY_RECIPIENT = "recipient";
    public static final String KEY_REPETITION = "repetition";
    public static final String KEY_ROWID = "_id";
    
    public static final String KEY_SENT = "sent";

    private static final String TAG = "TextDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static final String DATABASE_PENDING_CREATE =
        "create table pending (_id integer primary key autoincrement, "
        + "content text not null, description text not null, "
        + "time bigint(64) not null, recipient text not null, " +
          "repetition integer not null);";

    private static final String DATABASE_HISTORY_CREATE =
        "create table history (_id integer primary key autoincrement, "
        + "recipient text not null, content text not null, "
        + "description text not null, time bigint(64) not null, "
        + "repetition integer not null, sent integer not null);";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_PENDING_TABLE = "pending";
    private static final String DATABASE_HISTORY_TABLE = "history";
    private static final int DATABASE_VERSION = 2;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_HISTORY_CREATE);
            db.execSQL(DATABASE_PENDING_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS pending");
            onCreate(db);
        }
    }

    public TextDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    public TextDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    public long createText(FutureText text) {
        ContentValues initialValues = text.toContentValues();
        return text._id = mDb.insert(DATABASE_PENDING_TABLE, null, initialValues);
    }

    public long addTextToHistory(FutureText text, boolean sent) {
        ContentValues initialValues = text.toContentValues();
        initialValues.put(KEY_TIME, System.currentTimeMillis());
        initialValues.put(KEY_SENT, sent? 1:0);
        return mDb.insert(DATABASE_HISTORY_TABLE, null, initialValues);
    }

    public boolean deleteText(long rowId) {
        return mDb.delete(DATABASE_PENDING_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    public boolean deleteTextFromHistory(long rowId) {
        return mDb.delete(DATABASE_HISTORY_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    public boolean clearHistory() {
        return mDb.delete(DATABASE_HISTORY_TABLE, null, null) > 0;
    }
    
    public Cursor fetchAllTexts() {
        return mDb.query(DATABASE_PENDING_TABLE, new String[] {KEY_ROWID, KEY_CONTENT,
        		KEY_DESCRIPTION, KEY_TIME, KEY_RECIPIENT, KEY_REPETITION}, null, null, null, null, KEY_TIME);
    }
    
    public Cursor fetchAllHistory() {
        return mDb.query(DATABASE_HISTORY_TABLE, new String[] {KEY_ROWID, KEY_CONTENT,
        		KEY_DESCRIPTION, KEY_TIME, KEY_RECIPIENT, KEY_REPETITION, KEY_SENT}, null, null, null, null, KEY_TIME + " desc");
    }

    public Cursor fetchText(long rowId) throws SQLException {
        Cursor mCursor =
            mDb.query(true, DATABASE_PENDING_TABLE, new String[] {KEY_ROWID, KEY_CONTENT, 
            	KEY_DESCRIPTION, KEY_TIME, KEY_RECIPIENT, KEY_REPETITION}, 
            	KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public Cursor fetchTextFromHistory(long rowId) throws SQLException {
        Cursor mCursor =
            mDb.query(true, DATABASE_HISTORY_TABLE, new String[] {KEY_ROWID, KEY_CONTENT, 
            	KEY_DESCRIPTION, KEY_TIME, KEY_RECIPIENT, KEY_REPETITION, KEY_SENT}, 
            	KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public boolean updateText(long rowId, FutureText text) {
        ContentValues args = text.toContentValues();
        return mDb.update(DATABASE_PENDING_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
	public FutureText fetchTextObject(long id) {
		Cursor cursorText = fetchText(id);
		FutureText text = new FutureText(
			cursorText.getString(
				cursorText.getColumnIndexOrThrow(TextDbAdapter.KEY_CONTENT)),
			cursorText.getString(
				cursorText.getColumnIndexOrThrow(TextDbAdapter.KEY_DESCRIPTION)),
			cursorText.getString(
				cursorText.getColumnIndexOrThrow(TextDbAdapter.KEY_RECIPIENT)),
			cursorText.getLong(
				cursorText.getColumnIndexOrThrow(TextDbAdapter.KEY_TIME)),
			cursorText.getInt(
				cursorText.getColumnIndexOrThrow(TextDbAdapter.KEY_REPETITION)));
		text._id = id;
								
		cursorText.close();
		
		return text;
	}
}
