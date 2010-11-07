package com.organforce.futuretext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

public class BootReceiver extends BroadcastReceiver {
	private TextDbAdapter db;

	@Override
	public void onReceive(Context context, Intent i) {
		db = new TextDbAdapter(context);
		db.open();

		Cursor c = db.fetchAllTexts();
		if(c.getCount() > 0){
			c.moveToFirst();
			do {
				ScheduledTextService.scheduleFutureText(context, 
						c.getLong(c.getColumnIndexOrThrow(TextDbAdapter.KEY_ROWID)),
						c.getLong(c.getColumnIndexOrThrow(TextDbAdapter.KEY_TIME)));
			} while(c.moveToNext());
		}
		c.close();
		db.close();
	}

}
