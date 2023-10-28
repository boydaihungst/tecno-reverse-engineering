package com.transsion.hubcore.server.am;

import android.content.Context;
import com.android.server.am.ProcessList;
import com.android.server.am.ProcessRecord;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import com.transsion.hubcore.server.am.ITranProcessList;
import com.transsion.hubcore.utils.TranClassInfo;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranProcessList {
    public static final int SCHED_GROUP_BACKGROUND = 0;
    public static final int SCHED_GROUP_DEFAULT = 2;
    public static final int SCHED_GROUP_RESTRICTED = 1;
    public static final int SCHED_GROUP_TOP_APP = 3;
    public static final int SCHED_GROUP_TOP_APP_BOUND = 4;
    public static final TranClassInfo<ITranProcessList> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranProcessListImpl", ITranProcessList.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranProcessList$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranProcessList.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranProcessList {
    }

    static ITranProcessList Instance() {
        return (ITranProcessList) classInfo.getImpl();
    }

    default void onConstruct(String tag, Context context) {
    }

    default void addToMuteProcessList(ProcessList processList, ArrayList<ProcessRecord> lruProcessesLOSP, int pid, String packageName, int multiWindowId) {
    }

    default void removeFromMuteProcessList(ProcessList processList, ArrayList<ProcessRecord> lruProcessesLOSP, String packageName, int pid, int multiWindowId) {
    }

    default void clearMuteProcessList(ProcessList processList, int multiWindowId) {
    }

    default void inMuteProcessList(ProcessList processList, String packageName, int pid, int uid) {
    }

    default boolean isNoneEmptyMuteProcessList(ProcessList processList) {
        return false;
    }

    default long getCachedRestoreThresholdKbForGo(long level) {
        return level;
    }

    default void addGidForDualApp(ArrayList<Integer> gidList, int uid) {
    }

    default void dumpMuteList(Map<Integer, ArrayList<String>> mMuteList, PrintWriter pw) {
    }

    default void hookProcStart(TranProcessWrapper processWrapper, String arg1, String arg2) {
    }

    default void hookUidChangedRunning(int uid) {
    }

    default void hookUidChangedStoped(int uid) {
    }

    default int setMaxOomAdj(ProcessRecord r, TranProcessWrapper processWrapper, int origMaxAdj) {
        return origMaxAdj;
    }

    default void do_AOT_multikill() {
    }
}
