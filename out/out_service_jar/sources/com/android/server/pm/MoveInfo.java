package com.android.server.pm;
/* loaded from: classes2.dex */
final class MoveInfo {
    final int mAppId;
    final String mFromCodePath;
    final String mFromUuid;
    final int mMoveId;
    final String mPackageName;
    final String mSeInfo;
    final int mTargetSdkVersion;
    final String mToUuid;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MoveInfo(int moveId, String fromUuid, String toUuid, String packageName, int appId, String seInfo, int targetSdkVersion, String fromCodePath) {
        this.mMoveId = moveId;
        this.mFromUuid = fromUuid;
        this.mToUuid = toUuid;
        this.mPackageName = packageName;
        this.mAppId = appId;
        this.mSeInfo = seInfo;
        this.mTargetSdkVersion = targetSdkVersion;
        this.mFromCodePath = fromCodePath;
    }
}
