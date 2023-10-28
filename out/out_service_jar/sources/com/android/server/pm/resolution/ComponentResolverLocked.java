package com.android.server.pm.resolution;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import com.android.server.pm.Computer;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerTracedLock;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.pkg.component.ParsedProvider;
import com.android.server.pm.pkg.component.ParsedService;
import java.io.PrintWriter;
import java.util.List;
/* loaded from: classes2.dex */
public abstract class ComponentResolverLocked extends ComponentResolverBase {
    protected final PackageManagerTracedLock mLock;

    /* JADX INFO: Access modifiers changed from: protected */
    public ComponentResolverLocked(UserManagerService userManager) {
        super(userManager);
        this.mLock = new PackageManagerTracedLock();
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public boolean componentExists(ComponentName componentName) {
        boolean componentExists;
        synchronized (this.mLock) {
            componentExists = super.componentExists(componentName);
        }
        return componentExists;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public ParsedActivity getActivity(ComponentName component) {
        ParsedActivity activity;
        synchronized (this.mLock) {
            activity = super.getActivity(component);
        }
        return activity;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public ParsedProvider getProvider(ComponentName component) {
        ParsedProvider provider;
        synchronized (this.mLock) {
            provider = super.getProvider(component);
        }
        return provider;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public ParsedActivity getReceiver(ComponentName component) {
        ParsedActivity receiver;
        synchronized (this.mLock) {
            receiver = super.getReceiver(component);
        }
        return receiver;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public ParsedService getService(ComponentName component) {
        ParsedService service;
        synchronized (this.mLock) {
            service = super.getService(component);
        }
        return service;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public boolean isActivityDefined(ComponentName component) {
        boolean isActivityDefined;
        synchronized (this.mLock) {
            isActivityDefined = super.isActivityDefined(component);
        }
        return isActivityDefined;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryActivities(Computer computer, Intent intent, String resolvedType, long flags, int userId) {
        List<ResolveInfo> queryActivities;
        synchronized (this.mLock) {
            queryActivities = super.queryActivities(computer, intent, resolvedType, flags, userId);
        }
        return queryActivities;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryActivities(Computer computer, Intent intent, String resolvedType, long flags, List<ParsedActivity> activities, int userId) {
        List<ResolveInfo> queryActivities;
        synchronized (this.mLock) {
            queryActivities = super.queryActivities(computer, intent, resolvedType, flags, activities, userId);
        }
        return queryActivities;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public ProviderInfo queryProvider(Computer computer, String authority, long flags, int userId) {
        ProviderInfo queryProvider;
        synchronized (this.mLock) {
            queryProvider = super.queryProvider(computer, authority, flags, userId);
        }
        return queryProvider;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryProviders(Computer computer, Intent intent, String resolvedType, long flags, int userId) {
        List<ResolveInfo> queryProviders;
        synchronized (this.mLock) {
            queryProviders = super.queryProviders(computer, intent, resolvedType, flags, userId);
        }
        return queryProviders;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryProviders(Computer computer, Intent intent, String resolvedType, long flags, List<ParsedProvider> providers, int userId) {
        List<ResolveInfo> queryProviders;
        synchronized (this.mLock) {
            queryProviders = super.queryProviders(computer, intent, resolvedType, flags, providers, userId);
        }
        return queryProviders;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public List<ProviderInfo> queryProviders(Computer computer, String processName, String metaDataKey, int uid, long flags, int userId) {
        List<ProviderInfo> queryProviders;
        synchronized (this.mLock) {
            queryProviders = super.queryProviders(computer, processName, metaDataKey, uid, flags, userId);
        }
        return queryProviders;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryReceivers(Computer computer, Intent intent, String resolvedType, long flags, int userId) {
        List<ResolveInfo> queryReceivers;
        synchronized (this.mLock) {
            queryReceivers = super.queryReceivers(computer, intent, resolvedType, flags, userId);
        }
        return queryReceivers;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryReceivers(Computer computer, Intent intent, String resolvedType, long flags, List<ParsedActivity> receivers, int userId) {
        List<ResolveInfo> queryReceivers;
        synchronized (this.mLock) {
            queryReceivers = super.queryReceivers(computer, intent, resolvedType, flags, receivers, userId);
        }
        return queryReceivers;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryServices(Computer computer, Intent intent, String resolvedType, long flags, int userId) {
        List<ResolveInfo> queryServices;
        synchronized (this.mLock) {
            queryServices = super.queryServices(computer, intent, resolvedType, flags, userId);
        }
        return queryServices;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryServices(Computer computer, Intent intent, String resolvedType, long flags, List<ParsedService> services, int userId) {
        List<ResolveInfo> queryServices;
        synchronized (this.mLock) {
            queryServices = super.queryServices(computer, intent, resolvedType, flags, services, userId);
        }
        return queryServices;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public void querySyncProviders(Computer computer, List<String> outNames, List<ProviderInfo> outInfo, boolean safeMode, int userId) {
        synchronized (this.mLock) {
            super.querySyncProviders(computer, outNames, outInfo, safeMode, userId);
        }
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public void dumpActivityResolvers(PrintWriter pw, DumpState dumpState, String packageName) {
        synchronized (this.mLock) {
            super.dumpActivityResolvers(pw, dumpState, packageName);
        }
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public void dumpProviderResolvers(PrintWriter pw, DumpState dumpState, String packageName) {
        synchronized (this.mLock) {
            super.dumpProviderResolvers(pw, dumpState, packageName);
        }
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public void dumpReceiverResolvers(PrintWriter pw, DumpState dumpState, String packageName) {
        synchronized (this.mLock) {
            super.dumpReceiverResolvers(pw, dumpState, packageName);
        }
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public void dumpServiceResolvers(PrintWriter pw, DumpState dumpState, String packageName) {
        synchronized (this.mLock) {
            super.dumpServiceResolvers(pw, dumpState, packageName);
        }
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public void dumpContentProviders(Computer computer, PrintWriter pw, DumpState dumpState, String packageName) {
        synchronized (this.mLock) {
            super.dumpContentProviders(computer, pw, dumpState, packageName);
        }
    }

    @Override // com.android.server.pm.resolution.ComponentResolverBase, com.android.server.pm.resolution.ComponentResolverApi
    public void dumpServicePermissions(PrintWriter pw, DumpState dumpState) {
        synchronized (this.mLock) {
            super.dumpServicePermissions(pw, dumpState);
        }
    }
}
