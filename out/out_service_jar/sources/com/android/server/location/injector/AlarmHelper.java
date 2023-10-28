package com.android.server.location.injector;

import android.app.AlarmManager;
import android.os.WorkSource;
import com.android.internal.util.Preconditions;
/* loaded from: classes.dex */
public abstract class AlarmHelper {
    public abstract void cancel(AlarmManager.OnAlarmListener onAlarmListener);

    protected abstract void setDelayedAlarmInternal(long j, AlarmManager.OnAlarmListener onAlarmListener, WorkSource workSource);

    public final void setDelayedAlarm(long delayMs, AlarmManager.OnAlarmListener listener, WorkSource workSource) {
        Preconditions.checkArgument(delayMs > 0);
        setDelayedAlarmInternal(delayMs, listener, workSource);
    }
}
