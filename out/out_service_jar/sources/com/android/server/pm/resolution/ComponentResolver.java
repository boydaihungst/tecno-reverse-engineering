package com.android.server.pm.resolution;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.InstantAppResolveInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DebugUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.IntentResolver;
import com.android.server.am.HostingRecord;
import com.android.server.pm.Computer;
import com.android.server.pm.PackageManagerException;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.UserNeedsBadgingCache;
import com.android.server.pm.parsing.PackageInfoUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageStateUtils;
import com.android.server.pm.pkg.PackageUserStateInternal;
import com.android.server.pm.pkg.component.ComponentMutateUtils;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.pkg.component.ParsedComponent;
import com.android.server.pm.pkg.component.ParsedIntentInfo;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import com.android.server.pm.pkg.component.ParsedProvider;
import com.android.server.pm.pkg.component.ParsedProviderImpl;
import com.android.server.pm.pkg.component.ParsedService;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import com.android.server.pm.snapshot.PackageDataSnapshot;
import com.android.server.utils.Snappable;
import com.android.server.utils.SnapshotCache;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import com.transsion.server.foldable.TranFoldingScreenController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
/* loaded from: classes2.dex */
public class ComponentResolver extends ComponentResolverLocked implements Snappable<ComponentResolverApi> {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_FILTERS = false;
    private static final boolean DEBUG_SHOW_INFO = false;
    private static final Set<String> PROTECTED_ACTIONS;
    public static final Comparator<ResolveInfo> RESOLVE_PRIORITY_SORTER;
    private static final String TAG = "PackageManager";
    boolean mDeferProtectedFilters;
    List<Pair<ParsedMainComponent, ParsedIntentInfo>> mProtectedFilters;
    final SnapshotCache<ComponentResolverApi> mSnapshot;

    private void onChanged() {
        dispatchChange(this);
    }

    static {
        ArraySet arraySet = new ArraySet();
        PROTECTED_ACTIONS = arraySet;
        arraySet.add("android.intent.action.SEND");
        arraySet.add("android.intent.action.SENDTO");
        arraySet.add("android.intent.action.SEND_MULTIPLE");
        arraySet.add("android.intent.action.VIEW");
        RESOLVE_PRIORITY_SORTER = new Comparator() { // from class: com.android.server.pm.resolution.ComponentResolver$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ComponentResolver.lambda$static$0((ResolveInfo) obj, (ResolveInfo) obj2);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$static$0(ResolveInfo r1, ResolveInfo r2) {
        int v1 = r1.priority;
        int v2 = r2.priority;
        if (v1 != v2) {
            return v1 > v2 ? -1 : 1;
        }
        int v12 = r1.preferredOrder;
        int v22 = r2.preferredOrder;
        if (v12 != v22) {
            return v12 > v22 ? -1 : 1;
        } else if (r1.isDefault != r2.isDefault) {
            return r1.isDefault ? -1 : 1;
        } else {
            int v13 = r1.match;
            int v23 = r2.match;
            if (v13 != v23) {
                return v13 > v23 ? -1 : 1;
            } else if (r1.system != r2.system) {
                return r1.system ? -1 : 1;
            } else if (r1.activityInfo != null) {
                return r1.activityInfo.packageName.compareTo(r2.activityInfo.packageName);
            } else {
                if (r1.serviceInfo != null) {
                    return r1.serviceInfo.packageName.compareTo(r2.serviceInfo.packageName);
                }
                if (r1.providerInfo != null) {
                    return r1.providerInfo.packageName.compareTo(r2.providerInfo.packageName);
                }
                return 0;
            }
        }
    }

    public ComponentResolver(UserManagerService userManager, final UserNeedsBadgingCache userNeedsBadgingCache) {
        super(userManager);
        this.mDeferProtectedFilters = true;
        this.mActivities = new ActivityIntentResolver(userManager, userNeedsBadgingCache);
        this.mProviders = new ProviderIntentResolver(userManager);
        this.mReceivers = new ReceiverIntentResolver(userManager, userNeedsBadgingCache);
        this.mServices = new ServiceIntentResolver(userManager);
        this.mProvidersByAuthority = new ArrayMap<>();
        this.mDeferProtectedFilters = true;
        this.mSnapshot = new SnapshotCache<ComponentResolverApi>(this, this) { // from class: com.android.server.pm.resolution.ComponentResolver.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public ComponentResolverApi createSnapshot() {
                ComponentResolverSnapshot componentResolverSnapshot;
                synchronized (ComponentResolver.this.mLock) {
                    componentResolverSnapshot = new ComponentResolverSnapshot(ComponentResolver.this, userNeedsBadgingCache);
                }
                return componentResolverSnapshot;
            }
        };
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.android.server.utils.Snappable
    public ComponentResolverApi snapshot() {
        return this.mSnapshot.snapshot();
    }

    public void addAllComponents(AndroidPackage pkg, boolean chatty, String setupWizardPackage, Computer computer) {
        ArrayList<Pair<ParsedActivity, ParsedIntentInfo>> newIntents = new ArrayList<>();
        synchronized (this.mLock) {
            addActivitiesLocked(computer, pkg, newIntents, chatty);
            addReceiversLocked(computer, pkg, chatty);
            addProvidersLocked(computer, pkg, chatty);
            addServicesLocked(computer, pkg, chatty);
            onChanged();
        }
        for (int i = newIntents.size() - 1; i >= 0; i--) {
            Pair<ParsedActivity, ParsedIntentInfo> pair = newIntents.get(i);
            PackageStateInternal disabledPkgSetting = computer.getDisabledSystemPackage(((ParsedActivity) pair.first).getPackageName());
            List<ParsedActivity> list = null;
            AndroidPackage disabledPkg = disabledPkgSetting == null ? null : disabledPkgSetting.getPkg();
            if (disabledPkg != null) {
                list = disabledPkg.getActivities();
            }
            List<ParsedActivity> systemActivities = list;
            adjustPriority(computer, systemActivities, (ParsedActivity) pair.first, (ParsedIntentInfo) pair.second, setupWizardPackage);
            onChanged();
        }
    }

    public void removeAllComponents(AndroidPackage pkg, boolean chatty) {
        synchronized (this.mLock) {
            removeAllComponentsLocked(pkg, chatty);
            onChanged();
        }
    }

    public void fixProtectedFilterPriorities(String setupWizardPackage) {
        synchronized (this.mLock) {
            if (this.mDeferProtectedFilters) {
                this.mDeferProtectedFilters = false;
                List<Pair<ParsedMainComponent, ParsedIntentInfo>> list = this.mProtectedFilters;
                if (list != null && list.size() != 0) {
                    List<Pair<ParsedMainComponent, ParsedIntentInfo>> protectedFilters = this.mProtectedFilters;
                    this.mProtectedFilters = null;
                    for (int i = protectedFilters.size() - 1; i >= 0; i--) {
                        Pair<ParsedMainComponent, ParsedIntentInfo> pair = protectedFilters.get(i);
                        ParsedMainComponent component = (ParsedMainComponent) pair.first;
                        ParsedIntentInfo intentInfo = (ParsedIntentInfo) pair.second;
                        IntentFilter filter = intentInfo.getIntentFilter();
                        String packageName = component.getPackageName();
                        component.getClassName();
                        if (!packageName.equals(setupWizardPackage)) {
                            filter.setPriority(0);
                        }
                    }
                    onChanged();
                }
            }
        }
    }

    private void addActivitiesLocked(Computer computer, AndroidPackage pkg, List<Pair<ParsedActivity, ParsedIntentInfo>> newIntents, boolean chatty) {
        int activitiesSize = ArrayUtils.size(pkg.getActivities());
        StringBuilder r = null;
        boolean isPkgInUnresizeableBlockList = TranFoldingScreenController.isPkgInUnresizeableBlockList(pkg.getPackageName());
        boolean isPkgInForceResizeableWhiteList = TranFoldingScreenController.isPkgInForceResizeableWhiteList(pkg.getPackageName());
        for (int i = 0; i < activitiesSize; i++) {
            ParsedActivity a = pkg.getActivities().get(i);
            ITranPackageManagerService.Instance().setThemedIcon(a);
            if (isPkgInUnresizeableBlockList) {
                ComponentMutateUtils.setResizeMode(a, -1);
            } else if (isPkgInForceResizeableWhiteList) {
                ComponentMutateUtils.setResizeMode(a, -2);
            }
            this.mActivities.addActivity(computer, a, HostingRecord.HOSTING_TYPE_ACTIVITY, newIntents);
            if (PackageManagerService.DEBUG_PACKAGE_SCANNING && chatty) {
                if (r == null) {
                    r = new StringBuilder(256);
                } else {
                    r.append(' ');
                }
                r.append(a.getName());
            }
        }
        if (!PackageManagerService.DEBUG_PACKAGE_SCANNING || !chatty) {
            return;
        }
        Log.d(TAG, "  Activities: " + (r == null ? "<NONE>" : r));
    }

    private void addProvidersLocked(Computer computer, AndroidPackage pkg, boolean chatty) {
        ComponentName component;
        int providersSize = ArrayUtils.size(pkg.getProviders());
        StringBuilder r = null;
        for (int i = 0; i < providersSize; i++) {
            ParsedProvider p = pkg.getProviders().get(i);
            this.mProviders.addProvider(computer, p);
            if (p.getAuthority() != null) {
                String[] names = p.getAuthority().split(";");
                ComponentMutateUtils.setAuthority(p, null);
                for (int j = 0; j < names.length; j++) {
                    if (j == 1 && p.isSyncable()) {
                        p = new ParsedProviderImpl(p);
                        ComponentMutateUtils.setSyncable(p, false);
                    }
                    if (!this.mProvidersByAuthority.containsKey(names[j])) {
                        this.mProvidersByAuthority.put(names[j], p);
                        if (p.getAuthority() == null) {
                            ComponentMutateUtils.setAuthority(p, names[j]);
                        } else {
                            ComponentMutateUtils.setAuthority(p, p.getAuthority() + ";" + names[j]);
                        }
                        if (PackageManagerService.DEBUG_PACKAGE_SCANNING && chatty) {
                            Log.d(TAG, "Registered content provider: " + names[j] + ", className = " + p.getName() + ", isSyncable = " + p.isSyncable());
                        }
                    } else {
                        ParsedProvider other = this.mProvidersByAuthority.get(names[j]);
                        if (other == null || other.getComponentName() == null) {
                            component = null;
                        } else {
                            component = other.getComponentName();
                        }
                        String packageName = component != null ? component.getPackageName() : "?";
                        Slog.w(TAG, "Skipping provider name " + names[j] + " (in package " + pkg.getPackageName() + "): name already used by " + packageName);
                    }
                }
            }
            if (PackageManagerService.DEBUG_PACKAGE_SCANNING && chatty) {
                if (r == null) {
                    r = new StringBuilder(256);
                } else {
                    r.append(' ');
                }
                r.append(p.getName());
            }
        }
        if (!PackageManagerService.DEBUG_PACKAGE_SCANNING || !chatty) {
            return;
        }
        Log.d(TAG, "  Providers: " + (r == null ? "<NONE>" : r));
    }

    private void addReceiversLocked(Computer computer, AndroidPackage pkg, boolean chatty) {
        int receiversSize = ArrayUtils.size(pkg.getReceivers());
        StringBuilder r = null;
        for (int i = 0; i < receiversSize; i++) {
            ParsedActivity a = pkg.getReceivers().get(i);
            this.mReceivers.addActivity(computer, a, ParsingPackageUtils.TAG_RECEIVER, null);
            if (PackageManagerService.DEBUG_PACKAGE_SCANNING && chatty) {
                if (r == null) {
                    r = new StringBuilder(256);
                } else {
                    r.append(' ');
                }
                r.append(a.getName());
            }
        }
        if (!PackageManagerService.DEBUG_PACKAGE_SCANNING || !chatty) {
            return;
        }
        Log.d(TAG, "  Receivers: " + (r == null ? "<NONE>" : r));
    }

    private void addServicesLocked(Computer computer, AndroidPackage pkg, boolean chatty) {
        int servicesSize = ArrayUtils.size(pkg.getServices());
        StringBuilder r = null;
        for (int i = 0; i < servicesSize; i++) {
            ParsedService s = pkg.getServices().get(i);
            this.mServices.addService(computer, s);
            if (PackageManagerService.DEBUG_PACKAGE_SCANNING && chatty) {
                if (r == null) {
                    r = new StringBuilder(256);
                } else {
                    r.append(' ');
                }
                r.append(s.getName());
            }
        }
        if (!PackageManagerService.DEBUG_PACKAGE_SCANNING || !chatty) {
            return;
        }
        Log.d(TAG, "  Services: " + (r == null ? "<NONE>" : r));
    }

    private static <T> void getIntentListSubset(List<ParsedIntentInfo> intentList, Function<IntentFilter, Iterator<T>> generator, Iterator<T> searchIterator) {
        while (searchIterator.hasNext() && intentList.size() != 0) {
            T searchAction = searchIterator.next();
            Iterator<ParsedIntentInfo> intentIter = intentList.iterator();
            while (intentIter.hasNext()) {
                ParsedIntentInfo intentInfo = intentIter.next();
                boolean selectionFound = false;
                Iterator<T> intentSelectionIter = generator.apply(intentInfo.getIntentFilter());
                while (true) {
                    if (intentSelectionIter == null || !intentSelectionIter.hasNext()) {
                        break;
                    }
                    T intentSelection = intentSelectionIter.next();
                    if (intentSelection != null && intentSelection.equals(searchAction)) {
                        selectionFound = true;
                        break;
                    }
                }
                if (!selectionFound) {
                    intentIter.remove();
                }
            }
        }
    }

    private static boolean isProtectedAction(IntentFilter filter) {
        Iterator<String> actionsIter = filter.actionsIterator();
        while (actionsIter != null && actionsIter.hasNext()) {
            String filterAction = actionsIter.next();
            if (PROTECTED_ACTIONS.contains(filterAction)) {
                return true;
            }
        }
        return false;
    }

    private static ParsedActivity findMatchingActivity(List<ParsedActivity> activityList, ParsedActivity activityInfo) {
        for (ParsedActivity sysActivity : activityList) {
            if (sysActivity.getName().equals(activityInfo.getName())) {
                return sysActivity;
            }
            if (sysActivity.getName().equals(activityInfo.getTargetActivity())) {
                return sysActivity;
            }
            if (sysActivity.getTargetActivity() != null) {
                if (sysActivity.getTargetActivity().equals(activityInfo.getName())) {
                    return sysActivity;
                }
                if (sysActivity.getTargetActivity().equals(activityInfo.getTargetActivity())) {
                    return sysActivity;
                }
            }
        }
        return null;
    }

    private void adjustPriority(Computer computer, List<ParsedActivity> systemActivities, ParsedActivity activity, ParsedIntentInfo intentInfo, String setupWizardPackage) {
        IntentFilter intentFilter = intentInfo.getIntentFilter();
        if (intentFilter.getPriority() <= 0) {
            return;
        }
        String packageName = activity.getPackageName();
        AndroidPackage pkg = computer.getPackage(packageName);
        boolean privilegedApp = pkg.isPrivileged();
        activity.getClassName();
        if (!privilegedApp) {
            intentFilter.setPriority(0);
        } else if (isProtectedAction(intentFilter)) {
            if (this.mDeferProtectedFilters) {
                if (this.mProtectedFilters == null) {
                    this.mProtectedFilters = new ArrayList();
                }
                this.mProtectedFilters.add(Pair.create(activity, intentInfo));
            } else if (!packageName.equals(setupWizardPackage)) {
                intentFilter.setPriority(0);
            }
        } else if (systemActivities != null) {
            ParsedActivity foundActivity = findMatchingActivity(systemActivities, activity);
            if (foundActivity == null) {
                intentFilter.setPriority(0);
                return;
            }
            List<ParsedIntentInfo> intentListCopy = new ArrayList<>(foundActivity.getIntents());
            Iterator<String> actionsIterator = intentFilter.actionsIterator();
            if (actionsIterator != null) {
                getIntentListSubset(intentListCopy, new Function() { // from class: com.android.server.pm.resolution.ComponentResolver$$ExternalSyntheticLambda1
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return ((IntentFilter) obj).actionsIterator();
                    }
                }, actionsIterator);
                if (intentListCopy.size() == 0) {
                    intentFilter.setPriority(0);
                    return;
                }
            }
            Iterator<String> categoriesIterator = intentFilter.categoriesIterator();
            if (categoriesIterator != null) {
                getIntentListSubset(intentListCopy, new Function() { // from class: com.android.server.pm.resolution.ComponentResolver$$ExternalSyntheticLambda2
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return ((IntentFilter) obj).categoriesIterator();
                    }
                }, categoriesIterator);
                if (intentListCopy.size() == 0) {
                    intentFilter.setPriority(0);
                    return;
                }
            }
            Iterator<String> schemesIterator = intentFilter.schemesIterator();
            if (schemesIterator != null) {
                getIntentListSubset(intentListCopy, new Function() { // from class: com.android.server.pm.resolution.ComponentResolver$$ExternalSyntheticLambda3
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return ((IntentFilter) obj).schemesIterator();
                    }
                }, schemesIterator);
                if (intentListCopy.size() == 0) {
                    intentFilter.setPriority(0);
                    return;
                }
            }
            Iterator<IntentFilter.AuthorityEntry> authoritiesIterator = intentFilter.authoritiesIterator();
            if (authoritiesIterator != null) {
                getIntentListSubset(intentListCopy, new Function() { // from class: com.android.server.pm.resolution.ComponentResolver$$ExternalSyntheticLambda4
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return ((IntentFilter) obj).authoritiesIterator();
                    }
                }, authoritiesIterator);
                if (intentListCopy.size() == 0) {
                    intentFilter.setPriority(0);
                    return;
                }
            }
            int cappedPriority = 0;
            for (int i = intentListCopy.size() - 1; i >= 0; i--) {
                cappedPriority = Math.max(cappedPriority, intentListCopy.get(i).getIntentFilter().getPriority());
            }
            if (intentFilter.getPriority() > cappedPriority) {
                intentFilter.setPriority(cappedPriority);
            }
        }
    }

    private void removeAllComponentsLocked(AndroidPackage pkg, boolean chatty) {
        int componentSize = ArrayUtils.size(pkg.getActivities());
        StringBuilder r = null;
        for (int i = 0; i < componentSize; i++) {
            ParsedActivity a = pkg.getActivities().get(i);
            this.mActivities.removeActivity(a, HostingRecord.HOSTING_TYPE_ACTIVITY);
            if (PackageManagerService.DEBUG_REMOVE && chatty) {
                if (r == null) {
                    r = new StringBuilder(256);
                } else {
                    r.append(' ');
                }
                r.append(a.getName());
            }
        }
        if (PackageManagerService.DEBUG_REMOVE && chatty) {
            Log.d(TAG, "  Activities: " + ((Object) (r == null ? "<NONE>" : r)));
        }
        int componentSize2 = ArrayUtils.size(pkg.getProviders());
        StringBuilder r2 = null;
        for (int i2 = 0; i2 < componentSize2; i2++) {
            ParsedProvider p = pkg.getProviders().get(i2);
            this.mProviders.removeProvider(p);
            if (p.getAuthority() != null) {
                String[] names = p.getAuthority().split(";");
                for (int j = 0; j < names.length; j++) {
                    if (this.mProvidersByAuthority.get(names[j]) == p) {
                        this.mProvidersByAuthority.remove(names[j]);
                        if (PackageManagerService.DEBUG_REMOVE && chatty) {
                            Log.d(TAG, "Unregistered content provider: " + names[j] + ", className = " + p.getName() + ", isSyncable = " + p.isSyncable());
                        }
                    }
                }
                if (PackageManagerService.DEBUG_REMOVE && chatty) {
                    if (r2 == null) {
                        r2 = new StringBuilder(256);
                    } else {
                        r2.append(' ');
                    }
                    r2.append(p.getName());
                }
            }
        }
        if (PackageManagerService.DEBUG_REMOVE && chatty) {
            Log.d(TAG, "  Providers: " + ((Object) (r2 == null ? "<NONE>" : r2)));
        }
        int componentSize3 = ArrayUtils.size(pkg.getReceivers());
        StringBuilder r3 = null;
        for (int i3 = 0; i3 < componentSize3; i3++) {
            ParsedActivity a2 = pkg.getReceivers().get(i3);
            this.mReceivers.removeActivity(a2, ParsingPackageUtils.TAG_RECEIVER);
            if (PackageManagerService.DEBUG_REMOVE && chatty) {
                if (r3 == null) {
                    r3 = new StringBuilder(256);
                } else {
                    r3.append(' ');
                }
                r3.append(a2.getName());
            }
        }
        if (PackageManagerService.DEBUG_REMOVE && chatty) {
            Log.d(TAG, "  Receivers: " + ((Object) (r3 == null ? "<NONE>" : r3)));
        }
        int componentSize4 = ArrayUtils.size(pkg.getServices());
        StringBuilder r4 = null;
        for (int i4 = 0; i4 < componentSize4; i4++) {
            ParsedService s = pkg.getServices().get(i4);
            this.mServices.removeService(s);
            if (PackageManagerService.DEBUG_REMOVE && chatty) {
                if (r4 == 0) {
                    r4 = new StringBuilder(256);
                } else {
                    r4.append(' ');
                }
                r4.append(s.getName());
            }
        }
        if (!PackageManagerService.DEBUG_REMOVE || !chatty) {
            return;
        }
        Log.d(TAG, "  Services: " + ((Object) (r4 != null ? r4 : "<NONE>")));
    }

    public void assertProvidersNotDefined(AndroidPackage pkg) throws PackageManagerException {
        synchronized (this.mLock) {
            int providersSize = ArrayUtils.size(pkg.getProviders());
            for (int i = 0; i < providersSize; i++) {
                ParsedProvider p = pkg.getProviders().get(i);
                if (p.getAuthority() != null) {
                    String[] names = p.getAuthority().split(";");
                    for (int j = 0; j < names.length; j++) {
                        if (this.mProvidersByAuthority.containsKey(names[j])) {
                            ParsedProvider other = this.mProvidersByAuthority.get(names[j]);
                            String otherPackageName = (other == null || other.getComponentName() == null) ? "?" : other.getComponentName().getPackageName();
                            if (!otherPackageName.equals(pkg.getPackageName())) {
                                throw new PackageManagerException(-13, "Can't install because provider name " + names[j] + " (in package " + pkg.getPackageName() + ") is already used by " + otherPackageName);
                            }
                        }
                    }
                    continue;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static abstract class MimeGroupsAwareIntentResolver<F extends Pair<? extends ParsedComponent, ParsedIntentInfo>, R> extends IntentResolver<F, R> {
        private boolean mIsUpdatingMimeGroup;
        private final ArrayMap<String, F[]> mMimeGroupToFilter;
        protected final UserManagerService mUserManager;

        /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.server.pm.resolution.ComponentResolver$MimeGroupsAwareIntentResolver<F extends android.util.Pair<? extends com.android.server.pm.pkg.component.ParsedComponent, com.android.server.pm.pkg.component.ParsedIntentInfo>, R> */
        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.android.server.IntentResolver
        public /* bridge */ /* synthetic */ void addFilter(PackageDataSnapshot packageDataSnapshot, Object obj) {
            addFilter(packageDataSnapshot, (PackageDataSnapshot) ((Pair) obj));
        }

        /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.server.pm.resolution.ComponentResolver$MimeGroupsAwareIntentResolver<F extends android.util.Pair<? extends com.android.server.pm.pkg.component.ParsedComponent, com.android.server.pm.pkg.component.ParsedIntentInfo>, R> */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.android.server.IntentResolver
        public /* bridge */ /* synthetic */ void removeFilterInternal(Object obj) {
            removeFilterInternal((MimeGroupsAwareIntentResolver<F, R>) ((Pair) obj));
        }

        MimeGroupsAwareIntentResolver(UserManagerService userManager) {
            this.mMimeGroupToFilter = new ArrayMap<>();
            this.mIsUpdatingMimeGroup = false;
            this.mUserManager = userManager;
        }

        MimeGroupsAwareIntentResolver(MimeGroupsAwareIntentResolver<F, R> orig, UserManagerService userManager) {
            ArrayMap<String, F[]> arrayMap = new ArrayMap<>();
            this.mMimeGroupToFilter = arrayMap;
            this.mIsUpdatingMimeGroup = false;
            this.mUserManager = userManager;
            copyFrom(orig);
            copyInto(arrayMap, orig.mMimeGroupToFilter);
            this.mIsUpdatingMimeGroup = orig.mIsUpdatingMimeGroup;
        }

        public void addFilter(PackageDataSnapshot snapshot, F f) {
            IntentFilter intentFilter = getIntentFilter(f);
            applyMimeGroups((Computer) snapshot, f);
            super.addFilter(snapshot, (PackageDataSnapshot) f);
            if (!this.mIsUpdatingMimeGroup) {
                register_intent_filter(f, intentFilter.mimeGroupsIterator(), this.mMimeGroupToFilter, "      MimeGroup: ");
            }
        }

        protected void removeFilterInternal(F f) {
            IntentFilter intentFilter = getIntentFilter(f);
            if (!this.mIsUpdatingMimeGroup) {
                unregister_intent_filter(f, intentFilter.mimeGroupsIterator(), this.mMimeGroupToFilter, "      MimeGroup: ");
            }
            super.removeFilterInternal((MimeGroupsAwareIntentResolver<F, R>) f);
            intentFilter.clearDynamicDataTypes();
        }

        public boolean updateMimeGroup(Computer computer, String packageName, String mimeGroup) {
            F[] filters = this.mMimeGroupToFilter.get(mimeGroup);
            int n = filters != null ? filters.length : 0;
            this.mIsUpdatingMimeGroup = true;
            boolean hasChanges = false;
            for (int i = 0; i < n; i++) {
                F filter = filters[i];
                if (filter == null) {
                    break;
                }
                if (isPackageForFilter(packageName, filter)) {
                    hasChanges |= updateFilter(computer, filter);
                }
            }
            this.mIsUpdatingMimeGroup = false;
            return hasChanges;
        }

        private boolean updateFilter(Computer computer, F f) {
            IntentFilter filter = getIntentFilter(f);
            List<String> oldTypes = filter.dataTypes();
            removeFilter(f);
            addFilter((PackageDataSnapshot) computer, (Computer) f);
            List<String> newTypes = filter.dataTypes();
            return !equalLists(oldTypes, newTypes);
        }

        private boolean equalLists(List<String> first, List<String> second) {
            if (first == null) {
                return second == null;
            } else if (second == null || first.size() != second.size()) {
                return false;
            } else {
                Collections.sort(first);
                Collections.sort(second);
                return first.equals(second);
            }
        }

        private void applyMimeGroups(Computer computer, F f) {
            Collection<String> mimeTypes;
            IntentFilter filter = getIntentFilter(f);
            for (int i = filter.countMimeGroups() - 1; i >= 0; i--) {
                PackageStateInternal packageState = computer.getPackageStateInternal(((ParsedComponent) ((Pair) f).first).getPackageName());
                if (packageState == null) {
                    mimeTypes = Collections.emptyList();
                } else {
                    mimeTypes = packageState.getMimeGroups().get(filter.getMimeGroup(i));
                }
                for (String mimeType : mimeTypes) {
                    try {
                        filter.addDynamicDataType(mimeType);
                    } catch (IntentFilter.MalformedMimeTypeException e) {
                    }
                }
            }
        }

        @Override // com.android.server.IntentResolver
        protected boolean isFilterStopped(PackageStateInternal packageState, int userId) {
            if (this.mUserManager.exists(userId)) {
                if (packageState == null || packageState.getPkg() == null) {
                    return false;
                }
                return !packageState.isSystem() && packageState.getUserStateOrDefault(userId).isStopped();
            }
            return true;
        }
    }

    /* loaded from: classes2.dex */
    public static class ActivityIntentResolver extends MimeGroupsAwareIntentResolver<Pair<ParsedActivity, ParsedIntentInfo>, ResolveInfo> {
        protected final ArrayMap<ComponentName, ParsedActivity> mActivities;
        private UserNeedsBadgingCache mUserNeedsBadging;

        /* JADX DEBUG: Method arguments types fixed to match base method, original types: [com.android.server.pm.snapshot.PackageDataSnapshot, android.util.Pair] */
        @Override // com.android.server.pm.resolution.ComponentResolver.MimeGroupsAwareIntentResolver
        public /* bridge */ /* synthetic */ void addFilter(PackageDataSnapshot packageDataSnapshot, Pair<ParsedActivity, ParsedIntentInfo> pair) {
            super.addFilter(packageDataSnapshot, (PackageDataSnapshot) pair);
        }

        @Override // com.android.server.IntentResolver
        protected /* bridge */ /* synthetic */ boolean allowFilterResult(Object obj, List list) {
            return allowFilterResult((Pair) obj, (List<ResolveInfo>) list);
        }

        @Override // com.android.server.pm.resolution.ComponentResolver.MimeGroupsAwareIntentResolver
        public /* bridge */ /* synthetic */ boolean updateMimeGroup(Computer computer, String str, String str2) {
            return super.updateMimeGroup(computer, str, str2);
        }

        ActivityIntentResolver(UserManagerService userManager, UserNeedsBadgingCache userNeedsBadgingCache) {
            super(userManager);
            this.mActivities = new ArrayMap<>();
            this.mUserNeedsBadging = userNeedsBadgingCache;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public ActivityIntentResolver(ActivityIntentResolver orig, UserManagerService userManager, UserNeedsBadgingCache userNeedsBadgingCache) {
            super(orig, userManager);
            ArrayMap<ComponentName, ParsedActivity> arrayMap = new ArrayMap<>();
            this.mActivities = arrayMap;
            arrayMap.putAll((ArrayMap<? extends ComponentName, ? extends ParsedActivity>) orig.mActivities);
            this.mUserNeedsBadging = userNeedsBadgingCache;
        }

        @Override // com.android.server.IntentResolver
        public List<ResolveInfo> queryIntent(PackageDataSnapshot snapshot, Intent intent, String resolvedType, boolean defaultOnly, int userId) {
            if (this.mUserManager.exists(userId)) {
                long flags = defaultOnly ? 65536 : 0;
                return super.queryIntent(snapshot, intent, resolvedType, defaultOnly, userId, flags);
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public List<ResolveInfo> queryIntent(Computer computer, Intent intent, String resolvedType, long flags, int userId) {
            if (!this.mUserManager.exists(userId)) {
                return null;
            }
            List<ResolveInfo> infoList = super.queryIntent(computer, intent, resolvedType, (65536 & flags) != 0, userId, flags);
            ITranPackageManagerService.Instance().onServiceIntentResolverQuery(infoList, intent, flags, userId);
            return infoList;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public List<ResolveInfo> queryIntentForPackage(Computer computer, Intent intent, String resolvedType, long flags, List<ParsedActivity> packageActivities, int userId) {
            if (!this.mUserManager.exists(userId)) {
                return null;
            }
            if (packageActivities == null) {
                return Collections.emptyList();
            }
            boolean defaultOnly = (flags & 65536) != 0;
            int activitiesSize = packageActivities.size();
            ArrayList<Pair<ParsedActivity, ParsedIntentInfo>[]> listCut = new ArrayList<>(activitiesSize);
            for (int i = 0; i < activitiesSize; i++) {
                ParsedActivity activity = packageActivities.get(i);
                List<ParsedIntentInfo> intentFilters = activity.getIntents();
                if (!intentFilters.isEmpty()) {
                    Pair<ParsedActivity, ParsedIntentInfo>[] array = newArray(intentFilters.size());
                    for (int arrayIndex = 0; arrayIndex < intentFilters.size(); arrayIndex++) {
                        array[arrayIndex] = Pair.create(activity, intentFilters.get(arrayIndex));
                    }
                    listCut.add(array);
                }
            }
            return super.queryIntentFromList(computer, intent, resolvedType, defaultOnly, listCut, userId, flags);
        }

        protected void addActivity(Computer computer, ParsedActivity a, String type, List<Pair<ParsedActivity, ParsedIntentInfo>> newIntents) {
            this.mActivities.put(a.getComponentName(), a);
            int intentsSize = a.getIntents().size();
            for (int j = 0; j < intentsSize; j++) {
                ParsedIntentInfo intent = a.getIntents().get(j);
                IntentFilter intentFilter = intent.getIntentFilter();
                if (newIntents != null && HostingRecord.HOSTING_TYPE_ACTIVITY.equals(type)) {
                    newIntents.add(Pair.create(a, intent));
                }
                if (!intentFilter.debugCheck()) {
                    Log.w(ComponentResolver.TAG, "==> For Activity " + a.getName());
                }
                addFilter((PackageDataSnapshot) computer, Pair.create(a, intent));
            }
        }

        protected void removeActivity(ParsedActivity a, String type) {
            this.mActivities.remove(a.getComponentName());
            int intentsSize = a.getIntents().size();
            for (int j = 0; j < intentsSize; j++) {
                ParsedIntentInfo intent = a.getIntents().get(j);
                intent.getIntentFilter();
                removeFilter(Pair.create(a, intent));
            }
        }

        protected boolean allowFilterResult(Pair<ParsedActivity, ParsedIntentInfo> filter, List<ResolveInfo> dest) {
            for (int i = dest.size() - 1; i >= 0; i--) {
                ActivityInfo destAi = dest.get(i).activityInfo;
                if (Objects.equals(destAi.name, ((ParsedActivity) filter.first).getName()) && Objects.equals(destAi.packageName, ((ParsedActivity) filter.first).getPackageName())) {
                    return false;
                }
            }
            return true;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public Pair<ParsedActivity, ParsedIntentInfo>[] newArray(int size) {
            return new Pair[size];
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public boolean isPackageForFilter(String packageName, Pair<ParsedActivity, ParsedIntentInfo> info) {
            return packageName.equals(((ParsedActivity) info.first).getPackageName());
        }

        private void log(String reason, ParsedIntentInfo info, int match, int userId) {
            Slog.w(ComponentResolver.TAG, reason + "; match: " + DebugUtils.flagsToString(IntentFilter.class, "MATCH_", match) + "; userId: " + userId + "; intent info: " + info);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public ResolveInfo newResult(Computer computer, Pair<ParsedActivity, ParsedIntentInfo> pair, int match, int userId, long customFlags) {
            ParsedActivity activity = (ParsedActivity) pair.first;
            ParsedIntentInfo info = (ParsedIntentInfo) pair.second;
            IntentFilter intentFilter = info.getIntentFilter();
            if (this.mUserManager.exists(userId)) {
                PackageStateInternal packageState = computer.getPackageStateInternal(activity.getPackageName());
                if (packageState != null && packageState.getPkg() != null) {
                    if (PackageStateUtils.isEnabledAndMatches(packageState, activity, customFlags, userId)) {
                        PackageUserStateInternal userState = packageState.getUserStateOrDefault(userId);
                        ActivityInfo ai = PackageInfoUtils.generateActivityInfo(packageState.getPkg(), activity, customFlags, userState, userId, packageState);
                        if (ai == null) {
                            return null;
                        }
                        boolean matchExplicitlyVisibleOnly = (33554432 & customFlags) != 0;
                        boolean matchVisibleToInstantApp = (customFlags & 16777216) != 0;
                        boolean componentVisible = matchVisibleToInstantApp && intentFilter.isVisibleToInstantApp() && (!matchExplicitlyVisibleOnly || intentFilter.isExplicitlyVisibleToInstantApp());
                        boolean matchInstantApp = (customFlags & 8388608) != 0;
                        if (!matchVisibleToInstantApp || componentVisible || userState.isInstantApp()) {
                            if (matchInstantApp || !userState.isInstantApp()) {
                                if (userState.isInstantApp() && packageState.isUpdateAvailable()) {
                                    return null;
                                }
                                ResolveInfo res = new ResolveInfo(intentFilter.hasCategory("android.intent.category.BROWSABLE"));
                                res.activityInfo = ai;
                                if ((customFlags & 64) != 0) {
                                    res.filter = intentFilter;
                                }
                                res.handleAllWebDataURI = intentFilter.handleAllWebDataURI();
                                res.priority = intentFilter.getPriority();
                                res.match = match;
                                res.isDefault = info.isHasDefault();
                                res.labelRes = info.getLabelRes();
                                res.nonLocalizedLabel = info.getNonLocalizedLabel();
                                if (this.mUserNeedsBadging.get(userId)) {
                                    res.noResourceId = true;
                                } else {
                                    res.icon = info.getIcon();
                                }
                                res.iconResourceId = info.getIcon();
                                res.system = res.activityInfo.applicationInfo.isSystemApp();
                                res.isInstantAppAvailable = userState.isInstantApp();
                                return res;
                            }
                            return null;
                        }
                        return null;
                    }
                }
                return null;
            }
            return null;
        }

        @Override // com.android.server.IntentResolver
        protected void sortResults(List<ResolveInfo> results) {
            results.sort(ComponentResolver.RESOLVE_PRIORITY_SORTER);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public void dumpFilter(PrintWriter out, String prefix, Pair<ParsedActivity, ParsedIntentInfo> pair) {
            ParsedActivity activity = (ParsedActivity) pair.first;
            ParsedIntentInfo filter = (ParsedIntentInfo) pair.second;
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(activity)));
            out.print(' ');
            ComponentName.printShortString(out, activity.getPackageName(), activity.getClassName());
            out.print(" filter ");
            out.println(Integer.toHexString(System.identityHashCode(filter)));
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public Object filterToLabel(Pair<ParsedActivity, ParsedIntentInfo> filter) {
            return filter;
        }

        @Override // com.android.server.IntentResolver
        protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
            Pair<ParsedActivity, ParsedIntentInfo> pair = (Pair) label;
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(pair.first)));
            out.print(' ');
            ComponentName.printShortString(out, ((ParsedActivity) pair.first).getPackageName(), ((ParsedActivity) pair.first).getClassName());
            if (count > 1) {
                out.print(" (");
                out.print(count);
                out.print(" filters)");
            }
            out.println();
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public IntentFilter getIntentFilter(Pair<ParsedActivity, ParsedIntentInfo> input) {
            return ((ParsedIntentInfo) input.second).getIntentFilter();
        }

        protected List<ParsedActivity> getResolveList(AndroidPackage pkg) {
            return pkg.getActivities();
        }
    }

    /* loaded from: classes2.dex */
    public static final class ReceiverIntentResolver extends ActivityIntentResolver {
        ReceiverIntentResolver(UserManagerService userManager, UserNeedsBadgingCache userNeedsBadgingCache) {
            super(userManager, userNeedsBadgingCache);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public ReceiverIntentResolver(ReceiverIntentResolver orig, UserManagerService userManager, UserNeedsBadgingCache userNeedsBadgingCache) {
            super(orig, userManager, userNeedsBadgingCache);
        }

        @Override // com.android.server.pm.resolution.ComponentResolver.ActivityIntentResolver
        protected List<ParsedActivity> getResolveList(AndroidPackage pkg) {
            return pkg.getReceivers();
        }
    }

    /* loaded from: classes2.dex */
    public static final class ProviderIntentResolver extends MimeGroupsAwareIntentResolver<Pair<ParsedProvider, ParsedIntentInfo>, ResolveInfo> {
        final ArrayMap<ComponentName, ParsedProvider> mProviders;

        /* JADX DEBUG: Method arguments types fixed to match base method, original types: [com.android.server.pm.snapshot.PackageDataSnapshot, android.util.Pair] */
        @Override // com.android.server.pm.resolution.ComponentResolver.MimeGroupsAwareIntentResolver
        public /* bridge */ /* synthetic */ void addFilter(PackageDataSnapshot packageDataSnapshot, Pair<ParsedProvider, ParsedIntentInfo> pair) {
            super.addFilter(packageDataSnapshot, (PackageDataSnapshot) pair);
        }

        @Override // com.android.server.IntentResolver
        protected /* bridge */ /* synthetic */ boolean allowFilterResult(Object obj, List list) {
            return allowFilterResult((Pair) obj, (List<ResolveInfo>) list);
        }

        @Override // com.android.server.pm.resolution.ComponentResolver.MimeGroupsAwareIntentResolver
        public /* bridge */ /* synthetic */ boolean updateMimeGroup(Computer computer, String str, String str2) {
            return super.updateMimeGroup(computer, str, str2);
        }

        ProviderIntentResolver(UserManagerService userManager) {
            super(userManager);
            this.mProviders = new ArrayMap<>();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public ProviderIntentResolver(ProviderIntentResolver orig, UserManagerService userManager) {
            super(orig, userManager);
            ArrayMap<ComponentName, ParsedProvider> arrayMap = new ArrayMap<>();
            this.mProviders = arrayMap;
            arrayMap.putAll((ArrayMap<? extends ComponentName, ? extends ParsedProvider>) orig.mProviders);
        }

        @Override // com.android.server.IntentResolver
        public List<ResolveInfo> queryIntent(PackageDataSnapshot snapshot, Intent intent, String resolvedType, boolean defaultOnly, int userId) {
            if (!this.mUserManager.exists(userId)) {
                return null;
            }
            long flags = defaultOnly ? 65536L : 0L;
            return super.queryIntent(snapshot, intent, resolvedType, defaultOnly, userId, flags);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public List<ResolveInfo> queryIntent(Computer computer, Intent intent, String resolvedType, long flags, int userId) {
            if (this.mUserManager.exists(userId)) {
                return super.queryIntent(computer, intent, resolvedType, (65536 & flags) != 0, userId, flags);
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public List<ResolveInfo> queryIntentForPackage(Computer computer, Intent intent, String resolvedType, long flags, List<ParsedProvider> packageProviders, int userId) {
            if (!this.mUserManager.exists(userId)) {
                return null;
            }
            if (packageProviders == null) {
                return Collections.emptyList();
            }
            boolean defaultOnly = (flags & 65536) != 0;
            int providersSize = packageProviders.size();
            ArrayList<Pair<ParsedProvider, ParsedIntentInfo>[]> listCut = new ArrayList<>(providersSize);
            for (int i = 0; i < providersSize; i++) {
                ParsedProvider provider = packageProviders.get(i);
                List<ParsedIntentInfo> intentFilters = provider.getIntents();
                if (!intentFilters.isEmpty()) {
                    Pair<ParsedProvider, ParsedIntentInfo>[] array = newArray(intentFilters.size());
                    for (int arrayIndex = 0; arrayIndex < intentFilters.size(); arrayIndex++) {
                        array[arrayIndex] = Pair.create(provider, intentFilters.get(arrayIndex));
                    }
                    listCut.add(array);
                }
            }
            return super.queryIntentFromList(computer, intent, resolvedType, defaultOnly, listCut, userId, flags);
        }

        void addProvider(Computer computer, ParsedProvider p) {
            if (this.mProviders.containsKey(p.getComponentName())) {
                Slog.w(ComponentResolver.TAG, "Provider " + p.getComponentName() + " already defined; ignoring");
                return;
            }
            this.mProviders.put(p.getComponentName(), p);
            int intentsSize = p.getIntents().size();
            for (int j = 0; j < intentsSize; j++) {
                ParsedIntentInfo intent = p.getIntents().get(j);
                IntentFilter intentFilter = intent.getIntentFilter();
                if (!intentFilter.debugCheck()) {
                    Log.w(ComponentResolver.TAG, "==> For Provider " + p.getName());
                }
                addFilter((PackageDataSnapshot) computer, Pair.create(p, intent));
            }
        }

        void removeProvider(ParsedProvider p) {
            this.mProviders.remove(p.getComponentName());
            int intentsSize = p.getIntents().size();
            for (int j = 0; j < intentsSize; j++) {
                ParsedIntentInfo intent = p.getIntents().get(j);
                intent.getIntentFilter();
                removeFilter(Pair.create(p, intent));
            }
        }

        protected boolean allowFilterResult(Pair<ParsedProvider, ParsedIntentInfo> filter, List<ResolveInfo> dest) {
            for (int i = dest.size() - 1; i >= 0; i--) {
                ProviderInfo destPi = dest.get(i).providerInfo;
                if (Objects.equals(destPi.name, ((ParsedProvider) filter.first).getClassName()) && Objects.equals(destPi.packageName, ((ParsedProvider) filter.first).getPackageName())) {
                    return false;
                }
            }
            return true;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public Pair<ParsedProvider, ParsedIntentInfo>[] newArray(int size) {
            return new Pair[size];
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public boolean isPackageForFilter(String packageName, Pair<ParsedProvider, ParsedIntentInfo> info) {
            return packageName.equals(((ParsedProvider) info.first).getPackageName());
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public ResolveInfo newResult(Computer computer, Pair<ParsedProvider, ParsedIntentInfo> pair, int match, int userId, long customFlags) {
            ApplicationInfo appInfo;
            ProviderInfo pi;
            IntentFilter filter;
            if (this.mUserManager.exists(userId)) {
                ParsedProvider provider = (ParsedProvider) pair.first;
                ParsedIntentInfo intentInfo = (ParsedIntentInfo) pair.second;
                IntentFilter filter2 = intentInfo.getIntentFilter();
                PackageStateInternal packageState = computer.getPackageStateInternal(provider.getPackageName());
                if (packageState != null && packageState.getPkg() != null) {
                    if (PackageStateUtils.isEnabledAndMatches(packageState, provider, customFlags, userId)) {
                        PackageUserStateInternal userState = packageState.getUserStateOrDefault(userId);
                        boolean matchVisibleToInstantApp = (16777216 & customFlags) != 0;
                        boolean isInstantApp = (8388608 & customFlags) != 0;
                        if (!matchVisibleToInstantApp || filter2.isVisibleToInstantApp() || userState.isInstantApp()) {
                            if (isInstantApp || !userState.isInstantApp()) {
                                if ((userState.isInstantApp() && packageState.isUpdateAvailable()) || (appInfo = PackageInfoUtils.generateApplicationInfo(packageState.getPkg(), customFlags, userState, userId, packageState)) == null || (pi = PackageInfoUtils.generateProviderInfo(packageState.getPkg(), provider, customFlags, userState, appInfo, userId, packageState)) == null) {
                                    return null;
                                }
                                ResolveInfo res = new ResolveInfo();
                                res.providerInfo = pi;
                                if ((64 & customFlags) == 0) {
                                    filter = filter2;
                                } else {
                                    filter = filter2;
                                    res.filter = filter;
                                }
                                res.priority = filter.getPriority();
                                res.match = match;
                                res.isDefault = intentInfo.isHasDefault();
                                res.labelRes = intentInfo.getLabelRes();
                                res.nonLocalizedLabel = intentInfo.getNonLocalizedLabel();
                                res.icon = intentInfo.getIcon();
                                res.system = res.providerInfo.applicationInfo.isSystemApp();
                                return res;
                            }
                            return null;
                        }
                        return null;
                    }
                }
                return null;
            }
            return null;
        }

        @Override // com.android.server.IntentResolver
        protected void sortResults(List<ResolveInfo> results) {
            results.sort(ComponentResolver.RESOLVE_PRIORITY_SORTER);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public void dumpFilter(PrintWriter out, String prefix, Pair<ParsedProvider, ParsedIntentInfo> pair) {
            ParsedProvider provider = (ParsedProvider) pair.first;
            ParsedIntentInfo filter = (ParsedIntentInfo) pair.second;
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(provider)));
            out.print(' ');
            ComponentName.printShortString(out, provider.getPackageName(), provider.getClassName());
            out.print(" filter ");
            out.println(Integer.toHexString(System.identityHashCode(filter)));
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public Object filterToLabel(Pair<ParsedProvider, ParsedIntentInfo> filter) {
            return filter;
        }

        @Override // com.android.server.IntentResolver
        protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
            Pair<ParsedProvider, ParsedIntentInfo> pair = (Pair) label;
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(pair.first)));
            out.print(' ');
            ComponentName.printShortString(out, ((ParsedProvider) pair.first).getPackageName(), ((ParsedProvider) pair.first).getClassName());
            if (count > 1) {
                out.print(" (");
                out.print(count);
                out.print(" filters)");
            }
            out.println();
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public IntentFilter getIntentFilter(Pair<ParsedProvider, ParsedIntentInfo> input) {
            return ((ParsedIntentInfo) input.second).getIntentFilter();
        }
    }

    /* loaded from: classes2.dex */
    public static final class ServiceIntentResolver extends MimeGroupsAwareIntentResolver<Pair<ParsedService, ParsedIntentInfo>, ResolveInfo> {
        final ArrayMap<ComponentName, ParsedService> mServices;

        /* JADX DEBUG: Method arguments types fixed to match base method, original types: [com.android.server.pm.snapshot.PackageDataSnapshot, android.util.Pair] */
        @Override // com.android.server.pm.resolution.ComponentResolver.MimeGroupsAwareIntentResolver
        public /* bridge */ /* synthetic */ void addFilter(PackageDataSnapshot packageDataSnapshot, Pair<ParsedService, ParsedIntentInfo> pair) {
            super.addFilter(packageDataSnapshot, (PackageDataSnapshot) pair);
        }

        @Override // com.android.server.IntentResolver
        protected /* bridge */ /* synthetic */ boolean allowFilterResult(Object obj, List list) {
            return allowFilterResult((Pair) obj, (List<ResolveInfo>) list);
        }

        @Override // com.android.server.pm.resolution.ComponentResolver.MimeGroupsAwareIntentResolver
        public /* bridge */ /* synthetic */ boolean updateMimeGroup(Computer computer, String str, String str2) {
            return super.updateMimeGroup(computer, str, str2);
        }

        ServiceIntentResolver(UserManagerService userManager) {
            super(userManager);
            this.mServices = new ArrayMap<>();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public ServiceIntentResolver(ServiceIntentResolver orig, UserManagerService userManager) {
            super(orig, userManager);
            ArrayMap<ComponentName, ParsedService> arrayMap = new ArrayMap<>();
            this.mServices = arrayMap;
            arrayMap.putAll((ArrayMap<? extends ComponentName, ? extends ParsedService>) orig.mServices);
        }

        @Override // com.android.server.IntentResolver
        public List<ResolveInfo> queryIntent(PackageDataSnapshot snapshot, Intent intent, String resolvedType, boolean defaultOnly, int userId) {
            if (!this.mUserManager.exists(userId)) {
                return null;
            }
            long flags = defaultOnly ? 65536L : 0L;
            return super.queryIntent(snapshot, intent, resolvedType, defaultOnly, userId, flags);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public List<ResolveInfo> queryIntent(Computer computer, Intent intent, String resolvedType, long flags, int userId) {
            if (this.mUserManager.exists(userId)) {
                return super.queryIntent(computer, intent, resolvedType, (65536 & flags) != 0, userId, flags);
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public List<ResolveInfo> queryIntentForPackage(Computer computer, Intent intent, String resolvedType, long flags, List<ParsedService> packageServices, int userId) {
            if (this.mUserManager.exists(userId)) {
                if (packageServices == null) {
                    return Collections.emptyList();
                }
                boolean defaultOnly = (flags & 65536) != 0;
                int servicesSize = packageServices.size();
                ArrayList<Pair<ParsedService, ParsedIntentInfo>[]> listCut = new ArrayList<>(servicesSize);
                for (int i = 0; i < servicesSize; i++) {
                    ParsedService service = packageServices.get(i);
                    List<ParsedIntentInfo> intentFilters = service.getIntents();
                    if (intentFilters.size() > 0) {
                        Pair<ParsedService, ParsedIntentInfo>[] array = newArray(intentFilters.size());
                        for (int arrayIndex = 0; arrayIndex < intentFilters.size(); arrayIndex++) {
                            array[arrayIndex] = Pair.create(service, intentFilters.get(arrayIndex));
                        }
                        listCut.add(array);
                    }
                }
                return super.queryIntentFromList(computer, intent, resolvedType, defaultOnly, listCut, userId, flags);
            }
            return null;
        }

        void addService(Computer computer, ParsedService s) {
            this.mServices.put(s.getComponentName(), s);
            int intentsSize = s.getIntents().size();
            for (int j = 0; j < intentsSize; j++) {
                ParsedIntentInfo intent = s.getIntents().get(j);
                IntentFilter intentFilter = intent.getIntentFilter();
                if (!intentFilter.debugCheck()) {
                    Log.w(ComponentResolver.TAG, "==> For Service " + s.getName());
                }
                addFilter((PackageDataSnapshot) computer, Pair.create(s, intent));
            }
        }

        void removeService(ParsedService s) {
            this.mServices.remove(s.getComponentName());
            int intentsSize = s.getIntents().size();
            for (int j = 0; j < intentsSize; j++) {
                ParsedIntentInfo intent = s.getIntents().get(j);
                intent.getIntentFilter();
                removeFilter(Pair.create(s, intent));
            }
        }

        protected boolean allowFilterResult(Pair<ParsedService, ParsedIntentInfo> filter, List<ResolveInfo> dest) {
            for (int i = dest.size() - 1; i >= 0; i--) {
                ServiceInfo destAi = dest.get(i).serviceInfo;
                if (Objects.equals(destAi.name, ((ParsedService) filter.first).getClassName()) && Objects.equals(destAi.packageName, ((ParsedService) filter.first).getPackageName())) {
                    return false;
                }
            }
            return true;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public Pair<ParsedService, ParsedIntentInfo>[] newArray(int size) {
            return new Pair[size];
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public boolean isPackageForFilter(String packageName, Pair<ParsedService, ParsedIntentInfo> info) {
            return packageName.equals(((ParsedService) info.first).getPackageName());
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public ResolveInfo newResult(Computer computer, Pair<ParsedService, ParsedIntentInfo> pair, int match, int userId, long customFlags) {
            IntentFilter filter;
            if (this.mUserManager.exists(userId)) {
                ParsedService service = (ParsedService) pair.first;
                ParsedIntentInfo intentInfo = (ParsedIntentInfo) pair.second;
                IntentFilter filter2 = intentInfo.getIntentFilter();
                PackageStateInternal packageState = computer.getPackageStateInternal(service.getPackageName());
                if (packageState == null || packageState.getPkg() == null || !PackageStateUtils.isEnabledAndMatches(packageState, service, customFlags, userId)) {
                    if (!ITranPackageManagerService.Instance().needSkipAppInfo(service.getPackageName(), userId)) {
                        return null;
                    }
                    Slog.d(ComponentResolver.TAG, "ServiceIntentResolver() newResult " + service + ", match=" + DebugUtils.flagsToString(IntentFilter.class, "MATCH_", match));
                }
                PackageUserStateInternal userState = packageState.getUserStateOrDefault(userId);
                ServiceInfo si = PackageInfoUtils.generateServiceInfo(packageState.getPkg(), service, customFlags, userState, userId, packageState);
                if (si == null) {
                    return null;
                }
                boolean matchVisibleToInstantApp = (16777216 & customFlags) != 0;
                boolean isInstantApp = (customFlags & 8388608) != 0;
                if (matchVisibleToInstantApp && !filter2.isVisibleToInstantApp() && !userState.isInstantApp()) {
                    return null;
                }
                if (!isInstantApp && userState.isInstantApp()) {
                    return null;
                }
                if (userState.isInstantApp() && packageState.isUpdateAvailable()) {
                    return null;
                }
                ResolveInfo res = new ResolveInfo();
                res.serviceInfo = si;
                if ((customFlags & 64) == 0) {
                    filter = filter2;
                } else {
                    filter = filter2;
                    res.filter = filter;
                }
                res.priority = filter.getPriority();
                res.match = match;
                res.isDefault = intentInfo.isHasDefault();
                res.labelRes = intentInfo.getLabelRes();
                res.nonLocalizedLabel = intentInfo.getNonLocalizedLabel();
                res.icon = intentInfo.getIcon();
                res.system = res.serviceInfo.applicationInfo.isSystemApp();
                return res;
            }
            return null;
        }

        @Override // com.android.server.IntentResolver
        protected void sortResults(List<ResolveInfo> results) {
            results.sort(ComponentResolver.RESOLVE_PRIORITY_SORTER);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public void dumpFilter(PrintWriter out, String prefix, Pair<ParsedService, ParsedIntentInfo> pair) {
            ParsedService service = (ParsedService) pair.first;
            ParsedIntentInfo filter = (ParsedIntentInfo) pair.second;
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(service)));
            out.print(' ');
            ComponentName.printShortString(out, service.getPackageName(), service.getClassName());
            out.print(" filter ");
            out.print(Integer.toHexString(System.identityHashCode(filter)));
            if (service.getPermission() != null) {
                out.print(" permission ");
                out.println(service.getPermission());
                return;
            }
            out.println();
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public Object filterToLabel(Pair<ParsedService, ParsedIntentInfo> filter) {
            return filter;
        }

        @Override // com.android.server.IntentResolver
        protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
            Pair<ParsedService, ParsedIntentInfo> pair = (Pair) label;
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(pair.first)));
            out.print(' ');
            ComponentName.printShortString(out, ((ParsedService) pair.first).getPackageName(), ((ParsedService) pair.first).getClassName());
            if (count > 1) {
                out.print(" (");
                out.print(count);
                out.print(" filters)");
            }
            out.println();
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public IntentFilter getIntentFilter(Pair<ParsedService, ParsedIntentInfo> input) {
            return ((ParsedIntentInfo) input.second).getIntentFilter();
        }
    }

    /* loaded from: classes2.dex */
    public static final class InstantAppIntentResolver extends IntentResolver<AuxiliaryResolveInfo.AuxiliaryFilter, AuxiliaryResolveInfo.AuxiliaryFilter> {
        final ArrayMap<String, Pair<Integer, InstantAppResolveInfo>> mOrderResult = new ArrayMap<>();
        private final UserManagerService mUserManager;

        public InstantAppIntentResolver(UserManagerService userManager) {
            this.mUserManager = userManager;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // com.android.server.IntentResolver
        public AuxiliaryResolveInfo.AuxiliaryFilter[] newArray(int size) {
            return new AuxiliaryResolveInfo.AuxiliaryFilter[size];
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public boolean isPackageForFilter(String packageName, AuxiliaryResolveInfo.AuxiliaryFilter responseObj) {
            return true;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public AuxiliaryResolveInfo.AuxiliaryFilter newResult(Computer computer, AuxiliaryResolveInfo.AuxiliaryFilter responseObj, int match, int userId, long customFlags) {
            if (this.mUserManager.exists(userId)) {
                String packageName = responseObj.resolveInfo.getPackageName();
                Integer order = Integer.valueOf(responseObj.getOrder());
                Pair<Integer, InstantAppResolveInfo> lastOrderResult = this.mOrderResult.get(packageName);
                if (lastOrderResult == null || ((Integer) lastOrderResult.first).intValue() < order.intValue()) {
                    InstantAppResolveInfo res = responseObj.resolveInfo;
                    if (order.intValue() > 0) {
                        this.mOrderResult.put(packageName, new Pair<>(order, res));
                    }
                    return responseObj;
                }
                return null;
            }
            return null;
        }

        @Override // com.android.server.IntentResolver
        protected void filterResults(List<AuxiliaryResolveInfo.AuxiliaryFilter> results) {
            if (this.mOrderResult.size() == 0) {
                return;
            }
            int resultSize = results.size();
            int i = 0;
            while (i < resultSize) {
                InstantAppResolveInfo info = results.get(i).resolveInfo;
                String packageName = info.getPackageName();
                Pair<Integer, InstantAppResolveInfo> savedInfo = this.mOrderResult.get(packageName);
                if (savedInfo != null) {
                    if (savedInfo.second == info) {
                        this.mOrderResult.remove(packageName);
                        if (this.mOrderResult.size() == 0) {
                            return;
                        }
                    } else {
                        results.remove(i);
                        resultSize--;
                        i--;
                    }
                }
                i++;
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public IntentFilter getIntentFilter(AuxiliaryResolveInfo.AuxiliaryFilter input) {
            return input;
        }
    }

    public boolean updateMimeGroup(Computer computer, String packageName, String group) {
        boolean hasChanges;
        synchronized (this.mLock) {
            hasChanges = false | this.mActivities.updateMimeGroup(computer, packageName, group) | this.mProviders.updateMimeGroup(computer, packageName, group) | this.mReceivers.updateMimeGroup(computer, packageName, group) | this.mServices.updateMimeGroup(computer, packageName, group);
            if (hasChanges) {
                onChanged();
            }
        }
        return hasChanges;
    }

    public ArrayMap<ComponentName, ParsedActivity> getActivitiesMap() {
        return this.mActivities.mActivities;
    }
}
