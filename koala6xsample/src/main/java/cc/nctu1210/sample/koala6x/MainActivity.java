package cc.nctu1210.sample.koala6x;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cc.nctu1210.api.koala6x.KoalaDevice;
import cc.nctu1210.api.koala6x.KoalaService;
import cc.nctu1210.api.koala6x.KoalaServiceManager;
import cc.nctu1210.api.koala6x.SensorEvent;
import cc.nctu1210.api.koala6x.SensorEventListener;
import cc.nctu1210.view.CustomAdapter;
import cc.nctu1210.view.ModelObject;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener, SensorEventListener {
    private final static String TAG = "Koala";

    private boolean startScan = false;
    private KoalaServiceManager mServiceManager;
    private BluetoothAdapter mBluetoothAdapter;
    /******** for SDK version > 21 **********/
    private BluetoothLeScanner mBLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    /******** for SDK version > 21 **********/
    public static ArrayList<KoalaDevice> mDevices = new ArrayList<KoalaDevice>();  // Manage the devices
    public static ArrayList<AtomicBoolean> mFlags = new ArrayList<AtomicBoolean>();

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 2000;
    public static final int REQUEST_CODE = 30;

    private Button btScan;
//    private Button btStart;

    /* ListView Related */
    private String DEVICE_NAME = "name";
    private String DEVICE_ADDRESS = "address";
    private String DEVICE_RSSI = "rssi";
    private String DEVICE_GX = "gX";
    private String DEVICE_GY = "gY";
    private String DEVICE_GZ = "gZ";
    private ListView listView;
    private List<ModelObject> mObjects = new ArrayList<ModelObject>();
    private CustomAdapter mAdapter;

    private String endpoint;

    private void displayRSSI(final int position, final float rssi) {
        ModelObject object = mObjects.get(position);
        object.setRssi(rssi);
        mAdapter.notifyDataSetChanged();
    }

    private void displayAccData(final int position, final double acc[]) {
        ModelObject object = mObjects.get(position);
        object.setAccelerometerData(acc[0], acc[1], acc[2]);
        mAdapter.notifyDataSetChanged();
    }

    private void displayPDRData(final int position, final float pdrData[]) {
        ModelObject object = mObjects.get(position);
        object.setPedometerData(pdrData[0], pdrData[1], pdrData[2], pdrData[3]);
        mAdapter.notifyDataSetChanged();
    }

    private void updateSamplingRate(final int position, float sampling) {
        ModelObject object = mObjects.get(position);
        object.setSampling(sampling);
        mAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        Bundle bundle = getIntent().getExtras();
        endpoint = bundle.getString("endpoint");
        if (!endpoint.startsWith("http://")) {
            endpoint = "http://" + endpoint;
        }

        if (endpoint.length() - endpoint.replace(":", "").length() == 1) {
            endpoint += ":9999";
        }
        Utils.logging("MainActivity", "Selected EC: %s", endpoint);

//        Utils.show_ec_status_on_notification(SelectECActivity.this, DAN.ec_endpoint(), true);

        btScan = (Button) findViewById(R.id.bt_scan);
//        btStart = (Button) findViewById(R.id.bt_log);
        listView = (ListView) findViewById(R.id.listView);

        mAdapter = new CustomAdapter(this, mObjects);

        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);

        btScan.setOnClickListener(scanListener);

        Log.i(TAG, "getPackageManager");
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        mServiceManager = new KoalaServiceManager(MainActivity.this);
        mServiceManager.registerSensorEventListener(this, SensorEvent.TYPE_ACCELEROMETER, KoalaService.MOTION_WRITE_RATE_10, KoalaService.MOTION_ACCEL_SCALE_2G, KoalaService.MOTION_GYRO_SCALE_250);
        //mServiceManager.registerSensorEventListener(this, SensorEvent.TYPE_ACCELEROMETER);
        mServiceManager.registerSensorEventListener(this, SensorEvent.TYPE_GYROSCOPE);

    }

    private Button.OnClickListener scanListener = new Button.OnClickListener() {
        public void onClick(View v) {
            if (!startScan) {
                startScan = true;
                btScan.setText("Scanning");
                mDevices.clear();
                mFlags.clear();
                // Start to scan the ble device
                scanLeDevice();

                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(SCAN_PERIOD+1000);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startScan = false;
                                setupListView();
                                btScan.setText("Scan");
                            }
                        });
                    }
                }.start();

            } else {
                startScan = false;
                mServiceManager.disconnect();
                mServiceManager.close();
                mDevices.clear();
                mFlags.clear();
                setupListView();
                btScan.setText("Scan");
            }

        }
    };

    private void setupListView() {
        mAdapter.getData().clear();
        Log.i(TAG, "Initializing ListView....."+ mAdapter.getData().size());
        for (int i = 0, size = mDevices.size(); i < size; i++) {
            KoalaDevice d = mDevices.get(i);
            ModelObject object = new ModelObject(d.getDevice().getName(), d.getDevice().getAddress(), String.valueOf(d.getRssi()));
            mObjects.add(object);
        }
        Log.i(TAG, "Initialized ListView....."+ mAdapter.getData().size());

        mAdapter.notifyDataSetChanged();
    }

    private Button.OnClickListener logListener = new Button.OnClickListener() {
        public void onClick(View v) {

        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mBLEScanner =mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
        }
    }

    private void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {

                if (Build.VERSION.SDK_INT < 21) {
                    mBluetoothAdapter.startLeScan(mLeScanCallback);

                    try {
                        Thread.sleep(SCAN_PERIOD);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                } else {
                    mBLEScanner.startScan(mScanCallback);

                    try {
                        Thread.sleep(SCAN_PERIOD);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    mBLEScanner.stopScan(mScanCallback);
                }
                
                /*
                runOnUiThread(new Runnable() {
		            @Override
		            public void run() {
		                // This code will always run on the UI thread, therefore is safe to modify UI elements.
		            	setupListView();
		            }
		        });
		        */
            }
        }.start();
    }

    /**
     * The event callback to handle the found of near le devices
     * For SDK version < 21.
     *
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {

            new Thread() {
                @Override
                public void run() {
                    if (device != null) {
                        KoalaDevice p = new KoalaDevice(device, rssi, scanRecord);
                        int position = findKoalaDevice(device.getAddress());
                        if (position == -1) {
                            AtomicBoolean flag = new AtomicBoolean(false);
                            mDevices.add(p);
                            mFlags.add(flag);
                            Log.i(TAG, "Find device:"+p.getDevice().getAddress());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                    setupListView();
                                }
                            });
                        }
                    }
                }
            }.start();
        }
    };

    /**
     * The event callback to handle the found of near le devices
     * For SDK version >= 21.
     *
     */
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            final ScanResult scanResult = result;
            final BluetoothDevice device = scanResult.getDevice();

            new Thread() {
                @Override
                public void run() {
                    if (device != null) {
                        KoalaDevice p = new KoalaDevice(device, scanResult.getRssi(), scanResult.getScanRecord().getBytes());
                        int position = findKoalaDevice(device.getAddress());
                        if (position == -1) {
                            AtomicBoolean flag = new AtomicBoolean(false);
                            mDevices.add(p);
                            mFlags.add(flag);
                            Log.i(TAG, "Find device:"+p.getDevice().getAddress());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                    setupListView();
                                }
                            });
                        }
                    }
                }
            }.start();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private int findKoalaDevice(String macAddr) {
        if (mDevices.size() == 0)
            return -1;
        for (int i=0; i<mDevices.size(); i++) {
            KoalaDevice tmpDevice = mDevices.get(i);
            if (macAddr.matches(tmpDevice.getDevice().getAddress()))
                return i;
        }
        return -1;
    }

    @Override
    protected void onStop() {
        super.onStop();
        for (int i=0; i<mFlags.size(); i++) {
            AtomicBoolean flag = mFlags.get(i);
            flag.set(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mServiceManager.disconnect();
        mServiceManager.close();
        mDevices.clear();
        mFlags.clear();

        DAN.deregister();
        System.exit(0);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO Auto-generated method stub
        String macAddress = mAdapter.getData().get(position).getAddress();
        KoalaDevice d = mDevices.get(position);
//        Log.d(TAG, "Connecting to device:"+macAddress);
        Utils.logging("MainActivity", "Connecting to device: %s", macAddress);
        d.resetSamplingRate();
        d.setConnectedTime();
        mServiceManager.connect(macAddress);

        DAN.subscribe(DAN.CONTROL_CHANNEL, new EventSubscriber());
        JSONObject profile = new JSONObject();
        String clean_mac_addr = DAN.get_clean_mac_addr(macAddress);
        try {
            profile.put("d_name", "Koala-"+ clean_mac_addr.substring(clean_mac_addr.length() - 4));
            profile.put("dm_name", Constants.dm_name);
            JSONArray feature_list = new JSONArray();
            for (String f: Constants.df_list) {
                feature_list.put(f);
            }
            profile.put("df_list", feature_list);
            profile.put("u_name", Constants.u_name);
            profile.put("monitor", clean_mac_addr);
            DAN.register(endpoint, clean_mac_addr, profile);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChange(final SensorEvent e) {
        final int eventType = e.type;
        final double values [] = new double[3];
        values[0] = e.values[0];
        values[1] = e.values[1];
        values[2] = e.values[2];
        switch (eventType) {
            case SensorEvent.TYPE_ACCELEROMETER:
                final int acc_position = findKoalaDevice(e.device.getAddress());
                if (acc_position != -1) {
                    final double v [] = new double[3];
                    v[0] = e.values[0] * 9.8;
                    v[1] = e.values[1] * 9.8;
                    v[2] = e.values[2] * 9.8;
                    DAN.push("Acceleration", v);
                    final KoalaDevice d = mDevices.get(acc_position);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                d.addRecvItem();
//                                Log.d(TAG, "time="+System.currentTimeMillis()+ "gX:" + values[0]+"gY:" + values[1]+"gZ:" + values[2]+"\n");
                                Log.d(TAG, String.format("time=%d aX:%3.6f aY:%3.6f aZ:%3.6f", System.currentTimeMillis(), values[0], values[1], values[2]));
                                updateSamplingRate(acc_position, d.getCurrentSamplingRate());
                                displayAccData(acc_position, values);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });

                }
                break;
            case SensorEvent.TYPE_GYROSCOPE:
                final int gyro_position = findKoalaDevice(e.device.getAddress());
                if (gyro_position != -1) {
                    DAN.push("Gyroscope", values);
                    final KoalaDevice d = mDevices.get(gyro_position);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                //d.addRecvItem();
//                                values[0] = e.values[0];
//                                values[1] = e.values[1];
//                                values[2] = e.values[2];
//                                Log.d(TAG, "time="+System.currentTimeMillis()+ "aX:" + values[0]+"aY:" + values[1]+"aZ:" + values[2]+"\n");
                                Log.d(TAG, String.format("time=%d gX:%3.6f gY:%3.6f gZ:%3.6f", System.currentTimeMillis(), values[0], values[1], values[2]));
                                //updateSamplingRate(gyro_position, d.getCurrentSamplingRate());
                                //displayAccData(gyro_position, values);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });
                }
                break;
        }
    }

    @Override
    public void onConnectionStatusChange(boolean status) {

    }

    @Override
    public void onRSSIChange(String addr, float rssi) {
        final int position = findKoalaDevice(addr);
        if (position != -1) {
            Log.d(TAG, "mac Address:"+addr+" rssi:"+rssi);
            displayRSSI(position, rssi);
        }
    }

    class EventSubscriber implements DAN.Subscriber {
        public void odf_handler (final String feature, final DAN.ODFObject odf_object) {
            if (!feature.equals(DAN.CONTROL_CHANNEL)) {
                return;
            }
            switch (odf_object.event) {
                case REGISTER_FAILED:
                    Utils.logging("MainActivity", "Register failed");
                    finish();
                    break;
                case REGISTER_SUCCEED:
                    Utils.show_ec_status_on_notification(MainActivity.this, DAN.ec_endpoint(), true);
                    break;
                default:
                    break;
            }
        }
    }
}
