package com.android.server.location.injector;

import android.util.Log;
import com.android.server.location.LocationManagerService;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
/* loaded from: classes.dex */
public abstract class ScreenInteractiveHelper {
    private final CopyOnWriteArrayList<ScreenInteractiveChangedListener> mListeners = new CopyOnWriteArrayList<>();

    /* loaded from: classes.dex */
    public interface ScreenInteractiveChangedListener {
        void onScreenInteractiveChanged(boolean z);
    }

    public abstract boolean isInteractive();

    public final void addListener(ScreenInteractiveChangedListener listener) {
        this.mListeners.add(listener);
    }

    public final void removeListener(ScreenInteractiveChangedListener listener) {
        this.mListeners.remove(listener);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void notifyScreenInteractiveChanged(boolean interactive) {
        if (LocationManagerService.D) {
            Log.d(LocationManagerService.TAG, "screen interactive is now " + interactive);
        }
        Iterator<ScreenInteractiveChangedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            ScreenInteractiveChangedListener listener = it.next();
            listener.onScreenInteractiveChanged(interactive);
        }
    }
}
