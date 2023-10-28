package com.android.server.blob;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.StatsManager;
import android.app.blob.BlobHandle;
import android.app.blob.BlobInfo;
import android.app.blob.IBlobStoreManager;
import android.app.blob.IBlobStoreSession;
import android.app.blob.LeaseInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageStats;
import android.content.res.ResourceId;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.LimitExceededException;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.ExceptionUtils;
import android.util.IndentingPrintWriter;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.SparseArray;
import android.util.StatsEvent;
import android.util.Xml;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.function.LongObjPredicate;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalManagerRegistry;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.Watchdog;
import com.android.server.blob.BlobMetadata;
import com.android.server.blob.BlobStoreManagerService;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.verify.domain.DomainVerificationLegacySettings;
import com.android.server.usage.StorageStatsManagerLocal;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;
/* loaded from: classes.dex */
public class BlobStoreManagerService extends SystemService {
    private final ArraySet<Long> mActiveBlobIds;
    private final Handler mBackgroundHandler;
    private final Object mBlobsLock;
    private final ArrayMap<BlobHandle, BlobMetadata> mBlobsMap;
    private final Context mContext;
    private long mCurrentMaxSessionId;
    private final Handler mHandler;
    private final Injector mInjector;
    private final ArraySet<Long> mKnownBlobIds;
    private PackageManagerInternal mPackageManagerInternal;
    private final Random mRandom;
    private final Runnable mSaveBlobsInfoRunnable;
    private final Runnable mSaveSessionsRunnable;
    private final SessionStateChangeListener mSessionStateChangeListener;
    private final SparseArray<LongSparseArray<BlobStoreSession>> mSessions;
    private StatsPullAtomCallbackImpl mStatsCallbackImpl;
    private StatsManager mStatsManager;

    /* renamed from: -$$Nest$sminitializeMessageHandler  reason: not valid java name */
    static /* bridge */ /* synthetic */ Handler m2580$$Nest$sminitializeMessageHandler() {
        return initializeMessageHandler();
    }

    public BlobStoreManagerService(Context context) {
        this(context, new Injector());
    }

    BlobStoreManagerService(Context context, Injector injector) {
        super(context);
        this.mBlobsLock = new Object();
        this.mSessions = new SparseArray<>();
        this.mBlobsMap = new ArrayMap<>();
        this.mActiveBlobIds = new ArraySet<>();
        this.mKnownBlobIds = new ArraySet<>();
        this.mRandom = new SecureRandom();
        this.mSessionStateChangeListener = new SessionStateChangeListener();
        this.mStatsCallbackImpl = new StatsPullAtomCallbackImpl();
        this.mSaveBlobsInfoRunnable = new Runnable() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                BlobStoreManagerService.this.writeBlobsInfo();
            }
        };
        this.mSaveSessionsRunnable = new Runnable() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                BlobStoreManagerService.this.writeBlobSessions();
            }
        };
        this.mContext = context;
        this.mInjector = injector;
        this.mHandler = injector.initializeMessageHandler();
        this.mBackgroundHandler = injector.getBackgroundHandler();
    }

    private static Handler initializeMessageHandler() {
        HandlerThread handlerThread = new ServiceThread(BlobStoreConfig.TAG, 0, true);
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        Watchdog.getInstance().addThread(handler);
        return handler;
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("blob_store", new Stub());
        LocalServices.addService(BlobStoreManagerInternal.class, new LocalService());
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mStatsManager = (StatsManager) getContext().getSystemService(StatsManager.class);
        registerReceivers();
        ((StorageStatsManagerLocal) LocalManagerRegistry.getManager(StorageStatsManagerLocal.class)).registerStorageStatsAugmenter(new BlobStorageStatsAugmenter(), BlobStoreConfig.TAG);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 550) {
            BlobStoreConfig.initialize(this.mContext);
        } else if (phase == 600) {
            synchronized (this.mBlobsLock) {
                SparseArray<SparseArray<String>> allPackages = getAllPackages();
                readBlobSessionsLocked(allPackages);
                readBlobsInfoLocked(allPackages);
            }
            registerBlobStorePuller();
        } else if (phase == 1000) {
            BlobStoreIdleJobService.schedule(this.mContext);
        }
    }

    private long generateNextSessionIdLocked() {
        int n = 0;
        while (true) {
            long randomLong = this.mRandom.nextLong();
            long sessionId = randomLong == Long.MIN_VALUE ? 0L : Math.abs(randomLong);
            if (this.mKnownBlobIds.indexOf(Long.valueOf(sessionId)) < 0 && sessionId != 0) {
                return sessionId;
            }
            int n2 = n + 1;
            if (n >= 32) {
                throw new IllegalStateException("Failed to allocate session ID");
            }
            n = n2;
        }
    }

    private void registerReceivers() {
        IntentFilter packageChangedFilter = new IntentFilter();
        packageChangedFilter.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
        packageChangedFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        packageChangedFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(new PackageChangedReceiver(), UserHandle.ALL, packageChangedFilter, null, this.mHandler);
        IntentFilter userActionFilter = new IntentFilter();
        userActionFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(new UserActionReceiver(), UserHandle.ALL, userActionFilter, null, this.mHandler);
    }

    private LongSparseArray<BlobStoreSession> getUserSessionsLocked(int userId) {
        LongSparseArray<BlobStoreSession> userSessions = this.mSessions.get(userId);
        if (userSessions == null) {
            LongSparseArray<BlobStoreSession> userSessions2 = new LongSparseArray<>();
            this.mSessions.put(userId, userSessions2);
            return userSessions2;
        }
        return userSessions;
    }

    void addUserSessionsForTest(LongSparseArray<BlobStoreSession> userSessions, int userId) {
        synchronized (this.mBlobsLock) {
            this.mSessions.put(userId, userSessions);
        }
    }

    BlobMetadata getBlobForTest(BlobHandle blobHandle) {
        BlobMetadata blobMetadata;
        synchronized (this.mBlobsLock) {
            blobMetadata = this.mBlobsMap.get(blobHandle);
        }
        return blobMetadata;
    }

    int getBlobsCountForTest() {
        int size;
        synchronized (this.mBlobsLock) {
            size = this.mBlobsMap.size();
        }
        return size;
    }

    void addActiveIdsForTest(long... activeIds) {
        synchronized (this.mBlobsLock) {
            for (long id : activeIds) {
                addActiveBlobIdLocked(id);
            }
        }
    }

    Set<Long> getActiveIdsForTest() {
        ArraySet<Long> arraySet;
        synchronized (this.mBlobsLock) {
            arraySet = this.mActiveBlobIds;
        }
        return arraySet;
    }

    Set<Long> getKnownIdsForTest() {
        ArraySet<Long> arraySet;
        synchronized (this.mBlobsLock) {
            arraySet = this.mKnownBlobIds;
        }
        return arraySet;
    }

    private void addSessionForUserLocked(BlobStoreSession session, int userId) {
        getUserSessionsLocked(userId).put(session.getSessionId(), session);
        addActiveBlobIdLocked(session.getSessionId());
    }

    void addBlobLocked(BlobMetadata blobMetadata) {
        this.mBlobsMap.put(blobMetadata.getBlobHandle(), blobMetadata);
        addActiveBlobIdLocked(blobMetadata.getBlobId());
    }

    private void addActiveBlobIdLocked(long id) {
        this.mActiveBlobIds.add(Long.valueOf(id));
        this.mKnownBlobIds.add(Long.valueOf(id));
    }

    private int getSessionsCountLocked(final int uid, final String packageName) {
        final AtomicInteger sessionsCount = new AtomicInteger(0);
        forEachSessionInUser(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda12
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BlobStoreManagerService.lambda$getSessionsCountLocked$0(uid, packageName, sessionsCount, (BlobStoreSession) obj);
            }
        }, UserHandle.getUserId(uid));
        return sessionsCount.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getSessionsCountLocked$0(int uid, String packageName, AtomicInteger sessionsCount, BlobStoreSession session) {
        if (session.getOwnerUid() == uid && session.getOwnerPackageName().equals(packageName)) {
            sessionsCount.getAndIncrement();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long createSessionInternal(BlobHandle blobHandle, int callingUid, String callingPackage) {
        synchronized (this.mBlobsLock) {
            try {
                try {
                    int sessionsCount = getSessionsCountLocked(callingUid, callingPackage);
                    if (sessionsCount < BlobStoreConfig.getMaxActiveSessions()) {
                        long sessionId = generateNextSessionIdLocked();
                        BlobStoreSession session = new BlobStoreSession(this.mContext, sessionId, blobHandle, callingUid, callingPackage, this.mSessionStateChangeListener);
                        addSessionForUserLocked(session, UserHandle.getUserId(callingUid));
                        if (BlobStoreConfig.LOGV) {
                            Slog.v(BlobStoreConfig.TAG, "Created session for " + blobHandle + "; callingUid=" + callingUid + ", callingPackage=" + callingPackage);
                        }
                        writeBlobSessionsAsync();
                        return sessionId;
                    }
                    throw new LimitExceededException("Too many active sessions for the caller: " + sessionsCount);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public BlobStoreSession openSessionInternal(long sessionId, int callingUid, String callingPackage) {
        BlobStoreSession session;
        synchronized (this.mBlobsLock) {
            session = getUserSessionsLocked(UserHandle.getUserId(callingUid)).get(sessionId);
            if (session == null || !session.hasAccess(callingUid, callingPackage) || session.isFinalized()) {
                throw new SecurityException("Session not found: " + sessionId);
            }
        }
        session.open();
        return session;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void abandonSessionInternal(long sessionId, int callingUid, String callingPackage) {
        synchronized (this.mBlobsLock) {
            BlobStoreSession session = openSessionInternal(sessionId, callingUid, callingPackage);
            session.open();
            session.abandon();
            if (BlobStoreConfig.LOGV) {
                Slog.v(BlobStoreConfig.TAG, "Abandoned session with id " + sessionId + "; callingUid=" + callingUid + ", callingPackage=" + callingPackage);
            }
            writeBlobSessionsAsync();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:13:0x002b, code lost:
        com.android.internal.util.FrameworkStatsLog.write(300, r12, 0L, 0L, 2);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public ParcelFileDescriptor openBlobInternal(BlobHandle blobHandle, int callingUid, String callingPackage) throws IOException {
        ParcelFileDescriptor openForRead;
        synchronized (this.mBlobsLock) {
            BlobMetadata blobMetadata = this.mBlobsMap.get(blobHandle);
            if (blobMetadata != null && blobMetadata.isAccessAllowedForCaller(callingPackage, callingUid)) {
                FrameworkStatsLog.write(300, callingUid, blobMetadata.getBlobId(), blobMetadata.getSize(), 1);
                openForRead = blobMetadata.openForRead(callingPackage, callingUid);
            }
            FrameworkStatsLog.write(300, callingUid, blobMetadata.getBlobId(), blobMetadata.getSize(), 3);
            throw new SecurityException("Caller not allowed to access " + blobHandle + "; callingUid=" + callingUid + ", callingPackage=" + callingPackage);
        }
        return openForRead;
    }

    private int getCommittedBlobsCountLocked(final int uid, final String packageName) {
        final AtomicInteger blobsCount = new AtomicInteger(0);
        forEachBlobLocked(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda9
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BlobStoreManagerService.lambda$getCommittedBlobsCountLocked$1(packageName, uid, blobsCount, (BlobMetadata) obj);
            }
        });
        return blobsCount.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getCommittedBlobsCountLocked$1(String packageName, int uid, AtomicInteger blobsCount, BlobMetadata blobMetadata) {
        if (blobMetadata.isACommitter(packageName, uid)) {
            blobsCount.getAndIncrement();
        }
    }

    private int getLeasedBlobsCountLocked(final int uid, final String packageName) {
        final AtomicInteger blobsCount = new AtomicInteger(0);
        forEachBlobLocked(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BlobStoreManagerService.lambda$getLeasedBlobsCountLocked$2(packageName, uid, blobsCount, (BlobMetadata) obj);
            }
        });
        return blobsCount.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getLeasedBlobsCountLocked$2(String packageName, int uid, AtomicInteger blobsCount, BlobMetadata blobMetadata) {
        if (blobMetadata.isALeasee(packageName, uid)) {
            blobsCount.getAndIncrement();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x00c6, code lost:
        com.android.internal.util.FrameworkStatsLog.write((int) com.android.internal.util.FrameworkStatsLog.BLOB_LEASED, r20, 0L, 0L, 2);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void acquireLeaseInternal(BlobHandle blobHandle, int descriptionResId, CharSequence description, long leaseExpiryTimeMillis, int callingUid, String callingPackage) {
        synchronized (this.mBlobsLock) {
            int leasesCount = getLeasedBlobsCountLocked(callingUid, callingPackage);
            if (leasesCount >= BlobStoreConfig.getMaxLeasedBlobs()) {
                FrameworkStatsLog.write((int) FrameworkStatsLog.BLOB_LEASED, callingUid, 0L, 0L, 6);
                throw new LimitExceededException("Too many leased blobs for the caller: " + leasesCount);
            }
            if (leaseExpiryTimeMillis != 0 && blobHandle.expiryTimeMillis != 0 && leaseExpiryTimeMillis > blobHandle.expiryTimeMillis) {
                FrameworkStatsLog.write((int) FrameworkStatsLog.BLOB_LEASED, callingUid, 0L, 0L, 4);
                throw new IllegalArgumentException("Lease expiry cannot be later than blobs expiry time");
            }
            BlobMetadata blobMetadata = this.mBlobsMap.get(blobHandle);
            if (blobMetadata != null && blobMetadata.isAccessAllowedForCaller(callingPackage, callingUid)) {
                if (blobMetadata.getSize() > getRemainingLeaseQuotaBytesInternal(callingUid, callingPackage)) {
                    FrameworkStatsLog.write((int) FrameworkStatsLog.BLOB_LEASED, callingUid, blobMetadata.getBlobId(), blobMetadata.getSize(), 5);
                    throw new LimitExceededException("Total amount of data with an active lease is exceeding the max limit");
                }
                FrameworkStatsLog.write((int) FrameworkStatsLog.BLOB_LEASED, callingUid, blobMetadata.getBlobId(), blobMetadata.getSize(), 1);
                blobMetadata.addOrReplaceLeasee(callingPackage, callingUid, descriptionResId, description, leaseExpiryTimeMillis);
                if (BlobStoreConfig.LOGV) {
                    Slog.v(BlobStoreConfig.TAG, "Acquired lease on " + blobHandle + "; callingUid=" + callingUid + ", callingPackage=" + callingPackage);
                }
                writeBlobsInfoAsync();
            }
            FrameworkStatsLog.write((int) FrameworkStatsLog.BLOB_LEASED, callingUid, blobMetadata.getBlobId(), blobMetadata.getSize(), 3);
            throw new SecurityException("Caller not allowed to access " + blobHandle + "; callingUid=" + callingUid + ", callingPackage=" + callingPackage);
        }
    }

    long getTotalUsageBytesLocked(final int callingUid, final String callingPackage) {
        final AtomicLong totalBytes = new AtomicLong(0L);
        forEachBlobLocked(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda11
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BlobStoreManagerService.lambda$getTotalUsageBytesLocked$3(callingPackage, callingUid, totalBytes, (BlobMetadata) obj);
            }
        });
        return totalBytes.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getTotalUsageBytesLocked$3(String callingPackage, int callingUid, AtomicLong totalBytes, BlobMetadata blobMetadata) {
        if (blobMetadata.isALeasee(callingPackage, callingUid)) {
            totalBytes.getAndAdd(blobMetadata.getSize());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseLeaseInternal(final BlobHandle blobHandle, int callingUid, String callingPackage) {
        synchronized (this.mBlobsLock) {
            final BlobMetadata blobMetadata = this.mBlobsMap.get(blobHandle);
            if (blobMetadata == null || !blobMetadata.isAccessAllowedForCaller(callingPackage, callingUid)) {
                throw new SecurityException("Caller not allowed to access " + blobHandle + "; callingUid=" + callingUid + ", callingPackage=" + callingPackage);
            }
            blobMetadata.removeLeasee(callingPackage, callingUid);
            if (BlobStoreConfig.LOGV) {
                Slog.v(BlobStoreConfig.TAG, "Released lease on " + blobHandle + "; callingUid=" + callingUid + ", callingPackage=" + callingPackage);
            }
            if (!blobMetadata.hasValidLeases()) {
                this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        BlobStoreManagerService.this.m2589x6d7badd5(blobHandle, blobMetadata);
                    }
                }, BlobStoreConfig.getDeletionOnLastLeaseDelayMs());
            }
            writeBlobsInfoAsync();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$releaseLeaseInternal$4$com-android-server-blob-BlobStoreManagerService  reason: not valid java name */
    public /* synthetic */ void m2589x6d7badd5(BlobHandle blobHandle, BlobMetadata blobMetadata) {
        synchronized (this.mBlobsLock) {
            if (Objects.equals(this.mBlobsMap.get(blobHandle), blobMetadata)) {
                if (blobMetadata.shouldBeDeleted(true)) {
                    deleteBlobLocked(blobMetadata);
                    this.mBlobsMap.remove(blobHandle);
                }
                writeBlobsInfoAsync();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseAllLeasesInternal(final int callingUid, final String callingPackage) {
        synchronized (this.mBlobsLock) {
            this.mBlobsMap.forEach(new BiConsumer() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda14
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    BlobHandle blobHandle = (BlobHandle) obj;
                    ((BlobMetadata) obj2).removeLeasee(callingPackage, callingUid);
                }
            });
            writeBlobsInfoAsync();
            if (BlobStoreConfig.LOGV) {
                Slog.v(BlobStoreConfig.TAG, "Release all leases associated with pkg=" + callingPackage + ", uid=" + callingUid);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getRemainingLeaseQuotaBytesInternal(int callingUid, String callingPackage) {
        long j;
        synchronized (this.mBlobsLock) {
            long remainingQuota = BlobStoreConfig.getAppDataBytesLimit() - getTotalUsageBytesLocked(callingUid, callingPackage);
            j = remainingQuota > 0 ? remainingQuota : 0L;
        }
        return j;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<BlobInfo> queryBlobsForUserInternal(final int userId) {
        final ArrayList<BlobInfo> blobInfos = new ArrayList<>();
        synchronized (this.mBlobsLock) {
            final ArrayMap<String, WeakReference<Resources>> resources = new ArrayMap<>();
            final Function<String, Resources> resourcesGetter = new Function() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda7
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return BlobStoreManagerService.this.m2588x7ac29c70(resources, userId, (String) obj);
                }
            };
            forEachBlobLocked(new BiConsumer() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda8
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    BlobStoreManagerService.lambda$queryBlobsForUserInternal$8(userId, resourcesGetter, blobInfos, (BlobHandle) obj, (BlobMetadata) obj2);
                }
            });
        }
        return blobInfos;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$queryBlobsForUserInternal$6$com-android-server-blob-BlobStoreManagerService  reason: not valid java name */
    public /* synthetic */ Resources m2588x7ac29c70(ArrayMap resources, int userId, String packageName) {
        WeakReference<Resources> resourcesRef = (WeakReference) resources.get(packageName);
        Resources packageResources = resourcesRef == null ? null : resourcesRef.get();
        if (packageResources == null) {
            Resources packageResources2 = BlobStoreUtils.getPackageResources(this.mContext, packageName, userId);
            resources.put(packageName, new WeakReference(packageResources2));
            return packageResources2;
        }
        return packageResources;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$queryBlobsForUserInternal$8(final int userId, final Function resourcesGetter, ArrayList blobInfos, final BlobHandle blobHandle, BlobMetadata blobMetadata) {
        if (!blobMetadata.hasACommitterOrLeaseeInUser(userId)) {
            return;
        }
        final ArrayList<LeaseInfo> leaseInfos = new ArrayList<>();
        blobMetadata.forEachLeasee(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda6
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BlobStoreManagerService.lambda$queryBlobsForUserInternal$7(userId, resourcesGetter, blobHandle, leaseInfos, (BlobMetadata.Leasee) obj);
            }
        });
        blobInfos.add(new BlobInfo(blobMetadata.getBlobId(), blobHandle.getExpiryTimeMillis(), blobHandle.getLabel(), blobMetadata.getSize(), leaseInfos));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$queryBlobsForUserInternal$7(int userId, Function resourcesGetter, BlobHandle blobHandle, ArrayList leaseInfos, BlobMetadata.Leasee leasee) {
        int descriptionResId;
        if (!leasee.isStillValid() || userId != UserHandle.getUserId(leasee.uid)) {
            return;
        }
        if (leasee.descriptionResEntryName == null) {
            descriptionResId = 0;
        } else {
            descriptionResId = BlobStoreUtils.getDescriptionResourceId((Resources) resourcesGetter.apply(leasee.packageName), leasee.descriptionResEntryName, leasee.packageName);
        }
        long expiryTimeMs = leasee.expiryTimeMillis == 0 ? blobHandle.getExpiryTimeMillis() : leasee.expiryTimeMillis;
        leaseInfos.add(new LeaseInfo(leasee.packageName, expiryTimeMs, descriptionResId, leasee.description));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deleteBlobInternal(final long blobId, int callingUid) {
        synchronized (this.mBlobsLock) {
            this.mBlobsMap.entrySet().removeIf(new Predicate() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda2
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return BlobStoreManagerService.this.m2581xc57602f(blobId, (Map.Entry) obj);
                }
            });
            writeBlobsInfoAsync();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$deleteBlobInternal$9$com-android-server-blob-BlobStoreManagerService  reason: not valid java name */
    public /* synthetic */ boolean m2581xc57602f(long blobId, Map.Entry entry) {
        BlobMetadata blobMetadata = (BlobMetadata) entry.getValue();
        if (blobMetadata.getBlobId() == blobId) {
            deleteBlobLocked(blobMetadata);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<BlobHandle> getLeasedBlobsInternal(final int callingUid, final String callingPackage) {
        final ArrayList<BlobHandle> leasedBlobs = new ArrayList<>();
        synchronized (this.mBlobsLock) {
            forEachBlobLocked(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda15
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BlobStoreManagerService.lambda$getLeasedBlobsInternal$10(callingPackage, callingUid, leasedBlobs, (BlobMetadata) obj);
                }
            });
        }
        return leasedBlobs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getLeasedBlobsInternal$10(String callingPackage, int callingUid, ArrayList leasedBlobs, BlobMetadata blobMetadata) {
        if (blobMetadata.isALeasee(callingPackage, callingUid)) {
            leasedBlobs.add(blobMetadata.getBlobHandle());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LeaseInfo getLeaseInfoInternal(BlobHandle blobHandle, int callingUid, String callingPackage) {
        LeaseInfo leaseInfo;
        synchronized (this.mBlobsLock) {
            BlobMetadata blobMetadata = this.mBlobsMap.get(blobHandle);
            if (blobMetadata == null || !blobMetadata.isAccessAllowedForCaller(callingPackage, callingUid)) {
                throw new SecurityException("Caller not allowed to access " + blobHandle + "; callingUid=" + callingUid + ", callingPackage=" + callingPackage);
            }
            leaseInfo = blobMetadata.getLeaseInfo(callingPackage, callingUid);
        }
        return leaseInfo;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void verifyCallingPackage(int callingUid, String callingPackage) {
        if (this.mPackageManagerInternal.getPackageUid(callingPackage, 0L, UserHandle.getUserId(callingUid)) != callingUid) {
            throw new SecurityException("Specified calling package [" + callingPackage + "] does not match the calling uid " + callingUid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SessionStateChangeListener {
        SessionStateChangeListener() {
        }

        public void onStateChanged(BlobStoreSession session) {
            BlobStoreManagerService.this.mHandler.post(PooledLambda.obtainRunnable(new BiConsumer() { // from class: com.android.server.blob.BlobStoreManagerService$SessionStateChangeListener$$ExternalSyntheticLambda0
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    ((BlobStoreManagerService) obj).onStateChangedInternal((BlobStoreSession) obj2);
                }
            }, BlobStoreManagerService.this, session).recycleOnUse());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onStateChangedInternal(final BlobStoreSession session) {
        BlobMetadata blob;
        switch (session.getState()) {
            case 2:
            case 5:
                synchronized (this.mBlobsLock) {
                    deleteSessionLocked(session);
                    getUserSessionsLocked(UserHandle.getUserId(session.getOwnerUid())).remove(session.getSessionId());
                    if (BlobStoreConfig.LOGV) {
                        Slog.v(BlobStoreConfig.TAG, "Session is invalid; deleted " + session);
                    }
                }
                break;
            case 3:
                this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda10
                    @Override // java.lang.Runnable
                    public final void run() {
                        BlobStoreManagerService.this.m2587x34b83460(session);
                    }
                });
                break;
            case 4:
                synchronized (this.mBlobsLock) {
                    int committedBlobsCount = getCommittedBlobsCountLocked(session.getOwnerUid(), session.getOwnerPackageName());
                    if (committedBlobsCount >= BlobStoreConfig.getMaxCommittedBlobs()) {
                        Slog.d(BlobStoreConfig.TAG, "Failed to commit: too many committed blobs. count: " + committedBlobsCount + "; blob: " + session);
                        session.sendCommitCallbackResult(1);
                        deleteSessionLocked(session);
                        getUserSessionsLocked(UserHandle.getUserId(session.getOwnerUid())).remove(session.getSessionId());
                        FrameworkStatsLog.write((int) FrameworkStatsLog.BLOB_COMMITTED, session.getOwnerUid(), session.getSessionId(), session.getSize(), 4);
                        break;
                    } else {
                        int blobIndex = this.mBlobsMap.indexOfKey(session.getBlobHandle());
                        if (blobIndex >= 0) {
                            blob = this.mBlobsMap.valueAt(blobIndex);
                        } else {
                            BlobMetadata blob2 = new BlobMetadata(this.mContext, session.getSessionId(), session.getBlobHandle());
                            addBlobLocked(blob2);
                            blob = blob2;
                        }
                        BlobMetadata.Committer existingCommitter = blob.getExistingCommitter(session.getOwnerPackageName(), session.getOwnerUid());
                        long existingCommitTimeMs = existingCommitter == null ? 0L : existingCommitter.getCommitTimeMs();
                        BlobMetadata.Committer newCommitter = new BlobMetadata.Committer(session.getOwnerPackageName(), session.getOwnerUid(), session.getBlobAccessMode(), BlobStoreConfig.getAdjustedCommitTimeMs(existingCommitTimeMs, System.currentTimeMillis()));
                        blob.addOrReplaceCommitter(newCommitter);
                        try {
                            writeBlobsInfoLocked();
                            FrameworkStatsLog.write((int) FrameworkStatsLog.BLOB_COMMITTED, session.getOwnerUid(), blob.getBlobId(), blob.getSize(), 1);
                            session.sendCommitCallbackResult(0);
                        } catch (Exception e) {
                            if (existingCommitter == null) {
                                blob.removeCommitter(newCommitter);
                            } else {
                                blob.addOrReplaceCommitter(existingCommitter);
                            }
                            Slog.d(BlobStoreConfig.TAG, "Error committing the blob: " + session, e);
                            FrameworkStatsLog.write((int) FrameworkStatsLog.BLOB_COMMITTED, session.getOwnerUid(), session.getSessionId(), blob.getSize(), 2);
                            session.sendCommitCallbackResult(1);
                            if (session.getSessionId() == blob.getBlobId()) {
                                deleteBlobLocked(blob);
                                this.mBlobsMap.remove(blob.getBlobHandle());
                            }
                        }
                        if (session.getSessionId() != blob.getBlobId()) {
                            deleteSessionLocked(session);
                        }
                        getUserSessionsLocked(UserHandle.getUserId(session.getOwnerUid())).remove(session.getSessionId());
                        if (BlobStoreConfig.LOGV) {
                            Slog.v(BlobStoreConfig.TAG, "Successfully committed session " + session);
                        }
                        break;
                    }
                }
            default:
                Slog.wtf(BlobStoreConfig.TAG, "Invalid session state: " + BlobStoreSession.stateToString(session.getState()));
                break;
        }
        synchronized (this.mBlobsLock) {
            try {
                writeBlobSessionsLocked();
            } catch (Exception e2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onStateChangedInternal$11$com-android-server-blob-BlobStoreManagerService  reason: not valid java name */
    public /* synthetic */ void m2587x34b83460(BlobStoreSession session) {
        session.computeDigest();
        this.mHandler.post(PooledLambda.obtainRunnable(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda5
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((BlobStoreSession) obj).verifyBlobData();
            }
        }, session).recycleOnUse());
    }

    private void writeBlobSessionsLocked() throws Exception {
        AtomicFile sessionsIndexFile = prepareSessionsIndexFile();
        if (sessionsIndexFile == null) {
            Slog.wtf(BlobStoreConfig.TAG, "Error creating sessions index file");
            return;
        }
        FileOutputStream fos = null;
        try {
            fos = sessionsIndexFile.startWrite(SystemClock.uptimeMillis());
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, true);
            out.startTag(null, "ss");
            XmlUtils.writeIntAttribute(out, "v", 6);
            int userCount = this.mSessions.size();
            for (int i = 0; i < userCount; i++) {
                LongSparseArray<BlobStoreSession> userSessions = this.mSessions.valueAt(i);
                int sessionsCount = userSessions.size();
                for (int j = 0; j < sessionsCount; j++) {
                    out.startTag(null, "s");
                    userSessions.valueAt(j).writeToXml(out);
                    out.endTag(null, "s");
                }
            }
            out.endTag(null, "ss");
            out.endDocument();
            sessionsIndexFile.finishWrite(fos);
            if (BlobStoreConfig.LOGV) {
                Slog.v(BlobStoreConfig.TAG, "Finished persisting sessions data");
            }
        } catch (Exception e) {
            sessionsIndexFile.failWrite(fos);
            Slog.wtf(BlobStoreConfig.TAG, "Error writing sessions data", e);
            throw e;
        }
    }

    private void readBlobSessionsLocked(SparseArray<SparseArray<String>> allPackages) {
        BlobStoreSession session;
        if (!BlobStoreConfig.getBlobStoreRootDir().exists()) {
            return;
        }
        AtomicFile sessionsIndexFile = prepareSessionsIndexFile();
        if (sessionsIndexFile == null) {
            Slog.wtf(BlobStoreConfig.TAG, "Error creating sessions index file");
        } else if (!sessionsIndexFile.exists()) {
            Slog.w(BlobStoreConfig.TAG, "Sessions index file not available: " + sessionsIndexFile.getBaseFile());
        } else {
            this.mSessions.clear();
            try {
                FileInputStream fis = sessionsIndexFile.openRead();
                XmlPullParser in = Xml.newPullParser();
                in.setInput(fis, StandardCharsets.UTF_8.name());
                XmlUtils.beginDocument(in, "ss");
                int version = XmlUtils.readIntAttribute(in, "v");
                while (true) {
                    XmlUtils.nextElement(in);
                    if (in.getEventType() == 1) {
                        break;
                    } else if ("s".equals(in.getName()) && (session = BlobStoreSession.createFromXml(in, version, this.mContext, this.mSessionStateChangeListener)) != null) {
                        SparseArray<String> userPackages = allPackages.get(UserHandle.getUserId(session.getOwnerUid()));
                        if (userPackages != null && session.getOwnerPackageName().equals(userPackages.get(session.getOwnerUid()))) {
                            addSessionForUserLocked(session, UserHandle.getUserId(session.getOwnerUid()));
                        } else {
                            session.getSessionFile().delete();
                        }
                        this.mCurrentMaxSessionId = Math.max(this.mCurrentMaxSessionId, session.getSessionId());
                    }
                }
                if (BlobStoreConfig.LOGV) {
                    Slog.v(BlobStoreConfig.TAG, "Finished reading sessions data");
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception e) {
                Slog.wtf(BlobStoreConfig.TAG, "Error reading sessions data", e);
            }
        }
    }

    private void writeBlobsInfoLocked() throws Exception {
        AtomicFile blobsIndexFile = prepareBlobsIndexFile();
        if (blobsIndexFile == null) {
            Slog.wtf(BlobStoreConfig.TAG, "Error creating blobs index file");
            return;
        }
        FileOutputStream fos = null;
        try {
            fos = blobsIndexFile.startWrite(SystemClock.uptimeMillis());
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, true);
            out.startTag(null, "bs");
            XmlUtils.writeIntAttribute(out, "v", 6);
            int count = this.mBlobsMap.size();
            for (int i = 0; i < count; i++) {
                out.startTag(null, "b");
                this.mBlobsMap.valueAt(i).writeToXml(out);
                out.endTag(null, "b");
            }
            out.endTag(null, "bs");
            out.endDocument();
            blobsIndexFile.finishWrite(fos);
            if (BlobStoreConfig.LOGV) {
                Slog.v(BlobStoreConfig.TAG, "Finished persisting blobs data");
            }
        } catch (Exception e) {
            blobsIndexFile.failWrite(fos);
            Slog.wtf(BlobStoreConfig.TAG, "Error writing blobs data", e);
            throw e;
        }
    }

    private void readBlobsInfoLocked(SparseArray<SparseArray<String>> allPackages) {
        if (!BlobStoreConfig.getBlobStoreRootDir().exists()) {
            return;
        }
        AtomicFile blobsIndexFile = prepareBlobsIndexFile();
        if (blobsIndexFile == null) {
            Slog.wtf(BlobStoreConfig.TAG, "Error creating blobs index file");
        } else if (!blobsIndexFile.exists()) {
            Slog.w(BlobStoreConfig.TAG, "Blobs index file not available: " + blobsIndexFile.getBaseFile());
        } else {
            this.mBlobsMap.clear();
            try {
                FileInputStream fis = blobsIndexFile.openRead();
                XmlPullParser in = Xml.newPullParser();
                in.setInput(fis, StandardCharsets.UTF_8.name());
                XmlUtils.beginDocument(in, "bs");
                int version = XmlUtils.readIntAttribute(in, "v");
                while (true) {
                    XmlUtils.nextElement(in);
                    if (in.getEventType() == 1) {
                        break;
                    } else if ("b".equals(in.getName())) {
                        BlobMetadata blobMetadata = BlobMetadata.createFromXml(in, version, this.mContext);
                        blobMetadata.removeCommittersFromUnknownPkgs(allPackages);
                        blobMetadata.removeLeaseesFromUnknownPkgs(allPackages);
                        this.mCurrentMaxSessionId = Math.max(this.mCurrentMaxSessionId, blobMetadata.getBlobId());
                        if (version >= 6) {
                            addBlobLocked(blobMetadata);
                        } else {
                            BlobMetadata existingBlobMetadata = this.mBlobsMap.get(blobMetadata.getBlobHandle());
                            if (existingBlobMetadata == null) {
                                addBlobLocked(blobMetadata);
                            } else {
                                existingBlobMetadata.addCommittersAndLeasees(blobMetadata);
                                blobMetadata.getBlobFile().delete();
                            }
                        }
                    }
                }
                if (BlobStoreConfig.LOGV) {
                    Slog.v(BlobStoreConfig.TAG, "Finished reading blobs data");
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception e) {
                Slog.wtf(BlobStoreConfig.TAG, "Error reading blobs data", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeBlobsInfo() {
        synchronized (this.mBlobsLock) {
            try {
                writeBlobsInfoLocked();
            } catch (Exception e) {
            }
        }
    }

    private void writeBlobsInfoAsync() {
        if (!this.mHandler.hasCallbacks(this.mSaveBlobsInfoRunnable)) {
            this.mHandler.post(this.mSaveBlobsInfoRunnable);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeBlobSessions() {
        synchronized (this.mBlobsLock) {
            try {
                writeBlobSessionsLocked();
            } catch (Exception e) {
            }
        }
    }

    private void writeBlobSessionsAsync() {
        if (!this.mHandler.hasCallbacks(this.mSaveSessionsRunnable)) {
            this.mHandler.post(this.mSaveSessionsRunnable);
        }
    }

    private SparseArray<SparseArray<String>> getAllPackages() {
        SparseArray<SparseArray<String>> allPackages = new SparseArray<>();
        int[] allUsers = ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).getUserIds();
        for (int userId : allUsers) {
            SparseArray<String> userPackages = new SparseArray<>();
            allPackages.put(userId, userPackages);
            List<ApplicationInfo> applicationInfos = this.mPackageManagerInternal.getInstalledApplications(794624L, userId, Process.myUid());
            int count = applicationInfos.size();
            for (int i = 0; i < count; i++) {
                ApplicationInfo applicationInfo = applicationInfos.get(i);
                userPackages.put(applicationInfo.uid, applicationInfo.packageName);
            }
        }
        return allPackages;
    }

    private AtomicFile prepareSessionsIndexFile() {
        File file = BlobStoreConfig.prepareSessionIndexFile();
        if (file == null) {
            return null;
        }
        return new AtomicFile(file, "session_index");
    }

    private AtomicFile prepareBlobsIndexFile() {
        File file = BlobStoreConfig.prepareBlobsIndexFile();
        if (file == null) {
            return null;
        }
        return new AtomicFile(file, "blobs_index");
    }

    void handlePackageRemoved(final String packageName, final int uid) {
        synchronized (this.mBlobsLock) {
            LongSparseArray<BlobStoreSession> userSessions = getUserSessionsLocked(UserHandle.getUserId(uid));
            userSessions.removeIf(new LongObjPredicate() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda20
                public final boolean test(long j, Object obj) {
                    return BlobStoreManagerService.this.m2584x6ba071fe(uid, packageName, j, (BlobStoreSession) obj);
                }
            });
            writeBlobSessionsAsync();
            this.mBlobsMap.entrySet().removeIf(new Predicate() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda21
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return BlobStoreManagerService.this.m2585x6b2a0bff(packageName, uid, (Map.Entry) obj);
                }
            });
            writeBlobsInfoAsync();
            if (BlobStoreConfig.LOGV) {
                Slog.v(BlobStoreConfig.TAG, "Removed blobs data associated with pkg=" + packageName + ", uid=" + uid);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handlePackageRemoved$12$com-android-server-blob-BlobStoreManagerService  reason: not valid java name */
    public /* synthetic */ boolean m2584x6ba071fe(int uid, String packageName, long sessionId, BlobStoreSession blobStoreSession) {
        if (blobStoreSession.getOwnerUid() == uid && blobStoreSession.getOwnerPackageName().equals(packageName)) {
            deleteSessionLocked(blobStoreSession);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handlePackageRemoved$13$com-android-server-blob-BlobStoreManagerService  reason: not valid java name */
    public /* synthetic */ boolean m2585x6b2a0bff(String packageName, int uid, Map.Entry entry) {
        BlobMetadata blobMetadata = (BlobMetadata) entry.getValue();
        boolean isACommitter = blobMetadata.isACommitter(packageName, uid);
        if (isACommitter) {
            blobMetadata.removeCommitter(packageName, uid);
        }
        blobMetadata.removeLeasee(packageName, uid);
        if (blobMetadata.shouldBeDeleted(isACommitter)) {
            deleteBlobLocked(blobMetadata);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUserRemoved(final int userId) {
        synchronized (this.mBlobsLock) {
            LongSparseArray<BlobStoreSession> userSessions = (LongSparseArray) this.mSessions.removeReturnOld(userId);
            if (userSessions != null) {
                int count = userSessions.size();
                for (int i = 0; i < count; i++) {
                    BlobStoreSession session = userSessions.valueAt(i);
                    deleteSessionLocked(session);
                }
            }
            this.mBlobsMap.entrySet().removeIf(new Predicate() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda13
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return BlobStoreManagerService.this.m2586x6568d6f(userId, (Map.Entry) obj);
                }
            });
            if (BlobStoreConfig.LOGV) {
                Slog.v(BlobStoreConfig.TAG, "Removed blobs data in user " + userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleUserRemoved$14$com-android-server-blob-BlobStoreManagerService  reason: not valid java name */
    public /* synthetic */ boolean m2586x6568d6f(int userId, Map.Entry entry) {
        BlobMetadata blobMetadata = (BlobMetadata) entry.getValue();
        blobMetadata.removeDataForUser(userId);
        if (blobMetadata.shouldBeDeleted(true)) {
            deleteBlobLocked(blobMetadata);
            return true;
        }
        return false;
    }

    void handleIdleMaintenanceLocked() {
        File[] listFiles;
        final ArrayList<Long> deletedBlobIds = new ArrayList<>();
        ArrayList<File> filesToDelete = new ArrayList<>();
        File blobsDir = BlobStoreConfig.getBlobsDir();
        if (blobsDir.exists()) {
            for (File file : blobsDir.listFiles()) {
                try {
                    long id = Long.parseLong(file.getName());
                    if (this.mActiveBlobIds.indexOf(Long.valueOf(id)) < 0) {
                        filesToDelete.add(file);
                        deletedBlobIds.add(Long.valueOf(id));
                    }
                } catch (NumberFormatException e) {
                    Slog.wtf(BlobStoreConfig.TAG, "Error parsing the file name: " + file, e);
                    filesToDelete.add(file);
                }
            }
            int count = filesToDelete.size();
            for (int i = 0; i < count; i++) {
                filesToDelete.get(i).delete();
            }
        }
        this.mBlobsMap.entrySet().removeIf(new Predicate() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda16
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return BlobStoreManagerService.this.m2582xc7f3783c(deletedBlobIds, (Map.Entry) obj);
            }
        });
        writeBlobsInfoAsync();
        int userCount = this.mSessions.size();
        for (int i2 = 0; i2 < userCount; i2++) {
            LongSparseArray<BlobStoreSession> userSessions = this.mSessions.valueAt(i2);
            userSessions.removeIf(new LongObjPredicate() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda17
                public final boolean test(long j, Object obj) {
                    return BlobStoreManagerService.this.m2583xc77d123d(deletedBlobIds, j, (BlobStoreSession) obj);
                }
            });
        }
        Slog.d(BlobStoreConfig.TAG, "Completed idle maintenance; deleted " + Arrays.toString(deletedBlobIds.toArray()));
        writeBlobSessionsAsync();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleIdleMaintenanceLocked$15$com-android-server-blob-BlobStoreManagerService  reason: not valid java name */
    public /* synthetic */ boolean m2582xc7f3783c(ArrayList deletedBlobIds, Map.Entry entry) {
        BlobMetadata blobMetadata = (BlobMetadata) entry.getValue();
        blobMetadata.removeExpiredLeases();
        if (blobMetadata.shouldBeDeleted(true)) {
            deleteBlobLocked(blobMetadata);
            deletedBlobIds.add(Long.valueOf(blobMetadata.getBlobId()));
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleIdleMaintenanceLocked$16$com-android-server-blob-BlobStoreManagerService  reason: not valid java name */
    public /* synthetic */ boolean m2583xc77d123d(ArrayList deletedBlobIds, long sessionId, BlobStoreSession blobStoreSession) {
        boolean shouldRemove = false;
        if (blobStoreSession.isExpired()) {
            shouldRemove = true;
        }
        if (blobStoreSession.getBlobHandle().isExpired()) {
            shouldRemove = true;
        }
        if (shouldRemove) {
            deleteSessionLocked(blobStoreSession);
            deletedBlobIds.add(Long.valueOf(blobStoreSession.getSessionId()));
        }
        return shouldRemove;
    }

    private void deleteSessionLocked(BlobStoreSession blobStoreSession) {
        blobStoreSession.destroy();
        this.mActiveBlobIds.remove(Long.valueOf(blobStoreSession.getSessionId()));
    }

    private void deleteBlobLocked(BlobMetadata blobMetadata) {
        blobMetadata.destroy();
        this.mActiveBlobIds.remove(Long.valueOf(blobMetadata.getBlobId()));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void runClearAllSessions(int userId) {
        synchronized (this.mBlobsLock) {
            int userCount = this.mSessions.size();
            for (int i = 0; i < userCount; i++) {
                int sessionUserId = this.mSessions.keyAt(i);
                if (userId == -1 || userId == sessionUserId) {
                    LongSparseArray<BlobStoreSession> userSessions = this.mSessions.valueAt(i);
                    int sessionsCount = userSessions.size();
                    for (int j = 0; j < sessionsCount; j++) {
                        this.mActiveBlobIds.remove(Long.valueOf(userSessions.valueAt(j).getSessionId()));
                    }
                }
            }
            if (userId == -1) {
                this.mSessions.clear();
            } else {
                this.mSessions.remove(userId);
            }
            writeBlobSessionsAsync();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void runClearAllBlobs(final int userId) {
        synchronized (this.mBlobsLock) {
            this.mBlobsMap.entrySet().removeIf(new Predicate() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda3
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return BlobStoreManagerService.this.m2590xa5cb7cee(userId, (Map.Entry) obj);
                }
            });
            writeBlobsInfoAsync();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$runClearAllBlobs$17$com-android-server-blob-BlobStoreManagerService  reason: not valid java name */
    public /* synthetic */ boolean m2590xa5cb7cee(int userId, Map.Entry entry) {
        BlobMetadata blobMetadata = (BlobMetadata) entry.getValue();
        if (userId == -1) {
            this.mActiveBlobIds.remove(Long.valueOf(blobMetadata.getBlobId()));
            return true;
        }
        blobMetadata.removeDataForUser(userId);
        if (!blobMetadata.shouldBeDeleted(false)) {
            return false;
        }
        this.mActiveBlobIds.remove(Long.valueOf(blobMetadata.getBlobId()));
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void deleteBlob(BlobHandle blobHandle, int userId) {
        synchronized (this.mBlobsLock) {
            BlobMetadata blobMetadata = this.mBlobsMap.get(blobHandle);
            if (blobMetadata == null) {
                return;
            }
            blobMetadata.removeDataForUser(userId);
            if (blobMetadata.shouldBeDeleted(false)) {
                deleteBlobLocked(blobMetadata);
                this.mBlobsMap.remove(blobHandle);
            }
            writeBlobsInfoAsync();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void runIdleMaintenance() {
        synchronized (this.mBlobsLock) {
            handleIdleMaintenanceLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBlobAvailable(long blobId, int userId) {
        synchronized (this.mBlobsLock) {
            int blobCount = this.mBlobsMap.size();
            for (int i = 0; i < blobCount; i++) {
                BlobMetadata blobMetadata = this.mBlobsMap.valueAt(i);
                if (blobMetadata.getBlobId() == blobId) {
                    return blobMetadata.hasACommitterInUser(userId);
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpSessionsLocked(IndentingPrintWriter fout, DumpArgs dumpArgs) {
        int userCount = this.mSessions.size();
        for (int i = 0; i < userCount; i++) {
            int userId = this.mSessions.keyAt(i);
            if (dumpArgs.shouldDumpUser(userId)) {
                LongSparseArray<BlobStoreSession> userSessions = this.mSessions.valueAt(i);
                fout.println("List of sessions in user #" + userId + " (" + userSessions.size() + "):");
                fout.increaseIndent();
                int sessionsCount = userSessions.size();
                for (int j = 0; j < sessionsCount; j++) {
                    long sessionId = userSessions.keyAt(j);
                    BlobStoreSession session = userSessions.valueAt(j);
                    if (dumpArgs.shouldDumpSession(session.getOwnerPackageName(), session.getOwnerUid(), session.getSessionId())) {
                        fout.println("Session #" + sessionId);
                        fout.increaseIndent();
                        session.dump(fout, dumpArgs);
                        fout.decreaseIndent();
                    }
                }
                fout.decreaseIndent();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpBlobsLocked(IndentingPrintWriter fout, DumpArgs dumpArgs) {
        fout.println("List of blobs (" + this.mBlobsMap.size() + "):");
        fout.increaseIndent();
        int blobCount = this.mBlobsMap.size();
        for (int i = 0; i < blobCount; i++) {
            BlobMetadata blobMetadata = this.mBlobsMap.valueAt(i);
            if (dumpArgs.shouldDumpBlob(blobMetadata.getBlobId())) {
                fout.println("Blob #" + blobMetadata.getBlobId());
                fout.increaseIndent();
                blobMetadata.dump(fout, dumpArgs);
                fout.decreaseIndent();
            }
        }
        if (this.mBlobsMap.isEmpty()) {
            fout.println("<empty>");
        }
        fout.decreaseIndent();
    }

    /* loaded from: classes.dex */
    private class BlobStorageStatsAugmenter implements StorageStatsManagerLocal.StorageStatsAugmenter {
        private BlobStorageStatsAugmenter() {
        }

        @Override // com.android.server.usage.StorageStatsManagerLocal.StorageStatsAugmenter
        public void augmentStatsForPackageForUser(PackageStats stats, final String packageName, final UserHandle userHandle, final boolean callerHasStatsPermission) {
            final AtomicLong blobsDataSize = new AtomicLong(0L);
            BlobStoreManagerService.this.forEachSessionInUser(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$BlobStorageStatsAugmenter$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BlobStoreManagerService.BlobStorageStatsAugmenter.lambda$augmentStatsForPackageForUser$0(packageName, blobsDataSize, (BlobStoreSession) obj);
                }
            }, userHandle.getIdentifier());
            BlobStoreManagerService.this.forEachBlob(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$BlobStorageStatsAugmenter$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BlobStoreManagerService.BlobStorageStatsAugmenter.lambda$augmentStatsForPackageForUser$1(packageName, userHandle, callerHasStatsPermission, blobsDataSize, (BlobMetadata) obj);
                }
            });
            stats.dataSize += blobsDataSize.get();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$augmentStatsForPackageForUser$0(String packageName, AtomicLong blobsDataSize, BlobStoreSession session) {
            if (session.getOwnerPackageName().equals(packageName)) {
                blobsDataSize.getAndAdd(session.getSize());
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$augmentStatsForPackageForUser$1(String packageName, UserHandle userHandle, boolean callerHasStatsPermission, AtomicLong blobsDataSize, BlobMetadata blobMetadata) {
            if (blobMetadata.shouldAttributeToLeasee(packageName, userHandle.getIdentifier(), callerHasStatsPermission)) {
                blobsDataSize.getAndAdd(blobMetadata.getSize());
            }
        }

        @Override // com.android.server.usage.StorageStatsManagerLocal.StorageStatsAugmenter
        public void augmentStatsForUid(PackageStats stats, final int uid, final boolean callerHasStatsPermission) {
            int userId = UserHandle.getUserId(uid);
            final AtomicLong blobsDataSize = new AtomicLong(0L);
            BlobStoreManagerService.this.forEachSessionInUser(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$BlobStorageStatsAugmenter$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BlobStoreManagerService.BlobStorageStatsAugmenter.lambda$augmentStatsForUid$2(uid, blobsDataSize, (BlobStoreSession) obj);
                }
            }, userId);
            BlobStoreManagerService.this.forEachBlob(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$BlobStorageStatsAugmenter$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BlobStoreManagerService.BlobStorageStatsAugmenter.lambda$augmentStatsForUid$3(uid, callerHasStatsPermission, blobsDataSize, (BlobMetadata) obj);
                }
            });
            stats.dataSize += blobsDataSize.get();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$augmentStatsForUid$2(int uid, AtomicLong blobsDataSize, BlobStoreSession session) {
            if (session.getOwnerUid() == uid) {
                blobsDataSize.getAndAdd(session.getSize());
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$augmentStatsForUid$3(int uid, boolean callerHasStatsPermission, AtomicLong blobsDataSize, BlobMetadata blobMetadata) {
            if (blobMetadata.shouldAttributeToLeasee(uid, callerHasStatsPermission)) {
                blobsDataSize.getAndAdd(blobMetadata.getSize());
            }
        }

        @Override // com.android.server.usage.StorageStatsManagerLocal.StorageStatsAugmenter
        public void augmentStatsForUser(PackageStats stats, final UserHandle userHandle) {
            final AtomicLong blobsDataSize = new AtomicLong(0L);
            BlobStoreManagerService.this.forEachSessionInUser(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$BlobStorageStatsAugmenter$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    blobsDataSize.getAndAdd(((BlobStoreSession) obj).getSize());
                }
            }, userHandle.getIdentifier());
            BlobStoreManagerService.this.forEachBlob(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$BlobStorageStatsAugmenter$$ExternalSyntheticLambda5
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BlobStoreManagerService.BlobStorageStatsAugmenter.lambda$augmentStatsForUser$5(userHandle, blobsDataSize, (BlobMetadata) obj);
                }
            });
            stats.dataSize += blobsDataSize.get();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$augmentStatsForUser$5(UserHandle userHandle, AtomicLong blobsDataSize, BlobMetadata blobMetadata) {
            if (blobMetadata.shouldAttributeToUser(userHandle.getIdentifier())) {
                blobsDataSize.getAndAdd(blobMetadata.getSize());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void forEachSessionInUser(Consumer<BlobStoreSession> consumer, int userId) {
        synchronized (this.mBlobsLock) {
            LongSparseArray<BlobStoreSession> userSessions = getUserSessionsLocked(userId);
            int count = userSessions.size();
            for (int i = 0; i < count; i++) {
                BlobStoreSession session = userSessions.valueAt(i);
                consumer.accept(session);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void forEachBlob(Consumer<BlobMetadata> consumer) {
        synchronized (this.mBlobsMap) {
            forEachBlobLocked(consumer);
        }
    }

    private void forEachBlobLocked(Consumer<BlobMetadata> consumer) {
        int count = this.mBlobsMap.size();
        for (int blobIdx = 0; blobIdx < count; blobIdx++) {
            BlobMetadata blobMetadata = this.mBlobsMap.valueAt(blobIdx);
            consumer.accept(blobMetadata);
        }
    }

    private void forEachBlobLocked(BiConsumer<BlobHandle, BlobMetadata> consumer) {
        int count = this.mBlobsMap.size();
        for (int blobIdx = 0; blobIdx < count; blobIdx++) {
            BlobHandle blobHandle = this.mBlobsMap.keyAt(blobIdx);
            BlobMetadata blobMetadata = this.mBlobsMap.valueAt(blobIdx);
            consumer.accept(blobHandle, blobMetadata);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAllowedBlobStoreAccess(int uid, String packageName) {
        return (Process.isSdkSandboxUid(uid) || Process.isIsolated(uid) || this.mPackageManagerInternal.isInstantApp(packageName, UserHandle.getUserId(uid))) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PackageChangedReceiver extends BroadcastReceiver {
        private PackageChangedReceiver() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            boolean z;
            if (BlobStoreConfig.LOGV) {
                Slog.v(BlobStoreConfig.TAG, "Received " + intent);
            }
            String action = intent.getAction();
            switch (action.hashCode()) {
                case 267468725:
                    if (action.equals("android.intent.action.PACKAGE_DATA_CLEARED")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case 1580442797:
                    if (action.equals("android.intent.action.PACKAGE_FULLY_REMOVED")) {
                        z = false;
                        break;
                    }
                    z = true;
                    break;
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                case true:
                    String packageName = intent.getData().getSchemeSpecificPart();
                    if (packageName == null) {
                        Slog.wtf(BlobStoreConfig.TAG, "Package name is missing in the intent: " + intent);
                        return;
                    }
                    int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                    if (uid == -1) {
                        Slog.wtf(BlobStoreConfig.TAG, "uid is missing in the intent: " + intent);
                        return;
                    } else {
                        BlobStoreManagerService.this.handlePackageRemoved(packageName, uid);
                        return;
                    }
                default:
                    Slog.wtf(BlobStoreConfig.TAG, "Received unknown intent: " + intent);
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class UserActionReceiver extends BroadcastReceiver {
        private UserActionReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (BlobStoreConfig.LOGV) {
                Slog.v(BlobStoreConfig.TAG, "Received: " + intent);
            }
            String action = intent.getAction();
            char c = 65535;
            switch (action.hashCode()) {
                case -2061058799:
                    if (action.equals("android.intent.action.USER_REMOVED")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    if (userId == -10000) {
                        Slog.wtf(BlobStoreConfig.TAG, "userId is missing in the intent: " + intent);
                        return;
                    } else {
                        BlobStoreManagerService.this.handleUserRemoved(userId);
                        return;
                    }
                default:
                    Slog.wtf(BlobStoreConfig.TAG, "Received unknown intent: " + intent);
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class Stub extends IBlobStoreManager.Stub {
        private Stub() {
        }

        public long createSession(BlobHandle blobHandle, String packageName) {
            Objects.requireNonNull(blobHandle, "blobHandle must not be null");
            blobHandle.assertIsValid();
            Objects.requireNonNull(packageName, "packageName must not be null");
            int callingUid = Binder.getCallingUid();
            BlobStoreManagerService.this.verifyCallingPackage(callingUid, packageName);
            if (!BlobStoreManagerService.this.isAllowedBlobStoreAccess(callingUid, packageName)) {
                throw new SecurityException("Caller not allowed to create session; callingUid=" + callingUid + ", callingPackage=" + packageName);
            }
            try {
                return BlobStoreManagerService.this.createSessionInternal(blobHandle, callingUid, packageName);
            } catch (LimitExceededException e) {
                throw new ParcelableException(e);
            }
        }

        public IBlobStoreSession openSession(long sessionId, String packageName) {
            Preconditions.checkArgumentPositive((float) sessionId, "sessionId must be positive: " + sessionId);
            Objects.requireNonNull(packageName, "packageName must not be null");
            int callingUid = Binder.getCallingUid();
            BlobStoreManagerService.this.verifyCallingPackage(callingUid, packageName);
            return BlobStoreManagerService.this.openSessionInternal(sessionId, callingUid, packageName);
        }

        public void abandonSession(long sessionId, String packageName) {
            Preconditions.checkArgumentPositive((float) sessionId, "sessionId must be positive: " + sessionId);
            Objects.requireNonNull(packageName, "packageName must not be null");
            int callingUid = Binder.getCallingUid();
            BlobStoreManagerService.this.verifyCallingPackage(callingUid, packageName);
            BlobStoreManagerService.this.abandonSessionInternal(sessionId, callingUid, packageName);
        }

        public ParcelFileDescriptor openBlob(BlobHandle blobHandle, String packageName) {
            Objects.requireNonNull(blobHandle, "blobHandle must not be null");
            blobHandle.assertIsValid();
            Objects.requireNonNull(packageName, "packageName must not be null");
            int callingUid = Binder.getCallingUid();
            BlobStoreManagerService.this.verifyCallingPackage(callingUid, packageName);
            if (!BlobStoreManagerService.this.isAllowedBlobStoreAccess(callingUid, packageName)) {
                throw new SecurityException("Caller not allowed to open blob; callingUid=" + callingUid + ", callingPackage=" + packageName);
            }
            try {
                return BlobStoreManagerService.this.openBlobInternal(blobHandle, callingUid, packageName);
            } catch (IOException e) {
                throw ExceptionUtils.wrap(e);
            }
        }

        public void acquireLease(BlobHandle blobHandle, int descriptionResId, CharSequence description, long leaseExpiryTimeMillis, String packageName) {
            Objects.requireNonNull(blobHandle, "blobHandle must not be null");
            blobHandle.assertIsValid();
            Preconditions.checkArgument(ResourceId.isValid(descriptionResId) || description != null, "Description must be valid; descriptionId=" + descriptionResId + ", description=" + ((Object) description));
            Preconditions.checkArgumentNonnegative(leaseExpiryTimeMillis, "leaseExpiryTimeMillis must not be negative");
            Objects.requireNonNull(packageName, "packageName must not be null");
            CharSequence description2 = BlobStoreConfig.getTruncatedLeaseDescription(description);
            int callingUid = Binder.getCallingUid();
            BlobStoreManagerService.this.verifyCallingPackage(callingUid, packageName);
            if (!BlobStoreManagerService.this.isAllowedBlobStoreAccess(callingUid, packageName)) {
                throw new SecurityException("Caller not allowed to open blob; callingUid=" + callingUid + ", callingPackage=" + packageName);
            }
            try {
                BlobStoreManagerService.this.acquireLeaseInternal(blobHandle, descriptionResId, description2, leaseExpiryTimeMillis, callingUid, packageName);
            } catch (Resources.NotFoundException e) {
                throw new IllegalArgumentException(e);
            } catch (LimitExceededException e2) {
                throw new ParcelableException(e2);
            }
        }

        public void releaseLease(BlobHandle blobHandle, String packageName) {
            Objects.requireNonNull(blobHandle, "blobHandle must not be null");
            blobHandle.assertIsValid();
            Objects.requireNonNull(packageName, "packageName must not be null");
            int callingUid = Binder.getCallingUid();
            BlobStoreManagerService.this.verifyCallingPackage(callingUid, packageName);
            if (!BlobStoreManagerService.this.isAllowedBlobStoreAccess(callingUid, packageName)) {
                throw new SecurityException("Caller not allowed to open blob; callingUid=" + callingUid + ", callingPackage=" + packageName);
            }
            BlobStoreManagerService.this.releaseLeaseInternal(blobHandle, callingUid, packageName);
        }

        public void releaseAllLeases(String packageName) {
            Objects.requireNonNull(packageName, "packageName must not be null");
            int callingUid = Binder.getCallingUid();
            BlobStoreManagerService.this.verifyCallingPackage(callingUid, packageName);
            if (!BlobStoreManagerService.this.isAllowedBlobStoreAccess(callingUid, packageName)) {
                throw new SecurityException("Caller not allowed to open blob; callingUid=" + callingUid + ", callingPackage=" + packageName);
            }
            BlobStoreManagerService.this.releaseAllLeasesInternal(callingUid, packageName);
        }

        public long getRemainingLeaseQuotaBytes(String packageName) {
            int callingUid = Binder.getCallingUid();
            BlobStoreManagerService.this.verifyCallingPackage(callingUid, packageName);
            return BlobStoreManagerService.this.getRemainingLeaseQuotaBytesInternal(callingUid, packageName);
        }

        public void waitForIdle(final RemoteCallback remoteCallback) {
            Objects.requireNonNull(remoteCallback, "remoteCallback must not be null");
            BlobStoreManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", "Caller is not allowed to call this; caller=" + Binder.getCallingUid());
            BlobStoreManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.blob.BlobStoreManagerService$Stub$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    BlobStoreManagerService.Stub.this.m2593xad40b822(remoteCallback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$waitForIdle$1$com-android-server-blob-BlobStoreManagerService$Stub  reason: not valid java name */
        public /* synthetic */ void m2593xad40b822(final RemoteCallback remoteCallback) {
            BlobStoreManagerService.this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.blob.BlobStoreManagerService$Stub$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    BlobStoreManagerService.Stub.this.m2592x93253983(remoteCallback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$waitForIdle$0$com-android-server-blob-BlobStoreManagerService$Stub  reason: not valid java name */
        public /* synthetic */ void m2592x93253983(final RemoteCallback remoteCallback) {
            Handler handler = BlobStoreManagerService.this.mHandler;
            Objects.requireNonNull(remoteCallback);
            handler.post(PooledLambda.obtainRunnable(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$Stub$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    remoteCallback.sendResult((Bundle) obj);
                }
            }, (Object) null).recycleOnUse());
        }

        public List<BlobInfo> queryBlobsForUser(int userId) {
            if (Binder.getCallingUid() != 1000) {
                throw new SecurityException("Only system uid is allowed to call queryBlobsForUser()");
            }
            int resolvedUserId = userId == -2 ? ActivityManager.getCurrentUser() : userId;
            ActivityManagerInternal amInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            amInternal.ensureNotSpecialUser(resolvedUserId);
            return BlobStoreManagerService.this.queryBlobsForUserInternal(resolvedUserId);
        }

        public void deleteBlob(long blobId) {
            int callingUid = Binder.getCallingUid();
            if (callingUid != 1000) {
                throw new SecurityException("Only system uid is allowed to call deleteBlob()");
            }
            BlobStoreManagerService.this.deleteBlobInternal(blobId, callingUid);
        }

        public List<BlobHandle> getLeasedBlobs(String packageName) {
            Objects.requireNonNull(packageName, "packageName must not be null");
            int callingUid = Binder.getCallingUid();
            BlobStoreManagerService.this.verifyCallingPackage(callingUid, packageName);
            return BlobStoreManagerService.this.getLeasedBlobsInternal(callingUid, packageName);
        }

        public LeaseInfo getLeaseInfo(BlobHandle blobHandle, String packageName) {
            Objects.requireNonNull(blobHandle, "blobHandle must not be null");
            blobHandle.assertIsValid();
            Objects.requireNonNull(packageName, "packageName must not be null");
            int callingUid = Binder.getCallingUid();
            BlobStoreManagerService.this.verifyCallingPackage(callingUid, packageName);
            if (!BlobStoreManagerService.this.isAllowedBlobStoreAccess(callingUid, packageName)) {
                throw new SecurityException("Caller not allowed to open blob; callingUid=" + callingUid + ", callingPackage=" + packageName);
            }
            return BlobStoreManagerService.this.getLeaseInfoInternal(blobHandle, callingUid, packageName);
        }

        public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
            if (DumpUtils.checkDumpAndUsageStatsPermission(BlobStoreManagerService.this.mContext, BlobStoreConfig.TAG, writer)) {
                DumpArgs dumpArgs = DumpArgs.parse(args);
                IndentingPrintWriter fout = new IndentingPrintWriter(writer, "    ");
                if (dumpArgs.shouldDumpHelp()) {
                    writer.println("dumpsys blob_store [options]:");
                    fout.increaseIndent();
                    dumpArgs.dumpArgsUsage(fout);
                    fout.decreaseIndent();
                    return;
                }
                synchronized (BlobStoreManagerService.this.mBlobsLock) {
                    if (dumpArgs.shouldDumpAllSections()) {
                        fout.println("mCurrentMaxSessionId: " + BlobStoreManagerService.this.mCurrentMaxSessionId);
                        fout.println();
                    }
                    if (dumpArgs.shouldDumpSessions()) {
                        BlobStoreManagerService.this.dumpSessionsLocked(fout, dumpArgs);
                        fout.println();
                    }
                    if (dumpArgs.shouldDumpBlobs()) {
                        BlobStoreManagerService.this.dumpBlobsLocked(fout, dumpArgs);
                        fout.println();
                    }
                }
                if (dumpArgs.shouldDumpConfig()) {
                    fout.println("BlobStore config:");
                    fout.increaseIndent();
                    BlobStoreConfig.dump(fout, BlobStoreManagerService.this.mContext);
                    fout.decreaseIndent();
                    fout.println();
                }
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r6v0, resolved type: com.android.server.blob.BlobStoreManagerService$Stub */
        /* JADX WARN: Multi-variable type inference failed */
        public int handleShellCommand(ParcelFileDescriptor in, ParcelFileDescriptor out, ParcelFileDescriptor err, String[] args) {
            return new BlobStoreManagerShellCommand(BlobStoreManagerService.this).exec(this, in.getFileDescriptor(), out.getFileDescriptor(), err.getFileDescriptor(), args);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class DumpArgs {
        private static final int FLAG_DUMP_BLOBS = 2;
        private static final int FLAG_DUMP_CONFIG = 4;
        private static final int FLAG_DUMP_SESSIONS = 1;
        private boolean mDumpAll;
        private boolean mDumpHelp;
        private boolean mDumpUnredacted;
        private int mSelectedSectionFlags;
        private final ArrayList<String> mDumpPackages = new ArrayList<>();
        private final ArrayList<Integer> mDumpUids = new ArrayList<>();
        private final ArrayList<Integer> mDumpUserIds = new ArrayList<>();
        private final ArrayList<Long> mDumpBlobIds = new ArrayList<>();

        public boolean shouldDumpSession(String packageName, int uid, long blobId) {
            if (CollectionUtils.isEmpty(this.mDumpPackages) || this.mDumpPackages.indexOf(packageName) >= 0) {
                if (CollectionUtils.isEmpty(this.mDumpUids) || this.mDumpUids.indexOf(Integer.valueOf(uid)) >= 0) {
                    return CollectionUtils.isEmpty(this.mDumpBlobIds) || this.mDumpBlobIds.indexOf(Long.valueOf(blobId)) >= 0;
                }
                return false;
            }
            return false;
        }

        public boolean shouldDumpAllSections() {
            return this.mDumpAll || this.mSelectedSectionFlags == 0;
        }

        public void allowDumpSessions() {
            this.mSelectedSectionFlags |= 1;
        }

        public boolean shouldDumpSessions() {
            return shouldDumpAllSections() || (this.mSelectedSectionFlags & 1) != 0;
        }

        public void allowDumpBlobs() {
            this.mSelectedSectionFlags |= 2;
        }

        public boolean shouldDumpBlobs() {
            return shouldDumpAllSections() || (this.mSelectedSectionFlags & 2) != 0;
        }

        public void allowDumpConfig() {
            this.mSelectedSectionFlags |= 4;
        }

        public boolean shouldDumpConfig() {
            return shouldDumpAllSections() || (this.mSelectedSectionFlags & 4) != 0;
        }

        public boolean shouldDumpBlob(long blobId) {
            return CollectionUtils.isEmpty(this.mDumpBlobIds) || this.mDumpBlobIds.indexOf(Long.valueOf(blobId)) >= 0;
        }

        public boolean shouldDumpFull() {
            return this.mDumpUnredacted;
        }

        public boolean shouldDumpUser(int userId) {
            return CollectionUtils.isEmpty(this.mDumpUserIds) || this.mDumpUserIds.indexOf(Integer.valueOf(userId)) >= 0;
        }

        public boolean shouldDumpHelp() {
            return this.mDumpHelp;
        }

        private DumpArgs() {
        }

        public static DumpArgs parse(String[] args) {
            DumpArgs dumpArgs = new DumpArgs();
            if (args == null) {
                return dumpArgs;
            }
            int i = 0;
            while (i < args.length) {
                String opt = args[i];
                if ("--all".equals(opt) || "-a".equals(opt)) {
                    dumpArgs.mDumpAll = true;
                } else if ("--unredacted".equals(opt) || "-u".equals(opt)) {
                    int callingUid = Binder.getCallingUid();
                    if (callingUid == 2000 || callingUid == 0) {
                        dumpArgs.mDumpUnredacted = true;
                    }
                } else if ("--sessions".equals(opt)) {
                    dumpArgs.allowDumpSessions();
                } else if ("--blobs".equals(opt)) {
                    dumpArgs.allowDumpBlobs();
                } else if ("--config".equals(opt)) {
                    dumpArgs.allowDumpConfig();
                } else if ("--package".equals(opt) || "-p".equals(opt)) {
                    i++;
                    dumpArgs.mDumpPackages.add(getStringArgRequired(args, i, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME));
                } else if ("--uid".equals(opt)) {
                    i++;
                    dumpArgs.mDumpUids.add(Integer.valueOf(getIntArgRequired(args, i, WatchlistLoggingHandler.WatchlistEventKeys.UID)));
                } else if ("--user".equals(opt)) {
                    i++;
                    dumpArgs.mDumpUserIds.add(Integer.valueOf(getIntArgRequired(args, i, "userId")));
                } else if ("--blob".equals(opt) || "-b".equals(opt)) {
                    i++;
                    dumpArgs.mDumpBlobIds.add(Long.valueOf(getLongArgRequired(args, i, "blobId")));
                } else if ("--help".equals(opt) || "-h".equals(opt)) {
                    dumpArgs.mDumpHelp = true;
                } else {
                    dumpArgs.mDumpBlobIds.add(Long.valueOf(getLongArgRequired(args, i, "blobId")));
                }
                i++;
            }
            return dumpArgs;
        }

        private static String getStringArgRequired(String[] args, int index, String argName) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing " + argName);
            }
            return args[index];
        }

        private static int getIntArgRequired(String[] args, int index, String argName) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing " + argName);
            }
            try {
                int value = Integer.parseInt(args[index]);
                return value;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid " + argName + ": " + args[index]);
            }
        }

        private static long getLongArgRequired(String[] args, int index, String argName) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing " + argName);
            }
            try {
                long value = Long.parseLong(args[index]);
                return value;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid " + argName + ": " + args[index]);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dumpArgsUsage(IndentingPrintWriter pw) {
            pw.println("--help | -h");
            printWithIndent(pw, "Dump this help text");
            pw.println("--sessions");
            printWithIndent(pw, "Dump only the sessions info");
            pw.println("--blobs");
            printWithIndent(pw, "Dump only the committed blobs info");
            pw.println("--config");
            printWithIndent(pw, "Dump only the config values");
            pw.println("--package | -p [package-name]");
            printWithIndent(pw, "Dump blobs info associated with the given package");
            pw.println("--uid | -u [uid]");
            printWithIndent(pw, "Dump blobs info associated with the given uid");
            pw.println("--user [user-id]");
            printWithIndent(pw, "Dump blobs info in the given user");
            pw.println("--blob | -b [session-id | blob-id]");
            printWithIndent(pw, "Dump blob info corresponding to the given ID");
            pw.println("--full | -f");
            printWithIndent(pw, "Dump full unredacted blobs data");
        }

        private void printWithIndent(IndentingPrintWriter pw, String str) {
            pw.increaseIndent();
            pw.println(str);
            pw.decreaseIndent();
        }
    }

    private void registerBlobStorePuller() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.BLOB_INFO, (StatsManager.PullAtomMetadata) null, BackgroundThread.getExecutor(), this.mStatsCallbackImpl);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class StatsPullAtomCallbackImpl implements StatsManager.StatsPullAtomCallback {
        private StatsPullAtomCallbackImpl() {
        }

        public int onPullAtom(int atomTag, List<StatsEvent> data) {
            switch (atomTag) {
                case FrameworkStatsLog.BLOB_INFO /* 10081 */:
                    return BlobStoreManagerService.this.pullBlobData(atomTag, data);
                default:
                    throw new UnsupportedOperationException("Unknown tagId=" + atomTag);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int pullBlobData(final int atomTag, final List<StatsEvent> data) {
        forEachBlob(new Consumer() { // from class: com.android.server.blob.BlobStoreManagerService$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                data.add(((BlobMetadata) obj).dumpAsStatsEvent(atomTag));
            }
        });
        return 0;
    }

    /* loaded from: classes.dex */
    private class LocalService extends BlobStoreManagerInternal {
        private LocalService() {
        }

        @Override // com.android.server.blob.BlobStoreManagerInternal
        public void onIdleMaintenance() {
            BlobStoreManagerService.this.runIdleMaintenance();
        }
    }

    /* loaded from: classes.dex */
    static class Injector {
        Injector() {
        }

        public Handler initializeMessageHandler() {
            return BlobStoreManagerService.m2580$$Nest$sminitializeMessageHandler();
        }

        public Handler getBackgroundHandler() {
            return BackgroundThread.getHandler();
        }
    }
}
