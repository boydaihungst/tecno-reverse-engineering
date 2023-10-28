package android.app.servertransaction;

import android.app.ActivityClient;
import android.app.ActivityThread;
import android.app.ClientTransactionHandler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Trace;
/* loaded from: classes.dex */
public class PauseActivityItem extends ActivityLifecycleItem {
    public static final Parcelable.Creator<PauseActivityItem> CREATOR = new Parcelable.Creator<PauseActivityItem>() { // from class: android.app.servertransaction.PauseActivityItem.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PauseActivityItem createFromParcel(Parcel in) {
            return new PauseActivityItem(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PauseActivityItem[] newArray(int size) {
            return new PauseActivityItem[size];
        }
    };
    private static final String TAG = "PauseActivityItem";
    private int mConfigChanges;
    private boolean mDontReport;
    private boolean mFinished;
    private boolean mUserLeaving;

    @Override // android.app.servertransaction.ActivityTransactionItem
    public void execute(ClientTransactionHandler client, ActivityThread.ActivityClientRecord r, PendingTransactionActions pendingActions) {
        Trace.traceBegin(64L, "activityPause");
        client.handlePauseActivity(r, this.mFinished, this.mUserLeaving, this.mConfigChanges, pendingActions, "PAUSE_ACTIVITY_ITEM");
        Trace.traceEnd(64L);
    }

    @Override // android.app.servertransaction.ActivityLifecycleItem
    public int getTargetState() {
        return 4;
    }

    @Override // android.app.servertransaction.BaseClientRequest
    public void postExecute(ClientTransactionHandler client, IBinder token, PendingTransactionActions pendingActions) {
        if (this.mDontReport) {
            return;
        }
        ActivityClient.getInstance().activityPaused(token);
    }

    private PauseActivityItem() {
    }

    public static PauseActivityItem obtain(boolean finished, boolean userLeaving, int configChanges, boolean dontReport) {
        PauseActivityItem instance = (PauseActivityItem) ObjectPool.obtain(PauseActivityItem.class);
        if (instance == null) {
            instance = new PauseActivityItem();
        }
        instance.mFinished = finished;
        instance.mUserLeaving = userLeaving;
        instance.mConfigChanges = configChanges;
        instance.mDontReport = dontReport;
        return instance;
    }

    public static PauseActivityItem obtain() {
        PauseActivityItem instance = (PauseActivityItem) ObjectPool.obtain(PauseActivityItem.class);
        if (instance == null) {
            instance = new PauseActivityItem();
        }
        instance.mFinished = false;
        instance.mUserLeaving = false;
        instance.mConfigChanges = 0;
        instance.mDontReport = true;
        return instance;
    }

    @Override // android.app.servertransaction.ActivityLifecycleItem, android.app.servertransaction.ObjectPoolItem
    public void recycle() {
        super.recycle();
        this.mFinished = false;
        this.mUserLeaving = false;
        this.mConfigChanges = 0;
        this.mDontReport = false;
        ObjectPool.recycle(this);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBoolean(this.mFinished);
        dest.writeBoolean(this.mUserLeaving);
        dest.writeInt(this.mConfigChanges);
        dest.writeBoolean(this.mDontReport);
    }

    private PauseActivityItem(Parcel in) {
        this.mFinished = in.readBoolean();
        this.mUserLeaving = in.readBoolean();
        this.mConfigChanges = in.readInt();
        this.mDontReport = in.readBoolean();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PauseActivityItem other = (PauseActivityItem) o;
        if (this.mFinished == other.mFinished && this.mUserLeaving == other.mUserLeaving && this.mConfigChanges == other.mConfigChanges && this.mDontReport == other.mDontReport) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + (this.mFinished ? 1 : 0);
        return (((((result * 31) + (this.mUserLeaving ? 1 : 0)) * 31) + this.mConfigChanges) * 31) + (this.mDontReport ? 1 : 0);
    }

    public String toString() {
        return "PauseActivityItem{finished=" + this.mFinished + ",userLeaving=" + this.mUserLeaving + ",configChanges=" + this.mConfigChanges + ",dontReport=" + this.mDontReport + "}";
    }
}
