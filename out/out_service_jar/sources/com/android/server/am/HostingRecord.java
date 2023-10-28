package com.android.server.am;

import android.content.ComponentName;
/* loaded from: classes.dex */
public final class HostingRecord {
    private static final int APP_ZYGOTE = 2;
    public static final String HOSTING_TYPE_ACTIVITY = "activity";
    public static final String HOSTING_TYPE_ADDED_APPLICATION = "added application";
    public static final String HOSTING_TYPE_BACKUP = "backup";
    public static final String HOSTING_TYPE_BROADCAST = "broadcast";
    public static final String HOSTING_TYPE_CONTENT_PROVIDER = "content provider";
    public static final String HOSTING_TYPE_EMPTY = "";
    public static final String HOSTING_TYPE_LINK_FAIL = "link fail";
    public static final String HOSTING_TYPE_NEXT_ACTIVITY = "next-activity";
    public static final String HOSTING_TYPE_NEXT_TOP_ACTIVITY = "next-top-activity";
    public static final String HOSTING_TYPE_ON_HOLD = "on-hold";
    public static final String HOSTING_TYPE_RESTART = "restart";
    public static final String HOSTING_TYPE_SERVICE = "service";
    public static final String HOSTING_TYPE_SYSTEM = "system";
    public static final String HOSTING_TYPE_TOP_ACTIVITY = "top-activity";
    private static final int REGULAR_ZYGOTE = 0;
    private static final int WEBVIEW_ZYGOTE = 1;
    private final String mAction;
    private final String mDefiningPackageName;
    private final String mDefiningProcessName;
    private final int mDefiningUid;
    private final String mHostingName;
    private final String mHostingType;
    private final int mHostingZygote;
    private final boolean mIsTopApp;

    public HostingRecord(String hostingType) {
        this(hostingType, null, 0, null, -1, false, null, null);
    }

    public HostingRecord(String hostingType, ComponentName hostingName) {
        this(hostingType, hostingName, 0);
    }

    public HostingRecord(String hostingType, ComponentName hostingName, String action) {
        this(hostingType, hostingName.toShortString(), 0, null, -1, false, null, action);
    }

    public HostingRecord(String hostingType, ComponentName hostingName, String definingPackageName, int definingUid, String definingProcessName) {
        this(hostingType, hostingName.toShortString(), 0, definingPackageName, definingUid, false, definingProcessName, null);
    }

    public HostingRecord(String hostingType, ComponentName hostingName, boolean isTopApp) {
        this(hostingType, hostingName.toShortString(), 0, null, -1, isTopApp, null, null);
    }

    public HostingRecord(String hostingType, String hostingName) {
        this(hostingType, hostingName, 0);
    }

    private HostingRecord(String hostingType, ComponentName hostingName, int hostingZygote) {
        this(hostingType, hostingName.toShortString(), hostingZygote);
    }

    private HostingRecord(String hostingType, String hostingName, int hostingZygote) {
        this(hostingType, hostingName, hostingZygote, null, -1, false, null, null);
    }

    private HostingRecord(String hostingType, String hostingName, int hostingZygote, String definingPackageName, int definingUid, boolean isTopApp, String definingProcessName, String action) {
        this.mHostingType = hostingType;
        this.mHostingName = hostingName;
        this.mHostingZygote = hostingZygote;
        this.mDefiningPackageName = definingPackageName;
        this.mDefiningUid = definingUid;
        this.mIsTopApp = isTopApp;
        this.mDefiningProcessName = definingProcessName;
        this.mAction = action;
    }

    public String getType() {
        return this.mHostingType;
    }

    public String getName() {
        return this.mHostingName;
    }

    public boolean isTopApp() {
        return this.mIsTopApp;
    }

    public int getDefiningUid() {
        return this.mDefiningUid;
    }

    public String getDefiningPackageName() {
        return this.mDefiningPackageName;
    }

    public String getDefiningProcessName() {
        return this.mDefiningProcessName;
    }

    public String getAction() {
        return this.mAction;
    }

    public static HostingRecord byWebviewZygote(ComponentName hostingName, String definingPackageName, int definingUid, String definingProcessName) {
        return new HostingRecord("", hostingName.toShortString(), 1, definingPackageName, definingUid, false, definingProcessName, null);
    }

    public static HostingRecord byAppZygote(ComponentName hostingName, String definingPackageName, int definingUid, String definingProcessName) {
        return new HostingRecord("", hostingName.toShortString(), 2, definingPackageName, definingUid, false, definingProcessName, null);
    }

    public boolean usesAppZygote() {
        return this.mHostingZygote == 2;
    }

    public boolean usesWebviewZygote() {
        return this.mHostingZygote == 1;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static int getHostingTypeIdStatsd(String hostingType) {
        char c;
        switch (hostingType.hashCode()) {
            case -1726126969:
                if (hostingType.equals(HOSTING_TYPE_TOP_ACTIVITY)) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case -1682898044:
                if (hostingType.equals(HOSTING_TYPE_LINK_FAIL)) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -1655966961:
                if (hostingType.equals(HOSTING_TYPE_ACTIVITY)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -1618876223:
                if (hostingType.equals("broadcast")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -1526161119:
                if (hostingType.equals(HOSTING_TYPE_NEXT_TOP_ACTIVITY)) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -1396673086:
                if (hostingType.equals(HOSTING_TYPE_BACKUP)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1372333075:
                if (hostingType.equals(HOSTING_TYPE_ON_HOLD)) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -1355707223:
                if (hostingType.equals(HOSTING_TYPE_NEXT_ACTIVITY)) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -887328209:
                if (hostingType.equals(HOSTING_TYPE_SYSTEM)) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 0:
                if (hostingType.equals("")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case 1097506319:
                if (hostingType.equals(HOSTING_TYPE_RESTART)) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 1418439096:
                if (hostingType.equals(HOSTING_TYPE_CONTENT_PROVIDER)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 1637159472:
                if (hostingType.equals(HOSTING_TYPE_ADDED_APPLICATION)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1984153269:
                if (hostingType.equals(HOSTING_TYPE_SERVICE)) {
                    c = '\n';
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
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            case 5:
                return 6;
            case 6:
                return 7;
            case 7:
                return 8;
            case '\b':
                return 9;
            case '\t':
                return 10;
            case '\n':
                return 11;
            case 11:
                return 12;
            case '\f':
                return 13;
            case '\r':
                return 14;
            default:
                return 0;
        }
    }
}
