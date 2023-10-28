package com.transsion.griffin.lib.app;

import android.content.ComponentName;
import android.os.UserHandle;
/* loaded from: classes2.dex */
public class WidgetInfo {
    public static final int WIDGET_TYPE_ADD = 0;
    public static final int WIDGET_TYPE_REMOVE = 2;
    public static final int WIDGET_TYPE_UPDATE = 1;
    public int appWidgetId;
    public WidgetHost host;
    public long lastUpdateTime;
    public WidgetProvider provider;

    public WidgetInfo(int appWidgetId) {
        this.appWidgetId = -1;
        this.appWidgetId = appWidgetId;
    }

    /* loaded from: classes2.dex */
    public class WidgetProvider {
        ComponentName componentName;
        int uid;

        public WidgetProvider(int uid, ComponentName componentName) {
            this.uid = uid;
            this.componentName = componentName;
        }

        public int getUserId() {
            return UserHandle.getUserId(this.uid);
        }

        public int getAppId() {
            return UserHandle.getAppId(this.uid);
        }

        public ComponentName getComponentName() {
            return this.componentName;
        }

        public String getPkgName() {
            ComponentName componentName = this.componentName;
            if (componentName != null) {
                return componentName.getPackageName();
            }
            return null;
        }

        public String getClassName() {
            ComponentName componentName = this.componentName;
            if (componentName != null) {
                return componentName.getClassName();
            }
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public class WidgetHost {
        int hostId;
        String pkgName;
        int uid;

        public WidgetHost(int uid, int hostId, String pkgName) {
            this.uid = uid;
            this.hostId = hostId;
            this.pkgName = pkgName;
        }

        public int getUserId() {
            return UserHandle.getUserId(this.uid);
        }

        public int getAppId() {
            return UserHandle.getAppId(this.uid);
        }

        public int getHostId() {
            return this.hostId;
        }

        public String getHostPackageName() {
            return this.pkgName;
        }
    }
}
