package com.mediatek.boostfwk.scenario.message;

import com.mediatek.boostfwk.scenario.BasicScenario;
/* loaded from: classes4.dex */
public class MessageScenario extends BasicScenario {
    public static final String[] mAudioMsgViewList = {"com.tencent.mobileqq.activity.aio.audiopanel.AudioPanel", "com.tencent.mm.plugin.transvoice.ui"};
    protected int mScenarioAction;
    private String mViewName;
    private int mVisibilityMask;

    public int getScenarioAction() {
        return this.mScenarioAction;
    }

    public int getVisibilityMask() {
        return this.mVisibilityMask;
    }

    public String getViewName() {
        return this.mViewName;
    }

    public void setScenarioInfo(int scenario, int action, String viewName, int visibilityMask) {
        this.mScenario = scenario;
        this.mScenarioAction = action;
        this.mViewName = viewName;
        this.mVisibilityMask = visibilityMask;
    }

    public boolean isAudioMsgView(String viewMsgName) {
        String[] strArr;
        for (String audioMsgViewName : mAudioMsgViewList) {
            if (viewMsgName != null && viewMsgName.trim().contains(audioMsgViewName)) {
                return true;
            }
        }
        return false;
    }
}
