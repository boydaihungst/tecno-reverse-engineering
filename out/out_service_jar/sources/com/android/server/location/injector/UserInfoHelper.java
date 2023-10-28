package com.android.server.location.injector;

import android.util.IndentingPrintWriter;
import android.util.Log;
import com.android.server.location.LocationManagerService;
import com.android.server.location.eventlog.LocationEventLog;
import java.io.FileDescriptor;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
/* loaded from: classes.dex */
public abstract class UserInfoHelper {
    private final CopyOnWriteArrayList<UserListener> mListeners = new CopyOnWriteArrayList<>();

    /* loaded from: classes.dex */
    public interface UserListener {
        public static final int CURRENT_USER_CHANGED = 1;
        public static final int USER_STARTED = 2;
        public static final int USER_STOPPED = 3;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        public @interface UserChange {
        }

        void onUserChanged(int i, int i2);
    }

    public abstract void dump(FileDescriptor fileDescriptor, IndentingPrintWriter indentingPrintWriter, String[] strArr);

    public abstract int getCurrentUserId();

    protected abstract int[] getProfileIds(int i);

    public abstract int[] getRunningUserIds();

    public abstract boolean isCurrentUserId(int i);

    public final void addListener(UserListener listener) {
        this.mListeners.add(listener);
    }

    public final void removeListener(UserListener listener) {
        this.mListeners.remove(listener);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void dispatchOnUserStarted(int userId) {
        if (LocationManagerService.D) {
            Log.d(LocationManagerService.TAG, "u" + userId + " started");
        }
        Iterator<UserListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            UserListener listener = it.next();
            listener.onUserChanged(userId, 2);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void dispatchOnUserStopped(int userId) {
        if (LocationManagerService.D) {
            Log.d(LocationManagerService.TAG, "u" + userId + " stopped");
        }
        Iterator<UserListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            UserListener listener = it.next();
            listener.onUserChanged(userId, 3);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void dispatchOnCurrentUserChanged(int fromUserId, int toUserId) {
        int[] fromUserIds = getProfileIds(fromUserId);
        int[] toUserIds = getProfileIds(toUserId);
        if (LocationManagerService.D) {
            Log.d(LocationManagerService.TAG, "current user changed from u" + Arrays.toString(fromUserIds) + " to u" + Arrays.toString(toUserIds));
        }
        LocationEventLog.EVENT_LOG.logUserSwitched(fromUserId, toUserId);
        Iterator<UserListener> it = this.mListeners.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            UserListener listener = it.next();
            for (int userId : fromUserIds) {
                listener.onUserChanged(userId, 1);
            }
        }
        Iterator<UserListener> it2 = this.mListeners.iterator();
        while (it2.hasNext()) {
            UserListener listener2 = it2.next();
            for (int userId2 : toUserIds) {
                listener2.onUserChanged(userId2, 1);
            }
        }
    }
}
