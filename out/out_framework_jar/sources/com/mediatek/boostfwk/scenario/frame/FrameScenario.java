package com.mediatek.boostfwk.scenario.frame;

import android.view.Choreographer;
import android.view.ThreadedRenderer;
import com.mediatek.boostfwk.scenario.BasicScenario;
/* loaded from: classes4.dex */
public class FrameScenario extends BasicScenario {
    private int mBoostStatus;
    private Choreographer mChoreographer;
    private Choreographer.FrameData mFrameData;
    private long mFrameId;
    private int mFrameStep;
    private long mFrameTimeResult;
    private boolean mIsPreAnimEnable;
    private boolean mIsSFPEnable;
    private long mOrigFrameTimeNano;
    protected int mScenarioAction;
    private int mRenderThreadTid = Integer.MIN_VALUE;
    private boolean mIsListenFrameHint = true;
    private ThreadedRenderer mThreadedRenderer = null;
    private boolean mIsFling = false;
    private boolean mIsPreAnim = false;

    public FrameScenario() {
        this.mScenario = 2;
    }

    public FrameScenario(int scenario, int action, int boostStatus, long frameId) {
        this.mScenario = scenario;
        this.mScenarioAction = action;
        this.mBoostStatus = boostStatus;
        this.mFrameStep = -1;
        this.mFrameId = frameId;
    }

    public FrameScenario(int scenario, int action, int boostStatus, int frameStep) {
        this.mScenario = scenario;
        this.mScenarioAction = action;
        this.mBoostStatus = boostStatus;
        this.mFrameStep = frameStep;
    }

    public FrameScenario setAction(int action) {
        this.mScenarioAction = action;
        return this;
    }

    public FrameScenario setBoostStatus(int boostStatus) {
        this.mBoostStatus = boostStatus;
        return this;
    }

    public FrameScenario setFrameStep(int frameStep) {
        this.mFrameStep = frameStep;
        return this;
    }

    public FrameScenario setFrameId(long frameId) {
        this.mFrameId = frameId;
        return this;
    }

    public FrameScenario setRenderThreadId(int renderThreadTid) {
        this.mRenderThreadTid = renderThreadTid;
        return this;
    }

    public FrameScenario setThreadedRenderer(ThreadedRenderer render) {
        this.mThreadedRenderer = render;
        return this;
    }

    public boolean canInitRenderThreadId() {
        return this.mRenderThreadTid != Integer.MIN_VALUE;
    }

    public void setIsListenFrameHint(boolean listen) {
        if (listen != this.mIsListenFrameHint) {
            this.mIsListenFrameHint = listen;
        }
    }

    public FrameScenario setFling(boolean isFling) {
        this.mIsFling = isFling;
        return this;
    }

    public FrameScenario setChoreographer(Choreographer choreographer) {
        this.mChoreographer = choreographer;
        return this;
    }

    public FrameScenario setFrameData(Choreographer.FrameData frameData) {
        this.mFrameData = frameData;
        return this;
    }

    public FrameScenario setOrigFrameTimeNano(long origFrameTimeNano) {
        this.mOrigFrameTimeNano = origFrameTimeNano;
        return this;
    }

    public FrameScenario setFrameTimeResult(long frameTimeResult) {
        this.mFrameTimeResult = frameTimeResult;
        return this;
    }

    public FrameScenario setPreAnim(boolean isPreAnim) {
        this.mIsPreAnim = isPreAnim;
        return this;
    }

    public FrameScenario setSFPEnable(boolean enabled) {
        this.mIsSFPEnable = enabled;
        return this;
    }

    public FrameScenario setPreAnimEnable(boolean enabled) {
        this.mIsPreAnimEnable = enabled;
        return this;
    }

    public boolean isListenFrameHint() {
        return this.mIsListenFrameHint;
    }

    public int getScenarioAction() {
        return this.mScenarioAction;
    }

    public int getBoostStatus() {
        return this.mBoostStatus;
    }

    public int getFrameStep() {
        return this.mFrameStep;
    }

    public long getFrameId() {
        return this.mFrameId;
    }

    public int getRenderThreadTid() {
        return this.mRenderThreadTid;
    }

    public ThreadedRenderer getThreadedRendererAndClear() {
        ThreadedRenderer render = this.mThreadedRenderer;
        this.mThreadedRenderer = null;
        return render;
    }

    public boolean isFling() {
        return this.mIsFling;
    }

    public Choreographer getChoreographer() {
        Choreographer tmpChoreographer = this.mChoreographer;
        this.mChoreographer = null;
        return tmpChoreographer;
    }

    public Choreographer.FrameData getFrameData() {
        return this.mFrameData;
    }

    public long getOrigFrameTime() {
        return this.mOrigFrameTimeNano;
    }

    public long getFrameTimeResult() {
        return this.mFrameTimeResult;
    }

    public boolean isPreAnim() {
        return this.mIsPreAnim;
    }

    public boolean isSFPEnable() {
        return this.mIsSFPEnable;
    }

    public boolean isPreAnimEnable() {
        return this.mIsPreAnimEnable;
    }
}
