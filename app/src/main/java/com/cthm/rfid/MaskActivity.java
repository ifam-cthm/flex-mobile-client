package com.cthm.rfid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.dotel.rfid.R;

public class MaskActivity extends Activity implements View.OnClickListener
{
	private RadioButton mRadioTag;
	private TextView mLabelTag;
	private EditText mEditAccessTagId;
	
	private RadioButton mRadioMask;
	private TextView mLabelMask;
	private Spinner	 mSpinBank;
	private EditText mEditOffset;
	private EditText mEditBits;
	private EditText mEditPattern;
	
	public static boolean UseMask = false;
	public static int Type;
	public static String Tag;
	public static class SelectMask
	{
		public int Bank;
		public int Offset;
		public int Bits;
		public String Pattern;
		public String TagId;
	}
	public static SelectMask SelMask = new SelectMask();
	
	@Override
	public void onCreate( Bundle bundle )
	{
		super.onCreate( bundle );
		setContentView( R.layout.mask );
		
		//---
		mRadioTag = (RadioButton)findViewById( R.id.radio_access_tag );
		mLabelTag = (TextView)findViewById( R.id.label_tag );
		mEditAccessTagId = (EditText)findViewById( R.id.edt_tag );
		
		mRadioMask = (RadioButton)findViewById( R.id.radio_select_mask );
		mLabelMask = (TextView)findViewById( R.id.label_mask );
		
		mSpinBank = (Spinner)findViewById( R.id.spin_bank );
		mEditOffset = (EditText)findViewById( R.id.edt_offset );
		mEditBits = (EditText)findViewById( R.id.edt_bits);
		mEditPattern = (EditText)findViewById( R.id.edt_pattern );
		
		Intent intent = getIntent();
		final String tag = intent.getStringExtra( "tag" );
		mEditAccessTagId.setText( tag );
		
		//---
		mRadioTag.setOnClickListener( this );
		mRadioMask.setOnClickListener( this );
		((Button)findViewById( R.id.btn_exit)).setOnClickListener( this );
		
		selectOption( Type );
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
		UseMask = true;
	}
	
	public void onClick( View v )
	{
		switch( v.getId() )
		{
			case R.id.radio_access_tag:
			case R.id.label_tag:
			{
				Type = 0;
				mRadioMask.setChecked( false );
				enableTagControls( true );
				enableMaskControls( false );
				saveValue();
				break;
			}
			case R.id.radio_select_mask:
			case R.id.label_mask:
			{
				Type = 1;
				mRadioTag.setChecked( false );
				enableTagControls( false );
				enableMaskControls( true );
				saveValue();
				break;
			}
			case R.id.btn_exit:
			{
				saveValue();
				setResult( RESULT_OK, new Intent( this, RFIDHostActivity.class ) );
				finish();
				break;
			}
		}
	}
	
	public static SelectMask getSelectMask()
	{
		SelectMask selMask = new SelectMask();
		
		if( Type == 0 )
		{
			selMask.Bank = 1;//0;
			selMask.Offset = 16;//SelMask.Offset;
			
			final String pattern = selMask.Pattern = SelMask.TagId;
			if( pattern != null )
			{
				final int LEN = pattern.length();
				selMask.Bits = LEN * 4;
			}
			else
			{
				selMask.Bits = 0;
				selMask.Pattern = null;
			}
		}
		else
		{
			if( SelMask.Bank == 4/*0*/ )
			{
				selMask.Bits = 0;
			}
			else
			{
				selMask.Bank = SelMask.Bank;
				selMask.Offset = SelMask.Offset;
				
				final String pattern = selMask.Pattern = SelMask.Pattern;
				if( pattern != null )
				{
					final int LEN = pattern.length();
					selMask.Bits = LEN * 4;
				}
				else
				{
					selMask.Bits = 0;
					selMask.Pattern = null;
				}
			}
		}
		return selMask;
	}
	
	public static void clearSelectMask()
	{
		UseMask = false;
		Type = 0;
		SelMask.Bank = 4;
		SelMask.Bits = 0;
		SelMask.Offset = 0x10;
	}
	
	private void selectOption( int index )
	{
		Type = index;
    	mRadioTag.setChecked( index == 0 ? true : false );
    	mRadioMask.setChecked( index == 0 ? false : true );
    	enableTagControls( index == 0 ? true : false );
    	enableMaskControls( index == 0 ? false : true );
    	saveValue();
	}
	
	private void saveValue()
	{
		Tag = mEditAccessTagId.getText().toString();
		SelMask.Bank = 4 - mSpinBank.getSelectedItemPosition();
		SelMask.Offset = Integer.parseInt( mEditOffset.getText().toString() );
		
		SelMask.TagId = mEditAccessTagId.getText().toString();
		SelMask.Pattern = mEditPattern.getText().toString();
		
		String pattern = null;
		if( Type == 0 )
			pattern = SelMask.TagId;
		else
			pattern = SelMask.Pattern;
		
		if( pattern != null )
		{
			final int LEN = pattern.length();
			SelMask.Bits = LEN * 4;
		}
		else
			SelMask.Bits = 0;
		//SelMask.Bits = Integer.parseInt( mEditBits.getText().toString() );
	}
	
	private void enableTagControls( boolean enabled )
	{
		mEditAccessTagId.setEnabled(enabled);
	}
	
	private void enableMaskControls( boolean enabled )
	{
		mSpinBank.setEnabled(enabled);
		mEditOffset.setEnabled(enabled);
		mEditBits.setEnabled(enabled);
		mEditPattern.setEnabled(enabled);
	}
}
