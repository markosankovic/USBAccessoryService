package com.sankovicmarko.android.usbaccessoryservice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.sankovicmarko.android.usbaccessoryservice.UsbAccessoryService.LocalBinder;

public abstract class UsbAccessoryBaseActivity extends Activity {

    private static final String TAG = "UsbAccessoryBaseActivity";

    private UsbAccessoryService mService;
    private OBC obc;
    private boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Listen for USB Accessory Service stop
        LocalBroadcastManager.getInstance(this).registerReceiver(mUsbAccessoryBaseMessageReceiver,
                new IntentFilter(UsbAccessoryService.ACTION_STOPPED));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mUsbAccessoryBaseMessageReceiver);
        } catch (IllegalArgumentException e) {
            // FIXME Find cases in which mUsbAccessoryBaseMessageReceiver is not registered in onCreate
            // Ignore if mUsbAccessoryBaseMessageReceiver is already unregistered or not registered at all
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, UsbAccessoryService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.unbindFromService();
    }

    private void unbindFromService() {
        if (mBound) {
            unbindService(mConnection);
            obc = null;
            mBound = false;
        }
    }

    // When USB Accessory Service is stopped (currently only if read thread has ended,
    // in most cases when USB accessory is detached) return to the USB Accessory Activity as
    // there is no reason for a user to use this application if there is no connection to the OBC.
    private BroadcastReceiver mUsbAccessoryBaseMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbAccessoryService.ACTION_STOPPED.equals(action)) {
                unbindFromService();
                Intent usbAccessoryActivity = new Intent(UsbAccessoryBaseActivity.this, UsbAccessoryActivity.class);
                startActivity(usbAccessoryActivity);
            }
        }
    };

    // Defines callbacks for service binding, passed to bindService()
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) iBinder;
            obc = mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            obc = null;
            mBound = false;
        }
    };

    /**
     * USBAccessoryService implements OBC.
     * <p/>
     * Only the OBC public methods are exposed to activities extending this class.
     * Activities are unaware that they communicate with USBAccessoryService.
     *
     * @return
     */
    public OBC getOBC() {
        return obc;
    }

    /**
     * Tests can use this method to inject OBC mock.
     *
     * @param obc
     */
    public void setOBC(OBC obc) {
        this.obc = obc;
    }
}
