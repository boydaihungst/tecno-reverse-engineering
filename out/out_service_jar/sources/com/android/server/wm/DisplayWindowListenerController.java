package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.IntArray;
import android.view.IDisplayWindowListener;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class DisplayWindowListenerController {
    RemoteCallbackList<IDisplayWindowListener> mDisplayListeners = new RemoteCallbackList<>();
    private final WindowManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayWindowListenerController(WindowManagerService service) {
        this.mService = service;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int[] registerListener(IDisplayWindowListener listener) {
        int[] array;
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mDisplayListeners.register(listener);
                final IntArray displayIds = new IntArray();
                this.mService.mAtmService.mRootWindowContainer.forAllDisplays(new Consumer() { // from class: com.android.server.wm.DisplayWindowListenerController$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        displayIds.add(((DisplayContent) obj).mDisplayId);
                    }
                });
                array = displayIds.toArray();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return array;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterListener(IDisplayWindowListener listener) {
        this.mDisplayListeners.unregister(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchDisplayAdded(DisplayContent display) {
        int count = this.mDisplayListeners.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                this.mDisplayListeners.getBroadcastItem(i).onDisplayAdded(display.mDisplayId);
            } catch (RemoteException e) {
            }
        }
        this.mDisplayListeners.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchDisplayChanged(DisplayContent display, Configuration newConfig) {
        boolean isInHierarchy = false;
        for (int i = 0; i < display.getParent().getChildCount(); i++) {
            if (display.getParent().getChildAt(i) == display) {
                isInHierarchy = true;
            }
        }
        if (!isInHierarchy) {
            return;
        }
        int count = this.mDisplayListeners.beginBroadcast();
        for (int i2 = 0; i2 < count; i2++) {
            try {
                this.mDisplayListeners.getBroadcastItem(i2).onDisplayConfigurationChanged(display.getDisplayId(), newConfig);
            } catch (RemoteException e) {
            }
        }
        this.mDisplayListeners.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchDisplayRemoved(DisplayContent display) {
        int count = this.mDisplayListeners.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                this.mDisplayListeners.getBroadcastItem(i).onDisplayRemoved(display.mDisplayId);
            } catch (RemoteException e) {
            }
        }
        this.mDisplayListeners.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchFixedRotationStarted(DisplayContent display, int newRotation) {
        int count = this.mDisplayListeners.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                this.mDisplayListeners.getBroadcastItem(i).onFixedRotationStarted(display.mDisplayId, newRotation);
            } catch (RemoteException e) {
            }
        }
        this.mDisplayListeners.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchFixedRotationFinished(DisplayContent display) {
        int count = this.mDisplayListeners.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                this.mDisplayListeners.getBroadcastItem(i).onFixedRotationFinished(display.mDisplayId);
            } catch (RemoteException e) {
            }
        }
        this.mDisplayListeners.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchKeepClearAreasChanged(DisplayContent display, Set<Rect> restricted, Set<Rect> unrestricted) {
        int count = this.mDisplayListeners.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                this.mDisplayListeners.getBroadcastItem(i).onKeepClearAreasChanged(display.mDisplayId, new ArrayList(restricted), new ArrayList(unrestricted));
            } catch (RemoteException e) {
            }
        }
        this.mDisplayListeners.finishBroadcast();
    }
}
