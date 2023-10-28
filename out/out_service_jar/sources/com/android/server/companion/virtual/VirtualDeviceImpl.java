package com.android.server.companion.virtual;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.companion.AssociationInfo;
import android.companion.virtual.IVirtualDevice;
import android.companion.virtual.IVirtualDeviceActivityListener;
import android.companion.virtual.VirtualDeviceManager;
import android.companion.virtual.VirtualDeviceParams;
import android.companion.virtual.audio.IAudioConfigChangedCallback;
import android.companion.virtual.audio.IAudioRoutingCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.display.DisplayManager;
import android.hardware.input.VirtualKeyEvent;
import android.hardware.input.VirtualMouseButtonEvent;
import android.hardware.input.VirtualMouseRelativeEvent;
import android.hardware.input.VirtualMouseScrollEvent;
import android.hardware.input.VirtualTouchEvent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.internal.app.BlockedAppStreamingActivity;
import com.android.server.companion.virtual.GenericWindowPolicyController;
import com.android.server.companion.virtual.audio.VirtualAudioController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class VirtualDeviceImpl extends IVirtualDevice.Stub implements IBinder.DeathRecipient, GenericWindowPolicyController.RunningAppsChangedListener {
    private static final long PENDING_TRAMPOLINE_TIMEOUT_MS = 5000;
    private static final String TAG = "VirtualDeviceImpl";
    private final IVirtualDeviceActivityListener mActivityListener;
    private final IBinder mAppToken;
    private final AssociationInfo mAssociationInfo;
    private final Context mContext;
    private boolean mDefaultShowPointerIcon;
    private final InputController mInputController;
    private final OnDeviceCloseListener mListener;
    private final int mOwnerUid;
    private final VirtualDeviceParams mParams;
    private final PendingTrampolineCallback mPendingTrampolineCallback;
    private final Map<Integer, PowerManager.WakeLock> mPerDisplayWakelocks;
    private Consumer<ArraySet<Integer>> mRunningAppsChangedCallback;
    private VirtualAudioController mVirtualAudioController;
    private final Object mVirtualDeviceLock;
    final Set<Integer> mVirtualDisplayIds;
    private final SparseArray<GenericWindowPolicyController> mWindowPolicyControllers;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface OnDeviceCloseListener {
        void onClose(int i);
    }

    /* loaded from: classes.dex */
    interface PendingTrampolineCallback {
        void startWaitingForPendingTrampoline(PendingTrampoline pendingTrampoline);

        void stopWaitingForPendingTrampoline(PendingTrampoline pendingTrampoline);
    }

    private VirtualDeviceManager.ActivityListener createListenerAdapter() {
        return new VirtualDeviceManager.ActivityListener() { // from class: com.android.server.companion.virtual.VirtualDeviceImpl.1
            public void onTopActivityChanged(int displayId, ComponentName topActivity) {
                try {
                    VirtualDeviceImpl.this.mActivityListener.onTopActivityChanged(displayId, topActivity);
                } catch (RemoteException e) {
                    Slog.w(VirtualDeviceImpl.TAG, "Unable to call mActivityListener", e);
                }
            }

            public void onDisplayEmpty(int displayId) {
                try {
                    VirtualDeviceImpl.this.mActivityListener.onDisplayEmpty(displayId);
                } catch (RemoteException e) {
                    Slog.w(VirtualDeviceImpl.TAG, "Unable to call mActivityListener", e);
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public VirtualDeviceImpl(Context context, AssociationInfo associationInfo, IBinder token, int ownerUid, OnDeviceCloseListener listener, PendingTrampolineCallback pendingTrampolineCallback, IVirtualDeviceActivityListener activityListener, Consumer<ArraySet<Integer>> runningAppsChangedCallback, VirtualDeviceParams params) {
        this(context, associationInfo, token, ownerUid, null, listener, pendingTrampolineCallback, activityListener, runningAppsChangedCallback, params);
    }

    VirtualDeviceImpl(Context context, AssociationInfo associationInfo, IBinder token, int ownerUid, InputController inputController, OnDeviceCloseListener listener, PendingTrampolineCallback pendingTrampolineCallback, IVirtualDeviceActivityListener activityListener, Consumer<ArraySet<Integer>> runningAppsChangedCallback, VirtualDeviceParams params) {
        Object obj = new Object();
        this.mVirtualDeviceLock = obj;
        this.mVirtualDisplayIds = new ArraySet();
        this.mPerDisplayWakelocks = new ArrayMap();
        this.mDefaultShowPointerIcon = true;
        this.mWindowPolicyControllers = new SparseArray<>();
        UserHandle ownerUserHandle = UserHandle.getUserHandleForUid(ownerUid);
        this.mContext = context.createContextAsUser(ownerUserHandle, 0);
        this.mAssociationInfo = associationInfo;
        this.mPendingTrampolineCallback = pendingTrampolineCallback;
        this.mActivityListener = activityListener;
        this.mRunningAppsChangedCallback = runningAppsChangedCallback;
        this.mOwnerUid = ownerUid;
        this.mAppToken = token;
        this.mParams = params;
        if (inputController == null) {
            this.mInputController = new InputController(obj, context.getMainThreadHandler(), (WindowManager) context.getSystemService(WindowManager.class));
        } else {
            this.mInputController = inputController;
        }
        this.mListener = listener;
        try {
            token.linkToDeath(this, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getBaseVirtualDisplayFlags() {
        if (this.mParams.getLockState() != 1) {
            return 0;
        }
        int flags = 0 | 4096;
        return flags;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CharSequence getDisplayName() {
        return this.mAssociationInfo.getDisplayName();
    }

    public int getAssociationId() {
        return this.mAssociationInfo.getId();
    }

    public void launchPendingIntent(int displayId, PendingIntent pendingIntent, ResultReceiver resultReceiver) {
        if (!this.mVirtualDisplayIds.contains(Integer.valueOf(displayId))) {
            throw new SecurityException("Display ID " + displayId + " not found for this virtual device");
        }
        if (pendingIntent.isActivity()) {
            try {
                sendPendingIntent(displayId, pendingIntent);
                resultReceiver.send(0, null);
                return;
            } catch (PendingIntent.CanceledException e) {
                Slog.w(TAG, "Pending intent canceled", e);
                resultReceiver.send(1, null);
                return;
            }
        }
        final PendingTrampoline pendingTrampoline = new PendingTrampoline(pendingIntent, resultReceiver, displayId);
        this.mPendingTrampolineCallback.startWaitingForPendingTrampoline(pendingTrampoline);
        this.mContext.getMainThreadHandler().postDelayed(new Runnable() { // from class: com.android.server.companion.virtual.VirtualDeviceImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                VirtualDeviceImpl.this.m2732xccf55833(pendingTrampoline);
            }
        }, PENDING_TRAMPOLINE_TIMEOUT_MS);
        try {
            sendPendingIntent(displayId, pendingIntent);
        } catch (PendingIntent.CanceledException e2) {
            Slog.w(TAG, "Pending intent canceled", e2);
            resultReceiver.send(1, null);
            this.mPendingTrampolineCallback.stopWaitingForPendingTrampoline(pendingTrampoline);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$launchPendingIntent$0$com-android-server-companion-virtual-VirtualDeviceImpl  reason: not valid java name */
    public /* synthetic */ void m2732xccf55833(PendingTrampoline pendingTrampoline) {
        pendingTrampoline.mResultReceiver.send(2, null);
        this.mPendingTrampolineCallback.stopWaitingForPendingTrampoline(pendingTrampoline);
    }

    private void sendPendingIntent(int displayId, PendingIntent pendingIntent) throws PendingIntent.CanceledException {
        ActivityOptions options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId);
        options.setPendingIntentBackgroundActivityLaunchAllowed(true);
        options.setPendingIntentBackgroundActivityLaunchAllowedByPermission(true);
        pendingIntent.send(this.mContext, 0, null, null, null, null, options.toBundle());
    }

    public void close() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CREATE_VIRTUAL_DEVICE", "Permission required to close the virtual device");
        synchronized (this.mVirtualDeviceLock) {
            if (!this.mPerDisplayWakelocks.isEmpty()) {
                this.mPerDisplayWakelocks.forEach(new BiConsumer() { // from class: com.android.server.companion.virtual.VirtualDeviceImpl$$ExternalSyntheticLambda2
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        VirtualDeviceImpl.this.m2731x632c14fa((Integer) obj, (PowerManager.WakeLock) obj2);
                    }
                });
                this.mPerDisplayWakelocks.clear();
            }
            VirtualAudioController virtualAudioController = this.mVirtualAudioController;
            if (virtualAudioController != null) {
                virtualAudioController.stopListening();
                this.mVirtualAudioController = null;
            }
        }
        this.mListener.onClose(this.mAssociationInfo.getId());
        this.mAppToken.unlinkToDeath(this, 0);
        long token = Binder.clearCallingIdentity();
        try {
            this.mInputController.close();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$close$1$com-android-server-companion-virtual-VirtualDeviceImpl  reason: not valid java name */
    public /* synthetic */ void m2731x632c14fa(Integer displayId, PowerManager.WakeLock wakeLock) {
        Slog.w(TAG, "VirtualDisplay " + displayId + " owned by UID " + this.mOwnerUid + " was not properly released");
        wakeLock.release();
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        close();
    }

    @Override // com.android.server.companion.virtual.GenericWindowPolicyController.RunningAppsChangedListener
    public void onRunningAppsChanged(ArraySet<Integer> runningUids) {
        this.mRunningAppsChangedCallback.accept(runningUids);
    }

    VirtualAudioController getVirtualAudioControllerForTesting() {
        return this.mVirtualAudioController;
    }

    SparseArray<GenericWindowPolicyController> getWindowPolicyControllersForTesting() {
        return this.mWindowPolicyControllers;
    }

    public void onAudioSessionStarting(int displayId, IAudioRoutingCallback routingCallback, IAudioConfigChangedCallback configChangedCallback) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CREATE_VIRTUAL_DEVICE", "Permission required to start audio session");
        synchronized (this.mVirtualDeviceLock) {
            if (!this.mVirtualDisplayIds.contains(Integer.valueOf(displayId))) {
                throw new SecurityException("Cannot start audio session for a display not associated with this virtual device");
            }
            if (this.mVirtualAudioController == null) {
                this.mVirtualAudioController = new VirtualAudioController(this.mContext);
                GenericWindowPolicyController gwpc = this.mWindowPolicyControllers.get(displayId);
                this.mVirtualAudioController.startListening(gwpc, routingCallback, configChangedCallback);
            }
        }
    }

    public void onAudioSessionEnded() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CREATE_VIRTUAL_DEVICE", "Permission required to stop audio session");
        synchronized (this.mVirtualDeviceLock) {
            VirtualAudioController virtualAudioController = this.mVirtualAudioController;
            if (virtualAudioController != null) {
                virtualAudioController.stopListening();
                this.mVirtualAudioController = null;
            }
        }
    }

    public void createVirtualKeyboard(int displayId, String deviceName, int vendorId, int productId, IBinder deviceToken) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CREATE_VIRTUAL_DEVICE", "Permission required to create a virtual keyboard");
        synchronized (this.mVirtualDeviceLock) {
            if (!this.mVirtualDisplayIds.contains(Integer.valueOf(displayId))) {
                throw new SecurityException("Cannot create a virtual keyboard for a display not associated with this virtual device");
            }
        }
        long token = Binder.clearCallingIdentity();
        try {
            this.mInputController.createKeyboard(deviceName, vendorId, productId, deviceToken, displayId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void createVirtualMouse(int displayId, String deviceName, int vendorId, int productId, IBinder deviceToken) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CREATE_VIRTUAL_DEVICE", "Permission required to create a virtual mouse");
        synchronized (this.mVirtualDeviceLock) {
            if (!this.mVirtualDisplayIds.contains(Integer.valueOf(displayId))) {
                throw new SecurityException("Cannot create a virtual mouse for a display not associated with this virtual device");
            }
        }
        long token = Binder.clearCallingIdentity();
        try {
            this.mInputController.createMouse(deviceName, vendorId, productId, deviceToken, displayId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void createVirtualTouchscreen(int displayId, String deviceName, int vendorId, int productId, IBinder deviceToken, Point screenSize) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CREATE_VIRTUAL_DEVICE", "Permission required to create a virtual touchscreen");
        synchronized (this.mVirtualDeviceLock) {
            if (!this.mVirtualDisplayIds.contains(Integer.valueOf(displayId))) {
                throw new SecurityException("Cannot create a virtual touchscreen for a display not associated with this virtual device");
            }
        }
        long token = Binder.clearCallingIdentity();
        try {
            this.mInputController.createTouchscreen(deviceName, vendorId, productId, deviceToken, displayId, screenSize);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void unregisterInputDevice(IBinder token) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CREATE_VIRTUAL_DEVICE", "Permission required to unregister this input device");
        long binderToken = Binder.clearCallingIdentity();
        try {
            this.mInputController.unregisterInputDevice(token);
        } finally {
            Binder.restoreCallingIdentity(binderToken);
        }
    }

    public boolean sendKeyEvent(IBinder token, VirtualKeyEvent event) {
        long binderToken = Binder.clearCallingIdentity();
        try {
            return this.mInputController.sendKeyEvent(token, event);
        } finally {
            Binder.restoreCallingIdentity(binderToken);
        }
    }

    public boolean sendButtonEvent(IBinder token, VirtualMouseButtonEvent event) {
        long binderToken = Binder.clearCallingIdentity();
        try {
            return this.mInputController.sendButtonEvent(token, event);
        } finally {
            Binder.restoreCallingIdentity(binderToken);
        }
    }

    public boolean sendTouchEvent(IBinder token, VirtualTouchEvent event) {
        long binderToken = Binder.clearCallingIdentity();
        try {
            return this.mInputController.sendTouchEvent(token, event);
        } finally {
            Binder.restoreCallingIdentity(binderToken);
        }
    }

    public boolean sendRelativeEvent(IBinder token, VirtualMouseRelativeEvent event) {
        long binderToken = Binder.clearCallingIdentity();
        try {
            return this.mInputController.sendRelativeEvent(token, event);
        } finally {
            Binder.restoreCallingIdentity(binderToken);
        }
    }

    public boolean sendScrollEvent(IBinder token, VirtualMouseScrollEvent event) {
        long binderToken = Binder.clearCallingIdentity();
        try {
            return this.mInputController.sendScrollEvent(token, event);
        } finally {
            Binder.restoreCallingIdentity(binderToken);
        }
    }

    public PointF getCursorPosition(IBinder token) {
        long binderToken = Binder.clearCallingIdentity();
        try {
            return this.mInputController.getCursorPosition(token);
        } finally {
            Binder.restoreCallingIdentity(binderToken);
        }
    }

    public void setShowPointerIcon(boolean showPointerIcon) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CREATE_VIRTUAL_DEVICE", "Permission required to unregister this input device");
        long binderToken = Binder.clearCallingIdentity();
        try {
            synchronized (this.mVirtualDeviceLock) {
                this.mDefaultShowPointerIcon = showPointerIcon;
                for (Integer num : this.mVirtualDisplayIds) {
                    int displayId = num.intValue();
                    this.mInputController.setShowPointerIcon(this.mDefaultShowPointerIcon, displayId);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(binderToken);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        fout.println("  VirtualDevice: ");
        fout.println("    mAssociationId: " + this.mAssociationInfo.getId());
        fout.println("    mParams: " + this.mParams);
        fout.println("    mVirtualDisplayIds: ");
        synchronized (this.mVirtualDeviceLock) {
            for (Integer num : this.mVirtualDisplayIds) {
                int id = num.intValue();
                fout.println("      " + id);
            }
            fout.println("    mDefaultShowPointerIcon: " + this.mDefaultShowPointerIcon);
        }
        this.mInputController.dump(fout);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GenericWindowPolicyController createWindowPolicyController() {
        GenericWindowPolicyController gwpc;
        synchronized (this.mVirtualDeviceLock) {
            gwpc = new GenericWindowPolicyController(8192, 524288, getAllowedUserHandles(), this.mParams.getAllowedCrossTaskNavigations(), this.mParams.getBlockedCrossTaskNavigations(), this.mParams.getAllowedActivities(), this.mParams.getBlockedActivities(), this.mParams.getDefaultActivityPolicy(), createListenerAdapter(), new GenericWindowPolicyController.ActivityBlockedCallback() { // from class: com.android.server.companion.virtual.VirtualDeviceImpl$$ExternalSyntheticLambda1
                @Override // com.android.server.companion.virtual.GenericWindowPolicyController.ActivityBlockedCallback
                public final void onActivityBlocked(int i, ActivityInfo activityInfo) {
                    VirtualDeviceImpl.this.onActivityBlocked(i, activityInfo);
                }
            }, this.mAssociationInfo.getDeviceProfile());
            gwpc.registerRunningAppsChangedListener(this);
        }
        return gwpc;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onVirtualDisplayCreatedLocked(GenericWindowPolicyController gwpc, int displayId) {
        synchronized (this.mVirtualDeviceLock) {
            if (displayId == -1) {
                return;
            }
            if (this.mVirtualDisplayIds.contains(Integer.valueOf(displayId))) {
                throw new IllegalStateException("Virtual device already has a virtual display with ID " + displayId);
            }
            this.mVirtualDisplayIds.add(Integer.valueOf(displayId));
            gwpc.setDisplayId(displayId);
            this.mWindowPolicyControllers.put(displayId, gwpc);
            this.mInputController.setShowPointerIcon(this.mDefaultShowPointerIcon, displayId);
            this.mInputController.setPointerAcceleration(1.0f, displayId);
            this.mInputController.setDisplayEligibilityForPointerCapture(false, displayId);
            this.mInputController.setLocalIme(displayId);
            if (this.mPerDisplayWakelocks.containsKey(Integer.valueOf(displayId))) {
                Slog.e(TAG, "Not creating wakelock for displayId " + displayId);
                return;
            }
            PowerManager powerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(10, "VirtualDeviceImpl:" + displayId, displayId);
            this.mPerDisplayWakelocks.put(Integer.valueOf(displayId), wakeLock);
            wakeLock.acquire();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onActivityBlocked(int displayId, ActivityInfo activityInfo) {
        Intent intent = BlockedAppStreamingActivity.createIntent(activityInfo, this.mAssociationInfo.getDisplayName());
        this.mContext.startActivityAsUser(intent.addFlags(268468224), ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle(), this.mContext.getUser());
    }

    private ArraySet<UserHandle> getAllowedUserHandles() {
        ArraySet<UserHandle> result = new ArraySet<>();
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class);
        UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        for (UserHandle profile : userManager.getAllProfiles()) {
            int nearbyAppStreamingPolicy = dpm.getNearbyAppStreamingPolicy(profile.getIdentifier());
            if (nearbyAppStreamingPolicy == 2 || nearbyAppStreamingPolicy == 0) {
                result.add(profile);
            } else if (nearbyAppStreamingPolicy == 3 && this.mParams.getUsersWithMatchingAccounts().contains(profile)) {
                result.add(profile);
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onVirtualDisplayRemovedLocked(int displayId) {
        synchronized (this.mVirtualDeviceLock) {
            if (!this.mVirtualDisplayIds.contains(Integer.valueOf(displayId))) {
                throw new IllegalStateException("Virtual device doesn't have a virtual display with ID " + displayId);
            }
            PowerManager.WakeLock wakeLock = this.mPerDisplayWakelocks.get(Integer.valueOf(displayId));
            if (wakeLock != null) {
                wakeLock.release();
                this.mPerDisplayWakelocks.remove(Integer.valueOf(displayId));
            }
            GenericWindowPolicyController gwpc = this.mWindowPolicyControllers.get(displayId);
            if (gwpc != null) {
                gwpc.unregisterRunningAppsChangedListener(this);
            }
            this.mVirtualDisplayIds.remove(Integer.valueOf(displayId));
            this.mWindowPolicyControllers.remove(displayId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getOwnerUid() {
        return this.mOwnerUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAppRunningOnVirtualDevice(int uid) {
        int size = this.mWindowPolicyControllers.size();
        for (int i = 0; i < size; i++) {
            if (this.mWindowPolicyControllers.valueAt(i).containsUid(uid)) {
                return true;
            }
        }
        return false;
    }

    void showToastWhereUidIsRunning(int uid, int resId, int duration) {
        showToastWhereUidIsRunning(uid, this.mContext.getString(resId), duration);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showToastWhereUidIsRunning(int uid, String text, int duration) {
        synchronized (this.mVirtualDeviceLock) {
            DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService(DisplayManager.class);
            int size = this.mWindowPolicyControllers.size();
            for (int i = 0; i < size; i++) {
                if (this.mWindowPolicyControllers.valueAt(i).containsUid(uid)) {
                    int displayId = this.mWindowPolicyControllers.keyAt(i);
                    Display display = displayManager.getDisplay(displayId);
                    if (display != null && display.isValid()) {
                        Toast.makeText(this.mContext.createDisplayContext(display), text, duration).show();
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDisplayOwnedByVirtualDevice(int displayId) {
        return this.mVirtualDisplayIds.contains(Integer.valueOf(displayId));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class PendingTrampoline {
        final int mDisplayId;
        final PendingIntent mPendingIntent;
        final ResultReceiver mResultReceiver;

        private PendingTrampoline(PendingIntent pendingIntent, ResultReceiver resultReceiver, int displayId) {
            this.mPendingIntent = pendingIntent;
            this.mResultReceiver = resultReceiver;
            this.mDisplayId = displayId;
        }

        public String toString() {
            return "PendingTrampoline{pendingIntent=" + this.mPendingIntent + ", resultReceiver=" + this.mResultReceiver + ", displayId=" + this.mDisplayId + "}";
        }
    }
}
