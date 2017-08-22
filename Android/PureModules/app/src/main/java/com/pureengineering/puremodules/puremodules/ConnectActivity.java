
/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.pureengineering.puremodules.puremodules;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.util.Random;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;

public class ConnectActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    TextView mRemoteRssiVal;
    RadioGroup mRg;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect,btnSend;
    private EditText edtMessage;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    //Graph Setup Variables
    private LineGraphSeries<DataPoint> lis2de_Series;
    public int lis2de_LastXValue = 0;
    private LineGraphSeries<DataPoint> si1153_Series;
    public int si1153_LastXValue = 0;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        messageListView = (ListView) findViewById(R.id.listMessage);
        listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);
        //messageListView.setVisibility(View.INVISIBLE);
        btnConnectDisconnect = (Button) findViewById(R.id.btn_select);
        btnSend = (Button) findViewById(R.id.sendButton);
        edtMessage = (EditText) findViewById(R.id.sendText);

        final Switch lis2de_switch = (Switch) findViewById(R.id.lis2deswitch);
        final Switch tmp007_switch = (Switch) findViewById(R.id.tmp007switch);
        final Switch veml6075_switch = (Switch) findViewById(R.id.veml6075switch);
        final Switch bme280_switch = (Switch) findViewById(R.id.bme280switch);
        final Switch si1153_switch = (Switch) findViewById(R.id.si1153switch);
        final Switch ads1114_switch = (Switch) findViewById(R.id.ads1114switch);
        final Switch fdc2214_switch = (Switch) findViewById(R.id.fdc2214switch);

        //Accelerometer Graph setup
        GraphView lis2de_graphview = (GraphView) findViewById(R.id.lis2degraph);
        lis2de_graphview.getViewport().setXAxisBoundsManual(true);
        lis2de_graphview.getViewport().setMinX(0);
        lis2de_graphview.getViewport().setMaxX(10);
        lis2de_Series = new LineGraphSeries<>();
        lis2de_graphview.addSeries(lis2de_Series);

        //Prox Sensor Graph Setup
        GraphView si1153_graphview = (GraphView) findViewById(R.id.si1153graph);
        si1153_graphview.getViewport().setXAxisBoundsManual(true);
        si1153_graphview.getViewport().setMinX(0);
        si1153_graphview.getViewport().setMaxX(48);
        si1153_Series = new LineGraphSeries<>();
        si1153_graphview.addSeries(si1153_Series);



        service_init();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    //@Override 
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }


        lis2de_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LinearLayout rl = (LinearLayout) findViewById(R.id.lis2dedata);
                LinearLayout rl_graph = (LinearLayout) findViewById(R.id.lis2degraphlayout);

                if(isChecked){
                    if(btnConnectDisconnect.getText().equals("Connect")){
                        Context context = getApplicationContext();
                        CharSequence text = "Connect to CoreModule first";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                        lis2de_switch.setChecked(false);
                    }
                    else {
                        rl.setVisibility(View.VISIBLE);
                        rl_graph.setVisibility(View.VISIBLE);
                        String message = "1";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    rl.setVisibility(View.GONE);
                    rl_graph.setVisibility(View.GONE);
                    if(btnConnectDisconnect.getText().equals("Disconnect")) {
                        String message = "2";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                }
            }
        });

        tmp007_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LinearLayout rl = (LinearLayout) findViewById(R.id.tmp007data);
                if(isChecked){
                    if(btnConnectDisconnect.getText().equals("Connect")){
                        Context context = getApplicationContext();
                        CharSequence text = "Connect to CoreModule first";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                        tmp007_switch.setChecked(false);
                    }
                    else {
                        rl.setVisibility(View.VISIBLE);
                        String message = "h";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    rl.setVisibility(View.GONE);
                    if(btnConnectDisconnect.getText().equals("Disconnect")) {
                        String message = "i";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        veml6075_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LinearLayout rl = (LinearLayout) findViewById(R.id.veml6075data);
                if(isChecked){
                    if(btnConnectDisconnect.getText().equals("Connect")){
                        Context context = getApplicationContext();
                        CharSequence text = "Connect to CoreModule first";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                        veml6075_switch.setChecked(false);
                    }
                    else {
                        rl.setVisibility(View.VISIBLE);
                        String message = "7";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    rl.setVisibility(View.GONE);
                    if(btnConnectDisconnect.getText().equals("Disconnect")) {

                        String message = "8";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        bme280_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LinearLayout rl = (LinearLayout) findViewById(R.id.bme280data);
                if(isChecked){
                    if(btnConnectDisconnect.getText().equals("Connect")){
                        Context context = getApplicationContext();
                        CharSequence text = "Connect to CoreModule first";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                        bme280_switch.setChecked(false);
                    }
                    else {
                        rl.setVisibility(View.VISIBLE);
                        String message = "5";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    rl.setVisibility(View.GONE);
                    if(btnConnectDisconnect.getText().equals("Disconnect")) {
                        String message = "6";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        si1153_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LinearLayout rl = (LinearLayout) findViewById(R.id.si1153data);
                LinearLayout rl_graph = (LinearLayout) findViewById(R.id.si1153graphlayout);

                if(isChecked){
                    if(btnConnectDisconnect.getText().equals("Connect")){
                        Context context = getApplicationContext();
                        CharSequence text = "Connect to CoreModule first";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                        si1153_switch.setChecked(false);
                    }
                    else {
                        rl.setVisibility(View.VISIBLE);
                        rl_graph.setVisibility(View.VISIBLE);
                        String message = "9";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    rl.setVisibility(View.GONE);
                    rl_graph.setVisibility((View.GONE));
                    si1153_Series.resetData(new DataPoint[] {new DataPoint(0,0)});
                    if(btnConnectDisconnect.getText().equals("Disconnect")) {
                        String message = "a";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        ads1114_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LinearLayout rl = (LinearLayout) findViewById(R.id.ads1114data);
                if(isChecked){
                    if(btnConnectDisconnect.getText().equals("Connect")){
                        Context context = getApplicationContext();
                        CharSequence text = "Connect to CoreModule first";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                        ads1114_switch.setChecked(false);
                    }
                    else {
                        rl.setVisibility(View.VISIBLE);
                        String message = "j";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    rl.setVisibility(View.GONE);
                    if(btnConnectDisconnect.getText().equals("Disconnect")) {
                        String message = "k";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                }
            }
        });

        fdc2214_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LinearLayout rl = (LinearLayout) findViewById(R.id.fdc2214data);
                if(isChecked){
                    if(btnConnectDisconnect.getText().equals("Connect")){
                        Context context = getApplicationContext();
                        CharSequence text = "Connect to CoreModule first";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                        fdc2214_switch.setChecked(false);
                    }
                    else {
                        rl.setVisibility(View.VISIBLE);
                        String message = "l";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    rl.setVisibility(View.GONE);
                    if(btnConnectDisconnect.getText().equals("Disconnect")) {
                        String message = "m";
                        byte[] value;
                        try {
                            //send data to service
                            //Update the log with time stamp
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                }
            }
        });




        // Handle Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                    if (btnConnectDisconnect.getText().equals("Connect")){

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                        Intent newIntent = new Intent(ConnectActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice!=null)
                        {
                            mService.disconnect();

                        }
                    }
                }
            }
        });
        // Handle Send button
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.sendText);
                String message = editText.getText().toString();
                byte[] value;
                try {
                    //send data to service
                    //Update the log with time stamp
                    value = message.getBytes("UTF-8");
                    mService.writeRXCharacteristic(value);
                    String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                    listAdapter.add("["+currentDateTimeString+"] TX: "+ message);
                    messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                    edtMessage.setText("");
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });

        // Set initial UI state

    };


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override

        //Handler events that received from UART service
        public void handleMessage(Message msg) {

        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;

            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        edtMessage.setEnabled(true);
                        btnSend.setEnabled(true);
                        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - ready");
                        listAdapter.add("["+currentDateTimeString+"] Connected to: "+ mDevice.getName());
                        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }


            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        edtMessage.setEnabled(false);
                        btnSend.setEnabled(false);
                        ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        listAdapter.add("["+currentDateTimeString+"] Disconnected to: "+ mDevice.getName());
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        //setUiState();

                    }
                });
            }



            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }

            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String text = new String(txValue, "UTF-8");
                            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                            listAdapter.add("["+currentDateTimeString+"] RX: "+text);
                            messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                            updateSensorData(text);

                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }

            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }


        }
    };



    private void updateSensorData(String text){
        int dataStartIndex = text.indexOf(":");
        String data = text.substring(dataStartIndex+1);
        double data_double = Double.parseDouble(data);

        if(text.contains("lis2dex")){
            ((TextView) findViewById(R.id.lis2dex)).setText(data);
            lis2de_Series.appendData(new DataPoint(lis2de_LastXValue,data_double),true, 12);
            lis2de_LastXValue++;
        }
        if(text.contains("lis2dey")){
            ((TextView) findViewById(R.id.lis2dey)).setText(data);
        }
        if(text.contains("lis2dez")){
            ((TextView) findViewById(R.id.lis2dez)).setText(data);
        }
        if(text.contains("tmp007obj")){
            ((TextView) findViewById(R.id.tmp007tempobj)).setText(data);
        }
        if(text.contains("tmp007die")){
            ((TextView) findViewById(R.id.tmp007tempdie)).setText(data);
        }
        if(text.contains("veml6075a")){
            ((TextView) findViewById(R.id.veml6075a)).setText(data);
        }
        if(text.contains("veml6075b")){
            ((TextView) findViewById(R.id.veml6075b)).setText(data);
        }
        if(text.contains("bme280press")){
            ((TextView) findViewById(R.id.bme280press)).setText(data);
        }
        if(text.contains("bme280hum")){
            ((TextView) findViewById(R.id.bme280hum)).setText(data);
        }
        if(text.contains("bme280alt")){
            ((TextView) findViewById(R.id.bme280alt)).setText(data);
        }
        if(text.contains("bme280tempf")){
            ((TextView) findViewById(R.id.bme280tempf)).setText(data);
        }
        if(text.contains("bme280tempc")){
            ((TextView) findViewById(R.id.bme280tempc)).setText(data);
        }
        if(text.contains("sx")){
            ((TextView) findViewById(R.id.si1153x)).setText(data);
            //Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
            si1153_Series.appendData(new DataPoint(si1153_LastXValue,data_double),true, 50);
            si1153_LastXValue++;
        }
        if(text.contains("ads1114out")){
            ((TextView) findViewById(R.id.ads1114out)).setText(data);
        }
        if(text.contains("fdc2214ch0")){
            ((TextView) findViewById(R.id.fdc2214ch)).setText(data);
        }
    }

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }


    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    mService.connect(deviceAddress);


                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }


    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }
}
