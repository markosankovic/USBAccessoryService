package com.sankovicmarko.android.usbaccessoryservice;

import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.widget.TextView;

import com.robotium.solo.Condition;
import com.robotium.solo.Solo;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

public class DashboardActivityFunctionalTest extends ActivityInstrumentationTestCase2<DashboardActivity> {

    static final int WAIT_FOR_CONDITION_TIMEOUT = 2000;

    private Solo solo;
    private OBCBroadcastMessenger obcBroadcastMessenger;

    @Mock
    private OBC obcMock;

    public DashboardActivityFunctionalTest() {
        super(DashboardActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);

        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        DashboardActivity dashboardActivity = getActivity();
        solo = new Solo(getInstrumentation(), dashboardActivity);
        obcBroadcastMessenger = new OBCBroadcastMessenger(getInstrumentation().getTargetContext());

        MockitoAnnotations.initMocks(this);
        dashboardActivity.setOBC(obcMock);
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
        obcMock = null;
    }

    public void testUpdateSpeedTextViewOnBroadcastMessengerSendSpeed() throws InterruptedException {
        obcBroadcastMessenger.sendSpeed(42);

        final TextView speedTextView = (TextView) solo.getView(R.id.speedTextView);

        // Must wait for a condition.
        // If checking is done too soon broadcast receiver might not be executed.
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return speedTextView.getText().length() > 0;
            }
        }, WAIT_FOR_CONDITION_TIMEOUT);

        assertEquals("Speed: 42", speedTextView.getText());
    }

    public void testDummyDataIsSentToOBC() throws Throwable {
        // TODO Find out why solo.clickOnView does not behave in the same way as TouchUtils.clickView. It seems that solo.clickOnView is executed in different thread than this current one.)
        TouchUtils.clickView(this, solo.getView(R.id.sendDummyDataButton));
        verify(obcMock).sendDummyData();
    }
}
