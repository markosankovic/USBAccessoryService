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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class UsbAccessoryActivity extends Activity {

    private static final String TAG = "UsbAccessoryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(UsbAccessoryService.ACTION_RUNNING));

        UsbAccessory accessory = getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        if (accessory != null) {
            Log.d(TAG, "This is the accessory onCreate UsbAccessoryActivity: " + accessory.toString());
            Intent usbAccessoryServiceIntent = new Intent(this, UsbAccessoryService.class);
            usbAccessoryServiceIntent.putExtra(UsbManager.EXTRA_ACCESSORY, accessory);
            startService(usbAccessoryServiceIntent);
        }

        setContentView(R.layout.activity_usb_accessory);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbAccessoryService.ACTION_RUNNING.equals(action)) {
                Log.d(TAG, "ACTION_RUNNING got to DashboardActivity");
                Intent dashboardActivityIntent = new Intent(UsbAccessoryActivity.this, DashboardActivity.class);
                startActivity(dashboardActivityIntent);
            }
        }
    };

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
}
