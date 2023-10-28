package com.transsion.hubcore.server.wm.tranrefreshrate;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowState;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.server.wm.ITranRefreshRatePolicyProxy;
import com.transsion.hubcore.server.wm.ITranWindowState;
import com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy;
import com.transsion.hubsdk.trancare.cloudengine.ITranCloudEngineCallback;
import com.transsion.hubsdk.trancare.trancare.TranTrancareManager;
import java.io.PrintWriter;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes2.dex */
public class TranRefreshRatePolicy extends TranBaseRefreshRatePolicy {
    private static final String TAG = TranRefreshRatePolicy.class.getSimpleName();
    private TranRefreshRateConfig mTranRefreshRate90Config;

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean activityStateSceneCome(Bundle bundle) {
        return super.activityStateSceneCome(bundle);
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ void dump(PrintWriter printWriter) {
        super.dump(printWriter);
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ float getCurSettingModeHighRefreshRate() {
        return super.getCurSettingModeHighRefreshRate();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ int getCurSettingModeHighRefreshRateId() {
        return super.getCurSettingModeHighRefreshRateId();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ float getPreferredMaxRefreshRate(WindowState windowState, ITranRefreshRatePolicyProxy iTranRefreshRatePolicyProxy) {
        return super.getPreferredMaxRefreshRate(windowState, iTranRefreshRatePolicyProxy);
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ float getPreferredMinRefreshRate(WindowState windowState, ITranRefreshRatePolicyProxy iTranRefreshRatePolicyProxy) {
        return super.getPreferredMinRefreshRate(windowState, iTranRefreshRatePolicyProxy);
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ int getVideoRefreshRateId(boolean z) {
        return super.getVideoRefreshRateId(z);
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ void hookUpdateRefreshRateForScene(Bundle bundle) {
        super.hookUpdateRefreshRateForScene(bundle);
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ void hookupdateRefreshRateForVideoScene(int i, int i2, int i3) {
        super.hookupdateRefreshRateForVideoScene(i, i2, i3);
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean inNativeVideoScene(WindowState windowState, String str) {
        return super.inNativeVideoScene(windowState, str);
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean inNaviAppPackages(String str) {
        return super.inNaviAppPackages(str);
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean inVideoScene(WindowState windowState, String str) {
        return super.inVideoScene(windowState, str);
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean inVoiceCallScene(WindowState windowState, String str, boolean z) {
        return super.inVoiceCallScene(windowState, str, z);
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean inputMethodScene() {
        return super.inputMethodScene();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean isBigScreen() {
        return super.isBigScreen();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean isInDisplayStateChangeFromOffToOn() {
        return super.isInDisplayStateChangeFromOffToOn();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean isInOpticalDataAcquisitionState() {
        return super.isInOpticalDataAcquisitionState();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean isInScreenAnimationState() {
        return super.isInScreenAnimationState();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean isInScreenDimState() {
        return super.isInScreenDimState();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean isLTPOScreen() {
        return super.isLTPOScreen();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean isLowerThanBrightnessThreshold() {
        return super.isLowerThanBrightnessThreshold();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ boolean isSupportLessThan60hz() {
        return super.isSupportLessThan60hz();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ void registerPointerEventListener() {
        super.registerPointerEventListener();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ void setDebug(PrintWriter printWriter, boolean z) {
        super.setDebug(printWriter, z);
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ void start() {
        super.start();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ void unregisterPointerEventListener() {
        super.unregisterPointerEventListener();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public /* bridge */ /* synthetic */ void updateRefreshRateAfterBrightnessChange(boolean z, float f) {
        super.updateRefreshRateAfterBrightnessChange(z, f);
    }

    public TranRefreshRatePolicy(WindowManagerService wmService, DisplayInfo displayInfo, Context context) {
        super(wmService, displayInfo, context);
        this.mTranRefreshRate90Config = null;
        initRefreshRate(displayInfo);
        this.mSettingsObserver = RefreshRateSettingsObserver.getInstance(context, this.mWms, this.mInitMinRefreshRate);
        TranRefreshRateConfig tranRefreshRateConfig = TranRefreshRateConfig.getInstance();
        this.mTranRefreshRate90Config = tranRefreshRateConfig;
        tranRefreshRateConfig.loadTranRefreshRateConfig();
        if (!this.mTranRefreshRate90Config.isLoadCommonCfgSuccess()) {
            Slog.e(TAG, "load common cfg failed.");
        } else {
            Slog.d(TAG, "load common cfg succeed.");
        }
        Slog.d(TAG, getFeatureInfo());
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    protected void initRefreshRate(DisplayInfo displayInfo) {
        Display.Mode displayMode = displayInfo.getDefaultMode();
        float[] refreshRates = displayInfo.getDefaultRefreshRates();
        float bestRefreshRate = displayMode.getRefreshRate();
        float highRefreshRate = 90.0f;
        int i = refreshRates.length;
        while (true) {
            i--;
            if (i < 0) {
                break;
            }
            if (refreshRates[i] >= 60.0f && refreshRates[i] < bestRefreshRate) {
                bestRefreshRate = refreshRates[i];
            }
            if (refreshRates[i] > 60.0f && refreshRates[i] < highRefreshRate) {
                highRefreshRate = refreshRates[i];
            }
        }
        Display.Mode lowMode = displayInfo.findDefaultModeByRefreshRate(bestRefreshRate);
        if (lowMode != null) {
            this.mLowRefreshRateId = lowMode.getModeId();
        } else {
            this.mLowRefreshRateId = 0;
        }
        Display.Mode highMode = displayInfo.findDefaultModeByRefreshRate(highRefreshRate);
        if (highMode != null) {
            this.mHighRefreshRateId = highMode.getModeId();
        } else {
            this.mHighRefreshRateId = 0;
        }
        this.mInitMinRefreshRate = 60.0f;
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    protected boolean isDefaultMode() {
        boolean z;
        synchronized (this.mTranRefreshRate90Config.getPolicyModeLock()) {
            z = !this.mTranRefreshRate90Config.isLoadCommonCfgSuccess() && -1 == this.mSettingsObserver.mRefreshRateMode;
        }
        return z;
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public int getPreferredModeId(WindowState w, ITranRefreshRatePolicyProxy proxy) {
        if (mDebug) {
            String str = TAG;
            logd(str, "RefreshRateMode = " + this.mSettingsObserver.mRefreshRateMode + " CurrentRefreshRate = " + this.mSettingsObserver.getRefreshRate() + " PeakRefreshRate = " + this.mSettingsObserver.mPeakRefreshRate);
            logd(str, "Window = " + w.toString() + " PreferredRefreshRate = " + w.getAttrs().preferredRefreshRate + " PreferredDisplayModeId = " + w.getAttrs().preferredDisplayModeId);
        }
        int curSettingModelHighRefreshRateId = getCurSettingModeHighRefreshRateId();
        if (isDefaultMode()) {
            int result = proxy.getPreferredModeId(w);
            logd(TAG, "Default Mode, return modeId = " + result);
            return result;
        } else if (this.mDisplayManagerInternal.shouldAlwaysRespectAppRequestedMode()) {
            logd(TAG, "AlwaysRespectAppRequest! ------------------");
            return proxy.getPreferredModeId(w);
        } else if (isLowPowerMode()) {
            int result2 = proxy.getPreferredModeId(w);
            logd(TAG, "LowPower Mode, return modeId = " + result2);
            return result2;
        } else {
            String pkgName = w.getOwningPackage();
            String str2 = TAG;
            logd(str2, "PkgName = " + pkgName);
            if (w.isAnimatingLw() && (is90HzMode() || isAutoMode())) {
                float curRefreshRate = this.mSettingsObserver.getRefreshRate();
                if (curRefreshRate <= 59.99f || curRefreshRate >= 60.01f) {
                    return 0;
                }
            }
            if (proxy.inNonHighRefreshPackages(pkgName)) {
                logd(str2, " App in tmpNonHighRefreshRatePackages , using camera, return modeId = " + this.mLowRefreshRateId);
                return this.mLowRefreshRateId;
            } else if (w.getAttrs().preferredRefreshRate != 0.0f || w.getAttrs().preferredDisplayModeId != 0) {
                return proxy.getPreferredModeId(w);
            } else {
                WindowState.TranWindowStateProxy tranWindowStateProxy = new WindowState.TranWindowStateProxy(w);
                String lastTitle = tranWindowStateProxy.getLastTitle();
                if (lastTitle != null && ((lastTitle.equals("TranssionFingerSensorWindow") || lastTitle.equals("Sys2044:dream")) && w.isVisible())) {
                    logd(str2, "[90Hz/Auto mode] Window is TranssionFingerSensorWindow or Sys2044:dream, return modeId = " + this.mHighRefreshRateId);
                    return this.mHighRefreshRateId;
                } else if (this.mSettingsObserver.isScreenRecordRunning()) {
                    logd(str2, "Screen Record Running , return modeId = " + this.mLowRefreshRateId);
                    return this.mLowRefreshRateId;
                } else if (is60HzMode()) {
                    logd(str2, "[60Hz Mode] return modeId " + this.mLowRefreshRateId);
                    return this.mLowRefreshRateId;
                } else if (w.isInMultiWindow()) {
                    logd(str2, "Tran MultiWindow , Use modeId = 0");
                    return 0;
                } else if (w.getAttrs().type == 2040) {
                    logd(str2, "[90Hz/Auto mode]  TYPE_NOTIFICATION_SHADE return modeId = " + curSettingModelHighRefreshRateId);
                    return curSettingModelHighRefreshRateId;
                } else if (w.getAttrs().type >= 2000 && w.getAttrs().type <= 2999) {
                    logd(str2, "[90Hz/Auto mode]  system window  return modeId = 0");
                    return 0;
                } else if (is90HzMode()) {
                    synchronized (this.mTranRefreshRate90Config.getHighRefreshRateLock()) {
                        if (this.mTranRefreshRate90Config.projectUseWhiteList()) {
                            if (this.mTranRefreshRate90Config.getProjectList().contains(pkgName)) {
                                logd(str2, "[90Hz Mode] App in highRefreshRateWhiteList. return modeId = 0");
                                return 0;
                            }
                            logd(str2, "[90Hz Mode] App out of highRefreshRateWhiteList. return modeId = " + this.mLowRefreshRateId);
                            return this.mLowRefreshRateId;
                        } else if (this.mTranRefreshRate90Config.getAccumulatedHighRefreshRateBlackList().contains(pkgName)) {
                            logd(str2, "[90Hz Mode] App in highRefreshRateBlackList. return modeId = " + this.mLowRefreshRateId);
                            return this.mLowRefreshRateId;
                        } else {
                            if (ITranGriffinFeature.Instance().isGriffinSupport() && TRAN_NOTIFICATION_REFRESH_SUPPORT) {
                                logd(str2, "[90Hz Mode]  mResumedPkgName " + this.mTranRefreshRate90Config.mResumedPkgName);
                                synchronized (this.mTranRefreshRate90Config.mPubgPkgSetLock) {
                                    if (this.mTranRefreshRate90Config.mPubgPkgSet.contains(this.mTranRefreshRate90Config.mResumedPkgName) && lastTitle != null && lastTitle.equals("NotificationShade") && w.isVisible()) {
                                        logd(str2, "[90Hz Mode] Pubg in Bg, Window is NotificationShade, use mLowRefreshRateId. return modeId = " + this.mLowRefreshRateId);
                                        return this.mLowRefreshRateId;
                                    }
                                }
                            }
                            logd(str2, "[90Hz Mode] return modeId = 0");
                            return 0;
                        }
                    }
                } else if (isAutoMode()) {
                    if (!this.mSettingsObserver.isUserSetupCompleted()) {
                        logd(str2, "Not UserSetupCompleted!");
                        return 0;
                    } else if (lastTitle != null && lastTitle.equals("ImmersiveModeConfirmation") && w.isVisible()) {
                        logd(str2, "[Auto mode] Window is ImmersiveModeConfirmation,return modelID = 0");
                        return 0;
                    } else if (ITranWindowState.Instance().isVisibleOnePixelWindow(w)) {
                        logd(str2, "[Auto Mode]  Window above resumed window in Z order just 1 * 1 pixel return modeID = 0");
                        return 0;
                    } else {
                        synchronized (this.mTranRefreshRate90Config.getPolicyModeLock()) {
                            if (this.mTranRefreshRate90Config.getAutoRefreshRateWhiteList().contains(pkgName)) {
                                logd(str2, "[Auto Mode]  App be allowed HighRefreshRate. return modeId = mHighRefreshRateId");
                                return this.mHighRefreshRateId;
                            }
                            if (!"com.android.systemui".equals(pkgName) && !"com.transsion.smartpanel".equals(pkgName) && !"com.transsion.screenrecord".equals(pkgName) && !"com.google.android.googlequicksearchbox".equals(pkgName)) {
                                logd(str2, "[Auto Mode]  return modeId = " + this.mLowRefreshRateId);
                                return this.mLowRefreshRateId;
                            }
                            logd(str2, "[Auto Mode]  App above resumed window in Z Order.  return modeId = 0");
                            return 0;
                        }
                    }
                } else {
                    Slog.e(str2, "[Null Mode] window pkg:" + pkgName + " why happened?");
                    return 0;
                }
            }
        }
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public String getFeatureInfo() {
        StringBuffer buffer = new StringBuffer();
        synchronized (this.mTranRefreshRate90Config.getPolicyModeLock()) {
            if (this.mTranRefreshRate90Config.isLoadCommonCfgSuccess()) {
                buffer.append("Auto Policy version:" + this.mTranRefreshRate90Config.getVersion() + ", major version:" + this.mTranRefreshRate90Config.getMajorVersion() + ", updatetime:" + this.mTranRefreshRate90Config.getUpdateTime() + "\n");
                buffer.append("ExistCustomizedConfig " + this.mTranRefreshRate90Config.mExistCustomizedConfig + "\n");
                buffer.append("mExistParseCfgException(common/project/cloud, please check.) " + this.mTranRefreshRate90Config.mExistParseCfgException + "\n");
                buffer.append("Auto Whitelist:\n");
                for (String pkg : this.mTranRefreshRate90Config.getAutoRefreshRateWhiteList()) {
                    buffer.append("    " + pkg + "\n");
                }
            } else {
                buffer.append("Auto Refresh Rate Policy: null");
            }
        }
        buffer.append("\n");
        return buffer.toString();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public String getCurrentState(ITranRefreshRatePolicyProxy proxy) {
        StringBuffer buffer = new StringBuffer();
        StringBuffer append = buffer.append("Curent refresh rate=" + this.mSettingsObserver.getRefreshRate() + "\n").append("User selected mode=" + this.mSettingsObserver.mRefreshRateMode + "\n").append("Settings peak refresh rate=" + this.mSettingsObserver.mPeakRefreshRate + "\n").append("LowRefreshRateId=" + this.mLowRefreshRateId + ", HighRefreshRateId=" + this.mHighRefreshRateId + "\n").append("LowPowerSaving=" + this.mSettingsObserver.mLowPowerMode + "\n").append("NeedRecover mode=" + this.mSettingsObserver.getNeedRecoverRefreshState() + "\n").append("CpuName is " + this.mTranRefreshRate90Config.mCpuName + "\n");
        StringBuilder append2 = new StringBuilder().append("PlatformAlias is ");
        TranRefreshRateConfig tranRefreshRateConfig = this.mTranRefreshRate90Config;
        append.append(append2.append(tranRefreshRateConfig.getPlatformAlias(tranRefreshRateConfig.mCpuName)).append("\n").toString()).append("ProjectName is " + this.mTranRefreshRate90Config.mProjectName + "\n").append("LoadCommonConfigSuccess is " + this.mTranRefreshRate90Config.isLoadCommonCfgSuccess() + "\n").append("ProjectPkgUsedMode is " + this.mTranRefreshRate90Config.mProjectPkgUsedMode + "\n").append("UserSetupCompleted is " + this.mSettingsObserver.isUserSetupCompleted() + "\n");
        synchronized (this.mTranRefreshRate90Config.getHighRefreshRateLock()) {
            if (!this.mTranRefreshRate90Config.projectUseWhiteList()) {
                buffer.append("AccumulatedHighRefreshRateBlackList:\n");
                for (String str : this.mTranRefreshRate90Config.mAccumulatedHighRefreshRateBlackList) {
                    buffer.append("    " + str + "\n");
                }
                buffer.append("HighRefreshRateBlackList:\n");
                for (String str2 : this.mTranRefreshRate90Config.mHighRefreshRateBlackList) {
                    buffer.append("    " + str2 + "\n");
                }
                buffer.append("PlatformHighRefreshRateBlackList:\n");
                for (String str3 : this.mTranRefreshRate90Config.mPlatformHighRefreshRateBlackList) {
                    buffer.append("    " + str3 + "\n");
                }
            }
            buffer.append("ProjectRefreshRateList:\n");
            for (String str4 : this.mTranRefreshRate90Config.mProjectRefreshRateList) {
                buffer.append("    " + str4 + "\n");
            }
        }
        buffer.append("\n");
        buffer.append("PubgPkgSet:\n");
        for (String str5 : this.mTranRefreshRate90Config.mPubgPkgSet) {
            buffer.append("    " + str5 + "\n");
        }
        buffer.append("\n");
        buffer.append("Temp NonHighRefreshRateBlackList:");
        for (String str6 : proxy.getNonHighRefreshPackages()) {
            buffer.append("    " + str6 + "\n");
        }
        buffer.append("\n");
        return buffer.toString();
    }

    /* loaded from: classes2.dex */
    private static final class RefreshRateSettingsObserver extends TranBaseRefreshRatePolicy.BaseRefreshRateSettingsObserver {
        private static final String TAG = "RefreshRateObserver-90";
        private static volatile RefreshRateSettingsObserver sDefault;

        RefreshRateSettingsObserver(Context context, WindowManagerService wm, Handler handler, float minRefreshRate) {
            super(context, wm, handler);
            this.mMinRefreshRate = minRefreshRate;
            observe();
        }

        public static synchronized RefreshRateSettingsObserver getInstance(Context context, WindowManagerService wm, float minRefreshRate) {
            RefreshRateSettingsObserver refreshRateSettingsObserver;
            synchronized (RefreshRateSettingsObserver.class) {
                if (sDefault == null) {
                    synchronized (RefreshRateSettingsObserver.class) {
                        if (sDefault == null) {
                            sDefault = new RefreshRateSettingsObserver(context, wm, new Handler(), minRefreshRate);
                        }
                    }
                }
                refreshRateSettingsObserver = sDefault;
            }
            return refreshRateSettingsObserver;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.BaseRefreshRateSettingsObserver
        public void observe() {
            if (-1.0f == this.mPeakRefreshRate && -2 == this.mRefreshRateMode) {
                if (TranBaseRefreshRatePolicy.TRAN_DEFAULT_AUTO_REFRESH_SUPPORT) {
                    Settings.System.putInt(this.mContext.getContentResolver(), "tran_refresh_mode", 10000);
                    Slog.d(TAG, "Init default user settings as auto mode");
                } else {
                    Settings.System.putInt(this.mContext.getContentResolver(), "tran_refresh_mode", 90);
                    Slog.d(TAG, "Init default user settings as refresh_rate=90, mode=1");
                }
            }
            super.observe();
        }

        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.BaseRefreshRateSettingsObserver
        protected int getRecoverRefreshRateMode() {
            return Settings.System.getIntForUser(this.mContext.getContentResolver(), "last_tran_refresh_mode_in_refresh_setting", TranBaseRefreshRatePolicy.TRAN_DEFAULT_AUTO_REFRESH_SUPPORT ? 10000 : 90, 0);
        }

        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.BaseRefreshRateSettingsObserver
        protected void updateRefreshRateModeSettingsInternel() {
            if (10000 == this.mRefreshRateMode || 90 == this.mRefreshRateMode) {
                Settings.System.putFloat(this.mContext.getContentResolver(), "peak_refresh_rate", 90.0f);
                Settings.System.putFloat(this.mContext.getContentResolver(), "min_refresh_rate", 60.0f);
            } else if (60 == this.mRefreshRateMode) {
                Settings.System.putFloat(this.mContext.getContentResolver(), "peak_refresh_rate", 90.0f);
                Settings.System.putFloat(this.mContext.getContentResolver(), "min_refresh_rate", 60.0f);
            }
        }

        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.BaseRefreshRateSettingsObserver
        public int checkRefreshChoose(int refreshChoose) {
            if (refreshChoose == 0) {
                return 60;
            }
            if (TranBaseRefreshRatePolicy.TRAN_COMPATIBILITY_DEFAULT_REFRESH_SUPPORT && refreshChoose == 1) {
                return 10000;
            }
            if (refreshChoose == 1) {
                return 90;
            }
            if (refreshChoose == 2) {
                return 10000;
            }
            return refreshChoose;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class TranRefreshRateConfig extends TranBaseRefreshRatePolicy.TranBaseRefreshRateConfig {
        private static final String CLOUD_TRANREFERSHRATE_90HZ_FILENAME = "/refresh_rate_config.json";
        private static final String CLOUD_TRANREFRESHRATE_90HZ_BLACKLIST_FILE_KEY = "1001000023";
        private static final String HIGH_REFRESH_RATE_90HZ_BLACK_LIST = "refresh_rate_blacklist_in_90hz_mode";
        private static volatile TranRefreshRateConfig sDefault;
        private String mProjectPkgUsedMode = "blacklist";

        TranRefreshRateConfig() {
            TranTrancareManager.regCloudEngineCallback(CLOUD_TRANREFRESHRATE_90HZ_BLACKLIST_FILE_KEY, "v2.1", new ITranCloudEngineCallback.Stub() { // from class: com.transsion.hubcore.server.wm.tranrefreshrate.TranRefreshRatePolicy.TranRefreshRateConfig.1
                public void onUpdate(String Key, boolean fileState, String config) {
                    TranBaseRefreshRatePolicy.logd(TranRefreshRatePolicy.TAG, "onUpdate: Key:" + Key + ",fileState:" + fileState + ",config:" + config);
                    TranRefreshRateConfig.this.loadCloudTranRefreshRateConfig();
                    TranTrancareManager.feedBack(Key, true);
                }
            });
        }

        public static synchronized TranRefreshRateConfig getInstance() {
            TranRefreshRateConfig tranRefreshRateConfig;
            synchronized (TranRefreshRateConfig.class) {
                if (sDefault == null) {
                    synchronized (TranRefreshRateConfig.class) {
                        if (sDefault == null) {
                            sDefault = new TranRefreshRateConfig();
                        }
                    }
                }
                tranRefreshRateConfig = sDefault;
            }
            return tranRefreshRateConfig;
        }

        public boolean projectUseWhiteList() {
            return "whitelist".equalsIgnoreCase(this.mProjectPkgUsedMode);
        }

        public Set<String> getProjectList() {
            return this.mProjectRefreshRateList;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.TranBaseRefreshRateConfig
        public String getPlatformAlias(String p) {
            char c;
            switch (p.hashCode()) {
                case -2011283547:
                    if (p.equals("MT6785")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -957193546:
                    if (p.equals("MT6769V/CU")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -957193541:
                    if (p.equals("MT6769V/CZ")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case -903629346:
                    if (p.equals("MT6785V/CC")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -903629345:
                    if (p.equals("MT6785V/CD")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                case 2:
                    return "G95";
                case 3:
                    return "G80";
                case 4:
                    return "G85";
                default:
                    return "unKnown";
            }
        }

        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.TranBaseRefreshRateConfig
        public void loadTranRefreshRateConfig() {
            String localJsonFilePath = Environment.getProductDirectory().getPath() + "/apm/config/refresh_rate_config.json";
            String localJsonStr = readFileString(localJsonFilePath);
            if (TextUtils.isEmpty(localJsonStr)) {
                this.mLoadCommonCfgSuccessed = false;
                Slog.d(TranRefreshRatePolicy.TAG, "load local Json failed");
                return;
            }
            Slog.d(TranRefreshRatePolicy.TAG, "load RefreshRateConfig from local successfully");
            updateTranRefreshRateConfig(localJsonStr, false);
            String customizedLocalJsonFilePath = Environment.getProductDirectory().getPath() + "/apm/config/project_refresh_rate_config.json";
            String customizedLocalJsonStr = readFileString(customizedLocalJsonFilePath);
            if (!TextUtils.isEmpty(customizedLocalJsonStr)) {
                this.mExistCustomizedConfig = true;
                updateTranRefreshRateConfig(customizedLocalJsonStr, true);
            } else {
                Slog.d(TranRefreshRatePolicy.TAG, "no exist customized config!");
            }
            loadCloudTranRefreshRateConfig();
        }

        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.TranBaseRefreshRateConfig
        protected void loadCloudTranRefreshRateConfig() {
            if (TranTrancareManager.getEnabled(CLOUD_TRANREFRESHRATE_90HZ_BLACKLIST_FILE_KEY, false)) {
                String cloudJsonFilePath = TranTrancareManager.getFilePath(CLOUD_TRANREFRESHRATE_90HZ_BLACKLIST_FILE_KEY);
                if (cloudJsonFilePath != null) {
                    cloudJsonFilePath = cloudJsonFilePath + CLOUD_TRANREFERSHRATE_90HZ_FILENAME;
                }
                String cloudJsonStr = cloudJsonFilePath != null ? readFileString(FilePathCheckUtil.checkFilePath(cloudJsonFilePath, TranBaseRefreshRatePolicy.mDebug)) : null;
                if (!TextUtils.isEmpty(cloudJsonStr)) {
                    updateTranRefreshRateConfig(cloudJsonStr, false);
                    Slog.d(TranRefreshRatePolicy.TAG, "load RefreshRateConfig from cloud");
                    return;
                }
                return;
            }
            Slog.d(TranRefreshRatePolicy.TAG, "CLOUD_TRANREFRESHRATE_90HZ_BLACKLIST_FILE_KEY is disabled");
        }

        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.TranBaseRefreshRateConfig
        protected void updateTranRefreshRateConfig(String jsonStr, boolean ignoreMinor) {
            JSONObject projectEntrys;
            JSONObject projetIndexs;
            try {
                JSONObject originJson = new JSONObject(jsonStr);
                int major_version = originJson.optInt("major_version");
                int version = originJson.optInt("version");
                int updateTime = originJson.optInt("update_time");
                synchronized (this.mPolicyModeLock) {
                    if (this.mConfigLastVersion != 0) {
                        if (major_version != this.mConfigLastMajorVersion) {
                            Slog.d(TranRefreshRatePolicy.TAG, "major Version invalid: currentMajorVersion = " + this.mConfigLastMajorVersion + ", updateMajorVersion = " + major_version);
                            return;
                        } else if (!ignoreMinor && version <= this.mConfigLastVersion) {
                            Slog.d(TranRefreshRatePolicy.TAG, "update Version invalid: current = " + this.mConfigLastVersion + ", updateVersion = " + version);
                            return;
                        }
                    }
                    this.mConfigLastMajorVersion = major_version;
                    this.mConfigLastVersion = version;
                    this.mUpdateTime = updateTime;
                    if (originJson.has("auto_refresh_rate_whitelist")) {
                        synchronized (this.mHighRefreshRateLock) {
                            this.mAutoRefreshRateWhiteList = jsonArray2Set(originJson.optJSONArray("auto_refresh_rate_whitelist"));
                        }
                    }
                    if (originJson.has("pubg_package_list")) {
                        synchronized (this.mPubgPkgSetLock) {
                            this.mPubgPkgSet = jsonArray2Set(originJson.optJSONArray("pubg_package_list"));
                        }
                    }
                    synchronized (this.mHighRefreshRateLock) {
                        this.mAccumulatedHighRefreshRateBlackList.clear();
                        if (originJson.has(HIGH_REFRESH_RATE_90HZ_BLACK_LIST)) {
                            this.mHighRefreshRateBlackList = jsonArray2Set(originJson.optJSONArray(HIGH_REFRESH_RATE_90HZ_BLACK_LIST));
                        }
                        this.mAccumulatedHighRefreshRateBlackList.addAll(this.mHighRefreshRateBlackList);
                        if (originJson.has("platform_pkg_set_entrys")) {
                            JSONObject platformJson = originJson.optJSONObject("platform_pkg_set_entrys");
                            String cpuAlias = getPlatformAlias(this.mCpuName);
                            if (platformJson != null && platformJson.has(cpuAlias)) {
                                this.mPlatformHighRefreshRateBlackList = jsonArray2Set(platformJson.optJSONArray(cpuAlias));
                            }
                        }
                        this.mAccumulatedHighRefreshRateBlackList.addAll(this.mPlatformHighRefreshRateBlackList);
                        String projectPkgSetName = "";
                        if (originJson.has("project_pkg_set_entry_index") && (projetIndexs = originJson.optJSONObject("project_pkg_set_entry_index")) != null && projetIndexs.has(this.mProjectName)) {
                            projectPkgSetName = projetIndexs.optString(this.mProjectName);
                        }
                        if (originJson.has("project_pkg_set_entrys") && (projectEntrys = originJson.optJSONObject("project_pkg_set_entrys")) != null && projectEntrys.has(projectPkgSetName)) {
                            JSONObject pkgEntry = projectEntrys.optJSONObject(projectPkgSetName);
                            if (pkgEntry != null && pkgEntry.has("used_mode")) {
                                this.mProjectPkgUsedMode = pkgEntry.optString("used_mode");
                            }
                            if (pkgEntry != null && pkgEntry.has("values")) {
                                this.mProjectRefreshRateList = jsonArray2Set(pkgEntry.optJSONArray("values"));
                            }
                        }
                        this.mAccumulatedHighRefreshRateBlackList.addAll(this.mProjectRefreshRateList);
                    }
                    this.mLoadCommonCfgSuccessed = true;
                }
            } catch (JSONException e) {
                this.mExistParseCfgException = true;
                e.printStackTrace();
            }
        }
    }
}
