package android.app;

import android.Manifest;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.app.AppOpsManager;
import android.app.blob.XmlTags;
import android.compat.Compatibility;
import android.content.AttributionSource;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.database.DatabaseUtils;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.media.MediaMetrics;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PackageTagsList;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.security.keystore.KeyProperties;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LongSparseArray;
import android.util.LongSparseLongArray;
import android.util.Pools;
import android.util.SparseArray;
import com.android.internal.app.IAppOpsActiveCallback;
import com.android.internal.app.IAppOpsAsyncNotedCallback;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsNotedCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IAppOpsStartedCallback;
import com.android.internal.app.MessageSamplingConfig;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.os.RuntimeInit;
import com.android.internal.os.ZygoteInit;
import com.android.internal.util.AnnotationValidations;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Parcelling;
import com.android.internal.util.Preconditions;
import com.mediatek.cta.CtaManager;
import com.mediatek.cta.CtaManagerFactory;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class AppOpsManager {
    public static final int ATTRIBUTION_CHAIN_ID_NONE = -1;
    public static final int ATTRIBUTION_FLAGS_NONE = 0;
    public static final int ATTRIBUTION_FLAG_ACCESSOR = 1;
    public static final int ATTRIBUTION_FLAG_INTERMEDIARY = 2;
    public static final int ATTRIBUTION_FLAG_RECEIVER = 4;
    public static final int ATTRIBUTION_FLAG_TRUSTED = 8;
    public static final long CALL_BACK_ON_CHANGED_LISTENER_WITH_SWITCHED_OP_CHANGE = 148180766;
    public static final int CALL_BACK_ON_SWITCHED_OP = 2;
    private static final int COLLECT_ASYNC = 3;
    private static final int COLLECT_SELF = 1;
    private static final int COLLECT_SYNC = 2;
    private static final String DEBUG_LOGGING_ENABLE_PROP = "appops.logging_enabled";
    private static final String DEBUG_LOGGING_OPS_PROP = "appops.logging_ops";
    private static final String DEBUG_LOGGING_PACKAGES_PROP = "appops.logging_packages";
    private static final String DEBUG_LOGGING_TAG = "AppOpsManager";
    private static final int DONT_COLLECT = 0;
    public static final int FILTER_BY_ATTRIBUTION_TAG = 4;
    public static final int FILTER_BY_OP_NAMES = 8;
    public static final int FILTER_BY_PACKAGE_NAME = 2;
    public static final int FILTER_BY_UID = 1;
    private static final int FLAGS_MASK = -1;
    private static final String FULL_LOG = "privacy_attribution_tag_full_log_enabled";
    public static final int HISTORICAL_MODE_DISABLED = 0;
    public static final int HISTORICAL_MODE_ENABLED_ACTIVE = 1;
    public static final int HISTORICAL_MODE_ENABLED_PASSIVE = 2;
    @SystemApi
    public static final int HISTORY_FLAGS_ALL = 3;
    @SystemApi
    public static final int HISTORY_FLAG_AGGREGATE = 1;
    @SystemApi
    public static final int HISTORY_FLAG_DISCRETE = 2;
    public static final int HISTORY_FLAG_GET_ATTRIBUTION_CHAINS = 4;
    public static final String KEY_BG_STATE_SETTLE_TIME = "bg_state_settle_time";
    public static final String KEY_FG_SERVICE_STATE_SETTLE_TIME = "fg_service_state_settle_time";
    public static final String KEY_HISTORICAL_OPS = "historical_ops";
    public static final String KEY_TOP_STATE_SETTLE_TIME = "top_state_settle_time";
    public static final int MAX_PRIORITY_UID_STATE = 100;
    private static final int MAX_UNFORWARDED_OPS = 10;
    public static final int MIN_PRIORITY_UID_STATE = 700;
    public static final int MODE_ALLOWED = 0;
    public static final int MODE_DEFAULT = 3;
    public static final int MODE_ERRORED = 2;
    public static final int MODE_FOREGROUND = 4;
    public static final int MODE_IGNORED = 1;
    public static final boolean NOTE_OP_COLLECTION_ENABLED = false;
    public static final int OP_ACCEPT_HANDOVER = 74;
    public static final int OP_ACCESS_ACCESSIBILITY = 88;
    public static final int OP_ACCESS_MEDIA_LOCATION = 90;
    public static final int OP_ACCESS_NOTIFICATIONS = 25;
    public static final int OP_ACCESS_RESTRICTED_SETTINGS = 119;
    public static final int OP_ACTIVATE_PLATFORM_VPN = 94;
    public static final int OP_ACTIVATE_VPN = 47;
    public static final int OP_ACTIVITY_RECOGNITION = 79;
    public static final int OP_ACTIVITY_RECOGNITION_SOURCE = 113;
    public static final int OP_ADD_VOICEMAIL = 52;
    public static final int OP_ANSWER_PHONE_CALLS = 69;
    public static final int OP_ASSIST_SCREENSHOT = 50;
    public static final int OP_ASSIST_STRUCTURE = 49;
    public static final int OP_AUDIO_ACCESSIBILITY_VOLUME = 64;
    public static final int OP_AUDIO_ALARM_VOLUME = 37;
    public static final int OP_AUDIO_BLUETOOTH_VOLUME = 39;
    public static final int OP_AUDIO_MASTER_VOLUME = 33;
    public static final int OP_AUDIO_MEDIA_VOLUME = 36;
    public static final int OP_AUDIO_NOTIFICATION_VOLUME = 38;
    public static final int OP_AUDIO_RING_VOLUME = 35;
    public static final int OP_AUDIO_VOICE_VOLUME = 34;
    public static final int OP_AUTO_REVOKE_MANAGED_BY_INSTALLER = 98;
    public static final int OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED = 97;
    public static final int OP_BIND_ACCESSIBILITY_SERVICE = 73;
    public static final int OP_BLUETOOTH_ADVERTISE = 114;
    public static final int OP_BLUETOOTH_CONNECT = 111;
    public static final int OP_BLUETOOTH_SCAN = 77;
    public static final int OP_BODY_SENSORS = 56;
    public static final int OP_CALL_PHONE = 13;
    public static final int OP_CAMERA = 26;
    public static final int OP_CHANGE_WIFI_STATE = 71;
    public static final int OP_COARSE_LOCATION = 0;
    public static final int OP_COARSE_LOCATION_SOURCE = 109;
    private static final int OP_DEPRECATED_1 = 96;
    public static final int OP_ESTABLISH_VPN_MANAGER = 118;
    public static final int OP_ESTABLISH_VPN_SERVICE = 117;
    public static final int OP_FINE_LOCATION = 1;
    public static final int OP_FINE_LOCATION_SOURCE = 108;
    @SystemApi
    public static final int OP_FLAGS_ALL = 31;
    @SystemApi
    public static final int OP_FLAGS_ALL_TRUSTED = 13;
    @SystemApi
    public static final int OP_FLAG_SELF = 1;
    @SystemApi
    public static final int OP_FLAG_TRUSTED_PROXIED = 8;
    @SystemApi
    public static final int OP_FLAG_TRUSTED_PROXY = 2;
    @SystemApi
    public static final int OP_FLAG_UNTRUSTED_PROXIED = 16;
    @SystemApi
    public static final int OP_FLAG_UNTRUSTED_PROXY = 4;
    public static final int OP_GET_ACCOUNTS = 62;
    public static final int OP_GET_USAGE_STATS = 43;
    public static final int OP_GPS = 2;
    public static final int OP_INSTANT_APP_START_FOREGROUND = 68;
    public static final int OP_INTERACT_ACROSS_PROFILES = 93;
    public static final int OP_LEGACY_STORAGE = 87;
    public static final int OP_LOADER_USAGE_STATS = 95;
    public static final int OP_MANAGE_CREDENTIALS = 104;
    public static final int OP_MANAGE_EXTERNAL_STORAGE = 92;
    public static final int OP_MANAGE_IPSEC_TUNNELS = 75;
    public static final int OP_MANAGE_MEDIA = 110;
    public static final int OP_MANAGE_ONGOING_CALLS = 103;
    public static final int OP_MOCK_LOCATION = 58;
    public static final int OP_MONITOR_HIGH_POWER_LOCATION = 42;
    public static final int OP_MONITOR_LOCATION = 41;
    public static final int OP_MUTE_MICROPHONE = 44;
    public static final int OP_NEARBY_WIFI_DEVICES = 116;
    public static final int OP_NEIGHBORING_CELLS = 12;
    public static final int OP_NONE = -1;
    public static final int OP_NO_ISOLATED_STORAGE = 99;
    public static final int OP_PHONE_CALL_CAMERA = 101;
    public static final int OP_PHONE_CALL_MICROPHONE = 100;
    public static final int OP_PICTURE_IN_PICTURE = 67;
    public static final int OP_PLAY_AUDIO = 28;
    public static final int OP_POST_NOTIFICATION = 11;
    public static final int OP_PROCESS_OUTGOING_CALLS = 54;
    public static final int OP_PROJECT_MEDIA = 46;
    public static final int OP_QUERY_ALL_PACKAGES = 91;
    public static final int OP_READ_CALENDAR = 8;
    public static final int OP_READ_CALL_LOG = 6;
    public static final int OP_READ_CELL_BROADCASTS = 57;
    public static final int OP_READ_CLIPBOARD = 29;
    public static final int OP_READ_CONTACTS = 4;
    public static final int OP_READ_DEVICE_IDENTIFIERS = 89;
    public static final int OP_READ_EXTERNAL_STORAGE = 59;
    public static final int OP_READ_ICC_SMS = 21;
    public static final int OP_READ_MEDIA_AUDIO = 81;
    public static final int OP_READ_MEDIA_IMAGES = 85;
    public static final int OP_READ_MEDIA_VIDEO = 83;
    public static final int OP_READ_PHONE_NUMBERS = 65;
    public static final int OP_READ_PHONE_STATE = 51;
    public static final int OP_READ_SMS = 14;
    public static final int OP_RECEIVE_AMBIENT_TRIGGER_AUDIO = 120;
    public static final int OP_RECEIVE_EMERGECY_SMS = 17;
    public static final int OP_RECEIVE_MMS = 18;
    public static final int OP_RECEIVE_SMS = 16;
    public static final int OP_RECEIVE_WAP_PUSH = 19;
    public static final int OP_RECORD_AUDIO = 27;
    public static final int OP_RECORD_AUDIO_HOTWORD = 102;
    public static final int OP_RECORD_AUDIO_OUTPUT = 106;
    public static final int OP_RECORD_INCOMING_PHONE_AUDIO = 115;
    public static final int OP_REQUEST_DELETE_PACKAGES = 72;
    public static final int OP_REQUEST_INSTALL_PACKAGES = 66;
    public static final int OP_RUN_ANY_IN_BACKGROUND = 70;
    public static final int OP_RUN_IN_BACKGROUND = 63;
    public static final int OP_SCHEDULE_EXACT_ALARM = 107;
    public static final int OP_SEND_SMS = 20;
    public static final int OP_SMS_FINANCIAL_TRANSACTIONS = 80;
    public static final int OP_START_FOREGROUND = 76;
    public static final int OP_SYSTEM_ALERT_WINDOW = 24;
    public static final int OP_TAKE_AUDIO_FOCUS = 32;
    public static final int OP_TAKE_MEDIA_BUTTONS = 31;
    public static final int OP_TOAST_WINDOW = 45;
    public static final int OP_TURN_SCREEN_ON = 61;
    public static final int OP_USE_BIOMETRIC = 78;
    public static final int OP_USE_FINGERPRINT = 55;
    public static final int OP_USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER = 105;
    public static final int OP_USE_SIP = 53;
    public static final int OP_UWB_RANGING = 112;
    public static final int OP_VIBRATE = 3;
    public static final int OP_WAKE_LOCK = 40;
    public static final int OP_WIFI_SCAN = 10;
    public static final int OP_WRITE_CALENDAR = 9;
    public static final int OP_WRITE_CALL_LOG = 7;
    public static final int OP_WRITE_CLIPBOARD = 30;
    public static final int OP_WRITE_CONTACTS = 5;
    public static final int OP_WRITE_EXTERNAL_STORAGE = 60;
    public static final int OP_WRITE_ICC_SMS = 22;
    public static final int OP_WRITE_MEDIA_AUDIO = 82;
    public static final int OP_WRITE_MEDIA_IMAGES = 86;
    public static final int OP_WRITE_MEDIA_VIDEO = 84;
    public static final int OP_WRITE_SETTINGS = 23;
    public static final int OP_WRITE_SMS = 15;
    public static final int OP_WRITE_WALLPAPER = 48;
    public static final int SAMPLING_STRATEGY_BOOT_TIME_SAMPLING = 3;
    public static final int SAMPLING_STRATEGY_DEFAULT = 0;
    public static final int SAMPLING_STRATEGY_RARELY_USED = 2;
    public static final int SAMPLING_STRATEGY_UNIFORM = 1;
    public static final int SAMPLING_STRATEGY_UNIFORM_OPS = 4;
    public static final long SECURITY_EXCEPTION_ON_INVALID_ATTRIBUTION_TAG_CHANGE = 151105954;
    private static final byte SHOULD_COLLECT_NOTE_OP = 2;
    private static final byte SHOULD_COLLECT_NOTE_OP_NOT_INITIALIZED = 0;
    private static final byte SHOULD_NOT_COLLECT_NOTE_OP = 1;
    @SystemApi
    public static final int UID_STATE_BACKGROUND = 600;
    @SystemApi
    public static final int UID_STATE_CACHED = 700;
    @SystemApi
    public static final int UID_STATE_FOREGROUND = 500;
    @SystemApi
    public static final int UID_STATE_FOREGROUND_SERVICE = 400;
    @SystemApi
    @Deprecated
    public static final int UID_STATE_FOREGROUND_SERVICE_LOCATION = 300;
    public static final int UID_STATE_MAX_LAST_NON_RESTRICTED = 500;
    private static final int UID_STATE_OFFSET = 31;
    @SystemApi
    public static final int UID_STATE_PERSISTENT = 100;
    @SystemApi
    public static final int UID_STATE_TOP = 200;
    public static final int WATCH_FOREGROUND_CHANGES = 1;
    public static final int _NUM_OP = 121;
    private static final int _NUM_OP_INCLUDE_CTA = 125;
    static IBinder sClientId;
    private static MessageSamplingConfig sConfig;
    private static OnOpNotedCallback sOnOpNotedCallback;
    static IAppOpsService sService;
    final Context mContext;
    final IAppOpsService mService;
    private static Boolean sFullLog = null;
    private static final Object sLock = new Object();
    private static ArrayList<AsyncNotedAppOp> sUnforwardedOps = new ArrayList<>();
    private static OnOpNotedCallback sMessageCollector = new OnOpNotedCallback() { // from class: android.app.AppOpsManager.1
        @Override // android.app.AppOpsManager.OnOpNotedCallback
        public void onNoted(SyncNotedAppOp op) {
            reportStackTraceIfNeeded(op);
        }

        @Override // android.app.AppOpsManager.OnOpNotedCallback
        public void onAsyncNoted(AsyncNotedAppOp asyncOp) {
        }

        @Override // android.app.AppOpsManager.OnOpNotedCallback
        public void onSelfNoted(SyncNotedAppOp op) {
            reportStackTraceIfNeeded(op);
        }

        private void reportStackTraceIfNeeded(SyncNotedAppOp op) {
            if (!AppOpsManager.m200$$Nest$smisCollectingStackTraces()) {
                return;
            }
            MessageSamplingConfig config = AppOpsManager.sConfig;
            if (AppOpsManager.leftCircularDistance(AppOpsManager.strOpToOp(op.getOp()), config.getSampledOpCode(), 121) <= config.getAcceptableLeftDistance() || config.getExpirationTimeSinceBootMillis() < SystemClock.elapsedRealtime()) {
                String stackTrace = AppOpsManager.m197$$Nest$smgetFormattedStackTrace();
                try {
                    String packageName = ActivityThread.currentOpPackageName();
                    AppOpsManager.sConfig = AppOpsManager.m199$$Nest$smgetService().reportRuntimeAppOpAccessMessageAndGetConfig(packageName == null ? "" : packageName, op, stackTrace);
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
            }
        }
    };
    public static final String[] MODE_NAMES = {"allow", "ignore", "deny", "default", "foreground"};
    public static final int[] UID_STATES = {100, 200, 300, 400, 500, 600, 700};
    private static final CtaManager sCtaManager = CtaManagerFactory.getInstance().makeCtaManager();
    private static final int[] RUNTIME_AND_APPOP_PERMISSIONS_OPS = {4, 5, 62, 8, 9, 20, 16, 14, 19, 18, 57, 59, 60, 90, 0, 1, 51, 65, 13, 6, 7, 52, 53, 54, 69, 74, 27, 26, 56, 79, 81, 82, 83, 84, 85, 86, 77, 111, 114, 112, 116, 11, 25, 24, 23, 66, 76, 80, 75, 68, 92, 93, 95, 103, 105, 107, 110};
    private static int[] sOpToSwitch = {0, 1, 0, 3, 4, 5, 6, 7, 8, 9, 0, 11, 0, 13, 14, 15, 16, 16, 18, 19, 20, 14, 15, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 0, 0, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 1, 0, 110, 111, 112, 79, 114, 115, 116, 117, 118, 119, 120};
    public static final String OPSTR_COARSE_LOCATION = "android:coarse_location";
    public static final String OPSTR_FINE_LOCATION = "android:fine_location";
    @SystemApi
    public static final String OPSTR_GPS = "android:gps";
    @SystemApi
    public static final String OPSTR_VIBRATE = "android:vibrate";
    public static final String OPSTR_READ_CONTACTS = "android:read_contacts";
    public static final String OPSTR_WRITE_CONTACTS = "android:write_contacts";
    public static final String OPSTR_READ_CALL_LOG = "android:read_call_log";
    public static final String OPSTR_WRITE_CALL_LOG = "android:write_call_log";
    public static final String OPSTR_READ_CALENDAR = "android:read_calendar";
    public static final String OPSTR_WRITE_CALENDAR = "android:write_calendar";
    @SystemApi
    public static final String OPSTR_WIFI_SCAN = "android:wifi_scan";
    @SystemApi
    public static final String OPSTR_POST_NOTIFICATION = "android:post_notification";
    @SystemApi
    public static final String OPSTR_NEIGHBORING_CELLS = "android:neighboring_cells";
    public static final String OPSTR_CALL_PHONE = "android:call_phone";
    public static final String OPSTR_READ_SMS = "android:read_sms";
    @SystemApi
    public static final String OPSTR_WRITE_SMS = "android:write_sms";
    public static final String OPSTR_RECEIVE_SMS = "android:receive_sms";
    @SystemApi
    public static final String OPSTR_RECEIVE_EMERGENCY_BROADCAST = "android:receive_emergency_broadcast";
    public static final String OPSTR_RECEIVE_MMS = "android:receive_mms";
    public static final String OPSTR_RECEIVE_WAP_PUSH = "android:receive_wap_push";
    public static final String OPSTR_SEND_SMS = "android:send_sms";
    @SystemApi
    public static final String OPSTR_READ_ICC_SMS = "android:read_icc_sms";
    @SystemApi
    public static final String OPSTR_WRITE_ICC_SMS = "android:write_icc_sms";
    public static final String OPSTR_WRITE_SETTINGS = "android:write_settings";
    public static final String OPSTR_SYSTEM_ALERT_WINDOW = "android:system_alert_window";
    @SystemApi
    public static final String OPSTR_ACCESS_NOTIFICATIONS = "android:access_notifications";
    public static final String OPSTR_CAMERA = "android:camera";
    public static final String OPSTR_RECORD_AUDIO = "android:record_audio";
    @SystemApi
    public static final String OPSTR_PLAY_AUDIO = "android:play_audio";
    @SystemApi
    public static final String OPSTR_READ_CLIPBOARD = "android:read_clipboard";
    @SystemApi
    public static final String OPSTR_WRITE_CLIPBOARD = "android:write_clipboard";
    @SystemApi
    public static final String OPSTR_TAKE_MEDIA_BUTTONS = "android:take_media_buttons";
    @SystemApi
    public static final String OPSTR_TAKE_AUDIO_FOCUS = "android:take_audio_focus";
    @SystemApi
    public static final String OPSTR_AUDIO_MASTER_VOLUME = "android:audio_master_volume";
    @SystemApi
    public static final String OPSTR_AUDIO_VOICE_VOLUME = "android:audio_voice_volume";
    @SystemApi
    public static final String OPSTR_AUDIO_RING_VOLUME = "android:audio_ring_volume";
    @SystemApi
    public static final String OPSTR_AUDIO_MEDIA_VOLUME = "android:audio_media_volume";
    @SystemApi
    public static final String OPSTR_AUDIO_ALARM_VOLUME = "android:audio_alarm_volume";
    @SystemApi
    public static final String OPSTR_AUDIO_NOTIFICATION_VOLUME = "android:audio_notification_volume";
    @SystemApi
    public static final String OPSTR_AUDIO_BLUETOOTH_VOLUME = "android:audio_bluetooth_volume";
    @SystemApi
    public static final String OPSTR_WAKE_LOCK = "android:wake_lock";
    public static final String OPSTR_MONITOR_LOCATION = "android:monitor_location";
    public static final String OPSTR_MONITOR_HIGH_POWER_LOCATION = "android:monitor_location_high_power";
    public static final String OPSTR_GET_USAGE_STATS = "android:get_usage_stats";
    @SystemApi
    public static final String OPSTR_MUTE_MICROPHONE = "android:mute_microphone";
    @SystemApi
    public static final String OPSTR_TOAST_WINDOW = "android:toast_window";
    @SystemApi
    public static final String OPSTR_PROJECT_MEDIA = "android:project_media";
    @SystemApi
    public static final String OPSTR_ACTIVATE_VPN = "android:activate_vpn";
    @SystemApi
    public static final String OPSTR_WRITE_WALLPAPER = "android:write_wallpaper";
    @SystemApi
    public static final String OPSTR_ASSIST_STRUCTURE = "android:assist_structure";
    @SystemApi
    public static final String OPSTR_ASSIST_SCREENSHOT = "android:assist_screenshot";
    public static final String OPSTR_READ_PHONE_STATE = "android:read_phone_state";
    public static final String OPSTR_ADD_VOICEMAIL = "android:add_voicemail";
    public static final String OPSTR_USE_SIP = "android:use_sip";
    public static final String OPSTR_PROCESS_OUTGOING_CALLS = "android:process_outgoing_calls";
    public static final String OPSTR_USE_FINGERPRINT = "android:use_fingerprint";
    public static final String OPSTR_BODY_SENSORS = "android:body_sensors";
    public static final String OPSTR_READ_CELL_BROADCASTS = "android:read_cell_broadcasts";
    public static final String OPSTR_MOCK_LOCATION = "android:mock_location";
    public static final String OPSTR_READ_EXTERNAL_STORAGE = "android:read_external_storage";
    public static final String OPSTR_WRITE_EXTERNAL_STORAGE = "android:write_external_storage";
    @SystemApi
    public static final String OPSTR_TURN_SCREEN_ON = "android:turn_screen_on";
    @SystemApi
    public static final String OPSTR_GET_ACCOUNTS = "android:get_accounts";
    @SystemApi
    public static final String OPSTR_RUN_IN_BACKGROUND = "android:run_in_background";
    @SystemApi
    public static final String OPSTR_AUDIO_ACCESSIBILITY_VOLUME = "android:audio_accessibility_volume";
    public static final String OPSTR_READ_PHONE_NUMBERS = "android:read_phone_numbers";
    @SystemApi
    public static final String OPSTR_REQUEST_INSTALL_PACKAGES = "android:request_install_packages";
    public static final String OPSTR_PICTURE_IN_PICTURE = "android:picture_in_picture";
    @SystemApi
    public static final String OPSTR_INSTANT_APP_START_FOREGROUND = "android:instant_app_start_foreground";
    public static final String OPSTR_ANSWER_PHONE_CALLS = "android:answer_phone_calls";
    @SystemApi
    public static final String OPSTR_RUN_ANY_IN_BACKGROUND = "android:run_any_in_background";
    @SystemApi
    public static final String OPSTR_CHANGE_WIFI_STATE = "android:change_wifi_state";
    @SystemApi
    public static final String OPSTR_REQUEST_DELETE_PACKAGES = "android:request_delete_packages";
    @SystemApi
    public static final String OPSTR_BIND_ACCESSIBILITY_SERVICE = "android:bind_accessibility_service";
    @SystemApi
    public static final String OPSTR_ACCEPT_HANDOVER = "android:accept_handover";
    @SystemApi
    public static final String OPSTR_MANAGE_IPSEC_TUNNELS = "android:manage_ipsec_tunnels";
    @SystemApi
    public static final String OPSTR_START_FOREGROUND = "android:start_foreground";
    public static final String OPSTR_BLUETOOTH_SCAN = "android:bluetooth_scan";
    public static final String OPSTR_USE_BIOMETRIC = "android:use_biometric";
    public static final String OPSTR_ACTIVITY_RECOGNITION = "android:activity_recognition";
    public static final String OPSTR_SMS_FINANCIAL_TRANSACTIONS = "android:sms_financial_transactions";
    @SystemApi
    public static final String OPSTR_READ_MEDIA_AUDIO = "android:read_media_audio";
    @SystemApi
    public static final String OPSTR_WRITE_MEDIA_AUDIO = "android:write_media_audio";
    @SystemApi
    public static final String OPSTR_READ_MEDIA_VIDEO = "android:read_media_video";
    @SystemApi
    public static final String OPSTR_WRITE_MEDIA_VIDEO = "android:write_media_video";
    @SystemApi
    public static final String OPSTR_READ_MEDIA_IMAGES = "android:read_media_images";
    @SystemApi
    public static final String OPSTR_WRITE_MEDIA_IMAGES = "android:write_media_images";
    @SystemApi
    public static final String OPSTR_LEGACY_STORAGE = "android:legacy_storage";
    @SystemApi
    public static final String OPSTR_ACCESS_ACCESSIBILITY = "android:access_accessibility";
    public static final String OPSTR_READ_DEVICE_IDENTIFIERS = "android:read_device_identifiers";
    public static final String OPSTR_ACCESS_MEDIA_LOCATION = "android:access_media_location";
    public static final String OPSTR_QUERY_ALL_PACKAGES = "android:query_all_packages";
    @SystemApi
    public static final String OPSTR_MANAGE_EXTERNAL_STORAGE = "android:manage_external_storage";
    @SystemApi
    public static final String OPSTR_INTERACT_ACROSS_PROFILES = "android:interact_across_profiles";
    @SystemApi
    public static final String OPSTR_ACTIVATE_PLATFORM_VPN = "android:activate_platform_vpn";
    @SystemApi
    public static final String OPSTR_LOADER_USAGE_STATS = "android:loader_usage_stats";
    @SystemApi
    public static final String OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED = "android:auto_revoke_permissions_if_unused";
    @SystemApi
    public static final String OPSTR_AUTO_REVOKE_MANAGED_BY_INSTALLER = "android:auto_revoke_managed_by_installer";
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String OPSTR_NO_ISOLATED_STORAGE = "android:no_isolated_storage";
    @SystemApi
    public static final String OPSTR_PHONE_CALL_MICROPHONE = "android:phone_call_microphone";
    @SystemApi
    public static final String OPSTR_PHONE_CALL_CAMERA = "android:phone_call_camera";
    public static final String OPSTR_RECORD_AUDIO_HOTWORD = "android:record_audio_hotword";
    @SystemApi
    public static final String OPSTR_MANAGE_ONGOING_CALLS = "android:manage_ongoing_calls";
    public static final String OPSTR_MANAGE_CREDENTIALS = "android:manage_credentials";
    public static final String OPSTR_USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER = "android:use_icc_auth_with_device_identifier";
    public static final String OPSTR_RECORD_AUDIO_OUTPUT = "android:record_audio_output";
    public static final String OPSTR_SCHEDULE_EXACT_ALARM = "android:schedule_exact_alarm";
    public static final String OPSTR_FINE_LOCATION_SOURCE = "android:fine_location_source";
    public static final String OPSTR_COARSE_LOCATION_SOURCE = "android:coarse_location_source";
    public static final String OPSTR_MANAGE_MEDIA = "android:manage_media";
    public static final String OPSTR_BLUETOOTH_CONNECT = "android:bluetooth_connect";
    public static final String OPSTR_UWB_RANGING = "android:uwb_ranging";
    public static final String OPSTR_ACTIVITY_RECOGNITION_SOURCE = "android:activity_recognition_source";
    public static final String OPSTR_BLUETOOTH_ADVERTISE = "android:bluetooth_advertise";
    public static final String OPSTR_RECORD_INCOMING_PHONE_AUDIO = "android:record_incoming_phone_audio";
    public static final String OPSTR_NEARBY_WIFI_DEVICES = "android:nearby_wifi_devices";
    @SystemApi
    public static final String OPSTR_ESTABLISH_VPN_SERVICE = "android:establish_vpn_service";
    @SystemApi
    public static final String OPSTR_ESTABLISH_VPN_MANAGER = "android:establish_vpn_manager";
    public static final String OPSTR_ACCESS_RESTRICTED_SETTINGS = "android:access_restricted_settings";
    public static final String OPSTR_RECEIVE_AMBIENT_TRIGGER_AUDIO = "android:receive_ambient_trigger_audio";
    private static String[] sOpToString = {OPSTR_COARSE_LOCATION, OPSTR_FINE_LOCATION, OPSTR_GPS, OPSTR_VIBRATE, OPSTR_READ_CONTACTS, OPSTR_WRITE_CONTACTS, OPSTR_READ_CALL_LOG, OPSTR_WRITE_CALL_LOG, OPSTR_READ_CALENDAR, OPSTR_WRITE_CALENDAR, OPSTR_WIFI_SCAN, OPSTR_POST_NOTIFICATION, OPSTR_NEIGHBORING_CELLS, OPSTR_CALL_PHONE, OPSTR_READ_SMS, OPSTR_WRITE_SMS, OPSTR_RECEIVE_SMS, OPSTR_RECEIVE_EMERGENCY_BROADCAST, OPSTR_RECEIVE_MMS, OPSTR_RECEIVE_WAP_PUSH, OPSTR_SEND_SMS, OPSTR_READ_ICC_SMS, OPSTR_WRITE_ICC_SMS, OPSTR_WRITE_SETTINGS, OPSTR_SYSTEM_ALERT_WINDOW, OPSTR_ACCESS_NOTIFICATIONS, OPSTR_CAMERA, OPSTR_RECORD_AUDIO, OPSTR_PLAY_AUDIO, OPSTR_READ_CLIPBOARD, OPSTR_WRITE_CLIPBOARD, OPSTR_TAKE_MEDIA_BUTTONS, OPSTR_TAKE_AUDIO_FOCUS, OPSTR_AUDIO_MASTER_VOLUME, OPSTR_AUDIO_VOICE_VOLUME, OPSTR_AUDIO_RING_VOLUME, OPSTR_AUDIO_MEDIA_VOLUME, OPSTR_AUDIO_ALARM_VOLUME, OPSTR_AUDIO_NOTIFICATION_VOLUME, OPSTR_AUDIO_BLUETOOTH_VOLUME, OPSTR_WAKE_LOCK, OPSTR_MONITOR_LOCATION, OPSTR_MONITOR_HIGH_POWER_LOCATION, OPSTR_GET_USAGE_STATS, OPSTR_MUTE_MICROPHONE, OPSTR_TOAST_WINDOW, OPSTR_PROJECT_MEDIA, OPSTR_ACTIVATE_VPN, OPSTR_WRITE_WALLPAPER, OPSTR_ASSIST_STRUCTURE, OPSTR_ASSIST_SCREENSHOT, OPSTR_READ_PHONE_STATE, OPSTR_ADD_VOICEMAIL, OPSTR_USE_SIP, OPSTR_PROCESS_OUTGOING_CALLS, OPSTR_USE_FINGERPRINT, OPSTR_BODY_SENSORS, OPSTR_READ_CELL_BROADCASTS, OPSTR_MOCK_LOCATION, OPSTR_READ_EXTERNAL_STORAGE, OPSTR_WRITE_EXTERNAL_STORAGE, OPSTR_TURN_SCREEN_ON, OPSTR_GET_ACCOUNTS, OPSTR_RUN_IN_BACKGROUND, OPSTR_AUDIO_ACCESSIBILITY_VOLUME, OPSTR_READ_PHONE_NUMBERS, OPSTR_REQUEST_INSTALL_PACKAGES, OPSTR_PICTURE_IN_PICTURE, OPSTR_INSTANT_APP_START_FOREGROUND, OPSTR_ANSWER_PHONE_CALLS, OPSTR_RUN_ANY_IN_BACKGROUND, OPSTR_CHANGE_WIFI_STATE, OPSTR_REQUEST_DELETE_PACKAGES, OPSTR_BIND_ACCESSIBILITY_SERVICE, OPSTR_ACCEPT_HANDOVER, OPSTR_MANAGE_IPSEC_TUNNELS, OPSTR_START_FOREGROUND, OPSTR_BLUETOOTH_SCAN, OPSTR_USE_BIOMETRIC, OPSTR_ACTIVITY_RECOGNITION, OPSTR_SMS_FINANCIAL_TRANSACTIONS, OPSTR_READ_MEDIA_AUDIO, OPSTR_WRITE_MEDIA_AUDIO, OPSTR_READ_MEDIA_VIDEO, OPSTR_WRITE_MEDIA_VIDEO, OPSTR_READ_MEDIA_IMAGES, OPSTR_WRITE_MEDIA_IMAGES, OPSTR_LEGACY_STORAGE, OPSTR_ACCESS_ACCESSIBILITY, OPSTR_READ_DEVICE_IDENTIFIERS, OPSTR_ACCESS_MEDIA_LOCATION, OPSTR_QUERY_ALL_PACKAGES, OPSTR_MANAGE_EXTERNAL_STORAGE, OPSTR_INTERACT_ACROSS_PROFILES, OPSTR_ACTIVATE_PLATFORM_VPN, OPSTR_LOADER_USAGE_STATS, "", OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED, OPSTR_AUTO_REVOKE_MANAGED_BY_INSTALLER, OPSTR_NO_ISOLATED_STORAGE, OPSTR_PHONE_CALL_MICROPHONE, OPSTR_PHONE_CALL_CAMERA, OPSTR_RECORD_AUDIO_HOTWORD, OPSTR_MANAGE_ONGOING_CALLS, OPSTR_MANAGE_CREDENTIALS, OPSTR_USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER, OPSTR_RECORD_AUDIO_OUTPUT, OPSTR_SCHEDULE_EXACT_ALARM, OPSTR_FINE_LOCATION_SOURCE, OPSTR_COARSE_LOCATION_SOURCE, OPSTR_MANAGE_MEDIA, OPSTR_BLUETOOTH_CONNECT, OPSTR_UWB_RANGING, OPSTR_ACTIVITY_RECOGNITION_SOURCE, OPSTR_BLUETOOTH_ADVERTISE, OPSTR_RECORD_INCOMING_PHONE_AUDIO, OPSTR_NEARBY_WIFI_DEVICES, OPSTR_ESTABLISH_VPN_SERVICE, OPSTR_ESTABLISH_VPN_MANAGER, OPSTR_ACCESS_RESTRICTED_SETTINGS, OPSTR_RECEIVE_AMBIENT_TRIGGER_AUDIO};
    private static String[] sOpNames = {"COARSE_LOCATION", "FINE_LOCATION", "GPS", "VIBRATE", "READ_CONTACTS", "WRITE_CONTACTS", "READ_CALL_LOG", "WRITE_CALL_LOG", "READ_CALENDAR", "WRITE_CALENDAR", "WIFI_SCAN", "POST_NOTIFICATION", "NEIGHBORING_CELLS", "CALL_PHONE", "READ_SMS", "WRITE_SMS", "RECEIVE_SMS", "RECEIVE_EMERGECY_SMS", "RECEIVE_MMS", "RECEIVE_WAP_PUSH", "SEND_SMS", "READ_ICC_SMS", "WRITE_ICC_SMS", "WRITE_SETTINGS", "SYSTEM_ALERT_WINDOW", "ACCESS_NOTIFICATIONS", "CAMERA", "RECORD_AUDIO", "PLAY_AUDIO", "READ_CLIPBOARD", "WRITE_CLIPBOARD", "TAKE_MEDIA_BUTTONS", "TAKE_AUDIO_FOCUS", "AUDIO_MASTER_VOLUME", "AUDIO_VOICE_VOLUME", "AUDIO_RING_VOLUME", "AUDIO_MEDIA_VOLUME", "AUDIO_ALARM_VOLUME", "AUDIO_NOTIFICATION_VOLUME", "AUDIO_BLUETOOTH_VOLUME", "WAKE_LOCK", "MONITOR_LOCATION", "MONITOR_HIGH_POWER_LOCATION", "GET_USAGE_STATS", "MUTE_MICROPHONE", "TOAST_WINDOW", "PROJECT_MEDIA", "ACTIVATE_VPN", "WRITE_WALLPAPER", "ASSIST_STRUCTURE", "ASSIST_SCREENSHOT", "READ_PHONE_STATE", "ADD_VOICEMAIL", "USE_SIP", "PROCESS_OUTGOING_CALLS", "USE_FINGERPRINT", "BODY_SENSORS", "READ_CELL_BROADCASTS", "MOCK_LOCATION", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "TURN_ON_SCREEN", "GET_ACCOUNTS", "RUN_IN_BACKGROUND", "AUDIO_ACCESSIBILITY_VOLUME", "READ_PHONE_NUMBERS", "REQUEST_INSTALL_PACKAGES", "PICTURE_IN_PICTURE", "INSTANT_APP_START_FOREGROUND", "ANSWER_PHONE_CALLS", "RUN_ANY_IN_BACKGROUND", "CHANGE_WIFI_STATE", "REQUEST_DELETE_PACKAGES", "BIND_ACCESSIBILITY_SERVICE", "ACCEPT_HANDOVER", "MANAGE_IPSEC_TUNNELS", "START_FOREGROUND", "BLUETOOTH_SCAN", "USE_BIOMETRIC", "ACTIVITY_RECOGNITION", "SMS_FINANCIAL_TRANSACTIONS", "READ_MEDIA_AUDIO", "WRITE_MEDIA_AUDIO", "READ_MEDIA_VIDEO", "WRITE_MEDIA_VIDEO", "READ_MEDIA_IMAGES", "WRITE_MEDIA_IMAGES", "LEGACY_STORAGE", "ACCESS_ACCESSIBILITY", "READ_DEVICE_IDENTIFIERS", "ACCESS_MEDIA_LOCATION", "QUERY_ALL_PACKAGES", "MANAGE_EXTERNAL_STORAGE", "INTERACT_ACROSS_PROFILES", "ACTIVATE_PLATFORM_VPN", "LOADER_USAGE_STATS", "deprecated", "AUTO_REVOKE_PERMISSIONS_IF_UNUSED", "AUTO_REVOKE_MANAGED_BY_INSTALLER", "NO_ISOLATED_STORAGE", "PHONE_CALL_MICROPHONE", "PHONE_CALL_CAMERA", "RECORD_AUDIO_HOTWORD", "MANAGE_ONGOING_CALLS", "MANAGE_CREDENTIALS", "USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER", "RECORD_AUDIO_OUTPUT", "SCHEDULE_EXACT_ALARM", "FINE_LOCATION_SOURCE", "COARSE_LOCATION_SOURCE", "MANAGE_MEDIA", "BLUETOOTH_CONNECT", "UWB_RANGING", "ACTIVITY_RECOGNITION_SOURCE", "BLUETOOTH_ADVERTISE", "RECORD_INCOMING_PHONE_AUDIO", "NEARBY_WIFI_DEVICES", "ESTABLISH_VPN_SERVICE", "ESTABLISH_VPN_MANAGER", "ACCESS_RESTRICTED_SETTINGS", "RECEIVE_SOUNDTRIGGER_AUDIO"};
    private static String[] sOpPerms = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, null, Manifest.permission.VIBRATE, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.POST_NOTIFICATIONS, null, Manifest.permission.CALL_PHONE, Manifest.permission.READ_SMS, null, Manifest.permission.RECEIVE_SMS, Manifest.permission.RECEIVE_EMERGENCY_BROADCAST, Manifest.permission.RECEIVE_MMS, Manifest.permission.RECEIVE_WAP_PUSH, Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS, null, Manifest.permission.WRITE_SETTINGS, Manifest.permission.SYSTEM_ALERT_WINDOW, Manifest.permission.ACCESS_NOTIFICATIONS, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, null, null, null, null, null, null, null, null, null, null, null, null, Manifest.permission.WAKE_LOCK, null, null, Manifest.permission.PACKAGE_USAGE_STATS, null, null, null, null, null, null, null, Manifest.permission.READ_PHONE_STATE, Manifest.permission.ADD_VOICEMAIL, Manifest.permission.USE_SIP, Manifest.permission.PROCESS_OUTGOING_CALLS, Manifest.permission.USE_FINGERPRINT, Manifest.permission.BODY_SENSORS, Manifest.permission.READ_CELL_BROADCASTS, null, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, null, Manifest.permission.GET_ACCOUNTS, null, null, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.REQUEST_INSTALL_PACKAGES, null, Manifest.permission.INSTANT_APP_FOREGROUND_SERVICE, Manifest.permission.ANSWER_PHONE_CALLS, null, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.REQUEST_DELETE_PACKAGES, Manifest.permission.BIND_ACCESSIBILITY_SERVICE, Manifest.permission.ACCEPT_HANDOVER, Manifest.permission.MANAGE_IPSEC_TUNNELS, Manifest.permission.FOREGROUND_SERVICE, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.USE_BIOMETRIC, Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.SMS_FINANCIAL_TRANSACTIONS, Manifest.permission.READ_MEDIA_AUDIO, null, Manifest.permission.READ_MEDIA_VIDEO, null, Manifest.permission.READ_MEDIA_IMAGES, null, null, null, null, Manifest.permission.ACCESS_MEDIA_LOCATION, null, Manifest.permission.MANAGE_EXTERNAL_STORAGE, Manifest.permission.INTERACT_ACROSS_PROFILES, null, Manifest.permission.LOADER_USAGE_STATS, null, null, null, null, null, null, null, Manifest.permission.MANAGE_ONGOING_CALLS, null, Manifest.permission.USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER, null, Manifest.permission.SCHEDULE_EXACT_ALARM, null, null, Manifest.permission.MANAGE_MEDIA, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.UWB_RANGING, null, Manifest.permission.BLUETOOTH_ADVERTISE, null, Manifest.permission.NEARBY_WIFI_DEVICES, null, null, null, null};
    private static String[] sOpRestrictions = {UserManager.DISALLOW_SHARE_LOCATION, UserManager.DISALLOW_SHARE_LOCATION, UserManager.DISALLOW_SHARE_LOCATION, null, null, null, UserManager.DISALLOW_OUTGOING_CALLS, UserManager.DISALLOW_OUTGOING_CALLS, null, null, UserManager.DISALLOW_SHARE_LOCATION, null, null, null, UserManager.DISALLOW_SMS, UserManager.DISALLOW_SMS, UserManager.DISALLOW_SMS, null, UserManager.DISALLOW_SMS, null, UserManager.DISALLOW_SMS, UserManager.DISALLOW_SMS, UserManager.DISALLOW_SMS, null, UserManager.DISALLOW_CREATE_WINDOWS, null, UserManager.DISALLOW_CAMERA, UserManager.DISALLOW_RECORD_AUDIO, null, null, null, null, null, UserManager.DISALLOW_ADJUST_VOLUME, UserManager.DISALLOW_ADJUST_VOLUME, UserManager.DISALLOW_ADJUST_VOLUME, UserManager.DISALLOW_ADJUST_VOLUME, UserManager.DISALLOW_ADJUST_VOLUME, UserManager.DISALLOW_ADJUST_VOLUME, UserManager.DISALLOW_ADJUST_VOLUME, null, UserManager.DISALLOW_SHARE_LOCATION, UserManager.DISALLOW_SHARE_LOCATION, null, UserManager.DISALLOW_UNMUTE_MICROPHONE, UserManager.DISALLOW_CREATE_WINDOWS, null, null, UserManager.DISALLOW_WALLPAPER, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, UserManager.DISALLOW_ADJUST_VOLUME, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, UserManager.DISALLOW_SMS, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};
    private static RestrictionBypass[] sOpAllowSystemRestrictionBypass = {new RestrictionBypass(true, false, false), new RestrictionBypass(true, false, false), null, null, null, null, null, null, null, null, new RestrictionBypass(false, true, false), null, null, null, null, null, null, null, null, null, null, null, null, null, new RestrictionBypass(false, true, false), null, null, new RestrictionBypass(false, false, true), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, new RestrictionBypass(false, true, false), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, new RestrictionBypass(false, true, false), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};
    private static int[] sOpDefaultMode = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 3, getSystemAlertWindowDefault(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, 0, 3, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 3, 0, 2, 0, 2, 0, 2, 3, 0, 2, 0, 3, 3, 3, 1, 3, 1, 3, 0, 2, 0, 0, 0, 3, 3, 3, 0, 3, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static boolean[] sOpDisableReset = {false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, true, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false};
    private static boolean[] sOpRestrictRead = {false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false};
    private static HashMap<String, Integer> sOpStrToOp = new HashMap<>();
    private static HashMap<String, Integer> sPermToOp = new HashMap<>();
    private static final ThreadLocal<Integer> sBinderThreadCallingUid = new ThreadLocal<>();
    private static final ThreadLocal<ArrayMap<String, long[]>> sAppOpsNotedInThisBinderTransaction = new ThreadLocal<>();
    private static final byte[] sAppOpsToNote = new byte[125];
    private final ArrayMap<OnOpChangedListener, IAppOpsCallback> mModeWatchers = new ArrayMap<>();
    private final ArrayMap<OnOpActiveChangedListener, IAppOpsActiveCallback> mActiveWatchers = new ArrayMap<>();
    private final ArrayMap<OnOpStartedListener, IAppOpsStartedCallback> mStartedWatchers = new ArrayMap<>();
    private final ArrayMap<OnOpNotedListener, IAppOpsNotedCallback> mNotedWatchers = new ArrayMap<>();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface AttributionFlags {
    }

    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface DataBucketKey {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface HistoricalMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface HistoricalOpsRequestFilter {
    }

    /* loaded from: classes.dex */
    public interface HistoricalOpsVisitor {
        void visitHistoricalAttributionOps(AttributedHistoricalOps attributedHistoricalOps);

        void visitHistoricalOp(HistoricalOp historicalOp);

        void visitHistoricalOps(HistoricalOps historicalOps);

        void visitHistoricalPackageOps(HistoricalPackageOps historicalPackageOps);

        void visitHistoricalUidOps(HistoricalUidOps historicalUidOps);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface Mode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    private @interface NotedOpCollectionMode {
    }

    /* loaded from: classes.dex */
    public interface OnOpChangedListener {
        void onOpChanged(String str, String str2);
    }

    /* loaded from: classes.dex */
    public interface OnOpNotedListener {
        void onOpNoted(int i, int i2, String str, String str2, int i3, int i4);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface OpFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface OpHistoryFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface SamplingStrategy {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    private @interface ShouldCollectNoteOp {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface UidState {
    }

    /* renamed from: -$$Nest$smgetFormattedStackTrace */
    static /* bridge */ /* synthetic */ String m197$$Nest$smgetFormattedStackTrace() {
        return getFormattedStackTrace();
    }

    /* renamed from: -$$Nest$smgetService */
    static /* bridge */ /* synthetic */ IAppOpsService m199$$Nest$smgetService() {
        return getService();
    }

    /* renamed from: -$$Nest$smisCollectingStackTraces */
    static /* bridge */ /* synthetic */ boolean m200$$Nest$smisCollectingStackTraces() {
        return isCollectingStackTraces();
    }

    static {
        int[] iArr;
        if (sOpToSwitch.length == 121) {
            if (sOpToString.length == 121) {
                if (sOpNames.length == 121) {
                    if (sOpPerms.length == 121) {
                        if (sOpDefaultMode.length == 121) {
                            if (sOpDisableReset.length == 121) {
                                if (sOpRestrictions.length == 121) {
                                    if (sOpAllowSystemRestrictionBypass.length != 121) {
                                        throw new IllegalStateException("sOpAllowSYstemRestrictionsBypass length " + sOpRestrictions.length + " should be 121");
                                    }
                                    for (int i = 0; i < 121; i++) {
                                        String str = sOpToString[i];
                                        if (str != null) {
                                            sOpStrToOp.put(str, Integer.valueOf(i));
                                        }
                                    }
                                    for (int op : RUNTIME_AND_APPOP_PERMISSIONS_OPS) {
                                        String str2 = sOpPerms[op];
                                        if (str2 != null) {
                                            sPermToOp.put(str2, Integer.valueOf(op));
                                        }
                                    }
                                    sConfig = new MessageSamplingConfig(-1, 0, 0L);
                                    return;
                                }
                                throw new IllegalStateException("sOpRestrictions length " + sOpRestrictions.length + " should be 121");
                            }
                            throw new IllegalStateException("sOpDisableReset length " + sOpDisableReset.length + " should be 121");
                        }
                        throw new IllegalStateException("sOpDefaultMode length " + sOpDefaultMode.length + " should be 121");
                    }
                    throw new IllegalStateException("sOpPerms length " + sOpPerms.length + " should be 121");
                }
                throw new IllegalStateException("sOpNames length " + sOpNames.length + " should be 121");
            }
            throw new IllegalStateException("sOpToString length " + sOpToString.length + " should be 121");
        }
        throw new IllegalStateException("sOpToSwitch length " + sOpToSwitch.length + " should be 121");
    }

    public static int resolveFirstUnrestrictedUidState(int op) {
        return 500;
    }

    public static int resolveLastRestrictedUidState(int op) {
        return 600;
    }

    public static String getUidStateName(int uidState) {
        switch (uidState) {
            case 100:
                return "pers";
            case 200:
                return "top";
            case 300:
                return "fgsvcl";
            case 400:
                return "fgsvc";
            case 500:
                return "fg";
            case 600:
                return "bg";
            case 700:
                return "cch";
            default:
                return "unknown";
        }
    }

    public static final String getFlagName(int flag) {
        switch (flag) {
            case 1:
                return XmlTags.TAG_SESSION;
            case 2:
                return "tp";
            case 4:
                return MediaMetrics.Value.UP;
            case 8:
                return "tpd";
            case 16:
                return "upd";
            default:
                return "unknown";
        }
    }

    public static String keyToString(long key) {
        int uidState = extractUidStateFromKey(key);
        int flags = extractFlagsFromKey(key);
        return NavigationBarInflaterView.SIZE_MOD_START + getUidStateName(uidState) + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + flagsToString(flags) + NavigationBarInflaterView.SIZE_MOD_END;
    }

    public static long makeKey(int uidState, int flags) {
        return (uidState << 31) | flags;
    }

    public static int extractUidStateFromKey(long key) {
        return (int) (key >> 31);
    }

    public static int extractFlagsFromKey(long key) {
        return (int) ((-1) & key);
    }

    public static String flagsToString(int flags) {
        StringBuilder flagsBuilder = new StringBuilder();
        while (flags != 0) {
            int flag = 1 << Integer.numberOfTrailingZeros(flags);
            flags &= ~flag;
            if (flagsBuilder.length() > 0) {
                flagsBuilder.append('|');
            }
            flagsBuilder.append(getFlagName(flag));
        }
        return flagsBuilder.toString();
    }

    public static int opToSwitch(int op) {
        CtaManager ctaManager = sCtaManager;
        if (ctaManager.isCtaSupported()) {
            return ctaManager.opToSwitch(op);
        }
        return sOpToSwitch[op];
    }

    public static String opToName(int op) {
        CtaManager ctaManager = sCtaManager;
        if (ctaManager.isCtaSupported()) {
            return ctaManager.opToName(op);
        }
        if (op == -1) {
            return KeyProperties.DIGEST_NONE;
        }
        String[] strArr = sOpNames;
        return op < strArr.length ? strArr[op] : "Unknown(" + op + NavigationBarInflaterView.KEY_CODE_END;
    }

    public static String opToPublicName(int op) {
        CtaManager ctaManager = sCtaManager;
        if (ctaManager.isCtaSupported()) {
            return ctaManager.opToPublicName(op);
        }
        return sOpToString[op];
    }

    public static int strDebugOpToOp(String op) {
        CtaManager ctaManager = sCtaManager;
        if (ctaManager.isCtaSupported()) {
            return ctaManager.strDebugOpToOp(op);
        }
        int i = 0;
        while (true) {
            String[] strArr = sOpNames;
            if (i < strArr.length) {
                if (!strArr[i].equals(op)) {
                    i++;
                } else {
                    return i;
                }
            } else {
                throw new IllegalArgumentException("Unknown operation string: " + op);
            }
        }
    }

    public static String opToPermission(int op) {
        CtaManager ctaManager = sCtaManager;
        if (ctaManager.isCtaSupported()) {
            return ctaManager.opToPermission(op);
        }
        return sOpPerms[op];
    }

    @SystemApi
    public static String opToPermission(String op) {
        return opToPermission(strOpToOp(op));
    }

    public static String opToRestriction(int op) {
        CtaManager ctaManager = sCtaManager;
        if (ctaManager.isCtaSupported()) {
            return ctaManager.opToRestriction(op);
        }
        return sOpRestrictions[op];
    }

    public static int permissionToOpCode(String permission) {
        CtaManager ctaManager = sCtaManager;
        if (ctaManager.isCtaSupported()) {
            return ctaManager.permissionToOpCode(permission);
        }
        Integer boxedOpCode = sPermToOp.get(permission);
        if (boxedOpCode != null) {
            return boxedOpCode.intValue();
        }
        return -1;
    }

    public static RestrictionBypass opAllowSystemBypassRestriction(int op) {
        CtaManager ctaManager = sCtaManager;
        if (ctaManager.isCtaSupported()) {
            return ctaManager.opAllowSystemBypassRestriction(op);
        }
        return sOpAllowSystemRestrictionBypass[op];
    }

    public static int opToDefaultMode(int op) {
        CtaManager ctaManager = sCtaManager;
        if (!ctaManager.isCtaSupported()) {
            int[] iArr = sOpDefaultMode;
            if (op < iArr.length) {
                return iArr[op];
            }
        }
        return ctaManager.opToDefaultMode(op);
    }

    @SystemApi
    public static int opToDefaultMode(String appOp) {
        return opToDefaultMode(strOpToOp(appOp));
    }

    public static String modeToName(int mode) {
        if (mode >= 0) {
            String[] strArr = MODE_NAMES;
            if (mode < strArr.length) {
                return strArr[mode];
            }
        }
        return "mode=" + mode;
    }

    public static boolean opRestrictsRead(int op) {
        CtaManager ctaManager = sCtaManager;
        if (ctaManager.isCtaSupported()) {
            return ctaManager.opRestrictsRead(op);
        }
        return sOpRestrictRead[op];
    }

    public static boolean opAllowsReset(int op) {
        CtaManager ctaManager = sCtaManager;
        if (ctaManager.isCtaSupported()) {
            return ctaManager.opAllowsReset(op);
        }
        return !sOpDisableReset[op];
    }

    public static String toReceiverId(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof PendingIntent) {
            return toReceiverId((PendingIntent) obj);
        }
        return obj.getClass().getName() + "@" + System.identityHashCode(obj);
    }

    public static String toReceiverId(PendingIntent pendingIntent) {
        return pendingIntent.getTag("");
    }

    /* loaded from: classes.dex */
    public static class RestrictionBypass {
        public static RestrictionBypass UNRESTRICTED = new RestrictionBypass(false, true, true);
        public boolean isPrivileged;
        public boolean isRecordAudioRestrictionExcept;
        public boolean isSystemUid;

        public RestrictionBypass(boolean isSystemUid, boolean isPrivileged, boolean isRecordAudioRestrictionExcept) {
            this.isSystemUid = isSystemUid;
            this.isPrivileged = isPrivileged;
            this.isRecordAudioRestrictionExcept = isRecordAudioRestrictionExcept;
        }
    }

    @SystemApi
    /* loaded from: classes.dex */
    public static final class PackageOps implements Parcelable {
        public static final Parcelable.Creator<PackageOps> CREATOR = new Parcelable.Creator<PackageOps>() { // from class: android.app.AppOpsManager.PackageOps.1
            @Override // android.os.Parcelable.Creator
            public PackageOps createFromParcel(Parcel source) {
                return new PackageOps(source);
            }

            @Override // android.os.Parcelable.Creator
            public PackageOps[] newArray(int size) {
                return new PackageOps[size];
            }
        };
        private final List<OpEntry> mEntries;
        private final String mPackageName;
        private final int mUid;

        public PackageOps(String packageName, int uid, List<OpEntry> entries) {
            this.mPackageName = packageName;
            this.mUid = uid;
            this.mEntries = entries;
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public int getUid() {
            return this.mUid;
        }

        public List<OpEntry> getOps() {
            return this.mEntries;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.mPackageName);
            dest.writeInt(this.mUid);
            dest.writeInt(this.mEntries.size());
            for (int i = 0; i < this.mEntries.size(); i++) {
                this.mEntries.get(i).writeToParcel(dest, flags);
            }
        }

        PackageOps(Parcel source) {
            this.mPackageName = source.readString();
            this.mUid = source.readInt();
            this.mEntries = new ArrayList();
            int N = source.readInt();
            for (int i = 0; i < N; i++) {
                this.mEntries.add(OpEntry.CREATOR.createFromParcel(source));
            }
        }
    }

    @SystemApi
    /* loaded from: classes.dex */
    public static final class OpEventProxyInfo implements Parcelable {
        public static final Parcelable.Creator<OpEventProxyInfo> CREATOR = new Parcelable.Creator<OpEventProxyInfo>() { // from class: android.app.AppOpsManager.OpEventProxyInfo.1
            @Override // android.os.Parcelable.Creator
            public OpEventProxyInfo[] newArray(int size) {
                return new OpEventProxyInfo[size];
            }

            @Override // android.os.Parcelable.Creator
            public OpEventProxyInfo createFromParcel(Parcel in) {
                return new OpEventProxyInfo(in);
            }
        };
        private String mAttributionTag;
        private String mPackageName;
        private int mUid;

        public void reinit(int uid, String packageName, String attributionTag) {
            this.mUid = Preconditions.checkArgumentNonnegative(uid);
            this.mPackageName = packageName;
            this.mAttributionTag = attributionTag;
        }

        public OpEventProxyInfo(int uid, String packageName, String attributionTag) {
            this.mUid = uid;
            AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, uid, "from", 0L);
            this.mPackageName = packageName;
            this.mAttributionTag = attributionTag;
        }

        public OpEventProxyInfo(OpEventProxyInfo orig) {
            this.mUid = orig.mUid;
            this.mPackageName = orig.mPackageName;
            this.mAttributionTag = orig.mAttributionTag;
        }

        public int getUid() {
            return this.mUid;
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public String getAttributionTag() {
            return this.mAttributionTag;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            byte flg = this.mPackageName != null ? (byte) (0 | 2) : (byte) 0;
            if (this.mAttributionTag != null) {
                flg = (byte) (flg | 4);
            }
            dest.writeByte(flg);
            dest.writeInt(this.mUid);
            String str = this.mPackageName;
            if (str != null) {
                dest.writeString(str);
            }
            String str2 = this.mAttributionTag;
            if (str2 != null) {
                dest.writeString(str2);
            }
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        OpEventProxyInfo(Parcel in) {
            byte flg = in.readByte();
            int uid = in.readInt();
            String packageName = (flg & 2) == 0 ? null : in.readString();
            String attributionTag = (flg & 4) != 0 ? in.readString() : null;
            this.mUid = uid;
            AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, uid, "from", 0L);
            this.mPackageName = packageName;
            this.mAttributionTag = attributionTag;
        }
    }

    /* loaded from: classes.dex */
    public static final class NoteOpEvent implements Parcelable {
        public static final Parcelable.Creator<NoteOpEvent> CREATOR = new Parcelable.Creator<NoteOpEvent>() { // from class: android.app.AppOpsManager.NoteOpEvent.1
            @Override // android.os.Parcelable.Creator
            public NoteOpEvent[] newArray(int size) {
                return new NoteOpEvent[size];
            }

            @Override // android.os.Parcelable.Creator
            public NoteOpEvent createFromParcel(Parcel in) {
                return new NoteOpEvent(in);
            }
        };
        private long mDuration;
        private long mNoteTime;
        private OpEventProxyInfo mProxy;

        public void reinit(long noteTime, long duration, OpEventProxyInfo proxy, Pools.Pool<OpEventProxyInfo> proxyPool) {
            this.mNoteTime = Preconditions.checkArgumentNonnegative(noteTime);
            this.mDuration = Preconditions.checkArgumentInRange(duration, -1L, Long.MAX_VALUE, "duration");
            OpEventProxyInfo opEventProxyInfo = this.mProxy;
            if (opEventProxyInfo != null) {
                proxyPool.release(opEventProxyInfo);
            }
            this.mProxy = proxy;
        }

        public NoteOpEvent(NoteOpEvent original) {
            this(original.mNoteTime, original.mDuration, original.mProxy != null ? new OpEventProxyInfo(original.mProxy) : null);
        }

        public NoteOpEvent(long noteTime, long duration, OpEventProxyInfo proxy) {
            this.mNoteTime = noteTime;
            AnnotationValidations.validate(IntRange.class, (IntRange) null, noteTime, "from", 0L);
            this.mDuration = duration;
            AnnotationValidations.validate(IntRange.class, (IntRange) null, duration, "from", -1L);
            this.mProxy = proxy;
        }

        public long getNoteTime() {
            return this.mNoteTime;
        }

        public long getDuration() {
            return this.mDuration;
        }

        public OpEventProxyInfo getProxy() {
            return this.mProxy;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            byte flg = this.mProxy != null ? (byte) (0 | 4) : (byte) 0;
            dest.writeByte(flg);
            dest.writeLong(this.mNoteTime);
            dest.writeLong(this.mDuration);
            OpEventProxyInfo opEventProxyInfo = this.mProxy;
            if (opEventProxyInfo != null) {
                dest.writeTypedObject(opEventProxyInfo, flags);
            }
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        NoteOpEvent(Parcel in) {
            byte flg = in.readByte();
            long noteTime = in.readLong();
            long duration = in.readLong();
            OpEventProxyInfo proxy = (flg & 4) == 0 ? null : (OpEventProxyInfo) in.readTypedObject(OpEventProxyInfo.CREATOR);
            this.mNoteTime = noteTime;
            AnnotationValidations.validate(IntRange.class, (IntRange) null, noteTime, "from", 0L);
            this.mDuration = duration;
            AnnotationValidations.validate(IntRange.class, (IntRange) null, duration, "from", -1L);
            this.mProxy = proxy;
        }
    }

    @SystemApi
    /* loaded from: classes.dex */
    public static final class AttributedOpEntry implements Parcelable {
        public static final Parcelable.Creator<AttributedOpEntry> CREATOR;
        static Parcelling<LongSparseArray<NoteOpEvent>> sParcellingForAccessEvents;
        static Parcelling<LongSparseArray<NoteOpEvent>> sParcellingForRejectEvents;
        private final LongSparseArray<NoteOpEvent> mAccessEvents;
        private final int mOp;
        private final LongSparseArray<NoteOpEvent> mRejectEvents;
        private final boolean mRunning;

        private AttributedOpEntry(AttributedOpEntry other) {
            this.mOp = other.mOp;
            this.mRunning = other.mRunning;
            LongSparseArray<NoteOpEvent> longSparseArray = other.mAccessEvents;
            this.mAccessEvents = longSparseArray == null ? null : longSparseArray.m4662clone();
            LongSparseArray<NoteOpEvent> longSparseArray2 = other.mRejectEvents;
            this.mRejectEvents = longSparseArray2 != null ? longSparseArray2.m4662clone() : null;
        }

        public ArraySet<Long> collectKeys() {
            ArraySet<Long> keys = new ArraySet<>();
            LongSparseArray<NoteOpEvent> longSparseArray = this.mAccessEvents;
            if (longSparseArray != null) {
                int numEvents = longSparseArray.size();
                for (int i = 0; i < numEvents; i++) {
                    keys.add(Long.valueOf(this.mAccessEvents.keyAt(i)));
                }
            }
            LongSparseArray<NoteOpEvent> longSparseArray2 = this.mRejectEvents;
            if (longSparseArray2 != null) {
                int numEvents2 = longSparseArray2.size();
                for (int i2 = 0; i2 < numEvents2; i2++) {
                    keys.add(Long.valueOf(this.mRejectEvents.keyAt(i2)));
                }
            }
            return keys;
        }

        public long getLastAccessTime(int flags) {
            return getLastAccessTime(100, 700, flags);
        }

        public long getLastAccessForegroundTime(int flags) {
            return getLastAccessTime(100, AppOpsManager.resolveFirstUnrestrictedUidState(this.mOp), flags);
        }

        public long getLastAccessBackgroundTime(int flags) {
            return getLastAccessTime(AppOpsManager.resolveLastRestrictedUidState(this.mOp), 700, flags);
        }

        public NoteOpEvent getLastAccessEvent(int fromUidState, int toUidState, int flags) {
            return AppOpsManager.getLastEvent(this.mAccessEvents, fromUidState, toUidState, flags);
        }

        public long getLastAccessTime(int fromUidState, int toUidState, int flags) {
            NoteOpEvent lastEvent = getLastAccessEvent(fromUidState, toUidState, flags);
            if (lastEvent == null) {
                return -1L;
            }
            return lastEvent.getNoteTime();
        }

        public long getLastRejectTime(int flags) {
            return getLastRejectTime(100, 700, flags);
        }

        public long getLastRejectForegroundTime(int flags) {
            return getLastRejectTime(100, AppOpsManager.resolveFirstUnrestrictedUidState(this.mOp), flags);
        }

        public long getLastRejectBackgroundTime(int flags) {
            return getLastRejectTime(AppOpsManager.resolveLastRestrictedUidState(this.mOp), 700, flags);
        }

        public NoteOpEvent getLastRejectEvent(int fromUidState, int toUidState, int flags) {
            return AppOpsManager.getLastEvent(this.mRejectEvents, fromUidState, toUidState, flags);
        }

        public long getLastRejectTime(int fromUidState, int toUidState, int flags) {
            NoteOpEvent lastEvent = getLastRejectEvent(fromUidState, toUidState, flags);
            if (lastEvent == null) {
                return -1L;
            }
            return lastEvent.getNoteTime();
        }

        public long getLastDuration(int flags) {
            return getLastDuration(100, 700, flags);
        }

        public long getLastForegroundDuration(int flags) {
            return getLastDuration(100, AppOpsManager.resolveFirstUnrestrictedUidState(this.mOp), flags);
        }

        public long getLastBackgroundDuration(int flags) {
            return getLastDuration(AppOpsManager.resolveLastRestrictedUidState(this.mOp), 700, flags);
        }

        public long getLastDuration(int fromUidState, int toUidState, int flags) {
            NoteOpEvent lastEvent = getLastAccessEvent(fromUidState, toUidState, flags);
            if (lastEvent == null) {
                return -1L;
            }
            return lastEvent.getDuration();
        }

        public OpEventProxyInfo getLastProxyInfo(int flags) {
            return getLastProxyInfo(100, 700, flags);
        }

        public OpEventProxyInfo getLastForegroundProxyInfo(int flags) {
            return getLastProxyInfo(100, AppOpsManager.resolveFirstUnrestrictedUidState(this.mOp), flags);
        }

        public OpEventProxyInfo getLastBackgroundProxyInfo(int flags) {
            return getLastProxyInfo(AppOpsManager.resolveLastRestrictedUidState(this.mOp), 700, flags);
        }

        public OpEventProxyInfo getLastProxyInfo(int fromUidState, int toUidState, int flags) {
            NoteOpEvent lastEvent = getLastAccessEvent(fromUidState, toUidState, flags);
            if (lastEvent == null) {
                return null;
            }
            return lastEvent.getProxy();
        }

        String getOpName() {
            return AppOpsManager.opToPublicName(this.mOp);
        }

        int getOp() {
            return this.mOp;
        }

        /* loaded from: classes.dex */
        private static class LongSparseArrayParceling implements Parcelling<LongSparseArray<NoteOpEvent>> {
            private LongSparseArrayParceling() {
            }

            @Override // com.android.internal.util.Parcelling
            public void parcel(LongSparseArray<NoteOpEvent> array, Parcel dest, int parcelFlags) {
                if (array == null) {
                    dest.writeInt(-1);
                    return;
                }
                int numEntries = array.size();
                dest.writeInt(numEntries);
                for (int i = 0; i < numEntries; i++) {
                    dest.writeLong(array.keyAt(i));
                    dest.writeParcelable(array.valueAt(i), parcelFlags);
                }
            }

            @Override // com.android.internal.util.Parcelling
            public LongSparseArray<NoteOpEvent> unparcel(Parcel source) {
                int numEntries = source.readInt();
                if (numEntries == -1) {
                    return null;
                }
                LongSparseArray<NoteOpEvent> array = new LongSparseArray<>(numEntries);
                for (int i = 0; i < numEntries; i++) {
                    array.put(source.readLong(), (NoteOpEvent) source.readParcelable(null, NoteOpEvent.class));
                }
                return array;
            }
        }

        public AttributedOpEntry(int op, boolean running, LongSparseArray<NoteOpEvent> accessEvents, LongSparseArray<NoteOpEvent> rejectEvents) {
            this.mOp = op;
            AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, op, "from", 0L, "to", 124L);
            this.mRunning = running;
            this.mAccessEvents = accessEvents;
            this.mRejectEvents = rejectEvents;
        }

        public boolean isRunning() {
            return this.mRunning;
        }

        static {
            Parcelling<LongSparseArray<NoteOpEvent>> parcelling = Parcelling.Cache.get(LongSparseArrayParceling.class);
            sParcellingForAccessEvents = parcelling;
            if (parcelling == null) {
                sParcellingForAccessEvents = Parcelling.Cache.put(new LongSparseArrayParceling());
            }
            Parcelling<LongSparseArray<NoteOpEvent>> parcelling2 = Parcelling.Cache.get(LongSparseArrayParceling.class);
            sParcellingForRejectEvents = parcelling2;
            if (parcelling2 == null) {
                sParcellingForRejectEvents = Parcelling.Cache.put(new LongSparseArrayParceling());
            }
            CREATOR = new Parcelable.Creator<AttributedOpEntry>() { // from class: android.app.AppOpsManager.AttributedOpEntry.1
                @Override // android.os.Parcelable.Creator
                public AttributedOpEntry[] newArray(int size) {
                    return new AttributedOpEntry[size];
                }

                @Override // android.os.Parcelable.Creator
                public AttributedOpEntry createFromParcel(Parcel in) {
                    return new AttributedOpEntry(in);
                }
            };
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            byte flg = this.mRunning ? (byte) (0 | 2) : (byte) 0;
            if (this.mAccessEvents != null) {
                flg = (byte) (flg | 4);
            }
            if (this.mRejectEvents != null) {
                flg = (byte) (flg | 8);
            }
            dest.writeByte(flg);
            dest.writeInt(this.mOp);
            sParcellingForAccessEvents.parcel(this.mAccessEvents, dest, flags);
            sParcellingForRejectEvents.parcel(this.mRejectEvents, dest, flags);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        AttributedOpEntry(Parcel in) {
            byte flg = in.readByte();
            boolean running = (flg & 2) != 0;
            int op = in.readInt();
            LongSparseArray<NoteOpEvent> accessEvents = sParcellingForAccessEvents.unparcel(in);
            LongSparseArray<NoteOpEvent> rejectEvents = sParcellingForRejectEvents.unparcel(in);
            this.mOp = op;
            AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, op, "from", 0L, "to", 124L);
            this.mRunning = running;
            this.mAccessEvents = accessEvents;
            this.mRejectEvents = rejectEvents;
        }
    }

    @SystemApi
    /* loaded from: classes.dex */
    public static final class OpEntry implements Parcelable {
        public static final Parcelable.Creator<OpEntry> CREATOR = new Parcelable.Creator<OpEntry>() { // from class: android.app.AppOpsManager.OpEntry.1
            @Override // android.os.Parcelable.Creator
            public OpEntry[] newArray(int size) {
                return new OpEntry[size];
            }

            @Override // android.os.Parcelable.Creator
            public OpEntry createFromParcel(Parcel in) {
                return new OpEntry(in);
            }
        };
        private final Map<String, AttributedOpEntry> mAttributedOpEntries;
        private final int mMode;
        private final int mOp;

        public int getOp() {
            return this.mOp;
        }

        public String getOpStr() {
            if (AppOpsManager.sCtaManager.isCtaSupported()) {
                return AppOpsManager.getOpStrs()[this.mOp];
            }
            return AppOpsManager.sOpToString[this.mOp];
        }

        @Deprecated
        public long getTime() {
            return getLastAccessTime(31);
        }

        public long getLastAccessTime(int flags) {
            return getLastAccessTime(100, 700, flags);
        }

        public long getLastAccessForegroundTime(int flags) {
            return getLastAccessTime(100, AppOpsManager.resolveFirstUnrestrictedUidState(this.mOp), flags);
        }

        public long getLastAccessBackgroundTime(int flags) {
            return getLastAccessTime(AppOpsManager.resolveLastRestrictedUidState(this.mOp), 700, flags);
        }

        private NoteOpEvent getLastAccessEvent(int fromUidState, int toUidState, int flags) {
            NoteOpEvent lastAccessEvent = null;
            for (AttributedOpEntry attributionEntry : this.mAttributedOpEntries.values()) {
                NoteOpEvent lastAttributionAccessEvent = attributionEntry.getLastAccessEvent(fromUidState, toUidState, flags);
                if (lastAccessEvent == null || (lastAttributionAccessEvent != null && lastAttributionAccessEvent.getNoteTime() > lastAccessEvent.getNoteTime())) {
                    lastAccessEvent = lastAttributionAccessEvent;
                }
            }
            return lastAccessEvent;
        }

        public long getLastAccessTime(int fromUidState, int toUidState, int flags) {
            NoteOpEvent lastEvent = getLastAccessEvent(fromUidState, toUidState, flags);
            if (lastEvent == null) {
                return -1L;
            }
            return lastEvent.getNoteTime();
        }

        @Deprecated
        public long getRejectTime() {
            return getLastRejectTime(31);
        }

        public long getLastRejectTime(int flags) {
            return getLastRejectTime(100, 700, flags);
        }

        public long getLastRejectForegroundTime(int flags) {
            return getLastRejectTime(100, AppOpsManager.resolveFirstUnrestrictedUidState(this.mOp), flags);
        }

        public long getLastRejectBackgroundTime(int flags) {
            return getLastRejectTime(AppOpsManager.resolveLastRestrictedUidState(this.mOp), 700, flags);
        }

        private NoteOpEvent getLastRejectEvent(int fromUidState, int toUidState, int flags) {
            NoteOpEvent lastAccessEvent = null;
            for (AttributedOpEntry attributionEntry : this.mAttributedOpEntries.values()) {
                NoteOpEvent lastAttributionAccessEvent = attributionEntry.getLastRejectEvent(fromUidState, toUidState, flags);
                if (lastAccessEvent == null || (lastAttributionAccessEvent != null && lastAttributionAccessEvent.getNoteTime() > lastAccessEvent.getNoteTime())) {
                    lastAccessEvent = lastAttributionAccessEvent;
                }
            }
            return lastAccessEvent;
        }

        public long getLastRejectTime(int fromUidState, int toUidState, int flags) {
            NoteOpEvent lastEvent = getLastRejectEvent(fromUidState, toUidState, flags);
            if (lastEvent == null) {
                return -1L;
            }
            return lastEvent.getNoteTime();
        }

        public boolean isRunning() {
            for (AttributedOpEntry opAttributionEntry : this.mAttributedOpEntries.values()) {
                if (opAttributionEntry.isRunning()) {
                    return true;
                }
            }
            return false;
        }

        @Deprecated
        public long getDuration() {
            return getLastDuration(31);
        }

        public long getLastDuration(int flags) {
            return getLastDuration(100, 700, flags);
        }

        public long getLastForegroundDuration(int flags) {
            return getLastDuration(100, AppOpsManager.resolveFirstUnrestrictedUidState(this.mOp), flags);
        }

        public long getLastBackgroundDuration(int flags) {
            return getLastDuration(AppOpsManager.resolveLastRestrictedUidState(this.mOp), 700, flags);
        }

        public long getLastDuration(int fromUidState, int toUidState, int flags) {
            NoteOpEvent lastEvent = getLastAccessEvent(fromUidState, toUidState, flags);
            if (lastEvent == null) {
                return -1L;
            }
            return lastEvent.getDuration();
        }

        @Deprecated
        public int getProxyUid() {
            OpEventProxyInfo proxy = getLastProxyInfo(31);
            if (proxy == null) {
                return -1;
            }
            return proxy.getUid();
        }

        @Deprecated
        public int getProxyUid(int uidState, int flags) {
            OpEventProxyInfo proxy = getLastProxyInfo(uidState, uidState, flags);
            if (proxy == null) {
                return -1;
            }
            return proxy.getUid();
        }

        @Deprecated
        public String getProxyPackageName() {
            OpEventProxyInfo proxy = getLastProxyInfo(31);
            if (proxy == null) {
                return null;
            }
            return proxy.getPackageName();
        }

        @Deprecated
        public String getProxyPackageName(int uidState, int flags) {
            OpEventProxyInfo proxy = getLastProxyInfo(uidState, uidState, flags);
            if (proxy == null) {
                return null;
            }
            return proxy.getPackageName();
        }

        public OpEventProxyInfo getLastProxyInfo(int flags) {
            return getLastProxyInfo(100, 700, flags);
        }

        public OpEventProxyInfo getLastForegroundProxyInfo(int flags) {
            return getLastProxyInfo(100, AppOpsManager.resolveFirstUnrestrictedUidState(this.mOp), flags);
        }

        public OpEventProxyInfo getLastBackgroundProxyInfo(int flags) {
            return getLastProxyInfo(AppOpsManager.resolveLastRestrictedUidState(this.mOp), 700, flags);
        }

        public OpEventProxyInfo getLastProxyInfo(int fromUidState, int toUidState, int flags) {
            NoteOpEvent lastEvent = getLastAccessEvent(fromUidState, toUidState, flags);
            if (lastEvent == null) {
                return null;
            }
            return lastEvent.getProxy();
        }

        public OpEntry(int op, int mode, Map<String, AttributedOpEntry> attributedOpEntries) {
            this.mOp = op;
            AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, op, "from", 0L, "to", 124L);
            this.mMode = mode;
            AnnotationValidations.validate((Class<? extends Annotation>) Mode.class, (Annotation) null, mode);
            this.mAttributedOpEntries = attributedOpEntries;
            AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) attributedOpEntries);
        }

        public int getMode() {
            return this.mMode;
        }

        public Map<String, AttributedOpEntry> getAttributedOpEntries() {
            return this.mAttributedOpEntries;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mOp);
            dest.writeInt(this.mMode);
            dest.writeMap(this.mAttributedOpEntries);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        OpEntry(Parcel in) {
            int op = in.readInt();
            int mode = in.readInt();
            Map<String, AttributedOpEntry> attributions = new LinkedHashMap<>();
            in.readMap(attributions, AttributedOpEntry.class.getClassLoader());
            this.mOp = op;
            AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, op, "from", 0L, "to", 124L);
            this.mMode = mode;
            AnnotationValidations.validate((Class<? extends Annotation>) Mode.class, (Annotation) null, mode);
            this.mAttributedOpEntries = attributions;
            AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) attributions);
        }
    }

    @SystemApi
    /* loaded from: classes.dex */
    public static final class HistoricalOpsRequest {
        private final String mAttributionTag;
        private final long mBeginTimeMillis;
        private final long mEndTimeMillis;
        private final int mFilter;
        private final int mFlags;
        private final int mHistoryFlags;
        private final List<String> mOpNames;
        private final String mPackageName;
        private final int mUid;

        private HistoricalOpsRequest(int uid, String packageName, String attributionTag, List<String> opNames, int historyFlags, int filter, long beginTimeMillis, long endTimeMillis, int flags) {
            this.mUid = uid;
            this.mPackageName = packageName;
            this.mAttributionTag = attributionTag;
            this.mOpNames = opNames;
            this.mHistoryFlags = historyFlags;
            this.mFilter = filter;
            this.mBeginTimeMillis = beginTimeMillis;
            this.mEndTimeMillis = endTimeMillis;
            this.mFlags = flags;
        }

        @SystemApi
        /* loaded from: classes.dex */
        public static final class Builder {
            private String mAttributionTag;
            private final long mBeginTimeMillis;
            private final long mEndTimeMillis;
            private int mFilter;
            private int mHistoryFlags;
            private List<String> mOpNames;
            private String mPackageName;
            private int mUid = -1;
            private int mFlags = 31;

            public Builder(long beginTimeMillis, long endTimeMillis) {
                Preconditions.checkArgument(beginTimeMillis >= 0 && beginTimeMillis < endTimeMillis, "beginTimeMillis must be non negative and lesser than endTimeMillis");
                this.mBeginTimeMillis = beginTimeMillis;
                this.mEndTimeMillis = endTimeMillis;
                this.mHistoryFlags = 1;
            }

            public Builder setUid(int uid) {
                Preconditions.checkArgument(uid == -1 || uid >= 0, "uid must be -1 or non negative");
                this.mUid = uid;
                if (uid != -1) {
                    this.mFilter = 1 | this.mFilter;
                } else {
                    this.mFilter &= -2;
                }
                return this;
            }

            public Builder setPackageName(String packageName) {
                this.mPackageName = packageName;
                if (packageName == null) {
                    this.mFilter &= -3;
                } else {
                    this.mFilter |= 2;
                }
                return this;
            }

            public Builder setAttributionTag(String attributionTag) {
                this.mAttributionTag = attributionTag;
                this.mFilter |= 4;
                return this;
            }

            public Builder setOpNames(List<String> opNames) {
                if (opNames != null) {
                    int opCount = opNames.size();
                    for (int i = 0; i < opCount; i++) {
                        Preconditions.checkArgument(AppOpsManager.strOpToOp(opNames.get(i)) != -1);
                    }
                }
                this.mOpNames = opNames;
                if (opNames == null) {
                    this.mFilter &= -9;
                } else {
                    this.mFilter |= 8;
                }
                return this;
            }

            public Builder setFlags(int flags) {
                Preconditions.checkFlagsArgument(flags, 31);
                this.mFlags = flags;
                return this;
            }

            public Builder setHistoryFlags(int flags) {
                Preconditions.checkFlagsArgument(flags, 7);
                this.mHistoryFlags = flags;
                return this;
            }

            public HistoricalOpsRequest build() {
                return new HistoricalOpsRequest(this.mUid, this.mPackageName, this.mAttributionTag, this.mOpNames, this.mHistoryFlags, this.mFilter, this.mBeginTimeMillis, this.mEndTimeMillis, this.mFlags);
            }
        }
    }

    @SystemApi
    /* loaded from: classes.dex */
    public static final class HistoricalOps implements Parcelable {
        public static final Parcelable.Creator<HistoricalOps> CREATOR = new Parcelable.Creator<HistoricalOps>() { // from class: android.app.AppOpsManager.HistoricalOps.1
            @Override // android.os.Parcelable.Creator
            public HistoricalOps createFromParcel(Parcel parcel) {
                return new HistoricalOps(parcel);
            }

            @Override // android.os.Parcelable.Creator
            public HistoricalOps[] newArray(int size) {
                return new HistoricalOps[size];
            }
        };
        private long mBeginTimeMillis;
        private long mEndTimeMillis;
        private SparseArray<HistoricalUidOps> mHistoricalUidOps;

        public HistoricalOps(long beginTimeMillis, long endTimeMillis) {
            Preconditions.checkState(beginTimeMillis <= endTimeMillis);
            this.mBeginTimeMillis = beginTimeMillis;
            this.mEndTimeMillis = endTimeMillis;
        }

        public HistoricalOps(HistoricalOps other) {
            long j = other.mBeginTimeMillis;
            this.mBeginTimeMillis = j;
            long j2 = other.mEndTimeMillis;
            this.mEndTimeMillis = j2;
            Preconditions.checkState(j <= j2);
            if (other.mHistoricalUidOps != null) {
                int opCount = other.getUidCount();
                for (int i = 0; i < opCount; i++) {
                    HistoricalUidOps origOps = other.getUidOpsAt(i);
                    HistoricalUidOps clonedOps = new HistoricalUidOps(origOps);
                    if (this.mHistoricalUidOps == null) {
                        this.mHistoricalUidOps = new SparseArray<>(opCount);
                    }
                    this.mHistoricalUidOps.put(clonedOps.getUid(), clonedOps);
                }
            }
        }

        private HistoricalOps(Parcel parcel) {
            this.mBeginTimeMillis = parcel.readLong();
            this.mEndTimeMillis = parcel.readLong();
            int[] uids = parcel.createIntArray();
            if (!ArrayUtils.isEmpty(uids)) {
                ParceledListSlice<HistoricalUidOps> listSlice = (ParceledListSlice) parcel.readParcelable(HistoricalOps.class.getClassLoader(), ParceledListSlice.class);
                List<HistoricalUidOps> uidOps = listSlice != null ? listSlice.getList() : null;
                if (uidOps == null) {
                    return;
                }
                for (int i = 0; i < uids.length; i++) {
                    if (this.mHistoricalUidOps == null) {
                        this.mHistoricalUidOps = new SparseArray<>();
                    }
                    this.mHistoricalUidOps.put(uids[i], uidOps.get(i));
                }
            }
        }

        public HistoricalOps spliceFromBeginning(double splicePoint) {
            return splice(splicePoint, true);
        }

        public HistoricalOps spliceFromEnd(double fractionToRemove) {
            return splice(fractionToRemove, false);
        }

        private HistoricalOps splice(double fractionToRemove, boolean beginning) {
            long spliceBeginTimeMills;
            long spliceEndTimeMills;
            if (beginning) {
                spliceBeginTimeMills = this.mBeginTimeMillis;
                spliceEndTimeMills = (long) (this.mBeginTimeMillis + (getDurationMillis() * fractionToRemove));
                this.mBeginTimeMillis = spliceEndTimeMills;
            } else {
                spliceBeginTimeMills = (long) (this.mEndTimeMillis - (getDurationMillis() * fractionToRemove));
                spliceEndTimeMills = this.mEndTimeMillis;
                this.mEndTimeMillis = spliceBeginTimeMills;
            }
            HistoricalOps splice = null;
            int uidCount = getUidCount();
            for (int i = 0; i < uidCount; i++) {
                HistoricalUidOps origOps = getUidOpsAt(i);
                HistoricalUidOps spliceOps = origOps.splice(fractionToRemove);
                if (spliceOps != null) {
                    if (splice == null) {
                        splice = new HistoricalOps(spliceBeginTimeMills, spliceEndTimeMills);
                    }
                    if (splice.mHistoricalUidOps == null) {
                        splice.mHistoricalUidOps = new SparseArray<>();
                    }
                    splice.mHistoricalUidOps.put(spliceOps.getUid(), spliceOps);
                }
            }
            return splice;
        }

        public void merge(HistoricalOps other) {
            this.mBeginTimeMillis = Math.min(this.mBeginTimeMillis, other.mBeginTimeMillis);
            this.mEndTimeMillis = Math.max(this.mEndTimeMillis, other.mEndTimeMillis);
            int uidCount = other.getUidCount();
            for (int i = 0; i < uidCount; i++) {
                HistoricalUidOps otherUidOps = other.getUidOpsAt(i);
                HistoricalUidOps thisUidOps = getUidOps(otherUidOps.getUid());
                if (thisUidOps != null) {
                    thisUidOps.merge(otherUidOps);
                } else {
                    if (this.mHistoricalUidOps == null) {
                        this.mHistoricalUidOps = new SparseArray<>();
                    }
                    this.mHistoricalUidOps.put(otherUidOps.getUid(), otherUidOps);
                }
            }
        }

        public void filter(int uid, String packageName, String attributionTag, String[] opNames, int historyFilter, int filter, long beginTimeMillis, long endTimeMillis) {
            long durationMillis = getDurationMillis();
            this.mBeginTimeMillis = Math.max(this.mBeginTimeMillis, beginTimeMillis);
            this.mEndTimeMillis = Math.min(this.mEndTimeMillis, endTimeMillis);
            double scaleFactor = Math.min((endTimeMillis - beginTimeMillis) / durationMillis, 1.0d);
            int uidCount = getUidCount();
            for (int i = uidCount - 1; i >= 0; i--) {
                HistoricalUidOps uidOp = this.mHistoricalUidOps.valueAt(i);
                if ((filter & 1) != 0 && uid != uidOp.getUid()) {
                    this.mHistoricalUidOps.removeAt(i);
                }
                uidOp.filter(packageName, attributionTag, opNames, filter, historyFilter, scaleFactor, this.mBeginTimeMillis, this.mEndTimeMillis);
                if (uidOp.getPackageCount() == 0) {
                    this.mHistoricalUidOps.removeAt(i);
                }
            }
        }

        public boolean isEmpty() {
            if (getBeginTimeMillis() >= getEndTimeMillis()) {
                return true;
            }
            int uidCount = getUidCount();
            for (int i = uidCount - 1; i >= 0; i--) {
                HistoricalUidOps uidOp = this.mHistoricalUidOps.valueAt(i);
                if (!uidOp.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        public long getDurationMillis() {
            return this.mEndTimeMillis - this.mBeginTimeMillis;
        }

        public void increaseAccessCount(int opCode, int uid, String packageName, String attributionTag, int uidState, int flags, long increment) {
            getOrCreateHistoricalUidOps(uid).increaseAccessCount(opCode, packageName, attributionTag, uidState, flags, increment);
        }

        public void increaseRejectCount(int opCode, int uid, String packageName, String attributionTag, int uidState, int flags, long increment) {
            getOrCreateHistoricalUidOps(uid).increaseRejectCount(opCode, packageName, attributionTag, uidState, flags, increment);
        }

        public void increaseAccessDuration(int opCode, int uid, String packageName, String attributionTag, int uidState, int flags, long increment) {
            getOrCreateHistoricalUidOps(uid).increaseAccessDuration(opCode, packageName, attributionTag, uidState, flags, increment);
        }

        public void addDiscreteAccess(int opCode, int uid, String packageName, String attributionTag, int uidState, int opFlag, long discreteAccessTime, long discreteAccessDuration) {
            getOrCreateHistoricalUidOps(uid).addDiscreteAccess(opCode, packageName, attributionTag, uidState, opFlag, discreteAccessTime, discreteAccessDuration, null);
        }

        public void addDiscreteAccess(int opCode, int uid, String packageName, String attributionTag, int uidState, int opFlag, long discreteAccessTime, long discreteAccessDuration, OpEventProxyInfo proxy) {
            getOrCreateHistoricalUidOps(uid).addDiscreteAccess(opCode, packageName, attributionTag, uidState, opFlag, discreteAccessTime, discreteAccessDuration, proxy);
        }

        public void offsetBeginAndEndTime(long offsetMillis) {
            this.mBeginTimeMillis += offsetMillis;
            this.mEndTimeMillis += offsetMillis;
        }

        public void setBeginAndEndTime(long beginTimeMillis, long endTimeMillis) {
            this.mBeginTimeMillis = beginTimeMillis;
            this.mEndTimeMillis = endTimeMillis;
        }

        public void setBeginTime(long beginTimeMillis) {
            this.mBeginTimeMillis = beginTimeMillis;
        }

        public void setEndTime(long endTimeMillis) {
            this.mEndTimeMillis = endTimeMillis;
        }

        public long getBeginTimeMillis() {
            return this.mBeginTimeMillis;
        }

        public long getEndTimeMillis() {
            return this.mEndTimeMillis;
        }

        public int getUidCount() {
            SparseArray<HistoricalUidOps> sparseArray = this.mHistoricalUidOps;
            if (sparseArray == null) {
                return 0;
            }
            return sparseArray.size();
        }

        public HistoricalUidOps getUidOpsAt(int index) {
            SparseArray<HistoricalUidOps> sparseArray = this.mHistoricalUidOps;
            if (sparseArray == null) {
                throw new IndexOutOfBoundsException();
            }
            return sparseArray.valueAt(index);
        }

        public HistoricalUidOps getUidOps(int uid) {
            SparseArray<HistoricalUidOps> sparseArray = this.mHistoricalUidOps;
            if (sparseArray == null) {
                return null;
            }
            return sparseArray.get(uid);
        }

        public void clearHistory(int uid, String packageName) {
            HistoricalUidOps historicalUidOps = getOrCreateHistoricalUidOps(uid);
            historicalUidOps.clearHistory(packageName);
            if (historicalUidOps.isEmpty()) {
                this.mHistoricalUidOps.remove(uid);
            }
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeLong(this.mBeginTimeMillis);
            parcel.writeLong(this.mEndTimeMillis);
            SparseArray<HistoricalUidOps> sparseArray = this.mHistoricalUidOps;
            if (sparseArray != null) {
                int uidCount = sparseArray.size();
                parcel.writeInt(uidCount);
                for (int i = 0; i < uidCount; i++) {
                    parcel.writeInt(this.mHistoricalUidOps.keyAt(i));
                }
                List<HistoricalUidOps> opsList = new ArrayList<>(uidCount);
                for (int i2 = 0; i2 < uidCount; i2++) {
                    opsList.add(this.mHistoricalUidOps.valueAt(i2));
                }
                parcel.writeParcelable(new ParceledListSlice(opsList), flags);
                return;
            }
            parcel.writeInt(-1);
        }

        public void accept(HistoricalOpsVisitor visitor) {
            visitor.visitHistoricalOps(this);
            int uidCount = getUidCount();
            for (int i = 0; i < uidCount; i++) {
                getUidOpsAt(i).accept(visitor);
            }
        }

        private HistoricalUidOps getOrCreateHistoricalUidOps(int uid) {
            if (this.mHistoricalUidOps == null) {
                this.mHistoricalUidOps = new SparseArray<>();
            }
            HistoricalUidOps historicalUidOp = this.mHistoricalUidOps.get(uid);
            if (historicalUidOp == null) {
                HistoricalUidOps historicalUidOp2 = new HistoricalUidOps(uid);
                this.mHistoricalUidOps.put(uid, historicalUidOp2);
                return historicalUidOp2;
            }
            return historicalUidOp;
        }

        public static double round(double value) {
            return Math.floor(0.5d + value);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            HistoricalOps other = (HistoricalOps) obj;
            if (this.mBeginTimeMillis != other.mBeginTimeMillis || this.mEndTimeMillis != other.mEndTimeMillis) {
                return false;
            }
            SparseArray<HistoricalUidOps> sparseArray = this.mHistoricalUidOps;
            if (sparseArray == null) {
                if (other.mHistoricalUidOps != null) {
                    return false;
                }
            } else if (!sparseArray.equals(other.mHistoricalUidOps)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            long j = this.mBeginTimeMillis;
            int result = (int) (j ^ (j >>> 32));
            return (result * 31) + this.mHistoricalUidOps.hashCode();
        }

        public String toString() {
            return getClass().getSimpleName() + "[from:" + this.mBeginTimeMillis + " to:" + this.mEndTimeMillis + NavigationBarInflaterView.SIZE_MOD_END;
        }
    }

    @SystemApi
    /* loaded from: classes.dex */
    public static final class HistoricalUidOps implements Parcelable {
        public static final Parcelable.Creator<HistoricalUidOps> CREATOR = new Parcelable.Creator<HistoricalUidOps>() { // from class: android.app.AppOpsManager.HistoricalUidOps.1
            @Override // android.os.Parcelable.Creator
            public HistoricalUidOps createFromParcel(Parcel parcel) {
                return new HistoricalUidOps(parcel);
            }

            @Override // android.os.Parcelable.Creator
            public HistoricalUidOps[] newArray(int size) {
                return new HistoricalUidOps[size];
            }
        };
        private ArrayMap<String, HistoricalPackageOps> mHistoricalPackageOps;
        private final int mUid;

        public HistoricalUidOps(int uid) {
            this.mUid = uid;
        }

        private HistoricalUidOps(HistoricalUidOps other) {
            this.mUid = other.mUid;
            int opCount = other.getPackageCount();
            for (int i = 0; i < opCount; i++) {
                HistoricalPackageOps origOps = other.getPackageOpsAt(i);
                HistoricalPackageOps cloneOps = new HistoricalPackageOps(origOps);
                if (this.mHistoricalPackageOps == null) {
                    this.mHistoricalPackageOps = new ArrayMap<>(opCount);
                }
                this.mHistoricalPackageOps.put(cloneOps.getPackageName(), cloneOps);
            }
        }

        private HistoricalUidOps(Parcel parcel) {
            this.mUid = parcel.readInt();
            this.mHistoricalPackageOps = parcel.createTypedArrayMap(HistoricalPackageOps.CREATOR);
        }

        public HistoricalUidOps splice(double fractionToRemove) {
            HistoricalUidOps splice = null;
            int packageCount = getPackageCount();
            for (int i = 0; i < packageCount; i++) {
                HistoricalPackageOps origOps = getPackageOpsAt(i);
                HistoricalPackageOps spliceOps = origOps.splice(fractionToRemove);
                if (spliceOps != null) {
                    if (splice == null) {
                        splice = new HistoricalUidOps(this.mUid);
                    }
                    if (splice.mHistoricalPackageOps == null) {
                        splice.mHistoricalPackageOps = new ArrayMap<>();
                    }
                    splice.mHistoricalPackageOps.put(spliceOps.getPackageName(), spliceOps);
                }
            }
            return splice;
        }

        public void merge(HistoricalUidOps other) {
            int packageCount = other.getPackageCount();
            for (int i = 0; i < packageCount; i++) {
                HistoricalPackageOps otherPackageOps = other.getPackageOpsAt(i);
                HistoricalPackageOps thisPackageOps = getPackageOps(otherPackageOps.getPackageName());
                if (thisPackageOps != null) {
                    thisPackageOps.merge(otherPackageOps);
                } else {
                    if (this.mHistoricalPackageOps == null) {
                        this.mHistoricalPackageOps = new ArrayMap<>();
                    }
                    this.mHistoricalPackageOps.put(otherPackageOps.getPackageName(), otherPackageOps);
                }
            }
        }

        public void filter(String packageName, String attributionTag, String[] opNames, int filter, int historyFilter, double fractionToRemove, long beginTimeMillis, long endTimeMillis) {
            int packageCount = getPackageCount();
            for (int i = packageCount - 1; i >= 0; i--) {
                HistoricalPackageOps packageOps = getPackageOpsAt(i);
                if ((filter & 2) != 0 && !packageName.equals(packageOps.getPackageName())) {
                    this.mHistoricalPackageOps.removeAt(i);
                }
                packageOps.filter(attributionTag, opNames, filter, historyFilter, fractionToRemove, beginTimeMillis, endTimeMillis);
                if (packageOps.getAttributedOpsCount() == 0) {
                    this.mHistoricalPackageOps.removeAt(i);
                }
            }
        }

        public boolean isEmpty() {
            int packageCount = getPackageCount();
            for (int i = packageCount - 1; i >= 0; i--) {
                HistoricalPackageOps packageOps = this.mHistoricalPackageOps.valueAt(i);
                if (!packageOps.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        public void increaseAccessCount(int opCode, String packageName, String attributionTag, int uidState, int flags, long increment) {
            getOrCreateHistoricalPackageOps(packageName).increaseAccessCount(opCode, attributionTag, uidState, flags, increment);
        }

        public void increaseRejectCount(int opCode, String packageName, String attributionTag, int uidState, int flags, long increment) {
            getOrCreateHistoricalPackageOps(packageName).increaseRejectCount(opCode, attributionTag, uidState, flags, increment);
        }

        public void increaseAccessDuration(int opCode, String packageName, String attributionTag, int uidState, int flags, long increment) {
            getOrCreateHistoricalPackageOps(packageName).increaseAccessDuration(opCode, attributionTag, uidState, flags, increment);
        }

        public void addDiscreteAccess(int opCode, String packageName, String attributionTag, int uidState, int flag, long discreteAccessTime, long discreteAccessDuration, OpEventProxyInfo proxy) {
            getOrCreateHistoricalPackageOps(packageName).addDiscreteAccess(opCode, attributionTag, uidState, flag, discreteAccessTime, discreteAccessDuration, proxy);
        }

        public int getUid() {
            return this.mUid;
        }

        public int getPackageCount() {
            ArrayMap<String, HistoricalPackageOps> arrayMap = this.mHistoricalPackageOps;
            if (arrayMap == null) {
                return 0;
            }
            return arrayMap.size();
        }

        public HistoricalPackageOps getPackageOpsAt(int index) {
            ArrayMap<String, HistoricalPackageOps> arrayMap = this.mHistoricalPackageOps;
            if (arrayMap == null) {
                throw new IndexOutOfBoundsException();
            }
            return arrayMap.valueAt(index);
        }

        public HistoricalPackageOps getPackageOps(String packageName) {
            ArrayMap<String, HistoricalPackageOps> arrayMap = this.mHistoricalPackageOps;
            if (arrayMap == null) {
                return null;
            }
            return arrayMap.get(packageName);
        }

        public void clearHistory(String packageName) {
            ArrayMap<String, HistoricalPackageOps> arrayMap = this.mHistoricalPackageOps;
            if (arrayMap != null) {
                arrayMap.remove(packageName);
            }
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(this.mUid);
            parcel.writeTypedArrayMap(this.mHistoricalPackageOps, flags);
        }

        public void accept(HistoricalOpsVisitor visitor) {
            visitor.visitHistoricalUidOps(this);
            int packageCount = getPackageCount();
            for (int i = 0; i < packageCount; i++) {
                getPackageOpsAt(i).accept(visitor);
            }
        }

        private HistoricalPackageOps getOrCreateHistoricalPackageOps(String packageName) {
            if (this.mHistoricalPackageOps == null) {
                this.mHistoricalPackageOps = new ArrayMap<>();
            }
            HistoricalPackageOps historicalPackageOp = this.mHistoricalPackageOps.get(packageName);
            if (historicalPackageOp == null) {
                HistoricalPackageOps historicalPackageOp2 = new HistoricalPackageOps(packageName);
                this.mHistoricalPackageOps.put(packageName, historicalPackageOp2);
                return historicalPackageOp2;
            }
            return historicalPackageOp;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            HistoricalUidOps other = (HistoricalUidOps) obj;
            if (this.mUid != other.mUid) {
                return false;
            }
            ArrayMap<String, HistoricalPackageOps> arrayMap = this.mHistoricalPackageOps;
            if (arrayMap == null) {
                if (other.mHistoricalPackageOps != null) {
                    return false;
                }
            } else if (!arrayMap.equals(other.mHistoricalPackageOps)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            int result = this.mUid;
            int i = result * 31;
            ArrayMap<String, HistoricalPackageOps> arrayMap = this.mHistoricalPackageOps;
            int result2 = i + (arrayMap != null ? arrayMap.hashCode() : 0);
            return result2;
        }
    }

    @SystemApi
    /* loaded from: classes.dex */
    public static final class HistoricalPackageOps implements Parcelable {
        public static final Parcelable.Creator<HistoricalPackageOps> CREATOR = new Parcelable.Creator<HistoricalPackageOps>() { // from class: android.app.AppOpsManager.HistoricalPackageOps.1
            @Override // android.os.Parcelable.Creator
            public HistoricalPackageOps createFromParcel(Parcel parcel) {
                return new HistoricalPackageOps(parcel);
            }

            @Override // android.os.Parcelable.Creator
            public HistoricalPackageOps[] newArray(int size) {
                return new HistoricalPackageOps[size];
            }
        };
        private ArrayMap<String, AttributedHistoricalOps> mAttributedHistoricalOps;
        private final String mPackageName;

        public HistoricalPackageOps(String packageName) {
            this.mPackageName = packageName;
        }

        private HistoricalPackageOps(HistoricalPackageOps other) {
            this.mPackageName = other.mPackageName;
            int opCount = other.getAttributedOpsCount();
            for (int i = 0; i < opCount; i++) {
                AttributedHistoricalOps origOps = other.getAttributedOpsAt(i);
                AttributedHistoricalOps cloneOps = new AttributedHistoricalOps(origOps);
                if (this.mAttributedHistoricalOps == null) {
                    this.mAttributedHistoricalOps = new ArrayMap<>(opCount);
                }
                this.mAttributedHistoricalOps.put(cloneOps.getTag(), cloneOps);
            }
        }

        private HistoricalPackageOps(Parcel parcel) {
            this.mPackageName = parcel.readString();
            this.mAttributedHistoricalOps = parcel.createTypedArrayMap(AttributedHistoricalOps.CREATOR);
        }

        public HistoricalPackageOps splice(double fractionToRemove) {
            HistoricalPackageOps splice = null;
            int attributionCount = getAttributedOpsCount();
            for (int i = 0; i < attributionCount; i++) {
                AttributedHistoricalOps origOps = getAttributedOpsAt(i);
                AttributedHistoricalOps spliceOps = origOps.splice(fractionToRemove);
                if (spliceOps != null) {
                    if (splice == null) {
                        splice = new HistoricalPackageOps(this.mPackageName);
                    }
                    if (splice.mAttributedHistoricalOps == null) {
                        splice.mAttributedHistoricalOps = new ArrayMap<>();
                    }
                    splice.mAttributedHistoricalOps.put(spliceOps.getTag(), spliceOps);
                }
            }
            return splice;
        }

        public void merge(HistoricalPackageOps other) {
            int attributionCount = other.getAttributedOpsCount();
            for (int i = 0; i < attributionCount; i++) {
                AttributedHistoricalOps otherAttributionOps = other.getAttributedOpsAt(i);
                AttributedHistoricalOps thisAttributionOps = getAttributedOps(otherAttributionOps.getTag());
                if (thisAttributionOps != null) {
                    thisAttributionOps.merge(otherAttributionOps);
                } else {
                    if (this.mAttributedHistoricalOps == null) {
                        this.mAttributedHistoricalOps = new ArrayMap<>();
                    }
                    this.mAttributedHistoricalOps.put(otherAttributionOps.getTag(), otherAttributionOps);
                }
            }
        }

        public void filter(String attributionTag, String[] opNames, int filter, int historyFilter, double fractionToRemove, long beginTimeMillis, long endTimeMillis) {
            int attributionCount = getAttributedOpsCount();
            for (int i = attributionCount - 1; i >= 0; i--) {
                AttributedHistoricalOps attributionOps = getAttributedOpsAt(i);
                if ((filter & 4) != 0 && !Objects.equals(attributionTag, attributionOps.getTag())) {
                    this.mAttributedHistoricalOps.removeAt(i);
                }
                attributionOps.filter(opNames, filter, historyFilter, fractionToRemove, beginTimeMillis, endTimeMillis);
                if (attributionOps.getOpCount() == 0) {
                    this.mAttributedHistoricalOps.removeAt(i);
                }
            }
        }

        public void accept(HistoricalOpsVisitor visitor) {
            visitor.visitHistoricalPackageOps(this);
            int attributionCount = getAttributedOpsCount();
            for (int i = 0; i < attributionCount; i++) {
                getAttributedOpsAt(i).accept(visitor);
            }
        }

        public boolean isEmpty() {
            int attributionCount = getAttributedOpsCount();
            for (int i = attributionCount - 1; i >= 0; i--) {
                AttributedHistoricalOps attributionOps = this.mAttributedHistoricalOps.valueAt(i);
                if (!attributionOps.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        public void increaseAccessCount(int opCode, String attributionTag, int uidState, int flags, long increment) {
            getOrCreateAttributedHistoricalOps(attributionTag).increaseAccessCount(opCode, uidState, flags, increment);
        }

        public void increaseRejectCount(int opCode, String attributionTag, int uidState, int flags, long increment) {
            getOrCreateAttributedHistoricalOps(attributionTag).increaseRejectCount(opCode, uidState, flags, increment);
        }

        public void increaseAccessDuration(int opCode, String attributionTag, int uidState, int flags, long increment) {
            getOrCreateAttributedHistoricalOps(attributionTag).increaseAccessDuration(opCode, uidState, flags, increment);
        }

        public void addDiscreteAccess(int opCode, String attributionTag, int uidState, int flag, long discreteAccessTime, long discreteAccessDuration, OpEventProxyInfo proxy) {
            getOrCreateAttributedHistoricalOps(attributionTag).addDiscreteAccess(opCode, uidState, flag, discreteAccessTime, discreteAccessDuration, proxy);
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        private AttributedHistoricalOps getOrCreateAttributedHistoricalOps(String attributionTag) {
            if (this.mAttributedHistoricalOps == null) {
                this.mAttributedHistoricalOps = new ArrayMap<>();
            }
            AttributedHistoricalOps historicalAttributionOp = this.mAttributedHistoricalOps.get(attributionTag);
            if (historicalAttributionOp == null) {
                AttributedHistoricalOps historicalAttributionOp2 = new AttributedHistoricalOps(attributionTag);
                this.mAttributedHistoricalOps.put(attributionTag, historicalAttributionOp2);
                return historicalAttributionOp2;
            }
            return historicalAttributionOp;
        }

        public int getOpCount() {
            int numOps = 0;
            int numAttributions = getAttributedOpsCount();
            for (int code = 0; code < AppOpsManager.getNumOps(); code++) {
                String opName = AppOpsManager.opToPublicName(code);
                int attributionNum = 0;
                while (true) {
                    if (attributionNum < numAttributions) {
                        if (getAttributedOpsAt(attributionNum).getOp(opName) == null) {
                            attributionNum++;
                        } else {
                            numOps++;
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            return numOps;
        }

        /* JADX WARN: Code restructure failed: missing block: B:36:0x002a, code lost:
            r2 = r2 + 1;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public HistoricalOp getOpAt(int index) {
            int numOpsFound = 0;
            int numAttributions = getAttributedOpsCount();
            int code = 0;
            while (code < AppOpsManager.getNumOps()) {
                String opName = AppOpsManager.opToPublicName(code);
                int attributionNum = 0;
                while (true) {
                    if (attributionNum >= numAttributions) {
                        break;
                    } else if (getAttributedOpsAt(attributionNum).getOp(opName) == null) {
                        attributionNum++;
                    } else if (numOpsFound == index) {
                        return getOp(opName);
                    } else {
                        numOpsFound++;
                    }
                }
            }
            throw new IndexOutOfBoundsException();
        }

        public HistoricalOp getOp(String opName) {
            if (this.mAttributedHistoricalOps == null) {
                return null;
            }
            HistoricalOp combinedOp = null;
            int numAttributions = getAttributedOpsCount();
            for (int i = 0; i < numAttributions; i++) {
                HistoricalOp attributionOp = getAttributedOpsAt(i).getOp(opName);
                if (attributionOp != null) {
                    if (combinedOp == null) {
                        combinedOp = new HistoricalOp(attributionOp);
                    } else {
                        combinedOp.merge(attributionOp);
                    }
                }
            }
            return combinedOp;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(this.mPackageName);
            parcel.writeTypedArrayMap(this.mAttributedHistoricalOps, flags);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            HistoricalPackageOps other = (HistoricalPackageOps) obj;
            if (!this.mPackageName.equals(other.mPackageName)) {
                return false;
            }
            ArrayMap<String, AttributedHistoricalOps> arrayMap = this.mAttributedHistoricalOps;
            if (arrayMap == null) {
                if (other.mAttributedHistoricalOps != null) {
                    return false;
                }
            } else if (!arrayMap.equals(other.mAttributedHistoricalOps)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            String str = this.mPackageName;
            int result = str != null ? str.hashCode() : 0;
            int i = result * 31;
            ArrayMap<String, AttributedHistoricalOps> arrayMap = this.mAttributedHistoricalOps;
            int result2 = i + (arrayMap != null ? arrayMap.hashCode() : 0);
            return result2;
        }

        public int getAttributedOpsCount() {
            ArrayMap<String, AttributedHistoricalOps> arrayMap = this.mAttributedHistoricalOps;
            if (arrayMap == null) {
                return 0;
            }
            return arrayMap.size();
        }

        public AttributedHistoricalOps getAttributedOpsAt(int index) {
            ArrayMap<String, AttributedHistoricalOps> arrayMap = this.mAttributedHistoricalOps;
            if (arrayMap == null) {
                throw new IndexOutOfBoundsException();
            }
            return arrayMap.valueAt(index);
        }

        public AttributedHistoricalOps getAttributedOps(String attributionTag) {
            ArrayMap<String, AttributedHistoricalOps> arrayMap = this.mAttributedHistoricalOps;
            if (arrayMap == null) {
                return null;
            }
            return arrayMap.get(attributionTag);
        }
    }

    @SystemApi
    /* loaded from: classes.dex */
    public static final class AttributedHistoricalOps implements Parcelable {
        public static final Parcelable.Creator<AttributedHistoricalOps> CREATOR = new Parcelable.Creator<AttributedHistoricalOps>() { // from class: android.app.AppOpsManager.AttributedHistoricalOps.1
            @Override // android.os.Parcelable.Creator
            public AttributedHistoricalOps[] newArray(int size) {
                return new AttributedHistoricalOps[size];
            }

            @Override // android.os.Parcelable.Creator
            public AttributedHistoricalOps createFromParcel(Parcel in) {
                return new AttributedHistoricalOps(in);
            }
        };
        private ArrayMap<String, HistoricalOp> mHistoricalOps;
        private final String mTag;

        public AttributedHistoricalOps(String tag) {
            this.mTag = tag;
        }

        private AttributedHistoricalOps(AttributedHistoricalOps other) {
            this.mTag = other.mTag;
            int opCount = other.getOpCount();
            for (int i = 0; i < opCount; i++) {
                HistoricalOp origOp = other.getOpAt(i);
                if (origOp.getOpCode() < AppOpsManager.getNumOps()) {
                    HistoricalOp cloneOp = new HistoricalOp(origOp);
                    if (this.mHistoricalOps == null) {
                        this.mHistoricalOps = new ArrayMap<>(opCount);
                    }
                    this.mHistoricalOps.put(cloneOp.getOpName(), cloneOp);
                }
            }
        }

        public AttributedHistoricalOps splice(double fractionToRemove) {
            HistoricalOp spliceOps;
            AttributedHistoricalOps splice = null;
            int opCount = getOpCount();
            for (int i = 0; i < opCount; i++) {
                HistoricalOp origOps = getOpAt(i);
                if (origOps.getOpCode() < AppOpsManager.getNumOps() && (spliceOps = origOps.splice(fractionToRemove)) != null) {
                    if (splice == null) {
                        splice = new AttributedHistoricalOps(this.mTag, (ArrayMap<String, HistoricalOp>) null);
                    }
                    if (splice.mHistoricalOps == null) {
                        splice.mHistoricalOps = new ArrayMap<>();
                    }
                    splice.mHistoricalOps.put(spliceOps.getOpName(), spliceOps);
                }
            }
            return splice;
        }

        public void merge(AttributedHistoricalOps other) {
            int opCount = other.getOpCount();
            for (int i = 0; i < opCount; i++) {
                HistoricalOp otherOp = other.getOpAt(i);
                if (otherOp.getOpCode() < AppOpsManager.getNumOps()) {
                    HistoricalOp thisOp = getOp(otherOp.getOpName());
                    if (thisOp != null) {
                        thisOp.merge(otherOp);
                    } else {
                        if (this.mHistoricalOps == null) {
                            this.mHistoricalOps = new ArrayMap<>();
                        }
                        this.mHistoricalOps.put(otherOp.getOpName(), otherOp);
                    }
                }
            }
        }

        public void filter(String[] opNames, int filter, int historyFilter, double scaleFactor, long beginTimeMillis, long endTimeMillis) {
            int opCount = getOpCount();
            for (int i = opCount - 1; i >= 0; i--) {
                HistoricalOp op = this.mHistoricalOps.valueAt(i);
                if (op.getOpCode() < AppOpsManager.getNumOps()) {
                    if ((filter & 8) != 0 && !ArrayUtils.contains(opNames, op.getOpName())) {
                        this.mHistoricalOps.removeAt(i);
                    }
                    op.filter(historyFilter, scaleFactor, beginTimeMillis, endTimeMillis);
                }
            }
        }

        public boolean isEmpty() {
            int opCount = getOpCount();
            for (int i = opCount - 1; i >= 0; i--) {
                HistoricalOp op = this.mHistoricalOps.valueAt(i);
                if (!op.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        public void increaseAccessCount(int opCode, int uidState, int flags, long increment) {
            getOrCreateHistoricalOp(opCode).increaseAccessCount(uidState, flags, increment);
        }

        public void increaseRejectCount(int opCode, int uidState, int flags, long increment) {
            getOrCreateHistoricalOp(opCode).increaseRejectCount(uidState, flags, increment);
        }

        public void increaseAccessDuration(int opCode, int uidState, int flags, long increment) {
            getOrCreateHistoricalOp(opCode).increaseAccessDuration(uidState, flags, increment);
        }

        public void addDiscreteAccess(int opCode, int uidState, int flag, long discreteAccessTime, long discreteAccessDuration, OpEventProxyInfo proxy) {
            getOrCreateHistoricalOp(opCode).addDiscreteAccess(uidState, flag, discreteAccessTime, discreteAccessDuration, proxy);
        }

        public int getOpCount() {
            ArrayMap<String, HistoricalOp> arrayMap = this.mHistoricalOps;
            if (arrayMap == null) {
                return 0;
            }
            return arrayMap.size();
        }

        public HistoricalOp getOpAt(int index) {
            ArrayMap<String, HistoricalOp> arrayMap = this.mHistoricalOps;
            if (arrayMap == null) {
                throw new IndexOutOfBoundsException();
            }
            return arrayMap.valueAt(index);
        }

        public HistoricalOp getOp(String opName) {
            ArrayMap<String, HistoricalOp> arrayMap = this.mHistoricalOps;
            if (arrayMap == null) {
                return null;
            }
            return arrayMap.get(opName);
        }

        public void accept(HistoricalOpsVisitor visitor) {
            visitor.visitHistoricalAttributionOps(this);
            int opCount = getOpCount();
            for (int i = 0; i < opCount; i++) {
                getOpAt(i).accept(visitor);
            }
        }

        private HistoricalOp getOrCreateHistoricalOp(int opCode) {
            String opStr;
            if (this.mHistoricalOps == null) {
                this.mHistoricalOps = new ArrayMap<>();
            }
            if (AppOpsManager.sCtaManager.isCtaSupported()) {
                opStr = AppOpsManager.getOpStrs()[opCode];
            } else {
                opStr = AppOpsManager.sOpToString[opCode];
            }
            HistoricalOp op = this.mHistoricalOps.get(opStr);
            if (op == null) {
                HistoricalOp op2 = new HistoricalOp(opCode);
                this.mHistoricalOps.put(opStr, op2);
                return op2;
            }
            return op;
        }

        public AttributedHistoricalOps(String tag, ArrayMap<String, HistoricalOp> historicalOps) {
            this.mTag = tag;
            this.mHistoricalOps = historicalOps;
        }

        public String getTag() {
            return this.mTag;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AttributedHistoricalOps that = (AttributedHistoricalOps) o;
            if (Objects.equals(this.mTag, that.mTag) && Objects.equals(this.mHistoricalOps, that.mHistoricalOps)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int _hash = (1 * 31) + Objects.hashCode(this.mTag);
            return (_hash * 31) + Objects.hashCode(this.mHistoricalOps);
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            byte flg = this.mTag != null ? (byte) (0 | 1) : (byte) 0;
            if (this.mHistoricalOps != null) {
                flg = (byte) (flg | 2);
            }
            dest.writeByte(flg);
            String str = this.mTag;
            if (str != null) {
                dest.writeString(str);
            }
            ArrayMap<String, HistoricalOp> arrayMap = this.mHistoricalOps;
            if (arrayMap != null) {
                dest.writeMap(arrayMap);
            }
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        AttributedHistoricalOps(Parcel in) {
            byte flg = in.readByte();
            String attributionTag = (flg & 1) == 0 ? null : in.readString();
            ArrayMap<String, HistoricalOp> historicalOps = null;
            if ((flg & 2) != 0) {
                historicalOps = new ArrayMap<>();
                in.readMap(historicalOps, HistoricalOp.class.getClassLoader());
            }
            this.mTag = attributionTag;
            this.mHistoricalOps = historicalOps;
        }
    }

    @SystemApi
    /* loaded from: classes.dex */
    public static final class HistoricalOp implements Parcelable {
        public static final Parcelable.Creator<HistoricalOp> CREATOR = new Parcelable.Creator<HistoricalOp>() { // from class: android.app.AppOpsManager.HistoricalOp.1
            @Override // android.os.Parcelable.Creator
            public HistoricalOp createFromParcel(Parcel source) {
                return new HistoricalOp(source);
            }

            @Override // android.os.Parcelable.Creator
            public HistoricalOp[] newArray(int size) {
                return new HistoricalOp[size];
            }
        };
        private LongSparseLongArray mAccessCount;
        private LongSparseLongArray mAccessDuration;
        private List<AttributedOpEntry> mDiscreteAccesses;
        private final int mOp;
        private LongSparseLongArray mRejectCount;

        public HistoricalOp(int op) {
            this.mOp = op;
        }

        private HistoricalOp(HistoricalOp other) {
            this.mOp = other.mOp;
            LongSparseLongArray longSparseLongArray = other.mAccessCount;
            if (longSparseLongArray != null) {
                this.mAccessCount = longSparseLongArray.m4669clone();
            }
            LongSparseLongArray longSparseLongArray2 = other.mRejectCount;
            if (longSparseLongArray2 != null) {
                this.mRejectCount = longSparseLongArray2.m4669clone();
            }
            LongSparseLongArray longSparseLongArray3 = other.mAccessDuration;
            if (longSparseLongArray3 != null) {
                this.mAccessDuration = longSparseLongArray3.m4669clone();
            }
            int historicalOpCount = other.getDiscreteAccessCount();
            for (int i = 0; i < historicalOpCount; i++) {
                AttributedOpEntry origOp = other.getDiscreteAccessAt(i);
                AttributedOpEntry cloneOp = new AttributedOpEntry(origOp);
                getOrCreateDiscreteAccesses().add(cloneOp);
            }
        }

        private HistoricalOp(Parcel parcel) {
            this.mOp = parcel.readInt();
            this.mAccessCount = AppOpsManager.readLongSparseLongArrayFromParcel(parcel);
            this.mRejectCount = AppOpsManager.readLongSparseLongArrayFromParcel(parcel);
            this.mAccessDuration = AppOpsManager.readLongSparseLongArrayFromParcel(parcel);
            this.mDiscreteAccesses = AppOpsManager.readDiscreteAccessArrayFromParcel(parcel);
        }

        public void filter(int historyFlag, double scaleFactor, long beginTimeMillis, long endTimeMillis) {
            if ((historyFlag & 1) == 0) {
                this.mAccessCount = null;
                this.mRejectCount = null;
                this.mAccessDuration = null;
            } else {
                scale(this.mAccessCount, scaleFactor);
                scale(this.mRejectCount, scaleFactor);
                scale(this.mAccessDuration, scaleFactor);
            }
            if ((historyFlag & 2) == 0) {
                this.mDiscreteAccesses = null;
                return;
            }
            int discreteOpCount = getDiscreteAccessCount();
            for (int i = discreteOpCount - 1; i >= 0; i--) {
                AttributedOpEntry op = this.mDiscreteAccesses.get(i);
                long opBeginTime = op.getLastAccessTime(31);
                long opEndTime = op.getLastDuration(31) + opBeginTime;
                if (Long.max(opBeginTime, opEndTime) < beginTimeMillis || opBeginTime > endTimeMillis) {
                    this.mDiscreteAccesses.remove(i);
                }
            }
        }

        public boolean isEmpty() {
            return (hasData(this.mAccessCount) || hasData(this.mRejectCount) || hasData(this.mAccessDuration) || this.mDiscreteAccesses != null) ? false : true;
        }

        private boolean hasData(LongSparseLongArray array) {
            return array != null && array.size() > 0;
        }

        public HistoricalOp splice(double fractionToRemove) {
            HistoricalOp splice = new HistoricalOp(this.mOp);
            LongSparseLongArray longSparseLongArray = this.mAccessCount;
            Objects.requireNonNull(splice);
            splice(longSparseLongArray, new AppOpsManager$HistoricalOp$$ExternalSyntheticLambda0(splice), fractionToRemove);
            LongSparseLongArray longSparseLongArray2 = this.mRejectCount;
            Objects.requireNonNull(splice);
            splice(longSparseLongArray2, new AppOpsManager$HistoricalOp$$ExternalSyntheticLambda1(splice), fractionToRemove);
            LongSparseLongArray longSparseLongArray3 = this.mAccessDuration;
            Objects.requireNonNull(splice);
            splice(longSparseLongArray3, new AppOpsManager$HistoricalOp$$ExternalSyntheticLambda2(splice), fractionToRemove);
            return splice;
        }

        private static void splice(LongSparseLongArray sourceContainer, Supplier<LongSparseLongArray> destContainerProvider, double fractionToRemove) {
            if (sourceContainer != null) {
                int size = sourceContainer.size();
                for (int i = 0; i < size; i++) {
                    long key = sourceContainer.keyAt(i);
                    long value = sourceContainer.valueAt(i);
                    long removedFraction = Math.round(value * fractionToRemove);
                    if (removedFraction > 0) {
                        destContainerProvider.get().put(key, removedFraction);
                        sourceContainer.put(key, value - removedFraction);
                    }
                }
            }
        }

        public void merge(HistoricalOp other) {
            merge(new AppOpsManager$HistoricalOp$$ExternalSyntheticLambda0(this), other.mAccessCount);
            merge(new AppOpsManager$HistoricalOp$$ExternalSyntheticLambda1(this), other.mRejectCount);
            merge(new AppOpsManager$HistoricalOp$$ExternalSyntheticLambda2(this), other.mAccessDuration);
            if (other.mDiscreteAccesses == null) {
                return;
            }
            if (this.mDiscreteAccesses == null) {
                this.mDiscreteAccesses = new ArrayList(other.mDiscreteAccesses);
                return;
            }
            List<AttributedOpEntry> historicalDiscreteAccesses = new ArrayList<>();
            int otherHistoricalOpCount = other.getDiscreteAccessCount();
            int historicalOpCount = getDiscreteAccessCount();
            int i = 0;
            int j = 0;
            while (true) {
                if (i < otherHistoricalOpCount || j < historicalOpCount) {
                    if (i == otherHistoricalOpCount) {
                        historicalDiscreteAccesses.add(this.mDiscreteAccesses.get(j));
                        j++;
                    } else if (j == historicalOpCount) {
                        historicalDiscreteAccesses.add(other.mDiscreteAccesses.get(i));
                        i++;
                    } else if (this.mDiscreteAccesses.get(j).getLastAccessTime(31) < other.mDiscreteAccesses.get(i).getLastAccessTime(31)) {
                        historicalDiscreteAccesses.add(this.mDiscreteAccesses.get(j));
                        j++;
                    } else {
                        historicalDiscreteAccesses.add(other.mDiscreteAccesses.get(i));
                        i++;
                    }
                } else {
                    this.mDiscreteAccesses = AppOpsManager.deduplicateDiscreteEvents(historicalDiscreteAccesses);
                    return;
                }
            }
        }

        public void increaseAccessCount(int uidState, int flags, long increment) {
            increaseCount(getOrCreateAccessCount(), uidState, flags, increment);
        }

        public void increaseRejectCount(int uidState, int flags, long increment) {
            increaseCount(getOrCreateRejectCount(), uidState, flags, increment);
        }

        public void increaseAccessDuration(int uidState, int flags, long increment) {
            increaseCount(getOrCreateAccessDuration(), uidState, flags, increment);
        }

        private void increaseCount(LongSparseLongArray counts, int uidState, int flags, long increment) {
            while (flags != 0) {
                int flag = 1 << Integer.numberOfTrailingZeros(flags);
                flags &= ~flag;
                long key = AppOpsManager.makeKey(uidState, flag);
                counts.put(key, counts.get(key) + increment);
            }
        }

        public void addDiscreteAccess(int uidState, int flag, long discreteAccessTime, long discreteAccessDuration, OpEventProxyInfo proxy) {
            List<AttributedOpEntry> discreteAccesses = getOrCreateDiscreteAccesses();
            LongSparseArray<NoteOpEvent> accessEvents = new LongSparseArray<>();
            long key = AppOpsManager.makeKey(uidState, flag);
            NoteOpEvent note = new NoteOpEvent(discreteAccessTime, discreteAccessDuration, proxy);
            accessEvents.append(key, note);
            AttributedOpEntry access = new AttributedOpEntry(this.mOp, false, accessEvents, null);
            int insertionPoint = discreteAccesses.size() - 1;
            while (insertionPoint >= 0 && discreteAccesses.get(insertionPoint).getLastAccessTime(31) >= discreteAccessTime) {
                insertionPoint--;
            }
            int insertionPoint2 = insertionPoint + 1;
            if (insertionPoint2 < discreteAccesses.size() && discreteAccesses.get(insertionPoint2).getLastAccessTime(31) == discreteAccessTime) {
                discreteAccesses.set(insertionPoint2, AppOpsManager.mergeAttributedOpEntries(Arrays.asList(discreteAccesses.get(insertionPoint2), access)));
            } else {
                discreteAccesses.add(insertionPoint2, access);
            }
        }

        public String getOpName() {
            if (AppOpsManager.sCtaManager.isCtaSupported()) {
                return AppOpsManager.getOpStrs()[this.mOp];
            }
            return AppOpsManager.sOpToString[this.mOp];
        }

        public int getOpCode() {
            return this.mOp;
        }

        public int getDiscreteAccessCount() {
            List<AttributedOpEntry> list = this.mDiscreteAccesses;
            if (list == null) {
                return 0;
            }
            return list.size();
        }

        public AttributedOpEntry getDiscreteAccessAt(int index) {
            List<AttributedOpEntry> list = this.mDiscreteAccesses;
            if (list == null) {
                throw new IndexOutOfBoundsException();
            }
            return list.get(index);
        }

        public long getForegroundAccessCount(int flags) {
            return AppOpsManager.sumForFlagsInStates(this.mAccessCount, 100, AppOpsManager.resolveFirstUnrestrictedUidState(this.mOp), flags);
        }

        public List<AttributedOpEntry> getForegroundDiscreteAccesses(int flags) {
            return AppOpsManager.listForFlagsInStates(this.mDiscreteAccesses, 100, AppOpsManager.resolveFirstUnrestrictedUidState(this.mOp), flags);
        }

        public long getBackgroundAccessCount(int flags) {
            return AppOpsManager.sumForFlagsInStates(this.mAccessCount, AppOpsManager.resolveLastRestrictedUidState(this.mOp), 700, flags);
        }

        public List<AttributedOpEntry> getBackgroundDiscreteAccesses(int flags) {
            return AppOpsManager.listForFlagsInStates(this.mDiscreteAccesses, AppOpsManager.resolveLastRestrictedUidState(this.mOp), 700, flags);
        }

        public long getAccessCount(int fromUidState, int toUidState, int flags) {
            return AppOpsManager.sumForFlagsInStates(this.mAccessCount, fromUidState, toUidState, flags);
        }

        public List<AttributedOpEntry> getDiscreteAccesses(int fromUidState, int toUidState, int flags) {
            return AppOpsManager.listForFlagsInStates(this.mDiscreteAccesses, fromUidState, toUidState, flags);
        }

        public long getForegroundRejectCount(int flags) {
            return AppOpsManager.sumForFlagsInStates(this.mRejectCount, 100, AppOpsManager.resolveFirstUnrestrictedUidState(this.mOp), flags);
        }

        public long getBackgroundRejectCount(int flags) {
            return AppOpsManager.sumForFlagsInStates(this.mRejectCount, AppOpsManager.resolveLastRestrictedUidState(this.mOp), 700, flags);
        }

        public long getRejectCount(int fromUidState, int toUidState, int flags) {
            return AppOpsManager.sumForFlagsInStates(this.mRejectCount, fromUidState, toUidState, flags);
        }

        public long getForegroundAccessDuration(int flags) {
            return AppOpsManager.sumForFlagsInStates(this.mAccessDuration, 100, AppOpsManager.resolveFirstUnrestrictedUidState(this.mOp), flags);
        }

        public long getBackgroundAccessDuration(int flags) {
            return AppOpsManager.sumForFlagsInStates(this.mAccessDuration, AppOpsManager.resolveLastRestrictedUidState(this.mOp), 700, flags);
        }

        public long getAccessDuration(int fromUidState, int toUidState, int flags) {
            return AppOpsManager.sumForFlagsInStates(this.mAccessDuration, fromUidState, toUidState, flags);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(this.mOp);
            AppOpsManager.writeLongSparseLongArrayToParcel(this.mAccessCount, parcel);
            AppOpsManager.writeLongSparseLongArrayToParcel(this.mRejectCount, parcel);
            AppOpsManager.writeLongSparseLongArrayToParcel(this.mAccessDuration, parcel);
            AppOpsManager.writeDiscreteAccessArrayToParcel(this.mDiscreteAccesses, parcel, flags);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            HistoricalOp other = (HistoricalOp) obj;
            if (this.mOp != other.mOp || !AppOpsManager.equalsLongSparseLongArray(this.mAccessCount, other.mAccessCount) || !AppOpsManager.equalsLongSparseLongArray(this.mRejectCount, other.mRejectCount) || !AppOpsManager.equalsLongSparseLongArray(this.mAccessDuration, other.mAccessDuration)) {
                return false;
            }
            List<AttributedOpEntry> list = this.mDiscreteAccesses;
            if (list == null) {
                if (other.mDiscreteAccesses == null) {
                    return true;
                }
                return false;
            }
            return list.equals(other.mDiscreteAccesses);
        }

        public int hashCode() {
            int result = this.mOp;
            return (((((((result * 31) + Objects.hashCode(this.mAccessCount)) * 31) + Objects.hashCode(this.mRejectCount)) * 31) + Objects.hashCode(this.mAccessDuration)) * 31) + Objects.hashCode(this.mDiscreteAccesses);
        }

        public void accept(HistoricalOpsVisitor visitor) {
            visitor.visitHistoricalOp(this);
        }

        public LongSparseLongArray getOrCreateAccessCount() {
            if (this.mAccessCount == null) {
                this.mAccessCount = new LongSparseLongArray();
            }
            return this.mAccessCount;
        }

        public LongSparseLongArray getOrCreateRejectCount() {
            if (this.mRejectCount == null) {
                this.mRejectCount = new LongSparseLongArray();
            }
            return this.mRejectCount;
        }

        public LongSparseLongArray getOrCreateAccessDuration() {
            if (this.mAccessDuration == null) {
                this.mAccessDuration = new LongSparseLongArray();
            }
            return this.mAccessDuration;
        }

        private List<AttributedOpEntry> getOrCreateDiscreteAccesses() {
            if (this.mDiscreteAccesses == null) {
                this.mDiscreteAccesses = new ArrayList();
            }
            return this.mDiscreteAccesses;
        }

        private static void scale(LongSparseLongArray data, double scaleFactor) {
            if (data != null) {
                int size = data.size();
                for (int i = 0; i < size; i++) {
                    data.put(data.keyAt(i), (long) HistoricalOps.round(data.valueAt(i) * scaleFactor));
                }
            }
        }

        private static void merge(Supplier<LongSparseLongArray> thisSupplier, LongSparseLongArray other) {
            if (other != null) {
                int otherSize = other.size();
                for (int i = 0; i < otherSize; i++) {
                    LongSparseLongArray that = thisSupplier.get();
                    long otherKey = other.keyAt(i);
                    long otherValue = other.valueAt(i);
                    that.put(otherKey, that.get(otherKey) + otherValue);
                }
            }
        }

        public LongSparseArray<Object> collectKeys() {
            LongSparseArray<Object> result = AppOpsManager.collectKeys(this.mAccessCount, null);
            return AppOpsManager.collectKeys(this.mAccessDuration, AppOpsManager.collectKeys(this.mRejectCount, result));
        }
    }

    public static long sumForFlagsInStates(LongSparseLongArray counts, int beginUidState, int endUidState, int flags) {
        int[] iArr;
        if (counts == null) {
            return 0L;
        }
        long sum = 0;
        while (flags != 0) {
            int flag = 1 << Integer.numberOfTrailingZeros(flags);
            flags &= ~flag;
            for (int uidState : UID_STATES) {
                if (uidState >= beginUidState && uidState <= endUidState) {
                    long key = makeKey(uidState, flag);
                    sum += counts.get(key);
                }
            }
        }
        return sum;
    }

    public static List<AttributedOpEntry> listForFlagsInStates(List<AttributedOpEntry> accesses, int beginUidState, int endUidState, int flags) {
        List<AttributedOpEntry> result = new ArrayList<>();
        if (accesses == null) {
            return result;
        }
        int nAccesses = accesses.size();
        for (int i = 0; i < nAccesses; i++) {
            AttributedOpEntry entry = accesses.get(i);
            if (entry.getLastAccessTime(beginUidState, endUidState, flags) != -1) {
                result.add(entry);
            }
        }
        return deduplicateDiscreteEvents(result);
    }

    /* loaded from: classes.dex */
    public interface OnOpActiveChangedListener {
        void onOpActiveChanged(String str, int i, String str2, boolean z);

        default void onOpActiveChanged(String op, int uid, String packageName, String attributionTag, boolean active, int attributionFlags, int attributionChainId) {
            onOpActiveChanged(op, uid, packageName, active);
        }
    }

    /* loaded from: classes.dex */
    public static class OnOpChangedInternalListener implements OnOpChangedListener {
        @Override // android.app.AppOpsManager.OnOpChangedListener
        public void onOpChanged(String op, String packageName) {
        }

        public void onOpChanged(int op, String packageName) {
        }
    }

    /* loaded from: classes.dex */
    public interface OnOpActiveChangedInternalListener extends OnOpActiveChangedListener {
        @Override // android.app.AppOpsManager.OnOpActiveChangedListener
        default void onOpActiveChanged(String op, int uid, String packageName, boolean active) {
        }

        default void onOpActiveChanged(int op, int uid, String packageName, boolean active) {
        }
    }

    /* loaded from: classes.dex */
    public interface OnOpStartedListener {
        public static final int START_TYPE_FAILED = 0;
        public static final int START_TYPE_RESUMED = 2;
        public static final int START_TYPE_STARTED = 1;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        public @interface StartedType {
        }

        void onOpStarted(int i, int i2, String str, String str2, int i3, int i4);

        default void onOpStarted(int op, int uid, String packageName, String attributionTag, int flags, int result, int startType, int attributionFlags, int attributionChainId) {
            if (startType != 2) {
                onOpStarted(op, uid, packageName, attributionTag, flags, result);
            }
        }
    }

    public AppOpsManager(Context context, IAppOpsService service) {
        PackageManager pm;
        this.mContext = context;
        this.mService = service;
        if (context != null && (pm = context.getPackageManager()) != null) {
            try {
                if (pm.checkPermission(Manifest.permission.READ_DEVICE_CONFIG, context.getPackageName()) == 0) {
                    DeviceConfig.addOnPropertiesChangedListener(DeviceConfig.NAMESPACE_PRIVACY, context.getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: android.app.AppOpsManager$$ExternalSyntheticLambda5
                        @Override // android.provider.DeviceConfig.OnPropertiesChangedListener
                        public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                            AppOpsManager.lambda$new$0(properties);
                        }
                    });
                    return;
                }
            } catch (Exception e) {
            }
        }
        sFullLog = false;
    }

    public static /* synthetic */ void lambda$new$0(DeviceConfig.Properties properties) {
        if (properties.getKeyset().contains(FULL_LOG)) {
            sFullLog = Boolean.valueOf(properties.getBoolean(FULL_LOG, false));
        }
    }

    @SystemApi
    public List<PackageOps> getPackagesForOps(String[] ops) {
        int opCount = ops.length;
        int[] opCodes = new int[opCount];
        for (int i = 0; i < opCount; i++) {
            if (sCtaManager.isCtaSupported()) {
                opCodes[i] = strOpToOp(ops[i]);
            } else {
                opCodes[i] = sOpStrToOp.get(ops[i]).intValue();
            }
        }
        List<PackageOps> result = getPackagesForOps(opCodes);
        return result != null ? result : Collections.emptyList();
    }

    public List<PackageOps> getPackagesForOps(int[] ops) {
        try {
            return this.mService.getPackagesForOps(ops);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    @Deprecated
    public List<PackageOps> getOpsForPackage(int uid, String packageName, int[] ops) {
        try {
            return this.mService.getOpsForPackage(uid, packageName, ops);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public List<PackageOps> getOpsForPackage(int uid, String packageName, String... ops) {
        int[] opCodes = null;
        if (ops != null) {
            opCodes = new int[ops.length];
            for (int i = 0; i < ops.length; i++) {
                opCodes[i] = strOpToOp(ops[i]);
            }
        }
        try {
            List<PackageOps> result = this.mService.getOpsForPackage(uid, packageName, opCodes);
            if (result == null) {
                return Collections.emptyList();
            }
            return result;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void getHistoricalOps(HistoricalOpsRequest request, final Executor executor, final Consumer<HistoricalOps> callback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");
        try {
            this.mService.getHistoricalOps(request.mUid, request.mPackageName, request.mAttributionTag, request.mOpNames, request.mHistoryFlags, request.mFilter, request.mBeginTimeMillis, request.mEndTimeMillis, request.mFlags, new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: android.app.AppOpsManager$$ExternalSyntheticLambda0
                @Override // android.os.RemoteCallback.OnResultListener
                public final void onResult(Bundle bundle) {
                    AppOpsManager.lambda$getHistoricalOps$2(executor, callback, bundle);
                }
            }));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static /* synthetic */ void lambda$getHistoricalOps$2(Executor executor, final Consumer callback, Bundle result) {
        final HistoricalOps ops = (HistoricalOps) result.getParcelable(KEY_HISTORICAL_OPS);
        long identity = Binder.clearCallingIdentity();
        try {
            executor.execute(new Runnable() { // from class: android.app.AppOpsManager$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    callback.accept(ops);
                }
            });
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void getHistoricalOpsFromDiskRaw(HistoricalOpsRequest request, final Executor executor, final Consumer<HistoricalOps> callback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");
        try {
            this.mService.getHistoricalOpsFromDiskRaw(request.mUid, request.mPackageName, request.mAttributionTag, request.mOpNames, request.mHistoryFlags, request.mFilter, request.mBeginTimeMillis, request.mEndTimeMillis, request.mFlags, new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: android.app.AppOpsManager$$ExternalSyntheticLambda4
                @Override // android.os.RemoteCallback.OnResultListener
                public final void onResult(Bundle bundle) {
                    AppOpsManager.lambda$getHistoricalOpsFromDiskRaw$4(executor, callback, bundle);
                }
            }));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static /* synthetic */ void lambda$getHistoricalOpsFromDiskRaw$4(Executor executor, final Consumer callback, Bundle result) {
        final HistoricalOps ops = (HistoricalOps) result.getParcelable(KEY_HISTORICAL_OPS);
        long identity = Binder.clearCallingIdentity();
        try {
            executor.execute(new Runnable() { // from class: android.app.AppOpsManager$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    callback.accept(ops);
                }
            });
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void reloadNonHistoricalState() {
        try {
            this.mService.reloadNonHistoricalState();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setUidMode(int code, int uid, int mode) {
        try {
            this.mService.setUidMode(code, uid, mode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setUidMode(String appOp, int uid, int mode) {
        try {
            this.mService.setUidMode(strOpToOp(appOp), uid, mode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setUserRestriction(int code, boolean restricted, IBinder token) {
        setUserRestriction(code, restricted, token, null);
    }

    public void setUserRestriction(int code, boolean restricted, IBinder token, PackageTagsList excludedPackageTags) {
        setUserRestrictionForUser(code, restricted, token, excludedPackageTags, this.mContext.getUserId());
    }

    public void setUserRestrictionForUser(int code, boolean restricted, IBinder token, PackageTagsList excludedPackageTags, int userId) {
        try {
            this.mService.setUserRestriction(code, restricted, token, userId, excludedPackageTags);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMode(int code, int uid, String packageName, int mode) {
        try {
            this.mService.setMode(code, uid, packageName, mode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setMode(String op, int uid, String packageName, int mode) {
        try {
            this.mService.setMode(strOpToOp(op), uid, packageName, mode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setRestriction(int code, int usage, int mode, String[] exceptionPackages) {
        try {
            int uid = Binder.getCallingUid();
            this.mService.setAudioRestriction(code, usage, uid, mode, exceptionPackages);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void resetAllModes() {
        try {
            this.mService.resetAllModes(this.mContext.getUserId(), null);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static String permissionToOp(String permission) {
        CtaManager ctaManager = sCtaManager;
        if (ctaManager.isCtaSupported()) {
            return ctaManager.permissionToOp(permission);
        }
        Integer opCode = sPermToOp.get(permission);
        if (opCode == null) {
            return null;
        }
        return sOpToString[opCode.intValue()];
    }

    public static String resolvePackageName(int uid, String packageName) {
        if (uid == 0) {
            return "root";
        }
        if (uid == 2000) {
            return "com.android.shell";
        }
        if (uid == 1013) {
            return DeviceConfig.NAMESPACE_MEDIA;
        }
        if (uid == 1041) {
            return "audioserver";
        }
        if (uid == 1047) {
            return "cameraserver";
        }
        if (uid == 1000 && packageName == null) {
            return "android";
        }
        return packageName;
    }

    public void startWatchingMode(String op, String packageName, OnOpChangedListener callback) {
        startWatchingMode(strOpToOp(op), packageName, callback);
    }

    public void startWatchingMode(String op, String packageName, int flags, OnOpChangedListener callback) {
        startWatchingMode(strOpToOp(op), packageName, flags, callback);
    }

    public void startWatchingMode(int op, String packageName, OnOpChangedListener callback) {
        startWatchingMode(op, packageName, 0, callback);
    }

    public void startWatchingMode(int op, String packageName, int flags, final OnOpChangedListener callback) {
        synchronized (this.mModeWatchers) {
            IAppOpsCallback cb = this.mModeWatchers.get(callback);
            if (cb == null) {
                cb = new IAppOpsCallback.Stub() { // from class: android.app.AppOpsManager.2
                    {
                        AppOpsManager.this = this;
                    }

                    @Override // com.android.internal.app.IAppOpsCallback
                    public void opChanged(int op2, int uid, String packageName2) {
                        OnOpChangedListener onOpChangedListener = callback;
                        if (onOpChangedListener instanceof OnOpChangedInternalListener) {
                            ((OnOpChangedInternalListener) onOpChangedListener).onOpChanged(op2, packageName2);
                        }
                        if (AppOpsManager.sCtaManager.isCtaSupported()) {
                            if (AppOpsManager.getOpStrs()[op2] != null) {
                                callback.onOpChanged(AppOpsManager.getOpStrs()[op2], packageName2);
                            }
                        } else if (AppOpsManager.sOpToString[op2] != null) {
                            callback.onOpChanged(AppOpsManager.sOpToString[op2], packageName2);
                        }
                    }
                };
                this.mModeWatchers.put(callback, cb);
            }
            if (!Compatibility.isChangeEnabled((long) CALL_BACK_ON_CHANGED_LISTENER_WITH_SWITCHED_OP_CHANGE)) {
                flags |= 2;
            }
            try {
                this.mService.startWatchingModeWithFlags(op, packageName, flags, cb);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void stopWatchingMode(OnOpChangedListener callback) {
        synchronized (this.mModeWatchers) {
            IAppOpsCallback cb = this.mModeWatchers.remove(callback);
            if (cb != null) {
                try {
                    this.mService.stopWatchingMode(cb);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    @Deprecated
    public void startWatchingActive(int[] ops, OnOpActiveChangedListener callback) {
        String[] strOps = new String[ops.length];
        for (int i = 0; i < ops.length; i++) {
            strOps[i] = opToPublicName(ops[i]);
        }
        startWatchingActive(strOps, this.mContext.getMainExecutor(), callback);
    }

    public void startWatchingActive(String[] ops, Executor executor, OnOpActiveChangedListener callback) {
        Objects.requireNonNull(ops);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        synchronized (this.mActiveWatchers) {
            if (this.mActiveWatchers.get(callback) != null) {
                return;
            }
            IAppOpsActiveCallback cb = new AnonymousClass3(executor, callback);
            this.mActiveWatchers.put(callback, cb);
            int[] rawOps = new int[ops.length];
            for (int i = 0; i < ops.length; i++) {
                rawOps[i] = strOpToOp(ops[i]);
            }
            try {
                this.mService.startWatchingActive(rawOps, cb);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /* renamed from: android.app.AppOpsManager$3 */
    /* loaded from: classes.dex */
    public class AnonymousClass3 extends IAppOpsActiveCallback.Stub {
        final /* synthetic */ OnOpActiveChangedListener val$callback;
        final /* synthetic */ Executor val$executor;

        AnonymousClass3(Executor executor, OnOpActiveChangedListener onOpActiveChangedListener) {
            AppOpsManager.this = this$0;
            this.val$executor = executor;
            this.val$callback = onOpActiveChangedListener;
        }

        @Override // com.android.internal.app.IAppOpsActiveCallback
        public void opActiveChanged(final int op, final int uid, final String packageName, final String attributionTag, final boolean active, final int attributionFlags, final int attributionChainId) {
            Executor executor = this.val$executor;
            final OnOpActiveChangedListener onOpActiveChangedListener = this.val$callback;
            executor.execute(new Runnable() { // from class: android.app.AppOpsManager$3$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AppOpsManager.AnonymousClass3.lambda$opActiveChanged$0(AppOpsManager.OnOpActiveChangedListener.this, op, uid, packageName, active, attributionTag, attributionFlags, attributionChainId);
                }
            });
        }

        public static /* synthetic */ void lambda$opActiveChanged$0(OnOpActiveChangedListener callback, int op, int uid, String packageName, boolean active, String attributionTag, int attributionFlags, int attributionChainId) {
            if (callback instanceof OnOpActiveChangedInternalListener) {
                ((OnOpActiveChangedInternalListener) callback).onOpActiveChanged(op, uid, packageName, active);
            }
            if (AppOpsManager.sCtaManager.isCtaSupported()) {
                if (AppOpsManager.getOpStrs()[op] != null) {
                    callback.onOpActiveChanged(AppOpsManager.getOpStrs()[op], uid, packageName, attributionTag, active, attributionFlags, attributionChainId);
                }
            } else if (AppOpsManager.sOpToString[op] != null) {
                callback.onOpActiveChanged(AppOpsManager.sOpToString[op], uid, packageName, attributionTag, active, attributionFlags, attributionChainId);
            }
        }
    }

    public void stopWatchingActive(OnOpActiveChangedListener callback) {
        synchronized (this.mActiveWatchers) {
            IAppOpsActiveCallback cb = this.mActiveWatchers.remove(callback);
            if (cb != null) {
                try {
                    this.mService.stopWatchingActive(cb);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    public void startWatchingStarted(int[] ops, final OnOpStartedListener callback) {
        synchronized (this.mStartedWatchers) {
            if (this.mStartedWatchers.containsKey(callback)) {
                return;
            }
            IAppOpsStartedCallback cb = new IAppOpsStartedCallback.Stub() { // from class: android.app.AppOpsManager.4
                {
                    AppOpsManager.this = this;
                }

                @Override // com.android.internal.app.IAppOpsStartedCallback
                public void opStarted(int op, int uid, String packageName, String attributionTag, int flags, int mode, int startType, int attributionFlags, int attributionChainId) {
                    callback.onOpStarted(op, uid, packageName, attributionTag, flags, mode, startType, attributionFlags, attributionChainId);
                }
            };
            this.mStartedWatchers.put(callback, cb);
            try {
                this.mService.startWatchingStarted(ops, cb);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void stopWatchingStarted(OnOpStartedListener callback) {
        synchronized (this.mStartedWatchers) {
            IAppOpsStartedCallback cb = this.mStartedWatchers.remove(callback);
            if (cb != null) {
                try {
                    this.mService.stopWatchingStarted(cb);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    public void startWatchingNoted(int[] ops, final OnOpNotedListener callback) {
        synchronized (this.mNotedWatchers) {
            if (this.mNotedWatchers.get(callback) != null) {
                return;
            }
            IAppOpsNotedCallback cb = new IAppOpsNotedCallback.Stub() { // from class: android.app.AppOpsManager.5
                {
                    AppOpsManager.this = this;
                }

                @Override // com.android.internal.app.IAppOpsNotedCallback
                public void opNoted(int op, int uid, String packageName, String attributionTag, int flags, int mode) {
                    callback.onOpNoted(op, uid, packageName, attributionTag, flags, mode);
                }
            };
            this.mNotedWatchers.put(callback, cb);
            try {
                this.mService.startWatchingNoted(ops, cb);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void stopWatchingNoted(OnOpNotedListener callback) {
        synchronized (this.mNotedWatchers) {
            IAppOpsNotedCallback cb = this.mNotedWatchers.remove(callback);
            if (cb != null) {
                try {
                    this.mService.stopWatchingNoted(cb);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    private String buildSecurityExceptionMsg(int op, int uid, String packageName) {
        return packageName + " from uid " + uid + " not allowed to perform " + sOpNames[op];
    }

    public static int strOpToOp(String op) {
        CtaManager ctaManager = sCtaManager;
        if (ctaManager.isCtaSupported()) {
            return ctaManager.strOpToOp(op);
        }
        Integer val = sOpStrToOp.get(op);
        if (val == null) {
            throw new IllegalArgumentException("Unknown operation string: " + op);
        }
        return val.intValue();
    }

    public int unsafeCheckOp(String op, int uid, String packageName) {
        return checkOp(strOpToOp(op), uid, packageName);
    }

    @Deprecated
    public int checkOp(String op, int uid, String packageName) {
        return checkOp(strOpToOp(op), uid, packageName);
    }

    public int unsafeCheckOpNoThrow(String op, int uid, String packageName) {
        return checkOpNoThrow(strOpToOp(op), uid, packageName);
    }

    @Deprecated
    public int checkOpNoThrow(String op, int uid, String packageName) {
        return checkOpNoThrow(strOpToOp(op), uid, packageName);
    }

    public int unsafeCheckOpRaw(String op, int uid, String packageName) {
        return unsafeCheckOpRawNoThrow(op, uid, packageName);
    }

    public int unsafeCheckOpRawNoThrow(String op, int uid, String packageName) {
        return unsafeCheckOpRawNoThrow(strOpToOp(op), uid, packageName);
    }

    public int unsafeCheckOpRawNoThrow(int op, int uid, String packageName) {
        try {
            return this.mService.checkOperationRaw(op, uid, packageName, null);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public int noteOp(String op, int uid, String packageName) {
        return noteOp(op, uid, packageName, (String) null, (String) null);
    }

    @Deprecated
    public int noteOp(int op) {
        return noteOp(op, Process.myUid(), this.mContext.getOpPackageName(), (String) null, (String) null);
    }

    @Deprecated
    public int noteOp(int op, int uid, String packageName) {
        return noteOp(op, uid, packageName, (String) null, (String) null);
    }

    public int noteOp(String op, int uid, String packageName, String attributionTag, String message) {
        return noteOp(strOpToOp(op), uid, packageName, attributionTag, message);
    }

    public int noteOp(int op, int uid, String packageName, String attributionTag, String message) {
        int mode = noteOpNoThrow(op, uid, packageName, attributionTag, message);
        if (mode == 2) {
            throw new SecurityException(buildSecurityExceptionMsg(op, uid, packageName));
        }
        return mode;
    }

    @Deprecated
    public int noteOpNoThrow(String op, int uid, String packageName) {
        return noteOpNoThrow(op, uid, packageName, (String) null, (String) null);
    }

    @Deprecated
    public int noteOpNoThrow(int op, int uid, String packageName) {
        return noteOpNoThrow(op, uid, packageName, (String) null, (String) null);
    }

    public int noteOpNoThrow(String op, int uid, String packageName, String attributionTag, String message) {
        return noteOpNoThrow(strOpToOp(op), uid, packageName, attributionTag, message);
    }

    public int noteOpNoThrow(int op, int uid, String packageName, String attributionTag, String message) {
        String message2;
        boolean shouldCollectMessage;
        try {
            collectNoteOpCallsForValidation(op);
        } catch (RemoteException e) {
            e = e;
        }
        try {
            int collectionMode = getNotedOpCollectionMode(uid, packageName, op);
            boolean shouldCollectMessage2 = Process.myUid() == 1000;
            if (collectionMode == 3 && message == null) {
                shouldCollectMessage = true;
                message2 = getFormattedStackTrace();
            } else {
                message2 = message;
                shouldCollectMessage = shouldCollectMessage2;
            }
            try {
                SyncNotedAppOp syncOp = this.mService.noteOperation(op, uid, packageName, attributionTag, collectionMode == 3, message2, shouldCollectMessage);
                if (syncOp.getOpMode() == 0) {
                    if (collectionMode == 1) {
                        collectNotedOpForSelf(syncOp);
                    } else if (collectionMode == 2) {
                        collectNotedOpSync(syncOp);
                    }
                }
                return syncOp.getOpMode();
            } catch (RemoteException e2) {
                e = e2;
                throw e.rethrowFromSystemServer();
            }
        } catch (RemoteException e3) {
            e = e3;
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public int noteProxyOp(String op, String proxiedPackageName) {
        return noteProxyOp(op, proxiedPackageName, Binder.getCallingUid(), (String) null, (String) null);
    }

    @Deprecated
    public int noteProxyOp(int op, String proxiedPackageName) {
        return noteProxyOp(op, proxiedPackageName, Binder.getCallingUid(), (String) null, (String) null);
    }

    public int noteProxyOp(int op, String proxiedPackageName, int proxiedUid, String proxiedAttributionTag, String message) {
        return noteProxyOp(op, new AttributionSource(this.mContext.getAttributionSource(), new AttributionSource(proxiedUid, proxiedPackageName, proxiedAttributionTag, this.mContext.getAttributionSource().getToken())), message, false);
    }

    public int noteProxyOp(String op, String proxiedPackageName, int proxiedUid, String proxiedAttributionTag, String message) {
        return noteProxyOp(strOpToOp(op), proxiedPackageName, proxiedUid, proxiedAttributionTag, message);
    }

    public int noteProxyOp(int op, AttributionSource attributionSource, String message, boolean skipProxyOperation) {
        int mode = noteProxyOpNoThrow(op, attributionSource, message, skipProxyOperation);
        if (mode == 2) {
            throw new SecurityException("Proxy package " + attributionSource.getPackageName() + " from uid " + attributionSource.getUid() + " or calling package " + attributionSource.getNextPackageName() + " from uid " + attributionSource.getNextUid() + " not allowed to perform " + sOpNames[op]);
        }
        return mode;
    }

    @Deprecated
    public int noteProxyOpNoThrow(String op, String proxiedPackageName) {
        return noteProxyOpNoThrow(op, proxiedPackageName, Binder.getCallingUid(), null, null);
    }

    @Deprecated
    public int noteProxyOpNoThrow(String op, String proxiedPackageName, int proxiedUid) {
        return noteProxyOpNoThrow(op, proxiedPackageName, proxiedUid, null, null);
    }

    public int noteProxyOpNoThrow(String op, String proxiedPackageName, int proxiedUid, String proxiedAttributionTag, String message) {
        return noteProxyOpNoThrow(strOpToOp(op), new AttributionSource(this.mContext.getAttributionSource(), new AttributionSource(proxiedUid, proxiedPackageName, proxiedAttributionTag, this.mContext.getAttributionSource().getToken())), message, false);
    }

    public int noteProxyOpNoThrow(int op, AttributionSource attributionSource, String message, boolean skipProxyOperation) {
        int collectionMode;
        boolean shouldCollectMessage;
        String message2;
        int myUid = Process.myUid();
        try {
            collectNoteOpCallsForValidation(op);
            try {
                collectionMode = getNotedOpCollectionMode(attributionSource.getNextUid(), attributionSource.getNextAttributionTag(), op);
                boolean shouldCollectMessage2 = myUid == 1000;
                if (collectionMode == 3 && message == null) {
                    shouldCollectMessage = true;
                    message2 = getFormattedStackTrace();
                } else {
                    shouldCollectMessage = shouldCollectMessage2;
                    message2 = message;
                }
            } catch (RemoteException e) {
                e = e;
                throw e.rethrowFromSystemServer();
            }
        } catch (RemoteException e2) {
            e = e2;
        }
        try {
            SyncNotedAppOp syncOp = this.mService.noteProxyOperation(op, attributionSource, collectionMode == 3, message2, shouldCollectMessage, skipProxyOperation);
            if (syncOp.getOpMode() == 0) {
                if (collectionMode == 1) {
                    collectNotedOpForSelf(syncOp);
                } else if (collectionMode == 2 && (this.mContext.checkPermission(Manifest.permission.UPDATE_APP_OPS_STATS, -1, myUid) == 0 || Binder.getCallingUid() == attributionSource.getNextUid())) {
                    collectNotedOpSync(syncOp);
                }
            }
            return syncOp.getOpMode();
        } catch (RemoteException e3) {
            e = e3;
            throw e.rethrowFromSystemServer();
        }
    }

    private static String getComponentPackageNameFromString(String from) {
        ComponentName componentName = from != null ? ComponentName.unflattenFromString(from) : null;
        return componentName != null ? componentName.getPackageName() : "";
    }

    private static boolean isPackagePreInstalled(Context context, String packageName, int userId) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfoAsUser(packageName, 0, userId);
            return (info.flags & 1) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public int checkOp(int op, int uid, String packageName) {
        try {
            int mode = this.mService.checkOperation(op, uid, packageName);
            if (mode == 2) {
                throw new SecurityException(buildSecurityExceptionMsg(op, uid, packageName));
            }
            return mode;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int checkOpNoThrow(int op, int uid, String packageName) {
        try {
            int mode = this.mService.checkOperation(op, uid, packageName);
            if (mode == 4) {
                return 0;
            }
            return mode;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void checkPackage(int uid, String packageName) {
        try {
            if (this.mService.checkPackage(uid, packageName) != 0) {
                throw new SecurityException("Package " + packageName + " does not belong to " + uid);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int checkAudioOp(int op, int stream, int uid, String packageName) {
        try {
            int mode = this.mService.checkAudioOperation(op, stream, uid, packageName);
            if (mode == 2) {
                throw new SecurityException(buildSecurityExceptionMsg(op, uid, packageName));
            }
            return mode;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int checkAudioOpNoThrow(int op, int stream, int uid, String packageName) {
        try {
            return this.mService.checkAudioOperation(op, stream, uid, packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public static IBinder getToken(IAppOpsService service) {
        return getClientId();
    }

    public static IBinder getClientId() {
        IBinder iBinder;
        synchronized (AppOpsManager.class) {
            if (sClientId == null) {
                sClientId = new Binder();
            }
            iBinder = sClientId;
        }
        return iBinder;
    }

    private static IAppOpsService getService() {
        IAppOpsService iAppOpsService;
        synchronized (sLock) {
            if (sService == null) {
                sService = IAppOpsService.Stub.asInterface(ServiceManager.getService(Context.APP_OPS_SERVICE));
            }
            iAppOpsService = sService;
        }
        return iAppOpsService;
    }

    @Deprecated
    public int startOp(String op, int uid, String packageName) {
        return startOp(op, uid, packageName, null, null);
    }

    @Deprecated
    public int startOp(int op) {
        return startOp(op, Process.myUid(), this.mContext.getOpPackageName(), false, null, null);
    }

    @Deprecated
    public int startOp(int op, int uid, String packageName) {
        return startOp(op, uid, packageName, false, null, null);
    }

    @Deprecated
    public int startOp(int op, int uid, String packageName, boolean startIfModeDefault) {
        return startOp(op, uid, packageName, startIfModeDefault, null, null);
    }

    public int startOp(String op, int uid, String packageName, String attributionTag, String message) {
        return startOp(strOpToOp(op), uid, packageName, false, attributionTag, message);
    }

    public int startOp(int op, int uid, String packageName, boolean startIfModeDefault, String attributionTag, String message) {
        int mode = startOpNoThrow(op, uid, packageName, startIfModeDefault, attributionTag, message);
        if (mode == 2) {
            throw new SecurityException(buildSecurityExceptionMsg(op, uid, packageName));
        }
        return mode;
    }

    @Deprecated
    public int startOpNoThrow(String op, int uid, String packageName) {
        return startOpNoThrow(op, uid, packageName, null, null);
    }

    @Deprecated
    public int startOpNoThrow(int op, int uid, String packageName) {
        return startOpNoThrow(op, uid, packageName, false, null, null);
    }

    @Deprecated
    public int startOpNoThrow(int op, int uid, String packageName, boolean startIfModeDefault) {
        return startOpNoThrow(op, uid, packageName, startIfModeDefault, null, null);
    }

    public int startOpNoThrow(String op, int uid, String packageName, String attributionTag, String message) {
        return startOpNoThrow(strOpToOp(op), uid, packageName, false, attributionTag, message);
    }

    public int startOpNoThrow(int op, int uid, String packageName, boolean startIfModeDefault, String attributionTag, String message) {
        return startOpNoThrow(this.mContext.getAttributionSource().getToken(), op, uid, packageName, startIfModeDefault, attributionTag, message);
    }

    public int startOpNoThrow(IBinder token, int op, int uid, String packageName, boolean startIfModeDefault, String attributionTag, String message) {
        return startOpNoThrow(token, op, uid, packageName, startIfModeDefault, attributionTag, message, 0, -1);
    }

    public int startOpNoThrow(IBinder token, int op, int uid, String packageName, boolean startIfModeDefault, String attributionTag, String message, int attributionFlags, int attributionChainId) {
        int collectionMode;
        String message2;
        boolean shouldCollectMessage;
        try {
            collectNoteOpCallsForValidation(op);
            try {
                collectionMode = getNotedOpCollectionMode(uid, packageName, op);
                boolean shouldCollectMessage2 = Process.myUid() == 1000;
                if (collectionMode == 3 && message == null) {
                    shouldCollectMessage = true;
                    message2 = getFormattedStackTrace();
                } else {
                    message2 = message;
                    shouldCollectMessage = shouldCollectMessage2;
                }
            } catch (RemoteException e) {
                e = e;
                throw e.rethrowFromSystemServer();
            }
        } catch (RemoteException e2) {
            e = e2;
        }
        try {
            SyncNotedAppOp syncOp = this.mService.startOperation(token, op, uid, packageName, attributionTag, startIfModeDefault, collectionMode == 3, message2, shouldCollectMessage, attributionFlags, attributionChainId);
            if (syncOp.getOpMode() == 0) {
                if (collectionMode == 1) {
                    collectNotedOpForSelf(syncOp);
                } else if (collectionMode == 2) {
                    collectNotedOpSync(syncOp);
                }
            }
            return syncOp.getOpMode();
        } catch (RemoteException e3) {
            e = e3;
            throw e.rethrowFromSystemServer();
        }
    }

    public int startProxyOp(String op, int proxiedUid, String proxiedPackageName, String proxiedAttributionTag, String message) {
        return startProxyOp(op, new AttributionSource(this.mContext.getAttributionSource(), new AttributionSource(proxiedUid, proxiedPackageName, proxiedAttributionTag, this.mContext.getAttributionSource().getToken())), message, false);
    }

    public int startProxyOp(String op, AttributionSource attributionSource, String message, boolean skipProxyOperation) {
        int mode = startProxyOpNoThrow(strOpToOp(op), attributionSource, message, skipProxyOperation);
        if (mode == 2) {
            throw new SecurityException("Proxy package " + attributionSource.getPackageName() + " from uid " + attributionSource.getUid() + " or calling package " + attributionSource.getNextPackageName() + " from uid " + attributionSource.getNextUid() + " not allowed to perform " + op);
        }
        return mode;
    }

    public int startProxyOpNoThrow(String op, int proxiedUid, String proxiedPackageName, String proxiedAttributionTag, String message) {
        return startProxyOpNoThrow(strOpToOp(op), new AttributionSource(this.mContext.getAttributionSource(), new AttributionSource(proxiedUid, proxiedPackageName, proxiedAttributionTag, this.mContext.getAttributionSource().getToken())), message, false);
    }

    public int startProxyOpNoThrow(int op, AttributionSource attributionSource, String message, boolean skipProxyOperation) {
        return startProxyOpNoThrow(attributionSource.getToken(), op, attributionSource, message, skipProxyOperation, 0, 0, -1);
    }

    public int startProxyOpNoThrow(IBinder clientId, int op, AttributionSource attributionSource, String message, boolean skipProxyOperation, int proxyAttributionFlags, int proxiedAttributionFlags, int attributionChainId) {
        String message2;
        boolean shouldCollectMessage;
        try {
            collectNoteOpCallsForValidation(op);
            int collectionMode = getNotedOpCollectionMode(attributionSource.getNextUid(), attributionSource.getNextPackageName(), op);
            boolean shouldCollectMessage2 = Process.myUid() == 1000;
            if (collectionMode == 3 && message == null) {
                shouldCollectMessage = true;
                message2 = getFormattedStackTrace();
            } else {
                message2 = message;
                shouldCollectMessage = shouldCollectMessage2;
            }
            try {
                SyncNotedAppOp syncOp = this.mService.startProxyOperation(clientId, op, attributionSource, false, collectionMode == 3, message2, shouldCollectMessage, skipProxyOperation, proxyAttributionFlags, proxiedAttributionFlags, attributionChainId);
                if (syncOp.getOpMode() == 0) {
                    if (collectionMode == 1) {
                        collectNotedOpForSelf(syncOp);
                    } else if (collectionMode == 2 && (this.mContext.checkPermission(Manifest.permission.UPDATE_APP_OPS_STATS, -1, Process.myUid()) == 0 || Binder.getCallingUid() == attributionSource.getNextUid())) {
                        collectNotedOpSync(syncOp);
                    }
                }
                return syncOp.getOpMode();
            } catch (RemoteException e) {
                e = e;
                throw e.rethrowFromSystemServer();
            }
        } catch (RemoteException e2) {
            e = e2;
        }
    }

    @Deprecated
    public void finishOp(int op) {
        finishOp(op, Process.myUid(), this.mContext.getOpPackageName(), (String) null);
    }

    public void finishOp(String op, int uid, String packageName) {
        finishOp(strOpToOp(op), uid, packageName, (String) null);
    }

    public void finishOp(String op, int uid, String packageName, String attributionTag) {
        finishOp(strOpToOp(op), uid, packageName, attributionTag);
    }

    public void finishOp(int op, int uid, String packageName) {
        finishOp(op, uid, packageName, (String) null);
    }

    public void finishOp(int op, int uid, String packageName, String attributionTag) {
        finishOp(this.mContext.getAttributionSource().getToken(), op, uid, packageName, attributionTag);
    }

    public void finishOp(IBinder token, int op, int uid, String packageName, String attributionTag) {
        try {
            this.mService.finishOperation(token, op, uid, packageName, attributionTag);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void finishProxyOp(String op, int proxiedUid, String proxiedPackageName, String proxiedAttributionTag) {
        IBinder token = this.mContext.getAttributionSource().getToken();
        finishProxyOp(token, op, new AttributionSource(this.mContext.getAttributionSource(), new AttributionSource(proxiedUid, proxiedPackageName, proxiedAttributionTag, token)), false);
    }

    public void finishProxyOp(IBinder clientId, String op, AttributionSource attributionSource, boolean skipProxyOperation) {
        try {
            this.mService.finishProxyOperation(clientId, strOpToOp(op), attributionSource, skipProxyOperation);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isOpActive(String op, int uid, String packageName) {
        return isOperationActive(strOpToOp(op), uid, packageName);
    }

    public boolean isProxying(int op, String proxyAttributionTag, int proxiedUid, String proxiedPackageName) {
        try {
            return this.mService.isProxying(op, this.mContext.getOpPackageName(), this.mContext.getAttributionTag(), proxiedUid, proxiedPackageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void resetPackageOpsNoHistory(String packageName) {
        try {
            this.mService.resetPackageOpsNoHistory(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static void startNotedAppOpsCollection(int callingUid) {
        sBinderThreadCallingUid.set(Integer.valueOf(callingUid));
    }

    /* loaded from: classes.dex */
    public static class PausedNotedAppOpsCollection {
        final ArrayMap<String, long[]> mCollectedNotedAppOps;
        final int mUid;

        PausedNotedAppOpsCollection(int uid, ArrayMap<String, long[]> collectedNotedAppOps) {
            this.mUid = uid;
            this.mCollectedNotedAppOps = collectedNotedAppOps;
        }
    }

    public static PausedNotedAppOpsCollection pauseNotedAppOpsCollection() {
        ThreadLocal<Integer> threadLocal = sBinderThreadCallingUid;
        Integer previousUid = threadLocal.get();
        if (previousUid != null) {
            ThreadLocal<ArrayMap<String, long[]>> threadLocal2 = sAppOpsNotedInThisBinderTransaction;
            ArrayMap<String, long[]> previousCollectedNotedAppOps = threadLocal2.get();
            threadLocal.remove();
            threadLocal2.remove();
            return new PausedNotedAppOpsCollection(previousUid.intValue(), previousCollectedNotedAppOps);
        }
        return null;
    }

    public static void resumeNotedAppOpsCollection(PausedNotedAppOpsCollection prevCollection) {
        if (prevCollection != null) {
            sBinderThreadCallingUid.set(Integer.valueOf(prevCollection.mUid));
            if (prevCollection.mCollectedNotedAppOps != null) {
                sAppOpsNotedInThisBinderTransaction.set(prevCollection.mCollectedNotedAppOps);
            }
        }
    }

    public static void finishNotedAppOpsCollection() {
        sBinderThreadCallingUid.remove();
        sAppOpsNotedInThisBinderTransaction.remove();
    }

    private void collectNotedOpForSelf(SyncNotedAppOp syncOp) {
        synchronized (sLock) {
            OnOpNotedCallback onOpNotedCallback = sOnOpNotedCallback;
            if (onOpNotedCallback != null) {
                onOpNotedCallback.onSelfNoted(syncOp);
            }
        }
        sMessageCollector.onSelfNoted(syncOp);
    }

    public static void collectNotedOpSync(SyncNotedAppOp syncOp) {
        int op = strOpToOp(syncOp.getOp());
        ThreadLocal<ArrayMap<String, long[]>> threadLocal = sAppOpsNotedInThisBinderTransaction;
        ArrayMap<String, long[]> appOpsNoted = threadLocal.get();
        if (appOpsNoted == null) {
            appOpsNoted = new ArrayMap<>(1);
            threadLocal.set(appOpsNoted);
        }
        long[] appOpsNotedForAttribution = appOpsNoted.get(syncOp.getAttributionTag());
        if (appOpsNotedForAttribution == null) {
            appOpsNotedForAttribution = new long[2];
            appOpsNoted.put(syncOp.getAttributionTag(), appOpsNotedForAttribution);
        }
        if (op < 64) {
            appOpsNotedForAttribution[0] = (1 << op) | appOpsNotedForAttribution[0];
            return;
        }
        appOpsNotedForAttribution[1] = (1 << (op - 64)) | appOpsNotedForAttribution[1];
    }

    private int getNotedOpCollectionMode(int uid, String packageName, int op) {
        if (packageName == null) {
            packageName = "android";
        }
        byte[] bArr = sAppOpsToNote;
        if (bArr[op] == 0) {
            try {
                boolean shouldCollectNotes = this.mService.shouldCollectNotes(op);
                if (shouldCollectNotes) {
                    bArr[op] = 2;
                } else {
                    bArr[op] = 1;
                }
            } catch (RemoteException e) {
                return 0;
            }
        }
        if (bArr[op] != 2) {
            return 0;
        }
        synchronized (sLock) {
            if (uid == Process.myUid() && packageName.equals(ActivityThread.currentOpPackageName())) {
                return 1;
            }
            Integer binderUid = sBinderThreadCallingUid.get();
            return (binderUid == null || binderUid.intValue() != uid) ? 3 : 2;
        }
    }

    public static void prefixParcelWithAppOpsIfNeeded(Parcel p) {
        ArrayMap<String, long[]> notedAppOps = sAppOpsNotedInThisBinderTransaction.get();
        if (notedAppOps == null) {
            return;
        }
        p.writeInt(-127);
        int numAttributionWithNotesAppOps = notedAppOps.size();
        p.writeInt(numAttributionWithNotesAppOps);
        for (int i = 0; i < numAttributionWithNotesAppOps; i++) {
            p.writeString(notedAppOps.keyAt(i));
            p.writeLong(notedAppOps.valueAt(i)[0]);
            p.writeLong(notedAppOps.valueAt(i)[1]);
        }
    }

    public static void readAndLogNotedAppops(Parcel p) {
        int numAttributionsWithNotedAppOps = p.readInt();
        for (int i = 0; i < numAttributionsWithNotedAppOps; i++) {
            String attributionTag = p.readString();
            long[] rawNotedAppOps = {p.readLong(), p.readLong()};
            if (rawNotedAppOps[0] != 0 || rawNotedAppOps[1] != 0) {
                BitSet notedAppOps = BitSet.valueOf(rawNotedAppOps);
                synchronized (sLock) {
                    for (int code = notedAppOps.nextSetBit(0); code != -1; code = notedAppOps.nextSetBit(code + 1)) {
                        OnOpNotedCallback onOpNotedCallback = sOnOpNotedCallback;
                        if (onOpNotedCallback != null) {
                            onOpNotedCallback.onNoted(new SyncNotedAppOp(code, attributionTag));
                        } else {
                            String message = getFormattedStackTrace();
                            sUnforwardedOps.add(new AsyncNotedAppOp(code, Process.myUid(), attributionTag, message, System.currentTimeMillis()));
                            if (sUnforwardedOps.size() > 10) {
                                sUnforwardedOps.remove(0);
                            }
                        }
                    }
                }
                for (int code2 = notedAppOps.nextSetBit(0); code2 != -1; code2 = notedAppOps.nextSetBit(code2 + 1)) {
                    sMessageCollector.onNoted(new SyncNotedAppOp(code2, attributionTag));
                }
            }
        }
    }

    public void setOnOpNotedCallback(Executor asyncExecutor, OnOpNotedCallback callback) {
        Preconditions.checkState((callback == null) == (asyncExecutor == null));
        synchronized (sLock) {
            if (callback == null) {
                Preconditions.checkState(sOnOpNotedCallback != null, "No callback is currently registered");
                try {
                    this.mService.stopWatchingAsyncNoted(this.mContext.getPackageName(), sOnOpNotedCallback.mAsyncCb);
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
                sOnOpNotedCallback = null;
            } else {
                Preconditions.checkState(sOnOpNotedCallback == null, "Another callback is already registered");
                callback.mAsyncExecutor = asyncExecutor;
                sOnOpNotedCallback = callback;
                List<AsyncNotedAppOp> missedAsyncOps = null;
                try {
                    this.mService.startWatchingAsyncNoted(this.mContext.getPackageName(), sOnOpNotedCallback.mAsyncCb);
                    missedAsyncOps = this.mService.extractAsyncOps(this.mContext.getPackageName());
                } catch (RemoteException e2) {
                    e2.rethrowFromSystemServer();
                }
                final OnOpNotedCallback onOpNotedCallback = sOnOpNotedCallback;
                if (onOpNotedCallback != null && missedAsyncOps != null) {
                    int numMissedAsyncOps = missedAsyncOps.size();
                    for (int i = 0; i < numMissedAsyncOps; i++) {
                        final AsyncNotedAppOp asyncNotedAppOp = missedAsyncOps.get(i);
                        onOpNotedCallback.getAsyncNotedExecutor().execute(new Runnable() { // from class: android.app.AppOpsManager$$ExternalSyntheticLambda2
                            @Override // java.lang.Runnable
                            public final void run() {
                                AppOpsManager.OnOpNotedCallback.this.onAsyncNoted(asyncNotedAppOp);
                            }
                        });
                    }
                }
                synchronized (this) {
                    int numMissedSyncOps = sUnforwardedOps.size();
                    if (onOpNotedCallback != null) {
                        for (int i2 = 0; i2 < numMissedSyncOps; i2++) {
                            final AsyncNotedAppOp syncNotedAppOp = sUnforwardedOps.get(i2);
                            onOpNotedCallback.getAsyncNotedExecutor().execute(new Runnable() { // from class: android.app.AppOpsManager$$ExternalSyntheticLambda3
                                @Override // java.lang.Runnable
                                public final void run() {
                                    AppOpsManager.OnOpNotedCallback.this.onAsyncNoted(syncNotedAppOp);
                                }
                            });
                        }
                    }
                    sUnforwardedOps.clear();
                }
            }
        }
    }

    @SystemApi
    @Deprecated
    public void setNotedAppOpsCollector(AppOpsCollector collector) {
        synchronized (sLock) {
            if (collector != null) {
                if (isListeningForOpNoted()) {
                    setOnOpNotedCallback(null, null);
                }
                setOnOpNotedCallback(new HandlerExecutor(Handler.getMain()), collector);
            } else if (sOnOpNotedCallback != null) {
                setOnOpNotedCallback(null, null);
            }
        }
    }

    public static boolean isListeningForOpNoted() {
        return sOnOpNotedCallback != null || isCollectingStackTraces();
    }

    private static boolean isCollectingStackTraces() {
        if (sConfig.getSampledOpCode() == -1 && sConfig.getAcceptableLeftDistance() == 0 && sConfig.getExpirationTimeSinceBootMillis() >= SystemClock.elapsedRealtime()) {
            return false;
        }
        return true;
    }

    /* loaded from: classes.dex */
    public static abstract class OnOpNotedCallback {
        private final IAppOpsAsyncNotedCallback mAsyncCb = new AnonymousClass1();
        private Executor mAsyncExecutor;

        public abstract void onAsyncNoted(AsyncNotedAppOp asyncNotedAppOp);

        public abstract void onNoted(SyncNotedAppOp syncNotedAppOp);

        public abstract void onSelfNoted(SyncNotedAppOp syncNotedAppOp);

        /* renamed from: android.app.AppOpsManager$OnOpNotedCallback$1 */
        /* loaded from: classes.dex */
        public class AnonymousClass1 extends IAppOpsAsyncNotedCallback.Stub {
            AnonymousClass1() {
                OnOpNotedCallback.this = this$0;
            }

            @Override // com.android.internal.app.IAppOpsAsyncNotedCallback
            public void opNoted(final AsyncNotedAppOp op) {
                Objects.requireNonNull(op);
                long token = Binder.clearCallingIdentity();
                try {
                    OnOpNotedCallback.this.getAsyncNotedExecutor().execute(new Runnable() { // from class: android.app.AppOpsManager$OnOpNotedCallback$1$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            AppOpsManager.OnOpNotedCallback.AnonymousClass1.this.m262lambda$opNoted$0$androidappAppOpsManager$OnOpNotedCallback$1(op);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            /* renamed from: lambda$opNoted$0$android-app-AppOpsManager$OnOpNotedCallback$1 */
            public /* synthetic */ void m262lambda$opNoted$0$androidappAppOpsManager$OnOpNotedCallback$1(AsyncNotedAppOp op) {
                OnOpNotedCallback.this.onAsyncNoted(op);
            }
        }

        protected Executor getAsyncNotedExecutor() {
            return this.mAsyncExecutor;
        }
    }

    @SystemApi
    @Deprecated
    /* loaded from: classes.dex */
    public static abstract class AppOpsCollector extends OnOpNotedCallback {
        @Override // android.app.AppOpsManager.OnOpNotedCallback
        public Executor getAsyncNotedExecutor() {
            return new HandlerExecutor(Handler.getMain());
        }
    }

    private static String getFormattedStackTrace() {
        StackTraceElement[] trace = new Exception().getStackTrace();
        int firstInteresting = 0;
        for (int i = 0; i < trace.length && (trace[i].getClassName().startsWith(AppOpsManager.class.getName()) || trace[i].getClassName().startsWith(Parcel.class.getName()) || trace[i].getClassName().contains("$Stub$Proxy") || trace[i].getClassName().startsWith(DatabaseUtils.class.getName()) || trace[i].getClassName().startsWith("android.content.ContentProviderProxy") || trace[i].getClassName().startsWith(ContentResolver.class.getName())); i++) {
            firstInteresting = i;
        }
        int i2 = trace.length;
        int lastInteresting = i2 - 1;
        for (int i3 = trace.length - 1; i3 >= 0 && (trace[i3].getClassName().startsWith(HandlerThread.class.getName()) || trace[i3].getClassName().startsWith(Handler.class.getName()) || trace[i3].getClassName().startsWith(Looper.class.getName()) || trace[i3].getClassName().startsWith(Binder.class.getName()) || trace[i3].getClassName().startsWith(RuntimeInit.class.getName()) || trace[i3].getClassName().startsWith(ZygoteInit.class.getName()) || trace[i3].getClassName().startsWith(ActivityThread.class.getName()) || trace[i3].getClassName().startsWith(Method.class.getName()) || trace[i3].getClassName().startsWith("com.android.server.SystemServer")); i3--) {
            lastInteresting = i3;
        }
        StringBuilder sb = new StringBuilder();
        for (int i4 = firstInteresting; i4 <= lastInteresting; i4++) {
            if (sFullLog == null) {
                try {
                    sFullLog = Boolean.valueOf(DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY, FULL_LOG, false));
                } catch (Exception e) {
                    sFullLog = false;
                }
            }
            if (i4 != firstInteresting) {
                sb.append('\n');
            }
            if (!sFullLog.booleanValue() && sb.length() + trace[i4].toString().length() > 600) {
                break;
            }
            sb.append(trace[i4]);
        }
        return sb.toString();
    }

    public boolean isOperationActive(int code, int uid, String packageName) {
        try {
            return this.mService.isOperationActive(code, uid, packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setHistoryParameters(int mode, long baseSnapshotInterval, int compressionStep) {
        try {
            this.mService.setHistoryParameters(mode, baseSnapshotInterval, compressionStep);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void offsetHistory(long offsetMillis) {
        try {
            this.mService.offsetHistory(offsetMillis);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addHistoricalOps(HistoricalOps ops) {
        try {
            this.mService.addHistoricalOps(ops);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void resetHistoryParameters() {
        try {
            this.mService.resetHistoryParameters();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearHistory() {
        try {
            this.mService.clearHistory();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void rebootHistory(long offlineDurationMillis) {
        try {
            this.mService.rebootHistory(offlineDurationMillis);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public RuntimeAppOpAccessMessage collectRuntimeAppOpAccessMessage() {
        try {
            return this.mService.collectRuntimeAppOpAccessMessage();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public static String[] getOpStrs() {
        CtaManager ctaManager = sCtaManager;
        if (ctaManager.isCtaSupported()) {
            return ctaManager.getOpStrs();
        }
        String[] strArr = sOpToString;
        return (String[]) Arrays.copyOf(strArr, strArr.length);
    }

    public static int getNumOps() {
        if (sCtaManager.isCtaSupported()) {
            return 125;
        }
        return 121;
    }

    public static NoteOpEvent getLastEvent(LongSparseArray<NoteOpEvent> events, int beginUidState, int endUidState, int flags) {
        int[] iArr;
        if (events == null) {
            return null;
        }
        NoteOpEvent lastEvent = null;
        int flags2 = flags;
        while (flags2 != 0) {
            int flag = 1 << Integer.numberOfTrailingZeros(flags2);
            flags2 &= ~flag;
            for (int uidState : UID_STATES) {
                if (uidState >= beginUidState && uidState <= endUidState) {
                    long key = makeKey(uidState, flag);
                    NoteOpEvent event = events.get(key);
                    if (lastEvent == null || (event != null && event.getNoteTime() > lastEvent.getNoteTime())) {
                        lastEvent = event;
                    }
                }
            }
        }
        return lastEvent;
    }

    public static boolean equalsLongSparseLongArray(LongSparseLongArray a, LongSparseLongArray b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        int numEntries = a.size();
        for (int i = 0; i < numEntries; i++) {
            if (a.keyAt(i) != b.keyAt(i) || a.valueAt(i) != b.valueAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static void writeLongSparseLongArrayToParcel(LongSparseLongArray array, Parcel parcel) {
        if (array != null) {
            int size = array.size();
            parcel.writeInt(size);
            for (int i = 0; i < size; i++) {
                parcel.writeLong(array.keyAt(i));
                parcel.writeLong(array.valueAt(i));
            }
            return;
        }
        parcel.writeInt(-1);
    }

    public static LongSparseLongArray readLongSparseLongArrayFromParcel(Parcel parcel) {
        int size = parcel.readInt();
        if (size < 0) {
            return null;
        }
        LongSparseLongArray array = new LongSparseLongArray(size);
        for (int i = 0; i < size; i++) {
            array.append(parcel.readLong(), parcel.readLong());
        }
        return array;
    }

    public static void writeDiscreteAccessArrayToParcel(List<AttributedOpEntry> array, Parcel parcel, int flags) {
        ParceledListSlice<AttributedOpEntry> listSlice = array == null ? null : new ParceledListSlice<>(array);
        parcel.writeParcelable(listSlice, flags);
    }

    public static List<AttributedOpEntry> readDiscreteAccessArrayFromParcel(Parcel parcel) {
        ParceledListSlice<AttributedOpEntry> listSlice = (ParceledListSlice) parcel.readParcelable(null, ParceledListSlice.class);
        if (listSlice == null) {
            return null;
        }
        return listSlice.getList();
    }

    public static LongSparseArray<Object> collectKeys(LongSparseLongArray array, LongSparseArray<Object> result) {
        if (array != null) {
            if (result == null) {
                result = new LongSparseArray<>();
            }
            int accessSize = array.size();
            for (int i = 0; i < accessSize; i++) {
                result.put(array.keyAt(i), null);
            }
        }
        return result;
    }

    public static String uidStateToString(int uidState) {
        switch (uidState) {
            case 100:
                return "UID_STATE_PERSISTENT";
            case 200:
                return "UID_STATE_TOP";
            case 300:
                return "UID_STATE_FOREGROUND_SERVICE_LOCATION";
            case 400:
                return "UID_STATE_FOREGROUND_SERVICE";
            case 500:
                return "UID_STATE_FOREGROUND";
            case 600:
                return "UID_STATE_BACKGROUND";
            case 700:
                return "UID_STATE_CACHED";
            default:
                return "UNKNOWN";
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static int parseHistoricalMode(String mode) {
        char c;
        switch (mode.hashCode()) {
            case 155185419:
                if (mode.equals("HISTORICAL_MODE_ENABLED_ACTIVE")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 885538210:
                if (mode.equals("HISTORICAL_MODE_ENABLED_PASSIVE")) {
                    c = 1;
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
            default:
                return 0;
        }
    }

    public static String historicalModeToString(int mode) {
        switch (mode) {
            case 0:
                return "HISTORICAL_MODE_DISABLED";
            case 1:
                return "HISTORICAL_MODE_ENABLED_ACTIVE";
            case 2:
                return "HISTORICAL_MODE_ENABLED_PASSIVE";
            default:
                return "UNKNOWN";
        }
    }

    private static int getSystemAlertWindowDefault() {
        Context context = ActivityThread.currentApplication();
        if (context == null) {
            return 3;
        }
        PackageManager pm = context.getPackageManager();
        if (!ActivityManager.isLowRamDeviceStatic() || pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK, 0)) {
            return 3;
        }
        return 1;
    }

    public static int leftCircularDistance(int from, int to, int size) {
        return ((to + size) - from) % size;
    }

    private void collectNoteOpCallsForValidation(int op) {
    }

    public static List<AttributedOpEntry> deduplicateDiscreteEvents(List<AttributedOpEntry> list) {
        int n = list.size();
        int i = 0;
        int j = 0;
        while (j < n) {
            long currentAccessTime = list.get(j).getLastAccessTime(31);
            int k = j + 1;
            while (k < n && list.get(k).getLastAccessTime(31) == currentAccessTime) {
                k++;
            }
            list.set(i, mergeAttributedOpEntries(list.subList(j, k)));
            i++;
            j = k;
        }
        while (i < n) {
            list.remove(list.size() - 1);
            i++;
        }
        return list;
    }

    public static AttributedOpEntry mergeAttributedOpEntries(List<AttributedOpEntry> opEntries) {
        int opCount;
        if (opEntries.size() == 1) {
            return opEntries.get(0);
        }
        LongSparseArray<NoteOpEvent> accessEvents = new LongSparseArray<>();
        LongSparseArray<NoteOpEvent> rejectEvents = new LongSparseArray<>();
        int opCount2 = opEntries.size();
        for (int i = 0; i < opCount2; i++) {
            AttributedOpEntry a = opEntries.get(i);
            ArraySet<Long> keys = a.collectKeys();
            int keyCount = keys.size();
            int k = 0;
            while (k < keyCount) {
                long key = keys.valueAt(k).longValue();
                int uidState = extractUidStateFromKey(key);
                int flags = extractFlagsFromKey(key);
                NoteOpEvent access = a.getLastAccessEvent(uidState, uidState, flags);
                NoteOpEvent reject = a.getLastRejectEvent(uidState, uidState, flags);
                if (access == null) {
                    opCount = opCount2;
                } else {
                    NoteOpEvent existingAccess = accessEvents.get(key);
                    if (existingAccess == null) {
                        opCount = opCount2;
                    } else if (existingAccess.getDuration() == -1) {
                        opCount = opCount2;
                    } else if (existingAccess.mProxy != null || access.mProxy == null) {
                        opCount = opCount2;
                    } else {
                        opCount = opCount2;
                        existingAccess.mProxy = access.mProxy;
                    }
                    accessEvents.append(key, access);
                }
                if (reject != null) {
                    rejectEvents.append(key, reject);
                }
                k++;
                opCount2 = opCount;
            }
        }
        return new AttributedOpEntry(opEntries.get(0).mOp, false, accessEvents, rejectEvents);
    }
}
