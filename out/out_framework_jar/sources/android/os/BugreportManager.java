package android.os;

import android.annotation.SystemApi;
import android.app.ActivityManager;
import android.content.Context;
import android.os.BugreportManager;
import android.os.IDumpstateListener;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.R;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public final class BugreportManager {
    private static final String TAG = "BugreportManager";
    private final IDumpstate mBinder;
    private final Context mContext;

    public BugreportManager(Context context, IDumpstate binder) {
        this.mContext = context;
        this.mBinder = binder;
    }

    /* loaded from: classes2.dex */
    public static abstract class BugreportCallback {
        public static final int BUGREPORT_ERROR_ANOTHER_REPORT_IN_PROGRESS = 5;
        public static final int BUGREPORT_ERROR_INVALID_INPUT = 1;
        public static final int BUGREPORT_ERROR_RUNTIME = 2;
        public static final int BUGREPORT_ERROR_USER_CONSENT_TIMED_OUT = 4;
        public static final int BUGREPORT_ERROR_USER_DENIED_CONSENT = 3;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes2.dex */
        public @interface BugreportErrorCode {
        }

        public void onProgress(float progress) {
        }

        public void onError(int errorCode) {
        }

        public void onFinished() {
        }

        public void onEarlyReportFinished() {
        }
    }

    @SystemApi
    public void startBugreport(ParcelFileDescriptor bugreportFd, ParcelFileDescriptor screenshotFd, BugreportParams params, Executor executor, BugreportCallback callback) {
        try {
            try {
                try {
                    Preconditions.checkNotNull(bugreportFd);
                    Preconditions.checkNotNull(params);
                    Preconditions.checkNotNull(executor);
                    Preconditions.checkNotNull(callback);
                    boolean isScreenshotRequested = screenshotFd != null;
                    if (screenshotFd == null) {
                        screenshotFd = ParcelFileDescriptor.open(new File("/dev/null"), 268435456);
                    }
                    DumpstateListener dsListener = new DumpstateListener(executor, callback, isScreenshotRequested);
                    this.mBinder.startBugreport(-1, this.mContext.getOpPackageName(), bugreportFd.getFileDescriptor(), screenshotFd.getFileDescriptor(), params.getMode(), dsListener, isScreenshotRequested);
                    IoUtils.closeQuietly(bugreportFd);
                    if (screenshotFd == null) {
                        return;
                    }
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            } catch (FileNotFoundException e2) {
                Log.wtf(TAG, "Not able to find /dev/null file: ", e2);
                IoUtils.closeQuietly(bugreportFd);
                if (screenshotFd == null) {
                    return;
                }
            }
            IoUtils.closeQuietly(screenshotFd);
        } catch (Throwable th) {
            IoUtils.closeQuietly(bugreportFd);
            if (screenshotFd != null) {
                IoUtils.closeQuietly(screenshotFd);
            }
            throw th;
        }
    }

    public void startConnectivityBugreport(ParcelFileDescriptor bugreportFd, Executor executor, BugreportCallback callback) {
        startBugreport(bugreportFd, null, new BugreportParams(4), executor, callback);
    }

    public void cancelBugreport() {
        try {
            this.mBinder.cancelBugreport(-1, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void requestBugreport(BugreportParams params, CharSequence shareTitle, CharSequence shareDescription) {
        String title;
        String description = null;
        if (shareTitle == null) {
            title = null;
        } else {
            try {
                title = shareTitle.toString();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        if (shareDescription != null) {
            description = shareDescription.toString();
        }
        ActivityManager.getService().requestBugReportWithDescription(title, description, params.getMode());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class DumpstateListener extends IDumpstateListener.Stub {
        private final BugreportCallback mCallback;
        private final Executor mExecutor;
        private final boolean mIsScreenshotRequested;

        DumpstateListener(Executor executor, BugreportCallback callback, boolean isScreenshotRequested) {
            this.mExecutor = executor;
            this.mCallback = callback;
            this.mIsScreenshotRequested = isScreenshotRequested;
        }

        @Override // android.os.IDumpstateListener
        public void onProgress(final int progress) throws RemoteException {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.os.BugreportManager$DumpstateListener$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        BugreportManager.DumpstateListener.this.m2970x36369bae(progress);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onProgress$0$android-os-BugreportManager$DumpstateListener  reason: not valid java name */
        public /* synthetic */ void m2970x36369bae(int progress) {
            this.mCallback.onProgress(progress);
        }

        @Override // android.os.IDumpstateListener
        public void onError(final int errorCode) throws RemoteException {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.os.BugreportManager$DumpstateListener$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        BugreportManager.DumpstateListener.this.m2968lambda$onError$1$androidosBugreportManager$DumpstateListener(errorCode);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onError$1$android-os-BugreportManager$DumpstateListener  reason: not valid java name */
        public /* synthetic */ void m2968lambda$onError$1$androidosBugreportManager$DumpstateListener(int errorCode) {
            this.mCallback.onError(errorCode);
        }

        @Override // android.os.IDumpstateListener
        public void onFinished() throws RemoteException {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.os.BugreportManager$DumpstateListener$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        BugreportManager.DumpstateListener.this.m2969x69916875();
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onFinished$2$android-os-BugreportManager$DumpstateListener  reason: not valid java name */
        public /* synthetic */ void m2969x69916875() {
            this.mCallback.onFinished();
        }

        @Override // android.os.IDumpstateListener
        public void onScreenshotTaken(final boolean success) throws RemoteException {
            if (!this.mIsScreenshotRequested) {
                return;
            }
            Handler mainThreadHandler = new Handler(Looper.getMainLooper());
            mainThreadHandler.post(new Runnable() { // from class: android.os.BugreportManager$DumpstateListener$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    BugreportManager.DumpstateListener.this.m2971x691707cd(success);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onScreenshotTaken$3$android-os-BugreportManager$DumpstateListener  reason: not valid java name */
        public /* synthetic */ void m2971x691707cd(boolean success) {
            int message;
            if (success) {
                message = R.string.bugreport_screenshot_success_toast;
            } else {
                message = R.string.bugreport_screenshot_failure_toast;
            }
            Toast.makeText(BugreportManager.this.mContext, message, 1).show();
        }

        @Override // android.os.IDumpstateListener
        public void onUiIntensiveBugreportDumpsFinished() throws RemoteException {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.os.BugreportManager$DumpstateListener$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        BugreportManager.DumpstateListener.this.m2972x7a380e5f();
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onUiIntensiveBugreportDumpsFinished$4$android-os-BugreportManager$DumpstateListener  reason: not valid java name */
        public /* synthetic */ void m2972x7a380e5f() {
            this.mCallback.onEarlyReportFinished();
        }
    }
}
