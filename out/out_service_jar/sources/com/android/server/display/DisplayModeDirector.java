package com.android.server.display;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.BrightnessInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.fingerprint.IUdfpsHbmListener;
import android.net.Uri;
import android.os.Handler;
import android.os.IThermalEventListener;
import android.os.IThermalService;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Temperature;
import android.provider.DeviceConfig;
import android.provider.DeviceConfigInterface;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.IndentingPrintWriter;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.DisplayInfo;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.internal.os.BackgroundThread;
import com.android.server.LocalServices;
import com.android.server.display.DisplayModeDirector;
import com.android.server.display.utils.AmbientFilter;
import com.android.server.display.utils.AmbientFilterFactory;
import com.android.server.sensors.SensorManagerInternal;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
/* loaded from: classes.dex */
public class DisplayModeDirector {
    private static final float FLOAT_TOLERANCE = 0.01f;
    private static final int GLOBAL_ID = -1;
    private static final int MSG_DEFAULT_PEAK_REFRESH_RATE_CHANGED = 3;
    private static final int MSG_HIGH_BRIGHTNESS_THRESHOLDS_CHANGED = 6;
    private static final int MSG_LOW_BRIGHTNESS_THRESHOLDS_CHANGED = 2;
    private static final int MSG_REFRESH_RATE_IN_HBM_HDR_CHANGED = 8;
    private static final int MSG_REFRESH_RATE_IN_HBM_SUNLIGHT_CHANGED = 7;
    private static final int MSG_REFRESH_RATE_IN_HIGH_ZONE_CHANGED = 5;
    private static final int MSG_REFRESH_RATE_IN_LOW_ZONE_CHANGED = 4;
    private static final int MSG_REFRESH_RATE_RANGE_CHANGED = 1;
    private static final String TAG = "DisplayModeDirector";
    private boolean mAlwaysRespectAppRequest;
    private final AppRequestObserver mAppRequestObserver;
    private BrightnessObserver mBrightnessObserver;
    private final Context mContext;
    private SparseArray<Display.Mode> mDefaultModeByDisplay;
    private DesiredDisplayModeSpecsListener mDesiredDisplayModeSpecsListener;
    private final DeviceConfigInterface mDeviceConfig;
    private final DeviceConfigDisplaySettings mDeviceConfigDisplaySettings;
    private final DisplayObserver mDisplayObserver;
    private final DisplayModeDirectorHandler mHandler;
    private final HbmObserver mHbmObserver;
    private final Injector mInjector;
    private boolean mIsResolutionSwitchByMode;
    private final Object mLock;
    private boolean mLoggingEnabled;
    private int mModeSwitchingType;
    private final SensorObserver mSensorObserver;
    private final SettingsObserver mSettingsObserver;
    private final SkinThermalStatusObserver mSkinThermalStatusObserver;
    private SparseArray<Display.Mode[]> mSupportedModesByDisplay;
    private final UdfpsObserver mUdfpsObserver;
    private SparseArray<SparseArray<Vote>> mVotesByDisplay;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface BallotBox {
        void vote(int i, int i2, Vote vote);
    }

    /* loaded from: classes.dex */
    public interface DesiredDisplayModeSpecsListener {
        void onDesiredDisplayModeSpecsChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Injector {
        public static final Uri PEAK_REFRESH_RATE_URI = Settings.System.getUriFor("peak_refresh_rate");

        BrightnessInfo getBrightnessInfo(int i);

        DeviceConfigInterface getDeviceConfig();

        IThermalService getThermalService();

        boolean isDozeState(Display display);

        void registerDisplayListener(DisplayManager.DisplayListener displayListener, Handler handler, long j);

        void registerPeakRefreshRateObserver(ContentResolver contentResolver, ContentObserver contentObserver);
    }

    public DisplayModeDirector(Context context, Handler handler) {
        this(context, handler, new RealInjector(context));
    }

    public DisplayModeDirector(Context context, Handler handler, Injector injector) {
        this.mLock = new Object();
        this.mModeSwitchingType = 1;
        this.mContext = context;
        this.mHandler = new DisplayModeDirectorHandler(handler.getLooper());
        this.mInjector = injector;
        this.mVotesByDisplay = new SparseArray<>();
        this.mSupportedModesByDisplay = new SparseArray<>();
        this.mDefaultModeByDisplay = new SparseArray<>();
        this.mAppRequestObserver = new AppRequestObserver();
        this.mSettingsObserver = new SettingsObserver(context, handler);
        this.mDisplayObserver = new DisplayObserver(context, handler);
        this.mBrightnessObserver = new BrightnessObserver(context, handler, injector);
        this.mUdfpsObserver = new UdfpsObserver();
        BallotBox ballotBox = new BallotBox() { // from class: com.android.server.display.DisplayModeDirector$$ExternalSyntheticLambda0
            @Override // com.android.server.display.DisplayModeDirector.BallotBox
            public final void vote(int i, int i2, DisplayModeDirector.Vote vote) {
                DisplayModeDirector.this.m3359lambda$new$0$comandroidserverdisplayDisplayModeDirector(i, i2, vote);
            }
        };
        this.mSensorObserver = new SensorObserver(context, ballotBox, injector);
        this.mSkinThermalStatusObserver = new SkinThermalStatusObserver(injector, ballotBox);
        DeviceConfigDisplaySettings deviceConfigDisplaySettings = new DeviceConfigDisplaySettings();
        this.mDeviceConfigDisplaySettings = deviceConfigDisplaySettings;
        this.mHbmObserver = new HbmObserver(injector, ballotBox, BackgroundThread.getHandler(), deviceConfigDisplaySettings);
        this.mDeviceConfig = injector.getDeviceConfig();
        this.mAlwaysRespectAppRequest = false;
        this.mIsResolutionSwitchByMode = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-display-DisplayModeDirector  reason: not valid java name */
    public /* synthetic */ void m3359lambda$new$0$comandroidserverdisplayDisplayModeDirector(int displayId, int priority, Vote vote) {
        synchronized (this.mLock) {
            updateVoteLocked(displayId, priority, vote);
        }
    }

    public void start(SensorManager sensorManager) {
        this.mSettingsObserver.observe();
        this.mDisplayObserver.observe();
        this.mBrightnessObserver.observe(sensorManager);
        this.mSensorObserver.observe();
        this.mHbmObserver.observe();
        this.mSkinThermalStatusObserver.observe();
        synchronized (this.mLock) {
            notifyDesiredDisplayModeSpecsChangedLocked();
        }
    }

    public void onBootCompleted() {
        this.mUdfpsObserver.observe();
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
        if (this.mLoggingEnabled == loggingEnabled) {
            return;
        }
        this.mLoggingEnabled = loggingEnabled;
        this.mBrightnessObserver.setLoggingEnabled(loggingEnabled);
    }

    private SparseArray<Vote> getVotesLocked(int displayId) {
        SparseArray<Vote> votes;
        SparseArray<Vote> displayVotes = this.mVotesByDisplay.get(displayId);
        if (displayVotes != null) {
            votes = displayVotes.clone();
        } else {
            votes = new SparseArray<>();
        }
        SparseArray<Vote> globalVotes = this.mVotesByDisplay.get(-1);
        if (globalVotes != null) {
            for (int i = 0; i < globalVotes.size(); i++) {
                int priority = globalVotes.keyAt(i);
                if (votes.indexOfKey(priority) < 0) {
                    votes.put(priority, globalVotes.valueAt(i));
                }
            }
        }
        return votes;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class VoteSummary {
        public float baseModeRefreshRate;
        public boolean disableRefreshRateSwitching;
        public int height;
        public float maxRefreshRate;
        public float minRefreshRate;
        public int width;

        VoteSummary() {
            reset();
        }

        public void reset() {
            this.minRefreshRate = 0.0f;
            this.maxRefreshRate = Float.POSITIVE_INFINITY;
            this.width = -1;
            this.height = -1;
            this.disableRefreshRateSwitching = false;
            this.baseModeRefreshRate = 0.0f;
        }
    }

    private void summarizeVotes(SparseArray<Vote> votes, int lowestConsideredPriority, int highestConsideredPriority, VoteSummary summary) {
        summary.reset();
        for (int priority = highestConsideredPriority; priority >= lowestConsideredPriority; priority--) {
            Vote vote = votes.get(priority);
            if (vote != null) {
                summary.minRefreshRate = Math.max(summary.minRefreshRate, vote.refreshRateRange.min);
                summary.maxRefreshRate = Math.min(summary.maxRefreshRate, vote.refreshRateRange.max);
                if (summary.height == -1 && summary.width == -1 && vote.height > 0 && vote.width > 0) {
                    summary.width = vote.width;
                    summary.height = vote.height;
                }
                if (!summary.disableRefreshRateSwitching && vote.disableRefreshRateSwitching) {
                    summary.disableRefreshRateSwitching = true;
                }
                if (summary.baseModeRefreshRate == 0.0f && vote.baseModeRefreshRate > 0.0f) {
                    summary.baseModeRefreshRate = vote.baseModeRefreshRate;
                }
            }
        }
    }

    public DesiredDisplayModeSpecs getDesiredDisplayModeSpecs(int displayId) {
        boolean z;
        synchronized (this.mLock) {
            SparseArray<Vote> votes = getVotesLocked(displayId);
            Display.Mode[] modes = this.mSupportedModesByDisplay.get(displayId);
            Display.Mode defaultMode = this.mDefaultModeByDisplay.get(displayId);
            if (modes != null && defaultMode != null) {
                ArrayList<Display.Mode> availableModes = new ArrayList<>();
                availableModes.add(defaultMode);
                VoteSummary primarySummary = new VoteSummary();
                int lowestConsideredPriority = 0;
                int highestConsideredPriority = 15;
                if (this.mAlwaysRespectAppRequest) {
                    lowestConsideredPriority = 7;
                    highestConsideredPriority = 8;
                }
                while (true) {
                    if (lowestConsideredPriority > highestConsideredPriority) {
                        break;
                    }
                    summarizeVotes(votes, lowestConsideredPriority, highestConsideredPriority, primarySummary);
                    if (primarySummary.height == -1 || primarySummary.width == -1) {
                        if (this.mIsResolutionSwitchByMode && filterResolutionSupport(modes) != null) {
                            Display.Mode resolutionSwitchSupportMode = filterResolutionSupport(modes);
                            primarySummary.width = resolutionSwitchSupportMode.getPhysicalWidth();
                            primarySummary.height = resolutionSwitchSupportMode.getPhysicalHeight();
                        } else {
                            primarySummary.width = defaultMode.getPhysicalWidth();
                            primarySummary.height = defaultMode.getPhysicalHeight();
                        }
                    }
                    availableModes = filterModes(modes, primarySummary);
                    if (!availableModes.isEmpty()) {
                        if (this.mLoggingEnabled) {
                            Slog.w(TAG, "Found available modes=" + availableModes + " with lowest priority considered " + Vote.priorityToString(lowestConsideredPriority) + " and constraints: width=" + primarySummary.width + ", height=" + primarySummary.height + ", minRefreshRate=" + primarySummary.minRefreshRate + ", maxRefreshRate=" + primarySummary.maxRefreshRate + ", disableRefreshRateSwitching=" + primarySummary.disableRefreshRateSwitching + ", baseModeRefreshRate=" + primarySummary.baseModeRefreshRate);
                        }
                    } else {
                        if (this.mLoggingEnabled) {
                            Slog.w(TAG, "Couldn't find available modes with lowest priority set to " + Vote.priorityToString(lowestConsideredPriority) + " and with the following constraints: width=" + primarySummary.width + ", height=" + primarySummary.height + ", minRefreshRate=" + primarySummary.minRefreshRate + ", maxRefreshRate=" + primarySummary.maxRefreshRate + ", disableRefreshRateSwitching=" + primarySummary.disableRefreshRateSwitching + ", baseModeRefreshRate=" + primarySummary.baseModeRefreshRate);
                        }
                        lowestConsideredPriority++;
                    }
                }
                VoteSummary appRequestSummary = new VoteSummary();
                summarizeVotes(votes, 4, 15, appRequestSummary);
                appRequestSummary.minRefreshRate = Math.min(appRequestSummary.minRefreshRate, primarySummary.minRefreshRate);
                appRequestSummary.maxRefreshRate = Math.max(appRequestSummary.maxRefreshRate, primarySummary.maxRefreshRate);
                if (!this.mLoggingEnabled) {
                    z = true;
                } else {
                    z = true;
                    Slog.i(TAG, String.format("App request range: [%.0f %.0f]", Float.valueOf(appRequestSummary.minRefreshRate), Float.valueOf(appRequestSummary.maxRefreshRate)));
                }
                Display.Mode baseMode = null;
                Iterator<Display.Mode> it = availableModes.iterator();
                while (it.hasNext()) {
                    Display.Mode availableMode = it.next();
                    if (primarySummary.baseModeRefreshRate >= availableMode.getRefreshRate() - 0.01f && primarySummary.baseModeRefreshRate <= availableMode.getRefreshRate() + 0.01f) {
                        baseMode = availableMode;
                    }
                }
                if (baseMode == null) {
                    Iterator<Display.Mode> it2 = availableModes.iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            break;
                        } else if (it2.next().getModeId() == defaultMode.getModeId()) {
                            baseMode = defaultMode;
                            break;
                        }
                    }
                }
                if (baseMode == null && !availableModes.isEmpty()) {
                    baseMode = availableModes.get(0);
                }
                if (baseMode != null) {
                    if (this.mModeSwitchingType == 0 || primarySummary.disableRefreshRateSwitching) {
                        float fps = baseMode.getRefreshRate();
                        primarySummary.maxRefreshRate = fps;
                        primarySummary.minRefreshRate = fps;
                        if (this.mModeSwitchingType == 0) {
                            appRequestSummary.maxRefreshRate = fps;
                            appRequestSummary.minRefreshRate = fps;
                        }
                    }
                    boolean allowGroupSwitching = this.mModeSwitchingType == 2 ? z : false;
                    return new DesiredDisplayModeSpecs(baseMode.getModeId(), allowGroupSwitching, new DisplayManagerInternal.RefreshRateRange(primarySummary.minRefreshRate, primarySummary.maxRefreshRate), new DisplayManagerInternal.RefreshRateRange(appRequestSummary.minRefreshRate, appRequestSummary.maxRefreshRate));
                }
                Slog.w(TAG, "Can't find a set of allowed modes which satisfies the votes. Falling back to the default mode. Display = " + displayId + ", votes = " + votes + ", supported modes = " + Arrays.toString(modes));
                float fps2 = defaultMode.getRefreshRate();
                return new DesiredDisplayModeSpecs(defaultMode.getModeId(), false, new DisplayManagerInternal.RefreshRateRange(fps2, fps2), new DisplayManagerInternal.RefreshRateRange(fps2, fps2));
            }
            Slog.e(TAG, "Asked about unknown display, returning empty display mode specs!(id=" + displayId + ")");
            return new DesiredDisplayModeSpecs();
        }
    }

    private Display.Mode filterResolutionSupport(Display.Mode[] supportedModes) {
        ContentResolver cr = this.mContext.getContentResolver();
        String resolution = Settings.System.getStringForUser(cr, getMtkSettingsExtSystemSetting("SWITCH_RESOLUTION_BY_MODE"), cr.getUserId());
        int modeId = Integer.parseInt(resolution);
        for (Display.Mode m : supportedModes) {
            if (modeId == m.getModeId()) {
                Slog.w(TAG, "Discarding mode " + m.getModeId() + ": actualWidth=" + m.getPhysicalWidth() + ": actualHeight=" + m.getPhysicalHeight());
                return m;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getMtkSettingsExtSystemSetting(String name) {
        try {
            Class<?> rCls = Class.forName("com.mediatek.provider.MtkSettingsExt$System", false, ClassLoader.getSystemClassLoader());
            Field field = rCls.getField(name);
            field.setAccessible(true);
            return (String) field.get(rCls);
        } catch (Exception e) {
            Slog.e(TAG, "Cannot get MTK settings - " + e);
            return "";
        }
    }

    private ArrayList<Display.Mode> filterModes(Display.Mode[] supportedModes, VoteSummary summary) {
        ArrayList<Display.Mode> availableModes = new ArrayList<>();
        boolean missingBaseModeRefreshRate = summary.baseModeRefreshRate > 0.0f;
        for (Display.Mode mode : supportedModes) {
            if (mode.getPhysicalWidth() != summary.width || mode.getPhysicalHeight() != summary.height) {
                if (this.mLoggingEnabled) {
                    Slog.w(TAG, "Discarding mode " + mode.getModeId() + ", wrong size: desiredWidth=" + summary.width + ": desiredHeight=" + summary.height + ": actualWidth=" + mode.getPhysicalWidth() + ": actualHeight=" + mode.getPhysicalHeight());
                }
            } else {
                float refreshRate = mode.getRefreshRate();
                if (refreshRate < summary.minRefreshRate - 0.01f || refreshRate > summary.maxRefreshRate + 0.01f) {
                    if (this.mLoggingEnabled) {
                        Slog.w(TAG, "Discarding mode " + mode.getModeId() + ", outside refresh rate bounds: minRefreshRate=" + summary.minRefreshRate + ", maxRefreshRate=" + summary.maxRefreshRate + ", modeRefreshRate=" + refreshRate);
                    }
                } else {
                    availableModes.add(mode);
                    if (mode.getRefreshRate() >= summary.baseModeRefreshRate - 0.01f && mode.getRefreshRate() <= summary.baseModeRefreshRate + 0.01f) {
                        missingBaseModeRefreshRate = false;
                    }
                }
            }
        }
        if (missingBaseModeRefreshRate) {
            return new ArrayList<>();
        }
        return availableModes;
    }

    public AppRequestObserver getAppRequestObserver() {
        return this.mAppRequestObserver;
    }

    public void setDesiredDisplayModeSpecsListener(DesiredDisplayModeSpecsListener desiredDisplayModeSpecsListener) {
        synchronized (this.mLock) {
            this.mDesiredDisplayModeSpecsListener = desiredDisplayModeSpecsListener;
        }
    }

    public void setShouldAlwaysRespectAppRequestedMode(boolean enabled) {
        synchronized (this.mLock) {
            if (this.mAlwaysRespectAppRequest != enabled) {
                this.mAlwaysRespectAppRequest = enabled;
                notifyDesiredDisplayModeSpecsChangedLocked();
            }
        }
    }

    public boolean shouldAlwaysRespectAppRequestedMode() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mAlwaysRespectAppRequest;
        }
        return z;
    }

    public void setModeSwitchingType(int newType) {
        synchronized (this.mLock) {
            if (newType != this.mModeSwitchingType) {
                this.mModeSwitchingType = newType;
                notifyDesiredDisplayModeSpecsChangedLocked();
            }
        }
    }

    public int getModeSwitchingType() {
        int i;
        synchronized (this.mLock) {
            i = this.mModeSwitchingType;
        }
        return i;
    }

    Vote getVote(int displayId, int priority) {
        Vote vote;
        synchronized (this.mLock) {
            SparseArray<Vote> votes = getVotesLocked(displayId);
            vote = votes.get(priority);
        }
        return vote;
    }

    public void updateSceneRefreshRateLocked(boolean happened) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeSceneVoteLocked() {
        if (this.mLoggingEnabled) {
            Slog.i(TAG, "removeSceneVoteLocked");
        }
        SparseArray<Vote> votes = getOrCreateVotesByDisplay(-1);
        votes.remove(4);
        if (votes.size() == 0) {
            if (this.mLoggingEnabled) {
                Slog.i(TAG, "No votes left for display -1, removing.");
            }
            this.mVotesByDisplay.remove(-1);
        }
    }

    public void dump(PrintWriter pw) {
        pw.println(TAG);
        synchronized (this.mLock) {
            pw.println("  mSupportedModesByDisplay:");
            for (int i = 0; i < this.mSupportedModesByDisplay.size(); i++) {
                int id = this.mSupportedModesByDisplay.keyAt(i);
                Display.Mode[] modes = this.mSupportedModesByDisplay.valueAt(i);
                pw.println("    " + id + " -> " + Arrays.toString(modes));
            }
            pw.println("  mDefaultModeByDisplay:");
            for (int i2 = 0; i2 < this.mDefaultModeByDisplay.size(); i2++) {
                int id2 = this.mDefaultModeByDisplay.keyAt(i2);
                Display.Mode mode = this.mDefaultModeByDisplay.valueAt(i2);
                pw.println("    " + id2 + " -> " + mode);
            }
            pw.println("  mVotesByDisplay:");
            for (int i3 = 0; i3 < this.mVotesByDisplay.size(); i3++) {
                pw.println("    " + this.mVotesByDisplay.keyAt(i3) + ":");
                SparseArray<Vote> votes = this.mVotesByDisplay.valueAt(i3);
                for (int p = 15; p >= 0; p--) {
                    Vote vote = votes.get(p);
                    if (vote != null) {
                        pw.println("      " + Vote.priorityToString(p) + " -> " + vote);
                    }
                }
            }
            pw.println("  mModeSwitchingType: " + switchingTypeToString(this.mModeSwitchingType));
            pw.println("  mAlwaysRespectAppRequest: " + this.mAlwaysRespectAppRequest);
            this.mSettingsObserver.dumpLocked(pw);
            this.mAppRequestObserver.dumpLocked(pw);
            this.mBrightnessObserver.dumpLocked(pw);
            this.mUdfpsObserver.dumpLocked(pw);
            this.mHbmObserver.dumpLocked(pw);
            this.mSkinThermalStatusObserver.dumpLocked(pw);
        }
        this.mSensorObserver.dump(pw);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVoteLocked(int priority, Vote vote) {
        updateVoteLocked(-1, priority, vote);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVoteLocked(int displayId, int priority, Vote vote) {
        if (this.mLoggingEnabled) {
            Slog.i(TAG, "updateVoteLocked(displayId=" + displayId + ", priority=" + Vote.priorityToString(priority) + ", vote=" + vote + ")");
        }
        if (priority < 0 || priority > 15) {
            Slog.w(TAG, "Received a vote with an invalid priority, ignoring: priority=" + Vote.priorityToString(priority) + ", vote=" + vote, new Throwable());
            return;
        }
        SparseArray<Vote> votes = getOrCreateVotesByDisplay(displayId);
        if (vote != null) {
            votes.put(priority, vote);
        } else {
            votes.remove(priority);
        }
        if (votes.size() == 0) {
            if (this.mLoggingEnabled) {
                Slog.i(TAG, "No votes left for display " + displayId + ", removing.");
            }
            this.mVotesByDisplay.remove(displayId);
        }
        notifyDesiredDisplayModeSpecsChangedLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyDesiredDisplayModeSpecsChangedLocked() {
        if (this.mDesiredDisplayModeSpecsListener != null && !this.mHandler.hasMessages(1)) {
            Message msg = this.mHandler.obtainMessage(1, this.mDesiredDisplayModeSpecsListener);
            msg.sendToTarget();
        }
    }

    private SparseArray<Vote> getOrCreateVotesByDisplay(int displayId) {
        if (this.mVotesByDisplay.indexOfKey(displayId) >= 0) {
            return this.mVotesByDisplay.get(displayId);
        }
        SparseArray<Vote> votes = new SparseArray<>();
        this.mVotesByDisplay.put(displayId, votes);
        return votes;
    }

    private static String switchingTypeToString(int type) {
        switch (type) {
            case 0:
                return "SWITCHING_TYPE_NONE";
            case 1:
                return "SWITCHING_TYPE_WITHIN_GROUPS";
            case 2:
                return "SWITCHING_TYPE_ACROSS_AND_WITHIN_GROUPS";
            default:
                return "Unknown SwitchingType " + type;
        }
    }

    void injectSupportedModesByDisplay(SparseArray<Display.Mode[]> supportedModesByDisplay) {
        this.mSupportedModesByDisplay = supportedModesByDisplay;
    }

    void injectDefaultModeByDisplay(SparseArray<Display.Mode> defaultModeByDisplay) {
        this.mDefaultModeByDisplay = defaultModeByDisplay;
    }

    void injectVotesByDisplay(SparseArray<SparseArray<Vote>> votesByDisplay) {
        this.mVotesByDisplay = votesByDisplay;
    }

    void injectBrightnessObserver(BrightnessObserver brightnessObserver) {
        this.mBrightnessObserver = brightnessObserver;
    }

    BrightnessObserver getBrightnessObserver() {
        return this.mBrightnessObserver;
    }

    SettingsObserver getSettingsObserver() {
        return this.mSettingsObserver;
    }

    UdfpsObserver getUdpfsObserver() {
        return this.mUdfpsObserver;
    }

    HbmObserver getHbmObserver() {
        return this.mHbmObserver;
    }

    DesiredDisplayModeSpecs getDesiredDisplayModeSpecsWithInjectedFpsSettings(float minRefreshRate, float peakRefreshRate, float defaultRefreshRate) {
        DesiredDisplayModeSpecs desiredDisplayModeSpecs;
        synchronized (this.mLock) {
            this.mSettingsObserver.updateRefreshRateSettingLocked(minRefreshRate, peakRefreshRate, defaultRefreshRate);
            desiredDisplayModeSpecs = getDesiredDisplayModeSpecs(0);
        }
        return desiredDisplayModeSpecs;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DisplayModeDirectorHandler extends Handler {
        DisplayModeDirectorHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    DesiredDisplayModeSpecsListener desiredDisplayModeSpecsListener = (DesiredDisplayModeSpecsListener) msg.obj;
                    desiredDisplayModeSpecsListener.onDesiredDisplayModeSpecsChanged();
                    return;
                case 2:
                    Pair<int[], int[]> thresholds = (Pair) msg.obj;
                    DisplayModeDirector.this.mBrightnessObserver.onDeviceConfigLowBrightnessThresholdsChanged((int[]) thresholds.first, (int[]) thresholds.second);
                    return;
                case 3:
                    Float defaultPeakRefreshRate = (Float) msg.obj;
                    DisplayModeDirector.this.mSettingsObserver.onDeviceConfigDefaultPeakRefreshRateChanged(defaultPeakRefreshRate);
                    return;
                case 4:
                    int refreshRateInZone = msg.arg1;
                    DisplayModeDirector.this.mBrightnessObserver.onDeviceConfigRefreshRateInLowZoneChanged(refreshRateInZone);
                    return;
                case 5:
                    int refreshRateInZone2 = msg.arg1;
                    DisplayModeDirector.this.mBrightnessObserver.onDeviceConfigRefreshRateInHighZoneChanged(refreshRateInZone2);
                    return;
                case 6:
                    Pair<int[], int[]> thresholds2 = (Pair) msg.obj;
                    DisplayModeDirector.this.mBrightnessObserver.onDeviceConfigHighBrightnessThresholdsChanged((int[]) thresholds2.first, (int[]) thresholds2.second);
                    return;
                case 7:
                    int refreshRateInHbmHdr = msg.arg1;
                    DisplayModeDirector.this.mHbmObserver.onDeviceConfigRefreshRateInHbmSunlightChanged(refreshRateInHbmHdr);
                    return;
                case 8:
                    int refreshRateInHbmHdr2 = msg.arg1;
                    DisplayModeDirector.this.mHbmObserver.onDeviceConfigRefreshRateInHbmHdrChanged(refreshRateInHbmHdr2);
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    public static final class DesiredDisplayModeSpecs {
        public boolean allowGroupSwitching;
        public final DisplayManagerInternal.RefreshRateRange appRequestRefreshRateRange;
        public int baseModeId;
        public final DisplayManagerInternal.RefreshRateRange primaryRefreshRateRange;

        public DesiredDisplayModeSpecs() {
            this.primaryRefreshRateRange = new DisplayManagerInternal.RefreshRateRange();
            this.appRequestRefreshRateRange = new DisplayManagerInternal.RefreshRateRange();
        }

        public DesiredDisplayModeSpecs(int baseModeId, boolean allowGroupSwitching, DisplayManagerInternal.RefreshRateRange primaryRefreshRateRange, DisplayManagerInternal.RefreshRateRange appRequestRefreshRateRange) {
            this.baseModeId = baseModeId;
            this.allowGroupSwitching = allowGroupSwitching;
            this.primaryRefreshRateRange = primaryRefreshRateRange;
            this.appRequestRefreshRateRange = appRequestRefreshRateRange;
        }

        public String toString() {
            return String.format("baseModeId=%d allowGroupSwitching=%b primaryRefreshRateRange=[%.0f %.0f] appRequestRefreshRateRange=[%.0f %.0f]", Integer.valueOf(this.baseModeId), Boolean.valueOf(this.allowGroupSwitching), Float.valueOf(this.primaryRefreshRateRange.min), Float.valueOf(this.primaryRefreshRateRange.max), Float.valueOf(this.appRequestRefreshRateRange.min), Float.valueOf(this.appRequestRefreshRateRange.max));
        }

        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (other instanceof DesiredDisplayModeSpecs) {
                DesiredDisplayModeSpecs desiredDisplayModeSpecs = (DesiredDisplayModeSpecs) other;
                return this.baseModeId == desiredDisplayModeSpecs.baseModeId && this.allowGroupSwitching == desiredDisplayModeSpecs.allowGroupSwitching && this.primaryRefreshRateRange.equals(desiredDisplayModeSpecs.primaryRefreshRateRange) && this.appRequestRefreshRateRange.equals(desiredDisplayModeSpecs.appRequestRefreshRateRange);
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.baseModeId), Boolean.valueOf(this.allowGroupSwitching), this.primaryRefreshRateRange, this.appRequestRefreshRateRange);
        }

        public void copyFrom(DesiredDisplayModeSpecs other) {
            this.baseModeId = other.baseModeId;
            this.allowGroupSwitching = other.allowGroupSwitching;
            this.primaryRefreshRateRange.min = other.primaryRefreshRateRange.min;
            this.primaryRefreshRateRange.max = other.primaryRefreshRateRange.max;
            this.appRequestRefreshRateRange.min = other.appRequestRefreshRateRange.min;
            this.appRequestRefreshRateRange.max = other.appRequestRefreshRateRange.max;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class Vote {
        public static final int APP_REQUEST_REFRESH_RATE_RANGE_PRIORITY_CUTOFF = 4;
        public static final int INVALID_SIZE = -1;
        public static final int MAX_PRIORITY = 15;
        public static final int MIN_PRIORITY = 0;
        public static final int PRIORITY_APP_REQUEST_BASE_MODE_REFRESH_RATE = 7;
        public static final int PRIORITY_APP_REQUEST_REFRESH_RATE_RANGE = 5;
        public static final int PRIORITY_APP_REQUEST_SIZE = 8;
        public static final int PRIORITY_DEFAULT_REFRESH_RATE = 0;
        public static final int PRIORITY_FLICKER_REFRESH_RATE = 1;
        public static final int PRIORITY_FLICKER_REFRESH_RATE_SWITCH = 12;
        public static final int PRIORITY_HIGH_BRIGHTNESS_MODE = 2;
        public static final int PRIORITY_LOW_POWER_MODE = 11;
        public static final int PRIORITY_MSYNC_REQUEST_REFRESH_RATE_RANGE = 6;
        public static final int PRIORITY_PROXIMITY = 14;
        public static final int PRIORITY_SKIN_TEMPERATURE = 13;
        public static final int PRIORITY_UDFPS = 15;
        public static final int PRIORITY_USER_SETTING_MIN_REFRESH_RATE = 3;
        public static final int PRIORITY_USER_SETTING_PEAK_REFRESH_RATE = 9;
        public static final int PRIORITY_USER_SETTING_SCENE_REFRESH_RATE = 4;
        public static final int PRIORITY_USER_SETTING_SCENE_REFRESH_RATE_FORCED = 10;
        public final float baseModeRefreshRate;
        public final boolean disableRefreshRateSwitching;
        public final int height;
        public final DisplayManagerInternal.RefreshRateRange refreshRateRange;
        public final int width;

        public static Vote forRefreshRates(float minRefreshRate, float maxRefreshRate) {
            return new Vote(-1, -1, minRefreshRate, maxRefreshRate, minRefreshRate == maxRefreshRate, 0.0f);
        }

        public static Vote forSize(int width, int height) {
            return new Vote(width, height, 0.0f, Float.POSITIVE_INFINITY, false, 0.0f);
        }

        public static Vote forDisableRefreshRateSwitching() {
            return new Vote(-1, -1, 0.0f, Float.POSITIVE_INFINITY, true, 0.0f);
        }

        public static Vote forBaseModeRefreshRate(float baseModeRefreshRate) {
            return new Vote(-1, -1, 0.0f, Float.POSITIVE_INFINITY, false, baseModeRefreshRate);
        }

        private Vote(int width, int height, float minRefreshRate, float maxRefreshRate, boolean disableRefreshRateSwitching, float baseModeRefreshRate) {
            this.width = width;
            this.height = height;
            this.refreshRateRange = new DisplayManagerInternal.RefreshRateRange(minRefreshRate, maxRefreshRate);
            this.disableRefreshRateSwitching = disableRefreshRateSwitching;
            this.baseModeRefreshRate = baseModeRefreshRate;
        }

        public static String priorityToString(int priority) {
            switch (priority) {
                case 0:
                    return "PRIORITY_DEFAULT_REFRESH_RATE";
                case 1:
                    return "PRIORITY_FLICKER_REFRESH_RATE";
                case 2:
                    return "PRIORITY_HIGH_BRIGHTNESS_MODE";
                case 3:
                    return "PRIORITY_USER_SETTING_MIN_REFRESH_RATE";
                case 4:
                    return "PRIORITY_USER_SETTING_SCENE_REFRESH_RATE";
                case 5:
                    return "PRIORITY_APP_REQUEST_REFRESH_RATE_RANGE";
                case 6:
                default:
                    return Integer.toString(priority);
                case 7:
                    return "PRIORITY_APP_REQUEST_BASE_MODE_REFRESH_RATE";
                case 8:
                    return "PRIORITY_APP_REQUEST_SIZE";
                case 9:
                    return "PRIORITY_USER_SETTING_PEAK_REFRESH_RATE";
                case 10:
                    return "PRIORITY_USER_SETTING_SCENE_REFRESH_RATE_FORCED";
                case 11:
                    return "PRIORITY_LOW_POWER_MODE";
                case 12:
                    return "PRIORITY_FLICKER_REFRESH_RATE_SWITCH";
                case 13:
                    return "PRIORITY_SKIN_TEMPERATURE";
                case 14:
                    return "PRIORITY_PROXIMITY";
                case 15:
                    return "PRIORITY_UDFPS";
            }
        }

        public String toString() {
            return "Vote{width=" + this.width + ", height=" + this.height + ", minRefreshRate=" + this.refreshRateRange.min + ", maxRefreshRate=" + this.refreshRateRange.max + ", disableRefreshRateSwitching=" + this.disableRefreshRateSwitching + ", baseModeRefreshRate=" + this.baseModeRefreshRate + "}";
        }
    }

    /* loaded from: classes.dex */
    final class SettingsObserver extends ContentObserver {
        private final Context mContext;
        private float mDefaultPeakRefreshRate;
        private float mDefaultRefreshRate;
        private final Uri mLowPowerModeSetting;
        private final Uri mMatchContentFrameRateSetting;
        private final Uri mMinRefreshRateSetting;
        private final Uri mPeakRefreshRateSetting;
        private final Uri mSwitchResolutionByMode;

        SettingsObserver(Context context, Handler handler) {
            super(handler);
            this.mPeakRefreshRateSetting = Settings.System.getUriFor("peak_refresh_rate");
            this.mMinRefreshRateSetting = Settings.System.getUriFor("min_refresh_rate");
            this.mLowPowerModeSetting = Settings.Global.getUriFor("low_power");
            this.mMatchContentFrameRateSetting = Settings.Secure.getUriFor("match_content_frame_rate");
            this.mSwitchResolutionByMode = Settings.System.getUriFor(DisplayModeDirector.this.getMtkSettingsExtSystemSetting("SWITCH_RESOLUTION_BY_MODE"));
            this.mContext = context;
            this.mDefaultPeakRefreshRate = context.getResources().getInteger(17694794);
            this.mDefaultRefreshRate = context.getResources().getInteger(17694796);
        }

        public void observe() {
            ContentResolver cr = this.mContext.getContentResolver();
            DisplayModeDirector.this.mInjector.registerPeakRefreshRateObserver(cr, this);
            cr.registerContentObserver(this.mMinRefreshRateSetting, false, this, 0);
            cr.registerContentObserver(this.mLowPowerModeSetting, false, this, 0);
            cr.registerContentObserver(this.mMatchContentFrameRateSetting, false, this);
            cr.registerContentObserver(this.mSwitchResolutionByMode, false, this);
            Float deviceConfigDefaultPeakRefresh = DisplayModeDirector.this.mDeviceConfigDisplaySettings.getDefaultPeakRefreshRate();
            if (deviceConfigDefaultPeakRefresh != null) {
                this.mDefaultPeakRefreshRate = deviceConfigDefaultPeakRefresh.floatValue();
            }
            synchronized (DisplayModeDirector.this.mLock) {
                updateRefreshRateSettingLocked();
                updateLowPowerModeSettingLocked();
                updateModeSwitchingTypeSettingLocked();
            }
        }

        public void setDefaultRefreshRate(float refreshRate) {
            synchronized (DisplayModeDirector.this.mLock) {
                this.mDefaultRefreshRate = refreshRate;
                updateRefreshRateSettingLocked();
            }
        }

        public void onDeviceConfigDefaultPeakRefreshRateChanged(Float defaultPeakRefreshRate) {
            if (defaultPeakRefreshRate == null) {
                defaultPeakRefreshRate = Float.valueOf(this.mContext.getResources().getInteger(17694794));
            }
            if (this.mDefaultPeakRefreshRate != defaultPeakRefreshRate.floatValue()) {
                synchronized (DisplayModeDirector.this.mLock) {
                    this.mDefaultPeakRefreshRate = defaultPeakRefreshRate.floatValue();
                    updateRefreshRateSettingLocked();
                }
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            synchronized (DisplayModeDirector.this.mLock) {
                if (!this.mPeakRefreshRateSetting.equals(uri) && !this.mMinRefreshRateSetting.equals(uri)) {
                    if (this.mLowPowerModeSetting.equals(uri)) {
                        updateLowPowerModeSettingLocked();
                    } else if (this.mMatchContentFrameRateSetting.equals(uri)) {
                        updateModeSwitchingTypeSettingLocked();
                    } else if (this.mSwitchResolutionByMode.equals(uri)) {
                        DisplayModeDirector.this.mIsResolutionSwitchByMode = true;
                        updateRefreshRateSettingLocked();
                    }
                }
                updateRefreshRateSettingLocked();
            }
        }

        private void updateLowPowerModeSettingLocked() {
            Vote vote;
            boolean inLowPowerMode = Settings.Global.getInt(this.mContext.getContentResolver(), "low_power", 0) != 0;
            if (inLowPowerMode) {
                vote = Vote.forRefreshRates(0.0f, 60.0f);
            } else {
                vote = null;
            }
            DisplayModeDirector.this.updateVoteLocked(11, vote);
            DisplayModeDirector.this.mBrightnessObserver.onLowPowerModeEnabledLocked(inLowPowerMode);
        }

        private void updateRefreshRateSettingLocked() {
            ContentResolver cr = this.mContext.getContentResolver();
            float minRefreshRate = Settings.System.getFloatForUser(cr, "min_refresh_rate", 0.0f, cr.getUserId());
            float peakRefreshRate = Settings.System.getFloatForUser(cr, "peak_refresh_rate", this.mDefaultPeakRefreshRate, cr.getUserId());
            updateRefreshRateSettingLocked(minRefreshRate, peakRefreshRate, this.mDefaultRefreshRate);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateRefreshRateSettingLocked(float minRefreshRate, float peakRefreshRate, float defaultRefreshRate) {
            Vote peakVote;
            float maxRefreshRate;
            if (peakRefreshRate != 0.0f) {
                peakVote = Vote.forRefreshRates(0.0f, Math.max(minRefreshRate, peakRefreshRate));
            } else {
                peakVote = null;
            }
            DisplayModeDirector.this.updateVoteLocked(9, peakVote);
            DisplayModeDirector.this.updateVoteLocked(3, Vote.forRefreshRates(minRefreshRate, Float.POSITIVE_INFINITY));
            Vote defaultVote = defaultRefreshRate != 0.0f ? Vote.forRefreshRates(0.0f, defaultRefreshRate) : null;
            DisplayModeDirector.this.updateVoteLocked(0, defaultVote);
            if (peakRefreshRate == 0.0f && defaultRefreshRate == 0.0f) {
                Slog.e(DisplayModeDirector.TAG, "Default and peak refresh rates are both 0. One of them should be set to a valid value.");
                maxRefreshRate = minRefreshRate;
            } else if (peakRefreshRate == 0.0f) {
                maxRefreshRate = defaultRefreshRate;
            } else if (defaultRefreshRate == 0.0f) {
                maxRefreshRate = peakRefreshRate;
            } else {
                maxRefreshRate = Math.min(defaultRefreshRate, peakRefreshRate);
            }
            DisplayModeDirector.this.mBrightnessObserver.onRefreshRateSettingChangedLocked(minRefreshRate, maxRefreshRate);
        }

        private void updateModeSwitchingTypeSettingLocked() {
            ContentResolver cr = this.mContext.getContentResolver();
            int switchingType = Settings.Secure.getIntForUser(cr, "match_content_frame_rate", DisplayModeDirector.this.mModeSwitchingType, cr.getUserId());
            if (switchingType != DisplayModeDirector.this.mModeSwitchingType) {
                DisplayModeDirector.this.mModeSwitchingType = switchingType;
                DisplayModeDirector.this.notifyDesiredDisplayModeSpecsChangedLocked();
            }
        }

        public void dumpLocked(PrintWriter pw) {
            pw.println("  SettingsObserver");
            pw.println("    mDefaultRefreshRate: " + this.mDefaultRefreshRate);
            pw.println("    mDefaultPeakRefreshRate: " + this.mDefaultPeakRefreshRate);
        }
    }

    /* loaded from: classes.dex */
    final class AppRequestObserver {
        private final SparseArray<Display.Mode> mAppRequestedModeByDisplay = new SparseArray<>();
        private final SparseArray<DisplayManagerInternal.RefreshRateRange> mAppPreferredRefreshRateRangeByDisplay = new SparseArray<>();

        AppRequestObserver() {
        }

        public void setAppRequest(int displayId, int modeId, float requestedMinRefreshRateRange, float requestedMaxRefreshRateRange) {
            synchronized (DisplayModeDirector.this.mLock) {
                setAppRequestedModeLocked(displayId, modeId);
                setAppPreferredRefreshRateRangeLocked(displayId, requestedMinRefreshRateRange, requestedMaxRefreshRateRange);
            }
        }

        private void setAppRequestedModeLocked(int displayId, int modeId) {
            Vote baseModeRefreshRateVote;
            Vote sizeVote;
            DisplayModeDirector.this.removeSceneVoteLocked();
            Display.Mode requestedMode = findModeByIdLocked(displayId, modeId);
            if (Objects.equals(requestedMode, this.mAppRequestedModeByDisplay.get(displayId))) {
                return;
            }
            if (requestedMode != null) {
                this.mAppRequestedModeByDisplay.put(displayId, requestedMode);
                baseModeRefreshRateVote = Vote.forBaseModeRefreshRate(requestedMode.getRefreshRate());
                sizeVote = Vote.forSize(requestedMode.getPhysicalWidth(), requestedMode.getPhysicalHeight());
            } else {
                this.mAppRequestedModeByDisplay.remove(displayId);
                baseModeRefreshRateVote = null;
                sizeVote = null;
            }
            DisplayModeDirector.this.updateVoteLocked(displayId, 7, baseModeRefreshRateVote);
            DisplayModeDirector.this.updateVoteLocked(displayId, 8, sizeVote);
        }

        private void setAppPreferredRefreshRateRangeLocked(int displayId, float requestedMinRefreshRateRange, float requestedMaxRefreshRateRange) {
            Vote vote;
            DisplayManagerInternal.RefreshRateRange refreshRateRange = null;
            if (requestedMinRefreshRateRange > 0.0f || requestedMaxRefreshRateRange > 0.0f) {
                float max = requestedMaxRefreshRateRange > 0.0f ? requestedMaxRefreshRateRange : Float.POSITIVE_INFINITY;
                refreshRateRange = new DisplayManagerInternal.RefreshRateRange(requestedMinRefreshRateRange, max);
                if (refreshRateRange.min == 0.0f && refreshRateRange.max == 0.0f) {
                    refreshRateRange = null;
                }
            }
            if (Objects.equals(refreshRateRange, this.mAppPreferredRefreshRateRangeByDisplay.get(displayId))) {
                return;
            }
            if (refreshRateRange != null) {
                this.mAppPreferredRefreshRateRangeByDisplay.put(displayId, refreshRateRange);
                vote = Vote.forRefreshRates(refreshRateRange.min, refreshRateRange.max);
            } else {
                this.mAppPreferredRefreshRateRangeByDisplay.remove(displayId);
                vote = null;
            }
            synchronized (DisplayModeDirector.this.mLock) {
                DisplayModeDirector.this.updateVoteLocked(displayId, 5, vote);
            }
        }

        private Display.Mode findModeByIdLocked(int displayId, int modeId) {
            Display.Mode[] modes = (Display.Mode[]) DisplayModeDirector.this.mSupportedModesByDisplay.get(displayId);
            if (modes == null) {
                return null;
            }
            for (Display.Mode mode : modes) {
                if (mode.getModeId() == modeId) {
                    return mode;
                }
            }
            return null;
        }

        public void dumpLocked(PrintWriter pw) {
            pw.println("  AppRequestObserver");
            pw.println("    mAppRequestedModeByDisplay:");
            for (int i = 0; i < this.mAppRequestedModeByDisplay.size(); i++) {
                int id = this.mAppRequestedModeByDisplay.keyAt(i);
                Display.Mode mode = this.mAppRequestedModeByDisplay.valueAt(i);
                pw.println("    " + id + " -> " + mode);
            }
            pw.println("    mAppPreferredRefreshRateRangeByDisplay:");
            for (int i2 = 0; i2 < this.mAppPreferredRefreshRateRangeByDisplay.size(); i2++) {
                int id2 = this.mAppPreferredRefreshRateRangeByDisplay.keyAt(i2);
                DisplayManagerInternal.RefreshRateRange refreshRateRange = this.mAppPreferredRefreshRateRangeByDisplay.valueAt(i2);
                pw.println("    " + id2 + " -> " + refreshRateRange);
            }
        }
    }

    /* loaded from: classes.dex */
    private final class DisplayObserver implements DisplayManager.DisplayListener {
        private final Context mContext;
        private final Handler mHandler;

        DisplayObserver(Context context, Handler handler) {
            this.mContext = context;
            this.mHandler = handler;
        }

        public void observe() {
            DisplayManager dm = (DisplayManager) this.mContext.getSystemService(DisplayManager.class);
            dm.registerDisplayListener(this, this.mHandler);
            SparseArray<Display.Mode[]> modes = new SparseArray<>();
            SparseArray<Display.Mode> defaultModes = new SparseArray<>();
            DisplayInfo info = new DisplayInfo();
            Display[] displays = dm.getDisplays();
            for (Display d : displays) {
                int displayId = d.getDisplayId();
                d.getDisplayInfo(info);
                modes.put(displayId, info.supportedModes);
                defaultModes.put(displayId, info.getDefaultMode());
            }
            synchronized (DisplayModeDirector.this.mLock) {
                int size = modes.size();
                for (int i = 0; i < size; i++) {
                    DisplayModeDirector.this.mSupportedModesByDisplay.put(modes.keyAt(i), modes.valueAt(i));
                    DisplayModeDirector.this.mDefaultModeByDisplay.put(defaultModes.keyAt(i), defaultModes.valueAt(i));
                }
            }
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
            updateDisplayModes(displayId);
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
            synchronized (DisplayModeDirector.this.mLock) {
                DisplayModeDirector.this.mSupportedModesByDisplay.remove(displayId);
                DisplayModeDirector.this.mDefaultModeByDisplay.remove(displayId);
            }
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            updateDisplayModes(displayId);
        }

        private void updateDisplayModes(int displayId) {
            Display d = ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(displayId);
            if (d == null) {
                return;
            }
            DisplayInfo info = new DisplayInfo();
            d.getDisplayInfo(info);
            boolean changed = false;
            synchronized (DisplayModeDirector.this.mLock) {
                if (!Arrays.equals((Object[]) DisplayModeDirector.this.mSupportedModesByDisplay.get(displayId), info.supportedModes)) {
                    DisplayModeDirector.this.mSupportedModesByDisplay.put(displayId, info.supportedModes);
                    changed = true;
                }
                if (!Objects.equals(DisplayModeDirector.this.mDefaultModeByDisplay.get(displayId), info.getDefaultMode())) {
                    changed = true;
                    DisplayModeDirector.this.mDefaultModeByDisplay.put(displayId, info.getDefaultMode());
                }
                if (changed) {
                    DisplayModeDirector.this.notifyDesiredDisplayModeSpecsChangedLocked();
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public class BrightnessObserver implements DisplayManager.DisplayListener {
        private static final int LIGHT_SENSOR_RATE_MS = 250;
        private AmbientFilter mAmbientFilter;
        private final Context mContext;
        private final Handler mHandler;
        private int[] mHighAmbientBrightnessThresholds;
        private int[] mHighDisplayBrightnessThresholds;
        private final Injector mInjector;
        private Sensor mLightSensor;
        private boolean mLoggingEnabled;
        private int[] mLowAmbientBrightnessThresholds;
        private int[] mLowDisplayBrightnessThresholds;
        private int mRefreshRateInHighZone;
        private int mRefreshRateInLowZone;
        private SensorManager mSensorManager;
        private boolean mShouldObserveAmbientHighChange;
        private boolean mShouldObserveAmbientLowChange;
        private boolean mShouldObserveDisplayHighChange;
        private boolean mShouldObserveDisplayLowChange;
        private final LightSensorEventListener mLightSensorListener = new LightSensorEventListener();
        private float mAmbientLux = -1.0f;
        private int mBrightness = -1;
        private int mDefaultDisplayState = 0;
        private boolean mRefreshRateChangeable = false;
        private boolean mLowPowerModeEnabled = false;

        BrightnessObserver(Context context, Handler handler, Injector injector) {
            this.mContext = context;
            this.mHandler = handler;
            this.mInjector = injector;
            this.mLowDisplayBrightnessThresholds = context.getResources().getIntArray(17236005);
            int[] intArray = context.getResources().getIntArray(17235984);
            this.mLowAmbientBrightnessThresholds = intArray;
            if (this.mLowDisplayBrightnessThresholds.length != intArray.length) {
                throw new RuntimeException("display low brightness threshold array and ambient brightness threshold array have different length: displayBrightnessThresholds=" + Arrays.toString(this.mLowDisplayBrightnessThresholds) + ", ambientBrightnessThresholds=" + Arrays.toString(this.mLowAmbientBrightnessThresholds));
            }
            this.mHighDisplayBrightnessThresholds = context.getResources().getIntArray(17236077);
            int[] intArray2 = context.getResources().getIntArray(17236076);
            this.mHighAmbientBrightnessThresholds = intArray2;
            if (this.mHighDisplayBrightnessThresholds.length != intArray2.length) {
                throw new RuntimeException("display high brightness threshold array and ambient brightness threshold array have different length: displayBrightnessThresholds=" + Arrays.toString(this.mHighDisplayBrightnessThresholds) + ", ambientBrightnessThresholds=" + Arrays.toString(this.mHighAmbientBrightnessThresholds));
            }
            this.mRefreshRateInHighZone = context.getResources().getInteger(17694837);
        }

        int getRefreshRateInLowZone() {
            return this.mRefreshRateInLowZone;
        }

        int[] getLowDisplayBrightnessThresholds() {
            return this.mLowDisplayBrightnessThresholds;
        }

        int[] getLowAmbientBrightnessThresholds() {
            return this.mLowAmbientBrightnessThresholds;
        }

        public void registerLightSensor(SensorManager sensorManager, Sensor lightSensor) {
            this.mSensorManager = sensorManager;
            this.mLightSensor = lightSensor;
            sensorManager.registerListener(this.mLightSensorListener, lightSensor, 250000, this.mHandler);
        }

        public void observe(SensorManager sensorManager) {
            this.mSensorManager = sensorManager;
            this.mContext.getContentResolver();
            this.mBrightness = getBrightness(0);
            int[] lowDisplayBrightnessThresholds = DisplayModeDirector.this.mDeviceConfigDisplaySettings.getLowDisplayBrightnessThresholds();
            int[] lowAmbientBrightnessThresholds = DisplayModeDirector.this.mDeviceConfigDisplaySettings.getLowAmbientBrightnessThresholds();
            if (lowDisplayBrightnessThresholds != null && lowAmbientBrightnessThresholds != null && lowDisplayBrightnessThresholds.length == lowAmbientBrightnessThresholds.length) {
                this.mLowDisplayBrightnessThresholds = lowDisplayBrightnessThresholds;
                this.mLowAmbientBrightnessThresholds = lowAmbientBrightnessThresholds;
            }
            int[] highDisplayBrightnessThresholds = DisplayModeDirector.this.mDeviceConfigDisplaySettings.getHighDisplayBrightnessThresholds();
            int[] highAmbientBrightnessThresholds = DisplayModeDirector.this.mDeviceConfigDisplaySettings.getHighAmbientBrightnessThresholds();
            if (highDisplayBrightnessThresholds != null && highAmbientBrightnessThresholds != null && highDisplayBrightnessThresholds.length == highAmbientBrightnessThresholds.length) {
                this.mHighDisplayBrightnessThresholds = highDisplayBrightnessThresholds;
                this.mHighAmbientBrightnessThresholds = highAmbientBrightnessThresholds;
            }
            this.mRefreshRateInLowZone = DisplayModeDirector.this.mDeviceConfigDisplaySettings.getRefreshRateInLowZone();
            this.mRefreshRateInHighZone = DisplayModeDirector.this.mDeviceConfigDisplaySettings.getRefreshRateInHighZone();
            restartObserver();
            DisplayModeDirector.this.mDeviceConfigDisplaySettings.startListening();
            this.mInjector.registerDisplayListener(this, this.mHandler, 12L);
        }

        public void setLoggingEnabled(boolean loggingEnabled) {
            if (this.mLoggingEnabled == loggingEnabled) {
                return;
            }
            this.mLoggingEnabled = loggingEnabled;
            this.mLightSensorListener.setLoggingEnabled(loggingEnabled);
        }

        public void onRefreshRateSettingChangedLocked(float min, float max) {
            boolean changeable = max - min > 1.0f && max > 60.0f;
            if (this.mRefreshRateChangeable != changeable) {
                this.mRefreshRateChangeable = changeable;
                updateSensorStatus();
                if (!changeable) {
                    DisplayModeDirector.this.updateVoteLocked(1, null);
                    DisplayModeDirector.this.updateVoteLocked(12, null);
                }
            }
        }

        public void onLowPowerModeEnabledLocked(boolean b) {
            if (this.mLowPowerModeEnabled != b) {
                this.mLowPowerModeEnabled = b;
                updateSensorStatus();
            }
        }

        public void onDeviceConfigLowBrightnessThresholdsChanged(int[] displayThresholds, int[] ambientThresholds) {
            if (displayThresholds != null && ambientThresholds != null && displayThresholds.length == ambientThresholds.length) {
                this.mLowDisplayBrightnessThresholds = displayThresholds;
                this.mLowAmbientBrightnessThresholds = ambientThresholds;
            } else {
                this.mLowDisplayBrightnessThresholds = this.mContext.getResources().getIntArray(17236005);
                this.mLowAmbientBrightnessThresholds = this.mContext.getResources().getIntArray(17235984);
            }
            restartObserver();
        }

        public void onDeviceConfigRefreshRateInLowZoneChanged(int refreshRate) {
            if (refreshRate != this.mRefreshRateInLowZone) {
                this.mRefreshRateInLowZone = refreshRate;
                restartObserver();
            }
        }

        public void onDeviceConfigHighBrightnessThresholdsChanged(int[] displayThresholds, int[] ambientThresholds) {
            if (displayThresholds != null && ambientThresholds != null && displayThresholds.length == ambientThresholds.length) {
                this.mHighDisplayBrightnessThresholds = displayThresholds;
                this.mHighAmbientBrightnessThresholds = ambientThresholds;
            } else {
                this.mHighDisplayBrightnessThresholds = this.mContext.getResources().getIntArray(17236077);
                this.mHighAmbientBrightnessThresholds = this.mContext.getResources().getIntArray(17236076);
            }
            restartObserver();
        }

        public void onDeviceConfigRefreshRateInHighZoneChanged(int refreshRate) {
            if (refreshRate != this.mRefreshRateInHighZone) {
                this.mRefreshRateInHighZone = refreshRate;
                restartObserver();
            }
        }

        public void dumpLocked(PrintWriter pw) {
            int[] iArr;
            int[] iArr2;
            int[] iArr3;
            int[] iArr4;
            pw.println("  BrightnessObserver");
            pw.println("    mAmbientLux: " + this.mAmbientLux);
            pw.println("    mBrightness: " + this.mBrightness);
            pw.println("    mDefaultDisplayState: " + this.mDefaultDisplayState);
            pw.println("    mLowPowerModeEnabled: " + this.mLowPowerModeEnabled);
            pw.println("    mRefreshRateChangeable: " + this.mRefreshRateChangeable);
            pw.println("    mShouldObserveDisplayLowChange: " + this.mShouldObserveDisplayLowChange);
            pw.println("    mShouldObserveAmbientLowChange: " + this.mShouldObserveAmbientLowChange);
            pw.println("    mRefreshRateInLowZone: " + this.mRefreshRateInLowZone);
            for (int d : this.mLowDisplayBrightnessThresholds) {
                pw.println("    mDisplayLowBrightnessThreshold: " + d);
            }
            for (int d2 : this.mLowAmbientBrightnessThresholds) {
                pw.println("    mAmbientLowBrightnessThreshold: " + d2);
            }
            pw.println("    mShouldObserveDisplayHighChange: " + this.mShouldObserveDisplayHighChange);
            pw.println("    mShouldObserveAmbientHighChange: " + this.mShouldObserveAmbientHighChange);
            pw.println("    mRefreshRateInHighZone: " + this.mRefreshRateInHighZone);
            for (int d3 : this.mHighDisplayBrightnessThresholds) {
                pw.println("    mDisplayHighBrightnessThresholds: " + d3);
            }
            for (int d4 : this.mHighAmbientBrightnessThresholds) {
                pw.println("    mAmbientHighBrightnessThresholds: " + d4);
            }
            this.mLightSensorListener.dumpLocked(pw);
            if (this.mAmbientFilter != null) {
                this.mAmbientFilter.dump(new IndentingPrintWriter(pw, "    "));
            }
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            if (displayId == 0) {
                updateDefaultDisplayState();
                int brightness = getBrightness(displayId);
                synchronized (DisplayModeDirector.this.mLock) {
                    if (brightness != this.mBrightness) {
                        this.mBrightness = brightness;
                        onBrightnessChangedLocked();
                    }
                }
            }
        }

        private void restartObserver() {
            if (this.mRefreshRateInLowZone > 0) {
                this.mShouldObserveDisplayLowChange = hasValidThreshold(this.mLowDisplayBrightnessThresholds);
                this.mShouldObserveAmbientLowChange = hasValidThreshold(this.mLowAmbientBrightnessThresholds);
            } else {
                this.mShouldObserveDisplayLowChange = false;
                this.mShouldObserveAmbientLowChange = false;
            }
            if (this.mRefreshRateInHighZone > 0) {
                this.mShouldObserveDisplayHighChange = hasValidThreshold(this.mHighDisplayBrightnessThresholds);
                this.mShouldObserveAmbientHighChange = hasValidThreshold(this.mHighAmbientBrightnessThresholds);
            } else {
                this.mShouldObserveDisplayHighChange = false;
                this.mShouldObserveAmbientHighChange = false;
            }
            if (this.mShouldObserveAmbientLowChange || this.mShouldObserveAmbientHighChange) {
                Resources resources = this.mContext.getResources();
                String lightSensorType = resources.getString(17039959);
                Sensor lightSensor = null;
                if (!TextUtils.isEmpty(lightSensorType)) {
                    List<Sensor> sensors = this.mSensorManager.getSensorList(-1);
                    int i = 0;
                    while (true) {
                        if (i >= sensors.size()) {
                            break;
                        }
                        Sensor sensor = sensors.get(i);
                        if (!lightSensorType.equals(sensor.getStringType())) {
                            i++;
                        } else {
                            lightSensor = sensor;
                            break;
                        }
                    }
                }
                if (lightSensor == null) {
                    lightSensor = this.mSensorManager.getDefaultSensor(5);
                }
                if (lightSensor != null) {
                    Resources res = this.mContext.getResources();
                    this.mAmbientFilter = AmbientFilterFactory.createBrightnessFilter(DisplayModeDirector.TAG, res);
                    this.mLightSensor = lightSensor;
                }
            } else {
                this.mAmbientFilter = null;
                this.mLightSensor = null;
            }
            if (this.mRefreshRateChangeable) {
                updateSensorStatus();
                synchronized (DisplayModeDirector.this.mLock) {
                    onBrightnessChangedLocked();
                }
            }
        }

        private boolean hasValidThreshold(int[] a) {
            for (int d : a) {
                if (d >= 0) {
                    return true;
                }
            }
            return false;
        }

        private boolean isInsideLowZone(int brightness, float lux) {
            int i = 0;
            while (true) {
                int[] iArr = this.mLowDisplayBrightnessThresholds;
                if (i < iArr.length) {
                    int disp = iArr[i];
                    int ambi = this.mLowAmbientBrightnessThresholds[i];
                    if (disp >= 0 && ambi >= 0) {
                        if (brightness <= disp && lux <= ambi) {
                            return true;
                        }
                    } else if (disp >= 0) {
                        if (brightness <= disp) {
                            return true;
                        }
                    } else if (ambi >= 0 && lux <= ambi) {
                        return true;
                    }
                    i++;
                } else {
                    return false;
                }
            }
        }

        private boolean isInsideHighZone(int brightness, float lux) {
            int i = 0;
            while (true) {
                int[] iArr = this.mHighDisplayBrightnessThresholds;
                if (i < iArr.length) {
                    int disp = iArr[i];
                    int ambi = this.mHighAmbientBrightnessThresholds[i];
                    if (disp >= 0 && ambi >= 0) {
                        if (brightness >= disp && lux >= ambi) {
                            return true;
                        }
                    } else if (disp >= 0) {
                        if (brightness >= disp) {
                            return true;
                        }
                    } else if (ambi >= 0 && lux >= ambi) {
                        return true;
                    }
                    i++;
                } else {
                    return false;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onBrightnessChangedLocked() {
            Vote refreshRateVote = null;
            Vote refreshRateSwitchingVote = null;
            if (this.mBrightness < 0) {
                return;
            }
            boolean insideHighZone = false;
            boolean insideLowZone = hasValidLowZone() && isInsideLowZone(this.mBrightness, this.mAmbientLux);
            if (insideLowZone) {
                int i = this.mRefreshRateInLowZone;
                refreshRateVote = Vote.forRefreshRates(i, i);
                refreshRateSwitchingVote = Vote.forDisableRefreshRateSwitching();
            }
            if (hasValidHighZone() && isInsideHighZone(this.mBrightness, this.mAmbientLux)) {
                insideHighZone = true;
            }
            if (insideHighZone) {
                int i2 = this.mRefreshRateInHighZone;
                refreshRateVote = Vote.forRefreshRates(i2, i2);
                refreshRateSwitchingVote = Vote.forDisableRefreshRateSwitching();
            }
            if (this.mLoggingEnabled) {
                Slog.d(DisplayModeDirector.TAG, "Display brightness " + this.mBrightness + ", ambient lux " + this.mAmbientLux + ", Vote " + refreshRateVote);
            }
            DisplayModeDirector.this.updateVoteLocked(1, refreshRateVote);
            DisplayModeDirector.this.updateVoteLocked(12, refreshRateSwitchingVote);
        }

        private boolean hasValidLowZone() {
            return this.mRefreshRateInLowZone > 0 && (this.mShouldObserveDisplayLowChange || this.mShouldObserveAmbientLowChange);
        }

        private boolean hasValidHighZone() {
            return this.mRefreshRateInHighZone > 0 && (this.mShouldObserveDisplayHighChange || this.mShouldObserveAmbientHighChange);
        }

        private void updateDefaultDisplayState() {
            Display display = ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(0);
            if (display == null) {
                return;
            }
            setDefaultDisplayState(display.getState());
        }

        public void setDefaultDisplayState(int state) {
            if (this.mLoggingEnabled) {
                Slog.d(DisplayModeDirector.TAG, "setDefaultDisplayState: mDefaultDisplayState = " + this.mDefaultDisplayState + ", state = " + state);
            }
            if (this.mDefaultDisplayState != state) {
                this.mDefaultDisplayState = state;
                updateSensorStatus();
            }
        }

        private void updateSensorStatus() {
            if (this.mSensorManager == null || this.mLightSensorListener == null) {
                return;
            }
            if (this.mLoggingEnabled) {
                Slog.d(DisplayModeDirector.TAG, "updateSensorStatus: mShouldObserveAmbientLowChange = " + this.mShouldObserveAmbientLowChange + ", mShouldObserveAmbientHighChange = " + this.mShouldObserveAmbientHighChange);
                Slog.d(DisplayModeDirector.TAG, "updateSensorStatus: mLowPowerModeEnabled = " + this.mLowPowerModeEnabled + ", mRefreshRateChangeable = " + this.mRefreshRateChangeable);
            }
            if ((this.mShouldObserveAmbientLowChange || this.mShouldObserveAmbientHighChange) && isDeviceActive() && !this.mLowPowerModeEnabled && this.mRefreshRateChangeable) {
                this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, 250000, this.mHandler);
                if (this.mLoggingEnabled) {
                    Slog.d(DisplayModeDirector.TAG, "updateSensorStatus: registerListener");
                    return;
                }
                return;
            }
            this.mLightSensorListener.removeCallbacks();
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
            if (this.mLoggingEnabled) {
                Slog.d(DisplayModeDirector.TAG, "updateSensorStatus: unregisterListener");
            }
        }

        private boolean isDeviceActive() {
            return this.mDefaultDisplayState == 2;
        }

        private int getBrightness(int displayId) {
            BrightnessInfo info = this.mInjector.getBrightnessInfo(displayId);
            if (info != null) {
                return BrightnessSynchronizer.brightnessFloatToInt(info.adjustedBrightness);
            }
            return -1;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public final class LightSensorEventListener implements SensorEventListener {
            private static final int INJECT_EVENTS_INTERVAL_MS = 250;
            private final Runnable mInjectSensorEventRunnable;
            private float mLastSensorData;
            private boolean mLoggingEnabled;
            private long mTimestamp;

            private LightSensorEventListener() {
                this.mInjectSensorEventRunnable = new Runnable() { // from class: com.android.server.display.DisplayModeDirector.BrightnessObserver.LightSensorEventListener.1
                    @Override // java.lang.Runnable
                    public void run() {
                        long now = SystemClock.uptimeMillis();
                        LightSensorEventListener.this.processSensorData(now);
                        LightSensorEventListener lightSensorEventListener = LightSensorEventListener.this;
                        if (!lightSensorEventListener.isDifferentZone(lightSensorEventListener.mLastSensorData, BrightnessObserver.this.mAmbientLux, BrightnessObserver.this.mLowAmbientBrightnessThresholds)) {
                            LightSensorEventListener lightSensorEventListener2 = LightSensorEventListener.this;
                            if (!lightSensorEventListener2.isDifferentZone(lightSensorEventListener2.mLastSensorData, BrightnessObserver.this.mAmbientLux, BrightnessObserver.this.mHighAmbientBrightnessThresholds)) {
                                return;
                            }
                        }
                        BrightnessObserver.this.mHandler.postDelayed(LightSensorEventListener.this.mInjectSensorEventRunnable, 250L);
                    }
                };
            }

            public void dumpLocked(PrintWriter pw) {
                pw.println("    mLastSensorData: " + this.mLastSensorData);
                pw.println("    mTimestamp: " + formatTimestamp(this.mTimestamp));
            }

            public void setLoggingEnabled(boolean loggingEnabled) {
                if (this.mLoggingEnabled == loggingEnabled) {
                    return;
                }
                this.mLoggingEnabled = loggingEnabled;
            }

            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                this.mLastSensorData = event.values[0];
                if (this.mLoggingEnabled) {
                    Slog.d(DisplayModeDirector.TAG, "On sensor changed: " + this.mLastSensorData);
                }
                boolean lowZoneChanged = isDifferentZone(this.mLastSensorData, BrightnessObserver.this.mAmbientLux, BrightnessObserver.this.mLowAmbientBrightnessThresholds);
                boolean highZoneChanged = isDifferentZone(this.mLastSensorData, BrightnessObserver.this.mAmbientLux, BrightnessObserver.this.mHighAmbientBrightnessThresholds);
                if (((lowZoneChanged && this.mLastSensorData < BrightnessObserver.this.mAmbientLux) || (highZoneChanged && this.mLastSensorData > BrightnessObserver.this.mAmbientLux)) && BrightnessObserver.this.mAmbientFilter != null) {
                    BrightnessObserver.this.mAmbientFilter.clear();
                }
                long now = SystemClock.uptimeMillis();
                this.mTimestamp = System.currentTimeMillis();
                if (BrightnessObserver.this.mAmbientFilter != null) {
                    BrightnessObserver.this.mAmbientFilter.addValue(now, this.mLastSensorData);
                }
                BrightnessObserver.this.mHandler.removeCallbacks(this.mInjectSensorEventRunnable);
                processSensorData(now);
                if ((lowZoneChanged && this.mLastSensorData > BrightnessObserver.this.mAmbientLux) || (highZoneChanged && this.mLastSensorData < BrightnessObserver.this.mAmbientLux)) {
                    BrightnessObserver.this.mHandler.postDelayed(this.mInjectSensorEventRunnable, 250L);
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void removeCallbacks() {
                BrightnessObserver.this.mHandler.removeCallbacks(this.mInjectSensorEventRunnable);
            }

            private String formatTimestamp(long time) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
                return dateFormat.format(new Date(time));
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void processSensorData(long now) {
                if (BrightnessObserver.this.mAmbientFilter != null) {
                    BrightnessObserver brightnessObserver = BrightnessObserver.this;
                    brightnessObserver.mAmbientLux = brightnessObserver.mAmbientFilter.getEstimate(now);
                } else {
                    BrightnessObserver.this.mAmbientLux = this.mLastSensorData;
                }
                synchronized (DisplayModeDirector.this.mLock) {
                    BrightnessObserver.this.onBrightnessChangedLocked();
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public boolean isDifferentZone(float lux1, float lux2, int[] luxThresholds) {
                for (float boundary : luxThresholds) {
                    if (lux1 <= boundary && lux2 > boundary) {
                        return true;
                    }
                    if (lux1 > boundary && lux2 <= boundary) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /* loaded from: classes.dex */
    private class UdfpsObserver extends IUdfpsHbmListener.Stub {
        private final SparseBooleanArray mLocalHbmEnabled;

        private UdfpsObserver() {
            this.mLocalHbmEnabled = new SparseBooleanArray();
        }

        public void observe() {
            StatusBarManagerInternal statusBar = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            if (statusBar != null) {
                statusBar.setUdfpsHbmListener(this);
            }
        }

        public void onHbmEnabled(int displayId) {
            synchronized (DisplayModeDirector.this.mLock) {
                updateHbmStateLocked(displayId, true);
            }
        }

        public void onHbmDisabled(int displayId) {
            synchronized (DisplayModeDirector.this.mLock) {
                updateHbmStateLocked(displayId, false);
            }
        }

        private void updateHbmStateLocked(int displayId, boolean enabled) {
            this.mLocalHbmEnabled.put(displayId, enabled);
            updateVoteLocked(displayId);
        }

        private void updateVoteLocked(int displayId) {
            Vote vote;
            if (this.mLocalHbmEnabled.get(displayId)) {
                Display.Mode[] modes = (Display.Mode[]) DisplayModeDirector.this.mSupportedModesByDisplay.get(displayId);
                float maxRefreshRate = 0.0f;
                for (Display.Mode mode : modes) {
                    if (mode.getRefreshRate() > maxRefreshRate) {
                        maxRefreshRate = mode.getRefreshRate();
                    }
                }
                vote = Vote.forRefreshRates(maxRefreshRate, maxRefreshRate);
            } else {
                vote = null;
            }
            DisplayModeDirector.this.updateVoteLocked(displayId, 15, vote);
        }

        void dumpLocked(PrintWriter pw) {
            pw.println("  UdfpsObserver");
            pw.println("    mLocalHbmEnabled: ");
            for (int i = 0; i < this.mLocalHbmEnabled.size(); i++) {
                int displayId = this.mLocalHbmEnabled.keyAt(i);
                String enabled = this.mLocalHbmEnabled.valueAt(i) ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED;
                pw.println("      Display " + displayId + ": " + enabled);
            }
        }
    }

    /* loaded from: classes.dex */
    private static final class SensorObserver implements SensorManagerInternal.ProximityActiveListener, DisplayManager.DisplayListener {
        private final BallotBox mBallotBox;
        private final Context mContext;
        private DisplayManager mDisplayManager;
        private DisplayManagerInternal mDisplayManagerInternal;
        private final Injector mInjector;
        private final String mProximitySensorName = null;
        private final String mProximitySensorType = "android.sensor.proximity";
        private final SparseBooleanArray mDozeStateByDisplay = new SparseBooleanArray();
        private final Object mSensorObserverLock = new Object();
        private boolean mIsProxActive = false;

        SensorObserver(Context context, BallotBox ballotBox, Injector injector) {
            this.mContext = context;
            this.mBallotBox = ballotBox;
            this.mInjector = injector;
        }

        @Override // com.android.server.sensors.SensorManagerInternal.ProximityActiveListener
        public void onProximityActive(boolean isActive) {
            synchronized (this.mSensorObserverLock) {
                if (this.mIsProxActive != isActive) {
                    this.mIsProxActive = isActive;
                    recalculateVotesLocked();
                }
            }
        }

        public void observe() {
            Display[] displays;
            this.mDisplayManager = (DisplayManager) this.mContext.getSystemService(DisplayManager.class);
            this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
            SensorManagerInternal sensorManager = (SensorManagerInternal) LocalServices.getService(SensorManagerInternal.class);
            sensorManager.addProximityActiveListener(BackgroundThread.getExecutor(), this);
            synchronized (this.mSensorObserverLock) {
                for (Display d : this.mDisplayManager.getDisplays()) {
                    this.mDozeStateByDisplay.put(d.getDisplayId(), this.mInjector.isDozeState(d));
                }
            }
            this.mInjector.registerDisplayListener(this, BackgroundThread.getHandler(), 7L);
        }

        private void recalculateVotesLocked() {
            DisplayManagerInternal.RefreshRateRange rate;
            Display[] displays = this.mDisplayManager.getDisplays();
            for (Display d : displays) {
                int displayId = d.getDisplayId();
                Vote vote = null;
                if (this.mIsProxActive && !this.mDozeStateByDisplay.get(displayId) && (rate = this.mDisplayManagerInternal.getRefreshRateForDisplayAndSensor(displayId, this.mProximitySensorName, "android.sensor.proximity")) != null) {
                    vote = Vote.forRefreshRates(rate.min, rate.max);
                }
                this.mBallotBox.vote(displayId, 14, vote);
            }
        }

        void dump(PrintWriter pw) {
            pw.println("  SensorObserver");
            synchronized (this.mSensorObserverLock) {
                pw.println("    mIsProxActive=" + this.mIsProxActive);
                pw.println("    mDozeStateByDisplay:");
                for (int i = 0; i < this.mDozeStateByDisplay.size(); i++) {
                    int id = this.mDozeStateByDisplay.keyAt(i);
                    boolean dozed = this.mDozeStateByDisplay.valueAt(i);
                    pw.println("      " + id + " -> " + dozed);
                }
            }
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
            boolean isDozeState = this.mInjector.isDozeState(this.mDisplayManager.getDisplay(displayId));
            synchronized (this.mSensorObserverLock) {
                this.mDozeStateByDisplay.put(displayId, isDozeState);
                recalculateVotesLocked();
            }
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            boolean wasDozeState = this.mDozeStateByDisplay.get(displayId);
            synchronized (this.mSensorObserverLock) {
                this.mDozeStateByDisplay.put(displayId, this.mInjector.isDozeState(this.mDisplayManager.getDisplay(displayId)));
                if (wasDozeState != this.mDozeStateByDisplay.get(displayId)) {
                    recalculateVotesLocked();
                }
            }
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
            synchronized (this.mSensorObserverLock) {
                this.mDozeStateByDisplay.delete(displayId);
                recalculateVotesLocked();
            }
        }
    }

    /* loaded from: classes.dex */
    public static class HbmObserver implements DisplayManager.DisplayListener {
        private final BallotBox mBallotBox;
        private final DeviceConfigDisplaySettings mDeviceConfigDisplaySettings;
        private DisplayManagerInternal mDisplayManagerInternal;
        private final Handler mHandler;
        private final Injector mInjector;
        private int mRefreshRateInHbmHdr;
        private int mRefreshRateInHbmSunlight;
        private final SparseIntArray mHbmMode = new SparseIntArray();
        private final SparseBooleanArray mHbmActive = new SparseBooleanArray();

        HbmObserver(Injector injector, BallotBox ballotBox, Handler handler, DeviceConfigDisplaySettings displaySettings) {
            this.mInjector = injector;
            this.mBallotBox = ballotBox;
            this.mHandler = handler;
            this.mDeviceConfigDisplaySettings = displaySettings;
        }

        public void observe() {
            this.mRefreshRateInHbmSunlight = this.mDeviceConfigDisplaySettings.getRefreshRateInHbmSunlight();
            this.mRefreshRateInHbmHdr = this.mDeviceConfigDisplaySettings.getRefreshRateInHbmHdr();
            this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
            this.mInjector.registerDisplayListener(this, this.mHandler, 10L);
        }

        int getRefreshRateInHbmSunlight() {
            return this.mRefreshRateInHbmSunlight;
        }

        int getRefreshRateInHbmHdr() {
            return this.mRefreshRateInHbmHdr;
        }

        public void onDeviceConfigRefreshRateInHbmSunlightChanged(int refreshRate) {
            if (refreshRate != this.mRefreshRateInHbmSunlight) {
                this.mRefreshRateInHbmSunlight = refreshRate;
                onDeviceConfigRefreshRateInHbmChanged();
            }
        }

        public void onDeviceConfigRefreshRateInHbmHdrChanged(int refreshRate) {
            if (refreshRate != this.mRefreshRateInHbmHdr) {
                this.mRefreshRateInHbmHdr = refreshRate;
                onDeviceConfigRefreshRateInHbmChanged();
            }
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
            this.mBallotBox.vote(displayId, 2, null);
            this.mHbmMode.delete(displayId);
            this.mHbmActive.delete(displayId);
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            BrightnessInfo info = this.mInjector.getBrightnessInfo(displayId);
            if (info == null) {
                return;
            }
            int hbmMode = info.highBrightnessMode;
            boolean isHbmActive = hbmMode != 0 && info.adjustedBrightness > info.highBrightnessTransitionPoint;
            if (hbmMode == this.mHbmMode.get(displayId) && isHbmActive == this.mHbmActive.get(displayId)) {
                return;
            }
            this.mHbmMode.put(displayId, hbmMode);
            this.mHbmActive.put(displayId, isHbmActive);
            recalculateVotesForDisplay(displayId);
        }

        private void onDeviceConfigRefreshRateInHbmChanged() {
            int[] displayIds = this.mHbmMode.copyKeys();
            if (displayIds != null) {
                for (int id : displayIds) {
                    recalculateVotesForDisplay(id);
                }
            }
        }

        private void recalculateVotesForDisplay(int displayId) {
            int i;
            Vote vote = null;
            if (this.mHbmActive.get(displayId, false)) {
                int hbmMode = this.mHbmMode.get(displayId, 0);
                if (hbmMode == 1) {
                    int i2 = this.mRefreshRateInHbmSunlight;
                    if (i2 > 0) {
                        vote = Vote.forRefreshRates(i2, i2);
                    } else {
                        List<DisplayManagerInternal.RefreshRateLimitation> limits = this.mDisplayManagerInternal.getRefreshRateLimitations(displayId);
                        int i3 = 0;
                        while (true) {
                            if (limits == null || i3 >= limits.size()) {
                                break;
                            }
                            DisplayManagerInternal.RefreshRateLimitation limitation = limits.get(i3);
                            if (limitation.type != 1) {
                                i3++;
                            } else {
                                vote = Vote.forRefreshRates(limitation.range.min, limitation.range.max);
                                break;
                            }
                        }
                    }
                } else if (hbmMode == 2 && (i = this.mRefreshRateInHbmHdr) > 0) {
                    vote = Vote.forRefreshRates(i, i);
                } else {
                    Slog.w(DisplayModeDirector.TAG, "Unexpected HBM mode " + hbmMode + " for display ID " + displayId);
                }
            }
            this.mBallotBox.vote(displayId, 2, vote);
        }

        void dumpLocked(PrintWriter pw) {
            pw.println("   HbmObserver");
            pw.println("     mHbmMode: " + this.mHbmMode);
            pw.println("     mHbmActive: " + this.mHbmActive);
            pw.println("     mRefreshRateInHbmSunlight: " + this.mRefreshRateInHbmSunlight);
            pw.println("     mRefreshRateInHbmHdr: " + this.mRefreshRateInHbmHdr);
        }
    }

    /* loaded from: classes.dex */
    private final class SkinThermalStatusObserver extends IThermalEventListener.Stub {
        private final BallotBox mBallotBox;
        private final Injector mInjector;
        private int mStatus = -1;

        SkinThermalStatusObserver(Injector injector, BallotBox ballotBox) {
            this.mInjector = injector;
            this.mBallotBox = ballotBox;
        }

        public void notifyThrottling(Temperature temp) {
            Vote vote;
            this.mStatus = temp.getStatus();
            if (DisplayModeDirector.this.mLoggingEnabled) {
                Slog.d(DisplayModeDirector.TAG, "New thermal throttling status , current thermal status = " + this.mStatus);
            }
            if (this.mStatus >= 4) {
                vote = Vote.forRefreshRates(0.0f, 60.0f);
            } else {
                vote = null;
            }
            this.mBallotBox.vote(-1, 13, vote);
        }

        public void observe() {
            IThermalService thermalService = this.mInjector.getThermalService();
            if (thermalService == null) {
                Slog.w(DisplayModeDirector.TAG, "Could not observe thermal status. Service not available");
                return;
            }
            try {
                thermalService.registerThermalEventListenerWithType(this, 3);
            } catch (RemoteException e) {
                Slog.e(DisplayModeDirector.TAG, "Failed to register thermal status listener", e);
            }
        }

        void dumpLocked(PrintWriter writer) {
            writer.println("  SkinThermalStatusObserver:");
            writer.println("    mStatus: " + this.mStatus);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DeviceConfigDisplaySettings implements DeviceConfig.OnPropertiesChangedListener {
        public DeviceConfigDisplaySettings() {
        }

        public void startListening() {
            DisplayModeDirector.this.mDeviceConfig.addOnPropertiesChangedListener("display_manager", BackgroundThread.getExecutor(), this);
        }

        public int[] getLowDisplayBrightnessThresholds() {
            return getIntArrayProperty("peak_refresh_rate_brightness_thresholds");
        }

        public int[] getLowAmbientBrightnessThresholds() {
            return getIntArrayProperty("peak_refresh_rate_ambient_thresholds");
        }

        public int getRefreshRateInLowZone() {
            int defaultRefreshRateInZone = DisplayModeDirector.this.mContext.getResources().getInteger(17694799);
            int refreshRate = DisplayModeDirector.this.mDeviceConfig.getInt("display_manager", "refresh_rate_in_zone", defaultRefreshRateInZone);
            return refreshRate;
        }

        public int[] getHighDisplayBrightnessThresholds() {
            return getIntArrayProperty("fixed_refresh_rate_high_display_brightness_thresholds");
        }

        public int[] getHighAmbientBrightnessThresholds() {
            return getIntArrayProperty("fixed_refresh_rate_high_ambient_brightness_thresholds");
        }

        public int getRefreshRateInHighZone() {
            int defaultRefreshRateInZone = DisplayModeDirector.this.mContext.getResources().getInteger(17694837);
            int refreshRate = DisplayModeDirector.this.mDeviceConfig.getInt("display_manager", "refresh_rate_in_high_zone", defaultRefreshRateInZone);
            return refreshRate;
        }

        public int getRefreshRateInHbmSunlight() {
            int defaultRefreshRateInHbmSunlight = DisplayModeDirector.this.mContext.getResources().getInteger(17694798);
            int refreshRate = DisplayModeDirector.this.mDeviceConfig.getInt("display_manager", "refresh_rate_in_hbm_sunlight", defaultRefreshRateInHbmSunlight);
            return refreshRate;
        }

        public int getRefreshRateInHbmHdr() {
            int defaultRefreshRateInHbmHdr = DisplayModeDirector.this.mContext.getResources().getInteger(17694797);
            int refreshRate = DisplayModeDirector.this.mDeviceConfig.getInt("display_manager", "refresh_rate_in_hbm_hdr", defaultRefreshRateInHbmHdr);
            return refreshRate;
        }

        public Float getDefaultPeakRefreshRate() {
            float defaultPeakRefreshRate = DisplayModeDirector.this.mDeviceConfig.getFloat("display_manager", "peak_refresh_rate_default", -1.0f);
            if (defaultPeakRefreshRate == -1.0f) {
                return null;
            }
            return Float.valueOf(defaultPeakRefreshRate);
        }

        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            Float defaultPeakRefreshRate = getDefaultPeakRefreshRate();
            DisplayModeDirector.this.mHandler.obtainMessage(3, defaultPeakRefreshRate).sendToTarget();
            int[] lowDisplayBrightnessThresholds = getLowDisplayBrightnessThresholds();
            int[] lowAmbientBrightnessThresholds = getLowAmbientBrightnessThresholds();
            int refreshRateInLowZone = getRefreshRateInLowZone();
            DisplayModeDirector.this.mHandler.obtainMessage(2, new Pair(lowDisplayBrightnessThresholds, lowAmbientBrightnessThresholds)).sendToTarget();
            DisplayModeDirector.this.mHandler.obtainMessage(4, refreshRateInLowZone, 0).sendToTarget();
            int[] highDisplayBrightnessThresholds = getHighDisplayBrightnessThresholds();
            int[] highAmbientBrightnessThresholds = getHighAmbientBrightnessThresholds();
            int refreshRateInHighZone = getRefreshRateInHighZone();
            DisplayModeDirector.this.mHandler.obtainMessage(6, new Pair(highDisplayBrightnessThresholds, highAmbientBrightnessThresholds)).sendToTarget();
            DisplayModeDirector.this.mHandler.obtainMessage(5, refreshRateInHighZone, 0).sendToTarget();
            int refreshRateInHbmSunlight = getRefreshRateInHbmSunlight();
            DisplayModeDirector.this.mHandler.obtainMessage(7, refreshRateInHbmSunlight, 0).sendToTarget();
            int refreshRateInHbmHdr = getRefreshRateInHbmHdr();
            DisplayModeDirector.this.mHandler.obtainMessage(8, refreshRateInHbmHdr, 0).sendToTarget();
        }

        private int[] getIntArrayProperty(String prop) {
            String strArray = DisplayModeDirector.this.mDeviceConfig.getString("display_manager", prop, (String) null);
            if (strArray != null) {
                return parseIntArray(strArray);
            }
            return null;
        }

        private int[] parseIntArray(String strArray) {
            String[] items = strArray.split(",");
            int[] array = new int[items.length];
            for (int i = 0; i < array.length; i++) {
                try {
                    array[i] = Integer.parseInt(items[i]);
                } catch (NumberFormatException e) {
                    Slog.e(DisplayModeDirector.TAG, "Incorrect format for array: '" + strArray + "'", e);
                    return null;
                }
            }
            return array;
        }
    }

    /* loaded from: classes.dex */
    static class RealInjector implements Injector {
        private final Context mContext;
        private DisplayManager mDisplayManager;

        RealInjector(Context context) {
            this.mContext = context;
        }

        @Override // com.android.server.display.DisplayModeDirector.Injector
        public DeviceConfigInterface getDeviceConfig() {
            return DeviceConfigInterface.REAL;
        }

        @Override // com.android.server.display.DisplayModeDirector.Injector
        public void registerPeakRefreshRateObserver(ContentResolver cr, ContentObserver observer) {
            cr.registerContentObserver(PEAK_REFRESH_RATE_URI, false, observer, 0);
        }

        @Override // com.android.server.display.DisplayModeDirector.Injector
        public void registerDisplayListener(DisplayManager.DisplayListener listener, Handler handler, long flags) {
            getDisplayManager().registerDisplayListener(listener, handler, flags);
        }

        @Override // com.android.server.display.DisplayModeDirector.Injector
        public BrightnessInfo getBrightnessInfo(int displayId) {
            Display display = getDisplayManager().getDisplay(displayId);
            if (display != null) {
                return display.getBrightnessInfo();
            }
            return null;
        }

        @Override // com.android.server.display.DisplayModeDirector.Injector
        public boolean isDozeState(Display d) {
            if (d == null) {
                return false;
            }
            return Display.isDozeState(d.getState());
        }

        @Override // com.android.server.display.DisplayModeDirector.Injector
        public IThermalService getThermalService() {
            return IThermalService.Stub.asInterface(ServiceManager.getService("thermalservice"));
        }

        private DisplayManager getDisplayManager() {
            if (this.mDisplayManager == null) {
                this.mDisplayManager = (DisplayManager) this.mContext.getSystemService(DisplayManager.class);
            }
            return this.mDisplayManager;
        }
    }
}
