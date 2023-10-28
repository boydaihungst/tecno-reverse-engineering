package com.android.server.job.controllers;

import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class Package {
    public final String packageName;
    public final int userId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public Package(int userId, String packageName) {
        this.userId = userId;
        this.packageName = packageName;
    }

    public String toString() {
        return packageToString(this.userId, this.packageName);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Package) {
            Package other = (Package) obj;
            return this.userId == other.userId && Objects.equals(this.packageName, other.packageName);
        }
        return false;
    }

    public int hashCode() {
        return this.packageName.hashCode() + this.userId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String packageToString(int userId, String packageName) {
        return "<" + userId + ">" + packageName;
    }
}
