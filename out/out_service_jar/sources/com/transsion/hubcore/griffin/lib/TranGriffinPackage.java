package com.transsion.hubcore.griffin.lib;

import android.util.Pair;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.pkg.component.ParsedIntentInfo;
import com.android.server.pm.pkg.component.ParsedProvider;
import com.android.server.pm.pkg.component.ParsedService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public class TranGriffinPackage implements Serializable {
    private String packageName;
    private List<ParsedActivity> activities = new ArrayList();
    private List<ParsedActivity> receivers = new ArrayList();
    private List<ParsedService> services = new ArrayList();
    private List<ParsedProvider> providers = new ArrayList();
    private List<Pair<String, ParsedIntentInfo>> intentInfos = new ArrayList();

    public List<Pair<String, ParsedIntentInfo>> getIntentInfos() {
        return this.intentInfos;
    }

    public void setIntentInfos(List<Pair<String, ParsedIntentInfo>> intentInfos) {
        this.intentInfos = intentInfos;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<ParsedActivity> getActivities() {
        return this.activities;
    }

    public void setActivities(List<ParsedActivity> activities) {
        this.activities = activities;
    }

    public List<ParsedActivity> getReceivers() {
        return this.receivers;
    }

    public void setReceivers(List<ParsedActivity> receivers) {
        this.receivers = receivers;
    }

    public List<ParsedService> getServices() {
        return this.services;
    }

    public void setServices(List<ParsedService> services) {
        this.services = services;
    }

    public List<ParsedProvider> getProviders() {
        return this.providers;
    }

    public void setProviders(List<ParsedProvider> providers) {
        this.providers = providers;
    }
}
