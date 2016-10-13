/*
 * Copyright (C) 2013 The Android Open Source Project
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

package ch.ethz.inf.vs.a1.fabischn.ble;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.SecondScale;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private final int MAX_SERIES_ELEMENTS = 50; // How many elements should a series at most contain

    private TextView mConnectionState;
    private TextView mDataFieldHumid;
    private TextView mDataFieldTemp;
    private String mDeviceName;
    private String mDeviceAddress;
    private GraphView mGraphView;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private boolean mConnectedHumid = false;
    private boolean mConnectedTemp = false;

    private LineGraphSeries<DataPoint> mSeriesTemp;
    private LineGraphSeries<DataPoint> mSeriesHumid;

    private BluetoothGattService mServiceHumidity;
    private BluetoothGattService mServiceTemperature;
    private BluetoothGattCharacteristic mCharacteristicHumidity;
    private BluetoothGattCharacteristic mCharacteristicTemperature;

    private long mStartTime;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected");
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                mStartTime = SystemClock.elapsedRealtime();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getSHT31ServicesHumidAndTemp();
                        connectToSTH31Temp();
                    }
                });


            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String uuid = intent.getStringExtra("uuid");
                if (uuid.equals(SensirionSHT31UUIDS.UUID_HUMIDITY_CHARACTERISTIC.toString())){
                    displayHumidData(SystemClock.elapsedRealtime(),intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                } else if(uuid.equals(SensirionSHT31UUIDS.UUID_TEMPERATURE_CHARACTERISTIC.toString())){
                    displayTempData(SystemClock.elapsedRealtime(),intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                } else{
                    Log.e(TAG, "BroadcastReceiver: got data from unknown characteristic");
                }
            } else if (BluetoothLeService.ACTION_CHARACTERISTIC_HUMID_CONNECTED.equals(action)){
                Log.i(TAG,"received humid ok");
                mConnectedHumid = true;

            } else if (BluetoothLeService.ACTION_CHARACTERISTIC_TEMP_CONNECTED.equals(action)){
                Log.i(TAG,"received temp ok");
                mConnectedTemp = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectToSHT31Humid();
                    }
                });

            }
        }
    };

    private void clearUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDataFieldHumid.setText(R.string.no_data);
                mDataFieldTemp.setText(R.string.no_data);
            }
        });

    }


    private void getSHT31ServicesHumidAndTemp(){
        List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices();
        String uuid;
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            if (uuid.equals(SensirionSHT31UUIDS.UUID_TEMPERATURE_SERVICE.toString())) {
                mServiceTemperature = gattService;

            } else if (uuid.equals(SensirionSHT31UUIDS.UUID_HUMIDITY_SERVICE.toString())) {
                mServiceHumidity = gattService;
            }
        }
    }

    public void connectToSHT31Humid(){
        mCharacteristicHumidity = mServiceHumidity.getCharacteristic(SensirionSHT31UUIDS.UUID_HUMIDITY_CHARACTERISTIC);
        mBluetoothLeService.setCharacteristicNotification(mCharacteristicHumidity, true);
    }

    public void connectToSTH31Temp(){
        mCharacteristicTemperature = mServiceTemperature.getCharacteristic(SensirionSHT31UUIDS.UUID_TEMPERATURE_CHARACTERISTIC);
        mBluetoothLeService.setCharacteristicNotification(mCharacteristicTemperature, true);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataFieldHumid = (TextView) findViewById(R.id.data_value_humid);
        mDataFieldTemp = (TextView) findViewById(R.id.data_value_temp);


        // Format the GraphView
        mGraphView = (GraphView) findViewById(R.id.graphview);
        Viewport vp = mGraphView.getViewport();
        SecondScale ss = mGraphView.getSecondScale();
        GridLabelRenderer glr = mGraphView.getGridLabelRenderer();

        int colorTemp = Color.BLUE;
        int colorHumid = Color.RED;

        mSeriesTemp = new LineGraphSeries<>();
        mSeriesTemp.setColor(colorTemp);
        mSeriesTemp.setBackgroundColor(colorTemp);
//        mSeriesTemp.setDrawBackground(true);
        mSeriesTemp.setTitle("°C");
        mGraphView.addSeries(mSeriesTemp);

        mSeriesHumid = new LineGraphSeries<>();
        mSeriesHumid.setColor(colorHumid);
        mSeriesHumid.setBackgroundColor(colorHumid);
//        mSeriesHumid.setDrawBackground(true);
        mSeriesHumid.setTitle("hPa");
        ss.addSeries(mSeriesHumid);

        mGraphView.getLegendRenderer().setVisible(true);
        mGraphView.getLegendRenderer().setBackgroundColor(Color.LTGRAY);
        mGraphView.getLegendRenderer().setFixedPosition(0,0);


        // Set the scales
        vp.setXAxisBoundsManual(true); // if true, the labels don't update
        vp.setYAxisBoundsManual(true);
        vp.setScalableY(false);
        vp.setScalable(false);
        vp.setMinX(0);
        vp.setMaxX(30);
        vp.setMinY(-40);
        vp.setMaxY(65);
        ss.setMinY(0);
        ss.setMaxY(100);

        glr.setHighlightZeroLines(false);
        glr.setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);

        // Set Y axis labels
        glr.setNumVerticalLabels(6); // Somehow this affects second scale...
        glr.setVerticalLabelsSecondScaleAlign(Paint.Align.RIGHT);
        glr.setVerticalLabelsColor(colorTemp);
        glr.setVerticalLabelsSecondScaleColor(colorHumid);
        

        // Set X axis labels
        glr.setHorizontalAxisTitle("Time in seconds");
        glr.setNumHorizontalLabels(5);
        glr.reloadStyles();


        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);


        // TODO add progressbar for pending device connection
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayHumidData(final long realTime, final String data) {
        if (data != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDataFieldHumid.setText(data);

                    mSeriesHumid.appendData(new DataPoint((double) (realTime - mStartTime)/1e3,Double.parseDouble(data)), true, MAX_SERIES_ELEMENTS);
                }
            });
        }
    }
    private void displayTempData(final long realTime, final String data) {
        if (data != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDataFieldTemp.setText(data);
                    mSeriesTemp.appendData(new DataPoint((double) (realTime - mStartTime)/1e3,Double.parseDouble(data)), true, MAX_SERIES_ELEMENTS);
                }
            });
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_CHARACTERISTIC_HUMID_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_CHARACTERISTIC_TEMP_CONNECTED);
        return intentFilter;
    }


}
