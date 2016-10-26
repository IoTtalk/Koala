package cc.nctu1210.api.koala3x;

public interface SensorEventListener {
    public abstract void onSensorChange(SensorEvent e);
    public abstract void onConnectionStatusChange(boolean status);
    public abstract void onRSSIChange(String addr, float rssi);
}