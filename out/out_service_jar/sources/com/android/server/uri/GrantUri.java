package com.android.server.uri;

import android.content.ContentProvider;
import android.net.Uri;
import android.util.proto.ProtoOutputStream;
import com.android.server.wm.ActivityTaskManagerInternal;
/* loaded from: classes2.dex */
public class GrantUri {
    public final boolean prefix;
    public final int sourceUserId;
    public final Uri uri;

    public GrantUri(int sourceUserId, Uri uri, int modeFlags) {
        this.sourceUserId = sourceUserId;
        this.uri = uri;
        this.prefix = (modeFlags & 128) != 0;
    }

    public int hashCode() {
        int hashCode = (1 * 31) + this.sourceUserId;
        return (((hashCode * 31) + this.uri.hashCode()) * 31) + (this.prefix ? 1231 : 1237);
    }

    public boolean equals(Object o) {
        if (o instanceof GrantUri) {
            GrantUri other = (GrantUri) o;
            return this.uri.equals(other.uri) && this.sourceUserId == other.sourceUserId && this.prefix == other.prefix;
        }
        return false;
    }

    public String toString() {
        String result = this.uri.toString() + " [user " + this.sourceUserId + "]";
        return this.prefix ? result + " [prefix]" : result;
    }

    public String toSafeString() {
        String result = this.uri.toSafeString() + " [user " + this.sourceUserId + "]";
        return this.prefix ? result + " [prefix]" : result;
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1138166333442L, this.uri.toString());
        proto.write(CompanionMessage.MESSAGE_ID, this.sourceUserId);
        proto.end(token);
    }

    public static GrantUri resolve(int defaultSourceUserHandle, Uri uri, int modeFlags) {
        if (ActivityTaskManagerInternal.ASSIST_KEY_CONTENT.equals(uri.getScheme())) {
            return new GrantUri(ContentProvider.getUserIdFromUri(uri, defaultSourceUserHandle), ContentProvider.getUriWithoutUserId(uri), modeFlags);
        }
        return new GrantUri(defaultSourceUserHandle, uri, modeFlags);
    }
}
