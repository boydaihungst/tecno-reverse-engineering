package com.android.server.inputmethod;

import android.os.Binder;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Slog;
import android.view.InputChannel;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputMethodSubtype;
import android.window.ImeOnBackInvokedDispatcher;
import com.android.internal.inputmethod.IInputMethodPrivilegedOperations;
import com.android.internal.view.IInlineSuggestionsRequestCallback;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethod;
import com.android.internal.view.IInputMethodSession;
import com.android.internal.view.IInputSessionCallback;
import com.android.internal.view.InlineSuggestionsRequestInfo;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class IInputMethodInvoker {
    private static final boolean DEBUG = InputMethodManagerService.DEBUG;
    private static final String TAG = "InputMethodManagerService";
    private final IInputMethod mTarget;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static IInputMethodInvoker create(IInputMethod inputMethod) {
        if (inputMethod == null) {
            return null;
        }
        if (!Binder.isProxy(inputMethod)) {
            throw new UnsupportedOperationException(inputMethod + " must have been a BinderProxy.");
        }
        return new IInputMethodInvoker(inputMethod);
    }

    private static String getCallerMethodName() {
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        if (callStack.length <= 4) {
            return "<bottom of call stack>";
        }
        return callStack[4].getMethodName();
    }

    private static void logRemoteException(RemoteException e) {
        if (DEBUG || !(e instanceof DeadObjectException)) {
            Slog.w(TAG, "IPC failed at IInputMethodInvoker#" + getCallerMethodName(), e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getBinderIdentityHashCode(IInputMethodInvoker obj) {
        if (obj == null) {
            return 0;
        }
        return System.identityHashCode(obj.mTarget);
    }

    private IInputMethodInvoker(IInputMethod target) {
        this.mTarget = target;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IBinder asBinder() {
        return this.mTarget.asBinder();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initializeInternal(IBinder token, IInputMethodPrivilegedOperations privOps, int configChanges, boolean stylusHwSupported, int navButtonFlags) {
        try {
            this.mTarget.initializeInternal(token, privOps, configChanges, stylusHwSupported, navButtonFlags);
        } catch (RemoteException e) {
            logRemoteException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCreateInlineSuggestionsRequest(InlineSuggestionsRequestInfo requestInfo, IInlineSuggestionsRequestCallback cb) {
        try {
            this.mTarget.onCreateInlineSuggestionsRequest(requestInfo, cb);
        } catch (RemoteException e) {
            logRemoteException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void bindInput(InputBinding binding) {
        try {
            this.mTarget.bindInput(binding);
        } catch (RemoteException e) {
            logRemoteException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unbindInput() {
        try {
            this.mTarget.unbindInput();
        } catch (RemoteException e) {
            logRemoteException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startInput(IBinder startInputToken, IInputContext inputContext, EditorInfo attribute, boolean restarting, int navButtonFlags, ImeOnBackInvokedDispatcher imeDispatcher) {
        try {
            this.mTarget.startInput(startInputToken, inputContext, attribute, restarting, navButtonFlags, imeDispatcher);
        } catch (RemoteException e) {
            logRemoteException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onNavButtonFlagsChanged(int navButtonFlags) {
        try {
            this.mTarget.onNavButtonFlagsChanged(navButtonFlags);
        } catch (RemoteException e) {
            logRemoteException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void createSession(InputChannel channel, IInputSessionCallback callback) {
        try {
            this.mTarget.createSession(channel, callback);
        } catch (RemoteException e) {
            logRemoteException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSessionEnabled(IInputMethodSession session, boolean enabled) {
        try {
            this.mTarget.setSessionEnabled(session, enabled);
        } catch (RemoteException e) {
            logRemoteException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean showSoftInput(IBinder showInputToken, int flags, ResultReceiver resultReceiver) {
        try {
            this.mTarget.showSoftInput(showInputToken, flags, resultReceiver);
            return true;
        } catch (RemoteException e) {
            logRemoteException(e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hideSoftInput(IBinder hideInputToken, int flags, ResultReceiver resultReceiver) {
        try {
            this.mTarget.hideSoftInput(hideInputToken, flags, resultReceiver);
            return true;
        } catch (RemoteException e) {
            logRemoteException(e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void changeInputMethodSubtype(InputMethodSubtype subtype) {
        try {
            this.mTarget.changeInputMethodSubtype(subtype);
        } catch (RemoteException e) {
            logRemoteException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void canStartStylusHandwriting(int requestId) {
        try {
            this.mTarget.canStartStylusHandwriting(requestId);
        } catch (RemoteException e) {
            logRemoteException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean startStylusHandwriting(int requestId, InputChannel channel, List<MotionEvent> events) {
        try {
            this.mTarget.startStylusHandwriting(requestId, channel, events);
            return true;
        } catch (RemoteException e) {
            logRemoteException(e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initInkWindow() {
        try {
            this.mTarget.initInkWindow();
        } catch (RemoteException e) {
            logRemoteException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishStylusHandwriting() {
        try {
            this.mTarget.finishStylusHandwriting();
        } catch (RemoteException e) {
            logRemoteException(e);
        }
    }
}
