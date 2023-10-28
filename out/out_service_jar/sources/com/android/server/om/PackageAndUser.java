package com.android.server.om;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class PackageAndUser {
    public final String packageName;
    public final int userId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageAndUser(String packageName, int userId) {
        this.packageName = packageName;
        this.userId = userId;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PackageAndUser) {
            PackageAndUser other = (PackageAndUser) obj;
            return this.packageName.equals(other.packageName) && this.userId == other.userId;
        }
        return false;
    }

    public int hashCode() {
        int result = (1 * 31) + this.packageName.hashCode();
        return (result * 31) + this.userId;
    }

    public String toString() {
        return String.format("PackageAndUser{packageName=%s, userId=%d}", this.packageName, Integer.valueOf(this.userId));
    }
}
