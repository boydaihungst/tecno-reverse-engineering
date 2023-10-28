package com.android.server.am;

import android.os.Binder;
import android.os.SystemClock;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.app.procstats.AssociationState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.server.slice.SliceClientPermissions;
/* loaded from: classes.dex */
public final class ContentProviderConnection extends Binder {
    public AssociationState.SourceState association;
    public final ProcessRecord client;
    public final String clientPackage;
    public boolean dead;
    final int mExpectedUserId;
    private int mNumStableIncs;
    private int mNumUnstableIncs;
    private Object mProcStatsLock;
    private int mStableCount;
    private int mUnstableCount;
    public final ContentProviderRecord provider;
    public boolean waiting;
    private final Object mLock = new Object();
    public final long createTime = SystemClock.elapsedRealtime();

    public ContentProviderConnection(ContentProviderRecord _provider, ProcessRecord _client, String _clientPackage, int _expectedUserId) {
        this.provider = _provider;
        this.client = _client;
        this.clientPackage = _clientPackage;
        this.mExpectedUserId = _expectedUserId;
    }

    public void startAssociationIfNeeded() {
        if (this.association == null && this.provider.proc != null) {
            if (this.provider.appInfo.uid != this.client.uid || !this.provider.info.processName.equals(this.client.processName)) {
                ProcessStats.ProcessStateHolder holder = this.provider.proc.getPkgList().get(this.provider.name.getPackageName());
                if (holder == null) {
                    Slog.wtf("ActivityManager", "No package in referenced provider " + this.provider.name.toShortString() + ": proc=" + this.provider.proc);
                } else if (holder.pkg == null) {
                    Slog.wtf("ActivityManager", "Inactive holder in referenced provider " + this.provider.name.toShortString() + ": proc=" + this.provider.proc);
                } else {
                    Object obj = this.provider.proc.mService.mProcessStats.mLock;
                    this.mProcStatsLock = obj;
                    synchronized (obj) {
                        this.association = holder.pkg.getAssociationStateLocked(holder.state, this.provider.name.getClassName()).startSource(this.client.uid, this.client.processName, this.clientPackage);
                    }
                }
            }
        }
    }

    public void trackProcState(int procState, int seq) {
        if (this.association != null) {
            synchronized (this.mProcStatsLock) {
                this.association.trackProcState(procState, seq, SystemClock.uptimeMillis());
            }
        }
    }

    public void stopAssociation() {
        if (this.association != null) {
            synchronized (this.mProcStatsLock) {
                this.association.stop();
            }
            this.association = null;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("ContentProviderConnection{");
        toShortString(sb);
        sb.append('}');
        return sb.toString();
    }

    public String toShortString() {
        StringBuilder sb = new StringBuilder(128);
        toShortString(sb);
        return sb.toString();
    }

    public String toClientString() {
        StringBuilder sb = new StringBuilder(128);
        toClientString(sb);
        return sb.toString();
    }

    public void toShortString(StringBuilder sb) {
        sb.append(this.provider.toShortString());
        sb.append("->");
        toClientString(sb);
    }

    public void toClientString(StringBuilder sb) {
        sb.append(this.client.toShortString());
        synchronized (this.mLock) {
            sb.append(" s");
            sb.append(this.mStableCount);
            sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
            sb.append(this.mNumStableIncs);
            sb.append(" u");
            sb.append(this.mUnstableCount);
            sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
            sb.append(this.mNumUnstableIncs);
        }
        if (this.waiting) {
            sb.append(" WAITING");
        }
        if (this.dead) {
            sb.append(" DEAD");
        }
        long nowReal = SystemClock.elapsedRealtime();
        sb.append(" ");
        TimeUtils.formatDuration(nowReal - this.createTime, sb);
    }

    public void initializeCount(boolean stable) {
        synchronized (this.mLock) {
            if (stable) {
                this.mStableCount = 1;
                this.mNumStableIncs = 1;
                this.mUnstableCount = 0;
                this.mNumUnstableIncs = 0;
            } else {
                this.mStableCount = 0;
                this.mNumStableIncs = 0;
                this.mUnstableCount = 1;
                this.mNumUnstableIncs = 1;
            }
        }
    }

    public int incrementCount(boolean stable) {
        int i;
        synchronized (this.mLock) {
            if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
                ContentProviderRecord cpr = this.provider;
                Slog.v("ActivityManager", "Adding provider requested by " + this.client.processName + " from process " + cpr.info.processName + ": " + cpr.name.flattenToShortString() + " scnt=" + this.mStableCount + " uscnt=" + this.mUnstableCount);
            }
            if (stable) {
                this.mStableCount++;
                this.mNumStableIncs++;
            } else {
                this.mUnstableCount++;
                this.mNumUnstableIncs++;
            }
            i = this.mStableCount + this.mUnstableCount;
        }
        return i;
    }

    public int decrementCount(boolean stable) {
        int i;
        synchronized (this.mLock) {
            if (ActivityManagerDebugConfig.DEBUG_PROVIDER) {
                ContentProviderRecord cpr = this.provider;
                Slog.v("ActivityManager", "Removing provider requested by " + this.client.processName + " from process " + cpr.info.processName + ": " + cpr.name.flattenToShortString() + " scnt=" + this.mStableCount + " uscnt=" + this.mUnstableCount);
            }
            if (stable) {
                this.mStableCount--;
            } else {
                this.mUnstableCount--;
            }
            i = this.mStableCount + this.mUnstableCount;
        }
        return i;
    }

    public void adjustCounts(int stableIncrement, int unstableIncrement) {
        synchronized (this.mLock) {
            if (stableIncrement > 0) {
                this.mNumStableIncs += stableIncrement;
            }
            int stable = this.mStableCount + stableIncrement;
            if (stable < 0) {
                throw new IllegalStateException("stableCount < 0: " + stable);
            }
            if (unstableIncrement > 0) {
                this.mNumUnstableIncs += unstableIncrement;
            }
            int unstable = this.mUnstableCount + unstableIncrement;
            if (unstable < 0) {
                throw new IllegalStateException("unstableCount < 0: " + unstable);
            }
            if (stable + unstable <= 0) {
                throw new IllegalStateException("ref counts can't go to zero here: stable=" + stable + " unstable=" + unstable);
            }
            this.mStableCount = stable;
            this.mUnstableCount = unstable;
        }
    }

    public int stableCount() {
        int i;
        synchronized (this.mLock) {
            i = this.mStableCount;
        }
        return i;
    }

    public int unstableCount() {
        int i;
        synchronized (this.mLock) {
            i = this.mUnstableCount;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int totalRefCount() {
        int i;
        synchronized (this.mLock) {
            i = this.mStableCount + this.mUnstableCount;
        }
        return i;
    }
}
