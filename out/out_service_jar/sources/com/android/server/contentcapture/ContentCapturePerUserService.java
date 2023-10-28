package com.android.server.contentcapture;

import android.app.ActivityManagerInternal;
import android.app.assist.ActivityId;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.ComponentName;
import android.content.ContentCaptureOptions;
import android.content.pm.ActivityPresentationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.contentcapture.ActivityEvent;
import android.service.contentcapture.ContentCaptureService;
import android.service.contentcapture.ContentCaptureServiceInfo;
import android.service.contentcapture.FlushMetrics;
import android.service.contentcapture.IContentCaptureServiceCallback;
import android.service.contentcapture.IDataShareCallback;
import android.service.contentcapture.SnapshotData;
import android.service.voice.VoiceInteractionManagerInternal;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.contentcapture.ContentCaptureCondition;
import android.view.contentcapture.DataRemovalRequest;
import android.view.contentcapture.DataShareRequest;
import com.android.internal.os.IResultReceiver;
import com.android.server.LocalServices;
import com.android.server.contentcapture.RemoteContentCaptureService;
import com.android.server.infra.AbstractPerUserSystemService;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class ContentCapturePerUserService extends AbstractPerUserSystemService<ContentCapturePerUserService, ContentCaptureManagerService> implements RemoteContentCaptureService.ContentCaptureServiceCallbacks {
    private static final String TAG = ContentCapturePerUserService.class.getSimpleName();
    private final ArrayMap<String, ArraySet<ContentCaptureCondition>> mConditionsByPkg;
    private ContentCaptureServiceInfo mInfo;
    RemoteContentCaptureService mRemoteService;
    private final ContentCaptureServiceRemoteCallback mRemoteServiceCallback;
    private final SparseArray<ContentCaptureServerSession> mSessions;
    private boolean mZombie;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentCapturePerUserService(ContentCaptureManagerService master, Object lock, boolean disabled, int userId) {
        super(master, lock, userId);
        this.mSessions = new SparseArray<>();
        this.mRemoteServiceCallback = new ContentCaptureServiceRemoteCallback();
        this.mConditionsByPkg = new ArrayMap<>();
        updateRemoteServiceLocked(disabled);
    }

    private void updateRemoteServiceLocked(boolean disabled) {
        if (((ContentCaptureManagerService) this.mMaster).verbose) {
            Slog.v(TAG, "updateRemoteService(disabled=" + disabled + ")");
        }
        if (this.mRemoteService != null) {
            if (((ContentCaptureManagerService) this.mMaster).debug) {
                Slog.d(TAG, "updateRemoteService(): destroying old remote service");
            }
            this.mRemoteService.destroy();
            this.mRemoteService = null;
            resetContentCaptureWhitelistLocked();
        }
        ComponentName serviceComponentName = updateServiceInfoLocked();
        if (serviceComponentName == null) {
            if (((ContentCaptureManagerService) this.mMaster).debug) {
                Slog.d(TAG, "updateRemoteService(): no service component name");
            }
        } else if (!disabled) {
            if (((ContentCaptureManagerService) this.mMaster).debug) {
                Slog.d(TAG, "updateRemoteService(): creating new remote service for " + serviceComponentName);
            }
            this.mRemoteService = new RemoteContentCaptureService(((ContentCaptureManagerService) this.mMaster).getContext(), "android.service.contentcapture.ContentCaptureService", serviceComponentName, this.mRemoteServiceCallback, this.mUserId, this, ((ContentCaptureManagerService) this.mMaster).isBindInstantServiceAllowed(), ((ContentCaptureManagerService) this.mMaster).verbose, ((ContentCaptureManagerService) this.mMaster).mDevCfgIdleUnbindTimeoutMs);
        }
    }

    @Override // com.android.server.infra.AbstractPerUserSystemService
    protected ServiceInfo newServiceInfoLocked(ComponentName serviceComponent) throws PackageManager.NameNotFoundException {
        ContentCaptureServiceInfo contentCaptureServiceInfo = new ContentCaptureServiceInfo(getContext(), serviceComponent, isTemporaryServiceSetLocked(), this.mUserId);
        this.mInfo = contentCaptureServiceInfo;
        return contentCaptureServiceInfo.getServiceInfo();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractPerUserSystemService
    public boolean updateLocked(boolean disabled) {
        boolean disabledStateChanged = super.updateLocked(disabled);
        if (disabledStateChanged) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                this.mSessions.valueAt(i).setContentCaptureEnabledLocked(!disabled);
            }
        }
        destroyLocked();
        updateRemoteServiceLocked(disabled);
        return disabledStateChanged;
    }

    /* JADX DEBUG: Method merged with bridge method */
    public void onServiceDied(RemoteContentCaptureService service) {
        Slog.w(TAG, "remote service died: " + service);
        synchronized (this.mLock) {
            this.mZombie = true;
            ContentCaptureMetricsLogger.writeServiceEvent(16, getServiceComponentName());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onConnected() {
        synchronized (this.mLock) {
            if (this.mZombie) {
                if (this.mRemoteService == null) {
                    Slog.w(TAG, "Cannot ressurect sessions because remote service is null");
                } else {
                    this.mZombie = false;
                    resurrectSessionsLocked();
                }
            }
        }
    }

    private void resurrectSessionsLocked() {
        int numSessions = this.mSessions.size();
        if (((ContentCaptureManagerService) this.mMaster).debug) {
            Slog.d(TAG, "Ressurrecting remote service (" + this.mRemoteService + ") on " + numSessions + " sessions");
        }
        for (int i = 0; i < numSessions; i++) {
            ContentCaptureServerSession session = this.mSessions.valueAt(i);
            session.resurrectLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageUpdatingLocked() {
        int numSessions = this.mSessions.size();
        if (((ContentCaptureManagerService) this.mMaster).debug) {
            Slog.d(TAG, "Pausing " + numSessions + " sessions while package is updating");
        }
        for (int i = 0; i < numSessions; i++) {
            ContentCaptureServerSession session = this.mSessions.valueAt(i);
            session.pauseLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageUpdatedLocked() {
        updateRemoteServiceLocked(!isEnabledLocked());
        resurrectSessionsLocked();
    }

    public void startSessionLocked(IBinder activityToken, IBinder shareableActivityToken, ActivityPresentationInfo activityPresentationInfo, int sessionId, int uid, int flags, IResultReceiver clientReceiver) {
        if (activityPresentationInfo == null) {
            Slog.w(TAG, "basic activity info is null");
            ContentCaptureService.setClientState(clientReceiver, 260, (IBinder) null);
            return;
        }
        int taskId = activityPresentationInfo.taskId;
        int displayId = activityPresentationInfo.displayId;
        ComponentName componentName = activityPresentationInfo.componentName;
        boolean whiteListed = ((ContentCaptureManagerService) this.mMaster).mGlobalContentCaptureOptions.isWhitelisted(this.mUserId, componentName) || ((ContentCaptureManagerService) this.mMaster).mGlobalContentCaptureOptions.isWhitelisted(this.mUserId, componentName.getPackageName());
        ComponentName serviceComponentName = getServiceComponentName();
        boolean enabled = isEnabledLocked();
        if (((ContentCaptureManagerService) this.mMaster).mRequestsHistory != null) {
            String historyItem = "id=" + sessionId + " uid=" + uid + " a=" + ComponentName.flattenToShortString(componentName) + " t=" + taskId + " d=" + displayId + " s=" + ComponentName.flattenToShortString(serviceComponentName) + " u=" + this.mUserId + " f=" + flags + (enabled ? "" : " (disabled)") + " w=" + whiteListed;
            ((ContentCaptureManagerService) this.mMaster).mRequestsHistory.log(historyItem);
        }
        if (!enabled) {
            ContentCaptureService.setClientState(clientReceiver, 20, (IBinder) null);
            ContentCaptureMetricsLogger.writeSessionEvent(sessionId, 3, 20, serviceComponentName, false);
        } else if (serviceComponentName == null) {
            if (((ContentCaptureManagerService) this.mMaster).debug) {
                Slog.d(TAG, "startSession(" + activityToken + "): hold your horses");
            }
        } else if (!whiteListed) {
            if (((ContentCaptureManagerService) this.mMaster).debug) {
                Slog.d(TAG, "startSession(" + componentName + "): package or component not whitelisted");
            }
            ContentCaptureService.setClientState(clientReceiver, (int) UsbTerminalTypes.TERMINAL_IN_OMNI_MIC, (IBinder) null);
            ContentCaptureMetricsLogger.writeSessionEvent(sessionId, 3, UsbTerminalTypes.TERMINAL_IN_OMNI_MIC, serviceComponentName, false);
        } else {
            ContentCaptureServerSession existingSession = this.mSessions.get(sessionId);
            if (existingSession == null) {
                if (this.mRemoteService == null) {
                    updateRemoteServiceLocked(false);
                }
                RemoteContentCaptureService remoteContentCaptureService = this.mRemoteService;
                if (remoteContentCaptureService == null) {
                    Slog.w(TAG, "startSession(id=" + existingSession + ", token=" + activityToken + ": ignoring because service is not set");
                    ContentCaptureService.setClientState(clientReceiver, 20, (IBinder) null);
                    ContentCaptureMetricsLogger.writeSessionEvent(sessionId, 3, 20, serviceComponentName, false);
                    return;
                }
                remoteContentCaptureService.ensureBoundLocked();
                ContentCaptureServerSession newSession = new ContentCaptureServerSession(this.mLock, activityToken, new ActivityId(taskId, shareableActivityToken), this, componentName, clientReceiver, taskId, displayId, sessionId, uid, flags);
                if (((ContentCaptureManagerService) this.mMaster).verbose) {
                    Slog.v(TAG, "startSession(): new session for " + ComponentName.flattenToShortString(componentName) + " and id " + sessionId);
                }
                this.mSessions.put(sessionId, newSession);
                newSession.notifySessionStartedLocked(clientReceiver);
                return;
            }
            Slog.w(TAG, "startSession(id=" + existingSession + ", token=" + activityToken + ": ignoring because it already exists for " + existingSession.mActivityToken);
            ContentCaptureService.setClientState(clientReceiver, 12, (IBinder) null);
            ContentCaptureMetricsLogger.writeSessionEvent(sessionId, 3, 12, serviceComponentName, false);
        }
    }

    public void finishSessionLocked(int sessionId) {
        if (!isEnabledLocked()) {
            return;
        }
        ContentCaptureServerSession session = this.mSessions.get(sessionId);
        if (session == null) {
            if (((ContentCaptureManagerService) this.mMaster).debug) {
                Slog.d(TAG, "finishSession(): no session with id" + sessionId);
                return;
            }
            return;
        }
        if (((ContentCaptureManagerService) this.mMaster).verbose) {
            Slog.v(TAG, "finishSession(): id=" + sessionId);
        }
        session.removeSelfLocked(true);
    }

    public void removeDataLocked(DataRemovalRequest request) {
        if (!isEnabledLocked()) {
            return;
        }
        assertCallerLocked(request.getPackageName());
        this.mRemoteService.onDataRemovalRequest(request);
    }

    public void onDataSharedLocked(DataShareRequest request, IDataShareCallback.Stub dataShareCallback) {
        if (!isEnabledLocked()) {
            return;
        }
        assertCallerLocked(request.getPackageName());
        this.mRemoteService.onDataShareRequest(request, dataShareCallback);
    }

    public ComponentName getServiceSettingsActivityLocked() {
        String activityName;
        ContentCaptureServiceInfo contentCaptureServiceInfo = this.mInfo;
        if (contentCaptureServiceInfo == null || (activityName = contentCaptureServiceInfo.getSettingsActivity()) == null) {
            return null;
        }
        String packageName = this.mInfo.getServiceInfo().packageName;
        return new ComponentName(packageName, activityName);
    }

    private void assertCallerLocked(String packageName) {
        PackageManager pm = getContext().getPackageManager();
        int callingUid = Binder.getCallingUid();
        try {
            int packageUid = pm.getPackageUidAsUser(packageName, UserHandle.getCallingUserId());
            if (callingUid != packageUid && !((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).hasRunningActivity(callingUid, packageName)) {
                VoiceInteractionManagerInternal.HotwordDetectionServiceIdentity hotwordDetectionServiceIdentity = ((VoiceInteractionManagerInternal) LocalServices.getService(VoiceInteractionManagerInternal.class)).getHotwordDetectionServiceIdentity();
                boolean isHotwordDetectionServiceCall = hotwordDetectionServiceIdentity != null && callingUid == hotwordDetectionServiceIdentity.getIsolatedUid() && packageUid == hotwordDetectionServiceIdentity.getOwnerUid();
                if (!isHotwordDetectionServiceCall) {
                    String[] packages = pm.getPackagesForUid(callingUid);
                    String callingPackage = packages != null ? packages[0] : "uid-" + callingUid;
                    Slog.w(TAG, "App (package=" + callingPackage + ", UID=" + callingUid + ") passed package (" + packageName + ") owned by UID " + packageUid);
                    throw new SecurityException("Invalid package: " + packageName);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException("Could not verify UID for " + packageName);
        }
    }

    public boolean sendActivityAssistDataLocked(IBinder activityToken, Bundle data) {
        int id = getSessionId(activityToken);
        Bundle assistData = data.getBundle("data");
        AssistStructure assistStructure = (AssistStructure) data.getParcelable(ActivityTaskManagerInternal.ASSIST_KEY_STRUCTURE);
        AssistContent assistContent = (AssistContent) data.getParcelable(ActivityTaskManagerInternal.ASSIST_KEY_CONTENT);
        SnapshotData snapshotData = new SnapshotData(assistData, assistStructure, assistContent);
        if (id != 0) {
            ContentCaptureServerSession session = this.mSessions.get(id);
            session.sendActivitySnapshotLocked(snapshotData);
            return true;
        }
        RemoteContentCaptureService remoteContentCaptureService = this.mRemoteService;
        if (remoteContentCaptureService != null) {
            remoteContentCaptureService.onActivitySnapshotRequest(0, snapshotData);
            Slog.d(TAG, "Notified activity assist data for activity: " + activityToken + " without a session Id");
            return true;
        }
        return false;
    }

    public void removeSessionLocked(int sessionId) {
        this.mSessions.remove(sessionId);
    }

    public boolean isContentCaptureServiceForUserLocked(int uid) {
        return uid == getServiceUidLocked();
    }

    private ContentCaptureServerSession getSession(IBinder activityToken) {
        for (int i = 0; i < this.mSessions.size(); i++) {
            ContentCaptureServerSession session = this.mSessions.valueAt(i);
            if (session.mActivityToken.equals(activityToken)) {
                return session;
            }
        }
        return null;
    }

    public void destroyLocked() {
        if (((ContentCaptureManagerService) this.mMaster).debug) {
            Slog.d(TAG, "destroyLocked()");
        }
        RemoteContentCaptureService remoteContentCaptureService = this.mRemoteService;
        if (remoteContentCaptureService != null) {
            remoteContentCaptureService.destroy();
        }
        destroySessionsLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroySessionsLocked() {
        int numSessions = this.mSessions.size();
        for (int i = 0; i < numSessions; i++) {
            ContentCaptureServerSession session = this.mSessions.valueAt(i);
            session.destroyLocked(true);
        }
        this.mSessions.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void listSessionsLocked(ArrayList<String> output) {
        int numSessions = this.mSessions.size();
        for (int i = 0; i < numSessions; i++) {
            ContentCaptureServerSession session = this.mSessions.valueAt(i);
            output.add(session.toShortString());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArraySet<ContentCaptureCondition> getContentCaptureConditionsLocked(String packageName) {
        return this.mConditionsByPkg.get(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onActivityEventLocked(ComponentName componentName, int type) {
        if (this.mRemoteService == null) {
            if (((ContentCaptureManagerService) this.mMaster).debug) {
                Slog.d(this.mTag, "onActivityEvent(): no remote service");
                return;
            }
            return;
        }
        ActivityEvent event = new ActivityEvent(componentName, type);
        if (((ContentCaptureManagerService) this.mMaster).verbose) {
            Slog.v(this.mTag, "onActivityEvent(): " + event);
        }
        this.mRemoteService.onActivityLifecycleEvent(event);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractPerUserSystemService
    public void dumpLocked(String prefix, PrintWriter pw) {
        super.dumpLocked(prefix, pw);
        String prefix2 = prefix + "  ";
        pw.print(prefix);
        pw.print("Service Info: ");
        if (this.mInfo == null) {
            pw.println("N/A");
        } else {
            pw.println();
            this.mInfo.dump(prefix2, pw);
        }
        pw.print(prefix);
        pw.print("Zombie: ");
        pw.println(this.mZombie);
        if (this.mRemoteService != null) {
            pw.print(prefix);
            pw.println("remote service:");
            this.mRemoteService.dump(prefix2, pw);
        }
        if (this.mSessions.size() == 0) {
            pw.print(prefix);
            pw.println("no sessions");
            return;
        }
        int sessionsSize = this.mSessions.size();
        pw.print(prefix);
        pw.print("number sessions: ");
        pw.println(sessionsSize);
        for (int i = 0; i < sessionsSize; i++) {
            pw.print(prefix);
            pw.print("#");
            pw.println(i);
            ContentCaptureServerSession session = this.mSessions.valueAt(i);
            session.dumpLocked(prefix2, pw);
            pw.println();
        }
    }

    private int getSessionId(IBinder activityToken) {
        for (int i = 0; i < this.mSessions.size(); i++) {
            ContentCaptureServerSession session = this.mSessions.valueAt(i);
            if (session.isActivitySession(activityToken)) {
                return this.mSessions.keyAt(i);
            }
        }
        return 0;
    }

    private void resetContentCaptureWhitelistLocked() {
        if (((ContentCaptureManagerService) this.mMaster).verbose) {
            Slog.v(TAG, "resetting content capture whitelist");
        }
        ((ContentCaptureManagerService) this.mMaster).mGlobalContentCaptureOptions.resetWhitelist(this.mUserId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ContentCaptureServiceRemoteCallback extends IContentCaptureServiceCallback.Stub {
        private ContentCaptureServiceRemoteCallback() {
        }

        public void setContentCaptureWhitelist(List<String> packages, List<ComponentName> activities) {
            if (((ContentCaptureManagerService) ContentCapturePerUserService.this.mMaster).verbose) {
                Slog.v(ContentCapturePerUserService.TAG, "setContentCaptureWhitelist(" + (packages == null ? "null_packages" : packages.size() + " packages") + ", " + (activities == null ? "null_activities" : activities.size() + " activities") + ") for user " + ContentCapturePerUserService.this.mUserId);
            }
            ArraySet<String> oldList = ((ContentCaptureManagerService) ContentCapturePerUserService.this.mMaster).mGlobalContentCaptureOptions.getWhitelistedPackages(ContentCapturePerUserService.this.mUserId);
            ((ContentCaptureManagerService) ContentCapturePerUserService.this.mMaster).mGlobalContentCaptureOptions.setWhitelist(ContentCapturePerUserService.this.mUserId, packages, activities);
            ContentCaptureMetricsLogger.writeSetWhitelistEvent(ContentCapturePerUserService.this.getServiceComponentName(), packages, activities);
            updateContentCaptureOptions(oldList);
            int numSessions = ContentCapturePerUserService.this.mSessions.size();
            if (numSessions <= 0) {
                return;
            }
            SparseBooleanArray blacklistedSessions = new SparseBooleanArray(numSessions);
            for (int i = 0; i < numSessions; i++) {
                ContentCaptureServerSession session = (ContentCaptureServerSession) ContentCapturePerUserService.this.mSessions.valueAt(i);
                boolean whitelisted = ((ContentCaptureManagerService) ContentCapturePerUserService.this.mMaster).mGlobalContentCaptureOptions.isWhitelisted(ContentCapturePerUserService.this.mUserId, session.appComponentName);
                if (!whitelisted) {
                    int sessionId = ContentCapturePerUserService.this.mSessions.keyAt(i);
                    if (((ContentCaptureManagerService) ContentCapturePerUserService.this.mMaster).debug) {
                        Slog.d(ContentCapturePerUserService.TAG, "marking session " + sessionId + " (" + session.appComponentName + ") for un-whitelisting");
                    }
                    blacklistedSessions.append(sessionId, true);
                }
            }
            int numBlacklisted = blacklistedSessions.size();
            if (numBlacklisted <= 0) {
                return;
            }
            synchronized (ContentCapturePerUserService.this.mLock) {
                for (int i2 = 0; i2 < numBlacklisted; i2++) {
                    int sessionId2 = blacklistedSessions.keyAt(i2);
                    if (((ContentCaptureManagerService) ContentCapturePerUserService.this.mMaster).debug) {
                        Slog.d(ContentCapturePerUserService.TAG, "un-whitelisting " + sessionId2);
                    }
                    ((ContentCaptureServerSession) ContentCapturePerUserService.this.mSessions.get(sessionId2)).setContentCaptureEnabledLocked(false);
                }
            }
        }

        public void setContentCaptureConditions(String packageName, List<ContentCaptureCondition> conditions) {
            if (((ContentCaptureManagerService) ContentCapturePerUserService.this.mMaster).verbose) {
                Slog.v(ContentCapturePerUserService.TAG, "setContentCaptureConditions(" + packageName + "): " + (conditions == null ? "null" : conditions.size() + " conditions"));
            }
            synchronized (ContentCapturePerUserService.this.mLock) {
                if (conditions == null) {
                    ContentCapturePerUserService.this.mConditionsByPkg.remove(packageName);
                } else {
                    ContentCapturePerUserService.this.mConditionsByPkg.put(packageName, new ArraySet(conditions));
                }
            }
        }

        public void disableSelf() {
            if (((ContentCaptureManagerService) ContentCapturePerUserService.this.mMaster).verbose) {
                Slog.v(ContentCapturePerUserService.TAG, "disableSelf()");
            }
            long token = Binder.clearCallingIdentity();
            try {
                Settings.Secure.putStringForUser(ContentCapturePerUserService.this.getContext().getContentResolver(), "content_capture_enabled", "0", ContentCapturePerUserService.this.mUserId);
                Binder.restoreCallingIdentity(token);
                ContentCaptureMetricsLogger.writeServiceEvent(4, ContentCapturePerUserService.this.getServiceComponentName());
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        }

        public void writeSessionFlush(int sessionId, ComponentName app, FlushMetrics flushMetrics, ContentCaptureOptions options, int flushReason) {
            ContentCaptureMetricsLogger.writeSessionFlush(sessionId, ContentCapturePerUserService.this.getServiceComponentName(), flushMetrics, options, flushReason);
        }

        private void updateContentCaptureOptions(ArraySet<String> oldList) {
            ArraySet<String> adding = ((ContentCaptureManagerService) ContentCapturePerUserService.this.mMaster).mGlobalContentCaptureOptions.getWhitelistedPackages(ContentCapturePerUserService.this.mUserId);
            if (oldList != null && adding != null) {
                adding.removeAll((ArraySet<? extends String>) oldList);
            }
            int N = adding != null ? adding.size() : 0;
            for (int i = 0; i < N; i++) {
                String packageName = adding.valueAt(i);
                ContentCaptureOptions options = ((ContentCaptureManagerService) ContentCapturePerUserService.this.mMaster).mGlobalContentCaptureOptions.getOptions(ContentCapturePerUserService.this.mUserId, packageName);
                ((ContentCaptureManagerService) ContentCapturePerUserService.this.mMaster).updateOptions(packageName, options);
            }
        }
    }
}
