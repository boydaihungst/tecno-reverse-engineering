package com.transsion.hubcore.griffin.lib.app;

import android.content.pm.ApplicationInfo;
import com.transsion.hubcore.griffin.lib.app.TranAppInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
/* loaded from: classes2.dex */
public class TranProcessInfo {
    public final ApplicationInfo info;
    public final boolean persist;
    public final String processName;
    public final int uid;
    public Set<TranAppInfo> appInfoSet = new HashSet();
    public Map<String, TranAppInfo.Activity> activities = new HashMap();
    public Map<String, TranAppInfo.Receiver> receivers = new HashMap();
    public Map<String, TranAppInfo.Service> services = new HashMap();
    public Map<String, TranAppInfo.Provider> providers = new HashMap();

    public TranProcessInfo(String processName, int uid, ApplicationInfo applicationInfo) {
        this.info = applicationInfo;
        this.processName = processName;
        this.uid = uid;
        this.persist = (applicationInfo.flags & 8) != 0;
    }

    public void addAppInfo(TranAppInfo app) {
        if (app == null) {
            return;
        }
        for (TranAppInfo appInfo : this.appInfoSet) {
            if (appInfo.packageName.equals(app.packageName)) {
                return;
            }
        }
        this.appInfoSet.add(app);
        for (TranAppInfo.Activity activity : app.activities.values()) {
            if (activity.processName.equals(this.processName)) {
                this.activities.put(activity.className, activity);
            }
        }
        for (TranAppInfo.Receiver receiver : app.receivers.values()) {
            if (receiver.processName.equals(this.processName)) {
                this.receivers.put(receiver.className, receiver);
            }
        }
        for (TranAppInfo.Service service : app.services.values()) {
            if (service.processName.equals(this.processName)) {
                this.services.put(service.className, service);
            }
        }
        for (TranAppInfo.Provider provider : app.providers.values()) {
            if (provider.processName.equals(this.processName)) {
                this.providers.put(provider.className, provider);
            }
        }
    }

    public void removeAppInfo(TranAppInfo app) {
        if (app == null) {
            return;
        }
        boolean needRemove = false;
        Iterator<TranAppInfo> it = this.appInfoSet.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            TranAppInfo appInfo = it.next();
            if (appInfo.packageName.equals(app.packageName)) {
                this.appInfoSet.remove(appInfo);
                needRemove = true;
                break;
            }
        }
        if (needRemove) {
            for (TranAppInfo.Activity activity : app.activities.values()) {
                if (activity.processName.equals(this.processName)) {
                    this.activities.remove(activity.className);
                }
            }
            for (TranAppInfo.Receiver receiver : app.receivers.values()) {
                if (receiver.processName.equals(this.processName)) {
                    this.receivers.remove(receiver.className);
                }
            }
            for (TranAppInfo.Service service : app.services.values()) {
                if (service.processName.equals(this.processName)) {
                    this.services.remove(service.className);
                }
            }
            for (TranAppInfo.Provider provider : app.providers.values()) {
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
