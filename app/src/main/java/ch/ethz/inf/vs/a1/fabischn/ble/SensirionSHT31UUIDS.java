package ch.ethz.inf.vs.a1.fabischn.ble;

import java.util.UUID;

public final class SensirionSHT31UUIDS {
    protected static UUID UUID_HUMIDITY_SERVICE = UUID.fromString("00001234-b38d-4985-720e-0f993a68ee41");
    protected static UUID UUID_HUMIDITY_CHARACTERISTIC = UUID.fromString("00001235-b38d-4985-720e-0f993a68ee41");
    protected static UUID UUID_TEMPERATURE_SERVICE = UUID.fromString("00002234-b38d-4985-720e-0f993a68ee41");
    protected static UUID UUID_TEMPERATURE_CHARACTERISTIC = UUID.fromString("00002235-b38d-4985-720e-0f993a68ee41");
    protected static UUID NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
}
