
package com.cthm.libr900;

/***********************************************************************************
* R900UsbManager revision history                                                  *
*************+*************+********+***********************************************
* 2012.12.12	ver 1.0.0  	  eric     1. Generated(First release)                 *
************************************************************************************/

import java.nio.ByteBuffer;
import java.util.HashMap;

import com.cthm.rfid.R900Status;
import com.cthm.rfid.RFIDUsbHostActivity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


public class R900UsbManager implements Runnable {
	private static final String TAG = "R900UsbManager";
	private static final String ACTION_USB_PERMISSION = "com.dotel.libr900.USB_PERMISSION";

	private UsbManager mUsbManager;
	private UsbDevice mDevice;
	private UsbDeviceConnection mConnection;
	private UsbEndpoint mEndpointIntr;
	private UsbEndpoint mEndpointIn;
	private UsbEndpoint mEndpointOut;
	private Update updateView;
		
	
	Thread thread = null;
	private Activity activity;
		
	public String mLastCmd;
	
	// -- set op mode
	protected boolean mSingleTag;
	protected boolean mUseMask;
	protected int mTimeout;
	protected boolean mQuerySelected;
	
	private OnUsbEventListener mUsbEventListener;
	
	public static final int MSG_USB_DATA_RECV = 10;
	
	private R900RecvPacketParser mPacketParser = new R900RecvPacketParser();
	
	/**
	 * handler
	 */
	private Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage( final Message msg )
		{
			switch( msg.what )
			{
				case MSG_USB_DATA_RECV:
					mUsbEventListener.onNotifyUsbDataRecv();
					break;				
			}
		}
	};
	
	/**
	 * Broadcast routine
	 */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() 
	{
		public void onReceive(Context context, Intent intent) 
		{
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) 
			{
				synchronized (this)
				{
					UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) 
					{
						setDevice(device);
					} 
					else 
					{
						Log.d(TAG, "permission denied for device " + device);
					}
				}
			} 
			else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) 
			{
				setDevice(mDevice);
			} 
			else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) 
			{
				disconnect();
				
				if(mUsbEventListener != null)
					  mUsbEventListener.onUsbDisconnected(mDevice);								
			}
		}		
	};
	
	/**
	 *  initialization routine
	 */
	public void init(Activity activity)
	{
		R900Status.setStop(false);
		
		mUsbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
		this.activity = activity;
			
		updateView = new Update();
		updateView.start();
		
		mPacketParser.reset();

		activity.registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
		activity.registerReceiver(mUsbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
		activity.registerReceiver(mUsbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
		
	}
	
	/**
	 * set onUsbEventListener
	 */
	public void setOnUsbEventListener(OnUsbEventListener listener)
	{
		mUsbEventListener = listener;			
	}
	
	/*
	 * get recv packet parser
	 */
	public final R900RecvPacketParser getRecvPacketParser()
	{
		return mPacketParser;
	}
	
	/**
	 *  connect
	 */
	public void connect()
	{
		PendingIntent mPermissionIntent = PendingIntent.getBroadcast(activity,0, new Intent(ACTION_USB_PERMISSION), 0);
		
		HashMap<String, UsbDevice> map = mUsbManager.getDeviceList();
		
		for (UsbDevice device : map.values() ) 
		{
			mDevice = device;
			mUsbManager.requestPermission(device, mPermissionIntent);
		}
	}
	
	/**
	 * disconnect
	 */
	public void disconnect()
	{
		if(thread != null)
		{
			thread.stop();
			thread = null;
		}		
				
		R900Status.setStop(true);
		
	}

	/**
	 *  Usb Interface
	 */
	UsbInterface getUsbInterface(UsbEndpoint endPoinst)
	{
		for(int i = 0;i < mDevice.getInterfaceCount();i++)
		{
			UsbInterface usbInterface = mDevice.getInterface(i);
			
			for(int j = 0;j < usbInterface.getEndpointCount();j++)
			{
				if(usbInterface.getEndpoint(j) == endPoinst)
				{
					return usbInterface;
				}
			}
		}
		return null;
	}

	/**
	 * send data
	 * @param data
	 * @byte String
	 */
	public void send(String data) 
	{
		Log.d(TAG,"send "+data);
		if(mConnection != null && mEndpointOut != null)
		{
			  //byte[] buffer = data.getBytes();
			  byte[] buffer = R900Protocol.string2bytes(data);
			  			  
			  if(mConnection.claimInterface(getUsbInterface(mEndpointOut), true))
			  {
			     mConnection.bulkTransfer(mEndpointOut, buffer, buffer.length, mEndpointOut.getInterval());			     
			  }
		}
	}
	
	/**
	 *  send data
	 *  @param data
	 *  @type byte[]
	 */
	public void sendData( byte[] bytes )
	{
		Log.d(TAG,"send bytes : " + bytes);
		if(mConnection != null && mEndpointOut != null)
		{
			  byte[] buffer = bytes;
			  			  
			  if(mConnection.claimInterface(getUsbInterface(mEndpointOut), true))
			  {
			     mConnection.bulkTransfer(mEndpointOut, buffer, buffer.length, mEndpointOut.getInterval());			     
			  }
		}
	}	

	/**
	 * receive data
	 */
	public int receiveData() 
	{
		if (mDevice != null && mEndpointIn != null && mConnection != null) 
		{
			synchronized (this) 
			{
				byte[] buffer = new byte[mEndpointIn.getMaxPacketSize()];
				
				if(mConnection.claimInterface(getUsbInterface(mEndpointIn), true))
				{
					int size = mConnection.bulkTransfer(mEndpointIn, buffer,buffer.length,mEndpointIn.getInterval());
					
					if (size > 0) 
					{
						String reciveData = new String(buffer, 0, size);
						Log.d("PIC recive data",reciveData);
						
						mPacketParser.pushPacket(buffer, size);						
						
						mHandler.sendEmptyMessage(MSG_USB_DATA_RECV);					
						
						return size;
					}
				}
			}
		}
		return 0;
	}
	
		
	// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< Send
		
	/**
	 * Open Interface
	 * @response <0d><0a>$>
	 */
	public void sendCmdOpenInterface1()
	{
		if(mConnection != null && mEndpointOut != null)
			sendData(R900Protocol.OPEN_INTERFACE_1);
	}
		
	/**
	 * Open Interface
	 * @response <0d>$>
	 */
	public void sendCmdOpenInterface2()
	{
		if(mConnection != null && mEndpointOut != null)
			sendData(R900Protocol.OPEN_INTERFACE_2);
	}
	
	/**
	 * Inventory	
	 */
	public void sendCmdInventory()
	{
		sendCmdInventory( mSingleTag ? 1 : 0, mUseMask ? ( mQuerySelected ? 3 : 2 ) : 0, mTimeout  );
	}
		
	/**
	 * Inventory command
	 * @param f_s : stop automatically after a tag is inventoried. (default = 0)
	 * @param f_m :  select mask. (default = 0)
	 *       0 or 1 - query all tags without select mask.
	 *       2 - query unselected tags by select mask.
	 *       3 - query selected tags by select mask.
	 * @param to : the operation timeout value in msec. (default = 0)
	 */
	public void sendCmdInventory( int f_s, int f_m, int to )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			R900Status.setOperationMode(1);	// eric 2012.11.29
			mLastCmd = R900Protocol.CMD_INVENT;
			sendData(R900Protocol.makeProtocol(mLastCmd, new int[]{ f_s, f_m, to }));
		}
	}
	
	/**
	 * Stop command
	 */
	public void sendCmdStop()
	{
		if(mConnection != null && mEndpointOut != null)
		{
			R900Status.setOperationMode(0);	// eric 2012.11.29
			mLastCmd = R900Protocol.CMD_STOP;
			sendData(R900Protocol.makeProtocol(mLastCmd, (int[])null));
		}
	}
	
	/**
	 * Heart beat command
	 * @param value
	 */
	public void sendHeartBeat( int value )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_HEART_BEAT, new int[]{ value }));
		}
	}
	
	/**
	 * Select mask command
	 * @param n : the index of mask table(0~7). (default = 0)
	 * @param bits : number of bits of the select mask pattern. (default = 0)
	 * @param mem : memory bank id of the tag to match for the select mask. (default = 0)
	 *       0 - RESERVED
	 *       1 - EPC
	 *       2 - TID
	 *       3 - USER
	 * @param b_offset : bit offset of the memory bank of the tag to match for the select mask. (default = 0)
	 * @param pattern : Bit pattern of the memory in the tag to match for the select mask. Must be HEXA_STRING, MSB is starting bit. (default = 0)
	 * @param target : Target flag in the tag will be altered after select command. (default = 4)
	 * @param action : Flag setting option. (default = 4)  
	 */
	public void sendCmdSelectMask( int n, int bits, int mem, int b_offset, String pattern, int target, int action )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_SEL_MASK, new int[]{ n, bits, mem, b_offset}, pattern, new int[]{ target, action } ));
		}
	}
		
	/**
	 * set session	
	 * @param session : session value for query command. (default = 0)
	 */
	public void sendSetSession( int session )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_INVENT_PARAM, new int[]{ session, R900Protocol.SKIP_PARAM, R900Protocol.SKIP_PARAM } ) );
		}
	}
		
	/**
	 * set Q value
	 * @param q : q value for query command. (default = 5)
	 */
	public void sendSetQValue( int q )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_INVENT_PARAM, new int[]{ R900Protocol.SKIP_PARAM, q, R900Protocol.SKIP_PARAM } ) );
		}
	}
		
	
	/**
	 * set Inventory Target
	 * @param m_ab : target value for query command. (default = 2)
	 */
	public void sendSetInventoryTarget( int m_ab )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_INVENT_PARAM, new int[]{ R900Protocol.SKIP_PARAM, R900Protocol.SKIP_PARAM, m_ab } ) );
		}
	}
		
	/**
	 * Inventory parameter
	 * @param session : session value for query command. (default = 0)
	 * @param q : q value for query command. (default = 5)
	 * @param m_ab : target value for query command. (default = 2)
	 */
	public void sendInventParam( int session, int q, int m_ab )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_INVENT_PARAM, new int[]{ session, q, m_ab } ) );
		}
	}
		
	/**
	 * Select mask command
	 * @param n : the index of mask table(0~7). (default = 0)
	 * @param bits : number of bits of the select mask pattern. (default = 0)
	 * @param mem : memory bank id of the tag to match for the select mask. (default = 0)
	 *       0 - RESERVED
	 *       1 - EPC
	 *       2 - TID
	 *       3 - USER
	 * @param b_offset : bit offset of the memory bank of the tag to match for the select mask. (default = 0)
	 * @param pattern : Bit pattern of the memory in the tag to match for the select mask. Must be HEXA_STRING, MSB is starting bit. (default = 0)
	 * @param target : Target flag in the tag will be altered after select command. (default = 4)
	 * @param action : Flag setting option. (default = 4)  
	 */
	public void sendSetSelectAction( int bits, int mem, int b_offset, String pattern, int action )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_SEL_MASK, new int[]{ 0, bits, mem, b_offset}, pattern, new int[]{ R900Protocol.SKIP_PARAM, action } ));
		}
	}
		
	/**
	 * set operation mode
	 * @param singleTag : stop automatically after a tag is inventoried.
	 * @param useMask : query select tags by select mask.
	 * @param timeout : the operation time.
	 * @param querySelected : query select info.
	 */
	public void setOpMode( boolean singleTag, boolean useMask, int timeout, boolean querySelected  )
	{			
			mSingleTag = singleTag;
			mUseMask = useMask;
			mTimeout = timeout;
			mQuerySelected = querySelected;						
	}
		
	/**
	 * access operation - (read)
	 * @param w_count : number of words(16bits) to read(1 to 255).
	 * @param mem : memory bank address to read.
	 *        RESERVED is 0
	 *        EPC is 1
	 *        TID is 2
	 *        USER is 3
	 * @param w_offset : word offset of the memory bank to read.
	 * @param ACS_PWD : access password of the tag.
	 */
	public void sendReadTag( int w_count, int mem, int w_offset, String ACS_PWD )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_READ_TAG_MEM, 
				new int[]{ w_count, mem, w_offset }, 
				ACS_PWD, 
				new int[]{ mSingleTag ? 1 : 0, mUseMask ? ( mQuerySelected ? 3 : 2 ) : 0, mTimeout } ) );
		}
	}
		
	/**
	 * access operation(write)
	 * @param w_count
	 * @param mem
	 * @param w_offset
	 * @param ACS_PWD
	 * @param wordPattern
	 */
	public void sendWriteTag( int w_count, int mem, int w_offset, String ACS_PWD, String wordPattern )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_WRITE_TAG_MEM,
				new int[]{ w_count, mem, w_offset }, 
				new String[]{ wordPattern, ACS_PWD }, 
				new int[]{ mSingleTag ? 1 : 0, mUseMask ? ( mQuerySelected ? 3 : 2 ) : 0, mTimeout } ) );
		}
	}
		
	/**
	 * convert lock index
	 * @param enable : select to lock enable or disable.
	 * @param index : index of memory bank
	 * @return lock enable or disable
	 */
	private int convertLockIndex( boolean enable, boolean index )
	{
		return enable ? ( index ? 1 : 0 ) : -1;
	}
		
	/**
	 * lock operation	
	 * @param lockPattern : lock pattern 
	 * @param ACS_PWD : access password
	 */
	public void sendLockTag(RFIDUsbHostActivity.LockPattern lockPattern, String ACS_PWD )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			final int user = convertLockIndex( lockPattern.enableUser, lockPattern.indexUser );//( ( lockPattern.indexUser == false ) ? ( R900Protocol.SKIP_PARAM ) : ( lockPattern.enableUser ? 1 : 0 ) );
			final int tid = convertLockIndex( lockPattern.enableTid, lockPattern.indexTid );//( ( lockPattern.indexTid == false ) ? ( R900Protocol.SKIP_PARAM ) : ( lockPattern.enableTid ? 1 : 0 ) );
			final int epc = convertLockIndex( lockPattern.enableUii, lockPattern.indexUii );//( ( lockPattern.indexUii == false ) ? ( R900Protocol.SKIP_PARAM ) : ( lockPattern.enableUii ? 1 : 0 ) );
			final int acs_pwd = convertLockIndex( lockPattern.enableAcsPwd, lockPattern.indexAcsPwd );//( ( lockPattern.indexAcsPwd == false ) ? ( R900Protocol.SKIP_PARAM ) : ( lockPattern.enableAcsPwd ? 1 : 0 ) );
			final int kill_pwd = convertLockIndex( lockPattern.enableKillPwd, lockPattern.indexKillPwd );//( ( lockPattern.indexKillPwd == false ) ? ( R900Protocol.SKIP_PARAM ) : ( lockPattern.enableKillPwd ? 1 : 0 ) );
				
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_LOCK_TAG_MEM,
					new int[]{ user, tid, epc, acs_pwd, kill_pwd },
					ACS_PWD,
					new int[]{ mSingleTag ? 1 : 0, mUseMask ? ( mQuerySelected ? 3 : 2 ) : 0, mTimeout } ) );
		}
	}
	
	/**
	 * tag lock operation
	 * @param lockMask : lock mask
	 * @param lockEnable : lock enable or disable
	 * @param ACS_PWD : access password
	 */
	public void sendLockTag( int lockMask, int lockEnable, String ACS_PWD )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			int bitFlag;
			boolean mask;
			boolean enable;
				
			//---
			bitFlag = 0x200;//0x02;
			mask = ( lockMask & bitFlag ) == bitFlag;
			enable = ( lockEnable & bitFlag ) == bitFlag;
			final int user = ( ( mask == false ) ? ( R900Protocol.SKIP_PARAM ) : ( enable ? 1 : 0 ) );
				
			//---
			bitFlag = 0x80;//0x08;
			mask = ( lockMask & bitFlag ) == bitFlag;
			enable = ( lockEnable & bitFlag ) == bitFlag;
			final int tid = ( ( mask == false ) ? ( R900Protocol.SKIP_PARAM ) : ( enable ? 1 : 0 ) );
				
			//---
			bitFlag = 0x20;
			mask = ( lockMask & bitFlag ) == bitFlag;
			enable = ( lockEnable & bitFlag ) == bitFlag;
			final int epc = ( ( mask == false ) ? ( R900Protocol.SKIP_PARAM ) : ( enable ? 1 : 0 ) );
			
			//---
			bitFlag = 0x08;//0x80;
			mask = ( lockMask & bitFlag ) == bitFlag;
			enable = ( lockEnable & bitFlag ) == bitFlag;
			final int acs_pwd = ( ( mask == false ) ? ( R900Protocol.SKIP_PARAM ) : ( enable ? 1 : 0 ) );
			
			//---
			bitFlag = 0x02;//0x200;
			mask = ( lockMask & bitFlag ) == bitFlag;
			enable = ( lockEnable & bitFlag ) == bitFlag;
			final int kill_pwd = ( ( mask == false ) ? ( R900Protocol.SKIP_PARAM ) : ( enable ? 1 : 0 ) );
			
			//---
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_LOCK_TAG_MEM,
					new int[]{ user, tid, epc, acs_pwd, kill_pwd },
					ACS_PWD,
					new int[]{ mSingleTag ? 1 : 0, mUseMask ? ( mQuerySelected ? 3 : 2 ) : 0, mTimeout } ) );
		}
	}
		
	/**
	 * tag kill operation
	 * @param killPwd : kill password
	 */
	public void sendKillTag( String killPwd )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_KILL_TAG,
					killPwd,
					new int[]{ mSingleTag ? 1 : 0, mUseMask ? ( mQuerySelected ? 3 : 2 ) : 0, mTimeout } ) );
		}
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------------
	//---- other command
	//-----------------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------------
	/**
	 * get version
	 */
	public void sendGetVersion()
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_GET_VERSION ) );
		}
	}
	
	/**
	 * set default parameter
	 */
	public void sendSetDefaultParameter()
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_SET_DEF_PARAM ) );
		}
	}
		
	/**
	 * send getting parameter
	 */
	public void sendGettingParameter( String cmd, String p )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_GET_PARAM, new String[]{ cmd, p } ) );
		}
	}
		
	/**
	 * set tx power
	 * @param a : attenuation from the max power in 1dB(0 ~ 300).
	 */
	public void sendSettingTxPower( int a )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_SET_TX_POWER, new int[]{ a } ) );
		}
	}
		
	/**
	 * get max power
	 */
	public void sendGetMaxPower()
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_GET_MAX_POWER ) );
		}
	}
		
	/**
	 * set tx cycle
	 * @param on : Transmission interval in msec.
	 * @param off : Wait interval in msec.
	 */
	public void sendSettingTxCycle( int on, int off)
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_SET_TX_CYCLE, new int[]{ on, off } ) );
		}
	}
		
	/**
	 * change channel state
	 * @param n : channel number ( 1~ )
	 * @param f_e : select use or not to use
	 *           0 is not to use.
	 *           1 is use.
	 */
	public void sendChangeChannelState( int n, int f_e )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_CHANGE_CH_STATE, new int[]{ n, f_e } ) );
		}
	}
		
	/**
	 * setting country
	 * @param code : id code for a region to work.
	 */
	public void sendSettingCountry( int code )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_CHANGE_CH_STATE, new int[]{ code } ) );
		}
	}
		
	/**
	 * get country code
	 */
	public void sendGettingCountry()
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_GET_COUNTRY_CAP ) );
		}
	}
		
	/**
	 * set lock tag memory state permanently
	 * @param mem_id : ID of memory bank
	 * @param f_l : lock state to fix.
	 * @param ACS_PWD : access password.
	 */
	public void sendSetLockTagMemStatePerm( int mem_id, int f_l, String ACS_PWD )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_SET_LOCK_TAG_MEM,
					new int[]{ mem_id, f_l },
					ACS_PWD,
					new int[]{ mSingleTag ? 1 : 0, mUseMask ? ( mQuerySelected ? 3 : 2 ) : 0, mTimeout } ) );
		}
	}
		
	
	/**
	 * Pause tx 
	 */
	public void sendPauseTx()
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_PAUSE_TX ) );
		}
	}
		
	/**
	 * status reporting
	 * @param f_link : 
	 */
	public void sendStatusReporting( int f_link )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_STATUS_REPORT,
						new int[]{ f_link } ) );
		}
	}
	
	/**
	 * Inventory Reporting Format
	 * @param f_time : set to 1 to get inventoried time.         
	 * @param f_rssi : set to 1 to get rssi of the tag response.
	 */
	public void sendInventoryReportingFormat( int f_time, int f_rssi )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_INVENT_REPORT_FORMAT, 
					new int[]{ f_time, f_rssi } ) );
		}
	}
		
	/**
	 * close connection
	 */
	public void sendDislink()
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_DISLINK ) );
		}
	}
		
	
	//-----------------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------------
	//---- R900 Controls
	//-----------------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------------
	/**
	 * Uploading Tag Data
	 * @param index : first data to upload(0~ ).
	 * @param count : number of tag information.
	 */
	public void sendUploadingTagData( int index, int count )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_UPLOAD_TAG_DATA,
					new int[]{ index, count } ) );
		}
	}
	
	/**
	 *   Clear tag data
	 */
	public void sendClearingTagData()
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_CLEAR_TAG_DATA ) );
		}
	}
		
	/**
	 * Alert Reader Status
	 * @param f_link : set to 1 to be alerted, when link state changed.
	 * @param f_trigger : set to 1 to be alerted, when trigger state changed.
	 * @param f_lowbat : set to 1 to be alerted, when battery level is low.
	 * @param f_autooff : set to 1 to be alerted, when reader is going to be off.
	 * @param f_pwr : set to 1 to be alerted, when reader is going ot be off.
	 */
	public void sendAlertReaderStatus( int f_link, int f_trigger, int f_lowbat, int f_autooff, int f_pwr )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_ALERT_READER_STATUS,
				new int[]{ f_link, f_trigger, f_lowbat, f_autooff, f_pwr } ) );
		}
	}
		
	/**
	 * get status word
	 */
	public void sendGettingStatusWord()
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_GET_STATUS_WORD ) );
		}
	}
		
	/**
	 * set buzzer volume
	 * @param volume : volume to set(0 ~ 2). 
	 * @param f_nv : set to 1 to keep change after power cycle.
	 */
	public void sendSettingBuzzerVolume( int volume, int f_nv )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_SET_BUZZER_VOL,
				new int[]{ volume, f_nv } ) );
		}
	}
			
	/**
	 * set control buzzer
	 * @param f_on : buzzer control.
	 *       0 is start to buzzing.
	 *       1 is stop to buzzing.
	 */
	public void sendBeep( int f_on )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_BEEP,
				new int[]{ f_on } ) );
		}
	}
	
	/**
	 * set auto power off delay
	 * @param delay : delay interval in sec.
	 * @param f_nv : set to 1 to keep change after power cycle.      
	 */
	public void sendSettingAutoPowerOffDelay( int delay, int f_nv )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_SET_AUTO_POWER_OFF_DELAY,
				new int[]{ delay, f_nv } ) );
		}
	}
		
	/**
	 * get bettery level
	 * @param f_ext : set to 1 to get extended information.
	 */
	public void sendGettingBatteryLevel( int f_ext )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_GET_BATT_LEVEL,
				new int[]{ f_ext } ) );
		}
	}
		
	/**
	 * set reporting battery state
	 * @param f_report : set to 1 to be reported when state changed.
	 */
	public void sendReportingBatteryState( int f_report )
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_REPORT_BATT_STATE,
					new int[]{ f_report } ) );
		}
	}
	
	/**
	 * reader off
	 */
	public void sendTurningReaderOff()
	{
		if(mConnection != null && mEndpointOut != null)
		{
			sendData(R900Protocol.makeProtocol( R900Protocol.CMD_TURN_READER_OFF ) );
		}
	}
	

	/**
	 *  run
	 */
	@Override
	public void run() 
	{
		UsbRequest request = new UsbRequest();
		request.initialize(mConnection, mEndpointIntr);
		byte status = -1;
		ByteBuffer buffer = ByteBuffer.allocate(mEndpointIntr.getMaxPacketSize());
		
		//while (true)
		for(;;)
		{
			request.queue(buffer, mEndpointIntr.getMaxPacketSize());		
		
			if (mConnection != null) 
			{
				if (mConnection.requestWait() == request)
				{
					byte newStatus = buffer.get(0);
					
					if(newStatus < 0)
					{
						mConnection.close();
					}
					request.getClientData();
					
					if (newStatus != status) 
					{
						Log.d(TAG, "got status " + newStatus);
						status = newStatus;
					}				
				}				
			}			
			else
			{
				  try 
				  {
					  Thread.sleep(1000);
				  } 
				  catch (InterruptedException e) 
				  {
					  if(mUsbEventListener != null)
						  mUsbEventListener.onUsbDisconnected(mDevice);
				  }
			}					
		}
	}

	/**
	 * Update
	 * @author dime
	 *
	 */
	class Update extends Thread 
	{
		public void run() 
		{			
			for(;;)
			{		
				if(R900Status.getStop())
					return;
				
				receiveData();				
			
				try 
				{
					Thread.sleep(100);					
				}
				catch (InterruptedException e) 
				{
					if(mUsbEventListener != null)
						  mUsbEventListener.onUsbDisconnected(mDevice);
				}
								
			}
		}
	}
	
	/**
	 *  close
	 */
	void close() 
	{
		if (mConnection != null) 
		{			
			mConnection.close();			
			mConnection = null;
		}		
	}

	/**
	 * Set device
	 * @param device
	 */
	private void setDevice(UsbDevice device) 
	{
		if (device == null) 
		{
			mConnection = null;
			return;
		}
		
		switch(device.getDeviceClass())
		{
			case UsbConstants.USB_CLASS_APP_SPEC:
			case UsbConstants.USB_CLASS_AUDIO:
			case UsbConstants.USB_CLASS_CDC_DATA:
			case UsbConstants.USB_CLASS_COMM:
			case UsbConstants.USB_CLASS_CONTENT_SEC:
			case UsbConstants.USB_CLASS_CSCID:
			case UsbConstants.USB_CLASS_HID:
			case UsbConstants.USB_CLASS_HUB:
			case UsbConstants.USB_CLASS_MASS_STORAGE:
			case UsbConstants.USB_CLASS_MISC:
			case UsbConstants.USB_CLASS_PER_INTERFACE:
			case UsbConstants.USB_CLASS_PHYSICA:
			case UsbConstants.USB_CLASS_PRINTER:
			case UsbConstants.USB_CLASS_STILL_IMAGE:
			case UsbConstants.USB_CLASS_VENDOR_SPEC:
			case UsbConstants.USB_CLASS_VIDEO:
			case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER:
		}
		
		mDevice = device;
		
		if (device != null) 
		{
			UsbDeviceConnection connection = mUsbManager.openDevice(device);
			if (connection != null) // && connection.claimInterface(intf, true)) {
			{
				Log.d(TAG, "open SUCCESS");
						
				mConnection = connection;					
				
				for(int i = 0;i < device.getInterfaceCount();i++) 
				{
					UsbInterface ui = device.getInterface(i);
					for(int j = 0;j < ui.getEndpointCount();j++)
					{
						UsbEndpoint endPoint = ui.getEndpoint(j);
						
						switch(endPoint.getType())
						{
					     	case UsbConstants.USB_ENDPOINT_XFER_BULK:
					     		if(endPoint.getDirection() == UsbConstants.USB_DIR_IN)
					     		{
					     			mEndpointIn =endPoint;
					     		}
					     		else 
					     		{
					     			mEndpointOut = endPoint;
					     			
					     			if(mUsbEventListener != null)
									{
										mUsbEventListener.onUsbConnected(mDevice);
									}
					     		}
					     		break;
					     	
					     	case UsbConstants.USB_ENDPOINT_XFER_CONTROL:
					     		break;
					     		
					     	case UsbConstants.USB_ENDPOINT_XFER_INT:
					     		mEndpointIntr = endPoint;
					     		
					     		if (connection.claimInterface(ui, true)) 
					     		{
					     			if(thread == null)
					     			{
					     				thread = new Thread(this);
					     				thread.start();				    				
					     			}
					     		}
					     		break;
					     		
					     	case UsbConstants.USB_ENDPOINT_XFER_ISOC:
					     		break;
						}
					}
				}
			} 
			else 
			{
				Log.d(TAG, "open FAIL");
				
				thread = null;
				
				if(mUsbEventListener != null)
					mUsbEventListener.onUsbConnectFail(mDevice,"Connceted failed....");
				
				close();
			}
		}
	}	
}
