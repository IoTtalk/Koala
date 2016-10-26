package cc.nctu1210.api.koala3x;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for
 * demonstration purposes.
 */

public class Koala3xGattAttributes {
    private static HashMap<String, String> attributes = new HashMap<String, String>();

    public static String KOALA_PEDOMETER_SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static String KOALA_PEDOMETER_NOTIFICATION_CHARACTERISTIC_UUID = "0000FFF7-0000-1000-8000-00805F9B34FB";
    public static String KOALA_PEDOMETER_PARAM_CHANGE_CHARACTERISTIC_UUID = "0000FFF6-0000-1000-8000-00805F9B34FB";

    /**
     *  Currently, only pedometer related functions are available.
     */
    //public static String KOALA_MOTNIO_SERVICE_UUID                      = "eb371600-347c-fe94-1600-8295a1e42b09";
    //public static String KOALA_MOTION_MEASUREMENT_CHARACTERISTIC_UUID   = "eb371601-347c-fe94-1600-8295a1e42b09";
    //public static String KOALA_MOTION_PARAM_CHANGE_CHARACTERISTIC_UUID  = "eb371602-347c-fe94-1600-8295a1e42b09";

    static {
        //Koala pedometer services.
        attributes.put(KOALA_PEDOMETER_SERVICE_UUID, "Pedometer Services");
        //Koala pedometer services characteristics.
        attributes.put(KOALA_PEDOMETER_NOTIFICATION_CHARACTERISTIC_UUID, "Pedometer Data Notification");
        attributes.put(KOALA_PEDOMETER_PARAM_CHANGE_CHARACTERISTIC_UUID, "Pedoneter Params Configuration");

        // Koala Raw Data (Motion) Services.
        //attributes.put(KOALA_MOTNIO_SERVICE_UUID,
        //		"Motion Sensor Raw Data Service");
        // Koala Raw Data (Motion)  Characteristics.
        //attributes.put(KOALA_MOTION_MEASUREMENT_CHARACTERISTIC_UUID, "Motion Data Notification");
        //attributes.put(KOALA_MOTION_PARAM_CHANGE_CHARACTERISTIC_UUID, "Motion Param Configuration");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}