/*
 * Copyright (C) 2014 Bizan Nishimura (@lipoyang)
 * 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lipoyang.blepropo_ctrl;

import net.lipoyang.blepropo_ctrl.BLEPropoService;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class BLEPropoCtrl {

    // Debugging
    private static final String TAG = "BLEPropoCtrl";
    
    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 2;
    
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter;
    
    // BLEPropo Service
    private BLEPropoService mBLEPropoService = null;
    // BLE Device Name & MAC Address
    private String mDeviceName = null;
    private String mDeviceAddress = null;
    // TX data buffer
    byte[] TxData = new byte[2];
    // flag of connection
    private boolean isConnected;
    
    // parent activity
    private Activity parent;
    // event listener
    private BLEPropoListener listener;
    
    // constructor
    public BLEPropoCtrl(Activity p)
    {
        parent = p;
        listener = (BLEPropoListener)p;
        
        // initialize variables
        for(int i=0;i<2;i++){TxData[i]=0;}
        isConnected = false;
    }
    
    // API: start (called from onStart() of the Activity)
    public boolean start()
    {
        Log.d(TAG, "start" );
        
        // Check whether BLE is available or not
        if (!parent.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(parent, "Bluetooth LE is not available", Toast.LENGTH_LONG).show();
            return false;
        }
        // Initialize Bluetooth adapter (API 18 or more)
        final BluetoothManager bluetoothManager =
                (BluetoothManager) parent.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Check whether Bluetooth is available or not
        if (mBluetoothAdapter == null) {
            Toast.makeText(parent, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // bind BLEPropo service
        //Intent gattServiceIntent = new Intent(parent, BLEPropoService.class);
        //parent.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        
        return true;
    }
    
    // API: resume (called from onResume() of the Activity)
    public void resume()
    {
        Log.d(TAG, "resume" );
        
        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            parent.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        
        // bind BLEPropo service
        // TODO: Should it be called from onCreate()?
        Intent gattServiceIntent = new Intent(parent, BLEPropoService.class);
        parent.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        
        // register BLE event receiver, and connect to BLE device
        parent.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBLEPropoService != null) {
            final boolean result = mBLEPropoService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }else{
            Log.e(TAG, "mBLEPropoService is NULL! ");
            // TODO: What can I do?
        }
    }
    
    // API: stop (called from onDestroy() of the Activity)
    // TODO: Should it be called from onPause()?
    public void stop()
    {
        parent.unregisterReceiver(mGattUpdateReceiver);
        parent.unbindService(mServiceConnection);
        mBLEPropoService = null;
    }

    // API: open a Bluetooth device list (DeviceListActivity)
    public void openDeviceList()
    {
        Intent serverIntent = new Intent(parent, DeviceListActivity.class);
        parent.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }
    
    // on Activity Result (when back to this activity)
    // (a) get the result whether Bluetooth gets Enable or Not
    // (b) get the result of BLE Device selection (DeviceListActivity)
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean ret = true;
    
        switch (requestCode) {
        // (a) Bluetooth gets Enable or Not?
        case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
                //initCommunication();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(parent, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                
                ret = false;
            }
            break;
        // (b) BLE Device selection
        case REQUEST_CONNECT_DEVICE:
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                mDeviceName = data.getExtras()
                        .getString(DeviceListActivity.EXTRAS_DEVICE_NAME);
                mDeviceAddress = data.getExtras()
                        .getString(DeviceListActivity.EXTRAS_DEVICE_ADDRESS);
                
                Log.d(TAG, "NAME is [" + mDeviceName +"]");
                Log.d(TAG, "ADDR is [" + mDeviceAddress +"]");
            }
            break;
        }
        return ret;
    }
    
    // Service Connection ( with BLEPropo service)
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEPropoService = ((BLEPropoService.LocalBinder) service).getService();
            if (!mBLEPropoService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                // TODO finish();
            }else{
                // Connect to the BLE Device!
                mBLEPropoService.connect(mDeviceAddress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEPropoService = null;
        }
    };
    
    // BLE Event Receiver
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            
            // on Connected to GATT service
            if (BLEPropoService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_CONNECTED");
                listener.onConneted(mDeviceName);
                isConnected = true;
            }
            // on Disconnedted from GATT service
            else if (BLEPropoService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_DISCONNECTED");
                listener.onDisconneted();
                isConnected = false;
            } 
            // on Discoverd GATT services
            else if (BLEPropoService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
            } 
            // on Received Data
            else if (BLEPropoService.ACTION_DATA_AVAILABLE.equals(action)) {
                // get bytes data
                byte[] rsvData = intent.getByteArrayExtra(BLEPropoService.EXTRA_DATA);
                // TODO (Nothing to do with received data)
            }
        }
    };
    
    // mae GATT Intent Filter
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEPropoService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEPropoService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEPropoService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEPropoService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    
    /*
     * APIs to send data
     */
    
    // API: send F<->B data
    public void sendFB(float fb)
    {
        if(isConnected){
            int iFB = (int)(fb * 128.0);
            if(iFB<-128) iFB = -128;
            if(iFB>127) iFB = 127;
            
            TxData[0]=(byte)iFB;
            mBLEPropoService.sendData(TxData);
        }
    }
    
    // API: send L<->R data
    public void sendLR(float lr)
    {
        if(isConnected){
            int iLR = (int)(lr * 128.0);
            if(iLR<-128) iLR = -128;
            if(iLR>127) iLR = 127;
            
            TxData[1]=(byte)iLR;
            mBLEPropoService.sendData(TxData);
        }
    }
}
