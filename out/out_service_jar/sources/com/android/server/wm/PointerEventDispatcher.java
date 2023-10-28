package com.android.server.wm;

import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.android.server.UiThread;
import java.util.ArrayList;
/* loaded from: classes2.dex */
public class PointerEventDispatcher extends InputEventReceiver {
    private final ArrayList<WindowManagerPolicyConstants.PointerEventListener> mListeners;
    private WindowManagerPolicyConstants.PointerEventListener[] mListenersArray;

    public PointerEventDispatcher(InputChannel inputChannel) {
        super(inputChannel, UiThread.getHandler().getLooper());
        this.mListeners = new ArrayList<>();
        this.mListenersArray = new WindowManagerPolicyConstants.PointerEventListener[0];
    }

    public void onInputEvent(InputEvent event) {
        WindowManagerPolicyConstants.PointerEventListener[] listeners;
        try {
            if ((event instanceof MotionEvent) && (event.getSource() & 2) != 0) {
                MotionEvent motionEvent = (MotionEvent) event;
                synchronized (this.mListeners) {
                    if (this.mListenersArray == null) {
                        WindowManagerPolicyConstants.PointerEventListener[] pointerEventListenerArr = new WindowManagerPolicyConstants.PointerEventListener[this.mListeners.size()];
                        this.mListenersArray = pointerEventListenerArr;
                        this.mListeners.toArray(pointerEventListenerArr);
                    }
                    listeners = this.mListenersArray;
                }
                for (WindowManagerPolicyConstants.PointerEventListener pointerEventListener : listeners) {
                    pointerEventListener.onPointerEvent(motionEvent);
                }
            }
        } finally {
            finishInputEvent(event, false);
        }
    }

    public void registerInputEventListener(WindowManagerPolicyConstants.PointerEventListener listener) {
        synchronized (this.mListeners) {
            if (this.mListeners.contains(listener)) {
                throw new IllegalStateException("registerInputEventListener: trying to register" + listener + " twice.");
            }
            this.mListeners.add(listener);
            this.mListenersArray = null;
        }
    }

    public void unregisterInputEventListener(WindowManagerPolicyConstants.PointerEventListener listener) {
        synchronized (this.mListeners) {
            if (!this.mListeners.contains(listener)) {
                throw new IllegalStateException("registerInputEventListener: " + listener + " not registered.");
            }
            this.mListeners.remove(listener);
            this.mListenersArray = null;
        }
    }

    public void dispose() {
        super.dispose();
        synchronized (this.mListeners) {
            this.mListeners.clear();
            this.mListenersArray = null;
        }
    }
}
