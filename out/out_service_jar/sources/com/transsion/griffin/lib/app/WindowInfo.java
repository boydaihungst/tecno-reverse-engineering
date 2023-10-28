package com.transsion.griffin.lib.app;

import android.os.IBinder;
import android.view.DisplayInfo;
/* loaded from: classes2.dex */
public class WindowInfo {
    public int owningUid() {
        return -1;
    }

    public String owningPackage() {
        return "";
    }

    public int baseType() {
        return 0;
    }

    public int baseLayer() {
        return 0;
    }

    public boolean isImWindow() {
        return false;
    }

    public boolean isWallpaper() {
        return false;
    }

    public boolean isFloatingLayer() {
        return false;
    }

    public IBinder client() {
        return null;
    }

    public DisplayInfo displayInfo() {
        return null;
    }

    public int displayId() {
        return -1;
    }

    public int viewVisibility() {
        return 4;
    }

    public boolean isOnScreen() {
        return false;
    }

    public boolean isVisible() {
        return false;
    }
}
