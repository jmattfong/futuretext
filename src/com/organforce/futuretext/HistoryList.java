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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class HistoryList extends ListActivity {

	private TextDbAdapter db;

	public static final int DISPLAY_LENGTH = 36;
	public static final int VIEW_ID = Menu.FIRST;
	public static final int DELETE_ID = Menu.FIRST + 1;

	public static final int NEW_ID = Menu.FIRST;
	public static final int CLEAR_ID = Menu.FIRST + 1;
	public static final int ABOUT_ID = Menu.FIRST + 2;

	private static final int REQUEST_NEW_EDIT = 1;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.history_list);

		db = new TextDbAdapter(this);
		db.open();
		
		fillList();
		registerForContextMenu(getListView());

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				launchViewText(id);
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, VIEW_ID, 0, R.string.menu_view);
		menu.add(0, DELETE_ID, 0, R.string.menu_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId()) {
		case VIEW_ID:
			launchViewText(info.id);
			return true;
		case DELETE_ID:
			db.deleteTextFromHistory(info.id);
			fillList();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, NEW_ID, 0, R.string.btn_new_message);
		menu.add(0, CLEAR_ID, 0, R.string.menu_clear);
		menu.add(0, ABOUT_ID, 0, R.string.menu_about);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case NEW_ID:
			launchNewText();
			return true;
		case CLEAR_ID:
			db.clearHistory();
			fillList();
			return true;
		case ABOUT_ID:
			launchAbout();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void fillList() {
		Cursor c = db.fetchAllHistory();
		startManagingCursor(c);
		String[] from = {TextDbAdapter.KEY_RECIPIENT, TextDbAdapter.KEY_TIME, TextDbAdapter.KEY_CONTENT, TextDbAdapter.KEY_SENT};
		int[] to = {R.id.text1, R.id.text2, R.id.text3, R.id.img_not_sent};
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,R.layout.hist_list_item,c,from,to);
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				String columnName = cursor.getColumnName(columnIndex);
				int sent = cursor.getInt(cursor.getColumnIndexOrThrow(TextDbAdapter.KEY_SENT));
				if(columnName.equals(TextDbAdapter.KEY_TIME)) {
					if(sent != 0){
						TextView timeView = (TextView) view;
						long time = cursor.getLong(columnIndex);
						timeView.setText(DateFormat.format("E, MM/dd/yy h:mmaa", new Date(time)));
					}
					else{
						TextView timeView = (TextView) view;
						timeView.setText(R.string.not_sent);
					}
					return true;
				}
				if(columnName.equals(TextDbAdapter.KEY_SENT)){
					ImageView xImg = (ImageView) view;
					if(sent != 0){
						xImg.setVisibility(ImageView.INVISIBLE);
					}
					else{
						xImg.setVisibility(ImageView.VISIBLE);
					}
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

	protected void launchNewText() {
		Intent i = new Intent(this, NewText.class);
		startActivityForResult(i, REQUEST_NEW_EDIT);
	}

	protected void launchViewText(Long id) {
		Intent i = new Intent(this, ViewText.class);
		if(id != null) {
			i.putExtra(TextDbAdapter.KEY_ROWID, id);
		}
		startActivityForResult(i, REQUEST_NEW_EDIT);
	}
	
	protected void launchAbout() {
		Intent i = new Intent(this, About.class);
		startActivityForResult(i, REQUEST_NEW_EDIT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		fillList();

	}
}