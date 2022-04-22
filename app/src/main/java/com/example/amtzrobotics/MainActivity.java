package com.example.amtzrobotics;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
//import com.github.mikephil.charting.charts.LineChart;
//import com.github.mikephil.charting.components.Description;
//import com.github.mikephil.charting.components.XAxis;
//import com.github.mikephil.charting.data.Entry;
//import com.github.mikephil.charting.data.LineData;
//import com.github.mikephil.charting.data.LineDataSet;
//import com.github.mikephil.charting.formatter.ValueFormatter;
//import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
//import com.github.mikephil.charting.utils.ColorTemplate;


public class MainActivity extends AppCompatActivity {

    // GUI Components
    private TextView mBluetoothStatus;
    private TextView mReadBuffer;
    private Button mScanBtn;
    private Button mOffBtn;
    private Button mListPairedDevicesBtn;
    private Button mDiscoverBtn,mRegister;
    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private ListView mDevicesListView;
    private LinearLayout ll_temp,ll_bp,ll_spo2,ll_ecg;
    private CheckBox mLED1;
    private ProgressBar mProgressBar;
    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier


    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    String readMessage = null;
    private long xVal;
    private LineChart graphView;

    ArrayList<Entry> pricesHigh = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);
        mReadBuffer = (TextView) findViewById(R.id.readBuffer);
        mScanBtn = (Button)findViewById(R.id.scan);
        mOffBtn = (Button)findViewById(R.id.off);
        mDiscoverBtn = (Button)findViewById(R.id.discover);
        mListPairedDevicesBtn = (Button)findViewById(R.id.PairedBtn);
       // mLED1 = (CheckBox)findViewById(R.id.checkboxLED1);
      mRegister=(Button)findViewById(R.id.register);
        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        mProgressBar=(ProgressBar)findViewById(R.id.progressbar1_timerview);
        ll_temp=(LinearLayout)findViewById(R.id.ll_temperature);
        ll_spo2=(LinearLayout)findViewById(R.id.ll_pulse);
        ll_bp=(LinearLayout)findViewById(R.id.ll_bp);
        ll_ecg=(LinearLayout)findViewById(R.id.ll_ecg);

        mProgressBar.bringToFront();
        configureLineChart();

        mHandler = new Handler(){
            @SuppressLint("HandlerLeak")
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){

                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if(readMessage.equalsIgnoreCase("P100")){


                    }
                    Log.d("msg",readMessage);
                    //readMessage=readMessage.replace("P","");
                    //readMessage=readMessage+'%';
                    updateStatus(readMessage);


                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        mBluetoothStatus.setText("Connected to Device: " + (String)(msg.obj));
                    else
                        mBluetoothStatus.setText("Connection Failed");
                }
            }

            private void updateStatus(String msg) {
                //Log.d("mm", msg);
                if (msg.contains("P")) {
                    msg = msg.trim();
                    final String fmsg = msg.replace("P", "").trim();


                    new Handler().postDelayed(new Runnable() {
                        @SuppressLint("HandlerLeak")
                        @Override
                        public void run() {
                            try {

                                mReadBuffer.setText(fmsg);
                                //Integer val=Integer.valueOf(fmsg);
                                Log.d("mm", fmsg);
                                if (fmsg.contains("100")) {

                                    mConnectedThread.write("A");
                                    mProgressBar.setVisibility(View.GONE);
                                    mReadBuffer.setVisibility(View.GONE);

                                }
                            } catch (Exception e) {
                                Log.d("mm", e.getMessage());
                            }
                        }
                    }, 10);


                }
            }
        };

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {

//            mLED1.setOnClickListener(new View.OnClickListener(){
//                @Override
//                public void onClick(View v){
//                    if(mConnectedThread != null) //First check to make sure thread created
//                        mConnectedThread.write("1");
//                }
//            });


            mScanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn(v);
                }
            });

            mOffBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    bluetoothOff(v);
                }
            });

            mRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mConnectedThread.write("C");
                }
            });

            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    listPairedDevices(v);
                }
            });

            mDiscoverBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    discover(v);
                }
            });

            ll_temp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mConnectedThread.write("A");
                    mProgressBar.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onButtonShowPopupWindowClick(view,1);
                        }
                    },0);

                }
            });
            ll_spo2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mConnectedThread.write("A");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onButtonShowPopupWindowClick(view,2);
                        }
                    },0);

                }
            });
            ll_bp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mConnectedThread.write("A");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onButtonShowPopupWindowClick(view,3);
                        }
                    },0);

                }
            });
            ll_ecg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mConnectedThread.write("A");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onButtonShowPopupWindowClick(view,4);
                        }
                    },0);

                }
            });
        }
    }
    private void configureLineChart() {
        try {
            Description desc = new Description();
            desc.setText("Ecg");
            //desc.setTextSize(28);
            graphView.setDescription(desc);

            XAxis xAxis = graphView.getXAxis();
//        xAxis.setValueFormatter(new ValueFormatter() {
//            private final SimpleDateFormat mFormat = new SimpleDateFormat("dd MMM", Locale.ENGLISH);
//
//            @Override
//            public String getFormattedValue(float value) {
//                long millis = (long) value * 1000L;
//                return mFormat.format(new Date(millis));
//            }
//        });
            xAxis.setAxisMinimum(0);
            xAxis.setAxisMaximum(2500);
        }catch(Exception e){}
    }

//    private void updateStatus(String msg) {
//
//        if(msg.equals("P100")) {
//            Log.d("mm",msg);
//            mConnectedThread.write("A");
//            mProgressBar.setVisibility(View.GONE);
//            mReadBuffer.setVisibility(View.GONE);
//        }
//    }

    private void bluetoothOn(View view){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText("Enabled");
            } else
                mBluetoothStatus.setText("Disabled");
        }
    }

    private void bluetoothOff(View view){
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover(View view){
        // Check if the device is already discovering
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {


        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };
    public void onButtonShowPopupWindowClick(View view,int type) {

        // inflate the layout of the popup window


        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_window, null);
        TextView textView=(TextView) popupView.findViewById(R.id.txtResult);
        Button but_graph=(Button) popupView.findViewById(R.id.btn_getgraph);
        graphView = popupView.findViewById(R.id.idGraphView);
        LinearLayout ll=(LinearLayout)popupView.findViewById(R.id.linearLayout2);
        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched
        if(type==1){
            //textView.setText("");
            String cent=null;
            String fah=null;
            mConnectedThread.write("T");
            if(readMessage.contains("C")) {
            String[] temp = readMessage.split("\\s");
            if (temp.length >= 2) {
                cent = temp[0];
                cent=cent.replace("C","");
                fah = temp[2] ;
                fah=fah.replace("F","");
            }}
            String finalcent = cent;
            String finalfah = fah;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    textView.setText("Centigrade :"+ finalcent +"\u2103"+"\n"+"Fahrenheit :"+ finalfah+"\u2109");
                }
            },5000);

            //textView.setText("Centigrade : "+"\n"+"Faherheit :");
            mProgressBar.setVisibility(View.GONE);
        }
        if(type==2){
            //textView.setText("");
            String spO2=null;
            String bpm=null;
            mConnectedThread.write("S");
            if(readMessage.contains("H")) {
                String[] pulse = readMessage.split("\\s");
                if (pulse.length >= 2) {
                    spO2 = pulse[2];
                    spO2=spO2.replace("S","");
                    bpm = pulse[0] ;
                    bpm=bpm.replace("H","");
                }}
            String finalBpm = bpm;
            String finalSpO = spO2;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    textView.setText("SPO2 :"+ finalSpO +"%"+"\n"+"Pulse :"+ finalBpm+"bpm");
                }
            },5000);
            //textView.setText("Spo2 : "+"\n"+"Pulse :");
            mProgressBar.setVisibility(View.GONE);
        }
        if(type==3){
            String systolic=null;
            String diastolic=null;
            //textView.setText("");
            mConnectedThread.write("B");
            if(readMessage.contains("Y")) {
                String[] bp = readMessage.split("\\s");
                if (bp.length >= 2) {

                    systolic = bp[0];
                    systolic = systolic.replace("Y", "");
                    diastolic = bp[2];
                    diastolic = diastolic.replace("D", "");
                }
                Log.d("sys",systolic);
                String finalSystolic = systolic;
                String finalDiastolic = diastolic;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText("Systolic : " + finalSystolic + "\n" + "Diastolic :" + finalDiastolic);
                    }
                }, 5000);
                mProgressBar.setVisibility(View.GONE);
            }  //textView.setText("Systolic : "+"\n"+"Diastolic :");
        }
        if(type==4){
            //textView.setText("");
            mConnectedThread.write("E");
            if(readMessage.contains("E")) {
                String[] ecgVal = readMessage.split("\\s");
                String e = ecgVal[0].replace("E", "");
                final Handler handlern = new Handler();
                handlern.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Do something after 5s = 5000ms
                        try {
                            mProgressBar.setVisibility(View.VISIBLE);

                            if(e!="0") {
                                //if(xVal==0){
                                //  xVal=1;
                                //}else {
                                //  if (xVal < 2500) {
                                xVal = xVal + 1;
                                // }
                                //}

                                getStockData(xVal, Long.parseLong(e));
                            }else if(e=="0"){
                                mProgressBar.setVisibility(View.GONE);
                            }

                        }catch (Exception ee){}
                        //handlern.removeCallbacks(null);
                    }
                }, 5);
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    //ll.setVisibility(View.GONE);
                    textView.setText("Get graph");
                    //textView.setVisibility(View.GONE);
                    //textView.setText(readMessage);
                    mConnectedThread.write("A");
                }
            },20000);
            mProgressBar.setVisibility(View.GONE);
            //textView.setText("ECG : "+"\n"+"ECG :");
        }
        but_graph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                        LineDataSet highLineDataSet = new LineDataSet(pricesHigh, " ECG graph");
                        highLineDataSet.setDrawCircles(false);
                        highLineDataSet.setCircleRadius(4);
                        highLineDataSet.setDrawValues(false);
                        highLineDataSet.setLineWidth(1);
                        highLineDataSet.setColor(Color.GREEN);
                        highLineDataSet.setCircleColor(Color.GREEN);
                        dataSets.add(highLineDataSet);
                        LineData lineData = new LineData(dataSets);
                        //graphView.setData(null);
                        graphView.setData(lineData);
                    }
                },2000);

            }
        });
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if(textView.getText()=="Get graph"){
                    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                    LineDataSet highLineDataSet = new LineDataSet(pricesHigh, " ECG graph");
                    highLineDataSet.setDrawCircles(false);
                    highLineDataSet.setCircleRadius(4);
                    highLineDataSet.setDrawValues(false);
                    highLineDataSet.setLineWidth(1);
                    highLineDataSet.setColor(Color.GREEN);
                    highLineDataSet.setCircleColor(Color.GREEN);
                    dataSets.add(highLineDataSet);
                    LineData lineData = new LineData(dataSets);
                    //graphView.setData(null);
                    graphView.setData(lineData);
               // }
            }
        });
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mConnectedThread.write("A");
                popupWindow.dismiss();
                return true;
            }
        });
    }

    @SuppressLint("NewApi")
    private void getStockData(Long xVal,Long yval) {
        final Handler handler = new Handler();
        long finalXVal = xVal;
        //finalXVal=0L;
        long finalXVal1 = finalXVal;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                Log.d("ecgval", String.valueOf(yval));
                Log.d("ecgvalX", String.valueOf(finalXVal1));
               pricesHigh.add(new Entry(finalXVal1,yval));
            }
        }, 0);
    }
    
    
    private void listPairedDevices(View view){
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread()
            {
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(fail == false) {
                        mConnectedThread = new ConnectedThread(mBTSocket);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}