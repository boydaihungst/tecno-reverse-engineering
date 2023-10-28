package com.android.server.inputmethod;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManagerInternal;
import android.content.res.Resources;
import android.graphics.Matrix;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import android.view.IWindowManager;
import android.view.InputChannel;
import android.view.inputmethod.InputMethodInfo;
import com.android.internal.inputmethod.InputBindResult;
import com.android.internal.view.IInputMethod;
import com.android.internal.view.IInputMethodSession;
import com.android.server.EventLogTags;
import com.android.server.inputmethod.InputMethodUtils;
import com.android.server.wm.WindowManagerInternal;
import com.transsion.hubcore.server.inputmethod.ITranInputMethodBindingController;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class InputMethodBindingController {
    static final boolean DEBUG = false;
    private static final int IME_CONNECTION_BIND_FLAGS = 1082130437;
    private static final int IME_VISIBLE_BIND_FLAGS = 738725889;
    private static final String TAG = InputMethodBindingController.class.getSimpleName();
    static final long TIME_TO_RECONNECT = 3000;
    private final Context mContext;
    private String mCurId;
    private Intent mCurIntent;
    private IInputMethodInvoker mCurMethod;
    private int mCurSeq;
    private IBinder mCurToken;
    private boolean mHasConnection;
    private final IWindowManager mIWindowManager;
    private long mLastBindTime;
    private final ArrayMap<String, InputMethodInfo> mMethodMap;
    private final PackageManagerInternal mPackageManagerInternal;
    private final Resources mRes;
    private String mSelectedMethodId;
    private final InputMethodManagerService mService;
    private final InputMethodUtils.InputMethodSettings mSettings;
    private boolean mSupportsStylusHw;
    private boolean mVisibleBound;
    private final WindowManagerInternal mWindowManagerInternal;
    private int mCurMethodUid = -1;
    private final ServiceConnection mVisibleConnection = new ServiceConnection() { // from class: com.android.server.inputmethod.InputMethodBindingController.1
        @Override // android.content.ServiceConnection
        public void onBindingDied(ComponentName name) {
            synchronized (ImfLock.class) {
                InputMethodBindingController.this.mService.invalidateAutofillSessionLocked();
                if (InputMethodBindingController.this.mVisibleBound) {
                    InputMethodBindingController.this.unbindVisibleConnection();
                }
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            synchronized (ImfLock.class) {
                InputMethodBindingController.this.mService.invalidateAutofillSessionLocked();
            }
        }
    };
    private final ServiceConnection mMainConnection = new ServiceConnection() { // from class: com.android.server.inputmethod.InputMethodBindingController.2
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            Trace.traceBegin(32L, "IMMS.onServiceConnected");
            synchronized (ImfLock.class) {
                if (InputMethodBindingController.this.mCurIntent != null && name.equals(InputMethodBindingController.this.mCurIntent.getComponent())) {
                    InputMethodBindingController.this.mCurMethod = IInputMethodInvoker.create(IInputMethod.Stub.asInterface(service));
                    updateCurrentMethodUid();
                    if (InputMethodBindingController.this.mCurToken == null) {
                        Slog.w(InputMethodBindingController.TAG, "Service connected without a token!");
                        InputMethodBindingController.this.unbindCurrentMethod();
                        Trace.traceEnd(32L);
                        return;
                    }
                    InputMethodInfo info = (InputMethodInfo) InputMethodBindingController.this.mMethodMap.get(InputMethodBindingController.this.mSelectedMethodId);
                    InputMethodBindingController.this.mSupportsStylusHw = info.supportsStylusHandwriting();
                    InputMethodBindingController.this.mService.initializeImeLocked(InputMethodBindingController.this.mCurMethod, InputMethodBindingController.this.mCurToken, info.getConfigChanges(), InputMethodBindingController.this.mSupportsStylusHw);
                    InputMethodBindingController.this.mService.scheduleNotifyImeUidToAudioService(InputMethodBindingController.this.mCurMethodUid);
                    InputMethodBindingController.this.mService.reRequestCurrentClientSessionLocked();
                    InputMethodBindingController.this.mService.performOnCreateInlineSuggestionsRequestLocked();
                }
                InputMethodBindingController.this.mService.scheduleResetStylusHandwriting();
                Trace.traceEnd(32L);
            }
        }

        private void updateCurrentMethodUid() {
            String curMethodPackage = InputMethodBindingController.this.mCurIntent.getComponent().getPackageName();
            int curMethodUid = InputMethodBindingController.this.mPackageManagerInternal.getPackageUid(curMethodPackage, 0L, InputMethodBindingController.this.mSettings.getCurrentUserId());
            if (curMethodUid < 0) {
                Slog.e(InputMethodBindingController.TAG, "Failed to get UID for package=" + curMethodPackage);
                InputMethodBindingController.this.mCurMethodUid = -1;
                return;
            }
            InputMethodBindingController.this.mCurMethodUid = curMethodUid;
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            synchronized (ImfLock.class) {
                if (InputMethodBindingController.this.mCurMethod != null && InputMethodBindingController.this.mCurIntent != null && name.equals(InputMethodBindingController.this.mCurIntent.getComponent())) {
                    InputMethodBindingController.this.mLastBindTime = SystemClock.uptimeMillis();
                    InputMethodBindingController.this.clearCurMethodAndSessions();
                    InputMethodBindingController.this.mService.clearInputShowRequestLocked();
                    InputMethodBindingController.this.mService.unbindCurrentClientLocked(3);
                }
            }
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputMethodBindingController(InputMethodManagerService service) {
        this.mService = service;
        this.mContext = service.mContext;
        this.mMethodMap = service.mMethodMap;
        this.mSettings = service.mSettings;
        this.mPackageManagerInternal = service.mPackageManagerInternal;
        this.mIWindowManager = service.mIWindowManager;
        this.mWindowManagerInternal = service.mWindowManagerInternal;
        this.mRes = service.mRes;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastBindTime() {
        return this.mLastBindTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasConnection() {
        return this.mHasConnection;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getCurId() {
        return this.mCurId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getSelectedMethodId() {
        return this.mSelectedMethodId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSelectedMethodId(String selectedMethodId) {
        this.mSelectedMethodId = selectedMethodId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IBinder getCurToken() {
        return this.mCurToken;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Intent getCurIntent() {
        return this.mCurIntent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSequenceNumber() {
        return this.mCurSeq;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void advanceSequenceNumber() {
        int i = this.mCurSeq + 1;
        this.mCurSeq = i;
        if (i <= 0) {
            this.mCurSeq = 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IInputMethodInvoker getCurMethod() {
        return this.mCurMethod;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurMethodUid() {
        return this.mCurMethodUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVisibleBound() {
        return this.mVisibleBound;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean supportsStylusHandwriting() {
        return this.mSupportsStylusHw;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unbindCurrentMethod() {
        if (this.mVisibleBound) {
            unbindVisibleConnection();
        }
        if (this.mHasConnection) {
            unbindMainConnection();
        }
        if (this.mCurToken != null) {
            removeCurrentToken();
            this.mService.resetSystemUiLocked();
        }
        this.mCurId = null;
        clearCurMethodAndSessions();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearCurMethodAndSessions() {
        this.mService.clearClientSessionsLocked();
        this.mCurMethod = null;
        this.mCurMethodUid = -1;
    }

    private void removeCurrentToken() {
        int curTokenDisplayId = this.mService.getCurTokenDisplayIdLocked();
        this.mWindowManagerInternal.removeWindowToken(this.mCurToken, false, false, curTokenDisplayId);
        this.mCurToken = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputBindResult bindCurrentMethod() {
        String str = this.mSelectedMethodId;
        if (str == null) {
            Slog.e(TAG, "mSelectedMethodId is null!");
            return InputBindResult.NO_IME;
        }
        InputMethodInfo info = this.mMethodMap.get(str);
        if (info == null) {
            throw new IllegalArgumentException("Unknown id: " + this.mSelectedMethodId);
        }
        this.mCurIntent = createImeBindingIntent(info.getComponent());
        if (bindCurrentInputMethodServiceMainConnection()) {
            this.mCurId = info.getId();
            this.mLastBindTime = SystemClock.uptimeMillis();
            addFreshWindowToken();
            return new InputBindResult(2, (IInputMethodSession) null, (SparseArray) null, (InputChannel) null, this.mCurId, this.mCurSeq, (Matrix) null, false);
        }
        Slog.w("InputMethodManagerService", "Failure connecting to input method service: " + this.mCurIntent);
        this.mCurIntent = null;
        return InputBindResult.IME_NOT_CONNECTED;
    }

    private Intent createImeBindingIntent(ComponentName component) {
        Intent intent = new Intent("android.view.InputMethod");
        intent.setComponent(component);
        intent.putExtra("android.intent.extra.client_label", 17040492);
        intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivity(this.mContext, 0, new Intent("android.settings.INPUT_METHOD_SETTINGS"), 67108864));
        return intent;
    }

    private void addFreshWindowToken() {
        int displayIdToShowIme = this.mService.getDisplayIdToShowImeLocked();
        this.mCurToken = new Binder();
        this.mService.setCurTokenDisplayIdLocked(displayIdToShowIme);
        try {
            this.mIWindowManager.addWindowToken(this.mCurToken, 2011, displayIdToShowIme, (Bundle) null);
        } catch (RemoteException e) {
            Slog.e(TAG, "Could not add window token " + this.mCurToken + " for display " + displayIdToShowIme, e);
        }
    }

    private void unbindMainConnection() {
        this.mContext.unbindService(this.mMainConnection);
        this.mHasConnection = false;
    }

    void unbindVisibleConnection() {
        this.mContext.unbindService(this.mVisibleConnection);
        this.mVisibleBound = false;
    }

    private boolean bindCurrentInputMethodService(ServiceConnection conn, int flags) {
        Intent intent = this.mCurIntent;
        if (intent == null || conn == null) {
            Slog.e(TAG, "--- bind failed: service = " + this.mCurIntent + ", conn = " + conn);
            return false;
        }
        return this.mContext.bindServiceAsUser(intent, conn, flags, new UserHandle(this.mSettings.getCurrentUserId()));
    }

    private boolean bindCurrentInputMethodServiceVisibleConnection() {
        boolean bindCurrentInputMethodService = bindCurrentInputMethodService(this.mVisibleConnection, IME_VISIBLE_BIND_FLAGS);
        this.mVisibleBound = bindCurrentInputMethodService;
        return bindCurrentInputMethodService;
    }

    private boolean bindCurrentInputMethodServiceMainConnection() {
        boolean bindCurrentInputMethodService = bindCurrentInputMethodService(this.mMainConnection, IME_CONNECTION_BIND_FLAGS);
        this.mHasConnection = bindCurrentInputMethodService;
        return bindCurrentInputMethodService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurrentMethodVisible() {
        if (this.mCurMethod != null) {
            if (this.mHasConnection && !this.mVisibleBound) {
                ITranInputMethodBindingController.Instance().hookSetCurrentMethodVisible(this.mCurIntent);
                bindCurrentInputMethodServiceVisibleConnection();
            }
        } else if (!this.mHasConnection) {
            bindCurrentMethod();
        } else {
            long bindingDuration = SystemClock.uptimeMillis() - this.mLastBindTime;
            if (bindingDuration >= 3000) {
                EventLog.writeEvent((int) EventLogTags.IMF_FORCE_RECONNECT_IME, getSelectedMethodId(), Long.valueOf(bindingDuration), 1);
                Slog.w(TAG, "Force disconnect/connect to the IME in setCurrentMethodVisible()");
                unbindMainConnection();
                ITranInputMethodBindingController.Instance().hookSetCurrentMethodVisible(this.mCurIntent);
                bindCurrentInputMethodServiceMainConnection();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurrentMethodNotVisible() {
        if (this.mVisibleBound) {
            unbindVisibleConnection();
        }
    }
}
