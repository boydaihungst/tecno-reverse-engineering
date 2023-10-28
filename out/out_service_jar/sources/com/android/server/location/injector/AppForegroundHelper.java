package com.android.server.location.injector;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
/* loaded from: classes.dex */
public abstract class AppForegroundHelper {
    protected static final int FOREGROUND_IMPORTANCE_CUTOFF = 125;
    private final CopyOnWriteArrayList<AppForegroundListener> mListeners = new CopyOnWriteArrayList<>();

    /* loaded from: classes.dex */
    public interface AppForegroundListener {
        void onAppForegroundChanged(int i, boolean z);
    }

    public abstract boolean isAppForeground(int i);

    /* JADX INFO: Access modifiers changed from: protected */
    public static boolean isForeground(int importance) {
        return importance <= 125;
    }

    public final void addListener(AppForegroundListener listener) {
        this.mListeners.add(listener);
    }

    public final void removeListener(AppForegroundListener listener) {
        this.mListeners.remove(listener);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void notifyAppForeground(int uid, boolean foreground) {
        Iterator<AppForegroundListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            AppForegroundListener listener = it.next();
            listener.onAppForegroundChanged(uid, foreground);
        }
    }
}
