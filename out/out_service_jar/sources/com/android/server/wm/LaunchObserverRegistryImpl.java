package com.android.server.wm;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import java.util.ArrayList;
import java.util.function.BiConsumer;
/* loaded from: classes2.dex */
class LaunchObserverRegistryImpl extends ActivityMetricsLaunchObserver implements ActivityMetricsLaunchObserverRegistry {
    private final Handler mHandler;
    private final ArrayList<ActivityMetricsLaunchObserver> mList = new ArrayList<>();

    public LaunchObserverRegistryImpl(Looper looper) {
        this.mHandler = new Handler(looper);
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserverRegistry
    public void registerLaunchObserver(ActivityMetricsLaunchObserver launchObserver) {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.LaunchObserverRegistryImpl$$ExternalSyntheticLambda3
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((LaunchObserverRegistryImpl) obj).handleRegisterLaunchObserver((ActivityMetricsLaunchObserver) obj2);
            }
        }, this, launchObserver));
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserverRegistry
    public void unregisterLaunchObserver(ActivityMetricsLaunchObserver launchObserver) {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.LaunchObserverRegistryImpl$$ExternalSyntheticLambda5
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((LaunchObserverRegistryImpl) obj).handleUnregisterLaunchObserver((ActivityMetricsLaunchObserver) obj2);
            }
        }, this, launchObserver));
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserver
    public void onIntentStarted(Intent intent, long timestampNs) {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.wm.LaunchObserverRegistryImpl$$ExternalSyntheticLambda7
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((LaunchObserverRegistryImpl) obj).handleOnIntentStarted((Intent) obj2, ((Long) obj3).longValue());
            }
        }, this, intent, Long.valueOf(timestampNs)));
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserver
    public void onIntentFailed(long id) {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.LaunchObserverRegistryImpl$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((LaunchObserverRegistryImpl) obj).handleOnIntentFailed(((Long) obj2).longValue());
            }
        }, this, Long.valueOf(id)));
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserver
    public void onActivityLaunched(long id, ComponentName name, int temperature) {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuadConsumer() { // from class: com.android.server.wm.LaunchObserverRegistryImpl$$ExternalSyntheticLambda4
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                ((LaunchObserverRegistryImpl) obj).handleOnActivityLaunched(((Long) obj2).longValue(), (ComponentName) obj3, ((Integer) obj4).intValue());
            }
        }, this, Long.valueOf(id), name, Integer.valueOf(temperature)));
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserver
    public void onActivityLaunchCancelled(long id) {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.LaunchObserverRegistryImpl$$ExternalSyntheticLambda6
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((LaunchObserverRegistryImpl) obj).handleOnActivityLaunchCancelled(((Long) obj2).longValue());
            }
        }, this, Long.valueOf(id)));
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserver
    public void onActivityLaunchFinished(long id, ComponentName name, long timestampNs) {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuadConsumer() { // from class: com.android.server.wm.LaunchObserverRegistryImpl$$ExternalSyntheticLambda1
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                ((LaunchObserverRegistryImpl) obj).handleOnActivityLaunchFinished(((Long) obj2).longValue(), (ComponentName) obj3, ((Long) obj4).longValue());
            }
        }, this, Long.valueOf(id), name, Long.valueOf(timestampNs)));
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserver
    public void onReportFullyDrawn(long id, long timestampNs) {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.wm.LaunchObserverRegistryImpl$$ExternalSyntheticLambda0
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((LaunchObserverRegistryImpl) obj).handleOnReportFullyDrawn(((Long) obj2).longValue(), ((Long) obj3).longValue());
            }
        }, this, Long.valueOf(id), Long.valueOf(timestampNs)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRegisterLaunchObserver(ActivityMetricsLaunchObserver observer) {
        this.mList.add(observer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUnregisterLaunchObserver(ActivityMetricsLaunchObserver observer) {
        this.mList.remove(observer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnIntentStarted(Intent intent, long timestampNs) {
        for (int i = 0; i < this.mList.size(); i++) {
            this.mList.get(i).onIntentStarted(intent, timestampNs);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnIntentFailed(long id) {
        for (int i = 0; i < this.mList.size(); i++) {
            this.mList.get(i).onIntentFailed(id);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnActivityLaunched(long id, ComponentName name, int temperature) {
        for (int i = 0; i < this.mList.size(); i++) {
            this.mList.get(i).onActivityLaunched(id, name, temperature);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnActivityLaunchCancelled(long id) {
        for (int i = 0; i < this.mList.size(); i++) {
            this.mList.get(i).onActivityLaunchCancelled(id);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnActivityLaunchFinished(long id, ComponentName name, long timestampNs) {
        for (int i = 0; i < this.mList.size(); i++) {
            this.mList.get(i).onActivityLaunchFinished(id, name, timestampNs);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnReportFullyDrawn(long id, long timestampNs) {
        for (int i = 0; i < this.mList.size(); i++) {
            this.mList.get(i).onReportFullyDrawn(id, timestampNs);
        }
    }
}
