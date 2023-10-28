package android.media;

import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.app.compat.CompatChanges;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudioCodecConfig;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.CallbackUtil;
import android.media.IAudioFocusDispatcher;
import android.media.IAudioModeDispatcher;
import android.media.IAudioServerStateDispatcher;
import android.media.IAudioService;
import android.media.ICapturePresetDevicesRoleDispatcher;
import android.media.ICommunicationDeviceDispatcher;
import android.media.IMuteAwaitConnectionCallback;
import android.media.IPlaybackConfigDispatcher;
import android.media.IRecordingConfigDispatcher;
import android.media.IStrategyPreferredDevicesDispatcher;
import android.media.audiopolicy.AudioPolicy;
import android.media.audiopolicy.AudioVolumeGroupChangeHandler;
import android.media.projection.MediaProjection;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import com.android.internal.R;
import com.android.internal.util.Preconditions;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class AudioManager {
    public static final String ACTION_AUDIO_BECOMING_NOISY = "android.media.AUDIO_BECOMING_NOISY";
    public static final String ACTION_HDMI_AUDIO_PLUG = "android.media.action.HDMI_AUDIO_PLUG";
    public static final String ACTION_HEADSET_PLUG = "android.intent.action.HEADSET_PLUG";
    public static final String ACTION_MICROPHONE_MUTE_CHANGED = "android.media.action.MICROPHONE_MUTE_CHANGED";
    @Deprecated
    public static final String ACTION_SCO_AUDIO_STATE_CHANGED = "android.media.SCO_AUDIO_STATE_CHANGED";
    public static final String ACTION_SCO_AUDIO_STATE_UPDATED = "android.media.ACTION_SCO_AUDIO_STATE_UPDATED";
    public static final String ACTION_SPEAKERPHONE_STATE_CHANGED = "android.media.action.SPEAKERPHONE_STATE_CHANGED";
    @SystemApi
    public static final String ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION";
    public static final int ADJUST_LOWER = -1;
    public static final int ADJUST_MUTE = -100;
    public static final int ADJUST_RAISE = 1;
    public static final int ADJUST_SAME = 0;
    public static final int ADJUST_TOGGLE_MUTE = 101;
    public static final int ADJUST_UNMUTE = 100;
    public static final int AUDIOFOCUS_FLAGS_APPS = 3;
    public static final int AUDIOFOCUS_FLAGS_SYSTEM = 7;
    @SystemApi
    public static final int AUDIOFOCUS_FLAG_DELAY_OK = 1;
    @SystemApi
    public static final int AUDIOFOCUS_FLAG_LOCK = 4;
    @SystemApi
    public static final int AUDIOFOCUS_FLAG_PAUSES_ON_DUCKABLE_LOSS = 2;
    public static final int AUDIOFOCUS_FLAG_TEST = 8;
    public static final int AUDIOFOCUS_GAIN = 1;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT = 2;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE = 4;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK = 3;
    public static final int AUDIOFOCUS_LOSS = -1;
    public static final int AUDIOFOCUS_LOSS_TRANSIENT = -2;
    public static final int AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = -3;
    public static final int AUDIOFOCUS_NONE = 0;
    public static final int AUDIOFOCUS_REQUEST_DELAYED = 2;
    public static final int AUDIOFOCUS_REQUEST_FAILED = 0;
    public static final int AUDIOFOCUS_REQUEST_GRANTED = 1;
    public static final int AUDIOFOCUS_REQUEST_WAITING_FOR_EXT_POLICY = 100;
    static final int AUDIOPORT_GENERATION_INIT = 0;
    public static final int AUDIO_SESSION_ID_GENERATE = 0;
    public static final long CALL_REDIRECTION_AUDIO_MODES = 189472651;
    public static final int CALL_REDIRECT_NONE = 0;
    public static final int CALL_REDIRECT_PSTN = 1;
    public static final int CALL_REDIRECT_VOIP = 2;
    private static final boolean DEBUG;
    public static final int DEVICE_IN_ANLG_DOCK_HEADSET = -2147483136;
    public static final int DEVICE_IN_BACK_MIC = -2147483520;
    public static final int DEVICE_IN_BLE_HEADSET = -1610612736;
    public static final int DEVICE_IN_BLUETOOTH_SCO_HEADSET = -2147483640;
    public static final int DEVICE_IN_BUILTIN_MIC = -2147483644;
    public static final int DEVICE_IN_DGTL_DOCK_HEADSET = -2147482624;
    public static final int DEVICE_IN_ECHO_REFERENCE = -1879048192;
    public static final int DEVICE_IN_FM_TUNER = -2147475456;
    public static final int DEVICE_IN_HDMI = -2147483616;
    public static final int DEVICE_IN_HDMI_ARC = -2013265920;
    public static final int DEVICE_IN_HDMI_EARC = -2013265919;
    public static final int DEVICE_IN_LINE = -2147450880;
    public static final int DEVICE_IN_LOOPBACK = -2147221504;
    public static final int DEVICE_IN_SPDIF = -2147418112;
    public static final int DEVICE_IN_TELEPHONY_RX = -2147483584;
    public static final int DEVICE_IN_TV_TUNER = -2147467264;
    public static final int DEVICE_IN_USB_ACCESSORY = -2147481600;
    public static final int DEVICE_IN_USB_DEVICE = -2147479552;
    public static final int DEVICE_IN_WIRED_HEADSET = -2147483632;
    public static final int DEVICE_NONE = 0;
    public static final int DEVICE_OUT_ANLG_DOCK_HEADSET = 2048;
    public static final int DEVICE_OUT_AUX_DIGITAL = 1024;
    public static final int DEVICE_OUT_BLE_BROADCAST = 536870914;
    public static final int DEVICE_OUT_BLE_HEADSET = 536870912;
    public static final int DEVICE_OUT_BLE_SPEAKER = 536870913;
    public static final int DEVICE_OUT_BLUETOOTH_A2DP = 128;
    public static final int DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES = 256;
    public static final int DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER = 512;
    public static final int DEVICE_OUT_BLUETOOTH_SCO = 16;
    public static final int DEVICE_OUT_BLUETOOTH_SCO_CARKIT = 64;
    public static final int DEVICE_OUT_BLUETOOTH_SCO_HEADSET = 32;
    public static final int DEVICE_OUT_DEFAULT = 1073741824;
    public static final int DEVICE_OUT_DGTL_DOCK_HEADSET = 4096;
    public static final int DEVICE_OUT_EARPIECE = 1;
    public static final int DEVICE_OUT_ECHO_CANCELLER = 268435456;
    public static final int DEVICE_OUT_FM = 1048576;
    public static final int DEVICE_OUT_HDMI = 1024;
    public static final int DEVICE_OUT_HDMI_ARC = 262144;
    public static final int DEVICE_OUT_HDMI_EARC = 262145;
    public static final int DEVICE_OUT_LINE = 131072;
    public static final int DEVICE_OUT_REMOTE_SUBMIX = 32768;
    public static final int DEVICE_OUT_SPDIF = 524288;
    public static final int DEVICE_OUT_SPEAKER = 2;
    public static final int DEVICE_OUT_TELEPHONY_TX = 65536;
    public static final int DEVICE_OUT_USB_ACCESSORY = 8192;
    public static final int DEVICE_OUT_USB_DEVICE = 16384;
    public static final int DEVICE_OUT_USB_HEADSET = 67108864;
    public static final int DEVICE_OUT_WIRED_HEADPHONE = 8;
    public static final int DEVICE_OUT_WIRED_HEADSET = 4;
    @SystemApi
    public static final int DEVICE_VOLUME_BEHAVIOR_ABSOLUTE = 3;
    @SystemApi
    public static final int DEVICE_VOLUME_BEHAVIOR_ABSOLUTE_MULTI_MODE = 4;
    @SystemApi
    public static final int DEVICE_VOLUME_BEHAVIOR_FIXED = 2;
    @SystemApi
    public static final int DEVICE_VOLUME_BEHAVIOR_FULL = 1;
    public static final int DEVICE_VOLUME_BEHAVIOR_UNSET = -1;
    @SystemApi
    public static final int DEVICE_VOLUME_BEHAVIOR_VARIABLE = 0;
    public static final int DIRECT_PLAYBACK_BITSTREAM_SUPPORTED = 4;
    public static final int DIRECT_PLAYBACK_NOT_SUPPORTED = 0;
    public static final int DIRECT_PLAYBACK_OFFLOAD_GAPLESS_SUPPORTED = 3;
    public static final int DIRECT_PLAYBACK_OFFLOAD_SUPPORTED = 1;
    public static final int ENCODED_SURROUND_OUTPUT_ALWAYS = 2;
    public static final int ENCODED_SURROUND_OUTPUT_AUTO = 0;
    public static final int ENCODED_SURROUND_OUTPUT_MANUAL = 3;
    public static final int ENCODED_SURROUND_OUTPUT_NEVER = 1;
    public static final int ENCODED_SURROUND_OUTPUT_UNKNOWN = -1;
    public static final int ERROR = -1;
    public static final int ERROR_BAD_VALUE = -2;
    public static final int ERROR_DEAD_OBJECT = -6;
    public static final int ERROR_INVALID_OPERATION = -3;
    public static final int ERROR_NO_INIT = -5;
    public static final int ERROR_PERMISSION_DENIED = -4;
    public static final String EXTRA_AUDIO_PLUG_STATE = "android.media.extra.AUDIO_PLUG_STATE";
    public static final String EXTRA_ENCODINGS = "android.media.extra.ENCODINGS";
    public static final String EXTRA_MASTER_VOLUME_MUTED = "android.media.EXTRA_MASTER_VOLUME_MUTED";
    public static final String EXTRA_MAX_CHANNEL_COUNT = "android.media.extra.MAX_CHANNEL_COUNT";
    public static final String EXTRA_PREV_VOLUME_STREAM_DEVICES = "android.media.EXTRA_PREV_VOLUME_STREAM_DEVICES";
    public static final String EXTRA_PREV_VOLUME_STREAM_VALUE = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE";
    public static final String EXTRA_RINGER_MODE = "android.media.EXTRA_RINGER_MODE";
    public static final String EXTRA_SCO_AUDIO_PREVIOUS_STATE = "android.media.extra.SCO_AUDIO_PREVIOUS_STATE";
    public static final String EXTRA_SCO_AUDIO_STATE = "android.media.extra.SCO_AUDIO_STATE";
    public static final String EXTRA_STREAM_VOLUME_MUTED = "android.media.EXTRA_STREAM_VOLUME_MUTED";
    public static final String EXTRA_VIBRATE_SETTING = "android.media.EXTRA_VIBRATE_SETTING";
    public static final String EXTRA_VIBRATE_TYPE = "android.media.EXTRA_VIBRATE_TYPE";
    public static final String EXTRA_VOLUME_STREAM_DEVICES = "android.media.EXTRA_VOLUME_STREAM_DEVICES";
    @SystemApi
    public static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    public static final String EXTRA_VOLUME_STREAM_TYPE_ALIAS = "android.media.EXTRA_VOLUME_STREAM_TYPE_ALIAS";
    @SystemApi
    public static final String EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";
    private static final int EXT_FOCUS_POLICY_TIMEOUT_MS = 200;
    public static final int FLAG_ABSOLUTE_VOLUME = 8192;
    public static final int FLAG_ACTIVE_MEDIA_ONLY = 512;
    public static final int FLAG_ALLOW_RINGER_MODES = 2;
    @SystemApi
    public static final int FLAG_BLUETOOTH_ABS_VOLUME = 64;
    public static final int FLAG_FIXED_VOLUME = 32;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int FLAG_FROM_KEY = 4096;
    public static final int FLAG_HDMI_SYSTEM_AUDIO_VOLUME = 256;
    private static final TreeMap<Integer, String> FLAG_NAMES;
    public static final int FLAG_PLAY_SOUND = 4;
    public static final int FLAG_REMOVE_SOUND_AND_VIBRATE = 8;
    public static final int FLAG_SHOW_SILENT_HINT = 128;
    public static final int FLAG_SHOW_UI = 1;
    public static final int FLAG_SHOW_UI_WARNINGS = 1024;
    public static final int FLAG_SHOW_VIBRATE_HINT = 2048;
    public static final int FLAG_VIBRATE = 16;
    private static final String FOCUS_CLIENT_ID_STRING = "android_audio_focus_client_id";
    public static final int FX_BACK = 10;
    public static final int FX_FOCUS_NAVIGATION_DOWN = 2;
    public static final int FX_FOCUS_NAVIGATION_LEFT = 3;
    public static final int FX_FOCUS_NAVIGATION_REPEAT_1 = 12;
    public static final int FX_FOCUS_NAVIGATION_REPEAT_2 = 13;
    public static final int FX_FOCUS_NAVIGATION_REPEAT_3 = 14;
    public static final int FX_FOCUS_NAVIGATION_REPEAT_4 = 15;
    public static final int FX_FOCUS_NAVIGATION_RIGHT = 4;
    public static final int FX_FOCUS_NAVIGATION_UP = 1;
    public static final int FX_HOME = 11;
    public static final int FX_KEYPRESS_DELETE = 7;
    public static final int FX_KEYPRESS_INVALID = 9;
    public static final int FX_KEYPRESS_RETURN = 8;
    public static final int FX_KEYPRESS_SPACEBAR = 6;
    public static final int FX_KEYPRESS_STANDARD = 5;
    public static final int FX_KEY_CLICK = 0;
    public static final int GET_DEVICES_ALL = 3;
    public static final int GET_DEVICES_INPUTS = 1;
    public static final int GET_DEVICES_OUTPUTS = 2;
    public static final String INTERNAL_RINGER_MODE_CHANGED_ACTION = "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION";
    public static final String MASTER_MUTE_CHANGED_ACTION = "android.media.MASTER_MUTE_CHANGED_ACTION";
    public static final int MODE_CALL_REDIRECT = 5;
    public static final int MODE_CALL_SCREENING = 4;
    public static final int MODE_COMMUNICATION_REDIRECT = 6;
    public static final int MODE_CURRENT = -1;
    public static final int MODE_INVALID = -2;
    public static final int MODE_IN_CALL = 2;
    public static final int MODE_IN_COMMUNICATION = 3;
    public static final int MODE_NORMAL = 0;
    public static final int MODE_RINGTONE = 1;
    private static final int MSG_DEVICES_CALLBACK_REGISTERED = 0;
    private static final int MSG_DEVICES_DEVICES_ADDED = 1;
    private static final int MSG_DEVICES_DEVICES_REMOVED = 2;
    private static final int MSSG_FOCUS_CHANGE = 0;
    private static final int MSSG_PLAYBACK_CONFIG_CHANGE = 2;
    private static final int MSSG_RECORDING_CONFIG_CHANGE = 1;
    public static final int NUM_NAVIGATION_REPEAT_SOUND_EFFECTS = 4;
    public static final int NUM_SOUND_EFFECTS = 16;
    @Deprecated
    public static final int NUM_STREAMS = 5;
    public static final int PLAYBACK_OFFLOAD_GAPLESS_SUPPORTED = 2;
    public static final int PLAYBACK_OFFLOAD_NOT_SUPPORTED = 0;
    public static final int PLAYBACK_OFFLOAD_SUPPORTED = 1;
    public static final String PROPERTY_OUTPUT_FRAMES_PER_BUFFER = "android.media.property.OUTPUT_FRAMES_PER_BUFFER";
    public static final String PROPERTY_OUTPUT_SAMPLE_RATE = "android.media.property.OUTPUT_SAMPLE_RATE";
    public static final String PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED = "android.media.property.SUPPORT_AUDIO_SOURCE_UNPROCESSED";
    public static final String PROPERTY_SUPPORT_MIC_NEAR_ULTRASOUND = "android.media.property.SUPPORT_MIC_NEAR_ULTRASOUND";
    public static final String PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND = "android.media.property.SUPPORT_SPEAKER_NEAR_ULTRASOUND";
    private static final int[] PUBLIC_STREAM_TYPES;
    public static final int RECORDER_STATE_STARTED = 0;
    public static final int RECORDER_STATE_STOPPED = 1;
    public static final int RECORD_CONFIG_EVENT_NONE = -1;
    public static final int RECORD_CONFIG_EVENT_RELEASE = 3;
    public static final int RECORD_CONFIG_EVENT_START = 0;
    public static final int RECORD_CONFIG_EVENT_STOP = 1;
    public static final int RECORD_CONFIG_EVENT_UPDATE = 2;
    public static final int RECORD_RIID_INVALID = -1;
    public static final String RINGER_MODE_CHANGED_ACTION = "android.media.RINGER_MODE_CHANGED";
    public static final int RINGER_MODE_MAX = 2;
    public static final int RINGER_MODE_NORMAL = 2;
    public static final int RINGER_MODE_SILENT = 0;
    public static final int RINGER_MODE_VIBRATE = 1;
    @Deprecated
    public static final int ROUTE_ALL = -1;
    @Deprecated
    public static final int ROUTE_BLUETOOTH = 4;
    @Deprecated
    public static final int ROUTE_BLUETOOTH_A2DP = 16;
    @Deprecated
    public static final int ROUTE_BLUETOOTH_SCO = 4;
    @Deprecated
    public static final int ROUTE_EARPIECE = 1;
    @Deprecated
    public static final int ROUTE_HEADSET = 8;
    @Deprecated
    public static final int ROUTE_SPEAKER = 2;
    public static final int SCO_AUDIO_STATE_CONNECTED = 1;
    public static final int SCO_AUDIO_STATE_CONNECTING = 2;
    public static final int SCO_AUDIO_STATE_DISCONNECTED = 0;
    public static final int SCO_AUDIO_STATE_ERROR = -1;
    public static final int STREAM_ACCESSIBILITY = 10;
    public static final int STREAM_ALARM = 4;
    @SystemApi
    public static final int STREAM_ASSISTANT = 11;
    @SystemApi
    public static final int STREAM_BLUETOOTH_SCO = 6;
    public static final String STREAM_DEVICES_CHANGED_ACTION = "android.media.STREAM_DEVICES_CHANGED_ACTION";
    public static final int STREAM_DTMF = 8;
    public static final int STREAM_MUSIC = 3;
    public static final String STREAM_MUTE_CHANGED_ACTION = "android.media.STREAM_MUTE_CHANGED_ACTION";
    public static final int STREAM_NOTIFICATION = 5;
    public static final int STREAM_RING = 2;
    public static final int STREAM_SYSTEM = 1;
    public static final int STREAM_SYSTEM_ENFORCED = 7;
    public static final int STREAM_TTS = 9;
    public static final int STREAM_VOICE_CALL = 0;
    @SystemApi
    public static final int SUCCESS = 0;
    private static final String TAG = "AudioManager";
    public static final int USE_DEFAULT_STREAM_TYPE = Integer.MIN_VALUE;
    public static final String VIBRATE_SETTING_CHANGED_ACTION = "android.media.VIBRATE_SETTING_CHANGED";
    public static final int VIBRATE_SETTING_OFF = 0;
    public static final int VIBRATE_SETTING_ON = 1;
    public static final int VIBRATE_SETTING_ONLY_SILENT = 2;
    public static final int VIBRATE_TYPE_NOTIFICATION = 1;
    public static final int VIBRATE_TYPE_RINGER = 0;
    public static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private static final float VOLUME_MIN_DB = -758.0f;
    private static final AudioVolumeGroupChangeHandler sAudioAudioVolumeGroupChangedHandler;
    static ArrayList<AudioPatch> sAudioPatchesCached;
    private static final AudioPortEventHandler sAudioPortEventHandler;
    static Integer sAudioPortGeneration;
    static ArrayList<AudioPort> sAudioPortsCached;
    private static WeakReference<Context> sContext;
    static ArrayList<AudioPort> sPreviousAudioPortsCached;
    private static IAudioService sService;
    private Context mApplicationContext;
    private AudioServerStateCallback mAudioServerStateCb;
    private Executor mAudioServerStateExec;
    private ArrayList<CallIRedirectionClientInfo> mCallIRedirectionClients;
    private CallInjectionModeChangedListener mCallRedirectionModeListener;
    private CapturePresetDevicesRoleDispatcherStub mDevicesRoleForCapturePresetDispatcherStub;
    private HashMap<String, BlockingFocusResultReceiver> mFocusRequestsAwaitingResult;
    private MuteAwaitConnectionDispatcherStub mMuteAwaitConnDispatcherStub;
    private ArrayList<CallbackUtil.ListenerInfo<MuteAwaitConnectionCallback>> mMuteAwaitConnectionListeners;
    private Context mOriginalContext;
    private List<AudioPlaybackCallbackInfo> mPlaybackCallbackList;
    private StrategyPreferredDevicesDispatcherStub mPrefDevDispatcherStub;
    private ArrayList<PrefDevListenerInfo> mPrefDevListeners;
    private List<AudioRecordingCallbackInfo> mRecordCallbackList;
    private long mVolumeKeyUpTime;
    private final Object mPrefDevListenerLock = new Object();
    private final Map<Integer, Object> mDevRoleForCapturePresetListeners = new HashMap<Integer, Object>() { // from class: android.media.AudioManager.1
        {
            put(1, new DevRoleListeners());
        }
    };
    private final Object mDevRoleForCapturePresetListenersLock = new Object();
    private int mDeviceRoleListenersStatus = 0;
    private final CallbackUtil.LazyListenerManager<OnModeChangedListener> mModeChangedListenerMgr = new CallbackUtil.LazyListenerManager<>();
    private final ConcurrentHashMap<String, FocusRequestInfo> mAudioFocusIdListenerMap = new ConcurrentHashMap<>();
    private final ServiceEventHandlerDelegate mServiceEventHandlerDelegate = new ServiceEventHandlerDelegate(null);
    private final IAudioFocusDispatcher mAudioFocusDispatcher = new IAudioFocusDispatcher.Stub() { // from class: android.media.AudioManager.2
        @Override // android.media.IAudioFocusDispatcher
        public void dispatchAudioFocusChange(int focusChange, String id) {
            FocusRequestInfo fri = AudioManager.this.findFocusRequestInfo(id);
            if (fri != null) {
                OnAudioFocusChangeListener listener = fri.mRequest.getOnAudioFocusChangeListener();
                if (listener != null) {
                    Handler h = fri.mHandler == null ? AudioManager.this.mServiceEventHandlerDelegate.getHandler() : fri.mHandler;
                    Message m = h.obtainMessage(0, focusChange, 0, id);
                    h.sendMessage(m);
                }
            }
        }

        @Override // android.media.IAudioFocusDispatcher
        public void dispatchFocusResultFromExtPolicy(int requestResult, String clientId) {
            synchronized (AudioManager.this.mFocusRequestsLock) {
                BlockingFocusResultReceiver focusReceiver = (BlockingFocusResultReceiver) AudioManager.this.mFocusRequestsAwaitingResult.remove(clientId);
                if (focusReceiver != null) {
                    focusReceiver.notifyResult(requestResult);
                } else {
                    Log.e(AudioManager.TAG, "dispatchFocusResultFromExtPolicy found no result receiver");
                }
            }
        }
    };
    private final Object mFocusRequestsLock = new Object();
    private final Object mPlaybackCallbackLock = new Object();
    private final IPlaybackConfigDispatcher mPlayCb = new IPlaybackConfigDispatcher.Stub() { // from class: android.media.AudioManager.3
        @Override // android.media.IPlaybackConfigDispatcher
        public void dispatchPlaybackConfigChange(List<AudioPlaybackConfiguration> configs, boolean flush) {
            if (flush) {
                Binder.flushPendingCommands();
            }
            synchronized (AudioManager.this.mPlaybackCallbackLock) {
                if (AudioManager.this.mPlaybackCallbackList != null) {
                    for (int i = 0; i < AudioManager.this.mPlaybackCallbackList.size(); i++) {
                        AudioPlaybackCallbackInfo arci = (AudioPlaybackCallbackInfo) AudioManager.this.mPlaybackCallbackList.get(i);
                        if (arci.mHandler != null) {
                            Message m = arci.mHandler.obtainMessage(2, new PlaybackConfigChangeCallbackData(arci.mCb, configs));
                            arci.mHandler.sendMessage(m);
                        }
                    }
                }
            }
        }
    };
    private final Object mRecordCallbackLock = new Object();
    private final IRecordingConfigDispatcher mRecCb = new IRecordingConfigDispatcher.Stub() { // from class: android.media.AudioManager.4
        @Override // android.media.IRecordingConfigDispatcher
        public void dispatchRecordingConfigChange(List<AudioRecordingConfiguration> configs) {
            synchronized (AudioManager.this.mRecordCallbackLock) {
                if (AudioManager.this.mRecordCallbackList != null) {
                    for (int i = 0; i < AudioManager.this.mRecordCallbackList.size(); i++) {
                        AudioRecordingCallbackInfo arci = (AudioRecordingCallbackInfo) AudioManager.this.mRecordCallbackList.get(i);
                        if (arci.mHandler != null) {
                            Message m = arci.mHandler.obtainMessage(1, new RecordConfigChangeCallbackData(arci.mCb, configs));
                            arci.mHandler.sendMessage(m);
                        }
                    }
                }
            }
        }
    };
    private final IBinder mICallBack = new Binder();
    private OnAmPortUpdateListener mPortListener = null;
    private final ArrayMap<AudioDeviceCallback, NativeEventHandlerDelegate> mDeviceCallbacks = new ArrayMap<>();
    private ArrayList<AudioDevicePort> mPreviousPorts = new ArrayList<>();
    private final Object mAudioServerStateCbLock = new Object();
    private final IAudioServerStateDispatcher mAudioServerStateDispatcher = new AnonymousClass5();
    private final CallbackUtil.LazyListenerManager<OnCommunicationDeviceChangedListener> mCommDeviceChangedListenerMgr = new CallbackUtil.LazyListenerManager<>();
    private Object mCallRedirectionLock = new Object();
    private final Object mMuteAwaitConnectionListenerLock = new Object();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface AudioDeviceRole {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface AudioDirectPlaybackMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface AudioMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface AudioOffloadMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface CallRedirectionMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface DeviceVolumeBehavior {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface DeviceVolumeBehaviorState {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface EncodedSurroundOutputMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface Flags {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface FocusRequestResult {
    }

    /* loaded from: classes2.dex */
    public interface OnAudioFocusChangeListener {
        void onAudioFocusChange(int i);
    }

    /* loaded from: classes2.dex */
    public interface OnAudioPortUpdateListener {
        void onAudioPatchListUpdate(AudioPatch[] audioPatchArr);

        void onAudioPortListUpdate(AudioPort[] audioPortArr);

        void onServiceDied();
    }

    /* loaded from: classes2.dex */
    public interface OnCommunicationDeviceChangedListener {
        void onCommunicationDeviceChanged(AudioDeviceInfo audioDeviceInfo);
    }

    /* loaded from: classes2.dex */
    public interface OnModeChangedListener {
        void onModeChanged(int i);
    }

    @SystemApi
    @Deprecated
    /* loaded from: classes2.dex */
    public interface OnPreferredDeviceForStrategyChangedListener {
        void onPreferredDeviceForStrategyChanged(android.media.audiopolicy.AudioProductStrategy audioProductStrategy, AudioDeviceAttributes audioDeviceAttributes);
    }

    @SystemApi
    /* loaded from: classes2.dex */
    public interface OnPreferredDevicesForCapturePresetChangedListener {
        void onPreferredDevicesForCapturePresetChanged(int i, List<AudioDeviceAttributes> list);
    }

    @SystemApi
    /* loaded from: classes2.dex */
    public interface OnPreferredDevicesForStrategyChangedListener {
        void onPreferredDevicesForStrategyChanged(android.media.audiopolicy.AudioProductStrategy audioProductStrategy, List<AudioDeviceAttributes> list);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface PublicStreamTypes {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface SystemSoundEffect {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface VolumeAdjustment {
    }

    static {
        DEBUG = "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE) || "1".equals(SystemProperties.get("persist.user.root.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS)) || "1".equals(SystemProperties.get("persist.sys.fans.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS));
        sAudioPortEventHandler = new AudioPortEventHandler();
        sAudioAudioVolumeGroupChangedHandler = new AudioVolumeGroupChangeHandler();
        PUBLIC_STREAM_TYPES = new int[]{0, 1, 2, 3, 4, 5, 8, 10};
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        FLAG_NAMES = treeMap;
        treeMap.put(1, "FLAG_SHOW_UI");
        treeMap.put(2, "FLAG_ALLOW_RINGER_MODES");
        treeMap.put(4, "FLAG_PLAY_SOUND");
        treeMap.put(8, "FLAG_REMOVE_SOUND_AND_VIBRATE");
        treeMap.put(16, "FLAG_VIBRATE");
        treeMap.put(32, "FLAG_FIXED_VOLUME");
        treeMap.put(64, "FLAG_BLUETOOTH_ABS_VOLUME");
        treeMap.put(128, "FLAG_SHOW_SILENT_HINT");
        treeMap.put(256, "FLAG_HDMI_SYSTEM_AUDIO_VOLUME");
        treeMap.put(512, "FLAG_ACTIVE_MEDIA_ONLY");
        treeMap.put(1024, "FLAG_SHOW_UI_WARNINGS");
        treeMap.put(2048, "FLAG_SHOW_VIBRATE_HINT");
        treeMap.put(4096, "FLAG_FROM_KEY");
        treeMap.put(8192, "FLAG_ABSOLUTE_VOLUME");
        sAudioPortGeneration = new Integer(0);
        sAudioPortsCached = new ArrayList<>();
        sPreviousAudioPortsCached = new ArrayList<>();
        sAudioPatchesCached = new ArrayList<>();
    }

    public static final int[] getPublicStreamTypes() {
        return PUBLIC_STREAM_TYPES;
    }

    public static final String adjustToString(int adj) {
        switch (adj) {
            case -100:
                return "ADJUST_MUTE";
            case -1:
                return "ADJUST_LOWER";
            case 0:
                return "ADJUST_SAME";
            case 1:
                return "ADJUST_RAISE";
            case 100:
                return "ADJUST_UNMUTE";
            case 101:
                return "ADJUST_TOGGLE_MUTE";
            default:
                return "unknown adjust mode " + adj;
        }
    }

    public static String flagsToString(int flags) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, String> entry : FLAG_NAMES.entrySet()) {
            int flag = entry.getKey().intValue();
            if ((flags & flag) != 0) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(entry.getValue());
                flags &= ~flag;
            }
        }
        if (flags != 0) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(flags);
        }
        return sb.toString();
    }

    public AudioManager() {
    }

    public AudioManager(Context context) {
        setContext(context);
    }

    private Context getContext() {
        if (this.mApplicationContext == null) {
            setContext(this.mOriginalContext);
        }
        Context context = this.mApplicationContext;
        if (context != null) {
            return context;
        }
        return this.mOriginalContext;
    }

    private void setContext(Context context) {
        Context applicationContext = context.getApplicationContext();
        this.mApplicationContext = applicationContext;
        if (applicationContext != null) {
            this.mOriginalContext = null;
        } else {
            this.mOriginalContext = context;
        }
        sContext = new WeakReference<>(context);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static IAudioService getService() {
        IAudioService iAudioService = sService;
        if (iAudioService != null) {
            return iAudioService;
        }
        IBinder b = ServiceManager.getService("audio");
        IAudioService asInterface = IAudioService.Stub.asInterface(b);
        sService = asInterface;
        return asInterface;
    }

    public void dispatchMediaKeyEvent(KeyEvent keyEvent) {
        MediaSessionLegacyHelper helper = MediaSessionLegacyHelper.getHelper(getContext());
        helper.sendMediaButtonEvent(keyEvent, false);
    }

    public void preDispatchKeyEvent(KeyEvent event, int stream) {
        int keyCode = event.getKeyCode();
        if (keyCode != 25 && keyCode != 24 && keyCode != 164 && this.mVolumeKeyUpTime + 300 > SystemClock.uptimeMillis()) {
            adjustSuggestedStreamVolume(0, stream, 8);
        }
    }

    public boolean isVolumeFixed() {
        try {
            boolean res = getService().isVolumeFixed();
            return res;
        } catch (RemoteException e) {
            Log.e(TAG, "Error querying isVolumeFixed", e);
            return false;
        }
    }

    public void adjustStreamVolume(int streamType, int direction, int flags) {
        IAudioService service = getService();
        try {
            service.adjustStreamVolumeWithAttribution(streamType, direction, flags, getContext().getOpPackageName(), getContext().getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void adjustVolume(int direction, int flags) {
        MediaSessionLegacyHelper helper = MediaSessionLegacyHelper.getHelper(getContext());
        helper.sendAdjustVolumeBy(Integer.MIN_VALUE, direction, flags);
    }

    public void adjustSuggestedStreamVolume(int direction, int suggestedStreamType, int flags) {
        MediaSessionLegacyHelper helper = MediaSessionLegacyHelper.getHelper(getContext());
        helper.sendAdjustVolumeBy(suggestedStreamType, direction, flags);
    }

    public void setMasterMute(boolean mute, int flags) {
        IAudioService service = getService();
        try {
            service.setMasterMute(mute, flags, getContext().getOpPackageName(), UserHandle.getCallingUserId(), getContext().getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getRingerMode() {
        IAudioService service = getService();
        try {
            return service.getRingerModeExternal();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isRampingRingerEnabled() {
        return Settings.System.getInt(getContext().getContentResolver(), "apply_ramping_ringer", 0) != 0;
    }

    public void setRampingRingerEnabled(boolean enabled) {
        Settings.System.putInt(getContext().getContentResolver(), "apply_ramping_ringer", enabled ? 1 : 0);
    }

    public static boolean isValidRingerMode(int ringerMode) {
        if (ringerMode < 0 || ringerMode > 2) {
            return false;
        }
        IAudioService service = getService();
        try {
            return service.isValidRingerMode(ringerMode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getStreamMaxVolume(int streamType) {
        IAudioService service = getService();
        try {
            return service.getStreamMaxVolume(streamType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getStreamMinVolume(int streamType) {
        if (!isPublicStreamType(streamType)) {
            throw new IllegalArgumentException("Invalid stream type " + streamType);
        }
        return getStreamMinVolumeInt(streamType);
    }

    public int getStreamMinVolumeInt(int streamType) {
        IAudioService service = getService();
        try {
            return service.getStreamMinVolume(streamType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getStreamVolume(int streamType) {
        IAudioService service = getService();
        try {
            return service.getStreamVolume(streamType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public float getStreamVolumeDb(int streamType, int index, int deviceType) {
        if (!isPublicStreamType(streamType)) {
            throw new IllegalArgumentException("Invalid stream type " + streamType);
        }
        if (index > getStreamMaxVolume(streamType) || index < getStreamMinVolume(streamType)) {
            throw new IllegalArgumentException("Invalid stream volume index " + index);
        }
        if (!AudioDeviceInfo.isValidAudioDeviceTypeOut(deviceType)) {
            throw new IllegalArgumentException("Invalid audio output device type " + deviceType);
        }
        float gain = AudioSystem.getStreamVolumeDB(streamType, index, AudioDeviceInfo.convertDeviceTypeToInternalDevice(deviceType));
        if (gain <= VOLUME_MIN_DB) {
            return Float.NEGATIVE_INFINITY;
        }
        return gain;
    }

    private static boolean isPublicStreamType(int streamType) {
        switch (streamType) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 8:
            case 10:
                return true;
            case 6:
            case 7:
            case 9:
            default:
                return false;
        }
    }

    @SystemApi
    public int getLastAudibleStreamVolume(int streamType) {
        IAudioService service = getService();
        try {
            return service.getLastAudibleStreamVolume(streamType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getUiSoundsStreamType() {
        IAudioService service = getService();
        try {
            return service.getUiSoundsStreamType();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setRingerMode(int ringerMode) {
        if (!isValidRingerMode(ringerMode)) {
            return;
        }
        IAudioService service = getService();
        try {
            service.setRingerModeExternal(ringerMode, getContext().getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setStreamVolume(int streamType, int index, int flags) {
        IAudioService service = getService();
        try {
            service.setStreamVolumeWithAttribution(streamType, index, flags, getContext().getOpPackageName(), getContext().getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setVolumeIndexForAttributes(AudioAttributes attr, int index, int flags) {
        Preconditions.checkNotNull(attr, "attr must not be null");
        IAudioService service = getService();
        try {
            service.setVolumeIndexForAttributes(attr, index, flags, getContext().getOpPackageName(), getContext().getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int getVolumeIndexForAttributes(AudioAttributes attr) {
        Preconditions.checkNotNull(attr, "attr must not be null");
        IAudioService service = getService();
        try {
            return service.getVolumeIndexForAttributes(attr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int getMaxVolumeIndexForAttributes(AudioAttributes attr) {
        Preconditions.checkNotNull(attr, "attr must not be null");
        IAudioService service = getService();
        try {
            return service.getMaxVolumeIndexForAttributes(attr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int getMinVolumeIndexForAttributes(AudioAttributes attr) {
        Preconditions.checkNotNull(attr, "attr must not be null");
        IAudioService service = getService();
        try {
            return service.getMinVolumeIndexForAttributes(attr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setSupportedSystemUsages(int[] systemUsages) {
        Objects.requireNonNull(systemUsages, "systemUsages must not be null");
        IAudioService service = getService();
        try {
            service.setSupportedSystemUsages(systemUsages);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int[] getSupportedSystemUsages() {
        IAudioService service = getService();
        try {
            return service.getSupportedSystemUsages();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void setStreamSolo(int streamType, boolean state) {
        Log.w(TAG, "setStreamSolo has been deprecated. Do not use.");
    }

    @Deprecated
    public void setStreamMute(int streamType, boolean state) {
        Log.w(TAG, "setStreamMute is deprecated. adjustStreamVolume should be used instead.");
        int direction = state ? -100 : 100;
        if (streamType == Integer.MIN_VALUE) {
            adjustSuggestedStreamVolume(direction, streamType, 0);
        } else {
            adjustStreamVolume(streamType, direction, 0);
        }
    }

    public boolean isStreamMute(int streamType) {
        IAudioService service = getService();
        try {
            return service.isStreamMute(streamType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isMasterMute() {
        IAudioService service = getService();
        try {
            return service.isMasterMute();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void forceVolumeControlStream(int streamType) {
        IAudioService service = getService();
        try {
            service.forceVolumeControlStream(streamType, this.mICallBack);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean shouldVibrate(int vibrateType) {
        IAudioService service = getService();
        try {
            return service.shouldVibrate(vibrateType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getVibrateSetting(int vibrateType) {
        IAudioService service = getService();
        try {
            return service.getVibrateSetting(vibrateType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setVibrateSetting(int vibrateType, int vibrateSetting) {
        IAudioService service = getService();
        try {
            service.setVibrateSetting(vibrateType, vibrateSetting);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setSpeakerphoneOn(boolean on) {
        if (DEBUG) {
            Log.d(TAG, "setSpeakerphoneOn(" + on + NavigationBarInflaterView.KEY_CODE_END);
        }
        IAudioService service = getService();
        try {
            service.setSpeakerphoneOn(this.mICallBack, on);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSpeakerphoneOn() {
        IAudioService service = getService();
        try {
            return service.isSpeakerphoneOn();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setAllowedCapturePolicy(int capturePolicy) {
        IAudioService service = getService();
        try {
            int result = service.setAllowedCapturePolicy(capturePolicy);
            if (result != 0) {
                Log.e(TAG, "Could not setAllowedCapturePolicy: " + result);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getAllowedCapturePolicy() {
        try {
            int result = getService().getAllowedCapturePolicy();
            return result;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to query allowed capture policy: " + e);
            return 1;
        }
    }

    @SystemApi
    public boolean setPreferredDeviceForStrategy(android.media.audiopolicy.AudioProductStrategy strategy, AudioDeviceAttributes device) {
        return setPreferredDevicesForStrategy(strategy, Arrays.asList(device));
    }

    @SystemApi
    public boolean removePreferredDeviceForStrategy(android.media.audiopolicy.AudioProductStrategy strategy) {
        Objects.requireNonNull(strategy);
        try {
            int status = getService().removePreferredDevicesForStrategy(strategy.getId());
            return status == 0;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public AudioDeviceAttributes getPreferredDeviceForStrategy(android.media.audiopolicy.AudioProductStrategy strategy) {
        List<AudioDeviceAttributes> devices = getPreferredDevicesForStrategy(strategy);
        if (devices.isEmpty()) {
            return null;
        }
        return devices.get(0);
    }

    @SystemApi
    public boolean setPreferredDevicesForStrategy(android.media.audiopolicy.AudioProductStrategy strategy, List<AudioDeviceAttributes> devices) {
        Objects.requireNonNull(strategy);
        Objects.requireNonNull(devices);
        if (devices.isEmpty()) {
            throw new IllegalArgumentException("Tried to set preferred devices for strategy with a empty list");
        }
        for (AudioDeviceAttributes device : devices) {
            Objects.requireNonNull(device);
        }
        try {
            int status = getService().setPreferredDevicesForStrategy(strategy.getId(), devices);
            return status == 0;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public List<AudioDeviceAttributes> getPreferredDevicesForStrategy(android.media.audiopolicy.AudioProductStrategy strategy) {
        Objects.requireNonNull(strategy);
        try {
            return getService().getPreferredDevicesForStrategy(strategy.getId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    @Deprecated
    public void addOnPreferredDeviceForStrategyChangedListener(Executor executor, OnPreferredDeviceForStrategyChangedListener listener) throws SecurityException {
    }

    @SystemApi
    @Deprecated
    public void removeOnPreferredDeviceForStrategyChangedListener(OnPreferredDeviceForStrategyChangedListener listener) {
    }

    @SystemApi
    public void addOnPreferredDevicesForStrategyChangedListener(Executor executor, OnPreferredDevicesForStrategyChangedListener listener) throws SecurityException {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(listener);
        synchronized (this.mPrefDevListenerLock) {
            if (hasPrefDevListener(listener)) {
                throw new IllegalArgumentException("attempt to call addOnPreferredDevicesForStrategyChangedListener() on a previously registered listener");
            }
            if (this.mPrefDevListeners == null) {
                this.mPrefDevListeners = new ArrayList<>();
            }
            int oldCbCount = this.mPrefDevListeners.size();
            this.mPrefDevListeners.add(new PrefDevListenerInfo(listener, executor));
            if (oldCbCount == 0 && this.mPrefDevListeners.size() > 0) {
                if (this.mPrefDevDispatcherStub == null) {
                    this.mPrefDevDispatcherStub = new StrategyPreferredDevicesDispatcherStub();
                }
                try {
                    getService().registerStrategyPreferredDevicesDispatcher(this.mPrefDevDispatcherStub);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    @SystemApi
    public void removeOnPreferredDevicesForStrategyChangedListener(OnPreferredDevicesForStrategyChangedListener listener) {
        Objects.requireNonNull(listener);
        synchronized (this.mPrefDevListenerLock) {
            if (!removePrefDevListener(listener)) {
                throw new IllegalArgumentException("attempt to call removeOnPreferredDeviceForStrategyChangedListener() on an unregistered listener");
            }
            if (this.mPrefDevListeners.size() == 0) {
                try {
                    getService().unregisterStrategyPreferredDevicesDispatcher(this.mPrefDevDispatcherStub);
                    this.mPrefDevDispatcherStub = null;
                    this.mPrefDevListeners = null;
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class PrefDevListenerInfo {
        final Executor mExecutor;
        final OnPreferredDevicesForStrategyChangedListener mListener;

        PrefDevListenerInfo(OnPreferredDevicesForStrategyChangedListener listener, Executor exe) {
            this.mListener = listener;
            this.mExecutor = exe;
        }
    }

    /* loaded from: classes2.dex */
    private final class StrategyPreferredDevicesDispatcherStub extends IStrategyPreferredDevicesDispatcher.Stub {
        private StrategyPreferredDevicesDispatcherStub() {
        }

        @Override // android.media.IStrategyPreferredDevicesDispatcher
        public void dispatchPrefDevicesChanged(int strategyId, final List<AudioDeviceAttributes> devices) {
            synchronized (AudioManager.this.mPrefDevListenerLock) {
                if (AudioManager.this.mPrefDevListeners != null && AudioManager.this.mPrefDevListeners.size() != 0) {
                    ArrayList<PrefDevListenerInfo> prefDevListeners = (ArrayList) AudioManager.this.mPrefDevListeners.clone();
                    final android.media.audiopolicy.AudioProductStrategy strategy = android.media.audiopolicy.AudioProductStrategy.getAudioProductStrategyWithId(strategyId);
                    long ident = Binder.clearCallingIdentity();
                    try {
                        Iterator<PrefDevListenerInfo> it = prefDevListeners.iterator();
                        while (it.hasNext()) {
                            final PrefDevListenerInfo info = it.next();
                            info.mExecutor.execute(new Runnable() { // from class: android.media.AudioManager$StrategyPreferredDevicesDispatcherStub$$ExternalSyntheticLambda0
                                @Override // java.lang.Runnable
                                public final void run() {
                                    AudioManager.PrefDevListenerInfo.this.mListener.onPreferredDevicesForStrategyChanged(strategy, devices);
                                }
                            });
                        }
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }
        }
    }

    private PrefDevListenerInfo getPrefDevListenerInfo(OnPreferredDevicesForStrategyChangedListener listener) {
        ArrayList<PrefDevListenerInfo> arrayList = this.mPrefDevListeners;
        if (arrayList == null) {
            return null;
        }
        Iterator<PrefDevListenerInfo> it = arrayList.iterator();
        while (it.hasNext()) {
            PrefDevListenerInfo info = it.next();
            if (info.mListener == listener) {
                return info;
            }
        }
        return null;
    }

    private boolean hasPrefDevListener(OnPreferredDevicesForStrategyChangedListener listener) {
        return getPrefDevListenerInfo(listener) != null;
    }

    private boolean removePrefDevListener(OnPreferredDevicesForStrategyChangedListener listener) {
        PrefDevListenerInfo infoToRemove = getPrefDevListenerInfo(listener);
        if (infoToRemove != null) {
            this.mPrefDevListeners.remove(infoToRemove);
            return true;
        }
        return false;
    }

    @SystemApi
    public boolean setPreferredDeviceForCapturePreset(int capturePreset, AudioDeviceAttributes device) {
        return setPreferredDevicesForCapturePreset(capturePreset, Arrays.asList(device));
    }

    @SystemApi
    public boolean clearPreferredDevicesForCapturePreset(int capturePreset) {
        if (MediaRecorder.isValidAudioSource(capturePreset)) {
            try {
                int status = getService().clearPreferredDevicesForCapturePreset(capturePreset);
                return status == 0;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    @SystemApi
    public List<AudioDeviceAttributes> getPreferredDevicesForCapturePreset(int capturePreset) {
        if (!MediaRecorder.isValidAudioSource(capturePreset)) {
            return new ArrayList();
        }
        try {
            return getService().getPreferredDevicesForCapturePreset(capturePreset);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private boolean setPreferredDevicesForCapturePreset(int capturePreset, List<AudioDeviceAttributes> devices) {
        Objects.requireNonNull(devices);
        if (MediaRecorder.isValidAudioSource(capturePreset)) {
            if (devices.size() != 1) {
                throw new IllegalArgumentException("Only support setting one preferred devices for capture preset");
            }
            for (AudioDeviceAttributes device : devices) {
                Objects.requireNonNull(device);
            }
            try {
                int status = getService().setPreferredDevicesForCapturePreset(capturePreset, devices);
                return status == 0;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    @SystemApi
    public void addOnPreferredDevicesForCapturePresetChangedListener(Executor executor, OnPreferredDevicesForCapturePresetChangedListener listener) throws SecurityException {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(listener);
        int status = addOnDevRoleForCapturePresetChangedListener(executor, listener, 1);
        if (status == -1) {
            throw new RuntimeException("Unknown error happened");
        }
        if (status == -2) {
            throw new IllegalArgumentException("attempt to call addOnPreferredDevicesForCapturePresetChangedListener() on a previously registered listener");
        }
    }

    @SystemApi
    public void removeOnPreferredDevicesForCapturePresetChangedListener(OnPreferredDevicesForCapturePresetChangedListener listener) {
        Objects.requireNonNull(listener);
        int status = removeOnDevRoleForCapturePresetChangedListener(listener, 1);
        if (status == -1) {
            throw new RuntimeException("Unknown error happened");
        }
        if (status == -2) {
            throw new IllegalArgumentException("attempt to call removeOnPreferredDevicesForCapturePresetChangedListener() on an unregistered listener");
        }
    }

    private <T> int addOnDevRoleForCapturePresetChangedListener(Executor executor, T listener, int deviceRole) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(listener);
        DevRoleListeners<T> devRoleListeners = (DevRoleListeners) this.mDevRoleForCapturePresetListeners.get(Integer.valueOf(deviceRole));
        if (devRoleListeners == null) {
            return -1;
        }
        synchronized (((DevRoleListeners) devRoleListeners).mDevRoleListenersLock) {
            if (devRoleListeners.hasDevRoleListener(listener)) {
                return -2;
            }
            if (((DevRoleListeners) devRoleListeners).mListenerInfos == null) {
                ((DevRoleListeners) devRoleListeners).mListenerInfos = new ArrayList();
            }
            int oldCbCount = ((DevRoleListeners) devRoleListeners).mListenerInfos.size();
            ((DevRoleListeners) devRoleListeners).mListenerInfos.add(new DevRoleListenerInfo(executor, listener));
            if (oldCbCount == 0 && ((DevRoleListeners) devRoleListeners).mListenerInfos.size() > 0) {
                synchronized (this.mDevRoleForCapturePresetListenersLock) {
                    int deviceRoleListenerStatus = this.mDeviceRoleListenersStatus;
                    this.mDeviceRoleListenersStatus = deviceRoleListenerStatus | (1 << deviceRole);
                    if (deviceRoleListenerStatus != 0) {
                        return 0;
                    }
                    if (this.mDevicesRoleForCapturePresetDispatcherStub == null) {
                        this.mDevicesRoleForCapturePresetDispatcherStub = new CapturePresetDevicesRoleDispatcherStub();
                    }
                    try {
                        getService().registerCapturePresetDevicesRoleDispatcher(this.mDevicesRoleForCapturePresetDispatcherStub);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
            return 0;
        }
    }

    private <T> int removeOnDevRoleForCapturePresetChangedListener(T listener, int deviceRole) {
        Objects.requireNonNull(listener);
        DevRoleListeners<T> devRoleListeners = (DevRoleListeners) this.mDevRoleForCapturePresetListeners.get(Integer.valueOf(deviceRole));
        if (devRoleListeners == null) {
            return -1;
        }
        synchronized (((DevRoleListeners) devRoleListeners).mDevRoleListenersLock) {
            if (!devRoleListeners.removeDevRoleListener(listener)) {
                return -2;
            }
            if (((DevRoleListeners) devRoleListeners).mListenerInfos.size() == 0) {
                synchronized (this.mDevRoleForCapturePresetListenersLock) {
                    int i = this.mDeviceRoleListenersStatus ^ (1 << deviceRole);
                    this.mDeviceRoleListenersStatus = i;
                    if (i != 0) {
                        return 0;
                    }
                    try {
                        getService().unregisterCapturePresetDevicesRoleDispatcher(this.mDevicesRoleForCapturePresetDispatcherStub);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DevRoleListenerInfo<T> {
        final Executor mExecutor;
        final T mListener;

        DevRoleListenerInfo(Executor executor, T listener) {
            this.mExecutor = executor;
            this.mListener = listener;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DevRoleListeners<T> {
        private final Object mDevRoleListenersLock;
        private ArrayList<DevRoleListenerInfo<T>> mListenerInfos;

        private DevRoleListeners() {
            this.mDevRoleListenersLock = new Object();
        }

        private DevRoleListenerInfo<T> getDevRoleListenerInfo(T listener) {
            ArrayList<DevRoleListenerInfo<T>> arrayList = this.mListenerInfos;
            if (arrayList == null) {
                return null;
            }
            Iterator<DevRoleListenerInfo<T>> it = arrayList.iterator();
            while (it.hasNext()) {
                DevRoleListenerInfo<T> listenerInfo = it.next();
                if (listenerInfo.mListener == listener) {
                    return listenerInfo;
                }
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean hasDevRoleListener(T listener) {
            return getDevRoleListenerInfo(listener) != null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean removeDevRoleListener(T listener) {
            DevRoleListenerInfo<T> infoToRemove = getDevRoleListenerInfo(listener);
            if (infoToRemove != null) {
                this.mListenerInfos.remove(infoToRemove);
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class CapturePresetDevicesRoleDispatcherStub extends ICapturePresetDevicesRoleDispatcher.Stub {
        private CapturePresetDevicesRoleDispatcherStub() {
        }

        @Override // android.media.ICapturePresetDevicesRoleDispatcher
        public void dispatchDevicesRoleChanged(final int capturePreset, int role, final List<AudioDeviceAttributes> devices) {
            Object listenersObj = AudioManager.this.mDevRoleForCapturePresetListeners.get(Integer.valueOf(role));
            if (listenersObj == null) {
                return;
            }
            switch (role) {
                case 1:
                    DevRoleListeners<OnPreferredDevicesForCapturePresetChangedListener> listeners = (DevRoleListeners) listenersObj;
                    synchronized (((DevRoleListeners) listeners).mDevRoleListenersLock) {
                        if (((DevRoleListeners) listeners).mListenerInfos.isEmpty()) {
                            return;
                        }
                        ArrayList<DevRoleListenerInfo<OnPreferredDevicesForCapturePresetChangedListener>> prefDevListeners = (ArrayList) ((DevRoleListeners) listeners).mListenerInfos.clone();
                        long ident = Binder.clearCallingIdentity();
                        try {
                            Iterator<DevRoleListenerInfo<OnPreferredDevicesForCapturePresetChangedListener>> it = prefDevListeners.iterator();
                            while (it.hasNext()) {
                                final DevRoleListenerInfo<OnPreferredDevicesForCapturePresetChangedListener> info = it.next();
                                info.mExecutor.execute(new Runnable() { // from class: android.media.AudioManager$CapturePresetDevicesRoleDispatcherStub$$ExternalSyntheticLambda0
                                    @Override // java.lang.Runnable
                                    public final void run() {
                                        ((AudioManager.OnPreferredDevicesForCapturePresetChangedListener) AudioManager.DevRoleListenerInfo.this.mListener).onPreferredDevicesForCapturePresetChanged(capturePreset, devices);
                                    }
                                });
                            }
                            return;
                        } finally {
                            Binder.restoreCallingIdentity(ident);
                        }
                    }
                default:
                    return;
            }
        }
    }

    public static int getDirectPlaybackSupport(AudioFormat format, AudioAttributes attributes) {
        Objects.requireNonNull(format);
        Objects.requireNonNull(attributes);
        return AudioSystem.getDirectPlaybackSupport(format, attributes);
    }

    public static boolean isOffloadedPlaybackSupported(AudioFormat format, AudioAttributes attributes) {
        if (format == null) {
            throw new NullPointerException("Illegal null AudioFormat");
        }
        if (attributes != null) {
            return AudioSystem.getOffloadSupport(format, attributes) != 0;
        }
        throw new NullPointerException("Illegal null AudioAttributes");
    }

    @Deprecated
    public static int getPlaybackOffloadSupport(AudioFormat format, AudioAttributes attributes) {
        if (format == null) {
            throw new NullPointerException("Illegal null AudioFormat");
        }
        if (attributes == null) {
            throw new NullPointerException("Illegal null AudioAttributes");
        }
        return AudioSystem.getOffloadSupport(format, attributes);
    }

    public Spatializer getSpatializer() {
        return new Spatializer(this);
    }

    public boolean isBluetoothScoAvailableOffCall() {
        return getContext().getResources().getBoolean(R.bool.config_bluetooth_sco_off_call);
    }

    public void startBluetoothSco() {
        IAudioService service = getService();
        if (DEBUG) {
            Log.d(TAG, "startBluetoothSco()");
        }
        try {
            service.startBluetoothSco(this.mICallBack, getContext().getApplicationInfo().targetSdkVersion);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void startBluetoothScoVirtualCall() {
        if (DEBUG) {
            Log.d(TAG, "startBluetoothScoVirtualCall()");
        }
        IAudioService service = getService();
        try {
            service.startBluetoothScoVirtualCall(this.mICallBack);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void stopBluetoothSco() {
        if (DEBUG) {
            Log.d(TAG, "stopBluetoothSco()");
        }
        IAudioService service = getService();
        try {
            service.stopBluetoothSco(this.mICallBack);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setBluetoothScoOn(boolean on) {
        if (DEBUG) {
            Log.d(TAG, "setBluetoothScoOn(" + on + NavigationBarInflaterView.KEY_CODE_END);
        }
        IAudioService service = getService();
        try {
            service.setBluetoothScoOn(on);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isBluetoothScoOn() {
        IAudioService service = getService();
        try {
            boolean mBTScoStatus = service.isBluetoothScoOn();
            if (DEBUG) {
                Log.d(TAG, "isBluetoothScoOn()=" + mBTScoStatus);
            }
            return mBTScoStatus;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void setBluetoothA2dpOn(boolean on) {
    }

    public boolean isBluetoothA2dpOn() {
        boolean status = false;
        if (AudioSystem.getDeviceConnectionState(128, "") == 1) {
            status = true;
        } else if (AudioSystem.getDeviceConnectionState(256, "") == 1) {
            status = true;
        } else if (AudioSystem.getDeviceConnectionState(512, "") == 1) {
            status = true;
        } else if (AudioSystem.getDeviceConnectionState(536870912, "") == 1) {
            status = true;
        }
        if (DEBUG) {
            Log.d(TAG, "isBluetoothA2dpOn()" + status);
        }
        return status;
    }

    @Deprecated
    public void setWiredHeadsetOn(boolean on) {
    }

    public boolean isWiredHeadsetOn() {
        boolean status;
        if (AudioSystem.getDeviceConnectionState(4, "") == 0 && AudioSystem.getDeviceConnectionState(8, "") == 0 && AudioSystem.getDeviceConnectionState(67108864, "") == 0) {
            status = false;
        } else {
            status = true;
        }
        if (DEBUG) {
            Log.d(TAG, "isWiredHeadsetOn()" + status);
        }
        return status;
    }

    public boolean isBluetoothLeOn() {
        return AudioSystem.getDeviceConnectionState(536870912, "") == 1;
    }

    public boolean isBluetoothLeCgOn() {
        IAudioService service = getService();
        try {
            boolean mBtLeCGAudioStatus = service.isBluetoothLeCgOn();
            if (DEBUG) {
                Log.d(TAG, "isBluetoothLeCgOn()=" + mBtLeCGAudioStatus);
            }
            return mBtLeCGAudioStatus;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getBleCgVolume() {
        IAudioService service = getService();
        try {
            return service.getBleCgVolume();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getLastHfpScoVolume() {
        IAudioService service = getService();
        try {
            return service.getLastHfpScoVolume();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int rescaleCgVolumeIndexToHfpVolumeIndex(int index) {
        IAudioService service = getService();
        try {
            return service.rescaleCgVolumeIndexToHfpVolumeIndex(index);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMicrophoneMute(boolean on) {
        if (DEBUG) {
            Log.d(TAG, "setMicrophoneMute(" + on + NavigationBarInflaterView.KEY_CODE_END);
        }
        IAudioService service = getService();
        try {
            service.setMicrophoneMute(on, getContext().getOpPackageName(), UserHandle.getCallingUserId(), getContext().getAttributionTag());
            service.setMicrophoneMuteDeathHandler(on, getContext().getOpPackageName(), UserHandle.getCallingUserId(), this.mICallBack);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMicrophoneMuteFromSwitch(boolean on) {
        if (DEBUG) {
            Log.d(TAG, "setMicrophoneMuteFromSwitch(" + on + NavigationBarInflaterView.KEY_CODE_END);
        }
        IAudioService service = getService();
        try {
            service.setMicrophoneMuteFromSwitch(on);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isMicrophoneMute() {
        IAudioService service = getService();
        try {
            return service.isMicrophoneMuted();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMode(int mode) {
        IAudioService service = getService();
        if (DEBUG) {
            Log.d(TAG, "setMode(" + mode + NavigationBarInflaterView.KEY_CODE_END);
        }
        try {
            service.setMode(mode, this.mICallBack, this.mApplicationContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getMode() {
        int sdk;
        IAudioService service = getService();
        try {
            int mode = service.getMode();
            try {
                sdk = getContext().getApplicationInfo().targetSdkVersion;
            } catch (NullPointerException e) {
                sdk = Build.VERSION.SDK_INT;
            }
            if (mode == 4 && sdk <= 29) {
                return 2;
            }
            if (mode == 5 && !CompatChanges.isChangeEnabled(CALL_REDIRECTION_AUDIO_MODES)) {
                return 2;
            }
            if (mode == 6) {
                if (!CompatChanges.isChangeEnabled(CALL_REDIRECTION_AUDIO_MODES)) {
                    return 3;
                }
                return mode;
            }
            return mode;
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    /* loaded from: classes2.dex */
    final class ModeDispatcherStub extends IAudioModeDispatcher.Stub implements CallbackUtil.DispatcherStub {
        ModeDispatcherStub() {
        }

        @Override // android.media.CallbackUtil.DispatcherStub
        public void register(boolean register) {
            try {
                if (register) {
                    AudioManager.getService().registerModeDispatcher(this);
                } else {
                    AudioManager.getService().unregisterModeDispatcher(this);
                }
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }

        @Override // android.media.IAudioModeDispatcher
        public void dispatchAudioModeChanged(final int mode) {
            AudioManager.this.mModeChangedListenerMgr.callListeners(new CallbackUtil.CallbackMethod() { // from class: android.media.AudioManager$ModeDispatcherStub$$ExternalSyntheticLambda0
                @Override // android.media.CallbackUtil.CallbackMethod
                public final void callbackMethod(Object obj) {
                    ((AudioManager.OnModeChangedListener) obj).onModeChanged(mode);
                }
            });
        }
    }

    public void addOnModeChangedListener(Executor executor, OnModeChangedListener listener) {
        this.mModeChangedListenerMgr.addListener(executor, listener, "addOnModeChangedListener", new Supplier() { // from class: android.media.AudioManager$$ExternalSyntheticLambda1
            @Override // java.util.function.Supplier
            public final Object get() {
                return AudioManager.this.m2008lambda$addOnModeChangedListener$0$androidmediaAudioManager();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addOnModeChangedListener$0$android-media-AudioManager  reason: not valid java name */
    public /* synthetic */ CallbackUtil.DispatcherStub m2008lambda$addOnModeChangedListener$0$androidmediaAudioManager() {
        return new ModeDispatcherStub();
    }

    public void removeOnModeChangedListener(OnModeChangedListener listener) {
        this.mModeChangedListenerMgr.removeListener(listener, "removeOnModeChangedListener");
    }

    public boolean isCallScreeningModeSupported() {
        IAudioService service = getService();
        try {
            return service.isCallScreeningModeSupported();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void setRouting(int mode, int routes, int mask) {
    }

    @Deprecated
    public int getRouting(int mode) {
        return -1;
    }

    public boolean isMusicActive() {
        IAudioService service = getService();
        try {
            return service.isMusicActive(false);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isMusicActiveRemotely() {
        IAudioService service = getService();
        try {
            return service.isMusicActive(true);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isAudioFocusExclusive() {
        IAudioService service = getService();
        try {
            return service.getCurrentAudioFocus() == 4;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int generateAudioSessionId() {
        int session = AudioSystem.newAudioSessionId();
        if (session > 0) {
            return session;
        }
        Log.e(TAG, "Failure to generate a new audio session ID");
        return -1;
    }

    @Deprecated
    public void setParameter(String key, String value) {
        if (DEBUG) {
            Log.d(TAG, "setParameter(" + key + "=" + value + NavigationBarInflaterView.KEY_CODE_END);
        }
        setParameters(key + "=" + value);
    }

    public void setParameters(String keyValuePairs) {
        if (DEBUG) {
            Log.d(TAG, "setParameter(" + keyValuePairs + NavigationBarInflaterView.KEY_CODE_END);
        }
        AudioSystem.setParameters(keyValuePairs);
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public void setHfpEnabled(boolean enable) {
        if (DEBUG) {
            Log.d(TAG, "setHfpEnabled(" + enable + NavigationBarInflaterView.KEY_CODE_END);
        }
        AudioSystem.setParameters("hfp_enable=" + enable);
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public void setHfpVolume(int volume) {
        if (DEBUG) {
            Log.d(TAG, "setHfpVolume(" + volume + NavigationBarInflaterView.KEY_CODE_END);
        }
        AudioSystem.setParameters("hfp_volume=" + volume);
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public void setHfpSamplingRate(int rate) {
        if (DEBUG) {
            Log.d(TAG, "setHfpSamplingRate(" + rate + NavigationBarInflaterView.KEY_CODE_END);
        }
        AudioSystem.setParameters("hfp_set_sampling_rate=" + rate);
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public void setBluetoothHeadsetProperties(String name, boolean hasNrecEnabled, boolean hasWbsEnabled) {
        AudioSystem.setParameters("bt_headset_name=" + name + ";bt_headset_nrec=" + (hasNrecEnabled ? "on" : "off") + ";bt_wbs=" + (hasWbsEnabled ? "on" : "off"));
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public void setA2dpSuspended(boolean enable) {
        if (DEBUG) {
            Log.d(TAG, "setA2dpSuspended(" + enable + NavigationBarInflaterView.KEY_CODE_END);
        }
        AudioSystem.setParameters("A2dpSuspended=" + enable);
    }

    public String getParameters(String keys) {
        return AudioSystem.getParameters(keys);
    }

    public static int getNthNavigationRepeatSoundEffect(int n) {
        switch (n) {
            case 0:
                return 12;
            case 1:
                return 13;
            case 2:
                return 14;
            case 3:
                return 15;
            default:
                Log.w(TAG, "Invalid navigation repeat sound effect id: " + n);
                return -1;
        }
    }

    public void setNavigationRepeatSoundEffectsEnabled(boolean enabled) {
        try {
            getService().setNavigationRepeatSoundEffectsEnabled(enabled);
        } catch (RemoteException e) {
        }
    }

    public boolean areNavigationRepeatSoundEffectsEnabled() {
        try {
            return getService().areNavigationRepeatSoundEffectsEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setHomeSoundEffectEnabled(boolean enabled) {
        try {
            getService().setHomeSoundEffectEnabled(enabled);
        } catch (RemoteException e) {
        }
    }

    public boolean isHomeSoundEffectEnabled() {
        try {
            return getService().isHomeSoundEffectEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void playSoundEffect(int effectType) {
        playSoundEffect(effectType, -2);
    }

    public void playSoundEffect(int effectType, int userId) {
        if (effectType < 0 || effectType >= 16) {
            return;
        }
        IAudioService service = getService();
        try {
            service.playSoundEffect(effectType, userId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void playSoundEffect(int effectType, float volume) {
        if (effectType < 0 || effectType >= 16) {
            return;
        }
        IAudioService service = getService();
        try {
            service.playSoundEffectVolume(effectType, volume);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void loadSoundEffects() {
        IAudioService service = getService();
        try {
            service.loadSoundEffects();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unloadSoundEffects() {
        IAudioService service = getService();
        try {
            service.unloadSoundEffects();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static String audioFocusToString(int focus) {
        switch (focus) {
            case -3:
                return "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK";
            case -2:
                return "AUDIOFOCUS_LOSS_TRANSIENT";
            case -1:
                return "AUDIOFOCUS_LOSS";
            case 0:
                return "AUDIOFOCUS_NONE";
            case 1:
                return "AUDIOFOCUS_GAIN";
            case 2:
                return "AUDIOFOCUS_GAIN_TRANSIENT";
            case 3:
                return "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK";
            case 4:
                return "AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE";
            default:
                return "AUDIO_FOCUS_UNKNOWN(" + focus + NavigationBarInflaterView.KEY_CODE_END;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class FocusRequestInfo {
        final Handler mHandler;
        final AudioFocusRequest mRequest;

        FocusRequestInfo(AudioFocusRequest afr, Handler handler) {
            this.mRequest = afr;
            this.mHandler = handler;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public FocusRequestInfo findFocusRequestInfo(String id) {
        return this.mAudioFocusIdListenerMap.get(id);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ServiceEventHandlerDelegate {
        private final Handler mHandler;

        ServiceEventHandlerDelegate(Handler handler) {
            Looper looper;
            if (handler == null) {
                Looper myLooper = Looper.myLooper();
                looper = myLooper;
                if (myLooper == null) {
                    looper = Looper.getMainLooper();
                }
            } else {
                looper = handler.getLooper();
            }
            if (looper != null) {
                this.mHandler = new Handler(looper) { // from class: android.media.AudioManager.ServiceEventHandlerDelegate.1
                    @Override // android.os.Handler
                    public void handleMessage(Message msg) {
                        OnAudioFocusChangeListener listener;
                        switch (msg.what) {
                            case 0:
                                FocusRequestInfo fri = AudioManager.this.findFocusRequestInfo((String) msg.obj);
                                if (fri != null && (listener = fri.mRequest.getOnAudioFocusChangeListener()) != null) {
                                    Log.d(AudioManager.TAG, "dispatching onAudioFocusChange(" + msg.arg1 + ") to " + msg.obj);
                                    listener.onAudioFocusChange(msg.arg1);
                                    return;
                                }
                                return;
                            case 1:
                                RecordConfigChangeCallbackData cbData = (RecordConfigChangeCallbackData) msg.obj;
                                if (cbData.mCb != null) {
                                    if (AudioManager.DEBUG) {
                                        Log.d(AudioManager.TAG, "dispatching onRecordingConfigChanged()");
                                    }
                                    cbData.mCb.onRecordingConfigChanged(cbData.mConfigs);
                                    return;
                                }
                                return;
                            case 2:
                                PlaybackConfigChangeCallbackData cbData2 = (PlaybackConfigChangeCallbackData) msg.obj;
                                if (cbData2.mCb != null) {
                                    if (AudioManager.DEBUG) {
                                        Log.d(AudioManager.TAG, "dispatching onPlaybackConfigChanged()");
                                    }
                                    cbData2.mCb.onPlaybackConfigChanged(cbData2.mConfigs);
                                    return;
                                }
                                return;
                            default:
                                Log.e(AudioManager.TAG, "Unknown event " + msg.what);
                                return;
                        }
                    }
                };
            } else {
                this.mHandler = null;
            }
        }

        Handler getHandler() {
            return this.mHandler;
        }
    }

    private String getIdForAudioFocusListener(OnAudioFocusChangeListener l) {
        if (l == null) {
            return new String(toString());
        }
        return new String(toString() + l.toString());
    }

    public void registerAudioFocusRequest(AudioFocusRequest afr) {
        Handler h = afr.getOnAudioFocusChangeListenerHandler();
        FocusRequestInfo fri = new FocusRequestInfo(afr, h == null ? null : new ServiceEventHandlerDelegate(h).getHandler());
        String key = getIdForAudioFocusListener(afr.getOnAudioFocusChangeListener());
        this.mAudioFocusIdListenerMap.put(key, fri);
    }

    public void unregisterAudioFocusRequest(OnAudioFocusChangeListener l) {
        this.mAudioFocusIdListenerMap.remove(getIdForAudioFocusListener(l));
    }

    public int requestAudioFocus(OnAudioFocusChangeListener l, int streamType, int durationHint) {
        PlayerBase.deprecateStreamTypeForPlayback(streamType, TAG, "requestAudioFocus()");
        int status = 0;
        try {
            status = requestAudioFocus(l, new AudioAttributes.Builder().setInternalLegacyStreamType(streamType).build(), durationHint, 0);
            if (DEBUG) {
                Log.d(TAG, "requestAudioFocus() from " + l + ", streamType=" + streamType + ", durationHint=" + durationHint + "guaranteedstatus=" + status);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Audio focus request denied due to ", e);
        }
        return status;
    }

    public int requestAudioFocus(AudioFocusRequest focusRequest) {
        return requestAudioFocus(focusRequest, null);
    }

    public int abandonAudioFocusRequest(AudioFocusRequest focusRequest) {
        if (focusRequest == null) {
            throw new IllegalArgumentException("Illegal null AudioFocusRequest");
        }
        return abandonAudioFocus(focusRequest.getOnAudioFocusChangeListener(), focusRequest.getAudioAttributes());
    }

    @SystemApi
    public int requestAudioFocus(OnAudioFocusChangeListener l, AudioAttributes requestAttributes, int durationHint, int flags) throws IllegalArgumentException {
        if (flags != (flags & 3)) {
            throw new IllegalArgumentException("Invalid flags 0x" + Integer.toHexString(flags).toUpperCase());
        }
        return requestAudioFocus(l, requestAttributes, durationHint, flags & 3, null);
    }

    @SystemApi
    public int requestAudioFocus(OnAudioFocusChangeListener l, AudioAttributes requestAttributes, int durationHint, int flags, AudioPolicy ap) throws IllegalArgumentException {
        if (requestAttributes == null) {
            throw new IllegalArgumentException("Illegal null AudioAttributes argument");
        }
        if (!AudioFocusRequest.isValidFocusGain(durationHint)) {
            throw new IllegalArgumentException("Invalid duration hint");
        }
        if (flags != (flags & 7)) {
            throw new IllegalArgumentException("Illegal flags 0x" + Integer.toHexString(flags).toUpperCase());
        }
        if ((flags & 1) == 1 && l == null) {
            throw new IllegalArgumentException("Illegal null focus listener when flagged as accepting delayed focus grant");
        }
        if ((flags & 2) == 2 && l == null) {
            throw new IllegalArgumentException("Illegal null focus listener when flagged as pausing instead of ducking");
        }
        if ((flags & 4) == 4 && ap == null) {
            throw new IllegalArgumentException("Illegal null audio policy when locking audio focus");
        }
        AudioFocusRequest afr = new AudioFocusRequest.Builder(durationHint).setOnAudioFocusChangeListenerInt(l, null).setAudioAttributes(requestAttributes).setAcceptsDelayedFocusGain((flags & 1) == 1).setWillPauseWhenDucked((flags & 2) == 2).setLocksFocus((flags & 4) == 4).build();
        return requestAudioFocus(afr, ap);
    }

    public int requestAudioFocusForTest(AudioFocusRequest afr, String clientFakeId, int clientFakeUid, int clientTargetSdk) {
        Objects.requireNonNull(afr);
        Objects.requireNonNull(clientFakeId);
        try {
            return getService().requestAudioFocusForTest(afr.getAudioAttributes(), afr.getFocusGain(), this.mICallBack, this.mAudioFocusDispatcher, clientFakeId, "com.android.test.fakeclient", afr.getFlags() | 8, clientFakeUid, clientTargetSdk);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int abandonAudioFocusForTest(AudioFocusRequest afr, String clientFakeId) {
        Objects.requireNonNull(afr);
        Objects.requireNonNull(clientFakeId);
        try {
            return getService().abandonAudioFocusForTest(this.mAudioFocusDispatcher, clientFakeId, afr.getAudioAttributes(), "com.android.test.fakeclient");
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getFadeOutDurationOnFocusLossMillis(AudioAttributes aa) {
        Objects.requireNonNull(aa);
        try {
            return getService().getFadeOutDurationOnFocusLossMillis(aa);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int requestAudioFocus(AudioFocusRequest afr, AudioPolicy ap) {
        int sdk;
        if (afr == null) {
            throw new NullPointerException("Illegal null AudioFocusRequest");
        }
        if (afr.locksFocus() && ap == null) {
            throw new IllegalArgumentException("Illegal null audio policy when locking audio focus");
        }
        registerAudioFocusRequest(afr);
        IAudioService service = getService();
        try {
            int sdk2 = getContext().getApplicationInfo().targetSdkVersion;
            sdk = sdk2;
        } catch (NullPointerException e) {
            sdk = Build.VERSION.SDK_INT;
        }
        String clientId = getIdForAudioFocusListener(afr.getOnAudioFocusChangeListener());
        synchronized (this.mFocusRequestsLock) {
            try {
                try {
                    try {
                        int status = service.requestAudioFocus(afr.getAudioAttributes(), afr.getFocusGain(), this.mICallBack, this.mAudioFocusDispatcher, clientId, getContext().getOpPackageName(), getContext().getAttributionTag(), afr.getFlags(), ap != null ? ap.cb() : null, sdk);
                        if (status != 100) {
                            return status;
                        }
                        if (this.mFocusRequestsAwaitingResult == null) {
                            this.mFocusRequestsAwaitingResult = new HashMap<>(1);
                        }
                        BlockingFocusResultReceiver focusReceiver = new BlockingFocusResultReceiver(clientId);
                        this.mFocusRequestsAwaitingResult.put(clientId, focusReceiver);
                        focusReceiver.waitForResult(200L);
                        if (DEBUG && !focusReceiver.receivedResult()) {
                            Log.e(TAG, "requestAudio response from ext policy timed out, denying request");
                        }
                        synchronized (this.mFocusRequestsLock) {
                            this.mFocusRequestsAwaitingResult.remove(clientId);
                        }
                        return focusReceiver.requestResult();
                    } catch (RemoteException e2) {
                        e = e2;
                        throw e.rethrowFromSystemServer();
                    }
                } catch (Throwable th) {
                    e = th;
                    throw e;
                }
            } catch (RemoteException e3) {
                e = e3;
            } catch (Throwable th2) {
                e = th2;
                throw e;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class SafeWaitObject {
        private boolean mQuit;

        private SafeWaitObject() {
            this.mQuit = false;
        }

        public void safeNotify() {
            synchronized (this) {
                this.mQuit = true;
                notify();
            }
        }

        public void safeWait(long millis) throws InterruptedException {
            long timeOutTime = System.currentTimeMillis() + millis;
            synchronized (this) {
                while (!this.mQuit) {
                    long timeToWait = timeOutTime - System.currentTimeMillis();
                    if (timeToWait < 0) {
                        break;
                    }
                    wait(timeToWait);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class BlockingFocusResultReceiver {
        private final String mFocusClientId;
        private final SafeWaitObject mLock = new SafeWaitObject();
        private boolean mResultReceived = false;
        private int mFocusRequestResult = 0;

        BlockingFocusResultReceiver(String clientId) {
            this.mFocusClientId = clientId;
        }

        boolean receivedResult() {
            return this.mResultReceived;
        }

        int requestResult() {
            return this.mFocusRequestResult;
        }

        void notifyResult(int requestResult) {
            synchronized (this.mLock) {
                this.mResultReceived = true;
                this.mFocusRequestResult = requestResult;
                this.mLock.safeNotify();
            }
        }

        public void waitForResult(long timeOutMs) {
            synchronized (this.mLock) {
                if (this.mResultReceived) {
                    return;
                }
                try {
                    this.mLock.safeWait(timeOutMs);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void requestAudioFocusForCall(int streamType, int durationHint) {
        IAudioService service = getService();
        try {
            service.requestAudioFocus(new AudioAttributes.Builder().setInternalLegacyStreamType(streamType).build(), durationHint, this.mICallBack, null, AudioSystem.IN_VOICE_COMM_FOCUS_ID, getContext().getOpPackageName(), getContext().getAttributionTag(), 4, null, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getFocusRampTimeMs(int focusGain, AudioAttributes attr) {
        IAudioService service = getService();
        try {
            return service.getFocusRampTimeMs(focusGain, attr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setFocusRequestResult(AudioFocusInfo afi, int requestResult, AudioPolicy ap) {
        if (afi == null) {
            throw new IllegalArgumentException("Illegal null AudioFocusInfo");
        }
        if (ap == null) {
            throw new IllegalArgumentException("Illegal null AudioPolicy");
        }
        IAudioService service = getService();
        try {
            service.setFocusRequestResultFromExtPolicy(afi, requestResult, ap.cb());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int dispatchAudioFocusChange(AudioFocusInfo afi, int focusChange, AudioPolicy ap) {
        if (afi == null) {
            throw new NullPointerException("Illegal null AudioFocusInfo");
        }
        if (ap == null) {
            throw new NullPointerException("Illegal null AudioPolicy");
        }
        IAudioService service = getService();
        try {
            return service.dispatchFocusChange(afi, focusChange, ap.cb());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void abandonAudioFocusForCall() {
        IAudioService service = getService();
        try {
            service.abandonAudioFocus(null, AudioSystem.IN_VOICE_COMM_FOCUS_ID, null, getContext().getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int abandonAudioFocus(OnAudioFocusChangeListener l) {
        return abandonAudioFocus(l, null);
    }

    @SystemApi
    public int abandonAudioFocus(OnAudioFocusChangeListener l, AudioAttributes aa) {
        unregisterAudioFocusRequest(l);
        IAudioService service = getService();
        try {
            int status = service.abandonAudioFocus(this.mAudioFocusDispatcher, getIdForAudioFocusListener(l), aa, getContext().getOpPackageName());
            return status;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void registerMediaButtonEventReceiver(ComponentName eventReceiver) {
        if (eventReceiver == null) {
            return;
        }
        if (!eventReceiver.getPackageName().equals(getContext().getPackageName())) {
            Log.e(TAG, "registerMediaButtonEventReceiver() error: receiver and context package names don't match");
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "registerMediaButtonEventReceiver() , eventReceiver=" + eventReceiver + "packageName=" + eventReceiver.getPackageName());
        }
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(eventReceiver);
        PendingIntent pi = PendingIntent.getBroadcast(getContext(), 0, mediaButtonIntent, 67108864);
        registerMediaButtonIntent(pi, eventReceiver);
    }

    @Deprecated
    public void registerMediaButtonEventReceiver(PendingIntent eventReceiver) {
        if (eventReceiver == null) {
            return;
        }
        registerMediaButtonIntent(eventReceiver, null);
    }

    public void registerMediaButtonIntent(PendingIntent pi, ComponentName eventReceiver) {
        if (pi == null) {
            Log.e(TAG, "Cannot call registerMediaButtonIntent() with a null parameter");
            return;
        }
        MediaSessionLegacyHelper helper = MediaSessionLegacyHelper.getHelper(getContext());
        helper.addMediaButtonListener(pi, eventReceiver, getContext());
    }

    @Deprecated
    public void unregisterMediaButtonEventReceiver(ComponentName eventReceiver) {
        if (eventReceiver == null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "unregisterMediaButtonEventReceiver() , eventReceiver=" + eventReceiver + "packageName=" + eventReceiver.getPackageName());
        }
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(eventReceiver);
        PendingIntent pi = PendingIntent.getBroadcast(getContext(), 0, mediaButtonIntent, 67108864);
        unregisterMediaButtonIntent(pi);
    }

    @Deprecated
    public void unregisterMediaButtonEventReceiver(PendingIntent eventReceiver) {
        if (eventReceiver == null) {
            return;
        }
        unregisterMediaButtonIntent(eventReceiver);
    }

    public void unregisterMediaButtonIntent(PendingIntent pi) {
        MediaSessionLegacyHelper helper = MediaSessionLegacyHelper.getHelper(getContext());
        helper.removeMediaButtonListener(pi);
    }

    @Deprecated
    public void registerRemoteControlClient(RemoteControlClient rcClient) {
        if (rcClient == null || rcClient.getRcMediaIntent() == null) {
            return;
        }
        rcClient.registerWithSession(MediaSessionLegacyHelper.getHelper(getContext()));
    }

    @Deprecated
    public void unregisterRemoteControlClient(RemoteControlClient rcClient) {
        if (rcClient == null || rcClient.getRcMediaIntent() == null) {
            return;
        }
        rcClient.unregisterWithSession(MediaSessionLegacyHelper.getHelper(getContext()));
    }

    @Deprecated
    public boolean registerRemoteController(RemoteController rctlr) {
        if (rctlr == null) {
            return false;
        }
        rctlr.startListeningToSessions();
        return true;
    }

    @Deprecated
    public void unregisterRemoteController(RemoteController rctlr) {
        if (rctlr == null) {
            return;
        }
        rctlr.stopListeningToSessions();
    }

    @SystemApi
    public int registerAudioPolicy(AudioPolicy policy) {
        return registerAudioPolicyStatic(policy);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int registerAudioPolicyStatic(AudioPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Illegal null AudioPolicy argument");
        }
        IAudioService service = getService();
        try {
            MediaProjection projection = policy.getMediaProjection();
            String regId = service.registerAudioPolicy(policy.getConfig(), policy.cb(), policy.hasFocusListener(), policy.isFocusPolicy(), policy.isTestFocusPolicy(), policy.isVolumeController(), projection == null ? null : projection.getProjection());
            if (regId == null) {
                return -1;
            }
            policy.setRegistration(regId);
            return 0;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void unregisterAudioPolicyAsync(AudioPolicy policy) {
        unregisterAudioPolicyAsyncStatic(policy);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void unregisterAudioPolicyAsyncStatic(AudioPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Illegal null AudioPolicy argument");
        }
        IAudioService service = getService();
        try {
            service.unregisterAudioPolicyAsync(policy.cb());
            policy.reset();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void unregisterAudioPolicy(AudioPolicy policy) {
        Preconditions.checkNotNull(policy, "Illegal null AudioPolicy argument");
        IAudioService service = getService();
        try {
            policy.invalidateCaptorsAndInjectors();
            service.unregisterAudioPolicy(policy.cb());
            policy.reset();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasRegisteredDynamicPolicy() {
        IAudioService service = getService();
        try {
            return service.hasRegisteredDynamicPolicy();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class AudioPlaybackCallback {
        public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class AudioPlaybackCallbackInfo {
        final AudioPlaybackCallback mCb;
        final Handler mHandler;

        AudioPlaybackCallbackInfo(AudioPlaybackCallback cb, Handler handler) {
            this.mCb = cb;
            this.mHandler = handler;
        }
    }

    /* loaded from: classes2.dex */
    private static final class PlaybackConfigChangeCallbackData {
        final AudioPlaybackCallback mCb;
        final List<AudioPlaybackConfiguration> mConfigs;

        PlaybackConfigChangeCallbackData(AudioPlaybackCallback cb, List<AudioPlaybackConfiguration> configs) {
            this.mCb = cb;
            this.mConfigs = configs;
        }
    }

    public void registerAudioPlaybackCallback(AudioPlaybackCallback cb, Handler handler) {
        if (cb == null) {
            throw new IllegalArgumentException("Illegal null AudioPlaybackCallback argument");
        }
        synchronized (this.mPlaybackCallbackLock) {
            if (this.mPlaybackCallbackList == null) {
                this.mPlaybackCallbackList = new ArrayList();
            }
            int oldCbCount = this.mPlaybackCallbackList.size();
            if (!hasPlaybackCallback_sync(cb)) {
                this.mPlaybackCallbackList.add(new AudioPlaybackCallbackInfo(cb, new ServiceEventHandlerDelegate(handler).getHandler()));
                int newCbCount = this.mPlaybackCallbackList.size();
                if (oldCbCount == 0 && newCbCount > 0) {
                    try {
                        getService().registerPlaybackCallback(this.mPlayCb);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            } else {
                Log.w(TAG, "attempt to call registerAudioPlaybackCallback() on a previouslyregistered callback");
            }
        }
    }

    public void unregisterAudioPlaybackCallback(AudioPlaybackCallback cb) {
        if (cb == null) {
            throw new IllegalArgumentException("Illegal null AudioPlaybackCallback argument");
        }
        synchronized (this.mPlaybackCallbackLock) {
            List<AudioPlaybackCallbackInfo> list = this.mPlaybackCallbackList;
            if (list == null) {
                Log.w(TAG, "attempt to call unregisterAudioPlaybackCallback() on a callback that was never registered");
                return;
            }
            int oldCbCount = list.size();
            if (removePlaybackCallback_sync(cb)) {
                int newCbCount = this.mPlaybackCallbackList.size();
                if (oldCbCount > 0 && newCbCount == 0) {
                    try {
                        getService().unregisterPlaybackCallback(this.mPlayCb);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            } else {
                Log.w(TAG, "attempt to call unregisterAudioPlaybackCallback() on a callback already unregistered or never registered");
            }
        }
    }

    public List<AudioPlaybackConfiguration> getActivePlaybackConfigurations() {
        IAudioService service = getService();
        try {
            return service.getActivePlaybackConfigurations();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private boolean hasPlaybackCallback_sync(AudioPlaybackCallback cb) {
        if (this.mPlaybackCallbackList != null) {
            for (int i = 0; i < this.mPlaybackCallbackList.size(); i++) {
                if (cb.equals(this.mPlaybackCallbackList.get(i).mCb)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean removePlaybackCallback_sync(AudioPlaybackCallback cb) {
        if (this.mPlaybackCallbackList != null) {
            for (int i = 0; i < this.mPlaybackCallbackList.size(); i++) {
                if (cb.equals(this.mPlaybackCallbackList.get(i).mCb)) {
                    this.mPlaybackCallbackList.remove(i);
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /* loaded from: classes2.dex */
    public static abstract class AudioRecordingCallback {
        public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class AudioRecordingCallbackInfo {
        final AudioRecordingCallback mCb;
        final Handler mHandler;

        AudioRecordingCallbackInfo(AudioRecordingCallback cb, Handler handler) {
            this.mCb = cb;
            this.mHandler = handler;
        }
    }

    /* loaded from: classes2.dex */
    private static final class RecordConfigChangeCallbackData {
        final AudioRecordingCallback mCb;
        final List<AudioRecordingConfiguration> mConfigs;

        RecordConfigChangeCallbackData(AudioRecordingCallback cb, List<AudioRecordingConfiguration> configs) {
            this.mCb = cb;
            this.mConfigs = configs;
        }
    }

    public void registerAudioRecordingCallback(AudioRecordingCallback cb, Handler handler) {
        if (cb == null) {
            throw new IllegalArgumentException("Illegal null AudioRecordingCallback argument");
        }
        synchronized (this.mRecordCallbackLock) {
            if (this.mRecordCallbackList == null) {
                this.mRecordCallbackList = new ArrayList();
            }
            int oldCbCount = this.mRecordCallbackList.size();
            if (!hasRecordCallback_sync(cb)) {
                this.mRecordCallbackList.add(new AudioRecordingCallbackInfo(cb, new ServiceEventHandlerDelegate(handler).getHandler()));
                int newCbCount = this.mRecordCallbackList.size();
                if (oldCbCount == 0 && newCbCount > 0) {
                    IAudioService service = getService();
                    try {
                        service.registerRecordingCallback(this.mRecCb);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            } else {
                Log.w(TAG, "attempt to call registerAudioRecordingCallback() on a previouslyregistered callback");
            }
        }
    }

    public void unregisterAudioRecordingCallback(AudioRecordingCallback cb) {
        if (cb == null) {
            throw new IllegalArgumentException("Illegal null AudioRecordingCallback argument");
        }
        synchronized (this.mRecordCallbackLock) {
            List<AudioRecordingCallbackInfo> list = this.mRecordCallbackList;
            if (list == null) {
                return;
            }
            int oldCbCount = list.size();
            if (removeRecordCallback_sync(cb)) {
                int newCbCount = this.mRecordCallbackList.size();
                if (oldCbCount > 0 && newCbCount == 0) {
                    IAudioService service = getService();
                    try {
                        service.unregisterRecordingCallback(this.mRecCb);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            } else {
                Log.w(TAG, "attempt to call unregisterAudioRecordingCallback() on a callback already unregistered or never registered");
            }
        }
    }

    public List<AudioRecordingConfiguration> getActiveRecordingConfigurations() {
        IAudioService service = getService();
        try {
            return service.getActiveRecordingConfigurations();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private boolean hasRecordCallback_sync(AudioRecordingCallback cb) {
        if (this.mRecordCallbackList != null) {
            for (int i = 0; i < this.mRecordCallbackList.size(); i++) {
                if (cb.equals(this.mRecordCallbackList.get(i).mCb)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean removeRecordCallback_sync(AudioRecordingCallback cb) {
        if (this.mRecordCallbackList != null) {
            for (int i = 0; i < this.mRecordCallbackList.size(); i++) {
                if (cb.equals(this.mRecordCallbackList.get(i).mCb)) {
                    this.mRecordCallbackList.remove(i);
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public void reloadAudioSettings() {
        IAudioService service = getService();
        try {
            service.reloadAudioSettings();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSilentMode() {
        int ringerMode = getRingerMode();
        return ringerMode == 0 || ringerMode == 1;
    }

    public static boolean isOutputDevice(int device) {
        return (Integer.MIN_VALUE & device) == 0;
    }

    public static boolean isInputDevice(int device) {
        return (device & Integer.MIN_VALUE) == Integer.MIN_VALUE;
    }

    @Deprecated
    public int getDevicesForStream(int streamType) {
        switch (streamType) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 8:
            case 10:
                IAudioService service = getService();
                try {
                    return service.getDeviceMaskForStream(streamType);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            case 6:
            case 7:
            case 9:
            default:
                return 0;
        }
    }

    @SystemApi
    public List<AudioDeviceAttributes> getDevicesForAttributes(AudioAttributes attributes) {
        Objects.requireNonNull(attributes);
        IAudioService service = getService();
        try {
            return service.getDevicesForAttributes(attributes);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<AudioDeviceInfo> getAudioDevicesForAttributes(AudioAttributes attributes) {
        try {
            Objects.requireNonNull(attributes);
            IAudioService service = getService();
            List<AudioDeviceAttributes> devicesForAttributes = service.getDevicesForAttributesUnprotected(attributes);
            AudioDeviceInfo[] outputDeviceInfos = getDevicesStatic(2);
            List<AudioDeviceInfo> deviceInfosForAttributes = new ArrayList<>();
            for (AudioDeviceAttributes deviceForAttributes : devicesForAttributes) {
                for (AudioDeviceInfo deviceInfo : outputDeviceInfos) {
                    if (deviceForAttributes.getType() == deviceInfo.getType() && TextUtils.equals(deviceForAttributes.getAddress(), deviceInfo.getAddress())) {
                        deviceInfosForAttributes.add(deviceInfo);
                    }
                }
            }
            return Collections.unmodifiableList(deviceInfosForAttributes);
        } catch (Exception e) {
            Log.i(TAG, "No audio devices available for specified attributes.");
            return Collections.emptyList();
        }
    }

    public static void enforceValidVolumeBehavior(int volumeBehavior) {
        switch (volumeBehavior) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                return;
            default:
                throw new IllegalArgumentException("Illegal volume behavior " + volumeBehavior);
        }
    }

    @SystemApi
    public void setDeviceVolumeBehavior(AudioDeviceAttributes device, int deviceVolumeBehavior) {
        Objects.requireNonNull(device);
        enforceValidVolumeBehavior(deviceVolumeBehavior);
        IAudioService service = getService();
        try {
            service.setDeviceVolumeBehavior(device, deviceVolumeBehavior, this.mApplicationContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int getDeviceVolumeBehavior(AudioDeviceAttributes device) {
        Objects.requireNonNull(device);
        IAudioService service = getService();
        try {
            return service.getDeviceVolumeBehavior(device);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isFullVolumeDevice() {
        AudioAttributes attributes = new AudioAttributes.Builder().setUsage(1).build();
        List<AudioDeviceAttributes> devices = getDevicesForAttributes(attributes);
        for (AudioDeviceAttributes device : devices) {
            if (getDeviceVolumeBehavior(device) == 1) {
                return true;
            }
        }
        return false;
    }

    public void setWiredDeviceConnectionState(int device, int state, String address, String name) {
        AudioDeviceAttributes attributes = new AudioDeviceAttributes(device, address, name);
        setWiredDeviceConnectionState(attributes, state);
    }

    public void setWiredDeviceConnectionState(AudioDeviceAttributes attributes, int state) {
        IAudioService service = getService();
        try {
            service.setWiredDeviceConnectionState(attributes, state, this.mApplicationContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTestDeviceConnectionState(AudioDeviceAttributes device, boolean connected) {
        try {
            getService().setTestDeviceConnectionState(device, connected);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public void handleBluetoothActiveDeviceChanged(BluetoothDevice newDevice, BluetoothDevice previousDevice, BluetoothProfileConnectionInfo info) {
        IAudioService service = getService();
        try {
            service.handleBluetoothActiveDeviceChanged(newDevice, previousDevice, info);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public IRingtonePlayer getRingtonePlayer() {
        try {
            return getService().getRingtonePlayer();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getProperty(String key) {
        if (PROPERTY_OUTPUT_SAMPLE_RATE.equals(key)) {
            int outputSampleRate = AudioSystem.getPrimaryOutputSamplingRate();
            if (outputSampleRate > 0) {
                return Integer.toString(outputSampleRate);
            }
            return null;
        } else if (PROPERTY_OUTPUT_FRAMES_PER_BUFFER.equals(key)) {
            int outputFramesPerBuffer = AudioSystem.getPrimaryOutputFrameCount();
            if (outputFramesPerBuffer > 0) {
                return Integer.toString(outputFramesPerBuffer);
            }
            return null;
        } else if (PROPERTY_SUPPORT_MIC_NEAR_ULTRASOUND.equals(key)) {
            return String.valueOf(getContext().getResources().getBoolean(R.bool.config_supportMicNearUltrasound));
        } else {
            if (PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND.equals(key)) {
                return String.valueOf(getContext().getResources().getBoolean(R.bool.config_supportSpeakerNearUltrasound));
            }
            if (PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED.equals(key)) {
                return String.valueOf(getContext().getResources().getBoolean(R.bool.config_supportAudioSourceUnprocessed));
            }
            return null;
        }
    }

    @SystemApi
    public boolean setAdditionalOutputDeviceDelay(AudioDeviceInfo device, long delayMillis) {
        Objects.requireNonNull(device);
        try {
            return getService().setAdditionalOutputDeviceDelay(new AudioDeviceAttributes(device), delayMillis);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public long getAdditionalOutputDeviceDelay(AudioDeviceInfo device) {
        Objects.requireNonNull(device);
        try {
            return getService().getAdditionalOutputDeviceDelay(new AudioDeviceAttributes(device));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public long getMaxAdditionalOutputDeviceDelay(AudioDeviceInfo device) {
        Objects.requireNonNull(device);
        try {
            return getService().getMaxAdditionalOutputDeviceDelay(new AudioDeviceAttributes(device));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getOutputLatency(int streamType) {
        return AudioSystem.getOutputLatency(streamType);
    }

    public void setVolumeController(IVolumeController controller) {
        try {
            if (DEBUG) {
                Log.d(TAG, "setVolumeController(), controller=" + controller);
            }
            getService().setVolumeController(controller);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void notifyVolumeControllerVisible(IVolumeController controller, boolean visible) {
        try {
            getService().notifyVolumeControllerVisible(controller, visible);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isStreamAffectedByRingerMode(int streamType) {
        try {
            return getService().isStreamAffectedByRingerMode(streamType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isStreamAffectedByMute(int streamType) {
        try {
            return getService().isStreamAffectedByMute(streamType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void disableSafeMediaVolume() {
        try {
            getService().disableSafeMediaVolume(this.mApplicationContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setRingerModeInternal(int ringerMode) {
        try {
            if (DEBUG) {
                Log.d(TAG, "setRingerModeInternal(), ringerMode=" + ringerMode);
            }
            getService().setRingerModeInternal(ringerMode, getContext().getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getRingerModeInternal() {
        try {
            return getService().getRingerModeInternal();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setVolumePolicy(VolumePolicy policy) {
        try {
            if (DEBUG) {
                Log.d(TAG, "setVolumePolicy(), VolumePolicy=" + policy);
            }
            getService().setVolumePolicy(policy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int setHdmiSystemAudioSupported(boolean on) {
        try {
            if (DEBUG) {
                Log.d(TAG, "setHdmiSystemAudioSupported(" + on + NavigationBarInflaterView.KEY_CODE_END);
            }
            return getService().setHdmiSystemAudioSupported(on);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean isHdmiSystemAudioSupported() {
        try {
            return getService().isHdmiSystemAudioSupported();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static int listAudioPorts(ArrayList<AudioPort> ports) {
        return updateAudioPortCache(ports, null, null);
    }

    public static int listPreviousAudioPorts(ArrayList<AudioPort> ports) {
        return updateAudioPortCache(null, null, ports);
    }

    public static int listAudioDevicePorts(ArrayList<AudioDevicePort> devices) {
        if (devices == null) {
            return -2;
        }
        ArrayList<AudioPort> ports = new ArrayList<>();
        int status = updateAudioPortCache(ports, null, null);
        if (status == 0) {
            filterDevicePorts(ports, devices);
        }
        return status;
    }

    public static int listPreviousAudioDevicePorts(ArrayList<AudioDevicePort> devices) {
        if (devices == null) {
            return -2;
        }
        ArrayList<AudioPort> ports = new ArrayList<>();
        int status = updateAudioPortCache(null, null, ports);
        if (status == 0) {
            filterDevicePorts(ports, devices);
        }
        return status;
    }

    private static void filterDevicePorts(ArrayList<AudioPort> ports, ArrayList<AudioDevicePort> devices) {
        devices.clear();
        for (int i = 0; i < ports.size(); i++) {
            if (ports.get(i) instanceof AudioDevicePort) {
                devices.add((AudioDevicePort) ports.get(i));
            }
        }
    }

    public static int createAudioPatch(AudioPatch[] patch, AudioPortConfig[] sources, AudioPortConfig[] sinks) {
        if (DEBUG) {
            Log.d(TAG, "createAudioPatch()");
        }
        return AudioSystem.createAudioPatch(patch, sources, sinks);
    }

    public static int releaseAudioPatch(AudioPatch patch) {
        if (DEBUG) {
            Log.d(TAG, "releaseAudioPatch()");
        }
        return AudioSystem.releaseAudioPatch(patch);
    }

    public static int listAudioPatches(ArrayList<AudioPatch> patches) {
        return updateAudioPortCache(null, patches, null);
    }

    public static int setAudioPortGain(AudioPort port, AudioGainConfig gain) {
        if (port == null || gain == null) {
            return -2;
        }
        if (DEBUG) {
            Log.d(TAG, "setAudioPortGain() port=" + port + ",gain=" + gain);
        }
        AudioPortConfig activeConfig = port.activeConfig();
        AudioPortConfig config = new AudioPortConfig(port, activeConfig.samplingRate(), activeConfig.channelMask(), activeConfig.format(), gain);
        config.mConfigMask = 8;
        return AudioSystem.setAudioPortConfig(config);
    }

    public void registerAudioPortUpdateListener(OnAudioPortUpdateListener l) {
        AudioPortEventHandler audioPortEventHandler = sAudioPortEventHandler;
        audioPortEventHandler.init();
        audioPortEventHandler.registerListener(l);
    }

    public void unregisterAudioPortUpdateListener(OnAudioPortUpdateListener l) {
        sAudioPortEventHandler.unregisterListener(l);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int resetAudioPortGeneration() {
        int generation;
        synchronized (sAudioPortGeneration) {
            generation = sAudioPortGeneration.intValue();
            sAudioPortGeneration = 0;
        }
        return generation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x005b, code lost:
        if (r6[0] == r0[0]) goto L23;
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x005f, code lost:
        return -1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x0060, code lost:
        r10 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x0065, code lost:
        if (r10 >= r8.size()) goto L37;
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x0067, code lost:
        r11 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x0073, code lost:
        if (r11 >= r8.get(r10).sources().length) goto L30;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x0075, code lost:
        r12 = updatePortConfig(r8.get(r10).sources()[r11], r7);
        r8.get(r10).sources()[r11] = r12;
        r11 = r11 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x0094, code lost:
        r11 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x00a0, code lost:
        if (r11 >= r8.get(r10).sinks().length) goto L35;
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x00a2, code lost:
        r12 = updatePortConfig(r8.get(r10).sinks()[r11], r7);
        r8.get(r10).sinks()[r11] = r12;
        r11 = r11 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x00c1, code lost:
        r10 = r10 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x00c4, code lost:
        r10 = r8.iterator();
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x00cc, code lost:
        if (r10.hasNext() == false) goto L62;
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x00ce, code lost:
        r11 = r10.next();
        r12 = false;
        r13 = r11.sources();
        r14 = r13.length;
        r15 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x00db, code lost:
        if (r15 >= r14) goto L61;
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x00dd, code lost:
        r16 = r13[r15];
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x00df, code lost:
        if (r16 != null) goto L45;
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x00e1, code lost:
        r12 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x00e3, code lost:
        r15 = r15 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x00e6, code lost:
        r13 = r11.sinks();
        r14 = r13.length;
        r15 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:48:0x00ec, code lost:
        if (r15 >= r14) goto L60;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00ee, code lost:
        r16 = r13[r15];
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x00f0, code lost:
        if (r16 != null) goto L52;
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x00f2, code lost:
        r12 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x00f4, code lost:
        r15 = r15 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x00f7, code lost:
        if (r12 == false) goto L59;
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x00f9, code lost:
        r10.remove();
     */
    /* JADX WARN: Code restructure failed: missing block: B:56:0x00fd, code lost:
        android.media.AudioManager.sPreviousAudioPortsCached = android.media.AudioManager.sAudioPortsCached;
        android.media.AudioManager.sAudioPortsCached = r7;
        android.media.AudioManager.sAudioPatchesCached = r8;
        android.media.AudioManager.sAudioPortGeneration = java.lang.Integer.valueOf(r0[0]);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static int updateAudioPortCache(ArrayList<AudioPort> ports, ArrayList<AudioPatch> patches, ArrayList<AudioPort> previousPorts) {
        sAudioPortEventHandler.init();
        synchronized (sAudioPortGeneration) {
            if (sAudioPortGeneration.intValue() == 0) {
                int[] patchGeneration = new int[1];
                int[] portGeneration = new int[1];
                ArrayList<AudioPort> newPorts = new ArrayList<>();
                ArrayList<AudioPatch> newPatches = new ArrayList<>();
                while (true) {
                    newPorts.clear();
                    int status = AudioSystem.listAudioPorts(newPorts, portGeneration);
                    if (status != 0) {
                        Log.w(TAG, "updateAudioPortCache: listAudioPorts failed");
                        return status;
                    }
                    newPatches.clear();
                    int status2 = AudioSystem.listAudioPatches(newPatches, patchGeneration);
                    if (status2 != 0) {
                        Log.w(TAG, "updateAudioPortCache: listAudioPatches failed");
                        return status2;
                    } else if (patchGeneration[0] == portGeneration[0] || (ports != null && patches != null)) {
                        break;
                    }
                }
            }
            if (ports != null) {
                ports.clear();
                ports.addAll(sAudioPortsCached);
            }
            if (patches != null) {
                patches.clear();
                patches.addAll(sAudioPatchesCached);
            }
            if (previousPorts != null) {
                previousPorts.clear();
                previousPorts.addAll(sPreviousAudioPortsCached);
            }
            return 0;
        }
    }

    static AudioPortConfig updatePortConfig(AudioPortConfig portCfg, ArrayList<AudioPort> ports) {
        AudioPort port = portCfg.port();
        int k = 0;
        while (true) {
            if (k >= ports.size()) {
                break;
            } else if (!ports.get(k).handle().equals(port.handle())) {
                k++;
            } else {
                port = ports.get(k);
                break;
            }
        }
        if (k == ports.size()) {
            Log.e(TAG, "updatePortConfig port not found for handle: " + port.handle().id());
            return null;
        }
        AudioGainConfig gainCfg = portCfg.gain();
        if (gainCfg != null) {
            AudioGain gain = port.gain(gainCfg.index());
            gainCfg = gain.buildConfig(gainCfg.mode(), gainCfg.channelMask(), gainCfg.values(), gainCfg.rampDurationMs());
        }
        return port.buildConfig(portCfg.samplingRate(), portCfg.channelMask(), portCfg.format(), gainCfg);
    }

    private static boolean checkFlags(AudioDevicePort port, int flags) {
        if (port.role() != 2 || (flags & 2) == 0) {
            return port.role() == 1 && (flags & 1) != 0;
        }
        return true;
    }

    private static boolean checkTypes(AudioDevicePort port) {
        return AudioDeviceInfo.convertInternalDeviceToDeviceType(port.type()) != 0;
    }

    public AudioDeviceInfo[] getDevices(int flags) {
        return getDevicesStatic(flags);
    }

    private static AudioDeviceInfo[] infoListFromPortList(ArrayList<AudioDevicePort> ports, int flags) {
        int numRecs = 0;
        Iterator<AudioDevicePort> it = ports.iterator();
        while (it.hasNext()) {
            AudioDevicePort port = it.next();
            if (checkTypes(port) && checkFlags(port, flags)) {
                numRecs++;
            }
        }
        AudioDeviceInfo[] deviceList = new AudioDeviceInfo[numRecs];
        int slot = 0;
        Iterator<AudioDevicePort> it2 = ports.iterator();
        while (it2.hasNext()) {
            AudioDevicePort port2 = it2.next();
            if (checkTypes(port2) && checkFlags(port2, flags)) {
                deviceList[slot] = new AudioDeviceInfo(port2);
                slot++;
            }
        }
        return deviceList;
    }

    private static AudioDeviceInfo[] calcListDeltas(ArrayList<AudioDevicePort> ports_A, ArrayList<AudioDevicePort> ports_B, int flags) {
        ArrayList<AudioDevicePort> delta_ports = new ArrayList<>();
        for (int cur_index = 0; cur_index < ports_B.size(); cur_index++) {
            boolean cur_port_found = false;
            AudioDevicePort cur_port = ports_B.get(cur_index);
            for (int prev_index = 0; prev_index < ports_A.size() && !cur_port_found; prev_index++) {
                cur_port_found = cur_port.id() == ports_A.get(prev_index).id();
            }
            if (!cur_port_found) {
                delta_ports.add(cur_port);
            }
        }
        return infoListFromPortList(delta_ports, flags);
    }

    public static AudioDeviceInfo[] getDevicesStatic(int flags) {
        ArrayList<AudioDevicePort> ports = new ArrayList<>();
        int status = listAudioDevicePorts(ports);
        if (status != 0) {
            return new AudioDeviceInfo[0];
        }
        return infoListFromPortList(ports, flags);
    }

    public static AudioDeviceInfo getDeviceForPortId(int portId, int flags) {
        if (portId == 0) {
            return null;
        }
        AudioDeviceInfo[] devices = getDevicesStatic(flags);
        for (AudioDeviceInfo device : devices) {
            if (device.getId() == portId) {
                return device;
            }
        }
        return null;
    }

    public void registerAudioDeviceCallback(AudioDeviceCallback callback, Handler handler) {
        synchronized (this.mDeviceCallbacks) {
            if (callback != null) {
                if (!this.mDeviceCallbacks.containsKey(callback)) {
                    if (this.mDeviceCallbacks.size() == 0) {
                        if (this.mPortListener == null) {
                            this.mPortListener = new OnAmPortUpdateListener();
                        }
                        registerAudioPortUpdateListener(this.mPortListener);
                    }
                    NativeEventHandlerDelegate delegate = new NativeEventHandlerDelegate(callback, handler);
                    this.mDeviceCallbacks.put(callback, delegate);
                    broadcastDeviceListChange_sync(delegate.getHandler());
                }
            }
        }
    }

    public void unregisterAudioDeviceCallback(AudioDeviceCallback callback) {
        synchronized (this.mDeviceCallbacks) {
            if (this.mDeviceCallbacks.containsKey(callback)) {
                this.mDeviceCallbacks.remove(callback);
                if (this.mDeviceCallbacks.size() == 0) {
                    unregisterAudioPortUpdateListener(this.mPortListener);
                }
            }
        }
    }

    public static void setPortIdForMicrophones(ArrayList<MicrophoneInfo> microphones) {
        AudioDeviceInfo[] devices = getDevicesStatic(1);
        for (int i = microphones.size() - 1; i >= 0; i--) {
            boolean foundPortId = false;
            int length = devices.length;
            int i2 = 0;
            while (true) {
                if (i2 >= length) {
                    break;
                }
                AudioDeviceInfo device = devices[i2];
                if (device.getPort().type() != microphones.get(i).getInternalDeviceType() || !TextUtils.equals(device.getAddress(), microphones.get(i).getAddress())) {
                    i2++;
                } else {
                    microphones.get(i).setId(device.getId());
                    foundPortId = true;
                    break;
                }
            }
            if (!foundPortId) {
                Log.i(TAG, "Failed to find port id for device with type:" + microphones.get(i).getType() + " address:" + microphones.get(i).getAddress());
                microphones.remove(i);
            }
        }
    }

    public static MicrophoneInfo microphoneInfoFromAudioDeviceInfo(AudioDeviceInfo deviceInfo) {
        int micLocation;
        int deviceType = deviceInfo.getType();
        if (deviceType == 15 || deviceType == 18) {
            micLocation = 1;
        } else {
            micLocation = deviceType == 0 ? 0 : 3;
        }
        MicrophoneInfo microphone = new MicrophoneInfo(deviceInfo.getPort().name() + deviceInfo.getId(), deviceInfo.getPort().type(), deviceInfo.getAddress(), micLocation, -1, -1, MicrophoneInfo.POSITION_UNKNOWN, MicrophoneInfo.ORIENTATION_UNKNOWN, new ArrayList(), new ArrayList(), -3.4028235E38f, -3.4028235E38f, -3.4028235E38f, 0);
        microphone.setId(deviceInfo.getId());
        return microphone;
    }

    private void addMicrophonesFromAudioDeviceInfo(ArrayList<MicrophoneInfo> microphones, HashSet<Integer> filterTypes) {
        AudioDeviceInfo[] devices = getDevicesStatic(1);
        for (AudioDeviceInfo device : devices) {
            if (!filterTypes.contains(Integer.valueOf(device.getType()))) {
                MicrophoneInfo microphone = microphoneInfoFromAudioDeviceInfo(device);
                microphones.add(microphone);
            }
        }
    }

    public List<MicrophoneInfo> getMicrophones() throws IOException {
        ArrayList<MicrophoneInfo> microphones = new ArrayList<>();
        int status = AudioSystem.getMicrophones(microphones);
        HashSet<Integer> filterTypes = new HashSet<>();
        filterTypes.add(18);
        if (status != 0) {
            if (status != -3) {
                Log.e(TAG, "getMicrophones failed:" + status);
            }
            Log.i(TAG, "fallback on device info");
            addMicrophonesFromAudioDeviceInfo(microphones, filterTypes);
            return microphones;
        }
        setPortIdForMicrophones(microphones);
        filterTypes.add(15);
        addMicrophonesFromAudioDeviceInfo(microphones, filterTypes);
        return microphones;
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public List<BluetoothCodecConfig> getHwOffloadFormatsSupportedForA2dp() {
        ArrayList<Integer> formatsList = new ArrayList<>();
        ArrayList<BluetoothCodecConfig> codecConfigList = new ArrayList<>();
        int status = AudioSystem.getHwOffloadFormatsSupportedForBluetoothMedia(128, formatsList);
        if (status != 0) {
            Log.e(TAG, "getHwOffloadEncodingFormatsSupportedForA2DP failed:" + status);
            return codecConfigList;
        }
        Iterator<Integer> it = formatsList.iterator();
        while (it.hasNext()) {
            Integer format = it.next();
            int btSourceCodec = AudioSystem.audioFormatToBluetoothSourceCodec(format.intValue());
            if (DEBUG) {
                Log.d(TAG, " getHwOffloadFormatsSupportedForA2dp supported,Audio codec 0x" + Integer.toHexString(format.intValue()) + ", BT codec 0x" + Integer.toHexString(btSourceCodec));
            }
            if (btSourceCodec == 7) {
                codecConfigList.add(new BluetoothCodecConfig.Builder().setCodecType(6).build());
            }
            if (btSourceCodec != 1000000) {
                codecConfigList.add(new BluetoothCodecConfig.Builder().setCodecType(btSourceCodec).build());
            }
        }
        return codecConfigList;
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public List<BluetoothLeAudioCodecConfig> getHwOffloadFormatsSupportedForLeAudio() {
        ArrayList<Integer> formatsList = new ArrayList<>();
        ArrayList<BluetoothLeAudioCodecConfig> leAudioCodecConfigList = new ArrayList<>();
        int status = AudioSystem.getHwOffloadFormatsSupportedForBluetoothMedia(536870912, formatsList);
        if (status != 0) {
            Log.e(TAG, "getHwOffloadEncodingFormatsSupportedForLeAudio failed:" + status);
            return leAudioCodecConfigList;
        }
        Iterator<Integer> it = formatsList.iterator();
        while (it.hasNext()) {
            Integer format = it.next();
            int btLeAudioCodec = AudioSystem.audioFormatToBluetoothLeAudioSourceCodec(format.intValue());
            if (btLeAudioCodec != 1000000) {
                leAudioCodecConfigList.add(new BluetoothLeAudioCodecConfig.Builder().setCodecType(btLeAudioCodec).build());
            }
        }
        return leAudioCodecConfigList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void broadcastDeviceListChange_sync(Handler handler) {
        ArrayList<AudioDevicePort> current_ports = new ArrayList<>();
        int status = listAudioDevicePorts(current_ports);
        if (status != 0) {
            return;
        }
        if (handler != null) {
            AudioDeviceInfo[] deviceList = infoListFromPortList(current_ports, 3);
            handler.sendMessage(Message.obtain(handler, 0, deviceList));
        } else {
            AudioDeviceInfo[] added_devices = calcListDeltas(this.mPreviousPorts, current_ports, 3);
            AudioDeviceInfo[] removed_devices = calcListDeltas(current_ports, this.mPreviousPorts, 3);
            if (added_devices.length != 0 || removed_devices.length != 0) {
                for (int i = 0; i < this.mDeviceCallbacks.size(); i++) {
                    Handler handler2 = this.mDeviceCallbacks.valueAt(i).getHandler();
                    if (handler2 != null) {
                        if (removed_devices.length != 0) {
                            handler2.sendMessage(Message.obtain(handler2, 2, removed_devices));
                        }
                        if (added_devices.length != 0) {
                            handler2.sendMessage(Message.obtain(handler2, 1, added_devices));
                        }
                    }
                }
            }
        }
        this.mPreviousPorts = current_ports;
    }

    /* loaded from: classes2.dex */
    private class OnAmPortUpdateListener implements OnAudioPortUpdateListener {
        static final String TAG = "OnAmPortUpdateListener";

        private OnAmPortUpdateListener() {
        }

        @Override // android.media.AudioManager.OnAudioPortUpdateListener
        public void onAudioPortListUpdate(AudioPort[] portList) {
            synchronized (AudioManager.this.mDeviceCallbacks) {
                AudioManager.this.broadcastDeviceListChange_sync(null);
            }
        }

        @Override // android.media.AudioManager.OnAudioPortUpdateListener
        public void onAudioPatchListUpdate(AudioPatch[] patchList) {
        }

        @Override // android.media.AudioManager.OnAudioPortUpdateListener
        public void onServiceDied() {
            synchronized (AudioManager.this.mDeviceCallbacks) {
                AudioManager.this.broadcastDeviceListChange_sync(null);
            }
        }
    }

    @SystemApi
    /* loaded from: classes2.dex */
    public static abstract class AudioServerStateCallback {
        public void onAudioServerDown() {
        }

        public void onAudioServerUp() {
        }
    }

    /* renamed from: android.media.AudioManager$5  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass5 extends IAudioServerStateDispatcher.Stub {
        AnonymousClass5() {
        }

        @Override // android.media.IAudioServerStateDispatcher
        public void dispatchAudioServerStateChange(boolean state) {
            Executor exec;
            final AudioServerStateCallback cb;
            synchronized (AudioManager.this.mAudioServerStateCbLock) {
                exec = AudioManager.this.mAudioServerStateExec;
                cb = AudioManager.this.mAudioServerStateCb;
            }
            if (exec == null || cb == null) {
                return;
            }
            if (state) {
                exec.execute(new Runnable() { // from class: android.media.AudioManager$5$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        AudioManager.AudioServerStateCallback.this.onAudioServerUp();
                    }
                });
            } else {
                exec.execute(new Runnable() { // from class: android.media.AudioManager$5$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        AudioManager.AudioServerStateCallback.this.onAudioServerDown();
                    }
                });
            }
        }
    }

    @SystemApi
    public void setAudioServerStateCallback(Executor executor, AudioServerStateCallback stateCallback) {
        if (stateCallback == null) {
            throw new IllegalArgumentException("Illegal null AudioServerStateCallback");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Illegal null Executor for the AudioServerStateCallback");
        }
        synchronized (this.mAudioServerStateCbLock) {
            if (this.mAudioServerStateCb != null) {
                throw new IllegalStateException("setAudioServerStateCallback called with already registered callabck");
            }
            IAudioService service = getService();
            try {
                service.registerAudioServerStateDispatcher(this.mAudioServerStateDispatcher);
                this.mAudioServerStateExec = executor;
                this.mAudioServerStateCb = stateCallback;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @SystemApi
    public void clearAudioServerStateCallback() {
        synchronized (this.mAudioServerStateCbLock) {
            if (this.mAudioServerStateCb != null) {
                IAudioService service = getService();
                try {
                    service.unregisterAudioServerStateDispatcher(this.mAudioServerStateDispatcher);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            this.mAudioServerStateExec = null;
            this.mAudioServerStateCb = null;
        }
    }

    @SystemApi
    public boolean isAudioServerRunning() {
        IAudioService service = getService();
        try {
            return service.isAudioServerRunning();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setEncodedSurroundMode(int mode) {
        try {
            return getService().setEncodedSurroundMode(mode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getEncodedSurroundMode() {
        try {
            return getService().getEncodedSurroundMode(getContext().getApplicationInfo().targetSdkVersion);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Map<Integer, Boolean> getSurroundFormats() {
        try {
            return getService().getSurroundFormats();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setSurroundFormatEnabled(int audioFormat, boolean enabled) {
        try {
            return getService().setSurroundFormatEnabled(audioFormat, enabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSurroundFormatEnabled(int audioFormat) {
        try {
            return getService().isSurroundFormatEnabled(audioFormat);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<Integer> getReportedSurroundFormats() {
        try {
            return getService().getReportedSurroundFormats();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static boolean isHapticPlaybackSupported() {
        return AudioSystem.isHapticPlaybackSupported();
    }

    @SystemApi
    public boolean isUltrasoundSupported() {
        try {
            return getService().isUltrasoundSupported();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public static List<android.media.audiopolicy.AudioProductStrategy> getAudioProductStrategies() {
        IAudioService service = getService();
        try {
            return service.getAudioProductStrategies();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public static List<android.media.audiopolicy.AudioVolumeGroup> getAudioVolumeGroups() {
        IAudioService service = getService();
        try {
            return service.getAudioVolumeGroups();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    /* loaded from: classes2.dex */
    public static abstract class VolumeGroupCallback {
        public void onAudioVolumeGroupChanged(int group, int flags) {
        }
    }

    @SystemApi
    public void registerVolumeGroupCallback(Executor executor, VolumeGroupCallback callback) {
        Preconditions.checkNotNull(executor, "executor must not be null");
        Preconditions.checkNotNull(callback, "volume group change cb must not be null");
        AudioVolumeGroupChangeHandler audioVolumeGroupChangeHandler = sAudioAudioVolumeGroupChangedHandler;
        audioVolumeGroupChangeHandler.init();
        audioVolumeGroupChangeHandler.registerListener(callback);
    }

    @SystemApi
    public void unregisterVolumeGroupCallback(VolumeGroupCallback callback) {
        Preconditions.checkNotNull(callback, "volume group change cb must not be null");
        sAudioAudioVolumeGroupChangedHandler.unregisterListener(callback);
    }

    public static boolean hasHapticChannelsImpl(Context context, Uri uri) {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(context, uri, (Map<String, String>) null);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                if (format.containsKey(MediaFormat.KEY_HAPTIC_CHANNEL_COUNT) && format.getInteger(MediaFormat.KEY_HAPTIC_CHANNEL_COUNT) > 0) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            Log.e(TAG, "hasHapticChannels failure:" + e);
            return false;
        }
    }

    public static boolean hasHapticChannels(Context context, Uri uri) {
        Objects.requireNonNull(uri);
        if (context != null) {
            return hasHapticChannelsImpl(context, uri);
        }
        Context cachedContext = sContext.get();
        if (cachedContext != null) {
            if (DEBUG) {
                Log.d(TAG, "Try to use static context to query if having haptic channels");
            }
            return hasHapticChannelsImpl(cachedContext, uri);
        }
        if (DEBUG) {
            Log.d(TAG, "Try to use audio service context to query if having haptic channels");
        }
        try {
            return getService().hasHapticChannels(uri);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static void setRttEnabled(boolean rttEnabled) {
        try {
            getService().setRttEnabled(rttEnabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public void adjustSuggestedStreamVolumeForUid(int suggestedStreamType, int direction, int flags, String packageName, int uid, int pid, int targetSdkVersion) {
        try {
            getService().adjustSuggestedStreamVolumeForUid(suggestedStreamType, direction, flags, packageName, uid, pid, UserHandle.getUserHandleForUid(uid), targetSdkVersion);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public void adjustStreamVolumeForUid(int streamType, int direction, int flags, String packageName, int uid, int pid, int targetSdkVersion) {
        try {
            getService().adjustStreamVolumeForUid(streamType, direction, flags, packageName, uid, pid, UserHandle.getUserHandleForUid(uid), targetSdkVersion);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public void setStreamVolumeForUid(int streamType, int index, int flags, String packageName, int uid, int pid, int targetSdkVersion) {
        try {
            getService().setStreamVolumeForUid(streamType, index, flags, packageName, uid, pid, UserHandle.getUserHandleForUid(uid), targetSdkVersion);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMultiAudioFocusEnabled(boolean enabled) {
        try {
            getService().setMultiAudioFocusEnabled(enabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getAudioHwSyncForSession(int sessionId) {
        int hwSyncId = AudioSystem.getAudioHwSyncForSession(sessionId);
        if (hwSyncId == 0) {
            throw new UnsupportedOperationException("HW A/V synchronization is not supported.");
        }
        return hwSyncId;
    }

    public boolean setCommunicationDevice(AudioDeviceInfo device) {
        Objects.requireNonNull(device);
        try {
            if (device.getId() == 0) {
                throw new IllegalArgumentException("In valid device: " + device);
            }
            if (DEBUG) {
                Log.d(TAG, "setCommunicationDevice() device.getId(), devicename=" + AudioSystem.getOutputDeviceName(device.getInternalType()));
            }
            return getService().setCommunicationDevice(this.mICallBack, device.getId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearCommunicationDevice() {
        if (DEBUG) {
            Log.d(TAG, "clearCommunicationDevice()");
        }
        try {
            getService().setCommunicationDevice(this.mICallBack, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public AudioDeviceInfo getCommunicationDevice() {
        try {
            return getDeviceForPortId(getService().getCommunicationDevice(), 2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<AudioDeviceInfo> getAvailableCommunicationDevices() {
        try {
            ArrayList<AudioDeviceInfo> devices = new ArrayList<>();
            int[] portIds = getService().getAvailableCommunicationDeviceIds();
            for (int portId : portIds) {
                AudioDeviceInfo device = getDeviceForPortId(portId, 2);
                if (device != null) {
                    devices.add(device);
                }
            }
            return devices;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<AudioProfile> getDirectProfilesForAttributes(AudioAttributes attributes) {
        Objects.requireNonNull(attributes);
        ArrayList<AudioProfile> audioProfilesList = new ArrayList<>();
        int status = AudioSystem.getDirectProfilesForAttributes(attributes, audioProfilesList);
        if (status != 0) {
            Log.w(TAG, "getDirectProfilesForAttributes failed.");
            return new ArrayList();
        }
        return audioProfilesList;
    }

    public static AudioDeviceInfo getDeviceInfoFromType(int deviceType) {
        return getDeviceInfoFromTypeAndAddress(deviceType, null);
    }

    public static AudioDeviceInfo getDeviceInfoFromTypeAndAddress(int type, String address) {
        AudioDeviceInfo[] devices = getDevicesStatic(2);
        AudioDeviceInfo deviceForType = null;
        for (AudioDeviceInfo device : devices) {
            if (device.getType() == type) {
                deviceForType = device;
                if (address == null || address.equals(device.getAddress())) {
                    return device;
                }
            }
        }
        return deviceForType;
    }

    public void addOnCommunicationDeviceChangedListener(Executor executor, OnCommunicationDeviceChangedListener listener) {
        this.mCommDeviceChangedListenerMgr.addListener(executor, listener, "addOnCommunicationDeviceChangedListener", new Supplier() { // from class: android.media.AudioManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return AudioManager.this.m2007x4bc1ca96();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addOnCommunicationDeviceChangedListener$1$android-media-AudioManager  reason: not valid java name */
    public /* synthetic */ CallbackUtil.DispatcherStub m2007x4bc1ca96() {
        return new CommunicationDeviceDispatcherStub();
    }

    public void removeOnCommunicationDeviceChangedListener(OnCommunicationDeviceChangedListener listener) {
        this.mCommDeviceChangedListenerMgr.removeListener(listener, "removeOnCommunicationDeviceChangedListener");
    }

    /* loaded from: classes2.dex */
    private final class CommunicationDeviceDispatcherStub extends ICommunicationDeviceDispatcher.Stub implements CallbackUtil.DispatcherStub {
        private CommunicationDeviceDispatcherStub() {
        }

        @Override // android.media.CallbackUtil.DispatcherStub
        public void register(boolean register) {
            try {
                if (register) {
                    AudioManager.getService().registerCommunicationDeviceDispatcher(this);
                } else {
                    AudioManager.getService().unregisterCommunicationDeviceDispatcher(this);
                }
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }

        @Override // android.media.ICommunicationDeviceDispatcher
        public void dispatchCommunicationDeviceChanged(int portId) {
            final AudioDeviceInfo device = AudioManager.getDeviceForPortId(portId, 2);
            AudioManager.this.mCommDeviceChangedListenerMgr.callListeners(new CallbackUtil.CallbackMethod() { // from class: android.media.AudioManager$CommunicationDeviceDispatcherStub$$ExternalSyntheticLambda0
                @Override // android.media.CallbackUtil.CallbackMethod
                public final void callbackMethod(Object obj) {
                    ((AudioManager.OnCommunicationDeviceChangedListener) obj).onCommunicationDeviceChanged(AudioDeviceInfo.this);
                }
            });
        }
    }

    @SystemApi
    public boolean isPstnCallAudioInterceptable() {
        IAudioService service = getService();
        try {
            return service.isPstnCallAudioInterceptable();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private int getCallRedirectMode() {
        int mode = getMode();
        if (mode == 2 || mode == 4 || mode == 5) {
            return 1;
        }
        if (mode == 3 || mode == 6) {
            return 2;
        }
        return 0;
    }

    private void checkCallRedirectionFormat(AudioFormat format, boolean isOutput) {
        if (format.getEncoding() != 2 && format.getEncoding() != 4) {
            throw new UnsupportedOperationException(" Unsupported encoding ");
        }
        if (format.getSampleRate() < 8000 || format.getSampleRate() > 48000) {
            throw new UnsupportedOperationException(" Unsupported sample rate ");
        }
        if (isOutput && format.getChannelMask() != 4 && format.getChannelMask() != 12) {
            throw new UnsupportedOperationException(" Unsupported output channel mask ");
        }
        if (!isOutput && format.getChannelMask() != 16 && format.getChannelMask() != 12) {
            throw new UnsupportedOperationException(" Unsupported input channel mask ");
        }
    }

    /* loaded from: classes2.dex */
    class CallIRedirectionClientInfo {
        public int redirectMode;
        public WeakReference trackOrRecord;

        CallIRedirectionClientInfo() {
        }
    }

    @SystemApi
    public AudioTrack getCallUplinkInjectionAudioTrack(AudioFormat format) {
        Objects.requireNonNull(format);
        checkCallRedirectionFormat(format, true);
        int redirectMode = getCallRedirectMode();
        if (redirectMode == 0) {
            throw new IllegalStateException(" not available in mode " + AudioSystem.modeToString(getMode()));
        }
        if (redirectMode == 1 && !isPstnCallAudioInterceptable()) {
            throw new UnsupportedOperationException(" PSTN Call audio not accessible ");
        }
        AudioTrack track = new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setSystemUsage(17).setContentType(1).build()).setAudioFormat(format).setCallRedirectionMode(redirectMode).build();
        if (track != null && track.getState() != 0) {
            synchronized (this.mCallRedirectionLock) {
                if (this.mCallRedirectionModeListener == null) {
                    this.mCallRedirectionModeListener = new CallInjectionModeChangedListener();
                    try {
                        addOnModeChangedListener(Executors.newSingleThreadExecutor(), this.mCallRedirectionModeListener);
                        this.mCallIRedirectionClients = new ArrayList<>();
                    } catch (Exception e) {
                        Log.e(TAG, "addOnModeChangedListener failed with exception: " + e);
                        this.mCallRedirectionModeListener = null;
                        throw new UnsupportedOperationException(" Cannot register mode listener ");
                    }
                }
                CallIRedirectionClientInfo info = new CallIRedirectionClientInfo();
                info.redirectMode = redirectMode;
                info.trackOrRecord = new WeakReference(track);
                this.mCallIRedirectionClients.add(info);
            }
            return track;
        }
        throw new UnsupportedOperationException(" Cannot create the AudioTrack");
    }

    @SystemApi
    public AudioRecord getCallDownlinkExtractionAudioRecord(AudioFormat format) {
        Objects.requireNonNull(format);
        checkCallRedirectionFormat(format, false);
        int redirectMode = getCallRedirectMode();
        if (redirectMode == 0) {
            throw new IllegalStateException(" not available in mode " + AudioSystem.modeToString(getMode()));
        }
        if (redirectMode == 1 && !isPstnCallAudioInterceptable()) {
            throw new UnsupportedOperationException(" PSTN Call audio not accessible ");
        }
        AudioRecord record = new AudioRecord.Builder().setAudioAttributes(new AudioAttributes.Builder().setInternalCapturePreset(3).build()).setAudioFormat(format).setCallRedirectionMode(redirectMode).build();
        if (record != null && record.getState() != 0) {
            synchronized (this.mCallRedirectionLock) {
                if (this.mCallRedirectionModeListener == null) {
                    this.mCallRedirectionModeListener = new CallInjectionModeChangedListener();
                    try {
                        addOnModeChangedListener(Executors.newSingleThreadExecutor(), this.mCallRedirectionModeListener);
                        this.mCallIRedirectionClients = new ArrayList<>();
                    } catch (Exception e) {
                        Log.e(TAG, "addOnModeChangedListener failed with exception: " + e);
                        this.mCallRedirectionModeListener = null;
                        throw new UnsupportedOperationException(" Cannot register mode listener ");
                    }
                }
                CallIRedirectionClientInfo info = new CallIRedirectionClientInfo();
                info.redirectMode = redirectMode;
                info.trackOrRecord = new WeakReference(record);
                this.mCallIRedirectionClients.add(info);
            }
            return record;
        }
        throw new UnsupportedOperationException(" Cannot create the AudioRecord");
    }

    /* loaded from: classes2.dex */
    class CallInjectionModeChangedListener implements OnModeChangedListener {
        CallInjectionModeChangedListener() {
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [8380=4] */
        @Override // android.media.AudioManager.OnModeChangedListener
        public void onModeChanged(int mode) {
            AudioManager audioManager;
            synchronized (AudioManager.this.mCallRedirectionLock) {
                ArrayList<CallIRedirectionClientInfo> clientInfos = (ArrayList) AudioManager.this.mCallIRedirectionClients.clone();
                Iterator<CallIRedirectionClientInfo> it = clientInfos.iterator();
                while (it.hasNext()) {
                    CallIRedirectionClientInfo info = it.next();
                    Object trackOrRecord = info.trackOrRecord.get();
                    if (trackOrRecord != null && ((info.redirectMode == 1 && mode != 2 && mode != 4 && mode != 5) || (info.redirectMode == 2 && mode != 3 && mode != 6))) {
                        if (trackOrRecord instanceof AudioTrack) {
                            AudioTrack track = (AudioTrack) trackOrRecord;
                            track.release();
                        } else {
                            AudioRecord record = (AudioRecord) trackOrRecord;
                            record.release();
                        }
                        AudioManager.this.mCallIRedirectionClients.remove(info);
                    }
                }
                if (AudioManager.this.mCallIRedirectionClients.isEmpty()) {
                    try {
                        if (AudioManager.this.mCallRedirectionModeListener != null) {
                            AudioManager audioManager2 = AudioManager.this;
                            audioManager2.removeOnModeChangedListener(audioManager2.mCallRedirectionModeListener);
                        }
                        AudioManager.this.mCallRedirectionModeListener = null;
                        audioManager = AudioManager.this;
                    } catch (Exception e) {
                        Log.e(AudioManager.TAG, "removeOnModeChangedListener failed with exception: " + e);
                        AudioManager.this.mCallRedirectionModeListener = null;
                        audioManager = AudioManager.this;
                    }
                    audioManager.mCallIRedirectionClients = null;
                }
            }
        }
    }

    @SystemApi
    public void muteAwaitConnection(int[] usagesToMute, AudioDeviceAttributes device, long timeout, TimeUnit timeUnit) throws IllegalStateException {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be greater than 0");
        }
        Objects.requireNonNull(usagesToMute);
        if (usagesToMute.length == 0) {
            throw new IllegalArgumentException("Array of usages to mute cannot be empty");
        }
        Objects.requireNonNull(device);
        Objects.requireNonNull(timeUnit);
        try {
            getService().muteAwaitConnection(usagesToMute, device, timeUnit.toMillis(timeout));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public AudioDeviceAttributes getMutingExpectedDevice() {
        try {
            return getService().getMutingExpectedDevice();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void cancelMuteAwaitConnection(AudioDeviceAttributes device) throws IllegalStateException {
        Objects.requireNonNull(device);
        try {
            getService().cancelMuteAwaitConnection(device);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    /* loaded from: classes2.dex */
    public static abstract class MuteAwaitConnectionCallback {
        public static final int EVENT_CANCEL = 3;
        public static final int EVENT_CONNECTION = 1;
        public static final int EVENT_TIMEOUT = 2;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes2.dex */
        public @interface UnmuteEvent {
        }

        public void onMutedUntilConnection(AudioDeviceAttributes device, int[] mutedUsages) {
        }

        public void onUnmutedEvent(int unmuteEvent, AudioDeviceAttributes device, int[] mutedUsages) {
        }
    }

    @SystemApi
    public void registerMuteAwaitConnectionCallback(Executor executor, MuteAwaitConnectionCallback callback) {
        synchronized (this.mMuteAwaitConnectionListenerLock) {
            Pair<ArrayList<CallbackUtil.ListenerInfo<MuteAwaitConnectionCallback>>, MuteAwaitConnectionDispatcherStub> res = CallbackUtil.addListener("registerMuteAwaitConnectionCallback", executor, callback, this.mMuteAwaitConnectionListeners, this.mMuteAwaitConnDispatcherStub, new Supplier() { // from class: android.media.AudioManager$$ExternalSyntheticLambda2
                @Override // java.util.function.Supplier
                public final Object get() {
                    return AudioManager.this.m2009x38cb074();
                }
            }, new Consumer() { // from class: android.media.AudioManager$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AudioManager.MuteAwaitConnectionDispatcherStub) obj).register(true);
                }
            });
            this.mMuteAwaitConnectionListeners = (ArrayList) res.first;
            this.mMuteAwaitConnDispatcherStub = (MuteAwaitConnectionDispatcherStub) res.second;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerMuteAwaitConnectionCallback$2$android-media-AudioManager  reason: not valid java name */
    public /* synthetic */ MuteAwaitConnectionDispatcherStub m2009x38cb074() {
        return new MuteAwaitConnectionDispatcherStub();
    }

    @SystemApi
    public void unregisterMuteAwaitConnectionCallback(MuteAwaitConnectionCallback callback) {
        synchronized (this.mMuteAwaitConnectionListenerLock) {
            Pair<ArrayList<CallbackUtil.ListenerInfo<MuteAwaitConnectionCallback>>, MuteAwaitConnectionDispatcherStub> res = CallbackUtil.removeListener("unregisterMuteAwaitConnectionCallback", callback, this.mMuteAwaitConnectionListeners, this.mMuteAwaitConnDispatcherStub, new Consumer() { // from class: android.media.AudioManager$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AudioManager.MuteAwaitConnectionDispatcherStub) obj).register(false);
                }
            });
            this.mMuteAwaitConnectionListeners = (ArrayList) res.first;
            this.mMuteAwaitConnDispatcherStub = (MuteAwaitConnectionDispatcherStub) res.second;
        }
    }

    @SystemApi
    public void addAssistantServicesUids(int[] assistantUids) {
        try {
            getService().addAssistantServicesUids(assistantUids);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void removeAssistantServicesUids(int[] assistantUids) {
        try {
            getService().removeAssistantServicesUids(assistantUids);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int[] getAssistantServicesUids() {
        try {
            int[] uids = getService().getAssistantServicesUids();
            return Arrays.copyOf(uids, uids.length);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setActiveAssistantServiceUids(int[] assistantUids) {
        try {
            getService().setActiveAssistantServiceUids(assistantUids);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int[] getActiveAssistantServicesUids() {
        try {
            int[] uids = getService().getActiveAssistantServiceUids();
            return Arrays.copyOf(uids, uids.length);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static String getHalVersion() {
        try {
            return getService().getHalVersion();
        } catch (RemoteException e) {
            Log.e(TAG, "Error querying getHalVersion", e);
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class MuteAwaitConnectionDispatcherStub extends IMuteAwaitConnectionCallback.Stub {
        private MuteAwaitConnectionDispatcherStub() {
        }

        public void register(boolean register) {
            try {
                AudioManager.getService().registerMuteAwaitConnectionDispatcher(this, register);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        @Override // android.media.IMuteAwaitConnectionCallback
        public void dispatchOnMutedUntilConnection(final AudioDeviceAttributes device, final int[] mutedUsages) {
            CallbackUtil.callListeners(AudioManager.this.mMuteAwaitConnectionListeners, AudioManager.this.mMuteAwaitConnectionListenerLock, new CallbackUtil.CallbackMethod() { // from class: android.media.AudioManager$MuteAwaitConnectionDispatcherStub$$ExternalSyntheticLambda1
                @Override // android.media.CallbackUtil.CallbackMethod
                public final void callbackMethod(Object obj) {
                    ((AudioManager.MuteAwaitConnectionCallback) obj).onMutedUntilConnection(AudioDeviceAttributes.this, mutedUsages);
                }
            });
        }

        @Override // android.media.IMuteAwaitConnectionCallback
        public void dispatchOnUnmutedEvent(final int event, final AudioDeviceAttributes device, final int[] mutedUsages) {
            CallbackUtil.callListeners(AudioManager.this.mMuteAwaitConnectionListeners, AudioManager.this.mMuteAwaitConnectionListenerLock, new CallbackUtil.CallbackMethod() { // from class: android.media.AudioManager$MuteAwaitConnectionDispatcherStub$$ExternalSyntheticLambda0
                @Override // android.media.CallbackUtil.CallbackMethod
                public final void callbackMethod(Object obj) {
                    ((AudioManager.MuteAwaitConnectionCallback) obj).onUnmutedEvent(event, device, mutedUsages);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class NativeEventHandlerDelegate {
        private final Handler mHandler;

        NativeEventHandlerDelegate(final AudioDeviceCallback callback, Handler handler) {
            Looper looper;
            if (handler != null) {
                looper = handler.getLooper();
            } else {
                looper = Looper.getMainLooper();
            }
            if (looper != null) {
                this.mHandler = new Handler(looper) { // from class: android.media.AudioManager.NativeEventHandlerDelegate.1
                    @Override // android.os.Handler
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case 0:
                            case 1:
                                AudioDeviceCallback audioDeviceCallback = callback;
                                if (audioDeviceCallback != null) {
                                    audioDeviceCallback.onAudioDevicesAdded((AudioDeviceInfo[]) msg.obj);
                                    return;
                                }
                                return;
                            case 2:
                                AudioDeviceCallback audioDeviceCallback2 = callback;
                                if (audioDeviceCallback2 != null) {
                                    audioDeviceCallback2.onAudioDevicesRemoved((AudioDeviceInfo[]) msg.obj);
                                    return;
                                }
                                return;
                            default:
                                Log.e(AudioManager.TAG, "Unknown native event type: " + msg.what);
                                return;
                        }
                    }
                };
            } else {
                this.mHandler = null;
            }
        }

        Handler getHandler() {
            return this.mHandler;
        }
    }

    public String getCurrentAudioFocusPackageName() {
        IAudioService service = getService();
        try {
            return service.getCurrentAudioFocusPackageName();
        } catch (RemoteException e) {
            return null;
        }
    }
}
