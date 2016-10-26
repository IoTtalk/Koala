package cc.nctu1210.api.koala6x;

public interface SensorEventListener {
    public void onSensorChange(SensorEvent e);
    public void onConnectionStatusChange(boolean status);
    public void onRSSIChange(String addr, float rssi);
}