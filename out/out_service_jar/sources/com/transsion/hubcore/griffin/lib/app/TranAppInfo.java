package com.transsion.hubcore.griffin.lib.app;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import com.android.server.slice.SliceClientPermissions;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
/* loaded from: classes2.dex */
public final class TranAppInfo {
    public final ApplicationInfo aInfo;
    public final Map<String, Activity> activities;
    public final boolean coreApp;
    public final long firstInstallTime;
    public final long lastUpdatedTime;
    public final PackageInfo pInfo;
    public final String packageName;
    public final String processName;
    public final Set<String> processNameSet = new HashSet();
    public final Map<String, Provider> providers;
    public final Map<String, Receiver> receivers;
    public final Map<String, Service> services;
    public final String sharedUserId;
    public boolean showInLauncher;
    public final int uid;
    public final int versionCode;
    public final String versionName;

    public TranAppInfo(PackageInfo packageInfo) {
        this.pInfo = packageInfo;
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        this.aInfo = applicationInfo;
        this.uid = applicationInfo.uid;
        this.sharedUserId = packageInfo.sharedUserId;
        this.processName = applicationInfo.processName;
        this.versionName = packageInfo.versionName;
        this.versionCode = packageInfo.versionCode;
        this.packageName = packageInfo.packageName;
        this.coreApp = packageInfo.coreApp;
        this.firstInstallTime = packageInfo.firstInstallTime;
        this.lastUpdatedTime = packageInfo.lastUpdateTime;
        this.showInLauncher = false;
        this.activities = new HashMap();
        this.receivers = new HashMap();
        this.services = new HashMap();
        this.providers = new HashMap();
    }

    public String toString() {
        return "AppInfo{pInfo=" + this.pInfo + ", aInfo=" + this.aInfo + ", uid=" + this.uid + ", sharedUserId='" + this.sharedUserId + "', processName='" + this.processName + "', processNameSet=" + this.processNameSet + ", versionName='" + this.versionName + "', versionCode=" + this.versionCode + ", packageName='" + this.packageName + "', coreApp=" + this.coreApp + ", firstInstallTime=" + this.firstInstallTime + ", lastUpdatedTime=" + this.lastUpdatedTime + ", activities=" + this.activities + ", receivers=" + this.receivers + ", services=" + this.services + ", providers=" + this.providers + '}';
    }

    public String toBaseInfo() {
        return this.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + this.uid + ",version:" + this.versionName;
    }

    /* loaded from: classes2.dex */
    public static final class Activity {
        public final ActivityInfo activity;
        public final TranAppInfo appInfo;
        public final String className;
        public Map<String, TranIntentInfo> intentsByAction;
        public final String packageName;
        public final String processName;

        public Activity(ActivityInfo aInfo, TranAppInfo app) {
            this.appInfo = app;
            this.activity = aInfo;
            String str = aInfo.packageName;
            this.packageName = str;
            this.className = aInfo.name;
            if (aInfo.processName != null) {
                this.processName = aInfo.processName;
            } else {
                this.processName = str;
            }
            this.intentsByAction = new HashMap();
        }
    }

    /* loaded from: classes2.dex */
    public static final class Receiver {
        public final TranAppInfo appInfo;
        public final String className;
        public Map<String, TranIntentInfo> intentsByAction;
        public final String packageName;
        public final String processName;
        public final ActivityInfo receiver;

        public Receiver(ActivityInfo aInfo, TranAppInfo app) {
            this.appInfo = app;
            this.receiver = aInfo;
            String str = aInfo.packageName;
            this.packageName = str;
            this.className = aInfo.name;
            if (aInfo.processName != null) {
                this.processName = aInfo.processName;
            } else {
                this.processName = str;
            }
            this.intentsByAction = new HashMap();
        }
    }

    /* loaded from: classes2.dex */
    public static final class Provider {
        public final TranAppInfo appInfo;
        public final String authority;
        public final String className;
        public final boolean multiprocess;
        public final String packageName;
        public final String processName;
        public final ProviderInfo provider;
        public final String readPermission;
        public final String writePermission;

        public Provider(ProviderInfo pInfo, TranAppInfo app) {
            this.appInfo = app;
            this.provider = pInfo;
            String str = pInfo.packageName;
            this.packageName = str;
            this.className = pInfo.name;
            this.authority = pInfo.authority;
            this.multiprocess = pInfo.multiprocess;
            if (pInfo.processName != null) {
                this.processName = pInfo.processName;
            } else {
                this.processName = str;
            }
            this.readPermission = pInfo.readPermission;
            this.writePermission = pInfo.writePermission;
        }
    }

    /* loaded from: classes2.dex */
    public static final class Service {
        public final TranAppInfo appInfo;
        public final String className;
        public Map<String, TranIntentInfo> intentsByAction;
        public final String packageName;
        public final String permission;
        public final String processName;
        public final ServiceInfo service;

        public Service(ServiceInfo sInfo, TranAppInfo app) {
            this.appInfo = app;
            this.service = sInfo;
            String str = sInfo.packageName;
            this.packageName = str;
            this.className = sInfo.name;
            if (sInfo.processName != null) {
                this.processName = sInfo.processName;
            } else {
                this.processName = str;
            }
            this.permission = sInfo.permission;
            this.intentsByAction = new HashMap();
        }
    }
}
