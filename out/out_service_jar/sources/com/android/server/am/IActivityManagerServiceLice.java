package com.android.server.am;

import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.IBinder;
import com.android.server.am.IActivityManagerServiceLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.List;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes.dex */
public interface IActivityManagerServiceLice {
    public static final LiceInfo<IActivityManagerServiceLice> sLiceInfo = new LiceInfo<>("com.transsion.server.am.ActivityManagerServiceLice", IActivityManagerServiceLice.class, new Supplier() { // from class: com.android.server.am.IActivityManagerServiceLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IActivityManagerServiceLice.DefaultImpl();
        }
    });

    /* loaded from: classes.dex */
    public static class DefaultImpl implements IActivityManagerServiceLice {
    }

    static IActivityManagerServiceLice Instance() {
        return (IActivityManagerServiceLice) sLiceInfo.getImpl();
    }

    default void onInstallSystemProviders(Context context) {
    }

    default Integer sendIntentSender(IIntentSender target, IBinder whitelistToken, int code, Intent intent, String resolvedType, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options, int userId) {
        return null;
    }

    default void onMakeActive(Object processRecord) {
    }

    default void onMakeInactive(Object processRecord) {
    }

    default void onConstruct(Context context) {
    }

    default void onWakefulnessChanged(boolean isAwake) {
    }

    default void onBootCompleted() {
    }

    default void onFinishBooting(Context context) {
    }

    default void onProcessRecordKill(Object app, int reason, int subReason, String msg) {
    }

    default void onAudioVolumeChange(int streamType, int volume, int device, String packageName) {
    }

    default void onClipboardChange(String packageName) {
    }

    default void onAMSShutdown(int timeout) {
    }

    default void onUserSwitching(int oldUserId, int newUserId) {
    }

    default void onUserSwitchComplete(int newUserId) {
    }

    default void onForegroundProfileSwitch(int newProfileId) {
    }

    default void onLockedBootComplete(int newUserId) {
    }

    default void onProcessesReady() {
    }

    default boolean checkBroadcastFromSystem(Intent intent, ProcessRecord callerApp, String callerPackage, int callingUid, boolean isProtectedBroadcast, List receivers) {
        return false;
    }

    default int startProcessLocked(int policy, ApplicationInfo appInfo) {
        return policy;
    }

    default void onAttachApplicationLocked(Context context, ApplicationInfo appInfo, Bundle coreSettings) {
    }
}
