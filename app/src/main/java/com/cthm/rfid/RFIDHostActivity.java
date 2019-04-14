package com.cthm.rfid;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import com.cthm.libr900.BluetoothActivity;
import com.cthm.libr900.OnBtEventListener;
import com.cthm.libr900.R900Protocol;
import com.cthm.libr900.R900RecvPacketParser;
import com.dotel.rfid.R;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.widget.AdapterView.OnItemClickListener;
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

public class RFIDHostActivity extends BluetoothActivity implements
		View.OnClickListener, OnBtEventListener,
		AdapterView.OnItemSelectedListener, TabHost.OnTabChangeListener,
		SeekBar.OnSeekBarChangeListener
{

	private TabHost mTab;
	public static SoundManager mSoundManager = new SoundManager();
	private String mStrAccessErrMsg;
	private boolean mAllive;
	private boolean mForceDisconnect;
	private BluetoothDevice mConnectedDevice;

	Context context = this;		// eric 2012.11.30

	//private boolean mOp;		// eric 2012.11.23
	private boolean mBattflag;	// eric 2012.11.23
	private boolean mBattRun;	// eric 2013.4.9
	//private int mBattLevel;	// eric 2012.11.23

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


	private Spinner mSpinEpc;
	private EditText mEdtTagMemOffset;
	private EditText mEdtTagMemWordcount;
	private EditText mEdtTagMemData;

	private Spinner mSpinKillPassword;
	private Spinner mSpinAccessPassword;
	private Spinner mSpinUiiMem;
	private Spinner mSpinTidMem;
	private Spinner mSpinUserMem;
	private LinearLayout mLayoutLockPage1;
	private LinearLayout mLayoutLockPage2;
	private Button mBtnMore;

	private EditText mEdtKillPassword;
	private EditText mEdtUserPassword;

	private Button mBtnMask;

	// --- Config
	private CheckBox mChkAutoLink;
	private CheckBox mChkDetectSound;
	private CheckBox mChkSkipSame;
	private CheckBox mChkSingleTag;
	private CheckBox mChkContinuous;

	private Spinner mSpinQuerySession;
	private Spinner mSpinTargetAB;
	private Spinner mSpinQueryQ;
	private EditText mEdtTimeout;
	private TextView mTxtPower;
	private TextView mTxtDuty;	// eric 2013.4.9
	private SeekBar mSeekPower;
	private SeekBar mSeekDuty;	// eric 2013.4.9
	private ProgressBar mProgress;	// eric 2012.11.23
	private TextView mBattPower;	// eric 2012.11.23

	private static final String[] TXT_POWER =
	{ "Max", "-1dB", "-2dB", "-3dB", "-4dB", "-5dB", "-6dB", "-7dB", "-8dB",
			"-9dB" };
	private static final int[] TX_POWER =
	{ 0, -1, -2, -3, -4, -5, -6, -7, -8, -9 };

	//<-- eric 2013.4.9
	private static final String[] TXT_DUTY =
	{ "90%", "80%", "60%", "41%", "20%" };

	private static final int[] TX_DUTY_OFF =
	{ 10, 40, 80, 100, 160, 180 };

	private static final int[] TX_DUTY_ON =
	{ 190, 160, 70, 40, 20 };
	//--> eric 2013.4.9

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
				sendCmdStop();
				byeBluetoothDevice();
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

	private Handler mHandlerAutoLink = new Handler()
	{
		@Override
		public void handleMessage( final Message msg )
		{
			switch( msg.what )
			{
				case MSG_AUTO_LINK:
				{
					if( mAllive == true && mForceDisconnect == false )
					{
						final String strAutoLinkDevice = getAutoConnectDevice();
						if( mConnected == false
								&& strAutoLinkDevice != null
								&& strAutoLinkDevice.length() > 0
								&& RFIDHostActivity.this.mChkAutoLink
										.isChecked() )
						{
							try
							{
								if( mR900Manager.isTryingConnect() == false )
									mR900Manager.connectToBluetoothDevice(
											strAutoLinkDevice, MY_UUID);
							}
							catch( Exception ex )
							{
								ex.printStackTrace();
							}
						}
					}

					if( mExit == false )
						sendEmptyMessageDelayed(MSG_AUTO_LINK, 5000);
					break;
				}
			}
		}
	};

	//<-- eric 2012.11.23
	private Handler mBattHandler = new Handler()
	{
		@Override
		public void handleMessage(final Message msg)
		{
			switch(msg.what)
			{
				case MSG_BATTERY_CTRL:
				{
					// Set the text(power level)
					final String str = Integer.toString(R900Status.getBatteryLevel()) + "%";
					mBattPower.setText(str);

					// Set the progress bar(power level)
					//mProgress.setProgress(mBattLevel);
					mProgress.setProgress(R900Status.getBatteryLevel());
					break;
				}
			}
		}
	};
	//--> eric 2012.11.23

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
				case MSG_AUTO_REFRESH_DEVICE:
					// refreshBluetoothDevice();
					// sendEmptyMessageDelayed(MSG_AUTO_REFRESH_DEVICE, 3000);
					break;
				case MSG_ENABLE_LINK_CTRL:
					mListDevice.setEnabled(true);
					mBtnScan.setEnabled(true);
					break;
				case MSG_DISABLE_LINK_CTRL:
					mListDevice.setEnabled(false);
					mBtnScan.setEnabled(false);
					break;
				case MSG_ENABLE_DISCONNECT:
				{
					mBtnDisconnect.setEnabled(true);
					mBtnInventory.setText("INVENTORY");
					mBtnAccess.setText("Access");
					//setListStatus(null, "");
					break;
				}
				case MSG_DISABLE_DISCONNECT:
					mBtnDisconnect.setEnabled(false);
					break;
				case MSG_SHOW_TOAST:
					Toast.makeText(RFIDHostActivity.this, (String) msg.obj,
							msg.arg1).show();
					break;
				case MSG_REFRESH_LIST_DEVICE:
					mAdapterDevice.notifyDataSetChanged();
					break;
				case MSG_REFRESH_LIST_TAG:
				{
					// mSoundManager.playSound( 0 );
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
					if( RFIDHostActivity.this.isDetectSoundOn() )
						mSoundManager.playSound(0);
					break;
				}
				case MSG_SOUND_RX_HALF:
				{
					if( RFIDHostActivity.this.isDetectSoundOn() )
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
		mHandlerAutoLink.removeMessages( MSG_AUTO_LINK );
		mHandlerAutoLink.sendEmptyMessageDelayed(MSG_AUTO_LINK, 1000);
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
			sendCmdStop();
		//mOp = false;	// eric 2012.11.23
		//R900Status.setOperationMode(0);	// eric 2012.11.29
	}

	@Override
	public void onStop()
	{
		super.onStop();
		mAllive = false;
		mHandlerAutoLink.removeMessages(MSG_AUTO_LINK);

		int mode = R900Status.getInterfaceMode();
		if(mode == 1)
		{
			if( mConnected )
				sendCmdStop();
		}

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

		Log.i(TAG, "RFIDHostActivity.java......onCreate()~!~!~!~!~!~!~!~!~!");

		// -- ui
		mTab = getTabHost();
		LayoutInflater inflater = LayoutInflater.from(this);
		inflater.inflate(R.layout.rfid_host, mTab.getTabContentView(), true);
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
		initBluetoothDeviceList(R.id.list_btdevice);
		initTagList(R.id.list_tag);

		mBtnScan = findViewById(R.id.btn_scan);
		mBtnScan.setOnClickListener(this);

		mBtnDisconnect = findViewById(R.id.btn_disconnect);
		mBtnDisconnect.setOnClickListener(this);
		mBtnDisconnect.setEnabled(false);

		// -- Inventory
		mInventLampOn = findViewById(R.id.invent_lamp_on);
		mInventLampTx = findViewById(R.id.invent_lamp_tx);
		mInventLampRx = findViewById(R.id.invent_lamp_rx);
		mBtnInventory = findViewById(R.id.btn_inventory);
		mBtnInventory.setOnClickListener(this);

		mTxtTagCount = findViewById(R.id.txt_tagcount);
		mTxtTagCount.setOnClickListener(this);

		// -- Access
		mAccessLampOn = findViewById(R.id.access_lamp_on);
		mAccessLampTx = findViewById(R.id.access_lamp_tx);
		mAccessLampRx = findViewById(R.id.access_lamp_rx);

		mLampDetectReadWrite = findViewById(R.id.detect_lamp_readwrite);
		mLampDetectLock = findViewById(R.id.detect_lamp_lock);
		mLampDetectKill = findViewById(R.id.detect_lamp_kill);
		mLampStatusReadWrite = findViewById(R.id.status_lamp_readwrite);
		mLampStatusReadWrite.setOnClickListener(this);
		mLampStatusLock = findViewById(R.id.status_lamp_lock);
		mLampStatusLock.setOnClickListener(this);
		mLampStatusKill = findViewById(R.id.status_lamp_kill);
		mLampStatusKill.setOnClickListener(this);

		mLayoutReadWrite = findViewById(R.id.layout_read_write);
		mLayoutReadWrite.setVisibility(View.VISIBLE);
		mLayoutLock = findViewById(R.id.layout_lock);
		mLayoutLock.setVisibility(View.INVISIBLE);
		mLayoutKill = findViewById(R.id.layout_kill);
		mLayoutKill.setVisibility(View.INVISIBLE);

		mBtnAccess = findViewById(R.id.btn_access);
		mBtnAccess.setOnClickListener(this);
		mEdtPassword = findViewById(R.id.edt_password);

		mRdoRead = findViewById(R.id.radio_read);
		mRdoRead.setOnClickListener(this);
		mRdoRead.setChecked(true);
		mRdoWrite = findViewById(R.id.radio_write);
		mRdoWrite.setOnClickListener(this);
		mRdoLock = findViewById(R.id.radio_lock);
		mRdoLock.setOnClickListener(this);
		mRdoKill = findViewById(R.id.radio_kill);
		mRdoKill.setOnClickListener(this);

		mRdoTag = findViewById(R.id.radio_tag);
		mRdoTag.setChecked(true);
		mEdtTagId = findViewById(R.id.edt_tag);

		// ------- Access::read/write
		mSpinEpc = findViewById(R.id.spin_epc);
		mSpinEpc.setOnItemSelectedListener(this);
		mEdtTagMemOffset = findViewById(R.id.edt_tag_mem_offset);
		mEdtTagMemWordcount = findViewById(R.id.edt_tag_mem_wordcount);
		mEdtTagMemData = findViewById(R.id.edt_tag_mem_data);

		// ------- Access::lock
		mSpinKillPassword = findViewById(R.id.spin_kill_pwd);
		mSpinAccessPassword = findViewById(R.id.spin_access_pwd);
		mSpinUiiMem = findViewById(R.id.spin_uii_mem);
		mSpinTidMem = findViewById(R.id.spin_tid_mem);
		mSpinUserMem = findViewById(R.id.spin_user_mem);
		mLayoutLockPage1 = findViewById(R.id.layout_lock_page1);
		mLayoutLockPage1.setVisibility(View.VISIBLE);
		mLayoutLockPage2 = findViewById(R.id.layout_lock_page2);
		mLayoutLockPage2.setVisibility(View.INVISIBLE);
		mBtnMore = findViewById(R.id.btn_more);
		mBtnMore.setVisibility(View.INVISIBLE);
		mBtnMore.setOnClickListener(this);

		// ------- Access::kill
		mEdtKillPassword = findViewById(R.id.edt_kill_password);
		mEdtUserPassword = findViewById(R.id.edt_user_password);

		mBtnMask = findViewById(R.id.btn_mask);
		mBtnMask.setOnClickListener(this);

		// -- Config
		mChkAutoLink = findViewById(R.id.chk_auto_link);
		mChkDetectSound = findViewById(R.id.chk_detect_sound);
		mChkSkipSame = findViewById(R.id.chk_skip_same);
		mChkSingleTag = findViewById(R.id.chk_single_tag);
		mChkSingleTag.setOnClickListener(this);
		mChkContinuous = findViewById(R.id.chk_continuous);

		mSpinQuerySession = findViewById(R.id.spin_query_session);
		mSpinTargetAB = findViewById(R.id.spin_target_ab);
		mSpinQueryQ = findViewById(R.id.spin_query_q);
		mEdtTimeout = findViewById(R.id.edt_query_timeout);
		mTxtPower = findViewById(R.id.txt_power);
		mSeekPower = findViewById(R.id.seek_power);
		mSeekPower.setMax(9);
		mSeekPower.setOnSeekBarChangeListener(this);

		/* Add Tx Duty UI control */
		mTxtDuty = findViewById(R.id.txt_duty);
		mSeekDuty = findViewById(R.id.seek_duty);
		mSeekDuty.setMax(4);
		mSeekDuty.setOnSeekBarChangeListener(this);

		/* Add Batter Gauge Monitor*/
		mBattPower = findViewById(R.id.batt_power);	// eric 2012.11.23
		mProgress = findViewById(R.id.batt_progress);	// eric 2012.11.23
		mProgress.setMax(100);	// eric 2012.11.23
		mProgress.setProgress(0);	// eric 2012.11.23

		// --
		setOnBtEventListener(this);

		mHandler.removeMessages(MSG_AUTO_REFRESH_DEVICE);
		mHandler.sendEmptyMessageDelayed(MSG_AUTO_REFRESH_DEVICE, 3000);

		// ---
		mSoundManager.initSounds(this);
		mSoundManager.addSound(0, R.raw.success);

		setBitsAndOffset("01", "7", false);
		resetMemData(true, false);
		mEdtPassword.setText("00000000");

		mChkAutoLink.setChecked(true);
		mChkSkipSame.setChecked(true);
		mChkDetectSound.setChecked(true);
		mChkContinuous.setChecked(true);

		// eric 2013.4.9 mSpinQuerySession.setSelection(0);
		mSpinQuerySession.setSelection(1); // eric 2013.4.9 because... multi-reading tag
		// eric 2013.4.9 mSpinTargetAB.setSelection(2);
		mSpinTargetAB.setSelection(0); // eric 2013.4.9 because... multi-reading tag
		mSpinQueryQ.setSelection(5);
		mEdtTimeout.setText("0");

		mSingleTag = mChkSingleTag.isChecked();
		mForceDisconnect = false;
		// setLinkStatus( false );
		setConnectionStatus(false);
	}

	public String getEndMsg( String strEnd )
	{
		if( strEnd == null )
			return null;

		if( strEnd.equals("-1") || strEnd.equals("0") )
			return null;// return "Stopped";
		else
			return "Operation Terminated by " + strEnd;
	}

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
		sendCmdStop();
		//mOp = false;	// eric 2012.11.23
		//R900Status.setOperationMode(0);	// eric 2012.11.29

		mInventLampTx.setLamp(LampView.LAMP_GRAY);
		mInventLampRx.setLamp(LampView.LAMP_GRAY);
		mAccessLampTx.setLamp(LampView.LAMP_GRAY);
		mAccessLampRx.setLamp(LampView.LAMP_GRAY);

		mBtnAccess.setText("Access");
		mBtnInventory.setText("INVENTORY");

		if( str.equalsIgnoreCase("link") )
		{
			mTabMode = TAB_LINK;
			// finish the battery Gauge Monitoring..
			mBattRun = false;
		}
		else if( str.equalsIgnoreCase("inventory") )
		{
			mTabMode = TAB_INVENTORY;
			// finish the battery Gauge Monitoring..
			mBattRun = false;
		}
		else if( str.equalsIgnoreCase("access") )
		{
			mTabMode = TAB_ACCESS;
			// finish the battery Gauge Monitoring..
			mBattRun = false;
		}
		else if( str.equalsIgnoreCase("config") )
		{
			mTabMode = TAB_CONFIG;
			// start the battery Gauge Monitoring..
			mBattRun = true;
			onBattGaugingStart();	// eric 2012.11.23
			Log.d(TAG, "================ onBattGaugingStart =========================");
		}
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

	protected boolean initBluetoothDeviceList(int id) {
		mListDevice = findViewById(id);
		if( mListDevice != null )
		{
			mArrBtDevice = new ArrayList<HashMap<String, String>>();
			mAdapterDevice = new SimpleAdapter(this, mArrBtDevice,
					android.R.layout.simple_list_item_2, new String[]
					{ "name", "summary", "address", "status" }, new int[]
					{ android.R.id.text1, android.R.id.text2 });
			mListDevice.setAdapter(mAdapterDevice);
			mListDevice.setOnItemClickListener(mDeviceClickListener);
		}
		return mListDevice != null;
	}

	protected boolean initTagList( int id )
	{
		mListTag = findViewById(id);
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
			HashMap<String, String> hashMap = (HashMap<String, String>) mListTag
					.getItemAtPosition(info.position);
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
			// sendCmdSelectMask( 0, 96, 1, 16, mSelTag, 4, 1 );
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
		// if( mAdapterTag != null )
		// mAdapterTag.notifyDataSetChanged();
	}

	public void onClick( View v )
	{
		switch( v.getId() )
		{
			// --- Link
			case R.id.btn_scan:
				int mode = R900Status.getInterfaceMode();
				if(mode == 1)
				{
					resetBtDeviceList();
					scanBluetoothDevice();
				}
				else
				{
					new AlertDialog.Builder(this)
					.setTitle("Error Message")
					.setMessage("Not select Bluetooth mode..")
					.setPositiveButton("OK",
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
			case R.id.btn_disconnect:
				// setEnabledBtnDisconnect(false);
				// setListStatus(null, "");
				mForceDisconnect = true;
				byeBluetoothDevice();
				setConnectionStatus(false);
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
												Button v = findViewById(R.id.btn_inventory);
												v.setText("STOP");
												sendCmdInventory();
												//mOp = true;	// eric 2012.11.23
												//R900Status.setOperationMode(1);	// eric 2012.11.29
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
												Button v = findViewById(R.id.btn_inventory);
												v.setText("STOP");
												sendCmdInventory();
												//mOp = true;	// eric 2012.11.23
												//R900Status.setOperationMode(1);	// eric 2012.11.29
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
						sendCmdInventory();
						//mOp = true;	// eric 2012.11.23
						//R900Status.setOperationMode(1);	// eric 2012.11.29
						mInventLampTx.setLamp(LampView.LAMP_RED);
					}
				}
				else
				{
					( (Button) v ).setText("INVENTORY");
					sendCmdStop();
					//mOp = false;	// eric 2012.11.23
					//R900Status.setOperationMode(0);	// eric 2012.11.29
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
					sendCmdStop();
					//mOp = false;	// eric 2012.11.23
					//R900Status.setOperationMode(0);	// eric 2012.11.29
					mAccessLampTx.setLamp(LampView.LAMP_GRAY);
				}

				break;
			}
			case R.id.radio_read:
				sendCmdStop();
				//mOp = false;	// eric 2012.11.23
				//R900Status.setOperationMode(0);	// eric 2012.11.29
				mInventLampTx.setLamp(LampView.LAMP_GRAY);
				resetAccessLamp();

				resetMemData(false, false);
				resetSpinEpc(false);
				mAccessType = ACC_TYPE_READ;
				showLayout(mLayoutReadWrite);
				break;
			case R.id.radio_write:
				sendCmdStop();
				//mOp = false;	// eric 2012.11.23
				//R900Status.setOperationMode(0);	// eric 2012.11.29
				mInventLampTx.setLamp(LampView.LAMP_GRAY);
				resetAccessLamp();

				resetMemData(false, true);
				resetSpinEpc(false);
				mAccessType = ACC_TYPE_WRITE;
				showLayout(mLayoutReadWrite);
				break;
			case R.id.radio_lock:
				sendCmdStop();
				//mOp = false;	// eric 2012.11.23
				//R900Status.setOperationMode(0);	// eric 2012.11.29
				mInventLampTx.setLamp(LampView.LAMP_GRAY);
				resetAccessLamp();

				resetLockUi(false);
				mAccessType = ACC_TYPE_LOCK;
				showLayout(mLayoutLock);
				break;
			case R.id.radio_kill:
				sendCmdStop();
				//mOp = false;	// eric 2012.11.23
				//R900Status.setOperationMode(0);	// eric 2012.11.29
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
				final LampView LAMP = findViewById(v.getId());

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
				mSingleTag = mChkSingleTag.isChecked();
				break;
		}
	}

	// --- SeekBar Listener
	public void onProgressChanged( SeekBar seekBar, int progress,
			boolean fromUser )
	{
		if( seekBar == mSeekPower )
		{
			mTxtPower.setText(TXT_POWER[ progress ]);
			sendSettingTxPower(TX_POWER[ progress ]);
		}

		if( seekBar == mSeekDuty )
		{
			mTxtDuty.setText(TXT_DUTY[ progress ]);
			sendSettingTxCycle(TX_DUTY_ON[ progress ],TX_DUTY_OFF[ progress ]);
			Log.d(TAG, "============= Duty Set : " + progress );
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

	private OnItemClickListener mDeviceClickListener = new OnItemClickListener()
	{
		public void onItemClick( AdapterView<?> av, View v, int arg2, long arg3 )
		{
			mR900Manager.stopDiscovery();

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
			mR900Manager.connectToBluetoothDevice(BT_ADDR, MY_UUID);
		}
	};

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

	private void resetBtDeviceList()
	{
		mBtnScan.setEnabled(false);
		mBtnDisconnect.setEnabled(false);
		mArrBtDevice.clear();
		mAdapterDevice.notifyDataSetChanged();
	}

	private void refreshBluetoothDevice()
	{
		Set<BluetoothDevice> setDevice = mR900Manager.queryPairedDevices();
		Log.d(TAG, "refreshBluetoothDevice : " + setDevice.size());
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
		sendInventParam(mSpinQuerySession.getSelectedItemPosition(),
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
					sendSetSelectAction(selMask.Bits, selMask.Bank,
							selMask.Offset, selMask.Pattern, 0);
					bUseMask = true;
				}
			}
			else
			{
				if( selMask.Bank != 4 )
				{
					sendSetSelectAction(selMask.Bits, selMask.Bank,
							selMask.Offset, selMask.Pattern, 0);
					bUseMask = true;
				}
			}
		}
		setOpMode(mChkSingleTag.isChecked(), bUseMask, timeout, bQuerySelected);
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
		lockPattern.indexKillPwd = ((index != 1) && (index == 2));

		index = mSpinAccessPassword.getSelectedItemPosition();
		if( index > 2 )
			lockPattern.lockPerma = true;
		lockPattern.enableAcsPwd = index != 0;
		lockPattern.indexAcsPwd = ((index != 1) && (index == 2));

		index = mSpinUiiMem.getSelectedItemPosition();
		if( index > 2 )
			lockPattern.lockPerma = true;
		lockPattern.enableUii = index != 0;
		lockPattern.indexUii = ((index != 1) && (index == 2));

		index = mSpinTidMem.getSelectedItemPosition();
		if( index > 2 )
			lockPattern.lockPerma = true;
		lockPattern.enableTid = index != 0;
		lockPattern.indexTid = ((index != 1) && (index == 2));

		index = mSpinUserMem.getSelectedItemPosition();
		if( index > 2 )
			lockPattern.lockPerma = true;
		lockPattern.enableUser = index != 0;
		lockPattern.indexUser = ((index != 1) && (index == 2));

		return lockPattern;
	}

	public void sendReadTag()
	{
		String strPwd = getPassword();// mEdtPassword.getText().toString();
		AccessAddress accAddr = getAccessAddress();
		sendReadTag(accAddr.len, accAddr.bank, accAddr.offset, strPwd);
	}

	public void sendWriteTag()
	{
		final String strPwd = getPassword();// mEdtPassword.getText().toString();
		final String strWordPattern = mEdtTagMemData.getText().toString();
		AccessAddress accAddr = getAccessAddress();
		if( strWordPattern != null
				&& ( strWordPattern.length() == ( accAddr.len * 4 ) ) )
			sendWriteTag(accAddr.len, accAddr.bank, accAddr.offset, strPwd,
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
		return strAuth != null && strAuth.equalsIgnoreCase("tagkiller");
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
		// if ( ( lockPattern.lockEnable & LOCK_PERMA ) != 0 &&
		// !CheckAuthority() )
		if( lockPattern.lockPerma && !CheckAuthority() )
		{
			Toast.makeText(
					this,
					"Skip writing permanent lock\nAuthorized personnel only\nPlease Test non permanent only",
					Toast.LENGTH_LONG).show();
			return;
		}
		// sendLockTag( lockPattern.lockMask, lockPattern.lockEnable, strPwd );
		sendLockTag(lockPattern, strPwd);
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

		sendKillTag(killPwd);
	}

	// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	// Bluetooth Event Listener
	public void onBtFoundNewDevice( BluetoothDevice device )
	{
		HashMap<String, String> item = new HashMap<String, String>();
		item.put("name", device.getName());
		if( mConnected == true &&
			mConnectedDevice != null &&
			mConnectedDevice.getAddress().equals( device.getAddress() ) )
			item.put("summary", device.getAddress() + " - Connected" );
		else
			item.put("summary", device.getAddress());
		item.put("address", device.getAddress());
		item.put("status", "");

		mArrBtDevice.add(item);
		mAdapterDevice.notifyDataSetChanged();
	}

	/**
	 * Esta função foi alterada para realizar uma conexão forçada com o Leitor DOTR-9000. Aprimorar em uma próxima release
	 */
	public void onBtScanCompleted()
	{
		Log.e("Teste de Conexão", "finalizada a busca");
		mBtnScan.setEnabled(true);

		if( mArrBtDevice.size() == 0 )
		{
			Set<BluetoothDevice> mPairedDevices = mR900Manager.queryPairedDevices();
			HashMap<String, String> item = new HashMap<String, String>();
			for (BluetoothDevice device : mPairedDevices){
				if(device.getName().equals("HQ_UHF_READER")){
					item.put("name", device.getName());
					item.put("address", device.getAddress());
				}
			}
			mArrBtDevice.add(item);
			mAdapterDevice.notifyDataSetChanged();
		}
		//mBtnDisconnect.setEnabled( mConnected );
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

	private void setListStatus( BluetoothDevice device, String msg )
	{
		final String ADDR = device != null ? device.getAddress() : null;
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
		setEnabledBtnDisconnect(bConnected);
		setLinkStatus(bConnected);

		if( bConnected == false )
		{
			mConnectedDevice = null;
			setListStatus(null, "");
		}
	}

	public void onBtConnected( BluetoothDevice device )
	{
		Log.d(TAG, "onBtConnected");

		setEnabledLinkCtrl(true);
		// setEnabledBtnDisconnect(true);
		// Toast.makeText( this, "Connect OK", Toast.LENGTH_SHORT );
		mConnectedDevice = device;
		setListStatus(device, "Connected");
		showToastByOtherThread("Connect OK", Toast.LENGTH_SHORT);

		// mPacketParser.reset();
		sendCmdOpenInterface1();

		setConnectionStatus(true);
		mForceDisconnect = false;

		// set the tx duty
		sendSettingTxCycle(TX_DUTY_ON[0], TX_DUTY_OFF[0]);
		mTxtDuty.setText(TXT_DUTY[0]);
	}

	//<-- eric 2012.11.23
	// Battery Level Monitoring routine
	public void onBattGaugingStart()
	{
		mBattflag = true;
		Log.d(TAG, "******* Race Start~!! onBattGaugingStart");

		new Thread()
		{
			public void run()
			{
				// Reader Operation Check!!!!!
				Log.d(TAG, "******* Battery Runner~~~!!!! onBattGaugingStart");
				while(mBattRun)
				//<-- eric 2013.4.9
				/*
				{
					//if(!mOp)
					// 1. get current battery level
					sendGettingBatteryLevel(0);

					// 2. check battery flag...
					if(mBattflag == true)
					{
						// send message handler
						Message msg = mBattHandler.obtainMessage();
						mBattHandler.removeMessages(MSG_BATTERY_CTRL);
						mBattHandler.sendEmptyMessage(MSG_BATTERY_CTRL);

						mBattflag = false;
					}
				}
				*/
				//--> eric 2013.4.9
				try{

					// 1. get current battery level
					sendGettingBatteryLevel(0);

					// 2. Check battery flag...
					if(mBattflag == true)
					{
						// send message handler
						Message msg = mBattHandler.obtainMessage();
						mBattHandler.removeMessages(MSG_BATTERY_CTRL);
						mBattHandler.sendEmptyMessage(MSG_BATTERY_CTRL);

						mBattflag = false;
					}

					// as if 1sec, check the battery level..
					Thread.sleep(2000);
				}
				catch (InterruptedException e){
					// exeption debugging routine
					e.printStackTrace();
				}
			}
		}.start();	// thread structure
	}
	//--> eric 2012.11.23

	public void onBtDisconnected( BluetoothDevice device )
	{
		Log.d(TAG, "onBtDisconnected");
		setEnabledLinkCtrl(true);
		// setEnabledBtnDisconnect(false);
		setListStatus(device, "Connection closed");
		showToastByOtherThread("Disconnected", Toast.LENGTH_SHORT);

		setConnectionStatus(false);
	}

	public void onBtConnectFail( BluetoothDevice device, String msg )
	{
		Log.d(TAG, "onBtConnectFail : " + msg);
		setEnabledLinkCtrl(true);
		setListStatus(device, "Connect Fail");
		// showToastByOtherThread("Connect Fail", Toast.LENGTH_SHORT);

		setConnectionStatus(false);
	}

	// public void onBtDataRecv( byte[] data, int len )
	public void onNotifyBtDataRecv()
	{
		if( mR900Manager == null )
			return;

		R900RecvPacketParser packetParser = mR900Manager.getRecvPacketParser();
		// ---
		while( true )
		{
			final String parameter = packetParser.popPacket();

			if( mConnected == false )
				break;

			if( parameter != null )
			{
				Log.d(TAG, "onBtDataRecv : [parameter = " + parameter + "]");
				processPacket(parameter);
			}
			else
				break;
		}

		// ---
		// mHandler.removeMessages(MSG_REFRESH_LIST_TAG);
		// if( mHandler.hasMessages( MSG_REFRESH_LIST_TAG ) == false )
		// mHandler.sendEmptyMessageDelayed(MSG_REFRESH_LIST_TAG, 10);
		mHandler.sendEmptyMessage(MSG_REFRESH_LIST_TAG);
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
					mToast = Toast.makeText(RFIDHostActivity.this,
							bundle.getString("msg"), Toast.LENGTH_LONG);
					mToast.show();
					break;
				}
			}
		}
	};

	private Toast mToast;
	private LogfileMng mLogMng = new LogfileMng();

	public void onBtDataSent( byte[] data )
	{
		mLogMng.write(data);
		/*
		 * char charBuff[] = new char[ data.length ]; for( int i = 0; i <
		 * data.length; ++i ) charBuff[ i ] = (char)( data[ i ] & 0xff );
		 *
		 * StringBuilder strBuilder = new StringBuilder(); strBuilder.append(
		 * charBuff );
		 *
		 * //------------- Message msg = new Message(); Bundle bundle = new
		 * Bundle(); bundle.putString( "msg", strBuilder.toString() ); msg.what
		 * = MSG_TOAST; msg.setData( bundle ); mHandlerToast.sendMessage(msg);
		 */
	}

	public void onBtDataTransException( BluetoothDevice device, String msg )
	{

	}

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

			//<-- eric 2012.11.23
			// get current battery level
			if((CMD.indexOf("ok,") != -1) && (!mBattflag))
			{
				final int idxComma = CMD.indexOf(",");
				final String mStrBattLevel = CMD.substring(idxComma + 1);

				/*
				Log.i(TAG, "12312931298389128398129389128391289381298392183918293");
				Log.i(TAG, "[[[[[[[[ RECV Monitor ]]]]]]]] idxComma : " + idxComma);
				Log.i(TAG, "[[[[[[[[ RECV Monitor ]]]]]]]] mStrBattLevel : " + mStrBattLevel);
				Log.i(TAG, "12312931298389128398129389128391289381298392183918293");
				*/

				// type(string to int)
				//mBattLevel = Integer.parseInt(mStrBattLevel);
				R900Status.setBatteryLevel(Integer.parseInt(mStrBattLevel));

				// set flag...
				mBattflag = true;
			}
			//--> eric 2012.11.23

			if( mConnected == false )
				return;

			if( CMD.indexOf("$trigger=1") == 0 )
			{
				if( mTabMode == TAB_INVENTORY )
				{
					mBtnInventory.setText("STOP");
					mBtnInventory.setEnabled(false);
					setupOperationParameter();
					sendCmdInventory();
					mInventLampTx.setLamp(LampView.LAMP_RED);
				}
				else if( mTabMode == TAB_ACCESS )
				{
					mBtnAccess.setText("STOP");
					mBtnAccess.setEnabled(false);
					setupOperationParameter();
					sendCmdAccess();
					mAccessLampTx.setLamp(LampView.LAMP_RED);
				}
			}
			else if( CMD.indexOf("$trigger=0") == 0 )
			{
				if( mTabMode == TAB_INVENTORY )
				{
					mBtnInventory.setText("INVENTORY");
					mBtnInventory.setEnabled(true);
					sendCmdStop();
					//mOp = false; // eric 2012.11.23
					mInventLampTx.setLamp(LampView.LAMP_GRAY);
				}
				else if( mTabMode == TAB_ACCESS )
				{
					mBtnAccess.setText("Access");
					mBtnAccess.setEnabled(true);
					sendCmdStop();
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
			if( mLastCmd == null )
				return;

			if( mTabMode == TAB_INVENTORY )
			{
				if( mLastCmd.equalsIgnoreCase(R900Protocol.CMD_INVENT) )
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
					final String strTagError = strTagId.substring(errTagPrefix
					);
					mStrAccessErrMsg = getTagErrorMsg(strTagError);
				}
				else
				{
					final int errOpPrefix = strTagId.indexOf("err_op=");
					if( errOpPrefix == 0 )
					{
						final String strOpError = strTagId.substring(
								errOpPrefix);
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
					final String strTagError = strTagId.substring(errTagPrefix
					);
					mStrAccessErrMsg = getTagErrorMsg(strTagError);
				}
				else
				{
					final int errOpPrefix = strTagId.indexOf("err_op=");
					if( errOpPrefix == 0 )
					{
						final String strOpError = strTagId.substring(
								errOpPrefix);
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
			// setAccessLampStatus( bStatusOk ? LampView.LAMP_GREEN :
			// LampView.LAMP_RED );

			if( bStatusOk == false )
			{
				final int errTagPrefix = strTagId.indexOf("err_tag=");
				if( errTagPrefix == 0 )
				{
					final String strTagError = strTagId.substring(8
					);
					mStrAccessErrMsg = getTagErrorMsg(strTagError);
				}
				else
				{
					final int errOpPrefix = strTagId.indexOf("err_op=");
					if( errOpPrefix == 0 )
					{
						final String strOpError = strTagId.substring(7
						);
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
				final String strTagError = strTagId.substring(8
				);
				mStrAccessErrMsg = getTagErrorMsg(strTagError);
				bStatusOk = false;
			}
			else
			{
				final int errOpPrefix = strTagId.indexOf("err_op=");
				if( errOpPrefix == 0 )
				{
					final String strOpError = strTagId.substring(7
					);
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
			// setAccessLampStatus( bStatusOk ? LampView.LAMP_GREEN :
			// LampView.LAMP_RED );
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
			// mBtnInventory.setText("INVENTORY");
			// mInventLampTx.setLamp( LampView.LAMP_GRAY );
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

		sendCmdStop();
		//mOp = false;	// eric 2012.11.23
		byeBluetoothDevice();

		mLampDetectKill.stopBlink();
		mLampDetectLock.stopBlink();
		mLampDetectReadWrite.stopBlink();
		mLampStatusKill.stopBlink();
		mLampStatusLock.stopBlink();
		mLampStatusReadWrite.stopBlink();
		mHandlerAutoLink.removeMessages(MSG_AUTO_LINK);

		if( mLogMng != null )
			mLogMng.finalize();

		super.finalize();
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

		RFIDHostActivity.this.postCloseApp();
		RFIDHostActivity.this.closeApp();
		RFIDHostActivity.this.finish();

	}
}