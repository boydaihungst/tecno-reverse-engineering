package com.android.server.wm;

import android.hardware.display.DisplayManagerInternal;
import android.os.Bundle;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.wm.WmsExt;
import com.transsion.hubcore.server.wm.ITranDisplayPolicy;
import com.transsion.hubcore.server.wm.ITranRefreshRatePolicy;
import com.transsion.hubcore.server.wm.ITranRefreshRatePolicyProxy;
import com.transsion.hubcore.server.wm.tranrefreshrate.TranRefreshRatePolicy;
import com.transsion.hubcore.server.wm.tranrefreshrate.TranRefreshRatePolicy120;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class RefreshRatePolicy {
    static final int LAYER_PRIORITY_FOCUSED_WITHOUT_MODE = 1;
    static final int LAYER_PRIORITY_FOCUSED_WITH_MODE = 0;
    static final int LAYER_PRIORITY_NOT_FOCUSED_WITH_MODE = 2;
    static final int LAYER_PRIORITY_UNSET = -1;
    private static final String MSYNC_LOG_TAG = "MSyncRefreshRatePolicy";
    private static final float OFFSET_FPS = 2000.0f;
    private final HighRefreshRateDenylist mHighRefreshRateDenylist;
    private final Display.Mode mLowRefreshRateMode;
    private float mMaxSupportedRefreshRate;
    private float mMinSupportedRefreshRate;
    private TranRefreshRatePolicy mTranRefreshRatePolicy;
    private TranRefreshRatePolicy120 mTranRefreshRatePolicy120;
    private TranRefreshRatePolicyProxyImpl mTranRefreshRatePolicyProxyImpl;
    private final WindowManagerService mWmService;
    private final PackageRefreshRate mNonHighRefreshRatePackages = new PackageRefreshRate();
    private boolean mIsMax90hz = false;
    private boolean mIsMax120hz = false;
    private final WmsExt mWmsExt = MtkSystemServiceFactory.getInstance().makeWmsExt();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class PackageRefreshRate {
        private final HashMap<String, DisplayManagerInternal.RefreshRateRange> mPackages = new HashMap<>();

        PackageRefreshRate() {
        }

        public void add(String s, float minRefreshRate, float maxRefreshRate) {
            float minSupportedRefreshRate = Math.max(RefreshRatePolicy.this.mMinSupportedRefreshRate, minRefreshRate);
            float maxSupportedRefreshRate = Math.min(RefreshRatePolicy.this.mMaxSupportedRefreshRate, maxRefreshRate);
            this.mPackages.put(s, new DisplayManagerInternal.RefreshRateRange(minSupportedRefreshRate, maxSupportedRefreshRate));
        }

        public DisplayManagerInternal.RefreshRateRange get(String s) {
            return this.mPackages.get(s);
        }

        public void remove(String s) {
            this.mPackages.remove(s);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RefreshRatePolicy(WindowManagerService wmService, DisplayInfo displayInfo, HighRefreshRateDenylist denylist) {
        this.mLowRefreshRateMode = findLowRefreshRateMode(displayInfo);
        this.mHighRefreshRateDenylist = denylist;
        this.mWmService = wmService;
        initRefreshRatePolicy(displayInfo, this);
    }

    private Display.Mode findLowRefreshRateMode(DisplayInfo displayInfo) {
        Display.Mode mode = displayInfo.getDefaultMode();
        float[] refreshRates = displayInfo.getDefaultRefreshRates();
        float bestRefreshRate = mode.getRefreshRate();
        this.mMinSupportedRefreshRate = bestRefreshRate;
        this.mMaxSupportedRefreshRate = bestRefreshRate;
        for (int i = refreshRates.length - 1; i >= 0; i--) {
            this.mMinSupportedRefreshRate = Math.min(this.mMinSupportedRefreshRate, refreshRates[i]);
            this.mMaxSupportedRefreshRate = Math.max(this.mMaxSupportedRefreshRate, refreshRates[i]);
            if (refreshRates[i] >= 60.0f && refreshRates[i] < bestRefreshRate) {
                bestRefreshRate = refreshRates[i];
            }
        }
        return displayInfo.findDefaultModeByRefreshRate(bestRefreshRate);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addRefreshRateRangeForPackage(String packageName, float minRefreshRate, float maxRefreshRate) {
        this.mNonHighRefreshRatePackages.add(packageName, minRefreshRate, maxRefreshRate);
        this.mWmService.requestTraversal();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeRefreshRateRangeForPackage(String packageName) {
        this.mNonHighRefreshRatePackages.remove(packageName);
        this.mWmService.requestTraversal();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPreferredModeId(WindowState w) {
        if (this.mIsMax90hz) {
            return this.mTranRefreshRatePolicy.getPreferredModeId(w, this.mTranRefreshRatePolicyProxyImpl);
        }
        if (this.mIsMax120hz) {
            return this.mTranRefreshRatePolicy120.getPreferredModeId(w, this.mTranRefreshRatePolicyProxyImpl);
        }
        return getPreferredModeIdInternel(w);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int calculatePriority(WindowState w) {
        boolean isFocused = w.isFocused();
        int preferredModeId = getPreferredModeId(w);
        if (!isFocused && preferredModeId > 0) {
            return 2;
        }
        if (isFocused && preferredModeId == 0) {
            return 1;
        }
        if (isFocused && preferredModeId > 0) {
            return 0;
        }
        return -1;
    }

    float getMSyncPreferredRefreshRate(WindowState w) {
        DisplayInfo info;
        Display.Mode[] modeArr;
        if (w.isAnimating(3)) {
            if (this.mWmService.isMsyncLogOn) {
                Slog.i(MSYNC_LOG_TAG, "Animation Running");
            }
            return 0.0f;
        }
        String packageName = w.getOwningPackage();
        String activityName = "";
        if (w.mActivityRecord != null) {
            activityName = w.mActivityRecord.getName();
        }
        int action = w.mAttrs.mMSyncScenarioAction;
        int type = w.mAttrs.mMSyncScenarioType;
        boolean isIME = w.mIsImWindow;
        if (this.mWmService.isMsyncLogOn && isIME) {
            Slog.i(MSYNC_LOG_TAG, "Window is IME , Action:" + action + ", Type:" + type);
        }
        if (action != 0 && type != 0) {
            WindowState imeInputTarget = w.getImeInputTarget();
            if (isIME) {
                if (imeInputTarget == null) {
                    w.mAttrs.mMSyncScenarioAction = 0;
                    w.mAttrs.mMSyncScenarioType = 0;
                    return 0.0f;
                }
                WindowState imeParentWindowState = imeInputTarget.getWindowState();
                ActivityRecord mActivityRecord = imeParentWindowState == null ? null : imeParentWindowState.mActivityRecord;
                if (mActivityRecord != null) {
                    activityName = mActivityRecord.getName();
                    packageName = imeParentWindowState.getOwningPackage();
                }
            }
            float refreshRate = this.mWmsExt.getSpecialRefreshRate(type, action, packageName, activityName);
            if (refreshRate != 0.0f) {
                if (action == 2) {
                    w.mAttrs.mMSyncScenarioAction = 0;
                    w.mAttrs.mMSyncScenarioType = 0;
                }
                if (this.mWmService.isMsyncLogOn) {
                    Slog.i(MSYNC_LOG_TAG, "SpecialRefreshRate:" + refreshRate + ", ActivityName: " + activityName + ", packageName: " + packageName);
                }
                return refreshRate;
            }
        }
        if (w.mActivityRecord != null && activityName != null) {
            float refreshRate2 = this.mWmsExt.getPreferredRefreshRate(packageName, activityName);
            if (refreshRate2 != 0.0f) {
                if (this.mWmService.isMsyncLogOn) {
                    Slog.i(MSYNC_LOG_TAG, "Matching Information: " + activityName + " ,RefreshRate: " + refreshRate2);
                }
                return OFFSET_FPS + refreshRate2;
            }
        }
        int preferredModeId = w.mAttrs.preferredDisplayModeId;
        if (preferredModeId > 0 && (info = w.getDisplayInfo()) != null) {
            for (Display.Mode mode : info.supportedModes) {
                if (preferredModeId == mode.getModeId()) {
                    if (this.mWmService.isMsyncLogOn) {
                        Slog.i(MSYNC_LOG_TAG, "AppRequest, RefreshRate： " + mode.getRefreshRate() + ", ModeId： " + mode.getModeId());
                    }
                    return mode.getRefreshRate() + OFFSET_FPS;
                }
            }
        }
        if (w.mAttrs.preferredRefreshRate > 0.0f) {
            return w.mAttrs.preferredRefreshRate + OFFSET_FPS;
        }
        float defaultFPS = this.mWmsExt.getGlobalFPS() + OFFSET_FPS;
        if (defaultFPS == OFFSET_FPS) {
            Slog.i(MSYNC_LOG_TAG, "getGlobalFPS: 0");
            return 0.0f;
        }
        return defaultFPS;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getPreferredRefreshRate(WindowState w) {
        DisplayInfo info;
        Display.Mode[] modeArr;
        if (ITranRefreshRatePolicy.Instance().isHookRefreshRatePolicy()) {
            return ITranRefreshRatePolicy.Instance().getPreferredRefreshRate(w);
        }
        if (w.isAnimating(3)) {
            return 0.0f;
        }
        int preferredModeId = w.mAttrs.preferredDisplayModeId;
        if (preferredModeId > 0 && (info = w.getDisplayInfo()) != null) {
            for (Display.Mode mode : info.supportedModes) {
                if (preferredModeId == mode.getModeId()) {
                    return mode.getRefreshRate();
                }
            }
        }
        if (w.mAttrs.preferredRefreshRate > 0.0f) {
            return w.mAttrs.preferredRefreshRate;
        }
        String packageName = w.getOwningPackage();
        if (this.mWmService.isHighRefreshBlackListOn && this.mHighRefreshRateDenylist.isDenylisted(packageName)) {
            Slog.i("RefreshRatePolicy", "low refresh rate:" + packageName);
            return this.mLowRefreshRateMode.getRefreshRate();
        }
        return 0.0f;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getPreferredMinRefreshRate(WindowState w) {
        if (this.mIsMax90hz) {
            return this.mTranRefreshRatePolicy.getPreferredMinRefreshRate(w, this.mTranRefreshRatePolicyProxyImpl);
        }
        if (this.mIsMax120hz) {
            return this.mTranRefreshRatePolicy120.getPreferredMinRefreshRate(w, this.mTranRefreshRatePolicyProxyImpl);
        }
        if (w.isAnimating(3)) {
            return 0.0f;
        }
        if (w.mAttrs.preferredMinDisplayRefreshRate > 0.0f) {
            return w.mAttrs.preferredMinDisplayRefreshRate;
        }
        String packageName = w.getOwningPackage();
        DisplayManagerInternal.RefreshRateRange range = this.mNonHighRefreshRatePackages.get(packageName);
        if (range != null) {
            return range.min;
        }
        return 0.0f;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getPreferredMaxRefreshRate(WindowState w) {
        if (this.mIsMax90hz) {
            return this.mTranRefreshRatePolicy.getPreferredMaxRefreshRate(w, this.mTranRefreshRatePolicyProxyImpl);
        }
        if (this.mIsMax120hz) {
            return this.mTranRefreshRatePolicy120.getPreferredMaxRefreshRate(w, this.mTranRefreshRatePolicyProxyImpl);
        }
        if (w.isAnimating(3)) {
            return 0.0f;
        }
        String packageName = w.getOwningPackage();
        if (this.mWmService.isMsyncOn) {
            float maxFPS = confirmMaxFPS(w, packageName);
            if (maxFPS != 0.0f) {
                return maxFPS;
            }
        }
        if (w.mAttrs.preferredMaxDisplayRefreshRate > 0.0f) {
            return w.mAttrs.preferredMaxDisplayRefreshRate;
        }
        DisplayManagerInternal.RefreshRateRange range = this.mNonHighRefreshRatePackages.get(packageName);
        if (range != null) {
            return range.max;
        }
        return 0.0f;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateRefreshRateForScene(Bundle b) {
        if (this.mIsMax90hz) {
            this.mTranRefreshRatePolicy.hookUpdateRefreshRateForScene(b);
        } else if (this.mIsMax120hz) {
            this.mTranRefreshRatePolicy120.hookUpdateRefreshRateForScene(b);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateRefreshRateForVideoScene(int videoState, int videoFps, int videoSessionId) {
        if (this.mIsMax90hz) {
            this.mTranRefreshRatePolicy.hookupdateRefreshRateForVideoScene(videoState, videoFps, videoSessionId);
        } else if (this.mIsMax120hz) {
            this.mTranRefreshRatePolicy120.hookupdateRefreshRateForVideoScene(videoState, videoFps, videoSessionId);
        }
    }

    private float confirmMaxFPS(WindowState w, String pkgName) {
        int action = w.mAttrs.mMSyncScenarioAction;
        int type = w.mAttrs.mMSyncScenarioType;
        float maxFPS = w.mMaxFPS;
        String packageName = pkgName;
        String mActivityName = "";
        if (w.mActivityRecord != null) {
            mActivityName = w.mActivityRecord.getName();
        }
        if (action != 0 && type != 0) {
            if (w.mIsImWindow) {
                WindowState imeInputTarget = w.getImeInputTarget();
                if (imeInputTarget == null) {
                    return 0.0f;
                }
                WindowState imeParentWindowState = imeInputTarget.getWindowState();
                ActivityRecord activityRecord = imeParentWindowState.mActivityRecord;
                mActivityName = activityRecord == null ? "" : activityRecord.getName();
                packageName = imeParentWindowState.getOwningPackage();
            }
            float maxFPS2 = this.mWmsExt.getMaxRefreshRate(type, action, packageName, mActivityName);
            if (this.mWmService.isMsyncLogOn) {
                Slog.d(MSYNC_LOG_TAG, "IME or Other setCeiling: " + maxFPS2 + " ,action: " + action + " ,type: " + type + ", ActivityName: " + mActivityName + ", packageName: " + packageName);
            }
            return maxFPS2;
        } else if (maxFPS != 0.0f) {
            if (this.mWmService.isMsyncLogOn) {
                Slog.d(MSYNC_LOG_TAG, mActivityName + " setCeiling: " + maxFPS + ", packageName: " + packageName);
            }
            return maxFPS;
        } else {
            return 0.0f;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getPreferredModeIdInternel(WindowState w) {
        if (w.isAnimating(3)) {
            return 0;
        }
        return w.mAttrs.preferredDisplayModeId;
    }

    private void initRefreshRatePolicy(DisplayInfo displayInfo, RefreshRatePolicy refreshRatePolicy) {
        this.mIsMax90hz = ITranDisplayPolicy.Instance().isMaxSupport90HZRefreshRate();
        boolean isMaxSupport120HZRefreshRate = ITranDisplayPolicy.Instance().isMaxSupport120HZRefreshRate();
        this.mIsMax120hz = isMaxSupport120HZRefreshRate;
        if (this.mIsMax90hz) {
            WindowManagerService windowManagerService = this.mWmService;
            this.mTranRefreshRatePolicy = new TranRefreshRatePolicy(windowManagerService, displayInfo, windowManagerService.mContext);
        } else if (isMaxSupport120HZRefreshRate) {
            WindowManagerService windowManagerService2 = this.mWmService;
            this.mTranRefreshRatePolicy120 = new TranRefreshRatePolicy120(windowManagerService2, displayInfo, windowManagerService2.mContext);
        }
        if (this.mIsMax120hz || this.mIsMax90hz) {
            this.mTranRefreshRatePolicyProxyImpl = new TranRefreshRatePolicyProxyImpl(refreshRatePolicy);
        }
    }

    /* loaded from: classes2.dex */
    public static class TranRefreshRatePolicyProxyImpl implements ITranRefreshRatePolicyProxy {
        RefreshRatePolicy mRefreshRatePolicy;

        /* JADX INFO: Access modifiers changed from: package-private */
        public TranRefreshRatePolicyProxyImpl(RefreshRatePolicy refreshRatePolicy) {
            this.mRefreshRatePolicy = refreshRatePolicy;
        }

        @Override // com.transsion.hubcore.server.wm.ITranRefreshRatePolicyProxy
        public int getPreferredModeId(WindowState w) {
            RefreshRatePolicy refreshRatePolicy = this.mRefreshRatePolicy;
            if (refreshRatePolicy == null) {
                return 0;
            }
            return refreshRatePolicy.getPreferredModeIdInternel(w);
        }

        @Override // com.transsion.hubcore.server.wm.ITranRefreshRatePolicyProxy
        public boolean inNonHighRefreshPackages(String pkg) {
            RefreshRatePolicy refreshRatePolicy = this.mRefreshRatePolicy;
            return (refreshRatePolicy == null || refreshRatePolicy.mNonHighRefreshRatePackages.get(pkg) == null) ? false : true;
        }

        @Override // com.transsion.hubcore.server.wm.ITranRefreshRatePolicyProxy
        public Set<String> getNonHighRefreshPackages() {
            RefreshRatePolicy refreshRatePolicy = this.mRefreshRatePolicy;
            if (refreshRatePolicy == null) {
                return new HashSet();
            }
            return refreshRatePolicy.mNonHighRefreshRatePackages.mPackages.keySet();
        }

        @Override // com.transsion.hubcore.server.wm.ITranRefreshRatePolicyProxy
        public boolean isAnimating(WindowState w) {
            return w.isAnimating();
        }

        @Override // com.transsion.hubcore.server.wm.ITranRefreshRatePolicyProxy
        public TranRefreshRatePolicy getTranRefreshRatePolicy() {
            RefreshRatePolicy refreshRatePolicy = this.mRefreshRatePolicy;
            if (refreshRatePolicy == null) {
                return null;
            }
            return refreshRatePolicy.mTranRefreshRatePolicy;
        }

        @Override // com.transsion.hubcore.server.wm.ITranRefreshRatePolicyProxy
        public TranRefreshRatePolicy120 getTranRefreshRatePolicy120() {
            RefreshRatePolicy refreshRatePolicy = this.mRefreshRatePolicy;
            if (refreshRatePolicy == null) {
                return null;
            }
            return refreshRatePolicy.mTranRefreshRatePolicy120;
        }

        @Override // com.transsion.hubcore.server.wm.ITranRefreshRatePolicyProxy
        public float getPreferredRefreshRate(WindowState w) {
            RefreshRatePolicy refreshRatePolicy = this.mRefreshRatePolicy;
            if (refreshRatePolicy == null) {
                return 0.0f;
            }
            return refreshRatePolicy.getPreferredRefreshRate(w);
        }

        @Override // com.transsion.hubcore.server.wm.ITranRefreshRatePolicyProxy
        public DisplayManagerInternal.RefreshRateRange getRefreshRateRange(String packageName) {
            RefreshRatePolicy refreshRatePolicy = this.mRefreshRatePolicy;
            if (refreshRatePolicy == null) {
                return new DisplayManagerInternal.RefreshRateRange();
            }
            return refreshRatePolicy.mNonHighRefreshRatePackages.get(packageName);
        }
    }
}
