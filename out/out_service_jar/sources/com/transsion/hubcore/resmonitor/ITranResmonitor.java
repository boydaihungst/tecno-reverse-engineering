package com.transsion.hubcore.resmonitor;

import android.os.WorkSource;
import com.transsion.hubcore.resmonitor.ITranResmonitor;
import com.transsion.hubcore.utils.TranClassInfo;
import java.io.File;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranResmonitor {
    public static final TranClassInfo<ITranResmonitor> classInfo = new TranClassInfo<>("com.transsion.hubcore.resmonitor.TranResmonitorImpl", ITranResmonitor.class, new Supplier() { // from class: com.transsion.hubcore.resmonitor.ITranResmonitor$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranResmonitor.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranResmonitor {
    }

    static ITranResmonitor Instance() {
        return (ITranResmonitor) classInfo.getImpl();
    }

    default void noteScreenStateLocked(int state) {
    }

    default void noteVideoStateLocked(boolean running) {
    }

    default void noteAudioStateLocked(boolean running) {
    }

    default void shutdown() {
    }

    default void noteBatteryState(int plugType, int level, int temp, int volt, int chargeUAh) {
    }

    default void setSystemDir(File systemDir) {
    }

    default void notePhoneStateLocked(boolean running) {
    }

    default void noteCameraStateLocked(boolean running) {
    }

    default void noteGpsChanged(WorkSource oldWs, WorkSource newWs) {
    }

    default void noteWifiState(int wifiState) {
    }

    default void noteUidProcessState(int uid, int state) {
    }

    default void recordPss(int pid, int uid, String processName, int adj, long pss) {
    }
}
