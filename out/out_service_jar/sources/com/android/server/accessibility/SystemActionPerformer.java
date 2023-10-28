package com.android.server.accessibility;

import android.app.PendingIntent;
import android.app.RemoteAction;
import android.app.StatusBarManager;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.internal.util.ScreenshotHelper;
import com.android.server.LocalServices;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.WindowManagerInternal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class SystemActionPerformer {
    private static final String TAG = "SystemActionPerformer";
    private final Context mContext;
    private final AccessibilityNodeInfo.AccessibilityAction mLegacyBackAction;
    private final AccessibilityNodeInfo.AccessibilityAction mLegacyHomeAction;
    private final AccessibilityNodeInfo.AccessibilityAction mLegacyLockScreenAction;
    private final AccessibilityNodeInfo.AccessibilityAction mLegacyNotificationsAction;
    private final AccessibilityNodeInfo.AccessibilityAction mLegacyPowerDialogAction;
    private final AccessibilityNodeInfo.AccessibilityAction mLegacyQuickSettingsAction;
    private final AccessibilityNodeInfo.AccessibilityAction mLegacyRecentsAction;
    private final AccessibilityNodeInfo.AccessibilityAction mLegacyTakeScreenshotAction;
    private final SystemActionsChangedListener mListener;
    private final Map<Integer, RemoteAction> mRegisteredSystemActions;
    private Supplier<ScreenshotHelper> mScreenshotHelperSupplier;
    private final Object mSystemActionLock;
    private final WindowManagerInternal mWindowManagerService;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface SystemActionsChangedListener {
        void onSystemActionsChanged();
    }

    public SystemActionPerformer(Context context, WindowManagerInternal windowManagerInternal) {
        this(context, windowManagerInternal, null, null);
    }

    public SystemActionPerformer(Context context, WindowManagerInternal windowManagerInternal, Supplier<ScreenshotHelper> screenshotHelperSupplier) {
        this(context, windowManagerInternal, screenshotHelperSupplier, null);
    }

    public SystemActionPerformer(Context context, WindowManagerInternal windowManagerInternal, Supplier<ScreenshotHelper> screenshotHelperSupplier, SystemActionsChangedListener listener) {
        this.mSystemActionLock = new Object();
        this.mRegisteredSystemActions = new ArrayMap();
        this.mContext = context;
        this.mWindowManagerService = windowManagerInternal;
        this.mListener = listener;
        this.mScreenshotHelperSupplier = screenshotHelperSupplier;
        this.mLegacyHomeAction = new AccessibilityNodeInfo.AccessibilityAction(2, context.getResources().getString(17039613));
        this.mLegacyBackAction = new AccessibilityNodeInfo.AccessibilityAction(1, context.getResources().getString(17039604));
        this.mLegacyRecentsAction = new AccessibilityNodeInfo.AccessibilityAction(3, context.getResources().getString(17039620));
        this.mLegacyNotificationsAction = new AccessibilityNodeInfo.AccessibilityAction(4, context.getResources().getString(17039615));
        this.mLegacyQuickSettingsAction = new AccessibilityNodeInfo.AccessibilityAction(5, context.getResources().getString(17039619));
        this.mLegacyPowerDialogAction = new AccessibilityNodeInfo.AccessibilityAction(6, context.getResources().getString(17039618));
        this.mLegacyLockScreenAction = new AccessibilityNodeInfo.AccessibilityAction(8, context.getResources().getString(17039614));
        this.mLegacyTakeScreenshotAction = new AccessibilityNodeInfo.AccessibilityAction(9, context.getResources().getString(17039621));
    }

    public void registerSystemAction(int id, RemoteAction action) {
        synchronized (this.mSystemActionLock) {
            this.mRegisteredSystemActions.put(Integer.valueOf(id), action);
        }
        SystemActionsChangedListener systemActionsChangedListener = this.mListener;
        if (systemActionsChangedListener != null) {
            systemActionsChangedListener.onSystemActionsChanged();
        }
    }

    public void unregisterSystemAction(int id) {
        synchronized (this.mSystemActionLock) {
            this.mRegisteredSystemActions.remove(Integer.valueOf(id));
        }
        SystemActionsChangedListener systemActionsChangedListener = this.mListener;
        if (systemActionsChangedListener != null) {
            systemActionsChangedListener.onSystemActionsChanged();
        }
    }

    public List<AccessibilityNodeInfo.AccessibilityAction> getSystemActions() {
        List<AccessibilityNodeInfo.AccessibilityAction> systemActions = new ArrayList<>();
        synchronized (this.mSystemActionLock) {
            for (Map.Entry<Integer, RemoteAction> entry : this.mRegisteredSystemActions.entrySet()) {
                AccessibilityNodeInfo.AccessibilityAction systemAction = new AccessibilityNodeInfo.AccessibilityAction(entry.getKey().intValue(), entry.getValue().getTitle());
                systemActions.add(systemAction);
            }
            addLegacySystemActions(systemActions);
        }
        return systemActions;
    }

    private void addLegacySystemActions(List<AccessibilityNodeInfo.AccessibilityAction> systemActions) {
        if (!this.mRegisteredSystemActions.containsKey(1)) {
            systemActions.add(this.mLegacyBackAction);
        }
        if (!this.mRegisteredSystemActions.containsKey(2)) {
            systemActions.add(this.mLegacyHomeAction);
        }
        if (!this.mRegisteredSystemActions.containsKey(3)) {
            systemActions.add(this.mLegacyRecentsAction);
        }
        if (!this.mRegisteredSystemActions.containsKey(4)) {
            systemActions.add(this.mLegacyNotificationsAction);
        }
        if (!this.mRegisteredSystemActions.containsKey(5)) {
            systemActions.add(this.mLegacyQuickSettingsAction);
        }
        if (!this.mRegisteredSystemActions.containsKey(6)) {
            systemActions.add(this.mLegacyPowerDialogAction);
        }
        if (!this.mRegisteredSystemActions.containsKey(8)) {
            systemActions.add(this.mLegacyLockScreenAction);
        }
        if (!this.mRegisteredSystemActions.containsKey(9)) {
            systemActions.add(this.mLegacyTakeScreenshotAction);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [318=18] */
    public boolean performSystemAction(int actionId) {
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mSystemActionLock) {
                RemoteAction registeredAction = this.mRegisteredSystemActions.get(Integer.valueOf(actionId));
                if (registeredAction != null) {
                    try {
                        registeredAction.getActionIntent().send();
                        return true;
                    } catch (PendingIntent.CanceledException ex) {
                        Slog.e(TAG, "canceled PendingIntent for global action " + ((Object) registeredAction.getTitle()), ex);
                        return false;
                    }
                }
                switch (actionId) {
                    case 1:
                        sendDownAndUpKeyEvents(4);
                        return true;
                    case 2:
                        sendDownAndUpKeyEvents(3);
                        return true;
                    case 3:
                        return openRecents();
                    case 4:
                        expandNotifications();
                        return true;
                    case 5:
                        expandQuickSettings();
                        return true;
                    case 6:
                        showGlobalActions();
                        return true;
                    case 7:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    default:
                        Slog.e(TAG, "Invalid action id: " + actionId);
                        return false;
                    case 8:
                        return lockScreen();
                    case 9:
                        return takeScreenshot();
                    case 10:
                        sendDownAndUpKeyEvents(79);
                        return true;
                    case 16:
                        sendDownAndUpKeyEvents(19);
                        return true;
                    case 17:
                        sendDownAndUpKeyEvents(20);
                        return true;
                    case 18:
                        sendDownAndUpKeyEvents(21);
                        return true;
                    case 19:
                        sendDownAndUpKeyEvents(22);
                        return true;
                    case 20:
                        sendDownAndUpKeyEvents(23);
                        return true;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void sendDownAndUpKeyEvents(int keyCode) {
        long token = Binder.clearCallingIdentity();
        try {
            long downTime = SystemClock.uptimeMillis();
            sendKeyEventIdentityCleared(keyCode, 0, downTime, downTime);
            sendKeyEventIdentityCleared(keyCode, 1, downTime, SystemClock.uptimeMillis());
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void sendKeyEventIdentityCleared(int keyCode, int action, long downTime, long time) {
        KeyEvent event = KeyEvent.obtain(downTime, time, action, keyCode, 0, 0, -1, 0, 8, 257, null);
        InputManager.getInstance().injectInputEvent(event, 0);
        event.recycle();
    }

    private void expandNotifications() {
        long token = Binder.clearCallingIdentity();
        try {
            StatusBarManager statusBarManager = (StatusBarManager) this.mContext.getSystemService("statusbar");
            statusBarManager.expandNotificationsPanel();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void expandQuickSettings() {
        long token = Binder.clearCallingIdentity();
        try {
            StatusBarManager statusBarManager = (StatusBarManager) this.mContext.getSystemService("statusbar");
            statusBarManager.expandSettingsPanel();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean openRecents() {
        long token = Binder.clearCallingIdentity();
        try {
            StatusBarManagerInternal statusBarService = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            if (statusBarService != null) {
                statusBarService.toggleRecentApps();
                Binder.restoreCallingIdentity(token);
                return true;
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void showGlobalActions() {
        this.mWindowManagerService.showGlobalActions();
    }

    private boolean lockScreen() {
        ((PowerManager) this.mContext.getSystemService(PowerManager.class)).goToSleep(SystemClock.uptimeMillis(), 7, 0);
        this.mWindowManagerService.lockNow();
        return true;
    }

    private boolean takeScreenshot() {
        Supplier<ScreenshotHelper> supplier = this.mScreenshotHelperSupplier;
        ScreenshotHelper screenshotHelper = supplier != null ? supplier.get() : new ScreenshotHelper(this.mContext);
        screenshotHelper.takeScreenshot(1, true, true, 4, new Handler(Looper.getMainLooper()), (Consumer) null);
        return true;
    }
}
