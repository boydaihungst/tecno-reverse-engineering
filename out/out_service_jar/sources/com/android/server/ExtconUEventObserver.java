package com.android.server;

import android.os.FileUtils;
import android.os.UEventObserver;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
public abstract class ExtconUEventObserver extends UEventObserver {
    private static final boolean LOG = false;
    private static final String SELINUX_POLICIES_NEED_TO_BE_CHANGED = "This probably means the selinux policies need to be changed.";
    private static final String TAG = "ExtconUEventObserver";
    private final Map<String, ExtconInfo> mExtconInfos = new ArrayMap();

    protected abstract void onUEvent(ExtconInfo extconInfo, UEventObserver.UEvent uEvent);

    public final void onUEvent(UEventObserver.UEvent event) {
        String devPath = event.get("DEVPATH");
        ExtconInfo info = this.mExtconInfos.get(devPath);
        if (info != null) {
            onUEvent(info, event);
        } else {
            Slog.w(TAG, "No match found for DEVPATH of " + event + " in " + this.mExtconInfos);
        }
    }

    public void startObserving(ExtconInfo extconInfo) {
        String devicePath = extconInfo.getDevicePath();
        if (devicePath == null) {
            Slog.wtf(TAG, "Unable to start observing  " + extconInfo.getName() + " because the device path is null. " + SELINUX_POLICIES_NEED_TO_BE_CHANGED);
            return;
        }
        this.mExtconInfos.put(devicePath, extconInfo);
        startObserving("DEVPATH=" + devicePath);
    }

    /* loaded from: classes.dex */
    public static final class ExtconInfo {
        public static final String EXTCON_CHARGE_DOWNSTREAM = "CHARGE-DOWNSTREAM";
        public static final String EXTCON_DOCK = "DOCK";
        public static final String EXTCON_DVI = "DVI";
        public static final String EXTCON_FAST_CHARGER = "FAST-CHARGER";
        public static final String EXTCON_HDMI = "HDMI";
        public static final String EXTCON_HEADPHONE = "HEADPHONE";
        public static final String EXTCON_JIG = "JIG";
        public static final String EXTCON_LINE_IN = "LINE-IN";
        public static final String EXTCON_LINE_OUT = "LINE-OUT";
        public static final String EXTCON_MECHANICAL = "MECHANICAL";
        public static final String EXTCON_MHL = "MHL";
        public static final String EXTCON_MICROPHONE = "MICROPHONE";
        public static final String EXTCON_SLOW_CHARGER = "SLOW-CHARGER";
        public static final String EXTCON_SPDIF_IN = "SPDIF-IN";
        public static final String EXTCON_SPDIF_OUT = "SPDIF-OUT";
        public static final String EXTCON_TA = "TA";
        public static final String EXTCON_USB = "USB";
        public static final String EXTCON_USB_HOST = "USB-HOST";
        public static final String EXTCON_VGA = "VGA";
        public static final String EXTCON_VIDEO_IN = "VIDEO-IN";
        public static final String EXTCON_VIDEO_OUT = "VIDEO-OUT";
        private final HashSet<String> mDeviceTypes = new HashSet<>();
        private final String mName;
        private static final Object sLock = new Object();
        private static ExtconInfo[] sExtconInfos = null;

        /* loaded from: classes.dex */
        public @interface ExtconDeviceType {
        }

        private static void initExtconInfos() {
            if (sExtconInfos != null) {
                return;
            }
            File file = new File("/sys/class/extcon");
            File[] files = file.listFiles();
            if (files == null) {
                Slog.w(ExtconUEventObserver.TAG, file + " exists " + file.exists() + " isDir " + file.isDirectory() + " but listFiles returns null." + ExtconUEventObserver.SELINUX_POLICIES_NEED_TO_BE_CHANGED);
                sExtconInfos = new ExtconInfo[0];
                return;
            }
            List<ExtconInfo> list = new ArrayList<>(files.length);
            for (File f : files) {
                list.add(new ExtconInfo(f.getName()));
            }
            sExtconInfos = (ExtconInfo[]) list.toArray(new ExtconInfo[0]);
        }

        public static List<ExtconInfo> getExtconInfoForTypes(String[] extconTypes) {
            ExtconInfo[] extconInfoArr;
            synchronized (sLock) {
                initExtconInfos();
            }
            List<ExtconInfo> extcons = new ArrayList<>();
            for (ExtconInfo extcon : sExtconInfos) {
                int length = extconTypes.length;
                int i = 0;
                while (true) {
                    if (i < length) {
                        String type = extconTypes[i];
                        if (!extcon.hasCableType(type)) {
                            i++;
                        } else {
                            extcons.add(extcon);
                            break;
                        }
                    }
                }
            }
            return extcons;
        }

        public boolean hasCableType(String type) {
            return this.mDeviceTypes.contains(type);
        }

        private ExtconInfo(String extconName) {
            this.mName = extconName;
            File[] cableDirs = FileUtils.listFilesOrEmpty(new File("/sys/class/extcon", extconName), new FilenameFilter() { // from class: com.android.server.ExtconUEventObserver$ExtconInfo$$ExternalSyntheticLambda0
                @Override // java.io.FilenameFilter
                public final boolean accept(File file, String str) {
                    boolean startsWith;
                    startsWith = str.startsWith("cable.");
                    return startsWith;
                }
            });
            if (cableDirs.length == 0) {
                Slog.d(ExtconUEventObserver.TAG, "Unable to list cables in /sys/class/extcon/" + extconName + ". " + ExtconUEventObserver.SELINUX_POLICIES_NEED_TO_BE_CHANGED);
            }
            for (File cableDir : cableDirs) {
                String cableCanonicalPath = null;
                try {
                    cableCanonicalPath = cableDir.getCanonicalPath();
                    String name = FileUtils.readTextFile(new File(cableDir, "name"), 0, null);
                    this.mDeviceTypes.add(name.replace("\n", "").replace("\r", ""));
                } catch (IOException ex) {
                    Slog.w(ExtconUEventObserver.TAG, "Unable to read " + cableCanonicalPath + "/name. " + ExtconUEventObserver.SELINUX_POLICIES_NEED_TO_BE_CHANGED, ex);
                }
            }
        }

        public String getName() {
            return this.mName;
        }

        public String getDevicePath() {
            try {
                String extconPath = TextUtils.formatSimple("/sys/class/extcon/%s", new Object[]{this.mName});
                File devPath = new File(extconPath);
                if (!devPath.exists()) {
                    return null;
                }
                String canonicalPath = devPath.getCanonicalPath();
                int start = canonicalPath.indexOf("/devices");
                return canonicalPath.substring(start);
            } catch (IOException e) {
                Slog.e(ExtconUEventObserver.TAG, "Could not get the extcon device path for " + this.mName, e);
                return null;
            }
        }

        public String getStatePath() {
            return TextUtils.formatSimple("/sys/class/extcon/%s/state", new Object[]{this.mName});
        }
    }

    public static boolean extconExists() {
        File extconDir = new File("/sys/class/extcon");
        return extconDir.exists() && extconDir.isDirectory();
    }
}
