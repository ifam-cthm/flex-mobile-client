package com.cthm.rfid;

/***********************************************************************************
* revision history                                                                 *
*************+*************+********+***********************************************
* 2012.12.12	ver 1.0.0  	  eric     1. Generated(First release)                 *                     
************************************************************************************/

import com.dotel.rfid.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


public class RFIDStartActivity extends Activity implements
	View.OnClickListener
{
	private Context mContext;
	
	// --- Interface Mode
	public static final int MODE_NOT_DETECTED = 0;
	public static final int MODE_BT_INTERFACE = 1;
	public static final int MODE_USB_INTERFACE = 2;
	
	
	// --- Mode 
	private Button mBtnBT;
	private Button mBtnUSB;
	private ImageView mDoth900;
	
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);
		
		mContext = this;
		
		setContentView(R.layout.start);
		
		mDoth900 = (ImageView)findViewById(R.id.drawable_r900);		
		mDoth900.setScaleType(ImageView.ScaleType.FIT_XY);
		
		mBtnBT = (Button)findViewById(R.id.btn_BTMode);
		mBtnBT.setOnClickListener(this);
		
		mBtnUSB = (Button) findViewById(R.id.btn_USBMode);
		mBtnUSB.setOnClickListener(this);
		
		// interface mode
		R900Status.setInterfaceMode(MODE_NOT_DETECTED);
		
	}
	
	@Override
	public void onBackPressed()
	{
		new AlertDialog.Builder(this)
				.setTitle("EXIT")
				.setMessage("Do you want to exit program?")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					public void onClick( DialogInterface dialog, int whichButton )
					{
						finish();
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener()
				{
					public void onClick( DialogInterface dialog, int whichButton )
					{
					}
				}).create().show();
	}
	

	public void onClick( View v )
	{
		switch( v.getId() )
		{
			case R.id.btn_BTMode:
				R900Status.setInterfaceMode(MODE_BT_INTERFACE);
				
				Intent intent = new Intent(mContext.getApplicationContext(), RFIDHostActivity.class);
				startActivity(intent);				
				
				break;
				
			case R.id.btn_USBMode:
				R900Status.setInterfaceMode(MODE_USB_INTERFACE);
				
				Intent Usbintent = new Intent(mContext.getApplicationContext(), RFIDUsbHostActivity.class);
				startActivity(Usbintent);			
				
				break;		
		}
	}

}