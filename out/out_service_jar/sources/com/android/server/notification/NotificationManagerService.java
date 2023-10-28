package com.android.server.notification;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.AutomaticZenRule;
import android.app.IActivityManager;
import android.app.INotificationManager;
import android.app.ITransientNotification;
import android.app.ITransientNotificationCallback;
import android.app.IUriGrantsManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationHistory;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.StatsManager;
import android.app.UriGrantsManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.backup.BackupManager;
import android.app.compat.CompatChanges;
import android.app.role.OnRoleHoldersChangedListener;
import android.app.role.RoleManager;
import android.app.usage.UsageStatsManagerInternal;
import android.companion.ICompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ServiceInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutServiceInternal;
import android.content.pm.UserInfo;
import android.content.pm.VersionedPackage;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.hardware.audio.common.V2_0.AudioChannelMask;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioManagerInternal;
import android.media.IRingtonePlayer;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.DeviceIdleManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.VibrationEffect;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.service.notification.Adjustment;
import android.service.notification.Condition;
import android.service.notification.ConversationChannelWrapper;
import android.service.notification.IConditionProvider;
import android.service.notification.INotificationListener;
import android.service.notification.IStatusBarNotificationHolder;
import android.service.notification.NotificationListenerFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationRankingUpdate;
import android.service.notification.NotificationStats;
import android.service.notification.SnoozeCriterion;
import android.service.notification.StatusBarNotification;
import android.service.notification.ZenModeConfig;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.IntArray;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.StatsEvent;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.util.proto.ProtoOutputStream;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.android.internal.app.IAppOpsService;
import com.android.internal.compat.IPlatformCompat;
import com.android.internal.logging.InstanceId;
import com.android.internal.logging.InstanceIdSequence;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.function.TriPredicate;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.DeviceIdleInternal;
import com.android.server.EventLogTags;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.UiThread;
import com.android.server.am.HostingRecord;
import com.android.server.lights.LightsManager;
import com.android.server.lights.LogicalLight;
import com.android.server.notification.GroupHelper;
import com.android.server.notification.ManagedServices;
import com.android.server.notification.NotificationManagerService;
import com.android.server.notification.NotificationRecord;
import com.android.server.notification.NotificationRecordLogger;
import com.android.server.notification.ShortcutHelper;
import com.android.server.notification.SnoozeHelper;
import com.android.server.notification.SysUiStatsEvent;
import com.android.server.notification.ZenModeHelper;
import com.android.server.notification.toast.CustomToastRecord;
import com.android.server.notification.toast.TextToastRecord;
import com.android.server.notification.toast.ToastRecord;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.policy.PermissionPolicyInternal;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.uri.UriGrantsManagerInternal;
import com.android.server.utils.PriorityDump;
import com.android.server.utils.quota.MultiRateLimiter;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.BackgroundActivityStartCallback;
import com.android.server.wm.WindowManagerInternal;
import com.transsion.hubcore.server.notification.ITranNotificationManagerService;
import com.transsion.hubcore.server.tranled.ITranLedLightExt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import libcore.io.IoUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class NotificationManagerService extends SystemService {
    private static final String ATTR_VERSION = "version";
    private static final long CHANGE_BACKGROUND_CUSTOM_TOAST_BLOCK = 128611929;
    private static final int DB_VERSION = 1;
    static final float DEFAULT_MAX_NOTIFICATION_ENQUEUE_RATE = 5.0f;
    private static final long DELAY_FOR_ASSISTANT_TIME = 200;
    private static final int EVENTLOG_ENQUEUE_STATUS_IGNORED = 2;
    private static final int EVENTLOG_ENQUEUE_STATUS_NEW = 0;
    private static final int EVENTLOG_ENQUEUE_STATUS_UPDATE = 1;
    private static final String EXTRA_KEY = "key";
    static final int FINISH_TOKEN_TIMEOUT = 11000;
    static final int INVALID_UID = -1;
    private static final String LOCKSCREEN_ALLOW_SECURE_NOTIFICATIONS_TAG = "allow-secure-notifications-on-lockscreen";
    private static final String LOCKSCREEN_ALLOW_SECURE_NOTIFICATIONS_VALUE = "value";
    static final int LONG_DELAY = 3500;
    static final int MATCHES_CALL_FILTER_CONTACTS_TIMEOUT_MS = 3000;
    static final float MATCHES_CALL_FILTER_TIMEOUT_AFFINITY = 1.0f;
    static final int MAX_PACKAGE_NOTIFICATIONS = 50;
    static final int MAX_PACKAGE_TOASTS = 5;
    static final int MESSAGE_DURATION_REACHED = 2;
    static final int MESSAGE_FINISH_TOKEN_TIMEOUT = 7;
    static final int MESSAGE_LISTENER_HINTS_CHANGED = 5;
    static final int MESSAGE_LISTENER_NOTIFICATION_FILTER_CHANGED = 6;
    static final int MESSAGE_ON_PACKAGE_CHANGED = 8;
    private static final int MESSAGE_RANKING_SORT = 1001;
    private static final int MESSAGE_RECONSIDER_RANKING = 1000;
    static final int MESSAGE_SEND_RANKING_UPDATE = 4;
    private static final long MIN_PACKAGE_OVERRATE_LOG_INTERVAL = 5000;
    private static final long NOTIFICATION_CANCELLATION_REASONS = 175319604;
    private static final int NOTIFICATION_INSTANCE_ID_MAX = 8192;
    private static final long NOTIFICATION_LOG_ASSISTANT_CANCEL = 195579280;
    private static final long NOTIFICATION_TRAMPOLINE_BLOCK = 167676448;
    private static final long NOTIFICATION_TRAMPOLINE_BLOCK_FOR_EXEMPT_ROLES = 227752274;
    private static final long RATE_LIMIT_TOASTS = 174840628;
    public static final int REPORT_REMOTE_VIEWS = 1;
    private static final int REQUEST_CODE_TIMEOUT = 1;
    static final String REVIEW_NOTIF_ACTION_CANCELED = "REVIEW_NOTIF_ACTION_CANCELED";
    static final String REVIEW_NOTIF_ACTION_DISMISS = "REVIEW_NOTIF_ACTION_DISMISS";
    static final String REVIEW_NOTIF_ACTION_REMIND = "REVIEW_NOTIF_ACTION_REMIND";
    static final int REVIEW_NOTIF_STATE_DISMISSED = 2;
    static final int REVIEW_NOTIF_STATE_RESHOWN = 3;
    static final int REVIEW_NOTIF_STATE_SHOULD_SHOW = 0;
    static final int REVIEW_NOTIF_STATE_UNKNOWN = -1;
    static final int REVIEW_NOTIF_STATE_USER_INTERACTED = 1;
    static final String ROOT_PKG = "root";
    private static final String SCHEME_TIMEOUT = "timeout";
    static final int SHORT_DELAY = 2000;
    static final long SNOOZE_UNTIL_UNSPECIFIED = -1;
    private static final String TAG_NOTIFICATION_POLICY = "notification-policy";
    static final String TOAST_QUOTA_TAG = "toast_quota_tag";
    private static ITranLedLightExt mTranLedLightExt;
    private final boolean TRAN_NOTIFY_SCREEN_ON_SUPPORT;
    private AccessibilityManager mAccessibilityManager;
    private ActivityManager mActivityManager;
    private AlarmManager mAlarmManager;
    boolean mAllowFgsDismissal;
    private TriPredicate<String, Integer, String> mAllowedManagedServicePackages;
    private IActivityManager mAm;
    private ActivityManagerInternal mAmi;
    private AppOpsManager mAppOps;
    private IAppOpsService mAppOpsService;
    private UsageStatsManagerInternal mAppUsageStats;
    private Archive mArchive;
    private NotificationAssistants mAssistants;
    private ActivityTaskManagerInternal mAtm;
    LogicalLight mAttentionLight;
    AudioManager mAudioManager;
    AudioManagerInternal mAudioManagerInternal;
    private int mAutoGroupAtCount;
    final ArrayMap<Integer, ArrayMap<String, String>> mAutobundledSummaries;
    private Binder mCallNotificationToken;
    private int mCallState;
    private ICompanionDeviceManager mCompanionManager;
    private ConditionProviders mConditionProviders;
    private DeviceConfig.OnPropertiesChangedListener mDeviceConfigChangedListener;
    private DeviceIdleManager mDeviceIdleManager;
    private boolean mDisableNotificationEffects;
    private DevicePolicyManagerInternal mDpm;
    private List<ComponentName> mEffectsSuppressors;
    final ArrayList<NotificationRecord> mEnqueuedNotifications;
    final IBinder mForegroundToken;
    private GroupHelper mGroupHelper;
    private WorkerHandler mHandler;
    boolean mHasLight;
    private NotificationHistoryManager mHistoryManager;
    private AudioAttributes mInCallNotificationAudioAttributes;
    private Uri mInCallNotificationUri;
    private float mInCallNotificationVolume;
    protected boolean mInCallStateOffHook;
    final ArrayMap<String, InlineReplyUriRecord> mInlineReplyRecordsByKey;
    private final BroadcastReceiver mIntentReceiver;
    private final NotificationManagerInternal mInternalService;
    private int mInterruptionFilter;
    private boolean mIsAutomotive;
    private boolean mIsCurrentToastShown;
    private boolean mIsTelevision;
    private KeyguardManager mKeyguardManager;
    private long mLastOverRateLogTime;
    boolean mLightEnabled;
    ArrayList<String> mLights;
    private int mListenerHints;
    private NotificationListeners mListeners;
    private final SparseArray<ArraySet<ComponentName>> mListenersDisablingEffects;
    protected final BroadcastReceiver mLocaleChangeReceiver;
    private boolean mLockScreenAllowSecureNotifications;
    private float mMaxPackageEnqueueRate;
    private MetricsLogger mMetricsLogger;
    private Set<String> mMsgPkgsAllowedAsConvos;
    private NotificationChannelLogger mNotificationChannelLogger;
    final NotificationDelegate mNotificationDelegate;
    private boolean mNotificationEffectsEnabledForAutomotive;
    private InstanceIdSequence mNotificationInstanceIdSequence;
    private LogicalLight mNotificationLight;
    final ArrayList<NotificationRecord> mNotificationList;
    final Object mNotificationLock;
    boolean mNotificationPulseEnabled;
    private NotificationRecordLogger mNotificationRecordLogger;
    private final BroadcastReceiver mNotificationTimeoutReceiver;
    final ArrayMap<String, NotificationRecord> mNotificationsByKey;
    private final BroadcastReceiver mPackageIntentReceiver;
    private IPackageManager mPackageManager;
    private PackageManager mPackageManagerClient;
    private PackageManagerInternal mPackageManagerInternal;
    private PermissionHelper mPermissionHelper;
    private PermissionPolicyInternal mPermissionPolicyInternal;
    private IPlatformCompat mPlatformCompat;
    private AtomicFile mPolicyFile;
    PreferencesHelper mPreferencesHelper;
    private StatsPullAtomCallbackImpl mPullAtomCallback;
    protected RankingHandler mRankingHandler;
    RankingHelper mRankingHelper;
    private final HandlerThread mRankingThread;
    private final BroadcastReceiver mRestoreReceiver;
    private ReviewNotificationPermissionsReceiver mReviewNotificationPermissionsReceiver;
    private volatile RoleObserver mRoleObserver;
    private final SavePolicyFileRunnable mSavePolicyFile;
    boolean mScreenOn;
    final IBinder mService;
    private SettingsObserver mSettingsObserver;
    private ShortcutHelper mShortcutHelper;
    private ShortcutHelper.ShortcutListener mShortcutListener;
    protected boolean mShowReviewPermissionsNotification;
    protected SnoozeHelper mSnoozeHelper;
    private String mSoundNotificationKey;
    private StatsManager mStatsManager;
    StatusBarManagerInternal mStatusBar;
    private int mStripRemoteViewsSizeBytes;
    private StrongAuthTracker mStrongAuthTracker;
    final ArrayMap<String, NotificationRecord> mSummaryByGroupKey;
    boolean mSystemReady;
    private TelecomManager mTelecomManager;
    final ArrayList<ToastRecord> mToastQueue;
    private MultiRateLimiter mToastRateLimiter;
    private final Set<Integer> mToastRateLimitingDisabledUids;
    private IUriGrantsManager mUgm;
    private UriGrantsManagerInternal mUgmInternal;
    private Handler mUiHandler;
    private UserManager mUm;
    private NotificationUsageStats mUsageStats;
    private UsageStatsManagerInternal mUsageStatsManagerInternal;
    private boolean mUseAttentionLight;
    private final ManagedServices.UserProfiles mUserProfiles;
    private String mVibrateNotificationKey;
    private VibratorHelper mVibratorHelper;
    private int mWarnRemoteViewsSizeBytes;
    private WindowManagerInternal mWindowManagerInternal;
    protected ZenModeHelper mZenModeHelper;
    public static final String TAG = "NotificationService";
    public static final boolean DBG = Log.isLoggable(TAG, 3);
    public static final boolean ENABLE_CHILD_NOTIFICATIONS = SystemProperties.getBoolean("debug.child_notifs", true);
    static final boolean DEBUG_INTERRUPTIVENESS = SystemProperties.getBoolean("debug.notification.interruptiveness", false);
    static final String[] DEFAULT_ALLOWED_ADJUSTMENTS = {"key_contextual_actions", "key_text_replies", "key_not_conversation", "key_importance", "key_ranking_score"};
    static final String[] NON_BLOCKABLE_DEFAULT_ROLES = {"android.app.role.DIALER", "android.app.role.EMERGENCY"};
    private static final MultiRateLimiter.RateLimit[] TOAST_RATE_LIMITS = {MultiRateLimiter.RateLimit.create(3, Duration.ofSeconds(20)), MultiRateLimiter.RateLimit.create(5, Duration.ofSeconds(42)), MultiRateLimiter.RateLimit.create(6, Duration.ofSeconds(68))};
    private static final String ACTION_NOTIFICATION_TIMEOUT = NotificationManagerService.class.getSimpleName() + ".TIMEOUT";
    private static final int MY_UID = Process.myUid();
    private static final int MY_PID = Process.myPid();
    private static final IBinder ALLOWLIST_TOKEN = new Binder();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public interface FlagChecker {
        boolean apply(int i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Archive {
        final int mBufferSize;
        final Object mBufferLock = new Object();
        final LinkedList<Pair<StatusBarNotification, Integer>> mBuffer = new LinkedList<>();
        final SparseArray<Boolean> mEnabled = new SparseArray<>();

        public Archive(int size) {
            this.mBufferSize = size;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            int N = this.mBuffer.size();
            sb.append("Archive (");
            sb.append(N);
            sb.append(" notification");
            sb.append(N == 1 ? ")" : "s)");
            return sb.toString();
        }

        public void record(StatusBarNotification sbn, int reason) {
            if (!this.mEnabled.get(sbn.getNormalizedUserId(), false).booleanValue()) {
                return;
            }
            synchronized (this.mBufferLock) {
                if (this.mBuffer.size() == this.mBufferSize) {
                    this.mBuffer.removeFirst();
                }
                this.mBuffer.addLast(new Pair<>(sbn.cloneLight(), Integer.valueOf(reason)));
            }
        }

        public Iterator<Pair<StatusBarNotification, Integer>> descendingIterator() {
            return this.mBuffer.descendingIterator();
        }

        public StatusBarNotification[] getArray(final UserManager um, int count, boolean includeSnoozed) {
            StatusBarNotification[] statusBarNotificationArr;
            final ArrayList<Integer> currentUsers = new ArrayList<>();
            currentUsers.add(-1);
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.notification.NotificationManagerService$Archive$$ExternalSyntheticLambda0
                public final void runOrThrow() {
                    NotificationManagerService.Archive.lambda$getArray$0(um, currentUsers);
                }
            });
            synchronized (this.mBufferLock) {
                if (count == 0) {
                    count = this.mBufferSize;
                }
                List<StatusBarNotification> a = new ArrayList<>();
                Iterator<Pair<StatusBarNotification, Integer>> iter = descendingIterator();
                int i = 0;
                while (iter.hasNext() && i < count) {
                    Pair<StatusBarNotification, Integer> pair = iter.next();
                    if ((((Integer) pair.second).intValue() != 18 || includeSnoozed) && currentUsers.contains(Integer.valueOf(((StatusBarNotification) pair.first).getUserId()))) {
                        i++;
                        a.add((StatusBarNotification) pair.first);
                    }
                }
                statusBarNotificationArr = (StatusBarNotification[]) a.toArray(new StatusBarNotification[a.size()]);
            }
            return statusBarNotificationArr;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getArray$0(UserManager um, ArrayList currentUsers) throws Exception {
            int[] profileIds;
            for (int user : um.getProfileIds(ActivityManager.getCurrentUser(), false)) {
                currentUsers.add(Integer.valueOf(user));
            }
        }

        public void updateHistoryEnabled(int userId, boolean enabled) {
            this.mEnabled.put(userId, Boolean.valueOf(enabled));
            if (!enabled) {
                synchronized (this.mBufferLock) {
                    for (int i = this.mBuffer.size() - 1; i >= 0; i--) {
                        if (userId == ((StatusBarNotification) this.mBuffer.get(i).first).getNormalizedUserId()) {
                            this.mBuffer.remove(i);
                        }
                    }
                }
            }
        }

        public void removeChannelNotifications(String pkg, int userId, String channelId) {
            synchronized (this.mBufferLock) {
                Iterator<Pair<StatusBarNotification, Integer>> bufferIter = descendingIterator();
                while (bufferIter.hasNext()) {
                    Pair<StatusBarNotification, Integer> pair = bufferIter.next();
                    if (pair.first != null && userId == ((StatusBarNotification) pair.first).getNormalizedUserId() && pkg != null && pkg.equals(((StatusBarNotification) pair.first).getPackageName()) && ((StatusBarNotification) pair.first).getNotification() != null && Objects.equals(channelId, ((StatusBarNotification) pair.first).getNotification().getChannelId())) {
                        bufferIter.remove();
                    }
                }
            }
        }

        void dumpImpl(PrintWriter pw, DumpFilter filter) {
            synchronized (this.mBufferLock) {
                Iterator<Pair<StatusBarNotification, Integer>> iter = descendingIterator();
                int i = 0;
                while (true) {
                    if (!iter.hasNext()) {
                        break;
                    }
                    StatusBarNotification sbn = (StatusBarNotification) iter.next().first;
                    if (filter == null || filter.matches(sbn)) {
                        pw.println("    " + sbn);
                        i++;
                        if (i >= 5) {
                            if (iter.hasNext()) {
                                pw.println("    ...");
                            }
                        }
                    }
                }
            }
        }
    }

    void loadDefaultApprovedServices(int userId) {
        this.mListeners.loadDefaultsFromConfig();
        this.mConditionProviders.loadDefaultsFromConfig();
        this.mAssistants.loadDefaultsFromConfig();
    }

    protected void allowDefaultApprovedServices(int userId) {
        ArraySet<ComponentName> defaultListeners = this.mListeners.getDefaultComponents();
        for (int i = 0; i < defaultListeners.size(); i++) {
            ComponentName cn = defaultListeners.valueAt(i);
            allowNotificationListener(userId, cn);
        }
        ArraySet<String> defaultDnds = this.mConditionProviders.getDefaultPackages();
        for (int i2 = 0; i2 < defaultDnds.size(); i2++) {
            allowDndPackage(userId, defaultDnds.valueAt(i2));
        }
        setDefaultAssistantForUser(userId);
    }

    protected void migrateDefaultNAS() {
        List<UserInfo> activeUsers = this.mUm.getUsers();
        for (UserInfo userInfo : activeUsers) {
            int userId = userInfo.getUserHandle().getIdentifier();
            if (!isNASMigrationDone(userId) && !this.mUm.isManagedProfile(userId)) {
                List<ComponentName> allowedComponents = this.mAssistants.getAllowedComponents(userId);
                if (allowedComponents.size() == 0) {
                    Slog.d(TAG, "NAS Migration: user set to none, disable new NAS setting");
                    setNASMigrationDone(userId);
                    this.mAssistants.clearDefaults();
                } else {
                    Slog.d(TAG, "Reset NAS setting and migrate to new default");
                    resetAssistantUserSet(userId);
                    this.mAssistants.resetDefaultAssistantsIfNecessary();
                }
            }
        }
    }

    void setNASMigrationDone(int baseUserId) {
        int[] profileIds;
        for (int profileId : this.mUm.getProfileIds(baseUserId, false)) {
            Settings.Secure.putIntForUser(getContext().getContentResolver(), "nas_settings_updated", 1, profileId);
        }
    }

    boolean isNASMigrationDone(int userId) {
        return Settings.Secure.getIntForUser(getContext().getContentResolver(), "nas_settings_updated", 0, userId) == 1;
    }

    protected void setDefaultAssistantForUser(int userId) {
        String overrideDefaultAssistantString = DeviceConfig.getProperty("systemui", "nas_default_service");
        if (overrideDefaultAssistantString != null) {
            ArraySet<ComponentName> approved = this.mAssistants.queryPackageForServices(overrideDefaultAssistantString, 786432, userId);
            for (int i = 0; i < approved.size(); i++) {
                if (allowAssistant(userId, approved.valueAt(i))) {
                    return;
                }
            }
        }
        ArraySet<ComponentName> defaults = this.mAssistants.getDefaultComponents();
        for (int i2 = 0; i2 < defaults.size(); i2++) {
            ComponentName cn = defaults.valueAt(i2);
            if (allowAssistant(userId, cn)) {
                return;
            }
        }
    }

    protected void updateAutobundledSummaryFlags(int userId, String pkg, boolean needsOngoingFlag, boolean isAppForeground) {
        String summaryKey;
        NotificationRecord summary;
        ArrayMap<String, String> summaries = this.mAutobundledSummaries.get(Integer.valueOf(userId));
        if (summaries == null || (summaryKey = summaries.get(pkg)) == null || (summary = this.mNotificationsByKey.get(summaryKey)) == null) {
            return;
        }
        int oldFlags = summary.getSbn().getNotification().flags;
        if (needsOngoingFlag) {
            summary.getSbn().getNotification().flags |= 2;
        } else {
            summary.getSbn().getNotification().flags &= -3;
        }
        if (summary.getSbn().getNotification().flags != oldFlags) {
            this.mHandler.post(new EnqueueNotificationRunnable(userId, summary, isAppForeground, SystemClock.elapsedRealtime()));
        }
    }

    private void allowDndPackage(int userId, String packageName) {
        try {
            getBinderService().setNotificationPolicyAccessGrantedForUser(packageName, userId, true);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void allowNotificationListener(int userId, ComponentName cn) {
        try {
            getBinderService().setNotificationListenerAccessGrantedForUser(cn, userId, true, true);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private boolean allowAssistant(int userId, ComponentName candidate) {
        Set<ComponentName> validAssistants = this.mAssistants.queryPackageForServices(null, 786432, userId);
        if (candidate == null || !validAssistants.contains(candidate)) {
            return false;
        }
        setNotificationAssistantAccessGrantedForUserInternal(candidate, userId, true, false);
        return true;
    }

    /* JADX WARN: Removed duplicated region for block: B:51:0x00d4 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:62:0x002d A[ADDED_TO_REGION, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void readPolicyXml(InputStream stream, boolean forRestore, int userId) throws XmlPullParserException, NumberFormatException, IOException {
        TypedXmlPullParser parser;
        if (forRestore) {
            parser = Xml.newFastPullParser();
            parser.setInput(stream, StandardCharsets.UTF_8.name());
        } else {
            parser = Xml.resolvePullParser(stream);
        }
        XmlUtils.beginDocument(parser, TAG_NOTIFICATION_POLICY);
        boolean migratedManagedServices = false;
        boolean ineligibleForManagedServices = forRestore && this.mUm.isManagedProfile(userId);
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if ("zen".equals(parser.getName())) {
                this.mZenModeHelper.readXml(parser, forRestore, userId);
            } else if ("ranking".equals(parser.getName())) {
                this.mPreferencesHelper.readXml(parser, forRestore, userId);
            }
            if (this.mListeners.getConfig().xmlTag.equals(parser.getName())) {
                if (!ineligibleForManagedServices) {
                    this.mListeners.readXml(parser, this.mAllowedManagedServicePackages, forRestore, userId);
                    migratedManagedServices = true;
                    if (!LOCKSCREEN_ALLOW_SECURE_NOTIFICATIONS_TAG.equals(parser.getName()) && (!forRestore || userId == 0)) {
                        this.mLockScreenAllowSecureNotifications = parser.getAttributeBoolean((String) null, LOCKSCREEN_ALLOW_SECURE_NOTIFICATIONS_VALUE, true);
                    }
                }
            } else if (this.mAssistants.getConfig().xmlTag.equals(parser.getName())) {
                if (!ineligibleForManagedServices) {
                    this.mAssistants.readXml(parser, this.mAllowedManagedServicePackages, forRestore, userId);
                    migratedManagedServices = true;
                    if (!LOCKSCREEN_ALLOW_SECURE_NOTIFICATIONS_TAG.equals(parser.getName())) {
                        this.mLockScreenAllowSecureNotifications = parser.getAttributeBoolean((String) null, LOCKSCREEN_ALLOW_SECURE_NOTIFICATIONS_VALUE, true);
                    }
                }
            } else {
                if (this.mConditionProviders.getConfig().xmlTag.equals(parser.getName())) {
                    if (!ineligibleForManagedServices) {
                        this.mConditionProviders.readXml(parser, this.mAllowedManagedServicePackages, forRestore, userId);
                        migratedManagedServices = true;
                    }
                } else if ("snoozed-notifications".equals(parser.getName())) {
                    this.mSnoozeHelper.readXml(parser, System.currentTimeMillis());
                }
                if (!LOCKSCREEN_ALLOW_SECURE_NOTIFICATIONS_TAG.equals(parser.getName())) {
                }
            }
        }
        if (!migratedManagedServices) {
            this.mListeners.migrateToXml();
            this.mAssistants.migrateToXml();
            this.mConditionProviders.migrateToXml();
            handleSavePolicyFile();
        }
        this.mAssistants.resetDefaultAssistantsIfNecessary();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1033=4] */
    protected void loadPolicyFile() {
        if (DBG) {
            Slog.d(TAG, "loadPolicyFile");
        }
        synchronized (this.mPolicyFile) {
            InputStream infile = null;
            try {
                infile = this.mPolicyFile.openRead();
                readPolicyXml(infile, false, -1);
            } catch (FileNotFoundException e) {
                loadDefaultApprovedServices(0);
                allowDefaultApprovedServices(0);
                IoUtils.closeQuietly(infile);
            } catch (IOException e2) {
                Log.wtf(TAG, "Unable to read notification policy", e2);
            } catch (NumberFormatException e3) {
                Log.wtf(TAG, "Unable to parse notification policy", e3);
            } catch (XmlPullParserException e4) {
                Log.wtf(TAG, "Unable to parse notification policy", e4);
            }
            IoUtils.closeQuietly(infile);
        }
    }

    protected void handleSavePolicyFile() {
        if (!IoThread.getHandler().hasCallbacks(this.mSavePolicyFile)) {
            IoThread.getHandler().postDelayed(this.mSavePolicyFile, 250L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class SavePolicyFileRunnable implements Runnable {
        private SavePolicyFileRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            if (NotificationManagerService.DBG) {
                Slog.d(NotificationManagerService.TAG, "handleSavePolicyFile");
            }
            synchronized (NotificationManagerService.this.mPolicyFile) {
                try {
                    FileOutputStream stream = NotificationManagerService.this.mPolicyFile.startWrite();
                    try {
                        NotificationManagerService.this.writePolicyXml(stream, false, -1);
                        NotificationManagerService.this.mPolicyFile.finishWrite(stream);
                    } catch (IOException e) {
                        Slog.w(NotificationManagerService.TAG, "Failed to save policy file, restoring backup", e);
                        NotificationManagerService.this.mPolicyFile.failWrite(stream);
                    }
                } catch (IOException e2) {
                    Slog.w(NotificationManagerService.TAG, "Failed to save policy file", e2);
                    return;
                }
            }
            BackupManager.dataChanged(NotificationManagerService.this.getContext().getPackageName());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writePolicyXml(OutputStream stream, boolean forBackup, int userId) throws IOException {
        TypedXmlSerializer out;
        if (forBackup) {
            out = Xml.newFastSerializer();
            out.setOutput(stream, StandardCharsets.UTF_8.name());
        } else {
            out = Xml.resolveSerializer(stream);
        }
        out.startDocument((String) null, true);
        out.startTag((String) null, TAG_NOTIFICATION_POLICY);
        out.attributeInt((String) null, ATTR_VERSION, 1);
        this.mZenModeHelper.writeXml(out, forBackup, null, userId);
        this.mPreferencesHelper.writeXml(out, forBackup, userId);
        this.mListeners.writeXml(out, forBackup, userId);
        this.mAssistants.writeXml(out, forBackup, userId);
        this.mSnoozeHelper.writeXml(out);
        this.mConditionProviders.writeXml(out, forBackup, userId);
        if (!forBackup || userId == 0) {
            writeSecureNotificationsPolicy(out);
        }
        out.endTag((String) null, TAG_NOTIFICATION_POLICY);
        out.endDocument();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.notification.NotificationManagerService$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass1 implements NotificationDelegate {
        AnonymousClass1() {
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void prepareForPossibleShutdown() {
            NotificationManagerService.this.mHistoryManager.triggerWriteToDisk();
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onSetDisabled(int status) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationManagerService.this.mDisableNotificationEffects = (262144 & status) != 0;
                if (NotificationManagerService.this.disableNotificationEffects(null) != null) {
                    NotificationManagerService.this.clearSoundLocked();
                    NotificationManagerService.this.clearVibrateLocked();
                }
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onClearAll(int callingUid, int callingPid, int userId) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationManagerService.this.cancelAllLocked(callingUid, callingPid, userId, 3, null, true);
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onNotificationClick(int callingUid, int callingPid, String key, NotificationVisibility nv) {
            NotificationManagerService.this.exitIdle();
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r == null) {
                    Slog.w(NotificationManagerService.TAG, "No notification with key: " + key);
                    return;
                }
                long now = System.currentTimeMillis();
                MetricsLogger.action(r.getItemLogMaker().setType(4).addTaggedData(798, Integer.valueOf(nv.rank)).addTaggedData(1395, Integer.valueOf(nv.count)));
                NotificationManagerService.this.mNotificationRecordLogger.log(NotificationRecordLogger.NotificationEvent.NOTIFICATION_CLICKED, r);
                EventLogTags.writeNotificationClicked(key, r.getLifespanMs(now), r.getFreshnessMs(now), r.getExposureMs(now), nv.rank, nv.count);
                StatusBarNotification sbn = r.getSbn();
                NotificationManagerService.this.cancelNotification(callingUid, callingPid, sbn.getPackageName(), sbn.getTag(), sbn.getId(), 16, 4160, false, r.getUserId(), 1, nv.rank, nv.count, null);
                nv.recycle();
                NotificationManagerService.this.reportUserInteraction(r);
                NotificationManagerService.this.mAssistants.notifyAssistantNotificationClicked(r);
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onNotificationActionClick(int callingUid, int callingPid, String key, int actionIndex, Notification.Action action, NotificationVisibility nv, boolean generatedByAssistant) {
            NotificationManagerService.this.exitIdle();
            synchronized (NotificationManagerService.this.mNotificationLock) {
                try {
                    try {
                        NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                        if (r == null) {
                            Slog.w(NotificationManagerService.TAG, "No notification with key: " + key);
                            return;
                        }
                        long now = System.currentTimeMillis();
                        int i = 1;
                        LogMaker addTaggedData = r.getLogMaker(now).setCategory(129).setType(4).setSubtype(actionIndex).addTaggedData(798, Integer.valueOf(nv.rank)).addTaggedData(1395, Integer.valueOf(nv.count)).addTaggedData(1601, Integer.valueOf(action.isContextual() ? 1 : 0));
                        if (!generatedByAssistant) {
                            i = 0;
                        }
                        MetricsLogger.action(addTaggedData.addTaggedData(1600, Integer.valueOf(i)).addTaggedData(1629, Integer.valueOf(nv.location.toMetricsEventEnum())));
                        NotificationManagerService.this.mNotificationRecordLogger.log(NotificationRecordLogger.NotificationEvent.fromAction(actionIndex, generatedByAssistant, action.isContextual()), r);
                        EventLogTags.writeNotificationActionClicked(key, actionIndex, r.getLifespanMs(now), r.getFreshnessMs(now), r.getExposureMs(now), nv.rank, nv.count);
                        nv.recycle();
                        NotificationManagerService.this.reportUserInteraction(r);
                        NotificationManagerService.this.mAssistants.notifyAssistantActionClicked(r, action, generatedByAssistant);
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onNotificationClear(int callingUid, int callingPid, String pkg, int userId, String key, int dismissalSurface, int dismissalSentiment, NotificationVisibility nv) {
            String tag = null;
            int id = 0;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                try {
                    try {
                        try {
                            NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                            if (r != null) {
                                try {
                                    r.recordDismissalSurface(dismissalSurface);
                                    r.recordDismissalSentiment(dismissalSentiment);
                                    tag = r.getSbn().getTag();
                                    id = r.getSbn().getId();
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            }
                            NotificationManagerService.this.cancelNotification(callingUid, callingPid, pkg, tag, id, 0, 2, true, userId, 2, nv.rank, nv.count, null);
                            nv.recycle();
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onPanelRevealed(boolean clearEffects, int items) {
            MetricsLogger.visible(NotificationManagerService.this.getContext(), (int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_AUTO_TIME);
            MetricsLogger.histogram(NotificationManagerService.this.getContext(), "note_load", items);
            NotificationManagerService.this.mNotificationRecordLogger.log(NotificationRecordLogger.NotificationPanelEvent.NOTIFICATION_PANEL_OPEN);
            EventLogTags.writeNotificationPanelRevealed(items);
            if (clearEffects) {
                clearEffects();
            }
            NotificationManagerService.this.mAssistants.onPanelRevealed(items);
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onPanelHidden() {
            MetricsLogger.hidden(NotificationManagerService.this.getContext(), (int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_AUTO_TIME);
            NotificationManagerService.this.mNotificationRecordLogger.log(NotificationRecordLogger.NotificationPanelEvent.NOTIFICATION_PANEL_CLOSE);
            EventLogTags.writeNotificationPanelHidden();
            NotificationManagerService.this.mAssistants.onPanelHidden();
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void clearEffects() {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                if (NotificationManagerService.DBG) {
                    Slog.d(NotificationManagerService.TAG, "clearEffects");
                }
                NotificationManagerService.this.clearSoundLocked();
                NotificationManagerService.this.clearVibrateLocked();
                NotificationManagerService.this.clearLightsLocked();
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onNotificationError(int callingUid, int callingPid, final String pkg, final String tag, final int id, final int uid, final int initialPid, final String message, int userId) {
            boolean fgService;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = NotificationManagerService.this.findNotificationLocked(pkg, tag, id, userId);
                fgService = (r == null || (r.getNotification().flags & 64) == 0) ? false : true;
            }
            NotificationManagerService.this.cancelNotification(callingUid, callingPid, pkg, tag, id, 0, 0, false, userId, 4, null);
            if (fgService) {
                Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.notification.NotificationManagerService$1$$ExternalSyntheticLambda0
                    public final void runOrThrow() {
                        NotificationManagerService.AnonymousClass1.this.m5140xd9844f99(uid, initialPid, pkg, tag, id, message);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onNotificationError$0$com-android-server-notification-NotificationManagerService$1  reason: not valid java name */
        public /* synthetic */ void m5140xd9844f99(int uid, int initialPid, String pkg, String tag, int id, String message) throws Exception {
            NotificationManagerService.this.mAm.crashApplicationWithType(uid, initialPid, pkg, -1, "Bad notification(tag=" + tag + ", id=" + id + ") posted from package " + pkg + ", crashing app(uid=" + uid + ", pid=" + initialPid + "): " + message, true, 4);
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onNotificationVisibilityChanged(NotificationVisibility[] newlyVisibleKeys, NotificationVisibility[] noLongerVisibleKeys) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                for (NotificationVisibility nv : newlyVisibleKeys) {
                    NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(nv.key);
                    if (r != null) {
                        if (!r.isSeen()) {
                            if (NotificationManagerService.DBG) {
                                Slog.d(NotificationManagerService.TAG, "Marking notification as visible " + nv.key);
                            }
                            NotificationManagerService.this.reportSeen(r);
                        }
                        boolean z = true;
                        r.setVisibility(true, nv.rank, nv.count, NotificationManagerService.this.mNotificationRecordLogger);
                        NotificationManagerService.this.mAssistants.notifyAssistantVisibilityChangedLocked(r, true);
                        if (nv.location != NotificationVisibility.NotificationLocation.LOCATION_FIRST_HEADS_UP) {
                            z = false;
                        }
                        boolean isHun = z;
                        if (isHun || r.hasBeenVisiblyExpanded()) {
                            NotificationManagerService.this.logSmartSuggestionsVisible(r, nv.location.toMetricsEventEnum());
                        }
                        NotificationManagerService.this.maybeRecordInterruptionLocked(r);
                        nv.recycle();
                    }
                }
                for (NotificationVisibility nv2 : noLongerVisibleKeys) {
                    NotificationRecord r2 = NotificationManagerService.this.mNotificationsByKey.get(nv2.key);
                    if (r2 != null) {
                        r2.setVisibility(false, nv2.rank, nv2.count, NotificationManagerService.this.mNotificationRecordLogger);
                        NotificationManagerService.this.mAssistants.notifyAssistantVisibilityChangedLocked(r2, false);
                        nv2.recycle();
                    }
                }
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onNotificationExpansionChanged(String key, boolean userAction, boolean expanded, int notificationLocation) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r != null) {
                    r.stats.onExpansionChanged(userAction, expanded);
                    if (r.hasBeenVisiblyExpanded()) {
                        NotificationManagerService.this.logSmartSuggestionsVisible(r, notificationLocation);
                    }
                    if (userAction) {
                        MetricsLogger.action(r.getItemLogMaker().setType(expanded ? 3 : 14));
                        NotificationManagerService.this.mNotificationRecordLogger.log(NotificationRecordLogger.NotificationEvent.fromExpanded(expanded, userAction), r);
                    }
                    if (expanded && userAction) {
                        r.recordExpanded();
                        NotificationManagerService.this.reportUserInteraction(r);
                    }
                    NotificationManagerService.this.mAssistants.notifyAssistantExpansionChangedLocked(r.getSbn(), r.getNotificationType(), userAction, expanded);
                }
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onNotificationDirectReplied(String key) {
            NotificationManagerService.this.exitIdle();
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r != null) {
                    r.recordDirectReplied();
                    NotificationManagerService.this.mMetricsLogger.write(r.getLogMaker().setCategory(1590).setType(4));
                    NotificationManagerService.this.mNotificationRecordLogger.log(NotificationRecordLogger.NotificationEvent.NOTIFICATION_DIRECT_REPLIED, r);
                    NotificationManagerService.this.reportUserInteraction(r);
                    NotificationManagerService.this.mAssistants.notifyAssistantNotificationDirectReplyLocked(r);
                }
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onNotificationSmartSuggestionsAdded(String key, int smartReplyCount, int smartActionCount, boolean generatedByAssistant, boolean editBeforeSending) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r != null) {
                    r.setNumSmartRepliesAdded(smartReplyCount);
                    r.setNumSmartActionsAdded(smartActionCount);
                    r.setSuggestionsGeneratedByAssistant(generatedByAssistant);
                    r.setEditChoicesBeforeSending(editBeforeSending);
                }
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onNotificationSmartReplySent(String key, int replyIndex, CharSequence reply, int notificationLocation, boolean modifiedBeforeSending) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r != null) {
                    int i = 1;
                    LogMaker addTaggedData = r.getLogMaker().setCategory(1383).setSubtype(replyIndex).addTaggedData(1600, Integer.valueOf(r.getSuggestionsGeneratedByAssistant() ? 1 : 0)).addTaggedData(1629, Integer.valueOf(notificationLocation)).addTaggedData(1647, Integer.valueOf(r.getEditChoicesBeforeSending() ? 1 : 0));
                    if (!modifiedBeforeSending) {
                        i = 0;
                    }
                    LogMaker logMaker = addTaggedData.addTaggedData(1648, Integer.valueOf(i));
                    NotificationManagerService.this.mMetricsLogger.write(logMaker);
                    NotificationManagerService.this.mNotificationRecordLogger.log(NotificationRecordLogger.NotificationEvent.NOTIFICATION_SMART_REPLIED, r);
                    NotificationManagerService.this.reportUserInteraction(r);
                    NotificationManagerService.this.mAssistants.notifyAssistantSuggestedReplySent(r.getSbn(), r.getNotificationType(), reply, r.getSuggestionsGeneratedByAssistant());
                }
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onNotificationSettingsViewed(String key) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r != null) {
                    r.recordViewedSettings();
                }
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onNotificationBubbleChanged(String key, boolean isBubble, int bubbleFlags) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r != null) {
                    if (!isBubble) {
                        r.getNotification().flags &= -4097;
                        r.setFlagBubbleRemoved(true);
                    } else {
                        r.getNotification().flags |= 8;
                        r.setFlagBubbleRemoved(false);
                        if (r.getNotification().getBubbleMetadata() != null) {
                            r.getNotification().getBubbleMetadata().setFlags(bubbleFlags);
                        }
                        NotificationManagerService.this.mHandler.post(new EnqueueNotificationRunnable(r.getUser().getIdentifier(), r, true, SystemClock.elapsedRealtime()));
                    }
                }
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onBubbleMetadataFlagChanged(String key, int flags) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r != null) {
                    Notification.BubbleMetadata data = r.getNotification().getBubbleMetadata();
                    if (data == null) {
                        return;
                    }
                    if (flags != data.getFlags()) {
                        data.setFlags(flags);
                        r.getNotification().flags |= 8;
                        NotificationManagerService.this.mHandler.post(new EnqueueNotificationRunnable(r.getUser().getIdentifier(), r, true, SystemClock.elapsedRealtime()));
                    }
                }
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void grantInlineReplyUriPermission(String key, Uri uri, UserHandle user, String packageName, int callingUid) {
            InlineReplyUriRecord newRecord;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                try {
                    InlineReplyUriRecord r = NotificationManagerService.this.mInlineReplyRecordsByKey.get(key);
                    if (r == null) {
                        try {
                            InlineReplyUriRecord newRecord2 = new InlineReplyUriRecord(NotificationManagerService.this.mUgmInternal.newUriPermissionOwner("INLINE_REPLY:" + key), user, packageName, key);
                            NotificationManagerService.this.mInlineReplyRecordsByKey.put(key, newRecord2);
                            newRecord = newRecord2;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } else {
                        newRecord = r;
                    }
                    IBinder owner = newRecord.getPermissionOwner();
                    int uid = callingUid;
                    int userId = newRecord.getUserId();
                    if (UserHandle.getUserId(uid) != userId) {
                        try {
                            String[] pkgs = NotificationManagerService.this.mPackageManager.getPackagesForUid(callingUid);
                            if (pkgs == null) {
                                Log.e(NotificationManagerService.TAG, "Cannot grant uri permission to unknown UID: " + callingUid);
                            }
                            String pkg = pkgs[0];
                            uid = NotificationManagerService.this.mPackageManager.getPackageUid(pkg, 0L, userId);
                        } catch (RemoteException re) {
                            Log.e(NotificationManagerService.TAG, "Cannot talk to package manager", re);
                        }
                    }
                    newRecord.addUri(uri);
                    NotificationManagerService.this.grantUriPermission(owner, uri, uid, newRecord.getPackageName(), userId);
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void clearInlineReplyUriPermissions(String key, int callingUid) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                InlineReplyUriRecord uriRecord = NotificationManagerService.this.mInlineReplyRecordsByKey.get(key);
                if (uriRecord != null) {
                    NotificationManagerService.this.destroyPermissionOwner(uriRecord.getPermissionOwner(), uriRecord.getUserId(), "INLINE_REPLY: " + uriRecord.getKey());
                    NotificationManagerService.this.mInlineReplyRecordsByKey.remove(key);
                }
            }
        }

        @Override // com.android.server.notification.NotificationDelegate
        public void onNotificationFeedbackReceived(String key, Bundle feedback) {
            NotificationManagerService.this.exitIdle();
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r == null) {
                    if (NotificationManagerService.DBG) {
                        Slog.w(NotificationManagerService.TAG, "No notification with key: " + key);
                    }
                    return;
                }
                NotificationManagerService.this.mAssistants.notifyAssistantFeedbackReceived(r, feedback);
            }
        }
    }

    void logSmartSuggestionsVisible(NotificationRecord r, int notificationLocation) {
        if ((r.getNumSmartRepliesAdded() > 0 || r.getNumSmartActionsAdded() > 0) && !r.hasSeenSmartReplies()) {
            r.setSeenSmartReplies(true);
            LogMaker logMaker = r.getLogMaker().setCategory(1382).addTaggedData(1384, Integer.valueOf(r.getNumSmartRepliesAdded())).addTaggedData((int) AudioChannelMask.OUT_7POINT1, Integer.valueOf(r.getNumSmartActionsAdded())).addTaggedData(1600, Integer.valueOf(r.getSuggestionsGeneratedByAssistant() ? 1 : 0)).addTaggedData(1629, Integer.valueOf(notificationLocation)).addTaggedData(1647, Integer.valueOf(r.getEditChoicesBeforeSending() ? 1 : 0));
            this.mMetricsLogger.write(logMaker);
            this.mNotificationRecordLogger.log(NotificationRecordLogger.NotificationEvent.NOTIFICATION_SMART_REPLY_VISIBLE, r);
        }
    }

    void clearSoundLocked() {
        this.mSoundNotificationKey = null;
        long identity = Binder.clearCallingIdentity();
        try {
            IRingtonePlayer player = this.mAudioManager.getRingtonePlayer();
            if (player != null) {
                player.stopAsync();
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
        Binder.restoreCallingIdentity(identity);
    }

    void clearVibrateLocked() {
        this.mVibrateNotificationKey = null;
        long identity = Binder.clearCallingIdentity();
        try {
            this.mVibratorHelper.cancelVibration();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearLightsLocked() {
        this.mLights.clear();
        updateLightsLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class SettingsObserver extends ContentObserver {
        private final Uri LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS;
        private final Uri LOCK_SCREEN_SHOW_NOTIFICATIONS;
        private final Uri NOTIFICATION_BADGING_URI;
        private final Uri NOTIFICATION_BUBBLES_URI;
        private final Uri NOTIFICATION_HISTORY_ENABLED;
        private final Uri NOTIFICATION_LIGHT_PULSE_URI;
        private final Uri NOTIFICATION_RATE_LIMIT_URI;
        private final Uri NOTIFICATION_SHOW_MEDIA_ON_QUICK_SETTINGS_URI;

        SettingsObserver(Handler handler) {
            super(handler);
            this.NOTIFICATION_BADGING_URI = Settings.Secure.getUriFor("notification_badging");
            this.NOTIFICATION_BUBBLES_URI = Settings.Secure.getUriFor("notification_bubbles");
            this.NOTIFICATION_LIGHT_PULSE_URI = Settings.System.getUriFor("notification_light_pulse");
            this.NOTIFICATION_RATE_LIMIT_URI = Settings.Global.getUriFor("max_notification_enqueue_rate");
            this.NOTIFICATION_HISTORY_ENABLED = Settings.Secure.getUriFor("notification_history_enabled");
            this.NOTIFICATION_SHOW_MEDIA_ON_QUICK_SETTINGS_URI = Settings.Global.getUriFor("qs_media_controls");
            this.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS = Settings.Secure.getUriFor("lock_screen_allow_private_notifications");
            this.LOCK_SCREEN_SHOW_NOTIFICATIONS = Settings.Secure.getUriFor("lock_screen_show_notifications");
        }

        void observe() {
            ContentResolver resolver = NotificationManagerService.this.getContext().getContentResolver();
            resolver.registerContentObserver(this.NOTIFICATION_BADGING_URI, false, this, -1);
            resolver.registerContentObserver(this.NOTIFICATION_LIGHT_PULSE_URI, false, this, -1);
            resolver.registerContentObserver(this.NOTIFICATION_RATE_LIMIT_URI, false, this, -1);
            resolver.registerContentObserver(this.NOTIFICATION_BUBBLES_URI, false, this, -1);
            resolver.registerContentObserver(this.NOTIFICATION_HISTORY_ENABLED, false, this, -1);
            resolver.registerContentObserver(this.NOTIFICATION_SHOW_MEDIA_ON_QUICK_SETTINGS_URI, false, this, -1);
            resolver.registerContentObserver(this.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, false, this, -1);
            resolver.registerContentObserver(this.LOCK_SCREEN_SHOW_NOTIFICATIONS, false, this, -1);
            update(null);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            update(uri);
        }

        public void update(Uri uri) {
            ContentResolver resolver = NotificationManagerService.this.getContext().getContentResolver();
            if (uri == null || this.NOTIFICATION_LIGHT_PULSE_URI.equals(uri)) {
                boolean pulseEnabled = Settings.System.getIntForUser(resolver, "notification_light_pulse", 0, -2) != 0;
                if (NotificationManagerService.this.mNotificationPulseEnabled != pulseEnabled) {
                    NotificationManagerService.this.mNotificationPulseEnabled = pulseEnabled;
                    NotificationManagerService.this.updateNotificationPulse();
                }
            }
            if (uri == null || this.NOTIFICATION_RATE_LIMIT_URI.equals(uri)) {
                NotificationManagerService notificationManagerService = NotificationManagerService.this;
                notificationManagerService.mMaxPackageEnqueueRate = Settings.Global.getFloat(resolver, "max_notification_enqueue_rate", notificationManagerService.mMaxPackageEnqueueRate);
            }
            if (uri == null || this.NOTIFICATION_BADGING_URI.equals(uri)) {
                NotificationManagerService.this.mPreferencesHelper.updateBadgingEnabled();
            }
            if (uri == null || this.NOTIFICATION_BUBBLES_URI.equals(uri)) {
                NotificationManagerService.this.mPreferencesHelper.updateBubblesEnabled();
            }
            if (uri == null || this.NOTIFICATION_HISTORY_ENABLED.equals(uri)) {
                IntArray userIds = NotificationManagerService.this.mUserProfiles.getCurrentProfileIds();
                for (int i = 0; i < userIds.size(); i++) {
                    NotificationManagerService.this.mArchive.updateHistoryEnabled(userIds.get(i), Settings.Secure.getIntForUser(resolver, "notification_history_enabled", 0, userIds.get(i)) == 1);
                }
            }
            if (uri == null || this.NOTIFICATION_SHOW_MEDIA_ON_QUICK_SETTINGS_URI.equals(uri)) {
                NotificationManagerService.this.mPreferencesHelper.updateMediaNotificationFilteringEnabled();
            }
            if (uri == null || this.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS.equals(uri)) {
                NotificationManagerService.this.mPreferencesHelper.updateLockScreenPrivateNotifications();
            }
            if (uri == null || this.LOCK_SCREEN_SHOW_NOTIFICATIONS.equals(uri)) {
                NotificationManagerService.this.mPreferencesHelper.updateLockScreenShowNotifications();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public class StrongAuthTracker extends LockPatternUtils.StrongAuthTracker {
        boolean mIsInLockDownMode;
        SparseBooleanArray mUserInLockDownMode;

        StrongAuthTracker(Context context) {
            super(context);
            this.mUserInLockDownMode = new SparseBooleanArray();
            this.mIsInLockDownMode = false;
        }

        private boolean containsFlag(int haystack, int needle) {
            return (haystack & needle) != 0;
        }

        public boolean isInLockDownMode(int userId) {
            return this.mUserInLockDownMode.get(userId, false);
        }

        public synchronized void onStrongAuthRequiredChanged(int userId) {
            boolean userInLockDownModeNext = containsFlag(getStrongAuthForUser(userId), 32);
            if (userInLockDownModeNext == isInLockDownMode(userId)) {
                return;
            }
            if (userInLockDownModeNext) {
                NotificationManagerService.this.cancelNotificationsWhenEnterLockDownMode(userId);
            }
            this.mUserInLockDownMode.put(userId, userInLockDownModeNext);
            if (!userInLockDownModeNext) {
                NotificationManagerService.this.postNotificationsWhenExitLockDownMode(userId);
            }
        }
    }

    public NotificationManagerService(Context context) {
        this(context, new NotificationRecordLoggerImpl(), new InstanceIdSequence(8192));
    }

    public NotificationManagerService(Context context, NotificationRecordLogger notificationRecordLogger, InstanceIdSequence notificationInstanceIdSequence) {
        super(context);
        this.mForegroundToken = new Binder();
        this.mRankingThread = new HandlerThread("ranker", 10);
        this.mHasLight = true;
        this.mListenersDisablingEffects = new SparseArray<>();
        this.mEffectsSuppressors = new ArrayList();
        this.mInterruptionFilter = 0;
        this.mScreenOn = true;
        this.mInCallStateOffHook = false;
        this.mCallNotificationToken = null;
        this.mNotificationLock = new Object();
        this.mNotificationList = new ArrayList<>();
        this.mNotificationsByKey = new ArrayMap<>();
        this.mInlineReplyRecordsByKey = new ArrayMap<>();
        this.mEnqueuedNotifications = new ArrayList<>();
        this.mAutobundledSummaries = new ArrayMap<>();
        this.mToastQueue = new ArrayList<>();
        this.mToastRateLimitingDisabledUids = new ArraySet();
        this.mSummaryByGroupKey = new ArrayMap<>();
        this.mIsCurrentToastShown = false;
        this.mLights = new ArrayList<>();
        this.mUserProfiles = new ManagedServices.UserProfiles();
        this.mLockScreenAllowSecureNotifications = true;
        this.mAllowFgsDismissal = false;
        this.mMaxPackageEnqueueRate = DEFAULT_MAX_NOTIFICATION_ENQUEUE_RATE;
        this.mSavePolicyFile = new SavePolicyFileRunnable();
        this.mMsgPkgsAllowedAsConvos = new HashSet();
        this.TRAN_NOTIFY_SCREEN_ON_SUPPORT = "1".equals(SystemProperties.get("ro.transsion.notify.screenon", "0"));
        this.mNotificationDelegate = new AnonymousClass1();
        this.mLocaleChangeReceiver = new BroadcastReceiver() { // from class: com.android.server.notification.NotificationManagerService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                    SystemNotificationChannels.createAll(context2);
                    NotificationManagerService.this.mZenModeHelper.updateDefaultZenRules();
                    NotificationManagerService.this.mPreferencesHelper.onLocaleChanged(context2, ActivityManager.getCurrentUser());
                }
            }
        };
        this.mRestoreReceiver = new BroadcastReceiver() { // from class: com.android.server.notification.NotificationManagerService.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if ("android.os.action.SETTING_RESTORED".equals(intent.getAction())) {
                    try {
                        String element = intent.getStringExtra("setting_name");
                        String newValue = intent.getStringExtra("new_value");
                        int restoredFromSdkInt = intent.getIntExtra("restored_from_sdk_int", 0);
                        NotificationManagerService.this.mListeners.onSettingRestored(element, newValue, restoredFromSdkInt, getSendingUserId());
                        NotificationManagerService.this.mConditionProviders.onSettingRestored(element, newValue, restoredFromSdkInt, getSendingUserId());
                    } catch (Exception e) {
                        Slog.wtf(NotificationManagerService.TAG, "Cannot restore managed services from settings", e);
                    }
                }
            }
        };
        this.mNotificationTimeoutReceiver = new BroadcastReceiver() { // from class: com.android.server.notification.NotificationManagerService.4
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action != null && NotificationManagerService.ACTION_NOTIFICATION_TIMEOUT.equals(action)) {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        try {
                            try {
                                NotificationRecord record = NotificationManagerService.this.findNotificationByKeyLocked(intent.getStringExtra(NotificationManagerService.EXTRA_KEY));
                                if (record != null) {
                                    NotificationManagerService.this.cancelNotification(record.getSbn().getUid(), record.getSbn().getInitialPid(), record.getSbn().getPackageName(), record.getSbn().getTag(), record.getSbn().getId(), 0, 64, true, record.getUserId(), 19, null);
                                }
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                }
            }
        };
        this.mPackageIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.notification.NotificationManagerService.5
            /* JADX DEBUG: Multi-variable search result rejected for r16v2, resolved type: boolean */
            /* JADX WARN: Multi-variable type inference failed */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                boolean packageChanged;
                boolean packageChanged2;
                boolean queryRemove;
                boolean removingPackage;
                String pkgName;
                String[] pkgList;
                int i;
                int[] uidList;
                boolean removingPackage2;
                int changeUserId;
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                boolean queryRestart = false;
                boolean packageChanged3 = false;
                boolean cancelNotifications = true;
                boolean hideNotifications = false;
                boolean unhideNotifications = false;
                if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                    packageChanged = false;
                    packageChanged2 = false;
                    queryRemove = false;
                } else {
                    boolean queryRemove2 = action.equals("android.intent.action.PACKAGE_REMOVED");
                    if (!queryRemove2 && !action.equals("android.intent.action.PACKAGE_RESTARTED")) {
                        boolean equals = action.equals("android.intent.action.PACKAGE_CHANGED");
                        packageChanged3 = equals;
                        if (equals) {
                            packageChanged = packageChanged3;
                            packageChanged2 = queryRemove2;
                            queryRemove = false;
                        } else {
                            boolean equals2 = action.equals("android.intent.action.QUERY_PACKAGE_RESTART");
                            queryRestart = equals2;
                            if (!equals2 && !action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE") && !action.equals("android.intent.action.PACKAGES_SUSPENDED") && !action.equals("android.intent.action.PACKAGES_UNSUSPENDED") && !action.equals("android.intent.action.DISTRACTING_PACKAGES_CHANGED")) {
                                return;
                            }
                        }
                    }
                    packageChanged = packageChanged3;
                    packageChanged2 = queryRemove2;
                    queryRemove = queryRestart;
                }
                int changeUserId2 = intent.getIntExtra("android.intent.extra.user_handle", -1);
                boolean removingPackage3 = packageChanged2 && !intent.getBooleanExtra("android.intent.extra.REPLACING", false);
                if (NotificationManagerService.DBG) {
                    removingPackage = removingPackage3;
                    Slog.i(NotificationManagerService.TAG, "action=" + action + " removing=" + removingPackage);
                } else {
                    removingPackage = removingPackage3;
                }
                if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE")) {
                    pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                    uidList = intent.getIntArrayExtra("android.intent.extra.changed_uid_list");
                    i = 0;
                } else if (action.equals("android.intent.action.PACKAGES_SUSPENDED")) {
                    pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                    uidList = intent.getIntArrayExtra("android.intent.extra.changed_uid_list");
                    cancelNotifications = false;
                    hideNotifications = true;
                    i = 0;
                } else if (action.equals("android.intent.action.PACKAGES_UNSUSPENDED")) {
                    pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                    uidList = intent.getIntArrayExtra("android.intent.extra.changed_uid_list");
                    cancelNotifications = false;
                    unhideNotifications = true;
                    i = 0;
                } else if (action.equals("android.intent.action.DISTRACTING_PACKAGES_CHANGED")) {
                    int distractionRestrictions = intent.getIntExtra("android.intent.extra.distraction_restrictions", 0);
                    if ((distractionRestrictions & 2) != 0) {
                        String[] pkgList2 = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                        int[] uidList2 = intent.getIntArrayExtra("android.intent.extra.changed_uid_list");
                        cancelNotifications = false;
                        hideNotifications = true;
                        pkgList = pkgList2;
                        uidList = uidList2;
                    } else {
                        String[] pkgList3 = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                        int[] uidList3 = intent.getIntArrayExtra("android.intent.extra.changed_uid_list");
                        cancelNotifications = false;
                        unhideNotifications = true;
                        pkgList = pkgList3;
                        uidList = uidList3;
                    }
                    i = 0;
                } else if (queryRemove) {
                    pkgList = intent.getStringArrayExtra("android.intent.extra.PACKAGES");
                    uidList = new int[]{intent.getIntExtra("android.intent.extra.UID", -1)};
                    i = 0;
                } else {
                    Uri uri = intent.getData();
                    if (uri == null || (pkgName = uri.getSchemeSpecificPart()) == null) {
                        return;
                    }
                    if (packageChanged) {
                        try {
                            int enabled = NotificationManagerService.this.mPackageManager.getApplicationEnabledSetting(pkgName, changeUserId2 != -1 ? changeUserId2 : 0);
                            if (enabled == 1 || enabled == 0) {
                                cancelNotifications = false;
                            }
                        } catch (RemoteException e) {
                        } catch (IllegalArgumentException e2) {
                            if (NotificationManagerService.DBG) {
                                Slog.i(NotificationManagerService.TAG, "Exception trying to look up app enabled setting", e2);
                            }
                        }
                    }
                    i = 0;
                    pkgList = new String[]{pkgName};
                    uidList = new int[]{intent.getIntExtra("android.intent.extra.UID", -1)};
                }
                if (pkgList == null || pkgList.length <= 0) {
                    removingPackage2 = removingPackage;
                    changeUserId = changeUserId2;
                } else if (cancelNotifications) {
                    int length = pkgList.length;
                    int i2 = i;
                    while (i2 < length) {
                        int changeUserId3 = changeUserId2;
                        NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, pkgList[i2], null, 0, 0, !queryRemove ? 1 : i, changeUserId3, 5, null);
                        i2++;
                        i = i;
                        removingPackage = removingPackage;
                        length = length;
                        changeUserId2 = changeUserId3;
                    }
                    removingPackage2 = removingPackage;
                    changeUserId = changeUserId2;
                } else {
                    removingPackage2 = removingPackage;
                    changeUserId = changeUserId2;
                    if (hideNotifications && uidList != null && uidList.length > 0) {
                        NotificationManagerService.this.hideNotificationsForPackages(pkgList, uidList);
                    } else if (unhideNotifications && uidList != null && uidList.length > 0) {
                        NotificationManagerService.this.unhideNotificationsForPackages(pkgList, uidList);
                    }
                }
                NotificationManagerService.this.mHandler.scheduleOnPackageChanged(removingPackage2, changeUserId, pkgList, uidList);
            }
        };
        this.mIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.notification.NotificationManagerService.6
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    NotificationManagerService.this.mScreenOn = true;
                    NotificationManagerService.this.updateNotificationPulse();
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    NotificationManagerService.this.mScreenOn = false;
                    NotificationManagerService.this.updateNotificationPulse();
                } else if (action.equals("android.intent.action.PHONE_STATE")) {
                    NotificationManagerService.this.mInCallStateOffHook = TelephonyManager.EXTRA_STATE_OFFHOOK.equals(intent.getStringExtra("state"));
                    NotificationManagerService.this.updateNotificationPulse();
                } else if (action.equals("android.intent.action.USER_STOPPED")) {
                    int userHandle = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    if (userHandle >= 0) {
                        NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, null, null, 0, 0, true, userHandle, 6, null);
                    }
                } else if (action.equals("android.intent.action.MANAGED_PROFILE_UNAVAILABLE")) {
                    int userHandle2 = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    if (userHandle2 >= 0) {
                        NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, null, null, 0, 0, true, userHandle2, 15, null);
                    }
                } else if (action.equals("android.intent.action.USER_PRESENT")) {
                    if (NotificationManagerService.this.mNotificationLight != null) {
                        NotificationManagerService.this.mNotificationLight.turnOff();
                    }
                } else if (action.equals("android.intent.action.USER_SWITCHED")) {
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    NotificationManagerService.this.mUserProfiles.updateCache(context2);
                    if (!NotificationManagerService.this.mUserProfiles.isManagedProfile(userId) && !NotificationManagerService.this.mUserProfiles.isDualProfile(userId)) {
                        NotificationManagerService.this.mSettingsObserver.update(null);
                        NotificationManagerService.this.mConditionProviders.onUserSwitched(userId);
                        NotificationManagerService.this.mListeners.onUserSwitched(userId);
                        NotificationManagerService.this.mZenModeHelper.onUserSwitched(userId);
                    }
                    NotificationManagerService.this.mAssistants.onUserSwitched(userId);
                } else if (action.equals("android.intent.action.USER_ADDED")) {
                    int userId2 = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    if (userId2 != -10000) {
                        NotificationManagerService.this.mUserProfiles.updateCache(context2);
                        if (!NotificationManagerService.this.mUserProfiles.isManagedProfile(userId2) && !NotificationManagerService.this.mUserProfiles.isDualProfile(userId2)) {
                            NotificationManagerService.this.allowDefaultApprovedServices(userId2);
                        }
                    }
                } else if (action.equals("android.intent.action.USER_REMOVED")) {
                    int userId3 = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    NotificationManagerService.this.mUserProfiles.updateCache(context2);
                    NotificationManagerService.this.mZenModeHelper.onUserRemoved(userId3);
                    NotificationManagerService.this.mPreferencesHelper.onUserRemoved(userId3);
                    NotificationManagerService.this.mListeners.onUserRemoved(userId3);
                    NotificationManagerService.this.mConditionProviders.onUserRemoved(userId3);
                    NotificationManagerService.this.mAssistants.onUserRemoved(userId3);
                    NotificationManagerService.this.mHistoryManager.onUserRemoved(userId3);
                    NotificationManagerService.this.handleSavePolicyFile();
                } else if (action.equals("android.intent.action.USER_UNLOCKED")) {
                    int userId4 = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    NotificationManagerService.this.mUserProfiles.updateCache(context2);
                    NotificationManagerService.this.mAssistants.onUserUnlocked(userId4);
                    if (!NotificationManagerService.this.mUserProfiles.isManagedProfile(userId4) && !NotificationManagerService.this.mUserProfiles.isDualProfile(userId4)) {
                        NotificationManagerService.this.mConditionProviders.onUserUnlocked(userId4);
                        NotificationManagerService.this.mListeners.onUserUnlocked(userId4);
                        NotificationManagerService.this.mZenModeHelper.onUserUnlocked(userId4);
                    }
                }
            }
        };
        this.mService = new AnonymousClass10();
        this.mInternalService = new AnonymousClass11();
        this.mShortcutListener = new ShortcutHelper.ShortcutListener() { // from class: com.android.server.notification.NotificationManagerService.12
            @Override // com.android.server.notification.ShortcutHelper.ShortcutListener
            public void onShortcutRemoved(String key) {
                String packageName;
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                    packageName = r != null ? r.getSbn().getPackageName() : null;
                }
                boolean isAppForeground = packageName != null && NotificationManagerService.this.mActivityManager.getPackageImportance(packageName) == 100;
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationRecord r2 = NotificationManagerService.this.mNotificationsByKey.get(key);
                    if (r2 != null) {
                        r2.setShortcutInfo(null);
                        r2.getNotification().flags |= 8;
                        NotificationManagerService.this.mHandler.post(new EnqueueNotificationRunnable(r2.getUser().getIdentifier(), r2, isAppForeground, SystemClock.elapsedRealtime()));
                    }
                }
            }
        };
        this.mNotificationRecordLogger = notificationRecordLogger;
        this.mNotificationInstanceIdSequence = notificationInstanceIdSequence;
        Notification.processAllowlistToken = ALLOWLIST_TOKEN;
    }

    void setAudioManager(AudioManager audioMananger) {
        this.mAudioManager = audioMananger;
    }

    void setStrongAuthTracker(StrongAuthTracker strongAuthTracker) {
        this.mStrongAuthTracker = strongAuthTracker;
    }

    void setKeyguardManager(KeyguardManager keyguardManager) {
        this.mKeyguardManager = keyguardManager;
    }

    ShortcutHelper getShortcutHelper() {
        return this.mShortcutHelper;
    }

    void setShortcutHelper(ShortcutHelper helper) {
        this.mShortcutHelper = helper;
    }

    VibratorHelper getVibratorHelper() {
        return this.mVibratorHelper;
    }

    void setVibratorHelper(VibratorHelper helper) {
        this.mVibratorHelper = helper;
    }

    void setHints(int hints) {
        this.mListenerHints = hints;
    }

    void setLights(LogicalLight light) {
        this.mNotificationLight = light;
        this.mAttentionLight = light;
        this.mNotificationPulseEnabled = true;
    }

    void setScreenOn(boolean on) {
        this.mScreenOn = on;
    }

    int getNotificationRecordCount() {
        int count;
        synchronized (this.mNotificationLock) {
            count = this.mNotificationList.size() + this.mNotificationsByKey.size() + this.mSummaryByGroupKey.size() + this.mEnqueuedNotifications.size();
            Iterator<NotificationRecord> it = this.mNotificationList.iterator();
            while (it.hasNext()) {
                NotificationRecord posted = it.next();
                if (this.mNotificationsByKey.containsKey(posted.getKey())) {
                    count--;
                }
                if (posted.getSbn().isGroup() && posted.getNotification().isGroupSummary()) {
                    count--;
                }
            }
        }
        return count;
    }

    void clearNotifications() {
        synchronized (this.mNotificationList) {
            this.mEnqueuedNotifications.clear();
            this.mNotificationList.clear();
            this.mNotificationsByKey.clear();
            this.mSummaryByGroupKey.clear();
        }
    }

    void addNotification(NotificationRecord r) {
        this.mNotificationList.add(r);
        this.mNotificationsByKey.put(r.getSbn().getKey(), r);
        if (r.getSbn().isGroup()) {
            this.mSummaryByGroupKey.put(r.getGroupKey(), r);
        }
    }

    void addEnqueuedNotification(NotificationRecord r) {
        this.mEnqueuedNotifications.add(r);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public NotificationRecord getNotificationRecord(String key) {
        return this.mNotificationsByKey.get(key);
    }

    void setSystemReady(boolean systemReady) {
        this.mSystemReady = systemReady;
    }

    void setHandler(WorkerHandler handler) {
        this.mHandler = handler;
    }

    void setPackageManager(IPackageManager packageManager) {
        this.mPackageManager = packageManager;
    }

    void setRankingHelper(RankingHelper rankingHelper) {
        this.mRankingHelper = rankingHelper;
    }

    void setPreferencesHelper(PreferencesHelper prefHelper) {
        this.mPreferencesHelper = prefHelper;
    }

    void setZenHelper(ZenModeHelper zenHelper) {
        this.mZenModeHelper = zenHelper;
    }

    void setIsAutomotive(boolean isAutomotive) {
        this.mIsAutomotive = isAutomotive;
    }

    void setNotificationEffectsEnabledForAutomotive(boolean isEnabled) {
        this.mNotificationEffectsEnabledForAutomotive = isEnabled;
    }

    void setIsTelevision(boolean isTelevision) {
        this.mIsTelevision = isTelevision;
    }

    void setUsageStats(NotificationUsageStats us) {
        this.mUsageStats = us;
    }

    void setAccessibilityManager(AccessibilityManager am) {
        this.mAccessibilityManager = am;
    }

    void setTelecomManager(TelecomManager tm) {
        this.mTelecomManager = tm;
    }

    void init(WorkerHandler handler, RankingHandler rankingHandler, IPackageManager packageManager, PackageManager packageManagerClient, LightsManager lightsManager, NotificationListeners notificationListeners, NotificationAssistants notificationAssistants, ConditionProviders conditionProviders, ICompanionDeviceManager companionManager, SnoozeHelper snoozeHelper, NotificationUsageStats usageStats, AtomicFile policyFile, ActivityManager activityManager, GroupHelper groupHelper, IActivityManager am, ActivityTaskManagerInternal atm, UsageStatsManagerInternal appUsageStats, DevicePolicyManagerInternal dpm, IUriGrantsManager ugm, UriGrantsManagerInternal ugmInternal, AppOpsManager appOps, IAppOpsService iAppOps, UserManager userManager, NotificationHistoryManager historyManager, StatsManager statsManager, TelephonyManager telephonyManager, ActivityManagerInternal ami, MultiRateLimiter toastRateLimiter, PermissionHelper permissionHelper, UsageStatsManagerInternal usageStatsManagerInternal, TelecomManager telecomManager, NotificationChannelLogger channelLogger) {
        String[] extractorNames;
        this.mHandler = handler;
        Resources resources = getContext().getResources();
        this.mMaxPackageEnqueueRate = Settings.Global.getFloat(getContext().getContentResolver(), "max_notification_enqueue_rate", DEFAULT_MAX_NOTIFICATION_ENQUEUE_RATE);
        this.mAccessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
        this.mAm = am;
        this.mAtm = atm;
        atm.setBackgroundActivityStartCallback(new NotificationTrampolineCallback());
        this.mUgm = ugm;
        this.mUgmInternal = ugmInternal;
        this.mPackageManager = packageManager;
        this.mPackageManagerClient = packageManagerClient;
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mPermissionPolicyInternal = (PermissionPolicyInternal) LocalServices.getService(PermissionPolicyInternal.class);
        this.mUsageStatsManagerInternal = usageStatsManagerInternal;
        this.mAppOps = appOps;
        this.mAppOpsService = iAppOps;
        this.mAppUsageStats = appUsageStats;
        this.mAlarmManager = (AlarmManager) getContext().getSystemService("alarm");
        this.mCompanionManager = companionManager;
        this.mActivityManager = activityManager;
        this.mAmi = ami;
        this.mDeviceIdleManager = (DeviceIdleManager) getContext().getSystemService(DeviceIdleManager.class);
        this.mDpm = dpm;
        this.mUm = userManager;
        this.mTelecomManager = telecomManager;
        this.mPlatformCompat = IPlatformCompat.Stub.asInterface(ServiceManager.getService("platform_compat"));
        this.mStrongAuthTracker = new StrongAuthTracker(getContext());
        this.mUiHandler = new Handler(UiThread.get().getLooper());
        try {
            extractorNames = resources.getStringArray(17236105);
        } catch (Resources.NotFoundException e) {
            extractorNames = new String[0];
        }
        this.mUsageStats = usageStats;
        this.mMetricsLogger = new MetricsLogger();
        this.mRankingHandler = rankingHandler;
        this.mConditionProviders = conditionProviders;
        ZenModeHelper zenModeHelper = new ZenModeHelper(getContext(), this.mHandler.getLooper(), this.mConditionProviders, new SysUiStatsEvent.BuilderFactory());
        this.mZenModeHelper = zenModeHelper;
        zenModeHelper.addCallback(new AnonymousClass7());
        this.mPermissionHelper = permissionHelper;
        this.mNotificationChannelLogger = channelLogger;
        PreferencesHelper preferencesHelper = new PreferencesHelper(getContext(), this.mPackageManagerClient, this.mRankingHandler, this.mZenModeHelper, this.mPermissionHelper, this.mNotificationChannelLogger, this.mAppOps, new SysUiStatsEvent.BuilderFactory(), this.mShowReviewPermissionsNotification);
        this.mPreferencesHelper = preferencesHelper;
        preferencesHelper.updateFixedImportance(this.mUm.getUsers());
        this.mRankingHelper = new RankingHelper(getContext(), this.mRankingHandler, this.mPreferencesHelper, this.mZenModeHelper, this.mUsageStats, extractorNames);
        this.mSnoozeHelper = snoozeHelper;
        this.mGroupHelper = groupHelper;
        this.mVibratorHelper = new VibratorHelper(getContext());
        this.mHistoryManager = historyManager;
        this.mListeners = notificationListeners;
        this.mAssistants = notificationAssistants;
        this.mAllowedManagedServicePackages = new TriPredicate() { // from class: com.android.server.notification.NotificationManagerService$$ExternalSyntheticLambda11
            public final boolean test(Object obj, Object obj2, Object obj3) {
                return NotificationManagerService.this.canUseManagedServices((String) obj, (Integer) obj2, (String) obj3);
            }
        };
        this.mPolicyFile = policyFile;
        loadPolicyFile();
        if (this.TRAN_NOTIFY_SCREEN_ON_SUPPORT) {
            ITranNotificationManagerService.Instance().initNotifyScreenOn(this, this.mHandler);
        }
        StatusBarManagerInternal statusBarManagerInternal = (StatusBarManagerInternal) getLocalService(StatusBarManagerInternal.class);
        this.mStatusBar = statusBarManagerInternal;
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.setNotificationDelegate(this.mNotificationDelegate);
        }
        this.mNotificationLight = lightsManager.getLight(4);
        this.mAttentionLight = lightsManager.getLight(5);
        this.mInCallNotificationUri = Uri.parse("file://" + resources.getString(17039987));
        this.mInCallNotificationAudioAttributes = new AudioAttributes.Builder().setContentType(4).setUsage(2).build();
        this.mInCallNotificationVolume = resources.getFloat(17105081);
        this.mUseAttentionLight = resources.getBoolean(17891809);
        this.mHasLight = resources.getBoolean(17891684);
        if (Settings.Global.getInt(getContext().getContentResolver(), "device_provisioned", 0) == 0) {
            this.mDisableNotificationEffects = true;
        }
        this.mZenModeHelper.initZenMode();
        this.mInterruptionFilter = this.mZenModeHelper.getZenModeListenerInterruptionFilter();
        this.mUserProfiles.updateCache(getContext());
        if (this.mPackageManagerClient.hasSystemFeature("android.hardware.telephony")) {
            telephonyManager.listen(new PhoneStateListener() { // from class: com.android.server.notification.NotificationManagerService.8
                @Override // android.telephony.PhoneStateListener
                public void onCallStateChanged(int state, String incomingNumber) {
                    if (NotificationManagerService.this.mCallState == state) {
                        return;
                    }
                    if (NotificationManagerService.DBG) {
                        Slog.d(NotificationManagerService.TAG, "Call state changed: " + NotificationManagerService.callStateToString(state));
                    }
                    NotificationManagerService.this.mCallState = state;
                }
            }, 32);
        }
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mArchive = new Archive(resources.getInteger(17694896));
        this.mIsTelevision = this.mPackageManagerClient.hasSystemFeature("android.software.leanback") || this.mPackageManagerClient.hasSystemFeature("android.hardware.type.television");
        this.mIsAutomotive = this.mPackageManagerClient.hasSystemFeature("android.hardware.type.automotive", 0);
        this.mNotificationEffectsEnabledForAutomotive = resources.getBoolean(17891648);
        this.mZenModeHelper.setPriorityOnlyDndExemptPackages(getContext().getResources().getStringArray(17236109));
        this.mWarnRemoteViewsSizeBytes = getContext().getResources().getInteger(17694898);
        this.mStripRemoteViewsSizeBytes = getContext().getResources().getInteger(17694897);
        this.mMsgPkgsAllowedAsConvos = Set.of((Object[]) getStringArrayResource(17236104));
        this.mStatsManager = statsManager;
        this.mToastRateLimiter = toastRateLimiter;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("android.intent.action.USER_STOPPED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_UNLOCKED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        getContext().registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, filter, null, null);
        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction("android.intent.action.PACKAGE_ADDED");
        pkgFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        pkgFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        pkgFilter.addAction("android.intent.action.PACKAGE_RESTARTED");
        pkgFilter.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
        pkgFilter.addDataScheme("package");
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, pkgFilter, null, null);
        IntentFilter suspendedPkgFilter = new IntentFilter();
        suspendedPkgFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        suspendedPkgFilter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        suspendedPkgFilter.addAction("android.intent.action.DISTRACTING_PACKAGES_CHANGED");
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, suspendedPkgFilter, null, null);
        IntentFilter sdFilter = new IntentFilter("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, sdFilter, null, null);
        IntentFilter timeoutFilter = new IntentFilter(ACTION_NOTIFICATION_TIMEOUT);
        timeoutFilter.addDataScheme(SCHEME_TIMEOUT);
        getContext().registerReceiver(this.mNotificationTimeoutReceiver, timeoutFilter, 2);
        IntentFilter settingsRestoredFilter = new IntentFilter("android.os.action.SETTING_RESTORED");
        getContext().registerReceiver(this.mRestoreReceiver, settingsRestoredFilter);
        IntentFilter localeChangedFilter = new IntentFilter("android.intent.action.LOCALE_CHANGED");
        getContext().registerReceiver(this.mLocaleChangeReceiver, localeChangedFilter);
        this.mReviewNotificationPermissionsReceiver = new ReviewNotificationPermissionsReceiver();
        getContext().registerReceiver(this.mReviewNotificationPermissionsReceiver, ReviewNotificationPermissionsReceiver.getFilter(), 4);
        ITranLedLightExt Instance = ITranLedLightExt.Instance();
        mTranLedLightExt = Instance;
        Instance.init(lightsManager, getContext());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.notification.NotificationManagerService$7  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass7 extends ZenModeHelper.Callback {
        AnonymousClass7() {
        }

        @Override // com.android.server.notification.ZenModeHelper.Callback
        public void onConfigChanged() {
            NotificationManagerService.this.handleSavePolicyFile();
        }

        @Override // com.android.server.notification.ZenModeHelper.Callback
        void onZenModeChanged() {
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.notification.NotificationManagerService$7$$ExternalSyntheticLambda3
                public final void runOrThrow() {
                    NotificationManagerService.AnonymousClass7.this.m5146xaa776df6();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onZenModeChanged$0$com-android-server-notification-NotificationManagerService$7  reason: not valid java name */
        public /* synthetic */ void m5146xaa776df6() throws Exception {
            NotificationManagerService.this.sendRegisteredOnlyBroadcast("android.app.action.INTERRUPTION_FILTER_CHANGED");
            NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.INTERRUPTION_FILTER_CHANGED_INTERNAL").addFlags(67108864), UserHandle.ALL, "android.permission.MANAGE_NOTIFICATIONS");
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationManagerService.this.updateInterruptionFilterLocked();
            }
            NotificationManagerService.this.mRankingHandler.requestSort();
        }

        @Override // com.android.server.notification.ZenModeHelper.Callback
        void onPolicyChanged() {
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.notification.NotificationManagerService$7$$ExternalSyntheticLambda2
                public final void runOrThrow() {
                    NotificationManagerService.AnonymousClass7.this.m5145x5daf4359();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onPolicyChanged$1$com-android-server-notification-NotificationManagerService$7  reason: not valid java name */
        public /* synthetic */ void m5145x5daf4359() throws Exception {
            NotificationManagerService.this.sendRegisteredOnlyBroadcast("android.app.action.NOTIFICATION_POLICY_CHANGED");
            NotificationManagerService.this.mRankingHandler.requestSort();
        }

        @Override // com.android.server.notification.ZenModeHelper.Callback
        void onConsolidatedPolicyChanged() {
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.notification.NotificationManagerService$7$$ExternalSyntheticLambda0
                public final void runOrThrow() {
                    NotificationManagerService.AnonymousClass7.this.m5144xde55b693();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onConsolidatedPolicyChanged$2$com-android-server-notification-NotificationManagerService$7  reason: not valid java name */
        public /* synthetic */ void m5144xde55b693() throws Exception {
            NotificationManagerService.this.mRankingHandler.requestSort();
        }

        @Override // com.android.server.notification.ZenModeHelper.Callback
        void onAutomaticRuleStatusChanged(final int userId, final String pkg, final String id, final int status) {
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.notification.NotificationManagerService$7$$ExternalSyntheticLambda1
                public final void runOrThrow() {
                    NotificationManagerService.AnonymousClass7.this.m5143xba6eec46(pkg, id, status, userId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAutomaticRuleStatusChanged$3$com-android-server-notification-NotificationManagerService$7  reason: not valid java name */
        public /* synthetic */ void m5143xba6eec46(String pkg, String id, int status, int userId) throws Exception {
            Intent intent = new Intent("android.app.action.AUTOMATIC_ZEN_RULE_STATUS_CHANGED");
            intent.setPackage(pkg);
            intent.putExtra("android.app.extra.AUTOMATIC_ZEN_RULE_ID", id);
            intent.putExtra("android.app.extra.AUTOMATIC_ZEN_RULE_STATUS", status);
            NotificationManagerService.this.getContext().sendBroadcastAsUser(intent, UserHandle.of(userId));
        }
    }

    public void onDestroy() {
        getContext().unregisterReceiver(this.mIntentReceiver);
        getContext().unregisterReceiver(this.mPackageIntentReceiver);
        getContext().unregisterReceiver(this.mNotificationTimeoutReceiver);
        getContext().unregisterReceiver(this.mRestoreReceiver);
        getContext().unregisterReceiver(this.mLocaleChangeReceiver);
        DeviceConfig.OnPropertiesChangedListener onPropertiesChangedListener = this.mDeviceConfigChangedListener;
        if (onPropertiesChangedListener != null) {
            DeviceConfig.removeOnPropertiesChangedListener(onPropertiesChangedListener);
        }
    }

    protected String[] getStringArrayResource(int key) {
        return getContext().getResources().getStringArray(key);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        SnoozeHelper snoozeHelper = new SnoozeHelper(getContext(), new SnoozeHelper.Callback() { // from class: com.android.server.notification.NotificationManagerService$$ExternalSyntheticLambda1
            @Override // com.android.server.notification.SnoozeHelper.Callback
            public final void repost(int i, NotificationRecord notificationRecord, boolean z) {
                NotificationManagerService.this.m5132x5ba9a5a7(i, notificationRecord, z);
            }
        }, this.mUserProfiles);
        File systemDir = new File(Environment.getDataDirectory(), HostingRecord.HOSTING_TYPE_SYSTEM);
        this.mRankingThread.start();
        WorkerHandler handler = new WorkerHandler(Looper.myLooper());
        this.mShowReviewPermissionsNotification = getContext().getResources().getBoolean(17891717);
        RankingHandlerWorker rankingHandlerWorker = new RankingHandlerWorker(this.mRankingThread.getLooper());
        IPackageManager packageManager = AppGlobals.getPackageManager();
        PackageManager packageManager2 = getContext().getPackageManager();
        LightsManager lightsManager = (LightsManager) getLocalService(LightsManager.class);
        NotificationListeners notificationListeners = new NotificationListeners(getContext(), this.mNotificationLock, this.mUserProfiles, AppGlobals.getPackageManager());
        NotificationAssistants notificationAssistants = new NotificationAssistants(getContext(), this.mNotificationLock, this.mUserProfiles, AppGlobals.getPackageManager());
        ConditionProviders conditionProviders = new ConditionProviders(getContext(), this.mUserProfiles, AppGlobals.getPackageManager());
        NotificationUsageStats notificationUsageStats = new NotificationUsageStats(getContext());
        AtomicFile atomicFile = new AtomicFile(new File(systemDir, "notification_policy.xml"), TAG_NOTIFICATION_POLICY);
        ActivityManager activityManager = (ActivityManager) getContext().getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
        GroupHelper groupHelper = getGroupHelper();
        IActivityManager service = ActivityManager.getService();
        ActivityTaskManagerInternal activityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        UsageStatsManagerInternal usageStatsManagerInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        DevicePolicyManagerInternal devicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        IUriGrantsManager service2 = UriGrantsManager.getService();
        UriGrantsManagerInternal uriGrantsManagerInternal = (UriGrantsManagerInternal) LocalServices.getService(UriGrantsManagerInternal.class);
        AppOpsManager appOpsManager = (AppOpsManager) getContext().getSystemService(AppOpsManager.class);
        IAppOpsService asInterface = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
        UserManager userManager = (UserManager) getContext().getSystemService(UserManager.class);
        NotificationHistoryManager notificationHistoryManager = new NotificationHistoryManager(getContext(), handler);
        StatsManager statsManager = (StatsManager) getContext().getSystemService("stats");
        this.mStatsManager = statsManager;
        init(handler, rankingHandlerWorker, packageManager, packageManager2, lightsManager, notificationListeners, notificationAssistants, conditionProviders, null, snoozeHelper, notificationUsageStats, atomicFile, activityManager, groupHelper, service, activityTaskManagerInternal, usageStatsManagerInternal, devicePolicyManagerInternal, service2, uriGrantsManagerInternal, appOpsManager, asInterface, userManager, notificationHistoryManager, statsManager, (TelephonyManager) getContext().getSystemService(TelephonyManager.class), (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class), createToastRateLimiter(), new PermissionHelper((PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class), AppGlobals.getPackageManager(), AppGlobals.getPermissionManager()), (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class), (TelecomManager) getContext().getSystemService(TelecomManager.class), new NotificationChannelLoggerImpl());
        publishBinderService("notification", this.mService, false, 5);
        publishLocalService(NotificationManagerInternal.class, this.mInternalService);
        mTranLedLightExt.registerLedObserver();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onStart$0$com-android-server-notification-NotificationManagerService  reason: not valid java name */
    public /* synthetic */ void m5132x5ba9a5a7(int userId, NotificationRecord r, boolean muteOnReturn) {
        try {
            if (DBG) {
                Slog.d(TAG, "Reposting " + r.getKey());
            }
            enqueueNotificationInternal(r.getSbn().getPackageName(), r.getSbn().getOpPkg(), r.getSbn().getUid(), r.getSbn().getInitialPid(), r.getSbn().getTag(), r.getSbn().getId(), r.getSbn().getNotification(), userId, true);
        } catch (Exception e) {
            Slog.e(TAG, "Cannot un-snooze notification", e);
        }
    }

    void registerDeviceConfigChange() {
        this.mDeviceConfigChangedListener = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.notification.NotificationManagerService$$ExternalSyntheticLambda0
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                NotificationManagerService.this.m5137xc7a11424(properties);
            }
        };
        this.mAllowFgsDismissal = DeviceConfig.getBoolean("systemui", "task_manager_enabled", true);
        DeviceConfig.addOnPropertiesChangedListener("systemui", new HandlerExecutor(this.mHandler), this.mDeviceConfigChangedListener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerDeviceConfigChange$1$com-android-server-notification-NotificationManagerService  reason: not valid java name */
    public /* synthetic */ void m5137xc7a11424(DeviceConfig.Properties properties) {
        if (!"systemui".equals(properties.getNamespace())) {
            return;
        }
        for (String name : properties.getKeyset()) {
            if ("nas_default_service".equals(name)) {
                this.mAssistants.resetDefaultAssistantsIfNecessary();
            } else if ("enable_nas_prioritizer".equals(name)) {
                String value = properties.getString(name, (String) null);
                if ("true".equals(value)) {
                    this.mAssistants.allowAdjustmentType("key_importance");
                } else if ("false".equals(value)) {
                    this.mAssistants.disallowAdjustmentType("key_importance");
                }
            } else if ("enable_nas_ranking".equals(name)) {
                String value2 = properties.getString(name, (String) null);
                if ("true".equals(value2)) {
                    this.mAssistants.allowAdjustmentType("key_ranking_score");
                } else if ("false".equals(value2)) {
                    this.mAssistants.disallowAdjustmentType("key_ranking_score");
                }
            } else if ("enable_nas_not_conversation".equals(name)) {
                String value3 = properties.getString(name, (String) null);
                if ("true".equals(value3)) {
                    this.mAssistants.allowAdjustmentType("key_not_conversation");
                } else if ("false".equals(value3)) {
                    this.mAssistants.disallowAdjustmentType("key_not_conversation");
                }
            } else if ("task_manager_enabled".equals(name)) {
                String value4 = properties.getString(name, (String) null);
                if ("true".equals(value4)) {
                    this.mAllowFgsDismissal = true;
                } else if ("false".equals(value4)) {
                    this.mAllowFgsDismissal = false;
                }
            }
        }
    }

    private void registerNotificationPreferencesPullers() {
        this.mPullAtomCallback = new StatsPullAtomCallbackImpl();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PACKAGE_NOTIFICATION_PREFERENCES, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mPullAtomCallback);
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PACKAGE_NOTIFICATION_CHANNEL_PREFERENCES, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mPullAtomCallback);
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PACKAGE_NOTIFICATION_CHANNEL_GROUP_PREFERENCES, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mPullAtomCallback);
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.DND_MODE_RULE, (StatsManager.PullAtomMetadata) null, BackgroundThread.getExecutor(), this.mPullAtomCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class StatsPullAtomCallbackImpl implements StatsManager.StatsPullAtomCallback {
        private StatsPullAtomCallbackImpl() {
        }

        public int onPullAtom(int atomTag, List<StatsEvent> data) {
            switch (atomTag) {
                case FrameworkStatsLog.PACKAGE_NOTIFICATION_PREFERENCES /* 10071 */:
                case FrameworkStatsLog.PACKAGE_NOTIFICATION_CHANNEL_PREFERENCES /* 10072 */:
                case FrameworkStatsLog.PACKAGE_NOTIFICATION_CHANNEL_GROUP_PREFERENCES /* 10073 */:
                case FrameworkStatsLog.DND_MODE_RULE /* 10084 */:
                    return NotificationManagerService.this.pullNotificationStates(atomTag, data);
                default:
                    throw new UnsupportedOperationException("Unknown tagId=" + atomTag);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int pullNotificationStates(int atomTag, List<StatsEvent> data) {
        switch (atomTag) {
            case FrameworkStatsLog.PACKAGE_NOTIFICATION_PREFERENCES /* 10071 */:
                this.mPreferencesHelper.pullPackagePreferencesStats(data, getAllUsersNotificationPermissions());
                return 0;
            case FrameworkStatsLog.PACKAGE_NOTIFICATION_CHANNEL_PREFERENCES /* 10072 */:
                this.mPreferencesHelper.pullPackageChannelPreferencesStats(data);
                return 0;
            case FrameworkStatsLog.PACKAGE_NOTIFICATION_CHANNEL_GROUP_PREFERENCES /* 10073 */:
                this.mPreferencesHelper.pullPackageChannelGroupPreferencesStats(data);
                return 0;
            case FrameworkStatsLog.DND_MODE_RULE /* 10084 */:
                this.mZenModeHelper.pullRules(data);
                return 0;
            default:
                return 0;
        }
    }

    private GroupHelper getGroupHelper() {
        this.mAutoGroupAtCount = getContext().getResources().getInteger(17694743);
        return new GroupHelper(this.mAutoGroupAtCount, new GroupHelper.Callback() { // from class: com.android.server.notification.NotificationManagerService.9
            @Override // com.android.server.notification.GroupHelper.Callback
            public void addAutoGroup(String key) {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.addAutogroupKeyLocked(key);
                }
            }

            @Override // com.android.server.notification.GroupHelper.Callback
            public void removeAutoGroup(String key) {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.removeAutogroupKeyLocked(key);
                }
            }

            @Override // com.android.server.notification.GroupHelper.Callback
            public void addAutoGroupSummary(int userId, String pkg, String triggeringKey, boolean needsOngoingFlag) {
                NotificationManagerService.this.addAutoGroupSummary(userId, pkg, triggeringKey, needsOngoingFlag);
            }

            @Override // com.android.server.notification.GroupHelper.Callback
            public void removeAutoGroupSummary(int userId, String pkg) {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.clearAutogroupSummaryLocked(userId, pkg);
                }
            }

            @Override // com.android.server.notification.GroupHelper.Callback
            public void updateAutogroupSummary(int userId, String pkg, boolean needsOngoingFlag) {
                boolean isAppForeground = pkg != null && NotificationManagerService.this.mActivityManager.getPackageImportance(pkg) == 100;
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.updateAutobundledSummaryFlags(userId, pkg, needsOngoingFlag, isAppForeground);
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendRegisteredOnlyBroadcast(String action) {
        Intent intent = new Intent(action);
        getContext().sendBroadcastAsUser(intent.addFlags(1073741824), UserHandle.ALL, null);
        intent.setFlags(0);
        Set<String> dndApprovedPackages = this.mConditionProviders.getAllowedPackages();
        for (String pkg : dndApprovedPackages) {
            intent.setPackage(pkg);
            intent.addFlags(67108864);
            getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        onBootPhase(phase, Looper.getMainLooper());
    }

    void onBootPhase(int phase, Looper mainLooper) {
        if (phase != 500) {
            if (phase == 600) {
                this.mSettingsObserver.observe();
                this.mListeners.onBootPhaseAppsCanStart();
                this.mAssistants.onBootPhaseAppsCanStart();
                this.mConditionProviders.onBootPhaseAppsCanStart();
                this.mHistoryManager.onBootPhaseAppsCanStart();
                registerDeviceConfigChange();
                migrateDefaultNAS();
                maybeShowInitialReviewPermissionsNotification();
                ITranNotificationManagerService.Instance().onBootPhase(this.mHandler, getContext());
                return;
            } else if (phase == 550) {
                this.mSnoozeHelper.scheduleRepostsForPersistedNotifications(System.currentTimeMillis());
                return;
            } else {
                return;
            }
        }
        this.mSystemReady = true;
        this.mAudioManager = (AudioManager) getContext().getSystemService("audio");
        this.mAudioManagerInternal = (AudioManagerInternal) getLocalService(AudioManagerInternal.class);
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mKeyguardManager = (KeyguardManager) getContext().getSystemService(KeyguardManager.class);
        this.mZenModeHelper.onSystemReady();
        RoleObserver roleObserver = new RoleObserver(getContext(), (RoleManager) getContext().getSystemService(RoleManager.class), this.mPackageManager, mainLooper);
        roleObserver.init();
        this.mRoleObserver = roleObserver;
        LauncherApps launcherApps = (LauncherApps) getContext().getSystemService("launcherapps");
        UserManager userManager = (UserManager) getContext().getSystemService("user");
        this.mShortcutHelper = new ShortcutHelper(launcherApps, this.mShortcutListener, (ShortcutServiceInternal) getLocalService(ShortcutServiceInternal.class), userManager);
        BubbleExtractor bubbsExtractor = (BubbleExtractor) this.mRankingHelper.findExtractor(BubbleExtractor.class);
        if (bubbsExtractor != null) {
            bubbsExtractor.setShortcutHelper(this.mShortcutHelper);
        }
        registerNotificationPreferencesPullers();
        new LockPatternUtils(getContext()).registerStrongAuthTracker(this.mStrongAuthTracker);
    }

    @Override // com.android.server.SystemService
    public void onUserUnlocking(final SystemService.TargetUser user) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                NotificationManagerService.this.m5134xc08dd7d4(user);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUserUnlocking$2$com-android-server-notification-NotificationManagerService  reason: not valid java name */
    public /* synthetic */ void m5134xc08dd7d4(SystemService.TargetUser user) {
        Trace.traceBegin(524288L, "notifHistoryUnlockUser");
        try {
            this.mHistoryManager.onUserUnlocked(user.getUserIdentifier());
        } finally {
            Trace.traceEnd(524288L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendAppBlockStateChangedBroadcast(final String pkg, final int uid, final boolean blocked) {
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                NotificationManagerService.this.m5139xae2de7bd(blocked, pkg, uid);
            }
        }, 500L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendAppBlockStateChangedBroadcast$3$com-android-server-notification-NotificationManagerService  reason: not valid java name */
    public /* synthetic */ void m5139xae2de7bd(boolean blocked, String pkg, int uid) {
        try {
            getContext().sendBroadcastAsUser(new Intent("android.app.action.APP_BLOCK_STATE_CHANGED").putExtra("android.app.extra.BLOCKED_STATE", blocked).addFlags(268435456).setPackage(pkg), UserHandle.of(UserHandle.getUserId(uid)), null);
        } catch (SecurityException e) {
            Slog.w(TAG, "Can't notify app about app block change", e);
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStopping(final SystemService.TargetUser user) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                NotificationManagerService.this.m5133x116dadce(user);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUserStopping$4$com-android-server-notification-NotificationManagerService  reason: not valid java name */
    public /* synthetic */ void m5133x116dadce(SystemService.TargetUser user) {
        Trace.traceBegin(524288L, "notifHistoryStopUser");
        try {
            this.mHistoryManager.onUserStopped(user.getUserIdentifier());
        } finally {
            Trace.traceEnd(524288L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateListenerHintsLocked() {
        int hints = calculateHints();
        int i = this.mListenerHints;
        if (hints == i) {
            return;
        }
        ZenLog.traceListenerHintsChanged(i, hints, this.mEffectsSuppressors.size());
        this.mListenerHints = hints;
        scheduleListenerHintsChanged(hints);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateEffectsSuppressorLocked() {
        long updatedSuppressedEffects = calculateSuppressedEffects();
        if (updatedSuppressedEffects == this.mZenModeHelper.getSuppressedEffects()) {
            return;
        }
        List<ComponentName> suppressors = getSuppressors();
        ZenLog.traceEffectsSuppressorChanged(this.mEffectsSuppressors, suppressors, updatedSuppressedEffects);
        this.mEffectsSuppressors = suppressors;
        this.mZenModeHelper.setSuppressedEffects(updatedSuppressedEffects);
        sendRegisteredOnlyBroadcast("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void exitIdle() {
        DeviceIdleManager deviceIdleManager = this.mDeviceIdleManager;
        if (deviceIdleManager != null) {
            deviceIdleManager.endIdle("notification interaction");
        }
    }

    void updateNotificationChannelInt(String pkg, int uid, NotificationChannel channel, boolean fromListener) {
        if (channel.getImportance() == 0) {
            cancelAllNotificationsInt(MY_UID, MY_PID, pkg, channel.getId(), 0, 0, true, UserHandle.getUserId(uid), 17, null);
            if (isUidSystemOrPhone(uid)) {
                IntArray profileIds = this.mUserProfiles.getCurrentProfileIds();
                int i = 0;
                for (int N = profileIds.size(); i < N; N = N) {
                    int profileId = profileIds.get(i);
                    cancelAllNotificationsInt(MY_UID, MY_PID, pkg, channel.getId(), 0, 0, true, profileId, 17, null);
                    i++;
                }
            }
        }
        NotificationChannel preUpdate = this.mPreferencesHelper.getNotificationChannel(pkg, uid, channel.getId(), true);
        this.mPreferencesHelper.updateNotificationChannel(pkg, uid, channel, true);
        if (this.mPreferencesHelper.onlyHasDefaultChannel(pkg, uid)) {
            this.mPermissionHelper.setNotificationPermission(pkg, UserHandle.getUserId(uid), channel.getImportance() != 0, true);
        }
        maybeNotifyChannelOwner(pkg, uid, preUpdate, channel);
        if (!fromListener) {
            NotificationChannel modifiedChannel = this.mPreferencesHelper.getNotificationChannel(pkg, uid, channel.getId(), false);
            this.mListeners.notifyNotificationChannelChanged(pkg, UserHandle.getUserHandleForUid(uid), modifiedChannel, 2);
        }
        handleSavePolicyFile();
    }

    private void maybeNotifyChannelOwner(String pkg, int uid, NotificationChannel preUpdate, NotificationChannel update) {
        try {
            if ((preUpdate.getImportance() == 0 && update.getImportance() != 0) || (preUpdate.getImportance() != 0 && update.getImportance() == 0)) {
                getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED").putExtra("android.app.extra.NOTIFICATION_CHANNEL_ID", update.getId()).putExtra("android.app.extra.BLOCKED_STATE", update.getImportance() == 0).addFlags(268435456).setPackage(pkg), UserHandle.of(UserHandle.getUserId(uid)), null);
            }
        } catch (SecurityException e) {
            Slog.w(TAG, "Can't notify app about channel change", e);
        }
    }

    void createNotificationChannelGroup(String pkg, int uid, NotificationChannelGroup group, boolean fromApp, boolean fromListener) {
        Objects.requireNonNull(group);
        Objects.requireNonNull(pkg);
        NotificationChannelGroup preUpdate = this.mPreferencesHelper.getNotificationChannelGroup(group.getId(), pkg, uid);
        this.mPreferencesHelper.createNotificationChannelGroup(pkg, uid, group, fromApp);
        if (!fromApp) {
            maybeNotifyChannelGroupOwner(pkg, uid, preUpdate, group);
        }
        if (!fromListener) {
            this.mListeners.notifyNotificationChannelGroupChanged(pkg, UserHandle.of(UserHandle.getCallingUserId()), group, 1);
        }
    }

    private void maybeNotifyChannelGroupOwner(String pkg, int uid, NotificationChannelGroup preUpdate, NotificationChannelGroup update) {
        try {
            if (preUpdate.isBlocked() != update.isBlocked()) {
                getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_CHANNEL_GROUP_BLOCK_STATE_CHANGED").putExtra("android.app.extra.NOTIFICATION_CHANNEL_GROUP_ID", update.getId()).putExtra("android.app.extra.BLOCKED_STATE", update.isBlocked()).addFlags(268435456).setPackage(pkg), UserHandle.of(UserHandle.getUserId(uid)), null);
            }
        } catch (SecurityException e) {
            Slog.w(TAG, "Can't notify app about group change", e);
        }
    }

    private ArrayList<ComponentName> getSuppressors() {
        ArrayList<ComponentName> names = new ArrayList<>();
        for (int i = this.mListenersDisablingEffects.size() - 1; i >= 0; i--) {
            ArraySet<ComponentName> serviceInfoList = this.mListenersDisablingEffects.valueAt(i);
            Iterator<ComponentName> it = serviceInfoList.iterator();
            while (it.hasNext()) {
                ComponentName info = it.next();
                names.add(info);
            }
        }
        return names;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean removeDisabledHints(ManagedServices.ManagedServiceInfo info) {
        return removeDisabledHints(info, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean removeDisabledHints(ManagedServices.ManagedServiceInfo info, int hints) {
        boolean removed = false;
        for (int i = this.mListenersDisablingEffects.size() - 1; i >= 0; i--) {
            int hint = this.mListenersDisablingEffects.keyAt(i);
            ArraySet<ComponentName> listeners = this.mListenersDisablingEffects.valueAt(i);
            if (hints == 0 || (hint & hints) == hint) {
                removed |= listeners.remove(info.component);
            }
        }
        return removed;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addDisabledHints(ManagedServices.ManagedServiceInfo info, int hints) {
        if ((hints & 1) != 0) {
            addDisabledHint(info, 1);
        }
        if ((hints & 2) != 0) {
            addDisabledHint(info, 2);
        }
        if ((hints & 4) != 0) {
            addDisabledHint(info, 4);
        }
    }

    private void addDisabledHint(ManagedServices.ManagedServiceInfo info, int hint) {
        if (this.mListenersDisablingEffects.indexOfKey(hint) < 0) {
            this.mListenersDisablingEffects.put(hint, new ArraySet<>());
        }
        ArraySet<ComponentName> hintListeners = this.mListenersDisablingEffects.get(hint);
        hintListeners.add(info.component);
    }

    private int calculateHints() {
        int hints = 0;
        for (int i = this.mListenersDisablingEffects.size() - 1; i >= 0; i--) {
            int hint = this.mListenersDisablingEffects.keyAt(i);
            ArraySet<ComponentName> serviceInfoList = this.mListenersDisablingEffects.valueAt(i);
            if (!serviceInfoList.isEmpty()) {
                hints |= hint;
            }
        }
        return hints;
    }

    private long calculateSuppressedEffects() {
        int hints = calculateHints();
        long suppressedEffects = (hints & 1) != 0 ? 0 | 3 : 0L;
        if ((hints & 2) != 0) {
            suppressedEffects |= 1;
        }
        if ((hints & 4) != 0) {
            return suppressedEffects | 2;
        }
        return suppressedEffects;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateInterruptionFilterLocked() {
        int interruptionFilter = this.mZenModeHelper.getZenModeListenerInterruptionFilter();
        if (interruptionFilter == this.mInterruptionFilter) {
            return;
        }
        this.mInterruptionFilter = interruptionFilter;
        scheduleInterruptionFilterChanged(interruptionFilter);
    }

    int correctCategory(int requestedCategoryList, int categoryType, int currentCategoryList) {
        if ((requestedCategoryList & categoryType) != 0 && (currentCategoryList & categoryType) == 0) {
            return requestedCategoryList & (~categoryType);
        }
        if ((requestedCategoryList & categoryType) == 0 && (currentCategoryList & categoryType) != 0) {
            return requestedCategoryList | categoryType;
        }
        return requestedCategoryList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public INotificationManager getBinderService() {
        return INotificationManager.Stub.asInterface(this.mService);
    }

    protected void reportSeen(NotificationRecord r) {
        if (!r.isProxied()) {
            this.mAppUsageStats.reportEvent(r.getSbn().getPackageName(), getRealUserId(r.getSbn().getUserId()), 10);
        }
    }

    protected int calculateSuppressedVisualEffects(NotificationManager.Policy incomingPolicy, NotificationManager.Policy currPolicy, int targetSdkVersion) {
        if (incomingPolicy.suppressedVisualEffects == -1) {
            return incomingPolicy.suppressedVisualEffects;
        }
        int[] effectsIntroducedInP = {4, 8, 16, 32, 64, 128, 256};
        int newSuppressedVisualEffects = incomingPolicy.suppressedVisualEffects;
        if (targetSdkVersion < 28) {
            for (int i = 0; i < effectsIntroducedInP.length; i++) {
                newSuppressedVisualEffects = (newSuppressedVisualEffects & (~effectsIntroducedInP[i])) | (currPolicy.suppressedVisualEffects & effectsIntroducedInP[i]);
            }
            int i2 = newSuppressedVisualEffects & 1;
            if (i2 != 0) {
                newSuppressedVisualEffects = newSuppressedVisualEffects | 8 | 4;
            }
            if ((newSuppressedVisualEffects & 2) != 0) {
                return newSuppressedVisualEffects | 16;
            }
            return newSuppressedVisualEffects;
        }
        boolean hasNewEffects = (newSuppressedVisualEffects + (-2)) - 1 > 0;
        if (hasNewEffects) {
            int newSuppressedVisualEffects2 = newSuppressedVisualEffects & (-4);
            if ((newSuppressedVisualEffects2 & 16) != 0) {
                newSuppressedVisualEffects2 |= 2;
            }
            if ((newSuppressedVisualEffects2 & 8) != 0 && (newSuppressedVisualEffects2 & 4) != 0 && (newSuppressedVisualEffects2 & 128) != 0) {
                return newSuppressedVisualEffects2 | 1;
            }
            return newSuppressedVisualEffects2;
        }
        if ((newSuppressedVisualEffects & 1) != 0) {
            newSuppressedVisualEffects = newSuppressedVisualEffects | 8 | 4 | 128;
        }
        if ((newSuppressedVisualEffects & 2) != 0) {
            return newSuppressedVisualEffects | 16;
        }
        return newSuppressedVisualEffects;
    }

    protected void maybeRecordInterruptionLocked(NotificationRecord r) {
        if (r.isInterruptive() && !r.hasRecordedInterruption()) {
            this.mAppUsageStats.reportInterruptiveNotification(r.getSbn().getPackageName(), r.getChannel().getId(), getRealUserId(r.getSbn().getUserId()));
            Trace.traceBegin(524288L, "notifHistoryAddItem");
            try {
                if (r.getNotification().getSmallIcon() != null) {
                    this.mHistoryManager.addNotification(new NotificationHistory.HistoricalNotification.Builder().setPackage(r.getSbn().getPackageName()).setUid(r.getSbn().getUid()).setUserId(r.getSbn().getNormalizedUserId()).setChannelId(r.getChannel().getId()).setChannelName(r.getChannel().getName().toString()).setPostedTimeMs(System.currentTimeMillis()).setTitle(getHistoryTitle(r.getNotification())).setText(getHistoryText(r.getSbn().getPackageContext(getContext()), r.getNotification())).setIcon(r.getNotification().getSmallIcon()).build());
                }
                Trace.traceEnd(524288L);
                r.setRecordedInterruption(true);
            } catch (Throwable th) {
                Trace.traceEnd(524288L);
                throw th;
            }
        }
    }

    protected void reportForegroundServiceUpdate(final boolean shown, final Notification notification, final int id, final String pkg, final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                NotificationManagerService.this.m5138x55dd8f7e(shown, notification, id, pkg, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportForegroundServiceUpdate$5$com-android-server-notification-NotificationManagerService  reason: not valid java name */
    public /* synthetic */ void m5138x55dd8f7e(boolean shown, Notification notification, int id, String pkg, int userId) {
        this.mAmi.onForegroundServiceNotificationUpdate(shown, notification, id, pkg, userId);
    }

    protected void maybeReportForegroundServiceUpdate(NotificationRecord r, boolean shown) {
        if (r.isForegroundService()) {
            StatusBarNotification sbn = r.getSbn();
            reportForegroundServiceUpdate(shown, sbn.getNotification(), sbn.getId(), sbn.getPackageName(), sbn.getUser().getIdentifier());
        }
    }

    private String getHistoryTitle(Notification n) {
        CharSequence title = null;
        if (n.extras != null && (title = n.extras.getCharSequence("android.title")) == null) {
            title = n.extras.getCharSequence("android.title.big");
        }
        return title == null ? getContext().getResources().getString(17040920) : String.valueOf(title);
    }

    private String getHistoryText(Context appContext, Notification n) {
        CharSequence text = null;
        if (n.extras != null) {
            text = n.extras.getCharSequence("android.text");
            Notification.Builder nb = Notification.Builder.recoverBuilder(appContext, n);
            if (nb.getStyle() instanceof Notification.BigTextStyle) {
                text = ((Notification.BigTextStyle) nb.getStyle()).getBigText();
            } else if (nb.getStyle() instanceof Notification.MessagingStyle) {
                Notification.MessagingStyle ms = (Notification.MessagingStyle) nb.getStyle();
                List<Notification.MessagingStyle.Message> messages = ms.getMessages();
                if (messages != null && messages.size() > 0) {
                    text = messages.get(messages.size() - 1).getText();
                }
            }
            if (TextUtils.isEmpty(text)) {
                text = n.extras.getCharSequence("android.text");
            }
        }
        if (text == null) {
            return null;
        }
        return String.valueOf(text);
    }

    protected void maybeRegisterMessageSent(NotificationRecord r) {
        if (r.isConversation()) {
            if (r.getShortcutInfo() != null) {
                if (this.mPreferencesHelper.setValidMessageSent(r.getSbn().getPackageName(), r.getUid())) {
                    handleSavePolicyFile();
                } else if (r.getNotification().getBubbleMetadata() != null && this.mPreferencesHelper.setValidBubbleSent(r.getSbn().getPackageName(), r.getUid())) {
                    handleSavePolicyFile();
                }
            } else if (this.mPreferencesHelper.setInvalidMessageSent(r.getSbn().getPackageName(), r.getUid())) {
                handleSavePolicyFile();
            }
        }
    }

    protected void reportUserInteraction(NotificationRecord r) {
        this.mAppUsageStats.reportEvent(r.getSbn().getPackageName(), getRealUserId(r.getSbn().getUserId()), 7);
    }

    private int getRealUserId(int userId) {
        if (userId == -1) {
            return 0;
        }
        return userId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ToastRecord getToastRecord(int uid, int pid, String packageName, boolean isSystemToast, IBinder token, CharSequence text, ITransientNotification callback, int duration, Binder windowToken, int displayId, ITransientNotificationCallback textCallback) {
        return callback == null ? new TextToastRecord(this, this.mStatusBar, uid, pid, packageName, isSystemToast, token, text, duration, windowToken, displayId, textCallback) : new CustomToastRecord(this, uid, pid, packageName, isSystemToast, token, callback, duration, windowToken, displayId);
    }

    NotificationManagerInternal getInternalService() {
        return this.mInternalService;
    }

    private MultiRateLimiter createToastRateLimiter() {
        return new MultiRateLimiter.Builder(getContext()).addRateLimits(TOAST_RATE_LIMITS).build();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.notification.NotificationManagerService$10  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass10 extends INotificationManager.Stub {
        AnonymousClass10() {
        }

        public void enqueueTextToast(String pkg, IBinder token, CharSequence text, int duration, int displayId, ITransientNotificationCallback callback) {
            enqueueToast(pkg, token, text, null, duration, displayId, callback);
        }

        public void enqueueToast(String pkg, IBinder token, ITransientNotification callback, int duration, int displayId) {
            enqueueToast(pkg, token, null, callback, duration, displayId, null);
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3372=4] */
        private void enqueueToast(String pkg, IBinder token, CharSequence text, ITransientNotification callback, int duration, int displayId, ITransientNotificationCallback textCallback) {
            ArrayList<ToastRecord> arrayList;
            ArrayList<ToastRecord> arrayList2;
            boolean z;
            int index;
            int N;
            if (NotificationManagerService.DBG) {
                Slog.i(NotificationManagerService.TAG, "enqueueToast pkg=" + pkg + " token=" + token + " duration=" + duration + " displayId=" + displayId);
            }
            if (pkg == null || ((text == null && callback == null) || (!(text == null || callback == null) || token == null))) {
                Slog.e(NotificationManagerService.TAG, "Not enqueuing toast. pkg=" + pkg + " text=" + ((Object) text) + " callback= token=" + token);
                return;
            }
            int callingUid = Binder.getCallingUid();
            NotificationManagerService.this.checkCallerIsSameApp(pkg);
            boolean isSystemToast = NotificationManagerService.this.isCallerSystemOrPhone() || PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkg);
            boolean isAppRenderedToast = callback != null;
            if (checkCanEnqueueToast(pkg, callingUid, isAppRenderedToast, isSystemToast)) {
                ArrayList<ToastRecord> arrayList3 = NotificationManagerService.this.mToastQueue;
                synchronized (arrayList3) {
                    try {
                        try {
                            int callingPid = Binder.getCallingPid();
                            long callingId = Binder.clearCallingIdentity();
                            try {
                                int index2 = NotificationManagerService.this.indexOfToastLocked(pkg, token);
                                try {
                                    if (index2 >= 0) {
                                        ToastRecord record = NotificationManagerService.this.mToastQueue.get(index2);
                                        record.update(duration);
                                        arrayList2 = arrayList3;
                                        z = false;
                                    } else {
                                        int N2 = NotificationManagerService.this.mToastQueue.size();
                                        int i = 0;
                                        int count = 0;
                                        while (i < N2) {
                                            ToastRecord r = NotificationManagerService.this.mToastQueue.get(i);
                                            if (r.pkg.equals(pkg)) {
                                                int count2 = count + 1;
                                                index = index2;
                                                if (count2 >= 5) {
                                                    Slog.e(NotificationManagerService.TAG, "Package has already queued " + count2 + " toasts. Not showing more. Package=" + pkg);
                                                    try {
                                                        Binder.restoreCallingIdentity(callingId);
                                                    } catch (Throwable th) {
                                                        th = th;
                                                        arrayList = arrayList3;
                                                        throw th;
                                                    }
                                                } else {
                                                    N = N2;
                                                    count = count2;
                                                }
                                            } else {
                                                index = index2;
                                                N = N2;
                                            }
                                            i++;
                                            index2 = index;
                                            N2 = N;
                                        }
                                        Binder windowToken = new Binder();
                                        NotificationManagerService.this.mWindowManagerInternal.addWindowToken(windowToken, 2005, displayId, null);
                                        arrayList2 = arrayList3;
                                        z = false;
                                        try {
                                            ToastRecord record2 = NotificationManagerService.this.getToastRecord(callingUid, callingPid, pkg, isSystemToast, token, text, callback, duration, windowToken, displayId, textCallback);
                                            NotificationManagerService.this.mToastQueue.add(record2);
                                            NotificationManagerService.this.keepProcessAliveForToastIfNeededLocked(callingPid);
                                            index2 = NotificationManagerService.this.mToastQueue.size() - 1;
                                        } catch (Throwable th2) {
                                            th = th2;
                                            Binder.restoreCallingIdentity(callingId);
                                            throw th;
                                        }
                                    }
                                    if (index2 == 0) {
                                        NotificationManagerService.this.showNextToastLocked(z);
                                    }
                                    Binder.restoreCallingIdentity(callingId);
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            arrayList = arrayList3;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                    }
                }
            }
        }

        private boolean checkCanEnqueueToast(String pkg, int callingUid, boolean isAppRenderedToast, boolean isSystemToast) {
            boolean isPackageSuspended = isPackagePaused(pkg);
            boolean notificationsDisabledForPackage = !areNotificationsEnabledForPackage(pkg, callingUid);
            long callingIdentity = Binder.clearCallingIdentity();
            try {
                boolean appIsForeground = NotificationManagerService.this.mActivityManager.getUidImportance(callingUid) == 100;
                Binder.restoreCallingIdentity(callingIdentity);
                if (!isSystemToast && ((notificationsDisabledForPackage && !appIsForeground) || isPackageSuspended)) {
                    Slog.e(NotificationManagerService.TAG, "Suppressing toast from package " + pkg + (isPackageSuspended ? " due to package suspended." : " by user request."));
                    return false;
                }
                NotificationManagerService notificationManagerService = NotificationManagerService.this;
                if (notificationManagerService.blockToast(callingUid, isSystemToast, isAppRenderedToast, notificationManagerService.isPackageInForegroundForToast(callingUid))) {
                    Slog.w(NotificationManagerService.TAG, "Blocking custom toast from package " + pkg + " due to package not in the foreground at time the toast was posted");
                    return false;
                }
                return true;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(callingIdentity);
                throw th;
            }
        }

        public void cancelToast(String pkg, IBinder token) {
            Slog.i(NotificationManagerService.TAG, "cancelToast pkg=" + pkg + " token=" + token);
            if (pkg == null || token == null) {
                Slog.e(NotificationManagerService.TAG, "Not cancelling notification. pkg=" + pkg + " token=" + token);
                return;
            }
            synchronized (NotificationManagerService.this.mToastQueue) {
                long callingId = Binder.clearCallingIdentity();
                int index = NotificationManagerService.this.indexOfToastLocked(pkg, token);
                if (index >= 0) {
                    NotificationManagerService.this.cancelToastLocked(index);
                } else {
                    Slog.w(NotificationManagerService.TAG, "Toast already cancelled. pkg=" + pkg + " token=" + token);
                }
                Binder.restoreCallingIdentity(callingId);
            }
        }

        public void setToastRateLimitingEnabled(boolean enable) {
            NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.MANAGE_TOAST_RATE_LIMITING", "App doesn't have the permission to enable/disable toast rate limiting");
            synchronized (NotificationManagerService.this.mToastQueue) {
                int uid = Binder.getCallingUid();
                int userId = UserHandle.getUserId(uid);
                if (enable) {
                    NotificationManagerService.this.mToastRateLimitingDisabledUids.remove(Integer.valueOf(uid));
                    try {
                        String[] packages = NotificationManagerService.this.mPackageManager.getPackagesForUid(uid);
                        if (packages == null) {
                            Slog.e(NotificationManagerService.TAG, "setToastRateLimitingEnabled method haven't found any packages for the  given uid: " + uid + ", toast rate limiter not reset for that uid.");
                            return;
                        }
                        for (String pkg : packages) {
                            NotificationManagerService.this.mToastRateLimiter.clear(userId, pkg);
                        }
                    } catch (RemoteException e) {
                        Slog.e(NotificationManagerService.TAG, "Failed to reset toast rate limiter for given uid", e);
                    }
                } else {
                    NotificationManagerService.this.mToastRateLimitingDisabledUids.add(Integer.valueOf(uid));
                }
            }
        }

        public void finishToken(String pkg, IBinder token) {
            synchronized (NotificationManagerService.this.mToastQueue) {
                long callingId = Binder.clearCallingIdentity();
                int index = NotificationManagerService.this.indexOfToastLocked(pkg, token);
                if (index >= 0) {
                    ToastRecord record = NotificationManagerService.this.mToastQueue.get(index);
                    NotificationManagerService.this.finishWindowTokenLocked(record.windowToken, record.displayId);
                } else {
                    Slog.w(NotificationManagerService.TAG, "Toast already killed. pkg=" + pkg + " token=" + token);
                }
                Binder.restoreCallingIdentity(callingId);
            }
        }

        public void enqueueNotificationWithTag(String pkg, String opPkg, String tag, int id, Notification notification, int userId) throws RemoteException {
            NotificationManagerService.this.enqueueNotificationInternal(pkg, opPkg, Binder.getCallingUid(), Binder.getCallingPid(), tag, id, notification, userId);
        }

        public void cancelNotificationWithTag(String pkg, String opPkg, String tag, int id, int userId) {
            NotificationManagerService.this.cancelNotificationInternal(pkg, opPkg, Binder.getCallingUid(), Binder.getCallingPid(), tag, id, userId);
        }

        public void cancelAllNotifications(String pkg, int userId) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            NotificationManagerService.this.cancelAllNotificationsInt(Binder.getCallingUid(), Binder.getCallingPid(), pkg, null, 0, 64, true, ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, false, "cancelAllNotifications", pkg), 9, null);
        }

        public void silenceNotificationSound() {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.mNotificationDelegate.clearEffects();
        }

        public void setNotificationsEnabledForPackage(String pkg, int uid, boolean enabled) {
            enforceSystemOrSystemUI("setNotificationsEnabledForPackage");
            boolean wasEnabled = NotificationManagerService.this.mPermissionHelper.hasPermission(uid);
            if (wasEnabled == enabled) {
                return;
            }
            NotificationManagerService.this.mPermissionHelper.setNotificationPermission(pkg, UserHandle.getUserId(uid), enabled, true);
            NotificationManagerService.this.sendAppBlockStateChangedBroadcast(pkg, uid, !enabled ? 1 : 0);
            NotificationManagerService.this.mMetricsLogger.write(new LogMaker(147).setType(4).setPackageName(pkg).setSubtype(enabled ? 1 : 0));
            NotificationManagerService.this.mNotificationChannelLogger.logAppNotificationsAllowed(uid, pkg, enabled);
            if (!enabled) {
                NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, pkg, null, 0, 0, true, UserHandle.getUserId(uid), 7, null);
            }
            NotificationManagerService.this.handleSavePolicyFile();
        }

        public void setNotificationsEnabledWithImportanceLockForPackage(String pkg, int uid, boolean enabled) {
            setNotificationsEnabledForPackage(pkg, uid, enabled);
        }

        public boolean areNotificationsEnabled(String pkg) {
            return areNotificationsEnabledForPackage(pkg, Binder.getCallingUid());
        }

        public boolean areNotificationsEnabledForPackage(String pkg, int uid) {
            enforceSystemOrSystemUIOrSamePackage(pkg, "Caller not system or systemui or same package");
            if (UserHandle.getCallingUserId() != UserHandle.getUserId(uid)) {
                NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS", "canNotifyAsPackage for uid " + uid);
            }
            return NotificationManagerService.this.areNotificationsEnabledForPackageInt(pkg, uid);
        }

        public boolean areBubblesAllowed(String pkg) {
            return getBubblePreferenceForPackage(pkg, Binder.getCallingUid()) == 1;
        }

        public boolean areBubblesEnabled(UserHandle user) {
            if (UserHandle.getCallingUserId() != user.getIdentifier()) {
                NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS", "areBubblesEnabled for user " + user.getIdentifier());
            }
            return NotificationManagerService.this.mPreferencesHelper.bubblesEnabled(user);
        }

        public int getBubblePreferenceForPackage(String pkg, int uid) {
            enforceSystemOrSystemUIOrSamePackage(pkg, "Caller not system or systemui or same package");
            if (UserHandle.getCallingUserId() != UserHandle.getUserId(uid)) {
                NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS", "getBubblePreferenceForPackage for uid " + uid);
            }
            return NotificationManagerService.this.mPreferencesHelper.getBubblePreference(pkg, uid);
        }

        public void setBubblesAllowed(String pkg, int uid, int bubblePreference) {
            NotificationManagerService.this.checkCallerIsSystemOrSystemUiOrShell("Caller not system or sysui or shell");
            NotificationManagerService.this.mPreferencesHelper.setBubblesAllowed(pkg, uid, bubblePreference);
            NotificationManagerService.this.handleSavePolicyFile();
        }

        public boolean shouldHideSilentStatusIcons(String callingPkg) {
            NotificationManagerService.this.checkCallerIsSameApp(callingPkg);
            if (NotificationManagerService.this.isCallerSystemOrPhone() || NotificationManagerService.this.mListeners.isListenerPackage(callingPkg)) {
                return NotificationManagerService.this.mPreferencesHelper.shouldHideSilentStatusIcons();
            }
            throw new SecurityException("Only available for notification listeners");
        }

        public void setHideSilentStatusIcons(boolean hide) {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.mPreferencesHelper.setHideSilentStatusIcons(hide);
            NotificationManagerService.this.handleSavePolicyFile();
            NotificationManagerService.this.mListeners.onStatusBarIconsBehaviorChanged(hide);
        }

        public void deleteNotificationHistoryItem(String pkg, int uid, long postedTime) {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.mHistoryManager.deleteNotificationHistoryItem(pkg, uid, postedTime);
        }

        public NotificationListenerFilter getListenerFilter(ComponentName cn, int userId) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mListeners.getNotificationListenerFilter(Pair.create(cn, Integer.valueOf(userId)));
        }

        public void setListenerFilter(ComponentName cn, int userId, NotificationListenerFilter nlf) {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.mListeners.setNotificationListenerFilter(Pair.create(cn, Integer.valueOf(userId)), nlf);
            NotificationManagerService.this.handleSavePolicyFile();
        }

        public int getPackageImportance(String pkg) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            if (NotificationManagerService.this.mPermissionHelper.hasPermission(Binder.getCallingUid())) {
                return 3;
            }
            return 0;
        }

        public boolean isImportanceLocked(String pkg, int uid) {
            NotificationManagerService.this.checkCallerIsSystemOrSystemUi();
            return NotificationManagerService.this.mPreferencesHelper.isImportanceLocked(pkg, uid);
        }

        public boolean canShowBadge(String pkg, int uid) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mPreferencesHelper.canShowBadge(pkg, uid);
        }

        public void setShowBadge(String pkg, int uid, boolean showBadge) {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.mPreferencesHelper.setShowBadge(pkg, uid, showBadge);
            NotificationManagerService.this.handleSavePolicyFile();
        }

        public boolean hasSentValidMsg(String pkg, int uid) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mPreferencesHelper.hasSentValidMsg(pkg, uid);
        }

        public boolean isInInvalidMsgState(String pkg, int uid) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mPreferencesHelper.isInInvalidMsgState(pkg, uid);
        }

        public boolean hasUserDemotedInvalidMsgApp(String pkg, int uid) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mPreferencesHelper.hasUserDemotedInvalidMsgApp(pkg, uid);
        }

        public void setInvalidMsgAppDemoted(String pkg, int uid, boolean isDemoted) {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.mPreferencesHelper.setInvalidMsgAppDemoted(pkg, uid, isDemoted);
            NotificationManagerService.this.handleSavePolicyFile();
        }

        public boolean hasSentValidBubble(String pkg, int uid) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mPreferencesHelper.hasSentValidBubble(pkg, uid);
        }

        public void setNotificationDelegate(String callingPkg, String delegate) {
            NotificationManagerService.this.checkCallerIsSameApp(callingPkg);
            int callingUid = Binder.getCallingUid();
            UserHandle user = UserHandle.getUserHandleForUid(callingUid);
            if (delegate == null) {
                NotificationManagerService.this.mPreferencesHelper.revokeNotificationDelegate(callingPkg, Binder.getCallingUid());
                NotificationManagerService.this.handleSavePolicyFile();
                return;
            }
            try {
                ApplicationInfo info = NotificationManagerService.this.mPackageManager.getApplicationInfo(delegate, 786432L, user.getIdentifier());
                if (info != null) {
                    NotificationManagerService.this.mPreferencesHelper.setNotificationDelegate(callingPkg, callingUid, delegate, info.uid);
                    NotificationManagerService.this.handleSavePolicyFile();
                }
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }

        public String getNotificationDelegate(String callingPkg) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(callingPkg);
            return NotificationManagerService.this.mPreferencesHelper.getNotificationDelegate(callingPkg, Binder.getCallingUid());
        }

        public boolean canNotifyAsPackage(String callingPkg, String targetPkg, int userId) {
            NotificationManagerService.this.checkCallerIsSameApp(callingPkg);
            int callingUid = Binder.getCallingUid();
            UserHandle user = UserHandle.getUserHandleForUid(callingUid);
            if (user.getIdentifier() != userId) {
                NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS", "canNotifyAsPackage for user " + userId);
            }
            if (callingPkg.equals(targetPkg)) {
                return true;
            }
            try {
                ApplicationInfo info = NotificationManagerService.this.mPackageManager.getApplicationInfo(targetPkg, 786432L, userId);
                if (info != null) {
                    return NotificationManagerService.this.mPreferencesHelper.isDelegateAllowed(targetPkg, info.uid, callingPkg, callingUid);
                }
                return false;
            } catch (RemoteException e) {
                return false;
            }
        }

        public void updateNotificationChannelGroupForPackage(String pkg, int uid, NotificationChannelGroup group) throws RemoteException {
            enforceSystemOrSystemUI("Caller not system or systemui");
            NotificationManagerService.this.createNotificationChannelGroup(pkg, uid, group, false, false);
            NotificationManagerService.this.handleSavePolicyFile();
        }

        public void createNotificationChannelGroups(String pkg, ParceledListSlice channelGroupList) throws RemoteException {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            List<NotificationChannelGroup> groups = channelGroupList.getList();
            int groupSize = groups.size();
            for (int i = 0; i < groupSize; i++) {
                NotificationChannelGroup group = groups.get(i);
                NotificationManagerService.this.createNotificationChannelGroup(pkg, Binder.getCallingUid(), group, true, false);
            }
            NotificationManagerService.this.handleSavePolicyFile();
        }

        private void createNotificationChannelsImpl(String pkg, int uid, ParceledListSlice channelsList) {
            createNotificationChannelsImpl(pkg, uid, channelsList, -1);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r2v5, resolved type: com.android.server.notification.NotificationManagerService$NotificationListeners */
        /* JADX DEBUG: Multi-variable search result rejected for r3v7, resolved type: com.android.server.notification.PreferencesHelper */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r12v0 */
        /* JADX WARN: Type inference failed for: r12v1, types: [int, boolean] */
        /* JADX WARN: Type inference failed for: r12v4 */
        private void createNotificationChannelsImpl(String pkg, int uid, ParceledListSlice channelsList, int startingTaskId) {
            boolean z;
            int i;
            List<NotificationChannel> channels = channelsList.getList();
            int channelsSize = channels.size();
            ?? r12 = 1;
            ParceledListSlice<NotificationChannel> oldChannels = NotificationManagerService.this.mPreferencesHelper.getNotificationChannels(pkg, uid, true);
            if (oldChannels != null && !oldChannels.getList().isEmpty()) {
                z = true;
            } else {
                z = false;
            }
            boolean hadChannel = z;
            boolean needsPolicyFileChange = false;
            boolean hasRequestedNotificationPermission = false;
            int i2 = 0;
            while (i2 < channelsSize) {
                NotificationChannel channel = channels.get(i2);
                Objects.requireNonNull(channel, "channel in list is null");
                if (Build.IS_DEBUG_ENABLE && "com.google.android.gms.availability".equals(channel.getId())) {
                    Slog.d(NotificationManagerService.TAG, "ignore notification google play service availability");
                    i = i2;
                } else {
                    i = i2;
                    boolean needsPolicyFileChange2 = NotificationManagerService.this.mPreferencesHelper.createNotificationChannel(pkg, uid, channel, true, NotificationManagerService.this.mConditionProviders.isPackageOrComponentAllowed(pkg, UserHandle.getUserId(uid)));
                    if (needsPolicyFileChange2) {
                        NotificationManagerService.this.mListeners.notifyNotificationChannelChanged(pkg, UserHandle.getUserHandleForUid(uid), NotificationManagerService.this.mPreferencesHelper.getNotificationChannel(pkg, uid, channel.getId(), false), r12);
                        boolean hasChannel = (hadChannel || hasRequestedNotificationPermission) ? r12 : false;
                        if (!hasChannel) {
                            ParceledListSlice<NotificationChannel> currChannels = NotificationManagerService.this.mPreferencesHelper.getNotificationChannels(pkg, uid, r12);
                            hasChannel = (currChannels == null || currChannels.getList().isEmpty()) ? false : r12;
                        }
                        if (!hadChannel && hasChannel && !hasRequestedNotificationPermission && startingTaskId != -1) {
                            if (NotificationManagerService.this.mPermissionPolicyInternal == null) {
                                NotificationManagerService.this.mPermissionPolicyInternal = (PermissionPolicyInternal) LocalServices.getService(PermissionPolicyInternal.class);
                            }
                            NotificationManagerService.this.mHandler.post(new ShowNotificationPermissionPromptRunnable(pkg, UserHandle.getUserId(uid), startingTaskId, NotificationManagerService.this.mPermissionPolicyInternal));
                            needsPolicyFileChange = needsPolicyFileChange2;
                            hasRequestedNotificationPermission = true;
                        }
                    }
                    needsPolicyFileChange = needsPolicyFileChange2;
                }
                i2 = i + 1;
                r12 = 1;
            }
            if (needsPolicyFileChange) {
                NotificationManagerService.this.handleSavePolicyFile();
            }
        }

        public void createNotificationChannels(String pkg, ParceledListSlice channelsList) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            int taskId = -1;
            try {
                int uid = NotificationManagerService.this.mPackageManager.getPackageUid(pkg, 0L, UserHandle.getUserId(Binder.getCallingUid()));
                taskId = NotificationManagerService.this.mAtm.getTaskToShowPermissionDialogOn(pkg, uid);
            } catch (RemoteException e) {
            }
            createNotificationChannelsImpl(pkg, Binder.getCallingUid(), channelsList, taskId);
        }

        public void createNotificationChannelsForPackage(String pkg, int uid, ParceledListSlice channelsList) {
            enforceSystemOrSystemUI("only system can call this");
            createNotificationChannelsImpl(pkg, uid, channelsList);
        }

        public void createConversationNotificationChannelForPackage(String pkg, int uid, NotificationChannel parentChannel, String conversationId) {
            enforceSystemOrSystemUI("only system can call this");
            Preconditions.checkNotNull(parentChannel);
            Preconditions.checkNotNull(conversationId);
            String parentId = parentChannel.getId();
            parentChannel.setId(String.format("%1$s : %2$s", parentId, conversationId));
            parentChannel.setConversationId(parentId, conversationId);
            createNotificationChannelsImpl(pkg, uid, new ParceledListSlice(Arrays.asList(parentChannel)));
            NotificationManagerService.this.mRankingHandler.requestSort();
            NotificationManagerService.this.handleSavePolicyFile();
        }

        public NotificationChannel getNotificationChannel(String callingPkg, int userId, String targetPkg, String channelId) {
            return getConversationNotificationChannel(callingPkg, userId, targetPkg, channelId, true, null);
        }

        public NotificationChannel getConversationNotificationChannel(String callingPkg, int userId, String targetPkg, String channelId, boolean returnParentIfNoConversationChannel, String conversationId) {
            if (canNotifyAsPackage(callingPkg, targetPkg, userId) || NotificationManagerService.this.isCallerIsSystemOrSysemUiOrShell()) {
                int targetUid = -1;
                try {
                    targetUid = NotificationManagerService.this.mPackageManagerClient.getPackageUidAsUser(targetPkg, userId);
                } catch (PackageManager.NameNotFoundException e) {
                }
                return NotificationManagerService.this.mPreferencesHelper.getConversationNotificationChannel(targetPkg, targetUid, channelId, conversationId, returnParentIfNoConversationChannel, false);
            }
            throw new SecurityException("Pkg " + callingPkg + " cannot read channels for " + targetPkg + " in " + userId);
        }

        public NotificationChannel getNotificationChannelForPackage(String pkg, int uid, String channelId, String conversationId, boolean includeDeleted) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mPreferencesHelper.getConversationNotificationChannel(pkg, uid, channelId, conversationId, true, includeDeleted);
        }

        private void enforceDeletingChannelHasNoFgService(String pkg, int userId, String channelId) {
            if (NotificationManagerService.this.mAmi.hasForegroundServiceNotification(pkg, userId, channelId)) {
                Slog.w(NotificationManagerService.TAG, "Package u" + userId + SliceClientPermissions.SliceAuthority.DELIMITER + pkg + " may not delete notification channel '" + channelId + "' with fg service");
                throw new SecurityException("Not allowed to delete channel " + channelId + " with a foreground service");
            }
        }

        public void deleteNotificationChannel(String pkg, String channelId) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            int callingUid = Binder.getCallingUid();
            int callingUser = UserHandle.getUserId(callingUid);
            if ("miscellaneous".equals(channelId)) {
                throw new IllegalArgumentException("Cannot delete default channel");
            }
            enforceDeletingChannelHasNoFgService(pkg, callingUser, channelId);
            NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, pkg, channelId, 0, 0, true, callingUser, 20, null);
            boolean previouslyExisted = NotificationManagerService.this.mPreferencesHelper.deleteNotificationChannel(pkg, callingUid, channelId);
            if (previouslyExisted) {
                NotificationManagerService.this.mArchive.removeChannelNotifications(pkg, callingUser, channelId);
                NotificationManagerService.this.mHistoryManager.deleteNotificationChannel(pkg, callingUid, channelId);
                NotificationManagerService.this.mListeners.notifyNotificationChannelChanged(pkg, UserHandle.getUserHandleForUid(callingUid), NotificationManagerService.this.mPreferencesHelper.getNotificationChannel(pkg, callingUid, channelId, true), 3);
                NotificationManagerService.this.handleSavePolicyFile();
            }
        }

        public NotificationChannelGroup getNotificationChannelGroup(String pkg, String groupId) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            return NotificationManagerService.this.mPreferencesHelper.getNotificationChannelGroupWithChannels(pkg, Binder.getCallingUid(), groupId, false);
        }

        public ParceledListSlice<NotificationChannelGroup> getNotificationChannelGroups(String pkg) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            return NotificationManagerService.this.mPreferencesHelper.getNotificationChannelGroups(pkg, Binder.getCallingUid(), false, false, true);
        }

        public void deleteNotificationChannelGroup(String pkg, String groupId) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            int callingUid = Binder.getCallingUid();
            NotificationChannelGroup groupToDelete = NotificationManagerService.this.mPreferencesHelper.getNotificationChannelGroupWithChannels(pkg, callingUid, groupId, false);
            if (groupToDelete != null) {
                int userId = UserHandle.getUserId(callingUid);
                List<NotificationChannel> groupChannels = groupToDelete.getChannels();
                for (int i = 0; i < groupChannels.size(); i++) {
                    enforceDeletingChannelHasNoFgService(pkg, userId, groupChannels.get(i).getId());
                }
                List<NotificationChannel> deletedChannels = NotificationManagerService.this.mPreferencesHelper.deleteNotificationChannelGroup(pkg, callingUid, groupId);
                int i2 = 0;
                while (i2 < deletedChannels.size()) {
                    NotificationChannel deletedChannel = deletedChannels.get(i2);
                    NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, pkg, deletedChannel.getId(), 0, 0, true, userId, 20, null);
                    NotificationManagerService.this.mListeners.notifyNotificationChannelChanged(pkg, UserHandle.getUserHandleForUid(callingUid), deletedChannel, 3);
                    i2++;
                    deletedChannels = deletedChannels;
                    groupChannels = groupChannels;
                    userId = userId;
                }
                NotificationManagerService.this.mListeners.notifyNotificationChannelGroupChanged(pkg, UserHandle.getUserHandleForUid(callingUid), groupToDelete, 3);
                NotificationManagerService.this.handleSavePolicyFile();
            }
        }

        public void updateNotificationChannelForPackage(String pkg, int uid, NotificationChannel channel) {
            NotificationManagerService.this.checkCallerIsSystemOrSystemUiOrShell("Caller not system or sysui or shell");
            Objects.requireNonNull(channel);
            NotificationManagerService.this.updateNotificationChannelInt(pkg, uid, channel, false);
        }

        public void unlockNotificationChannel(String pkg, int uid, String channelId) {
            NotificationManagerService.this.checkCallerIsSystemOrSystemUiOrShell("Caller not system or sysui or shell");
            NotificationManagerService.this.mPreferencesHelper.unlockNotificationChannelImportance(pkg, uid, channelId);
            NotificationManagerService.this.handleSavePolicyFile();
        }

        public void unlockAllNotificationChannels() {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.mPreferencesHelper.unlockAllNotificationChannels();
            NotificationManagerService.this.handleSavePolicyFile();
        }

        public ParceledListSlice<NotificationChannel> getNotificationChannelsForPackage(String pkg, int uid, boolean includeDeleted) {
            enforceSystemOrSystemUI("getNotificationChannelsForPackage");
            return NotificationManagerService.this.mPreferencesHelper.getNotificationChannels(pkg, uid, includeDeleted);
        }

        public int getNumNotificationChannelsForPackage(String pkg, int uid, boolean includeDeleted) {
            enforceSystemOrSystemUI("getNumNotificationChannelsForPackage");
            return NotificationManagerService.this.getNumNotificationChannelsForPackage(pkg, uid, includeDeleted);
        }

        public boolean onlyHasDefaultChannel(String pkg, int uid) {
            enforceSystemOrSystemUI("onlyHasDefaultChannel");
            return NotificationManagerService.this.mPreferencesHelper.onlyHasDefaultChannel(pkg, uid);
        }

        public int getDeletedChannelCount(String pkg, int uid) {
            enforceSystemOrSystemUI("getDeletedChannelCount");
            return NotificationManagerService.this.mPreferencesHelper.getDeletedChannelCount(pkg, uid);
        }

        public int getBlockedChannelCount(String pkg, int uid) {
            enforceSystemOrSystemUI("getBlockedChannelCount");
            return NotificationManagerService.this.mPreferencesHelper.getBlockedChannelCount(pkg, uid);
        }

        public ParceledListSlice<ConversationChannelWrapper> getConversations(boolean onlyImportant) {
            enforceSystemOrSystemUI("getConversations");
            IntArray userIds = NotificationManagerService.this.mUserProfiles.getCurrentProfileIds();
            ArrayList<ConversationChannelWrapper> conversations = NotificationManagerService.this.mPreferencesHelper.getConversations(userIds, onlyImportant);
            Iterator<ConversationChannelWrapper> it = conversations.iterator();
            while (it.hasNext()) {
                ConversationChannelWrapper conversation = it.next();
                if (NotificationManagerService.this.mShortcutHelper == null) {
                    conversation.setShortcutInfo((ShortcutInfo) null);
                } else {
                    conversation.setShortcutInfo(NotificationManagerService.this.mShortcutHelper.getValidShortcutInfo(conversation.getNotificationChannel().getConversationId(), conversation.getPkg(), UserHandle.of(UserHandle.getUserId(conversation.getUid()))));
                }
            }
            return new ParceledListSlice<>(conversations);
        }

        public ParceledListSlice<NotificationChannelGroup> getNotificationChannelGroupsForPackage(String pkg, int uid, boolean includeDeleted) {
            enforceSystemOrSystemUI("getNotificationChannelGroupsForPackage");
            return NotificationManagerService.this.mPreferencesHelper.getNotificationChannelGroups(pkg, uid, includeDeleted, true, false);
        }

        public ParceledListSlice<ConversationChannelWrapper> getConversationsForPackage(String pkg, int uid) {
            enforceSystemOrSystemUI("getConversationsForPackage");
            ArrayList<ConversationChannelWrapper> conversations = NotificationManagerService.this.mPreferencesHelper.getConversations(pkg, uid);
            Iterator<ConversationChannelWrapper> it = conversations.iterator();
            while (it.hasNext()) {
                ConversationChannelWrapper conversation = it.next();
                if (NotificationManagerService.this.mShortcutHelper == null) {
                    conversation.setShortcutInfo((ShortcutInfo) null);
                } else {
                    conversation.setShortcutInfo(NotificationManagerService.this.mShortcutHelper.getValidShortcutInfo(conversation.getNotificationChannel().getConversationId(), pkg, UserHandle.of(UserHandle.getUserId(uid))));
                }
            }
            return new ParceledListSlice<>(conversations);
        }

        public NotificationChannelGroup getPopulatedNotificationChannelGroupForPackage(String pkg, int uid, String groupId, boolean includeDeleted) {
            enforceSystemOrSystemUI("getPopulatedNotificationChannelGroupForPackage");
            return NotificationManagerService.this.mPreferencesHelper.getNotificationChannelGroupWithChannels(pkg, uid, groupId, includeDeleted);
        }

        public NotificationChannelGroup getNotificationChannelGroupForPackage(String groupId, String pkg, int uid) {
            enforceSystemOrSystemUI("getNotificationChannelGroupForPackage");
            return NotificationManagerService.this.mPreferencesHelper.getNotificationChannelGroup(groupId, pkg, uid);
        }

        public ParceledListSlice<NotificationChannel> getNotificationChannels(String callingPkg, String targetPkg, int userId) {
            if (canNotifyAsPackage(callingPkg, targetPkg, userId) || NotificationManagerService.this.isCallingUidSystem()) {
                int targetUid = -1;
                try {
                    targetUid = NotificationManagerService.this.mPackageManagerClient.getPackageUidAsUser(targetPkg, userId);
                } catch (PackageManager.NameNotFoundException e) {
                }
                return NotificationManagerService.this.mPreferencesHelper.getNotificationChannels(targetPkg, targetUid, false);
            }
            throw new SecurityException("Pkg " + callingPkg + " cannot read channels for " + targetPkg + " in " + userId);
        }

        public ParceledListSlice<NotificationChannel> getNotificationChannelsBypassingDnd(String pkg, int uid) {
            NotificationManagerService.this.checkCallerIsSystem();
            if (!areNotificationsEnabledForPackage(pkg, uid)) {
                return ParceledListSlice.emptyList();
            }
            return NotificationManagerService.this.mPreferencesHelper.getNotificationChannelsBypassingDnd(pkg, uid);
        }

        public boolean areChannelsBypassingDnd() {
            return NotificationManagerService.this.mPreferencesHelper.areChannelsBypassingDnd();
        }

        public void clearData(String packageName, int uid, boolean fromApp) throws RemoteException {
            NotificationManagerService.this.checkCallerIsSystem();
            int userId = UserHandle.getUserId(uid);
            NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, packageName, null, 0, 0, true, UserHandle.getUserId(Binder.getCallingUid()), 21, null);
            boolean packagesChanged = NotificationManagerService.this.mConditionProviders.resetPackage(packageName, userId) | false;
            ArrayMap<Boolean, ArrayList<ComponentName>> changedListeners = NotificationManagerService.this.mListeners.resetComponents(packageName, userId);
            boolean packagesChanged2 = packagesChanged | (changedListeners.get(true).size() > 0 || changedListeners.get(false).size() > 0);
            for (int i = 0; i < changedListeners.get(true).size(); i++) {
                NotificationManagerService.this.mConditionProviders.setPackageOrComponentEnabled(changedListeners.get(true).get(i).getPackageName(), userId, false, true);
            }
            ArrayMap<Boolean, ArrayList<ComponentName>> changedAssistants = NotificationManagerService.this.mAssistants.resetComponents(packageName, userId);
            boolean packagesChanged3 = packagesChanged2 | (changedAssistants.get(true).size() > 0 || changedAssistants.get(false).size() > 0);
            for (int i2 = 1; i2 < changedAssistants.get(true).size(); i2++) {
                NotificationManagerService.this.mAssistants.setPackageOrComponentEnabled(changedAssistants.get(true).get(i2).flattenToString(), userId, true, false);
            }
            if (changedAssistants.get(true).size() > 0) {
                NotificationManagerService.this.mConditionProviders.setPackageOrComponentEnabled(changedAssistants.get(true).get(0).getPackageName(), userId, false, true);
            }
            NotificationManagerService.this.mSnoozeHelper.clearData(UserHandle.getUserId(uid), packageName);
            if (!fromApp) {
                NotificationManagerService.this.mPreferencesHelper.clearData(packageName, uid);
            }
            if (packagesChanged3) {
                NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED").setPackage(packageName).addFlags(67108864), UserHandle.of(userId), null);
            }
            NotificationManagerService.this.handleSavePolicyFile();
        }

        public List<String> getAllowedAssistantAdjustments(String pkg) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            if (!NotificationManagerService.this.isCallerSystemOrPhone() && !NotificationManagerService.this.mAssistants.isPackageAllowed(pkg, UserHandle.getCallingUserId())) {
                throw new SecurityException("Not currently an assistant");
            }
            return NotificationManagerService.this.mAssistants.getAllowedAssistantAdjustments();
        }

        public void allowAssistantAdjustment(String adjustmentType) {
            NotificationManagerService.this.checkCallerIsSystemOrSystemUiOrShell();
            NotificationManagerService.this.mAssistants.allowAdjustmentType(adjustmentType);
            NotificationManagerService.this.handleSavePolicyFile();
        }

        public void disallowAssistantAdjustment(String adjustmentType) {
            NotificationManagerService.this.checkCallerIsSystemOrSystemUiOrShell();
            NotificationManagerService.this.mAssistants.disallowAdjustmentType(adjustmentType);
            NotificationManagerService.this.handleSavePolicyFile();
        }

        @Deprecated
        public StatusBarNotification[] getActiveNotifications(String callingPkg) {
            return getActiveNotificationsWithAttribution(callingPkg, null);
        }

        public StatusBarNotification[] getActiveNotificationsWithAttribution(String callingPkg, String callingAttributionTag) {
            NotificationManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.ACCESS_NOTIFICATIONS", "NotificationManagerService.getActiveNotifications");
            ArrayList<StatusBarNotification> tmp = new ArrayList<>();
            int uid = Binder.getCallingUid();
            final ArrayList<Integer> currentUsers = new ArrayList<>();
            currentUsers.add(-1);
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.notification.NotificationManagerService$10$$ExternalSyntheticLambda0
                public final void runOrThrow() {
                    NotificationManagerService.AnonymousClass10.this.m5141x1d897326(currentUsers);
                }
            });
            if (NotificationManagerService.this.mAppOps.noteOpNoThrow(25, uid, callingPkg, callingAttributionTag, (String) null) == 0) {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    int N = NotificationManagerService.this.mNotificationList.size();
                    for (int i = 0; i < N; i++) {
                        StatusBarNotification sbn = NotificationManagerService.this.mNotificationList.get(i).getSbn();
                        if (currentUsers.contains(Integer.valueOf(sbn.getUserId()))) {
                            tmp.add(sbn);
                        }
                    }
                }
            }
            return (StatusBarNotification[]) tmp.toArray(new StatusBarNotification[tmp.size()]);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getActiveNotificationsWithAttribution$0$com-android-server-notification-NotificationManagerService$10  reason: not valid java name */
        public /* synthetic */ void m5141x1d897326(ArrayList currentUsers) throws Exception {
            int[] profileIds;
            for (int user : NotificationManagerService.this.mUm.getProfileIds(ActivityManager.getCurrentUser(), false)) {
                currentUsers.add(Integer.valueOf(user));
            }
        }

        public ParceledListSlice<StatusBarNotification> getAppActiveNotifications(String pkg, int incomingUserId) {
            ParceledListSlice<StatusBarNotification> parceledListSlice;
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            int userId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), incomingUserId, true, false, "getAppActiveNotifications", pkg);
            synchronized (NotificationManagerService.this.mNotificationLock) {
                ArrayMap<String, StatusBarNotification> map = new ArrayMap<>(NotificationManagerService.this.mNotificationList.size() + NotificationManagerService.this.mEnqueuedNotifications.size());
                int N = NotificationManagerService.this.mNotificationList.size();
                for (int i = 0; i < N; i++) {
                    StatusBarNotification sbn = sanitizeSbn(pkg, userId, NotificationManagerService.this.mNotificationList.get(i).getSbn());
                    if (sbn != null) {
                        map.put(sbn.getKey(), sbn);
                    }
                }
                for (NotificationRecord snoozed : NotificationManagerService.this.mSnoozeHelper.getSnoozed(userId, pkg)) {
                    StatusBarNotification sbn2 = sanitizeSbn(pkg, userId, snoozed.getSbn());
                    if (sbn2 != null) {
                        map.put(sbn2.getKey(), sbn2);
                    }
                }
                int M = NotificationManagerService.this.mEnqueuedNotifications.size();
                for (int i2 = 0; i2 < M; i2++) {
                    StatusBarNotification sbn3 = sanitizeSbn(pkg, userId, NotificationManagerService.this.mEnqueuedNotifications.get(i2).getSbn());
                    if (sbn3 != null) {
                        map.put(sbn3.getKey(), sbn3);
                    }
                }
                ArrayList<StatusBarNotification> list = new ArrayList<>(map.size());
                list.addAll(map.values());
                parceledListSlice = new ParceledListSlice<>(list);
            }
            return parceledListSlice;
        }

        private StatusBarNotification sanitizeSbn(String pkg, int userId, StatusBarNotification sbn) {
            if (sbn.getUserId() == userId && (sbn.getPackageName().equals(pkg) || sbn.getOpPkg().equals(pkg))) {
                Notification notification = sbn.getNotification().clone();
                notification.setAllowlistToken(null);
                return new StatusBarNotification(sbn.getPackageName(), sbn.getOpPkg(), sbn.getId(), sbn.getTag(), sbn.getUid(), sbn.getInitialPid(), notification, sbn.getUser(), sbn.getOverrideGroupKey(), sbn.getPostTime());
            }
            return null;
        }

        @Deprecated
        public StatusBarNotification[] getHistoricalNotifications(String callingPkg, int count, boolean includeSnoozed) {
            return getHistoricalNotificationsWithAttribution(callingPkg, null, count, includeSnoozed);
        }

        public StatusBarNotification[] getHistoricalNotificationsWithAttribution(String callingPkg, String callingAttributionTag, int count, boolean includeSnoozed) {
            NotificationManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.ACCESS_NOTIFICATIONS", "NotificationManagerService.getHistoricalNotifications");
            StatusBarNotification[] tmp = null;
            int uid = Binder.getCallingUid();
            if (NotificationManagerService.this.mAppOps.noteOpNoThrow(25, uid, callingPkg, callingAttributionTag, (String) null) == 0) {
                synchronized (NotificationManagerService.this.mArchive) {
                    tmp = NotificationManagerService.this.mArchive.getArray(NotificationManagerService.this.mUm, count, includeSnoozed);
                }
            }
            return tmp;
        }

        public NotificationHistory getNotificationHistory(String callingPkg, String callingAttributionTag) {
            NotificationManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.ACCESS_NOTIFICATIONS", "NotificationManagerService.getNotificationHistory");
            int uid = Binder.getCallingUid();
            if (NotificationManagerService.this.mAppOps.noteOpNoThrow(25, uid, callingPkg, callingAttributionTag, (String) null) == 0) {
                IntArray currentUserIds = NotificationManagerService.this.mUserProfiles.getCurrentProfileIds();
                Trace.traceBegin(524288L, "notifHistoryReadHistory");
                try {
                    return NotificationManagerService.this.mHistoryManager.readNotificationHistory(currentUserIds.toArray());
                } finally {
                    Trace.traceEnd(524288L);
                }
            }
            return new NotificationHistory();
        }

        public void registerListener(INotificationListener listener, ComponentName component, int userid) {
            enforceSystemOrSystemUI("INotificationManager.registerListener");
            NotificationManagerService.this.mListeners.registerSystemService(listener, component, userid, Binder.getCallingUid());
        }

        public void unregisterListener(INotificationListener token, int userid) {
            NotificationManagerService.this.mListeners.unregisterService((IInterface) token, userid);
        }

        public void cancelNotificationsFromListener(INotificationListener token, String[] keys) {
            int reason;
            Object obj;
            int i;
            int N;
            Object obj2;
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            long identity = Binder.clearCallingIdentity();
            try {
                Object obj3 = NotificationManagerService.this.mNotificationLock;
                try {
                    synchronized (obj3) {
                        try {
                            ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                            if (!NotificationManagerService.this.mAssistants.isServiceTokenValidLocked(token)) {
                                reason = 10;
                            } else {
                                reason = 22;
                            }
                            if (keys != null) {
                                int N2 = keys.length;
                                int i2 = 0;
                                while (i2 < N2) {
                                    NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(keys[i2]);
                                    if (r == null) {
                                        i = i2;
                                        N = N2;
                                        obj2 = obj3;
                                    } else {
                                        int userId = r.getSbn().getUserId();
                                        if (userId != info.userid && userId != -1 && !NotificationManagerService.this.mUserProfiles.isCurrentProfile(userId)) {
                                            i = i2;
                                            N = N2;
                                            obj2 = obj3;
                                        } else {
                                            String packageName = r.getSbn().getPackageName();
                                            String tag = r.getSbn().getTag();
                                            int N3 = r.getSbn().getId();
                                            i = i2;
                                            N = N2;
                                            obj2 = obj3;
                                            cancelNotificationFromListenerLocked(info, callingUid, callingPid, packageName, tag, N3, userId, reason);
                                        }
                                    }
                                    i2 = i + 1;
                                    N2 = N;
                                    obj3 = obj2;
                                }
                                obj = obj3;
                            } else {
                                obj = obj3;
                                NotificationManagerService.this.cancelAllLocked(callingUid, callingPid, info.userid, 11, info, info.supportsProfiles());
                            }
                        } catch (Throwable th) {
                            th = th;
                            Object obj4 = obj3;
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void requestBindListener(ComponentName component) {
            ManagedServices manager;
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(component.getPackageName());
            int uid = Binder.getCallingUid();
            long identity = Binder.clearCallingIdentity();
            try {
                if (NotificationManagerService.this.mAssistants.isComponentEnabledForCurrentProfiles(component)) {
                    manager = NotificationManagerService.this.mAssistants;
                } else {
                    manager = NotificationManagerService.this.mListeners;
                }
                manager.setComponentState(component, UserHandle.getUserId(uid), true);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void requestUnbindListener(INotificationListener token) {
            int uid = Binder.getCallingUid();
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    info.getOwner().setComponentState(info.component, UserHandle.getUserId(uid), false);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setNotificationsShownFromListener(INotificationListener token, String[] keys) {
            int userId;
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    if (keys == null) {
                        return;
                    }
                    ArrayList<NotificationRecord> seen = new ArrayList<>();
                    int n = keys.length;
                    for (int i = 0; i < n; i++) {
                        NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(keys[i]);
                        if (r != null && ((userId = r.getSbn().getUserId()) == info.userid || userId == -1 || NotificationManagerService.this.mUserProfiles.isCurrentProfile(userId))) {
                            seen.add(r);
                            if (!r.isSeen()) {
                                if (NotificationManagerService.DBG) {
                                    Slog.d(NotificationManagerService.TAG, "Marking notification as seen " + keys[i]);
                                }
                                NotificationManagerService.this.reportSeen(r);
                                r.setSeen();
                                NotificationManagerService.this.maybeRecordInterruptionLocked(r);
                            }
                        }
                    }
                    if (!seen.isEmpty()) {
                        NotificationManagerService.this.mAssistants.onNotificationsSeenLocked(seen);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private void cancelNotificationFromListenerLocked(ManagedServices.ManagedServiceInfo info, int callingUid, int callingPid, String pkg, String tag, int id, int userId, int reason) {
            NotificationManagerService.this.cancelNotification(callingUid, callingPid, pkg, tag, id, 0, 2, true, userId, reason, info);
        }

        public void snoozeNotificationUntilContextFromListener(INotificationListener token, String key, String snoozeCriterionId) {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    NotificationManagerService.this.snoozeNotificationInt(key, -1L, snoozeCriterionId, info);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void snoozeNotificationUntilFromListener(INotificationListener token, String key, long duration) {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    NotificationManagerService.this.snoozeNotificationInt(key, duration, null, info);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void unsnoozeNotificationFromAssistant(INotificationListener token, String key) {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mAssistants.checkServiceTokenLocked(token);
                    NotificationManagerService.this.unsnoozeNotificationInt(key, info, false);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void unsnoozeNotificationFromSystemListener(INotificationListener token, String key) {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    if (!info.isSystem) {
                        throw new SecurityException("Not allowed to unsnooze before deadline");
                    }
                    NotificationManagerService.this.unsnoozeNotificationInt(key, info, true);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void migrateNotificationFilter(INotificationListener token, int defaultTypes, List<String> disallowedApps) {
            NotificationListenerFilter nlf;
            int[] profileIds;
            long identity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        try {
                            try {
                                ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                                Pair key = Pair.create(info.component, Integer.valueOf(info.userid));
                                NotificationListenerFilter nlf2 = NotificationManagerService.this.mListeners.getNotificationListenerFilter(key);
                                if (nlf2 != null) {
                                    nlf = nlf2;
                                } else {
                                    nlf = new NotificationListenerFilter();
                                }
                                if (nlf.getDisallowedPackages().isEmpty() && disallowedApps != null) {
                                    for (String pkg : disallowedApps) {
                                        for (int userId : NotificationManagerService.this.mUm.getProfileIds(info.userid, false)) {
                                            try {
                                                int uid = getUidForPackageAndUser(pkg, UserHandle.of(userId));
                                                VersionedPackage vp = new VersionedPackage(pkg, uid);
                                                nlf.addPackage(vp);
                                            } catch (Exception e) {
                                            }
                                        }
                                    }
                                }
                                if (nlf.areAllTypesAllowed()) {
                                    nlf.setTypes(defaultTypes);
                                }
                                NotificationManagerService.this.mListeners.setNotificationListenerFilter(key, nlf);
                                Binder.restoreCallingIdentity(identity);
                            } catch (Throwable th) {
                                th = th;
                                try {
                                    throw th;
                                } catch (Throwable th2) {
                                    th = th2;
                                    Binder.restoreCallingIdentity(identity);
                                    throw th;
                                }
                            }
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
            } catch (Throwable th5) {
                th = th5;
            }
        }

        public void cancelNotificationFromListener(INotificationListener token, String pkg, String tag, int id) {
            int cancelReason;
            Object obj;
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            long identity = Binder.clearCallingIdentity();
            try {
                Object obj2 = NotificationManagerService.this.mNotificationLock;
                try {
                    synchronized (obj2) {
                        try {
                            ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                            if (!NotificationManagerService.this.mAssistants.isServiceTokenValidLocked(token)) {
                                cancelReason = 10;
                            } else {
                                cancelReason = 22;
                            }
                            if (info.supportsProfiles()) {
                                Slog.e(NotificationManagerService.TAG, "Ignoring deprecated cancelNotification(pkg, tag, id) from " + info.component + " use cancelNotification(key) instead.");
                                obj = obj2;
                            } else {
                                obj = obj2;
                                cancelNotificationFromListenerLocked(info, callingUid, callingPid, pkg, tag, id, info.userid, cancelReason);
                            }
                            return;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
                throw th;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public ParceledListSlice<StatusBarNotification> getActiveNotificationsFromListener(INotificationListener token, String[] keys, int trim) {
            ParceledListSlice<StatusBarNotification> parceledListSlice;
            NotificationRecord r;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                boolean getKeys = keys != null;
                int N = getKeys ? keys.length : NotificationManagerService.this.mNotificationList.size();
                ArrayList<StatusBarNotification> list = new ArrayList<>(N);
                for (int i = 0; i < N; i++) {
                    if (getKeys) {
                        r = NotificationManagerService.this.mNotificationsByKey.get(keys[i]);
                    } else {
                        r = NotificationManagerService.this.mNotificationList.get(i);
                    }
                    if (r != null) {
                        StatusBarNotification sbn = r.getSbn();
                        if (NotificationManagerService.this.isVisibleToListener(sbn, r.getNotificationType(), info)) {
                            StatusBarNotification sbnToSend = trim == 0 ? sbn : sbn.cloneLight();
                            list.add(sbnToSend);
                        }
                    }
                }
                parceledListSlice = new ParceledListSlice<>(list);
            }
            return parceledListSlice;
        }

        public ParceledListSlice<StatusBarNotification> getSnoozedNotificationsFromListener(INotificationListener token, int trim) {
            ParceledListSlice<StatusBarNotification> parceledListSlice;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                List<NotificationRecord> snoozedRecords = NotificationManagerService.this.mSnoozeHelper.getSnoozed();
                int N = snoozedRecords.size();
                ArrayList<StatusBarNotification> list = new ArrayList<>(N);
                for (int i = 0; i < N; i++) {
                    NotificationRecord r = snoozedRecords.get(i);
                    if (r != null) {
                        StatusBarNotification sbn = r.getSbn();
                        if (NotificationManagerService.this.isVisibleToListener(sbn, r.getNotificationType(), info)) {
                            StatusBarNotification sbnToSend = trim == 0 ? sbn : sbn.cloneLight();
                            list.add(sbnToSend);
                        }
                    }
                }
                parceledListSlice = new ParceledListSlice<>(list);
            }
            return parceledListSlice;
        }

        public void clearRequestedListenerHints(INotificationListener token) {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    NotificationManagerService.this.removeDisabledHints(info);
                    NotificationManagerService.this.updateListenerHintsLocked();
                    NotificationManagerService.this.updateEffectsSuppressorLocked();
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void requestHintsFromListener(INotificationListener token, int hints) {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    boolean disableEffects = (hints & 7) != 0;
                    if (disableEffects) {
                        NotificationManagerService.this.addDisabledHints(info, hints);
                    } else {
                        NotificationManagerService.this.removeDisabledHints(info, hints);
                    }
                    NotificationManagerService.this.updateListenerHintsLocked();
                    NotificationManagerService.this.updateEffectsSuppressorLocked();
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public int getHintsFromListener(INotificationListener token) {
            int i;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                i = NotificationManagerService.this.mListenerHints;
            }
            return i;
        }

        public void requestInterruptionFilterFromListener(INotificationListener token, int interruptionFilter) throws RemoteException {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    NotificationManagerService.this.mZenModeHelper.requestFromListener(info.component, interruptionFilter);
                    NotificationManagerService.this.updateInterruptionFilterLocked();
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public int getInterruptionFilterFromListener(INotificationListener token) throws RemoteException {
            int i;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                i = NotificationManagerService.this.mInterruptionFilter;
            }
            return i;
        }

        public void setOnNotificationPostedTrimFromListener(INotificationListener token, int trim) throws RemoteException {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                if (info == null) {
                    return;
                }
                NotificationManagerService.this.mListeners.setOnNotificationPostedTrimLocked(info, trim);
            }
        }

        public int getZenMode() {
            return NotificationManagerService.this.mZenModeHelper.getZenMode();
        }

        public ZenModeConfig getZenModeConfig() {
            enforceSystemOrSystemUI("INotificationManager.getZenModeConfig");
            return NotificationManagerService.this.mZenModeHelper.getConfig();
        }

        public void setZenMode(int mode, Uri conditionId, String reason) throws RemoteException {
            enforceSystemOrSystemUI("INotificationManager.setZenMode");
            long identity = Binder.clearCallingIdentity();
            try {
                NotificationManagerService.this.mZenModeHelper.setManualZenMode(mode, conditionId, null, reason);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<ZenModeConfig.ZenRule> getZenRules() throws RemoteException {
            enforcePolicyAccess(Binder.getCallingUid(), "getAutomaticZenRules");
            return NotificationManagerService.this.mZenModeHelper.getZenRules();
        }

        public AutomaticZenRule getAutomaticZenRule(String id) throws RemoteException {
            Objects.requireNonNull(id, "Id is null");
            enforcePolicyAccess(Binder.getCallingUid(), "getAutomaticZenRule");
            return NotificationManagerService.this.mZenModeHelper.getAutomaticZenRule(id);
        }

        public String addAutomaticZenRule(AutomaticZenRule automaticZenRule, String pkg) {
            Objects.requireNonNull(automaticZenRule, "automaticZenRule is null");
            Objects.requireNonNull(automaticZenRule.getName(), "Name is null");
            if (automaticZenRule.getOwner() == null && automaticZenRule.getConfigurationActivity() == null) {
                throw new NullPointerException("Rule must have a conditionproviderservice and/or configuration activity");
            }
            Objects.requireNonNull(automaticZenRule.getConditionId(), "ConditionId is null");
            NotificationManagerService.this.checkCallerIsSameApp(pkg);
            if (automaticZenRule.getZenPolicy() != null && automaticZenRule.getInterruptionFilter() != 2) {
                throw new IllegalArgumentException("ZenPolicy is only applicable to INTERRUPTION_FILTER_PRIORITY filters");
            }
            enforcePolicyAccess(Binder.getCallingUid(), "addAutomaticZenRule");
            String rulePkg = pkg;
            if (NotificationManagerService.this.isCallingAppIdSystem() && automaticZenRule.getOwner() != null) {
                rulePkg = automaticZenRule.getOwner().getPackageName();
            }
            return NotificationManagerService.this.mZenModeHelper.addAutomaticZenRule(rulePkg, automaticZenRule, "addAutomaticZenRule");
        }

        public boolean updateAutomaticZenRule(String id, AutomaticZenRule automaticZenRule) throws RemoteException {
            Objects.requireNonNull(automaticZenRule, "automaticZenRule is null");
            Objects.requireNonNull(automaticZenRule.getName(), "Name is null");
            if (automaticZenRule.getOwner() == null && automaticZenRule.getConfigurationActivity() == null) {
                throw new NullPointerException("Rule must have a conditionproviderservice and/or configuration activity");
            }
            Objects.requireNonNull(automaticZenRule.getConditionId(), "ConditionId is null");
            enforcePolicyAccess(Binder.getCallingUid(), "updateAutomaticZenRule");
            return NotificationManagerService.this.mZenModeHelper.updateAutomaticZenRule(id, automaticZenRule, "updateAutomaticZenRule");
        }

        public boolean removeAutomaticZenRule(String id) throws RemoteException {
            Objects.requireNonNull(id, "Id is null");
            enforcePolicyAccess(Binder.getCallingUid(), "removeAutomaticZenRule");
            return NotificationManagerService.this.mZenModeHelper.removeAutomaticZenRule(id, "removeAutomaticZenRule");
        }

        public boolean removeAutomaticZenRules(String packageName) throws RemoteException {
            Objects.requireNonNull(packageName, "Package name is null");
            enforceSystemOrSystemUI("removeAutomaticZenRules");
            return NotificationManagerService.this.mZenModeHelper.removeAutomaticZenRules(packageName, packageName + "|removeAutomaticZenRules");
        }

        public int getRuleInstanceCount(ComponentName owner) throws RemoteException {
            Objects.requireNonNull(owner, "Owner is null");
            enforceSystemOrSystemUI("getRuleInstanceCount");
            return NotificationManagerService.this.mZenModeHelper.getCurrentInstanceCount(owner);
        }

        public void setAutomaticZenRuleState(String id, Condition condition) {
            Objects.requireNonNull(id, "id is null");
            Objects.requireNonNull(condition, "Condition is null");
            enforcePolicyAccess(Binder.getCallingUid(), "setAutomaticZenRuleState");
            NotificationManagerService.this.mZenModeHelper.setAutomaticZenRuleState(id, condition);
        }

        public void setInterruptionFilter(String pkg, int filter) throws RemoteException {
            enforcePolicyAccess(pkg, "setInterruptionFilter");
            int zen = NotificationManager.zenModeFromInterruptionFilter(filter, -1);
            if (zen == -1) {
                throw new IllegalArgumentException("Invalid filter: " + filter);
            }
            long identity = Binder.clearCallingIdentity();
            try {
                NotificationManagerService.this.mZenModeHelper.setManualZenMode(zen, null, pkg, "setInterruptionFilter");
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void notifyConditions(final String pkg, IConditionProvider provider, final Condition[] conditions) {
            final ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mConditionProviders.checkServiceToken(provider);
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService.10.1
                @Override // java.lang.Runnable
                public void run() {
                    NotificationManagerService.this.mConditionProviders.notifyConditions(pkg, info, conditions);
                }
            });
        }

        public void requestUnbindProvider(IConditionProvider provider) {
            int uid = Binder.getCallingUid();
            long identity = Binder.clearCallingIdentity();
            try {
                ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mConditionProviders.checkServiceToken(provider);
                info.getOwner().setComponentState(info.component, UserHandle.getUserId(uid), false);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void requestBindProvider(ComponentName component) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(component.getPackageName());
            int uid = Binder.getCallingUid();
            long identity = Binder.clearCallingIdentity();
            try {
                NotificationManagerService.this.mConditionProviders.setComponentState(component, UserHandle.getUserId(uid), true);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private void enforceSystemOrSystemUI(String message) {
            if (NotificationManagerService.this.isCallerSystemOrPhone()) {
                return;
            }
            NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.STATUS_BAR_SERVICE", message);
        }

        private void enforceSystemOrSystemUIOrSamePackage(String pkg, String message) {
            try {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            } catch (SecurityException e) {
                NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.STATUS_BAR_SERVICE", message);
            }
        }

        private void enforcePolicyAccess(int uid, String method) {
            if (NotificationManagerService.this.getContext().checkCallingPermission("android.permission.MANAGE_NOTIFICATIONS") == 0) {
                return;
            }
            boolean accessAllowed = false;
            String[] packages = NotificationManagerService.this.mPackageManagerClient.getPackagesForUid(uid);
            for (String str : packages) {
                if (NotificationManagerService.this.mConditionProviders.isPackageOrComponentAllowed(str, UserHandle.getUserId(uid))) {
                    accessAllowed = true;
                }
            }
            if (!accessAllowed) {
                Slog.w(NotificationManagerService.TAG, "Notification policy access denied calling " + method);
                throw new SecurityException("Notification policy access denied");
            }
        }

        private void enforcePolicyAccess(String pkg, String method) {
            if (NotificationManagerService.this.getContext().checkCallingPermission("android.permission.MANAGE_NOTIFICATIONS") == 0) {
                return;
            }
            NotificationManagerService.this.checkCallerIsSameApp(pkg);
            if (!checkPolicyAccess(pkg)) {
                Slog.w(NotificationManagerService.TAG, "Notification policy access denied calling " + method);
                throw new SecurityException("Notification policy access denied");
            }
        }

        private boolean checkPackagePolicyAccess(String pkg) {
            return NotificationManagerService.this.mConditionProviders.isPackageOrComponentAllowed(pkg, getCallingUserHandle().getIdentifier());
        }

        private boolean checkPolicyAccess(String pkg) {
            try {
                int uid = NotificationManagerService.this.getContext().getPackageManager().getPackageUidAsUser(pkg, UserHandle.getCallingUserId());
                if (ActivityManager.checkComponentPermission("android.permission.MANAGE_NOTIFICATIONS", uid, -1, true) == 0) {
                    return true;
                }
                if (!checkPackagePolicyAccess(pkg) && !NotificationManagerService.this.mListeners.isComponentEnabledForPackage(pkg)) {
                    if (NotificationManagerService.this.mDpm == null) {
                        return false;
                    }
                    if (!NotificationManagerService.this.mDpm.isActiveProfileOwner(Binder.getCallingUid()) && !NotificationManagerService.this.mDpm.isActiveDeviceOwner(Binder.getCallingUid())) {
                        return false;
                    }
                }
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpAndUsageStatsPermission(NotificationManagerService.this.getContext(), NotificationManagerService.TAG, pw)) {
                DumpFilter filter = DumpFilter.parseFromArguments(args);
                long token = Binder.clearCallingIdentity();
                try {
                    ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> pkgPermissions = NotificationManagerService.this.getAllUsersNotificationPermissions();
                    if (filter.stats) {
                        NotificationManagerService.this.dumpJson(pw, filter, pkgPermissions);
                    } else if (filter.rvStats) {
                        NotificationManagerService.this.dumpRemoteViewStats(pw, filter);
                    } else if (filter.proto) {
                        NotificationManagerService.this.dumpProto(fd, filter, pkgPermissions);
                    } else if (filter.criticalPriority) {
                        NotificationManagerService.this.dumpNotificationRecords(pw, filter);
                    } else {
                        NotificationManagerService.this.dumpImpl(pw, filter, pkgPermissions);
                        INotificationManagerServiceLice.Instance().onDump(pw);
                    }
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        public ComponentName getEffectsSuppressor() {
            if (NotificationManagerService.this.mEffectsSuppressors.isEmpty()) {
                return null;
            }
            return (ComponentName) NotificationManagerService.this.mEffectsSuppressors.get(0);
        }

        /* JADX WARN: Code restructure failed: missing block: B:13:0x003b, code lost:
            if (r3 == false) goto L15;
         */
        /* JADX WARN: Code restructure failed: missing block: B:14:0x003d, code lost:
            r10.this$0.getContext().enforceCallingPermission("android.permission.READ_CONTACTS", "matchesCallFilter requires listener permission, contacts read access, or system level access");
         */
        /* JADX WARN: Code restructure failed: missing block: B:22:0x0059, code lost:
            if (r3 == false) goto L15;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public boolean matchesCallFilter(Bundle extras) {
            boolean systemAccess = false;
            try {
                enforceSystemOrSystemUI("INotificationManager.matchesCallFilter");
                systemAccess = true;
            } catch (SecurityException e) {
            }
            boolean listenerAccess = false;
            try {
                String[] pkgNames = NotificationManagerService.this.mPackageManager.getPackagesForUid(Binder.getCallingUid());
                for (String str : pkgNames) {
                    listenerAccess |= NotificationManagerService.this.mListeners.hasAllowedListener(str, Binder.getCallingUserHandle().getIdentifier());
                }
                if (!systemAccess) {
                }
            } catch (RemoteException e2) {
                if (!systemAccess) {
                }
            } catch (Throwable th) {
                if (!systemAccess && !listenerAccess) {
                    NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.READ_CONTACTS", "matchesCallFilter requires listener permission, contacts read access, or system level access");
                }
                throw th;
            }
            return NotificationManagerService.this.mZenModeHelper.matchesCallFilter(Binder.getCallingUserHandle(), extras, (ValidateNotificationPeople) NotificationManagerService.this.mRankingHelper.findExtractor(ValidateNotificationPeople.class), NotificationManagerService.MATCHES_CALL_FILTER_CONTACTS_TIMEOUT_MS, 1.0f);
        }

        public void cleanUpCallersAfter(long timeThreshold) {
            enforceSystemOrSystemUI("INotificationManager.cleanUpCallersAfter");
            NotificationManagerService.this.mZenModeHelper.cleanUpCallersAfter(timeThreshold);
        }

        public boolean isSystemConditionProviderEnabled(String path) {
            enforceSystemOrSystemUI("INotificationManager.isSystemConditionProviderEnabled");
            return NotificationManagerService.this.mConditionProviders.isSystemProviderEnabled(path);
        }

        public byte[] getBackupPayload(int user) {
            NotificationManagerService.this.checkCallerIsSystem();
            if (NotificationManagerService.DBG) {
                Slog.d(NotificationManagerService.TAG, "getBackupPayload u=" + user);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                NotificationManagerService.this.writePolicyXml(baos, true, user);
                return baos.toByteArray();
            } catch (IOException e) {
                Slog.w(NotificationManagerService.TAG, "getBackupPayload: error writing payload for user " + user, e);
                return null;
            }
        }

        public void applyRestore(byte[] payload, int user) {
            NotificationManagerService.this.checkCallerIsSystem();
            if (NotificationManagerService.DBG) {
                Slog.d(NotificationManagerService.TAG, "applyRestore u=" + user + " payload=" + (payload != null ? new String(payload, StandardCharsets.UTF_8) : null));
            }
            if (payload == null) {
                Slog.w(NotificationManagerService.TAG, "applyRestore: no payload to restore for user " + user);
                return;
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(payload);
            try {
                NotificationManagerService.this.readPolicyXml(bais, true, user);
                NotificationManagerService.this.handleSavePolicyFile();
            } catch (IOException | NumberFormatException | XmlPullParserException e) {
                Slog.w(NotificationManagerService.TAG, "applyRestore: error reading payload", e);
            }
        }

        public boolean isNotificationPolicyAccessGranted(String pkg) {
            return checkPolicyAccess(pkg);
        }

        public boolean isNotificationPolicyAccessGrantedForPackage(String pkg) {
            enforceSystemOrSystemUIOrSamePackage(pkg, "request policy access status for another package");
            return checkPolicyAccess(pkg);
        }

        public void setNotificationPolicyAccessGranted(String pkg, boolean granted) throws RemoteException {
            setNotificationPolicyAccessGrantedForUser(pkg, getCallingUserHandle().getIdentifier(), granted);
        }

        public void setNotificationPolicyAccessGrantedForUser(String pkg, int userId, boolean granted) {
            NotificationManagerService.this.checkCallerIsSystemOrShell();
            long identity = Binder.clearCallingIdentity();
            try {
                if (NotificationManagerService.this.mAllowedManagedServicePackages.test(pkg, Integer.valueOf(userId), NotificationManagerService.this.mConditionProviders.getRequiredPermission())) {
                    NotificationManagerService.this.mConditionProviders.setPackageOrComponentEnabled(pkg, userId, true, granted);
                    NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED").setPackage(pkg).addFlags(67108864), UserHandle.of(userId), null);
                    NotificationManagerService.this.handleSavePolicyFile();
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public NotificationManager.Policy getNotificationPolicy(String pkg) {
            long identity = Binder.clearCallingIdentity();
            try {
                return NotificationManagerService.this.mZenModeHelper.getNotificationPolicy();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public NotificationManager.Policy getConsolidatedNotificationPolicy() {
            long identity = Binder.clearCallingIdentity();
            try {
                return NotificationManagerService.this.mZenModeHelper.getConsolidatedNotificationPolicy();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setNotificationPolicy(String pkg, NotificationManager.Policy policy) {
            NotificationManager.Policy policy2 = policy;
            enforcePolicyAccess(pkg, "setNotificationPolicy");
            int callingUid = Binder.getCallingUid();
            long identity = Binder.clearCallingIdentity();
            try {
                ApplicationInfo applicationInfo = NotificationManagerService.this.mPackageManager.getApplicationInfo(pkg, 0L, UserHandle.getUserId(callingUid));
                NotificationManager.Policy currPolicy = NotificationManagerService.this.mZenModeHelper.getNotificationPolicy();
                if (applicationInfo.targetSdkVersion < 28) {
                    int priorityCategories = policy2.priorityCategories;
                    policy2 = new NotificationManager.Policy((priorityCategories & (-33) & (-65) & (-129)) | (currPolicy.priorityCategories & 32) | (currPolicy.priorityCategories & 64) | (currPolicy.priorityCategories & 128), policy2.priorityCallSenders, policy2.priorityMessageSenders, policy2.suppressedVisualEffects);
                }
                try {
                    int priorityCategories2 = applicationInfo.targetSdkVersion;
                    if (priorityCategories2 < 30) {
                        int priorityCategories3 = NotificationManagerService.this.correctCategory(policy2.priorityCategories, 256, currPolicy.priorityCategories);
                        policy2 = new NotificationManager.Policy(priorityCategories3, policy2.priorityCallSenders, policy2.priorityMessageSenders, policy2.suppressedVisualEffects, currPolicy.priorityConversationSenders);
                    }
                    int newVisualEffects = NotificationManagerService.this.calculateSuppressedVisualEffects(policy2, currPolicy, applicationInfo.targetSdkVersion);
                    NotificationManager.Policy policy3 = new NotificationManager.Policy(policy2.priorityCategories, policy2.priorityCallSenders, policy2.priorityMessageSenders, newVisualEffects, policy2.priorityConversationSenders);
                    ZenLog.traceSetNotificationPolicy(pkg, applicationInfo.targetSdkVersion, policy3);
                    NotificationManagerService.this.mZenModeHelper.setNotificationPolicy(policy3);
                } catch (RemoteException e) {
                } catch (Throwable th) {
                    th = th;
                    Binder.restoreCallingIdentity(identity);
                    throw th;
                }
            } catch (RemoteException e2) {
            } catch (Throwable th2) {
                th = th2;
            }
            Binder.restoreCallingIdentity(identity);
        }

        public List<String> getEnabledNotificationListenerPackages() {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mListeners.getAllowedPackages(getCallingUserHandle().getIdentifier());
        }

        public List<ComponentName> getEnabledNotificationListeners(int userId) {
            NotificationManagerService.this.checkNotificationListenerAccess();
            return NotificationManagerService.this.mListeners.getAllowedComponents(userId);
        }

        public ComponentName getAllowedNotificationAssistantForUser(int userId) {
            NotificationManagerService.this.checkCallerIsSystemOrSystemUiOrShell();
            List<ComponentName> allowedComponents = NotificationManagerService.this.mAssistants.getAllowedComponents(userId);
            if (allowedComponents.size() > 1) {
                throw new IllegalStateException("At most one NotificationAssistant: " + allowedComponents.size());
            }
            return (ComponentName) CollectionUtils.firstOrNull(allowedComponents);
        }

        public ComponentName getAllowedNotificationAssistant() {
            return getAllowedNotificationAssistantForUser(getCallingUserHandle().getIdentifier());
        }

        public ComponentName getDefaultNotificationAssistant() {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mAssistants.getDefaultFromConfig();
        }

        public void setNASMigrationDoneAndResetDefault(int userId, boolean loadFromConfig) {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.setNASMigrationDone(userId);
            if (loadFromConfig) {
                NotificationManagerService.this.mAssistants.resetDefaultFromConfig();
            } else {
                NotificationManagerService.this.mAssistants.clearDefaults();
            }
        }

        public boolean hasEnabledNotificationListener(String packageName, int userId) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mListeners.isPackageAllowed(packageName, userId);
        }

        public boolean isNotificationListenerAccessGranted(ComponentName listener) {
            Objects.requireNonNull(listener);
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(listener.getPackageName());
            return NotificationManagerService.this.mListeners.isPackageOrComponentAllowed(listener.flattenToString(), getCallingUserHandle().getIdentifier());
        }

        public boolean isNotificationListenerAccessGrantedForUser(ComponentName listener, int userId) {
            Objects.requireNonNull(listener);
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mListeners.isPackageOrComponentAllowed(listener.flattenToString(), userId);
        }

        public boolean isNotificationAssistantAccessGranted(ComponentName assistant) {
            Objects.requireNonNull(assistant);
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(assistant.getPackageName());
            return NotificationManagerService.this.mAssistants.isPackageOrComponentAllowed(assistant.flattenToString(), getCallingUserHandle().getIdentifier());
        }

        public void setNotificationListenerAccessGranted(ComponentName listener, boolean granted, boolean userSet) throws RemoteException {
            setNotificationListenerAccessGrantedForUser(listener, getCallingUserHandle().getIdentifier(), granted, userSet);
        }

        public void setNotificationAssistantAccessGranted(ComponentName assistant, boolean granted) {
            setNotificationAssistantAccessGrantedForUser(assistant, getCallingUserHandle().getIdentifier(), granted);
        }

        public void setNotificationListenerAccessGrantedForUser(ComponentName listener, int userId, boolean granted, boolean userSet) {
            Objects.requireNonNull(listener);
            NotificationManagerService.this.checkNotificationListenerAccess();
            if (!userSet && isNotificationListenerAccessUserSet(listener)) {
                return;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                if (NotificationManagerService.this.mAllowedManagedServicePackages.test(listener.getPackageName(), Integer.valueOf(userId), NotificationManagerService.this.mListeners.getRequiredPermission())) {
                    NotificationManagerService.this.mConditionProviders.setPackageOrComponentEnabled(listener.flattenToString(), userId, false, granted, userSet);
                    NotificationManagerService.this.mListeners.setPackageOrComponentEnabled(listener.flattenToString(), userId, true, granted, userSet);
                    NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED").setPackage(listener.getPackageName()).addFlags(1073741824), UserHandle.of(userId), null);
                    NotificationManagerService.this.handleSavePolicyFile();
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private boolean isNotificationListenerAccessUserSet(ComponentName listener) {
            return NotificationManagerService.this.mListeners.isPackageOrComponentUserSet(listener.flattenToString(), getCallingUserHandle().getIdentifier());
        }

        public void setNotificationAssistantAccessGrantedForUser(ComponentName assistant, int userId, boolean granted) {
            NotificationManagerService.this.checkCallerIsSystemOrSystemUiOrShell();
            for (UserInfo ui : NotificationManagerService.this.mUm.getEnabledProfiles(userId)) {
                NotificationManagerService.this.mAssistants.setUserSet(ui.id, true);
            }
            long identity = Binder.clearCallingIdentity();
            try {
                NotificationManagerService.this.setNotificationAssistantAccessGrantedForUserInternal(assistant, userId, granted, true);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void applyEnqueuedAdjustmentFromAssistant(INotificationListener token, Adjustment adjustment) {
            boolean foundEnqueued = false;
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.mAssistants.checkServiceTokenLocked(token);
                    int N = NotificationManagerService.this.mEnqueuedNotifications.size();
                    for (int i = 0; i < N; i++) {
                        NotificationRecord r = NotificationManagerService.this.mEnqueuedNotifications.get(i);
                        if (Objects.equals(adjustment.getKey(), r.getKey()) && Objects.equals(Integer.valueOf(adjustment.getUser()), Integer.valueOf(r.getUserId())) && NotificationManagerService.this.mAssistants.isSameUser(token, r.getUserId())) {
                            NotificationManagerService.this.applyAdjustment(r, adjustment);
                            r.applyAdjustments();
                            r.calculateImportance();
                            foundEnqueued = true;
                        }
                    }
                    if (!foundEnqueued) {
                        applyAdjustmentFromAssistant(token, adjustment);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void applyAdjustmentFromAssistant(INotificationListener token, Adjustment adjustment) {
            List<Adjustment> adjustments = new ArrayList<>();
            adjustments.add(adjustment);
            applyAdjustmentsFromAssistant(token, adjustments);
        }

        public void applyAdjustmentsFromAssistant(INotificationListener token, List<Adjustment> adjustments) {
            boolean needsSort = false;
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.mAssistants.checkServiceTokenLocked(token);
                    for (Adjustment adjustment : adjustments) {
                        NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(adjustment.getKey());
                        if (r != null && NotificationManagerService.this.mAssistants.isSameUser(token, r.getUserId())) {
                            NotificationManagerService.this.applyAdjustment(r, adjustment);
                            if (adjustment.getSignals().containsKey("key_importance") && adjustment.getSignals().getInt("key_importance") == 0) {
                                cancelNotificationsFromListener(token, new String[]{r.getKey()});
                            } else {
                                r.setPendingLogUpdate(true);
                                needsSort = true;
                            }
                        }
                    }
                }
                if (needsSort) {
                    NotificationManagerService.this.mRankingHandler.requestSort();
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void updateNotificationChannelGroupFromPrivilegedListener(INotificationListener token, String pkg, UserHandle user, NotificationChannelGroup group) throws RemoteException {
            Objects.requireNonNull(user);
            verifyPrivilegedListener(token, user, false);
            NotificationManagerService.this.createNotificationChannelGroup(pkg, getUidForPackageAndUser(pkg, user), group, false, true);
            NotificationManagerService.this.handleSavePolicyFile();
        }

        public void updateNotificationChannelFromPrivilegedListener(INotificationListener token, String pkg, UserHandle user, NotificationChannel channel) throws RemoteException {
            Objects.requireNonNull(channel);
            Objects.requireNonNull(pkg);
            Objects.requireNonNull(user);
            verifyPrivilegedListener(token, user, false);
            NotificationManagerService.this.updateNotificationChannelInt(pkg, getUidForPackageAndUser(pkg, user), channel, true);
        }

        public ParceledListSlice<NotificationChannel> getNotificationChannelsFromPrivilegedListener(INotificationListener token, String pkg, UserHandle user) throws RemoteException {
            Objects.requireNonNull(pkg);
            Objects.requireNonNull(user);
            verifyPrivilegedListener(token, user, true);
            return NotificationManagerService.this.mPreferencesHelper.getNotificationChannels(pkg, getUidForPackageAndUser(pkg, user), false);
        }

        public ParceledListSlice<NotificationChannelGroup> getNotificationChannelGroupsFromPrivilegedListener(INotificationListener token, String pkg, UserHandle user) throws RemoteException {
            Objects.requireNonNull(pkg);
            Objects.requireNonNull(user);
            verifyPrivilegedListener(token, user, true);
            List<NotificationChannelGroup> groups = new ArrayList<>();
            groups.addAll(NotificationManagerService.this.mPreferencesHelper.getNotificationChannelGroups(pkg, getUidForPackageAndUser(pkg, user)));
            return new ParceledListSlice<>(groups);
        }

        public boolean isInCall(String pkg, int uid) {
            NotificationManagerService.this.checkCallerIsSystemOrSystemUiOrShell();
            return NotificationManagerService.this.isCallNotification(pkg, uid);
        }

        public void setPrivateNotificationsAllowed(boolean allow) {
            if (NotificationManagerService.this.getContext().checkCallingPermission("android.permission.CONTROL_KEYGUARD_SECURE_NOTIFICATIONS") != 0) {
                throw new SecurityException("Requires CONTROL_KEYGUARD_SECURE_NOTIFICATIONS permission");
            }
            if (allow != NotificationManagerService.this.mLockScreenAllowSecureNotifications) {
                NotificationManagerService.this.mLockScreenAllowSecureNotifications = allow;
                NotificationManagerService.this.handleSavePolicyFile();
            }
        }

        public boolean getPrivateNotificationsAllowed() {
            if (NotificationManagerService.this.getContext().checkCallingPermission("android.permission.CONTROL_KEYGUARD_SECURE_NOTIFICATIONS") != 0) {
                throw new SecurityException("Requires CONTROL_KEYGUARD_SECURE_NOTIFICATIONS permission");
            }
            return NotificationManagerService.this.mLockScreenAllowSecureNotifications;
        }

        public boolean isPackagePaused(String pkg) {
            Objects.requireNonNull(pkg);
            NotificationManagerService.this.checkCallerIsSameApp(pkg);
            return NotificationManagerService.this.isPackagePausedOrSuspended(pkg, Binder.getCallingUid());
        }

        public boolean isPermissionFixed(String pkg, int userId) {
            enforceSystemOrSystemUI("isPermissionFixed");
            return NotificationManagerService.this.mPermissionHelper.isPermissionFixed(pkg, userId);
        }

        private void verifyPrivilegedListener(INotificationListener token, UserHandle user, boolean assistantAllowed) {
            ManagedServices.ManagedServiceInfo info;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
            }
            if (!NotificationManagerService.this.hasCompanionDevice(info)) {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    if (assistantAllowed) {
                        if (NotificationManagerService.this.mAssistants.isServiceTokenValidLocked(info.service)) {
                        }
                    }
                    throw new SecurityException(info + " does not have access");
                }
            }
            if (!info.enabledAndUserMatches(user.getIdentifier())) {
                throw new SecurityException(info + " does not have access");
            }
        }

        private int getUidForPackageAndUser(String pkg, UserHandle user) throws RemoteException {
            long identity = Binder.clearCallingIdentity();
            try {
                int uid = NotificationManagerService.this.mPackageManager.getPackageUid(pkg, 0L, user.getIdentifier());
                return uid;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.notification.NotificationManagerService$10 */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            new NotificationShellCmd(NotificationManagerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5796=5] */
        public long pullStats(long startNs, int report, boolean doAgg, List<ParcelFileDescriptor> out) {
            NotificationManagerService.this.checkCallerIsSystemOrShell();
            long startMs = TimeUnit.MILLISECONDS.convert(startNs, TimeUnit.NANOSECONDS);
            long identity = Binder.clearCallingIdentity();
            try {
                switch (report) {
                    case 1:
                        try {
                            Slog.e(NotificationManagerService.TAG, "pullStats REPORT_REMOTE_VIEWS from: " + startMs + "  wtih " + doAgg);
                            PulledStats stats = NotificationManagerService.this.mUsageStats.remoteViewStats(startMs, doAgg);
                            try {
                                if (stats == null) {
                                    Slog.e(NotificationManagerService.TAG, "null stats for: " + report);
                                    break;
                                } else {
                                    out.add(stats.toParcelFileDescriptor(report));
                                    Slog.e(NotificationManagerService.TAG, "exiting pullStats with: " + out.size());
                                    long endNs = TimeUnit.NANOSECONDS.convert(stats.endTimeMs(), TimeUnit.MILLISECONDS);
                                    Binder.restoreCallingIdentity(identity);
                                    return endNs;
                                }
                            } catch (IOException e) {
                                e = e;
                                Slog.e(NotificationManagerService.TAG, "exiting pullStats: on error", e);
                                Binder.restoreCallingIdentity(identity);
                                return 0L;
                            }
                        } catch (IOException e2) {
                            e = e2;
                        } catch (Throwable th) {
                            e = th;
                            Binder.restoreCallingIdentity(identity);
                            throw e;
                        }
                }
                Binder.restoreCallingIdentity(identity);
                Slog.e(NotificationManagerService.TAG, "exiting pullStats: bad request");
                return 0L;
            } catch (Throwable th2) {
                e = th2;
            }
        }

        public boolean canPlaySound(String pkg, int uid) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mPreferencesHelper.canPlaySound(pkg, uid);
        }

        public void setPlaySound(String pkg, int uid, boolean playSound) {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.mPreferencesHelper.setPlaySound(pkg, uid, playSound);
            NotificationManagerService.this.handleSavePolicyFile();
        }

        public boolean canPlayVibration(String pkg, int uid) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mPreferencesHelper.canPlayVibration(pkg, uid);
        }

        public void setPlayVibration(String pkg, int uid, boolean playVibration) {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.mPreferencesHelper.setPlayVibration(pkg, uid, playVibration);
            NotificationManagerService.this.handleSavePolicyFile();
        }
    }

    protected void checkNotificationListenerAccess() {
        if (!isCallerSystemOrPhone()) {
            getContext().enforceCallingPermission("android.permission.MANAGE_NOTIFICATION_LISTENERS", "Caller must hold android.permission.MANAGE_NOTIFICATION_LISTENERS");
        }
    }

    protected void setNotificationAssistantAccessGrantedForUserInternal(ComponentName assistant, int baseUserId, boolean granted, boolean userSet) {
        List<UserInfo> users = this.mUm.getEnabledProfiles(baseUserId);
        if (users != null) {
            for (UserInfo user : users) {
                int userId = user.id;
                if (assistant == null) {
                    ComponentName allowedAssistant = (ComponentName) CollectionUtils.firstOrNull(this.mAssistants.getAllowedComponents(userId));
                    if (allowedAssistant != null) {
                        setNotificationAssistantAccessGrantedForUserInternal(allowedAssistant, userId, false, userSet);
                    }
                } else if (!granted || this.mAllowedManagedServicePackages.test(assistant.getPackageName(), Integer.valueOf(userId), this.mAssistants.getRequiredPermission())) {
                    this.mConditionProviders.setPackageOrComponentEnabled(assistant.flattenToString(), userId, false, granted);
                    this.mAssistants.setPackageOrComponentEnabled(assistant.flattenToString(), userId, true, granted, userSet);
                    getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED").setPackage(assistant.getPackageName()).addFlags(1073741824), UserHandle.of(userId), null);
                    handleSavePolicyFile();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyAdjustment(NotificationRecord r, Adjustment adjustment) {
        if (r != null && adjustment.getSignals() != null) {
            Bundle adjustments = adjustment.getSignals();
            Bundle.setDefusable(adjustments, true);
            List<String> toRemove = new ArrayList<>();
            for (String potentialKey : adjustments.keySet()) {
                if (!this.mAssistants.isAdjustmentAllowed(potentialKey)) {
                    toRemove.add(potentialKey);
                }
            }
            for (String removeKey : toRemove) {
                adjustments.remove(removeKey);
            }
            r.addAdjustment(adjustment);
        }
    }

    void addAutogroupKeyLocked(String key) {
        NotificationRecord r = this.mNotificationsByKey.get(key);
        if (r != null && r.getSbn().getOverrideGroupKey() == null) {
            addAutoGroupAdjustment(r, "ranker_group");
            EventLogTags.writeNotificationAutogrouped(key);
            this.mRankingHandler.requestSort();
        }
    }

    void removeAutogroupKeyLocked(String key) {
        NotificationRecord r = this.mNotificationsByKey.get(key);
        if (r == null) {
            Slog.w(TAG, "Failed to remove autogroup " + key);
        } else if (r.getSbn().getOverrideGroupKey() != null) {
            addAutoGroupAdjustment(r, null);
            EventLogTags.writeNotificationUnautogrouped(key);
            this.mRankingHandler.requestSort();
        }
    }

    private void addAutoGroupAdjustment(NotificationRecord r, String overrideGroupKey) {
        Bundle signals = new Bundle();
        signals.putString("key_group_key", overrideGroupKey);
        Adjustment adjustment = new Adjustment(r.getSbn().getPackageName(), r.getKey(), signals, "", r.getSbn().getUserId());
        r.addAdjustment(adjustment);
    }

    void addAutoGroupSummary(int userId, String pkg, String triggeringKey, boolean needsOngoingFlag) {
        NotificationRecord r = createAutoGroupSummary(userId, pkg, triggeringKey, needsOngoingFlag);
        if (r != null) {
            boolean isAppForeground = this.mActivityManager.getPackageImportance(pkg) == 100;
            this.mHandler.post(new EnqueueNotificationRunnable(userId, r, isAppForeground, SystemClock.elapsedRealtime()));
        }
    }

    void clearAutogroupSummaryLocked(int userId, String pkg) {
        NotificationRecord removed;
        ArrayMap<String, String> summaries = this.mAutobundledSummaries.get(Integer.valueOf(userId));
        if (summaries != null && summaries.containsKey(pkg) && (removed = findNotificationByKeyLocked(summaries.remove(pkg))) != null) {
            StatusBarNotification sbn = removed.getSbn();
            cancelNotification(MY_UID, MY_PID, pkg, sbn.getTag(), sbn.getId(), 0, 0, false, userId, 16, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasAutoGroupSummaryLocked(StatusBarNotification sbn) {
        ArrayMap<String, String> summaries = this.mAutobundledSummaries.get(Integer.valueOf(sbn.getUserId()));
        return summaries != null && summaries.containsKey(sbn.getPackageName());
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [6030=5] */
    NotificationRecord createAutoGroupSummary(int userId, String pkg, String triggeringKey, boolean needsOngoingFlag) {
        Object obj;
        ArrayMap<String, String> summaries;
        NotificationRecord summaryRecord;
        ArrayMap<String, String> summaries2;
        boolean isPermissionFixed = this.mPermissionHelper.isPermissionFixed(pkg, userId);
        Object obj2 = this.mNotificationLock;
        synchronized (obj2) {
            try {
                NotificationRecord notificationRecord = this.mNotificationsByKey.get(triggeringKey);
                if (notificationRecord == null) {
                    try {
                    } catch (Throwable th) {
                        th = th;
                        obj = obj2;
                    }
                } else {
                    notificationRecord.getChannel();
                    StatusBarNotification adjustedSbn = notificationRecord.getSbn();
                    int userId2 = adjustedSbn.getUser().getIdentifier();
                    try {
                        int uid = adjustedSbn.getUid();
                        ArrayMap<String, String> summaries3 = this.mAutobundledSummaries.get(Integer.valueOf(userId2));
                        if (summaries3 == null) {
                            try {
                                summaries = new ArrayMap<>();
                            } catch (Throwable th2) {
                                th = th2;
                                obj = obj2;
                            }
                        } else {
                            summaries = summaries3;
                        }
                        this.mAutobundledSummaries.put(Integer.valueOf(userId2), summaries);
                        if (summaries.containsKey(pkg)) {
                            summaryRecord = null;
                        } else {
                            ApplicationInfo appInfo = (ApplicationInfo) adjustedSbn.getNotification().extras.getParcelable("android.appInfo", ApplicationInfo.class);
                            Bundle extras = new Bundle();
                            extras.putParcelable("android.appInfo", appInfo);
                            String channelId = notificationRecord.getChannel().getId();
                            Notification summaryNotification = new Notification.Builder(getContext(), channelId).setSmallIcon(adjustedSbn.getNotification().getSmallIcon()).setGroupSummary(true).setGroupAlertBehavior(2).setGroup("ranker_group").setFlag(1024, true).setFlag(512, true).setFlag(2, needsOngoingFlag).setColor(adjustedSbn.getNotification().color).setLocalOnly(true).build();
                            summaryNotification.extras.putAll(extras);
                            Intent appIntent = getContext().getPackageManager().getLaunchIntentForPackage(pkg);
                            if (appIntent != null) {
                                summaries2 = summaries;
                                summaryNotification = summaryNotification;
                                summaryNotification.contentIntent = this.mAmi.getPendingIntentActivityAsApp(0, appIntent, 67108864, (Bundle) null, pkg, appInfo.uid);
                            } else {
                                summaries2 = summaries;
                            }
                            StatusBarNotification summarySbn = new StatusBarNotification(adjustedSbn.getPackageName(), adjustedSbn.getOpPkg(), Integer.MAX_VALUE, "ranker_group", adjustedSbn.getUid(), adjustedSbn.getInitialPid(), summaryNotification, adjustedSbn.getUser(), "ranker_group", System.currentTimeMillis());
                            NotificationRecord summaryRecord2 = new NotificationRecord(getContext(), summarySbn, notificationRecord.getChannel());
                            summaryRecord2.setImportanceFixed(isPermissionFixed);
                            summaryRecord2.setIsAppImportanceLocked(notificationRecord.getIsAppImportanceLocked());
                            summaries2.put(pkg, summarySbn.getKey());
                            summaryRecord = summaryRecord2;
                        }
                        if (summaryRecord != null) {
                            try {
                                obj = obj2;
                                try {
                                    if (checkDisqualifyingFeatures(userId2, uid, summaryRecord.getSbn().getId(), summaryRecord.getSbn().getTag(), summaryRecord, true)) {
                                        return summaryRecord;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                obj = obj2;
                            }
                        } else {
                            obj = obj2;
                        }
                        return null;
                    } catch (Throwable th5) {
                        th = th5;
                        obj = obj2;
                    }
                }
            } catch (Throwable th6) {
                th = th6;
                obj = obj2;
            }
            while (true) {
                try {
                    break;
                } catch (Throwable th7) {
                    th = th7;
                }
            }
            throw th;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String disableNotificationEffects(NotificationRecord record) {
        if (this.mDisableNotificationEffects) {
            return "booleanState";
        }
        if ((this.mListenerHints & 1) != 0) {
            return "listenerHints";
        }
        if (record != null && record.getAudioAttributes() != null) {
            if ((this.mListenerHints & 2) != 0 && record.getAudioAttributes().getUsage() != 6) {
                return "listenerNoti";
            }
            if ((this.mListenerHints & 4) != 0 && record.getAudioAttributes().getUsage() == 6) {
                return "listenerCall";
            }
        }
        if (this.mCallState != 0 && !this.mZenModeHelper.isCall(record)) {
            return "callState";
        }
        return null;
    }

    protected ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> getAllUsersNotificationPermissions() {
        ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> allPermissions = new ArrayMap<>();
        List<UserInfo> allUsers = this.mUm.getUsers();
        for (UserInfo ui : allUsers) {
            ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> userPermissions = this.mPermissionHelper.getNotificationPermissionValues(ui.getUserHandle().getIdentifier());
            for (Pair<Integer, String> pair : userPermissions.keySet()) {
                allPermissions.put(pair, userPermissions.get(pair));
            }
        }
        return allPermissions;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpJson(PrintWriter pw, DumpFilter filter, ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> pkgPermissions) {
        JSONObject dump = new JSONObject();
        try {
            dump.put(HostingRecord.HOSTING_TYPE_SERVICE, "Notification Manager");
            dump.put("bans", this.mPreferencesHelper.dumpBansJson(filter, pkgPermissions));
            dump.put("ranking", this.mPreferencesHelper.dumpJson(filter, pkgPermissions));
            dump.put("stats", this.mUsageStats.dumpJson(filter));
            dump.put("channels", this.mPreferencesHelper.dumpChannelsJson(filter));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        pw.println(dump);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpRemoteViewStats(PrintWriter pw, DumpFilter filter) {
        PulledStats stats = this.mUsageStats.remoteViewStats(filter.since, true);
        if (stats == null) {
            pw.println("no remote view stats reported.");
        } else {
            stats.dump(1, pw, filter);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpProto(FileDescriptor fd, DumpFilter filter, ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> pkgPermissions) {
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        synchronized (this.mNotificationLock) {
            try {
                try {
                    int N = this.mNotificationList.size();
                    for (int i = 0; i < N; i++) {
                        NotificationRecord nr = this.mNotificationList.get(i);
                        if (!filter.filtered || filter.matches(nr.getSbn())) {
                            nr.dump(proto, CompanionAppsPermissions.APP_PERMISSIONS, filter.redact, 1);
                        }
                    }
                    int N2 = this.mEnqueuedNotifications.size();
                    for (int i2 = 0; i2 < N2; i2++) {
                        NotificationRecord nr2 = this.mEnqueuedNotifications.get(i2);
                        if (!filter.filtered || filter.matches(nr2.getSbn())) {
                            nr2.dump(proto, CompanionAppsPermissions.APP_PERMISSIONS, filter.redact, 0);
                        }
                    }
                    List<NotificationRecord> snoozed = this.mSnoozeHelper.getSnoozed();
                    int N3 = snoozed.size();
                    for (int i3 = 0; i3 < N3; i3++) {
                        NotificationRecord nr3 = snoozed.get(i3);
                        if (!filter.filtered || filter.matches(nr3.getSbn())) {
                            nr3.dump(proto, CompanionAppsPermissions.APP_PERMISSIONS, filter.redact, 2);
                        }
                    }
                    long zenLog = proto.start(1146756268034L);
                    this.mZenModeHelper.dump(proto);
                    for (ComponentName suppressor : this.mEffectsSuppressors) {
                        suppressor.dumpDebug(proto, 2246267895812L);
                    }
                    proto.end(zenLog);
                    long listenersToken = proto.start(1146756268035L);
                    this.mListeners.dump(proto, filter);
                    proto.end(listenersToken);
                    proto.write(1120986464260L, this.mListenerHints);
                    int i4 = 0;
                    while (i4 < this.mListenersDisablingEffects.size()) {
                        long effectsToken = proto.start(2246267895813L);
                        long zenLog2 = zenLog;
                        proto.write(CompanionMessage.MESSAGE_ID, this.mListenersDisablingEffects.keyAt(i4));
                        ArraySet<ComponentName> listeners = this.mListenersDisablingEffects.valueAt(i4);
                        int j = 0;
                        while (j < listeners.size()) {
                            ComponentName componentName = listeners.valueAt(j);
                            componentName.dumpDebug(proto, 2246267895811L);
                            j++;
                            listenersToken = listenersToken;
                        }
                        proto.end(effectsToken);
                        i4++;
                        zenLog = zenLog2;
                        listenersToken = listenersToken;
                    }
                    long assistantsToken = proto.start(1146756268038L);
                    this.mAssistants.dump(proto, filter);
                    proto.end(assistantsToken);
                    long conditionsToken = proto.start(1146756268039L);
                    this.mConditionProviders.dump(proto, filter);
                    proto.end(conditionsToken);
                    long rankingToken = proto.start(1146756268040L);
                    this.mRankingHelper.dump(proto, filter);
                    this.mPreferencesHelper.dump(proto, filter, pkgPermissions);
                    proto.end(rankingToken);
                    proto.flush();
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpNotificationRecords(PrintWriter pw, DumpFilter filter) {
        synchronized (this.mNotificationLock) {
            int N = this.mNotificationList.size();
            if (N > 0) {
                pw.println("  Notification List:");
                for (int i = 0; i < N; i++) {
                    NotificationRecord nr = this.mNotificationList.get(i);
                    if (!filter.filtered || filter.matches(nr.getSbn())) {
                        nr.dump(pw, "    ", getContext(), filter.redact);
                    }
                }
                pw.println("  ");
            }
        }
    }

    void dumpImpl(PrintWriter pw, DumpFilter filter, ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> pkgPermissions) {
        pw.print("Current Notification Manager state");
        if (filter.filtered) {
            pw.print(" (filtered to ");
            pw.print(filter);
            pw.print(")");
        }
        pw.println(':');
        boolean zenOnly = filter.filtered && filter.zen;
        if (!zenOnly) {
            synchronized (this.mToastQueue) {
                int N = this.mToastQueue.size();
                if (N > 0) {
                    pw.println("  Toast Queue:");
                    for (int i = 0; i < N; i++) {
                        this.mToastQueue.get(i).dump(pw, "    ", filter);
                    }
                    pw.println("  ");
                }
            }
        }
        synchronized (this.mNotificationLock) {
            if (!zenOnly) {
                try {
                    if (!filter.normalPriority) {
                        dumpNotificationRecords(pw, filter);
                    }
                    if (!filter.filtered) {
                        int N2 = this.mLights.size();
                        if (N2 > 0) {
                            pw.println("  Lights List:");
                            for (int i2 = 0; i2 < N2; i2++) {
                                if (i2 == N2 - 1) {
                                    pw.print("  > ");
                                } else {
                                    pw.print("    ");
                                }
                                pw.println(this.mLights.get(i2));
                            }
                            pw.println("  ");
                        }
                        pw.println("  mUseAttentionLight=" + this.mUseAttentionLight);
                        pw.println("  mHasLight=" + this.mHasLight);
                        pw.println("  mNotificationPulseEnabled=" + this.mNotificationPulseEnabled);
                        pw.println("  mSoundNotificationKey=" + this.mSoundNotificationKey);
                        pw.println("  mVibrateNotificationKey=" + this.mVibrateNotificationKey);
                        pw.println("  mDisableNotificationEffects=" + this.mDisableNotificationEffects);
                        pw.println("  mCallState=" + callStateToString(this.mCallState));
                        pw.println("  mSystemReady=" + this.mSystemReady);
                        pw.println("  mMaxPackageEnqueueRate=" + this.mMaxPackageEnqueueRate);
                        pw.println("  hideSilentStatusBar=" + this.mPreferencesHelper.shouldHideSilentStatusIcons());
                    }
                    pw.println("  mArchive=" + this.mArchive.toString());
                    this.mArchive.dumpImpl(pw, filter);
                    if (!zenOnly) {
                        int N3 = this.mEnqueuedNotifications.size();
                        if (N3 > 0) {
                            pw.println("  Enqueued Notification List:");
                            for (int i3 = 0; i3 < N3; i3++) {
                                NotificationRecord nr = this.mEnqueuedNotifications.get(i3);
                                if (!filter.filtered || filter.matches(nr.getSbn())) {
                                    nr.dump(pw, "    ", getContext(), filter.redact);
                                }
                            }
                            pw.println("  ");
                        }
                        this.mSnoozeHelper.dump(pw, filter);
                    }
                } finally {
                }
            }
            if (!zenOnly) {
                pw.println("\n  Ranking Config:");
                this.mRankingHelper.dump(pw, "    ", filter);
                pw.println("\n Notification Preferences:");
                this.mPreferencesHelper.dump(pw, "    ", filter, pkgPermissions);
                pw.println("\n  Notification listeners:");
                this.mListeners.dump(pw, filter);
                pw.print("    mListenerHints: ");
                pw.println(this.mListenerHints);
                pw.print("    mListenersDisablingEffects: (");
                int N4 = this.mListenersDisablingEffects.size();
                for (int i4 = 0; i4 < N4; i4++) {
                    int hint = this.mListenersDisablingEffects.keyAt(i4);
                    if (i4 > 0) {
                        pw.print(';');
                    }
                    pw.print("hint[" + hint + "]:");
                    ArraySet<ComponentName> listeners = this.mListenersDisablingEffects.valueAt(i4);
                    int listenerSize = listeners.size();
                    for (int j = 0; j < listenerSize; j++) {
                        if (j > 0) {
                            pw.print(',');
                        }
                        ComponentName listener = listeners.valueAt(j);
                        if (listener != null) {
                            pw.print(listener);
                        }
                    }
                }
                pw.println(')');
                pw.println("\n  Notification assistant services:");
                this.mAssistants.dump(pw, filter);
            }
            if (!filter.filtered || zenOnly) {
                pw.println("\n  Zen Mode:");
                pw.print("    mInterruptionFilter=");
                pw.println(this.mInterruptionFilter);
                this.mZenModeHelper.dump(pw, "    ");
                pw.println("\n  Zen Log:");
                ZenLog.dump(pw, "    ");
            }
            pw.println("\n  Condition providers:");
            this.mConditionProviders.dump(pw, filter);
            pw.println("\n  Group summaries:");
            for (Map.Entry<String, NotificationRecord> entry : this.mSummaryByGroupKey.entrySet()) {
                NotificationRecord r = entry.getValue();
                pw.println("    " + entry.getKey() + " -> " + r.getKey());
                if (this.mNotificationsByKey.get(r.getKey()) != r) {
                    pw.println("!!!!!!LEAK: Record not found in mNotificationsByKey.");
                    r.dump(pw, "      ", getContext(), filter.redact);
                }
            }
            if (!zenOnly) {
                pw.println("\n  Usage Stats:");
                this.mUsageStats.dump(pw, "    ", filter);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.notification.NotificationManagerService$11  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass11 implements NotificationManagerInternal {
        AnonymousClass11() {
        }

        @Override // com.android.server.notification.NotificationManagerInternal
        public NotificationChannel getNotificationChannel(String pkg, int uid, String channelId) {
            return NotificationManagerService.this.mPreferencesHelper.getNotificationChannel(pkg, uid, channelId, false);
        }

        @Override // com.android.server.notification.NotificationManagerInternal
        public NotificationChannelGroup getNotificationChannelGroup(String pkg, int uid, String channelId) {
            return NotificationManagerService.this.mPreferencesHelper.getGroupForChannel(pkg, uid, channelId);
        }

        @Override // com.android.server.notification.NotificationManagerInternal
        public void enqueueNotification(String pkg, String opPkg, int callingUid, int callingPid, String tag, int id, Notification notification, int userId) {
            NotificationManagerService.this.enqueueNotificationInternal(pkg, opPkg, callingUid, callingPid, tag, id, notification, userId);
        }

        @Override // com.android.server.notification.NotificationManagerInternal
        public void cancelNotification(String pkg, String opPkg, int callingUid, int callingPid, String tag, int id, int userId) {
            NotificationManagerService.this.cancelNotificationInternal(pkg, opPkg, callingUid, callingPid, tag, id, userId);
        }

        @Override // com.android.server.notification.NotificationManagerInternal
        public boolean isNotificationShown(String pkg, String tag, int notificationId, int userId) {
            return NotificationManagerService.this.isNotificationShownInternal(pkg, tag, notificationId, userId);
        }

        @Override // com.android.server.notification.NotificationManagerInternal
        public void removeForegroundServiceFlagFromNotification(final String pkg, final int notificationId, final int userId) {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$11$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    NotificationManagerService.AnonymousClass11.this.m5142x756f0929(pkg, notificationId, userId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$removeForegroundServiceFlagFromNotification$0$com-android-server-notification-NotificationManagerService$11  reason: not valid java name */
        public /* synthetic */ void m5142x756f0929(String pkg, int notificationId, int userId) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationManagerService notificationManagerService = NotificationManagerService.this;
                List<NotificationRecord> enqueued = notificationManagerService.findNotificationsByListLocked(notificationManagerService.mEnqueuedNotifications, pkg, null, notificationId, userId);
                for (int i = 0; i < enqueued.size(); i++) {
                    removeForegroundServiceFlagLocked(enqueued.get(i));
                }
                NotificationManagerService notificationManagerService2 = NotificationManagerService.this;
                NotificationRecord r = notificationManagerService2.findNotificationByListLocked(notificationManagerService2.mNotificationList, pkg, null, notificationId, userId);
                if (r != null) {
                    removeForegroundServiceFlagLocked(r);
                    NotificationManagerService.this.mRankingHelper.sort(NotificationManagerService.this.mNotificationList);
                    NotificationManagerService.this.mListeners.notifyPostedLocked(r, r);
                }
            }
        }

        @Override // com.android.server.notification.NotificationManagerInternal
        public void onConversationRemoved(String pkg, int uid, Set<String> shortcuts) {
            NotificationManagerService.this.onConversationRemovedInternal(pkg, uid, shortcuts);
        }

        private void removeForegroundServiceFlagLocked(NotificationRecord r) {
            if (r == null) {
                return;
            }
            StatusBarNotification sbn = r.getSbn();
            sbn.getNotification().flags = r.mOriginalFlags & (-65);
        }

        @Override // com.android.server.notification.NotificationManagerInternal
        public int getNumNotificationChannelsForPackage(String pkg, int uid, boolean includeDeleted) {
            return NotificationManagerService.this.getNumNotificationChannelsForPackage(pkg, uid, includeDeleted);
        }

        @Override // com.android.server.notification.NotificationManagerInternal
        public boolean areNotificationsEnabledForPackage(String pkg, int uid) {
            return NotificationManagerService.this.areNotificationsEnabledForPackageInt(pkg, uid);
        }

        @Override // com.android.server.notification.NotificationManagerInternal
        public void sendReviewPermissionsNotification() {
            if (!NotificationManagerService.this.mShowReviewPermissionsNotification) {
                return;
            }
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManager nm = (NotificationManager) NotificationManagerService.this.getContext().getSystemService(NotificationManager.class);
            nm.notify(NotificationManagerService.TAG, 71, NotificationManagerService.this.createReviewPermissionsNotification());
            Settings.Global.putInt(NotificationManagerService.this.getContext().getContentResolver(), "review_permissions_notification_state", 3);
        }
    }

    int getNumNotificationChannelsForPackage(String pkg, int uid, boolean includeDeleted) {
        return this.mPreferencesHelper.getNotificationChannels(pkg, uid, includeDeleted).getList().size();
    }

    void cancelNotificationInternal(String pkg, String opPkg, int callingUid, int callingPid, String tag, int id, int userId) {
        int userId2 = ActivityManager.handleIncomingUser(callingPid, callingUid, userId, true, false, "cancelNotificationWithTag", pkg);
        int uid = resolveNotificationUid(opPkg, pkg, callingUid, userId2);
        if (uid == -1) {
            Slog.w(TAG, opPkg + ":" + callingUid + " trying to cancel notification for nonexistent pkg " + pkg + " in user " + userId2);
            return;
        }
        if (!Objects.equals(pkg, opPkg)) {
            synchronized (this.mNotificationLock) {
                NotificationRecord r = findNotificationLocked(pkg, tag, id, userId2);
                if (r != null && !Objects.equals(opPkg, r.getSbn().getOpPkg())) {
                    throw new SecurityException(opPkg + " does not have permission to cancel a notification they did not post " + tag + " " + id);
                }
            }
        }
        int mustNotHaveFlags = isCallingUidSystem() ? 0 : 1088;
        cancelNotification(uid, callingPid, pkg, tag, id, 0, mustNotHaveFlags, false, userId2, 8, null);
    }

    boolean isNotificationShownInternal(String pkg, String tag, int notificationId, int userId) {
        boolean z;
        synchronized (this.mNotificationLock) {
            z = findNotificationLocked(pkg, tag, notificationId, userId) != null;
        }
        return z;
    }

    void enqueueNotificationInternal(String pkg, String opPkg, int callingUid, int callingPid, String tag, int id, Notification notification, int incomingUserId) {
        enqueueNotificationInternal(pkg, opPkg, callingUid, callingPid, tag, id, notification, incomingUserId, false);
    }

    void enqueueNotificationInternal(String pkg, String opPkg, int callingUid, int callingPid, String tag, int id, Notification notification, int incomingUserId, boolean postSilently) {
        String channelId;
        boolean z;
        UserHandle user;
        ShortcutInfo shortcutInfo;
        int intentCount;
        int intentCount2;
        if (DBG) {
            Slog.v(TAG, "enqueueNotificationInternal: pkg=" + pkg + " id=" + id + " notification=" + notification);
        }
        if (pkg == null || notification == null) {
            throw new IllegalArgumentException("null not allowed: pkg=" + pkg + " id=" + id + " notification=" + notification);
        }
        final int userId = ActivityManager.handleIncomingUser(callingPid, callingUid, incomingUserId, true, false, "enqueueNotification", pkg);
        UserHandle user2 = UserHandle.of(userId);
        int notificationUid = resolveNotificationUid(opPkg, pkg, callingUid, userId);
        if (notificationUid == -1) {
            throw new SecurityException("Caller " + opPkg + ":" + callingUid + " trying to post for invalid pkg " + pkg + " in user " + incomingUserId);
        }
        checkRestrictedCategories(notification);
        try {
            fixNotification(notification, pkg, tag, id, userId);
            ActivityManagerInternal.ServiceNotificationPolicy policy = this.mAmi.applyForegroundServiceNotification(notification, tag, id, pkg, userId);
            if (policy == ActivityManagerInternal.ServiceNotificationPolicy.UPDATE_ONLY && !isNotificationShownInternal(pkg, tag, id, userId)) {
                reportForegroundServiceUpdate(false, notification, id, pkg, userId);
                return;
            }
            this.mUsageStats.registerEnqueuedByApp(pkg);
            StatusBarNotification n = new StatusBarNotification(pkg, opPkg, id, tag, notificationUid, callingPid, notification, user2, (String) null, System.currentTimeMillis());
            String channelId2 = notification.getChannelId();
            if (this.mIsTelevision && new Notification.TvExtender(notification).getChannelId() != null) {
                String channelId3 = new Notification.TvExtender(notification).getChannelId();
                channelId = channelId3;
            } else {
                channelId = channelId2;
            }
            String shortcutId = n.getShortcutId();
            NotificationChannel channel = this.mPreferencesHelper.getConversationNotificationChannel(pkg, notificationUid, channelId, shortcutId, true, false);
            if (channel == null) {
                String noChannelStr = "No Channel found for pkg=" + pkg + ", channelId=" + channelId + ", id=" + id + ", tag=" + tag + ", opPkg=" + opPkg + ", callingUid=" + callingUid + ", userId=" + userId + ", incomingUserId=" + incomingUserId + ", notificationUid=" + notificationUid + ", notification=" + notification;
                Slog.e(TAG, noChannelStr);
                boolean appNotificationsOff = true ^ this.mPermissionHelper.hasPermission(notificationUid);
                if (!appNotificationsOff) {
                    doChannelWarningToast(notificationUid, "Developer warning for package \"" + pkg + "\"\nFailed to post notification on channel \"" + channelId + "\"\nSee log for more details");
                    return;
                }
                return;
            }
            final NotificationRecord r = new NotificationRecord(getContext(), n, channel);
            r.setIsAppImportanceLocked(this.mPermissionHelper.isPermissionUserSet(pkg, userId));
            r.setPostSilently(postSilently);
            r.setFlagBubbleRemoved(false);
            r.setPkgAllowedAsConvo(this.mMsgPkgsAllowedAsConvos.contains(pkg));
            boolean isImportanceFixed = this.mPermissionHelper.isPermissionFixed(pkg, userId);
            r.setImportanceFixed(isImportanceFixed);
            if ((notification.flags & 64) != 0) {
                boolean fgServiceShown = channel.isFgServiceShown();
                if (((channel.getUserLockedFields() & 4) == 0 || !fgServiceShown) && (r.getImportance() == 1 || r.getImportance() == 0)) {
                    if (TextUtils.isEmpty(channelId)) {
                        z = false;
                    } else if ("miscellaneous".equals(channelId)) {
                        z = false;
                    } else {
                        channel.setImportance(2);
                        r.setSystemImportance(2);
                        if (!fgServiceShown) {
                            channel.unlockFields(4);
                            channel.setFgServiceShown(true);
                        }
                        this.mPreferencesHelper.updateNotificationChannel(pkg, notificationUid, channel, false);
                        r.updateNotificationChannel(channel);
                        z = false;
                    }
                    r.setSystemImportance(2);
                } else if (fgServiceShown || TextUtils.isEmpty(channelId)) {
                    z = false;
                } else if ("miscellaneous".equals(channelId)) {
                    z = false;
                } else {
                    channel.setFgServiceShown(true);
                    r.updateNotificationChannel(channel);
                    z = false;
                }
            } else {
                z = false;
            }
            ShortcutHelper shortcutHelper = this.mShortcutHelper;
            if (shortcutHelper != null) {
                user = user2;
                shortcutInfo = shortcutHelper.getValidShortcutInfo(notification.getShortcutId(), pkg, user);
            } else {
                user = user2;
                shortcutInfo = null;
            }
            ShortcutInfo info = shortcutInfo;
            if (notification.getShortcutId() != null && info == null) {
                Slog.w(TAG, "notification " + r.getKey() + " added an invalid shortcut");
            }
            r.setShortcutInfo(info);
            r.setHasSentValidMsg(this.mPreferencesHelper.hasSentValidMsg(pkg, notificationUid));
            r.userDemotedAppFromConvoSpace(this.mPreferencesHelper.hasUserDemotedInvalidMsgApp(pkg, notificationUid));
            INotificationManagerServiceLice.Instance().onUpdateNotificationRecordImportance(getContext(), pkg, notificationUid, r);
            UserHandle user3 = user;
            if (!checkDisqualifyingFeatures(userId, notificationUid, id, tag, r, r.getSbn().getOverrideGroupKey() != null ? true : z)) {
                return;
            }
            if (info != null) {
                this.mShortcutHelper.cacheShortcut(info, user3);
            }
            if (notification.allPendingIntents != null && (intentCount = notification.allPendingIntents.size()) > 0) {
                long duration = ((DeviceIdleInternal) LocalServices.getService(DeviceIdleInternal.class)).getNotificationAllowlistDuration();
                int i = 0;
                while (i < intentCount) {
                    PendingIntent pendingIntent = (PendingIntent) notification.allPendingIntents.valueAt(i);
                    if (pendingIntent == null) {
                        intentCount2 = intentCount;
                    } else {
                        ActivityManagerInternal activityManagerInternal = this.mAmi;
                        IIntentSender target = pendingIntent.getTarget();
                        IBinder iBinder = ALLOWLIST_TOKEN;
                        activityManagerInternal.setPendingIntentAllowlistDuration(target, iBinder, duration, 0, 310, "NotificationManagerService");
                        intentCount2 = intentCount;
                        this.mAmi.setPendingIntentAllowBgActivityStarts(pendingIntent.getTarget(), iBinder, 7);
                    }
                    i++;
                    intentCount = intentCount2;
                }
            }
            long token = Binder.clearCallingIdentity();
            try {
                final boolean isAppForeground = this.mActivityManager.getPackageImportance(pkg) == 100 ? true : z;
                Binder.restoreCallingIdentity(token);
                if (ITranNotificationManagerService.Instance().onEnqueueNotificationInternal(this.mHandler, new Supplier() { // from class: com.android.server.notification.NotificationManagerService$$ExternalSyntheticLambda7
                    @Override // java.util.function.Supplier
                    public final Object get() {
                        return NotificationManagerService.this.m5131xcb85fb54(userId, r, isAppForeground);
                    }
                }, r, n, callingUid)) {
                    return;
                }
                this.mHandler.post(new EnqueueNotificationRunnable(userId, r, isAppForeground, SystemClock.elapsedRealtime()));
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        } catch (Exception e) {
            if (notification.isForegroundService()) {
                throw new SecurityException("Invalid FGS notification", e);
            }
            Slog.e(TAG, "Cannot fix notification", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$enqueueNotificationInternal$6$com-android-server-notification-NotificationManagerService  reason: not valid java name */
    public /* synthetic */ Runnable m5131xcb85fb54(int userId, NotificationRecord r, boolean isAppForeground) {
        return new EnqueueNotificationRunnable(userId, r, isAppForeground, SystemClock.elapsedRealtime());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onConversationRemovedInternal(String pkg, int uid, Set<String> shortcuts) {
        checkCallerIsSystem();
        Preconditions.checkStringNotEmpty(pkg);
        this.mHistoryManager.deleteConversations(pkg, uid, shortcuts);
        List<String> deletedChannelIds = this.mPreferencesHelper.deleteConversations(pkg, uid, shortcuts);
        for (String channelId : deletedChannelIds) {
            cancelAllNotificationsInt(MY_UID, MY_PID, pkg, channelId, 0, 0, true, UserHandle.getUserId(uid), 20, null);
        }
        handleSavePolicyFile();
    }

    protected void fixNotification(Notification notification, String pkg, String tag, int id, int userId) throws PackageManager.NameNotFoundException, RemoteException {
        ApplicationInfo ai = this.mPackageManagerClient.getApplicationInfoAsUser(pkg, 268435456, userId == -1 ? 0 : userId);
        Notification.addFieldsFromContext(ai, notification);
        int canColorize = this.mPackageManagerClient.checkPermission("android.permission.USE_COLORIZED_NOTIFICATIONS", pkg);
        if (canColorize == 0) {
            notification.flags |= 2048;
        } else {
            notification.flags &= -2049;
        }
        if (notification.fullScreenIntent != null && ai.targetSdkVersion >= 29) {
            int fullscreenIntentPermission = this.mPackageManagerClient.checkPermission("android.permission.USE_FULL_SCREEN_INTENT", pkg);
            if (fullscreenIntentPermission != 0) {
                notification.fullScreenIntent = null;
                Slog.w(TAG, "Package " + pkg + ": Use of fullScreenIntent requires the USE_FULL_SCREEN_INTENT permission");
            }
        }
        if (notification.isStyle(Notification.CallStyle.class)) {
            Notification.Builder builder = Notification.Builder.recoverBuilder(getContext(), notification);
            Notification.CallStyle style = (Notification.CallStyle) builder.getStyle();
            List<Notification.Action> actions = style.getActionsListWithSystemActions();
            notification.actions = new Notification.Action[actions.size()];
            actions.toArray(notification.actions);
        }
        if (notification.isStyle(Notification.MediaStyle.class) || notification.isStyle(Notification.DecoratedMediaCustomViewStyle.class)) {
            int hasMediaContentControlPermission = this.mPackageManager.checkPermission("android.permission.MEDIA_CONTENT_CONTROL", pkg, userId);
            if (hasMediaContentControlPermission != 0) {
                notification.extras.remove("android.mediaRemoteDevice");
                notification.extras.remove("android.mediaRemoteIcon");
                notification.extras.remove("android.mediaRemoteIntent");
                if (DBG) {
                    Slog.w(TAG, "Package " + pkg + ": Use of setRemotePlayback requires the MEDIA_CONTENT_CONTROL permission");
                }
            }
        }
        checkRemoteViews(pkg, tag, id, notification);
    }

    private void checkRemoteViews(String pkg, String tag, int id, Notification notification) {
        if (removeRemoteView(pkg, tag, id, notification.contentView)) {
            notification.contentView = null;
        }
        if (removeRemoteView(pkg, tag, id, notification.bigContentView)) {
            notification.bigContentView = null;
        }
        if (removeRemoteView(pkg, tag, id, notification.headsUpContentView)) {
            notification.headsUpContentView = null;
        }
        if (notification.publicVersion != null) {
            if (removeRemoteView(pkg, tag, id, notification.publicVersion.contentView)) {
                notification.publicVersion.contentView = null;
            }
            if (removeRemoteView(pkg, tag, id, notification.publicVersion.bigContentView)) {
                notification.publicVersion.bigContentView = null;
            }
            if (removeRemoteView(pkg, tag, id, notification.publicVersion.headsUpContentView)) {
                notification.publicVersion.headsUpContentView = null;
            }
        }
    }

    private boolean removeRemoteView(String pkg, String tag, int id, RemoteViews contentView) {
        if (contentView == null) {
            return false;
        }
        int contentViewSize = contentView.estimateMemoryUsage();
        if (contentViewSize > this.mWarnRemoteViewsSizeBytes && contentViewSize < this.mStripRemoteViewsSizeBytes) {
            Slog.w(TAG, "RemoteViews too large on pkg: " + pkg + " tag: " + tag + " id: " + id + " this might be stripped in a future release");
        }
        if (contentViewSize < this.mStripRemoteViewsSizeBytes) {
            return false;
        }
        this.mUsageStats.registerImageRemoved(pkg);
        Slog.w(TAG, "Removed too large RemoteViews (" + contentViewSize + " bytes) on pkg: " + pkg + " tag: " + tag + " id: " + id);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNotificationBubbleFlags(NotificationRecord r, boolean isAppForeground) {
        Notification notification = r.getNotification();
        Notification.BubbleMetadata metadata = notification.getBubbleMetadata();
        if (metadata == null) {
            return;
        }
        if (!isAppForeground) {
            int flags = metadata.getFlags();
            metadata.setFlags(flags & (-2));
        }
        if (!metadata.isBubbleSuppressable()) {
            int flags2 = metadata.getFlags();
            metadata.setFlags(flags2 & (-9));
        }
    }

    protected void doChannelWarningToast(int forUid, final CharSequence toastText) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.notification.NotificationManagerService$$ExternalSyntheticLambda5
            public final void runOrThrow() {
                NotificationManagerService.this.m5130xf892ca40(toastText);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$doChannelWarningToast$7$com-android-server-notification-NotificationManagerService  reason: not valid java name */
    public /* synthetic */ void m5130xf892ca40(CharSequence toastText) throws Exception {
        boolean z = Build.IS_DEBUGGABLE;
        ContentResolver contentResolver = getContext().getContentResolver();
        int defaultWarningEnabled = z ? 1 : 0;
        boolean warningEnabled = Settings.Global.getInt(contentResolver, "show_notification_channel_warnings", defaultWarningEnabled) != 0;
        if (warningEnabled && !Build.IS_DEBUG_ENABLE) {
            Toast toast = Toast.makeText(getContext(), this.mHandler.getLooper(), toastText, 0);
            toast.show();
        }
    }

    int resolveNotificationUid(String callingPkg, String targetPkg, int callingUid, int userId) {
        if (userId == -1) {
            userId = 0;
        }
        if (isCallerSameApp(targetPkg, callingUid, userId) && (TextUtils.equals(callingPkg, targetPkg) || isCallerSameApp(callingPkg, callingUid, userId))) {
            return callingUid;
        }
        int targetUid = -1;
        try {
            targetUid = this.mPackageManagerClient.getPackageUidAsUser(targetPkg, userId);
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (isCallerAndroid(callingPkg, callingUid) || this.mPreferencesHelper.isDelegateAllowed(targetPkg, targetUid, callingPkg, callingUid)) {
            return targetUid;
        }
        throw new SecurityException("Caller " + callingPkg + ":" + callingUid + " cannot post for pkg " + targetPkg + " in user " + userId);
    }

    public boolean hasFlag(int flags, int flag) {
        return (flags & flag) != 0;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [6959=4] */
    boolean checkDisqualifyingFeatures(int userId, int uid, int id, String tag, NotificationRecord r, boolean isAutogroup) {
        boolean isBlocked;
        Notification.Action[] actionArr;
        int count;
        Notification n = r.getNotification();
        String pkg = r.getSbn().getPackageName();
        boolean isSystemNotification = isUidSystemOrPhone(uid) || PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkg);
        boolean isNotificationFromListener = this.mListeners.isListenerPackage(pkg);
        if (!isSystemNotification && !isNotificationFromListener) {
            int callingUid = Binder.getCallingUid();
            synchronized (this.mNotificationLock) {
                try {
                    if (this.mNotificationsByKey.get(r.getSbn().getKey()) == null) {
                        try {
                            if (isCallerInstantApp(callingUid, userId)) {
                                throw new SecurityException("Instant app " + pkg + " cannot create notifications");
                            }
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
                        if (this.mNotificationsByKey.get(r.getSbn().getKey()) != null) {
                            try {
                                if (!r.getNotification().hasCompletedProgress() && !isAutogroup) {
                                    float appEnqueueRate = this.mUsageStats.getAppEnqueueRate(pkg);
                                    if (appEnqueueRate > this.mMaxPackageEnqueueRate) {
                                        this.mUsageStats.registerOverRateQuota(pkg);
                                        long now = SystemClock.elapsedRealtime();
                                        if (now - this.mLastOverRateLogTime > MIN_PACKAGE_OVERRATE_LOG_INTERVAL) {
                                            Slog.e(TAG, "Package enqueue rate is " + appEnqueueRate + ". Shedding " + r.getSbn().getKey() + ". package=" + pkg);
                                            this.mLastOverRateLogTime = now;
                                        }
                                        return false;
                                    }
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                while (true) {
                                    break;
                                    break;
                                }
                                throw th;
                            }
                        }
                        if (!n.isForegroundService() && (count = getNotificationCount(pkg, userId, id, tag)) >= 50) {
                            this.mUsageStats.registerOverCountQuota(pkg);
                            Slog.e(TAG, "Package has already posted or enqueued " + count + " notifications.  Not showing more.  package=" + pkg);
                            return false;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                } catch (Throwable th5) {
                    th = th5;
                }
            }
        }
        if (n.getBubbleMetadata() == null || n.getBubbleMetadata().getIntent() == null || !hasFlag(this.mAmi.getPendingIntentFlags(n.getBubbleMetadata().getIntent().getTarget()), 67108864)) {
            if (n.actions != null) {
                for (Notification.Action action : n.actions) {
                    if (!(action.getRemoteInputs() == null && action.getDataOnlyRemoteInputs() == null) && hasFlag(this.mAmi.getPendingIntentFlags(action.actionIntent.getTarget()), 67108864)) {
                        throw new IllegalArgumentException(r.getKey() + " Not posted. PendingIntents attached to actions with remote inputs must be mutable");
                    }
                }
            }
            if (r.getSystemGeneratedSmartActions() != null) {
                Iterator<Notification.Action> it = r.getSystemGeneratedSmartActions().iterator();
                while (it.hasNext()) {
                    Notification.Action action2 = it.next();
                    if (action2.getRemoteInputs() != null || action2.getDataOnlyRemoteInputs() != null) {
                        if (hasFlag(this.mAmi.getPendingIntentFlags(action2.actionIntent.getTarget()), 67108864)) {
                            throw new IllegalArgumentException(r.getKey() + " Not posted. PendingIntents attached to contextual actions with remote inputs must be mutable");
                        }
                    }
                }
            }
            if (n.isStyle(Notification.CallStyle.class)) {
                boolean isForegroundService = (n.flags & 64) != 0;
                boolean hasFullScreenIntent = n.fullScreenIntent != null;
                if (!isForegroundService && !hasFullScreenIntent) {
                    throw new IllegalArgumentException(r.getKey() + " Not posted. CallStyle notifications must either be for a foreground Service or use a fullScreenIntent.");
                }
            }
            if (this.mSnoozeHelper.isSnoozed(userId, pkg, r.getKey())) {
                MetricsLogger.action(r.getLogMaker().setType(6).setCategory(831));
                this.mNotificationRecordLogger.log(NotificationRecordLogger.NotificationEvent.NOTIFICATION_NOT_POSTED_SNOOZED, r);
                if (DBG) {
                    Slog.d(TAG, "Ignored enqueue for snoozed notification " + r.getKey());
                }
                this.mSnoozeHelper.update(userId, r);
                handleSavePolicyFile();
                return false;
            }
            boolean isBlocked2 = !areNotificationsEnabledForPackageInt(pkg, uid);
            synchronized (this.mNotificationLock) {
                isBlocked = isBlocked2 | isRecordBlockedLocked(r);
            }
            if (!isBlocked || n.isMediaNotification() || isCallNotification(pkg, uid, n)) {
                return true;
            }
            if (DBG) {
                Slog.e(TAG, "Suppressing notification from package " + r.getSbn().getPackageName() + " by user request.");
            }
            this.mUsageStats.registerBlocked(r);
            return false;
        }
        throw new IllegalArgumentException(r.getKey() + " Not posted. PendingIntents attached to bubbles must be mutable");
    }

    private boolean isCallNotification(String pkg, int uid, Notification n) {
        if (n.isStyle(Notification.CallStyle.class)) {
            return isCallNotification(pkg, uid);
        }
        return false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [7073=4] */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:10:0x0023, code lost:
        if (r5.mTelecomManager.isInSelfManagedCall(r6, android.os.UserHandle.getUserHandleForUid(r7)) != false) goto L16;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean isCallNotification(String pkg, int uid) {
        TelecomManager telecomManager;
        long identity = Binder.clearCallingIdentity();
        try {
            boolean z = false;
            if (!this.mPackageManagerClient.hasSystemFeature("android.software.telecom") || (telecomManager = this.mTelecomManager) == null) {
                return false;
            }
            try {
                if (!telecomManager.isInManagedCall()) {
                }
                z = true;
                return z;
            } catch (IllegalStateException e) {
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean areNotificationsEnabledForPackageInt(String pkg, int uid) {
        return this.mPermissionHelper.hasPermission(uid);
    }

    protected int getNotificationCount(String pkg, int userId, int excludedId, String excludedTag) {
        int count = 0;
        synchronized (this.mNotificationLock) {
            int N = this.mNotificationList.size();
            for (int i = 0; i < N; i++) {
                NotificationRecord existing = this.mNotificationList.get(i);
                if (existing.getSbn().getPackageName().equals(pkg) && existing.getSbn().getUserId() == userId && (existing.getSbn().getId() != excludedId || !TextUtils.equals(existing.getSbn().getTag(), excludedTag))) {
                    count++;
                }
            }
            int M = this.mEnqueuedNotifications.size();
            for (int i2 = 0; i2 < M; i2++) {
                NotificationRecord existing2 = this.mEnqueuedNotifications.get(i2);
                if (existing2.getSbn().getPackageName().equals(pkg) && existing2.getSbn().getUserId() == userId) {
                    count++;
                }
            }
        }
        return count;
    }

    boolean isRecordBlockedLocked(NotificationRecord r) {
        String pkg = r.getSbn().getPackageName();
        int callingUid = r.getSbn().getUid();
        return this.mPreferencesHelper.isGroupBlocked(pkg, callingUid, r.getChannel().getGroup()) || r.getImportance() == 0;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public class SnoozeNotificationRunnable implements Runnable {
        private final long mDuration;
        private final String mKey;
        private final String mSnoozeCriterionId;

        SnoozeNotificationRunnable(String key, long duration, String snoozeCriterionId) {
            this.mKey = key;
            this.mDuration = duration;
            this.mSnoozeCriterionId = snoozeCriterionId;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = NotificationManagerService.this.findInCurrentAndSnoozedNotificationByKeyLocked(this.mKey);
                if (r != null) {
                    snoozeLocked(r);
                }
            }
        }

        void snoozeLocked(NotificationRecord r) {
            List<NotificationRecord> recordsToSnooze = new ArrayList<>();
            if (r.getSbn().isGroup()) {
                List<NotificationRecord> groupNotifications = NotificationManagerService.this.findCurrentAndSnoozedGroupNotificationsLocked(r.getSbn().getPackageName(), r.getSbn().getGroupKey(), r.getSbn().getUserId());
                if (r.getNotification().isGroupSummary()) {
                    for (int i = 0; i < groupNotifications.size(); i++) {
                        if (!this.mKey.equals(groupNotifications.get(i).getKey())) {
                            recordsToSnooze.add(groupNotifications.get(i));
                        }
                    }
                } else if (NotificationManagerService.this.mSummaryByGroupKey.containsKey(r.getSbn().getGroupKey()) && groupNotifications.size() == 2) {
                    for (int i2 = 0; i2 < groupNotifications.size(); i2++) {
                        if (!this.mKey.equals(groupNotifications.get(i2).getKey())) {
                            recordsToSnooze.add(groupNotifications.get(i2));
                        }
                    }
                }
            }
            recordsToSnooze.add(r);
            if (NotificationManagerService.this.mSnoozeHelper.canSnooze(recordsToSnooze.size())) {
                for (int i3 = 0; i3 < recordsToSnooze.size(); i3++) {
                    snoozeNotificationLocked(recordsToSnooze.get(i3));
                }
                return;
            }
            Log.w(NotificationManagerService.TAG, "Cannot snooze " + r.getKey() + ": too many snoozed notifications");
        }

        void snoozeNotificationLocked(NotificationRecord r) {
            MetricsLogger.action(r.getLogMaker().setCategory(831).setType(2).addTaggedData(1139, Long.valueOf(this.mDuration)).addTaggedData(832, Integer.valueOf(this.mSnoozeCriterionId == null ? 0 : 1)));
            NotificationManagerService.this.mNotificationRecordLogger.log(NotificationRecordLogger.NotificationEvent.NOTIFICATION_SNOOZED, r);
            NotificationManagerService.this.reportUserInteraction(r);
            boolean wasPosted = NotificationManagerService.this.removeFromNotificationListsLocked(r);
            NotificationManagerService.this.cancelNotificationLocked(r, false, 18, wasPosted, null, SystemClock.elapsedRealtime());
            NotificationManagerService.this.updateLightsLocked();
            if (this.mSnoozeCriterionId != null) {
                NotificationManagerService.this.mAssistants.notifyAssistantSnoozedLocked(r, this.mSnoozeCriterionId);
                NotificationManagerService.this.mSnoozeHelper.snooze(r, this.mSnoozeCriterionId);
            } else {
                NotificationManagerService.this.mSnoozeHelper.snooze(r, this.mDuration);
            }
            r.recordSnoozed();
            NotificationManagerService.this.handleSavePolicyFile();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public class CancelNotificationRunnable implements Runnable {
        private final int mCallingPid;
        private final int mCallingUid;
        private final long mCancellationElapsedTimeMs;
        private final int mCount;
        private final int mId;
        private final ManagedServices.ManagedServiceInfo mListener;
        private final int mMustHaveFlags;
        private final int mMustNotHaveFlags;
        private final String mPkg;
        private final int mRank;
        private final int mReason;
        private final boolean mSendDelete;
        private final String mTag;
        private final int mUserId;

        CancelNotificationRunnable(int callingUid, int callingPid, String pkg, String tag, int id, int mustHaveFlags, int mustNotHaveFlags, boolean sendDelete, int userId, int reason, int rank, int count, ManagedServices.ManagedServiceInfo listener, long cancellationElapsedTimeMs) {
            this.mCallingUid = callingUid;
            this.mCallingPid = callingPid;
            this.mPkg = pkg;
            this.mTag = tag;
            this.mId = id;
            this.mMustHaveFlags = mustHaveFlags;
            this.mMustNotHaveFlags = mustNotHaveFlags;
            this.mSendDelete = sendDelete;
            this.mUserId = userId;
            this.mReason = reason;
            this.mRank = rank;
            this.mCount = count;
            this.mListener = listener;
            this.mCancellationElapsedTimeMs = cancellationElapsedTimeMs;
        }

        @Override // java.lang.Runnable
        public void run() {
            ManagedServices.ManagedServiceInfo managedServiceInfo = this.mListener;
            String listenerName = managedServiceInfo == null ? null : managedServiceInfo.component.toShortString();
            if (NotificationManagerService.DBG) {
                EventLogTags.writeNotificationCancel(this.mCallingUid, this.mCallingPid, this.mPkg, this.mId, this.mTag, this.mUserId, this.mMustHaveFlags, this.mMustNotHaveFlags, this.mReason, listenerName);
            }
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = NotificationManagerService.this.findNotificationLocked(this.mPkg, this.mTag, this.mId, this.mUserId);
                if (r != null) {
                    if (this.mReason == 1) {
                        NotificationManagerService.this.mUsageStats.registerClickedByUser(r);
                    }
                    if ((this.mReason == 10 && r.getNotification().isBubbleNotification()) || (this.mReason == 1 && r.canBubble() && r.isFlagBubbleRemoved())) {
                        int flags = 0;
                        if (r.getNotification().getBubbleMetadata() != null) {
                            flags = r.getNotification().getBubbleMetadata().getFlags();
                        }
                        NotificationManagerService.this.mNotificationDelegate.onBubbleMetadataFlagChanged(r.getKey(), flags | 2);
                        return;
                    }
                    int i = r.getNotification().flags;
                    int i2 = this.mMustHaveFlags;
                    if ((i & i2) != i2) {
                        return;
                    }
                    if ((r.getNotification().flags & this.mMustNotHaveFlags) != 0) {
                        return;
                    }
                    FlagChecker childrenFlagChecker = new FlagChecker() { // from class: com.android.server.notification.NotificationManagerService$CancelNotificationRunnable$$ExternalSyntheticLambda0
                        @Override // com.android.server.notification.NotificationManagerService.FlagChecker
                        public final boolean apply(int i3) {
                            return NotificationManagerService.CancelNotificationRunnable.this.m5147xdadb1a47(i3);
                        }
                    };
                    boolean wasPosted = NotificationManagerService.this.removeFromNotificationListsLocked(r);
                    NotificationManagerService.this.cancelNotificationLocked(r, this.mSendDelete, this.mReason, this.mRank, this.mCount, wasPosted, listenerName, this.mCancellationElapsedTimeMs);
                    NotificationManagerService.this.cancelGroupChildrenLocked(r, this.mCallingUid, this.mCallingPid, listenerName, this.mSendDelete, childrenFlagChecker, this.mReason, this.mCancellationElapsedTimeMs);
                    NotificationManagerService.this.updateLightsLocked();
                    if (NotificationManagerService.this.mShortcutHelper != null) {
                        NotificationManagerService.this.mShortcutHelper.maybeListenForShortcutChangesForBubbles(r, true, NotificationManagerService.this.mHandler);
                    }
                } else if (this.mReason != 18) {
                    boolean wasSnoozed = NotificationManagerService.this.mSnoozeHelper.cancel(this.mUserId, this.mPkg, this.mTag, this.mId);
                    if (wasSnoozed) {
                        NotificationManagerService.this.handleSavePolicyFile();
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$run$0$com-android-server-notification-NotificationManagerService$CancelNotificationRunnable  reason: not valid java name */
        public /* synthetic */ boolean m5147xdadb1a47(int flags) {
            int i = this.mReason;
            if (i == 2 || i == 1 || i == 3) {
                if ((flags & 4096) != 0) {
                    return false;
                }
            } else if (i == 8 && (flags & 64) != 0) {
                return false;
            }
            return (this.mMustNotHaveFlags & flags) == 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public static class ShowNotificationPermissionPromptRunnable implements Runnable {
        private final String mPkgName;
        private final PermissionPolicyInternal mPpi;
        private final int mTaskId;
        private final int mUserId;

        ShowNotificationPermissionPromptRunnable(String pkg, int user, int task, PermissionPolicyInternal pPi) {
            this.mPkgName = pkg;
            this.mUserId = user;
            this.mTaskId = task;
            this.mPpi = pPi;
        }

        public boolean equals(Object o) {
            if (o instanceof ShowNotificationPermissionPromptRunnable) {
                ShowNotificationPermissionPromptRunnable other = (ShowNotificationPermissionPromptRunnable) o;
                return Objects.equals(this.mPkgName, other.mPkgName) && this.mUserId == other.mUserId && this.mTaskId == other.mTaskId;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.mPkgName, Integer.valueOf(this.mUserId), Integer.valueOf(this.mTaskId));
        }

        @Override // java.lang.Runnable
        public void run() {
            this.mPpi.showNotificationPromptIfNeeded(this.mPkgName, this.mUserId, this.mTaskId);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public class EnqueueNotificationRunnable implements Runnable {
        private final long enqueueElapsedTimeMs;
        private final boolean isAppForeground;
        private final NotificationRecord r;
        private final int userId;

        EnqueueNotificationRunnable(int userId, NotificationRecord r, boolean foreground, long enqueueElapsedTimeMs) {
            this.userId = userId;
            this.r = r;
            this.isAppForeground = foreground;
            this.enqueueElapsedTimeMs = enqueueElapsedTimeMs;
        }

        /* JADX WARN: Removed duplicated region for block: B:42:0x018c A[Catch: all -> 0x01f8, TryCatch #0 {, blocks: (B:4:0x0007, B:6:0x001d, B:8:0x001f, B:10:0x0051, B:11:0x006e, B:13:0x0070, B:15:0x0098, B:16:0x00b2, B:18:0x00b4, B:20:0x00ce, B:21:0x00ea, B:23:0x00fa, B:24:0x00ff, B:26:0x0131, B:28:0x0137, B:29:0x0148, B:31:0x0150, B:40:0x0180, B:42:0x018c, B:44:0x01f6, B:43:0x01c8, B:39:0x0168), top: B:49:0x0007 }] */
        /* JADX WARN: Removed duplicated region for block: B:43:0x01c8 A[Catch: all -> 0x01f8, TryCatch #0 {, blocks: (B:4:0x0007, B:6:0x001d, B:8:0x001f, B:10:0x0051, B:11:0x006e, B:13:0x0070, B:15:0x0098, B:16:0x00b2, B:18:0x00b4, B:20:0x00ce, B:21:0x00ea, B:23:0x00fa, B:24:0x00ff, B:26:0x0131, B:28:0x0137, B:29:0x0148, B:31:0x0150, B:40:0x0180, B:42:0x018c, B:44:0x01f6, B:43:0x01c8, B:39:0x0168), top: B:49:0x0007 }] */
        @Override // java.lang.Runnable
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void run() {
            int enqueueStatus;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                if (ITranNotificationManagerService.Instance().filter(NotificationManagerService.this.getContext(), this.r.getSbn(), this)) {
                    return;
                }
                Long snoozeAt = NotificationManagerService.this.mSnoozeHelper.getSnoozeTimeForUnpostedNotification(this.r.getUser().getIdentifier(), this.r.getSbn().getPackageName(), this.r.getSbn().getKey());
                long currentTime = System.currentTimeMillis();
                if (snoozeAt.longValue() > currentTime) {
                    new SnoozeNotificationRunnable(this.r.getSbn().getKey(), snoozeAt.longValue() - currentTime, null).snoozeLocked(this.r);
                    return;
                }
                String contextId = NotificationManagerService.this.mSnoozeHelper.getSnoozeContextForUnpostedNotification(this.r.getUser().getIdentifier(), this.r.getSbn().getPackageName(), this.r.getSbn().getKey());
                if (contextId == null) {
                    NotificationManagerService.this.mEnqueuedNotifications.add(this.r);
                    NotificationManagerService.this.scheduleTimeoutLocked(this.r);
                    StatusBarNotification n = this.r.getSbn();
                    if (NotificationManagerService.DBG) {
                        Slog.d(NotificationManagerService.TAG, "EnqueueNotificationRunnable.run for: " + n.getKey());
                    }
                    NotificationRecord old = NotificationManagerService.this.mNotificationsByKey.get(n.getKey());
                    if (old != null) {
                        this.r.copyRankingInformation(old);
                    }
                    int callingUid = n.getUid();
                    int callingPid = n.getInitialPid();
                    Notification notification = n.getNotification();
                    String pkg = n.getPackageName();
                    int id = n.getId();
                    String tag = n.getTag();
                    NotificationManagerService.this.updateNotificationBubbleFlags(this.r, this.isAppForeground);
                    NotificationManagerService.this.handleGroupedNotificationLocked(this.r, old, callingUid, callingPid);
                    if (n.isGroup() && notification.isGroupChild()) {
                        NotificationManagerService.this.mSnoozeHelper.repostGroupSummary(pkg, this.r.getUserId(), n.getGroupKey());
                    }
                    if (pkg.equals("com.android.providers.downloads") && !Log.isLoggable("DownloadManager", 2)) {
                        if (!NotificationManagerService.this.mAssistants.isEnabled()) {
                            NotificationManagerService.this.mAssistants.onNotificationEnqueuedLocked(this.r);
                            NotificationManagerService.this.mHandler.postDelayed(new PostNotificationRunnable(this.r.getKey(), this.r.getSbn().getPackageName(), this.r.getUid(), this.enqueueElapsedTimeMs), NotificationManagerService.DELAY_FOR_ASSISTANT_TIME);
                        } else {
                            NotificationManagerService.this.mHandler.post(new PostNotificationRunnable(this.r.getKey(), this.r.getSbn().getPackageName(), this.r.getUid(), this.enqueueElapsedTimeMs));
                        }
                        return;
                    }
                    if (old == null) {
                        enqueueStatus = 0;
                    } else {
                        enqueueStatus = 1;
                    }
                    EventLogTags.writeNotificationEnqueue(callingUid, callingPid, pkg, id, tag, this.userId, notification.toString(), enqueueStatus);
                    if (!NotificationManagerService.this.mAssistants.isEnabled()) {
                    }
                    return;
                }
                new SnoozeNotificationRunnable(this.r.getSbn().getKey(), 0L, contextId).snoozeLocked(this.r);
            }
        }
    }

    boolean isPackagePausedOrSuspended(String pkg, int uid) {
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int flags = pmi.getDistractingPackageRestrictions(pkg, Binder.getCallingUserHandle().getIdentifier());
        boolean isPaused = (flags & 2) != 0;
        return isPaused | isPackageSuspendedForUser(pkg, uid);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public class PostNotificationRunnable implements Runnable {
        private final String key;
        private final String pkg;
        private final long postElapsedTimeMs;
        private final int uid;

        PostNotificationRunnable(String key, String pkg, int uid, long postElapsedTimeMs) {
            this.key = key;
            this.pkg = pkg;
            this.uid = uid;
            this.postElapsedTimeMs = postElapsedTimeMs;
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [7649=6, 7650=8, 7651=4, 7652=4, 7653=4] */
        /* JADX WARN: Removed duplicated region for block: B:102:0x02c7 A[Catch: all -> 0x0393, TryCatch #1 {all -> 0x0393, blocks: (B:77:0x021e, B:79:0x022f, B:81:0x023b, B:82:0x0241, B:84:0x0266, B:86:0x026f, B:87:0x0276, B:90:0x027e, B:92:0x0284, B:94:0x0290, B:100:0x02b7, B:108:0x031d, B:110:0x0325, B:111:0x0335, B:96:0x029e, B:98:0x02a6, B:102:0x02c7, B:104:0x02e1, B:106:0x02e5, B:107:0x0301, B:76:0x01ff), top: B:140:0x01ff }] */
        /* JADX WARN: Removed duplicated region for block: B:110:0x0325 A[Catch: all -> 0x0393, TryCatch #1 {all -> 0x0393, blocks: (B:77:0x021e, B:79:0x022f, B:81:0x023b, B:82:0x0241, B:84:0x0266, B:86:0x026f, B:87:0x0276, B:90:0x027e, B:92:0x0284, B:94:0x0290, B:100:0x02b7, B:108:0x031d, B:110:0x0325, B:111:0x0335, B:96:0x029e, B:98:0x02a6, B:102:0x02c7, B:104:0x02e1, B:106:0x02e5, B:107:0x0301, B:76:0x01ff), top: B:140:0x01ff }] */
        /* JADX WARN: Removed duplicated region for block: B:114:0x036f A[Catch: all -> 0x03cb, TryCatch #6 {all -> 0x03cb, blocks: (B:136:0x03cc, B:112:0x0364, B:114:0x036f, B:116:0x0385, B:117:0x038d, B:119:0x0391, B:127:0x039e, B:129:0x03a9, B:131:0x03bf, B:132:0x03c2, B:134:0x03ca), top: B:148:0x001f }] */
        /* JADX WARN: Removed duplicated region for block: B:129:0x03a9 A[Catch: all -> 0x03cb, TryCatch #6 {all -> 0x03cb, blocks: (B:136:0x03cc, B:112:0x0364, B:114:0x036f, B:116:0x0385, B:117:0x038d, B:119:0x0391, B:127:0x039e, B:129:0x03a9, B:131:0x03bf, B:132:0x03c2, B:134:0x03ca), top: B:148:0x001f }] */
        /* JADX WARN: Removed duplicated region for block: B:138:0x017b A[EXC_TOP_SPLITTER, SYNTHETIC] */
        /* JADX WARN: Removed duplicated region for block: B:152:0x03ca A[EDGE_INSN: B:152:0x03ca->B:134:0x03ca ?: BREAK  , SYNTHETIC] */
        /* JADX WARN: Removed duplicated region for block: B:159:0x0391 A[EDGE_INSN: B:159:0x0391->B:119:0x0391 ?: BREAK  , SYNTHETIC] */
        /* JADX WARN: Removed duplicated region for block: B:73:0x01c9  */
        /* JADX WARN: Removed duplicated region for block: B:79:0x022f A[Catch: all -> 0x0393, TryCatch #1 {all -> 0x0393, blocks: (B:77:0x021e, B:79:0x022f, B:81:0x023b, B:82:0x0241, B:84:0x0266, B:86:0x026f, B:87:0x0276, B:90:0x027e, B:92:0x0284, B:94:0x0290, B:100:0x02b7, B:108:0x031d, B:110:0x0325, B:111:0x0335, B:96:0x029e, B:98:0x02a6, B:102:0x02c7, B:104:0x02e1, B:106:0x02e5, B:107:0x0301, B:76:0x01ff), top: B:140:0x01ff }] */
        /* JADX WARN: Removed duplicated region for block: B:84:0x0266 A[Catch: all -> 0x0393, TryCatch #1 {all -> 0x0393, blocks: (B:77:0x021e, B:79:0x022f, B:81:0x023b, B:82:0x0241, B:84:0x0266, B:86:0x026f, B:87:0x0276, B:90:0x027e, B:92:0x0284, B:94:0x0290, B:100:0x02b7, B:108:0x031d, B:110:0x0325, B:111:0x0335, B:96:0x029e, B:98:0x02a6, B:102:0x02c7, B:104:0x02e1, B:106:0x02e5, B:107:0x0301, B:76:0x01ff), top: B:140:0x01ff }] */
        /* JADX WARN: Removed duplicated region for block: B:89:0x027c  */
        @Override // java.lang.Runnable
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void run() {
            int N;
            int i;
            int index;
            NotificationRecord old;
            int N2;
            int i2;
            boolean appBanned = !NotificationManagerService.this.areNotificationsEnabledForPackageInt(this.pkg, this.uid);
            boolean isCallNotification = NotificationManagerService.this.isCallNotification(this.pkg, this.uid);
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = null;
                try {
                    try {
                        int N3 = NotificationManagerService.this.mEnqueuedNotifications.size();
                        for (int i3 = 0; i3 < N3; i3++) {
                            try {
                                NotificationRecord enqueued = NotificationManagerService.this.mEnqueuedNotifications.get(i3);
                                if (Objects.equals(this.key, enqueued.getKey())) {
                                    r = enqueued;
                                    break;
                                }
                            } catch (Throwable th) {
                                th = th;
                                N = NotificationManagerService.this.mEnqueuedNotifications.size();
                                i = 0;
                                while (true) {
                                    if (i < N) {
                                    }
                                    i++;
                                }
                                throw th;
                            }
                        }
                        try {
                            if (r == null) {
                                Slog.i(NotificationManagerService.TAG, "Cannot find enqueued record for key: " + this.key);
                                int N4 = NotificationManagerService.this.mEnqueuedNotifications.size();
                                int i4 = 0;
                                while (true) {
                                    if (i4 >= N4) {
                                        break;
                                    }
                                    NotificationRecord enqueued2 = NotificationManagerService.this.mEnqueuedNotifications.get(i4);
                                    if (Objects.equals(this.key, enqueued2.getKey())) {
                                        NotificationManagerService.this.mEnqueuedNotifications.remove(i4);
                                        break;
                                    }
                                    i4++;
                                }
                                return;
                            }
                            final StatusBarNotification n = r.getSbn();
                            Notification notification = n.getNotification();
                            boolean isCallNotificationAndCorrectStyle = isCallNotification && notification.isStyle(Notification.CallStyle.class);
                            if (!notification.isMediaNotification() && !isCallNotificationAndCorrectStyle && (appBanned || NotificationManagerService.this.isRecordBlockedLocked(r))) {
                                NotificationManagerService.this.mUsageStats.registerBlocked(r);
                                if (NotificationManagerService.DBG) {
                                    Slog.e(NotificationManagerService.TAG, "Suppressing notification from package " + this.pkg);
                                }
                                int N5 = NotificationManagerService.this.mEnqueuedNotifications.size();
                                int i5 = 0;
                                while (true) {
                                    if (i5 >= N5) {
                                        break;
                                    }
                                    NotificationRecord enqueued3 = NotificationManagerService.this.mEnqueuedNotifications.get(i5);
                                    if (Objects.equals(this.key, enqueued3.getKey())) {
                                        NotificationManagerService.this.mEnqueuedNotifications.remove(i5);
                                        break;
                                    }
                                    i5++;
                                }
                                return;
                            }
                            boolean isPackageSuspended = NotificationManagerService.this.isPackagePausedOrSuspended(r.getSbn().getPackageName(), r.getUid());
                            r.setHidden(isPackageSuspended);
                            if (isPackageSuspended) {
                                NotificationManagerService.this.mUsageStats.registerSuspendedByAdmin(r);
                            }
                            NotificationRecord old2 = NotificationManagerService.this.mNotificationsByKey.get(this.key);
                            if (old2 != null && old2.getSbn().getInstanceId() != null) {
                                n.setInstanceId(old2.getSbn().getInstanceId());
                                index = NotificationManagerService.this.indexOfNotificationLocked(n.getKey());
                                if (index >= 0) {
                                    try {
                                        NotificationManagerService.this.mNotificationList.add(r);
                                        NotificationManagerService.this.mUsageStats.registerPostedByApp(r);
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                    try {
                                        NotificationManagerService.this.mUsageStatsManagerInternal.reportNotificationPosted(r.getSbn().getOpPkg(), r.getSbn().getUser(), this.postElapsedTimeMs);
                                        boolean isInterruptive = NotificationManagerService.this.isVisuallyInterruptive(null, r);
                                        r.setInterruptive(isInterruptive);
                                        r.setTextChanged(isInterruptive);
                                        old = old2;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        N = NotificationManagerService.this.mEnqueuedNotifications.size();
                                        i = 0;
                                        while (true) {
                                            if (i < N) {
                                            }
                                            i++;
                                        }
                                        throw th;
                                    }
                                } else {
                                    try {
                                        NotificationRecord old3 = NotificationManagerService.this.mNotificationList.get(index);
                                        NotificationManagerService.this.mNotificationList.set(index, r);
                                        NotificationManagerService.this.mUsageStats.registerUpdatedByApp(r, old3);
                                        try {
                                            NotificationManagerService.this.mUsageStatsManagerInternal.reportNotificationUpdated(r.getSbn().getOpPkg(), r.getSbn().getUser(), this.postElapsedTimeMs);
                                            notification.flags |= old3.getNotification().flags & 64;
                                            r.isUpdate = true;
                                            r.setTextChanged(NotificationManagerService.this.isVisuallyInterruptive(old3, r));
                                            old = old3;
                                        } catch (Throwable th4) {
                                            th = th4;
                                            N = NotificationManagerService.this.mEnqueuedNotifications.size();
                                            i = 0;
                                            while (true) {
                                                if (i < N) {
                                                    break;
                                                }
                                                NotificationRecord enqueued4 = NotificationManagerService.this.mEnqueuedNotifications.get(i);
                                                if (Objects.equals(this.key, enqueued4.getKey())) {
                                                    NotificationManagerService.this.mEnqueuedNotifications.remove(i);
                                                    break;
                                                }
                                                i++;
                                            }
                                            throw th;
                                        }
                                    } catch (Throwable th5) {
                                        th = th5;
                                        N = NotificationManagerService.this.mEnqueuedNotifications.size();
                                        i = 0;
                                        while (true) {
                                            if (i < N) {
                                            }
                                            i++;
                                        }
                                        throw th;
                                    }
                                }
                                NotificationManagerService.this.mNotificationsByKey.put(n.getKey(), r);
                                if ((notification.flags & 64) != 0) {
                                    notification.flags |= 32;
                                    if (!NotificationManagerService.this.mAllowFgsDismissal) {
                                        notification.flags |= 2;
                                    }
                                }
                                NotificationManagerService.this.mRankingHelper.extractSignals(r);
                                NotificationManagerService.this.mRankingHelper.sort(NotificationManagerService.this.mNotificationList);
                                int position = NotificationManagerService.this.mRankingHelper.indexOf(NotificationManagerService.this.mNotificationList, r);
                                int buzzBeepBlinkLoggingCode = 0;
                                if (!r.isHidden()) {
                                    buzzBeepBlinkLoggingCode = NotificationManagerService.this.buzzBeepBlinkLocked(r);
                                    if (n != null) {
                                        ITranNotificationManagerService.Instance().notificationPostedScreenOn(n);
                                    }
                                }
                                if (notification.getSmallIcon() == null) {
                                    StatusBarNotification oldSbn = old != null ? old.getSbn() : null;
                                    NotificationManagerService.this.mListeners.notifyPostedLocked(r, old);
                                    if ((oldSbn == null || !Objects.equals(oldSbn.getGroup(), n.getGroup())) && !NotificationManagerService.this.isCritical(r)) {
                                        NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$PostNotificationRunnable$$ExternalSyntheticLambda0
                                            @Override // java.lang.Runnable
                                            public final void run() {
                                                NotificationManagerService.PostNotificationRunnable.this.m5172x43a12ad(n);
                                            }
                                        });
                                    } else if (oldSbn != null) {
                                        final NotificationRecord finalRecord = r;
                                        NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$PostNotificationRunnable$$ExternalSyntheticLambda1
                                            @Override // java.lang.Runnable
                                            public final void run() {
                                                NotificationManagerService.PostNotificationRunnable.this.m5173xbc26802e(finalRecord);
                                            }
                                        });
                                    }
                                } else {
                                    Slog.e(NotificationManagerService.TAG, "Not posting notification without small icon: " + notification);
                                    if (old != null && !old.isCanceled) {
                                        NotificationManagerService.this.mListeners.notifyRemovedLocked(r, 4, r.getStats());
                                        NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService.PostNotificationRunnable.1
                                            @Override // java.lang.Runnable
                                            public void run() {
                                                NotificationManagerService.this.mGroupHelper.onNotificationRemoved(n);
                                            }
                                        });
                                    }
                                    Slog.e(NotificationManagerService.TAG, "WARNING: In a future release this will crash the app: " + n.getPackageName());
                                }
                                if (NotificationManagerService.this.mShortcutHelper != null) {
                                    NotificationManagerService.this.mShortcutHelper.maybeListenForShortcutChangesForBubbles(r, false, NotificationManagerService.this.mHandler);
                                }
                                NotificationManagerService.this.maybeRecordInterruptionLocked(r);
                                NotificationManagerService.this.maybeRegisterMessageSent(r);
                                NotificationManagerService.this.maybeReportForegroundServiceUpdate(r, true);
                                NotificationManagerService.this.mNotificationRecordLogger.maybeLogNotificationPosted(r, old, position, buzzBeepBlinkLoggingCode, NotificationManagerService.this.getGroupInstanceId(r.getSbn().getGroupKey()));
                                N2 = NotificationManagerService.this.mEnqueuedNotifications.size();
                                i2 = 0;
                                while (true) {
                                    if (i2 < N2) {
                                        break;
                                    }
                                    NotificationRecord enqueued5 = NotificationManagerService.this.mEnqueuedNotifications.get(i2);
                                    if (Objects.equals(this.key, enqueued5.getKey())) {
                                        NotificationManagerService.this.mEnqueuedNotifications.remove(i2);
                                        break;
                                    }
                                    i2++;
                                }
                            }
                            n.setInstanceId(NotificationManagerService.this.mNotificationInstanceIdSequence.newInstanceId());
                            index = NotificationManagerService.this.indexOfNotificationLocked(n.getKey());
                            if (index >= 0) {
                            }
                            NotificationManagerService.this.mNotificationsByKey.put(n.getKey(), r);
                            if ((notification.flags & 64) != 0) {
                            }
                            NotificationManagerService.this.mRankingHelper.extractSignals(r);
                            NotificationManagerService.this.mRankingHelper.sort(NotificationManagerService.this.mNotificationList);
                            int position2 = NotificationManagerService.this.mRankingHelper.indexOf(NotificationManagerService.this.mNotificationList, r);
                            int buzzBeepBlinkLoggingCode2 = 0;
                            if (!r.isHidden()) {
                            }
                            if (notification.getSmallIcon() == null) {
                            }
                            if (NotificationManagerService.this.mShortcutHelper != null) {
                            }
                            NotificationManagerService.this.maybeRecordInterruptionLocked(r);
                            NotificationManagerService.this.maybeRegisterMessageSent(r);
                            NotificationManagerService.this.maybeReportForegroundServiceUpdate(r, true);
                            NotificationManagerService.this.mNotificationRecordLogger.maybeLogNotificationPosted(r, old, position2, buzzBeepBlinkLoggingCode2, NotificationManagerService.this.getGroupInstanceId(r.getSbn().getGroupKey()));
                            N2 = NotificationManagerService.this.mEnqueuedNotifications.size();
                            i2 = 0;
                            while (true) {
                                if (i2 < N2) {
                                }
                                i2++;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            throw th;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                    }
                } catch (Throwable th8) {
                    th = th8;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$run$0$com-android-server-notification-NotificationManagerService$PostNotificationRunnable  reason: not valid java name */
        public /* synthetic */ void m5172x43a12ad(StatusBarNotification n) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationManagerService.this.mGroupHelper.onNotificationPosted(n, NotificationManagerService.this.hasAutoGroupSummaryLocked(n));
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$run$1$com-android-server-notification-NotificationManagerService$PostNotificationRunnable  reason: not valid java name */
        public /* synthetic */ void m5173xbc26802e(NotificationRecord finalRecord) {
            NotificationManagerService.this.mGroupHelper.onNotificationUpdated(finalRecord.getSbn());
        }
    }

    InstanceId getGroupInstanceId(String groupKey) {
        NotificationRecord group;
        if (groupKey == null || (group = this.mSummaryByGroupKey.get(groupKey)) == null) {
            return null;
        }
        return group.getSbn().getInstanceId();
    }

    protected boolean isVisuallyInterruptive(NotificationRecord old, NotificationRecord r) {
        Notification.Builder oldB;
        Notification.Builder newB;
        if (r.getSbn().isGroup() && r.getSbn().getNotification().isGroupSummary()) {
            if (DEBUG_INTERRUPTIVENESS) {
                Slog.v(TAG, "INTERRUPTIVENESS: " + r.getKey() + " is not interruptive: summary");
            }
            return false;
        } else if (old == null) {
            if (DEBUG_INTERRUPTIVENESS) {
                Slog.v(TAG, "INTERRUPTIVENESS: " + r.getKey() + " is interruptive: new notification");
            }
            return true;
        } else if (r == null) {
            if (DEBUG_INTERRUPTIVENESS) {
                Slog.v(TAG, "INTERRUPTIVENESS: " + r.getKey() + " is not interruptive: null");
            }
            return false;
        } else {
            Notification oldN = old.getSbn().getNotification();
            Notification newN = r.getSbn().getNotification();
            if (oldN.extras == null || newN.extras == null) {
                if (DEBUG_INTERRUPTIVENESS) {
                    Slog.v(TAG, "INTERRUPTIVENESS: " + r.getKey() + " is not interruptive: no extras");
                }
                return false;
            } else if ((r.getSbn().getNotification().flags & 64) != 0) {
                if (DEBUG_INTERRUPTIVENESS) {
                    Slog.v(TAG, "INTERRUPTIVENESS: " + r.getKey() + " is not interruptive: foreground service");
                }
                return false;
            } else {
                String oldTitle = String.valueOf(oldN.extras.get("android.title"));
                String newTitle = String.valueOf(newN.extras.get("android.title"));
                if (!Objects.equals(oldTitle, newTitle)) {
                    if (DEBUG_INTERRUPTIVENESS) {
                        Slog.v(TAG, "INTERRUPTIVENESS: " + r.getKey() + " is interruptive: changed title");
                        Slog.v(TAG, "INTERRUPTIVENESS: " + String.format("   old title: %s (%s@0x%08x)", oldTitle, oldTitle.getClass(), Integer.valueOf(oldTitle.hashCode())));
                        Slog.v(TAG, "INTERRUPTIVENESS: " + String.format("   new title: %s (%s@0x%08x)", newTitle, newTitle.getClass(), Integer.valueOf(newTitle.hashCode())));
                    }
                    return true;
                }
                String oldText = String.valueOf(oldN.extras.get("android.text"));
                String newText = String.valueOf(newN.extras.get("android.text"));
                if (!Objects.equals(oldText, newText)) {
                    if (DEBUG_INTERRUPTIVENESS) {
                        Slog.v(TAG, "INTERRUPTIVENESS: " + r.getKey() + " is interruptive: changed text");
                        Slog.v(TAG, "INTERRUPTIVENESS: " + String.format("   old text: %s (%s@0x%08x)", oldText, oldText.getClass(), Integer.valueOf(oldText.hashCode())));
                        Slog.v(TAG, "INTERRUPTIVENESS: " + String.format("   new text: %s (%s@0x%08x)", newText, newText.getClass(), Integer.valueOf(newText.hashCode())));
                    }
                    return true;
                } else if (oldN.hasCompletedProgress() != newN.hasCompletedProgress()) {
                    if (DEBUG_INTERRUPTIVENESS) {
                        Slog.v(TAG, "INTERRUPTIVENESS: " + r.getKey() + " is interruptive: completed progress");
                    }
                    return true;
                } else if (r.canBubble()) {
                    if (DEBUG_INTERRUPTIVENESS) {
                        Slog.v(TAG, "INTERRUPTIVENESS: " + r.getKey() + " is not interruptive: bubble");
                    }
                    return false;
                } else if (Notification.areActionsVisiblyDifferent(oldN, newN)) {
                    if (DEBUG_INTERRUPTIVENESS) {
                        Slog.v(TAG, "INTERRUPTIVENESS: " + r.getKey() + " is interruptive: changed actions");
                    }
                    return true;
                } else {
                    try {
                        oldB = Notification.Builder.recoverBuilder(getContext(), oldN);
                        newB = Notification.Builder.recoverBuilder(getContext(), newN);
                    } catch (Exception e) {
                        Slog.w(TAG, "error recovering builder", e);
                    }
                    if (Notification.areStyledNotificationsVisiblyDifferent(oldB, newB)) {
                        if (DEBUG_INTERRUPTIVENESS) {
                            Slog.v(TAG, "INTERRUPTIVENESS: " + r.getKey() + " is interruptive: styles differ");
                        }
                        return true;
                    }
                    if (Notification.areRemoteViewsChanged(oldB, newB)) {
                        if (DEBUG_INTERRUPTIVENESS) {
                            Slog.v(TAG, "INTERRUPTIVENESS: " + r.getKey() + " is interruptive: remoteviews differ");
                        }
                        return true;
                    }
                    return false;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isCritical(NotificationRecord record) {
        return record.getCriticality() < 2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleGroupedNotificationLocked(NotificationRecord r, NotificationRecord old, int callingUid, int callingPid) {
        NotificationRecord removedSummary;
        StatusBarNotification sbn = r.getSbn();
        Notification n = sbn.getNotification();
        if (n.isGroupSummary() && !sbn.isAppGroup()) {
            n.flags &= -513;
        }
        String group = sbn.getGroupKey();
        boolean isSummary = n.isGroupSummary();
        Notification oldN = old != null ? old.getSbn().getNotification() : null;
        String oldGroup = old != null ? old.getSbn().getGroupKey() : null;
        boolean oldIsSummary = old != null && oldN.isGroupSummary();
        if (oldIsSummary && (removedSummary = this.mSummaryByGroupKey.remove(oldGroup)) != old) {
            String removedKey = removedSummary != null ? removedSummary.getKey() : "<null>";
            Slog.w(TAG, "Removed summary didn't match old notification: old=" + old.getKey() + ", removed=" + removedKey);
        }
        if (isSummary) {
            this.mSummaryByGroupKey.put(group, r);
        }
        FlagChecker childrenFlagChecker = new FlagChecker() { // from class: com.android.server.notification.NotificationManagerService$$ExternalSyntheticLambda9
            @Override // com.android.server.notification.NotificationManagerService.FlagChecker
            public final boolean apply(int i) {
                return NotificationManagerService.lambda$handleGroupedNotificationLocked$8(i);
            }
        };
        if (oldIsSummary) {
            if (!isSummary || !oldGroup.equals(group)) {
                cancelGroupChildrenLocked(old, callingUid, callingPid, null, false, childrenFlagChecker, 8, SystemClock.elapsedRealtime());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$handleGroupedNotificationLocked$8(int flags) {
        if ((flags & 64) != 0) {
            return false;
        }
        return true;
    }

    void scheduleTimeoutLocked(NotificationRecord record) {
        if (record.getNotification().getTimeoutAfter() > 0) {
            PendingIntent pi = PendingIntent.getBroadcast(getContext(), 1, new Intent(ACTION_NOTIFICATION_TIMEOUT).setPackage(PackageManagerService.PLATFORM_PACKAGE_NAME).setData(new Uri.Builder().scheme(SCHEME_TIMEOUT).appendPath(record.getKey()).build()).addFlags(268435456).putExtra(EXTRA_KEY, record.getKey()), AudioFormat.DTS_HD);
            this.mAlarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + record.getNotification().getTimeoutAfter(), pi);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [8034=4] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:101:0x0190 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:103:0x01b8 */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:144:0x0283  */
    /* JADX WARN: Removed duplicated region for block: B:145:0x0288  */
    /* JADX WARN: Removed duplicated region for block: B:150:0x02ac A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:153:0x02b3 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:157:0x02c6  */
    /* JADX WARN: Removed duplicated region for block: B:163:0x02db  */
    /* JADX WARN: Removed duplicated region for block: B:167:0x02e4  */
    /* JADX WARN: Removed duplicated region for block: B:168:0x02e6  */
    /* JADX WARN: Removed duplicated region for block: B:172:0x02eb  */
    /* JADX WARN: Removed duplicated region for block: B:175:0x02ef  */
    /* JADX WARN: Removed duplicated region for block: B:191:0x0395  */
    /* JADX WARN: Removed duplicated region for block: B:193:0x0398 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:71:0x00f9  */
    /* JADX WARN: Removed duplicated region for block: B:72:0x00fb  */
    /* JADX WARN: Type inference failed for: r2v15 */
    /* JADX WARN: Type inference failed for: r4v0 */
    /* JADX WARN: Type inference failed for: r4v1, types: [int] */
    /* JADX WARN: Type inference failed for: r4v12 */
    /* JADX WARN: Type inference failed for: r4v8 */
    /* JADX WARN: Type inference failed for: r4v9 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    int buzzBeepBlinkLocked(NotificationRecord record) {
        int i;
        int i2;
        boolean aboveThreshold;
        boolean wasBuzz;
        int i3;
        int i4;
        boolean wasBuzz2;
        int i5;
        int buzzBeepBlink;
        boolean z;
        LogicalLight logicalLight;
        int i6;
        VibrationEffect vibration;
        boolean hasAudibleAlert;
        boolean wasBuzz3;
        if (!this.mIsAutomotive || this.mNotificationEffectsEnabledForAutomotive) {
            ?? r4 = 0;
            beep = false;
            boolean beep = false;
            r4 = 0;
            r4 = 0;
            String key = record.getKey();
            String pkg = record.getSbn().getPackageName();
            int uid = record.getSbn().getUid();
            boolean isPlaySound = this.mPreferencesHelper.canPlaySound(pkg, uid);
            boolean isPlayVibration = this.mPreferencesHelper.canPlayVibration(pkg, uid);
            boolean aboveThreshold2 = this.mIsAutomotive ? record.getImportance() > 3 : record.getImportance() >= 3;
            boolean wasBeep = key != null && key.equals(this.mSoundNotificationKey);
            boolean wasBuzz4 = key != null && key.equals(this.mVibrateNotificationKey);
            boolean hasValidVibrate = false;
            boolean hasValidSound = false;
            boolean sentAccessibilityEvent = false;
            boolean suppressedByDnd = record.isIntercepted() && (record.getSuppressedVisualEffects() & 32) != 0;
            if (!record.isUpdate && record.getImportance() > 1 && !suppressedByDnd && isNotificationForCurrentUser(record)) {
                sendAccessibilityEvent(record);
                sentAccessibilityEvent = true;
            }
            if (!aboveThreshold2 || !isNotificationForCurrentUser(record)) {
                i = 0;
                i2 = 0;
                aboveThreshold = aboveThreshold2;
                wasBuzz = wasBuzz4;
                i3 = 4;
            } else if (this.mSystemReady && this.mAudioManager != null) {
                Uri soundUri = record.getSound();
                boolean hasValidSound2 = (soundUri == null || Uri.EMPTY.equals(soundUri)) ? false : true;
                VibrationEffect vibration2 = record.getVibration();
                if (vibration2 == null && hasValidSound2) {
                    i6 = 0;
                    i2 = 0;
                    if (this.mAudioManager.getRingerModeInternal() == 1 && this.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(record.getAudioAttributes())) == 0) {
                        boolean insistent = (record.getFlags() & 4) != 0;
                        vibration = this.mVibratorHelper.createFallbackVibration(insistent);
                        hasValidVibrate = vibration == null;
                        hasAudibleAlert = !hasValidSound2 || hasValidVibrate;
                        if (hasAudibleAlert || shouldMuteNotificationLocked(record)) {
                            aboveThreshold = aboveThreshold2;
                            boolean z2 = wasBuzz4;
                            i3 = 4;
                            if ((record.getFlags() & 4) == 0) {
                                hasValidSound = false;
                                i4 = i6;
                                wasBuzz2 = z2;
                            } else {
                                hasValidSound = hasValidSound2;
                                i4 = i6;
                                wasBuzz2 = z2;
                            }
                        } else {
                            if (!sentAccessibilityEvent) {
                                sendAccessibilityEvent(record);
                            }
                            boolean z3 = DBG;
                            if (z3) {
                                Slog.v(TAG, "Interrupting!");
                            }
                            boolean isInsistentUpdate = isInsistentUpdate(record);
                            if (!hasValidSound2) {
                                aboveThreshold = aboveThreshold2;
                                wasBuzz3 = wasBuzz4;
                            } else if (isInsistentUpdate) {
                                beep = true;
                                aboveThreshold = aboveThreshold2;
                                wasBuzz3 = wasBuzz4;
                            } else {
                                if (isInCall()) {
                                    playInCallNotification();
                                    aboveThreshold = aboveThreshold2;
                                    wasBuzz3 = wasBuzz4;
                                    if (this.mAudioManager.getMode() == 3) {
                                        this.mAudioManager.setParameters("SET_AUDIO_NOTIFICATION=1");
                                        Log.i(TAG, "zhujb buzzBeepBlinkLocked: playSound key = " + key + ", isPlaySound = " + isPlaySound + ", beep = false");
                                    }
                                    beep = true;
                                } else {
                                    aboveThreshold = aboveThreshold2;
                                    wasBuzz3 = wasBuzz4;
                                    if (isPlaySound) {
                                        boolean beep2 = playSound(record, soundUri);
                                        beep = beep2;
                                    }
                                    if (z3) {
                                        Log.i(TAG, "buzzBeepBlinkLocked: playSound key = " + key + ", isPlaySound = " + isPlaySound + ", beep = " + beep);
                                    }
                                }
                                if (beep) {
                                    this.mSoundNotificationKey = key;
                                }
                            }
                            boolean ringerModeSilent = this.mAudioManager.getRingerModeInternal() == 0;
                            if (!isInCall() && hasValidVibrate && !ringerModeSilent) {
                                if (isInsistentUpdate) {
                                    i4 = 1;
                                } else if (isPlayVibration) {
                                    boolean buzz = playVibration(record, vibration, hasValidSound2);
                                    boolean newInsistent = (record.getNotification().flags & 4) != 0;
                                    NotificationRecord oldRecord = this.mNotificationsByKey.get(this.mVibrateNotificationKey);
                                    boolean updateVibrateKey = true;
                                    if (oldRecord != null) {
                                        boolean oldInsistent = (oldRecord.getNotification().flags & 4) != 0;
                                        if (oldInsistent && !newInsistent) {
                                            updateVibrateKey = false;
                                        }
                                        if (z3) {
                                            Log.i(TAG, "buzzBeepBlinkLocked: playVibration key = " + key + ", isPlayVibration = " + isPlayVibration + ", buzz = " + buzz);
                                        }
                                    }
                                    if (buzz && updateVibrateKey) {
                                        this.mVibrateNotificationKey = key;
                                    }
                                    i4 = buzz;
                                }
                                hasValidSound = hasValidSound2;
                                i3 = 4;
                                r4 = beep;
                                wasBuzz2 = wasBuzz3;
                            }
                            i4 = i6;
                            hasValidSound = hasValidSound2;
                            i3 = 4;
                            r4 = beep;
                            wasBuzz2 = wasBuzz3;
                        }
                        if (wasBeep && !hasValidSound) {
                            clearSoundLocked();
                        }
                        if (wasBuzz2 && !hasValidVibrate) {
                            clearVibrateLocked();
                        }
                        boolean wasShowLights = this.mLights.remove(key);
                        if (canShowLightsLocked(record, aboveThreshold)) {
                            this.mLights.add(key);
                            updateLightsLocked();
                            if (this.mUseAttentionLight && (logicalLight = this.mAttentionLight) != null) {
                                logicalLight.pulse();
                            }
                            i5 = 1;
                        } else {
                            if (wasShowLights) {
                                updateLightsLocked();
                            }
                            i5 = i2;
                        }
                        int i7 = (r4 != 0 ? 2 : 0) | i4;
                        if (i5 == 0) {
                            i3 = 0;
                        }
                        buzzBeepBlink = i3 | i7;
                        if (buzzBeepBlink > 0) {
                            if (record.getSbn().isGroup() && record.getSbn().getNotification().isGroupSummary()) {
                                if (DEBUG_INTERRUPTIVENESS) {
                                    Slog.v(TAG, "INTERRUPTIVENESS: " + record.getKey() + " is not interruptive: summary");
                                }
                            } else if (!record.canBubble()) {
                                record.setInterruptive(true);
                                if (DEBUG_INTERRUPTIVENESS) {
                                    Slog.v(TAG, "INTERRUPTIVENESS: " + record.getKey() + " is interruptive: alerted");
                                }
                            } else if (DEBUG_INTERRUPTIVENESS) {
                                Slog.v(TAG, "INTERRUPTIVENESS: " + record.getKey() + " is not interruptive: bubble");
                            }
                            z = true;
                            MetricsLogger.action(record.getLogMaker().setCategory((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_PREFERENTIAL_NETWORK_SERVICE_ENABLED).setType(1).setSubtype(buzzBeepBlink));
                            EventLogTags.writeNotificationAlert(key, i4, r4, i5);
                        } else {
                            z = true;
                        }
                        if (i4 == 0 && r4 == 0) {
                            z = false;
                        }
                        record.setAudiblyAlerted(z);
                        return buzzBeepBlink;
                    }
                } else {
                    i6 = 0;
                    i2 = 0;
                }
                vibration = vibration2;
                hasValidVibrate = vibration == null;
                if (hasValidSound2) {
                }
                if (hasAudibleAlert) {
                }
                aboveThreshold = aboveThreshold2;
                boolean z22 = wasBuzz4;
                i3 = 4;
                if ((record.getFlags() & 4) == 0) {
                }
                if (wasBeep) {
                    clearSoundLocked();
                }
                if (wasBuzz2) {
                    clearVibrateLocked();
                }
                boolean wasShowLights2 = this.mLights.remove(key);
                if (canShowLightsLocked(record, aboveThreshold)) {
                }
                int i72 = (r4 != 0 ? 2 : 0) | i4;
                if (i5 == 0) {
                }
                buzzBeepBlink = i3 | i72;
                if (buzzBeepBlink > 0) {
                }
                if (i4 == 0) {
                    z = false;
                }
                record.setAudiblyAlerted(z);
                return buzzBeepBlink;
            } else {
                i = 0;
                i2 = 0;
                aboveThreshold = aboveThreshold2;
                wasBuzz = wasBuzz4;
                i3 = 4;
            }
            i4 = i;
            wasBuzz2 = wasBuzz;
            if (wasBeep) {
            }
            if (wasBuzz2) {
            }
            boolean wasShowLights22 = this.mLights.remove(key);
            if (canShowLightsLocked(record, aboveThreshold)) {
            }
            int i722 = (r4 != 0 ? 2 : 0) | i4;
            if (i5 == 0) {
            }
            buzzBeepBlink = i3 | i722;
            if (buzzBeepBlink > 0) {
            }
            if (i4 == 0) {
            }
            record.setAudiblyAlerted(z);
            return buzzBeepBlink;
        }
        return 0;
    }

    boolean canShowLightsLocked(NotificationRecord record, boolean aboveThreshold) {
        if ((mTranLedLightExt.isLedWork() || this.mHasLight) && this.mNotificationPulseEnabled && record.getLight() != null && aboveThreshold && (record.getSuppressedVisualEffects() & 8) == 0) {
            Notification notification = record.getNotification();
            if (!record.isUpdate || (notification.flags & 8) == 0) {
                if (record.getSbn().isGroup() && record.getNotification().suppressAlertingDueToGrouping()) {
                    return false;
                }
                return (mTranLedLightExt.isLedWork() || !isInCall()) && isNotificationForCurrentUser(record);
            }
            return false;
        }
        return false;
    }

    boolean isInsistentUpdate(NotificationRecord record) {
        return (Objects.equals(record.getKey(), this.mSoundNotificationKey) || Objects.equals(record.getKey(), this.mVibrateNotificationKey)) && isCurrentlyInsistent();
    }

    boolean isCurrentlyInsistent() {
        return isLoopingRingtoneNotification(this.mNotificationsByKey.get(this.mSoundNotificationKey)) || isLoopingRingtoneNotification(this.mNotificationsByKey.get(this.mVibrateNotificationKey));
    }

    boolean shouldMuteNotificationLocked(NotificationRecord record) {
        Notification notification = record.getNotification();
        if ((!record.isUpdate || (notification.flags & 8) == 0) && !record.shouldPostSilently()) {
            String disableEffects = disableNotificationEffects(record);
            if (disableEffects != null) {
                ZenLog.traceDisableEffects(record, disableEffects);
                return true;
            } else if (record.isIntercepted()) {
                return true;
            } else {
                if (record.getSbn().isGroup() && notification.suppressAlertingDueToGrouping()) {
                    return true;
                }
                String pkg = record.getSbn().getPackageName();
                if (this.mUsageStats.isAlertRateLimited(pkg)) {
                    Slog.e(TAG, "Muting recently noisy " + record.getKey());
                    return true;
                } else if (!isCurrentlyInsistent() || isInsistentUpdate(record)) {
                    boolean isBubbleOrOverflowed = record.canBubble() && (record.isFlagBubbleRemoved() || record.getNotification().isBubbleNotification());
                    return record.isUpdate && !record.isInterruptive() && isBubbleOrOverflowed && record.getNotification().getBubbleMetadata() != null && record.getNotification().getBubbleMetadata().isNotificationSuppressed();
                } else {
                    return true;
                }
            }
        }
        return true;
    }

    private boolean isLoopingRingtoneNotification(NotificationRecord playingRecord) {
        if (playingRecord != null && playingRecord.getAudioAttributes().getUsage() == 6 && (playingRecord.getNotification().flags & 4) != 0) {
            return true;
        }
        return false;
    }

    private boolean playSound(NotificationRecord record, Uri soundUri) {
        boolean looping = (record.getNotification().flags & 4) != 0;
        if (!this.mAudioManager.isAudioFocusExclusive() && this.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(record.getAudioAttributes())) != 0) {
            long identity = Binder.clearCallingIdentity();
            try {
                IRingtonePlayer player = this.mAudioManager.getRingtonePlayer();
                if (player != null) {
                    if (DBG) {
                        Slog.v(TAG, "Playing sound " + soundUri + " with attributes " + record.getAudioAttributes());
                    }
                    player.playAsync(soundUri, record.getSbn().getUser(), looping, record.getAudioAttributes());
                    Binder.restoreCallingIdentity(identity);
                    return true;
                }
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
            Binder.restoreCallingIdentity(identity);
        }
        return false;
    }

    private boolean playVibration(final NotificationRecord record, final VibrationEffect effect, boolean delayVibForSound) {
        long identity = Binder.clearCallingIdentity();
        try {
            if (delayVibForSound) {
                new Thread(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        NotificationManagerService.this.m5135x39b69423(record, effect);
                    }
                }).start();
            } else {
                vibrate(record, effect, false);
            }
            return true;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$playVibration$9$com-android-server-notification-NotificationManagerService  reason: not valid java name */
    public /* synthetic */ void m5135x39b69423(NotificationRecord record, VibrationEffect effect) {
        int waitMs = this.mAudioManager.getFocusRampTimeMs(3, record.getAudioAttributes());
        if (DBG) {
            Slog.v(TAG, "Delaying vibration for notification " + record.getKey() + " by " + waitMs + "ms");
        }
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
        }
        synchronized (this.mNotificationLock) {
            if (this.mNotificationsByKey.get(record.getKey()) != null) {
                if (record.getKey().equals(this.mVibrateNotificationKey)) {
                    vibrate(record, effect, true);
                } else if (DBG) {
                    Slog.v(TAG, "No vibration for notification " + record.getKey() + ": a new notification is vibrating, or effects were cleared while waiting");
                }
            } else {
                Slog.w(TAG, "No vibration for canceled notification " + record.getKey());
            }
        }
    }

    private void vibrate(NotificationRecord record, VibrationEffect effect, boolean delayed) {
        String reason = "Notification (" + record.getSbn().getOpPkg() + " " + record.getSbn().getUid() + ") " + (delayed ? "(Delayed)" : "");
        this.mVibratorHelper.vibrate(effect, record.getAudioAttributes(), reason);
    }

    private boolean isNotificationForCurrentUser(NotificationRecord record) {
        long token = Binder.clearCallingIdentity();
        try {
            int currentUser = ActivityManager.getCurrentUser();
            Binder.restoreCallingIdentity(token);
            return record.getUserId() == -1 || record.getUserId() == currentUser || this.mUserProfiles.isCurrentProfile(record.getUserId());
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    /* JADX WARN: Type inference failed for: r1v4, types: [com.android.server.notification.NotificationManagerService$13] */
    protected void playInCallNotification() {
        ContentResolver cr = getContext().getContentResolver();
        if (this.mAudioManager.getRingerModeInternal() == 2 && Settings.Secure.getIntForUser(cr, "in_call_notification_enabled", 1, cr.getUserId()) != 0) {
            new Thread() { // from class: com.android.server.notification.NotificationManagerService.13
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() {
                    long identity = Binder.clearCallingIdentity();
                    try {
                        IRingtonePlayer player = NotificationManagerService.this.mAudioManager.getRingtonePlayer();
                        if (player != null) {
                            if (NotificationManagerService.this.mCallNotificationToken != null) {
                                player.stop(NotificationManagerService.this.mCallNotificationToken);
                            }
                            NotificationManagerService.this.mCallNotificationToken = new Binder();
                            player.play(NotificationManagerService.this.mCallNotificationToken, NotificationManagerService.this.mInCallNotificationUri, NotificationManagerService.this.mInCallNotificationAudioAttributes, NotificationManagerService.this.mInCallNotificationVolume, false);
                        }
                    } catch (RemoteException e) {
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(identity);
                        throw th;
                    }
                    Binder.restoreCallingIdentity(identity);
                }
            }.start();
        }
    }

    void showNextToastLocked(boolean lastToastWasTextRecord) {
        if (this.mIsCurrentToastShown) {
            return;
        }
        ToastRecord record = this.mToastQueue.get(0);
        while (record != null) {
            int userId = UserHandle.getUserId(record.uid);
            boolean rateLimitingEnabled = !this.mToastRateLimitingDisabledUids.contains(Integer.valueOf(record.uid));
            boolean isWithinQuota = this.mToastRateLimiter.isWithinQuota(userId, record.pkg, TOAST_QUOTA_TAG) || isExemptFromRateLimiting(record.pkg, userId);
            boolean isPackageInForeground = isPackageInForegroundForToast(record.uid);
            if (tryShowToast(record, rateLimitingEnabled, isWithinQuota, isPackageInForeground)) {
                scheduleDurationReachedLocked(record, lastToastWasTextRecord);
                this.mIsCurrentToastShown = true;
                if (rateLimitingEnabled && !isPackageInForeground) {
                    this.mToastRateLimiter.noteEvent(userId, record.pkg, TOAST_QUOTA_TAG);
                    return;
                }
                return;
            }
            int index = this.mToastQueue.indexOf(record);
            if (index >= 0) {
                ToastRecord toast = this.mToastQueue.remove(index);
                this.mWindowManagerInternal.removeWindowToken(toast.windowToken, true, toast.displayId);
            }
            record = this.mToastQueue.size() > 0 ? this.mToastQueue.get(0) : null;
        }
    }

    private boolean tryShowToast(ToastRecord record, boolean rateLimitingEnabled, boolean isWithinQuota, boolean isPackageInForeground) {
        if (rateLimitingEnabled && !isWithinQuota && !isPackageInForeground) {
            reportCompatRateLimitingToastsChange(record.uid);
            Slog.w(TAG, "Package " + record.pkg + " is above allowed toast quota, the following toast was blocked and discarded: " + record);
            return false;
        } else if (blockToast(record.uid, record.isSystemToast, record.isAppRendered(), isPackageInForeground)) {
            Slog.w(TAG, "Blocking custom toast from package " + record.pkg + " due to package not in the foreground at the time of showing the toast");
            return false;
        } else {
            return record.show();
        }
    }

    private boolean isExemptFromRateLimiting(String pkg, int userId) {
        try {
            boolean isExemptFromRateLimiting = this.mPackageManager.checkPermission("android.permission.UNLIMITED_TOASTS", pkg, userId) == 0;
            return isExemptFromRateLimiting;
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to connect with package manager");
            return false;
        }
    }

    private void reportCompatRateLimitingToastsChange(int uid) {
        long id = Binder.clearCallingIdentity();
        try {
            try {
                this.mPlatformCompat.reportChangeByUid((long) RATE_LIMIT_TOASTS, uid);
            } catch (RemoteException e) {
                Slog.e(TAG, "Unexpected exception while reporting toast was blocked due to rate limiting", e);
            }
        } finally {
            Binder.restoreCallingIdentity(id);
        }
    }

    void cancelToastLocked(int index) {
        ToastRecord record = this.mToastQueue.get(index);
        record.hide();
        if (index == 0) {
            this.mIsCurrentToastShown = false;
        }
        ToastRecord lastToast = this.mToastQueue.remove(index);
        this.mWindowManagerInternal.removeWindowToken(lastToast.windowToken, false, lastToast.displayId);
        scheduleKillTokenTimeout(lastToast);
        keepProcessAliveForToastIfNeededLocked(record.pid);
        if (this.mToastQueue.size() > 0) {
            showNextToastLocked(lastToast instanceof TextToastRecord);
        }
    }

    void finishWindowTokenLocked(IBinder t, int displayId) {
        this.mHandler.removeCallbacksAndMessages(t);
        this.mWindowManagerInternal.removeWindowToken(t, true, displayId);
    }

    private void scheduleDurationReachedLocked(ToastRecord r, boolean lastToastWasTextRecord) {
        this.mHandler.removeCallbacksAndMessages(r);
        Message m = Message.obtain(this.mHandler, 2, r);
        int delay = this.mAccessibilityManager.getRecommendedTimeoutMillis(r.getDuration() == 1 ? LONG_DELAY : 2000, 2);
        if (lastToastWasTextRecord) {
            delay += 250;
        }
        if (r instanceof TextToastRecord) {
            delay += FrameworkStatsLog.DEVICE_ROTATED;
        }
        this.mHandler.sendMessageDelayed(m, delay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDurationReached(ToastRecord record) {
        if (DBG) {
            Slog.d(TAG, "Timeout pkg=" + record.pkg + " token=" + record.token);
        }
        synchronized (this.mToastQueue) {
            int index = indexOfToastLocked(record.pkg, record.token);
            if (index >= 0) {
                cancelToastLocked(index);
            }
        }
    }

    private void scheduleKillTokenTimeout(ToastRecord r) {
        this.mHandler.removeCallbacksAndMessages(r);
        Message m = Message.obtain(this.mHandler, 7, r);
        this.mHandler.sendMessageDelayed(m, 11000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleKillTokenTimeout(ToastRecord record) {
        if (DBG) {
            Slog.d(TAG, "Kill Token Timeout token=" + record.windowToken);
        }
        synchronized (this.mToastQueue) {
            finishWindowTokenLocked(record.windowToken, record.displayId);
        }
    }

    int indexOfToastLocked(String pkg, IBinder token) {
        ArrayList<ToastRecord> list = this.mToastQueue;
        int len = list.size();
        for (int i = 0; i < len; i++) {
            ToastRecord r = list.get(i);
            if (r.pkg.equals(pkg) && r.token == token) {
                return i;
            }
        }
        return -1;
    }

    public void keepProcessAliveForToastIfNeeded(int pid) {
        synchronized (this.mToastQueue) {
            keepProcessAliveForToastIfNeededLocked(pid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void keepProcessAliveForToastIfNeededLocked(int pid) {
        int toastCount = 0;
        ArrayList<ToastRecord> list = this.mToastQueue;
        int n = list.size();
        for (int i = 0; i < n; i++) {
            ToastRecord r = list.get(i);
            if (r.pid == pid && r.keepProcessAlive()) {
                toastCount++;
            }
        }
        try {
            this.mAm.setProcessImportant(this.mForegroundToken, pid, toastCount > 0, "toast");
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isPackageInForegroundForToast(int callingUid) {
        return this.mAtm.hasResumedActivity(callingUid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean blockToast(int uid, boolean isSystemToast, boolean isAppRenderedToast, boolean isPackageInForeground) {
        return isAppRenderedToast && !isSystemToast && !isPackageInForeground && CompatChanges.isChangeEnabled((long) CHANGE_BACKGROUND_CUSTOM_TOAST_BLOCK, uid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:47:0x0098  */
    /* JADX WARN: Removed duplicated region for block: B:58:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void handleRankingReconsideration(Message message) {
        NotificationRecord record;
        if (!(message.obj instanceof RankingReconsideration)) {
            return;
        }
        RankingReconsideration recon = (RankingReconsideration) message.obj;
        recon.run();
        synchronized (this.mNotificationLock) {
            try {
                try {
                    record = this.mNotificationsByKey.get(recon.getKey());
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    if (record == null) {
                        return;
                    }
                    int indexBefore = findNotificationRecordIndexLocked(record);
                    boolean interceptBefore = record.isIntercepted();
                    int visibilityBefore = record.getPackageVisibilityOverride();
                    boolean interruptiveBefore = record.isInterruptive();
                    recon.applyChangesLocked(record);
                    applyZenModeLocked(record);
                    this.mRankingHelper.sort(this.mNotificationList);
                    boolean changed = true;
                    boolean indexChanged = indexBefore != findNotificationRecordIndexLocked(record);
                    boolean interceptChanged = interceptBefore != record.isIntercepted();
                    boolean visibilityChanged = visibilityBefore != record.getPackageVisibilityOverride();
                    boolean interruptiveChanged = record.canBubble() && interruptiveBefore != record.isInterruptive();
                    if (!indexChanged && !interceptChanged && !visibilityChanged && !interruptiveChanged) {
                        changed = false;
                    }
                    if (interceptBefore) {
                        if (!record.isIntercepted()) {
                            if (record.isNewEnoughForAlerting(System.currentTimeMillis())) {
                                buzzBeepBlinkLocked(record);
                            }
                            if (!changed) {
                                this.mHandler.scheduleSendRankingUpdate();
                                return;
                            }
                            return;
                        }
                    }
                    if (!changed) {
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class NotificationRecordExtractorData {
        boolean mAllowBubble;
        NotificationChannel mChannel;
        String mGroupKey;
        int mImportance;
        boolean mIsBubble;
        boolean mIsConversation;
        ArrayList<String> mOverridePeople;
        int mPosition;
        float mRankingScore;
        boolean mShowBadge;
        ArrayList<CharSequence> mSmartReplies;
        ArrayList<SnoozeCriterion> mSnoozeCriteria;
        Integer mSuppressVisually;
        ArrayList<Notification.Action> mSystemSmartActions;
        Integer mUserSentiment;
        int mVisibility;

        NotificationRecordExtractorData(int position, int visibility, boolean showBadge, boolean allowBubble, boolean isBubble, NotificationChannel channel, String groupKey, ArrayList<String> overridePeople, ArrayList<SnoozeCriterion> snoozeCriteria, Integer userSentiment, Integer suppressVisually, ArrayList<Notification.Action> systemSmartActions, ArrayList<CharSequence> smartReplies, int importance, float rankingScore, boolean isConversation) {
            this.mPosition = position;
            this.mVisibility = visibility;
            this.mShowBadge = showBadge;
            this.mAllowBubble = allowBubble;
            this.mIsBubble = isBubble;
            this.mChannel = channel;
            this.mGroupKey = groupKey;
            this.mOverridePeople = overridePeople;
            this.mSnoozeCriteria = snoozeCriteria;
            this.mUserSentiment = userSentiment;
            this.mSuppressVisually = suppressVisually;
            this.mSystemSmartActions = systemSmartActions;
            this.mSmartReplies = smartReplies;
            this.mImportance = importance;
            this.mRankingScore = rankingScore;
            this.mIsConversation = isConversation;
        }

        boolean hasDiffForRankingLocked(NotificationRecord r, int newPosition) {
            return (this.mPosition == newPosition && this.mVisibility == r.getPackageVisibilityOverride() && this.mShowBadge == r.canShowBadge() && this.mAllowBubble == r.canBubble() && this.mIsBubble == r.getNotification().isBubbleNotification() && Objects.equals(this.mChannel, r.getChannel()) && Objects.equals(this.mGroupKey, r.getGroupKey()) && Objects.equals(this.mOverridePeople, r.getPeopleOverride()) && Objects.equals(this.mSnoozeCriteria, r.getSnoozeCriteria()) && Objects.equals(this.mUserSentiment, Integer.valueOf(r.getUserSentiment())) && Objects.equals(this.mSuppressVisually, Integer.valueOf(r.getSuppressedVisualEffects())) && Objects.equals(this.mSystemSmartActions, r.getSystemGeneratedSmartActions()) && Objects.equals(this.mSmartReplies, r.getSmartReplies()) && this.mImportance == r.getImportance()) ? false : true;
        }

        boolean hasDiffForLoggingLocked(NotificationRecord r, int newPosition) {
            return (this.mPosition == newPosition && Objects.equals(this.mChannel, r.getChannel()) && Objects.equals(this.mGroupKey, r.getGroupKey()) && Objects.equals(this.mOverridePeople, r.getPeopleOverride()) && Objects.equals(this.mSnoozeCriteria, r.getSnoozeCriteria()) && Objects.equals(this.mUserSentiment, Integer.valueOf(r.getUserSentiment())) && Objects.equals(this.mSystemSmartActions, r.getSystemGeneratedSmartActions()) && Objects.equals(this.mSmartReplies, r.getSmartReplies()) && this.mImportance == r.getImportance() && r.rankingScoreMatches(this.mRankingScore) && this.mIsConversation == r.isConversation()) ? false : true;
        }
    }

    void handleRankingSort() {
        if (this.mRankingHelper == null) {
            return;
        }
        synchronized (this.mNotificationLock) {
            int N = this.mNotificationList.size();
            ArrayMap<String, NotificationRecordExtractorData> extractorDataBefore = new ArrayMap<>(N);
            for (int i = 0; i < N; i++) {
                NotificationRecord r = this.mNotificationList.get(i);
                NotificationRecordExtractorData extractorData = new NotificationRecordExtractorData(i, r.getPackageVisibilityOverride(), r.canShowBadge(), r.canBubble(), r.getNotification().isBubbleNotification(), r.getChannel(), r.getGroupKey(), r.getPeopleOverride(), r.getSnoozeCriteria(), Integer.valueOf(r.getUserSentiment()), Integer.valueOf(r.getSuppressedVisualEffects()), r.getSystemGeneratedSmartActions(), r.getSmartReplies(), r.getImportance(), r.getRankingScore(), r.isConversation());
                extractorDataBefore.put(r.getKey(), extractorData);
                this.mRankingHelper.extractSignals(r);
            }
            this.mRankingHelper.sort(this.mNotificationList);
            for (int i2 = 0; i2 < N; i2++) {
                NotificationRecord r2 = this.mNotificationList.get(i2);
                if (extractorDataBefore.containsKey(r2.getKey())) {
                    if (extractorDataBefore.get(r2.getKey()).hasDiffForRankingLocked(r2, i2)) {
                        this.mHandler.scheduleSendRankingUpdate();
                    }
                    if (r2.hasPendingLogUpdate()) {
                        NotificationRecordExtractorData prevData = extractorDataBefore.get(r2.getKey());
                        if (prevData.hasDiffForLoggingLocked(r2, i2)) {
                            this.mNotificationRecordLogger.logNotificationAdjusted(r2, i2, 0, getGroupInstanceId(r2.getSbn().getGroupKey()));
                        }
                        r2.setPendingLogUpdate(false);
                    }
                }
            }
        }
    }

    private void recordCallerLocked(NotificationRecord record) {
        if (this.mZenModeHelper.isCall(record)) {
            this.mZenModeHelper.recordCaller(record);
        }
    }

    private void applyZenModeLocked(NotificationRecord record) {
        record.setIntercepted(this.mZenModeHelper.shouldIntercept(record));
        if (record.isIntercepted()) {
            record.setSuppressedVisualEffects(this.mZenModeHelper.getConsolidatedNotificationPolicy().suppressedVisualEffects);
        } else {
            record.setSuppressedVisualEffects(0);
        }
    }

    private int findNotificationRecordIndexLocked(NotificationRecord target) {
        return this.mRankingHelper.indexOf(this.mNotificationList, target);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSendRankingUpdate() {
        synchronized (this.mNotificationLock) {
            this.mListeners.notifyRankingUpdateLocked(null);
        }
    }

    private void scheduleListenerHintsChanged(int state) {
        this.mHandler.removeMessages(5);
        this.mHandler.obtainMessage(5, state, 0).sendToTarget();
    }

    private void scheduleInterruptionFilterChanged(int listenerInterruptionFilter) {
        this.mHandler.removeMessages(6);
        this.mHandler.obtainMessage(6, listenerInterruptionFilter, 0).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleListenerHintsChanged(int hints) {
        synchronized (this.mNotificationLock) {
            this.mListeners.notifyListenerHintsChangedLocked(hints);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleListenerInterruptionFilterChanged(int interruptionFilter) {
        synchronized (this.mNotificationLock) {
            this.mListeners.notifyInterruptionFilterChanged(interruptionFilter);
        }
    }

    void handleOnPackageChanged(boolean removingPackage, int changeUserId, String[] pkgList, int[] uidList) {
        this.mListeners.onPackagesChanged(removingPackage, pkgList, uidList);
        this.mAssistants.onPackagesChanged(removingPackage, pkgList, uidList);
        this.mConditionProviders.onPackagesChanged(removingPackage, pkgList, uidList);
        if (removingPackage) {
            INotificationManagerServiceLice.Instance().onPackagesChanged(removingPackage, changeUserId, pkgList, uidList);
        }
        boolean preferencesChanged = removingPackage | this.mPreferencesHelper.onPackagesChanged(removingPackage, changeUserId, pkgList, uidList);
        if (removingPackage) {
            int size = Math.min(pkgList.length, uidList.length);
            for (int i = 0; i < size; i++) {
                String pkg = pkgList[i];
                int uid = uidList[i];
                this.mHistoryManager.onPackageRemoved(UserHandle.getUserId(uid), pkg);
            }
        }
        if (preferencesChanged) {
            handleSavePolicyFile();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    NotificationManagerService.this.handleDurationReached((ToastRecord) msg.obj);
                    return;
                case 3:
                default:
                    return;
                case 4:
                    NotificationManagerService.this.handleSendRankingUpdate();
                    return;
                case 5:
                    NotificationManagerService.this.handleListenerHintsChanged(msg.arg1);
                    return;
                case 6:
                    NotificationManagerService.this.handleListenerInterruptionFilterChanged(msg.arg1);
                    return;
                case 7:
                    NotificationManagerService.this.handleKillTokenTimeout((ToastRecord) msg.obj);
                    return;
                case 8:
                    SomeArgs args = (SomeArgs) msg.obj;
                    NotificationManagerService.this.handleOnPackageChanged(((Boolean) args.arg1).booleanValue(), args.argi1, (String[]) args.arg2, (int[]) args.arg3);
                    args.recycle();
                    return;
            }
        }

        protected void scheduleSendRankingUpdate() {
            if (!hasMessages(4)) {
                Message m = Message.obtain(this, 4);
                sendMessage(m);
            }
        }

        protected void scheduleCancelNotification(CancelNotificationRunnable cancelRunnable) {
            if (!hasCallbacks(cancelRunnable)) {
                sendMessage(Message.obtain(this, cancelRunnable));
            }
        }

        protected void scheduleOnPackageChanged(boolean removingPackage, int changeUserId, String[] pkgList, int[] uidList) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = Boolean.valueOf(removingPackage);
            args.argi1 = changeUserId;
            args.arg2 = pkgList;
            args.arg3 = uidList;
            sendMessage(Message.obtain(this, 8, args));
        }
    }

    /* loaded from: classes2.dex */
    private final class RankingHandlerWorker extends Handler implements RankingHandler {
        public RankingHandlerWorker(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1000:
                    NotificationManagerService.this.handleRankingReconsideration(msg);
                    return;
                case 1001:
                    NotificationManagerService.this.handleRankingSort();
                    return;
                default:
                    return;
            }
        }

        @Override // com.android.server.notification.RankingHandler
        public void requestSort() {
            removeMessages(1001);
            Message msg = Message.obtain();
            msg.what = 1001;
            sendMessage(msg);
        }

        @Override // com.android.server.notification.RankingHandler
        public void requestReconsideration(RankingReconsideration recon) {
            Message m = Message.obtain(this, 1000, recon);
            long delay = recon.getDelay(TimeUnit.MILLISECONDS);
            sendMessageDelayed(m, delay);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int clamp(int x, int low, int high) {
        return x < low ? low : x > high ? high : x;
    }

    void sendAccessibilityEvent(NotificationRecord record) {
        if (!this.mAccessibilityManager.isEnabled()) {
            return;
        }
        Notification notification = record.getNotification();
        CharSequence packageName = record.getSbn().getPackageName();
        AccessibilityEvent event = AccessibilityEvent.obtain(64);
        event.setPackageName(packageName);
        event.setClassName(Notification.class.getName());
        int visibilityOverride = record.getPackageVisibilityOverride();
        int notifVisibility = visibilityOverride == -1000 ? notification.visibility : visibilityOverride;
        int userId = record.getUser().getIdentifier();
        boolean needPublic = userId >= 0 && this.mKeyguardManager.isDeviceLocked(userId);
        if (needPublic && notifVisibility != 1) {
            event.setParcelableData(notification.publicVersion);
        } else {
            event.setParcelableData(notification);
        }
        CharSequence tickerText = notification.tickerText;
        if (!TextUtils.isEmpty(tickerText)) {
            event.getText().add(tickerText);
        }
        this.mAccessibilityManager.sendAccessibilityEvent(event);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean removeFromNotificationListsLocked(NotificationRecord r) {
        boolean wasPosted = false;
        NotificationRecord recordInList = findNotificationByListLocked(this.mNotificationList, r.getKey());
        if (recordInList != null) {
            this.mNotificationList.remove(recordInList);
            this.mNotificationsByKey.remove(recordInList.getSbn().getKey());
            wasPosted = true;
        }
        while (true) {
            NotificationRecord recordInList2 = findNotificationByListLocked(this.mEnqueuedNotifications, r.getKey());
            if (recordInList2 != null) {
                this.mEnqueuedNotifications.remove(recordInList2);
            } else {
                return wasPosted;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelNotificationLocked(NotificationRecord r, boolean sendDelete, int reason, boolean wasPosted, String listenerName, long cancellationElapsedTimeMs) {
        cancelNotificationLocked(r, sendDelete, reason, -1, -1, wasPosted, listenerName, cancellationElapsedTimeMs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:55:0x01c8  */
    /* JADX WARN: Removed duplicated region for block: B:59:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void cancelNotificationLocked(final NotificationRecord r, boolean sendDelete, int reason, int rank, int count, boolean wasPosted, String listenerName, long cancellationElapsedTimeMs) {
        PendingIntent deleteIntent;
        String canceledKey = r.getKey();
        PendingIntent pi = PendingIntent.getBroadcast(getContext(), 1, new Intent(ACTION_NOTIFICATION_TIMEOUT).setData(new Uri.Builder().scheme(SCHEME_TIMEOUT).appendPath(r.getKey()).build()).addFlags(268435456), 603979776);
        if (pi != null) {
            this.mAlarmManager.cancel(pi);
        }
        recordCallerLocked(r);
        if (r.getStats().getDismissalSurface() == -1) {
            r.recordDismissalSurface(0);
        }
        if (sendDelete && (deleteIntent = r.getNotification().deleteIntent) != null) {
            try {
                ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).clearPendingIntentAllowBgActivityStarts(deleteIntent.getTarget(), ALLOWLIST_TOKEN);
                deleteIntent.send();
            } catch (PendingIntent.CanceledException ex) {
                Slog.w(TAG, "canceled PendingIntent for " + r.getSbn().getPackageName(), ex);
            }
        }
        if (wasPosted) {
            if (r.getNotification().getSmallIcon() != null) {
                if (reason != 18) {
                    r.isCanceled = true;
                }
                this.mListeners.notifyRemovedLocked(r, reason, r.getStats());
                this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService.14
                    @Override // java.lang.Runnable
                    public void run() {
                        NotificationManagerService.this.mGroupHelper.onNotificationRemoved(r.getSbn());
                    }
                });
            }
            if (canceledKey.equals(this.mSoundNotificationKey)) {
                clearSoundLocked();
            }
            if (canceledKey.equals(this.mVibrateNotificationKey)) {
                clearVibrateLocked();
            }
            this.mLights.remove(canceledKey);
            mTranLedLightExt.upNotificationList(this.mLights);
        }
        switch (reason) {
            case 2:
            case 3:
            case 10:
            case 11:
                this.mUsageStats.registerDismissedByUser(r);
                break;
            case 8:
            case 9:
                this.mUsageStats.registerRemovedByApp(r);
                this.mUsageStatsManagerInternal.reportNotificationRemoved(r.getSbn().getOpPkg(), r.getUser(), cancellationElapsedTimeMs);
                break;
        }
        String groupKey = r.getGroupKey();
        NotificationRecord groupSummary = this.mSummaryByGroupKey.get(groupKey);
        if (groupSummary != null && groupSummary.getKey().equals(canceledKey)) {
            this.mSummaryByGroupKey.remove(groupKey);
        }
        ArrayMap<String, String> summaries = this.mAutobundledSummaries.get(Integer.valueOf(r.getSbn().getUserId()));
        if (summaries != null && r.getSbn().getKey().equals(summaries.get(r.getSbn().getPackageName()))) {
            summaries.remove(r.getSbn().getPackageName());
        }
        if (reason != 20) {
            this.mArchive.record(r.getSbn(), reason);
        }
        long now = System.currentTimeMillis();
        LogMaker logMaker = r.getItemLogMaker().setType(5).setSubtype(reason);
        if (rank != -1 && count != -1) {
            logMaker.addTaggedData(798, Integer.valueOf(rank)).addTaggedData(1395, Integer.valueOf(count));
            MetricsLogger.action(logMaker);
            EventLogTags.writeNotificationCanceled(canceledKey, reason, r.getLifespanMs(now), r.getFreshnessMs(now), r.getExposureMs(now), rank, count, listenerName);
            if (!wasPosted) {
                this.mNotificationRecordLogger.logNotificationCancelled(r, reason, r.getStats().getDismissalSurface());
                return;
            }
            return;
        }
        MetricsLogger.action(logMaker);
        EventLogTags.writeNotificationCanceled(canceledKey, reason, r.getLifespanMs(now), r.getFreshnessMs(now), r.getExposureMs(now), rank, count, listenerName);
        if (!wasPosted) {
        }
    }

    void updateUriPermissions(NotificationRecord newRecord, NotificationRecord oldRecord, String targetPkg, int targetUserId) {
        updateUriPermissions(newRecord, oldRecord, targetPkg, targetUserId, false);
    }

    void updateUriPermissions(NotificationRecord newRecord, NotificationRecord oldRecord, String targetPkg, int targetUserId, boolean onlyRevokeCurrentTarget) {
        IBinder permissionOwner;
        String key = newRecord != null ? newRecord.getKey() : oldRecord.getKey();
        boolean z = DBG;
        if (z) {
            Slog.d(TAG, key + ": updating permissions");
        }
        ArraySet<Uri> newUris = newRecord != null ? newRecord.getGrantableUris() : null;
        ArraySet<Uri> oldUris = oldRecord != null ? oldRecord.getGrantableUris() : null;
        if (newUris == null && oldUris == null) {
            return;
        }
        IBinder permissionOwner2 = null;
        if (newRecord != null && 0 == 0) {
            permissionOwner2 = newRecord.permissionOwner;
        }
        if (oldRecord != null && permissionOwner2 == null) {
            permissionOwner2 = oldRecord.permissionOwner;
        }
        if (newUris != null && permissionOwner2 == null) {
            if (z) {
                Slog.d(TAG, key + ": creating owner");
            }
            permissionOwner2 = this.mUgmInternal.newUriPermissionOwner("NOTIF:" + key);
        }
        if (newUris == null && permissionOwner2 != null && !onlyRevokeCurrentTarget) {
            destroyPermissionOwner(permissionOwner2, UserHandle.getUserId(oldRecord.getUid()), key);
            permissionOwner = null;
        } else {
            permissionOwner = permissionOwner2;
        }
        if (newUris != null && permissionOwner != null) {
            for (int i = 0; i < newUris.size(); i++) {
                Uri uri = newUris.valueAt(i);
                if (oldUris == null || !oldUris.contains(uri)) {
                    if (DBG) {
                        Slog.d(TAG, key + ": granting " + uri);
                    }
                    grantUriPermission(permissionOwner, uri, newRecord.getUid(), targetPkg, targetUserId);
                }
            }
        }
        if (oldUris != null && permissionOwner != null) {
            for (int i2 = 0; i2 < oldUris.size(); i2++) {
                Uri uri2 = oldUris.valueAt(i2);
                if (newUris == null || !newUris.contains(uri2)) {
                    if (DBG) {
                        Slog.d(TAG, key + ": revoking " + uri2);
                    }
                    if (onlyRevokeCurrentTarget) {
                        revokeUriPermission(permissionOwner, uri2, UserHandle.getUserId(oldRecord.getUid()), targetPkg, targetUserId);
                    } else {
                        revokeUriPermission(permissionOwner, uri2, UserHandle.getUserId(oldRecord.getUid()), null, -1);
                    }
                }
            }
        }
        if (newRecord != null) {
            newRecord.permissionOwner = permissionOwner;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void grantUriPermission(IBinder owner, Uri uri, int sourceUid, String targetPkg, int targetUserId) {
        if (uri == null || !ActivityTaskManagerInternal.ASSIST_KEY_CONTENT.equals(uri.getScheme())) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            try {
                this.mUgm.grantUriPermissionFromOwner(owner, sourceUid, targetPkg, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid)), targetUserId);
            } catch (RemoteException e) {
            } catch (SecurityException e2) {
                Slog.e(TAG, "Cannot grant uri access; " + sourceUid + " does not own " + uri);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void revokeUriPermission(IBinder owner, Uri uri, int sourceUserId, String targetPkg, int targetUserId) {
        if (uri == null || !ActivityTaskManagerInternal.ASSIST_KEY_CONTENT.equals(uri.getScheme())) {
            return;
        }
        int userId = ContentProvider.getUserIdFromUri(uri, sourceUserId);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mUgmInternal.revokeUriPermissionFromOwner(owner, ContentProvider.getUriWithoutUserId(uri), 1, userId, targetPkg, targetUserId);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void destroyPermissionOwner(IBinder owner, int userId, String logKey) {
        long ident = Binder.clearCallingIdentity();
        try {
            if (DBG) {
                Slog.d(TAG, logKey + ": destroying owner");
            }
            this.mUgmInternal.revokeUriPermissionFromOwner(owner, null, -1, userId);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    void cancelNotification(int callingUid, int callingPid, String pkg, String tag, int id, int mustHaveFlags, int mustNotHaveFlags, boolean sendDelete, int userId, int reason, ManagedServices.ManagedServiceInfo listener) {
        cancelNotification(callingUid, callingPid, pkg, tag, id, mustHaveFlags, mustNotHaveFlags, sendDelete, userId, reason, -1, -1, listener);
    }

    void cancelNotification(final int callingUid, final int callingPid, final String pkg, final String tag, final int id, final int mustHaveFlags, final int mustNotHaveFlags, final boolean sendDelete, final int userId, final int reason, final int rank, final int count, final ManagedServices.ManagedServiceInfo listener) {
        if (ITranNotificationManagerService.Instance().onCancelNotification(this.mHandler, new Supplier() { // from class: com.android.server.notification.NotificationManagerService$$ExternalSyntheticLambda6
            @Override // java.util.function.Supplier
            public final Object get() {
                return NotificationManagerService.this.m5129xca517b8(callingUid, callingPid, pkg, tag, id, mustHaveFlags, mustNotHaveFlags, sendDelete, userId, reason, rank, count, listener);
            }
        }, callingUid, callingPid, pkg, tag, id, mustHaveFlags, mustNotHaveFlags, sendDelete, userId, reason, rank, count, listener)) {
            return;
        }
        this.mHandler.scheduleCancelNotification(new CancelNotificationRunnable(callingUid, callingPid, pkg, tag, id, mustHaveFlags, mustNotHaveFlags, sendDelete, userId, reason, rank, count, listener, SystemClock.elapsedRealtime()));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelNotification$10$com-android-server-notification-NotificationManagerService  reason: not valid java name */
    public /* synthetic */ Runnable m5129xca517b8(int callingUid, int callingPid, String pkg, String tag, int id, int mustHaveFlags, int mustNotHaveFlags, boolean sendDelete, int userId, int reason, int rank, int count, ManagedServices.ManagedServiceInfo listener) {
        return new CancelNotificationRunnable(callingUid, callingPid, pkg, tag, id, mustHaveFlags, mustNotHaveFlags, sendDelete, userId, reason, rank, count, listener, SystemClock.elapsedRealtime());
    }

    private boolean notificationMatchesUserId(NotificationRecord r, int userId) {
        return userId == -1 || r.getUserId() == -1 || r.getUserId() == userId;
    }

    private boolean notificationMatchesCurrentProfiles(NotificationRecord r, int userId) {
        return notificationMatchesUserId(r, userId) || this.mUserProfiles.isCurrentProfile(r.getUserId());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.notification.NotificationManagerService$15  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass15 implements Runnable {
        final /* synthetic */ int val$callingPid;
        final /* synthetic */ int val$callingUid;
        final /* synthetic */ long val$cancellationElapsedTimeMs;
        final /* synthetic */ String val$channelId;
        final /* synthetic */ boolean val$doit;
        final /* synthetic */ ManagedServices.ManagedServiceInfo val$listener;
        final /* synthetic */ int val$mustHaveFlags;
        final /* synthetic */ int val$mustNotHaveFlags;
        final /* synthetic */ String val$pkg;
        final /* synthetic */ int val$reason;
        final /* synthetic */ int val$userId;

        AnonymousClass15(ManagedServices.ManagedServiceInfo managedServiceInfo, int i, int i2, String str, int i3, int i4, int i5, int i6, boolean z, String str2, long j) {
            this.val$listener = managedServiceInfo;
            this.val$callingUid = i;
            this.val$callingPid = i2;
            this.val$pkg = str;
            this.val$userId = i3;
            this.val$mustHaveFlags = i4;
            this.val$mustNotHaveFlags = i5;
            this.val$reason = i6;
            this.val$doit = z;
            this.val$channelId = str2;
            this.val$cancellationElapsedTimeMs = j;
        }

        @Override // java.lang.Runnable
        public void run() {
            ManagedServices.ManagedServiceInfo managedServiceInfo = this.val$listener;
            String listenerName = managedServiceInfo == null ? null : managedServiceInfo.component.toShortString();
            EventLogTags.writeNotificationCancelAll(this.val$callingUid, this.val$callingPid, this.val$pkg, this.val$userId, this.val$mustHaveFlags, this.val$mustNotHaveFlags, this.val$reason, listenerName);
            if (!this.val$doit) {
                return;
            }
            synchronized (NotificationManagerService.this.mNotificationLock) {
                final int i = this.val$mustHaveFlags;
                final int i2 = this.val$mustNotHaveFlags;
                FlagChecker flagChecker = new FlagChecker() { // from class: com.android.server.notification.NotificationManagerService$15$$ExternalSyntheticLambda0
                    @Override // com.android.server.notification.NotificationManagerService.FlagChecker
                    public final boolean apply(int i3) {
                        return NotificationManagerService.AnonymousClass15.lambda$run$0(i, i2, i3);
                    }
                };
                NotificationManagerService notificationManagerService = NotificationManagerService.this;
                notificationManagerService.cancelAllNotificationsByListLocked(notificationManagerService.mNotificationList, this.val$callingUid, this.val$callingPid, this.val$pkg, true, this.val$channelId, flagChecker, false, this.val$userId, false, this.val$reason, listenerName, true, this.val$cancellationElapsedTimeMs);
                NotificationManagerService notificationManagerService2 = NotificationManagerService.this;
                notificationManagerService2.cancelAllNotificationsByListLocked(notificationManagerService2.mEnqueuedNotifications, this.val$callingUid, this.val$callingPid, this.val$pkg, true, this.val$channelId, flagChecker, false, this.val$userId, false, this.val$reason, listenerName, false, this.val$cancellationElapsedTimeMs);
                NotificationManagerService.this.mSnoozeHelper.cancel(this.val$userId, this.val$pkg);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$run$0(int mustHaveFlags, int mustNotHaveFlags, int flags) {
            return (flags & mustHaveFlags) == mustHaveFlags && (flags & mustNotHaveFlags) == 0;
        }
    }

    void cancelAllNotificationsInt(int callingUid, int callingPid, String pkg, String channelId, int mustHaveFlags, int mustNotHaveFlags, boolean doit, int userId, int reason, ManagedServices.ManagedServiceInfo listener) {
        long cancellationElapsedTimeMs = SystemClock.elapsedRealtime();
        this.mHandler.post(new AnonymousClass15(listener, callingUid, callingPid, pkg, userId, mustHaveFlags, mustNotHaveFlags, reason, doit, channelId, cancellationElapsedTimeMs));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelAllNotificationsByListLocked(ArrayList<NotificationRecord> notificationList, int callingUid, int callingPid, String pkg, boolean nullPkgIndicatesUserSwitch, String channelId, FlagChecker flagChecker, boolean includeCurrentProfiles, int userId, boolean sendDelete, int reason, String listenerName, boolean wasPosted, long cancellationElapsedTimeMs) {
        int i;
        Set<String> childNotifications = null;
        for (int i2 = notificationList.size() - 1; i2 >= 0; i2--) {
            NotificationRecord r = notificationList.get(i2);
            if (includeCurrentProfiles) {
                if (!notificationMatchesCurrentProfiles(r, userId)) {
                }
                if ((nullPkgIndicatesUserSwitch || pkg != null || r.getUserId() != -1) && flagChecker.apply(r.getFlags()) && ((pkg == null || r.getSbn().getPackageName().equals(pkg)) && (channelId == null || channelId.equals(r.getChannel().getId())))) {
                    if (!r.getSbn().isGroup() && r.getNotification().isGroupChild()) {
                        if (childNotifications == null) {
                            childNotifications = new HashSet<>();
                        }
                        childNotifications.add(r.getKey());
                    } else {
                        notificationList.remove(i2);
                        this.mNotificationsByKey.remove(r.getKey());
                        r.recordDismissalSentiment(1);
                        cancelNotificationLocked(r, sendDelete, reason, wasPosted, listenerName, cancellationElapsedTimeMs);
                    }
                }
            } else {
                if (!notificationMatchesUserId(r, userId)) {
                }
                if (nullPkgIndicatesUserSwitch) {
                }
                if (!r.getSbn().isGroup()) {
                }
                notificationList.remove(i2);
                this.mNotificationsByKey.remove(r.getKey());
                r.recordDismissalSentiment(1);
                cancelNotificationLocked(r, sendDelete, reason, wasPosted, listenerName, cancellationElapsedTimeMs);
            }
        }
        if (childNotifications != null) {
            int M = notificationList.size();
            int i3 = M - 1;
            while (i3 >= 0) {
                NotificationRecord r2 = notificationList.get(i3);
                if (!childNotifications.contains(r2.getKey())) {
                    i = i3;
                } else {
                    notificationList.remove(i3);
                    this.mNotificationsByKey.remove(r2.getKey());
                    r2.recordDismissalSentiment(1);
                    i = i3;
                    cancelNotificationLocked(r2, sendDelete, reason, wasPosted, listenerName, cancellationElapsedTimeMs);
                }
                i3 = i - 1;
            }
            updateLightsLocked();
        }
    }

    void snoozeNotificationInt(String key, long duration, String snoozeCriterionId, ManagedServices.ManagedServiceInfo listener) {
        if (listener == null) {
            return;
        }
        String listenerName = listener.component.toShortString();
        if ((duration <= 0 && snoozeCriterionId == null) || key == null) {
            return;
        }
        synchronized (this.mNotificationLock) {
            NotificationRecord r = findInCurrentAndSnoozedNotificationByKeyLocked(key);
            if (r == null) {
                return;
            }
            if (listener.enabledAndUserMatches(r.getSbn().getNormalizedUserId())) {
                if (DBG) {
                    Slog.d(TAG, String.format("snooze event(%s, %d, %s, %s)", key, Long.valueOf(duration), snoozeCriterionId, listenerName));
                }
                this.mHandler.post(new SnoozeNotificationRunnable(key, duration, snoozeCriterionId));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unsnoozeNotificationInt(String key, ManagedServices.ManagedServiceInfo listener, boolean muteOnReturn) {
        String listenerName = listener == null ? null : listener.component.toShortString();
        if (DBG) {
            Slog.d(TAG, String.format("unsnooze event(%s, %s)", key, listenerName));
        }
        this.mSnoozeHelper.repost(key, muteOnReturn);
        handleSavePolicyFile();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.notification.NotificationManagerService$16  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass16 implements Runnable {
        final /* synthetic */ int val$callingPid;
        final /* synthetic */ int val$callingUid;
        final /* synthetic */ long val$cancellationElapsedTimeMs;
        final /* synthetic */ boolean val$includeCurrentProfiles;
        final /* synthetic */ ManagedServices.ManagedServiceInfo val$listener;
        final /* synthetic */ int val$reason;
        final /* synthetic */ int val$userId;

        AnonymousClass16(ManagedServices.ManagedServiceInfo managedServiceInfo, int i, int i2, int i3, int i4, boolean z, long j) {
            this.val$listener = managedServiceInfo;
            this.val$callingUid = i;
            this.val$callingPid = i2;
            this.val$userId = i3;
            this.val$reason = i4;
            this.val$includeCurrentProfiles = z;
            this.val$cancellationElapsedTimeMs = j;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                ManagedServices.ManagedServiceInfo managedServiceInfo = this.val$listener;
                String listenerName = managedServiceInfo == null ? null : managedServiceInfo.component.toShortString();
                EventLogTags.writeNotificationCancelAll(this.val$callingUid, this.val$callingPid, null, this.val$userId, 0, 0, this.val$reason, listenerName);
                final int i = this.val$reason;
                FlagChecker flagChecker = new FlagChecker() { // from class: com.android.server.notification.NotificationManagerService$16$$ExternalSyntheticLambda0
                    @Override // com.android.server.notification.NotificationManagerService.FlagChecker
                    public final boolean apply(int i2) {
                        return NotificationManagerService.AnonymousClass16.lambda$run$0(i, i2);
                    }
                };
                NotificationManagerService notificationManagerService = NotificationManagerService.this;
                notificationManagerService.cancelAllNotificationsByListLocked(notificationManagerService.mNotificationList, this.val$callingUid, this.val$callingPid, null, false, null, flagChecker, this.val$includeCurrentProfiles, this.val$userId, true, this.val$reason, listenerName, true, this.val$cancellationElapsedTimeMs);
                NotificationManagerService notificationManagerService2 = NotificationManagerService.this;
                notificationManagerService2.cancelAllNotificationsByListLocked(notificationManagerService2.mEnqueuedNotifications, this.val$callingUid, this.val$callingPid, null, false, null, flagChecker, this.val$includeCurrentProfiles, this.val$userId, true, this.val$reason, listenerName, false, this.val$cancellationElapsedTimeMs);
                NotificationManagerService.this.mSnoozeHelper.cancel(this.val$userId, this.val$includeCurrentProfiles);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$run$0(int reason, int flags) {
            int flagsToCheck = 34;
            if (11 == reason || 3 == reason) {
                flagsToCheck = 34 | 4096;
            }
            if ((flags & flagsToCheck) != 0) {
                return false;
            }
            return true;
        }
    }

    void cancelAllLocked(int callingUid, int callingPid, int userId, int reason, ManagedServices.ManagedServiceInfo listener, boolean includeCurrentProfiles) {
        long cancellationElapsedTimeMs = SystemClock.elapsedRealtime();
        this.mHandler.post(new AnonymousClass16(listener, callingUid, callingPid, userId, reason, includeCurrentProfiles, cancellationElapsedTimeMs));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelGroupChildrenLocked(NotificationRecord r, int callingUid, int callingPid, String listenerName, boolean sendDelete, FlagChecker flagChecker, int reason, long cancellationElapsedTimeMs) {
        Notification n = r.getNotification();
        if (!n.isGroupSummary()) {
            return;
        }
        String pkg = r.getSbn().getPackageName();
        if (pkg == null) {
            if (DBG) {
                Slog.e(TAG, "No package for group summary: " + r.getKey());
                return;
            }
            return;
        }
        cancelGroupChildrenByListLocked(this.mNotificationList, r, callingUid, callingPid, listenerName, sendDelete, true, flagChecker, reason, cancellationElapsedTimeMs);
        cancelGroupChildrenByListLocked(this.mEnqueuedNotifications, r, callingUid, callingPid, listenerName, sendDelete, false, flagChecker, reason, cancellationElapsedTimeMs);
    }

    private void cancelGroupChildrenByListLocked(ArrayList<NotificationRecord> notificationList, NotificationRecord parentNotification, int callingUid, int callingPid, String listenerName, boolean sendDelete, boolean wasPosted, FlagChecker flagChecker, int reason, long cancellationElapsedTimeMs) {
        String pkg = parentNotification.getSbn().getPackageName();
        int userId = parentNotification.getUserId();
        for (int i = notificationList.size() - 1; i >= 0; i--) {
            NotificationRecord childR = notificationList.get(i);
            StatusBarNotification childSbn = childR.getSbn();
            if (childSbn.isGroup() && !childSbn.getNotification().isGroupSummary()) {
                if (childR.getGroupKey().equals(parentNotification.getGroupKey())) {
                    if (flagChecker != null) {
                        if (!flagChecker.apply(childR.getFlags())) {
                        }
                    }
                    if (childR.getChannel().isImportantConversation() && reason == 2) {
                    }
                    EventLogTags.writeNotificationCancel(callingUid, callingPid, pkg, childSbn.getId(), childSbn.getTag(), userId, 0, 0, 12, listenerName);
                    notificationList.remove(i);
                    this.mNotificationsByKey.remove(childR.getKey());
                    cancelNotificationLocked(childR, sendDelete, 12, wasPosted, listenerName, cancellationElapsedTimeMs);
                }
            }
        }
    }

    void updateLightsLocked() {
        if (this.mNotificationLight == null) {
            return;
        }
        NotificationRecord ledNotification = null;
        while (ledNotification == null && !this.mLights.isEmpty()) {
            ArrayList<String> arrayList = this.mLights;
            String owner = arrayList.get(arrayList.size() - 1);
            NotificationRecord ledNotification2 = this.mNotificationsByKey.get(owner);
            ledNotification = ledNotification2;
            if (ledNotification == null) {
                Slog.wtfStack(TAG, "LED Notification does not exist: " + owner);
                this.mLights.remove(owner);
            }
        }
        if (mTranLedLightExt.isLedWork()) {
            mTranLedLightExt.upNotificationList(this.mLights);
        } else if (ledNotification == null || isInCall() || this.mScreenOn) {
            this.mNotificationLight.turnOff();
        } else {
            NotificationRecord.Light light = ledNotification.getLight();
            if (light != null && this.mNotificationPulseEnabled) {
                this.mNotificationLight.setFlashing(light.color, 1, light.onMs, light.offMs);
            }
        }
    }

    List<NotificationRecord> findCurrentAndSnoozedGroupNotificationsLocked(String pkg, String groupKey, int userId) {
        List<NotificationRecord> records = this.mSnoozeHelper.getNotifications(pkg, groupKey, Integer.valueOf(userId));
        records.addAll(findGroupNotificationsLocked(pkg, groupKey, userId));
        return records;
    }

    List<NotificationRecord> findGroupNotificationsLocked(String pkg, String groupKey, int userId) {
        List<NotificationRecord> records = new ArrayList<>();
        records.addAll(findGroupNotificationByListLocked(this.mNotificationList, pkg, groupKey, userId));
        records.addAll(findGroupNotificationByListLocked(this.mEnqueuedNotifications, pkg, groupKey, userId));
        return records;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public NotificationRecord findInCurrentAndSnoozedNotificationByKeyLocked(String key) {
        NotificationRecord r = findNotificationByKeyLocked(key);
        if (r == null) {
            return this.mSnoozeHelper.getNotification(key);
        }
        return r;
    }

    private List<NotificationRecord> findGroupNotificationByListLocked(ArrayList<NotificationRecord> list, String pkg, String groupKey, int userId) {
        List<NotificationRecord> records = new ArrayList<>();
        int len = list.size();
        for (int i = 0; i < len; i++) {
            NotificationRecord r = list.get(i);
            if (notificationMatchesUserId(r, userId) && r.getGroupKey().equals(groupKey) && r.getSbn().getPackageName().equals(pkg)) {
                records.add(r);
            }
        }
        return records;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public NotificationRecord findNotificationByKeyLocked(String key) {
        NotificationRecord r = findNotificationByListLocked(this.mNotificationList, key);
        if (r != null) {
            return r;
        }
        NotificationRecord r2 = findNotificationByListLocked(this.mEnqueuedNotifications, key);
        if (r2 != null) {
            return r2;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public NotificationRecord findNotificationLocked(String pkg, String tag, int id, int userId) {
        NotificationRecord r = findNotificationByListLocked(this.mNotificationList, pkg, tag, id, userId);
        if (r != null) {
            return r;
        }
        NotificationRecord r2 = findNotificationByListLocked(this.mEnqueuedNotifications, pkg, tag, id, userId);
        if (r2 != null) {
            return r2;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public NotificationRecord findNotificationByListLocked(ArrayList<NotificationRecord> list, String pkg, String tag, int id, int userId) {
        int len = list.size();
        for (int i = 0; i < len; i++) {
            NotificationRecord r = list.get(i);
            if (notificationMatchesUserId(r, userId) && r.getSbn().getId() == id && TextUtils.equals(r.getSbn().getTag(), tag) && r.getSbn().getPackageName().equals(pkg)) {
                return r;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<NotificationRecord> findNotificationsByListLocked(ArrayList<NotificationRecord> list, String pkg, String tag, int id, int userId) {
        List<NotificationRecord> matching = new ArrayList<>();
        int len = list.size();
        for (int i = 0; i < len; i++) {
            NotificationRecord r = list.get(i);
            if (notificationMatchesUserId(r, userId) && r.getSbn().getId() == id && TextUtils.equals(r.getSbn().getTag(), tag) && r.getSbn().getPackageName().equals(pkg)) {
                matching.add(r);
            }
        }
        return matching;
    }

    private NotificationRecord findNotificationByListLocked(ArrayList<NotificationRecord> list, String key) {
        int N = list.size();
        for (int i = 0; i < N; i++) {
            if (key.equals(list.get(i).getKey())) {
                return list.get(i);
            }
        }
        return null;
    }

    private List<NotificationRecord> findEnqueuedNotificationsForCriteria(String pkg, String tag, int id, int userId) {
        ArrayList<NotificationRecord> records = new ArrayList<>();
        int n = this.mEnqueuedNotifications.size();
        for (int i = 0; i < n; i++) {
            NotificationRecord r = this.mEnqueuedNotifications.get(i);
            if (notificationMatchesUserId(r, userId) && r.getSbn().getId() == id && TextUtils.equals(r.getSbn().getTag(), tag) && r.getSbn().getPackageName().equals(pkg)) {
                records.add(r);
            }
        }
        return records;
    }

    int indexOfNotificationLocked(String key) {
        int N = this.mNotificationList.size();
        for (int i = 0; i < N; i++) {
            if (key.equals(this.mNotificationList.get(i).getKey())) {
                return i;
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideNotificationsForPackages(String[] pkgs, int[] uidList) {
        synchronized (this.mNotificationLock) {
            Set<Integer> uidSet = (Set) Arrays.stream(uidList).boxed().collect(Collectors.toSet());
            List<String> pkgList = Arrays.asList(pkgs);
            List<NotificationRecord> changedNotifications = new ArrayList<>();
            int numNotifications = this.mNotificationList.size();
            for (int i = 0; i < numNotifications; i++) {
                NotificationRecord rec = this.mNotificationList.get(i);
                if (pkgList.contains(rec.getSbn().getPackageName()) && uidSet.contains(Integer.valueOf(rec.getUid()))) {
                    rec.setHidden(true);
                    changedNotifications.add(rec);
                }
            }
            this.mListeners.notifyHiddenLocked(changedNotifications);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unhideNotificationsForPackages(String[] pkgs, int[] uidList) {
        synchronized (this.mNotificationLock) {
            Set<Integer> uidSet = (Set) Arrays.stream(uidList).boxed().collect(Collectors.toSet());
            List<String> pkgList = Arrays.asList(pkgs);
            List<NotificationRecord> changedNotifications = new ArrayList<>();
            int numNotifications = this.mNotificationList.size();
            for (int i = 0; i < numNotifications; i++) {
                NotificationRecord rec = this.mNotificationList.get(i);
                if (pkgList.contains(rec.getSbn().getPackageName()) && uidSet.contains(Integer.valueOf(rec.getUid()))) {
                    rec.setHidden(false);
                    changedNotifications.add(rec);
                }
            }
            this.mListeners.notifyUnhiddenLocked(changedNotifications);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelNotificationsWhenEnterLockDownMode(int userId) {
        synchronized (this.mNotificationLock) {
            int numNotifications = this.mNotificationList.size();
            for (int i = 0; i < numNotifications; i++) {
                NotificationRecord rec = this.mNotificationList.get(i);
                if (rec.getUser().getIdentifier() == userId) {
                    this.mListeners.notifyRemovedLocked(rec, 3, rec.getStats());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postNotificationsWhenExitLockDownMode(int userId) {
        synchronized (this.mNotificationLock) {
            int numNotifications = this.mNotificationList.size();
            long delay = 0;
            for (int i = 0; i < numNotifications; i++) {
                final NotificationRecord rec = this.mNotificationList.get(i);
                if (rec.getUser().getIdentifier() == userId) {
                    this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$$ExternalSyntheticLambda12
                        @Override // java.lang.Runnable
                        public final void run() {
                            NotificationManagerService.this.m5136x35cae748(rec);
                        }
                    }, delay);
                    delay += 20;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$postNotificationsWhenExitLockDownMode$11$com-android-server-notification-NotificationManagerService  reason: not valid java name */
    public /* synthetic */ void m5136x35cae748(NotificationRecord rec) {
        synchronized (this.mNotificationLock) {
            this.mListeners.notifyPostedLocked(rec, rec);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNotificationPulse() {
        synchronized (this.mNotificationLock) {
            updateLightsLocked();
        }
    }

    protected boolean isCallingUidSystem() {
        int uid = Binder.getCallingUid();
        return uid == 1000;
    }

    protected boolean isCallingAppIdSystem() {
        int uid = Binder.getCallingUid();
        int appid = UserHandle.getAppId(uid);
        return appid == 1000;
    }

    protected boolean isUidSystemOrPhone(int uid) {
        int appid = UserHandle.getAppId(uid);
        return appid == 1000 || appid == 1001 || uid == 0;
    }

    protected boolean isCallerSystemOrPhone() {
        return isUidSystemOrPhone(Binder.getCallingUid());
    }

    private boolean isCallerIsSystemOrSystemUi() {
        return isCallerSystemOrPhone() || getContext().checkCallingPermission("android.permission.STATUS_BAR_SERVICE") == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isCallerIsSystemOrSysemUiOrShell() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 2000 || callingUid == 0) {
            return true;
        }
        return isCallerIsSystemOrSystemUi();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkCallerIsSystemOrShell() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 2000 || callingUid == 0) {
            return;
        }
        checkCallerIsSystem();
    }

    private boolean isTpms() {
        return UserHandle.getAppId(Binder.getCallingUid()) == PackageManager.tpmsUid;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkCallerIsSystem() {
        if (isCallerSystemOrPhone() || isTpms()) {
            return;
        }
        throw new SecurityException("Disallowed call for uid " + Binder.getCallingUid());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkCallerIsSystemOrSystemUi() {
        if (isCallerIsSystemOrSystemUi() || isTpms()) {
            return;
        }
        throw new SecurityException("Disallowed call for uid " + Binder.getCallingUid());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkCallerIsSystemOrSystemUiOrShell() {
        checkCallerIsSystemOrSystemUiOrShell(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkCallerIsSystemOrSystemUiOrShell(String message) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 2000 || callingUid == 0 || isCallerSystemOrPhone()) {
            return;
        }
        getContext().enforceCallingPermission("android.permission.STATUS_BAR_SERVICE", message);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkCallerIsSystemOrSameApp(String pkg) {
        if (isCallerSystemOrPhone()) {
            return;
        }
        checkCallerIsSameApp(pkg);
    }

    private boolean isCallerAndroid(String callingPkg, int uid) {
        return isUidSystemOrPhone(uid) && callingPkg != null && PackageManagerService.PLATFORM_PACKAGE_NAME.equals(callingPkg);
    }

    private void checkRestrictedCategories(Notification notification) {
        try {
            if (!this.mPackageManager.hasSystemFeature("android.hardware.type.automotive", 0)) {
                return;
            }
        } catch (RemoteException e) {
            if (DBG) {
                Slog.e(TAG, "Unable to confirm if it's safe to skip category restrictions check thus the check will be done anyway");
            }
        }
        if ("car_emergency".equals(notification.category) || "car_warning".equals(notification.category) || "car_information".equals(notification.category)) {
            getContext().enforceCallingPermission("android.permission.SEND_CATEGORY_CAR_NOTIFICATIONS", String.format("Notification category %s restricted", notification.category));
        }
    }

    boolean isCallerInstantApp(int callingUid, int userId) {
        if (isUidSystemOrPhone(callingUid)) {
            return false;
        }
        if (userId == -1) {
            userId = 0;
        }
        try {
            String[] pkgs = this.mPackageManager.getPackagesForUid(callingUid);
            if (pkgs == null) {
                throw new SecurityException("Unknown uid " + callingUid);
            }
            String pkg = pkgs[0];
            this.mAppOps.checkPackage(callingUid, pkg);
            ApplicationInfo ai = this.mPackageManager.getApplicationInfo(pkg, 0L, userId);
            if (ai == null) {
                throw new SecurityException("Unknown package " + pkg);
            }
            return ai.isInstantApp();
        } catch (RemoteException re) {
            throw new SecurityException("Unknown uid " + callingUid, re);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkCallerIsSameApp(String pkg) {
        checkCallerIsSameApp(pkg, Binder.getCallingUid(), UserHandle.getCallingUserId());
    }

    private void checkCallerIsSameApp(String pkg, int uid, int userId) {
        if ((uid != 0 || !ROOT_PKG.equals(pkg)) && !this.mPackageManagerInternal.isSameApp(pkg, uid, userId)) {
            throw new SecurityException("Package " + pkg + " is not owned by uid " + uid);
        }
    }

    private boolean isCallerSameApp(String pkg, int uid, int userId) {
        try {
            checkCallerIsSameApp(pkg, uid, userId);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String callStateToString(int state) {
        switch (state) {
            case 0:
                return "CALL_STATE_IDLE";
            case 1:
                return "CALL_STATE_RINGING";
            case 2:
                return "CALL_STATE_OFFHOOK";
            default:
                return "CALL_STATE_UNKNOWN_" + state;
        }
    }

    NotificationRankingUpdate makeRankingUpdateLocked(ManagedServices.ManagedServiceInfo info) {
        int i;
        int N = this.mNotificationList.size();
        ArrayList<NotificationListenerService.Ranking> rankings = new ArrayList<>();
        for (int i2 = 0; i2 < N; i2++) {
            NotificationRecord record = this.mNotificationList.get(i2);
            if (!isInLockDownMode(record.getUser().getIdentifier()) && isVisibleToListener(record.getSbn(), record.getNotificationType(), info)) {
                String key = record.getSbn().getKey();
                NotificationListenerService.Ranking ranking = new NotificationListenerService.Ranking();
                int size = rankings.size();
                boolean z = !record.isIntercepted();
                int packageVisibilityOverride = record.getPackageVisibilityOverride();
                int suppressedVisualEffects = record.getSuppressedVisualEffects();
                int importance = record.getImportance();
                CharSequence importanceExplanation = record.getImportanceExplanation();
                String overrideGroupKey = record.getSbn().getOverrideGroupKey();
                NotificationChannel channel = record.getChannel();
                ArrayList<String> peopleOverride = record.getPeopleOverride();
                ArrayList<SnoozeCriterion> snoozeCriteria = record.getSnoozeCriteria();
                boolean canShowBadge = record.canShowBadge();
                int userSentiment = record.getUserSentiment();
                boolean isHidden = record.isHidden();
                long lastAudiblyAlertedMs = record.getLastAudiblyAlertedMs();
                boolean z2 = (record.getSound() == null && record.getVibration() == null) ? false : true;
                ArrayList<Notification.Action> systemGeneratedSmartActions = record.getSystemGeneratedSmartActions();
                ArrayList<CharSequence> smartReplies = record.getSmartReplies();
                boolean canBubble = record.canBubble();
                boolean isTextChanged = record.isTextChanged();
                boolean isConversation = record.isConversation();
                ShortcutInfo shortcutInfo = record.getShortcutInfo();
                if (record.getRankingScore() == 0.0f) {
                    i = 0;
                } else {
                    i = record.getRankingScore() > 0.0f ? 1 : -1;
                }
                ranking.populate(key, size, z, packageVisibilityOverride, suppressedVisualEffects, importance, importanceExplanation, overrideGroupKey, channel, peopleOverride, snoozeCriteria, canShowBadge, userSentiment, isHidden, lastAudiblyAlertedMs, z2, systemGeneratedSmartActions, smartReplies, canBubble, isTextChanged, isConversation, shortcutInfo, i, record.getNotification().isBubbleNotification());
                rankings.add(ranking);
            }
        }
        return new NotificationRankingUpdate((NotificationListenerService.Ranking[]) rankings.toArray(new NotificationListenerService.Ranking[0]));
    }

    boolean isInLockDownMode(int userId) {
        return this.mStrongAuthTracker.isInLockDownMode(userId);
    }

    boolean hasCompanionDevice(ManagedServices.ManagedServiceInfo info) {
        if (this.mCompanionManager == null) {
            this.mCompanionManager = getCompanionManager();
        }
        if (this.mCompanionManager == null) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                try {
                    List<?> associations = this.mCompanionManager.getAssociations(info.component.getPackageName(), info.userid);
                    if (!ArrayUtils.isEmpty(associations)) {
                        return true;
                    }
                } catch (Exception e) {
                    Slog.e(TAG, "Cannot verify listener " + info, e);
                }
            } catch (RemoteException re) {
                Slog.e(TAG, "Cannot reach companion device service", re);
            } catch (SecurityException e2) {
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    protected ICompanionDeviceManager getCompanionManager() {
        return ICompanionDeviceManager.Stub.asInterface(ServiceManager.getService("companiondevice"));
    }

    boolean isVisibleToListener(StatusBarNotification sbn, int notificationType, ManagedServices.ManagedServiceInfo listener) {
        Boolean resLice = ITranNotificationManagerService.Instance().onIsVisibleToListener(sbn, listener);
        if (resLice != null) {
            return resLice.booleanValue();
        }
        if (listener.enabledAndUserMatches(sbn.getUserId()) && isInteractionVisibleToListener(listener, sbn.getUserId())) {
            NotificationListenerFilter nls = this.mListeners.getNotificationListenerFilter(listener.mKey);
            if (nls != null) {
                return nls.isTypeAllowed(notificationType) && nls.isPackageAllowed(new VersionedPackage(sbn.getPackageName(), sbn.getUid()));
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isInteractionVisibleToListener(ManagedServices.ManagedServiceInfo info, int userId) {
        boolean isAssistantService = this.mAssistants.isServiceTokenValidLocked(info.service);
        return !isAssistantService || info.isSameUser(userId);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [10176=4] */
    private boolean isPackageSuspendedForUser(String pkg, int uid) {
        long identity = Binder.clearCallingIdentity();
        int userId = UserHandle.getUserId(uid);
        try {
            try {
                boolean isPackageSuspendedForUser = this.mPackageManager.isPackageSuspendedForUser(pkg, userId);
                Binder.restoreCallingIdentity(identity);
                return isPackageSuspendedForUser;
            } catch (RemoteException e) {
                throw new SecurityException("Could not talk to package manager service");
            } catch (IllegalArgumentException e2) {
                Binder.restoreCallingIdentity(identity);
                return false;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canUseManagedServices(String pkg, Integer userId, String requiredPermission) {
        if (requiredPermission == null) {
            return true;
        }
        try {
            if (this.mPackageManager.checkPermission(requiredPermission, pkg, userId.intValue()) == 0) {
                return true;
            }
            return false;
        } catch (RemoteException e) {
            Slog.e(TAG, "can't talk to pm", e);
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class TrimCache {
        StatusBarNotification heavy;
        StatusBarNotification sbnClone;
        StatusBarNotification sbnCloneLight;

        TrimCache(StatusBarNotification sbn) {
            this.heavy = sbn;
        }

        StatusBarNotification ForListener(ManagedServices.ManagedServiceInfo info) {
            if (NotificationManagerService.this.mListeners.getOnNotificationPostedTrim(info) == 1) {
                if (this.sbnCloneLight == null) {
                    this.sbnCloneLight = this.heavy.cloneLight();
                }
                return this.sbnCloneLight;
            }
            if (this.sbnClone == null) {
                this.sbnClone = this.heavy.clone();
            }
            return this.sbnClone;
        }
    }

    private boolean isInCall() {
        int audioMode;
        return this.mInCallStateOffHook || (audioMode = this.mAudioManager.getMode()) == 2 || audioMode == 3;
    }

    /* loaded from: classes2.dex */
    public class NotificationAssistants extends ManagedServices {
        private static final String ATT_TYPES = "types";
        private static final String TAG_ALLOWED_ADJUSTMENT_TYPES = "s_allowed_adjustments";
        private static final String TAG_ALLOWED_ADJUSTMENT_TYPES_OLD = "q_allowed_adjustments";
        static final String TAG_ENABLED_NOTIFICATION_ASSISTANTS = "enabled_assistants";
        private Set<String> mAllowedAdjustments;
        protected ComponentName mDefaultFromConfig;
        private final Object mLock;

        @Override // com.android.server.notification.ManagedServices
        protected void loadDefaultsFromConfig() {
            loadDefaultsFromConfig(true);
        }

        protected void loadDefaultsFromConfig(boolean addToDefault) {
            ArraySet<String> assistants = new ArraySet<>();
            assistants.addAll(Arrays.asList(this.mContext.getResources().getString(17039919).split(":")));
            for (int i = 0; i < assistants.size(); i++) {
                ComponentName assistantCn = ComponentName.unflattenFromString(assistants.valueAt(i));
                String packageName = assistants.valueAt(i);
                if (assistantCn != null) {
                    packageName = assistantCn.getPackageName();
                }
                if (!TextUtils.isEmpty(packageName)) {
                    ArraySet<ComponentName> approved = queryPackageForServices(packageName, 786432, 0);
                    if (approved.contains(assistantCn)) {
                        if (addToDefault) {
                            addDefaultComponentOrPackage(assistantCn.flattenToString());
                        } else {
                            this.mDefaultFromConfig = assistantCn;
                        }
                    }
                }
            }
        }

        ComponentName getDefaultFromConfig() {
            if (this.mDefaultFromConfig == null) {
                loadDefaultsFromConfig(false);
            }
            return this.mDefaultFromConfig;
        }

        @Override // com.android.server.notification.ManagedServices
        protected void upgradeUserSet() {
            for (Integer num : this.mApproved.keySet()) {
                int userId = num.intValue();
                ArraySet<String> userSetServices = this.mUserSetServices.get(Integer.valueOf(userId));
                this.mIsUserChanged.put(Integer.valueOf(userId), Boolean.valueOf(userSetServices != null && userSetServices.size() > 0));
            }
        }

        @Override // com.android.server.notification.ManagedServices
        protected void addApprovedList(String approved, int userId, boolean isPrimary, String userSet) {
            if (!TextUtils.isEmpty(approved)) {
                String[] approvedArray = approved.split(":");
                if (approvedArray.length > 1) {
                    Slog.d(this.TAG, "More than one approved assistants");
                    approved = approvedArray[0];
                }
            }
            super.addApprovedList(approved, userId, isPrimary, userSet);
        }

        public NotificationAssistants(Context context, Object lock, ManagedServices.UserProfiles up, IPackageManager pm) {
            super(context, lock, up, pm);
            this.mLock = new Object();
            this.mAllowedAdjustments = new ArraySet();
            this.mDefaultFromConfig = null;
            for (int i = 0; i < NotificationManagerService.DEFAULT_ALLOWED_ADJUSTMENTS.length; i++) {
                this.mAllowedAdjustments.add(NotificationManagerService.DEFAULT_ALLOWED_ADJUSTMENTS[i]);
            }
        }

        @Override // com.android.server.notification.ManagedServices
        protected ManagedServices.Config getConfig() {
            ManagedServices.Config c = new ManagedServices.Config();
            c.caption = "notification assistant";
            c.serviceInterface = "android.service.notification.NotificationAssistantService";
            c.xmlTag = TAG_ENABLED_NOTIFICATION_ASSISTANTS;
            c.secureSettingName = "enabled_notification_assistant";
            c.bindPermission = "android.permission.BIND_NOTIFICATION_ASSISTANT_SERVICE";
            c.settingsAction = "android.settings.MANAGE_DEFAULT_APPS_SETTINGS";
            c.clientLabel = 17040925;
            return c;
        }

        @Override // com.android.server.notification.ManagedServices
        protected IInterface asInterface(IBinder binder) {
            return INotificationListener.Stub.asInterface(binder);
        }

        @Override // com.android.server.notification.ManagedServices
        protected boolean checkType(IInterface service) {
            return service instanceof INotificationListener;
        }

        @Override // com.android.server.notification.ManagedServices
        protected void onServiceAdded(ManagedServices.ManagedServiceInfo info) {
            NotificationManagerService.this.mListeners.registerGuestService(info);
        }

        @Override // com.android.server.notification.ManagedServices
        protected void ensureFilters(ServiceInfo si, int userId) {
        }

        @Override // com.android.server.notification.ManagedServices
        protected void onServiceRemovedLocked(ManagedServices.ManagedServiceInfo removed) {
            NotificationManagerService.this.mListeners.unregisterService(removed.service, removed.userid);
        }

        @Override // com.android.server.notification.ManagedServices
        public void onUserUnlocked(int user) {
            if (this.DEBUG) {
                Slog.d(this.TAG, "onUserUnlocked u=" + user);
            }
            rebindServices(true, user);
        }

        @Override // com.android.server.notification.ManagedServices
        protected String getRequiredPermission() {
            return "android.permission.REQUEST_NOTIFICATION_ASSISTANT_SERVICE";
        }

        @Override // com.android.server.notification.ManagedServices
        protected void writeExtraXmlTags(TypedXmlSerializer out) throws IOException {
            synchronized (this.mLock) {
                out.startTag((String) null, TAG_ALLOWED_ADJUSTMENT_TYPES);
                out.attribute((String) null, ATT_TYPES, TextUtils.join(",", this.mAllowedAdjustments));
                out.endTag((String) null, TAG_ALLOWED_ADJUSTMENT_TYPES);
            }
        }

        @Override // com.android.server.notification.ManagedServices
        protected void readExtraTag(String tag, TypedXmlPullParser parser) throws IOException {
            if (TAG_ALLOWED_ADJUSTMENT_TYPES_OLD.equals(tag) || TAG_ALLOWED_ADJUSTMENT_TYPES.equals(tag)) {
                String types = XmlUtils.readStringAttribute(parser, ATT_TYPES);
                synchronized (this.mLock) {
                    this.mAllowedAdjustments.clear();
                    if (!TextUtils.isEmpty(types)) {
                        this.mAllowedAdjustments.addAll(Arrays.asList(types.split(",")));
                    }
                    if (TAG_ALLOWED_ADJUSTMENT_TYPES_OLD.equals(tag)) {
                        if (this.DEBUG) {
                            Slog.d(this.TAG, "Migrate allowed adjustments.");
                        }
                        this.mAllowedAdjustments.addAll(Arrays.asList(NotificationManagerService.DEFAULT_ALLOWED_ADJUSTMENTS));
                    }
                }
            }
        }

        protected void allowAdjustmentType(String type) {
            synchronized (this.mLock) {
                this.mAllowedAdjustments.add(type);
            }
            for (final ManagedServices.ManagedServiceInfo info : getServices()) {
                NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationAssistants$$ExternalSyntheticLambda12
                    @Override // java.lang.Runnable
                    public final void run() {
                        NotificationManagerService.NotificationAssistants.this.m5150x7628438a(info);
                    }
                });
            }
        }

        protected void disallowAdjustmentType(String type) {
            synchronized (this.mLock) {
                this.mAllowedAdjustments.remove(type);
            }
            for (final ManagedServices.ManagedServiceInfo info : getServices()) {
                NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationAssistants$$ExternalSyntheticLambda9
                    @Override // java.lang.Runnable
                    public final void run() {
                        NotificationManagerService.NotificationAssistants.this.m5151x78b7fdd7(info);
                    }
                });
            }
        }

        protected List<String> getAllowedAssistantAdjustments() {
            List<String> types;
            synchronized (this.mLock) {
                types = new ArrayList<>();
                types.addAll(this.mAllowedAdjustments);
            }
            return types;
        }

        protected boolean isAdjustmentAllowed(String type) {
            boolean contains;
            synchronized (this.mLock) {
                contains = this.mAllowedAdjustments.contains(type);
            }
            return contains;
        }

        protected void onNotificationsSeenLocked(ArrayList<NotificationRecord> records) {
            for (final ManagedServices.ManagedServiceInfo info : getServices()) {
                final ArrayList<String> keys = new ArrayList<>(records.size());
                Iterator<NotificationRecord> it = records.iterator();
                while (it.hasNext()) {
                    NotificationRecord r = it.next();
                    boolean sbnVisible = NotificationManagerService.this.isVisibleToListener(r.getSbn(), r.getNotificationType(), info) && info.isSameUser(r.getUserId());
                    if (sbnVisible) {
                        keys.add(r.getKey());
                    }
                }
                if (!keys.isEmpty()) {
                    NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationAssistants$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            NotificationManagerService.NotificationAssistants.this.m5159xc2a12b6a(info, keys);
                        }
                    });
                }
            }
        }

        protected void onPanelRevealed(final int items) {
            for (final ManagedServices.ManagedServiceInfo info : getServices()) {
                NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationAssistants$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        NotificationManagerService.NotificationAssistants.this.m5161x67c0bd8e(info, items);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onPanelRevealed$3$com-android-server-notification-NotificationManagerService$NotificationAssistants  reason: not valid java name */
        public /* synthetic */ void m5161x67c0bd8e(ManagedServices.ManagedServiceInfo info, int items) {
            INotificationListener assistant = info.service;
            try {
                assistant.onPanelRevealed(items);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify assistant (panel revealed): " + info, ex);
            }
        }

        protected void onPanelHidden() {
            for (final ManagedServices.ManagedServiceInfo info : getServices()) {
                NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationAssistants$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        NotificationManagerService.NotificationAssistants.this.m5160x3dadaecd(info);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onPanelHidden$4$com-android-server-notification-NotificationManagerService$NotificationAssistants  reason: not valid java name */
        public /* synthetic */ void m5160x3dadaecd(ManagedServices.ManagedServiceInfo info) {
            INotificationListener assistant = info.service;
            try {
                assistant.onPanelHidden();
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify assistant (panel hidden): " + info, ex);
            }
        }

        boolean hasUserSet(int userId) {
            Boolean userSet = this.mIsUserChanged.get(Integer.valueOf(userId));
            return userSet != null && userSet.booleanValue();
        }

        void setUserSet(int userId, boolean set) {
            this.mIsUserChanged.put(Integer.valueOf(userId), Boolean.valueOf(set));
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: notifyCapabilitiesChanged */
        public void m5151x78b7fdd7(ManagedServices.ManagedServiceInfo info) {
            INotificationListener assistant = info.service;
            try {
                assistant.onAllowedAdjustmentsChanged();
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify assistant (capabilities): " + info, ex);
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: notifySeen */
        public void m5159xc2a12b6a(ManagedServices.ManagedServiceInfo info, ArrayList<String> keys) {
            INotificationListener assistant = info.service;
            try {
                assistant.onNotificationsSeen(keys);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify assistant (seen): " + info, ex);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onNotificationEnqueuedLocked(NotificationRecord r) {
            boolean debug = isVerboseLogEnabled();
            if (debug) {
                Slog.v(this.TAG, "onNotificationEnqueuedLocked() called with: r = [" + r + "]");
            }
            StatusBarNotification sbn = r.getSbn();
            for (ManagedServices.ManagedServiceInfo info : getServices()) {
                boolean sbnVisible = NotificationManagerService.this.isVisibleToListener(sbn, r.getNotificationType(), info) && info.isSameUser(r.getUserId());
                if (sbnVisible) {
                    TrimCache trimCache = new TrimCache(sbn);
                    INotificationListener assistant = info.service;
                    StatusBarNotification sbnToPost = trimCache.ForListener(info);
                    StatusBarNotificationHolder sbnHolder = new StatusBarNotificationHolder(sbnToPost);
                    if (debug) {
                        try {
                            Slog.v(this.TAG, "calling onNotificationEnqueuedWithChannel " + sbnHolder);
                        } catch (RemoteException ex) {
                            Slog.e(this.TAG, "unable to notify assistant (enqueued): " + assistant, ex);
                        }
                    }
                    NotificationRankingUpdate update = NotificationManagerService.this.makeRankingUpdateLocked(info);
                    assistant.onNotificationEnqueuedWithChannel(sbnHolder, r.getChannel(), update);
                }
            }
        }

        void notifyAssistantVisibilityChangedLocked(NotificationRecord r, final boolean isVisible) {
            final String key = r.getSbn().getKey();
            if (NotificationManagerService.DBG) {
                Slog.d(this.TAG, "notifyAssistantVisibilityChangedLocked: " + key);
            }
            notifyAssistantLocked(r.getSbn(), r.getNotificationType(), true, new BiConsumer() { // from class: com.android.server.notification.NotificationManagerService$NotificationAssistants$$ExternalSyntheticLambda8
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    NotificationManagerService.NotificationAssistants.this.m5158xa998b50(key, isVisible, (INotificationListener) obj, (NotificationManagerService.StatusBarNotificationHolder) obj2);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyAssistantVisibilityChangedLocked$5$com-android-server-notification-NotificationManagerService$NotificationAssistants  reason: not valid java name */
        public /* synthetic */ void m5158xa998b50(String key, boolean isVisible, INotificationListener assistant, StatusBarNotificationHolder sbnHolder) {
            try {
                assistant.onNotificationVisibilityChanged(key, isVisible);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify assistant (visible): " + assistant, ex);
            }
        }

        void notifyAssistantExpansionChangedLocked(StatusBarNotification sbn, int notificationType, final boolean isUserAction, final boolean isExpanded) {
            final String key = sbn.getKey();
            notifyAssistantLocked(sbn, notificationType, true, new BiConsumer() { // from class: com.android.server.notification.NotificationManagerService$NotificationAssistants$$ExternalSyntheticLambda11
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    NotificationManagerService.NotificationAssistants.this.m5153x8e9ccbb4(key, isUserAction, isExpanded, (INotificationListener) obj, (NotificationManagerService.StatusBarNotificationHolder) obj2);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyAssistantExpansionChangedLocked$6$com-android-server-notification-NotificationManagerService$NotificationAssistants  reason: not valid java name */
        public /* synthetic */ void m5153x8e9ccbb4(String key, boolean isUserAction, boolean isExpanded, INotificationListener assistant, StatusBarNotificationHolder sbnHolder) {
            try {
                assistant.onNotificationExpansionChanged(key, isUserAction, isExpanded);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify assistant (expanded): " + assistant, ex);
            }
        }

        void notifyAssistantNotificationDirectReplyLocked(NotificationRecord r) {
            final String key = r.getKey();
            notifyAssistantLocked(r.getSbn(), r.getNotificationType(), true, new BiConsumer() { // from class: com.android.server.notification.NotificationManagerService$NotificationAssistants$$ExternalSyntheticLambda6
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    NotificationManagerService.NotificationAssistants.this.m5155x3ce73f26(key, (INotificationListener) obj, (NotificationManagerService.StatusBarNotificationHolder) obj2);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyAssistantNotificationDirectReplyLocked$7$com-android-server-notification-NotificationManagerService$NotificationAssistants  reason: not valid java name */
        public /* synthetic */ void m5155x3ce73f26(String key, INotificationListener assistant, StatusBarNotificationHolder sbnHolder) {
            try {
                assistant.onNotificationDirectReply(key);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify assistant (expanded): " + assistant, ex);
            }
        }

        void notifyAssistantSuggestedReplySent(StatusBarNotification sbn, int notificationType, final CharSequence reply, final boolean generatedByAssistant) {
            final String key = sbn.getKey();
            notifyAssistantLocked(sbn, notificationType, true, new BiConsumer() { // from class: com.android.server.notification.NotificationManagerService$NotificationAssistants$$ExternalSyntheticLambda3
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    NotificationManagerService.NotificationAssistants.this.m5157x53a5c96(key, reply, generatedByAssistant, (INotificationListener) obj, (NotificationManagerService.StatusBarNotificationHolder) obj2);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyAssistantSuggestedReplySent$8$com-android-server-notification-NotificationManagerService$NotificationAssistants  reason: not valid java name */
        public /* synthetic */ void m5157x53a5c96(String key, CharSequence reply, boolean generatedByAssistant, INotificationListener assistant, StatusBarNotificationHolder sbnHolder) {
            int i;
            if (generatedByAssistant) {
                i = 1;
            } else {
                i = 0;
            }
            try {
                assistant.onSuggestedReplySent(key, reply, i);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify assistant (snoozed): " + assistant, ex);
            }
        }

        void notifyAssistantActionClicked(NotificationRecord r, final Notification.Action action, final boolean generatedByAssistant) {
            final String key = r.getSbn().getKey();
            notifyAssistantLocked(r.getSbn(), r.getNotificationType(), true, new BiConsumer() { // from class: com.android.server.notification.NotificationManagerService$NotificationAssistants$$ExternalSyntheticLambda10
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    NotificationManagerService.NotificationAssistants.this.m5152x80bbb3f9(key, action, generatedByAssistant, (INotificationListener) obj, (NotificationManagerService.StatusBarNotificationHolder) obj2);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyAssistantActionClicked$9$com-android-server-notification-NotificationManagerService$NotificationAssistants  reason: not valid java name */
        public /* synthetic */ void m5152x80bbb3f9(String key, Notification.Action action, boolean generatedByAssistant, INotificationListener assistant, StatusBarNotificationHolder sbnHolder) {
            int i;
            if (generatedByAssistant) {
                i = 1;
            } else {
                i = 0;
            }
            try {
                assistant.onActionClicked(key, action, i);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify assistant (snoozed): " + assistant, ex);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifyAssistantSnoozedLocked(NotificationRecord r, final String snoozeCriterionId) {
            notifyAssistantLocked(r.getSbn(), r.getNotificationType(), true, new BiConsumer() { // from class: com.android.server.notification.NotificationManagerService$NotificationAssistants$$ExternalSyntheticLambda5
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    NotificationManagerService.NotificationAssistants.this.m5156xdb04f45e(snoozeCriterionId, (INotificationListener) obj, (NotificationManagerService.StatusBarNotificationHolder) obj2);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyAssistantSnoozedLocked$10$com-android-server-notification-NotificationManagerService$NotificationAssistants  reason: not valid java name */
        public /* synthetic */ void m5156xdb04f45e(String snoozeCriterionId, INotificationListener assistant, StatusBarNotificationHolder sbnHolder) {
            try {
                assistant.onNotificationSnoozedUntilContext(sbnHolder, snoozeCriterionId);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify assistant (snoozed): " + assistant, ex);
            }
        }

        void notifyAssistantNotificationClicked(NotificationRecord r) {
            final String key = r.getSbn().getKey();
            notifyAssistantLocked(r.getSbn(), r.getNotificationType(), true, new BiConsumer() { // from class: com.android.server.notification.NotificationManagerService$NotificationAssistants$$ExternalSyntheticLambda7
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    NotificationManagerService.NotificationAssistants.this.m5154xd5b88fcb(key, (INotificationListener) obj, (NotificationManagerService.StatusBarNotificationHolder) obj2);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyAssistantNotificationClicked$11$com-android-server-notification-NotificationManagerService$NotificationAssistants  reason: not valid java name */
        public /* synthetic */ void m5154xd5b88fcb(String key, INotificationListener assistant, StatusBarNotificationHolder sbnHolder) {
            try {
                assistant.onNotificationClicked(key);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify assistant (clicked): " + assistant, ex);
            }
        }

        void notifyAssistantFeedbackReceived(NotificationRecord r, Bundle feedback) {
            StatusBarNotification sbn = r.getSbn();
            for (ManagedServices.ManagedServiceInfo info : getServices()) {
                boolean sbnVisible = NotificationManagerService.this.isVisibleToListener(sbn, r.getNotificationType(), info) && info.isSameUser(r.getUserId());
                if (sbnVisible) {
                    INotificationListener assistant = info.service;
                    try {
                        NotificationRankingUpdate update = NotificationManagerService.this.makeRankingUpdateLocked(info);
                        assistant.onNotificationFeedbackReceived(sbn.getKey(), update, feedback);
                    } catch (RemoteException ex) {
                        Slog.e(this.TAG, "unable to notify assistant (feedback): " + assistant, ex);
                    }
                }
            }
        }

        private void notifyAssistantLocked(StatusBarNotification sbn, int notificationType, boolean sameUserOnly, final BiConsumer<INotificationListener, StatusBarNotificationHolder> callback) {
            TrimCache trimCache = new TrimCache(sbn);
            boolean debug = isVerboseLogEnabled();
            if (debug) {
                Slog.v(this.TAG, "notifyAssistantLocked() called with: sbn = [" + sbn + "], sameUserOnly = [" + sameUserOnly + "], callback = [" + callback + "]");
            }
            for (ManagedServices.ManagedServiceInfo info : getServices()) {
                boolean sbnVisible = NotificationManagerService.this.isVisibleToListener(sbn, notificationType, info) && (!sameUserOnly || info.isSameUser(sbn.getUserId()));
                if (debug) {
                    Slog.v(this.TAG, "notifyAssistantLocked info=" + info + " snbVisible=" + sbnVisible);
                }
                if (sbnVisible) {
                    final INotificationListener assistant = info.service;
                    StatusBarNotification sbnToPost = trimCache.ForListener(info);
                    final StatusBarNotificationHolder sbnHolder = new StatusBarNotificationHolder(sbnToPost);
                    NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationAssistants$$ExternalSyntheticLambda4
                        @Override // java.lang.Runnable
                        public final void run() {
                            callback.accept(assistant, sbnHolder);
                        }
                    });
                }
            }
        }

        public boolean isEnabled() {
            return !getServices().isEmpty();
        }

        protected void resetDefaultAssistantsIfNecessary() {
            List<UserInfo> activeUsers = this.mUm.getAliveUsers();
            for (UserInfo userInfo : activeUsers) {
                int userId = userInfo.getUserHandle().getIdentifier();
                if (!hasUserSet(userId)) {
                    if (!NotificationManagerService.this.isNASMigrationDone(userId)) {
                        resetDefaultFromConfig();
                        NotificationManagerService.this.setNASMigrationDone(userId);
                    }
                    Slog.d(this.TAG, "Approving default notification assistant for user " + userId);
                    NotificationManagerService.this.setDefaultAssistantForUser(userId);
                }
            }
        }

        protected void resetDefaultFromConfig() {
            clearDefaults();
            loadDefaultsFromConfig();
        }

        protected void clearDefaults() {
            this.mDefaultComponents.clear();
            this.mDefaultPackages.clear();
        }

        @Override // com.android.server.notification.ManagedServices
        protected void setPackageOrComponentEnabled(String pkgOrComponent, int userId, boolean isPrimary, boolean enabled, boolean userSet) {
            if (enabled) {
                List<ComponentName> allowedComponents = getAllowedComponents(userId);
                if (!allowedComponents.isEmpty()) {
                    ComponentName currentComponent = (ComponentName) CollectionUtils.firstOrNull(allowedComponents);
                    if (currentComponent.flattenToString().equals(pkgOrComponent)) {
                        return;
                    }
                    NotificationManagerService.this.setNotificationAssistantAccessGrantedForUserInternal(currentComponent, userId, false, userSet);
                }
            }
            super.setPackageOrComponentEnabled(pkgOrComponent, userId, isPrimary, enabled, userSet);
        }

        private boolean isVerboseLogEnabled() {
            return Log.isLoggable("notification_assistant", 2);
        }
    }

    /* loaded from: classes2.dex */
    public class NotificationListeners extends ManagedServices {
        static final String ATT_COMPONENT = "component";
        static final String ATT_PKG = "pkg";
        static final String ATT_TYPES = "types";
        static final String ATT_UID = "uid";
        static final String FLAG_SEPARATOR = "\\|";
        static final String TAG_APPROVED = "allowed";
        static final String TAG_DISALLOWED = "disallowed";
        static final String TAG_ENABLED_NOTIFICATION_LISTENERS = "enabled_listeners";
        static final String TAG_REQUESTED_LISTENER = "listener";
        static final String TAG_REQUESTED_LISTENERS = "request_listeners";
        static final String XML_SEPARATOR = ",";
        private final ArraySet<ManagedServices.ManagedServiceInfo> mLightTrimListeners;
        ArrayMap<Pair<ComponentName, Integer>, NotificationListenerFilter> mRequestedNotificationListeners;

        public NotificationListeners(Context context, Object lock, ManagedServices.UserProfiles userProfiles, IPackageManager pm) {
            super(context, lock, userProfiles, pm);
            this.mLightTrimListeners = new ArraySet<>();
            this.mRequestedNotificationListeners = new ArrayMap<>();
        }

        @Override // com.android.server.notification.ManagedServices
        protected void setPackageOrComponentEnabled(String pkgOrComponent, int userId, boolean isPrimary, boolean enabled, boolean userSet) {
            super.setPackageOrComponentEnabled(pkgOrComponent, userId, isPrimary, enabled, userSet);
            this.mContext.sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_LISTENER_ENABLED_CHANGED").addFlags(1073741824), UserHandle.of(userId), null);
        }

        @Override // com.android.server.notification.ManagedServices
        protected void loadDefaultsFromConfig() {
            String defaultListenerAccess = this.mContext.getResources().getString(17039928);
            if (defaultListenerAccess != null) {
                String[] listeners = defaultListenerAccess.split(":");
                for (int i = 0; i < listeners.length; i++) {
                    if (!TextUtils.isEmpty(listeners[i])) {
                        ArraySet<ComponentName> approvedListeners = queryPackageForServices(listeners[i], 786432, 0);
                        for (int k = 0; k < approvedListeners.size(); k++) {
                            ComponentName cn = approvedListeners.valueAt(k);
                            addDefaultComponentOrPackage(cn.flattenToString());
                        }
                    }
                }
            }
        }

        @Override // com.android.server.notification.ManagedServices
        protected int getBindFlags() {
            return 83886337;
        }

        @Override // com.android.server.notification.ManagedServices
        protected ManagedServices.Config getConfig() {
            ManagedServices.Config c = new ManagedServices.Config();
            c.caption = "notification listener";
            c.serviceInterface = "android.service.notification.NotificationListenerService";
            c.xmlTag = TAG_ENABLED_NOTIFICATION_LISTENERS;
            c.secureSettingName = "enabled_notification_listeners";
            c.bindPermission = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE";
            c.settingsAction = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
            c.clientLabel = 17040922;
            return c;
        }

        @Override // com.android.server.notification.ManagedServices
        protected IInterface asInterface(IBinder binder) {
            return INotificationListener.Stub.asInterface(binder);
        }

        @Override // com.android.server.notification.ManagedServices
        protected boolean checkType(IInterface service) {
            return service instanceof INotificationListener;
        }

        @Override // com.android.server.notification.ManagedServices
        public void onServiceAdded(ManagedServices.ManagedServiceInfo info) {
            NotificationRankingUpdate update;
            INotificationListener listener = info.service;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                update = NotificationManagerService.this.makeRankingUpdateLocked(info);
                updateUriPermissionsForActiveNotificationsLocked(info, true);
            }
            try {
                listener.onListenerConnected(update);
            } catch (RemoteException e) {
            }
        }

        @Override // com.android.server.notification.ManagedServices
        protected void onServiceRemovedLocked(ManagedServices.ManagedServiceInfo removed) {
            updateUriPermissionsForActiveNotificationsLocked(removed, false);
            if (NotificationManagerService.this.removeDisabledHints(removed)) {
                NotificationManagerService.this.updateListenerHintsLocked();
                NotificationManagerService.this.updateEffectsSuppressorLocked();
            }
            this.mLightTrimListeners.remove(removed);
        }

        @Override // com.android.server.notification.ManagedServices
        public void onUserRemoved(int user) {
            super.onUserRemoved(user);
            for (int i = this.mRequestedNotificationListeners.size() - 1; i >= 0; i--) {
                if (((Integer) this.mRequestedNotificationListeners.keyAt(i).second).intValue() == user) {
                    this.mRequestedNotificationListeners.removeAt(i);
                }
            }
        }

        @Override // com.android.server.notification.ManagedServices
        public void onPackagesChanged(boolean removingPackage, String[] pkgList, int[] uidList) {
            super.onPackagesChanged(removingPackage, pkgList, uidList);
            if (removingPackage) {
                for (int i = 0; i < pkgList.length; i++) {
                    String pkg = pkgList[i];
                    int userId = UserHandle.getUserId(uidList[i]);
                    for (int j = this.mRequestedNotificationListeners.size() - 1; j >= 0; j--) {
                        Pair<ComponentName, Integer> key = this.mRequestedNotificationListeners.keyAt(j);
                        if (((Integer) key.second).intValue() == userId && ((ComponentName) key.first).getPackageName().equals(pkg)) {
                            this.mRequestedNotificationListeners.removeAt(j);
                        }
                    }
                }
            }
            for (int i2 = 0; i2 < pkgList.length; i2++) {
                String pkg2 = pkgList[i2];
                UserHandle.getUserId(uidList[i2]);
                for (int j2 = this.mRequestedNotificationListeners.size() - 1; j2 >= 0; j2--) {
                    NotificationListenerFilter nlf = this.mRequestedNotificationListeners.valueAt(j2);
                    VersionedPackage ai = new VersionedPackage(pkg2, uidList[i2]);
                    nlf.removePackage(ai);
                }
            }
        }

        @Override // com.android.server.notification.ManagedServices
        protected String getRequiredPermission() {
            return null;
        }

        @Override // com.android.server.notification.ManagedServices
        protected boolean shouldReflectToSettings() {
            return true;
        }

        @Override // com.android.server.notification.ManagedServices
        protected void readExtraTag(String tag, TypedXmlPullParser parser) throws IOException, XmlPullParserException {
            if (TAG_REQUESTED_LISTENERS.equals(tag)) {
                int listenersOuterDepth = parser.getDepth();
                while (XmlUtils.nextElementWithin(parser, listenersOuterDepth)) {
                    if (TAG_REQUESTED_LISTENER.equals(parser.getName())) {
                        int userId = XmlUtils.readIntAttribute(parser, "user");
                        ComponentName cn = ComponentName.unflattenFromString(XmlUtils.readStringAttribute(parser, ATT_COMPONENT));
                        int approved = 15;
                        ArraySet<VersionedPackage> disallowedPkgs = new ArraySet<>();
                        int listenerOuterDepth = parser.getDepth();
                        while (XmlUtils.nextElementWithin(parser, listenerOuterDepth)) {
                            if (TAG_APPROVED.equals(parser.getName())) {
                                approved = XmlUtils.readIntAttribute(parser, ATT_TYPES);
                            } else if (TAG_DISALLOWED.equals(parser.getName())) {
                                String pkg = XmlUtils.readStringAttribute(parser, ATT_PKG);
                                int uid = XmlUtils.readIntAttribute(parser, "uid");
                                if (!TextUtils.isEmpty(pkg)) {
                                    VersionedPackage ai = new VersionedPackage(pkg, uid);
                                    disallowedPkgs.add(ai);
                                }
                            }
                        }
                        NotificationListenerFilter nlf = new NotificationListenerFilter(approved, disallowedPkgs);
                        this.mRequestedNotificationListeners.put(Pair.create(cn, Integer.valueOf(userId)), nlf);
                    }
                }
            }
        }

        @Override // com.android.server.notification.ManagedServices
        protected void writeExtraXmlTags(TypedXmlSerializer out) throws IOException {
            out.startTag((String) null, TAG_REQUESTED_LISTENERS);
            for (Pair<ComponentName, Integer> listener : this.mRequestedNotificationListeners.keySet()) {
                NotificationListenerFilter nlf = this.mRequestedNotificationListeners.get(listener);
                out.startTag((String) null, TAG_REQUESTED_LISTENER);
                XmlUtils.writeStringAttribute(out, ATT_COMPONENT, ((ComponentName) listener.first).flattenToString());
                XmlUtils.writeIntAttribute(out, "user", ((Integer) listener.second).intValue());
                out.startTag((String) null, TAG_APPROVED);
                XmlUtils.writeIntAttribute(out, ATT_TYPES, nlf.getTypes());
                out.endTag((String) null, TAG_APPROVED);
                Iterator it = nlf.getDisallowedPackages().iterator();
                while (it.hasNext()) {
                    VersionedPackage ai = (VersionedPackage) it.next();
                    if (!TextUtils.isEmpty(ai.getPackageName())) {
                        out.startTag((String) null, TAG_DISALLOWED);
                        XmlUtils.writeStringAttribute(out, ATT_PKG, ai.getPackageName());
                        XmlUtils.writeIntAttribute(out, "uid", ai.getVersionCode());
                        out.endTag((String) null, TAG_DISALLOWED);
                    }
                }
                out.endTag((String) null, TAG_REQUESTED_LISTENER);
            }
            out.endTag((String) null, TAG_REQUESTED_LISTENERS);
        }

        protected NotificationListenerFilter getNotificationListenerFilter(Pair<ComponentName, Integer> pair) {
            return this.mRequestedNotificationListeners.get(pair);
        }

        protected void setNotificationListenerFilter(Pair<ComponentName, Integer> pair, NotificationListenerFilter nlf) {
            this.mRequestedNotificationListeners.put(pair, nlf);
        }

        @Override // com.android.server.notification.ManagedServices
        protected void ensureFilters(ServiceInfo si, int userId) {
            int neverBridge;
            String typeList;
            Pair listener = Pair.create(si.getComponentName(), Integer.valueOf(userId));
            NotificationListenerFilter existingNlf = this.mRequestedNotificationListeners.get(listener);
            if (si.metaData != null) {
                if (existingNlf == null && si.metaData.containsKey("android.service.notification.default_filter_types") && (typeList = si.metaData.get("android.service.notification.default_filter_types").toString()) != null) {
                    int types = getTypesFromStringList(typeList);
                    this.mRequestedNotificationListeners.put(listener, new NotificationListenerFilter(types, new ArraySet()));
                }
                if (si.metaData.containsKey("android.service.notification.disabled_filter_types") && (neverBridge = getTypesFromStringList(si.metaData.get("android.service.notification.disabled_filter_types").toString())) != 0) {
                    NotificationListenerFilter nlf = this.mRequestedNotificationListeners.getOrDefault(listener, new NotificationListenerFilter());
                    nlf.setTypes(nlf.getTypes() & (~neverBridge));
                    this.mRequestedNotificationListeners.put(listener, nlf);
                }
            }
        }

        private int getTypesFromStringList(String typeList) {
            int types = 0;
            if (typeList != null) {
                String[] typeStrings = typeList.split(FLAG_SEPARATOR);
                for (String typeString : typeStrings) {
                    if (!TextUtils.isEmpty(typeString)) {
                        if (typeString.equalsIgnoreCase("ONGOING")) {
                            types |= 8;
                        } else if (typeString.equalsIgnoreCase("CONVERSATIONS")) {
                            types |= 1;
                        } else if (typeString.equalsIgnoreCase("SILENT")) {
                            types |= 4;
                        } else if (typeString.equalsIgnoreCase("ALERTING")) {
                            types |= 2;
                        } else {
                            try {
                                types |= Integer.parseInt(typeString);
                            } catch (NumberFormatException e) {
                            }
                        }
                    }
                }
            }
            return types;
        }

        public void setOnNotificationPostedTrimLocked(ManagedServices.ManagedServiceInfo info, int trim) {
            if (trim == 1) {
                this.mLightTrimListeners.add(info);
            } else {
                this.mLightTrimListeners.remove(info);
            }
        }

        public int getOnNotificationPostedTrim(ManagedServices.ManagedServiceInfo info) {
            return this.mLightTrimListeners.contains(info) ? 1 : 0;
        }

        public void onStatusBarIconsBehaviorChanged(final boolean hideSilentStatusIcons) {
            for (final ManagedServices.ManagedServiceInfo info : getServices()) {
                NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationListeners$$ExternalSyntheticLambda10
                    @Override // java.lang.Runnable
                    public final void run() {
                        NotificationManagerService.NotificationListeners.this.m5171xc65a4d50(info, hideSilentStatusIcons);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onStatusBarIconsBehaviorChanged$0$com-android-server-notification-NotificationManagerService$NotificationListeners  reason: not valid java name */
        public /* synthetic */ void m5171xc65a4d50(ManagedServices.ManagedServiceInfo info, boolean hideSilentStatusIcons) {
            INotificationListener listener = info.service;
            try {
                listener.onStatusBarIconsBehaviorChanged(hideSilentStatusIcons);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify listener (hideSilentStatusIcons): " + info, ex);
            }
        }

        public void notifyPostedLocked(NotificationRecord r, NotificationRecord old) {
            notifyPostedLocked(r, old, true);
        }

        void notifyPostedLocked(NotificationRecord r, NotificationRecord old, boolean notifyAllListeners) {
            if (NotificationManagerService.this.isInLockDownMode(r.getUser().getIdentifier())) {
                return;
            }
            try {
                StatusBarNotification sbn = r.getSbn();
                StatusBarNotification oldSbn = old != null ? old.getSbn() : null;
                TrimCache trimCache = new TrimCache(sbn);
                for (final ManagedServices.ManagedServiceInfo info : getServices()) {
                    boolean sbnVisible = NotificationManagerService.this.isVisibleToListener(sbn, r.getNotificationType(), info);
                    int targetUserId = 0;
                    boolean oldSbnVisible = oldSbn != null && NotificationManagerService.this.isVisibleToListener(oldSbn, old.getNotificationType(), info);
                    if (oldSbnVisible || sbnVisible) {
                        if (!r.isHidden() || info.targetSdkVersion >= 28) {
                            if (notifyAllListeners || info.targetSdkVersion < 28) {
                                final NotificationRankingUpdate update = NotificationManagerService.this.makeRankingUpdateLocked(info);
                                if (oldSbnVisible && !sbnVisible) {
                                    final StatusBarNotification oldSbnLightClone = oldSbn.cloneLight();
                                    NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationListeners$$ExternalSyntheticLambda6
                                        @Override // java.lang.Runnable
                                        public final void run() {
                                            NotificationManagerService.NotificationListeners.this.m5166x9283b48d(info, oldSbnLightClone, update);
                                        }
                                    });
                                } else {
                                    if (info.userid != -1) {
                                        targetUserId = info.userid;
                                    }
                                    NotificationManagerService.this.updateUriPermissions(r, old, info.component.getPackageName(), targetUserId);
                                    final StatusBarNotification sbnToPost = trimCache.ForListener(info);
                                    NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationListeners$$ExternalSyntheticLambda7
                                        @Override // java.lang.Runnable
                                        public final void run() {
                                            NotificationManagerService.NotificationListeners.this.m5167xc1351eac(info, sbnToPost, update);
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Slog.e(this.TAG, "Could not notify listeners for " + r.getKey(), e);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyPostedLocked$1$com-android-server-notification-NotificationManagerService$NotificationListeners  reason: not valid java name */
        public /* synthetic */ void m5166x9283b48d(ManagedServices.ManagedServiceInfo info, StatusBarNotification oldSbnLightClone, NotificationRankingUpdate update) {
            m5169xd5da2f56(info, oldSbnLightClone, update, null, 6);
        }

        private void updateUriPermissionsForActiveNotificationsLocked(ManagedServices.ManagedServiceInfo info, boolean grant) {
            try {
                Iterator<NotificationRecord> it = NotificationManagerService.this.mNotificationList.iterator();
                while (it.hasNext()) {
                    NotificationRecord r = it.next();
                    if (!grant || NotificationManagerService.this.isVisibleToListener(r.getSbn(), r.getNotificationType(), info)) {
                        if (!r.isHidden() || info.targetSdkVersion >= 28) {
                            int targetUserId = info.userid == -1 ? 0 : info.userid;
                            if (grant) {
                                NotificationManagerService.this.updateUriPermissions(r, null, info.component.getPackageName(), targetUserId);
                            } else {
                                NotificationManagerService.this.updateUriPermissions(null, r, info.component.getPackageName(), targetUserId, true);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Slog.e(this.TAG, "Could not " + (grant ? "grant" : "revoke") + " Uri permissions to " + info.component, e);
            }
        }

        /* JADX WARN: Code restructure failed: missing block: B:19:0x0054, code lost:
            if (r12.targetSdkVersion < 28) goto L20;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void notifyRemovedLocked(final NotificationRecord r, final int reason, final NotificationStats notificationStats) {
            if (NotificationManagerService.this.isInLockDownMode(r.getUser().getIdentifier())) {
                return;
            }
            StatusBarNotification sbn = r.getSbn();
            final StatusBarNotification sbnLight = sbn.cloneLight();
            Iterator<ManagedServices.ManagedServiceInfo> it = getServices().iterator();
            while (it.hasNext()) {
                final ManagedServices.ManagedServiceInfo info = it.next();
                if (NotificationManagerService.this.isVisibleToListener(sbn, r.getNotificationType(), info) && (!r.isHidden() || reason == 14 || info.targetSdkVersion >= 28)) {
                    final NotificationStats stats = NotificationManagerService.this.mAssistants.isServiceTokenValidLocked(info.service) ? notificationStats : null;
                    final NotificationRankingUpdate update = NotificationManagerService.this.makeRankingUpdateLocked(info);
                    NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationListeners$$ExternalSyntheticLambda3
                        @Override // java.lang.Runnable
                        public final void run() {
                            NotificationManagerService.NotificationListeners.this.m5169xd5da2f56(info, sbnLight, update, stats, reason);
                        }
                    });
                }
            }
            NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationListeners$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    NotificationManagerService.NotificationListeners.this.m5170x48b9975(r);
                }
            });
            NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationListeners$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    ITranNotificationManagerService.Instance().onNotifyRemoved(sbnLight, notificationStats, reason);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyRemovedLocked$4$com-android-server-notification-NotificationManagerService$NotificationListeners  reason: not valid java name */
        public /* synthetic */ void m5170x48b9975(NotificationRecord r) {
            NotificationManagerService.this.updateUriPermissions(null, r, null, 0);
        }

        public void notifyRankingUpdateLocked(List<NotificationRecord> changedHiddenNotifications) {
            boolean isHiddenRankingUpdate = changedHiddenNotifications != null && changedHiddenNotifications.size() > 0;
            for (final ManagedServices.ManagedServiceInfo serviceInfo : getServices()) {
                if (serviceInfo.isEnabledForCurrentProfiles() && NotificationManagerService.this.isInteractionVisibleToListener(serviceInfo, ActivityManager.getCurrentUser())) {
                    boolean notifyThisListener = false;
                    if (isHiddenRankingUpdate && serviceInfo.targetSdkVersion >= 28) {
                        Iterator<NotificationRecord> it = changedHiddenNotifications.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            NotificationRecord rec = it.next();
                            if (NotificationManagerService.this.isVisibleToListener(rec.getSbn(), rec.getNotificationType(), serviceInfo)) {
                                notifyThisListener = true;
                                break;
                            }
                        }
                    }
                    if (notifyThisListener || !isHiddenRankingUpdate) {
                        final NotificationRankingUpdate update = NotificationManagerService.this.makeRankingUpdateLocked(serviceInfo);
                        NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationListeners$$ExternalSyntheticLambda2
                            @Override // java.lang.Runnable
                            public final void run() {
                                NotificationManagerService.NotificationListeners.this.m5168xbd02bdd4(serviceInfo, update);
                            }
                        });
                    }
                }
            }
        }

        public void notifyListenerHintsChangedLocked(final int hints) {
            for (final ManagedServices.ManagedServiceInfo serviceInfo : getServices()) {
                if (serviceInfo.isEnabledForCurrentProfiles() && NotificationManagerService.this.isInteractionVisibleToListener(serviceInfo, ActivityManager.getCurrentUser())) {
                    NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationListeners$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            NotificationManagerService.NotificationListeners.this.m5163x497f5e8a(serviceInfo, hints);
                        }
                    });
                }
            }
        }

        public void notifyHiddenLocked(List<NotificationRecord> changedNotifications) {
            if (changedNotifications == null || changedNotifications.size() == 0) {
                return;
            }
            notifyRankingUpdateLocked(changedNotifications);
            int numChangedNotifications = changedNotifications.size();
            for (int i = 0; i < numChangedNotifications; i++) {
                NotificationRecord rec = changedNotifications.get(i);
                NotificationManagerService.this.mListeners.notifyRemovedLocked(rec, 14, rec.getStats());
            }
        }

        public void notifyUnhiddenLocked(List<NotificationRecord> changedNotifications) {
            if (changedNotifications == null || changedNotifications.size() == 0) {
                return;
            }
            notifyRankingUpdateLocked(changedNotifications);
            int numChangedNotifications = changedNotifications.size();
            for (int i = 0; i < numChangedNotifications; i++) {
                NotificationRecord rec = changedNotifications.get(i);
                NotificationManagerService.this.mListeners.notifyPostedLocked(rec, rec, false);
            }
        }

        public void notifyInterruptionFilterChanged(final int interruptionFilter) {
            for (final ManagedServices.ManagedServiceInfo serviceInfo : getServices()) {
                if (serviceInfo.isEnabledForCurrentProfiles() && NotificationManagerService.this.isInteractionVisibleToListener(serviceInfo, ActivityManager.getCurrentUser())) {
                    NotificationManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationListeners$$ExternalSyntheticLambda8
                        @Override // java.lang.Runnable
                        public final void run() {
                            NotificationManagerService.NotificationListeners.this.m5162x550f3604(serviceInfo, interruptionFilter);
                        }
                    });
                }
            }
        }

        protected void notifyNotificationChannelChanged(final String pkg, final UserHandle user, final NotificationChannel channel, final int modificationType) {
            if (channel == null) {
                return;
            }
            for (final ManagedServices.ManagedServiceInfo info : getServices()) {
                if (info.enabledAndUserMatches(UserHandle.getCallingUserId()) && NotificationManagerService.this.isInteractionVisibleToListener(info, UserHandle.getCallingUserId())) {
                    BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationListeners$$ExternalSyntheticLambda9
                        @Override // java.lang.Runnable
                        public final void run() {
                            NotificationManagerService.NotificationListeners.this.m5164xa4dfd852(info, pkg, user, channel, modificationType);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyNotificationChannelChanged$9$com-android-server-notification-NotificationManagerService$NotificationListeners  reason: not valid java name */
        public /* synthetic */ void m5164xa4dfd852(ManagedServices.ManagedServiceInfo info, String pkg, UserHandle user, NotificationChannel channel, int modificationType) {
            if (info.isSystem || NotificationManagerService.this.hasCompanionDevice(info) || NotificationManagerService.this.mAssistants.isServiceTokenValidLocked(info.service)) {
                notifyNotificationChannelChanged(info, pkg, user, channel, modificationType);
            }
        }

        protected void notifyNotificationChannelGroupChanged(final String pkg, final UserHandle user, final NotificationChannelGroup group, final int modificationType) {
            if (group == null) {
                return;
            }
            for (final ManagedServices.ManagedServiceInfo info : getServices()) {
                if (info.enabledAndUserMatches(UserHandle.getCallingUserId()) && NotificationManagerService.this.isInteractionVisibleToListener(info, UserHandle.getCallingUserId())) {
                    BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.notification.NotificationManagerService$NotificationListeners$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            NotificationManagerService.NotificationListeners.this.m5165xc3db5cd(info, pkg, user, group, modificationType);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyNotificationChannelGroupChanged$10$com-android-server-notification-NotificationManagerService$NotificationListeners  reason: not valid java name */
        public /* synthetic */ void m5165xc3db5cd(ManagedServices.ManagedServiceInfo info, String pkg, UserHandle user, NotificationChannelGroup group, int modificationType) {
            if (info.isSystem || NotificationManagerService.this.hasCompanionDevice(info)) {
                notifyNotificationChannelGroupChanged(info, pkg, user, group, modificationType);
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: notifyPosted */
        public void m5167xc1351eac(ManagedServices.ManagedServiceInfo info, StatusBarNotification sbn, NotificationRankingUpdate rankingUpdate) {
            INotificationListener listener = info.service;
            StatusBarNotificationHolder sbnHolder = new StatusBarNotificationHolder(ITranNotificationManagerService.Instance().onNotifyPosted(sbn, info));
            try {
                listener.onNotificationPosted(sbnHolder, rankingUpdate);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify listener (posted): " + info, ex);
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: notifyRemoved */
        public void m5169xd5da2f56(ManagedServices.ManagedServiceInfo info, StatusBarNotification sbn, NotificationRankingUpdate rankingUpdate, NotificationStats stats, int reason) {
            INotificationListener listener = info.service;
            StatusBarNotificationHolder sbnHolder = new StatusBarNotificationHolder(sbn);
            try {
                if (!CompatChanges.isChangeEnabled((long) NotificationManagerService.NOTIFICATION_CANCELLATION_REASONS, info.uid) && (reason == 20 || reason == 21)) {
                    reason = 17;
                }
                if (!CompatChanges.isChangeEnabled((long) NotificationManagerService.NOTIFICATION_LOG_ASSISTANT_CANCEL, info.uid) && reason == 22) {
                    reason = 10;
                }
                listener.onNotificationRemoved(sbnHolder, rankingUpdate, stats, reason);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify listener (removed): " + info, ex);
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: notifyRankingUpdate */
        public void m5168xbd02bdd4(ManagedServices.ManagedServiceInfo info, NotificationRankingUpdate rankingUpdate) {
            INotificationListener listener = info.service;
            try {
                listener.onNotificationRankingUpdate(rankingUpdate);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify listener (ranking update): " + info, ex);
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: notifyListenerHintsChanged */
        public void m5163x497f5e8a(ManagedServices.ManagedServiceInfo info, int hints) {
            INotificationListener listener = info.service;
            try {
                listener.onListenerHintsChanged(hints);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify listener (listener hints): " + info, ex);
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: notifyInterruptionFilterChanged */
        public void m5162x550f3604(ManagedServices.ManagedServiceInfo info, int interruptionFilter) {
            INotificationListener listener = info.service;
            try {
                listener.onInterruptionFilterChanged(interruptionFilter);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify listener (interruption filter): " + info, ex);
            }
        }

        void notifyNotificationChannelChanged(ManagedServices.ManagedServiceInfo info, String pkg, UserHandle user, NotificationChannel channel, int modificationType) {
            INotificationListener listener = info.service;
            try {
                listener.onNotificationChannelModification(pkg, user, channel, modificationType);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify listener (channel changed): " + info, ex);
            }
        }

        private void notifyNotificationChannelGroupChanged(ManagedServices.ManagedServiceInfo info, String pkg, UserHandle user, NotificationChannelGroup group, int modificationType) {
            INotificationListener listener = info.service;
            try {
                listener.onNotificationChannelGroupModification(pkg, user, group, modificationType);
            } catch (RemoteException ex) {
                Slog.e(this.TAG, "unable to notify listener (channel group changed): " + info, ex);
            }
        }

        public boolean isListenerPackage(String packageName) {
            if (packageName == null) {
                return false;
            }
            synchronized (NotificationManagerService.this.mNotificationLock) {
                for (ManagedServices.ManagedServiceInfo serviceInfo : getServices()) {
                    if (packageName.equals(serviceInfo.component.getPackageName())) {
                        return true;
                    }
                }
                return false;
            }
        }

        boolean hasAllowedListener(String packageName, int userId) {
            if (packageName == null) {
                return false;
            }
            List<ComponentName> allowedComponents = getAllowedComponents(userId);
            for (int i = 0; i < allowedComponents.size(); i++) {
                if (allowedComponents.get(i).getPackageName().equals(packageName)) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class RoleObserver implements OnRoleHoldersChangedListener {
        private final Executor mExecutor;
        private final Looper mMainLooper;
        private ArrayMap<String, ArrayMap<Integer, ArraySet<String>>> mNonBlockableDefaultApps;
        private final IPackageManager mPm;
        private final RoleManager mRm;
        private volatile ArraySet<Integer> mTrampolineExemptUids = new ArraySet<>();

        RoleObserver(Context context, RoleManager roleManager, IPackageManager pkgMgr, Looper mainLooper) {
            this.mRm = roleManager;
            this.mPm = pkgMgr;
            this.mExecutor = context.getMainExecutor();
            this.mMainLooper = mainLooper;
        }

        public void init() {
            List<UserHandle> users = NotificationManagerService.this.mUm.getUserHandles(true);
            this.mNonBlockableDefaultApps = new ArrayMap<>();
            for (int i = 0; i < NotificationManagerService.NON_BLOCKABLE_DEFAULT_ROLES.length; i++) {
                ArrayMap<Integer, ArraySet<String>> userToApprovedList = new ArrayMap<>();
                this.mNonBlockableDefaultApps.put(NotificationManagerService.NON_BLOCKABLE_DEFAULT_ROLES[i], userToApprovedList);
                for (int j = 0; j < users.size(); j++) {
                    Integer userId = Integer.valueOf(users.get(j).getIdentifier());
                    ArraySet<String> approvedForUserId = new ArraySet<>(this.mRm.getRoleHoldersAsUser(NotificationManagerService.NON_BLOCKABLE_DEFAULT_ROLES[i], UserHandle.of(userId.intValue())));
                    ArraySet<Pair<String, Integer>> approvedAppUids = new ArraySet<>();
                    Iterator<String> it = approvedForUserId.iterator();
                    while (it.hasNext()) {
                        String pkg = it.next();
                        approvedAppUids.add(new Pair<>(pkg, Integer.valueOf(getUidForPackage(pkg, userId.intValue()))));
                    }
                    userToApprovedList.put(userId, approvedForUserId);
                    NotificationManagerService.this.mPreferencesHelper.updateDefaultApps(userId.intValue(), null, approvedAppUids);
                }
            }
            updateTrampolineExemptUidsForUsers((UserHandle[]) users.toArray(new UserHandle[0]));
            this.mRm.addOnRoleHoldersChangedListenerAsUser(this.mExecutor, this, UserHandle.ALL);
        }

        public boolean isApprovedPackageForRoleForUser(String role, String pkg, int userId) {
            return this.mNonBlockableDefaultApps.get(role).get(Integer.valueOf(userId)).contains(pkg);
        }

        public boolean isUidExemptFromTrampolineRestrictions(int uid) {
            return this.mTrampolineExemptUids.contains(Integer.valueOf(uid));
        }

        public void onRoleHoldersChanged(String roleName, UserHandle user) {
            onRoleHoldersChangedForNonBlockableDefaultApps(roleName, user);
            onRoleHoldersChangedForTrampolines(roleName, user);
        }

        private void onRoleHoldersChangedForNonBlockableDefaultApps(String roleName, UserHandle user) {
            boolean relevantChange = false;
            int i = 0;
            while (true) {
                if (i >= NotificationManagerService.NON_BLOCKABLE_DEFAULT_ROLES.length) {
                    break;
                } else if (!NotificationManagerService.NON_BLOCKABLE_DEFAULT_ROLES[i].equals(roleName)) {
                    i++;
                } else {
                    relevantChange = true;
                    break;
                }
            }
            if (!relevantChange) {
                return;
            }
            ArraySet<String> roleHolders = new ArraySet<>(this.mRm.getRoleHoldersAsUser(roleName, user));
            ArrayMap<Integer, ArraySet<String>> prevApprovedForRole = this.mNonBlockableDefaultApps.getOrDefault(roleName, new ArrayMap<>());
            ArraySet<String> previouslyApproved = prevApprovedForRole.getOrDefault(Integer.valueOf(user.getIdentifier()), new ArraySet<>());
            ArraySet<String> toRemove = new ArraySet<>();
            ArraySet<Pair<String, Integer>> toAdd = new ArraySet<>();
            Iterator<String> it = previouslyApproved.iterator();
            while (it.hasNext()) {
                String previous = it.next();
                if (!roleHolders.contains(previous)) {
                    toRemove.add(previous);
                }
            }
            Iterator<String> it2 = roleHolders.iterator();
            while (it2.hasNext()) {
                String nowApproved = it2.next();
                if (!previouslyApproved.contains(nowApproved)) {
                    toAdd.add(new Pair<>(nowApproved, Integer.valueOf(getUidForPackage(nowApproved, user.getIdentifier()))));
                }
            }
            prevApprovedForRole.put(Integer.valueOf(user.getIdentifier()), roleHolders);
            this.mNonBlockableDefaultApps.put(roleName, prevApprovedForRole);
            NotificationManagerService.this.mPreferencesHelper.updateDefaultApps(user.getIdentifier(), toRemove, toAdd);
        }

        private void onRoleHoldersChangedForTrampolines(String roleName, UserHandle user) {
            if (!"android.app.role.BROWSER".equals(roleName)) {
                return;
            }
            updateTrampolineExemptUidsForUsers(user);
        }

        private void updateTrampolineExemptUidsForUsers(UserHandle... users) {
            Preconditions.checkState(this.mMainLooper.isCurrentThread());
            ArraySet<Integer> oldUids = this.mTrampolineExemptUids;
            ArraySet<Integer> newUids = new ArraySet<>();
            int n = oldUids.size();
            for (int i = 0; i < n; i++) {
                int uid = oldUids.valueAt(i).intValue();
                if (!ArrayUtils.contains(users, UserHandle.of(UserHandle.getUserId(uid)))) {
                    newUids.add(Integer.valueOf(uid));
                }
            }
            for (UserHandle user : users) {
                for (String pkg : this.mRm.getRoleHoldersAsUser("android.app.role.BROWSER", user)) {
                    int uid2 = getUidForPackage(pkg, user.getIdentifier());
                    if (uid2 != -1) {
                        newUids.add(Integer.valueOf(uid2));
                    } else {
                        Slog.e(NotificationManagerService.TAG, "Bad uid (-1) for browser package " + pkg);
                    }
                }
            }
            this.mTrampolineExemptUids = newUids;
        }

        private int getUidForPackage(String pkg, int userId) {
            try {
                return this.mPm.getPackageUid(pkg, 131072L, userId);
            } catch (RemoteException e) {
                Slog.e(NotificationManagerService.TAG, "role manager has bad default " + pkg + " " + userId);
                return -1;
            }
        }
    }

    /* loaded from: classes2.dex */
    public static final class DumpFilter {
        public String pkgFilter;
        public boolean rvStats;
        public long since;
        public boolean stats;
        public boolean zen;
        public boolean filtered = false;
        public boolean redact = true;
        public boolean proto = false;
        public boolean criticalPriority = false;
        public boolean normalPriority = false;

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code restructure failed: missing block: B:45:0x00b6, code lost:
            if (r3.equals(com.android.server.utils.PriorityDump.PRIORITY_ARG_CRITICAL) != false) goto L47;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public static DumpFilter parseFromArguments(String[] args) {
            DumpFilter filter = new DumpFilter();
            int ai = 0;
            while (ai < args.length) {
                String a = args[ai];
                if ("--proto".equals(a)) {
                    filter.proto = true;
                } else {
                    boolean z = false;
                    if ("--noredact".equals(a) || "--reveal".equals(a)) {
                        filter.redact = false;
                    } else if ("p".equals(a) || "pkg".equals(a) || "--package".equals(a)) {
                        if (ai < args.length - 1) {
                            ai++;
                            String lowerCase = args[ai].trim().toLowerCase();
                            filter.pkgFilter = lowerCase;
                            if (lowerCase.isEmpty()) {
                                filter.pkgFilter = null;
                            } else {
                                filter.filtered = true;
                            }
                        }
                    } else if ("--zen".equals(a) || "zen".equals(a)) {
                        filter.filtered = true;
                        filter.zen = true;
                    } else if ("--stats".equals(a)) {
                        filter.stats = true;
                        if (ai < args.length - 1) {
                            ai++;
                            filter.since = Long.parseLong(args[ai]);
                        } else {
                            filter.since = 0L;
                        }
                    } else if ("--remote-view-stats".equals(a)) {
                        filter.rvStats = true;
                        if (ai < args.length - 1) {
                            ai++;
                            filter.since = Long.parseLong(args[ai]);
                        } else {
                            filter.since = 0L;
                        }
                    } else if (PriorityDump.PRIORITY_ARG.equals(a) && ai < args.length - 1) {
                        ai++;
                        String str = args[ai];
                        switch (str.hashCode()) {
                            case -1986416409:
                                if (str.equals(PriorityDump.PRIORITY_ARG_NORMAL)) {
                                    z = true;
                                    break;
                                }
                                z = true;
                                break;
                            case -1560189025:
                                break;
                            default:
                                z = true;
                                break;
                        }
                        switch (z) {
                            case false:
                                filter.criticalPriority = true;
                                continue;
                            case true:
                                filter.normalPriority = true;
                                continue;
                        }
                    }
                }
                ai++;
            }
            return filter;
        }

        public boolean matches(StatusBarNotification sbn) {
            if (this.filtered && !this.zen) {
                return sbn != null && (matches(sbn.getPackageName()) || matches(sbn.getOpPkg()));
            }
            return true;
        }

        public boolean matches(ComponentName component) {
            if (this.filtered && !this.zen) {
                return component != null && matches(component.getPackageName());
            }
            return true;
        }

        public boolean matches(String pkg) {
            if (this.filtered && !this.zen) {
                return pkg != null && pkg.toLowerCase().contains(this.pkgFilter);
            }
            return true;
        }

        public String toString() {
            return this.stats ? "stats" : this.zen ? "zen" : '\'' + this.pkgFilter + '\'';
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetAssistantUserSet(int userId) {
        checkCallerIsSystemOrShell();
        this.mAssistants.setUserSet(userId, false);
        handleSavePolicyFile();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getApprovedAssistant(int userId) {
        checkCallerIsSystemOrShell();
        List<ComponentName> allowedComponents = this.mAssistants.getAllowedComponents(userId);
        return (ComponentName) CollectionUtils.firstOrNull(allowedComponents);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class StatusBarNotificationHolder extends IStatusBarNotificationHolder.Stub {
        private StatusBarNotification mValue;

        public StatusBarNotificationHolder(StatusBarNotification value) {
            this.mValue = value;
        }

        public StatusBarNotification get() {
            StatusBarNotification value = this.mValue;
            this.mValue = null;
            return value;
        }
    }

    private void writeSecureNotificationsPolicy(TypedXmlSerializer out) throws IOException {
        out.startTag((String) null, LOCKSCREEN_ALLOW_SECURE_NOTIFICATIONS_TAG);
        out.attributeBoolean((String) null, LOCKSCREEN_ALLOW_SECURE_NOTIFICATIONS_VALUE, this.mLockScreenAllowSecureNotifications);
        out.endTag((String) null, LOCKSCREEN_ALLOW_SECURE_NOTIFICATIONS_TAG);
    }

    protected Notification createReviewPermissionsNotification() {
        Intent tapIntent = new Intent("android.settings.ALL_APPS_NOTIFICATION_SETTINGS_FOR_REVIEW");
        Intent remindIntent = new Intent(REVIEW_NOTIF_ACTION_REMIND);
        Intent dismissIntent = new Intent(REVIEW_NOTIF_ACTION_DISMISS);
        Intent swipeIntent = new Intent(REVIEW_NOTIF_ACTION_CANCELED);
        Notification.Action remindMe = new Notification.Action.Builder((Icon) null, getContext().getResources().getString(17041416), PendingIntent.getBroadcast(getContext(), 0, remindIntent, AudioFormat.DTS_HD)).build();
        Notification.Action dismiss = new Notification.Action.Builder((Icon) null, getContext().getResources().getString(17041415), PendingIntent.getBroadcast(getContext(), 0, dismissIntent, AudioFormat.DTS_HD)).build();
        return new Notification.Builder(getContext(), SystemNotificationChannels.SYSTEM_CHANGES).setSmallIcon(17303614).setContentTitle(getContext().getResources().getString(17041418)).setContentText(getContext().getResources().getString(17041417)).setContentIntent(PendingIntent.getActivity(getContext(), 0, tapIntent, AudioFormat.DTS_HD)).setStyle(new Notification.BigTextStyle()).setFlag(32, true).setAutoCancel(true).addAction(remindMe).addAction(dismiss).setDeleteIntent(PendingIntent.getBroadcast(getContext(), 0, swipeIntent, AudioFormat.DTS_HD)).build();
    }

    protected void maybeShowInitialReviewPermissionsNotification() {
        if (!this.mShowReviewPermissionsNotification) {
            return;
        }
        int currentState = Settings.Global.getInt(getContext().getContentResolver(), "review_permissions_notification_state", -1);
        if (currentState == 0 || currentState == 3) {
            NotificationManager nm = (NotificationManager) getContext().getSystemService(NotificationManager.class);
            nm.notify(TAG, 71, createReviewPermissionsNotification());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class NotificationTrampolineCallback implements BackgroundActivityStartCallback {
        private NotificationTrampolineCallback() {
        }

        @Override // com.android.server.wm.BackgroundActivityStartCallback
        public boolean isActivityStartAllowed(Collection<IBinder> tokens, int uid, String packageName) {
            Preconditions.checkArgument(!tokens.isEmpty());
            for (IBinder token : tokens) {
                if (token != NotificationManagerService.ALLOWLIST_TOKEN) {
                    return true;
                }
            }
            String logcatMessage = "Indirect notification activity start (trampoline) from " + packageName;
            if (blockTrampoline(uid)) {
                Slog.e(NotificationManagerService.TAG, logcatMessage + " blocked");
                return false;
            }
            Slog.w(NotificationManagerService.TAG, logcatMessage + ", this should be avoided for performance reasons");
            return true;
        }

        private boolean blockTrampoline(int uid) {
            if (NotificationManagerService.this.mRoleObserver != null && NotificationManagerService.this.mRoleObserver.isUidExemptFromTrampolineRestrictions(uid)) {
                return CompatChanges.isChangeEnabled((long) NotificationManagerService.NOTIFICATION_TRAMPOLINE_BLOCK_FOR_EXEMPT_ROLES, uid);
            }
            return CompatChanges.isChangeEnabled((long) NotificationManagerService.NOTIFICATION_TRAMPOLINE_BLOCK, uid);
        }

        @Override // com.android.server.wm.BackgroundActivityStartCallback
        public boolean canCloseSystemDialogs(Collection<IBinder> tokens, int uid) {
            return tokens.contains(NotificationManagerService.ALLOWLIST_TOKEN) && !CompatChanges.isChangeEnabled((long) NotificationManagerService.NOTIFICATION_TRAMPOLINE_BLOCK, uid);
        }
    }

    @Override // com.android.server.SystemService
    public void onUserUnlocked(SystemService.TargetUser user) {
        INotificationManagerServiceLice.Instance().onUserUnlocked(getContext(), user);
    }
}
