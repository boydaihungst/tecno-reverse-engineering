package com.android.server.translation;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.service.translation.TranslationServiceInfo;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.view.autofill.AutofillId;
import android.view.inputmethod.InputMethodInfo;
import android.view.translation.ITranslationServiceCallback;
import android.view.translation.TranslationCapability;
import android.view.translation.TranslationContext;
import android.view.translation.TranslationSpec;
import android.view.translation.UiTranslationSpec;
import com.android.internal.os.IResultReceiver;
import com.android.internal.os.TransferPipe;
import com.android.server.LocalServices;
import com.android.server.infra.AbstractPerUserSystemService;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class TranslationManagerServiceImpl extends AbstractPerUserSystemService<TranslationManagerServiceImpl, TranslationManagerService> implements IBinder.DeathRecipient {
    private final ArrayMap<IBinder, ActiveTranslation> mActiveTranslations;
    private final ActivityTaskManagerInternal mActivityTaskManagerInternal;
    private final RemoteCallbackList<IRemoteCallback> mCallbacks;
    private WeakReference<ActivityTaskManagerInternal.ActivityTokens> mLastActivityTokens;
    private final TranslationServiceRemoteCallback mRemoteServiceCallback;
    private RemoteTranslationService mRemoteTranslationService;
    private ServiceInfo mRemoteTranslationServiceInfo;
    private final RemoteCallbackList<IRemoteCallback> mTranslationCapabilityCallbacks;
    private TranslationServiceInfo mTranslationServiceInfo;
    private final ArraySet<IBinder> mWaitingFinishedCallbackActivities;
    private static final String TAG = "TranslationManagerServiceImpl";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    /* JADX INFO: Access modifiers changed from: protected */
    public TranslationManagerServiceImpl(TranslationManagerService master, Object lock, int userId, boolean disabled) {
        super(master, lock, userId);
        this.mRemoteServiceCallback = new TranslationServiceRemoteCallback();
        this.mTranslationCapabilityCallbacks = new RemoteCallbackList<>();
        this.mWaitingFinishedCallbackActivities = new ArraySet<>();
        this.mActiveTranslations = new ArrayMap<>();
        this.mCallbacks = new RemoteCallbackList<>();
        updateRemoteServiceLocked();
        this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
    }

    @Override // com.android.server.infra.AbstractPerUserSystemService
    protected ServiceInfo newServiceInfoLocked(ComponentName serviceComponent) throws PackageManager.NameNotFoundException {
        TranslationServiceInfo translationServiceInfo = new TranslationServiceInfo(getContext(), serviceComponent, isTemporaryServiceSetLocked(), this.mUserId);
        this.mTranslationServiceInfo = translationServiceInfo;
        this.mRemoteTranslationServiceInfo = translationServiceInfo.getServiceInfo();
        return this.mTranslationServiceInfo.getServiceInfo();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractPerUserSystemService
    public boolean updateLocked(boolean disabled) {
        boolean enabledChanged = super.updateLocked(disabled);
        updateRemoteServiceLocked();
        return enabledChanged;
    }

    private void updateRemoteServiceLocked() {
        if (this.mRemoteTranslationService != null) {
            if (((TranslationManagerService) this.mMaster).debug) {
                Slog.d(TAG, "updateRemoteService(): destroying old remote service");
            }
            this.mRemoteTranslationService.unbind();
            this.mRemoteTranslationService = null;
        }
    }

    private RemoteTranslationService ensureRemoteServiceLocked() {
        if (this.mRemoteTranslationService == null) {
            String serviceName = getComponentNameLocked();
            if (serviceName == null) {
                if (((TranslationManagerService) this.mMaster).verbose) {
                    Slog.v(TAG, "ensureRemoteServiceLocked(): no service component name.");
                    return null;
                }
                return null;
            }
            ComponentName serviceComponent = ComponentName.unflattenFromString(serviceName);
            this.mRemoteTranslationService = new RemoteTranslationService(getContext(), serviceComponent, this.mUserId, false, this.mRemoteServiceCallback);
        }
        return this.mRemoteTranslationService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTranslationCapabilitiesRequestLocked(int sourceFormat, int destFormat, ResultReceiver resultReceiver) {
        RemoteTranslationService remoteService = ensureRemoteServiceLocked();
        if (remoteService != null) {
            remoteService.onTranslationCapabilitiesRequest(sourceFormat, destFormat, resultReceiver);
        }
    }

    public void registerTranslationCapabilityCallback(IRemoteCallback callback, int sourceUid) {
        this.mTranslationCapabilityCallbacks.register(callback, Integer.valueOf(sourceUid));
        ensureRemoteServiceLocked();
    }

    public void unregisterTranslationCapabilityCallback(IRemoteCallback callback) {
        this.mTranslationCapabilityCallbacks.unregister(callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSessionCreatedLocked(TranslationContext translationContext, int sessionId, IResultReceiver resultReceiver) {
        RemoteTranslationService remoteService = ensureRemoteServiceLocked();
        if (remoteService != null) {
            remoteService.onSessionCreated(translationContext, sessionId, resultReceiver);
        }
    }

    private int getAppUidByComponentName(Context context, ComponentName componentName, int userId) {
        if (componentName == null) {
            return -1;
        }
        try {
            int translatedAppUid = context.getPackageManager().getApplicationInfoAsUser(componentName.getPackageName(), 0, userId).uid;
            return translatedAppUid;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.d(TAG, "Cannot find packageManager for" + componentName);
            return -1;
        }
    }

    public void onTranslationFinishedLocked(boolean activityDestroyed, IBinder token, ComponentName componentName) {
        int translatedAppUid = getAppUidByComponentName(getContext(), componentName, getUserId());
        String packageName = componentName.getPackageName();
        if (activityDestroyed) {
            invokeCallbacks(3, null, null, packageName, translatedAppUid);
            this.mWaitingFinishedCallbackActivities.remove(token);
        } else if (this.mWaitingFinishedCallbackActivities.contains(token)) {
            invokeCallbacks(3, null, null, packageName, translatedAppUid);
            this.mWaitingFinishedCallbackActivities.remove(token);
        }
    }

    public void updateUiTranslationStateLocked(int state, TranslationSpec sourceSpec, TranslationSpec targetSpec, List<AutofillId> viewIds, IBinder token, int taskId, UiTranslationSpec uiTranslationSpec) {
        ActivityTaskManagerInternal.ActivityTokens candidateActivityTokens = this.mActivityTaskManagerInternal.getAttachedNonFinishingActivityForTask(taskId, token);
        if (candidateActivityTokens != null) {
            this.mLastActivityTokens = new WeakReference<>(candidateActivityTokens);
            if (state == 3) {
                this.mWaitingFinishedCallbackActivities.add(token);
            }
            IBinder activityToken = candidateActivityTokens.getActivityToken();
            try {
                candidateActivityTokens.getApplicationThread().updateUiTranslationState(activityToken, state, sourceSpec, targetSpec, viewIds, uiTranslationSpec);
            } catch (RemoteException e) {
                Slog.w(TAG, "Update UiTranslationState fail: " + e);
            }
            ComponentName componentName = this.mActivityTaskManagerInternal.getActivityName(activityToken);
            int translatedAppUid = getAppUidByComponentName(getContext(), componentName, getUserId());
            String packageName = componentName.getPackageName();
            invokeCallbacksIfNecessaryLocked(state, sourceSpec, targetSpec, packageName, activityToken, translatedAppUid);
            updateActiveTranslationsLocked(state, sourceSpec, targetSpec, packageName, activityToken, translatedAppUid);
            return;
        }
        Slog.w(TAG, "Unknown activity or it was finished to query for update translation state for token=" + token + " taskId=" + taskId + " for state= " + state);
    }

    private void updateActiveTranslationsLocked(int state, TranslationSpec sourceSpec, TranslationSpec targetSpec, String packageName, IBinder activityToken, int translatedAppUid) {
        ActiveTranslation activeTranslation = this.mActiveTranslations.get(activityToken);
        switch (state) {
            case 0:
                if (activeTranslation == null) {
                    try {
                        activityToken.linkToDeath(this, 0);
                        this.mActiveTranslations.put(activityToken, new ActiveTranslation(sourceSpec, targetSpec, translatedAppUid, packageName));
                        break;
                    } catch (RemoteException e) {
                        Slog.w(TAG, "Failed to call linkToDeath for translated app with uid=" + translatedAppUid + "; activity is already dead", e);
                        invokeCallbacks(3, sourceSpec, targetSpec, packageName, translatedAppUid);
                        return;
                    }
                }
                break;
            case 1:
                if (activeTranslation != null) {
                    activeTranslation.isPaused = true;
                    break;
                }
                break;
            case 2:
                if (activeTranslation != null) {
                    activeTranslation.isPaused = false;
                    break;
                }
                break;
            case 3:
                if (activeTranslation != null) {
                    this.mActiveTranslations.remove(activityToken);
                    break;
                }
                break;
        }
        if (DEBUG) {
            Slog.d(TAG, "Updating to translation state=" + state + " for app with uid=" + translatedAppUid + " packageName=" + packageName);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private void invokeCallbacksIfNecessaryLocked(int state, TranslationSpec sourceSpec, TranslationSpec targetSpec, String packageName, IBinder activityToken, int translatedAppUid) {
        boolean shouldInvokeCallbacks;
        int stateForCallbackInvocation;
        ActiveTranslation activeTranslation = this.mActiveTranslations.get(activityToken);
        if (activeTranslation == null) {
            if (state != 0) {
                Slog.w(TAG, "Updating to translation state=" + state + " for app with uid=" + translatedAppUid + " packageName=" + packageName + " but no active translation was found for it");
                shouldInvokeCallbacks = false;
                stateForCallbackInvocation = state;
            }
            shouldInvokeCallbacks = true;
            stateForCallbackInvocation = state;
        } else {
            switch (state) {
                case 0:
                    boolean specsAreIdentical = activeTranslation.sourceSpec.getLocale().equals(sourceSpec.getLocale()) && activeTranslation.targetSpec.getLocale().equals(targetSpec.getLocale());
                    if (specsAreIdentical) {
                        if (activeTranslation.isPaused) {
                            shouldInvokeCallbacks = true;
                            stateForCallbackInvocation = 2;
                            break;
                        } else {
                            shouldInvokeCallbacks = false;
                            stateForCallbackInvocation = state;
                            break;
                        }
                    }
                    shouldInvokeCallbacks = true;
                    stateForCallbackInvocation = state;
                    break;
                case 1:
                    if (activeTranslation.isPaused) {
                        shouldInvokeCallbacks = false;
                        stateForCallbackInvocation = state;
                        break;
                    }
                    shouldInvokeCallbacks = true;
                    stateForCallbackInvocation = state;
                    break;
                case 2:
                    if (!activeTranslation.isPaused) {
                        shouldInvokeCallbacks = false;
                        stateForCallbackInvocation = state;
                        break;
                    }
                    shouldInvokeCallbacks = true;
                    stateForCallbackInvocation = state;
                    break;
                case 3:
                    shouldInvokeCallbacks = false;
                    stateForCallbackInvocation = state;
                    break;
                default:
                    shouldInvokeCallbacks = true;
                    stateForCallbackInvocation = state;
                    break;
            }
        }
        boolean shouldInvokeCallbacks2 = DEBUG;
        if (shouldInvokeCallbacks2) {
            Slog.d(TAG, (shouldInvokeCallbacks ? "" : "NOT ") + "Invoking callbacks for translation state=" + stateForCallbackInvocation + " for app with uid=" + translatedAppUid + " packageName=" + packageName);
        }
        if (shouldInvokeCallbacks) {
            invokeCallbacks(stateForCallbackInvocation, sourceSpec, targetSpec, packageName, translatedAppUid);
        }
    }

    public void dumpLocked(String prefix, FileDescriptor fd, PrintWriter pw) {
        WeakReference<ActivityTaskManagerInternal.ActivityTokens> weakReference = this.mLastActivityTokens;
        if (weakReference != null) {
            ActivityTaskManagerInternal.ActivityTokens activityTokens = weakReference.get();
            if (activityTokens == null) {
                return;
            }
            try {
                TransferPipe tp = new TransferPipe();
                try {
                    activityTokens.getApplicationThread().dumpActivity(tp.getWriteFd(), activityTokens.getActivityToken(), prefix, new String[]{"--dump-dumpable", "UiTranslationController"});
                    tp.go(fd);
                    tp.close();
                } catch (Throwable th) {
                    try {
                        tp.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (RemoteException e) {
                pw.println(prefix + "Got a RemoteException while dumping the activity");
            } catch (IOException e2) {
                pw.println(prefix + "Failure while dumping the activity: " + e2);
            }
        } else {
            pw.print(prefix);
            pw.println("No requested UiTranslation Activity.");
        }
        int waitingFinishCallbackSize = this.mWaitingFinishedCallbackActivities.size();
        if (waitingFinishCallbackSize > 0) {
            pw.print(prefix);
            pw.print("number waiting finish callback activities: ");
            pw.println(waitingFinishCallbackSize);
            Iterator<IBinder> it = this.mWaitingFinishedCallbackActivities.iterator();
            while (it.hasNext()) {
                IBinder activityToken = it.next();
                pw.print(prefix);
                pw.print("activityToken: ");
                pw.println(activityToken);
            }
        }
    }

    private void invokeCallbacks(int state, TranslationSpec sourceSpec, TranslationSpec targetSpec, String packageName, final int translatedAppUid) {
        final Bundle result = createResultForCallback(state, sourceSpec, targetSpec, packageName);
        if (this.mCallbacks.getRegisteredCallbackCount() == 0) {
            return;
        }
        final List<InputMethodInfo> enabledInputMethods = getEnabledInputMethods();
        this.mCallbacks.broadcast(new BiConsumer() { // from class: com.android.server.translation.TranslationManagerServiceImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                TranslationManagerServiceImpl.this.m6936x560548dc(translatedAppUid, result, enabledInputMethods, (IRemoteCallback) obj, obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$invokeCallbacks$0$com-android-server-translation-TranslationManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6936x560548dc(int translatedAppUid, Bundle result, List enabledInputMethods, IRemoteCallback callback, Object uid) {
        invokeCallback(((Integer) uid).intValue(), translatedAppUid, callback, result, enabledInputMethods);
    }

    private List<InputMethodInfo> getEnabledInputMethods() {
        return ((InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class)).getEnabledInputMethodListAsUser(this.mUserId);
    }

    private Bundle createResultForCallback(int state, TranslationSpec sourceSpec, TranslationSpec targetSpec, String packageName) {
        Bundle result = new Bundle();
        result.putInt("state", state);
        if (sourceSpec != null) {
            result.putSerializable("source_locale", sourceSpec.getLocale());
            result.putSerializable("target_locale", targetSpec.getLocale());
        }
        result.putString("package_name", packageName);
        return result;
    }

    private void invokeCallback(int callbackSourceUid, int translatedAppUid, IRemoteCallback callback, Bundle result, List<InputMethodInfo> enabledInputMethods) {
        if (callbackSourceUid == translatedAppUid) {
            try {
                callback.sendResult(result);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to invoke UiTranslationStateCallback: " + e);
                return;
            }
        }
        boolean isIme = false;
        Iterator<InputMethodInfo> it = enabledInputMethods.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            InputMethodInfo inputMethod = it.next();
            if (callbackSourceUid == inputMethod.getServiceInfo().applicationInfo.uid) {
                isIme = true;
                break;
            }
        }
        if (!isIme) {
            return;
        }
        try {
            callback.sendResult(result);
        } catch (RemoteException e2) {
            Slog.w(TAG, "Failed to invoke UiTranslationStateCallback: " + e2);
        }
    }

    public void registerUiTranslationStateCallbackLocked(IRemoteCallback callback, int sourceUid) {
        this.mCallbacks.register(callback, Integer.valueOf(sourceUid));
        if (this.mActiveTranslations.size() == 0) {
            return;
        }
        List<InputMethodInfo> enabledInputMethods = getEnabledInputMethods();
        for (int i = 0; i < this.mActiveTranslations.size(); i++) {
            ActiveTranslation activeTranslation = this.mActiveTranslations.valueAt(i);
            int translatedAppUid = activeTranslation.translatedAppUid;
            String packageName = activeTranslation.packageName;
            if (DEBUG) {
                Slog.d(TAG, "Triggering callback for sourceUid=" + sourceUid + " for translated app with uid=" + translatedAppUid + "packageName=" + packageName + " isPaused=" + activeTranslation.isPaused);
            }
            Bundle startedResult = createResultForCallback(0, activeTranslation.sourceSpec, activeTranslation.targetSpec, packageName);
            invokeCallback(sourceUid, translatedAppUid, callback, startedResult, enabledInputMethods);
            if (activeTranslation.isPaused) {
                Bundle pausedResult = createResultForCallback(1, activeTranslation.sourceSpec, activeTranslation.targetSpec, packageName);
                invokeCallback(sourceUid, translatedAppUid, callback, pausedResult, enabledInputMethods);
            }
        }
    }

    public void unregisterUiTranslationStateCallback(IRemoteCallback callback) {
        this.mCallbacks.unregister(callback);
    }

    public ComponentName getServiceSettingsActivityLocked() {
        String activityName;
        TranslationServiceInfo translationServiceInfo = this.mTranslationServiceInfo;
        if (translationServiceInfo == null || (activityName = translationServiceInfo.getSettingsActivity()) == null) {
            return null;
        }
        String packageName = this.mTranslationServiceInfo.getServiceInfo().packageName;
        return new ComponentName(packageName, activityName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyClientsTranslationCapability(TranslationCapability capability) {
        final Bundle res = new Bundle();
        res.putParcelable("translation_capabilities", capability);
        this.mTranslationCapabilityCallbacks.broadcast(new BiConsumer() { // from class: com.android.server.translation.TranslationManagerServiceImpl$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                TranslationManagerServiceImpl.lambda$notifyClientsTranslationCapability$1(res, (IRemoteCallback) obj, obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$notifyClientsTranslationCapability$1(Bundle res, IRemoteCallback callback, Object uid) {
        try {
            callback.sendResult(res);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to invoke UiTranslationStateCallback: " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class TranslationServiceRemoteCallback extends ITranslationServiceCallback.Stub {
        private TranslationServiceRemoteCallback() {
        }

        public void updateTranslationCapability(TranslationCapability capability) {
            if (capability == null) {
                Slog.wtf(TranslationManagerServiceImpl.TAG, "received a null TranslationCapability from TranslationService.");
            } else {
                TranslationManagerServiceImpl.this.notifyClientsTranslationCapability(capability);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class ActiveTranslation {
        public boolean isPaused;
        public final String packageName;
        public final TranslationSpec sourceSpec;
        public final TranslationSpec targetSpec;
        public final int translatedAppUid;

        private ActiveTranslation(TranslationSpec sourceSpec, TranslationSpec targetSpec, int translatedAppUid, String packageName) {
            this.isPaused = false;
            this.sourceSpec = sourceSpec;
            this.targetSpec = targetSpec;
            this.translatedAppUid = translatedAppUid;
            this.packageName = packageName;
        }
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
    }

    public void binderDied(IBinder who) {
        synchronized (this.mLock) {
            this.mWaitingFinishedCallbackActivities.remove(who);
            ActiveTranslation activeTranslation = this.mActiveTranslations.remove(who);
            if (activeTranslation != null) {
                invokeCallbacks(3, activeTranslation.sourceSpec, activeTranslation.targetSpec, activeTranslation.packageName, activeTranslation.translatedAppUid);
            }
        }
    }
}
