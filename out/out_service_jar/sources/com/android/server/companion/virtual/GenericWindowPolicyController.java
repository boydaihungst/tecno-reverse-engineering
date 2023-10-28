package com.android.server.companion.virtual;

import android.app.compat.CompatChanges;
import android.companion.virtual.VirtualDeviceManager;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Slog;
import android.window.DisplayWindowPolicyController;
import com.android.internal.app.BlockedAppStreamingActivity;
import com.android.server.pm.PackageManagerService;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
/* loaded from: classes.dex */
public class GenericWindowPolicyController extends DisplayWindowPolicyController {
    public static final long ALLOW_SECURE_ACTIVITY_DISPLAY_ON_REMOTE_DEVICE = 201712607;
    private static final ComponentName BLOCKED_APP_STREAMING_COMPONENT = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, BlockedAppStreamingActivity.class.getName());
    private static final String TAG = "GenericWindowPolicyController";
    private final ActivityBlockedCallback mActivityBlockedCallback;
    private final VirtualDeviceManager.ActivityListener mActivityListener;
    private final ArraySet<ComponentName> mAllowedActivities;
    private final ArraySet<ComponentName> mAllowedCrossTaskNavigations;
    private final ArraySet<UserHandle> mAllowedUsers;
    private final ArraySet<ComponentName> mBlockedActivities;
    private final ArraySet<ComponentName> mBlockedCrossTaskNavigations;
    private final int mDefaultActivityPolicy;
    private final String mDeviceProfile;
    private final Object mGenericWindowPolicyControllerLock = new Object();
    private int mDisplayId = -1;
    final ArraySet<Integer> mRunningUids = new ArraySet<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ArraySet<RunningAppsChangedListener> mRunningAppsChangedListener = new ArraySet<>();

    /* loaded from: classes.dex */
    public interface ActivityBlockedCallback {
        void onActivityBlocked(int i, ActivityInfo activityInfo);
    }

    /* loaded from: classes.dex */
    public interface RunningAppsChangedListener {
        void onRunningAppsChanged(ArraySet<Integer> arraySet);
    }

    public GenericWindowPolicyController(int windowFlags, int systemWindowFlags, ArraySet<UserHandle> allowedUsers, Set<ComponentName> allowedCrossTaskNavigations, Set<ComponentName> blockedCrossTaskNavigations, Set<ComponentName> allowedActivities, Set<ComponentName> blockedActivities, int defaultActivityPolicy, VirtualDeviceManager.ActivityListener activityListener, ActivityBlockedCallback activityBlockedCallback, String deviceProfile) {
        this.mAllowedUsers = allowedUsers;
        this.mAllowedCrossTaskNavigations = new ArraySet<>(allowedCrossTaskNavigations);
        this.mBlockedCrossTaskNavigations = new ArraySet<>(blockedCrossTaskNavigations);
        this.mAllowedActivities = new ArraySet<>(allowedActivities);
        this.mBlockedActivities = new ArraySet<>(blockedActivities);
        this.mDefaultActivityPolicy = defaultActivityPolicy;
        this.mActivityBlockedCallback = activityBlockedCallback;
        setInterestedWindowFlags(windowFlags, systemWindowFlags);
        this.mActivityListener = activityListener;
        this.mDeviceProfile = deviceProfile;
    }

    public void setDisplayId(int displayId) {
        this.mDisplayId = displayId;
    }

    public void registerRunningAppsChangedListener(RunningAppsChangedListener listener) {
        this.mRunningAppsChangedListener.add(listener);
    }

    public void unregisterRunningAppsChangedListener(RunningAppsChangedListener listener) {
        this.mRunningAppsChangedListener.remove(listener);
    }

    public boolean canContainActivities(List<ActivityInfo> activities, int windowingMode) {
        if (isWindowingModeSupported(windowingMode)) {
            int activityCount = activities.size();
            for (int i = 0; i < activityCount; i++) {
                ActivityInfo aInfo = activities.get(i);
                if (!canContainActivity(aInfo, 0, 0)) {
                    this.mActivityBlockedCallback.onActivityBlocked(this.mDisplayId, aInfo);
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean canActivityBeLaunched(ActivityInfo activityInfo, int windowingMode, int launchingFromDisplayId, boolean isNewTask) {
        if (isWindowingModeSupported(windowingMode)) {
            ComponentName activityComponent = activityInfo.getComponentName();
            if (BLOCKED_APP_STREAMING_COMPONENT.equals(activityComponent)) {
                return true;
            }
            if (!canContainActivity(activityInfo, 0, 0)) {
                this.mActivityBlockedCallback.onActivityBlocked(this.mDisplayId, activityInfo);
                return false;
            } else if (launchingFromDisplayId == 0) {
                return true;
            } else {
                if (isNewTask && !this.mBlockedCrossTaskNavigations.isEmpty() && this.mBlockedCrossTaskNavigations.contains(activityComponent)) {
                    Slog.d(TAG, "Virtual device blocking cross task navigation of " + activityComponent);
                    this.mActivityBlockedCallback.onActivityBlocked(this.mDisplayId, activityInfo);
                    return false;
                } else if (!isNewTask || this.mAllowedCrossTaskNavigations.isEmpty() || this.mAllowedCrossTaskNavigations.contains(activityComponent)) {
                    return true;
                } else {
                    Slog.d(TAG, "Virtual device not allowing cross task navigation of " + activityComponent);
                    this.mActivityBlockedCallback.onActivityBlocked(this.mDisplayId, activityInfo);
                    return false;
                }
            }
        }
        return false;
    }

    public boolean keepActivityOnWindowFlagsChanged(ActivityInfo activityInfo, int windowFlags, int systemWindowFlags) {
        if (!canContainActivity(activityInfo, windowFlags, systemWindowFlags)) {
            this.mActivityBlockedCallback.onActivityBlocked(this.mDisplayId, activityInfo);
            return false;
        }
        return true;
    }

    public void onTopActivityChanged(final ComponentName topActivity, int uid) {
        if (this.mActivityListener != null && topActivity != null) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.companion.virtual.GenericWindowPolicyController$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    GenericWindowPolicyController.this.m2715xb647473e(topActivity);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onTopActivityChanged$0$com-android-server-companion-virtual-GenericWindowPolicyController  reason: not valid java name */
    public /* synthetic */ void m2715xb647473e(ComponentName topActivity) {
        this.mActivityListener.onTopActivityChanged(this.mDisplayId, topActivity);
    }

    public void onRunningAppsChanged(final ArraySet<Integer> runningUids) {
        synchronized (this.mGenericWindowPolicyControllerLock) {
            this.mRunningUids.clear();
            this.mRunningUids.addAll((ArraySet<? extends Integer>) runningUids);
            if (this.mActivityListener != null && this.mRunningUids.isEmpty()) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.companion.virtual.GenericWindowPolicyController$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        GenericWindowPolicyController.this.m2713x3923396a();
                    }
                });
            }
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.companion.virtual.GenericWindowPolicyController$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                GenericWindowPolicyController.this.m2714xd59135c9(runningUids);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onRunningAppsChanged$1$com-android-server-companion-virtual-GenericWindowPolicyController  reason: not valid java name */
    public /* synthetic */ void m2713x3923396a() {
        this.mActivityListener.onDisplayEmpty(this.mDisplayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onRunningAppsChanged$2$com-android-server-companion-virtual-GenericWindowPolicyController  reason: not valid java name */
    public /* synthetic */ void m2714xd59135c9(ArraySet runningUids) {
        Iterator<RunningAppsChangedListener> it = this.mRunningAppsChangedListener.iterator();
        while (it.hasNext()) {
            RunningAppsChangedListener listener = it.next();
            listener.onRunningAppsChanged(runningUids);
        }
    }

    public boolean canShowTasksInRecents() {
        String str = this.mDeviceProfile;
        if (str == null) {
            return true;
        }
        char c = 65535;
        switch (str.hashCode()) {
            case -1006843014:
                if (str.equals("android.app.role.COMPANION_DEVICE_APP_STREAMING")) {
                    c = 1;
                    break;
                }
                break;
            case 199080309:
                if (str.equals("android.app.role.SYSTEM_AUTOMOTIVE_PROJECTION")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return false;
            default:
                return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean containsUid(int uid) {
        boolean contains;
        synchronized (this.mGenericWindowPolicyControllerLock) {
            contains = this.mRunningUids.contains(Integer.valueOf(uid));
        }
        return contains;
    }

    private boolean canContainActivity(ActivityInfo activityInfo, int windowFlags, int systemWindowFlags) {
        if ((activityInfo.flags & 65536) == 0) {
            return false;
        }
        ComponentName activityComponent = activityInfo.getComponentName();
        if (BLOCKED_APP_STREAMING_COMPONENT.equals(activityComponent)) {
            return true;
        }
        UserHandle activityUser = UserHandle.getUserHandleForUid(activityInfo.applicationInfo.uid);
        if (!this.mAllowedUsers.contains(activityUser)) {
            Slog.d(TAG, "Virtual device activity not allowed from user " + activityUser);
            return false;
        } else if (this.mDefaultActivityPolicy == 0 && this.mBlockedActivities.contains(activityComponent)) {
            Slog.d(TAG, "Virtual device blocking launch of " + activityComponent);
            return false;
        } else if (this.mDefaultActivityPolicy != 1 || this.mAllowedActivities.contains(activityComponent)) {
            return CompatChanges.isChangeEnabled((long) ALLOW_SECURE_ACTIVITY_DISPLAY_ON_REMOTE_DEVICE, activityInfo.packageName, activityUser) || ((windowFlags & 8192) == 0 && (524288 & systemWindowFlags) == 0);
        } else {
            Slog.d(TAG, activityComponent + " is not in the allowed list.");
            return false;
        }
    }
}
