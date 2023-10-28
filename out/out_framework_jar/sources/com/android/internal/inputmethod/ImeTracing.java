package com.android.internal.inputmethod;

import android.app.ActivityThread;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.view.IInputMethodManager;
import java.io.PrintWriter;
/* loaded from: classes4.dex */
public abstract class ImeTracing {
    public static final int IME_TRACING_FROM_CLIENT = 0;
    public static final int IME_TRACING_FROM_IMMS = 2;
    public static final int IME_TRACING_FROM_IMS = 1;
    public static final String PROTO_ARG = "--proto-com-android-imetracing";
    static final String TAG = "imeTracing";
    static boolean sEnabled = false;
    private static ImeTracing sInstance;
    protected boolean mDumpInProgress;
    protected final Object mDumpInProgressLock = new Object();
    IInputMethodManager mService = IInputMethodManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.INPUT_METHOD_SERVICE));

    @FunctionalInterface
    /* loaded from: classes4.dex */
    public interface ServiceDumper {
        void dumpToProto(ProtoOutputStream protoOutputStream, byte[] bArr);
    }

    public abstract void addToBuffer(ProtoOutputStream protoOutputStream, int i);

    public abstract void startTrace(PrintWriter printWriter);

    public abstract void stopTrace(PrintWriter printWriter);

    public abstract void triggerClientDump(String str, InputMethodManager inputMethodManager, byte[] bArr);

    public abstract void triggerManagerServiceDump(String str);

    public abstract void triggerServiceDump(String str, ServiceDumper serviceDumper, byte[] bArr);

    public static ImeTracing getInstance() {
        if (sInstance == null) {
            try {
                sInstance = isSystemProcess() ? new ImeTracingServerImpl() : new ImeTracingClientImpl();
            } catch (RemoteException | ServiceManager.ServiceNotFoundException e) {
                Log.e(TAG, "Exception while creating ImeTracing instance", e);
            }
        }
        return sInstance;
    }

    public void sendToService(byte[] protoDump, int source, String where) throws RemoteException {
        this.mService.startProtoDump(protoDump, source, where);
    }

    public final void startImeTrace() {
        try {
            this.mService.startImeTrace();
        } catch (RemoteException e) {
            Log.e(TAG, "Could not start ime trace." + e);
        }
    }

    public final void stopImeTrace() {
        try {
            this.mService.stopImeTrace();
        } catch (RemoteException e) {
            Log.e(TAG, "Could not stop ime trace." + e);
        }
    }

    public void saveForBugreport(PrintWriter pw) {
    }

    public void setEnabled(boolean enabled) {
        sEnabled = enabled;
    }

    public boolean isEnabled() {
        return sEnabled;
    }

    public boolean isAvailable() {
        return this.mService != null;
    }

    private static boolean isSystemProcess() {
        return ActivityThread.isSystem();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void logAndPrintln(PrintWriter pw, String msg) {
        Log.i(TAG, msg);
        if (pw != null) {
            pw.println(msg);
            pw.flush();
        }
    }
}
