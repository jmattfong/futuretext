/*
 * Parts of this code are used from google's android samples and the
 * source for the stock music app.
 * 
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.organforce.futuretext;

import java.util.Calendar;
import java.util.Date;

import com.organforce.futuretext.google.ContactAccessor;
import com.organforce.futuretext.google.ContactInfo;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;


public class NewText extends Activity 
{
	public static final int TEXT_LENGTH = 160;
	public static final int INIT_EXTRA_TIME = 10;

	private Button btnSave;
	private Button btnCancel;
	private Button mPickDate;
	private Button mPickTime;
	private Button mOpenContacts;

	private DatePickerDialog datePickerDialog;
	private TimePickerDialog timePickerDialog;

	private EditText txtPhoneNo;
	private EditText txtMessage;
	private EditText txtNotes;
	private Spinner spinnerRep;
	private Calendar cal;

	private TextDbAdapter db;
	private Long mRowId;

	private static final int PICK_CONTACT_REQUEST = 1;
	private final ContactAccessor mContactAccessor = ContactAccessor.getInstance();

	private int mRepetition;

	static final int DATE_DIALOG_ID = 0;
	static final int TIME_DIALOG_ID = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		db = new TextDbAdapter(this);
		db.open();
		setContentView(R.layout.new_text);

		Bundle extras = getIntent().getExtras();
		mRowId = extras != null ? extras.getLong(TextDbAdapter.KEY_ROWID)
				: null;


		// get the current date
		cal = Calendar.getInstance();
		cal.set(Calendar.SECOND, 0);
		cal.add(Calendar.MINUTE, INIT_EXTRA_TIME);

		mPickTime = (Button) findViewById(R.id.pickTime);
		mPickDate = (Button) findViewById(R.id.pickDate);
		mOpenContacts = (Button) findViewById(R.id.openContacts);
		btnSave = (Button) findViewById(R.id.btnSave);
		btnCancel = (Button) findViewById(R.id.btnCancel);
		txtPhoneNo = (EditText) findViewById(R.id.txtPhoneNo);
		txtMessage = (EditText) findViewById(R.id.txtMessage);
		txtNotes = (EditText) findViewById(R.id.notes);
		spinnerRep = (Spinner) findViewById(R.id.spinner);


		btnSave.setOnClickListener(btnSaveOnClickListener);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		mOpenContacts.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				pickContact();
			}
		});
		mPickTime.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(timePickerDialog != null) {
					timePickerDialog.updateTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
				}
				showDialog(TIME_DIALOG_ID);
			}
		});
		mPickDate.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(datePickerDialog != null) {
					datePickerDialog.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
				}
				showDialog(DATE_DIALOG_ID);
			}
		});



		ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
				this, R.array.repetition_array, android.R.layout.simple_spinner_item);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerRep.setAdapter(spinnerAdapter);
		spinnerRep.setOnItemSelectedListener(spinnerOnItemSelectedListener);

		ContactListAdapter adapter = new ContactListAdapter(this);
		AutoCompleteTextView textView = (AutoCompleteTextView)
		findViewById(R.id.txtPhoneNo);
		textView.setAdapter(adapter);

		populateFields();
		// display the current date (this method is below)
		updateDisplay();


	}

	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			return datePickerDialog = new DatePickerDialog(this,
					mDateSetListener,
					cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
		case TIME_DIALOG_ID:
			return timePickerDialog = new TimePickerDialog(this,
					mTimeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false);
		}
		return null;
	}

	private void populateFields() {
		if(mRowId != null) {
			Cursor c = db.fetchText(mRowId);
			if(c != null){
				startManagingCursor(c);
				txtMessage.setText(c.getString(c.getColumnIndexOrThrow(TextDbAdapter.KEY_CONTENT)));
				txtPhoneNo.setText(c.getString(c.getColumnIndexOrThrow(TextDbAdapter.KEY_RECIPIENT)));
				txtNotes.setText(c.getString(c.getColumnIndexOrThrow(TextDbAdapter.KEY_DESCRIPTION)));
				mRepetition = c.getInt(c.getColumnIndexOrThrow(TextDbAdapter.KEY_REPETITION));
				long time = c.getLong(c.getColumnIndexOrThrow(TextDbAdapter.KEY_TIME));

				spinnerRep.setSelection(mRepetition);
				cal.setTimeInMillis(time);
			}
		}
	}

	// updates the date in the TextView
	private void updateDisplay() {
		Date date = new Date(cal.getTimeInMillis());
		mPickDate.setText(
				new StringBuilder()
				.append(DateFormat.format("E, MMM dd, yyyy", date)));
		mPickTime.setText(
				new StringBuilder()
				.append(DateFormat.format("hh:mmaa", date)));
	}

	protected void pickContact() {
		startActivityForResult(mContactAccessor.getPickContactIntent(), PICK_CONTACT_REQUEST);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
			loadContactInfo(data.getData());
		}
	}

	private void loadContactInfo(Uri contactUri) {

		/*
		 * We should always run database queries on a background thread. The database may be
		 * locked by some process for a long time.  If we locked up the UI thread while waiting
		 * for the query to come back, we might get an "Application Not Responding" dialog.
		 */
		AsyncTask<Uri, Void, ContactInfo> task = new AsyncTask<Uri, Void, ContactInfo>() {

			@Override
			protected ContactInfo doInBackground(Uri... uris) {
				return mContactAccessor.loadContact(getContentResolver(), uris[0]);
			}

			@Override
			protected void onPostExecute(ContactInfo result) {
				bindView(result);
			}
		};

		task.execute(contactUri);
	}

	protected void bindView(ContactInfo contactInfo) {/*
        TextView displayNameView = (TextView) findViewById(R.id.display_name_text_view);
        displayNameView.setText(contactInfo.getDisplayName());*/

		TextView phoneNumberView = (TextView) findViewById(R.id.txtPhoneNo);
		phoneNumberView.setText(contactInfo.getPhoneNumber());
	}



	// the callback received when the user "sets" the date in the dialog
	private DatePickerDialog.OnDateSetListener mDateSetListener =
		new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, 
				int monthOfYear, int dayOfMonth) {
			Calendar c = Calendar.getInstance();
			c.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
			c.add(Calendar.DATE, 1);
			if(c.compareTo(Calendar.getInstance()) > 0){
				cal.set(Calendar.MONTH, monthOfYear);
				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			}
			else{
				c = Calendar.getInstance();
				cal.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
				Toast.makeText(getBaseContext(), "Past dates are invalid, reset to present", Toast.LENGTH_SHORT).show();
			}
			updateDisplay();
		}
	};


	// the callback received when the user "sets" the time in the dialog
	private TimePickerDialog.OnTimeSetListener mTimeSetListener =
		new TimePickerDialog.OnTimeSetListener() {

		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

			Calendar c = Calendar.getInstance();
			c.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), hourOfDay, minute, 0);
			c.add(Calendar.MINUTE, 1);
			if(c.compareTo(Calendar.getInstance()) > 0){
				cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
				cal.set(Calendar.MINUTE, minute);
			}
			else{
				c = Calendar.getInstance();
				cal.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY));
				cal.set(Calendar.MINUTE, c.get(Calendar.MINUTE));
				Toast.makeText(getBaseContext(), "Past times are invalid, reset to present", Toast.LENGTH_SHORT).show();
			}
			updateDisplay();
		}
	};

	View.OnClickListener btnSaveOnClickListener = new View.OnClickListener() 
	{
		public void onClick(View v) 
		{                
			String phoneNo = txtPhoneNo.getText().toString();
			String message = txtMessage.getText().toString();
			if(message.length() > TEXT_LENGTH){
				Toast.makeText(getBaseContext(), 
						"Your message is over " + TEXT_LENGTH + " characters and will be sent as " 
						+ (((message.length() - 1)/TEXT_LENGTH) + 1) + " texts", Toast.LENGTH_LONG).show();
			}
			String note = txtNotes.getText().toString();
			if (phoneNo.length()>0 && message.length()>0
					&& PhoneNumberUtils.isWellFormedSmsAddress(phoneNo)) {
				FutureText text = new FutureText(message, note, phoneNo, cal.getTimeInMillis(), mRepetition);
				if(mRowId == null) {
					db.createText(text);	
				}
				else {
					text._id = mRowId;
					db.updateText(mRowId, text);
				}
				ScheduledTextService.scheduleFutureText(NewText.this, text);
				finish();
			}
			else
				Toast.makeText(getBaseContext(), 
						"Please enter a valid phone number and message.", 
						Toast.LENGTH_SHORT).show();
		}

	};

	OnItemSelectedListener spinnerOnItemSelectedListener = new OnItemSelectedListener() {
		public void onItemSelected(AdapterView<?> parent,
				View view, int pos, long id) {
			mRepetition = pos;
		}

		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
		}
	};

	// XXX compiler bug in javac 1.5.0_07-164, we need to implement Filterable
	// to make compilation work
	public static class ContactListAdapter extends CursorAdapter implements Filterable {
		private static final String[] PROJECTION_PHONE = {
			Phone._ID,                  // 0
			Phone.CONTACT_ID,           // 1
			Phone.TYPE,                 // 2
			Phone.NUMBER,               // 3
			Phone.LABEL,                // 4
			Phone.DISPLAY_NAME,         // 5
		};

		private static final String SORT_ORDER = Contacts.TIMES_CONTACTED + " DESC,"
		+ Contacts.DISPLAY_NAME + "," + Phone.TYPE;

		private ContentResolver mContent;

		public ContactListAdapter(Context context) {
			super(context, null);
			mContent = context.getContentResolver();
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final LayoutInflater inflater = LayoutInflater.from(context);
			final View view = inflater.inflate(
					R.layout.list_item, parent, false);
			((TextView) view.findViewById(R.id.text1)).setText(
					cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)));
			((TextView) view.findViewById(R.id.text3)).setText(
					cursor.getString(cursor.getColumnIndexOrThrow(Phone.NUMBER)));
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			((TextView) view.findViewById(R.id.text1)).setText(
					cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)));
			((TextView) view.findViewById(R.id.text3)).setText(
					cursor.getString(cursor.getColumnIndexOrThrow(Phone.NUMBER)));
		}

		@Override
		public String convertToString(Cursor cId) {
			/*
        	String number = null;
            Cursor cNumber = mContent.query(Phone.CONTENT_URI,
                new String[]{Phone.NUMBER},
                Phone.CONTACT_ID + "=" + cId.getLong(cId.getColumnIndexOrThrow(ContactsContract.Contacts._ID)), 
                null, Phone.IS_SUPER_PRIMARY + " DESC");
            try {
                if (cNumber.moveToFirst()) {
                    number = cNumber.getString(cNumber.getColumnIndexOrThrow(Phone.NUMBER));
                }
            } finally {
                cNumber.close();
            }
            return number;
			 */
			return cId.getString(cId.getColumnIndexOrThrow(Phone.NUMBER));
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
			if (getFilterQueryProvider() != null) {
				return getFilterQueryProvider().runQuery(constraint);
			}
			String cons = null;
			if(constraint != null) {
				cons = constraint.toString();
			}
			//StringBuilder buffer = null;
			//String[] args = null;
			/*
            if (constraint != null) {
                buffer = new StringBuilder();
                buffer.append("UPPER(");
                buffer.append(ContactsContract.Contacts.DISPLAY_NAME);
                buffer.append(") GLOB ?");
                args = new String[] { constraint.toString().toUpperCase() + "*" };
            }*/


			Uri uri = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri.encode(cons));
			Cursor phoneCursor =
				mContent.query(uri,
						PROJECTION_PHONE,
						null, //selection,
						null,
						SORT_ORDER);

			return phoneCursor;
		}
	}
}