package com.sankovicmarko.android.usbaccessoryservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Binder;
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

public class UsbAccessoryService extends Service implements Runnable, OBC {

    private static final String TAG = "UsbAccessoryService";

    public static final String ACTION_RUNNING = "com.sankovicmarko.android.usbaccessoryservice.UsbAccessoryService.action.running";
    public static final String ACTION_STOPPED = "com.sankovicmarko.android.usbaccessoryservice.UsbAccessoryService.action.stopped";

    private boolean mRunning;

    private UsbManager mUsbManager;

    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {

        UsbAccessoryService getService() {
            // Return this instance of UsbAccessoryService so clients can call public methods
            return UsbAccessoryService.this;
        }
    }

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

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
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
        Log.d(TAG, "accessory close");
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

        // Must be called before stopSelf, so that the running activity which is bound this service,
        // releases the service and allow it to stop.
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_STOPPED));

        stopSelf();
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

    private void writeBytes(byte[] bytes) {
        if (mOutputStream != null) {
            try {
                mOutputStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write failed", e);
            }
        }
    }

    @Override
    public void sendDummyData() {
        writeBytes("A0000".getBytes());
    }
}
