package cc.nctu1210.api.koala3x;

import android.bluetooth.BluetoothDevice;

public class KoalaDevice {
    private BluetoothDevice mDevices = null;
    private int rssi;
    private byte[] scanRecord;
    private int numRevItems;
    private long startTime;

    public KoalaDevice(BluetoothDevice mDevices, int rssi, byte[] scanRecord) {
        this.mDevices = mDevices;
        this.rssi = rssi;
        this.scanRecord = scanRecord;
        this.numRevItems = 0;
    }

    public void setConnectedTime() {
        this.startTime = System.currentTimeMillis();
    }

    public BluetoothDevice getDevice() {
        return this.mDevices;
    }

    public int getRssi(){
        return this.rssi;
    }

    public void setRssi(int r){
        this.rssi= r;
    }

    public byte[] getScanRecordData() {
        return  this.scanRecord;
    }

    public void addRecvItem() {
        this.numRevItems++;
    }

    public float getCurrentSamplingRate() {
        return (float) this.numRevItems / ((System.currentTimeMillis() - this.startTime)/1000);
    }
}
