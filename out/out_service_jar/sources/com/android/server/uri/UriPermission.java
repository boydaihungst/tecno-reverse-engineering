package com.android.server.uri;

import android.app.GrantedUriPermission;
import android.os.Binder;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.google.android.collect.Sets;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Iterator;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class UriPermission {
    static final long INVALID_TIME = Long.MIN_VALUE;
    public static final int STRENGTH_GLOBAL = 2;
    public static final int STRENGTH_NONE = 0;
    public static final int STRENGTH_OWNED = 1;
    public static final int STRENGTH_PERSISTABLE = 3;
    private static final String TAG = "UriPermission";
    private ArraySet<UriPermissionOwner> mReadOwners;
    private ArraySet<UriPermissionOwner> mWriteOwners;
    final String sourcePkg;
    private String stringName;
    final String targetPkg;
    final int targetUid;
    final int targetUserId;
    final GrantUri uri;
    int modeFlags = 0;
    int ownedModeFlags = 0;
    int globalModeFlags = 0;
    int persistableModeFlags = 0;
    int persistedModeFlags = 0;
    long persistedCreateTime = INVALID_TIME;

    /* JADX INFO: Access modifiers changed from: package-private */
    public UriPermission(String sourcePkg, String targetPkg, int targetUid, GrantUri uri) {
        this.targetUserId = UserHandle.getUserId(targetUid);
        this.sourcePkg = sourcePkg;
        this.targetPkg = targetPkg;
        this.targetUid = targetUid;
        this.uri = uri;
    }

    private void updateModeFlags() {
        int oldModeFlags = this.modeFlags;
        this.modeFlags = this.ownedModeFlags | this.globalModeFlags | this.persistedModeFlags;
        if (Log.isLoggable(TAG, 2) && this.modeFlags != oldModeFlags) {
            Slog.d(TAG, "Permission for " + this.targetPkg + " to " + this.uri + " is changing from 0x" + Integer.toHexString(oldModeFlags) + " to 0x" + Integer.toHexString(this.modeFlags) + " via calling UID " + Binder.getCallingUid() + " PID " + Binder.getCallingPid(), new Throwable());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initPersistedModes(int modeFlags, long createdTime) {
        int modeFlags2 = modeFlags & 3;
        this.persistableModeFlags = modeFlags2;
        this.persistedModeFlags = modeFlags2;
        this.persistedCreateTime = createdTime;
        updateModeFlags();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean grantModes(int modeFlags, UriPermissionOwner owner) {
        boolean persistable = (modeFlags & 64) != 0;
        int modeFlags2 = modeFlags & 3;
        if (persistable) {
            this.persistableModeFlags |= modeFlags2;
        }
        if (owner == null) {
            this.globalModeFlags |= modeFlags2;
        } else {
            if ((modeFlags2 & 1) != 0) {
                addReadOwner(owner);
            }
            if ((modeFlags2 & 2) != 0) {
                addWriteOwner(owner);
            }
        }
        updateModeFlags();
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean takePersistableModes(int modeFlags) {
        int modeFlags2 = modeFlags & 3;
        int i = this.persistableModeFlags;
        if ((modeFlags2 & i) != modeFlags2) {
            Slog.w(TAG, "Requested flags 0x" + Integer.toHexString(modeFlags2) + ", but only 0x" + Integer.toHexString(this.persistableModeFlags) + " are allowed");
            return false;
        }
        int before = this.persistedModeFlags;
        int i2 = (i & modeFlags2) | this.persistedModeFlags;
        this.persistedModeFlags = i2;
        if (i2 != 0) {
            this.persistedCreateTime = System.currentTimeMillis();
        }
        updateModeFlags();
        return this.persistedModeFlags != before;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean releasePersistableModes(int modeFlags) {
        int before = this.persistedModeFlags;
        int i = this.persistedModeFlags & (~(modeFlags & 3));
        this.persistedModeFlags = i;
        if (i == 0) {
            this.persistedCreateTime = INVALID_TIME;
        }
        updateModeFlags();
        return this.persistedModeFlags != before;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean revokeModes(int modeFlags, boolean includingOwners) {
        boolean persistable = (modeFlags & 64) != 0;
        int modeFlags2 = modeFlags & 3;
        int before = this.persistedModeFlags;
        if ((modeFlags2 & 1) != 0) {
            if (persistable) {
                this.persistableModeFlags &= -2;
                this.persistedModeFlags &= -2;
            }
            this.globalModeFlags &= -2;
            ArraySet<UriPermissionOwner> arraySet = this.mReadOwners;
            if (arraySet != null && includingOwners) {
                this.ownedModeFlags &= -2;
                Iterator<UriPermissionOwner> it = arraySet.iterator();
                while (it.hasNext()) {
                    UriPermissionOwner r = it.next();
                    r.removeReadPermission(this);
                }
                this.mReadOwners = null;
            }
        }
        if ((modeFlags2 & 2) != 0) {
            if (persistable) {
                this.persistableModeFlags &= -3;
                this.persistedModeFlags &= -3;
            }
            this.globalModeFlags &= -3;
            ArraySet<UriPermissionOwner> arraySet2 = this.mWriteOwners;
            if (arraySet2 != null && includingOwners) {
                this.ownedModeFlags &= -3;
                Iterator<UriPermissionOwner> it2 = arraySet2.iterator();
                while (it2.hasNext()) {
                    UriPermissionOwner r2 = it2.next();
                    r2.removeWritePermission(this);
                }
                this.mWriteOwners = null;
            }
        }
        if (this.persistedModeFlags == 0) {
            this.persistedCreateTime = INVALID_TIME;
        }
        updateModeFlags();
        return this.persistedModeFlags != before;
    }

    public int getStrength(int modeFlags) {
        int modeFlags2 = modeFlags & 3;
        if ((this.persistableModeFlags & modeFlags2) == modeFlags2) {
            return 3;
        }
        if ((this.globalModeFlags & modeFlags2) == modeFlags2) {
            return 2;
        }
        if ((this.ownedModeFlags & modeFlags2) == modeFlags2) {
            return 1;
        }
        return 0;
    }

    private void addReadOwner(UriPermissionOwner owner) {
        if (this.mReadOwners == null) {
            this.mReadOwners = Sets.newArraySet();
            this.ownedModeFlags |= 1;
            updateModeFlags();
        }
        if (this.mReadOwners.add(owner)) {
            owner.addReadPermission(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeReadOwner(UriPermissionOwner owner) {
        if (!this.mReadOwners.remove(owner)) {
            Slog.wtf(TAG, "Unknown read owner " + owner + " in " + this);
        }
        if (this.mReadOwners.size() == 0) {
            this.mReadOwners = null;
            this.ownedModeFlags &= -2;
            updateModeFlags();
        }
    }

    private void addWriteOwner(UriPermissionOwner owner) {
        if (this.mWriteOwners == null) {
            this.mWriteOwners = Sets.newArraySet();
            this.ownedModeFlags |= 2;
            updateModeFlags();
        }
        if (this.mWriteOwners.add(owner)) {
            owner.addWritePermission(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeWriteOwner(UriPermissionOwner owner) {
        if (!this.mWriteOwners.remove(owner)) {
            Slog.wtf(TAG, "Unknown write owner " + owner + " in " + this);
        }
        if (this.mWriteOwners.size() == 0) {
            this.mWriteOwners = null;
            this.ownedModeFlags &= -3;
            updateModeFlags();
        }
    }

    public String toString() {
        String str = this.stringName;
        if (str != null) {
            return str;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("UriPermission{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.uri);
        sb.append('}');
        String sb2 = sb.toString();
        this.stringName = sb2;
        return sb2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("targetUserId=" + this.targetUserId);
        pw.print(" sourcePkg=" + this.sourcePkg);
        pw.println(" targetPkg=" + this.targetPkg);
        pw.print(prefix);
        pw.print("mode=0x" + Integer.toHexString(this.modeFlags));
        pw.print(" owned=0x" + Integer.toHexString(this.ownedModeFlags));
        pw.print(" global=0x" + Integer.toHexString(this.globalModeFlags));
        pw.print(" persistable=0x" + Integer.toHexString(this.persistableModeFlags));
        pw.print(" persisted=0x" + Integer.toHexString(this.persistedModeFlags));
        if (this.persistedCreateTime != INVALID_TIME) {
            pw.print(" persistedCreate=" + this.persistedCreateTime);
        }
        pw.println();
        if (this.mReadOwners != null) {
            pw.print(prefix);
            pw.println("readOwners:");
            Iterator<UriPermissionOwner> it = this.mReadOwners.iterator();
            while (it.hasNext()) {
                UriPermissionOwner owner = it.next();
                pw.print(prefix);
                pw.println("  * " + owner);
            }
        }
        if (this.mWriteOwners != null) {
            pw.print(prefix);
            pw.println("writeOwners:");
            Iterator<UriPermissionOwner> it2 = this.mReadOwners.iterator();
            while (it2.hasNext()) {
                UriPermissionOwner owner2 = it2.next();
                pw.print(prefix);
                pw.println("  * " + owner2);
            }
        }
    }

    /* loaded from: classes2.dex */
    public static class PersistedTimeComparator implements Comparator<UriPermission> {
        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.Comparator
        public int compare(UriPermission lhs, UriPermission rhs) {
            return Long.compare(lhs.persistedCreateTime, rhs.persistedCreateTime);
        }
    }

    /* loaded from: classes2.dex */
    public static class Snapshot {
        final long persistedCreateTime;
        final int persistedModeFlags;
        final String sourcePkg;
        final String targetPkg;
        final int targetUserId;
        final GrantUri uri;

        private Snapshot(UriPermission perm) {
            this.targetUserId = perm.targetUserId;
            this.sourcePkg = perm.sourcePkg;
            this.targetPkg = perm.targetPkg;
            this.uri = perm.uri;
            this.persistedModeFlags = perm.persistedModeFlags;
            this.persistedCreateTime = perm.persistedCreateTime;
        }
    }

    public Snapshot snapshot() {
        return new Snapshot();
    }

    public android.content.UriPermission buildPersistedPublicApiObject() {
        return new android.content.UriPermission(this.uri.uri, this.persistedModeFlags, this.persistedCreateTime);
    }

    public GrantedUriPermission buildGrantedUriPermission() {
        return new GrantedUriPermission(this.uri.uri, this.targetPkg);
    }
}
