package com.android.server.notification;

import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutServiceInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/* loaded from: classes2.dex */
public class ShortcutHelper {
    private static final IntentFilter SHARING_FILTER;
    private static final String TAG = "ShortcutHelper";
    private HashMap<String, HashMap<String, String>> mActiveShortcutBubbles = new HashMap<>();
    private final LauncherApps.Callback mLauncherAppsCallback = new LauncherApps.Callback() { // from class: com.android.server.notification.ShortcutHelper.1
        @Override // android.content.pm.LauncherApps.Callback
        public void onPackageRemoved(String packageName, UserHandle user) {
        }

        @Override // android.content.pm.LauncherApps.Callback
        public void onPackageAdded(String packageName, UserHandle user) {
        }

        @Override // android.content.pm.LauncherApps.Callback
        public void onPackageChanged(String packageName, UserHandle user) {
        }

        @Override // android.content.pm.LauncherApps.Callback
        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
        }

        @Override // android.content.pm.LauncherApps.Callback
        public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
        }

        @Override // android.content.pm.LauncherApps.Callback
        public void onShortcutsChanged(String packageName, List<ShortcutInfo> shortcuts, UserHandle user) {
            HashMap<String, String> shortcutBubbles = (HashMap) ShortcutHelper.this.mActiveShortcutBubbles.get(packageName);
            ArrayList<String> bubbleKeysToRemove = new ArrayList<>();
            if (shortcutBubbles != null) {
                Set<String> shortcutIds = new HashSet<>(shortcutBubbles.keySet());
                for (String shortcutId : shortcutIds) {
                    boolean foundShortcut = false;
                    int i = 0;
                    while (true) {
                        if (i < shortcuts.size()) {
                            if (!shortcuts.get(i).getId().equals(shortcutId)) {
                                i++;
                            } else {
                                foundShortcut = true;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (!foundShortcut) {
                        bubbleKeysToRemove.add(shortcutBubbles.get(shortcutId));
                        shortcutBubbles.remove(shortcutId);
                        if (shortcutBubbles.isEmpty()) {
                            ShortcutHelper.this.mActiveShortcutBubbles.remove(packageName);
                            if (ShortcutHelper.this.mLauncherAppsCallbackRegistered && ShortcutHelper.this.mActiveShortcutBubbles.isEmpty()) {
                                ShortcutHelper.this.mLauncherAppsService.unregisterCallback(ShortcutHelper.this.mLauncherAppsCallback);
                                ShortcutHelper.this.mLauncherAppsCallbackRegistered = false;
                            }
                        }
                    }
                }
            }
            for (int i2 = 0; i2 < bubbleKeysToRemove.size(); i2++) {
                String bubbleKey = bubbleKeysToRemove.get(i2);
                if (ShortcutHelper.this.mShortcutListener != null) {
                    ShortcutHelper.this.mShortcutListener.onShortcutRemoved(bubbleKey);
                }
            }
        }
    };
    private boolean mLauncherAppsCallbackRegistered;
    private LauncherApps mLauncherAppsService;
    private ShortcutListener mShortcutListener;
    private ShortcutServiceInternal mShortcutServiceInternal;
    private UserManager mUserManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface ShortcutListener {
        void onShortcutRemoved(String str);
    }

    static {
        IntentFilter intentFilter = new IntentFilter();
        SHARING_FILTER = intentFilter;
        try {
            intentFilter.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Slog.e(TAG, "Bad mime type", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ShortcutHelper(LauncherApps launcherApps, ShortcutListener listener, ShortcutServiceInternal shortcutServiceInternal, UserManager userManager) {
        this.mLauncherAppsService = launcherApps;
        this.mShortcutListener = listener;
        this.mShortcutServiceInternal = shortcutServiceInternal;
        this.mUserManager = userManager;
    }

    void setLauncherApps(LauncherApps launcherApps) {
        this.mLauncherAppsService = launcherApps;
    }

    void setShortcutServiceInternal(ShortcutServiceInternal shortcutServiceInternal) {
        this.mShortcutServiceInternal = shortcutServiceInternal;
    }

    void setUserManager(UserManager userManager) {
        this.mUserManager = userManager;
    }

    public static boolean isConversationShortcut(ShortcutInfo shortcutInfo, ShortcutServiceInternal mShortcutServiceInternal, int callingUserId) {
        if (shortcutInfo == null || !shortcutInfo.isLongLived() || !shortcutInfo.isEnabled()) {
            return false;
        }
        return true;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [216=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public ShortcutInfo getValidShortcutInfo(String shortcutId, String packageName, UserHandle user) {
        if (this.mLauncherAppsService == null || !this.mUserManager.isUserUnlocked(user)) {
            return null;
        }
        long token = Binder.clearCallingIdentity();
        if (shortcutId == null || packageName == null || user == null) {
            return null;
        }
        try {
            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
            query.setPackage(packageName);
            query.setShortcutIds(Arrays.asList(shortcutId));
            query.setQueryFlags(3089);
            List<ShortcutInfo> shortcuts = this.mLauncherAppsService.getShortcuts(query, user);
            ShortcutInfo info = (shortcuts == null || shortcuts.size() <= 0) ? null : shortcuts.get(0);
            if (isConversationShortcut(info, this.mShortcutServiceInternal, user.getIdentifier())) {
                return info;
            }
            return null;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cacheShortcut(ShortcutInfo shortcutInfo, UserHandle user) {
        if (shortcutInfo.isLongLived() && !shortcutInfo.isCached()) {
            this.mShortcutServiceInternal.cacheShortcuts(user.getIdentifier(), PackageManagerService.PLATFORM_PACKAGE_NAME, shortcutInfo.getPackage(), Collections.singletonList(shortcutInfo.getId()), shortcutInfo.getUserId(), 16384);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void maybeListenForShortcutChangesForBubbles(NotificationRecord r, boolean removedNotification, Handler handler) {
        String shortcutId;
        if (r.getNotification().getBubbleMetadata() != null) {
            shortcutId = r.getNotification().getBubbleMetadata().getShortcutId();
        } else {
            shortcutId = null;
        }
        if (!removedNotification && !TextUtils.isEmpty(shortcutId) && r.getShortcutInfo() != null && r.getShortcutInfo().getId().equals(shortcutId)) {
            HashMap<String, String> packageBubbles = this.mActiveShortcutBubbles.get(r.getSbn().getPackageName());
            if (packageBubbles == null) {
                packageBubbles = new HashMap<>();
            }
            packageBubbles.put(shortcutId, r.getKey());
            this.mActiveShortcutBubbles.put(r.getSbn().getPackageName(), packageBubbles);
            if (!this.mLauncherAppsCallbackRegistered) {
                this.mLauncherAppsService.registerCallback(this.mLauncherAppsCallback, handler);
                this.mLauncherAppsCallbackRegistered = true;
                return;
            }
            return;
        }
        HashMap<String, String> packageBubbles2 = this.mActiveShortcutBubbles.get(r.getSbn().getPackageName());
        if (packageBubbles2 != null) {
            if (!TextUtils.isEmpty(shortcutId)) {
                packageBubbles2.remove(shortcutId);
            } else {
                Set<String> shortcutIds = new HashSet<>(packageBubbles2.keySet());
                for (String pkgShortcutId : shortcutIds) {
                    String entryKey = packageBubbles2.get(pkgShortcutId);
                    if (r.getKey().equals(entryKey)) {
                        packageBubbles2.remove(pkgShortcutId);
                    }
                }
            }
            if (packageBubbles2.isEmpty()) {
                this.mActiveShortcutBubbles.remove(r.getSbn().getPackageName());
            }
        }
        if (this.mLauncherAppsCallbackRegistered && this.mActiveShortcutBubbles.isEmpty()) {
            this.mLauncherAppsService.unregisterCallback(this.mLauncherAppsCallback);
            this.mLauncherAppsCallbackRegistered = false;
        }
    }
}
