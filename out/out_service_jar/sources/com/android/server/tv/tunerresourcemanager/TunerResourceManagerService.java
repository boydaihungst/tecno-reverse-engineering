package com.android.server.tv.tunerresourcemanager;

import android.app.ActivityManager;
import android.content.Context;
import android.media.IResourceManagerService;
import android.media.tv.TvInputManager;
import android.media.tv.tunerresourcemanager.CasSessionRequest;
import android.media.tv.tunerresourcemanager.IResourcesReclaimListener;
import android.media.tv.tunerresourcemanager.ITunerResourceManager;
import android.media.tv.tunerresourcemanager.ResourceClientProfile;
import android.media.tv.tunerresourcemanager.TunerCiCamRequest;
import android.media.tv.tunerresourcemanager.TunerDemuxRequest;
import android.media.tv.tunerresourcemanager.TunerDescramblerRequest;
import android.media.tv.tunerresourcemanager.TunerFrontendInfo;
import android.media.tv.tunerresourcemanager.TunerFrontendRequest;
import android.media.tv.tunerresourcemanager.TunerLnbRequest;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.util.SparseIntArray;
import com.android.server.SystemService;
import com.android.server.am.HostingRecord;
import com.android.server.job.controllers.JobStatus;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.tv.tunerresourcemanager.CasResource;
import com.android.server.tv.tunerresourcemanager.CiCamResource;
import com.android.server.tv.tunerresourcemanager.ClientProfile;
import com.android.server.tv.tunerresourcemanager.FrontendResource;
import com.android.server.tv.tunerresourcemanager.LnbResource;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
/* loaded from: classes2.dex */
public class TunerResourceManagerService extends SystemService implements IBinder.DeathRecipient {
    public static final int INVALID_CLIENT_ID = -1;
    private static final int INVALID_FE_COUNT = -1;
    private static final long INVALID_THREAD_ID = -1;
    private static final int MAX_CLIENT_PRIORITY = 1000;
    private static final long TRMS_LOCK_TIMEOUT = 500;
    private ActivityManager mActivityManager;
    private Map<Integer, CasResource> mCasResources;
    private Map<Integer, CiCamResource> mCiCamResources;
    private Map<Integer, ClientProfile> mClientProfiles;
    private SparseIntArray mFrontendExistingNums;
    private SparseIntArray mFrontendExistingNumsBackup;
    private SparseIntArray mFrontendMaxUsableNums;
    private SparseIntArray mFrontendMaxUsableNumsBackup;
    private Map<Integer, FrontendResource> mFrontendResources;
    private Map<Integer, FrontendResource> mFrontendResourcesBackup;
    private SparseIntArray mFrontendUsedNums;
    private SparseIntArray mFrontendUsedNumsBackup;
    private Map<Integer, ResourcesReclaimListenerRecord> mListeners;
    private Map<Integer, LnbResource> mLnbResources;
    private final Object mLock;
    private final ReentrantLock mLockForTRMSLock;
    private IResourceManagerService mMediaResourceManager;
    private int mNextUnusedClientId;
    private UseCasePriorityHints mPriorityCongfig;
    private int mResourceRequestCount;
    private int mTunerApiLockHolder;
    private long mTunerApiLockHolderThreadId;
    private int mTunerApiLockNestedCount;
    private final Condition mTunerApiLockReleasedCV;
    private TvInputManager mTvInputManager;
    private static final String TAG = "TunerResourceManagerService";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    public TunerResourceManagerService(Context context) {
        super(context);
        this.mClientProfiles = new HashMap();
        this.mNextUnusedClientId = 0;
        this.mFrontendResources = new HashMap();
        this.mFrontendMaxUsableNums = new SparseIntArray();
        this.mFrontendUsedNums = new SparseIntArray();
        this.mFrontendExistingNums = new SparseIntArray();
        this.mFrontendResourcesBackup = new HashMap();
        this.mFrontendMaxUsableNumsBackup = new SparseIntArray();
        this.mFrontendUsedNumsBackup = new SparseIntArray();
        this.mFrontendExistingNumsBackup = new SparseIntArray();
        this.mLnbResources = new HashMap();
        this.mCasResources = new HashMap();
        this.mCiCamResources = new HashMap();
        this.mListeners = new HashMap();
        this.mPriorityCongfig = new UseCasePriorityHints();
        this.mResourceRequestCount = 0;
        this.mLock = new Object();
        ReentrantLock reentrantLock = new ReentrantLock();
        this.mLockForTRMSLock = reentrantLock;
        this.mTunerApiLockReleasedCV = reentrantLock.newCondition();
        this.mTunerApiLockHolder = -1;
        this.mTunerApiLockHolderThreadId = -1L;
        this.mTunerApiLockNestedCount = 0;
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        onStart(false);
    }

    protected void onStart(boolean isForTesting) {
        if (!isForTesting) {
            publishBinderService("tv_tuner_resource_mgr", new BinderService());
        }
        this.mTvInputManager = (TvInputManager) getContext().getSystemService("tv_input");
        this.mActivityManager = (ActivityManager) getContext().getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
        this.mPriorityCongfig.parse();
        if (this.mMediaResourceManager == null) {
            IBinder mediaResourceManagerBinder = getBinderService("media.resource_manager");
            if (mediaResourceManagerBinder == null) {
                Slog.w(TAG, "Resource Manager Service not available.");
                return;
            }
            try {
                mediaResourceManagerBinder.linkToDeath(this, 0);
                this.mMediaResourceManager = IResourceManagerService.Stub.asInterface(mediaResourceManagerBinder);
            } catch (RemoteException e) {
                Slog.w(TAG, "Could not link to death of native resource manager service.");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class BinderService extends ITunerResourceManager.Stub {
        private BinderService() {
        }

        public void registerClientProfile(ResourceClientProfile profile, IResourcesReclaimListener listener, int[] clientId) throws RemoteException {
            TunerResourceManagerService.this.enforceTrmAccessPermission("registerClientProfile");
            TunerResourceManagerService.this.enforceTunerAccessPermission("registerClientProfile");
            if (profile == null) {
                throw new RemoteException("ResourceClientProfile can't be null");
            }
            if (clientId == null) {
                throw new RemoteException("clientId can't be null!");
            }
            if (listener == null) {
                throw new RemoteException("IResourcesReclaimListener can't be null!");
            }
            if (!TunerResourceManagerService.this.mPriorityCongfig.isDefinedUseCase(profile.useCase)) {
                throw new RemoteException("Use undefined client use case:" + profile.useCase);
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                TunerResourceManagerService.this.registerClientProfileInternal(profile, listener, clientId);
            }
        }

        public void unregisterClientProfile(int clientId) throws RemoteException {
            TunerResourceManagerService.this.enforceTrmAccessPermission("unregisterClientProfile");
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(clientId)) {
                    Slog.e(TunerResourceManagerService.TAG, "Unregistering non exists client:" + clientId);
                } else {
                    TunerResourceManagerService.this.unregisterClientProfileInternal(clientId);
                }
            }
        }

        public boolean updateClientPriority(int clientId, int priority, int niceValue) {
            boolean updateClientPriorityInternal;
            TunerResourceManagerService.this.enforceTrmAccessPermission("updateClientPriority");
            synchronized (TunerResourceManagerService.this.mLock) {
                updateClientPriorityInternal = TunerResourceManagerService.this.updateClientPriorityInternal(clientId, priority, niceValue);
            }
            return updateClientPriorityInternal;
        }

        public boolean hasUnusedFrontend(int frontendType) {
            boolean hasUnusedFrontendInternal;
            TunerResourceManagerService.this.enforceTrmAccessPermission("hasUnusedFrontend");
            synchronized (TunerResourceManagerService.this.mLock) {
                hasUnusedFrontendInternal = TunerResourceManagerService.this.hasUnusedFrontendInternal(frontendType);
            }
            return hasUnusedFrontendInternal;
        }

        public boolean isLowestPriority(int clientId, int frontendType) throws RemoteException {
            boolean isLowestPriorityInternal;
            TunerResourceManagerService.this.enforceTrmAccessPermission("isLowestPriority");
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(clientId)) {
                    throw new RemoteException("isLowestPriority called from unregistered client: " + clientId);
                }
                isLowestPriorityInternal = TunerResourceManagerService.this.isLowestPriorityInternal(clientId, frontendType);
            }
            return isLowestPriorityInternal;
        }

        public void setFrontendInfoList(TunerFrontendInfo[] infos) throws RemoteException {
            TunerResourceManagerService.this.enforceTrmAccessPermission("setFrontendInfoList");
            if (infos == null) {
                throw new RemoteException("TunerFrontendInfo can't be null");
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                TunerResourceManagerService.this.setFrontendInfoListInternal(infos);
            }
        }

        public void updateCasInfo(int casSystemId, int maxSessionNum) {
            TunerResourceManagerService.this.enforceTrmAccessPermission("updateCasInfo");
            synchronized (TunerResourceManagerService.this.mLock) {
                TunerResourceManagerService.this.updateCasInfoInternal(casSystemId, maxSessionNum);
            }
        }

        public void setLnbInfoList(int[] lnbHandles) throws RemoteException {
            TunerResourceManagerService.this.enforceTrmAccessPermission("setLnbInfoList");
            if (lnbHandles == null) {
                throw new RemoteException("Lnb handle list can't be null");
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                TunerResourceManagerService.this.setLnbInfoListInternal(lnbHandles);
            }
        }

        public boolean requestFrontend(TunerFrontendRequest request, int[] frontendHandle) {
            TunerResourceManagerService.this.enforceTunerAccessPermission("requestFrontend");
            TunerResourceManagerService.this.enforceTrmAccessPermission("requestFrontend");
            if (frontendHandle == null) {
                Slog.e(TunerResourceManagerService.TAG, "frontendHandle can't be null");
                return false;
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(request.clientId)) {
                    Slog.e(TunerResourceManagerService.TAG, "Request frontend from unregistered client: " + request.clientId);
                    return false;
                } else if (!TunerResourceManagerService.this.getClientProfile(request.clientId).getInUseFrontendHandles().isEmpty()) {
                    Slog.e(TunerResourceManagerService.TAG, "Release frontend before requesting another one. Client id: " + request.clientId);
                    return false;
                } else {
                    return TunerResourceManagerService.this.requestFrontendInternal(request, frontendHandle);
                }
            }
        }

        public boolean setMaxNumberOfFrontends(int frontendType, int maxUsableNum) {
            boolean maxNumberOfFrontendsInternal;
            TunerResourceManagerService.this.enforceTunerAccessPermission("requestFrontend");
            TunerResourceManagerService.this.enforceTrmAccessPermission("requestFrontend");
            if (maxUsableNum < 0) {
                Slog.w(TunerResourceManagerService.TAG, "setMaxNumberOfFrontends failed with maxUsableNum:" + maxUsableNum + " frontendType:" + frontendType);
                return false;
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                maxNumberOfFrontendsInternal = TunerResourceManagerService.this.setMaxNumberOfFrontendsInternal(frontendType, maxUsableNum);
            }
            return maxNumberOfFrontendsInternal;
        }

        public int getMaxNumberOfFrontends(int frontendType) {
            int maxNumberOfFrontendsInternal;
            TunerResourceManagerService.this.enforceTunerAccessPermission("requestFrontend");
            TunerResourceManagerService.this.enforceTrmAccessPermission("requestFrontend");
            synchronized (TunerResourceManagerService.this.mLock) {
                maxNumberOfFrontendsInternal = TunerResourceManagerService.this.getMaxNumberOfFrontendsInternal(frontendType);
            }
            return maxNumberOfFrontendsInternal;
        }

        public void shareFrontend(int selfClientId, int targetClientId) throws RemoteException {
            TunerResourceManagerService.this.enforceTunerAccessPermission("shareFrontend");
            TunerResourceManagerService.this.enforceTrmAccessPermission("shareFrontend");
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(selfClientId)) {
                    throw new RemoteException("Share frontend request from an unregistered client:" + selfClientId);
                }
                if (!TunerResourceManagerService.this.checkClientExists(targetClientId)) {
                    throw new RemoteException("Request to share frontend with an unregistered client:" + targetClientId);
                }
                if (TunerResourceManagerService.this.getClientProfile(targetClientId).getInUseFrontendHandles().isEmpty()) {
                    throw new RemoteException("Request to share frontend with a client that has no frontend resources. Target client id:" + targetClientId);
                }
                TunerResourceManagerService.this.shareFrontendInternal(selfClientId, targetClientId);
            }
        }

        public boolean transferOwner(int resourceType, int currentOwnerId, int newOwnerId) {
            TunerResourceManagerService.this.enforceTunerAccessPermission("transferOwner");
            TunerResourceManagerService.this.enforceTrmAccessPermission("transferOwner");
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(currentOwnerId)) {
                    Slog.e(TunerResourceManagerService.TAG, "currentOwnerId:" + currentOwnerId + " does not exit");
                    return false;
                } else if (!TunerResourceManagerService.this.checkClientExists(newOwnerId)) {
                    Slog.e(TunerResourceManagerService.TAG, "newOwnerId:" + newOwnerId + " does not exit");
                    return false;
                } else {
                    return TunerResourceManagerService.this.transferOwnerInternal(resourceType, currentOwnerId, newOwnerId);
                }
            }
        }

        public boolean requestDemux(TunerDemuxRequest request, int[] demuxHandle) throws RemoteException {
            boolean requestDemuxInternal;
            TunerResourceManagerService.this.enforceTunerAccessPermission("requestDemux");
            TunerResourceManagerService.this.enforceTrmAccessPermission("requestDemux");
            if (demuxHandle == null) {
                throw new RemoteException("demuxHandle can't be null");
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(request.clientId)) {
                    throw new RemoteException("Request demux from unregistered client:" + request.clientId);
                }
                requestDemuxInternal = TunerResourceManagerService.this.requestDemuxInternal(request, demuxHandle);
            }
            return requestDemuxInternal;
        }

        public boolean requestDescrambler(TunerDescramblerRequest request, int[] descramblerHandle) throws RemoteException {
            boolean requestDescramblerInternal;
            TunerResourceManagerService.this.enforceDescramblerAccessPermission("requestDescrambler");
            TunerResourceManagerService.this.enforceTrmAccessPermission("requestDescrambler");
            if (descramblerHandle == null) {
                throw new RemoteException("descramblerHandle can't be null");
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(request.clientId)) {
                    throw new RemoteException("Request descrambler from unregistered client:" + request.clientId);
                }
                requestDescramblerInternal = TunerResourceManagerService.this.requestDescramblerInternal(request, descramblerHandle);
            }
            return requestDescramblerInternal;
        }

        public boolean requestCasSession(CasSessionRequest request, int[] casSessionHandle) throws RemoteException {
            boolean requestCasSessionInternal;
            TunerResourceManagerService.this.enforceTrmAccessPermission("requestCasSession");
            if (casSessionHandle == null) {
                throw new RemoteException("casSessionHandle can't be null");
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(request.clientId)) {
                    throw new RemoteException("Request cas from unregistered client:" + request.clientId);
                }
                requestCasSessionInternal = TunerResourceManagerService.this.requestCasSessionInternal(request, casSessionHandle);
            }
            return requestCasSessionInternal;
        }

        public boolean requestCiCam(TunerCiCamRequest request, int[] ciCamHandle) throws RemoteException {
            boolean requestCiCamInternal;
            TunerResourceManagerService.this.enforceTrmAccessPermission("requestCiCam");
            if (ciCamHandle == null) {
                throw new RemoteException("ciCamHandle can't be null");
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(request.clientId)) {
                    throw new RemoteException("Request ciCam from unregistered client:" + request.clientId);
                }
                requestCiCamInternal = TunerResourceManagerService.this.requestCiCamInternal(request, ciCamHandle);
            }
            return requestCiCamInternal;
        }

        public boolean requestLnb(TunerLnbRequest request, int[] lnbHandle) throws RemoteException {
            boolean requestLnbInternal;
            TunerResourceManagerService.this.enforceTunerAccessPermission("requestLnb");
            TunerResourceManagerService.this.enforceTrmAccessPermission("requestLnb");
            if (lnbHandle == null) {
                throw new RemoteException("lnbHandle can't be null");
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(request.clientId)) {
                    throw new RemoteException("Request lnb from unregistered client:" + request.clientId);
                }
                requestLnbInternal = TunerResourceManagerService.this.requestLnbInternal(request, lnbHandle);
            }
            return requestLnbInternal;
        }

        public void releaseFrontend(int frontendHandle, int clientId) throws RemoteException {
            TunerResourceManagerService.this.enforceTunerAccessPermission("releaseFrontend");
            TunerResourceManagerService.this.enforceTrmAccessPermission("releaseFrontend");
            if (!TunerResourceManagerService.this.validateResourceHandle(0, frontendHandle)) {
                throw new RemoteException("frontendHandle can't be invalid");
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(clientId)) {
                    throw new RemoteException("Release frontend from unregistered client:" + clientId);
                }
                FrontendResource fe = TunerResourceManagerService.this.getFrontendResource(frontendHandle);
                if (fe == null) {
                    throw new RemoteException("Releasing frontend does not exist.");
                }
                int ownerClientId = fe.getOwnerClientId();
                ClientProfile ownerProfile = TunerResourceManagerService.this.getClientProfile(ownerClientId);
                if (ownerClientId != clientId && ownerProfile != null && !ownerProfile.getShareFeClientIds().contains(Integer.valueOf(clientId))) {
                    throw new RemoteException("Client is not the current owner of the releasing fe.");
                }
                TunerResourceManagerService.this.releaseFrontendInternal(fe, clientId);
            }
        }

        public void releaseDemux(int demuxHandle, int clientId) {
            TunerResourceManagerService.this.enforceTunerAccessPermission("releaseDemux");
            TunerResourceManagerService.this.enforceTrmAccessPermission("releaseDemux");
            if (TunerResourceManagerService.DEBUG) {
                Slog.d(TunerResourceManagerService.TAG, "releaseDemux(demuxHandle=" + demuxHandle + ")");
            }
        }

        public void releaseDescrambler(int descramblerHandle, int clientId) {
            TunerResourceManagerService.this.enforceTunerAccessPermission("releaseDescrambler");
            TunerResourceManagerService.this.enforceTrmAccessPermission("releaseDescrambler");
            if (TunerResourceManagerService.DEBUG) {
                Slog.d(TunerResourceManagerService.TAG, "releaseDescrambler(descramblerHandle=" + descramblerHandle + ")");
            }
        }

        public void releaseCasSession(int casSessionHandle, int clientId) throws RemoteException {
            TunerResourceManagerService.this.enforceTrmAccessPermission("releaseCasSession");
            if (!TunerResourceManagerService.this.validateResourceHandle(4, casSessionHandle)) {
                throw new RemoteException("casSessionHandle can't be invalid");
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(clientId)) {
                    throw new RemoteException("Release cas from unregistered client:" + clientId);
                }
                int casSystemId = TunerResourceManagerService.this.getClientProfile(clientId).getInUseCasSystemId();
                CasResource cas = TunerResourceManagerService.this.getCasResource(casSystemId);
                if (cas == null) {
                    throw new RemoteException("Releasing cas does not exist.");
                }
                if (!cas.getOwnerClientIds().contains(Integer.valueOf(clientId))) {
                    throw new RemoteException("Client is not the current owner of the releasing cas.");
                }
                TunerResourceManagerService.this.releaseCasSessionInternal(cas, clientId);
            }
        }

        public void releaseCiCam(int ciCamHandle, int clientId) throws RemoteException {
            TunerResourceManagerService.this.enforceTrmAccessPermission("releaseCiCam");
            if (!TunerResourceManagerService.this.validateResourceHandle(5, ciCamHandle)) {
                throw new RemoteException("ciCamHandle can't be invalid");
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(clientId)) {
                    throw new RemoteException("Release ciCam from unregistered client:" + clientId);
                }
                int ciCamId = TunerResourceManagerService.this.getClientProfile(clientId).getInUseCiCamId();
                if (ciCamId != TunerResourceManagerService.this.getResourceIdFromHandle(ciCamHandle)) {
                    throw new RemoteException("The client " + clientId + " is not the owner of the releasing ciCam.");
                }
                CiCamResource ciCam = TunerResourceManagerService.this.getCiCamResource(ciCamId);
                if (ciCam == null) {
                    throw new RemoteException("Releasing ciCam does not exist.");
                }
                if (!ciCam.getOwnerClientIds().contains(Integer.valueOf(clientId))) {
                    throw new RemoteException("Client is not the current owner of the releasing ciCam.");
                }
                TunerResourceManagerService.this.releaseCiCamInternal(ciCam, clientId);
            }
        }

        public void releaseLnb(int lnbHandle, int clientId) throws RemoteException {
            TunerResourceManagerService.this.enforceTunerAccessPermission("releaseLnb");
            TunerResourceManagerService.this.enforceTrmAccessPermission("releaseLnb");
            if (!TunerResourceManagerService.this.validateResourceHandle(3, lnbHandle)) {
                throw new RemoteException("lnbHandle can't be invalid");
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                if (!TunerResourceManagerService.this.checkClientExists(clientId)) {
                    throw new RemoteException("Release lnb from unregistered client:" + clientId);
                }
                LnbResource lnb = TunerResourceManagerService.this.getLnbResource(lnbHandle);
                if (lnb == null) {
                    throw new RemoteException("Releasing lnb does not exist.");
                }
                if (lnb.getOwnerClientId() != clientId) {
                    throw new RemoteException("Client is not the current owner of the releasing lnb.");
                }
                TunerResourceManagerService.this.releaseLnbInternal(lnb);
            }
        }

        public boolean isHigherPriority(ResourceClientProfile challengerProfile, ResourceClientProfile holderProfile) throws RemoteException {
            boolean isHigherPriorityInternal;
            TunerResourceManagerService.this.enforceTrmAccessPermission("isHigherPriority");
            if (challengerProfile == null || holderProfile == null) {
                throw new RemoteException("Client profiles can't be null.");
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                isHigherPriorityInternal = TunerResourceManagerService.this.isHigherPriorityInternal(challengerProfile, holderProfile);
            }
            return isHigherPriorityInternal;
        }

        public void storeResourceMap(int resourceType) {
            TunerResourceManagerService.this.enforceTrmAccessPermission("storeResourceMap");
            synchronized (TunerResourceManagerService.this.mLock) {
                TunerResourceManagerService.this.storeResourceMapInternal(resourceType);
            }
        }

        public void clearResourceMap(int resourceType) {
            TunerResourceManagerService.this.enforceTrmAccessPermission("clearResourceMap");
            synchronized (TunerResourceManagerService.this.mLock) {
                TunerResourceManagerService.this.clearResourceMapInternal(resourceType);
            }
        }

        public void restoreResourceMap(int resourceType) {
            TunerResourceManagerService.this.enforceTrmAccessPermission("restoreResourceMap");
            synchronized (TunerResourceManagerService.this.mLock) {
                TunerResourceManagerService.this.restoreResourceMapInternal(resourceType);
            }
        }

        public boolean acquireLock(int clientId, long clientThreadId) {
            TunerResourceManagerService.this.enforceTrmAccessPermission("acquireLock");
            return TunerResourceManagerService.this.acquireLockInternal(clientId, clientThreadId, 500L);
        }

        public boolean releaseLock(int clientId) {
            TunerResourceManagerService.this.enforceTrmAccessPermission("releaseLock");
            return TunerResourceManagerService.this.releaseLockInternal(clientId, 500L, false, false);
        }

        protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
            IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
            if (TunerResourceManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump!");
                return;
            }
            synchronized (TunerResourceManagerService.this.mLock) {
                TunerResourceManagerService tunerResourceManagerService = TunerResourceManagerService.this;
                tunerResourceManagerService.dumpMap(tunerResourceManagerService.mClientProfiles, "ClientProfiles:", "\n", pw);
                TunerResourceManagerService tunerResourceManagerService2 = TunerResourceManagerService.this;
                tunerResourceManagerService2.dumpMap(tunerResourceManagerService2.mFrontendResources, "FrontendResources:", "\n", pw);
                TunerResourceManagerService tunerResourceManagerService3 = TunerResourceManagerService.this;
                tunerResourceManagerService3.dumpSIA(tunerResourceManagerService3.mFrontendExistingNums, "FrontendExistingNums:", ", ", pw);
                TunerResourceManagerService tunerResourceManagerService4 = TunerResourceManagerService.this;
                tunerResourceManagerService4.dumpSIA(tunerResourceManagerService4.mFrontendUsedNums, "FrontendUsedNums:", ", ", pw);
                TunerResourceManagerService tunerResourceManagerService5 = TunerResourceManagerService.this;
                tunerResourceManagerService5.dumpSIA(tunerResourceManagerService5.mFrontendMaxUsableNums, "FrontendMaxUsableNums:", ", ", pw);
                TunerResourceManagerService tunerResourceManagerService6 = TunerResourceManagerService.this;
                tunerResourceManagerService6.dumpMap(tunerResourceManagerService6.mFrontendResourcesBackup, "FrontendResourcesBackUp:", "\n", pw);
                TunerResourceManagerService tunerResourceManagerService7 = TunerResourceManagerService.this;
                tunerResourceManagerService7.dumpSIA(tunerResourceManagerService7.mFrontendExistingNumsBackup, "FrontendExistingNumsBackup:", ", ", pw);
                TunerResourceManagerService tunerResourceManagerService8 = TunerResourceManagerService.this;
                tunerResourceManagerService8.dumpSIA(tunerResourceManagerService8.mFrontendUsedNumsBackup, "FrontendUsedNumsBackup:", ", ", pw);
                TunerResourceManagerService tunerResourceManagerService9 = TunerResourceManagerService.this;
                tunerResourceManagerService9.dumpSIA(tunerResourceManagerService9.mFrontendMaxUsableNumsBackup, "FrontendUsedNumsBackup:", ", ", pw);
                TunerResourceManagerService tunerResourceManagerService10 = TunerResourceManagerService.this;
                tunerResourceManagerService10.dumpMap(tunerResourceManagerService10.mLnbResources, "LnbResource:", "\n", pw);
                TunerResourceManagerService tunerResourceManagerService11 = TunerResourceManagerService.this;
                tunerResourceManagerService11.dumpMap(tunerResourceManagerService11.mCasResources, "CasResource:", "\n", pw);
                TunerResourceManagerService tunerResourceManagerService12 = TunerResourceManagerService.this;
                tunerResourceManagerService12.dumpMap(tunerResourceManagerService12.mCiCamResources, "CiCamResource:", "\n", pw);
                TunerResourceManagerService tunerResourceManagerService13 = TunerResourceManagerService.this;
                tunerResourceManagerService13.dumpMap(tunerResourceManagerService13.mListeners, "Listners:", "\n", pw);
            }
        }

        public int getClientPriority(int useCase, int pid) throws RemoteException {
            int clientPriority;
            TunerResourceManagerService.this.enforceTrmAccessPermission("getClientPriority");
            synchronized (TunerResourceManagerService.this.mLock) {
                TunerResourceManagerService tunerResourceManagerService = TunerResourceManagerService.this;
                clientPriority = tunerResourceManagerService.getClientPriority(useCase, tunerResourceManagerService.checkIsForeground(pid));
            }
            return clientPriority;
        }

        public int getConfigPriority(int useCase, boolean isForeground) throws RemoteException {
            int clientPriority;
            TunerResourceManagerService.this.enforceTrmAccessPermission("getConfigPriority");
            synchronized (TunerResourceManagerService.this.mLock) {
                clientPriority = TunerResourceManagerService.this.getClientPriority(useCase, isForeground);
            }
            return clientPriority;
        }
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        if (DEBUG) {
            Slog.w(TAG, "Native media resource manager service has died");
        }
        synchronized (this.mLock) {
            this.mMediaResourceManager = null;
        }
    }

    protected void registerClientProfileInternal(ResourceClientProfile profile, IResourcesReclaimListener listener, int[] clientId) {
        int pid;
        IResourceManagerService iResourceManagerService;
        if (DEBUG) {
            Slog.d(TAG, "registerClientProfile(clientProfile=" + profile + ")");
        }
        clientId[0] = -1;
        if (this.mTvInputManager == null) {
            Slog.e(TAG, "TvInputManager is null. Can't register client profile.");
            return;
        }
        int i = this.mNextUnusedClientId;
        this.mNextUnusedClientId = i + 1;
        clientId[0] = i;
        if (profile.tvInputSessionId == null) {
            pid = Binder.getCallingPid();
        } else {
            pid = this.mTvInputManager.getClientPid(profile.tvInputSessionId);
        }
        if (profile.tvInputSessionId != null && (iResourceManagerService = this.mMediaResourceManager) != null) {
            try {
                iResourceManagerService.overridePid(Binder.getCallingPid(), pid);
            } catch (RemoteException e) {
                Slog.e(TAG, "Could not overridePid in resourceManagerSercice, remote exception: " + e);
            }
        }
        ClientProfile clientProfile = new ClientProfile.Builder(clientId[0]).tvInputSessionId(profile.tvInputSessionId).useCase(profile.useCase).processId(pid).build();
        clientProfile.setPriority(getClientPriority(profile.useCase, checkIsForeground(pid)));
        addClientProfile(clientId[0], clientProfile, listener);
    }

    protected void unregisterClientProfileInternal(int clientId) {
        if (DEBUG) {
            Slog.d(TAG, "unregisterClientProfile(clientId=" + clientId + ")");
        }
        removeClientProfile(clientId);
        IResourceManagerService iResourceManagerService = this.mMediaResourceManager;
        if (iResourceManagerService != null) {
            try {
                iResourceManagerService.overridePid(Binder.getCallingPid(), -1);
            } catch (RemoteException e) {
                Slog.e(TAG, "Could not overridePid in resourceManagerSercice when unregister, remote exception: " + e);
            }
        }
    }

    protected boolean updateClientPriorityInternal(int clientId, int priority, int niceValue) {
        if (DEBUG) {
            Slog.d(TAG, "updateClientPriority(clientId=" + clientId + ", priority=" + priority + ", niceValue=" + niceValue + ")");
        }
        ClientProfile profile = getClientProfile(clientId);
        if (profile == null) {
            Slog.e(TAG, "Can not find client profile with id " + clientId + " when trying to update the client priority.");
            return false;
        }
        profile.overwritePriority(priority);
        profile.setNiceValue(niceValue);
        return true;
    }

    protected boolean hasUnusedFrontendInternal(int frontendType) {
        for (FrontendResource fr : getFrontendResources().values()) {
            if (fr.getType() == frontendType && !fr.isInUse()) {
                return true;
            }
        }
        return false;
    }

    protected boolean isLowestPriorityInternal(int clientId, int frontendType) throws RemoteException {
        ClientProfile requestClient = getClientProfile(clientId);
        if (requestClient == null) {
            return true;
        }
        clientPriorityUpdateOnRequest(requestClient);
        int clientPriority = requestClient.getPriority();
        for (FrontendResource fr : getFrontendResources().values()) {
            if (fr.getType() == frontendType && fr.isInUse()) {
                int priority = updateAndGetOwnerClientPriority(fr.getOwnerClientId());
                if (clientPriority > priority) {
                    return false;
                }
            }
        }
        return true;
    }

    protected void storeResourceMapInternal(int resourceType) {
        switch (resourceType) {
            case 0:
                replaceFeResourceMap(this.mFrontendResources, this.mFrontendResourcesBackup);
                replaceFeCounts(this.mFrontendExistingNums, this.mFrontendExistingNumsBackup);
                replaceFeCounts(this.mFrontendUsedNums, this.mFrontendUsedNumsBackup);
                replaceFeCounts(this.mFrontendMaxUsableNums, this.mFrontendMaxUsableNumsBackup);
                return;
            default:
                return;
        }
    }

    protected void clearResourceMapInternal(int resourceType) {
        switch (resourceType) {
            case 0:
                replaceFeResourceMap(null, this.mFrontendResources);
                replaceFeCounts(null, this.mFrontendExistingNums);
                replaceFeCounts(null, this.mFrontendUsedNums);
                replaceFeCounts(null, this.mFrontendMaxUsableNums);
                return;
            default:
                return;
        }
    }

    protected void restoreResourceMapInternal(int resourceType) {
        switch (resourceType) {
            case 0:
                replaceFeResourceMap(this.mFrontendResourcesBackup, this.mFrontendResources);
                replaceFeCounts(this.mFrontendExistingNumsBackup, this.mFrontendExistingNums);
                replaceFeCounts(this.mFrontendUsedNumsBackup, this.mFrontendUsedNums);
                replaceFeCounts(this.mFrontendMaxUsableNumsBackup, this.mFrontendMaxUsableNums);
                return;
            default:
                return;
        }
    }

    protected void setFrontendInfoListInternal(TunerFrontendInfo[] infos) {
        if (DEBUG) {
            Slog.d(TAG, "updateFrontendInfo:");
            for (TunerFrontendInfo tunerFrontendInfo : infos) {
                Slog.d(TAG, tunerFrontendInfo.toString());
            }
        }
        Set<Integer> updatingFrontendHandles = new HashSet<>(getFrontendResources().keySet());
        for (int i = 0; i < infos.length; i++) {
            if (getFrontendResource(infos[i].handle) != null) {
                if (DEBUG) {
                    Slog.d(TAG, "Frontend handle=" + infos[i].handle + "exists.");
                }
                updatingFrontendHandles.remove(Integer.valueOf(infos[i].handle));
            } else {
                FrontendResource newFe = new FrontendResource.Builder(infos[i].handle).type(infos[i].type).exclusiveGroupId(infos[i].exclusiveGroupId).build();
                addFrontendResource(newFe);
            }
        }
        for (Integer num : updatingFrontendHandles) {
            int removingHandle = num.intValue();
            removeFrontendResource(removingHandle);
        }
    }

    protected void setLnbInfoListInternal(int[] lnbHandles) {
        if (DEBUG) {
            for (int i = 0; i < lnbHandles.length; i++) {
                Slog.d(TAG, "updateLnbInfo(lnbHanle=" + lnbHandles[i] + ")");
            }
        }
        Set<Integer> updatingLnbHandles = new HashSet<>(getLnbResources().keySet());
        for (int i2 = 0; i2 < lnbHandles.length; i2++) {
            if (getLnbResource(lnbHandles[i2]) != null) {
                if (DEBUG) {
                    Slog.d(TAG, "Lnb handle=" + lnbHandles[i2] + "exists.");
                }
                updatingLnbHandles.remove(Integer.valueOf(lnbHandles[i2]));
            } else {
                LnbResource newLnb = new LnbResource.Builder(lnbHandles[i2]).build();
                addLnbResource(newLnb);
            }
        }
        for (Integer num : updatingLnbHandles) {
            int removingHandle = num.intValue();
            removeLnbResource(removingHandle);
        }
    }

    protected void updateCasInfoInternal(int casSystemId, int maxSessionNum) {
        if (DEBUG) {
            Slog.d(TAG, "updateCasInfo(casSystemId=" + casSystemId + ", maxSessionNum=" + maxSessionNum + ")");
        }
        if (maxSessionNum == 0) {
            removeCasResource(casSystemId);
            removeCiCamResource(casSystemId);
            return;
        }
        CasResource cas = getCasResource(casSystemId);
        CiCamResource ciCam = getCiCamResource(casSystemId);
        if (cas != null) {
            if (cas.getUsedSessionNum() > maxSessionNum) {
                cas.getUsedSessionNum();
            }
            cas.updateMaxSessionNum(maxSessionNum);
            if (ciCam != null) {
                ciCam.updateMaxSessionNum(maxSessionNum);
                return;
            }
            return;
        }
        CasResource cas2 = new CasResource.Builder(casSystemId).maxSessionNum(maxSessionNum).build();
        CiCamResource ciCam2 = new CiCamResource.Builder(casSystemId).maxSessionNum(maxSessionNum).build();
        addCasResource(cas2);
        addCiCamResource(ciCam2);
    }

    protected boolean requestFrontendInternal(TunerFrontendRequest request, int[] frontendHandle) {
        int priority;
        if (DEBUG) {
            Slog.d(TAG, "requestFrontend(request=" + request + ")");
        }
        frontendHandle[0] = -1;
        ClientProfile requestClient = getClientProfile(request.clientId);
        if (requestClient == null) {
            return false;
        }
        clientPriorityUpdateOnRequest(requestClient);
        int grantingFrontendHandle = -1;
        int inUseLowestPriorityFrHandle = -1;
        int currentLowestPriority = 1001;
        Iterator<FrontendResource> it = getFrontendResources().values().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            FrontendResource fr = it.next();
            if (fr.getType() == request.frontendType) {
                if (!fr.isInUse()) {
                    if (isFrontendMaxNumUseReached(request.frontendType)) {
                        continue;
                    } else if (fr.getExclusiveGroupMemberFeHandles().isEmpty()) {
                        grantingFrontendHandle = fr.getHandle();
                        break;
                    } else if (grantingFrontendHandle == -1) {
                        grantingFrontendHandle = fr.getHandle();
                    }
                } else if (grantingFrontendHandle == -1 && currentLowestPriority > (priority = getFrontendHighestClientPriority(fr.getOwnerClientId()))) {
                    inUseLowestPriorityFrHandle = fr.getHandle();
                    currentLowestPriority = priority;
                }
            }
        }
        if (grantingFrontendHandle != -1) {
            frontendHandle[0] = grantingFrontendHandle;
            updateFrontendClientMappingOnNewGrant(grantingFrontendHandle, request.clientId);
            return true;
        } else if (inUseLowestPriorityFrHandle == -1 || requestClient.getPriority() <= currentLowestPriority || !reclaimResource(getFrontendResource(inUseLowestPriorityFrHandle).getOwnerClientId(), 0)) {
            return false;
        } else {
            frontendHandle[0] = inUseLowestPriorityFrHandle;
            updateFrontendClientMappingOnNewGrant(inUseLowestPriorityFrHandle, request.clientId);
            return true;
        }
    }

    protected void shareFrontendInternal(int selfClientId, int targetClientId) {
        if (DEBUG) {
            Slog.d(TAG, "shareFrontend from " + selfClientId + " with " + targetClientId);
        }
        for (Integer num : getClientProfile(targetClientId).getInUseFrontendHandles()) {
            int feId = num.intValue();
            getClientProfile(selfClientId).useFrontend(feId);
        }
        getClientProfile(targetClientId).shareFrontend(selfClientId);
    }

    private boolean transferFeOwner(int currentOwnerId, int newOwnerId) {
        ClientProfile currentOwnerProfile = getClientProfile(currentOwnerId);
        ClientProfile newOwnerProfile = getClientProfile(newOwnerId);
        newOwnerProfile.shareFrontend(currentOwnerId);
        currentOwnerProfile.stopSharingFrontend(newOwnerId);
        for (Integer num : newOwnerProfile.getInUseFrontendHandles()) {
            getFrontendResource(num.intValue()).setOwner(newOwnerId);
        }
        newOwnerProfile.setPrimaryFrontend(currentOwnerProfile.getPrimaryFrontend());
        currentOwnerProfile.setPrimaryFrontend(-1);
        for (Integer num2 : currentOwnerProfile.getInUseFrontendHandles()) {
            int inUseHandle = num2.intValue();
            int ownerId = getFrontendResource(inUseHandle).getOwnerClientId();
            if (ownerId != newOwnerId) {
                Slog.e(TAG, "something is wrong in transferFeOwner:" + inUseHandle + ", " + ownerId + ", " + newOwnerId);
                return false;
            }
        }
        return true;
    }

    private boolean transferFeCiCamOwner(int currentOwnerId, int newOwnerId) {
        ClientProfile currentOwnerProfile = getClientProfile(currentOwnerId);
        ClientProfile newOwnerProfile = getClientProfile(newOwnerId);
        int ciCamId = currentOwnerProfile.getInUseCiCamId();
        newOwnerProfile.useCiCam(ciCamId);
        CiCamResource ciCam = getCiCamResource(ciCamId);
        ciCam.setOwner(newOwnerId);
        currentOwnerProfile.releaseCiCam();
        return true;
    }

    private boolean transferLnbOwner(int currentOwnerId, int newOwnerId) {
        ClientProfile currentOwnerProfile = getClientProfile(currentOwnerId);
        ClientProfile newOwnerProfile = getClientProfile(newOwnerId);
        Set<Integer> inUseLnbHandles = new HashSet<>();
        for (Integer lnbHandle : currentOwnerProfile.getInUseLnbHandles()) {
            newOwnerProfile.useLnb(lnbHandle.intValue());
            LnbResource lnb = getLnbResource(lnbHandle.intValue());
            lnb.setOwner(newOwnerId);
            inUseLnbHandles.add(lnbHandle);
        }
        for (Integer lnbHandle2 : inUseLnbHandles) {
            currentOwnerProfile.releaseLnb(lnbHandle2.intValue());
        }
        return true;
    }

    protected boolean transferOwnerInternal(int resourceType, int currentOwnerId, int newOwnerId) {
        switch (resourceType) {
            case 0:
                return transferFeOwner(currentOwnerId, newOwnerId);
            case 3:
                return transferLnbOwner(currentOwnerId, newOwnerId);
            case 5:
                return transferFeCiCamOwner(currentOwnerId, newOwnerId);
            default:
                Slog.e(TAG, "transferOwnerInternal. unsupported resourceType: " + resourceType);
                return false;
        }
    }

    protected boolean requestLnbInternal(TunerLnbRequest request, int[] lnbHandle) {
        if (DEBUG) {
            Slog.d(TAG, "requestLnb(request=" + request + ")");
        }
        lnbHandle[0] = -1;
        ClientProfile requestClient = getClientProfile(request.clientId);
        clientPriorityUpdateOnRequest(requestClient);
        int grantingLnbHandle = -1;
        int inUseLowestPriorityLnbHandle = -1;
        int currentLowestPriority = 1001;
        Iterator<LnbResource> it = getLnbResources().values().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            LnbResource lnb = it.next();
            if (!lnb.isInUse()) {
                grantingLnbHandle = lnb.getHandle();
                break;
            }
            int priority = updateAndGetOwnerClientPriority(lnb.getOwnerClientId());
            if (currentLowestPriority > priority) {
                inUseLowestPriorityLnbHandle = lnb.getHandle();
                currentLowestPriority = priority;
            }
        }
        if (grantingLnbHandle > -1) {
            lnbHandle[0] = grantingLnbHandle;
            updateLnbClientMappingOnNewGrant(grantingLnbHandle, request.clientId);
            return true;
        } else if (inUseLowestPriorityLnbHandle <= -1 || requestClient.getPriority() <= currentLowestPriority || !reclaimResource(getLnbResource(inUseLowestPriorityLnbHandle).getOwnerClientId(), 3)) {
            return false;
        } else {
            lnbHandle[0] = inUseLowestPriorityLnbHandle;
            updateLnbClientMappingOnNewGrant(inUseLowestPriorityLnbHandle, request.clientId);
            return true;
        }
    }

    protected boolean requestCasSessionInternal(CasSessionRequest request, int[] casSessionHandle) {
        if (DEBUG) {
            Slog.d(TAG, "requestCasSession(request=" + request + ")");
        }
        CasResource cas = getCasResource(request.casSystemId);
        if (cas == null) {
            cas = new CasResource.Builder(request.casSystemId).maxSessionNum(Integer.MAX_VALUE).build();
            addCasResource(cas);
        }
        casSessionHandle[0] = -1;
        ClientProfile requestClient = getClientProfile(request.clientId);
        clientPriorityUpdateOnRequest(requestClient);
        int lowestPriorityOwnerId = -1;
        int currentLowestPriority = 1001;
        if (!cas.isFullyUsed()) {
            casSessionHandle[0] = generateResourceHandle(4, cas.getSystemId());
            updateCasClientMappingOnNewGrant(request.casSystemId, request.clientId);
            return true;
        }
        for (Integer num : cas.getOwnerClientIds()) {
            int ownerId = num.intValue();
            int priority = updateAndGetOwnerClientPriority(ownerId);
            if (currentLowestPriority > priority) {
                lowestPriorityOwnerId = ownerId;
                currentLowestPriority = priority;
            }
        }
        if (lowestPriorityOwnerId <= -1 || requestClient.getPriority() <= currentLowestPriority || !reclaimResource(lowestPriorityOwnerId, 4)) {
            return false;
        }
        casSessionHandle[0] = generateResourceHandle(4, cas.getSystemId());
        updateCasClientMappingOnNewGrant(request.casSystemId, request.clientId);
        return true;
    }

    protected boolean requestCiCamInternal(TunerCiCamRequest request, int[] ciCamHandle) {
        if (DEBUG) {
            Slog.d(TAG, "requestCiCamInternal(TunerCiCamRequest=" + request + ")");
        }
        CiCamResource ciCam = getCiCamResource(request.ciCamId);
        if (ciCam == null) {
            ciCam = new CiCamResource.Builder(request.ciCamId).maxSessionNum(Integer.MAX_VALUE).build();
            addCiCamResource(ciCam);
        }
        ciCamHandle[0] = -1;
        ClientProfile requestClient = getClientProfile(request.clientId);
        clientPriorityUpdateOnRequest(requestClient);
        int lowestPriorityOwnerId = -1;
        int currentLowestPriority = 1001;
        if (!ciCam.isFullyUsed()) {
            ciCamHandle[0] = generateResourceHandle(5, ciCam.getCiCamId());
            updateCiCamClientMappingOnNewGrant(request.ciCamId, request.clientId);
            return true;
        }
        for (Integer num : ciCam.getOwnerClientIds()) {
            int ownerId = num.intValue();
            int priority = updateAndGetOwnerClientPriority(ownerId);
            if (currentLowestPriority > priority) {
                lowestPriorityOwnerId = ownerId;
                currentLowestPriority = priority;
            }
        }
        if (lowestPriorityOwnerId <= -1 || requestClient.getPriority() <= currentLowestPriority || !reclaimResource(lowestPriorityOwnerId, 5)) {
            return false;
        }
        ciCamHandle[0] = generateResourceHandle(5, ciCam.getCiCamId());
        updateCiCamClientMappingOnNewGrant(request.ciCamId, request.clientId);
        return true;
    }

    protected boolean isHigherPriorityInternal(ResourceClientProfile challengerProfile, ResourceClientProfile holderProfile) {
        int challengerPid;
        int holderPid;
        if (DEBUG) {
            Slog.d(TAG, "isHigherPriority(challengerProfile=" + challengerProfile + ", holderProfile=" + challengerProfile + ")");
        }
        if (this.mTvInputManager == null) {
            Slog.e(TAG, "TvInputManager is null. Can't compare the priority.");
            return true;
        }
        if (challengerProfile.tvInputSessionId == null) {
            challengerPid = Binder.getCallingPid();
        } else {
            challengerPid = this.mTvInputManager.getClientPid(challengerProfile.tvInputSessionId);
        }
        if (holderProfile.tvInputSessionId == null) {
            holderPid = Binder.getCallingPid();
        } else {
            holderPid = this.mTvInputManager.getClientPid(holderProfile.tvInputSessionId);
        }
        int challengerPriority = getClientPriority(challengerProfile.useCase, checkIsForeground(challengerPid));
        int holderPriority = getClientPriority(holderProfile.useCase, checkIsForeground(holderPid));
        return challengerPriority > holderPriority;
    }

    protected void releaseFrontendInternal(FrontendResource fe, int clientId) {
        ClientProfile ownerClient;
        if (DEBUG) {
            Slog.d(TAG, "releaseFrontend(id=" + fe.getHandle() + ", clientId=" + clientId + " )");
        }
        if (clientId == fe.getOwnerClientId() && (ownerClient = getClientProfile(fe.getOwnerClientId())) != null) {
            for (Integer num : ownerClient.getShareFeClientIds()) {
                int shareOwnerId = num.intValue();
                reclaimResource(shareOwnerId, 0);
            }
        }
        clearFrontendAndClientMapping(getClientProfile(clientId));
    }

    protected void releaseLnbInternal(LnbResource lnb) {
        if (DEBUG) {
            Slog.d(TAG, "releaseLnb(lnbHandle=" + lnb.getHandle() + ")");
        }
        updateLnbClientMappingOnRelease(lnb);
    }

    protected void releaseCasSessionInternal(CasResource cas, int ownerClientId) {
        if (DEBUG) {
            Slog.d(TAG, "releaseCasSession(sessionResourceId=" + cas.getSystemId() + ")");
        }
        updateCasClientMappingOnRelease(cas, ownerClientId);
    }

    protected void releaseCiCamInternal(CiCamResource ciCam, int ownerClientId) {
        if (DEBUG) {
            Slog.d(TAG, "releaseCiCamInternal(ciCamId=" + ciCam.getCiCamId() + ")");
        }
        updateCiCamClientMappingOnRelease(ciCam, ownerClientId);
    }

    protected boolean requestDemuxInternal(TunerDemuxRequest request, int[] demuxHandle) {
        if (DEBUG) {
            Slog.d(TAG, "requestDemux(request=" + request + ")");
        }
        demuxHandle[0] = generateResourceHandle(1, 0);
        return true;
    }

    protected void clientPriorityUpdateOnRequest(ClientProfile profile) {
        if (profile.isPriorityOverwritten()) {
            return;
        }
        int pid = profile.getProcessId();
        boolean currentIsForeground = checkIsForeground(pid);
        profile.setPriority(getClientPriority(profile.getUseCase(), currentIsForeground));
    }

    protected boolean requestDescramblerInternal(TunerDescramblerRequest request, int[] descramblerHandle) {
        if (DEBUG) {
            Slog.d(TAG, "requestDescrambler(request=" + request + ")");
        }
        descramblerHandle[0] = generateResourceHandle(2, 0);
        return true;
    }

    private long getElapsedTime(long begin) {
        long now = SystemClock.uptimeMillis();
        if (now >= begin) {
            return now - begin;
        }
        long elapsed = (JobStatus.NO_LATEST_RUNTIME - begin) + now;
        if (elapsed < 0) {
            return JobStatus.NO_LATEST_RUNTIME;
        }
        return elapsed;
    }

    private boolean lockForTunerApiLock(int clientId, long timeoutMS, String callerFunction) {
        try {
            if (!this.mLockForTRMSLock.tryLock(timeoutMS, TimeUnit.MILLISECONDS)) {
                Slog.e(TAG, "FAILED to lock mLockForTRMSLock in " + callerFunction + ", clientId:" + clientId + ", timeoutMS:" + timeoutMS + ", mTunerApiLockHolder:" + this.mTunerApiLockHolder);
                return false;
            }
            return true;
        } catch (InterruptedException ie) {
            Slog.e(TAG, "exception thrown in " + callerFunction + ":" + ie);
            if (this.mLockForTRMSLock.isHeldByCurrentThread()) {
                this.mLockForTRMSLock.unlock();
            }
            return false;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1459=5] */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:44:0x0108 A[Catch: InterruptedException -> 0x01e5, all -> 0x022e, TryCatch #0 {InterruptedException -> 0x01e5, blocks: (B:29:0x0050, B:44:0x0108, B:46:0x0112, B:48:0x0148, B:49:0x0174, B:51:0x0178, B:52:0x01a4, B:54:0x01a8, B:55:0x01c6, B:30:0x0098, B:36:0x00ad), top: B:83:0x0098 }] */
    /* JADX WARN: Removed duplicated region for block: B:48:0x0148 A[Catch: InterruptedException -> 0x01e5, all -> 0x022e, TryCatch #0 {InterruptedException -> 0x01e5, blocks: (B:29:0x0050, B:44:0x0108, B:46:0x0112, B:48:0x0148, B:49:0x0174, B:51:0x0178, B:52:0x01a4, B:54:0x01a8, B:55:0x01c6, B:30:0x0098, B:36:0x00ad), top: B:83:0x0098 }] */
    /* JADX WARN: Removed duplicated region for block: B:51:0x0178 A[Catch: InterruptedException -> 0x01e5, all -> 0x022e, TryCatch #0 {InterruptedException -> 0x01e5, blocks: (B:29:0x0050, B:44:0x0108, B:46:0x0112, B:48:0x0148, B:49:0x0174, B:51:0x0178, B:52:0x01a4, B:54:0x01a8, B:55:0x01c6, B:30:0x0098, B:36:0x00ad), top: B:83:0x0098 }] */
    /* JADX WARN: Removed duplicated region for block: B:54:0x01a8 A[Catch: InterruptedException -> 0x01e5, all -> 0x022e, TryCatch #0 {InterruptedException -> 0x01e5, blocks: (B:29:0x0050, B:44:0x0108, B:46:0x0112, B:48:0x0148, B:49:0x0174, B:51:0x0178, B:52:0x01a4, B:54:0x01a8, B:55:0x01c6, B:30:0x0098, B:36:0x00ad), top: B:83:0x0098 }] */
    /* JADX WARN: Removed duplicated region for block: B:57:0x01cf A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:64:0x01df  */
    /* JADX WARN: Removed duplicated region for block: B:75:0x0227  */
    /* JADX WARN: Removed duplicated region for block: B:81:0x0237  */
    /* JADX WARN: Removed duplicated region for block: B:83:0x0098 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:88:0x0050 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:93:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean acquireLockInternal(int clientId, long clientThreadId, long timeoutMS) {
        boolean available;
        boolean nestedSelf;
        boolean nestedSelf2;
        boolean recovery;
        boolean z;
        long leftOverMS;
        boolean available2;
        long begin = SystemClock.uptimeMillis();
        if (!lockForTunerApiLock(clientId, timeoutMS, "acquireLockInternal()")) {
            return false;
        }
        try {
            int i = this.mTunerApiLockHolder;
            boolean available3 = i == -1;
            if (clientId == i) {
                available = available3;
                try {
                    if (clientThreadId == this.mTunerApiLockHolderThreadId) {
                        nestedSelf = true;
                        boolean recovery2 = false;
                        while (!available && !nestedSelf) {
                            leftOverMS = timeoutMS - getElapsedTime(begin);
                            long begin2 = begin;
                            if (leftOverMS > 0) {
                                nestedSelf2 = nestedSelf;
                                Slog.e(TAG, "FAILED:acquireLockInternal(" + clientId + ", " + clientThreadId + ", " + timeoutMS + ") - timed out, but will grant the lock to the callee by stealing it from the current holder:" + this.mTunerApiLockHolder + "(" + this.mTunerApiLockHolderThreadId + "), who likely failed to call releaseLock(), to prevent this from becoming an unrecoverable error");
                                recovery = true;
                                break;
                            }
                            try {
                                try {
                                    boolean nestedSelf3 = nestedSelf;
                                    boolean recovery3 = recovery2;
                                    this.mTunerApiLockReleasedCV.await(leftOverMS, TimeUnit.MILLISECONDS);
                                    boolean available4 = this.mTunerApiLockHolder == -1;
                                    if (available4) {
                                        available2 = available4;
                                    } else {
                                        available2 = available4;
                                        Slog.w(TAG, "acquireLockInternal(" + clientId + ", " + clientThreadId + ", " + timeoutMS + ") - woken up from cond wait, but " + this.mTunerApiLockHolder + "(" + this.mTunerApiLockHolderThreadId + ") is already holding the lock. Going to wait again if timeout hasn't reached yet");
                                    }
                                    available = available2;
                                    nestedSelf = nestedSelf3;
                                    recovery2 = recovery3;
                                    begin = begin2;
                                } catch (InterruptedException e) {
                                    ie = e;
                                    Slog.e(TAG, "exception thrown in acquireLockInternal(" + clientId + ", " + clientThreadId + ", " + timeoutMS + "):" + ie);
                                    if (this.mLockForTRMSLock.isHeldByCurrentThread()) {
                                        return false;
                                    }
                                    this.mLockForTRMSLock.unlock();
                                    return false;
                                }
                            } catch (Throwable th) {
                                ie = th;
                                if (this.mLockForTRMSLock.isHeldByCurrentThread()) {
                                    this.mLockForTRMSLock.unlock();
                                }
                                throw ie;
                            }
                        }
                        nestedSelf2 = nestedSelf;
                        recovery = recovery2;
                        if (!available && !recovery) {
                            if (nestedSelf2) {
                                Slog.e(TAG, "acquireLockInternal(" + clientId + ", " + clientThreadId + ", " + timeoutMS + ") - should not reach here");
                                z = true;
                            } else {
                                this.mTunerApiLockNestedCount++;
                                if (DEBUG) {
                                    Slog.d(TAG, "acquireLockInternal(" + clientId + ", " + clientThreadId + ", " + timeoutMS + ") - nested count incremented to " + this.mTunerApiLockNestedCount);
                                    z = true;
                                } else {
                                    z = true;
                                }
                            }
                            boolean z2 = (!available || nestedSelf2 || recovery) ? z : false;
                            if (this.mLockForTRMSLock.isHeldByCurrentThread()) {
                                this.mLockForTRMSLock.unlock();
                            }
                            return z2;
                        }
                        if (DEBUG) {
                            Slog.d(TAG, "SUCCESS:acquireLockInternal(" + clientId + ", " + clientThreadId + ", " + timeoutMS + ")");
                        }
                        if (this.mTunerApiLockNestedCount != 0) {
                            Slog.w(TAG, "Something is wrong as nestedCount(" + this.mTunerApiLockNestedCount + ") is not zero. Will overriding it to 1 anyways");
                        }
                        this.mTunerApiLockHolder = clientId;
                        this.mTunerApiLockHolderThreadId = clientThreadId;
                        z = true;
                        this.mTunerApiLockNestedCount = 1;
                        if (available) {
                        }
                        if (this.mLockForTRMSLock.isHeldByCurrentThread()) {
                        }
                        return z2;
                    }
                } catch (InterruptedException e2) {
                    ie = e2;
                    Slog.e(TAG, "exception thrown in acquireLockInternal(" + clientId + ", " + clientThreadId + ", " + timeoutMS + "):" + ie);
                    if (this.mLockForTRMSLock.isHeldByCurrentThread()) {
                    }
                } catch (Throwable th2) {
                    ie = th2;
                    if (this.mLockForTRMSLock.isHeldByCurrentThread()) {
                    }
                    throw ie;
                }
            } else {
                available = available3;
            }
            nestedSelf = false;
            boolean recovery22 = false;
            while (!available) {
                leftOverMS = timeoutMS - getElapsedTime(begin);
                long begin22 = begin;
                if (leftOverMS > 0) {
                }
            }
            nestedSelf2 = nestedSelf;
            recovery = recovery22;
            if (!available) {
                if (nestedSelf2) {
                }
                if (available) {
                }
                if (this.mLockForTRMSLock.isHeldByCurrentThread()) {
                }
                return z2;
            }
            if (DEBUG) {
            }
            if (this.mTunerApiLockNestedCount != 0) {
            }
            this.mTunerApiLockHolder = clientId;
            this.mTunerApiLockHolderThreadId = clientThreadId;
            z = true;
            this.mTunerApiLockNestedCount = 1;
            if (available) {
            }
            if (this.mLockForTRMSLock.isHeldByCurrentThread()) {
            }
            return z2;
        } catch (InterruptedException e3) {
            ie = e3;
        } catch (Throwable th3) {
            ie = th3;
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IGET, INVOKE]}, finally: {[IGET, INVOKE, IGET, INVOKE, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1515=4, 1516=4] */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:21:0x00b3 A[DONT_GENERATE] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean releaseLockInternal(int clientId, long timeoutMS, boolean ignoreNestedCount, boolean suppressError) {
        if (lockForTunerApiLock(clientId, timeoutMS, "releaseLockInternal()")) {
            try {
                int i = this.mTunerApiLockHolder;
                if (i != clientId) {
                    if (i == -1) {
                        if (!suppressError) {
                            Slog.w(TAG, "releaseLockInternal(" + clientId + ", " + timeoutMS + ") - called while there is no current holder");
                        }
                        if (this.mLockForTRMSLock.isHeldByCurrentThread()) {
                            this.mLockForTRMSLock.unlock();
                        }
                        return false;
                    }
                    if (!suppressError) {
                        Slog.e(TAG, "releaseLockInternal(" + clientId + ", " + timeoutMS + ") - called while someone else:" + this.mTunerApiLockHolder + "is the current holder");
                    }
                    if (this.mLockForTRMSLock.isHeldByCurrentThread()) {
                        this.mLockForTRMSLock.unlock();
                    }
                    return false;
                }
                int i2 = this.mTunerApiLockNestedCount - 1;
                this.mTunerApiLockNestedCount = i2;
                if (!ignoreNestedCount && i2 > 0) {
                    if (DEBUG) {
                        Slog.d(TAG, "releaseLockInternal(" + clientId + ", " + timeoutMS + ", " + ignoreNestedCount + ", " + suppressError + ") - NOT signaling because nested count is not zero (" + this.mTunerApiLockNestedCount + ")");
                    }
                    return true;
                }
                if (DEBUG) {
                    Slog.d(TAG, "SUCCESS:releaseLockInternal(" + clientId + ", " + timeoutMS + ", " + ignoreNestedCount + ", " + suppressError + ") - signaling!");
                }
                this.mTunerApiLockHolder = -1;
                this.mTunerApiLockHolderThreadId = -1L;
                this.mTunerApiLockNestedCount = 0;
                this.mTunerApiLockReleasedCV.signal();
                return true;
            } finally {
                if (this.mLockForTRMSLock.isHeldByCurrentThread()) {
                    this.mLockForTRMSLock.unlock();
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public class ResourcesReclaimListenerRecord implements IBinder.DeathRecipient {
        private final int mClientId;
        private final IResourcesReclaimListener mListener;

        public ResourcesReclaimListenerRecord(IResourcesReclaimListener listener, int clientId) {
            this.mListener = listener;
            this.mClientId = clientId;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            try {
                synchronized (TunerResourceManagerService.this.mLock) {
                    if (TunerResourceManagerService.this.checkClientExists(this.mClientId)) {
                        TunerResourceManagerService.this.removeClientProfile(this.mClientId);
                    }
                }
            } finally {
                TunerResourceManagerService.this.releaseLockInternal(this.mClientId, 500L, true, true);
            }
        }

        public int getId() {
            return this.mClientId;
        }

        public IResourcesReclaimListener getListener() {
            return this.mListener;
        }
    }

    private void addResourcesReclaimListener(int clientId, IResourcesReclaimListener listener) {
        if (listener == null) {
            if (DEBUG) {
                Slog.w(TAG, "Listener is null when client " + clientId + " registered!");
                return;
            }
            return;
        }
        ResourcesReclaimListenerRecord record = new ResourcesReclaimListenerRecord(listener, clientId);
        try {
            listener.asBinder().linkToDeath(record, 0);
            this.mListeners.put(Integer.valueOf(clientId), record);
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died.");
        }
    }

    protected boolean reclaimResource(int reclaimingClientId, int resourceType) {
        Binder.allowBlockingForCurrentThread();
        ClientProfile profile = getClientProfile(reclaimingClientId);
        if (profile == null) {
            return true;
        }
        Set<Integer> shareFeClientIds = profile.getShareFeClientIds();
        for (Integer num : shareFeClientIds) {
            int clientId = num.intValue();
            try {
                this.mListeners.get(Integer.valueOf(clientId)).getListener().onReclaimResources();
                clearAllResourcesAndClientMapping(getClientProfile(clientId));
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to reclaim resources on client " + clientId, e);
                return false;
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "Reclaiming resources because higher priority client request resource type " + resourceType + ", clientId:" + reclaimingClientId);
        }
        try {
            this.mListeners.get(Integer.valueOf(reclaimingClientId)).getListener().onReclaimResources();
            clearAllResourcesAndClientMapping(profile);
            return true;
        } catch (RemoteException e2) {
            Slog.e(TAG, "Failed to reclaim resources on client " + reclaimingClientId, e2);
            return false;
        }
    }

    protected int getClientPriority(int useCase, boolean isForeground) {
        if (DEBUG) {
            Slog.d(TAG, "getClientPriority useCase=" + useCase + ", isForeground=" + isForeground + ")");
        }
        if (isForeground) {
            return this.mPriorityCongfig.getForegroundPriority(useCase);
        }
        return this.mPriorityCongfig.getBackgroundPriority(useCase);
    }

    protected boolean checkIsForeground(int pid) {
        List<ActivityManager.RunningAppProcessInfo> appProcesses;
        ActivityManager activityManager = this.mActivityManager;
        if (activityManager == null || (appProcesses = activityManager.getRunningAppProcesses()) == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.pid == pid && appProcess.importance == 100) {
                return true;
            }
        }
        return false;
    }

    private void updateFrontendClientMappingOnNewGrant(int grantingHandle, int ownerClientId) {
        FrontendResource grantingFrontend = getFrontendResource(grantingHandle);
        ClientProfile ownerProfile = getClientProfile(ownerClientId);
        grantingFrontend.setOwner(ownerClientId);
        increFrontendNum(this.mFrontendUsedNums, grantingFrontend.getType());
        ownerProfile.useFrontend(grantingHandle);
        for (Integer num : grantingFrontend.getExclusiveGroupMemberFeHandles()) {
            int exclusiveGroupMember = num.intValue();
            getFrontendResource(exclusiveGroupMember).setOwner(ownerClientId);
            ownerProfile.useFrontend(exclusiveGroupMember);
        }
        ownerProfile.setPrimaryFrontend(grantingHandle);
    }

    private void updateLnbClientMappingOnNewGrant(int grantingHandle, int ownerClientId) {
        LnbResource grantingLnb = getLnbResource(grantingHandle);
        ClientProfile ownerProfile = getClientProfile(ownerClientId);
        grantingLnb.setOwner(ownerClientId);
        ownerProfile.useLnb(grantingHandle);
    }

    private void updateLnbClientMappingOnRelease(LnbResource releasingLnb) {
        ClientProfile ownerProfile = getClientProfile(releasingLnb.getOwnerClientId());
        releasingLnb.removeOwner();
        ownerProfile.releaseLnb(releasingLnb.getHandle());
    }

    private void updateCasClientMappingOnNewGrant(int grantingId, int ownerClientId) {
        CasResource grantingCas = getCasResource(grantingId);
        ClientProfile ownerProfile = getClientProfile(ownerClientId);
        grantingCas.setOwner(ownerClientId);
        ownerProfile.useCas(grantingId);
    }

    private void updateCiCamClientMappingOnNewGrant(int grantingId, int ownerClientId) {
        CiCamResource grantingCiCam = getCiCamResource(grantingId);
        ClientProfile ownerProfile = getClientProfile(ownerClientId);
        grantingCiCam.setOwner(ownerClientId);
        ownerProfile.useCiCam(grantingId);
    }

    private void updateCasClientMappingOnRelease(CasResource releasingCas, int ownerClientId) {
        ClientProfile ownerProfile = getClientProfile(ownerClientId);
        releasingCas.removeOwner(ownerClientId);
        ownerProfile.releaseCas();
    }

    private void updateCiCamClientMappingOnRelease(CiCamResource releasingCiCam, int ownerClientId) {
        ClientProfile ownerProfile = getClientProfile(ownerClientId);
        releasingCiCam.removeOwner(ownerClientId);
        ownerProfile.releaseCiCam();
    }

    private int updateAndGetOwnerClientPriority(int clientId) {
        ClientProfile profile = getClientProfile(clientId);
        clientPriorityUpdateOnRequest(profile);
        return profile.getPriority();
    }

    private int getFrontendHighestClientPriority(int clientId) {
        ClientProfile ownerClient = getClientProfile(clientId);
        if (ownerClient == null) {
            return 0;
        }
        int highestPriority = updateAndGetOwnerClientPriority(clientId);
        for (Integer num : ownerClient.getShareFeClientIds()) {
            int shareeId = num.intValue();
            int priority = updateAndGetOwnerClientPriority(shareeId);
            if (priority > highestPriority) {
                highestPriority = priority;
            }
        }
        return highestPriority;
    }

    protected FrontendResource getFrontendResource(int frontendHandle) {
        return this.mFrontendResources.get(Integer.valueOf(frontendHandle));
    }

    protected Map<Integer, FrontendResource> getFrontendResources() {
        return this.mFrontendResources;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setMaxNumberOfFrontendsInternal(int frontendType, int maxUsableNum) {
        int usedNum = this.mFrontendUsedNums.get(frontendType, -1);
        if (usedNum == -1 || usedNum <= maxUsableNum) {
            this.mFrontendMaxUsableNums.put(frontendType, maxUsableNum);
            return true;
        }
        Slog.e(TAG, "max number of frontend for frontendType: " + frontendType + " cannot be set to a value lower than the current usage count. (requested max num = " + maxUsableNum + ", current usage = " + usedNum);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getMaxNumberOfFrontendsInternal(int frontendType) {
        int existingNum = this.mFrontendExistingNums.get(frontendType, -1);
        if (existingNum == -1) {
            Log.e(TAG, "existingNum is -1 for " + frontendType);
            return -1;
        }
        int maxUsableNum = this.mFrontendMaxUsableNums.get(frontendType, -1);
        if (maxUsableNum == -1) {
            return existingNum;
        }
        return maxUsableNum;
    }

    private boolean isFrontendMaxNumUseReached(int frontendType) {
        int maxUsableNum = this.mFrontendMaxUsableNums.get(frontendType, -1);
        if (maxUsableNum == -1) {
            return false;
        }
        int useNum = this.mFrontendUsedNums.get(frontendType, -1);
        if (useNum == -1) {
            useNum = 0;
        }
        return useNum >= maxUsableNum;
    }

    private void increFrontendNum(SparseIntArray targetNums, int frontendType) {
        int num = targetNums.get(frontendType, -1);
        if (num == -1) {
            targetNums.put(frontendType, 1);
        } else {
            targetNums.put(frontendType, num + 1);
        }
    }

    private void decreFrontendNum(SparseIntArray targetNums, int frontendType) {
        int num = targetNums.get(frontendType, -1);
        if (num != -1) {
            targetNums.put(frontendType, num - 1);
        }
    }

    private void replaceFeResourceMap(Map<Integer, FrontendResource> srcMap, Map<Integer, FrontendResource> dstMap) {
        if (dstMap != null) {
            dstMap.clear();
            if (srcMap != null && srcMap.size() > 0) {
                dstMap.putAll(srcMap);
            }
        }
    }

    private void replaceFeCounts(SparseIntArray srcCounts, SparseIntArray dstCounts) {
        if (dstCounts != null) {
            dstCounts.clear();
            if (srcCounts != null) {
                for (int i = 0; i < srcCounts.size(); i++) {
                    dstCounts.put(srcCounts.keyAt(i), srcCounts.valueAt(i));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpMap(Map<?, ?> targetMap, String headline, String delimiter, IndentingPrintWriter pw) {
        if (targetMap != null) {
            pw.println(headline);
            pw.increaseIndent();
            for (Map.Entry<?, ?> entry : targetMap.entrySet()) {
                pw.print(entry.getKey() + " : " + entry.getValue());
                pw.print(delimiter);
            }
            pw.println();
            pw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpSIA(SparseIntArray array, String headline, String delimiter, IndentingPrintWriter pw) {
        if (array != null) {
            pw.println(headline);
            pw.increaseIndent();
            for (int i = 0; i < array.size(); i++) {
                pw.print(array.keyAt(i) + " : " + array.valueAt(i));
                pw.print(delimiter);
            }
            pw.println();
            pw.decreaseIndent();
        }
    }

    private void addFrontendResource(FrontendResource newFe) {
        Iterator<FrontendResource> it = getFrontendResources().values().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            FrontendResource fe = it.next();
            if (fe.getExclusiveGroupId() == newFe.getExclusiveGroupId()) {
                newFe.addExclusiveGroupMemberFeHandle(fe.getHandle());
                newFe.addExclusiveGroupMemberFeHandles(fe.getExclusiveGroupMemberFeHandles());
                for (Integer num : fe.getExclusiveGroupMemberFeHandles()) {
                    int excGroupmemberFeHandle = num.intValue();
                    getFrontendResource(excGroupmemberFeHandle).addExclusiveGroupMemberFeHandle(newFe.getHandle());
                }
                fe.addExclusiveGroupMemberFeHandle(newFe.getHandle());
            }
        }
        this.mFrontendResources.put(Integer.valueOf(newFe.getHandle()), newFe);
        increFrontendNum(this.mFrontendExistingNums, newFe.getType());
    }

    private void removeFrontendResource(int removingHandle) {
        FrontendResource fe = getFrontendResource(removingHandle);
        if (fe == null) {
            return;
        }
        if (fe.isInUse()) {
            ClientProfile ownerClient = getClientProfile(fe.getOwnerClientId());
            for (Integer num : ownerClient.getShareFeClientIds()) {
                int shareOwnerId = num.intValue();
                clearFrontendAndClientMapping(getClientProfile(shareOwnerId));
            }
            clearFrontendAndClientMapping(ownerClient);
        }
        for (Integer num2 : fe.getExclusiveGroupMemberFeHandles()) {
            int excGroupmemberFeHandle = num2.intValue();
            getFrontendResource(excGroupmemberFeHandle).removeExclusiveGroupMemberFeId(fe.getHandle());
        }
        decreFrontendNum(this.mFrontendExistingNums, fe.getType());
        this.mFrontendResources.remove(Integer.valueOf(removingHandle));
    }

    protected LnbResource getLnbResource(int lnbHandle) {
        return this.mLnbResources.get(Integer.valueOf(lnbHandle));
    }

    protected Map<Integer, LnbResource> getLnbResources() {
        return this.mLnbResources;
    }

    private void addLnbResource(LnbResource newLnb) {
        this.mLnbResources.put(Integer.valueOf(newLnb.getHandle()), newLnb);
    }

    private void removeLnbResource(int removingHandle) {
        LnbResource lnb = getLnbResource(removingHandle);
        if (lnb == null) {
            return;
        }
        if (lnb.isInUse()) {
            releaseLnbInternal(lnb);
        }
        this.mLnbResources.remove(Integer.valueOf(removingHandle));
    }

    protected CasResource getCasResource(int systemId) {
        return this.mCasResources.get(Integer.valueOf(systemId));
    }

    protected CiCamResource getCiCamResource(int ciCamId) {
        return this.mCiCamResources.get(Integer.valueOf(ciCamId));
    }

    protected Map<Integer, CasResource> getCasResources() {
        return this.mCasResources;
    }

    protected Map<Integer, CiCamResource> getCiCamResources() {
        return this.mCiCamResources;
    }

    private void addCasResource(CasResource newCas) {
        this.mCasResources.put(Integer.valueOf(newCas.getSystemId()), newCas);
    }

    private void addCiCamResource(CiCamResource newCiCam) {
        this.mCiCamResources.put(Integer.valueOf(newCiCam.getCiCamId()), newCiCam);
    }

    private void removeCasResource(int removingId) {
        CasResource cas = getCasResource(removingId);
        if (cas == null) {
            return;
        }
        for (Integer num : cas.getOwnerClientIds()) {
            int ownerId = num.intValue();
            getClientProfile(ownerId).releaseCas();
        }
        this.mCasResources.remove(Integer.valueOf(removingId));
    }

    private void removeCiCamResource(int removingId) {
        CiCamResource ciCam = getCiCamResource(removingId);
        if (ciCam == null) {
            return;
        }
        for (Integer num : ciCam.getOwnerClientIds()) {
            int ownerId = num.intValue();
            getClientProfile(ownerId).releaseCiCam();
        }
        this.mCiCamResources.remove(Integer.valueOf(removingId));
    }

    private void releaseLowerPriorityClientCasResources(int releasingCasResourceNum) {
    }

    protected ClientProfile getClientProfile(int clientId) {
        return this.mClientProfiles.get(Integer.valueOf(clientId));
    }

    private void addClientProfile(int clientId, ClientProfile profile, IResourcesReclaimListener listener) {
        this.mClientProfiles.put(Integer.valueOf(clientId), profile);
        addResourcesReclaimListener(clientId, listener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeClientProfile(int clientId) {
        for (Integer num : getClientProfile(clientId).getShareFeClientIds()) {
            int shareOwnerId = num.intValue();
            clearFrontendAndClientMapping(getClientProfile(shareOwnerId));
        }
        clearAllResourcesAndClientMapping(getClientProfile(clientId));
        this.mClientProfiles.remove(Integer.valueOf(clientId));
        this.mListeners.remove(Integer.valueOf(clientId));
    }

    private void clearFrontendAndClientMapping(ClientProfile profile) {
        FrontendResource primaryFe;
        if (profile == null) {
            return;
        }
        for (Integer feId : profile.getInUseFrontendHandles()) {
            FrontendResource fe = getFrontendResource(feId.intValue());
            int ownerClientId = fe.getOwnerClientId();
            if (ownerClientId == profile.getId()) {
                fe.removeOwner();
            } else {
                ClientProfile ownerClientProfile = getClientProfile(ownerClientId);
                if (ownerClientProfile != null) {
                    ownerClientProfile.stopSharingFrontend(profile.getId());
                }
            }
        }
        int primaryFeId = profile.getPrimaryFrontend();
        if (primaryFeId != -1 && (primaryFe = getFrontendResource(primaryFeId)) != null) {
            decreFrontendNum(this.mFrontendUsedNums, primaryFe.getType());
        }
        profile.releaseFrontend();
    }

    private void clearAllResourcesAndClientMapping(ClientProfile profile) {
        if (profile == null) {
            return;
        }
        for (Integer lnbHandle : profile.getInUseLnbHandles()) {
            getLnbResource(lnbHandle.intValue()).removeOwner();
        }
        if (profile.getInUseCasSystemId() != -1) {
            getCasResource(profile.getInUseCasSystemId()).removeOwner(profile.getId());
        }
        if (profile.getInUseCiCamId() != -1) {
            getCiCamResource(profile.getInUseCiCamId()).removeOwner(profile.getId());
        }
        clearFrontendAndClientMapping(profile);
        profile.reclaimAllResources();
    }

    protected boolean checkClientExists(int clientId) {
        return this.mClientProfiles.keySet().contains(Integer.valueOf(clientId));
    }

    private int generateResourceHandle(int resourceType, int resourceId) {
        int i = ((resourceType & 255) << 24) | (resourceId << 16);
        int i2 = this.mResourceRequestCount;
        this.mResourceRequestCount = i2 + 1;
        return i | (i2 & GnssNative.GNSS_AIDING_TYPE_ALL);
    }

    protected int getResourceIdFromHandle(int resourceHandle) {
        if (resourceHandle == -1) {
            return resourceHandle;
        }
        return (16711680 & resourceHandle) >> 16;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean validateResourceHandle(int resourceType, int resourceHandle) {
        if (resourceHandle == -1 || (((-16777216) & resourceHandle) >> 24) != resourceType) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceTrmAccessPermission(String apiName) {
        getContext().enforceCallingOrSelfPermission("android.permission.TUNER_RESOURCE_ACCESS", "TunerResourceManagerService: " + apiName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceTunerAccessPermission(String apiName) {
        getContext().enforceCallingPermission("android.permission.ACCESS_TV_TUNER", "TunerResourceManagerService: " + apiName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceDescramblerAccessPermission(String apiName) {
        getContext().enforceCallingPermission("android.permission.ACCESS_TV_DESCRAMBLER", "TunerResourceManagerService: " + apiName);
    }
}
