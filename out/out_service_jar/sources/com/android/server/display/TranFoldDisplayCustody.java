package com.android.server.display;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.LongSparseArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.view.DisplayAddress;
import android.view.DisplayInfo;
import com.android.server.display.TranFoldDisplayCustody;
import com.android.server.display.layout.Layout;
import com.android.server.wm.TranFoldWMCustody;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public final class TranFoldDisplayCustody {
    public static final boolean DEBUG = true;
    public static final boolean DEBUG_HEAVY = false;
    public static final boolean FOLD_SUPPORT = "1".equals(SystemProperties.get("ro.os_foldable_screen_support", ""));
    public static final boolean SWITCH_DISABLE_ALL_ANIMATIONS = false;
    public static final boolean SWITCH_WAITING_DRAWN = true;
    public static final boolean SWITCH_WALLPAPER_REDRAW = false;
    public static final String TAG = "os.fold";
    private static final int TIMEOUT_STATE_TRANSITION_MILLIS = 1000;
    private final DisplayLayoutMap mDisplayLayoutMap;
    private long mDisplayTransitionStartTime;
    private final DisplayUniqueIdMap mDisplayUniqueIdMap;
    private final LongSparseArray<Integer> mSettingPowerMode;
    private final WaitingForDrawn mWaitingForDrawn;

    /* loaded from: classes.dex */
    private static class Holder {
        static final TranFoldDisplayCustody instance = new TranFoldDisplayCustody();

        private Holder() {
        }
    }

    public static TranFoldDisplayCustody instance() {
        return Holder.instance;
    }

    private TranFoldDisplayCustody() {
        this.mDisplayUniqueIdMap = new DisplayUniqueIdMap();
        this.mDisplayLayoutMap = new DisplayLayoutMap();
        this.mWaitingForDrawn = new WaitingForDrawn();
        this.mSettingPowerMode = new LongSparseArray<>();
    }

    public static boolean disable() {
        return !FOLD_SUPPORT;
    }

    public static boolean disableWaitingDrawn() {
        return disable();
    }

    public static boolean disableWallpaperRedraw() {
        disable();
        return true;
    }

    public static boolean disableDisableAllAnimations() {
        disable();
        return true;
    }

    public int getWaitingForDrawnTimeout(int timeout) {
        return disable() ? timeout : this.mWaitingForDrawn.getWaitingForDrawnTimeout(timeout);
    }

    public void keyguardDrawnTimeout() {
        this.mWaitingForDrawn.keyguardDrawnTimeout();
    }

    public boolean isSettingPowerMode() {
        boolean z;
        synchronized (this.mSettingPowerMode) {
            z = this.mSettingPowerMode.size() != 0;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static long getTimeoutStateTransition(long timeout) {
        return disable() ? timeout : Math.max(1000L, timeout);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeviceStateLockedBegin(LogicalDisplayMapper ldm) {
        if (disable()) {
            return;
        }
        Slog.d(TAG, "setDeviceStateLockedBegin");
        this.mDisplayTransitionStartTime = SystemClock.elapsedRealtime();
        this.mDisplayLayoutMap.transitionToPendingStateLockedBegin();
        this.mWaitingForDrawn.setDeviceStateLockedBegin(ldm);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void wakeOrSleepDeviceWhenStateChanged(boolean wakeDevice, boolean sleepDevice) {
        if (disable()) {
            return;
        }
        Slog.d(TAG, "wakeOrSleepDeviceWhenStateChanged  wakeDevice:" + wakeDevice + " sleepDevice:" + sleepDevice);
        this.mWaitingForDrawn.wakeOrSleepDeviceWhenStateChanged(wakeDevice, sleepDevice);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyLayoutLocked(Layout.Display displayLayout) {
        if (disable()) {
            return;
        }
        this.mDisplayLayoutMap.updateDisplayLayout(displayLayout);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void transitionToPendingStateLockedBegin() {
        if (disable()) {
            return;
        }
        Slog.d(TAG, "transitionToPendingStateLockedBegin. pass " + (SystemClock.elapsedRealtime() - this.mDisplayTransitionStartTime) + " milliseconds");
        WaitingForDrawn.transitionToPendingStateLockedBegin();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void transitionToPendingStateLockedEnd() {
        if (disable()) {
            return;
        }
        Slog.d(TAG, "transitionToPendingStateLockedEnd. pass " + (SystemClock.elapsedRealtime() - this.mDisplayTransitionStartTime) + " milliseconds");
        DisplayLayoutMap displayLayoutMap = this.mDisplayLayoutMap;
        final WaitingForDrawn waitingForDrawn = this.mWaitingForDrawn;
        Objects.requireNonNull(waitingForDrawn);
        displayLayoutMap.transitionToPendingStateLockedEnd(new Consumer() { // from class: com.android.server.display.TranFoldDisplayCustody$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TranFoldDisplayCustody.WaitingForDrawn.this.updateDisplayLayout((ArrayList) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPrimaryDisplayDeviceLocked(LogicalDisplay display, DisplayDevice newDevice) {
        if (disable()) {
            return;
        }
        this.mDisplayUniqueIdMap.setDisplayUniqueId(display.getDisplayIdLocked(), newDevice == null ? null : newDevice.getUniqueId());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisplayPowerModeStart(long physicalDisplayId, String uniqueDisplayId, int oldState, int newState) {
        if (disable()) {
            return;
        }
        Slog.d(TAG, "setDisplayPowerModeStart physicalDisplayId:" + physicalDisplayId + " uniqueDisplayId:" + uniqueDisplayId + " oldState:" + oldState + " newState:" + newState);
        synchronized (this.mSettingPowerMode) {
            this.mSettingPowerMode.put(physicalDisplayId, Integer.valueOf(newState));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisplayPowerModeEnd(long physicalDisplayId) {
        if (disable()) {
            return;
        }
        Slog.d(TAG, "setDisplayPowerModeEnd physicalDisplayId:" + physicalDisplayId);
        synchronized (this.mSettingPowerMode) {
            this.mSettingPowerMode.remove(physicalDisplayId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dismissColorFadeResources(int displayId, DisplayPowerState powerState) {
        if (disable()) {
            return;
        }
        WaitingForDrawn.dismissColorFadeResources(displayId, powerState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean needBrightnessOff(int displayId, int displayState, boolean pendingScreenOnUnblock) {
        if (disable()) {
            return false;
        }
        return WaitingForDrawn.needBrightnessOff(displayId, displayState, pendingScreenOnUnblock);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onInitializeColorFadeAnimator(int displayId, ObjectAnimator colorFadeOnAnimator, ObjectAnimator colorFadeOffAnimator) {
        if (disable()) {
            return;
        }
        ColorFadeGuard.onInitializeColorFadeAnimator(displayId, colorFadeOnAnimator);
    }

    /* loaded from: classes.dex */
    private static final class ColorFadeGuard {
        private ColorFadeGuard() {
        }

        static void onInitializeColorFadeAnimator(final int displayId, ObjectAnimator colorFadeOnAnimator) {
            if (disable(displayId)) {
                return;
            }
            colorFadeOnAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.display.TranFoldDisplayCustody$ColorFadeGuard$$ExternalSyntheticLambda0
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    TranFoldDisplayCustody.ColorFadeGuard.onFadeOnAnimatorUpdate(displayId, valueAnimator);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static void onFadeOnAnimatorUpdate(int displayId, ValueAnimator animation) {
            WaitingForDrawn.onFadeOnAnimatorUpdate(displayId, animation);
        }

        private static boolean disable(int displayId) {
            return disable() || displayId != 0;
        }

        private static boolean disable() {
            return TranFoldDisplayCustody.disableWallpaperRedraw();
        }
    }

    /* loaded from: classes.dex */
    private static final class DisplayLayoutMap {
        private final SparseArray<DisplayLayoutState> mDisplayLayout;
        private final Object mLock;

        private DisplayLayoutMap() {
            this.mLock = new Object();
            this.mDisplayLayout = new SparseArray<>();
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class DisplayLayout {
            private final DisplayAddress mAddress;
            private final boolean mIsEnabled;
            private final int mLogicalDisplayId;

            DisplayLayout(Layout.Display displayLayout) {
                this.mAddress = displayLayout.getAddress();
                this.mLogicalDisplayId = displayLayout.getLogicalDisplayId();
                this.mIsEnabled = displayLayout.isEnabled();
            }

            DisplayLayout(DisplayLayout displayLayout) {
                this.mAddress = displayLayout.mAddress;
                this.mLogicalDisplayId = displayLayout.mLogicalDisplayId;
                this.mIsEnabled = displayLayout.mIsEnabled;
            }

            public boolean isSame(DisplayLayout displayLayout) {
                DisplayAddress displayAddress;
                return displayLayout != null && (((displayAddress = this.mAddress) == null && displayLayout.mAddress == null) || (displayAddress != null && displayAddress.equals(displayLayout.mAddress))) && this.mIsEnabled == displayLayout.mIsEnabled;
            }

            static DisplayLayout clone(DisplayLayout displayLayout) {
                if (displayLayout == null) {
                    return null;
                }
                return new DisplayLayout(displayLayout);
            }

            public String toString() {
                return "{addr: " + this.mAddress + ", dispId: " + this.mLogicalDisplayId + "(" + (this.mIsEnabled ? "ON" : "OFF") + ")}";
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class DisplayLayoutState {
            final ArrayList<DisplayLayout> mCurrent;
            DisplayLayout mLast;

            private DisplayLayoutState() {
                this.mCurrent = new ArrayList<>();
            }
        }

        public void transitionToPendingStateLockedBegin() {
            synchronized (this.mLock) {
                for (int i = 0; i < this.mDisplayLayout.size(); i++) {
                    DisplayLayoutState state = this.mDisplayLayout.valueAt(i);
                    if (state.mCurrent.size() > 1) {
                        DisplayLayout current = state.mCurrent.get(state.mCurrent.size() - 1);
                        state.mCurrent.clear();
                        state.mCurrent.add(current);
                    }
                }
            }
        }

        public void updateDisplayLayout(Layout.Display displayLayout) {
            synchronized (this.mLock) {
                DisplayLayoutState state = this.mDisplayLayout.get(displayLayout.getLogicalDisplayId(), null);
                if (state == null) {
                    state = new DisplayLayoutState();
                    this.mDisplayLayout.put(displayLayout.getLogicalDisplayId(), state);
                }
                state.mCurrent.add(new DisplayLayout(displayLayout));
            }
        }

        public void transitionToPendingStateLockedEnd(Consumer<ArrayList<Pair<DisplayLayout, DisplayLayout>>> callback) {
            ArrayList<Pair<DisplayLayout, DisplayLayout>> changed = callback == null ? null : new ArrayList<>();
            synchronized (this.mLock) {
                updateLocked(changed);
            }
            if (callback != null) {
                callback.accept(changed);
            }
        }

        private void updateLocked(ArrayList<Pair<DisplayLayout, DisplayLayout>> changed) {
            for (int i = 0; i < this.mDisplayLayout.size(); i++) {
                DisplayLayoutState state = this.mDisplayLayout.valueAt(i);
                if (!state.mCurrent.isEmpty()) {
                    DisplayLayout current = state.mCurrent.get(state.mCurrent.size() - 1);
                    if (!current.isSame(state.mLast)) {
                        Slog.d(TranFoldDisplayCustody.TAG, "layout modified from " + state.mLast + " to " + current);
                        if (changed != null) {
                            changed.add(new Pair<>(DisplayLayout.clone(state.mLast), DisplayLayout.clone(current)));
                        }
                        state.mLast = current;
                    }
                    if (state.mCurrent.size() > 1) {
                        state.mCurrent.clear();
                        state.mCurrent.add(state.mLast);
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private static final class DisplayUniqueIdMap {
        private final SparseArray<String> mDisplayUniqueId;
        private final Object mDisplayUniqueIdLock;

        private DisplayUniqueIdMap() {
            this.mDisplayUniqueIdLock = new Object();
            this.mDisplayUniqueId = new SparseArray<>();
        }

        public void setDisplayUniqueId(int displayId, String uniqueDisplayId) {
            synchronized (this.mDisplayUniqueIdLock) {
                this.mDisplayUniqueId.put(displayId, uniqueDisplayId);
            }
        }

        public boolean needUpdate(int displayId, String uniqueDisplayId) {
            boolean z;
            synchronized (this.mDisplayUniqueIdLock) {
                z = !Objects.equals(this.mDisplayUniqueId.get(displayId, null), uniqueDisplayId);
            }
            return z;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class WaitingForDrawn {
        private static final int DISPLAY_ID = 0;
        private boolean mDisplayOff;

        private WaitingForDrawn() {
        }

        public int getWaitingForDrawnTimeout(int timeout) {
            if (TranFoldDisplayCustody.disableWaitingDrawn()) {
                return timeout;
            }
            return TranFoldWMCustody.instance().getWaitingForDrawnTimeout(timeout);
        }

        public void keyguardDrawnTimeout() {
            if (TranFoldDisplayCustody.disableWaitingDrawn()) {
                return;
            }
            TranFoldWMCustody.instance().keyguardDrawnTimeout();
        }

        public void setDeviceStateLockedBegin(LogicalDisplayMapper ldm) {
            if (TranFoldDisplayCustody.disableWaitingDrawn()) {
                return;
            }
            LogicalDisplay display = ldm.getDisplayLocked(0, true);
            boolean isOff = true;
            if (display != null) {
                DisplayDevice device = display.getPrimaryDisplayDeviceLocked();
                if (device != null) {
                    DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
                    if (info.state != 1) {
                        isOff = false;
                    }
                } else {
                    DisplayInfo info2 = display.getDisplayInfoLocked();
                    if (info2.state != 1) {
                        isOff = false;
                    }
                }
            }
            this.mDisplayOff = isOff;
            TranFoldWMCustody.instance().setDeviceStateLockedBegin();
        }

        public void wakeOrSleepDeviceWhenStateChanged(boolean wakeDevice, boolean sleepDevice) {
            if (wakeDevice) {
                this.mDisplayOff = false;
            } else if (sleepDevice) {
                this.mDisplayOff = true;
            }
        }

        public void updateDisplayLayout(List<Pair<DisplayLayoutMap.DisplayLayout, DisplayLayoutMap.DisplayLayout>> changed) {
            if (TranFoldDisplayCustody.disableWaitingDrawn()) {
                return;
            }
            int displayId = -1;
            if (!this.mDisplayOff && changed != null && !changed.isEmpty()) {
                Iterator<Pair<DisplayLayoutMap.DisplayLayout, DisplayLayoutMap.DisplayLayout>> it = changed.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    Pair<DisplayLayoutMap.DisplayLayout, DisplayLayoutMap.DisplayLayout> p = it.next();
                    DisplayLayoutMap.DisplayLayout oldLayout = (DisplayLayoutMap.DisplayLayout) p.first;
                    DisplayLayoutMap.DisplayLayout newLayout = (DisplayLayoutMap.DisplayLayout) p.second;
                    if (oldLayout != null && newLayout != null && newLayout.mIsEnabled && newLayout.mLogicalDisplayId == 0 && (oldLayout.mAddress instanceof DisplayAddress.Physical) && (newLayout.mAddress instanceof DisplayAddress.Physical) && !newLayout.mAddress.equals(oldLayout.mAddress)) {
                        displayId = 0;
                        break;
                    }
                }
            }
            TranFoldWMCustody.instance().updateDisplayLayout(displayId);
        }

        public static void dismissColorFadeResources(int displayId, DisplayPowerState powerState) {
            if (disable(displayId)) {
                return;
            }
            powerState.dismissColorFade();
            Slog.d(TranFoldDisplayCustody.TAG, "dismissColorFade with SWITCH_WAITING_DRAWN");
        }

        public static void transitionToPendingStateLockedBegin() {
            if (TranFoldDisplayCustody.disableWaitingDrawn()) {
                return;
            }
            TranFoldWMCustody.instance().transitionToPendingStateLockedBegin();
        }

        public static boolean needBrightnessOff(int displayId, int displayState, boolean pendingScreenOnUnblock) {
            return !disable(displayId) && displayState == 2 && pendingScreenOnUnblock;
        }

        public static void onFadeOnAnimatorUpdate(int displayId, ValueAnimator animation) {
            if (disable(displayId)) {
                return;
            }
            TranFoldWMCustody.instance().onFadeOnAnimatorUpdate(displayId, animation);
        }

        private static boolean disable(int displayId) {
            return TranFoldDisplayCustody.disableWaitingDrawn() || displayId != 0;
        }
    }
}
