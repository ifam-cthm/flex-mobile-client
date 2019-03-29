package com.dotel.rfid;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class LampView extends View
{
	public static final int LAMP_GRAY = 0;
	public static final int LAMP_RED = 1;
	public static final int LAMP_GREEN = 2;
	
	private int mLampCur = LAMP_GRAY;
	private int mLampBlink = LAMP_RED;
	private static final int RES_LAMP[] = new int[]{
		R.drawable.lamp_gray,
		R.drawable.lamp_red,
		R.drawable.lamp_green, 
	};
	
	private boolean mBlinkContinue;
	private boolean mBlinkSoundOn;
	private boolean mLampEffectOn;
	
	public LampView( Context context )
	{
		super(context);
		init();
	}

	public LampView( Context context, AttributeSet attrs )
	{
		super(context, attrs);
		init();
	}

	public LampView( Context context, AttributeSet attrs, int defStyle )
	{
		super(context, attrs, defStyle);
		init();
	}
	
	private void init()
	{
		setBackgroundResource( RES_LAMP[ mLampCur ] );
	}
	
	@Override
	public void onMeasure( int widthMeasureSpec, int heightMeasureSpec )
	{
		int wMode, hMode;
		int wSpec, hSpec;
		int Width, Height;
		Width = 15;

		wMode = MeasureSpec.getMode(widthMeasureSpec);
		wSpec = MeasureSpec.getSize(widthMeasureSpec);
		hMode = MeasureSpec.getMode(heightMeasureSpec);
		hSpec = MeasureSpec.getSize(heightMeasureSpec);

		switch( wMode )
		{
			case MeasureSpec.AT_MOST:
				Width = Math.min(wSpec, Width);
				break;
			case MeasureSpec.EXACTLY:
				Width = wSpec;
				break;
			case MeasureSpec.UNSPECIFIED:
				break;
		}
		Height = (int) ( Width );

		switch( hMode )
		{
			case MeasureSpec.AT_MOST:
				Height = Math.min(hSpec, Height);
				break;
			case MeasureSpec.EXACTLY:
				Height = hSpec;
				break;
			case MeasureSpec.UNSPECIFIED:
				break;
		}

		setMeasuredDimension(Width, Height);
	}
	
	//--- Blink
	private static final int MSG_CHANGE_LAMP = 100;
	private static final int MSG_BLINK_LAMP_OFF = 101;
	private static final int MSG_BLINK_LAMP_ON = 102;
	public static final int BLINK_INTERVAL = 300;
	private int mLampBlinkInterval = BLINK_INTERVAL;
	private Handler mHandlerBlink = new Handler()
	{
		@Override
		public void handleMessage( Message msg )
		{
			if( RFIDHostActivity.mExit )
				return;
			switch( msg.what )
			{
				case MSG_CHANGE_LAMP:
					setBackgroundResource( RES_LAMP[ mLampCur ] );
					break;
				case MSG_BLINK_LAMP_OFF:
					setBackgroundResource( RES_LAMP[ mLampCur ] );
					if( mBlinkContinue )
						mHandlerBlink.sendEmptyMessageDelayed( mLampEffectOn ? MSG_BLINK_LAMP_ON : MSG_BLINK_LAMP_OFF, mLampBlinkInterval );
					break;
				case MSG_BLINK_LAMP_ON:
				//	if( mBlinkSoundOn == true )
				//		RFIDHostActivity.mSoundManager.playSound( 0 );
					setBackgroundResource( RES_LAMP[ mLampBlink ] );
					mHandlerBlink.sendEmptyMessageDelayed( MSG_BLINK_LAMP_OFF, mLampBlinkInterval );
					break;
			}
		}
	};
	
	public void setLamp( int status )
	{
		if( RFIDHostActivity.mExit)
			return;
		
		mBlinkContinue = false;
		mLampCur = status;
		mHandlerBlink.sendEmptyMessage( MSG_CHANGE_LAMP );
	}
	
	public int getLamp()
	{
		return mLampCur;
	}
	
	public void startBlink( int blinkOff, int blinkOn, int interval, boolean continuous )
	{
		startBlink( blinkOff, blinkOn, interval, continuous, true, false );
	}
	
	public void startBlink( int blinkOff, int blinkOn, int interval, boolean continuous, boolean lampEffect, boolean soundEffect )
	{
		if( RFIDHostActivity.mExit)
			return;
		
		mBlinkContinue = continuous;
		mBlinkSoundOn = soundEffect;
		mLampEffectOn = lampEffect;
		mLampCur = blinkOff;
		mLampBlink = blinkOn;
		mLampBlinkInterval = interval;
		if( mHandlerBlink.hasMessages( MSG_BLINK_LAMP_ON ) == false )
			mHandlerBlink.sendEmptyMessage( MSG_BLINK_LAMP_ON );
		else
			Log.d( "kueen108", "Already has Blink Message..." );
	}
	
	public void stopBlink()
	{
		mBlinkContinue = false;
	}
}
