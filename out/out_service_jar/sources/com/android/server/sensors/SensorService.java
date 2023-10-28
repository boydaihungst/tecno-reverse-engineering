package com.android.server.sensors;

import android.content.Context;
import android.util.ArrayMap;
import com.android.internal.util.ConcurrentUtils;
import com.android.server.LocalServices;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.SystemService;
import com.android.server.sensors.SensorManagerInternal;
import com.android.server.sensors.SensorService;
import com.android.server.utils.TimingsTraceAndSlog;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
/* loaded from: classes2.dex */
public class SensorService extends SystemService {
    private static final String START_NATIVE_SENSOR_SERVICE = "StartNativeSensorService";
    private final Object mLock;
    private final ArrayMap<SensorManagerInternal.ProximityActiveListener, ProximityListenerProxy> mProximityListeners;
    private long mPtr;
    private Future<?> mSensorServiceStart;

    /* JADX INFO: Access modifiers changed from: private */
    public static native void registerProximityActiveListenerNative(long j);

    private static native long startSensorServiceNative(SensorManagerInternal.ProximityActiveListener proximityActiveListener);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void unregisterProximityActiveListenerNative(long j);

    public SensorService(Context ctx) {
        super(ctx);
        Object obj = new Object();
        this.mLock = obj;
        this.mProximityListeners = new ArrayMap<>();
        synchronized (obj) {
            this.mSensorServiceStart = SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.sensors.SensorService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SensorService.this.m6474lambda$new$0$comandroidserversensorsSensorService();
                }
            }, START_NATIVE_SENSOR_SERVICE);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-sensors-SensorService  reason: not valid java name */
    public /* synthetic */ void m6474lambda$new$0$comandroidserversensorsSensorService() {
        TimingsTraceAndSlog traceLog = TimingsTraceAndSlog.newAsyncLog();
        traceLog.traceBegin(START_NATIVE_SENSOR_SERVICE);
        long ptr = startSensorServiceNative(new ProximityListenerDelegate());
        synchronized (this.mLock) {
            this.mPtr = ptr;
        }
        traceLog.traceEnd();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        LocalServices.addService(SensorManagerInternal.class, new LocalService());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 200) {
            ConcurrentUtils.waitForFutureNoInterrupt(this.mSensorServiceStart, START_NATIVE_SENSOR_SERVICE);
            synchronized (this.mLock) {
                this.mSensorServiceStart = null;
            }
        }
    }

    /* loaded from: classes2.dex */
    class LocalService extends SensorManagerInternal {
        LocalService() {
        }

        @Override // com.android.server.sensors.SensorManagerInternal
        public void addProximityActiveListener(Executor executor, SensorManagerInternal.ProximityActiveListener listener) {
            Objects.requireNonNull(executor, "executor must not be null");
            Objects.requireNonNull(listener, "listener must not be null");
            ProximityListenerProxy proxy = new ProximityListenerProxy(executor, listener);
            synchronized (SensorService.this.mLock) {
                if (SensorService.this.mProximityListeners.containsKey(listener)) {
                    throw new IllegalArgumentException("listener already registered");
                }
                SensorService.this.mProximityListeners.put(listener, proxy);
                if (SensorService.this.mProximityListeners.size() == 1) {
                    SensorService.registerProximityActiveListenerNative(SensorService.this.mPtr);
                }
            }
        }

        @Override // com.android.server.sensors.SensorManagerInternal
        public void removeProximityActiveListener(SensorManagerInternal.ProximityActiveListener listener) {
            Objects.requireNonNull(listener, "listener must not be null");
            synchronized (SensorService.this.mLock) {
                ProximityListenerProxy proxy = (ProximityListenerProxy) SensorService.this.mProximityListeners.remove(listener);
                if (proxy == null) {
                    throw new IllegalArgumentException("listener was not registered with sensor service");
                }
                if (SensorService.this.mProximityListeners.isEmpty()) {
                    SensorService.unregisterProximityActiveListenerNative(SensorService.this.mPtr);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ProximityListenerProxy implements SensorManagerInternal.ProximityActiveListener {
        private final Executor mExecutor;
        private final SensorManagerInternal.ProximityActiveListener mListener;

        ProximityListenerProxy(Executor executor, SensorManagerInternal.ProximityActiveListener listener) {
            this.mExecutor = executor;
            this.mListener = listener;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onProximityActive$0$com-android-server-sensors-SensorService$ProximityListenerProxy  reason: not valid java name */
        public /* synthetic */ void m6475x2199d8c9(boolean isActive) {
            this.mListener.onProximityActive(isActive);
        }

        @Override // com.android.server.sensors.SensorManagerInternal.ProximityActiveListener
        public void onProximityActive(final boolean isActive) {
            this.mExecutor.execute(new Runnable() { // from class: com.android.server.sensors.SensorService$ProximityListenerProxy$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SensorService.ProximityListenerProxy.this.m6475x2199d8c9(isActive);
                }
            });
        }
    }

    /* loaded from: classes2.dex */
    private class ProximityListenerDelegate implements SensorManagerInternal.ProximityActiveListener {
        private ProximityListenerDelegate() {
        }

        @Override // com.android.server.sensors.SensorManagerInternal.ProximityActiveListener
        public void onProximityActive(boolean isActive) {
            int i;
            ProximityListenerProxy[] listeners;
            synchronized (SensorService.this.mLock) {
                listeners = (ProximityListenerProxy[]) SensorService.this.mProximityListeners.values().toArray(new ProximityListenerProxy[0]);
            }
            for (ProximityListenerProxy listener : listeners) {
                listener.onProximityActive(isActive);
            }
        }
    }
}
