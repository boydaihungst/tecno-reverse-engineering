package com.android.server.om;

import android.os.FabricatedOverlayInfo;
import android.os.FabricatedOverlayInternal;
import android.os.IBinder;
import android.os.IIdmap2;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemService;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.FgThread;
import com.android.server.job.controllers.JobStatus;
import com.android.server.om.IdmapDaemon;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class IdmapDaemon {
    private static final String IDMAP_DAEMON = "idmap2d";
    private static final int SERVICE_CONNECT_INTERVAL_SLEEP_MS = 5;
    private static final int SERVICE_CONNECT_TIMEOUT_MS = 5000;
    private static final int SERVICE_TIMEOUT_MS = 10000;
    private static IdmapDaemon sInstance;
    private volatile IIdmap2 mService;
    private final AtomicInteger mOpenedCount = new AtomicInteger();
    private final Object mIdmapToken = new Object();

    IdmapDaemon() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Connection implements AutoCloseable {
        private final IIdmap2 mIdmap2;
        private boolean mOpened;

        private Connection(IIdmap2 idmap2) {
            this.mOpened = true;
            synchronized (IdmapDaemon.this.mIdmapToken) {
                IdmapDaemon.this.mOpenedCount.incrementAndGet();
                this.mIdmap2 = idmap2;
            }
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            synchronized (IdmapDaemon.this.mIdmapToken) {
                if (this.mOpened) {
                    this.mOpened = false;
                    if (IdmapDaemon.this.mOpenedCount.decrementAndGet() != 0) {
                        return;
                    }
                    FgThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.om.IdmapDaemon$Connection$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            IdmapDaemon.Connection.this.m5229lambda$close$0$comandroidserveromIdmapDaemon$Connection();
                        }
                    }, IdmapDaemon.this.mIdmapToken, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$close$0$com-android-server-om-IdmapDaemon$Connection  reason: not valid java name */
        public /* synthetic */ void m5229lambda$close$0$comandroidserveromIdmapDaemon$Connection() {
            synchronized (IdmapDaemon.this.mIdmapToken) {
                if (IdmapDaemon.this.mService != null && IdmapDaemon.this.mOpenedCount.get() == 0) {
                    IdmapDaemon.stopIdmapService();
                    IdmapDaemon.this.mService = null;
                }
            }
        }

        public IIdmap2 getIdmap2() {
            return this.mIdmap2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static IdmapDaemon getInstance() {
        if (sInstance == null) {
            sInstance = new IdmapDaemon();
        }
        return sInstance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:64:0x00aa A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public String createIdmap(String targetPath, String overlayPath, String overlayName, int policies, boolean enforce, int userId) throws TimeoutException, RemoteException {
        Connection c = connect();
        try {
            IIdmap2 idmap2 = c.getIdmap2();
            try {
                if (idmap2 == null) {
                    try {
                    } catch (Throwable th) {
                        th = th;
                        Throwable th2 = th;
                        if (c != null) {
                            try {
                                c.close();
                            } catch (Throwable th3) {
                                th2.addSuppressed(th3);
                            }
                        }
                        throw th2;
                    }
                    try {
                        try {
                            try {
                                try {
                                    Slog.w("OverlayManager", "idmap2d service is not ready for createIdmap(\"" + targetPath + "\", \"" + overlayPath + "\", \"" + overlayName + "\", " + policies + ", " + enforce + ", " + userId + ")");
                                    if (c != null) {
                                        c.close();
                                    }
                                    return null;
                                } catch (Throwable th4) {
                                    th = th4;
                                    Throwable th22 = th;
                                    if (c != null) {
                                    }
                                    throw th22;
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                Throwable th222 = th;
                                if (c != null) {
                                }
                                throw th222;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            Throwable th2222 = th;
                            if (c != null) {
                            }
                            throw th2222;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        Throwable th22222 = th;
                        if (c != null) {
                        }
                        throw th22222;
                    }
                }
                String createIdmap = idmap2.createIdmap(targetPath, overlayPath, TextUtils.emptyIfNull(overlayName), policies, enforce, userId);
                if (c != null) {
                    c.close();
                }
                return createIdmap;
            } catch (Throwable th8) {
                th = th8;
                Throwable th222222 = th;
                if (c != null) {
                }
                throw th222222;
            }
        } catch (Throwable th9) {
            th = th9;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeIdmap(String overlayPath, int userId) throws TimeoutException, RemoteException {
        Connection c = connect();
        try {
            IIdmap2 idmap2 = c.getIdmap2();
            if (idmap2 == null) {
                Slog.w("OverlayManager", "idmap2d service is not ready for removeIdmap(\"" + overlayPath + "\", " + userId + ")");
                if (c != null) {
                    c.close();
                }
                return false;
            }
            boolean removeIdmap = idmap2.removeIdmap(overlayPath, userId);
            if (c != null) {
                c.close();
            }
            return removeIdmap;
        } catch (Throwable th) {
            if (c != null) {
                try {
                    c.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:64:0x00aa A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean verifyIdmap(String targetPath, String overlayPath, String overlayName, int policies, boolean enforce, int userId) throws Exception {
        Connection c = connect();
        try {
            IIdmap2 idmap2 = c.getIdmap2();
            try {
                if (idmap2 == null) {
                    try {
                    } catch (Throwable th) {
                        th = th;
                        Throwable th2 = th;
                        if (c != null) {
                            try {
                                c.close();
                            } catch (Throwable th3) {
                                th2.addSuppressed(th3);
                            }
                        }
                        throw th2;
                    }
                    try {
                        try {
                            try {
                                try {
                                    Slog.w("OverlayManager", "idmap2d service is not ready for verifyIdmap(\"" + targetPath + "\", \"" + overlayPath + "\", \"" + overlayName + "\", " + policies + ", " + enforce + ", " + userId + ")");
                                    if (c != null) {
                                        c.close();
                                    }
                                    return false;
                                } catch (Throwable th4) {
                                    th = th4;
                                    Throwable th22 = th;
                                    if (c != null) {
                                    }
                                    throw th22;
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                Throwable th222 = th;
                                if (c != null) {
                                }
                                throw th222;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            Throwable th2222 = th;
                            if (c != null) {
                            }
                            throw th2222;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        Throwable th22222 = th;
                        if (c != null) {
                        }
                        throw th22222;
                    }
                }
                boolean verifyIdmap = idmap2.verifyIdmap(targetPath, overlayPath, TextUtils.emptyIfNull(overlayName), policies, enforce, userId);
                if (c != null) {
                    c.close();
                }
                return verifyIdmap;
            } catch (Throwable th8) {
                th = th8;
                Throwable th222222 = th;
                if (c != null) {
                }
                throw th222222;
            }
        } catch (Throwable th9) {
            th = th9;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean idmapExists(String overlayPath, int userId) {
        try {
            Connection c = connect();
            IIdmap2 idmap2 = c.getIdmap2();
            if (idmap2 == null) {
                Slog.w("OverlayManager", "idmap2d service is not ready for idmapExists(\"" + overlayPath + "\", " + userId + ")");
                if (c != null) {
                    c.close();
                }
                return false;
            }
            boolean isFile = new File(idmap2.getIdmapPath(overlayPath, userId)).isFile();
            if (c != null) {
                c.close();
            }
            return isFile;
        } catch (Exception e) {
            Slog.wtf("OverlayManager", "failed to check if idmap exists for " + overlayPath, e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FabricatedOverlayInfo createFabricatedOverlay(FabricatedOverlayInternal overlay) {
        try {
            Connection c = connect();
            IIdmap2 idmap2 = c.getIdmap2();
            if (idmap2 == null) {
                Slog.w("OverlayManager", "idmap2d service is not ready for createFabricatedOverlay()");
                if (c != null) {
                    c.close();
                }
                return null;
            }
            FabricatedOverlayInfo createFabricatedOverlay = idmap2.createFabricatedOverlay(overlay);
            if (c != null) {
                c.close();
            }
            return createFabricatedOverlay;
        } catch (Exception e) {
            Slog.wtf("OverlayManager", "failed to fabricate overlay " + overlay, e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean deleteFabricatedOverlay(String path) {
        try {
            Connection c = connect();
            IIdmap2 idmap2 = c.getIdmap2();
            if (idmap2 == null) {
                Slog.w("OverlayManager", "idmap2d service is not ready for deleteFabricatedOverlay(\"" + path + "\")");
                if (c != null) {
                    c.close();
                }
                return false;
            }
            boolean deleteFabricatedOverlay = idmap2.deleteFabricatedOverlay(path);
            if (c != null) {
                c.close();
            }
            return deleteFabricatedOverlay;
        } catch (Exception e) {
            Slog.wtf("OverlayManager", "failed to delete fabricated overlay '" + path + "'", e);
            return false;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [238=4, 239=4, 241=4, 243=4, 244=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized List<FabricatedOverlayInfo> getFabricatedOverlayInfos() {
        ArrayList<FabricatedOverlayInfo> allInfos = new ArrayList<>();
        Connection c = null;
        try {
            Connection c2 = connect();
            IIdmap2 service = c2.getIdmap2();
            if (service == null) {
                Slog.w("OverlayManager", "idmap2d service is not ready for getFabricatedOverlayInfos()");
                List<FabricatedOverlayInfo> emptyList = Collections.emptyList();
                try {
                    if (c2.getIdmap2() != null) {
                        c2.getIdmap2().releaseFabricatedOverlayIterator();
                    }
                } catch (RemoteException e) {
                }
                c2.close();
                return emptyList;
            }
            service.acquireFabricatedOverlayIterator();
            while (true) {
                List<FabricatedOverlayInfo> infos = service.nextFabricatedOverlayInfos();
                if (infos.isEmpty()) {
                    try {
                        break;
                    } catch (RemoteException e2) {
                    }
                } else {
                    allInfos.addAll(infos);
                }
            }
            if (c2.getIdmap2() != null) {
                c2.getIdmap2().releaseFabricatedOverlayIterator();
            }
            c2.close();
            return allInfos;
        } catch (Exception e3) {
            Slog.wtf("OverlayManager", "failed to get all fabricated overlays", e3);
            try {
                if (c.getIdmap2() != null) {
                    c.getIdmap2().releaseFabricatedOverlayIterator();
                }
            } catch (RemoteException e4) {
            }
            c.close();
            return allInfos;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String dumpIdmap(String overlayPath) {
        try {
            Connection c = connect();
            IIdmap2 service = c.getIdmap2();
            if (service == null) {
                Slog.w("OverlayManager", "idmap2d service is not ready for dumpIdmap()");
                if (c != null) {
                    c.close();
                }
                return "idmap2d service is not ready for dumpIdmap()";
            }
            String dump = service.dumpIdmap(overlayPath);
            String nullIfEmpty = TextUtils.nullIfEmpty(dump);
            if (c != null) {
                c.close();
            }
            return nullIfEmpty;
        } catch (Exception e) {
            Slog.wtf("OverlayManager", "failed to dump idmap", e);
            return null;
        }
    }

    private IBinder getIdmapService() throws TimeoutException, RemoteException {
        try {
            SystemService.start(IDMAP_DAEMON);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("failed to set system property")) {
                Slog.w("OverlayManager", "Failed to enable idmap2 daemon", e);
                return null;
            }
        }
        long endMillis = SystemClock.elapsedRealtime() + 5000;
        while (SystemClock.elapsedRealtime() <= endMillis) {
            IBinder binder = ServiceManager.getService("idmap");
            if (binder != null) {
                binder.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.om.IdmapDaemon$$ExternalSyntheticLambda0
                    @Override // android.os.IBinder.DeathRecipient
                    public final void binderDied() {
                        Slog.w("OverlayManager", String.format("service '%s' died", "idmap"));
                    }
                }, 0);
                return binder;
            }
            try {
                Thread.sleep(5L);
            } catch (InterruptedException e2) {
            }
        }
        throw new TimeoutException(String.format("Failed to connect to '%s' in %d milliseconds", "idmap", 5000));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void stopIdmapService() {
        try {
            SystemService.stop(IDMAP_DAEMON);
        } catch (RuntimeException e) {
            Slog.w("OverlayManager", "Failed to disable idmap2 daemon", e);
        }
    }

    private Connection connect() throws TimeoutException, RemoteException {
        synchronized (this.mIdmapToken) {
            FgThread.getHandler().removeCallbacksAndMessages(this.mIdmapToken);
            if (this.mService != null) {
                return new Connection(this.mService);
            }
            IBinder binder = getIdmapService();
            if (binder == null) {
                return new Connection(null);
            }
            this.mService = IIdmap2.Stub.asInterface(binder);
            return new Connection(this.mService);
        }
    }
}
