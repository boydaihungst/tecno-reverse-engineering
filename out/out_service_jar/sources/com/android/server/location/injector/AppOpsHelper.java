package com.android.server.location.injector;

import android.location.util.identity.CallerIdentity;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
/* loaded from: classes.dex */
public abstract class AppOpsHelper {
    private final CopyOnWriteArrayList<LocationAppOpListener> mListeners = new CopyOnWriteArrayList<>();

    /* loaded from: classes.dex */
    public interface LocationAppOpListener {
        void onAppOpsChanged(String str);
    }

    public abstract boolean checkOpNoThrow(int i, CallerIdentity callerIdentity);

    public abstract void finishOp(int i, CallerIdentity callerIdentity);

    public abstract boolean noteOp(int i, CallerIdentity callerIdentity);

    public abstract boolean noteOpNoThrow(int i, CallerIdentity callerIdentity);

    public abstract boolean startOpNoThrow(int i, CallerIdentity callerIdentity);

    /* JADX INFO: Access modifiers changed from: protected */
    public final void notifyAppOpChanged(String packageName) {
        Iterator<LocationAppOpListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            LocationAppOpListener listener = it.next();
            listener.onAppOpsChanged(packageName);
        }
    }

    public final void addListener(LocationAppOpListener listener) {
        this.mListeners.add(listener);
    }

    public final void removeListener(LocationAppOpListener listener) {
        this.mListeners.remove(listener);
    }
}
