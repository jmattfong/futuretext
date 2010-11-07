package com.organforce.futuretext;

import java.util.Date;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class TextList extends ListActivity {

	private Button btnNewMessage;
	private TextDbAdapter db;
	
	public static final int DISPLAY_LENGTH = 36;
	
	public static final int EDIT_ID = Menu.FIRST;
	public static final int DELETE_ID = Menu.FIRST + 1;
	
	public static final int NEW_ID = Menu.FIRST;
	public static final int HISTORY_ID = Menu.FIRST + 1;
	public static final int ABOUT_ID = Menu.FIRST + 2;
	
	private static final int REQUEST_NEW_EDIT = 1;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.text_list);
		db = new TextDbAdapter(this);
		db.open();   
		fillList();
        registerForContextMenu(getListView());

		btnNewMessage = (Button) findViewById(R.id.btnNewMessage);

		// add a click listener to the button
		btnNewMessage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchNewText(null);
			}
		});


		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				launchNewText(id);
			}
		});


	}
	
	@Override
	public void onDestroy(){
		db.close();
		super.onDestroy();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, EDIT_ID, 0, R.string.menu_edit);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
        case EDIT_ID:
        	launchNewText(info.id);
        	return true;
        case DELETE_ID:
            db.deleteText(info.id);
            ScheduledTextService.unscheduleFutureText(this, info.id);
            fillList();
            return true;
        
	    }
	    return super.onContextItemSelected(item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, NEW_ID, 0, R.string.btn_new_message);
		menu.add(0, HISTORY_ID, 0, R.string.menu_history);
		menu.add(0, ABOUT_ID, 0, R.string.menu_about);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case NEW_ID:
        	launchNewText(null);
        	return true;
        case HISTORY_ID:
        	launchHistoryList();
        	return true;
        case ABOUT_ID:
        	launchAbout();
        	return true;
	    }
	    return super.onOptionsItemSelected(item);
	}

	private void fillList() {
		Cursor c = db.fetchAllTexts();
		startManagingCursor(c);
		String[] from = {TextDbAdapter.KEY_RECIPIENT, TextDbAdapter.KEY_TIME, TextDbAdapter.KEY_CONTENT};
		int[] to = {R.id.text1, R.id.text2, R.id.text3};
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,R.layout.list_item,c,from,to);
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				String columnName = cursor.getColumnName(columnIndex);
				if(columnName.equals(TextDbAdapter.KEY_TIME)) {
					TextView timeView = (TextView) view;
					long time = cursor.getLong(columnIndex);
					timeView.setText(DateFormat.format("E, MM/dd/yy h:mmaa", new Date(time)));
					return true;
				}
				if(columnName.equals(TextDbAdapter.KEY_CONTENT)) {
					TextView contentView = (TextView) view;
					String content = cursor.getString(columnIndex);
					content = content.length() < DISPLAY_LENGTH ? content : content.substring(0, DISPLAY_LENGTH) + ". . .";
					contentView.setText(content);
					return true;
				}
				
				return false;
			}
		});
		setListAdapter(adapter);
	}
	

	protected void launchNewText(Long id) {
		Intent i = new Intent(this, NewText.class);
		if(id != null) {
			i.putExtra(TextDbAdapter.KEY_ROWID, id);
		}
		startActivityForResult(i, REQUEST_NEW_EDIT);
	}
	
	protected void launchHistoryList() {
		Intent i = new Intent(this, HistoryList.class);
		startActivity(i);
	}
	
	protected void launchAbout() {
		Intent i = new Intent(this, About.class);
		startActivity(i);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		fillList();
	}
}