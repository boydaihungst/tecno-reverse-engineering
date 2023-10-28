package com.android.server.trust;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.admin.DevicePolicyManager;
import android.app.trust.ITrustListener;
import android.app.trust.ITrustManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricSourceType;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.security.Authorization;
import android.service.trust.GrantTrustResult;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.Xml;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.R;
import com.android.internal.content.PackageMonitor;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.util.DumpUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.am.HostingRecord;
import com.android.server.companion.virtual.VirtualDeviceManagerInternal;
import com.android.server.trust.TrustManagerService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class TrustManagerService extends SystemService {
    static final boolean DEBUG;
    public static final boolean ENABLE_ACTIVE_UNLOCK_FLAG;
    private static final int MSG_CLEANUP_USER = 8;
    private static final int MSG_DISPATCH_UNLOCK_ATTEMPT = 3;
    private static final int MSG_DISPATCH_UNLOCK_LOCKOUT = 13;
    private static final int MSG_ENABLED_AGENTS_CHANGED = 4;
    private static final int MSG_FLUSH_TRUST_USUALLY_MANAGED = 10;
    private static final int MSG_KEYGUARD_SHOWING_CHANGED = 6;
    private static final int MSG_REFRESH_DEVICE_LOCKED_FOR_USER = 14;
    private static final int MSG_REFRESH_TRUSTABLE_TIMERS_AFTER_AUTH = 17;
    private static final int MSG_REGISTER_LISTENER = 1;
    private static final int MSG_SCHEDULE_TRUST_TIMEOUT = 15;
    private static final int MSG_START_USER = 7;
    private static final int MSG_STOP_USER = 12;
    private static final int MSG_SWITCH_USER = 9;
    private static final int MSG_UNLOCK_USER = 11;
    private static final int MSG_UNREGISTER_LISTENER = 2;
    private static final int MSG_USER_MAY_REQUEST_UNLOCK = 18;
    private static final int MSG_USER_REQUESTED_UNLOCK = 16;
    private static final String PERMISSION_PROVIDE_AGENT = "android.permission.PROVIDE_TRUST_AGENT";
    private static final String PRIV_NAMESPACE = "http://schemas.android.com/apk/prv/res/android";
    private static final String REFRESH_DEVICE_LOCKED_EXCEPT_USER = "except";
    private static final String TAG = "TrustManagerService";
    private static final long TRUSTABLE_IDLE_TIMEOUT_IN_MILLIS = 28800000;
    private static final long TRUSTABLE_TIMEOUT_IN_MILLIS = 86400000;
    private static final Intent TRUST_AGENT_INTENT;
    private static final String TRUST_TIMEOUT_ALARM_TAG = "TrustManagerService.trustTimeoutForUser";
    private static final long TRUST_TIMEOUT_IN_MILLIS = 14400000;
    private static final int TRUST_USUALLY_MANAGED_FLUSH_DELAY = 120000;
    private final ArraySet<AgentInfo> mActiveAgents;
    private final ActivityManager mActivityManager;
    private final Object mAlarmLock;
    private AlarmManager mAlarmManager;
    final TrustArchive mArchive;
    private final Context mContext;
    private int mCurrentUser;
    private final SparseBooleanArray mDeviceLockedForUser;
    private final Handler mHandler;
    private final SparseArray<TrustableTimeoutAlarmListener> mIdleTrustableTimeoutAlarmListenerForUser;
    private final LockPatternUtils mLockPatternUtils;
    private final PackageMonitor mPackageMonitor;
    private final Receiver mReceiver;
    private final IBinder mService;
    private final SettingsObserver mSettingsObserver;
    private final StrongAuthTracker mStrongAuthTracker;
    private boolean mTrustAgentsCanRun;
    private final ArrayList<ITrustListener> mTrustListeners;
    private final ArrayMap<Integer, TrustedTimeoutAlarmListener> mTrustTimeoutAlarmListenerForUser;
    private final SparseBooleanArray mTrustUsuallyManagedForUser;
    private final SparseArray<TrustableTimeoutAlarmListener> mTrustableTimeoutAlarmListenerForUser;
    private final SparseBooleanArray mUserIsTrusted;
    private final UserManager mUserManager;
    private final SparseArray<TrustState> mUserTrustState;
    private final SparseBooleanArray mUsersUnlockedByBiometric;
    private VirtualDeviceManagerInternal mVirtualDeviceManager;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public enum TimeoutType {
        TRUSTED,
        TRUSTABLE
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public enum TrustState {
        UNTRUSTED,
        TRUSTABLE,
        TRUSTED
    }

    static {
        DEBUG = Build.IS_DEBUGGABLE && Log.isLoggable(TAG, 2);
        TRUST_AGENT_INTENT = new Intent("android.service.trust.TrustAgentService");
        ENABLE_ACTIVE_UNLOCK_FLAG = SystemProperties.getBoolean("fw.enable_active_unlock_flag", true);
    }

    public TrustManagerService(Context context) {
        super(context);
        this.mActiveAgents = new ArraySet<>();
        this.mTrustListeners = new ArrayList<>();
        this.mReceiver = new Receiver();
        this.mArchive = new TrustArchive();
        this.mUserIsTrusted = new SparseBooleanArray();
        this.mUserTrustState = new SparseArray<>();
        this.mDeviceLockedForUser = new SparseBooleanArray();
        this.mTrustUsuallyManagedForUser = new SparseBooleanArray();
        this.mUsersUnlockedByBiometric = new SparseBooleanArray();
        this.mTrustTimeoutAlarmListenerForUser = new ArrayMap<>();
        this.mTrustableTimeoutAlarmListenerForUser = new SparseArray<>();
        this.mIdleTrustableTimeoutAlarmListenerForUser = new SparseArray<>();
        this.mAlarmLock = new Object();
        this.mTrustAgentsCanRun = false;
        this.mCurrentUser = 0;
        this.mService = new AnonymousClass1();
        Handler handler = new Handler() { // from class: com.android.server.trust.TrustManagerService.2
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                SparseBooleanArray usuallyManaged;
                switch (msg.what) {
                    case 1:
                        TrustManagerService.this.addListener((ITrustListener) msg.obj);
                        return;
                    case 2:
                        TrustManagerService.this.removeListener((ITrustListener) msg.obj);
                        return;
                    case 3:
                        TrustManagerService.this.dispatchUnlockAttempt(msg.arg1 != 0, msg.arg2);
                        return;
                    case 4:
                        TrustManagerService.this.refreshAgentList(-1);
                        TrustManagerService.this.refreshDeviceLockedForUser(-1);
                        return;
                    case 5:
                    default:
                        return;
                    case 6:
                        TrustManagerService.this.dispatchTrustableDowngrade();
                        TrustManagerService trustManagerService = TrustManagerService.this;
                        trustManagerService.refreshDeviceLockedForUser(trustManagerService.mCurrentUser);
                        return;
                    case 7:
                    case 8:
                    case 11:
                        TrustManagerService.this.refreshAgentList(msg.arg1);
                        return;
                    case 9:
                        TrustManagerService.this.mCurrentUser = msg.arg1;
                        TrustManagerService.this.mSettingsObserver.updateContentObserver();
                        TrustManagerService.this.refreshDeviceLockedForUser(-1);
                        return;
                    case 10:
                        synchronized (TrustManagerService.this.mTrustUsuallyManagedForUser) {
                            usuallyManaged = TrustManagerService.this.mTrustUsuallyManagedForUser.clone();
                        }
                        for (int i = 0; i < usuallyManaged.size(); i++) {
                            int userId = usuallyManaged.keyAt(i);
                            boolean value = usuallyManaged.valueAt(i);
                            if (value != TrustManagerService.this.mLockPatternUtils.isTrustUsuallyManaged(userId)) {
                                TrustManagerService.this.mLockPatternUtils.setTrustUsuallyManaged(value, userId);
                            }
                        }
                        return;
                    case 12:
                        TrustManagerService.this.setDeviceLockedForUser(msg.arg1, true);
                        return;
                    case 13:
                        TrustManagerService.this.dispatchUnlockLockout(msg.arg1, msg.arg2);
                        return;
                    case 14:
                        if (msg.arg2 == 1) {
                            TrustManagerService.this.updateTrust(msg.arg1, 0, true, null);
                        }
                        int unlockedUser = msg.getData().getInt(TrustManagerService.REFRESH_DEVICE_LOCKED_EXCEPT_USER, -10000);
                        TrustManagerService.this.refreshDeviceLockedForUser(msg.arg1, unlockedUser);
                        return;
                    case 15:
                        boolean shouldOverride = msg.arg1 == 1;
                        TimeoutType timeoutType = msg.arg2 == 1 ? TimeoutType.TRUSTABLE : TimeoutType.TRUSTED;
                        TrustManagerService.this.handleScheduleTrustTimeout(shouldOverride, timeoutType);
                        return;
                    case 16:
                        TrustManagerService.this.dispatchUserRequestedUnlock(msg.arg1, msg.arg2 != 0);
                        return;
                    case 17:
                        TrustManagerService.this.refreshTrustableTimers(msg.arg1);
                        return;
                    case 18:
                        TrustManagerService.this.dispatchUserMayRequestUnlock(msg.arg1);
                        return;
                }
            }
        };
        this.mHandler = handler;
        this.mPackageMonitor = new PackageMonitor() { // from class: com.android.server.trust.TrustManagerService.3
            public void onSomePackagesChanged() {
                TrustManagerService.this.refreshAgentList(-1);
            }

            public boolean onPackageChanged(String packageName, int uid, String[] components) {
                return true;
            }

            public void onPackageDisappeared(String packageName, int reason) {
                TrustManagerService.this.removeAgentsOfPackage(packageName);
            }
        };
        this.mContext = context;
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mActivityManager = (ActivityManager) context.getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mStrongAuthTracker = new StrongAuthTracker(context);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mSettingsObserver = new SettingsObserver(handler);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("trust", this.mService);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (isSafeMode()) {
            return;
        }
        if (phase == 500) {
            this.mPackageMonitor.register(this.mContext, this.mHandler.getLooper(), UserHandle.ALL, true);
            this.mReceiver.register(this.mContext);
            this.mLockPatternUtils.registerStrongAuthTracker(this.mStrongAuthTracker);
        } else if (phase == 600) {
            this.mTrustAgentsCanRun = true;
            refreshAgentList(-1);
            refreshDeviceLockedForUser(-1);
        } else if (phase == 1000) {
            maybeEnableFactoryTrustAgents(this.mLockPatternUtils, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class SettingsObserver extends ContentObserver {
        private final Uri LOCK_SCREEN_WHEN_TRUST_LOST;
        private final Uri TRUST_AGENTS_EXTEND_UNLOCK;
        private final ContentResolver mContentResolver;
        private final boolean mIsAutomotive;
        private boolean mLockWhenTrustLost;
        private boolean mTrustAgentsNonrenewableTrust;

        SettingsObserver(Handler handler) {
            super(handler);
            this.TRUST_AGENTS_EXTEND_UNLOCK = Settings.Secure.getUriFor("trust_agents_extend_unlock");
            this.LOCK_SCREEN_WHEN_TRUST_LOST = Settings.Secure.getUriFor("lock_screen_when_trust_lost");
            PackageManager packageManager = TrustManagerService.this.getContext().getPackageManager();
            this.mIsAutomotive = packageManager.hasSystemFeature("android.hardware.type.automotive");
            this.mContentResolver = TrustManagerService.this.getContext().getContentResolver();
            updateContentObserver();
        }

        void updateContentObserver() {
            this.mContentResolver.unregisterContentObserver(this);
            this.mContentResolver.registerContentObserver(this.TRUST_AGENTS_EXTEND_UNLOCK, false, this, TrustManagerService.this.mCurrentUser);
            this.mContentResolver.registerContentObserver(this.LOCK_SCREEN_WHEN_TRUST_LOST, false, this, TrustManagerService.this.mCurrentUser);
            onChange(true, this.TRUST_AGENTS_EXTEND_UNLOCK);
            onChange(true, this.LOCK_SCREEN_WHEN_TRUST_LOST);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (this.TRUST_AGENTS_EXTEND_UNLOCK.equals(uri)) {
                int defaultValue = !this.mIsAutomotive ? 1 : 0;
                this.mTrustAgentsNonrenewableTrust = Settings.Secure.getIntForUser(this.mContentResolver, "trust_agents_extend_unlock", defaultValue, TrustManagerService.this.mCurrentUser) != 0;
            } else if (this.LOCK_SCREEN_WHEN_TRUST_LOST.equals(uri)) {
                this.mLockWhenTrustLost = Settings.Secure.getIntForUser(this.mContentResolver, "lock_screen_when_trust_lost", 0, TrustManagerService.this.mCurrentUser) != 0;
            }
        }

        boolean getTrustAgentsNonrenewableTrust() {
            return this.mTrustAgentsNonrenewableTrust;
        }

        boolean getLockWhenTrustLost() {
            return this.mLockWhenTrustLost;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeLockScreen(int userId) {
        if (userId == this.mCurrentUser && this.mSettingsObserver.getLockWhenTrustLost()) {
            if (DEBUG) {
                Slog.d(TAG, "Locking device because trust was lost");
            }
            try {
                WindowManagerGlobal.getWindowManagerService().lockNow((Bundle) null);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error locking screen when trust was lost");
            }
            TrustedTimeoutAlarmListener alarm = this.mTrustTimeoutAlarmListenerForUser.get(Integer.valueOf(userId));
            if (alarm != null && this.mSettingsObserver.getTrustAgentsNonrenewableTrust()) {
                this.mAlarmManager.cancel(alarm);
                alarm.setQueued(false);
            }
        }
    }

    private void scheduleTrustTimeout(boolean override, boolean isTrustableTimeout) {
        this.mHandler.obtainMessage(15, override ? 1 : 0, isTrustableTimeout ? 1 : 0).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScheduleTrustTimeout(boolean shouldOverride, TimeoutType timeoutType) {
        int userId = this.mCurrentUser;
        if (timeoutType == TimeoutType.TRUSTABLE) {
            handleScheduleTrustableTimeouts(userId, shouldOverride, false);
        } else {
            handleScheduleTrustedTimeout(userId, shouldOverride);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshTrustableTimers(int userId) {
        handleScheduleTrustableTimeouts(userId, true, true);
    }

    private void handleScheduleTrustedTimeout(int userId, boolean shouldOverride) {
        long when = SystemClock.elapsedRealtime() + 14400000;
        TrustedTimeoutAlarmListener alarm = this.mTrustTimeoutAlarmListenerForUser.get(Integer.valueOf(userId));
        if (alarm != null) {
            if (!shouldOverride && alarm.isQueued()) {
                if (DEBUG) {
                    Slog.d(TAG, "Found existing trust timeout alarm. Skipping.");
                    return;
                }
                return;
            }
            this.mAlarmManager.cancel(alarm);
        } else {
            alarm = new TrustedTimeoutAlarmListener(userId);
            this.mTrustTimeoutAlarmListenerForUser.put(Integer.valueOf(userId), alarm);
        }
        if (DEBUG) {
            Slog.d(TAG, "\tSetting up trust timeout alarm");
        }
        alarm.setQueued(true);
        this.mAlarmManager.setExact(2, when, TRUST_TIMEOUT_ALARM_TAG, alarm, this.mHandler);
    }

    private void handleScheduleTrustableTimeouts(int userId, boolean overrideIdleTimeout, boolean overrideHardTimeout) {
        setUpIdleTimeout(userId, overrideIdleTimeout);
        setUpHardTimeout(userId, overrideHardTimeout);
    }

    private void setUpIdleTimeout(int userId, boolean overrideIdleTimeout) {
        long when = SystemClock.elapsedRealtime() + TRUSTABLE_IDLE_TIMEOUT_IN_MILLIS;
        TrustableTimeoutAlarmListener alarm = this.mIdleTrustableTimeoutAlarmListenerForUser.get(userId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.SCHEDULE_EXACT_ALARM", null);
        if (alarm != null) {
            if (!overrideIdleTimeout && alarm.isQueued()) {
                if (DEBUG) {
                    Slog.d(TAG, "Found existing trustable timeout alarm. Skipping.");
                    return;
                }
                return;
            }
            this.mAlarmManager.cancel(alarm);
        } else {
            alarm = new TrustableTimeoutAlarmListener(userId);
            this.mIdleTrustableTimeoutAlarmListenerForUser.put(userId, alarm);
        }
        if (DEBUG) {
            Slog.d(TAG, "\tSetting up trustable idle timeout alarm");
        }
        alarm.setQueued(true);
        this.mAlarmManager.setExact(2, when, TRUST_TIMEOUT_ALARM_TAG, alarm, this.mHandler);
    }

    private void setUpHardTimeout(int userId, boolean overrideHardTimeout) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SCHEDULE_EXACT_ALARM", null);
        TrustableTimeoutAlarmListener alarm = this.mTrustableTimeoutAlarmListenerForUser.get(userId);
        if (alarm == null || !alarm.isQueued() || overrideHardTimeout) {
            long when = SystemClock.elapsedRealtime() + 86400000;
            if (alarm == null) {
                alarm = new TrustableTimeoutAlarmListener(userId);
                this.mTrustableTimeoutAlarmListenerForUser.put(userId, alarm);
            } else if (overrideHardTimeout) {
                this.mAlarmManager.cancel(alarm);
            }
            if (DEBUG) {
                Slog.d(TAG, "\tSetting up trustable hard timeout alarm");
            }
            alarm.setQueued(true);
            this.mAlarmManager.setExact(2, when, TRUST_TIMEOUT_ALARM_TAG, alarm, this.mHandler);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class AgentInfo {
        TrustAgentWrapper agent;
        ComponentName component;
        Drawable icon;
        CharSequence label;
        SettingsAttrs settings;
        int userId;

        private AgentInfo() {
        }

        public boolean equals(Object other) {
            if (other instanceof AgentInfo) {
                AgentInfo o = (AgentInfo) other;
                return this.component.equals(o.component) && this.userId == o.userId;
            }
            return false;
        }

        public int hashCode() {
            return (this.component.hashCode() * 31) + this.userId;
        }
    }

    private void updateTrustAll() {
        List<UserInfo> userInfos = this.mUserManager.getAliveUsers();
        for (UserInfo userInfo : userInfos) {
            updateTrust(userInfo.id, 0);
        }
    }

    public void updateTrust(int userId, int flags) {
        updateTrust(userId, flags, null);
    }

    public void updateTrust(int userId, int flags, AndroidFuture<GrantTrustResult> resultCallback) {
        updateTrust(userId, flags, false, resultCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTrust(int userId, int flags, boolean isFromUnlock, AndroidFuture<GrantTrustResult> resultCallback) {
        if (ENABLE_ACTIVE_UNLOCK_FLAG) {
            updateTrustWithRenewableUnlock(userId, flags, isFromUnlock, resultCallback);
        } else {
            updateTrustWithNonrenewableTrust(userId, flags, isFromUnlock);
        }
    }

    private void updateTrustWithNonrenewableTrust(int userId, int flags, boolean isFromUnlock) {
        boolean changed;
        boolean managed = aggregateIsTrustManaged(userId);
        dispatchOnTrustManagedChanged(managed, userId);
        if (this.mStrongAuthTracker.isTrustAllowedForUser(userId) && isTrustUsuallyManagedInternal(userId) != managed) {
            updateTrustUsuallyManaged(userId, managed);
        }
        boolean trusted = aggregateIsTrusted(userId);
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        boolean showingKeyguard = true;
        try {
            showingKeyguard = wm.isKeyguardLocked();
        } catch (RemoteException e) {
        }
        synchronized (this.mUserIsTrusted) {
            boolean z = true;
            if (this.mSettingsObserver.getTrustAgentsNonrenewableTrust()) {
                boolean changed2 = this.mUserIsTrusted.get(userId) != trusted;
                trusted = trusted && !(showingKeyguard && !isFromUnlock && changed2) && userId == this.mCurrentUser;
                if (DEBUG) {
                    Slog.d(TAG, "Extend unlock setting trusted as " + Boolean.toString(trusted) + " && " + Boolean.toString(!showingKeyguard) + " && " + Boolean.toString(userId == this.mCurrentUser));
                }
            }
            if (this.mUserIsTrusted.get(userId) == trusted) {
                z = false;
            }
            changed = z;
            this.mUserIsTrusted.put(userId, trusted);
        }
        dispatchOnTrustChanged(trusted, userId, flags, getTrustGrantedMessages(userId));
        if (changed) {
            refreshDeviceLockedForUser(userId);
            if (!trusted) {
                maybeLockScreen(userId);
            } else {
                scheduleTrustTimeout(false, false);
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:34:0x006a  */
    /* JADX WARN: Removed duplicated region for block: B:35:0x006c  */
    /* JADX WARN: Removed duplicated region for block: B:59:0x00a0  */
    /* JADX WARN: Removed duplicated region for block: B:62:0x00bd  */
    /* JADX WARN: Removed duplicated region for block: B:63:0x00bf  */
    /* JADX WARN: Removed duplicated region for block: B:66:0x00c9  */
    /* JADX WARN: Removed duplicated region for block: B:75:0x00de  */
    /* JADX WARN: Removed duplicated region for block: B:76:0x00e0  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void updateTrustWithRenewableUnlock(int userId, int flags, boolean isFromUnlock, AndroidFuture<GrantTrustResult> resultCallback) {
        boolean canMoveToTrusted;
        TrustState pendingTrustState;
        boolean z;
        boolean isNowTrusted;
        boolean shouldSendCallback;
        boolean managed = aggregateIsTrustManaged(userId);
        dispatchOnTrustManagedChanged(managed, userId);
        if (this.mStrongAuthTracker.isTrustAllowedForUser(userId) && isTrustUsuallyManagedInternal(userId) != managed) {
            updateTrustUsuallyManaged(userId, managed);
        }
        boolean trustedByAtLeastOneAgent = aggregateIsTrusted(userId);
        boolean trustableByAtLeastOneAgent = aggregateIsTrustable(userId);
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        boolean alreadyUnlocked = false;
        try {
            alreadyUnlocked = !wm.isKeyguardLocked();
        } catch (RemoteException e) {
        }
        synchronized (this.mUserTrustState) {
            try {
                try {
                    boolean wasTrusted = this.mUserTrustState.get(userId) == TrustState.TRUSTED;
                    boolean wasTrustable = this.mUserTrustState.get(userId) == TrustState.TRUSTABLE;
                    boolean renewingTrust = wasTrustable && (flags & 4) != 0;
                    try {
                        if (!alreadyUnlocked && !isFromUnlock && !renewingTrust) {
                            canMoveToTrusted = false;
                            boolean upgradingTrustForCurrentUser = userId != this.mCurrentUser;
                            if (trustedByAtLeastOneAgent || !wasTrusted) {
                                if (!trustedByAtLeastOneAgent && canMoveToTrusted && upgradingTrustForCurrentUser) {
                                    pendingTrustState = TrustState.TRUSTED;
                                } else if (!trustableByAtLeastOneAgent && ((wasTrusted || wasTrustable) && upgradingTrustForCurrentUser)) {
                                    pendingTrustState = TrustState.TRUSTABLE;
                                } else {
                                    TrustState pendingTrustState2 = TrustState.UNTRUSTED;
                                    pendingTrustState = pendingTrustState2;
                                }
                                this.mUserTrustState.put(userId, pendingTrustState);
                                z = DEBUG;
                                if (z) {
                                    Slog.d(TAG, "pendingTrustState: " + pendingTrustState);
                                }
                                isNowTrusted = pendingTrustState != TrustState.TRUSTED;
                                dispatchOnTrustChanged(isNowTrusted, userId, flags, getTrustGrantedMessages(userId));
                                if (isNowTrusted != wasTrusted) {
                                    refreshDeviceLockedForUser(userId);
                                    if (!isNowTrusted) {
                                        maybeLockScreen(userId);
                                    } else {
                                        boolean isTrustableTimeout = (flags & 4) != 0;
                                        scheduleTrustTimeout(isTrustableTimeout, isTrustableTimeout);
                                    }
                                }
                                boolean wasLocked = alreadyUnlocked;
                                shouldSendCallback = !wasLocked && pendingTrustState == TrustState.TRUSTED;
                                if (!shouldSendCallback && resultCallback != null) {
                                    if (z) {
                                        Slog.d(TAG, "calling back with UNLOCKED_BY_GRANT");
                                    }
                                    resultCallback.complete(new GrantTrustResult(1));
                                    return;
                                }
                                return;
                            }
                            return;
                        }
                        if (trustedByAtLeastOneAgent) {
                        }
                        if (!trustedByAtLeastOneAgent) {
                        }
                        if (!trustableByAtLeastOneAgent) {
                        }
                        TrustState pendingTrustState22 = TrustState.UNTRUSTED;
                        pendingTrustState = pendingTrustState22;
                        this.mUserTrustState.put(userId, pendingTrustState);
                        z = DEBUG;
                        if (z) {
                        }
                        if (pendingTrustState != TrustState.TRUSTED) {
                        }
                        dispatchOnTrustChanged(isNowTrusted, userId, flags, getTrustGrantedMessages(userId));
                        if (isNowTrusted != wasTrusted) {
                        }
                        if (alreadyUnlocked) {
                        }
                        shouldSendCallback = !wasLocked && pendingTrustState == TrustState.TRUSTED;
                        if (!shouldSendCallback) {
                            return;
                        }
                        return;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                    canMoveToTrusted = true;
                    if (userId != this.mCurrentUser) {
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    private void updateTrustUsuallyManaged(int userId, boolean managed) {
        synchronized (this.mTrustUsuallyManagedForUser) {
            this.mTrustUsuallyManagedForUser.put(userId, managed);
        }
        this.mHandler.removeMessages(10);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(10), 120000L);
    }

    public long addEscrowToken(byte[] token, int userId) {
        return this.mLockPatternUtils.addEscrowToken(token, userId, new LockPatternUtils.EscrowTokenStateChangeCallback() { // from class: com.android.server.trust.TrustManagerService$$ExternalSyntheticLambda0
            public final void onEscrowTokenActivated(long j, int i) {
                TrustManagerService.this.m7013xc0090753(j, i);
            }
        });
    }

    public boolean removeEscrowToken(long handle, int userId) {
        return this.mLockPatternUtils.removeEscrowToken(handle, userId);
    }

    public boolean isEscrowTokenActive(long handle, int userId) {
        return this.mLockPatternUtils.isEscrowTokenActive(handle, userId);
    }

    public void unlockUserWithToken(long handle, byte[] token, int userId) {
        this.mLockPatternUtils.unlockUserWithToken(handle, token, userId);
    }

    public void lockUser(int userId) {
        this.mLockPatternUtils.requireStrongAuth(4, userId);
        try {
            WindowManagerGlobal.getWindowManagerService().lockNow((Bundle) null);
        } catch (RemoteException e) {
            Slog.e(TAG, "Error locking screen when called from trust agent");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showKeyguardErrorMessage(CharSequence message) {
        dispatchOnTrustError(message);
    }

    void refreshAgentList(int userIdOrAll) {
        List<UserInfo> userInfos;
        List<ResolveInfo> resolveInfos;
        PackageManager pm;
        String str;
        int userIdOrAll2 = userIdOrAll;
        boolean z = DEBUG;
        String str2 = TAG;
        if (z) {
            Slog.d(TAG, "refreshAgentList(" + userIdOrAll2 + ")");
        }
        if (!this.mTrustAgentsCanRun) {
            return;
        }
        if (userIdOrAll2 != -1 && userIdOrAll2 < 0) {
            Log.e(TAG, "refreshAgentList(userId=" + userIdOrAll2 + "): Invalid user handle, must be USER_ALL or a specific user.", new Throwable("here"));
            userIdOrAll2 = -1;
        }
        PackageManager pm2 = this.mContext.getPackageManager();
        if (userIdOrAll2 == -1) {
            userInfos = this.mUserManager.getAliveUsers();
        } else {
            userInfos = new ArrayList<>();
            userInfos.add(this.mUserManager.getUserInfo(userIdOrAll2));
        }
        LockPatternUtils lockPatternUtils = this.mLockPatternUtils;
        ArraySet<AgentInfo> obsoleteAgents = new ArraySet<>();
        obsoleteAgents.addAll(this.mActiveAgents);
        Iterator<UserInfo> it = userInfos.iterator();
        while (it.hasNext()) {
            UserInfo userInfo = it.next();
            if (userInfo != null && !userInfo.partial && userInfo.isEnabled()) {
                if (!userInfo.guestToRemove) {
                    if (!userInfo.supportsSwitchToByUser()) {
                        if (DEBUG) {
                            Slog.d(str2, "refreshAgentList: skipping user " + userInfo.id + ": switchToByUser=false");
                        }
                    } else if (!this.mActivityManager.isUserRunning(userInfo.id)) {
                        if (DEBUG) {
                            Slog.d(str2, "refreshAgentList: skipping user " + userInfo.id + ": user not started");
                        }
                    } else if (!lockPatternUtils.isSecure(userInfo.id)) {
                        if (DEBUG) {
                            Slog.d(str2, "refreshAgentList: skipping user " + userInfo.id + ": no secure credential");
                        }
                    } else {
                        DevicePolicyManager dpm = lockPatternUtils.getDevicePolicyManager();
                        int disabledFeatures = dpm.getKeyguardDisabledFeatures(null, userInfo.id);
                        boolean disableTrustAgents = (disabledFeatures & 16) != 0;
                        List<ComponentName> enabledAgents = lockPatternUtils.getEnabledTrustAgents(userInfo.id);
                        if (!enabledAgents.isEmpty()) {
                            List<ResolveInfo> resolveInfos2 = resolveAllowedTrustAgents(pm2, userInfo.id);
                            for (ResolveInfo resolveInfo : resolveInfos2) {
                                List<UserInfo> userInfos2 = userInfos;
                                ComponentName name = getComponentName(resolveInfo);
                                List<ComponentName> enabledAgents2 = enabledAgents;
                                LockPatternUtils lockPatternUtils2 = lockPatternUtils;
                                if (!enabledAgents.contains(name)) {
                                    if (DEBUG) {
                                        Slog.d(str2, "refreshAgentList: skipping " + name.flattenToShortString() + " u" + userInfo.id + ": not enabled by user");
                                        userInfos = userInfos2;
                                        it = it;
                                    } else {
                                        userInfos = userInfos2;
                                    }
                                    enabledAgents = enabledAgents2;
                                    lockPatternUtils = lockPatternUtils2;
                                } else {
                                    Iterator<UserInfo> it2 = it;
                                    if (!disableTrustAgents) {
                                        resolveInfos = resolveInfos2;
                                    } else {
                                        resolveInfos = resolveInfos2;
                                        List<PersistableBundle> config = dpm.getTrustAgentConfiguration(null, name, userInfo.id);
                                        if (config == null || config.isEmpty()) {
                                            if (DEBUG) {
                                                Slog.d(str2, "refreshAgentList: skipping " + name.flattenToShortString() + " u" + userInfo.id + ": not allowed by DPM");
                                            }
                                            userInfos = userInfos2;
                                            it = it2;
                                            enabledAgents = enabledAgents2;
                                            lockPatternUtils = lockPatternUtils2;
                                            resolveInfos2 = resolveInfos;
                                        }
                                    }
                                    AgentInfo agentInfo = new AgentInfo();
                                    agentInfo.component = name;
                                    agentInfo.userId = userInfo.id;
                                    if (!this.mActiveAgents.contains(agentInfo)) {
                                        agentInfo.label = resolveInfo.loadLabel(pm2);
                                        agentInfo.icon = resolveInfo.loadIcon(pm2);
                                        agentInfo.settings = getSettingsAttrs(pm2, resolveInfo);
                                    } else {
                                        int index = this.mActiveAgents.indexOf(agentInfo);
                                        agentInfo = this.mActiveAgents.valueAt(index);
                                    }
                                    boolean directUnlock = false;
                                    if (agentInfo.settings != null) {
                                        directUnlock = resolveInfo.serviceInfo.directBootAware && agentInfo.settings.canUnlockProfile;
                                    }
                                    if (directUnlock && DEBUG) {
                                        Slog.d(str2, "refreshAgentList: trustagent " + name + "of user " + userInfo.id + "can unlock user profile.");
                                    }
                                    if (!this.mUserManager.isUserUnlockingOrUnlocked(userInfo.id) && !directUnlock) {
                                        if (DEBUG) {
                                            Slog.d(str2, "refreshAgentList: skipping user " + userInfo.id + "'s trust agent " + name + ": FBE still locked and  the agent cannot unlock user profile.");
                                        }
                                        userInfos = userInfos2;
                                        it = it2;
                                        enabledAgents = enabledAgents2;
                                        lockPatternUtils = lockPatternUtils2;
                                        resolveInfos2 = resolveInfos;
                                    } else {
                                        if (this.mStrongAuthTracker.canAgentsRunForUser(userInfo.id)) {
                                            pm = pm2;
                                        } else {
                                            int flag = this.mStrongAuthTracker.getStrongAuthForUser(userInfo.id);
                                            if (flag == 8) {
                                                pm = pm2;
                                            } else if (flag == 1 && directUnlock) {
                                                pm = pm2;
                                            } else if (DEBUG) {
                                                Slog.d(str2, "refreshAgentList: skipping user " + userInfo.id + ": prevented by StrongAuthTracker = 0x" + Integer.toHexString(this.mStrongAuthTracker.getStrongAuthForUser(userInfo.id)));
                                                userInfos = userInfos2;
                                                it = it2;
                                                enabledAgents = enabledAgents2;
                                                lockPatternUtils = lockPatternUtils2;
                                                resolveInfos2 = resolveInfos;
                                                pm2 = pm2;
                                            } else {
                                                userInfos = userInfos2;
                                                it = it2;
                                                enabledAgents = enabledAgents2;
                                                lockPatternUtils = lockPatternUtils2;
                                                resolveInfos2 = resolveInfos;
                                            }
                                        }
                                        if (agentInfo.agent == null) {
                                            str = str2;
                                            agentInfo.agent = new TrustAgentWrapper(this.mContext, this, new Intent().setComponent(name), userInfo.getUserHandle());
                                        } else {
                                            str = str2;
                                        }
                                        if (!this.mActiveAgents.contains(agentInfo)) {
                                            this.mActiveAgents.add(agentInfo);
                                        } else {
                                            obsoleteAgents.remove(agentInfo);
                                        }
                                        userInfos = userInfos2;
                                        it = it2;
                                        enabledAgents = enabledAgents2;
                                        lockPatternUtils = lockPatternUtils2;
                                        resolveInfos2 = resolveInfos;
                                        pm2 = pm;
                                        str2 = str;
                                    }
                                }
                            }
                        } else if (DEBUG) {
                            Slog.d(str2, "refreshAgentList: skipping user " + userInfo.id + ": no agents enabled by user");
                        }
                    }
                }
            }
        }
        boolean trustMayHaveChanged = false;
        for (int i = 0; i < obsoleteAgents.size(); i++) {
            AgentInfo info = obsoleteAgents.valueAt(i);
            if (userIdOrAll2 == -1 || userIdOrAll2 == info.userId) {
                if (info.agent.isManagingTrust()) {
                    trustMayHaveChanged = true;
                }
                info.agent.destroy();
                this.mActiveAgents.remove(info);
            }
        }
        if (trustMayHaveChanged) {
            if (userIdOrAll2 != -1) {
                updateTrust(userIdOrAll2, 0);
            } else {
                updateTrustAll();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDeviceLockedInner(int userId) {
        boolean z;
        synchronized (this.mDeviceLockedForUser) {
            z = this.mDeviceLockedForUser.get(userId, true);
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshDeviceLockedForUser(int userId) {
        refreshDeviceLockedForUser(userId, -10000);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshDeviceLockedForUser(int userId, int unlockedUser) {
        int userId2;
        List<UserInfo> userInfos;
        if (userId != -1 && userId < 0) {
            Log.e(TAG, "refreshDeviceLockedForUser(userId=" + userId + "): Invalid user handle, must be USER_ALL or a specific user.", new Throwable("here"));
            userId2 = -1;
        } else {
            userId2 = userId;
        }
        if (userId2 == -1) {
            userInfos = this.mUserManager.getAliveUsers();
        } else {
            List<UserInfo> arrayList = new ArrayList<>();
            arrayList.add(this.mUserManager.getUserInfo(userId2));
            userInfos = arrayList;
        }
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        for (int i = 0; i < userInfos.size(); i++) {
            UserInfo info = userInfos.get(i);
            if (info != null && !info.partial && info.isEnabled()) {
                if (!info.guestToRemove) {
                    int id = info.id;
                    boolean secure = this.mLockPatternUtils.isSecure(id);
                    if (!info.supportsSwitchToByUser()) {
                        if (info.isManagedProfile() && !secure) {
                            setDeviceLockedForUser(id, false);
                        }
                    } else {
                        boolean trusted = aggregateIsTrusted(id);
                        boolean showingKeyguard = true;
                        boolean biometricAuthenticated = false;
                        boolean currentUserIsUnlocked = false;
                        boolean z = true;
                        if (this.mCurrentUser == id) {
                            synchronized (this.mUsersUnlockedByBiometric) {
                                try {
                                    biometricAuthenticated = this.mUsersUnlockedByBiometric.get(id, false);
                                } catch (Throwable th) {
                                    th = th;
                                    while (true) {
                                        try {
                                            break;
                                        } catch (Throwable th2) {
                                            th = th2;
                                        }
                                    }
                                    throw th;
                                }
                            }
                            try {
                                showingKeyguard = wm.isKeyguardLocked();
                            } catch (RemoteException e) {
                                Log.w(TAG, "Unable to check keyguard lock state", e);
                            }
                            currentUserIsUnlocked = unlockedUser == id;
                        }
                        boolean deviceLocked = (!secure || !showingKeyguard || trusted || biometricAuthenticated) ? false : false;
                        if (!deviceLocked || !currentUserIsUnlocked) {
                            setDeviceLockedForUser(id, deviceLocked);
                        }
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDeviceLockedForUser(int userId, boolean locked) {
        int i;
        boolean changed;
        int[] enabledProfileIds;
        synchronized (this.mDeviceLockedForUser) {
            changed = isDeviceLockedInner(userId) != locked;
            this.mDeviceLockedForUser.put(userId, locked);
        }
        if (changed) {
            dispatchDeviceLocked(userId, locked);
            Authorization.onLockScreenEvent(locked, userId, (byte[]) null, getBiometricSids(userId));
            for (int profileHandle : this.mUserManager.getEnabledProfileIds(userId)) {
                if (this.mLockPatternUtils.isManagedProfileWithUnifiedChallenge(profileHandle)) {
                    Authorization.onLockScreenEvent(locked, profileHandle, (byte[]) null, getBiometricSids(profileHandle));
                }
            }
        }
    }

    private void dispatchDeviceLocked(int userId, boolean isLocked) {
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo agent = this.mActiveAgents.valueAt(i);
            if (agent.userId == userId) {
                if (isLocked) {
                    agent.agent.onDeviceLocked();
                } else {
                    agent.agent.onDeviceUnlocked();
                }
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: dispatchEscrowTokenActivatedLocked */
    public void m7013xc0090753(long handle, int userId) {
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo agent = this.mActiveAgents.valueAt(i);
            if (agent.userId == userId) {
                agent.agent.onEscrowTokenActivated(handle, userId);
            }
        }
    }

    void updateDevicePolicyFeatures() {
        boolean changed = false;
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (info.agent.isConnected()) {
                info.agent.updateDevicePolicyFeatures();
                changed = true;
            }
        }
        if (changed) {
            this.mArchive.logDevicePolicyChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeAgentsOfPackage(String packageName) {
        boolean trustMayHaveChanged = false;
        for (int i = this.mActiveAgents.size() - 1; i >= 0; i--) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (packageName.equals(info.component.getPackageName())) {
                Log.i(TAG, "Resetting agent " + info.component.flattenToShortString());
                if (info.agent.isManagingTrust()) {
                    trustMayHaveChanged = true;
                }
                info.agent.destroy();
                this.mActiveAgents.removeAt(i);
            }
        }
        if (trustMayHaveChanged) {
            updateTrustAll();
        }
    }

    public void resetAgent(ComponentName name, int userId) {
        boolean trustMayHaveChanged = false;
        for (int i = this.mActiveAgents.size() - 1; i >= 0; i--) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (name.equals(info.component) && userId == info.userId) {
                Log.i(TAG, "Resetting agent " + info.component.flattenToShortString());
                if (info.agent.isManagingTrust()) {
                    trustMayHaveChanged = true;
                }
                info.agent.destroy();
                this.mActiveAgents.removeAt(i);
            }
        }
        if (trustMayHaveChanged) {
            updateTrust(userId, 0);
        }
        refreshAgentList(userId);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1066=6] */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x007a, code lost:
        if (r7 != null) goto L30;
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x007c, code lost:
        r7.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x0089, code lost:
        if (0 == 0) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x008e, code lost:
        if (0 == 0) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x0093, code lost:
        if (0 == 0) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x0096, code lost:
        if (r8 == null) goto L34;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x0098, code lost:
        android.util.Slog.w(com.android.server.trust.TrustManagerService.TAG, "Error parsing : " + r18.serviceInfo.packageName, r8);
     */
    /* JADX WARN: Code restructure failed: missing block: B:48:0x00b2, code lost:
        return null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00b3, code lost:
        if (r5 != null) goto L36;
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x00b5, code lost:
        return null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x00bc, code lost:
        if (r5.indexOf(47) >= 0) goto L39;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x00be, code lost:
        r5 = r18.serviceInfo.packageName + com.android.server.slice.SliceClientPermissions.SliceAuthority.DELIMITER + r5;
     */
    /* JADX WARN: Code restructure failed: missing block: B:55:0x00e2, code lost:
        return new com.android.server.trust.TrustManagerService.SettingsAttrs(android.content.ComponentName.unflattenFromString(r5), r6);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private SettingsAttrs getSettingsAttrs(PackageManager pm, ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null || resolveInfo.serviceInfo.metaData == null) {
            return null;
        }
        String cn = null;
        boolean canUnlockProfile = false;
        XmlResourceParser parser = null;
        Exception caughtException = null;
        try {
            parser = resolveInfo.serviceInfo.loadXmlMetaData(pm, "android.service.trust.trustagent");
            if (parser == null) {
                Slog.w(TAG, "Can't find android.service.trust.trustagent meta-data");
                if (parser != null) {
                    parser.close();
                }
                return null;
            }
            Resources res = pm.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            while (true) {
                int type = parser.next();
                if (type == 1 || type == 2) {
                    break;
                }
            }
            String nodeName = parser.getName();
            if (!"trust-agent".equals(nodeName)) {
                Slog.w(TAG, "Meta-data does not start with trust-agent tag");
                if (parser != null) {
                    parser.close();
                }
                return null;
            }
            TypedArray sa = res.obtainAttributes(attrs, R.styleable.TrustAgent);
            cn = sa.getString(2);
            canUnlockProfile = attrs.getAttributeBooleanValue(PRIV_NAMESPACE, "unlockProfile", false);
            sa.recycle();
        } catch (PackageManager.NameNotFoundException e) {
            caughtException = e;
        } catch (IOException e2) {
            caughtException = e2;
        } catch (XmlPullParserException e3) {
            caughtException = e3;
        } catch (Throwable th) {
            if (0 != 0) {
                parser.close();
            }
            throw th;
        }
    }

    private ComponentName getComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            return null;
        }
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeEnableFactoryTrustAgents(LockPatternUtils utils, int userId) {
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "trust_agents_initialized", 0, userId) != 0) {
            return;
        }
        PackageManager pm = this.mContext.getPackageManager();
        List<ResolveInfo> resolveInfos = resolveAllowedTrustAgents(pm, userId);
        ComponentName defaultAgent = getDefaultFactoryTrustAgent(this.mContext);
        boolean shouldUseDefaultAgent = defaultAgent != null;
        ArraySet<ComponentName> discoveredAgents = new ArraySet<>();
        if (shouldUseDefaultAgent) {
            discoveredAgents.add(defaultAgent);
            Log.i(TAG, "Enabling " + defaultAgent + " because it is a default agent.");
        } else {
            for (ResolveInfo resolveInfo : resolveInfos) {
                ComponentName componentName = getComponentName(resolveInfo);
                int applicationInfoFlags = resolveInfo.serviceInfo.applicationInfo.flags;
                if ((applicationInfoFlags & 1) == 0) {
                    Log.i(TAG, "Leaving agent " + componentName + " disabled because package is not a system package.");
                } else {
                    discoveredAgents.add(componentName);
                }
            }
        }
        List<ComponentName> previouslyEnabledAgents = utils.getEnabledTrustAgents(userId);
        discoveredAgents.addAll(previouslyEnabledAgents);
        utils.setEnabledTrustAgents(discoveredAgents, userId);
        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "trust_agents_initialized", 1, userId);
    }

    private static ComponentName getDefaultFactoryTrustAgent(Context context) {
        String defaultTrustAgent = context.getResources().getString(17039948);
        if (TextUtils.isEmpty(defaultTrustAgent)) {
            return null;
        }
        return ComponentName.unflattenFromString(defaultTrustAgent);
    }

    private List<ResolveInfo> resolveAllowedTrustAgents(PackageManager pm, int userId) {
        List<ResolveInfo> resolveInfos = pm.queryIntentServicesAsUser(TRUST_AGENT_INTENT, 786560, userId);
        ArrayList<ResolveInfo> allowedAgents = new ArrayList<>(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.serviceInfo != null && resolveInfo.serviceInfo.applicationInfo != null) {
                String packageName = resolveInfo.serviceInfo.packageName;
                if (pm.checkPermission(PERMISSION_PROVIDE_AGENT, packageName) != 0) {
                    ComponentName name = getComponentName(resolveInfo);
                    Log.w(TAG, "Skipping agent " + name + " because package does not have permission " + PERMISSION_PROVIDE_AGENT + ".");
                } else {
                    allowedAgents.add(resolveInfo);
                }
            }
        }
        return allowedAgents;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean aggregateIsTrusted(int userId) {
        if (this.mStrongAuthTracker.isTrustAllowedForUser(userId)) {
            for (int i = 0; i < this.mActiveAgents.size(); i++) {
                AgentInfo info = this.mActiveAgents.valueAt(i);
                if (info.userId == userId && info.agent.isTrusted()) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean aggregateIsTrustable(int userId) {
        if (this.mStrongAuthTracker.isTrustAllowedForUser(userId)) {
            for (int i = 0; i < this.mActiveAgents.size(); i++) {
                AgentInfo info = this.mActiveAgents.valueAt(i);
                if (info.userId == userId && info.agent.isTrustable()) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchTrustableDowngrade() {
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (info.userId == this.mCurrentUser) {
                info.agent.downgradeToTrustable();
            }
        }
    }

    private List<String> getTrustGrantedMessages(int userId) {
        if (!this.mStrongAuthTracker.isTrustAllowedForUser(userId)) {
            return new ArrayList();
        }
        List<String> trustGrantedMessages = new ArrayList<>();
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (info.userId == userId && info.agent.isTrusted() && info.agent.shouldDisplayTrustGrantedMessage() && !TextUtils.isEmpty(info.agent.getMessage())) {
                trustGrantedMessages.add(info.agent.getMessage().toString());
            }
        }
        return trustGrantedMessages;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean aggregateIsTrustManaged(int userId) {
        if (this.mStrongAuthTracker.isTrustAllowedForUser(userId)) {
            for (int i = 0; i < this.mActiveAgents.size(); i++) {
                AgentInfo info = this.mActiveAgents.valueAt(i);
                if (info.userId == userId && info.agent.isManagingTrust()) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchUnlockAttempt(boolean successful, int userId) {
        if (successful) {
            this.mStrongAuthTracker.allowTrustFromUnlock(userId);
            updateTrust(userId, 0, true, null);
            this.mHandler.obtainMessage(17, Integer.valueOf(userId)).sendToTarget();
        }
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (info.userId == userId) {
                info.agent.onUnlockAttempt(successful);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchUserRequestedUnlock(int userId, boolean dismissKeyguard) {
        if (DEBUG) {
            Slog.d(TAG, "dispatchUserRequestedUnlock(user=" + userId + ", dismissKeyguard=" + dismissKeyguard + ")");
        }
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (info.userId == userId) {
                info.agent.onUserRequestedUnlock(dismissKeyguard);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchUserMayRequestUnlock(int userId) {
        if (DEBUG) {
            Slog.d(TAG, "dispatchUserMayRequestUnlock(user=" + userId + ")");
        }
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (info.userId == userId) {
                info.agent.onUserMayRequestUnlock();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchUnlockLockout(int timeoutMs, int userId) {
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (info.userId == userId) {
                info.agent.onUnlockLockout(timeoutMs);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addListener(ITrustListener listener) {
        for (int i = 0; i < this.mTrustListeners.size(); i++) {
            if (this.mTrustListeners.get(i).asBinder() == listener.asBinder()) {
                return;
            }
        }
        this.mTrustListeners.add(listener);
        updateTrustAll();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeListener(ITrustListener listener) {
        for (int i = 0; i < this.mTrustListeners.size(); i++) {
            if (this.mTrustListeners.get(i).asBinder() == listener.asBinder()) {
                this.mTrustListeners.remove(i);
                return;
            }
        }
    }

    private void dispatchOnTrustChanged(boolean enabled, int userId, int flags, List<String> trustGrantedMessages) {
        if (DEBUG) {
            Log.i(TAG, "onTrustChanged(" + enabled + ", " + userId + ", 0x" + Integer.toHexString(flags) + ")");
        }
        if (!enabled) {
            flags = 0;
        }
        int i = 0;
        while (i < this.mTrustListeners.size()) {
            try {
                this.mTrustListeners.get(i).onTrustChanged(enabled, userId, flags, trustGrantedMessages);
            } catch (DeadObjectException e) {
                Slog.d(TAG, "Removing dead TrustListener.");
                this.mTrustListeners.remove(i);
                i--;
            } catch (RemoteException e2) {
                Slog.e(TAG, "Exception while notifying TrustListener.", e2);
            }
            i++;
        }
    }

    private void dispatchOnTrustManagedChanged(boolean managed, int userId) {
        if (DEBUG) {
            Log.i(TAG, "onTrustManagedChanged(" + managed + ", " + userId + ")");
        }
        int i = 0;
        while (i < this.mTrustListeners.size()) {
            try {
                this.mTrustListeners.get(i).onTrustManagedChanged(managed, userId);
            } catch (DeadObjectException e) {
                Slog.d(TAG, "Removing dead TrustListener.");
                this.mTrustListeners.remove(i);
                i--;
            } catch (RemoteException e2) {
                Slog.e(TAG, "Exception while notifying TrustListener.", e2);
            }
            i++;
        }
    }

    private void dispatchOnTrustError(CharSequence message) {
        if (DEBUG) {
            Log.i(TAG, "onTrustError(" + ((Object) message) + ")");
        }
        int i = 0;
        while (i < this.mTrustListeners.size()) {
            try {
                this.mTrustListeners.get(i).onTrustError(message);
            } catch (DeadObjectException e) {
                Slog.d(TAG, "Removing dead TrustListener.");
                this.mTrustListeners.remove(i);
                i--;
            } catch (RemoteException e2) {
                Slog.e(TAG, "Exception while notifying TrustListener.", e2);
            }
            i++;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long[] getBiometricSids(int userId) {
        BiometricManager biometricManager = (BiometricManager) this.mContext.getSystemService(BiometricManager.class);
        if (biometricManager == null) {
            return null;
        }
        return biometricManager.getAuthenticatorIds(userId);
    }

    @Override // com.android.server.SystemService
    public void onUserStarting(SystemService.TargetUser user) {
        this.mHandler.obtainMessage(7, user.getUserIdentifier(), 0, null).sendToTarget();
    }

    @Override // com.android.server.SystemService
    public void onUserStopped(SystemService.TargetUser user) {
        this.mHandler.obtainMessage(8, user.getUserIdentifier(), 0, null).sendToTarget();
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        this.mHandler.obtainMessage(9, to.getUserIdentifier(), 0, null).sendToTarget();
    }

    @Override // com.android.server.SystemService
    public void onUserUnlocking(SystemService.TargetUser user) {
        this.mHandler.obtainMessage(11, user.getUserIdentifier(), 0, null).sendToTarget();
    }

    @Override // com.android.server.SystemService
    public void onUserStopping(SystemService.TargetUser user) {
        this.mHandler.obtainMessage(12, user.getUserIdentifier(), 0, null).sendToTarget();
    }

    /* renamed from: com.android.server.trust.TrustManagerService$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass1 extends ITrustManager.Stub {
        AnonymousClass1() {
        }

        public void reportUnlockAttempt(boolean authenticated, int userId) throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.obtainMessage(3, authenticated ? 1 : 0, userId).sendToTarget();
        }

        public void reportUserRequestedUnlock(int userId, boolean dismissKeyguard) throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.obtainMessage(16, userId, dismissKeyguard ? 1 : 0).sendToTarget();
        }

        public void reportUserMayRequestUnlock(int userId) throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.obtainMessage(18, Integer.valueOf(userId)).sendToTarget();
        }

        public void reportUnlockLockout(int timeoutMs, int userId) throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.obtainMessage(13, timeoutMs, userId).sendToTarget();
        }

        public void reportEnabledTrustAgentsChanged(int userId) throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.removeMessages(4);
            TrustManagerService.this.mHandler.sendEmptyMessage(4);
        }

        public void reportKeyguardShowingChanged() throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.removeMessages(6);
            TrustManagerService.this.mHandler.sendEmptyMessage(6);
            TrustManagerService.this.mHandler.runWithScissors(new Runnable() { // from class: com.android.server.trust.TrustManagerService$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    TrustManagerService.AnonymousClass1.lambda$reportKeyguardShowingChanged$0();
                }
            }, 0L);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$reportKeyguardShowingChanged$0() {
        }

        public void registerTrustListener(ITrustListener trustListener) throws RemoteException {
            enforceListenerPermission();
            TrustManagerService.this.mHandler.obtainMessage(1, trustListener).sendToTarget();
        }

        public void unregisterTrustListener(ITrustListener trustListener) throws RemoteException {
            enforceListenerPermission();
            TrustManagerService.this.mHandler.obtainMessage(2, trustListener).sendToTarget();
        }

        private boolean isAppOrDisplayOnAnyVirtualDevice(int uid, int displayId) {
            if (UserHandle.isCore(uid)) {
                return false;
            }
            if (TrustManagerService.this.mVirtualDeviceManager == null) {
                TrustManagerService.this.mVirtualDeviceManager = (VirtualDeviceManagerInternal) LocalServices.getService(VirtualDeviceManagerInternal.class);
                if (TrustManagerService.this.mVirtualDeviceManager == null) {
                    return false;
                }
            }
            switch (displayId) {
                case -1:
                    if (TrustManagerService.this.mVirtualDeviceManager.isAppRunningOnAnyVirtualDevice(uid)) {
                        return true;
                    }
                    break;
                case 0:
                    break;
                default:
                    if (TrustManagerService.this.mVirtualDeviceManager.isDisplayOwnedByAnyVirtualDevice(displayId)) {
                        return true;
                    }
                    break;
            }
            return false;
        }

        public boolean isDeviceLocked(int userId, int displayId) throws RemoteException {
            int uid = getCallingUid();
            if (isAppOrDisplayOnAnyVirtualDevice(uid, displayId)) {
                return false;
            }
            int userId2 = ActivityManager.handleIncomingUser(getCallingPid(), uid, userId, false, true, "isDeviceLocked", null);
            long token = Binder.clearCallingIdentity();
            try {
                if (!TrustManagerService.this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId2)) {
                    userId2 = TrustManagerService.this.resolveProfileParent(userId2);
                }
                return TrustManagerService.this.isDeviceLockedInner(userId2);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isDeviceSecure(int userId, int displayId) throws RemoteException {
            int uid = getCallingUid();
            if (isAppOrDisplayOnAnyVirtualDevice(uid, displayId)) {
                return false;
            }
            int userId2 = ActivityManager.handleIncomingUser(getCallingPid(), uid, userId, false, true, "isDeviceSecure", null);
            long token = Binder.clearCallingIdentity();
            try {
                if (!TrustManagerService.this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId2)) {
                    userId2 = TrustManagerService.this.resolveProfileParent(userId2);
                }
                return TrustManagerService.this.mLockPatternUtils.isSecure(userId2);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        private void enforceReportPermission() {
            TrustManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_KEYGUARD_SECURE_STORAGE", "reporting trust events");
        }

        private void enforceListenerPermission() {
            TrustManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.TRUST_LISTENER", "register trust listener");
        }

        protected void dump(FileDescriptor fd, final PrintWriter fout, String[] args) {
            if (DumpUtils.checkDumpPermission(TrustManagerService.this.mContext, TrustManagerService.TAG, fout)) {
                if (TrustManagerService.this.isSafeMode()) {
                    fout.println("disabled because the system is in safe mode.");
                } else if (!TrustManagerService.this.mTrustAgentsCanRun) {
                    fout.println("disabled because the third-party apps can't run yet.");
                } else {
                    final List<UserInfo> userInfos = TrustManagerService.this.mUserManager.getAliveUsers();
                    TrustManagerService.this.mHandler.runWithScissors(new Runnable() { // from class: com.android.server.trust.TrustManagerService.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            fout.println("Trust manager state:");
                            for (UserInfo user : userInfos) {
                                AnonymousClass1.this.dumpUser(fout, user, user.id == TrustManagerService.this.mCurrentUser);
                            }
                        }
                    }, 1500L);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dumpUser(PrintWriter fout, UserInfo user, boolean isCurrent) {
            fout.printf(" User \"%s\" (id=%d, flags=%#x)", user.name, Integer.valueOf(user.id), Integer.valueOf(user.flags));
            if (!user.supportsSwitchToByUser()) {
                fout.println("(managed profile)");
                fout.println("   disabled because switching to this user is not possible.");
                return;
            }
            if (isCurrent) {
                fout.print(" (current)");
            }
            fout.print(": trusted=" + dumpBool(TrustManagerService.this.aggregateIsTrusted(user.id)));
            fout.print(", trustManaged=" + dumpBool(TrustManagerService.this.aggregateIsTrustManaged(user.id)));
            fout.print(", deviceLocked=" + dumpBool(TrustManagerService.this.isDeviceLockedInner(user.id)));
            fout.print(", strongAuthRequired=" + dumpHex(TrustManagerService.this.mStrongAuthTracker.getStrongAuthForUser(user.id)));
            fout.println();
            fout.println("   Enabled agents:");
            boolean duplicateSimpleNames = false;
            ArraySet<String> simpleNames = new ArraySet<>();
            Iterator it = TrustManagerService.this.mActiveAgents.iterator();
            while (it.hasNext()) {
                AgentInfo info = (AgentInfo) it.next();
                if (info.userId == user.id) {
                    boolean trusted = info.agent.isTrusted();
                    fout.print("    ");
                    fout.println(info.component.flattenToShortString());
                    fout.print("     bound=" + dumpBool(info.agent.isBound()));
                    fout.print(", connected=" + dumpBool(info.agent.isConnected()));
                    fout.print(", managingTrust=" + dumpBool(info.agent.isManagingTrust()));
                    fout.print(", trusted=" + dumpBool(trusted));
                    fout.println();
                    if (trusted) {
                        fout.println("      message=\"" + ((Object) info.agent.getMessage()) + "\"");
                    }
                    if (!info.agent.isConnected()) {
                        String restartTime = TrustArchive.formatDuration(info.agent.getScheduledRestartUptimeMillis() - SystemClock.uptimeMillis());
                        fout.println("      restartScheduledAt=" + restartTime);
                    }
                    if (!simpleNames.add(TrustArchive.getSimpleName(info.component))) {
                        duplicateSimpleNames = true;
                    }
                }
            }
            fout.println("   Events:");
            TrustManagerService.this.mArchive.dump(fout, 50, user.id, "    ", duplicateSimpleNames);
            fout.println();
        }

        private String dumpBool(boolean b) {
            return b ? "1" : "0";
        }

        private String dumpHex(int i) {
            return "0x" + Integer.toHexString(i);
        }

        public void setDeviceLockedForUser(int userId, boolean locked) {
            enforceReportPermission();
            long identity = Binder.clearCallingIdentity();
            try {
                if (TrustManagerService.this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId) && TrustManagerService.this.mLockPatternUtils.isSecure(userId)) {
                    synchronized (TrustManagerService.this.mDeviceLockedForUser) {
                        TrustManagerService.this.mDeviceLockedForUser.put(userId, locked);
                    }
                    Authorization.onLockScreenEvent(locked, userId, (byte[]) null, TrustManagerService.this.getBiometricSids(userId));
                    if (locked) {
                        try {
                            ActivityManager.getService().notifyLockedProfile(userId);
                        } catch (RemoteException e) {
                        }
                    }
                    Intent lockIntent = new Intent("android.intent.action.DEVICE_LOCKED_CHANGED");
                    lockIntent.addFlags(1073741824);
                    lockIntent.putExtra("android.intent.extra.user_handle", userId);
                    TrustManagerService.this.mContext.sendBroadcastAsUser(lockIntent, UserHandle.SYSTEM, "android.permission.TRUST_LISTENER", null);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean isTrustUsuallyManaged(int userId) {
            TrustManagerService.this.mContext.enforceCallingPermission("android.permission.TRUST_LISTENER", "query trust state");
            return TrustManagerService.this.isTrustUsuallyManagedInternal(userId);
        }

        public void unlockedByBiometricForUser(int userId, BiometricSourceType biometricSource) {
            enforceReportPermission();
            synchronized (TrustManagerService.this.mUsersUnlockedByBiometric) {
                TrustManagerService.this.mUsersUnlockedByBiometric.put(userId, true);
            }
            boolean trustAgentsNonrenewableTrust = TrustManagerService.this.mSettingsObserver.getTrustAgentsNonrenewableTrust();
            Handler handler = TrustManagerService.this.mHandler;
            int updateTrustOnUnlock = trustAgentsNonrenewableTrust ? 1 : 0;
            handler.obtainMessage(14, userId, updateTrustOnUnlock).sendToTarget();
            TrustManagerService.this.mHandler.obtainMessage(17, Integer.valueOf(userId)).sendToTarget();
        }

        public void clearAllBiometricRecognized(BiometricSourceType biometricSource, int unlockedUser) {
            enforceReportPermission();
            synchronized (TrustManagerService.this.mUsersUnlockedByBiometric) {
                TrustManagerService.this.mUsersUnlockedByBiometric.clear();
            }
            Message message = TrustManagerService.this.mHandler.obtainMessage(14, -1, 0);
            if (unlockedUser >= 0) {
                Bundle bundle = new Bundle();
                bundle.putInt(TrustManagerService.REFRESH_DEVICE_LOCKED_EXCEPT_USER, unlockedUser);
                message.setData(bundle);
            }
            message.sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTrustUsuallyManagedInternal(int userId) {
        synchronized (this.mTrustUsuallyManagedForUser) {
            int i = this.mTrustUsuallyManagedForUser.indexOfKey(userId);
            if (i >= 0) {
                return this.mTrustUsuallyManagedForUser.valueAt(i);
            }
            boolean persistedValue = this.mLockPatternUtils.isTrustUsuallyManaged(userId);
            synchronized (this.mTrustUsuallyManagedForUser) {
                int i2 = this.mTrustUsuallyManagedForUser.indexOfKey(userId);
                if (i2 >= 0) {
                    return this.mTrustUsuallyManagedForUser.valueAt(i2);
                }
                this.mTrustUsuallyManagedForUser.put(userId, persistedValue);
                return persistedValue;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int resolveProfileParent(int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            UserInfo parent = this.mUserManager.getProfileParent(userId);
            if (parent != null) {
                return parent.getUserHandle().getIdentifier();
            }
            return userId;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class SettingsAttrs {
        public boolean canUnlockProfile;
        public ComponentName componentName;

        public SettingsAttrs(ComponentName componentName, boolean canUnlockProfile) {
            this.componentName = componentName;
            this.canUnlockProfile = canUnlockProfile;
        }
    }

    /* loaded from: classes2.dex */
    private class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int userId;
            String action = intent.getAction();
            if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action)) {
                TrustManagerService.this.refreshAgentList(getSendingUserId());
                TrustManagerService.this.updateDevicePolicyFeatures();
            } else if ("android.intent.action.USER_ADDED".equals(action) || "android.intent.action.USER_STARTED".equals(action)) {
                int userId2 = getUserId(intent);
                if (userId2 > 0) {
                    TrustManagerService trustManagerService = TrustManagerService.this;
                    trustManagerService.maybeEnableFactoryTrustAgents(trustManagerService.mLockPatternUtils, userId2);
                }
            } else if ("android.intent.action.USER_REMOVED".equals(action) && (userId = getUserId(intent)) > 0) {
                synchronized (TrustManagerService.this.mUserIsTrusted) {
                    TrustManagerService.this.mUserIsTrusted.delete(userId);
                }
                synchronized (TrustManagerService.this.mDeviceLockedForUser) {
                    TrustManagerService.this.mDeviceLockedForUser.delete(userId);
                }
                synchronized (TrustManagerService.this.mTrustUsuallyManagedForUser) {
                    TrustManagerService.this.mTrustUsuallyManagedForUser.delete(userId);
                }
                synchronized (TrustManagerService.this.mUsersUnlockedByBiometric) {
                    TrustManagerService.this.mUsersUnlockedByBiometric.delete(userId);
                }
                TrustManagerService.this.refreshAgentList(userId);
                TrustManagerService.this.refreshDeviceLockedForUser(userId);
            }
        }

        private int getUserId(Intent intent) {
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -100);
            if (userId > 0) {
                return userId;
            }
            Log.w(TrustManagerService.TAG, "EXTRA_USER_HANDLE missing or invalid, value=" + userId);
            return -100;
        }

        public void register(Context context) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
            filter.addAction("android.intent.action.USER_ADDED");
            filter.addAction("android.intent.action.USER_REMOVED");
            filter.addAction("android.intent.action.USER_STARTED");
            context.registerReceiverAsUser(this, UserHandle.ALL, filter, null, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class StrongAuthTracker extends LockPatternUtils.StrongAuthTracker {
        SparseBooleanArray mStartFromSuccessfulUnlock;

        public StrongAuthTracker(Context context) {
            super(context);
            this.mStartFromSuccessfulUnlock = new SparseBooleanArray();
        }

        public void onStrongAuthRequiredChanged(int userId) {
            this.mStartFromSuccessfulUnlock.delete(userId);
            if (TrustManagerService.DEBUG) {
                Log.i(TrustManagerService.TAG, "onStrongAuthRequiredChanged(" + userId + ") -> trustAllowed=" + isTrustAllowedForUser(userId) + " agentsCanRun=" + canAgentsRunForUser(userId));
            }
            if (!isTrustAllowedForUser(userId)) {
                TrustTimeoutAlarmListener alarm = (TrustTimeoutAlarmListener) TrustManagerService.this.mTrustTimeoutAlarmListenerForUser.get(Integer.valueOf(userId));
                cancelPendingAlarm(alarm);
                TrustTimeoutAlarmListener alarm2 = (TrustTimeoutAlarmListener) TrustManagerService.this.mTrustableTimeoutAlarmListenerForUser.get(userId);
                cancelPendingAlarm(alarm2);
                TrustTimeoutAlarmListener alarm3 = (TrustTimeoutAlarmListener) TrustManagerService.this.mIdleTrustableTimeoutAlarmListenerForUser.get(userId);
                cancelPendingAlarm(alarm3);
            }
            TrustManagerService.this.refreshAgentList(userId);
            TrustManagerService.this.updateTrust(userId, 0);
        }

        private void cancelPendingAlarm(TrustTimeoutAlarmListener alarm) {
            if (alarm != null && alarm.isQueued()) {
                alarm.setQueued(false);
                TrustManagerService.this.mAlarmManager.cancel(alarm);
            }
        }

        boolean canAgentsRunForUser(int userId) {
            return this.mStartFromSuccessfulUnlock.get(userId) || super.isTrustAllowedForUser(userId);
        }

        void allowTrustFromUnlock(int userId) {
            if (userId < 0) {
                throw new IllegalArgumentException("userId must be a valid user: " + userId);
            }
            boolean previous = canAgentsRunForUser(userId);
            this.mStartFromSuccessfulUnlock.put(userId, true);
            if (TrustManagerService.DEBUG) {
                Log.i(TrustManagerService.TAG, "allowTrustFromUnlock(" + userId + ") -> trustAllowed=" + isTrustAllowedForUser(userId) + " agentsCanRun=" + canAgentsRunForUser(userId));
            }
            if (canAgentsRunForUser(userId) != previous) {
                TrustManagerService.this.refreshAgentList(userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public abstract class TrustTimeoutAlarmListener implements AlarmManager.OnAlarmListener {
        protected boolean mIsQueued = false;
        protected final int mUserId;

        protected abstract void handleAlarm();

        TrustTimeoutAlarmListener(int userId) {
            this.mUserId = userId;
        }

        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            this.mIsQueued = false;
            handleAlarm();
            if (TrustManagerService.this.mStrongAuthTracker.isTrustAllowedForUser(this.mUserId)) {
                if (TrustManagerService.DEBUG) {
                    Slog.d(TrustManagerService.TAG, "Revoking all trust because of trust timeout");
                }
                LockPatternUtils lockPatternUtils = TrustManagerService.this.mLockPatternUtils;
                StrongAuthTracker unused = TrustManagerService.this.mStrongAuthTracker;
                lockPatternUtils.requireStrongAuth(4, this.mUserId);
            }
            TrustManagerService.this.maybeLockScreen(this.mUserId);
        }

        public boolean isQueued() {
            return this.mIsQueued;
        }

        public void setQueued(boolean isQueued) {
            this.mIsQueued = isQueued;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class TrustedTimeoutAlarmListener extends TrustTimeoutAlarmListener {
        TrustedTimeoutAlarmListener(int userId) {
            super(userId);
        }

        @Override // com.android.server.trust.TrustManagerService.TrustTimeoutAlarmListener
        public void handleAlarm() {
            if (TrustManagerService.ENABLE_ACTIVE_UNLOCK_FLAG) {
                TrustableTimeoutAlarmListener otherAlarm = (TrustableTimeoutAlarmListener) TrustManagerService.this.mTrustableTimeoutAlarmListenerForUser.get(this.mUserId);
                boolean otherAlarmPresent = otherAlarm != null && otherAlarm.isQueued();
                if (otherAlarmPresent) {
                    synchronized (TrustManagerService.this.mAlarmLock) {
                        disableNonrenewableTrustWhileRenewableTrustIsPresent();
                    }
                }
            }
        }

        private void disableNonrenewableTrustWhileRenewableTrustIsPresent() {
            synchronized (TrustManagerService.this.mUserTrustState) {
                if (TrustManagerService.this.mUserTrustState.get(this.mUserId) == TrustState.TRUSTED) {
                    TrustManagerService.this.mUserTrustState.put(this.mUserId, TrustState.TRUSTABLE);
                    TrustManagerService.this.updateTrust(this.mUserId, 0);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class TrustableTimeoutAlarmListener extends TrustTimeoutAlarmListener {
        TrustableTimeoutAlarmListener(int userId) {
            super(userId);
        }

        @Override // com.android.server.trust.TrustManagerService.TrustTimeoutAlarmListener
        public void handleAlarm() {
            if (TrustManagerService.ENABLE_ACTIVE_UNLOCK_FLAG) {
                cancelBothTrustableAlarms();
                TrustedTimeoutAlarmListener otherAlarm = (TrustedTimeoutAlarmListener) TrustManagerService.this.mTrustTimeoutAlarmListenerForUser.get(Integer.valueOf(this.mUserId));
                boolean otherAlarmPresent = otherAlarm != null && otherAlarm.isQueued();
                if (otherAlarmPresent) {
                    synchronized (TrustManagerService.this.mAlarmLock) {
                        disableRenewableTrustWhileNonrenewableTrustIsPresent();
                    }
                }
            }
        }

        private void cancelBothTrustableAlarms() {
            TrustableTimeoutAlarmListener idleTimeout = (TrustableTimeoutAlarmListener) TrustManagerService.this.mIdleTrustableTimeoutAlarmListenerForUser.get(this.mUserId);
            TrustableTimeoutAlarmListener trustableTimeout = (TrustableTimeoutAlarmListener) TrustManagerService.this.mTrustableTimeoutAlarmListenerForUser.get(this.mUserId);
            if (idleTimeout != null && idleTimeout.isQueued()) {
                idleTimeout.setQueued(false);
                TrustManagerService.this.mAlarmManager.cancel(idleTimeout);
            }
            if (trustableTimeout != null && trustableTimeout.isQueued()) {
                trustableTimeout.setQueued(false);
                TrustManagerService.this.mAlarmManager.cancel(trustableTimeout);
            }
        }

        private void disableRenewableTrustWhileNonrenewableTrustIsPresent() {
            Iterator it = TrustManagerService.this.mActiveAgents.iterator();
            while (it.hasNext()) {
                AgentInfo agentInfo = (AgentInfo) it.next();
                agentInfo.agent.setUntrustable();
            }
            TrustManagerService.this.updateTrust(this.mUserId, 0);
        }
    }
}
