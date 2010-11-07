package com.organforce.futuretext;

import java.util.ArrayList;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

public class ScheduledTextService extends Service {
	private TextDbAdapter db;

	public static NotificationManager mNotificationManager;
	public static int NOTIFICATION_ID = 1;

	private FutureText text;
	private long histID;
	private Context thisContext = this;

	@Override
	public void onStart(Intent intent, int startId) {
		if(intent != null){
			super.onStart(intent, startId);
			long id = Long.parseLong(intent.getData().getLastPathSegment());


			text = db.fetchTextObject(id);

			//---when the SMS has been sent---
			registerReceiver(br, new IntentFilter("SMS_SENT"));

			sendSMS(text.recipient, text.content);
			updateNextTime(text);
			scheduleFutureText(this, text);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		db = new TextDbAdapter(this);
		db.open();
	}

	@Override
	public void onDestroy() {
		db.close();
		super.onDestroy();
	}

	private void updateNextTime(FutureText text) {
		text.time = text.nextTime();
		if(text.time == null) {
			db.deleteText(text._id);
		}
		else {
			db.updateText(text._id, text);
		}
	}

	public static void scheduleFutureText(Context context, FutureText text) {
		if(text.time != null) {
			scheduleFutureText(context, text._id, text.time);
		}
	}

	public static void scheduleFutureText(Context context, long id, long time) {
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pendingIntent = getPendingIntentForScheduledText(context, id);
		am.cancel(pendingIntent);
		am.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
	}

	public static void unscheduleFutureText(Context context, long id) {
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pendingIntent = getPendingIntentForScheduledText(context, id);
		am.cancel(pendingIntent);
	}

	public static PendingIntent getPendingIntentForScheduledText(Context context, long id) {
		Intent i = new Intent(context, ScheduledTextService.class);
		Uri uri = Uri.withAppendedPath(Uri.parse("ftext://text/id/"), String.valueOf(id));
		i.setData(uri);
		return PendingIntent.getService(context, 0,
				i, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	//---sends an SMS message to another device---
	private void sendSMS(String phoneNumber, String message)
	{        
		String SENT = "SMS_SENT";

		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
				new Intent(SENT), 0);

		SmsManager sms = SmsManager.getDefault();
		ArrayList<String> multipartMessage = sms.divideMessage(message);
		ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
		sentIntents.add(sentPI);
		for(int i = 0; i < multipartMessage.size() - 1; i++){
			//sentIntents.add(null);
		}
		
		sms.sendMultipartTextMessage(phoneNumber, null, multipartMessage, sentIntents, null);
		//sms.sendTextMessage(phoneNumber, null, message, sentPI, null);   

	}

	private BroadcastReceiver br = new BroadcastReceiver(){
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			CharSequence contentTitle = "Future Text: " + text.recipient;
			CharSequence contentText = text.content;
			CharSequence tickerText = "";

			switch (getResultCode())
			{
			case Activity.RESULT_OK:
				tickerText = "Future Text sent!";
				histID = db.addTextToHistory(text, true);
				break;
			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
				tickerText = "ERROR: Generic failure";
				contentTitle = "Future Text failed to send.";
				contentText = text.recipient + ": " + contentText;
				histID = db.addTextToHistory(text, false);
				break;
			case SmsManager.RESULT_ERROR_NO_SERVICE:
				tickerText = "ERROR: No service";
				contentTitle = "Future Text failed to send.";
				contentText = text.recipient + ": " + contentText;
				histID = db.addTextToHistory(text, false);
				break;
			case SmsManager.RESULT_ERROR_NULL_PDU:
				tickerText = "ERROR: Null PDU";
				contentTitle = "Future Text failed to send.";
				contentText = text.recipient + ": " + contentText;
				histID = db.addTextToHistory(text, false);
				break;
			case SmsManager.RESULT_ERROR_RADIO_OFF:
				tickerText = "ERROR: Radio off";
				contentTitle = "Future Text failed to send.";
				contentText = text.recipient + ": " + contentText;
				histID = db.addTextToHistory(text, false);
				break;
			}
			Log.e("resultCode: ", ""+getResultCode());

			//notification
			// Get the notification manager service.
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancel(NOTIFICATION_ID);
			NOTIFICATION_ID++;

			Context context = getApplicationContext();
			Intent notificationIntent = new Intent(thisContext, ViewText.class);

			notificationIntent.putExtra(TextDbAdapter.KEY_ROWID, histID);
			Log.e("ScheduledTextService: histID", ""+histID);
			Log.e("NotificationIntent: ", ""+notificationIntent.getExtras().getLong(TextDbAdapter.KEY_ROWID));
			PendingIntent contentIntent = PendingIntent.getActivity(thisContext, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

			Notification notification = new Notification(R.drawable.icon, tickerText, System.currentTimeMillis());
			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
			mNotificationManager.notify(NOTIFICATION_ID, notification);
		}
	};

}
