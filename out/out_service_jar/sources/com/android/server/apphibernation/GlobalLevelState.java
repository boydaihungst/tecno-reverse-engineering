package com.android.server.apphibernation;

import java.text.SimpleDateFormat;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class GlobalLevelState {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public boolean hibernated;
    public long lastUnhibernatedMs;
    public String packageName;
    public long savedByte;

    public String toString() {
        return "GlobalLevelState{packageName='" + this.packageName + "', hibernated=" + this.hibernated + "', savedByte=" + this.savedByte + "', lastUnhibernated=" + DATE_FORMAT.format(Long.valueOf(this.lastUnhibernatedMs)) + '}';
    }
}
