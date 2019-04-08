package com.cthm.rfid;


/***********************************************************************************
* USB Host revision history                                                        *
*************+*************+********+***********************************************
* 2012.12.12	ver 1.0.0  	  eric     1. Generated(First release)                 *
************************************************************************************/



import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.cthm.libr900.OnUsbEventListener;
import com.cthm.libr900.R900Protocol;
import com.cthm.libr900.R900RecvPacketParser;
import com.dotel.rfid.R;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;


//<-- eric 2012.12.7
// import Usb Interface 
import com.cthm.libr900.R900UsbManager;
//--> eric 2012.12.7

public class RFIDUsbHostActivity extends TabActivity implements
		View.OnClickListener, AdapterView.OnItemSelectedListener, TabHost.OnTabChangeListener,
		SeekBar.OnSeekBarChangeListener, OnUsbEventListener
{
	private TabHost mTab;
	
	public static SoundManager mSoundManager = new SoundManager();
	private String mStrAccessErrMsg;
	private boolean mAllive;
	private boolean mForceDisconnect;
			
	Context context = this;	// eric 2012.11.30
	
	private boolean mBattflag;	// eric 2012.11.23
	
	// eric 2012.12.7
	private R900UsbManager r900UsbManager;
	
	// ReceiveData Thread object
	Handler handler = new Handler();  // eric 2012.12.7
		
	private static final String TAG = "RFIDUsbHostActivity";
	
	// --- Link
	private Button mBtnScan;
	private Button mBtnDisconnect;

	protected ListView mListDevice;
	private ArrayList<HashMap<String, String>> mArrBtDevice;
	private BaseAdapter mAdapterDevice;

	// --- Inventory
	private LampView mInventLampOn;
	private LampView mInventLampTx;
	private LampView mInventLampRx;
	private Button mBtnInventory;
	private TextView mTxtTagCount;

	protected ListView mListTag;
	private ArrayList<HashMap<String, String>> mArrTag;
	private BaseAdapter mAdapterTag;

	// --- Access
	private LampView mAccessLampOn;
	private LampView mAccessLampTx;
	private LampView mAccessLampRx;
	private LampView mLampDetectReadWrite;
	private LampView mLampDetectLock;
	private LampView mLampDetectKill;
	private LampView mLampStatusReadWrite;
	private LampView mLampStatusLock;
	private LampView mLampStatusKill;

	private Button mBtnAccess;
	private EditText mEdtPassword;

	private RadioButton mRdoRead;
	private RadioButton mRdoWrite;
	private RadioButton mRdoLock;
	private RadioButton mRdoKill;

	private RadioButton mRdoTag;
	private EditText mEdtTagId;

	private LinearLayout mLayoutReadWrite;
	private LinearLayout mLayoutLock;
	private LinearLayout mLayoutKill;

	// ------- Access::read/write
	private Spinner mSpinEpc;
	private EditText mEdtTagMemOffset;
	private EditText mEdtTagMemWordcount;
	private EditText mEdtTagMemData;

	// ------- Access::lock
	private Spinner mSpinKillPassword;
	private Spinner mSpinAccessPassword;
	private Spinner mSpinUiiMem;
	private Spinner mSpinTidMem;
	private Spinner mSpinUserMem;
	private LinearLayout mLayoutLockPage1;
	private LinearLayout mLayoutLockPage2;
	private Button mBtnMore;

	// ------- Access::kill
	private EditText mEdtKillPassword;
	private EditText mEdtUserPassword;

	private Button mBtnMask;

	// --- Config
	//private CheckBox mChkAutoLink;
	private CheckBox mChkDetectSound;
	private CheckBox mChkSkipSame;
	private CheckBox mChkSingleTag;
	private CheckBox mChkContinuous;

	private Spinner mSpinQuerySession;
	private Spinner mSpinTargetAB;
	private Spinner mSpinQueryQ;
	private EditText mEdtTimeout;
	private TextView mTxtPower;
	private SeekBar mSeekPower;
	private ProgressBar mProgress;	// eric 2012.11.23
	private TextView mBattPower;	// eric 2012.11.23
	
	protected boolean mConnected;
	
	private static final String[] TXT_POWER =
	{ "Max", "-1dB", "-2dB", "-3dB", "-4dB", "-5dB", "-6dB", "-7dB", "-8dB",
			"-9dB" };
	private static final int[] TX_POWER =
	{ 0, -1, -2, -3, -4, -5, -6, -7, -8, -9 };

	private InputFilter mHexFilter = new InputFilter()
	{
		public CharSequence filter( CharSequence source, int start, int end,
				Spanned dest, int dstart, int dend )
		{
			for( int i = start; i < end; i++ )
			{
				final char CHAR = source.charAt(i);
				if( !Character.isDigit(CHAR)
						&& !( CHAR == 'a' || CHAR == 'A' || CHAR == 'b'
								|| CHAR == 'B' || CHAR == 'c' || CHAR == 'C'
								|| CHAR == 'd' || CHAR == 'D' || CHAR == 'e'
								|| CHAR == 'E' || CHAR == 'f' || CHAR == 'F' ) )
				{
					return "";
				}
			}
			return null;
		}
	};
	
	private BroadcastReceiver mPowerOffReceiver = new BroadcastReceiver()
	{
		public void onReceive( Context context, Intent intent )
		{
			
			if( mConnected )
			{
				r900UsbManager.sendCmdStop();				
			}
			
		}
	};

	// ---
	public static final int MSG_QUIT = 9999;
	public static final int MSG_AUTO_REFRESH_DEVICE = 1;
	public static final int MSG_ENABLE_LINK_CTRL = 10;
	public static final int MSG_DISABLE_LINK_CTRL = 11;
	public static final int MSG_ENABLE_DISCONNECT = 12;
	public static final int MSG_DISABLE_DISCONNECT = 13;
	public static final int MSG_SHOW_TOAST = 20;
	public static final int MSG_REFRESH_LIST_DEVICE = 21;
	public static final int MSG_REFRESH_LIST_TAG = 22;

	public static final int MSG_LINK_ON = 30;
	public static final int MSG_LINK_OFF = 31;

	public static final int MSG_SOUND_RX = 40;
	public static final int MSG_SOUND_RX_HALF = 41;

	public static final int MSG_AUTO_LINK = 100;

	// ---
	private int mTabMode = TAB_LINK;
	public static final int TAB_LINK = 0;
	public static final int TAB_INVENTORY = 1;
	public static final int TAB_ACCESS = 2;
	public static final int TAB_CONFIG = 3;

	private int mAccessType = ACC_TYPE_READ;
	public static final int ACC_TYPE_READ = 0;
	public static final int ACC_TYPE_WRITE = 1;
	public static final int ACC_TYPE_LOCK = 2;
	public static final int ACC_TYPE_KILL = 3;
	
	// --- Battery Message
	public static final int MSG_BATTERY_CTRL = 50;

	private String mSelTag;
	public static boolean mExit = false;

	public static final int INTENT_MASK = 1;
	
	// --- Interface Mode
	public static final int MODE_NOT_DETECTED = 0;
	public static final int MODE_BT_INTERFACE = 1;
	public static final int MODE_USB_INTERFACE = 2;

	
	private Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage( final Message msg )
		{
			switch( msg.what )
			{
				case MSG_QUIT:
					closeApp();
					break;
				case MSG_ENABLE_LINK_CTRL:					
					break;
				case MSG_DISABLE_LINK_CTRL:
					break;
				case MSG_ENABLE_DISCONNECT:
				{
					mBtnInventory.setText("INVENTORY");
					mBtnAccess.setText("Access");					
					break;
				}
				case MSG_DISABLE_DISCONNECT:
					mBtnDisconnect.setEnabled(false);
					break;
				case MSG_SHOW_TOAST:
					Toast.makeText(RFIDUsbHostActivity.this, (String) msg.obj,
							msg.arg1).show();
					break;
				case MSG_REFRESH_LIST_DEVICE:
					mAdapterDevice.notifyDataSetChanged();
					break;
				case MSG_REFRESH_LIST_TAG:
				{
					mTxtTagCount.setText(String.valueOf(mArrTag.size()));
					mAdapterTag.notifyDataSetChanged();
					break;
				}
				case MSG_LINK_ON:
				{
					mBtnInventory.setEnabled(true);
					mBtnAccess.setEnabled(true);
					break;
				}
				case MSG_LINK_OFF:
				{
					mBtnInventory.setEnabled(false);
					mBtnAccess.setEnabled(false);
					break;
				}
				case MSG_SOUND_RX:
				{
					if( RFIDUsbHostActivity.this.isDetectSoundOn() )
						mSoundManager.playSound(0);
					break;
				}
				case MSG_SOUND_RX_HALF:
				{
					if( RFIDUsbHostActivity.this.isDetectSoundOn() )
						mSoundManager.playSound(0, 0.5f);
					break;
				}
			}
		}
	};

	@Override
	public void onDestroy()
	{
		finalize();
		super.onDestroy();
	}

	@Override 
	public void onStart()
	{
		super.onStart();
		mExit = false;
		mAllive = true;		
	}

	@Override
	public void onResume()
	{
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction( "android.intent.action.ACTION_SHUTDOWN" );
		registerReceiver( mPowerOffReceiver, filter );
	}

	@Override
	public void onPause()
	{
		super.onPause();
		unregisterReceiver( mPowerOffReceiver );
		
		if( mConnected )
			r900UsbManager.sendCmdStop();
		
		R900Status.setOperationMode(0);	// eric 2012.11.29
	}

	@Override
	public void onStop()
	{
		super.onStop();
		mAllive = false;				
		
		if(mConnected)
			r900UsbManager.sendCmdStop();
		
	}

	@Override
	public void onRestart()
	{
		super.onRestart();
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);
		
		// -- ui
		mTab = getTabHost();
		LayoutInflater inflater = LayoutInflater.from(this);
		inflater.inflate(R.layout.rfid_usb_host, mTab.getTabContentView(), true);
		mTab.addTab(mTab.newTabSpec("link").setIndicator("Link")
				.setContent(R.id.opt_link));
		mTab.addTab(mTab.newTabSpec("inventory").setIndicator("Inventory")
				.setContent(R.id.opt_inventory));
		mTab.addTab(mTab.newTabSpec("access").setIndicator("Access")
				.setContent(R.id.opt_access));
		mTab.addTab(mTab.newTabSpec("config").setIndicator("Config")
				.setContent(R.id.opt_config));
		mTab.setOnTabChangedListener(this);

		// -- event					
		// -- Link	
		//initBluetoothDeviceList(R.id.list_btdevice);
		initTagList(R.id.list_tag);

		mBtnScan = (Button) findViewById(R.id.btn_scan);
		mBtnScan.setOnClickListener(this);

		mBtnDisconnect = (Button) findViewById(R.id.btn_disconnect);
		mBtnDisconnect.setOnClickListener(this);			

		// -- Inventory
		mInventLampOn = (LampView) findViewById(R.id.invent_lamp_on);
		mInventLampTx = (LampView) findViewById(R.id.invent_lamp_tx);
		mInventLampRx = (LampView) findViewById(R.id.invent_lamp_rx);
		mBtnInventory = (Button) findViewById(R.id.btn_inventory);
		mBtnInventory.setOnClickListener(this);

		mTxtTagCount = (TextView) findViewById(R.id.txt_tagcount);
		mTxtTagCount.setOnClickListener(this);

		// -- Access
		mAccessLampOn = (LampView) findViewById(R.id.access_lamp_on);
		mAccessLampTx = (LampView) findViewById(R.id.access_lamp_tx);
		mAccessLampRx = (LampView) findViewById(R.id.access_lamp_rx);

		mLampDetectReadWrite = (LampView) findViewById(R.id.detect_lamp_readwrite);
		mLampDetectLock = (LampView) findViewById(R.id.detect_lamp_lock);
		mLampDetectKill = (LampView) findViewById(R.id.detect_lamp_kill);
		mLampStatusReadWrite = (LampView) findViewById(R.id.status_lamp_readwrite);
		mLampStatusReadWrite.setOnClickListener(this);
		mLampStatusLock = (LampView) findViewById(R.id.status_lamp_lock);
		mLampStatusLock.setOnClickListener(this);
		mLampStatusKill = (LampView) findViewById(R.id.status_lamp_kill);
		mLampStatusKill.setOnClickListener(this);

		mLayoutReadWrite = (LinearLayout) findViewById(R.id.layout_read_write);
		mLayoutReadWrite.setVisibility(View.VISIBLE);
		mLayoutLock = (LinearLayout) findViewById(R.id.layout_lock);
		mLayoutLock.setVisibility(View.INVISIBLE);
		mLayoutKill = (LinearLayout) findViewById(R.id.layout_kill);
		mLayoutKill.setVisibility(View.INVISIBLE);

		mBtnAccess = (Button) findViewById(R.id.btn_access);
		mBtnAccess.setOnClickListener(this);
		mEdtPassword = (EditText) findViewById(R.id.edt_password);

		mRdoRead = (RadioButton) findViewById(R.id.radio_read);
		mRdoRead.setOnClickListener(this);
		mRdoRead.setChecked(true);
		mRdoWrite = (RadioButton) findViewById(R.id.radio_write);
		mRdoWrite.setOnClickListener(this);
		mRdoLock = (RadioButton) findViewById(R.id.radio_lock);
		mRdoLock.setOnClickListener(this);
		mRdoKill = (RadioButton) findViewById(R.id.radio_kill);
		mRdoKill.setOnClickListener(this);

		mRdoTag = (RadioButton) findViewById(R.id.radio_tag);
		mRdoTag.setChecked(true);
		mEdtTagId = (EditText) findViewById(R.id.edt_tag);

		// ------- Access::read/write
		mSpinEpc = (Spinner) findViewById(R.id.spin_epc);
		mSpinEpc.setOnItemSelectedListener(this);
		mEdtTagMemOffset = (EditText) findViewById(R.id.edt_tag_mem_offset);
		mEdtTagMemWordcount = (EditText) findViewById(R.id.edt_tag_mem_wordcount);
		mEdtTagMemData = (EditText) findViewById(R.id.edt_tag_mem_data);

		// ------- Access::lock
		mSpinKillPassword = (Spinner) findViewById(R.id.spin_kill_pwd);
		mSpinAccessPassword = (Spinner) findViewById(R.id.spin_access_pwd);
		mSpinUiiMem = (Spinner) findViewById(R.id.spin_uii_mem);
		mSpinTidMem = (Spinner) findViewById(R.id.spin_tid_mem);
		mSpinUserMem = (Spinner) findViewById(R.id.spin_user_mem);
		mLayoutLockPage1 = (LinearLayout) findViewById(R.id.layout_lock_page1);
		mLayoutLockPage1.setVisibility(View.VISIBLE);
		mLayoutLockPage2 = (LinearLayout) findViewById(R.id.layout_lock_page2);
		mLayoutLockPage2.setVisibility(View.INVISIBLE);
		mBtnMore = (Button) findViewById(R.id.btn_more);
		mBtnMore.setVisibility(View.INVISIBLE);
		mBtnMore.setOnClickListener(this);

		// ------- Access::kill
		mEdtKillPassword = (EditText) findViewById(R.id.edt_kill_password);
		mEdtUserPassword = (EditText) findViewById(R.id.edt_user_password);

		mBtnMask = (Button) findViewById(R.id.btn_mask);
		mBtnMask.setOnClickListener(this);

		// -- Config
		//mChkAutoLink = (CheckBox) findViewById(R.id.chk_auto_link);
		mChkDetectSound = (CheckBox) findViewById(R.id.chk_detect_sound);
		mChkSkipSame = (CheckBox) findViewById(R.id.chk_skip_same);
		mChkSingleTag = (CheckBox) findViewById(R.id.chk_single_tag);
		mChkSingleTag.setOnClickListener(this);
		mChkContinuous = (CheckBox) findViewById(R.id.chk_continuous);

		mSpinQuerySession = (Spinner) findViewById(R.id.spin_query_session);
		mSpinTargetAB = (Spinner) findViewById(R.id.spin_target_ab);
		mSpinQueryQ = (Spinner) findViewById(R.id.spin_query_q);
		mEdtTimeout = (EditText) findViewById(R.id.edt_query_timeout);
		mTxtPower = (TextView) findViewById(R.id.txt_power);
		mSeekPower = (SeekBar) findViewById(R.id.seek_power);
		mSeekPower.setMax(9);
		mSeekPower.setOnSeekBarChangeListener(this);
		
		// ---
		mSoundManager.initSounds(this);
		mSoundManager.addSound(0, R.raw.success);

		setBitsAndOffset("01", "7", false);
		resetMemData(true, false);
		mEdtPassword.setText("00000000");

		//mChkAutoLink.setChecked(true);
		mChkDetectSound.setChecked(true);
		mChkContinuous.setChecked(true);

		mSpinQuerySession.setSelection(0);
		mSpinTargetAB.setSelection(2);
		mSpinQueryQ.setSelection(5);
		mEdtTimeout.setText("0");

		//mSingleTag = mChkSingleTag.isChecked();
		R900Status.setSingleTag(mChkSingleTag.isChecked());
		mForceDisconnect = false;
		
		// setLinkStatus( false );
		setConnectionStatus(false);	
	}

	/*
	 *  get end message
	 */
	public String getEndMsg( String strEnd )
	{
		if( strEnd == null )
			return null;

		if( strEnd.equals("-1") || strEnd.equals("0") )
			return null;// return "Stopped";
		else
			return "Operation Terminated by " + strEnd;
	}

	/*
	 * get Tag error message
	 */
	public String getTagErrorMsg( String strTagError )
	{
		if( strTagError.equals("0") )
			return "Tag Error : General Error";
		else if( strTagError.equals("3") )
			return "Tag Error : Memory Overrun";
		else if( strTagError.equals("4") )
			return "Tag Error : Memory Locked";
		else if( strTagError.equals("11") )
			return "Tag Error : Insufficient Power";
		else if( strTagError.endsWith("15") )
			return "Tag Error : Not Supported";
		else
			return "Tag Error : Unknown Error : " + strTagError;
	}

	public boolean isDetectSoundOn()
	{
		return mChkDetectSound.isChecked();
	}

	public void setAccessLampDetect( int id )
	{
		mLampDetectReadWrite.setLamp(id);
		mLampDetectLock.setLamp(id);
		mLampDetectKill.setLamp(id);
	}

	public void setAccessLampStatus( int id )
	{
		mLampStatusReadWrite.setLamp(id);
		mLampStatusLock.setLamp(id);
		mLampStatusKill.setLamp(id);
	}

	public void resetAccessLamp()
	{
		setAccessLampDetect(LampView.LAMP_GRAY);
		setAccessLampStatus(LampView.LAMP_GRAY);
	}

	public void onTabChanged( String str )
	{
		//r900UsbManager.sendCmdStop();
		//R900Status.setOperationMode(0);	// eric 2012.11.29

		mInventLampTx.setLamp(LampView.LAMP_GRAY);
		mInventLampRx.setLamp(LampView.LAMP_GRAY);
		mAccessLampTx.setLamp(LampView.LAMP_GRAY);
		mAccessLampRx.setLamp(LampView.LAMP_GRAY);

		mBtnAccess.setText("Access");
		mBtnInventory.setText("INVENTORY");

		if( str.equalsIgnoreCase("link") )
			mTabMode = TAB_LINK;
		else if( str.equalsIgnoreCase("inventory") )
			mTabMode = TAB_INVENTORY;
		else if( str.equalsIgnoreCase("access") )
			mTabMode = TAB_ACCESS;
		else if( str.equalsIgnoreCase("config") )
			mTabMode = TAB_CONFIG;
	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode,
			Intent data )
	{
		switch( requestCode )
		{
			case INTENT_MASK:
			{
				mRdoTag.setChecked(MaskActivity.Type == 0);
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	protected boolean initTagList( int id )
	{
		mListTag = (ListView) findViewById(id);
		if( mListTag != null )
		{
			mArrTag = new ArrayList<HashMap<String, String>>();
			mAdapterTag = new SimpleAdapter(this, mArrTag,
					R.layout.list_item_inventory, new String[]
					{ "tag", "summary", "count", "first", "last" }, new int[]
					{ android.R.id.text1, android.R.id.text2 });
			mListTag.setAdapter(mAdapterTag);

			// -- Context Menu
			registerForContextMenu(mListTag);
		}
		return mListTag != null;
	}

	@Override
	public void onCreateContextMenu( ContextMenu menu, View v,
			ContextMenuInfo menuInfo )
	{
		if( v.getId() == R.id.list_tag )
		{
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			menu.setHeaderTitle("Menu");
			HashMap<String, String> hashMap = (HashMap<String, String>) mListTag.getItemAtPosition(info.position);
			setSelTag(hashMap.get("tag"));
			String[] menuItems = getResources().getStringArray(
					R.array.list_menu_inventory);
			for( int i = 0; i < menuItems.length; i++ )
				menu.add(Menu.NONE, i, i, menuItems[ i ]);
		}
	}

	@Override
	public boolean onContextItemSelected( MenuItem item )
	{
		Log.d(TAG, "onContenxtItemSelected : " + item.getItemId());
		switch( item.getItemId() )
		{
			case 0:
			{
				startMaskActivity();
				break;
			}
			case 1:
			{
				clearArrTag();
				break;
			}
			case 2:
				break;
		}
		return true;
	}

	private void setBitsAndOffset( String Offset, String Wordcount,
			boolean bEnabled )
	{
		mEdtTagMemOffset.setText(Offset);
		mEdtTagMemWordcount.setText(Wordcount);

		mEdtTagMemOffset.setEnabled(bEnabled);
		mEdtTagMemWordcount.setEnabled(bEnabled);
	}

	public void onItemSelected( AdapterView<?> parent, View view, int position,
			long id )
	{
		switch( parent.getId() )
		{
			case R.id.spin_epc:
			{
				switch( position )
				{
					case 0: // PC/EPC
						setBitsAndOffset("01", "7", false);
						break;
					case 1: // ACS PWD
						setBitsAndOffset("02", "2", false);
						break;
					case 2: // Kill PWD
						setBitsAndOffset("00", "2", false);
						break;
					case 3: // TID
						setBitsAndOffset("00", "2", false);
						break;
					default:
						setBitsAndOffset("00", "1", true);
						break;
				}
				break;
			}
		}
	}

	public void onNothingSelected( AdapterView<?> parent )
	{
	}

	private void startMaskActivity()
	{
		// if( mSelTag != null )
		{
			Log.d(TAG, "onContextItemSelected : selTag = " + mSelTag);
			
			Intent intent = new Intent(this, MaskActivity.class);
			intent.putExtra("tag", mSelTag);
			startActivityForResult(intent, INTENT_MASK);
		}
	}
	
	private void clearArrTag()
	{
		if( mArrTag != null )
			mArrTag.clear();
		setSelTag(null);
		mHandler.sendEmptyMessage(MSG_REFRESH_LIST_TAG);
	}

	public void onClick( View v )
	{
		switch( v.getId() )
		{
			// --- Link
			case R.id.btn_scan:
				// Add auto connect routine...
				r900UsbManager = new R900UsbManager();
				
				r900UsbManager.setOnUsbEventListener(this);
				
				// Usb init
				r900UsbManager.init(this);				
				r900UsbManager.connect();
				
				setLinkStatus(true);
				setConnectionStatus(true);
				break;
				
			case R.id.btn_disconnect:				
				closeApp();				
				break;

			// --- Inventory
			case R.id.btn_inventory:
			{
				final String LABEL = ( (Button) v ).getText().toString();
				if( LABEL.equalsIgnoreCase("INVENTORY") )
				{
					final MaskActivity.SelectMask selMask = MaskActivity
							.getSelectMask();
					if( MaskActivity.UseMask
							&& selMask.Bits != 0
							&& ( MaskActivity.Type == 0 || ( MaskActivity.Type == 1 && selMask.Bank != 4 ) ) )
					{
						new AlertDialog.Builder(this)
								.setTitle("Inventory")
								.setMessage(
										"Inventory only tags with Access Tag ID ?")
								.setPositiveButton("Yes",
										new DialogInterface.OnClickListener()
										{
											public void onClick(
													DialogInterface dialog,
													int whichButton )
											{
												setupOperationParameter();
												Button v = (Button) findViewById(R.id.btn_inventory);
												( (Button) v ).setText("STOP");
												r900UsbManager.sendCmdInventory();
												R900Status.setOperationMode(1);	// eric 2012.11.29
												mInventLampTx
												.setLamp(LampView.LAMP_RED);
											}
										})
								.setNegativeButton("No",
										new DialogInterface.OnClickListener()
										{
											public void onClick(
													DialogInterface dialog,
													int whichButton )
											{
												MaskActivity.clearSelectMask();

												setupOperationParameter();
												Button v = (Button) findViewById(R.id.btn_inventory);
												( (Button) v ).setText("STOP");
												r900UsbManager.sendCmdInventory(); 
												R900Status.setOperationMode(1);	// eric 2012.11.29
												mInventLampTx
														.setLamp(LampView.LAMP_RED);
											}
										}).create().show();
					}
					else
					{
						if( MaskActivity.UseMask == false
								|| ( MaskActivity.Type == 1 && selMask.Bank == 4 ) )
							MaskActivity.clearSelectMask();
						setupOperationParameter();

						( (Button) v ).setText("STOP");
						r900UsbManager.sendCmdInventory();
						R900Status.setOperationMode(1);	
						mInventLampTx.setLamp(LampView.LAMP_RED);
					}
				}
				else
				{
					( (Button) v ).setText("INVENTORY");
					r900UsbManager.sendCmdStop();
					R900Status.setOperationMode(0);	// eric 2012.11.29
					mInventLampTx.setLamp(LampView.LAMP_GRAY);
				}
				break;
			}

				// --- Access
			case R.id.btn_access:
			{
				final String LABEL = ( (Button) v ).getText().toString();
				if( LABEL.equalsIgnoreCase("Access") )
				{
					( (Button) v ).setText("STOP");
					mBtnMask.setEnabled(false);
					clickAccessButton();
					mAccessLampTx.setLamp(LampView.LAMP_RED);
				}
				else
				{
					( (Button) v ).setText("Access");
					mBtnMask.setEnabled(true);
					r900UsbManager.sendCmdStop();
					mAccessLampTx.setLamp(LampView.LAMP_GRAY);
				}

				break;
			}
			case R.id.radio_read:
				if(r900UsbManager != null)
				{
					r900UsbManager.sendCmdStop();
				}
				mInventLampTx.setLamp(LampView.LAMP_GRAY);
				resetAccessLamp();

				resetMemData(false, false);
				resetSpinEpc(false);
				mAccessType = ACC_TYPE_READ;
				showLayout(mLayoutReadWrite);
				break;
			case R.id.radio_write:
				if(r900UsbManager != null)
				{
					r900UsbManager.sendCmdStop();
				}
				mInventLampTx.setLamp(LampView.LAMP_GRAY);
				resetAccessLamp();

				resetMemData(false, true);
				resetSpinEpc(false);
				mAccessType = ACC_TYPE_WRITE;
				showLayout(mLayoutReadWrite);
				break;
			case R.id.radio_lock:
				if(r900UsbManager != null)
				{
					r900UsbManager.sendCmdStop();
				}
				mInventLampTx.setLamp(LampView.LAMP_GRAY);
				resetAccessLamp();

				resetLockUi(false);
				mAccessType = ACC_TYPE_LOCK;
				showLayout(mLayoutLock);
				break;
			case R.id.radio_kill:
				if(r900UsbManager != null)
				{
					r900UsbManager.sendCmdStop();
				}
				mInventLampTx.setLamp(LampView.LAMP_GRAY);
				resetAccessLamp();

				resetKillUi(false);
				mAccessType = ACC_TYPE_KILL;
				showLayout(mLayoutKill);
				break;
			case R.id.status_lamp_readwrite:
			case R.id.status_lamp_lock:
			case R.id.status_lamp_kill:
			{
				final LampView LAMP = (LampView) findViewById(v.getId());

				if( LAMP.getLamp() == LampView.LAMP_RED
						&& mStrAccessErrMsg != null )
				{
					new AlertDialog.Builder(this)
							.setTitle("Error Message")
							.setMessage(mStrAccessErrMsg)
							.setPositiveButton("Close",
									new DialogInterface.OnClickListener()
									{
										public void onClick(
												DialogInterface dialog,
												int whichButton )
										{
										}
									}).create().show();
				}
				break;
			}

				// --
			case R.id.btn_mask:
				startMaskActivity();
				break;

			case R.id.btn_more:
				mLayoutLockPage1
						.setVisibility(mLayoutLockPage1.getVisibility() == View.VISIBLE ? View.INVISIBLE
								: View.VISIBLE);
				mLayoutLockPage2
						.setVisibility(mLayoutLockPage2.getVisibility() == View.VISIBLE ? View.INVISIBLE
								: View.VISIBLE);
				break;
			// --- Config
			case R.id.chk_single_tag:
				//mSingleTag = mChkSingleTag.isChecked();
				R900Status.setSingleTag(mChkSingleTag.isChecked());
				break;
		}
	}
	
	public void setDisconnect()
	{
		mForceDisconnect = true;
		setConnectionStatus(false);
		
		r900UsbManager.sendCmdStop();
		r900UsbManager.disconnect();
		R900Status.setOperationMode(0);		
		
	}

	// --- SeekBar Listener
	public void onProgressChanged( SeekBar seekBar, int progress,
			boolean fromUser )
	{
		if( seekBar == mSeekPower )
		{
			mTxtPower.setText(TXT_POWER[ progress ]);
			//sendSettingTxPower(TX_POWER[ progress ]);
		}
	}

	public void onStartTrackingTouch( SeekBar seekBar )
	{
	}

	public void onStopTrackingTouch( SeekBar seekBar )
	{
	}

	private void resetMemData( boolean force, boolean enabled )
	{
		if( force
				|| ( mAccessType != ACC_TYPE_READ && mAccessType != ACC_TYPE_WRITE ) )
			mEdtTagMemData.setText("0000");
		mEdtTagMemData.setEnabled(enabled);
	}

	private void resetSpinEpc( boolean force )
	{
		if( force
				|| ( mAccessType != ACC_TYPE_READ && mAccessType != ACC_TYPE_WRITE ) )
			mSpinEpc.setSelection(0);
	}

	private void resetLockUi( boolean force )
	{
		if( force || mAccessType != ACC_TYPE_LOCK )
		{
			mSpinKillPassword.setSelection(0);
			mSpinAccessPassword.setSelection(0);
			mSpinUiiMem.setSelection(0);
			mSpinTidMem.setSelection(0);
			mSpinUserMem.setSelection(0);
		}
	}

	private void resetKillUi( boolean force )
	{
		if( force || mAccessType != ACC_TYPE_KILL )
		{
			mEdtKillPassword.setText("00000000");
			mEdtUserPassword.setText("");
		}
	}

	private void showLayout( LinearLayout layout )
	{
		mLayoutReadWrite.setVisibility(View.INVISIBLE);
		mLayoutLock.setVisibility(View.INVISIBLE);
		mLayoutKill.setVisibility(View.INVISIBLE);
		layout.setVisibility(View.VISIBLE);

		// -- more
		mBtnMore.setVisibility(layout == mLayoutLock ? View.VISIBLE
				: View.INVISIBLE);
	}

	/*
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener()
	{
		public void onItemClick( AdapterView<?> av, View v, int arg2, long arg3 )
		{
			//mR900UsbManager.stopDiscovery();

			// ---
			HashMap<String, String> map = mArrBtDevice.get(arg2);
			if( map == null )
				return;

			// ---
			final String BT_ADDR = map.get("address");
			if( BT_ADDR == null )
				return;

			// ---
			setEnabledLinkCtrl(false);
			setAutoConnectDevice(BT_ADDR);			
		}
	};
	*/

	private void setAutoConnectDevice( String strAddr )
	{
		if( strAddr != null )
		{
			SharedPreferences pref = getSharedPreferences("RFIDHost", 0);
			SharedPreferences.Editor editor = pref.edit();
			editor.putString("auto_link_device", strAddr);
			editor.commit();
		}
	}

	private String getAutoConnectDevice()
	{
		SharedPreferences pref = getSharedPreferences("RFIDHost", 0);
		return pref.getString("auto_link_device", "");
	}
	
	public void clickAccessButton()
	{
		sendCmdAccess();
	}

	public void sendCmdAccess()
	{
		switch( mAccessType )
		{
			case ACC_TYPE_READ:
			{
				setupOperationParameter();
				sendReadTag();
				break;
			}
			case ACC_TYPE_WRITE:
			{
				setupOperationParameter();
				sendWriteTag();
				break;
			}
			case ACC_TYPE_LOCK:
			{
				setupOperationParameter();
				sendLockTag();
				break;
			}
			case ACC_TYPE_KILL:
			{
				setupOperationParameter();
				sendKillTag();
				break;
			}
		}
	}

	public void setupOperationParameter()
	{
		r900UsbManager.sendInventParam(mSpinQuerySession.getSelectedItemPosition(),
				mSpinQueryQ.getSelectedItemPosition(),
				mSpinTargetAB.getSelectedItemPosition());
		final MaskActivity.SelectMask selMask = MaskActivity.getSelectMask();
		boolean bUseMask = false;
		boolean bQuerySelected = ( selMask.Bits > 0 );

		int timeout = 0;
		final String strTimeout = mEdtTimeout.getText().toString();
		if( strTimeout != null && strTimeout.length() > 0 )
		{
			timeout = Integer.parseInt(strTimeout);
			if( timeout < 0 )
				timeout = 0;
			timeout *= 1000;
		}

		if( MaskActivity.UseMask == true && selMask.Bits > 0 )
		{
			if( MaskActivity.Type == 0 )
			{
				if( selMask.Pattern != null && selMask.Pattern.length() != 0 )
				{
					r900UsbManager.sendSetSelectAction(selMask.Bits, selMask.Bank,
							selMask.Offset, selMask.Pattern, 0);
					bUseMask = true;
				}
			}
			else
			{
				if( selMask.Bank != 4 )
				{
					r900UsbManager.sendSetSelectAction(selMask.Bits, selMask.Bank,
							selMask.Offset, selMask.Pattern, 0);
					bUseMask = true;
				}
			}
		}
		r900UsbManager.setOpMode(mChkSingleTag.isChecked(), bUseMask, timeout, bQuerySelected);
	}

	public static class AccessAddress
	{
		public int bank;
		public int offset;
		public int len;
	}

	private AccessAddress getAccessAddress()
	{
		AccessAddress accAddr = new AccessAddress();
		accAddr.bank = mSpinEpc.getSelectedItemPosition();
		accAddr.offset = Integer
				.parseInt(mEdtTagMemOffset.getText().toString());
		accAddr.len = Integer
				.parseInt(mEdtTagMemWordcount.getText().toString());
		switch( accAddr.bank )
		{
			case 0:
				accAddr.bank = 1;
				accAddr.offset = 0x01;
				accAddr.len = 7;
				break;
			case 1:
				accAddr.bank = 0;
				accAddr.offset = 0x02;
				accAddr.len = 2;
				break;
			case 2:
				accAddr.bank = 0;
				accAddr.offset = 0x00;
				accAddr.len = 2;
				break;
			case 3:
				accAddr.bank = 2;
				accAddr.offset = 0x00;
				accAddr.len = 2;
				break;
			default:
				accAddr.bank -= 4;
				break;
		}
		return accAddr;
	}

	public static class LockPattern
	{
		/*
		 * public short lockMask; public short lockEnable;
		 */
		public boolean enableUser;
		public boolean enableTid;
		public boolean enableUii;
		public boolean enableAcsPwd;
		public boolean enableKillPwd;

		public boolean indexUser;
		public boolean indexTid;
		public boolean indexUii;
		public boolean indexAcsPwd;
		public boolean indexKillPwd;

		public boolean lockPerma;
	}

	private short SWAPLSB2( int mark )
	{
		return (short) ( ( ( ( ( ( mark ) & 1 ) << 1 ) | ( ( ( mark ) >> 1 ) & 1 ) ) & 3 ) & 0xffff );
	}

	private LockPattern getLockPattern()
	{
		/*
		 * int mark; short lock_mask = 0; short lock_enable = 0;
		 * 
		 * //--- mark = mSpinKillPassword.getSelectedItemPosition(); lock_enable
		 * |= SWAPLSB2( mark != 0 ? mark - 1 : 0 ); lock_mask |= ( mark == 0 ? 0
		 * : 3 );
		 * 
		 * //--- lock_enable <<= 2; lock_mask <<= 2; mark =
		 * mSpinAccessPassword.getSelectedItemPosition(); lock_enable |=
		 * SWAPLSB2( mark != 0 ? mark - 1 : 0 ); lock_mask |= (mark == 0 ? 0 :
		 * 3);
		 * 
		 * //--- lock_enable <<= 2; lock_mask <<= 2; mark =
		 * mSpinUiiMem.getSelectedItemPosition(); lock_enable |= SWAPLSB2( mark
		 * != 0 ? mark - 1 : 0 ); lock_mask |= (mark == 0 ? 0 : 3);
		 * 
		 * //--- lock_enable <<= 2; lock_mask <<= 2; mark =
		 * mSpinTidMem.getSelectedItemPosition(); lock_enable |= SWAPLSB2( mark
		 * != 0 ? mark - 1 : 0 ); lock_mask |= (mark == 0 ? 0 : 3);
		 * 
		 * //--- lock_enable <<= 2; lock_mask <<= 2; mark =
		 * mSpinUserMem.getSelectedItemPosition(); lock_enable |= SWAPLSB2( mark
		 * != 0 ? mark - 1 : 0 ); lock_mask |= (mark == 0 ? 0 : 3);
		 * 
		 * //--- LockPattern lockPattern = new LockPattern();
		 * lockPattern.lockMask = lock_mask; lockPattern.lockEnable =
		 * lock_enable; return lockPattern;
		 */
		int index = 0;
		LockPattern lockPattern = new LockPattern();
		lockPattern.lockPerma = false;

		index = mSpinKillPassword.getSelectedItemPosition();
		if( index > 2 )
			lockPattern.lockPerma = true;
		lockPattern.enableKillPwd = index != 0;
		lockPattern.indexKillPwd = ( ( index == 1 ) ? false
				: ( ( index == 2 ) ? true : false ) );

		index = mSpinAccessPassword.getSelectedItemPosition();
		if( index > 2 )
			lockPattern.lockPerma = true;
		lockPattern.enableAcsPwd = index != 0;
		lockPattern.indexAcsPwd = ( ( index == 1 ) ? false
				: ( ( index == 2 ) ? true : false ) );

		index = mSpinUiiMem.getSelectedItemPosition();
		if( index > 2 )
			lockPattern.lockPerma = true;
		lockPattern.enableUii = index != 0;
		lockPattern.indexUii = ( ( index == 1 ) ? false
				: ( ( index == 2 ) ? true : false ) );

		index = mSpinTidMem.getSelectedItemPosition();
		if( index > 2 )
			lockPattern.lockPerma = true;
		lockPattern.enableTid = index != 0;
		lockPattern.indexTid = ( ( index == 1 ) ? false
				: ( ( index == 2 ) ? true : false ) );

		index = mSpinUserMem.getSelectedItemPosition();
		if( index > 2 )
			lockPattern.lockPerma = true;
		lockPattern.enableUser = index != 0;
		lockPattern.indexUser = ( ( index == 1 ) ? false
				: ( ( index == 2 ) ? true : false ) );

		return lockPattern;
	}

	public void sendReadTag()
	{
		String strPwd = getPassword();
		AccessAddress accAddr = getAccessAddress();
		r900UsbManager.sendReadTag(accAddr.len, accAddr.bank, accAddr.offset, strPwd);
	}

	public void sendWriteTag()
	{
		final String strPwd = getPassword();
		final String strWordPattern = mEdtTagMemData.getText().toString();
		AccessAddress accAddr = getAccessAddress();
		
		if( strWordPattern != null
				&& ( strWordPattern.length() == ( accAddr.len * 4 ) ) )
			r900UsbManager.sendWriteTag(accAddr.len, accAddr.bank, accAddr.offset, strPwd,
					strWordPattern);
		else
		{
			// ---
			mBtnAccess.setText("Access");
			mAccessLampTx.setLamp(LampView.LAMP_GRAY);

			new AlertDialog.Builder(this)
					.setTitle("Error")
					.setMessage("Write Data and Count are inconsistant.")
					.setPositiveButton("Close",
							new DialogInterface.OnClickListener()
							{
								public void onClick( DialogInterface dialog,
										int whichButton )
								{
								}
							}).create().show();
		}
		
	}

	public static final int LM_KILL_PWD_RW_LOCK = 1 << 9;
	public static final int LM_KILL_PWD_PERM_LOCK = 1 << 8;
	public static final int LM_ACCESS_PWD_RW_LOCK = 1 << 7;
	public static final int LM_ACCESS_PWD_PERM_LOCK = 1 << 6;
	public static final int LM_EPC_MEM_RW_LOCK = 1 << 5;
	public static final int LM_EPC_MEM_PERM_LOCK = 1 << 4;
	public static final int LM_TID_MEM_RW_LOCK = 1 << 3;
	public static final int LM_TID_MEM_PERM_LOCK = 1 << 2;
	public static final int LM_USER_MEM_RW_LOCK = 1 << 1;
	public static final int LM_USER_MEM_PERM_LOCK = 1 << 0;
	public static final int LOCK_PERMA = ( LM_KILL_PWD_PERM_LOCK
			| LM_ACCESS_PWD_PERM_LOCK | LM_EPC_MEM_PERM_LOCK
			| LM_TID_MEM_PERM_LOCK | LM_USER_MEM_PERM_LOCK );

	private boolean CheckAuthority()
	{
		final String strAuth = mEdtUserPassword.getText().toString();
		if( strAuth != null && strAuth.equalsIgnoreCase("tagkiller") )
			return true;
		return false;
	}

	public String getPassword()
	{
		String strPwd = mEdtPassword.getText().toString();
		return "0x" + strPwd.replace("0x", "");
	}

	public String getKillPassword()
	{
		String strPwd = mEdtKillPassword.getText().toString();
		return "0x" + strPwd.replace("0x", "");
	}

	public void sendLockTag()
	{
		String strPwd = getPassword();// mEdtPassword.getText().toString();

		LockPattern lockPattern = getLockPattern();
		
		if( lockPattern.lockPerma && !CheckAuthority() )
		{
			Toast.makeText(
					this,
					"Skip writing permanent lock\nAuthorized personnel only\nPlease Test non permanent only",
					Toast.LENGTH_LONG).show();
			return;
		}		
		r900UsbManager.sendLockTag(lockPattern, strPwd);
	}

	private void resetInventoryButton()
	{
		mBtnInventory.setText("INVENTORY");
		mInventLampTx.setLamp(LampView.LAMP_GRAY);
	}

	private void resetAccessButton()
	{
		mBtnAccess.setText("Access");
		mBtnMask.setEnabled(true);
		mAccessLampTx.setLamp(LampView.LAMP_GRAY);
	}

	public void sendKillTag()
	{
		String killPwd = getKillPassword();// mEdtKillPassword.getText().toString();
		killPwd = killPwd.replace("0x", "");
		killPwd = "0x" + killPwd;

		if( killPwd == null || killPwd.length() != ( 2 + 8 ) )
		{
			Toast.makeText(this, "Kill PWD must be 8 hexadecimal digits",
					Toast.LENGTH_LONG).show();
			resetAccessButton();
			return;
		}

		if( mRdoTag.isChecked() == false )
		{
			Toast.makeText(this, "Designate tag to kill by <Access Tag ID>",
					Toast.LENGTH_LONG).show();
			resetAccessButton();
			return;
		}

		if( CheckAuthority() == false )
		{
			Toast.makeText(this, "Skip killing tag\nAuthorized personnel only",
					Toast.LENGTH_LONG).show();
			resetAccessButton();
			return;
		}

		r900UsbManager.sendKillTag(killPwd);
	}

	private void setEnabledLinkCtrl( boolean bEnable )
	{
		if( bEnable )
			mHandler.sendEmptyMessageDelayed(MSG_ENABLE_LINK_CTRL, 50);
		else
			mHandler.sendEmptyMessageDelayed(MSG_DISABLE_LINK_CTRL, 50);
	}

	private void setEnabledBtnDisconnect( boolean bEnable )
	{
		if( bEnable )
			mHandler.sendEmptyMessageDelayed(MSG_ENABLE_DISCONNECT, 50);
		else
			mHandler.sendEmptyMessageDelayed(MSG_DISABLE_DISCONNECT, 50);
	}

	private void showToastByOtherThread( String msg, int time )
	{
		mHandler.removeMessages(MSG_SHOW_TOAST);
		mHandler.sendMessage(mHandler.obtainMessage(MSG_SHOW_TOAST, time, 0,
				msg));
	}
	
	private void setListStatus( UsbDevice device, String msg )
	{
		final String ADDR = device != null ? device.getDeviceName() : null;
		for( HashMap<String, String> map : mArrBtDevice )
		{
			final String tmpADDR = map.get("address");
			if( ADDR != null && ADDR.equals(tmpADDR) )
			{
				map.put("status", msg);
				map.put("summary", tmpADDR + " - " + msg);
			}
			else
			{
				map.put("status", "");
				map.put("summary", tmpADDR);
			}
		}
		mHandler.removeMessages(MSG_REFRESH_LIST_DEVICE);
		mHandler.sendEmptyMessage(MSG_REFRESH_LIST_DEVICE);
	}

	private void setLinkStatus( boolean bConnected )
	{
		if( mExit == true )
			return;
		
		if( bConnected == false )
		{
			mInventLampOn.startBlink(LampView.LAMP_GRAY, LampView.LAMP_GREEN,
					LampView.BLINK_INTERVAL, true);
			mInventLampTx.setLamp(LampView.LAMP_GRAY);
			mInventLampRx.setLamp(LampView.LAMP_GRAY);

			mAccessLampOn.startBlink(LampView.LAMP_GRAY, LampView.LAMP_GREEN,
					LampView.BLINK_INTERVAL, true);
			mAccessLampTx.setLamp(LampView.LAMP_GRAY);
			mAccessLampRx.setLamp(LampView.LAMP_GRAY);
		}
		else
		{
			mInventLampOn.setLamp(LampView.LAMP_GREEN);
			mAccessLampOn.setLamp(LampView.LAMP_GREEN);
		}

		mHandler.sendEmptyMessage(bConnected ? MSG_LINK_ON : MSG_LINK_OFF);
	}

	private void setConnectionStatus( boolean bConnected )
	{
		
		if( mExit == true )
			return;
		
		mConnected = bConnected;
		
		setLinkStatus(bConnected);		
	}
	
	public void onUsbDisconnected(UsbDevice device)
	{
		setEnabledLinkCtrl(true);
		showToastByOtherThread("Disconnected..", Toast.LENGTH_SHORT);
		
		setConnectionStatus(false);
				
		onBackPressed();		// eric 2012.12.12
	}	
	
	private static final int MSG_TOAST = 100;
	private Handler mHandlerToast = new Handler()
	{
		@Override
		public void handleMessage( Message msg )
		{
			switch( msg.what )
			{
				case MSG_TOAST:
				{
					if( mToast != null )
						mToast.cancel();
					Bundle bundle = msg.getData();
					mToast = Toast.makeText(RFIDUsbHostActivity.this,
							bundle.getString("msg"), Toast.LENGTH_LONG);
					mToast.show();
					break;
				}
			}
		}
	};

	private Toast mToast;
	private LogfileMng mLogMng = new LogfileMng();	

	
	// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	// process packet
	// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
		// process packet
		private synchronized void processPacket( final String param )
		{
			if( param == null || param.length() <= 0 )
				return;

			final String CMD = param.toLowerCase();// ( param.length() >= 2 ?
												   // param.substring(0, 2) : param
												   // ).toLowerCase();
			if( CMD.indexOf("^") == 0 || CMD.indexOf("$") == 0
					|| CMD.indexOf("ok") == 0 || CMD.indexOf("err") == 0
					|| CMD.indexOf("end") == 0 )
			{
				/*
				if( CMD.indexOf( "$>" ) == 0 )
				{
					if( mConnected == false )
						setConnectionStatus(true);
				}
				*/						
				
				if( mConnected == false )
					return;
				
				if( CMD.indexOf("$trigger=1") == 0 )
				{
					if( mTabMode == TAB_INVENTORY )
					{
						mBtnInventory.setText("STOP");
						mBtnInventory.setEnabled(false);
						if(r900UsbManager != null)
						{
							setupOperationParameter();
							r900UsbManager.sendCmdInventory();
						}
						R900Status.setOperationMode(1);
						//mOp = true;	// eric 2012.11.23
						mInventLampTx.setLamp(LampView.LAMP_RED);
					}
					else if( mTabMode == TAB_ACCESS )
					{
						mBtnAccess.setText("STOP");
						mBtnAccess.setEnabled(false);
						if(r900UsbManager != null)
						{
							setupOperationParameter();
							sendCmdAccess();
						}
						mAccessLampTx.setLamp(LampView.LAMP_RED);
					}
				}
				else if( CMD.indexOf("$trigger=0") == 0 )
				{
					if( mTabMode == TAB_INVENTORY )
					{
						mBtnInventory.setText("INVENTORY");
						mBtnInventory.setEnabled(true);
						r900UsbManager.sendCmdStop();
						R900Status.setOperationMode(0);
						mInventLampTx.setLamp(LampView.LAMP_GRAY);
					}
					else if( mTabMode == TAB_ACCESS )
					{
						mBtnAccess.setText("Access");
						mBtnAccess.setEnabled(true);
						r900UsbManager.sendCmdStop();
						R900Status.setOperationMode(0);
						mAccessLampTx.setLamp(LampView.LAMP_GRAY);
					}
				}
				else if( CMD.indexOf("$online=0") == 0 )
				{
					setConnectionStatus(false);
				}
				else if( CMD.indexOf("end") == 0 )
				{
					if( mTabMode == TAB_INVENTORY )
					{
						final int idxComma = CMD.indexOf(",");
						if( idxComma > 4 )
						{
							final String strNum = CMD.substring(4, idxComma);
							final String strErr = getEndMsg(strNum);
							if( strErr != null )
								mStrAccessErrMsg = strErr; 
						}
					}
					else if( mTabMode == TAB_ACCESS )
					{
						final int idxComma = CMD.indexOf(",");
						if( idxComma > 4 )
						{
							final String strNum = CMD.substring(4, idxComma);
							//mStrAccessErrMsg = getEndMsg(strNum);
							final String strErr = getEndMsg(strNum);
							if( strErr != null )
								mStrAccessErrMsg = strErr;
						}
					}

					// ---
					resetAccessButton();
					resetInventoryButton();

					setAccessLampStatus(mStrAccessErrMsg == null ? LampView.LAMP_GREEN : LampView.LAMP_RED);
				}
				else if( CMD.indexOf("err") == 0 || CMD.indexOf("ok") == 0 )
				{
					if( mTabMode == TAB_INVENTORY )
					{

					}
					else if( mTabMode == TAB_ACCESS )
					{
						switch( mAccessType )
						{
							case ACC_TYPE_READ:
								updateAccessDataRead(param);
								break;
							case ACC_TYPE_WRITE:
								updateAccessDataWrite(param);
								break;
							case ACC_TYPE_LOCK:
								updateAccessDataLock(param);
								break;
							case ACC_TYPE_KILL:
								updateAccessDataKill(param);
								break;
						}
					}

					// ---
					if( CMD.indexOf("ok") == 0 )
						mStrAccessErrMsg = null;
				}
			}
			else
			{
				if( r900UsbManager.mLastCmd == null )
					return;
				
				if( mTabMode == TAB_INVENTORY )
				{
					if( r900UsbManager.mLastCmd.equalsIgnoreCase(R900Protocol.CMD_INVENT) )
						updateTagCount(param);
				}
				else if( mTabMode == TAB_ACCESS )
				{
					switch( mAccessType )
					{
						case ACC_TYPE_READ:
							updateAccessDataRead(param);
							break;
						case ACC_TYPE_WRITE:
							updateAccessDataWrite(param);
							break;
						case ACC_TYPE_LOCK:
							updateAccessDataLock(param);
							break;
						case ACC_TYPE_KILL:
							updateAccessDataKill(param);
							break;
					}
				}
				
			}
		}

	private void updateAccessDataKill( final String param )
	{
		if( param == null || param.length() <= 4 )
			return;

		final String tagId = param.substring(0, param.length() - 4);
		mAccessLampRx.startBlink(LampView.LAMP_GRAY, LampView.LAMP_GREEN, 10,
				false, false, true);
		if( mHandler.hasMessages(MSG_SOUND_RX) == false )
			mHandler.sendEmptyMessage(MSG_SOUND_RX);

		if( param.equalsIgnoreCase("ok") )
		{
			mStrAccessErrMsg = null;
			setAccessLampDetect(LampView.LAMP_GREEN);
			setAccessLampStatus(LampView.LAMP_GREEN);
			return;
		}

		final String packet = param.toLowerCase();
		final int index = packet.indexOf(",e=");
		if( index > 0 && ( index + 3 ) < ( packet.length() - 4 ) )
		{
			final String strTagId = packet.substring(0, index);
			boolean bStatusOk = ( strTagId.indexOf("ok") == 0 );

			final String strTag = packet.substring(index + 3,
					packet.length() - 4);
			// mEdtTagId.setText( strTag );
			if( bStatusOk )
				mEdtTagMemData.setText(strTagId);
			else
				mEdtTagMemData.setText("");

			setAccessLampDetect(LampView.LAMP_GREEN);
			// setAccessLampStatus( bStatusOk ? LampView.LAMP_GREEN :
			// LampView.LAMP_RED );

			if( bStatusOk == false )
			{
				final int errTagPrefix = strTagId.indexOf("err_tag=");
				if( errTagPrefix == 0 )
				{
					final String strTagError = strTagId.substring(errTagPrefix,
							strTagId.length());
					mStrAccessErrMsg = getTagErrorMsg(strTagError);
				}
				else
				{
					final int errOpPrefix = strTagId.indexOf("err_op=");
					if( errOpPrefix == 0 )
					{
						final String strOpError = strTagId.substring(
								errOpPrefix, strTagId.length());
						mStrAccessErrMsg = "Operation Error : " + strOpError;
					}
					else
						mStrAccessErrMsg = null;
				}
			}
			setAccessLampStatus(mStrAccessErrMsg == null ? LampView.LAMP_GREEN
					: LampView.LAMP_RED);
		}

		if( mChkSingleTag.isChecked() )
		{
			mBtnAccess.setText("Access");
			mAccessLampTx.setLamp(LampView.LAMP_GRAY);
		}
	}

	private void updateAccessDataLock( final String param )
	{
		if( param == null || param.length() <= 4 )
			return;

		final String tagId = param.substring(0, param.length() - 4);
		mAccessLampRx.startBlink(LampView.LAMP_GRAY, LampView.LAMP_GREEN, 10,
				false, false, true);
		if( mHandler.hasMessages(MSG_SOUND_RX) == false )
			mHandler.sendEmptyMessage(MSG_SOUND_RX);

		if( param.equalsIgnoreCase("ok") )
		{
			mStrAccessErrMsg = null;
			setAccessLampDetect(LampView.LAMP_GREEN);
			setAccessLampStatus(LampView.LAMP_GREEN);
			return;
		}

		final String packet = param.toLowerCase();
		final int index = packet.indexOf(",e=");
		if( index > 0 && ( index + 3 ) < ( packet.length() - 4 ) )
		{
			final String strTagId = packet.substring(0, index);
			boolean bStatusOk = ( strTagId.indexOf("ok") == 0 );

			final String strTag = packet.substring(index + 3,
					packet.length() - 4);
			// mEdtTagId.setText( strTag );
			if( bStatusOk )
				mEdtTagMemData.setText(strTagId);
			else
				mEdtTagMemData.setText("");

			setAccessLampDetect(LampView.LAMP_GREEN);
			// setAccessLampStatus( bStatusOk ? LampView.LAMP_GREEN :
			// LampView.LAMP_RED );

			if( bStatusOk == false )
			{
				final int errTagPrefix = strTagId.indexOf("err_tag=");
				if( errTagPrefix == 0 )
				{
					final String strTagError = strTagId.substring(errTagPrefix,
							strTagId.length());
					mStrAccessErrMsg = getTagErrorMsg(strTagError);
				}
				else
				{
					final int errOpPrefix = strTagId.indexOf("err_op=");
					if( errOpPrefix == 0 )
					{
						final String strOpError = strTagId.substring(
								errOpPrefix, strTagId.length());
						mStrAccessErrMsg = "Operation Error : " + strOpError;
					}
					else
						mStrAccessErrMsg = null;
				}
			}
			else
				mStrAccessErrMsg = null;
			setAccessLampStatus(mStrAccessErrMsg == null ? LampView.LAMP_GREEN
					: LampView.LAMP_RED);
		}

		if( mChkSingleTag.isChecked() )
		{
			mBtnAccess.setText("Access");
			mAccessLampTx.setLamp(LampView.LAMP_GRAY);
		}
	}

	private void updateAccessDataWrite( final String param )
	{
		if( param == null || param.length() <= 4 )
			return;

		final String tagId = param.substring(0, param.length() - 4);
		mAccessLampRx.startBlink(LampView.LAMP_GRAY, LampView.LAMP_GREEN, 10,
				false, false, true);
		if( mHandler.hasMessages(MSG_SOUND_RX) == false )
			mHandler.sendEmptyMessage(MSG_SOUND_RX);

		if( param.equalsIgnoreCase("ok") )
		{
			mStrAccessErrMsg = null;
			setAccessLampDetect(LampView.LAMP_GREEN);
			setAccessLampStatus(LampView.LAMP_GREEN);
			return;
		}

		final String packet = param.toLowerCase();
		final int index = packet.indexOf(",e=");
		if( index > 0 && ( index + 3 ) < ( packet.length() - 4 ) )
		{
			final String strTagId = packet.substring(0, index);
			boolean bStatusOk = ( strTagId.indexOf("ok") == 0 );

			final String strTag = packet.substring(index + 3,
					packet.length() - 4);
			// mEdtTagId.setText( strTag );
			if( bStatusOk )
				mEdtTagMemData.setText(strTagId);
			else
				mEdtTagMemData.setText("");

			setAccessLampDetect(LampView.LAMP_GREEN);

			if( bStatusOk == false )
			{
				final int errTagPrefix = strTagId.indexOf("err_tag=");
				if( errTagPrefix == 0 )
				{
					final String strTagError = strTagId.substring(8,
							strTagId.length());
					mStrAccessErrMsg = getTagErrorMsg(strTagError);
				}
				else
				{
					final int errOpPrefix = strTagId.indexOf("err_op=");
					if( errOpPrefix == 0 )
					{
						final String strOpError = strTagId.substring(7,
								strTagId.length());
						mStrAccessErrMsg = "Operation Error : " + strOpError;
					}
					else
						mStrAccessErrMsg = null;
				}
			}
			else
				mStrAccessErrMsg = null;
			setAccessLampStatus(mStrAccessErrMsg == null ? LampView.LAMP_GREEN
					: LampView.LAMP_RED);
		}

		if( mChkSingleTag.isChecked() )
		{
			mBtnAccess.setText("Access");
			mAccessLampTx.setLamp(LampView.LAMP_GRAY);
		}
	}

	private void updateAccessDataRead( final String param )
	{
		if( param == null || param.length() <= 4 )
			return;

		mStrAccessErrMsg = null;
		final String tagId = param.substring(0, param.length() - 4);

		mAccessLampRx.startBlink(LampView.LAMP_GRAY, LampView.LAMP_GREEN, 10,
				false, true, true);
		if( mHandler.hasMessages(MSG_SOUND_RX) == false )
			mHandler.sendEmptyMessage(MSG_SOUND_RX);

		final String packet = param.toLowerCase();
		final int index = packet.indexOf(",e=");
		if( index > 0 && ( index + 3 ) < ( packet.length() - 4 ) )
		{
			final String strTagId = packet.substring(0, index);
			final String strTag = packet.substring(index + 3,
					packet.length() - 4);
			mEdtTagId.setText(strTag);

			boolean bStatusOk = true;
			final int errTagPrefix = strTagId.indexOf("err_tag=");
			if( errTagPrefix == 0 )
			{
				final String strTagError = strTagId.substring(8,
						strTagId.length());
				mStrAccessErrMsg = getTagErrorMsg(strTagError);
				bStatusOk = false;
			}
			else
			{
				final int errOpPrefix = strTagId.indexOf("err_op=");
				if( errOpPrefix == 0 )
				{
					final String strOpError = strTagId.substring(7,
							strTagId.length());
					mStrAccessErrMsg = "Operation Error : " + strOpError;
					bStatusOk = false;
				}
				else
					mStrAccessErrMsg = null;
			}

			if( bStatusOk )
				mEdtTagMemData.setText(strTagId);
			else
				mEdtTagMemData.setText("");

			setAccessLampDetect(LampView.LAMP_GREEN);
			setAccessLampStatus(mStrAccessErrMsg == null ? LampView.LAMP_GREEN
					: LampView.LAMP_RED);
		}
		else
			setAccessLampStatus(LampView.LAMP_RED);

		// --
		if( mChkSingleTag.isChecked() )
		{
			mBtnAccess.setText("Access");
			mAccessLampTx.setLamp(LampView.LAMP_GRAY);
		}
	}

	private void updateTagCount( final String param )
	{
		if( param == null || param.length() <= 4 )
			return;

		mStrAccessErrMsg = null;
		final String tagId = param.substring(0, param.length() - 4);
		mInventLampRx.startBlink(LampView.LAMP_GRAY, LampView.LAMP_GREEN, 10,
				false, true, true);

		// ---
		boolean bUpdate = false;
		for( HashMap<String, String> map : mArrTag )
		{
			if( tagId.equals(map.get("tag")) )
			{
				if( mChkSkipSame.isChecked() == false )
				{
					if( mHandler.hasMessages(MSG_SOUND_RX) == false )
						mHandler.sendEmptyMessage(MSG_SOUND_RX);

					final String count = String.valueOf(Integer.parseInt(map
							.get("count")) + 1);
					Date curDate = new Date();
					final String last = curDate.toLocaleString();
					map.put("count", String.valueOf(count));
					map.put("last", last);
					map.put("summary", String.format("- count\t: %s", count));
				}
				else
				{
					if( mHandler.hasMessages(MSG_SOUND_RX_HALF) == false )
						mHandler.sendEmptyMessage(MSG_SOUND_RX_HALF);
				}
				bUpdate = true;
				break;
			}
		}

		// ---
		if( bUpdate == false )
		{
			if( mHandler.hasMessages(MSG_SOUND_RX) == false )
				mHandler.sendEmptyMessage(MSG_SOUND_RX);

			HashMap<String, String> map = new HashMap<String, String>();
			Date curDate = new Date();
			final String count = "1";
			final String first = curDate.toLocaleString();
			map.put("tag", tagId);
			map.put("count", count);
			map.put("first", first);
			map.put("last", first);
			map.put("summary", String.format("- count\t: %s", count));
			mArrTag.add(map);

			// --
			if( mSelTag == null )
				setSelTag(tagId);
		}

		// --
		if( mChkSingleTag.isChecked() )
		{
			resetInventoryButton();
		}
	}

	private void setSelTag( String strTag )
	{
		mSelTag = strTag;
	}

	private void setAccessTagId( String strRead, String strTagId )
	{
		// ---
		if( strRead == null )
			mEdtTagId.setText("");
		else
			mEdtTagId.setText(strRead);

		// ---
		if( strTagId == null )
			mEdtTagMemData.setText("");
		else
			mEdtTagMemData.setText(strTagId);
	}

	protected void finalize()
	{
		//mExit = true;
		
		if(r900UsbManager != null)
		{
			r900UsbManager.sendCmdStop();
			r900UsbManager.disconnect();
			R900Status.setOperationMode(0);
		}
		
		/*
		mLampDetectKill.stopBlink();
		mLampDetectLock.stopBlink();
		mLampDetectReadWrite.stopBlink();
		mLampStatusKill.stopBlink();
		mLampStatusLock.stopBlink();
		mLampStatusReadWrite.stopBlink();
		*/
			
		/*
		if( mLogMng != null )
			mLogMng.finalize();
		 */
		
		//super.finalize();
	}
	
	public void closeApp()
	{
		finalize();		
		finish();
	}

	public void postCloseApp()
	{
		mHandler.sendEmptyMessageDelayed(MSG_QUIT, 1000);
	}

	@Override
	public void onBackPressed()
	{
		/*
		 * if( mLogMng != null ) mLogMng.finalize(); finalize(); finish();
		 */
		
		RFIDUsbHostActivity.this.postCloseApp();
		RFIDUsbHostActivity.this.closeApp();
		RFIDUsbHostActivity.this.finish();
	}

	@Override
	public void onUsbConnected(UsbDevice device) 
	{
		Log.d(TAG, "OnUsbConnected");
		
		setEnabledLinkCtrl(true);
			
		showToastByOtherThread("Connect OK", Toast.LENGTH_SHORT);
	
		setConnectionStatus(true);
		r900UsbManager.sendCmdOpenInterface1();
		mForceDisconnect = false;
			
		// start the battery Gauge Monitoring..
		//onBattGaugingStart();	// eric 2012.11.23			
	}

	@Override
	public void onUsbConnectFail(UsbDevice device, String msg) 
	{
		// TODO Auto-generated method stub
		showToastByOtherThread("ConnectFail", Toast.LENGTH_SHORT);
	}

	@Override
	public void onNotifyUsbDataRecv() 
	{
		// TODO Auto-generated method stub

		if( r900UsbManager == null )
			return;

		R900RecvPacketParser packetParser = r900UsbManager.getRecvPacketParser();
		// ---
		while( true )
		{
			final String parameter = packetParser.popPacket();
			
			if( mConnected == false )
				break;
			
			if( parameter != null )
			{
				Log.d(TAG, "onUsbDataRecv : [parameter = " + parameter + "]");
				processPacket(parameter);
			}
			else
			{
				break;
			}
		}

		// ---		
		mHandler.sendEmptyMessage(MSG_REFRESH_LIST_TAG);		
	}	
}













