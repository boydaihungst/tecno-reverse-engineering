package com.android.server.display;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.DisplayAddress;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import com.android.server.display.DisplayDeviceRepository;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.layout.Layout;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class LogicalDisplayMapper implements DisplayDeviceRepository.Listener {
    private static final boolean DEBUG;
    public static final int DISPLAY_GROUP_EVENT_ADDED = 1;
    public static final int DISPLAY_GROUP_EVENT_CHANGED = 2;
    public static final int DISPLAY_GROUP_EVENT_REMOVED = 3;
    public static final int LOGICAL_DISPLAY_EVENT_ADDED = 1;
    public static final int LOGICAL_DISPLAY_EVENT_CHANGED = 2;
    public static final int LOGICAL_DISPLAY_EVENT_DEVICE_STATE_TRANSITION = 6;
    public static final int LOGICAL_DISPLAY_EVENT_DUAL_DISPLAY_CLOSED = 62;
    public static final int LOGICAL_DISPLAY_EVENT_DUAL_DISPLAY_OPENED = 61;
    public static final int LOGICAL_DISPLAY_EVENT_FRAME_RATE_OVERRIDES_CHANGED = 5;
    public static final int LOGICAL_DISPLAY_EVENT_REMOVED = 3;
    public static final int LOGICAL_DISPLAY_EVENT_SWAPPED = 4;
    public static final int LOGICAL_DISPLAY_EVENT_UPDATE_POWER_STATE = 60;
    private static final int MSG_TRANSITION_TO_PENDING_DEVICE_STATE = 1;
    private static final String TAG = "LogicalDisplayMapper";
    private static final int UPDATE_STATE_DISABLED = 2;
    private static final int UPDATE_STATE_FLAG_TRANSITION = 256;
    private static final int UPDATE_STATE_MASK = 3;
    private static final int UPDATE_STATE_NEW = 0;
    private static final int UPDATE_STATE_UPDATED = 1;
    public static boolean mDualDisplayFlipSupport;
    public static final boolean mDualDisplaySupport;
    public static boolean mMultipleDisplayFlipSupport;
    private boolean mBootCompleted;
    private int mDeviceState;
    private SparseIntArray mDeviceStateFromTo;
    private SparseArray<DisplayAddress> mDeviceStateToDisplayAddress;
    private final DeviceStateToLayoutMap mDeviceStateToLayoutMap;
    private final SparseBooleanArray mDeviceStatesOnWhichToSleep;
    private final SparseBooleanArray mDeviceStatesOnWhichToWakeUp;
    private final DisplayDeviceRepository mDisplayDeviceRepo;
    private boolean mDualDisplayEnabled;
    private final LogicalDisplayMapperHandler mHandler;
    private boolean mInteractive;
    private final Listener mListener;
    private int mPendingDeviceState;
    private final PowerManager mPowerManager;
    private final boolean mSingleDisplayDemoMode;
    private final boolean mSupportsConcurrentInternalDisplays;
    private final DisplayManagerService.SyncRoot mSyncRoot;
    private final long TIMEOUT_STATE_TRANSITION_MILLIS = TranFoldDisplayCustody.getTimeoutStateTransition(500);
    private final DisplayInfo mTempDisplayInfo = new DisplayInfo();
    private final DisplayInfo mTempNonOverrideDisplayInfo = new DisplayInfo();
    private final SparseArray<LogicalDisplay> mLogicalDisplays = new SparseArray<>();
    private final SparseArray<DisplayGroup> mDisplayGroups = new SparseArray<>();
    private final SparseIntArray mUpdatedLogicalDisplays = new SparseIntArray();
    private final SparseIntArray mUpdatedDisplayGroups = new SparseIntArray();
    private final SparseIntArray mLogicalDisplaysToUpdate = new SparseIntArray();
    private final SparseIntArray mDisplayGroupsToUpdate = new SparseIntArray();
    private int mNextNonDefaultGroupId = 1;
    private Layout mCurrentLayout = null;

    /* loaded from: classes.dex */
    public interface Listener {
        void onDisplayGroupEventLocked(int i, int i2);

        void onLogicalDisplayEventLocked(LogicalDisplay logicalDisplay, int i);

        void onTraversalRequested();
    }

    static {
        DEBUG = SystemProperties.getInt("ro.product.dualdisplay.support", 0) == 1;
        mMultipleDisplayFlipSupport = SystemProperties.getInt("ro.product.multiple_display_flip.support", 0) == 1;
        mDualDisplayFlipSupport = SystemProperties.getInt("ro.product.dual_display_flip.support", 0) == 1;
        mDualDisplaySupport = SystemProperties.getInt("ro.product.dualdisplay.support", 0) == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LogicalDisplayMapper(Context context, DisplayDeviceRepository repo, Listener listener, DisplayManagerService.SyncRoot syncRoot, Handler handler, DeviceStateToLayoutMap deviceStateToLayoutMap) {
        this.mDeviceState = mDualDisplaySupport ? 99 : -1;
        this.mPendingDeviceState = -1;
        this.mBootCompleted = false;
        this.mDeviceStateFromTo = new SparseIntArray();
        this.mDeviceStateToDisplayAddress = new SparseArray<>();
        this.mDualDisplayEnabled = false;
        this.mSyncRoot = syncRoot;
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mPowerManager = powerManager;
        this.mInteractive = powerManager.isInteractive();
        this.mHandler = new LogicalDisplayMapperHandler(handler.getLooper());
        this.mDisplayDeviceRepo = repo;
        this.mListener = listener;
        this.mSingleDisplayDemoMode = SystemProperties.getBoolean("persist.demo.singledisplay", false);
        this.mSupportsConcurrentInternalDisplays = context.getResources().getBoolean(17891779);
        this.mDeviceStatesOnWhichToWakeUp = toSparseBooleanArray(context.getResources().getIntArray(17236029));
        this.mDeviceStatesOnWhichToSleep = toSparseBooleanArray(context.getResources().getIntArray(17236028));
        repo.addListener(this);
        this.mDeviceStateToLayoutMap = deviceStateToLayoutMap;
        if (mMultipleDisplayFlipSupport) {
            initForFlip();
        }
    }

    @Override // com.android.server.display.DisplayDeviceRepository.Listener
    public void onDisplayDeviceEventLocked(DisplayDevice device, int event) {
        switch (event) {
            case 1:
                if (DEBUG) {
                    Slog.d(TAG, "Display device added: " + device.getDisplayDeviceInfoLocked());
                }
                handleDisplayDeviceAddedLocked(device);
                return;
            case 2:
                if (DEBUG) {
                    Slog.d(TAG, "Display device changed: " + device.getDisplayDeviceInfoLocked());
                }
                finishStateTransitionLocked(false);
                updateLogicalDisplaysLocked();
                return;
            case 3:
                if (DEBUG) {
                    Slog.d(TAG, "Display device removed: " + device.getDisplayDeviceInfoLocked());
                }
                handleDisplayDeviceRemovedLocked(device);
                updateLogicalDisplaysLocked();
                return;
            default:
                return;
        }
    }

    @Override // com.android.server.display.DisplayDeviceRepository.Listener
    public void onTraversalRequested() {
        this.mListener.onTraversalRequested();
    }

    public LogicalDisplay getDisplayLocked(int displayId) {
        return getDisplayLocked(displayId, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LogicalDisplay getDisplayLocked(int displayId, boolean includeDisabled) {
        LogicalDisplay display = this.mLogicalDisplays.get(displayId);
        if (display != null) {
            if (display.isEnabled() || includeDisabled) {
                return display;
            }
            return null;
        }
        return null;
    }

    public LogicalDisplay getDisplayLocked(DisplayDevice device) {
        return getDisplayLocked(device, false);
    }

    private LogicalDisplay getDisplayLocked(DisplayDevice device, boolean includeDisabled) {
        if (device == null) {
            return null;
        }
        int count = this.mLogicalDisplays.size();
        for (int i = 0; i < count; i++) {
            LogicalDisplay display = this.mLogicalDisplays.valueAt(i);
            if (display.getPrimaryDisplayDeviceLocked() == device) {
                if (!display.isEnabled() && !includeDisabled) {
                    return null;
                } else {
                    return display;
                }
            }
        }
        return null;
    }

    public int[] getDisplayIdsLocked(int callingUid) {
        return getDisplayIdsLocked(callingUid, false);
    }

    public int[] getDisplayIdsLocked(int callingUid, boolean includeDisabledDisplays) {
        int count = this.mLogicalDisplays.size();
        int[] displayIds = new int[count];
        int n = 0;
        for (int i = 0; i < count; i++) {
            LogicalDisplay display = this.mLogicalDisplays.valueAt(i);
            if (includeDisabledDisplays || display.isEnabled()) {
                DisplayInfo info = display.getDisplayInfoLocked();
                if (info.hasAccess(callingUid)) {
                    displayIds[n] = this.mLogicalDisplays.keyAt(i);
                    n++;
                }
            }
        }
        if (n != count) {
            return Arrays.copyOfRange(displayIds, 0, n);
        }
        return displayIds;
    }

    public void forEachLocked(Consumer<LogicalDisplay> consumer) {
        int count = this.mLogicalDisplays.size();
        for (int i = 0; i < count; i++) {
            LogicalDisplay display = this.mLogicalDisplays.valueAt(i);
            if (display.isEnabled()) {
                consumer.accept(display);
            }
        }
    }

    public int getDisplayGroupIdFromDisplayIdLocked(int displayId) {
        LogicalDisplay display = getDisplayLocked(displayId);
        if (display == null) {
            return -1;
        }
        int size = this.mDisplayGroups.size();
        for (int i = 0; i < size; i++) {
            DisplayGroup displayGroup = this.mDisplayGroups.valueAt(i);
            if (displayGroup.containsLocked(display)) {
                return this.mDisplayGroups.keyAt(i);
            }
        }
        return -1;
    }

    public DisplayGroup getDisplayGroupLocked(int groupId) {
        return this.mDisplayGroups.get(groupId);
    }

    public Set<DisplayInfo> getDisplayInfoForStateLocked(int deviceState, int displayId, int groupId) {
        Set<DisplayInfo> displayInfos = new ArraySet<>();
        Layout layout = this.mDeviceStateToLayoutMap.get(deviceState);
        int layoutSize = layout.size();
        for (int i = 0; i < layoutSize; i++) {
            Layout.Display displayLayout = layout.getAt(i);
            if (displayLayout != null) {
                DisplayAddress address = displayLayout.getAddress();
                DisplayDevice device = this.mDisplayDeviceRepo.getByAddressLocked(address);
                if (device == null) {
                    Slog.w(TAG, "The display device (" + address + "), is not available for the display state " + deviceState);
                } else {
                    int logicalDisplayId = displayLayout.getLogicalDisplayId();
                    LogicalDisplay logicalDisplay = getDisplayLocked(logicalDisplayId, true);
                    if (logicalDisplay == null) {
                        Slog.w(TAG, "The logical display (" + address + "), is not available for the display state " + deviceState);
                    } else {
                        DisplayInfo temp = logicalDisplay.getDisplayInfoLocked();
                        DisplayInfo displayInfo = new DisplayInfo(temp);
                        if (displayInfo.displayGroupId == groupId) {
                            displayInfo.displayId = displayId;
                            displayInfos.add(displayInfo);
                        }
                    }
                }
            }
        }
        return displayInfos;
    }

    public void dumpLocked(PrintWriter pw) {
        pw.println("LogicalDisplayMapper:");
        PrintWriter indentingPrintWriter = new IndentingPrintWriter(pw, "  ");
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.println("mSingleDisplayDemoMode=" + this.mSingleDisplayDemoMode);
        indentingPrintWriter.println("mCurrentLayout=" + this.mCurrentLayout);
        indentingPrintWriter.println("mDeviceStatesOnWhichToWakeUp=" + this.mDeviceStatesOnWhichToWakeUp);
        indentingPrintWriter.println("mDeviceStatesOnWhichToSleep=" + this.mDeviceStatesOnWhichToSleep);
        indentingPrintWriter.println("mInteractive=" + this.mInteractive);
        indentingPrintWriter.println("mDualDisplayEnabled=" + this.mDualDisplayEnabled);
        int logicalDisplayCount = this.mLogicalDisplays.size();
        indentingPrintWriter.println();
        indentingPrintWriter.println("Logical Displays: size=" + logicalDisplayCount);
        for (int i = 0; i < logicalDisplayCount; i++) {
            int displayId = this.mLogicalDisplays.keyAt(i);
            LogicalDisplay display = this.mLogicalDisplays.valueAt(i);
            indentingPrintWriter.println("Display " + displayId + ":");
            indentingPrintWriter.increaseIndent();
            display.dumpLocked(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
        }
        this.mDeviceStateToLayoutMap.dumpLocked(indentingPrintWriter);
    }

    public int getDualDeviceStateLocked() {
        return this.mDeviceStateToLayoutMap.getDualDeviceStateLocked(this.mDeviceState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDualDisplayEnabled() {
        return this.mDualDisplayEnabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeviceStateLocked(int state, boolean isOverrideActive) {
        int i;
        Slog.i(TAG, "Requesting Transition to state: " + state + ", from state=" + this.mDeviceState + ", interactive=" + this.mInteractive);
        IDisplayManagerServiceLice.Instance().setDeviceStateLockedEnter(state, this.mPendingDeviceState, this.mDeviceState, this.mInteractive, this.mBootCompleted);
        TranFoldDisplayCustody.instance().setDeviceStateLockedBegin(this);
        int i2 = this.mDeviceState;
        if (i2 != -1) {
            resetLayoutLocked(i2, state, 0);
        }
        this.mPendingDeviceState = state;
        boolean wakeDevice = shouldDeviceBeWoken(state, this.mDeviceState, this.mInteractive, this.mBootCompleted);
        boolean sleepDevice = shouldDeviceBePutToSleep(this.mPendingDeviceState, this.mDeviceState, isOverrideActive, this.mInteractive, this.mBootCompleted);
        if (areAllTransitioningDisplaysOffLocked() && !wakeDevice && !sleepDevice) {
            transitionToPendingStateLocked();
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "Postponing transition to state: " + this.mPendingDeviceState);
        }
        updateLogicalDisplaysLocked();
        if (mMultipleDisplayFlipSupport && this.mBootCompleted && this.mInteractive && (((i = this.mDeviceState) == 1 || i == 2) && state == 0)) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.LogicalDisplayMapper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    LogicalDisplayMapper.this.m3473xa33c5b2d();
                }
            });
        }
        if (wakeDevice || sleepDevice) {
            TranFoldDisplayCustody.instance().wakeOrSleepDeviceWhenStateChanged(wakeDevice, sleepDevice);
            if (wakeDevice) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.display.LogicalDisplayMapper$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        LogicalDisplayMapper.this.m3474xa2c5f52e();
                    }
                });
            } else if (sleepDevice) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.display.LogicalDisplayMapper$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        LogicalDisplayMapper.this.m3475xa24f8f2f();
                    }
                });
            }
        }
        this.mHandler.sendEmptyMessageDelayed(1, this.TIMEOUT_STATE_TRANSITION_MILLIS);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setDeviceStateLocked$0$com-android-server-display-LogicalDisplayMapper  reason: not valid java name */
    public /* synthetic */ void m3473xa33c5b2d() {
        this.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 13, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setDeviceStateLocked$1$com-android-server-display-LogicalDisplayMapper  reason: not valid java name */
    public /* synthetic */ void m3474xa2c5f52e() {
        this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 12, "server.display:unfold");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setDeviceStateLocked$2$com-android-server-display-LogicalDisplayMapper  reason: not valid java name */
    public /* synthetic */ void m3475xa24f8f2f() {
        this.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 13, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBootCompleted() {
        synchronized (this.mSyncRoot) {
            this.mBootCompleted = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onEarlyInteractivityChange(boolean interactive) {
        synchronized (this.mSyncRoot) {
            if (this.mInteractive != interactive) {
                this.mInteractive = interactive;
                finishStateTransitionLocked(false);
            }
        }
    }

    boolean shouldDeviceBeWoken(int pendingState, int currentState, boolean isInteractive, boolean isBootCompleted) {
        return this.mDeviceStatesOnWhichToWakeUp.get(pendingState) && !this.mDeviceStatesOnWhichToWakeUp.get(currentState) && !isInteractive && isBootCompleted;
    }

    boolean shouldDeviceBePutToSleep(int pendingState, int currentState, boolean isOverrideActive, boolean isInteractive, boolean isBootCompleted) {
        return this.mDeviceStatesOnWhichToSleep.get(pendingState) && !this.mDeviceStatesOnWhichToSleep.get(currentState) && !isOverrideActive && isInteractive && isBootCompleted;
    }

    private boolean areAllTransitioningDisplaysOffLocked() {
        DisplayDevice device;
        int count = this.mLogicalDisplays.size();
        for (int i = 0; i < count; i++) {
            LogicalDisplay display = this.mLogicalDisplays.valueAt(i);
            if (display.getPhase() == 0 && (device = display.getPrimaryDisplayDeviceLocked()) != null) {
                DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
                if (info.state != 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private void transitionToPendingStateLocked() {
        TranFoldDisplayCustody.instance().transitionToPendingStateLockedBegin();
        resetLayoutLocked(this.mDeviceState, this.mPendingDeviceState, 1);
        Slog.d(TAG, "mDualDisplayEnabled: " + this.mDualDisplayEnabled);
        if (mDualDisplaySupport || mMultipleDisplayFlipSupport) {
            if (this.mPendingDeviceState == getDualDeviceStateLocked()) {
                this.mDualDisplayEnabled = true;
            } else {
                this.mDualDisplayEnabled = false;
            }
        }
        this.mDeviceState = this.mPendingDeviceState;
        this.mPendingDeviceState = -1;
        if (mMultipleDisplayFlipSupport) {
            for (int i = 0; i < this.mLogicalDisplays.size(); i++) {
                LogicalDisplay logicalDisplay = this.mLogicalDisplays.valueAt(i);
                logicalDisplay.setDeviceState(this.mDeviceState);
            }
        }
        applyLayoutLocked();
        updateLogicalDisplaysLocked();
        TranFoldDisplayCustody.instance().transitionToPendingStateLockedEnd();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finishStateTransitionLocked(boolean force) {
        int i = this.mPendingDeviceState;
        if (i == -1) {
            return;
        }
        boolean isReadyToTransition = false;
        boolean waitingToWakeDevice = this.mDeviceStatesOnWhichToWakeUp.get(i) && !this.mDeviceStatesOnWhichToWakeUp.get(this.mDeviceState) && !this.mInteractive && this.mBootCompleted;
        boolean waitingToSleepDevice = this.mDeviceStatesOnWhichToSleep.get(this.mPendingDeviceState) && !this.mDeviceStatesOnWhichToSleep.get(this.mDeviceState) && this.mInteractive && this.mBootCompleted;
        boolean displaysOff = areAllTransitioningDisplaysOffLocked();
        if (displaysOff && !waitingToWakeDevice && !waitingToSleepDevice) {
            isReadyToTransition = true;
        }
        if (isReadyToTransition || force) {
            transitionToPendingStateLocked();
            this.mHandler.removeMessages(1);
        } else if (DEBUG) {
            Slog.d(TAG, "Not yet ready to transition to state=" + this.mPendingDeviceState + " with displays-off=" + displaysOff + ", force=" + force + ", mInteractive=" + this.mInteractive + ", isReady=" + isReadyToTransition);
        }
    }

    private void handleDisplayDeviceAddedLocked(DisplayDevice device) {
        DisplayDeviceInfo deviceInfo = device.getDisplayDeviceInfoLocked();
        if ((deviceInfo.flags & 1) != 0) {
            initializeDefaultDisplayDeviceLocked(device);
        }
        createNewLogicalDisplayLocked(device, Layout.assignDisplayIdLocked(false));
        applyLayoutLocked();
        updateLogicalDisplaysLocked();
    }

    private void handleDisplayDeviceRemovedLocked(DisplayDevice device) {
        Layout layout = this.mDeviceStateToLayoutMap.get(-1);
        Layout.Display layoutDisplay = layout.getById(0);
        if (layoutDisplay == null) {
            return;
        }
        DisplayDeviceInfo deviceInfo = device.getDisplayDeviceInfoLocked();
        if (layoutDisplay.getAddress().equals(deviceInfo.address)) {
            layout.removeDisplayLocked(0);
            for (int i = 0; i < this.mLogicalDisplays.size(); i++) {
                LogicalDisplay nextDisplay = this.mLogicalDisplays.valueAt(i);
                DisplayDevice nextDevice = nextDisplay.getPrimaryDisplayDeviceLocked();
                if (nextDevice != null) {
                    DisplayDeviceInfo nextDeviceInfo = nextDevice.getDisplayDeviceInfoLocked();
                    if ((nextDeviceInfo.flags & 1) != 0 && !nextDeviceInfo.address.equals(deviceInfo.address)) {
                        layout.createDisplayLocked(nextDeviceInfo.address, true, true);
                        applyLayoutLocked();
                        return;
                    }
                }
            }
        }
    }

    private void updateLogicalDisplaysLocked() {
        DisplayDevice device;
        for (int i = this.mLogicalDisplays.size() - 1; i >= 0; i--) {
            int displayId = this.mLogicalDisplays.keyAt(i);
            LogicalDisplay display = this.mLogicalDisplays.valueAt(i);
            this.mTempDisplayInfo.copyFrom(display.getDisplayInfoLocked());
            display.getNonOverrideDisplayInfoLocked(this.mTempNonOverrideDisplayInfo);
            display.updateLocked(this.mDisplayDeviceRepo);
            DisplayInfo newDisplayInfo = display.getDisplayInfoLocked();
            int storedState = this.mUpdatedLogicalDisplays.get(displayId, 0);
            int updateState = storedState & 3;
            boolean isTransitioning = (storedState & 256) != 0;
            boolean wasPreviouslyUpdated = updateState == 1;
            if (!display.isValidLocked()) {
                this.mUpdatedLogicalDisplays.delete(displayId);
                DisplayGroup displayGroup = getDisplayGroupLocked(getDisplayGroupIdFromDisplayIdLocked(displayId));
                if (displayGroup != null) {
                    displayGroup.removeDisplayLocked(display);
                }
                if (wasPreviouslyUpdated) {
                    Slog.i(TAG, "Removing display: " + displayId);
                    this.mLogicalDisplaysToUpdate.put(displayId, 3);
                } else {
                    this.mLogicalDisplays.removeAt(i);
                }
            } else if (!display.isEnabled() || (display.getPhase() == 0 && updateState == 2)) {
                this.mUpdatedLogicalDisplays.put(displayId, 2);
                if (!wasPreviouslyUpdated) {
                    if (!display.isEnabled() && (device = display.getPrimaryDisplayDeviceLocked()) != null && device.getUniqueId().startsWith("local:")) {
                        SurfaceControl.Transaction t = new SurfaceControl.Transaction();
                        device.setLayerStackLocked(t, -1);
                        t.apply();
                    }
                } else {
                    DisplayGroup displayGroup2 = getDisplayGroupLocked(getDisplayGroupIdFromDisplayIdLocked(displayId));
                    if (displayGroup2 != null) {
                        displayGroup2.removeDisplayLocked(display);
                    }
                    Slog.i(TAG, "Removing (disabled) display: " + displayId);
                    this.mLogicalDisplaysToUpdate.put(displayId, 3);
                }
            } else {
                if (!wasPreviouslyUpdated) {
                    Slog.i(TAG, "Adding new display: " + displayId + ": " + newDisplayInfo);
                    assignDisplayGroupLocked(display);
                    this.mLogicalDisplaysToUpdate.put(displayId, 1);
                    if (mDualDisplaySupport && displayId != 0 && display.getPrimaryDisplayDeviceLocked().getUniqueId().startsWith("local:")) {
                        display.setAlwaysUnlocked();
                        display.setRemoveMode(1);
                        display.setDualDisplay(true);
                        if (this.mBootCompleted) {
                            display.setDualDisplayPowerController(true);
                        }
                        Slog.d(TAG, "new displayId: " + displayId + " removeMode: 1 ownContentOnly: true");
                    }
                } else if (!TextUtils.equals(this.mTempDisplayInfo.uniqueId, newDisplayInfo.uniqueId)) {
                    assignDisplayGroupLocked(display);
                    this.mLogicalDisplaysToUpdate.put(displayId, 4);
                } else if (!this.mTempDisplayInfo.equals(newDisplayInfo) && !isTransitioning) {
                    assignDisplayGroupLocked(display);
                    this.mLogicalDisplaysToUpdate.put(displayId, 2);
                } else if (isTransitioning) {
                    this.mLogicalDisplaysToUpdate.put(displayId, 6);
                } else if (!display.getPendingFrameRateOverrideUids().isEmpty()) {
                    this.mLogicalDisplaysToUpdate.put(displayId, 5);
                } else {
                    display.getNonOverrideDisplayInfoLocked(this.mTempDisplayInfo);
                    if (!this.mTempNonOverrideDisplayInfo.equals(this.mTempDisplayInfo)) {
                        this.mLogicalDisplaysToUpdate.put(displayId, 2);
                    }
                    if (mMultipleDisplayFlipSupport) {
                        this.mLogicalDisplaysToUpdate.put(displayId, 60);
                        if (mDualDisplayFlipSupport && displayId == 1) {
                            if (this.mDualDisplayEnabled) {
                                this.mLogicalDisplaysToUpdate.put(displayId, 61);
                            } else {
                                this.mLogicalDisplaysToUpdate.put(displayId, 62);
                            }
                        }
                    }
                }
                this.mUpdatedLogicalDisplays.put(displayId, 1);
            }
        }
        for (int i2 = this.mDisplayGroups.size() - 1; i2 >= 0; i2--) {
            int groupId = this.mDisplayGroups.keyAt(i2);
            DisplayGroup group = this.mDisplayGroups.valueAt(i2);
            boolean wasPreviouslyUpdated2 = this.mUpdatedDisplayGroups.indexOfKey(groupId) > -1;
            int changeCount = group.getChangeCountLocked();
            if (group.isEmptyLocked()) {
                this.mUpdatedDisplayGroups.delete(groupId);
                if (wasPreviouslyUpdated2) {
                    this.mDisplayGroupsToUpdate.put(groupId, 3);
                }
            } else {
                if (!wasPreviouslyUpdated2) {
                    this.mDisplayGroupsToUpdate.put(groupId, 1);
                } else if (this.mUpdatedDisplayGroups.get(groupId) != changeCount) {
                    this.mDisplayGroupsToUpdate.put(groupId, 2);
                }
                this.mUpdatedDisplayGroups.put(groupId, changeCount);
            }
        }
        sendUpdatesForDisplaysLocked(6);
        sendUpdatesForGroupsLocked(1);
        sendUpdatesForDisplaysLocked(3);
        sendUpdatesForDisplaysLocked(2);
        sendUpdatesForDisplaysLocked(5);
        sendUpdatesForDisplaysLocked(4);
        sendUpdatesForDisplaysLocked(1);
        if (mMultipleDisplayFlipSupport) {
            sendUpdatesForDisplaysLocked(60);
            sendUpdatesForDisplaysLocked(61);
            sendUpdatesForDisplaysLocked(62);
        }
        sendUpdatesForGroupsLocked(2);
        sendUpdatesForGroupsLocked(3);
        this.mLogicalDisplaysToUpdate.clear();
        this.mDisplayGroupsToUpdate.clear();
    }

    private void sendUpdatesForDisplaysLocked(int msg) {
        for (int i = this.mLogicalDisplaysToUpdate.size() - 1; i >= 0; i--) {
            int currMsg = this.mLogicalDisplaysToUpdate.valueAt(i);
            if (currMsg == msg) {
                int id = this.mLogicalDisplaysToUpdate.keyAt(i);
                LogicalDisplay display = getDisplayLocked(id, true);
                if (DEBUG) {
                    DisplayDevice device = display.getPrimaryDisplayDeviceLocked();
                    String uniqueId = device == null ? "null" : device.getUniqueId();
                    Slog.d(TAG, "Sending " + displayEventToString(msg) + " for display=" + id + " with device=" + uniqueId);
                }
                this.mListener.onLogicalDisplayEventLocked(display, msg);
                if (msg == 3 && !display.isValidLocked()) {
                    this.mLogicalDisplays.delete(id);
                }
            }
        }
    }

    private void sendUpdatesForGroupsLocked(int msg) {
        for (int i = this.mDisplayGroupsToUpdate.size() - 1; i >= 0; i--) {
            int currMsg = this.mDisplayGroupsToUpdate.valueAt(i);
            if (currMsg == msg) {
                int id = this.mDisplayGroupsToUpdate.keyAt(i);
                this.mListener.onDisplayGroupEventLocked(id, msg);
                if (msg == 3) {
                    this.mDisplayGroups.delete(id);
                }
            }
        }
    }

    private void assignDisplayGroupLocked(LogicalDisplay display) {
        int displayId = display.getDisplayIdLocked();
        int groupId = getDisplayGroupIdFromDisplayIdLocked(displayId);
        DisplayGroup oldGroup = getDisplayGroupLocked(groupId);
        DisplayInfo info = display.getDisplayInfoLocked();
        boolean needsOwnDisplayGroup = (info.flags & 256) != 0;
        boolean hasOwnDisplayGroup = groupId != 0;
        if (groupId == -1 || hasOwnDisplayGroup != needsOwnDisplayGroup) {
            groupId = assignDisplayGroupIdLocked(needsOwnDisplayGroup);
        }
        DisplayGroup newGroup = getDisplayGroupLocked(groupId);
        if (newGroup == null) {
            newGroup = new DisplayGroup(groupId);
            this.mDisplayGroups.append(groupId, newGroup);
        }
        if (oldGroup != newGroup) {
            if (oldGroup != null) {
                oldGroup.removeDisplayLocked(display);
            }
            newGroup.addDisplayLocked(display);
            display.updateDisplayGroupIdLocked(groupId);
            Slog.i(TAG, "Setting new display group " + groupId + " for display " + displayId + ", from previous group: " + (oldGroup != null ? Integer.valueOf(oldGroup.getGroupId()) : "null"));
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:57:0x00e9  */
    /* JADX WARN: Removed duplicated region for block: B:64:0x00f7 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void resetLayoutLocked(int fromState, int toState, int phase) {
        Layout fromLayout;
        boolean willBeEnabled;
        boolean isTransitioning;
        boolean z;
        LogicalDisplayMapper logicalDisplayMapper = this;
        int i = fromState;
        int i2 = toState;
        Layout fromLayout2 = logicalDisplayMapper.mDeviceStateToLayoutMap.get(i);
        Layout toLayout = logicalDisplayMapper.mDeviceStateToLayoutMap.get(i2);
        int count = logicalDisplayMapper.mLogicalDisplays.size();
        int i3 = 0;
        while (i3 < count) {
            LogicalDisplay logicalDisplay = logicalDisplayMapper.mLogicalDisplays.valueAt(i3);
            int displayId = logicalDisplay.getDisplayIdLocked();
            DisplayDevice device = logicalDisplay.getPrimaryDisplayDeviceLocked();
            if (device == null) {
                fromLayout = fromLayout2;
            } else {
                DisplayAddress address = device.getDisplayDeviceInfoLocked().address;
                Layout.Display fromDisplay = address != null ? fromLayout2.getByAddress(address) : null;
                Layout.Display toDisplay = address != null ? toLayout.getByAddress(address) : null;
                boolean wasEnabled = fromDisplay == null || fromDisplay.isEnabled();
                boolean willBeEnabled2 = toDisplay == null || toDisplay.isEnabled();
                boolean deviceHasNewLogicalDisplayId = (fromDisplay == null || toDisplay == null || fromDisplay.getLogicalDisplayId() == toDisplay.getLogicalDisplayId()) ? false : true;
                if (logicalDisplay.getPhase() != 0) {
                    fromLayout = fromLayout2;
                    willBeEnabled = willBeEnabled2;
                    if (wasEnabled == willBeEnabled && !deviceHasNewLogicalDisplayId) {
                        isTransitioning = false;
                        if (mMultipleDisplayFlipSupport || address == null) {
                            z = false;
                        } else {
                            z = logicalDisplayMapper.mDeviceStateFromTo.get(i) == i2 && address.equals(logicalDisplayMapper.mDeviceStateToDisplayAddress.get(i));
                        }
                        boolean needTransitionForFlip = z;
                        Slog.d(TAG, "display=" + displayId + " NeedTransition=" + needTransitionForFlip);
                        if (!isTransitioning || needTransitionForFlip) {
                            logicalDisplayMapper.setDisplayPhase(logicalDisplay, phase);
                            if (phase != 0) {
                                int oldState = logicalDisplayMapper.mUpdatedLogicalDisplays.get(displayId, 0);
                                logicalDisplayMapper.mUpdatedLogicalDisplays.put(displayId, oldState | 256);
                            }
                        }
                    }
                } else {
                    fromLayout = fromLayout2;
                    willBeEnabled = willBeEnabled2;
                }
                isTransitioning = true;
                if (mMultipleDisplayFlipSupport) {
                }
                z = false;
                boolean needTransitionForFlip2 = z;
                Slog.d(TAG, "display=" + displayId + " NeedTransition=" + needTransitionForFlip2);
                if (!isTransitioning) {
                }
                logicalDisplayMapper.setDisplayPhase(logicalDisplay, phase);
                if (phase != 0) {
                }
            }
            i3++;
            logicalDisplayMapper = this;
            i = fromState;
            i2 = toState;
            fromLayout2 = fromLayout;
        }
    }

    private void applyLayoutLocked() {
        Layout oldLayout = this.mCurrentLayout;
        this.mCurrentLayout = this.mDeviceStateToLayoutMap.get(this.mDeviceState);
        Slog.i(TAG, "Applying layout: " + this.mCurrentLayout + ", Previous layout: " + oldLayout);
        int size = this.mCurrentLayout.size();
        for (int i = 0; i < size; i++) {
            Layout.Display displayLayout = this.mCurrentLayout.getAt(i);
            DisplayAddress address = displayLayout.getAddress();
            DisplayDevice device = this.mDisplayDeviceRepo.getByAddressLocked(address);
            if (device == null) {
                Slog.w(TAG, "The display device (" + address + "), is not available for the display state " + this.mDeviceState);
            } else {
                int logicalDisplayId = displayLayout.getLogicalDisplayId();
                LogicalDisplay newDisplay = getDisplayLocked(logicalDisplayId, true);
                if (newDisplay == null) {
                    newDisplay = createNewLogicalDisplayLocked(null, logicalDisplayId);
                }
                LogicalDisplay oldDisplay = getDisplayLocked(device, true);
                if (newDisplay != oldDisplay) {
                    newDisplay.swapDisplaysLocked(oldDisplay);
                }
                if (!displayLayout.isEnabled()) {
                    setDisplayPhase(newDisplay, -1);
                }
                TranFoldDisplayCustody.instance().applyLayoutLocked(displayLayout);
            }
        }
    }

    private LogicalDisplay createNewLogicalDisplayLocked(DisplayDevice displayDevice, int displayId) {
        int layerStack = assignLayerStackLocked(displayId);
        LogicalDisplay display = new LogicalDisplay(displayId, layerStack, displayDevice);
        display.updateLocked(this.mDisplayDeviceRepo);
        this.mLogicalDisplays.put(displayId, display);
        setDisplayPhase(display, 1);
        return display;
    }

    private void setDisplayPhase(LogicalDisplay display, int phase) {
        display.getDisplayIdLocked();
        DisplayInfo info = display.getDisplayInfoLocked();
        boolean z = true;
        boolean disallowSecondaryDisplay = (!this.mSingleDisplayDemoMode || info.type == 1) ? false : false;
        if (phase != -1 && disallowSecondaryDisplay) {
            Slog.i(TAG, "Not creating a logical display for a secondary display because single display demo mode is enabled: " + display.getDisplayInfoLocked());
            phase = -1;
        }
        display.setPhase(phase);
    }

    private int assignDisplayGroupIdLocked(boolean isOwnDisplayGroup) {
        if (isOwnDisplayGroup) {
            int i = this.mNextNonDefaultGroupId;
            this.mNextNonDefaultGroupId = i + 1;
            return i;
        }
        return 0;
    }

    private void initializeDefaultDisplayDeviceLocked(DisplayDevice device) {
        Layout layout = this.mDeviceStateToLayoutMap.get(-1);
        if (layout.getById(0) != null) {
            return;
        }
        DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
        layout.createDisplayLocked(info.address, true, true);
    }

    private int assignLayerStackLocked(int displayId) {
        return displayId;
    }

    private void initForFlip() {
        this.mDeviceStateFromTo.put(1, 0);
        this.mDeviceStateFromTo.put(2, 0);
        this.mDeviceStateFromTo.put(0, 1);
        this.mDeviceStateToDisplayAddress.put(1, DisplayAddress.fromPhysicalDisplayId(4627039422300187648L));
        this.mDeviceStateToDisplayAddress.put(2, DisplayAddress.fromPhysicalDisplayId(4627039422300187648L));
        this.mDeviceStateToDisplayAddress.put(0, DisplayAddress.fromPhysicalDisplayId(4627039422300187651L));
    }

    private SparseBooleanArray toSparseBooleanArray(int[] input) {
        SparseBooleanArray retval = new SparseBooleanArray(2);
        for (int i = 0; input != null && i < input.length; i++) {
            retval.put(input[i], true);
        }
        return retval;
    }

    private String displayEventToString(int msg) {
        switch (msg) {
            case 1:
                return "added";
            case 2:
                return "changed";
            case 3:
                return "removed";
            case 4:
                return "swapped";
            case 5:
                return "framerate_override";
            case 6:
                return "transition";
            case 60:
                return "update_power_state";
            case 61:
                return "dual_display_opened";
            case 62:
                return "dual_display_closed";
            default:
                return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class LogicalDisplayMapperHandler extends Handler {
        LogicalDisplayMapperHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (LogicalDisplayMapper.this.mSyncRoot) {
                        LogicalDisplayMapper.this.finishStateTransitionLocked(true);
                    }
                    return;
                default:
                    return;
            }
        }
    }
}
