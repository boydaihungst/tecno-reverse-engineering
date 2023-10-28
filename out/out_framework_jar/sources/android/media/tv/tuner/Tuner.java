package android.media.tv.tuner;

import android.Manifest;
import android.annotation.SystemApi;
import android.app.PendingIntent$$ExternalSyntheticLambda1;
import android.content.Context;
import android.media.MediaMetrics;
import android.media.tv.tuner.Tuner;
import android.media.tv.tuner.dvr.DvrPlayback;
import android.media.tv.tuner.dvr.DvrRecorder;
import android.media.tv.tuner.dvr.OnPlaybackStatusChangedListener;
import android.media.tv.tuner.dvr.OnRecordStatusChangedListener;
import android.media.tv.tuner.filter.Filter;
import android.media.tv.tuner.filter.FilterCallback;
import android.media.tv.tuner.filter.SharedFilter;
import android.media.tv.tuner.filter.SharedFilterCallback;
import android.media.tv.tuner.filter.TimeFilter;
import android.media.tv.tuner.frontend.Atsc3PlpInfo;
import android.media.tv.tuner.frontend.FrontendInfo;
import android.media.tv.tuner.frontend.FrontendSettings;
import android.media.tv.tuner.frontend.FrontendStatus;
import android.media.tv.tuner.frontend.FrontendStatusReadiness;
import android.media.tv.tuner.frontend.OnTuneEventListener;
import android.media.tv.tuner.frontend.ScanCallback;
import android.media.tv.tunerresourcemanager.ResourceClientProfile;
import android.media.tv.tunerresourcemanager.TunerCiCamRequest;
import android.media.tv.tunerresourcemanager.TunerDemuxRequest;
import android.media.tv.tunerresourcemanager.TunerDescramblerRequest;
import android.media.tv.tunerresourcemanager.TunerFrontendRequest;
import android.media.tv.tunerresourcemanager.TunerLnbRequest;
import android.media.tv.tunerresourcemanager.TunerResourceManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import com.android.internal.util.FrameworkStatsLog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntFunction;
import java.util.function.Predicate;
@SystemApi
/* loaded from: classes2.dex */
public class Tuner implements AutoCloseable {
    public static final int DVR_TYPE_PLAYBACK = 1;
    public static final int DVR_TYPE_RECORD = 0;
    private static final int FILTER_CLEANUP_THRESHOLD = 256;
    public static final int INVALID_AV_SYNC_ID = -1;
    public static final int INVALID_FILTER_ID = -1;
    public static final long INVALID_FILTER_ID_LONG = -1;
    public static final int INVALID_FIRST_MACROBLOCK_IN_SLICE = -1;
    public static final int INVALID_FRONTEND_ID = -1;
    public static final int INVALID_FRONTEND_SETTING_FREQUENCY = -1;
    public static final int INVALID_LNB_ID = -1;
    public static final int INVALID_LTS_ID = -1;
    public static final int INVALID_MMTP_RECORD_EVENT_MPT_SEQUENCE_NUM = -1;
    public static final int INVALID_STREAM_ID = 65535;
    public static final long INVALID_TIMESTAMP = -1;
    public static final int INVALID_TS_PID = 65535;
    private static final int MSG_ON_FILTER_EVENT = 2;
    private static final int MSG_ON_FILTER_STATUS = 3;
    private static final int MSG_ON_LNB_EVENT = 4;
    private static final int MSG_RESOURCE_LOST = 1;
    public static final int RESULT_INVALID_ARGUMENT = 4;
    public static final int RESULT_INVALID_STATE = 3;
    public static final int RESULT_NOT_INITIALIZED = 2;
    public static final int RESULT_OUT_OF_MEMORY = 5;
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_UNAVAILABLE = 1;
    public static final int RESULT_UNKNOWN_ERROR = 6;
    public static final int SCAN_TYPE_AUTO = 1;
    public static final int SCAN_TYPE_BLIND = 2;
    public static final int SCAN_TYPE_UNDEFINED = 0;
    private static int sTunerVersion;
    private final int mClientId;
    private final Context mContext;
    private Integer mDemuxHandle;
    private Frontend mFrontend;
    private Integer mFrontendCiCamHandle;
    private Integer mFrontendCiCamId;
    private Integer mFrontendHandle;
    private FrontendInfo mFrontendInfo;
    private EventHandler mHandler;
    private Lnb mLnb;
    private Integer mLnbHandle;
    private long mNativeContext;
    private OnResourceLostListener mOnResourceLostListener;
    private Executor mOnResourceLostListenerExecutor;
    private Executor mOnTuneEventExecutor;
    private OnTuneEventListener mOnTuneEventListener;
    private int mRequestedCiCamId;
    private final TunerResourceManager.ResourcesReclaimListener mResourceListener;
    private ScanCallback mScanCallback;
    private Executor mScanCallbackExecutor;
    private final TunerResourceManager mTunerResourceManager;
    private int mUserId;
    public static final byte[] VOID_KEYTOKEN = {0};
    private static final String TAG = "MediaTvTuner";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private Tuner mFeOwnerTuner = null;
    private int mFrontendType = 0;
    private final Object mOnTuneEventLock = new Object();
    private final Object mScanCallbackLock = new Object();
    private final Object mOnResourceLostListenerLock = new Object();
    private final ReentrantLock mFrontendLock = new ReentrantLock();
    private final ReentrantLock mLnbLock = new ReentrantLock();
    private final ReentrantLock mFrontendCiCamLock = new ReentrantLock();
    private final ReentrantLock mDemuxLock = new ReentrantLock();
    private Map<Integer, WeakReference<Descrambler>> mDescramblers = new HashMap();
    private List<WeakReference<Filter>> mFilters = new ArrayList();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface DvrType {
    }

    /* loaded from: classes2.dex */
    public interface OnResourceLostListener {
        void onResourceLost(Tuner tuner);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface Result {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ScanType {
    }

    private native int nativeClose();

    private native int nativeCloseDemux(int i);

    private native int nativeCloseFrontend(int i);

    private native int nativeConnectCiCam(int i);

    private native int nativeDisconnectCiCam();

    private native Integer nativeGetAvSyncHwId(Filter filter);

    private native Long nativeGetAvSyncTime(int i);

    private native DemuxCapabilities nativeGetDemuxCapabilities();

    private native String nativeGetFrontendHardwareInfo();

    private native List<Integer> nativeGetFrontendIds();

    private native FrontendInfo nativeGetFrontendInfo(int i);

    private native FrontendStatus nativeGetFrontendStatus(int[] iArr);

    private native FrontendStatusReadiness[] nativeGetFrontendStatusReadiness(int[] iArr);

    private native int nativeGetMaxNumberOfFrontends(int i);

    private native int nativeGetTunerVersion();

    private static native void nativeInit();

    private native int nativeLinkCiCam(int i);

    private native int nativeOpenDemuxByhandle(int i);

    private native Descrambler nativeOpenDescramblerByHandle(int i);

    private native DvrPlayback nativeOpenDvrPlayback(long j);

    private native DvrRecorder nativeOpenDvrRecorder(long j);

    private native Filter nativeOpenFilter(int i, int i2, long j);

    private native Frontend nativeOpenFrontendByHandle(int i);

    private native Lnb nativeOpenLnbByHandle(int i);

    private native Lnb nativeOpenLnbByName(String str);

    private static native SharedFilter nativeOpenSharedFilter(String str);

    private native TimeFilter nativeOpenTimeFilter();

    private native void nativeRegisterFeCbListener(long j);

    private native int nativeRemoveOutputPid(int i);

    private native int nativeScan(int i, FrontendSettings frontendSettings, int i2);

    private native int nativeSetLna(boolean z);

    private native int nativeSetLnb(Lnb lnb);

    private native int nativeSetMaxNumberOfFrontends(int i, int i2);

    private native void nativeSetup();

    private native int nativeShareFrontend(int i);

    private native int nativeStopScan();

    private native int nativeStopTune();

    private native int nativeTune(int i, FrontendSettings frontendSettings);

    private native int nativeUnlinkCiCam(int i);

    private native void nativeUnregisterFeCbListener(long j);

    private native int nativeUnshareFrontend();

    private native void nativeUpdateFrontend(long j);

    static {
        try {
            System.loadLibrary("media_tv_tuner");
            nativeInit();
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, "tuner JNI library not found!");
        }
        sTunerVersion = 0;
    }

    public Tuner(Context context, String tvInputSessionId, int useCase) {
        TunerResourceManager.ResourcesReclaimListener resourcesReclaimListener = new TunerResourceManager.ResourcesReclaimListener() { // from class: android.media.tv.tuner.Tuner.1
            @Override // android.media.tv.tunerresourcemanager.TunerResourceManager.ResourcesReclaimListener
            public void onReclaimResources() {
                if (Tuner.this.mFrontend != null) {
                    FrameworkStatsLog.write(276, Tuner.this.mUserId, 0);
                }
                Tuner.this.releaseAll();
                Tuner.this.mHandler.sendMessage(Tuner.this.mHandler.obtainMessage(1));
            }
        };
        this.mResourceListener = resourcesReclaimListener;
        nativeSetup();
        int nativeGetTunerVersion = nativeGetTunerVersion();
        sTunerVersion = nativeGetTunerVersion;
        if (nativeGetTunerVersion == 0) {
            Log.e(TAG, "Unknown Tuner version!");
        } else {
            Log.d(TAG, "Current Tuner version is " + TunerVersionChecker.getMajorVersion(sTunerVersion) + MediaMetrics.SEPARATOR + TunerVersionChecker.getMinorVersion(sTunerVersion) + MediaMetrics.SEPARATOR);
        }
        this.mContext = context;
        TunerResourceManager tunerResourceManager = (TunerResourceManager) context.getSystemService(Context.TV_TUNER_RESOURCE_MGR_SERVICE);
        this.mTunerResourceManager = tunerResourceManager;
        if (this.mHandler == null) {
            this.mHandler = createEventHandler();
        }
        int[] clientId = new int[1];
        ResourceClientProfile profile = new ResourceClientProfile();
        profile.tvInputSessionId = tvInputSessionId;
        profile.useCase = useCase;
        tunerResourceManager.registerClientProfile(profile, new PendingIntent$$ExternalSyntheticLambda1(), resourcesReclaimListener, clientId);
        this.mClientId = clientId[0];
        this.mUserId = Process.myUid();
    }

    private FrontendInfo[] getFrontendInfoListInternal() {
        List<Integer> ids = getFrontendIds();
        if (ids == null) {
            return null;
        }
        FrontendInfo[] infos = new FrontendInfo[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i).intValue();
            FrontendInfo frontendInfo = getFrontendInfoById(id);
            if (frontendInfo == null) {
                Log.e(TAG, "Failed to get a FrontendInfo on frontend id:" + id + "!");
            } else {
                infos[i] = frontendInfo;
            }
        }
        return (FrontendInfo[]) Arrays.stream(infos).filter(new Predicate() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda9
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Objects.nonNull((FrontendInfo) obj);
            }
        }).toArray(new IntFunction() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda10
            @Override // java.util.function.IntFunction
            public final Object apply(int i2) {
                return Tuner.lambda$getFrontendInfoListInternal$0(i2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ FrontendInfo[] lambda$getFrontendInfoListInternal$0(int x$0) {
        return new FrontendInfo[x$0];
    }

    public static int getTunerVersion() {
        return sTunerVersion;
    }

    public List<Integer> getFrontendIds() {
        this.mFrontendLock.lock();
        try {
            return nativeGetFrontendIds();
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    public void setResourceLostListener(Executor executor, OnResourceLostListener listener) {
        synchronized (this.mOnResourceLostListenerLock) {
            Objects.requireNonNull(executor, "OnResourceLostListener must not be null");
            Objects.requireNonNull(listener, "executor must not be null");
            this.mOnResourceLostListener = listener;
            this.mOnResourceLostListenerExecutor = executor;
        }
    }

    public void clearResourceLostListener() {
        synchronized (this.mOnResourceLostListenerLock) {
            this.mOnResourceLostListener = null;
            this.mOnResourceLostListenerExecutor = null;
        }
    }

    public void shareFrontendFromTuner(Tuner tuner) {
        acquireTRMSLock("shareFrontendFromTuner()");
        this.mFrontendLock.lock();
        try {
            this.mTunerResourceManager.shareFrontend(this.mClientId, tuner.mClientId);
            this.mFeOwnerTuner = tuner;
            tuner.registerFrontendCallbackListener(this);
            Tuner tuner2 = this.mFeOwnerTuner;
            this.mFrontendHandle = tuner2.mFrontendHandle;
            Frontend frontend = tuner2.mFrontend;
            this.mFrontend = frontend;
            nativeShareFrontend(frontend.mId);
        } finally {
            releaseTRMSLock();
            this.mFrontendLock.unlock();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [491=6, 492=6, 493=6, 494=6] */
    public int transferOwner(Tuner newOwner) {
        acquireTRMSLock("transferOwner()");
        this.mFrontendLock.lock();
        this.mFrontendCiCamLock.lock();
        this.mLnbLock.lock();
        try {
            if (isFrontendOwner() && isNewOwnerQualifiedForTransfer(newOwner)) {
                int res = transferFeOwner(newOwner);
                if (res != 0) {
                    return res;
                }
                int res2 = transferCiCamOwner(newOwner);
                if (res2 != 0) {
                    return res2;
                }
                int res3 = transferLnbOwner(newOwner);
                if (res3 != 0) {
                    return res3;
                }
                this.mFrontendLock.unlock();
                this.mFrontendCiCamLock.unlock();
                this.mLnbLock.unlock();
                releaseTRMSLock();
                return 0;
            }
            return 3;
        } finally {
            this.mFrontendLock.unlock();
            this.mFrontendCiCamLock.unlock();
            this.mLnbLock.unlock();
            releaseTRMSLock();
        }
    }

    private void replicateFrontendSettings(Tuner src) {
        this.mFrontendLock.lock();
        try {
            if (src == null) {
                if (DEBUG) {
                    Log.d(TAG, "resetting Frontend params for " + this.mClientId);
                }
                this.mFrontend = null;
                this.mFrontendHandle = null;
                this.mFrontendInfo = null;
                this.mFrontendType = 0;
            } else {
                if (DEBUG) {
                    Log.d(TAG, "copying Frontend params from " + src.mClientId + " to " + this.mClientId);
                }
                this.mFrontend = src.mFrontend;
                this.mFrontendHandle = src.mFrontendHandle;
                this.mFrontendInfo = src.mFrontendInfo;
                this.mFrontendType = src.mFrontendType;
            }
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    private void setFrontendOwner(Tuner owner) {
        this.mFrontendLock.lock();
        try {
            this.mFeOwnerTuner = owner;
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    private void replicateCiCamSettings(Tuner src) {
        this.mFrontendCiCamLock.lock();
        try {
            if (src == null) {
                if (DEBUG) {
                    Log.d(TAG, "resetting CiCamParams: " + this.mClientId);
                }
                this.mFrontendCiCamHandle = null;
                this.mFrontendCiCamId = null;
            } else {
                if (DEBUG) {
                    Log.d(TAG, "copying CiCamParams from " + src.mClientId + " to " + this.mClientId);
                    Log.d(TAG, "mFrontendCiCamHandle:" + src.mFrontendCiCamHandle + ", mFrontendCiCamId:" + src.mFrontendCiCamId);
                }
                this.mFrontendCiCamHandle = src.mFrontendCiCamHandle;
                this.mFrontendCiCamId = src.mFrontendCiCamId;
            }
        } finally {
            this.mFrontendCiCamLock.unlock();
        }
    }

    private void replicateLnbSettings(Tuner src) {
        this.mLnbLock.lock();
        try {
            if (src == null) {
                if (DEBUG) {
                    Log.d(TAG, "resetting Lnb params");
                }
                this.mLnb = null;
                this.mLnbHandle = null;
            } else {
                if (DEBUG) {
                    Log.d(TAG, "copying Lnb params from " + src.mClientId + " to " + this.mClientId);
                }
                this.mLnb = src.mLnb;
                this.mLnbHandle = src.mLnbHandle;
            }
        } finally {
            this.mLnbLock.unlock();
        }
    }

    private boolean isFrontendOwner() {
        boolean notAnOwner = this.mFeOwnerTuner != null;
        if (notAnOwner) {
            Log.e(TAG, "transferOwner() - cannot be called on the non-owner");
            return false;
        }
        return true;
    }

    private boolean isNewOwnerQualifiedForTransfer(Tuner newOwner) {
        boolean newOwnerIsTheCurrentSharee = newOwner.mFeOwnerTuner == this && newOwner.mFrontendHandle.equals(this.mFrontendHandle);
        if (!newOwnerIsTheCurrentSharee) {
            Log.e(TAG, "transferOwner() - new owner must be the current sharee");
            return false;
        }
        boolean newOwnerAlreadyHoldsToBeSharedResource = (newOwner.mFrontendCiCamHandle == null && newOwner.mLnb == null) ? false : true;
        if (newOwnerAlreadyHoldsToBeSharedResource) {
            Log.e(TAG, "transferOwner() - new owner cannot be holding CiCam nor Lnb resource");
            return false;
        }
        return true;
    }

    private int transferFeOwner(Tuner newOwner) {
        newOwner.nativeUpdateFrontend(getNativeContext());
        nativeUpdateFrontend(0L);
        newOwner.replicateFrontendSettings(this);
        setFrontendOwner(newOwner);
        newOwner.setFrontendOwner(null);
        return this.mTunerResourceManager.transferOwner(0, this.mClientId, newOwner.mClientId) ? 0 : 6;
    }

    private int transferCiCamOwner(Tuner newOwner) {
        boolean notAnOwner = this.mFrontendCiCamHandle == null;
        if (notAnOwner) {
            return 0;
        }
        newOwner.replicateCiCamSettings(this);
        replicateCiCamSettings(null);
        return this.mTunerResourceManager.transferOwner(5, this.mClientId, newOwner.mClientId) ? 0 : 6;
    }

    private int transferLnbOwner(Tuner newOwner) {
        Lnb lnb = this.mLnb;
        boolean notAnOwner = lnb == null;
        if (notAnOwner) {
            return 0;
        }
        lnb.setOwner(newOwner);
        newOwner.replicateLnbSettings(this);
        replicateLnbSettings(null);
        return this.mTunerResourceManager.transferOwner(3, this.mClientId, newOwner.mClientId) ? 0 : 6;
    }

    public void updateResourcePriority(int priority, int niceValue) {
        this.mTunerResourceManager.updateClientPriority(this.mClientId, priority, niceValue);
    }

    public boolean hasUnusedFrontend(int frontendType) {
        return this.mTunerResourceManager.hasUnusedFrontend(frontendType);
    }

    public boolean isLowestPriority(int frontendType) {
        return this.mTunerResourceManager.isLowestPriority(this.mClientId, frontendType);
    }

    private void registerFrontendCallbackListener(Tuner tuner) {
        nativeRegisterFeCbListener(tuner.getNativeContext());
    }

    private void unregisterFrontendCallbackListener(Tuner tuner) {
        nativeUnregisterFeCbListener(tuner.getNativeContext());
    }

    long getNativeContext() {
        return this.mNativeContext;
    }

    @Override // java.lang.AutoCloseable
    public void close() {
        acquireTRMSLock("close()");
        try {
            releaseAll();
            TunerUtils.throwExceptionForResult(nativeClose(), "failed to close tuner");
        } finally {
            releaseTRMSLock();
        }
    }

    public void closeFrontend() {
        acquireTRMSLock("closeFrontend()");
        try {
            releaseFrontend();
        } finally {
            releaseTRMSLock();
        }
    }

    private void releaseFrontend() {
        boolean z = DEBUG;
        if (z) {
            Log.d(TAG, "Tuner#releaseFrontend");
        }
        this.mFrontendLock.lock();
        try {
            if (this.mFrontendHandle != null) {
                if (z) {
                    Log.d(TAG, "mFrontendHandle not null");
                }
                if (this.mFeOwnerTuner != null) {
                    if (z) {
                        Log.d(TAG, "mFeOwnerTuner not null - sharee");
                    }
                    this.mFeOwnerTuner.unregisterFrontendCallbackListener(this);
                    this.mFeOwnerTuner = null;
                    nativeUnshareFrontend();
                } else {
                    if (z) {
                        Log.d(TAG, "mFeOwnerTuner null - owner");
                    }
                    int res = nativeCloseFrontend(this.mFrontendHandle.intValue());
                    if (res != 0) {
                        TunerUtils.throwExceptionForResult(res, "failed to close frontend");
                    }
                }
                if (z) {
                    Log.d(TAG, "call TRM#releaseFrontend :" + this.mFrontendHandle + ", " + this.mClientId);
                }
                this.mTunerResourceManager.releaseFrontend(this.mFrontendHandle.intValue(), this.mClientId);
                FrameworkStatsLog.write(276, this.mUserId, 0);
                replicateFrontendSettings(null);
            }
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    private void releaseCiCam() {
        this.mFrontendCiCamLock.lock();
        try {
            if (this.mFrontendCiCamHandle != null) {
                if (DEBUG) {
                    Log.d(TAG, "unlinking CiCam : " + this.mFrontendCiCamHandle + " for " + this.mClientId);
                }
                int result = nativeUnlinkCiCam(this.mFrontendCiCamId.intValue());
                if (result == 0) {
                    this.mTunerResourceManager.releaseCiCam(this.mFrontendCiCamHandle.intValue(), this.mClientId);
                    replicateCiCamSettings(null);
                } else {
                    Log.e(TAG, "nativeUnlinkCiCam(" + this.mFrontendCiCamHandle + ") for mClientId:" + this.mClientId + "failed with result:" + result);
                }
            } else if (DEBUG) {
                Log.d(TAG, "NOT unlinking CiCam : " + this.mClientId);
            }
        } finally {
            this.mFrontendCiCamLock.unlock();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseAll() {
        releaseCiCam();
        releaseFrontend();
        this.mLnbLock.lock();
        try {
            if (this.mLnb != null) {
                if (DEBUG) {
                    Log.d(TAG, "calling mLnb.close() : " + this.mClientId);
                }
                this.mLnb.close();
            } else if (DEBUG) {
                Log.d(TAG, "NOT calling mLnb.close() : " + this.mClientId);
            }
            this.mLnbLock.unlock();
            synchronized (this.mDescramblers) {
                if (!this.mDescramblers.isEmpty()) {
                    for (Map.Entry<Integer, WeakReference<Descrambler>> d : this.mDescramblers.entrySet()) {
                        Descrambler descrambler = d.getValue().get();
                        if (descrambler != null) {
                            descrambler.close();
                        }
                        this.mTunerResourceManager.releaseDescrambler(d.getKey().intValue(), this.mClientId);
                    }
                    this.mDescramblers.clear();
                }
            }
            synchronized (this.mFilters) {
                if (!this.mFilters.isEmpty()) {
                    for (WeakReference<Filter> weakFilter : this.mFilters) {
                        Filter filter = weakFilter.get();
                        if (filter != null) {
                            filter.close();
                        }
                    }
                    this.mFilters.clear();
                }
            }
            this.mDemuxLock.lock();
            try {
                Integer num = this.mDemuxHandle;
                if (num != null) {
                    int res = nativeCloseDemux(num.intValue());
                    if (res != 0) {
                        TunerUtils.throwExceptionForResult(res, "failed to close demux");
                    }
                    this.mTunerResourceManager.releaseDemux(this.mDemuxHandle.intValue(), this.mClientId);
                    this.mDemuxHandle = null;
                }
                this.mDemuxLock.unlock();
                this.mTunerResourceManager.unregisterClientProfile(this.mClientId);
            } catch (Throwable th) {
                this.mDemuxLock.unlock();
                throw th;
            }
        } catch (Throwable th2) {
            this.mLnbLock.unlock();
            throw th2;
        }
    }

    private EventHandler createEventHandler() {
        Looper looper = Looper.myLooper();
        if (looper != null) {
            return new EventHandler(looper);
        }
        Looper looper2 = Looper.getMainLooper();
        if (looper2 != null) {
            return new EventHandler(looper2);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class EventHandler extends Handler {
        private EventHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (Tuner.this.mOnResourceLostListenerLock) {
                        if (Tuner.this.mOnResourceLostListener != null && Tuner.this.mOnResourceLostListenerExecutor != null) {
                            Tuner.this.mOnResourceLostListenerExecutor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$EventHandler$$ExternalSyntheticLambda0
                                @Override // java.lang.Runnable
                                public final void run() {
                                    Tuner.EventHandler.this.m2671lambda$handleMessage$0$androidmediatvtunerTuner$EventHandler();
                                }
                            });
                        }
                    }
                    return;
                case 2:
                default:
                    return;
                case 3:
                    Filter filter = (Filter) msg.obj;
                    if (filter.getCallback() != null) {
                        filter.getCallback().onFilterStatusChanged(filter, msg.arg1);
                        return;
                    }
                    return;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$handleMessage$0$android-media-tv-tuner-Tuner$EventHandler  reason: not valid java name */
        public /* synthetic */ void m2671lambda$handleMessage$0$androidmediatvtunerTuner$EventHandler() {
            synchronized (Tuner.this.mOnResourceLostListenerLock) {
                if (Tuner.this.mOnResourceLostListener != null) {
                    Tuner.this.mOnResourceLostListener.onResourceLost(Tuner.this);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Frontend {
        private int mId;

        private Frontend(int id) {
            this.mId = id;
        }
    }

    public void setOnTuneEventListener(Executor executor, OnTuneEventListener eventListener) {
        synchronized (this.mOnTuneEventLock) {
            this.mOnTuneEventListener = eventListener;
            this.mOnTuneEventExecutor = executor;
        }
    }

    public void clearOnTuneEventListener() {
        synchronized (this.mOnTuneEventLock) {
            this.mOnTuneEventListener = null;
            this.mOnTuneEventExecutor = null;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1191=5] */
    public int tune(FrontendSettings settings) {
        this.mFrontendLock.lock();
        try {
            int type = settings.getType();
            if (this.mFrontendHandle != null && type != this.mFrontendType) {
                Log.e(TAG, "Frontend was opened with type " + this.mFrontendType + ", new type is " + type);
                return 3;
            }
            Log.d(TAG, "Tune to " + settings.getFrequencyLong());
            this.mFrontendType = type;
            if (type != 10 || TunerVersionChecker.checkHigherOrEqualVersionTo(65537, "Tuner with DTMB Frontend")) {
                if (checkResource(0, this.mFrontendLock)) {
                    this.mFrontendInfo = null;
                    Log.d(TAG, "Write Stats Log for tuning.");
                    FrameworkStatsLog.write(276, this.mUserId, 1);
                    int res = nativeTune(settings.getType(), settings);
                    return res;
                }
                return 1;
            }
            return 1;
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    public int cancelTuning() {
        this.mFrontendLock.lock();
        try {
            return nativeStopTune();
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1271=4] */
    public int scan(FrontendSettings settings, int scanType, Executor executor, ScanCallback scanCallback) {
        Executor executor2;
        this.mFrontendLock.lock();
        try {
            synchronized (this.mScanCallbackLock) {
                ScanCallback scanCallback2 = this.mScanCallback;
                if ((scanCallback2 != null && scanCallback2 != scanCallback) || ((executor2 = this.mScanCallbackExecutor) != null && executor2 != executor)) {
                    throw new IllegalStateException("Different Scan session already in progress.  stopScan must be called before a new scan session can be started.");
                }
                int type = settings.getType();
                this.mFrontendType = type;
                if (type != 10 || TunerVersionChecker.checkHigherOrEqualVersionTo(65537, "Scan with DTMB Frontend")) {
                    if (checkResource(0, this.mFrontendLock)) {
                        this.mScanCallback = scanCallback;
                        this.mScanCallbackExecutor = executor;
                        this.mFrontendInfo = null;
                        FrameworkStatsLog.write(276, this.mUserId, 5);
                        return nativeScan(settings.getType(), settings, scanType);
                    }
                    return 1;
                }
                return 1;
            }
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    public int cancelScanning() {
        int retVal;
        this.mFrontendLock.lock();
        try {
            synchronized (this.mScanCallbackLock) {
                FrameworkStatsLog.write(276, this.mUserId, 6);
                retVal = nativeStopScan();
                this.mScanCallback = null;
                this.mScanCallbackExecutor = null;
            }
            return retVal;
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    private boolean requestFrontend() {
        Lnb lnb;
        int[] feHandle = new int[1];
        TunerFrontendRequest request = new TunerFrontendRequest();
        request.clientId = this.mClientId;
        request.frontendType = this.mFrontendType;
        boolean granted = this.mTunerResourceManager.requestFrontend(request, feHandle);
        if (granted) {
            Integer valueOf = Integer.valueOf(feHandle[0]);
            this.mFrontendHandle = valueOf;
            this.mFrontend = nativeOpenFrontendByHandle(valueOf.intValue());
        }
        int i = this.mFrontendType;
        if (i == 5 || i == 7 || i == 8) {
            this.mLnbLock.lock();
            try {
                if (this.mLnbHandle != null && (lnb = this.mLnb) != null) {
                    nativeSetLnb(lnb);
                }
            } finally {
                this.mLnbLock.unlock();
            }
        }
        return granted;
    }

    private int setLnb(Lnb lnb) {
        this.mLnbLock.lock();
        try {
            return nativeSetLnb(lnb);
        } finally {
            this.mLnbLock.unlock();
        }
    }

    public int setLnaEnabled(boolean enable) {
        return nativeSetLna(enable);
    }

    public FrontendStatus getFrontendStatus(int[] statusTypes) {
        this.mFrontendLock.lock();
        try {
            if (this.mFrontend == null) {
                throw new IllegalStateException("frontend is not initialized");
            }
            return nativeGetFrontendStatus(statusTypes);
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    public int getAvSyncHwId(Filter filter) {
        this.mDemuxLock.lock();
        try {
            int i = -1;
            if (checkResource(1, this.mDemuxLock)) {
                Integer id = nativeGetAvSyncHwId(filter);
                if (id != null) {
                    i = id.intValue();
                }
                return i;
            }
            return -1;
        } finally {
            this.mDemuxLock.unlock();
        }
    }

    public long getAvSyncTime(int avSyncHwId) {
        this.mDemuxLock.lock();
        try {
            long j = -1;
            if (checkResource(1, this.mDemuxLock)) {
                Long time = nativeGetAvSyncTime(avSyncHwId);
                if (time != null) {
                    j = time.longValue();
                }
                return j;
            }
            return -1L;
        } finally {
            this.mDemuxLock.unlock();
        }
    }

    public int connectCiCam(int ciCamId) {
        this.mDemuxLock.lock();
        try {
            if (checkResource(1, this.mDemuxLock)) {
                return nativeConnectCiCam(ciCamId);
            }
            return 1;
        } finally {
            this.mDemuxLock.unlock();
        }
    }

    public int connectFrontendToCiCam(int ciCamId) {
        acquireTRMSLock("connectFrontendToCiCam()");
        this.mFrontendCiCamLock.lock();
        this.mFrontendLock.lock();
        try {
            if (TunerVersionChecker.checkHigherOrEqualVersionTo(65537, "linkFrontendToCiCam")) {
                this.mRequestedCiCamId = ciCamId;
                if (checkResource(5, null) && checkResource(0, null)) {
                    return nativeLinkCiCam(ciCamId);
                }
            }
            return -1;
        } finally {
            releaseTRMSLock();
            this.mFrontendCiCamLock.unlock();
            this.mFrontendLock.unlock();
        }
    }

    public int disconnectCiCam() {
        this.mDemuxLock.lock();
        try {
            if (this.mDemuxHandle != null) {
                return nativeDisconnectCiCam();
            }
            return 1;
        } finally {
            this.mDemuxLock.unlock();
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IGET, INVOKE]}, finally: {[IGET, INVOKE, INVOKE, IGET, INVOKE, INVOKE, IF] complete} */
    public int disconnectFrontendToCiCam(int ciCamId) {
        Integer num;
        acquireTRMSLock("disconnectFrontendToCiCam()");
        try {
            if (TunerVersionChecker.checkHigherOrEqualVersionTo(65537, "unlinkFrontendToCiCam")) {
                this.mFrontendCiCamLock.lock();
                if (this.mFrontendCiCamHandle != null && (num = this.mFrontendCiCamId) != null && num.intValue() == ciCamId) {
                    int result = nativeUnlinkCiCam(ciCamId);
                    if (result == 0) {
                        this.mTunerResourceManager.releaseCiCam(this.mFrontendCiCamHandle.intValue(), this.mClientId);
                        this.mFrontendCiCamId = null;
                        this.mFrontendCiCamHandle = null;
                    }
                    return result;
                }
            }
            if (this.mFrontendCiCamLock.isLocked()) {
                this.mFrontendCiCamLock.unlock();
            }
            releaseTRMSLock();
            return 1;
        } finally {
            if (this.mFrontendCiCamLock.isLocked()) {
                this.mFrontendCiCamLock.unlock();
            }
            releaseTRMSLock();
        }
    }

    public int removeOutputPid(int pid) {
        this.mFrontendLock.lock();
        try {
            if (TunerVersionChecker.checkHigherOrEqualVersionTo(131072, "Remove output PID")) {
                if (this.mFrontend == null) {
                    throw new IllegalStateException("frontend is not initialized");
                }
                return nativeRemoveOutputPid(pid);
            }
            return 1;
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1630=4] */
    public List<FrontendStatusReadiness> getFrontendStatusReadiness(int[] statusTypes) {
        this.mFrontendLock.lock();
        try {
            if (TunerVersionChecker.checkHigherOrEqualVersionTo(131072, "Get fronted status readiness")) {
                if (this.mFrontend != null) {
                    FrontendStatusReadiness[] readiness = nativeGetFrontendStatusReadiness(statusTypes);
                    return readiness == null ? Collections.EMPTY_LIST : Arrays.asList(readiness);
                }
                throw new IllegalStateException("frontend is not initialized");
            }
            return Collections.EMPTY_LIST;
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    public FrontendInfo getFrontendInfo() {
        this.mFrontendLock.lock();
        try {
            if (checkResource(0, this.mFrontendLock)) {
                Frontend frontend = this.mFrontend;
                if (frontend == null) {
                    throw new IllegalStateException("frontend is not initialized");
                }
                if (this.mFrontendInfo == null) {
                    this.mFrontendInfo = getFrontendInfoById(frontend.mId);
                }
                return this.mFrontendInfo;
            }
            return null;
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    public List<FrontendInfo> getAvailableFrontendInfos() {
        FrontendInfo[] feInfoList = getFrontendInfoListInternal();
        if (feInfoList == null) {
            return null;
        }
        return Arrays.asList(feInfoList);
    }

    public String getCurrentFrontendHardwareInfo() {
        this.mFrontendLock.lock();
        try {
            if (TunerVersionChecker.checkHigherOrEqualVersionTo(131072, "Get Frontend hardware info")) {
                if (this.mFrontend == null) {
                    throw new IllegalStateException("frontend is not initialized");
                }
                return nativeGetFrontendHardwareInfo();
            }
            return null;
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    public int setMaxNumberOfFrontends(int frontendType, int maxNumber) {
        if (!TunerVersionChecker.checkHigherOrEqualVersionTo(131072, "Set maximum Frontends")) {
            return 1;
        }
        if (maxNumber < 0) {
            return 4;
        }
        int res = nativeSetMaxNumberOfFrontends(frontendType, maxNumber);
        if (res == 0 && !this.mTunerResourceManager.setMaxNumberOfFrontends(frontendType, maxNumber)) {
            return 4;
        }
        return res;
    }

    public int getMaxNumberOfFrontends(int frontendType) {
        if (!TunerVersionChecker.checkHigherOrEqualVersionTo(131072, "Set maximum Frontends")) {
            return -1;
        }
        int maxNumFromHAL = nativeGetMaxNumberOfFrontends(frontendType);
        int maxNumFromTRM = this.mTunerResourceManager.getMaxNumberOfFrontends(frontendType);
        if (maxNumFromHAL != maxNumFromTRM) {
            Log.w(TAG, "max num of usable frontend is out-of-sync b/w " + maxNumFromHAL + " != " + maxNumFromTRM);
        }
        return maxNumFromHAL;
    }

    public FrontendInfo getFrontendInfoById(int id) {
        this.mFrontendLock.lock();
        try {
            return nativeGetFrontendInfo(id);
        } finally {
            this.mFrontendLock.unlock();
        }
    }

    public DemuxCapabilities getDemuxCapabilities() {
        this.mDemuxLock.lock();
        try {
            return nativeGetDemuxCapabilities();
        } finally {
            this.mDemuxLock.unlock();
        }
    }

    private void onFrontendEvent(final int eventType) {
        Log.d(TAG, "Got event from tuning. Event type: " + eventType + " for " + this);
        synchronized (this.mOnTuneEventLock) {
            Executor executor = this.mOnTuneEventExecutor;
            if (executor != null && this.mOnTuneEventListener != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda7
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2658lambda$onFrontendEvent$1$androidmediatvtunerTuner(eventType);
                    }
                });
            }
        }
        Log.d(TAG, "Wrote Stats Log for the events from tuning.");
        if (eventType == 0) {
            FrameworkStatsLog.write(276, this.mUserId, 2);
        } else if (eventType == 1) {
            FrameworkStatsLog.write(276, this.mUserId, 3);
        } else if (eventType == 2) {
            FrameworkStatsLog.write(276, this.mUserId, 4);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onFrontendEvent$1$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2658lambda$onFrontendEvent$1$androidmediatvtunerTuner(int eventType) {
        synchronized (this.mOnTuneEventLock) {
            OnTuneEventListener onTuneEventListener = this.mOnTuneEventListener;
            if (onTuneEventListener != null) {
                onTuneEventListener.onTuneEvent(eventType);
            }
        }
    }

    private void onLocked() {
        Log.d(TAG, "Wrote Stats Log for locked event from scanning.");
        FrameworkStatsLog.write(276, this.mUserId, 2);
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda14
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2662lambda$onLocked$2$androidmediatvtunerTuner();
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onLocked$2$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2662lambda$onLocked$2$androidmediatvtunerTuner() {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onLocked();
            }
        }
    }

    private void onUnlocked() {
        Log.d(TAG, "Wrote Stats Log for unlocked event from scanning.");
        FrameworkStatsLog.write(276, this.mUserId, 2);
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda15
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2670lambda$onUnlocked$3$androidmediatvtunerTuner();
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUnlocked$3$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2670lambda$onUnlocked$3$androidmediatvtunerTuner() {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onUnlocked();
            }
        }
    }

    private void onScanStopped() {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2667lambda$onScanStopped$4$androidmediatvtunerTuner();
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onScanStopped$4$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2667lambda$onScanStopped$4$androidmediatvtunerTuner() {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onScanStopped();
            }
        }
    }

    private void onProgress(final int percent) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda12
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2666lambda$onProgress$5$androidmediatvtunerTuner(percent);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onProgress$5$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2666lambda$onProgress$5$androidmediatvtunerTuner(int percent) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onProgress(percent);
            }
        }
    }

    private void onFrequenciesReport(final long[] frequencies) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2657lambda$onFrequenciesReport$6$androidmediatvtunerTuner(frequencies);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onFrequenciesReport$6$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2657lambda$onFrequenciesReport$6$androidmediatvtunerTuner(long[] frequencies) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onFrequenciesLongReported(frequencies);
            }
        }
    }

    private void onSymbolRates(final int[] rate) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda11
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2669lambda$onSymbolRates$7$androidmediatvtunerTuner(rate);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSymbolRates$7$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2669lambda$onSymbolRates$7$androidmediatvtunerTuner(int[] rate) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onSymbolRatesReported(rate);
            }
        }
    }

    private void onHierarchy(final int hierarchy) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda16
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2660lambda$onHierarchy$8$androidmediatvtunerTuner(hierarchy);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onHierarchy$8$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2660lambda$onHierarchy$8$androidmediatvtunerTuner(int hierarchy) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onHierarchyReported(hierarchy);
            }
        }
    }

    private void onSignalType(final int signalType) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda8
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2668lambda$onSignalType$9$androidmediatvtunerTuner(signalType);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSignalType$9$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2668lambda$onSignalType$9$androidmediatvtunerTuner(int signalType) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onSignalTypeReported(signalType);
            }
        }
    }

    private void onPlpIds(final int[] plpIds) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2664lambda$onPlpIds$10$androidmediatvtunerTuner(plpIds);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onPlpIds$10$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2664lambda$onPlpIds$10$androidmediatvtunerTuner(int[] plpIds) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onPlpIdsReported(plpIds);
            }
        }
    }

    private void onGroupIds(final int[] groupIds) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda21
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2659lambda$onGroupIds$11$androidmediatvtunerTuner(groupIds);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onGroupIds$11$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2659lambda$onGroupIds$11$androidmediatvtunerTuner(int[] groupIds) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onGroupIdsReported(groupIds);
            }
        }
    }

    private void onInputStreamIds(final int[] inputStreamIds) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda19
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2661lambda$onInputStreamIds$12$androidmediatvtunerTuner(inputStreamIds);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onInputStreamIds$12$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2661lambda$onInputStreamIds$12$androidmediatvtunerTuner(int[] inputStreamIds) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onInputStreamIdsReported(inputStreamIds);
            }
        }
    }

    private void onDvbsStandard(final int dvbsStandandard) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda18
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2654lambda$onDvbsStandard$13$androidmediatvtunerTuner(dvbsStandandard);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onDvbsStandard$13$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2654lambda$onDvbsStandard$13$androidmediatvtunerTuner(int dvbsStandandard) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onDvbsStandardReported(dvbsStandandard);
            }
        }
    }

    private void onDvbtStandard(final int dvbtStandard) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2656lambda$onDvbtStandard$14$androidmediatvtunerTuner(dvbtStandard);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onDvbtStandard$14$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2656lambda$onDvbtStandard$14$androidmediatvtunerTuner(int dvbtStandard) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onDvbtStandardReported(dvbtStandard);
            }
        }
    }

    private void onAnalogSifStandard(final int sif) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda13
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2651lambda$onAnalogSifStandard$15$androidmediatvtunerTuner(sif);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onAnalogSifStandard$15$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2651lambda$onAnalogSifStandard$15$androidmediatvtunerTuner(int sif) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onAnalogSifStandardReported(sif);
            }
        }
    }

    private void onAtsc3PlpInfos(final Atsc3PlpInfo[] atsc3PlpInfos) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2652lambda$onAtsc3PlpInfos$16$androidmediatvtunerTuner(atsc3PlpInfos);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onAtsc3PlpInfos$16$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2652lambda$onAtsc3PlpInfos$16$androidmediatvtunerTuner(Atsc3PlpInfo[] atsc3PlpInfos) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onAtsc3PlpInfosReported(atsc3PlpInfos);
            }
        }
    }

    private void onModulationReported(final int modulation) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda20
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2663lambda$onModulationReported$17$androidmediatvtunerTuner(modulation);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onModulationReported$17$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2663lambda$onModulationReported$17$androidmediatvtunerTuner(int modulation) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onModulationReported(modulation);
            }
        }
    }

    private void onPriorityReported(final boolean isHighPriority) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2665lambda$onPriorityReported$18$androidmediatvtunerTuner(isHighPriority);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onPriorityReported$18$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2665lambda$onPriorityReported$18$androidmediatvtunerTuner(boolean isHighPriority) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onPriorityReported(isHighPriority);
            }
        }
    }

    private void onDvbcAnnexReported(final int dvbcAnnex) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2653lambda$onDvbcAnnexReported$19$androidmediatvtunerTuner(dvbcAnnex);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onDvbcAnnexReported$19$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2653lambda$onDvbcAnnexReported$19$androidmediatvtunerTuner(int dvbcAnnex) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onDvbcAnnexReported(dvbcAnnex);
            }
        }
    }

    private void onDvbtCellIdsReported(final int[] dvbtCellIds) {
        synchronized (this.mScanCallbackLock) {
            Executor executor = this.mScanCallbackExecutor;
            if (executor != null && this.mScanCallback != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.Tuner$$ExternalSyntheticLambda17
                    @Override // java.lang.Runnable
                    public final void run() {
                        Tuner.this.m2655lambda$onDvbtCellIdsReported$20$androidmediatvtunerTuner(dvbtCellIds);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onDvbtCellIdsReported$20$android-media-tv-tuner-Tuner  reason: not valid java name */
    public /* synthetic */ void m2655lambda$onDvbtCellIdsReported$20$androidmediatvtunerTuner(int[] dvbtCellIds) {
        synchronized (this.mScanCallbackLock) {
            ScanCallback scanCallback = this.mScanCallback;
            if (scanCallback != null) {
                scanCallback.onDvbtCellIdsReported(dvbtCellIds);
            }
        }
    }

    public Filter openFilter(int mainType, int subType, long bufferSize, Executor executor, FilterCallback cb) {
        this.mDemuxLock.lock();
        try {
            if (checkResource(1, this.mDemuxLock)) {
                Filter filter = nativeOpenFilter(mainType, TunerUtils.getFilterSubtype(mainType, subType), bufferSize);
                if (filter != null) {
                    filter.setType(mainType, subType);
                    filter.setCallback(cb, executor);
                    if (this.mHandler == null) {
                        this.mHandler = createEventHandler();
                    }
                    synchronized (this.mFilters) {
                        WeakReference<Filter> weakFilter = new WeakReference<>(filter);
                        this.mFilters.add(weakFilter);
                        if (this.mFilters.size() > 256) {
                            Iterator<WeakReference<Filter>> iterator = this.mFilters.iterator();
                            while (iterator.hasNext()) {
                                WeakReference<Filter> wFilter = iterator.next();
                                if (wFilter.get() == null) {
                                    iterator.remove();
                                }
                            }
                        }
                    }
                }
                return filter;
            }
            return null;
        } finally {
            this.mDemuxLock.unlock();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2173=4] */
    public Lnb openLnb(Executor executor, LnbCallback cb) {
        Lnb lnb;
        this.mLnbLock.lock();
        try {
            Objects.requireNonNull(executor, "executor must not be null");
            Objects.requireNonNull(cb, "LnbCallback must not be null");
            Lnb lnb2 = this.mLnb;
            if (lnb2 != null) {
                lnb2.setCallbackAndOwner(this, executor, cb);
                return this.mLnb;
            } else if (!checkResource(3, this.mLnbLock) || (lnb = this.mLnb) == null) {
                return null;
            } else {
                lnb.setCallbackAndOwner(this, executor, cb);
                if (this.mFrontendHandle != null && this.mFrontend != null) {
                    setLnb(this.mLnb);
                }
                return this.mLnb;
            }
        } finally {
            this.mLnbLock.unlock();
        }
    }

    public Lnb openLnbByName(String name, Executor executor, LnbCallback cb) {
        this.mLnbLock.lock();
        try {
            Objects.requireNonNull(name, "LNB name must not be null");
            Objects.requireNonNull(executor, "executor must not be null");
            Objects.requireNonNull(cb, "LnbCallback must not be null");
            Lnb newLnb = nativeOpenLnbByName(name);
            if (newLnb != null) {
                Lnb lnb = this.mLnb;
                if (lnb != null) {
                    lnb.close();
                    this.mLnbHandle = null;
                }
                this.mLnb = newLnb;
                newLnb.setCallbackAndOwner(this, executor, cb);
                if (this.mFrontendHandle != null && this.mFrontend != null) {
                    setLnb(this.mLnb);
                }
            }
            return this.mLnb;
        } finally {
            this.mLnbLock.unlock();
        }
    }

    private boolean requestLnb() {
        int[] lnbHandle = new int[1];
        TunerLnbRequest request = new TunerLnbRequest();
        request.clientId = this.mClientId;
        boolean granted = this.mTunerResourceManager.requestLnb(request, lnbHandle);
        if (granted) {
            Integer valueOf = Integer.valueOf(lnbHandle[0]);
            this.mLnbHandle = valueOf;
            this.mLnb = nativeOpenLnbByHandle(valueOf.intValue());
        }
        return granted;
    }

    public TimeFilter openTimeFilter() {
        this.mDemuxLock.lock();
        try {
            if (checkResource(1, this.mDemuxLock)) {
                return nativeOpenTimeFilter();
            }
            return null;
        } finally {
            this.mDemuxLock.unlock();
        }
    }

    public Descrambler openDescrambler() {
        this.mDemuxLock.lock();
        try {
            if (checkResource(1, this.mDemuxLock)) {
                return requestDescrambler();
            }
            return null;
        } finally {
            this.mDemuxLock.unlock();
        }
    }

    public DvrRecorder openDvrRecorder(long bufferSize, Executor executor, OnRecordStatusChangedListener l) {
        this.mDemuxLock.lock();
        try {
            Objects.requireNonNull(executor, "executor must not be null");
            Objects.requireNonNull(l, "OnRecordStatusChangedListener must not be null");
            if (checkResource(1, this.mDemuxLock)) {
                DvrRecorder dvr = nativeOpenDvrRecorder(bufferSize);
                dvr.setListener(executor, l);
                return dvr;
            }
            return null;
        } finally {
            this.mDemuxLock.unlock();
        }
    }

    public DvrPlayback openDvrPlayback(long bufferSize, Executor executor, OnPlaybackStatusChangedListener l) {
        this.mDemuxLock.lock();
        try {
            Objects.requireNonNull(executor, "executor must not be null");
            Objects.requireNonNull(l, "OnPlaybackStatusChangedListener must not be null");
            if (checkResource(1, this.mDemuxLock)) {
                DvrPlayback dvr = nativeOpenDvrPlayback(bufferSize);
                dvr.setListener(executor, l);
                return dvr;
            }
            return null;
        } finally {
            this.mDemuxLock.unlock();
        }
    }

    public static SharedFilter openSharedFilter(Context context, String sharedFilterToken, Executor executor, SharedFilterCallback cb) {
        Objects.requireNonNull(sharedFilterToken, "sharedFilterToken must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(cb, "SharedFilterCallback must not be null");
        if (context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_TV_SHARED_FILTER) != 0) {
            throw new SecurityException("Caller must have ACCESS_TV_SHAREDFILTER permission.");
        }
        SharedFilter filter = nativeOpenSharedFilter(sharedFilterToken);
        if (filter != null) {
            filter.setCallback(cb, executor);
        }
        return filter;
    }

    private boolean requestDemux() {
        int[] demuxHandle = new int[1];
        TunerDemuxRequest request = new TunerDemuxRequest();
        request.clientId = this.mClientId;
        boolean granted = this.mTunerResourceManager.requestDemux(request, demuxHandle);
        if (granted) {
            Integer valueOf = Integer.valueOf(demuxHandle[0]);
            this.mDemuxHandle = valueOf;
            nativeOpenDemuxByhandle(valueOf.intValue());
        }
        return granted;
    }

    private Descrambler requestDescrambler() {
        int[] descramblerHandle = new int[1];
        TunerDescramblerRequest request = new TunerDescramblerRequest();
        request.clientId = this.mClientId;
        boolean granted = this.mTunerResourceManager.requestDescrambler(request, descramblerHandle);
        if (!granted) {
            return null;
        }
        int handle = descramblerHandle[0];
        Descrambler descrambler = nativeOpenDescramblerByHandle(handle);
        if (descrambler != null) {
            synchronized (this.mDescramblers) {
                WeakReference weakDescrambler = new WeakReference(descrambler);
                this.mDescramblers.put(Integer.valueOf(handle), weakDescrambler);
            }
        } else {
            this.mTunerResourceManager.releaseDescrambler(handle, this.mClientId);
        }
        return descrambler;
    }

    private boolean requestFrontendCiCam(int ciCamId) {
        int[] ciCamHandle = new int[1];
        TunerCiCamRequest request = new TunerCiCamRequest();
        request.clientId = this.mClientId;
        request.ciCamId = ciCamId;
        boolean granted = this.mTunerResourceManager.requestCiCam(request, ciCamHandle);
        if (granted) {
            this.mFrontendCiCamHandle = Integer.valueOf(ciCamHandle[0]);
            this.mFrontendCiCamId = Integer.valueOf(ciCamId);
        }
        return granted;
    }

    private boolean checkResource(int resourceType, ReentrantLock localLock) {
        switch (resourceType) {
            case 0:
                if (this.mFrontendHandle == null && !requestResource(resourceType, localLock)) {
                    return false;
                }
                return true;
            case 1:
                if (this.mDemuxHandle == null && !requestResource(resourceType, localLock)) {
                    return false;
                }
                return true;
            case 2:
            case 4:
            default:
                return false;
            case 3:
                if (this.mLnb == null && !requestResource(resourceType, localLock)) {
                    return false;
                }
                return true;
            case 5:
                if (this.mFrontendCiCamHandle == null && !requestResource(resourceType, localLock)) {
                    return false;
                }
                return true;
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2477=7, 2478=6] */
    private boolean requestResource(int resourceType, ReentrantLock localLock) {
        boolean enableLockOperations = localLock != null;
        if (enableLockOperations) {
            if (!localLock.isLocked()) {
                throw new IllegalStateException("local lock must be locked beforehand");
            }
            localLock.unlock();
        }
        if (enableLockOperations) {
            acquireTRMSLock("requestResource:" + resourceType);
        }
        if (enableLockOperations) {
            try {
                localLock.lock();
            } finally {
                if (enableLockOperations) {
                    releaseTRMSLock();
                }
            }
        }
        switch (resourceType) {
            case 0:
                boolean requestFrontend = requestFrontend();
                if (enableLockOperations) {
                    releaseTRMSLock();
                }
                return requestFrontend;
            case 1:
                boolean requestDemux = requestDemux();
                if (enableLockOperations) {
                    releaseTRMSLock();
                }
                return requestDemux;
            case 2:
            case 4:
            default:
                if (enableLockOperations) {
                    releaseTRMSLock();
                }
                return false;
            case 3:
                boolean requestLnb = requestLnb();
                if (enableLockOperations) {
                    releaseTRMSLock();
                }
                return requestLnb;
            case 5:
                return requestFrontendCiCam(this.mRequestedCiCamId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void releaseLnb() {
        acquireTRMSLock("releaseLnb()");
        this.mLnbLock.lock();
        try {
            if (this.mLnbHandle != null) {
                if (DEBUG) {
                    Log.d(TAG, "releasing Lnb");
                }
                this.mTunerResourceManager.releaseLnb(this.mLnbHandle.intValue(), this.mClientId);
                this.mLnbHandle = null;
            } else if (DEBUG) {
                Log.d(TAG, "NOT releasing Lnb because mLnbHandle is null");
            }
            this.mLnb = null;
        } finally {
            releaseTRMSLock();
            this.mLnbLock.unlock();
        }
    }

    public int getClientId() {
        return this.mClientId;
    }

    private void acquireTRMSLock(String functionNameForLog) {
        if (DEBUG) {
            Log.d(TAG, "ATTEMPT:acquireLock() in " + functionNameForLog + "for clientId:" + this.mClientId);
        }
        if (!this.mTunerResourceManager.acquireLock(this.mClientId)) {
            Log.e(TAG, "FAILED:acquireLock() in " + functionNameForLog + " for clientId:" + this.mClientId + " - this can cause deadlock between Tuner API calls and onReclaimResources()");
        }
    }

    private void releaseTRMSLock() {
        this.mTunerResourceManager.releaseLock(this.mClientId);
    }
}
