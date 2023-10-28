package com.android.server.devicestate;

import android.content.Context;
import android.hardware.devicestate.DeviceStateInfo;
import android.hardware.devicestate.DeviceStateManagerInternal;
import android.hardware.devicestate.IDeviceStateManager;
import android.hardware.devicestate.IDeviceStateManagerCallback;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.DisplayThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.devicestate.DeviceStateManagerService;
import com.android.server.devicestate.DeviceStatePolicy;
import com.android.server.devicestate.DeviceStateProvider;
import com.android.server.devicestate.OverrideRequestController;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowProcessController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public final class DeviceStateManagerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String TAG = "DeviceStateManagerService";
    private Optional<OverrideRequest> mActiveOverride;
    public ActivityTaskManagerInternal mActivityTaskManagerInternal;
    private Optional<DeviceState> mBaseState;
    private final BinderService mBinderService;
    private Optional<DeviceState> mCommittedState;
    private final DeviceStatePolicy mDeviceStatePolicy;
    private SparseArray<DeviceState> mDeviceStates;
    private Set<Integer> mDeviceStatesAvailableForAppRequests;
    private final Handler mHandler;
    private boolean mIsPolicyWaitingForState;
    private final Object mLock;
    private final OverrideRequestController mOverrideRequestController;
    private Optional<DeviceState> mPendingState;
    private final SparseArray<ProcessRecord> mProcessRecords;

    public DeviceStateManagerService(Context context) {
        this(context, DeviceStatePolicy.Provider.fromResources(context.getResources()).instantiate(context));
    }

    DeviceStateManagerService(Context context, DeviceStatePolicy policy) {
        super(context);
        this.mLock = new Object();
        this.mDeviceStates = new SparseArray<>();
        this.mCommittedState = Optional.empty();
        this.mPendingState = Optional.empty();
        this.mIsPolicyWaitingForState = false;
        this.mBaseState = Optional.empty();
        this.mActiveOverride = Optional.empty();
        this.mProcessRecords = new SparseArray<>();
        DisplayThread displayThread = DisplayThread.get();
        this.mHandler = new Handler(displayThread.getLooper());
        this.mOverrideRequestController = new OverrideRequestController(new OverrideRequestController.StatusChangeListener() { // from class: com.android.server.devicestate.DeviceStateManagerService$$ExternalSyntheticLambda3
            @Override // com.android.server.devicestate.OverrideRequestController.StatusChangeListener
            public final void onStatusChanged(OverrideRequest overrideRequest, int i) {
                DeviceStateManagerService.this.onOverrideRequestStatusChangedLocked(overrideRequest, i);
            }
        });
        this.mDeviceStatePolicy = policy;
        policy.getDeviceStateProvider().setListener(new DeviceStateProviderListener());
        this.mBinderService = new BinderService();
        this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("device_state", this.mBinderService);
        publishLocalService(DeviceStateManagerInternal.class, new LocalService());
        synchronized (this.mLock) {
            readStatesAvailableForRequestFromApps();
        }
    }

    Handler getHandler() {
        return this.mHandler;
    }

    public Optional<DeviceState> getCommittedState() {
        Optional<DeviceState> optional;
        synchronized (this.mLock) {
            optional = this.mCommittedState;
        }
        return optional;
    }

    Optional<DeviceState> getPendingState() {
        Optional<DeviceState> optional;
        synchronized (this.mLock) {
            optional = this.mPendingState;
        }
        return optional;
    }

    public Optional<DeviceState> getBaseState() {
        Optional<DeviceState> optional;
        synchronized (this.mLock) {
            optional = this.mBaseState;
        }
        return optional;
    }

    public Optional<DeviceState> getOverrideState() {
        synchronized (this.mLock) {
            if (this.mActiveOverride.isPresent()) {
                return getStateLocked(this.mActiveOverride.get().getRequestedState());
            }
            return Optional.empty();
        }
    }

    public DeviceState[] getSupportedStates() {
        DeviceState[] supportedStates;
        synchronized (this.mLock) {
            supportedStates = new DeviceState[this.mDeviceStates.size()];
            for (int i = 0; i < supportedStates.length; i++) {
                supportedStates[i] = this.mDeviceStates.valueAt(i);
            }
        }
        return supportedStates;
    }

    public int[] getSupportedStateIdentifiersLocked() {
        int[] supportedStates = new int[this.mDeviceStates.size()];
        for (int i = 0; i < supportedStates.length; i++) {
            supportedStates[i] = this.mDeviceStates.valueAt(i).getIdentifier();
        }
        return supportedStates;
    }

    public DeviceStateInfo getDeviceStateInfoLocked() {
        if (!this.mBaseState.isPresent() || !this.mCommittedState.isPresent()) {
            throw new IllegalStateException("Trying to get the current DeviceStateInfo before the initial state has been committed.");
        }
        int[] supportedStates = getSupportedStateIdentifiersLocked();
        int baseState = this.mBaseState.get().getIdentifier();
        int currentState = this.mCommittedState.get().getIdentifier();
        return new DeviceStateInfo(supportedStates, baseState, currentState);
    }

    IDeviceStateManager getBinderService() {
        return this.mBinderService;
    }

    public void updateSupportedStates(DeviceState[] supportedDeviceStates) {
        synchronized (this.mLock) {
            int[] oldStateIdentifiers = getSupportedStateIdentifiersLocked();
            boolean hasTerminalDeviceState = false;
            this.mDeviceStates.clear();
            for (DeviceState state : supportedDeviceStates) {
                if (state.hasFlag(1)) {
                    hasTerminalDeviceState = true;
                }
                this.mDeviceStates.put(state.getIdentifier(), state);
            }
            this.mOverrideRequestController.setStickyRequestsAllowed(hasTerminalDeviceState);
            int[] newStateIdentifiers = getSupportedStateIdentifiersLocked();
            if (Arrays.equals(oldStateIdentifiers, newStateIdentifiers)) {
                return;
            }
            this.mOverrideRequestController.handleNewSupportedStates(newStateIdentifiers);
            updatePendingStateLocked();
            if (!this.mPendingState.isPresent()) {
                notifyDeviceStateInfoChangedAsync();
            }
            this.mHandler.post(new DeviceStateManagerService$$ExternalSyntheticLambda0(this));
        }
    }

    private boolean isSupportedStateLocked(int identifier) {
        return this.mDeviceStates.contains(identifier);
    }

    private Optional<DeviceState> getStateLocked(int identifier) {
        return Optional.ofNullable(this.mDeviceStates.get(identifier));
    }

    public void setBaseState(int identifier) {
        synchronized (this.mLock) {
            Optional<DeviceState> baseStateOptional = getStateLocked(identifier);
            if (!baseStateOptional.isPresent()) {
                throw new IllegalArgumentException("Base state is not supported");
            }
            DeviceState baseState = baseStateOptional.get();
            if (this.mBaseState.isPresent() && this.mBaseState.get().equals(baseState)) {
                return;
            }
            this.mBaseState = Optional.of(baseState);
            if (baseState.hasFlag(1)) {
                this.mOverrideRequestController.cancelOverrideRequest();
            }
            this.mOverrideRequestController.handleBaseStateChanged();
            updatePendingStateLocked();
            if (!this.mPendingState.isPresent()) {
                notifyDeviceStateInfoChangedAsync();
            }
            this.mHandler.post(new DeviceStateManagerService$$ExternalSyntheticLambda0(this));
        }
    }

    private boolean updatePendingStateLocked() {
        DeviceState stateToConfigure;
        if (this.mPendingState.isPresent()) {
            return false;
        }
        if (this.mActiveOverride.isPresent()) {
            stateToConfigure = getStateLocked(this.mActiveOverride.get().getRequestedState()).get();
        } else if (this.mBaseState.isPresent() && isSupportedStateLocked(this.mBaseState.get().getIdentifier())) {
            stateToConfigure = this.mBaseState.get();
        } else {
            stateToConfigure = null;
        }
        if (stateToConfigure == null) {
            return false;
        }
        if (this.mCommittedState.isPresent() && stateToConfigure.equals(this.mCommittedState.get())) {
            return false;
        }
        this.mPendingState = Optional.of(stateToConfigure);
        this.mIsPolicyWaitingForState = true;
        return true;
    }

    public void notifyPolicyIfNeeded() {
        if (Thread.holdsLock(this.mLock)) {
            Throwable error = new Throwable("Attempting to notify DeviceStatePolicy with service lock held");
            error.fillInStackTrace();
            Slog.w(TAG, error);
        }
        synchronized (this.mLock) {
            if (this.mIsPolicyWaitingForState) {
                this.mIsPolicyWaitingForState = false;
                int state = this.mPendingState.get().getIdentifier();
                this.mDeviceStatePolicy.configureDeviceForState(state, new Runnable() { // from class: com.android.server.devicestate.DeviceStateManagerService$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        DeviceStateManagerService.this.commitPendingState();
                    }
                });
            }
        }
    }

    public void commitPendingState() {
        ProcessRecord processRecord;
        synchronized (this.mLock) {
            DeviceState newState = this.mPendingState.get();
            FrameworkStatsLog.write(350, newState.getIdentifier(), !this.mCommittedState.isPresent());
            this.mCommittedState = Optional.of(newState);
            this.mPendingState = Optional.empty();
            updatePendingStateLocked();
            notifyDeviceStateInfoChangedAsync();
            OverrideRequest activeRequest = this.mActiveOverride.orElse(null);
            if (activeRequest != null && activeRequest.getRequestedState() == newState.getIdentifier() && (processRecord = this.mProcessRecords.get(activeRequest.getPid())) != null) {
                processRecord.notifyRequestActiveAsync(activeRequest.getToken());
            }
            this.mHandler.post(new DeviceStateManagerService$$ExternalSyntheticLambda0(this));
        }
    }

    private void notifyDeviceStateInfoChangedAsync() {
        synchronized (this.mLock) {
            if (this.mProcessRecords.size() == 0) {
                return;
            }
            ArrayList<ProcessRecord> registeredProcesses = new ArrayList<>();
            for (int i = 0; i < this.mProcessRecords.size(); i++) {
                registeredProcesses.add(this.mProcessRecords.valueAt(i));
            }
            DeviceStateInfo info = getDeviceStateInfoLocked();
            for (int i2 = 0; i2 < registeredProcesses.size(); i2++) {
                registeredProcesses.get(i2).notifyDeviceStateInfoAsync(info);
            }
        }
    }

    public void onOverrideRequestStatusChangedLocked(OverrideRequest request, int status) {
        if (status == 1) {
            this.mActiveOverride = Optional.of(request);
        } else if (status == 2) {
            if (this.mActiveOverride.isPresent() && this.mActiveOverride.get() == request) {
                this.mActiveOverride = Optional.empty();
            }
        } else {
            throw new IllegalArgumentException("Unknown request status: " + status);
        }
        boolean updatedPendingState = updatePendingStateLocked();
        ProcessRecord processRecord = this.mProcessRecords.get(request.getPid());
        if (processRecord == null) {
            this.mHandler.post(new DeviceStateManagerService$$ExternalSyntheticLambda0(this));
            return;
        }
        if (status == 1) {
            if (!updatedPendingState && !this.mPendingState.isPresent()) {
                processRecord.notifyRequestActiveAsync(request.getToken());
            }
        } else {
            processRecord.notifyRequestCanceledAsync(request.getToken());
        }
        this.mHandler.post(new DeviceStateManagerService$$ExternalSyntheticLambda0(this));
    }

    public void registerProcess(int pid, IDeviceStateManagerCallback callback) {
        synchronized (this.mLock) {
            if (this.mProcessRecords.contains(pid)) {
                throw new SecurityException("The calling process has already registered an IDeviceStateManagerCallback.");
            }
            ProcessRecord record = new ProcessRecord(callback, pid, new ProcessRecord.DeathListener() { // from class: com.android.server.devicestate.DeviceStateManagerService$$ExternalSyntheticLambda1
                @Override // com.android.server.devicestate.DeviceStateManagerService.ProcessRecord.DeathListener
                public final void onProcessDied(DeviceStateManagerService.ProcessRecord processRecord) {
                    DeviceStateManagerService.this.handleProcessDied(processRecord);
                }
            }, this.mHandler);
            try {
                callback.asBinder().linkToDeath(record, 0);
                this.mProcessRecords.put(pid, record);
                DeviceStateInfo currentInfo = this.mCommittedState.isPresent() ? getDeviceStateInfoLocked() : null;
                if (currentInfo != null) {
                    record.notifyDeviceStateInfoAsync(currentInfo);
                }
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void handleProcessDied(ProcessRecord processRecord) {
        synchronized (this.mLock) {
            this.mProcessRecords.remove(processRecord.mPid);
            this.mOverrideRequestController.handleProcessDied(processRecord.mPid);
        }
    }

    public void requestStateInternal(int state, int flags, int callingPid, IBinder token) {
        synchronized (this.mLock) {
            ProcessRecord processRecord = this.mProcessRecords.get(callingPid);
            if (processRecord == null) {
                throw new IllegalStateException("Process " + callingPid + " has no registered callback.");
            }
            if (this.mOverrideRequestController.hasRequest(token)) {
                throw new IllegalStateException("Request has already been made for the supplied token: " + token);
            }
            Optional<DeviceState> deviceState = getStateLocked(state);
            if (!deviceState.isPresent()) {
                throw new IllegalArgumentException("Requested state: " + state + " is not supported.");
            }
            OverrideRequest request = new OverrideRequest(token, callingPid, state, flags);
            this.mOverrideRequestController.addRequest(request);
        }
    }

    public void cancelStateRequestInternal(int callingPid) {
        synchronized (this.mLock) {
            ProcessRecord processRecord = this.mProcessRecords.get(callingPid);
            if (processRecord == null) {
                throw new IllegalStateException("Process " + callingPid + " has no registered callback.");
            }
            Optional<OverrideRequest> optional = this.mActiveOverride;
            final OverrideRequestController overrideRequestController = this.mOverrideRequestController;
            Objects.requireNonNull(overrideRequestController);
            optional.ifPresent(new Consumer() { // from class: com.android.server.devicestate.DeviceStateManagerService$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    OverrideRequestController.this.cancelRequest((OverrideRequest) obj);
                }
            });
        }
    }

    public void dumpInternal(PrintWriter pw) {
        pw.println("DEVICE STATE MANAGER (dumpsys device_state)");
        synchronized (this.mLock) {
            pw.println("  mCommittedState=" + this.mCommittedState);
            pw.println("  mPendingState=" + this.mPendingState);
            pw.println("  mBaseState=" + this.mBaseState);
            pw.println("  mOverrideState=" + getOverrideState());
            int processCount = this.mProcessRecords.size();
            pw.println();
            pw.println("Registered processes: size=" + processCount);
            for (int i = 0; i < processCount; i++) {
                ProcessRecord processRecord = this.mProcessRecords.valueAt(i);
                pw.println("  " + i + ": mPid=" + processRecord.mPid);
            }
            this.mOverrideRequestController.dumpInternal(pw);
        }
    }

    public void assertCanRequestDeviceState(int callingPid, int state) {
        WindowProcessController topApp = this.mActivityTaskManagerInternal.getTopApp();
        if (topApp == null || topApp.getPid() != callingPid || !isStateAvailableForAppRequests(state)) {
            getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DEVICE_STATE", "Permission required to request device state, or the call must come from the top app and be a device state that is available for apps to request.");
        }
    }

    public void assertCanControlDeviceState(int callingPid) {
        WindowProcessController topApp = this.mActivityTaskManagerInternal.getTopApp();
        if (topApp == null || topApp.getPid() != callingPid) {
            getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DEVICE_STATE", "Permission required to request device state, or the call must come from the top app.");
        }
    }

    private boolean isStateAvailableForAppRequests(int state) {
        boolean contains;
        synchronized (this.mLock) {
            contains = this.mDeviceStatesAvailableForAppRequests.contains(Integer.valueOf(state));
        }
        return contains;
    }

    private void readStatesAvailableForRequestFromApps() {
        this.mDeviceStatesAvailableForAppRequests = new HashSet();
        String[] availableAppStatesConfigIdentifiers = getContext().getResources().getStringArray(17236027);
        for (String identifierToFetch : availableAppStatesConfigIdentifiers) {
            int configValueIdentifier = getContext().getResources().getIdentifier(identifierToFetch, "integer", PackageManagerService.PLATFORM_PACKAGE_NAME);
            int state = getContext().getResources().getInteger(configValueIdentifier);
            if (isValidState(state)) {
                this.mDeviceStatesAvailableForAppRequests.add(Integer.valueOf(state));
            } else {
                Slog.e(TAG, "Invalid device state was found in the configuration file. State id: " + state);
            }
        }
    }

    private boolean isValidState(int state) {
        for (int i = 0; i < this.mDeviceStates.size(); i++) {
            if (state == this.mDeviceStates.valueAt(i).getIdentifier()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DeviceStateProviderListener implements DeviceStateProvider.Listener {
        private DeviceStateProviderListener() {
            DeviceStateManagerService.this = r1;
        }

        @Override // com.android.server.devicestate.DeviceStateProvider.Listener
        public void onSupportedDeviceStatesChanged(DeviceState[] newDeviceStates) {
            if (newDeviceStates.length == 0) {
                throw new IllegalArgumentException("Supported device states must not be empty");
            }
            DeviceStateManagerService.this.updateSupportedStates(newDeviceStates);
        }

        @Override // com.android.server.devicestate.DeviceStateProvider.Listener
        public void onStateChanged(int identifier) {
            if (identifier < 0 || identifier > 255) {
                throw new IllegalArgumentException("Invalid identifier: " + identifier);
            }
            DeviceStateManagerService.this.setBaseState(identifier);
        }
    }

    /* loaded from: classes.dex */
    public static final class ProcessRecord implements IBinder.DeathRecipient {
        private static final int STATUS_ACTIVE = 0;
        private static final int STATUS_CANCELED = 2;
        private static final int STATUS_SUSPENDED = 1;
        private final IDeviceStateManagerCallback mCallback;
        private final DeathListener mDeathListener;
        private final Handler mHandler;
        private final WeakHashMap<IBinder, Integer> mLastNotifiedStatus = new WeakHashMap<>();
        private final int mPid;

        /* loaded from: classes.dex */
        public interface DeathListener {
            void onProcessDied(ProcessRecord processRecord);
        }

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        private @interface RequestStatus {
        }

        ProcessRecord(IDeviceStateManagerCallback callback, int pid, DeathListener deathListener, Handler handler) {
            this.mCallback = callback;
            this.mPid = pid;
            this.mDeathListener = deathListener;
            this.mHandler = handler;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            this.mDeathListener.onProcessDied(this);
        }

        public void notifyDeviceStateInfoAsync(final DeviceStateInfo info) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.devicestate.DeviceStateManagerService$ProcessRecord$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    DeviceStateManagerService.ProcessRecord.this.m3180xd0f82a8c(info);
                }
            });
        }

        /* renamed from: lambda$notifyDeviceStateInfoAsync$0$com-android-server-devicestate-DeviceStateManagerService$ProcessRecord */
        public /* synthetic */ void m3180xd0f82a8c(DeviceStateInfo info) {
            try {
                this.mCallback.onDeviceStateInfoChanged(info);
            } catch (RemoteException ex) {
                Slog.w(DeviceStateManagerService.TAG, "Failed to notify process " + this.mPid + " that device state changed.", ex);
            }
        }

        public void notifyRequestActiveAsync(final IBinder token) {
            Integer lastStatus = this.mLastNotifiedStatus.get(token);
            if (lastStatus != null && (lastStatus.intValue() == 0 || lastStatus.intValue() == 2)) {
                return;
            }
            this.mLastNotifiedStatus.put(token, 0);
            this.mHandler.post(new Runnable() { // from class: com.android.server.devicestate.DeviceStateManagerService$ProcessRecord$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DeviceStateManagerService.ProcessRecord.this.m3181x82c20037(token);
                }
            });
        }

        /* renamed from: lambda$notifyRequestActiveAsync$1$com-android-server-devicestate-DeviceStateManagerService$ProcessRecord */
        public /* synthetic */ void m3181x82c20037(IBinder token) {
            try {
                this.mCallback.onRequestActive(token);
            } catch (RemoteException ex) {
                Slog.w(DeviceStateManagerService.TAG, "Failed to notify process " + this.mPid + " that request state changed.", ex);
            }
        }

        public void notifyRequestCanceledAsync(final IBinder token) {
            Integer lastStatus = this.mLastNotifiedStatus.get(token);
            if (lastStatus == null || lastStatus.intValue() != 2) {
                this.mLastNotifiedStatus.put(token, 2);
                this.mHandler.post(new Runnable() { // from class: com.android.server.devicestate.DeviceStateManagerService$ProcessRecord$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        DeviceStateManagerService.ProcessRecord.this.m3182x72c46729(token);
                    }
                });
            }
        }

        /* renamed from: lambda$notifyRequestCanceledAsync$2$com-android-server-devicestate-DeviceStateManagerService$ProcessRecord */
        public /* synthetic */ void m3182x72c46729(IBinder token) {
            try {
                this.mCallback.onRequestCanceled(token);
            } catch (RemoteException ex) {
                Slog.w(DeviceStateManagerService.TAG, "Failed to notify process " + this.mPid + " that request state changed.", ex);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BinderService extends IDeviceStateManager.Stub {
        private BinderService() {
            DeviceStateManagerService.this = r1;
        }

        public DeviceStateInfo getDeviceStateInfo() {
            DeviceStateInfo deviceStateInfoLocked;
            synchronized (DeviceStateManagerService.this.mLock) {
                deviceStateInfoLocked = DeviceStateManagerService.this.getDeviceStateInfoLocked();
            }
            return deviceStateInfoLocked;
        }

        public void registerCallback(IDeviceStateManagerCallback callback) {
            if (callback == null) {
                throw new IllegalArgumentException("Device state callback must not be null.");
            }
            int callingPid = Binder.getCallingPid();
            long token = Binder.clearCallingIdentity();
            try {
                DeviceStateManagerService.this.registerProcess(callingPid, callback);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void requestState(IBinder token, int state, int flags) {
            int callingPid = Binder.getCallingPid();
            DeviceStateManagerService.this.assertCanRequestDeviceState(callingPid, state);
            if (token == null) {
                throw new IllegalArgumentException("Request token must not be null.");
            }
            long callingIdentity = Binder.clearCallingIdentity();
            try {
                DeviceStateManagerService.this.requestStateInternal(state, flags, callingPid, token);
            } finally {
                Binder.restoreCallingIdentity(callingIdentity);
            }
        }

        public void cancelStateRequest() {
            int callingPid = Binder.getCallingPid();
            DeviceStateManagerService.this.assertCanControlDeviceState(callingPid);
            long callingIdentity = Binder.clearCallingIdentity();
            try {
                DeviceStateManagerService.this.cancelStateRequestInternal(callingPid);
            } finally {
                Binder.restoreCallingIdentity(callingIdentity);
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.devicestate.DeviceStateManagerService$BinderService */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver result) {
            new DeviceStateManagerShellCommand(DeviceStateManagerService.this).exec(this, in, out, err, args, callback, result);
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(DeviceStateManagerService.this.getContext(), DeviceStateManagerService.TAG, pw)) {
                long token = Binder.clearCallingIdentity();
                try {
                    DeviceStateManagerService.this.dumpInternal(pw);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private final class LocalService extends DeviceStateManagerInternal {
        private LocalService() {
            DeviceStateManagerService.this = r1;
        }

        public int[] getSupportedStateIdentifiers() {
            int[] supportedStateIdentifiersLocked;
            synchronized (DeviceStateManagerService.this.mLock) {
                supportedStateIdentifiersLocked = DeviceStateManagerService.this.getSupportedStateIdentifiersLocked();
            }
            return supportedStateIdentifiersLocked;
        }

        public void onHallKeyUp(boolean isUp) {
            DeviceStateManagerService.this.mDeviceStatePolicy.getDeviceStateProvider().setHallKeyStateUp(isUp);
        }

        public void onSystemBootedEnd() {
            DeviceStateManagerService.this.mDeviceStatePolicy.getDeviceStateProvider().onSystemBootedEnd();
        }

        public int getCurrentState() {
            return DeviceStateManagerService.this.mBinderService.getDeviceStateInfo().currentState;
        }
    }
}
