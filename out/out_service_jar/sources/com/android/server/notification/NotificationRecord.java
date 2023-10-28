package com.android.server.notification;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.Person;
import android.content.ContentProvider;
import android.content.Context;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioSystem;
import android.metrics.LogMaker;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.provider.Settings;
import android.service.notification.Adjustment;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationStats;
import android.service.notification.SnoozeCriterion;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.widget.RemoteViews;
import com.android.internal.logging.MetricsLogger;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.am.HostingRecord;
import com.android.server.notification.NotificationUsageStats;
import com.android.server.uri.UriGrantsManagerInternal;
import com.android.server.wm.ActivityTaskManagerInternal;
import dalvik.annotation.optimization.NeverCompile;
import defpackage.CompanionAppsPermissions;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public final class NotificationRecord {
    private static final int MAX_SOUND_DELAY_MS = 2000;
    boolean isCanceled;
    public boolean isUpdate;
    private String mAdjustmentIssuer;
    private final List<Adjustment> mAdjustments;
    private boolean mAllowBubble;
    private boolean mAppDemotedFromConvo;
    private AudioAttributes mAttributes;
    private int mAuthoritativeRank;
    private NotificationChannel mChannel;
    private float mContactAffinity;
    private final Context mContext;
    private long mCreationTimeMs;
    private boolean mEditChoicesBeforeSending;
    private boolean mFlagBubbleRemoved;
    private String mGlobalSortKey;
    private ArraySet<Uri> mGrantableUris;
    private boolean mHasSeenSmartReplies;
    private boolean mHasSentValidMsg;
    private boolean mHidden;
    private int mImportance;
    private boolean mImportanceFixed;
    private boolean mIntercept;
    private long mInterruptionTimeMs;
    private boolean mIsAppImportanceLocked;
    private boolean mIsInterruptive;
    private boolean mIsNotConversationOverride;
    private long mLastAudiblyAlertedMs;
    private long mLastIntrusive;
    private Light mLight;
    private int mNumberOfSmartActionsAdded;
    private int mNumberOfSmartRepliesAdded;
    final int mOriginalFlags;
    private int mPackagePriority;
    private int mPackageVisibility;
    private ArrayList<String> mPeopleOverride;
    private ArraySet<String> mPhoneNumbers;
    private boolean mPkgAllowedAsConvo;
    private boolean mPostSilently;
    private boolean mPreChannelsNotification;
    private boolean mRecentlyIntrusive;
    private boolean mRecordedInterruption;
    private ShortcutInfo mShortcutInfo;
    private boolean mShowBadge;
    private ArrayList<CharSequence> mSmartReplies;
    private ArrayList<SnoozeCriterion> mSnoozeCriteria;
    private Uri mSound;
    private final NotificationStats mStats;
    private boolean mSuggestionsGeneratedByAssistant;
    private ArrayList<Notification.Action> mSystemGeneratedSmartActions;
    final int mTargetSdkVersion;
    private boolean mTextChanged;
    final long mUpdateTimeMs;
    private String mUserExplanation;
    private int mUserSentiment;
    private VibrationEffect mVibration;
    private long mVisibleSinceMs;
    IBinder permissionOwner;
    private final StatusBarNotification sbn;
    NotificationUsageStats.SingleNotificationStats stats;
    static final String TAG = "NotificationRecord";
    static final boolean DBG = Log.isLoggable(TAG, 3);
    private int mSystemImportance = -1000;
    private int mAssistantImportance = -1000;
    private float mRankingScore = 0.0f;
    private int mCriticality = 2;
    private int mImportanceExplanationCode = 0;
    private int mInitialImportanceExplanationCode = 0;
    private int mSuppressedVisualEffects = 0;
    private boolean mPendingLogUpdate = false;
    private boolean mInitSet = false;
    IActivityManager mAm = ActivityManager.getService();
    UriGrantsManagerInternal mUgmInternal = (UriGrantsManagerInternal) LocalServices.getService(UriGrantsManagerInternal.class);
    private long mRankingTimeMs = calculateRankingTimeMs(0);

    public NotificationRecord(Context context, StatusBarNotification sbn, NotificationChannel channel) {
        this.mImportance = -1000;
        this.mPreChannelsNotification = true;
        this.sbn = sbn;
        this.mTargetSdkVersion = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageTargetSdkVersion(sbn.getPackageName());
        this.mOriginalFlags = sbn.getNotification().flags;
        long postTime = sbn.getPostTime();
        this.mCreationTimeMs = postTime;
        this.mUpdateTimeMs = postTime;
        this.mInterruptionTimeMs = postTime;
        this.mContext = context;
        this.stats = new NotificationUsageStats.SingleNotificationStats();
        this.mChannel = channel;
        this.mPreChannelsNotification = isPreChannelsNotification();
        this.mSound = calculateSound();
        this.mVibration = calculateVibration();
        this.mAttributes = calculateAttributes();
        this.mImportance = calculateInitialImportance();
        this.mLight = calculateLights();
        this.mAdjustments = new ArrayList();
        this.mStats = new NotificationStats();
        calculateUserSentiment();
        calculateGrantableUris();
    }

    private boolean isPreChannelsNotification() {
        if ("miscellaneous".equals(getChannel().getId()) && this.mTargetSdkVersion < 26) {
            return true;
        }
        return false;
    }

    private Uri calculateSound() {
        Notification n = getSbn().getNotification();
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.leanback")) {
            return null;
        }
        Uri sound = this.mChannel.getSound();
        if (this.mPreChannelsNotification && (getChannel().getUserLockedFields() & 32) == 0) {
            boolean useDefaultSound = (n.defaults & 1) != 0;
            if (useDefaultSound) {
                return Settings.System.DEFAULT_NOTIFICATION_URI;
            }
            return n.sound;
        }
        return sound;
    }

    private Light calculateLights() {
        int defaultLightColor = this.mContext.getResources().getColor(17170814);
        int defaultLightOn = this.mContext.getResources().getInteger(17694792);
        int defaultLightOff = this.mContext.getResources().getInteger(17694791);
        int channelLightColor = getChannel().getLightColor() != 0 ? getChannel().getLightColor() : defaultLightColor;
        Light light = getChannel().shouldShowLights() ? new Light(channelLightColor, defaultLightOn, defaultLightOff) : null;
        if (this.mPreChannelsNotification && (getChannel().getUserLockedFields() & 8) == 0) {
            Notification notification = getSbn().getNotification();
            if ((notification.flags & 1) != 0) {
                Light light2 = new Light(notification.ledARGB, notification.ledOnMS, notification.ledOffMS);
                if ((notification.defaults & 4) != 0) {
                    return new Light(defaultLightColor, defaultLightOn, defaultLightOff);
                }
                return light2;
            }
            return null;
        }
        return light;
    }

    private VibrationEffect calculateVibration() {
        VibrationEffect vibration;
        VibratorHelper helper = new VibratorHelper(this.mContext);
        Notification notification = getSbn().getNotification();
        boolean insistent = (notification.flags & 4) != 0;
        VibrationEffect defaultVibration = helper.createDefaultVibration(insistent);
        if (getChannel().shouldVibrate()) {
            if (getChannel().getVibrationPattern() == null) {
                vibration = defaultVibration;
            } else {
                vibration = VibratorHelper.createWaveformVibration(getChannel().getVibrationPattern(), insistent);
            }
        } else {
            vibration = null;
        }
        if (this.mPreChannelsNotification && (getChannel().getUserLockedFields() & 16) == 0) {
            boolean useDefaultVibrate = (notification.defaults & 2) != 0;
            if (useDefaultVibrate) {
                return defaultVibration;
            }
            VibrationEffect vibration2 = VibratorHelper.createWaveformVibration(notification.vibrate, insistent);
            return vibration2;
        }
        return vibration;
    }

    private AudioAttributes calculateAttributes() {
        Notification n = getSbn().getNotification();
        AudioAttributes attributes = getChannel().getAudioAttributes();
        if (attributes == null) {
            attributes = Notification.AUDIO_ATTRIBUTES_DEFAULT;
        }
        if (this.mPreChannelsNotification && (getChannel().getUserLockedFields() & 32) == 0) {
            if (n.audioAttributes != null) {
                return n.audioAttributes;
            }
            if (n.audioStreamType >= 0 && n.audioStreamType < AudioSystem.getNumStreamTypes()) {
                return new AudioAttributes.Builder().setInternalLegacyStreamType(n.audioStreamType).build();
            }
            if (n.audioStreamType != -1) {
                Log.w(TAG, String.format("Invalid stream type: %d", Integer.valueOf(n.audioStreamType)));
                return attributes;
            }
            return attributes;
        }
        return attributes;
    }

    private int calculateInitialImportance() {
        int i;
        Notification n = getSbn().getNotification();
        int importance = getChannel().getImportance();
        boolean z = true;
        if (getChannel().hasUserSetImportance()) {
            i = 2;
        } else {
            i = 1;
        }
        this.mInitialImportanceExplanationCode = i;
        if ((n.flags & 128) != 0) {
            n.priority = 2;
        }
        int requestedImportance = 3;
        n.priority = NotificationManagerService.clamp(n.priority, -2, 2);
        switch (n.priority) {
            case -2:
                requestedImportance = 1;
                break;
            case -1:
                requestedImportance = 2;
                break;
            case 0:
                requestedImportance = 3;
                break;
            case 1:
            case 2:
                requestedImportance = 4;
                break;
        }
        this.stats.requestedImportance = requestedImportance;
        NotificationUsageStats.SingleNotificationStats singleNotificationStats = this.stats;
        if (this.mSound == null && this.mVibration == null) {
            z = false;
        }
        singleNotificationStats.isNoisy = z;
        if (this.mPreChannelsNotification && (importance == -1000 || !getChannel().hasUserSetImportance())) {
            if (!this.stats.isNoisy && requestedImportance > 2) {
                requestedImportance = 2;
            }
            if (this.stats.isNoisy && requestedImportance < 3) {
                requestedImportance = 3;
            }
            if (n.fullScreenIntent != null) {
                requestedImportance = 4;
            }
            importance = requestedImportance;
            this.mInitialImportanceExplanationCode = 5;
        }
        this.stats.naturalImportance = importance;
        return importance;
    }

    public void copyRankingInformation(NotificationRecord previous) {
        this.mContactAffinity = previous.mContactAffinity;
        this.mRecentlyIntrusive = previous.mRecentlyIntrusive;
        this.mPackagePriority = previous.mPackagePriority;
        this.mPackageVisibility = previous.mPackageVisibility;
        this.mIntercept = previous.mIntercept;
        this.mHidden = previous.mHidden;
        this.mRankingTimeMs = calculateRankingTimeMs(previous.getRankingTimeMs());
        this.mCreationTimeMs = previous.mCreationTimeMs;
        this.mVisibleSinceMs = previous.mVisibleSinceMs;
        if (previous.getSbn().getOverrideGroupKey() != null && !getSbn().isAppGroup()) {
            getSbn().setOverrideGroupKey(previous.getSbn().getOverrideGroupKey());
        }
    }

    public Notification getNotification() {
        return getSbn().getNotification();
    }

    public int getFlags() {
        return getSbn().getNotification().flags;
    }

    public UserHandle getUser() {
        return getSbn().getUser();
    }

    public String getKey() {
        return getSbn().getKey();
    }

    public int getUserId() {
        return getSbn().getUserId();
    }

    public int getUid() {
        return getSbn().getUid();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(ProtoOutputStream proto, long fieldId, boolean redact, int state) {
        long token = proto.start(fieldId);
        proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, getSbn().getKey());
        proto.write(CompanionMessage.TYPE, state);
        if (getChannel() != null) {
            proto.write(1138166333444L, getChannel().getId());
        }
        proto.write(1133871366152L, getLight() != null);
        proto.write(1133871366151L, getVibration() != null);
        proto.write(1120986464259L, getSbn().getNotification().flags);
        proto.write(1138166333449L, getGroupKey());
        proto.write(1172526071818L, getImportance());
        if (getSound() != null) {
            proto.write(1138166333445L, getSound().toString());
        }
        if (getAudioAttributes() != null) {
            getAudioAttributes().dumpDebug(proto, 1146756268038L);
        }
        proto.write(1138166333451L, getSbn().getPackageName());
        proto.write(1138166333452L, getSbn().getOpPkg());
        proto.end(token);
    }

    String formatRemoteViews(RemoteViews rv) {
        return rv == null ? "null" : String.format("%s/0x%08x (%d bytes): %s", rv.getPackage(), Integer.valueOf(rv.getLayoutId()), Integer.valueOf(rv.estimateMemoryUsage()), rv.toString());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @NeverCompile
    public void dump(PrintWriter pw, String prefix, Context baseContext, boolean redact) {
        Notification notification = getSbn().getNotification();
        pw.println(prefix + this);
        String prefix2 = prefix + "  ";
        pw.println(prefix2 + "uid=" + getSbn().getUid() + " userId=" + getSbn().getUserId());
        pw.println(prefix2 + "opPkg=" + getSbn().getOpPkg());
        pw.println(prefix2 + "icon=" + notification.getSmallIcon());
        pw.println(prefix2 + "flags=0x" + Integer.toHexString(notification.flags));
        pw.println(prefix2 + "originalFlags=0x" + Integer.toHexString(this.mOriginalFlags));
        pw.println(prefix2 + "pri=" + notification.priority);
        pw.println(prefix2 + "key=" + getSbn().getKey());
        pw.println(prefix2 + "seen=" + this.mStats.hasSeen());
        pw.println(prefix2 + "groupKey=" + getGroupKey());
        pw.println(prefix2 + "notification=");
        dumpNotification(pw, prefix2 + prefix2, notification, redact);
        pw.println(prefix2 + "publicNotification=");
        dumpNotification(pw, prefix2 + prefix2, notification.publicVersion, redact);
        pw.println(prefix2 + "stats=" + this.stats.toString());
        pw.println(prefix2 + "mContactAffinity=" + this.mContactAffinity);
        pw.println(prefix2 + "mRecentlyIntrusive=" + this.mRecentlyIntrusive);
        pw.println(prefix2 + "mPackagePriority=" + this.mPackagePriority);
        pw.println(prefix2 + "mPackageVisibility=" + this.mPackageVisibility);
        pw.println(prefix2 + "mSystemImportance=" + NotificationListenerService.Ranking.importanceToString(this.mSystemImportance));
        pw.println(prefix2 + "mAsstImportance=" + NotificationListenerService.Ranking.importanceToString(this.mAssistantImportance));
        pw.println(prefix2 + "mImportance=" + NotificationListenerService.Ranking.importanceToString(this.mImportance));
        pw.println(prefix2 + "mImportanceExplanation=" + ((Object) getImportanceExplanation()));
        pw.println(prefix2 + "mIsAppImportanceLocked=" + this.mIsAppImportanceLocked);
        pw.println(prefix2 + "mIntercept=" + this.mIntercept);
        pw.println(prefix2 + "mHidden==" + this.mHidden);
        pw.println(prefix2 + "mGlobalSortKey=" + this.mGlobalSortKey);
        pw.println(prefix2 + "mRankingTimeMs=" + this.mRankingTimeMs);
        pw.println(prefix2 + "mCreationTimeMs=" + this.mCreationTimeMs);
        pw.println(prefix2 + "mVisibleSinceMs=" + this.mVisibleSinceMs);
        pw.println(prefix2 + "mUpdateTimeMs=" + this.mUpdateTimeMs);
        pw.println(prefix2 + "mInterruptionTimeMs=" + this.mInterruptionTimeMs);
        pw.println(prefix2 + "mSuppressedVisualEffects= " + this.mSuppressedVisualEffects);
        if (this.mPreChannelsNotification) {
            pw.println(prefix2 + String.format("defaults=0x%08x flags=0x%08x", Integer.valueOf(notification.defaults), Integer.valueOf(notification.flags)));
            pw.println(prefix2 + "n.sound=" + notification.sound);
            pw.println(prefix2 + "n.audioStreamType=" + notification.audioStreamType);
            pw.println(prefix2 + "n.audioAttributes=" + notification.audioAttributes);
            pw.println(prefix2 + String.format("  led=0x%08x onMs=%d offMs=%d", Integer.valueOf(notification.ledARGB), Integer.valueOf(notification.ledOnMS), Integer.valueOf(notification.ledOffMS)));
            pw.println(prefix2 + "vibrate=" + Arrays.toString(notification.vibrate));
        }
        pw.println(prefix2 + "mSound= " + this.mSound);
        pw.println(prefix2 + "mVibration= " + this.mVibration);
        pw.println(prefix2 + "mAttributes= " + this.mAttributes);
        pw.println(prefix2 + "mLight= " + this.mLight);
        pw.println(prefix2 + "mShowBadge=" + this.mShowBadge);
        pw.println(prefix2 + "mColorized=" + notification.isColorized());
        pw.println(prefix2 + "mAllowBubble=" + this.mAllowBubble);
        pw.println(prefix2 + "isBubble=" + notification.isBubbleNotification());
        pw.println(prefix2 + "mIsInterruptive=" + this.mIsInterruptive);
        pw.println(prefix2 + "effectiveNotificationChannel=" + getChannel());
        if (getPeopleOverride() != null) {
            pw.println(prefix2 + "overridePeople= " + TextUtils.join(",", getPeopleOverride()));
        }
        if (getSnoozeCriteria() != null) {
            pw.println(prefix2 + "snoozeCriteria=" + TextUtils.join(",", getSnoozeCriteria()));
        }
        pw.println(prefix2 + "mAdjustments=" + this.mAdjustments);
        pw.println(prefix2 + "shortcut=" + notification.getShortcutId() + " found valid? " + (this.mShortcutInfo != null));
    }

    private void dumpNotification(PrintWriter pw, String prefix, Notification notification, boolean redact) {
        if (notification == null) {
            pw.println(prefix + "None");
            return;
        }
        pw.println(prefix + "fullscreenIntent=" + notification.fullScreenIntent);
        pw.println(prefix + "contentIntent=" + notification.contentIntent);
        pw.println(prefix + "deleteIntent=" + notification.deleteIntent);
        pw.println(prefix + "number=" + notification.number);
        pw.println(prefix + "groupAlertBehavior=" + notification.getGroupAlertBehavior());
        pw.println(prefix + "when=" + notification.when);
        pw.print(prefix + "tickerText=");
        if (!TextUtils.isEmpty(notification.tickerText)) {
            String ticker = notification.tickerText.toString();
            if (redact) {
                pw.print(ticker.length() > 16 ? ticker.substring(0, 8) : "");
                pw.println("...");
            } else {
                pw.println(ticker);
            }
        } else {
            pw.println("null");
        }
        pw.println(prefix + "contentView=" + formatRemoteViews(notification.contentView));
        pw.println(prefix + "bigContentView=" + formatRemoteViews(notification.bigContentView));
        pw.println(prefix + "headsUpContentView=" + formatRemoteViews(notification.headsUpContentView));
        pw.println(prefix + String.format("color=0x%08x", Integer.valueOf(notification.color)));
        pw.println(prefix + "timeout=" + TimeUtils.formatForLogging(notification.getTimeoutAfter()));
        if (notification.actions != null && notification.actions.length > 0) {
            pw.println(prefix + "actions={");
            int N = notification.actions.length;
            for (int i = 0; i < N; i++) {
                Notification.Action action = notification.actions[i];
                if (action != null) {
                    Object[] objArr = new Object[4];
                    objArr[0] = prefix;
                    objArr[1] = Integer.valueOf(i);
                    objArr[2] = action.title;
                    objArr[3] = action.actionIntent == null ? "null" : action.actionIntent.toString();
                    pw.println(String.format("%s    [%d] \"%s\" -> %s", objArr));
                }
            }
            pw.println(prefix + "  }");
        }
        if (notification.extras != null && notification.extras.size() > 0) {
            pw.println(prefix + "extras={");
            for (String key : notification.extras.keySet()) {
                pw.print(prefix + "    " + key + "=");
                Object val = notification.extras.get(key);
                if (val == null) {
                    pw.println("null");
                } else {
                    pw.print(val.getClass().getSimpleName());
                    if (redact && (val instanceof CharSequence) && shouldRedactStringExtra(key)) {
                        pw.print(String.format(" [length=%d]", Integer.valueOf(((CharSequence) val).length())));
                    } else if (val instanceof Bitmap) {
                        pw.print(String.format(" (%dx%d)", Integer.valueOf(((Bitmap) val).getWidth()), Integer.valueOf(((Bitmap) val).getHeight())));
                    } else if (val.getClass().isArray()) {
                        int N2 = Array.getLength(val);
                        pw.print(" (" + N2 + ")");
                        if (!redact) {
                            for (int j = 0; j < N2; j++) {
                                pw.println();
                                pw.print(String.format("%s      [%d] %s", prefix, Integer.valueOf(j), String.valueOf(Array.get(val, j))));
                            }
                        }
                    } else {
                        pw.print(" (" + String.valueOf(val) + ")");
                    }
                    pw.println();
                }
            }
            pw.println(prefix + "}");
        }
    }

    private boolean shouldRedactStringExtra(String key) {
        if (key == null) {
            return true;
        }
        char c = 65535;
        switch (key.hashCode()) {
            case -1349298919:
                if (key.equals("android.template")) {
                    c = 1;
                    break;
                }
                break;
            case -330858995:
                if (key.equals("android.substName")) {
                    c = 0;
                    break;
                }
                break;
            case 1258919194:
                if (key.equals("android.support.v4.app.extra.COMPAT_TEMPLATE")) {
                    c = 2;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
            case 1:
            case 2:
                return false;
            default:
                return true;
        }
    }

    public final String toString() {
        return String.format("NotificationRecord(0x%08x: pkg=%s user=%s id=%d tag=%s importance=%d key=%s: %s)", Integer.valueOf(System.identityHashCode(this)), getSbn().getPackageName(), getSbn().getUser(), Integer.valueOf(getSbn().getId()), getSbn().getTag(), Integer.valueOf(this.mImportance), getSbn().getKey(), getSbn().getNotification());
    }

    public boolean hasAdjustment(String key) {
        synchronized (this.mAdjustments) {
            for (Adjustment adjustment : this.mAdjustments) {
                if (adjustment.getSignals().containsKey(key)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void addAdjustment(Adjustment adjustment) {
        synchronized (this.mAdjustments) {
            this.mAdjustments.add(adjustment);
        }
    }

    public void applyAdjustments() {
        System.currentTimeMillis();
        synchronized (this.mAdjustments) {
            for (Adjustment adjustment : this.mAdjustments) {
                Bundle signals = adjustment.getSignals();
                if (signals.containsKey("key_people")) {
                    ArrayList<String> people = adjustment.getSignals().getStringArrayList("key_people");
                    setPeopleOverride(people);
                    EventLogTags.writeNotificationAdjusted(getKey(), "key_people", people.toString());
                }
                if (signals.containsKey("key_snooze_criteria")) {
                    ArrayList<SnoozeCriterion> snoozeCriterionList = adjustment.getSignals().getParcelableArrayList("key_snooze_criteria");
                    setSnoozeCriteria(snoozeCriterionList);
                    EventLogTags.writeNotificationAdjusted(getKey(), "key_snooze_criteria", snoozeCriterionList.toString());
                }
                if (signals.containsKey("key_group_key")) {
                    String groupOverrideKey = adjustment.getSignals().getString("key_group_key");
                    setOverrideGroupKey(groupOverrideKey);
                    EventLogTags.writeNotificationAdjusted(getKey(), "key_group_key", groupOverrideKey);
                }
                if (signals.containsKey("key_user_sentiment") && !this.mIsAppImportanceLocked && (getChannel().getUserLockedFields() & 4) == 0) {
                    setUserSentiment(adjustment.getSignals().getInt("key_user_sentiment", 0));
                    EventLogTags.writeNotificationAdjusted(getKey(), "key_user_sentiment", Integer.toString(getUserSentiment()));
                }
                if (signals.containsKey("key_contextual_actions")) {
                    setSystemGeneratedSmartActions(signals.getParcelableArrayList("key_contextual_actions"));
                    EventLogTags.writeNotificationAdjusted(getKey(), "key_contextual_actions", getSystemGeneratedSmartActions().toString());
                }
                if (signals.containsKey("key_text_replies")) {
                    setSmartReplies(signals.getCharSequenceArrayList("key_text_replies"));
                    EventLogTags.writeNotificationAdjusted(getKey(), "key_text_replies", getSmartReplies().toString());
                }
                if (signals.containsKey("key_importance")) {
                    int importance = Math.min(4, Math.max(-1000, signals.getInt("key_importance")));
                    setAssistantImportance(importance);
                    EventLogTags.writeNotificationAdjusted(getKey(), "key_importance", Integer.toString(importance));
                }
                if (signals.containsKey("key_ranking_score")) {
                    this.mRankingScore = signals.getFloat("key_ranking_score");
                    EventLogTags.writeNotificationAdjusted(getKey(), "key_ranking_score", Float.toString(this.mRankingScore));
                }
                if (signals.containsKey("key_not_conversation")) {
                    this.mIsNotConversationOverride = signals.getBoolean("key_not_conversation");
                    EventLogTags.writeNotificationAdjusted(getKey(), "key_not_conversation", Boolean.toString(this.mIsNotConversationOverride));
                }
                if (!signals.isEmpty() && adjustment.getIssuer() != null) {
                    this.mAdjustmentIssuer = adjustment.getIssuer();
                }
            }
            this.mAdjustments.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getAdjustmentIssuer() {
        return this.mAdjustmentIssuer;
    }

    public void setIsAppImportanceLocked(boolean isAppImportanceLocked) {
        this.mIsAppImportanceLocked = isAppImportanceLocked;
        calculateUserSentiment();
    }

    public void setContactAffinity(float contactAffinity) {
        this.mContactAffinity = contactAffinity;
    }

    public float getContactAffinity() {
        return this.mContactAffinity;
    }

    public void setRecentlyIntrusive(boolean recentlyIntrusive) {
        this.mRecentlyIntrusive = recentlyIntrusive;
        if (recentlyIntrusive) {
            this.mLastIntrusive = System.currentTimeMillis();
        }
    }

    public boolean isRecentlyIntrusive() {
        return this.mRecentlyIntrusive;
    }

    public long getLastIntrusive() {
        return this.mLastIntrusive;
    }

    public void setPackagePriority(int packagePriority) {
        this.mPackagePriority = packagePriority;
    }

    public int getPackagePriority() {
        return this.mPackagePriority;
    }

    public void setPackageVisibilityOverride(int packageVisibility) {
        this.mPackageVisibility = packageVisibility;
    }

    public int getPackageVisibilityOverride() {
        return this.mPackageVisibility;
    }

    private String getUserExplanation() {
        if (this.mUserExplanation == null) {
            this.mUserExplanation = this.mContext.getResources().getString(17040489);
        }
        return this.mUserExplanation;
    }

    public void setSystemImportance(int importance) {
        this.mSystemImportance = importance;
        calculateImportance();
    }

    public void setAssistantImportance(int importance) {
        this.mAssistantImportance = importance;
    }

    public int getAssistantImportance() {
        return this.mAssistantImportance;
    }

    public void setImportanceFixed(boolean fixed) {
        this.mImportanceFixed = fixed;
    }

    public boolean isImportanceFixed() {
        return this.mImportanceFixed;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void calculateImportance() {
        int i;
        if (this.mInitSet) {
            Log.v(TAG, "force dont update mImportance " + this.mImportance);
            return;
        }
        this.mImportance = calculateInitialImportance();
        this.mImportanceExplanationCode = this.mInitialImportanceExplanationCode;
        if (!getChannel().hasUserSetImportance() && (i = this.mAssistantImportance) != -1000 && !this.mImportanceFixed) {
            this.mImportance = i;
            this.mImportanceExplanationCode = 3;
        }
        int i2 = this.mSystemImportance;
        if (i2 != -1000) {
            this.mImportance = i2;
            this.mImportanceExplanationCode = 4;
        }
    }

    public int getImportance() {
        return this.mImportance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getInitialImportance() {
        return this.stats.naturalImportance;
    }

    public float getRankingScore() {
        return this.mRankingScore;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getImportanceExplanationCode() {
        return this.mImportanceExplanationCode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getInitialImportanceExplanationCode() {
        return this.mInitialImportanceExplanationCode;
    }

    public CharSequence getImportanceExplanation() {
        switch (this.mImportanceExplanationCode) {
            case 0:
                return null;
            case 1:
            case 5:
                return "app";
            case 2:
                return "user";
            case 3:
                return "asst";
            case 4:
                return HostingRecord.HOSTING_TYPE_SYSTEM;
            default:
                return null;
        }
    }

    public boolean setIntercepted(boolean intercept) {
        this.mIntercept = intercept;
        return intercept;
    }

    public void setCriticality(int criticality) {
        this.mCriticality = criticality;
    }

    public int getCriticality() {
        return this.mCriticality;
    }

    public boolean isIntercepted() {
        return this.mIntercept;
    }

    public boolean isNewEnoughForAlerting(long now) {
        return getFreshnessMs(now) <= 2000;
    }

    public void setHidden(boolean hidden) {
        this.mHidden = hidden;
    }

    public boolean isHidden() {
        return this.mHidden;
    }

    public boolean isForegroundService() {
        return (getFlags() & 64) != 0;
    }

    public void setPostSilently(boolean postSilently) {
        this.mPostSilently = postSilently;
    }

    public boolean shouldPostSilently() {
        return this.mPostSilently;
    }

    public void setSuppressedVisualEffects(int effects) {
        this.mSuppressedVisualEffects = effects;
    }

    public int getSuppressedVisualEffects() {
        return this.mSuppressedVisualEffects;
    }

    public boolean isCategory(String category) {
        return Objects.equals(getNotification().category, category);
    }

    public boolean isAudioAttributesUsage(int usage) {
        AudioAttributes audioAttributes = this.mAttributes;
        return audioAttributes != null && audioAttributes.getUsage() == usage;
    }

    public long getRankingTimeMs() {
        return this.mRankingTimeMs;
    }

    public int getFreshnessMs(long now) {
        return (int) (now - this.mUpdateTimeMs);
    }

    public int getLifespanMs(long now) {
        return (int) (now - this.mCreationTimeMs);
    }

    public int getExposureMs(long now) {
        long j = this.mVisibleSinceMs;
        if (j == 0) {
            return 0;
        }
        return (int) (now - j);
    }

    public int getInterruptionMs(long now) {
        return (int) (now - this.mInterruptionTimeMs);
    }

    public long getUpdateTimeMs() {
        return this.mUpdateTimeMs;
    }

    public void setVisibility(boolean visible, int rank, int count, NotificationRecordLogger notificationRecordLogger) {
        long now = System.currentTimeMillis();
        this.mVisibleSinceMs = visible ? now : this.mVisibleSinceMs;
        this.stats.onVisibilityChanged(visible);
        MetricsLogger.action(getLogMaker(now).setCategory(128).setType(visible ? 1 : 2).addTaggedData(798, Integer.valueOf(rank)).addTaggedData(1395, Integer.valueOf(count)));
        if (visible) {
            setSeen();
            MetricsLogger.histogram(this.mContext, "note_freshness", getFreshnessMs(now));
        }
        EventLogTags.writeNotificationVisibility(getKey(), visible ? 1 : 0, getLifespanMs(now), getFreshnessMs(now), 0, rank);
        notificationRecordLogger.logNotificationVisibility(this, visible);
    }

    private long calculateRankingTimeMs(long previousRankingTimeMs) {
        Notification n = getNotification();
        if (n.when != 0 && n.when <= getSbn().getPostTime()) {
            return n.when;
        }
        if (previousRankingTimeMs > 0) {
            return previousRankingTimeMs;
        }
        return getSbn().getPostTime();
    }

    public void setGlobalSortKey(String globalSortKey) {
        this.mGlobalSortKey = globalSortKey;
    }

    public String getGlobalSortKey() {
        return this.mGlobalSortKey;
    }

    public boolean isSeen() {
        return this.mStats.hasSeen();
    }

    public void setSeen() {
        this.mStats.setSeen();
        if (this.mTextChanged) {
            setInterruptive(true);
        }
    }

    public void setAuthoritativeRank(int authoritativeRank) {
        this.mAuthoritativeRank = authoritativeRank;
    }

    public int getAuthoritativeRank() {
        return this.mAuthoritativeRank;
    }

    public String getGroupKey() {
        return getSbn().getGroupKey();
    }

    public void setOverrideGroupKey(String overrideGroupKey) {
        getSbn().setOverrideGroupKey(overrideGroupKey);
    }

    public NotificationChannel getChannel() {
        return this.mChannel;
    }

    public boolean getIsAppImportanceLocked() {
        return this.mIsAppImportanceLocked;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateNotificationChannel(NotificationChannel channel) {
        if (channel != null) {
            this.mChannel = channel;
            calculateImportance();
            calculateUserSentiment();
        }
    }

    public void setShowBadge(boolean showBadge) {
        this.mShowBadge = showBadge;
    }

    public boolean canBubble() {
        return this.mAllowBubble;
    }

    public void setAllowBubble(boolean allow) {
        this.mAllowBubble = allow;
    }

    public boolean canShowBadge() {
        return this.mShowBadge;
    }

    public Light getLight() {
        return this.mLight;
    }

    public Uri getSound() {
        return this.mSound;
    }

    public VibrationEffect getVibration() {
        return this.mVibration;
    }

    public AudioAttributes getAudioAttributes() {
        return this.mAttributes;
    }

    public ArrayList<String> getPeopleOverride() {
        return this.mPeopleOverride;
    }

    public void setInterruptive(boolean interruptive) {
        this.mIsInterruptive = interruptive;
        long now = System.currentTimeMillis();
        this.mInterruptionTimeMs = interruptive ? now : this.mInterruptionTimeMs;
        if (interruptive) {
            MetricsLogger.action(getLogMaker().setCategory(1501).setType(1).addTaggedData((int) NetworkConstants.ETHER_MTU, Integer.valueOf(getInterruptionMs(now))));
            MetricsLogger.histogram(this.mContext, "note_interruptive", getInterruptionMs(now));
        }
    }

    public void setAudiblyAlerted(boolean audiblyAlerted) {
        this.mLastAudiblyAlertedMs = audiblyAlerted ? System.currentTimeMillis() : -1L;
    }

    public void setTextChanged(boolean textChanged) {
        this.mTextChanged = textChanged;
    }

    public void setRecordedInterruption(boolean recorded) {
        this.mRecordedInterruption = recorded;
    }

    public boolean hasRecordedInterruption() {
        return this.mRecordedInterruption;
    }

    public boolean isInterruptive() {
        return this.mIsInterruptive;
    }

    public boolean isTextChanged() {
        return this.mTextChanged;
    }

    public long getLastAudiblyAlertedMs() {
        return this.mLastAudiblyAlertedMs;
    }

    protected void setPeopleOverride(ArrayList<String> people) {
        this.mPeopleOverride = people;
    }

    public ArrayList<SnoozeCriterion> getSnoozeCriteria() {
        return this.mSnoozeCriteria;
    }

    protected void setSnoozeCriteria(ArrayList<SnoozeCriterion> snoozeCriteria) {
        this.mSnoozeCriteria = snoozeCriteria;
    }

    private void calculateUserSentiment() {
        if ((getChannel().getUserLockedFields() & 4) != 0 || this.mIsAppImportanceLocked) {
            this.mUserSentiment = 1;
        }
    }

    private void setUserSentiment(int userSentiment) {
        this.mUserSentiment = userSentiment;
    }

    public int getUserSentiment() {
        return this.mUserSentiment;
    }

    public NotificationStats getStats() {
        return this.mStats;
    }

    public void recordExpanded() {
        this.mStats.setExpanded();
    }

    public void recordDirectReplied() {
        this.mStats.setDirectReplied();
    }

    public void recordDismissalSurface(int surface) {
        this.mStats.setDismissalSurface(surface);
    }

    public void recordDismissalSentiment(int sentiment) {
        this.mStats.setDismissalSentiment(sentiment);
    }

    public void recordSnoozed() {
        this.mStats.setSnoozed();
    }

    public void recordViewedSettings() {
        this.mStats.setViewedSettings();
    }

    public void setNumSmartRepliesAdded(int noReplies) {
        this.mNumberOfSmartRepliesAdded = noReplies;
    }

    public int getNumSmartRepliesAdded() {
        return this.mNumberOfSmartRepliesAdded;
    }

    public void setNumSmartActionsAdded(int noActions) {
        this.mNumberOfSmartActionsAdded = noActions;
    }

    public int getNumSmartActionsAdded() {
        return this.mNumberOfSmartActionsAdded;
    }

    public void setSuggestionsGeneratedByAssistant(boolean generatedByAssistant) {
        this.mSuggestionsGeneratedByAssistant = generatedByAssistant;
    }

    public boolean getSuggestionsGeneratedByAssistant() {
        return this.mSuggestionsGeneratedByAssistant;
    }

    public boolean getEditChoicesBeforeSending() {
        return this.mEditChoicesBeforeSending;
    }

    public void setEditChoicesBeforeSending(boolean editChoicesBeforeSending) {
        this.mEditChoicesBeforeSending = editChoicesBeforeSending;
    }

    public boolean hasSeenSmartReplies() {
        return this.mHasSeenSmartReplies;
    }

    public void setSeenSmartReplies(boolean hasSeenSmartReplies) {
        this.mHasSeenSmartReplies = hasSeenSmartReplies;
    }

    public boolean hasBeenVisiblyExpanded() {
        return this.stats.hasBeenVisiblyExpanded();
    }

    public boolean isFlagBubbleRemoved() {
        return this.mFlagBubbleRemoved;
    }

    public void setFlagBubbleRemoved(boolean flagBubbleRemoved) {
        this.mFlagBubbleRemoved = flagBubbleRemoved;
    }

    public void setSystemGeneratedSmartActions(ArrayList<Notification.Action> systemGeneratedSmartActions) {
        this.mSystemGeneratedSmartActions = systemGeneratedSmartActions;
    }

    public ArrayList<Notification.Action> getSystemGeneratedSmartActions() {
        return this.mSystemGeneratedSmartActions;
    }

    public void setSmartReplies(ArrayList<CharSequence> smartReplies) {
        this.mSmartReplies = smartReplies;
    }

    public ArrayList<CharSequence> getSmartReplies() {
        return this.mSmartReplies;
    }

    public boolean isProxied() {
        return !Objects.equals(getSbn().getPackageName(), getSbn().getOpPkg());
    }

    public int getNotificationType() {
        if (isConversation()) {
            return 1;
        }
        if (getImportance() >= 3) {
            return 2;
        }
        return 4;
    }

    public ArraySet<Uri> getGrantableUris() {
        return this.mGrantableUris;
    }

    protected void calculateGrantableUris() {
        NotificationChannel channel;
        Notification notification = getNotification();
        notification.visitUris(new Consumer() { // from class: com.android.server.notification.NotificationRecord$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                NotificationRecord.this.m5174xf4bbd782((Uri) obj);
            }
        });
        if (notification.getChannelId() != null && (channel = getChannel()) != null) {
            visitGrantableUri(channel.getSound(), (channel.getUserLockedFields() & 32) != 0, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$calculateGrantableUris$0$com-android-server-notification-NotificationRecord  reason: not valid java name */
    public /* synthetic */ void m5174xf4bbd782(Uri uri) {
        visitGrantableUri(uri, false, false);
    }

    private void visitGrantableUri(Uri uri, boolean userOverriddenUri, boolean isSound) {
        int sourceUid;
        if (uri == null || !ActivityTaskManagerInternal.ASSIST_KEY_CONTENT.equals(uri.getScheme()) || (sourceUid = getSbn().getUid()) == 1000) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            try {
                this.mUgmInternal.checkGrantUriPermission(sourceUid, null, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid)));
                if (this.mGrantableUris == null) {
                    this.mGrantableUris = new ArraySet<>();
                }
                this.mGrantableUris.add(uri);
            } catch (SecurityException e) {
                if (!userOverriddenUri) {
                    if (isSound) {
                        this.mSound = Settings.System.DEFAULT_NOTIFICATION_URI;
                        Log.w(TAG, "Replacing " + uri + " from " + sourceUid + ": " + e.getMessage());
                    } else if (this.mTargetSdkVersion >= 28) {
                        throw e;
                    } else {
                        Log.w(TAG, "Ignoring " + uri + " from " + sourceUid + ": " + e.getMessage());
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public LogMaker getLogMaker(long now) {
        LogMaker lm = getSbn().getLogMaker().addTaggedData(858, Integer.valueOf(this.mImportance)).addTaggedData(793, Integer.valueOf(getLifespanMs(now))).addTaggedData(795, Integer.valueOf(getFreshnessMs(now))).addTaggedData(794, Integer.valueOf(getExposureMs(now))).addTaggedData((int) NetworkConstants.ETHER_MTU, Integer.valueOf(getInterruptionMs(now)));
        int i = this.mImportanceExplanationCode;
        if (i != 0) {
            lm.addTaggedData(1688, Integer.valueOf(i));
            int i2 = this.mImportanceExplanationCode;
            if ((i2 == 3 || i2 == 4) && this.stats.naturalImportance != -1000) {
                lm.addTaggedData(1690, Integer.valueOf(this.mInitialImportanceExplanationCode));
                lm.addTaggedData(1689, Integer.valueOf(this.stats.naturalImportance));
            }
        }
        int i3 = this.mAssistantImportance;
        if (i3 != -1000) {
            lm.addTaggedData(1691, Integer.valueOf(i3));
        }
        String str = this.mAdjustmentIssuer;
        if (str != null) {
            lm.addTaggedData(1742, Integer.valueOf(str.hashCode()));
        }
        return lm;
    }

    public LogMaker getLogMaker() {
        return getLogMaker(System.currentTimeMillis());
    }

    public LogMaker getItemLogMaker() {
        return getLogMaker().setCategory(128);
    }

    public boolean hasUndecoratedRemoteView() {
        Notification notification = getNotification();
        boolean hasDecoratedStyle = notification.isStyle(Notification.DecoratedCustomViewStyle.class) || notification.isStyle(Notification.DecoratedMediaCustomViewStyle.class);
        boolean hasCustomRemoteView = (notification.contentView == null && notification.bigContentView == null && notification.headsUpContentView == null) ? false : true;
        return hasCustomRemoteView && !hasDecoratedStyle;
    }

    public void setShortcutInfo(ShortcutInfo shortcutInfo) {
        this.mShortcutInfo = shortcutInfo;
    }

    public ShortcutInfo getShortcutInfo() {
        return this.mShortcutInfo;
    }

    public void setHasSentValidMsg(boolean hasSentValidMsg) {
        this.mHasSentValidMsg = hasSentValidMsg;
    }

    public void userDemotedAppFromConvoSpace(boolean userDemoted) {
        this.mAppDemotedFromConvo = userDemoted;
    }

    public void setPkgAllowedAsConvo(boolean allowedAsConvo) {
        this.mPkgAllowedAsConvo = allowedAsConvo;
    }

    public boolean isConversation() {
        ShortcutInfo shortcutInfo;
        Notification notification = getNotification();
        if (this.mChannel.isDemoted() || this.mAppDemotedFromConvo || this.mIsNotConversationOverride) {
            return false;
        }
        if (!notification.isStyle(Notification.MessagingStyle.class)) {
            return this.mPkgAllowedAsConvo && this.mTargetSdkVersion < 30 && "msg".equals(getNotification().category);
        } else if (this.mTargetSdkVersion >= 30 && notification.isStyle(Notification.MessagingStyle.class) && ((shortcutInfo = this.mShortcutInfo) == null || isOnlyBots(shortcutInfo.getPersons()))) {
            return false;
        } else {
            return (this.mHasSentValidMsg && this.mShortcutInfo == null) ? false : true;
        }
    }

    private boolean isOnlyBots(Person[] persons) {
        if (persons == null || persons.length == 0) {
            return false;
        }
        for (Person person : persons) {
            if (!person.isBot()) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public StatusBarNotification getSbn() {
        return this.sbn;
    }

    public boolean rankingScoreMatches(float otherScore) {
        return ((double) Math.abs(this.mRankingScore - otherScore)) < 1.0E-4d;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setPendingLogUpdate(boolean pendingLogUpdate) {
        this.mPendingLogUpdate = pendingLogUpdate;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean hasPendingLogUpdate() {
        return this.mPendingLogUpdate;
    }

    public void mergePhoneNumbers(ArraySet<String> phoneNumbers) {
        if (phoneNumbers == null || phoneNumbers.size() == 0) {
            return;
        }
        if (this.mPhoneNumbers == null) {
            this.mPhoneNumbers = new ArraySet<>();
        }
        this.mPhoneNumbers.addAll((ArraySet<? extends String>) phoneNumbers);
    }

    public ArraySet<String> getPhoneNumbers() {
        return this.mPhoneNumbers;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class Light {
        public final int color;
        public final int offMs;
        public final int onMs;

        public Light(int color, int onMs, int offMs) {
            this.color = color;
            this.onMs = onMs;
            this.offMs = offMs;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Light light = (Light) o;
            if (this.color == light.color && this.onMs == light.onMs && this.offMs == light.offMs) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int result = this.color;
            return (((result * 31) + this.onMs) * 31) + this.offMs;
        }

        public String toString() {
            return "Light{color=" + this.color + ", onMs=" + this.onMs + ", offMs=" + this.offMs + '}';
        }
    }

    public void setImportance(int importance) {
        this.mInitSet = true;
        this.mImportance = importance;
    }
}
