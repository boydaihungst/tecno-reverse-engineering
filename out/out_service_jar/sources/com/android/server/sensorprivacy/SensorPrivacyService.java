package com.android.server.sensorprivacy;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.AppOpsManager;
import android.app.AppOpsManagerInternal;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.graphics.drawable.Icon;
import android.hardware.ISensorPrivacyListener;
import android.hardware.ISensorPrivacyManager;
import android.hardware.SensorPrivacyManager;
import android.hardware.SensorPrivacyManagerInternal;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.voice.VoiceInteractionManagerInternal;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.emergency.EmergencyNumber;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Pair;
import android.util.proto.ProtoOutputStream;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.function.HexConsumer;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.sensorprivacy.SensorPrivacyService;
import com.android.server.sensorprivacy.SensorPrivacyStateController;
import com.android.server.slice.SliceClientPermissions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
/* loaded from: classes2.dex */
public final class SensorPrivacyService extends SystemService {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_LOGGING = false;
    public static final int REMINDER_DIALOG_DELAY_MILLIS = 500;
    private static final String SENSOR_PRIVACY_CHANNEL_ID = "sensor_privacy";
    private final ActivityManager mActivityManager;
    private final ActivityManagerInternal mActivityManagerInternal;
    private final ActivityTaskManager mActivityTaskManager;
    private final AppOpsManager mAppOpsManager;
    private final AppOpsManagerInternal mAppOpsManagerInternal;
    private final IBinder mAppOpsRestrictionToken;
    private CallStateHelper mCallStateHelper;
    private CameraPrivacyLightController mCameraPrivacyLightController;
    private final Context mContext;
    private int mCurrentUser;
    private KeyguardManager mKeyguardManager;
    private final PackageManagerInternal mPackageManagerInternal;
    private SensorPrivacyManagerInternalImpl mSensorPrivacyManagerInternal;
    private final SensorPrivacyServiceImpl mSensorPrivacyServiceImpl;
    private final TelephonyManager mTelephonyManager;
    private final UserManagerInternal mUserManagerInternal;
    private static final String TAG = SensorPrivacyService.class.getSimpleName();
    private static final String ACTION_DISABLE_TOGGLE_SENSOR_PRIVACY = SensorPrivacyService.class.getName() + ".action.disable_sensor_privacy";

    public SensorPrivacyService(Context context) {
        super(context);
        this.mAppOpsRestrictionToken = new Binder();
        this.mCurrentUser = -10000;
        this.mContext = context;
        this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mAppOpsManagerInternal = (AppOpsManagerInternal) getLocalService(AppOpsManagerInternal.class);
        this.mUserManagerInternal = (UserManagerInternal) getLocalService(UserManagerInternal.class);
        this.mActivityManager = (ActivityManager) context.getSystemService(ActivityManager.class);
        this.mActivityManagerInternal = (ActivityManagerInternal) getLocalService(ActivityManagerInternal.class);
        this.mActivityTaskManager = (ActivityTaskManager) context.getSystemService(ActivityTaskManager.class);
        this.mTelephonyManager = (TelephonyManager) context.getSystemService(TelephonyManager.class);
        this.mPackageManagerInternal = (PackageManagerInternal) getLocalService(PackageManagerInternal.class);
        this.mSensorPrivacyServiceImpl = new SensorPrivacyServiceImpl();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService(SENSOR_PRIVACY_CHANNEL_ID, this.mSensorPrivacyServiceImpl);
        SensorPrivacyManagerInternalImpl sensorPrivacyManagerInternalImpl = new SensorPrivacyManagerInternalImpl();
        this.mSensorPrivacyManagerInternal = sensorPrivacyManagerInternalImpl;
        publishLocalService(SensorPrivacyManagerInternal.class, sensorPrivacyManagerInternalImpl);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService(KeyguardManager.class);
            this.mCallStateHelper = new CallStateHelper();
        } else if (phase == 550) {
            this.mCameraPrivacyLightController = new CameraPrivacyLightController(this.mContext);
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStarting(SystemService.TargetUser user) {
        if (this.mCurrentUser == -10000) {
            this.mCurrentUser = user.getUserIdentifier();
            this.mSensorPrivacyServiceImpl.userSwitching(-10000, user.getUserIdentifier());
        }
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        this.mCurrentUser = to.getUserIdentifier();
        this.mSensorPrivacyServiceImpl.userSwitching(from.getUserIdentifier(), to.getUserIdentifier());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class SensorPrivacyServiceImpl extends ISensorPrivacyManager.Stub implements AppOpsManager.OnOpNotedListener, AppOpsManager.OnOpStartedListener, IBinder.DeathRecipient, UserManagerInternal.UserRestrictionsListener {
        private final SensorPrivacyHandler mHandler;
        private SensorPrivacyStateController mSensorPrivacyStateController;
        private final Object mLock = new Object();
        private ArrayMap<Pair<Integer, UserHandle>, ArrayList<IBinder>> mSuppressReminders = new ArrayMap<>();
        private final ArrayMap<SensorUseReminderDialogInfo, ArraySet<Integer>> mQueuedSensorUseReminderDialogs = new ArrayMap<>();

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class SensorUseReminderDialogInfo {
            private String mPackageName;
            private int mTaskId;
            private UserHandle mUser;

            SensorUseReminderDialogInfo(int taskId, UserHandle user, String packageName) {
                this.mTaskId = taskId;
                this.mUser = user;
                this.mPackageName = packageName;
            }

            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || !(o instanceof SensorUseReminderDialogInfo)) {
                    return false;
                }
                SensorUseReminderDialogInfo that = (SensorUseReminderDialogInfo) o;
                if (this.mTaskId == that.mTaskId && Objects.equals(this.mUser, that.mUser) && Objects.equals(this.mPackageName, that.mPackageName)) {
                    return true;
                }
                return false;
            }

            public int hashCode() {
                return Objects.hash(Integer.valueOf(this.mTaskId), this.mUser, this.mPackageName);
            }
        }

        SensorPrivacyServiceImpl() {
            final SensorPrivacyHandler sensorPrivacyHandler = new SensorPrivacyHandler(FgThread.get().getLooper(), SensorPrivacyService.this.mContext);
            this.mHandler = sensorPrivacyHandler;
            this.mSensorPrivacyStateController = SensorPrivacyStateController.getInstance();
            int[] micAndCameraOps = {27, 100, 26, 101};
            SensorPrivacyService.this.mAppOpsManager.startWatchingNoted(micAndCameraOps, this);
            SensorPrivacyService.this.mAppOpsManager.startWatchingStarted(micAndCameraOps, this);
            SensorPrivacyService.this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.sensorprivacy.SensorPrivacyService.SensorPrivacyServiceImpl.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    SensorPrivacyServiceImpl.this.setToggleSensorPrivacy(((UserHandle) intent.getParcelableExtra("android.intent.extra.USER")).getIdentifier(), 5, intent.getIntExtra(SensorPrivacyManager.EXTRA_SENSOR, 0), false);
                }
            }, new IntentFilter(SensorPrivacyService.ACTION_DISABLE_TOGGLE_SENSOR_PRIVACY), "android.permission.MANAGE_SENSOR_PRIVACY", null, 2);
            SensorPrivacyService.this.mContext.registerReceiver(new AnonymousClass2(SensorPrivacyService.this), new IntentFilter("android.intent.action.ACTION_SHUTDOWN"));
            SensorPrivacyService.this.mUserManagerInternal.addUserRestrictionsListener(this);
            SensorPrivacyStateController sensorPrivacyStateController = this.mSensorPrivacyStateController;
            Objects.requireNonNull(sensorPrivacyHandler);
            sensorPrivacyStateController.setAllSensorPrivacyListener(sensorPrivacyHandler, new SensorPrivacyStateController.AllSensorPrivacyListener() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl$$ExternalSyntheticLambda2
                @Override // com.android.server.sensorprivacy.SensorPrivacyStateController.AllSensorPrivacyListener
                public final void onAllSensorPrivacyChanged(boolean z) {
                    SensorPrivacyService.SensorPrivacyHandler.this.handleSensorPrivacyChanged(z);
                }
            });
            this.mSensorPrivacyStateController.setSensorPrivacyListener(sensorPrivacyHandler, new SensorPrivacyStateController.SensorPrivacyListener() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl$$ExternalSyntheticLambda3
                @Override // com.android.server.sensorprivacy.SensorPrivacyStateController.SensorPrivacyListener
                public final void onSensorPrivacyChanged(int i, int i2, int i3, SensorState sensorState) {
                    SensorPrivacyService.SensorPrivacyServiceImpl.this.m6460x530afe5a(i, i2, i3, sensorState);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl$2  reason: invalid class name */
        /* loaded from: classes2.dex */
        public class AnonymousClass2 extends BroadcastReceiver {
            final /* synthetic */ SensorPrivacyService val$this$0;

            AnonymousClass2(SensorPrivacyService sensorPrivacyService) {
                this.val$this$0 = sensorPrivacyService;
            }

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                SensorPrivacyServiceImpl.this.mSensorPrivacyStateController.forEachState(new SensorPrivacyStateController.SensorPrivacyStateConsumer() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl$2$$ExternalSyntheticLambda0
                    @Override // com.android.server.sensorprivacy.SensorPrivacyStateController.SensorPrivacyStateConsumer
                    public final void accept(int i, int i2, int i3, SensorState sensorState) {
                        SensorPrivacyService.SensorPrivacyServiceImpl.AnonymousClass2.this.m6466xd5cd5c4(i, i2, i3, sensorState);
                    }
                });
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$onReceive$0$com-android-server-sensorprivacy-SensorPrivacyService$SensorPrivacyServiceImpl$2  reason: not valid java name */
            public /* synthetic */ void m6466xd5cd5c4(int toggleType, int userId, int sensor, SensorState state) {
                SensorPrivacyServiceImpl.this.logSensorPrivacyToggle(5, sensor, state.isEnabled(), state.getLastChange(), true);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$0$com-android-server-sensorprivacy-SensorPrivacyService$SensorPrivacyServiceImpl  reason: not valid java name */
        public /* synthetic */ void m6460x530afe5a(int toggleType, int userId, int sensor, SensorState state) {
            this.mHandler.handleSensorPrivacyChanged(userId, toggleType, sensor, state.isEnabled());
        }

        @Override // com.android.server.pm.UserManagerInternal.UserRestrictionsListener
        public void onUserRestrictionsChanged(int userId, Bundle newRestrictions, Bundle prevRestrictions) {
            if (!prevRestrictions.getBoolean("disallow_camera_toggle") && newRestrictions.getBoolean("disallow_camera_toggle")) {
                setToggleSensorPrivacyUnchecked(1, userId, 5, 2, false);
            }
            if (!prevRestrictions.getBoolean("disallow_microphone_toggle") && newRestrictions.getBoolean("disallow_microphone_toggle")) {
                setToggleSensorPrivacyUnchecked(1, userId, 5, 1, false);
            }
        }

        public void onOpStarted(int code, int uid, String packageName, String attributionTag, int flags, int result) {
            onOpNoted(code, uid, packageName, attributionTag, flags, result);
        }

        public void onOpNoted(int code, int uid, String packageName, String attributionTag, int flags, int result) {
            int sensor;
            if ((flags & 13) != 0 && result == 1) {
                if (code == 27 || code == 100) {
                    sensor = 1;
                } else if (code == 26 || code == 101) {
                    sensor = 2;
                } else {
                    return;
                }
                long token = Binder.clearCallingIdentity();
                try {
                    onSensorUseStarted(uid, packageName, sensor);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        private void onSensorUseStarted(int uid, String packageName, int sensor) {
            UserHandle user = UserHandle.of(SensorPrivacyService.this.mCurrentUser);
            if (!isCombinedToggleSensorPrivacyEnabled(sensor) || uid == 1000) {
                return;
            }
            synchronized (this.mLock) {
                if (this.mSuppressReminders.containsKey(new Pair(Integer.valueOf(sensor), user))) {
                    Log.d(SensorPrivacyService.TAG, "Suppressed sensor privacy reminder for " + packageName + SliceClientPermissions.SliceAuthority.DELIMITER + user);
                    return;
                }
                List<ActivityManager.RunningTaskInfo> tasksOfPackageUsingSensor = new ArrayList<>();
                List<ActivityManager.RunningTaskInfo> tasks = SensorPrivacyService.this.mActivityTaskManager.getTasks(Integer.MAX_VALUE);
                int numTasks = tasks.size();
                for (int taskNum = 0; taskNum < numTasks; taskNum++) {
                    ActivityManager.RunningTaskInfo task = tasks.get(taskNum);
                    if (task.isVisible) {
                        if (task.topActivity.getPackageName().equals(packageName)) {
                            if (task.isFocused) {
                                enqueueSensorUseReminderDialogAsync(task.taskId, user, packageName, sensor);
                                return;
                            }
                            tasksOfPackageUsingSensor.add(task);
                        } else if (task.topActivity.flattenToString().equals(getSensorUseActivityName(new ArraySet<>(Arrays.asList(Integer.valueOf(sensor))))) && task.isFocused) {
                            enqueueSensorUseReminderDialogAsync(task.taskId, user, packageName, sensor);
                        }
                    }
                }
                int taskNum2 = tasksOfPackageUsingSensor.size();
                if (taskNum2 == 1) {
                    enqueueSensorUseReminderDialogAsync(tasksOfPackageUsingSensor.get(0).taskId, user, packageName, sensor);
                } else if (tasksOfPackageUsingSensor.size() > 1) {
                    showSensorUseReminderNotification(user, packageName, sensor);
                } else {
                    List<ActivityManager.RunningServiceInfo> services = SensorPrivacyService.this.mActivityManager.getRunningServices(Integer.MAX_VALUE);
                    int numServices = services.size();
                    for (int serviceNum = 0; serviceNum < numServices; serviceNum++) {
                        ActivityManager.RunningServiceInfo service = services.get(serviceNum);
                        if (service.foreground && service.service.getPackageName().equals(packageName)) {
                            showSensorUseReminderNotification(user, packageName, sensor);
                            return;
                        }
                    }
                    String inputMethodComponent = Settings.Secure.getStringForUser(SensorPrivacyService.this.mContext.getContentResolver(), "default_input_method", user.getIdentifier());
                    String inputMethodPackageName = null;
                    if (inputMethodComponent != null && !inputMethodComponent.isEmpty()) {
                        inputMethodPackageName = ComponentName.unflattenFromString(inputMethodComponent).getPackageName();
                    }
                    try {
                        int capability = SensorPrivacyService.this.mActivityManagerInternal.getUidCapability(uid);
                        if (sensor == 1) {
                            VoiceInteractionManagerInternal voiceInteractionManagerInternal = (VoiceInteractionManagerInternal) LocalServices.getService(VoiceInteractionManagerInternal.class);
                            if (voiceInteractionManagerInternal != null && voiceInteractionManagerInternal.hasActiveSession(packageName)) {
                                enqueueSensorUseReminderDialogAsync(-1, user, packageName, sensor);
                                return;
                            } else if (TextUtils.equals(packageName, inputMethodPackageName) && (capability & 4) != 0) {
                                enqueueSensorUseReminderDialogAsync(-1, user, packageName, sensor);
                                return;
                            }
                        }
                        if (sensor == 2 && TextUtils.equals(packageName, inputMethodPackageName) && (capability & 2) != 0) {
                            enqueueSensorUseReminderDialogAsync(-1, user, packageName, sensor);
                        } else {
                            Log.i(SensorPrivacyService.TAG, packageName + SliceClientPermissions.SliceAuthority.DELIMITER + uid + " started using sensor " + sensor + " but no activity or foreground service was running. The user will not be informed. System components should check if sensor privacy is enabled for the sensor before accessing it.");
                        }
                    } catch (IllegalArgumentException e) {
                        Log.w(SensorPrivacyService.TAG, e);
                    }
                }
            }
        }

        private void enqueueSensorUseReminderDialogAsync(int taskId, UserHandle user, String packageName, int sensor) {
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuintConsumer() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl$$ExternalSyntheticLambda6
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                    ((SensorPrivacyService.SensorPrivacyServiceImpl) obj).enqueueSensorUseReminderDialog(((Integer) obj2).intValue(), (UserHandle) obj3, (String) obj4, ((Integer) obj5).intValue());
                }
            }, this, Integer.valueOf(taskId), user, packageName, Integer.valueOf(sensor)));
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void enqueueSensorUseReminderDialog(int taskId, UserHandle user, String packageName, int sensor) {
            SensorUseReminderDialogInfo info = new SensorUseReminderDialogInfo(taskId, user, packageName);
            if (!this.mQueuedSensorUseReminderDialogs.containsKey(info)) {
                ArraySet<Integer> sensors = new ArraySet<>();
                if ((sensor == 1 && this.mSuppressReminders.containsKey(new Pair(2, user))) || (sensor == 2 && this.mSuppressReminders.containsKey(new Pair(1, user)))) {
                    sensors.add(1);
                    sensors.add(2);
                } else {
                    sensors.add(Integer.valueOf(sensor));
                }
                this.mQueuedSensorUseReminderDialogs.put(info, sensors);
                this.mHandler.sendMessageDelayed(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl$$ExternalSyntheticLambda0
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        ((SensorPrivacyService.SensorPrivacyServiceImpl) obj).showSensorUserReminderDialog((SensorPrivacyService.SensorPrivacyServiceImpl.SensorUseReminderDialogInfo) obj2);
                    }
                }, this, info), 500L);
                return;
            }
            this.mQueuedSensorUseReminderDialogs.get(info).add(Integer.valueOf(sensor));
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void showSensorUserReminderDialog(SensorUseReminderDialogInfo info) {
            ArraySet<Integer> sensors = this.mQueuedSensorUseReminderDialogs.get(info);
            this.mQueuedSensorUseReminderDialogs.remove(info);
            if (sensors == null) {
                Log.e(SensorPrivacyService.TAG, "Unable to show sensor use dialog because sensor set is null. Was the dialog queue modified from outside the handler thread?");
                return;
            }
            Intent dialogIntent = new Intent();
            dialogIntent.setComponent(ComponentName.unflattenFromString(getSensorUseActivityName(sensors)));
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchTaskId(info.mTaskId);
            options.setTaskOverlay(true, true);
            dialogIntent.addFlags(8650752);
            dialogIntent.putExtra("android.intent.extra.PACKAGE_NAME", info.mPackageName);
            if (sensors.size() == 1) {
                dialogIntent.putExtra(SensorPrivacyManager.EXTRA_SENSOR, sensors.valueAt(0));
            } else if (sensors.size() == 2) {
                dialogIntent.putExtra(SensorPrivacyManager.EXTRA_ALL_SENSORS, true);
            } else {
                Log.e(SensorPrivacyService.TAG, "Attempted to show sensor use dialog for " + sensors.size() + " sensors");
                return;
            }
            SensorPrivacyService.this.mContext.startActivityAsUser(dialogIntent, options.toBundle(), UserHandle.SYSTEM);
        }

        private String getSensorUseActivityName(ArraySet<Integer> sensors) {
            Iterator<Integer> it = sensors.iterator();
            while (it.hasNext()) {
                Integer sensor = it.next();
                if (isToggleSensorPrivacyEnabled(2, sensor.intValue())) {
                    return SensorPrivacyService.this.mContext.getResources().getString(17040036);
                }
            }
            return SensorPrivacyService.this.mContext.getResources().getString(17040035);
        }

        private void showSensorUseReminderNotification(UserHandle user, String packageName, int sensor) {
            int iconRes;
            int messageRes;
            int notificationId;
            long j;
            try {
                CharSequence packageLabel = SensorPrivacyService.this.getUiContext().getPackageManager().getApplicationInfoAsUser(packageName, 0, user).loadLabel(SensorPrivacyService.this.mContext.getPackageManager());
                if (sensor == 1) {
                    iconRes = 17302801;
                    messageRes = 17041480;
                    notificationId = 65;
                } else {
                    iconRes = 17302387;
                    messageRes = 17041478;
                    notificationId = 66;
                }
                NotificationManager notificationManager = (NotificationManager) SensorPrivacyService.this.mContext.getSystemService(NotificationManager.class);
                NotificationChannel channel = new NotificationChannel(SensorPrivacyService.SENSOR_PRIVACY_CHANNEL_ID, SensorPrivacyService.this.getUiContext().getString(17041477), 4);
                channel.setSound(null, null);
                channel.setBypassDnd(true);
                channel.enableVibration(false);
                channel.setBlockable(false);
                notificationManager.createNotificationChannel(channel);
                Icon icon = Icon.createWithResource(SensorPrivacyService.this.getUiContext().getResources(), iconRes);
                String contentTitle = SensorPrivacyService.this.getUiContext().getString(messageRes);
                Spanned contentText = Html.fromHtml(SensorPrivacyService.this.getUiContext().getString(17041481, packageLabel), 0);
                PendingIntent contentIntent = PendingIntent.getActivity(SensorPrivacyService.this.mContext, sensor, new Intent("android.settings.PRIVACY_SETTINGS"), AudioFormat.DTS_HD);
                String actionTitle = SensorPrivacyService.this.getUiContext().getString(17041479);
                PendingIntent actionIntent = PendingIntent.getBroadcast(SensorPrivacyService.this.mContext, sensor, new Intent(SensorPrivacyService.ACTION_DISABLE_TOGGLE_SENSOR_PRIVACY).setPackage(SensorPrivacyService.this.mContext.getPackageName()).putExtra(SensorPrivacyManager.EXTRA_SENSOR, sensor).putExtra("android.intent.extra.USER", user), AudioFormat.DTS_HD);
                Notification.Builder extend = new Notification.Builder(SensorPrivacyService.this.mContext, SensorPrivacyService.SENSOR_PRIVACY_CHANNEL_ID).setContentTitle(contentTitle).setContentText(contentText).setSmallIcon(icon).addAction(new Notification.Action.Builder(icon, actionTitle, actionIntent).build()).setContentIntent(contentIntent).extend(new Notification.TvExtender());
                if (isTelevision(SensorPrivacyService.this.mContext)) {
                    j = 1;
                } else {
                    j = 0;
                }
                notificationManager.notify(notificationId, extend.setTimeoutAfter(j).build());
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(SensorPrivacyService.TAG, "Cannot show sensor use notification for " + packageName);
            }
        }

        private boolean isTelevision(Context context) {
            int uiMode = context.getResources().getConfiguration().uiMode;
            return (uiMode & 15) == 4;
        }

        public void setSensorPrivacy(boolean enable) {
            enforceManageSensorPrivacyPermission();
            this.mSensorPrivacyStateController.setAllSensorState(enable);
        }

        public void setToggleSensorPrivacy(int userId, int source, int sensor, boolean enable) {
            enforceManageSensorPrivacyPermission();
            if (userId == -2) {
                userId = SensorPrivacyService.this.mCurrentUser;
            }
            if (!canChangeToggleSensorPrivacy(userId, sensor)) {
                return;
            }
            setToggleSensorPrivacyUnchecked(1, userId, source, sensor, enable);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setToggleSensorPrivacyUnchecked(final int toggleType, final int userId, final int source, final int sensor, final boolean enable) {
            final long[] lastChange = new long[1];
            this.mSensorPrivacyStateController.atomic(new Runnable() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    SensorPrivacyService.SensorPrivacyServiceImpl.this.m6463xea02aae2(toggleType, userId, sensor, lastChange, enable, source);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setToggleSensorPrivacyUnchecked$2$com-android-server-sensorprivacy-SensorPrivacyService$SensorPrivacyServiceImpl  reason: not valid java name */
        public /* synthetic */ void m6463xea02aae2(int toggleType, final int userId, final int sensor, final long[] lastChange, final boolean enable, final int source) {
            SensorState sensorState = this.mSensorPrivacyStateController.getState(toggleType, userId, sensor);
            lastChange[0] = sensorState.getLastChange();
            this.mSensorPrivacyStateController.setState(toggleType, userId, sensor, enable, this.mHandler, new SensorPrivacyStateController.SetStateResultCallback() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl$$ExternalSyntheticLambda1
                @Override // com.android.server.sensorprivacy.SensorPrivacyStateController.SetStateResultCallback
                public final void callback(boolean z) {
                    SensorPrivacyService.SensorPrivacyServiceImpl.this.m6462xc0f4503(userId, source, sensor, enable, lastChange, z);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setToggleSensorPrivacyUnchecked$1$com-android-server-sensorprivacy-SensorPrivacyService$SensorPrivacyServiceImpl  reason: not valid java name */
        public /* synthetic */ void m6462xc0f4503(int userId, int source, int sensor, boolean enable, long[] lastChange, boolean changeSuccessful) {
            if (changeSuccessful && userId == SensorPrivacyService.this.mUserManagerInternal.getProfileParentId(userId)) {
                this.mHandler.sendMessage(PooledLambda.obtainMessage(new HexConsumer() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl$$ExternalSyntheticLambda4
                    public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
                        ((SensorPrivacyService.SensorPrivacyServiceImpl) obj).logSensorPrivacyToggle(((Integer) obj2).intValue(), ((Integer) obj3).intValue(), ((Boolean) obj4).booleanValue(), ((Long) obj5).longValue(), ((Boolean) obj6).booleanValue());
                    }
                }, this, Integer.valueOf(source), Integer.valueOf(sensor), Boolean.valueOf(enable), Long.valueOf(lastChange[0]), false));
            }
        }

        private boolean canChangeToggleSensorPrivacy(int userId, int sensor) {
            if (sensor == 1 && SensorPrivacyService.this.mCallStateHelper.isInEmergencyCall()) {
                Log.i(SensorPrivacyService.TAG, "Can't change mic toggle during an emergency call");
                return false;
            } else if (requiresAuthentication() && SensorPrivacyService.this.mKeyguardManager != null && SensorPrivacyService.this.mKeyguardManager.isDeviceLocked(userId)) {
                Log.i(SensorPrivacyService.TAG, "Can't change mic/cam toggle while device is locked");
                return false;
            } else if (sensor == 1 && SensorPrivacyService.this.mUserManagerInternal.getUserRestriction(userId, "disallow_microphone_toggle")) {
                Log.i(SensorPrivacyService.TAG, "Can't change mic toggle due to admin restriction");
                return false;
            } else if (sensor != 2 || !SensorPrivacyService.this.mUserManagerInternal.getUserRestriction(userId, "disallow_camera_toggle")) {
                return true;
            } else {
                Log.i(SensorPrivacyService.TAG, "Can't change camera toggle due to admin restriction");
                return false;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void logSensorPrivacyToggle(int source, int sensor, boolean enabled, long lastChange, boolean onShutDown) {
            int logAction;
            int logSensor;
            int logSource;
            long logMins = Math.max(0L, (SensorPrivacyService.getCurrentTimeMillis() - lastChange) / 60000);
            if (onShutDown) {
                if (enabled) {
                    logAction = 0;
                } else {
                    logAction = 0;
                }
            } else if (enabled) {
                logAction = 2;
            } else {
                logAction = 1;
            }
            switch (sensor) {
                case 1:
                    logSensor = 1;
                    break;
                case 2:
                    logSensor = 2;
                    break;
                default:
                    logSensor = 0;
                    break;
            }
            switch (source) {
                case 1:
                    logSource = 3;
                    break;
                case 2:
                    logSource = 2;
                    break;
                case 3:
                    logSource = 1;
                    break;
                default:
                    logSource = 0;
                    break;
            }
            FrameworkStatsLog.write((int) FrameworkStatsLog.PRIVACY_SENSOR_TOGGLE_INTERACTION, logSensor, logAction, logSource, logMins);
        }

        public void setToggleSensorPrivacyForProfileGroup(int userId, final int source, final int sensor, final boolean enable) {
            enforceManageSensorPrivacyPermission();
            if (userId == -2) {
                userId = SensorPrivacyService.this.mCurrentUser;
            }
            final int parentId = SensorPrivacyService.this.mUserManagerInternal.getProfileParentId(userId);
            SensorPrivacyService.this.forAllUsers(new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl$$ExternalSyntheticLambda7
                public final void acceptOrThrow(Object obj) {
                    SensorPrivacyService.SensorPrivacyServiceImpl.this.m6461xf8a04a50(parentId, source, sensor, enable, (Integer) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setToggleSensorPrivacyForProfileGroup$3$com-android-server-sensorprivacy-SensorPrivacyService$SensorPrivacyServiceImpl  reason: not valid java name */
        public /* synthetic */ void m6461xf8a04a50(int parentId, int source, int sensor, boolean enable, Integer userId2) throws Exception {
            if (parentId == SensorPrivacyService.this.mUserManagerInternal.getProfileParentId(userId2.intValue())) {
                setToggleSensorPrivacy(userId2.intValue(), source, sensor, enable);
            }
        }

        private void enforceManageSensorPrivacyPermission() {
            enforcePermission("android.permission.MANAGE_SENSOR_PRIVACY", "Changing sensor privacy requires the following permission: android.permission.MANAGE_SENSOR_PRIVACY");
        }

        private void enforceObserveSensorPrivacyPermission() {
            String systemUIPackage = SensorPrivacyService.this.mContext.getString(17039418);
            if (Binder.getCallingUid() == SensorPrivacyService.this.mPackageManagerInternal.getPackageUid(systemUIPackage, 1048576L, 0)) {
                return;
            }
            enforcePermission("android.permission.OBSERVE_SENSOR_PRIVACY", "Observing sensor privacy changes requires the following permission: android.permission.OBSERVE_SENSOR_PRIVACY");
        }

        private void enforcePermission(String permission, String message) {
            if (SensorPrivacyService.this.mContext.checkCallingOrSelfPermission(permission) == 0) {
                return;
            }
            throw new SecurityException(message);
        }

        public boolean isSensorPrivacyEnabled() {
            enforceObserveSensorPrivacyPermission();
            return this.mSensorPrivacyStateController.getAllSensorState();
        }

        public boolean isToggleSensorPrivacyEnabled(int toggleType, int sensor) {
            enforceObserveSensorPrivacyPermission();
            return this.mSensorPrivacyStateController.getState(toggleType, SensorPrivacyService.this.mCurrentUser, sensor).isEnabled();
        }

        public boolean isCombinedToggleSensorPrivacyEnabled(int sensor) {
            return isToggleSensorPrivacyEnabled(1, sensor) || isToggleSensorPrivacyEnabled(2, sensor);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isToggleSensorPrivacyEnabledInternal(int userId, int toggleType, int sensor) {
            return this.mSensorPrivacyStateController.getState(toggleType, userId, sensor).isEnabled();
        }

        public boolean supportsSensorToggle(int toggleType, int sensor) {
            if (toggleType == 1) {
                if (sensor == 1) {
                    return SensorPrivacyService.this.mContext.getResources().getBoolean(17891783);
                }
                if (sensor == 2) {
                    return SensorPrivacyService.this.mContext.getResources().getBoolean(17891778);
                }
            } else if (toggleType == 2) {
                if (sensor == 1) {
                    return SensorPrivacyService.this.mContext.getResources().getBoolean(17891781);
                }
                if (sensor == 2) {
                    return SensorPrivacyService.this.mContext.getResources().getBoolean(17891780);
                }
            }
            throw new IllegalArgumentException("Invalid arguments. toggleType=" + toggleType + " sensor=" + sensor);
        }

        public void addSensorPrivacyListener(ISensorPrivacyListener listener) {
            enforceObserveSensorPrivacyPermission();
            if (listener == null) {
                throw new NullPointerException("listener cannot be null");
            }
            this.mHandler.addListener(listener);
        }

        public void addToggleSensorPrivacyListener(ISensorPrivacyListener listener) {
            enforceObserveSensorPrivacyPermission();
            if (listener == null) {
                throw new IllegalArgumentException("listener cannot be null");
            }
            this.mHandler.addToggleListener(listener);
        }

        public void removeSensorPrivacyListener(ISensorPrivacyListener listener) {
            enforceObserveSensorPrivacyPermission();
            if (listener == null) {
                throw new NullPointerException("listener cannot be null");
            }
            this.mHandler.removeListener(listener);
        }

        public void removeToggleSensorPrivacyListener(ISensorPrivacyListener listener) {
            enforceObserveSensorPrivacyPermission();
            if (listener == null) {
                throw new IllegalArgumentException("listener cannot be null");
            }
            this.mHandler.removeToggleListener(listener);
        }

        public void suppressToggleSensorPrivacyReminders(int userId, int sensor, IBinder token, boolean suppress) {
            enforceManageSensorPrivacyPermission();
            if (userId == -2) {
                userId = SensorPrivacyService.this.mCurrentUser;
            }
            Objects.requireNonNull(token);
            Pair<Integer, UserHandle> key = new Pair<>(Integer.valueOf(sensor), UserHandle.of(userId));
            synchronized (this.mLock) {
                if (suppress) {
                    try {
                        token.linkToDeath(this, 0);
                        ArrayList<IBinder> suppressPackageReminderTokens = this.mSuppressReminders.get(key);
                        if (suppressPackageReminderTokens == null) {
                            suppressPackageReminderTokens = new ArrayList<>(1);
                            this.mSuppressReminders.put(key, suppressPackageReminderTokens);
                        }
                        suppressPackageReminderTokens.add(token);
                    } catch (RemoteException e) {
                        Log.e(SensorPrivacyService.TAG, "Could not suppress sensor use reminder", e);
                    }
                } else {
                    this.mHandler.removeSuppressPackageReminderToken(key, token);
                }
            }
        }

        public boolean requiresAuthentication() {
            enforceObserveSensorPrivacyPermission();
            return SensorPrivacyService.this.mContext.getResources().getBoolean(17891740);
        }

        public void showSensorUseDialog(int sensor) {
            if (Binder.getCallingUid() != 1000) {
                throw new SecurityException("Can only be called by the system uid");
            }
            if (!isCombinedToggleSensorPrivacyEnabled(sensor)) {
                return;
            }
            enqueueSensorUseReminderDialogAsync(-1, UserHandle.of(SensorPrivacyService.this.mCurrentUser), PackageManagerService.PLATFORM_PACKAGE_NAME, sensor);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void userSwitching(final int from, final int to) {
            int i;
            final boolean[] micState = new boolean[2];
            final boolean[] camState = new boolean[2];
            final boolean[] prevMicState = new boolean[2];
            final boolean[] prevCamState = new boolean[2];
            this.mSensorPrivacyStateController.atomic(new Runnable() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    SensorPrivacyService.SensorPrivacyServiceImpl.this.m6464x56368f3(prevMicState, from, prevCamState, micState, to, camState);
                }
            });
            this.mSensorPrivacyStateController.atomic(new Runnable() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    SensorPrivacyService.SensorPrivacyServiceImpl.this.m6465xe356ced2(prevMicState, from, prevCamState, micState, to, camState);
                }
            });
            boolean z = false;
            if (from != -10000 && prevMicState[0] == micState[0] && prevMicState[1] == micState[1]) {
                i = to;
            } else {
                i = to;
                this.mHandler.handleSensorPrivacyChanged(i, 1, 1, micState[0]);
                this.mHandler.handleSensorPrivacyChanged(i, 2, 1, micState[1]);
                setGlobalRestriction(1, micState[0] || micState[1]);
            }
            if (from == -10000 || prevCamState[0] != camState[0] || prevCamState[1] != camState[1]) {
                this.mHandler.handleSensorPrivacyChanged(i, 1, 2, camState[0]);
                this.mHandler.handleSensorPrivacyChanged(i, 2, 2, camState[1]);
                if (camState[0] || camState[1]) {
                    z = true;
                }
                setGlobalRestriction(2, z);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$userSwitching$4$com-android-server-sensorprivacy-SensorPrivacyService$SensorPrivacyServiceImpl  reason: not valid java name */
        public /* synthetic */ void m6464x56368f3(boolean[] prevMicState, int from, boolean[] prevCamState, boolean[] micState, int to, boolean[] camState) {
            prevMicState[0] = isToggleSensorPrivacyEnabledInternal(from, 1, 1);
            prevCamState[0] = isToggleSensorPrivacyEnabledInternal(from, 1, 2);
            micState[0] = isToggleSensorPrivacyEnabledInternal(to, 1, 1);
            camState[0] = isToggleSensorPrivacyEnabledInternal(to, 1, 2);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$userSwitching$5$com-android-server-sensorprivacy-SensorPrivacyService$SensorPrivacyServiceImpl  reason: not valid java name */
        public /* synthetic */ void m6465xe356ced2(boolean[] prevMicState, int from, boolean[] prevCamState, boolean[] micState, int to, boolean[] camState) {
            prevMicState[1] = isToggleSensorPrivacyEnabledInternal(from, 2, 1);
            prevCamState[1] = isToggleSensorPrivacyEnabledInternal(from, 2, 2);
            micState[1] = isToggleSensorPrivacyEnabledInternal(to, 2, 1);
            camState[1] = isToggleSensorPrivacyEnabledInternal(to, 2, 2);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setGlobalRestriction(int sensor, boolean enabled) {
            switch (sensor) {
                case 1:
                    SensorPrivacyService.this.mAppOpsManagerInternal.setGlobalRestriction(27, enabled, SensorPrivacyService.this.mAppOpsRestrictionToken);
                    SensorPrivacyService.this.mAppOpsManagerInternal.setGlobalRestriction(100, enabled, SensorPrivacyService.this.mAppOpsRestrictionToken);
                    SensorPrivacyService.this.mAppOpsManagerInternal.setGlobalRestriction(120, enabled, SensorPrivacyService.this.mAppOpsRestrictionToken);
                    return;
                case 2:
                    SensorPrivacyService.this.mAppOpsManagerInternal.setGlobalRestriction(26, enabled, SensorPrivacyService.this.mAppOpsRestrictionToken);
                    SensorPrivacyService.this.mAppOpsManagerInternal.setGlobalRestriction(101, enabled, SensorPrivacyService.this.mAppOpsRestrictionToken);
                    return;
                default:
                    return;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void removeSuppressPackageReminderToken(Pair<Integer, UserHandle> key, IBinder token) {
            synchronized (this.mLock) {
                ArrayList<IBinder> suppressPackageReminderTokens = this.mSuppressReminders.get(key);
                if (suppressPackageReminderTokens == null) {
                    Log.e(SensorPrivacyService.TAG, "No tokens for " + key);
                    return;
                }
                boolean wasRemoved = suppressPackageReminderTokens.remove(token);
                if (wasRemoved) {
                    token.unlinkToDeath(this, 0);
                    if (suppressPackageReminderTokens.isEmpty()) {
                        this.mSuppressReminders.remove(key);
                    }
                } else {
                    Log.w(SensorPrivacyService.TAG, "Could not remove sensor use reminder suppression token " + token + " from " + key);
                }
            }
        }

        public void binderDied(IBinder token) {
            synchronized (this.mLock) {
                for (Pair<Integer, UserHandle> key : this.mSuppressReminders.keySet()) {
                    removeSuppressPackageReminderToken(key, token);
                }
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            String opt;
            Objects.requireNonNull(fd);
            if (DumpUtils.checkDumpPermission(SensorPrivacyService.this.mContext, SensorPrivacyService.TAG, pw)) {
                int opti = 0;
                boolean dumpAsProto = false;
                while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == '-') {
                    opti++;
                    if ("--proto".equals(opt)) {
                        dumpAsProto = true;
                    } else {
                        pw.println("Unknown argument: " + opt + "; use -h for help");
                    }
                }
                long identity = Binder.clearCallingIdentity();
                try {
                    if (dumpAsProto) {
                        this.mSensorPrivacyStateController.dump(new DualDumpOutputStream(new ProtoOutputStream(fd)));
                    } else {
                        pw.println("SENSOR PRIVACY MANAGER STATE (dumpsys sensor_privacy)");
                        this.mSensorPrivacyStateController.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")));
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int sensorStrToId(String sensor) {
            if (sensor == null) {
                return 0;
            }
            char c = 65535;
            switch (sensor.hashCode()) {
                case -1367751899:
                    if (sensor.equals("camera")) {
                        c = 1;
                        break;
                    }
                    break;
                case 1370921258:
                    if (sensor.equals("microphone")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    return 1;
                case 1:
                    return 2;
                default:
                    return 0;
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyServiceImpl$3] */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new ShellCommand() { // from class: com.android.server.sensorprivacy.SensorPrivacyService.SensorPrivacyServiceImpl.3
                /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
                public int onCommand(String cmd) {
                    boolean z;
                    if (cmd == null) {
                        return handleDefaultCommands(cmd);
                    }
                    int userId = Integer.parseInt(getNextArgRequired());
                    PrintWriter pw = getOutPrintWriter();
                    switch (cmd.hashCode()) {
                        case -1298848381:
                            if (cmd.equals("enable")) {
                                z = false;
                                break;
                            }
                            z = true;
                            break;
                        case 1671308008:
                            if (cmd.equals("disable")) {
                                z = true;
                                break;
                            }
                            z = true;
                            break;
                        default:
                            z = true;
                            break;
                    }
                    switch (z) {
                        case false:
                            int sensor = SensorPrivacyServiceImpl.this.sensorStrToId(getNextArgRequired());
                            if (sensor == 0) {
                                pw.println("Invalid sensor");
                                return -1;
                            }
                            SensorPrivacyServiceImpl.this.setToggleSensorPrivacy(userId, 4, sensor, true);
                            break;
                        case true:
                            int sensor2 = SensorPrivacyServiceImpl.this.sensorStrToId(getNextArgRequired());
                            if (sensor2 == 0) {
                                pw.println("Invalid sensor");
                                return -1;
                            }
                            SensorPrivacyServiceImpl.this.setToggleSensorPrivacy(userId, 4, sensor2, false);
                            break;
                        default:
                            return handleDefaultCommands(cmd);
                    }
                    return 0;
                }

                public void onHelp() {
                    PrintWriter pw = getOutPrintWriter();
                    pw.println("Sensor privacy manager (sensor_privacy) commands:");
                    pw.println("  help");
                    pw.println("    Print this help text.");
                    pw.println("");
                    pw.println("  enable USER_ID SENSOR");
                    pw.println("    Enable privacy for a certain sensor.");
                    pw.println("");
                    pw.println("  disable USER_ID SENSOR");
                    pw.println("    Disable privacy for a certain sensor.");
                    pw.println("");
                }
            }.exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class SensorPrivacyHandler extends Handler {
        private static final int MESSAGE_SENSOR_PRIVACY_CHANGED = 1;
        private final Context mContext;
        private final ArrayMap<ISensorPrivacyListener, Pair<DeathRecipient, Integer>> mDeathRecipients;
        private final Object mListenerLock;
        private final RemoteCallbackList<ISensorPrivacyListener> mListeners;
        private final RemoteCallbackList<ISensorPrivacyListener> mToggleSensorListeners;

        SensorPrivacyHandler(Looper looper, Context context) {
            super(looper);
            this.mListenerLock = new Object();
            this.mListeners = new RemoteCallbackList<>();
            this.mToggleSensorListeners = new RemoteCallbackList<>();
            this.mDeathRecipients = new ArrayMap<>();
            this.mContext = context;
        }

        public void addListener(ISensorPrivacyListener listener) {
            synchronized (this.mListenerLock) {
                if (this.mListeners.register(listener)) {
                    addDeathRecipient(listener);
                }
            }
        }

        public void addToggleListener(ISensorPrivacyListener listener) {
            synchronized (this.mListenerLock) {
                if (this.mToggleSensorListeners.register(listener)) {
                    addDeathRecipient(listener);
                }
            }
        }

        public void removeListener(ISensorPrivacyListener listener) {
            synchronized (this.mListenerLock) {
                if (this.mListeners.unregister(listener)) {
                    removeDeathRecipient(listener);
                }
            }
        }

        public void removeToggleListener(ISensorPrivacyListener listener) {
            synchronized (this.mListenerLock) {
                if (this.mToggleSensorListeners.unregister(listener)) {
                    removeDeathRecipient(listener);
                }
            }
        }

        public void handleSensorPrivacyChanged(boolean enabled) {
            int count = this.mListeners.beginBroadcast();
            for (int i = 0; i < count; i++) {
                ISensorPrivacyListener listener = this.mListeners.getBroadcastItem(i);
                try {
                    listener.onSensorPrivacyChanged(-1, -1, enabled);
                } catch (RemoteException e) {
                    Log.e(SensorPrivacyService.TAG, "Caught an exception notifying listener " + listener + ": ", e);
                }
            }
            this.mListeners.finishBroadcast();
        }

        public void handleSensorPrivacyChanged(int userId, int toggleType, int sensor, boolean enabled) {
            SensorPrivacyService.this.mSensorPrivacyManagerInternal.dispatch(userId, sensor, enabled);
            if (userId == SensorPrivacyService.this.mCurrentUser) {
                SensorPrivacyService.this.mSensorPrivacyServiceImpl.setGlobalRestriction(sensor, SensorPrivacyService.this.mSensorPrivacyServiceImpl.isCombinedToggleSensorPrivacyEnabled(sensor));
            }
            if (userId != SensorPrivacyService.this.mCurrentUser) {
                return;
            }
            synchronized (this.mListenerLock) {
                int count = this.mToggleSensorListeners.beginBroadcast();
                for (int i = 0; i < count; i++) {
                    ISensorPrivacyListener listener = this.mToggleSensorListeners.getBroadcastItem(i);
                    try {
                        listener.onSensorPrivacyChanged(toggleType, sensor, enabled);
                    } catch (RemoteException e) {
                        Log.e(SensorPrivacyService.TAG, "Caught an exception notifying listener " + listener + ": ", e);
                    }
                }
                this.mToggleSensorListeners.finishBroadcast();
            }
        }

        public void removeSuppressPackageReminderToken(Pair<Integer, UserHandle> key, IBinder token) {
            sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyHandler$$ExternalSyntheticLambda0
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((SensorPrivacyService.SensorPrivacyServiceImpl) obj).removeSuppressPackageReminderToken((Pair) obj2, (IBinder) obj3);
                }
            }, SensorPrivacyService.this.mSensorPrivacyServiceImpl, key, token));
        }

        /* JADX DEBUG: Multi-variable search result rejected for r1v2, resolved type: android.util.ArrayMap<android.hardware.ISensorPrivacyListener, android.util.Pair<com.android.server.sensorprivacy.SensorPrivacyService$DeathRecipient, java.lang.Integer>> */
        /* JADX WARN: Multi-variable type inference failed */
        private void addDeathRecipient(ISensorPrivacyListener listener) {
            Pair<DeathRecipient, Integer> deathRecipient;
            Pair<DeathRecipient, Integer> deathRecipient2 = this.mDeathRecipients.get(listener);
            if (deathRecipient2 != null) {
                int newRefCount = ((Integer) deathRecipient2.second).intValue() + 1;
                deathRecipient = new Pair<>((DeathRecipient) deathRecipient2.first, Integer.valueOf(newRefCount));
            } else {
                deathRecipient = new Pair<>(new DeathRecipient(listener), 1);
            }
            this.mDeathRecipients.put(listener, deathRecipient);
        }

        private void removeDeathRecipient(ISensorPrivacyListener listener) {
            Pair<DeathRecipient, Integer> deathRecipient = this.mDeathRecipients.get(listener);
            if (deathRecipient == null) {
                return;
            }
            int newRefCount = ((Integer) deathRecipient.second).intValue() - 1;
            if (newRefCount == 0) {
                this.mDeathRecipients.remove(listener);
                ((DeathRecipient) deathRecipient.first).destroy();
                return;
            }
            this.mDeathRecipients.put(listener, new Pair<>((DeathRecipient) deathRecipient.first, Integer.valueOf(newRefCount)));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class DeathRecipient implements IBinder.DeathRecipient {
        private ISensorPrivacyListener mListener;

        DeathRecipient(ISensorPrivacyListener listener) {
            this.mListener = listener;
            try {
                listener.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            SensorPrivacyService.this.mSensorPrivacyServiceImpl.removeSensorPrivacyListener(this.mListener);
        }

        public void destroy() {
            try {
                this.mListener.asBinder().unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void forAllUsers(FunctionalUtils.ThrowingConsumer<Integer> c) {
        int[] userIds = this.mUserManagerInternal.getUserIds();
        for (int i : userIds) {
            c.accept(Integer.valueOf(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class SensorPrivacyManagerInternalImpl extends SensorPrivacyManagerInternal {
        private ArrayMap<Integer, ArraySet<SensorPrivacyManagerInternal.OnUserSensorPrivacyChangedListener>> mAllUserListeners;
        private ArrayMap<Integer, ArrayMap<Integer, ArraySet<SensorPrivacyManagerInternal.OnSensorPrivacyChangedListener>>> mListeners;
        private final Object mLock;

        private SensorPrivacyManagerInternalImpl() {
            this.mListeners = new ArrayMap<>();
            this.mAllUserListeners = new ArrayMap<>();
            this.mLock = new Object();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dispatch(final int userId, int sensor, final boolean enabled) {
            ArraySet<SensorPrivacyManagerInternal.OnSensorPrivacyChangedListener> sensorListeners;
            synchronized (this.mLock) {
                ArraySet<SensorPrivacyManagerInternal.OnUserSensorPrivacyChangedListener> allUserSensorListeners = this.mAllUserListeners.get(Integer.valueOf(sensor));
                if (allUserSensorListeners != null) {
                    for (int i = 0; i < allUserSensorListeners.size(); i++) {
                        final SensorPrivacyManagerInternal.OnUserSensorPrivacyChangedListener listener = allUserSensorListeners.valueAt(i);
                        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyManagerInternalImpl$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                listener.onSensorPrivacyChanged(userId, enabled);
                            }
                        });
                    }
                }
                ArrayMap<Integer, ArraySet<SensorPrivacyManagerInternal.OnSensorPrivacyChangedListener>> userSensorListeners = this.mListeners.get(Integer.valueOf(userId));
                if (userSensorListeners != null && (sensorListeners = userSensorListeners.get(Integer.valueOf(sensor))) != null) {
                    for (int i2 = 0; i2 < sensorListeners.size(); i2++) {
                        final SensorPrivacyManagerInternal.OnSensorPrivacyChangedListener listener2 = sensorListeners.valueAt(i2);
                        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.sensorprivacy.SensorPrivacyService$SensorPrivacyManagerInternalImpl$$ExternalSyntheticLambda1
                            @Override // java.lang.Runnable
                            public final void run() {
                                listener2.onSensorPrivacyChanged(enabled);
                            }
                        });
                    }
                }
            }
        }

        public boolean isSensorPrivacyEnabled(int userId, int sensor) {
            return SensorPrivacyService.this.mSensorPrivacyServiceImpl.isToggleSensorPrivacyEnabledInternal(userId, 1, sensor);
        }

        public void addSensorPrivacyListener(int userId, int sensor, SensorPrivacyManagerInternal.OnSensorPrivacyChangedListener listener) {
            synchronized (this.mLock) {
                ArrayMap<Integer, ArraySet<SensorPrivacyManagerInternal.OnSensorPrivacyChangedListener>> userSensorListeners = this.mListeners.get(Integer.valueOf(userId));
                if (userSensorListeners == null) {
                    userSensorListeners = new ArrayMap<>();
                    this.mListeners.put(Integer.valueOf(userId), userSensorListeners);
                }
                ArraySet<SensorPrivacyManagerInternal.OnSensorPrivacyChangedListener> sensorListeners = userSensorListeners.get(Integer.valueOf(sensor));
                if (sensorListeners == null) {
                    sensorListeners = new ArraySet<>();
                    userSensorListeners.put(Integer.valueOf(sensor), sensorListeners);
                }
                sensorListeners.add(listener);
            }
        }

        public void addSensorPrivacyListenerForAllUsers(int sensor, SensorPrivacyManagerInternal.OnUserSensorPrivacyChangedListener listener) {
            synchronized (this.mLock) {
                ArraySet<SensorPrivacyManagerInternal.OnUserSensorPrivacyChangedListener> sensorListeners = this.mAllUserListeners.get(Integer.valueOf(sensor));
                if (sensorListeners == null) {
                    sensorListeners = new ArraySet<>();
                    this.mAllUserListeners.put(Integer.valueOf(sensor), sensorListeners);
                }
                sensorListeners.add(listener);
            }
        }

        public void setPhysicalToggleSensorPrivacy(int userId, int sensor, boolean enable) {
            SensorPrivacyServiceImpl sps = SensorPrivacyService.this.mSensorPrivacyServiceImpl;
            int userId2 = userId == -2 ? SensorPrivacyService.this.mCurrentUser : userId;
            int realUserId = userId2 == -10000 ? SensorPrivacyService.this.mContext.getUserId() : userId2;
            sps.setToggleSensorPrivacyUnchecked(2, realUserId, 5, sensor, enable);
            if (!enable) {
                sps.setToggleSensorPrivacyUnchecked(1, realUserId, 5, sensor, enable);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class CallStateHelper {
        private boolean mIsInEmergencyCall;
        private boolean mMicUnmutedForEmergencyCall;
        private Object mCallStateLock = new Object();
        private OutgoingEmergencyStateCallback mEmergencyStateCallback = new OutgoingEmergencyStateCallback();
        private CallStateCallback mCallStateCallback = new CallStateCallback();

        CallStateHelper() {
            SensorPrivacyService.this.mTelephonyManager.registerTelephonyCallback(FgThread.getExecutor(), this.mEmergencyStateCallback);
            SensorPrivacyService.this.mTelephonyManager.registerTelephonyCallback(FgThread.getExecutor(), this.mCallStateCallback);
        }

        boolean isInEmergencyCall() {
            boolean z;
            synchronized (this.mCallStateLock) {
                z = this.mIsInEmergencyCall;
            }
            return z;
        }

        /* loaded from: classes2.dex */
        private class OutgoingEmergencyStateCallback extends TelephonyCallback implements TelephonyCallback.OutgoingEmergencyCallListener {
            private OutgoingEmergencyStateCallback() {
            }

            public void onOutgoingEmergencyCall(EmergencyNumber placedEmergencyNumber, int subscriptionId) {
                CallStateHelper.this.onEmergencyCall();
            }
        }

        /* loaded from: classes2.dex */
        private class CallStateCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {
            private CallStateCallback() {
            }

            @Override // android.telephony.TelephonyCallback.CallStateListener
            public void onCallStateChanged(int state) {
                if (state == 0) {
                    CallStateHelper.this.onCallOver();
                } else {
                    CallStateHelper.this.onCall();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onEmergencyCall() {
            synchronized (this.mCallStateLock) {
                if (!this.mIsInEmergencyCall) {
                    this.mIsInEmergencyCall = true;
                    if (SensorPrivacyService.this.mSensorPrivacyServiceImpl.isToggleSensorPrivacyEnabled(1, 1)) {
                        SensorPrivacyService.this.mSensorPrivacyServiceImpl.setToggleSensorPrivacyUnchecked(1, SensorPrivacyService.this.mCurrentUser, 5, 1, false);
                        this.mMicUnmutedForEmergencyCall = true;
                    } else {
                        this.mMicUnmutedForEmergencyCall = false;
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onCall() {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (this.mCallStateLock) {
                    SensorPrivacyService.this.mSensorPrivacyServiceImpl.showSensorUseDialog(1);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onCallOver() {
            synchronized (this.mCallStateLock) {
                if (this.mIsInEmergencyCall) {
                    this.mIsInEmergencyCall = false;
                    if (this.mMicUnmutedForEmergencyCall) {
                        SensorPrivacyService.this.mSensorPrivacyServiceImpl.setToggleSensorPrivacyUnchecked(1, SensorPrivacyService.this.mCurrentUser, 5, 1, true);
                        this.mMicUnmutedForEmergencyCall = false;
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static long getCurrentTimeMillis() {
        return SystemClock.elapsedRealtime();
    }
}
