package com.transsion.griffin.lib.app;

import android.content.pm.ApplicationInfo;
import android.os.SystemClock;
import android.util.ArraySet;
import com.transsion.griffin.lib.app.AppInfo;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public class ProcessWrapper {
    private long createTime = SystemClock.elapsedRealtime();
    private long lastPausedTime = -2;
    public final ProcessInfo processInfo;

    public ProcessWrapper(ProcessInfo processInfo) {
        this.processInfo = processInfo;
    }

    public String processName() {
        return "";
    }

    public int uid() {
        return -1;
    }

    public int pid() {
        return -1;
    }

    public ArrayList<String> pkgList() {
        return null;
    }

    public ArraySet<String> pkgDeps() {
        return null;
    }

    public boolean starting() {
        return false;
    }

    public int maxAdj() {
        return -10000;
    }

    public int setAdj() {
        return -10000;
    }

    public int setSchedGroup() {
        return 0;
    }

    public int setProcState() {
        return 19;
    }

    public boolean persistent() {
        return false;
    }

    public boolean serviceb() {
        return false;
    }

    public boolean serviceHighRam() {
        return false;
    }

    public boolean notCachedSinceIdle() {
        return true;
    }

    public boolean hasClientActivities() {
        return false;
    }

    public boolean hasStartedServices() {
        return false;
    }

    public boolean foregroundServices() {
        return false;
    }

    public boolean foregroundActivities() {
        return false;
    }

    public boolean systemNoUi() {
        return false;
    }

    public boolean hasShownUi() {
        return false;
    }

    public boolean hasTopUi() {
        return false;
    }

    public boolean hasOverlayUi() {
        return false;
    }

    public boolean hasAboveClient() {
        return false;
    }

    public boolean treatLikeActivity() {
        return false;
    }

    public boolean bad() {
        return false;
    }

    public boolean killed() {
        return false;
    }

    public boolean killedByAm() {
        return false;
    }

    public boolean unlocked() {
        return false;
    }

    public long interactionEventTime() {
        return 0L;
    }

    public long fgInteractionTime() {
        return 0L;
    }

    public long lastCpuTime() {
        return 0L;
    }

    public long curCpuTime() {
        return 0L;
    }

    public long lastRequestedGc() {
        return 0L;
    }

    public long lastLowMemory() {
        return 0L;
    }

    public long lastProviderTime() {
        return 0L;
    }

    public boolean reportLowMemory() {
        return false;
    }

    public boolean empty() {
        return false;
    }

    public boolean cached() {
        return false;
    }

    public boolean execServicesFg() {
        return false;
    }

    public String hostingType() {
        return "";
    }

    public String hostingNameStr() {
        return "";
    }

    public boolean isolated() {
        return false;
    }

    public ApplicationInfo info() {
        return null;
    }

    public long createTime() {
        return this.createTime;
    }

    public void setPaused() {
        this.lastPausedTime = SystemClock.elapsedRealtime();
    }

    public void setResumed() {
        this.lastPausedTime = -1L;
    }

    public long pausedTime() {
        return this.lastPausedTime;
    }

    public long lastPssTime() {
        return 0L;
    }

    public long lastPss() {
        return 0L;
    }

    public long lastSwapPss() {
        return 0L;
    }

    public long lastCachedPss() {
        return 0L;
    }

    public long lastCachedSwapPss() {
        return 0L;
    }

    public boolean launchingProvider() {
        return false;
    }

    public boolean hasActivities() {
        return false;
    }

    public boolean hasServices() {
        return false;
    }

    public boolean hasExecutingServices() {
        return false;
    }

    public boolean hasConnections() {
        return false;
    }

    public boolean hasReceivers() {
        return false;
    }

    public boolean hasPubProviders() {
        return false;
    }

    public boolean hasConProviders() {
        return false;
    }

    public List<ProcessWrapper> getServiceClients() {
        return null;
    }

    public List<ProcessWrapper> getProviderClients() {
        return null;
    }

    public List<ProcessWrapper> getConnectServices() {
        return null;
    }

    public List<ProcessWrapper> getConnectProviders() {
        return null;
    }

    public List<AppInfo.Activity> activities() {
        return null;
    }

    public List<AppInfo.Service> services() {
        return null;
    }

    public List<AppInfo.Service> executingServices() {
        return null;
    }

    public List<AppInfo> connections() {
        return null;
    }

    public List<AppInfo.Receiver> receivers() {
        return null;
    }

    public List<AppInfo.Provider> pubProviders() {
        return null;
    }

    public List<AppInfo.Provider> conProviders() {
        return null;
    }

    public int preloadState() {
        return 0;
    }
}
