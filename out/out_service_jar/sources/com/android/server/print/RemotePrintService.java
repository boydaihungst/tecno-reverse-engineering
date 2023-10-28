package com.android.server.print;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ParceledListSlice;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.IPrintService;
import android.printservice.IPrintServiceClient;
import android.util.Slog;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.dump.DumpUtils;
import com.android.internal.util.function.pooled.PooledLambda;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class RemotePrintService implements IBinder.DeathRecipient {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "RemotePrintService";
    private boolean mBinding;
    private final PrintServiceCallbacks mCallbacks;
    private final ComponentName mComponentName;
    private final Context mContext;
    private boolean mDestroyed;
    private List<PrinterId> mDiscoveryPriorityList;
    private boolean mHasActivePrintJobs;
    private boolean mHasPrinterDiscoverySession;
    private final Intent mIntent;
    private IPrintService mPrintService;
    private boolean mServiceDied;
    private final RemotePrintSpooler mSpooler;
    private List<PrinterId> mTrackedPrinterList;
    private final int mUserId;
    private final Object mLock = new Object();
    private final List<Runnable> mPendingCommands = new ArrayList();
    private final ServiceConnection mServiceConnection = new RemoteServiceConneciton();
    private final RemotePrintServiceClient mPrintServiceClient = new RemotePrintServiceClient(this);

    /* loaded from: classes2.dex */
    public interface PrintServiceCallbacks {
        void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon);

        void onPrintersAdded(List<PrinterInfo> list);

        void onPrintersRemoved(List<PrinterId> list);

        void onServiceDied(RemotePrintService remotePrintService);
    }

    public RemotePrintService(Context context, ComponentName componentName, int userId, RemotePrintSpooler spooler, PrintServiceCallbacks callbacks) {
        this.mContext = context;
        this.mCallbacks = callbacks;
        this.mComponentName = componentName;
        this.mIntent = new Intent().setComponent(componentName);
        this.mUserId = userId;
        this.mSpooler = spooler;
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public void destroy() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((RemotePrintService) obj).handleDestroy();
            }
        }, this));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDestroy() {
        stopTrackingAllPrinters();
        if (this.mDiscoveryPriorityList != null) {
            handleStopPrinterDiscovery();
        }
        if (this.mHasPrinterDiscoverySession) {
            handleDestroyPrinterDiscoverySession();
        }
        ensureUnbound();
        this.mDestroyed = true;
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((RemotePrintService) obj).handleBinderDied();
            }
        }, this));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleBinderDied() {
        IPrintService iPrintService = this.mPrintService;
        if (iPrintService != null) {
            iPrintService.asBinder().unlinkToDeath(this, 0);
        }
        this.mPrintService = null;
        this.mServiceDied = true;
        this.mCallbacks.onServiceDied(this);
    }

    public void onAllPrintJobsHandled() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((RemotePrintService) obj).handleOnAllPrintJobsHandled();
            }
        }, this));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnAllPrintJobsHandled() {
        this.mHasActivePrintJobs = false;
        if (!isBound()) {
            if (this.mServiceDied && !this.mHasPrinterDiscoverySession) {
                ensureUnbound();
                return;
            }
            ensureBound();
            this.mPendingCommands.add(new Runnable() { // from class: com.android.server.print.RemotePrintService.1
                @Override // java.lang.Runnable
                public void run() {
                    RemotePrintService.this.handleOnAllPrintJobsHandled();
                }
            });
        } else if (!this.mHasPrinterDiscoverySession) {
            ensureUnbound();
        }
    }

    public void onRequestCancelPrintJob(PrintJobInfo printJob) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda12
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).handleRequestCancelPrintJob((PrintJobInfo) obj2);
            }
        }, this, printJob));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRequestCancelPrintJob(final PrintJobInfo printJob) {
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() { // from class: com.android.server.print.RemotePrintService.2
                @Override // java.lang.Runnable
                public void run() {
                    RemotePrintService.this.handleRequestCancelPrintJob(printJob);
                }
            });
            return;
        }
        try {
            this.mPrintService.requestCancelPrintJob(printJob);
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error canceling a pring job.", re);
        }
    }

    public void onPrintJobQueued(PrintJobInfo printJob) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda5
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).handleOnPrintJobQueued((PrintJobInfo) obj2);
            }
        }, this, printJob));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnPrintJobQueued(final PrintJobInfo printJob) {
        this.mHasActivePrintJobs = true;
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() { // from class: com.android.server.print.RemotePrintService.3
                @Override // java.lang.Runnable
                public void run() {
                    RemotePrintService.this.handleOnPrintJobQueued(printJob);
                }
            });
            return;
        }
        try {
            this.mPrintService.onPrintJobQueued(printJob);
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error announcing queued pring job.", re);
        }
    }

    public void createPrinterDiscoverySession() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((RemotePrintService) obj).handleCreatePrinterDiscoverySession();
            }
        }, this));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCreatePrinterDiscoverySession() {
        this.mHasPrinterDiscoverySession = true;
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() { // from class: com.android.server.print.RemotePrintService.4
                @Override // java.lang.Runnable
                public void run() {
                    RemotePrintService.this.handleCreatePrinterDiscoverySession();
                }
            });
            return;
        }
        try {
            this.mPrintService.createPrinterDiscoverySession();
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error creating printer discovery session.", re);
        }
    }

    public void destroyPrinterDiscoverySession() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda13
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((RemotePrintService) obj).handleDestroyPrinterDiscoverySession();
            }
        }, this));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDestroyPrinterDiscoverySession() {
        this.mHasPrinterDiscoverySession = false;
        if (!isBound()) {
            if (this.mServiceDied && !this.mHasActivePrintJobs) {
                ensureUnbound();
                return;
            }
            ensureBound();
            this.mPendingCommands.add(new Runnable() { // from class: com.android.server.print.RemotePrintService.5
                @Override // java.lang.Runnable
                public void run() {
                    RemotePrintService.this.handleDestroyPrinterDiscoverySession();
                }
            });
            return;
        }
        try {
            this.mPrintService.destroyPrinterDiscoverySession();
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error destroying printer dicovery session.", re);
        }
        if (!this.mHasActivePrintJobs) {
            ensureUnbound();
        }
    }

    public void startPrinterDiscovery(List<PrinterId> priorityList) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda4
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).handleStartPrinterDiscovery((List) obj2);
            }
        }, this, priorityList));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleStartPrinterDiscovery(final List<PrinterId> priorityList) {
        ArrayList arrayList = new ArrayList();
        this.mDiscoveryPriorityList = arrayList;
        if (priorityList != null) {
            arrayList.addAll(priorityList);
        }
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() { // from class: com.android.server.print.RemotePrintService.6
                @Override // java.lang.Runnable
                public void run() {
                    RemotePrintService.this.handleStartPrinterDiscovery(priorityList);
                }
            });
            return;
        }
        try {
            this.mPrintService.startPrinterDiscovery(priorityList);
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error starting printer dicovery.", re);
        }
    }

    public void stopPrinterDiscovery() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda6
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((RemotePrintService) obj).handleStopPrinterDiscovery();
            }
        }, this));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleStopPrinterDiscovery() {
        this.mDiscoveryPriorityList = null;
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() { // from class: com.android.server.print.RemotePrintService.7
                @Override // java.lang.Runnable
                public void run() {
                    RemotePrintService.this.handleStopPrinterDiscovery();
                }
            });
            return;
        }
        stopTrackingAllPrinters();
        try {
            this.mPrintService.stopPrinterDiscovery();
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error stopping printer discovery.", re);
        }
    }

    public void validatePrinters(List<PrinterId> printerIds) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).handleValidatePrinters((List) obj2);
            }
        }, this, printerIds));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleValidatePrinters(final List<PrinterId> printerIds) {
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() { // from class: com.android.server.print.RemotePrintService.8
                @Override // java.lang.Runnable
                public void run() {
                    RemotePrintService.this.handleValidatePrinters(printerIds);
                }
            });
            return;
        }
        try {
            this.mPrintService.validatePrinters(printerIds);
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error requesting printers validation.", re);
        }
    }

    public void startPrinterStateTracking(PrinterId printerId) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda10
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).handleStartPrinterStateTracking((PrinterId) obj2);
            }
        }, this, printerId));
    }

    public void requestCustomPrinterIcon(PrinterId printerId) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda7
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).m6299xbd6aaaee((PrinterId) obj2);
            }
        }, this, printerId));
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleRequestCustomPrinterIcon */
    public void m6299xbd6aaaee(final PrinterId printerId) {
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    RemotePrintService.this.m6299xbd6aaaee(printerId);
                }
            });
            return;
        }
        try {
            this.mPrintService.requestCustomPrinterIcon(printerId);
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error requesting icon for " + printerId, re);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleStartPrinterStateTracking(final PrinterId printerId) {
        synchronized (this.mLock) {
            if (this.mTrackedPrinterList == null) {
                this.mTrackedPrinterList = new ArrayList();
            }
            this.mTrackedPrinterList.add(printerId);
        }
        if (!isBound()) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() { // from class: com.android.server.print.RemotePrintService.9
                @Override // java.lang.Runnable
                public void run() {
                    RemotePrintService.this.handleStartPrinterStateTracking(printerId);
                }
            });
            return;
        }
        try {
            this.mPrintService.startPrinterStateTracking(printerId);
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error requesting start printer tracking.", re);
        }
    }

    public void stopPrinterStateTracking(PrinterId printerId) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.print.RemotePrintService$$ExternalSyntheticLambda9
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((RemotePrintService) obj).handleStopPrinterStateTracking((PrinterId) obj2);
            }
        }, this, printerId));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleStopPrinterStateTracking(final PrinterId printerId) {
        synchronized (this.mLock) {
            List<PrinterId> list = this.mTrackedPrinterList;
            if (list != null && list.remove(printerId)) {
                if (this.mTrackedPrinterList.isEmpty()) {
                    this.mTrackedPrinterList = null;
                }
                if (!isBound()) {
                    ensureBound();
                    this.mPendingCommands.add(new Runnable() { // from class: com.android.server.print.RemotePrintService.10
                        @Override // java.lang.Runnable
                        public void run() {
                            RemotePrintService.this.handleStopPrinterStateTracking(printerId);
                        }
                    });
                    return;
                }
                try {
                    this.mPrintService.stopPrinterStateTracking(printerId);
                } catch (RemoteException re) {
                    Slog.e(LOG_TAG, "Error requesting stop printer tracking.", re);
                }
            }
        }
    }

    private void stopTrackingAllPrinters() {
        synchronized (this.mLock) {
            List<PrinterId> list = this.mTrackedPrinterList;
            if (list == null) {
                return;
            }
            int trackedPrinterCount = list.size();
            for (int i = trackedPrinterCount - 1; i >= 0; i--) {
                PrinterId printerId = this.mTrackedPrinterList.get(i);
                if (printerId.getServiceName().equals(this.mComponentName)) {
                    handleStopPrinterStateTracking(printerId);
                }
            }
        }
    }

    public void dump(DualDumpOutputStream proto) {
        DumpUtils.writeComponentName(proto, "component_name", 1146756268033L, this.mComponentName);
        proto.write("is_destroyed", 1133871366146L, this.mDestroyed);
        proto.write("is_bound", 1133871366147L, isBound());
        proto.write("has_discovery_session", 1133871366148L, this.mHasPrinterDiscoverySession);
        proto.write("has_active_print_jobs", 1133871366149L, this.mHasActivePrintJobs);
        proto.write("is_discovering_printers", 1133871366150L, this.mDiscoveryPriorityList != null);
        synchronized (this.mLock) {
            List<PrinterId> list = this.mTrackedPrinterList;
            if (list != null) {
                int numTrackedPrinters = list.size();
                for (int i = 0; i < numTrackedPrinters; i++) {
                    com.android.internal.print.DumpUtils.writePrinterId(proto, "tracked_printers", 2246267895815L, this.mTrackedPrinterList.get(i));
                }
            }
        }
    }

    private boolean isBound() {
        return this.mPrintService != null;
    }

    private void ensureBound() {
        if (isBound() || this.mBinding) {
            return;
        }
        this.mBinding = true;
        boolean wasBound = this.mContext.bindServiceAsUser(this.mIntent, this.mServiceConnection, 71307265, new UserHandle(this.mUserId));
        if (!wasBound) {
            this.mBinding = false;
            if (!this.mServiceDied) {
                handleBinderDied();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void ensureUnbound() {
        if (!isBound() && !this.mBinding) {
            return;
        }
        this.mBinding = false;
        this.mPendingCommands.clear();
        this.mHasActivePrintJobs = false;
        this.mHasPrinterDiscoverySession = false;
        this.mDiscoveryPriorityList = null;
        synchronized (this.mLock) {
            this.mTrackedPrinterList = null;
        }
        if (isBound()) {
            try {
                this.mPrintService.setClient((IPrintServiceClient) null);
            } catch (RemoteException e) {
            }
            this.mPrintService.asBinder().unlinkToDeath(this, 0);
            this.mPrintService = null;
            this.mContext.unbindService(this.mServiceConnection);
        }
    }

    /* loaded from: classes2.dex */
    private class RemoteServiceConneciton implements ServiceConnection {
        private RemoteServiceConneciton() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (RemotePrintService.this.mDestroyed || !RemotePrintService.this.mBinding) {
                RemotePrintService.this.mContext.unbindService(RemotePrintService.this.mServiceConnection);
                return;
            }
            RemotePrintService.this.mBinding = false;
            RemotePrintService.this.mPrintService = IPrintService.Stub.asInterface(service);
            try {
                service.linkToDeath(RemotePrintService.this, 0);
                try {
                    RemotePrintService.this.mPrintService.setClient(RemotePrintService.this.mPrintServiceClient);
                    if (RemotePrintService.this.mServiceDied && RemotePrintService.this.mHasPrinterDiscoverySession) {
                        RemotePrintService.this.handleCreatePrinterDiscoverySession();
                    }
                    if (RemotePrintService.this.mServiceDied && RemotePrintService.this.mDiscoveryPriorityList != null) {
                        RemotePrintService remotePrintService = RemotePrintService.this;
                        remotePrintService.handleStartPrinterDiscovery(remotePrintService.mDiscoveryPriorityList);
                    }
                    synchronized (RemotePrintService.this.mLock) {
                        if (RemotePrintService.this.mServiceDied && RemotePrintService.this.mTrackedPrinterList != null) {
                            int trackedPrinterCount = RemotePrintService.this.mTrackedPrinterList.size();
                            for (int i = 0; i < trackedPrinterCount; i++) {
                                RemotePrintService remotePrintService2 = RemotePrintService.this;
                                remotePrintService2.handleStartPrinterStateTracking((PrinterId) remotePrintService2.mTrackedPrinterList.get(i));
                            }
                        }
                    }
                    while (!RemotePrintService.this.mPendingCommands.isEmpty()) {
                        Runnable pendingCommand = (Runnable) RemotePrintService.this.mPendingCommands.remove(0);
                        pendingCommand.run();
                    }
                    if (!RemotePrintService.this.mHasPrinterDiscoverySession && !RemotePrintService.this.mHasActivePrintJobs) {
                        RemotePrintService.this.ensureUnbound();
                    }
                    RemotePrintService.this.mServiceDied = false;
                } catch (RemoteException re) {
                    Slog.e(RemotePrintService.LOG_TAG, "Error setting client for: " + service, re);
                    RemotePrintService.this.handleBinderDied();
                }
            } catch (RemoteException e) {
                RemotePrintService.this.mPrintService = null;
                RemotePrintService.this.handleBinderDied();
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            RemotePrintService.this.mBinding = true;
        }
    }

    /* loaded from: classes2.dex */
    private static final class RemotePrintServiceClient extends IPrintServiceClient.Stub {
        private final WeakReference<RemotePrintService> mWeakService;

        public RemotePrintServiceClient(RemotePrintService service) {
            this.mWeakService = new WeakReference<>(service);
        }

        public List<PrintJobInfo> getPrintJobInfos() {
            RemotePrintService service = this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    return service.mSpooler.getPrintJobInfos(service.mComponentName, -4, -2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
            return null;
        }

        public PrintJobInfo getPrintJobInfo(PrintJobId printJobId) {
            RemotePrintService service = this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    return service.mSpooler.getPrintJobInfo(printJobId, -2);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
            return null;
        }

        public boolean setPrintJobState(PrintJobId printJobId, int state, String error) {
            RemotePrintService service = this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    return service.mSpooler.setPrintJobState(printJobId, state, error);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
            return false;
        }

        public boolean setPrintJobTag(PrintJobId printJobId, String tag) {
            RemotePrintService service = this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    return service.mSpooler.setPrintJobTag(printJobId, tag);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
            return false;
        }

        public void writePrintJobData(ParcelFileDescriptor fd, PrintJobId printJobId) {
            RemotePrintService service = this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mSpooler.writePrintJobData(fd, printJobId);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void setProgress(PrintJobId printJobId, float progress) {
            RemotePrintService service = this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mSpooler.setProgress(printJobId, progress);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void setStatus(PrintJobId printJobId, CharSequence status) {
            RemotePrintService service = this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mSpooler.setStatus(printJobId, status);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void setStatusRes(PrintJobId printJobId, int status, CharSequence appPackageName) {
            RemotePrintService service = this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mSpooler.setStatus(printJobId, status, appPackageName);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onPrintersAdded(ParceledListSlice printers) {
            RemotePrintService service = this.mWeakService.get();
            if (service != null) {
                List<PrinterInfo> addedPrinters = printers.getList();
                throwIfPrinterIdsForPrinterInfoTampered(service.mComponentName, addedPrinters);
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mCallbacks.onPrintersAdded(addedPrinters);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onPrintersRemoved(ParceledListSlice printerIds) {
            RemotePrintService service = this.mWeakService.get();
            if (service != null) {
                List<PrinterId> removedPrinterIds = printerIds.getList();
                throwIfPrinterIdsTampered(service.mComponentName, removedPrinterIds);
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mCallbacks.onPrintersRemoved(removedPrinterIds);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        private void throwIfPrinterIdsForPrinterInfoTampered(ComponentName serviceName, List<PrinterInfo> printerInfos) {
            int printerInfoCount = printerInfos.size();
            for (int i = 0; i < printerInfoCount; i++) {
                PrinterId printerId = printerInfos.get(i).getId();
                throwIfPrinterIdTampered(serviceName, printerId);
            }
        }

        private void throwIfPrinterIdsTampered(ComponentName serviceName, List<PrinterId> printerIds) {
            int printerIdCount = printerIds.size();
            for (int i = 0; i < printerIdCount; i++) {
                PrinterId printerId = printerIds.get(i);
                throwIfPrinterIdTampered(serviceName, printerId);
            }
        }

        private void throwIfPrinterIdTampered(ComponentName serviceName, PrinterId printerId) {
            if (printerId == null || !printerId.getServiceName().equals(serviceName)) {
                throw new IllegalArgumentException("Invalid printer id: " + printerId);
            }
        }

        public void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon) throws RemoteException {
            RemotePrintService service = this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mCallbacks.onCustomPrinterIconLoaded(printerId, icon);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }
}
