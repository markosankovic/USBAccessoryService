package com.sankovicmarko.android.usbaccessoryservice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;

public class UsbAccessoryActivity extends Activity {

    private static final String TAG = "UsbAccessoryActivity";

    private Thread mUsbAccessoryThread;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbAccessoryService.ACTION_RUNNING.equals(action)) {
                Intent dashboardActivityIntent = new Intent(UsbAccessoryActivity.this, DashboardActivity.class);
                startActivity(dashboardActivityIntent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(UsbAccessoryService.ACTION_RUNNING));

        UsbAccessory accessory = getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        if (accessory != null) {
            startUsbAccessoryService(accessory);
        }

        setContentView(R.layout.activity_usb_accessory);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mUsbAccessoryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        /**
                         * Wait some time before checking if USB accessory is attached.
                         * This is because when UsbAccessoryService "read thread" sends broadcast that its stopped,
                         * input and output streams are still not closed on an accessory,
                         * so the usbManager here lists accessory that is about to be removed.
                         */
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        break;
                    }

                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

                    UsbAccessory[] accessories = usbManager.getAccessoryList();
                    UsbAccessory accessory = (accessories == null ? null : accessories[0]);
                    if (accessory != null) {
                        startUsbAccessoryService(accessory);
                        break;
                    }
                }
            }
        });

        mUsbAccessoryThread.start();
    }

    @Override
    protected void onPause() {
        if (mUsbAccessoryThread != null) {
            mUsbAccessoryThread.interrupt();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.usb_accessory, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void startUsbAccessoryService(UsbAccessory accessory) {
        Intent usbAccessoryServiceIntent = new Intent(UsbAccessoryActivity.this, UsbAccessoryService.class);
        usbAccessoryServiceIntent.putExtra(UsbManager.EXTRA_ACCESSORY, accessory);
        startService(usbAccessoryServiceIntent);
    }
}
