package com.android.server.apphibernation;

import java.text.SimpleDateFormat;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class UserLevelState {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public boolean hibernated;
    public long lastUnhibernatedMs;
    public String packageName;
    public long savedByte;

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserLevelState() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserLevelState(UserLevelState state) {
        this.packageName = state.packageName;
        this.hibernated = state.hibernated;
        this.savedByte = state.savedByte;
        this.lastUnhibernatedMs = state.lastUnhibernatedMs;
    }

    public String toString() {
        return "UserLevelState{packageName='" + this.packageName + "', hibernated=" + this.hibernated + "', savedByte=" + this.savedByte + "', lastUnhibernated=" + DATE_FORMAT.format(Long.valueOf(this.lastUnhibernatedMs)) + '}';
    }
}
