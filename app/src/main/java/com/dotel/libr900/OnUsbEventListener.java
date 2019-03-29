package com.dotel.libr900;

/***********************************************************************************
* OnUsbEventListener revision history                                              *
*************+*************+********+***********************************************
* 2012.12.12	ver 1.0.0  	  eric     1. Generated(First release)                 *
************************************************************************************/

import android.hardware.usb.UsbDevice;

public interface OnUsbEventListener
{
	//--- synch functions.
	abstract void onUsbConnected(UsbDevice device);
	abstract void onUsbDisconnected(UsbDevice device);
	abstract void onUsbConnectFail(UsbDevice device, String msg);	
	/*
    abstract void onBtFoundNewDevice( BluetoothDevice device );
    abstract void onBtScanCompleted();
    abstract void onBtConnected( BluetoothDevice device );
    abstract void onBtDisconnected( BluetoothDevice device );
    abstract void onBtConnectFail( BluetoothDevice device, String msg );
    abstract void onBtDataSent( byte[] data );
    abstract void onBtDataTransException( BluetoothDevice device, String msg );
    */
    //--- asynch function.
    abstract void onNotifyUsbDataRecv();
}