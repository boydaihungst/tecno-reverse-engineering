package com.android.server.wm;

import android.app.IWindowToken;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.view.Display;
import android.window.WindowProviderService;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WindowContextListenerController {
    final ArrayMap<IBinder, WindowContextListenerImpl> mListeners = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerWindowContainerListener(IBinder clientToken, WindowContainer<?> container, int ownerUid, int type, Bundle options) {
        registerWindowContainerListener(clientToken, container, ownerUid, type, options, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerWindowContainerListener(IBinder clientToken, WindowContainer<?> container, int ownerUid, int type, Bundle options, boolean shouDispatchConfigWhenRegistering) {
        WindowContextListenerImpl listener = this.mListeners.get(clientToken);
        if (listener == null) {
            new WindowContextListenerImpl(clientToken, container, ownerUid, type, options).register(shouDispatchConfigWhenRegistering);
        } else {
            listener.updateContainer(container);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterWindowContainerListener(IBinder clientToken) {
        WindowContextListenerImpl listener = this.mListeners.get(clientToken);
        if (listener == null) {
            return;
        }
        listener.unregister();
        if (listener.mDeathRecipient != null) {
            listener.mDeathRecipient.unlinkToDeath();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchPendingConfigurationIfNeeded(int displayId) {
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            WindowContextListenerImpl listener = this.mListeners.valueAt(i);
            if (listener.getWindowContainer().getDisplayContent().getDisplayId() == displayId && listener.mHasPendingConfiguration) {
                listener.reportConfigToWindowTokenClient();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean assertCallerCanModifyListener(IBinder clientToken, boolean callerCanManageAppTokens, int callingUid) {
        WindowContextListenerImpl listener = this.mListeners.get(clientToken);
        if (listener == null) {
            if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -1136467585, 0, (String) null, (Object[]) null);
            }
            return false;
        } else if (callerCanManageAppTokens || callingUid == listener.mOwnerUid) {
            return true;
        } else {
            throw new UnsupportedOperationException("Uid mismatch. Caller uid is " + callingUid + ", while the listener's owner is from " + listener.mOwnerUid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasListener(IBinder clientToken) {
        return this.mListeners.containsKey(clientToken);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getWindowType(IBinder clientToken) {
        WindowContextListenerImpl listener = this.mListeners.get(clientToken);
        if (listener != null) {
            return listener.mType;
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bundle getOptions(IBinder clientToken) {
        WindowContextListenerImpl listener = this.mListeners.get(clientToken);
        if (listener != null) {
            return listener.mOptions;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowContainer<?> getContainer(IBinder clientToken) {
        WindowContextListenerImpl listener = this.mListeners.get(clientToken);
        if (listener != null) {
            return listener.mContainer;
        }
        return null;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("WindowContextListenerController{");
        builder.append("mListeners=[");
        int size = this.mListeners.values().size();
        for (int i = 0; i < size; i++) {
            builder.append(this.mListeners.valueAt(i));
            if (i != size - 1) {
                builder.append(", ");
            }
        }
        builder.append("]}");
        return builder.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class WindowContextListenerImpl implements WindowContainerListener {
        private final IWindowToken mClientToken;
        private WindowContainer<?> mContainer;
        private DeathRecipient mDeathRecipient;
        private boolean mHasPendingConfiguration;
        private Configuration mLastReportedConfig;
        private int mLastReportedDisplay;
        private final Bundle mOptions;
        private final int mOwnerUid;
        private final int mType;

        private WindowContextListenerImpl(IBinder clientToken, WindowContainer<?> container, int ownerUid, int type, Bundle options) {
            this.mLastReportedDisplay = -1;
            this.mClientToken = IWindowToken.Stub.asInterface(clientToken);
            this.mContainer = (WindowContainer) Objects.requireNonNull(container);
            this.mOwnerUid = ownerUid;
            this.mType = type;
            this.mOptions = options;
            DeathRecipient deathRecipient = new DeathRecipient();
            try {
                deathRecipient.linkToDeath();
                this.mDeathRecipient = deathRecipient;
            } catch (RemoteException e) {
                if (ProtoLogCache.WM_ERROR_enabled) {
                    String protoLogParam0 = String.valueOf(clientToken);
                    String protoLogParam1 = String.valueOf(this.mContainer);
                    ProtoLogImpl.e(ProtoLogGroup.WM_ERROR, -2014162875, 0, "Could not register window container listener token=%s, container=%s", new Object[]{protoLogParam0, protoLogParam1});
                }
            }
        }

        WindowContainer<?> getWindowContainer() {
            return this.mContainer;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateContainer(WindowContainer<?> newContainer) {
            Objects.requireNonNull(newContainer);
            if (this.mContainer.equals(newContainer)) {
                return;
            }
            this.mContainer.unregisterWindowContainerListener(this);
            this.mContainer = newContainer;
            clear();
            register();
        }

        private void register() {
            register(true);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void register(boolean shouldDispatchConfig) {
            IBinder token = this.mClientToken.asBinder();
            if (this.mDeathRecipient == null) {
                throw new IllegalStateException("Invalid client token: " + token);
            }
            WindowContextListenerController.this.mListeners.putIfAbsent(token, this);
            this.mContainer.registerWindowContainerListener(this, shouldDispatchConfig);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void unregister() {
            this.mContainer.unregisterWindowContainerListener(this);
            WindowContextListenerController.this.mListeners.remove(this.mClientToken.asBinder());
        }

        private void clear() {
            this.mLastReportedConfig = null;
            this.mLastReportedDisplay = -1;
        }

        @Override // com.android.server.wm.ConfigurationContainerListener
        public void onMergedOverrideConfigurationChanged(Configuration mergedOverrideConfig) {
            reportConfigToWindowTokenClient();
        }

        @Override // com.android.server.wm.WindowContainerListener
        public void onDisplayChanged(DisplayContent dc) {
            reportConfigToWindowTokenClient();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void reportConfigToWindowTokenClient() {
            if (this.mDeathRecipient == null) {
                throw new IllegalStateException("Invalid client token: " + this.mClientToken.asBinder());
            }
            DisplayContent dc = this.mContainer.getDisplayContent();
            if (!dc.isReady()) {
                return;
            }
            if (!WindowProviderService.isWindowProviderService(this.mOptions) && Display.isSuspendedState(dc.getDisplayInfo().state)) {
                this.mHasPendingConfiguration = true;
                return;
            }
            Configuration config = this.mContainer.getConfiguration();
            int displayId = dc.getDisplayId();
            if (this.mLastReportedConfig == null) {
                this.mLastReportedConfig = new Configuration();
            }
            if (config.equals(this.mLastReportedConfig) && displayId == this.mLastReportedDisplay) {
                return;
            }
            this.mLastReportedConfig.setTo(config);
            this.mLastReportedDisplay = displayId;
            try {
                this.mClientToken.onConfigurationChanged(config, displayId);
            } catch (RemoteException e) {
                if (ProtoLogCache.WM_ERROR_enabled) {
                    ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1948483534, 0, "Could not report config changes to the window token client.", (Object[]) null);
                }
            }
            this.mHasPendingConfiguration = false;
        }

        @Override // com.android.server.wm.WindowContainerListener
        public void onRemoved() {
            DisplayContent dc;
            if (this.mDeathRecipient == null) {
                throw new IllegalStateException("Invalid client token: " + this.mClientToken.asBinder());
            }
            WindowToken windowToken = this.mContainer.asWindowToken();
            if (windowToken != null && windowToken.isFromClient() && (dc = windowToken.mWmService.mRoot.getDisplayContent(this.mLastReportedDisplay)) != null) {
                DisplayArea<?> da = dc.findAreaForToken(windowToken);
                updateContainer(da);
                return;
            }
            this.mDeathRecipient.unlinkToDeath();
            try {
                this.mClientToken.onWindowTokenRemoved();
            } catch (RemoteException e) {
                if (ProtoLogCache.WM_ERROR_enabled) {
                    ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 90764070, 0, "Could not report token removal to the window token client.", (Object[]) null);
                }
            }
            unregister();
        }

        public String toString() {
            return "WindowContextListenerImpl{clientToken=" + this.mClientToken.asBinder() + ", container=" + this.mContainer + "}";
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class DeathRecipient implements IBinder.DeathRecipient {
            private DeathRecipient() {
            }

            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
                synchronized (WindowContextListenerImpl.this.mContainer.mWmService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        WindowContextListenerImpl.this.mDeathRecipient = null;
                        WindowContextListenerImpl.this.unregister();
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }

            void linkToDeath() throws RemoteException {
                WindowContextListenerImpl.this.mClientToken.asBinder().linkToDeath(this, 0);
            }

            void unlinkToDeath() {
                WindowContextListenerImpl.this.mClientToken.asBinder().unlinkToDeath(this, 0);
            }
        }
    }
}
