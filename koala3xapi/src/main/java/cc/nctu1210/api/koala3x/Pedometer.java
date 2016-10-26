package cc.nctu1210.api.koala3x;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

import cc.nctu1210.api.koala3x.KoalaService;

/**
 * We need:
 * 1. query command definition.
 * 2. data parser implementation.
 * 3. SensorEventListener registration/unregistration implementation.
 * 4. SensorEvent firer implementation.
 * @author Yi-Ta_Chuang
 *
 */

public class Pedometer {
    private static final String TAG = Pedometer.class.getSimpleName();


    private BluetoothDevice device;
    private BluetoothGatt gattServer;
    private KoalaService mBluetoothLeService;

    /**
     * Data fields
     */
    private float[] pdrData = new float[5];
    private float[] accData = new float[3];


    public static final byte SET_TIME = 0x01;
    public static final byte SET_USER_INFO = 0x02;
    public static final byte SET_CLEAR_SPORT = 0x04;
    public static final byte SET_DEVICE_ID = 0x05;
    public static final byte GET_DAY_TOTAL_SPORT_INFORMATION = 0x07;
    public static final byte GET_DAY_TARGET_ACHIEVEMNET = 0x08;
    public static final byte SET_START_TIME_PEDOMETER = 0x09;
    public static final byte SET_STOP_TIME_PEDOMETER = 0x0A;
    public static final byte SET_TARGET_ACHIEVEMNET = 0x0B;
    public static final byte GET_TARGET_ACHIEVEMNET = 0x4B;
    public static final byte SET_FACTORY_CONFIG = 0x12;
    public static final byte GET_MAC_ADDRESS = 0x22;
    public static final byte GET_SOFTWARE_REVISION = 0x27;
    public static final byte SET_MCU_SOFT_RECOVERY = 0x2E;
    public static final byte GET_TIME = 0x41;
    public static final byte GET_USER_INFO = 0x42;
    public static final byte GET_DAY_SPORT_INFORMATION = 0x43;
    public static final byte GET_DAY_SPORT_DATA_DISTRIBUTION = 0x45;
    public static final byte SET_FIRMWARE_UPGRADE = 0x47;
    public static final byte GET_CURRENT_SPORT_INFORMATION = 0x48;
    public static final byte SET_START_SLEEP_MONITORING = 0x49;
    public static final byte GET_SLEEP_MONITOR_STATE = 0x6B;
    public static final byte GET_G_SENSOR_CONTROL = (byte) 0x98;
    public static final byte GET_G_SENSOR_RAW_DATA = (byte) 0x99;



    public Pedometer(KoalaService service) {
        this.mBluetoothLeService = service;
    }

    public void setBTDevice(BluetoothDevice d) {
        this.device = d;
    }

    public void setGattServer(BluetoothGatt gatt) {
        this.gattServer = gatt;
    }

    public boolean sendPedometerCommand(final Byte cmd) {
        BluetoothGatt tmpGatt = this.gattServer;

        if (tmpGatt==null) {
            Log.w(TAG, "GATT server is not set!!");
            return false;
        }

        BluetoothGattService pedometerService = tmpGatt.getService(KoalaService.UUID_KOALA_PEDOMETER_SERVICE);
        if (pedometerService == null) {
            Log.w(TAG, "pedometerService service not found!");
            return false;
        }
        BluetoothGattCharacteristic paramCharacteristic =
                pedometerService.getCharacteristic(KoalaService.UUID_KOALA_PEDOMETER_PARAM_CHANGE_CHARACTERISTIC);
        if (paramCharacteristic == null) {
            Log.w(TAG, "param change characteristic not found!");
            return false;
        }
        byte[] command = new byte[16];

        command = commandConstruction(cmd);
        paramCharacteristic.setValue(command);
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        tmpGatt.writeCharacteristic(paramCharacteristic);

        return true;
    }

    private byte[] commandConstruction(final Byte cmd) {
        byte[] command = new byte[16];
        command[0] = (byte) (cmd.byteValue() & 0xFF);
        int crc = command[0];
        int index = 0;

        switch (cmd.byteValue()) {
            case SET_TIME:
            case SET_CLEAR_SPORT:
            case SET_DEVICE_ID:
            case GET_DAY_TOTAL_SPORT_INFORMATION:
            case SET_TARGET_ACHIEVEMNET:
            case GET_TARGET_ACHIEVEMNET:
            case GET_MAC_ADDRESS:
            case GET_SOFTWARE_REVISION:
            case SET_MCU_SOFT_RECOVERY:
            case GET_TIME:
            case GET_USER_INFO:
            case GET_DAY_SPORT_INFORMATION:
            case GET_DAY_SPORT_DATA_DISTRIBUTION:
            case GET_CURRENT_SPORT_INFORMATION:
            case SET_START_SLEEP_MONITORING:
            case GET_SLEEP_MONITOR_STATE:
                Log.e(TAG, "not support yet!!");
                break;
            case GET_G_SENSOR_CONTROL:
                Log.d(TAG, "GET_G_SENSOR_CONTROL!!");
                command[1] = 0x01;
                command[2] = 0x01;
                command[3] = 0x07;
                while (true) {
                    if (index >= 15) {
                        break;
                    }
                    if (index >=3 ) {
                        command[index + 1] = 0;
                    }
                    crc += command[index + 1];
                    ++index;
                }
                break;
            case GET_DAY_TARGET_ACHIEVEMNET:
                Log.d(TAG, "GET_DAY_TARGET_ACHIEVEMNET!!");
                while (true) {
                    if (index >= 15) {
                        break;
                    }
                    command[index + 1] = 0;
                    crc += command[index + 1];
                    ++index;
                }
                break;
            case SET_FIRMWARE_UPGRADE:
                Log.d(TAG, "SET_FIRMWARE_UPGRADE!!");
                while (true) {
                    if (index >= 15) {
                        break;
                    }
                    command[index + 1] = 0;
                    crc += command[index + 1];
                    ++index;
                }
                break;
            case GET_G_SENSOR_RAW_DATA:
                Log.d(TAG, "GET_G_SENSOR_RAW_DATA");
                while (true) {
                    if (index >= 15) {
                        break;
                    }
                    command[index + 1] = 0;
                    crc += command[index + 1];
                    ++index;
                }
                break;
            case SET_FACTORY_CONFIG:
                Log.d(TAG, "SET_CLEAR_SPORT!!");
                while (true) {
                    if (index >= 15) {
                        break;
                    }
                    command[index + 1] = 0;
                    crc += command[index + 1];
                    ++index;
                }
                break;
            case SET_START_TIME_PEDOMETER:
                Log.d(TAG, "SET_START_TIME_PEDOMETER!!");
                index = 0;
                while (true) {
                    if (index >= 15) {
                        break;
                    }
                    command[index + 1] = 0;
                    crc += command[index + 1];
                    ++index;
                }
                break;
            case SET_STOP_TIME_PEDOMETER:
                break;
            default:
                Log.e(TAG, "unrecognized command!!");
        }

        command[15] = (byte) (crc & 0xFF);

        Log.d(TAG, "command:"+(byte)(command[0])+":"+(command[1])+":"+(command[2])+":"+(command[3])+":"+
                (command[4])+":"+(command[5])+":"+(command[6])+":"+(command[7])+":"+
                (command[8])+":"+(command[9])+":"+(command[10])+":"+(command[11])+":"+
                (command[12])+":"+(command[13])+":"+(command[14])+":"+(command[15]));
        return command;
    }

    public void fireSensorEvent(byte[] data) {
        int dataLength = data.length;
        int crcCheck=0;
        Intent intent;
        SensorEvent e;

        byte command = (byte) (data[0] & 0xFF);
        for (int i=0; i<dataLength; i++) {
            if (i<dataLength-1) {
                crcCheck += (byte)data[i];
            }
        }
        if (dataLength>=16 && ((0xFF & data[dataLength-1]) == (crcCheck & 0xFF))) {
            Log.d(TAG, "recv command:"+(byte)(data[0])+":"+(data[1])+":"+(data[2])+":"+(data[3])+":"+
                    (data[4])+":"+(data[5])+":"+(data[6])+":"+(data[7])+":"+
                    (data[8])+":"+(data[9])+":"+(data[10])+":"+(data[11])+":"+
                    (data[12])+":"+(data[13])+":"+(data[14])+":"+(data[15]));
            switch (command) {
                case SET_TIME:
                case SET_CLEAR_SPORT:
                case SET_DEVICE_ID:
                case GET_DAY_TOTAL_SPORT_INFORMATION:
                case SET_TARGET_ACHIEVEMNET:
                case GET_TARGET_ACHIEVEMNET:
                case SET_FACTORY_CONFIG:
                case GET_MAC_ADDRESS:
                case GET_SOFTWARE_REVISION:
                case SET_MCU_SOFT_RECOVERY:
                case GET_TIME:
                case GET_USER_INFO:
                case GET_DAY_SPORT_INFORMATION:
                case GET_DAY_SPORT_DATA_DISTRIBUTION:
                case GET_CURRENT_SPORT_INFORMATION:
                case SET_START_SLEEP_MONITORING:
                case GET_SLEEP_MONITOR_STATE:
                    Log.e(TAG, "not support yet!!");
                    break;
                case GET_G_SENSOR_CONTROL:
                    Log.d(TAG, "recv a GET_G_SENSOR_CONTROL event!!");
                    byte b_accel_z = data[4];
                    float acc_z = (float) (b_accel_z * 4.0d/Math.pow(2,8));
                    Log.d(TAG, "time="+System.currentTimeMillis()+" gZ:"+acc_z+"\n");
                    break;
                case GET_DAY_TARGET_ACHIEVEMNET:
                    Log.d(TAG, "recv a GET_DAY_TARGET_ACHIEVEMNET event!!");
                    break;
                case SET_FIRMWARE_UPGRADE:
                    Log.d(TAG, "recv a SET_FIRMWARE_UPGRADE event!!");
                    break;
                case GET_G_SENSOR_RAW_DATA:
                    Log.d(TAG, "recv a GET_G_SENSOR_RAW_DATA event!!");


                    short s_accel_x = (short)( (short)data[4]) ;
                    short s_accel_y = (short)( (short)data[8]) ;
                    short s_accel_z = (short)( (short)data[12]) ;

                    //short s_accel_x = (short)( (short)data[4] *256  + (short)data[3]) ;
                    //short s_accel_y = (short)( (short)data[8] *256  + (short)data[7]) ;
                    //short s_accel_z = (short)( (short)data[12]*256  + (short)data[11]) ;

                    double accel_x = (double) (s_accel_x * 4.0d / Math.pow(2,8)) ; // +-2g
                    double accel_y = (double) (s_accel_y * 4.0d/ Math.pow(2,8)) ;
                    double accel_z = (double) (s_accel_z * 4.0d / Math.pow(2,8)) ;

                    accData[0] = (float) accel_x;
                    accData[1] = (float) accel_y;
                    accData[2] = (float) accel_z;
                    //fire sensor event
                    //e = new SensorEvent(SensorEvent.TYPE_ACCEROMETER, device, 3);
                    intent = new Intent(KoalaService.ACTION_RAW_ACC_DATA_AVAILABLE);
                    intent.putExtra(KoalaService.EXTRA_NAME, String.valueOf(device.getAddress()));
                    intent.putExtra(KoalaService.EXTRA_DATA, accData);
                    mBluetoothLeService.sendBroadcast(intent);
                    /*
                    for (int i=0, size=eventListeners.size(); i<size; i++) {
                        if (eventListeners.get(i).get(SensorEvent.TYPE_ACCEROMETER) != null) {
                            SensorEventListener l = eventListeners.get(i).get(SensorEvent.TYPE_ACCEROMETER);
                            e.values[0] = accData[0];
                            e.values[1] = accData[1];
                            e.values[2] = accData[2];
                            l.onSensorChange(e);
                            Log.d(TAG, "time="+System.currentTimeMillis()+ "gX:" + e.values[0]+"gY:" + e.values[1]+"gZ:" + e.values[2]+"\n");
                        }
                    }*/
                    sendPedometerCommand(GET_G_SENSOR_RAW_DATA);
                    break;
                case SET_START_TIME_PEDOMETER:
                    //step count data
                    Log.d(TAG, "fire a SET_START_TIME_PEDOMETER event!!");
                    String dataStr;
                    if (((data[1] >> 7) & 0x1) == 1) {
                        pdrData[0]=0;
                    } else {
                        dataStr = String.valueOf(256 * (256 * (0xFF & data[1]))
                                + 256 * (0xFF & data[2])
                                + (0xFF & data[3]));
                        pdrData[0] = Float.valueOf(dataStr);
                    }
                    //calorie data
                    dataStr = String.valueOf(256 * (256 * (0xFF & data[7]))
                            + 256 * (0xFF & data[8]) + (0xFF & data[9]));
                    pdrData[1] = Float.valueOf(dataStr).floatValue() / 100.0F;
                    //moving distance data
                    dataStr = String.valueOf(256 * (256 * (0xFF & data[10])) + 256
                            * (0xFF & data[11])
                            + (0xFF & data[12]));
                    pdrData[2] = Float.valueOf(dataStr).floatValue() / 100.0F;
                    //moving time data
                    dataStr = String.valueOf(256 * (0xFF & data[13])
                            + (0xFF & data[14]));
                    if (dataStr != null) {
                        pdrData[3] = Float.valueOf(dataStr);
                    }
                    //heart rate data
                    if (((data[1] >> 7) & 0x1) == 1) {
                        dataStr = String.valueOf(+256 * (0xFF & data[2])
                                + (0xFF & data[3]));
                        pdrData[4] = Float.valueOf(dataStr);
                    }
                    else
                        pdrData[4]=0.0F;
                    //fire sensor event
                    //e = new SensorEvent(SensorEvent.TYPE_PEDOMETER, device, 5);
                    intent = new Intent(KoalaService.ACTION_PDR_DATA_AVAILABLE);
                    intent.putExtra(KoalaService.EXTRA_NAME, String.valueOf(device.getAddress()));
                    intent.putExtra(KoalaService.EXTRA_DATA, pdrData);
                    mBluetoothLeService.sendBroadcast(intent);
                    /*
                    for (int i=0, size=eventListeners.size(); i<size; i++) {
                        if (eventListeners.get(i).get(SensorEvent.TYPE_PEDOMETER) != null) {
                            SensorEventListener l = eventListeners.get(i).get(SensorEvent.TYPE_PEDOMETER);
                            e.values[0] = pdrData[0];
                            e.values[1] = pdrData[1];
                            e.values[2] = pdrData[2];
                            e.values[3] = pdrData[3];
                            e.values[4] = pdrData[4];
                            l.onSensorChange(e);
                            Log.d(TAG, "time="+System.currentTimeMillis()+ "step counts:" + e.values[0]+"\n");
                        }
                    }*/
                    break;
            }

        }
    }
}