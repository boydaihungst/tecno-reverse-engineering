package com.android.server.notification;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.util.NotificationMessagingUtil;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
/* loaded from: classes2.dex */
public class ZenModeFiltering {
    private static final boolean DEBUG = ZenModeHelper.DEBUG;
    static final RepeatCallers REPEAT_CALLERS = new RepeatCallers();
    private static final String TAG = "ZenModeHelper";
    private final Context mContext;
    private ComponentName mDefaultPhoneApp;
    private final NotificationMessagingUtil mMessagingUtil;

    public ZenModeFiltering(Context context) {
        this.mContext = context;
        this.mMessagingUtil = new NotificationMessagingUtil(context);
    }

    public ZenModeFiltering(Context context, NotificationMessagingUtil messagingUtil) {
        this.mContext = context;
        this.mMessagingUtil = messagingUtil;
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mDefaultPhoneApp=");
        pw.println(this.mDefaultPhoneApp);
        pw.print(prefix);
        pw.print("RepeatCallers.mThresholdMinutes=");
        RepeatCallers repeatCallers = REPEAT_CALLERS;
        pw.println(repeatCallers.mThresholdMinutes);
        synchronized (repeatCallers) {
            if (!repeatCallers.mTelCalls.isEmpty()) {
                pw.print(prefix);
                pw.println("RepeatCallers.mTelCalls=");
                int i = 0;
                while (true) {
                    RepeatCallers repeatCallers2 = REPEAT_CALLERS;
                    if (i >= repeatCallers2.mTelCalls.size()) {
                        break;
                    }
                    pw.print(prefix);
                    pw.print("  ");
                    pw.print((String) repeatCallers2.mTelCalls.keyAt(i));
                    pw.print(" at ");
                    pw.println(ts(((Long) repeatCallers2.mTelCalls.valueAt(i)).longValue()));
                    i++;
                }
            }
            if (!REPEAT_CALLERS.mOtherCalls.isEmpty()) {
                pw.print(prefix);
                pw.println("RepeatCallers.mOtherCalls=");
                int i2 = 0;
                while (true) {
                    RepeatCallers repeatCallers3 = REPEAT_CALLERS;
                    if (i2 >= repeatCallers3.mOtherCalls.size()) {
                        break;
                    }
                    pw.print(prefix);
                    pw.print("  ");
                    pw.print((String) repeatCallers3.mOtherCalls.keyAt(i2));
                    pw.print(" at ");
                    pw.println(ts(((Long) repeatCallers3.mOtherCalls.valueAt(i2)).longValue()));
                    i2++;
                }
            }
        }
    }

    private static String ts(long time) {
        return new Date(time) + " (" + time + ")";
    }

    public static boolean matchesCallFilter(Context context, int zen, NotificationManager.Policy consolidatedPolicy, UserHandle userHandle, Bundle extras, ValidateNotificationPeople validator, int contactsTimeoutMs, float timeoutAffinity) {
        if (zen == 2) {
            ZenLog.traceMatchesCallFilter(false, "no interruptions");
            return false;
        } else if (zen == 3) {
            ZenLog.traceMatchesCallFilter(false, "alarms only");
            return false;
        } else {
            if (zen == 1) {
                if (consolidatedPolicy.allowRepeatCallers() && REPEAT_CALLERS.isRepeat(context, extras, null)) {
                    ZenLog.traceMatchesCallFilter(true, "repeat caller");
                    return true;
                } else if (!consolidatedPolicy.allowCalls()) {
                    ZenLog.traceMatchesCallFilter(false, "calls not allowed");
                    return false;
                } else if (validator != null) {
                    float contactAffinity = validator.getContactAffinity(userHandle, extras, contactsTimeoutMs, timeoutAffinity);
                    boolean match = audienceMatches(consolidatedPolicy.allowCallsFrom(), contactAffinity);
                    ZenLog.traceMatchesCallFilter(match, "contact affinity " + contactAffinity);
                    return match;
                }
            }
            ZenLog.traceMatchesCallFilter(true, "no restrictions");
            return true;
        }
    }

    private static Bundle extras(NotificationRecord record) {
        if (record == null || record.getSbn() == null || record.getSbn().getNotification() == null) {
            return null;
        }
        return record.getSbn().getNotification().extras;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void recordCall(NotificationRecord record) {
        REPEAT_CALLERS.recordCall(this.mContext, extras(record), record.getPhoneNumbers());
    }

    public boolean shouldIntercept(int zen, NotificationManager.Policy policy, NotificationRecord record) {
        if (zen == 0 || isCritical(record)) {
            return false;
        }
        if (NotificationManager.Policy.areAllVisualEffectsSuppressed(policy.suppressedVisualEffects) && PackageManagerService.PLATFORM_PACKAGE_NAME.equals(record.getSbn().getPackageName()) && 48 == record.getSbn().getId()) {
            ZenLog.traceNotIntercepted(record, "systemDndChangedNotification");
            return false;
        }
        switch (zen) {
            case 1:
                if (record.getPackagePriority() == 2) {
                    ZenLog.traceNotIntercepted(record, "priorityApp");
                    return false;
                } else if (isAlarm(record)) {
                    if (policy.allowAlarms()) {
                        return false;
                    }
                    ZenLog.traceIntercepted(record, "!allowAlarms");
                    return true;
                } else if (isEvent(record)) {
                    if (policy.allowEvents()) {
                        return false;
                    }
                    ZenLog.traceIntercepted(record, "!allowEvents");
                    return true;
                } else if (isReminder(record)) {
                    if (policy.allowReminders()) {
                        return false;
                    }
                    ZenLog.traceIntercepted(record, "!allowReminders");
                    return true;
                } else if (isMedia(record)) {
                    if (policy.allowMedia()) {
                        return false;
                    }
                    ZenLog.traceIntercepted(record, "!allowMedia");
                    return true;
                } else if (isSystem(record)) {
                    if (policy.allowSystem()) {
                        return false;
                    }
                    ZenLog.traceIntercepted(record, "!allowSystem");
                    return true;
                } else {
                    if (isConversation(record) && policy.allowConversations()) {
                        if (policy.priorityConversationSenders == 1) {
                            ZenLog.traceNotIntercepted(record, "conversationAnyone");
                            return false;
                        } else if (policy.priorityConversationSenders == 2 && record.getChannel().isImportantConversation()) {
                            ZenLog.traceNotIntercepted(record, "conversationMatches");
                            return false;
                        }
                    }
                    if (isCall(record)) {
                        if (policy.allowRepeatCallers() && REPEAT_CALLERS.isRepeat(this.mContext, extras(record), record.getPhoneNumbers())) {
                            ZenLog.traceNotIntercepted(record, "repeatCaller");
                            return false;
                        } else if (!policy.allowCalls()) {
                            ZenLog.traceIntercepted(record, "!allowCalls");
                            return true;
                        } else {
                            return shouldInterceptAudience(policy.allowCallsFrom(), record);
                        }
                    } else if (isMessage(record)) {
                        if (!policy.allowMessages()) {
                            ZenLog.traceIntercepted(record, "!allowMessages");
                            return true;
                        }
                        return shouldInterceptAudience(policy.allowMessagesFrom(), record);
                    } else {
                        ZenLog.traceIntercepted(record, "!priority");
                        return true;
                    }
                }
            case 2:
                ZenLog.traceIntercepted(record, "none");
                return true;
            case 3:
                if (isAlarm(record)) {
                    return false;
                }
                ZenLog.traceIntercepted(record, "alarmsOnly");
                return true;
            default:
                return false;
        }
    }

    private boolean isCritical(NotificationRecord record) {
        return record.getCriticality() < 2;
    }

    private static boolean shouldInterceptAudience(int source, NotificationRecord record) {
        if (!audienceMatches(source, record.getContactAffinity())) {
            ZenLog.traceIntercepted(record, "!audienceMatches");
            return true;
        }
        return false;
    }

    protected static boolean isAlarm(NotificationRecord record) {
        return record.isCategory("alarm") || record.isAudioAttributesUsage(4);
    }

    private static boolean isEvent(NotificationRecord record) {
        return record.isCategory("event");
    }

    private static boolean isReminder(NotificationRecord record) {
        return record.isCategory("reminder");
    }

    public boolean isCall(NotificationRecord record) {
        return record != null && (isDefaultPhoneApp(record.getSbn().getPackageName()) || record.isCategory("call"));
    }

    public boolean isMedia(NotificationRecord record) {
        AudioAttributes aa = record.getAudioAttributes();
        return aa != null && AudioAttributes.SUPPRESSIBLE_USAGES.get(aa.getUsage()) == 5;
    }

    public boolean isSystem(NotificationRecord record) {
        AudioAttributes aa = record.getAudioAttributes();
        return aa != null && AudioAttributes.SUPPRESSIBLE_USAGES.get(aa.getUsage()) == 6;
    }

    private boolean isDefaultPhoneApp(String pkg) {
        ComponentName componentName;
        if (this.mDefaultPhoneApp == null) {
            TelecomManager telecomm = (TelecomManager) this.mContext.getSystemService("telecom");
            this.mDefaultPhoneApp = telecomm != null ? telecomm.getDefaultPhoneApp() : null;
            if (DEBUG) {
                Slog.d(TAG, "Default phone app: " + this.mDefaultPhoneApp);
            }
        }
        return (pkg == null || (componentName = this.mDefaultPhoneApp) == null || !pkg.equals(componentName.getPackageName())) ? false : true;
    }

    protected boolean isMessage(NotificationRecord record) {
        return this.mMessagingUtil.isMessaging(record.getSbn());
    }

    protected boolean isConversation(NotificationRecord record) {
        return record.isConversation();
    }

    private static boolean audienceMatches(int source, float contactAffinity) {
        switch (source) {
            case 0:
                return true;
            case 1:
                return contactAffinity >= 0.5f;
            case 2:
                return contactAffinity >= 1.0f;
            default:
                Slog.w(TAG, "Encountered unknown source: " + source);
                return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void cleanUpCallersAfter(long timeThreshold) {
        REPEAT_CALLERS.cleanUpCallsAfter(timeThreshold);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class RepeatCallers {
        private final ArrayMap<String, Long> mOtherCalls;
        private final ArrayMap<String, Long> mTelCalls;
        private int mThresholdMinutes;

        private RepeatCallers() {
            this.mTelCalls = new ArrayMap<>();
            this.mOtherCalls = new ArrayMap<>();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public synchronized void recordCall(Context context, Bundle extras, ArraySet<String> phoneNumbers) {
            setThresholdMinutes(context);
            if (this.mThresholdMinutes > 0 && extras != null) {
                String[] extraPeople = ValidateNotificationPeople.getExtraPeople(extras);
                if (extraPeople != null && extraPeople.length != 0) {
                    long now = System.currentTimeMillis();
                    cleanUp(this.mTelCalls, now);
                    cleanUp(this.mOtherCalls, now);
                    recordCallers(extraPeople, phoneNumbers, now);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public synchronized boolean isRepeat(Context context, Bundle extras, ArraySet<String> phoneNumbers) {
            setThresholdMinutes(context);
            if (this.mThresholdMinutes > 0 && extras != null) {
                String[] extraPeople = ValidateNotificationPeople.getExtraPeople(extras);
                if (extraPeople != null && extraPeople.length != 0) {
                    long now = System.currentTimeMillis();
                    cleanUp(this.mTelCalls, now);
                    cleanUp(this.mOtherCalls, now);
                    return checkCallers(context, extraPeople, phoneNumbers);
                }
                return false;
            }
            return false;
        }

        private synchronized void cleanUp(ArrayMap<String, Long> calls, long now) {
            int N = calls.size();
            for (int i = N - 1; i >= 0; i--) {
                long time = calls.valueAt(i).longValue();
                if (time > now || now - time > this.mThresholdMinutes * 1000 * 60) {
                    calls.removeAt(i);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public synchronized void cleanUpCallsAfter(long timeThreshold) {
            for (int i = this.mTelCalls.size() - 1; i >= 0; i--) {
                long time = this.mTelCalls.valueAt(i).longValue();
                if (time > timeThreshold) {
                    this.mTelCalls.removeAt(i);
                }
            }
            for (int j = this.mOtherCalls.size() - 1; j >= 0; j--) {
                long time2 = this.mOtherCalls.valueAt(j).longValue();
                if (time2 > timeThreshold) {
                    this.mOtherCalls.removeAt(j);
                }
            }
        }

        private void setThresholdMinutes(Context context) {
            if (this.mThresholdMinutes <= 0) {
                this.mThresholdMinutes = context.getResources().getInteger(17694976);
            }
        }

        private synchronized void recordCallers(String[] people, ArraySet<String> phoneNumbers, long now) {
            for (String person : people) {
                if (person != null) {
                    Uri uri = Uri.parse(person);
                    if ("tel".equals(uri.getScheme())) {
                        String tel = Uri.decode(uri.getSchemeSpecificPart());
                        if (tel != null) {
                            this.mTelCalls.put(tel, Long.valueOf(now));
                        }
                    } else {
                        this.mOtherCalls.put(person, Long.valueOf(now));
                    }
                }
            }
            if (phoneNumbers != null) {
                Iterator<String> it = phoneNumbers.iterator();
                while (it.hasNext()) {
                    String num = it.next();
                    if (num != null) {
                        this.mTelCalls.put(num, Long.valueOf(now));
                    }
                }
            }
        }

        private synchronized boolean checkForNumber(String number, String defaultCountryCode) {
            if (this.mTelCalls.containsKey(number)) {
                return true;
            }
            String numberToCheck = Uri.decode(number);
            if (numberToCheck != null) {
                for (String prev : this.mTelCalls.keySet()) {
                    if (PhoneNumberUtils.areSamePhoneNumber(numberToCheck, prev, defaultCountryCode)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private synchronized boolean checkCallers(Context context, String[] people, ArraySet<String> phoneNumbers) {
            String defaultCountryCode = ((TelephonyManager) context.getSystemService(TelephonyManager.class)).getNetworkCountryIso();
            for (String person : people) {
                if (person != null) {
                    Uri uri = Uri.parse(person);
                    if ("tel".equals(uri.getScheme())) {
                        String number = uri.getSchemeSpecificPart();
                        if (checkForNumber(number, defaultCountryCode)) {
                            return true;
                        }
                    } else if (this.mOtherCalls.containsKey(person)) {
                        return true;
                    }
                }
            }
            if (phoneNumbers != null) {
                Iterator<String> it = phoneNumbers.iterator();
                while (it.hasNext()) {
                    String num = it.next();
                    if (checkForNumber(num, defaultCountryCode)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
