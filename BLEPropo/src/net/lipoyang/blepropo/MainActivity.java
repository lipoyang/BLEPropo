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

package net.lipoyang.blepropo;

import net.lipoyang.blepropo.R;
import net.lipoyang.blepropo_ctrl.BLEPropoCtrl;
import net.lipoyang.blepropo_ctrl.BLEPropoListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity  implements BLEPropoListener{

    // Debugging
    private static final String TAG = "BluePropo";
    private static final boolean DEBUGGING = true;
   
    // Propo View
    private PropoView propoView;
    // Bluetooth SPP module
    private BLEPropoCtrl blePropoCtrl;
    
    // Bluetooth state
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    private int btState = STATE_DISCONNECTED;
    
    private long lastUpdateTimeFB;
    private long lastUpdateTimeLR;
    
    //***** onCreate, onStart, onResume, onPause, onStop, onDestroy
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUGGING) Log.e(TAG, "++ ON CREATE ++");

        setContentView(R.layout.activity_main);
        propoView = (PropoView)findViewById(R.id.propoView1);
        propoView.setMainActivity(this);
       
        // create a BlueSerial.
        blePropoCtrl = new BLEPropoCtrl(this);
        
        // initialize variables
        lastUpdateTimeFB = 0;
        lastUpdateTimeLR = 0;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if(DEBUGGING) Log.e(TAG, "++ ON START ++");
        
        // start the BlueSerial, or finish this application.
        if( !blePropoCtrl.start()) {
        	finish();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(DEBUGGING) Log.e(TAG, "+ ON RESUME +");
        
        // resume the BlueSerial.
        blePropoCtrl.resume();
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();
        if(DEBUGGING) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(DEBUGGING) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        // stop the BlueSerial.
        blePropoCtrl.stop();
        
        super.onDestroy();
        if(DEBUGGING) Log.e(TAG, "--- ON DESTROY ---");
    }

    // onConneting, onConneted, onDisconneted  of BlueSerialLinster
    public void onConneting()
    {
    	btState = STATE_CONNECTING;
    	propoView.setBtState(btState);
    }
    public void onConneted(String devideName)
    {
    	btState = STATE_CONNECTED;
    	propoView.setBtState(btState);
    }
    public void onDisconneted()
    {
    	btState = STATE_DISCONNECTED;
    	propoView.setBtState(btState);
    }
    
    // On touch PropoView's Bluetooth Button
    public void onTouchBtButton()
    {
    	if(btState == STATE_DISCONNECTED){
	    	// open a Bluetooth device list.
	    	blePropoCtrl.openDeviceList();
    	}
    }
    
    // get the result from a Bluetooth device list activity launched by onTouchBtButton()
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(DEBUGGING) Log.d(TAG, "onActivityResult " + resultCode);
    	
    	// put the result to the BlueSerial.
    	// if failure, finish this application.
      	if( !blePropoCtrl.onActivityResult(requestCode, resultCode, data))
      	{
      		finish();
      	}
    }
    
    // On touch PropoView's FB Stick
    // fb = -1.0 ... +1.0
    public void onTouchFbStick(float fb)
    {
    	boolean update = false;
    	if(lastUpdateTimeFB + 50 < System.currentTimeMillis()) update = true;
    	if(fb == 0.0) update = true;
    	
    	// send the BlueSerial a message.
    	if (update){
    		blePropoCtrl.sendFB(fb);
    		lastUpdateTimeFB = System.currentTimeMillis();
    	}
    }
    
    // On touch PropoView's LR Stick
    // lr = -1.0 ... +1.0
    public void onTouchLrStick(float lr)
    {
    	boolean update = false;
    	if(lastUpdateTimeLR + 50 < System.currentTimeMillis()) update = true;
    	if(lr == 0.0) update = true;
    	
    	// send the BlueSerial a message.
    	if (update){
    		blePropoCtrl.sendLR(lr);
    		lastUpdateTimeLR = System.currentTimeMillis();
    	}
    }
    
}
