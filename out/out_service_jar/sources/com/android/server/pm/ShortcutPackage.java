package com.android.server.pm;

import android.app.Person;
import android.app.appsearch.AppSearchBatchResult;
import android.app.appsearch.AppSearchManager;
import android.app.appsearch.AppSearchResult;
import android.app.appsearch.AppSearchSession;
import android.app.appsearch.BatchResultCallback;
import android.app.appsearch.GenericDocument;
import android.app.appsearch.GetByDocumentIdRequest;
import android.app.appsearch.PutDocumentsRequest;
import android.app.appsearch.RemoveByDocumentIdRequest;
import android.app.appsearch.ReportUsageRequest;
import android.app.appsearch.SearchResult;
import android.app.appsearch.SearchResults;
import android.app.appsearch.SearchSpec;
import android.app.appsearch.SetSchemaRequest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.LocusId;
import android.content.pm.AppSearchShortcutInfo;
import android.content.pm.AppSearchShortcutPerson;
import android.content.pm.PackageInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Build;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.ShareTargetInfo;
import com.android.server.pm.ShortcutPackage;
import com.android.server.pm.ShortcutService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import libcore.io.IoUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ShortcutPackage extends ShortcutPackageItem {
    private static final String ATTR_ACTIVITY = "activity";
    private static final String ATTR_BITMAP_PATH = "bitmap-path";
    private static final String ATTR_CALL_COUNT = "call-count";
    private static final String ATTR_DISABLED_MESSAGE = "dmessage";
    private static final String ATTR_DISABLED_MESSAGE_RES_ID = "dmessageid";
    private static final String ATTR_DISABLED_MESSAGE_RES_NAME = "dmessagename";
    private static final String ATTR_DISABLED_REASON = "disabled-reason";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_ICON_RES_ID = "icon-res";
    private static final String ATTR_ICON_RES_NAME = "icon-resname";
    private static final String ATTR_ICON_URI = "icon-uri";
    private static final String ATTR_ID = "id";
    private static final String ATTR_INTENT_LEGACY = "intent";
    private static final String ATTR_INTENT_NO_EXTRA = "intent-base";
    private static final String ATTR_LAST_RESET = "last-reset";
    private static final String ATTR_LOCUS_ID = "locus-id";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_NAME_XMLUTILS = "name";
    private static final String ATTR_PERSON_IS_BOT = "is-bot";
    private static final String ATTR_PERSON_IS_IMPORTANT = "is-important";
    private static final String ATTR_PERSON_KEY = "key";
    private static final String ATTR_PERSON_NAME = "name";
    private static final String ATTR_PERSON_URI = "uri";
    private static final String ATTR_RANK = "rank";
    private static final String ATTR_SCHEMA_VERSON = "schema-version";
    private static final String ATTR_SPLASH_SCREEN_THEME_NAME = "splash-screen-theme-name";
    private static final String ATTR_TEXT = "text";
    private static final String ATTR_TEXT_RES_ID = "textid";
    private static final String ATTR_TEXT_RES_NAME = "textname";
    private static final String ATTR_TIMESTAMP = "timestamp";
    private static final String ATTR_TITLE = "title";
    private static final String ATTR_TITLE_RES_ID = "titleid";
    private static final String ATTR_TITLE_RES_NAME = "titlename";
    private static final String KEY_BITMAPS = "bitmaps";
    private static final String KEY_BITMAP_BYTES = "bitmapBytes";
    private static final String KEY_DYNAMIC = "dynamic";
    private static final String KEY_MANIFEST = "manifest";
    private static final String KEY_PINNED = "pinned";
    private static final String NAME_CAPABILITY = "capability";
    private static final String NAME_CATEGORIES = "categories";
    private static final String TAG = "ShortcutService";
    private static final String TAG_CATEGORIES = "categories";
    private static final String TAG_EXTRAS = "extras";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_INTENT_EXTRAS_LEGACY = "intent-extras";
    private static final String TAG_MAP_XMLUTILS = "map";
    private static final String TAG_PERSON = "person";
    static final String TAG_ROOT = "package";
    private static final String TAG_SHARE_TARGET = "share-target";
    private static final String TAG_SHORTCUT = "shortcut";
    private static final String TAG_STRING_ARRAY_XMLUTILS = "string-array";
    private static final String TAG_VERIFY = "ShortcutService.verify";
    private int mApiCallCount;
    private final Executor mExecutor;
    private boolean mIsAppSearchSchemaUpToDate;
    private long mLastKnownForegroundElapsedTime;
    private long mLastResetTime;
    private final int mPackageUid;
    private final ArrayList<ShareTargetInfo> mShareTargets;
    final Comparator<ShortcutInfo> mShortcutRankComparator;
    final Comparator<ShortcutInfo> mShortcutTypeAndRankComparator;
    final Comparator<ShortcutInfo> mShortcutTypeRankAndTimeComparator;
    private final ArrayMap<String, ShortcutInfo> mShortcuts;
    private final ArrayMap<String, ShortcutInfo> mTransientShortcuts;

    private ShortcutPackage(ShortcutUser shortcutUser, int packageUserId, String packageName, ShortcutPackageInfo spi) {
        super(shortcutUser, packageUserId, packageName, spi != null ? spi : ShortcutPackageInfo.newEmpty());
        this.mShortcuts = new ArrayMap<>();
        this.mTransientShortcuts = new ArrayMap<>(0);
        this.mShareTargets = new ArrayList<>(0);
        this.mShortcutTypeAndRankComparator = new Comparator() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda18
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ShortcutPackage.lambda$new$19((ShortcutInfo) obj, (ShortcutInfo) obj2);
            }
        };
        this.mShortcutTypeRankAndTimeComparator = new Comparator() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda19
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ShortcutPackage.lambda$new$20((ShortcutInfo) obj, (ShortcutInfo) obj2);
            }
        };
        this.mShortcutRankComparator = new Comparator() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda20
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ShortcutPackage.lambda$new$25((ShortcutInfo) obj, (ShortcutInfo) obj2);
            }
        };
        this.mPackageUid = shortcutUser.mService.injectGetPackageUid(packageName, packageUserId);
        this.mExecutor = BackgroundThread.getExecutor();
    }

    public ShortcutPackage(ShortcutUser shortcutUser, int packageUserId, String packageName) {
        this(shortcutUser, packageUserId, packageName, null);
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    public int getOwnerUserId() {
        return getPackageUserId();
    }

    public int getPackageUid() {
        return this.mPackageUid;
    }

    public Resources getPackageResources() {
        return this.mShortcutUser.mService.injectGetResourcesForApplicationAsUser(getPackageName(), getPackageUserId());
    }

    private boolean isAppSearchEnabled() {
        return this.mShortcutUser.mService.isAppSearchEnabled();
    }

    public int getShortcutCount() {
        int size;
        synchronized (this.mLock) {
            size = this.mShortcuts.size();
        }
        return size;
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    protected boolean canRestoreAnyVersion() {
        return false;
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    protected void onRestored(final int restoreBlockReason) {
        String.format("%s:-%s AND %s:%s", ATTR_FLAGS, 4096, "disabledReason", Integer.valueOf(restoreBlockReason));
        forEachShortcutMutate(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda57
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.lambda$onRestored$0(restoreBlockReason, (ShortcutInfo) obj);
            }
        });
        refreshPinnedFlags();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onRestored$0(int restoreBlockReason, ShortcutInfo si) {
        if (restoreBlockReason == 0 && !si.hasFlags(4096) && si.getDisabledReason() == restoreBlockReason) {
            return;
        }
        si.clearFlags(4096);
        si.setDisabledReason(restoreBlockReason);
        if (restoreBlockReason != 0) {
            si.addFlags(64);
        }
    }

    public ShortcutInfo findShortcutById(String id) {
        ShortcutInfo shortcutInfo;
        if (id == null) {
            return null;
        }
        synchronized (this.mLock) {
            shortcutInfo = this.mShortcuts.get(id);
        }
        return shortcutInfo;
    }

    public boolean isShortcutExistsAndInvisibleToPublisher(String id) {
        ShortcutInfo si = findShortcutById(id);
        return (si == null || si.isVisibleToPublisher()) ? false : true;
    }

    public boolean isShortcutExistsAndVisibleToPublisher(String id) {
        ShortcutInfo si = findShortcutById(id);
        return si != null && si.isVisibleToPublisher();
    }

    private void ensureNotImmutable(ShortcutInfo shortcut, boolean ignoreInvisible) {
        if (shortcut != null && shortcut.isImmutable()) {
            if (!ignoreInvisible || shortcut.isVisibleToPublisher()) {
                throw new IllegalArgumentException("Manifest shortcut ID=" + shortcut.getId() + " may not be manipulated via APIs");
            }
        }
    }

    public void ensureNotImmutable(String id, boolean ignoreInvisible) {
        ensureNotImmutable(findShortcutById(id), ignoreInvisible);
    }

    public void ensureImmutableShortcutsNotIncludedWithIds(List<String> shortcutIds, boolean ignoreInvisible) {
        for (int i = shortcutIds.size() - 1; i >= 0; i--) {
            ensureNotImmutable(shortcutIds.get(i), ignoreInvisible);
        }
    }

    public void ensureImmutableShortcutsNotIncluded(List<ShortcutInfo> shortcuts, boolean ignoreInvisible) {
        for (int i = shortcuts.size() - 1; i >= 0; i--) {
            ensureNotImmutable(shortcuts.get(i).getId(), ignoreInvisible);
        }
    }

    public void ensureNoBitmapIconIfShortcutIsLongLived(List<ShortcutInfo> shortcuts) {
        Icon icon;
        for (int i = shortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = shortcuts.get(i);
            if (si.isLongLived() && (((icon = si.getIcon()) == null || icon.getType() == 1 || icon.getType() == 5) && (icon != null || si.hasIconFile()))) {
                Slog.e(TAG, "Invalid icon type in shortcut " + si.getId() + ". Bitmaps are not allowed in long-lived shortcuts. Use Resource icons, or Uri-based icons instead.");
                return;
            }
        }
    }

    public void ensureAllShortcutsVisibleToLauncher(List<ShortcutInfo> shortcuts) {
        for (ShortcutInfo shortcut : shortcuts) {
            if (shortcut.isExcludedFromSurfaces(1)) {
                throw new IllegalArgumentException("Shortcut ID=" + shortcut.getId() + " is hidden from launcher and may not be manipulated via APIs");
            }
        }
    }

    private ShortcutInfo forceDeleteShortcutInner(String id) {
        ShortcutInfo shortcut;
        synchronized (this.mLock) {
            shortcut = this.mShortcuts.remove(id);
            if (shortcut != null) {
                removeIcon(shortcut);
                shortcut.clearFlags(1610629155);
            }
        }
        return shortcut;
    }

    private void forceReplaceShortcutInner(ShortcutInfo newShortcut) {
        ShortcutService s = this.mShortcutUser.mService;
        forceDeleteShortcutInner(newShortcut.getId());
        s.saveIconAndFixUpShortcutLocked(this, newShortcut);
        s.fixUpShortcutResourceNamesAndValues(newShortcut);
        saveShortcut(newShortcut);
    }

    public boolean addOrReplaceDynamicShortcut(ShortcutInfo newShortcut) {
        Preconditions.checkArgument(newShortcut.isEnabled(), "add/setDynamicShortcuts() cannot publish disabled shortcuts");
        newShortcut.addFlags(1);
        ShortcutInfo oldShortcut = findShortcutById(newShortcut.getId());
        if (oldShortcut != null) {
            oldShortcut.ensureUpdatableWith(newShortcut, false);
            newShortcut.addFlags(oldShortcut.getFlags() & 1610629122);
        }
        if (newShortcut.isExcludedFromSurfaces(1)) {
            if (isAppSearchEnabled()) {
                synchronized (this.mLock) {
                    this.mTransientShortcuts.put(newShortcut.getId(), newShortcut);
                }
            }
        } else {
            forceReplaceShortcutInner(newShortcut);
        }
        if (oldShortcut != null) {
            return true;
        }
        return false;
    }

    public boolean pushDynamicShortcut(final ShortcutInfo newShortcut, List<ShortcutInfo> changedShortcuts) {
        boolean z;
        boolean z2;
        Preconditions.checkArgument(newShortcut.isEnabled(), "pushDynamicShortcuts() cannot publish disabled shortcuts");
        ensureShortcutCountBeforePush();
        newShortcut.addFlags(1);
        changedShortcuts.clear();
        ShortcutInfo oldShortcut = findShortcutById(newShortcut.getId());
        boolean deleted = false;
        if (oldShortcut == null || !oldShortcut.isDynamic()) {
            ShortcutService service = this.mShortcutUser.mService;
            int maxShortcuts = service.getMaxActivityShortcuts();
            ArrayMap<ComponentName, ArrayList<ShortcutInfo>> all = sortShortcutsToActivities();
            ArrayList<ShortcutInfo> activityShortcuts = all.get(newShortcut.getActivity());
            if (activityShortcuts != null && activityShortcuts.size() > maxShortcuts) {
                service.wtf("Error pushing shortcut. There are already " + activityShortcuts.size() + " shortcuts.");
                if (Build.IS_DEBUG_ENABLE && activityShortcuts.size() > 100) {
                    Collections.sort(activityShortcuts, this.mShortcutTypeAndRankComparator);
                    ShortcutInfo shortcut = activityShortcuts.get(maxShortcuts - 1);
                    if (shortcut.isManifestShortcut()) {
                        Slog.e(TAG, "Failed to remove manifest shortcut while pushing dynamic shortcut " + newShortcut.getId());
                        return true;
                    }
                    changedShortcuts.add(shortcut);
                    if (deleteDynamicWithId(shortcut.getId(), true, true) != null) {
                        z2 = true;
                    } else {
                        z2 = false;
                    }
                    deleted = z2;
                }
            }
            if (activityShortcuts != null && activityShortcuts.size() == maxShortcuts) {
                Collections.sort(activityShortcuts, this.mShortcutTypeAndRankComparator);
                ShortcutInfo shortcut2 = activityShortcuts.get(maxShortcuts - 1);
                if (shortcut2.isManifestShortcut()) {
                    Slog.e(TAG, "Failed to remove manifest shortcut while pushing dynamic shortcut " + newShortcut.getId());
                    return true;
                }
                changedShortcuts.add(shortcut2);
                if (deleteDynamicWithId(shortcut2.getId(), true, true) != null) {
                    z = true;
                } else {
                    z = false;
                }
                deleted = z;
            }
        }
        if (oldShortcut != null) {
            oldShortcut.ensureUpdatableWith(newShortcut, false);
            newShortcut.addFlags(oldShortcut.getFlags() & 1610629122);
        }
        if (newShortcut.isExcludedFromSurfaces(1)) {
            if (isAppSearchEnabled()) {
                synchronized (this.mLock) {
                    this.mTransientShortcuts.put(newShortcut.getId(), newShortcut);
                }
            }
        } else {
            forceReplaceShortcutInner(newShortcut);
        }
        if (isAppSearchEnabled()) {
            runAsSystem(new Runnable() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda26
                @Override // java.lang.Runnable
                public final void run() {
                    ShortcutPackage.this.m5631x1b15c149(newShortcut);
                }
            });
        }
        return deleted;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pushDynamicShortcut$3$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5631x1b15c149(final ShortcutInfo newShortcut) {
        fromAppSearch().thenAccept(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5630x5214ca08(newShortcut, (AppSearchSession) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pushDynamicShortcut$2$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5630x5214ca08(ShortcutInfo newShortcut, AppSearchSession session) {
        session.reportUsage(new ReportUsageRequest.Builder(getPackageName(), newShortcut.getId()).build(), this.mExecutor, new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda39
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.lambda$pushDynamicShortcut$1((AppSearchResult) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$pushDynamicShortcut$1(AppSearchResult result) {
        if (!result.isSuccess()) {
            Slog.e(TAG, "Failed to report usage via AppSearch. " + result.getErrorMessage());
        }
    }

    private void ensureShortcutCountBeforePush() {
        ShortcutService service = this.mShortcutUser.mService;
        int maxShortcutPerApp = service.getMaxAppShortcuts();
        synchronized (this.mLock) {
            List<ShortcutInfo> appShortcuts = (List) this.mShortcuts.values().stream().filter(new Predicate() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda50
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ShortcutPackage.lambda$ensureShortcutCountBeforePush$4((ShortcutInfo) obj);
                }
            }).collect(Collectors.toList());
            if (appShortcuts.size() >= maxShortcutPerApp) {
                Collections.sort(appShortcuts, this.mShortcutTypeRankAndTimeComparator);
                while (appShortcuts.size() >= maxShortcutPerApp) {
                    ShortcutInfo shortcut = appShortcuts.remove(appShortcuts.size() - 1);
                    if (shortcut.isDeclaredInManifest()) {
                        throw new IllegalArgumentException(getPackageName() + " has published " + appShortcuts.size() + " manifest shortcuts across different activities.");
                    }
                    forceDeleteShortcutInner(shortcut.getId());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$ensureShortcutCountBeforePush$4(ShortcutInfo si) {
        return !si.isPinned();
    }

    private List<ShortcutInfo> removeOrphans() {
        final List<ShortcutInfo> removeList = new ArrayList<>(1);
        forEachShortcut(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda35
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.lambda$removeOrphans$5(removeList, (ShortcutInfo) obj);
            }
        });
        if (!removeList.isEmpty()) {
            for (int i = removeList.size() - 1; i >= 0; i--) {
                forceDeleteShortcutInner(removeList.get(i).getId());
            }
        }
        return removeList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$removeOrphans$5(List removeList, ShortcutInfo si) {
        if (si.isAlive()) {
            return;
        }
        removeList.add(si);
    }

    public List<ShortcutInfo> deleteAllDynamicShortcuts() {
        long now = this.mShortcutUser.mService.injectCurrentTimeMillis();
        boolean changed = false;
        synchronized (this.mLock) {
            for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
                ShortcutInfo si = this.mShortcuts.valueAt(i);
                if (si.isDynamic() && si.isVisibleToPublisher()) {
                    changed = true;
                    si.setTimestamp(now);
                    si.clearFlags(1);
                    si.setRank(0);
                }
            }
        }
        removeAllShortcutsAsync();
        if (changed) {
            return removeOrphans();
        }
        return null;
    }

    public ShortcutInfo deleteDynamicWithId(String shortcutId, boolean ignoreInvisible, boolean ignorePersistedShortcuts) {
        return deleteOrDisableWithId(shortcutId, false, false, ignoreInvisible, 0, ignorePersistedShortcuts);
    }

    private ShortcutInfo disableDynamicWithId(String shortcutId, boolean ignoreInvisible, int disabledReason, boolean ignorePersistedShortcuts) {
        return deleteOrDisableWithId(shortcutId, true, false, ignoreInvisible, disabledReason, ignorePersistedShortcuts);
    }

    public ShortcutInfo deleteLongLivedWithId(String shortcutId, boolean ignoreInvisible) {
        ShortcutInfo shortcut = findShortcutById(shortcutId);
        if (shortcut != null) {
            mutateShortcut(shortcutId, null, new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda5
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((ShortcutInfo) obj).clearFlags(1610629120);
                }
            });
        }
        return deleteOrDisableWithId(shortcutId, false, false, ignoreInvisible, 0, false);
    }

    public ShortcutInfo disableWithId(String shortcutId, final String disabledMessage, final int disabledMessageResId, boolean overrideImmutable, boolean ignoreInvisible, int disabledReason) {
        ShortcutInfo deleted = deleteOrDisableWithId(shortcutId, true, overrideImmutable, ignoreInvisible, disabledReason, false);
        mutateShortcut(shortcutId, null, new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda16
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5620lambda$disableWithId$7$comandroidserverpmShortcutPackage(disabledMessage, disabledMessageResId, (ShortcutInfo) obj);
            }
        });
        return deleted;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$disableWithId$7$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5620lambda$disableWithId$7$comandroidserverpmShortcutPackage(String disabledMessage, int disabledMessageResId, ShortcutInfo disabled) {
        if (disabled != null) {
            if (disabledMessage != null) {
                disabled.setDisabledMessage(disabledMessage);
            } else if (disabledMessageResId != 0) {
                disabled.setDisabledMessageResId(disabledMessageResId);
                this.mShortcutUser.mService.fixUpShortcutResourceNamesAndValues(disabled);
            }
        }
    }

    private ShortcutInfo deleteOrDisableWithId(String shortcutId, final boolean disable, boolean overrideImmutable, boolean ignoreInvisible, final int disabledReason, boolean ignorePersistedShortcuts) {
        Preconditions.checkState(disable == (disabledReason != 0), "disable and disabledReason disagree: " + disable + " vs " + disabledReason);
        ShortcutInfo oldShortcut = findShortcutById(shortcutId);
        if (oldShortcut == null || (!oldShortcut.isEnabled() && ignoreInvisible && !oldShortcut.isVisibleToPublisher())) {
            return null;
        }
        if (!overrideImmutable) {
            ensureNotImmutable(oldShortcut, true);
        }
        if (!ignorePersistedShortcuts) {
            removeShortcutAsync(shortcutId);
        }
        if (oldShortcut.isPinned() || oldShortcut.isCached()) {
            mutateShortcut(oldShortcut.getId(), oldShortcut, new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda49
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ShortcutPackage.this.m5619x8c98b8de(disable, disabledReason, (ShortcutInfo) obj);
                }
            });
            return null;
        }
        forceDeleteShortcutInner(shortcutId);
        return oldShortcut;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$deleteOrDisableWithId$8$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5619x8c98b8de(boolean disable, int disabledReason, ShortcutInfo si) {
        si.setRank(0);
        si.clearFlags(33);
        if (disable) {
            si.addFlags(64);
            if (si.getDisabledReason() == 0) {
                si.setDisabledReason(disabledReason);
            }
        }
        si.setTimestamp(this.mShortcutUser.mService.injectCurrentTimeMillis());
        if (this.mShortcutUser.mService.isDummyMainActivity(si.getActivity())) {
            si.setActivity(null);
        }
    }

    public void enableWithId(String shortcutId) {
        mutateShortcut(shortcutId, null, new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda56
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5621lambda$enableWithId$9$comandroidserverpmShortcutPackage((ShortcutInfo) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$enableWithId$9$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5621lambda$enableWithId$9$comandroidserverpmShortcutPackage(ShortcutInfo si) {
        ensureNotImmutable(si, true);
        si.clearFlags(64);
        si.setDisabledReason(0);
    }

    public void updateInvisibleShortcutForPinRequestWith(ShortcutInfo shortcut) {
        ShortcutInfo source = findShortcutById(shortcut.getId());
        Objects.requireNonNull(source);
        this.mShortcutUser.mService.validateShortcutForPinRequest(shortcut);
        shortcut.addFlags(2);
        forceReplaceShortcutInner(shortcut);
        adjustRanks();
    }

    public void refreshPinnedFlags() {
        final Set<String> pinnedShortcuts = new ArraySet<>();
        this.mShortcutUser.forAllLaunchers(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda30
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5632xa760a19e(pinnedShortcuts, (ShortcutLauncher) obj);
            }
        });
        List<ShortcutInfo> pinned = findAll(pinnedShortcuts);
        if (pinned != null) {
            pinned.forEach(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda31
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ShortcutPackage.lambda$refreshPinnedFlags$11((ShortcutInfo) obj);
                }
            });
        }
        forEachShortcutMutate(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda32
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.lambda$refreshPinnedFlags$12(pinnedShortcuts, (ShortcutInfo) obj);
            }
        });
        this.mShortcutUser.forAllLaunchers(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda33
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ShortcutLauncher) obj).scheduleSave();
            }
        });
        removeOrphans();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$refreshPinnedFlags$10$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5632xa760a19e(Set pinnedShortcuts, ShortcutLauncher launcherShortcuts) {
        ArraySet<String> pinned = launcherShortcuts.getPinnedShortcutIds(getPackageName(), getPackageUserId());
        if (pinned == null || pinned.size() == 0) {
            return;
        }
        pinnedShortcuts.addAll(pinned);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$refreshPinnedFlags$11(ShortcutInfo si) {
        if (!si.isPinned()) {
            si.addFlags(2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$refreshPinnedFlags$12(Set pinnedShortcuts, ShortcutInfo si) {
        if (!pinnedShortcuts.contains(si.getId()) && si.isPinned()) {
            si.clearFlags(2);
        }
    }

    public int getApiCallCount(boolean unlimited) {
        ShortcutService s = this.mShortcutUser.mService;
        if (s.isUidForegroundLocked(this.mPackageUid) || this.mLastKnownForegroundElapsedTime < s.getUidLastForegroundElapsedTimeLocked(this.mPackageUid) || unlimited) {
            this.mLastKnownForegroundElapsedTime = s.injectElapsedRealtime();
            resetRateLimiting();
        }
        long last = s.getLastResetTimeLocked();
        long now = s.injectCurrentTimeMillis();
        if (ShortcutService.isClockValid(now) && this.mLastResetTime > now) {
            Slog.w(TAG, "Clock rewound");
            this.mLastResetTime = now;
            this.mApiCallCount = 0;
            return 0;
        }
        if (this.mLastResetTime < last) {
            this.mApiCallCount = 0;
            this.mLastResetTime = last;
        }
        return this.mApiCallCount;
    }

    public boolean tryApiCall(boolean unlimited) {
        ShortcutService s = this.mShortcutUser.mService;
        if (getApiCallCount(unlimited) >= s.mMaxUpdatesPerInterval) {
            return false;
        }
        this.mApiCallCount++;
        scheduleSave();
        return true;
    }

    public void resetRateLimiting() {
        if (this.mApiCallCount > 0) {
            this.mApiCallCount = 0;
            scheduleSave();
        }
    }

    public void resetRateLimitingForCommandLineNoSaving() {
        this.mApiCallCount = 0;
        this.mLastResetTime = 0L;
    }

    public void findAll(List<ShortcutInfo> result, Predicate<ShortcutInfo> filter, int cloneFlag) {
        findAll(result, filter, cloneFlag, null, 0, false);
    }

    public void findAll(final List<ShortcutInfo> result, final Predicate<ShortcutInfo> filter, final int cloneFlag, final String callingLauncher, int launcherUserId, final boolean getPinnedByAnyLauncher) {
        if (getPackageInfo().isShadow()) {
            return;
        }
        ShortcutService s = this.mShortcutUser.mService;
        final ArraySet<String> pinnedByCallerSet = callingLauncher == null ? null : s.getLauncherShortcutsLocked(callingLauncher, getPackageUserId(), launcherUserId).getPinnedShortcutIds(getPackageName(), getPackageUserId());
        forEachShortcut(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5623lambda$findAll$13$comandroidserverpmShortcutPackage(result, filter, cloneFlag, callingLauncher, pinnedByCallerSet, getPinnedByAnyLauncher, (ShortcutInfo) obj);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: filter */
    public void m5623lambda$findAll$13$comandroidserverpmShortcutPackage(List<ShortcutInfo> result, Predicate<ShortcutInfo> query, int cloneFlag, String callingLauncher, ArraySet<String> pinnedByCallerSet, boolean getPinnedByAnyLauncher, ShortcutInfo si) {
        boolean isPinnedByCaller = callingLauncher == null || (pinnedByCallerSet != null && pinnedByCallerSet.contains(si.getId()));
        if (!getPinnedByAnyLauncher && si.isFloating() && !si.isCached() && !isPinnedByCaller) {
            return;
        }
        ShortcutInfo clone = si.clone(cloneFlag);
        if (!getPinnedByAnyLauncher && !isPinnedByCaller) {
            clone.clearFlags(2);
        }
        if (query == null || query.test(clone)) {
            if (!isPinnedByCaller) {
                clone.clearFlags(2);
            }
            result.add(clone);
        }
    }

    public void resetThrottling() {
        this.mApiCallCount = 0;
    }

    public List<ShortcutManager.ShareShortcutInfo> getMatchingShareTargets(IntentFilter filter) {
        synchronized (this.mLock) {
            List<ShareTargetInfo> matchedTargets = new ArrayList<>();
            for (int i = 0; i < this.mShareTargets.size(); i++) {
                ShareTargetInfo target = this.mShareTargets.get(i);
                ShareTargetInfo.TargetData[] targetDataArr = target.mTargetData;
                int length = targetDataArr.length;
                int i2 = 0;
                while (true) {
                    if (i2 < length) {
                        ShareTargetInfo.TargetData data = targetDataArr[i2];
                        if (!filter.hasDataType(data.mMimeType)) {
                            i2++;
                        } else {
                            matchedTargets.add(target);
                            break;
                        }
                    }
                }
            }
            if (matchedTargets.isEmpty()) {
                return new ArrayList();
            }
            ArrayList<ShortcutInfo> shortcuts = new ArrayList<>();
            findAll(shortcuts, new ShortcutPackage$$ExternalSyntheticLambda27(), 9, this.mShortcutUser.mService.mContext.getPackageName(), 0, false);
            List<ShortcutManager.ShareShortcutInfo> result = new ArrayList<>();
            for (int i3 = 0; i3 < shortcuts.size(); i3++) {
                Set<String> categories = shortcuts.get(i3).getCategories();
                if (categories != null && !categories.isEmpty()) {
                    int j = 0;
                    while (true) {
                        if (j < matchedTargets.size()) {
                            boolean hasAllCategories = true;
                            ShareTargetInfo target2 = matchedTargets.get(j);
                            int q = 0;
                            while (true) {
                                if (q < target2.mCategories.length) {
                                    if (categories.contains(target2.mCategories[q])) {
                                        q++;
                                    } else {
                                        hasAllCategories = false;
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }
                            if (!hasAllCategories) {
                                j++;
                            } else {
                                result.add(new ShortcutManager.ShareShortcutInfo(shortcuts.get(i3), new ComponentName(getPackageName(), target2.mTargetClass)));
                                break;
                            }
                        }
                    }
                }
            }
            return result;
        }
    }

    public boolean hasShareTargets() {
        boolean z;
        synchronized (this.mLock) {
            z = !this.mShareTargets.isEmpty();
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSharingShortcutCount() {
        synchronized (this.mLock) {
            if (this.mShareTargets.isEmpty()) {
                return 0;
            }
            ArrayList<ShortcutInfo> shortcuts = new ArrayList<>();
            findAll(shortcuts, new ShortcutPackage$$ExternalSyntheticLambda27(), 27);
            int sharingShortcutCount = 0;
            for (int i = 0; i < shortcuts.size(); i++) {
                Set<String> categories = shortcuts.get(i).getCategories();
                if (categories != null && !categories.isEmpty()) {
                    int j = 0;
                    while (true) {
                        if (j < this.mShareTargets.size()) {
                            boolean hasAllCategories = true;
                            ShareTargetInfo target = this.mShareTargets.get(j);
                            int q = 0;
                            while (true) {
                                if (q < target.mCategories.length) {
                                    if (categories.contains(target.mCategories[q])) {
                                        q++;
                                    } else {
                                        hasAllCategories = false;
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }
                            if (!hasAllCategories) {
                                j++;
                            } else {
                                sharingShortcutCount++;
                                break;
                            }
                        }
                    }
                }
            }
            return sharingShortcutCount;
        }
    }

    private ArraySet<String> getUsedBitmapFilesLocked() {
        final ArraySet<String> usedFiles = new ArraySet<>(1);
        forEachShortcut(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda7
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.lambda$getUsedBitmapFilesLocked$14(usedFiles, (ShortcutInfo) obj);
            }
        });
        return usedFiles;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getUsedBitmapFilesLocked$14(ArraySet usedFiles, ShortcutInfo si) {
        if (si.getBitmapPath() != null) {
            usedFiles.add(getFileName(si.getBitmapPath()));
        }
    }

    public void cleanupDanglingBitmapFiles(File path) {
        File[] listFiles;
        synchronized (this.mLock) {
            boolean success = this.mShortcutBitmapSaver.waitForAllSavesLocked();
            if (!success) {
                this.mShortcutUser.mService.wtf("Timed out waiting on saving bitmaps.");
            }
            ArraySet<String> usedFiles = getUsedBitmapFilesLocked();
            for (File child : path.listFiles()) {
                if (child.isFile()) {
                    String name = child.getName();
                    if (!usedFiles.contains(name)) {
                        child.delete();
                    }
                }
            }
        }
    }

    private static String getFileName(String path) {
        int sep = path.lastIndexOf(File.separatorChar);
        if (sep == -1) {
            return path;
        }
        return path.substring(sep + 1);
    }

    private boolean areAllActivitiesStillEnabled() {
        final ShortcutService s = this.mShortcutUser.mService;
        final ArrayList<ComponentName> checked = new ArrayList<>(4);
        final boolean[] reject = new boolean[1];
        forEachShortcutStopWhen(new Function() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda29
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ShortcutPackage.this.m5618x40ac80a4(checked, s, reject, (ShortcutInfo) obj);
            }
        });
        return true ^ reject[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$areAllActivitiesStillEnabled$15$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ Boolean m5618x40ac80a4(ArrayList checked, ShortcutService s, boolean[] reject, ShortcutInfo si) {
        ComponentName activity = si.getActivity();
        if (checked.contains(activity)) {
            return false;
        }
        checked.add(activity);
        if (activity == null || s.injectIsActivityEnabledAndExported(activity, getOwnerUserId())) {
            return false;
        }
        reject[0] = true;
        return true;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1149=4] */
    public boolean rescanPackageIfNeeded(boolean isNewApp, boolean forceRescan) {
        final ShortcutService s = this.mShortcutUser.mService;
        long start = s.getStatStartTime();
        try {
            PackageInfo pi = this.mShortcutUser.mService.getPackageInfo(getPackageName(), getPackageUserId());
            if (pi == null) {
                return false;
            }
            if (!isNewApp && !forceRescan && getPackageInfo().getVersionCode() == pi.getLongVersionCode() && getPackageInfo().getLastUpdateTime() == pi.lastUpdateTime) {
                if (areAllActivitiesStillEnabled()) {
                    return false;
                }
            }
            s.logDurationStat(14, start);
            List<ShortcutInfo> newManifestShortcutList = null;
            synchronized (this.mLock) {
                try {
                    this.mShareTargets.size();
                    newManifestShortcutList = ShortcutParser.parseShortcuts(this.mShortcutUser.mService, getPackageName(), getPackageUserId(), this.mShareTargets);
                } catch (IOException | XmlPullParserException e) {
                    Slog.e(TAG, "Failed to load shortcuts from AndroidManifest.xml.", e);
                }
            }
            int manifestShortcutSize = newManifestShortcutList == null ? 0 : newManifestShortcutList.size();
            if (isNewApp && manifestShortcutSize == 0) {
                return false;
            }
            getPackageInfo().updateFromPackageInfo(pi);
            final long newVersionCode = getPackageInfo().getVersionCode();
            forEachShortcutMutate(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda59
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ShortcutPackage.this.m5637x7539a2ea(newVersionCode, (ShortcutInfo) obj);
                }
            });
            if (!isNewApp) {
                final Resources publisherRes = getPackageResources();
                forEachShortcutMutate(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda60
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ShortcutPackage.this.m5638x3e3a9a2b(s, publisherRes, (ShortcutInfo) obj);
                    }
                });
            }
            publishManifestShortcuts(newManifestShortcutList);
            if (newManifestShortcutList != null) {
                pushOutExcessShortcuts();
            }
            s.verifyStates();
            s.packageShortcutsChanged(this, null, null);
            return true;
        } finally {
            s.logDurationStat(14, start);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$rescanPackageIfNeeded$16$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5637x7539a2ea(long newVersionCode, ShortcutInfo si) {
        if (si.getDisabledReason() != 100 || getPackageInfo().getBackupSourceVersionCode() > newVersionCode) {
            return;
        }
        Slog.i(TAG, String.format("Restoring shortcut: %s", si.getId()));
        si.clearFlags(64);
        si.setDisabledReason(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$rescanPackageIfNeeded$17$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5638x3e3a9a2b(ShortcutService s, Resources publisherRes, ShortcutInfo si) {
        if (si.isDynamic()) {
            if (si.getActivity() == null) {
                s.wtf("null activity detected.");
            } else if (!s.injectIsMainActivity(si.getActivity(), getPackageUserId())) {
                Slog.w(TAG, String.format("%s is no longer main activity. Disabling shorcut %s.", getPackageName(), si.getId()));
                if (disableDynamicWithId(si.getId(), false, 2, false) != null) {
                    return;
                }
            }
        }
        if (!si.hasAnyResources() || publisherRes == null) {
            return;
        }
        if (!si.isOriginallyFromManifest()) {
            si.lookupAndFillInResourceIds(publisherRes);
        }
        si.setTimestamp(s.injectCurrentTimeMillis());
    }

    private boolean publishManifestShortcuts(List<ShortcutInfo> newManifestShortcutList) {
        boolean changed = false;
        final ArraySet<String> toDisableList = new ArraySet<>(1);
        forEachShortcut(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda14
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.lambda$publishManifestShortcuts$18(toDisableList, (ShortcutInfo) obj);
            }
        });
        if (newManifestShortcutList != null) {
            int newListSize = newManifestShortcutList.size();
            for (int i = 0; i < newListSize; i++) {
                changed = true;
                ShortcutInfo newShortcut = newManifestShortcutList.get(i);
                boolean newDisabled = !newShortcut.isEnabled();
                String id = newShortcut.getId();
                ShortcutInfo oldShortcut = findShortcutById(id);
                boolean wasPinned = false;
                if (oldShortcut != null) {
                    if (!oldShortcut.isOriginallyFromManifest()) {
                        Slog.e(TAG, "Shortcut with ID=" + newShortcut.getId() + " exists but is not from AndroidManifest.xml, not updating.");
                    } else if (oldShortcut.isPinned()) {
                        wasPinned = true;
                        newShortcut.addFlags(2);
                    }
                }
                if (!newDisabled || wasPinned) {
                    forceReplaceShortcutInner(newShortcut);
                    if (!newDisabled && !toDisableList.isEmpty()) {
                        toDisableList.remove(id);
                    }
                }
            }
        }
        if (!toDisableList.isEmpty()) {
            for (int i2 = toDisableList.size() - 1; i2 >= 0; i2--) {
                changed = true;
                disableWithId(toDisableList.valueAt(i2), null, 0, true, false, 2);
            }
            removeOrphans();
        }
        adjustRanks();
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$publishManifestShortcuts$18(ArraySet toDisableList, ShortcutInfo si) {
        if (si.isManifestShortcut()) {
            toDisableList.add(si.getId());
        }
    }

    private boolean pushOutExcessShortcuts() {
        ShortcutService service = this.mShortcutUser.mService;
        int maxShortcuts = service.getMaxActivityShortcuts();
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> all = sortShortcutsToActivities();
        for (int outer = all.size() - 1; outer >= 0; outer--) {
            ArrayList<ShortcutInfo> list = all.valueAt(outer);
            if (list.size() > maxShortcuts) {
                Collections.sort(list, this.mShortcutTypeAndRankComparator);
                for (int inner = list.size() - 1; inner >= maxShortcuts; inner--) {
                    ShortcutInfo shortcut = list.get(inner);
                    if (shortcut.isManifestShortcut()) {
                        service.wtf("Found manifest shortcuts in excess list.");
                    } else {
                        deleteDynamicWithId(shortcut.getId(), true, true);
                    }
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$new$19(ShortcutInfo a, ShortcutInfo b) {
        if (a.isManifestShortcut() && !b.isManifestShortcut()) {
            return -1;
        }
        if (!a.isManifestShortcut() && b.isManifestShortcut()) {
            return 1;
        }
        return Integer.compare(a.getRank(), b.getRank());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$new$20(ShortcutInfo a, ShortcutInfo b) {
        if (!a.isDeclaredInManifest() || b.isDeclaredInManifest()) {
            if (a.isDeclaredInManifest() || !b.isDeclaredInManifest()) {
                if (a.isDynamic() && b.isDynamic()) {
                    return Integer.compare(a.getRank(), b.getRank());
                }
                if (a.isDynamic()) {
                    return -1;
                }
                if (b.isDynamic()) {
                    return 1;
                }
                if (a.isCached() && b.isCached()) {
                    if (a.hasFlags(536870912) && !b.hasFlags(536870912)) {
                        return -1;
                    }
                    if (!a.hasFlags(536870912) && b.hasFlags(536870912)) {
                        return 1;
                    }
                    if (a.hasFlags(1073741824) && !b.hasFlags(1073741824)) {
                        return -1;
                    }
                    if (!a.hasFlags(1073741824) && b.hasFlags(1073741824)) {
                        return 1;
                    }
                }
                if (a.isCached()) {
                    return -1;
                }
                if (b.isCached()) {
                    return 1;
                }
                return Long.compare(b.getLastChangedTimestamp(), a.getLastChangedTimestamp());
            }
            return 1;
        }
        return -1;
    }

    private ArrayMap<ComponentName, ArrayList<ShortcutInfo>> sortShortcutsToActivities() {
        final ArrayMap<ComponentName, ArrayList<ShortcutInfo>> activitiesToShortcuts = new ArrayMap<>();
        forEachShortcut(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda52
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5641x15970e16(activitiesToShortcuts, (ShortcutInfo) obj);
            }
        });
        return activitiesToShortcuts;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sortShortcutsToActivities$22$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5641x15970e16(ArrayMap activitiesToShortcuts, ShortcutInfo si) {
        if (si.isFloating()) {
            return;
        }
        ComponentName activity = si.getActivity();
        if (activity == null) {
            this.mShortcutUser.mService.wtf("null activity detected.");
            return;
        }
        ArrayList<ShortcutInfo> list = (ArrayList) activitiesToShortcuts.computeIfAbsent(activity, new Function() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ShortcutPackage.lambda$sortShortcutsToActivities$21((ComponentName) obj);
            }
        });
        list.add(si);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ArrayList lambda$sortShortcutsToActivities$21(ComponentName k) {
        return new ArrayList();
    }

    private void incrementCountForActivity(ArrayMap<ComponentName, Integer> counts, ComponentName cn, int increment) {
        Integer oldValue = counts.get(cn);
        if (oldValue == null) {
            oldValue = 0;
        }
        counts.put(cn, Integer.valueOf(oldValue.intValue() + increment));
    }

    public void enforceShortcutCountsBeforeOperation(List<ShortcutInfo> newList, final int operation) {
        ShortcutService service = this.mShortcutUser.mService;
        final ArrayMap<ComponentName, Integer> counts = new ArrayMap<>(4);
        forEachShortcut(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda55
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5622x690dd8d8(counts, operation, (ShortcutInfo) obj);
            }
        });
        for (int i = newList.size() - 1; i >= 0; i--) {
            ShortcutInfo newShortcut = newList.get(i);
            ComponentName newActivity = newShortcut.getActivity();
            if (newActivity == null) {
                if (operation != 2) {
                    service.wtf("Activity must not be null at this point");
                }
            } else {
                ShortcutInfo original = findShortcutById(newShortcut.getId());
                if (original == null) {
                    if (operation != 2) {
                        incrementCountForActivity(counts, newActivity, 1);
                    }
                } else if (!original.isFloating() || operation != 2) {
                    if (operation != 0) {
                        ComponentName oldActivity = original.getActivity();
                        if (!original.isFloating()) {
                            incrementCountForActivity(counts, oldActivity, -1);
                        }
                    }
                    incrementCountForActivity(counts, newActivity, 1);
                }
            }
        }
        int i2 = counts.size();
        for (int i3 = i2 - 1; i3 >= 0; i3--) {
            service.enforceMaxActivityShortcuts(counts.valueAt(i3).intValue());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$enforceShortcutCountsBeforeOperation$23$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5622x690dd8d8(ArrayMap counts, int operation, ShortcutInfo shortcut) {
        if (shortcut.isManifestShortcut()) {
            incrementCountForActivity(counts, shortcut.getActivity(), 1);
        } else if (shortcut.isDynamic() && operation != 0) {
            incrementCountForActivity(counts, shortcut.getActivity(), 1);
        }
    }

    public void resolveResourceStrings() {
        final ShortcutService s = this.mShortcutUser.mService;
        final Resources publisherRes = getPackageResources();
        final List<ShortcutInfo> changedShortcuts = new ArrayList<>(1);
        if (publisherRes != null) {
            forEachShortcutMutate(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda17
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ShortcutPackage.lambda$resolveResourceStrings$24(publisherRes, s, changedShortcuts, (ShortcutInfo) obj);
                }
            });
        }
        if (!CollectionUtils.isEmpty(changedShortcuts)) {
            s.packageShortcutsChanged(this, changedShortcuts, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$resolveResourceStrings$24(Resources publisherRes, ShortcutService s, List changedShortcuts, ShortcutInfo si) {
        if (si.hasStringResources()) {
            si.resolveResourceStrings(publisherRes);
            si.setTimestamp(s.injectCurrentTimeMillis());
            changedShortcuts.add(si);
        }
    }

    public void clearAllImplicitRanks() {
        forEachShortcutMutate(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda48
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ShortcutInfo) obj).clearImplicitRankAndRankChangedFlag();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$new$25(ShortcutInfo a, ShortcutInfo b) {
        int ret = Integer.compare(a.getRank(), b.getRank());
        if (ret != 0) {
            return ret;
        }
        if (a.isRankChanged() != b.isRankChanged()) {
            return a.isRankChanged() ? -1 : 1;
        }
        int ret2 = Integer.compare(a.getImplicitRank(), b.getImplicitRank());
        if (ret2 != 0) {
            return ret2;
        }
        return a.getId().compareTo(b.getId());
    }

    public void adjustRanks() {
        ShortcutService s = this.mShortcutUser.mService;
        final long now = s.injectCurrentTimeMillis();
        forEachShortcutMutate(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda9
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.lambda$adjustRanks$26(now, (ShortcutInfo) obj);
            }
        });
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> all = sortShortcutsToActivities();
        for (int outer = all.size() - 1; outer >= 0; outer--) {
            ArrayList<ShortcutInfo> list = all.valueAt(outer);
            Collections.sort(list, this.mShortcutRankComparator);
            final int thisRank = 0;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                ShortcutInfo si = list.get(i);
                if (!si.isManifestShortcut()) {
                    if (!si.isDynamic()) {
                        s.wtf("Non-dynamic shortcut found. " + si.toInsecureString());
                    } else {
                        int rank = thisRank + 1;
                        if (si.getRank() != thisRank) {
                            mutateShortcut(si.getId(), si, new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda10
                                @Override // java.util.function.Consumer
                                public final void accept(Object obj) {
                                    ShortcutPackage.lambda$adjustRanks$27(now, thisRank, (ShortcutInfo) obj);
                                }
                            });
                        }
                        thisRank = rank;
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$adjustRanks$26(long now, ShortcutInfo si) {
        if (si.isFloating() && si.getRank() != 0) {
            si.setTimestamp(now);
            si.setRank(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$adjustRanks$27(long now, int thisRank, ShortcutInfo shortcut) {
        shortcut.setTimestamp(now);
        shortcut.setRank(thisRank);
    }

    public boolean hasNonManifestShortcuts() {
        final boolean[] condition = new boolean[1];
        forEachShortcutStopWhen(new Function() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda53
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ShortcutPackage.lambda$hasNonManifestShortcuts$28(condition, (ShortcutInfo) obj);
            }
        });
        return condition[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Boolean lambda$hasNonManifestShortcuts$28(boolean[] condition, ShortcutInfo si) {
        if (!si.isDeclaredInManifest()) {
            condition[0] = true;
            return true;
        }
        return false;
    }

    public void dump(final PrintWriter pw, final String prefix, ShortcutService.DumpFilter filter) {
        pw.println();
        pw.print(prefix);
        pw.print("Package: ");
        pw.print(getPackageName());
        pw.print("  UID: ");
        pw.print(this.mPackageUid);
        pw.println();
        pw.print(prefix);
        pw.print("  ");
        pw.print("Calls: ");
        pw.print(getApiCallCount(false));
        pw.println();
        pw.print(prefix);
        pw.print("  ");
        pw.print("Last known FG: ");
        pw.print(this.mLastKnownForegroundElapsedTime);
        pw.println();
        pw.print(prefix);
        pw.print("  ");
        pw.print("Last reset: [");
        pw.print(this.mLastResetTime);
        pw.print("] ");
        pw.print(ShortcutService.formatTime(this.mLastResetTime));
        pw.println();
        getPackageInfo().dump(pw, prefix + "  ");
        pw.println();
        pw.print(prefix);
        pw.println("  Shortcuts:");
        final long[] totalBitmapSize = new long[1];
        forEachShortcut(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda6
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.lambda$dump$29(pw, prefix, totalBitmapSize, (ShortcutInfo) obj);
            }
        });
        pw.print(prefix);
        pw.print("  ");
        pw.print("Total bitmap size: ");
        pw.print(totalBitmapSize[0]);
        pw.print(" (");
        pw.print(Formatter.formatFileSize(this.mShortcutUser.mService.mContext, totalBitmapSize[0]));
        pw.println(")");
        pw.println();
        synchronized (this.mLock) {
            this.mShortcutBitmapSaver.dumpLocked(pw, "  ");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dump$29(PrintWriter pw, String prefix, long[] totalBitmapSize, ShortcutInfo si) {
        pw.println(si.toDumpString(prefix + "    "));
        if (si.getBitmapPath() != null) {
            long len = new File(si.getBitmapPath()).length();
            pw.print(prefix);
            pw.print("      ");
            pw.print("bitmap size=");
            pw.println(len);
            totalBitmapSize[0] = totalBitmapSize[0] + len;
        }
    }

    public void dumpShortcuts(final PrintWriter pw, int matchFlags) {
        boolean matchDynamic = (matchFlags & 2) != 0;
        boolean matchPinned = (matchFlags & 4) != 0;
        boolean matchManifest = (matchFlags & 1) != 0;
        boolean matchCached = (matchFlags & 8) != 0;
        final int shortcutFlags = (matchDynamic ? 1 : 0) | (matchPinned ? 2 : 0) | (matchManifest ? 32 : 0) | (matchCached ? 1610629120 : 0);
        forEachShortcut(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.lambda$dumpShortcuts$30(shortcutFlags, pw, (ShortcutInfo) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpShortcuts$30(int shortcutFlags, PrintWriter pw, ShortcutInfo si) {
        if ((si.getFlags() & shortcutFlags) != 0) {
            pw.println(si.toDumpString(""));
        }
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    public JSONObject dumpCheckin(boolean clear) throws JSONException {
        JSONObject result = super.dumpCheckin(clear);
        final int[] numDynamic = new int[1];
        final int[] numPinned = new int[1];
        final int[] numManifest = new int[1];
        final int[] numBitmaps = new int[1];
        final long[] totalBitmapSize = new long[1];
        forEachShortcut(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda21
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.lambda$dumpCheckin$31(numDynamic, numManifest, numPinned, numBitmaps, totalBitmapSize, (ShortcutInfo) obj);
            }
        });
        result.put(KEY_DYNAMIC, numDynamic[0]);
        result.put("manifest", numManifest[0]);
        result.put(KEY_PINNED, numPinned[0]);
        result.put(KEY_BITMAPS, numBitmaps[0]);
        result.put(KEY_BITMAP_BYTES, totalBitmapSize[0]);
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpCheckin$31(int[] numDynamic, int[] numManifest, int[] numPinned, int[] numBitmaps, long[] totalBitmapSize, ShortcutInfo si) {
        if (si.isDynamic()) {
            numDynamic[0] = numDynamic[0] + 1;
        }
        if (si.isDeclaredInManifest()) {
            numManifest[0] = numManifest[0] + 1;
        }
        if (si.isPinned()) {
            numPinned[0] = numPinned[0] + 1;
        }
        if (si.getBitmapPath() != null) {
            numBitmaps[0] = numBitmaps[0] + 1;
            totalBitmapSize[0] = totalBitmapSize[0] + new File(si.getBitmapPath()).length();
        }
    }

    private boolean hasNoShortcut() {
        if (!isAppSearchEnabled()) {
            return getShortcutCount() == 0;
        }
        final boolean[] hasAnyShortcut = new boolean[1];
        forEachShortcutStopWhen(new Function() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda12
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ShortcutPackage.lambda$hasNoShortcut$32(hasAnyShortcut, (ShortcutInfo) obj);
            }
        });
        return !hasAnyShortcut[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Boolean lambda$hasNoShortcut$32(boolean[] hasAnyShortcut, ShortcutInfo si) {
        hasAnyShortcut[0] = true;
        return true;
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    public void saveToXml(TypedXmlSerializer out, boolean forBackup) throws IOException, XmlPullParserException {
        synchronized (this.mLock) {
            int size = this.mShortcuts.size();
            int shareTargetSize = this.mShareTargets.size();
            if (hasNoShortcut() && shareTargetSize == 0 && this.mApiCallCount == 0) {
                return;
            }
            out.startTag((String) null, "package");
            ShortcutService.writeAttr(out, "name", getPackageName());
            ShortcutService.writeAttr(out, ATTR_CALL_COUNT, this.mApiCallCount);
            ShortcutService.writeAttr(out, ATTR_LAST_RESET, this.mLastResetTime);
            if (!forBackup) {
                ShortcutService.writeAttr(out, ATTR_SCHEMA_VERSON, this.mIsAppSearchSchemaUpToDate ? 3L : 0L);
            }
            getPackageInfo().saveToXml(this.mShortcutUser.mService, out, forBackup);
            for (int j = 0; j < size; j++) {
                saveShortcut(out, this.mShortcuts.valueAt(j), forBackup, getPackageInfo().isBackupAllowed());
            }
            if (!forBackup) {
                for (int j2 = 0; j2 < shareTargetSize; j2++) {
                    this.mShareTargets.get(j2).saveToXml(out);
                }
            }
            out.endTag((String) null, "package");
        }
    }

    private void saveShortcut(TypedXmlSerializer out, ShortcutInfo si, boolean forBackup, boolean appSupportsBackup) throws IOException, XmlPullParserException {
        ShortcutService s = this.mShortcutUser.mService;
        if (forBackup && (!si.isPinned() || !si.isEnabled())) {
            return;
        }
        boolean shouldBackupDetails = !forBackup || appSupportsBackup;
        if (si.isIconPendingSave()) {
            removeIcon(si);
        }
        out.startTag((String) null, TAG_SHORTCUT);
        ShortcutService.writeAttr(out, ATTR_ID, si.getId());
        ShortcutService.writeAttr(out, "activity", si.getActivity());
        ShortcutService.writeAttr(out, ATTR_TITLE, si.getTitle());
        ShortcutService.writeAttr(out, ATTR_TITLE_RES_ID, si.getTitleResId());
        ShortcutService.writeAttr(out, ATTR_TITLE_RES_NAME, si.getTitleResName());
        ShortcutService.writeAttr(out, ATTR_SPLASH_SCREEN_THEME_NAME, si.getStartingThemeResName());
        ShortcutService.writeAttr(out, ATTR_TEXT, si.getText());
        ShortcutService.writeAttr(out, ATTR_TEXT_RES_ID, si.getTextResId());
        ShortcutService.writeAttr(out, ATTR_TEXT_RES_NAME, si.getTextResName());
        if (shouldBackupDetails) {
            ShortcutService.writeAttr(out, ATTR_DISABLED_MESSAGE, si.getDisabledMessage());
            ShortcutService.writeAttr(out, ATTR_DISABLED_MESSAGE_RES_ID, si.getDisabledMessageResourceId());
            ShortcutService.writeAttr(out, ATTR_DISABLED_MESSAGE_RES_NAME, si.getDisabledMessageResName());
        }
        ShortcutService.writeAttr(out, ATTR_DISABLED_REASON, si.getDisabledReason());
        ShortcutService.writeAttr(out, "timestamp", si.getLastChangedTimestamp());
        LocusId locusId = si.getLocusId();
        if (locusId != null) {
            ShortcutService.writeAttr(out, ATTR_LOCUS_ID, si.getLocusId().getId());
        }
        if (forBackup) {
            int flags = si.getFlags() & (-35342);
            ShortcutService.writeAttr(out, ATTR_FLAGS, flags);
            long packageVersionCode = getPackageInfo().getVersionCode();
            if (packageVersionCode == 0) {
                s.wtf("Package version code should be available at this point.");
            }
        } else {
            ShortcutService.writeAttr(out, ATTR_RANK, si.getRank());
            ShortcutService.writeAttr(out, ATTR_FLAGS, si.getFlags());
            ShortcutService.writeAttr(out, ATTR_ICON_RES_ID, si.getIconResourceId());
            ShortcutService.writeAttr(out, ATTR_ICON_RES_NAME, si.getIconResName());
            ShortcutService.writeAttr(out, ATTR_BITMAP_PATH, si.getBitmapPath());
            ShortcutService.writeAttr(out, ATTR_ICON_URI, si.getIconUri());
        }
        if (shouldBackupDetails) {
            Set<String> cat = si.getCategories();
            if (cat != null && cat.size() > 0) {
                out.startTag((String) null, "categories");
                XmlUtils.writeStringArrayXml((String[]) cat.toArray(new String[cat.size()]), "categories", XmlUtils.makeTyped(out));
                out.endTag((String) null, "categories");
            }
            if (!forBackup) {
                Person[] persons = si.getPersons();
                if (!ArrayUtils.isEmpty(persons)) {
                    for (Person p : persons) {
                        out.startTag((String) null, TAG_PERSON);
                        ShortcutService.writeAttr(out, "name", p.getName());
                        ShortcutService.writeAttr(out, ATTR_PERSON_URI, p.getUri());
                        ShortcutService.writeAttr(out, ATTR_PERSON_KEY, p.getKey());
                        ShortcutService.writeAttr(out, ATTR_PERSON_IS_BOT, p.isBot());
                        ShortcutService.writeAttr(out, ATTR_PERSON_IS_IMPORTANT, p.isImportant());
                        out.endTag((String) null, TAG_PERSON);
                    }
                }
            }
            Intent[] intentsNoExtras = si.getIntentsNoExtras();
            PersistableBundle[] intentsExtras = si.getIntentPersistableExtrases();
            if (intentsNoExtras != null && intentsExtras != null) {
                int numIntents = intentsNoExtras.length;
                for (int i = 0; i < numIntents; i++) {
                    out.startTag((String) null, "intent");
                    ShortcutService.writeAttr(out, ATTR_INTENT_NO_EXTRA, intentsNoExtras[i]);
                    ShortcutService.writeTagExtra(out, TAG_EXTRAS, intentsExtras[i]);
                    out.endTag((String) null, "intent");
                }
            }
            ShortcutService.writeTagExtra(out, TAG_EXTRAS, si.getExtras());
            Map<String, Map<String, List<String>>> capabilityBindings = si.getCapabilityBindingsInternal();
            if (capabilityBindings != null && !capabilityBindings.isEmpty()) {
                XmlUtils.writeMapXml(capabilityBindings, NAME_CAPABILITY, out);
            }
        }
        out.endTag((String) null, TAG_SHORTCUT);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2027=4] */
    public static ShortcutPackage loadFromFile(ShortcutService s, ShortcutUser shortcutUser, File path, boolean fromBackup) {
        AtomicFile file = new AtomicFile(path);
        try {
            FileInputStream in = file.openRead();
            ShortcutPackage ret = null;
            try {
                TypedXmlPullParser parser = Xml.resolvePullParser(in);
                while (true) {
                    int type = parser.next();
                    if (type == 1) {
                        return ret;
                    }
                    if (type == 2) {
                        int depth = parser.getDepth();
                        String tag = parser.getName();
                        if (depth == 1 && "package".equals(tag)) {
                            ret = loadFromXml(s, shortcutUser, parser, fromBackup);
                        } else {
                            ShortcutService.throwForInvalidTag(depth, tag);
                        }
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                Slog.e(TAG, "Failed to read file " + file.getBaseFile(), e);
                return null;
            } finally {
                IoUtils.closeQuietly(in);
            }
        } catch (FileNotFoundException e2) {
            return null;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static ShortcutPackage loadFromXml(ShortcutService s, ShortcutUser shortcutUser, TypedXmlPullParser parser, boolean fromBackup) throws IOException, XmlPullParserException {
        String packageName = ShortcutService.parseStringAttribute(parser, "name");
        ShortcutPackage ret = new ShortcutPackage(shortcutUser, shortcutUser.getUserId(), packageName);
        synchronized (ret.mLock) {
            ret.mIsAppSearchSchemaUpToDate = ShortcutService.parseIntAttribute(parser, ATTR_SCHEMA_VERSON, 0) == 3;
            ret.mApiCallCount = ShortcutService.parseIntAttribute(parser, ATTR_CALL_COUNT);
            ret.mLastResetTime = ShortcutService.parseLongAttribute(parser, ATTR_LAST_RESET);
            int outerDepth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                    char c = 2;
                    if (type == 2) {
                        int depth = parser.getDepth();
                        String tag = parser.getName();
                        if (depth == outerDepth + 1) {
                            switch (tag.hashCode()) {
                                case -1923478059:
                                    if (tag.equals("package-info")) {
                                        c = 0;
                                        break;
                                    }
                                    c = 65535;
                                    break;
                                case -1680817345:
                                    if (tag.equals(TAG_SHARE_TARGET)) {
                                        break;
                                    }
                                    c = 65535;
                                    break;
                                case -342500282:
                                    if (tag.equals(TAG_SHORTCUT)) {
                                        c = 1;
                                        break;
                                    }
                                    c = 65535;
                                    break;
                                default:
                                    c = 65535;
                                    break;
                            }
                            switch (c) {
                                case 0:
                                    ret.getPackageInfo().loadFromXml(parser, fromBackup);
                                    break;
                                case 1:
                                    try {
                                        ShortcutInfo si = parseShortcut(parser, packageName, shortcutUser.getUserId(), fromBackup);
                                        ret.mShortcuts.put(si.getId(), si);
                                        break;
                                    } catch (Exception e) {
                                        Slog.e(TAG, "Failed parsing shortcut.", e);
                                        break;
                                    }
                                case 2:
                                    ret.mShareTargets.add(ShareTargetInfo.loadFromXml(parser));
                                    break;
                            }
                        }
                        ShortcutService.warnForInvalidTag(depth, tag);
                    }
                }
            }
        }
        return ret;
    }

    /* JADX WARN: Code restructure failed: missing block: B:60:0x01f4, code lost:
        if (r15 == null) goto L12;
     */
    /* JADX WARN: Code restructure failed: missing block: B:61:0x01f6, code lost:
        android.content.pm.ShortcutInfo.setIntentExtras(r15, r1);
        r2.clear();
        r2.add(r15);
     */
    /* JADX WARN: Code restructure failed: missing block: B:62:0x01ff, code lost:
        if (r8 != 0) goto L25;
     */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x0203, code lost:
        if ((r10 & 64) == 0) goto L25;
     */
    /* JADX WARN: Code restructure failed: missing block: B:65:0x0205, code lost:
        r3 = 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:66:0x0208, code lost:
        r3 = r8;
     */
    /* JADX WARN: Code restructure failed: missing block: B:67:0x0209, code lost:
        if (r67 == false) goto L24;
     */
    /* JADX WARN: Code restructure failed: missing block: B:68:0x020b, code lost:
        r6 = r10 | 4096;
     */
    /* JADX WARN: Code restructure failed: missing block: B:69:0x020f, code lost:
        r6 = r10;
     */
    /* JADX WARN: Code restructure failed: missing block: B:70:0x0210, code lost:
        if (r11 != null) goto L23;
     */
    /* JADX WARN: Code restructure failed: missing block: B:71:0x0212, code lost:
        r36 = r9;
     */
    /* JADX WARN: Code restructure failed: missing block: B:72:0x0215, code lost:
        r36 = new android.content.LocusId(r11);
     */
    /* JADX WARN: Code restructure failed: missing block: B:74:0x027d, code lost:
        return new android.content.pm.ShortcutInfo(r66, r7, r65, r39, null, r40, r41, r42, r44, r45, r46, r47, r48, r49, r4, (android.content.Intent[]) r2.toArray(new android.content.Intent[r2.size()]), r17, r56, r50, r6, r16, r52, r53, r54, r3, (android.app.Person[]) r5.toArray(new android.app.Person[r5.size()]), r36, r43, r55);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static ShortcutInfo parseShortcut(TypedXmlPullParser parser, String packageName, int userId, boolean fromBackup) throws IOException, XmlPullParserException {
        PersistableBundle extras;
        int outerDepth;
        int type;
        LocusId locusId;
        Map<String, Map<String, List<String>>> capabilityBindings;
        PersistableBundle extras2;
        Map<String, Map<String, List<String>>> capabilityBindings2;
        String str;
        int outerDepth2;
        char c;
        PersistableBundle intentPersistableExtrasLegacy = null;
        ArrayList<Intent> intents = new ArrayList<>();
        PersistableBundle extras3 = null;
        ArraySet<String> categories = null;
        ArrayList<Person> persons = new ArrayList<>();
        Map<String, Map<String, List<String>>> capabilityBindings3 = null;
        String id = ShortcutService.parseStringAttribute(parser, ATTR_ID);
        ComponentName activityComponent = ShortcutService.parseComponentNameAttribute(parser, "activity");
        String title = ShortcutService.parseStringAttribute(parser, ATTR_TITLE);
        int titleResId = ShortcutService.parseIntAttribute(parser, ATTR_TITLE_RES_ID);
        String titleResName = ShortcutService.parseStringAttribute(parser, ATTR_TITLE_RES_NAME);
        String splashScreenThemeResName = ShortcutService.parseStringAttribute(parser, ATTR_SPLASH_SCREEN_THEME_NAME);
        String text = ShortcutService.parseStringAttribute(parser, ATTR_TEXT);
        int textResId = ShortcutService.parseIntAttribute(parser, ATTR_TEXT_RES_ID);
        String textResName = ShortcutService.parseStringAttribute(parser, ATTR_TEXT_RES_NAME);
        String disabledMessage = ShortcutService.parseStringAttribute(parser, ATTR_DISABLED_MESSAGE);
        int disabledMessageResId = ShortcutService.parseIntAttribute(parser, ATTR_DISABLED_MESSAGE_RES_ID);
        String disabledMessageResName = ShortcutService.parseStringAttribute(parser, ATTR_DISABLED_MESSAGE_RES_NAME);
        int disabledReason = ShortcutService.parseIntAttribute(parser, ATTR_DISABLED_REASON);
        String str2 = "intent";
        Intent intentLegacy = ShortcutService.parseIntentAttributeNoDefault(parser, "intent");
        int rank = (int) ShortcutService.parseLongAttribute(parser, ATTR_RANK);
        long lastChangedTimestamp = ShortcutService.parseLongAttribute(parser, "timestamp");
        int flags = (int) ShortcutService.parseLongAttribute(parser, ATTR_FLAGS);
        int depth = (int) ShortcutService.parseLongAttribute(parser, ATTR_ICON_RES_ID);
        String iconResName = ShortcutService.parseStringAttribute(parser, ATTR_ICON_RES_NAME);
        String bitmapPath = ShortcutService.parseStringAttribute(parser, ATTR_BITMAP_PATH);
        String iconUri = ShortcutService.parseStringAttribute(parser, ATTR_ICON_URI);
        String locusIdString = ShortcutService.parseStringAttribute(parser, ATTR_LOCUS_ID);
        int outerDepth3 = parser.getDepth();
        while (true) {
            int iconResId = depth;
            int type2 = parser.next();
            int rank2 = rank;
            if (type2 == 1) {
                extras = extras3;
                outerDepth = outerDepth3;
                type = type2;
                locusId = null;
                capabilityBindings = capabilityBindings3;
            } else if (type2 != 3 || parser.getDepth() > outerDepth3) {
                if (type2 != 2) {
                    extras2 = extras3;
                    capabilityBindings2 = capabilityBindings3;
                    str = str2;
                    outerDepth2 = outerDepth3;
                } else {
                    int depth2 = parser.getDepth();
                    outerDepth2 = outerDepth3;
                    String tag = parser.getName();
                    capabilityBindings2 = capabilityBindings3;
                    switch (tag.hashCode()) {
                        case -1289032093:
                            extras2 = extras3;
                            if (tag.equals(TAG_EXTRAS)) {
                                c = 2;
                                break;
                            }
                            c = 65535;
                            break;
                        case -1183762788:
                            extras2 = extras3;
                            if (tag.equals(str2)) {
                                c = 1;
                                break;
                            }
                            c = 65535;
                            break;
                        case -1044333900:
                            extras2 = extras3;
                            if (tag.equals(TAG_INTENT_EXTRAS_LEGACY)) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        case -1024600675:
                            extras2 = extras3;
                            if (tag.equals(TAG_STRING_ARRAY_XMLUTILS)) {
                                c = 5;
                                break;
                            }
                            c = 65535;
                            break;
                        case -991716523:
                            extras2 = extras3;
                            if (tag.equals(TAG_PERSON)) {
                                c = 4;
                                break;
                            }
                            c = 65535;
                            break;
                        case 107868:
                            extras2 = extras3;
                            if (tag.equals(TAG_MAP_XMLUTILS)) {
                                c = 6;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1296516636:
                            extras2 = extras3;
                            if (tag.equals("categories")) {
                                c = 3;
                                break;
                            }
                            c = 65535;
                            break;
                        default:
                            extras2 = extras3;
                            c = 65535;
                            break;
                    }
                    str = str2;
                    switch (c) {
                        case 0:
                            intentPersistableExtrasLegacy = PersistableBundle.restoreFromXml(parser);
                            depth = iconResId;
                            rank = rank2;
                            str2 = str;
                            outerDepth3 = outerDepth2;
                            capabilityBindings3 = capabilityBindings2;
                            extras3 = extras2;
                            continue;
                        case 1:
                            intents.add(parseIntent(parser));
                            break;
                        case 2:
                            extras3 = PersistableBundle.restoreFromXml(parser);
                            depth = iconResId;
                            rank = rank2;
                            str2 = str;
                            outerDepth3 = outerDepth2;
                            capabilityBindings3 = capabilityBindings2;
                            continue;
                        case 3:
                            break;
                        case 4:
                            persons.add(parsePerson(parser));
                            break;
                        case 5:
                            if (!"categories".equals(ShortcutService.parseStringAttribute(parser, "name"))) {
                                break;
                            } else {
                                String[] ar = XmlUtils.readThisStringArrayXml(XmlUtils.makeTyped(parser), TAG_STRING_ARRAY_XMLUTILS, (String[]) null);
                                categories = new ArraySet<>(ar.length);
                                for (String str3 : ar) {
                                    categories.add(str3);
                                }
                                depth = iconResId;
                                rank = rank2;
                                str2 = str;
                                outerDepth3 = outerDepth2;
                                capabilityBindings3 = capabilityBindings2;
                                extras3 = extras2;
                                continue;
                            }
                        case 6:
                            if (!NAME_CAPABILITY.equals(ShortcutService.parseStringAttribute(parser, "name"))) {
                                break;
                            } else {
                                capabilityBindings3 = (Map) XmlUtils.readValueXml(parser, new String[1]);
                                depth = iconResId;
                                rank = rank2;
                                str2 = str;
                                outerDepth3 = outerDepth2;
                                extras3 = extras2;
                                continue;
                            }
                        default:
                            throw ShortcutService.throwForInvalidTag(depth2, tag);
                    }
                }
                depth = iconResId;
                rank = rank2;
                str2 = str;
                outerDepth3 = outerDepth2;
                capabilityBindings3 = capabilityBindings2;
                extras3 = extras2;
            } else {
                extras = extras3;
                capabilityBindings = capabilityBindings3;
                outerDepth = outerDepth3;
                type = type2;
                locusId = null;
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:22:0x004a, code lost:
        return r0;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static Intent parseIntent(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        Intent intent = ShortcutService.parseIntentAttribute(parser, ATTR_INTENT_NO_EXTRA);
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                if (type == 2) {
                    int depth = parser.getDepth();
                    String tag = parser.getName();
                    char c = 65535;
                    switch (tag.hashCode()) {
                        case -1289032093:
                            if (tag.equals(TAG_EXTRAS)) {
                                c = 0;
                                break;
                            }
                            break;
                    }
                    switch (c) {
                        case 0:
                            ShortcutInfo.setIntentExtras(intent, PersistableBundle.restoreFromXml(parser));
                            break;
                        default:
                            throw ShortcutService.throwForInvalidTag(depth, tag);
                    }
                }
            }
        }
    }

    private static Person parsePerson(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        CharSequence name = ShortcutService.parseStringAttribute(parser, "name");
        String uri = ShortcutService.parseStringAttribute(parser, ATTR_PERSON_URI);
        String key = ShortcutService.parseStringAttribute(parser, ATTR_PERSON_KEY);
        boolean isBot = ShortcutService.parseBooleanAttribute(parser, ATTR_PERSON_IS_BOT);
        boolean isImportant = ShortcutService.parseBooleanAttribute(parser, ATTR_PERSON_IS_IMPORTANT);
        Person.Builder builder = new Person.Builder();
        builder.setName(name).setUri(uri).setKey(key).setBot(isBot).setImportant(isImportant);
        return builder.build();
    }

    List<ShortcutInfo> getAllShortcutsForTest() {
        List<ShortcutInfo> ret = new ArrayList<>(1);
        Objects.requireNonNull(ret);
        forEachShortcut(new ShortcutPackage$$ExternalSyntheticLambda46(ret));
        return ret;
    }

    List<ShareTargetInfo> getAllShareTargetsForTest() {
        ArrayList arrayList;
        synchronized (this.mLock) {
            arrayList = new ArrayList(this.mShareTargets);
        }
        return arrayList;
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    public void verifyStates() {
        super.verifyStates();
        final boolean[] failed = new boolean[1];
        final ShortcutService s = this.mShortcutUser.mService;
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> all = sortShortcutsToActivities();
        for (int outer = all.size() - 1; outer >= 0; outer--) {
            ArrayList<ShortcutInfo> list = all.valueAt(outer);
            if (list.size() > this.mShortcutUser.mService.getMaxActivityShortcuts()) {
                failed[0] = true;
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": activity " + all.keyAt(outer) + " has " + all.valueAt(outer).size() + " shortcuts.");
            }
            Collections.sort(list, new Comparator() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda40
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    int compare;
                    compare = Integer.compare(((ShortcutInfo) obj).getRank(), ((ShortcutInfo) obj2).getRank());
                    return compare;
                }
            });
            ArrayList<ShortcutInfo> dynamicList = new ArrayList<>(list);
            dynamicList.removeIf(new Predicate() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda41
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ShortcutPackage.lambda$verifyStates$34((ShortcutInfo) obj);
                }
            });
            ArrayList<ShortcutInfo> manifestList = new ArrayList<>(list);
            manifestList.removeIf(new Predicate() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda42
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ShortcutPackage.lambda$verifyStates$35((ShortcutInfo) obj);
                }
            });
            verifyRanksSequential(dynamicList);
            verifyRanksSequential(manifestList);
        }
        forEachShortcut(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda43
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5642lambda$verifyStates$36$comandroidserverpmShortcutPackage(failed, s, (ShortcutInfo) obj);
            }
        });
        if (failed[0]) {
            throw new IllegalStateException("See logcat for errors");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$verifyStates$34(ShortcutInfo si) {
        return !si.isDynamic();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$verifyStates$35(ShortcutInfo si) {
        return !si.isManifestShortcut();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$verifyStates$36$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5642lambda$verifyStates$36$comandroidserverpmShortcutPackage(boolean[] failed, ShortcutService s, ShortcutInfo si) {
        if (!si.isDeclaredInManifest() && !si.isDynamic() && !si.isPinned() && !si.isCached()) {
            failed[0] = true;
            Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " is not manifest, dynamic or pinned.");
        }
        if (si.isDeclaredInManifest() && si.isDynamic()) {
            failed[0] = true;
            Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " is both dynamic and manifest at the same time.");
        }
        if (si.getActivity() == null && !si.isFloating()) {
            failed[0] = true;
            Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " has null activity, but not floating.");
        }
        if ((si.isDynamic() || si.isManifestShortcut()) && !si.isEnabled()) {
            failed[0] = true;
            Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " is not floating, but is disabled.");
        }
        if (si.isFloating() && si.getRank() != 0) {
            failed[0] = true;
            Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " is floating, but has rank=" + si.getRank());
        }
        if (si.getIcon() != null) {
            failed[0] = true;
            Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " still has an icon");
        }
        if (si.hasAdaptiveBitmap() && !si.hasIconFile() && !si.hasIconUri()) {
            failed[0] = true;
            Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " has adaptive bitmap but was not saved to a file nor has icon uri.");
        }
        if (si.hasIconFile() && si.hasIconResource()) {
            failed[0] = true;
            Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " has both resource and bitmap icons");
        }
        if (si.hasIconFile() && si.hasIconUri()) {
            failed[0] = true;
            Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " has both url and bitmap icons");
        }
        if (si.hasIconUri() && si.hasIconResource()) {
            failed[0] = true;
            Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " has both url and resource icons");
        }
        if (si.isEnabled() != (si.getDisabledReason() == 0)) {
            failed[0] = true;
            Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " isEnabled() and getDisabledReason() disagree: " + si.isEnabled() + " vs " + si.getDisabledReason());
        }
        if (si.getDisabledReason() == 100 && getPackageInfo().getBackupSourceVersionCode() == -1) {
            failed[0] = true;
            Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " RESTORED_VERSION_LOWER with no backup source version code.");
        }
        if (s.isDummyMainActivity(si.getActivity())) {
            failed[0] = true;
            Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " has a dummy target activity");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void mutateShortcut(String id, ShortcutInfo shortcut, Consumer<ShortcutInfo> transform) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(transform);
        synchronized (this.mLock) {
            if (shortcut != null) {
                transform.accept(shortcut);
            }
            ShortcutInfo si = findShortcutById(id);
            if (si == null) {
                return;
            }
            transform.accept(si);
            saveShortcut(si);
        }
    }

    private void saveShortcut(ShortcutInfo... shortcuts) {
        Objects.requireNonNull(shortcuts);
        saveShortcut(Arrays.asList(shortcuts));
    }

    private void saveShortcut(Collection<ShortcutInfo> shortcuts) {
        Objects.requireNonNull(shortcuts);
        synchronized (this.mLock) {
            for (ShortcutInfo si : shortcuts) {
                this.mShortcuts.put(si.getId(), si);
            }
        }
    }

    List<ShortcutInfo> findAll(Collection<String> ids) {
        List<ShortcutInfo> list;
        synchronized (this.mLock) {
            Stream<String> stream = ids.stream();
            final ArrayMap<String, ShortcutInfo> arrayMap = this.mShortcuts;
            Objects.requireNonNull(arrayMap);
            list = (List) stream.map(new Function() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda24
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return (ShortcutInfo) arrayMap.get((String) obj);
                }
            }).filter(new Predicate() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda25
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return Objects.nonNull((ShortcutInfo) obj);
                }
            }).collect(Collectors.toList());
        }
        return list;
    }

    private void forEachShortcut(final Consumer<ShortcutInfo> cb) {
        forEachShortcutStopWhen(new Function() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda58
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ShortcutPackage.lambda$forEachShortcut$37(cb, (ShortcutInfo) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Boolean lambda$forEachShortcut$37(Consumer cb, ShortcutInfo si) {
        cb.accept(si);
        return false;
    }

    private void forEachShortcutMutate(Consumer<ShortcutInfo> cb) {
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = this.mShortcuts.valueAt(i);
            cb.accept(si);
        }
    }

    private void forEachShortcutStopWhen(Function<ShortcutInfo, Boolean> cb) {
        synchronized (this.mLock) {
            for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
                ShortcutInfo si = this.mShortcuts.valueAt(i);
                if (cb.apply(si).booleanValue()) {
                    return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public AndroidFuture<AppSearchSession> setupSchema(final AppSearchSession session) {
        SetSchemaRequest.Builder schemaBuilder = new SetSchemaRequest.Builder().addSchemas(AppSearchShortcutPerson.SCHEMA, AppSearchShortcutInfo.SCHEMA).setForceOverride(true).addRequiredPermissionsForSchemaTypeVisibility("Shortcut", Collections.singleton(5)).addRequiredPermissionsForSchemaTypeVisibility("Shortcut", Collections.singleton(6)).addRequiredPermissionsForSchemaTypeVisibility("ShortcutPerson", Collections.singleton(5)).addRequiredPermissionsForSchemaTypeVisibility("ShortcutPerson", Collections.singleton(6));
        final AndroidFuture<AppSearchSession> future = new AndroidFuture<>();
        session.setSchema(schemaBuilder.build(), this.mExecutor, this.mShortcutUser.mExecutor, new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda11
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.lambda$setupSchema$38(future, session, (AppSearchResult) obj);
            }
        });
        return future;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setupSchema$38(AndroidFuture future, AppSearchSession session, AppSearchResult result) {
        if (!result.isSuccess()) {
            future.completeExceptionally(new IllegalArgumentException(result.getErrorMessage()));
        } else {
            future.complete(session);
        }
    }

    private SearchSpec getSearchSpec() {
        return new SearchSpec.Builder().addFilterSchemas("Shortcut").addFilterNamespaces(getPackageName()).setTermMatch(1).setResultCountPerPage(this.mShortcutUser.mService.getMaxActivityShortcuts()).build();
    }

    private boolean verifyRanksSequential(List<ShortcutInfo> list) {
        boolean failed = false;
        for (int i = 0; i < list.size(); i++) {
            ShortcutInfo si = list.get(i);
            if (si.getRank() != i) {
                failed = true;
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + si.getId() + " rank=" + si.getRank() + " but expected to be " + i);
            }
        }
        return failed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAllShortcutsAsync() {
        if (!isAppSearchEnabled()) {
            return;
        }
        runAsSystem(new Runnable() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda37
            @Override // java.lang.Runnable
            public final void run() {
                ShortcutPackage.this.m5634x76677d5e();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeAllShortcutsAsync$41$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5634x76677d5e() {
        fromAppSearch().thenAccept(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda54
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5633xad66861d((AppSearchSession) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeAllShortcutsAsync$40$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5633xad66861d(AppSearchSession session) {
        session.remove("", getSearchSpec(), this.mShortcutUser.mExecutor, new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda45
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.lambda$removeAllShortcutsAsync$39((AppSearchResult) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$removeAllShortcutsAsync$39(AppSearchResult result) {
        if (!result.isSuccess()) {
            Slog.e(TAG, "Failed to remove shortcuts from AppSearch. " + result.getErrorMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getShortcutByIdsAsync(final Set<String> ids, final Consumer<List<ShortcutInfo>> cb) {
        if (!isAppSearchEnabled()) {
            cb.accept(Collections.emptyList());
        } else {
            runAsSystem(new Runnable() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda38
                @Override // java.lang.Runnable
                public final void run() {
                    ShortcutPackage.this.m5625xebef25f5(ids, cb);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getShortcutByIdsAsync$43$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5625xebef25f5(final Set ids, final Consumer cb) {
        fromAppSearch().thenAccept(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5624x22ee2eb4(ids, cb, (AppSearchSession) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getShortcutByIdsAsync$42$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5624x22ee2eb4(Set ids, Consumer cb, AppSearchSession session) {
        session.getByDocumentId(new GetByDocumentIdRequest.Builder(getPackageName()).addIds(ids).build(), this.mShortcutUser.mExecutor, new AnonymousClass1(cb));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.pm.ShortcutPackage$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass1 implements BatchResultCallback<String, GenericDocument> {
        final /* synthetic */ Consumer val$cb;

        AnonymousClass1(Consumer consumer) {
            this.val$cb = consumer;
        }

        @Override // android.app.appsearch.BatchResultCallback
        public void onResult(AppSearchBatchResult<String, GenericDocument> appSearchBatchResult) {
            List ret = (List) appSearchBatchResult.getSuccesses().values().stream().map(new Function() { // from class: com.android.server.pm.ShortcutPackage$1$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return ShortcutPackage.AnonymousClass1.this.m5643lambda$onResult$0$comandroidserverpmShortcutPackage$1((GenericDocument) obj);
                }
            }).collect(Collectors.toList());
            this.val$cb.accept(ret);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onResult$0$com-android-server-pm-ShortcutPackage$1  reason: not valid java name */
        public /* synthetic */ ShortcutInfo m5643lambda$onResult$0$comandroidserverpmShortcutPackage$1(GenericDocument doc) {
            return ShortcutInfo.createFromGenericDocument(ShortcutPackage.this.mShortcutUser.getUserId(), doc);
        }

        @Override // android.app.appsearch.BatchResultCallback
        public void onSystemError(Throwable throwable) {
            Slog.d(ShortcutPackage.TAG, "Error retrieving shortcuts", throwable);
        }
    }

    private void removeShortcutAsync(String... id) {
        Objects.requireNonNull(id);
        removeShortcutAsync(Arrays.asList(id));
    }

    private void removeShortcutAsync(final Collection<String> ids) {
        if (!isAppSearchEnabled()) {
            return;
        }
        runAsSystem(new Runnable() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda51
            @Override // java.lang.Runnable
            public final void run() {
                ShortcutPackage.this.m5636x699e515c(ids);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeShortcutAsync$45$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5636x699e515c(final Collection ids) {
        fromAppSearch().thenAccept(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5635xa09d5a1b(ids, (AppSearchSession) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeShortcutAsync$44$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5635xa09d5a1b(Collection ids, AppSearchSession session) {
        session.remove(new RemoveByDocumentIdRequest.Builder(getPackageName()).addIds(ids).build(), this.mShortcutUser.mExecutor, new BatchResultCallback<String, Void>() { // from class: com.android.server.pm.ShortcutPackage.2
            @Override // android.app.appsearch.BatchResultCallback
            public void onResult(AppSearchBatchResult<String, Void> appSearchBatchResult) {
                if (!appSearchBatchResult.isSuccess()) {
                    Map failures = appSearchBatchResult.getFailures();
                    for (String key : failures.keySet()) {
                        Slog.e(ShortcutPackage.TAG, "Failed deleting " + key + ", error message:" + failures.get(key).getErrorMessage());
                    }
                }
            }

            @Override // android.app.appsearch.BatchResultCallback
            public void onSystemError(Throwable throwable) {
                Slog.e(ShortcutPackage.TAG, "Error removing shortcuts", throwable);
            }
        });
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    void scheduleSaveToAppSearchLocked() {
        Map<String, ShortcutInfo> copy = new ArrayMap<>(this.mShortcuts);
        if (!this.mTransientShortcuts.isEmpty()) {
            copy.putAll(this.mTransientShortcuts);
            this.mTransientShortcuts.clear();
        }
        saveShortcutsAsync((Collection) copy.values().stream().filter(new Predicate() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda34
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ((ShortcutInfo) obj).usesQuota();
            }
        }).collect(Collectors.toList()));
    }

    private void saveShortcutsAsync(final Collection<ShortcutInfo> shortcuts) {
        Objects.requireNonNull(shortcuts);
        if (!isAppSearchEnabled() || shortcuts.isEmpty()) {
            return;
        }
        runAsSystem(new Runnable() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda36
            @Override // java.lang.Runnable
            public final void run() {
                ShortcutPackage.this.m5640xb4af254a(shortcuts);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$saveShortcutsAsync$47$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5640xb4af254a(final Collection shortcuts) {
        fromAppSearch().thenAccept(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda28
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5639xebae2e09(shortcuts, (AppSearchSession) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$saveShortcutsAsync$46$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5639xebae2e09(Collection shortcuts, AppSearchSession session) {
        if (shortcuts.isEmpty()) {
            return;
        }
        session.put(new PutDocumentsRequest.Builder().addGenericDocuments(AppSearchShortcutInfo.toGenericDocuments(shortcuts)).build(), this.mShortcutUser.mExecutor, new BatchResultCallback<String, Void>() { // from class: com.android.server.pm.ShortcutPackage.3
            @Override // android.app.appsearch.BatchResultCallback
            public void onResult(AppSearchBatchResult<String, Void> appSearchBatchResult) {
                if (!appSearchBatchResult.isSuccess()) {
                    for (AppSearchResult k : appSearchBatchResult.getFailures().values()) {
                        Slog.e(ShortcutPackage.TAG, k.getErrorMessage());
                    }
                }
            }

            @Override // android.app.appsearch.BatchResultCallback
            public void onSystemError(Throwable throwable) {
                Slog.d(ShortcutPackage.TAG, "Error persisting shortcuts", throwable);
            }
        });
    }

    void getTopShortcutsFromPersistence(final AndroidFuture<List<ShortcutInfo>> cb) {
        if (!isAppSearchEnabled()) {
            cb.complete((Object) null);
        }
        runAsSystem(new Runnable() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda44
            @Override // java.lang.Runnable
            public final void run() {
                ShortcutPackage.this.m5629x4b593bc8(cb);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getTopShortcutsFromPersistence$51$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5629x4b593bc8(final AndroidFuture cb) {
        fromAppSearch().thenAccept(new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda13
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5628x82584487(cb, (AppSearchSession) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getTopShortcutsFromPersistence$50$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5628x82584487(final AndroidFuture cb, AppSearchSession session) {
        SearchResults res = session.search("", getSearchSpec());
        res.getNextPage(this.mShortcutUser.mExecutor, new Consumer() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda15
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutPackage.this.m5627x3c4304f1(cb, (AppSearchResult) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getTopShortcutsFromPersistence$49$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ void m5627x3c4304f1(AndroidFuture cb, AppSearchResult results) {
        if (!results.isSuccess()) {
            cb.completeExceptionally(new IllegalStateException(results.getErrorMessage()));
        } else {
            cb.complete((List) ((List) results.getResultValue()).stream().map(new Function() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda22
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return ((SearchResult) obj).getGenericDocument();
                }
            }).map(new Function() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda23
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return ShortcutPackage.this.m5626x73420db0((GenericDocument) obj);
                }
            }).collect(Collectors.toList()));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getTopShortcutsFromPersistence$48$com-android-server-pm-ShortcutPackage  reason: not valid java name */
    public /* synthetic */ ShortcutInfo m5626x73420db0(GenericDocument doc) {
        return ShortcutInfo.createFromGenericDocument(this.mShortcutUser.getUserId(), doc);
    }

    private AndroidFuture<AppSearchSession> fromAppSearch() {
        StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        AppSearchManager.SearchContext searchContext = new AppSearchManager.SearchContext.Builder(getPackageName()).build();
        AndroidFuture<AppSearchSession> future = null;
        try {
            try {
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
                future = this.mShortcutUser.getAppSearch(searchContext);
                synchronized (this.mLock) {
                    if (!this.mIsAppSearchSchemaUpToDate) {
                        future = future.thenCompose(new Function() { // from class: com.android.server.pm.ShortcutPackage$$ExternalSyntheticLambda47
                            @Override // java.util.function.Function
                            public final Object apply(Object obj) {
                                CompletionStage completionStage;
                                completionStage = ShortcutPackage.this.setupSchema((AppSearchSession) obj);
                                return completionStage;
                            }
                        });
                    }
                    this.mIsAppSearchSchemaUpToDate = true;
                }
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create app search session. pkg=" + getPackageName() + " user=" + this.mShortcutUser.getUserId(), e);
                ((AndroidFuture) Objects.requireNonNull(future)).completeExceptionally(e);
            }
            return (AndroidFuture) Objects.requireNonNull(future);
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void runAsSystem(Runnable fn) {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            fn.run();
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    protected File getShortcutPackageItemFile() {
        File path = new File(this.mShortcutUser.mService.injectUserDataPath(this.mShortcutUser.getUserId()), "packages");
        String fileName = getPackageName() + ".xml";
        return new File(path, fileName);
    }
}
