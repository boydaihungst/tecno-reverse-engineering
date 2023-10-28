package android.companion;

import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.companion.CompanionDeviceManager;
import android.companion.IAssociationRequestCallback;
import android.companion.IOnAssociationsChangedListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentSender;
import android.net.MacAddress;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ExceptionUtils;
import com.android.internal.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
/* loaded from: classes.dex */
public final class CompanionDeviceManager {
    public static final String COMPANION_DEVICE_DISCOVERY_PACKAGE_NAME = "com.android.companiondevicemanager";
    private static final boolean DEBUG = false;
    public static final String EXTRA_ASSOCIATION = "android.companion.extra.ASSOCIATION";
    @Deprecated
    public static final String EXTRA_DEVICE = "android.companion.extra.DEVICE";
    private static final String LOG_TAG = "CompanionDeviceManager";
    public static final String REASON_CANCELED = "canceled";
    public static final String REASON_DISCOVERY_TIMEOUT = "discovery_timeout";
    public static final String REASON_USER_REJECTED = "user_rejected";
    public static final int RESULT_DISCOVERY_TIMEOUT = 2;
    public static final int RESULT_INTERNAL_ERROR = 3;
    public static final int RESULT_USER_REJECTED = 1;
    private Context mContext;
    private final ArrayList<OnAssociationsChangedListenerProxy> mListeners = new ArrayList<>();
    private final ICompanionDeviceManager mService;

    @SystemApi
    /* loaded from: classes.dex */
    public interface OnAssociationsChangedListener {
        void onAssociationsChanged(List<AssociationInfo> list);
    }

    /* loaded from: classes.dex */
    public static abstract class Callback {
        public abstract void onFailure(CharSequence charSequence);

        @Deprecated
        public void onDeviceFound(IntentSender intentSender) {
        }

        public void onAssociationPending(IntentSender intentSender) {
            onDeviceFound(intentSender);
        }

        public void onAssociationCreated(AssociationInfo associationInfo) {
        }
    }

    public CompanionDeviceManager(ICompanionDeviceManager service, Context context) {
        this.mService = service;
        this.mContext = context;
    }

    public void associate(AssociationRequest request, Callback callback, Handler handler) {
        if (checkFeaturePresent()) {
            Objects.requireNonNull(request, "Request cannot be null");
            Objects.requireNonNull(callback, "Callback cannot be null");
            try {
                this.mService.associate(request, new AssociationRequestCallbackProxy(Handler.mainIfNull(handler), callback), this.mContext.getOpPackageName(), this.mContext.getUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void associate(AssociationRequest request, Executor executor, Callback callback) {
        if (checkFeaturePresent()) {
            Objects.requireNonNull(request, "Request cannot be null");
            Objects.requireNonNull(executor, "Executor cannot be null");
            Objects.requireNonNull(callback, "Callback cannot be null");
            try {
                this.mService.associate(request, new AssociationRequestCallbackProxy(executor, callback), this.mContext.getOpPackageName(), this.mContext.getUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @Deprecated
    public List<String> getAssociations() {
        return CollectionUtils.mapNotNull(getMyAssociations(), new Function() { // from class: android.companion.CompanionDeviceManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return CompanionDeviceManager.lambda$getAssociations$0((AssociationInfo) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String lambda$getAssociations$0(AssociationInfo a) {
        if (a.isSelfManaged()) {
            return null;
        }
        return a.getDeviceMacAddressAsString();
    }

    public List<AssociationInfo> getMyAssociations() {
        if (checkFeaturePresent()) {
            try {
                return this.mService.getAssociations(this.mContext.getOpPackageName(), this.mContext.getUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return Collections.emptyList();
    }

    @Deprecated
    public void disassociate(String deviceMacAddress) {
        if (checkFeaturePresent()) {
            try {
                this.mService.legacyDisassociate(deviceMacAddress, this.mContext.getOpPackageName(), this.mContext.getUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void disassociate(int associationId) {
        if (checkFeaturePresent()) {
            try {
                this.mService.disassociate(associationId);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void requestNotificationAccess(ComponentName component) {
        if (!checkFeaturePresent()) {
            return;
        }
        try {
            IntentSender intentSender = this.mService.requestNotificationAccess(component, this.mContext.getUserId()).getIntentSender();
            this.mContext.startIntentSender(intentSender, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            throw new RuntimeException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean hasNotificationAccess(ComponentName component) {
        if (!checkFeaturePresent()) {
            return false;
        }
        try {
            return this.mService.hasNotificationAccess(component);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean isDeviceAssociatedForWifiConnection(String packageName, MacAddress macAddress, UserHandle user) {
        if (checkFeaturePresent()) {
            Objects.requireNonNull(packageName, "package name cannot be null");
            Objects.requireNonNull(macAddress, "mac address cannot be null");
            Objects.requireNonNull(user, "user cannot be null");
            try {
                return this.mService.isDeviceAssociatedForWifiConnection(packageName, macAddress.toString(), user.getIdentifier());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    @SystemApi
    public List<AssociationInfo> getAllAssociations() {
        if (checkFeaturePresent()) {
            try {
                return this.mService.getAllAssociationsForUser(this.mContext.getUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return Collections.emptyList();
    }

    @SystemApi
    public void addOnAssociationsChangedListener(Executor executor, OnAssociationsChangedListener listener) {
        if (checkFeaturePresent()) {
            synchronized (this.mListeners) {
                OnAssociationsChangedListenerProxy proxy = new OnAssociationsChangedListenerProxy(executor, listener);
                try {
                    this.mService.addOnAssociationsChangedListener(proxy, this.mContext.getUserId());
                    this.mListeners.add(proxy);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    @SystemApi
    public void removeOnAssociationsChangedListener(OnAssociationsChangedListener listener) {
        if (checkFeaturePresent()) {
            synchronized (this.mListeners) {
                Iterator<OnAssociationsChangedListenerProxy> iterator = this.mListeners.iterator();
                while (iterator.hasNext()) {
                    OnAssociationsChangedListenerProxy proxy = iterator.next();
                    if (proxy.mListener == listener) {
                        try {
                            this.mService.removeOnAssociationsChangedListener(proxy, this.mContext.getUserId());
                            iterator.remove();
                        } catch (RemoteException e) {
                            throw e.rethrowFromSystemServer();
                        }
                    }
                }
            }
        }
    }

    @SystemApi
    public boolean canPairWithoutPrompt(String packageName, String deviceMacAddress, UserHandle user) {
        if (!checkFeaturePresent()) {
            return false;
        }
        Objects.requireNonNull(packageName, "package name cannot be null");
        Objects.requireNonNull(deviceMacAddress, "device mac address cannot be null");
        Objects.requireNonNull(user, "user handle cannot be null");
        try {
            return this.mService.canPairWithoutPrompt(packageName, deviceMacAddress, user.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void startObservingDevicePresence(String deviceAddress) throws DeviceNotAssociatedException {
        if (!checkFeaturePresent()) {
            return;
        }
        Objects.requireNonNull(deviceAddress, "address cannot be null");
        try {
            this.mService.registerDevicePresenceListenerService(deviceAddress, this.mContext.getOpPackageName(), this.mContext.getUserId());
        } catch (RemoteException e) {
            ExceptionUtils.propagateIfInstanceOf(e.getCause(), DeviceNotAssociatedException.class);
            throw e.rethrowFromSystemServer();
        }
    }

    public void stopObservingDevicePresence(String deviceAddress) throws DeviceNotAssociatedException {
        if (!checkFeaturePresent()) {
            return;
        }
        Objects.requireNonNull(deviceAddress, "address cannot be null");
        try {
            this.mService.unregisterDevicePresenceListenerService(deviceAddress, this.mContext.getPackageName(), this.mContext.getUserId());
        } catch (RemoteException e) {
            ExceptionUtils.propagateIfInstanceOf(e.getCause(), DeviceNotAssociatedException.class);
        }
    }

    public void dispatchMessage(int messageId, int associationId, byte[] message) throws DeviceNotAssociatedException {
        try {
            this.mService.dispatchMessage(messageId, associationId, message);
        } catch (RemoteException e) {
            ExceptionUtils.propagateIfInstanceOf(e.getCause(), DeviceNotAssociatedException.class);
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void associate(String packageName, MacAddress macAddress, byte[] certificate) {
        if (!checkFeaturePresent()) {
            return;
        }
        Objects.requireNonNull(packageName, "package name cannot be null");
        Objects.requireNonNull(macAddress, "mac address cannot be null");
        UserHandle user = Process.myUserHandle();
        try {
            this.mService.createAssociation(packageName, macAddress.toString(), user.getIdentifier(), certificate);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void notifyDeviceAppeared(int associationId) {
        try {
            this.mService.notifyDeviceAppeared(associationId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void notifyDeviceDisappeared(int associationId) {
        try {
            this.mService.notifyDeviceDisappeared(associationId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private boolean checkFeaturePresent() {
        return this.mService != null;
    }

    /* loaded from: classes.dex */
    private static class AssociationRequestCallbackProxy extends IAssociationRequestCallback.Stub {
        private final Callback mCallback;
        private final Executor mExecutor;
        private final Handler mHandler;

        private AssociationRequestCallbackProxy(Executor executor, Callback callback) {
            this.mExecutor = executor;
            this.mHandler = null;
            this.mCallback = callback;
        }

        private AssociationRequestCallbackProxy(Handler handler, Callback callback) {
            this.mHandler = handler;
            this.mExecutor = null;
            this.mCallback = callback;
        }

        @Override // android.companion.IAssociationRequestCallback
        public void onAssociationPending(PendingIntent pi) {
            final Callback callback = this.mCallback;
            Objects.requireNonNull(callback);
            execute(new Consumer() { // from class: android.companion.CompanionDeviceManager$AssociationRequestCallbackProxy$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    CompanionDeviceManager.Callback.this.onAssociationPending((IntentSender) obj);
                }
            }, pi.getIntentSender());
        }

        @Override // android.companion.IAssociationRequestCallback
        public void onAssociationCreated(AssociationInfo association) {
            final Callback callback = this.mCallback;
            Objects.requireNonNull(callback);
            execute(new Consumer() { // from class: android.companion.CompanionDeviceManager$AssociationRequestCallbackProxy$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    CompanionDeviceManager.Callback.this.onAssociationCreated((AssociationInfo) obj);
                }
            }, association);
        }

        @Override // android.companion.IAssociationRequestCallback
        public void onFailure(CharSequence error) throws RemoteException {
            final Callback callback = this.mCallback;
            Objects.requireNonNull(callback);
            execute(new Consumer() { // from class: android.companion.CompanionDeviceManager$AssociationRequestCallbackProxy$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    CompanionDeviceManager.Callback.this.onFailure((CharSequence) obj);
                }
            }, error);
        }

        private <T> void execute(final Consumer<T> callback, final T arg) {
            Executor executor = this.mExecutor;
            if (executor != null) {
                executor.execute(new Runnable() { // from class: android.companion.CompanionDeviceManager$AssociationRequestCallbackProxy$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        callback.accept(arg);
                    }
                });
            } else {
                this.mHandler.post(new Runnable() { // from class: android.companion.CompanionDeviceManager$AssociationRequestCallbackProxy$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        callback.accept(arg);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class OnAssociationsChangedListenerProxy extends IOnAssociationsChangedListener.Stub {
        private final Executor mExecutor;
        private final OnAssociationsChangedListener mListener;

        private OnAssociationsChangedListenerProxy(Executor executor, OnAssociationsChangedListener listener) {
            this.mExecutor = executor;
            this.mListener = listener;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAssociationsChanged$0$android-companion-CompanionDeviceManager$OnAssociationsChangedListenerProxy  reason: not valid java name */
        public /* synthetic */ void m693x21c6a5bf(List associations) {
            this.mListener.onAssociationsChanged(associations);
        }

        @Override // android.companion.IOnAssociationsChangedListener
        public void onAssociationsChanged(final List<AssociationInfo> associations) {
            this.mExecutor.execute(new Runnable() { // from class: android.companion.CompanionDeviceManager$OnAssociationsChangedListenerProxy$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    CompanionDeviceManager.OnAssociationsChangedListenerProxy.this.m693x21c6a5bf(associations);
                }
            });
        }
    }
}
