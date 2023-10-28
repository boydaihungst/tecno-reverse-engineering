package com.android.server.pm;
/* loaded from: classes2.dex */
public final class ApexSystemServiceInfo implements Comparable<ApexSystemServiceInfo> {
    final int mInitOrder;
    final String mJarPath;
    final String mName;

    public ApexSystemServiceInfo(String name, String jarPath, int initOrder) {
        this.mName = name;
        this.mJarPath = jarPath;
        this.mInitOrder = initOrder;
    }

    public String getName() {
        return this.mName;
    }

    public String getJarPath() {
        return this.mJarPath;
    }

    public int getInitOrder() {
        return this.mInitOrder;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // java.lang.Comparable
    public int compareTo(ApexSystemServiceInfo other) {
        int i = this.mInitOrder;
        int i2 = other.mInitOrder;
        if (i == i2) {
            return this.mName.compareTo(other.mName);
        }
        return -Integer.compare(i, i2);
    }
}
