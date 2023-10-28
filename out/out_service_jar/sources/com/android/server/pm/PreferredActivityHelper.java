package com.android.server.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.LogPrinter;
import android.util.PrintStreamPrinter;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.snapshot.PackageDataSnapshot;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class PreferredActivityHelper {
    private static final String TAG_DEFAULT_APPS = "da";
    private static final String TAG_PREFERRED_BACKUP = "pa";
    private final PackageManagerService mPm;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public interface BlobXmlRestorer {
        void apply(TypedXmlPullParser typedXmlPullParser, int i) throws IOException, XmlPullParserException;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PreferredActivityHelper(PackageManagerService pm) {
        this.mPm = pm;
    }

    private ResolveInfo findPreferredActivityNotLocked(Computer snapshot, Intent intent, String resolvedType, long flags, List<ResolveInfo> query, boolean always, boolean removeMatches, boolean debug, int userId) {
        return findPreferredActivityNotLocked(snapshot, intent, resolvedType, flags, query, always, removeMatches, debug, userId, UserHandle.getAppId(Binder.getCallingUid()) >= 10000);
    }

    public ResolveInfo findPreferredActivityNotLocked(Computer snapshot, Intent intent, String resolvedType, long flags, List<ResolveInfo> query, boolean always, boolean removeMatches, boolean debug, int userId, boolean queryMayBeFiltered) {
        if (Thread.holdsLock(this.mPm.mLock)) {
            Slog.wtf("PackageManager", "Calling thread " + Thread.currentThread().getName() + " is holding mLock", new Throwable());
        }
        if (this.mPm.mUserManager.exists(userId)) {
            PackageManagerService.FindPreferredActivityBodyResult body = snapshot.findPreferredActivityInternal(intent, resolvedType, flags, query, always, removeMatches, debug, userId, queryMayBeFiltered);
            if (body.mChanged) {
                if (PackageManagerService.DEBUG_PREFERRED) {
                    Slog.v("PackageManager", "Preferred activity bookkeeping changed; writing restrictions");
                }
                this.mPm.scheduleWritePackageRestrictions(userId);
            }
            if ((PackageManagerService.DEBUG_PREFERRED || debug) && body.mPreferredResolveInfo == null) {
                Slog.v("PackageManager", "No preferred activity to return");
            }
            if (intent.getAction() != null && "android.intent.action.WEB_SEARCH".equals(intent.getAction())) {
                int N = query.size();
                for (int i = 0; i < N; i++) {
                    ResolveInfo ri2 = query.get(i);
                    if ("com.google.android.googlequicksearchbox".equals(ri2.activityInfo.applicationInfo.packageName) || "com.google.android.apps.searchlite".equals(ri2.activityInfo.applicationInfo.packageName)) {
                        return ri2;
                    }
                }
            }
            return body.mPreferredResolveInfo;
        }
        return null;
    }

    public void clearPackagePreferredActivities(String packageName, int userId) {
        SparseBooleanArray changedUsers = new SparseBooleanArray();
        synchronized (this.mPm.mLock) {
            this.mPm.clearPackagePreferredActivitiesLPw(packageName, changedUsers, userId);
        }
        if (changedUsers.size() > 0) {
            updateDefaultHomeNotLocked(this.mPm.snapshotComputer(), changedUsers);
            this.mPm.postPreferredActivityChangedBroadcast(userId);
            this.mPm.scheduleWritePackageRestrictions(userId);
        }
    }

    public boolean updateDefaultHomeNotLocked(Computer snapshot, final int userId) {
        if (Thread.holdsLock(this.mPm.mLock)) {
            Slog.wtf("PackageManager", "Calling thread " + Thread.currentThread().getName() + " is holding mLock", new Throwable());
        }
        if (this.mPm.isSystemReady()) {
            Intent intent = snapshot.getHomeIntent();
            List<ResolveInfo> resolveInfos = snapshot.queryIntentActivitiesInternal(intent, null, 786432L, userId);
            ResolveInfo preferredResolveInfo = findPreferredActivityNotLocked(snapshot, intent, null, 0L, resolveInfos, true, false, false, userId);
            String packageName = (preferredResolveInfo == null || preferredResolveInfo.activityInfo == null) ? null : preferredResolveInfo.activityInfo.packageName;
            String currentPackageName = this.mPm.getActiveLauncherPackageName(userId);
            if (TextUtils.equals(currentPackageName, packageName)) {
                return false;
            }
            String[] callingPackages = snapshot.getPackagesForUid(Binder.getCallingUid());
            if ((callingPackages == null || !ArrayUtils.contains(callingPackages, this.mPm.mRequiredPermissionControllerPackage)) && packageName != null) {
                return this.mPm.setActiveLauncherPackage(packageName, userId, new Consumer() { // from class: com.android.server.pm.PreferredActivityHelper$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        PreferredActivityHelper.this.m5600xa9798c59(userId, (Boolean) obj);
                    }
                });
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateDefaultHomeNotLocked$0$com-android-server-pm-PreferredActivityHelper  reason: not valid java name */
    public /* synthetic */ void m5600xa9798c59(int userId, Boolean successful) {
        if (successful.booleanValue()) {
            this.mPm.postPreferredActivityChangedBroadcast(userId);
        }
    }

    public void addPreferredActivity(Computer snapshot, WatchedIntentFilter filter, int match, ComponentName[] set, ComponentName activity, boolean always, int userId, String opname, boolean removeExisting) {
        int callingUid = Binder.getCallingUid();
        snapshot.enforceCrossUserPermission(callingUid, userId, true, false, "add preferred activity");
        if (this.mPm.mContext.checkCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS") != 0) {
            if (snapshot.getUidTargetSdkVersion(callingUid) < 8) {
                Slog.w("PackageManager", "Ignoring addPreferredActivity() from uid " + callingUid);
                return;
            }
            this.mPm.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
        }
        if (filter.countActions() == 0) {
            Slog.w("PackageManager", "Cannot set a preferred activity with no filter actions");
            return;
        }
        if (PackageManagerService.DEBUG_PREFERRED) {
            Slog.i("PackageManager", opname + " activity " + activity.flattenToShortString() + " for user " + userId + ":");
            filter.dump(new LogPrinter(4, "PackageManager"), "  ");
        }
        synchronized (this.mPm.mLock) {
            try {
                try {
                    PreferredIntentResolver pir = this.mPm.mSettings.editPreferredActivitiesLPw(userId);
                    ArrayList<PreferredActivity> existing = pir.findFilters(filter);
                    if (removeExisting && existing != null) {
                        try {
                            Settings.removeFilters(pir, filter, existing);
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    pir.addFilter((PackageDataSnapshot) this.mPm.snapshotComputer(), (Computer) new PreferredActivity(filter, match, set, activity, always));
                    this.mPm.scheduleWritePackageRestrictions(userId);
                    if (!isHomeFilter(filter) || !updateDefaultHomeNotLocked(this.mPm.snapshotComputer(), userId)) {
                        this.mPm.postPreferredActivityChangedBroadcast(userId);
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [313=4] */
    public void replacePreferredActivity(Computer snapshot, WatchedIntentFilter filter, int match, ComponentName[] set, ComponentName activity, int userId) {
        if (filter.countActions() != 1) {
            throw new IllegalArgumentException("replacePreferredActivity expects filter to have only 1 action.");
        }
        if (filter.countDataAuthorities() == 0 && filter.countDataPaths() == 0 && filter.countDataSchemes() <= 1 && filter.countDataTypes() == 0) {
            int callingUid = Binder.getCallingUid();
            snapshot.enforceCrossUserPermission(callingUid, userId, true, false, "replace preferred activity");
            if (this.mPm.mContext.checkCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS") != 0) {
                synchronized (this.mPm.mLock) {
                    if (this.mPm.snapshotComputer().getUidTargetSdkVersion(callingUid) < 8) {
                        Slog.w("PackageManager", "Ignoring replacePreferredActivity() from uid " + Binder.getCallingUid());
                        return;
                    }
                    this.mPm.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
                }
            }
            synchronized (this.mPm.mLock) {
                try {
                    try {
                        PreferredIntentResolver pir = this.mPm.mSettings.getPreferredActivities(userId);
                        if (pir != null) {
                            try {
                                ArrayList<PreferredActivity> existing = pir.findFilters(filter);
                                if (existing != null && existing.size() == 1) {
                                    PreferredActivity cur = existing.get(0);
                                    if (PackageManagerService.DEBUG_PREFERRED) {
                                        Slog.i("PackageManager", "Checking replace of preferred:");
                                        filter.dump(new LogPrinter(4, "PackageManager"), "  ");
                                        if (cur.mPref.mAlways) {
                                            Slog.i("PackageManager", "  -- CUR: mMatch=" + cur.mPref.mMatch);
                                            Slog.i("PackageManager", "  -- CUR: mSet=" + Arrays.toString(cur.mPref.mSetComponents));
                                            Slog.i("PackageManager", "  -- CUR: mComponent=" + cur.mPref.mShortComponent);
                                            Slog.i("PackageManager", "  -- NEW: mMatch=" + (match & 268369920));
                                            Slog.i("PackageManager", "  -- CUR: mSet=" + Arrays.toString(set));
                                            Slog.i("PackageManager", "  -- CUR: mComponent=" + activity.flattenToShortString());
                                        } else {
                                            Slog.i("PackageManager", "  -- CUR; not mAlways!");
                                        }
                                    }
                                    if (cur.mPref.mAlways) {
                                        try {
                                            if (cur.mPref.mComponent.equals(activity) && cur.mPref.mMatch == (match & 268369920)) {
                                                if (cur.mPref.sameSet(set)) {
                                                    if (PackageManagerService.DEBUG_PREFERRED) {
                                                        Slog.i("PackageManager", "Replacing with same preferred activity " + cur.mPref.mShortComponent + " for user " + userId + ":");
                                                        filter.dump(new LogPrinter(4, "PackageManager"), "  ");
                                                    }
                                                    return;
                                                }
                                            }
                                        } catch (Throwable th) {
                                            th = th;
                                            throw th;
                                        }
                                    }
                                }
                                if (existing != null) {
                                    Settings.removeFilters(pir, filter, existing);
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        }
                        addPreferredActivity(this.mPm.snapshotComputer(), filter, match, set, activity, true, userId, "Replacing preferred", false);
                        return;
                    } catch (Throwable th3) {
                        th = th3;
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
            }
        }
        throw new IllegalArgumentException("replacePreferredActivity expects filter to have no data authorities, paths, or types; and at most one scheme.");
    }

    public void clearPackagePreferredActivities(Computer snapshot, String packageName) {
        int callingUid = Binder.getCallingUid();
        if (snapshot.getInstantAppPackageName(callingUid) != null) {
            return;
        }
        PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
        if ((packageState == null || !snapshot.isCallerSameApp(packageName, callingUid)) && this.mPm.mContext.checkCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS") != 0) {
            if (snapshot.getUidTargetSdkVersion(callingUid) < 8) {
                Slog.w("PackageManager", "Ignoring clearPackagePreferredActivities() from uid " + callingUid);
                return;
            }
            this.mPm.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
        }
        if (packageState != null && snapshot.shouldFilterApplication(packageState, callingUid, UserHandle.getUserId(callingUid))) {
            return;
        }
        int callingUserId = UserHandle.getCallingUserId();
        clearPackagePreferredActivities(packageName, callingUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateDefaultHomeNotLocked(Computer snapshot, SparseBooleanArray userIds) {
        if (Thread.holdsLock(this.mPm.mLock)) {
            Slog.wtf("PackageManager", "Calling thread " + Thread.currentThread().getName() + " is holding mLock", new Throwable());
        }
        for (int i = userIds.size() - 1; i >= 0; i--) {
            int userId = userIds.keyAt(i);
            updateDefaultHomeNotLocked(snapshot, userId);
        }
    }

    public void setHomeActivity(Computer snapshot, ComponentName comp, int userId) {
        if (snapshot.getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return;
        }
        ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
        snapshot.getHomeActivitiesAsUser(homeActivities, userId);
        boolean found = false;
        int size = homeActivities.size();
        ComponentName[] set = new ComponentName[size];
        for (int i = 0; i < size; i++) {
            ResolveInfo candidate = homeActivities.get(i);
            ActivityInfo info = candidate.activityInfo;
            ComponentName activityName = new ComponentName(info.packageName, info.name);
            set[i] = activityName;
            if (!found && activityName.equals(comp)) {
                found = true;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Component " + comp + " cannot be home on user " + userId);
        }
        replacePreferredActivity(snapshot, getHomeFilter(), 1048576, set, comp, userId);
    }

    private WatchedIntentFilter getHomeFilter() {
        WatchedIntentFilter filter = new WatchedIntentFilter("android.intent.action.MAIN");
        filter.addCategory("android.intent.category.HOME");
        filter.addCategory("android.intent.category.DEFAULT");
        return filter;
    }

    public void addPersistentPreferredActivity(WatchedIntentFilter filter, ComponentName activity, int userId) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000) {
            throw new SecurityException("addPersistentPreferredActivity can only be run by the system");
        }
        if (filter.countActions() == 0) {
            Slog.w("PackageManager", "Cannot set a preferred activity with no filter actions");
            return;
        }
        if (PackageManagerService.DEBUG_PREFERRED) {
            Slog.i("PackageManager", "Adding persistent preferred activity " + activity + " for user " + userId + ":");
            filter.dump(new LogPrinter(4, "PackageManager"), "  ");
        }
        synchronized (this.mPm.mLock) {
            this.mPm.mSettings.editPersistentPreferredActivitiesLPw(userId).addFilter((PackageDataSnapshot) this.mPm.snapshotComputer(), (Computer) new PersistentPreferredActivity(filter, activity, true));
            this.mPm.scheduleWritePackageRestrictions(userId);
        }
        if (isHomeFilter(filter)) {
            updateDefaultHomeNotLocked(this.mPm.snapshotComputer(), userId);
        }
        this.mPm.postPreferredActivityChangedBroadcast(userId);
    }

    public void clearPackagePersistentPreferredActivities(String packageName, int userId) {
        boolean changed;
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000) {
            throw new SecurityException("clearPackagePersistentPreferredActivities can only be run by the system");
        }
        synchronized (this.mPm.mLock) {
            changed = this.mPm.mSettings.clearPackagePersistentPreferredActivities(packageName, userId);
        }
        if (changed) {
            updateDefaultHomeNotLocked(this.mPm.snapshotComputer(), userId);
            this.mPm.postPreferredActivityChangedBroadcast(userId);
            this.mPm.scheduleWritePackageRestrictions(userId);
        }
    }

    private boolean isHomeFilter(WatchedIntentFilter filter) {
        return filter.hasAction("android.intent.action.MAIN") && filter.hasCategory("android.intent.category.HOME") && filter.hasCategory("android.intent.category.DEFAULT");
    }

    private void restoreFromXml(TypedXmlPullParser parser, int userId, String expectedStartTag, BlobXmlRestorer functor) throws IOException, XmlPullParserException {
        int type;
        do {
            type = parser.next();
            if (type == 2) {
                break;
            }
        } while (type != 1);
        if (type != 2) {
            if (PackageManagerService.DEBUG_BACKUP) {
                Slog.e("PackageManager", "Didn't find start tag during restore");
            }
        } else if (!expectedStartTag.equals(parser.getName())) {
            if (PackageManagerService.DEBUG_BACKUP) {
                Slog.e("PackageManager", "Found unexpected tag " + parser.getName());
            }
        } else {
            do {
            } while (parser.next() == 4);
            functor.apply(parser, userId);
        }
    }

    public byte[] getPreferredActivityBackup(int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getPreferredActivityBackup()");
        }
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        try {
            TypedXmlSerializer serializer = Xml.newFastSerializer();
            serializer.setOutput(dataStream, StandardCharsets.UTF_8.name());
            serializer.startDocument((String) null, true);
            serializer.startTag((String) null, TAG_PREFERRED_BACKUP);
            synchronized (this.mPm.mLock) {
                this.mPm.mSettings.writePreferredActivitiesLPr(serializer, userId, true);
            }
            serializer.endTag((String) null, TAG_PREFERRED_BACKUP);
            serializer.endDocument();
            serializer.flush();
            return dataStream.toByteArray();
        } catch (Exception e) {
            if (PackageManagerService.DEBUG_BACKUP) {
                Slog.e("PackageManager", "Unable to write preferred activities for backup", e);
            }
            return null;
        }
    }

    public void restorePreferredActivities(byte[] backup, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restorePreferredActivities()");
        }
        try {
            TypedXmlPullParser parser = Xml.newFastPullParser();
            parser.setInput(new ByteArrayInputStream(backup), StandardCharsets.UTF_8.name());
            restoreFromXml(parser, userId, TAG_PREFERRED_BACKUP, new BlobXmlRestorer() { // from class: com.android.server.pm.PreferredActivityHelper$$ExternalSyntheticLambda0
                @Override // com.android.server.pm.PreferredActivityHelper.BlobXmlRestorer
                public final void apply(TypedXmlPullParser typedXmlPullParser, int i) {
                    PreferredActivityHelper.this.m5599xbf6b5d34(typedXmlPullParser, i);
                }
            });
        } catch (Exception e) {
            if (PackageManagerService.DEBUG_BACKUP) {
                Slog.e("PackageManager", "Exception restoring preferred activities: " + e.getMessage());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$restorePreferredActivities$1$com-android-server-pm-PreferredActivityHelper  reason: not valid java name */
    public /* synthetic */ void m5599xbf6b5d34(TypedXmlPullParser readParser, int readUserId) throws IOException, XmlPullParserException {
        synchronized (this.mPm.mLock) {
            this.mPm.mSettings.readPreferredActivitiesLPw(readParser, readUserId);
        }
        updateDefaultHomeNotLocked(this.mPm.snapshotComputer(), readUserId);
    }

    public byte[] getDefaultAppsBackup(int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getDefaultAppsBackup()");
        }
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        try {
            XmlSerializer newFastSerializer = Xml.newFastSerializer();
            newFastSerializer.setOutput(dataStream, StandardCharsets.UTF_8.name());
            newFastSerializer.startDocument((String) null, true);
            newFastSerializer.startTag((String) null, TAG_DEFAULT_APPS);
            synchronized (this.mPm.mLock) {
                this.mPm.mSettings.writeDefaultAppsLPr(newFastSerializer, userId);
            }
            newFastSerializer.endTag((String) null, TAG_DEFAULT_APPS);
            newFastSerializer.endDocument();
            newFastSerializer.flush();
            return dataStream.toByteArray();
        } catch (Exception e) {
            if (PackageManagerService.DEBUG_BACKUP) {
                Slog.e("PackageManager", "Unable to write default apps for backup", e);
            }
            return null;
        }
    }

    public void restoreDefaultApps(byte[] backup, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restoreDefaultApps()");
        }
        try {
            TypedXmlPullParser parser = Xml.newFastPullParser();
            parser.setInput(new ByteArrayInputStream(backup), StandardCharsets.UTF_8.name());
            restoreFromXml(parser, userId, TAG_DEFAULT_APPS, new BlobXmlRestorer() { // from class: com.android.server.pm.PreferredActivityHelper$$ExternalSyntheticLambda2
                @Override // com.android.server.pm.PreferredActivityHelper.BlobXmlRestorer
                public final void apply(TypedXmlPullParser typedXmlPullParser, int i) {
                    PreferredActivityHelper.this.m5598x45a4597a(typedXmlPullParser, i);
                }
            });
        } catch (Exception e) {
            if (PackageManagerService.DEBUG_BACKUP) {
                Slog.e("PackageManager", "Exception restoring default apps: " + e.getMessage());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$restoreDefaultApps$2$com-android-server-pm-PreferredActivityHelper  reason: not valid java name */
    public /* synthetic */ void m5598x45a4597a(TypedXmlPullParser parser1, int userId1) throws IOException, XmlPullParserException {
        String defaultBrowser;
        synchronized (this.mPm.mLock) {
            this.mPm.mSettings.readDefaultAppsLPw(parser1, userId1);
            defaultBrowser = this.mPm.mSettings.removeDefaultBrowserPackageNameLPw(userId1);
        }
        if (defaultBrowser != null) {
            this.mPm.setDefaultBrowser(defaultBrowser, false, userId1);
        }
    }

    public void resetApplicationPreferences(int userId) {
        this.mPm.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
        long identity = Binder.clearCallingIdentity();
        try {
            SparseBooleanArray changedUsers = new SparseBooleanArray();
            synchronized (this.mPm.mLock) {
                this.mPm.clearPackagePreferredActivitiesLPw(null, changedUsers, userId);
            }
            if (changedUsers.size() > 0) {
                this.mPm.postPreferredActivityChangedBroadcast(userId);
            }
            synchronized (this.mPm.mLock) {
                this.mPm.mSettings.applyDefaultPreferredAppsLPw(userId);
                this.mPm.mDomainVerificationManager.clearUser(userId);
                int numPackages = this.mPm.mPackages.size();
                for (int i = 0; i < numPackages; i++) {
                    AndroidPackage pkg = this.mPm.mPackages.valueAt(i);
                    this.mPm.mPermissionManager.resetRuntimePermissions(pkg, userId);
                }
            }
            ITranPackageManagerService.Instance().applyFactoryDefaultGallerLPw(userId);
            ITranPackageManagerService.Instance().applyFactoryDefaultMusicLPw(userId);
            updateDefaultHomeNotLocked(this.mPm.snapshotComputer(), userId);
            resetNetworkPolicies(userId);
            this.mPm.scheduleWritePackageRestrictions(userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void resetNetworkPolicies(int userId) {
        ((NetworkPolicyManagerInternal) this.mPm.mInjector.getLocalService(NetworkPolicyManagerInternal.class)).resetUserState(userId);
    }

    public int getPreferredActivities(Computer snapshot, List<IntentFilter> outFilters, List<ComponentName> outActivities, String packageName) {
        List<WatchedIntentFilter> temp = WatchedIntentFilter.toWatchedIntentFilterList(outFilters);
        int result = getPreferredActivitiesInternal(snapshot, temp, outActivities, packageName);
        outFilters.clear();
        for (int i = 0; i < temp.size(); i++) {
            outFilters.add(temp.get(i).getIntentFilter());
        }
        return result;
    }

    private int getPreferredActivitiesInternal(Computer snapshot, List<WatchedIntentFilter> outFilters, List<ComponentName> outActivities, String packageName) {
        int userId;
        PreferredIntentResolver pir;
        int callingUid = Binder.getCallingUid();
        if (snapshot.getInstantAppPackageName(callingUid) == null && (pir = snapshot.getPreferredActivities((userId = UserHandle.getCallingUserId()))) != null) {
            Iterator<F> filterIterator = pir.filterIterator();
            while (filterIterator.hasNext()) {
                PreferredActivity pa = (PreferredActivity) filterIterator.next();
                String prefPackageName = pa.mPref.mComponent.getPackageName();
                if (packageName == null || (prefPackageName.equals(packageName) && pa.mPref.mAlways)) {
                    if (!snapshot.shouldFilterApplication(snapshot.getPackageStateInternal(prefPackageName), callingUid, userId)) {
                        if (outFilters != null) {
                            outFilters.add(new WatchedIntentFilter(pa.getIntentFilter()));
                        }
                        if (outActivities != null) {
                            outActivities.add(pa.mPref.mComponent);
                        }
                    }
                }
            }
        }
        return 0;
    }

    public ResolveInfo findPersistentPreferredActivity(Computer snapshot, Intent intent, int userId) {
        if (UserHandle.isSameApp(Binder.getCallingUid(), 1000)) {
            if (!this.mPm.mUserManager.exists(userId)) {
                return null;
            }
            int callingUid = Binder.getCallingUid();
            Intent intent2 = PackageManagerServiceUtils.updateIntentForResolve(intent);
            String resolvedType = intent2.resolveTypeIfNeeded(this.mPm.mContext.getContentResolver());
            long flags = snapshot.updateFlagsForResolve(0L, userId, callingUid, false, snapshot.isImplicitImageCaptureIntentAndNotSetByDpc(intent2, userId, resolvedType, 0L));
            List<ResolveInfo> query = snapshot.queryIntentActivitiesInternal(intent2, resolvedType, flags, userId);
            return snapshot.findPersistentPreferredActivity(intent2, resolvedType, flags, query, false, userId);
        }
        throw new SecurityException("findPersistentPreferredActivity can only be run by the system");
    }

    public void setLastChosenActivity(Computer snapshot, Intent intent, String resolvedType, int flags, WatchedIntentFilter filter, int match, ComponentName activity) {
        if (snapshot.getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return;
        }
        int userId = UserHandle.getCallingUserId();
        if (PackageManagerService.DEBUG_PREFERRED) {
            Log.v("PackageManager", "setLastChosenActivity intent=" + intent + " resolvedType=" + resolvedType + " flags=" + flags + " filter=" + filter + " match=" + match + " activity=" + activity);
            filter.dump(new PrintStreamPrinter(System.out), "    ");
        }
        intent.setComponent(null);
        List<ResolveInfo> query = snapshot.queryIntentActivitiesInternal(intent, resolvedType, flags, userId);
        findPreferredActivityNotLocked(snapshot, intent, resolvedType, flags, query, false, true, false, userId);
        addPreferredActivity(snapshot, filter, match, null, activity, false, userId, "Setting last chosen", false);
    }

    public ResolveInfo getLastChosenActivity(Computer snapshot, Intent intent, String resolvedType, int flags) {
        if (snapshot.getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        int userId = UserHandle.getCallingUserId();
        if (PackageManagerService.DEBUG_PREFERRED) {
            Log.v("PackageManager", "Querying last chosen activity for " + intent);
        }
        List<ResolveInfo> query = snapshot.queryIntentActivitiesInternal(intent, resolvedType, flags, userId);
        return findPreferredActivityNotLocked(snapshot, intent, resolvedType, flags, query, false, false, false, userId);
    }
}
