package com.mediatek.boostfwk.identify.ime;

import android.view.Window;
import com.mediatek.boostfwk.identify.BaseIdentify;
import com.mediatek.boostfwk.scenario.BasicScenario;
import com.mediatek.boostfwk.scenario.ime.IMEScenario;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.boostfwk.utils.LogUtil;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class IMEIdentify extends BaseIdentify {
    private static final String TAG = "IMEIdentify";
    private List<IMEStateListener> mIMEStateListeners = new ArrayList();
    private boolean mImeShow = false;
    private static final Object LOCK = new Object();
    private static IMEIdentify mInstance = null;

    /* loaded from: classes.dex */
    public interface IMEStateListener {
        void onInit(Window window);

        void onVisibilityChange(boolean z);
    }

    public static IMEIdentify getInstance() {
        if (mInstance == null) {
            synchronized (LOCK) {
                if (mInstance == null) {
                    mInstance = new IMEIdentify();
                }
            }
        }
        return mInstance;
    }

    private IMEIdentify() {
    }

    @Override // com.mediatek.boostfwk.identify.BaseIdentify
    public boolean isMainThreadOnly() {
        return false;
    }

    @Override // com.mediatek.boostfwk.identify.BaseIdentify
    public boolean dispatchScenario(BasicScenario basicScenario) {
        if (basicScenario == null) {
            return false;
        }
        IMEScenario scenario = (IMEScenario) basicScenario;
        int action = scenario.getScenarioAction();
        switch (action) {
            case 1:
                notifyVisibilityChange(true);
                break;
            case 2:
                notifyVisibilityChange(false);
                break;
            case 3:
                notifyIMEInit(scenario.getWindowAndClear());
                break;
            default:
                if (Config.isBoostFwkLogEnable()) {
                    LogUtil.mLogd(TAG, "not support scnario action");
                    break;
                }
                break;
        }
        return true;
    }

    private void notifyIMEInit(Window window) {
        if (window != null) {
            for (IMEStateListener imeStateListener : this.mIMEStateListeners) {
                imeStateListener.onInit(window);
            }
        }
    }

    private void notifyVisibilityChange(boolean show) {
        if (this.mImeShow == show) {
            return;
        }
        this.mImeShow = show;
        for (IMEStateListener imeStateListener : this.mIMEStateListeners) {
            imeStateListener.onVisibilityChange(show);
        }
    }

    public void registerIMEStateListener(IMEStateListener listener) {
        if (listener == null) {
            return;
        }
        this.mIMEStateListeners.add(listener);
    }
}
