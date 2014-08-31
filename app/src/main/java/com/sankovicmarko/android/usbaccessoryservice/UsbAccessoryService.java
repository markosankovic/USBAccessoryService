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
import java.util.Random;

public class UsbAccessoryService extends Service implements Runnable, OBC {

    private static final String TAG = "UsbAccessoryService";

    public static final String ACTION_RUNNING = "com.sankovicmarko.android.usbaccessoryservice.UsbAccessoryService.action.running";
    public static final String ACTION_STOPPED = "com.sankovicmarko.android.usbaccessoryservice.UsbAccessoryService.action.stopped";

    private OBCBroadcastMessenger obcBroadcastMessenger;

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
        obcBroadcastMessenger = new OBCBroadcastMessenger(this);
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
        obcBroadcastMessenger = null;
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
            m.obj = new DummyMsg("Read: " + ret + " bytes");
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
                    obcBroadcastMessenger.sendSpeed(randInt(0, 40));
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

    /**
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int min, int max) {

        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }
}
