package com.android.server.policy;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.SparseArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import com.android.internal.policy.IShortcutService;
import com.android.internal.util.XmlUtils;
import com.android.server.slice.SliceClientPermissions;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
class ModifierShortcutManager {
    private static final String ATTRIBUTE_CATEGORY = "category";
    private static final String ATTRIBUTE_CLASS = "class";
    private static final String ATTRIBUTE_PACKAGE = "package";
    private static final String ATTRIBUTE_SHIFT = "shift";
    private static final String ATTRIBUTE_SHORTCUT = "shortcut";
    private static final String TAG = "WindowManager";
    private static final String TAG_BOOKMARK = "bookmark";
    private static final String TAG_BOOKMARKS = "bookmarks";
    static SparseArray<String> sApplicationLaunchKeyCategories;
    private final Context mContext;
    private final SparseArray<ShortcutInfo> mIntentShortcuts = new SparseArray<>();
    private final SparseArray<ShortcutInfo> mShiftShortcuts = new SparseArray<>();
    private LongSparseArray<IShortcutService> mShortcutKeyServices = new LongSparseArray<>();
    private boolean mSearchKeyShortcutPending = false;
    private boolean mConsumeSearchKeyUp = true;

    static {
        SparseArray<String> sparseArray = new SparseArray<>();
        sApplicationLaunchKeyCategories = sparseArray;
        sparseArray.append(64, "android.intent.category.APP_BROWSER");
        sApplicationLaunchKeyCategories.append(65, "android.intent.category.APP_EMAIL");
        sApplicationLaunchKeyCategories.append(207, "android.intent.category.APP_CONTACTS");
        sApplicationLaunchKeyCategories.append(208, "android.intent.category.APP_CALENDAR");
        sApplicationLaunchKeyCategories.append(209, "android.intent.category.APP_MUSIC");
        sApplicationLaunchKeyCategories.append(210, "android.intent.category.APP_CALCULATOR");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ModifierShortcutManager(Context context) {
        this.mContext = context;
        loadShortcuts();
    }

    private Intent getIntent(KeyCharacterMap kcm, int keyCode, int metaState) {
        int shortcutChar;
        boolean isShiftOn = KeyEvent.metaStateHasModifiers(metaState, 1);
        if (!isShiftOn && !KeyEvent.metaStateHasNoModifiers(metaState)) {
            return null;
        }
        ShortcutInfo shortcut = null;
        SparseArray<ShortcutInfo> shortcutMap = isShiftOn ? this.mShiftShortcuts : this.mIntentShortcuts;
        int shortcutChar2 = kcm.get(keyCode, metaState);
        if (shortcutChar2 != 0) {
            ShortcutInfo shortcut2 = shortcutMap.get(shortcutChar2);
            shortcut = shortcut2;
        }
        if (shortcut == null && (shortcutChar = Character.toLowerCase(kcm.getDisplayLabel(keyCode))) != 0) {
            ShortcutInfo shortcut3 = shortcutMap.get(shortcutChar);
            shortcut = shortcut3;
        }
        if (shortcut != null) {
            return shortcut.intent;
        }
        return null;
    }

    private void loadShortcuts() {
        Intent intent;
        String title;
        ActivityInfo info;
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            XmlResourceParser parser = this.mContext.getResources().getXml(18284548);
            XmlUtils.beginDocument(parser, TAG_BOOKMARKS);
            while (true) {
                XmlUtils.nextElement(parser);
                if (parser.getEventType() == 1 || !TAG_BOOKMARK.equals(parser.getName())) {
                    break;
                }
                String packageName = parser.getAttributeValue(null, "package");
                String className = parser.getAttributeValue(null, ATTRIBUTE_CLASS);
                String shortcutName = parser.getAttributeValue(null, ATTRIBUTE_SHORTCUT);
                String categoryName = parser.getAttributeValue(null, ATTRIBUTE_CATEGORY);
                String shiftName = parser.getAttributeValue(null, ATTRIBUTE_SHIFT);
                if (TextUtils.isEmpty(shortcutName)) {
                    Log.w("WindowManager", "Unable to get shortcut for: " + packageName + SliceClientPermissions.SliceAuthority.DELIMITER + className);
                } else {
                    int shortcutChar = shortcutName.charAt(0);
                    boolean isShiftShortcut = shiftName != null && shiftName.equals("true");
                    if (packageName != null && className != null) {
                        ComponentName componentName = new ComponentName(packageName, className);
                        try {
                            info = packageManager.getActivityInfo(componentName, 794624);
                        } catch (PackageManager.NameNotFoundException e) {
                            String[] packages = packageManager.canonicalToCurrentPackageNames(new String[]{packageName});
                            componentName = new ComponentName(packages[0], className);
                            try {
                                info = packageManager.getActivityInfo(componentName, 794624);
                            } catch (PackageManager.NameNotFoundException e2) {
                                Log.w("WindowManager", "Unable to add bookmark: " + packageName + SliceClientPermissions.SliceAuthority.DELIMITER + className + " not found.");
                            }
                        }
                        intent = new Intent("android.intent.action.MAIN");
                        intent.addCategory("android.intent.category.LAUNCHER");
                        intent.setComponent(componentName);
                        title = info.loadLabel(packageManager).toString();
                    } else if (categoryName != null) {
                        intent = Intent.makeMainSelectorActivity("android.intent.action.MAIN", categoryName);
                        title = "";
                    } else {
                        Log.w("WindowManager", "Unable to add bookmark for shortcut " + shortcutName + ": missing package/class or category attributes");
                    }
                    ShortcutInfo shortcut = new ShortcutInfo(title, intent);
                    if (isShiftShortcut) {
                        this.mShiftShortcuts.put(shortcutChar, shortcut);
                    } else {
                        this.mIntentShortcuts.put(shortcutChar, shortcut);
                    }
                }
            }
        } catch (IOException | XmlPullParserException e3) {
            Log.e("WindowManager", "Got exception parsing bookmarks.", e3);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerShortcutKey(long shortcutCode, IShortcutService shortcutService) throws RemoteException {
        IShortcutService service = this.mShortcutKeyServices.get(shortcutCode);
        if (service != null && service.asBinder().pingBinder()) {
            throw new RemoteException("Key already exists.");
        }
        this.mShortcutKeyServices.put(shortcutCode, shortcutService);
    }

    private boolean handleShortcutService(int keyCode, int metaState) {
        long shortcutCode = keyCode;
        if ((metaState & 4096) != 0) {
            shortcutCode |= 17592186044416L;
        }
        if ((metaState & 2) != 0) {
            shortcutCode |= 8589934592L;
        }
        if ((metaState & 1) != 0) {
            shortcutCode |= 4294967296L;
        }
        if ((65536 & metaState) != 0) {
            shortcutCode |= 281474976710656L;
        }
        IShortcutService shortcutService = this.mShortcutKeyServices.get(shortcutCode);
        if (shortcutService != null) {
            try {
                shortcutService.notifyShortcutKeyPressed(shortcutCode);
                return true;
            } catch (RemoteException e) {
                this.mShortcutKeyServices.delete(shortcutCode);
                return true;
            }
        }
        return false;
    }

    private boolean handleIntentShortcut(KeyCharacterMap kcm, int keyCode, int metaState) {
        if (this.mSearchKeyShortcutPending) {
            if (!kcm.isPrintingKey(keyCode)) {
                return false;
            }
            this.mConsumeSearchKeyUp = true;
            this.mSearchKeyShortcutPending = false;
        } else if ((458752 & metaState) != 0) {
            metaState &= -458753;
        } else {
            String category = sApplicationLaunchKeyCategories.get(keyCode);
            if (category != null) {
                Intent intent = Intent.makeMainSelectorActivity("android.intent.action.MAIN", category);
                intent.setFlags(268435456);
                try {
                    this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                } catch (ActivityNotFoundException e) {
                    Slog.w("WindowManager", "Dropping application launch key because the activity to which it is registered was not found: keyCode=" + KeyEvent.keyCodeToString(keyCode) + ", category=" + category);
                }
                return true;
            }
            return false;
        }
        Intent shortcutIntent = getIntent(kcm, keyCode, metaState);
        if (shortcutIntent != null) {
            shortcutIntent.addFlags(268435456);
            try {
                this.mContext.startActivityAsUser(shortcutIntent, UserHandle.CURRENT);
            } catch (ActivityNotFoundException e2) {
                Slog.w("WindowManager", "Dropping shortcut key combination because the activity to which it is registered was not found: META+ or SEARCH" + KeyEvent.keyCodeToString(keyCode));
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean interceptKey(KeyEvent event) {
        if (event.getRepeatCount() != 0) {
            return false;
        }
        int metaState = event.getModifiers();
        int keyCode = event.getKeyCode();
        if (keyCode == 84) {
            if (event.getAction() == 0) {
                this.mSearchKeyShortcutPending = true;
                this.mConsumeSearchKeyUp = false;
            } else {
                this.mSearchKeyShortcutPending = false;
                if (this.mConsumeSearchKeyUp) {
                    this.mConsumeSearchKeyUp = false;
                    return true;
                }
            }
            return false;
        } else if (event.getAction() != 0) {
            return false;
        } else {
            KeyCharacterMap kcm = event.getKeyCharacterMap();
            return handleIntentShortcut(kcm, keyCode, metaState) || handleShortcutService(keyCode, metaState);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class ShortcutInfo {
        public final Intent intent;
        public final String title;

        ShortcutInfo(String title, Intent intent) {
            this.title = title;
            this.intent = intent;
        }
    }
}
