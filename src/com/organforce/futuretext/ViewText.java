package com.organforce.futuretext;

import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class ViewText extends Activity {
    private Long mRowId;
	private TextDbAdapter db;
	
	private TextView contact;
	private TextView time;
	private TextView message;
	private TextView notes;
	private TextView sent;
	private TextView repetition;
	private int repInt;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_text);
        
        db = new TextDbAdapter(this);
		db.open(); 
        
        contact = (TextView) findViewById(R.id.view_contact);
        time = (TextView) findViewById(R.id.view_time);
        message = (TextView) findViewById(R.id.view_message);
        notes = (TextView) findViewById(R.id.view_notes);
        sent = (TextView) findViewById(R.id.view_sent);
        repetition = (TextView) findViewById(R.id.view_repetition);
    }

    @Override
	protected void onStart() {
		super.onStart();

        
        if(ScheduledTextService.mNotificationManager != null)
        	ScheduledTextService.mNotificationManager.cancel(ScheduledTextService.NOTIFICATION_ID);

        Bundle extras = getIntent().getExtras();
		mRowId = extras != null ? extras.getLong(TextDbAdapter.KEY_ROWID)
								: null;

		populateFields();
	}

	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}

	private void populateFields() {
		if(mRowId != null) {
			Log.e("ViewText, mRowId", "" + mRowId);
			Cursor c = db.fetchTextFromHistory(mRowId);
			startManagingCursor(c);
			contact.setText("Sent To:\n" + c.getString(c.getColumnIndexOrThrow(TextDbAdapter.KEY_RECIPIENT)));
			Date date = new Date(c.getLong(c.getColumnIndexOrThrow(TextDbAdapter.KEY_TIME)));
			time.setText(DateFormat.format("EEEE, MMMM dd, yyyy \nh:mmaa", date));
			message.setText("Message:\n" + c.getString(c.getColumnIndexOrThrow(TextDbAdapter.KEY_CONTENT)));
			notes.setText("Notes:\n" + c.getString(c.getColumnIndexOrThrow(TextDbAdapter.KEY_DESCRIPTION)));
			
			repInt = c.getInt(c.getColumnIndexOrThrow(TextDbAdapter.KEY_REPETITION));
			switch(repInt){
				case FutureText.DOES_NOT_REPEAT:
					repetition.setText("Repetition: Does not repeat"); break;
				case FutureText.DAILY:
					repetition.setText("Repetition: Daily"); break;
				case FutureText.EVERY_WEEKDAY:
					repetition.setText("Repetition: Every weekday"); break;
				case FutureText.MONTHLY_DATE:
					repetition.setText("Repetition: Monthly on date"); break;
				case FutureText.MONTHLY_DAY_OF_WEEK:
					repetition.setText("Repetition: Monthly on day of week"); break;
				case FutureText.WEEKLY:
					repetition.setText("Repetition: Weekly"); break;
				case FutureText.YEARLY:
					repetition.setText("Repetition: Yearly"); break;
			}
			
			int sentInt = c.getInt(c.getColumnIndexOrThrow(TextDbAdapter.KEY_SENT));
			if(sentInt == 0)
				sent.setVisibility(View.VISIBLE);
		}
	}

	protected void launchTextList() {
		Intent i = new Intent(this, TextList.class);
		startActivity(i);
	}
}