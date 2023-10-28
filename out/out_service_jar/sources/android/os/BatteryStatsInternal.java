package android.os;

import com.android.internal.os.BinderCallsStats;
import com.android.internal.os.SystemServerCpuThreadReader;
import java.util.Collection;
import java.util.List;
/* loaded from: classes.dex */
public abstract class BatteryStatsInternal {
    public abstract List<BatteryUsageStats> getBatteryUsageStats(List<BatteryUsageStatsQuery> list);

    public abstract String[] getMobileIfaces();

    public abstract SystemServerCpuThreadReader.SystemServiceCpuThreadTimes getSystemServiceCpuThreadTimes();

    public abstract String[] getWifiIfaces();

    public abstract void noteBinderCallStats(int i, long j, Collection<BinderCallsStats.CallStat> collection);

    public abstract void noteBinderThreadNativeIds(int[] iArr);

    public abstract void noteJobsDeferred(int i, int i2, long j);
}
