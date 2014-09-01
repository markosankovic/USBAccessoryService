package com.sankovicmarko.android.usbaccessoryservice;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

public class UsbAccessoryActivityFunctionalTest extends ActivityInstrumentationTestCase2<UsbAccessoryActivity> {

    private Solo solo;

    public UsbAccessoryActivityFunctionalTest() {
        super(UsbAccessoryActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        solo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    public void testUsbAccessoryServiceActionRunningBroadcastStartsDashboardActivity() {

        LocalBroadcastManager.getInstance(getInstrumentation().getTargetContext()).sendBroadcast(new Intent(UsbAccessoryService.ACTION_RUNNING));

        solo.assertCurrentActivity("DashboardActivity is not started by UsbAccessoryService.ACTION_RUNNING broadcast message",
                DashboardActivity.class);
    }
}
