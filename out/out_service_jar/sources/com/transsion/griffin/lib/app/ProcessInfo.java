package com.transsion.griffin.lib.app;

import android.content.pm.ApplicationInfo;
import com.transsion.griffin.lib.app.AppInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
/* loaded from: classes2.dex */
public class ProcessInfo {
    public final ApplicationInfo info;
    public final boolean persist;
    public final String processName;
    public final int uid;
    public Set<AppInfo> appInfoSet = new HashSet();
    public Map<String, AppInfo.Activity> activities = new HashMap();
    public Map<String, AppInfo.Receiver> receivers = new HashMap();
    public Map<String, AppInfo.Service> services = new HashMap();
    public Map<String, AppInfo.Provider> providers = new HashMap();

    public ProcessInfo(String processName, int uid, ApplicationInfo applicationInfo) {
        this.info = applicationInfo;
        this.processName = processName;
        this.uid = uid;
        this.persist = (applicationInfo.flags & 8) != 0;
    }

    public void addAppInfo(AppInfo app) {
        if (app == null) {
            return;
        }
        for (AppInfo appInfo : this.appInfoSet) {
            if (appInfo.packageName.equals(app.packageName)) {
                return;
            }
        }
        this.appInfoSet.add(app);
        for (AppInfo.Activity activity : app.activities.values()) {
            if (activity.processName.equals(this.processName)) {
                this.activities.put(activity.className, activity);
            }
        }
        for (AppInfo.Receiver receiver : app.receivers.values()) {
            if (receiver.processName.equals(this.processName)) {
                this.receivers.put(receiver.className, receiver);
            }
        }
        for (AppInfo.Service service : app.services.values()) {
            if (service.processName.equals(this.processName)) {
                this.services.put(service.className, service);
            }
        }
        for (AppInfo.Provider provider : app.providers.values()) {
            if (provider.processName.equals(this.processName)) {
                this.providers.put(provider.className, provider);
            }
        }
    }

    public void removeAppInfo(AppInfo app) {
        if (app == null) {
            return;
        }
        boolean needRemove = false;
        Iterator<AppInfo> it = this.appInfoSet.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            AppInfo appInfo = it.next();
            if (appInfo.packageName.equals(app.packageName)) {
                this.appInfoSet.remove(appInfo);
                needRemove = true;
                break;
            }
        }
        if (needRemove) {
            for (AppInfo.Activity activity : app.activities.values()) {
                if (activity.processName.equals(this.processName)) {
                    this.activities.remove(activity.className);
                }
            }
            for (AppInfo.Receiver receiver : app.receivers.values()) {
                if (receiver.processName.equals(this.processName)) {
                    this.receivers.remove(receiver.className);
                }
            }
            for (AppInfo.Service service : app.services.values()) {
                if (service.processName.equals(this.processName)) {
                    this.services.remove(service.className);
                }
            }
            for (AppInfo.Provider provider : app.providers.values()) {
                if (provider.processName.equals(this.processName)) {
                    this.providers.remove(provider.className);
                }
            }
        }
    }

    public String toString() {
        return "ProcessInfo{info=" + this.info + ", processName='" + this.processName + "', uid=" + this.uid + ", persist=" + this.persist + ", activities=" + this.activities + ", receivers=" + this.receivers + ", services=" + this.services + ", providers=" + this.providers + '}';
    }
}
