package com.mediatek.boostfwk.identify.refreshrate;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.view.Window;
import com.mediatek.boostfwk.identify.BaseIdentify;
import com.mediatek.boostfwk.identify.ime.IMEIdentify;
import com.mediatek.boostfwk.identify.message.MsgIdentify;
import com.mediatek.boostfwk.identify.scroll.ScrollIdentify;
import com.mediatek.boostfwk.info.ActivityInfo;
import com.mediatek.boostfwk.policy.refreshrate.RefreshRatePolicy;
import com.mediatek.boostfwk.scenario.BasicScenario;
import com.mediatek.boostfwk.scenario.refreshrate.EventScenario;
import com.mediatek.boostfwk.scenario.refreshrate.RefreshRateScenario;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.boostfwk.utils.LogUtil;
import com.mediatek.boostfwk.utils.TasksUtil;
/* loaded from: classes.dex */
public class RefreshRateIdentify extends BaseIdentify implements IMEIdentify.IMEStateListener, MsgIdentify.AudioStateListener, ScrollIdentify.TouchEventListener {
    private static final String TAG = "MSYNC3-VariableRefreshRate";
    private boolean mIsSmoothFlingEnabled;
    private boolean mIsVeriableRefreshRateSupported;
    private RefreshRatePolicy mRefreshRatePolicy;
    private static RefreshRateIdentify sInstance = null;
    private static Object lock = new Object();
    private ActivityInfo activityInfo = null;
    private boolean mIsConfigInited = false;
    private String mPackageName = null;

    public static RefreshRateIdentify getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    sInstance = new RefreshRateIdentify();
                }
            }
        }
        return sInstance;
    }

    private RefreshRateIdentify() {
        this.mIsVeriableRefreshRateSupported = false;
        this.mIsSmoothFlingEnabled = false;
        this.mIsVeriableRefreshRateSupported = Config.isVariableRefreshRateSupported();
        this.mIsSmoothFlingEnabled = Config.isMSync3SmoothFlingEnabled();
        if (this.mIsVeriableRefreshRateSupported) {
            LogUtil.mLogi(TAG, "Variable refreshrate is enabled");
            this.mRefreshRatePolicy = new RefreshRatePolicy();
            IMEIdentify.getInstance().registerIMEStateListener(this);
            MsgIdentify.getInstance().registerAudioStateListener(this);
            ScrollIdentify.getInstance().registerTouchEventListener(this);
            return;
        }
        LogUtil.mLogi(TAG, "Variable refreshrate is disabled");
    }

    private void configInit(RefreshRateScenario refreshRateScenario) {
        if (refreshRateScenario != null) {
            if (refreshRateScenario.getScenarioContext() != null) {
                Context viewContext = refreshRateScenario.getScenarioContext();
                String packageName = ActivityInfo.getInstance().getPackageName();
                this.mPackageName = packageName;
                if (packageName == null) {
                    this.mPackageName = viewContext.getPackageName();
                }
                if (this.mPackageName == null || Config.SYSTEM_PACKAGE_ARRAY.contains(this.mPackageName)) {
                    this.mIsVeriableRefreshRateSupported = false;
                    this.mRefreshRatePolicy.setVeriableRefreshRateSupported(false);
                    LogUtil.mLogi(TAG, "App is not support");
                }
                if (this.mIsVeriableRefreshRateSupported && TasksUtil.isGameAPP(this.mPackageName)) {
                    this.mIsVeriableRefreshRateSupported = false;
                    this.mRefreshRatePolicy.setVeriableRefreshRateSupported(false);
                    LogUtil.mLogi(TAG, "Game is not support");
                }
                this.mIsConfigInited = true;
                return;
            }
            this.mIsConfigInited = false;
        }
    }

    @Override // com.mediatek.boostfwk.identify.BaseIdentify
    public boolean dispatchScenario(BasicScenario scenario) {
        if (this.mIsVeriableRefreshRateSupported) {
            LogUtil.traceBegin("Dispatch refresh rate scenario");
            if (scenario != null) {
                if (scenario.getScenario() == 6) {
                    RefreshRateScenario refreshRateScenario = (RefreshRateScenario) scenario;
                    if (!this.mIsConfigInited) {
                        configInit(refreshRateScenario);
                    }
                    Context activityContext = refreshRateScenario.getScenarioContext();
                    if (activityContext != null && (activityContext instanceof Activity)) {
                        this.mRefreshRatePolicy.dispatchScenario(refreshRateScenario);
                        LogUtil.traceEnd();
                        return true;
                    }
                } else if (scenario.getScenario() == 7) {
                    EventScenario eventScenario = (EventScenario) scenario;
                    this.mRefreshRatePolicy.dispatchEvent(eventScenario);
                    LogUtil.traceEnd();
                    return true;
                }
            }
            LogUtil.traceEnd();
            return false;
        }
        return false;
    }

    @Override // com.mediatek.boostfwk.identify.scroll.ScrollIdentify.TouchEventListener
    public void onTouchEvent(MotionEvent event) {
        if (this.mIsVeriableRefreshRateSupported) {
            this.mRefreshRatePolicy.onTouchEvent(event);
        }
    }

    @Override // com.mediatek.boostfwk.identify.ime.IMEIdentify.IMEStateListener
    public void onInit(Window window) {
        if (this.mIsVeriableRefreshRateSupported) {
            this.mRefreshRatePolicy.onIMEInit(window);
        }
    }

    @Override // com.mediatek.boostfwk.identify.ime.IMEIdentify.IMEStateListener
    public void onVisibilityChange(boolean show) {
        if (this.mIsVeriableRefreshRateSupported) {
            this.mRefreshRatePolicy.onIMEVisibilityChange(show);
        }
    }

    @Override // com.mediatek.boostfwk.identify.message.MsgIdentify.AudioStateListener
    public void onAudioMsgStatusUpdate(boolean isAudioMsgBegin) {
        if (this.mIsVeriableRefreshRateSupported) {
            this.mRefreshRatePolicy.onVoiceDialogEvent(isAudioMsgBegin);
        }
    }
}
