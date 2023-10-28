package com.android.server.location.gnss.hal;

import android.location.GnssAntennaInfo;
import android.location.GnssCapabilities;
import android.location.GnssMeasurementCorrections;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.os.Binder;
import android.os.SystemClock;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.location.gnss.GnssConfiguration;
import com.android.server.location.gnss.GnssManagerService;
import com.android.server.location.gnss.GnssPowerStats;
import com.android.server.location.injector.EmergencyHelper;
import com.android.server.location.injector.Injector;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public class GnssNative {
    public static final int AGPS_REF_LOCATION_TYPE_GSM_CELLID = 1;
    public static final int AGPS_REF_LOCATION_TYPE_LTE_CELLID = 4;
    public static final int AGPS_REF_LOCATION_TYPE_NR_CELLID = 8;
    public static final int AGPS_REF_LOCATION_TYPE_UMTS_CELLID = 2;
    public static final int AGPS_SETID_TYPE_IMSI = 1;
    public static final int AGPS_SETID_TYPE_MSISDN = 2;
    public static final int AGPS_SETID_TYPE_NONE = 0;
    public static final int GNSS_AIDING_TYPE_ALL = 65535;
    public static final int GNSS_AIDING_TYPE_ALMANAC = 2;
    public static final int GNSS_AIDING_TYPE_CELLDB_INFO = 32768;
    public static final int GNSS_AIDING_TYPE_EPHEMERIS = 1;
    public static final int GNSS_AIDING_TYPE_HEALTH = 64;
    public static final int GNSS_AIDING_TYPE_IONO = 16;
    public static final int GNSS_AIDING_TYPE_POSITION = 4;
    public static final int GNSS_AIDING_TYPE_RTI = 1024;
    public static final int GNSS_AIDING_TYPE_SADATA = 512;
    public static final int GNSS_AIDING_TYPE_SVDIR = 128;
    public static final int GNSS_AIDING_TYPE_SVSTEER = 256;
    public static final int GNSS_AIDING_TYPE_TIME = 8;
    public static final int GNSS_AIDING_TYPE_UTC = 32;
    public static final int GNSS_LOCATION_HAS_ALTITUDE = 2;
    public static final int GNSS_LOCATION_HAS_BEARING = 8;
    public static final int GNSS_LOCATION_HAS_BEARING_ACCURACY = 128;
    public static final int GNSS_LOCATION_HAS_HORIZONTAL_ACCURACY = 16;
    public static final int GNSS_LOCATION_HAS_LAT_LONG = 1;
    public static final int GNSS_LOCATION_HAS_SPEED = 4;
    public static final int GNSS_LOCATION_HAS_SPEED_ACCURACY = 64;
    public static final int GNSS_LOCATION_HAS_VERTICAL_ACCURACY = 32;
    public static final int GNSS_POSITION_MODE_MS_ASSISTED = 2;
    public static final int GNSS_POSITION_MODE_MS_BASED = 1;
    public static final int GNSS_POSITION_MODE_STANDALONE = 0;
    public static final int GNSS_POSITION_RECURRENCE_PERIODIC = 0;
    public static final int GNSS_POSITION_RECURRENCE_SINGLE = 1;
    public static final int GNSS_REALTIME_HAS_TIMESTAMP_NS = 1;
    public static final int GNSS_REALTIME_HAS_TIME_UNCERTAINTY_NS = 2;
    private static final float ITAR_SPEED_LIMIT_METERS_PER_SECOND = 400.0f;
    private static GnssHal sGnssHal;
    private static boolean sGnssHalInitialized;
    private static GnssNative sInstance;
    private AGpsCallbacks mAGpsCallbacks;
    private final GnssConfiguration mConfiguration;
    private final EmergencyHelper mEmergencyHelper;
    private GeofenceCallbacks mGeofenceCallbacks;
    private final GnssHal mGnssHal;
    private volatile boolean mItarSpeedLimitExceeded;
    private LocationRequestCallbacks mLocationRequestCallbacks;
    private NotificationCallbacks mNotificationCallbacks;
    private PsdsCallbacks mPsdsCallbacks;
    private boolean mRegistered;
    private TimeCallbacks mTimeCallbacks;
    private int mTopFlags;
    private BaseCallbacks[] mBaseCallbacks = new BaseCallbacks[0];
    private StatusCallbacks[] mStatusCallbacks = new StatusCallbacks[0];
    private SvStatusCallbacks[] mSvStatusCallbacks = new SvStatusCallbacks[0];
    private NmeaCallbacks[] mNmeaCallbacks = new NmeaCallbacks[0];
    private LocationCallbacks[] mLocationCallbacks = new LocationCallbacks[0];
    private MeasurementCallbacks[] mMeasurementCallbacks = new MeasurementCallbacks[0];
    private AntennaInfoCallbacks[] mAntennaInfoCallbacks = new AntennaInfoCallbacks[0];
    private NavigationMessageCallbacks[] mNavigationMessageCallbacks = new NavigationMessageCallbacks[0];
    private GnssCapabilities mCapabilities = new GnssCapabilities.Builder().build();
    private GnssPowerStats mPowerStats = null;
    private int mHardwareYear = 0;
    private String mHardwareModelName = null;
    private long mStartRealtimeMs = 0;
    private boolean mHasFirstFix = false;

    /* loaded from: classes.dex */
    public interface AGpsCallbacks {
        public static final int AGPS_REQUEST_SETID_IMSI = 1;
        public static final int AGPS_REQUEST_SETID_MSISDN = 2;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        public @interface AgpsSetIdFlags {
        }

        void onReportAGpsStatus(int i, int i2, byte[] bArr);

        void onRequestSetID(int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface AgpsReferenceLocationType {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface AgpsSetIdType {
    }

    /* loaded from: classes.dex */
    public interface AntennaInfoCallbacks {
        void onReportAntennaInfo(List<GnssAntennaInfo> list);
    }

    /* loaded from: classes.dex */
    public interface GeofenceCallbacks {
        public static final int GEOFENCE_AVAILABILITY_AVAILABLE = 2;
        public static final int GEOFENCE_AVAILABILITY_UNAVAILABLE = 1;
        public static final int GEOFENCE_STATUS_ERROR_GENERIC = -149;
        public static final int GEOFENCE_STATUS_ERROR_ID_EXISTS = -101;
        public static final int GEOFENCE_STATUS_ERROR_ID_UNKNOWN = -102;
        public static final int GEOFENCE_STATUS_ERROR_INVALID_TRANSITION = -103;
        public static final int GEOFENCE_STATUS_ERROR_TOO_MANY_GEOFENCES = 100;
        public static final int GEOFENCE_STATUS_OPERATION_SUCCESS = 0;
        public static final int GEOFENCE_TRANSITION_ENTERED = 1;
        public static final int GEOFENCE_TRANSITION_EXITED = 2;
        public static final int GEOFENCE_TRANSITION_UNCERTAIN = 4;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        public @interface GeofenceAvailability {
        }

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        public @interface GeofenceStatus {
        }

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        public @interface GeofenceTransition {
        }

        void onReportGeofenceAddStatus(int i, int i2);

        void onReportGeofencePauseStatus(int i, int i2);

        void onReportGeofenceRemoveStatus(int i, int i2);

        void onReportGeofenceResumeStatus(int i, int i2);

        void onReportGeofenceStatus(int i, Location location);

        void onReportGeofenceTransition(int i, Location location, int i2, long j);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface GnssAidingTypeFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface GnssLocationFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface GnssPositionMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface GnssPositionRecurrence {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface GnssRealtimeFlags {
    }

    /* loaded from: classes.dex */
    public interface LocationCallbacks {
        void onReportLocation(boolean z, Location location);

        void onReportLocations(Location[] locationArr);
    }

    /* loaded from: classes.dex */
    public interface LocationRequestCallbacks {
        void onRequestLocation(boolean z, boolean z2);

        void onRequestRefLocation();
    }

    /* loaded from: classes.dex */
    public interface MeasurementCallbacks {
        void onReportMeasurements(GnssMeasurementsEvent gnssMeasurementsEvent);
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    private @interface NativeEntryPoint {
    }

    /* loaded from: classes.dex */
    public interface NavigationMessageCallbacks {
        void onReportNavigationMessage(GnssNavigationMessage gnssNavigationMessage);
    }

    /* loaded from: classes.dex */
    public interface NmeaCallbacks {
        void onReportNmea(long j);
    }

    /* loaded from: classes.dex */
    public interface NotificationCallbacks {
        void onReportNfwNotification(String str, byte b, String str2, byte b2, String str3, byte b3, boolean z, boolean z2);

        void onReportNiNotification(int i, int i2, int i3, int i4, int i5, String str, String str2, int i6, int i7);
    }

    /* loaded from: classes.dex */
    public interface PsdsCallbacks {
        void onRequestPsdsDownload(int i);
    }

    /* loaded from: classes.dex */
    public interface StatusCallbacks {
        public static final int GNSS_STATUS_ENGINE_OFF = 4;
        public static final int GNSS_STATUS_ENGINE_ON = 3;
        public static final int GNSS_STATUS_NONE = 0;
        public static final int GNSS_STATUS_SESSION_BEGIN = 1;
        public static final int GNSS_STATUS_SESSION_END = 2;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        public @interface GnssStatusValue {
        }

        void onReportFirstFix(int i);

        void onReportStatus(int i);
    }

    /* loaded from: classes.dex */
    public interface SvStatusCallbacks {
        void onReportSvStatus(GnssStatus gnssStatus);
    }

    /* loaded from: classes.dex */
    public interface TimeCallbacks {
        void onRequestUtcTime();
    }

    /* renamed from: -$$Nest$smnative_get_batch_size  reason: not valid java name */
    static /* bridge */ /* synthetic */ int m4442$$Nest$smnative_get_batch_size() {
        return native_get_batch_size();
    }

    /* renamed from: -$$Nest$smnative_get_internal_state  reason: not valid java name */
    static /* bridge */ /* synthetic */ String m4443$$Nest$smnative_get_internal_state() {
        return native_get_internal_state();
    }

    /* renamed from: -$$Nest$smnative_init  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4444$$Nest$smnative_init() {
        return native_init();
    }

    /* renamed from: -$$Nest$smnative_init_batching  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4445$$Nest$smnative_init_batching() {
        return native_init_batching();
    }

    /* renamed from: -$$Nest$smnative_is_antenna_info_supported  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4451$$Nest$smnative_is_antenna_info_supported() {
        return native_is_antenna_info_supported();
    }

    /* renamed from: -$$Nest$smnative_is_geofence_supported  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4452$$Nest$smnative_is_geofence_supported() {
        return native_is_geofence_supported();
    }

    /* renamed from: -$$Nest$smnative_is_gnss_visibility_control_supported  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4453$$Nest$smnative_is_gnss_visibility_control_supported() {
        return native_is_gnss_visibility_control_supported();
    }

    /* renamed from: -$$Nest$smnative_is_measurement_corrections_supported  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4454$$Nest$smnative_is_measurement_corrections_supported() {
        return native_is_measurement_corrections_supported();
    }

    /* renamed from: -$$Nest$smnative_is_measurement_supported  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4455$$Nest$smnative_is_measurement_supported() {
        return native_is_measurement_supported();
    }

    /* renamed from: -$$Nest$smnative_is_navigation_message_supported  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4456$$Nest$smnative_is_navigation_message_supported() {
        return native_is_navigation_message_supported();
    }

    /* renamed from: -$$Nest$smnative_is_supported  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4457$$Nest$smnative_is_supported() {
        return native_is_supported();
    }

    /* renamed from: -$$Nest$smnative_start  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4466$$Nest$smnative_start() {
        return native_start();
    }

    /* renamed from: -$$Nest$smnative_start_antenna_info_listening  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4467$$Nest$smnative_start_antenna_info_listening() {
        return native_start_antenna_info_listening();
    }

    /* renamed from: -$$Nest$smnative_start_navigation_message_collection  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4470$$Nest$smnative_start_navigation_message_collection() {
        return native_start_navigation_message_collection();
    }

    /* renamed from: -$$Nest$smnative_start_nmea_message_collection  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4471$$Nest$smnative_start_nmea_message_collection() {
        return native_start_nmea_message_collection();
    }

    /* renamed from: -$$Nest$smnative_start_sv_status_collection  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4472$$Nest$smnative_start_sv_status_collection() {
        return native_start_sv_status_collection();
    }

    /* renamed from: -$$Nest$smnative_stop  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4473$$Nest$smnative_stop() {
        return native_stop();
    }

    /* renamed from: -$$Nest$smnative_stop_antenna_info_listening  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4474$$Nest$smnative_stop_antenna_info_listening() {
        return native_stop_antenna_info_listening();
    }

    /* renamed from: -$$Nest$smnative_stop_batch  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4475$$Nest$smnative_stop_batch() {
        return native_stop_batch();
    }

    /* renamed from: -$$Nest$smnative_stop_measurement_collection  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4476$$Nest$smnative_stop_measurement_collection() {
        return native_stop_measurement_collection();
    }

    /* renamed from: -$$Nest$smnative_stop_navigation_message_collection  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4477$$Nest$smnative_stop_navigation_message_collection() {
        return native_stop_navigation_message_collection();
    }

    /* renamed from: -$$Nest$smnative_stop_nmea_message_collection  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4478$$Nest$smnative_stop_nmea_message_collection() {
        return native_stop_nmea_message_collection();
    }

    /* renamed from: -$$Nest$smnative_stop_sv_status_collection  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4479$$Nest$smnative_stop_sv_status_collection() {
        return native_stop_sv_status_collection();
    }

    /* renamed from: -$$Nest$smnative_supports_psds  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m4480$$Nest$smnative_supports_psds() {
        return native_supports_psds();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_add_geofence(int i, double d, double d2, double d3, int i2, int i3, int i4, int i5);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_agps_set_id(int i, String str);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_agps_set_ref_location_cellid(int i, int i2, int i3, int i4, long j, int i5, int i6, int i7);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_class_init_once();

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_cleanup();

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_cleanup_batching();

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_delete_aiding_data(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_flush_batch();

    private static native int native_get_batch_size();

    private static native String native_get_internal_state();

    private static native boolean native_init();

    private static native boolean native_init_batching();

    /* JADX INFO: Access modifiers changed from: private */
    public native void native_init_once(boolean z);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_inject_best_location(int i, double d, double d2, double d3, float f, float f2, float f3, float f4, float f5, float f6, long j, int i2, long j2, double d4);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_inject_location(int i, double d, double d2, double d3, float f, float f2, float f3, float f4, float f5, float f6, long j, int i2, long j2, double d4);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_inject_measurement_corrections(GnssMeasurementCorrections gnssMeasurementCorrections);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_inject_psds_data(byte[] bArr, int i, int i2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_inject_time(long j, long j2, int i);

    private static native boolean native_is_antenna_info_supported();

    private static native boolean native_is_geofence_supported();

    private static native boolean native_is_gnss_visibility_control_supported();

    private static native boolean native_is_measurement_corrections_supported();

    private static native boolean native_is_measurement_supported();

    private static native boolean native_is_navigation_message_supported();

    private static native boolean native_is_supported();

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_pause_geofence(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int native_read_nmea(byte[] bArr, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_remove_geofence(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_request_power_stats();

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_resume_geofence(int i, int i2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_send_ni_response(int i, int i2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_set_agps_server(int i, String str, int i2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_position_mode(int i, int i2, int i3, int i4, int i5, boolean z);

    private static native boolean native_start();

    private static native boolean native_start_antenna_info_listening();

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_start_batch(long j, float f, boolean z);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_start_measurement_collection(boolean z, boolean z2, int i);

    private static native boolean native_start_navigation_message_collection();

    private static native boolean native_start_nmea_message_collection();

    private static native boolean native_start_sv_status_collection();

    private static native boolean native_stop();

    private static native boolean native_stop_antenna_info_listening();

    private static native boolean native_stop_batch();

    private static native boolean native_stop_measurement_collection();

    private static native boolean native_stop_navigation_message_collection();

    private static native boolean native_stop_nmea_message_collection();

    private static native boolean native_stop_sv_status_collection();

    private static native boolean native_supports_psds();

    /* loaded from: classes.dex */
    public interface BaseCallbacks {
        void onHalRestarted();

        default void onHalStarted() {
        }

        default void onCapabilitiesChanged(GnssCapabilities oldCapabilities, GnssCapabilities newCapabilities) {
        }
    }

    public static synchronized void setGnssHalForTest(GnssHal gnssHal) {
        synchronized (GnssNative.class) {
            sGnssHal = (GnssHal) Objects.requireNonNull(gnssHal);
            sGnssHalInitialized = false;
            sInstance = null;
        }
    }

    private static synchronized void initializeHal() {
        synchronized (GnssNative.class) {
            if (!sGnssHalInitialized) {
                if (sGnssHal == null) {
                    sGnssHal = new GnssHal();
                }
                sGnssHal.classInitOnce();
                sGnssHalInitialized = true;
            }
        }
    }

    public static synchronized boolean isSupported() {
        boolean isSupported;
        synchronized (GnssNative.class) {
            initializeHal();
            isSupported = sGnssHal.isSupported();
        }
        return isSupported;
    }

    public static synchronized GnssNative create(Injector injector, GnssConfiguration configuration) {
        GnssNative gnssNative;
        synchronized (GnssNative.class) {
            Preconditions.checkState(isSupported());
            Preconditions.checkState(sInstance == null);
            gnssNative = new GnssNative(sGnssHal, injector, configuration);
            sInstance = gnssNative;
        }
        return gnssNative;
    }

    private GnssNative(GnssHal gnssHal, Injector injector, GnssConfiguration configuration) {
        this.mGnssHal = (GnssHal) Objects.requireNonNull(gnssHal);
        this.mEmergencyHelper = injector.getEmergencyHelper();
        this.mConfiguration = configuration;
    }

    public void addBaseCallbacks(BaseCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        this.mBaseCallbacks = (BaseCallbacks[]) ArrayUtils.appendElement(BaseCallbacks.class, this.mBaseCallbacks, callbacks);
    }

    public void addStatusCallbacks(StatusCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        this.mStatusCallbacks = (StatusCallbacks[]) ArrayUtils.appendElement(StatusCallbacks.class, this.mStatusCallbacks, callbacks);
    }

    public void addSvStatusCallbacks(SvStatusCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        this.mSvStatusCallbacks = (SvStatusCallbacks[]) ArrayUtils.appendElement(SvStatusCallbacks.class, this.mSvStatusCallbacks, callbacks);
    }

    public void addNmeaCallbacks(NmeaCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        this.mNmeaCallbacks = (NmeaCallbacks[]) ArrayUtils.appendElement(NmeaCallbacks.class, this.mNmeaCallbacks, callbacks);
    }

    public void addLocationCallbacks(LocationCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        this.mLocationCallbacks = (LocationCallbacks[]) ArrayUtils.appendElement(LocationCallbacks.class, this.mLocationCallbacks, callbacks);
    }

    public void addMeasurementCallbacks(MeasurementCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        this.mMeasurementCallbacks = (MeasurementCallbacks[]) ArrayUtils.appendElement(MeasurementCallbacks.class, this.mMeasurementCallbacks, callbacks);
    }

    public void addAntennaInfoCallbacks(AntennaInfoCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        this.mAntennaInfoCallbacks = (AntennaInfoCallbacks[]) ArrayUtils.appendElement(AntennaInfoCallbacks.class, this.mAntennaInfoCallbacks, callbacks);
    }

    public void addNavigationMessageCallbacks(NavigationMessageCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        this.mNavigationMessageCallbacks = (NavigationMessageCallbacks[]) ArrayUtils.appendElement(NavigationMessageCallbacks.class, this.mNavigationMessageCallbacks, callbacks);
    }

    public void setGeofenceCallbacks(GeofenceCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        Preconditions.checkState(this.mGeofenceCallbacks == null);
        this.mGeofenceCallbacks = (GeofenceCallbacks) Objects.requireNonNull(callbacks);
    }

    public void setTimeCallbacks(TimeCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        Preconditions.checkState(this.mTimeCallbacks == null);
        this.mTimeCallbacks = (TimeCallbacks) Objects.requireNonNull(callbacks);
    }

    public void setLocationRequestCallbacks(LocationRequestCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        Preconditions.checkState(this.mLocationRequestCallbacks == null);
        this.mLocationRequestCallbacks = (LocationRequestCallbacks) Objects.requireNonNull(callbacks);
    }

    public void setPsdsCallbacks(PsdsCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        Preconditions.checkState(this.mPsdsCallbacks == null);
        this.mPsdsCallbacks = (PsdsCallbacks) Objects.requireNonNull(callbacks);
    }

    public void setAGpsCallbacks(AGpsCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        Preconditions.checkState(this.mAGpsCallbacks == null);
        this.mAGpsCallbacks = (AGpsCallbacks) Objects.requireNonNull(callbacks);
    }

    public void setNotificationCallbacks(NotificationCallbacks callbacks) {
        Preconditions.checkState(!this.mRegistered);
        Preconditions.checkState(this.mNotificationCallbacks == null);
        this.mNotificationCallbacks = (NotificationCallbacks) Objects.requireNonNull(callbacks);
    }

    public void register() {
        Preconditions.checkState(!this.mRegistered);
        this.mRegistered = true;
        initializeGnss(false);
        Log.i(GnssManagerService.TAG, "gnss hal started");
        int i = 0;
        while (true) {
            BaseCallbacks[] baseCallbacksArr = this.mBaseCallbacks;
            if (i < baseCallbacksArr.length) {
                baseCallbacksArr[i].onHalStarted();
                i++;
            } else {
                return;
            }
        }
    }

    private void initializeGnss(boolean restart) {
        Preconditions.checkState(this.mRegistered);
        this.mTopFlags = 0;
        this.mGnssHal.initOnce(this, restart);
        if (this.mGnssHal.init()) {
            this.mGnssHal.cleanup();
            Log.i(GnssManagerService.TAG, "gnss hal initialized");
            return;
        }
        Log.e(GnssManagerService.TAG, "gnss hal initialization failed");
    }

    public GnssConfiguration getConfiguration() {
        return this.mConfiguration;
    }

    public boolean init() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.init();
    }

    public void cleanup() {
        Preconditions.checkState(this.mRegistered);
        this.mGnssHal.cleanup();
    }

    public GnssPowerStats getPowerStats() {
        return this.mPowerStats;
    }

    public GnssCapabilities getCapabilities() {
        return this.mCapabilities;
    }

    public int getHardwareYear() {
        return this.mHardwareYear;
    }

    public String getHardwareModelName() {
        return this.mHardwareModelName;
    }

    public boolean isItarSpeedLimitExceeded() {
        return this.mItarSpeedLimitExceeded;
    }

    public boolean start() {
        Preconditions.checkState(this.mRegistered);
        this.mStartRealtimeMs = SystemClock.elapsedRealtime();
        this.mHasFirstFix = false;
        return this.mGnssHal.start();
    }

    public boolean stop() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.stop();
    }

    public boolean setPositionMode(int mode, int recurrence, int minInterval, int preferredAccuracy, int preferredTime, boolean lowPowerMode) {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.setPositionMode(mode, recurrence, minInterval, preferredAccuracy, preferredTime, lowPowerMode);
    }

    public String getInternalState() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.getInternalState();
    }

    public void deleteAidingData(int flags) {
        Preconditions.checkState(this.mRegistered);
        this.mGnssHal.deleteAidingData(flags);
    }

    public int readNmea(byte[] buffer, int bufferSize) {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.readNmea(buffer, bufferSize);
    }

    public void injectLocation(Location location) {
        Preconditions.checkState(this.mRegistered);
        if (location.hasAccuracy()) {
            int gnssLocationFlags = (location.hasAltitude() ? 2 : 0) | 1 | (location.hasSpeed() ? 4 : 0) | (location.hasBearing() ? 8 : 0) | (location.hasAccuracy() ? 16 : 0) | (location.hasVerticalAccuracy() ? 32 : 0) | (location.hasSpeedAccuracy() ? 64 : 0) | (location.hasBearingAccuracy() ? 128 : 0);
            double latitudeDegrees = location.getLatitude();
            double longitudeDegrees = location.getLongitude();
            double altitudeMeters = location.getAltitude();
            float speedMetersPerSec = location.getSpeed();
            float bearingDegrees = location.getBearing();
            float horizontalAccuracyMeters = location.getAccuracy();
            float verticalAccuracyMeters = location.getVerticalAccuracyMeters();
            float speedAccuracyMetersPerSecond = location.getSpeedAccuracyMetersPerSecond();
            float bearingAccuracyDegrees = location.getBearingAccuracyDegrees();
            long timestamp = location.getTime();
            int elapsedRealtimeFlags = (location.hasElapsedRealtimeUncertaintyNanos() ? 2 : 0) | 1;
            long elapsedRealtimeNanos = location.getElapsedRealtimeNanos();
            double elapsedRealtimeUncertaintyNanos = location.getElapsedRealtimeUncertaintyNanos();
            this.mGnssHal.injectLocation(gnssLocationFlags, latitudeDegrees, longitudeDegrees, altitudeMeters, speedMetersPerSec, bearingDegrees, horizontalAccuracyMeters, verticalAccuracyMeters, speedAccuracyMetersPerSecond, bearingAccuracyDegrees, timestamp, elapsedRealtimeFlags, elapsedRealtimeNanos, elapsedRealtimeUncertaintyNanos);
        }
    }

    public void injectBestLocation(Location location) {
        Preconditions.checkState(this.mRegistered);
        int gnssLocationFlags = (location.hasAltitude() ? 2 : 0) | 1 | (location.hasSpeed() ? 4 : 0) | (location.hasBearing() ? 8 : 0) | (location.hasAccuracy() ? 16 : 0) | (location.hasVerticalAccuracy() ? 32 : 0) | (location.hasSpeedAccuracy() ? 64 : 0) | (location.hasBearingAccuracy() ? 128 : 0);
        double latitudeDegrees = location.getLatitude();
        double longitudeDegrees = location.getLongitude();
        double altitudeMeters = location.getAltitude();
        float speedMetersPerSec = location.getSpeed();
        float bearingDegrees = location.getBearing();
        float horizontalAccuracyMeters = location.getAccuracy();
        float verticalAccuracyMeters = location.getVerticalAccuracyMeters();
        float speedAccuracyMetersPerSecond = location.getSpeedAccuracyMetersPerSecond();
        float bearingAccuracyDegrees = location.getBearingAccuracyDegrees();
        long timestamp = location.getTime();
        int elapsedRealtimeFlags = (location.hasElapsedRealtimeUncertaintyNanos() ? 2 : 0) | 1;
        long elapsedRealtimeNanos = location.getElapsedRealtimeNanos();
        double elapsedRealtimeUncertaintyNanos = location.getElapsedRealtimeUncertaintyNanos();
        this.mGnssHal.injectBestLocation(gnssLocationFlags, latitudeDegrees, longitudeDegrees, altitudeMeters, speedMetersPerSec, bearingDegrees, horizontalAccuracyMeters, verticalAccuracyMeters, speedAccuracyMetersPerSecond, bearingAccuracyDegrees, timestamp, elapsedRealtimeFlags, elapsedRealtimeNanos, elapsedRealtimeUncertaintyNanos);
    }

    public void injectTime(long time, long timeReference, int uncertainty) {
        Preconditions.checkState(this.mRegistered);
        this.mGnssHal.injectTime(time, timeReference, uncertainty);
    }

    public boolean isNavigationMessageCollectionSupported() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.isNavigationMessageCollectionSupported();
    }

    public boolean startNavigationMessageCollection() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.startNavigationMessageCollection();
    }

    public boolean stopNavigationMessageCollection() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.stopNavigationMessageCollection();
    }

    public boolean isAntennaInfoSupported() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.isAntennaInfoSupported();
    }

    public boolean startAntennaInfoListening() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.startAntennaInfoListening();
    }

    public boolean stopAntennaInfoListening() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.stopAntennaInfoListening();
    }

    public boolean isMeasurementSupported() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.isMeasurementSupported();
    }

    public boolean startMeasurementCollection(boolean enableFullTracking, boolean enableCorrVecOutputs, int intervalMillis) {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.startMeasurementCollection(enableFullTracking, enableCorrVecOutputs, intervalMillis);
    }

    public boolean stopMeasurementCollection() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.stopMeasurementCollection();
    }

    public boolean startSvStatusCollection() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.startSvStatusCollection();
    }

    public boolean stopSvStatusCollection() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.stopSvStatusCollection();
    }

    public boolean startNmeaMessageCollection() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.startNmeaMessageCollection();
    }

    public boolean stopNmeaMessageCollection() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.stopNmeaMessageCollection();
    }

    public boolean isMeasurementCorrectionsSupported() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.isMeasurementCorrectionsSupported();
    }

    public boolean injectMeasurementCorrections(GnssMeasurementCorrections corrections) {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.injectMeasurementCorrections(corrections);
    }

    public boolean initBatching() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.initBatching();
    }

    public void cleanupBatching() {
        Preconditions.checkState(this.mRegistered);
        this.mGnssHal.cleanupBatching();
    }

    public boolean startBatch(long periodNanos, float minUpdateDistanceMeters, boolean wakeOnFifoFull) {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.startBatch(periodNanos, minUpdateDistanceMeters, wakeOnFifoFull);
    }

    public void flushBatch() {
        Preconditions.checkState(this.mRegistered);
        this.mGnssHal.flushBatch();
    }

    public void stopBatch() {
        Preconditions.checkState(this.mRegistered);
        this.mGnssHal.stopBatch();
    }

    public int getBatchSize() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.getBatchSize();
    }

    public boolean isGeofencingSupported() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.isGeofencingSupported();
    }

    public boolean addGeofence(int geofenceId, double latitude, double longitude, double radius, int lastTransition, int monitorTransitions, int notificationResponsiveness, int unknownTimer) {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.addGeofence(geofenceId, latitude, longitude, radius, lastTransition, monitorTransitions, notificationResponsiveness, unknownTimer);
    }

    public boolean resumeGeofence(int geofenceId, int monitorTransitions) {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.resumeGeofence(geofenceId, monitorTransitions);
    }

    public boolean pauseGeofence(int geofenceId) {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.pauseGeofence(geofenceId);
    }

    public boolean removeGeofence(int geofenceId) {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.removeGeofence(geofenceId);
    }

    public boolean isGnssVisibilityControlSupported() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.isGnssVisibilityControlSupported();
    }

    public void sendNiResponse(int notificationId, int userResponse) {
        Preconditions.checkState(this.mRegistered);
        this.mGnssHal.sendNiResponse(notificationId, userResponse);
    }

    public void requestPowerStats() {
        Preconditions.checkState(this.mRegistered);
        this.mGnssHal.requestPowerStats();
    }

    public void setAgpsServer(int type, String hostname, int port) {
        Preconditions.checkState(this.mRegistered);
        this.mGnssHal.setAgpsServer(type, hostname, port);
    }

    public void setAgpsSetId(int type, String setId) {
        Preconditions.checkState(this.mRegistered);
        this.mGnssHal.setAgpsSetId(type, setId);
    }

    public void setAgpsReferenceLocationCellId(int type, int mcc, int mnc, int lac, long cid, int tac, int pcid, int arfcn) {
        Preconditions.checkState(this.mRegistered);
        this.mGnssHal.setAgpsReferenceLocationCellId(type, mcc, mnc, lac, cid, tac, pcid, arfcn);
    }

    public boolean isPsdsSupported() {
        Preconditions.checkState(this.mRegistered);
        return this.mGnssHal.isPsdsSupported();
    }

    public void injectPsdsData(byte[] data, int length, int psdsType) {
        Preconditions.checkState(this.mRegistered);
        this.mGnssHal.injectPsdsData(data, length, psdsType);
    }

    void reportGnssServiceDied() {
        Log.e(GnssManagerService.TAG, "gnss hal died - restarting shortly...");
        FgThread.getExecutor().execute(new Runnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                GnssNative.this.restartHal();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void restartHal() {
        initializeGnss(true);
        Log.e(GnssManagerService.TAG, "gnss hal restarted");
        int i = 0;
        while (true) {
            BaseCallbacks[] baseCallbacksArr = this.mBaseCallbacks;
            if (i < baseCallbacksArr.length) {
                baseCallbacksArr[i].onHalRestarted();
                i++;
            } else {
                return;
            }
        }
    }

    void reportLocation(final boolean hasLatLong, final Location location) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda1
            public final void runOrThrow() {
                GnssNative.this.m4492x58fa894(hasLatLong, location);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportLocation$0$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4492x58fa894(boolean hasLatLong, Location location) throws Exception {
        if (hasLatLong && !this.mHasFirstFix) {
            this.mHasFirstFix = true;
            int ttff = (int) (SystemClock.elapsedRealtime() - this.mStartRealtimeMs);
            int i = 0;
            while (true) {
                StatusCallbacks[] statusCallbacksArr = this.mStatusCallbacks;
                if (i >= statusCallbacksArr.length) {
                    break;
                }
                statusCallbacksArr[i].onReportFirstFix(ttff);
                i++;
            }
        }
        if (location.hasSpeed()) {
            boolean exceeded = location.getSpeed() > ITAR_SPEED_LIMIT_METERS_PER_SECOND;
            if (!this.mItarSpeedLimitExceeded && exceeded) {
                Log.w(GnssManagerService.TAG, "speed nearing ITAR threshold - blocking further GNSS output");
            } else if (this.mItarSpeedLimitExceeded && !exceeded) {
                Log.w(GnssManagerService.TAG, "speed leaving ITAR threshold - allowing further GNSS output");
            }
            this.mItarSpeedLimitExceeded = exceeded;
        }
        if (this.mItarSpeedLimitExceeded) {
            return;
        }
        int i2 = 0;
        while (true) {
            LocationCallbacks[] locationCallbacksArr = this.mLocationCallbacks;
            if (i2 < locationCallbacksArr.length) {
                locationCallbacksArr[i2].onReportLocation(hasLatLong, location);
                i2++;
            } else {
                return;
            }
        }
    }

    void reportStatus(final int gnssStatus) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda21
            public final void runOrThrow() {
                GnssNative.this.m4499x1767b9d2(gnssStatus);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportStatus$1$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4499x1767b9d2(int gnssStatus) throws Exception {
        int i = 0;
        while (true) {
            StatusCallbacks[] statusCallbacksArr = this.mStatusCallbacks;
            if (i < statusCallbacksArr.length) {
                statusCallbacksArr[i].onReportStatus(gnssStatus);
                i++;
            } else {
                return;
            }
        }
    }

    void reportSvStatus(final int svCount, final int[] svidWithFlags, final float[] cn0DbHzs, final float[] elevations, final float[] azimuths, final float[] carrierFrequencies, final float[] basebandCn0DbHzs) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda19
            public final void runOrThrow() {
                GnssNative.this.m4500x6a4e5116(svCount, svidWithFlags, cn0DbHzs, elevations, azimuths, carrierFrequencies, basebandCn0DbHzs);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportSvStatus$2$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4500x6a4e5116(int svCount, int[] svidWithFlags, float[] cn0DbHzs, float[] elevations, float[] azimuths, float[] carrierFrequencies, float[] basebandCn0DbHzs) throws Exception {
        GnssStatus gnssStatus = GnssStatus.wrap(svCount, svidWithFlags, cn0DbHzs, elevations, azimuths, carrierFrequencies, basebandCn0DbHzs);
        int i = 0;
        while (true) {
            SvStatusCallbacks[] svStatusCallbacksArr = this.mSvStatusCallbacks;
            if (i < svStatusCallbacksArr.length) {
                svStatusCallbacksArr[i].onReportSvStatus(gnssStatus);
                i++;
            } else {
                return;
            }
        }
    }

    void reportAGpsStatus(final int agpsType, final int agpsStatus, final byte[] suplIpAddr) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda17
            public final void runOrThrow() {
                GnssNative.this.m4484x626542fd(agpsType, agpsStatus, suplIpAddr);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportAGpsStatus$3$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4484x626542fd(int agpsType, int agpsStatus, byte[] suplIpAddr) throws Exception {
        this.mAGpsCallbacks.onReportAGpsStatus(agpsType, agpsStatus, suplIpAddr);
    }

    void reportNmea(final long timestamp) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda6
            public final void runOrThrow() {
                GnssNative.this.m4498xf4c4c27e(timestamp);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportNmea$4$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4498xf4c4c27e(long timestamp) throws Exception {
        if (this.mItarSpeedLimitExceeded) {
            return;
        }
        int i = 0;
        while (true) {
            NmeaCallbacks[] nmeaCallbacksArr = this.mNmeaCallbacks;
            if (i < nmeaCallbacksArr.length) {
                nmeaCallbacksArr[i].onReportNmea(timestamp);
                i++;
            } else {
                return;
            }
        }
    }

    void reportMeasurementData(final GnssMeasurementsEvent event) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda23
            public final void runOrThrow() {
                GnssNative.this.m4494x13cbd5e8(event);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportMeasurementData$5$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4494x13cbd5e8(GnssMeasurementsEvent event) throws Exception {
        if (this.mItarSpeedLimitExceeded) {
            return;
        }
        int i = 0;
        while (true) {
            MeasurementCallbacks[] measurementCallbacksArr = this.mMeasurementCallbacks;
            if (i < measurementCallbacksArr.length) {
                measurementCallbacksArr[i].onReportMeasurements(event);
                i++;
            } else {
                return;
            }
        }
    }

    void reportAntennaInfo(final List<GnssAntennaInfo> antennaInfos) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda10
            public final void runOrThrow() {
                GnssNative.this.m4485xb6b0c6f4(antennaInfos);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportAntennaInfo$6$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4485xb6b0c6f4(List antennaInfos) throws Exception {
        int i = 0;
        while (true) {
            AntennaInfoCallbacks[] antennaInfoCallbacksArr = this.mAntennaInfoCallbacks;
            if (i < antennaInfoCallbacksArr.length) {
                antennaInfoCallbacksArr[i].onReportAntennaInfo(antennaInfos);
                i++;
            } else {
                return;
            }
        }
    }

    void reportNavigationMessage(final GnssNavigationMessage event) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda2
            public final void runOrThrow() {
                GnssNative.this.m4495xf0b52b7(event);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportNavigationMessage$7$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4495xf0b52b7(GnssNavigationMessage event) throws Exception {
        if (this.mItarSpeedLimitExceeded) {
            return;
        }
        int i = 0;
        while (true) {
            NavigationMessageCallbacks[] navigationMessageCallbacksArr = this.mNavigationMessageCallbacks;
            if (i < navigationMessageCallbacksArr.length) {
                navigationMessageCallbacksArr[i].onReportNavigationMessage(event);
                i++;
            } else {
                return;
            }
        }
    }

    void setTopHalCapabilities(int capabilities) {
        int i = this.mTopFlags | capabilities;
        this.mTopFlags = i;
        GnssCapabilities oldCapabilities = this.mCapabilities;
        GnssCapabilities withTopHalFlags = oldCapabilities.withTopHalFlags(i);
        this.mCapabilities = withTopHalFlags;
        onCapabilitiesChanged(oldCapabilities, withTopHalFlags);
    }

    void setSubHalMeasurementCorrectionsCapabilities(int capabilities) {
        GnssCapabilities oldCapabilities = this.mCapabilities;
        GnssCapabilities withSubHalMeasurementCorrectionsFlags = oldCapabilities.withSubHalMeasurementCorrectionsFlags(capabilities);
        this.mCapabilities = withSubHalMeasurementCorrectionsFlags;
        onCapabilitiesChanged(oldCapabilities, withSubHalMeasurementCorrectionsFlags);
    }

    void setSubHalPowerIndicationCapabilities(int capabilities) {
        GnssCapabilities oldCapabilities = this.mCapabilities;
        GnssCapabilities withSubHalPowerFlags = oldCapabilities.withSubHalPowerFlags(capabilities);
        this.mCapabilities = withSubHalPowerFlags;
        onCapabilitiesChanged(oldCapabilities, withSubHalPowerFlags);
    }

    private void onCapabilitiesChanged(final GnssCapabilities oldCapabilities, final GnssCapabilities newCapabilities) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda14
            public final void runOrThrow() {
                GnssNative.this.m4482xd6f09ab8(newCapabilities, oldCapabilities);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onCapabilitiesChanged$8$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4482xd6f09ab8(GnssCapabilities newCapabilities, GnssCapabilities oldCapabilities) throws Exception {
        if (newCapabilities.equals(oldCapabilities)) {
            return;
        }
        Log.i(GnssManagerService.TAG, "gnss capabilities changed to " + newCapabilities);
        int i = 0;
        while (true) {
            BaseCallbacks[] baseCallbacksArr = this.mBaseCallbacks;
            if (i < baseCallbacksArr.length) {
                baseCallbacksArr[i].onCapabilitiesChanged(oldCapabilities, newCapabilities);
                i++;
            } else {
                return;
            }
        }
    }

    void reportGnssPowerStats(GnssPowerStats powerStats) {
        this.mPowerStats = powerStats;
    }

    void setGnssYearOfHardware(int year) {
        this.mHardwareYear = year;
    }

    private void setGnssHardwareModelName(String modelName) {
        this.mHardwareModelName = modelName;
    }

    void reportLocationBatch(final Location[] locations) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda18
            public final void runOrThrow() {
                GnssNative.this.m4493xbe5636eb(locations);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportLocationBatch$9$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4493xbe5636eb(Location[] locations) throws Exception {
        int i = 0;
        while (true) {
            LocationCallbacks[] locationCallbacksArr = this.mLocationCallbacks;
            if (i < locationCallbacksArr.length) {
                locationCallbacksArr[i].onReportLocations(locations);
                i++;
            } else {
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$psdsDownloadRequest$10$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4483xa07d8ded(int psdsType) throws Exception {
        this.mPsdsCallbacks.onRequestPsdsDownload(psdsType);
    }

    void psdsDownloadRequest(final int psdsType) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda22
            public final void runOrThrow() {
                GnssNative.this.m4483xa07d8ded(psdsType);
            }
        });
    }

    void reportGeofenceTransition(final int geofenceId, final Location location, final int transition, final long transitionTimestamp) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda0
            public final void runOrThrow() {
                GnssNative.this.m4491x39888200(geofenceId, location, transition, transitionTimestamp);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportGeofenceTransition$11$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4491x39888200(int geofenceId, Location location, int transition, long transitionTimestamp) throws Exception {
        this.mGeofenceCallbacks.onReportGeofenceTransition(geofenceId, location, transition, transitionTimestamp);
    }

    void reportGeofenceStatus(final int status, final Location location) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda15
            public final void runOrThrow() {
                GnssNative.this.m4490x41995c4(status, location);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportGeofenceStatus$12$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4490x41995c4(int status, Location location) throws Exception {
        this.mGeofenceCallbacks.onReportGeofenceStatus(status, location);
    }

    void reportGeofenceAddStatus(final int geofenceId, final int status) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda11
            public final void runOrThrow() {
                GnssNative.this.m4486x5547c66(geofenceId, status);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportGeofenceAddStatus$13$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4486x5547c66(int geofenceId, int status) throws Exception {
        this.mGeofenceCallbacks.onReportGeofenceAddStatus(geofenceId, status);
    }

    void reportGeofenceRemoveStatus(final int geofenceId, final int status) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda12
            public final void runOrThrow() {
                GnssNative.this.m4488x19af5e62(geofenceId, status);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportGeofenceRemoveStatus$14$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4488x19af5e62(int geofenceId, int status) throws Exception {
        this.mGeofenceCallbacks.onReportGeofenceRemoveStatus(geofenceId, status);
    }

    void reportGeofencePauseStatus(final int geofenceId, final int status) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda13
            public final void runOrThrow() {
                GnssNative.this.m4487x44b11873(geofenceId, status);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportGeofencePauseStatus$15$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4487x44b11873(int geofenceId, int status) throws Exception {
        this.mGeofenceCallbacks.onReportGeofencePauseStatus(geofenceId, status);
    }

    void reportGeofenceResumeStatus(final int geofenceId, final int status) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda4
            public final void runOrThrow() {
                GnssNative.this.m4489x536e6db(geofenceId, status);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportGeofenceResumeStatus$16$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4489x536e6db(int geofenceId, int status) throws Exception {
        this.mGeofenceCallbacks.onReportGeofenceResumeStatus(geofenceId, status);
    }

    void reportNiNotification(final int notificationId, final int niType, final int notifyFlags, final int timeout, final int defaultResponse, final String requestorId, final String text, final int requestorIdEncoding, final int textEncoding) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda24
            public final void runOrThrow() {
                GnssNative.this.m4497x20942095(notificationId, niType, notifyFlags, timeout, defaultResponse, requestorId, text, requestorIdEncoding, textEncoding);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportNiNotification$17$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4497x20942095(int notificationId, int niType, int notifyFlags, int timeout, int defaultResponse, String requestorId, String text, int requestorIdEncoding, int textEncoding) throws Exception {
        this.mNotificationCallbacks.onReportNiNotification(notificationId, niType, notifyFlags, timeout, defaultResponse, requestorId, text, requestorIdEncoding, textEncoding);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestSetID$18$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4503x489c4a02(int flags) throws Exception {
        this.mAGpsCallbacks.onRequestSetID(flags);
    }

    void requestSetID(final int flags) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda8
            public final void runOrThrow() {
                GnssNative.this.m4503x489c4a02(flags);
            }
        });
    }

    void requestLocation(final boolean independentFromGnss, final boolean isUserEmergency) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda20
            public final void runOrThrow() {
                GnssNative.this.m4501x3e4e9b67(independentFromGnss, isUserEmergency);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestLocation$19$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4501x3e4e9b67(boolean independentFromGnss, boolean isUserEmergency) throws Exception {
        this.mLocationRequestCallbacks.onRequestLocation(independentFromGnss, isUserEmergency);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestUtcTime$20$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4504xc48a6d85() throws Exception {
        this.mTimeCallbacks.onRequestUtcTime();
    }

    void requestUtcTime() {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda3
            public final void runOrThrow() {
                GnssNative.this.m4504xc48a6d85();
            }
        });
    }

    void requestRefLocation() {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda9
            public final void runOrThrow() {
                GnssNative.this.m4502xbe6b164f();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestRefLocation$21$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4502xbe6b164f() throws Exception {
        this.mLocationRequestCallbacks.onRequestRefLocation();
    }

    void reportNfwNotification(final String proxyAppPackageName, final byte protocolStack, final String otherProtocolStackName, final byte requestor, final String requestorId, final byte responseType, final boolean inEmergencyMode, final boolean isCachedLocation) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda7
            public final void runOrThrow() {
                GnssNative.this.m4496x8a7ed76d(proxyAppPackageName, protocolStack, otherProtocolStackName, requestor, requestorId, responseType, inEmergencyMode, isCachedLocation);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportNfwNotification$22$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ void m4496x8a7ed76d(String proxyAppPackageName, byte protocolStack, String otherProtocolStackName, byte requestor, String requestorId, byte responseType, boolean inEmergencyMode, boolean isCachedLocation) throws Exception {
        this.mNotificationCallbacks.onReportNfwNotification(proxyAppPackageName, protocolStack, otherProtocolStackName, requestor, requestorId, responseType, inEmergencyMode, isCachedLocation);
    }

    boolean isInEmergencySession() {
        return ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.location.gnss.hal.GnssNative$$ExternalSyntheticLambda16
            public final Object getOrThrow() {
                return GnssNative.this.m4481x8dc4bf36();
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isInEmergencySession$23$com-android-server-location-gnss-hal-GnssNative  reason: not valid java name */
    public /* synthetic */ Boolean m4481x8dc4bf36() throws Exception {
        return Boolean.valueOf(this.mEmergencyHelper.isInEmergency(TimeUnit.SECONDS.toMillis(this.mConfiguration.getEsExtensionSec())));
    }

    /* loaded from: classes.dex */
    public static class GnssHal {
        protected GnssHal() {
        }

        protected void classInitOnce() {
            GnssNative.native_class_init_once();
        }

        protected boolean isSupported() {
            return GnssNative.m4457$$Nest$smnative_is_supported();
        }

        protected void initOnce(GnssNative gnssNative, boolean reinitializeGnssServiceHandle) {
            gnssNative.native_init_once(reinitializeGnssServiceHandle);
        }

        protected boolean init() {
            return GnssNative.m4444$$Nest$smnative_init();
        }

        protected void cleanup() {
            GnssNative.native_cleanup();
        }

        protected boolean start() {
            return GnssNative.m4466$$Nest$smnative_start();
        }

        protected boolean stop() {
            return GnssNative.m4473$$Nest$smnative_stop();
        }

        protected boolean setPositionMode(int mode, int recurrence, int minInterval, int preferredAccuracy, int preferredTime, boolean lowPowerMode) {
            return GnssNative.native_set_position_mode(mode, recurrence, minInterval, preferredAccuracy, preferredTime, lowPowerMode);
        }

        protected String getInternalState() {
            return GnssNative.m4443$$Nest$smnative_get_internal_state();
        }

        protected void deleteAidingData(int flags) {
            GnssNative.native_delete_aiding_data(flags);
        }

        protected int readNmea(byte[] buffer, int bufferSize) {
            return GnssNative.native_read_nmea(buffer, bufferSize);
        }

        protected void injectLocation(int gnssLocationFlags, double latitude, double longitude, double altitude, float speed, float bearing, float horizontalAccuracy, float verticalAccuracy, float speedAccuracy, float bearingAccuracy, long timestamp, int elapsedRealtimeFlags, long elapsedRealtimeNanos, double elapsedRealtimeUncertaintyNanos) {
            GnssNative.native_inject_location(gnssLocationFlags, latitude, longitude, altitude, speed, bearing, horizontalAccuracy, verticalAccuracy, speedAccuracy, bearingAccuracy, timestamp, elapsedRealtimeFlags, elapsedRealtimeNanos, elapsedRealtimeUncertaintyNanos);
        }

        protected void injectBestLocation(int gnssLocationFlags, double latitude, double longitude, double altitude, float speed, float bearing, float horizontalAccuracy, float verticalAccuracy, float speedAccuracy, float bearingAccuracy, long timestamp, int elapsedRealtimeFlags, long elapsedRealtimeNanos, double elapsedRealtimeUncertaintyNanos) {
            GnssNative.native_inject_best_location(gnssLocationFlags, latitude, longitude, altitude, speed, bearing, horizontalAccuracy, verticalAccuracy, speedAccuracy, bearingAccuracy, timestamp, elapsedRealtimeFlags, elapsedRealtimeNanos, elapsedRealtimeUncertaintyNanos);
        }

        protected void injectTime(long time, long timeReference, int uncertainty) {
            GnssNative.native_inject_time(time, timeReference, uncertainty);
        }

        protected boolean isNavigationMessageCollectionSupported() {
            return GnssNative.m4456$$Nest$smnative_is_navigation_message_supported();
        }

        protected boolean startNavigationMessageCollection() {
            return GnssNative.m4470$$Nest$smnative_start_navigation_message_collection();
        }

        protected boolean stopNavigationMessageCollection() {
            return GnssNative.m4477$$Nest$smnative_stop_navigation_message_collection();
        }

        protected boolean isAntennaInfoSupported() {
            return GnssNative.m4451$$Nest$smnative_is_antenna_info_supported();
        }

        protected boolean startAntennaInfoListening() {
            return GnssNative.m4467$$Nest$smnative_start_antenna_info_listening();
        }

        protected boolean stopAntennaInfoListening() {
            return GnssNative.m4474$$Nest$smnative_stop_antenna_info_listening();
        }

        protected boolean isMeasurementSupported() {
            return GnssNative.m4455$$Nest$smnative_is_measurement_supported();
        }

        protected boolean startMeasurementCollection(boolean enableFullTracking, boolean enableCorrVecOutputs, int intervalMillis) {
            return GnssNative.native_start_measurement_collection(enableFullTracking, enableCorrVecOutputs, intervalMillis);
        }

        protected boolean stopMeasurementCollection() {
            return GnssNative.m4476$$Nest$smnative_stop_measurement_collection();
        }

        protected boolean isMeasurementCorrectionsSupported() {
            return GnssNative.m4454$$Nest$smnative_is_measurement_corrections_supported();
        }

        protected boolean injectMeasurementCorrections(GnssMeasurementCorrections corrections) {
            return GnssNative.native_inject_measurement_corrections(corrections);
        }

        protected boolean startSvStatusCollection() {
            return GnssNative.m4472$$Nest$smnative_start_sv_status_collection();
        }

        protected boolean stopSvStatusCollection() {
            return GnssNative.m4479$$Nest$smnative_stop_sv_status_collection();
        }

        protected boolean startNmeaMessageCollection() {
            return GnssNative.m4471$$Nest$smnative_start_nmea_message_collection();
        }

        protected boolean stopNmeaMessageCollection() {
            return GnssNative.m4478$$Nest$smnative_stop_nmea_message_collection();
        }

        protected int getBatchSize() {
            return GnssNative.m4442$$Nest$smnative_get_batch_size();
        }

        protected boolean initBatching() {
            return GnssNative.m4445$$Nest$smnative_init_batching();
        }

        protected void cleanupBatching() {
            GnssNative.native_cleanup_batching();
        }

        protected boolean startBatch(long periodNanos, float minUpdateDistanceMeters, boolean wakeOnFifoFull) {
            return GnssNative.native_start_batch(periodNanos, minUpdateDistanceMeters, wakeOnFifoFull);
        }

        protected void flushBatch() {
            GnssNative.native_flush_batch();
        }

        protected void stopBatch() {
            GnssNative.m4475$$Nest$smnative_stop_batch();
        }

        protected boolean isGeofencingSupported() {
            return GnssNative.m4452$$Nest$smnative_is_geofence_supported();
        }

        protected boolean addGeofence(int geofenceId, double latitude, double longitude, double radius, int lastTransition, int monitorTransitions, int notificationResponsiveness, int unknownTimer) {
            return GnssNative.native_add_geofence(geofenceId, latitude, longitude, radius, lastTransition, monitorTransitions, notificationResponsiveness, unknownTimer);
        }

        protected boolean resumeGeofence(int geofenceId, int monitorTransitions) {
            return GnssNative.native_resume_geofence(geofenceId, monitorTransitions);
        }

        protected boolean pauseGeofence(int geofenceId) {
            return GnssNative.native_pause_geofence(geofenceId);
        }

        protected boolean removeGeofence(int geofenceId) {
            return GnssNative.native_remove_geofence(geofenceId);
        }

        protected boolean isGnssVisibilityControlSupported() {
            return GnssNative.m4453$$Nest$smnative_is_gnss_visibility_control_supported();
        }

        protected void sendNiResponse(int notificationId, int userResponse) {
            GnssNative.native_send_ni_response(notificationId, userResponse);
        }

        protected void requestPowerStats() {
            GnssNative.native_request_power_stats();
        }

        protected void setAgpsServer(int type, String hostname, int port) {
            GnssNative.native_set_agps_server(type, hostname, port);
        }

        protected void setAgpsSetId(int type, String setId) {
            GnssNative.native_agps_set_id(type, setId);
        }

        protected void setAgpsReferenceLocationCellId(int type, int mcc, int mnc, int lac, long cid, int tac, int pcid, int arfcn) {
            GnssNative.native_agps_set_ref_location_cellid(type, mcc, mnc, lac, cid, tac, pcid, arfcn);
        }

        protected boolean isPsdsSupported() {
            return GnssNative.m4480$$Nest$smnative_supports_psds();
        }

        protected void injectPsdsData(byte[] data, int length, int psdsType) {
            GnssNative.native_inject_psds_data(data, length, psdsType);
        }
    }
}
