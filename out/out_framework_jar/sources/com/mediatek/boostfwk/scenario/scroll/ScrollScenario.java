package com.mediatek.boostfwk.scenario.scroll;

import android.content.Context;
import android.view.MotionEvent;
/* loaded from: classes4.dex */
public class ScrollScenario extends ViewScenario {
    private int mBoostStatus;
    private Context mContext;
    private MotionEvent mEvent;
    private boolean mIsSFPEnable = false;
    private Object mObject;

    public ScrollScenario() {
        this.mScenario = 1;
    }

    public ScrollScenario(int scenario, int action, int boostStatus, Context context) {
        this.mScenario = scenario;
        this.mScenarioAction = action;
        this.mBoostStatus = boostStatus;
        this.mContext = context;
    }

    public ScrollScenario(int scenario, int action, Context context, Object object) {
        this.mScenario = scenario;
        this.mScenarioAction = action;
        this.mContext = context;
        this.mObject = object;
    }

    public ScrollScenario(int scenario, int action, int boostStatus, Context context, Object object) {
        this.mScenario = scenario;
        this.mScenarioAction = action;
        this.mBoostStatus = boostStatus;
        this.mContext = context;
        this.mObject = object;
    }

    public ScrollScenario(int scenario, int action, MotionEvent event, Context context) {
        this.mScenario = scenario;
        this.mScenarioAction = action;
        this.mEvent = event;
        this.mContext = context;
    }

    public ScrollScenario setAction(int action) {
        this.mScenarioAction = action;
        return this;
    }

    public ScrollScenario setMotionEvent(MotionEvent event) {
        this.mEvent = event;
        return this;
    }

    public ScrollScenario setBoostStatus(int boostStatus) {
        this.mBoostStatus = boostStatus;
        return this;
    }

    public ScrollScenario setContext(Context context) {
        this.mContext = context;
        return this;
    }

    public ScrollScenario setObject(Object object) {
        this.mObject = object;
        return this;
    }

    public ScrollScenario setSFPEnable(boolean enable) {
        this.mIsSFPEnable = enable;
        return this;
    }

    public int getBoostStatus() {
        return this.mBoostStatus;
    }

    public Context getScenarioContext() {
        return this.mContext;
    }

    public MotionEvent getScenarioInputEvent() {
        return this.mEvent;
    }

    public Object getScenarioObj() {
        return this.mObject;
    }

    public boolean isSFPEnable() {
        return this.mIsSFPEnable;
    }
}
