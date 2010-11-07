package com.organforce.futuretext;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


public class About extends Activity 
{
	private ImageView paypal;
	private ImageView market;
	private TextView website;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		
		paypal = (ImageView) findViewById(R.id.donate_paypal);
		market = (ImageView) findViewById(R.id.donate_market);
		website = (TextView) findViewById(R.id.website);
		
		paypal.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent myIntent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(getString(R.string.donate_website)));
				startActivity(myIntent);
			}
		});
		
		market.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent myIntent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(getString(R.string.donate_market)));
				startActivity(myIntent);
			}
		});
		
		website.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent myIntent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(getString(R.string.website)));
				startActivity(myIntent);
			}
		});
		
	}
	
}