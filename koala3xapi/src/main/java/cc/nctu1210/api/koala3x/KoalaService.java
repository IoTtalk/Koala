package cc.nctu1210.api.koala3x;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cc.nctu1210.api.koala3x.Pedometer;
import cc.nctu1210.api.koala3x.SensorEvent;
import cc.nctu1210.api.koala3x.SensorEventListener;

public class KoalaService extends Service {
    private final static String TAG = KoalaService.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private List<Map<String, BluetoothGatt>> mBluetoothGattList = new ArrayList<Map<String, BluetoothGatt>>();
    private final Map<String, Pedometer> pedometers = new HashMap<String, Pedometer>();


    /**
     * Provided actions
     */
    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_GATT_RSSI = "ACTION_GATT_RSSI";
    public final static String ACTION_RAW_ACC_DATA_AVAILABLE = "ACTION_RAW_ACC_DATA_AVAILABLE";
    public final static String ACTION_PDR_DATA_AVAILABLE = "ACTION_PDR_DATA_AVAILABLE";
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";


    public final static String EXTRA_DATA = "EXTRA_DATA";
    public final static String EXTRA_NAME = "EXTRA_NAME";



    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); //CLIENT_CHARACTERISTIC_CONFIG
    public static final UUID UUID_KOALA_PEDOMETER_SERVICE = UUID.fromString(Koala3xGattAttributes.KOALA_PEDOMETER_SERVICE_UUID);
    public static final UUID UUID_KOALA_PEDOMETER_NOTIFICATION_CHARACTERISTIC = UUID.fromString(Koala3xGattAttributes.KOALA_PEDOMETER_NOTIFICATION_CHARACTERISTIC_UUID);
    public static final UUID UUID_KOALA_PEDOMETER_PARAM_CHANGE_CHARACTERISTIC = UUID.fromString(Koala3xGattAttributes.KOALA_PEDOMETER_PARAM_CHANGE_CHARACTERISTIC_UUID);
    //public static final UUID UUID_KOALA_MOTNIO_SERVICE = UUID.fromString("eb371600-347c-fe94-1600-8295a1e42b09"); // not support yet
    //public static final UUID UUID_KOALA_MOTION_MEASUREMENT_CHARACTERISTIC = UUID.fromString("eb371601-347c-fe94-1600-8295a1e42b09"); //not support yet

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular
        // example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();



    public class LocalBinder extends Binder {
        public KoalaService getService() {
            return KoalaService.this;
        }
    }

    /**
     * The callback function to handle the connection stage of BLE
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            String intentAction;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                String addr = gatt.getDevice().getAddress();
                broadcastUpdate(intentAction, addr);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:"
                        + gatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                String addr = gatt.getDevice().getAddress();
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction, addr);
            }
        }

        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String addr = gatt.getDevice().getAddress();
                broadcastUpdate(ACTION_GATT_RSSI, addr, rssi);
            } else {
                Log.w(TAG, "onReadRemoteRssi received: " + status);
            }
        };

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String addr = gatt.getDevice().getAddress();
                Pedometer pdr = new Pedometer(KoalaService.this);
                pdr.setBTDevice(gatt.getDevice());
                pdr.setGattServer(gatt);
                pedometers.put(addr, pdr);
                enableNotificationService(addr);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, addr);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String addr = gatt.getDevice().getAddress();
                broadcastUpdate(ACTION_DATA_AVAILABLE, addr, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            String addr = gatt.getDevice().getAddress();
            broadcastUpdate(ACTION_DATA_AVAILABLE, addr, characteristic);
        }
    };

    private void broadcastUpdate(final String action, final String addr) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_NAME, String.valueOf(addr));
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String addr, int rssi) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_NAME, String.valueOf(addr));
        intent.putExtra(EXTRA_DATA, String.valueOf(rssi));
        sendBroadcast(intent);
    }

    /**
     * Fire sensor event.
     * @param action
     * @param addr
     * @param characteristic
     */
    private void broadcastUpdate(final String action,
                                 final String addr,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_NAME, String.valueOf(addr));
        // This is special handling for the Motion Raw Data Measurement profile. Data
        if (UUID_KOALA_PEDOMETER_NOTIFICATION_CHARACTERISTIC.equals(characteristic.getUuid())) {
            final byte[] rx = characteristic.getValue();
            Pedometer pdr = pedometers.get(addr);
            if (pdr == null) {
                Log.e(TAG, "Pedometer can not be found!!");
            }
            pdr.fireSensorEvent(rx);
            return;
        }

        sendBroadcast(intent);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     *
     *
     *  Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address
     *            The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The
     *         connection result is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG,
                    "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (!this.mBluetoothGattList.isEmpty()) {
            for (int i=0; i<this.mBluetoothGattList.size(); i++) {
                if (this.mBluetoothGattList.get(i).containsKey(address)) {
                    BluetoothGatt tmpGatt = this.mBluetoothGattList.get(i).get(address);
                    Log.d(TAG,
                            "Trying to use an existing mBluetoothGatt for connection.");
                    if (tmpGatt.connect()) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        Map<String, BluetoothGatt> map = new HashMap<String, BluetoothGatt>();
        String macAddr = address;
        BluetoothGatt tmpGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        map.put(macAddr, tmpGatt);
        this.mBluetoothGattList.add(map);

        return true;
    }

    /**
     * Get Gatt server by id
     */
    protected BluetoothGatt getGattbyAddr(String addr) {
        for (int i=0; i<this.mBluetoothGattList.size(); i++) {
            if (this.mBluetoothGattList.get(i).containsKey(addr)) {
                return this.mBluetoothGattList.get(i).get(addr);
            }

        }
        return null;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || this.mBluetoothGattList.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        for (int i=0; i<this.mBluetoothGattList.size(); i++) {
            BluetoothGatt tmpGatt =  this.mBluetoothGattList.get(i).values().iterator().next();
            String addr = tmpGatt.getDevice().getAddress();
            disableNotificationService(addr);
            tmpGatt.disconnect();
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        if (this.mBluetoothGattList.isEmpty()) {
            return;
        }
        for (int i=0; i<this.mBluetoothGattList.size(); i++) {
            BluetoothGatt tmpGatt =  this.mBluetoothGattList.get(i).values().iterator().next();
            tmpGatt.close();
            tmpGatt = null;
        }
        this.mBluetoothGattList.clear();
    }

    public boolean readRssi(String addr) {
        if (mBluetoothAdapter == null || this.mBluetoothGattList.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        BluetoothGatt tmpGatt = getGattbyAddr(addr);
        tmpGatt.readRemoteRssi();
        return true;
    }

    /**
     * We need to enable notification service first before doing any commands to the Koala sensor.
     */
    private boolean enableNotificationService(String addr) {
        BluetoothGatt tmpGatt = getGattbyAddr(addr);
        BluetoothGattService pedometerService = tmpGatt.getService(UUID_KOALA_PEDOMETER_SERVICE);
        if (pedometerService == null) {
            Log.w(TAG, "pedometerService service not found!");
            return false;
        }
        BluetoothGattCharacteristic notificationCharacteristic =
                pedometerService.getCharacteristic(UUID_KOALA_PEDOMETER_NOTIFICATION_CHARACTERISTIC);
        if (notificationCharacteristic == null) {
            Log.w(TAG, "nortification characteristic not found!");
            return false;
        }

        tmpGatt.setCharacteristicNotification(notificationCharacteristic,true);

        BluetoothGattDescriptor descriptor = notificationCharacteristic.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        tmpGatt.writeDescriptor(descriptor);
        notificationCharacteristic.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        return true;
    }

    /**
     *  Disable notification service if the Koala sensor is disconnected.
     *
     */
    private boolean disableNotificationService(String addr){
        BluetoothGatt tmpGatt = getGattbyAddr(addr);
        BluetoothGattService pedometerService = tmpGatt.getService(UUID_KOALA_PEDOMETER_SERVICE);
        if (pedometerService == null) {
            Log.w(TAG, "pedometerService service not found!");
            return false;
        }
        BluetoothGattCharacteristic notificationCharacteristic =
                pedometerService.getCharacteristic(UUID_KOALA_PEDOMETER_NOTIFICATION_CHARACTERISTIC);
        if (notificationCharacteristic == null) {
            Log.w(TAG, "nortification characteristic not found!");
            return false;
        }

        tmpGatt.setCharacteristicNotification(notificationCharacteristic,false);

        BluetoothGattDescriptor descriptor = notificationCharacteristic.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        tmpGatt.writeDescriptor(descriptor);
        notificationCharacteristic.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        return true;
    }



    /**
     *
     * @param addr
     * @return
     */
    public boolean resetPedometer(String addr) {
        Pedometer tmpPDR = this.pedometers.get(addr);
        if (tmpPDR == null) {
            Log.e(TAG, "Pedometer can not be found!!");
            return false;
        }
        tmpPDR.sendPedometerCommand(Pedometer.SET_FACTORY_CONFIG);
        return true;
    }

    /**
     *
     * @param addr
     * @return return true if the operation is successful.
     */
    public boolean enablePedometerService(String addr) {
        Pedometer tmpPDR = this.pedometers.get(addr);
        if (tmpPDR == null) {
            Log.e(TAG, "Pedometer can not be found!!");
            return false;
        }
        tmpPDR.sendPedometerCommand(Pedometer.SET_START_TIME_PEDOMETER);
        return true;
    }

    public boolean disablePedometerService(String addr) {
        Pedometer tmpPDR = this.pedometers.get(addr);
        if (tmpPDR == null) {
            Log.e(TAG, "Pedometer can not be found!!");
            return false;
        }
        tmpPDR.sendPedometerCommand(Pedometer.SET_STOP_TIME_PEDOMETER);
        return true;
    }

    public boolean setToFactoryMode(String addr) {
        Pedometer tmpPDR = this.pedometers.get(addr);
        if (tmpPDR == null) {
            Log.e(TAG, "Pedometer can not be found!!");
            return false;
        }
        tmpPDR.sendPedometerCommand(Pedometer.SET_FIRMWARE_UPGRADE);
        return true;
    }

    public boolean setMotionParameter(String addr) {
        Pedometer tmpPDR = this.pedometers.get(addr);
        if (tmpPDR == null) {
            Log.e(TAG, "Pedometer can not be found!!");
            return false;
        }
        tmpPDR.sendPedometerCommand(Pedometer.GET_G_SENSOR_CONTROL);
        return true;
    }

    public boolean getSportInformation(String addr) {
        Pedometer tmpPDR = this.pedometers.get(addr);
        if (tmpPDR == null) {
            Log.e(TAG, "Pedometer can not be found!!");
            return false;
        }
        tmpPDR.sendPedometerCommand(Pedometer.GET_DAY_TARGET_ACHIEVEMNET);
        return true;
    }

    /**
     *
     * @param addr
     * @return return true if the operation is successful.
     */
    public boolean enableMotionRawService(String addr) {
        //Log.w(TAG, "not support yet!!");
        Pedometer tmpPDR = this.pedometers.get(addr);
        if (tmpPDR == null) {
            Log.e(TAG, "Pedometer can not be found!!");
            return false;
        }
        tmpPDR.sendPedometerCommand(Pedometer.GET_G_SENSOR_RAW_DATA);
        return true;
    }

    public boolean disableMotionRawService(String addr){
        Log.w(TAG, "not support yet!!");
        return false;
    }
}
