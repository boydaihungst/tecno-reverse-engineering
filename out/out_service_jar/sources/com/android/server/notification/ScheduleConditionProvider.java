package com.android.server.notification;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.os.Binder;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.service.notification.ScheduleCalendar;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.notification.NotificationManagerService;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
/* loaded from: classes2.dex */
public class ScheduleConditionProvider extends SystemConditionProviderService {
    private static final String ACTION_EVALUATE;
    private static final String EXTRA_TIME = "time";
    private static final String NOT_SHOWN = "...";
    private static final int REQUEST_CODE_EVALUATE = 1;
    private static final String SCP_SETTING = "snoozed_schedule_condition_provider";
    private static final String SEPARATOR = ";";
    private static final String SIMPLE_NAME;
    static final String TAG = "ConditionProviders.SCP";
    private AlarmManager mAlarmManager;
    private boolean mConnected;
    private long mNextAlarmTime;
    private boolean mRegistered;
    static final boolean DEBUG = true;
    public static final ComponentName COMPONENT = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, ScheduleConditionProvider.class.getName());
    private final Context mContext = this;
    private final ArrayMap<Uri, ScheduleCalendar> mSubscriptions = new ArrayMap<>();
    private ArraySet<Uri> mSnoozedForAlarm = new ArraySet<>();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.notification.ScheduleConditionProvider.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (ScheduleConditionProvider.DEBUG) {
                Slog.d(ScheduleConditionProvider.TAG, "onReceive " + intent.getAction());
            }
            if ("android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())) {
                synchronized (ScheduleConditionProvider.this.mSubscriptions) {
                    for (Uri conditionId : ScheduleConditionProvider.this.mSubscriptions.keySet()) {
                        ScheduleCalendar cal = (ScheduleCalendar) ScheduleConditionProvider.this.mSubscriptions.get(conditionId);
                        if (cal != null) {
                            cal.setTimeZone(Calendar.getInstance().getTimeZone());
                        }
                    }
                }
            }
            ScheduleConditionProvider.this.evaluateSubscriptions();
        }
    };

    static {
        String simpleName = ScheduleConditionProvider.class.getSimpleName();
        SIMPLE_NAME = simpleName;
        ACTION_EVALUATE = simpleName + ".EVALUATE";
    }

    public ScheduleConditionProvider() {
        if (DEBUG) {
            Slog.d(TAG, "new " + SIMPLE_NAME + "()");
        }
    }

    @Override // com.android.server.notification.SystemConditionProviderService
    public ComponentName getComponent() {
        return COMPONENT;
    }

    @Override // com.android.server.notification.SystemConditionProviderService
    public boolean isValidConditionId(Uri id) {
        return ZenModeConfig.isValidScheduleConditionId(id);
    }

    @Override // com.android.server.notification.SystemConditionProviderService
    public void dump(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        pw.print("    ");
        pw.print(SIMPLE_NAME);
        pw.println(":");
        pw.print("      mConnected=");
        pw.println(this.mConnected);
        pw.print("      mRegistered=");
        pw.println(this.mRegistered);
        pw.println("      mSubscriptions=");
        long now = System.currentTimeMillis();
        synchronized (this.mSubscriptions) {
            for (Uri conditionId : this.mSubscriptions.keySet()) {
                pw.print("        ");
                pw.print(meetsSchedule(this.mSubscriptions.get(conditionId), now) ? "* " : "  ");
                pw.println(conditionId);
                pw.print("            ");
                pw.println(this.mSubscriptions.get(conditionId).toString());
            }
        }
        pw.println("      snoozed due to alarm: " + TextUtils.join(SEPARATOR, this.mSnoozedForAlarm));
        dumpUpcomingTime(pw, "mNextAlarmTime", this.mNextAlarmTime, now);
    }

    @Override // android.service.notification.ConditionProviderService
    public void onConnected() {
        if (DEBUG) {
            Slog.d(TAG, "onConnected");
        }
        this.mConnected = true;
        readSnoozed();
    }

    @Override // com.android.server.notification.SystemConditionProviderService
    public void onBootComplete() {
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) {
            Slog.d(TAG, "onDestroy");
        }
        this.mConnected = false;
    }

    @Override // android.service.notification.ConditionProviderService
    public void onSubscribe(Uri conditionId) {
        if (DEBUG) {
            Slog.d(TAG, "onSubscribe " + conditionId);
        }
        if (!ZenModeConfig.isValidScheduleConditionId(conditionId)) {
            notifyCondition(createCondition(conditionId, 3, "invalidId"));
            return;
        }
        synchronized (this.mSubscriptions) {
            this.mSubscriptions.put(conditionId, ZenModeConfig.toScheduleCalendar(conditionId));
        }
        evaluateSubscriptions();
    }

    @Override // android.service.notification.ConditionProviderService
    public void onUnsubscribe(Uri conditionId) {
        if (DEBUG) {
            Slog.d(TAG, "onUnsubscribe " + conditionId);
        }
        synchronized (this.mSubscriptions) {
            this.mSubscriptions.remove(conditionId);
        }
        removeSnoozed(conditionId);
        evaluateSubscriptions();
    }

    @Override // com.android.server.notification.SystemConditionProviderService
    public void attachBase(Context base) {
        attachBaseContext(base);
    }

    @Override // com.android.server.notification.SystemConditionProviderService
    public IConditionProvider asInterface() {
        return onBind(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void evaluateSubscriptions() {
        if (this.mAlarmManager == null) {
            this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        }
        long now = System.currentTimeMillis();
        this.mNextAlarmTime = 0L;
        long nextUserAlarmTime = getNextAlarm();
        List<Condition> conditionsToNotify = new ArrayList<>();
        synchronized (this.mSubscriptions) {
            setRegistered(!this.mSubscriptions.isEmpty());
            for (Uri conditionId : this.mSubscriptions.keySet()) {
                Condition condition = evaluateSubscriptionLocked(conditionId, this.mSubscriptions.get(conditionId), now, nextUserAlarmTime);
                if (condition != null) {
                    conditionsToNotify.add(condition);
                }
            }
        }
        notifyConditions((Condition[]) conditionsToNotify.toArray(new Condition[conditionsToNotify.size()]));
        updateAlarm(now, this.mNextAlarmTime);
    }

    Condition evaluateSubscriptionLocked(Uri conditionId, ScheduleCalendar cal, long now, long nextUserAlarmTime) {
        Condition condition;
        if (DEBUG) {
            Slog.d(TAG, String.format("evaluateSubscriptionLocked cal=%s, now=%s, nextUserAlarmTime=%s", cal, ts(now), ts(nextUserAlarmTime)));
        }
        if (cal == null) {
            Condition condition2 = createCondition(conditionId, 3, "!invalidId");
            removeSnoozed(conditionId);
            return condition2;
        }
        if (cal.isInSchedule(now)) {
            if (conditionSnoozed(conditionId)) {
                condition = createCondition(conditionId, 0, "snoozed");
            } else if (cal.shouldExitForAlarm(now)) {
                condition = createCondition(conditionId, 0, "alarmCanceled");
                addSnoozed(conditionId);
            } else {
                condition = createCondition(conditionId, 1, "meetsSchedule");
            }
        } else {
            condition = createCondition(conditionId, 0, "!meetsSchedule");
            removeSnoozed(conditionId);
        }
        cal.maybeSetNextAlarm(now, nextUserAlarmTime);
        long nextChangeTime = cal.getNextChangeTime(now);
        if (nextChangeTime > 0 && nextChangeTime > now) {
            long j = this.mNextAlarmTime;
            if (j == 0 || nextChangeTime < j) {
                this.mNextAlarmTime = nextChangeTime;
            }
        }
        return condition;
    }

    private void updateAlarm(long now, long time) {
        AlarmManager alarms = (AlarmManager) this.mContext.getSystemService("alarm");
        PendingIntent pendingIntent = getPendingIntent(time);
        alarms.cancel(pendingIntent);
        if (time > now) {
            if (DEBUG) {
                Slog.d(TAG, String.format("Scheduling evaluate for %s, in %s, now=%s", ts(time), formatDuration(time - now), ts(now)));
            }
            alarms.setExact(0, time, pendingIntent);
        } else if (DEBUG) {
            Slog.d(TAG, "Not scheduling evaluate");
        }
    }

    PendingIntent getPendingIntent(long time) {
        return PendingIntent.getBroadcast(this.mContext, 1, new Intent(ACTION_EVALUATE).setPackage(PackageManagerService.PLATFORM_PACKAGE_NAME).addFlags(268435456).putExtra(EXTRA_TIME, time), AudioFormat.DTS_HD);
    }

    public long getNextAlarm() {
        AlarmManager.AlarmClockInfo info = this.mAlarmManager.getNextAlarmClock(ActivityManager.getCurrentUser());
        if (info != null) {
            return info.getTriggerTime();
        }
        return 0L;
    }

    private boolean meetsSchedule(ScheduleCalendar cal, long time) {
        return cal != null && cal.isInSchedule(time);
    }

    private void setRegistered(boolean registered) {
        if (this.mRegistered == registered) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "setRegistered " + registered);
        }
        this.mRegistered = registered;
        if (registered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.TIME_SET");
            filter.addAction("android.intent.action.TIMEZONE_CHANGED");
            filter.addAction(ACTION_EVALUATE);
            filter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
            registerReceiver(this.mReceiver, filter, 2);
            return;
        }
        unregisterReceiver(this.mReceiver);
    }

    private Condition createCondition(Uri id, int state, String reason) {
        if (DEBUG) {
            Slog.d(TAG, "notifyCondition " + id + " " + Condition.stateToString(state) + " reason=" + reason);
        }
        return new Condition(id, NOT_SHOWN, NOT_SHOWN, NOT_SHOWN, 0, state, 2);
    }

    private boolean conditionSnoozed(Uri conditionId) {
        boolean contains;
        synchronized (this.mSnoozedForAlarm) {
            contains = this.mSnoozedForAlarm.contains(conditionId);
        }
        return contains;
    }

    void addSnoozed(Uri conditionId) {
        synchronized (this.mSnoozedForAlarm) {
            this.mSnoozedForAlarm.add(conditionId);
            saveSnoozedLocked();
        }
    }

    private void removeSnoozed(Uri conditionId) {
        synchronized (this.mSnoozedForAlarm) {
            this.mSnoozedForAlarm.remove(conditionId);
            saveSnoozedLocked();
        }
    }

    private void saveSnoozedLocked() {
        String setting = TextUtils.join(SEPARATOR, this.mSnoozedForAlarm);
        int currentUser = ActivityManager.getCurrentUser();
        Settings.Secure.putStringForUser(this.mContext.getContentResolver(), SCP_SETTING, setting, currentUser);
    }

    private void readSnoozed() {
        synchronized (this.mSnoozedForAlarm) {
            long identity = Binder.clearCallingIdentity();
            String setting = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), SCP_SETTING, ActivityManager.getCurrentUser());
            if (setting != null) {
                String[] tokens = setting.split(SEPARATOR);
                for (int i = 0; i < tokens.length; i++) {
                    String token = tokens[i];
                    if (token != null) {
                        token = token.trim();
                    }
                    if (!TextUtils.isEmpty(token)) {
                        this.mSnoozedForAlarm.add(Uri.parse(token));
                    }
                }
            }
            Binder.restoreCallingIdentity(identity);
        }
    }
}
