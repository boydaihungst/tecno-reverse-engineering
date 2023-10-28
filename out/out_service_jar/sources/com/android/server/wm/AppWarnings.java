package com.android.server.wm;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AtomicFile;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.server.job.controllers.JobStatus;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class AppWarnings {
    private static final String CONFIG_FILE_NAME = "packages-warnings.xml";
    public static final int FLAG_HIDE_COMPILE_SDK = 2;
    public static final int FLAG_HIDE_DEPRECATED_SDK = 4;
    public static final int FLAG_HIDE_DISPLAY_SIZE = 1;
    private static final String TAG = "AppWarnings";
    private final ActivityTaskManagerService mAtm;
    private final AtomicFile mConfigFile;
    private DeprecatedTargetSdkVersionDialog mDeprecatedTargetSdkVersionDialog;
    private final ConfigHandler mHandler;
    private final Context mUiContext;
    private final UiHandler mUiHandler;
    private UnsupportedCompileSdkDialog mUnsupportedCompileSdkDialog;
    private UnsupportedDisplaySizeDialog mUnsupportedDisplaySizeDialog;
    private final HashMap<String, Integer> mPackageFlags = new HashMap<>();
    private HashSet<ComponentName> mAlwaysShowUnsupportedCompileSdkWarningActivities = new HashSet<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public void alwaysShowUnsupportedCompileSdkWarning(ComponentName activity) {
        this.mAlwaysShowUnsupportedCompileSdkWarningActivities.add(activity);
    }

    public AppWarnings(ActivityTaskManagerService atm, Context uiContext, Handler handler, Handler uiHandler, File systemDir) {
        this.mAtm = atm;
        this.mUiContext = uiContext;
        this.mHandler = new ConfigHandler(handler.getLooper());
        this.mUiHandler = new UiHandler(uiHandler.getLooper());
        this.mConfigFile = new AtomicFile(new File(systemDir, CONFIG_FILE_NAME), "warnings-config");
        readConfigFromFileAmsThread();
    }

    public void showUnsupportedDisplaySizeDialogIfNeeded(ActivityRecord r) {
        Configuration globalConfig = this.mAtm.getGlobalConfiguration();
        if (globalConfig.densityDpi != DisplayMetrics.DENSITY_DEVICE_STABLE && r.info.applicationInfo.requiresSmallestWidthDp > globalConfig.smallestScreenWidthDp) {
            this.mUiHandler.showUnsupportedDisplaySizeDialog(r);
        }
    }

    public void showUnsupportedCompileSdkDialogIfNeeded(ActivityRecord r) {
        if (r.info.applicationInfo.compileSdkVersion == 0 || r.info.applicationInfo.compileSdkVersionCodename == null || !this.mAlwaysShowUnsupportedCompileSdkWarningActivities.contains(r.mActivityComponent)) {
            return;
        }
        int compileSdk = r.info.applicationInfo.compileSdkVersion;
        int platformSdk = Build.VERSION.SDK_INT;
        boolean isCompileSdkPreview = !"REL".equals(r.info.applicationInfo.compileSdkVersionCodename);
        boolean isPlatformSdkPreview = !"REL".equals(Build.VERSION.CODENAME);
        if ((isCompileSdkPreview && compileSdk < platformSdk) || ((isPlatformSdkPreview && platformSdk < compileSdk) || (isCompileSdkPreview && isPlatformSdkPreview && platformSdk == compileSdk && !Build.VERSION.CODENAME.equals(r.info.applicationInfo.compileSdkVersionCodename)))) {
            this.mUiHandler.showUnsupportedCompileSdkDialog(r);
        }
    }

    public void showDeprecatedTargetDialogIfNeeded(ActivityRecord r) {
        if (r.info.applicationInfo.targetSdkVersion < Build.VERSION.MIN_SUPPORTED_TARGET_SDK_INT) {
            this.mUiHandler.showDeprecatedTargetDialog(r);
        }
    }

    public void onStartActivity(ActivityRecord r) {
        showUnsupportedCompileSdkDialogIfNeeded(r);
        showUnsupportedDisplaySizeDialogIfNeeded(r);
        showDeprecatedTargetDialogIfNeeded(r);
    }

    public void onResumeActivity(ActivityRecord r) {
        showUnsupportedDisplaySizeDialogIfNeeded(r);
    }

    public void onPackageDataCleared(String name) {
        removePackageAndHideDialogs(name);
    }

    public void onPackageUninstalled(String name) {
        removePackageAndHideDialogs(name);
    }

    public void onDensityChanged() {
        this.mUiHandler.hideUnsupportedDisplaySizeDialog();
    }

    private void removePackageAndHideDialogs(String name) {
        this.mUiHandler.hideDialogsForPackage(name);
        synchronized (this.mPackageFlags) {
            this.mPackageFlags.remove(name);
            this.mHandler.scheduleWrite();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideUnsupportedDisplaySizeDialogUiThread() {
        UnsupportedDisplaySizeDialog unsupportedDisplaySizeDialog = this.mUnsupportedDisplaySizeDialog;
        if (unsupportedDisplaySizeDialog != null) {
            unsupportedDisplaySizeDialog.dismiss();
            this.mUnsupportedDisplaySizeDialog = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showUnsupportedDisplaySizeDialogUiThread(ActivityRecord ar) {
        UnsupportedDisplaySizeDialog unsupportedDisplaySizeDialog = this.mUnsupportedDisplaySizeDialog;
        if (unsupportedDisplaySizeDialog != null) {
            unsupportedDisplaySizeDialog.dismiss();
            this.mUnsupportedDisplaySizeDialog = null;
        }
        if (ar != null && !hasPackageFlag(ar.packageName, 1)) {
            UnsupportedDisplaySizeDialog unsupportedDisplaySizeDialog2 = new UnsupportedDisplaySizeDialog(this, this.mUiContext, ar.info.applicationInfo);
            this.mUnsupportedDisplaySizeDialog = unsupportedDisplaySizeDialog2;
            unsupportedDisplaySizeDialog2.show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showUnsupportedCompileSdkDialogUiThread(ActivityRecord ar) {
        UnsupportedCompileSdkDialog unsupportedCompileSdkDialog = this.mUnsupportedCompileSdkDialog;
        if (unsupportedCompileSdkDialog != null) {
            unsupportedCompileSdkDialog.dismiss();
            this.mUnsupportedCompileSdkDialog = null;
        }
        if (ar != null && !hasPackageFlag(ar.packageName, 2)) {
            UnsupportedCompileSdkDialog unsupportedCompileSdkDialog2 = new UnsupportedCompileSdkDialog(this, this.mUiContext, ar.info.applicationInfo);
            this.mUnsupportedCompileSdkDialog = unsupportedCompileSdkDialog2;
            unsupportedCompileSdkDialog2.show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showDeprecatedTargetSdkDialogUiThread(ActivityRecord ar) {
        DeprecatedTargetSdkVersionDialog deprecatedTargetSdkVersionDialog = this.mDeprecatedTargetSdkVersionDialog;
        if (deprecatedTargetSdkVersionDialog != null) {
            deprecatedTargetSdkVersionDialog.dismiss();
            this.mDeprecatedTargetSdkVersionDialog = null;
        }
        if (ar != null && !hasPackageFlag(ar.packageName, 4)) {
            DeprecatedTargetSdkVersionDialog deprecatedTargetSdkVersionDialog2 = new DeprecatedTargetSdkVersionDialog(this, this.mUiContext, ar.info.applicationInfo);
            this.mDeprecatedTargetSdkVersionDialog = deprecatedTargetSdkVersionDialog2;
            deprecatedTargetSdkVersionDialog2.show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideDialogsForPackageUiThread(String name) {
        UnsupportedDisplaySizeDialog unsupportedDisplaySizeDialog = this.mUnsupportedDisplaySizeDialog;
        if (unsupportedDisplaySizeDialog != null && (name == null || name.equals(unsupportedDisplaySizeDialog.getPackageName()))) {
            this.mUnsupportedDisplaySizeDialog.dismiss();
            this.mUnsupportedDisplaySizeDialog = null;
        }
        UnsupportedCompileSdkDialog unsupportedCompileSdkDialog = this.mUnsupportedCompileSdkDialog;
        if (unsupportedCompileSdkDialog != null && (name == null || name.equals(unsupportedCompileSdkDialog.getPackageName()))) {
            this.mUnsupportedCompileSdkDialog.dismiss();
            this.mUnsupportedCompileSdkDialog = null;
        }
        DeprecatedTargetSdkVersionDialog deprecatedTargetSdkVersionDialog = this.mDeprecatedTargetSdkVersionDialog;
        if (deprecatedTargetSdkVersionDialog != null) {
            if (name == null || name.equals(deprecatedTargetSdkVersionDialog.getPackageName())) {
                this.mDeprecatedTargetSdkVersionDialog.dismiss();
                this.mDeprecatedTargetSdkVersionDialog = null;
            }
        }
    }

    boolean hasPackageFlag(String name, int flag) {
        return (getPackageFlags(name) & flag) == flag;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPackageFlag(String name, int flag, boolean enabled) {
        synchronized (this.mPackageFlags) {
            int curFlags = getPackageFlags(name);
            int newFlags = enabled ? curFlags | flag : (~flag) & curFlags;
            if (curFlags != newFlags) {
                if (newFlags != 0) {
                    this.mPackageFlags.put(name, Integer.valueOf(newFlags));
                } else {
                    this.mPackageFlags.remove(name);
                }
                this.mHandler.scheduleWrite();
            }
        }
    }

    private int getPackageFlags(String name) {
        int intValue;
        synchronized (this.mPackageFlags) {
            intValue = this.mPackageFlags.getOrDefault(name, 0).intValue();
        }
        return intValue;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class UiHandler extends Handler {
        private static final int MSG_HIDE_DIALOGS_FOR_PACKAGE = 4;
        private static final int MSG_HIDE_UNSUPPORTED_DISPLAY_SIZE_DIALOG = 2;
        private static final int MSG_SHOW_DEPRECATED_TARGET_SDK_DIALOG = 5;
        private static final int MSG_SHOW_UNSUPPORTED_COMPILE_SDK_DIALOG = 3;
        private static final int MSG_SHOW_UNSUPPORTED_DISPLAY_SIZE_DIALOG = 1;

        public UiHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ActivityRecord ar = (ActivityRecord) msg.obj;
                    AppWarnings.this.showUnsupportedDisplaySizeDialogUiThread(ar);
                    return;
                case 2:
                    AppWarnings.this.hideUnsupportedDisplaySizeDialogUiThread();
                    return;
                case 3:
                    ActivityRecord ar2 = (ActivityRecord) msg.obj;
                    AppWarnings.this.showUnsupportedCompileSdkDialogUiThread(ar2);
                    return;
                case 4:
                    String name = (String) msg.obj;
                    AppWarnings.this.hideDialogsForPackageUiThread(name);
                    return;
                case 5:
                    ActivityRecord ar3 = (ActivityRecord) msg.obj;
                    AppWarnings.this.showDeprecatedTargetSdkDialogUiThread(ar3);
                    return;
                default:
                    return;
            }
        }

        public void showUnsupportedDisplaySizeDialog(ActivityRecord r) {
            removeMessages(1);
            obtainMessage(1, r).sendToTarget();
        }

        public void hideUnsupportedDisplaySizeDialog() {
            removeMessages(2);
            sendEmptyMessage(2);
        }

        public void showUnsupportedCompileSdkDialog(ActivityRecord r) {
            removeMessages(3);
            obtainMessage(3, r).sendToTarget();
        }

        public void showDeprecatedTargetDialog(ActivityRecord r) {
            removeMessages(5);
            obtainMessage(5, r).sendToTarget();
        }

        public void hideDialogsForPackage(String name) {
            obtainMessage(4, name).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ConfigHandler extends Handler {
        private static final int DELAY_MSG_WRITE = 10000;
        private static final int MSG_WRITE = 1;

        public ConfigHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AppWarnings.this.writeConfigToFileAmsThread();
                    return;
                default:
                    return;
            }
        }

        public void scheduleWrite() {
            removeMessages(1);
            sendEmptyMessageDelayed(1, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeConfigToFileAmsThread() {
        HashMap<String, Integer> packageFlags;
        synchronized (this.mPackageFlags) {
            packageFlags = new HashMap<>(this.mPackageFlags);
        }
        FileOutputStream fos = null;
        try {
            fos = this.mConfigFile.startWrite();
            TypedXmlSerializer out = Xml.resolveSerializer(fos);
            out.startDocument((String) null, true);
            out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            out.startTag((String) null, "packages");
            for (Map.Entry<String, Integer> entry : packageFlags.entrySet()) {
                String pkg = entry.getKey();
                int mode = entry.getValue().intValue();
                if (mode != 0) {
                    out.startTag((String) null, "package");
                    out.attribute((String) null, "name", pkg);
                    out.attributeInt((String) null, "flags", mode);
                    out.endTag((String) null, "package");
                }
            }
            out.endTag((String) null, "packages");
            out.endDocument();
            this.mConfigFile.finishWrite(fos);
        } catch (IOException e1) {
            Slog.w(TAG, "Error writing package metadata", e1);
            if (fos != null) {
                this.mConfigFile.failWrite(fos);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [554=5, 556=5] */
    /* JADX WARN: Removed duplicated region for block: B:30:0x0075 A[Catch: IOException -> 0x0079, TRY_ENTER, TRY_LEAVE, TryCatch #0 {IOException -> 0x0079, blocks: (B:30:0x0075, B:40:0x0085, B:45:0x0090, B:3:0x0005, B:7:0x001a, B:15:0x002a, B:17:0x0037, B:19:0x003e, B:21:0x0049, B:23:0x0052, B:25:0x005c, B:26:0x006c), top: B:53:0x0005, inners: #1, #4 }] */
    /* JADX WARN: Removed duplicated region for block: B:47:0x0094 A[ORIG_RETURN, RETURN] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void readConfigFromFileAmsThread() {
        String name;
        FileInputStream fis = null;
        try {
            try {
                try {
                    try {
                        fis = this.mConfigFile.openRead();
                        TypedXmlPullParser parser = Xml.resolvePullParser(fis);
                        int eventType = parser.getEventType();
                        while (eventType != 2 && eventType != 1) {
                            eventType = parser.next();
                        }
                        if (eventType == 1) {
                            if (fis != null) {
                                try {
                                    fis.close();
                                    return;
                                } catch (IOException e) {
                                    return;
                                }
                            }
                            return;
                        }
                        String tagName = parser.getName();
                        if ("packages".equals(tagName)) {
                            int eventType2 = parser.next();
                            do {
                                if (eventType2 == 2) {
                                    String tagName2 = parser.getName();
                                    if (parser.getDepth() == 2 && "package".equals(tagName2) && (name = parser.getAttributeValue((String) null, "name")) != null) {
                                        int flagsInt = parser.getAttributeInt((String) null, "flags", 0);
                                        this.mPackageFlags.put(name, Integer.valueOf(flagsInt));
                                    }
                                }
                                eventType2 = parser.next();
                            } while (eventType2 != 1);
                            if (fis == null) {
                                fis.close();
                            }
                        } else if (fis == null) {
                        }
                    } catch (XmlPullParserException e2) {
                        Slog.w(TAG, "Error reading package metadata", e2);
                        if (fis != null) {
                            fis.close();
                        }
                    }
                } catch (IOException e3) {
                    if (fis != null) {
                        Slog.w(TAG, "Error reading package metadata", e3);
                    }
                    if (fis != null) {
                        fis.close();
                    }
                }
            } catch (IOException e4) {
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    fis.close();
                } catch (IOException e5) {
                }
            }
            throw th;
        }
    }
}
