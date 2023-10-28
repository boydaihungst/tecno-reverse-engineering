package com.android.server.notification;

import android.app.NotificationChannel;
import android.app.Person;
import android.os.Bundle;
import com.android.internal.art.ArtStatsLog;
import com.android.internal.logging.InstanceId;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.util.FrameworkStatsLog;
import java.util.ArrayList;
import java.util.Objects;
/* loaded from: classes2.dex */
public interface NotificationRecordLogger {
    void log(UiEventLogger.UiEventEnum uiEventEnum);

    void log(UiEventLogger.UiEventEnum uiEventEnum, NotificationRecord notificationRecord);

    void logNotificationAdjusted(NotificationRecord notificationRecord, int i, int i2, InstanceId instanceId);

    void maybeLogNotificationPosted(NotificationRecord notificationRecord, NotificationRecord notificationRecord2, int i, int i2, InstanceId instanceId);

    default void logNotificationCancelled(NotificationRecord r, int reason, int dismissalSurface) {
        log(NotificationCancelledEvent.fromCancelReason(reason, dismissalSurface), r);
    }

    default void logNotificationVisibility(NotificationRecord r, boolean visible) {
        log(NotificationEvent.fromVisibility(visible), r);
    }

    /* loaded from: classes2.dex */
    public enum NotificationReportedEvent implements UiEventLogger.UiEventEnum {
        NOTIFICATION_POSTED(162),
        NOTIFICATION_UPDATED(163),
        NOTIFICATION_ADJUSTED(908);
        
        private final int mId;

        NotificationReportedEvent(int id) {
            this.mId = id;
        }

        public int getId() {
            return this.mId;
        }

        public static NotificationReportedEvent fromRecordPair(NotificationRecordPair p) {
            return p.old != null ? NOTIFICATION_UPDATED : NOTIFICATION_POSTED;
        }
    }

    /* loaded from: classes2.dex */
    public enum NotificationCancelledEvent implements UiEventLogger.UiEventEnum {
        INVALID(0),
        NOTIFICATION_CANCEL_CLICK(164),
        NOTIFICATION_CANCEL_USER_OTHER(165),
        NOTIFICATION_CANCEL_USER_CANCEL_ALL(166),
        NOTIFICATION_CANCEL_ERROR(167),
        NOTIFICATION_CANCEL_PACKAGE_CHANGED(168),
        NOTIFICATION_CANCEL_USER_STOPPED(169),
        NOTIFICATION_CANCEL_PACKAGE_BANNED(170),
        NOTIFICATION_CANCEL_APP_CANCEL(FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__CROSS_PROFILE_SETTINGS_PAGE_USER_DECLINED_CONSENT),
        NOTIFICATION_CANCEL_APP_CANCEL_ALL(FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__CROSS_PROFILE_SETTINGS_PAGE_PERMISSION_REVOKED),
        NOTIFICATION_CANCEL_LISTENER_CANCEL(173),
        NOTIFICATION_CANCEL_LISTENER_CANCEL_ALL(FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__DOCSUI_EMPTY_STATE_QUIET_MODE),
        NOTIFICATION_CANCEL_GROUP_SUMMARY_CANCELED(FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__DOCSUI_LAUNCH_OTHER_APP),
        NOTIFICATION_CANCEL_GROUP_OPTIMIZATION(FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__DOCSUI_PICK_RESULT),
        NOTIFICATION_CANCEL_PACKAGE_SUSPENDED(177),
        NOTIFICATION_CANCEL_PROFILE_TURNED_OFF(178),
        NOTIFICATION_CANCEL_UNAUTOBUNDLED(179),
        NOTIFICATION_CANCEL_CHANNEL_BANNED(FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__CREDENTIAL_MANAGEMENT_APP_REQUEST_ACCEPTED),
        NOTIFICATION_CANCEL_SNOOZED(181),
        NOTIFICATION_CANCEL_TIMEOUT(FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__CREDENTIAL_MANAGEMENT_APP_REQUEST_FAILED),
        NOTIFICATION_CANCEL_USER_PEEK(FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PLATFORM_PROVISIONING_COPY_ACCOUNT_MS),
        NOTIFICATION_CANCEL_USER_AOD(FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PLATFORM_PROVISIONING_CREATE_PROFILE_MS),
        NOTIFICATION_CANCEL_USER_SHADE(192),
        NOTIFICATION_CANCEL_USER_LOCKSCREEN(193),
        NOTIFICATION_CANCEL_ASSISTANT(906);
        
        private final int mId;

        NotificationCancelledEvent(int id) {
            this.mId = id;
        }

        public int getId() {
            return this.mId;
        }

        public static NotificationCancelledEvent fromCancelReason(int reason, int surface) {
            if (surface == -1) {
                if (NotificationManagerService.DBG) {
                    throw new IllegalArgumentException("Unexpected surface " + surface);
                }
                return INVALID;
            } else if (surface == 0) {
                if (1 <= reason && reason <= 19) {
                    return values()[reason];
                }
                if (reason == 22) {
                    return NOTIFICATION_CANCEL_ASSISTANT;
                }
                if (NotificationManagerService.DBG) {
                    throw new IllegalArgumentException("Unexpected cancel reason " + reason);
                }
                return INVALID;
            } else if (reason != 2) {
                if (NotificationManagerService.DBG) {
                    throw new IllegalArgumentException("Unexpected cancel with surface " + reason);
                }
                return INVALID;
            } else {
                switch (surface) {
                    case 1:
                        return NOTIFICATION_CANCEL_USER_PEEK;
                    case 2:
                        return NOTIFICATION_CANCEL_USER_AOD;
                    case 3:
                        return NOTIFICATION_CANCEL_USER_SHADE;
                    default:
                        if (NotificationManagerService.DBG) {
                            throw new IllegalArgumentException("Unexpected surface for user-dismiss " + reason);
                        }
                        return INVALID;
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    public enum NotificationEvent implements UiEventLogger.UiEventEnum {
        NOTIFICATION_OPEN(197),
        NOTIFICATION_CLOSE(FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_USB_DATA_SIGNALING),
        NOTIFICATION_SNOOZED(FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_MEDIA_SESSION_CALLBACK),
        NOTIFICATION_NOT_POSTED_SNOOZED(319),
        NOTIFICATION_CLICKED(320),
        NOTIFICATION_ACTION_CLICKED(321),
        NOTIFICATION_DETAIL_OPEN_SYSTEM(FrameworkStatsLog.TIF_TUNE_CHANGED),
        NOTIFICATION_DETAIL_CLOSE_SYSTEM(FrameworkStatsLog.AUTO_ROTATE_REPORTED),
        NOTIFICATION_DETAIL_OPEN_USER(329),
        NOTIFICATION_DETAIL_CLOSE_USER(330),
        NOTIFICATION_DIRECT_REPLIED(331),
        NOTIFICATION_SMART_REPLIED(ArtStatsLog.ART_DATUM_REPORTED),
        NOTIFICATION_SMART_REPLY_VISIBLE(FrameworkStatsLog.DEVICE_ROTATED),
        NOTIFICATION_ACTION_CLICKED_0(450),
        NOTIFICATION_ACTION_CLICKED_1(FrameworkStatsLog.CDM_ASSOCIATION_ACTION),
        NOTIFICATION_ACTION_CLICKED_2(FrameworkStatsLog.MAGNIFICATION_TRIPLE_TAP_AND_HOLD_ACTIVATED_SESSION_REPORTED),
        NOTIFICATION_CONTEXTUAL_ACTION_CLICKED_0(FrameworkStatsLog.MAGNIFICATION_FOLLOW_TYPING_FOCUS_ACTIVATED_SESSION_REPORTED),
        NOTIFICATION_CONTEXTUAL_ACTION_CLICKED_1(454),
        NOTIFICATION_CONTEXTUAL_ACTION_CLICKED_2(455),
        NOTIFICATION_ASSIST_ACTION_CLICKED_0(456),
        NOTIFICATION_ASSIST_ACTION_CLICKED_1(ArtStatsLog.ISOLATED_COMPILATION_SCHEDULED),
        NOTIFICATION_ASSIST_ACTION_CLICKED_2(ArtStatsLog.ISOLATED_COMPILATION_ENDED);
        
        private final int mId;

        NotificationEvent(int id) {
            this.mId = id;
        }

        public int getId() {
            return this.mId;
        }

        public static NotificationEvent fromVisibility(boolean visible) {
            return visible ? NOTIFICATION_OPEN : NOTIFICATION_CLOSE;
        }

        public static NotificationEvent fromExpanded(boolean expanded, boolean userAction) {
            return userAction ? expanded ? NOTIFICATION_DETAIL_OPEN_USER : NOTIFICATION_DETAIL_CLOSE_USER : expanded ? NOTIFICATION_DETAIL_OPEN_SYSTEM : NOTIFICATION_DETAIL_CLOSE_SYSTEM;
        }

        public static NotificationEvent fromAction(int index, boolean isAssistant, boolean isContextual) {
            if (index < 0 || index > 2) {
                return NOTIFICATION_ACTION_CLICKED;
            }
            if (isAssistant) {
                return values()[NOTIFICATION_ASSIST_ACTION_CLICKED_0.ordinal() + index];
            }
            if (isContextual) {
                return values()[NOTIFICATION_CONTEXTUAL_ACTION_CLICKED_0.ordinal() + index];
            }
            return values()[NOTIFICATION_ACTION_CLICKED_0.ordinal() + index];
        }
    }

    /* loaded from: classes2.dex */
    public enum NotificationPanelEvent implements UiEventLogger.UiEventEnum {
        NOTIFICATION_PANEL_OPEN(FrameworkStatsLog.APP_PROCESS_DIED__IMPORTANCE__IMPORTANCE_TOP_SLEEPING),
        NOTIFICATION_PANEL_CLOSE(326);
        
        private final int mId;

        NotificationPanelEvent(int id) {
            this.mId = id;
        }

        public int getId() {
            return this.mId;
        }
    }

    /* loaded from: classes2.dex */
    public static class NotificationRecordPair {
        public final NotificationRecord old;
        public final NotificationRecord r;

        /* JADX INFO: Access modifiers changed from: package-private */
        public NotificationRecordPair(NotificationRecord r, NotificationRecord old) {
            this.r = r;
            this.old = old;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean shouldLogReported(int buzzBeepBlink) {
            NotificationRecord notificationRecord = this.r;
            if (notificationRecord == null) {
                return false;
            }
            if (this.old == null || buzzBeepBlink > 0) {
                return true;
            }
            return (Objects.equals(notificationRecord.getSbn().getChannelIdLogTag(), this.old.getSbn().getChannelIdLogTag()) && Objects.equals(this.r.getSbn().getGroupLogTag(), this.old.getSbn().getGroupLogTag()) && this.r.getSbn().getNotification().isGroupSummary() == this.old.getSbn().getNotification().isGroupSummary() && Objects.equals(this.r.getSbn().getNotification().category, this.old.getSbn().getNotification().category) && this.r.getImportance() == this.old.getImportance() && NotificationRecordLogger.getLoggingImportance(this.r) == NotificationRecordLogger.getLoggingImportance(this.old) && this.r.rankingScoreMatches(this.old.getRankingScore())) ? false : true;
        }

        public int getStyle() {
            return getStyle(this.r.getSbn().getNotification().extras);
        }

        private int getStyle(Bundle extras) {
            String template;
            if (extras != null && (template = extras.getString("android.template")) != null && !template.isEmpty()) {
                return template.hashCode();
            }
            return 0;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getNumPeople() {
            return getNumPeople(this.r.getSbn().getNotification().extras);
        }

        private int getNumPeople(Bundle extras) {
            ArrayList<Person> people;
            if (extras != null && (people = extras.getParcelableArrayList("android.people.list")) != null && !people.isEmpty()) {
                return people.size();
            }
            return 0;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getAssistantHash() {
            String assistant = this.r.getAdjustmentIssuer();
            if (assistant == null) {
                return 0;
            }
            return assistant.hashCode();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getInstanceId() {
            if (this.r.getSbn().getInstanceId() == null) {
                return 0;
            }
            return this.r.getSbn().getInstanceId().getId();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getNotificationIdHash() {
            return SmallHash.hash(Objects.hashCode(this.r.getSbn().getTag()) ^ this.r.getSbn().getId());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getChannelIdHash() {
            return SmallHash.hash(this.r.getSbn().getNotification().getChannelId());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getGroupIdHash() {
            return SmallHash.hash(this.r.getSbn().getGroup());
        }
    }

    static int getLoggingImportance(NotificationRecord r) {
        int importance = r.getImportance();
        NotificationChannel channel = r.getChannel();
        if (channel == null) {
            return importance;
        }
        return NotificationChannelLogger.getLoggingImportance(channel, importance);
    }

    static boolean isForegroundService(NotificationRecord r) {
        return (r.getSbn() == null || r.getSbn().getNotification() == null || (r.getSbn().getNotification().flags & 64) == 0) ? false : true;
    }
}
