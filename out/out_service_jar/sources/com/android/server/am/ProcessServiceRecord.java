package com.android.server.am;

import android.os.IBinder;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.transsion.hubcore.server.am.ITranProcessServiceRecord;
import java.io.PrintWriter;
import java.util.ArrayList;
/* loaded from: classes.dex */
public final class ProcessServiceRecord {
    boolean mAllowlistManager;
    final ProcessRecord mApp;
    private int mConnectionGroup;
    private int mConnectionImportance;
    private ServiceRecord mConnectionService;
    private boolean mExecServicesFg;
    private int mFgServiceTypes;
    private boolean mHasAboveClient;
    private boolean mHasClientActivities;
    private boolean mHasForegroundServices;
    private boolean mHasTopStartedAlmostPerceptibleServices;
    private long mLastTopStartedAlmostPerceptibleBindRequestUptimeMs;
    private int mRepFgServiceTypes;
    private boolean mRepHasForegroundServices;
    private final ActivityManagerService mService;
    private boolean mTreatLikeActivity;
    final ArraySet<ServiceRecord> mServices = new ArraySet<>();
    private final ArraySet<ServiceRecord> mExecutingServices = new ArraySet<>();
    private final ArraySet<ConnectionRecord> mConnections = new ArraySet<>();
    private ArraySet<Integer> mBoundClientUids = new ArraySet<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessServiceRecord(ProcessRecord app) {
        this.mApp = app;
        this.mService = app.mService;
        ITranProcessServiceRecord.instance().hookConstructor();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHasClientActivities(boolean hasClientActivities) {
        this.mHasClientActivities = hasClientActivities;
        this.mApp.getWindowProcessController().setHasClientActivities(hasClientActivities);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasClientActivities() {
        return this.mHasClientActivities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHasForegroundServices(boolean hasForegroundServices, int fgServiceTypes) {
        ITranProcessServiceRecord.instance().hookSetHasForegroundServices(this.mApp, hasForegroundServices, fgServiceTypes);
        this.mHasForegroundServices = hasForegroundServices;
        this.mFgServiceTypes = fgServiceTypes;
        this.mApp.getWindowProcessController().setHasForegroundServices(hasForegroundServices);
        if (hasForegroundServices) {
            this.mApp.mProfile.addHostingComponentType(256);
        } else {
            this.mApp.mProfile.clearHostingComponentType(256);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasForegroundServices() {
        return this.mHasForegroundServices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHasReportedForegroundServices(boolean hasForegroundServices) {
        this.mRepHasForegroundServices = hasForegroundServices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasReportedForegroundServices() {
        return this.mRepHasForegroundServices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getForegroundServiceTypes() {
        if (this.mHasForegroundServices) {
            return this.mFgServiceTypes;
        }
        return 0;
    }

    int getReportedForegroundServiceTypes() {
        return this.mRepFgServiceTypes;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReportedForegroundServiceTypes(int foregroundServiceTypes) {
        this.mRepFgServiceTypes = foregroundServiceTypes;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateHasTopStartedAlmostPerceptibleServices() {
        this.mHasTopStartedAlmostPerceptibleServices = false;
        this.mLastTopStartedAlmostPerceptibleBindRequestUptimeMs = 0L;
        for (int s = this.mServices.size() - 1; s >= 0; s--) {
            ServiceRecord sr = this.mServices.valueAt(s);
            this.mLastTopStartedAlmostPerceptibleBindRequestUptimeMs = Math.max(this.mLastTopStartedAlmostPerceptibleBindRequestUptimeMs, sr.lastTopAlmostPerceptibleBindRequestUptimeMs);
            if (!this.mHasTopStartedAlmostPerceptibleServices && isAlmostPerceptible(sr)) {
                this.mHasTopStartedAlmostPerceptibleServices = true;
            }
        }
    }

    private boolean isAlmostPerceptible(ServiceRecord record) {
        if (record.lastTopAlmostPerceptibleBindRequestUptimeMs <= 0) {
            return false;
        }
        ArrayMap<IBinder, ArrayList<ConnectionRecord>> serviceConnections = record.getConnections();
        for (int m = serviceConnections.size() - 1; m >= 0; m--) {
            ArrayList<ConnectionRecord> clist = serviceConnections.valueAt(m);
            for (int c = clist.size() - 1; c >= 0; c--) {
                ConnectionRecord cr = clist.get(c);
                if ((cr.flags & 65536) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasTopStartedAlmostPerceptibleServices() {
        return this.mHasTopStartedAlmostPerceptibleServices || (this.mLastTopStartedAlmostPerceptibleBindRequestUptimeMs > 0 && SystemClock.uptimeMillis() - this.mLastTopStartedAlmostPerceptibleBindRequestUptimeMs < this.mService.mConstants.mServiceBindAlmostPerceptibleTimeoutMs);
    }

    ServiceRecord getConnectionService() {
        return this.mConnectionService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setConnectionService(ServiceRecord connectionService) {
        this.mConnectionService = connectionService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getConnectionGroup() {
        return this.mConnectionGroup;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setConnectionGroup(int connectionGroup) {
        this.mConnectionGroup = connectionGroup;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getConnectionImportance() {
        return this.mConnectionImportance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setConnectionImportance(int connectionImportance) {
        this.mConnectionImportance = connectionImportance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateHasAboveClientLocked() {
        this.mHasAboveClient = false;
        for (int i = this.mConnections.size() - 1; i >= 0; i--) {
            ConnectionRecord cr = this.mConnections.valueAt(i);
            if ((cr.flags & 8) != 0) {
                this.mHasAboveClient = true;
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHasAboveClient(boolean hasAboveClient) {
        this.mHasAboveClient = hasAboveClient;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasAboveClient() {
        return this.mHasAboveClient;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int modifyRawOomAdj(int adj) {
        if (this.mHasAboveClient && adj >= 0) {
            if (adj < 100) {
                return 100;
            }
            if (adj < 200) {
                return 200;
            }
            if (adj < 250) {
                return 250;
            }
            if (adj < 900) {
                return 900;
            }
            if (adj < 999) {
                return adj + 1;
            }
            return adj;
        }
        return adj;
    }

    public boolean isTreatedLikeActivity() {
        return this.mTreatLikeActivity;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTreatLikeActivity(boolean treatLikeActivity) {
        this.mTreatLikeActivity = treatLikeActivity;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldExecServicesFg() {
        return this.mExecServicesFg;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setExecServicesFg(boolean execServicesFg) {
        this.mExecServicesFg = execServicesFg;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean startService(ServiceRecord record) {
        if (record == null) {
            return false;
        }
        boolean added = this.mServices.add(record);
        if (this.mServices.size() > 100) {
            Slog.d("ProcessServiceRecord", "service size = " + this.mServices.size() + ", this = " + this);
        }
        if (added && record.serviceInfo != null) {
            this.mApp.getWindowProcessController().onServiceStarted(record.serviceInfo);
            updateHostingComonentTypeForBindingsLocked();
        }
        if (record.lastTopAlmostPerceptibleBindRequestUptimeMs > 0) {
            this.mLastTopStartedAlmostPerceptibleBindRequestUptimeMs = Math.max(this.mLastTopStartedAlmostPerceptibleBindRequestUptimeMs, record.lastTopAlmostPerceptibleBindRequestUptimeMs);
            if (!this.mHasTopStartedAlmostPerceptibleServices) {
                this.mHasTopStartedAlmostPerceptibleServices = isAlmostPerceptible(record);
            }
        }
        return added;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean stopService(ServiceRecord record) {
        boolean removed = this.mServices.remove(record);
        if (record.lastTopAlmostPerceptibleBindRequestUptimeMs > 0) {
            updateHasTopStartedAlmostPerceptibleServices();
        }
        if (removed) {
            updateHostingComonentTypeForBindingsLocked();
        }
        return removed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopAllServices() {
        this.mServices.clear();
        updateHasTopStartedAlmostPerceptibleServices();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int numberOfRunningServices() {
        return this.mServices.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ServiceRecord getRunningServiceAt(int index) {
        return this.mServices.valueAt(index);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startExecutingService(ServiceRecord service) {
        this.mExecutingServices.add(service);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopExecutingService(ServiceRecord service) {
        this.mExecutingServices.remove(service);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopAllExecutingServices() {
        this.mExecutingServices.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ServiceRecord getExecutingServiceAt(int index) {
        return this.mExecutingServices.valueAt(index);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int numberOfExecutingServices() {
        return this.mExecutingServices.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addConnection(ConnectionRecord connection) {
        this.mConnections.add(connection);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeConnection(ConnectionRecord connection) {
        this.mConnections.remove(connection);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAllConnections() {
        this.mConnections.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ConnectionRecord getConnectionAt(int index) {
        return this.mConnections.valueAt(index);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int numberOfConnections() {
        return this.mConnections.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addBoundClientUid(int clientUid) {
        this.mBoundClientUids.add(Integer.valueOf(clientUid));
        this.mApp.getWindowProcessController().setBoundClientUids(this.mBoundClientUids);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateBoundClientUids() {
        if (this.mServices.isEmpty()) {
            clearBoundClientUids();
            return;
        }
        ArraySet<Integer> boundClientUids = new ArraySet<>();
        int serviceCount = this.mServices.size();
        for (int j = 0; j < serviceCount; j++) {
            ArrayMap<IBinder, ArrayList<ConnectionRecord>> conns = this.mServices.valueAt(j).getConnections();
            int size = conns.size();
            for (int conni = 0; conni < size; conni++) {
                ArrayList<ConnectionRecord> c = conns.valueAt(conni);
                for (int i = 0; i < c.size(); i++) {
                    boundClientUids.add(Integer.valueOf(c.get(i).clientUid));
                }
            }
        }
        this.mBoundClientUids = boundClientUids;
        this.mApp.getWindowProcessController().setBoundClientUids(this.mBoundClientUids);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addBoundClientUidsOfNewService(ServiceRecord sr) {
        if (sr == null) {
            return;
        }
        ArrayMap<IBinder, ArrayList<ConnectionRecord>> conns = sr.getConnections();
        for (int conni = conns.size() - 1; conni >= 0; conni--) {
            ArrayList<ConnectionRecord> c = conns.valueAt(conni);
            for (int i = 0; i < c.size(); i++) {
                this.mBoundClientUids.add(Integer.valueOf(c.get(i).clientUid));
            }
        }
        this.mApp.getWindowProcessController().setBoundClientUids(this.mBoundClientUids);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearBoundClientUids() {
        this.mBoundClientUids.clear();
        this.mApp.getWindowProcessController().setBoundClientUids(this.mBoundClientUids);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateHostingComonentTypeForBindingsLocked() {
        boolean hasBoundClient = false;
        int i = numberOfRunningServices() - 1;
        while (true) {
            if (i >= 0) {
                ServiceRecord sr = getRunningServiceAt(i);
                if (sr == null || sr.getConnections().isEmpty()) {
                    i--;
                } else {
                    hasBoundClient = true;
                    break;
                }
            } else {
                break;
            }
        }
        if (hasBoundClient) {
            this.mApp.mProfile.addHostingComponentType(512);
        } else {
            this.mApp.mProfile.clearHostingComponentType(512);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean incServiceCrashCountLocked(long now) {
        boolean procIsBoundForeground = this.mApp.mState.getCurProcState() == 5;
        boolean tryAgain = false;
        for (int i = numberOfRunningServices() - 1; i >= 0; i--) {
            ServiceRecord sr = getRunningServiceAt(i);
            if (now > sr.restartTime + ActivityManagerConstants.MIN_CRASH_INTERVAL) {
                sr.crashCount = 1;
            } else {
                sr.crashCount++;
            }
            if (sr.crashCount < this.mService.mConstants.BOUND_SERVICE_MAX_CRASH_RETRY && (sr.isForeground || procIsBoundForeground)) {
                tryAgain = true;
            }
        }
        return tryAgain;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCleanupApplicationRecordLocked() {
        this.mTreatLikeActivity = false;
        this.mHasAboveClient = false;
        setHasClientActivities(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix, long nowUptime) {
        if (this.mHasForegroundServices || this.mApp.mState.getForcingToImportant() != null) {
            pw.print(prefix);
            pw.print("mHasForegroundServices=");
            pw.print(this.mHasForegroundServices);
            pw.print(" forcingToImportant=");
            pw.println(this.mApp.mState.getForcingToImportant());
        }
        if (this.mHasTopStartedAlmostPerceptibleServices || this.mLastTopStartedAlmostPerceptibleBindRequestUptimeMs > 0) {
            pw.print(prefix);
            pw.print("mHasTopStartedAlmostPerceptibleServices=");
            pw.print(this.mHasTopStartedAlmostPerceptibleServices);
            pw.print(" mLastTopStartedAlmostPerceptibleBindRequestUptimeMs=");
            pw.println(this.mLastTopStartedAlmostPerceptibleBindRequestUptimeMs);
        }
        if (this.mHasClientActivities || this.mHasAboveClient || this.mTreatLikeActivity) {
            pw.print(prefix);
            pw.print("hasClientActivities=");
            pw.print(this.mHasClientActivities);
            pw.print(" hasAboveClient=");
            pw.print(this.mHasAboveClient);
            pw.print(" treatLikeActivity=");
            pw.println(this.mTreatLikeActivity);
        }
        if (this.mConnectionService != null || this.mConnectionGroup != 0) {
            pw.print(prefix);
            pw.print("connectionGroup=");
            pw.print(this.mConnectionGroup);
            pw.print(" Importance=");
            pw.print(this.mConnectionImportance);
            pw.print(" Service=");
            pw.println(this.mConnectionService);
        }
        if (this.mAllowlistManager) {
            pw.print(prefix);
            pw.print("allowlistManager=");
            pw.println(this.mAllowlistManager);
        }
        if (this.mServices.size() > 0) {
            pw.print(prefix);
            pw.println("Services:");
            int size = this.mServices.size();
            for (int i = 0; i < size; i++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.mServices.valueAt(i));
            }
        }
        if (this.mExecutingServices.size() > 0) {
            pw.print(prefix);
            pw.print("Executing Services (fg=");
            pw.print(this.mExecServicesFg);
            pw.println(")");
            int size2 = this.mExecutingServices.size();
            for (int i2 = 0; i2 < size2; i2++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.mExecutingServices.valueAt(i2));
            }
        }
        if (this.mConnections.size() > 0) {
            pw.print(prefix);
            pw.println("mConnections:");
            int size3 = this.mConnections.size();
            for (int i3 = 0; i3 < size3; i3++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.mConnections.valueAt(i3));
            }
        }
    }
}
