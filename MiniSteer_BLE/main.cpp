/*
 * This repo was forked from jksoft/BLE_RCBController2.
 * The copyrights of the original repo belongs to Junichi Katsu.
 */

/*
 * Copyright (C) 2014 Bizan Nishimura (@lipoyang)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Library Dependencies:
 * - BLE_API (revision 111)
 * - HRM1017
 *   - HRM1017/projectconfig.h
 *     connection interval constants are modified. (No need?)
 *     CFG_GAP_CONNECTION_MIN_INTERVAL_MS 30 
 *     CFG_GAP_CONNECTION_MAX_INTERVAL_MS 50
 */

#include "mbed.h"
#include "BLEDevice.h"

#define DBG 1

// BLE device object
BLEDevice  ble;

// BluePropo service UUID
//static const uint16_t UUID_BLUEPROPO = 0xFFF0;
static const uint8_t UUID_BLUEPROPO[] = 
{ 0xc4, 0x9d, 0xfd, 0x1b, 0x86, 0x04, 0x41, 0xd2, 0x89, 0x43, 0x13, 0x6f, 0x21, 0x4d, 0xd0, 0xbf };

// BluePropo::Stick characteristic UUID
//static const uint16_t UUID_BLUEPROPO_STICK = 0xFFF1;
static const uint8_t UUID_BLUEPROPO_STICK[] =
{ 0x74, 0x25, 0xfb, 0xa0, 0x72, 0x15, 0x41, 0x36, 0xaa, 0x3f, 0x07, 0x2a, 0xa0, 0x7d, 0x93, 0x54 };

// Device Name (for display)
#define DEVICE_NAME "MiniSteer HRM1017"

// BluePropo::Stick data structure
union StickData
{
	struct  {
		// F(-128)<- 0 ->B(+127)
		signed char fb;
		// L(-128)<- 0 ->R(+127)
		signed char lr;
	}value;
	unsigned char bytes[2];
};
StickData stickData;

// buffer for BluePropo payload
uint8_t payload[10] = {0,};

// BluePropo::Stick characteristic
GattCharacteristic  charStick (UUID_BLUEPROPO_STICK, payload, 2, 2,
								GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_WRITE | 
								GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_WRITE_WITHOUT_RESPONSE);
// BluePropo characteristics set
GattCharacteristic *chars[] = {&charStick};
// BluePropo service
GattService         serviceBluePropo(UUID_BLUEPROPO, chars, sizeof(chars) / sizeof(GattCharacteristic *));

// USB COM port for Debug
Serial  pc(USBTX, USBRX);

// pin asign
DigitalOut tb6612_ain1(P0_28);
DigitalOut tb6612_ain2(P0_29);
PwmOut     tb6612_pwma(P0_30);
PwmOut     servo_pwm  (P0_12);

// DC motor driver (TB6612)
void motor (float speed)
{
    if (speed > 0) {
        // CW
        tb6612_pwma = speed;
        tb6612_ain1 = 1;
        tb6612_ain2 = 0;
    } else
    if (speed < 0) {
        // CCW
        tb6612_pwma = - speed;
        tb6612_ain1 = 0;
        tb6612_ain2 = 1;
    } else {
        // stop
        tb6612_pwma = 1;
        tb6612_ain1 = 0;
        tb6612_ain2 = 0;
//        // break
//        tb6612_pwma = 1;
//        tb6612_ain1 = 1;
//        tb6612_ain2 = 1;
    }
}

// RC servo
void servo (float deg)
{
    servo_pwm.pulsewidth_us(1500 + (int)(500.0 * deg));
}

// BLE onConnection handler
void onConnected(uint16_t h)
{
#if DBG
	pc.printf("Connected\n\r");
#endif
}

// BLE onDisconnection handler
void onDisconnected(uint16_t h)
{
    ble.startAdvertising();
#if DBG
	pc.printf("Disconnected\n\r");
#endif
}

// BLE onDataWritten handler (Gatt event)
void onDataWritten(uint16_t charHandle)
{
	if (charHandle == charStick.getHandle()) {
		uint16_t bytesRead;
	 	ble.readCharacteristicValue(charStick.getHandle(),payload, &bytesRead);
        memcpy( &stickData.bytes[0], payload, sizeof(stickData));
#if DBG

		pc.printf("DATA:%02X %02X\n\r",stickData.bytes[0],stickData.bytes[1]);
#endif
 		float m = (float)stickData.value.fb / 128.0;
        motor(m);
        float s = (float)stickData.value.lr / 128.0;
        servo(s);
	}
}

// Program entry point
int main(void)
{
#if DBG
		pc.printf("Start\n\r");
#endif
	// initialize servo & motor
    servo_pwm.period_ms(20);
    servo(0.5);
    motor(0);
	
	// initialize BLE
    ble.init(); 
    ble.onConnection(onConnected);
    ble.onDisconnection(onDisconnected);
    ble.onDataWritten(onDataWritten);
    // setup advertising
    ble.accumulateAdvertisingPayload(GapAdvertisingData::BREDR_NOT_SUPPORTED);
    ble.setAdvertisingType(GapAdvertisingParams::ADV_CONNECTABLE_UNDIRECTED);
    ble.accumulateAdvertisingPayload(GapAdvertisingData::SHORTENED_LOCAL_NAME,
                                    (const uint8_t *)DEVICE_NAME, sizeof(DEVICE_NAME) - 1);
    ble.accumulateAdvertisingPayload(GapAdvertisingData::COMPLETE_LIST_16BIT_SERVICE_IDS,
                                    (const uint8_t *)UUID_BLUEPROPO, sizeof(UUID_BLUEPROPO));
    ble.setAdvertisingInterval(160); /* 100ms; in multiples of 0.625ms. */
    ble.startAdvertising();
    ble.addService(serviceBluePropo);

	// main loop (wait for BLE event)
    while (true) {
        ble.waitForEvent();
    }
}
