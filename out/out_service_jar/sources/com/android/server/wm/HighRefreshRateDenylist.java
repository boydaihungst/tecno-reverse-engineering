package com.android.server.wm;

import android.content.res.Resources;
import android.provider.DeviceConfig;
import android.provider.DeviceConfigInterface;
import android.util.ArraySet;
import com.android.internal.os.BackgroundThread;
import java.io.PrintWriter;
import java.util.Iterator;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class HighRefreshRateDenylist {
    private final String[] mDefaultDenylist;
    private final ArraySet<String> mDenylistedPackages = new ArraySet<>();
    private final Object mLock = new Object();

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HighRefreshRateDenylist create(Resources r) {
        return new HighRefreshRateDenylist(r, DeviceConfigInterface.REAL);
    }

    HighRefreshRateDenylist(Resources r, DeviceConfigInterface deviceConfig) {
        this.mDefaultDenylist = r.getStringArray(17236078);
        deviceConfig.addOnPropertiesChangedListener("display_manager", BackgroundThread.getExecutor(), new OnPropertiesChangedListener());
        String property = deviceConfig.getProperty("display_manager", "high_refresh_rate_blacklist");
        updateDenylist(property);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDenylist(String property) {
        synchronized (this.mLock) {
            this.mDenylistedPackages.clear();
            int i = 0;
            if (property != null) {
                String[] packages = property.split(",");
                int length = packages.length;
                while (i < length) {
                    String pkg = packages[i];
                    String pkgName = pkg.trim();
                    if (!pkgName.isEmpty()) {
                        this.mDenylistedPackages.add(pkgName);
                    }
                    i++;
                }
            } else {
                String[] strArr = this.mDefaultDenylist;
                int length2 = strArr.length;
                while (i < length2) {
                    String pkg2 = strArr[i];
                    this.mDenylistedPackages.add(pkg2);
                    i++;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDenylisted(String packageName) {
        boolean contains;
        synchronized (this.mLock) {
            contains = this.mDenylistedPackages.contains(packageName);
        }
        return contains;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        pw.println("High Refresh Rate Denylist");
        pw.println("  Packages:");
        synchronized (this.mLock) {
            Iterator<String> it = this.mDenylistedPackages.iterator();
            while (it.hasNext()) {
                String pkg = it.next();
                pw.println("    " + pkg);
            }
        }
    }

    /* loaded from: classes2.dex */
    private class OnPropertiesChangedListener implements DeviceConfig.OnPropertiesChangedListener {
        private OnPropertiesChangedListener() {
        }

        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            if (properties.getKeyset().contains("high_refresh_rate_blacklist")) {
                HighRefreshRateDenylist.this.updateDenylist(properties.getString("high_refresh_rate_blacklist", (String) null));
            }
        }
    }
}
