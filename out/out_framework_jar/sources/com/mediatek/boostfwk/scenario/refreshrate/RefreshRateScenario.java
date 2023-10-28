package com.mediatek.boostfwk.scenario.refreshrate;

import android.content.Context;
import com.mediatek.boostfwk.scenario.BasicScenario;
/* loaded from: classes4.dex */
public class RefreshRateScenario extends BasicScenario {
    private String mActivityName;
    private Context mContext;
    private double mCurrentDistance;
    private float mCurrentDistanceLower;
    private int mCurrentFlingState;
    private int mCurrentRefreshRate;
    private long mCurrentTime;
    private float mCurrentVelocity;
    private float mDistanceCoef;
    private float mFlingFriction;
    private int mFlingRefreshRateChangeIndex;
    private Object mObject;
    private float[] mOriginalSplinePosition;
    private float[] mOriginalSplineTime;
    private float mPhysicalCoeff;
    private String mPkgName;
    private float[] mRealSplinePosition;
    private float[] mRealSplineTime;
    private int mScenarioAction;
    private int mScenarioId;
    private int mSplineDistance;
    private int mSplineDuration;
    private double mSplineFlingDistance;
    private boolean mIsSmoothFlingEnabled = false;
    private boolean mIsVariableRefreshRateEnabled = false;
    private boolean mIsTouchScrollEnable = true;
    private boolean mHasVideo = false;
    private boolean mIsRefreshRateChangeEnabledWhenFling = false;
    private boolean mIsListScrollStateListening = false;
    private long mLastFlingFinishTime = 0;
    private long mLastTouchDownTime = 0;

    public RefreshRateScenario(Context context) {
        this.mContext = context;
        this.mScenario = 6;
    }

    public RefreshRateScenario(int scenario, int action, String packageName, Context context) {
        this.mScenario = scenario;
        this.mScenarioAction = action;
        this.mContext = context;
        this.mPkgName = packageName;
    }

    public RefreshRateScenario(int scenario, int action, Context context) {
        this.mScenario = scenario;
        this.mScenarioAction = action;
        this.mContext = context;
    }

    public Context getScenarioContext() {
        return this.mContext;
    }

    public Object getScenarioObj() {
        return this.mObject;
    }

    public RefreshRateScenario setVariableRefreshRateEnabled(boolean isVariableRefreshRateEnabled) {
        this.mIsVariableRefreshRateEnabled = isVariableRefreshRateEnabled;
        return this;
    }

    public boolean getVariableRefreshRateEnabled() {
        return this.mIsVariableRefreshRateEnabled;
    }

    public RefreshRateScenario setCurrentVelocity(float currentVelocity) {
        this.mCurrentVelocity = currentVelocity;
        return this;
    }

    public float getCurrentVelocity() {
        return this.mCurrentVelocity;
    }

    public RefreshRateScenario setScenarioAction(int scenarioAction) {
        this.mScenarioAction = scenarioAction;
        return this;
    }

    public int getScenarioAction() {
        return this.mScenarioAction;
    }

    public RefreshRateScenario setCurrentDistance(double currentDistance) {
        this.mCurrentDistance = currentDistance;
        return this;
    }

    public double getCurrentDistance() {
        return this.mCurrentDistance;
    }

    public RefreshRateScenario setCurrentTime(long currentTime) {
        this.mCurrentTime = currentTime;
        return this;
    }

    public long getCurrentTime() {
        return this.mCurrentTime;
    }

    public RefreshRateScenario setCurrentDistanceLower(float distanceResult) {
        this.mCurrentDistanceLower = Math.abs(distanceResult);
        return this;
    }

    public float getCurrentDistanceLower() {
        return this.mCurrentDistanceLower;
    }

    public RefreshRateScenario setCurrentRefreshrate(int currentRefreshRate) {
        this.mCurrentRefreshRate = Math.abs(currentRefreshRate);
        return this;
    }

    public int getCurrentRefreshrate() {
        return this.mCurrentRefreshRate;
    }

    public RefreshRateScenario setSplineDuration(int splineDuration) {
        this.mSplineDuration = splineDuration;
        return this;
    }

    public int getSplineDuration() {
        return this.mSplineDuration;
    }

    public RefreshRateScenario setSplineFlingDistance(double splineFlingDistance) {
        this.mSplineFlingDistance = splineFlingDistance;
        return this;
    }

    public double getSplineFlingDistance() {
        return this.mSplineFlingDistance;
    }

    public RefreshRateScenario setSplineDistance(int splineDistance) {
        this.mSplineDistance = splineDistance;
        return this;
    }

    public int getSplineDistance() {
        return this.mSplineDistance;
    }

    public RefreshRateScenario setFlingFriction(float flingFriction) {
        this.mFlingFriction = flingFriction;
        return this;
    }

    public float getFlingFriction() {
        return this.mFlingFriction;
    }

    public RefreshRateScenario setPhysicalCoeff(float flingFriction) {
        this.mPhysicalCoeff = flingFriction;
        return this;
    }

    public float getPhysicalCoeff() {
        return this.mPhysicalCoeff;
    }

    public RefreshRateScenario setOriginalSplinePosition(float[] splinePosition) {
        this.mOriginalSplinePosition = splinePosition;
        return this;
    }

    public float[] getOriginalSplinePosition() {
        return this.mOriginalSplinePosition;
    }

    public RefreshRateScenario setOriginalSplineTime(float[] splineTime) {
        this.mOriginalSplineTime = splineTime;
        return this;
    }

    public float[] getOriginalSplineTime() {
        return this.mOriginalSplineTime;
    }

    public RefreshRateScenario setRealSplinePosition(float[] realSplinePosition) {
        this.mRealSplinePosition = realSplinePosition;
        return this;
    }

    public float[] getRealSplinePosition() {
        return this.mRealSplinePosition;
    }

    public RefreshRateScenario setRealSplineTime(float[] realSplineTime) {
        this.mRealSplineTime = realSplineTime;
        return this;
    }

    public float[] getRealSplineTime() {
        return this.mRealSplineTime;
    }

    public RefreshRateScenario setSmoothFlingEnabled(boolean isSmoothFlingEnabled) {
        this.mIsSmoothFlingEnabled = isSmoothFlingEnabled;
        return this;
    }

    public boolean getSmoothFlingEnabled() {
        return this.mIsSmoothFlingEnabled;
    }

    public RefreshRateScenario setTouchScrollEnabled(boolean isTouchScrollEnabled) {
        this.mIsTouchScrollEnable = isTouchScrollEnabled;
        return this;
    }

    public boolean getTouchScrollEnabled() {
        return this.mIsTouchScrollEnable;
    }

    public int getFlingRefreshRateChangeIndex() {
        return this.mFlingRefreshRateChangeIndex;
    }

    public RefreshRateScenario setFlingRefreshRateChangeIndex(int flingRefreshRateChangeIndex) {
        this.mFlingRefreshRateChangeIndex = flingRefreshRateChangeIndex;
        return this;
    }

    public RefreshRateScenario increaseFlingRefreshRateChangeIndex() {
        this.mFlingRefreshRateChangeIndex++;
        return this;
    }

    public int getCurrentFlingState() {
        return this.mCurrentFlingState;
    }

    public RefreshRateScenario setCurrentFlingState(int currentFlingState) {
        this.mCurrentFlingState = currentFlingState;
        return this;
    }

    public RefreshRateScenario setRefreshRateChangeEnabledWhenFling(boolean isRefreshRateChangeEnabledWhenFling) {
        this.mIsRefreshRateChangeEnabledWhenFling = isRefreshRateChangeEnabledWhenFling;
        return this;
    }

    public boolean getRefreshRateChangeEnabledWhenFling() {
        return this.mIsRefreshRateChangeEnabledWhenFling;
    }

    public RefreshRateScenario setListScrollStateListening(boolean isListScrollStateListening) {
        this.mIsListScrollStateListening = isListScrollStateListening;
        return this;
    }

    public boolean getListScrollStateListening() {
        return this.mIsListScrollStateListening;
    }

    public RefreshRateScenario setScenarioID(int id) {
        this.mScenarioId = id;
        return this;
    }

    public int getScenarioID() {
        return this.mScenarioId;
    }

    public RefreshRateScenario setLastFlingFinishTime(long lastFlingFinishTime) {
        this.mLastFlingFinishTime = lastFlingFinishTime;
        return this;
    }

    public long getLastFlingFinishTime() {
        return this.mLastFlingFinishTime;
    }

    public RefreshRateScenario setLastTouchDownTime(long lastTouchDownTime) {
        this.mLastTouchDownTime = lastTouchDownTime;
        return this;
    }

    public long getLastTouchDownTime() {
        return this.mLastTouchDownTime;
    }

    public RefreshRateScenario setDistanceCoef(float distanceCoef) {
        this.mDistanceCoef = distanceCoef;
        return this;
    }

    public float getDistanceCoef() {
        return this.mDistanceCoef;
    }

    public RefreshRateScenario setHasVideo(boolean hasVideo) {
        this.mHasVideo = hasVideo;
        return this;
    }

    public boolean hasVideo() {
        return this.mHasVideo;
    }
}
