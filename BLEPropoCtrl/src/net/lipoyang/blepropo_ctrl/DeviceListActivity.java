/*
 * Copyright (C) 2014 Bizan Nishimura (@lipoyang)
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

// This file is diverted from a Android SDK sample project 'BluetoothChat'
// package com.example.android.BluetoothChat;
package net.lipoyang.blepropo_ctrl;

import net.lipoyang.blepropo_ctrl.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends Activity {
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRAS_DEVICE_ADDRESS = "device_address";
    public static String EXTRAS_DEVICE_NAME    = "device_name";
    
    // Bluetooth API
    private BluetoothAdapter mBluetoothAdapter;
    
    // for Device List
    private ArrayAdapter<String> mPairedDevicesArrayAdapter; // (not use now)
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    
    // Scan Timeout [msec]
    private static final long SCAN_TIMEOUT = 3000;
    // Scan Timeout handler
    private Handler mScanTimeoutHandler;
    // Scanning flag
    private boolean mScanning;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mScanTimeoutHandler = new Handler();
        
        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	scanLeDevice(true);
                //v.setVisibility(View.GONE);
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name); // (not use now)
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        // (not use now)
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Get the local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        // (not use now)
//      Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
//
//        // If there are paired devices, add each one to the ArrayAdapter
//        if (pairedDevices.size() > 0) {
//            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
//            for (BluetoothDevice device : pairedDevices) {
//                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//            }
//        } else {
//          String noDevices = getResources().getText(R.string.none_paired).toString();
//           mPairedDevicesArrayAdapter.add(noDevices);
//      }
    } // onCreate
    
    @Override
    protected void onResume() {
        super.onResume();

        // Initializes list view adapter.
        //mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        //ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        //newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        //newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        
        // start scanning
        scanLeDevice(true);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // stop scanning
        scanLeDevice(false);
        //mNewDevicesArrayAdapter.clear();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        scanLeDevice(false);
    }

    //Å@Scan BLE Device
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // set Callback for Timeout
            mScanTimeoutHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                	// Stop Scanning
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    // view
                    setProgressBarIndeterminateVisibility(false);
                    setTitle(R.string.select_device);
                    if (mNewDevicesArrayAdapter.getCount() == 0) {
                        String noDevices = getResources().getText(R.string.none_found).toString();
                        mNewDevicesArrayAdapter.add(noDevices);
                    }
                }
            }, SCAN_TIMEOUT);
            // clear the device list
            mNewDevicesArrayAdapter.clear();
            // view
            setProgressBarIndeterminateVisibility(true);
            setTitle(R.string.scanning);
            // Turn on sub-title for new devices
            //findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
            
            //Å@Start Scanning
            mScanning = true;
            // This should be startLeScan(UUID[],Callback) for detect only BLEPropo device.
            // in 2013.10, nevertheless, 
            // Android has a bug that 128bit long UUID device can not be detected.
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            if (mScanning) {
            	// Stop Scanning
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                // view
                setProgressBarIndeterminateVisibility(false);
            	setTitle(R.string.select_device);
            }
        }
    }
    
    // callback from Device Scan
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
        new BluetoothAdapter.LeScanCallback() {
            // When discovery finds a device
            @Override
            public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                      if(device.getName().equals("BLESerial")){               
                            // If it's already paired, skip it, because it's been listed already
                            //if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                                mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                                Log.d(TAG, "- onLeScan.run ADD!- Name is " + device.getName());
                                Log.d(TAG, "- onLeScan.run ADD!- Addr is " + device.toString() );
                            //}
//                      }
                    }
                });
        }
    };
    
    // on Click one of all Devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long arg3) {
            
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            String name = info.substring(0, info.length() - 18);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRAS_DEVICE_NAME, name);
            intent.putExtra(EXTRAS_DEVICE_ADDRESS, address);

            // Cancel discovery because it's costly and we're about to connect
            scanLeDevice(false);
            
            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };
}
