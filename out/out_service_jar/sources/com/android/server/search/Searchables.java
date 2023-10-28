package com.android.server.search;

import android.app.AppGlobals;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.LocalServices;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
/* loaded from: classes2.dex */
public class Searchables {
    private static final String LOG_TAG = "Searchables";
    private static final String MD_LABEL_DEFAULT_SEARCHABLE = "android.app.default_searchable";
    private static final String MD_SEARCHABLE_SYSTEM_SEARCH = "*";
    private Context mContext;
    private List<ResolveInfo> mGlobalSearchActivities;
    private int mUserId;
    public static String GOOGLE_SEARCH_COMPONENT_NAME = "com.android.googlesearch/.GoogleSearch";
    public static String ENHANCED_GOOGLE_SEARCH_COMPONENT_NAME = "com.google.android.providers.enhancedgooglesearch/.Launcher";
    private static final Comparator<ResolveInfo> GLOBAL_SEARCH_RANKER = new Comparator<ResolveInfo>() { // from class: com.android.server.search.Searchables.1
        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.Comparator
        public int compare(ResolveInfo lhs, ResolveInfo rhs) {
            if (lhs == rhs) {
                return 0;
            }
            boolean lhsSystem = Searchables.isSystemApp(lhs);
            boolean rhsSystem = Searchables.isSystemApp(rhs);
            if (lhsSystem && !rhsSystem) {
                return -1;
            }
            if (rhsSystem && !lhsSystem) {
                return 1;
            }
            return rhs.priority - lhs.priority;
        }
    };
    private HashMap<ComponentName, SearchableInfo> mSearchablesMap = null;
    private ArrayList<SearchableInfo> mSearchablesList = null;
    private ArrayList<SearchableInfo> mSearchablesInGlobalSearchList = null;
    private ComponentName mCurrentGlobalSearchActivity = null;
    private ComponentName mWebSearchActivity = null;
    private final IPackageManager mPm = AppGlobals.getPackageManager();

    public Searchables(Context context, int userId) {
        this.mContext = context;
        this.mUserId = userId;
    }

    public SearchableInfo getSearchableInfo(ComponentName activity) {
        ComponentName referredActivity;
        SearchableInfo result;
        Bundle md;
        synchronized (this) {
            SearchableInfo result2 = this.mSearchablesMap.get(activity);
            if (result2 != null) {
                PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                if (pm.canAccessComponent(Binder.getCallingUid(), result2.getSearchActivity(), UserHandle.getCallingUserId())) {
                    return result2;
                }
                return null;
            }
            try {
                ActivityInfo ai = this.mPm.getActivityInfo(activity, 128L, this.mUserId);
                String refActivityName = null;
                if (ai == null) {
                    return null;
                }
                Bundle md2 = ai.metaData;
                if (md2 != null) {
                    refActivityName = md2.getString(MD_LABEL_DEFAULT_SEARCHABLE);
                }
                if (refActivityName == null && (md = ai.applicationInfo.metaData) != null) {
                    refActivityName = md.getString(MD_LABEL_DEFAULT_SEARCHABLE);
                }
                if (refActivityName == null || refActivityName.equals(MD_SEARCHABLE_SYSTEM_SEARCH)) {
                    return null;
                }
                String pkg = activity.getPackageName();
                if (refActivityName.charAt(0) == '.') {
                    referredActivity = new ComponentName(pkg, pkg + refActivityName);
                } else {
                    referredActivity = new ComponentName(pkg, refActivityName);
                }
                synchronized (this) {
                    result = this.mSearchablesMap.get(referredActivity);
                    if (result != null) {
                        this.mSearchablesMap.put(activity, result);
                    }
                }
                if (result != null) {
                    PackageManagerInternal pm2 = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                    if (pm2.canAccessComponent(Binder.getCallingUid(), result.getSearchActivity(), UserHandle.getCallingUserId())) {
                        return result;
                    }
                    return null;
                }
                return null;
            } catch (RemoteException re) {
                Log.e(LOG_TAG, "Error getting activity info " + re);
                return null;
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:37:0x00bd A[Catch: all -> 0x00cf, TRY_ENTER, TryCatch #4 {all -> 0x00d2, blocks: (B:35:0x00ad, B:36:0x00bc, B:26:0x0072, B:28:0x0081, B:30:0x008b, B:32:0x009b, B:33:0x009e, B:37:0x00bd, B:38:0x00c9), top: B:54:0x0072 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void updateSearchableList() {
        ResolveInfo info;
        SearchableInfo searchable;
        HashMap<ComponentName, SearchableInfo> newSearchablesMap = new HashMap<>();
        ArrayList<SearchableInfo> newSearchablesList = new ArrayList<>();
        ArrayList<SearchableInfo> newSearchablesInGlobalSearchList = new ArrayList<>();
        Intent intent = new Intent("android.intent.action.SEARCH");
        long ident = Binder.clearCallingIdentity();
        try {
            List<ResolveInfo> searchList = queryIntentActivities(intent, 268435584);
            Intent webSearchIntent = new Intent("android.intent.action.WEB_SEARCH");
            List<ResolveInfo> webSearchInfoList = queryIntentActivities(webSearchIntent, 268435584);
            if (searchList == null && webSearchInfoList == null) {
                List<ResolveInfo> newGlobalSearchActivities = findGlobalSearchActivities();
                ComponentName newGlobalSearchActivity = findGlobalSearchActivity(newGlobalSearchActivities);
                ComponentName newWebSearchActivity = findWebSearchActivity(newGlobalSearchActivity);
                synchronized (this) {
                    this.mSearchablesMap = newSearchablesMap;
                    this.mSearchablesList = newSearchablesList;
                    this.mSearchablesInGlobalSearchList = newSearchablesInGlobalSearchList;
                    this.mGlobalSearchActivities = newGlobalSearchActivities;
                    this.mCurrentGlobalSearchActivity = newGlobalSearchActivity;
                    this.mWebSearchActivity = newWebSearchActivity;
                }
                Binder.restoreCallingIdentity(ident);
                return;
            }
            int web_search_count = 0;
            int search_count = searchList == null ? 0 : searchList.size();
            if (webSearchInfoList != null) {
                web_search_count = webSearchInfoList.size();
            }
            int count = search_count + web_search_count;
            int ii = 0;
            while (ii < count) {
                if (ii < search_count) {
                    try {
                        info = searchList.get(ii);
                    } catch (Throwable th) {
                        th = th;
                        Binder.restoreCallingIdentity(ident);
                        throw th;
                    }
                } else {
                    info = webSearchInfoList.get(ii - search_count);
                }
                ActivityInfo ai = info.activityInfo;
                int web_search_count2 = web_search_count;
                Intent intent2 = intent;
                try {
                    List<ResolveInfo> searchList2 = searchList;
                    if (newSearchablesMap.get(new ComponentName(ai.packageName, ai.name)) == null && (searchable = SearchableInfo.getActivityMetaData(this.mContext, ai, this.mUserId)) != null) {
                        newSearchablesList.add(searchable);
                        newSearchablesMap.put(searchable.getSearchActivity(), searchable);
                        if (searchable.shouldIncludeInGlobalSearch()) {
                            newSearchablesInGlobalSearchList.add(searchable);
                        }
                    }
                    ii++;
                    web_search_count = web_search_count2;
                    intent = intent2;
                    searchList = searchList2;
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            }
            List<ResolveInfo> newGlobalSearchActivities2 = findGlobalSearchActivities();
            ComponentName newGlobalSearchActivity2 = findGlobalSearchActivity(newGlobalSearchActivities2);
            ComponentName newWebSearchActivity2 = findWebSearchActivity(newGlobalSearchActivity2);
            synchronized (this) {
            }
        } catch (Throwable th3) {
            th = th3;
        }
    }

    private List<ResolveInfo> findGlobalSearchActivities() {
        Intent intent = new Intent("android.search.action.GLOBAL_SEARCH");
        List<ResolveInfo> activities = queryIntentActivities(intent, 268500992);
        if (activities != null && !activities.isEmpty()) {
            Collections.sort(activities, GLOBAL_SEARCH_RANKER);
        }
        return activities;
    }

    private ComponentName findGlobalSearchActivity(List<ResolveInfo> installed) {
        ComponentName globalSearchComponent;
        String searchProviderSetting = getGlobalSearchProviderSetting();
        if (!TextUtils.isEmpty(searchProviderSetting) && (globalSearchComponent = ComponentName.unflattenFromString(searchProviderSetting)) != null && isInstalled(globalSearchComponent)) {
            return globalSearchComponent;
        }
        return getDefaultGlobalSearchProvider(installed);
    }

    private boolean isInstalled(ComponentName globalSearch) {
        Intent intent = new Intent("android.search.action.GLOBAL_SEARCH");
        intent.setComponent(globalSearch);
        List<ResolveInfo> activities = queryIntentActivities(intent, 65536);
        if (activities != null && !activities.isEmpty()) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean isSystemApp(ResolveInfo res) {
        return (res.activityInfo.applicationInfo.flags & 1) != 0;
    }

    private ComponentName getDefaultGlobalSearchProvider(List<ResolveInfo> providerList) {
        if (providerList != null && !providerList.isEmpty()) {
            ActivityInfo ai = providerList.get(0).activityInfo;
            return new ComponentName(ai.packageName, ai.name);
        }
        Log.w(LOG_TAG, "No global search activity found");
        return null;
    }

    private String getGlobalSearchProviderSetting() {
        ContentResolver cr = this.mContext.getContentResolver();
        return Settings.Secure.getStringForUser(cr, "search_global_search_activity", cr.getUserId());
    }

    private ComponentName findWebSearchActivity(ComponentName globalSearchActivity) {
        if (globalSearchActivity == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.WEB_SEARCH");
        intent.setPackage(globalSearchActivity.getPackageName());
        List<ResolveInfo> activities = queryIntentActivities(intent, 65536);
        if (activities != null && !activities.isEmpty()) {
            ActivityInfo ai = activities.get(0).activityInfo;
            return new ComponentName(ai.packageName, ai.name);
        }
        Log.w(LOG_TAG, "No web search activity found");
        return null;
    }

    private List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        try {
            List<ResolveInfo> activities = this.mPm.queryIntentActivities(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 8388608 | flags, this.mUserId).getList();
            return activities;
        } catch (RemoteException e) {
            return null;
        }
    }

    public synchronized ArrayList<SearchableInfo> getSearchablesList() {
        return createFilterdSearchableInfoList(this.mSearchablesList);
    }

    public synchronized ArrayList<SearchableInfo> getSearchablesInGlobalSearchList() {
        return createFilterdSearchableInfoList(this.mSearchablesInGlobalSearchList);
    }

    public synchronized ArrayList<ResolveInfo> getGlobalSearchActivities() {
        return createFilterdResolveInfoList(this.mGlobalSearchActivities);
    }

    private ArrayList<SearchableInfo> createFilterdSearchableInfoList(List<SearchableInfo> list) {
        if (list == null) {
            return null;
        }
        ArrayList<SearchableInfo> resultList = new ArrayList<>(list.size());
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        for (SearchableInfo info : list) {
            if (pm.canAccessComponent(callingUid, info.getSearchActivity(), callingUserId)) {
                resultList.add(info);
            }
        }
        return resultList;
    }

    private ArrayList<ResolveInfo> createFilterdResolveInfoList(List<ResolveInfo> list) {
        if (list == null) {
            return null;
        }
        ArrayList<ResolveInfo> resultList = new ArrayList<>(list.size());
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        for (ResolveInfo info : list) {
            if (pm.canAccessComponent(callingUid, info.activityInfo.getComponentName(), callingUserId)) {
                resultList.add(info);
            }
        }
        return resultList;
    }

    public synchronized ComponentName getGlobalSearchActivity() {
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        ComponentName componentName = this.mCurrentGlobalSearchActivity;
        if (componentName != null && pm.canAccessComponent(callingUid, componentName, callingUserId)) {
            return this.mCurrentGlobalSearchActivity;
        }
        return null;
    }

    public synchronized ComponentName getWebSearchActivity() {
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        ComponentName componentName = this.mWebSearchActivity;
        if (componentName != null && pm.canAccessComponent(callingUid, componentName, callingUserId)) {
            return this.mWebSearchActivity;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Searchable authorities:");
        synchronized (this) {
            ArrayList<SearchableInfo> arrayList = this.mSearchablesList;
            if (arrayList != null) {
                Iterator<SearchableInfo> it = arrayList.iterator();
                while (it.hasNext()) {
                    SearchableInfo info = it.next();
                    pw.print("  ");
                    pw.println(info.getSuggestAuthority());
                }
            }
        }
    }
}
