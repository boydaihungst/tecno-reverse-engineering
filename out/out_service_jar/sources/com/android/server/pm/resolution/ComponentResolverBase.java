package com.android.server.pm.resolution;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Pair;
import com.android.server.pm.Computer;
import com.android.server.pm.DumpState;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.parsing.PackageInfoUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageUserStateInternal;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.pkg.component.ParsedIntentInfo;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import com.android.server.pm.pkg.component.ParsedProvider;
import com.android.server.pm.pkg.component.ParsedService;
import com.android.server.pm.resolution.ComponentResolver;
import com.android.server.utils.WatchableImpl;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/* loaded from: classes2.dex */
public abstract class ComponentResolverBase extends WatchableImpl implements ComponentResolverApi {
    protected ComponentResolver.ActivityIntentResolver mActivities;
    protected ComponentResolver.ProviderIntentResolver mProviders;
    protected ArrayMap<String, ParsedProvider> mProvidersByAuthority;
    protected ComponentResolver.ReceiverIntentResolver mReceivers;
    protected ComponentResolver.ServiceIntentResolver mServices;
    protected UserManagerService mUserManager;

    /* JADX INFO: Access modifiers changed from: protected */
    public ComponentResolverBase(UserManagerService userManager) {
        this.mUserManager = userManager;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public boolean componentExists(ComponentName componentName) {
        ParsedMainComponent component = this.mActivities.mActivities.get(componentName);
        if (component != null) {
            return true;
        }
        ParsedMainComponent component2 = this.mReceivers.mActivities.get(componentName);
        if (component2 != null) {
            return true;
        }
        ParsedMainComponent component3 = this.mServices.mServices.get(componentName);
        return (component3 == null && this.mProviders.mProviders.get(componentName) == null) ? false : true;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public ParsedActivity getActivity(ComponentName component) {
        return this.mActivities.mActivities.get(component);
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public ParsedProvider getProvider(ComponentName component) {
        return this.mProviders.mProviders.get(component);
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public ParsedActivity getReceiver(ComponentName component) {
        return this.mReceivers.mActivities.get(component);
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public ParsedService getService(ComponentName component) {
        return this.mServices.mServices.get(component);
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public boolean isActivityDefined(ComponentName component) {
        return this.mActivities.mActivities.get(component) != null;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryActivities(Computer computer, Intent intent, String resolvedType, long flags, int userId) {
        return this.mActivities.queryIntent(computer, intent, resolvedType, flags, userId);
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryActivities(Computer computer, Intent intent, String resolvedType, long flags, List<ParsedActivity> activities, int userId) {
        return this.mActivities.queryIntentForPackage(computer, intent, resolvedType, flags, activities, userId);
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public ProviderInfo queryProvider(Computer computer, String authority, long flags, int userId) {
        PackageStateInternal packageState;
        AndroidPackage pkg;
        PackageUserStateInternal state;
        ApplicationInfo appInfo;
        ParsedProvider p = this.mProvidersByAuthority.get(authority);
        if (p == null || (packageState = computer.getPackageStateInternal(p.getPackageName())) == null || (pkg = packageState.getPkg()) == null || (appInfo = PackageInfoUtils.generateApplicationInfo(pkg, flags, (state = packageState.getUserStateOrDefault(userId)), userId, packageState)) == null) {
            return null;
        }
        return PackageInfoUtils.generateProviderInfo(pkg, p, flags, state, appInfo, userId, packageState);
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryProviders(Computer computer, Intent intent, String resolvedType, long flags, int userId) {
        return this.mProviders.queryIntent(computer, intent, resolvedType, flags, userId);
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryProviders(Computer computer, Intent intent, String resolvedType, long flags, List<ParsedProvider> providers, int userId) {
        return this.mProviders.queryIntentForPackage(computer, intent, resolvedType, flags, providers, userId);
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public List<ProviderInfo> queryProviders(Computer computer, String processName, String metaDataKey, int uid, long flags, int userId) {
        PackageInfoUtils.CachedApplicationInfoGenerator appInfoGenerator;
        ProviderInfo info;
        if (!this.mUserManager.exists(userId)) {
            return null;
        }
        PackageInfoUtils.CachedApplicationInfoGenerator appInfoGenerator2 = null;
        List<ProviderInfo> providerList = null;
        for (int i = this.mProviders.mProviders.size() - 1; i >= 0; i--) {
            ParsedProvider p = this.mProviders.mProviders.valueAt(i);
            if (p.getAuthority() != null) {
                PackageStateInternal ps = computer.getPackageStateInternal(p.getPackageName());
                if (ps != null) {
                    AndroidPackage pkg = ps.getPkg();
                    if (pkg != null) {
                        if (processName != null) {
                            if (p.getProcessName().equals(processName)) {
                                if (!UserHandle.isSameApp(pkg.getUid(), uid)) {
                                }
                            }
                        }
                        if (metaDataKey == null || p.getMetaData().containsKey(metaDataKey)) {
                            if (appInfoGenerator2 != null) {
                                appInfoGenerator = appInfoGenerator2;
                            } else {
                                PackageInfoUtils.CachedApplicationInfoGenerator appInfoGenerator3 = new PackageInfoUtils.CachedApplicationInfoGenerator();
                                appInfoGenerator = appInfoGenerator3;
                            }
                            PackageUserStateInternal state = ps.getUserStateOrDefault(userId);
                            ApplicationInfo appInfo = appInfoGenerator.generate(pkg, flags, state, userId, ps);
                            if (appInfo == null || (info = PackageInfoUtils.generateProviderInfo(pkg, p, flags, state, appInfo, userId, ps)) == null) {
                                appInfoGenerator2 = appInfoGenerator;
                            } else {
                                if (providerList == null) {
                                    providerList = new ArrayList<>(i + 1);
                                }
                                providerList.add(info);
                                appInfoGenerator2 = appInfoGenerator;
                            }
                        }
                    }
                }
            }
        }
        return providerList;
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryReceivers(Computer computer, Intent intent, String resolvedType, long flags, int userId) {
        return this.mReceivers.queryIntent(computer, intent, resolvedType, flags, userId);
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryReceivers(Computer computer, Intent intent, String resolvedType, long flags, List<ParsedActivity> receivers, int userId) {
        return this.mReceivers.queryIntentForPackage(computer, intent, resolvedType, flags, receivers, userId);
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryServices(Computer computer, Intent intent, String resolvedType, long flags, int userId) {
        return this.mServices.queryIntent(computer, intent, resolvedType, flags, userId);
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public List<ResolveInfo> queryServices(Computer computer, Intent intent, String resolvedType, long flags, List<ParsedService> services, int userId) {
        return this.mServices.queryIntentForPackage(computer, intent, resolvedType, flags, services, userId);
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public void querySyncProviders(Computer computer, List<String> outNames, List<ProviderInfo> outInfo, boolean safeMode, int userId) {
        PackageStateInternal ps;
        AndroidPackage pkg;
        ProviderInfo info;
        PackageInfoUtils.CachedApplicationInfoGenerator appInfoGenerator = null;
        for (int i = this.mProvidersByAuthority.size() - 1; i >= 0; i--) {
            ParsedProvider p = this.mProvidersByAuthority.valueAt(i);
            if (p.isSyncable() && (ps = computer.getPackageStateInternal(p.getPackageName())) != null && (pkg = ps.getPkg()) != null && (!safeMode || pkg.isSystem())) {
                if (appInfoGenerator == null) {
                    appInfoGenerator = new PackageInfoUtils.CachedApplicationInfoGenerator();
                }
                PackageUserStateInternal state = ps.getUserStateOrDefault(userId);
                ApplicationInfo appInfo = appInfoGenerator.generate(pkg, 0L, state, userId, ps);
                if (appInfo != null && (info = PackageInfoUtils.generateProviderInfo(pkg, p, 0L, state, appInfo, userId, ps)) != null) {
                    outNames.add(this.mProvidersByAuthority.keyAt(i));
                    outInfo.add(info);
                }
            }
        }
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public void dumpActivityResolvers(PrintWriter pw, DumpState dumpState, String packageName) {
        if (this.mActivities.dump(pw, dumpState.getTitlePrinted() ? "\nActivity Resolver Table:" : "Activity Resolver Table:", "  ", packageName, dumpState.isOptionEnabled(1), true)) {
            dumpState.setTitlePrinted(true);
        }
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public void dumpProviderResolvers(PrintWriter pw, DumpState dumpState, String packageName) {
        if (this.mProviders.dump(pw, dumpState.getTitlePrinted() ? "\nProvider Resolver Table:" : "Provider Resolver Table:", "  ", packageName, dumpState.isOptionEnabled(1), true)) {
            dumpState.setTitlePrinted(true);
        }
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public void dumpReceiverResolvers(PrintWriter pw, DumpState dumpState, String packageName) {
        if (this.mReceivers.dump(pw, dumpState.getTitlePrinted() ? "\nReceiver Resolver Table:" : "Receiver Resolver Table:", "  ", packageName, dumpState.isOptionEnabled(1), true)) {
            dumpState.setTitlePrinted(true);
        }
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public void dumpServiceResolvers(PrintWriter pw, DumpState dumpState, String packageName) {
        if (this.mServices.dump(pw, dumpState.getTitlePrinted() ? "\nService Resolver Table:" : "Service Resolver Table:", "  ", packageName, dumpState.isOptionEnabled(1), true)) {
            dumpState.setTitlePrinted(true);
        }
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public void dumpContentProviders(Computer computer, PrintWriter pw, DumpState dumpState, String packageName) {
        boolean printedSomething = false;
        for (ParsedProvider p : this.mProviders.mProviders.values()) {
            if (packageName == null || packageName.equals(p.getPackageName())) {
                if (!printedSomething) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    pw.println("Registered ContentProviders:");
                    printedSomething = true;
                }
                pw.print("  ");
                ComponentName.printShortString(pw, p.getPackageName(), p.getName());
                pw.println(":");
                pw.print("    ");
                pw.println(p.toString());
            }
        }
        boolean printedSomething2 = false;
        for (Map.Entry<String, ParsedProvider> entry : this.mProvidersByAuthority.entrySet()) {
            ParsedProvider p2 = entry.getValue();
            if (packageName == null || packageName.equals(p2.getPackageName())) {
                if (!printedSomething2) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    pw.println("ContentProvider Authorities:");
                    printedSomething2 = true;
                }
                pw.print("  [");
                pw.print(entry.getKey());
                pw.println("]:");
                pw.print("    ");
                pw.println(p2.toString());
                AndroidPackage pkg = computer.getPackage(p2.getPackageName());
                if (pkg != null) {
                    pw.print("      applicationInfo=");
                    pw.println(AndroidPackageUtils.generateAppInfoWithoutState(pkg));
                }
            }
        }
    }

    @Override // com.android.server.pm.resolution.ComponentResolverApi
    public void dumpServicePermissions(PrintWriter pw, DumpState dumpState) {
        if (dumpState.onTitlePrinted()) {
            pw.println();
        }
        pw.println("Service permissions:");
        Iterator<F> filterIterator = this.mServices.filterIterator();
        while (filterIterator.hasNext()) {
            Pair<ParsedService, ParsedIntentInfo> pair = (Pair) filterIterator.next();
            ParsedService service = (ParsedService) pair.first;
            String permission = service.getPermission();
            if (permission != null) {
                pw.print("    ");
                pw.print(service.getComponentName().flattenToShortString());
                pw.print(": ");
                pw.println(permission);
            }
        }
    }
}
