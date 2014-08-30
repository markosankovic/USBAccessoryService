package com.sankovicmarko.android.usbaccessoryservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class UsbAccessoryService extends Service implements Runnable {

    private static final String TAG = "UsbAccessoryService";

    public static final String ACTION_RUNNING = "com.sankovicmarko.android.usbaccessoryservice.UsbAccessoryService.action.running";
    public static final String ACTION_STOPPED = "com.sankovicmarko.android.usbaccessoryservice.UsbAccessoryService.action.stopped";

    private boolean mRunning;

    private UsbManager mUsbManager;

    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;

    private static final int MESSAGE_DUMMY = 64;

    protected class DummyMsg {
        private String message;

        public DummyMsg(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!mRunning) {

            mRunning = true;

            mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            mAccessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

            openAccessory(mAccessory);

            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_RUNNING));
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        closeAccessory();
        mRunning = false;
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_STOPPED));
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
            Thread thread = new Thread(null, this, "UsbAccessoryService");
            thread.start();
            Log.d(TAG, "accessory opened");
        } else {
            Log.d(TAG, "accessory open fail");
        }
    }

    private void closeAccessory() {
        Log.d(TAG, "closeAccessory");
        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    @Override
    public void run() {

        int ret = 0;
        byte[] buffer = new byte[16384];

        while (ret >= 0) {
            try {
                ret = mInputStream.read(buffer);
            } catch (IOException e) {
                break;
            }

            Message m = Message.obtain(mHandler, MESSAGE_DUMMY);
            m.obj = new DummyMsg("Read: " + ret + "bytes");
            mHandler.sendMessage(m);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        stopSelf();
        Log.d(TAG, "end UsbAccessoryService run() with IOException");
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_DUMMY:
                    DummyMsg o = (DummyMsg) msg.obj;
                    handleDummyMessage(o);
            }
        }
    };

    protected void handleDummyMessage(DummyMsg msg) {
        Log.d(TAG, msg.getMessage());
    }
}
