package cc.nctu1210.api.koala6x;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for
 * demonstration purposes.
 */

public class Koala6xGattAttributes {
    private static HashMap<String, String> attributes = new HashMap<String, String>();

    public static String KOALA_MOTNIO_SERVICE_UUID                      = "eb371600-347c-fe94-1600-8295a1e42b09";
    public static String KOALA_MOTION_MEASUREMENT_CHARACTERISTIC_UUID   = "eb371601-347c-fe94-1600-8295a1e42b09";
    public static String KOALA_MOTION_RATE_CHANGE_CHARACTERISTIC_UUID  = "eb371603-347c-fe94-1600-8295a1e42b09";
    public static String KOALA_MOTION_ACC_FSR_CHANGE_CHARACTERISTIC_UUID  = "eb371604-347c-fe94-1600-8295a1e42b09";
    public static String KOALA_MOTION_GYRO_FSR_CHANGE_CHARACTERISTIC_UUID  = "eb371605-347c-fe94-1600-8295a1e42b09";

    static {
        // Koala Raw Data (Motion) Services.
        attributes.put(KOALA_MOTNIO_SERVICE_UUID,
                "Motion Sensor Raw Data Service");
        // Koala Raw Data (Motion)  Characteristics.
        attributes.put(KOALA_MOTION_MEASUREMENT_CHARACTERISTIC_UUID, "Motion Data Notification");
        attributes.put(KOALA_MOTION_RATE_CHANGE_CHARACTERISTIC_UUID, "Motion Rate Configuration");
        attributes.put(KOALA_MOTION_ACC_FSR_CHANGE_CHARACTERISTIC_UUID, "Motion ACC FSR Configuration");
        attributes.put(KOALA_MOTION_GYRO_FSR_CHANGE_CHARACTERISTIC_UUID, "Motion GYRO FSR Configuration");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}