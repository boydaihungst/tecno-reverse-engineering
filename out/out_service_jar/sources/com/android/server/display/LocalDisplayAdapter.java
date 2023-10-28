package com.android.server.display;

import android.app.ActivityThread;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.sidekick.SidekickInternal;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.DisplayUtils;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayAddress;
import android.view.DisplayCutout;
import android.view.DisplayEventReceiver;
import android.view.RoundedCorners;
import android.view.SurfaceControl;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalServices;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.DisplayModeDirector;
import com.android.server.display.LocalDisplayAdapter;
import com.android.server.lights.LightsManager;
import com.android.server.lights.LogicalLight;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class LocalDisplayAdapter extends DisplayAdapter {
    private static final boolean DEBUG = SystemProperties.getBoolean("dbg.dms.lda", false);
    private static final boolean MTK_DEBUG = "eng".equals(Build.TYPE);
    private static final String PROPERTY_EMULATOR_CIRCULAR = "ro.emulator.circular";
    private static final String TAG = "LocalDisplayAdapter";
    private static final String UNIQUE_ID_PREFIX = "local:";
    private final LongSparseArray<LocalDisplayDevice> mDevices;
    private final Injector mInjector;
    private final boolean mIsBootDisplayModeSupported;
    private Context mOverlayContext;
    private final SurfaceControlProxy mSurfaceControlProxy;

    /* loaded from: classes.dex */
    public interface DisplayEventListener {
        void onFrameRateOverridesChanged(long j, long j2, DisplayEventReceiver.FrameRateOverride[] frameRateOverrideArr);

        void onHotplug(long j, long j2, boolean z);

        void onModeChanged(long j, long j2, int i);
    }

    public LocalDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener) {
        this(syncRoot, context, handler, listener, new Injector());
    }

    LocalDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener, Injector injector) {
        super(syncRoot, context, handler, listener, TAG);
        this.mDevices = new LongSparseArray<>();
        this.mInjector = injector;
        SurfaceControlProxy surfaceControlProxy = injector.getSurfaceControlProxy();
        this.mSurfaceControlProxy = surfaceControlProxy;
        this.mIsBootDisplayModeSupported = surfaceControlProxy.getBootDisplayModeSupport();
    }

    @Override // com.android.server.display.DisplayAdapter
    public void registerLocked() {
        long[] physicalDisplayIds;
        super.registerLocked();
        this.mInjector.setDisplayEventListenerLocked(getHandler().getLooper(), new LocalDisplayEventListener());
        for (long physicalDisplayId : this.mSurfaceControlProxy.getPhysicalDisplayIds()) {
            tryConnectDisplayLocked(physicalDisplayId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tryConnectDisplayLocked(long physicalDisplayId) {
        IBinder displayToken = this.mSurfaceControlProxy.getPhysicalDisplayToken(physicalDisplayId);
        if (displayToken != null) {
            SurfaceControl.StaticDisplayInfo staticInfo = this.mSurfaceControlProxy.getStaticDisplayInfo(displayToken);
            if (staticInfo == null) {
                Slog.w(TAG, "No valid static info found for display device " + physicalDisplayId);
                return;
            }
            SurfaceControl.DynamicDisplayInfo dynamicInfo = this.mSurfaceControlProxy.getDynamicDisplayInfo(displayToken);
            if (dynamicInfo == null) {
                Slog.w(TAG, "No valid dynamic info found for display device " + physicalDisplayId);
            } else if (dynamicInfo.supportedDisplayModes == null) {
                Slog.w(TAG, "No valid modes found for display device " + physicalDisplayId);
            } else if (dynamicInfo.activeDisplayModeId < 0) {
                Slog.w(TAG, "No valid active mode found for display device " + physicalDisplayId);
            } else {
                if (dynamicInfo.activeColorMode < 0) {
                    Slog.w(TAG, "No valid active color mode for display device " + physicalDisplayId);
                    dynamicInfo.activeColorMode = -1;
                }
                SurfaceControl.DesiredDisplayModeSpecs modeSpecs = this.mSurfaceControlProxy.getDesiredDisplayModeSpecs(displayToken);
                LocalDisplayDevice device = this.mDevices.get(physicalDisplayId);
                if (device == null) {
                    boolean isFirstDisplay = this.mDevices.size() == 0;
                    LocalDisplayDevice device2 = new LocalDisplayDevice(displayToken, physicalDisplayId, staticInfo, dynamicInfo, modeSpecs, isFirstDisplay);
                    this.mDevices.put(physicalDisplayId, device2);
                    sendDisplayDeviceEventLocked(device2, 1);
                } else if (device.updateDisplayPropertiesLocked(staticInfo, dynamicInfo, modeSpecs)) {
                    sendDisplayDeviceEventLocked(device, 2);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tryDisconnectDisplayLocked(long physicalDisplayId) {
        LocalDisplayDevice device = this.mDevices.get(physicalDisplayId);
        if (device != null) {
            this.mDevices.remove(physicalDisplayId);
            sendDisplayDeviceEventLocked(device, 3);
        }
    }

    static int getPowerModeForState(int state) {
        switch (state) {
            case 1:
                return 0;
            case 2:
            case 5:
            default:
                return 2;
            case 3:
                return 1;
            case 4:
                return 3;
            case 6:
                return 4;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class LocalDisplayDevice extends DisplayDevice {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private int mActiveColorMode;
        private int mActiveDisplayModeAtStartId;
        private int mActiveModeId;
        private SurfaceControl.DisplayMode mActiveSfDisplayMode;
        private boolean mAllmRequested;
        private boolean mAllmSupported;
        private final BacklightAdapter mBacklightAdapter;
        private float mBrightnessState;
        private int mDefaultModeGroup;
        private int mDefaultModeId;
        private final DisplayModeDirector.DesiredDisplayModeSpecs mDisplayModeSpecs;
        private boolean mDisplayModeSpecsInvalid;
        private DisplayEventReceiver.FrameRateOverride[] mFrameRateOverrides;
        private boolean mGameContentTypeRequested;
        private boolean mGameContentTypeSupported;
        private boolean mHavePendingChanges;
        private Display.HdrCapabilities mHdrCapabilities;
        private DisplayDeviceInfo mInfo;
        private final boolean mIsFirstDisplay;
        private final long mPhysicalDisplayId;
        private float mSdrBrightnessState;
        private SurfaceControl.DisplayMode[] mSfDisplayModes;
        private boolean mSidekickActive;
        private final SidekickInternal mSidekickInternal;
        private int mState;
        private SurfaceControl.StaticDisplayInfo mStaticDisplayInfo;
        private final ArrayList<Integer> mSupportedColorModes;
        private final SparseArray<DisplayModeRecord> mSupportedModes;
        private int mSystemPreferredModeId;
        private Display.Mode mUserPreferredMode;
        private int mUserPreferredModeId;

        LocalDisplayDevice(IBinder displayToken, long physicalDisplayId, SurfaceControl.StaticDisplayInfo staticDisplayInfo, SurfaceControl.DynamicDisplayInfo dynamicInfo, SurfaceControl.DesiredDisplayModeSpecs modeSpecs, boolean isFirstDisplay) {
            super(LocalDisplayAdapter.this, displayToken, LocalDisplayAdapter.UNIQUE_ID_PREFIX + physicalDisplayId, LocalDisplayAdapter.this.getContext());
            this.mSupportedModes = new SparseArray<>();
            this.mSupportedColorModes = new ArrayList<>();
            this.mDisplayModeSpecs = new DisplayModeDirector.DesiredDisplayModeSpecs();
            this.mState = 0;
            this.mBrightnessState = Float.NaN;
            this.mSdrBrightnessState = Float.NaN;
            this.mDefaultModeId = -1;
            this.mSystemPreferredModeId = -1;
            this.mUserPreferredModeId = -1;
            this.mActiveDisplayModeAtStartId = -1;
            this.mActiveModeId = -1;
            this.mFrameRateOverrides = new DisplayEventReceiver.FrameRateOverride[0];
            this.mPhysicalDisplayId = physicalDisplayId;
            this.mIsFirstDisplay = isFirstDisplay;
            updateDisplayPropertiesLocked(staticDisplayInfo, dynamicInfo, modeSpecs);
            this.mSidekickInternal = (SidekickInternal) LocalServices.getService(SidekickInternal.class);
            this.mBacklightAdapter = new BacklightAdapter(displayToken, isFirstDisplay, LocalDisplayAdapter.this.mSurfaceControlProxy);
            this.mActiveDisplayModeAtStartId = dynamicInfo.activeDisplayModeId;
        }

        @Override // com.android.server.display.DisplayDevice
        public boolean hasStableUniqueId() {
            return true;
        }

        @Override // com.android.server.display.DisplayDevice
        public Display.Mode getActiveDisplayModeAtStartLocked() {
            return findMode(this.mActiveDisplayModeAtStartId);
        }

        public boolean updateDisplayPropertiesLocked(SurfaceControl.StaticDisplayInfo staticInfo, SurfaceControl.DynamicDisplayInfo dynamicInfo, SurfaceControl.DesiredDisplayModeSpecs modeSpecs) {
            boolean changed = updateDisplayModesLocked(dynamicInfo.supportedDisplayModes, dynamicInfo.preferredBootDisplayMode, dynamicInfo.activeDisplayModeId, modeSpecs) | updateStaticInfo(staticInfo) | updateColorModesLocked(dynamicInfo.supportedColorModes, dynamicInfo.activeColorMode) | updateHdrCapabilitiesLocked(dynamicInfo.hdrCapabilities) | updateAllmSupport(dynamicInfo.autoLowLatencyModeSupported) | updateGameContentTypeSupport(dynamicInfo.gameContentTypeSupported);
            if (changed) {
                this.mHavePendingChanges = true;
            }
            return changed;
        }

        public boolean updateDisplayModesLocked(SurfaceControl.DisplayMode[] displayModes, int preferredSfDisplayModeId, int activeSfDisplayModeId, SurfaceControl.DesiredDisplayModeSpecs modeSpecs) {
            int i;
            int activeBaseMode;
            this.mSfDisplayModes = (SurfaceControl.DisplayMode[]) Arrays.copyOf(displayModes, displayModes.length);
            this.mActiveSfDisplayMode = getModeById(displayModes, activeSfDisplayModeId);
            SurfaceControl.DisplayMode preferredSfDisplayMode = getModeById(displayModes, preferredSfDisplayModeId);
            ArrayList<DisplayModeRecord> records = new ArrayList<>();
            boolean modesAdded = false;
            int i2 = 0;
            while (i2 < displayModes.length) {
                SurfaceControl.DisplayMode mode = displayModes[i2];
                List<Float> alternativeRefreshRates = new ArrayList<>();
                int j = 0;
                while (j < displayModes.length) {
                    SurfaceControl.DisplayMode other = displayModes[j];
                    boolean isAlternative = j != i2 && other.width == mode.width && other.height == mode.height && other.refreshRate != mode.refreshRate && other.group == mode.group;
                    if (isAlternative) {
                        alternativeRefreshRates.add(Float.valueOf(displayModes[j].refreshRate));
                    }
                    j++;
                }
                Collections.sort(alternativeRefreshRates);
                boolean existingMode = false;
                Iterator<DisplayModeRecord> it = records.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    DisplayModeRecord record = it.next();
                    if (record.hasMatchingMode(mode) && refreshRatesEquals(alternativeRefreshRates, record.mMode.getAlternativeRefreshRates())) {
                        existingMode = true;
                        break;
                    }
                }
                if (!existingMode) {
                    DisplayModeRecord record2 = findDisplayModeRecord(mode, alternativeRefreshRates);
                    if (record2 == null) {
                        float[] alternativeRates = new float[alternativeRefreshRates.size()];
                        for (int j2 = 0; j2 < alternativeRates.length; j2++) {
                            alternativeRates[j2] = alternativeRefreshRates.get(j2).floatValue();
                        }
                        record2 = new DisplayModeRecord(mode, alternativeRates);
                        modesAdded = true;
                    }
                    records.add(record2);
                }
                i2++;
            }
            DisplayModeRecord activeRecord = null;
            Iterator<DisplayModeRecord> it2 = records.iterator();
            while (true) {
                if (!it2.hasNext()) {
                    break;
                }
                DisplayModeRecord record3 = it2.next();
                if (record3.hasMatchingMode(this.mActiveSfDisplayMode)) {
                    activeRecord = record3;
                    break;
                }
            }
            boolean preferredModeChanged = false;
            if (preferredSfDisplayModeId != -1 && preferredSfDisplayMode != null) {
                DisplayModeRecord preferredRecord = null;
                Iterator<DisplayModeRecord> it3 = records.iterator();
                while (true) {
                    if (!it3.hasNext()) {
                        break;
                    }
                    DisplayModeRecord record4 = it3.next();
                    if (record4.hasMatchingMode(preferredSfDisplayMode)) {
                        preferredRecord = record4;
                        break;
                    }
                }
                if (preferredRecord != null) {
                    int preferredModeId = preferredRecord.mMode.getModeId();
                    if (LocalDisplayAdapter.this.mIsBootDisplayModeSupported && this.mSystemPreferredModeId != preferredModeId) {
                        this.mSystemPreferredModeId = preferredModeId;
                        preferredModeChanged = true;
                    }
                }
            }
            boolean activeModeChanged = false;
            int i3 = this.mActiveModeId;
            if (i3 != -1 && i3 != activeRecord.mMode.getModeId()) {
                Slog.d(LocalDisplayAdapter.TAG, "The active mode was changed from SurfaceFlinger or the display device to " + activeRecord.mMode);
                this.mActiveModeId = activeRecord.mMode.getModeId();
                activeModeChanged = true;
                LocalDisplayAdapter.this.sendTraversalRequestLocked();
            }
            if (this.mDisplayModeSpecs.baseModeId != -1 && ((activeBaseMode = findMatchingModeIdLocked(modeSpecs.defaultMode)) == -1 || this.mDisplayModeSpecs.baseModeId != activeBaseMode || this.mDisplayModeSpecs.primaryRefreshRateRange.min != modeSpecs.primaryRefreshRateMin || this.mDisplayModeSpecs.primaryRefreshRateRange.max != modeSpecs.primaryRefreshRateMax || this.mDisplayModeSpecs.appRequestRefreshRateRange.min != modeSpecs.appRequestRefreshRateMin || this.mDisplayModeSpecs.appRequestRefreshRateRange.max != modeSpecs.appRequestRefreshRateMax)) {
                this.mDisplayModeSpecsInvalid = true;
                LocalDisplayAdapter.this.sendTraversalRequestLocked();
            }
            boolean recordsChanged = records.size() != this.mSupportedModes.size() || modesAdded;
            if (!recordsChanged) {
                return activeModeChanged || preferredModeChanged;
            }
            this.mSupportedModes.clear();
            Iterator<DisplayModeRecord> it4 = records.iterator();
            while (it4.hasNext()) {
                DisplayModeRecord record5 = it4.next();
                this.mSupportedModes.put(record5.mMode.getModeId(), record5);
            }
            int i4 = this.mDefaultModeId;
            if (i4 == -1) {
                int i5 = this.mSystemPreferredModeId;
                if (i5 == -1) {
                    i5 = activeRecord.mMode.getModeId();
                }
                this.mDefaultModeId = i5;
                if (this.mSystemPreferredModeId != -1) {
                    i = preferredSfDisplayMode.group;
                } else {
                    i = this.mActiveSfDisplayMode.group;
                }
                this.mDefaultModeGroup = i;
            } else if (!modesAdded || !activeModeChanged) {
                if (findSfDisplayModeIdLocked(i4, this.mDefaultModeGroup) < 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Default display mode no longer available, using currently active mode as default.");
                    this.mDefaultModeId = activeRecord.mMode.getModeId();
                    this.mDefaultModeGroup = this.mActiveSfDisplayMode.group;
                }
            } else {
                Slog.d(LocalDisplayAdapter.TAG, "New display modes are added and the active mode has changed, use active mode as default mode.");
                this.mDefaultModeId = activeRecord.mMode.getModeId();
                this.mDefaultModeGroup = this.mActiveSfDisplayMode.group;
            }
            if (this.mSupportedModes.indexOfKey(this.mDisplayModeSpecs.baseModeId) < 0) {
                if (this.mDisplayModeSpecs.baseModeId != -1) {
                    Slog.w(LocalDisplayAdapter.TAG, "DisplayModeSpecs base mode no longer available, using currently active mode.");
                }
                this.mDisplayModeSpecs.baseModeId = activeRecord.mMode.getModeId();
                this.mDisplayModeSpecsInvalid = true;
            }
            Display.Mode mode2 = this.mUserPreferredMode;
            if (mode2 != null) {
                this.mUserPreferredModeId = findUserPreferredModeIdLocked(mode2);
            }
            if (this.mSupportedModes.indexOfKey(this.mActiveModeId) < 0) {
                if (this.mActiveModeId != -1) {
                    Slog.w(LocalDisplayAdapter.TAG, "Active display mode no longer available, reverting to default mode.");
                }
                this.mActiveModeId = getPreferredModeId();
            }
            LocalDisplayAdapter.this.sendTraversalRequestLocked();
            return true;
        }

        @Override // com.android.server.display.DisplayDevice
        public DisplayDeviceConfig getDisplayDeviceConfig() {
            if (this.mDisplayDeviceConfig == null) {
                loadDisplayDeviceConfig();
            }
            return this.mDisplayDeviceConfig;
        }

        private int getPreferredModeId() {
            int i = this.mUserPreferredModeId;
            if (i != -1) {
                return i;
            }
            return this.mDefaultModeId;
        }

        private int getLogicalDensity() {
            DensityMapping densityMapping = getDisplayDeviceConfig().getDensityMapping();
            if (densityMapping == null) {
                return (int) ((this.mStaticDisplayInfo.density * 160.0f) + 0.5d);
            }
            return densityMapping.getDensityForResolution(this.mInfo.width, this.mInfo.height);
        }

        private void loadDisplayDeviceConfig() {
            Context context = LocalDisplayAdapter.this.getOverlayContext();
            this.mDisplayDeviceConfig = DisplayDeviceConfig.create(context, this.mPhysicalDisplayId, this.mIsFirstDisplay);
            this.mBacklightAdapter.setForceSurfaceControl(this.mDisplayDeviceConfig.hasQuirk(DisplayDeviceConfig.QUIRK_CAN_SET_BRIGHTNESS_VIA_HWC));
        }

        private boolean updateStaticInfo(SurfaceControl.StaticDisplayInfo info) {
            if (Objects.equals(this.mStaticDisplayInfo, info)) {
                return false;
            }
            this.mStaticDisplayInfo = info;
            return true;
        }

        private boolean updateColorModesLocked(int[] colorModes, int activeColorMode) {
            if (colorModes == null) {
                return false;
            }
            List<Integer> pendingColorModes = new ArrayList<>();
            boolean colorModesAdded = false;
            for (int colorMode : colorModes) {
                if (!this.mSupportedColorModes.contains(Integer.valueOf(colorMode))) {
                    colorModesAdded = true;
                }
                pendingColorModes.add(Integer.valueOf(colorMode));
            }
            boolean colorModesChanged = pendingColorModes.size() != this.mSupportedColorModes.size() || colorModesAdded;
            if (!colorModesChanged) {
                return false;
            }
            this.mSupportedColorModes.clear();
            this.mSupportedColorModes.addAll(pendingColorModes);
            Collections.sort(this.mSupportedColorModes);
            if (!this.mSupportedColorModes.contains(Integer.valueOf(this.mActiveColorMode))) {
                if (this.mActiveColorMode != 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Active color mode no longer available, reverting to default mode.");
                    this.mActiveColorMode = 0;
                } else if (!this.mSupportedColorModes.isEmpty()) {
                    Slog.e(LocalDisplayAdapter.TAG, "Default and active color mode is no longer available! Reverting to first available mode.");
                    this.mActiveColorMode = this.mSupportedColorModes.get(0).intValue();
                } else {
                    Slog.e(LocalDisplayAdapter.TAG, "No color modes available!");
                }
            }
            return true;
        }

        private boolean updateHdrCapabilitiesLocked(Display.HdrCapabilities newHdrCapabilities) {
            if (Objects.equals(this.mHdrCapabilities, newHdrCapabilities)) {
                return false;
            }
            this.mHdrCapabilities = newHdrCapabilities;
            return true;
        }

        private boolean updateAllmSupport(boolean supported) {
            if (this.mAllmSupported == supported) {
                return false;
            }
            this.mAllmSupported = supported;
            return true;
        }

        private boolean updateGameContentTypeSupport(boolean supported) {
            if (this.mGameContentTypeSupported == supported) {
                return false;
            }
            this.mGameContentTypeSupported = supported;
            return true;
        }

        private SurfaceControl.DisplayMode getModeById(SurfaceControl.DisplayMode[] supportedModes, int modeId) {
            for (SurfaceControl.DisplayMode mode : supportedModes) {
                if (mode.id == modeId) {
                    return mode;
                }
            }
            Slog.e(LocalDisplayAdapter.TAG, "Can't find display mode with id " + modeId);
            return null;
        }

        private DisplayModeRecord findDisplayModeRecord(SurfaceControl.DisplayMode mode, List<Float> alternativeRefreshRates) {
            for (int i = 0; i < this.mSupportedModes.size(); i++) {
                DisplayModeRecord record = this.mSupportedModes.valueAt(i);
                if (record.hasMatchingMode(mode) && refreshRatesEquals(alternativeRefreshRates, record.mMode.getAlternativeRefreshRates())) {
                    return record;
                }
            }
            return null;
        }

        private boolean refreshRatesEquals(List<Float> list, float[] array) {
            if (list.size() != array.length) {
                return false;
            }
            for (int i = 0; i < list.size(); i++) {
                if (Float.floatToIntBits(list.get(i).floatValue()) != Float.floatToIntBits(array[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override // com.android.server.display.DisplayDevice
        public void applyPendingDisplayDeviceInfoChangesLocked() {
            if (this.mHavePendingChanges) {
                this.mInfo = null;
                this.mHavePendingChanges = false;
            }
        }

        @Override // com.android.server.display.DisplayDevice
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                DisplayDeviceInfo displayDeviceInfo = new DisplayDeviceInfo();
                this.mInfo = displayDeviceInfo;
                displayDeviceInfo.width = this.mActiveSfDisplayMode.width;
                this.mInfo.height = this.mActiveSfDisplayMode.height;
                this.mInfo.modeId = this.mActiveModeId;
                this.mInfo.defaultModeId = getPreferredModeId();
                this.mInfo.supportedModes = getDisplayModes(this.mSupportedModes);
                this.mInfo.colorMode = this.mActiveColorMode;
                this.mInfo.allmSupported = this.mAllmSupported;
                this.mInfo.gameContentTypeSupported = this.mGameContentTypeSupported;
                this.mInfo.supportedColorModes = new int[this.mSupportedColorModes.size()];
                for (int i = 0; i < this.mSupportedColorModes.size(); i++) {
                    this.mInfo.supportedColorModes[i] = this.mSupportedColorModes.get(i).intValue();
                }
                this.mInfo.hdrCapabilities = this.mHdrCapabilities;
                this.mInfo.appVsyncOffsetNanos = this.mActiveSfDisplayMode.appVsyncOffsetNanos;
                this.mInfo.presentationDeadlineNanos = this.mActiveSfDisplayMode.presentationDeadlineNanos;
                this.mInfo.state = this.mState;
                this.mInfo.uniqueId = getUniqueId();
                DisplayAddress.Physical physicalAddress = DisplayAddress.fromPhysicalDisplayId(this.mPhysicalDisplayId);
                this.mInfo.address = physicalAddress;
                this.mInfo.densityDpi = getLogicalDensity();
                this.mInfo.xDpi = this.mActiveSfDisplayMode.xDpi;
                this.mInfo.yDpi = this.mActiveSfDisplayMode.yDpi;
                this.mInfo.deviceProductInfo = this.mStaticDisplayInfo.deviceProductInfo;
                if (this.mStaticDisplayInfo.secure) {
                    this.mInfo.flags = 12;
                }
                Resources res = LocalDisplayAdapter.this.getOverlayContext().getResources();
                this.mInfo.flags |= 1;
                if (this.mIsFirstDisplay) {
                    if (res.getBoolean(17891702) || (Build.IS_EMULATOR && SystemProperties.getBoolean(LocalDisplayAdapter.PROPERTY_EMULATOR_CIRCULAR, false))) {
                        this.mInfo.flags |= 256;
                    }
                } else {
                    if (!res.getBoolean(17891695)) {
                        this.mInfo.flags |= 128;
                    }
                    if (isDisplayPrivate(physicalAddress)) {
                        this.mInfo.flags |= 16;
                    }
                }
                if (DisplayCutout.getMaskBuiltInDisplayCutout(res, this.mInfo.uniqueId)) {
                    this.mInfo.flags |= 2048;
                }
                Display.Mode maxDisplayMode = DisplayUtils.getMaximumResolutionDisplayMode(this.mInfo.supportedModes);
                int maxWidth = maxDisplayMode == null ? this.mInfo.width : maxDisplayMode.getPhysicalWidth();
                int maxHeight = maxDisplayMode == null ? this.mInfo.height : maxDisplayMode.getPhysicalHeight();
                DisplayDeviceInfo displayDeviceInfo2 = this.mInfo;
                displayDeviceInfo2.displayCutout = DisplayCutout.fromResourcesRectApproximation(res, displayDeviceInfo2.uniqueId, maxWidth, maxHeight, this.mInfo.width, this.mInfo.height);
                DisplayDeviceInfo displayDeviceInfo3 = this.mInfo;
                displayDeviceInfo3.roundedCorners = RoundedCorners.fromResources(res, displayDeviceInfo3.uniqueId, maxWidth, maxHeight, this.mInfo.width, this.mInfo.height);
                this.mInfo.installOrientation = this.mStaticDisplayInfo.installOrientation;
                if (this.mStaticDisplayInfo.isInternal) {
                    this.mInfo.type = 1;
                    this.mInfo.touch = 1;
                    DisplayDeviceInfo displayDeviceInfo4 = this.mInfo;
                    displayDeviceInfo4.flags = 2 | displayDeviceInfo4.flags;
                    this.mInfo.name = res.getString(17040162);
                } else {
                    this.mInfo.type = 2;
                    this.mInfo.touch = 2;
                    this.mInfo.flags |= 64;
                    this.mInfo.name = LocalDisplayAdapter.this.getContext().getResources().getString(17040163);
                }
                this.mInfo.frameRateOverrides = this.mFrameRateOverrides;
                this.mInfo.flags |= 8192;
                this.mInfo.brightnessMinimum = 0.0f;
                this.mInfo.brightnessMaximum = 1.0f;
                this.mInfo.brightnessDefault = getDisplayDeviceConfig().getBrightnessDefault();
            }
            return this.mInfo;
        }

        @Override // com.android.server.display.DisplayDevice
        public Runnable requestDisplayStateLocked(final int state, final float brightnessState, final float sdrBrightnessState) {
            boolean z = true;
            boolean stateChanged = this.mState != state;
            if (this.mBrightnessState == brightnessState && this.mSdrBrightnessState == sdrBrightnessState) {
                z = false;
            }
            final boolean brightnessChanged = z;
            if (stateChanged || brightnessChanged) {
                final long physicalDisplayId = this.mPhysicalDisplayId;
                final IBinder token = getDisplayTokenLocked();
                final int oldState = this.mState;
                if (stateChanged) {
                    this.mState = state;
                    updateDeviceInfoLocked();
                }
                return new Runnable() { // from class: com.android.server.display.LocalDisplayAdapter.LocalDisplayDevice.1
                    @Override // java.lang.Runnable
                    public void run() {
                        int i;
                        int currentState = oldState;
                        if (Display.isSuspendedState(oldState) || oldState == 0) {
                            if (!Display.isSuspendedState(state)) {
                                setDisplayState(state);
                                currentState = state;
                            } else {
                                int i2 = state;
                                if (i2 == 4 || (i = oldState) == 4) {
                                    setDisplayState(3);
                                    currentState = 3;
                                } else if (i2 == 6 || i == 6) {
                                    setDisplayState(2);
                                    currentState = 2;
                                } else if (i != 0) {
                                    return;
                                }
                            }
                        }
                        boolean vrModeChange = false;
                        int i3 = state;
                        if ((i3 == 5 || currentState == 5) && currentState != i3) {
                            setVrMode(i3 == 5);
                            vrModeChange = true;
                        }
                        if (brightnessChanged || vrModeChange) {
                            setDisplayBrightness(brightnessState, sdrBrightnessState);
                            LocalDisplayDevice.this.mBrightnessState = brightnessState;
                            LocalDisplayDevice.this.mSdrBrightnessState = sdrBrightnessState;
                        }
                        int i4 = state;
                        if (i4 != currentState) {
                            setDisplayState(i4);
                        }
                    }

                    private void setVrMode(boolean isVrEnabled) {
                        if (LocalDisplayAdapter.DEBUG) {
                            Slog.d(LocalDisplayAdapter.TAG, "setVrMode(id=" + physicalDisplayId + ", state=" + Display.stateToString(state) + ")");
                        }
                        LocalDisplayDevice.this.mBacklightAdapter.setVrMode(isVrEnabled);
                    }

                    private void setDisplayState(int state2) {
                        if (LocalDisplayAdapter.DEBUG || LocalDisplayAdapter.MTK_DEBUG) {
                            Slog.d(LocalDisplayAdapter.TAG, "setDisplayState(id=" + physicalDisplayId + ", state=" + Display.stateToString(state2) + ")");
                        }
                        if (LocalDisplayDevice.this.mSidekickActive) {
                            Trace.traceBegin(131072L, "SidekickInternal#endDisplayControl");
                            try {
                                LocalDisplayDevice.this.mSidekickInternal.endDisplayControl();
                                Trace.traceEnd(131072L);
                                LocalDisplayDevice.this.mSidekickActive = false;
                            } finally {
                            }
                        }
                        int mode = LocalDisplayAdapter.getPowerModeForState(state2);
                        TranFoldDisplayCustody.instance().setDisplayPowerModeStart(LocalDisplayDevice.this.mPhysicalDisplayId, LocalDisplayDevice.this.getUniqueId(), oldState, state2);
                        Trace.traceBegin(131072L, "setDisplayState(id=" + physicalDisplayId + ", state=" + Display.stateToString(state2) + ")");
                        try {
                            Slog.d(LocalDisplayAdapter.TAG, "setDisplayPowerMode start:" + mode);
                            LocalDisplayAdapter.this.mSurfaceControlProxy.setDisplayPowerMode(token, mode);
                            Slog.d(LocalDisplayAdapter.TAG, "setDisplayPowerMode end:" + mode);
                            Trace.traceCounter(131072L, "DisplayPowerMode", mode);
                            Trace.traceEnd(131072L);
                            TranFoldDisplayCustody.instance().setDisplayPowerModeEnd(LocalDisplayDevice.this.mPhysicalDisplayId);
                            IDisplayManagerServiceLice.Instance().onSetDisplayPowerMode(LocalDisplayDevice.this, mode);
                            if (Display.isSuspendedState(state2) && state2 != 1 && LocalDisplayDevice.this.mSidekickInternal != null && !LocalDisplayDevice.this.mSidekickActive) {
                                Trace.traceBegin(131072L, "SidekickInternal#startDisplayControl");
                                try {
                                    LocalDisplayDevice localDisplayDevice = LocalDisplayDevice.this;
                                    localDisplayDevice.mSidekickActive = localDisplayDevice.mSidekickInternal.startDisplayControl(state2);
                                } finally {
                                }
                            }
                        } catch (Throwable th) {
                            Trace.traceEnd(131072L);
                            TranFoldDisplayCustody.instance().setDisplayPowerModeEnd(LocalDisplayDevice.this.mPhysicalDisplayId);
                            IDisplayManagerServiceLice.Instance().onSetDisplayPowerMode(LocalDisplayDevice.this, mode);
                            throw th;
                        }
                    }

                    private void setDisplayBrightness(float brightnessState2, float sdrBrightnessState2) {
                        if (Float.isNaN(brightnessState2) || Float.isNaN(sdrBrightnessState2)) {
                            return;
                        }
                        if (LocalDisplayAdapter.DEBUG || LocalDisplayAdapter.MTK_DEBUG) {
                            Slog.d(LocalDisplayAdapter.TAG, "setDisplayBrightness(id=" + physicalDisplayId + ", brightnessState=" + brightnessState2 + ", sdrBrightnessState=" + sdrBrightnessState2 + ")");
                        }
                        Trace.traceBegin(131072L, "setDisplayBrightness(id=" + physicalDisplayId + ", brightnessState=" + brightnessState2 + ", sdrBrightnessState=" + sdrBrightnessState2 + ")");
                        try {
                            float backlight = brightnessToBacklight(brightnessState2);
                            float sdrBacklight = brightnessToBacklight(sdrBrightnessState2);
                            float nits = backlightToNits(backlight);
                            float sdrNits = backlightToNits(sdrBacklight);
                            LocalDisplayDevice.this.mBacklightAdapter.setBacklight(sdrBacklight, sdrNits, backlight, nits);
                            Trace.traceCounter(131072L, "ScreenBrightness", BrightnessSynchronizer.brightnessFloatToInt(brightnessState2));
                            Trace.traceCounter(131072L, "SdrScreenBrightness", BrightnessSynchronizer.brightnessFloatToInt(sdrBrightnessState2));
                        } finally {
                            Trace.traceEnd(131072L);
                        }
                    }

                    private float brightnessToBacklight(float brightness) {
                        if (brightness == -1.0f) {
                            return -1.0f;
                        }
                        return LocalDisplayDevice.this.getDisplayDeviceConfig().getBacklightFromBrightness(brightness);
                    }

                    private float backlightToNits(float backlight) {
                        return LocalDisplayDevice.this.getDisplayDeviceConfig().getNitsFromBacklight(backlight);
                    }
                };
            }
            return null;
        }

        @Override // com.android.server.display.DisplayDevice
        public void setUserPreferredDisplayModeLocked(Display.Mode mode) {
            Display.Mode matchingSupportedMode;
            int oldModeId = getPreferredModeId();
            this.mUserPreferredMode = mode;
            if (mode != null && ((mode.isRefreshRateSet() || mode.isResolutionSet()) && (matchingSupportedMode = findMode(mode.getPhysicalWidth(), mode.getPhysicalHeight(), mode.getRefreshRate())) != null)) {
                this.mUserPreferredMode = matchingSupportedMode;
            }
            this.mUserPreferredModeId = findUserPreferredModeIdLocked(this.mUserPreferredMode);
            if (oldModeId == getPreferredModeId()) {
                return;
            }
            updateDeviceInfoLocked();
            if (!LocalDisplayAdapter.this.mIsBootDisplayModeSupported) {
                return;
            }
            if (this.mUserPreferredModeId == -1) {
                LocalDisplayAdapter.this.mSurfaceControlProxy.clearBootDisplayMode(getDisplayTokenLocked());
                return;
            }
            int preferredSfDisplayModeId = findSfDisplayModeIdLocked(this.mUserPreferredMode.getModeId(), this.mDefaultModeGroup);
            LocalDisplayAdapter.this.mSurfaceControlProxy.setBootDisplayMode(getDisplayTokenLocked(), preferredSfDisplayModeId);
        }

        @Override // com.android.server.display.DisplayDevice
        public Display.Mode getUserPreferredDisplayModeLocked() {
            return this.mUserPreferredMode;
        }

        @Override // com.android.server.display.DisplayDevice
        public Display.Mode getSystemPreferredDisplayModeLocked() {
            return findMode(this.mSystemPreferredModeId);
        }

        @Override // com.android.server.display.DisplayDevice
        public void setRequestedColorModeLocked(int colorMode) {
            requestColorModeLocked(colorMode);
        }

        @Override // com.android.server.display.DisplayDevice
        public void setDesiredDisplayModeSpecsLocked(DisplayModeDirector.DesiredDisplayModeSpecs displayModeSpecs) {
            if (displayModeSpecs.baseModeId == 0) {
                return;
            }
            int baseSfModeId = findSfDisplayModeIdLocked(displayModeSpecs.baseModeId, this.mDefaultModeGroup);
            if (baseSfModeId < 0) {
                Slog.w(LocalDisplayAdapter.TAG, "Ignoring request for invalid base mode id " + displayModeSpecs.baseModeId);
                updateDeviceInfoLocked();
            } else if ((!this.mDisplayModeSpecsInvalid && displayModeSpecs.equals(this.mDisplayModeSpecs)) || IDisplayManagerServiceLice.Instance().onSetDesiredDisplayModeSpecsLocked(this, this.mDisplayModeSpecs, displayModeSpecs)) {
            } else {
                this.mDisplayModeSpecsInvalid = false;
                this.mDisplayModeSpecs.copyFrom(displayModeSpecs);
                LocalDisplayAdapter.this.getHandler().sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.display.LocalDisplayAdapter$LocalDisplayDevice$$ExternalSyntheticLambda0
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((LocalDisplayAdapter.LocalDisplayDevice) obj).setDesiredDisplayModeSpecsAsync((IBinder) obj2, (SurfaceControl.DesiredDisplayModeSpecs) obj3);
                    }
                }, this, getDisplayTokenLocked(), new SurfaceControl.DesiredDisplayModeSpecs(baseSfModeId, this.mDisplayModeSpecs.allowGroupSwitching, this.mDisplayModeSpecs.primaryRefreshRateRange.min, this.mDisplayModeSpecs.primaryRefreshRateRange.max, this.mDisplayModeSpecs.appRequestRefreshRateRange.min, this.mDisplayModeSpecs.appRequestRefreshRateRange.max)));
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setDesiredDisplayModeSpecsAsync(IBinder displayToken, SurfaceControl.DesiredDisplayModeSpecs modeSpecs) {
            LocalDisplayAdapter.this.mSurfaceControlProxy.setDesiredDisplayModeSpecs(displayToken, modeSpecs);
        }

        @Override // com.android.server.display.DisplayDevice
        public void onOverlayChangedLocked() {
            updateDeviceInfoLocked();
        }

        public void onActiveDisplayModeChangedLocked(int sfModeId) {
            if (updateActiveModeLocked(sfModeId)) {
                updateDeviceInfoLocked();
            }
        }

        public void onFrameRateOverridesChanged(DisplayEventReceiver.FrameRateOverride[] overrides) {
            if (updateFrameRateOverridesLocked(overrides)) {
                updateDeviceInfoLocked();
            }
        }

        public boolean updateActiveModeLocked(int activeSfModeId) {
            if (this.mActiveSfDisplayMode.id == activeSfModeId) {
                return false;
            }
            this.mActiveSfDisplayMode = getModeById(this.mSfDisplayModes, activeSfModeId);
            int findMatchingModeIdLocked = findMatchingModeIdLocked(activeSfModeId);
            this.mActiveModeId = findMatchingModeIdLocked;
            if (findMatchingModeIdLocked == -1) {
                Slog.w(LocalDisplayAdapter.TAG, "In unknown mode after setting allowed modes, activeModeId=" + activeSfModeId);
                return true;
            }
            return true;
        }

        public boolean updateFrameRateOverridesLocked(DisplayEventReceiver.FrameRateOverride[] overrides) {
            if (Arrays.equals(overrides, this.mFrameRateOverrides)) {
                return false;
            }
            this.mFrameRateOverrides = overrides;
            return true;
        }

        public void requestColorModeLocked(int colorMode) {
            if (this.mActiveColorMode == colorMode) {
                return;
            }
            if (!this.mSupportedColorModes.contains(Integer.valueOf(colorMode))) {
                Slog.w(LocalDisplayAdapter.TAG, "Unable to find color mode " + colorMode + ", ignoring request.");
                return;
            }
            this.mActiveColorMode = colorMode;
            LocalDisplayAdapter.this.getHandler().sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.display.LocalDisplayAdapter$LocalDisplayDevice$$ExternalSyntheticLambda1
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((LocalDisplayAdapter.LocalDisplayDevice) obj).requestColorModeAsync((IBinder) obj2, ((Integer) obj3).intValue());
                }
            }, this, getDisplayTokenLocked(), Integer.valueOf(colorMode)));
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void requestColorModeAsync(IBinder displayToken, int colorMode) {
            LocalDisplayAdapter.this.mSurfaceControlProxy.setActiveColorMode(displayToken, colorMode);
            synchronized (LocalDisplayAdapter.this.getSyncRoot()) {
                updateDeviceInfoLocked();
            }
        }

        @Override // com.android.server.display.DisplayDevice
        public void setAutoLowLatencyModeLocked(boolean on) {
            if (this.mAllmRequested == on) {
                return;
            }
            this.mAllmRequested = on;
            if (!this.mAllmSupported) {
                Slog.d(LocalDisplayAdapter.TAG, "Unable to set ALLM because the connected display does not support ALLM.");
            } else {
                LocalDisplayAdapter.this.mSurfaceControlProxy.setAutoLowLatencyMode(getDisplayTokenLocked(), on);
            }
        }

        @Override // com.android.server.display.DisplayDevice
        public void setGameContentTypeLocked(boolean on) {
            if (this.mGameContentTypeRequested == on) {
                return;
            }
            this.mGameContentTypeRequested = on;
            LocalDisplayAdapter.this.mSurfaceControlProxy.setGameContentType(getDisplayTokenLocked(), on);
        }

        @Override // com.android.server.display.DisplayDevice
        public void dumpLocked(PrintWriter pw) {
            SurfaceControl.DisplayMode[] displayModeArr;
            super.dumpLocked(pw);
            pw.println("mPhysicalDisplayId=" + this.mPhysicalDisplayId);
            pw.println("mDisplayModeSpecs={" + this.mDisplayModeSpecs + "}");
            pw.println("mDisplayModeSpecsInvalid=" + this.mDisplayModeSpecsInvalid);
            pw.println("mActiveModeId=" + this.mActiveModeId);
            pw.println("mActiveColorMode=" + this.mActiveColorMode);
            pw.println("mDefaultModeId=" + this.mDefaultModeId);
            pw.println("mUserPreferredModeId=" + this.mUserPreferredModeId);
            pw.println("mState=" + Display.stateToString(this.mState));
            pw.println("mBrightnessState=" + this.mBrightnessState);
            pw.println("mBacklightAdapter=" + this.mBacklightAdapter);
            pw.println("mAllmSupported=" + this.mAllmSupported);
            pw.println("mAllmRequested=" + this.mAllmRequested);
            pw.println("mGameContentTypeSupported=" + this.mGameContentTypeSupported);
            pw.println("mGameContentTypeRequested=" + this.mGameContentTypeRequested);
            pw.println("mStaticDisplayInfo=" + this.mStaticDisplayInfo);
            pw.println("mSfDisplayModes=");
            for (SurfaceControl.DisplayMode sfDisplayMode : this.mSfDisplayModes) {
                pw.println("  " + sfDisplayMode);
            }
            pw.println("mActiveSfDisplayMode=" + this.mActiveSfDisplayMode);
            pw.println("mSupportedModes=");
            for (int i = 0; i < this.mSupportedModes.size(); i++) {
                pw.println("  " + this.mSupportedModes.valueAt(i));
            }
            pw.println("mSupportedColorModes=" + this.mSupportedColorModes);
            pw.println("mDisplayDeviceConfig=" + this.mDisplayDeviceConfig);
        }

        private int findSfDisplayModeIdLocked(int displayModeId, int modeGroup) {
            SurfaceControl.DisplayMode[] displayModeArr;
            int matchingSfDisplayModeId = -1;
            DisplayModeRecord record = this.mSupportedModes.get(displayModeId);
            if (record != null) {
                for (SurfaceControl.DisplayMode mode : this.mSfDisplayModes) {
                    if (record.hasMatchingMode(mode)) {
                        if (matchingSfDisplayModeId == -1) {
                            matchingSfDisplayModeId = mode.id;
                        }
                        if (mode.group == modeGroup) {
                            return mode.id;
                        }
                    }
                }
            }
            return matchingSfDisplayModeId;
        }

        private Display.Mode findMode(int modeId) {
            for (int i = 0; i < this.mSupportedModes.size(); i++) {
                Display.Mode supportedMode = this.mSupportedModes.valueAt(i).mMode;
                if (supportedMode.getModeId() == modeId) {
                    return supportedMode;
                }
            }
            return null;
        }

        private Display.Mode findMode(int width, int height, float refreshRate) {
            for (int i = 0; i < this.mSupportedModes.size(); i++) {
                Display.Mode supportedMode = this.mSupportedModes.valueAt(i).mMode;
                if (supportedMode.matchesIfValid(width, height, refreshRate)) {
                    return supportedMode;
                }
            }
            return null;
        }

        private int findUserPreferredModeIdLocked(Display.Mode userPreferredMode) {
            if (userPreferredMode != null) {
                for (int i = 0; i < this.mSupportedModes.size(); i++) {
                    Display.Mode supportedMode = this.mSupportedModes.valueAt(i).mMode;
                    if (userPreferredMode.matches(supportedMode.getPhysicalWidth(), supportedMode.getPhysicalHeight(), supportedMode.getRefreshRate())) {
                        return supportedMode.getModeId();
                    }
                }
                return -1;
            }
            return -1;
        }

        private int findMatchingModeIdLocked(int sfModeId) {
            SurfaceControl.DisplayMode mode = getModeById(this.mSfDisplayModes, sfModeId);
            if (mode == null) {
                Slog.e(LocalDisplayAdapter.TAG, "Invalid display mode ID " + sfModeId);
                return -1;
            }
            for (int i = 0; i < this.mSupportedModes.size(); i++) {
                DisplayModeRecord record = this.mSupportedModes.valueAt(i);
                if (record.hasMatchingMode(mode)) {
                    return record.mMode.getModeId();
                }
            }
            return -1;
        }

        private void updateDeviceInfoLocked() {
            this.mInfo = null;
            LocalDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
        }

        private Display.Mode[] getDisplayModes(SparseArray<DisplayModeRecord> records) {
            int size = records.size();
            Display.Mode[] modes = new Display.Mode[size];
            for (int i = 0; i < size; i++) {
                DisplayModeRecord record = records.valueAt(i);
                modes[i] = record.mMode;
            }
            return modes;
        }

        private boolean isDisplayPrivate(DisplayAddress.Physical physicalAddress) {
            if (physicalAddress == null) {
                return false;
            }
            Resources res = LocalDisplayAdapter.this.getOverlayContext().getResources();
            int[] ports = res.getIntArray(17236081);
            if (ports != null) {
                int port = physicalAddress.getPort();
                for (int p : ports) {
                    if (p == port) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    Context getOverlayContext() {
        if (this.mOverlayContext == null) {
            this.mOverlayContext = ActivityThread.currentActivityThread().getSystemUiContext();
        }
        return this.mOverlayContext;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class DisplayModeRecord {
        public final Display.Mode mMode;

        DisplayModeRecord(SurfaceControl.DisplayMode mode, float[] alternativeRefreshRates) {
            this.mMode = DisplayAdapter.createMode(mode.width, mode.height, mode.refreshRate, alternativeRefreshRates);
        }

        public boolean hasMatchingMode(SurfaceControl.DisplayMode mode) {
            return this.mMode.getPhysicalWidth() == mode.width && this.mMode.getPhysicalHeight() == mode.height && Float.floatToIntBits(this.mMode.getRefreshRate()) == Float.floatToIntBits(mode.refreshRate);
        }

        public String toString() {
            return "DisplayModeRecord{mMode=" + this.mMode + "}";
        }
    }

    /* loaded from: classes.dex */
    public static class Injector {
        private ProxyDisplayEventReceiver mReceiver;

        public void setDisplayEventListenerLocked(Looper looper, DisplayEventListener listener) {
            this.mReceiver = new ProxyDisplayEventReceiver(looper, listener);
        }

        public SurfaceControlProxy getSurfaceControlProxy() {
            return new SurfaceControlProxy();
        }
    }

    /* loaded from: classes.dex */
    public static final class ProxyDisplayEventReceiver extends DisplayEventReceiver {
        private final DisplayEventListener mListener;

        ProxyDisplayEventReceiver(Looper looper, DisplayEventListener listener) {
            super(looper, 0, 3);
            this.mListener = listener;
        }

        public void onHotplug(long timestampNanos, long physicalDisplayId, boolean connected) {
            this.mListener.onHotplug(timestampNanos, physicalDisplayId, connected);
        }

        public void onModeChanged(long timestampNanos, long physicalDisplayId, int modeId) {
            this.mListener.onModeChanged(timestampNanos, physicalDisplayId, modeId);
        }

        public void onFrameRateOverridesChanged(long timestampNanos, long physicalDisplayId, DisplayEventReceiver.FrameRateOverride[] overrides) {
            this.mListener.onFrameRateOverridesChanged(timestampNanos, physicalDisplayId, overrides);
        }
    }

    /* loaded from: classes.dex */
    private final class LocalDisplayEventListener implements DisplayEventListener {
        private LocalDisplayEventListener() {
        }

        @Override // com.android.server.display.LocalDisplayAdapter.DisplayEventListener
        public void onHotplug(long timestampNanos, long physicalDisplayId, boolean connected) {
            synchronized (LocalDisplayAdapter.this.getSyncRoot()) {
                if (connected) {
                    LocalDisplayAdapter.this.tryConnectDisplayLocked(physicalDisplayId);
                } else {
                    LocalDisplayAdapter.this.tryDisconnectDisplayLocked(physicalDisplayId);
                }
            }
        }

        @Override // com.android.server.display.LocalDisplayAdapter.DisplayEventListener
        public void onModeChanged(long timestampNanos, long physicalDisplayId, int modeId) {
            if (LocalDisplayAdapter.DEBUG) {
                Slog.d(LocalDisplayAdapter.TAG, "onModeChanged(timestampNanos=" + timestampNanos + ", physicalDisplayId=" + physicalDisplayId + ", modeId=" + modeId + ")");
            }
            synchronized (LocalDisplayAdapter.this.getSyncRoot()) {
                LocalDisplayDevice device = (LocalDisplayDevice) LocalDisplayAdapter.this.mDevices.get(physicalDisplayId);
                if (device == null) {
                    if (LocalDisplayAdapter.DEBUG) {
                        Slog.d(LocalDisplayAdapter.TAG, "Received mode change for unhandled physical display: physicalDisplayId=" + physicalDisplayId);
                    }
                    return;
                }
                device.onActiveDisplayModeChangedLocked(modeId);
            }
        }

        @Override // com.android.server.display.LocalDisplayAdapter.DisplayEventListener
        public void onFrameRateOverridesChanged(long timestampNanos, long physicalDisplayId, DisplayEventReceiver.FrameRateOverride[] overrides) {
            if (LocalDisplayAdapter.DEBUG) {
                Slog.d(LocalDisplayAdapter.TAG, "onFrameRateOverrideChanged(timestampNanos=" + timestampNanos + ", physicalDisplayId=" + physicalDisplayId + " overrides=" + Arrays.toString(overrides) + ")");
            }
            synchronized (LocalDisplayAdapter.this.getSyncRoot()) {
                LocalDisplayDevice device = (LocalDisplayDevice) LocalDisplayAdapter.this.mDevices.get(physicalDisplayId);
                if (device == null) {
                    if (LocalDisplayAdapter.DEBUG) {
                        Slog.d(LocalDisplayAdapter.TAG, "Received frame rate override event for unhandled physical display: physicalDisplayId=" + physicalDisplayId);
                    }
                    return;
                }
                device.onFrameRateOverridesChanged(overrides);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class SurfaceControlProxy {
        SurfaceControlProxy() {
        }

        public SurfaceControl.DynamicDisplayInfo getDynamicDisplayInfo(IBinder token) {
            return SurfaceControl.getDynamicDisplayInfo(token);
        }

        public long[] getPhysicalDisplayIds() {
            return SurfaceControl.getPhysicalDisplayIds();
        }

        public IBinder getPhysicalDisplayToken(long physicalDisplayId) {
            return SurfaceControl.getPhysicalDisplayToken(physicalDisplayId);
        }

        public SurfaceControl.StaticDisplayInfo getStaticDisplayInfo(IBinder displayToken) {
            return SurfaceControl.getStaticDisplayInfo(displayToken);
        }

        public SurfaceControl.DesiredDisplayModeSpecs getDesiredDisplayModeSpecs(IBinder displayToken) {
            return SurfaceControl.getDesiredDisplayModeSpecs(displayToken);
        }

        public boolean setDesiredDisplayModeSpecs(IBinder token, SurfaceControl.DesiredDisplayModeSpecs specs) {
            return SurfaceControl.setDesiredDisplayModeSpecs(token, specs);
        }

        public void setDisplayPowerMode(IBinder displayToken, int mode) {
            SurfaceControl.setDisplayPowerMode(displayToken, mode);
        }

        public boolean setActiveColorMode(IBinder displayToken, int colorMode) {
            return SurfaceControl.setActiveColorMode(displayToken, colorMode);
        }

        public boolean getBootDisplayModeSupport() {
            Trace.traceBegin(32L, "getBootDisplayModeSupport");
            try {
                return SurfaceControl.getBootDisplayModeSupport();
            } finally {
                Trace.traceEnd(32L);
            }
        }

        public void setBootDisplayMode(IBinder displayToken, int modeId) {
            SurfaceControl.setBootDisplayMode(displayToken, modeId);
        }

        public void clearBootDisplayMode(IBinder displayToken) {
            SurfaceControl.clearBootDisplayMode(displayToken);
        }

        public void setAutoLowLatencyMode(IBinder displayToken, boolean on) {
            SurfaceControl.setAutoLowLatencyMode(displayToken, on);
        }

        public void setGameContentType(IBinder displayToken, boolean on) {
            SurfaceControl.setGameContentType(displayToken, on);
        }

        public boolean getDisplayBrightnessSupport(IBinder displayToken) {
            return SurfaceControl.getDisplayBrightnessSupport(displayToken);
        }

        public boolean setDisplayBrightness(IBinder displayToken, float brightness) {
            return SurfaceControl.setDisplayBrightness(displayToken, brightness);
        }

        public boolean setDisplayBrightness(IBinder displayToken, float sdrBacklight, float sdrNits, float displayBacklight, float displayNits) {
            return SurfaceControl.setDisplayBrightness(displayToken, sdrBacklight, sdrNits, displayBacklight, displayNits);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class BacklightAdapter {
        private static boolean sForceSurfaceControlForDefaultDisplay = false;
        private static boolean sUseSurfaceControlBrightnessForDefaultDisplay;
        private final LogicalLight mBacklight;
        private final IBinder mDisplayToken;
        private boolean mForceSurfaceControl = false;
        private boolean mIsFirstDisplay;
        private final SurfaceControlProxy mSurfaceControlProxy;
        private final boolean mUseSurfaceControlBrightness;

        public static boolean useSurfaceControlBrightness() {
            return sUseSurfaceControlBrightnessForDefaultDisplay || sForceSurfaceControlForDefaultDisplay;
        }

        BacklightAdapter(IBinder displayToken, boolean isFirstDisplay, SurfaceControlProxy surfaceControlProxy) {
            this.mIsFirstDisplay = true;
            this.mDisplayToken = displayToken;
            this.mSurfaceControlProxy = surfaceControlProxy;
            this.mIsFirstDisplay = isFirstDisplay;
            boolean displayBrightnessSupport = surfaceControlProxy.getDisplayBrightnessSupport(displayToken);
            this.mUseSurfaceControlBrightness = displayBrightnessSupport;
            if (isFirstDisplay) {
                sUseSurfaceControlBrightnessForDefaultDisplay = displayBrightnessSupport;
            }
            if (!displayBrightnessSupport && isFirstDisplay) {
                LightsManager lights = (LightsManager) LocalServices.getService(LightsManager.class);
                this.mBacklight = lights.getLight(0);
                return;
            }
            LightsManager lights2 = (LightsManager) LocalServices.getService(LightsManager.class);
            this.mBacklight = lights2.getLight(0);
        }

        void setBacklight(float sdrBacklight, float sdrNits, float backlight, float nits) {
            if (this.mUseSurfaceControlBrightness || this.mForceSurfaceControl) {
                if (BrightnessSynchronizer.floatEquals(sdrBacklight, Float.NaN)) {
                    this.mSurfaceControlProxy.setDisplayBrightness(this.mDisplayToken, backlight);
                    return;
                } else {
                    this.mSurfaceControlProxy.setDisplayBrightness(this.mDisplayToken, sdrBacklight, sdrNits, backlight, nits);
                    return;
                }
            }
            LogicalLight logicalLight = this.mBacklight;
            if (logicalLight != null) {
                logicalLight.setBrightness(backlight);
            }
        }

        void setVrMode(boolean isVrModeEnabled) {
            LogicalLight logicalLight = this.mBacklight;
            if (logicalLight != null) {
                logicalLight.setVrMode(isVrModeEnabled);
            }
        }

        void setForceSurfaceControl(boolean forceSurfaceControl) {
            this.mForceSurfaceControl = forceSurfaceControl;
            if (this.mIsFirstDisplay) {
                sForceSurfaceControlForDefaultDisplay = forceSurfaceControl;
            }
        }

        public String toString() {
            return "BacklightAdapter [useSurfaceControl=" + this.mUseSurfaceControlBrightness + " (force_anyway? " + this.mForceSurfaceControl + "), backlight=" + this.mBacklight + "]";
        }
    }
}
