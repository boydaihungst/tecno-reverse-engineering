package com.android.server.print;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Icon;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.print.IPrintSpooler;
import android.print.IPrintSpoolerCallbacks;
import android.print.IPrintSpoolerClient;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.util.Slog;
import android.util.TimedRemoteCaller;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.job.controllers.JobStatus;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeoutException;
import libcore.io.IoUtils;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class RemotePrintSpooler {
    private static final long BIND_SPOOLER_SERVICE_TIMEOUT;
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "RemotePrintSpooler";
    private final PrintSpoolerCallbacks mCallbacks;
    private boolean mCanUnbind;
    private final Context mContext;
    private boolean mDestroyed;
    private final Intent mIntent;
    private boolean mIsBinding;
    private boolean mIsLowPriority;
    private IPrintSpooler mRemoteInstance;
    private final UserHandle mUserHandle;
    private final Object mLock = new Object();
    private final GetPrintJobInfosCaller mGetPrintJobInfosCaller = new GetPrintJobInfosCaller();
    private final GetPrintJobInfoCaller mGetPrintJobInfoCaller = new GetPrintJobInfoCaller();
    private final SetPrintJobStateCaller mSetPrintJobStatusCaller = new SetPrintJobStateCaller();
    private final SetPrintJobTagCaller mSetPrintJobTagCaller = new SetPrintJobTagCaller();
    private final OnCustomPrinterIconLoadedCaller mCustomPrinterIconLoadedCaller = new OnCustomPrinterIconLoadedCaller();
    private final ClearCustomPrinterIconCacheCaller mClearCustomPrinterIconCache = new ClearCustomPrinterIconCacheCaller();
    private final GetCustomPrinterIconCaller mGetCustomPrinterIconCaller = new GetCustomPrinterIconCaller();
    private final ServiceConnection mServiceConnection = new MyServiceConnection();
    private final PrintSpoolerClient mClient = new PrintSpoolerClient(this);

    /* loaded from: classes2.dex */
    public interface PrintSpoolerCallbacks {
        void onAllPrintJobsForServiceHandled(ComponentName componentName);

        void onPrintJobQueued(PrintJobInfo printJobInfo);

        void onPrintJobStateChanged(PrintJobInfo printJobInfo);
    }

    static {
        BIND_SPOOLER_SERVICE_TIMEOUT = Build.IS_ENG ? 120000L : JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
    }

    public RemotePrintSpooler(Context context, int userId, boolean lowPriority, PrintSpoolerCallbacks callbacks) {
        this.mContext = context;
        this.mUserHandle = new UserHandle(userId);
        this.mCallbacks = callbacks;
        this.mIsLowPriority = lowPriority;
        Intent intent = new Intent();
        this.mIntent = intent;
        intent.setComponent(new ComponentName("com.android.printspooler", "com.android.printspooler.model.PrintSpoolerService"));
    }

    public void increasePriority() {
        if (this.mIsLowPriority) {
            this.mIsLowPriority = false;
            synchronized (this.mLock) {
                throwIfDestroyedLocked();
                while (!this.mCanUnbind) {
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                        Slog.e(LOG_TAG, "Interrupted while waiting for operation to complete");
                    }
                }
                unbindLocked();
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [181=6] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    public final List<PrintJobInfo> getPrintJobInfos(ComponentName componentName, int state, int appId) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                List<PrintJobInfo> printJobInfos = this.mGetPrintJobInfosCaller.getPrintJobInfos(getRemoteInstanceLazy(), componentName, state, appId);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return printJobInfos;
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error getting print jobs.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return null;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [203=6] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:42:0x000e */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r0v3, resolved type: java.lang.Object */
    /* JADX DEBUG: Multi-variable search result rejected for r0v9, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    public final void createPrintJob(PrintJobInfo printJob) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        boolean z = true;
        z = true;
        try {
            try {
                getRemoteInstanceLazy().createPrintJob(printJob);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj = this.mLock;
                    obj.notifyAll();
                    z = obj;
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error creating print job.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj2 = this.mLock;
                    obj2.notifyAll();
                    z = obj2;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = z;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [227=6] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    public final void writePrintJobData(ParcelFileDescriptor fd, PrintJobId printJobId) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                getRemoteInstanceLazy().writePrintJobData(fd, printJobId);
                IoUtils.closeQuietly(fd);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error writing print job data.", e);
                IoUtils.closeQuietly(fd);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly(fd);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [249=6] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    public final PrintJobInfo getPrintJobInfo(PrintJobId printJobId, int appId) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                PrintJobInfo printJobInfo = this.mGetPrintJobInfoCaller.getPrintJobInfo(getRemoteInstanceLazy(), printJobId, appId);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return printJobInfo;
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error getting print job info.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return null;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [272=6] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    public final boolean setPrintJobState(PrintJobId printJobId, int state, String error) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                boolean printJobState = this.mSetPrintJobStatusCaller.setPrintJobState(getRemoteInstanceLazy(), printJobId, state, error);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return printJobState;
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error setting print job state.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return false;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [301=6] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:42:0x000e */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r0v3, resolved type: java.lang.Object */
    /* JADX DEBUG: Multi-variable search result rejected for r0v9, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    public final void setProgress(PrintJobId printJobId, float progress) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        boolean z = true;
        z = true;
        try {
            try {
                getRemoteInstanceLazy().setProgress(printJobId, progress);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj = this.mLock;
                    obj.notifyAll();
                    z = obj;
                }
            } catch (RemoteException | InterruptedException | TimeoutException re) {
                Slog.e(LOG_TAG, "Error setting progress.", re);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj2 = this.mLock;
                    obj2.notifyAll();
                    z = obj2;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = z;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [328=6] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:42:0x000e */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r0v3, resolved type: java.lang.Object */
    /* JADX DEBUG: Multi-variable search result rejected for r0v9, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    public final void setStatus(PrintJobId printJobId, CharSequence status) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        boolean z = true;
        z = true;
        try {
            try {
                getRemoteInstanceLazy().setStatus(printJobId, status);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj = this.mLock;
                    obj.notifyAll();
                    z = obj;
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error setting status.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj2 = this.mLock;
                    obj2.notifyAll();
                    z = obj2;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = z;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [357=6] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:42:0x000e */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r0v3, resolved type: java.lang.Object */
    /* JADX DEBUG: Multi-variable search result rejected for r0v9, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    public final void setStatus(PrintJobId printJobId, int status, CharSequence appPackageName) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        boolean z = true;
        z = true;
        try {
            try {
                getRemoteInstanceLazy().setStatusRes(printJobId, status, appPackageName);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj = this.mLock;
                    obj.notifyAll();
                    z = obj;
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error setting status.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj2 = this.mLock;
                    obj2.notifyAll();
                    z = obj2;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = z;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [388=6] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    public final void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                this.mCustomPrinterIconLoadedCaller.onCustomPrinterIconLoaded(getRemoteInstanceLazy(), printerId, icon);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (RemoteException | InterruptedException | TimeoutException re) {
                Slog.e(LOG_TAG, "Error loading new custom printer icon.", re);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [421=6] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    public final Icon getCustomPrinterIcon(PrinterId printerId) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                Icon customPrinterIcon = this.mGetCustomPrinterIconCaller.getCustomPrinterIcon(getRemoteInstanceLazy(), printerId);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return customPrinterIcon;
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error getting custom printer icon.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return null;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [447=6] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    public void clearCustomPrinterIconCache() {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                this.mClearCustomPrinterIconCache.clearCustomPrinterIconCache(getRemoteInstanceLazy());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error clearing custom printer icon cache.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [469=6] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    public final boolean setPrintJobTag(PrintJobId printJobId, String tag) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            try {
                boolean printJobTag = this.mSetPrintJobTagCaller.setPrintJobTag(getRemoteInstanceLazy(), printJobId, tag);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
                return printJobTag;
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error setting print job tag.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return false;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [493=6] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:42:0x000e */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r0v3, resolved type: java.lang.Object */
    /* JADX DEBUG: Multi-variable search result rejected for r0v9, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    public final void setPrintJobCancelling(PrintJobId printJobId, boolean cancelling) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        boolean z = true;
        z = true;
        try {
            try {
                getRemoteInstanceLazy().setPrintJobCancelling(printJobId, cancelling);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj = this.mLock;
                    obj.notifyAll();
                    z = obj;
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error setting print job cancelling.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj2 = this.mLock;
                    obj2.notifyAll();
                    z = obj2;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = z;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [520=6] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:42:0x000e */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r0v3, resolved type: java.lang.Object */
    /* JADX DEBUG: Multi-variable search result rejected for r0v9, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    public final void pruneApprovedPrintServices(List<ComponentName> servicesToKeep) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        boolean z = true;
        z = true;
        try {
            try {
                getRemoteInstanceLazy().pruneApprovedPrintServices(servicesToKeep);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj = this.mLock;
                    obj.notifyAll();
                    z = obj;
                }
            } catch (RemoteException | InterruptedException | TimeoutException e) {
                Slog.e(LOG_TAG, "Error pruning approved print services.", e);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj2 = this.mLock;
                    obj2.notifyAll();
                    z = obj2;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = z;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [542=6] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:42:0x000e */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r0v3, resolved type: java.lang.Object */
    /* JADX DEBUG: Multi-variable search result rejected for r0v9, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    public final void removeObsoletePrintJobs() {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        boolean z = true;
        z = true;
        try {
            try {
                getRemoteInstanceLazy().removeObsoletePrintJobs();
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj = this.mLock;
                    obj.notifyAll();
                    z = obj;
                }
            } catch (RemoteException | InterruptedException | TimeoutException te) {
                Slog.e(LOG_TAG, "Error removing obsolete print jobs .", te);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    Object obj2 = this.mLock;
                    obj2.notifyAll();
                    z = obj2;
                }
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCanUnbind = z;
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public final void destroy() {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            unbindLocked();
            this.mDestroyed = true;
            this.mCanUnbind = false;
        }
    }

    public void dump(DualDumpOutputStream dumpStream) {
        synchronized (this.mLock) {
            dumpStream.write("is_destroyed", 1133871366145L, this.mDestroyed);
            dumpStream.write("is_bound", 1133871366146L, this.mRemoteInstance != null);
        }
        try {
            if (dumpStream.isProto()) {
                dumpStream.write((String) null, 1146756268035L, TransferPipe.dumpAsync(getRemoteInstanceLazy().asBinder(), new String[]{"--proto"}));
            } else {
                dumpStream.writeNested("internal_state", TransferPipe.dumpAsync(getRemoteInstanceLazy().asBinder(), new String[0]));
            }
        } catch (RemoteException | IOException | InterruptedException | TimeoutException e) {
            Slog.e(LOG_TAG, "Failed to dump remote instance", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAllPrintJobsHandled() {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            unbindLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPrintJobStateChanged(PrintJobInfo printJob) {
        this.mCallbacks.onPrintJobStateChanged(printJob);
    }

    private IPrintSpooler getRemoteInstanceLazy() throws TimeoutException, InterruptedException {
        synchronized (this.mLock) {
            IPrintSpooler iPrintSpooler = this.mRemoteInstance;
            if (iPrintSpooler != null) {
                return iPrintSpooler;
            }
            bindLocked();
            return this.mRemoteInstance;
        }
    }

    private void bindLocked() throws TimeoutException, InterruptedException {
        int flags;
        while (this.mIsBinding) {
            this.mLock.wait();
        }
        if (this.mRemoteInstance != null) {
            return;
        }
        this.mIsBinding = true;
        try {
            if (this.mIsLowPriority) {
                flags = 1;
            } else {
                flags = AudioFormat.AAC_MAIN;
            }
            this.mContext.bindServiceAsUser(this.mIntent, this.mServiceConnection, flags, this.mUserHandle);
            long startMillis = SystemClock.uptimeMillis();
            while (this.mRemoteInstance == null) {
                long elapsedMillis = SystemClock.uptimeMillis() - startMillis;
                long remainingMillis = BIND_SPOOLER_SERVICE_TIMEOUT - elapsedMillis;
                if (remainingMillis <= 0) {
                    throw new TimeoutException("Cannot get spooler!");
                }
                this.mLock.wait(remainingMillis);
            }
            this.mCanUnbind = true;
        } finally {
            this.mIsBinding = false;
            this.mLock.notifyAll();
        }
    }

    private void unbindLocked() {
        if (this.mRemoteInstance == null) {
            return;
        }
        while (!this.mCanUnbind) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
            }
        }
        clearClientLocked();
        this.mRemoteInstance = null;
        this.mContext.unbindService(this.mServiceConnection);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setClientLocked() {
        try {
            this.mRemoteInstance.setClient(this.mClient);
        } catch (RemoteException re) {
            Slog.d(LOG_TAG, "Error setting print spooler client", re);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearClientLocked() {
        try {
            this.mRemoteInstance.setClient((IPrintSpoolerClient) null);
        } catch (RemoteException re) {
            Slog.d(LOG_TAG, "Error clearing print spooler client", re);
        }
    }

    private void throwIfDestroyedLocked() {
        if (this.mDestroyed) {
            throw new IllegalStateException("Cannot interact with a destroyed instance.");
        }
    }

    private void throwIfCalledOnMainThread() {
        if (Thread.currentThread() == this.mContext.getMainLooper().getThread()) {
            throw new RuntimeException("Cannot invoke on the main thread");
        }
    }

    /* loaded from: classes2.dex */
    private final class MyServiceConnection implements ServiceConnection {
        private MyServiceConnection() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (RemotePrintSpooler.this.mLock) {
                RemotePrintSpooler.this.mRemoteInstance = IPrintSpooler.Stub.asInterface(service);
                RemotePrintSpooler.this.setClientLocked();
                RemotePrintSpooler.this.mLock.notifyAll();
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            synchronized (RemotePrintSpooler.this.mLock) {
                if (RemotePrintSpooler.this.mRemoteInstance != null) {
                    RemotePrintSpooler.this.clearClientLocked();
                    RemotePrintSpooler.this.mRemoteInstance = null;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class GetPrintJobInfosCaller extends TimedRemoteCaller<List<PrintJobInfo>> {
        private final IPrintSpoolerCallbacks mCallback;

        public GetPrintJobInfosCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() { // from class: com.android.server.print.RemotePrintSpooler.GetPrintJobInfosCaller.1
                @Override // com.android.server.print.RemotePrintSpooler.BasePrintSpoolerServiceCallbacks
                public void onGetPrintJobInfosResult(List<PrintJobInfo> printJobs, int sequence) {
                    GetPrintJobInfosCaller.this.onRemoteMethodResult(printJobs, sequence);
                }
            };
        }

        public List<PrintJobInfo> getPrintJobInfos(IPrintSpooler target, ComponentName componentName, int state, int appId) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.getPrintJobInfos(this.mCallback, componentName, state, appId, sequence);
            return (List) getResultTimed(sequence);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class GetPrintJobInfoCaller extends TimedRemoteCaller<PrintJobInfo> {
        private final IPrintSpoolerCallbacks mCallback;

        public GetPrintJobInfoCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() { // from class: com.android.server.print.RemotePrintSpooler.GetPrintJobInfoCaller.1
                @Override // com.android.server.print.RemotePrintSpooler.BasePrintSpoolerServiceCallbacks
                public void onGetPrintJobInfoResult(PrintJobInfo printJob, int sequence) {
                    GetPrintJobInfoCaller.this.onRemoteMethodResult(printJob, sequence);
                }
            };
        }

        public PrintJobInfo getPrintJobInfo(IPrintSpooler target, PrintJobId printJobId, int appId) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.getPrintJobInfo(printJobId, this.mCallback, appId, sequence);
            return (PrintJobInfo) getResultTimed(sequence);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class SetPrintJobStateCaller extends TimedRemoteCaller<Boolean> {
        private final IPrintSpoolerCallbacks mCallback;

        public SetPrintJobStateCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() { // from class: com.android.server.print.RemotePrintSpooler.SetPrintJobStateCaller.1
                @Override // com.android.server.print.RemotePrintSpooler.BasePrintSpoolerServiceCallbacks
                public void onSetPrintJobStateResult(boolean success, int sequence) {
                    SetPrintJobStateCaller.this.onRemoteMethodResult(Boolean.valueOf(success), sequence);
                }
            };
        }

        public boolean setPrintJobState(IPrintSpooler target, PrintJobId printJobId, int status, String error) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.setPrintJobState(printJobId, status, error, this.mCallback, sequence);
            return ((Boolean) getResultTimed(sequence)).booleanValue();
        }
    }

    /* loaded from: classes2.dex */
    private static final class SetPrintJobTagCaller extends TimedRemoteCaller<Boolean> {
        private final IPrintSpoolerCallbacks mCallback;

        public SetPrintJobTagCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() { // from class: com.android.server.print.RemotePrintSpooler.SetPrintJobTagCaller.1
                @Override // com.android.server.print.RemotePrintSpooler.BasePrintSpoolerServiceCallbacks
                public void onSetPrintJobTagResult(boolean success, int sequence) {
                    SetPrintJobTagCaller.this.onRemoteMethodResult(Boolean.valueOf(success), sequence);
                }
            };
        }

        public boolean setPrintJobTag(IPrintSpooler target, PrintJobId printJobId, String tag) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.setPrintJobTag(printJobId, tag, this.mCallback, sequence);
            return ((Boolean) getResultTimed(sequence)).booleanValue();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class OnCustomPrinterIconLoadedCaller extends TimedRemoteCaller<Void> {
        private final IPrintSpoolerCallbacks mCallback;

        public OnCustomPrinterIconLoadedCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() { // from class: com.android.server.print.RemotePrintSpooler.OnCustomPrinterIconLoadedCaller.1
                @Override // com.android.server.print.RemotePrintSpooler.BasePrintSpoolerServiceCallbacks
                public void onCustomPrinterIconCached(int sequence) {
                    OnCustomPrinterIconLoadedCaller.this.onRemoteMethodResult(null, sequence);
                }
            };
        }

        public Void onCustomPrinterIconLoaded(IPrintSpooler target, PrinterId printerId, Icon icon) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.onCustomPrinterIconLoaded(printerId, icon, this.mCallback, sequence);
            return (Void) getResultTimed(sequence);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class ClearCustomPrinterIconCacheCaller extends TimedRemoteCaller<Void> {
        private final IPrintSpoolerCallbacks mCallback;

        public ClearCustomPrinterIconCacheCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() { // from class: com.android.server.print.RemotePrintSpooler.ClearCustomPrinterIconCacheCaller.1
                @Override // com.android.server.print.RemotePrintSpooler.BasePrintSpoolerServiceCallbacks
                public void customPrinterIconCacheCleared(int sequence) {
                    ClearCustomPrinterIconCacheCaller.this.onRemoteMethodResult(null, sequence);
                }
            };
        }

        public Void clearCustomPrinterIconCache(IPrintSpooler target) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.clearCustomPrinterIconCache(this.mCallback, sequence);
            return (Void) getResultTimed(sequence);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class GetCustomPrinterIconCaller extends TimedRemoteCaller<Icon> {
        private final IPrintSpoolerCallbacks mCallback;

        public GetCustomPrinterIconCaller() {
            super(5000L);
            this.mCallback = new BasePrintSpoolerServiceCallbacks() { // from class: com.android.server.print.RemotePrintSpooler.GetCustomPrinterIconCaller.1
                @Override // com.android.server.print.RemotePrintSpooler.BasePrintSpoolerServiceCallbacks
                public void onGetCustomPrinterIconResult(Icon icon, int sequence) {
                    GetCustomPrinterIconCaller.this.onRemoteMethodResult(icon, sequence);
                }
            };
        }

        public Icon getCustomPrinterIcon(IPrintSpooler target, PrinterId printerId) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.getCustomPrinterIcon(printerId, this.mCallback, sequence);
            return (Icon) getResultTimed(sequence);
        }
    }

    /* loaded from: classes2.dex */
    private static abstract class BasePrintSpoolerServiceCallbacks extends IPrintSpoolerCallbacks.Stub {
        private BasePrintSpoolerServiceCallbacks() {
        }

        public void onGetPrintJobInfosResult(List<PrintJobInfo> printJobIds, int sequence) {
        }

        public void onGetPrintJobInfoResult(PrintJobInfo printJob, int sequence) {
        }

        public void onCancelPrintJobResult(boolean canceled, int sequence) {
        }

        public void onSetPrintJobStateResult(boolean success, int sequece) {
        }

        public void onSetPrintJobTagResult(boolean success, int sequence) {
        }

        public void onCustomPrinterIconCached(int sequence) {
        }

        public void onGetCustomPrinterIconResult(Icon icon, int sequence) {
        }

        public void customPrinterIconCacheCleared(int sequence) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class PrintSpoolerClient extends IPrintSpoolerClient.Stub {
        private final WeakReference<RemotePrintSpooler> mWeakSpooler;

        public PrintSpoolerClient(RemotePrintSpooler spooler) {
            this.mWeakSpooler = new WeakReference<>(spooler);
        }

        public void onPrintJobQueued(PrintJobInfo printJob) {
            RemotePrintSpooler spooler = this.mWeakSpooler.get();
            if (spooler != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    spooler.mCallbacks.onPrintJobQueued(printJob);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onAllPrintJobsForServiceHandled(ComponentName printService) {
            RemotePrintSpooler spooler = this.mWeakSpooler.get();
            if (spooler != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    spooler.mCallbacks.onAllPrintJobsForServiceHandled(printService);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onAllPrintJobsHandled() {
            RemotePrintSpooler spooler = this.mWeakSpooler.get();
            if (spooler != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    spooler.onAllPrintJobsHandled();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onPrintJobStateChanged(PrintJobInfo printJob) {
            RemotePrintSpooler spooler = this.mWeakSpooler.get();
            if (spooler != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    spooler.onPrintJobStateChanged(printJob);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }
}
