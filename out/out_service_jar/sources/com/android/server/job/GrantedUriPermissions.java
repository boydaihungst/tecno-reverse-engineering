package com.android.server.job;

import android.app.UriGrantsManager;
import android.content.ClipData;
import android.content.ContentProvider;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.server.LocalServices;
import com.android.server.uri.UriGrantsManagerInternal;
import java.io.PrintWriter;
import java.util.ArrayList;
/* loaded from: classes.dex */
public final class GrantedUriPermissions {
    private final int mGrantFlags;
    private final IBinder mPermissionOwner;
    private final int mSourceUserId;
    private final String mTag;
    private final ArrayList<Uri> mUris = new ArrayList<>();

    private GrantedUriPermissions(int grantFlags, int uid, String tag) throws RemoteException {
        this.mGrantFlags = grantFlags;
        this.mSourceUserId = UserHandle.getUserId(uid);
        this.mTag = tag;
        this.mPermissionOwner = ((UriGrantsManagerInternal) LocalServices.getService(UriGrantsManagerInternal.class)).newUriPermissionOwner("job: " + tag);
    }

    public void revoke() {
        for (int i = this.mUris.size() - 1; i >= 0; i--) {
            ((UriGrantsManagerInternal) LocalServices.getService(UriGrantsManagerInternal.class)).revokeUriPermissionFromOwner(this.mPermissionOwner, this.mUris.get(i), this.mGrantFlags, this.mSourceUserId);
        }
        this.mUris.clear();
    }

    public static boolean checkGrantFlags(int grantFlags) {
        return (grantFlags & 3) != 0;
    }

    public static GrantedUriPermissions createFromIntent(Intent intent, int sourceUid, String targetPackage, int targetUserId, String tag) {
        int grantFlags = intent.getFlags();
        if (!checkGrantFlags(grantFlags)) {
            return null;
        }
        GrantedUriPermissions perms = null;
        Uri data = intent.getData();
        if (data != null) {
            perms = grantUri(data, sourceUid, targetPackage, targetUserId, grantFlags, tag, null);
        }
        ClipData clip = intent.getClipData();
        if (clip != null) {
            return grantClip(clip, sourceUid, targetPackage, targetUserId, grantFlags, tag, perms);
        }
        return perms;
    }

    public static GrantedUriPermissions createFromClip(ClipData clip, int sourceUid, String targetPackage, int targetUserId, int grantFlags, String tag) {
        if (!checkGrantFlags(grantFlags) || clip == null) {
            return null;
        }
        GrantedUriPermissions perms = grantClip(clip, sourceUid, targetPackage, targetUserId, grantFlags, tag, null);
        return perms;
    }

    private static GrantedUriPermissions grantClip(ClipData clip, int sourceUid, String targetPackage, int targetUserId, int grantFlags, String tag, GrantedUriPermissions curPerms) {
        int N = clip.getItemCount();
        for (int i = 0; i < N; i++) {
            curPerms = grantItem(clip.getItemAt(i), sourceUid, targetPackage, targetUserId, grantFlags, tag, curPerms);
        }
        return curPerms;
    }

    private static GrantedUriPermissions grantUri(Uri uri, int sourceUid, String targetPackage, int targetUserId, int grantFlags, String tag, GrantedUriPermissions curPerms) {
        try {
            int sourceUserId = ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid));
            Uri uri2 = ContentProvider.getUriWithoutUserId(uri);
            if (curPerms == null) {
                curPerms = new GrantedUriPermissions(grantFlags, sourceUid, tag);
            }
            UriGrantsManager.getService().grantUriPermissionFromOwner(curPerms.mPermissionOwner, sourceUid, targetPackage, uri2, grantFlags, sourceUserId, targetUserId);
            curPerms.mUris.add(uri2);
        } catch (RemoteException e) {
            Slog.e(JobSchedulerService.TAG, "AM dead");
        }
        return curPerms;
    }

    private static GrantedUriPermissions grantItem(ClipData.Item item, int sourceUid, String targetPackage, int targetUserId, int grantFlags, String tag, GrantedUriPermissions curPerms) {
        if (item.getUri() != null) {
            curPerms = grantUri(item.getUri(), sourceUid, targetPackage, targetUserId, grantFlags, tag, curPerms);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            return grantUri(intent.getData(), sourceUid, targetPackage, targetUserId, grantFlags, tag, curPerms);
        }
        return curPerms;
    }

    public void dump(PrintWriter pw) {
        pw.print("mGrantFlags=0x");
        pw.print(Integer.toHexString(this.mGrantFlags));
        pw.print(" mSourceUserId=");
        pw.println(this.mSourceUserId);
        pw.print("mTag=");
        pw.println(this.mTag);
        pw.print("mPermissionOwner=");
        pw.println(this.mPermissionOwner);
        for (int i = 0; i < this.mUris.size(); i++) {
            pw.print("#");
            pw.print(i);
            pw.print(": ");
            pw.println(this.mUris.get(i));
        }
    }

    public void dump(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, this.mGrantFlags);
        proto.write(1120986464258L, this.mSourceUserId);
        proto.write(1138166333443L, this.mTag);
        proto.write(1138166333444L, this.mPermissionOwner.toString());
        for (int i = 0; i < this.mUris.size(); i++) {
            Uri u = this.mUris.get(i);
            if (u != null) {
                proto.write(2237677961221L, u.toString());
            }
        }
        proto.end(token);
    }
}
