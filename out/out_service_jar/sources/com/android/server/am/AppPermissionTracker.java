package com.android.server.am;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import com.android.server.am.BaseAppStateTracker;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AppPermissionTracker extends BaseAppStateTracker<AppPermissionPolicy> implements PackageManager.OnPermissionsChangedListener {
    static final boolean DEBUG_PERMISSION_TRACKER = false;
    static final String TAG = "ActivityManager";
    private final SparseArray<MyAppOpsCallback> mAppOpsCallbacks;
    private final MyHandler mHandler;
    private volatile boolean mLockedBootCompleted;
    private SparseArray<ArraySet<UidGrantedPermissionState>> mUidGrantedPermissionsInMonitor;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppPermissionTracker(Context context, AppRestrictionController controller) {
        this(context, controller, null, null);
    }

    AppPermissionTracker(Context context, AppRestrictionController controller, Constructor<? extends BaseAppStateTracker.Injector<AppPermissionPolicy>> injector, Object outerContext) {
        super(context, controller, injector, outerContext);
        this.mAppOpsCallbacks = new SparseArray<>();
        this.mUidGrantedPermissionsInMonitor = new SparseArray<>();
        this.mLockedBootCompleted = false;
        this.mHandler = new MyHandler(this);
        this.mInjector.setPolicy(new AppPermissionPolicy(this.mInjector, this));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public int getType() {
        return 5;
    }

    public void onPermissionsChanged(int uid) {
        this.mHandler.obtainMessage(2, uid, 0).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAppOpsInit() {
        ArrayList<Integer> ops = new ArrayList<>();
        Pair<String, Integer>[] permissions = ((AppPermissionPolicy) this.mInjector.getPolicy()).getBgPermissionsInMonitor();
        for (Pair<String, Integer> pair : permissions) {
            if (((Integer) pair.second).intValue() != -1) {
                ops.add((Integer) pair.second);
            }
        }
        int i = ops.size();
        startWatchingMode((Integer[]) ops.toArray(new Integer[i]));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:44:? -> B:35:0x0101). Please submit an issue!!! */
    public void handlePermissionsInit() {
        int i;
        Object obj;
        ApplicationInfo ai;
        UidGrantedPermissionState state;
        List<ApplicationInfo> apps;
        int size;
        int userId;
        int[] allUsers = this.mInjector.getUserManagerInternal().getUserIds();
        PackageManagerInternal pmi = this.mInjector.getPackageManagerInternal();
        this.mInjector.getPermissionManagerServiceInternal();
        Pair<String, Integer>[] permissions = ((AppPermissionPolicy) this.mInjector.getPolicy()).getBgPermissionsInMonitor();
        SparseArray<ArraySet<UidGrantedPermissionState>> uidPerms = this.mUidGrantedPermissionsInMonitor;
        int length = allUsers.length;
        int i2 = 0;
        while (i2 < length) {
            int userId2 = allUsers[i2];
            List<ApplicationInfo> apps2 = pmi.getInstalledApplications(0L, userId2, 1000);
            if (apps2 == null) {
                i = length;
            } else {
                long now = SystemClock.elapsedRealtime();
                int size2 = apps2.size();
                int i3 = 0;
                while (i3 < size2) {
                    ApplicationInfo ai2 = apps2.get(i3);
                    int length2 = permissions.length;
                    int i4 = 0;
                    while (i4 < length2) {
                        Pair<String, Integer> permission = permissions[i4];
                        int i5 = length;
                        int i6 = i4;
                        int i7 = length2;
                        int i8 = i3;
                        UidGrantedPermissionState state2 = new UidGrantedPermissionState(ai2.uid, (String) permission.first, ((Integer) permission.second).intValue());
                        if (!state2.isGranted()) {
                            ai = ai2;
                            apps = apps2;
                            size = size2;
                            userId = userId2;
                        } else {
                            Object obj2 = this.mLock;
                            synchronized (obj2) {
                                try {
                                    ArraySet<UidGrantedPermissionState> grantedPermissions = uidPerms.get(ai2.uid);
                                    if (grantedPermissions != null) {
                                        obj = obj2;
                                        ai = ai2;
                                        state = state2;
                                        apps = apps2;
                                        size = size2;
                                        userId = userId2;
                                    } else {
                                        try {
                                            grantedPermissions = new ArraySet<>();
                                            uidPerms.put(ai2.uid, grantedPermissions);
                                            obj = obj2;
                                            ai = ai2;
                                            state = state2;
                                            apps = apps2;
                                            size = size2;
                                            userId = userId2;
                                        } catch (Throwable th) {
                                            th = th;
                                            obj = obj2;
                                        }
                                        try {
                                            notifyListenersOnStateChange(ai2.uid, "", true, now, 16);
                                        } catch (Throwable th2) {
                                            th = th2;
                                            throw th;
                                        }
                                    }
                                    try {
                                        grantedPermissions.add(state);
                                    } catch (Throwable th3) {
                                        th = th3;
                                        throw th;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                    obj = obj2;
                                }
                            }
                        }
                        i4 = i6 + 1;
                        length = i5;
                        length2 = i7;
                        i3 = i8;
                        apps2 = apps;
                        size2 = size;
                        ai2 = ai;
                        userId2 = userId;
                    }
                    i3++;
                }
                i = length;
            }
            i2++;
            length = i;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAppOpsDestroy() {
        stopWatchingMode();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePermissionsDestroy() {
        synchronized (this.mLock) {
            SparseArray<ArraySet<UidGrantedPermissionState>> uidPerms = this.mUidGrantedPermissionsInMonitor;
            long now = SystemClock.elapsedRealtime();
            int size = uidPerms.size();
            for (int i = 0; i < size; i++) {
                int uid = uidPerms.keyAt(i);
                ArraySet<UidGrantedPermissionState> grantedPermissions = uidPerms.valueAt(i);
                if (grantedPermissions.size() > 0) {
                    notifyListenersOnStateChange(uid, "", false, now, 16);
                }
            }
            uidPerms.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOpChanged(int op, int uid, String packageName) {
        Pair<String, Integer>[] permissions = ((AppPermissionPolicy) this.mInjector.getPolicy()).getBgPermissionsInMonitor();
        if (permissions != null && permissions.length > 0) {
            for (Pair<String, Integer> pair : permissions) {
                if (((Integer) pair.second).intValue() == op) {
                    UidGrantedPermissionState state = new UidGrantedPermissionState(uid, (String) pair.first, op);
                    synchronized (this.mLock) {
                        handlePermissionsChangedLocked(uid, new UidGrantedPermissionState[]{state});
                    }
                    return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePermissionsChanged(int uid) {
        Pair<String, Integer>[] permissions = ((AppPermissionPolicy) this.mInjector.getPolicy()).getBgPermissionsInMonitor();
        if (permissions != null && permissions.length > 0) {
            this.mInjector.getPermissionManagerServiceInternal();
            UidGrantedPermissionState[] states = new UidGrantedPermissionState[permissions.length];
            for (int i = 0; i < permissions.length; i++) {
                Pair<String, Integer> pair = permissions[i];
                states[i] = new UidGrantedPermissionState(uid, (String) pair.first, ((Integer) pair.second).intValue());
            }
            synchronized (this.mLock) {
                handlePermissionsChangedLocked(uid, states);
            }
        }
    }

    private void handlePermissionsChangedLocked(int uid, UidGrantedPermissionState[] states) {
        ArraySet<UidGrantedPermissionState> grantedPermissions;
        boolean changed;
        int index = this.mUidGrantedPermissionsInMonitor.indexOfKey(uid);
        ArraySet<UidGrantedPermissionState> grantedPermissions2 = index >= 0 ? this.mUidGrantedPermissionsInMonitor.valueAt(index) : null;
        long now = SystemClock.elapsedRealtime();
        int i = 0;
        while (i < states.length) {
            boolean granted = states[i].isGranted();
            boolean changed2 = false;
            if (granted) {
                if (grantedPermissions2 == null) {
                    grantedPermissions2 = new ArraySet<>();
                    this.mUidGrantedPermissionsInMonitor.put(uid, grantedPermissions2);
                    changed2 = true;
                }
                grantedPermissions2.add(states[i]);
                grantedPermissions = grantedPermissions2;
                changed = changed2;
            } else if (grantedPermissions2 != null && !grantedPermissions2.isEmpty() && grantedPermissions2.remove(states[i]) && grantedPermissions2.isEmpty()) {
                this.mUidGrantedPermissionsInMonitor.removeAt(index);
                grantedPermissions = grantedPermissions2;
                changed = true;
            } else {
                grantedPermissions = grantedPermissions2;
                changed = false;
            }
            if (changed) {
                notifyListenersOnStateChange(uid, "", granted, now, 16);
            }
            i++;
            grantedPermissions2 = grantedPermissions;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class UidGrantedPermissionState {
        final int mAppOp;
        private boolean mAppOpAllowed;
        final String mPermission;
        private boolean mPermissionGranted;
        final int mUid;

        UidGrantedPermissionState(int uid, String permission, int appOp) {
            this.mUid = uid;
            this.mPermission = permission;
            this.mAppOp = appOp;
            updatePermissionState();
            updateAppOps();
        }

        void updatePermissionState() {
            if (TextUtils.isEmpty(this.mPermission)) {
                this.mPermissionGranted = true;
            } else {
                this.mPermissionGranted = AppPermissionTracker.this.mInjector.getPermissionManagerServiceInternal().checkUidPermission(this.mUid, this.mPermission) == 0;
            }
        }

        void updateAppOps() {
            int mode;
            if (this.mAppOp == -1) {
                this.mAppOpAllowed = true;
                return;
            }
            String[] packages = AppPermissionTracker.this.mInjector.getPackageManager().getPackagesForUid(this.mUid);
            if (packages != null) {
                IAppOpsService appOpsService = AppPermissionTracker.this.mInjector.getIAppOpsService();
                for (String pkg : packages) {
                    try {
                        mode = appOpsService.checkOperation(this.mAppOp, this.mUid, pkg);
                    } catch (RemoteException e) {
                    }
                    if (mode != 0) {
                        continue;
                    } else {
                        this.mAppOpAllowed = true;
                        return;
                    }
                }
            }
            this.mAppOpAllowed = false;
        }

        boolean isGranted() {
            return this.mPermissionGranted && this.mAppOpAllowed;
        }

        public boolean equals(Object other) {
            if (other == null || !(other instanceof UidGrantedPermissionState)) {
                return false;
            }
            UidGrantedPermissionState otherState = (UidGrantedPermissionState) other;
            return this.mUid == otherState.mUid && this.mAppOp == otherState.mAppOp && Objects.equals(this.mPermission, otherState.mPermission);
        }

        public int hashCode() {
            int hashCode = ((Integer.hashCode(this.mUid) * 31) + Integer.hashCode(this.mAppOp)) * 31;
            String str = this.mPermission;
            return hashCode + (str == null ? 0 : str.hashCode());
        }

        public String toString() {
            String s = "UidGrantedPermissionState{" + System.identityHashCode(this) + " " + UserHandle.formatUid(this.mUid) + ": ";
            boolean emptyPermissionName = TextUtils.isEmpty(this.mPermission);
            if (!emptyPermissionName) {
                s = s + this.mPermission + "=" + this.mPermissionGranted;
            }
            if (this.mAppOp != -1) {
                if (!emptyPermissionName) {
                    s = s + ",";
                }
                s = s + AppOpsManager.opToPublicName(this.mAppOp) + "=" + this.mAppOpAllowed;
            }
            return s + "}";
        }
    }

    private void startWatchingMode(Integer[] ops) {
        synchronized (this.mAppOpsCallbacks) {
            stopWatchingMode();
            IAppOpsService appOpsService = this.mInjector.getIAppOpsService();
            try {
                for (Integer num : ops) {
                    int op = num.intValue();
                    MyAppOpsCallback cb = new MyAppOpsCallback();
                    this.mAppOpsCallbacks.put(op, cb);
                    appOpsService.startWatchingModeWithFlags(op, (String) null, 1, cb);
                }
            } catch (RemoteException e) {
            }
        }
    }

    private void stopWatchingMode() {
        synchronized (this.mAppOpsCallbacks) {
            IAppOpsService appOpsService = this.mInjector.getIAppOpsService();
            for (int i = this.mAppOpsCallbacks.size() - 1; i >= 0; i--) {
                try {
                    appOpsService.stopWatchingMode(this.mAppOpsCallbacks.valueAt(i));
                } catch (RemoteException e) {
                }
            }
            this.mAppOpsCallbacks.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyAppOpsCallback extends IAppOpsCallback.Stub {
        private MyAppOpsCallback() {
        }

        public void opChanged(int op, int uid, String packageName) {
            AppPermissionTracker.this.mHandler.obtainMessage(3, op, uid, packageName).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class MyHandler extends Handler {
        static final int MSG_APPOPS_CHANGED = 3;
        static final int MSG_PERMISSIONS_CHANGED = 2;
        static final int MSG_PERMISSIONS_DESTROY = 1;
        static final int MSG_PERMISSIONS_INIT = 0;
        private AppPermissionTracker mTracker;

        MyHandler(AppPermissionTracker tracker) {
            super(tracker.mBgHandler.getLooper());
            this.mTracker = tracker;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    this.mTracker.handleAppOpsInit();
                    this.mTracker.handlePermissionsInit();
                    return;
                case 1:
                    this.mTracker.handlePermissionsDestroy();
                    this.mTracker.handleAppOpsDestroy();
                    return;
                case 2:
                    this.mTracker.handlePermissionsChanged(msg.arg1);
                    return;
                case 3:
                    this.mTracker.handleOpChanged(msg.arg1, msg.arg2, (String) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPermissionTrackerEnabled(boolean enabled) {
        if (!this.mLockedBootCompleted) {
            return;
        }
        PermissionManager pm = this.mInjector.getPermissionManager();
        if (enabled) {
            pm.addOnPermissionsChangeListener(this);
            this.mHandler.obtainMessage(0).sendToTarget();
            return;
        }
        pm.removeOnPermissionsChangeListener(this);
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onLockedBootCompleted() {
        this.mLockedBootCompleted = true;
        onPermissionTrackerEnabled(((AppPermissionPolicy) this.mInjector.getPolicy()).isEnabled());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:44:? -> B:37:0x010f). Please submit an issue!!! */
    @Override // com.android.server.am.BaseAppStateTracker
    public void dump(PrintWriter pw, String prefix) {
        int i;
        Pair[] permissions;
        Pair[] permissions2;
        AppPermissionTracker appPermissionTracker = this;
        pw.print(prefix);
        pw.println("APP PERMISSIONS TRACKER:");
        Pair[] permissions3 = ((AppPermissionPolicy) appPermissionTracker.mInjector.getPolicy()).getBgPermissionsInMonitor();
        String prefixMore = "  " + prefix;
        String prefixMoreMore = "  " + prefixMore;
        int length = permissions3.length;
        int i2 = 0;
        while (i2 < length) {
            Pair<String, Integer> permission = permissions3[i2];
            pw.print(prefixMore);
            boolean emptyPermissionName = TextUtils.isEmpty((CharSequence) permission.first);
            if (!emptyPermissionName) {
                pw.print((String) permission.first);
            }
            if (((Integer) permission.second).intValue() != -1) {
                if (!emptyPermissionName) {
                    pw.print('+');
                }
                pw.print(AppOpsManager.opToPublicName(((Integer) permission.second).intValue()));
            }
            pw.println(':');
            synchronized (appPermissionTracker.mLock) {
                try {
                    SparseArray<ArraySet<UidGrantedPermissionState>> uidPerms = appPermissionTracker.mUidGrantedPermissionsInMonitor;
                    pw.print(prefixMoreMore);
                    pw.print('[');
                    boolean needDelimiter = false;
                    int i3 = 0;
                    int size = uidPerms.size();
                    while (i3 < size) {
                        ArraySet<UidGrantedPermissionState> uidPerm = uidPerms.valueAt(i3);
                        int i4 = length;
                        int j = uidPerm.size() - 1;
                        while (true) {
                            if (j < 0) {
                                permissions2 = permissions3;
                                break;
                            }
                            UidGrantedPermissionState state = uidPerm.valueAt(j);
                            permissions2 = permissions3;
                            try {
                                if (state.mAppOp != ((Integer) permission.second).intValue() || !TextUtils.equals(state.mPermission, (CharSequence) permission.first)) {
                                    j--;
                                    permissions3 = permissions2;
                                } else {
                                    if (needDelimiter) {
                                        pw.print(',');
                                    }
                                    pw.print(UserHandle.formatUid(state.mUid));
                                    needDelimiter = true;
                                }
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                        i3++;
                        permissions3 = permissions2;
                        length = i4;
                    }
                    i = length;
                    permissions = permissions3;
                    pw.println(']');
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
            i2++;
            appPermissionTracker = this;
            permissions3 = permissions;
            length = i;
        }
        super.dump(pw, prefix);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AppPermissionPolicy extends BaseAppStatePolicy<AppPermissionTracker> {
        static final String[] DEFAULT_BG_PERMISSIONS_IN_MONITOR = {"android.permission.ACCESS_FINE_LOCATION", "android:fine_location", "android.permission.CAMERA", "android:camera", "android.permission.RECORD_AUDIO", "android:record_audio"};
        static final boolean DEFAULT_BG_PERMISSION_MONITOR_ENABLED = true;
        static final String KEY_BG_PERMISSIONS_IN_MONITOR = "bg_permission_in_monitor";
        static final String KEY_BG_PERMISSION_MONITOR_ENABLED = "bg_permission_monitor_enabled";
        volatile Pair[] mBgPermissionsInMonitor;

        AppPermissionPolicy(BaseAppStateTracker.Injector injector, AppPermissionTracker tracker) {
            super(injector, tracker, KEY_BG_PERMISSION_MONITOR_ENABLED, true);
            this.mBgPermissionsInMonitor = parsePermissionConfig(DEFAULT_BG_PERMISSIONS_IN_MONITOR);
        }

        @Override // com.android.server.am.BaseAppStatePolicy
        public void onSystemReady() {
            super.onSystemReady();
            updateBgPermissionsInMonitor();
        }

        @Override // com.android.server.am.BaseAppStatePolicy
        public void onPropertiesChanged(String name) {
            char c;
            switch (name.hashCode()) {
                case -1888141258:
                    if (name.equals(KEY_BG_PERMISSIONS_IN_MONITOR)) {
                        c = 0;
                        break;
                    }
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    updateBgPermissionsInMonitor();
                    return;
                default:
                    super.onPropertiesChanged(name);
                    return;
            }
        }

        Pair[] getBgPermissionsInMonitor() {
            return this.mBgPermissionsInMonitor;
        }

        private Pair[] parsePermissionConfig(String[] perms) {
            Pair[] result = new Pair[perms.length / 2];
            int i = 0;
            int j = 0;
            while (i < perms.length) {
                try {
                    result[j] = Pair.create(TextUtils.isEmpty(perms[i]) ? null : perms[i], Integer.valueOf(TextUtils.isEmpty(perms[i + 1]) ? -1 : AppOpsManager.strOpToOp(perms[i + 1])));
                } catch (Exception e) {
                }
                i += 2;
                j++;
            }
            return result;
        }

        private void updateBgPermissionsInMonitor() {
            String config = DeviceConfig.getString("activity_manager", KEY_BG_PERMISSIONS_IN_MONITOR, (String) null);
            Pair[] newPermsInMonitor = parsePermissionConfig(config != null ? config.split(",") : DEFAULT_BG_PERMISSIONS_IN_MONITOR);
            if (!Arrays.equals(this.mBgPermissionsInMonitor, newPermsInMonitor)) {
                this.mBgPermissionsInMonitor = newPermsInMonitor;
                if (isEnabled()) {
                    onTrackerEnabled(false);
                    onTrackerEnabled(true);
                }
            }
        }

        @Override // com.android.server.am.BaseAppStatePolicy
        public void onTrackerEnabled(boolean enabled) {
            ((AppPermissionTracker) this.mTracker).onPermissionTrackerEnabled(enabled);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.am.BaseAppStatePolicy
        public void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.println("APP PERMISSION TRACKER POLICY SETTINGS:");
            String prefix2 = "  " + prefix;
            super.dump(pw, prefix2);
            pw.print(prefix2);
            pw.print(KEY_BG_PERMISSIONS_IN_MONITOR);
            pw.print('=');
            pw.print('[');
            for (int i = 0; i < this.mBgPermissionsInMonitor.length; i++) {
                if (i > 0) {
                    pw.print(',');
                }
                Pair<String, Integer> pair = this.mBgPermissionsInMonitor[i];
                if (pair.first != null) {
                    pw.print((String) pair.first);
                }
                pw.print(',');
                if (((Integer) pair.second).intValue() != -1) {
                    pw.print(AppOpsManager.opToPublicName(((Integer) pair.second).intValue()));
                }
            }
            pw.println(']');
        }
    }
}
