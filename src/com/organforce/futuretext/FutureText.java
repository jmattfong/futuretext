package com.organforce.futuretext;

import java.util.Calendar;

import android.content.ContentValues;

public class FutureText {
	public static final int DOES_NOT_REPEAT = 0;
	public static final int DAILY = 1;
	public static final int EVERY_WEEKDAY = 2;
	public static final int WEEKLY = 3;
	public static final int MONTHLY_DAY_OF_WEEK = 4;
	public static final int MONTHLY_DATE = 5;
	public static final int YEARLY = 6;
	
	public String content, description, recipient;
	public Long time,_id;
	public int repetition;
	
	public FutureText(String content, String description, String recipient,
			long time, int repetition) {
		this.content = content;
		this.description = description;
		this.recipient = recipient;
		this.time = time;
		this.repetition = repetition;
	}
	
	public ContentValues toContentValues() {
		ContentValues cv = new ContentValues();
		cv.put(TextDbAdapter.KEY_CONTENT, content);
		cv.put(TextDbAdapter.KEY_DESCRIPTION, description);
		cv.put(TextDbAdapter.KEY_TIME, time);
		cv.put(TextDbAdapter.KEY_RECIPIENT, recipient);
		cv.put(TextDbAdapter.KEY_REPETITION, repetition);
		return cv;
	}

	public Long nextTime() {
		Calendar systemTime = Calendar.getInstance();
		Calendar thisTime = Calendar.getInstance();
		thisTime.setTimeInMillis(this.time);
		
		setTime(systemTime, thisTime);
		
		switch(this.repetition){
			case FutureText.DOES_NOT_REPEAT: return null;
			case FutureText.DAILY: systemTime.add(Calendar.DATE, 1); break;
			case FutureText.EVERY_WEEKDAY:
				if (systemTime.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
					systemTime.add(Calendar.DAY_OF_WEEK, 3);
				}
				else if (systemTime.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
					systemTime.add(Calendar.DAY_OF_WEEK, 2);
				}
				else {
					systemTime.add(Calendar.DAY_OF_WEEK, 1);
				}
				break;
			case FutureText.WEEKLY: 
				do {
					systemTime.add(Calendar.DATE, 1);
				} while(systemTime.get(Calendar.DAY_OF_WEEK) != thisTime.get(Calendar.DAY_OF_WEEK));
				break;
			case FutureText.MONTHLY_DAY_OF_WEEK:
				do {
					systemTime.add(Calendar.DATE, 1);
				} while(systemTime.get(Calendar.DAY_OF_WEEK) != thisTime.get(Calendar.DAY_OF_WEEK) || 
						systemTime.get(Calendar.WEEK_OF_MONTH) != thisTime.get(Calendar.WEEK_OF_MONTH));
				break;
			case FutureText.MONTHLY_DATE:
				do {
					systemTime.add(Calendar.DATE, 1);
				} while(systemTime.get(Calendar.DAY_OF_MONTH) != thisTime.get(Calendar.DAY_OF_MONTH));
				break;
			case FutureText.YEARLY: 
				do {
					systemTime.add(Calendar.DATE, 1);
				} while(systemTime.get(Calendar.MONTH) != thisTime.get(Calendar.MONTH) || 
						systemTime.get(Calendar.DAY_OF_MONTH) != thisTime.get(Calendar.DAY_OF_MONTH));
				break;
		}
		
		this.time = systemTime.getTimeInMillis();
		return this.time;
	}
	
	private void setTime(Calendar toBeSet, Calendar setFrom){
		toBeSet.set(Calendar.HOUR_OF_DAY, setFrom.get(Calendar.HOUR_OF_DAY)); 
		toBeSet.set(Calendar.MINUTE, setFrom.get(Calendar.MINUTE));
		toBeSet.set(0, setFrom.get(0));
	}
}
