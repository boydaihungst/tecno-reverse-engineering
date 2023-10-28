package com.mediatek.boostfwk.identify.message;

import com.mediatek.boostfwk.identify.BaseIdentify;
import com.mediatek.boostfwk.scenario.BasicScenario;
import com.mediatek.boostfwk.scenario.message.MessageScenario;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.boostfwk.utils.LogUtil;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class MsgIdentify extends BaseIdentify {
    private static final String TAG = "MsgIdentify";
    private static MsgIdentify sInstance = null;
    private static Object slock = new Object();
    private boolean mIsAudioMsgBegin = false;
    private List<Integer> mSpAudioStepList = new ArrayList();
    private final String[] mSpAudioMsgViewList = {"SoundWaveView", "LanguageChoiceLayout"};
    private List<AudioStateListener> mAudioStateListeners = new ArrayList();

    /* loaded from: classes.dex */
    public interface AudioStateListener {
        void onAudioMsgStatusUpdate(boolean z);
    }

    public static MsgIdentify getInstance() {
        if (sInstance == null) {
            synchronized (slock) {
                if (sInstance == null) {
                    sInstance = new MsgIdentify();
                }
            }
        }
        return sInstance;
    }

    public void registerAudioStateListener(AudioStateListener listener) {
        if (listener == null) {
            return;
        }
        this.mAudioStateListeners.add(listener);
    }

    @Override // com.mediatek.boostfwk.identify.BaseIdentify
    public boolean dispatchScenario(BasicScenario basicScenario) {
        if (basicScenario == null) {
            LogUtil.mLogw(TAG, "No message scenario to dispatcher.");
            return false;
        }
        MessageScenario scenario = (MessageScenario) basicScenario;
        int action = scenario.getScenarioAction();
        if (Config.isBoostFwkLogEnable()) {
            LogUtil.mLogd(TAG, "Message action dispatcher to = " + action);
        }
        switch (action) {
            case 0:
                audioMsgStatusUpdate(scenario);
                return true;
            default:
                LogUtil.mLogw(TAG, "Not found dispatcher message action.");
                return true;
        }
    }

    public void audioMsgStatusUpdate(MessageScenario scenario) {
        String viewName = scenario.getViewName();
        int visibilityMask = scenario.getVisibilityMask();
        boolean enabled = false;
        if (viewName.equals(MessageScenario.mAudioMsgViewList[0])) {
            enabled = audioMsgUpdate(visibilityMask);
        } else if (viewName.contains(MessageScenario.mAudioMsgViewList[1])) {
            enabled = specialAudioMsgUpdate(viewName, visibilityMask);
        }
        if (this.mIsAudioMsgBegin != enabled) {
            this.mIsAudioMsgBegin = enabled;
            for (AudioStateListener audioStateListener : this.mAudioStateListeners) {
                audioStateListener.onAudioMsgStatusUpdate(this.mIsAudioMsgBegin);
            }
            LogUtil.mLogd(TAG, "audioMsgStatusUpdate. status = " + this.mIsAudioMsgBegin);
        }
    }

    private boolean audioMsgUpdate(int visibilityMask) {
        return visibilityMask == 0;
    }

    private boolean specialAudioMsgUpdate(String viewName, int visibilityMask) {
        LogUtil.mLogd(TAG, "specialAudioMsgUpdate. viewName = " + viewName + " visibilityMask = " + visibilityMask);
        if (this.mIsAudioMsgBegin) {
            if (viewName.trim().contains(this.mSpAudioMsgViewList[1]) && this.mSpAudioStepList.size() >= 3 && visibilityMask == 8) {
                this.mSpAudioStepList.clear();
                return false;
            }
            viewName.trim().contains(this.mSpAudioMsgViewList[0]);
            return true;
        } else if (!viewName.trim().contains(this.mSpAudioMsgViewList[0])) {
            return false;
        } else {
            switch (this.mSpAudioStepList.size()) {
                case 0:
                    if (visibilityMask != 0) {
                        return false;
                    }
                    this.mSpAudioStepList.add(Integer.valueOf(visibilityMask));
                    return false;
                case 1:
                    if (visibilityMask != 8 || this.mSpAudioStepList.get(0).intValue() != 0) {
                        return false;
                    }
                    this.mSpAudioStepList.add(Integer.valueOf(visibilityMask));
                    return false;
                case 2:
                    if (visibilityMask != 0 || this.mSpAudioStepList.get(1).intValue() != 8) {
                        return false;
                    }
                    this.mSpAudioStepList.add(Integer.valueOf(visibilityMask));
                    return true;
                default:
                    return false;
            }
        }
    }
}
