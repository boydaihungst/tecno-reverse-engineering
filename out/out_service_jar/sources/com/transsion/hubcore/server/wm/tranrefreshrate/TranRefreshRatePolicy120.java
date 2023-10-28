package com.transsion.hubcore.server.wm.tranrefreshrate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
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
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes2.dex */
public class TranRefreshRatePolicy120 extends TranBaseRefreshRatePolicy {
    private static final String TAG = TranRefreshRatePolicy120.class.getSimpleName();
    private static final boolean TRAN_90HZ_REFRESH_RATE_NOT_SUPPORT;
    private static final boolean TRAN_FINGER_RINTER_HIGH_REFRESH_SUPPORT;
    private static final boolean TRAN_LOW_BATTERY_60HZ_REFRESH_RATE_SUPPORT;
    boolean mIsExist90Hz;

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

    static {
        TRAN_FINGER_RINTER_HIGH_REFRESH_SUPPORT = SystemProperties.getInt("ro.local_highlight_support", 0) == 0 && SystemProperties.getInt("ro.aod_nodoze_support", 0) == 0;
        TRAN_90HZ_REFRESH_RATE_NOT_SUPPORT = 1 == SystemProperties.getInt("ro.tran_90hz_refresh_rate.not_support", 0);
        TRAN_LOW_BATTERY_60HZ_REFRESH_RATE_SUPPORT = 1 == SystemProperties.getInt("ro.tran_low_battery_60hz_refresh_rate.support", 1);
    }

    public TranRefreshRatePolicy120(WindowManagerService wmService, DisplayInfo displayInfo, Context context) {
        super(wmService, displayInfo, context);
        this.mIsExist90Hz = false;
        initRefreshRate(displayInfo);
        this.mSettingsObserver = RefreshRateSettingsObserver120.getInstance(context, this.mWms, this.mInitMinRefreshRate);
        this.mTranRefreshRateConfig = TranRefreshRateConfig120.getInstance();
        this.mTranRefreshRateConfig.loadTranRefreshRateConfig();
        if (!this.mTranRefreshRateConfig.isLoadCommonCfgSuccess()) {
            Slog.e(TAG, "load common cfg failed.");
        } else {
            Slog.d(TAG, "load common cfg succeed.");
        }
        Slog.d(TAG, getFeatureInfo());
    }

    private int getPreferredModeIdInAutoMode(String pkgName, WindowState w) {
        if (!this.mSettingsObserver.isUserSetupCompleted()) {
            logd(TAG, "Not UserSetupCompleted!");
            return 0;
        }
        synchronized (this.mTranRefreshRateConfig.getPolicyModeLock()) {
            if (this.mTranRefreshRateConfig.getAutoRefreshRateWhiteList().contains(pkgName)) {
                logd(TAG, "[Auto Mode]" + pkgName + " in autoRefreshRateWhiteList, use mHighRefreshRateId. return modeId = " + this.mHighRefreshRateId);
                return 0;
            } else if (this.mTranRefreshRateConfig.getMinRefreshRate90HzInAutoModeList().contains(pkgName)) {
                logd(TAG, "[Auto Mode]" + pkgName + " in autoMode, use m90HzRefreshRateId. return modeId = " + this.m90HZRefreshRateId);
                return this.m90HZRefreshRateId;
            } else {
                logd(TAG, "[Auto Mode] return modeId = " + this.mLowRefreshRateId);
                return this.mLowRefreshRateId;
            }
        }
    }

    private boolean ignorePreferredRefreshRate(String packageName) {
        return "com.android.systemui".equals(packageName) || "com.transsion.smartpanel".equals(packageName) || "com.transsion.screenrecord".equals(packageName);
    }

    private void initInternalRefreshRate() {
        this.mAodRefreshRateId = this.mLowRefreshRateId;
        if (this.m10HzRefreshRateId != -1) {
            this.mInitMinRefreshRate = 10.0f;
            if (this.m30HzRefreshRateId == -1) {
                this.m30HzRefreshRateId = this.mLowRefreshRateId;
            }
            if (this.m45HzRefreshRateId == -1) {
                this.m45HzRefreshRateId = this.mLowRefreshRateId;
            }
        } else {
            this.mInitMinRefreshRate = 60.0f;
            if (this.m45HzRefreshRateId != -1) {
                this.mAodRefreshRateId = this.m45HzRefreshRateId;
            }
            if (this.m30HzRefreshRateId != -1) {
                this.mAodRefreshRateId = this.m30HzRefreshRateId;
            }
            this.m30HzRefreshRateId = this.mLowRefreshRateId;
            this.m45HzRefreshRateId = this.mLowRefreshRateId;
        }
        this.mInputMethodRefreshRateId = this.mLowRefreshRateId;
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    protected void initRefreshRate(DisplayInfo di) {
        DisplayInfo displayInfo = di;
        if (di == null && (DisplayManagerGlobal.getInstance() == null || (displayInfo = DisplayManagerGlobal.getInstance().getDisplayInfo(0)) == null)) {
            return;
        }
        this.mCurrentAppWidth = displayInfo.appWidth;
        this.mCurrentAppHeight = displayInfo.appHeight;
        Display.Mode displayMode = displayInfo.getDefaultMode();
        float[] refreshRates = displayInfo.getDefaultRefreshRates();
        float bestRefreshRate = displayMode.getRefreshRate();
        float highRefreshRate = bestRefreshRate;
        float middleRefreshRate = bestRefreshRate;
        this.m45HzRefreshRateId = -1;
        this.m30HzRefreshRateId = -1;
        this.m10HzRefreshRateId = -1;
        for (int i = refreshRates.length - 1; i >= 0; i--) {
            if (refreshRates[i] >= 59.99f && refreshRates[i] < 60.01f) {
                bestRefreshRate = refreshRates[i];
            }
            if (refreshRates[i] > highRefreshRate) {
                highRefreshRate = refreshRates[i];
            }
            if (refreshRates[i] > 89.99f && refreshRates[i] < 90.01f) {
                middleRefreshRate = refreshRates[i];
            }
            if (refreshRates[i] > 44.99f && refreshRates[i] < 45.01f) {
                float refreshRate = refreshRates[i];
                Display.Mode mode = displayInfo.findDefaultModeByRefreshRate(refreshRate);
                if (mode != null) {
                    this.m45HzRefreshRateId = mode.getModeId();
                }
            }
            float refreshRate2 = refreshRates[i];
            if (refreshRate2 > 29.99f && refreshRates[i] < 30.01f) {
                float refreshRate3 = refreshRates[i];
                Display.Mode mode2 = displayInfo.findDefaultModeByRefreshRate(refreshRate3);
                if (mode2 != null) {
                    this.m30HzRefreshRateId = mode2.getModeId();
                }
            }
            float refreshRate4 = refreshRates[i];
            if (refreshRate4 > 9.99f && refreshRates[i] < 10.01f) {
                float refreshRate5 = refreshRates[i];
                Display.Mode mode3 = displayInfo.findDefaultModeByRefreshRate(refreshRate5);
                if (mode3 != null) {
                    this.m10HzRefreshRateId = mode3.getModeId();
                }
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
        Display.Mode midMode = displayInfo.findDefaultModeByRefreshRate(middleRefreshRate);
        if (midMode != null) {
            this.m90HZRefreshRateId = midMode.getModeId();
        } else {
            this.m90HZRefreshRateId = this.mLowRefreshRateId;
        }
        initInternalRefreshRate();
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    protected boolean isDefaultMode() {
        boolean z;
        synchronized (this.mTranRefreshRateConfig.getPolicyModeLock()) {
            z = !this.mTranRefreshRateConfig.isLoadCommonCfgSuccess() && -1 == this.mSettingsObserver.mRefreshRateMode;
        }
        return z;
    }

    private boolean is120HzMode() {
        return 120 == this.mSettingsObserver.mRefreshRateMode;
    }

    private boolean isInFastSlowSlideScene(String pkgName) {
        if (this.mSlideSceneState != 0 && TSSI_TRAN_FASTSLOWSLIDE_SUPPORT && !TextUtils.isEmpty(this.mScenePkgName) && this.mScenePkgName.equals(pkgName) && !this.mTranRefreshRateConfig.getFastSlowSlideBlackList().contains(pkgName)) {
            logd(TAG, "[Auto Mode] slide scene, fast slide switch to HighMode  return modeId 0");
            return true;
        }
        return false;
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public int getPreferredModeId(WindowState w, ITranRefreshRatePolicyProxy proxy) {
        if (mDebug) {
            String str = TAG;
            logd(str, "RefreshRateMode = " + this.mSettingsObserver.mRefreshRateMode + " CurrentRefreshRate = " + this.mSettingsObserver.getRefreshRate() + " PeakRefreshRate = " + this.mSettingsObserver.mPeakRefreshRate);
            logd(str, "Window = " + w.toString() + " PreferredRefreshRate = " + w.getAttrs().preferredRefreshRate + " PreferredDisplayModeId = " + w.getAttrs().preferredDisplayModeId);
        }
        if (isLTPOScreen()) {
            initRefreshRate(null);
            this.mAodRefreshRateId = getCurSettingModeHighRefreshRateId();
        }
        int curSettingModelHighRefreshRateId = getCurSettingModeHighRefreshRateId();
        if (isDefaultMode()) {
            int result = proxy.getPreferredModeId(w);
            logd(TAG, "Default Mode, return modeId = " + result);
            return result;
        } else if (this.mDisplayManagerInternal.shouldAlwaysRespectAppRequestedMode()) {
            logd(TAG, "AlwaysRespectAppRequest! ------------------");
            return proxy.getPreferredModeId(w);
        } else {
            String pkgName = w.getOwningPackage();
            String str2 = TAG;
            logd(str2, "PkgName = " + pkgName);
            WindowState.TranWindowStateProxy tranWindowStateProxy = new WindowState.TranWindowStateProxy(w);
            String lastTitle = tranWindowStateProxy.getLastTitle();
            if (lastTitle != null && lastTitle.equals("Sys2044:dream") && w.isVisible()) {
                logd(str2, "[120Hz/Auto mode] Window is aod Sys2044:dream, return modeId = " + this.mAodRefreshRateId);
                return this.mAodRefreshRateId;
            }
            boolean z = TRAN_FINGER_RINTER_HIGH_REFRESH_SUPPORT;
            if (z) {
                if (lastTitle != null && lastTitle.equals("TranssionFingerSensorWindow") && w.isVisible()) {
                    logd(str2, "Window TranssionFingerSensorWindow or Sys2044:dream, always return modeId = " + curSettingModelHighRefreshRateId);
                    return curSettingModelHighRefreshRateId;
                } else if (lastTitle != null && lastTitle.equals("com.android.settings/com.transsion.fpmanager.TranAddFpActivity") && w.isVisible()) {
                    logd(str2, "Window com.android.settings/com.transsion.fpmanager.TranAddFpActivity, always return modeId = " + curSettingModelHighRefreshRateId);
                    return curSettingModelHighRefreshRateId;
                } else if ("com.goodix.fingerprint.producttest".equals(pkgName) && w.isVisible()) {
                    logd(str2, "fingerprint prducttest, always return modeId = " + curSettingModelHighRefreshRateId);
                    return curSettingModelHighRefreshRateId;
                }
            }
            if (isLowPowerMode()) {
                int result2 = proxy.getPreferredModeId(w);
                logd(str2, "LowPower Mode, return modeId = " + result2);
                return result2;
            } else if (this.mSettingsObserver.isScreenRecordRunning()) {
                logd(str2, "Screen Record Running , return modeId = " + this.mLowRefreshRateId);
                return this.mLowRefreshRateId;
            } else if (w.isAnimatingLw() && ((is120HzMode() || isAutoMode() || is90HzMode()) && !this.mTranRefreshRateConfig.getActiveSwitchProblemPackageList().contains(pkgName))) {
                logd(str2, "w.isAnimatingLw(), return modeId = 0");
                return 0;
            } else if ("com.netflix.mediaclient".equals(pkgName)) {
                logd(str2, " App com.netflix.mediaclient  return modeId = 0");
                return 0;
            } else if (w.getWindowingMode() == 6) {
                return curSettingModelHighRefreshRateId;
            } else {
                if (w.getAttrs().type == 2040) {
                    logd(str2, "[120Hz/Auto mode] 2040 return modeId = " + curSettingModelHighRefreshRateId);
                    return curSettingModelHighRefreshRateId;
                }
                if (isInFastSlowSlideScene(pkgName)) {
                    if (isAutoMode() || is120HzMode()) {
                        logd(str2, "[Auto/120]  App in isInFastSlowSlideScene , return modeId = 0");
                        return 0;
                    } else if (is90HzMode()) {
                        logd(str2, " [90HzMode] App in isInFastSlowSlideScene , return modeId = " + this.m90HZRefreshRateId);
                        return this.m90HZRefreshRateId;
                    }
                }
                if (lastTitle != null && this.mTranRefreshRateConfig.getVoiceCallWhiteList().contains(lastTitle) && w.isVisible()) {
                    logd(str2, "voice call app, isFullScreen:" + tranWindowStateProxy.isFullscreen());
                    if (inVoiceCallScene(w, lastTitle, true) && tranWindowStateProxy.isFullscreen()) {
                        logd(str2, "[voice-call-detector]  voice app  updateRefreshRateForVoiceCallScene return m30HzRefreshRateId =" + this.m30HzRefreshRateId);
                        if (!isSupportLessThan60hz()) {
                            return this.mLowRefreshRateId;
                        }
                        return this.m30HzRefreshRateId;
                    }
                }
                if (proxy.inNonHighRefreshPackages(pkgName)) {
                    logd(str2, " App in tmpNonHighRefreshRatePackages , using camera, return modeId = " + this.mLowRefreshRateId);
                    return this.mLowRefreshRateId;
                }
                if (pkgName != null && this.mTranRefreshRateConfig.getVideoAppSupportedList().contains(pkgName)) {
                    if (inNativeVideoScene(w, lastTitle)) {
                        logd(str2, "[video-detector]  native video app  updateRefreshRateForNativeVideoScene return m30HzRefreshRateId =" + this.m30HzRefreshRateId);
                        if (!isSupportLessThan60hz()) {
                            return this.mLowRefreshRateId;
                        }
                        return getVideoRefreshRateId(true);
                    } else if (inVideoScene(w, lastTitle)) {
                        logd(str2, "[video-detector]  voice app  updateRefreshRateForVideoScene return m45HzRefreshRateId =" + this.m45HzRefreshRateId);
                        if (!isSupportLessThan60hz()) {
                            return this.mLowRefreshRateId;
                        }
                        return getVideoRefreshRateId(false);
                    }
                }
                if ((w.getAttrs().preferredRefreshRate != 0.0f || w.getAttrs().preferredDisplayModeId != 0) && !"com.eg.android.AlipayGphone".equals(pkgName)) {
                    return proxy.getPreferredModeId(w);
                }
                if (this.mSettingsObserver.mUnconsciousSwitch) {
                    logd(str2, " happen unconscious switch to LowMode " + this.mLowRefreshRateId);
                    return this.mLowRefreshRateId;
                } else if (inNaviAppPackages(pkgName)) {
                    logd(str2, " App inNaviAppPackages , navigating, return modeId = " + this.m45HzRefreshRateId);
                    if (!isSupportLessThan60hz()) {
                        return this.mLowRefreshRateId;
                    }
                    return this.m45HzRefreshRateId;
                } else if (inputMethodScene()) {
                    logd(str2, "InputMethodShown, return mInputMethodRefreshRateId = " + this.mInputMethodRefreshRateId);
                    return this.mInputMethodRefreshRateId;
                } else if (w.isInMultiWindow()) {
                    logd(str2, " w.isInMultiWindow force return modeId 0");
                    return 0;
                } else {
                    synchronized (this.mTranRefreshRateConfig.getHighRefreshRateLock()) {
                        if (this.mTranRefreshRateConfig.getAboveResumedWindowAppList().contains(pkgName)) {
                            logd(str2, "[120Hz/Auto mode]  App above resumed window in Z Order.  return modeId = 0");
                            return 0;
                        } else if (ITranWindowState.Instance().isVisibleOnePixelWindow(w)) {
                            logd(str2, "[Any mode]  Window above resumed window in Z order just 1 * 1 pixel return modeID = 0");
                            return 0;
                        } else if (w.getAttrs().type == 2000) {
                            logd(str2, "[120Hz/Auto mode]  TYPE_STATUS_BAR  return modeId = 0");
                            return 0;
                        } else if (w.getAttrs().type == 2013) {
                            logd(str2, "[120Hz/Auto mode]  TYPE_WALLPAPER  return modeId = 0");
                            return 0;
                        } else if (w.getAttrs().type >= 2000 && w.getAttrs().type <= 2999) {
                            logd(str2, "[120Hz/Auto mode]  system window w.getAttrs().type = " + w.getAttrs().type + " return modeId = " + curSettingModelHighRefreshRateId);
                            if (w.getAttrs().type != 2019 && isLTPOScreen()) {
                                return curSettingModelHighRefreshRateId;
                            }
                            return 0;
                        } else if (this.mSettingsObserver.mIsHighRefreshRateLimitTimeOn) {
                            logd(str2, "calculatePreferredModeIdInAnimating, mSettingsObserver.mIsHighRefreshRateLimitTimeOn 1 = " + this.mSettingsObserver.mIsHighRefreshRateLimitTimeOn);
                            return 0;
                        } else if (is60HzMode()) {
                            if (z && ignorePreferredRefreshRate(pkgName) && !isLTPOScreen()) {
                                logd(str2, "[60Hz Mode] force return modeId 0");
                                return 0;
                            }
                            logd(str2, "[60Hz Mode] return modeId " + this.mLowRefreshRateId);
                            return this.mLowRefreshRateId;
                        } else if (!TRAN_90HZ_REFRESH_RATE_NOT_SUPPORT && is90HzMode()) {
                            synchronized (this.mTranRefreshRateConfig.getHighRefreshRateLock()) {
                                if (this.mTranRefreshRateConfig.get90HZRefreshRateBlackList().contains(pkgName)) {
                                    logd(str2, "[90Hz Mode] App in 90HzRefreshRateBlackList. return modeId = " + this.mLowRefreshRateId);
                                    return this.mLowRefreshRateId;
                                }
                                if (ITranGriffinFeature.Instance().isGriffinSupport() && TRAN_NOTIFICATION_REFRESH_SUPPORT) {
                                    logd(str2, "[90Hz Mode]  mResumedPkgName " + this.mTranRefreshRateConfig.mResumedPkgName);
                                    synchronized (this.mTranRefreshRateConfig.mPubgPkgSetLock) {
                                        if (this.mTranRefreshRateConfig.mPubgPkgSet.contains(this.mTranRefreshRateConfig.mResumedPkgName) && lastTitle != null && lastTitle.equals("NotificationShade") && w.isVisible()) {
                                            logd(str2, "[90Hz Mode] Pubg in Bg, Window is NotificationShade, use mLowRefreshRateId. return modeId = " + this.mLowRefreshRateId);
                                            return this.mLowRefreshRateId;
                                        }
                                    }
                                }
                                logd(str2, "[90Hz Mode] return modeId=" + this.m90HZRefreshRateId);
                                return this.m90HZRefreshRateId;
                            }
                        } else if (is120HzMode()) {
                            synchronized (this.mTranRefreshRateConfig.getHighRefreshRateLock()) {
                                if (this.mTranRefreshRateConfig.mProjectListUseWhite) {
                                    if (this.mTranRefreshRateConfig.getProjectRefreshRateList().contains(pkgName)) {
                                        logd(str2, "[120Hz Mode]" + this.mTranRefreshRateConfig.mProjectName + "use WhiteList Mode / App in projectHighRefreshRateWhiteList. return modeId = " + curSettingModelHighRefreshRateId);
                                        return curSettingModelHighRefreshRateId;
                                    }
                                    logd(str2, "[120Hz Mode]" + this.mTranRefreshRateConfig.mProjectName + "use WhiteList Mode / App out of projectHighRefreshRateWhiteList. return modeId = " + this.mLowRefreshRateId);
                                    return this.mLowRefreshRateId;
                                } else if (this.mTranRefreshRateConfig.getAccumulatedHighRefreshRateBlackList().contains(pkgName)) {
                                    logd(str2, "[120Hz Mode] App in getAccumulatedHighRefreshRateBlackList. return modeId = " + this.mLowRefreshRateId);
                                    return this.mLowRefreshRateId;
                                } else if (this.mTranRefreshRateConfig.get90HZRefreshRateWhiteList().contains(pkgName)) {
                                    logd(str2, "[120Hz Mode] App in 90HZRefreshRateWhiteList. return modeId = " + this.m90HZRefreshRateId);
                                    return this.m90HZRefreshRateId;
                                } else {
                                    if (ITranGriffinFeature.Instance().isGriffinSupport() && TRAN_NOTIFICATION_REFRESH_SUPPORT) {
                                        logd(str2, "[120Hz Mode]  mResumedPkgName " + this.mTranRefreshRateConfig.mResumedPkgName);
                                        synchronized (this.mTranRefreshRateConfig.mPubgPkgSetLock) {
                                            if (this.mTranRefreshRateConfig.mPubgPkgSet.contains(this.mTranRefreshRateConfig.mResumedPkgName) && lastTitle != null && lastTitle.equals("NotificationShade") && w.isVisible()) {
                                                logd(str2, "[120Hz Mode] Pubg in Bg, Window is NotificationShade, use mLowRefreshRateId. return modeId = " + this.mLowRefreshRateId);
                                                return this.mLowRefreshRateId;
                                            }
                                        }
                                    }
                                    logd(str2, "[120Hz Mode] return modeId = " + curSettingModelHighRefreshRateId);
                                    return curSettingModelHighRefreshRateId;
                                }
                            }
                        } else if (isAutoMode()) {
                            return getPreferredModeIdInAutoMode(pkgName, w);
                        } else {
                            Slog.e(str2, "[Null Mode] window pkg:" + pkgName + " why happened?");
                            return 0;
                        }
                    }
                }
            }
        }
    }

    @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy
    public String getFeatureInfo() {
        StringBuffer buffer = new StringBuffer();
        synchronized (this.mTranRefreshRateConfig.getHighRefreshRateLock()) {
            if (this.mTranRefreshRateConfig.isLoadCommonCfgSuccess()) {
                buffer.append("Auto Policy version:" + this.mTranRefreshRateConfig.getVersion() + ", major version:" + this.mTranRefreshRateConfig.getMajorVersion() + ", updatetime:" + this.mTranRefreshRateConfig.getUpdateTime() + "\n");
                buffer.append("ExistCustomizedConfig " + this.mTranRefreshRateConfig.mExistCustomizedConfig + "\n");
                buffer.append("mExistParseCfgException(common/project/cloud, please check.) " + this.mTranRefreshRateConfig.mExistParseCfgException + "\n");
                buffer.append("lowBatteryLevel    " + this.mTranRefreshRateConfig.getLowBatteryLevel() + "\n");
                buffer.append("recoveryBatteryLevel    " + this.mTranRefreshRateConfig.getRecoveryBatteryLevel() + "\n");
                buffer.append("FastSlowSlideBlackList:\n");
                for (String pkg : this.mTranRefreshRateConfig.mFastSlowSlideBlackList) {
                    buffer.append("    " + pkg + "\n");
                }
                buffer.append("VoiceCallWhiteList:\n");
                for (String pkg2 : this.mTranRefreshRateConfig.mVoiceCallWhiteList) {
                    buffer.append("    " + pkg2 + "\n");
                }
                buffer.append("VideoAppSupportedList:\n");
                for (String pkg3 : this.mTranRefreshRateConfig.mVideoAppSupportedList) {
                    buffer.append("    " + pkg3 + "\n");
                }
                buffer.append("TransitionAnimationList:\n");
                for (String pkg4 : this.mTranRefreshRateConfig.mTransitionAnimationList) {
                    buffer.append("    " + pkg4 + "\n");
                }
                buffer.append("ActiveSwitchProblemPackageList:\n");
                for (String pkg5 : this.mTranRefreshRateConfig.mActiveSwitchProblemPackageList) {
                    buffer.append("    " + pkg5 + "\n");
                }
                buffer.append("Choose90HzInAutoModeList:\n");
                for (String pkg6 : this.mTranRefreshRateConfig.getMinRefreshRate90HzInAutoModeList()) {
                    buffer.append("    " + pkg6 + "\n");
                }
                buffer.append("Auto Whitelist:\n");
                for (String pkg7 : this.mTranRefreshRateConfig.getAutoRefreshRateWhiteList()) {
                    buffer.append("    " + pkg7 + "\n");
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
        buffer.append("Curent refresh rate=" + this.mSettingsObserver.getRefreshRate() + "\n").append("User selected mode=" + this.mSettingsObserver.mRefreshRateMode + "\n").append("Settings peak refresh rate=" + this.mSettingsObserver.mPeakRefreshRate + "\n").append("LowRefreshRateId=" + this.mLowRefreshRateId + " m90HZRefreshRateId=" + this.m90HZRefreshRateId + ", HighRefreshRateId=" + this.mHighRefreshRateId + "\n").append("LowPowerSaving=" + this.mSettingsObserver.mLowPowerMode + "\n").append("NeedRecover mode=" + this.mSettingsObserver.getNeedRecoverRefreshState() + "\n").append("UnconsciousState is " + this.mSettingsObserver.mUnconsciousSwitch + "\n").append("CpuName is " + this.mTranRefreshRateConfig.mCpuName + "\n").append("PlatformAlias is " + this.mTranRefreshRateConfig.getPlatformAlias(this.mTranRefreshRateConfig.mCpuName) + "\n").append("ProjectName is " + this.mTranRefreshRateConfig.mProjectName + "\n").append("LoadCommonConfigSuccess is " + this.mTranRefreshRateConfig.isLoadCommonCfgSuccess() + "\n").append("project list use WhiteList = " + this.mTranRefreshRateConfig.mProjectListUseWhite + "\n").append("UserSetupCompleted is " + this.mSettingsObserver.isUserSetupCompleted() + "\n");
        synchronized (this.mTranRefreshRateConfig.getHighRefreshRateLock()) {
            buffer.append("RefreshRate90hzWhitelistIn120Mode:\n");
            for (String str : this.mTranRefreshRateConfig.get90HZRefreshRateWhiteList()) {
                buffer.append("    " + str + "\n");
            }
            if (!TRAN_90HZ_REFRESH_RATE_NOT_SUPPORT) {
                buffer.append("RefreshRate90hzBlacklist:\n");
                for (String str2 : this.mTranRefreshRateConfig.get90HZRefreshRateBlackList()) {
                    buffer.append("    " + str2 + "\n");
                }
            }
            if (!this.mTranRefreshRateConfig.mProjectListUseWhite) {
                buffer.append("AccumulatedHighRefreshRateBlackList:\n");
                for (String str3 : this.mTranRefreshRateConfig.mAccumulatedHighRefreshRateBlackList) {
                    buffer.append("    " + str3 + "\n");
                }
                buffer.append("HighRefreshRateBlackList:\n");
                for (String str4 : this.mTranRefreshRateConfig.getHighRefreshRateBlackList()) {
                    buffer.append("    " + str4 + "\n");
                }
                buffer.append("PlatformHighRefreshRateBlackList:\n");
                for (String str5 : this.mTranRefreshRateConfig.getPlatformHighRefreshRateBlackList()) {
                    buffer.append("    " + str5 + "\n");
                }
            }
            buffer.append("ProjectRefreshRateList:\n");
            for (String str6 : this.mTranRefreshRateConfig.getProjectRefreshRateList()) {
                buffer.append("    " + str6 + "\n");
            }
        }
        buffer.append("\n");
        buffer.append("AboveResumedWindowAppList:");
        for (String str7 : this.mTranRefreshRateConfig.mAboveResumedWindowAppList) {
            buffer.append("    " + str7 + "\n");
        }
        buffer.append("\n");
        buffer.append("PubgPkgSet:\n");
        for (String str8 : this.mTranRefreshRateConfig.mPubgPkgSet) {
            buffer.append("    " + str8 + "\n");
        }
        buffer.append("\n");
        buffer.append("Temp NonHighRefreshRateBlackList:");
        for (String str9 : proxy.getNonHighRefreshPackages()) {
            buffer.append("    " + str9 + "\n");
        }
        buffer.append("\n");
        return buffer.toString();
    }

    /* loaded from: classes2.dex */
    private static final class RefreshRateSettingsObserver120 extends TranBaseRefreshRatePolicy.BaseRefreshRateSettingsObserver {
        private static final String TAG = "RefreshRateObserver-120";
        private static volatile RefreshRateSettingsObserver120 sDefault;

        RefreshRateSettingsObserver120(Context context, WindowManagerService wm, Handler handler, float minRefreshRate) {
            super(context, wm, handler);
            this.mMinRefreshRate = minRefreshRate;
            IntentFilter powerFilter = new IntentFilter();
            powerFilter.addAction("android.intent.action.BATTERY_CHANGED");
            final TranRefreshRateConfig120 mTranrefreshRateConfig = TranRefreshRateConfig120.getInstance();
            BroadcastReceiver powerReceiver = new BroadcastReceiver() { // from class: com.transsion.hubcore.server.wm.tranrefreshrate.TranRefreshRatePolicy120.RefreshRateSettingsObserver120.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context2, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                        int percent = RefreshRateSettingsObserver120.this.getPowerPercent(intent);
                        boolean charging = RefreshRateSettingsObserver120.this.isCharging(intent);
                        TranBaseRefreshRatePolicy.logd(RefreshRateSettingsObserver120.TAG, "powerReceiver->onReceive()  percent = " + percent + " charging = " + charging);
                        if (-1 == percent) {
                            Slog.d(RefreshRateSettingsObserver120.TAG, "powerReceiver->onReceive() percent is invalid ");
                            return;
                        }
                        synchronized (mTranrefreshRateConfig.getCloudConfigLock()) {
                            if (percent < mTranrefreshRateConfig.getLowBatteryLevel() && !charging) {
                                RefreshRateSettingsObserver120.this.mUnconsciousSwitch = true;
                                TranBaseRefreshRatePolicy.logd(RefreshRateSettingsObserver120.TAG, "powerReceiver->onReceive() set mUnconsciousSwitch = true");
                            } else if (percent > mTranrefreshRateConfig.getRecoveryBatteryLevel()) {
                                RefreshRateSettingsObserver120.this.mUnconsciousSwitch = false;
                                TranBaseRefreshRatePolicy.logd(RefreshRateSettingsObserver120.TAG, "powerReceiver->onReceive() set mUnconsciousSwitch = false");
                            }
                        }
                    }
                }
            };
            if (TranRefreshRatePolicy120.TRAN_LOW_BATTERY_60HZ_REFRESH_RATE_SUPPORT) {
                this.mContext.registerReceiverAsUser(powerReceiver, UserHandle.ALL, powerFilter, null, null);
            }
            observe();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getPowerPercent(Intent batteryIntent) {
            if (batteryIntent == null) {
                return -1;
            }
            int level = batteryIntent.getIntExtra("level", 0);
            int scale = batteryIntent.getIntExtra("scale", 100);
            TranBaseRefreshRatePolicy.logd(TAG, "getPowerPercent() level = " + level + " scale = " + scale);
            return (level * 100) / scale;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isCharging(Intent batteryIntent) {
            if (batteryIntent == null) {
                return false;
            }
            int status = batteryIntent.getIntExtra("status", 1);
            int plugged = batteryIntent.getIntExtra("plugged", 0);
            boolean pluggedIn = plugged != 0;
            boolean isChargingState = status == 2;
            TranBaseRefreshRatePolicy.logd(TAG, "isCharging()  status = " + status + " plugged = " + plugged + " isChargingState = " + isChargingState + " pluggedIn = " + pluggedIn);
            return pluggedIn && isChargingState;
        }

        public static synchronized RefreshRateSettingsObserver120 getInstance(Context context, WindowManagerService wm, float minRefreshRate) {
            RefreshRateSettingsObserver120 refreshRateSettingsObserver120;
            synchronized (RefreshRateSettingsObserver120.class) {
                if (sDefault == null) {
                    synchronized (RefreshRateSettingsObserver120.class) {
                        if (sDefault == null) {
                            sDefault = new RefreshRateSettingsObserver120(context, wm, new Handler(), minRefreshRate);
                        }
                    }
                }
                refreshRateSettingsObserver120 = sDefault;
            }
            return refreshRateSettingsObserver120;
        }

        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.BaseRefreshRateSettingsObserver
        protected int getInitRefreshRateMode() {
            if (this.mPeakRefreshRate <= 60.0f) {
                return 60;
            }
            if (!TranRefreshRatePolicy120.TRAN_90HZ_REFRESH_RATE_NOT_SUPPORT && this.mPeakRefreshRate <= 90.0f) {
                return 90;
            }
            return 120;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.BaseRefreshRateSettingsObserver
        public void observe() {
            if (-1.0f == this.mPeakRefreshRate && -2 == this.mRefreshRateMode) {
                if (TranBaseRefreshRatePolicy.TRAN_DEFAULT_AUTO_REFRESH_SUPPORT) {
                    Settings.System.putInt(this.mContext.getContentResolver(), "tran_refresh_mode", 10000);
                    Slog.d(TAG, "Init default user settings as auto mode");
                } else {
                    Settings.System.putInt(this.mContext.getContentResolver(), "tran_refresh_mode", 120);
                    Slog.d(TAG, "Init default user settings as refresh_rate = 120");
                }
            }
            super.observe();
        }

        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.BaseRefreshRateSettingsObserver
        protected int getRecoverRefreshRateMode() {
            return Settings.System.getIntForUser(this.mContext.getContentResolver(), "last_tran_refresh_mode_in_refresh_setting", TranBaseRefreshRatePolicy.TRAN_DEFAULT_AUTO_REFRESH_SUPPORT ? 10000 : 120, 0);
        }

        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.BaseRefreshRateSettingsObserver
        protected void updateRefreshRateModeSettingsInternel() {
            this.mCurrentMinRefreshRate = this.mMinRefreshRate;
            if (isLowerThanBrightnessThreshold()) {
                this.mCurrentMinRefreshRate = 60.0f;
            }
            if (TranRefreshRatePolicy120.TRAN_FINGER_RINTER_HIGH_REFRESH_SUPPORT) {
                if (!TranRefreshRatePolicy120.TRAN_90HZ_REFRESH_RATE_NOT_SUPPORT && 90 == this.mRefreshRateMode) {
                    Settings.System.putFloat(this.mContext.getContentResolver(), "peak_refresh_rate", 90.0f);
                    Settings.System.putFloat(this.mContext.getContentResolver(), "min_refresh_rate", this.mCurrentMinRefreshRate);
                } else if (60 == this.mRefreshRateMode) {
                    Settings.System.putFloat(this.mContext.getContentResolver(), "peak_refresh_rate", 60.0f);
                    Settings.System.putFloat(this.mContext.getContentResolver(), "min_refresh_rate", this.mCurrentMinRefreshRate);
                } else {
                    Settings.System.putFloat(this.mContext.getContentResolver(), "peak_refresh_rate", 120.0f);
                    Settings.System.putFloat(this.mContext.getContentResolver(), "min_refresh_rate", this.mCurrentMinRefreshRate);
                }
            } else if (10000 == this.mRefreshRateMode || 120 == this.mRefreshRateMode) {
                Settings.System.putFloat(this.mContext.getContentResolver(), "peak_refresh_rate", 120.0f);
                Settings.System.putFloat(this.mContext.getContentResolver(), "min_refresh_rate", this.mCurrentMinRefreshRate);
            } else if (60 == this.mRefreshRateMode) {
                Settings.System.putFloat(this.mContext.getContentResolver(), "peak_refresh_rate", 60.0f);
                Settings.System.putFloat(this.mContext.getContentResolver(), "min_refresh_rate", this.mCurrentMinRefreshRate);
            } else if (!TranRefreshRatePolicy120.TRAN_90HZ_REFRESH_RATE_NOT_SUPPORT && 90 == this.mRefreshRateMode) {
                Settings.System.putFloat(this.mContext.getContentResolver(), "peak_refresh_rate", 90.0f);
                Settings.System.putFloat(this.mContext.getContentResolver(), "min_refresh_rate", this.mCurrentMinRefreshRate);
            }
            TranBaseRefreshRatePolicy.logd(TAG, "updateRefreshRateModeSettingsInternel mRefreshRateMode:" + this.mRefreshRateMode);
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
                return 120;
            }
            if (refreshChoose == 2) {
                return 10000;
            }
            return refreshChoose;
        }
    }

    /* loaded from: classes2.dex */
    private static final class TranRefreshRateConfig120 extends TranBaseRefreshRatePolicy.TranBaseRefreshRateConfig {
        private static volatile TranRefreshRateConfig120 sDefault;

        TranRefreshRateConfig120() {
            TranTrancareManager.regCloudEngineCallback("1001000008", "v2.1", new ITranCloudEngineCallback.Stub() { // from class: com.transsion.hubcore.server.wm.tranrefreshrate.TranRefreshRatePolicy120.TranRefreshRateConfig120.1
                public void onUpdate(String Key, boolean fileState, String config) {
                    TranBaseRefreshRatePolicy.logd(TranRefreshRatePolicy120.TAG, "onUpdate: Key:" + Key + ",fileState:" + fileState + ",config:" + config);
                    TranRefreshRateConfig120.this.loadCloudTranRefreshRateConfig();
                    TranTrancareManager.feedBack(Key, true);
                }
            });
        }

        public static synchronized TranRefreshRateConfig120 getInstance() {
            TranRefreshRateConfig120 tranRefreshRateConfig120;
            synchronized (TranRefreshRateConfig120.class) {
                if (sDefault == null) {
                    synchronized (TranRefreshRateConfig120.class) {
                        if (sDefault == null) {
                            sDefault = new TranRefreshRateConfig120();
                        }
                    }
                }
                tranRefreshRateConfig120 = sDefault;
            }
            return tranRefreshRateConfig120;
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
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -957193541:
                    if (p.equals("MT6769V/CZ")) {
                        c = 2;
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
                    return "G95";
                case 1:
                    return "G80";
                case 2:
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
                Slog.d(TranRefreshRatePolicy120.TAG, "load localJs failed");
                return;
            }
            updateTranRefreshRateConfig(localJsonStr, false);
            String customizedLocalJsonFilePath = Environment.getProductDirectory().getPath() + "/apm/config/project_refresh_rate_config.json";
            String customizedLocalJsonStr = readFileString(customizedLocalJsonFilePath);
            if (!TextUtils.isEmpty(customizedLocalJsonStr)) {
                this.mExistCustomizedConfig = true;
                updateTranRefreshRateConfig(customizedLocalJsonStr, true);
            } else {
                Slog.d(TranRefreshRatePolicy120.TAG, "no exist customized config!");
            }
            loadCloudTranRefreshRateConfig();
        }

        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.TranBaseRefreshRateConfig
        protected void updateTranRefreshRateConfig(String jsonStr, boolean ignoreMinor) {
            try {
                JSONObject jsonObject = new JSONObject(jsonStr);
                int major_version = jsonObject.optInt("major_version");
                int version = jsonObject.optInt("version");
                int updateTime = jsonObject.optInt("update_time");
                synchronized (this.mPolicyModeLock) {
                    if (this.mConfigLastVersion != 0) {
                        if (major_version != this.mConfigLastMajorVersion) {
                            Slog.d(TranRefreshRatePolicy120.TAG, "major Version invalid: currentMajorVersion = " + this.mConfigLastMajorVersion + ", updateMajorVersion = " + major_version);
                            return;
                        } else if (!ignoreMinor && version <= this.mConfigLastVersion) {
                            Slog.d(TranRefreshRatePolicy120.TAG, "update Version invalid: current = " + this.mConfigLastVersion + ", updateVersion = " + version);
                            return;
                        }
                    }
                    this.mConfigLastMajorVersion = major_version;
                    this.mConfigLastVersion = version;
                    this.mUpdateTime = updateTime;
                    if (jsonObject.has("fast_slow_slide_blacklist")) {
                        synchronized (this.mHighRefreshRateLock) {
                            this.mFastSlowSlideBlackList = jsonArray2Set(jsonObject.optJSONArray("fast_slow_slide_blacklist"));
                        }
                    }
                    if (jsonObject.has("voice_and_video_call_whitelist")) {
                        synchronized (this.mHighRefreshRateLock) {
                            this.mVoiceCallWhiteList = jsonArray2Set(jsonObject.optJSONArray("voice_and_video_call_whitelist"));
                        }
                    }
                    if (jsonObject.has("detect_video_app_scope")) {
                        synchronized (this.mHighRefreshRateLock) {
                            this.mVideoAppSupportedList = jsonArray2Set(jsonObject.optJSONArray("detect_video_app_scope"));
                        }
                    }
                    if (jsonObject.has("transition_animation_whitelist")) {
                        synchronized (this.mHighRefreshRateLock) {
                            this.mTransitionAnimationList = jsonArray2Set(jsonObject.optJSONArray("transition_animation_whitelist"));
                        }
                    }
                    if (jsonObject.has("active_switch_package_list")) {
                        synchronized (this.mHighRefreshRateLock) {
                            this.mActiveSwitchProblemPackageList = jsonArray2Set(jsonObject.optJSONArray("active_switch_package_list"));
                        }
                    }
                    if (jsonObject.has("auto_refresh_rate_whitelist")) {
                        synchronized (this.mHighRefreshRateLock) {
                            this.mAutoRefreshRateWhiteList = jsonArray2Set(jsonObject.optJSONArray("auto_refresh_rate_whitelist"));
                        }
                    }
                    if (jsonObject.has("min_refresh_rate_90hz_in_auto_mode")) {
                        synchronized (this.mHighRefreshRateLock) {
                            this.mMinRefreshRate90HzInAutoModeList = jsonArray2Set(jsonObject.optJSONArray("min_refresh_rate_90hz_in_auto_mode"));
                        }
                    }
                    if (jsonObject.has("pubg_package_list")) {
                        synchronized (this.mPubgPkgSetLock) {
                            this.mPubgPkgSet = jsonArray2Set(jsonObject.optJSONArray("pubg_package_list"));
                        }
                    }
                    if (jsonObject.has("app_above_resumed_window_list")) {
                        synchronized (this.mHighRefreshRateLock) {
                            this.mAboveResumedWindowAppList = jsonArray2Set(jsonObject.optJSONArray("app_above_resumed_window_list"));
                        }
                    }
                    if (jsonObject.has("refresh_rate_90hz_whitelist_in_120hz_mode")) {
                        synchronized (this.mHighRefreshRateLock) {
                            this.m90HZRefreshRateWhiteListIn120HzMode = jsonArray2Set(jsonObject.optJSONArray("refresh_rate_90hz_whitelist_in_120hz_mode"));
                        }
                    }
                    if (!TranRefreshRatePolicy120.TRAN_90HZ_REFRESH_RATE_NOT_SUPPORT && jsonObject.has("refresh_rate_blacklist_in_90hz_mode")) {
                        synchronized (this.mHighRefreshRateLock) {
                            this.mRefreshRateBlackListIn90HzMode = jsonArray2Set(jsonObject.optJSONArray("refresh_rate_blacklist_in_90hz_mode"));
                        }
                    }
                    synchronized (this.mHighRefreshRateLock) {
                        this.mAccumulatedHighRefreshRateBlackList.clear();
                        if (jsonObject.has("refresh_rate_blacklist_in_120hz_mode")) {
                            synchronized (this.mHighRefreshRateLock) {
                                this.mHighRefreshRateBlackList = jsonArray2Set(jsonObject.optJSONArray("refresh_rate_blacklist_in_120hz_mode"));
                            }
                        }
                        this.mAccumulatedHighRefreshRateBlackList.addAll(this.mHighRefreshRateBlackList);
                        if (jsonObject.has("platform_pkg_set_entrys")) {
                            JSONObject platJsonObject = jsonObject.optJSONObject("platform_pkg_set_entrys");
                            String platformAlias = getPlatformAlias(this.mCpuName);
                            if (platJsonObject.has(platformAlias)) {
                                this.mPlatformHighRefreshRateBlackList = jsonArray2Set(platJsonObject.optJSONArray(platformAlias));
                            }
                        }
                        this.mAccumulatedHighRefreshRateBlackList.addAll(this.mPlatformHighRefreshRateBlackList);
                        String projectPkgSetName = "";
                        if (jsonObject.has("project_pkg_set_entry_index")) {
                            JSONObject projectIndexJson = jsonObject.optJSONObject("project_pkg_set_entry_index");
                            if (projectIndexJson.has(this.mProjectName)) {
                                projectPkgSetName = projectIndexJson.optString(this.mProjectName);
                            }
                        }
                        if (!TextUtils.isEmpty(projectPkgSetName) && jsonObject.has("project_pkg_set_entrys")) {
                            JSONObject allProjectSet = jsonObject.optJSONObject("project_pkg_set_entrys");
                            if (allProjectSet.has(projectPkgSetName)) {
                                JSONObject projectJson = allProjectSet.optJSONObject(projectPkgSetName);
                                String projectUseMode = "";
                                if (projectJson.has("used_mode")) {
                                    projectUseMode = projectJson.optString("used_mode");
                                    this.mProjectListUseWhite = "whitelist".equals(projectUseMode);
                                }
                                if (!TextUtils.isEmpty(projectUseMode) && projectJson.has("values")) {
                                    this.mProjectRefreshRateList = jsonArray2Set(projectJson.optJSONArray("values"));
                                }
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

        @Override // com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.TranBaseRefreshRateConfig
        protected void loadCloudTranRefreshRateConfig() {
            if (TranTrancareManager.getEnabled("1001000008", false)) {
                String cloudJsonFilePath = TranTrancareManager.getFilePath("1001000008");
                if (cloudJsonFilePath != null) {
                    cloudJsonFilePath = cloudJsonFilePath + "/refresh_rate_config.json";
                }
                String cloudJsonStr = cloudJsonFilePath != null ? readFileString(FilePathCheckUtil.checkFilePath(cloudJsonFilePath, TranBaseRefreshRatePolicy.mDebug)) : null;
                if (!TextUtils.isEmpty(cloudJsonStr)) {
                    updateTranRefreshRateConfig(cloudJsonStr, false);
                    Slog.d(TranRefreshRatePolicy120.TAG, "load RefreshRateConfig from cloud");
                    return;
                }
                return;
            }
            Slog.d(TranRefreshRatePolicy120.TAG, "CLOUD_TRANREFRESHRATE_120HZ_BLACKLIST_FILE_KEY is disabled");
        }
    }
}
