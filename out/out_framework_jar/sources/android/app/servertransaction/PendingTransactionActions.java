package android.app.servertransaction;

import android.app.ActivityClient;
import android.app.ActivityThread;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.TransactionTooLargeException;
import android.util.Log;
import android.util.LogWriter;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
/* loaded from: classes.dex */
public class PendingTransactionActions {
    private boolean mCallOnPostCreate;
    private Bundle mOldState;
    private boolean mReportRelaunchToWM;
    private boolean mRestoreInstanceState;
    private StopInfo mStopInfo;

    public PendingTransactionActions() {
        clear();
    }

    public void clear() {
        this.mRestoreInstanceState = false;
        this.mCallOnPostCreate = false;
        this.mOldState = null;
        this.mStopInfo = null;
    }

    public boolean shouldRestoreInstanceState() {
        return this.mRestoreInstanceState;
    }

    public void setRestoreInstanceState(boolean restoreInstanceState) {
        this.mRestoreInstanceState = restoreInstanceState;
    }

    public boolean shouldCallOnPostCreate() {
        return this.mCallOnPostCreate;
    }

    public void setCallOnPostCreate(boolean callOnPostCreate) {
        this.mCallOnPostCreate = callOnPostCreate;
    }

    public Bundle getOldState() {
        return this.mOldState;
    }

    public void setOldState(Bundle oldState) {
        this.mOldState = oldState;
    }

    public StopInfo getStopInfo() {
        return this.mStopInfo;
    }

    public void setStopInfo(StopInfo stopInfo) {
        this.mStopInfo = stopInfo;
    }

    public boolean shouldReportRelaunchToWindowManager() {
        return this.mReportRelaunchToWM;
    }

    public void setReportRelaunchToWindowManager(boolean reportToWm) {
        this.mReportRelaunchToWM = reportToWm;
    }

    /* loaded from: classes.dex */
    public static class StopInfo implements Runnable {
        private static final String TAG = "ActivityStopInfo";
        private ActivityThread.ActivityClientRecord mActivity;
        private CharSequence mDescription;
        private PersistableBundle mPersistentState;
        private Bundle mState;

        public void setActivity(ActivityThread.ActivityClientRecord activity) {
            this.mActivity = activity;
        }

        public void setState(Bundle state) {
            this.mState = state;
        }

        public void setPersistentState(PersistableBundle persistentState) {
            this.mPersistentState = persistentState;
        }

        public void setDescription(CharSequence description) {
            this.mDescription = description;
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                if (ActivityThread.DEBUG_MEMORY_TRIM) {
                    Slog.v(TAG, "Reporting activity stopped: " + this.mActivity);
                }
                ActivityClient.getInstance().activityStopped(this.mActivity.token, this.mState, this.mPersistentState, this.mDescription);
            } catch (RuntimeException ex) {
                LogWriter writer = new LogWriter(5, TAG);
                IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
                pw.println("Bundle stats:");
                Bundle.dumpStats(pw, this.mState);
                pw.println("PersistableBundle stats:");
                Bundle.dumpStats(pw, this.mPersistentState);
                if ((ex.getCause() instanceof TransactionTooLargeException) && this.mActivity.packageInfo.getTargetSdkVersion() < 24) {
                    Log.e(TAG, "App sent too much data in instance state, so it was ignored", ex);
                    return;
                }
                throw ex;
            }
        }
    }
}
