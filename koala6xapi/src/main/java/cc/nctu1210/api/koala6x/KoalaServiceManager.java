package cc.nctu1210.api.koala6x;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yi-Ta_Chuang on 2016/1/31.
 */
public class KoalaServiceManager {
    private final static String TAG = KoalaServiceManager.class.getSimpleName();
    private final List<SparseArray<SensorEventListener>> eventListeners = new ArrayList<SparseArray<SensorEventListener>>();
    private int sensor_rate = KoalaService.MOTION_WRITE_RATE_10;
    private int acc_fsr = KoalaService.MOTION_ACCEL_SCALE_2G;
    private int gyro_fsr = KoalaService.MOTION_GYRO_SCALE_250;
    private Activity mActivity;
    private KoalaService mBluetoothLeService; // the main service to control the ble device

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((KoalaService.LocalBinder) service)
                    .getService();
            Log.i(TAG, "Initializing Bluetooth.....");
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
            Log.i(TAG, "Success!");
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (KoalaService.ACTION_GATT_CONNECTED.equals(action)) {
                final String addr = intent.getStringExtra(KoalaService.EXTRA_NAME);
                startReadRssi(addr);
                for (int i=0, size=eventListeners.size(); i<size; i++) {
                    if (eventListeners.get(i).get(SensorEvent.TYPE_ACCELEROMETER) != null) {
                        SensorEventListener l = eventListeners.get(i).get(SensorEvent.TYPE_ACCELEROMETER);
                        l.onConnectionStatusChange(true);
                    }
                }
            } else if (KoalaService.ACTION_GATT_DISCONNECTED.equals(action)) {
                final String addr = intent.getStringExtra(KoalaService.EXTRA_NAME);
                //fire a disconnected event
                for (int i=0, size=eventListeners.size(); i<size; i++) {
                    if (eventListeners.get(i).get(SensorEvent.TYPE_ACCELEROMETER) != null) {
                        SensorEventListener l = eventListeners.get(i).get(SensorEvent.TYPE_ACCELEROMETER);
                        l.onConnectionStatusChange(false);
                    }
                }
            } else if (KoalaService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                final String addr = intent.getStringExtra(KoalaService.EXTRA_NAME);
                Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED! mac Address:" + addr);
                setAccFSR(addr, acc_fsr);
                setGyroFSR(addr, gyro_fsr);
                setSamplingRate(addr, sensor_rate);
                startToReadData(addr);
            } else if (KoalaService.ACTION_GATT_RSSI.equals(action)) {
                final String addr = intent.getStringExtra(KoalaService.EXTRA_NAME);
                float rssi = Float.valueOf(intent.getStringExtra(KoalaService.EXTRA_DATA));
                Log.d(TAG, "mac Address:" + addr + " rssi:" + rssi);
                //fire a rssi event
                for (int i=0, size=eventListeners.size(); i<size; i++) {
                    if (eventListeners.get(i).get(SensorEvent.TYPE_ACCELEROMETER) != null) {
                        SensorEventListener l = eventListeners.get(i).get(SensorEvent.TYPE_ACCELEROMETER);
                        l.onRSSIChange(addr, rssi);
                    }
                }
                startReadRssi(addr);
            } else if (KoalaService.ACTION_RAW_GYRO_DATA_AVAILABLE.equals(action)) {
                final String addr = intent.getStringExtra(KoalaService.EXTRA_NAME);
                final double values [] = intent.getDoubleArrayExtra(KoalaService.EXTRA_DATA);
//                Log.i(TAG, "ACTION_GYRO_DATA_AVAILABLE received!!");
                //fire a pdr data event
                BluetoothGatt gattServer = mBluetoothLeService.getGattbyAddr(addr);
                if (gattServer != null) {
                    BluetoothDevice device = gattServer.getDevice();
                    SensorEvent e = new SensorEvent(SensorEvent.TYPE_GYROSCOPE, device, 5);
                    e.values[0] = (float) values[0];
                    e.values[1] = (float) values[1];
                    e.values[2] = (float) values[2];
                    for (int i = 0, size = eventListeners.size(); i < size; i++) {
                        if (eventListeners.get(i).get(SensorEvent.TYPE_GYROSCOPE) != null) {
                            SensorEventListener l = eventListeners.get(i).get(SensorEvent.TYPE_GYROSCOPE);
                            l.onSensorChange(e);
                        }
                    }
                }
            } else if (KoalaService.ACTION_RAW_ACC_DATA_AVAILABLE.equals(action)) {
                final String addr = intent.getStringExtra(KoalaService.EXTRA_NAME);
                final double values [] = intent.getDoubleArrayExtra(KoalaService.EXTRA_DATA);
//                Log.i(TAG, "ACTION_RAW_ACC_DATA_AVAILABLE received!!");
                //fire a raw acc data event
                BluetoothGatt gattServer = mBluetoothLeService.getGattbyAddr(addr);
                if (gattServer != null) {
                    BluetoothDevice device = gattServer.getDevice();
                    SensorEvent e = new SensorEvent(SensorEvent.TYPE_ACCELEROMETER, device, 3);
                    e.values[0] = (float) values[0];
                    e.values[1] = (float) values[1];
                    e.values[2] = (float) values[2];
                    for (int i = 0, size = eventListeners.size(); i < size; i++) {
                        if (eventListeners.get(i).get(SensorEvent.TYPE_ACCELEROMETER) != null) {
                            SensorEventListener l = eventListeners.get(i).get(SensorEvent.TYPE_ACCELEROMETER);
                            l.onSensorChange(e);
                        }
                    }
                }
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(KoalaService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(KoalaService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(KoalaService.ACTION_GATT_RSSI);
        intentFilter.addAction(KoalaService.ACTION_GATT_SERVICES_DISCOVERED);
        //intentFilter.addAction(KoalaService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(KoalaService.ACTION_RAW_GYRO_DATA_AVAILABLE);
        intentFilter.addAction(KoalaService.ACTION_RAW_ACC_DATA_AVAILABLE);

        return intentFilter;
    }

    public KoalaServiceManager(Activity act) {
        this.mActivity = act;
        Log.i(TAG, "Starting Koala service!!");
        Intent gattServiceIntent = new Intent(this.mActivity, KoalaService.class);
        this.mActivity.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        this.mActivity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        Log.i(TAG, "Koala service started!!");
    }

    public void connect(final String addr) {
        Log.i(TAG, "Connect to device: " + addr);
        mBluetoothLeService.connect(addr);
    }

    public void disconnect() {
        mBluetoothLeService.disconnect();
    }

    public void close() {
        mBluetoothLeService.close();
    }

    public void registerSensorEventListener(SensorEventListener listener, final int type) {
        SparseArray<SensorEventListener> e = new SparseArray<SensorEventListener>();
        e.put(type, listener);
        this.eventListeners.add(e);
    }

    public void registerSensorEventListener(SensorEventListener listener, final int type, final int rate, final int acc_scale, final int gyro_scale) {
        SparseArray<SensorEventListener> e = new SparseArray<SensorEventListener>();
        e.put(type, listener);
        this.sensor_rate = rate;
        KoalaService.setSensorWriteRate(this.sensor_rate);
        this.acc_fsr = acc_scale;
        KoalaService.setAccFSR(this.acc_fsr);
        this.gyro_fsr = gyro_scale;
        KoalaService.setGyroFSR(this.gyro_fsr);
        this.eventListeners.add(e);
    }

    public void unRegisterSensorEventListener(SensorEventListener listener, final int type) {
        for (int i=0, size=this.eventListeners.size(); i<size; i++) {
            SensorEventListener l = this.eventListeners.get(i).get(type);
            if (l.equals(listener)) {
                this.eventListeners.remove(i);
                return;
            }
        }
    }

    private void startToReadData(final String addr) {
        new Thread() {
            public void run() {
                try {
                    sleep(1000);   // update every 500ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // we enable the raw data notification here
                mBluetoothLeService.enableMotionRawService(addr);

            };
        }.start();
    }

    private void stopReadingData(final String addr) {
        new Thread() {
            public void run() {
                try {
                    sleep(1000);   // update every 500ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // we enable the raw data notification here
                mBluetoothLeService.disableMotionRawService(addr);
            };
        }.start();
    }

    private void setSamplingRate(final String addr, final int write_rate) {
        new Thread() {
            public void run() {
                try {
                    sleep(300);   // update every 500ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBluetoothLeService.setMotionDataWriteRate(addr, write_rate);
            };
        }.start();
    }

    private void setAccFSR(final String addr, final int scale) {
        new Thread() {
            public void run() {
                try {
                    sleep(100);   // update every 500ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBluetoothLeService.setMotionAccelScale(addr, scale);
            };
        }.start();
    }

    private void setGyroFSR(final String addr, final int scale) {
        new Thread() {
            public void run() {
                try {
                    sleep(200);   // update every 500ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBluetoothLeService.setMotionGyroScale(addr, scale);
            };
        }.start();
    }

    private void startReadRssi(final String addr) {
        new Thread() {
            public void run() {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBluetoothLeService.readRssi(addr);
            };
        }.start();
    }

}
