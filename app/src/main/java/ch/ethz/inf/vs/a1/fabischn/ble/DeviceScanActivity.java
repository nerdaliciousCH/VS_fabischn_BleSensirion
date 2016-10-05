package ch.ethz.inf.vs.a1.fabischn.ble;


import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;

public class DeviceScanActivity extends ListActivity{

    // GOOGLE SAMPLE CODE...Continue
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }
}