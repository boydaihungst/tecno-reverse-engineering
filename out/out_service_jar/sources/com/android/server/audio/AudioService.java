package com.android.server.audio;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IUidObserver;
import android.app.NotificationManager;
import android.app.role.OnRoleHoldersChangedListener;
import android.app.role.RoleManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.SensorPrivacyManager;
import android.hardware.SensorPrivacyManagerInternal;
import android.hardware.hdmi.HdmiAudioSystemClient;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiPlaybackClient;
import android.hardware.hdmi.HdmiTvClient;
import android.hardware.input.InputManager;
import android.hidl.manager.V1_0.IServiceManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioDeviceVolumeManager;
import android.media.AudioFocusInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioManagerInternal;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRecordingConfiguration;
import android.media.AudioRoutesInfo;
import android.media.AudioSystem;
import android.media.BluetoothProfileConnectionInfo;
import android.media.IAudioDeviceVolumeDispatcher;
import android.media.IAudioFocusDispatcher;
import android.media.IAudioModeDispatcher;
import android.media.IAudioRoutesObserver;
import android.media.IAudioServerStateDispatcher;
import android.media.IAudioService;
import android.media.ICapturePresetDevicesRoleDispatcher;
import android.media.ICommunicationDeviceDispatcher;
import android.media.IDeviceVolumeBehaviorDispatcher;
import android.media.IMuteAwaitConnectionCallback;
import android.media.IPlaybackConfigDispatcher;
import android.media.IRecordingConfigDispatcher;
import android.media.IRingtonePlayer;
import android.media.ISpatializerCallback;
import android.media.ISpatializerHeadToSoundStagePoseCallback;
import android.media.ISpatializerHeadTrackerAvailableCallback;
import android.media.ISpatializerHeadTrackingModeCallback;
import android.media.ISpatializerOutputCallback;
import android.media.IStrategyPreferredDevicesDispatcher;
import android.media.IVolumeController;
import android.media.MediaMetrics;
import android.media.PlayerBase;
import android.media.VolumeInfo;
import android.media.VolumePolicy;
import android.media.audiopolicy.AudioMix;
import android.media.audiopolicy.AudioPolicyConfig;
import android.media.audiopolicy.AudioProductStrategy;
import android.media.audiopolicy.AudioVolumeGroup;
import android.media.audiopolicy.IAudioPolicyCallback;
import android.media.projection.IMediaProjection;
import android.media.projection.IMediaProjectionCallback;
import android.media.projection.IMediaProjectionManager;
import android.net.INetd;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HwBinder;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Log;
import android.util.MathUtils;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.UiModeManagerService;
import com.android.server.am.HostingRecord;
import com.android.server.audio.AudioDeviceBroker;
import com.android.server.audio.AudioEventLogger;
import com.android.server.audio.AudioService;
import com.android.server.audio.AudioServiceEvents;
import com.android.server.audio.AudioSystemAdapter;
import com.android.server.audio.SoundEffectsHelper;
import com.android.server.health.HealthServiceWrapperHidl;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.utils.PriorityDump;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.audio.AudioServiceExt;
import com.transsion.hubcore.server.am.ITranActivityManagerService;
import com.transsion.hubcore.server.audio.ITranAudioService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
/* loaded from: classes.dex */
public class AudioService extends IAudioService.Stub implements AccessibilityManager.TouchExplorationStateChangeListener, AccessibilityManager.AccessibilityServicesStateChangeListener, AudioSystemAdapter.OnRoutingUpdatedListener, AudioSystemAdapter.OnVolRangeInitRequestListener {
    private static final String AUDIO_HAL_SERVICE_PREFIX = "android.hardware.audio";
    public static final int BECOMING_NOISY_DELAY_MS = 1000;
    private static final String CALLER_PACKAGE = "com.android.systemui";
    private static final int CHECK_MODE_FOR_UID_PERIOD_MS = 6000;
    static final int CONNECTION_STATE_CONNECTED = 1;
    static final int CONNECTION_STATE_DISCONNECTED = 0;
    protected static final boolean DEBUG_AP;
    protected static final boolean DEBUG_COMM_RTE;
    protected static final boolean DEBUG_DEVICES;
    protected static final boolean DEBUG_MODE;
    protected static final boolean DEBUG_VOL;
    private static final int DEFAULT_STREAM_TYPE_OVERRIDE_DELAY_MS = 0;
    protected static final int DEFAULT_VOL_STREAM_NO_PLAYBACK = 3;
    private static final Set<Integer> DEVICE_MEDIA_UNMUTED_ON_PLUG_SET;
    private static final int FLAG_ADJUST_VOLUME = 1;
    private static final String[] HAL_VERSIONS;
    private static final int INDICATE_SYSTEM_READY_RETRY_DELAY_MS = 1000;
    protected static final boolean LOGD;
    static final int LOG_NB_EVENTS_DEVICE_CONNECTION = 50;
    static final int LOG_NB_EVENTS_DYN_POLICY = 10;
    static final int LOG_NB_EVENTS_FORCE_USE = 20;
    static final int LOG_NB_EVENTS_LIFECYCLE = 20;
    static final int LOG_NB_EVENTS_PHONE_STATE = 20;
    static final int LOG_NB_EVENTS_SPATIAL = 30;
    static final int LOG_NB_EVENTS_VOLUME = 40;
    protected static int[] MAX_STREAM_VOLUME = null;
    protected static final float MIN_ALARM_ATTENUATION_NON_PRIVILEGED_DB = -36.0f;
    protected static int[] MIN_STREAM_VOLUME = null;
    private static final int MSG_ACCESSORY_PLUG_MEDIA_UNMUTE = 21;
    private static final int MSG_ADD_ASSISTANT_SERVICE_UID = 44;
    private static final int MSG_AUDIO_SERVER_DIED = 4;
    private static final int MSG_BROADCAST_MICROPHONE_MUTE = 30;
    private static final int MSG_BT_DEV_CHANGED = 38;
    private static final int MSG_BT_HEADSET_CNCT_FAILED = 9;
    private static final int MSG_CHECK_MODE_FOR_UID = 31;
    private static final int MSG_CHECK_MUSIC_ACTIVE = 11;
    private static final int MSG_CONFIGURE_SAFE_MEDIA_VOLUME = 12;
    private static final int MSG_CONFIGURE_SAFE_MEDIA_VOLUME_FORCED = 13;
    private static final int MSG_DISABLE_AUDIO_FOR_UID = 100;
    private static final int MSG_DISPATCH_AUDIO_MODE = 40;
    private static final int MSG_DISPATCH_AUDIO_SERVER_STATE = 23;
    private static final int MSG_DISPATCH_DEVICE_VOLUME_BEHAVIOR = 47;
    private static final int MSG_DYN_POLICY_MIX_STATE_UPDATE = 19;
    private static final int MSG_ENABLE_SURROUND_FORMATS = 24;
    private static final int MSG_HDMI_VOLUME_CHECK = 28;
    private static final int MSG_INDICATE_SYSTEM_READY = 20;
    private static final int MSG_INIT_DTS = 106;
    private static final int MSG_INIT_HEADTRACKING_SENSORS = 42;
    private static final int MSG_INIT_SPATIALIZER = 102;
    private static final int MSG_INIT_STREAMS_VOLUMES = 101;
    private static final int MSG_LOAD_SOUND_EFFECTS = 7;
    private static final int MSG_NOTIFY_VOL_EVENT = 22;
    private static final int MSG_OBSERVE_DEVICES_FOR_ALL_STREAMS = 27;
    private static final int MSG_PERSIST_MUSIC_ACTIVE_MS = 17;
    private static final int MSG_PERSIST_RINGER_MODE = 3;
    private static final int MSG_PERSIST_SAFE_VOLUME_STATE = 14;
    private static final int MSG_PERSIST_SPATIAL_AUDIO_DEVICE_SETTINGS = 43;
    private static final int MSG_PERSIST_VOLUME = 1;
    private static final int MSG_PERSIST_VOLUME_GROUP = 2;
    private static final int MSG_PLAYBACK_CONFIG_CHANGE = 29;
    private static final int MSG_PLAY_SOUND_EFFECT = 5;
    private static final int MSG_RECORDING_CONFIG_CHANGE = 37;
    private static final int MSG_REINIT_VOLUMES = 34;
    private static final int MSG_REMOVE_ASSISTANT_SERVICE_UID = 45;
    private static final int MSG_REQUEST_PACKAGE_NAME = 105;
    private static final int MSG_ROUTING_UPDATED = 41;
    private static final int MSG_SET_ALL_VOLUMES = 10;
    private static final int MSG_SET_DEVICE_STREAM_VOLUME = 26;
    private static final int MSG_SET_DEVICE_VOLUME = 0;
    private static final int MSG_SET_FORCE_USE = 8;
    private static final int MSG_STREAM_DEVICES_CHANGED = 32;
    private static final int MSG_SYSTEM_READY = 16;
    private static final int MSG_UNLOAD_SOUND_EFFECTS = 15;
    private static final int MSG_UNMUTE_STREAM = 18;
    private static final int MSG_UPDATE_A11Y_SERVICE_UIDS = 35;
    private static final int MSG_UPDATE_ACTIVE_ASSISTANT_SERVICE_UID = 46;
    private static final int MSG_UPDATE_AUDIO_MODE = 36;
    private static final int MSG_UPDATE_RINGER_MODE = 25;
    private static final int MSG_UPDATE_VOLUME_STATES_FOR_DEVICE = 33;
    private static final int MUSIC_ACTIVE_MAX_PERIOD_MS = 120000;
    private static final int MUSIC_ACTIVE_POLL_PERIOD_MS = 60000;
    private static final int[] NO_ACTIVE_ASSISTANT_SERVICE_UIDS;
    private static final int PERSIST_DELAY = 500;
    private static final String[] RINGER_MODE_NAMES;
    private static final int SAFE_MEDIA_VOLUME_ACTIVE = 3;
    private static final int SAFE_MEDIA_VOLUME_DISABLED = 1;
    private static final int SAFE_MEDIA_VOLUME_INACTIVE = 2;
    private static final int SAFE_MEDIA_VOLUME_NOT_CONFIGURED = 0;
    private static final int SAFE_VOLUME_CONFIGURE_TIMEOUT_MS = 30000;
    private static final int SENDMSG_NOOP = 1;
    private static final int SENDMSG_QUEUE = 2;
    private static final int SENDMSG_REPLACE = 0;
    private static final boolean SPATIAL_AUDIO_ENABLED_DEFAULT = true;
    private static final int SPECIAL_MUSIC_VOL_DEFAULT;
    private static final int[] STREAM_VOLUME_OPS;
    private static final String TAG = "AS.AudioService";
    private static final int TOUCH_EXPLORE_STREAM_TYPE_OVERRIDE_DELAY_MS = 1000;
    private static final VibrationAttributes TOUCH_VIBRATION_ATTRIBUTES;
    public static final boolean TRAN_DTS_SUPPORT;
    private static final boolean TRAN_DUAL_MIC_SUPPORT;
    private static final int UNMUTE_STREAM_DELAY = 350;
    private static final String UNSAFE_VOLUME_MUSIC_ACTIVE_CURRENT = "unsafe_volume_music_active_current";
    private static final int UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX = 72000000;
    private static final int UNSET_INDEX = -1;
    private static final int[] VALID_COMMUNICATION_DEVICE_TYPES;
    private static final String mMetricsId = "audio.service.";
    protected static int[] mStreamVolumeAlias;
    static VolumeInfo sDefaultVolumeInfo;
    static final AudioEventLogger sDeviceLogger;
    static final AudioEventLogger sForceUseLogger;
    private static boolean sIndependentA11yVolume;
    static final AudioEventLogger sLifecycleLogger;
    static final AudioEventLogger sSpatialLogger;
    private static int sStreamOverrideDelayMs;
    private static final SparseArray<VolumeGroupState> sVolumeGroupStates;
    static final AudioEventLogger sVolumeLogger;
    private final int[] STREAM_VOLUME_ALIAS_DEFAULT;
    private final int[] STREAM_VOLUME_ALIAS_NONE;
    private final int[] STREAM_VOLUME_ALIAS_TELEVISION;
    private final int[] STREAM_VOLUME_ALIAS_VOICE;
    Set<Integer> mAbsVolumeMultiModeCaseDevices;
    Map<Integer, AbsoluteVolumeDeviceInfo> mAbsoluteVolumeDeviceInfoMap;
    private int[] mAccessibilityServiceUids;
    private final Object mAccessibilityServiceUidsLock;
    private int[] mActiveAssistantServiceUids;
    private final ActivityManagerInternal mActivityManagerInternal;
    private final AppOpsManager mAppOps;
    private final ArraySet<Integer> mAssistantUids;
    private PowerManager.WakeLock mAudioEventWakeLock;
    private AudioHandler mAudioHandler;
    private final HashMap<IBinder, AudioPolicyProxy> mAudioPolicies;
    private int mAudioPolicyCounter;
    private final HashMap<IBinder, AsdProxy> mAudioServerStateListeners;
    private AudioServiceExt mAudioServiceExt;
    private final Object mAudioServiceExtLock;
    private final AudioSystemAdapter mAudioSystem;
    private final AudioSystem.ErrorCallback mAudioSystemCallback;
    private AudioSystemThread mAudioSystemThread;
    private volatile boolean mAvrcpAbsVolSupported;
    private volatile boolean mBleVcAbsVolSupported;
    private boolean mBtScoOnByApp;
    private boolean mCameraSoundForced;
    private ComponentName mComponentName;
    private final ContentResolver mContentResolver;
    final Context mContext;
    private int mCurrentImeUid;
    private final AudioDeviceBroker mDeviceBroker;
    final RemoteCallbackList<IDeviceVolumeBehaviorDispatcher> mDeviceVolumeBehaviorDispatchers;
    private boolean mDockAudioMediaEnabled;
    private int mDockState;
    private final AudioSystem.DynamicPolicyCallback mDynPolicyCallback;
    private final AudioEventLogger mDynPolicyLogger;
    private String mEnabledSurroundFormats;
    private int mEncodedSurroundMode;
    private IAudioPolicyCallback mExtVolumeController;
    private final Object mExtVolumeControllerLock;
    Set<Integer> mFixedVolumeDevices;
    private ForceControlStreamClient mForceControlStreamClient;
    private final Object mForceControlStreamLock;
    Set<Integer> mFullVolumeDevices;
    private final boolean mHasSpatializerEffect;
    private final boolean mHasVibrator;
    private HdmiAudioSystemClient mHdmiAudioSystemClient;
    private boolean mHdmiCecVolumeControlEnabled;
    private final Object mHdmiClientLock;
    private MyHdmiControlStatusChangeListenerCallback mHdmiControlStatusChangeListenerCallback;
    private HdmiControlManager mHdmiManager;
    private HdmiPlaybackClient mHdmiPlaybackClient;
    private boolean mHdmiSystemAudioSupported;
    private HdmiTvClient mHdmiTvClient;
    private boolean mHomeSoundEffectEnabled;
    private int mInputMethodServiceUid;
    private final Object mInputMethodServiceUidLock;
    private Intent mIntentService;
    private boolean mIsCallScreeningModeSupported;
    private final boolean mIsSingleVolume;
    private long mLoweredFromNormalToVibrateTime;
    private int mMcc;
    private final MediaFocusControl mMediaFocusControl;
    private boolean mMicMuteFromApi;
    private boolean mMicMuteFromPrivacyToggle;
    private boolean mMicMuteFromRestrictions;
    private boolean mMicMuteFromSwitch;
    private boolean mMicMuteFromSystemCached;
    private AtomicInteger mMode;
    final RemoteCallbackList<IAudioModeDispatcher> mModeDispatchers;
    private final AudioEventLogger mModeLogger;
    private final boolean mMonitorRotation;
    private int mMusicActiveMs;
    private int mMuteAffectedStreams;
    final RemoteCallbackList<IMuteAwaitConnectionCallback> mMuteAwaitConnectionDispatchers;
    private final Object mMuteAwaitConnectionLock;
    private int[] mMutedUsagesAwaitingConnection;
    private AudioDeviceAttributes mMutingExpectedDevice;
    private MyHdmiCecVolumeControlFeatureListener mMyHdmiCecVolumeControlFeatureListener;
    private boolean mNavigationRepeatSoundEffectsEnabled;
    private NotificationManager mNm;
    private StreamVolumeCommand mPendingVolumeCommand;
    private final int mPlatformType;
    private final PlaybackActivityMonitor mPlaybackMonitor;
    private float[] mPrescaleAbsoluteVolume;
    private int mPrevVolDirection;
    private String mPreviouPackageName;
    private int mPrimaryAssistantUid;
    private IMediaProjectionManager mProjectionService;
    private final BroadcastReceiver mReceiver;
    private final RecordingActivityMonitor mRecordMonitor;
    private RestorableParameters mRestorableParameters;
    private int mRingerAndZenModeMutedStreams;
    private int mRingerMode;
    private int mRingerModeAffectedStreams;
    private AudioManagerInternal.RingerModeDelegate mRingerModeDelegate;
    private int mRingerModeExternal;
    private volatile IRingtonePlayer mRingtonePlayer;
    private final ArrayList<RmtSbmxFullVolDeathHandler> mRmtSbmxFullVolDeathHandlers;
    private int mRmtSbmxFullVolRefCount;
    RoleObserver mRoleObserver;
    private boolean mRttEnabled;
    Set<Integer> mSafeMediaVolumeDevices;
    private int mSafeMediaVolumeIndex;
    private int mSafeMediaVolumeState;
    private final Object mSafeMediaVolumeStateLock;
    private float mSafeUsbMediaVolumeDbfs;
    private int mSafeUsbMediaVolumeIndex;
    private final SensorPrivacyManagerInternal mSensorPrivacyManagerInternal;
    private final Object mSetMicrophoneDeathHandlerLock;
    private setMicrophoneMuteClient mSetMicrophoneMuteClient;
    final ArrayList<SetModeDeathHandler> mSetModeDeathHandlers;
    private final SettingsAdapter mSettings;
    private final Object mSettingsLock;
    private SettingsObserver mSettingsObserver;
    private SoundEffectsHelper mSfxHelper;
    private final SpatializerHelper mSpatializerHelper;
    private VolumeStreamState[] mStreamStates;
    private int[] mSupportedSystemUsages;
    private final Object mSupportedSystemUsagesLock;
    private boolean mSupportsMicPrivacyToggle;
    private boolean mSurroundModeChanged;
    private boolean mSystemReady;
    private final SystemServerAdapter mSystemServer;
    private final IUidObserver mUidObserver;
    private final boolean mUseFixedVolume;
    private final boolean mUseVolumeGroupAliases;
    private final UserManagerInternal mUserManagerInternal;
    private final UserManagerInternal.UserRestrictionsListener mUserRestrictionsListener;
    private boolean mUserSelectedVolumeControlStream;
    private boolean mUserSwitchedReceived;
    private int mVibrateSetting;
    private Vibrator mVibrator;
    private AtomicBoolean mVoicePlaybackActive;
    private final IPlaybackConfigDispatcher mVoicePlaybackActivityMonitor;
    private final IRecordingConfigDispatcher mVoiceRecordingActivityMonitor;
    private int mVolumeControlStream;
    private final VolumeController mVolumeController;
    private VolumePolicy mVolumePolicy;
    private int mZenModeAffectedStreams;
    private volatile boolean mleCallVcSupportsAbsoluteVolume;
    private volatile boolean mleVcSupportsAbsoluteVolume;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface BtProfile {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface BtProfileConnectionState {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface ConnectionState {
    }

    static {
        boolean z = "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE) || "1".equals(SystemProperties.get("persist.user.root.support", "0")) || "1".equals(SystemProperties.get("persist.sys.fans.support", "0"));
        LOGD = z;
        DEBUG_MODE = z;
        DEBUG_AP = z;
        DEBUG_VOL = z;
        DEBUG_DEVICES = z;
        DEBUG_COMM_RTE = z;
        TRAN_DTS_SUPPORT = "1".equals(SystemProperties.get("ro.tran_dts.support", "0"));
        TRAN_DUAL_MIC_SUPPORT = "1".equals(SystemProperties.get("ro.vendor.tran_dual_mic_support", "false"));
        SPECIAL_MUSIC_VOL_DEFAULT = SystemProperties.getInt("ro.config.special_music_vol_default", -1);
        NO_ACTIVE_ASSISTANT_SERVICE_UIDS = new int[0];
        MAX_STREAM_VOLUME = new int[]{15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15};
        MIN_STREAM_VOLUME = new int[]{1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0};
        STREAM_VOLUME_OPS = new int[]{34, 36, 35, 36, 37, 38, 39, 36, 36, 36, 64, 36};
        TOUCH_VIBRATION_ATTRIBUTES = VibrationAttributes.createForUsage(18);
        VALID_COMMUNICATION_DEVICE_TYPES = new int[]{2, 7, 3, 22, 1, 4, 23, 26, 11, 27, 5, 9, 19};
        HashSet hashSet = new HashSet();
        DEVICE_MEDIA_UNMUTED_ON_PLUG_SET = hashSet;
        hashSet.add(4);
        hashSet.add(8);
        hashSet.add(131072);
        hashSet.addAll(AudioSystem.DEVICE_OUT_ALL_A2DP_SET);
        hashSet.addAll(AudioSystem.DEVICE_OUT_ALL_USB_SET);
        hashSet.add(1024);
        sVolumeGroupStates = new SparseArray<>();
        sIndependentA11yVolume = false;
        sLifecycleLogger = new AudioEventLogger(20, "audio services lifecycle");
        sDeviceLogger = new AudioEventLogger(50, "wired/A2DP/hearing aid device connection");
        sForceUseLogger = new AudioEventLogger(20, "force use (logged before setForceUse() is executed)");
        sVolumeLogger = new AudioEventLogger(40, "volume changes (logged when command received by AudioService)");
        sSpatialLogger = new AudioEventLogger(30, "spatial audio");
        RINGER_MODE_NAMES = new String[]{"SILENT", "VIBRATE", PriorityDump.PRIORITY_ARG_NORMAL};
        HAL_VERSIONS = new String[]{"7.1", "7.0", "6.0", "5.0", "4.0", "2.0"};
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPlatformVoice() {
        return this.mPlatformType == 1;
    }

    boolean isPlatformTelevision() {
        return this.mPlatformType == 2;
    }

    boolean isPlatformAutomotive() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getVssVolumeForDevice(int stream, int device) {
        return this.mStreamStates[stream].getIndex(device);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getMaxVssVolumeForStream(int stream) {
        return this.mStreamStates[stream].getMaxIndex();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class AbsoluteVolumeDeviceInfo {
        private final IAudioDeviceVolumeDispatcher mCallback;
        private final AudioDeviceAttributes mDevice;
        private final boolean mHandlesVolumeAdjustment;
        private final List<VolumeInfo> mVolumeInfos;

        /* JADX DEBUG: Marked for inline */
        /* JADX DEBUG: Method not inlined, still used in: [com.android.server.audio.AudioService.adjustStreamVolume(int, int, int, java.lang.String, java.lang.String, int, int, java.lang.String, boolean, int):void] */
        /* renamed from: -$$Nest$fgetmHandlesVolumeAdjustment  reason: not valid java name */
        static /* bridge */ /* synthetic */ boolean m1916$$Nest$fgetmHandlesVolumeAdjustment(AbsoluteVolumeDeviceInfo absoluteVolumeDeviceInfo) {
            return absoluteVolumeDeviceInfo.mHandlesVolumeAdjustment;
        }

        private AbsoluteVolumeDeviceInfo(AudioDeviceAttributes device, List<VolumeInfo> volumeInfos, IAudioDeviceVolumeDispatcher callback, boolean handlesVolumeAdjustment) {
            this.mDevice = device;
            this.mVolumeInfos = volumeInfos;
            this.mCallback = callback;
            this.mHandlesVolumeAdjustment = handlesVolumeAdjustment;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public VolumeInfo getMatchingVolumeInfoForStream(final int streamType) {
            for (VolumeInfo volumeInfo : this.mVolumeInfos) {
                boolean volumeGroupMatches = true;
                boolean streamTypeMatches = volumeInfo.hasStreamType() && volumeInfo.getStreamType() == streamType;
                volumeGroupMatches = (volumeInfo.hasVolumeGroup() && Arrays.stream(volumeInfo.getVolumeGroup().getLegacyStreamTypes()).anyMatch(new IntPredicate() { // from class: com.android.server.audio.AudioService$AbsoluteVolumeDeviceInfo$$ExternalSyntheticLambda0
                    @Override // java.util.function.IntPredicate
                    public final boolean test(int i) {
                        return AudioService.AbsoluteVolumeDeviceInfo.lambda$getMatchingVolumeInfoForStream$0(streamType, i);
                    }
                })) ? false : false;
                if (!streamTypeMatches) {
                    if (volumeGroupMatches) {
                    }
                }
                return volumeInfo;
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$getMatchingVolumeInfoForStream$0(int streamType, int s) {
            return s == streamType;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class RestorableParameters {
        private Map<String, BooleanSupplier> mMap;

        private RestorableParameters() {
            this.mMap = new LinkedHashMap<String, BooleanSupplier>() { // from class: com.android.server.audio.AudioService.RestorableParameters.1
                private static final int MAX_ENTRIES = 1000;

                /* JADX DEBUG: Method arguments types fixed to match base method, original types: [java.util.Map$Entry] */
                @Override // java.util.LinkedHashMap
                protected boolean removeEldestEntry(Map.Entry<String, BooleanSupplier> entry) {
                    if (size() <= 1000) {
                        return false;
                    }
                    Log.w(AudioService.TAG, "Parameter map exceeds 1000 removing " + ((Object) entry.getKey()));
                    return true;
                }
            };
        }

        public int setParameters(String id, final String parameter) {
            int status;
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(parameter, "parameter must not be null");
            synchronized (this.mMap) {
                status = AudioSystem.setParameters(parameter);
                if (status == 0) {
                    queueRestoreWithRemovalIfTrue(id, new BooleanSupplier() { // from class: com.android.server.audio.AudioService$RestorableParameters$$ExternalSyntheticLambda0
                        @Override // java.util.function.BooleanSupplier
                        public final boolean getAsBoolean() {
                            return AudioService.RestorableParameters.lambda$setParameters$0(parameter);
                        }
                    });
                }
            }
            return status;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$setParameters$0(String parameter) {
            return AudioSystem.setParameters(parameter) != 0;
        }

        public void queueRestoreWithRemovalIfTrue(String id, BooleanSupplier supplier) {
            Objects.requireNonNull(id, "id must not be null");
            synchronized (this.mMap) {
                if (supplier != null) {
                    this.mMap.put(id, supplier);
                } else {
                    this.mMap.remove(id);
                }
            }
        }

        public void restoreAll() {
            synchronized (this.mMap) {
                this.mMap.values().removeIf(new Predicate() { // from class: com.android.server.audio.AudioService$RestorableParameters$$ExternalSyntheticLambda1
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        boolean asBoolean;
                        asBoolean = ((BooleanSupplier) obj).getAsBoolean();
                        return asBoolean;
                    }
                });
            }
        }
    }

    public static String makeAlsaAddressString(int card, int device) {
        return "card=" + card + ";device=" + device + ";";
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private AudioService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new AudioService(context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            publishBinderService("audio", this.mService);
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (phase == 550) {
                this.mService.systemReady();
            }
        }
    }

    public AudioService(Context context) {
        this(context, AudioSystemAdapter.getDefaultAdapter(), SystemServerAdapter.getDefaultAdapter(context), SettingsAdapter.getDefaultAdapter(), null);
    }

    public AudioService(Context context, AudioSystemAdapter audioSystem, SystemServerAdapter systemServer, SettingsAdapter settings, Looper looper) {
        this.mVolumeController = new VolumeController();
        this.mMode = new AtomicInteger(0);
        this.mSettingsLock = new Object();
        this.STREAM_VOLUME_ALIAS_VOICE = new int[]{0, 2, 2, 3, 4, 5, 6, 2, 2, 3, 3, 3};
        this.STREAM_VOLUME_ALIAS_TELEVISION = new int[]{3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3};
        this.STREAM_VOLUME_ALIAS_NONE = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        this.STREAM_VOLUME_ALIAS_DEFAULT = new int[]{0, 2, 2, 3, 4, 5, 6, 2, 2, 3, 3, 3};
        this.mAvrcpAbsVolSupported = false;
        this.mBleVcAbsVolSupported = false;
        AudioSystem.ErrorCallback errorCallback = new AudioSystem.ErrorCallback() { // from class: com.android.server.audio.AudioService.1
            public void onError(int error) {
                switch (error) {
                    case 100:
                        if (AudioService.this.mRecordMonitor != null) {
                            AudioService.this.mRecordMonitor.onAudioServerDied();
                        }
                        AudioService.sendMsg(AudioService.this.mAudioHandler, 4, 1, 0, 0, null, 0);
                        AudioService.sendMsg(AudioService.this.mAudioHandler, 23, 2, 0, 0, null, 0);
                        return;
                    default:
                        return;
                }
            }
        };
        this.mAudioSystemCallback = errorCallback;
        this.mRingerModeExternal = -1;
        this.mRingerModeAffectedStreams = 0;
        this.mZenModeAffectedStreams = 0;
        this.mReceiver = new AudioServiceBroadcastReceiver();
        AudioServiceUserRestrictionsListener audioServiceUserRestrictionsListener = new AudioServiceUserRestrictionsListener();
        this.mUserRestrictionsListener = audioServiceUserRestrictionsListener;
        this.mSetModeDeathHandlers = new ArrayList<>();
        this.mPrevVolDirection = 0;
        this.mVolumeControlStream = -1;
        this.mUserSelectedVolumeControlStream = false;
        this.mSetMicrophoneDeathHandlerLock = new Object();
        this.mSetMicrophoneMuteClient = null;
        this.mForceControlStreamLock = new Object();
        this.mForceControlStreamClient = null;
        this.mFixedVolumeDevices = new HashSet(Arrays.asList(2048, 2097152));
        this.mFullVolumeDevices = new HashSet(Arrays.asList(262144, 262145));
        this.mAbsoluteVolumeDeviceInfoMap = new ArrayMap();
        this.mAbsVolumeMultiModeCaseDevices = new HashSet(Arrays.asList(134217728));
        this.mDockAudioMediaEnabled = true;
        this.mRestorableParameters = new RestorableParameters();
        this.mDockState = 0;
        this.mPrescaleAbsoluteVolume = new float[]{0.6f, 0.8f, 0.9f};
        this.mVolumePolicy = VolumePolicy.DEFAULT;
        this.mAssistantUids = new ArraySet<>();
        this.mPrimaryAssistantUid = -1;
        this.mActiveAssistantServiceUids = NO_ACTIVE_ASSISTANT_SERVICE_UIDS;
        this.mAccessibilityServiceUidsLock = new Object();
        this.mInputMethodServiceUid = -1;
        this.mInputMethodServiceUidLock = new Object();
        this.mSupportedSystemUsagesLock = new Object();
        this.mSupportedSystemUsages = new int[]{17};
        this.mUidObserver = new IUidObserver.Stub() { // from class: com.android.server.audio.AudioService.2
            public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) {
            }

            public void onUidGone(int uid, boolean disabled) {
                disableAudioForUid(false, uid);
            }

            public void onUidActive(int uid) throws RemoteException {
            }

            public void onUidIdle(int uid, boolean disabled) {
            }

            public void onUidCachedChanged(int uid, boolean cached) {
                disableAudioForUid(cached, uid);
            }

            public void onUidProcAdjChanged(int uid) {
            }

            private void disableAudioForUid(boolean disable, int uid) {
                AudioService audioService = AudioService.this;
                audioService.queueMsgUnderWakeLock(audioService.mAudioHandler, 100, disable ? 1 : 0, uid, null, 0);
            }
        };
        this.mRttEnabled = false;
        this.mVoicePlaybackActive = new AtomicBoolean(false);
        IPlaybackConfigDispatcher iPlaybackConfigDispatcher = new IPlaybackConfigDispatcher.Stub() { // from class: com.android.server.audio.AudioService.3
            public void dispatchPlaybackConfigChange(List<AudioPlaybackConfiguration> configs, boolean flush) {
                AudioService.sendMsg(AudioService.this.mAudioHandler, 29, 0, 0, 0, configs, 0);
            }
        };
        this.mVoicePlaybackActivityMonitor = iPlaybackConfigDispatcher;
        IRecordingConfigDispatcher iRecordingConfigDispatcher = new IRecordingConfigDispatcher.Stub() { // from class: com.android.server.audio.AudioService.4
            public void dispatchRecordingConfigChange(List<AudioRecordingConfiguration> configs) {
                AudioService.sendMsg(AudioService.this.mAudioHandler, 37, 0, 0, 0, configs, 0);
            }
        };
        this.mVoiceRecordingActivityMonitor = iRecordingConfigDispatcher;
        this.mRmtSbmxFullVolRefCount = 0;
        this.mRmtSbmxFullVolDeathHandlers = new ArrayList<>();
        this.mIsCallScreeningModeSupported = false;
        this.mModeDispatchers = new RemoteCallbackList<>();
        this.mMuteAwaitConnectionLock = new Object();
        this.mMuteAwaitConnectionDispatchers = new RemoteCallbackList<>();
        this.mDeviceVolumeBehaviorDispatchers = new RemoteCallbackList<>();
        this.mSafeMediaVolumeStateLock = new Object();
        this.mMcc = 0;
        this.mSafeMediaVolumeDevices = new HashSet(Arrays.asList(4, 8, 67108864));
        this.mHdmiClientLock = new Object();
        this.mHdmiSystemAudioSupported = false;
        this.mHdmiControlStatusChangeListenerCallback = new MyHdmiControlStatusChangeListenerCallback();
        this.mMyHdmiCecVolumeControlFeatureListener = new MyHdmiCecVolumeControlFeatureListener();
        this.mModeLogger = new AudioEventLogger(20, "phone state (logged after successful call to AudioSystem.setPhoneState(int, int))");
        this.mDynPolicyLogger = new AudioEventLogger(10, "dynamic policy events (logged when command received by AudioService)");
        this.mExtVolumeControllerLock = new Object();
        this.mDynPolicyCallback = new AudioSystem.DynamicPolicyCallback() { // from class: com.android.server.audio.AudioService.6
            public void onDynamicPolicyMixStateUpdate(String regId, int state) {
                if (!TextUtils.isEmpty(regId)) {
                    AudioService.sendMsg(AudioService.this.mAudioHandler, 19, 2, state, 0, regId, 0);
                }
            }
        };
        this.mAudioServerStateListeners = new HashMap<>();
        this.mAudioPolicies = new HashMap<>();
        this.mAudioPolicyCounter = 0;
        this.mAudioServiceExtLock = new Object();
        this.mleVcSupportsAbsoluteVolume = false;
        this.mleCallVcSupportsAbsoluteVolume = false;
        sLifecycleLogger.log(new AudioEventLogger.StringEvent("AudioService()"));
        this.mContext = context;
        ContentResolver contentResolver = context.getContentResolver();
        this.mContentResolver = contentResolver;
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mAudioSystem = audioSystem;
        this.mSystemServer = systemServer;
        this.mSettings = settings;
        this.mPlatformType = AudioSystem.getPlatformType(context);
        this.mIsSingleVolume = AudioSystem.isSingleVolume(context);
        UserManagerInternal userManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        this.mUserManagerInternal = userManagerInternal;
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mSensorPrivacyManagerInternal = (SensorPrivacyManagerInternal) LocalServices.getService(SensorPrivacyManagerInternal.class);
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mAudioEventWakeLock = pm.newWakeLock(1, "handleAudioEvent");
        this.mSfxHelper = new SoundEffectsHelper(context);
        this.mSpatializerHelper = new SpatializerHelper(this, audioSystem);
        Vibrator vibrator = (Vibrator) context.getSystemService("vibrator");
        this.mVibrator = vibrator;
        this.mHasVibrator = vibrator == null ? false : vibrator.hasVibrator();
        this.mSupportsMicPrivacyToggle = ((SensorPrivacyManager) context.getSystemService(SensorPrivacyManager.class)).supportsSensorToggle(1);
        this.mUseVolumeGroupAliases = context.getResources().getBoolean(17891675);
        int i = SPECIAL_MUSIC_VOL_DEFAULT;
        if (i >= 0 && i <= 15) {
            AudioSystem.DEFAULT_STREAM_VOLUME[3] = i;
        }
        if (looper != null) {
            this.mAudioHandler = new AudioHandler(looper);
        } else {
            createAudioSystemThread();
        }
        AudioSystem.setErrorCallback(errorCallback);
        updateAudioHalPids();
        boolean cameraSoundForced = readCameraSoundForced();
        this.mCameraSoundForced = new Boolean(cameraSoundForced).booleanValue();
        sendMsg(this.mAudioHandler, 8, 2, 4, cameraSoundForced ? 11 : 0, new String("AudioService ctor"), 0);
        this.mSafeMediaVolumeState = settings.getGlobalInt(contentResolver, "audio_safe_volume_state", 0);
        this.mSafeMediaVolumeIndex = context.getResources().getInteger(17694927) * 10;
        this.mUseFixedVolume = context.getResources().getBoolean(17891812);
        this.mDeviceBroker = new AudioDeviceBroker(context, this);
        RecordingActivityMonitor recordingActivityMonitor = new RecordingActivityMonitor(context);
        this.mRecordMonitor = recordingActivityMonitor;
        recordingActivityMonitor.registerRecordingCallback(iRecordingConfigDispatcher, true);
        updateStreamVolumeAlias(false, TAG);
        readPersistedSettings();
        readUserRestrictions();
        PlaybackActivityMonitor playbackActivityMonitor = new PlaybackActivityMonitor(context, MAX_STREAM_VOLUME[4], new Consumer() { // from class: com.android.server.audio.AudioService$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                AudioService.this.m1910lambda$new$0$comandroidserveraudioAudioService((AudioDeviceAttributes) obj);
            }
        });
        this.mPlaybackMonitor = playbackActivityMonitor;
        playbackActivityMonitor.registerPlaybackCallback(iPlaybackConfigDispatcher, true);
        this.mMediaFocusControl = new MediaFocusControl(context, playbackActivityMonitor);
        readAndSetLowRamDevice();
        this.mIsCallScreeningModeSupported = AudioSystem.isCallScreeningModeSupported();
        if (systemServer.isPrivileged()) {
            LocalServices.addService(AudioManagerInternal.class, new AudioServiceInternal());
            userManagerInternal.addUserRestrictionsListener(audioServiceUserRestrictionsListener);
            recordingActivityMonitor.initMonitor();
        }
        this.mMonitorRotation = SystemProperties.getBoolean("ro.audio.monitorRotation", false);
        this.mHasSpatializerEffect = SystemProperties.getBoolean("ro.audio.spatializer_enabled", false);
        AudioSystemAdapter.setRoutingListener(this);
        AudioSystemAdapter.setVolRangeInitReqListener(this);
        queueMsgUnderWakeLock(this.mAudioHandler, 101, 0, 0, null, 0);
        queueMsgUnderWakeLock(this.mAudioHandler, 102, 0, 0, null, 0);
        if (!this.mSafeMediaVolumeDevices.contains(16)) {
            this.mSafeMediaVolumeDevices.add(16);
            this.mSafeMediaVolumeDevices.add(32);
            this.mSafeMediaVolumeDevices.add(128);
            this.mSafeMediaVolumeDevices.add(256);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onInitStreamsAndVolumes() {
        createStreamStates();
        initVolumeGroupStates();
        this.mSafeUsbMediaVolumeIndex = getSafeUsbMediaVolumeIndex();
        this.mRingerAndZenModeMutedStreams = 0;
        setRingerModeInt(getRingerModeInternal(), false);
        float[] preScale = {this.mContext.getResources().getFraction(18022403, 1, 1), this.mContext.getResources().getFraction(18022404, 1, 1), this.mContext.getResources().getFraction(18022405, 1, 1)};
        for (int i = 0; i < preScale.length; i++) {
            if (0.0f <= preScale[i] && preScale[i] <= 1.0f) {
                this.mPrescaleAbsoluteVolume[i] = preScale[i];
            }
        }
        if (TRAN_DTS_SUPPORT) {
            sendMsg(this.mAudioHandler, 106, 2, 0, 0, null, 0);
        }
        getAudioServiceExtInstance();
        initExternalEventReceivers();
        checkVolumeRangeInitialization("AudioService()");
    }

    private void initExternalEventReceivers() {
        this.mSettingsObserver = new SettingsObserver();
        IntentFilter intentFilter = new IntentFilter("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED");
        intentFilter.addAction("android.intent.action.DOCK_EVENT");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_BACKGROUND");
        intentFilter.addAction("android.intent.action.USER_FOREGROUND");
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        intentFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        if (this.mMonitorRotation) {
            RotationHelper.init(this.mContext, this.mAudioHandler);
        }
        intentFilter.addAction("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION");
        intentFilter.addAction("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION");
        getAudioServiceExtInstance().getBleIntentFilters(intentFilter);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter, null, null, 2);
    }

    public void systemReady() {
        sendMsg(this.mAudioHandler, 16, 2, 0, 0, null, 0);
    }

    private void updateVibratorInfos() {
        VibratorManager vibratorManager = (VibratorManager) this.mContext.getSystemService(VibratorManager.class);
        if (vibratorManager == null) {
            Slog.e(TAG, "Vibrator manager is not found");
            return;
        }
        int[] vibratorIds = vibratorManager.getVibratorIds();
        if (vibratorIds.length == 0) {
            Slog.d(TAG, "No vibrator found");
            return;
        }
        List<Vibrator> vibrators = new ArrayList<>(vibratorIds.length);
        for (int id : vibratorIds) {
            Vibrator vibrator = vibratorManager.getVibrator(id);
            if (vibrator == null) {
                Slog.w(TAG, "Vibrator(" + id + ") is not found");
            } else {
                vibrators.add(vibrator);
            }
        }
        if (vibrators.isEmpty()) {
            Slog.w(TAG, "Cannot find any available vibrator");
        } else {
            AudioSystem.setVibratorInfos(vibrators);
        }
    }

    public void onSystemReady() {
        this.mSystemReady = true;
        scheduleLoadSoundEffects();
        this.mDeviceBroker.onSystemReady();
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.hdmi.cec")) {
            synchronized (this.mHdmiClientLock) {
                HdmiControlManager hdmiControlManager = (HdmiControlManager) this.mContext.getSystemService(HdmiControlManager.class);
                this.mHdmiManager = hdmiControlManager;
                if (hdmiControlManager != null) {
                    hdmiControlManager.addHdmiControlStatusChangeListener(this.mHdmiControlStatusChangeListenerCallback);
                    this.mHdmiManager.addHdmiCecVolumeControlFeatureListener(this.mContext.getMainExecutor(), this.mMyHdmiCecVolumeControlFeatureListener);
                }
                HdmiTvClient tvClient = this.mHdmiManager.getTvClient();
                this.mHdmiTvClient = tvClient;
                if (tvClient != null) {
                    this.mFixedVolumeDevices.removeAll(AudioSystem.DEVICE_ALL_HDMI_SYSTEM_AUDIO_AND_SPEAKER_SET);
                }
                this.mHdmiPlaybackClient = this.mHdmiManager.getPlaybackClient();
                this.mHdmiAudioSystemClient = this.mHdmiManager.getAudioSystemClient();
            }
        }
        if (this.mSupportsMicPrivacyToggle) {
            this.mSensorPrivacyManagerInternal.addSensorPrivacyListenerForAllUsers(1, new SensorPrivacyManagerInternal.OnUserSensorPrivacyChangedListener() { // from class: com.android.server.audio.AudioService$$ExternalSyntheticLambda3
                public final void onSensorPrivacyChanged(int i, boolean z) {
                    AudioService.this.m1911lambda$onSystemReady$1$comandroidserveraudioAudioService(i, z);
                }
            });
        }
        this.mNm = (NotificationManager) this.mContext.getSystemService("notification");
        sendMsg(this.mAudioHandler, 13, 0, 0, 0, TAG, SystemProperties.getBoolean("audio.safemedia.bypass", false) ? 0 : 30000);
        initA11yMonitoring();
        RoleObserver roleObserver = new RoleObserver();
        this.mRoleObserver = roleObserver;
        roleObserver.register();
        onIndicateSystemReady();
        this.mMicMuteFromSystemCached = this.mAudioSystem.isMicrophoneMuted();
        setMicMuteFromSwitchInput();
        initMinStreamVolumeWithoutModifyAudioSettings();
        updateVibratorInfos();
        getAudioServiceExtInstance().onSystemReadyExt();
        synchronized (this.mSupportedSystemUsagesLock) {
            AudioSystem.setSupportedSystemUsages(this.mSupportedSystemUsages);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSystemReady$1$com-android-server-audio-AudioService  reason: not valid java name */
    public /* synthetic */ void m1911lambda$onSystemReady$1$comandroidserveraudioAudioService(int userId, boolean enabled) {
        if (userId == getCurrentUserId()) {
            this.mMicMuteFromPrivacyToggle = enabled;
            setMicrophoneMuteNoCallerCheck(getCurrentUserId());
        }
    }

    @Override // com.android.server.audio.AudioSystemAdapter.OnRoutingUpdatedListener
    public void onRoutingUpdatedFromNative() {
        sendMsg(this.mAudioHandler, 41, 0, 0, 0, null, 0);
    }

    void onRoutingUpdatedFromAudioThread() {
        if (this.mHasSpatializerEffect) {
            this.mSpatializerHelper.onRoutingUpdated();
        }
        checkMuteAwaitConnection();
    }

    @Override // com.android.server.audio.AudioSystemAdapter.OnVolRangeInitRequestListener
    public void onVolumeRangeInitRequestFromNative() {
        sendMsg(this.mAudioHandler, 34, 0, 0, 0, "onVolumeRangeInitRequestFromNative", 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class RoleObserver implements OnRoleHoldersChangedListener {
        private final Executor mExecutor;
        private RoleManager mRm;

        RoleObserver() {
            this.mExecutor = AudioService.this.mContext.getMainExecutor();
        }

        public void register() {
            RoleManager roleManager = (RoleManager) AudioService.this.mContext.getSystemService("role");
            this.mRm = roleManager;
            if (roleManager != null) {
                roleManager.addOnRoleHoldersChangedListenerAsUser(this.mExecutor, this, UserHandle.ALL);
                synchronized (AudioService.this.mSettingsLock) {
                    AudioService.this.updateAssistantUIdLocked(true);
                }
            }
        }

        public void onRoleHoldersChanged(String roleName, UserHandle user) {
            if ("android.app.role.ASSISTANT".equals(roleName)) {
                synchronized (AudioService.this.mSettingsLock) {
                    AudioService.this.updateAssistantUIdLocked(false);
                }
            }
        }

        public String getAssistantRoleHolder() {
            RoleManager roleManager = this.mRm;
            if (roleManager == null) {
                return "";
            }
            List<String> assistants = roleManager.getRoleHolders("android.app.role.ASSISTANT");
            String assitantPackage = assistants.size() == 0 ? "" : assistants.get(0);
            return assitantPackage;
        }
    }

    void onIndicateSystemReady() {
        if (AudioSystem.systemReady() == 0) {
            return;
        }
        sendMsg(this.mAudioHandler, 20, 0, 0, 0, null, 1000);
    }

    public void onAudioServerDied() {
        int forSys;
        if (!this.mSystemReady || AudioSystem.checkAudioFlinger() != 0) {
            Log.e(TAG, "Audioserver died.");
            sLifecycleLogger.log(new AudioEventLogger.StringEvent("onAudioServerDied() audioserver died"));
            sendMsg(this.mAudioHandler, 4, 1, 0, 0, null, 500);
            return;
        }
        Log.i(TAG, "Audioserver started.");
        sLifecycleLogger.log(new AudioEventLogger.StringEvent("onAudioServerDied() audioserver started"));
        updateAudioHalPids();
        AudioSystem.setParameters("restarting=true");
        readAndSetLowRamDevice();
        this.mIsCallScreeningModeSupported = AudioSystem.isCallScreeningModeSupported();
        this.mDeviceBroker.onAudioServerDied();
        synchronized (this.mDeviceBroker.mSetModeLock) {
            onUpdateAudioMode(-1, Process.myPid(), this.mContext.getPackageName(), true);
        }
        synchronized (this.mSettingsLock) {
            forSys = this.mCameraSoundForced ? 11 : 0;
        }
        this.mDeviceBroker.setForceUse_Async(4, forSys, "onAudioServerDied");
        onReinitVolumes("after audioserver restart");
        restoreVolumeGroups();
        updateMasterMono(this.mContentResolver);
        updateMasterBalance(this.mContentResolver);
        setRingerModeInt(getRingerModeInternal(), false);
        if (this.mMonitorRotation) {
            RotationHelper.updateOrientation();
        }
        this.mRestorableParameters.restoreAll();
        synchronized (this.mSettingsLock) {
            int forDock = this.mDockAudioMediaEnabled ? 8 : 0;
            this.mDeviceBroker.setForceUse_Async(3, forDock, "onAudioServerDied");
            sendEncodedSurroundMode(this.mContentResolver, "onAudioServerDied");
            sendEnabledSurroundFormats(this.mContentResolver, true);
            AudioSystem.setRttEnabled(this.mRttEnabled);
            resetAssistantServicesUidsLocked();
        }
        synchronized (this.mAccessibilityServiceUidsLock) {
            AudioSystem.setA11yServicesUids(this.mAccessibilityServiceUids);
        }
        synchronized (this.mInputMethodServiceUidLock) {
            this.mAudioSystem.setCurrentImeUid(this.mInputMethodServiceUid);
        }
        synchronized (this.mHdmiClientLock) {
            if (this.mHdmiManager != null && this.mHdmiTvClient != null) {
                setHdmiSystemAudioSupported(this.mHdmiSystemAudioSupported);
            }
        }
        synchronized (this.mSupportedSystemUsagesLock) {
            AudioSystem.setSupportedSystemUsages(this.mSupportedSystemUsages);
        }
        synchronized (this.mAudioPolicies) {
            for (AudioPolicyProxy policy : this.mAudioPolicies.values()) {
                int status = policy.connectMixes();
                if (status != 0) {
                    Log.e(TAG, "onAudioServerDied: error " + AudioSystem.audioSystemErrorToString(status) + " when connecting mixes for policy " + policy.toLogFriendlyString());
                    policy.release();
                } else {
                    int deviceAffinitiesStatus = policy.setupDeviceAffinities();
                    if (deviceAffinitiesStatus != 0) {
                        Log.e(TAG, "onAudioServerDied: error " + AudioSystem.audioSystemErrorToString(deviceAffinitiesStatus) + " when connecting device affinities for policy " + policy.toLogFriendlyString());
                        policy.release();
                    }
                }
            }
        }
        synchronized (this.mPlaybackMonitor) {
            HashMap<Integer, Integer> allowedCapturePolicies = this.mPlaybackMonitor.getAllAllowedCapturePolicies();
            for (Map.Entry<Integer, Integer> entry : allowedCapturePolicies.entrySet()) {
                int result = this.mAudioSystem.setAllowedCapturePolicy(entry.getKey().intValue(), AudioAttributes.capturePolicyToFlags(entry.getValue().intValue(), 0));
                if (result != 0) {
                    Log.e(TAG, "Failed to restore capture policy, uid: " + entry.getKey() + ", capture policy: " + entry.getValue() + ", result: " + result);
                    this.mPlaybackMonitor.setAllowedCapturePolicy(entry.getKey().intValue(), 1);
                }
            }
        }
        this.mSpatializerHelper.reset(this.mHasSpatializerEffect);
        onIndicateSystemReady();
        AudioSystem.setParameters("restarting=false");
        sendMsg(this.mAudioHandler, 23, 2, 1, 0, null, 0);
        setMicrophoneMuteNoCallerCheck(getCurrentUserId());
        setMicMuteFromSwitchInput();
        updateVibratorInfos();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onRemoveAssistantServiceUids(int[] uids) {
        synchronized (this.mSettingsLock) {
            removeAssistantServiceUidsLocked(uids);
        }
    }

    private void removeAssistantServiceUidsLocked(int[] uids) {
        boolean changed = false;
        for (int index = 0; index < uids.length; index++) {
            if (!this.mAssistantUids.remove(Integer.valueOf(uids[index]))) {
                Slog.e(TAG, TextUtils.formatSimple("Cannot remove assistant service, uid(%d) not present", new Object[]{Integer.valueOf(uids[index])}));
            } else {
                changed = true;
            }
        }
        if (changed) {
            updateAssistantServicesUidsLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAddAssistantServiceUids(int[] uids) {
        synchronized (this.mSettingsLock) {
            addAssistantServiceUidsLocked(uids);
        }
    }

    private void addAssistantServiceUidsLocked(int[] uids) {
        boolean changed = false;
        for (int index = 0; index < uids.length; index++) {
            if (uids[index] != -1) {
                if (!this.mAssistantUids.add(Integer.valueOf(uids[index]))) {
                    Slog.e(TAG, TextUtils.formatSimple("Cannot add assistant service, uid(%d) already present", new Object[]{Integer.valueOf(uids[index])}));
                } else {
                    changed = true;
                }
            }
        }
        if (changed) {
            updateAssistantServicesUidsLocked();
        }
    }

    private void resetAssistantServicesUidsLocked() {
        this.mAssistantUids.clear();
        updateAssistantUIdLocked(true);
    }

    private void updateAssistantServicesUidsLocked() {
        int[] assistantUids = this.mAssistantUids.stream().mapToInt(new AudioService$$ExternalSyntheticLambda1()).toArray();
        AudioSystem.setAssistantServicesUids(assistantUids);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateActiveAssistantServiceUids() {
        int[] activeAssistantServiceUids;
        synchronized (this.mSettingsLock) {
            activeAssistantServiceUids = this.mActiveAssistantServiceUids;
        }
        AudioSystem.setActiveAssistantServicesUids(activeAssistantServiceUids);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onReinitVolumes(String caller) {
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        int status = 0;
        int streamType = numStreamTypes - 1;
        while (true) {
            if (streamType < 0) {
                break;
            }
            VolumeStreamState streamState = this.mStreamStates[streamType];
            int res = AudioSystem.initStreamVolume(streamType, streamState.mIndexMin / 10, streamState.mIndexMax / 10);
            if (res != 0) {
                status = res;
                Log.e(TAG, "Failed to initStreamVolume (" + res + ") for stream " + streamType);
                break;
            }
            streamState.applyAllVolumes();
            streamType--;
        }
        if (status != 0) {
            sLifecycleLogger.log(new AudioEventLogger.StringEvent(caller + ": initStreamVolume failed with " + status + " will retry").printLog(1, TAG));
            sendMsg(this.mAudioHandler, 34, 1, 0, 0, caller, 2000);
        } else if (!checkVolumeRangeInitialization(caller)) {
        } else {
            sLifecycleLogger.log(new AudioEventLogger.StringEvent(caller + ": initStreamVolume succeeded").printLog(0, TAG));
        }
    }

    private boolean checkVolumeRangeInitialization(String caller) {
        boolean success = true;
        int[] basicStreams = {4, 2, 3, 0, 10};
        for (int streamType : basicStreams) {
            AudioAttributes aa = new AudioAttributes.Builder().setInternalLegacyStreamType(streamType).build();
            if (AudioSystem.getMaxVolumeIndexForAttributes(aa) < 0 || AudioSystem.getMinVolumeIndexForAttributes(aa) < 0) {
                success = false;
                break;
            }
        }
        if (!success) {
            sLifecycleLogger.log(new AudioEventLogger.StringEvent(caller + ": initStreamVolume succeeded but invalid mix/max levels, will retry").printLog(2, TAG));
            sendMsg(this.mAudioHandler, 34, 1, 0, 0, caller, 2000);
        }
        return success;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onDispatchAudioServerStateChange(boolean state) {
        synchronized (this.mAudioServerStateListeners) {
            for (AsdProxy asdp : this.mAudioServerStateListeners.values()) {
                try {
                    asdp.callback().dispatchAudioServerStateChange(state);
                } catch (RemoteException e) {
                    Log.w(TAG, "Could not call dispatchAudioServerStateChange()", e);
                }
            }
        }
    }

    private void createAudioSystemThread() {
        AudioSystemThread audioSystemThread = new AudioSystemThread();
        this.mAudioSystemThread = audioSystemThread;
        audioSystemThread.start();
        waitForAudioHandlerCreation();
    }

    private void waitForAudioHandlerCreation() {
        synchronized (this) {
            while (this.mAudioHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting on volume handler.");
                }
            }
        }
    }

    public void setSupportedSystemUsages(int[] systemUsages) {
        enforceModifyAudioRoutingPermission();
        verifySystemUsages(systemUsages);
        synchronized (this.mSupportedSystemUsagesLock) {
            AudioSystem.setSupportedSystemUsages(systemUsages);
            this.mSupportedSystemUsages = systemUsages;
        }
    }

    public int[] getSupportedSystemUsages() {
        int[] copyOf;
        enforceModifyAudioRoutingPermission();
        synchronized (this.mSupportedSystemUsagesLock) {
            int[] iArr = this.mSupportedSystemUsages;
            copyOf = Arrays.copyOf(iArr, iArr.length);
        }
        return copyOf;
    }

    private void verifySystemUsages(int[] systemUsages) {
        for (int i = 0; i < systemUsages.length; i++) {
            if (!AudioAttributes.isSystemUsage(systemUsages[i])) {
                throw new IllegalArgumentException("Non-system usage provided: " + systemUsages[i]);
            }
        }
    }

    public List<AudioProductStrategy> getAudioProductStrategies() {
        enforceModifyAudioRoutingPermission();
        return AudioProductStrategy.getAudioProductStrategies();
    }

    public List<AudioVolumeGroup> getAudioVolumeGroups() {
        enforceModifyAudioRoutingPermission();
        return AudioVolumeGroup.getAudioVolumeGroups();
    }

    private void checkAllAliasStreamVolumes() {
        synchronized (this.mSettingsLock) {
            synchronized (VolumeStreamState.class) {
                int numStreamTypes = AudioSystem.getNumStreamTypes();
                for (int streamType = 0; streamType < numStreamTypes; streamType++) {
                    VolumeStreamState[] volumeStreamStateArr = this.mStreamStates;
                    volumeStreamStateArr[streamType].setAllIndexes(volumeStreamStateArr[mStreamVolumeAlias[streamType]], TAG);
                    if (!this.mStreamStates[streamType].mIsMuted) {
                        this.mStreamStates[streamType].applyAllVolumes();
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postCheckVolumeCecOnHdmiConnection(int state, String caller) {
        sendMsg(this.mAudioHandler, 28, 0, state, 0, caller, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCheckVolumeCecOnHdmiConnection(int state, String caller) {
        if (state == 1) {
            if (this.mSafeMediaVolumeDevices.contains(1024)) {
                sendMsg(this.mAudioHandler, 11, 0, 0, 0, caller, 60000);
            }
            if (isPlatformTelevision()) {
                synchronized (this.mHdmiClientLock) {
                    if (this.mHdmiManager != null && this.mHdmiPlaybackClient != null) {
                        updateHdmiCecSinkLocked(this.mFullVolumeDevices.contains(1024));
                    }
                }
            }
            sendEnabledSurroundFormats(this.mContentResolver, true);
        } else if (isPlatformTelevision()) {
            synchronized (this.mHdmiClientLock) {
                if (this.mHdmiManager != null) {
                    updateHdmiCecSinkLocked(this.mFullVolumeDevices.contains(1024));
                }
            }
        }
    }

    private void postUpdateVolumeStatesForAudioDevice(int device, String caller) {
        sendMsg(this.mAudioHandler, 33, 2, device, 0, caller, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUpdateVolumeStatesForAudioDevice(int device, String caller) {
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        synchronized (this.mSettingsLock) {
            synchronized (VolumeStreamState.class) {
                for (int streamType = 0; streamType < numStreamTypes; streamType++) {
                    updateVolumeStates(device, streamType, caller);
                }
            }
        }
    }

    private void updateVolumeStates(int device, int streamType, String caller) {
        if (device == 4194304) {
            device = 2;
        }
        if (!this.mStreamStates[streamType].hasIndexForDevice(device)) {
            VolumeStreamState[] volumeStreamStateArr = this.mStreamStates;
            volumeStreamStateArr[streamType].setIndex(volumeStreamStateArr[mStreamVolumeAlias[streamType]].getIndex(1073741824), device, caller, true);
        }
        List<AudioDeviceAttributes> devicesForAttributes = getDevicesForAttributesInt(new AudioAttributes.Builder().setInternalLegacyStreamType(streamType).build(), true);
        for (AudioDeviceAttributes deviceAttributes : devicesForAttributes) {
            if (deviceAttributes.getType() == AudioDeviceInfo.convertInternalDeviceToDeviceType(device)) {
                this.mStreamStates[streamType].checkFixedVolumeDevices();
                if (isStreamMute(streamType) && this.mFullVolumeDevices.contains(Integer.valueOf(device))) {
                    this.mStreamStates[streamType].mute(false);
                }
            }
        }
    }

    private void checkAllFixedVolumeDevices() {
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        for (int streamType = 0; streamType < numStreamTypes; streamType++) {
            this.mStreamStates[streamType].checkFixedVolumeDevices();
        }
    }

    private void checkAllFixedVolumeDevices(int streamType) {
        this.mStreamStates[streamType].checkFixedVolumeDevices();
    }

    private void checkMuteAffectedStreams() {
        int i = 0;
        while (true) {
            VolumeStreamState[] volumeStreamStateArr = this.mStreamStates;
            if (i < volumeStreamStateArr.length) {
                VolumeStreamState vss = volumeStreamStateArr[i];
                if (vss.mIndexMin > 0 && vss.mStreamType != 0 && vss.mStreamType != 6) {
                    this.mMuteAffectedStreams &= ~(1 << vss.mStreamType);
                }
                i++;
            } else {
                return;
            }
        }
    }

    private void createStreamStates() {
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        VolumeStreamState[] streams = new VolumeStreamState[numStreamTypes];
        this.mStreamStates = streams;
        for (int i = 0; i < numStreamTypes; i++) {
            streams[i] = new VolumeStreamState(Settings.System.VOLUME_SETTINGS_INT[mStreamVolumeAlias[i]], i);
        }
        checkAllFixedVolumeDevices();
        checkAllAliasStreamVolumes();
        checkMuteAffectedStreams();
        updateDefaultVolumes();
    }

    private void updateDefaultVolumes() {
        for (int stream = 0; stream < this.mStreamStates.length; stream++) {
            int streamVolumeAlias = mStreamVolumeAlias[stream];
            if (this.mUseVolumeGroupAliases) {
                if (AudioSystem.DEFAULT_STREAM_VOLUME[stream] == -1) {
                    streamVolumeAlias = 3;
                    int defaultAliasVolume = getUiDefaultRescaledIndex(3, stream);
                    if (defaultAliasVolume >= MIN_STREAM_VOLUME[stream] && defaultAliasVolume <= MAX_STREAM_VOLUME[stream]) {
                        AudioSystem.DEFAULT_STREAM_VOLUME[stream] = defaultAliasVolume;
                    }
                }
            }
            if (stream != streamVolumeAlias) {
                AudioSystem.DEFAULT_STREAM_VOLUME[stream] = getUiDefaultRescaledIndex(streamVolumeAlias, stream);
            }
        }
    }

    private int getUiDefaultRescaledIndex(int srcStream, int dstStream) {
        return (rescaleIndex(AudioSystem.DEFAULT_STREAM_VOLUME[srcStream] * 10, srcStream, dstStream) + 5) / 10;
    }

    private void dumpStreamStates(PrintWriter pw) {
        pw.println("\nStream volumes (device: index)");
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        for (int i = 0; i < numStreamTypes; i++) {
            pw.println("- " + AudioSystem.STREAM_NAMES[i] + ":");
            this.mStreamStates[i].dump(pw);
            pw.println("");
        }
        pw.print("\n- mute affected streams = 0x");
        pw.println(Integer.toHexString(this.mMuteAffectedStreams));
    }

    private void updateStreamVolumeAlias(boolean updateVolumes, String caller) {
        int dtmfStreamAlias;
        int a11yStreamAlias = sIndependentA11yVolume ? 10 : 3;
        int assistantStreamAlias = this.mContext.getResources().getBoolean(17891808) ? 11 : 3;
        boolean z = this.mIsSingleVolume;
        if (z) {
            mStreamVolumeAlias = this.STREAM_VOLUME_ALIAS_TELEVISION;
            dtmfStreamAlias = 3;
        } else if (this.mUseVolumeGroupAliases) {
            mStreamVolumeAlias = this.STREAM_VOLUME_ALIAS_NONE;
            dtmfStreamAlias = 8;
        } else {
            int dtmfStreamAlias2 = this.mPlatformType;
            switch (dtmfStreamAlias2) {
                case 1:
                    mStreamVolumeAlias = this.STREAM_VOLUME_ALIAS_VOICE;
                    dtmfStreamAlias = 2;
                    break;
                default:
                    mStreamVolumeAlias = this.STREAM_VOLUME_ALIAS_DEFAULT;
                    dtmfStreamAlias = 3;
                    break;
            }
        }
        if (z) {
            this.mRingerModeAffectedStreams = 0;
        } else if (!isInCommunication()) {
            this.mRingerModeAffectedStreams |= 256;
        } else {
            dtmfStreamAlias = 0;
            this.mRingerModeAffectedStreams &= -257;
        }
        int[] iArr = mStreamVolumeAlias;
        iArr[8] = dtmfStreamAlias;
        iArr[10] = a11yStreamAlias;
        iArr[11] = assistantStreamAlias;
        if (updateVolumes && this.mStreamStates != null) {
            updateDefaultVolumes();
            synchronized (this.mSettingsLock) {
                synchronized (VolumeStreamState.class) {
                    VolumeStreamState[] volumeStreamStateArr = this.mStreamStates;
                    volumeStreamStateArr[8].setAllIndexes(volumeStreamStateArr[dtmfStreamAlias], caller);
                    this.mStreamStates[10].mVolumeIndexSettingName = Settings.System.VOLUME_SETTINGS_INT[a11yStreamAlias];
                    VolumeStreamState[] volumeStreamStateArr2 = this.mStreamStates;
                    volumeStreamStateArr2[10].setAllIndexes(volumeStreamStateArr2[a11yStreamAlias], caller);
                }
            }
            if (sIndependentA11yVolume) {
                this.mStreamStates[10].readSettings();
            }
            setRingerModeInt(getRingerModeInternal(), false);
            sendMsg(this.mAudioHandler, 10, 2, 0, 0, this.mStreamStates[8], 0);
            sendMsg(this.mAudioHandler, 10, 2, 0, 0, this.mStreamStates[10], 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readDockAudioSettings(ContentResolver cr) {
        boolean z = this.mSettings.getGlobalInt(cr, "dock_audio_media_enabled", 0) == 1;
        this.mDockAudioMediaEnabled = z;
        sendMsg(this.mAudioHandler, 8, 2, 3, z ? 8 : 0, new String("readDockAudioSettings"), 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMasterMono(ContentResolver cr) {
        boolean masterMono = this.mSettings.getSystemIntForUser(cr, "master_mono", 0, -2) == 1;
        if (DEBUG_VOL) {
            Log.d(TAG, String.format("Master mono %b", Boolean.valueOf(masterMono)));
        }
        AudioSystem.setMasterMono(masterMono);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMasterBalance(ContentResolver cr) {
        float masterBalance = Settings.System.getFloatForUser(cr, "master_balance", 0.0f, -2);
        if (DEBUG_VOL) {
            Log.d(TAG, String.format("Master balance %f", Float.valueOf(masterBalance)));
        }
        if (AudioSystem.setMasterBalance(masterBalance) != 0) {
            Log.e(TAG, String.format("setMasterBalance failed for %f", Float.valueOf(masterBalance)));
        }
    }

    private void sendEncodedSurroundMode(ContentResolver cr, String eventSource) {
        int encodedSurroundMode = this.mSettings.getGlobalInt(cr, "encoded_surround_output", 0);
        sendEncodedSurroundMode(encodedSurroundMode, eventSource);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendEncodedSurroundMode(int encodedSurroundMode, String eventSource) {
        int forceSetting = 16;
        switch (encodedSurroundMode) {
            case 0:
                forceSetting = 0;
                break;
            case 1:
                forceSetting = 13;
                break;
            case 2:
                forceSetting = 14;
                break;
            case 3:
                forceSetting = 15;
                break;
            default:
                Log.e(TAG, "updateSurroundSoundSettings: illegal value " + encodedSurroundMode);
                break;
        }
        if (forceSetting != 16) {
            this.mDeviceBroker.setForceUse_Async(6, forceSetting, eventSource);
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r9v0, resolved type: com.android.server.audio.AudioService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MANAGE_AUDIO_POLICY") != 0) {
            throw new SecurityException("Missing MANAGE_AUDIO_POLICY permission");
        }
        new AudioManagerShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    public Map<Integer, Boolean> getSurroundFormats() {
        Map<Integer, Boolean> surroundFormats = new HashMap<>();
        int status = AudioSystem.getSurroundFormats(surroundFormats);
        if (status != 0) {
            Log.e(TAG, "getSurroundFormats failed:" + status);
            return new HashMap();
        }
        return surroundFormats;
    }

    public List<Integer> getReportedSurroundFormats() {
        ArrayList<Integer> reportedSurroundFormats = new ArrayList<>();
        int status = AudioSystem.getReportedSurroundFormats(reportedSurroundFormats);
        if (status != 0) {
            Log.e(TAG, "getReportedSurroundFormats failed:" + status);
            return new ArrayList();
        }
        return reportedSurroundFormats;
    }

    public boolean isSurroundFormatEnabled(int audioFormat) {
        boolean contains;
        if (!isSurroundFormat(audioFormat)) {
            Log.w(TAG, "audioFormat to enable is not a surround format.");
            return false;
        } else if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SETTINGS") != 0) {
            throw new SecurityException("Missing WRITE_SETTINGS permission");
        } else {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (this.mSettingsLock) {
                    HashSet<Integer> enabledFormats = getEnabledFormats();
                    contains = enabledFormats.contains(Integer.valueOf(audioFormat));
                }
                return contains;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public boolean setSurroundFormatEnabled(int audioFormat, boolean enabled) {
        if (!isSurroundFormat(audioFormat)) {
            Log.w(TAG, "audioFormat to enable is not a surround format.");
            return false;
        } else if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SETTINGS") != 0) {
            throw new SecurityException("Missing WRITE_SETTINGS permission");
        } else {
            HashSet<Integer> enabledFormats = getEnabledFormats();
            if (enabled) {
                enabledFormats.add(Integer.valueOf(audioFormat));
            } else {
                enabledFormats.remove(Integer.valueOf(audioFormat));
            }
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (this.mSettingsLock) {
                    this.mSettings.putGlobalString(this.mContentResolver, "encoded_surround_output_enabled_formats", TextUtils.join(",", enabledFormats));
                }
                Binder.restoreCallingIdentity(token);
                return true;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        }
    }

    public boolean setEncodedSurroundMode(int mode) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SETTINGS") != 0) {
            throw new SecurityException("Missing WRITE_SETTINGS permission");
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mSettingsLock) {
                this.mSettings.putGlobalInt(this.mContentResolver, "encoded_surround_output", toEncodedSurroundSetting(mode));
            }
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    public int getEncodedSurroundMode(int targetSdkVersion) {
        int encodedSurroundOutputMode;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SETTINGS") != 0) {
            throw new SecurityException("Missing WRITE_SETTINGS permission");
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mSettingsLock) {
                int encodedSurroundSetting = this.mSettings.getGlobalInt(this.mContentResolver, "encoded_surround_output", 0);
                encodedSurroundOutputMode = toEncodedSurroundOutputMode(encodedSurroundSetting, targetSdkVersion);
            }
            return encodedSurroundOutputMode;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private HashSet<Integer> getEnabledFormats() {
        final HashSet<Integer> formats = new HashSet<>();
        String enabledFormats = this.mSettings.getGlobalString(this.mContentResolver, "encoded_surround_output_enabled_formats");
        if (enabledFormats != null) {
            try {
                IntStream mapToInt = Arrays.stream(TextUtils.split(enabledFormats, ",")).mapToInt(new ToIntFunction() { // from class: com.android.server.audio.AudioService$$ExternalSyntheticLambda9
                    @Override // java.util.function.ToIntFunction
                    public final int applyAsInt(Object obj) {
                        return Integer.parseInt((String) obj);
                    }
                });
                Objects.requireNonNull(formats);
                mapToInt.forEach(new IntConsumer() { // from class: com.android.server.audio.AudioService$$ExternalSyntheticLambda10
                    @Override // java.util.function.IntConsumer
                    public final void accept(int i) {
                        formats.add(Integer.valueOf(i));
                    }
                });
            } catch (NumberFormatException e) {
                Log.w(TAG, "ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS misformatted.", e);
            }
        }
        return formats;
    }

    private int toEncodedSurroundOutputMode(int encodedSurroundSetting, int targetSdkVersion) {
        if (targetSdkVersion <= 31 && encodedSurroundSetting > 3) {
            return -1;
        }
        switch (encodedSurroundSetting) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            default:
                return -1;
        }
    }

    private int toEncodedSurroundSetting(int encodedSurroundOutputMode) {
        switch (encodedSurroundOutputMode) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            default:
                return 0;
        }
    }

    private boolean isSurroundFormat(int audioFormat) {
        int[] iArr;
        for (int sf : AudioFormat.SURROUND_SOUND_ENCODING) {
            if (sf == audioFormat) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendEnabledSurroundFormats(ContentResolver cr, boolean forceUpdate) {
        if (this.mEncodedSurroundMode != 3) {
            return;
        }
        String enabledSurroundFormats = this.mSettings.getGlobalString(cr, "encoded_surround_output_enabled_formats");
        if (enabledSurroundFormats == null) {
            enabledSurroundFormats = "";
        }
        if (!forceUpdate && TextUtils.equals(enabledSurroundFormats, this.mEnabledSurroundFormats)) {
            return;
        }
        this.mEnabledSurroundFormats = enabledSurroundFormats;
        String[] surroundFormats = TextUtils.split(enabledSurroundFormats, ",");
        ArrayList<Integer> formats = new ArrayList<>();
        for (String format : surroundFormats) {
            try {
                int audioFormat = Integer.valueOf(format).intValue();
                if (isSurroundFormat(audioFormat) && !formats.contains(Integer.valueOf(audioFormat))) {
                    formats.add(Integer.valueOf(audioFormat));
                }
            } catch (Exception e) {
                Log.e(TAG, "Invalid enabled surround format:" + format);
            }
        }
        this.mSettings.putGlobalString(this.mContext.getContentResolver(), "encoded_surround_output_enabled_formats", TextUtils.join(",", formats));
        sendMsg(this.mAudioHandler, 24, 2, 0, 0, formats, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onEnableSurroundFormats(ArrayList<Integer> enabledSurroundFormats) {
        int[] iArr;
        for (int surroundFormat : AudioFormat.SURROUND_SOUND_ENCODING) {
            boolean enabled = enabledSurroundFormats.contains(Integer.valueOf(surroundFormat));
            int ret = AudioSystem.setSurroundFormatEnabled(surroundFormat, enabled);
            Log.i(TAG, "enable surround format:" + surroundFormat + " " + enabled + " " + ret);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAssistantUIdLocked(boolean forceUpdate) {
        int assistantUid = -1;
        String packageName = "";
        RoleObserver roleObserver = this.mRoleObserver;
        if (roleObserver != null) {
            packageName = roleObserver.getAssistantRoleHolder();
        }
        if (TextUtils.isEmpty(packageName)) {
            String assistantName = this.mSettings.getSecureStringForUser(this.mContentResolver, "voice_interaction_service", -2);
            if (TextUtils.isEmpty(assistantName)) {
                assistantName = this.mSettings.getSecureStringForUser(this.mContentResolver, "assistant", -2);
            }
            if (!TextUtils.isEmpty(assistantName)) {
                ComponentName componentName = ComponentName.unflattenFromString(assistantName);
                if (componentName == null) {
                    Slog.w(TAG, "Invalid service name for voice_interaction_service: " + assistantName);
                    return;
                }
                packageName = componentName.getPackageName();
            }
        }
        if (!TextUtils.isEmpty(packageName)) {
            PackageManager pm = this.mContext.getPackageManager();
            ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
            if (pm.checkPermission("android.permission.CAPTURE_AUDIO_HOTWORD", packageName) == 0) {
                try {
                    assistantUid = pm.getPackageUidAsUser(packageName, ActivityManager.getCurrentUser());
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "updateAssistantUId() could not find UID for package: " + packageName);
                }
            }
        }
        int i = this.mPrimaryAssistantUid;
        if (i != assistantUid || forceUpdate) {
            this.mAssistantUids.remove(Integer.valueOf(i));
            this.mPrimaryAssistantUid = assistantUid;
            addAssistantServiceUidsLocked(new int[]{assistantUid});
        }
    }

    private void readPersistedSettings() {
        int i;
        if (!this.mSystemServer.isPrivileged()) {
            return;
        }
        ContentResolver cr = this.mContentResolver;
        int i2 = 2;
        int ringerModeFromSettings = this.mSettings.getGlobalInt(cr, "mode_ringer", 2);
        int ringerMode = ringerModeFromSettings;
        if (!isValidRingerMode(ringerMode)) {
            ringerMode = 2;
        }
        if (ringerMode == 1 && !this.mHasVibrator) {
            ringerMode = 0;
        }
        if (ringerMode != ringerModeFromSettings) {
            this.mSettings.putGlobalInt(cr, "mode_ringer", ringerMode);
        }
        ringerMode = (this.mUseFixedVolume || this.mIsSingleVolume) ? 2 : 2;
        synchronized (this.mSettingsLock) {
            this.mRingerMode = ringerMode;
            if (this.mRingerModeExternal == -1) {
                this.mRingerModeExternal = ringerMode;
            }
            if (this.mHasVibrator) {
                i = 2;
            } else {
                i = 0;
            }
            int valueForVibrateSetting = AudioSystem.getValueForVibrateSetting(0, 1, i);
            this.mVibrateSetting = valueForVibrateSetting;
            if (!this.mHasVibrator) {
                i2 = 0;
            }
            this.mVibrateSetting = AudioSystem.getValueForVibrateSetting(valueForVibrateSetting, 0, i2);
            updateRingerAndZenModeAffectedStreams();
            readDockAudioSettings(cr);
            sendEncodedSurroundMode(cr, "readPersistedSettings");
            sendEnabledSurroundFormats(cr, true);
            updateAssistantUIdLocked(true);
            resetActiveAssistantUidsLocked();
            AudioSystem.setRttEnabled(this.mRttEnabled);
        }
        this.mMuteAffectedStreams = this.mSettings.getSystemIntForUser(cr, "mute_streams_affected", 111, -2);
        updateMasterMono(cr);
        updateMasterBalance(cr);
        broadcastRingerMode("android.media.RINGER_MODE_CHANGED", this.mRingerModeExternal);
        broadcastRingerMode("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION", this.mRingerMode);
        broadcastVibrateSetting(0);
        broadcastVibrateSetting(1);
        this.mVolumeController.loadSettings(cr);
    }

    private void resetActiveAssistantUidsLocked() {
        this.mActiveAssistantServiceUids = NO_ACTIVE_ASSISTANT_SERVICE_UIDS;
        updateActiveAssistantServiceUids();
    }

    private void readUserRestrictions() {
        if (!this.mSystemServer.isPrivileged()) {
            return;
        }
        int currentUser = getCurrentUserId();
        boolean masterMute = this.mUserManagerInternal.getUserRestriction(currentUser, "disallow_unmute_device") || this.mUserManagerInternal.getUserRestriction(currentUser, "no_adjust_volume");
        if (this.mUseFixedVolume) {
            masterMute = false;
            AudioSystem.setMasterVolume(1.0f);
        }
        boolean z = DEBUG_VOL;
        if (z) {
            Log.d(TAG, String.format("Master mute %s, user=%d", Boolean.valueOf(masterMute), Integer.valueOf(currentUser)));
        }
        AudioSystem.setMasterMute(masterMute);
        broadcastMasterMuteStatus(masterMute);
        boolean userRestriction = this.mUserManagerInternal.getUserRestriction(currentUser, "no_unmute_microphone");
        this.mMicMuteFromRestrictions = userRestriction;
        if (z) {
            Log.d(TAG, String.format("Mic mute %b, user=%d", Boolean.valueOf(userRestriction), Integer.valueOf(currentUser)));
        }
        setMicrophoneMuteNoCallerCheck(currentUser);
    }

    private int getIndexRange(int streamType) {
        return this.mStreamStates[streamType].getMaxIndex() - this.mStreamStates[streamType].getMinIndex();
    }

    private int rescaleIndex(VolumeInfo volumeInfo, int dstStream) {
        if (volumeInfo.getVolumeIndex() == -100 || volumeInfo.getMinVolumeIndex() == -100 || volumeInfo.getMaxVolumeIndex() == -100) {
            Log.e(TAG, "rescaleIndex: volumeInfo has invalid index or range");
            return this.mStreamStates[dstStream].getMinIndex();
        }
        return rescaleIndex(volumeInfo.getVolumeIndex(), volumeInfo.getMinVolumeIndex(), volumeInfo.getMaxVolumeIndex(), this.mStreamStates[dstStream].getMinIndex(), this.mStreamStates[dstStream].getMaxIndex());
    }

    private int rescaleIndex(int index, int srcStream, VolumeInfo dstVolumeInfo) {
        int dstMin = dstVolumeInfo.getMinVolumeIndex();
        int dstMax = dstVolumeInfo.getMaxVolumeIndex();
        if (dstMin == -100 || dstMax == -100) {
            return index;
        }
        return rescaleIndex(index, this.mStreamStates[srcStream].getMinIndex(), this.mStreamStates[srcStream].getMaxIndex(), dstMin, dstMax);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int rescaleIndex(int index, int srcStream, int dstStream) {
        return rescaleIndex(index, this.mStreamStates[srcStream].getMinIndex(), this.mStreamStates[srcStream].getMaxIndex(), this.mStreamStates[dstStream].getMinIndex(), this.mStreamStates[dstStream].getMaxIndex());
    }

    private int rescaleIndex(int index, int srcMin, int srcMax, int dstMin, int dstMax) {
        int srcRange = srcMax - srcMin;
        int dstRange = dstMax - dstMin;
        if (srcRange == 0) {
            Log.e(TAG, "rescaleIndex : index range should not be zero");
            return dstMin;
        }
        return ((((index - srcMin) * dstRange) + (srcRange / 2)) / srcRange) + dstMin;
    }

    private int rescaleStep(int step, int srcStream, int dstStream) {
        int srcRange = getIndexRange(srcStream);
        int dstRange = getIndexRange(dstStream);
        if (srcRange == 0) {
            Log.e(TAG, "rescaleStep : index range should not be zero");
            return 0;
        }
        return ((step * dstRange) + (srcRange / 2)) / srcRange;
    }

    public int setPreferredDevicesForStrategy(int strategy, List<AudioDeviceAttributes> devices) {
        if (devices == null) {
            return -1;
        }
        enforceModifyAudioRoutingPermission();
        String logString = String.format("setPreferredDeviceForStrategy u/pid:%d/%d strat:%d dev:%s", Integer.valueOf(Binder.getCallingUid()), Integer.valueOf(Binder.getCallingPid()), Integer.valueOf(strategy), devices.stream().map(new Function() { // from class: com.android.server.audio.AudioService$$ExternalSyntheticLambda12
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                String audioDeviceAttributes;
                audioDeviceAttributes = ((AudioDeviceAttributes) obj).toString();
                return audioDeviceAttributes;
            }
        }).collect(Collectors.joining(",")));
        sDeviceLogger.log(new AudioEventLogger.StringEvent(logString).printLog(TAG));
        if (devices.stream().anyMatch(new Predicate() { // from class: com.android.server.audio.AudioService$$ExternalSyntheticLambda13
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AudioService.lambda$setPreferredDevicesForStrategy$3((AudioDeviceAttributes) obj);
            }
        })) {
            Log.e(TAG, "Unsupported input routing in " + logString);
            return -1;
        }
        int status = this.mDeviceBroker.setPreferredDevicesForStrategySync(strategy, devices);
        if (status != 0) {
            Log.e(TAG, String.format("Error %d in %s)", Integer.valueOf(status), logString));
        }
        return status;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$setPreferredDevicesForStrategy$3(AudioDeviceAttributes device) {
        return device.getRole() == 1;
    }

    public int removePreferredDevicesForStrategy(int strategy) {
        enforceModifyAudioRoutingPermission();
        String logString = String.format("removePreferredDeviceForStrategy strat:%d", Integer.valueOf(strategy));
        sDeviceLogger.log(new AudioEventLogger.StringEvent(logString).printLog(TAG));
        int status = this.mDeviceBroker.removePreferredDevicesForStrategySync(strategy);
        if (status != 0) {
            Log.e(TAG, String.format("Error %d in %s)", Integer.valueOf(status), logString));
        }
        return status;
    }

    public List<AudioDeviceAttributes> getPreferredDevicesForStrategy(int strategy) {
        enforceModifyAudioRoutingPermission();
        List<AudioDeviceAttributes> devices = new ArrayList<>();
        long identity = Binder.clearCallingIdentity();
        int status = AudioSystem.getDevicesForRoleAndStrategy(strategy, 1, devices);
        Binder.restoreCallingIdentity(identity);
        if (status != 0) {
            Log.e(TAG, String.format("Error %d in getPreferredDeviceForStrategy(%d)", Integer.valueOf(status), Integer.valueOf(strategy)));
            return new ArrayList();
        }
        return devices;
    }

    public void registerStrategyPreferredDevicesDispatcher(IStrategyPreferredDevicesDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        enforceModifyAudioRoutingPermission();
        this.mDeviceBroker.registerStrategyPreferredDevicesDispatcher(dispatcher);
    }

    public void unregisterStrategyPreferredDevicesDispatcher(IStrategyPreferredDevicesDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        enforceModifyAudioRoutingPermission();
        this.mDeviceBroker.unregisterStrategyPreferredDevicesDispatcher(dispatcher);
    }

    public int setPreferredDevicesForCapturePreset(int capturePreset, List<AudioDeviceAttributes> devices) {
        if (devices == null) {
            return -1;
        }
        enforceModifyAudioRoutingPermission();
        String logString = String.format("setPreferredDevicesForCapturePreset u/pid:%d/%d source:%d dev:%s", Integer.valueOf(Binder.getCallingUid()), Integer.valueOf(Binder.getCallingPid()), Integer.valueOf(capturePreset), devices.stream().map(new Function() { // from class: com.android.server.audio.AudioService$$ExternalSyntheticLambda4
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                String audioDeviceAttributes;
                audioDeviceAttributes = ((AudioDeviceAttributes) obj).toString();
                return audioDeviceAttributes;
            }
        }).collect(Collectors.joining(",")));
        sDeviceLogger.log(new AudioEventLogger.StringEvent(logString).printLog(TAG));
        if (devices.stream().anyMatch(new Predicate() { // from class: com.android.server.audio.AudioService$$ExternalSyntheticLambda5
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AudioService.lambda$setPreferredDevicesForCapturePreset$5((AudioDeviceAttributes) obj);
            }
        })) {
            Log.e(TAG, "Unsupported output routing in " + logString);
            return -1;
        }
        int status = this.mDeviceBroker.setPreferredDevicesForCapturePresetSync(capturePreset, devices);
        if (status != 0) {
            Log.e(TAG, String.format("Error %d in %s)", Integer.valueOf(status), logString));
        }
        return status;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$setPreferredDevicesForCapturePreset$5(AudioDeviceAttributes device) {
        return device.getRole() == 2;
    }

    public int clearPreferredDevicesForCapturePreset(int capturePreset) {
        enforceModifyAudioRoutingPermission();
        String logString = String.format("removePreferredDeviceForCapturePreset source:%d", Integer.valueOf(capturePreset));
        sDeviceLogger.log(new AudioEventLogger.StringEvent(logString).printLog(TAG));
        int status = this.mDeviceBroker.clearPreferredDevicesForCapturePresetSync(capturePreset);
        if (status != 0) {
            Log.e(TAG, String.format("Error %d in %s", Integer.valueOf(status), logString));
        }
        return status;
    }

    public List<AudioDeviceAttributes> getPreferredDevicesForCapturePreset(int capturePreset) {
        enforceModifyAudioRoutingPermission();
        List<AudioDeviceAttributes> devices = new ArrayList<>();
        long identity = Binder.clearCallingIdentity();
        int status = AudioSystem.getDevicesForRoleAndCapturePreset(capturePreset, 1, devices);
        Binder.restoreCallingIdentity(identity);
        if (status != 0) {
            Log.e(TAG, String.format("Error %d in getPreferredDeviceForCapturePreset(%d)", Integer.valueOf(status), Integer.valueOf(capturePreset)));
            return new ArrayList();
        }
        return devices;
    }

    public void registerCapturePresetDevicesRoleDispatcher(ICapturePresetDevicesRoleDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        enforceModifyAudioRoutingPermission();
        this.mDeviceBroker.registerCapturePresetDevicesRoleDispatcher(dispatcher);
    }

    public void unregisterCapturePresetDevicesRoleDispatcher(ICapturePresetDevicesRoleDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        enforceModifyAudioRoutingPermission();
        this.mDeviceBroker.unregisterCapturePresetDevicesRoleDispatcher(dispatcher);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* renamed from: getDevicesForAttributes */
    public ArrayList<AudioDeviceAttributes> m1912getDevicesForAttributes(AudioAttributes attributes) {
        enforceQueryStateOrModifyRoutingPermission();
        return getDevicesForAttributesInt(attributes, false);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* renamed from: getDevicesForAttributesUnprotected */
    public ArrayList<AudioDeviceAttributes> m1913getDevicesForAttributesUnprotected(AudioAttributes attributes) {
        return getDevicesForAttributesInt(attributes, false);
    }

    public boolean isMusicActive(boolean remotely) {
        long token = Binder.clearCallingIdentity();
        try {
            if (remotely) {
                return AudioSystem.isStreamActiveRemotely(3, 0);
            }
            return AudioSystem.isStreamActive(3, 0);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    protected ArrayList<AudioDeviceAttributes> getDevicesForAttributesInt(AudioAttributes attributes, boolean forVolume) {
        Objects.requireNonNull(attributes);
        return this.mAudioSystem.getDevicesForAttributes(attributes, forVolume);
    }

    public void handleVolumeKey(KeyEvent event, boolean isOnTv, String callingPackage, String caller) {
        int keyEventMode = 0;
        if (isOnTv) {
            if (event.getAction() == 0) {
                keyEventMode = 1;
            } else {
                keyEventMode = 2;
            }
        } else if (event.getAction() != 0) {
            return;
        }
        switch (event.getKeyCode()) {
            case 24:
                adjustSuggestedStreamVolume(1, Integer.MIN_VALUE, 4101, callingPackage, caller, Binder.getCallingUid(), Binder.getCallingPid(), true, keyEventMode);
                return;
            case 25:
                adjustSuggestedStreamVolume(-1, Integer.MIN_VALUE, 4101, callingPackage, caller, Binder.getCallingUid(), Binder.getCallingPid(), true, keyEventMode);
                return;
            case 164:
                if (event.getAction() == 0 && event.getRepeatCount() == 0) {
                    adjustSuggestedStreamVolume(101, Integer.MIN_VALUE, 4101, callingPackage, caller, Binder.getCallingUid(), Binder.getCallingPid(), true, 0);
                    return;
                }
                return;
            default:
                Log.e(TAG, "Invalid key code " + event.getKeyCode() + " sent by " + callingPackage);
                return;
        }
    }

    public void setNavigationRepeatSoundEffectsEnabled(boolean enabled) {
        this.mNavigationRepeatSoundEffectsEnabled = enabled;
    }

    public boolean areNavigationRepeatSoundEffectsEnabled() {
        return this.mNavigationRepeatSoundEffectsEnabled;
    }

    public void setHomeSoundEffectEnabled(boolean enabled) {
        this.mHomeSoundEffectEnabled = enabled;
    }

    public boolean isHomeSoundEffectEnabled() {
        return this.mHomeSoundEffectEnabled;
    }

    private void adjustSuggestedStreamVolume(int direction, int suggestedStreamType, int flags, String callingPackage, String caller, int uid, int pid, boolean hasModifyAudioSettings, int keyEventMode) {
        boolean activeForReal;
        int streamType;
        int i;
        int direction2;
        int flags2;
        int flags3 = flags;
        boolean z = DEBUG_VOL;
        if (z) {
            Log.d(TAG, "adjustSuggestedStreamVolume() stream=" + suggestedStreamType + ", flags=" + flags3 + ", caller=" + caller + ", volControlStream=" + this.mVolumeControlStream + ", userSelect=" + this.mUserSelectedVolumeControlStream);
        }
        if (direction != 0) {
            sVolumeLogger.log(new AudioServiceEvents.VolumeEvent(0, suggestedStreamType, direction, flags, callingPackage + SliceClientPermissions.SliceAuthority.DELIMITER + caller + " uid:" + uid));
        }
        boolean hasExternalVolumeController = notifyExternalVolumeController(direction);
        new MediaMetrics.Item("audio.service.adjustSuggestedStreamVolume").setUid(Binder.getCallingUid()).set(MediaMetrics.Property.CALLING_PACKAGE, callingPackage).set(MediaMetrics.Property.CLIENT_NAME, caller).set(MediaMetrics.Property.DIRECTION, direction > 0 ? INetd.IF_STATE_UP : INetd.IF_STATE_DOWN).set(MediaMetrics.Property.EXTERNAL, hasExternalVolumeController ? UiModeManagerService.Shell.NIGHT_MODE_STR_YES : UiModeManagerService.Shell.NIGHT_MODE_STR_NO).set(MediaMetrics.Property.FLAGS, Integer.valueOf(flags)).record();
        if (hasExternalVolumeController) {
            return;
        }
        synchronized (this.mForceControlStreamLock) {
            if (this.mUserSelectedVolumeControlStream) {
                streamType = this.mVolumeControlStream;
            } else {
                int maybeActiveStreamType = getActiveStreamType(suggestedStreamType);
                if (maybeActiveStreamType != 2 && maybeActiveStreamType != 5) {
                    activeForReal = this.mAudioSystem.isStreamActive(maybeActiveStreamType, 0);
                    if (!activeForReal && (i = this.mVolumeControlStream) != -1) {
                        streamType = i;
                    }
                    streamType = maybeActiveStreamType;
                }
                activeForReal = wasStreamActiveRecently(maybeActiveStreamType, 0);
                if (!activeForReal) {
                    streamType = i;
                }
                streamType = maybeActiveStreamType;
            }
        }
        boolean isMute = isMuteAdjust(direction);
        ensureValidStreamType(streamType);
        int resolvedStream = mStreamVolumeAlias[streamType];
        if ((flags3 & 4) != 0 && resolvedStream != 2) {
            flags3 &= -5;
        }
        if (!this.mVolumeController.suppressAdjustment(resolvedStream, flags3, isMute) || this.mIsSingleVolume) {
            direction2 = direction;
            flags2 = flags3;
        } else {
            int flags4 = flags3 & (-5) & (-17);
            if (z) {
                Log.d(TAG, "Volume controller suppressed adjustment");
            }
            direction2 = 0;
            flags2 = flags4;
        }
        if (ITranAudioService.Instance().isTheftAlertRinging(this.mContext)) {
            return;
        }
        adjustStreamVolume(streamType, direction2, flags2, callingPackage, caller, uid, pid, null, hasModifyAudioSettings, keyEventMode);
    }

    private boolean notifyExternalVolumeController(int direction) {
        IAudioPolicyCallback externalVolumeController;
        synchronized (this.mExtVolumeControllerLock) {
            externalVolumeController = this.mExtVolumeController;
        }
        if (externalVolumeController == null) {
            return false;
        }
        sendMsg(this.mAudioHandler, 22, 2, direction, 0, externalVolumeController, 0);
        return true;
    }

    public void adjustStreamVolume(int streamType, int direction, int flags, String callingPackage) {
        adjustStreamVolumeWithAttribution(streamType, direction, flags, callingPackage, null);
    }

    public void adjustStreamVolumeWithAttribution(int streamType, int direction, int flags, String callingPackage, String attributionTag) {
        if (streamType == 10 && !canChangeAccessibilityVolume()) {
            Log.w(TAG, "Trying to call adjustStreamVolume() for a11y withoutCHANGE_ACCESSIBILITY_VOLUME / callingPackage=" + callingPackage);
            return;
        }
        sVolumeLogger.log(new AudioServiceEvents.VolumeEvent(1, streamType, direction, flags, callingPackage));
        adjustStreamVolume(streamType, direction, flags, callingPackage, callingPackage, Binder.getCallingUid(), Binder.getCallingPid(), attributionTag, callingHasAudioSettingsPermission(), 0);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:233:0x0490
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    protected void adjustStreamVolume(int r28, int r29, int r30, java.lang.String r31, java.lang.String r32, int r33, int r34, java.lang.String r35, boolean r36, int r37) {
        /*
            r27 = this;
            r8 = r27
            r9 = r28
            r10 = r29
            r1 = r30
            r11 = r32
            r0 = r33
            r12 = r34
            r13 = r37
            boolean r2 = r8.mUseFixedVolume
            if (r2 == 0) goto L15
            return
        L15:
            boolean r2 = com.android.server.audio.AudioService.DEBUG_VOL
            if (r2 == 0) goto L4f
            java.lang.String r3 = "AS.AudioService"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "adjustStreamVolume() stream="
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r9)
            java.lang.String r5 = ", dir="
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r10)
            java.lang.String r5 = ", flags="
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r1)
            java.lang.String r5 = ", caller="
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r11)
            java.lang.String r4 = r4.toString()
            android.util.Log.d(r3, r4)
        L4f:
            r8.ensureValidDirection(r10)
            r27.ensureValidStreamType(r28)
            boolean r14 = r8.isMuteAdjust(r10)
            if (r14 == 0) goto L62
            boolean r3 = r27.isStreamAffectedByMute(r28)
            if (r3 != 0) goto L62
            return
        L62:
            if (r14 == 0) goto L9e
            if (r9 == 0) goto L69
            r3 = 6
            if (r9 != r3) goto L9e
        L69:
            android.content.Context r3 = r8.mContext
            java.lang.String r4 = "android.permission.MODIFY_PHONE_STATE"
            int r3 = r3.checkPermission(r4, r12, r0)
            if (r3 == 0) goto L9e
            java.lang.String r2 = "AS.AudioService"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "MODIFY_PHONE_STATE Permission Denial: adjustStreamVolume from pid="
            java.lang.StringBuilder r3 = r3.append(r4)
            int r4 = android.os.Binder.getCallingPid()
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r4 = ", uid="
            java.lang.StringBuilder r3 = r3.append(r4)
            int r4 = android.os.Binder.getCallingUid()
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Log.w(r2, r3)
            return
        L9e:
            r3 = 11
            if (r9 != r3) goto Ld7
            android.content.Context r3 = r8.mContext
            java.lang.String r4 = "android.permission.MODIFY_AUDIO_ROUTING"
            int r3 = r3.checkPermission(r4, r12, r0)
            if (r3 == 0) goto Ld7
            java.lang.String r2 = "AS.AudioService"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "MODIFY_AUDIO_ROUTING Permission Denial: adjustStreamVolume from pid="
            java.lang.StringBuilder r3 = r3.append(r4)
            int r4 = android.os.Binder.getCallingPid()
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r4 = ", uid="
            java.lang.StringBuilder r3 = r3.append(r4)
            int r4 = android.os.Binder.getCallingUid()
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Log.w(r2, r3)
            return
        Ld7:
            int[] r3 = com.android.server.audio.AudioService.mStreamVolumeAlias
            r15 = r3[r9]
            com.android.server.audio.AudioService$VolumeStreamState[] r3 = r8.mStreamStates
            r7 = r3[r15]
            int r6 = r8.getDeviceForStream(r15)
            if (r2 == 0) goto Lfd
            java.lang.String r2 = "AS.AudioService"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "adjustStreamVolume getDeviceForStream device = "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r6)
            java.lang.String r3 = r3.toString()
            android.util.Log.d(r2, r3)
        Lfd:
            int r2 = r7.getIndex(r6)
            r16 = 1
            java.util.Set r3 = android.media.AudioSystem.DEVICE_OUT_ALL_A2DP_SET
            java.lang.Integer r4 = java.lang.Integer.valueOf(r6)
            boolean r3 = r3.contains(r4)
            if (r3 != 0) goto L120
            java.util.Set r3 = android.media.AudioSystem.DEVICE_OUT_ALL_BLE_SET
            java.lang.Integer r4 = java.lang.Integer.valueOf(r6)
            boolean r3 = r3.contains(r4)
            if (r3 != 0) goto L120
            r3 = r1 & 64
            if (r3 == 0) goto L120
            return
        L120:
            r3 = 1000(0x3e8, float:1.401E-42)
            if (r0 != r3) goto L132
            int r3 = r27.getCurrentUserId()
            int r4 = android.os.UserHandle.getAppId(r33)
            int r0 = android.os.UserHandle.getUid(r3, r4)
            r5 = r0
            goto L133
        L132:
            r5 = r0
        L133:
            int[] r0 = com.android.server.audio.AudioService.STREAM_VOLUME_OPS
            r0 = r0[r15]
            r4 = r31
            r3 = r35
            boolean r0 = r8.checkNoteAppOp(r0, r5, r4, r3)
            if (r0 != 0) goto L142
            return
        L142:
            java.lang.Object r3 = r8.mSafeMediaVolumeStateLock
            monitor-enter(r3)
            r0 = 0
            r8.mPendingVolumeCommand = r0     // Catch: java.lang.Throwable -> L4d3
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L4d3
            r0 = r1 & (-33)
            r3 = 3
            if (r15 != r3) goto L17c
            boolean r1 = r8.isFixedVolumeDevice(r6)
            if (r1 == 0) goto L17c
            r0 = r0 | 32
            int r1 = r8.mSafeMediaVolumeState
            if (r1 != r3) goto L16b
            java.util.Set<java.lang.Integer> r1 = r8.mSafeMediaVolumeDevices
            java.lang.Integer r3 = java.lang.Integer.valueOf(r6)
            boolean r1 = r1.contains(r3)
            if (r1 == 0) goto L16b
            int r1 = r8.safeMediaVolumeIndex(r6)
            goto L16f
        L16b:
            int r1 = r7.getMaxIndex()
        L16f:
            if (r2 == 0) goto L177
            r2 = r1
            r23 = r1
            r24 = r2
            goto L186
        L177:
            r23 = r1
            r24 = r2
            goto L186
        L17c:
            r1 = 10
            int r1 = r8.rescaleStep(r1, r9, r15)
            r23 = r1
            r24 = r2
        L186:
            r1 = r0 & 2
            r3 = 0
            r2 = 1
            if (r1 != 0) goto L19a
            boolean r1 = r8.isUiSoundsStreamType(r15)
            if (r1 == 0) goto L193
            goto L19a
        L193:
            r25 = r5
            r12 = r6
            r33 = r7
            r7 = r0
            goto L1d7
        L19a:
            int r1 = r27.getRingerModeInternal()
            if (r1 != r2) goto L1a2
            r0 = r0 & (-17)
        L1a2:
            boolean r17 = com.android.server.audio.AudioService.VolumeStreamState.m1923$$Nest$fgetmIsMuted(r7)
            r18 = r1
            r1 = r27
            r12 = r2
            r2 = r24
            r3 = r29
            r4 = r23
            r25 = r5
            r5 = r17
            r12 = r6
            r6 = r31
            r33 = r7
            r7 = r0
            int r1 = r1.checkForRingerModeChange(r2, r3, r4, r5, r6, r7)
            r2 = r1 & 1
            if (r2 == 0) goto L1c5
            r3 = 1
            goto L1c6
        L1c5:
            r3 = 0
        L1c6:
            r16 = r3
            r2 = r1 & 128(0x80, float:1.794E-43)
            if (r2 == 0) goto L1ce
            r0 = r0 | 128(0x80, float:1.794E-43)
        L1ce:
            r2 = r1 & 2048(0x800, float:2.87E-42)
            if (r2 == 0) goto L1d6
            r0 = r0 | 2048(0x800, float:2.87E-42)
            r7 = r0
            goto L1d7
        L1d6:
            r7 = r0
        L1d7:
            boolean r0 = r8.volumeAdjustmentAllowedByDnd(r15, r7)
            if (r0 != 0) goto L1e2
            r16 = 0
            r26 = r16
            goto L1e4
        L1e2:
            r26 = r16
        L1e4:
            com.android.server.audio.AudioService$VolumeStreamState[] r0 = r8.mStreamStates
            r0 = r0[r9]
            int r6 = r0.getIndex(r12)
            boolean r0 = r8.isAbsoluteVolumeDevice(r12)
            if (r0 == 0) goto L219
            r0 = r7 & 8192(0x2000, float:1.14794E-41)
            if (r0 != 0) goto L219
            java.util.Map<java.lang.Integer, com.android.server.audio.AudioService$AbsoluteVolumeDeviceInfo> r0 = r8.mAbsoluteVolumeDeviceInfoMap
            java.lang.Integer r1 = java.lang.Integer.valueOf(r12)
            java.lang.Object r0 = r0.get(r1)
            com.android.server.audio.AudioService$AbsoluteVolumeDeviceInfo r0 = (com.android.server.audio.AudioService.AbsoluteVolumeDeviceInfo) r0
            boolean r1 = com.android.server.audio.AudioService.AbsoluteVolumeDeviceInfo.m1916$$Nest$fgetmHandlesVolumeAdjustment(r0)
            if (r1 == 0) goto L217
            r1 = r27
            r2 = r28
            r3 = r0
            r4 = r6
            r5 = r29
            r9 = r6
            r6 = r37
            r1.dispatchAbsoluteVolumeAdjusted(r2, r3, r4, r5, r6)
            return
        L217:
            r9 = r6
            goto L21a
        L219:
            r9 = r6
        L21a:
            if (r26 == 0) goto L3f1
            if (r10 == 0) goto L3f1
            r0 = 2
            if (r13 == r0) goto L3f1
            com.android.server.audio.AudioService$AudioHandler r0 = r8.mAudioHandler
            r1 = 18
            r0.removeMessages(r1)
            if (r14 == 0) goto L278
            java.util.Set<java.lang.Integer> r0 = r8.mFullVolumeDevices
            java.lang.Integer r1 = java.lang.Integer.valueOf(r12)
            boolean r0 = r0.contains(r1)
            if (r0 != 0) goto L278
            r0 = 101(0x65, float:1.42E-43)
            if (r10 != r0) goto L241
            boolean r0 = com.android.server.audio.AudioService.VolumeStreamState.m1923$$Nest$fgetmIsMuted(r33)
            r1 = 1
            r0 = r0 ^ r1
            goto L249
        L241:
            r0 = -100
            if (r10 != r0) goto L247
            r3 = 1
            goto L248
        L247:
            r3 = 0
        L248:
            r0 = r3
        L249:
            r1 = 0
        L24a:
            com.android.server.audio.AudioService$VolumeStreamState[] r2 = r8.mStreamStates
            int r2 = r2.length
            if (r1 >= r2) goto L270
            int[] r2 = com.android.server.audio.AudioService.mStreamVolumeAlias
            r2 = r2[r1]
            if (r15 != r2) goto L26d
            boolean r2 = r27.readCameraSoundForced()
            if (r2 == 0) goto L266
            com.android.server.audio.AudioService$VolumeStreamState[] r2 = r8.mStreamStates
            r2 = r2[r1]
            int r2 = r2.getStreamType()
            r3 = 7
            if (r2 == r3) goto L26d
        L266:
            com.android.server.audio.AudioService$VolumeStreamState[] r2 = r8.mStreamStates
            r2 = r2[r1]
            r2.mute(r0)
        L26d:
            int r1 = r1 + 1
            goto L24a
        L270:
            r5 = r33
            r6 = r36
            r4 = r15
            r0 = 0
            goto L30f
        L278:
            r0 = 1
            if (r10 != r0) goto L2a8
            int r0 = r24 + r23
            boolean r0 = r8.checkSafeMediaVolume(r15, r0, r12)
            if (r0 != 0) goto L2a8
            java.lang.String r0 = "AS.AudioService"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "adjustStreamVolume() safe volume index = "
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r1 = r1.append(r9)
            java.lang.String r1 = r1.toString()
            android.util.Log.e(r0, r1)
            com.android.server.audio.AudioService$VolumeController r0 = r8.mVolumeController
            r0.postDisplaySafeVolumeWarning(r7)
            r5 = r33
            r6 = r36
            r4 = r15
            r0 = 0
            goto L30f
        L2a8:
            boolean r0 = r8.isFullVolumeDevice(r12)
            if (r0 != 0) goto L309
            int r0 = r10 * r23
            r5 = r33
            r6 = r36
            boolean r0 = r5.adjustIndex(r0, r12, r11, r6)
            if (r0 != 0) goto L2c4
            boolean r0 = com.android.server.audio.AudioService.VolumeStreamState.m1923$$Nest$fgetmIsMuted(r5)
            if (r0 == 0) goto L2c1
            goto L2c4
        L2c1:
            r4 = r15
            r0 = 0
            goto L30f
        L2c4:
            boolean r0 = com.android.server.audio.AudioService.VolumeStreamState.m1923$$Nest$fgetmIsMuted(r5)
            if (r0 == 0) goto L2f3
            r0 = 1
            if (r10 != r0) goto L2d3
            r0 = 0
            r5.mute(r0)
            r4 = r15
            goto L2f5
        L2d3:
            r0 = 0
            r1 = -1
            if (r10 != r1) goto L2f1
            boolean r1 = r8.mIsSingleVolume
            if (r1 == 0) goto L2ef
            com.android.server.audio.AudioService$AudioHandler r1 = r8.mAudioHandler
            r16 = 18
            r17 = 2
            r20 = 0
            r21 = 350(0x15e, float:4.9E-43)
            r4 = r15
            r15 = r1
            r18 = r4
            r19 = r7
            sendMsg(r15, r16, r17, r18, r19, r20, r21)
            goto L2f5
        L2ef:
            r4 = r15
            goto L2f5
        L2f1:
            r4 = r15
            goto L2f5
        L2f3:
            r4 = r15
            r0 = 0
        L2f5:
            com.android.server.audio.AudioService$AudioHandler r1 = r8.mAudioHandler
            r17 = 0
            r18 = 2
            r20 = 0
            r22 = 0
            r16 = r1
            r19 = r12
            r21 = r5
            sendMsg(r16, r17, r18, r19, r20, r21, r22)
            goto L30f
        L309:
            r5 = r33
            r6 = r36
            r4 = r15
            r0 = 0
        L30f:
            com.android.server.audio.AudioService$VolumeStreamState[] r1 = r8.mStreamStates
            r15 = r9
            r9 = r28
            r1 = r1[r9]
            int r1 = r1.getIndex(r12)
            r2 = 3
            if (r4 != r2) goto L35c
            java.util.Set r3 = android.media.AudioSystem.DEVICE_OUT_ALL_A2DP_SET
            java.lang.Integer r0 = java.lang.Integer.valueOf(r12)
            boolean r0 = r3.contains(r0)
            if (r0 == 0) goto L35c
            r0 = r7 & 64
            if (r0 != 0) goto L35c
            boolean r0 = com.android.server.audio.AudioService.DEBUG_VOL
            if (r0 == 0) goto L354
            java.lang.String r0 = "AS.AudioService"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r2 = "adjustSreamVolume: postSetAvrcpAbsoluteVolumeIndex index="
            java.lang.StringBuilder r2 = r3.append(r2)
            java.lang.StringBuilder r2 = r2.append(r1)
            java.lang.String r3 = "stream="
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r9)
            java.lang.String r2 = r2.toString()
            android.util.Log.d(r0, r2)
        L354:
            com.android.server.audio.AudioDeviceBroker r0 = r8.mDeviceBroker
            int r2 = r1 / 10
            r0.postSetAvrcpAbsoluteVolumeIndex(r2)
            goto L375
        L35c:
            boolean r0 = r8.isAbsoluteVolumeDevice(r12)
            if (r0 == 0) goto L375
            r0 = r7 & 8192(0x2000, float:1.14794E-41)
            if (r0 != 0) goto L375
            java.util.Map<java.lang.Integer, com.android.server.audio.AudioService$AbsoluteVolumeDeviceInfo> r0 = r8.mAbsoluteVolumeDeviceInfoMap
            java.lang.Integer r2 = java.lang.Integer.valueOf(r12)
            java.lang.Object r0 = r0.get(r2)
            com.android.server.audio.AudioService$AbsoluteVolumeDeviceInfo r0 = (com.android.server.audio.AudioService.AbsoluteVolumeDeviceInfo) r0
            r8.dispatchAbsoluteVolumeChanged(r9, r0, r1)
        L375:
            r0 = 536870912(0x20000000, float:1.0842022E-19)
            if (r12 == r0) goto L37e
            r0 = 536870914(0x20000002, float:1.0842024E-19)
            if (r12 != r0) goto L3bb
        L37e:
            int r0 = r27.getBluetoothContextualVolumeStream()
            if (r9 != r0) goto L3bb
            r0 = r7 & 64
            if (r0 != 0) goto L3bb
            boolean r0 = com.android.server.audio.AudioService.DEBUG_VOL
            if (r0 == 0) goto L3ae
            java.lang.String r0 = "AS.AudioService"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "adjustSreamVolume postSetLeAudioVolumeIndex index="
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r1)
            java.lang.String r3 = " stream="
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r9)
            java.lang.String r2 = r2.toString()
            android.util.Log.d(r0, r2)
        L3ae:
            com.android.server.audio.AudioDeviceBroker r0 = r8.mDeviceBroker
            com.android.server.audio.AudioService$VolumeStreamState[] r2 = r8.mStreamStates
            r2 = r2[r9]
            int r2 = r2.getMaxIndex()
            r0.postSetLeAudioVolumeIndex(r1, r2, r9)
        L3bb:
            r0 = 134217728(0x8000000, float:3.85186E-34)
            if (r12 != r0) goto L3f9
            int r0 = r27.getBluetoothContextualVolumeStream()
            if (r9 != r0) goto L3f9
            boolean r0 = com.android.server.audio.AudioService.DEBUG_VOL
            if (r0 == 0) goto L3eb
            java.lang.String r0 = "AS.AudioService"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "adjustSreamVolume postSetHearingAidVolumeIndex index="
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r1)
            java.lang.String r3 = " stream="
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r9)
            java.lang.String r2 = r2.toString()
            android.util.Log.d(r0, r2)
        L3eb:
            com.android.server.audio.AudioDeviceBroker r0 = r8.mDeviceBroker
            r0.postSetHearingAidVolumeIndex(r1, r9)
            goto L3f9
        L3f1:
            r5 = r33
            r6 = r36
            r4 = r15
            r15 = r9
            r9 = r28
        L3f9:
            com.android.server.audio.AudioService$VolumeStreamState[] r0 = r8.mStreamStates
            r0 = r0[r9]
            int r3 = r0.getIndex(r12)
            if (r26 == 0) goto L4be
            java.lang.Object r1 = r8.mHdmiClientLock
            monitor-enter(r1)
            android.hardware.hdmi.HdmiControlManager r0 = r8.mHdmiManager     // Catch: java.lang.Throwable -> L4b7
            if (r0 == 0) goto L4b3
            android.hardware.hdmi.HdmiPlaybackClient r0 = r8.mHdmiPlaybackClient     // Catch: java.lang.Throwable -> L4b7
            android.hardware.hdmi.HdmiTvClient r2 = r8.mHdmiTvClient     // Catch: java.lang.Throwable -> L4b7
            if (r2 == 0) goto L412
            r0 = r2
            goto L413
        L412:
            r2 = r0
        L413:
            if (r2 == 0) goto L4a4
            boolean r0 = r8.mHdmiCecVolumeControlEnabled     // Catch: java.lang.Throwable -> L4b7
            if (r0 == 0) goto L4a4
            r0 = 3
            if (r4 != r0) goto L4a4
            boolean r0 = r8.isFullVolumeDevice(r12)     // Catch: java.lang.Throwable -> L4b7
            if (r0 == 0) goto L49f
            r0 = 0
            switch(r10) {
                case -100: goto L436;
                case -1: goto L430;
                case 1: goto L42a;
                case 100: goto L436;
                case 101: goto L436;
                default: goto L426;
            }
        L426:
            r33 = r5
            r5 = r0
            goto L43b
        L42a:
            r0 = 24
            r33 = r5
            r5 = r0
            goto L43b
        L430:
            r0 = 25
            r33 = r5
            r5 = r0
            goto L43b
        L436:
            r0 = 164(0xa4, float:2.3E-43)
            r33 = r5
            r5 = r0
        L43b:
            if (r5 == 0) goto L49a
            long r16 = android.os.Binder.clearCallingIdentity()     // Catch: java.lang.Throwable -> L4bc
            switch(r13) {
                case 0: goto L459;
                case 1: goto L450;
                case 2: goto L447;
                default: goto L444;
            }
        L444:
            java.lang.String r0 = "AS.AudioService"
            goto L46c
        L447:
            r0 = 0
            r2.sendVolumeKeyEvent(r5, r0)     // Catch: java.lang.Throwable -> L466
            r30 = r2
            r18 = r5
            goto L486
        L450:
            r0 = 1
            r2.sendVolumeKeyEvent(r5, r0)     // Catch: java.lang.Throwable -> L466
            r30 = r2
            r18 = r5
            goto L486
        L459:
            r0 = 1
            r2.sendVolumeKeyEvent(r5, r0)     // Catch: java.lang.Throwable -> L466
            r0 = 0
            r2.sendVolumeKeyEvent(r5, r0)     // Catch: java.lang.Throwable -> L466
            r30 = r2
            r18 = r5
            goto L486
        L466:
            r0 = move-exception
            r30 = r2
            r18 = r5
            goto L495
        L46c:
            r30 = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L48c
            r2.<init>()     // Catch: java.lang.Throwable -> L48c
            r18 = r5
            java.lang.String r5 = "Invalid keyEventMode "
            java.lang.StringBuilder r2 = r2.append(r5)     // Catch: java.lang.Throwable -> L48a
            java.lang.StringBuilder r2 = r2.append(r13)     // Catch: java.lang.Throwable -> L48a
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L48a
            android.util.Log.e(r0, r2)     // Catch: java.lang.Throwable -> L48a
        L486:
            android.os.Binder.restoreCallingIdentity(r16)     // Catch: java.lang.Throwable -> L4bc
            goto L4a8
        L48a:
            r0 = move-exception
            goto L495
        L48c:
            r0 = move-exception
            r18 = r5
            goto L495
        L490:
            r0 = move-exception
            r30 = r2
            r18 = r5
        L495:
            android.os.Binder.restoreCallingIdentity(r16)     // Catch: java.lang.Throwable -> L4bc
            throw r0     // Catch: java.lang.Throwable -> L4bc
        L49a:
            r30 = r2
            r18 = r5
            goto L4a8
        L49f:
            r30 = r2
            r33 = r5
            goto L4a8
        L4a4:
            r30 = r2
            r33 = r5
        L4a8:
            r0 = 3
            if (r4 != r0) goto L4b5
            if (r15 != r3) goto L4af
            if (r14 == 0) goto L4b5
        L4af:
            r8.maybeSendSystemAudioStatusCommand(r14)     // Catch: java.lang.Throwable -> L4bc
            goto L4b5
        L4b3:
            r33 = r5
        L4b5:
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L4bc
            goto L4c0
        L4b7:
            r0 = move-exception
            r33 = r5
        L4ba:
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L4bc
            throw r0
        L4bc:
            r0 = move-exception
            goto L4ba
        L4be:
            r33 = r5
        L4c0:
            r1 = r27
            r2 = r28
            r16 = r3
            r3 = r15
            r17 = r4
            r4 = r16
            r18 = r33
            r5 = r7
            r6 = r12
            r1.sendVolumeUpdate(r2, r3, r4, r5, r6)
            return
        L4d3:
            r0 = move-exception
            r25 = r5
            r12 = r6
            r18 = r7
            r17 = r15
        L4db:
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L4dd
            throw r0
        L4dd:
            r0 = move-exception
            goto L4db
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.AudioService.adjustStreamVolume(int, int, int, java.lang.String, java.lang.String, int, int, java.lang.String, boolean, int):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUnmuteStream(int stream, int flags) {
        boolean wasMuted;
        synchronized (VolumeStreamState.class) {
            VolumeStreamState streamState = this.mStreamStates[stream];
            wasMuted = streamState.mute(false);
            int device = getDeviceForStream(stream);
            int index = streamState.getIndex(device);
            sendVolumeUpdate(stream, index, index, flags, device);
        }
        if (stream == 3 && wasMuted) {
            synchronized (this.mHdmiClientLock) {
                maybeSendSystemAudioStatusCommand(true);
            }
        }
    }

    private void maybeSendSystemAudioStatusCommand(boolean isMuteAdjust) {
        if (this.mHdmiAudioSystemClient == null || !this.mHdmiSystemAudioSupported || !this.mHdmiCecVolumeControlEnabled) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        this.mHdmiAudioSystemClient.sendReportAudioStatusCecCommand(isMuteAdjust, getStreamVolume(3), getStreamMaxVolume(3), isStreamMute(3));
        Binder.restoreCallingIdentity(identity);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class StreamVolumeCommand {
        public final int mDevice;
        public final int mFlags;
        public final int mIndex;
        public final int mStreamType;

        StreamVolumeCommand(int streamType, int index, int flags, int device) {
            this.mStreamType = streamType;
            this.mIndex = index;
            this.mFlags = flags;
            this.mDevice = device;
        }

        public String toString() {
            return "{streamType=" + this.mStreamType + ",index=" + this.mIndex + ",flags=" + this.mFlags + ",device=" + this.mDevice + '}';
        }
    }

    private int getNewRingerMode(int stream, int index, int flags) {
        if (this.mIsSingleVolume) {
            return getRingerModeExternal();
        }
        if ((flags & 2) != 0 || stream == getUiSoundsStreamType()) {
            if (index == 0) {
                if (this.mHasVibrator) {
                    return 1;
                }
                return this.mVolumePolicy.volumeDownToEnterSilent ? 0 : 2;
            }
            return 2;
        }
        return getRingerModeExternal();
    }

    private boolean isAndroidNPlus(String caller) {
        try {
            ApplicationInfo applicationInfo = this.mContext.getPackageManager().getApplicationInfoAsUser(caller, 0, UserHandle.getUserId(Binder.getCallingUid()));
            if (applicationInfo.targetSdkVersion >= 24) {
                return true;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    private boolean wouldToggleZenMode(int newMode) {
        if (getRingerModeExternal() != 0 || newMode == 0) {
            return getRingerModeExternal() != 0 && newMode == 0;
        }
        return true;
    }

    private void onSetStreamVolume(int streamType, int index, int flags, int device, String caller, boolean hasModifyAudioSettings) {
        int stream = mStreamVolumeAlias[streamType];
        setStreamVolumeInt(stream, index, device, false, caller, hasModifyAudioSettings);
        if ((flags & 2) != 0 || stream == getUiSoundsStreamType()) {
            setRingerMode(getNewRingerMode(stream, index, flags), "AS.AudioService.onSetStreamVolume", false);
        }
        if (streamType != 6) {
            this.mStreamStates[stream].mute(index == 0);
        }
    }

    private void enforceModifyAudioRoutingPermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") != 0) {
            throw new SecurityException("Missing MODIFY_AUDIO_ROUTING permission");
        }
    }

    private void enforceAccessUltrasoundPermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.ACCESS_ULTRASOUND") != 0) {
            throw new SecurityException("Missing ACCESS_ULTRASOUND permission");
        }
    }

    private void enforceQueryStatePermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.QUERY_AUDIO_STATE") != 0) {
            throw new SecurityException("Missing QUERY_AUDIO_STATE permissions");
        }
    }

    private void enforceQueryStateOrModifyRoutingPermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.QUERY_AUDIO_STATE") != 0) {
            throw new SecurityException("Missing MODIFY_AUDIO_ROUTING or QUERY_AUDIO_STATE permissions");
        }
    }

    private void enforceCallAudioInterceptionPermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.CALL_AUDIO_INTERCEPTION") != 0) {
            throw new SecurityException("Missing CALL_AUDIO_INTERCEPTION permission");
        }
    }

    public void setVolumeIndexForAttributes(AudioAttributes attr, int index, int flags, String callingPackage, String attributionTag) {
        int[] legacyStreamTypes;
        enforceModifyAudioRoutingPermission();
        Objects.requireNonNull(attr, "attr must not be null");
        int volumeGroup = getVolumeGroupIdForAttributes(attr);
        SparseArray<VolumeGroupState> sparseArray = sVolumeGroupStates;
        if (sparseArray.indexOfKey(volumeGroup) < 0) {
            Log.e(TAG, ": no volume group found for attributes " + attr.toString());
            return;
        }
        VolumeGroupState vgs = sparseArray.get(volumeGroup);
        sVolumeLogger.log(new AudioServiceEvents.VolumeEvent(8, attr, vgs.name(), index, flags, callingPackage));
        vgs.setVolumeIndex(index, flags);
        for (int groupedStream : vgs.getLegacyStreamTypes()) {
            try {
                ensureValidStreamType(groupedStream);
                setStreamVolume(groupedStream, index, flags, callingPackage, callingPackage, attributionTag, Binder.getCallingUid(), true);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "volume group " + volumeGroup + " has internal streams (" + groupedStream + "), do not change associated stream volume");
            }
        }
    }

    private AudioVolumeGroup getAudioVolumeGroupById(int volumeGroupId) {
        for (AudioVolumeGroup avg : AudioVolumeGroup.getAudioVolumeGroups()) {
            if (avg.getId() == volumeGroupId) {
                return avg;
            }
        }
        Log.e(TAG, ": invalid volume group id: " + volumeGroupId + " requested");
        return null;
    }

    public int getVolumeIndexForAttributes(AudioAttributes attr) {
        enforceModifyAudioRoutingPermission();
        Objects.requireNonNull(attr, "attr must not be null");
        int volumeGroup = getVolumeGroupIdForAttributes(attr);
        SparseArray<VolumeGroupState> sparseArray = sVolumeGroupStates;
        if (sparseArray.indexOfKey(volumeGroup) < 0) {
            throw new IllegalArgumentException("No volume group for attributes " + attr);
        }
        VolumeGroupState vgs = sparseArray.get(volumeGroup);
        return vgs.getVolumeIndex();
    }

    public int getMaxVolumeIndexForAttributes(AudioAttributes attr) {
        enforceModifyAudioRoutingPermission();
        Objects.requireNonNull(attr, "attr must not be null");
        return AudioSystem.getMaxVolumeIndexForAttributes(attr);
    }

    public int getMinVolumeIndexForAttributes(AudioAttributes attr) {
        enforceModifyAudioRoutingPermission();
        Objects.requireNonNull(attr, "attr must not be null");
        return AudioSystem.getMinVolumeIndexForAttributes(attr);
    }

    public void setStreamVolume(int streamType, int index, int flags, String callingPackage) {
        setStreamVolumeWithAttribution(streamType, index, flags, callingPackage, null);
    }

    public void setStreamVolumeWithAttribution(int streamType, int index, int flags, String callingPackage, String attributionTag) {
        if (streamType == 10 && !canChangeAccessibilityVolume()) {
            Log.w(TAG, "Trying to call setStreamVolume() for a11y without CHANGE_ACCESSIBILITY_VOLUME  callingPackage=" + callingPackage);
        } else if (streamType == 0 && index == 0 && this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            Log.w(TAG, "Trying to call setStreamVolume() for STREAM_VOICE_CALL and index 0 without MODIFY_PHONE_STATE  callingPackage=" + callingPackage);
        } else if (streamType == 11 && this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") != 0) {
            Log.w(TAG, "Trying to call setStreamVolume() for STREAM_ASSISTANT without MODIFY_AUDIO_ROUTING  callingPackage=" + callingPackage);
        } else {
            sVolumeLogger.log(new AudioServiceEvents.VolumeEvent(2, streamType, index, flags, callingPackage));
            setStreamVolume(streamType, index, flags, callingPackage, callingPackage, attributionTag, Binder.getCallingUid(), callingOrSelfHasAudioSettingsPermission());
        }
    }

    public boolean isUltrasoundSupported() {
        enforceAccessUltrasoundPermission();
        return AudioSystem.isUltrasoundSupported();
    }

    private boolean canChangeAccessibilityVolume() {
        synchronized (this.mAccessibilityServiceUidsLock) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_ACCESSIBILITY_VOLUME") == 0) {
                return true;
            }
            if (this.mAccessibilityServiceUids != null) {
                int callingUid = Binder.getCallingUid();
                int i = 0;
                while (true) {
                    int[] iArr = this.mAccessibilityServiceUids;
                    if (i >= iArr.length) {
                        break;
                    } else if (iArr[i] == callingUid) {
                        return true;
                    } else {
                        i++;
                    }
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getBluetoothContextualVolumeStream() {
        return getBluetoothContextualVolumeStream(this.mMode.get());
    }

    private int getBluetoothContextualVolumeStream(int mode) {
        switch (mode) {
            case 2:
            case 3:
                return 0;
            default:
                if (this.mVoicePlaybackActive.get()) {
                    return 0;
                }
                return 3;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPlaybackConfigChange(List<AudioPlaybackConfiguration> configs) {
        boolean voiceActive;
        Iterator<AudioPlaybackConfiguration> it = configs.iterator();
        while (true) {
            if (!it.hasNext()) {
                voiceActive = false;
                break;
            }
            AudioPlaybackConfiguration config = it.next();
            int usage = config.getAudioAttributes().getUsage();
            if (usage == 2 || usage == 3) {
                if (config.getPlayerState() == 2) {
                    voiceActive = true;
                    break;
                }
            }
        }
        if (this.mVoicePlaybackActive.getAndSet(voiceActive) != voiceActive) {
            updateHearingAidVolumeOnVoiceActivityUpdate();
        }
        synchronized (this.mDeviceBroker.mSetModeLock) {
            boolean updateAudioMode = false;
            int existingMsgPolicy = 2;
            int delay = CHECK_MODE_FOR_UID_PERIOD_MS;
            Iterator<SetModeDeathHandler> it2 = this.mSetModeDeathHandlers.iterator();
            while (it2.hasNext()) {
                SetModeDeathHandler h = it2.next();
                boolean wasActive = h.isActive();
                h.setPlaybackActive(false);
                Iterator<AudioPlaybackConfiguration> it3 = configs.iterator();
                while (true) {
                    if (!it3.hasNext()) {
                        break;
                    }
                    AudioPlaybackConfiguration config2 = it3.next();
                    int usage2 = config2.getAudioAttributes().getUsage();
                    if (config2.getClientUid() == h.getUid() && ((usage2 == 2 || usage2 == 3) && config2.getPlayerState() == 2)) {
                        h.setPlaybackActive(true);
                        break;
                    }
                }
                if (wasActive != h.isActive()) {
                    updateAudioMode = true;
                    if (h.isActive() && h == getAudioModeOwnerHandler()) {
                        existingMsgPolicy = 0;
                        delay = 0;
                    }
                }
            }
            if (updateAudioMode) {
                sendMsg(this.mAudioHandler, 36, existingMsgPolicy, -1, Process.myPid(), this.mContext.getPackageName(), delay);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onRecordingConfigChange(List<AudioRecordingConfiguration> configs) {
        synchronized (this.mDeviceBroker.mSetModeLock) {
            boolean updateAudioMode = false;
            int existingMsgPolicy = 2;
            int delay = CHECK_MODE_FOR_UID_PERIOD_MS;
            Iterator<SetModeDeathHandler> it = this.mSetModeDeathHandlers.iterator();
            while (it.hasNext()) {
                SetModeDeathHandler h = it.next();
                boolean wasActive = h.isActive();
                h.setRecordingActive(false);
                Iterator<AudioRecordingConfiguration> it2 = configs.iterator();
                while (true) {
                    if (!it2.hasNext()) {
                        break;
                    }
                    AudioRecordingConfiguration config = it2.next();
                    if (config.getClientUid() == h.getUid() && config.getAudioSource() == 7) {
                        h.setRecordingActive(true);
                        break;
                    }
                }
                if (wasActive != h.isActive()) {
                    updateAudioMode = true;
                    if (h.isActive() && h == getAudioModeOwnerHandler()) {
                        existingMsgPolicy = 0;
                        delay = 0;
                    }
                }
            }
            if (updateAudioMode) {
                sendMsg(this.mAudioHandler, 36, existingMsgPolicy, -1, Process.myPid(), this.mContext.getPackageName(), delay);
            }
        }
    }

    private void dumpAudioMode(PrintWriter pw) {
        pw.println("\nAudio mode: ");
        pw.println("- Requested mode = " + AudioSystem.modeToString(getMode()));
        pw.println("- Actual mode = " + AudioSystem.modeToString(this.mMode.get()));
        pw.println("- Mode owner: ");
        SetModeDeathHandler hdlr = getAudioModeOwnerHandler();
        if (hdlr != null) {
            hdlr.dump(pw, -1);
        } else {
            pw.println("   None");
        }
        pw.println("- Mode owner stack: ");
        if (this.mSetModeDeathHandlers.isEmpty()) {
            pw.println("   Empty");
            return;
        }
        for (int i = 0; i < this.mSetModeDeathHandlers.size(); i++) {
            this.mSetModeDeathHandlers.get(i).dump(pw, i);
        }
    }

    private void updateHearingAidVolumeOnVoiceActivityUpdate() {
        int streamType = getBluetoothContextualVolumeStream();
        int index = getStreamVolume(streamType);
        sVolumeLogger.log(new AudioServiceEvents.VolumeEvent(6, this.mVoicePlaybackActive.get(), streamType, index));
        this.mDeviceBroker.postSetHearingAidVolumeIndex(index * 10, streamType);
    }

    void updateAbsVolumeMultiModeDevices(int oldMode, int newMode) {
        if (oldMode == newMode) {
            return;
        }
        switch (newMode) {
            case 0:
            case 2:
            case 3:
                int streamType = getBluetoothContextualVolumeStream(newMode);
                Set<Integer> deviceTypes = getDeviceSetForStreamDirect(streamType);
                Set<Integer> absVolumeMultiModeCaseDevices = AudioSystem.intersectionAudioDeviceTypes(this.mAbsVolumeMultiModeCaseDevices, deviceTypes);
                if (!absVolumeMultiModeCaseDevices.isEmpty() && AudioSystem.isSingleAudioDeviceType(absVolumeMultiModeCaseDevices, 134217728)) {
                    int index = getStreamVolume(streamType);
                    sVolumeLogger.log(new AudioServiceEvents.VolumeEvent(7, newMode, streamType, index));
                    this.mDeviceBroker.postSetHearingAidVolumeIndex(index * 10, streamType);
                    return;
                }
                return;
            case 1:
                return;
            default:
                return;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4037=4] */
    /* JADX WARN: Can't wrap try/catch for region: R(19:34|(3:35|36|37)|(5:(3:128|129|(16:(1:134)|135|44|(2:(1:126)|127)|48|(1:52)|63|(3:107|108|(11:110|(2:112|(2:117|118)(1:116))(1:119)|67|68|(3:96|97|98)(4:70|71|72|73)|74|75|76|21e|87|88)(1:120))(1:65)|66|67|68|(0)(0)|74|75|76|21e))|74|75|76|21e)|39|(1:43)|44|(1:46)|121|123|(0)|127|48|(2:50|52)|63|(0)(0)|66|67|68|(0)(0)) */
    /* JADX WARN: Code restructure failed: missing block: B:113:0x0243, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:114:0x0244, code lost:
        r18 = r7;
     */
    /* JADX WARN: Removed duplicated region for block: B:123:0x01a3 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:128:0x01e0 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:61:0x013d A[Catch: all -> 0x024f, TryCatch #8 {all -> 0x024f, blocks: (B:40:0x00d1, B:42:0x00dd, B:45:0x00e3, B:46:0x0107, B:65:0x0170, B:67:0x0176, B:56:0x0131, B:58:0x0137, B:61:0x013d, B:62:0x015f, B:47:0x010f, B:49:0x0115, B:51:0x0119), top: B:138:0x00d1 }] */
    /* JADX WARN: Removed duplicated region for block: B:85:0x01d7  */
    /* JADX WARN: Removed duplicated region for block: B:93:0x01fb  */
    /* JADX WARN: Removed duplicated region for block: B:99:0x021f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void setStreamVolume(int streamType, int index, int flags, String callingPackage, String caller, String attributionTag, int uid, boolean hasModifyAudioSettings) {
        Object obj;
        int oldIndex;
        int index2;
        int i;
        int index3;
        int flags2;
        int flags3;
        int i2;
        int oldIndex2;
        int index4;
        boolean z = DEBUG_VOL;
        if (z) {
            Log.d(TAG, "setStreamVolume(stream=" + streamType + ", index=" + index + ", calling=" + callingPackage + ")");
        }
        if (this.mUseFixedVolume) {
            return;
        }
        ensureValidStreamType(streamType);
        int streamTypeAlias = mStreamVolumeAlias[streamType];
        VolumeStreamState streamState = this.mStreamStates[streamTypeAlias];
        int device = getDeviceForStream(streamType);
        if (!AudioSystem.DEVICE_OUT_ALL_A2DP_SET.contains(Integer.valueOf(device)) && !AudioSystem.DEVICE_OUT_ALL_BLE_SET.contains(Integer.valueOf(device)) && (flags & 64) != 0) {
            return;
        }
        if (!checkNoteAppOp(STREAM_VOLUME_OPS[streamTypeAlias], uid == 1000 ? UserHandle.getUid(getCurrentUserId(), UserHandle.getAppId(uid)) : uid, callingPackage, attributionTag)) {
            return;
        }
        if (isAndroidNPlus(callingPackage) && wouldToggleZenMode(getNewRingerMode(streamTypeAlias, index, flags)) && !this.mNm.isNotificationPolicyAccessGrantedForPackage(callingPackage)) {
            throw new SecurityException("Not allowed to change Do Not Disturb state");
        }
        if (!volumeAdjustmentAllowedByDnd(streamTypeAlias, flags)) {
            return;
        }
        Object obj2 = this.mSafeMediaVolumeStateLock;
        synchronized (obj2) {
            try {
                this.mPendingVolumeCommand = null;
                oldIndex = streamState.getIndex(device);
                index2 = rescaleIndex(index * 10, streamType, streamTypeAlias);
            } catch (Throwable th) {
                th = th;
                obj = obj2;
            }
            try {
                if (streamTypeAlias == 3) {
                    try {
                        if (AudioSystem.DEVICE_OUT_ALL_A2DP_SET.contains(Integer.valueOf(device)) && (flags & 64) == 0) {
                            if (z) {
                                Log.d(TAG, "setStreamVolume postSetAvrcpAbsoluteVolumeIndex index=" + index2 + "stream=" + streamType);
                            }
                            this.mDeviceBroker.postSetAvrcpAbsoluteVolumeIndex(index2 / 10);
                            if ((device != 536870912 || device == 536870914) && streamType == getBluetoothContextualVolumeStream() && (flags & 64) == 0) {
                                if (z) {
                                    Log.d(TAG, "adjustSreamVolume postSetLeAudioVolumeIndex index=" + index2 + " stream=" + streamType);
                                }
                                this.mDeviceBroker.postSetLeAudioVolumeIndex(index2, this.mStreamStates[streamType].getMaxIndex(), streamType);
                            }
                            if (device == 134217728 && streamType == getBluetoothContextualVolumeStream()) {
                                Log.i(TAG, "setStreamVolume postSetHearingAidVolumeIndex index=" + index2 + " stream=" + streamType);
                                this.mDeviceBroker.postSetHearingAidVolumeIndex(index2, streamType);
                            }
                            int flags4 = flags & (-33);
                            if (streamTypeAlias != 3) {
                                try {
                                    if (isFixedVolumeDevice(device)) {
                                        int flags5 = flags4 | 32;
                                        if (index2 != 0) {
                                            i = 3;
                                            if (this.mSafeMediaVolumeState == 3 && this.mSafeMediaVolumeDevices.contains(Integer.valueOf(device))) {
                                                index3 = safeMediaVolumeIndex(device);
                                                flags2 = flags5;
                                            } else {
                                                index3 = streamState.getMaxIndex();
                                                flags2 = flags5;
                                            }
                                        } else {
                                            i = 3;
                                            index3 = index2;
                                            flags2 = flags5;
                                        }
                                        if (checkSafeMediaVolume(streamTypeAlias, index3, device)) {
                                            flags3 = flags2;
                                            i2 = i;
                                            oldIndex2 = oldIndex;
                                            obj = obj2;
                                            try {
                                                onSetStreamVolume(streamType, index3, flags3, device, caller, hasModifyAudioSettings);
                                                index4 = this.mStreamStates[streamType].getIndex(device);
                                            } catch (Throwable th2) {
                                                th = th2;
                                                while (true) {
                                                    break;
                                                    break;
                                                }
                                                throw th;
                                            }
                                        } else {
                                            try {
                                                this.mVolumeController.postDisplaySafeVolumeWarning(flags2);
                                                this.mPendingVolumeCommand = new StreamVolumeCommand(streamType, index3, flags2, device);
                                                flags3 = flags2;
                                                i2 = i;
                                                oldIndex2 = oldIndex;
                                                obj = obj2;
                                                index4 = index3;
                                            } catch (Throwable th3) {
                                                th = th3;
                                                obj = obj2;
                                                while (true) {
                                                    try {
                                                        break;
                                                    } catch (Throwable th4) {
                                                        th = th4;
                                                    }
                                                }
                                                throw th;
                                            }
                                        }
                                        synchronized (this.mHdmiClientLock) {
                                            if (streamTypeAlias == i2 && oldIndex2 != index4) {
                                                maybeSendSystemAudioStatusCommand(false);
                                            }
                                        }
                                        sendVolumeUpdate(streamType, oldIndex2, index4, flags3, device);
                                        return;
                                    }
                                    i = 3;
                                } catch (Throwable th5) {
                                    th = th5;
                                    obj = obj2;
                                    while (true) {
                                        break;
                                        break;
                                    }
                                    throw th;
                                }
                            } else {
                                i = 3;
                            }
                            index3 = index2;
                            flags2 = flags4;
                            if (checkSafeMediaVolume(streamTypeAlias, index3, device)) {
                            }
                            synchronized (this.mHdmiClientLock) {
                            }
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        obj = obj2;
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                }
                synchronized (this.mHdmiClientLock) {
                }
            } catch (Throwable th7) {
                th = th7;
                while (true) {
                    break;
                    break;
                }
                throw th;
            }
            if (isAbsoluteVolumeDevice(device) && (flags & 8192) == 0) {
                AbsoluteVolumeDeviceInfo info = this.mAbsoluteVolumeDeviceInfoMap.get(Integer.valueOf(device));
                dispatchAbsoluteVolumeChanged(streamType, info, index2);
            }
            if (device != 536870912) {
            }
            if (z) {
            }
            this.mDeviceBroker.postSetLeAudioVolumeIndex(index2, this.mStreamStates[streamType].getMaxIndex(), streamType);
            if (device == 134217728) {
                Log.i(TAG, "setStreamVolume postSetHearingAidVolumeIndex index=" + index2 + " stream=" + streamType);
                this.mDeviceBroker.postSetHearingAidVolumeIndex(index2, streamType);
            }
            int flags42 = flags & (-33);
            if (streamTypeAlias != 3) {
            }
            index3 = index2;
            flags2 = flags42;
            if (checkSafeMediaVolume(streamTypeAlias, index3, device)) {
            }
        }
    }

    private int getVolumeGroupIdForAttributes(AudioAttributes attributes) {
        Objects.requireNonNull(attributes, "attributes must not be null");
        int volumeGroupId = getVolumeGroupIdForAttributesInt(attributes);
        if (volumeGroupId != -1) {
            return volumeGroupId;
        }
        return getVolumeGroupIdForAttributesInt(AudioProductStrategy.getDefaultAttributes());
    }

    private int getVolumeGroupIdForAttributesInt(AudioAttributes attributes) {
        Objects.requireNonNull(attributes, "attributes must not be null");
        for (AudioProductStrategy productStrategy : AudioProductStrategy.getAudioProductStrategies()) {
            int volumeGroupId = productStrategy.getVolumeGroupIdForAudioAttributes(attributes);
            if (volumeGroupId != -1) {
                return volumeGroupId;
            }
        }
        return -1;
    }

    private void dispatchAbsoluteVolumeChanged(int streamType, AbsoluteVolumeDeviceInfo deviceInfo, int index) {
        VolumeInfo volumeInfo = deviceInfo.getMatchingVolumeInfoForStream(streamType);
        if (volumeInfo != null) {
            try {
                deviceInfo.mCallback.dispatchDeviceVolumeChanged(deviceInfo.mDevice, new VolumeInfo.Builder(volumeInfo).setVolumeIndex(rescaleIndex(index, streamType, volumeInfo)).build());
            } catch (RemoteException e) {
                Log.w(TAG, "Couldn't dispatch absolute volume behavior volume change");
            }
        }
    }

    private void dispatchAbsoluteVolumeAdjusted(int streamType, AbsoluteVolumeDeviceInfo deviceInfo, int index, int direction, int mode) {
        VolumeInfo volumeInfo = deviceInfo.getMatchingVolumeInfoForStream(streamType);
        if (volumeInfo != null) {
            try {
                deviceInfo.mCallback.dispatchDeviceVolumeAdjusted(deviceInfo.mDevice, new VolumeInfo.Builder(volumeInfo).setVolumeIndex(rescaleIndex(index, streamType, volumeInfo)).build(), direction, mode);
            } catch (RemoteException e) {
                Log.w(TAG, "Couldn't dispatch absolute volume behavior volume adjustment");
            }
        }
    }

    private boolean volumeAdjustmentAllowedByDnd(int streamTypeAlias, int flags) {
        switch (this.mNm.getZenMode()) {
            case 0:
                return true;
            case 1:
            case 2:
            case 3:
                return (isStreamMutedByRingerOrZenMode(streamTypeAlias) && !isUiSoundsStreamType(streamTypeAlias) && (flags & 2) == 0) ? false : true;
            default:
                return true;
        }
    }

    public void forceVolumeControlStream(int streamType, IBinder cb) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            return;
        }
        if (DEBUG_VOL) {
            Log.d(TAG, String.format("forceVolumeControlStream(%d)", Integer.valueOf(streamType)));
        }
        synchronized (this.mForceControlStreamLock) {
            if (this.mVolumeControlStream != -1 && streamType != -1) {
                this.mUserSelectedVolumeControlStream = true;
            }
            this.mVolumeControlStream = streamType;
            if (streamType == -1) {
                ForceControlStreamClient forceControlStreamClient = this.mForceControlStreamClient;
                if (forceControlStreamClient != null) {
                    forceControlStreamClient.release();
                    this.mForceControlStreamClient = null;
                }
                this.mUserSelectedVolumeControlStream = false;
            } else {
                ForceControlStreamClient forceControlStreamClient2 = this.mForceControlStreamClient;
                if (forceControlStreamClient2 == null) {
                    this.mForceControlStreamClient = new ForceControlStreamClient(cb);
                } else if (forceControlStreamClient2.getBinder() == cb) {
                    Log.d(TAG, "forceVolumeControlStream cb:" + cb + " is already linked.");
                } else {
                    this.mForceControlStreamClient.release();
                    this.mForceControlStreamClient = new ForceControlStreamClient(cb);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private class ForceControlStreamClient implements IBinder.DeathRecipient {
        private IBinder mCb;

        ForceControlStreamClient(IBinder cb) {
            if (cb != null) {
                try {
                    cb.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    Log.w(AudioService.TAG, "ForceControlStreamClient() could not link to " + cb + " binder death");
                    cb = null;
                }
            }
            this.mCb = cb;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (AudioService.this.mForceControlStreamLock) {
                Log.w(AudioService.TAG, "SCO client died");
                if (AudioService.this.mForceControlStreamClient != this) {
                    Log.w(AudioService.TAG, "unregistered control stream client died");
                } else {
                    AudioService.this.mForceControlStreamClient = null;
                    AudioService.this.mVolumeControlStream = -1;
                    AudioService.this.mUserSelectedVolumeControlStream = false;
                }
            }
        }

        public void release() {
            IBinder iBinder = this.mCb;
            if (iBinder != null) {
                iBinder.unlinkToDeath(this, 0);
                this.mCb = null;
            }
        }

        public IBinder getBinder() {
            return this.mCb;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendBroadcastToAll(Intent intent) {
        if (!this.mSystemServer.isPrivileged()) {
            return;
        }
        intent.addFlags(67108864);
        intent.addFlags(268435456);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void sendStickyBroadcastToAll(Intent intent) {
        intent.addFlags(268435456);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getCurrentUserId() {
        long ident = Binder.clearCallingIdentity();
        try {
            UserInfo currentUser = ActivityManager.getService().getCurrentUser();
            int i = currentUser.id;
            Binder.restoreCallingIdentity(ident);
            return i;
        } catch (RemoteException e) {
            Binder.restoreCallingIdentity(ident);
            return 0;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    protected void sendVolumeUpdate(int streamType, int oldIndex, int index, int flags, int device) {
        int streamType2 = mStreamVolumeAlias[streamType];
        if (streamType2 == 3 && isFullVolumeDevice(device)) {
            flags &= -2;
        }
        this.mVolumeController.postVolumeChanged(streamType2, flags);
        int volume = getStreamVolume(streamType2);
        String pkgName = this.mMediaFocusControl.getCurrentAudioPackageName();
        ITranActivityManagerService.Instance().onAudioVolumeChange(streamType2, volume, device, pkgName);
    }

    private int updateFlagsForTvPlatform(int flags) {
        synchronized (this.mHdmiClientLock) {
            if (this.mHdmiTvClient != null && this.mHdmiSystemAudioSupported && this.mHdmiCecVolumeControlEnabled) {
                flags &= -2;
            }
        }
        return flags;
    }

    private void sendMasterMuteUpdate(boolean muted, int flags) {
        this.mVolumeController.postMasterMuteChanged(updateFlagsForTvPlatform(flags));
        broadcastMasterMuteStatus(muted);
    }

    private void broadcastMasterMuteStatus(boolean muted) {
        Intent intent = new Intent("android.media.MASTER_MUTE_CHANGED_ACTION");
        intent.putExtra("android.media.EXTRA_MASTER_VOLUME_MUTED", muted);
        intent.addFlags(603979776);
        sendStickyBroadcastToAll(intent);
    }

    private void setStreamVolumeInt(int streamType, int index, int device, boolean force, String caller, boolean hasModifyAudioSettings) {
        if (isFullVolumeDevice(device)) {
            return;
        }
        VolumeStreamState streamState = this.mStreamStates[streamType];
        if (streamState.setIndex(index, device, caller, hasModifyAudioSettings) || force) {
            sendMsg(this.mAudioHandler, 0, 2, device, 0, streamState, 0);
        }
    }

    public boolean isStreamMute(int streamType) {
        boolean z;
        if (streamType == Integer.MIN_VALUE) {
            streamType = getActiveStreamType(streamType);
        }
        synchronized (VolumeStreamState.class) {
            ensureValidStreamType(streamType);
            z = this.mStreamStates[streamType].mIsMuted;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class RmtSbmxFullVolDeathHandler implements IBinder.DeathRecipient {
        private IBinder mICallback;

        RmtSbmxFullVolDeathHandler(IBinder cb) {
            this.mICallback = cb;
            try {
                cb.linkToDeath(this, 0);
            } catch (RemoteException e) {
                Log.e(AudioService.TAG, "can't link to death", e);
            }
        }

        boolean isHandlerFor(IBinder cb) {
            return this.mICallback.equals(cb);
        }

        void forget() {
            try {
                this.mICallback.unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
                Log.e(AudioService.TAG, "error unlinking to death", e);
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Log.w(AudioService.TAG, "Recorder with remote submix at full volume died " + this.mICallback);
            AudioService.this.forceRemoteSubmixFullVolume(false, this.mICallback);
        }
    }

    private boolean discardRmtSbmxFullVolDeathHandlerFor(IBinder cb) {
        Iterator<RmtSbmxFullVolDeathHandler> it = this.mRmtSbmxFullVolDeathHandlers.iterator();
        while (it.hasNext()) {
            RmtSbmxFullVolDeathHandler handler = it.next();
            if (handler.isHandlerFor(cb)) {
                handler.forget();
                this.mRmtSbmxFullVolDeathHandlers.remove(handler);
                return true;
            }
        }
        return false;
    }

    private boolean hasRmtSbmxFullVolDeathHandlerFor(IBinder cb) {
        Iterator<RmtSbmxFullVolDeathHandler> it = this.mRmtSbmxFullVolDeathHandlers.iterator();
        while (it.hasNext()) {
            if (it.next().isHandlerFor(cb)) {
                return true;
            }
        }
        return false;
    }

    public void forceRemoteSubmixFullVolume(boolean startForcing, IBinder cb) {
        int i;
        if (cb == null) {
            return;
        }
        if (this.mContext.checkCallingOrSelfPermission("android.permission.CAPTURE_AUDIO_OUTPUT") != 0) {
            Log.w(TAG, "Trying to call forceRemoteSubmixFullVolume() without CAPTURE_AUDIO_OUTPUT");
            return;
        }
        synchronized (this.mRmtSbmxFullVolDeathHandlers) {
            boolean applyRequired = false;
            if (startForcing) {
                if (!hasRmtSbmxFullVolDeathHandlerFor(cb)) {
                    this.mRmtSbmxFullVolDeathHandlers.add(new RmtSbmxFullVolDeathHandler(cb));
                    if (this.mRmtSbmxFullVolRefCount == 0) {
                        this.mFullVolumeDevices.add(32768);
                        this.mFixedVolumeDevices.add(32768);
                        applyRequired = true;
                    }
                    this.mRmtSbmxFullVolRefCount++;
                }
            } else if (discardRmtSbmxFullVolDeathHandlerFor(cb) && (i = this.mRmtSbmxFullVolRefCount) > 0) {
                int i2 = i - 1;
                this.mRmtSbmxFullVolRefCount = i2;
                if (i2 == 0) {
                    this.mFullVolumeDevices.remove(32768);
                    this.mFixedVolumeDevices.remove(32768);
                    applyRequired = true;
                }
            }
            if (applyRequired) {
                checkAllFixedVolumeDevices(3);
                this.mStreamStates[3].applyAllVolumes();
            }
        }
    }

    private void setMasterMuteInternal(boolean mute, int flags, String callingPackage, int uid, int userId, int pid, String attributionTag) {
        if (uid == 1000) {
            uid = UserHandle.getUid(userId, UserHandle.getAppId(uid));
        }
        if (!mute && !checkNoteAppOp(33, uid, callingPackage, attributionTag)) {
            return;
        }
        if (userId != UserHandle.getCallingUserId() && this.mContext.checkPermission("android.permission.INTERACT_ACROSS_USERS_FULL", pid, uid) != 0) {
            return;
        }
        setMasterMuteInternalNoCallerCheck(mute, flags, userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setMasterMuteInternalNoCallerCheck(boolean mute, int flags, int userId) {
        if (DEBUG_VOL) {
            Log.d(TAG, String.format("Master mute %s, %d, user=%d", Boolean.valueOf(mute), Integer.valueOf(flags), Integer.valueOf(userId)));
        }
        if (!isPlatformAutomotive() && this.mUseFixedVolume) {
            return;
        }
        if (((isPlatformAutomotive() && userId == 0) || getCurrentUserId() == userId) && mute != AudioSystem.getMasterMute()) {
            AudioSystem.setMasterMute(mute);
            sendMasterMuteUpdate(mute, flags);
        }
    }

    public boolean isMasterMute() {
        return AudioSystem.getMasterMute();
    }

    public void setMasterMute(boolean mute, int flags, String callingPackage, int userId, String attributionTag) {
        enforceModifyAudioRoutingPermission();
        setMasterMuteInternal(mute, flags, callingPackage, Binder.getCallingUid(), userId, Binder.getCallingPid(), attributionTag);
    }

    public int getStreamVolume(int streamType) {
        int i;
        ensureValidStreamType(streamType);
        int device = getDeviceForStream(streamType);
        synchronized (VolumeStreamState.class) {
            int index = this.mStreamStates[streamType].getIndex(device);
            if (this.mStreamStates[streamType].mIsMuted) {
                index = 0;
            }
            if (index != 0 && mStreamVolumeAlias[streamType] == 3 && isFixedVolumeDevice(device)) {
                index = this.mStreamStates[streamType].getMaxIndex();
            }
            i = (index + 5) / 10;
        }
        return i;
    }

    public int getStreamMaxVolume(int streamType) {
        ensureValidStreamType(streamType);
        return (this.mStreamStates[streamType].getMaxIndex() + 5) / 10;
    }

    public int getStreamMinVolume(int streamType) {
        ensureValidStreamType(streamType);
        boolean isPrivileged = Binder.getCallingUid() == 1000 || callingHasAudioSettingsPermission() || this.mContext.checkCallingPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0;
        return (this.mStreamStates[streamType].getMinIndex(isPrivileged) + 5) / 10;
    }

    public int getLastAudibleStreamVolume(int streamType) {
        enforceQueryStatePermission();
        ensureValidStreamType(streamType);
        int device = getDeviceForStream(streamType);
        return (this.mStreamStates[streamType].getIndex(device) + 5) / 10;
    }

    public VolumeInfo getDefaultVolumeInfo() {
        if (sDefaultVolumeInfo == null) {
            sDefaultVolumeInfo = new VolumeInfo.Builder(3).setMinVolumeIndex(getStreamMinVolume(3)).setMaxVolumeIndex(getStreamMaxVolume(3)).setMuted(false).build();
        }
        return sDefaultVolumeInfo;
    }

    public int getUiSoundsStreamType() {
        return this.mUseVolumeGroupAliases ? this.STREAM_VOLUME_ALIAS_VOICE[1] : mStreamVolumeAlias[1];
    }

    private boolean isUiSoundsStreamType(int aliasStreamType) {
        if (!this.mUseVolumeGroupAliases) {
            return aliasStreamType == mStreamVolumeAlias[1];
        }
        int[] iArr = this.STREAM_VOLUME_ALIAS_VOICE;
        return iArr[aliasStreamType] == iArr[1];
    }

    public void setMicrophoneMute(boolean on, String callingPackage, int userId, String attributionTag) {
        int uid = Binder.getCallingUid();
        if (uid == 1000) {
            uid = UserHandle.getUid(userId, UserHandle.getAppId(uid));
        }
        MediaMetrics.Item mmi = new MediaMetrics.Item("audio.mic").setUid(uid).set(MediaMetrics.Property.CALLING_PACKAGE, callingPackage).set(MediaMetrics.Property.EVENT, "setMicrophoneMute").set(MediaMetrics.Property.REQUEST, on ? "mute" : "unmute");
        if (!on && !checkNoteAppOp(44, uid, callingPackage, attributionTag)) {
            mmi.set(MediaMetrics.Property.EARLY_RETURN, "disallow unmuting").record();
        } else if (!checkAudioSettingsPermission("setMicrophoneMute()")) {
            mmi.set(MediaMetrics.Property.EARLY_RETURN, "!checkAudioSettingsPermission").record();
        } else if (userId != UserHandle.getCallingUserId() && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            mmi.set(MediaMetrics.Property.EARLY_RETURN, ParsingPackageUtils.TAG_PERMISSION).record();
        } else {
            if (!on && callingPackage.equals("com.whatsapp") && getMode() == 3) {
                AudioSystem.setParameters("SET_VOIP_ACTIVE=1");
            }
            this.mMicMuteFromApi = on;
            mmi.record();
            setMicrophoneMuteNoCallerCheck(userId);
        }
    }

    public void setMicrophoneMuteFromSwitch(boolean on) {
        int userId = Binder.getCallingUid();
        if (userId != 1000) {
            Log.e(TAG, "setMicrophoneMuteFromSwitch() called from non system user!");
            return;
        }
        this.mMicMuteFromSwitch = on;
        new MediaMetrics.Item("audio.mic").setUid(userId).set(MediaMetrics.Property.EVENT, "setMicrophoneMuteFromSwitch").set(MediaMetrics.Property.REQUEST, on ? "mute" : "unmute").record();
        setMicrophoneMuteNoCallerCheck(userId);
    }

    private void setMicMuteFromSwitchInput() {
        InputManager im = (InputManager) this.mContext.getSystemService(InputManager.class);
        int isMicMuted = im.isMicMuted();
        if (isMicMuted != -1) {
            setMicrophoneMuteFromSwitch(im.isMicMuted() != 0);
        }
    }

    public boolean isMicrophoneMuted() {
        return this.mMicMuteFromSystemCached && (!this.mMicMuteFromPrivacyToggle || this.mMicMuteFromApi || this.mMicMuteFromRestrictions || this.mMicMuteFromSwitch);
    }

    private boolean isMicrophoneSupposedToBeMuted() {
        return this.mMicMuteFromSwitch || this.mMicMuteFromRestrictions || this.mMicMuteFromApi || this.mMicMuteFromPrivacyToggle;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setMicrophoneMuteNoCallerCheck(int userId) {
        boolean muted = isMicrophoneSupposedToBeMuted();
        if (DEBUG_VOL) {
            Log.d(TAG, String.format("Mic mute %b, user=%d", Boolean.valueOf(muted), Integer.valueOf(userId)));
        }
        if (getCurrentUserId() == userId || userId == 1000) {
            boolean currentMute = this.mAudioSystem.isMicrophoneMuted();
            long identity = Binder.clearCallingIdentity();
            int ret = this.mAudioSystem.muteMicrophone(muted);
            this.mMicMuteFromSystemCached = this.mAudioSystem.isMicrophoneMuted();
            if (ret != 0) {
                Log.e(TAG, "Error changing mic mute state to " + muted + " current:" + this.mMicMuteFromSystemCached);
            }
            new MediaMetrics.Item("audio.mic").setUid(userId).set(MediaMetrics.Property.EVENT, "setMicrophoneMuteNoCallerCheck").set(MediaMetrics.Property.MUTE, this.mMicMuteFromSystemCached ? "on" : "off").set(MediaMetrics.Property.REQUEST, muted ? "mute" : "unmute").set(MediaMetrics.Property.STATUS, Integer.valueOf(ret)).record();
            if (muted != currentMute) {
                try {
                    sendMsg(this.mAudioHandler, 30, 1, 0, 0, null, 0);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    public int getRingerModeExternal() {
        int i;
        synchronized (this.mSettingsLock) {
            i = this.mRingerModeExternal;
        }
        return i;
    }

    public int getRingerModeInternal() {
        int i;
        synchronized (this.mSettingsLock) {
            i = this.mRingerMode;
        }
        return i;
    }

    private void ensureValidRingerMode(int ringerMode) {
        if (!isValidRingerMode(ringerMode)) {
            throw new IllegalArgumentException("Bad ringer mode " + ringerMode);
        }
    }

    public boolean isValidRingerMode(int ringerMode) {
        return ringerMode >= 0 && ringerMode <= 2;
    }

    public void setRingerModeExternal(int ringerMode, String caller) {
        if (isAndroidNPlus(caller) && wouldToggleZenMode(ringerMode) && !this.mNm.isNotificationPolicyAccessGrantedForPackage(caller)) {
            throw new SecurityException("Not allowed to change Do Not Disturb state");
        }
        setRingerMode(ringerMode, caller, true);
    }

    public void setRingerModeInternal(int ringerMode, String caller) {
        enforceVolumeController("setRingerModeInternal");
        setRingerMode(ringerMode, caller, false);
    }

    public void silenceRingerModeInternal(String reason) {
        VibrationEffect effect = null;
        int ringerMode = 0;
        int toastText = 0;
        int silenceRingerSetting = 0;
        if (this.mContext.getResources().getBoolean(17891829)) {
            silenceRingerSetting = this.mSettings.getSecureIntForUser(this.mContentResolver, "volume_hush_gesture", 0, -2);
        }
        switch (silenceRingerSetting) {
            case 1:
                effect = VibrationEffect.get(5);
                ringerMode = 1;
                toastText = 17041699;
                break;
            case 2:
                effect = VibrationEffect.get(1);
                ringerMode = 0;
                toastText = 17041698;
                break;
        }
        maybeVibrate(effect, reason);
        setRingerModeInternal(ringerMode, reason);
        Toast.makeText(this.mContext, toastText, 0).show();
    }

    private boolean maybeVibrate(VibrationEffect effect, String reason) {
        if (this.mHasVibrator && effect != null) {
            this.mVibrator.vibrate(Binder.getCallingUid(), this.mContext.getOpPackageName(), effect, reason, TOUCH_VIBRATION_ATTRIBUTES);
            return true;
        }
        return false;
    }

    private void setRingerMode(int ringerMode, String caller, boolean external) {
        int ringerMode2;
        if (!this.mUseFixedVolume && !this.mIsSingleVolume) {
            if (caller == null || caller.length() == 0) {
                throw new IllegalArgumentException("Bad caller: " + caller);
            }
            ensureValidRingerMode(ringerMode);
            if (ringerMode == 1 && !this.mHasVibrator) {
                ringerMode2 = 0;
            } else {
                ringerMode2 = ringerMode;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mSettingsLock) {
                    int ringerModeInternal = getRingerModeInternal();
                    int ringerModeExternal = getRingerModeExternal();
                    if (external) {
                        setRingerModeExt(ringerMode2);
                        AudioManagerInternal.RingerModeDelegate ringerModeDelegate = this.mRingerModeDelegate;
                        if (ringerModeDelegate != null) {
                            ringerMode2 = ringerModeDelegate.onSetRingerModeExternal(ringerModeExternal, ringerMode2, caller, ringerModeInternal, this.mVolumePolicy);
                        }
                        if (ringerMode2 != ringerModeInternal) {
                            setRingerModeInt(ringerMode2, true);
                        }
                    } else {
                        if (ringerMode2 != ringerModeInternal) {
                            setRingerModeInt(ringerMode2, true);
                        }
                        AudioManagerInternal.RingerModeDelegate ringerModeDelegate2 = this.mRingerModeDelegate;
                        if (ringerModeDelegate2 != null) {
                            ringerMode2 = ringerModeDelegate2.onSetRingerModeInternal(ringerModeInternal, ringerMode2, caller, ringerModeExternal, this.mVolumePolicy);
                        }
                        setRingerModeExt(ringerMode2);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private void setRingerModeExt(int ringerMode) {
        synchronized (this.mSettingsLock) {
            if (ringerMode == this.mRingerModeExternal) {
                return;
            }
            this.mRingerModeExternal = ringerMode;
            broadcastRingerMode("android.media.RINGER_MODE_CHANGED", ringerMode);
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:53:0x00cf
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    private void muteRingerModeStreams() {
        /*
            r27 = this;
            r1 = r27
            int r2 = android.media.AudioSystem.getNumStreamTypes()
            android.app.NotificationManager r0 = r1.mNm
            if (r0 != 0) goto L17
            android.content.Context r0 = r1.mContext
            java.lang.String r3 = "notification"
            java.lang.Object r0 = r0.getSystemService(r3)
            android.app.NotificationManager r0 = (android.app.NotificationManager) r0
            r1.mNm = r0
        L17:
            int r3 = r1.mRingerMode
            r4 = 1
            if (r3 == r4) goto L21
            if (r3 != 0) goto L1f
            goto L21
        L1f:
            r5 = 0
            goto L22
        L21:
            r5 = r4
        L22:
            if (r3 != r4) goto L2e
            com.android.server.audio.AudioDeviceBroker r6 = r1.mDeviceBroker
            boolean r6 = r6.isBluetoothScoActive()
            if (r6 == 0) goto L2e
            r6 = r4
            goto L2f
        L2e:
            r6 = 0
        L2f:
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "muteRingerModeStreams() from u/pid:"
            java.lang.StringBuilder r7 = r7.append(r8)
            int r8 = android.os.Binder.getCallingUid()
            java.lang.StringBuilder r7 = r7.append(r8)
            java.lang.String r8 = "/"
            java.lang.StringBuilder r7 = r7.append(r8)
            int r8 = android.os.Binder.getCallingPid()
            java.lang.StringBuilder r7 = r7.append(r8)
            java.lang.String r7 = r7.toString()
            com.android.server.audio.AudioService$AudioHandler r8 = r1.mAudioHandler
            r9 = 8
            r10 = 2
            r11 = 7
            if (r6 == 0) goto L5f
            r12 = 3
            goto L60
        L5f:
            r12 = 0
        L60:
            r14 = 0
            r13 = r7
            sendMsg(r8, r9, r10, r11, r12, r13, r14)
            int r8 = r2 + (-1)
        L67:
            if (r8 < 0) goto L13a
            boolean r9 = r1.isStreamMutedByRingerOrZenMode(r8)
            r10 = 2
            if (r6 == 0) goto L75
            if (r8 == r10) goto L73
            goto L75
        L73:
            r11 = 0
            goto L76
        L75:
            r11 = r4
        L76:
            boolean r12 = r1.shouldZenMuteStream(r8)
            if (r12 != 0) goto L89
            if (r5 == 0) goto L87
            boolean r13 = r1.isStreamAffectedByRingerMode(r8)
            if (r13 == 0) goto L87
            if (r11 == 0) goto L87
            goto L89
        L87:
            r13 = 0
            goto L8a
        L89:
            r13 = r4
        L8a:
            if (r9 != r13) goto L94
            r17 = r2
            r19 = r3
            r3 = r4
            r2 = 0
            goto L131
        L94:
            if (r13 != 0) goto L11d
            int[] r14 = com.android.server.audio.AudioService.mStreamVolumeAlias
            r14 = r14[r8]
            if (r14 != r10) goto L107
            java.lang.Class<com.android.server.audio.AudioService$VolumeStreamState> r10 = com.android.server.audio.AudioService.VolumeStreamState.class
            monitor-enter(r10)
            com.android.server.audio.AudioService$VolumeStreamState[] r14 = r1.mStreamStates     // Catch: java.lang.Throwable -> Lfe
            r14 = r14[r8]     // Catch: java.lang.Throwable -> Lfe
            r15 = 0
        La4:
            android.util.SparseIntArray r16 = com.android.server.audio.AudioService.VolumeStreamState.m1920$$Nest$fgetmIndexMap(r14)     // Catch: java.lang.Throwable -> Lfe
            int r0 = r16.size()     // Catch: java.lang.Throwable -> Lfe
            if (r15 >= r0) goto Le1
            android.util.SparseIntArray r0 = com.android.server.audio.AudioService.VolumeStreamState.m1920$$Nest$fgetmIndexMap(r14)     // Catch: java.lang.Throwable -> Lfe
            int r0 = r0.keyAt(r15)     // Catch: java.lang.Throwable -> Lfe
            android.util.SparseIntArray r4 = com.android.server.audio.AudioService.VolumeStreamState.m1920$$Nest$fgetmIndexMap(r14)     // Catch: java.lang.Throwable -> Lfe
            int r4 = r4.valueAt(r15)     // Catch: java.lang.Throwable -> Lfe
            if (r4 != 0) goto Ld3
            r17 = r2
            java.lang.String r2 = "AS.AudioService"
            r19 = r3
            r18 = r4
            r3 = 10
            r4 = 1
            r14.setIndex(r3, r0, r2, r4)     // Catch: java.lang.Throwable -> L105
            goto Ld9
        Lcf:
            r0 = move-exception
            r19 = r3
            goto L103
        Ld3:
            r17 = r2
            r19 = r3
            r18 = r4
        Ld9:
            int r15 = r15 + 1
            r2 = r17
            r3 = r19
            r4 = 1
            goto La4
        Le1:
            r17 = r2
            r19 = r3
            int r23 = r1.getDeviceForStream(r8)     // Catch: java.lang.Throwable -> L105
            com.android.server.audio.AudioService$AudioHandler r0 = r1.mAudioHandler     // Catch: java.lang.Throwable -> L105
            r21 = 1
            r22 = 2
            r24 = 0
            com.android.server.audio.AudioService$VolumeStreamState[] r2 = r1.mStreamStates     // Catch: java.lang.Throwable -> L105
            r25 = r2[r8]     // Catch: java.lang.Throwable -> L105
            r26 = 500(0x1f4, float:7.0E-43)
            r20 = r0
            sendMsg(r20, r21, r22, r23, r24, r25, r26)     // Catch: java.lang.Throwable -> L105
            monitor-exit(r10)     // Catch: java.lang.Throwable -> L105
            goto L10b
        Lfe:
            r0 = move-exception
            r17 = r2
            r19 = r3
        L103:
            monitor-exit(r10)     // Catch: java.lang.Throwable -> L105
            throw r0
        L105:
            r0 = move-exception
            goto L103
        L107:
            r17 = r2
            r19 = r3
        L10b:
            com.android.server.audio.AudioService$VolumeStreamState[] r0 = r1.mStreamStates
            r0 = r0[r8]
            r2 = 0
            r0.mute(r2)
            int r0 = r1.mRingerAndZenModeMutedStreams
            r3 = 1
            int r4 = r3 << r8
            int r4 = ~r4
            r0 = r0 & r4
            r1.mRingerAndZenModeMutedStreams = r0
            goto L131
        L11d:
            r17 = r2
            r19 = r3
            r3 = r4
            r2 = 0
            com.android.server.audio.AudioService$VolumeStreamState[] r0 = r1.mStreamStates
            r0 = r0[r8]
            r0.mute(r3)
            int r0 = r1.mRingerAndZenModeMutedStreams
            int r4 = r3 << r8
            r0 = r0 | r4
            r1.mRingerAndZenModeMutedStreams = r0
        L131:
            int r8 = r8 + (-1)
            r4 = r3
            r2 = r17
            r3 = r19
            goto L67
        L13a:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.AudioService.muteRingerModeStreams():void");
    }

    private boolean isAlarm(int streamType) {
        return streamType == 4;
    }

    private boolean isNotificationOrRinger(int streamType) {
        return streamType == 5 || streamType == 2;
    }

    private boolean isMedia(int streamType) {
        return streamType == 3;
    }

    private boolean isSystem(int streamType) {
        return streamType == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setRingerModeInt(int ringerMode, boolean persist) {
        boolean change;
        synchronized (this.mSettingsLock) {
            change = this.mRingerMode != ringerMode;
            this.mRingerMode = ringerMode;
            muteRingerModeStreams();
        }
        if (persist) {
            sendMsg(this.mAudioHandler, 3, 0, 0, 0, null, 500);
        }
        if (change) {
            broadcastRingerMode("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION", ringerMode);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postUpdateRingerModeServiceInt() {
        sendMsg(this.mAudioHandler, 25, 2, 0, 0, null, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUpdateRingerModeServiceInt() {
        setRingerModeInt(getRingerModeInternal(), false);
    }

    public boolean shouldVibrate(int vibrateType) {
        if (this.mHasVibrator) {
            switch (getVibrateSetting(vibrateType)) {
                case 0:
                    return false;
                case 1:
                    return getRingerModeExternal() != 0;
                case 2:
                    return getRingerModeExternal() == 1;
                default:
                    return false;
            }
        }
        return false;
    }

    public int getVibrateSetting(int vibrateType) {
        if (this.mHasVibrator) {
            return (this.mVibrateSetting >> (vibrateType * 2)) & 3;
        }
        return 0;
    }

    public void setVibrateSetting(int vibrateType, int vibrateSetting) {
        if (this.mHasVibrator) {
            this.mVibrateSetting = AudioSystem.getValueForVibrateSetting(this.mVibrateSetting, vibrateType, vibrateSetting);
            broadcastVibrateSetting(vibrateType);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SetModeDeathHandler implements IBinder.DeathRecipient {
        private final IBinder mCb;
        private final boolean mIsPrivileged;
        private int mMode;
        private final String mPackage;
        private final int mPid;
        private final int mUid;
        private boolean mPlaybackActive = false;
        private boolean mRecordingActive = false;
        private long mUpdateTime = System.currentTimeMillis();

        SetModeDeathHandler(IBinder cb, int pid, int uid, boolean isPrivileged, String caller, int mode) {
            this.mMode = mode;
            this.mCb = cb;
            this.mPid = pid;
            this.mUid = uid;
            this.mPackage = caller;
            this.mIsPrivileged = isPrivileged;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (AudioService.this.mDeviceBroker.mSetModeLock) {
                Log.w(AudioService.TAG, "SetModeDeathHandler client died");
                int index = AudioService.this.mSetModeDeathHandlers.indexOf(this);
                if (index < 0) {
                    Log.w(AudioService.TAG, "unregistered SetModeDeathHandler client died");
                } else {
                    AudioService.this.mSetModeDeathHandlers.get(index);
                    AudioService.this.mSetModeDeathHandlers.remove(index);
                    AudioService.sendMsg(AudioService.this.mAudioHandler, 36, 2, -1, Process.myPid(), AudioService.this.mContext.getPackageName(), 0);
                }
            }
        }

        public int getPid() {
            return this.mPid;
        }

        public void setMode(int mode) {
            this.mMode = mode;
            this.mUpdateTime = System.currentTimeMillis();
        }

        public int getMode() {
            return this.mMode;
        }

        public IBinder getBinder() {
            return this.mCb;
        }

        public int getUid() {
            return this.mUid;
        }

        public String getPackage() {
            return this.mPackage;
        }

        public boolean isPrivileged() {
            return this.mIsPrivileged;
        }

        public long getUpdateTime() {
            return this.mUpdateTime;
        }

        public void setPlaybackActive(boolean active) {
            this.mPlaybackActive = active;
        }

        public void setRecordingActive(boolean active) {
            this.mRecordingActive = active;
        }

        public boolean isActive() {
            if (this.mIsPrivileged) {
                return true;
            }
            int i = this.mMode;
            return (i == 3 && (this.mRecordingActive || this.mPlaybackActive)) || i == 1 || i == 4;
        }

        public void dump(PrintWriter pw, int index) {
            SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
            if (index >= 0) {
                pw.println("  Requester # " + (index + 1) + ":");
            }
            pw.println("  - Mode: " + AudioSystem.modeToString(this.mMode));
            pw.println("  - Binder: " + this.mCb);
            pw.println("  - Pid: " + this.mPid);
            pw.println("  - Uid: " + this.mUid);
            pw.println("  - Package: " + this.mPackage);
            pw.println("  - Privileged: " + this.mIsPrivileged);
            pw.println("  - Active: " + isActive());
            pw.println("    Playback active: " + this.mPlaybackActive);
            pw.println("    Recording active: " + this.mRecordingActive);
            pw.println("  - update time: " + format.format(new Date(this.mUpdateTime)));
        }
    }

    private SetModeDeathHandler getAudioModeOwnerHandler() {
        SetModeDeathHandler modeOwner = null;
        SetModeDeathHandler privilegedModeOwner = null;
        Iterator<SetModeDeathHandler> it = this.mSetModeDeathHandlers.iterator();
        while (it.hasNext()) {
            SetModeDeathHandler h = it.next();
            if (h.isActive()) {
                if (h.isPrivileged()) {
                    if (privilegedModeOwner == null || h.getUpdateTime() > privilegedModeOwner.getUpdateTime()) {
                        privilegedModeOwner = h;
                    }
                } else if (modeOwner == null || h.getUpdateTime() > modeOwner.getUpdateTime()) {
                    modeOwner = h;
                }
            }
        }
        return privilegedModeOwner != null ? privilegedModeOwner : modeOwner;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getModeOwnerPid() {
        SetModeDeathHandler hdlr = getAudioModeOwnerHandler();
        if (hdlr != null) {
            return hdlr.getPid();
        }
        return 0;
    }

    int getModeOwnerUid() {
        SetModeDeathHandler hdlr = getAudioModeOwnerHandler();
        if (hdlr != null) {
            return hdlr.getUid();
        }
        return 0;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5295=4] */
    public void setMode(int mode, IBinder cb, String callingPackage) {
        Object obj;
        SetModeDeathHandler currentModeHandler;
        SetModeDeathHandler currentModeHandler2;
        int i;
        SetModeDeathHandler currentModeHandler3;
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        if (DEBUG_MODE) {
            Log.v(TAG, "setMode(mode=" + mode + ", pid=" + pid + ", uid=" + uid + ", caller=" + callingPackage + ")");
        }
        if (checkAudioSettingsPermission("setMode()")) {
            if (cb == null) {
                Log.e(TAG, "setMode() called with null binder");
            } else if (mode < -1 || mode >= 7) {
                Log.w(TAG, "setMode() invalid mode: " + mode);
            } else {
                int mode2 = mode == -1 ? getMode() : mode;
                ITranAudioService.Instance().notifyModeChange(mode2, callingPackage);
                if (mode2 == 4 && !this.mIsCallScreeningModeSupported) {
                    Log.w(TAG, "setMode(MODE_CALL_SCREENING) not permitted when call screening is not supported");
                    return;
                }
                boolean hasModifyPhoneStatePermission = this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") == 0;
                if ((mode2 == 2 || mode2 == 5 || mode2 == 6) && !hasModifyPhoneStatePermission) {
                    Log.w(TAG, "MODIFY_PHONE_STATE Permission Denial: setMode(" + AudioSystem.modeToString(mode2) + ") from pid=" + pid + ", uid=" + Binder.getCallingUid());
                    getAudioServiceExtInstance().stopBluetoothLeCg(cb);
                    return;
                }
                Object obj2 = this.mDeviceBroker.mSetModeLock;
                synchronized (obj2) {
                    try {
                        Iterator<SetModeDeathHandler> it = this.mSetModeDeathHandlers.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                currentModeHandler = null;
                                break;
                            }
                            SetModeDeathHandler h = it.next();
                            if (h.getPid() == pid) {
                                currentModeHandler = h;
                                break;
                            }
                        }
                    } catch (Throwable th) {
                        e = th;
                        obj = obj2;
                    }
                    try {
                        if (mode2 == 0) {
                            if (currentModeHandler != null) {
                                if (!currentModeHandler.isPrivileged() && currentModeHandler.getMode() == 3) {
                                    this.mAudioHandler.removeEqualMessages(31, currentModeHandler);
                                }
                                getAudioServiceExtInstance().stopBluetoothLeCgLater(cb);
                                this.mSetModeDeathHandlers.remove(currentModeHandler);
                                if (DEBUG_MODE) {
                                    Log.v(TAG, "setMode(" + mode2 + ") removing hldr for pid: " + pid);
                                }
                                try {
                                    currentModeHandler.getBinder().unlinkToDeath(currentModeHandler, 0);
                                } catch (NoSuchElementException e) {
                                    Log.w(TAG, "setMode link does not exist ...");
                                }
                            }
                            obj = obj2;
                            i = 1;
                            currentModeHandler3 = currentModeHandler;
                        } else {
                            if (currentModeHandler != null) {
                                currentModeHandler.setMode(mode2);
                                if (DEBUG_MODE) {
                                    Log.v(TAG, "setMode(" + mode2 + ") updating hldr for pid: " + pid);
                                }
                                currentModeHandler2 = currentModeHandler;
                                obj = obj2;
                            } else {
                                try {
                                    obj = obj2;
                                    try {
                                        currentModeHandler2 = new SetModeDeathHandler(cb, pid, uid, hasModifyPhoneStatePermission, callingPackage, mode2);
                                    } catch (Throwable th2) {
                                        e = th2;
                                    }
                                } catch (Throwable th3) {
                                    e = th3;
                                    obj = obj2;
                                }
                                try {
                                    try {
                                        cb.linkToDeath(currentModeHandler2, 0);
                                        this.mSetModeDeathHandlers.add(currentModeHandler2);
                                        if (DEBUG_MODE) {
                                            Log.v(TAG, "setMode(" + mode2 + ") adding handler for pid=" + pid);
                                        }
                                    } catch (RemoteException e2) {
                                        Log.w(TAG, "setMode() could not link to " + cb + " binder death");
                                        return;
                                    }
                                } catch (Throwable th4) {
                                    e = th4;
                                    throw e;
                                }
                            }
                            if (mode2 != 3) {
                                i = 1;
                            } else if (currentModeHandler2.isPrivileged()) {
                                i = 1;
                            } else {
                                i = 1;
                                currentModeHandler2.setPlaybackActive(true);
                                currentModeHandler2.setRecordingActive(true);
                                sendMsg(this.mAudioHandler, 31, 2, 0, 0, currentModeHandler2, CHECK_MODE_FOR_UID_PERIOD_MS);
                                this.mDeviceBroker.preSetModeOwnerPid(pid, mode2);
                            }
                            currentModeHandler3 = currentModeHandler2;
                        }
                        if (mode2 == i) {
                            try {
                                getAudioServiceExtInstance().setPreferredDeviceForHfpInbandRinging(pid, uid, mode2, cb);
                            } catch (Throwable th5) {
                                e = th5;
                                throw e;
                            }
                        }
                        String packageName = "setMode_packageName=" + callingPackage;
                        AudioSystem.setParameters(packageName);
                        sendMsg(this.mAudioHandler, 36, 0, mode2, pid, callingPackage, 0);
                    } catch (Throwable th6) {
                        e = th6;
                        obj = obj2;
                        throw e;
                    }
                }
            }
        }
    }

    void onUpdateAudioMode(int requestedMode, int requesterPid, String requesterPackage, boolean force) {
        int requestedMode2;
        int uid;
        int pid;
        IBinder cb;
        if (requestedMode != -1) {
            requestedMode2 = requestedMode;
        } else {
            requestedMode2 = getMode();
        }
        int mode = 0;
        SetModeDeathHandler currentModeHandler = getAudioModeOwnerHandler();
        if (currentModeHandler == null) {
            uid = 0;
            pid = 0;
            cb = null;
        } else {
            mode = currentModeHandler.getMode();
            int uid2 = currentModeHandler.getUid();
            int pid2 = currentModeHandler.getPid();
            IBinder cb2 = currentModeHandler.getBinder();
            uid = uid2;
            pid = pid2;
            cb = cb2;
        }
        boolean z = DEBUG_MODE;
        if (z) {
            Log.v(TAG, "onUpdateAudioMode() new mode: " + mode + ", current mode: " + this.mMode.get() + " requested mode: " + requestedMode2);
        }
        if (mode != this.mMode.get() || force) {
            getAudioServiceExtInstance().startBluetoothLeCg(pid, uid, mode, cb);
            long identity = Binder.clearCallingIdentity();
            int status = this.mAudioSystem.setPhoneState(mode, uid);
            Binder.restoreCallingIdentity(identity);
            if (status == 0) {
                if (z) {
                    Log.v(TAG, "onUpdateAudioMode: mode successfully set to " + mode);
                }
                sendMsg(this.mAudioHandler, 40, 0, mode, 0, null, 0);
                int previousMode = this.mMode.getAndSet(mode);
                int pid3 = pid;
                int uid3 = mode;
                this.mModeLogger.log(new AudioServiceEvents.PhoneStateEvent(requesterPackage, requesterPid, requestedMode2, pid, uid3));
                int streamType = getActiveStreamType(Integer.MIN_VALUE);
                int device = getDeviceForStream(streamType);
                int index = this.mStreamStates[mStreamVolumeAlias[streamType]].getIndex(device);
                int mode2 = mode;
                setStreamVolumeInt(mStreamVolumeAlias[streamType], index, device, true, requesterPackage, true);
                updateStreamVolumeAlias(true, requesterPackage);
                updateAbsVolumeMultiModeDevices(previousMode, mode2);
                this.mDeviceBroker.postSetModeOwnerPid(pid3, mode2);
                return;
            }
            Log.w(TAG, "onUpdateAudioMode: failed to set audio mode to: " + mode);
        }
    }

    public int getMode() {
        synchronized (this.mDeviceBroker.mSetModeLock) {
            SetModeDeathHandler currentModeHandler = getAudioModeOwnerHandler();
            if (currentModeHandler != null) {
                return currentModeHandler.getMode();
            }
            return 0;
        }
    }

    public boolean isCallScreeningModeSupported() {
        return this.mIsCallScreeningModeSupported;
    }

    protected void dispatchMode(int mode) {
        int nbDispatchers = this.mModeDispatchers.beginBroadcast();
        for (int i = 0; i < nbDispatchers; i++) {
            try {
                this.mModeDispatchers.getBroadcastItem(i).dispatchAudioModeChanged(mode);
            } catch (RemoteException e) {
            }
        }
        this.mModeDispatchers.finishBroadcast();
    }

    public void registerModeDispatcher(IAudioModeDispatcher dispatcher) {
        this.mModeDispatchers.register(dispatcher);
    }

    public void unregisterModeDispatcher(IAudioModeDispatcher dispatcher) {
        this.mModeDispatchers.unregister(dispatcher);
    }

    public boolean isPstnCallAudioInterceptable() {
        enforceCallAudioInterceptionPermission();
        boolean uplinkDeviceFound = false;
        boolean downlinkDeviceFound = false;
        AudioDeviceInfo[] devices = AudioManager.getDevicesStatic(3);
        for (AudioDeviceInfo device : devices) {
            if (device.getInternalType() == 65536) {
                uplinkDeviceFound = true;
            } else if (device.getInternalType() == -2147483584) {
                downlinkDeviceFound = true;
            }
            if (uplinkDeviceFound && downlinkDeviceFound) {
                return true;
            }
        }
        return false;
    }

    public void setRttEnabled(boolean rttEnabled) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            Log.w(TAG, "MODIFY_PHONE_STATE Permission Denial: setRttEnabled from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (this.mSettingsLock) {
            this.mRttEnabled = rttEnabled;
            long identity = Binder.clearCallingIdentity();
            AudioSystem.setRttEnabled(rttEnabled);
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void adjustSuggestedStreamVolumeForUid(int streamType, int direction, int flags, String packageName, int uid, int pid, UserHandle userHandle, int targetSdkVersion) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Should only be called from system process");
        }
        adjustSuggestedStreamVolume(direction, streamType, flags, packageName, packageName, uid, pid, hasAudioSettingsPermission(uid, pid), 0);
    }

    public void adjustStreamVolumeForUid(int streamType, int direction, int flags, String packageName, int uid, int pid, UserHandle userHandle, int targetSdkVersion) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Should only be called from system process");
        }
        if (direction != 0) {
            sVolumeLogger.log(new AudioServiceEvents.VolumeEvent(5, streamType, direction, flags, packageName + " uid:" + uid));
        }
        adjustStreamVolume(streamType, direction, flags, packageName, packageName, uid, pid, null, hasAudioSettingsPermission(uid, pid), 0);
    }

    public void setStreamVolumeForUid(int streamType, int index, int flags, String packageName, int uid, int pid, UserHandle userHandle, int targetSdkVersion) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Should only be called from system process");
        }
        setStreamVolume(streamType, index, flags, packageName, packageName, null, uid, hasAudioSettingsPermission(uid, pid));
    }

    /* loaded from: classes.dex */
    private static final class LoadSoundEffectReply implements SoundEffectsHelper.OnEffectsLoadCompleteHandler {
        private static final int SOUND_EFFECTS_ERROR = -1;
        private static final int SOUND_EFFECTS_LOADED = 0;
        private static final int SOUND_EFFECTS_LOADING = 1;
        private static final int SOUND_EFFECTS_LOAD_TIMEOUT_MS = 5000;
        private int mStatus;

        private LoadSoundEffectReply() {
            this.mStatus = 1;
        }

        @Override // com.android.server.audio.SoundEffectsHelper.OnEffectsLoadCompleteHandler
        public synchronized void run(boolean success) {
            this.mStatus = success ? 0 : -1;
            notify();
        }

        public synchronized boolean waitForLoaded(int attempts) {
            int i;
            while (true) {
                i = this.mStatus;
                if (i != 1) {
                    break;
                }
                int attempts2 = attempts - 1;
                if (attempts <= 0) {
                    break;
                }
                try {
                    wait(5000L);
                } catch (InterruptedException e) {
                    Log.w(AudioService.TAG, "Interrupted while waiting sound pool loaded.");
                }
                attempts = attempts2;
            }
            return i == 0;
        }
    }

    public void playSoundEffect(int effectType, int userId) {
        if (querySoundEffectsEnabled(userId)) {
            playSoundEffectVolume(effectType, -1.0f);
        }
    }

    private boolean querySoundEffectsEnabled(int user) {
        return this.mSettings.getSystemIntForUser(getContentResolver(), "sound_effects_enabled", 0, user) != 0;
    }

    public void playSoundEffectVolume(int effectType, float volume) {
        if (isStreamMute(1)) {
            return;
        }
        if (effectType >= 16 || effectType < 0) {
            Log.w(TAG, "AudioService effectType value " + effectType + " out of range");
        } else {
            sendMsg(this.mAudioHandler, 5, 2, effectType, (int) (1000.0f * volume), null, 0);
        }
    }

    public boolean loadSoundEffects() {
        LoadSoundEffectReply reply = new LoadSoundEffectReply();
        sendMsg(this.mAudioHandler, 7, 2, 0, 0, reply, 0);
        return reply.waitForLoaded(3);
    }

    protected void scheduleLoadSoundEffects() {
        sendMsg(this.mAudioHandler, 7, 2, 0, 0, null, 0);
    }

    public void unloadSoundEffects() {
        sendMsg(this.mAudioHandler, 15, 2, 0, 0, null, 0);
    }

    public void reloadAudioSettings() {
        readAudioSettings(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readAudioSettings(boolean userSwitch) {
        readPersistedSettings();
        readUserRestrictions();
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        for (int streamType = 0; streamType < numStreamTypes; streamType++) {
            VolumeStreamState streamState = this.mStreamStates[streamType];
            if (!userSwitch || mStreamVolumeAlias[streamType] != 3) {
                streamState.readSettings();
                synchronized (VolumeStreamState.class) {
                    if (streamState.mIsMuted && ((!isStreamAffectedByMute(streamType) && !isStreamMutedByRingerOrZenMode(streamType)) || this.mUseFixedVolume)) {
                        streamState.mIsMuted = false;
                    }
                }
                continue;
            }
        }
        int streamType2 = getRingerModeInternal();
        setRingerModeInt(streamType2, false);
        checkAllFixedVolumeDevices();
        checkAllAliasStreamVolumes();
        checkMuteAffectedStreams();
        synchronized (this.mSafeMediaVolumeStateLock) {
            this.mMusicActiveMs = MathUtils.constrain(this.mSettings.getSecureIntForUser(this.mContentResolver, "unsafe_volume_music_active_ms", 0, -2), 0, (int) UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX);
            if (this.mSafeMediaVolumeState == 3) {
                enforceSafeMediaVolume(TAG);
            }
        }
        readVolumeGroupsSettings();
        if (DEBUG_VOL) {
            Log.d(TAG, "Restoring device volume behavior");
        }
        restoreDeviceVolumeBehavior();
    }

    private boolean isValidCommunicationDevice(AudioDeviceInfo device) {
        int[] iArr;
        for (int type : VALID_COMMUNICATION_DEVICE_TYPES) {
            if (device.getType() == type) {
                return true;
            }
        }
        return false;
    }

    public int[] getAvailableCommunicationDeviceIds() {
        ArrayList<Integer> deviceIds = new ArrayList<>();
        AudioDeviceInfo[] devices = AudioManager.getDevicesStatic(2);
        for (AudioDeviceInfo device : devices) {
            if (isValidCommunicationDevice(device)) {
                deviceIds.add(Integer.valueOf(device.getId()));
            }
        }
        return deviceIds.stream().mapToInt(new AudioService$$ExternalSyntheticLambda1()).toArray();
    }

    public boolean setCommunicationDevice(IBinder cb, int portId) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        boolean status = false;
        AudioDeviceInfo device = null;
        if (portId != 0) {
            device = AudioManager.getDeviceForPortId(portId, 2);
            if (device == null) {
                throw new IllegalArgumentException("invalid portID " + portId);
            }
            if (!isValidCommunicationDevice(device)) {
                throw new IllegalArgumentException("invalid device type " + device.getType());
            }
        }
        String eventSource = "setCommunicationDevice() from u/pid:" + uid + SliceClientPermissions.SliceAuthority.DELIMITER + pid;
        if (DEBUG_MODE) {
            String deviceName = null;
            if (device != null) {
                deviceName = AudioSystem.getOutputDeviceName(device.getInternalType());
            }
            Log.d(TAG, eventSource + ", deviceName=" + deviceName + ", portId=" + portId);
        }
        int deviceType = 1073741824;
        String deviceAddress = null;
        if (device != null) {
            deviceType = device.getPort().type();
            deviceAddress = device.getAddress();
        } else {
            AudioDeviceInfo curDevice = this.mDeviceBroker.getCommunicationDevice();
            if (curDevice != null) {
                deviceType = curDevice.getPort().type();
                deviceAddress = curDevice.getAddress();
            }
        }
        if (deviceType != 1073741824) {
            new MediaMetrics.Item("audio.device.setCommunicationDevice").set(MediaMetrics.Property.DEVICE, AudioSystem.getDeviceName(deviceType)).set(MediaMetrics.Property.ADDRESS, deviceAddress).set(MediaMetrics.Property.STATE, device != null ? "connected" : "disconnected").record();
        }
        boolean z = true;
        boolean isBleDeviceRequested = device != null && device.getType() == 26;
        if (device == null || device.getType() != 2) {
            z = false;
        }
        boolean isSpeakerDeviceRequested = z;
        boolean isBleDeviceCommunicationDevice = false;
        AudioDeviceInfo curDevice2 = this.mDeviceBroker.getCommunicationDevice();
        if (curDevice2 != null && curDevice2.getType() == 26) {
            isBleDeviceCommunicationDevice = true;
        }
        if (isBleDeviceCommunicationDevice && isSpeakerDeviceRequested) {
            getAudioServiceExtInstance().setCommunicationDeviceExt(cb, pid, null, eventSource);
            long ident = Binder.clearCallingIdentity();
            boolean status2 = this.mDeviceBroker.setCommunicationDevice(cb, pid, device, eventSource);
            Binder.restoreCallingIdentity(ident);
            return status2;
        }
        if (isBleDeviceCommunicationDevice || isBleDeviceRequested) {
            status = getAudioServiceExtInstance().setCommunicationDeviceExt(cb, pid, device, eventSource);
        }
        if (!status) {
            long ident2 = Binder.clearCallingIdentity();
            boolean status3 = this.mDeviceBroker.setCommunicationDevice(cb, pid, device, eventSource);
            Binder.restoreCallingIdentity(ident2);
            return status3;
        }
        return status;
    }

    public int getCommunicationDevice() {
        long ident = Binder.clearCallingIdentity();
        AudioDeviceInfo device = this.mDeviceBroker.getCommunicationDevice();
        Binder.restoreCallingIdentity(ident);
        if (device == null) {
            return 0;
        }
        return device.getId();
    }

    public void registerCommunicationDeviceDispatcher(ICommunicationDeviceDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        this.mDeviceBroker.registerCommunicationDeviceDispatcher(dispatcher);
    }

    public void unregisterCommunicationDeviceDispatcher(ICommunicationDeviceDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        this.mDeviceBroker.unregisterCommunicationDeviceDispatcher(dispatcher);
    }

    public void setSpeakerphoneOn(IBinder cb, boolean on) {
        AudioDeviceInfo curDevice;
        if (!checkAudioSettingsPermission("setSpeakerphoneOn()")) {
            return;
        }
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        if (on && (curDevice = this.mDeviceBroker.getCommunicationDevice()) != null && curDevice.getType() == 26) {
            getAudioServiceExtInstance().setCommunicationDeviceExt(cb, pid, null, "setSpeakerphoneOn");
        }
        String eventSource = "setSpeakerphoneOn(" + on + ") from u/pid:" + uid + SliceClientPermissions.SliceAuthority.DELIMITER + pid;
        if (DEBUG_MODE) {
            String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            Log.d(TAG, eventSource + ", callingApp=" + callingApp);
        }
        new MediaMetrics.Item("audio.device.setSpeakerphoneOn").setUid(uid).setPid(pid).set(MediaMetrics.Property.STATE, on ? "on" : "off").record();
        long ident = Binder.clearCallingIdentity();
        this.mDeviceBroker.setSpeakerphoneOn(cb, pid, on, eventSource);
        Binder.restoreCallingIdentity(ident);
    }

    public boolean isSpeakerphoneOn() {
        return this.mDeviceBroker.isSpeakerphoneOn();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetBluetoothScoOfApp() {
        this.mBtScoOnByApp = false;
    }

    public void setBluetoothScoOn(boolean on) {
        if (!checkAudioSettingsPermission("setBluetoothScoOn()")) {
            return;
        }
        if (getAudioServiceExtInstance().isBluetoothLeTbsDeviceActive()) {
            this.mBtScoOnByApp = false;
        } else if (UserHandle.getCallingAppId() >= 10000) {
            this.mBtScoOnByApp = on;
        } else {
            int uid = Binder.getCallingUid();
            int pid = Binder.getCallingPid();
            String eventSource = "setBluetoothScoOn(" + on + ") from u/pid:" + uid + SliceClientPermissions.SliceAuthority.DELIMITER + pid;
            new MediaMetrics.Item("audio.device.setBluetoothScoOn").setUid(uid).setPid(pid).set(MediaMetrics.Property.STATE, on ? "on" : "off").record();
            if (DEBUG_MODE) {
                String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
                Log.d(TAG, eventSource + ", callingApp=" + callingApp);
            }
            this.mDeviceBroker.setBluetoothScoOn(on, eventSource);
        }
    }

    public boolean isBluetoothScoOn() {
        boolean mBleCSstatus;
        if (getAudioServiceExtInstance().isBluetoothLeTbsDeviceActive() && (mBleCSstatus = getAudioServiceExtInstance().isBluetoothLeCgOn())) {
            return mBleCSstatus;
        }
        return this.mBtScoOnByApp || this.mDeviceBroker.isBluetoothScoOn();
    }

    public void setBluetoothA2dpOn(boolean on) {
        if (!checkAudioSettingsPermission("setBluetoothA2dpOn()")) {
            return;
        }
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        String eventSource = "setBluetoothA2dpOn(" + on + ") from u/pid:" + uid + SliceClientPermissions.SliceAuthority.DELIMITER + pid;
        new MediaMetrics.Item("audio.device.setBluetoothA2dpOn").setUid(uid).setPid(pid).set(MediaMetrics.Property.STATE, on ? "on" : "off").record();
        if (DEBUG_MODE) {
            String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            Log.d(TAG, eventSource + ", callingApp=" + callingApp);
        }
        this.mDeviceBroker.setBluetoothA2dpOn_Async(on, eventSource);
    }

    public boolean isBluetoothA2dpOn() {
        return this.mDeviceBroker.isBluetoothA2dpOn();
    }

    public void startBluetoothSco(IBinder cb, int targetSdkVersion) {
        if (!checkAudioSettingsPermission("startBluetoothSco()")) {
            return;
        }
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        int scoAudioMode = targetSdkVersion < 18 ? 0 : -1;
        String eventSource = "startBluetoothSco()) from u/pid:" + uid + SliceClientPermissions.SliceAuthority.DELIMITER + pid;
        new MediaMetrics.Item("audio.bluetooth").setUid(uid).setPid(pid).set(MediaMetrics.Property.EVENT, "startBluetoothSco").set(MediaMetrics.Property.SCO_AUDIO_MODE, BtHelper.scoAudioModeToString(scoAudioMode)).record();
        if (DEBUG_MODE) {
            String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            Log.d(TAG, eventSource + ", callingApp=" + callingApp + ", targetSdkVersion=" + targetSdkVersion);
        }
        if (getAudioServiceExtInstance().isBluetoothLeTbsDeviceActive() && targetSdkVersion <= 31) {
            getAudioServiceExtInstance().startBluetoothLeCg(cb, targetSdkVersion);
        } else {
            startBluetoothScoInt(cb, pid, scoAudioMode, eventSource);
        }
    }

    public void startBluetoothScoVirtualCall(IBinder cb) {
        if (!checkAudioSettingsPermission("startBluetoothScoVirtualCall()")) {
            return;
        }
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        String eventSource = "startBluetoothScoVirtualCall()) from u/pid:" + uid + SliceClientPermissions.SliceAuthority.DELIMITER + pid;
        new MediaMetrics.Item("audio.bluetooth").setUid(uid).setPid(pid).set(MediaMetrics.Property.EVENT, "startBluetoothScoVirtualCall").set(MediaMetrics.Property.SCO_AUDIO_MODE, BtHelper.scoAudioModeToString(0)).record();
        if (DEBUG_MODE) {
            String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            Log.d(TAG, eventSource + ", callingApp=" + callingApp);
        }
        startBluetoothScoInt(cb, pid, 0, eventSource);
    }

    void startBluetoothScoInt(IBinder cb, int pid, int scoAudioMode, String eventSource) {
        MediaMetrics.Item mmi = new MediaMetrics.Item("audio.bluetooth").set(MediaMetrics.Property.EVENT, "startBluetoothScoInt").set(MediaMetrics.Property.SCO_AUDIO_MODE, BtHelper.scoAudioModeToString(scoAudioMode));
        if (!checkAudioSettingsPermission("startBluetoothSco()") || !this.mSystemReady) {
            mmi.set(MediaMetrics.Property.EARLY_RETURN, "permission or systemReady").record();
            return;
        }
        long ident = Binder.clearCallingIdentity();
        this.mDeviceBroker.startBluetoothScoForClient(cb, pid, scoAudioMode, eventSource);
        Binder.restoreCallingIdentity(ident);
        mmi.record();
    }

    public void stopBluetoothSco(IBinder cb) {
        if (!checkAudioSettingsPermission("stopBluetoothSco()") || !this.mSystemReady) {
            return;
        }
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        String eventSource = "stopBluetoothSco()) from u/pid:" + uid + SliceClientPermissions.SliceAuthority.DELIMITER + pid;
        boolean mCGstatus = getAudioServiceExtInstance().stopBluetoothLeCg(cb);
        if (mCGstatus) {
            if (DEBUG_MODE) {
                Log.d(TAG, "stopBluetoothLeCg,BLE-CG Audio is disconnected successfully");
                return;
            }
            return;
        }
        if (DEBUG_MODE) {
            String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            Log.d(TAG, eventSource + ", callingApp=" + callingApp);
        }
        long ident = Binder.clearCallingIdentity();
        this.mDeviceBroker.stopBluetoothScoForClient(cb, pid, eventSource);
        Binder.restoreCallingIdentity(ident);
        new MediaMetrics.Item("audio.bluetooth").setUid(uid).setPid(pid).set(MediaMetrics.Property.EVENT, "stopBluetoothSco").set(MediaMetrics.Property.SCO_AUDIO_MODE, BtHelper.scoAudioModeToString(-1)).record();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentResolver getContentResolver() {
        return this.mContentResolver;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCheckMusicActive(String caller) {
        synchronized (this.mSafeMediaVolumeStateLock) {
            if (this.mSafeMediaVolumeState == 2) {
                int device = getDeviceForStream(3);
                Log.d(TAG, " device = " + device + " Is Contain?: " + this.mSafeMediaVolumeDevices.contains(Integer.valueOf(device)));
                if (this.mSafeMediaVolumeDevices.contains(Integer.valueOf(device))) {
                    sendMsg(this.mAudioHandler, 11, 0, 0, 0, caller, 60000);
                    int index = this.mStreamStates[3].getIndex(device);
                    long nowTimeus = System.currentTimeMillis();
                    long lastTimeus = Settings.Secure.getLongForUser(this.mContentResolver, UNSAFE_VOLUME_MUSIC_ACTIVE_CURRENT, nowTimeus, -2);
                    long deltaMs = nowTimeus - lastTimeus;
                    if (deltaMs > 120000) {
                        Log.d(TAG, "deltaMs more than 2 min,deltaMs = " + deltaMs);
                        deltaMs = 60000;
                    }
                    Log.d(TAG, " deltaMs = " + deltaMs + " nowTimeus = " + nowTimeus + " lastTimeus = " + lastTimeus);
                    Settings.Secure.putLongForUser(this.mContentResolver, UNSAFE_VOLUME_MUSIC_ACTIVE_CURRENT, nowTimeus, -2);
                    if (this.mAudioSystem.isStreamActive(3, 0) && index > safeMediaVolumeIndex(device)) {
                        this.mMusicActiveMs = (int) (this.mMusicActiveMs + deltaMs);
                        Log.d(TAG, "mMusicActiveMs = " + this.mMusicActiveMs);
                        if (this.mMusicActiveMs > UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX) {
                            Log.d(TAG, "onCheckMusicActive Set to safe vol index");
                            setSafeMediaVolumeEnabled(true, caller);
                            this.mMusicActiveMs = 0;
                        }
                        saveMusicActiveMs();
                    }
                }
            }
        }
    }

    private void saveMusicActiveMs() {
        this.mAudioHandler.obtainMessage(17, this.mMusicActiveMs, 0).sendToTarget();
    }

    private int getSafeUsbMediaVolumeIndex() {
        int min = MIN_STREAM_VOLUME[3];
        int max = MAX_STREAM_VOLUME[3];
        this.mSafeUsbMediaVolumeDbfs = this.mContext.getResources().getInteger(17694928) / 100.0f;
        while (true) {
            if (Math.abs(max - min) <= 1) {
                break;
            }
            int index = (max + min) / 2;
            float gainDB = AudioSystem.getStreamVolumeDB(3, index, 67108864);
            if (Float.isNaN(gainDB)) {
                break;
            }
            float f = this.mSafeUsbMediaVolumeDbfs;
            if (gainDB == f) {
                min = index;
                break;
            } else if (gainDB < f) {
                min = index;
            } else {
                max = index;
            }
        }
        return min * 10;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onConfigureSafeVolume(boolean force, String caller) {
        boolean safeMediaVolumeEnabled;
        int persistedState;
        synchronized (this.mSafeMediaVolumeStateLock) {
            int mcc = this.mContext.getResources().getConfiguration().mcc;
            int i = this.mMcc;
            if (i != mcc || (i == 0 && force)) {
                this.mSafeMediaVolumeIndex = this.mContext.getResources().getInteger(17694927) * 10;
                this.mSafeUsbMediaVolumeIndex = getSafeUsbMediaVolumeIndex();
                if (!SystemProperties.getBoolean("audio.safemedia.force", false) && !this.mContext.getResources().getBoolean(17891736)) {
                    safeMediaVolumeEnabled = false;
                    boolean safeMediaVolumeBypass = SystemProperties.getBoolean("audio.safemedia.bypass", false);
                    if (!safeMediaVolumeEnabled && !safeMediaVolumeBypass) {
                        persistedState = 3;
                        if (this.mSafeMediaVolumeState != 2) {
                            if (this.mMusicActiveMs == 0) {
                                Log.d(TAG, " onConfigureSafeVolume mSafeMediaVolumeState = SAFE_MEDIA_VOLUME_ACTIVE");
                                this.mSafeMediaVolumeState = 3;
                                enforceSafeMediaVolume(caller);
                            } else {
                                Log.d(TAG, " onConfigureSafeVolume mSafeMediaVolumeState = SAFE_MEDIA_VOLUME_INACTIVE");
                                this.mSafeMediaVolumeState = 2;
                            }
                        }
                    } else {
                        this.mSafeMediaVolumeState = 1;
                        persistedState = 1;
                    }
                    this.mMcc = mcc;
                    sendMsg(this.mAudioHandler, 14, 2, persistedState, 0, null, 0);
                }
                safeMediaVolumeEnabled = true;
                boolean safeMediaVolumeBypass2 = SystemProperties.getBoolean("audio.safemedia.bypass", false);
                if (!safeMediaVolumeEnabled) {
                }
                this.mSafeMediaVolumeState = 1;
                persistedState = 1;
                this.mMcc = mcc;
                sendMsg(this.mAudioHandler, 14, 2, persistedState, 0, null, 0);
            }
        }
    }

    private int checkForRingerModeChange(int oldIndex, int direction, int step, boolean isMuted, String caller, int flags) {
        int result = 1;
        if (isPlatformTelevision() || this.mIsSingleVolume) {
            return 1;
        }
        int ringerMode = getRingerModeInternal();
        switch (ringerMode) {
            case 0:
                if (this.mIsSingleVolume && direction == -1 && oldIndex >= step * 2 && isMuted) {
                    ringerMode = 2;
                } else if (direction == 1 || direction == 101 || direction == 100) {
                    if (!this.mVolumePolicy.volumeUpToExitSilent) {
                        result = 1 | 128;
                    } else {
                        ringerMode = (this.mHasVibrator && direction == 1) ? 1 : 2;
                    }
                }
                result &= -2;
                break;
            case 1:
                if (!this.mHasVibrator) {
                    Log.e(TAG, "checkForRingerModeChange() current ringer mode is vibratebut no vibrator is present");
                    break;
                } else {
                    if (direction == -1) {
                        if (this.mIsSingleVolume && oldIndex >= step * 2 && isMuted) {
                            ringerMode = 2;
                        } else if (this.mPrevVolDirection != -1) {
                            if (this.mVolumePolicy.volumeDownToEnterSilent) {
                                long diff = SystemClock.uptimeMillis() - this.mLoweredFromNormalToVibrateTime;
                                if (diff > this.mVolumePolicy.vibrateToSilentDebounce && this.mRingerModeDelegate.canVolumeDownEnterSilent()) {
                                    ringerMode = 0;
                                }
                            } else {
                                result = 1 | 2048;
                            }
                        }
                    } else if (direction == 1 || direction == 101 || direction == 100) {
                        ringerMode = 2;
                    }
                    result &= -2;
                    break;
                }
            case 2:
                if (direction == -1) {
                    if (this.mHasVibrator) {
                        if (step <= oldIndex && oldIndex < step * 2) {
                            ringerMode = 1;
                            this.mLoweredFromNormalToVibrateTime = SystemClock.uptimeMillis();
                            break;
                        }
                    } else if (oldIndex == step && this.mVolumePolicy.volumeDownToEnterSilent) {
                        ringerMode = 0;
                        break;
                    }
                } else if (this.mIsSingleVolume && (direction == 101 || direction == -100)) {
                    if (this.mHasVibrator) {
                        ringerMode = 1;
                    } else {
                        ringerMode = 0;
                    }
                    result = 1 & (-2);
                    break;
                }
                break;
            default:
                Log.e(TAG, "checkForRingerModeChange() wrong ringer mode: " + ringerMode);
                break;
        }
        if (isAndroidNPlus(caller) && wouldToggleZenMode(ringerMode) && !this.mNm.isNotificationPolicyAccessGrantedForPackage(caller) && (flags & 4096) == 0) {
            throw new SecurityException("Not allowed to change Do Not Disturb state");
        }
        setRingerMode(ringerMode, "AS.AudioService.checkForRingerModeChange", false);
        this.mPrevVolDirection = direction;
        return result;
    }

    public boolean isStreamAffectedByRingerMode(int streamType) {
        return (this.mRingerModeAffectedStreams & (1 << streamType)) != 0;
    }

    private boolean shouldZenMuteStream(int streamType) {
        if (this.mNm.getZenMode() != 1) {
            return false;
        }
        NotificationManager.Policy zenPolicy = this.mNm.getConsolidatedNotificationPolicy();
        boolean muteAlarms = (zenPolicy.priorityCategories & 32) == 0;
        boolean muteMedia = (zenPolicy.priorityCategories & 64) == 0;
        boolean muteSystem = (zenPolicy.priorityCategories & 128) == 0;
        boolean muteNotificationAndRing = ZenModeConfig.areAllPriorityOnlyRingerSoundsMuted(zenPolicy);
        return (muteAlarms && isAlarm(streamType)) || (muteMedia && isMedia(streamType)) || ((muteSystem && isSystem(streamType)) || (muteNotificationAndRing && isNotificationOrRinger(streamType)));
    }

    private boolean isStreamMutedByRingerOrZenMode(int streamType) {
        return (this.mRingerAndZenModeMutedStreams & (1 << streamType)) != 0;
    }

    private boolean updateZenModeAffectedStreams() {
        if (this.mSystemReady) {
            int zenModeAffectedStreams = 0;
            int zenMode = this.mNm.getZenMode();
            if (zenMode == 2) {
                zenModeAffectedStreams = 0 | 16 | 8;
            } else if (zenMode == 1) {
                NotificationManager.Policy zenPolicy = this.mNm.getConsolidatedNotificationPolicy();
                if ((zenPolicy.priorityCategories & 32) == 0) {
                    zenModeAffectedStreams = 0 | 16;
                }
                if ((zenPolicy.priorityCategories & 64) == 0) {
                    zenModeAffectedStreams |= 8;
                }
                if ((zenPolicy.priorityCategories & 128) == 0) {
                    zenModeAffectedStreams |= 2;
                }
                if (ZenModeConfig.areAllPriorityOnlyRingerSoundsMuted(zenPolicy)) {
                    zenModeAffectedStreams = zenModeAffectedStreams | 32 | 4;
                }
            }
            if (this.mZenModeAffectedStreams != zenModeAffectedStreams) {
                this.mZenModeAffectedStreams = zenModeAffectedStreams;
                return true;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updateRingerAndZenModeAffectedStreams() {
        int ringerModeAffectedStreams;
        int ringerModeAffectedStreams2;
        boolean updatedZenModeAffectedStreams = updateZenModeAffectedStreams();
        int ringerModeAffectedStreams3 = this.mSettings.getSystemIntForUser(this.mContentResolver, "mode_ringer_streams_affected", 166, -2);
        if (this.mIsSingleVolume) {
            ringerModeAffectedStreams3 = 0;
        } else {
            AudioManagerInternal.RingerModeDelegate ringerModeDelegate = this.mRingerModeDelegate;
            if (ringerModeDelegate != null) {
                ringerModeAffectedStreams3 = ringerModeDelegate.getRingerModeAffectedStreams(ringerModeAffectedStreams3);
            }
        }
        if (this.mCameraSoundForced) {
            ringerModeAffectedStreams = ringerModeAffectedStreams3 & (-129);
        } else {
            ringerModeAffectedStreams = ringerModeAffectedStreams3 | 128;
        }
        if (mStreamVolumeAlias[8] == 2) {
            ringerModeAffectedStreams2 = ringerModeAffectedStreams | 256;
        } else {
            ringerModeAffectedStreams2 = ringerModeAffectedStreams & (-257);
        }
        if (ringerModeAffectedStreams2 != this.mRingerModeAffectedStreams) {
            this.mSettings.putSystemIntForUser(this.mContentResolver, "mode_ringer_streams_affected", ringerModeAffectedStreams2, -2);
            this.mRingerModeAffectedStreams = ringerModeAffectedStreams2;
            return true;
        }
        return updatedZenModeAffectedStreams;
    }

    public boolean isStreamAffectedByMute(int streamType) {
        return (this.mMuteAffectedStreams & (1 << streamType)) != 0;
    }

    private void ensureValidDirection(int direction) {
        switch (direction) {
            case -100:
            case -1:
            case 0:
            case 1:
            case 100:
            case 101:
                return;
            default:
                throw new IllegalArgumentException("Bad direction " + direction);
        }
    }

    private void ensureValidStreamType(int streamType) {
        if (streamType < 0 || streamType >= this.mStreamStates.length) {
            throw new IllegalArgumentException("Bad stream type " + streamType);
        }
    }

    private boolean isMuteAdjust(int adjust) {
        return adjust == -100 || adjust == 100 || adjust == 101;
    }

    public boolean isInCommunication() {
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        long ident = Binder.clearCallingIdentity();
        boolean IsInCall = telecomManager.isInCall();
        Binder.restoreCallingIdentity(ident);
        int mode = this.mMode.get();
        return IsInCall || mode == 3 || mode == 2;
    }

    private boolean wasStreamActiveRecently(int stream, int delay_ms) {
        return this.mAudioSystem.isStreamActive(stream, delay_ms) || this.mAudioSystem.isStreamActiveRemotely(stream, delay_ms);
    }

    private int getActiveStreamType(int suggestedStreamType) {
        if (this.mIsSingleVolume && suggestedStreamType == Integer.MIN_VALUE) {
            return 3;
        }
        switch (this.mPlatformType) {
            case 1:
                if (isInCommunication()) {
                    return this.mDeviceBroker.isBluetoothScoActive() ? 6 : 0;
                } else if (suggestedStreamType == Integer.MIN_VALUE) {
                    if (wasStreamActiveRecently(2, sStreamOverrideDelayMs)) {
                        if (DEBUG_VOL) {
                            Log.v(TAG, "getActiveStreamType: Forcing STREAM_RING stream active");
                        }
                        return 2;
                    } else if (wasStreamActiveRecently(5, sStreamOverrideDelayMs)) {
                        if (DEBUG_VOL) {
                            Log.v(TAG, "getActiveStreamType: Forcing STREAM_NOTIFICATION stream active");
                        }
                        return 5;
                    } else {
                        if (DEBUG_VOL) {
                            Log.v(TAG, "getActiveStreamType: Forcing DEFAULT_VOL_STREAM_NO_PLAYBACK(3) b/c default");
                        }
                        return 3;
                    }
                } else if (wasStreamActiveRecently(5, sStreamOverrideDelayMs)) {
                    if (DEBUG_VOL) {
                        Log.v(TAG, "getActiveStreamType: Forcing STREAM_NOTIFICATION stream active");
                    }
                    return 5;
                } else if (wasStreamActiveRecently(2, sStreamOverrideDelayMs)) {
                    if (DEBUG_VOL) {
                        Log.v(TAG, "getActiveStreamType: Forcing STREAM_RING stream active");
                    }
                    return 2;
                }
                break;
        }
        if (isInCommunication()) {
            if (this.mDeviceBroker.isBluetoothScoActive()) {
                if (DEBUG_VOL) {
                    Log.v(TAG, "getActiveStreamType: Forcing STREAM_BLUETOOTH_SCO");
                }
                return 6;
            }
            if (DEBUG_VOL) {
                Log.v(TAG, "getActiveStreamType: Forcing STREAM_VOICE_CALL");
            }
            return 0;
        } else if (this.mAudioSystem.isStreamActive(5, sStreamOverrideDelayMs)) {
            if (DEBUG_VOL) {
                Log.v(TAG, "getActiveStreamType: Forcing STREAM_NOTIFICATION");
            }
            return 5;
        } else if (this.mAudioSystem.isStreamActive(2, sStreamOverrideDelayMs)) {
            if (DEBUG_VOL) {
                Log.v(TAG, "getActiveStreamType: Forcing STREAM_RING");
            }
            return 2;
        } else if (suggestedStreamType == Integer.MIN_VALUE) {
            if (this.mAudioSystem.isStreamActive(5, sStreamOverrideDelayMs)) {
                if (DEBUG_VOL) {
                    Log.v(TAG, "getActiveStreamType: Forcing STREAM_NOTIFICATION");
                }
                return 5;
            } else if (this.mAudioSystem.isStreamActive(2, sStreamOverrideDelayMs)) {
                if (DEBUG_VOL) {
                    Log.v(TAG, "getActiveStreamType: Forcing STREAM_RING");
                }
                return 2;
            } else {
                if (DEBUG_VOL) {
                    Log.v(TAG, "getActiveStreamType: Forcing DEFAULT_VOL_STREAM_NO_PLAYBACK(3) b/c default");
                }
                return 3;
            }
        } else {
            if (DEBUG_VOL) {
                Log.v(TAG, "getActiveStreamType: Returning suggested type " + suggestedStreamType);
            }
            return suggestedStreamType;
        }
    }

    private void broadcastRingerMode(String action, int ringerMode) {
        if (!this.mSystemServer.isPrivileged()) {
            return;
        }
        Intent broadcast = new Intent(action);
        broadcast.putExtra("android.media.EXTRA_RINGER_MODE", ringerMode);
        broadcast.addFlags(603979776);
        sendStickyBroadcastToAll(broadcast);
    }

    private void broadcastVibrateSetting(int vibrateType) {
        if (this.mSystemServer.isPrivileged() && this.mActivityManagerInternal.isSystemReady()) {
            Intent broadcast = new Intent("android.media.VIBRATE_SETTING_CHANGED");
            broadcast.putExtra("android.media.EXTRA_VIBRATE_TYPE", vibrateType);
            broadcast.putExtra("android.media.EXTRA_VIBRATE_SETTING", getVibrateSetting(vibrateType));
            sendBroadcastToAll(broadcast);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void queueMsgUnderWakeLock(Handler handler, int msg, int arg1, int arg2, Object obj, int delay) {
        long ident = Binder.clearCallingIdentity();
        this.mAudioEventWakeLock.acquire();
        Binder.restoreCallingIdentity(ident);
        sendMsg(handler, msg, 2, arg1, arg2, obj, delay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void sendMsg(Handler handler, int msg, int existingMsgPolicy, int arg1, int arg2, Object obj, int delay) {
        if (existingMsgPolicy == 0) {
            handler.removeMessages(msg);
        } else if (existingMsgPolicy == 1 && handler.hasMessages(msg)) {
            return;
        }
        long time = SystemClock.uptimeMillis() + delay;
        handler.sendMessageAtTime(handler.obtainMessage(msg, arg1, arg2, obj), time);
    }

    boolean checkAudioSettingsPermission(String method) {
        if (callingOrSelfHasAudioSettingsPermission()) {
            return true;
        }
        String msg = "Audio Settings Permission Denial: " + method + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
        Log.w(TAG, msg);
        return false;
    }

    private boolean callingOrSelfHasAudioSettingsPermission() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_SETTINGS") == 0;
    }

    private boolean callingHasAudioSettingsPermission() {
        return this.mContext.checkCallingPermission("android.permission.MODIFY_AUDIO_SETTINGS") == 0;
    }

    private boolean hasAudioSettingsPermission(int uid, int pid) {
        return this.mContext.checkPermission("android.permission.MODIFY_AUDIO_SETTINGS", pid, uid) == 0;
    }

    protected void initMinStreamVolumeWithoutModifyAudioSettings() {
        int[] iArr;
        int deviceForAlarm = 4194304;
        if (Float.isNaN(AudioSystem.getStreamVolumeDB(4, MIN_STREAM_VOLUME[4], 4194304))) {
            deviceForAlarm = 2;
        }
        int idx = MAX_STREAM_VOLUME[4];
        while (idx >= MIN_STREAM_VOLUME[4] && AudioSystem.getStreamVolumeDB(4, idx, deviceForAlarm) >= MIN_ALARM_ATTENUATION_NON_PRIVILEGED_DB) {
            idx--;
        }
        int safeIndex = MIN_STREAM_VOLUME[4];
        if (idx > safeIndex) {
            safeIndex = Math.min(idx + 1, MAX_STREAM_VOLUME[4]);
        }
        for (int stream : mStreamVolumeAlias) {
            if (mStreamVolumeAlias[stream] == 4) {
                this.mStreamStates[stream].updateNoPermMinIndex(safeIndex);
            }
        }
    }

    public int getDeviceForStream(int stream) {
        return selectOneAudioDevice(getDeviceSetForStream(stream));
    }

    private int selectOneAudioDevice(Set<Integer> deviceSet) {
        if (deviceSet.isEmpty()) {
            return 0;
        }
        if (deviceSet.size() == 1) {
            return deviceSet.iterator().next().intValue();
        }
        if (deviceSet.contains(2)) {
            return 2;
        }
        if (deviceSet.contains(4194304)) {
            return 4194304;
        }
        if (deviceSet.contains(262144)) {
            return 262144;
        }
        if (deviceSet.contains(262145)) {
            return 262145;
        }
        if (deviceSet.contains(2097152)) {
            return 2097152;
        }
        if (deviceSet.contains(524288)) {
            return 524288;
        }
        for (Integer num : deviceSet) {
            int deviceType = num.intValue();
            if (AudioSystem.DEVICE_OUT_ALL_A2DP_SET.contains(Integer.valueOf(deviceType))) {
                return deviceType;
            }
        }
        Log.w(TAG, "selectOneAudioDevice returning DEVICE_NONE from invalid device combination " + AudioSystem.deviceSetToString(deviceSet));
        return 0;
    }

    @Deprecated
    public int getDeviceMaskForStream(int streamType) {
        ensureValidStreamType(streamType);
        long token = Binder.clearCallingIdentity();
        try {
            return AudioSystem.getDeviceMaskFromSet(getDeviceSetForStreamDirect(streamType));
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Set<Integer> getDeviceSetForStreamDirect(int stream) {
        AudioAttributes attr = AudioProductStrategy.getAudioAttributesForStrategyWithLegacyStreamType(stream);
        Set<Integer> deviceSet = AudioSystem.generateAudioDeviceTypesSet(getDevicesForAttributesInt(attr, true));
        return deviceSet;
    }

    public Set<Integer> getDeviceSetForStream(int stream) {
        Set<Integer> observeDevicesForStream_syncVSS;
        ensureValidStreamType(stream);
        synchronized (VolumeStreamState.class) {
            observeDevicesForStream_syncVSS = this.mStreamStates[stream].observeDevicesForStream_syncVSS(true);
        }
        return observeDevicesForStream_syncVSS;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onObserveDevicesForAllStreams(int skipStream) {
        synchronized (this.mSettingsLock) {
            synchronized (VolumeStreamState.class) {
                int stream = 0;
                while (true) {
                    VolumeStreamState[] volumeStreamStateArr = this.mStreamStates;
                    if (stream < volumeStreamStateArr.length) {
                        if (stream != skipStream) {
                            Set<Integer> deviceSet = volumeStreamStateArr[stream].observeDevicesForStream_syncVSS(false);
                            for (Integer device : deviceSet) {
                                updateVolumeStates(device.intValue(), stream, "AudioService#onObserveDevicesForAllStreams");
                            }
                        }
                        stream++;
                    }
                }
            }
        }
    }

    public void postObserveDevicesForAllStreams() {
        postObserveDevicesForAllStreams(-1);
    }

    public void postObserveDevicesForAllStreams(int skipStream) {
        sendMsg(this.mAudioHandler, 27, 2, skipStream, 0, null, 0);
    }

    public void registerDeviceVolumeDispatcherForAbsoluteVolume(boolean register, IAudioDeviceVolumeDispatcher cb, String packageName, AudioDeviceAttributes device, List<VolumeInfo> volumes, boolean handlesVolumeAdjustment) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED") != 0) {
            throw new SecurityException("Missing MODIFY_AUDIO_ROUTING or BLUETOOTH_PRIVILEGED permissions");
        }
        Objects.requireNonNull(device);
        Objects.requireNonNull(volumes);
        int deviceOut = device.getInternalType();
        if (register) {
            AbsoluteVolumeDeviceInfo info = new AbsoluteVolumeDeviceInfo(device, volumes, cb, handlesVolumeAdjustment);
            boolean volumeBehaviorChanged = removeAudioSystemDeviceOutFromFullVolumeDevices(deviceOut) | removeAudioSystemDeviceOutFromFixedVolumeDevices(deviceOut) | (addAudioSystemDeviceOutToAbsVolumeDevices(deviceOut, info) == null);
            if (volumeBehaviorChanged) {
                dispatchDeviceVolumeBehavior(device, 3);
            }
            for (VolumeInfo volumeInfo : volumes) {
                if (volumeInfo.getVolumeIndex() != -100 && volumeInfo.getMinVolumeIndex() != -100 && volumeInfo.getMaxVolumeIndex() != -100) {
                    if (volumeInfo.hasStreamType()) {
                        setStreamVolumeInt(volumeInfo.getStreamType(), rescaleIndex(volumeInfo, volumeInfo.getStreamType()), deviceOut, false, packageName, true);
                    } else {
                        int[] legacyStreamTypes = volumeInfo.getVolumeGroup().getLegacyStreamTypes();
                        int i = 0;
                        for (int length = legacyStreamTypes.length; i < length; length = length) {
                            int streamType = legacyStreamTypes[i];
                            setStreamVolumeInt(streamType, rescaleIndex(volumeInfo, streamType), deviceOut, false, packageName, true);
                            i++;
                        }
                    }
                }
            }
            return;
        }
        boolean wasAbsVol = removeAudioSystemDeviceOutFromAbsVolumeDevices(deviceOut) != null;
        if (wasAbsVol) {
            dispatchDeviceVolumeBehavior(device, 0);
        }
    }

    public void setDeviceVolumeBehavior(AudioDeviceAttributes device, int deviceVolumeBehavior, String pkgName) {
        enforceModifyAudioRoutingPermission();
        Objects.requireNonNull(device);
        AudioManager.enforceValidVolumeBehavior(deviceVolumeBehavior);
        if (DEBUG_DEVICES) {
            sVolumeLogger.log(new AudioEventLogger.StringEvent("setDeviceVolumeBehavior: dev:" + AudioSystem.getOutputDeviceName(device.getInternalType()) + " addr:" + device.getAddress() + " behavior:" + AudioDeviceVolumeManager.volumeBehaviorName(deviceVolumeBehavior) + " pack:" + pkgName).printLog(TAG));
        }
        if (pkgName == null) {
            pkgName = "";
        }
        if (device.getType() == 8) {
            avrcpSupportsAbsoluteVolume(device.getAddress(), deviceVolumeBehavior == 3);
        } else if (device.getType() == 26) {
            leVcSupportsAbsoluteVolume(device.getAddress(), deviceVolumeBehavior == 3);
        } else {
            setDeviceVolumeBehaviorInternal(device, deviceVolumeBehavior, pkgName);
            persistDeviceVolumeBehavior(device.getInternalType(), deviceVolumeBehavior);
        }
    }

    private void setDeviceVolumeBehaviorInternal(AudioDeviceAttributes device, int deviceVolumeBehavior, String caller) {
        int audioSystemDeviceOut = device.getInternalType();
        boolean volumeBehaviorChanged = false;
        switch (deviceVolumeBehavior) {
            case 0:
                volumeBehaviorChanged = false | (removeAudioSystemDeviceOutFromAbsVolumeDevices(audioSystemDeviceOut) != null) | removeAudioSystemDeviceOutFromFullVolumeDevices(audioSystemDeviceOut) | removeAudioSystemDeviceOutFromFixedVolumeDevices(audioSystemDeviceOut);
                break;
            case 1:
                volumeBehaviorChanged = false | (removeAudioSystemDeviceOutFromAbsVolumeDevices(audioSystemDeviceOut) != null) | addAudioSystemDeviceOutToFullVolumeDevices(audioSystemDeviceOut) | removeAudioSystemDeviceOutFromFixedVolumeDevices(audioSystemDeviceOut);
                break;
            case 2:
                volumeBehaviorChanged = false | (removeAudioSystemDeviceOutFromAbsVolumeDevices(audioSystemDeviceOut) != null) | removeAudioSystemDeviceOutFromFullVolumeDevices(audioSystemDeviceOut) | addAudioSystemDeviceOutToFixedVolumeDevices(audioSystemDeviceOut);
                break;
            case 3:
            case 4:
                throw new IllegalArgumentException("Absolute volume unsupported for now");
        }
        if (volumeBehaviorChanged) {
            sendMsg(this.mAudioHandler, 47, 2, deviceVolumeBehavior, 0, device, 0);
        }
        sDeviceLogger.log(new AudioEventLogger.StringEvent("Volume behavior " + deviceVolumeBehavior + " for dev=0x" + Integer.toHexString(audioSystemDeviceOut) + " from:" + caller));
        postUpdateVolumeStatesForAudioDevice(audioSystemDeviceOut, "setDeviceVolumeBehavior:" + caller);
    }

    public int getDeviceVolumeBehavior(AudioDeviceAttributes device) {
        Objects.requireNonNull(device);
        enforceQueryStateOrModifyRoutingPermission();
        return getDeviceVolumeBehaviorInt(device);
    }

    private int getDeviceVolumeBehaviorInt(AudioDeviceAttributes device) {
        int audioSystemDeviceOut = AudioDeviceInfo.convertDeviceTypeToInternalDevice(device.getType());
        int setDeviceVolumeBehavior = retrieveStoredDeviceVolumeBehavior(audioSystemDeviceOut);
        if (setDeviceVolumeBehavior != -1) {
            return setDeviceVolumeBehavior;
        }
        if (this.mFullVolumeDevices.contains(Integer.valueOf(audioSystemDeviceOut))) {
            return 1;
        }
        if (this.mFixedVolumeDevices.contains(Integer.valueOf(audioSystemDeviceOut))) {
            return 2;
        }
        if (this.mAbsVolumeMultiModeCaseDevices.contains(Integer.valueOf(audioSystemDeviceOut))) {
            return 4;
        }
        if (isAbsoluteVolumeDevice(audioSystemDeviceOut) || isA2dpAbsoluteVolumeDevice(audioSystemDeviceOut)) {
            return 3;
        }
        return (audioSystemDeviceOut == 536870912 && this.mBleVcAbsVolSupported) ? 3 : 0;
    }

    public boolean isVolumeFixed() {
        if (this.mUseFixedVolume) {
            return true;
        }
        AudioAttributes attributes = new AudioAttributes.Builder().setUsage(1).build();
        List<AudioDeviceAttributes> devices = getDevicesForAttributesInt(attributes, true);
        for (AudioDeviceAttributes device : devices) {
            if (getDeviceVolumeBehaviorInt(device) == 2) {
                return true;
            }
        }
        return false;
    }

    public void setWiredDeviceConnectionState(AudioDeviceAttributes attributes, int state, String caller) {
        enforceModifyAudioRoutingPermission();
        if (state != 1 && state != 0) {
            throw new IllegalArgumentException("Invalid state " + state);
        }
        new MediaMetrics.Item("audio.service.setWiredDeviceConnectionState").set(MediaMetrics.Property.ADDRESS, attributes.getAddress()).set(MediaMetrics.Property.CLIENT_NAME, caller).set(MediaMetrics.Property.DEVICE, AudioSystem.getDeviceName(attributes.getInternalType())).set(MediaMetrics.Property.NAME, attributes.getName()).set(MediaMetrics.Property.STATE, state == 1 ? "connected" : "disconnected").record();
        if (DEBUG_DEVICES) {
            String stateInfo = state == 1 ? "Connected" : "Disconnected";
            String eventSource = "setWiredDeviceConnectionState()) from u/pid:" + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid() + ", type=" + AudioSystem.getDeviceName(attributes.getInternalType()) + ", state=" + state + ("(" + stateInfo + ")") + ", address=" + attributes.getAddress() + ", name=" + attributes.getAddress() + ", caller=" + caller;
            Log.d(TAG, eventSource);
        }
        this.mDeviceBroker.setWiredDeviceConnectionState(attributes, state, caller);
    }

    public void setTestDeviceConnectionState(AudioDeviceAttributes device, boolean connected) {
        Objects.requireNonNull(device);
        enforceModifyAudioRoutingPermission();
        this.mDeviceBroker.setTestDeviceConnectionState(device, connected ? 1 : 0);
        sendMsg(this.mAudioHandler, 41, 0, 0, 0, null, 0);
    }

    public void handleBluetoothActiveDeviceChanged(BluetoothDevice newDevice, BluetoothDevice previousDevice, BluetoothProfileConnectionInfo info) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.BLUETOOTH_STACK") != 0) {
            throw new SecurityException("Bluetooth is the only caller allowed");
        }
        if (info == null) {
            throw new IllegalArgumentException("Illegal null BluetoothProfileConnectionInfo for device " + previousDevice + " -> " + newDevice);
        }
        int profile = info.getProfile();
        if (profile != 2 && profile != 11 && profile != 22 && profile != 26 && profile != 21) {
            throw new IllegalArgumentException("Illegal BluetoothProfile profile for device " + previousDevice + " -> " + newDevice + ". Got: " + profile);
        }
        AudioDeviceBroker.BtDeviceChangedData data = new AudioDeviceBroker.BtDeviceChangedData(newDevice, previousDevice, info, "AudioService");
        if (DEBUG_DEVICES) {
            String eventSource = "handleBluetoothActiveDeviceChanged()) from u/pid:" + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid() + " ," + data + ", isSuppressNoisyIntent=" + info.isSuppressNoisyIntent() + ", volume=" + info.getVolume();
            Log.d(TAG, eventSource);
        }
        if ((profile == 22 || profile == 26) && previousDevice != null && info.isLeOutput()) {
            setleVcAbsoluteVolumeSupported(false);
        }
        if (info.isSuppressNoisyIntent()) {
            this.mDeviceBroker.postCheckMessagesMuteMusic();
            if (info.getVolume() != -1 && ((profile == 2 || profile == 11) && newDevice != null)) {
                this.mDeviceBroker.postSetVolumeIndexOnDevice(3, info.getVolume() * 10, 128, "handleBluetoothActiveDeviceChanged");
            }
        }
        sendMsg(this.mAudioHandler, 38, 2, 0, 0, data, 0);
    }

    public void setMusicMute(boolean mute) {
        this.mStreamStates[3].muteInternally(mute);
    }

    public void postAccessoryPlugMediaUnmute(int newDevice) {
        sendMsg(this.mAudioHandler, 21, 2, newDevice, 0, null, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAccessoryPlugMediaUnmute(int newDevice) {
        boolean z = DEBUG_VOL;
        if (z) {
            Log.i(TAG, String.format("onAccessoryPlugMediaUnmute newDevice=%d [%s]", Integer.valueOf(newDevice), AudioSystem.getOutputDeviceName(newDevice)));
        }
        if (this.mNm.getZenMode() != 2 && !isStreamMutedByRingerOrZenMode(3) && DEVICE_MEDIA_UNMUTED_ON_PLUG_SET.contains(Integer.valueOf(newDevice)) && this.mStreamStates[3].mIsMuted && this.mStreamStates[3].getIndex(newDevice) != 0 && getDeviceSetForStreamDirect(3).contains(Integer.valueOf(newDevice))) {
            if (z) {
                Log.i(TAG, String.format("onAccessoryPlugMediaUnmute unmuting device=%d [%s]", Integer.valueOf(newDevice), AudioSystem.getOutputDeviceName(newDevice)));
            }
            this.mStreamStates[3].mute(false);
        }
    }

    public boolean hasHapticChannels(Uri uri) {
        return AudioManager.hasHapticChannelsImpl(this.mContext, uri);
    }

    private void initVolumeGroupStates() {
        for (AudioVolumeGroup avg : getAudioVolumeGroups()) {
            try {
                ensureValidAttributes(avg);
                sVolumeGroupStates.append(avg.getId(), new VolumeGroupState(avg));
            } catch (IllegalArgumentException e) {
                if (DEBUG_VOL) {
                    Log.d(TAG, "volume group " + avg.name() + " for internal policy needs");
                }
            }
        }
        int i = 0;
        while (true) {
            SparseArray<VolumeGroupState> sparseArray = sVolumeGroupStates;
            if (i < sparseArray.size()) {
                VolumeGroupState vgs = sparseArray.valueAt(i);
                vgs.applyAllVolumes();
                i++;
            } else {
                return;
            }
        }
    }

    private void ensureValidAttributes(AudioVolumeGroup avg) {
        boolean hasAtLeastOneValidAudioAttributes = avg.getAudioAttributes().stream().anyMatch(new Predicate() { // from class: com.android.server.audio.AudioService$$ExternalSyntheticLambda7
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AudioService.lambda$ensureValidAttributes$6((AudioAttributes) obj);
            }
        });
        if (!hasAtLeastOneValidAudioAttributes) {
            throw new IllegalArgumentException("Volume Group " + avg.name() + " has no valid audio attributes");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$ensureValidAttributes$6(AudioAttributes aa) {
        return !aa.equals(AudioProductStrategy.getDefaultAttributes());
    }

    private void readVolumeGroupsSettings() {
        if (DEBUG_VOL) {
            Log.v(TAG, "readVolumeGroupsSettings");
        }
        int i = 0;
        while (true) {
            SparseArray<VolumeGroupState> sparseArray = sVolumeGroupStates;
            if (i < sparseArray.size()) {
                VolumeGroupState vgs = sparseArray.valueAt(i);
                vgs.readSettings();
                vgs.applyAllVolumes();
                i++;
            } else {
                return;
            }
        }
    }

    private void restoreVolumeGroups() {
        if (DEBUG_VOL) {
            Log.v(TAG, "restoreVolumeGroups");
        }
        int i = 0;
        while (true) {
            SparseArray<VolumeGroupState> sparseArray = sVolumeGroupStates;
            if (i < sparseArray.size()) {
                VolumeGroupState vgs = sparseArray.valueAt(i);
                vgs.applyAllVolumes();
                i++;
            } else {
                return;
            }
        }
    }

    private void dumpVolumeGroups(PrintWriter pw) {
        pw.println("\nVolume Groups (device: index)");
        int i = 0;
        while (true) {
            SparseArray<VolumeGroupState> sparseArray = sVolumeGroupStates;
            if (i < sparseArray.size()) {
                VolumeGroupState vgs = sparseArray.valueAt(i);
                vgs.dump(pw);
                pw.println("");
                i++;
            } else {
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class VolumeGroupState {
        private AudioAttributes mAudioAttributes;
        private final AudioVolumeGroup mAudioVolumeGroup;
        private final SparseIntArray mIndexMap;
        private int mIndexMax;
        private int mIndexMin;
        private int mLegacyStreamType;
        private int mPublicStreamType;

        private int getDeviceForVolume() {
            return AudioService.this.getDeviceForStream(this.mPublicStreamType);
        }

        private VolumeGroupState(AudioVolumeGroup avg) {
            this.mIndexMap = new SparseIntArray(8);
            this.mLegacyStreamType = -1;
            this.mPublicStreamType = 3;
            this.mAudioAttributes = AudioProductStrategy.getDefaultAttributes();
            this.mAudioVolumeGroup = avg;
            if (AudioService.DEBUG_VOL) {
                Log.v(AudioService.TAG, "VolumeGroupState for " + avg.toString());
            }
            Iterator it = avg.getAudioAttributes().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                AudioAttributes aa = (AudioAttributes) it.next();
                if (!aa.equals(this.mAudioAttributes)) {
                    this.mAudioAttributes = aa;
                    break;
                }
            }
            int[] streamTypes = this.mAudioVolumeGroup.getLegacyStreamTypes();
            if (streamTypes.length != 0) {
                int i = 0;
                this.mLegacyStreamType = streamTypes[0];
                int length = streamTypes.length;
                while (true) {
                    if (i < length) {
                        int streamType = streamTypes[i];
                        if (streamType == -1 || streamType >= AudioSystem.getNumStreamTypes()) {
                            i++;
                        } else {
                            this.mPublicStreamType = streamType;
                            break;
                        }
                    } else {
                        break;
                    }
                }
                this.mIndexMin = AudioService.MIN_STREAM_VOLUME[this.mPublicStreamType];
                this.mIndexMax = AudioService.MAX_STREAM_VOLUME[this.mPublicStreamType];
            } else if (!avg.getAudioAttributes().isEmpty()) {
                this.mIndexMin = AudioSystem.getMinVolumeIndexForAttributes(this.mAudioAttributes);
                this.mIndexMax = AudioSystem.getMaxVolumeIndexForAttributes(this.mAudioAttributes);
            } else {
                Log.e(AudioService.TAG, "volume group: " + this.mAudioVolumeGroup.name() + " has neither valid attributes nor valid stream types assigned");
                return;
            }
            readSettings();
        }

        public int[] getLegacyStreamTypes() {
            return this.mAudioVolumeGroup.getLegacyStreamTypes();
        }

        public String name() {
            return this.mAudioVolumeGroup.name();
        }

        public int getVolumeIndex() {
            return getIndex(getDeviceForVolume());
        }

        public void setVolumeIndex(int index, int flags) {
            if (AudioService.this.mUseFixedVolume) {
                return;
            }
            setVolumeIndex(index, getDeviceForVolume(), flags);
        }

        private void setVolumeIndex(int index, int device, int flags) {
            setVolumeIndexInt(index, device, flags);
            this.mIndexMap.put(device, index);
            AudioService.sendMsg(AudioService.this.mAudioHandler, 2, 2, device, 0, this, 500);
        }

        private void setVolumeIndexInt(int index, int device, int flags) {
            if (AudioService.this.mStreamStates[this.mPublicStreamType].isFullyMuted()) {
                index = 0;
            } else if (this.mPublicStreamType == 6 && index == 0) {
                index = 1;
            }
            AudioSystem.setVolumeIndexForAttributes(this.mAudioAttributes, index, device);
        }

        public int getIndex(int device) {
            int i;
            synchronized (VolumeGroupState.class) {
                int index = this.mIndexMap.get(device, -1);
                i = index != -1 ? index : this.mIndexMap.get(1073741824);
            }
            return i;
        }

        public boolean hasIndexForDevice(int device) {
            boolean z;
            synchronized (VolumeGroupState.class) {
                z = this.mIndexMap.get(device, -1) != -1;
            }
            return z;
        }

        public int getMaxIndex() {
            return this.mIndexMax;
        }

        public int getMinIndex() {
            return this.mIndexMin;
        }

        private boolean isValidLegacyStreamType() {
            int i = this.mLegacyStreamType;
            return i != -1 && i < AudioService.this.mStreamStates.length;
        }

        public void applyAllVolumes() {
            synchronized (VolumeGroupState.class) {
                int deviceForStream = 0;
                int volumeIndexForStream = 0;
                if (isValidLegacyStreamType()) {
                    deviceForStream = AudioService.this.getDeviceForStream(this.mLegacyStreamType);
                    volumeIndexForStream = AudioService.this.getStreamVolume(this.mLegacyStreamType);
                }
                for (int i = 0; i < this.mIndexMap.size(); i++) {
                    int device = this.mIndexMap.keyAt(i);
                    if (device != 1073741824) {
                        int index = this.mIndexMap.valueAt(i);
                        if (device != deviceForStream || volumeIndexForStream != index) {
                            if (AudioService.DEBUG_VOL) {
                                Log.v(AudioService.TAG, "applyAllVolumes: restore index " + index + " for group " + this.mAudioVolumeGroup.name() + " and device " + AudioSystem.getOutputDeviceName(device));
                            }
                            setVolumeIndexInt(index, device, 0);
                        }
                    }
                }
                int index2 = getIndex(1073741824);
                if (AudioService.DEBUG_VOL) {
                    Log.v(AudioService.TAG, "applyAllVolumes: restore default device index " + index2 + " for group " + this.mAudioVolumeGroup.name());
                }
                if (isValidLegacyStreamType()) {
                    int defaultStreamIndex = (AudioService.this.mStreamStates[this.mLegacyStreamType].getIndex(1073741824) + 5) / 10;
                    if (defaultStreamIndex == index2) {
                        return;
                    }
                }
                setVolumeIndexInt(index2, 1073741824, 0);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void persistVolumeGroup(int device) {
            if (AudioService.this.mUseFixedVolume) {
                return;
            }
            if (AudioService.DEBUG_VOL) {
                Log.v(AudioService.TAG, "persistVolumeGroup: storing index " + getIndex(device) + " for group " + this.mAudioVolumeGroup.name() + ", device " + AudioSystem.getOutputDeviceName(device) + " and User=" + ActivityManager.getCurrentUser());
            }
            boolean success = AudioService.this.mSettings.putSystemIntForUser(AudioService.this.mContentResolver, getSettingNameForDevice(device), getIndex(device), -2);
            if (!success) {
                Log.e(AudioService.TAG, "persistVolumeGroup failed for group " + this.mAudioVolumeGroup.name());
            }
        }

        public void readSettings() {
            int defaultIndex;
            synchronized (VolumeGroupState.class) {
                this.mIndexMap.clear();
                if (AudioService.this.mUseFixedVolume) {
                    this.mIndexMap.put(1073741824, this.mIndexMax);
                    return;
                }
                for (Integer num : AudioSystem.DEVICE_OUT_ALL_SET) {
                    int device = num.intValue();
                    if (device != 1073741824) {
                        defaultIndex = -1;
                    } else {
                        defaultIndex = AudioSystem.DEFAULT_STREAM_VOLUME[this.mPublicStreamType];
                    }
                    String name = getSettingNameForDevice(device);
                    int index = AudioService.this.mSettings.getSystemIntForUser(AudioService.this.mContentResolver, name, defaultIndex, -2);
                    if (index != -1) {
                        if (this.mPublicStreamType == 7 && AudioService.this.mCameraSoundForced) {
                            index = this.mIndexMax;
                        }
                        if (AudioService.DEBUG_VOL) {
                            Log.v(AudioService.TAG, "readSettings: found stored index " + getValidIndex(index) + " for group " + this.mAudioVolumeGroup.name() + ", device: " + name + ", User=" + ActivityManager.getCurrentUser());
                        }
                        this.mIndexMap.put(device, getValidIndex(index));
                    }
                }
            }
        }

        private int getValidIndex(int index) {
            int i = this.mIndexMin;
            if (index < i) {
                return i;
            }
            if (AudioService.this.mUseFixedVolume || index > this.mIndexMax) {
                return this.mIndexMax;
            }
            return index;
        }

        public String getSettingNameForDevice(int device) {
            String suffix = AudioSystem.getOutputDeviceName(device);
            if (suffix.isEmpty()) {
                return this.mAudioVolumeGroup.name();
            }
            return this.mAudioVolumeGroup.name() + "_" + AudioSystem.getOutputDeviceName(device);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(PrintWriter pw) {
            pw.println("- VOLUME GROUP " + this.mAudioVolumeGroup.name() + ":");
            pw.print("   Min: ");
            pw.println(this.mIndexMin);
            pw.print("   Max: ");
            pw.println(this.mIndexMax);
            pw.print("   Current: ");
            for (int i = 0; i < this.mIndexMap.size(); i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                int device = this.mIndexMap.keyAt(i);
                pw.print(Integer.toHexString(device));
                String deviceName = device == 1073741824 ? HealthServiceWrapperHidl.INSTANCE_VENDOR : AudioSystem.getOutputDeviceName(device);
                if (!deviceName.isEmpty()) {
                    pw.print(" (");
                    pw.print(deviceName);
                    pw.print(")");
                }
                pw.print(": ");
                pw.print(this.mIndexMap.valueAt(i));
            }
            pw.println();
            pw.print("   Devices: ");
            int n = 0;
            int devices = getDeviceForVolume();
            for (Integer num : AudioSystem.DEVICE_OUT_ALL_SET) {
                int device2 = num.intValue();
                if ((devices & device2) == device2) {
                    int n2 = n + 1;
                    if (n > 0) {
                        pw.print(", ");
                    }
                    pw.print(AudioSystem.getOutputDeviceName(device2));
                    n = n2;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class VolumeStreamState {
        private final SparseIntArray mIndexMap;
        private int mIndexMax;
        private int mIndexMin;
        private int mIndexMinNoPerm;
        private boolean mIsMuted;
        private boolean mIsMutedInternally;
        private Set<Integer> mObservedDeviceSet;
        private final Intent mStreamDevicesChanged;
        private final int mStreamType;
        private final Intent mVolumeChanged;
        private String mVolumeIndexSettingName;

        /* JADX DEBUG: Marked for inline */
        /* JADX DEBUG: Method not inlined, still used in: [com.android.server.audio.AudioService.muteRingerModeStreams():void] */
        /* renamed from: -$$Nest$fgetmIndexMap  reason: not valid java name */
        static /* bridge */ /* synthetic */ SparseIntArray m1920$$Nest$fgetmIndexMap(VolumeStreamState volumeStreamState) {
            return volumeStreamState.mIndexMap;
        }

        /* JADX DEBUG: Marked for inline */
        /* JADX DEBUG: Method not inlined, still used in: [com.android.server.audio.AudioService.adjustStreamVolume(int, int, int, java.lang.String, java.lang.String, int, int, java.lang.String, boolean, int):void] */
        /* renamed from: -$$Nest$fgetmIsMuted  reason: not valid java name */
        static /* bridge */ /* synthetic */ boolean m1923$$Nest$fgetmIsMuted(VolumeStreamState volumeStreamState) {
            return volumeStreamState.mIsMuted;
        }

        private VolumeStreamState(String settingName, int streamType) {
            this.mObservedDeviceSet = new TreeSet();
            this.mIndexMap = new SparseIntArray(8) { // from class: com.android.server.audio.AudioService.VolumeStreamState.1
                @Override // android.util.SparseIntArray
                public void put(int key, int value) {
                    super.put(key, value);
                    record("put", key, value);
                }

                @Override // android.util.SparseIntArray
                public void setValueAt(int index, int value) {
                    super.setValueAt(index, value);
                    record("setValueAt", keyAt(index), value);
                }

                private void record(String event, int key, int value) {
                    String device = key == 1073741824 ? HealthServiceWrapperHidl.INSTANCE_VENDOR : AudioSystem.getOutputDeviceName(key);
                    new MediaMetrics.Item("audio.volume." + AudioSystem.streamToString(VolumeStreamState.this.mStreamType) + "." + device).set(MediaMetrics.Property.EVENT, event).set(MediaMetrics.Property.INDEX, Integer.valueOf(value)).set(MediaMetrics.Property.MIN_INDEX, Integer.valueOf(VolumeStreamState.this.mIndexMin)).set(MediaMetrics.Property.MAX_INDEX, Integer.valueOf(VolumeStreamState.this.mIndexMax)).record();
                }
            };
            this.mVolumeIndexSettingName = settingName;
            this.mStreamType = streamType;
            int i = AudioService.MIN_STREAM_VOLUME[streamType] * 10;
            this.mIndexMin = i;
            this.mIndexMinNoPerm = i;
            int i2 = AudioService.MAX_STREAM_VOLUME[streamType] * 10;
            this.mIndexMax = i2;
            int status = AudioSystem.initStreamVolume(streamType, this.mIndexMin / 10, i2 / 10);
            if (status != 0) {
                AudioService.sLifecycleLogger.log(new AudioEventLogger.StringEvent("VSS() stream:" + streamType + " initStreamVolume=" + status).printLog(1, AudioService.TAG));
                AudioService.sendMsg(AudioService.this.mAudioHandler, 34, 1, 0, 0, "VSS()", 2000);
            }
            readSettings();
            Intent intent = new Intent("android.media.VOLUME_CHANGED_ACTION");
            this.mVolumeChanged = intent;
            intent.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", streamType);
            Intent intent2 = new Intent("android.media.STREAM_DEVICES_CHANGED_ACTION");
            this.mStreamDevicesChanged = intent2;
            intent2.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", streamType);
        }

        public void updateNoPermMinIndex(int index) {
            int i = index * 10;
            this.mIndexMinNoPerm = i;
            if (i < this.mIndexMin) {
                Log.e(AudioService.TAG, "Invalid mIndexMinNoPerm for stream " + this.mStreamType);
                this.mIndexMinNoPerm = this.mIndexMin;
            }
        }

        public Set<Integer> observeDevicesForStream_syncVSS(boolean checkOthers) {
            if (!AudioService.this.mSystemServer.isPrivileged()) {
                return new TreeSet();
            }
            Set<Integer> deviceSet = AudioService.this.getDeviceSetForStreamDirect(this.mStreamType);
            if (deviceSet.equals(this.mObservedDeviceSet)) {
                return this.mObservedDeviceSet;
            }
            int devices = AudioSystem.getDeviceMaskFromSet(deviceSet);
            int prevDevices = AudioSystem.getDeviceMaskFromSet(this.mObservedDeviceSet);
            this.mObservedDeviceSet = deviceSet;
            if (checkOthers) {
                AudioService.this.postObserveDevicesForAllStreams(this.mStreamType);
            }
            int[] iArr = AudioService.mStreamVolumeAlias;
            int i = this.mStreamType;
            if (iArr[i] == i) {
                EventLogTags.writeStreamDevicesChanged(i, prevDevices, devices);
            }
            AudioService.sendMsg(AudioService.this.mAudioHandler, 32, 2, prevDevices, devices, this.mStreamDevicesChanged, 0);
            return this.mObservedDeviceSet;
        }

        public String getSettingNameForDevice(int device) {
            if (!hasValidSettingsName()) {
                return null;
            }
            String suffix = AudioSystem.getOutputDeviceName(device);
            if (suffix.isEmpty()) {
                return this.mVolumeIndexSettingName;
            }
            return this.mVolumeIndexSettingName + "_" + suffix;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean hasValidSettingsName() {
            String str = this.mVolumeIndexSettingName;
            return (str == null || str.isEmpty()) ? false : true;
        }

        public void readSettings() {
            int defaultIndex;
            int index;
            synchronized (AudioService.this.mSettingsLock) {
                synchronized (VolumeStreamState.class) {
                    if (AudioService.this.mUseFixedVolume) {
                        this.mIndexMap.put(1073741824, this.mIndexMax);
                        return;
                    }
                    int i = this.mStreamType;
                    if (i != 1 && i != 7) {
                        synchronized (VolumeStreamState.class) {
                            for (Integer num : AudioSystem.DEVICE_OUT_ALL_SET) {
                                int device = num.intValue();
                                if (device != 1073741824) {
                                    defaultIndex = -1;
                                } else {
                                    defaultIndex = AudioSystem.DEFAULT_STREAM_VOLUME[this.mStreamType];
                                }
                                if (!hasValidSettingsName()) {
                                    index = defaultIndex;
                                } else {
                                    String name = getSettingNameForDevice(device);
                                    index = AudioService.this.mSettings.getSystemIntForUser(AudioService.this.mContentResolver, name, defaultIndex, -2);
                                }
                                if (index != -1) {
                                    this.mIndexMap.put(device, getValidIndex(index * 10, true));
                                }
                            }
                        }
                        return;
                    }
                    int index2 = AudioSystem.DEFAULT_STREAM_VOLUME[this.mStreamType] * 10;
                    if (AudioService.this.mCameraSoundForced) {
                        index2 = this.mIndexMax;
                    }
                    this.mIndexMap.put(1073741824, index2);
                }
            }
        }

        private int getAbsoluteVolumeIndex(int index) {
            if (index == 0) {
                return 0;
            }
            if (index > 0 && index <= 3) {
                return ((int) (this.mIndexMax * AudioService.this.mPrescaleAbsoluteVolume[index - 1])) / 10;
            }
            return (this.mIndexMax + 5) / 10;
        }

        private void setStreamVolumeIndex(int index, int device) {
            if (this.mStreamType == 6 && index == 0 && !isFullyMuted()) {
                index = 1;
            }
            AudioSystem.setStreamVolumeIndexAS(this.mStreamType, index, device);
        }

        void applyDeviceVolume_syncVSS(int device) {
            int index;
            if (isFullyMuted()) {
                index = 0;
            } else if (AudioService.this.isAbsoluteVolumeDevice(device) || AudioService.this.isA2dpAbsoluteVolumeDevice(device)) {
                int index2 = getIndex(device);
                index = getAbsoluteVolumeIndex((index2 + 5) / 10);
            } else if (AudioSystem.DEVICE_OUT_ALL_BLE_SET.contains(Integer.valueOf(device)) && AudioService.this.mBleVcAbsVolSupported) {
                index = getAbsoluteVolumeIndex((getIndex(device) + 5) / 10);
            } else if (AudioService.this.isFullVolumeDevice(device)) {
                index = (this.mIndexMax + 5) / 10;
            } else if (device == 134217728) {
                index = (this.mIndexMax + 5) / 10;
            } else {
                int index3 = getIndex(device);
                index = (index3 + 5) / 10;
            }
            setStreamVolumeIndex(index, device);
        }

        public void applyAllVolumes() {
            int index;
            int index2;
            synchronized (VolumeStreamState.class) {
                for (int i = 0; i < this.mIndexMap.size(); i++) {
                    int device = this.mIndexMap.keyAt(i);
                    if (device != 1073741824) {
                        if (isFullyMuted()) {
                            index2 = 0;
                        } else {
                            if (!AudioService.this.isAbsoluteVolumeDevice(device) && !AudioService.this.isA2dpAbsoluteVolumeDevice(device)) {
                                if (AudioSystem.DEVICE_OUT_ALL_BLE_SET.contains(Integer.valueOf(device)) && AudioService.this.mBleVcAbsVolSupported) {
                                    index2 = getAbsoluteVolumeIndex((getIndex(device) + 5) / 10);
                                } else if (AudioService.this.isFullVolumeDevice(device)) {
                                    index2 = (this.mIndexMax + 5) / 10;
                                } else if (device == 134217728) {
                                    index2 = (this.mIndexMax + 5) / 10;
                                } else {
                                    index2 = (this.mIndexMap.valueAt(i) + 5) / 10;
                                }
                            }
                            int index3 = getIndex(device);
                            index2 = getAbsoluteVolumeIndex((index3 + 5) / 10);
                        }
                        if (this.mStreamType != 2 || 10 != this.mIndexMap.get(2, -1) || device != 4 || index2 != 13) {
                            setStreamVolumeIndex(index2, device);
                        }
                    }
                }
                if (isFullyMuted()) {
                    index = 0;
                } else {
                    int index4 = getIndex(1073741824);
                    index = (index4 + 5) / 10;
                }
                setStreamVolumeIndex(index, 1073741824);
            }
        }

        public boolean adjustIndex(int deltaIndex, int device, String caller, boolean hasModifyAudioSettings) {
            return setIndex(getIndex(device) + deltaIndex, device, caller, hasModifyAudioSettings);
        }

        public boolean setIndex(int index, int device, String caller, boolean hasModifyAudioSettings) {
            int oldIndex;
            int index2;
            boolean changed;
            synchronized (AudioService.this.mSettingsLock) {
                synchronized (VolumeStreamState.class) {
                    oldIndex = getIndex(device);
                    index2 = getValidIndex(index, hasModifyAudioSettings);
                    if (this.mStreamType == 7 && AudioService.this.mCameraSoundForced) {
                        index2 = this.mIndexMax;
                    }
                    this.mIndexMap.put(device, index2);
                    changed = oldIndex != index2;
                    boolean isCurrentDevice = device == AudioService.this.getDeviceForStream(this.mStreamType);
                    int numStreamTypes = AudioSystem.getNumStreamTypes();
                    for (int streamType = numStreamTypes - 1; streamType >= 0; streamType--) {
                        VolumeStreamState aliasStreamState = AudioService.this.mStreamStates[streamType];
                        if (streamType != this.mStreamType && AudioService.mStreamVolumeAlias[streamType] == this.mStreamType && (changed || !aliasStreamState.hasIndexForDevice(device))) {
                            int scaledIndex = AudioService.this.rescaleIndex(index2, this.mStreamType, streamType);
                            aliasStreamState.setIndex(scaledIndex, device, caller, hasModifyAudioSettings);
                            if (isCurrentDevice) {
                                aliasStreamState.setIndex(scaledIndex, AudioService.this.getDeviceForStream(streamType), caller, hasModifyAudioSettings);
                            }
                        }
                    }
                    if (changed && this.mStreamType == 2 && device == 2) {
                        for (int i = 0; i < this.mIndexMap.size(); i++) {
                            int otherDevice = this.mIndexMap.keyAt(i);
                            if (AudioSystem.DEVICE_OUT_ALL_SCO_SET.contains(Integer.valueOf(otherDevice))) {
                                this.mIndexMap.put(otherDevice, index2);
                            }
                        }
                    }
                }
            }
            if (changed) {
                int oldIndex2 = (oldIndex + 5) / 10;
                int index3 = (index2 + 5) / 10;
                int[] iArr = AudioService.mStreamVolumeAlias;
                int i2 = this.mStreamType;
                if (iArr[i2] == i2) {
                    if (caller == null) {
                        Log.w(AudioService.TAG, "No caller for volume_changed event", new Throwable());
                    }
                    EventLogTags.writeVolumeChanged(this.mStreamType, oldIndex2, index3, this.mIndexMax / 10, caller);
                }
                if (index3 != oldIndex2) {
                    this.mVolumeChanged.putExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", index3);
                    this.mVolumeChanged.putExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", oldIndex2);
                    this.mVolumeChanged.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE_ALIAS", AudioService.mStreamVolumeAlias[this.mStreamType]);
                    AudioService.this.sendBroadcastToAll(this.mVolumeChanged);
                }
            }
            return changed;
        }

        public int getIndex(int device) {
            int index;
            synchronized (VolumeStreamState.class) {
                index = this.mIndexMap.get(device, -1);
                if (index == -1) {
                    index = this.mIndexMap.get(1073741824);
                }
            }
            return index;
        }

        public boolean hasIndexForDevice(int device) {
            boolean z;
            synchronized (VolumeStreamState.class) {
                z = this.mIndexMap.get(device, -1) != -1;
            }
            return z;
        }

        public int getMaxIndex() {
            return this.mIndexMax;
        }

        public int getMinIndex() {
            return this.mIndexMin;
        }

        public int getMinIndex(boolean isPrivileged) {
            return isPrivileged ? this.mIndexMin : this.mIndexMinNoPerm;
        }

        public void setAllIndexes(VolumeStreamState srcStream, String caller) {
            if (this.mStreamType == srcStream.mStreamType) {
                return;
            }
            int srcStreamType = srcStream.getStreamType();
            int index = srcStream.getIndex(1073741824);
            int index2 = AudioService.this.rescaleIndex(index, srcStreamType, this.mStreamType);
            for (int i = 0; i < this.mIndexMap.size(); i++) {
                SparseIntArray sparseIntArray = this.mIndexMap;
                sparseIntArray.put(sparseIntArray.keyAt(i), index2);
            }
            SparseIntArray srcMap = srcStream.mIndexMap;
            for (int i2 = 0; i2 < srcMap.size(); i2++) {
                int device = srcMap.keyAt(i2);
                int index3 = srcMap.valueAt(i2);
                setIndex(AudioService.this.rescaleIndex(index3, srcStreamType, this.mStreamType), device, caller, true);
            }
        }

        public void setAllIndexesToMax() {
            for (int i = 0; i < this.mIndexMap.size(); i++) {
                SparseIntArray sparseIntArray = this.mIndexMap;
                sparseIntArray.put(sparseIntArray.keyAt(i), this.mIndexMax);
            }
        }

        public boolean mute(boolean state) {
            boolean changed = false;
            synchronized (VolumeStreamState.class) {
                if (state != this.mIsMuted) {
                    changed = true;
                    this.mIsMuted = state;
                    AudioService.sendMsg(AudioService.this.mAudioHandler, 10, 2, 0, 0, this, 0);
                }
            }
            if (changed) {
                Intent intent = new Intent("android.media.STREAM_MUTE_CHANGED_ACTION");
                intent.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", this.mStreamType);
                intent.putExtra("android.media.EXTRA_STREAM_VOLUME_MUTED", state);
                AudioService.this.sendBroadcastToAll(intent);
            }
            return changed;
        }

        public boolean muteInternally(boolean state) {
            boolean changed = false;
            synchronized (VolumeStreamState.class) {
                if (state != this.mIsMutedInternally) {
                    changed = true;
                    this.mIsMutedInternally = state;
                    applyAllVolumes();
                }
            }
            if (changed) {
                AudioService.sVolumeLogger.log(new AudioServiceEvents.VolumeEvent(9, this.mStreamType, state));
            }
            return changed;
        }

        public boolean isFullyMuted() {
            return this.mIsMuted || this.mIsMutedInternally;
        }

        public int getStreamType() {
            return this.mStreamType;
        }

        public void checkFixedVolumeDevices() {
            synchronized (VolumeStreamState.class) {
                if (AudioService.mStreamVolumeAlias[this.mStreamType] == 3) {
                    for (int i = 0; i < this.mIndexMap.size(); i++) {
                        int device = this.mIndexMap.keyAt(i);
                        int index = this.mIndexMap.valueAt(i);
                        if (AudioService.this.isFullVolumeDevice(device) || (AudioService.this.isFixedVolumeDevice(device) && index != 0)) {
                            this.mIndexMap.put(device, this.mIndexMax);
                        }
                        applyDeviceVolume_syncVSS(device);
                    }
                }
            }
        }

        private int getValidIndex(int index, boolean hasModifyAudioSettings) {
            int indexMin = hasModifyAudioSettings ? this.mIndexMin : this.mIndexMinNoPerm;
            if (index < indexMin) {
                return indexMin;
            }
            if (AudioService.this.mUseFixedVolume || index > this.mIndexMax) {
                return this.mIndexMax;
            }
            return index;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(PrintWriter pw) {
            pw.print("   Muted: ");
            pw.println(this.mIsMuted);
            pw.print("   Muted Internally: ");
            pw.println(this.mIsMutedInternally);
            pw.print("   Min: ");
            pw.print((this.mIndexMin + 5) / 10);
            if (this.mIndexMin != this.mIndexMinNoPerm) {
                pw.print(" w/o perm:");
                pw.println((this.mIndexMinNoPerm + 5) / 10);
            } else {
                pw.println();
            }
            pw.print("   Max: ");
            pw.println((this.mIndexMax + 5) / 10);
            pw.print("   streamVolume:");
            pw.println(AudioService.this.getStreamVolume(this.mStreamType));
            pw.print("   Current: ");
            for (int i = 0; i < this.mIndexMap.size(); i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                int device = this.mIndexMap.keyAt(i);
                pw.print(Integer.toHexString(device));
                String deviceName = device == 1073741824 ? HealthServiceWrapperHidl.INSTANCE_VENDOR : AudioSystem.getOutputDeviceName(device);
                if (!deviceName.isEmpty()) {
                    pw.print(" (");
                    pw.print(deviceName);
                    pw.print(")");
                }
                pw.print(": ");
                int index = (this.mIndexMap.valueAt(i) + 5) / 10;
                pw.print(index);
            }
            pw.println();
            pw.print("   Devices: ");
            pw.print(AudioSystem.deviceSetToString(AudioService.this.getDeviceSetForStream(this.mStreamType)));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AudioSystemThread extends Thread {
        AudioSystemThread() {
            super("AudioService");
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            Looper.prepare();
            synchronized (AudioService.this) {
                AudioService.this.mAudioHandler = new AudioHandler();
                AudioService.this.notify();
            }
            Looper.loop();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class DeviceVolumeUpdate {
        private static final int NO_NEW_INDEX = -2049;
        final String mCaller;
        final int mDevice;
        final int mStreamType;
        private final int mVssVolIndex;

        DeviceVolumeUpdate(int streamType, int vssVolIndex, int device, String caller) {
            this.mStreamType = streamType;
            this.mVssVolIndex = vssVolIndex;
            this.mDevice = device;
            this.mCaller = caller;
        }

        DeviceVolumeUpdate(int streamType, int device, String caller) {
            this.mStreamType = streamType;
            this.mVssVolIndex = NO_NEW_INDEX;
            this.mDevice = device;
            this.mCaller = caller;
        }

        boolean hasVolumeIndex() {
            return this.mVssVolIndex != NO_NEW_INDEX;
        }

        int getVolumeIndex() throws IllegalStateException {
            Preconditions.checkState(this.mVssVolIndex != NO_NEW_INDEX);
            return this.mVssVolIndex;
        }
    }

    public void postSetVolumeIndexOnDevice(int streamType, int vssVolIndex, int device, String caller) {
        sendMsg(this.mAudioHandler, 26, 2, 0, 0, new DeviceVolumeUpdate(streamType, vssVolIndex, device, caller), 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postApplyVolumeOnDevice(int streamType, int device, String caller) {
        sendMsg(this.mAudioHandler, 26, 2, 0, 0, new DeviceVolumeUpdate(streamType, device, caller), 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSetVolumeIndexOnDevice(DeviceVolumeUpdate update) {
        VolumeStreamState streamState = this.mStreamStates[update.mStreamType];
        if (update.hasVolumeIndex()) {
            int index = update.getVolumeIndex();
            if (!checkSafeMediaVolume(update.mStreamType, index, update.mDevice)) {
                index = safeMediaVolumeIndex(update.mDevice);
            }
            streamState.setIndex(index, update.mDevice, update.mCaller, true);
            sVolumeLogger.log(new AudioEventLogger.StringEvent(update.mCaller + " dev:0x" + Integer.toHexString(update.mDevice) + " volIdx:" + index));
        } else {
            sVolumeLogger.log(new AudioEventLogger.StringEvent(update.mCaller + " update vol on dev:0x" + Integer.toHexString(update.mDevice)));
        }
        setDeviceVolume(streamState, update.mDevice);
    }

    void setDeviceVolume(VolumeStreamState streamState, int device) {
        synchronized (VolumeStreamState.class) {
            streamState.applyDeviceVolume_syncVSS(device);
            int numStreamTypes = AudioSystem.getNumStreamTypes();
            for (int streamType = numStreamTypes - 1; streamType >= 0; streamType--) {
                if (streamType != streamState.mStreamType && mStreamVolumeAlias[streamType] == streamState.mStreamType) {
                    int streamDevice = getDeviceForStream(streamType);
                    if (device != streamDevice && (isAbsoluteVolumeDevice(device) || isA2dpAbsoluteVolumeDevice(device))) {
                        this.mStreamStates[streamType].applyDeviceVolume_syncVSS(device);
                    }
                    this.mStreamStates[streamType].applyDeviceVolume_syncVSS(streamDevice);
                }
            }
        }
        sendMsg(this.mAudioHandler, 1, 2, device, 0, streamState, 500);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AudioHandler extends Handler {
        AudioHandler() {
        }

        AudioHandler(Looper looper) {
            super(looper);
        }

        private void setAllVolumes(VolumeStreamState streamState) {
            streamState.applyAllVolumes();
            int numStreamTypes = AudioSystem.getNumStreamTypes();
            for (int streamType = numStreamTypes - 1; streamType >= 0; streamType--) {
                if (streamType != streamState.mStreamType && AudioService.mStreamVolumeAlias[streamType] == streamState.mStreamType) {
                    AudioService.this.mStreamStates[streamType].applyAllVolumes();
                }
            }
        }

        private void persistVolume(VolumeStreamState streamState, int device) {
            if (AudioService.this.mUseFixedVolume) {
                return;
            }
            if ((!AudioService.this.mIsSingleVolume || streamState.mStreamType == 3) && streamState.hasValidSettingsName()) {
                AudioService.this.mSettings.putSystemIntForUser(AudioService.this.mContentResolver, streamState.getSettingNameForDevice(device), (streamState.getIndex(device) + 5) / 10, -2);
            }
        }

        private void persistRingerMode(int ringerMode) {
            if (AudioService.this.mUseFixedVolume) {
                return;
            }
            AudioService.this.mSettings.putGlobalInt(AudioService.this.mContentResolver, "mode_ringer", ringerMode);
        }

        private void onPersistSafeVolumeState(int state) {
            AudioService.this.mSettings.putGlobalInt(AudioService.this.mContentResolver, "audio_safe_volume_state", state);
        }

        private void onNotifyVolumeEvent(IAudioPolicyCallback apc, int direction) {
            try {
                apc.notifyVolumeAdjust(direction);
            } catch (Exception e) {
            }
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    AudioService.this.setDeviceVolume((VolumeStreamState) msg.obj, msg.arg1);
                    return;
                case 1:
                    persistVolume((VolumeStreamState) msg.obj, msg.arg1);
                    return;
                case 2:
                    VolumeGroupState vgs = (VolumeGroupState) msg.obj;
                    vgs.persistVolumeGroup(msg.arg1);
                    return;
                case 3:
                    persistRingerMode(AudioService.this.getRingerModeInternal());
                    return;
                case 4:
                    AudioService.this.onAudioServerDied();
                    return;
                case 5:
                    AudioService.this.mSfxHelper.playSoundEffect(msg.arg1, msg.arg2);
                    return;
                case 7:
                    LoadSoundEffectReply reply = (LoadSoundEffectReply) msg.obj;
                    if (AudioService.this.mSystemReady) {
                        AudioService.this.mSfxHelper.loadSoundEffects(reply);
                        return;
                    }
                    Log.w(AudioService.TAG, "[schedule]loadSoundEffects() called before boot complete");
                    if (reply != null) {
                        reply.run(false);
                        return;
                    }
                    return;
                case 8:
                    String eventSource = (String) msg.obj;
                    int useCase = msg.arg1;
                    int config = msg.arg2;
                    if (useCase == 1) {
                        Log.wtf(AudioService.TAG, "Invalid force use FOR_MEDIA in AudioService from " + eventSource);
                        return;
                    }
                    new MediaMetrics.Item("audio.forceUse." + AudioSystem.forceUseUsageToString(useCase)).set(MediaMetrics.Property.EVENT, "setForceUse").set(MediaMetrics.Property.FORCE_USE_DUE_TO, eventSource).set(MediaMetrics.Property.FORCE_USE_MODE, AudioSystem.forceUseConfigToString(config)).record();
                    AudioService.sForceUseLogger.log(new AudioServiceEvents.ForceUseEvent(useCase, config, eventSource));
                    AudioService.this.mAudioSystem.setForceUse(useCase, config);
                    return;
                case 10:
                    setAllVolumes((VolumeStreamState) msg.obj);
                    return;
                case 11:
                    AudioService.this.onCheckMusicActive((String) msg.obj);
                    return;
                case 12:
                case 13:
                    AudioService.this.onConfigureSafeVolume(msg.what == 13, (String) msg.obj);
                    return;
                case 14:
                    onPersistSafeVolumeState(msg.arg1);
                    return;
                case 15:
                    AudioService.this.mSfxHelper.unloadSoundEffects();
                    return;
                case 16:
                    AudioService.this.onSystemReady();
                    return;
                case 17:
                    int musicActiveMs = msg.arg1;
                    AudioService.this.mSettings.putSecureIntForUser(AudioService.this.mContentResolver, "unsafe_volume_music_active_ms", musicActiveMs, -2);
                    return;
                case 18:
                    AudioService.this.onUnmuteStream(msg.arg1, msg.arg2);
                    return;
                case 19:
                    AudioService.this.onDynPolicyMixStateUpdate((String) msg.obj, msg.arg1);
                    return;
                case 20:
                    AudioService.this.onIndicateSystemReady();
                    return;
                case 21:
                    AudioService.this.onAccessoryPlugMediaUnmute(msg.arg1);
                    return;
                case 22:
                    onNotifyVolumeEvent((IAudioPolicyCallback) msg.obj, msg.arg1);
                    return;
                case 23:
                    AudioService.this.onDispatchAudioServerStateChange(msg.arg1 == 1);
                    return;
                case 24:
                    AudioService.this.onEnableSurroundFormats((ArrayList) msg.obj);
                    return;
                case 25:
                    AudioService.this.onUpdateRingerModeServiceInt();
                    return;
                case 26:
                    AudioService.this.onSetVolumeIndexOnDevice((DeviceVolumeUpdate) msg.obj);
                    return;
                case 27:
                    AudioService.this.onObserveDevicesForAllStreams(msg.arg1);
                    return;
                case 28:
                    AudioService.this.onCheckVolumeCecOnHdmiConnection(msg.arg1, (String) msg.obj);
                    return;
                case 29:
                    AudioService.this.onPlaybackConfigChange((List) msg.obj);
                    return;
                case 30:
                    AudioService.this.mSystemServer.sendMicrophoneMuteChangedIntent();
                    return;
                case 31:
                    synchronized (AudioService.this.mDeviceBroker.mSetModeLock) {
                        if (msg.obj != null) {
                            SetModeDeathHandler h = (SetModeDeathHandler) msg.obj;
                            if (AudioService.this.mSetModeDeathHandlers.indexOf(h) >= 0) {
                                boolean wasActive = h.isActive();
                                h.setPlaybackActive(AudioService.this.mPlaybackMonitor.isPlaybackActiveForUid(h.getUid()));
                                h.setRecordingActive(AudioService.this.mRecordMonitor.isRecordingActiveForUid(h.getUid()));
                                if (wasActive != h.isActive()) {
                                    AudioService.this.onUpdateAudioMode(-1, Process.myPid(), AudioService.this.mContext.getPackageName(), false);
                                }
                                return;
                            }
                            return;
                        }
                        return;
                    }
                case 32:
                    AudioService.this.sendBroadcastToAll(((Intent) msg.obj).putExtra("android.media.EXTRA_PREV_VOLUME_STREAM_DEVICES", msg.arg1).putExtra("android.media.EXTRA_VOLUME_STREAM_DEVICES", msg.arg2));
                    return;
                case 33:
                    AudioService.this.onUpdateVolumeStatesForAudioDevice(msg.arg1, (String) msg.obj);
                    return;
                case 34:
                    AudioService.this.onReinitVolumes((String) msg.obj);
                    return;
                case 35:
                    AudioService.this.onUpdateAccessibilityServiceUids();
                    return;
                case 36:
                    synchronized (AudioService.this.mDeviceBroker.mSetModeLock) {
                        AudioService.this.onUpdateAudioMode(msg.arg1, msg.arg2, (String) msg.obj, false);
                    }
                    return;
                case 37:
                    AudioService.this.onRecordingConfigChange((List) msg.obj);
                    return;
                case 38:
                    AudioService.this.mDeviceBroker.queueOnBluetoothActiveDeviceChanged((AudioDeviceBroker.BtDeviceChangedData) msg.obj);
                    return;
                case 40:
                    AudioService.this.dispatchMode(msg.arg1);
                    return;
                case 41:
                    AudioService.this.onRoutingUpdatedFromAudioThread();
                    return;
                case 42:
                    AudioService.this.mSpatializerHelper.onInitSensors();
                    return;
                case 43:
                    AudioService.this.onPersistSpatialAudioDeviceSettings();
                    return;
                case 44:
                    AudioService.this.onAddAssistantServiceUids(new int[]{msg.arg1});
                    return;
                case 45:
                    AudioService.this.onRemoveAssistantServiceUids(new int[]{msg.arg1});
                    return;
                case 46:
                    AudioService.this.updateActiveAssistantServiceUids();
                    return;
                case 47:
                    AudioService.this.dispatchDeviceVolumeBehavior((AudioDeviceAttributes) msg.obj, msg.arg1);
                    return;
                case 100:
                    AudioService.this.mPlaybackMonitor.disableAudioForUid(msg.arg1 == 1, msg.arg2);
                    AudioService.this.mAudioEventWakeLock.release();
                    return;
                case 101:
                    AudioService.this.onInitStreamsAndVolumes();
                    AudioService.this.mAudioEventWakeLock.release();
                    return;
                case 102:
                    AudioService.this.onInitSpatializer();
                    AudioService.this.mAudioEventWakeLock.release();
                    return;
                case 105:
                    if (!"sss".equals((String) msg.obj) && !AudioService.CALLER_PACKAGE.equals((String) msg.obj)) {
                        AudioService.this.mIntentService.putExtra("request_change_content_mode_pkg", (String) msg.obj);
                        AudioService.this.mContext.startService(AudioService.this.mIntentService);
                        Log.d("DTS_AUDIO", "Start dts service from package " + ((String) msg.obj));
                        return;
                    }
                    return;
                case 106:
                    if (AudioService.TRAN_DUAL_MIC_SUPPORT) {
                        Settings.Global.putString(AudioService.this.mContext.getContentResolver(), "tran_dual_mic_support", "1");
                    } else {
                        Settings.Global.putString(AudioService.this.mContext.getContentResolver(), "tran_dual_mic_support", "0");
                    }
                    AudioService.this.mIntentService = new Intent();
                    AudioService.this.mComponentName = new ComponentName("com.transsion.dtsaudio", "com.transsion.dtsaudio.service.ChangeContentModeService");
                    AudioService.this.mIntentService.setComponent(AudioService.this.mComponentName);
                    Log.d("DTS_AUDIO", "Init dts mic and service");
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SettingsObserver extends ContentObserver {
        SettingsObserver() {
            super(new Handler());
            AudioService.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("zen_mode"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("zen_mode_config_etag"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("mode_ringer_streams_affected"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("dock_audio_media_enabled"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("master_mono"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("master_balance"), false, this);
            AudioService.this.mEncodedSurroundMode = AudioService.this.mSettings.getGlobalInt(AudioService.this.mContentResolver, "encoded_surround_output", 0);
            AudioService.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("encoded_surround_output"), false, this);
            AudioService.this.mEnabledSurroundFormats = AudioService.this.mSettings.getGlobalString(AudioService.this.mContentResolver, "encoded_surround_output_enabled_formats");
            AudioService.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("encoded_surround_output_enabled_formats"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor("voice_interaction_service"), false, this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            synchronized (AudioService.this.mSettingsLock) {
                if (AudioService.this.updateRingerAndZenModeAffectedStreams()) {
                    AudioService audioService = AudioService.this;
                    audioService.setRingerModeInt(audioService.getRingerModeInternal(), false);
                }
                AudioService audioService2 = AudioService.this;
                audioService2.readDockAudioSettings(audioService2.mContentResolver);
                AudioService audioService3 = AudioService.this;
                audioService3.updateMasterMono(audioService3.mContentResolver);
                AudioService audioService4 = AudioService.this;
                audioService4.updateMasterBalance(audioService4.mContentResolver);
                updateEncodedSurroundOutput();
                AudioService audioService5 = AudioService.this;
                audioService5.sendEnabledSurroundFormats(audioService5.mContentResolver, AudioService.this.mSurroundModeChanged);
                AudioService.this.updateAssistantUIdLocked(false);
            }
        }

        private void updateEncodedSurroundOutput() {
            int newSurroundMode = AudioService.this.mSettings.getGlobalInt(AudioService.this.mContentResolver, "encoded_surround_output", 0);
            if (AudioService.this.mEncodedSurroundMode != newSurroundMode) {
                AudioService.this.sendEncodedSurroundMode(newSurroundMode, "SettingsObserver");
                AudioService.this.mDeviceBroker.toggleHdmiIfConnected_Async();
                AudioService.this.mEncodedSurroundMode = newSurroundMode;
                AudioService.this.mSurroundModeChanged = true;
                return;
            }
            AudioService.this.mSurroundModeChanged = false;
        }
    }

    private void avrcpSupportsAbsoluteVolume(String address, boolean support) {
        sVolumeLogger.log(new AudioEventLogger.StringEvent("avrcpSupportsAbsoluteVolume addr=" + address + " support=" + support));
        if (DEBUG_DEVICES) {
            String eventSource = "avrcpSupportsAbsoluteVolume()) from u/pid:" + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid() + ", addr=" + address + ", support=" + support;
            Log.d(TAG, eventSource);
        }
        this.mDeviceBroker.setAvrcpAbsoluteVolumeSupported(support);
        setAvrcpAbsoluteVolumeSupported(support);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAvrcpAbsoluteVolumeSupported(boolean support) {
        if (DEBUG_DEVICES) {
            String eventSource = "setAvrcpAbsoluteVolumeSupported(), support=" + support;
            Log.d(TAG, eventSource);
        }
        this.mAvrcpAbsVolSupported = support;
        sendMsg(this.mAudioHandler, 0, 2, 128, 0, this.mStreamStates[3], 0);
    }

    private void leVcSupportsAbsoluteVolume(String address, boolean support) {
        sVolumeLogger.log(new AudioEventLogger.StringEvent("leVcSupportsAbsoluteVolume addr=" + address + " support=" + support));
        setleVcAbsoluteVolumeSupported(support);
    }

    void setleVcAbsoluteVolumeSupported(boolean support) {
        this.mBleVcAbsVolSupported = support;
        if (DEBUG_MODE) {
            Log.d(TAG, "setleVcAbsoluteVolumeSupported()," + support);
        }
        sendMsg(this.mAudioHandler, 0, 2, 536870912, 0, this.mStreamStates[3], 0);
    }

    public boolean hasMediaDynamicPolicy() {
        synchronized (this.mAudioPolicies) {
            if (this.mAudioPolicies.isEmpty()) {
                return false;
            }
            Collection<AudioPolicyProxy> appColl = this.mAudioPolicies.values();
            for (AudioPolicyProxy app : appColl) {
                if (app.hasMixAffectingUsage(1, 3)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void checkMusicActive(int deviceType, String caller) {
        if (this.mSafeMediaVolumeDevices.contains(Integer.valueOf(deviceType))) {
            sendMsg(this.mAudioHandler, 11, 0, 0, 0, caller, 60000);
        }
    }

    /* loaded from: classes.dex */
    private class AudioServiceBroadcastReceiver extends BroadcastReceiver {
        private AudioServiceBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int config;
            String action = intent.getAction();
            if (action.equals("android.intent.action.DOCK_EVENT")) {
                int dockState = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                switch (dockState) {
                    case 1:
                        config = 7;
                        break;
                    case 2:
                        config = 6;
                        break;
                    case 3:
                        config = 8;
                        break;
                    case 4:
                        config = 9;
                        break;
                    default:
                        config = 0;
                        break;
                }
                if (dockState != 3 && (dockState != 0 || AudioService.this.mDockState != 3)) {
                    AudioService.this.mDeviceBroker.setForceUse_Async(3, config, "ACTION_DOCK_EVENT intent");
                }
                AudioService.this.mDockState = dockState;
            } else if (action.equals("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED")) {
                AudioService.this.mDeviceBroker.receiveBtEvent(intent);
            } else if (action.equals("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED")) {
                boolean isBleTbsIntent = false;
                String broadcastType = intent.getStringExtra("android.bluetooth.device.extra.NAME");
                if (broadcastType != null && "fake_hfp_broadcast".equals(broadcastType)) {
                    isBleTbsIntent = true;
                }
                if (isBleTbsIntent) {
                    if (AudioService.DEBUG_MODE) {
                        Log.d(AudioService.TAG, "Skipped BLE TBS CG Audio State intent here,");
                    }
                } else {
                    AudioService.this.mDeviceBroker.receiveBtEvent(intent);
                    return;
                }
            } else if (action.equals("android.intent.action.SCREEN_ON")) {
                if (AudioService.this.mMonitorRotation) {
                    RotationHelper.enable();
                }
                AudioSystem.setParameters("screen_state=on");
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                if (AudioService.this.mMonitorRotation) {
                    RotationHelper.disable();
                }
                AudioSystem.setParameters("screen_state=off");
            } else if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                AudioService.this.handleConfigurationChanged(context);
            } else if (action.equals("android.intent.action.USER_SWITCHED")) {
                if (AudioService.this.mUserSwitchedReceived) {
                    AudioService.this.mDeviceBroker.postBroadcastBecomingNoisy();
                }
                AudioService.this.mUserSwitchedReceived = true;
                AudioService.this.mMediaFocusControl.discardAudioFocusOwner();
                if (AudioService.this.mSupportsMicPrivacyToggle) {
                    AudioService audioService = AudioService.this;
                    audioService.mMicMuteFromPrivacyToggle = audioService.mSensorPrivacyManagerInternal.isSensorPrivacyEnabled(AudioService.this.getCurrentUserId(), 1);
                    AudioService audioService2 = AudioService.this;
                    audioService2.setMicrophoneMuteNoCallerCheck(audioService2.getCurrentUserId());
                }
                AudioService.this.readAudioSettings(true);
                AudioService.sendMsg(AudioService.this.mAudioHandler, 10, 2, 0, 0, AudioService.this.mStreamStates[3], 0);
            } else if (action.equals("android.intent.action.USER_BACKGROUND")) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (userId >= 0) {
                    UserInfo userInfo = UserManagerService.getInstance().getUserInfo(userId);
                    AudioService.this.killBackgroundUserProcessesWithRecordAudioPermission(userInfo);
                }
                UserManagerService.getInstance().setUserRestriction("no_record_audio", true, userId);
            } else if (action.equals("android.intent.action.USER_FOREGROUND")) {
                UserManagerService.getInstance().setUserRestriction("no_record_audio", false, intent.getIntExtra("android.intent.extra.user_handle", -1));
            } else if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1);
                if (state == 10 || state == 13) {
                    AudioService.this.mDeviceBroker.disconnectAllBluetoothProfiles();
                    AudioService.this.getAudioServiceExtInstance().onReceiveExt(context, intent);
                }
            } else if (action.equals("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION") || action.equals("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION")) {
                AudioService.this.handleAudioEffectBroadcast(context, intent);
            } else if (action.equals("android.intent.action.PACKAGES_SUSPENDED")) {
                int[] suspendedUids = intent.getIntArrayExtra("android.intent.extra.changed_uid_list");
                String[] suspendedPackages = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                if (suspendedPackages == null || suspendedUids == null || suspendedPackages.length != suspendedUids.length) {
                    return;
                }
                for (int i = 0; i < suspendedUids.length; i++) {
                    if (!TextUtils.isEmpty(suspendedPackages[i])) {
                        AudioService.this.mMediaFocusControl.noFocusForSuspendedApp(suspendedPackages[i], suspendedUids[i]);
                    }
                }
            }
            AudioService.this.getAudioServiceExtInstance().onReceiveExt(context, intent);
        }
    }

    /* loaded from: classes.dex */
    private class AudioServiceUserRestrictionsListener implements UserManagerInternal.UserRestrictionsListener {
        private AudioServiceUserRestrictionsListener() {
        }

        @Override // com.android.server.pm.UserManagerInternal.UserRestrictionsListener
        public void onUserRestrictionsChanged(int userId, Bundle newRestrictions, Bundle prevRestrictions) {
            boolean wasRestricted = prevRestrictions.getBoolean("no_unmute_microphone");
            boolean isRestricted = newRestrictions.getBoolean("no_unmute_microphone");
            if (wasRestricted != isRestricted) {
                AudioService.this.mMicMuteFromRestrictions = isRestricted;
                AudioService.this.setMicrophoneMuteNoCallerCheck(userId);
            }
            boolean z = true;
            boolean wasRestricted2 = prevRestrictions.getBoolean("no_adjust_volume") || prevRestrictions.getBoolean("disallow_unmute_device");
            if (!newRestrictions.getBoolean("no_adjust_volume") && !newRestrictions.getBoolean("disallow_unmute_device")) {
                z = false;
            }
            boolean isRestricted2 = z;
            if (wasRestricted2 != isRestricted2) {
                AudioService.this.setMasterMuteInternalNoCallerCheck(isRestricted2, 0, userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAudioEffectBroadcast(Context context, Intent intent) {
        ResolveInfo ri;
        String target = intent.getPackage();
        if (target != null) {
            Log.w(TAG, "effect broadcast already targeted to " + target);
            return;
        }
        intent.addFlags(32);
        List<ResolveInfo> ril = context.getPackageManager().queryBroadcastReceivers(intent, 0);
        if (ril == null || ril.size() == 0 || (ri = ril.get(0)) == null || ri.activityInfo == null || ri.activityInfo.packageName == null) {
            Log.w(TAG, "couldn't find receiver package for effect intent");
            return;
        }
        intent.setPackage(ri.activityInfo.packageName);
        context.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void killBackgroundUserProcessesWithRecordAudioPermission(UserInfo oldUser) {
        PackageManager pm = this.mContext.getPackageManager();
        ComponentName homeActivityName = null;
        if (!oldUser.isManagedProfile()) {
            homeActivityName = ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).getHomeActivityForUser(oldUser.id);
        }
        String[] permissions = {"android.permission.RECORD_AUDIO"};
        try {
            List<PackageInfo> packages = AppGlobals.getPackageManager().getPackagesHoldingPermissions(permissions, 0L, oldUser.id).getList();
            for (int j = packages.size() - 1; j >= 0; j--) {
                PackageInfo pkg = packages.get(j);
                if (UserHandle.getAppId(pkg.applicationInfo.uid) >= 10000 && pm.checkPermission("android.permission.INTERACT_ACROSS_USERS", pkg.packageName) != 0 && (homeActivityName == null || !pkg.packageName.equals(homeActivityName.getPackageName()) || !pkg.applicationInfo.isSystemApp())) {
                    try {
                        int uid = pkg.applicationInfo.uid;
                        ActivityManager.getService().killUid(UserHandle.getAppId(uid), UserHandle.getUserId(uid), "killBackgroundUserProcessesWithAudioRecordPermission");
                    } catch (RemoteException e) {
                        Log.w(TAG, "Error calling killUid", e);
                    }
                }
            }
        } catch (RemoteException e2) {
            throw new AndroidRuntimeException(e2);
        }
    }

    private boolean forceFocusDuckingForAccessibility(AudioAttributes aa, int request, int uid) {
        Bundle extraInfo;
        if (aa != null && aa.getUsage() == 11 && request == 3 && (extraInfo = aa.getBundle()) != null && extraInfo.getBoolean("a11y_force_ducking")) {
            if (uid == 0) {
                return true;
            }
            synchronized (this.mAccessibilityServiceUidsLock) {
                if (this.mAccessibilityServiceUids != null) {
                    int callingUid = Binder.getCallingUid();
                    int i = 0;
                    while (true) {
                        int[] iArr = this.mAccessibilityServiceUids;
                        if (i >= iArr.length) {
                            break;
                        } else if (iArr[i] == callingUid) {
                            return true;
                        } else {
                            i++;
                        }
                    }
                }
                return false;
            }
        }
        return false;
    }

    private boolean isSupportedSystemUsage(int usage) {
        synchronized (this.mSupportedSystemUsagesLock) {
            int i = 0;
            while (true) {
                int[] iArr = this.mSupportedSystemUsages;
                if (i < iArr.length) {
                    if (iArr[i] == usage) {
                        return true;
                    }
                    i++;
                } else {
                    return false;
                }
            }
        }
    }

    private void validateAudioAttributesUsage(AudioAttributes audioAttributes) {
        int usage = audioAttributes.getSystemUsage();
        if (AudioAttributes.isSystemUsage(usage)) {
            if ((usage == 17 && (audioAttributes.getAllFlags() & 65536) != 0 && callerHasPermission("android.permission.CALL_AUDIO_INTERCEPTION")) || callerHasPermission("android.permission.MODIFY_AUDIO_ROUTING")) {
                if (!isSupportedSystemUsage(usage)) {
                    throw new IllegalArgumentException("Unsupported usage " + AudioAttributes.usageToString(usage));
                }
                return;
            }
            throw new SecurityException("Missing MODIFY_AUDIO_ROUTING permission");
        }
    }

    private boolean isValidAudioAttributesUsage(AudioAttributes audioAttributes) {
        int usage = audioAttributes.getSystemUsage();
        if (AudioAttributes.isSystemUsage(usage)) {
            return isSupportedSystemUsage(usage) && ((usage == 17 && (audioAttributes.getAllFlags() & 65536) != 0 && callerHasPermission("android.permission.CALL_AUDIO_INTERCEPTION")) || callerHasPermission("android.permission.MODIFY_AUDIO_ROUTING"));
        }
        return true;
    }

    public int requestAudioFocus(AudioAttributes aa, int durationHint, IBinder cb, IAudioFocusDispatcher fd, String clientId, String callingPackageName, String attributionTag, int flags, IAudioPolicyCallback pcb, int sdk) {
        MediaMetrics.Item mmi;
        if ((flags & 8) != 0) {
            throw new IllegalArgumentException("Invalid test flag");
        }
        int uid = Binder.getCallingUid();
        MediaMetrics.Item mmi2 = new MediaMetrics.Item("audio.service.focus").setUid(uid).set(MediaMetrics.Property.CALLING_PACKAGE, callingPackageName).set(MediaMetrics.Property.CLIENT_NAME, clientId).set(MediaMetrics.Property.EVENT, "requestAudioFocus").set(MediaMetrics.Property.FLAGS, Integer.valueOf(flags));
        if (aa != null && !isValidAudioAttributesUsage(aa)) {
            Log.w(TAG, "Request using unsupported usage");
            mmi2.set(MediaMetrics.Property.EARLY_RETURN, "Request using unsupported usage").record();
            return 0;
        }
        if ((flags & 4) == 4) {
            if ("AudioFocus_For_Phone_Ring_And_Calls".equals(clientId)) {
                if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
                    Log.e(TAG, "Invalid permission to (un)lock audio focus", new Exception());
                    mmi2.set(MediaMetrics.Property.EARLY_RETURN, "Invalid permission to (un)lock audio focus").record();
                    return 0;
                }
            } else {
                synchronized (this.mAudioPolicies) {
                    if (!this.mAudioPolicies.containsKey(pcb.asBinder())) {
                        Log.e(TAG, "Invalid unregistered AudioPolicy to (un)lock audio focus");
                        mmi2.set(MediaMetrics.Property.EARLY_RETURN, "Invalid unregistered AudioPolicy to (un)lock audio focus").record();
                        return 0;
                    }
                }
            }
        }
        if (callingPackageName == null || clientId == null) {
            mmi = mmi2;
        } else if (aa != null) {
            mmi2.record();
            if (TRAN_DTS_SUPPORT && !callingPackageName.equals(this.mPreviouPackageName) && durationHint != 4 && durationHint != 2) {
                this.mPreviouPackageName = callingPackageName;
                sendMsg(this.mAudioHandler, 105, 2, 0, 0, callingPackageName, 0);
                Log.d("DTS_AUDIO", "requestAudioFocus sendMsg callingPackageName = " + callingPackageName);
            }
            return this.mMediaFocusControl.requestAudioFocus(aa, durationHint, cb, fd, clientId, callingPackageName, attributionTag, flags, sdk, forceFocusDuckingForAccessibility(aa, durationHint, uid), -1);
        } else {
            mmi = mmi2;
        }
        Log.e(TAG, "Invalid null parameter to request audio focus");
        mmi.set(MediaMetrics.Property.EARLY_RETURN, "Invalid null parameter to request audio focus").record();
        return 0;
    }

    public int requestAudioFocusForTest(AudioAttributes aa, int durationHint, IBinder cb, IAudioFocusDispatcher fd, String clientId, String callingPackageName, int flags, int fakeUid, int sdk) {
        if (enforceQueryAudioStateForTest("focus request")) {
            if (callingPackageName == null || clientId == null || aa == null) {
                Log.e(TAG, "Invalid null parameter to request audio focus");
                return 0;
            }
            return this.mMediaFocusControl.requestAudioFocus(aa, durationHint, cb, fd, clientId, callingPackageName, null, flags, sdk, false, fakeUid);
        }
        return 0;
    }

    public int abandonAudioFocus(IAudioFocusDispatcher fd, String clientId, AudioAttributes aa, String callingPackageName) {
        MediaMetrics.Item mmi = new MediaMetrics.Item("audio.service.focus").set(MediaMetrics.Property.CALLING_PACKAGE, callingPackageName).set(MediaMetrics.Property.CLIENT_NAME, clientId).set(MediaMetrics.Property.EVENT, "abandonAudioFocus");
        if (aa != null && !isValidAudioAttributesUsage(aa)) {
            Log.w(TAG, "Request using unsupported usage.");
            mmi.set(MediaMetrics.Property.EARLY_RETURN, "unsupported usage").record();
            return 0;
        }
        mmi.record();
        return this.mMediaFocusControl.abandonAudioFocus(fd, clientId, aa, callingPackageName);
    }

    public int abandonAudioFocusForTest(IAudioFocusDispatcher fd, String clientId, AudioAttributes aa, String callingPackageName) {
        if (!enforceQueryAudioStateForTest("focus abandon")) {
            return 0;
        }
        return this.mMediaFocusControl.abandonAudioFocus(fd, clientId, aa, callingPackageName);
    }

    public void unregisterAudioFocusClient(String clientId) {
        new MediaMetrics.Item("audio.service.focus").set(MediaMetrics.Property.CLIENT_NAME, clientId).set(MediaMetrics.Property.EVENT, "unregisterAudioFocusClient").record();
        this.mMediaFocusControl.unregisterAudioFocusClient(clientId);
    }

    public int getCurrentAudioFocus() {
        return this.mMediaFocusControl.getCurrentAudioFocus();
    }

    public int getFocusRampTimeMs(int focusGain, AudioAttributes attr) {
        return MediaFocusControl.getFocusRampTimeMs(focusGain, attr);
    }

    public boolean hasAudioFocusUsers() {
        return this.mMediaFocusControl.hasAudioFocusUsers();
    }

    public long getFadeOutDurationOnFocusLossMillis(AudioAttributes aa) {
        if (!enforceQueryAudioStateForTest("fade out duration")) {
            return 0L;
        }
        return this.mMediaFocusControl.getFadeOutDurationOnFocusLossMillis(aa);
    }

    private boolean enforceQueryAudioStateForTest(String mssg) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.QUERY_AUDIO_STATE") != 0) {
            String reason = "Doesn't have QUERY_AUDIO_STATE permission for " + mssg + " test API";
            Log.e(TAG, reason, new Exception());
            return false;
        }
        return true;
    }

    private void enforceModifyDefaultAudioEffectsPermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_DEFAULT_AUDIO_EFFECTS") != 0) {
            throw new SecurityException("Missing MODIFY_DEFAULT_AUDIO_EFFECTS permission");
        }
    }

    public int getSpatializerImmersiveAudioLevel() {
        return this.mSpatializerHelper.getCapableImmersiveAudioLevel();
    }

    public boolean isSpatializerEnabled() {
        return this.mSpatializerHelper.isEnabled();
    }

    public boolean isSpatializerAvailable() {
        return this.mSpatializerHelper.isAvailable();
    }

    public boolean isSpatializerAvailableForDevice(AudioDeviceAttributes device) {
        enforceModifyDefaultAudioEffectsPermission();
        return this.mSpatializerHelper.isAvailableForDevice((AudioDeviceAttributes) Objects.requireNonNull(device));
    }

    public boolean hasHeadTracker(AudioDeviceAttributes device) {
        enforceModifyDefaultAudioEffectsPermission();
        return this.mSpatializerHelper.hasHeadTracker((AudioDeviceAttributes) Objects.requireNonNull(device));
    }

    public void setHeadTrackerEnabled(boolean enabled, AudioDeviceAttributes device) {
        enforceModifyDefaultAudioEffectsPermission();
        this.mSpatializerHelper.setHeadTrackerEnabled(enabled, (AudioDeviceAttributes) Objects.requireNonNull(device));
    }

    public boolean isHeadTrackerEnabled(AudioDeviceAttributes device) {
        enforceModifyDefaultAudioEffectsPermission();
        return this.mSpatializerHelper.isHeadTrackerEnabled((AudioDeviceAttributes) Objects.requireNonNull(device));
    }

    public boolean isHeadTrackerAvailable() {
        return this.mSpatializerHelper.isHeadTrackerAvailable();
    }

    public void setSpatializerEnabled(boolean enabled) {
        enforceModifyDefaultAudioEffectsPermission();
        this.mSpatializerHelper.setFeatureEnabled(enabled);
    }

    public boolean canBeSpatialized(AudioAttributes attributes, AudioFormat format) {
        Objects.requireNonNull(attributes);
        Objects.requireNonNull(format);
        return this.mSpatializerHelper.canBeSpatialized(attributes, format);
    }

    public void registerSpatializerCallback(ISpatializerCallback cb) {
        Objects.requireNonNull(cb);
        this.mSpatializerHelper.registerStateCallback(cb);
    }

    public void unregisterSpatializerCallback(ISpatializerCallback cb) {
        Objects.requireNonNull(cb);
        this.mSpatializerHelper.unregisterStateCallback(cb);
    }

    public void registerSpatializerHeadTrackingCallback(ISpatializerHeadTrackingModeCallback cb) {
        enforceModifyDefaultAudioEffectsPermission();
        Objects.requireNonNull(cb);
        this.mSpatializerHelper.registerHeadTrackingModeCallback(cb);
    }

    public void unregisterSpatializerHeadTrackingCallback(ISpatializerHeadTrackingModeCallback cb) {
        enforceModifyDefaultAudioEffectsPermission();
        Objects.requireNonNull(cb);
        this.mSpatializerHelper.unregisterHeadTrackingModeCallback(cb);
    }

    public void registerSpatializerHeadTrackerAvailableCallback(ISpatializerHeadTrackerAvailableCallback cb, boolean register) {
        Objects.requireNonNull(cb);
        this.mSpatializerHelper.registerHeadTrackerAvailableCallback(cb, register);
    }

    public void registerHeadToSoundstagePoseCallback(ISpatializerHeadToSoundStagePoseCallback cb) {
        enforceModifyDefaultAudioEffectsPermission();
        Objects.requireNonNull(cb);
        this.mSpatializerHelper.registerHeadToSoundstagePoseCallback(cb);
    }

    public void unregisterHeadToSoundstagePoseCallback(ISpatializerHeadToSoundStagePoseCallback cb) {
        enforceModifyDefaultAudioEffectsPermission();
        Objects.requireNonNull(cb);
        this.mSpatializerHelper.unregisterHeadToSoundstagePoseCallback(cb);
    }

    public List<AudioDeviceAttributes> getSpatializerCompatibleAudioDevices() {
        enforceModifyDefaultAudioEffectsPermission();
        return this.mSpatializerHelper.getCompatibleAudioDevices();
    }

    public void addSpatializerCompatibleAudioDevice(AudioDeviceAttributes ada) {
        enforceModifyDefaultAudioEffectsPermission();
        Objects.requireNonNull(ada);
        this.mSpatializerHelper.addCompatibleAudioDevice(ada);
    }

    public void removeSpatializerCompatibleAudioDevice(AudioDeviceAttributes ada) {
        enforceModifyDefaultAudioEffectsPermission();
        Objects.requireNonNull(ada);
        this.mSpatializerHelper.removeCompatibleAudioDevice(ada);
    }

    public int[] getSupportedHeadTrackingModes() {
        enforceModifyDefaultAudioEffectsPermission();
        return this.mSpatializerHelper.getSupportedHeadTrackingModes();
    }

    public int getActualHeadTrackingMode() {
        enforceModifyDefaultAudioEffectsPermission();
        return this.mSpatializerHelper.getActualHeadTrackingMode();
    }

    public int getDesiredHeadTrackingMode() {
        enforceModifyDefaultAudioEffectsPermission();
        return this.mSpatializerHelper.getDesiredHeadTrackingMode();
    }

    public void setSpatializerGlobalTransform(float[] transform) {
        enforceModifyDefaultAudioEffectsPermission();
        Objects.requireNonNull(transform);
        this.mSpatializerHelper.setGlobalTransform(transform);
    }

    public void recenterHeadTracker() {
        enforceModifyDefaultAudioEffectsPermission();
        this.mSpatializerHelper.recenterHeadTracker();
    }

    public void setDesiredHeadTrackingMode(int mode) {
        enforceModifyDefaultAudioEffectsPermission();
        switch (mode) {
            case -1:
            case 1:
            case 2:
                this.mSpatializerHelper.setDesiredHeadTrackingMode(mode);
                return;
            case 0:
            default:
                return;
        }
    }

    public void setSpatializerParameter(int key, byte[] value) {
        enforceModifyDefaultAudioEffectsPermission();
        Objects.requireNonNull(value);
        this.mSpatializerHelper.setEffectParameter(key, value);
    }

    public void getSpatializerParameter(int key, byte[] value) {
        enforceModifyDefaultAudioEffectsPermission();
        Objects.requireNonNull(value);
        this.mSpatializerHelper.getEffectParameter(key, value);
    }

    public int getSpatializerOutput() {
        enforceModifyDefaultAudioEffectsPermission();
        return this.mSpatializerHelper.getOutput();
    }

    public void registerSpatializerOutputCallback(ISpatializerOutputCallback cb) {
        enforceModifyDefaultAudioEffectsPermission();
        Objects.requireNonNull(cb);
        this.mSpatializerHelper.registerSpatializerOutputCallback(cb);
    }

    public void unregisterSpatializerOutputCallback(ISpatializerOutputCallback cb) {
        enforceModifyDefaultAudioEffectsPermission();
        Objects.requireNonNull(cb);
        this.mSpatializerHelper.unregisterSpatializerOutputCallback(cb);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postInitSpatializerHeadTrackingSensors() {
        sendMsg(this.mAudioHandler, 42, 0, 0, 0, TAG, 0);
    }

    void onInitSpatializer() {
        String settings = this.mSettings.getSecureStringForUser(this.mContentResolver, "spatial_audio_enabled", -2);
        if (settings == null) {
            Log.e(TAG, "error reading spatial audio device settings");
        } else {
            Log.v(TAG, "restoring spatial audio device settings: " + settings);
            this.mSpatializerHelper.setSADeviceSettings(settings);
        }
        this.mSpatializerHelper.init(this.mHasSpatializerEffect);
        this.mSpatializerHelper.setFeatureEnabled(this.mHasSpatializerEffect);
    }

    public void persistSpatialAudioDeviceSettings() {
        sendMsg(this.mAudioHandler, 43, 0, 0, 0, TAG, 1000);
    }

    void onPersistSpatialAudioDeviceSettings() {
        String settings = this.mSpatializerHelper.getSADeviceSettings();
        Log.v(TAG, "saving spatial audio device settings: " + settings);
        boolean res = this.mSettings.putSecureStringForUser(this.mContentResolver, "spatial_audio_enabled", settings, -2);
        if (!res) {
            Log.e(TAG, "error saving spatial audio device settings: " + settings);
        }
    }

    private boolean readCameraSoundForced() {
        return SystemProperties.getBoolean("audio.camerasound.force", false) || this.mContext.getResources().getBoolean(17891400);
    }

    public void muteAwaitConnection(final int[] usages, final AudioDeviceAttributes device, long timeOutMs) {
        Objects.requireNonNull(usages);
        Objects.requireNonNull(device);
        enforceModifyAudioRoutingPermission();
        if (timeOutMs <= 0 || usages.length == 0) {
            throw new IllegalArgumentException("Invalid timeOutMs/usagesToMute");
        }
        Log.i(TAG, "muteAwaitConnection dev:" + device + " timeOutMs:" + timeOutMs + " usages:" + Arrays.toString(usages));
        if (this.mDeviceBroker.isDeviceConnected(device)) {
            Log.i(TAG, "muteAwaitConnection ignored, device (" + device + ") already connected");
            return;
        }
        synchronized (this.mMuteAwaitConnectionLock) {
            if (this.mMutingExpectedDevice != null) {
                Log.e(TAG, "muteAwaitConnection ignored, another in progress for device:" + this.mMutingExpectedDevice);
                throw new IllegalStateException("muteAwaitConnection already in progress");
            }
            this.mMutingExpectedDevice = device;
            this.mMutedUsagesAwaitingConnection = usages;
            this.mPlaybackMonitor.muteAwaitConnection(usages, device, timeOutMs);
        }
        dispatchMuteAwaitConnection(new Consumer() { // from class: com.android.server.audio.AudioService$$ExternalSyntheticLambda6
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((IMuteAwaitConnectionCallback) obj).dispatchOnMutedUntilConnection(device, usages);
            }
        });
    }

    public AudioDeviceAttributes getMutingExpectedDevice() {
        AudioDeviceAttributes audioDeviceAttributes;
        enforceModifyAudioRoutingPermission();
        synchronized (this.mMuteAwaitConnectionLock) {
            audioDeviceAttributes = this.mMutingExpectedDevice;
        }
        return audioDeviceAttributes;
    }

    public void cancelMuteAwaitConnection(final AudioDeviceAttributes device) {
        Objects.requireNonNull(device);
        enforceModifyAudioRoutingPermission();
        Log.i(TAG, "cancelMuteAwaitConnection for device:" + device);
        synchronized (this.mMuteAwaitConnectionLock) {
            AudioDeviceAttributes audioDeviceAttributes = this.mMutingExpectedDevice;
            if (audioDeviceAttributes == null) {
                Log.i(TAG, "cancelMuteAwaitConnection ignored, no expected device");
            } else if (!device.equalTypeAddress(audioDeviceAttributes)) {
                Log.e(TAG, "cancelMuteAwaitConnection ignored, got " + device + "] but expected device is" + this.mMutingExpectedDevice);
                throw new IllegalStateException("cancelMuteAwaitConnection for wrong device");
            } else {
                final int[] mutedUsages = this.mMutedUsagesAwaitingConnection;
                this.mMutingExpectedDevice = null;
                this.mMutedUsagesAwaitingConnection = null;
                this.mPlaybackMonitor.cancelMuteAwaitConnection("cancelMuteAwaitConnection dev:" + device);
                dispatchMuteAwaitConnection(new Consumer() { // from class: com.android.server.audio.AudioService$$ExternalSyntheticLambda11
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((IMuteAwaitConnectionCallback) obj).dispatchOnUnmutedEvent(3, device, mutedUsages);
                    }
                });
            }
        }
    }

    public void registerMuteAwaitConnectionDispatcher(IMuteAwaitConnectionCallback cb, boolean register) {
        enforceModifyAudioRoutingPermission();
        if (register) {
            this.mMuteAwaitConnectionDispatchers.register(cb);
        } else {
            this.mMuteAwaitConnectionDispatchers.unregister(cb);
        }
    }

    void checkMuteAwaitConnection() {
        synchronized (this.mMuteAwaitConnectionLock) {
            final AudioDeviceAttributes device = this.mMutingExpectedDevice;
            if (device == null) {
                return;
            }
            final int[] mutedUsages = this.mMutedUsagesAwaitingConnection;
            if (this.mDeviceBroker.isDeviceConnected(device)) {
                this.mMutingExpectedDevice = null;
                this.mMutedUsagesAwaitingConnection = null;
                this.mPlaybackMonitor.cancelMuteAwaitConnection("checkMuteAwaitConnection device " + device + " connected, unmuting");
                dispatchMuteAwaitConnection(new Consumer() { // from class: com.android.server.audio.AudioService$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((IMuteAwaitConnectionCallback) obj).dispatchOnUnmutedEvent(1, device, mutedUsages);
                    }
                });
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: onMuteAwaitConnectionTimeout */
    public void m1910lambda$new$0$comandroidserveraudioAudioService(final AudioDeviceAttributes timedOutDevice) {
        synchronized (this.mMuteAwaitConnectionLock) {
            if (timedOutDevice.equals(this.mMutingExpectedDevice)) {
                Log.i(TAG, "muteAwaitConnection timeout, clearing expected device " + this.mMutingExpectedDevice);
                final int[] mutedUsages = this.mMutedUsagesAwaitingConnection;
                this.mMutingExpectedDevice = null;
                this.mMutedUsagesAwaitingConnection = null;
                dispatchMuteAwaitConnection(new Consumer() { // from class: com.android.server.audio.AudioService$$ExternalSyntheticLambda2
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((IMuteAwaitConnectionCallback) obj).dispatchOnUnmutedEvent(2, timedOutDevice, mutedUsages);
                    }
                });
            }
        }
    }

    private void dispatchMuteAwaitConnection(Consumer<IMuteAwaitConnectionCallback> callback) {
        int nbDispatchers = this.mMuteAwaitConnectionDispatchers.beginBroadcast();
        ArrayList<IMuteAwaitConnectionCallback> errorList = null;
        for (int i = 0; i < nbDispatchers; i++) {
            try {
                callback.accept(this.mMuteAwaitConnectionDispatchers.getBroadcastItem(i));
            } catch (Exception e) {
                if (errorList == null) {
                    errorList = new ArrayList<>(1);
                }
                errorList.add(this.mMuteAwaitConnectionDispatchers.getBroadcastItem(i));
            }
        }
        if (errorList != null) {
            Iterator<IMuteAwaitConnectionCallback> it = errorList.iterator();
            while (it.hasNext()) {
                IMuteAwaitConnectionCallback errorItem = it.next();
                this.mMuteAwaitConnectionDispatchers.unregister(errorItem);
            }
        }
        this.mMuteAwaitConnectionDispatchers.finishBroadcast();
    }

    public void registerDeviceVolumeBehaviorDispatcher(boolean register, IDeviceVolumeBehaviorDispatcher dispatcher) {
        enforceQueryStateOrModifyRoutingPermission();
        Objects.requireNonNull(dispatcher);
        if (register) {
            this.mDeviceVolumeBehaviorDispatchers.register(dispatcher);
        } else {
            this.mDeviceVolumeBehaviorDispatchers.unregister(dispatcher);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchDeviceVolumeBehavior(AudioDeviceAttributes device, int volumeBehavior) {
        int dispatchers = this.mDeviceVolumeBehaviorDispatchers.beginBroadcast();
        for (int i = 0; i < dispatchers; i++) {
            try {
                this.mDeviceVolumeBehaviorDispatchers.getBroadcastItem(i).dispatchDeviceVolumeBehaviorChanged(device, volumeBehavior);
            } catch (RemoteException e) {
            }
        }
        this.mDeviceVolumeBehaviorDispatchers.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleConfigurationChanged(Context context) {
        try {
            Configuration config = context.getResources().getConfiguration();
            sendMsg(this.mAudioHandler, 12, 0, 0, 0, TAG, 0);
            boolean cameraSoundForced = readCameraSoundForced();
            synchronized (this.mSettingsLock) {
                boolean cameraSoundForcedChanged = cameraSoundForced != this.mCameraSoundForced;
                this.mCameraSoundForced = cameraSoundForced;
                if (cameraSoundForcedChanged) {
                    if (!this.mIsSingleVolume) {
                        synchronized (VolumeStreamState.class) {
                            VolumeStreamState[] volumeStreamStateArr = this.mStreamStates;
                            VolumeStreamState s = volumeStreamStateArr[7];
                            if (cameraSoundForced) {
                                s.setAllIndexesToMax();
                                this.mRingerModeAffectedStreams &= -129;
                            } else {
                                s.setAllIndexes(volumeStreamStateArr[1], TAG);
                                this.mRingerModeAffectedStreams |= 128;
                            }
                        }
                        setRingerModeInt(getRingerModeInternal(), false);
                    }
                    this.mDeviceBroker.setForceUse_Async(4, cameraSoundForced ? 11 : 0, "handleConfigurationChanged");
                    sendMsg(this.mAudioHandler, 10, 2, 0, 0, this.mStreamStates[7], 0);
                }
            }
            this.mVolumeController.setLayoutDirection(config.getLayoutDirection());
        } catch (Exception e) {
            Log.e(TAG, "Error handling configuration change: ", e);
        }
    }

    public void setRingtonePlayer(IRingtonePlayer player) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.REMOTE_AUDIO_PLAYBACK", null);
        this.mRingtonePlayer = player;
    }

    public IRingtonePlayer getRingtonePlayer() {
        return this.mRingtonePlayer;
    }

    public AudioRoutesInfo startWatchingRoutes(IAudioRoutesObserver observer) {
        return this.mDeviceBroker.startWatchingRoutes(observer);
    }

    private int safeMediaVolumeIndex(int device) {
        if (!this.mSafeMediaVolumeDevices.contains(Integer.valueOf(device))) {
            return MAX_STREAM_VOLUME[3];
        }
        if (device == 67108864) {
            return this.mSafeUsbMediaVolumeIndex;
        }
        return this.mSafeMediaVolumeIndex;
    }

    private void setSafeMediaVolumeEnabled(boolean on, String caller) {
        synchronized (this.mSafeMediaVolumeStateLock) {
            Log.d(TAG, " setSafeMediaVolumeEnabled on = " + on + " caller = " + caller + " mSafeMediaVolumeState = " + this.mSafeMediaVolumeState);
            int i = this.mSafeMediaVolumeState;
            if (i != 0 && i != 1) {
                if (on && i == 2) {
                    this.mSafeMediaVolumeState = 3;
                    enforceSafeMediaVolume(caller);
                } else if (!on && i == 3) {
                    this.mSafeMediaVolumeState = 2;
                    this.mMusicActiveMs = 1;
                    saveMusicActiveMs();
                    Log.d(TAG, " setSafeMediaVolumeEnabled start MSG_CHECK_MUSIC_ACTIVE");
                    sendMsg(this.mAudioHandler, 11, 0, 0, 0, caller, 60000);
                }
            }
        }
    }

    private void enforceSafeMediaVolume(String caller) {
        VolumeStreamState streamState = this.mStreamStates[3];
        Set<Integer> devices = this.mSafeMediaVolumeDevices;
        Log.d(TAG, " enforceSafeMediaVolume devices = " + devices);
        for (Integer num : devices) {
            int device = num.intValue();
            int index = streamState.getIndex(device);
            if (index > safeMediaVolumeIndex(device)) {
                streamState.setIndex(safeMediaVolumeIndex(device), device, caller, true);
                sendMsg(this.mAudioHandler, 0, 2, device, 0, streamState, 0);
            }
        }
    }

    private boolean checkSafeMediaVolume(int streamType, int index, int device) {
        synchronized (this.mSafeMediaVolumeStateLock) {
            if (DEBUG_VOL) {
                Log.d(TAG, "checkSafeMediaVolume streamType = " + streamType + " index = " + index + " device = " + device + " mSafeMediaVolumeState = " + this.mSafeMediaVolumeState + " safeMediaVolumeIndex = " + safeMediaVolumeIndex(device) + " mSafeMediaVolumeDevices.contains(device)" + this.mSafeMediaVolumeDevices.contains(Integer.valueOf(device)));
            }
            if (this.mSafeMediaVolumeState == 3 && mStreamVolumeAlias[streamType] == 3 && this.mSafeMediaVolumeDevices.contains(Integer.valueOf(device)) && index > safeMediaVolumeIndex(device)) {
                return false;
            }
            if (this.mSafeMediaVolumeState == 2 && mStreamVolumeAlias[streamType] == 3 && this.mSafeMediaVolumeDevices.contains(Integer.valueOf(device)) && index > safeMediaVolumeIndex(device) && this.mMusicActiveMs != 0) {
                sendMsg(this.mAudioHandler, 11, 0, 0, 0, CALLER_PACKAGE, 60000);
            }
            return true;
        }
    }

    public void disableSafeMediaVolume(String callingPackage) {
        Log.d(TAG, " disableSafeMediaVolume callingPackage = " + callingPackage);
        enforceVolumeController("disable the safe media volume");
        synchronized (this.mSafeMediaVolumeStateLock) {
            setSafeMediaVolumeEnabled(false, callingPackage);
            StreamVolumeCommand streamVolumeCommand = this.mPendingVolumeCommand;
            if (streamVolumeCommand != null) {
                onSetStreamVolume(streamVolumeCommand.mStreamType, this.mPendingVolumeCommand.mIndex, this.mPendingVolumeCommand.mFlags, this.mPendingVolumeCommand.mDevice, callingPackage, true);
                this.mPendingVolumeCommand = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateHdmiCecSinkLocked(boolean hdmiCecSink) {
        if (!hasDeviceVolumeBehavior(1024)) {
            if (hdmiCecSink) {
                if (DEBUG_VOL) {
                    Log.d(TAG, "CEC sink: setting HDMI as full vol device");
                }
                setDeviceVolumeBehaviorInternal(new AudioDeviceAttributes(1024, ""), 1, "AudioService.updateHdmiCecSinkLocked()");
            } else {
                if (DEBUG_VOL) {
                    Log.d(TAG, "TV, no CEC: setting HDMI as regular vol device");
                }
                setDeviceVolumeBehaviorInternal(new AudioDeviceAttributes(1024, ""), 0, "AudioService.updateHdmiCecSinkLocked()");
            }
            postUpdateVolumeStatesForAudioDevice(1024, "HdmiPlaybackClient.DisplayStatusCallback");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHdmiControlStatusChangeListenerCallback implements HdmiControlManager.HdmiControlStatusChangeListener {
        private MyHdmiControlStatusChangeListenerCallback() {
        }

        public void onStatusChange(int isCecEnabled, boolean isCecAvailable) {
            synchronized (AudioService.this.mHdmiClientLock) {
                if (AudioService.this.mHdmiManager == null) {
                    return;
                }
                boolean cecEnabled = true;
                if (isCecEnabled != 1) {
                    cecEnabled = false;
                }
                AudioService.this.updateHdmiCecSinkLocked(cecEnabled ? isCecAvailable : false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHdmiCecVolumeControlFeatureListener implements HdmiControlManager.HdmiCecVolumeControlFeatureListener {
        private MyHdmiCecVolumeControlFeatureListener() {
        }

        public void onHdmiCecVolumeControlFeature(int hdmiCecVolumeControl) {
            synchronized (AudioService.this.mHdmiClientLock) {
                if (AudioService.this.mHdmiManager == null) {
                    return;
                }
                AudioService audioService = AudioService.this;
                boolean z = true;
                if (hdmiCecVolumeControl != 1) {
                    z = false;
                }
                audioService.mHdmiCecVolumeControlEnabled = z;
            }
        }
    }

    public int setHdmiSystemAudioSupported(boolean on) {
        int device = 0;
        synchronized (this.mHdmiClientLock) {
            if (this.mHdmiManager != null) {
                if (this.mHdmiTvClient == null && this.mHdmiAudioSystemClient == null) {
                    Log.w(TAG, "Only Hdmi-Cec enabled TV or audio system device supportssystem audio mode.");
                    return 0;
                }
                if (this.mHdmiSystemAudioSupported != on) {
                    this.mHdmiSystemAudioSupported = on;
                    int config = on ? 12 : 0;
                    this.mDeviceBroker.setForceUse_Async(5, config, "setHdmiSystemAudioSupported");
                }
                device = getDeviceMaskForStream(3);
            }
            return device;
        }
    }

    public boolean isHdmiSystemAudioSupported() {
        return this.mHdmiSystemAudioSupported;
    }

    private void initA11yMonitoring() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        updateDefaultStreamOverrideDelay(accessibilityManager.isTouchExplorationEnabled());
        updateA11yVolumeAlias(accessibilityManager.isAccessibilityVolumeStreamActive());
        accessibilityManager.addTouchExplorationStateChangeListener(this, null);
        accessibilityManager.addAccessibilityServicesStateChangeListener(this);
    }

    @Override // android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
    public void onTouchExplorationStateChanged(boolean enabled) {
        updateDefaultStreamOverrideDelay(enabled);
    }

    private void updateDefaultStreamOverrideDelay(boolean touchExploreEnabled) {
        if (touchExploreEnabled) {
            sStreamOverrideDelayMs = 1000;
        } else {
            sStreamOverrideDelayMs = 0;
        }
        if (DEBUG_VOL) {
            Log.d(TAG, "Touch exploration enabled=" + touchExploreEnabled + " stream override delay is now " + sStreamOverrideDelayMs + " ms");
        }
    }

    public void onAccessibilityServicesStateChanged(AccessibilityManager accessibilityManager) {
        updateA11yVolumeAlias(accessibilityManager.isAccessibilityVolumeStreamActive());
    }

    private void updateA11yVolumeAlias(boolean a11VolEnabled) {
        if (DEBUG_VOL) {
            Log.d(TAG, "Accessibility volume enabled = " + a11VolEnabled);
        }
        if (sIndependentA11yVolume != a11VolEnabled) {
            sIndependentA11yVolume = a11VolEnabled;
            int i = 1;
            updateStreamVolumeAlias(true, TAG);
            VolumeController volumeController = this.mVolumeController;
            if (!sIndependentA11yVolume) {
                i = 0;
            }
            volumeController.setA11yMode(i);
            this.mVolumeController.postVolumeChanged(10, 0);
        }
    }

    public boolean isCameraSoundForced() {
        boolean z;
        synchronized (this.mSettingsLock) {
            z = this.mCameraSoundForced;
        }
        return z;
    }

    private void dumpRingerMode(PrintWriter pw) {
        pw.println("\nRinger mode: ");
        StringBuilder append = new StringBuilder().append("- mode (internal) = ");
        String[] strArr = RINGER_MODE_NAMES;
        pw.println(append.append(strArr[this.mRingerMode]).toString());
        pw.println("- mode (external) = " + strArr[this.mRingerModeExternal]);
        pw.println("- zen mode:" + Settings.Global.zenModeToString(this.mNm.getZenMode()));
        dumpRingerModeStreams(pw, "affected", this.mRingerModeAffectedStreams);
        dumpRingerModeStreams(pw, "muted", this.mRingerAndZenModeMutedStreams);
        pw.print("- delegate = ");
        pw.println(this.mRingerModeDelegate);
    }

    private void dumpRingerModeStreams(PrintWriter pw, String type, int streams) {
        pw.print("- ringer mode ");
        pw.print(type);
        pw.print(" streams = 0x");
        pw.print(Integer.toHexString(streams));
        if (streams != 0) {
            pw.print(" (");
            boolean first = true;
            for (int i = 0; i < AudioSystem.STREAM_NAMES.length; i++) {
                int stream = 1 << i;
                if ((streams & stream) != 0) {
                    if (!first) {
                        pw.print(',');
                    }
                    pw.print(AudioSystem.STREAM_NAMES[i]);
                    streams &= ~stream;
                    first = false;
                }
            }
            if (streams != 0) {
                if (!first) {
                    pw.print(',');
                }
                pw.print(streams);
            }
            pw.print(')');
        }
        pw.println();
    }

    private String dumpDeviceTypes(Set<Integer> deviceTypes) {
        Iterator<Integer> it = deviceTypes.iterator();
        if (!it.hasNext()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("0x" + Integer.toHexString(it.next().intValue()));
        while (it.hasNext()) {
            sb.append(",0x" + Integer.toHexString(it.next().intValue()));
        }
        return sb.toString();
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            sLifecycleLogger.dump(pw);
            if (this.mAudioHandler != null) {
                pw.println("\nMessage handler (watch for unhandled messages):");
                this.mAudioHandler.dump(new PrintWriterPrinter(pw), "  ");
            } else {
                pw.println("\nMessage handler is null");
            }
            this.mMediaFocusControl.dump(pw);
            dumpStreamStates(pw);
            dumpVolumeGroups(pw);
            dumpRingerMode(pw);
            dumpAudioMode(pw);
            pw.println("\nAudio routes:");
            pw.print("  mMainType=0x");
            pw.println(Integer.toHexString(this.mDeviceBroker.getCurAudioRoutes().mainType));
            pw.print("  mBluetoothName=");
            pw.println(this.mDeviceBroker.getCurAudioRoutes().bluetoothName);
            pw.println("\nOther state:");
            pw.print("  mVolumeController=");
            pw.println(this.mVolumeController);
            pw.print("  mSafeMediaVolumeState=");
            pw.println(safeMediaVolumeStateToString(this.mSafeMediaVolumeState));
            pw.print("  mSafeMediaVolumeIndex=");
            pw.println(this.mSafeMediaVolumeIndex);
            pw.print("  mSafeUsbMediaVolumeIndex=");
            pw.println(this.mSafeUsbMediaVolumeIndex);
            pw.print("  mSafeUsbMediaVolumeDbfs=");
            pw.println(this.mSafeUsbMediaVolumeDbfs);
            pw.print("  sIndependentA11yVolume=");
            pw.println(sIndependentA11yVolume);
            pw.print("  mPendingVolumeCommand=");
            pw.println(this.mPendingVolumeCommand);
            pw.print("  mMusicActiveMs=");
            pw.println(this.mMusicActiveMs);
            pw.print("  mMcc=");
            pw.println(this.mMcc);
            pw.print("  mCameraSoundForced=");
            pw.println(isCameraSoundForced());
            pw.print("  mHasVibrator=");
            pw.println(this.mHasVibrator);
            pw.print("  mVolumePolicy=");
            pw.println(this.mVolumePolicy);
            pw.print("  mAvrcpAbsVolSupported=");
            pw.println(this.mAvrcpAbsVolSupported);
            pw.print("  mBtScoOnByApp=");
            pw.println(this.mBtScoOnByApp);
            pw.print("  mIsSingleVolume=");
            pw.println(this.mIsSingleVolume);
            pw.print("  mUseFixedVolume=");
            pw.println(this.mUseFixedVolume);
            pw.print("  mFixedVolumeDevices=");
            pw.println(dumpDeviceTypes(this.mFixedVolumeDevices));
            pw.print("  mFullVolumeDevices=");
            pw.println(dumpDeviceTypes(this.mFullVolumeDevices));
            pw.print("  mAbsoluteVolumeDevices.keySet()=");
            pw.println(dumpDeviceTypes(this.mAbsoluteVolumeDeviceInfoMap.keySet()));
            pw.print("  mExtVolumeController=");
            pw.println(this.mExtVolumeController);
            pw.print("  mHdmiAudioSystemClient=");
            pw.println(this.mHdmiAudioSystemClient);
            pw.print("  mHdmiPlaybackClient=");
            pw.println(this.mHdmiPlaybackClient);
            pw.print("  mHdmiTvClient=");
            pw.println(this.mHdmiTvClient);
            pw.print("  mHdmiSystemAudioSupported=");
            pw.println(this.mHdmiSystemAudioSupported);
            pw.print("  mHdmiCecVolumeControlEnabled=");
            pw.println(this.mHdmiCecVolumeControlEnabled);
            pw.print("  mIsCallScreeningModeSupported=");
            pw.println(this.mIsCallScreeningModeSupported);
            pw.print("  mic mute FromSwitch=" + this.mMicMuteFromSwitch + " FromRestrictions=" + this.mMicMuteFromRestrictions + " FromApi=" + this.mMicMuteFromApi + " from system=" + this.mMicMuteFromSystemCached);
            pw.print("  mCurrentImeUid=");
            pw.println(this.mCurrentImeUid);
            dumpAccessibilityServiceUids(pw);
            dumpAssistantServicesUids(pw);
            dumpAudioPolicies(pw);
            this.mDynPolicyLogger.dump(pw);
            this.mPlaybackMonitor.dump(pw);
            this.mRecordMonitor.dump(pw);
            pw.println("\nAudioDeviceBroker:");
            this.mDeviceBroker.dump(pw, "  ");
            pw.println("\nSoundEffects:");
            this.mSfxHelper.dump(pw, "  ");
            pw.println("\n");
            pw.println("\nEvent logs:");
            this.mModeLogger.dump(pw);
            pw.println("\n");
            sDeviceLogger.dump(pw);
            pw.println("\n");
            sForceUseLogger.dump(pw);
            pw.println("\n");
            sVolumeLogger.dump(pw);
            pw.println("\n");
            dumpSupportedSystemUsage(pw);
            pw.println("\n");
            pw.println("\nSpatial audio:");
            pw.println("mHasSpatializerEffect:" + this.mHasSpatializerEffect + " (effect present)");
            pw.println("isSpatializerEnabled:" + isSpatializerEnabled() + " (routing dependent)");
            this.mSpatializerHelper.dump(pw);
            sSpatialLogger.dump(pw);
            this.mAudioSystem.dump(pw);
        }
    }

    private void dumpSupportedSystemUsage(PrintWriter pw) {
        pw.println("Supported System Usages:");
        synchronized (this.mSupportedSystemUsagesLock) {
            int i = 0;
            while (true) {
                int[] iArr = this.mSupportedSystemUsages;
                if (i < iArr.length) {
                    pw.printf("\t%s\n", AudioAttributes.usageToString(iArr[i]));
                    i++;
                }
            }
        }
    }

    private void dumpAssistantServicesUids(PrintWriter pw) {
        synchronized (this.mSettingsLock) {
            if (this.mAssistantUids.size() > 0) {
                pw.println("  Assistant service UIDs:");
                Iterator<Integer> it = this.mAssistantUids.iterator();
                while (it.hasNext()) {
                    int uid = it.next().intValue();
                    pw.println("  - " + uid);
                }
            } else {
                pw.println("  No Assistant service Uids.");
            }
        }
    }

    private void dumpAccessibilityServiceUids(PrintWriter pw) {
        int[] iArr;
        synchronized (this.mSupportedSystemUsagesLock) {
            int[] iArr2 = this.mAccessibilityServiceUids;
            if (iArr2 != null && iArr2.length > 0) {
                pw.println("  Accessibility service Uids:");
                for (int uid : this.mAccessibilityServiceUids) {
                    pw.println("  - " + uid);
                }
            } else {
                pw.println("  No accessibility service Uids.");
            }
        }
    }

    private static String safeMediaVolumeStateToString(int state) {
        switch (state) {
            case 0:
                return "SAFE_MEDIA_VOLUME_NOT_CONFIGURED";
            case 1:
                return "SAFE_MEDIA_VOLUME_DISABLED";
            case 2:
                return "SAFE_MEDIA_VOLUME_INACTIVE";
            case 3:
                return "SAFE_MEDIA_VOLUME_ACTIVE";
            default:
                return null;
        }
    }

    private static void readAndSetLowRamDevice() {
        boolean isLowRamDevice = ActivityManager.isLowRamDeviceStatic();
        long totalMemory = 1073741824;
        try {
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            ActivityManager.getService().getMemoryInfo(info);
            totalMemory = info.totalMem;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot obtain MemoryInfo from ActivityManager, assume low memory device");
            isLowRamDevice = true;
        }
        int status = AudioSystem.setLowRamDevice(isLowRamDevice, totalMemory);
        if (status != 0) {
            Log.w(TAG, "AudioFlinger informed of device's low RAM attribute; status " + status);
        }
    }

    private void enforceVolumeController(String action) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "Only SystemUI can " + action);
    }

    public void setVolumeController(final IVolumeController controller) {
        enforceVolumeController("set the volume controller");
        if (this.mVolumeController.isSameBinder(controller)) {
            return;
        }
        this.mVolumeController.postDismiss();
        if (controller != null) {
            try {
                controller.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.audio.AudioService.5
                    @Override // android.os.IBinder.DeathRecipient
                    public void binderDied() {
                        if (AudioService.this.mVolumeController.isSameBinder(controller)) {
                            Log.w(AudioService.TAG, "Current remote volume controller died, unregistering");
                            AudioService.this.setVolumeController(null);
                        }
                    }
                }, 0);
            } catch (RemoteException e) {
            }
        }
        this.mVolumeController.setController(controller);
        if (DEBUG_VOL) {
            Log.d(TAG, "Volume controller: " + this.mVolumeController);
        }
    }

    public void notifyVolumeControllerVisible(IVolumeController controller, boolean visible) {
        enforceVolumeController("notify about volume controller visibility");
        if (!this.mVolumeController.isSameBinder(controller)) {
            return;
        }
        this.mVolumeController.setVisible(visible);
        if (DEBUG_VOL) {
            Log.d(TAG, "Volume controller visible: " + visible);
        }
    }

    public void setVolumePolicy(VolumePolicy policy) {
        enforceVolumeController("set volume policy");
        if (policy != null && !policy.equals(this.mVolumePolicy)) {
            this.mVolumePolicy = policy;
            if (DEBUG_VOL) {
                Log.d(TAG, "Volume policy changed: " + this.mVolumePolicy);
            }
        }
    }

    /* loaded from: classes.dex */
    public class VolumeController {
        private static final String TAG = "VolumeController";
        private IVolumeController mController;
        private int mLongPressTimeout;
        private long mNextLongPress;
        private boolean mVisible;

        public VolumeController() {
        }

        public void setController(IVolumeController controller) {
            this.mController = controller;
            this.mVisible = false;
        }

        public void loadSettings(ContentResolver cr) {
            this.mLongPressTimeout = AudioService.this.mSettings.getSecureIntForUser(cr, "long_press_timeout", 500, -2);
        }

        public boolean suppressAdjustment(int resolvedStream, int flags, boolean isMute) {
            if (isMute || resolvedStream != 3 || this.mController == null) {
                return false;
            }
            if (resolvedStream == 3 && AudioService.this.mAudioSystem.isStreamActive(3, this.mLongPressTimeout)) {
                return false;
            }
            long now = SystemClock.uptimeMillis();
            if ((flags & 1) != 0 && !this.mVisible) {
                if (this.mNextLongPress < now) {
                    this.mNextLongPress = this.mLongPressTimeout + now;
                }
                return true;
            }
            long j = this.mNextLongPress;
            if (j <= 0) {
                return false;
            }
            if (now > j) {
                this.mNextLongPress = 0L;
                return false;
            }
            return true;
        }

        public void setVisible(boolean visible) {
            this.mVisible = visible;
        }

        public boolean isSameBinder(IVolumeController controller) {
            return Objects.equals(asBinder(), binder(controller));
        }

        public IBinder asBinder() {
            return binder(this.mController);
        }

        private IBinder binder(IVolumeController controller) {
            if (controller == null) {
                return null;
            }
            return controller.asBinder();
        }

        public String toString() {
            return "VolumeController(" + asBinder() + ",mVisible=" + this.mVisible + ")";
        }

        public void postDisplaySafeVolumeWarning(int flags) {
            IVolumeController iVolumeController = this.mController;
            if (iVolumeController == null) {
                return;
            }
            try {
                iVolumeController.displaySafeVolumeWarning(flags | 1);
            } catch (RemoteException e) {
                Log.w(TAG, "Error calling displaySafeVolumeWarning", e);
            }
        }

        public void postVolumeChanged(int streamType, int flags) {
            IVolumeController iVolumeController = this.mController;
            if (iVolumeController == null) {
                return;
            }
            try {
                iVolumeController.volumeChanged(streamType, flags);
            } catch (RemoteException e) {
                Log.w(TAG, "Error calling volumeChanged", e);
            }
        }

        public void postMasterMuteChanged(int flags) {
            IVolumeController iVolumeController = this.mController;
            if (iVolumeController == null) {
                return;
            }
            try {
                iVolumeController.masterMuteChanged(flags);
            } catch (RemoteException e) {
                Log.w(TAG, "Error calling masterMuteChanged", e);
            }
        }

        public void setLayoutDirection(int layoutDirection) {
            IVolumeController iVolumeController = this.mController;
            if (iVolumeController == null) {
                return;
            }
            try {
                iVolumeController.setLayoutDirection(layoutDirection);
            } catch (RemoteException e) {
                Log.w(TAG, "Error calling setLayoutDirection", e);
            }
        }

        public void postDismiss() {
            IVolumeController iVolumeController = this.mController;
            if (iVolumeController == null) {
                return;
            }
            try {
                iVolumeController.dismiss();
            } catch (RemoteException e) {
                Log.w(TAG, "Error calling dismiss", e);
            }
        }

        public void setA11yMode(int a11yMode) {
            IVolumeController iVolumeController = this.mController;
            if (iVolumeController == null) {
                return;
            }
            try {
                iVolumeController.setA11yMode(a11yMode);
            } catch (RemoteException e) {
                Log.w(TAG, "Error calling setA11Mode", e);
            }
        }
    }

    /* loaded from: classes.dex */
    final class AudioServiceInternal extends AudioManagerInternal {
        AudioServiceInternal() {
        }

        public void setRingerModeDelegate(AudioManagerInternal.RingerModeDelegate delegate) {
            AudioService.this.mRingerModeDelegate = delegate;
            if (AudioService.this.mRingerModeDelegate != null) {
                synchronized (AudioService.this.mSettingsLock) {
                    AudioService.this.updateRingerAndZenModeAffectedStreams();
                }
                setRingerModeInternal(getRingerModeInternal(), "AS.AudioService.setRingerModeDelegate");
            }
        }

        public int getRingerModeInternal() {
            return AudioService.this.getRingerModeInternal();
        }

        public void setRingerModeInternal(int ringerMode, String caller) {
            AudioService.this.setRingerModeInternal(ringerMode, caller);
        }

        public void silenceRingerModeInternal(String caller) {
            AudioService.this.silenceRingerModeInternal(caller);
        }

        public void updateRingerModeAffectedStreamsInternal() {
            synchronized (AudioService.this.mSettingsLock) {
                if (AudioService.this.updateRingerAndZenModeAffectedStreams()) {
                    AudioService.this.setRingerModeInt(getRingerModeInternal(), false);
                }
            }
        }

        public void addAssistantServiceUid(int uid) {
            AudioService.sendMsg(AudioService.this.mAudioHandler, 44, 2, uid, 0, null, 0);
        }

        public void removeAssistantServiceUid(int uid) {
            AudioService.sendMsg(AudioService.this.mAudioHandler, 45, 2, uid, 0, null, 0);
        }

        /* JADX WARN: Removed duplicated region for block: B:15:0x0032  */
        /* JADX WARN: Removed duplicated region for block: B:23:0x0051 A[Catch: all -> 0x006c, TryCatch #0 {, blocks: (B:4:0x0007, B:6:0x000d, B:24:0x005a, B:7:0x0017, B:9:0x001f, B:16:0x0033, B:18:0x003c, B:21:0x004c, B:23:0x0051), top: B:30:0x0007 }] */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void setActiveAssistantServicesUids(IntArray activeUids) {
            boolean changed;
            synchronized (AudioService.this.mSettingsLock) {
                if (activeUids.size() == 0) {
                    AudioService.this.mActiveAssistantServiceUids = AudioService.NO_ACTIVE_ASSISTANT_SERVICE_UIDS;
                } else {
                    if (AudioService.this.mActiveAssistantServiceUids != null && AudioService.this.mActiveAssistantServiceUids.length == activeUids.size()) {
                        changed = false;
                        if (!changed) {
                            int i = 0;
                            while (true) {
                                if (i >= AudioService.this.mActiveAssistantServiceUids.length) {
                                    break;
                                } else if (activeUids.get(i) == AudioService.this.mActiveAssistantServiceUids[i]) {
                                    i++;
                                } else {
                                    changed = true;
                                    break;
                                }
                            }
                        }
                        if (changed) {
                            AudioService.this.mActiveAssistantServiceUids = activeUids.toArray();
                        }
                    }
                    changed = true;
                    if (!changed) {
                    }
                    if (changed) {
                    }
                }
            }
            AudioService.sendMsg(AudioService.this.mAudioHandler, 46, 0, 0, 0, null, 0);
        }

        /* JADX WARN: Removed duplicated region for block: B:15:0x002f  */
        /* JADX WARN: Removed duplicated region for block: B:23:0x004e A[Catch: all -> 0x0069, TryCatch #0 {, blocks: (B:4:0x0007, B:6:0x000d, B:24:0x0057, B:25:0x0067, B:7:0x0014, B:9:0x001c, B:16:0x0030, B:18:0x0039, B:21:0x0049, B:23:0x004e), top: B:30:0x0007 }] */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void setAccessibilityServiceUids(IntArray uids) {
            boolean changed;
            synchronized (AudioService.this.mAccessibilityServiceUidsLock) {
                if (uids.size() == 0) {
                    AudioService.this.mAccessibilityServiceUids = null;
                } else {
                    if (AudioService.this.mAccessibilityServiceUids != null && AudioService.this.mAccessibilityServiceUids.length == uids.size()) {
                        changed = false;
                        if (!changed) {
                            int i = 0;
                            while (true) {
                                if (i >= AudioService.this.mAccessibilityServiceUids.length) {
                                    break;
                                } else if (uids.get(i) == AudioService.this.mAccessibilityServiceUids[i]) {
                                    i++;
                                } else {
                                    changed = true;
                                    break;
                                }
                            }
                        }
                        if (changed) {
                            AudioService.this.mAccessibilityServiceUids = uids.toArray();
                        }
                    }
                    changed = true;
                    if (!changed) {
                    }
                    if (changed) {
                    }
                }
                AudioService.sendMsg(AudioService.this.mAudioHandler, 35, 0, 0, 0, null, 0);
            }
        }

        public void setInputMethodServiceUid(int uid) {
            synchronized (AudioService.this.mInputMethodServiceUidLock) {
                if (AudioService.this.mInputMethodServiceUid != uid) {
                    AudioService.this.mAudioSystem.setCurrentImeUid(uid);
                    AudioService.this.mInputMethodServiceUid = uid;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUpdateAccessibilityServiceUids() {
        int[] accessibilityServiceUids;
        synchronized (this.mAccessibilityServiceUidsLock) {
            accessibilityServiceUids = this.mAccessibilityServiceUids;
        }
        AudioSystem.setA11yServicesUids(accessibilityServiceUids);
    }

    public String registerAudioPolicy(AudioPolicyConfig policyConfig, IAudioPolicyCallback pcb, boolean hasFocusListener, boolean isFocusPolicy, boolean isTestFocusPolicy, boolean isVolumeController, IMediaProjection projection) {
        HashMap<IBinder, AudioPolicyProxy> hashMap;
        AudioSystem.setDynamicPolicyCallback(this.mDynPolicyCallback);
        if (!isPolicyRegisterAllowed(policyConfig, isFocusPolicy || isTestFocusPolicy || hasFocusListener, isVolumeController, projection)) {
            Slog.w(TAG, "Permission denied to register audio policy for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid() + ", need system permission or a MediaProjection that can project audio");
            return null;
        }
        HashMap<IBinder, AudioPolicyProxy> hashMap2 = this.mAudioPolicies;
        synchronized (hashMap2) {
            try {
                try {
                    if (this.mAudioPolicies.containsKey(pcb.asBinder())) {
                        Slog.e(TAG, "Cannot re-register policy");
                        return null;
                    }
                    try {
                        hashMap = hashMap2;
                    } catch (RemoteException e) {
                        e = e;
                        hashMap = hashMap2;
                    } catch (IllegalStateException e2) {
                        e = e2;
                        hashMap = hashMap2;
                    }
                    try {
                        AudioPolicyProxy app = new AudioPolicyProxy(policyConfig, pcb, hasFocusListener, isFocusPolicy, isTestFocusPolicy, isVolumeController, projection);
                        pcb.asBinder().linkToDeath(app, 0);
                        this.mDynPolicyLogger.log(new AudioEventLogger.StringEvent("registerAudioPolicy for " + pcb.asBinder() + " u/pid:" + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid() + " with config:" + app.toCompactLogString()).printLog(TAG));
                        String regId = app.getRegistrationId();
                        this.mAudioPolicies.put(pcb.asBinder(), app);
                        return regId;
                    } catch (RemoteException e3) {
                        e = e3;
                        Slog.w(TAG, "Audio policy registration failed, could not link to " + pcb + " binder death", e);
                        return null;
                    } catch (IllegalStateException e4) {
                        e = e4;
                        Slog.w(TAG, "Audio policy registration failed for binder " + pcb, e);
                        return null;
                    }
                } catch (Throwable th) {
                    e = th;
                    throw e;
                }
            } catch (Throwable th2) {
                e = th2;
            }
        }
    }

    private boolean isPolicyRegisterAllowed(AudioPolicyConfig policyConfig, boolean hasFocusAccess, boolean isVolumeController, IMediaProjection projection) {
        boolean requireValidProjection = false;
        boolean requireCaptureAudioOrMediaOutputPerm = false;
        boolean requireModifyRouting = false;
        boolean requireCallAudioInterception = false;
        ArrayList<AudioMix> voiceCommunicationCaptureMixes = null;
        if (hasFocusAccess || isVolumeController) {
            requireModifyRouting = false | true;
        } else if (policyConfig.getMixes().isEmpty()) {
            requireModifyRouting = false | true;
        }
        Iterator it = policyConfig.getMixes().iterator();
        while (it.hasNext()) {
            AudioMix mix = (AudioMix) it.next();
            if (mix.getRule().allowPrivilegedMediaPlaybackCapture()) {
                String privilegedMediaCaptureError = AudioMix.canBeUsedForPrivilegedMediaCapture(mix.getFormat());
                if (privilegedMediaCaptureError != null) {
                    Log.e(TAG, privilegedMediaCaptureError);
                    return false;
                }
                requireCaptureAudioOrMediaOutputPerm |= true;
            }
            if (mix.containsMatchAttributeRuleForUsage(2) && mix.getRouteFlags() == 3) {
                if (voiceCommunicationCaptureMixes == null) {
                    voiceCommunicationCaptureMixes = new ArrayList<>();
                }
                voiceCommunicationCaptureMixes.add(mix);
            }
            if (mix.getRouteFlags() == 3 && projection != null) {
                requireValidProjection |= true;
            } else if (mix.isForCallRedirection()) {
                requireCallAudioInterception |= true;
            } else if (mix.containsMatchAttributeRuleForUsage(2)) {
                requireModifyRouting |= true;
            }
        }
        if (requireCaptureAudioOrMediaOutputPerm && !callerHasPermission("android.permission.CAPTURE_MEDIA_OUTPUT") && !callerHasPermission("android.permission.CAPTURE_AUDIO_OUTPUT")) {
            Log.e(TAG, "Privileged audio capture requires CAPTURE_MEDIA_OUTPUT or CAPTURE_AUDIO_OUTPUT system permission");
            return false;
        }
        if (voiceCommunicationCaptureMixes != null && voiceCommunicationCaptureMixes.size() > 0) {
            if (!callerHasPermission("android.permission.CAPTURE_VOICE_COMMUNICATION_OUTPUT")) {
                Log.e(TAG, "Audio capture for voice communication requires CAPTURE_VOICE_COMMUNICATION_OUTPUT system permission");
                return false;
            }
            Iterator<AudioMix> it2 = voiceCommunicationCaptureMixes.iterator();
            while (it2.hasNext()) {
                it2.next().getRule().setVoiceCommunicationCaptureAllowed(true);
            }
        }
        if (!requireValidProjection || canProjectAudio(projection)) {
            if (requireModifyRouting && !callerHasPermission("android.permission.MODIFY_AUDIO_ROUTING")) {
                Log.e(TAG, "Can not capture audio without MODIFY_AUDIO_ROUTING");
                return false;
            } else if (!requireCallAudioInterception || callerHasPermission("android.permission.CALL_AUDIO_INTERCEPTION")) {
                return true;
            } else {
                Log.e(TAG, "Can not capture audio without CALL_AUDIO_INTERCEPTION");
                return false;
            }
        }
        return false;
    }

    private boolean callerHasPermission(String permission) {
        return this.mContext.checkCallingPermission(permission) == 0;
    }

    private boolean canProjectAudio(IMediaProjection projection) {
        if (projection == null) {
            Log.e(TAG, "MediaProjection is null");
            return false;
        }
        IMediaProjectionManager projectionService = getProjectionService();
        if (projectionService == null) {
            Log.e(TAG, "Can't get service IMediaProjectionManager");
            return false;
        }
        try {
            if (!projectionService.isValidMediaProjection(projection)) {
                Log.w(TAG, "App passed invalid MediaProjection token");
                return false;
            }
            try {
                if (!projection.canProjectAudio()) {
                    Log.w(TAG, "App passed MediaProjection that can not project audio");
                    return false;
                }
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Can't call .canProjectAudio() on valid IMediaProjection" + projection.asBinder(), e);
                return false;
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "Can't call .isValidMediaProjection() on IMediaProjectionManager" + projectionService.asBinder(), e2);
            return false;
        }
    }

    private IMediaProjectionManager getProjectionService() {
        if (this.mProjectionService == null) {
            IBinder b = ServiceManager.getService("media_projection");
            this.mProjectionService = IMediaProjectionManager.Stub.asInterface(b);
        }
        return this.mProjectionService;
    }

    public void unregisterAudioPolicyAsync(IAudioPolicyCallback pcb) {
        if (pcb == null) {
            return;
        }
        unregisterAudioPolicyInt(pcb, "unregisterAudioPolicyAsync");
    }

    public void unregisterAudioPolicy(IAudioPolicyCallback pcb) {
        if (pcb == null) {
            return;
        }
        unregisterAudioPolicyInt(pcb, "unregisterAudioPolicy");
    }

    private void unregisterAudioPolicyInt(IAudioPolicyCallback pcb, String operationName) {
        this.mDynPolicyLogger.log(new AudioEventLogger.StringEvent(operationName + " for " + pcb.asBinder()).printLog(TAG));
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy app = this.mAudioPolicies.remove(pcb.asBinder());
            if (app == null) {
                Slog.w(TAG, "Trying to unregister unknown audio policy for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid());
                return;
            }
            pcb.asBinder().unlinkToDeath(app, 0);
            app.release();
        }
    }

    private AudioPolicyProxy checkUpdateForPolicy(IAudioPolicyCallback pcb, String errorMsg) {
        boolean hasPermissionForPolicy = this.mContext.checkCallingPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0;
        if (!hasPermissionForPolicy) {
            Slog.w(TAG, errorMsg + " for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid() + ", need MODIFY_AUDIO_ROUTING");
            return null;
        }
        AudioPolicyProxy app = this.mAudioPolicies.get(pcb.asBinder());
        if (app == null) {
            Slog.w(TAG, errorMsg + " for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid() + ", unregistered policy");
            return null;
        }
        return app;
    }

    public int addMixForPolicy(AudioPolicyConfig policyConfig, IAudioPolicyCallback pcb) {
        if (DEBUG_AP) {
            Log.d(TAG, "addMixForPolicy for " + pcb.asBinder() + " with config:" + policyConfig);
        }
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy app = checkUpdateForPolicy(pcb, "Cannot add AudioMix in audio policy");
            int i = -1;
            if (app == null) {
                return -1;
            }
            if (app.addMixes(policyConfig.getMixes()) == 0) {
                i = 0;
            }
            return i;
        }
    }

    public int removeMixForPolicy(AudioPolicyConfig policyConfig, IAudioPolicyCallback pcb) {
        if (DEBUG_AP) {
            Log.d(TAG, "removeMixForPolicy for " + pcb.asBinder() + " with config:" + policyConfig);
        }
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy app = checkUpdateForPolicy(pcb, "Cannot add AudioMix in audio policy");
            int i = -1;
            if (app == null) {
                return -1;
            }
            if (app.removeMixes(policyConfig.getMixes()) == 0) {
                i = 0;
            }
            return i;
        }
    }

    public int setUidDeviceAffinity(IAudioPolicyCallback pcb, int uid, int[] deviceTypes, String[] deviceAddresses) {
        if (DEBUG_AP) {
            Log.d(TAG, "setUidDeviceAffinity for " + pcb.asBinder() + " uid:" + uid);
        }
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy app = checkUpdateForPolicy(pcb, "Cannot change device affinity in audio policy");
            if (app == null) {
                return -1;
            }
            if (!app.hasMixRoutedToDevices(deviceTypes, deviceAddresses)) {
                return -1;
            }
            return app.setUidDeviceAffinities(uid, deviceTypes, deviceAddresses);
        }
    }

    public int setUserIdDeviceAffinity(IAudioPolicyCallback pcb, int userId, int[] deviceTypes, String[] deviceAddresses) {
        if (DEBUG_AP) {
            Log.d(TAG, "setUserIdDeviceAffinity for " + pcb.asBinder() + " user:" + userId);
        }
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy app = checkUpdateForPolicy(pcb, "Cannot change device affinity in audio policy");
            if (app == null) {
                return -1;
            }
            if (!app.hasMixRoutedToDevices(deviceTypes, deviceAddresses)) {
                return -1;
            }
            return app.setUserIdDeviceAffinities(userId, deviceTypes, deviceAddresses);
        }
    }

    public int removeUidDeviceAffinity(IAudioPolicyCallback pcb, int uid) {
        if (DEBUG_AP) {
            Log.d(TAG, "removeUidDeviceAffinity for " + pcb.asBinder() + " uid:" + uid);
        }
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy app = checkUpdateForPolicy(pcb, "Cannot remove device affinity in audio policy");
            if (app == null) {
                return -1;
            }
            return app.removeUidDeviceAffinities(uid);
        }
    }

    public int removeUserIdDeviceAffinity(IAudioPolicyCallback pcb, int userId) {
        if (DEBUG_AP) {
            Log.d(TAG, "removeUserIdDeviceAffinity for " + pcb.asBinder() + " userId:" + userId);
        }
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy app = checkUpdateForPolicy(pcb, "Cannot remove device affinity in audio policy");
            if (app == null) {
                return -1;
            }
            return app.removeUserIdDeviceAffinities(userId);
        }
    }

    public int setFocusPropertiesForPolicy(int duckingBehavior, IAudioPolicyCallback pcb) {
        if (DEBUG_AP) {
            Log.d(TAG, "setFocusPropertiesForPolicy() duck behavior=" + duckingBehavior + " policy " + pcb.asBinder());
        }
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy app = checkUpdateForPolicy(pcb, "Cannot change audio policy focus properties");
            if (app == null) {
                return -1;
            }
            if (!this.mAudioPolicies.containsKey(pcb.asBinder())) {
                Slog.e(TAG, "Cannot change audio policy focus properties, unregistered policy");
                return -1;
            }
            boolean z = true;
            if (duckingBehavior == 1) {
                for (AudioPolicyProxy policy : this.mAudioPolicies.values()) {
                    if (policy.mFocusDuckBehavior == 1) {
                        Slog.e(TAG, "Cannot change audio policy ducking behavior, already handled");
                        return -1;
                    }
                }
            }
            app.mFocusDuckBehavior = duckingBehavior;
            MediaFocusControl mediaFocusControl = this.mMediaFocusControl;
            if (duckingBehavior != 1) {
                z = false;
            }
            mediaFocusControl.setDuckingInExtPolicyAvailable(z);
            return 0;
        }
    }

    public List<AudioFocusInfo> getFocusStack() {
        enforceModifyAudioRoutingPermission();
        return this.mMediaFocusControl.getFocusStack();
    }

    public boolean sendFocusLoss(AudioFocusInfo focusLoser, IAudioPolicyCallback apcb) {
        Objects.requireNonNull(focusLoser);
        Objects.requireNonNull(apcb);
        enforceModifyAudioRoutingPermission();
        if (!this.mAudioPolicies.containsKey(apcb.asBinder())) {
            throw new IllegalStateException("Only registered AudioPolicy can change focus");
        }
        if (!this.mAudioPolicies.get(apcb.asBinder()).mHasFocusListener) {
            throw new IllegalStateException("AudioPolicy must have focus listener to change focus");
        }
        return this.mMediaFocusControl.sendFocusLoss(focusLoser);
    }

    public String getHalVersion() {
        String[] strArr;
        for (String version : HAL_VERSIONS) {
            try {
                HwBinder.getService(String.format("android.hardware.audio@%s::IDevicesFactory", version), HealthServiceWrapperHidl.INSTANCE_VENDOR);
                return version;
            } catch (RemoteException re) {
                Log.e(TAG, "Remote exception when getting hardware audio service:", re);
            } catch (NoSuchElementException e) {
            }
        }
        return null;
    }

    public boolean hasRegisteredDynamicPolicy() {
        boolean z;
        synchronized (this.mAudioPolicies) {
            z = !this.mAudioPolicies.isEmpty();
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setExtVolumeController(IAudioPolicyCallback apc) {
        if (!this.mContext.getResources().getBoolean(17891676)) {
            Log.e(TAG, "Cannot set external volume controller: device not set for volume keys handled in PhoneWindowManager");
            return;
        }
        synchronized (this.mExtVolumeControllerLock) {
            IAudioPolicyCallback iAudioPolicyCallback = this.mExtVolumeController;
            if (iAudioPolicyCallback != null && !iAudioPolicyCallback.asBinder().pingBinder()) {
                Log.e(TAG, "Cannot set external volume controller: existing controller");
            }
            this.mExtVolumeController = apc;
        }
    }

    private void dumpAudioPolicies(PrintWriter pw) {
        pw.println("\nAudio policies:");
        synchronized (this.mAudioPolicies) {
            for (AudioPolicyProxy policy : this.mAudioPolicies.values()) {
                pw.println(policy.toLogFriendlyString());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onDynPolicyMixStateUpdate(String regId, int state) {
        if (DEBUG_AP) {
            Log.d(TAG, "onDynamicPolicyMixStateUpdate(" + regId + ", " + state + ")");
        }
        synchronized (this.mAudioPolicies) {
            for (AudioPolicyProxy policy : this.mAudioPolicies.values()) {
                Iterator it = policy.getMixes().iterator();
                while (it.hasNext()) {
                    AudioMix mix = (AudioMix) it.next();
                    if (mix.getRegistration().equals(regId)) {
                        try {
                            policy.mPolicyCallback.notifyMixStateUpdate(regId, state);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Can't call notifyMixStateUpdate() on IAudioPolicyCallback " + policy.mPolicyCallback.asBinder(), e);
                        }
                        return;
                    }
                }
            }
        }
    }

    public void registerRecordingCallback(IRecordingConfigDispatcher rcdb) {
        boolean isPrivileged = this.mContext.checkCallingPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0;
        this.mRecordMonitor.registerRecordingCallback(rcdb, isPrivileged);
    }

    public void unregisterRecordingCallback(IRecordingConfigDispatcher rcdb) {
        this.mRecordMonitor.unregisterRecordingCallback(rcdb);
    }

    public List<AudioRecordingConfiguration> getActiveRecordingConfigurations() {
        boolean isPrivileged = this.mContext.checkCallingPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0;
        return this.mRecordMonitor.getActiveRecordingConfigurations(isPrivileged);
    }

    public int trackRecorder(IBinder recorder) {
        return this.mRecordMonitor.trackRecorder(recorder);
    }

    public void recorderEvent(int riid, int event) {
        this.mRecordMonitor.recorderEvent(riid, event);
    }

    public void releaseRecorder(int riid) {
        this.mRecordMonitor.releaseRecorder(riid);
    }

    public void registerPlaybackCallback(IPlaybackConfigDispatcher pcdb) {
        boolean isPrivileged = this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0;
        this.mPlaybackMonitor.registerPlaybackCallback(pcdb, isPrivileged);
    }

    public void unregisterPlaybackCallback(IPlaybackConfigDispatcher pcdb) {
        this.mPlaybackMonitor.unregisterPlaybackCallback(pcdb);
    }

    public List<AudioPlaybackConfiguration> getActivePlaybackConfigurations() {
        boolean isPrivileged = this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0;
        return this.mPlaybackMonitor.getActivePlaybackConfigurations(isPrivileged);
    }

    public int trackPlayer(PlayerBase.PlayerIdCard pic) {
        if (pic != null && pic.mAttributes != null) {
            validateAudioAttributesUsage(pic.mAttributes);
        }
        return this.mPlaybackMonitor.trackPlayer(pic);
    }

    public void playerAttributes(int piid, AudioAttributes attr) {
        if (attr != null) {
            validateAudioAttributesUsage(attr);
        }
        this.mPlaybackMonitor.playerAttributes(piid, attr, Binder.getCallingUid());
    }

    public void playerSessionId(int piid, int sessionId) {
        if (sessionId <= 0) {
            throw new IllegalArgumentException("invalid session Id " + sessionId);
        }
        this.mPlaybackMonitor.playerSessionId(piid, sessionId, Binder.getCallingUid());
    }

    public void playerEvent(int piid, int event, int deviceId) {
        this.mPlaybackMonitor.playerEvent(piid, event, deviceId, Binder.getCallingUid());
    }

    public void playerHasOpPlayAudio(int piid, boolean hasOpPlayAudio) {
        this.mPlaybackMonitor.playerHasOpPlayAudio(piid, hasOpPlayAudio, Binder.getCallingUid());
    }

    public void releasePlayer(int piid) {
        this.mPlaybackMonitor.releasePlayer(piid, Binder.getCallingUid());
    }

    public int setAllowedCapturePolicy(int capturePolicy) {
        int result;
        int callingUid = Binder.getCallingUid();
        int flags = AudioAttributes.capturePolicyToFlags(capturePolicy, 0);
        long identity = Binder.clearCallingIdentity();
        synchronized (this.mPlaybackMonitor) {
            result = this.mAudioSystem.setAllowedCapturePolicy(callingUid, flags);
            if (result == 0) {
                this.mPlaybackMonitor.setAllowedCapturePolicy(callingUid, capturePolicy);
            }
            Binder.restoreCallingIdentity(identity);
        }
        return result;
    }

    public int getAllowedCapturePolicy() {
        int callingUid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        int capturePolicy = this.mPlaybackMonitor.getAllowedCapturePolicy(callingUid);
        Binder.restoreCallingIdentity(identity);
        return capturePolicy;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class AudioDeviceArray {
        final String[] mDeviceAddresses;
        final int[] mDeviceTypes;

        AudioDeviceArray(int[] types, String[] addresses) {
            this.mDeviceTypes = types;
            this.mDeviceAddresses = addresses;
        }
    }

    /* loaded from: classes.dex */
    public class AudioPolicyProxy extends AudioPolicyConfig implements IBinder.DeathRecipient {
        private static final String TAG = "AudioPolicyProxy";
        int mFocusDuckBehavior;
        final boolean mHasFocusListener;
        boolean mIsFocusPolicy;
        boolean mIsTestFocusPolicy;
        final boolean mIsVolumeController;
        final IAudioPolicyCallback mPolicyCallback;
        final IMediaProjection mProjection;
        UnregisterOnStopCallback mProjectionCallback;
        final HashMap<Integer, AudioDeviceArray> mUidDeviceAffinities;
        final HashMap<Integer, AudioDeviceArray> mUserIdDeviceAffinities;

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public final class UnregisterOnStopCallback extends IMediaProjectionCallback.Stub {
            private UnregisterOnStopCallback() {
            }

            public void onStop() {
                AudioService.this.unregisterAudioPolicyAsync(AudioPolicyProxy.this.mPolicyCallback);
            }
        }

        AudioPolicyProxy(AudioPolicyConfig config, IAudioPolicyCallback token, boolean hasFocusListener, boolean isFocusPolicy, boolean isTestFocusPolicy, boolean isVolumeController, IMediaProjection projection) {
            super(config);
            this.mUidDeviceAffinities = new HashMap<>();
            this.mUserIdDeviceAffinities = new HashMap<>();
            this.mFocusDuckBehavior = 0;
            this.mIsFocusPolicy = false;
            this.mIsTestFocusPolicy = false;
            StringBuilder append = new StringBuilder().append(config.hashCode()).append(":ap:");
            int i = AudioService.this.mAudioPolicyCounter;
            AudioService.this.mAudioPolicyCounter = i + 1;
            setRegistration(new String(append.append(i).toString()));
            this.mPolicyCallback = token;
            this.mHasFocusListener = hasFocusListener;
            this.mIsVolumeController = isVolumeController;
            this.mProjection = projection;
            if (hasFocusListener) {
                AudioService.this.mMediaFocusControl.addFocusFollower(token);
                if (isFocusPolicy) {
                    this.mIsFocusPolicy = true;
                    this.mIsTestFocusPolicy = isTestFocusPolicy;
                    AudioService.this.mMediaFocusControl.setFocusPolicy(token, this.mIsTestFocusPolicy);
                }
            }
            if (isVolumeController) {
                AudioService.this.setExtVolumeController(token);
            }
            if (projection != null) {
                UnregisterOnStopCallback unregisterOnStopCallback = new UnregisterOnStopCallback();
                this.mProjectionCallback = unregisterOnStopCallback;
                try {
                    projection.registerCallback(unregisterOnStopCallback);
                } catch (RemoteException e) {
                    release();
                    throw new IllegalStateException("MediaProjection callback registration failed, could not link to " + projection + " binder death", e);
                }
            }
            int status = connectMixes();
            if (status != 0) {
                release();
                throw new IllegalStateException("Could not connect mix, error: " + status);
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            AudioService.this.mDynPolicyLogger.log(new AudioEventLogger.StringEvent("AudioPolicy " + this.mPolicyCallback.asBinder() + " died").printLog(TAG));
            release();
        }

        String getRegistrationId() {
            return getRegistration();
        }

        void release() {
            if (this.mIsFocusPolicy) {
                AudioService.this.mMediaFocusControl.unsetFocusPolicy(this.mPolicyCallback, this.mIsTestFocusPolicy);
            }
            if (this.mFocusDuckBehavior == 1) {
                AudioService.this.mMediaFocusControl.setDuckingInExtPolicyAvailable(false);
            }
            if (this.mHasFocusListener) {
                AudioService.this.mMediaFocusControl.removeFocusFollower(this.mPolicyCallback);
            }
            UnregisterOnStopCallback unregisterOnStopCallback = this.mProjectionCallback;
            if (unregisterOnStopCallback != null) {
                try {
                    this.mProjection.unregisterCallback(unregisterOnStopCallback);
                } catch (RemoteException e) {
                    Log.e(TAG, "Fail to unregister Audiopolicy callback from MediaProjection");
                }
            }
            if (this.mIsVolumeController) {
                synchronized (AudioService.this.mExtVolumeControllerLock) {
                    AudioService.this.mExtVolumeController = null;
                }
            }
            long identity = Binder.clearCallingIdentity();
            AudioService.this.mAudioSystem.registerPolicyMixes(this.mMixes, false);
            Binder.restoreCallingIdentity(identity);
            synchronized (AudioService.this.mAudioPolicies) {
                AudioService.this.mAudioPolicies.remove(this.mPolicyCallback.asBinder());
            }
            try {
                this.mPolicyCallback.notifyUnregistration();
            } catch (RemoteException e2) {
            }
        }

        boolean hasMixAffectingUsage(int usage, int excludedFlags) {
            Iterator it = this.mMixes.iterator();
            while (it.hasNext()) {
                AudioMix mix = (AudioMix) it.next();
                if (mix.isAffectingUsage(usage) && (mix.getRouteFlags() & excludedFlags) != excludedFlags) {
                    return true;
                }
            }
            return false;
        }

        boolean hasMixRoutedToDevices(int[] deviceTypes, String[] deviceAddresses) {
            for (int i = 0; i < deviceTypes.length; i++) {
                boolean hasDevice = false;
                Iterator it = this.mMixes.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    AudioMix mix = (AudioMix) it.next();
                    if (mix.isRoutedToDevice(deviceTypes[i], deviceAddresses[i])) {
                        hasDevice = true;
                        break;
                    }
                }
                if (!hasDevice) {
                    return false;
                }
            }
            return true;
        }

        int addMixes(ArrayList<AudioMix> mixes) {
            int registerPolicyMixes;
            synchronized (this.mMixes) {
                AudioService.this.mAudioSystem.registerPolicyMixes(this.mMixes, false);
                add(mixes);
                registerPolicyMixes = AudioService.this.mAudioSystem.registerPolicyMixes(this.mMixes, true);
            }
            return registerPolicyMixes;
        }

        int removeMixes(ArrayList<AudioMix> mixes) {
            int registerPolicyMixes;
            synchronized (this.mMixes) {
                AudioService.this.mAudioSystem.registerPolicyMixes(this.mMixes, false);
                remove(mixes);
                registerPolicyMixes = AudioService.this.mAudioSystem.registerPolicyMixes(this.mMixes, true);
            }
            return registerPolicyMixes;
        }

        int connectMixes() {
            long identity = Binder.clearCallingIdentity();
            int status = AudioService.this.mAudioSystem.registerPolicyMixes(this.mMixes, true);
            Binder.restoreCallingIdentity(identity);
            return status;
        }

        int setUidDeviceAffinities(int uid, int[] types, String[] addresses) {
            Integer Uid = new Integer(uid);
            if (this.mUidDeviceAffinities.remove(Uid) != null && removeUidDeviceAffinitiesFromSystem(uid) != 0) {
                Log.e(TAG, "AudioSystem. removeUidDeviceAffinities(" + uid + ") failed,  cannot call AudioSystem.setUidDeviceAffinities");
                return -1;
            }
            AudioDeviceArray deviceArray = new AudioDeviceArray(types, addresses);
            if (setUidDeviceAffinitiesOnSystem(uid, deviceArray) == 0) {
                this.mUidDeviceAffinities.put(Uid, deviceArray);
                return 0;
            }
            Log.e(TAG, "AudioSystem. setUidDeviceAffinities(" + uid + ") failed");
            return -1;
        }

        int removeUidDeviceAffinities(int uid) {
            if (this.mUidDeviceAffinities.remove(new Integer(uid)) != null && removeUidDeviceAffinitiesFromSystem(uid) == 0) {
                return 0;
            }
            Log.e(TAG, "AudioSystem. removeUidDeviceAffinities failed");
            return -1;
        }

        private int removeUidDeviceAffinitiesFromSystem(int uid) {
            long identity = Binder.clearCallingIdentity();
            try {
                return AudioService.this.mAudioSystem.removeUidDeviceAffinities(uid);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private int setUidDeviceAffinitiesOnSystem(int uid, AudioDeviceArray deviceArray) {
            long identity = Binder.clearCallingIdentity();
            try {
                return AudioService.this.mAudioSystem.setUidDeviceAffinities(uid, deviceArray.mDeviceTypes, deviceArray.mDeviceAddresses);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        int setUserIdDeviceAffinities(int userId, int[] types, String[] addresses) {
            Integer UserId = new Integer(userId);
            if (this.mUserIdDeviceAffinities.remove(UserId) != null && removeUserIdDeviceAffinitiesFromSystem(userId) != 0) {
                Log.e(TAG, "AudioSystem. removeUserIdDeviceAffinities(" + UserId + ") failed,  cannot call AudioSystem.setUserIdDeviceAffinities");
                return -1;
            }
            AudioDeviceArray audioDeviceArray = new AudioDeviceArray(types, addresses);
            if (setUserIdDeviceAffinitiesOnSystem(userId, audioDeviceArray) == 0) {
                this.mUserIdDeviceAffinities.put(UserId, audioDeviceArray);
                return 0;
            }
            Log.e(TAG, "AudioSystem.setUserIdDeviceAffinities(" + userId + ") failed");
            return -1;
        }

        int removeUserIdDeviceAffinities(int userId) {
            if (this.mUserIdDeviceAffinities.remove(new Integer(userId)) != null && removeUserIdDeviceAffinitiesFromSystem(userId) == 0) {
                return 0;
            }
            Log.e(TAG, "AudioSystem.removeUserIdDeviceAffinities failed");
            return -1;
        }

        private int removeUserIdDeviceAffinitiesFromSystem(int userId) {
            long identity = Binder.clearCallingIdentity();
            try {
                return AudioService.this.mAudioSystem.removeUserIdDeviceAffinities(userId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private int setUserIdDeviceAffinitiesOnSystem(int userId, AudioDeviceArray deviceArray) {
            long identity = Binder.clearCallingIdentity();
            try {
                return AudioService.this.mAudioSystem.setUserIdDeviceAffinities(userId, deviceArray.mDeviceTypes, deviceArray.mDeviceAddresses);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        int setupDeviceAffinities() {
            for (Map.Entry<Integer, AudioDeviceArray> uidEntry : this.mUidDeviceAffinities.entrySet()) {
                int uidStatus = removeUidDeviceAffinitiesFromSystem(uidEntry.getKey().intValue());
                if (uidStatus != 0) {
                    Log.e(TAG, "setupDeviceAffinities failed to remove device affinity for uid " + uidEntry.getKey());
                    return uidStatus;
                }
                int uidStatus2 = setUidDeviceAffinitiesOnSystem(uidEntry.getKey().intValue(), uidEntry.getValue());
                if (uidStatus2 != 0) {
                    Log.e(TAG, "setupDeviceAffinities failed to set device affinity for uid " + uidEntry.getKey());
                    return uidStatus2;
                }
            }
            for (Map.Entry<Integer, AudioDeviceArray> userIdEntry : this.mUserIdDeviceAffinities.entrySet()) {
                int userIdStatus = removeUserIdDeviceAffinitiesFromSystem(userIdEntry.getKey().intValue());
                if (userIdStatus != 0) {
                    Log.e(TAG, "setupDeviceAffinities failed to remove device affinity for userId " + userIdEntry.getKey());
                    return userIdStatus;
                }
                int userIdStatus2 = setUserIdDeviceAffinitiesOnSystem(userIdEntry.getKey().intValue(), userIdEntry.getValue());
                if (userIdStatus2 != 0) {
                    Log.e(TAG, "setupDeviceAffinities failed to set device affinity for userId " + userIdEntry.getKey());
                    return userIdStatus2;
                }
            }
            return 0;
        }

        public String toLogFriendlyString() {
            String textDump = super.toLogFriendlyString();
            String textDump2 = (((((textDump + " Uid Device Affinities:\n") + logFriendlyAttributeDeviceArrayMap("Uid", this.mUidDeviceAffinities, "     ")) + " UserId Device Affinities:\n") + logFriendlyAttributeDeviceArrayMap("UserId", this.mUserIdDeviceAffinities, "     ")) + " Proxy:\n") + "   is focus policy= " + this.mIsFocusPolicy + "\n";
            if (this.mIsFocusPolicy) {
                textDump2 = ((textDump2 + "     focus duck behaviour= " + this.mFocusDuckBehavior + "\n") + "     is test focus policy= " + this.mIsTestFocusPolicy + "\n") + "     has focus listener= " + this.mHasFocusListener + "\n";
            }
            return textDump2 + "   media projection= " + this.mProjection + "\n";
        }

        private String logFriendlyAttributeDeviceArrayMap(String attribute, Map<Integer, AudioDeviceArray> map, String spacer) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Map.Entry<Integer, AudioDeviceArray> mapEntry : map.entrySet()) {
                stringBuilder.append(spacer).append(attribute).append(": ").append(mapEntry.getKey()).append("\n");
                AudioDeviceArray deviceArray = mapEntry.getValue();
                String deviceSpacer = spacer + "   ";
                for (int i = 0; i < deviceArray.mDeviceTypes.length; i++) {
                    stringBuilder.append(deviceSpacer).append("Type: 0x").append(Integer.toHexString(deviceArray.mDeviceTypes[i])).append(" Address: ").append(deviceArray.mDeviceAddresses[i]).append("\n");
                }
            }
            return stringBuilder.toString();
        }
    }

    public int dispatchFocusChange(AudioFocusInfo afi, int focusChange, IAudioPolicyCallback pcb) {
        int dispatchFocusChange;
        if (afi == null) {
            throw new IllegalArgumentException("Illegal null AudioFocusInfo");
        }
        if (pcb == null) {
            throw new IllegalArgumentException("Illegal null AudioPolicy callback");
        }
        synchronized (this.mAudioPolicies) {
            if (!this.mAudioPolicies.containsKey(pcb.asBinder())) {
                throw new IllegalStateException("Unregistered AudioPolicy for focus dispatch");
            }
            dispatchFocusChange = this.mMediaFocusControl.dispatchFocusChange(afi, focusChange);
        }
        return dispatchFocusChange;
    }

    public void setFocusRequestResultFromExtPolicy(AudioFocusInfo afi, int requestResult, IAudioPolicyCallback pcb) {
        if (afi == null) {
            throw new IllegalArgumentException("Illegal null AudioFocusInfo");
        }
        if (pcb == null) {
            throw new IllegalArgumentException("Illegal null AudioPolicy callback");
        }
        synchronized (this.mAudioPolicies) {
            if (!this.mAudioPolicies.containsKey(pcb.asBinder())) {
                throw new IllegalStateException("Unregistered AudioPolicy for external focus");
            }
            this.mMediaFocusControl.setFocusRequestResultFromExtPolicy(afi, requestResult);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AsdProxy implements IBinder.DeathRecipient {
        private final IAudioServerStateDispatcher mAsd;

        AsdProxy(IAudioServerStateDispatcher asd) {
            this.mAsd = asd;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (AudioService.this.mAudioServerStateListeners) {
                AudioService.this.mAudioServerStateListeners.remove(this.mAsd.asBinder());
            }
        }

        IAudioServerStateDispatcher callback() {
            return this.mAsd;
        }
    }

    private void checkMonitorAudioServerStatePermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") != 0) {
            throw new SecurityException("Not allowed to monitor audioserver state");
        }
    }

    public void registerAudioServerStateDispatcher(IAudioServerStateDispatcher asd) {
        checkMonitorAudioServerStatePermission();
        synchronized (this.mAudioServerStateListeners) {
            if (this.mAudioServerStateListeners.containsKey(asd.asBinder())) {
                Slog.w(TAG, "Cannot re-register audio server state dispatcher");
                return;
            }
            AsdProxy asdp = new AsdProxy(asd);
            try {
                asd.asBinder().linkToDeath(asdp, 0);
            } catch (RemoteException e) {
            }
            this.mAudioServerStateListeners.put(asd.asBinder(), asdp);
        }
    }

    public void unregisterAudioServerStateDispatcher(IAudioServerStateDispatcher asd) {
        checkMonitorAudioServerStatePermission();
        synchronized (this.mAudioServerStateListeners) {
            AsdProxy asdp = this.mAudioServerStateListeners.remove(asd.asBinder());
            if (asdp == null) {
                Slog.w(TAG, "Trying to unregister unknown audioserver state dispatcher for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid());
            } else {
                asd.asBinder().unlinkToDeath(asdp, 0);
            }
        }
    }

    public boolean isAudioServerRunning() {
        checkMonitorAudioServerStatePermission();
        return AudioSystem.checkAudioFlinger() == 0;
    }

    private Set<Integer> getAudioHalPids() {
        try {
            IServiceManager serviceManager = IServiceManager.getService();
            ArrayList<IServiceManager.InstanceDebugInfo> dump = serviceManager.debugDump();
            HashSet<Integer> pids = new HashSet<>();
            Iterator<IServiceManager.InstanceDebugInfo> it = dump.iterator();
            while (it.hasNext()) {
                IServiceManager.InstanceDebugInfo info = it.next();
                if (info.pid != -1 && info.interfaceName != null && info.interfaceName.startsWith(AUDIO_HAL_SERVICE_PREFIX)) {
                    pids.add(Integer.valueOf(info.pid));
                }
            }
            return pids;
        } catch (RemoteException e) {
            return new HashSet();
        }
    }

    private void updateAudioHalPids() {
        Set<Integer> pidsSet = getAudioHalPids();
        if (pidsSet.isEmpty()) {
            Slog.w(TAG, "Could not retrieve audio HAL service pids");
            return;
        }
        int[] pidsArray = pidsSet.stream().mapToInt(new AudioService$$ExternalSyntheticLambda1()).toArray();
        AudioSystem.setAudioHalPids(pidsArray);
    }

    public void setMultiAudioFocusEnabled(boolean enabled) {
        enforceModifyAudioRoutingPermission();
        MediaFocusControl mediaFocusControl = this.mMediaFocusControl;
        if (mediaFocusControl != null) {
            boolean mafEnabled = mediaFocusControl.getMultiAudioFocusEnabled();
            if (mafEnabled != enabled) {
                this.mMediaFocusControl.updateMultiAudioFocus(enabled);
                if (!enabled) {
                    this.mDeviceBroker.postBroadcastBecomingNoisy();
                }
            }
        }
    }

    public boolean setAdditionalOutputDeviceDelay(AudioDeviceAttributes device, long delayMillis) {
        Objects.requireNonNull(device, "device must not be null");
        enforceModifyAudioRoutingPermission();
        String getterKey = "additional_output_device_delay=" + device.getInternalType() + "," + device.getAddress();
        String setterKey = getterKey + "," + delayMillis;
        return this.mRestorableParameters.setParameters(getterKey, setterKey) == 0;
    }

    public long getAdditionalOutputDeviceDelay(AudioDeviceAttributes device) {
        Objects.requireNonNull(device, "device must not be null");
        String reply = AudioSystem.getParameters("additional_output_device_delay=" + device.getInternalType() + "," + device.getAddress());
        try {
            long delayMillis = Long.parseLong(reply.substring("additional_output_device_delay".length() + 1));
            return delayMillis;
        } catch (NullPointerException e) {
            return 0L;
        }
    }

    public long getMaxAdditionalOutputDeviceDelay(AudioDeviceAttributes device) {
        Objects.requireNonNull(device, "device must not be null");
        String reply = AudioSystem.getParameters("max_additional_output_device_delay=" + device.getInternalType() + "," + device.getAddress());
        try {
            long delayMillis = Long.parseLong(reply.substring("max_additional_output_device_delay".length() + 1));
            return delayMillis;
        } catch (NullPointerException e) {
            return 0L;
        }
    }

    public void addAssistantServicesUids(int[] assistantUids) {
        enforceModifyAudioRoutingPermission();
        Objects.requireNonNull(assistantUids);
        synchronized (this.mSettingsLock) {
            addAssistantServiceUidsLocked(assistantUids);
        }
    }

    public void removeAssistantServicesUids(int[] assistantUids) {
        enforceModifyAudioRoutingPermission();
        Objects.requireNonNull(assistantUids);
        synchronized (this.mSettingsLock) {
            removeAssistantServiceUidsLocked(assistantUids);
        }
    }

    public int[] getAssistantServicesUids() {
        int[] assistantUids;
        enforceModifyAudioRoutingPermission();
        synchronized (this.mSettingsLock) {
            assistantUids = this.mAssistantUids.stream().mapToInt(new AudioService$$ExternalSyntheticLambda1()).toArray();
        }
        return assistantUids;
    }

    public void setActiveAssistantServiceUids(int[] activeAssistantUids) {
        enforceModifyAudioRoutingPermission();
        Objects.requireNonNull(activeAssistantUids);
        synchronized (this.mSettingsLock) {
            this.mActiveAssistantServiceUids = activeAssistantUids;
        }
        updateActiveAssistantServiceUids();
    }

    public int[] getActiveAssistantServiceUids() {
        int[] activeAssistantUids;
        enforceModifyAudioRoutingPermission();
        synchronized (this.mSettingsLock) {
            activeAssistantUids = (int[]) this.mActiveAssistantServiceUids.clone();
        }
        return activeAssistantUids;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UUID getDeviceSensorUuid(AudioDeviceAttributes device) {
        return this.mDeviceBroker.getDeviceSensorUuid(device);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isFixedVolumeDevice(int deviceType) {
        if (deviceType == 32768 && this.mRecordMonitor.isLegacyRemoteSubmixActive()) {
            return false;
        }
        return this.mFixedVolumeDevices.contains(Integer.valueOf(deviceType));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isFullVolumeDevice(int deviceType) {
        if (deviceType == 32768 && this.mRecordMonitor.isLegacyRemoteSubmixActive()) {
            return false;
        }
        return this.mFullVolumeDevices.contains(Integer.valueOf(deviceType));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAbsoluteVolumeDevice(int deviceType) {
        return this.mAbsoluteVolumeDeviceInfoMap.containsKey(Integer.valueOf(deviceType));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isA2dpAbsoluteVolumeDevice(int deviceType) {
        return this.mAvrcpAbsVolSupported && AudioSystem.DEVICE_OUT_ALL_A2DP_SET.contains(Integer.valueOf(deviceType));
    }

    private static String getSettingsNameForDeviceVolumeBehavior(int deviceType) {
        return "AudioService_DeviceVolumeBehavior_" + AudioSystem.getOutputDeviceName(deviceType);
    }

    private void persistDeviceVolumeBehavior(int deviceType, int deviceVolumeBehavior) {
        if (DEBUG_VOL) {
            Log.d(TAG, "Persisting Volume Behavior for DeviceType: " + deviceType);
        }
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            this.mSettings.putSystemIntForUser(this.mContentResolver, getSettingsNameForDeviceVolumeBehavior(deviceType), deviceVolumeBehavior, -2);
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private int retrieveStoredDeviceVolumeBehavior(int deviceType) {
        return this.mSettings.getSystemIntForUser(this.mContentResolver, getSettingsNameForDeviceVolumeBehavior(deviceType), -1, -2);
    }

    private void restoreDeviceVolumeBehavior() {
        for (Integer num : AudioSystem.DEVICE_OUT_ALL_SET) {
            int deviceType = num.intValue();
            boolean z = DEBUG_VOL;
            if (z) {
                Log.d(TAG, "Retrieving Volume Behavior for DeviceType: " + deviceType);
            }
            int deviceVolumeBehavior = retrieveStoredDeviceVolumeBehavior(deviceType);
            if (deviceVolumeBehavior == -1) {
                if (z) {
                    Log.d(TAG, "Skipping Setting Volume Behavior for DeviceType: " + deviceType);
                }
            } else {
                setDeviceVolumeBehaviorInternal(new AudioDeviceAttributes(deviceType, ""), deviceVolumeBehavior, "AudioService.restoreDeviceVolumeBehavior()");
            }
        }
    }

    private boolean hasDeviceVolumeBehavior(int audioSystemDeviceOut) {
        return retrieveStoredDeviceVolumeBehavior(audioSystemDeviceOut) != -1;
    }

    private boolean addAudioSystemDeviceOutToFixedVolumeDevices(int audioSystemDeviceOut) {
        if (DEBUG_VOL) {
            Log.d(TAG, "Adding DeviceType: 0x" + Integer.toHexString(audioSystemDeviceOut) + " to mFixedVolumeDevices");
        }
        return this.mFixedVolumeDevices.add(Integer.valueOf(audioSystemDeviceOut));
    }

    private boolean removeAudioSystemDeviceOutFromFixedVolumeDevices(int audioSystemDeviceOut) {
        if (DEBUG_VOL) {
            Log.d(TAG, "Removing DeviceType: 0x" + Integer.toHexString(audioSystemDeviceOut) + " from mFixedVolumeDevices");
        }
        return this.mFixedVolumeDevices.remove(Integer.valueOf(audioSystemDeviceOut));
    }

    private boolean addAudioSystemDeviceOutToFullVolumeDevices(int audioSystemDeviceOut) {
        if (DEBUG_VOL) {
            Log.d(TAG, "Adding DeviceType: 0x" + Integer.toHexString(audioSystemDeviceOut) + " to mFullVolumeDevices");
        }
        return this.mFullVolumeDevices.add(Integer.valueOf(audioSystemDeviceOut));
    }

    private boolean removeAudioSystemDeviceOutFromFullVolumeDevices(int audioSystemDeviceOut) {
        if (DEBUG_VOL) {
            Log.d(TAG, "Removing DeviceType: 0x" + Integer.toHexString(audioSystemDeviceOut) + " from mFullVolumeDevices");
        }
        return this.mFullVolumeDevices.remove(Integer.valueOf(audioSystemDeviceOut));
    }

    private AbsoluteVolumeDeviceInfo addAudioSystemDeviceOutToAbsVolumeDevices(int audioSystemDeviceOut, AbsoluteVolumeDeviceInfo info) {
        if (DEBUG_VOL) {
            Log.d(TAG, "Adding DeviceType: 0x" + Integer.toHexString(audioSystemDeviceOut) + " from mAbsoluteVolumeDeviceInfoMap");
        }
        return this.mAbsoluteVolumeDeviceInfoMap.put(Integer.valueOf(audioSystemDeviceOut), info);
    }

    private AbsoluteVolumeDeviceInfo removeAudioSystemDeviceOutFromAbsVolumeDevices(int audioSystemDeviceOut) {
        if (DEBUG_VOL) {
            Log.d(TAG, "Removing DeviceType: 0x" + Integer.toHexString(audioSystemDeviceOut) + " from mAbsoluteVolumeDeviceInfoMap");
        }
        return this.mAbsoluteVolumeDeviceInfoMap.remove(Integer.valueOf(audioSystemDeviceOut));
    }

    private boolean checkNoteAppOp(int op, int uid, String packageName, String attributionTag) {
        try {
            if (this.mAppOps.noteOp(op, uid, packageName, attributionTag, (String) null) != 0) {
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error noting op:" + op + " on uid:" + uid + " for package:" + packageName, e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioServiceExt getAudioServiceExtInstance() {
        AudioDeviceBroker audioDeviceBroker;
        synchronized (this.mAudioServiceExtLock) {
            if (this.mAudioServiceExt == null) {
                this.mAudioServiceExt = MtkSystemServiceFactory.getInstance().makeAudioServiceExt();
            }
            if (!this.mAudioServiceExt.isSystemReady() && (audioDeviceBroker = this.mDeviceBroker) != null) {
                this.mAudioServiceExt.init(this.mContext, this, this.mAudioSystem, this.mSystemServer, audioDeviceBroker);
            }
        }
        return this.mAudioServiceExt;
    }

    public int getBleCgVolume() {
        int leAudioVolIndex = 0;
        int leAudioVssVol = getVssVolumeForDevice(0, 536870912);
        if (leAudioVssVol > 0) {
            leAudioVolIndex = leAudioVssVol / 10;
        }
        if (DEBUG_VOL) {
            Log.d(TAG, "getBleCgVolume() index=" + leAudioVolIndex);
        }
        return leAudioVolIndex;
    }

    public int getLastHfpScoVolume() {
        int scoAudioVssVol = getVssVolumeForDevice(6, 32);
        if (scoAudioVssVol > 0) {
            scoAudioVssVol /= 10;
        }
        if (DEBUG_VOL) {
            Log.d(TAG, "getLastHfpScoVolume() index=" + scoAudioVssVol);
        }
        return scoAudioVssVol;
    }

    public int rescaleCgVolumeIndexToHfpVolumeIndex(int index) {
        int rescaled = ((this.mStreamStates[6].getMaxIndex() * index) + (this.mStreamStates[0].getMaxIndex() / 2)) / this.mStreamStates[0].getMaxIndex();
        if (rescaled < this.mStreamStates[6].getMinIndex()) {
            rescaled = this.mStreamStates[6].getMinIndex();
        }
        if (DEBUG_VOL) {
            Log.d(TAG, "rescaleCgVolumeIndexToHfpVolumeIndex()" + index + "->" + rescaled);
        }
        this.mDeviceBroker.postSetVolumeIndexOnDevice(6, rescaled * 10, 32, "rescaleCgVolumeIndexToHfpVolumeIndex");
        return rescaled;
    }

    public boolean isBluetoothLeCgOn() {
        if (!getAudioServiceExtInstance().isBluetoothLeTbsDeviceActive()) {
            return false;
        }
        boolean mBleCgstatus = getAudioServiceExtInstance().isBluetoothLeCgOn();
        return mBleCgstatus;
    }

    public void setMicrophoneMuteDeathHandler(boolean on, String callingPackage, int userId, IBinder cb) {
        Log.d(TAG, "setMicrophoneDeathHandler on:" + on + " callingPackage = " + callingPackage + " cb:" + cb + " mSetMicrophoneMuteClient before= " + this.mSetMicrophoneMuteClient);
        synchronized (this.mSetMicrophoneDeathHandlerLock) {
            setMicrophoneMuteClient setmicrophonemuteclient = this.mSetMicrophoneMuteClient;
            if (setmicrophonemuteclient == null) {
                this.mSetMicrophoneMuteClient = new setMicrophoneMuteClient(cb);
            } else if (setmicrophonemuteclient.getBinder() == cb) {
                Log.d(TAG, "SetMicrophoneMuteClient cb:" + cb + " is already linked.");
            } else {
                this.mSetMicrophoneMuteClient.release();
                this.mSetMicrophoneMuteClient = new setMicrophoneMuteClient(cb);
            }
        }
    }

    /* loaded from: classes.dex */
    private class setMicrophoneMuteClient implements IBinder.DeathRecipient {
        private IBinder mCb;

        setMicrophoneMuteClient(IBinder cb) {
            if (cb != null) {
                try {
                    cb.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    Log.w(AudioService.TAG, "setMicrophoneMuteClient() could not link to " + cb + " binder death");
                    cb = null;
                }
            }
            this.mCb = cb;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (AudioService.this.mSetMicrophoneDeathHandlerLock) {
                Log.w(AudioService.TAG, "setMicrophoneMute client died ,check mic is muted mCb = " + this.mCb);
                if (AudioService.this.mSetMicrophoneMuteClient != this) {
                    Log.w(AudioService.TAG, "unregistered setMicrophoneMute client died");
                } else {
                    if (AudioService.this.mMicMuteFromApi && AudioService.this.isMicrophoneMuted()) {
                        Log.w(AudioService.TAG, "binderDied mic is muted,so set to unMute");
                        AudioService.this.mMicMuteFromApi = false;
                        AudioService audioService = AudioService.this;
                        audioService.setMicrophoneMuteNoCallerCheck(audioService.getCurrentUserId());
                    }
                    release();
                    AudioService.this.mSetMicrophoneMuteClient = null;
                }
            }
        }

        public void release() {
            IBinder iBinder = this.mCb;
            if (iBinder != null) {
                try {
                    iBinder.unlinkToDeath(this, 0);
                } catch (NoSuchElementException e) {
                    Log.w(AudioService.TAG, "setMicrophoneMuteClient link does not exist ...");
                }
                this.mCb = null;
            }
        }

        public IBinder getBinder() {
            return this.mCb;
        }
    }

    public String getCurrentAudioFocusPackageName() {
        return this.mMediaFocusControl.getCurrentAudioFocusPackageName();
    }

    public int getCurrentAudioFocusUid() {
        return this.mMediaFocusControl.getCurrentAudioFocusUid();
    }
}
