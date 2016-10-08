package ch.ethz.inf.vs.a1.fabischn.ble;

import java.util.HashMap;
import java.util.UUID;

// Extended this with code from Google's BLE sample

public final class SensirionSHT31UUIDS {

    private static String S_UUID_HUMIDITY_SERVICE = "00001234-b38d-4985-720e-0f993a68ee41";
    private static String S_UUID_HUMIDITY_CHARACTERISTIC = "00001235-b38d-4985-720e-0f993a68ee41";
    private static String S_UUID_TEMPERATURE_SERVICE = "00002234-b38d-4985-720e-0f993a68ee41";
    private static String S_UUID_TEMPERATURE_CHARACTERISTIC = "00002235-b38d-4985-720e-0f993a68ee41";
    private static String S_NOTIFICATION_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";


    protected static UUID UUID_HUMIDITY_SERVICE = UUID.fromString(S_UUID_HUMIDITY_SERVICE);
    protected static UUID UUID_HUMIDITY_CHARACTERISTIC = UUID.fromString(S_UUID_HUMIDITY_CHARACTERISTIC);
    protected static UUID UUID_TEMPERATURE_SERVICE = UUID.fromString(S_UUID_TEMPERATURE_SERVICE);
    protected static UUID UUID_TEMPERATURE_CHARACTERISTIC = UUID.fromString(S_UUID_TEMPERATURE_CHARACTERISTIC);
    protected static UUID NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString(S_NOTIFICATION_DESCRIPTOR_UUID);


    private static HashMap<String, String> attributes = new HashMap();

    static{
        // SHT31 Services.
        attributes.put(S_UUID_HUMIDITY_SERVICE,"SHT31 Humidity Service");
        attributes.put(S_UUID_TEMPERATURE_SERVICE,"SHT31 Temperature Service");

        // SHT31 Characteristics.
        attributes.put(S_UUID_HUMIDITY_CHARACTERISTIC,"SHT31 Humidity Characteristic");
        attributes.put(S_UUID_TEMPERATURE_CHARACTERISTIC,"SHT31 Temperature Characteristic");

        // SHT31 Notification Descriptor
        attributes.put(S_NOTIFICATION_DESCRIPTOR_UUID, "SHT31 Notification Descriptor");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}



