package com.android.server.sensorprivacy;

import android.app.AppOpsManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.hardware.lights.LightsManager;
import android.hardware.lights.LightsRequest;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.SystemClock;
import android.permission.PermissionManager;
import android.util.ArraySet;
import android.util.Pair;
import com.android.server.FgThread;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
/* loaded from: classes2.dex */
public class CameraPrivacyLightController implements AppOpsManager.OnOpActiveChangedListener, SensorEventListener {
    static final double LIGHT_VALUE_MULTIPLIER = 1.0d / Math.log(1.1d);
    private final Set<String> mActivePackages;
    private final Set<String> mActivePhonePackages;
    private long mAlvSum;
    private final ArrayDeque<Pair<Long, Integer>> mAmbientLightValues;
    private final AppOpsManager mAppOpsManager;
    private final List<Light> mCameraLights;
    private final Context mContext;
    private final int mDayColor;
    private final Object mDelayedUpdateToken;
    private long mElapsedRealTime;
    private long mElapsedTimeStartedReading;
    private final Executor mExecutor;
    private final Handler mHandler;
    private boolean mIsAmbientLightListenerRegistered;
    private int mLastLightColor;
    private final Sensor mLightSensor;
    private final LightsManager mLightsManager;
    private LightsManager.LightsSession mLightsSession;
    private final long mMovingAverageIntervalMillis;
    private final int mNightColor;
    private final long mNightThreshold;
    private final SensorManager mSensorManager;

    public CameraPrivacyLightController(Context context) {
        this(context, FgThread.get().getLooper());
    }

    CameraPrivacyLightController(Context context, Looper looper) {
        this.mActivePackages = new ArraySet();
        this.mActivePhonePackages = new ArraySet();
        this.mCameraLights = new ArrayList();
        this.mLightsSession = null;
        this.mIsAmbientLightListenerRegistered = false;
        this.mAmbientLightValues = new ArrayDeque<>();
        this.mAlvSum = 0L;
        this.mLastLightColor = 0;
        this.mDelayedUpdateToken = new Object();
        this.mElapsedRealTime = -1L;
        this.mContext = context;
        Handler handler = new Handler(looper);
        this.mHandler = handler;
        this.mExecutor = new HandlerExecutor(handler);
        this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        LightsManager lightsManager = (LightsManager) context.getSystemService(LightsManager.class);
        this.mLightsManager = lightsManager;
        this.mSensorManager = (SensorManager) context.getSystemService(SensorManager.class);
        this.mDayColor = context.getColor(17170609);
        this.mNightColor = context.getColor(17170610);
        this.mMovingAverageIntervalMillis = context.getResources().getInteger(17694766);
        this.mNightThreshold = (long) (Math.log(context.getResources().getInteger(17694767)) * LIGHT_VALUE_MULTIPLIER);
        List<Light> lights = lightsManager.getLights();
        for (int i = 0; i < lights.size(); i++) {
            Light light = lights.get(i);
            if (light.getType() == 9) {
                this.mCameraLights.add(light);
            }
        }
        if (this.mCameraLights.isEmpty()) {
            this.mLightSensor = null;
            return;
        }
        this.mAppOpsManager.startWatchingActive(new String[]{"android:camera", "android:phone_call_camera"}, this.mExecutor, this);
        this.mLightSensor = this.mSensorManager.getDefaultSensor(5);
    }

    private void addElement(long time, int value) {
        if (this.mAmbientLightValues.isEmpty()) {
            this.mAmbientLightValues.add(new Pair<>(Long.valueOf((time - getCurrentIntervalMillis()) - 1), Integer.valueOf(value)));
        }
        Pair<Long, Integer> lastElement = this.mAmbientLightValues.peekLast();
        this.mAmbientLightValues.add(new Pair<>(Long.valueOf(time), Integer.valueOf(value)));
        this.mAlvSum += (time - ((Long) lastElement.first).longValue()) * ((Integer) lastElement.second).intValue();
        removeObsoleteData(time);
    }

    private void removeObsoleteData(long time) {
        while (this.mAmbientLightValues.size() > 1) {
            Pair<Long, Integer> element0 = this.mAmbientLightValues.pollFirst();
            Pair<Long, Integer> element1 = this.mAmbientLightValues.peekFirst();
            if (((Long) element1.first).longValue() > time - getCurrentIntervalMillis()) {
                this.mAmbientLightValues.addFirst(element0);
                return;
            }
            this.mAlvSum -= (((Long) element1.first).longValue() - ((Long) element0.first).longValue()) * ((Integer) element0.second).intValue();
        }
    }

    private long getLiveAmbientLightTotal() {
        if (this.mAmbientLightValues.isEmpty()) {
            return this.mAlvSum;
        }
        long time = getElapsedRealTime();
        removeObsoleteData(time);
        Pair<Long, Integer> firstElement = this.mAmbientLightValues.peekFirst();
        Pair<Long, Integer> lastElement = this.mAmbientLightValues.peekLast();
        return (this.mAlvSum - (Math.max(0L, (time - getCurrentIntervalMillis()) - ((Long) firstElement.first).longValue()) * ((Integer) firstElement.second).intValue())) + ((time - ((Long) lastElement.first).longValue()) * ((Integer) lastElement.second).intValue());
    }

    @Override // android.app.AppOpsManager.OnOpActiveChangedListener
    public void onOpActiveChanged(String op, int uid, String packageName, boolean active) {
        Set<String> activePackages;
        if ("android:camera".equals(op)) {
            activePackages = this.mActivePackages;
        } else if ("android:phone_call_camera".equals(op)) {
            activePackages = this.mActivePhonePackages;
        } else {
            return;
        }
        if (active) {
            activePackages.add(packageName);
        } else {
            activePackages.remove(packageName);
        }
        updateLightSession();
    }

    public void updateLightSession() {
        int lightColor;
        if (Looper.myLooper() != this.mHandler.getLooper()) {
            this.mHandler.post(new CameraPrivacyLightController$$ExternalSyntheticLambda0(this));
            return;
        }
        Set<String> exemptedPackages = PermissionManager.getIndicatorExemptedPackages(this.mContext);
        boolean shouldSessionEnd = exemptedPackages.containsAll(this.mActivePackages) && exemptedPackages.containsAll(this.mActivePhonePackages);
        updateSensorListener(shouldSessionEnd);
        if (shouldSessionEnd) {
            LightsManager.LightsSession lightsSession = this.mLightsSession;
            if (lightsSession == null) {
                return;
            }
            lightsSession.close();
            this.mLightsSession = null;
            return;
        }
        if (this.mLightSensor != null && getLiveAmbientLightTotal() < getCurrentIntervalMillis() * this.mNightThreshold) {
            lightColor = this.mNightColor;
        } else {
            lightColor = this.mDayColor;
        }
        if (this.mLastLightColor == lightColor && this.mLightsSession != null) {
            return;
        }
        this.mLastLightColor = lightColor;
        LightsRequest.Builder requestBuilder = new LightsRequest.Builder();
        for (int i = 0; i < this.mCameraLights.size(); i++) {
            requestBuilder.addLight(this.mCameraLights.get(i), new LightState.Builder().setColor(lightColor).build());
        }
        if (this.mLightsSession == null) {
            this.mLightsSession = this.mLightsManager.openSession(Integer.MAX_VALUE);
        }
        this.mLightsSession.requestLights(requestBuilder.build());
    }

    private void updateSensorListener(boolean shouldSessionEnd) {
        Sensor sensor;
        if (shouldSessionEnd && this.mIsAmbientLightListenerRegistered) {
            this.mSensorManager.unregisterListener(this);
            this.mIsAmbientLightListenerRegistered = false;
        }
        if (!shouldSessionEnd && !this.mIsAmbientLightListenerRegistered && (sensor = this.mLightSensor) != null) {
            this.mSensorManager.registerListener(this, sensor, 3, this.mHandler);
            this.mIsAmbientLightListenerRegistered = true;
            this.mElapsedTimeStartedReading = getElapsedRealTime();
        }
    }

    private long getElapsedRealTime() {
        long j = this.mElapsedRealTime;
        return j == -1 ? SystemClock.elapsedRealtime() : j;
    }

    void setElapsedRealTime(long time) {
        this.mElapsedRealTime = time;
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent event) {
        addElement(event.timestamp, Math.max(0, (int) (Math.log(event.values[0]) * LIGHT_VALUE_MULTIPLIER)));
        updateLightSession();
        this.mHandler.removeCallbacksAndMessages(this.mDelayedUpdateToken);
        this.mHandler.postDelayed(new CameraPrivacyLightController$$ExternalSyntheticLambda0(this), this.mDelayedUpdateToken, this.mMovingAverageIntervalMillis);
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private long getCurrentIntervalMillis() {
        return Math.min(this.mMovingAverageIntervalMillis, getElapsedRealTime() - this.mElapsedTimeStartedReading);
    }
}
