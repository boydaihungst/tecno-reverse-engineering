package com.android.server.wm;

import android.app.AppGlobals;
import android.app.compat.CompatChanges;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.app.GameManagerService;
import com.android.server.job.controllers.JobStatus;
import com.mediatek.server.wm.WmsExt;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public final class CompatModePackages {
    private static final int COMPAT_FLAG_DONT_ASK = 1;
    private static final int COMPAT_FLAG_ENABLED = 2;
    public static final long DOWNSCALED = 168419799;
    public static final long DOWNSCALE_30 = 189970040;
    public static final long DOWNSCALE_35 = 189969749;
    public static final long DOWNSCALE_40 = 189970038;
    public static final long DOWNSCALE_45 = 189969782;
    public static final long DOWNSCALE_50 = 176926741;
    public static final long DOWNSCALE_55 = 189970036;
    public static final long DOWNSCALE_60 = 176926771;
    public static final long DOWNSCALE_65 = 189969744;
    public static final long DOWNSCALE_70 = 176926829;
    public static final long DOWNSCALE_75 = 189969779;
    public static final long DOWNSCALE_80 = 176926753;
    public static final long DOWNSCALE_85 = 189969734;
    public static final long DOWNSCALE_90 = 182811243;
    private static final long DO_NOT_DOWNSCALE_TO_1080P_ON_TV = 157629738;
    private static final int MSG_WRITE = 300;
    private static final String TAG = "ActivityTaskManager";
    private static final String TAG_CONFIGURATION = TAG + ActivityTaskManagerDebugConfig.POSTFIX_CONFIGURATION;
    private final AtomicFile mFile;
    private final CompatHandler mHandler;
    private final HashMap<String, Integer> mPackages = new HashMap<>();
    private final ActivityTaskManagerService mService;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class CompatHandler extends Handler {
        public CompatHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 300:
                    CompatModePackages.this.saveCompatModes();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [304=5, 306=5] */
    /* JADX WARN: Removed duplicated region for block: B:30:0x009b A[Catch: IOException -> 0x009f, TRY_ENTER, TRY_LEAVE, TryCatch #4 {IOException -> 0x009f, blocks: (B:30:0x009b, B:40:0x00ab, B:45:0x00b6), top: B:58:0x002d }] */
    /* JADX WARN: Removed duplicated region for block: B:67:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public CompatModePackages(ActivityTaskManagerService service, File systemDir, Handler handler) {
        TypedXmlPullParser parser;
        int eventType;
        String pkg;
        this.mService = service;
        AtomicFile atomicFile = new AtomicFile(new File(systemDir, "packages-compat.xml"), "compat-mode");
        this.mFile = atomicFile;
        this.mHandler = new CompatHandler(handler.getLooper());
        FileInputStream fis = null;
        try {
            try {
                try {
                    try {
                        fis = atomicFile.openRead();
                        parser = Xml.resolvePullParser(fis);
                        eventType = parser.getEventType();
                        while (eventType != 2 && eventType != 1) {
                            eventType = parser.next();
                        }
                    } catch (IOException e) {
                        if (fis != null) {
                            Slog.w(TAG, "Error reading compat-packages", e);
                        }
                        if (fis == null) {
                            return;
                        }
                        fis.close();
                    }
                } catch (XmlPullParserException e2) {
                    Slog.w(TAG, "Error reading compat-packages", e2);
                    if (fis == null) {
                        return;
                    }
                    fis.close();
                }
                if (eventType == 1) {
                    if (fis != null) {
                        try {
                            fis.close();
                            return;
                        } catch (IOException e3) {
                            return;
                        }
                    }
                    return;
                }
                String tagName = parser.getName();
                if ("compat-packages".equals(tagName)) {
                    int eventType2 = parser.next();
                    do {
                        if (eventType2 == 2) {
                            String tagName2 = parser.getName();
                            if (parser.getDepth() == 2 && "pkg".equals(tagName2) && (pkg = parser.getAttributeValue((String) null, "name")) != null) {
                                int modeInt = parser.getAttributeInt((String) null, GameManagerService.GamePackageConfiguration.GameModeConfiguration.MODE_KEY, 0);
                                this.mPackages.put(pkg, Integer.valueOf(modeInt));
                            }
                        }
                        eventType2 = parser.next();
                    } while (eventType2 != 1);
                    if (fis == null) {
                        fis.close();
                    }
                } else if (fis == null) {
                }
            } catch (Throwable th) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (IOException e5) {
        }
    }

    public HashMap<String, Integer> getPackages() {
        return this.mPackages;
    }

    private int getPackageFlags(String packageName) {
        Integer flags = this.mPackages.get(packageName);
        if (flags != null) {
            return flags.intValue();
        }
        return 0;
    }

    public void handlePackageDataClearedLocked(String packageName) {
        removePackage(packageName);
    }

    public void handlePackageUninstalledLocked(String packageName) {
        removePackage(packageName);
    }

    private void removePackage(String packageName) {
        if (this.mPackages.containsKey(packageName)) {
            this.mPackages.remove(packageName);
            scheduleWrite();
        }
    }

    public void handlePackageAddedLocked(String packageName, boolean updated) {
        ApplicationInfo ai = null;
        boolean mayCompat = false;
        try {
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0L, 0);
        } catch (RemoteException e) {
        }
        if (ai == null) {
            return;
        }
        CompatibilityInfo ci = compatibilityInfoForPackageLocked(ai);
        if (!ci.alwaysSupportsScreen() && !ci.neverSupportsScreen()) {
            mayCompat = true;
        }
        if (updated && !mayCompat && this.mPackages.containsKey(packageName)) {
            this.mPackages.remove(packageName);
            scheduleWrite();
        }
    }

    private void scheduleWrite() {
        this.mHandler.removeMessages(300);
        Message msg = this.mHandler.obtainMessage(300);
        this.mHandler.sendMessageDelayed(msg, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    public CompatibilityInfo compatibilityInfoForPackageLocked(ApplicationInfo ai) {
        boolean forceCompat = getPackageCompatModeEnabledLocked(ai);
        float compatScale = getCompatScale(ai.packageName, ai.uid);
        Configuration config = this.mService.getGlobalConfiguration();
        return new CompatibilityInfo(ai, config.screenLayout, config.smallestScreenWidthDp, forceCompat, compatScale);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getCompatScale(String packageName, int uid) {
        UserHandle userHandle = UserHandle.getUserHandleForUid(uid);
        if (CompatChanges.isChangeEnabled((long) DOWNSCALED, packageName, userHandle)) {
            WmsExt mWmsExt = this.mService.mWindowManager.getWmsExt();
            if (mWmsExt.isAppResolutionTunerAISupport()) {
                float scaleForAI = mWmsExt.getWindowScaleForAI(packageName);
                if (scaleForAI != 0.0f && scaleForAI != 1.0f) {
                    return scaleForAI;
                }
            }
            if (CompatChanges.isChangeEnabled((long) DOWNSCALE_90, packageName, userHandle)) {
                return 1.1111112f;
            }
            if (CompatChanges.isChangeEnabled((long) DOWNSCALE_85, packageName, userHandle)) {
                return 1.1764705f;
            }
            if (CompatChanges.isChangeEnabled((long) DOWNSCALE_80, packageName, userHandle)) {
                return 1.25f;
            }
            if (CompatChanges.isChangeEnabled((long) DOWNSCALE_75, packageName, userHandle)) {
                return 1.3333334f;
            }
            if (CompatChanges.isChangeEnabled((long) DOWNSCALE_70, packageName, userHandle)) {
                return 1.4285715f;
            }
            if (CompatChanges.isChangeEnabled((long) DOWNSCALE_65, packageName, userHandle)) {
                return 1.5384616f;
            }
            if (CompatChanges.isChangeEnabled((long) DOWNSCALE_60, packageName, userHandle)) {
                return 1.6666666f;
            }
            if (CompatChanges.isChangeEnabled((long) DOWNSCALE_55, packageName, userHandle)) {
                return 1.8181818f;
            }
            if (CompatChanges.isChangeEnabled((long) DOWNSCALE_50, packageName, userHandle)) {
                return 2.0f;
            }
            if (CompatChanges.isChangeEnabled((long) DOWNSCALE_45, packageName, userHandle)) {
                return 2.2222223f;
            }
            if (CompatChanges.isChangeEnabled((long) DOWNSCALE_40, packageName, userHandle)) {
                return 2.5f;
            }
            if (CompatChanges.isChangeEnabled((long) DOWNSCALE_35, packageName, userHandle)) {
                return 2.857143f;
            }
            if (CompatChanges.isChangeEnabled((long) DOWNSCALE_30, packageName, userHandle)) {
                return 3.3333333f;
            }
        }
        if (this.mService.mHasLeanbackFeature) {
            Configuration config = this.mService.getGlobalConfiguration();
            float density = config.densityDpi / 160.0f;
            int smallestScreenWidthPx = (int) ((config.smallestScreenWidthDp * density) + 0.5f);
            if (smallestScreenWidthPx > 1080 && !CompatChanges.isChangeEnabled((long) DO_NOT_DOWNSCALE_TO_1080P_ON_TV, packageName, userHandle)) {
                return smallestScreenWidthPx / 1080.0f;
            }
        }
        return 1.0f;
    }

    public int computeCompatModeLocked(ApplicationInfo ai) {
        CompatibilityInfo info = compatibilityInfoForPackageLocked(ai);
        if (info.alwaysSupportsScreen()) {
            return -2;
        }
        if (info.neverSupportsScreen()) {
            return -1;
        }
        return getPackageCompatModeEnabledLocked(ai) ? 1 : 0;
    }

    public boolean getPackageAskCompatModeLocked(String packageName) {
        return (getPackageFlags(packageName) & 1) == 0;
    }

    public void setPackageAskCompatModeLocked(String packageName, boolean ask) {
        setPackageFlagLocked(packageName, 1, ask);
    }

    private boolean getPackageCompatModeEnabledLocked(ApplicationInfo ai) {
        return (getPackageFlags(ai.packageName) & 2) != 0;
    }

    private void setPackageFlagLocked(String packageName, int flag, boolean set) {
        int curFlags = getPackageFlags(packageName);
        int newFlags = set ? (~flag) & curFlags : curFlags | flag;
        if (curFlags != newFlags) {
            if (newFlags != 0) {
                this.mPackages.put(packageName, Integer.valueOf(newFlags));
            } else {
                this.mPackages.remove(packageName);
            }
            scheduleWrite();
        }
    }

    public int getPackageScreenCompatModeLocked(String packageName) {
        ApplicationInfo ai = null;
        try {
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0L, 0);
        } catch (RemoteException e) {
        }
        if (ai == null) {
            return -3;
        }
        return computeCompatModeLocked(ai);
    }

    public void setPackageScreenCompatModeLocked(String packageName, int mode) {
        ApplicationInfo ai = null;
        try {
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0L, 0);
        } catch (RemoteException e) {
        }
        if (ai == null) {
            Slog.w(TAG, "setPackageScreenCompatMode failed: unknown package " + packageName);
        } else {
            setPackageScreenCompatModeLocked(ai, mode);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPackageScreenCompatModeLocked(ApplicationInfo ai, int mode) {
        boolean enable;
        int newFlags;
        boolean z;
        String packageName = ai.packageName;
        int curFlags = getPackageFlags(packageName);
        switch (mode) {
            case 0:
                enable = false;
                break;
            case 1:
                enable = true;
                break;
            case 2:
                if ((curFlags & 2) != 0) {
                    enable = false;
                    break;
                } else {
                    enable = true;
                    break;
                }
            default:
                Slog.w(TAG, "Unknown screen compat mode req #" + mode + "; ignoring");
                return;
        }
        int i = 2;
        if (enable) {
            newFlags = curFlags | 2;
        } else {
            newFlags = curFlags & (-3);
        }
        CompatibilityInfo ci = compatibilityInfoForPackageLocked(ai);
        if (ci.alwaysSupportsScreen()) {
            Slog.w(TAG, "Ignoring compat mode change of " + packageName + "; compatibility never needed");
            newFlags = 0;
        }
        if (ci.neverSupportsScreen()) {
            Slog.w(TAG, "Ignoring compat mode change of " + packageName + "; compatibility always needed");
            newFlags = 0;
        }
        if (newFlags != curFlags) {
            if (newFlags != 0) {
                this.mPackages.put(packageName, Integer.valueOf(newFlags));
            } else {
                this.mPackages.remove(packageName);
            }
            CompatibilityInfo ci2 = compatibilityInfoForPackageLocked(ai);
            scheduleWrite();
            Task rootTask = this.mService.getTopDisplayFocusedRootTask();
            ActivityRecord starting = rootTask.restartPackage(packageName);
            SparseArray<WindowProcessController> pidMap = this.mService.mProcessMap.getPidMap();
            int i2 = pidMap.size() - 1;
            while (i2 >= 0) {
                WindowProcessController app = pidMap.valueAt(i2);
                if (!app.mPkgList.contains(packageName)) {
                    z = true;
                } else {
                    try {
                        if (!app.hasThread()) {
                            z = true;
                        } else {
                            if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                                String protoLogParam0 = String.valueOf(app.mName);
                                String protoLogParam1 = String.valueOf(ci2);
                                ProtoLogGroup protoLogGroup = ProtoLogGroup.WM_DEBUG_CONFIGURATION;
                                Object[] objArr = new Object[i];
                                objArr[0] = protoLogParam0;
                                z = true;
                                try {
                                    objArr[1] = protoLogParam1;
                                    ProtoLogImpl.v(protoLogGroup, 1337596507, 0, (String) null, objArr);
                                } catch (Exception e) {
                                }
                            } else {
                                z = true;
                            }
                            app.getThread().updatePackageCompatibilityInfo(packageName, ci2);
                        }
                    } catch (Exception e2) {
                        z = true;
                    }
                }
                i2--;
                i = 2;
            }
            if (starting != null) {
                starting.ensureActivityConfiguration(0, false);
                rootTask.ensureActivitiesVisible(starting, 0, false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveCompatModes() {
        HashMap<String, Integer> pkgs;
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                pkgs = new HashMap<>(this.mPackages);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        FileOutputStream fos = null;
        try {
            fos = this.mFile.startWrite();
            TypedXmlSerializer out = Xml.resolveSerializer(fos);
            out.startDocument((String) null, true);
            out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            out.startTag((String) null, "compat-packages");
            IPackageManager pm = AppGlobals.getPackageManager();
            for (Map.Entry<String, Integer> entry : pkgs.entrySet()) {
                String pkg = entry.getKey();
                int mode = entry.getValue().intValue();
                if (mode != 0) {
                    ApplicationInfo ai = null;
                    try {
                        ai = pm.getApplicationInfo(pkg, 0L, 0);
                    } catch (RemoteException e) {
                    }
                    if (ai != null) {
                        CompatibilityInfo info = compatibilityInfoForPackageLocked(ai);
                        if (!info.alwaysSupportsScreen() && !info.neverSupportsScreen()) {
                            out.startTag((String) null, "pkg");
                            out.attribute((String) null, "name", pkg);
                            out.attributeInt((String) null, GameManagerService.GamePackageConfiguration.GameModeConfiguration.MODE_KEY, mode);
                            out.endTag((String) null, "pkg");
                        }
                    }
                }
            }
            out.endTag((String) null, "compat-packages");
            out.endDocument();
            this.mFile.finishWrite(fos);
        } catch (IOException e1) {
            Slog.w(TAG, "Error writing compat packages", e1);
            if (fos != null) {
                this.mFile.failWrite(fos);
            }
        }
    }
}
