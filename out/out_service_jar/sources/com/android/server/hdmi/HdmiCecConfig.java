package com.android.server.hdmi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.util.ConcurrentUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public class HdmiCecConfig {
    private static final String CONFIG_FILE = "cec_config.xml";
    private static final String ETC_DIR = "etc";
    private static final String SHARED_PREFS_DIR = "shared_prefs";
    private static final String SHARED_PREFS_NAME = "cec_config.xml";
    private static final int STORAGE_GLOBAL_SETTINGS = 1;
    private static final int STORAGE_SHARED_PREFS = 2;
    private static final int STORAGE_SYSPROPS = 0;
    private static final String TAG = "HdmiCecConfig";
    private static final String VALUE_TYPE_INT = "int";
    private static final String VALUE_TYPE_STRING = "string";
    private final Context mContext;
    private final Object mLock;
    private final ArrayMap<Setting, ArrayMap<SettingChangeListener, Executor>> mSettingChangeListeners;
    private LinkedHashMap<String, Setting> mSettings;
    private final StorageAdapter mStorageAdapter;

    /* loaded from: classes.dex */
    public interface SettingChangeListener {
        void onChange(String str);
    }

    /* loaded from: classes.dex */
    private @interface Storage {
    }

    /* loaded from: classes.dex */
    private @interface ValueType {
    }

    /* loaded from: classes.dex */
    public static class VerificationException extends RuntimeException {
        public VerificationException(String message) {
            super(message);
        }
    }

    /* loaded from: classes.dex */
    public static class StorageAdapter {
        private final Context mContext;
        private final SharedPreferences mSharedPrefs;

        StorageAdapter(Context context) {
            this.mContext = context;
            Context deviceContext = context.createDeviceProtectedStorageContext();
            File prefsFile = new File(new File(Environment.getDataSystemDirectory(), HdmiCecConfig.SHARED_PREFS_DIR), "cec_config.xml");
            this.mSharedPrefs = deviceContext.getSharedPreferences(prefsFile, 0);
        }

        public String retrieveSystemProperty(String storageKey, String defaultValue) {
            return SystemProperties.get(storageKey, defaultValue);
        }

        public void storeSystemProperty(String storageKey, String value) {
            SystemProperties.set(storageKey, value);
        }

        public String retrieveGlobalSetting(String storageKey, String defaultValue) {
            String value = Settings.Global.getString(this.mContext.getContentResolver(), storageKey);
            return value != null ? value : defaultValue;
        }

        public void storeGlobalSetting(String storageKey, String value) {
            Settings.Global.putString(this.mContext.getContentResolver(), storageKey, value);
        }

        public String retrieveSharedPref(String storageKey, String defaultValue) {
            return this.mSharedPrefs.getString(storageKey, defaultValue);
        }

        public void storeSharedPref(String storageKey, String value) {
            this.mSharedPrefs.edit().putString(storageKey, value).apply();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class Value {
        private final Integer mIntValue;
        private final String mStringValue;

        Value(String value) {
            this.mStringValue = value;
            this.mIntValue = null;
        }

        Value(Integer value) {
            this.mStringValue = null;
            this.mIntValue = value;
        }

        String getStringValue() {
            return this.mStringValue;
        }

        Integer getIntValue() {
            return this.mIntValue;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public class Setting {
        private final Context mContext;
        private final String mName;
        private final boolean mUserConfigurable;
        private Value mDefaultValue = null;
        private List<Value> mAllowedValues = new ArrayList();

        Setting(Context context, String name, int userConfResId) {
            this.mContext = context;
            this.mName = name;
            this.mUserConfigurable = context.getResources().getBoolean(userConfResId);
        }

        public String getName() {
            return this.mName;
        }

        public String getValueType() {
            if (getDefaultValue().getStringValue() != null) {
                return HdmiCecConfig.VALUE_TYPE_STRING;
            }
            return HdmiCecConfig.VALUE_TYPE_INT;
        }

        public Value getDefaultValue() {
            Value value = this.mDefaultValue;
            if (value == null) {
                throw new VerificationException("Invalid CEC setup for '" + getName() + "' setting. Setting has no default value.");
            }
            return value;
        }

        public boolean getUserConfigurable() {
            return this.mUserConfigurable;
        }

        private void registerValue(Value value, int allowedResId, int defaultResId) {
            if (this.mContext.getResources().getBoolean(allowedResId)) {
                this.mAllowedValues.add(value);
                if (this.mContext.getResources().getBoolean(defaultResId)) {
                    if (this.mDefaultValue != null) {
                        Slog.e(HdmiCecConfig.TAG, "Failed to set '" + value + "' as a default for '" + getName() + "': Setting already has a default ('" + this.mDefaultValue + "').");
                    } else {
                        this.mDefaultValue = value;
                    }
                }
            }
        }

        public void registerValue(String value, int allowedResId, int defaultResId) {
            registerValue(new Value(value), allowedResId, defaultResId);
        }

        public void registerValue(int value, int allowedResId, int defaultResId) {
            registerValue(new Value(Integer.valueOf(value)), allowedResId, defaultResId);
        }

        public List<Value> getAllowedValues() {
            return this.mAllowedValues;
        }
    }

    HdmiCecConfig(Context context, StorageAdapter storageAdapter) {
        this.mLock = new Object();
        this.mSettingChangeListeners = new ArrayMap<>();
        this.mSettings = new LinkedHashMap<>();
        this.mContext = context;
        this.mStorageAdapter = storageAdapter;
        Setting hdmiCecEnabled = registerSetting("hdmi_cec_enabled", 17891411);
        hdmiCecEnabled.registerValue(1, 17891409, 17891410);
        hdmiCecEnabled.registerValue(0, 17891407, 17891408);
        Setting hdmiCecVersion = registerSetting("hdmi_cec_version", 17891416);
        hdmiCecVersion.registerValue(5, 17891412, 17891413);
        hdmiCecVersion.registerValue(6, 17891414, 17891415);
        Setting routingControlControl = registerSetting("routing_control", 17891546);
        routingControlControl.registerValue(1, 17891544, 17891545);
        routingControlControl.registerValue(0, 17891542, 17891543);
        Setting powerControlMode = registerSetting("power_control_mode", 17891425);
        powerControlMode.registerValue("to_tv", 17891423, 17891424);
        powerControlMode.registerValue("broadcast", 17891417, 17891418);
        powerControlMode.registerValue("none", 17891419, 17891420);
        powerControlMode.registerValue("to_tv_and_audio_system", 17891421, 17891422);
        Setting powerStateChangeOnActiveSourceLost = registerSetting("power_state_change_on_active_source_lost", 17891430);
        powerStateChangeOnActiveSourceLost.registerValue("none", 17891426, 17891427);
        powerStateChangeOnActiveSourceLost.registerValue("standby_now", 17891428, 17891429);
        Setting systemAudioControl = registerSetting("system_audio_control", 17891556);
        systemAudioControl.registerValue(1, 17891554, 17891555);
        systemAudioControl.registerValue(0, 17891552, 17891553);
        Setting systemAudioModeMuting = registerSetting("system_audio_mode_muting", 17891561);
        systemAudioModeMuting.registerValue(1, 17891559, 17891560);
        systemAudioModeMuting.registerValue(0, 17891557, 17891558);
        Setting volumeControlMode = registerSetting("volume_control_enabled", 17891576);
        volumeControlMode.registerValue(1, 17891574, 17891575);
        volumeControlMode.registerValue(0, 17891572, 17891573);
        Setting tvWakeOnOneTouchPlay = registerSetting("tv_wake_on_one_touch_play", 17891571);
        tvWakeOnOneTouchPlay.registerValue(1, 17891569, 17891570);
        tvWakeOnOneTouchPlay.registerValue(0, 17891567, 17891568);
        Setting tvSendStandbyOnSleep = registerSetting("tv_send_standby_on_sleep", 17891566);
        tvSendStandbyOnSleep.registerValue(1, 17891564, 17891565);
        tvSendStandbyOnSleep.registerValue(0, 17891562, 17891563);
        Setting setMenuLanguage = registerSetting("set_menu_language", 17891551);
        setMenuLanguage.registerValue(1, 17891549, 17891550);
        setMenuLanguage.registerValue(0, 17891547, 17891548);
        Setting rcProfileTv = registerSetting("rc_profile_tv", 17891541);
        rcProfileTv.registerValue(0, 17891533, 17891534);
        rcProfileTv.registerValue(2, 17891535, 17891536);
        rcProfileTv.registerValue(6, 17891539, 17891540);
        rcProfileTv.registerValue(10, 17891537, 17891538);
        rcProfileTv.registerValue(14, 17891531, 17891532);
        Setting rcProfileSourceRootMenu = registerSetting("rc_profile_source_handles_root_menu", 17891520);
        rcProfileSourceRootMenu.registerValue(1, 17891516, 17891517);
        rcProfileSourceRootMenu.registerValue(0, 17891518, 17891519);
        Setting rcProfileSourceSetupMenu = registerSetting("rc_profile_source_handles_setup_menu", 17891525);
        rcProfileSourceSetupMenu.registerValue(1, 17891521, 17891522);
        rcProfileSourceSetupMenu.registerValue(0, 17891523, 17891524);
        Setting rcProfileSourceContentsMenu = registerSetting("rc_profile_source_handles_contents_menu", 17891510);
        rcProfileSourceContentsMenu.registerValue(1, 17891506, 17891507);
        rcProfileSourceContentsMenu.registerValue(0, 17891508, 17891509);
        Setting rcProfileSourceTopMenu = registerSetting("rc_profile_source_handles_top_menu", 17891530);
        rcProfileSourceTopMenu.registerValue(1, 17891526, 17891527);
        rcProfileSourceTopMenu.registerValue(0, 17891528, 17891529);
        Setting rcProfileSourceMediaContextSensitiveMenu = registerSetting("rc_profile_source_handles_media_context_sensitive_menu", 17891515);
        rcProfileSourceMediaContextSensitiveMenu.registerValue(1, 17891511, 17891512);
        rcProfileSourceMediaContextSensitiveMenu.registerValue(0, 17891513, 17891514);
        Setting querySadLpcm = registerSetting("query_sad_lpcm", 17891470);
        querySadLpcm.registerValue(1, 17891468, 17891469);
        querySadLpcm.registerValue(0, 17891466, 17891467);
        Setting querySadDd = registerSetting("query_sad_dd", 17891445);
        querySadDd.registerValue(1, 17891443, 17891444);
        querySadDd.registerValue(0, 17891441, 17891442);
        Setting querySadMpeg1 = registerSetting("query_sad_mpeg1", 17891485);
        querySadMpeg1.registerValue(1, 17891483, 17891484);
        querySadMpeg1.registerValue(0, 17891481, 17891482);
        Setting querySadMp3 = registerSetting("query_sad_mp3", 17891480);
        querySadMp3.registerValue(1, 17891478, 17891479);
        querySadMp3.registerValue(0, 17891476, 17891477);
        Setting querySadMpeg2 = registerSetting("query_sad_mpeg2", 17891490);
        querySadMpeg2.registerValue(1, 17891488, 17891489);
        querySadMpeg2.registerValue(0, 17891486, 17891487);
        Setting querySadAac = registerSetting("query_sad_aac", 17891435);
        querySadAac.registerValue(1, 17891433, 17891434);
        querySadAac.registerValue(0, 17891431, 17891432);
        Setting querySadDts = registerSetting("query_sad_dts", 17891460);
        querySadDts.registerValue(1, 17891458, 17891459);
        querySadDts.registerValue(0, 17891456, 17891457);
        Setting querySadAtrac = registerSetting("query_sad_atrac", 17891440);
        querySadAtrac.registerValue(1, 17891438, 17891439);
        querySadAtrac.registerValue(0, 17891436, 17891437);
        Setting querySadOnebitaudio = registerSetting("query_sad_onebitaudio", 17891495);
        querySadOnebitaudio.registerValue(1, 17891493, 17891494);
        querySadOnebitaudio.registerValue(0, 17891491, 17891492);
        Setting querySadDdp = registerSetting("query_sad_ddp", 17891450);
        querySadDdp.registerValue(1, 17891448, 17891449);
        querySadDdp.registerValue(0, 17891446, 17891447);
        Setting querySadDtshd = registerSetting("query_sad_dtshd", 17891465);
        querySadDtshd.registerValue(1, 17891463, 17891464);
        querySadDtshd.registerValue(0, 17891461, 17891462);
        Setting querySadTruehd = registerSetting("query_sad_truehd", 17891500);
        querySadTruehd.registerValue(1, 17891498, 17891499);
        querySadTruehd.registerValue(0, 17891496, 17891497);
        Setting querySadDst = registerSetting("query_sad_dst", 17891455);
        querySadDst.registerValue(1, 17891453, 17891454);
        querySadDst.registerValue(0, 17891451, 17891452);
        Setting querySadWmapro = registerSetting("query_sad_wmapro", 17891505);
        querySadWmapro.registerValue(1, 17891503, 17891504);
        querySadWmapro.registerValue(0, 17891501, 17891502);
        Setting querySadMax = registerSetting("query_sad_max", 17891475);
        querySadMax.registerValue(1, 17891473, 17891474);
        querySadMax.registerValue(0, 17891471, 17891472);
        verifySettings();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiCecConfig(Context context) {
        this(context, new StorageAdapter(context));
    }

    private Setting registerSetting(String name, int userConfResId) {
        Setting setting = new Setting(this.mContext, name, userConfResId);
        this.mSettings.put(name, setting);
        return setting;
    }

    private void verifySettings() {
        for (Setting setting : this.mSettings.values()) {
            setting.getDefaultValue();
            getStorage(setting);
            getStorageKey(setting);
        }
    }

    private Setting getSetting(String name) {
        if (this.mSettings.containsKey(name)) {
            return this.mSettings.get(name);
        }
        return null;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int getStorage(Setting setting) {
        char c;
        String name = setting.getName();
        switch (name.hashCode()) {
            case -2072577869:
                if (name.equals("hdmi_cec_version")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1788790343:
                if (name.equals("system_audio_mode_muting")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -1618836197:
                if (name.equals("set_menu_language")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case -1275604441:
                if (name.equals("rc_profile_source_handles_media_context_sensitive_menu")) {
                    c = 16;
                    break;
                }
                c = 65535;
                break;
            case -1253675651:
                if (name.equals("rc_profile_source_handles_top_menu")) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case -1188289112:
                if (name.equals("rc_profile_source_handles_root_menu")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case -1157203295:
                if (name.equals("query_sad_atrac")) {
                    c = 24;
                    break;
                }
                c = 65535;
                break;
            case -1154431553:
                if (name.equals("query_sad_dtshd")) {
                    c = 27;
                    break;
                }
                c = 65535;
                break;
            case -1146252564:
                if (name.equals("query_sad_mpeg1")) {
                    c = 19;
                    break;
                }
                c = 65535;
                break;
            case -1146252563:
                if (name.equals("query_sad_mpeg2")) {
                    c = 21;
                    break;
                }
                c = 65535;
                break;
            case -971363478:
                if (name.equals("query_sad_truehd")) {
                    c = 28;
                    break;
                }
                c = 65535;
                break;
            case -910325648:
                if (name.equals("rc_profile_source_handles_contents_menu")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case -890678558:
                if (name.equals("query_sad_wmapro")) {
                    c = 30;
                    break;
                }
                c = 65535;
                break;
            case -412334364:
                if (name.equals("routing_control")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -314100402:
                if (name.equals("query_sad_lpcm")) {
                    c = 17;
                    break;
                }
                c = 65535;
                break;
            case -293445547:
                if (name.equals("rc_profile_source_handles_setup_menu")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case -219770232:
                if (name.equals("power_state_change_on_active_source_lost")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -25374657:
                if (name.equals("power_control_mode")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 73184058:
                if (name.equals("volume_control_enabled")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 261187356:
                if (name.equals("hdmi_cec_enabled")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 791759782:
                if (name.equals("rc_profile_tv")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 799280879:
                if (name.equals("query_sad_onebitaudio")) {
                    c = 25;
                    break;
                }
                c = 65535;
                break;
            case 1577324768:
                if (name.equals("query_sad_dd")) {
                    c = 18;
                    break;
                }
                c = 65535;
                break;
            case 1629183631:
                if (name.equals("tv_wake_on_one_touch_play")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 1652424675:
                if (name.equals("query_sad_aac")) {
                    c = 22;
                    break;
                }
                c = 65535;
                break;
            case 1652427664:
                if (name.equals("query_sad_ddp")) {
                    c = 26;
                    break;
                }
                c = 65535;
                break;
            case 1652428133:
                if (name.equals("query_sad_dst")) {
                    c = 29;
                    break;
                }
                c = 65535;
                break;
            case 1652428163:
                if (name.equals("query_sad_dts")) {
                    c = 23;
                    break;
                }
                c = 65535;
                break;
            case 1652436228:
                if (name.equals("query_sad_max")) {
                    c = 31;
                    break;
                }
                c = 65535;
                break;
            case 1652436624:
                if (name.equals("query_sad_mp3")) {
                    c = 20;
                    break;
                }
                c = 65535;
                break;
            case 2055627683:
                if (name.equals("tv_send_standby_on_sleep")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 2118236132:
                if (name.equals("system_audio_control")) {
                    c = 6;
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
                return 2;
            case 1:
                return 2;
            case 2:
                return 2;
            case 3:
                return 2;
            case 4:
                return 2;
            case 5:
                return 2;
            case 6:
                return 2;
            case 7:
                return 2;
            case '\b':
                return 2;
            case '\t':
                return 2;
            case '\n':
                return 2;
            case 11:
                return 2;
            case '\f':
                return 2;
            case '\r':
                return 2;
            case 14:
                return 2;
            case 15:
                return 2;
            case 16:
                return 2;
            case 17:
                return 2;
            case 18:
                return 2;
            case 19:
                return 2;
            case 20:
                return 2;
            case 21:
                return 2;
            case 22:
                return 2;
            case 23:
                return 2;
            case 24:
                return 2;
            case 25:
                return 2;
            case 26:
                return 2;
            case 27:
                return 2;
            case 28:
                return 2;
            case 29:
                return 2;
            case 30:
                return 2;
            case 31:
                return 2;
            default:
                throw new VerificationException("Invalid CEC setting '" + setting.getName() + "' storage.");
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private String getStorageKey(Setting setting) {
        char c;
        String name = setting.getName();
        switch (name.hashCode()) {
            case -2072577869:
                if (name.equals("hdmi_cec_version")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1788790343:
                if (name.equals("system_audio_mode_muting")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -1618836197:
                if (name.equals("set_menu_language")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case -1275604441:
                if (name.equals("rc_profile_source_handles_media_context_sensitive_menu")) {
                    c = 16;
                    break;
                }
                c = 65535;
                break;
            case -1253675651:
                if (name.equals("rc_profile_source_handles_top_menu")) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case -1188289112:
                if (name.equals("rc_profile_source_handles_root_menu")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case -1157203295:
                if (name.equals("query_sad_atrac")) {
                    c = 24;
                    break;
                }
                c = 65535;
                break;
            case -1154431553:
                if (name.equals("query_sad_dtshd")) {
                    c = 27;
                    break;
                }
                c = 65535;
                break;
            case -1146252564:
                if (name.equals("query_sad_mpeg1")) {
                    c = 19;
                    break;
                }
                c = 65535;
                break;
            case -1146252563:
                if (name.equals("query_sad_mpeg2")) {
                    c = 21;
                    break;
                }
                c = 65535;
                break;
            case -971363478:
                if (name.equals("query_sad_truehd")) {
                    c = 28;
                    break;
                }
                c = 65535;
                break;
            case -910325648:
                if (name.equals("rc_profile_source_handles_contents_menu")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case -890678558:
                if (name.equals("query_sad_wmapro")) {
                    c = 30;
                    break;
                }
                c = 65535;
                break;
            case -412334364:
                if (name.equals("routing_control")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -314100402:
                if (name.equals("query_sad_lpcm")) {
                    c = 17;
                    break;
                }
                c = 65535;
                break;
            case -293445547:
                if (name.equals("rc_profile_source_handles_setup_menu")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case -219770232:
                if (name.equals("power_state_change_on_active_source_lost")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -25374657:
                if (name.equals("power_control_mode")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 73184058:
                if (name.equals("volume_control_enabled")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 261187356:
                if (name.equals("hdmi_cec_enabled")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 791759782:
                if (name.equals("rc_profile_tv")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 799280879:
                if (name.equals("query_sad_onebitaudio")) {
                    c = 25;
                    break;
                }
                c = 65535;
                break;
            case 1577324768:
                if (name.equals("query_sad_dd")) {
                    c = 18;
                    break;
                }
                c = 65535;
                break;
            case 1629183631:
                if (name.equals("tv_wake_on_one_touch_play")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 1652424675:
                if (name.equals("query_sad_aac")) {
                    c = 22;
                    break;
                }
                c = 65535;
                break;
            case 1652427664:
                if (name.equals("query_sad_ddp")) {
                    c = 26;
                    break;
                }
                c = 65535;
                break;
            case 1652428133:
                if (name.equals("query_sad_dst")) {
                    c = 29;
                    break;
                }
                c = 65535;
                break;
            case 1652428163:
                if (name.equals("query_sad_dts")) {
                    c = 23;
                    break;
                }
                c = 65535;
                break;
            case 1652436228:
                if (name.equals("query_sad_max")) {
                    c = 31;
                    break;
                }
                c = 65535;
                break;
            case 1652436624:
                if (name.equals("query_sad_mp3")) {
                    c = 20;
                    break;
                }
                c = 65535;
                break;
            case 2055627683:
                if (name.equals("tv_send_standby_on_sleep")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 2118236132:
                if (name.equals("system_audio_control")) {
                    c = 6;
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
                return setting.getName();
            case 1:
                return setting.getName();
            case 2:
                return setting.getName();
            case 3:
                return setting.getName();
            case 4:
                return setting.getName();
            case 5:
                return setting.getName();
            case 6:
                return setting.getName();
            case 7:
                return setting.getName();
            case '\b':
                return setting.getName();
            case '\t':
                return setting.getName();
            case '\n':
                return setting.getName();
            case 11:
                return setting.getName();
            case '\f':
                return setting.getName();
            case '\r':
                return setting.getName();
            case 14:
                return setting.getName();
            case 15:
                return setting.getName();
            case 16:
                return setting.getName();
            case 17:
                return setting.getName();
            case 18:
                return setting.getName();
            case 19:
                return setting.getName();
            case 20:
                return setting.getName();
            case 21:
                return setting.getName();
            case 22:
                return setting.getName();
            case 23:
                return setting.getName();
            case 24:
                return setting.getName();
            case 25:
                return setting.getName();
            case 26:
                return setting.getName();
            case 27:
                return setting.getName();
            case 28:
                return setting.getName();
            case 29:
                return setting.getName();
            case 30:
                return setting.getName();
            case 31:
                return setting.getName();
            default:
                throw new VerificationException("Invalid CEC setting '" + setting.getName() + "' storage key.");
        }
    }

    protected String retrieveValue(Setting setting, String defaultValue) {
        int storage = getStorage(setting);
        String storageKey = getStorageKey(setting);
        if (storage == 0) {
            HdmiLogger.debug("Reading '" + storageKey + "' sysprop.", new Object[0]);
            return this.mStorageAdapter.retrieveSystemProperty(storageKey, defaultValue);
        } else if (storage == 1) {
            HdmiLogger.debug("Reading '" + storageKey + "' global setting.", new Object[0]);
            return this.mStorageAdapter.retrieveGlobalSetting(storageKey, defaultValue);
        } else if (storage == 2) {
            HdmiLogger.debug("Reading '" + storageKey + "' shared preference.", new Object[0]);
            return this.mStorageAdapter.retrieveSharedPref(storageKey, defaultValue);
        } else {
            return null;
        }
    }

    protected void storeValue(Setting setting, String value) {
        int storage = getStorage(setting);
        String storageKey = getStorageKey(setting);
        if (storage == 0) {
            HdmiLogger.debug("Setting '" + storageKey + "' sysprop.", new Object[0]);
            this.mStorageAdapter.storeSystemProperty(storageKey, value);
        } else if (storage == 1) {
            HdmiLogger.debug("Setting '" + storageKey + "' global setting.", new Object[0]);
            this.mStorageAdapter.storeGlobalSetting(storageKey, value);
        } else if (storage == 2) {
            HdmiLogger.debug("Setting '" + storageKey + "' shared pref.", new Object[0]);
            this.mStorageAdapter.storeSharedPref(storageKey, value);
            notifySettingChanged(setting);
        }
    }

    private void notifySettingChanged(String name) {
        Setting setting = getSetting(name);
        if (setting == null) {
            throw new IllegalArgumentException("Setting '" + name + "' does not exist.");
        }
        notifySettingChanged(setting);
    }

    protected void notifySettingChanged(final Setting setting) {
        synchronized (this.mLock) {
            ArrayMap<SettingChangeListener, Executor> listeners = this.mSettingChangeListeners.get(setting);
            if (listeners == null) {
                return;
            }
            for (Map.Entry<SettingChangeListener, Executor> entry : listeners.entrySet()) {
                final SettingChangeListener listener = entry.getKey();
                Executor executor = entry.getValue();
                executor.execute(new Runnable() { // from class: com.android.server.hdmi.HdmiCecConfig.1
                    @Override // java.lang.Runnable
                    public void run() {
                        listener.onChange(setting.getName());
                    }
                });
            }
        }
    }

    public void registerChangeListener(String name, SettingChangeListener listener) {
        registerChangeListener(name, listener, ConcurrentUtils.DIRECT_EXECUTOR);
    }

    public void registerChangeListener(String name, SettingChangeListener listener, Executor executor) {
        Setting setting = getSetting(name);
        if (setting == null) {
            throw new IllegalArgumentException("Setting '" + name + "' does not exist.");
        }
        int storage = getStorage(setting);
        if (storage != 1 && storage != 2) {
            throw new IllegalArgumentException("Change listeners for setting '" + name + "' not supported.");
        }
        synchronized (this.mLock) {
            if (!this.mSettingChangeListeners.containsKey(setting)) {
                this.mSettingChangeListeners.put(setting, new ArrayMap<>());
            }
            this.mSettingChangeListeners.get(setting).put(listener, executor);
        }
    }

    public void removeChangeListener(String name, SettingChangeListener listener) {
        Setting setting = getSetting(name);
        if (setting == null) {
            throw new IllegalArgumentException("Setting '" + name + "' does not exist.");
        }
        synchronized (this.mLock) {
            if (this.mSettingChangeListeners.containsKey(setting)) {
                ArrayMap<SettingChangeListener, Executor> listeners = this.mSettingChangeListeners.get(setting);
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    this.mSettingChangeListeners.remove(setting);
                }
            }
        }
    }

    public List<String> getAllSettings() {
        return new ArrayList(this.mSettings.keySet());
    }

    public List<String> getUserSettings() {
        List<String> settings = new ArrayList<>();
        for (Setting setting : this.mSettings.values()) {
            if (setting.getUserConfigurable()) {
                settings.add(setting.getName());
            }
        }
        return settings;
    }

    public boolean isStringValueType(String name) {
        Setting setting = getSetting(name);
        if (setting == null) {
            throw new IllegalArgumentException("Setting '" + name + "' does not exist.");
        }
        return getSetting(name).getValueType().equals(VALUE_TYPE_STRING);
    }

    public boolean isIntValueType(String name) {
        Setting setting = getSetting(name);
        if (setting == null) {
            throw new IllegalArgumentException("Setting '" + name + "' does not exist.");
        }
        return getSetting(name).getValueType().equals(VALUE_TYPE_INT);
    }

    public List<String> getAllowedStringValues(String name) {
        Setting setting = getSetting(name);
        if (setting == null) {
            throw new IllegalArgumentException("Setting '" + name + "' does not exist.");
        }
        if (!setting.getValueType().equals(VALUE_TYPE_STRING)) {
            throw new IllegalArgumentException("Setting '" + name + "' is not a string-type setting.");
        }
        List<String> allowedValues = new ArrayList<>();
        for (Value allowedValue : setting.getAllowedValues()) {
            allowedValues.add(allowedValue.getStringValue());
        }
        return allowedValues;
    }

    public List<Integer> getAllowedIntValues(String name) {
        Setting setting = getSetting(name);
        if (setting == null) {
            throw new IllegalArgumentException("Setting '" + name + "' does not exist.");
        }
        if (!setting.getValueType().equals(VALUE_TYPE_INT)) {
            throw new IllegalArgumentException("Setting '" + name + "' is not a string-type setting.");
        }
        List<Integer> allowedValues = new ArrayList<>();
        for (Value allowedValue : setting.getAllowedValues()) {
            allowedValues.add(allowedValue.getIntValue());
        }
        return allowedValues;
    }

    public String getDefaultStringValue(String name) {
        Setting setting = getSetting(name);
        if (setting == null) {
            throw new IllegalArgumentException("Setting '" + name + "' does not exist.");
        }
        if (!setting.getValueType().equals(VALUE_TYPE_STRING)) {
            throw new IllegalArgumentException("Setting '" + name + "' is not a string-type setting.");
        }
        return getSetting(name).getDefaultValue().getStringValue();
    }

    public int getDefaultIntValue(String name) {
        Setting setting = getSetting(name);
        if (setting == null) {
            throw new IllegalArgumentException("Setting '" + name + "' does not exist.");
        }
        if (!setting.getValueType().equals(VALUE_TYPE_INT)) {
            throw new IllegalArgumentException("Setting '" + name + "' is not a string-type setting.");
        }
        return getSetting(name).getDefaultValue().getIntValue().intValue();
    }

    public String getStringValue(String name) {
        Setting setting = getSetting(name);
        if (setting == null) {
            throw new IllegalArgumentException("Setting '" + name + "' does not exist.");
        }
        if (!setting.getValueType().equals(VALUE_TYPE_STRING)) {
            throw new IllegalArgumentException("Setting '" + name + "' is not a string-type setting.");
        }
        HdmiLogger.debug("Getting CEC setting value '" + name + "'.", new Object[0]);
        return retrieveValue(setting, setting.getDefaultValue().getStringValue());
    }

    public int getIntValue(String name) {
        Setting setting = getSetting(name);
        if (setting == null) {
            throw new IllegalArgumentException("Setting '" + name + "' does not exist.");
        }
        if (!setting.getValueType().equals(VALUE_TYPE_INT)) {
            throw new IllegalArgumentException("Setting '" + name + "' is not a int-type setting.");
        }
        HdmiLogger.debug("Getting CEC setting value '" + name + "'.", new Object[0]);
        String defaultValue = Integer.toString(setting.getDefaultValue().getIntValue().intValue());
        String value = retrieveValue(setting, defaultValue);
        return Integer.parseInt(value);
    }

    public void setStringValue(String name, String value) {
        Setting setting = getSetting(name);
        if (setting == null) {
            throw new IllegalArgumentException("Setting '" + name + "' does not exist.");
        }
        if (!setting.getUserConfigurable()) {
            throw new IllegalArgumentException("Updating CEC setting '" + name + "' prohibited.");
        }
        if (!setting.getValueType().equals(VALUE_TYPE_STRING)) {
            throw new IllegalArgumentException("Setting '" + name + "' is not a string-type setting.");
        }
        if (!getAllowedStringValues(name).contains(value)) {
            throw new IllegalArgumentException("Invalid CEC setting '" + name + "' value: '" + value + "'.");
        }
        HdmiLogger.debug("Updating CEC setting '" + name + "' to '" + value + "'.", new Object[0]);
        storeValue(setting, value);
    }

    public void setIntValue(String name, int value) {
        Setting setting = getSetting(name);
        if (setting == null) {
            throw new IllegalArgumentException("Setting '" + name + "' does not exist.");
        }
        if (!setting.getUserConfigurable()) {
            throw new IllegalArgumentException("Updating CEC setting '" + name + "' prohibited.");
        }
        if (!setting.getValueType().equals(VALUE_TYPE_INT)) {
            throw new IllegalArgumentException("Setting '" + name + "' is not a int-type setting.");
        }
        if (!getAllowedIntValues(name).contains(Integer.valueOf(value))) {
            throw new IllegalArgumentException("Invalid CEC setting '" + name + "' value: '" + value + "'.");
        }
        HdmiLogger.debug("Updating CEC setting '" + name + "' to '" + value + "'.", new Object[0]);
        storeValue(setting, Integer.toString(value));
    }
}
