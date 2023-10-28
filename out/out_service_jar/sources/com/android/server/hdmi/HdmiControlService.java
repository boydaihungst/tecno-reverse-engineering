package com.android.server.hdmi;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiHotplugEvent;
import android.hardware.hdmi.HdmiPortInfo;
import android.hardware.hdmi.IHdmiCecSettingChangeListener;
import android.hardware.hdmi.IHdmiCecVolumeControlFeatureListener;
import android.hardware.hdmi.IHdmiControlCallback;
import android.hardware.hdmi.IHdmiControlService;
import android.hardware.hdmi.IHdmiControlStatusChangeListener;
import android.hardware.hdmi.IHdmiDeviceEventListener;
import android.hardware.hdmi.IHdmiHotplugEventListener;
import android.hardware.hdmi.IHdmiInputChangeListener;
import android.hardware.hdmi.IHdmiMhlVendorCommandListener;
import android.hardware.hdmi.IHdmiRecordListener;
import android.hardware.hdmi.IHdmiSystemAudioModeChangeListener;
import android.hardware.hdmi.IHdmiVendorCommandListener;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceVolumeManager;
import android.media.AudioManager;
import android.media.VolumeInfo;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.sysprop.HdmiProperties;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiCecConfig;
import com.android.server.hdmi.HdmiCecController;
import com.android.server.hdmi.HdmiCecLocalDevice;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.power.ShutdownThread;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import libcore.util.EmptyArray;
/* loaded from: classes.dex */
public class HdmiControlService extends SystemService {
    static final AudioDeviceAttributes AUDIO_OUTPUT_DEVICE_HDMI;
    static final AudioDeviceAttributes AUDIO_OUTPUT_DEVICE_HDMI_ARC;
    static final AudioDeviceAttributes AUDIO_OUTPUT_DEVICE_HDMI_EARC;
    private static final List<AudioDeviceAttributes> AVC_AUDIO_OUTPUT_DEVICES;
    static final int INITIATED_BY_BOOT_UP = 1;
    static final int INITIATED_BY_ENABLE_CEC = 0;
    static final int INITIATED_BY_HOTPLUG = 4;
    static final int INITIATED_BY_SCREEN_ON = 2;
    static final int INITIATED_BY_WAKE_UP_MESSAGE = 3;
    static final String PERMISSION = "android.permission.HDMI_CEC";
    static final int STANDBY_SCREEN_OFF = 0;
    static final int STANDBY_SHUTDOWN = 1;
    static final AudioAttributes STREAM_MUSIC_ATTRIBUTES;
    private static final String TAG = "HdmiControlService";
    static final int WAKE_UP_BOOT_UP = 1;
    static final int WAKE_UP_SCREEN_ON = 0;
    private AbsoluteVolumeChangedListener mAbsoluteVolumeChangedListener;
    @HdmiAnnotations.ServiceThreadOnly
    private int mActivePortId;
    protected final HdmiCecLocalDevice.ActiveSource mActiveSource;
    private boolean mAddressAllocated;
    private HdmiCecAtomWriter mAtomWriter;
    private Map<AudioDeviceAttributes, Integer> mAudioDeviceVolumeBehaviors;
    private AudioDeviceVolumeManagerWrapperInterface mAudioDeviceVolumeManager;
    private AudioManager mAudioManager;
    private HdmiCecController mCecController;
    private CecMessageBuffer mCecMessageBuffer;
    private int mCecVersion;
    private final ArrayList<DeviceEventListenerRecord> mDeviceEventListenerRecords;
    private DisplayManager mDisplayManager;
    private IHdmiControlCallback mDisplayStatusCallback;
    private final Handler mHandler;
    private HdmiCecConfig mHdmiCecConfig;
    private HdmiCecNetwork mHdmiCecNetwork;
    private final ArrayMap<String, RemoteCallbackList<IHdmiCecSettingChangeListener>> mHdmiCecSettingChangeListenerRecords;
    private int mHdmiCecVolumeControl;
    private final RemoteCallbackList<IHdmiCecVolumeControlFeatureListener> mHdmiCecVolumeControlFeatureListenerRecords;
    private final HdmiControlBroadcastReceiver mHdmiControlBroadcastReceiver;
    private int mHdmiControlEnabled;
    private final ArrayList<HdmiControlStatusChangeListenerRecord> mHdmiControlStatusChangeListenerRecords;
    private final ArrayList<HotplugEventListenerRecord> mHotplugEventListenerRecords;
    private InputChangeListenerRecord mInputChangeListenerRecord;
    private Looper mIoLooper;
    private final HandlerThread mIoThread;
    private boolean mIsCecAvailable;
    @HdmiAnnotations.ServiceThreadOnly
    private int mLastInputMhl;
    private final List<Integer> mLocalDevices;
    private final Object mLock;
    @HdmiAnnotations.ServiceThreadOnly
    private String mMenuLanguage;
    private HdmiMhlControllerStub mMhlController;
    private List<HdmiDeviceInfo> mMhlDevices;
    private boolean mMhlInputChangeEnabled;
    private final ArrayList<HdmiMhlVendorCommandListenerRecord> mMhlVendorCommandListenerRecords;
    private IHdmiControlCallback mOtpCallbackPendingAddressAllocation;
    private PowerManagerWrapper mPowerManager;
    private PowerManagerInternalWrapper mPowerManagerInternal;
    private HdmiCecPowerStatusController mPowerStatusController;
    private boolean mProhibitMode;
    private HdmiRecordListenerRecord mRecordListenerRecord;
    private final SelectRequestBuffer mSelectRequestBuffer;
    private final Executor mServiceThreadExecutor;
    private HdmiCecConfig.SettingChangeListener mSettingChangeListener;
    private final SettingsObserver mSettingsObserver;
    @HdmiAnnotations.ServiceThreadOnly
    private boolean mStandbyMessageReceived;
    private int mStreamMusicMaxVolume;
    private boolean mSystemAudioActivated;
    private final ArrayList<SystemAudioModeChangeListenerRecord> mSystemAudioModeChangeListenerRecords;
    private TvInputManager mTvInputManager;
    private final ArrayList<VendorCommandListenerRecord> mVendorCommandListenerRecords;
    @HdmiAnnotations.ServiceThreadOnly
    private boolean mWakeUpMessageReceived;
    private static final Locale HONG_KONG = new Locale("zh", "HK");
    private static final Locale MACAU = new Locale("zh", "MO");
    private static final Map<String, String> sTerminologyToBibliographicMap = createsTerminologyToBibliographicMap();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface DevicePollingCallback {
        void onPollingFinished(List<Integer> list);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface SendMessageCallback {
        void onSendCompleted(int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface WakeReason {
    }

    static {
        AudioDeviceAttributes audioDeviceAttributes = new AudioDeviceAttributes(2, 9, "");
        AUDIO_OUTPUT_DEVICE_HDMI = audioDeviceAttributes;
        AudioDeviceAttributes audioDeviceAttributes2 = new AudioDeviceAttributes(2, 10, "");
        AUDIO_OUTPUT_DEVICE_HDMI_ARC = audioDeviceAttributes2;
        AudioDeviceAttributes audioDeviceAttributes3 = new AudioDeviceAttributes(2, 29, "");
        AUDIO_OUTPUT_DEVICE_HDMI_EARC = audioDeviceAttributes3;
        AVC_AUDIO_OUTPUT_DEVICES = Collections.unmodifiableList(Arrays.asList(audioDeviceAttributes, audioDeviceAttributes2, audioDeviceAttributes3));
        STREAM_MUSIC_ATTRIBUTES = new AudioAttributes.Builder().setLegacyStreamType(3).build();
    }

    private static Map<String, String> createsTerminologyToBibliographicMap() {
        Map<String, String> temp = new HashMap<>();
        temp.put("sqi", "alb");
        temp.put("hye", "arm");
        temp.put("eus", "baq");
        temp.put("mya", "bur");
        temp.put("ces", "cze");
        temp.put("nld", "dut");
        temp.put("kat", "geo");
        temp.put("deu", "ger");
        temp.put("ell", "gre");
        temp.put("fra", "fre");
        temp.put("isl", "ice");
        temp.put("mkd", "mac");
        temp.put("mri", "mao");
        temp.put("msa", "may");
        temp.put("fas", "per");
        temp.put("ron", "rum");
        temp.put("slk", "slo");
        temp.put("bod", "tib");
        temp.put("cym", "wel");
        return Collections.unmodifiableMap(temp);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String localeToMenuLanguage(Locale locale) {
        if (locale.equals(Locale.TAIWAN) || locale.equals(HONG_KONG) || locale.equals(MACAU)) {
            return "chi";
        }
        String language = locale.getISO3Language();
        Map<String, String> map = sTerminologyToBibliographicMap;
        if (map.containsKey(language)) {
            return map.get(language);
        }
        return language;
    }

    Executor getServiceThreadExecutor() {
        return this.mServiceThreadExecutor;
    }

    /* loaded from: classes.dex */
    private class HdmiControlBroadcastReceiver extends BroadcastReceiver {
        private HdmiControlBroadcastReceiver() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        @HdmiAnnotations.ServiceThreadOnly
        public void onReceive(Context context, Intent intent) {
            char c;
            HdmiControlService.this.assertRunOnServiceThread();
            boolean isReboot = SystemProperties.get(ShutdownThread.SHUTDOWN_ACTION_PROPERTY).contains("1");
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -2128145023:
                    if (action.equals("android.intent.action.SCREEN_OFF")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -1454123155:
                    if (action.equals("android.intent.action.SCREEN_ON")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 158859398:
                    if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1947666138:
                    if (action.equals("android.intent.action.ACTION_SHUTDOWN")) {
                        c = 3;
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
                    if (HdmiControlService.this.isPowerOnOrTransient() && !isReboot) {
                        HdmiControlService.this.onStandby(0);
                        return;
                    }
                    return;
                case 1:
                    if (HdmiControlService.this.isPowerStandbyOrTransient()) {
                        HdmiControlService.this.onWakeUp(0);
                        return;
                    }
                    return;
                case 2:
                    String language = HdmiControlService.localeToMenuLanguage(Locale.getDefault());
                    if (!HdmiControlService.this.mMenuLanguage.equals(language)) {
                        HdmiControlService.this.onLanguageChanged(language);
                        return;
                    }
                    return;
                case 3:
                    if (HdmiControlService.this.isPowerOnOrTransient() && !isReboot) {
                        HdmiControlService.this.onStandby(1);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    HdmiControlService(Context context, List<Integer> deviceTypes, AudioDeviceVolumeManagerWrapperInterface audioDeviceVolumeManager) {
        super(context);
        this.mServiceThreadExecutor = new Executor() { // from class: com.android.server.hdmi.HdmiControlService.1
            @Override // java.util.concurrent.Executor
            public void execute(Runnable r) {
                HdmiControlService.this.runOnServiceThread(r);
            }
        };
        this.mActiveSource = new HdmiCecLocalDevice.ActiveSource();
        this.mSystemAudioActivated = false;
        this.mAudioDeviceVolumeBehaviors = new HashMap();
        this.mIoThread = new HandlerThread("Hdmi Control Io Thread");
        this.mLock = new Object();
        this.mHdmiControlStatusChangeListenerRecords = new ArrayList<>();
        this.mHdmiCecVolumeControlFeatureListenerRecords = new RemoteCallbackList<>();
        this.mHotplugEventListenerRecords = new ArrayList<>();
        this.mDeviceEventListenerRecords = new ArrayList<>();
        this.mVendorCommandListenerRecords = new ArrayList<>();
        this.mHdmiCecSettingChangeListenerRecords = new ArrayMap<>();
        this.mSystemAudioModeChangeListenerRecords = new ArrayList<>();
        Handler handler = new Handler();
        this.mHandler = handler;
        this.mHdmiControlBroadcastReceiver = new HdmiControlBroadcastReceiver();
        this.mDisplayStatusCallback = null;
        this.mOtpCallbackPendingAddressAllocation = null;
        this.mMenuLanguage = localeToMenuLanguage(Locale.getDefault());
        this.mStandbyMessageReceived = false;
        this.mWakeUpMessageReceived = false;
        this.mActivePortId = -1;
        this.mMhlVendorCommandListenerRecords = new ArrayList<>();
        this.mLastInputMhl = -1;
        this.mAddressAllocated = false;
        this.mIsCecAvailable = false;
        this.mAtomWriter = new HdmiCecAtomWriter();
        this.mSelectRequestBuffer = new SelectRequestBuffer();
        this.mSettingChangeListener = new AnonymousClass18();
        this.mLocalDevices = deviceTypes;
        this.mSettingsObserver = new SettingsObserver(handler);
        this.mHdmiCecConfig = new HdmiCecConfig(context);
        this.mAudioDeviceVolumeManager = audioDeviceVolumeManager;
    }

    public HdmiControlService(Context context) {
        super(context);
        this.mServiceThreadExecutor = new Executor() { // from class: com.android.server.hdmi.HdmiControlService.1
            @Override // java.util.concurrent.Executor
            public void execute(Runnable r) {
                HdmiControlService.this.runOnServiceThread(r);
            }
        };
        this.mActiveSource = new HdmiCecLocalDevice.ActiveSource();
        this.mSystemAudioActivated = false;
        this.mAudioDeviceVolumeBehaviors = new HashMap();
        this.mIoThread = new HandlerThread("Hdmi Control Io Thread");
        this.mLock = new Object();
        this.mHdmiControlStatusChangeListenerRecords = new ArrayList<>();
        this.mHdmiCecVolumeControlFeatureListenerRecords = new RemoteCallbackList<>();
        this.mHotplugEventListenerRecords = new ArrayList<>();
        this.mDeviceEventListenerRecords = new ArrayList<>();
        this.mVendorCommandListenerRecords = new ArrayList<>();
        this.mHdmiCecSettingChangeListenerRecords = new ArrayMap<>();
        this.mSystemAudioModeChangeListenerRecords = new ArrayList<>();
        Handler handler = new Handler();
        this.mHandler = handler;
        this.mHdmiControlBroadcastReceiver = new HdmiControlBroadcastReceiver();
        this.mDisplayStatusCallback = null;
        this.mOtpCallbackPendingAddressAllocation = null;
        this.mMenuLanguage = localeToMenuLanguage(Locale.getDefault());
        this.mStandbyMessageReceived = false;
        this.mWakeUpMessageReceived = false;
        this.mActivePortId = -1;
        this.mMhlVendorCommandListenerRecords = new ArrayList<>();
        this.mLastInputMhl = -1;
        this.mAddressAllocated = false;
        this.mIsCecAvailable = false;
        this.mAtomWriter = new HdmiCecAtomWriter();
        this.mSelectRequestBuffer = new SelectRequestBuffer();
        this.mSettingChangeListener = new AnonymousClass18();
        this.mLocalDevices = readDeviceTypes();
        this.mSettingsObserver = new SettingsObserver(handler);
        this.mHdmiCecConfig = new HdmiCecConfig(context);
    }

    protected List<HdmiProperties.cec_device_types_values> getCecDeviceTypes() {
        return HdmiProperties.cec_device_types();
    }

    protected List<Integer> getDeviceTypes() {
        return HdmiProperties.device_type();
    }

    protected List<Integer> readDeviceTypes() {
        List<HdmiProperties.cec_device_types_values> cecDeviceTypes = getCecDeviceTypes();
        if (!cecDeviceTypes.isEmpty()) {
            if (cecDeviceTypes.contains(null)) {
                Slog.w(TAG, "Error parsing ro.hdmi.cec_device_types: " + SystemProperties.get("ro.hdmi.cec_device_types"));
            }
            return (List) cecDeviceTypes.stream().map(new Function() { // from class: com.android.server.hdmi.HdmiControlService$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    Integer enumToIntDeviceType;
                    enumToIntDeviceType = HdmiControlService.enumToIntDeviceType((HdmiProperties.cec_device_types_values) obj);
                    return enumToIntDeviceType;
                }
            }).filter(new Predicate() { // from class: com.android.server.hdmi.HdmiControlService$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return Objects.nonNull((Integer) obj);
                }
            }).collect(Collectors.toList());
        }
        List<Integer> deviceTypes = getDeviceTypes();
        if (deviceTypes.contains(null)) {
            Slog.w(TAG, "Error parsing ro.hdmi.device_type: " + SystemProperties.get("ro.hdmi.device_type"));
        }
        return (List) deviceTypes.stream().filter(new Predicate() { // from class: com.android.server.hdmi.HdmiControlService$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Objects.nonNull((Integer) obj);
            }
        }).collect(Collectors.toList());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Integer enumToIntDeviceType(HdmiProperties.cec_device_types_values cecDeviceType) {
        if (cecDeviceType == null) {
            return null;
        }
        switch (AnonymousClass19.$SwitchMap$android$sysprop$HdmiProperties$cec_device_types_values[cecDeviceType.ordinal()]) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
                return 3;
            case 5:
                return 4;
            case 6:
                return 5;
            case 7:
                return 6;
            case 8:
                return 7;
            default:
                Slog.w(TAG, "Unrecognized device type in ro.hdmi.cec_device_types: " + cecDeviceType.getPropValue());
                return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.hdmi.HdmiControlService$19  reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass19 {
        static final /* synthetic */ int[] $SwitchMap$android$sysprop$HdmiProperties$cec_device_types_values;

        static {
            int[] iArr = new int[HdmiProperties.cec_device_types_values.values().length];
            $SwitchMap$android$sysprop$HdmiProperties$cec_device_types_values = iArr;
            try {
                iArr[HdmiProperties.cec_device_types_values.TV.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$sysprop$HdmiProperties$cec_device_types_values[HdmiProperties.cec_device_types_values.RECORDING_DEVICE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$sysprop$HdmiProperties$cec_device_types_values[HdmiProperties.cec_device_types_values.RESERVED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$sysprop$HdmiProperties$cec_device_types_values[HdmiProperties.cec_device_types_values.TUNER.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$sysprop$HdmiProperties$cec_device_types_values[HdmiProperties.cec_device_types_values.PLAYBACK_DEVICE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$sysprop$HdmiProperties$cec_device_types_values[HdmiProperties.cec_device_types_values.AUDIO_SYSTEM.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$sysprop$HdmiProperties$cec_device_types_values[HdmiProperties.cec_device_types_values.PURE_CEC_SWITCH.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$android$sysprop$HdmiProperties$cec_device_types_values[HdmiProperties.cec_device_types_values.VIDEO_PROCESSOR.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
        }
    }

    protected static List<Integer> getIntList(String string) {
        ArrayList<Integer> list = new ArrayList<>();
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
        splitter.setString(string);
        Iterator<String> it = splitter.iterator();
        while (it.hasNext()) {
            String item = it.next();
            try {
                list.add(Integer.valueOf(Integer.parseInt(item)));
            } catch (NumberFormatException e) {
                Slog.w(TAG, "Can't parseInt: " + item);
            }
        }
        return Collections.unmodifiableList(list);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        initService();
        publishBinderService("hdmi_control", new BinderService());
        if (this.mCecController != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.ACTION_SHUTDOWN");
            filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
            getContext().registerReceiver(this.mHdmiControlBroadcastReceiver, filter);
            registerContentObserver();
        }
        this.mMhlController.setOption(104, 1);
    }

    void initService() {
        if (this.mIoLooper == null) {
            this.mIoThread.start();
            this.mIoLooper = this.mIoThread.getLooper();
        }
        if (this.mPowerStatusController == null) {
            this.mPowerStatusController = new HdmiCecPowerStatusController(this);
        }
        this.mPowerStatusController.setPowerStatus(getInitialPowerStatus());
        this.mProhibitMode = false;
        this.mHdmiControlEnabled = this.mHdmiCecConfig.getIntValue("hdmi_cec_enabled");
        setHdmiCecVolumeControlEnabledInternal(getHdmiCecConfig().getIntValue("volume_control_enabled"));
        this.mMhlInputChangeEnabled = readBooleanSetting("mhl_input_switching_enabled", true);
        if (this.mCecMessageBuffer == null) {
            this.mCecMessageBuffer = new CecMessageBuffer(this);
        }
        if (this.mCecController == null) {
            this.mCecController = HdmiCecController.create(this, getAtomWriter());
        }
        if (this.mCecController == null) {
            Slog.i(TAG, "Device does not support HDMI-CEC.");
            return;
        }
        if (this.mMhlController == null) {
            this.mMhlController = HdmiMhlControllerStub.create(this);
        }
        if (!this.mMhlController.isReady()) {
            Slog.i(TAG, "Device does not support MHL-control.");
        }
        this.mHdmiCecNetwork = new HdmiCecNetwork(this, this.mCecController, this.mMhlController);
        if (this.mHdmiControlEnabled != 1) {
            this.mCecController.setOption(2, false);
        } else {
            initializeCec(1);
        }
        this.mMhlDevices = Collections.emptyList();
        this.mHdmiCecNetwork.initPortInfo();
        this.mHdmiCecConfig.registerChangeListener("hdmi_cec_enabled", new HdmiCecConfig.SettingChangeListener() { // from class: com.android.server.hdmi.HdmiControlService.2
            @Override // com.android.server.hdmi.HdmiCecConfig.SettingChangeListener
            public void onChange(String setting) {
                int enabled = HdmiControlService.this.mHdmiCecConfig.getIntValue("hdmi_cec_enabled");
                HdmiControlService.this.setControlEnabled(enabled);
            }
        }, this.mServiceThreadExecutor);
        this.mHdmiCecConfig.registerChangeListener("hdmi_cec_version", new HdmiCecConfig.SettingChangeListener() { // from class: com.android.server.hdmi.HdmiControlService.3
            @Override // com.android.server.hdmi.HdmiCecConfig.SettingChangeListener
            public void onChange(String setting) {
                HdmiControlService.this.initializeCec(0);
            }
        }, this.mServiceThreadExecutor);
        this.mHdmiCecConfig.registerChangeListener("routing_control", new HdmiCecConfig.SettingChangeListener() { // from class: com.android.server.hdmi.HdmiControlService.4
            @Override // com.android.server.hdmi.HdmiCecConfig.SettingChangeListener
            public void onChange(String setting) {
                boolean enabled = HdmiControlService.this.mHdmiCecConfig.getIntValue("routing_control") == 1;
                if (HdmiControlService.this.isAudioSystemDevice()) {
                    if (HdmiControlService.this.audioSystem() == null) {
                        Slog.w(HdmiControlService.TAG, "Switch device has not registered yet. Can't turn routing on.");
                    } else {
                        HdmiControlService.this.audioSystem().setRoutingControlFeatureEnabled(enabled);
                    }
                }
            }
        }, this.mServiceThreadExecutor);
        this.mHdmiCecConfig.registerChangeListener("system_audio_control", new HdmiCecConfig.SettingChangeListener() { // from class: com.android.server.hdmi.HdmiControlService.5
            @Override // com.android.server.hdmi.HdmiCecConfig.SettingChangeListener
            public void onChange(String setting) {
                boolean enabled = HdmiControlService.this.mHdmiCecConfig.getIntValue("system_audio_control") == 1;
                if (HdmiControlService.this.isTvDeviceEnabled()) {
                    HdmiControlService.this.tv().setSystemAudioControlFeatureEnabled(enabled);
                }
                if (HdmiControlService.this.isAudioSystemDevice()) {
                    if (HdmiControlService.this.audioSystem() == null) {
                        Slog.e(HdmiControlService.TAG, "Audio System device has not registered yet. Can't turn system audio mode on.");
                    } else {
                        HdmiControlService.this.audioSystem().onSystemAudioControlFeatureSupportChanged(enabled);
                    }
                }
            }
        }, this.mServiceThreadExecutor);
        this.mHdmiCecConfig.registerChangeListener("volume_control_enabled", new HdmiCecConfig.SettingChangeListener() { // from class: com.android.server.hdmi.HdmiControlService.6
            @Override // com.android.server.hdmi.HdmiCecConfig.SettingChangeListener
            public void onChange(String setting) {
                HdmiControlService hdmiControlService = HdmiControlService.this;
                hdmiControlService.setHdmiCecVolumeControlEnabledInternal(hdmiControlService.getHdmiCecConfig().getIntValue("volume_control_enabled"));
            }
        }, this.mServiceThreadExecutor);
        this.mHdmiCecConfig.registerChangeListener("tv_wake_on_one_touch_play", new HdmiCecConfig.SettingChangeListener() { // from class: com.android.server.hdmi.HdmiControlService.7
            @Override // com.android.server.hdmi.HdmiCecConfig.SettingChangeListener
            public void onChange(String setting) {
                if (HdmiControlService.this.isTvDeviceEnabled()) {
                    HdmiControlService hdmiControlService = HdmiControlService.this;
                    hdmiControlService.setCecOption(1, hdmiControlService.tv().getAutoWakeup());
                }
            }
        }, this.mServiceThreadExecutor);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isScreenOff() {
        return this.mDisplayManager.getDisplay(0).getState() == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bootCompleted() {
        if (this.mPowerManager.isInteractive() && isPowerStandbyOrTransient()) {
            this.mPowerStatusController.setPowerStatus(0);
            if (this.mAddressAllocated) {
                for (HdmiCecLocalDevice localDevice : getAllLocalDevices()) {
                    localDevice.startQueuedActions();
                }
            }
        }
    }

    int getInitialPowerStatus() {
        return 3;
    }

    void setCecController(HdmiCecController cecController) {
        this.mCecController = cecController;
    }

    void setHdmiCecNetwork(HdmiCecNetwork hdmiCecNetwork) {
        this.mHdmiCecNetwork = hdmiCecNetwork;
    }

    void setHdmiCecConfig(HdmiCecConfig hdmiCecConfig) {
        this.mHdmiCecConfig = hdmiCecConfig;
    }

    public HdmiCecNetwork getHdmiCecNetwork() {
        return this.mHdmiCecNetwork;
    }

    void setHdmiMhlController(HdmiMhlControllerStub hdmiMhlController) {
        this.mMhlController = hdmiMhlController;
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mDisplayManager = (DisplayManager) getContext().getSystemService(DisplayManager.class);
            this.mTvInputManager = (TvInputManager) getContext().getSystemService("tv_input");
            this.mPowerManager = new PowerManagerWrapper(getContext());
            this.mPowerManagerInternal = new PowerManagerInternalWrapper();
            this.mAudioManager = (AudioManager) getContext().getSystemService("audio");
            this.mStreamMusicMaxVolume = getAudioManager().getStreamMaxVolume(3);
            if (this.mAudioDeviceVolumeManager == null) {
                this.mAudioDeviceVolumeManager = new AudioDeviceVolumeManagerWrapper(getContext());
            }
            getAudioDeviceVolumeManager().addOnDeviceVolumeBehaviorChangedListener(this.mServiceThreadExecutor, new AudioDeviceVolumeManager.OnDeviceVolumeBehaviorChangedListener() { // from class: com.android.server.hdmi.HdmiControlService$$ExternalSyntheticLambda3
                public final void onDeviceVolumeBehaviorChanged(AudioDeviceAttributes audioDeviceAttributes, int i) {
                    HdmiControlService.this.onDeviceVolumeBehaviorChanged(audioDeviceAttributes, i);
                }
            });
        } else if (phase == 1000) {
            runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    HdmiControlService.this.bootCompleted();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TvInputManager getTvInputManager() {
        return this.mTvInputManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerTvInputCallback(TvInputManager.TvInputCallback callback) {
        TvInputManager tvInputManager = this.mTvInputManager;
        if (tvInputManager == null) {
            return;
        }
        tvInputManager.registerCallback(callback, this.mHandler);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterTvInputCallback(TvInputManager.TvInputCallback callback) {
        TvInputManager tvInputManager = this.mTvInputManager;
        if (tvInputManager == null) {
            return;
        }
        tvInputManager.unregisterCallback(callback);
    }

    void setPowerManager(PowerManagerWrapper powerManager) {
        this.mPowerManager = powerManager;
    }

    void setPowerManagerInternal(PowerManagerInternalWrapper powerManagerInternal) {
        this.mPowerManagerInternal = powerManagerInternal;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PowerManagerWrapper getPowerManager() {
        return this.mPowerManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PowerManagerInternalWrapper getPowerManagerInternal() {
        return this.mPowerManagerInternal;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onInitializeCecComplete(int initiatedBy) {
        updatePowerStatusOnInitializeCecComplete();
        this.mWakeUpMessageReceived = false;
        if (isTvDeviceEnabled()) {
            this.mCecController.setOption(1, tv().getAutoWakeup());
        }
        int reason = -1;
        switch (initiatedBy) {
            case 0:
                reason = 1;
                break;
            case 1:
                reason = 0;
                break;
            case 2:
                reason = 2;
                List<HdmiCecLocalDevice> devices = getAllLocalDevices();
                for (HdmiCecLocalDevice device : devices) {
                    device.onInitializeCecComplete(initiatedBy);
                }
                break;
            case 3:
                reason = 2;
                break;
        }
        if (reason != -1) {
            invokeVendorCommandListenersOnControlStateChanged(true, reason);
            announceHdmiControlStatusChange(1);
        }
    }

    private void updatePowerStatusOnInitializeCecComplete() {
        if (this.mPowerStatusController.isPowerStatusTransientToOn()) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    HdmiControlService.this.m3851xb44924a3();
                }
            });
        } else if (this.mPowerStatusController.isPowerStatusTransientToStandby()) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    HdmiControlService.this.m3852x77358e02();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updatePowerStatusOnInitializeCecComplete$0$com-android-server-hdmi-HdmiControlService  reason: not valid java name */
    public /* synthetic */ void m3851xb44924a3() {
        this.mPowerStatusController.setPowerStatus(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updatePowerStatusOnInitializeCecComplete$1$com-android-server-hdmi-HdmiControlService  reason: not valid java name */
    public /* synthetic */ void m3852x77358e02() {
        this.mPowerStatusController.setPowerStatus(1);
    }

    private void registerContentObserver() {
        ContentResolver resolver = getContext().getContentResolver();
        String[] settings = {"mhl_input_switching_enabled", "mhl_power_charge_enabled", "device_name"};
        for (String s : settings) {
            resolver.registerContentObserver(Settings.Global.getUriFor(s), false, this.mSettingsObserver, -1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code restructure failed: missing block: B:6:0x001a, code lost:
            if (r0.equals("mhl_power_charge_enabled") != false) goto L5;
         */
        @Override // android.database.ContentObserver
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void onChange(boolean selfChange, Uri uri) {
            String option = uri.getLastPathSegment();
            char c = 1;
            boolean enabled = HdmiControlService.this.readBooleanSetting(option, true);
            switch (option.hashCode()) {
                case -1543071020:
                    if (option.equals("device_name")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -1262529811:
                    if (option.equals("mhl_input_switching_enabled")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -885757826:
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    HdmiControlService.this.setMhlInputChangeEnabled(enabled);
                    return;
                case 1:
                    HdmiControlService.this.mMhlController.setOption(102, HdmiControlService.toInt(enabled));
                    return;
                case 2:
                    String deviceName = HdmiControlService.this.readStringSetting(option, Build.MODEL);
                    HdmiControlService.this.setDisplayName(deviceName);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int toInt(boolean enabled) {
        return enabled ? 1 : 0;
    }

    boolean readBooleanSetting(String key, boolean defVal) {
        ContentResolver cr = getContext().getContentResolver();
        return Settings.Global.getInt(cr, key, toInt(defVal)) == 1;
    }

    int readIntSetting(String key, int defVal) {
        ContentResolver cr = getContext().getContentResolver();
        return Settings.Global.getInt(cr, key, defVal);
    }

    void writeBooleanSetting(String key, boolean value) {
        ContentResolver cr = getContext().getContentResolver();
        Settings.Global.putInt(cr, key, toInt(value));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void writeStringSystemProperty(String key, String value) {
        SystemProperties.set(key, value);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean readBooleanSystemProperty(String key, boolean defVal) {
        return SystemProperties.getBoolean(key, defVal);
    }

    String readStringSetting(String key, String defVal) {
        ContentResolver cr = getContext().getContentResolver();
        String content = Settings.Global.getString(cr, key);
        if (TextUtils.isEmpty(content)) {
            return defVal;
        }
        return content;
    }

    void writeStringSetting(String key, String value) {
        ContentResolver cr = getContext().getContentResolver();
        Settings.Global.putString(cr, key, value);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initializeCec(int initiatedBy) {
        this.mAddressAllocated = false;
        int settingsCecVersion = getHdmiCecConfig().getIntValue("hdmi_cec_version");
        int supportedCecVersion = this.mCecController.getVersion();
        this.mCecVersion = Math.max(5, Math.min(settingsCecVersion, supportedCecVersion));
        this.mCecController.setOption(3, true);
        this.mCecController.setLanguage(this.mMenuLanguage);
        initializeLocalDevices(initiatedBy);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void initializeLocalDevices(int initiatedBy) {
        assertRunOnServiceThread();
        ArrayList<HdmiCecLocalDevice> localDevices = new ArrayList<>();
        for (Integer num : this.mLocalDevices) {
            int type = num.intValue();
            HdmiCecLocalDevice localDevice = this.mHdmiCecNetwork.getLocalDevice(type);
            if (localDevice == null) {
                localDevice = HdmiCecLocalDevice.create(this, type);
            }
            localDevice.init();
            localDevices.add(localDevice);
        }
        clearLocalDevices();
        allocateLogicalAddress(localDevices, initiatedBy);
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected void allocateLogicalAddress(final ArrayList<HdmiCecLocalDevice> allocatingDevices, final int initiatedBy) {
        assertRunOnServiceThread();
        this.mCecController.clearLogicalAddress();
        final ArrayList<HdmiCecLocalDevice> allocatedDevices = new ArrayList<>();
        final int[] finished = new int[1];
        this.mAddressAllocated = allocatingDevices.isEmpty();
        this.mSelectRequestBuffer.clear();
        Iterator<HdmiCecLocalDevice> it = allocatingDevices.iterator();
        while (it.hasNext()) {
            final HdmiCecLocalDevice localDevice = it.next();
            this.mCecController.allocateLogicalAddress(localDevice.getType(), localDevice.getPreferredAddress(), new HdmiCecController.AllocateAddressCallback() { // from class: com.android.server.hdmi.HdmiControlService.8
                @Override // com.android.server.hdmi.HdmiCecController.AllocateAddressCallback
                public void onAllocated(int deviceType, int logicalAddress) {
                    if (logicalAddress == 15) {
                        Slog.e(HdmiControlService.TAG, "Failed to allocate address:[device_type:" + deviceType + "]");
                    } else {
                        HdmiControlService hdmiControlService = HdmiControlService.this;
                        HdmiDeviceInfo deviceInfo = hdmiControlService.createDeviceInfo(logicalAddress, deviceType, 0, hdmiControlService.getCecVersion());
                        localDevice.setDeviceInfo(deviceInfo);
                        HdmiControlService.this.mHdmiCecNetwork.addLocalDevice(deviceType, localDevice);
                        HdmiControlService.this.mCecController.addLogicalAddress(logicalAddress);
                        allocatedDevices.add(localDevice);
                    }
                    int size = allocatingDevices.size();
                    int[] iArr = finished;
                    int i = iArr[0] + 1;
                    iArr[0] = i;
                    if (size == i) {
                        HdmiControlService.this.mAddressAllocated = true;
                        int i2 = initiatedBy;
                        if (i2 != 4) {
                            HdmiControlService.this.onInitializeCecComplete(i2);
                        }
                        HdmiControlService.this.notifyAddressAllocated(allocatedDevices, initiatedBy);
                        if (HdmiControlService.this.mDisplayStatusCallback != null) {
                            HdmiControlService hdmiControlService2 = HdmiControlService.this;
                            hdmiControlService2.queryDisplayStatus(hdmiControlService2.mDisplayStatusCallback);
                            HdmiControlService.this.mDisplayStatusCallback = null;
                        }
                        if (HdmiControlService.this.mOtpCallbackPendingAddressAllocation != null) {
                            HdmiControlService hdmiControlService3 = HdmiControlService.this;
                            hdmiControlService3.oneTouchPlay(hdmiControlService3.mOtpCallbackPendingAddressAllocation);
                            HdmiControlService.this.mOtpCallbackPendingAddressAllocation = null;
                        }
                        HdmiControlService.this.mCecMessageBuffer.processMessages();
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void notifyAddressAllocated(ArrayList<HdmiCecLocalDevice> devices, int initiatedBy) {
        assertRunOnServiceThread();
        Iterator<HdmiCecLocalDevice> it = devices.iterator();
        while (it.hasNext()) {
            HdmiCecLocalDevice device = it.next();
            int address = device.getDeviceInfo().getLogicalAddress();
            device.handleAddressAllocated(address, initiatedBy);
        }
        if (isTvDeviceEnabled()) {
            tv().setSelectRequestBuffer(this.mSelectRequestBuffer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAddressAllocated() {
        return this.mAddressAllocated;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<HdmiPortInfo> getPortInfo() {
        List<HdmiPortInfo> portInfo;
        synchronized (this.mLock) {
            portInfo = this.mHdmiCecNetwork.getPortInfo();
        }
        return portInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiPortInfo getPortInfo(int portId) {
        return this.mHdmiCecNetwork.getPortInfo(portId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int portIdToPath(int portId) {
        return this.mHdmiCecNetwork.portIdToPath(portId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int pathToPortId(int path) {
        return this.mHdmiCecNetwork.physicalAddressToPortId(path);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isValidPortId(int portId) {
        return this.mHdmiCecNetwork.getPortInfo(portId) != null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Looper getIoLooper() {
        return this.mIoLooper;
    }

    void setIoLooper(Looper ioLooper) {
        this.mIoLooper = ioLooper;
    }

    void setCecMessageBuffer(CecMessageBuffer cecMessageBuffer) {
        this.mCecMessageBuffer = cecMessageBuffer;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Looper getServiceLooper() {
        return this.mHandler.getLooper();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPhysicalAddress() {
        return this.mCecController.getPhysicalAddress();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getVendorId() {
        return this.mCecController.getVendorId();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public HdmiDeviceInfo getDeviceInfo(int logicalAddress) {
        assertRunOnServiceThread();
        return this.mHdmiCecNetwork.getCecDeviceInfo(logicalAddress);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public HdmiDeviceInfo getDeviceInfoByPort(int port) {
        assertRunOnServiceThread();
        HdmiMhlLocalDeviceStub info = this.mMhlController.getLocalDevice(port);
        if (info != null) {
            return info.getInfo();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int getCecVersion() {
        return this.mCecVersion;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isConnectedToArcPort(int physicalAddress) {
        return this.mHdmiCecNetwork.isConnectedToArcPort(physicalAddress);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean isConnected(int portId) {
        assertRunOnServiceThread();
        return this.mCecController.isConnected(portId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void runOnServiceThread(Runnable runnable) {
        this.mHandler.post(new WorkSourceUidPreservingRunnable(runnable));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void assertRunOnServiceThread() {
        if (Looper.myLooper() != this.mHandler.getLooper()) {
            throw new IllegalStateException("Should run on service thread.");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void sendCecCommand(HdmiCecMessage command, SendMessageCallback callback) {
        assertRunOnServiceThread();
        if (command.getValidationResult() == 0 && verifyPhysicalAddresses(command)) {
            this.mCecController.sendCommand(command, callback);
            return;
        }
        HdmiLogger.error("Invalid message type:" + command, new Object[0]);
        if (callback != null) {
            callback.onSendCompleted(3);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void sendCecCommand(HdmiCecMessage command) {
        assertRunOnServiceThread();
        sendCecCommand(command, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void maySendFeatureAbortCommand(HdmiCecMessage command, int reason) {
        assertRunOnServiceThread();
        this.mCecController.maySendFeatureAbortCommand(command, reason);
    }

    boolean verifyPhysicalAddresses(HdmiCecMessage message) {
        byte[] params = message.getParams();
        switch (message.getOpcode()) {
            case 112:
                return params.length == 0 || verifyPhysicalAddress(params, 0);
            case 128:
                return verifyPhysicalAddress(params, 0) && verifyPhysicalAddress(params, 2);
            case 129:
            case 130:
            case 132:
            case 134:
            case 157:
                return verifyPhysicalAddress(params, 0);
            case 161:
            case 162:
                return verifyExternalSourcePhysicalAddress(params, 7);
            default:
                return true;
        }
    }

    private boolean verifyPhysicalAddress(byte[] params, int offset) {
        if (isTvDevice()) {
            int path = HdmiUtils.twoBytesToInt(params, offset);
            if (path == 65535 || path != getPhysicalAddress()) {
                int portId = pathToPortId(path);
                return portId != -1;
            }
            return true;
        }
        return true;
    }

    private boolean verifyExternalSourcePhysicalAddress(byte[] params, int offset) {
        int externalSourceSpecifier = params[offset];
        int offset2 = offset + 1;
        if (externalSourceSpecifier != 5 || params.length - offset2 < 2) {
            return true;
        }
        return verifyPhysicalAddress(params, offset2);
    }

    private boolean sourceAddressIsLocal(HdmiCecMessage message) {
        for (HdmiCecLocalDevice device : getAllLocalDevices()) {
            synchronized (device.mLock) {
                if (message.getSource() == device.getDeviceInfo().getLogicalAddress() && message.getSource() != 15) {
                    HdmiLogger.warning("Unexpected source: message sent from device itself, " + message, new Object[0]);
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @HdmiAnnotations.ServiceThreadOnly
    public int handleCecCommand(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int validationResult = message.getValidationResult();
        if (validationResult == 3 || !verifyPhysicalAddresses(message)) {
            return 3;
        }
        if (validationResult != 0 || sourceAddressIsLocal(message)) {
            return -1;
        }
        getHdmiCecNetwork().handleCecMessage(message);
        int handleMessageResult = dispatchMessageToLocalDevice(message);
        if (handleMessageResult == -2 && !this.mAddressAllocated && this.mCecMessageBuffer.bufferMessage(message)) {
            return -1;
        }
        return handleMessageResult;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enableAudioReturnChannel(int portId, boolean enabled) {
        this.mCecController.enableAudioReturnChannel(portId, enabled);
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int dispatchMessageToLocalDevice(HdmiCecMessage message) {
        assertRunOnServiceThread();
        for (HdmiCecLocalDevice device : this.mHdmiCecNetwork.getLocalDeviceList()) {
            int messageResult = device.dispatchMessage(message);
            if (messageResult != -2 && message.getDestination() != 15) {
                return messageResult;
            }
        }
        if (message.getDestination() == 15) {
            return -1;
        }
        HdmiLogger.warning("Unhandled cec command:" + message, new Object[0]);
        return -2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void onHotplug(int portId, boolean connected) {
        assertRunOnServiceThread();
        this.mHdmiCecNetwork.initPortInfo();
        if (connected && !isTvDevice() && getPortInfo(portId).getType() == 1) {
            ArrayList<HdmiCecLocalDevice> localDevices = new ArrayList<>();
            for (Integer num : this.mLocalDevices) {
                int type = num.intValue();
                HdmiCecLocalDevice localDevice = this.mHdmiCecNetwork.getLocalDevice(type);
                if (localDevice == null) {
                    localDevice = HdmiCecLocalDevice.create(this, type);
                    localDevice.init();
                }
                localDevices.add(localDevice);
            }
            allocateLogicalAddress(localDevices, 4);
        }
        for (HdmiCecLocalDevice device : this.mHdmiCecNetwork.getLocalDeviceList()) {
            device.onHotplug(portId, connected);
        }
        announceHotplugEvent(portId, connected);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void pollDevices(DevicePollingCallback callback, int sourceAddress, int pickStrategy, int retryCount) {
        assertRunOnServiceThread();
        this.mCecController.pollDevices(callback, sourceAddress, checkPollStrategy(pickStrategy), retryCount);
    }

    private int checkPollStrategy(int pickStrategy) {
        int strategy = pickStrategy & 3;
        if (strategy == 0) {
            throw new IllegalArgumentException("Invalid poll strategy:" + pickStrategy);
        }
        int iterationStrategy = 196608 & pickStrategy;
        if (iterationStrategy == 0) {
            throw new IllegalArgumentException("Invalid iteration strategy:" + pickStrategy);
        }
        return strategy | iterationStrategy;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<HdmiCecLocalDevice> getAllLocalDevices() {
        assertRunOnServiceThread();
        return this.mHdmiCecNetwork.getLocalDeviceList();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void checkLogicalAddressConflictAndReallocate(int logicalAddress, int physicalAddress) {
        if (physicalAddress == getPhysicalAddress()) {
            return;
        }
        for (HdmiCecLocalDevice device : getAllLocalDevices()) {
            if (device.getDeviceInfo().getLogicalAddress() == logicalAddress) {
                HdmiLogger.debug("allocate logical address for " + device.getDeviceInfo(), new Object[0]);
                ArrayList<HdmiCecLocalDevice> localDevices = new ArrayList<>();
                localDevices.add(device);
                allocateLogicalAddress(localDevices, 4);
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Object getServiceLock() {
        return this.mLock;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAudioStatus(boolean mute, int volume) {
        if (!isTvDeviceEnabled() || !tv().isSystemAudioActivated() || !tv().isArcEstablished() || getHdmiCecVolumeControl() == 0) {
            return;
        }
        AudioManager audioManager = getAudioManager();
        boolean muted = audioManager.isStreamMute(3);
        if (mute) {
            if (!muted) {
                audioManager.setStreamMute(3, true);
                return;
            }
            return;
        }
        if (muted) {
            audioManager.setStreamMute(3, false);
        }
        if (volume >= 0 && volume <= 100) {
            Slog.i(TAG, "volume: " + volume);
            int flag = 1 | 256;
            audioManager.setStreamVolume(3, volume, flag);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void announceSystemAudioModeChange(boolean enabled) {
        synchronized (this.mLock) {
            Iterator<SystemAudioModeChangeListenerRecord> it = this.mSystemAudioModeChangeListenerRecords.iterator();
            while (it.hasNext()) {
                SystemAudioModeChangeListenerRecord record = it.next();
                invokeSystemAudioModeChangeLocked(record.mListener, enabled);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public HdmiDeviceInfo createDeviceInfo(int logicalAddress, int deviceType, int powerStatus, int cecVersion) {
        String displayName = readStringSetting("device_name", Build.MODEL);
        return HdmiDeviceInfo.cecDeviceBuilder().setLogicalAddress(logicalAddress).setPhysicalAddress(getPhysicalAddress()).setPortId(pathToPortId(getPhysicalAddress())).setDeviceType(deviceType).setVendorId(getVendorId()).setDisplayName(displayName).setDevicePowerStatus(powerStatus).setCecVersion(cecVersion).build();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDisplayName(String newDisplayName) {
        for (HdmiCecLocalDevice device : getAllLocalDevices()) {
            HdmiDeviceInfo deviceInfo = device.getDeviceInfo();
            if (!deviceInfo.getDisplayName().equals(newDisplayName)) {
                synchronized (device.mLock) {
                    device.setDeviceInfo(deviceInfo.toBuilder().setDisplayName(newDisplayName).build());
                }
                sendCecCommand(HdmiCecMessageBuilder.buildSetOsdNameCommand(deviceInfo.getLogicalAddress(), 0, newDisplayName));
            }
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void handleMhlHotplugEvent(int portId, boolean connected) {
        assertRunOnServiceThread();
        if (connected) {
            HdmiMhlLocalDeviceStub newDevice = new HdmiMhlLocalDeviceStub(this, portId);
            HdmiMhlLocalDeviceStub oldDevice = this.mMhlController.addLocalDevice(newDevice);
            if (oldDevice != null) {
                oldDevice.onDeviceRemoved();
                Slog.i(TAG, "Old device of port " + portId + " is removed");
            }
            invokeDeviceEventListeners(newDevice.getInfo(), 1);
            updateSafeMhlInput();
        } else {
            HdmiMhlLocalDeviceStub device = this.mMhlController.removeLocalDevice(portId);
            if (device == null) {
                Slog.w(TAG, "No device to remove:[portId=" + portId);
            } else {
                device.onDeviceRemoved();
                invokeDeviceEventListeners(device.getInfo(), 2);
                updateSafeMhlInput();
            }
        }
        announceHotplugEvent(portId, connected);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void handleMhlBusModeChanged(int portId, int busmode) {
        assertRunOnServiceThread();
        HdmiMhlLocalDeviceStub device = this.mMhlController.getLocalDevice(portId);
        if (device != null) {
            device.setBusMode(busmode);
        } else {
            Slog.w(TAG, "No mhl device exists for bus mode change[portId:" + portId + ", busmode:" + busmode + "]");
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void handleMhlBusOvercurrent(int portId, boolean on) {
        assertRunOnServiceThread();
        HdmiMhlLocalDeviceStub device = this.mMhlController.getLocalDevice(portId);
        if (device != null) {
            device.onBusOvercurrentDetected(on);
        } else {
            Slog.w(TAG, "No mhl device exists for bus overcurrent event[portId:" + portId + "]");
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void handleMhlDeviceStatusChanged(int portId, int adopterId, int deviceId) {
        assertRunOnServiceThread();
        HdmiMhlLocalDeviceStub device = this.mMhlController.getLocalDevice(portId);
        if (device != null) {
            device.setDeviceStatusChange(adopterId, deviceId);
        } else {
            Slog.w(TAG, "No mhl device exists for device status event[portId:" + portId + ", adopterId:" + adopterId + ", deviceId:" + deviceId + "]");
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void updateSafeMhlInput() {
        assertRunOnServiceThread();
        List<HdmiDeviceInfo> inputs = Collections.emptyList();
        SparseArray<HdmiMhlLocalDeviceStub> devices = this.mMhlController.getAllLocalDevices();
        for (int i = 0; i < devices.size(); i++) {
            HdmiMhlLocalDeviceStub device = devices.valueAt(i);
            HdmiDeviceInfo info = device.getInfo();
            if (info != null) {
                if (inputs.isEmpty()) {
                    inputs = new ArrayList();
                }
                inputs.add(device.getInfo());
            }
        }
        synchronized (this.mLock) {
            this.mMhlDevices = inputs;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<HdmiDeviceInfo> getMhlDevicesLocked() {
        return this.mMhlDevices;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class HdmiMhlVendorCommandListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiMhlVendorCommandListener mListener;

        public HdmiMhlVendorCommandListenerRecord(IHdmiMhlVendorCommandListener listener) {
            this.mListener = listener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            HdmiControlService.this.mMhlVendorCommandListenerRecords.remove(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class HdmiControlStatusChangeListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiControlStatusChangeListener mListener;

        HdmiControlStatusChangeListenerRecord(IHdmiControlStatusChangeListener listener) {
            this.mListener = listener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                HdmiControlService.this.mHdmiControlStatusChangeListenerRecords.remove(this);
            }
        }

        public boolean equals(Object obj) {
            if (obj instanceof HdmiControlStatusChangeListenerRecord) {
                if (obj == this) {
                    return true;
                }
                HdmiControlStatusChangeListenerRecord other = (HdmiControlStatusChangeListenerRecord) obj;
                return other.mListener == this.mListener;
            }
            return false;
        }

        public int hashCode() {
            return this.mListener.hashCode();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class HotplugEventListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiHotplugEventListener mListener;

        public HotplugEventListenerRecord(IHdmiHotplugEventListener listener) {
            this.mListener = listener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                HdmiControlService.this.mHotplugEventListenerRecords.remove(this);
            }
        }

        public boolean equals(Object obj) {
            if (obj instanceof HotplugEventListenerRecord) {
                if (obj == this) {
                    return true;
                }
                HotplugEventListenerRecord other = (HotplugEventListenerRecord) obj;
                return other.mListener == this.mListener;
            }
            return false;
        }

        public int hashCode() {
            return this.mListener.hashCode();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DeviceEventListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiDeviceEventListener mListener;

        public DeviceEventListenerRecord(IHdmiDeviceEventListener listener) {
            this.mListener = listener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                HdmiControlService.this.mDeviceEventListenerRecords.remove(this);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SystemAudioModeChangeListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiSystemAudioModeChangeListener mListener;

        public SystemAudioModeChangeListenerRecord(IHdmiSystemAudioModeChangeListener listener) {
            this.mListener = listener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                HdmiControlService.this.mSystemAudioModeChangeListenerRecords.remove(this);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class VendorCommandListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiVendorCommandListener mListener;
        private final int mVendorId;

        VendorCommandListenerRecord(IHdmiVendorCommandListener listener, int vendorId) {
            this.mListener = listener;
            this.mVendorId = vendorId;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                HdmiControlService.this.mVendorCommandListenerRecords.remove(this);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class HdmiRecordListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiRecordListener mListener;

        public HdmiRecordListenerRecord(IHdmiRecordListener listener) {
            this.mListener = listener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                if (HdmiControlService.this.mRecordListenerRecord == this) {
                    HdmiControlService.this.mRecordListenerRecord = null;
                }
            }
        }
    }

    private void setWorkSourceUidToCallingUid() {
        Binder.setCallingWorkSourceUid(Binder.getCallingUid());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceAccessPermission() {
        getContext().enforceCallingOrSelfPermission(PERMISSION, TAG);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initBinderCall() {
        enforceAccessPermission();
        setWorkSourceUidToCallingUid();
    }

    /* loaded from: classes.dex */
    private final class BinderService extends IHdmiControlService.Stub {
        private BinderService() {
        }

        public int[] getSupportedTypes() {
            HdmiControlService.this.initBinderCall();
            int[] localDevices = new int[HdmiControlService.this.mLocalDevices.size()];
            for (int i = 0; i < localDevices.length; i++) {
                localDevices[i] = ((Integer) HdmiControlService.this.mLocalDevices.get(i)).intValue();
            }
            return localDevices;
        }

        public HdmiDeviceInfo getActiveSource() {
            HdmiControlService.this.initBinderCall();
            return HdmiControlService.this.getActiveSource();
        }

        public void deviceSelect(final int deviceId, final IHdmiControlCallback callback) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.1
                @Override // java.lang.Runnable
                public void run() {
                    if (callback == null) {
                        Slog.e(HdmiControlService.TAG, "Callback cannot be null");
                        return;
                    }
                    HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
                    HdmiCecLocalDevicePlayback playback = HdmiControlService.this.playback();
                    if (tv == null && playback == null) {
                        if (!HdmiControlService.this.mAddressAllocated) {
                            HdmiControlService.this.mSelectRequestBuffer.set(SelectRequestBuffer.newDeviceSelect(HdmiControlService.this, deviceId, callback));
                        } else if (HdmiControlService.this.isTvDevice()) {
                            Slog.e(HdmiControlService.TAG, "Local tv device not available");
                        } else {
                            HdmiControlService.this.invokeCallback(callback, 2);
                        }
                    } else if (tv != null) {
                        HdmiMhlLocalDeviceStub device = HdmiControlService.this.mMhlController.getLocalDeviceById(deviceId);
                        if (device != null) {
                            if (device.getPortId() == tv.getActivePortId()) {
                                HdmiControlService.this.invokeCallback(callback, 0);
                                return;
                            }
                            device.turnOn(callback);
                            tv.doManualPortSwitching(device.getPortId(), null);
                            return;
                        }
                        tv.deviceSelect(deviceId, callback);
                    } else {
                        playback.deviceSelect(deviceId, callback);
                    }
                }
            });
        }

        public void portSelect(final int portId, final IHdmiControlCallback callback) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.2
                @Override // java.lang.Runnable
                public void run() {
                    if (callback == null) {
                        Slog.e(HdmiControlService.TAG, "Callback cannot be null");
                        return;
                    }
                    HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
                    if (tv != null) {
                        tv.doManualPortSwitching(portId, callback);
                        return;
                    }
                    HdmiCecLocalDeviceAudioSystem audioSystem = HdmiControlService.this.audioSystem();
                    if (audioSystem != null) {
                        audioSystem.doManualPortSwitching(portId, callback);
                    } else if (!HdmiControlService.this.mAddressAllocated) {
                        HdmiControlService.this.mSelectRequestBuffer.set(SelectRequestBuffer.newPortSelect(HdmiControlService.this, portId, callback));
                    } else {
                        Slog.w(HdmiControlService.TAG, "Local device not available");
                        HdmiControlService.this.invokeCallback(callback, 2);
                    }
                }
            });
        }

        public void sendKeyEvent(final int deviceType, final int keyCode, final boolean isPressed) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.3
                @Override // java.lang.Runnable
                public void run() {
                    HdmiMhlLocalDeviceStub device = HdmiControlService.this.mMhlController.getLocalDevice(HdmiControlService.this.mActivePortId);
                    if (device != null) {
                        device.sendKeyEvent(keyCode, isPressed);
                    } else if (HdmiControlService.this.mCecController != null) {
                        HdmiCecLocalDevice localDevice = HdmiControlService.this.mHdmiCecNetwork.getLocalDevice(deviceType);
                        if (localDevice == null) {
                            Slog.w(HdmiControlService.TAG, "Local device not available to send key event.");
                        } else {
                            localDevice.sendKeyEvent(keyCode, isPressed);
                        }
                    }
                }
            });
        }

        public void sendVolumeKeyEvent(final int deviceType, final int keyCode, final boolean isPressed) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.4
                @Override // java.lang.Runnable
                public void run() {
                    if (HdmiControlService.this.mCecController == null) {
                        Slog.w(HdmiControlService.TAG, "CEC controller not available to send volume key event.");
                        return;
                    }
                    HdmiCecLocalDevice localDevice = HdmiControlService.this.mHdmiCecNetwork.getLocalDevice(deviceType);
                    if (localDevice == null) {
                        Slog.w(HdmiControlService.TAG, "Local device " + deviceType + " not available to send volume key event.");
                    } else {
                        localDevice.sendVolumeKeyEvent(keyCode, isPressed);
                    }
                }
            });
        }

        public void oneTouchPlay(final IHdmiControlCallback callback) {
            HdmiControlService.this.initBinderCall();
            int pid = Binder.getCallingPid();
            Slog.d(HdmiControlService.TAG, "Process pid: " + pid + " is calling oneTouchPlay.");
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.5
                @Override // java.lang.Runnable
                public void run() {
                    HdmiControlService.this.oneTouchPlay(callback);
                }
            });
        }

        public void toggleAndFollowTvPower() {
            HdmiControlService.this.initBinderCall();
            int pid = Binder.getCallingPid();
            Slog.d(HdmiControlService.TAG, "Process pid: " + pid + " is calling toggleAndFollowTvPower.");
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.6
                @Override // java.lang.Runnable
                public void run() {
                    HdmiControlService.this.toggleAndFollowTvPower();
                }
            });
        }

        public boolean shouldHandleTvPowerKey() {
            HdmiControlService.this.initBinderCall();
            return HdmiControlService.this.shouldHandleTvPowerKey();
        }

        public void queryDisplayStatus(final IHdmiControlCallback callback) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.7
                @Override // java.lang.Runnable
                public void run() {
                    HdmiControlService.this.queryDisplayStatus(callback);
                }
            });
        }

        public void addHdmiControlStatusChangeListener(IHdmiControlStatusChangeListener listener) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.addHdmiControlStatusChangeListener(listener);
        }

        public void removeHdmiControlStatusChangeListener(IHdmiControlStatusChangeListener listener) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.removeHdmiControlStatusChangeListener(listener);
        }

        public void addHdmiCecVolumeControlFeatureListener(IHdmiCecVolumeControlFeatureListener listener) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.addHdmiCecVolumeControlFeatureListener(listener);
        }

        public void removeHdmiCecVolumeControlFeatureListener(IHdmiCecVolumeControlFeatureListener listener) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.removeHdmiControlVolumeControlStatusChangeListener(listener);
        }

        public void addHotplugEventListener(IHdmiHotplugEventListener listener) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.addHotplugEventListener(listener);
        }

        public void removeHotplugEventListener(IHdmiHotplugEventListener listener) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.removeHotplugEventListener(listener);
        }

        public void addDeviceEventListener(IHdmiDeviceEventListener listener) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.addDeviceEventListener(listener);
        }

        public List<HdmiPortInfo> getPortInfo() {
            HdmiControlService.this.initBinderCall();
            if (HdmiControlService.this.getPortInfo() == null) {
                return Collections.emptyList();
            }
            return HdmiControlService.this.getPortInfo();
        }

        public boolean canChangeSystemAudioMode() {
            HdmiControlService.this.initBinderCall();
            HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
            if (tv == null) {
                return false;
            }
            return tv.hasSystemAudioDevice();
        }

        public boolean getSystemAudioMode() {
            HdmiControlService.this.initBinderCall();
            HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
            HdmiCecLocalDeviceAudioSystem audioSystem = HdmiControlService.this.audioSystem();
            return (tv != null && tv.isSystemAudioActivated()) || (audioSystem != null && audioSystem.isSystemAudioActivated());
        }

        public int getPhysicalAddress() {
            int physicalAddress;
            HdmiControlService.this.initBinderCall();
            synchronized (HdmiControlService.this.mLock) {
                physicalAddress = HdmiControlService.this.mHdmiCecNetwork.getPhysicalAddress();
            }
            return physicalAddress;
        }

        public void setSystemAudioMode(final boolean enabled, final IHdmiControlCallback callback) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.8
                @Override // java.lang.Runnable
                public void run() {
                    HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
                    if (tv == null) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available");
                        HdmiControlService.this.invokeCallback(callback, 2);
                        return;
                    }
                    tv.changeSystemAudioMode(enabled, callback);
                }
            });
        }

        public void addSystemAudioModeChangeListener(IHdmiSystemAudioModeChangeListener listener) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.addSystemAudioModeChangeListner(listener);
        }

        public void removeSystemAudioModeChangeListener(IHdmiSystemAudioModeChangeListener listener) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.removeSystemAudioModeChangeListener(listener);
        }

        public void setInputChangeListener(IHdmiInputChangeListener listener) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.setInputChangeListener(listener);
        }

        public List<HdmiDeviceInfo> getInputDevices() {
            HdmiControlService.this.initBinderCall();
            return HdmiUtils.mergeToUnmodifiableList(HdmiControlService.this.mHdmiCecNetwork.getSafeExternalInputsLocked(), HdmiControlService.this.getMhlDevicesLocked());
        }

        public List<HdmiDeviceInfo> getDeviceList() {
            HdmiControlService.this.initBinderCall();
            return HdmiControlService.this.mHdmiCecNetwork.getSafeCecDevicesLocked();
        }

        public void powerOffRemoteDevice(final int logicalAddress, final int powerStatus) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.9
                @Override // java.lang.Runnable
                public void run() {
                    Slog.w(HdmiControlService.TAG, "Device " + logicalAddress + " power status is " + powerStatus + " before standby command sent out");
                    HdmiControlService.this.sendCecCommand(HdmiCecMessageBuilder.buildStandby(HdmiControlService.this.getRemoteControlSourceAddress(), logicalAddress));
                }
            });
        }

        public void powerOnRemoteDevice(final int logicalAddress, final int powerStatus) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.10
                @Override // java.lang.Runnable
                public void run() {
                    Slog.i(HdmiControlService.TAG, "Device " + logicalAddress + " power status is " + powerStatus + " before power on command sent out");
                    if (HdmiControlService.this.getSwitchDevice() != null) {
                        HdmiControlService.this.getSwitchDevice().sendUserControlPressedAndReleased(logicalAddress, 109);
                    } else {
                        Slog.e(HdmiControlService.TAG, "Can't get the correct local device to handle routing.");
                    }
                }
            });
        }

        public void askRemoteDeviceToBecomeActiveSource(final int physicalAddress) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.11
                @Override // java.lang.Runnable
                public void run() {
                    HdmiCecMessage setStreamPath = HdmiCecMessageBuilder.buildSetStreamPath(HdmiControlService.this.getRemoteControlSourceAddress(), physicalAddress);
                    if (HdmiControlService.this.pathToPortId(physicalAddress) != -1) {
                        if (HdmiControlService.this.getSwitchDevice() != null) {
                            HdmiControlService.this.getSwitchDevice().handleSetStreamPath(setStreamPath);
                        } else {
                            Slog.e(HdmiControlService.TAG, "Can't get the correct local device to handle routing.");
                        }
                    }
                    HdmiControlService.this.sendCecCommand(setStreamPath);
                }
            });
        }

        public void setSystemAudioVolume(final int oldIndex, final int newIndex, final int maxIndex) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.12
                @Override // java.lang.Runnable
                public void run() {
                    HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
                    if (tv == null) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available");
                        return;
                    }
                    int i = oldIndex;
                    tv.changeVolume(i, newIndex - i, maxIndex);
                }
            });
        }

        public void setSystemAudioMute(final boolean mute) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.13
                @Override // java.lang.Runnable
                public void run() {
                    HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
                    if (tv == null) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available");
                    } else {
                        tv.changeMute(mute);
                    }
                }
            });
        }

        public void setArcMode(boolean enabled) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.14
                @Override // java.lang.Runnable
                public void run() {
                    HdmiCecLocalDeviceTv tv = HdmiControlService.this.tv();
                    if (tv == null) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available to change arc mode.");
                    }
                }
            });
        }

        public void setProhibitMode(boolean enabled) {
            HdmiControlService.this.initBinderCall();
            if (!HdmiControlService.this.isTvDevice()) {
                return;
            }
            HdmiControlService.this.setProhibitMode(enabled);
        }

        public void addVendorCommandListener(IHdmiVendorCommandListener listener, int vendorId) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.addVendorCommandListener(listener, vendorId);
        }

        public void sendVendorCommand(final int deviceType, final int targetAddress, final byte[] params, final boolean hasVendorId) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.15
                @Override // java.lang.Runnable
                public void run() {
                    HdmiCecLocalDevice device = HdmiControlService.this.mHdmiCecNetwork.getLocalDevice(deviceType);
                    if (device == null) {
                        Slog.w(HdmiControlService.TAG, "Local device not available");
                    } else if (hasVendorId) {
                        HdmiControlService.this.sendCecCommand(HdmiCecMessageBuilder.buildVendorCommandWithId(device.getDeviceInfo().getLogicalAddress(), targetAddress, HdmiControlService.this.getVendorId(), params));
                    } else {
                        HdmiControlService.this.sendCecCommand(HdmiCecMessageBuilder.buildVendorCommand(device.getDeviceInfo().getLogicalAddress(), targetAddress, params));
                    }
                }
            });
        }

        public void sendStandby(final int deviceType, final int deviceId) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.16
                @Override // java.lang.Runnable
                public void run() {
                    HdmiMhlLocalDeviceStub mhlDevice = HdmiControlService.this.mMhlController.getLocalDeviceById(deviceId);
                    if (mhlDevice != null) {
                        mhlDevice.sendStandby();
                        return;
                    }
                    HdmiCecLocalDevice device = HdmiControlService.this.mHdmiCecNetwork.getLocalDevice(deviceType);
                    if (device == null) {
                        device = HdmiControlService.this.audioSystem();
                    }
                    if (device == null) {
                        Slog.w(HdmiControlService.TAG, "Local device not available");
                    } else {
                        device.sendStandby(deviceId);
                    }
                }
            });
        }

        public void setHdmiRecordListener(IHdmiRecordListener listener) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.setHdmiRecordListener(listener);
        }

        public void startOneTouchRecord(final int recorderAddress, final byte[] recordSource) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.17
                @Override // java.lang.Runnable
                public void run() {
                    if (!HdmiControlService.this.isTvDeviceEnabled()) {
                        Slog.w(HdmiControlService.TAG, "TV device is not enabled.");
                    } else {
                        HdmiControlService.this.tv().startOneTouchRecord(recorderAddress, recordSource);
                    }
                }
            });
        }

        public void stopOneTouchRecord(final int recorderAddress) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.18
                @Override // java.lang.Runnable
                public void run() {
                    if (!HdmiControlService.this.isTvDeviceEnabled()) {
                        Slog.w(HdmiControlService.TAG, "TV device is not enabled.");
                    } else {
                        HdmiControlService.this.tv().stopOneTouchRecord(recorderAddress);
                    }
                }
            });
        }

        public void startTimerRecording(final int recorderAddress, final int sourceType, final byte[] recordSource) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.19
                @Override // java.lang.Runnable
                public void run() {
                    if (!HdmiControlService.this.isTvDeviceEnabled()) {
                        Slog.w(HdmiControlService.TAG, "TV device is not enabled.");
                    } else {
                        HdmiControlService.this.tv().startTimerRecording(recorderAddress, sourceType, recordSource);
                    }
                }
            });
        }

        public void clearTimerRecording(final int recorderAddress, final int sourceType, final byte[] recordSource) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.20
                @Override // java.lang.Runnable
                public void run() {
                    if (!HdmiControlService.this.isTvDeviceEnabled()) {
                        Slog.w(HdmiControlService.TAG, "TV device is not enabled.");
                    } else {
                        HdmiControlService.this.tv().clearTimerRecording(recorderAddress, sourceType, recordSource);
                    }
                }
            });
        }

        public void sendMhlVendorCommand(final int portId, final int offset, final int length, final byte[] data) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.21
                @Override // java.lang.Runnable
                public void run() {
                    if (!HdmiControlService.this.isControlEnabled()) {
                        Slog.w(HdmiControlService.TAG, "Hdmi control is disabled.");
                        return;
                    }
                    HdmiMhlLocalDeviceStub device = HdmiControlService.this.mMhlController.getLocalDevice(portId);
                    if (device == null) {
                        Slog.w(HdmiControlService.TAG, "Invalid port id:" + portId);
                    } else {
                        HdmiControlService.this.mMhlController.sendVendorCommand(portId, offset, length, data);
                    }
                }
            });
        }

        public void addHdmiMhlVendorCommandListener(IHdmiMhlVendorCommandListener listener) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.addHdmiMhlVendorCommandListener(listener);
        }

        public void setStandbyMode(final boolean isStandbyModeOn) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.22
                @Override // java.lang.Runnable
                public void run() {
                    HdmiControlService.this.setStandbyMode(isStandbyModeOn);
                }
            });
        }

        public void reportAudioStatus(final int deviceType, int volume, int maxVolume, boolean isMute) {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.23
                @Override // java.lang.Runnable
                public void run() {
                    HdmiCecLocalDevice device = HdmiControlService.this.mHdmiCecNetwork.getLocalDevice(deviceType);
                    if (device == null) {
                        Slog.w(HdmiControlService.TAG, "Local device not available");
                    } else if (HdmiControlService.this.audioSystem() == null) {
                        Slog.w(HdmiControlService.TAG, "audio system is not available");
                    } else if (!HdmiControlService.this.audioSystem().isSystemAudioActivated()) {
                        Slog.w(HdmiControlService.TAG, "audio system is not in system audio mode");
                    } else {
                        HdmiControlService.this.audioSystem().reportAudioStatus(0);
                    }
                }
            });
        }

        public void setSystemAudioModeOnForAudioOnlySource() {
            HdmiControlService.this.initBinderCall();
            HdmiControlService.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.BinderService.24
                @Override // java.lang.Runnable
                public void run() {
                    if (!HdmiControlService.this.isAudioSystemDevice()) {
                        Slog.e(HdmiControlService.TAG, "Not an audio system device. Won't set system audio mode on");
                    } else if (HdmiControlService.this.audioSystem() == null) {
                        Slog.e(HdmiControlService.TAG, "Audio System local device is not registered");
                    } else if (!HdmiControlService.this.audioSystem().checkSupportAndSetSystemAudioMode(true)) {
                        Slog.e(HdmiControlService.TAG, "System Audio Mode is not supported.");
                    } else {
                        HdmiControlService.this.sendCecCommand(HdmiCecMessageBuilder.buildSetSystemAudioMode(HdmiControlService.this.audioSystem().getDeviceInfo().getLogicalAddress(), 15, true));
                    }
                }
            });
        }

        /* JADX DEBUG: Multi-variable search result rejected for r9v0, resolved type: com.android.server.hdmi.HdmiControlService$BinderService */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            HdmiControlService.this.initBinderCall();
            new HdmiControlShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
            if (DumpUtils.checkDumpPermission(HdmiControlService.this.getContext(), HdmiControlService.TAG, writer)) {
                IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
                pw.println("mProhibitMode: " + HdmiControlService.this.mProhibitMode);
                pw.println("mPowerStatus: " + HdmiControlService.this.mPowerStatusController.getPowerStatus());
                pw.println("mIsCecAvailable: " + HdmiControlService.this.mIsCecAvailable);
                pw.println("mCecVersion: " + HdmiControlService.this.mCecVersion);
                pw.println("mIsAbsoluteVolumeControlEnabled: " + HdmiControlService.this.isAbsoluteVolumeControlEnabled());
                pw.println("System_settings:");
                pw.increaseIndent();
                pw.println("mMhlInputChangeEnabled: " + HdmiControlService.this.mMhlInputChangeEnabled);
                pw.println("mSystemAudioActivated: " + HdmiControlService.this.isSystemAudioActivated());
                pw.println("mHdmiCecVolumeControlEnabled: " + HdmiControlService.this.mHdmiCecVolumeControl);
                pw.decreaseIndent();
                pw.println("CEC settings:");
                pw.increaseIndent();
                HdmiCecConfig hdmiCecConfig = HdmiControlService.this.getHdmiCecConfig();
                List<String> allSettings = hdmiCecConfig.getAllSettings();
                Set<String> userSettings = new HashSet<>(hdmiCecConfig.getUserSettings());
                for (String setting : allSettings) {
                    if (hdmiCecConfig.isStringValueType(setting)) {
                        pw.println(setting + " (string): " + hdmiCecConfig.getStringValue(setting) + " (default: " + hdmiCecConfig.getDefaultStringValue(setting) + ")" + (userSettings.contains(setting) ? " [modifiable]" : ""));
                    } else if (hdmiCecConfig.isIntValueType(setting)) {
                        pw.println(setting + " (int): " + hdmiCecConfig.getIntValue(setting) + " (default: " + hdmiCecConfig.getDefaultIntValue(setting) + ")" + (userSettings.contains(setting) ? " [modifiable]" : ""));
                    }
                }
                pw.decreaseIndent();
                pw.println("mMhlController: ");
                pw.increaseIndent();
                HdmiControlService.this.mMhlController.dump(pw);
                pw.decreaseIndent();
                HdmiControlService.this.mHdmiCecNetwork.dump(pw);
                if (HdmiControlService.this.mCecController != null) {
                    pw.println("mCecController: ");
                    pw.increaseIndent();
                    HdmiControlService.this.mCecController.dump(pw);
                    pw.decreaseIndent();
                }
            }
        }

        public boolean setMessageHistorySize(int newSize) {
            HdmiControlService.this.enforceAccessPermission();
            if (HdmiControlService.this.mCecController == null) {
                return false;
            }
            return HdmiControlService.this.mCecController.setMessageHistorySize(newSize);
        }

        public int getMessageHistorySize() {
            HdmiControlService.this.enforceAccessPermission();
            if (HdmiControlService.this.mCecController != null) {
                return HdmiControlService.this.mCecController.getMessageHistorySize();
            }
            return 0;
        }

        public void addCecSettingChangeListener(String name, IHdmiCecSettingChangeListener listener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.addCecSettingChangeListener(name, listener);
        }

        public void removeCecSettingChangeListener(String name, IHdmiCecSettingChangeListener listener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.removeCecSettingChangeListener(name, listener);
        }

        public List<String> getUserCecSettings() {
            HdmiControlService.this.initBinderCall();
            long token = Binder.clearCallingIdentity();
            try {
                return HdmiControlService.this.getHdmiCecConfig().getUserSettings();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public List<String> getAllowedCecSettingStringValues(String name) {
            HdmiControlService.this.initBinderCall();
            long token = Binder.clearCallingIdentity();
            try {
                return HdmiControlService.this.getHdmiCecConfig().getAllowedStringValues(name);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int[] getAllowedCecSettingIntValues(String name) {
            HdmiControlService.this.initBinderCall();
            long token = Binder.clearCallingIdentity();
            try {
                List<Integer> allowedValues = HdmiControlService.this.getHdmiCecConfig().getAllowedIntValues(name);
                return allowedValues.stream().mapToInt(new ToIntFunction() { // from class: com.android.server.hdmi.HdmiControlService$BinderService$$ExternalSyntheticLambda0
                    @Override // java.util.function.ToIntFunction
                    public final int applyAsInt(Object obj) {
                        int intValue;
                        intValue = ((Integer) obj).intValue();
                        return intValue;
                    }
                }).toArray();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public String getCecSettingStringValue(String name) {
            HdmiControlService.this.initBinderCall();
            long token = Binder.clearCallingIdentity();
            try {
                return HdmiControlService.this.getHdmiCecConfig().getStringValue(name);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setCecSettingStringValue(String name, String value) {
            HdmiControlService.this.initBinderCall();
            long token = Binder.clearCallingIdentity();
            try {
                HdmiControlService.this.getHdmiCecConfig().setStringValue(name, value);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getCecSettingIntValue(String name) {
            HdmiControlService.this.initBinderCall();
            long token = Binder.clearCallingIdentity();
            try {
                return HdmiControlService.this.getHdmiCecConfig().getIntValue(name);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setCecSettingIntValue(String name, int value) {
            HdmiControlService.this.initBinderCall();
            long token = Binder.clearCallingIdentity();
            try {
                HdmiControlService.this.getHdmiCecConfig().setIntValue(name, value);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    void setHdmiCecVolumeControlEnabledInternal(int hdmiCecVolumeControl) {
        this.mHdmiCecVolumeControl = hdmiCecVolumeControl;
        announceHdmiCecVolumeControlFeatureChange(hdmiCecVolumeControl);
        runOnServiceThread(new HdmiControlService$$ExternalSyntheticLambda2(this));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getRemoteControlSourceAddress() {
        if (isAudioSystemDevice()) {
            return audioSystem().getDeviceInfo().getLogicalAddress();
        }
        if (isPlaybackDevice()) {
            return playback().getDeviceInfo().getLogicalAddress();
        }
        return 15;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public HdmiCecLocalDeviceSource getSwitchDevice() {
        if (isAudioSystemDevice()) {
            return audioSystem();
        }
        if (isPlaybackDevice()) {
            return playback();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @HdmiAnnotations.ServiceThreadOnly
    public void oneTouchPlay(IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        if (!this.mAddressAllocated) {
            this.mOtpCallbackPendingAddressAllocation = callback;
            Slog.d(TAG, "Local device is under address allocation. Save OTP callback for later process.");
            return;
        }
        HdmiCecLocalDeviceSource source = playback();
        if (source == null) {
            source = audioSystem();
        }
        if (source == null) {
            Slog.w(TAG, "Local source device not available");
            invokeCallback(callback, 2);
            return;
        }
        source.oneTouchPlay(callback);
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected void toggleAndFollowTvPower() {
        assertRunOnServiceThread();
        HdmiCecLocalDeviceSource source = playback();
        if (source == null) {
            source = audioSystem();
        }
        if (source == null) {
            Slog.w(TAG, "Local source device not available");
        } else {
            source.toggleAndFollowTvPower();
        }
    }

    protected boolean shouldHandleTvPowerKey() {
        if (isTvDevice()) {
            return false;
        }
        String powerControlMode = getHdmiCecConfig().getStringValue("power_control_mode");
        if (powerControlMode.equals("none")) {
            return false;
        }
        int hdmiCecEnabled = getHdmiCecConfig().getIntValue("hdmi_cec_enabled");
        if (hdmiCecEnabled != 1) {
            return false;
        }
        return this.mIsCecAvailable;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @HdmiAnnotations.ServiceThreadOnly
    public void queryDisplayStatus(IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        if (!this.mAddressAllocated) {
            this.mDisplayStatusCallback = callback;
            Slog.d(TAG, "Local device is under address allocation. Queue display callback for later process.");
            return;
        }
        HdmiCecLocalDeviceSource source = playback();
        if (source == null) {
            source = audioSystem();
        }
        if (source == null) {
            Slog.w(TAG, "Local source device not available");
            invokeCallback(callback, 2);
            return;
        }
        source.queryDisplayStatus(callback);
    }

    protected HdmiDeviceInfo getActiveSource() {
        int activePath;
        if (playback() != null && playback().isActiveSource()) {
            return playback().getDeviceInfo();
        }
        HdmiCecLocalDevice.ActiveSource activeSource = getLocalActiveSource();
        if (activeSource.isValid()) {
            HdmiDeviceInfo activeSourceInfo = this.mHdmiCecNetwork.getSafeCecDeviceInfo(activeSource.logicalAddress);
            if (activeSourceInfo != null) {
                return activeSourceInfo;
            }
            return HdmiDeviceInfo.hardwarePort(activeSource.physicalAddress, pathToPortId(activeSource.physicalAddress));
        } else if (tv() != null && (activePath = tv().getActivePath()) != 65535) {
            HdmiDeviceInfo info = this.mHdmiCecNetwork.getSafeDeviceInfoByPath(activePath);
            return info != null ? info : HdmiDeviceInfo.hardwarePort(activePath, tv().getActivePortId());
        } else {
            return null;
        }
    }

    void addHdmiControlStatusChangeListener(final IHdmiControlStatusChangeListener listener) {
        final HdmiControlStatusChangeListenerRecord record = new HdmiControlStatusChangeListenerRecord(listener);
        try {
            listener.asBinder().linkToDeath(record, 0);
            synchronized (this.mLock) {
                this.mHdmiControlStatusChangeListenerRecords.add(record);
            }
            runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.9
                @Override // java.lang.Runnable
                public void run() {
                    synchronized (HdmiControlService.this.mLock) {
                        if (HdmiControlService.this.mHdmiControlStatusChangeListenerRecords.contains(record)) {
                            synchronized (HdmiControlService.this.mLock) {
                                HdmiControlService hdmiControlService = HdmiControlService.this;
                                hdmiControlService.invokeHdmiControlStatusChangeListenerLocked(listener, hdmiControlService.mHdmiControlEnabled);
                            }
                        }
                    }
                }
            });
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeHdmiControlStatusChangeListener(IHdmiControlStatusChangeListener listener) {
        synchronized (this.mLock) {
            Iterator<HdmiControlStatusChangeListenerRecord> it = this.mHdmiControlStatusChangeListenerRecords.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                HdmiControlStatusChangeListenerRecord record = it.next();
                if (record.mListener.asBinder() == listener.asBinder()) {
                    listener.asBinder().unlinkToDeath(record, 0);
                    this.mHdmiControlStatusChangeListenerRecords.remove(record);
                    break;
                }
            }
        }
    }

    void addHdmiCecVolumeControlFeatureListener(final IHdmiCecVolumeControlFeatureListener listener) {
        this.mHdmiCecVolumeControlFeatureListenerRecords.register(listener);
        runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.10
            @Override // java.lang.Runnable
            public void run() {
                synchronized (HdmiControlService.this.mLock) {
                    try {
                        listener.onHdmiCecVolumeControlFeature(HdmiControlService.this.mHdmiCecVolumeControl);
                    } catch (RemoteException e) {
                        Slog.e(HdmiControlService.TAG, "Failed to report HdmiControlVolumeControlStatusChange: " + HdmiControlService.this.mHdmiCecVolumeControl, e);
                    }
                }
            }
        });
    }

    void removeHdmiControlVolumeControlStatusChangeListener(IHdmiCecVolumeControlFeatureListener listener) {
        this.mHdmiCecVolumeControlFeatureListenerRecords.unregister(listener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addHotplugEventListener(final IHdmiHotplugEventListener listener) {
        final HotplugEventListenerRecord record = new HotplugEventListenerRecord(listener);
        try {
            listener.asBinder().linkToDeath(record, 0);
            synchronized (this.mLock) {
                this.mHotplugEventListenerRecords.add(record);
            }
            runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.11
                @Override // java.lang.Runnable
                public void run() {
                    synchronized (HdmiControlService.this.mLock) {
                        if (HdmiControlService.this.mHotplugEventListenerRecords.contains(record)) {
                            for (HdmiPortInfo port : HdmiControlService.this.getPortInfo()) {
                                HdmiHotplugEvent event = new HdmiHotplugEvent(port.getId(), HdmiControlService.this.mCecController.isConnected(port.getId()));
                                synchronized (HdmiControlService.this.mLock) {
                                    HdmiControlService.this.invokeHotplugEventListenerLocked(listener, event);
                                }
                            }
                        }
                    }
                }
            });
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeHotplugEventListener(IHdmiHotplugEventListener listener) {
        synchronized (this.mLock) {
            Iterator<HotplugEventListenerRecord> it = this.mHotplugEventListenerRecords.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                HotplugEventListenerRecord record = it.next();
                if (record.mListener.asBinder() == listener.asBinder()) {
                    listener.asBinder().unlinkToDeath(record, 0);
                    this.mHotplugEventListenerRecords.remove(record);
                    break;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addDeviceEventListener(IHdmiDeviceEventListener listener) {
        DeviceEventListenerRecord record = new DeviceEventListenerRecord(listener);
        try {
            listener.asBinder().linkToDeath(record, 0);
            synchronized (this.mLock) {
                this.mDeviceEventListenerRecords.add(record);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void invokeDeviceEventListeners(HdmiDeviceInfo device, int status) {
        synchronized (this.mLock) {
            Iterator<DeviceEventListenerRecord> it = this.mDeviceEventListenerRecords.iterator();
            while (it.hasNext()) {
                DeviceEventListenerRecord record = it.next();
                try {
                    record.mListener.onStatusChanged(device, status);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to report device event:" + e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addSystemAudioModeChangeListner(IHdmiSystemAudioModeChangeListener listener) {
        SystemAudioModeChangeListenerRecord record = new SystemAudioModeChangeListenerRecord(listener);
        try {
            listener.asBinder().linkToDeath(record, 0);
            synchronized (this.mLock) {
                this.mSystemAudioModeChangeListenerRecords.add(record);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeSystemAudioModeChangeListener(IHdmiSystemAudioModeChangeListener listener) {
        synchronized (this.mLock) {
            Iterator<SystemAudioModeChangeListenerRecord> it = this.mSystemAudioModeChangeListenerRecords.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                SystemAudioModeChangeListenerRecord record = it.next();
                if (record.mListener.asBinder() == listener) {
                    listener.asBinder().unlinkToDeath(record, 0);
                    this.mSystemAudioModeChangeListenerRecords.remove(record);
                    break;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class InputChangeListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiInputChangeListener mListener;

        public InputChangeListenerRecord(IHdmiInputChangeListener listener) {
            this.mListener = listener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                if (HdmiControlService.this.mInputChangeListenerRecord == this) {
                    HdmiControlService.this.mInputChangeListenerRecord = null;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setInputChangeListener(IHdmiInputChangeListener listener) {
        synchronized (this.mLock) {
            this.mInputChangeListenerRecord = new InputChangeListenerRecord(listener);
            try {
                listener.asBinder().linkToDeath(this.mInputChangeListenerRecord, 0);
            } catch (RemoteException e) {
                Slog.w(TAG, "Listener already died");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void invokeInputChangeListener(HdmiDeviceInfo info) {
        synchronized (this.mLock) {
            InputChangeListenerRecord inputChangeListenerRecord = this.mInputChangeListenerRecord;
            if (inputChangeListenerRecord != null) {
                try {
                    inputChangeListenerRecord.mListener.onChanged(info);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Exception thrown by IHdmiInputChangeListener: " + e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setHdmiRecordListener(IHdmiRecordListener listener) {
        synchronized (this.mLock) {
            this.mRecordListenerRecord = new HdmiRecordListenerRecord(listener);
            try {
                listener.asBinder().linkToDeath(this.mRecordListenerRecord, 0);
            } catch (RemoteException e) {
                Slog.w(TAG, "Listener already died.", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public byte[] invokeRecordRequestListener(int recorderAddress) {
        synchronized (this.mLock) {
            HdmiRecordListenerRecord hdmiRecordListenerRecord = this.mRecordListenerRecord;
            if (hdmiRecordListenerRecord != null) {
                try {
                    return hdmiRecordListenerRecord.mListener.getOneTouchRecordSource(recorderAddress);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to start record.", e);
                }
            }
            return EmptyArray.BYTE;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void invokeOneTouchRecordResult(int recorderAddress, int result) {
        synchronized (this.mLock) {
            HdmiRecordListenerRecord hdmiRecordListenerRecord = this.mRecordListenerRecord;
            if (hdmiRecordListenerRecord != null) {
                try {
                    hdmiRecordListenerRecord.mListener.onOneTouchRecordResult(recorderAddress, result);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to call onOneTouchRecordResult.", e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void invokeTimerRecordingResult(int recorderAddress, int result) {
        synchronized (this.mLock) {
            HdmiRecordListenerRecord hdmiRecordListenerRecord = this.mRecordListenerRecord;
            if (hdmiRecordListenerRecord != null) {
                try {
                    hdmiRecordListenerRecord.mListener.onTimerRecordingResult(recorderAddress, result);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to call onTimerRecordingResult.", e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void invokeClearTimerRecordingResult(int recorderAddress, int result) {
        synchronized (this.mLock) {
            HdmiRecordListenerRecord hdmiRecordListenerRecord = this.mRecordListenerRecord;
            if (hdmiRecordListenerRecord != null) {
                try {
                    hdmiRecordListenerRecord.mListener.onClearTimerRecordingResult(recorderAddress, result);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to call onClearTimerRecordingResult.", e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void invokeCallback(IHdmiControlCallback callback, int result) {
        try {
            callback.onComplete(result);
        } catch (RemoteException e) {
            Slog.e(TAG, "Invoking callback failed:" + e);
        }
    }

    private void invokeSystemAudioModeChangeLocked(IHdmiSystemAudioModeChangeListener listener, boolean enabled) {
        try {
            listener.onStatusChanged(enabled);
        } catch (RemoteException e) {
            Slog.e(TAG, "Invoking callback failed:" + e);
        }
    }

    private void announceHotplugEvent(int portId, boolean connected) {
        HdmiHotplugEvent event = new HdmiHotplugEvent(portId, connected);
        synchronized (this.mLock) {
            Iterator<HotplugEventListenerRecord> it = this.mHotplugEventListenerRecords.iterator();
            while (it.hasNext()) {
                HotplugEventListenerRecord record = it.next();
                invokeHotplugEventListenerLocked(record.mListener, event);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void invokeHotplugEventListenerLocked(IHdmiHotplugEventListener listener, HdmiHotplugEvent event) {
        try {
            listener.onReceived(event);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to report hotplug event:" + event.toString(), e);
        }
    }

    private void announceHdmiControlStatusChange(int isEnabled) {
        assertRunOnServiceThread();
        synchronized (this.mLock) {
            List<IHdmiControlStatusChangeListener> listeners = new ArrayList<>(this.mHdmiControlStatusChangeListenerRecords.size());
            Iterator<HdmiControlStatusChangeListenerRecord> it = this.mHdmiControlStatusChangeListenerRecords.iterator();
            while (it.hasNext()) {
                HdmiControlStatusChangeListenerRecord record = it.next();
                listeners.add(record.mListener);
            }
            invokeHdmiControlStatusChangeListenerLocked(listeners, isEnabled);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void invokeHdmiControlStatusChangeListenerLocked(IHdmiControlStatusChangeListener listener, int isEnabled) {
        invokeHdmiControlStatusChangeListenerLocked(Collections.singletonList(listener), isEnabled);
    }

    private void invokeHdmiControlStatusChangeListenerLocked(final Collection<IHdmiControlStatusChangeListener> listeners, final int isEnabled) {
        if (isEnabled == 1) {
            queryDisplayStatus(new IHdmiControlCallback.Stub() { // from class: com.android.server.hdmi.HdmiControlService.12
                public void onComplete(int status) {
                    if (status == -1 || status == 5 || status == 2) {
                        HdmiControlService.this.mIsCecAvailable = false;
                    } else {
                        HdmiControlService.this.mIsCecAvailable = true;
                    }
                    if (!listeners.isEmpty()) {
                        HdmiControlService hdmiControlService = HdmiControlService.this;
                        hdmiControlService.invokeHdmiControlStatusChangeListenerLocked(listeners, isEnabled, hdmiControlService.mIsCecAvailable);
                    }
                }
            });
            return;
        }
        this.mIsCecAvailable = false;
        if (!listeners.isEmpty()) {
            invokeHdmiControlStatusChangeListenerLocked(listeners, isEnabled, this.mIsCecAvailable);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void invokeHdmiControlStatusChangeListenerLocked(Collection<IHdmiControlStatusChangeListener> listeners, int isEnabled, boolean isCecAvailable) {
        for (IHdmiControlStatusChangeListener listener : listeners) {
            try {
                listener.onStatusChange(isEnabled, isCecAvailable);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to report HdmiControlStatusChange: " + isEnabled + " isAvailable: " + isCecAvailable, e);
            }
        }
    }

    private void announceHdmiCecVolumeControlFeatureChange(final int hdmiCecVolumeControl) {
        assertRunOnServiceThread();
        synchronized (this.mLock) {
            this.mHdmiCecVolumeControlFeatureListenerRecords.broadcast(new Consumer() { // from class: com.android.server.hdmi.HdmiControlService$$ExternalSyntheticLambda5
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    HdmiControlService.lambda$announceHdmiCecVolumeControlFeatureChange$2(hdmiCecVolumeControl, (IHdmiCecVolumeControlFeatureListener) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$announceHdmiCecVolumeControlFeatureChange$2(int hdmiCecVolumeControl, IHdmiCecVolumeControlFeatureListener listener) {
        try {
            listener.onHdmiCecVolumeControlFeature(hdmiCecVolumeControl);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to report HdmiControlVolumeControlStatusChange: " + hdmiCecVolumeControl);
        }
    }

    public HdmiCecLocalDeviceTv tv() {
        return (HdmiCecLocalDeviceTv) this.mHdmiCecNetwork.getLocalDevice(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTvDevice() {
        return this.mLocalDevices.contains(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAudioSystemDevice() {
        return this.mLocalDevices.contains(5);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPlaybackDevice() {
        return this.mLocalDevices.contains(4);
    }

    boolean isSwitchDevice() {
        return ((Boolean) HdmiProperties.is_switch().orElse(false)).booleanValue();
    }

    boolean isTvDeviceEnabled() {
        return isTvDevice() && tv() != null;
    }

    protected HdmiCecLocalDevicePlayback playback() {
        return (HdmiCecLocalDevicePlayback) this.mHdmiCecNetwork.getLocalDevice(4);
    }

    public HdmiCecLocalDeviceAudioSystem audioSystem() {
        return (HdmiCecLocalDeviceAudioSystem) this.mHdmiCecNetwork.getLocalDevice(5);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioManager getAudioManager() {
        return this.mAudioManager;
    }

    private AudioDeviceVolumeManagerWrapperInterface getAudioDeviceVolumeManager() {
        return this.mAudioDeviceVolumeManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isControlEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = true;
            if (this.mHdmiControlEnabled != 1) {
                z = false;
            }
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public int getPowerStatus() {
        assertRunOnServiceThread();
        return this.mPowerStatusController.getPowerStatus();
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setPowerStatus(int powerStatus) {
        assertRunOnServiceThread();
        this.mPowerStatusController.setPowerStatus(powerStatus);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean isPowerOnOrTransient() {
        assertRunOnServiceThread();
        return this.mPowerStatusController.isPowerStatusOn() || this.mPowerStatusController.isPowerStatusTransientToOn();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean isPowerStandbyOrTransient() {
        assertRunOnServiceThread();
        return this.mPowerStatusController.isPowerStatusStandby() || this.mPowerStatusController.isPowerStatusTransientToStandby();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean isPowerStandby() {
        assertRunOnServiceThread();
        return this.mPowerStatusController.isPowerStatusStandby();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void wakeUp() {
        assertRunOnServiceThread();
        this.mWakeUpMessageReceived = true;
        this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 8, "android.server.hdmi:WAKE");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void standby() {
        assertRunOnServiceThread();
        if (!canGoToStandby()) {
            return;
        }
        this.mStandbyMessageReceived = true;
        this.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 5, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isWakeUpMessageReceived() {
        return this.mWakeUpMessageReceived;
    }

    protected boolean isStandbyMessageReceived() {
        return this.mStandbyMessageReceived;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void onWakeUp(int wakeUpAction) {
        int startReason;
        assertRunOnServiceThread();
        this.mPowerStatusController.setPowerStatus(2, false);
        if (this.mCecController != null) {
            if (this.mHdmiControlEnabled == 1) {
                switch (wakeUpAction) {
                    case 0:
                        startReason = 2;
                        if (this.mWakeUpMessageReceived) {
                            startReason = 3;
                            break;
                        }
                        break;
                    case 1:
                        startReason = 1;
                        break;
                    default:
                        Slog.e(TAG, "wakeUpAction " + wakeUpAction + " not defined.");
                        return;
                }
                initializeCec(startReason);
                return;
            }
            return;
        }
        Slog.i(TAG, "Device does not support HDMI-CEC.");
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected void onStandby(final int standbyAction) {
        this.mWakeUpMessageReceived = false;
        assertRunOnServiceThread();
        this.mPowerStatusController.setPowerStatus(3, false);
        invokeVendorCommandListenersOnControlStateChanged(false, 3);
        final List<HdmiCecLocalDevice> devices = getAllLocalDevices();
        if (!isStandbyMessageReceived() && !canGoToStandby()) {
            this.mPowerStatusController.setPowerStatus(1);
            for (HdmiCecLocalDevice device : devices) {
                device.onStandby(this.mStandbyMessageReceived, standbyAction);
            }
            return;
        }
        disableDevices(new HdmiCecLocalDevice.PendingActionClearedCallback() { // from class: com.android.server.hdmi.HdmiControlService.13
            @Override // com.android.server.hdmi.HdmiCecLocalDevice.PendingActionClearedCallback
            public void onCleared(HdmiCecLocalDevice device2) {
                Slog.v(HdmiControlService.TAG, "On standby-action cleared:" + device2.mDeviceType);
                devices.remove(device2);
                if (devices.isEmpty()) {
                    HdmiControlService.this.onPendingActionsCleared(standbyAction);
                }
            }
        });
    }

    boolean canGoToStandby() {
        for (HdmiCecLocalDevice device : this.mHdmiCecNetwork.getLocalDeviceList()) {
            if (!device.canGoToStandby()) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void onLanguageChanged(String language) {
        assertRunOnServiceThread();
        this.mMenuLanguage = language;
        if (isTvDeviceEnabled()) {
            tv().broadcastMenuLanguage(language);
            this.mCecController.setLanguage(language);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public String getLanguage() {
        assertRunOnServiceThread();
        return this.mMenuLanguage;
    }

    private void disableDevices(HdmiCecLocalDevice.PendingActionClearedCallback callback) {
        if (this.mCecController != null) {
            for (HdmiCecLocalDevice device : this.mHdmiCecNetwork.getLocalDeviceList()) {
                device.disableDevice(this.mStandbyMessageReceived, callback);
            }
        }
        this.mMhlController.clearAllLocalDevices();
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void clearLocalDevices() {
        assertRunOnServiceThread();
        HdmiCecController hdmiCecController = this.mCecController;
        if (hdmiCecController == null) {
            return;
        }
        hdmiCecController.clearLogicalAddress();
        this.mHdmiCecNetwork.clearLocalDevices();
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void onPendingActionsCleared(int standbyAction) {
        assertRunOnServiceThread();
        Slog.v(TAG, "onPendingActionsCleared");
        if (this.mPowerStatusController.isPowerStatusTransientToStandby()) {
            this.mPowerStatusController.setPowerStatus(1);
            for (HdmiCecLocalDevice device : this.mHdmiCecNetwork.getLocalDeviceList()) {
                device.onStandby(this.mStandbyMessageReceived, standbyAction);
            }
            if (!isAudioSystemDevice()) {
                this.mCecController.setOption(3, false);
                this.mMhlController.setOption(104, 0);
            }
        }
        this.mStandbyMessageReceived = false;
    }

    void addVendorCommandListener(IHdmiVendorCommandListener listener, int vendorId) {
        VendorCommandListenerRecord record = new VendorCommandListenerRecord(listener, vendorId);
        try {
            listener.asBinder().linkToDeath(record, 0);
            synchronized (this.mLock) {
                this.mVendorCommandListenerRecords.add(record);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't wrap try/catch for region: R(8:12|(2:14|(2:16|17))|18|19|20|21|17|10) */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x0045, code lost:
        r4 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0046, code lost:
        android.util.Slog.e(com.android.server.hdmi.HdmiControlService.TAG, "Failed to notify vendor command reception", r4);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean invokeVendorCommandListenersOnReceived(int deviceType, int srcAddress, int destAddress, byte[] params, boolean hasVendorId) {
        synchronized (this.mLock) {
            if (this.mVendorCommandListenerRecords.isEmpty()) {
                return false;
            }
            Iterator<VendorCommandListenerRecord> it = this.mVendorCommandListenerRecords.iterator();
            while (it.hasNext()) {
                VendorCommandListenerRecord record = it.next();
                if (hasVendorId) {
                    int vendorId = ((params[0] & 255) << 16) + ((params[1] & 255) << 8) + (params[2] & 255);
                    if (record.mVendorId != vendorId) {
                    }
                }
                record.mListener.onReceived(srcAddress, destAddress, params, hasVendorId);
            }
            return true;
        }
    }

    boolean invokeVendorCommandListenersOnControlStateChanged(boolean enabled, int reason) {
        synchronized (this.mLock) {
            if (this.mVendorCommandListenerRecords.isEmpty()) {
                return false;
            }
            Iterator<VendorCommandListenerRecord> it = this.mVendorCommandListenerRecords.iterator();
            while (it.hasNext()) {
                VendorCommandListenerRecord record = it.next();
                try {
                    record.mListener.onControlStateChanged(enabled, reason);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to notify control-state-changed to vendor handler", e);
                }
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addHdmiMhlVendorCommandListener(IHdmiMhlVendorCommandListener listener) {
        HdmiMhlVendorCommandListenerRecord record = new HdmiMhlVendorCommandListenerRecord(listener);
        try {
            listener.asBinder().linkToDeath(record, 0);
            synchronized (this.mLock) {
                this.mMhlVendorCommandListenerRecords.add(record);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died.");
        }
    }

    void invokeMhlVendorCommandListeners(int portId, int offest, int length, byte[] data) {
        synchronized (this.mLock) {
            Iterator<HdmiMhlVendorCommandListenerRecord> it = this.mMhlVendorCommandListenerRecords.iterator();
            while (it.hasNext()) {
                HdmiMhlVendorCommandListenerRecord record = it.next();
                try {
                    record.mListener.onReceived(portId, offest, length, data);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to notify MHL vendor command", e);
                }
            }
        }
    }

    void setStandbyMode(boolean isStandbyModeOn) {
        assertRunOnServiceThread();
        if (isPowerOnOrTransient() && isStandbyModeOn) {
            this.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 5, 0);
            if (playback() != null) {
                playback().sendStandby(0);
            }
        } else if (isPowerStandbyOrTransient() && !isStandbyModeOn) {
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 8, "android.server.hdmi:WAKE");
            if (playback() != null) {
                oneTouchPlay(new IHdmiControlCallback.Stub() { // from class: com.android.server.hdmi.HdmiControlService.14
                    public void onComplete(int result) {
                        if (result != 0) {
                            Slog.w(HdmiControlService.TAG, "Failed to complete 'one touch play'. result=" + result);
                        }
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getHdmiCecVolumeControl() {
        int i;
        synchronized (this.mLock) {
            i = this.mHdmiCecVolumeControl;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isProhibitMode() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mProhibitMode;
        }
        return z;
    }

    void setProhibitMode(boolean enabled) {
        synchronized (this.mLock) {
            this.mProhibitMode = enabled;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSystemAudioActivated() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSystemAudioActivated;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSystemAudioActivated(boolean on) {
        synchronized (this.mLock) {
            this.mSystemAudioActivated = on;
        }
        runOnServiceThread(new HdmiControlService$$ExternalSyntheticLambda2(this));
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setCecOption(int key, boolean value) {
        assertRunOnServiceThread();
        this.mCecController.setOption(key, value);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setControlEnabled(int enabled) {
        assertRunOnServiceThread();
        synchronized (this.mLock) {
            this.mHdmiControlEnabled = enabled;
        }
        if (enabled == 1) {
            enableHdmiControlService();
            setHdmiCecVolumeControlEnabledInternal(getHdmiCecConfig().getIntValue("volume_control_enabled"));
            return;
        }
        setHdmiCecVolumeControlEnabledInternal(0);
        invokeVendorCommandListenersOnControlStateChanged(false, 1);
        runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.15
            @Override // java.lang.Runnable
            public void run() {
                HdmiControlService.this.disableHdmiControlService();
            }
        });
        announceHdmiControlStatusChange(enabled);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void enableHdmiControlService() {
        this.mCecController.setOption(2, true);
        this.mCecController.setOption(3, true);
        this.mMhlController.setOption(103, 1);
        initializeCec(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void disableHdmiControlService() {
        disableDevices(new HdmiCecLocalDevice.PendingActionClearedCallback() { // from class: com.android.server.hdmi.HdmiControlService.16
            @Override // com.android.server.hdmi.HdmiCecLocalDevice.PendingActionClearedCallback
            public void onCleared(HdmiCecLocalDevice device) {
                HdmiControlService.this.assertRunOnServiceThread();
                HdmiControlService.this.mCecController.flush(new Runnable() { // from class: com.android.server.hdmi.HdmiControlService.16.1
                    @Override // java.lang.Runnable
                    public void run() {
                        HdmiControlService.this.mCecController.setOption(2, false);
                        HdmiControlService.this.mCecController.setOption(3, false);
                        HdmiControlService.this.mMhlController.setOption(103, 0);
                        HdmiControlService.this.clearLocalDevices();
                    }
                });
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void setActivePortId(int portId) {
        assertRunOnServiceThread();
        this.mActivePortId = portId;
        setLastInputForMhl(-1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiCecLocalDevice.ActiveSource getLocalActiveSource() {
        HdmiCecLocalDevice.ActiveSource activeSource;
        synchronized (this.mLock) {
            activeSource = this.mActiveSource;
        }
        return activeSource;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void pauseActiveMediaSessions() {
        MediaSessionManager mediaSessionManager = (MediaSessionManager) getContext().getSystemService(MediaSessionManager.class);
        List<MediaController> mediaControllers = mediaSessionManager.getActiveSessions(null);
        for (MediaController mediaController : mediaControllers) {
            mediaController.getTransportControls().pause();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setActiveSource(int logicalAddress, int physicalAddress, String caller) {
        synchronized (this.mLock) {
            this.mActiveSource.logicalAddress = logicalAddress;
            this.mActiveSource.physicalAddress = physicalAddress;
        }
        getAtomWriter().activeSourceChanged(logicalAddress, physicalAddress, HdmiUtils.pathRelationship(getPhysicalAddress(), physicalAddress));
        for (HdmiCecLocalDevice device : getAllLocalDevices()) {
            boolean deviceIsActiveSource = logicalAddress == device.getDeviceInfo().getLogicalAddress() && physicalAddress == getPhysicalAddress();
            device.addActiveSourceHistoryItem(new HdmiCecLocalDevice.ActiveSource(logicalAddress, physicalAddress), deviceIsActiveSource, caller);
        }
        runOnServiceThread(new HdmiControlService$$ExternalSyntheticLambda2(this));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setAndBroadcastActiveSource(int physicalAddress, int deviceType, int source, String caller) {
        if (deviceType == 4) {
            HdmiCecLocalDevicePlayback playback = playback();
            playback.setActiveSource(playback.getDeviceInfo().getLogicalAddress(), physicalAddress, caller);
            playback.wakeUpIfActiveSource();
            playback.maySendActiveSource(source);
        }
        if (deviceType == 5) {
            HdmiCecLocalDeviceAudioSystem audioSystem = audioSystem();
            if (playback() == null) {
                audioSystem.setActiveSource(audioSystem.getDeviceInfo().getLogicalAddress(), physicalAddress, caller);
                audioSystem.wakeUpIfActiveSource();
                audioSystem.maySendActiveSource(source);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setAndBroadcastActiveSourceFromOneDeviceType(int sourceAddress, int physicalAddress, String caller) {
        HdmiCecLocalDevicePlayback playback = playback();
        HdmiCecLocalDeviceAudioSystem audioSystem = audioSystem();
        if (playback != null) {
            playback.setActiveSource(playback.getDeviceInfo().getLogicalAddress(), physicalAddress, caller);
            playback.wakeUpIfActiveSource();
            playback.maySendActiveSource(sourceAddress);
        } else if (audioSystem != null) {
            audioSystem.setActiveSource(audioSystem.getDeviceInfo().getLogicalAddress(), physicalAddress, caller);
            audioSystem.wakeUpIfActiveSource();
            audioSystem.maySendActiveSource(sourceAddress);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void setLastInputForMhl(int portId) {
        assertRunOnServiceThread();
        this.mLastInputMhl = portId;
    }

    @HdmiAnnotations.ServiceThreadOnly
    int getLastInputForMhl() {
        assertRunOnServiceThread();
        return this.mLastInputMhl;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void changeInputForMhl(int portId, boolean contentOn) {
        assertRunOnServiceThread();
        if (tv() == null) {
            return;
        }
        final int lastInput = contentOn ? tv().getActivePortId() : -1;
        if (portId != -1) {
            tv().doManualPortSwitching(portId, new IHdmiControlCallback.Stub() { // from class: com.android.server.hdmi.HdmiControlService.17
                public void onComplete(int result) throws RemoteException {
                    HdmiControlService.this.setLastInputForMhl(lastInput);
                }
            });
        }
        tv().setActivePortId(portId);
        HdmiMhlLocalDeviceStub device = this.mMhlController.getLocalDevice(portId);
        HdmiDeviceInfo info = device != null ? device.getInfo() : this.mHdmiCecNetwork.getDeviceForPortId(portId);
        invokeInputChangeListener(info);
    }

    void setMhlInputChangeEnabled(boolean enabled) {
        this.mMhlController.setOption(101, toInt(enabled));
        synchronized (this.mLock) {
            this.mMhlInputChangeEnabled = enabled;
        }
    }

    protected HdmiCecAtomWriter getAtomWriter() {
        return this.mAtomWriter;
    }

    boolean isMhlInputChangeEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mMhlInputChangeEnabled;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void displayOsd(int messageId) {
        assertRunOnServiceThread();
        Intent intent = new Intent("android.hardware.hdmi.action.OSD_MESSAGE");
        intent.putExtra("android.hardware.hdmi.extra.MESSAGE_ID", messageId);
        getContext().sendBroadcastAsUser(intent, UserHandle.ALL, PERMISSION);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void displayOsd(int messageId, int extra) {
        assertRunOnServiceThread();
        Intent intent = new Intent("android.hardware.hdmi.action.OSD_MESSAGE");
        intent.putExtra("android.hardware.hdmi.extra.MESSAGE_ID", messageId);
        intent.putExtra("android.hardware.hdmi.extra.MESSAGE_EXTRA_PARAM1", extra);
        getContext().sendBroadcastAsUser(intent, UserHandle.ALL, PERMISSION);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public HdmiCecConfig getHdmiCecConfig() {
        return this.mHdmiCecConfig;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.hdmi.HdmiControlService$18  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass18 implements HdmiCecConfig.SettingChangeListener {
        AnonymousClass18() {
        }

        @Override // com.android.server.hdmi.HdmiCecConfig.SettingChangeListener
        public void onChange(final String name) {
            synchronized (HdmiControlService.this.mLock) {
                if (HdmiControlService.this.mHdmiCecSettingChangeListenerRecords.containsKey(name)) {
                    ((RemoteCallbackList) HdmiControlService.this.mHdmiCecSettingChangeListenerRecords.get(name)).broadcast(new Consumer() { // from class: com.android.server.hdmi.HdmiControlService$18$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            HdmiControlService.AnonymousClass18.this.m3853lambda$onChange$0$comandroidserverhdmiHdmiControlService$18(name, (IHdmiCecSettingChangeListener) obj);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onChange$0$com-android-server-hdmi-HdmiControlService$18  reason: not valid java name */
        public /* synthetic */ void m3853lambda$onChange$0$comandroidserverhdmiHdmiControlService$18(String name, IHdmiCecSettingChangeListener listener) {
            HdmiControlService.this.invokeCecSettingChangeListenerLocked(name, listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addCecSettingChangeListener(String name, IHdmiCecSettingChangeListener listener) {
        synchronized (this.mLock) {
            if (!this.mHdmiCecSettingChangeListenerRecords.containsKey(name)) {
                this.mHdmiCecSettingChangeListenerRecords.put(name, new RemoteCallbackList<>());
                this.mHdmiCecConfig.registerChangeListener(name, this.mSettingChangeListener);
            }
            this.mHdmiCecSettingChangeListenerRecords.get(name).register(listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeCecSettingChangeListener(String name, IHdmiCecSettingChangeListener listener) {
        synchronized (this.mLock) {
            if (this.mHdmiCecSettingChangeListenerRecords.containsKey(name)) {
                this.mHdmiCecSettingChangeListenerRecords.get(name).unregister(listener);
                if (this.mHdmiCecSettingChangeListenerRecords.get(name).getRegisteredCallbackCount() == 0) {
                    this.mHdmiCecSettingChangeListenerRecords.remove(name);
                    this.mHdmiCecConfig.removeChangeListener(name, this.mSettingChangeListener);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void invokeCecSettingChangeListenerLocked(String name, IHdmiCecSettingChangeListener listener) {
        try {
            listener.onChange(name);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to report setting change", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void onDeviceVolumeBehaviorChanged(AudioDeviceAttributes device, int volumeBehavior) {
        assertRunOnServiceThread();
        if (AVC_AUDIO_OUTPUT_DEVICES.contains(device)) {
            synchronized (this.mLock) {
                this.mAudioDeviceVolumeBehaviors.put(device, Integer.valueOf(volumeBehavior));
            }
            checkAndUpdateAbsoluteVolumeControlState();
        }
    }

    private int getDeviceVolumeBehavior(AudioDeviceAttributes device) {
        if (AVC_AUDIO_OUTPUT_DEVICES.contains(device)) {
            synchronized (this.mLock) {
                if (this.mAudioDeviceVolumeBehaviors.containsKey(device)) {
                    return this.mAudioDeviceVolumeBehaviors.get(device).intValue();
                }
            }
        }
        return getAudioManager().getDeviceVolumeBehavior(device);
    }

    public boolean isAbsoluteVolumeControlEnabled() {
        AudioDeviceAttributes avcAudioOutputDevice;
        return (isTvDevice() || isPlaybackDevice()) && (avcAudioOutputDevice = getAvcAudioOutputDevice()) != null && getDeviceVolumeBehavior(avcAudioOutputDevice) == 3;
    }

    private AudioDeviceAttributes getAvcAudioOutputDevice() {
        if (isTvDevice()) {
            return tv().getSystemAudioOutputDevice();
        }
        if (isPlaybackDevice()) {
            return AUDIO_OUTPUT_DEVICE_HDMI;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void checkAndUpdateAbsoluteVolumeControlState() {
        HdmiCecLocalDevice localCecDevice;
        assertRunOnServiceThread();
        if (getAudioManager() == null) {
            return;
        }
        if (isTvDevice() && tv() != null) {
            localCecDevice = tv();
            if (!isSystemAudioActivated()) {
                disableAbsoluteVolumeControl();
                return;
            }
        } else if (isPlaybackDevice() && playback() != null) {
            localCecDevice = playback();
        } else {
            return;
        }
        HdmiDeviceInfo systemAudioDeviceInfo = getHdmiCecNetwork().getSafeCecDeviceInfo(localCecDevice.findAudioReceiverAddress());
        int currentVolumeBehavior = getDeviceVolumeBehavior(getAvcAudioOutputDevice());
        boolean alreadyUsingFullOrAbsoluteVolume = currentVolumeBehavior == 1 || currentVolumeBehavior == 3;
        boolean cecVolumeEnabled = getHdmiCecVolumeControl() == 1;
        if (!cecVolumeEnabled || !alreadyUsingFullOrAbsoluteVolume) {
            disableAbsoluteVolumeControl();
        } else if (systemAudioDeviceInfo == null) {
            disableAbsoluteVolumeControl();
        } else {
            switch (systemAudioDeviceInfo.getDeviceFeatures().getSetAudioVolumeLevelSupport()) {
                case 0:
                    disableAbsoluteVolumeControl();
                    return;
                case 1:
                    if (!isAbsoluteVolumeControlEnabled()) {
                        localCecDevice.addAvcAudioStatusAction(systemAudioDeviceInfo.getLogicalAddress());
                        return;
                    }
                    return;
                case 2:
                    disableAbsoluteVolumeControl();
                    localCecDevice.queryAvcSupport(systemAudioDeviceInfo.getLogicalAddress());
                    return;
                default:
                    return;
            }
        }
    }

    private void disableAbsoluteVolumeControl() {
        if (isPlaybackDevice()) {
            playback().removeAvcAudioStatusAction();
        } else if (isTvDevice()) {
            tv().removeAvcAudioStatusAction();
        }
        AudioDeviceAttributes device = getAvcAudioOutputDevice();
        if (getDeviceVolumeBehavior(device) == 3) {
            getAudioManager().setDeviceVolumeBehavior(device, 1);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enableAbsoluteVolumeControl(AudioStatus audioStatus) {
        HdmiCecLocalDevice localDevice = isPlaybackDevice() ? playback() : tv();
        HdmiDeviceInfo systemAudioDevice = getHdmiCecNetwork().getDeviceInfo(localDevice.findAudioReceiverAddress());
        VolumeInfo volumeInfo = new VolumeInfo.Builder(3).setMuted(audioStatus.getMute()).setVolumeIndex(audioStatus.getVolume()).setMaxVolumeIndex(100).setMinVolumeIndex(0).build();
        this.mAbsoluteVolumeChangedListener = new AbsoluteVolumeChangedListener(localDevice, systemAudioDevice);
        notifyAvcMuteChange(audioStatus.getMute());
        getAudioDeviceVolumeManager().setDeviceAbsoluteVolumeBehavior(getAvcAudioOutputDevice(), volumeInfo, this.mServiceThreadExecutor, this.mAbsoluteVolumeChangedListener, true);
    }

    AbsoluteVolumeChangedListener getAbsoluteVolumeChangedListener() {
        return this.mAbsoluteVolumeChangedListener;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AbsoluteVolumeChangedListener implements AudioDeviceVolumeManager.OnAudioDeviceVolumeChangedListener {
        private HdmiCecLocalDevice mLocalDevice;
        private HdmiDeviceInfo mSystemAudioDevice;

        private AbsoluteVolumeChangedListener(HdmiCecLocalDevice localDevice, HdmiDeviceInfo systemAudioDevice) {
            this.mLocalDevice = localDevice;
            this.mSystemAudioDevice = systemAudioDevice;
        }

        public void onAudioDeviceVolumeChanged(AudioDeviceAttributes audioDevice, final VolumeInfo volumeInfo) {
            final int localDeviceAddress;
            synchronized (this.mLocalDevice.mLock) {
                localDeviceAddress = this.mLocalDevice.getDeviceInfo().getLogicalAddress();
            }
            HdmiControlService.this.sendCecCommand(SetAudioVolumeLevelMessage.build(localDeviceAddress, this.mSystemAudioDevice.getLogicalAddress(), volumeInfo.getVolumeIndex()), new SendMessageCallback() { // from class: com.android.server.hdmi.HdmiControlService$AbsoluteVolumeChangedListener$$ExternalSyntheticLambda0
                @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
                public final void onSendCompleted(int i) {
                    HdmiControlService.AbsoluteVolumeChangedListener.this.m3854xec16d786(volumeInfo, localDeviceAddress, i);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAudioDeviceVolumeChanged$0$com-android-server-hdmi-HdmiControlService$AbsoluteVolumeChangedListener  reason: not valid java name */
        public /* synthetic */ void m3854xec16d786(VolumeInfo volumeInfo, int localDeviceAddress, int errorCode) {
            if (errorCode == 0) {
                HdmiCecLocalDevice avcDevice = HdmiControlService.this.isTvDevice() ? HdmiControlService.this.tv() : HdmiControlService.this.playback();
                avcDevice.updateAvcVolume(volumeInfo.getVolumeIndex());
                return;
            }
            HdmiControlService.this.sendCecCommand(HdmiCecMessageBuilder.buildGiveAudioStatus(localDeviceAddress, this.mSystemAudioDevice.getLogicalAddress()));
        }

        public void onAudioDeviceVolumeAdjusted(AudioDeviceAttributes audioDevice, VolumeInfo volumeInfo, int direction, int mode) {
            int keyCode;
            switch (direction) {
                case -100:
                case 100:
                case 101:
                    keyCode = 164;
                    break;
                case -1:
                    keyCode = 25;
                    break;
                case 1:
                    keyCode = 24;
                    break;
                default:
                    return;
            }
            switch (mode) {
                case 0:
                    this.mLocalDevice.sendVolumeKeyEvent(keyCode, true);
                    this.mLocalDevice.sendVolumeKeyEvent(keyCode, false);
                    return;
                case 1:
                    this.mLocalDevice.sendVolumeKeyEvent(keyCode, true);
                    return;
                case 2:
                    this.mLocalDevice.sendVolumeKeyEvent(keyCode, false);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyAvcVolumeChange(int volume) {
        if (isAbsoluteVolumeControlEnabled()) {
            List<AudioDeviceAttributes> streamMusicDevices = getAudioManager().getDevicesForAttributes(STREAM_MUSIC_ATTRIBUTES);
            if (streamMusicDevices.contains(getAvcAudioOutputDevice())) {
                int flags = 8192;
                if (isTvDevice()) {
                    flags = 8192 | 1;
                }
                setStreamMusicVolume(volume, flags);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyAvcMuteChange(boolean mute) {
        if (isAbsoluteVolumeControlEnabled()) {
            List<AudioDeviceAttributes> streamMusicDevices = getAudioManager().getDevicesForAttributes(STREAM_MUSIC_ATTRIBUTES);
            if (streamMusicDevices.contains(getAvcAudioOutputDevice())) {
                int direction = mute ? -100 : 100;
                int flags = 8192;
                if (isTvDevice()) {
                    flags = 8192 | 1;
                }
                getAudioManager().adjustStreamVolume(3, direction, flags);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setStreamMusicVolume(int volume, int flags) {
        getAudioManager().setStreamVolume(3, (this.mStreamMusicMaxVolume * volume) / 100, flags);
    }
}
