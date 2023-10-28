package android.app.timezone;

import android.app.timezone.ICallback;
import android.app.timezone.IRulesManager;
import android.app.timezone.RulesManager;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
/* loaded from: classes.dex */
public final class RulesManager {
    public static final String ACTION_RULES_UPDATE_OPERATION = "com.android.intent.action.timezone.RULES_UPDATE_OPERATION";
    private static final boolean DEBUG = false;
    public static final int ERROR_OPERATION_IN_PROGRESS = 1;
    public static final int ERROR_UNKNOWN_FAILURE = 2;
    public static final String EXTRA_OPERATION_STAGED = "staged";
    public static final int SUCCESS = 0;
    private static final String TAG = "timezone.RulesManager";
    private final Context mContext;
    private final IRulesManager mIRulesManager = IRulesManager.Stub.asInterface(ServiceManager.getService(Context.TIME_ZONE_RULES_MANAGER_SERVICE));

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface ResultCode {
    }

    public RulesManager(Context context) {
        this.mContext = context;
    }

    public RulesState getRulesState() {
        try {
            logDebug("mIRulesManager.getRulesState()");
            RulesState rulesState = this.mIRulesManager.getRulesState();
            logDebug("mIRulesManager.getRulesState() returned " + rulesState);
            return rulesState;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int requestInstall(ParcelFileDescriptor distroFileDescriptor, byte[] checkToken, Callback callback) throws IOException {
        ICallback iCallback = new CallbackWrapper(this.mContext, callback);
        try {
            logDebug("mIRulesManager.requestInstall()");
            return this.mIRulesManager.requestInstall(distroFileDescriptor, checkToken, iCallback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int requestUninstall(byte[] checkToken, Callback callback) {
        ICallback iCallback = new CallbackWrapper(this.mContext, callback);
        try {
            logDebug("mIRulesManager.requestUninstall()");
            return this.mIRulesManager.requestUninstall(checkToken, iCallback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class CallbackWrapper extends ICallback.Stub {
        final Callback mCallback;
        final Handler mHandler;

        CallbackWrapper(Context context, Callback callback) {
            this.mCallback = callback;
            this.mHandler = new Handler(context.getMainLooper());
        }

        @Override // android.app.timezone.ICallback
        public void onFinished(final int status) {
            RulesManager.logDebug("mCallback.onFinished(status), status=" + status);
            this.mHandler.post(new Runnable() { // from class: android.app.timezone.RulesManager$CallbackWrapper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    RulesManager.CallbackWrapper.this.m656xf0fa82c0(status);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onFinished$0$android-app-timezone-RulesManager$CallbackWrapper  reason: not valid java name */
        public /* synthetic */ void m656xf0fa82c0(int status) {
            this.mCallback.onFinished(status);
        }
    }

    public void requestNothing(byte[] checkToken, boolean succeeded) {
        try {
            logDebug("mIRulesManager.requestNothing() with token=" + Arrays.toString(checkToken));
            this.mIRulesManager.requestNothing(checkToken, succeeded);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    static void logDebug(String msg) {
    }
}
