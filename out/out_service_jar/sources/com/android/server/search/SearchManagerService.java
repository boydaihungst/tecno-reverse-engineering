package com.android.server.search;

import android.app.ISearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.search.SearchManagerService;
import com.android.server.statusbar.StatusBarManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
/* loaded from: classes2.dex */
public class SearchManagerService extends ISearchManager.Stub {
    private static final String TAG = "SearchManagerService";
    private final Context mContext;
    final Handler mHandler;
    private final SparseArray<Searchables> mSearchables = new SparseArray<>();

    /* loaded from: classes2.dex */
    public static class Lifecycle extends SystemService {
        private SearchManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.search.SearchManagerService$Lifecycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.search.SearchManagerService, android.os.IBinder] */
        @Override // com.android.server.SystemService
        public void onStart() {
            ?? searchManagerService = new SearchManagerService(getContext());
            this.mService = searchManagerService;
            publishBinderService("search", searchManagerService);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onUserUnlocking$0$com-android-server-search-SearchManagerService$Lifecycle  reason: not valid java name */
        public /* synthetic */ void m6403x46f6c19c(SystemService.TargetUser user) {
            this.mService.onUnlockUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(final SystemService.TargetUser user) {
            this.mService.mHandler.post(new Runnable() { // from class: com.android.server.search.SearchManagerService$Lifecycle$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SearchManagerService.Lifecycle.this.m6403x46f6c19c(user);
                }
            });
        }

        @Override // com.android.server.SystemService
        public void onUserStopped(SystemService.TargetUser user) {
            this.mService.onCleanupUser(user.getUserIdentifier());
        }
    }

    public SearchManagerService(Context context) {
        this.mContext = context;
        new MyPackageMonitor().register(context, null, UserHandle.ALL, true);
        new GlobalSearchProviderObserver(context.getContentResolver());
        this.mHandler = BackgroundThread.getHandler();
    }

    private Searchables getSearchables(int userId) {
        return getSearchables(userId, false);
    }

    private Searchables getSearchables(int userId, boolean forceUpdate) {
        Searchables searchables;
        long token = Binder.clearCallingIdentity();
        try {
            UserManager um = (UserManager) this.mContext.getSystemService(UserManager.class);
            if (um.getUserInfo(userId) == null) {
                throw new IllegalStateException("User " + userId + " doesn't exist");
            }
            if (!um.isUserUnlockingOrUnlocked(userId)) {
                throw new IllegalStateException("User " + userId + " isn't unlocked");
            }
            Binder.restoreCallingIdentity(token);
            synchronized (this.mSearchables) {
                searchables = this.mSearchables.get(userId);
                if (searchables == null) {
                    searchables = new Searchables(this.mContext, userId);
                    searchables.updateSearchableList();
                    this.mSearchables.append(userId, searchables);
                } else if (forceUpdate) {
                    searchables.updateSearchableList();
                }
            }
            return searchables;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUnlockUser(int userId) {
        try {
            getSearchables(userId, true);
        } catch (IllegalStateException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCleanupUser(int userId) {
        synchronized (this.mSearchables) {
            this.mSearchables.remove(userId);
        }
    }

    /* loaded from: classes2.dex */
    class MyPackageMonitor extends PackageMonitor {
        MyPackageMonitor() {
        }

        public void onSomePackagesChanged() {
            updateSearchables();
        }

        public void onPackageModified(String pkg) {
            updateSearchables();
        }

        private void updateSearchables() {
            int changingUserId = getChangingUserId();
            synchronized (SearchManagerService.this.mSearchables) {
                int i = 0;
                while (true) {
                    if (i >= SearchManagerService.this.mSearchables.size()) {
                        break;
                    } else if (changingUserId != SearchManagerService.this.mSearchables.keyAt(i)) {
                        i++;
                    } else {
                        ((Searchables) SearchManagerService.this.mSearchables.valueAt(i)).updateSearchableList();
                        break;
                    }
                }
            }
            Intent intent = new Intent("android.search.action.SEARCHABLES_CHANGED");
            intent.addFlags(603979776);
            SearchManagerService.this.mContext.sendBroadcastAsUser(intent, new UserHandle(changingUserId));
        }
    }

    /* loaded from: classes2.dex */
    class GlobalSearchProviderObserver extends ContentObserver {
        private final ContentResolver mResolver;

        public GlobalSearchProviderObserver(ContentResolver resolver) {
            super(null);
            this.mResolver = resolver;
            resolver.registerContentObserver(Settings.Secure.getUriFor("search_global_search_activity"), false, this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            synchronized (SearchManagerService.this.mSearchables) {
                for (int i = 0; i < SearchManagerService.this.mSearchables.size(); i++) {
                    ((Searchables) SearchManagerService.this.mSearchables.valueAt(i)).updateSearchableList();
                }
            }
            Intent intent = new Intent("android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED");
            intent.addFlags(536870912);
            SearchManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    public SearchableInfo getSearchableInfo(ComponentName launchActivity) {
        if (launchActivity == null) {
            Log.e(TAG, "getSearchableInfo(), activity == null");
            return null;
        }
        return getSearchables(UserHandle.getCallingUserId()).getSearchableInfo(launchActivity);
    }

    public List<SearchableInfo> getSearchablesInGlobalSearch() {
        return getSearchables(UserHandle.getCallingUserId()).getSearchablesInGlobalSearchList();
    }

    public List<ResolveInfo> getGlobalSearchActivities() {
        return getSearchables(UserHandle.getCallingUserId()).getGlobalSearchActivities();
    }

    public ComponentName getGlobalSearchActivity() {
        return getSearchables(UserHandle.getCallingUserId()).getGlobalSearchActivity();
    }

    public ComponentName getWebSearchActivity() {
        return getSearchables(UserHandle.getCallingUserId()).getWebSearchActivity();
    }

    public void launchAssist(int userHandle, Bundle args) {
        StatusBarManagerInternal statusBarManager = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
        if (statusBarManager != null) {
            statusBarManager.startAssist(args);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            PrintWriter indentingPrintWriter = new IndentingPrintWriter(pw, "  ");
            synchronized (this.mSearchables) {
                for (int i = 0; i < this.mSearchables.size(); i++) {
                    indentingPrintWriter.print("\nUser: ");
                    indentingPrintWriter.println(this.mSearchables.keyAt(i));
                    indentingPrintWriter.increaseIndent();
                    this.mSearchables.valueAt(i).dump(fd, indentingPrintWriter, args);
                    indentingPrintWriter.decreaseIndent();
                }
            }
        }
    }
}
