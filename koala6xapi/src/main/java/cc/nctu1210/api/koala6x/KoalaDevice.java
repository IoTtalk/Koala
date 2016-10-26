package cc.nctu1210.api.koala6x;

import android.bluetooth.BluetoothDevice;

public class KoalaDevice {
    private BluetoothDevice mDevice = null;
    private int rssi;
    private byte[] scanRecord;
    private int numRevItems;
    private long startTime;

    public KoalaDevice(BluetoothDevice mDevice, int rssi, byte[] scanRecord) {
        this.mDevice = mDevice;
        this.rssi = rssi;
        this.scanRecord = scanRecord;
        this.numRevItems = 0;
    }

    public void setConnectedTime() {
        this.startTime = System.currentTimeMillis();
    }

    public BluetoothDevice getDevice() {
        return this.mDevice;
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

    public void resetSamplingRate() {
        this.numRevItems = 0;
    }

    public float getCurrentSamplingRate() {
        return (float) this.numRevItems / ((System.currentTimeMillis() - this.startTime)/1000);
    }
}
