package com.android.server.app;

import android.app.ActivityManager;
import android.app.GameModeInfo;
import android.app.GameState;
import android.app.IGameManagerService;
import android.app.compat.PackageOverride;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.provider.DeviceConfig;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.KeyValueListParser;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.R;
import com.android.internal.compat.CompatibilityOverrideConfig;
import com.android.internal.compat.IPlatformCompat;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.wm.CompatModePackages;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public final class GameManagerService extends IGameManagerService.Stub {
    static final int CANCEL_GAME_LOADING_MODE = 5;
    private static final boolean DEBUG = false;
    static final int LOADING_BOOST_MAX_DURATION = 5000;
    private static final String PACKAGE_NAME_MSG_KEY = "packageName";
    static final int POPULATE_GAME_MODE_SETTINGS = 3;
    static final int REMOVE_SETTINGS = 2;
    static final int SET_GAME_STATE = 4;
    public static final String TAG = "GameManagerService";
    private static final String USER_ID_MSG_KEY = "userId";
    static final int WRITE_SETTINGS = 1;
    static final int WRITE_SETTINGS_DELAY = 10000;
    private final ArrayMap<String, GamePackageConfiguration> mConfigs;
    private final Context mContext;
    private DeviceConfigListener mDeviceConfigListener;
    private final Object mDeviceConfigLock;
    private final GameServiceController mGameServiceController;
    private final Handler mHandler;
    private final Object mLock;
    private final Object mOverrideConfigLock;
    private final ArrayMap<String, GamePackageConfiguration> mOverrideConfigs;
    private final PackageManager mPackageManager;
    private final IPlatformCompat mPlatformCompat;
    private final PowerManagerInternal mPowerManagerInternal;
    private final ArrayMap<Integer, GameManagerSettings> mSettings;
    static final PackageOverride COMPAT_ENABLED = new PackageOverride.Builder().setEnabled(true).build();
    static final PackageOverride COMPAT_DISABLED = new PackageOverride.Builder().setEnabled(false).build();

    private static native void nativeSetOverrideFrameRate(int i, float f);

    public GameManagerService(Context context) {
        this(context, createServiceThread().getLooper());
    }

    GameManagerService(Context context, Looper looper) {
        this.mLock = new Object();
        this.mDeviceConfigLock = new Object();
        this.mOverrideConfigLock = new Object();
        this.mSettings = new ArrayMap<>();
        this.mConfigs = new ArrayMap<>();
        this.mOverrideConfigs = new ArrayMap<>();
        this.mContext = context;
        this.mHandler = new SettingsHandler(looper);
        this.mPackageManager = context.getPackageManager();
        this.mPlatformCompat = IPlatformCompat.Stub.asInterface(ServiceManager.getService("platform_compat"));
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        if (context.getPackageManager().hasSystemFeature("android.software.game_service")) {
            this.mGameServiceController = new GameServiceController(context, BackgroundThread.getExecutor(), new GameServiceProviderSelectorImpl(context.getResources(), context.getPackageManager()), new GameServiceProviderInstanceFactoryImpl(context));
        } else {
            this.mGameServiceController = null;
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.app.GameManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver result) {
        new GameManagerShellCommand().exec(this, in, out, err, args, callback, result);
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            writer.println("Permission Denial: can't dump GameManagerService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
        } else if (args == null || args.length == 0) {
            writer.println("*Dump GameManagerService*");
            dumpAllGameConfigs(writer);
        }
    }

    private void dumpAllGameConfigs(PrintWriter pw) {
        int userId = ActivityManager.getCurrentUser();
        String[] packageList = getInstalledGamePackageNames(userId);
        for (String packageName : packageList) {
            pw.println(getInterventionList(packageName));
        }
    }

    /* loaded from: classes.dex */
    class SettingsHandler extends Handler {
        SettingsHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            doHandleMessage(msg);
        }

        void doHandleMessage(Message msg) {
            int uid;
            switch (msg.what) {
                case 1:
                    int userId = ((Integer) msg.obj).intValue();
                    if (userId < 0) {
                        Slog.wtf(GameManagerService.TAG, "Attempt to write settings for invalid user: " + userId);
                        synchronized (GameManagerService.this.mLock) {
                            removeMessages(1, msg.obj);
                        }
                        return;
                    }
                    Process.setThreadPriority(0);
                    synchronized (GameManagerService.this.mLock) {
                        removeMessages(1, msg.obj);
                        if (GameManagerService.this.mSettings.containsKey(Integer.valueOf(userId))) {
                            GameManagerSettings userSettings = (GameManagerSettings) GameManagerService.this.mSettings.get(Integer.valueOf(userId));
                            userSettings.writePersistentDataLocked();
                        }
                    }
                    Process.setThreadPriority(10);
                    return;
                case 2:
                    int userId2 = ((Integer) msg.obj).intValue();
                    if (userId2 < 0) {
                        Slog.wtf(GameManagerService.TAG, "Attempt to write settings for invalid user: " + userId2);
                        synchronized (GameManagerService.this.mLock) {
                            removeMessages(1, msg.obj);
                            removeMessages(2, msg.obj);
                        }
                        return;
                    }
                    synchronized (GameManagerService.this.mLock) {
                        removeMessages(1, msg.obj);
                        removeMessages(2, msg.obj);
                        if (GameManagerService.this.mSettings.containsKey(Integer.valueOf(userId2))) {
                            GameManagerSettings userSettings2 = (GameManagerSettings) GameManagerService.this.mSettings.get(Integer.valueOf(userId2));
                            GameManagerService.this.mSettings.remove(Integer.valueOf(userId2));
                            userSettings2.writePersistentDataLocked();
                        }
                    }
                    return;
                case 3:
                    removeMessages(3, msg.obj);
                    int userId3 = ((Integer) msg.obj).intValue();
                    String[] packageNames = GameManagerService.this.getInstalledGamePackageNames(userId3);
                    GameManagerService.this.updateConfigsForUser(userId3, packageNames);
                    return;
                case 4:
                    GameState gameState = (GameState) msg.obj;
                    boolean isLoading = gameState.isLoading();
                    Bundle data = msg.getData();
                    String packageName = data.getString("packageName");
                    int userId4 = data.getInt("userId");
                    boolean boostEnabled = GameManagerService.this.getGameMode(packageName, userId4) == 2;
                    try {
                        uid = GameManagerService.this.mPackageManager.getPackageUidAsUser(packageName, userId4);
                    } catch (PackageManager.NameNotFoundException e) {
                        Slog.v(GameManagerService.TAG, "Failed to get package metadata");
                        uid = -1;
                    }
                    FrameworkStatsLog.write((int) FrameworkStatsLog.GAME_STATE_CHANGED, packageName, uid, boostEnabled, GameManagerService.gameStateModeToStatsdGameState(gameState.getMode()), isLoading, gameState.getLabel(), gameState.getQuality());
                    if (boostEnabled) {
                        if (GameManagerService.this.mPowerManagerInternal == null) {
                            Slog.d(GameManagerService.TAG, "Error setting loading mode for package " + packageName + " and userId " + userId4);
                            return;
                        } else {
                            GameManagerService.this.mPowerManagerInternal.setPowerMode(16, isLoading);
                            return;
                        }
                    }
                    return;
                case 5:
                    GameManagerService.this.mPowerManagerInternal.setPowerMode(16, false);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DeviceConfigListener implements DeviceConfig.OnPropertiesChangedListener {
        DeviceConfigListener() {
            DeviceConfig.addOnPropertiesChangedListener("game_overlay", GameManagerService.this.mContext.getMainExecutor(), this);
        }

        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            String[] packageNames = (String[]) properties.getKeyset().toArray(new String[0]);
            GameManagerService.this.updateConfigsForUser(ActivityManager.getCurrentUser(), packageNames);
        }

        public void finalize() {
            DeviceConfig.removeOnPropertiesChangedListener(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static long getCompatChangeId(String raw) {
        char c;
        switch (raw.hashCode()) {
            case 47605:
                if (raw.equals("0.3")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 47606:
                if (raw.equals("0.4")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 47607:
                if (raw.equals("0.5")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 47608:
                if (raw.equals("0.6")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 47609:
                if (raw.equals("0.7")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 47610:
                if (raw.equals("0.8")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case 47611:
                if (raw.equals("0.9")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case 1475808:
                if (raw.equals("0.35")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1475839:
                if (raw.equals("0.45")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1475870:
                if (raw.equals("0.55")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 1475901:
                if (raw.equals("0.65")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 1475932:
                if (raw.equals("0.75")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 1475963:
                if (raw.equals("0.85")) {
                    c = 11;
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
                return CompatModePackages.DOWNSCALE_30;
            case 1:
                return CompatModePackages.DOWNSCALE_35;
            case 2:
                return CompatModePackages.DOWNSCALE_40;
            case 3:
                return CompatModePackages.DOWNSCALE_45;
            case 4:
                return CompatModePackages.DOWNSCALE_50;
            case 5:
                return CompatModePackages.DOWNSCALE_55;
            case 6:
                return CompatModePackages.DOWNSCALE_60;
            case 7:
                return CompatModePackages.DOWNSCALE_65;
            case '\b':
                return CompatModePackages.DOWNSCALE_70;
            case '\t':
                return CompatModePackages.DOWNSCALE_75;
            case '\n':
                return CompatModePackages.DOWNSCALE_80;
            case 11:
                return CompatModePackages.DOWNSCALE_85;
            case '\f':
                return CompatModePackages.DOWNSCALE_90;
            default:
                return 0L;
        }
    }

    /* loaded from: classes.dex */
    public enum FrameRate {
        FPS_DEFAULT(0),
        FPS_30(30),
        FPS_40(40),
        FPS_45(45),
        FPS_60(60),
        FPS_90(90),
        FPS_120(120),
        FPS_INVALID(-1);
        
        public final int fps;

        FrameRate(int fps) {
            this.fps = fps;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static int getFpsInt(String raw) {
        char c;
        switch (raw.hashCode()) {
            case 0:
                if (raw.equals("")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 1629:
                if (raw.equals("30")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1660:
                if (raw.equals("40")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1665:
                if (raw.equals("45")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1722:
                if (raw.equals("60")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1815:
                if (raw.equals("90")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 48687:
                if (raw.equals("120")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 1671308008:
                if (raw.equals("disable")) {
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
                return FrameRate.FPS_30.fps;
            case 1:
                return FrameRate.FPS_40.fps;
            case 2:
                return FrameRate.FPS_45.fps;
            case 3:
                return FrameRate.FPS_60.fps;
            case 4:
                return FrameRate.FPS_90.fps;
            case 5:
                return FrameRate.FPS_120.fps;
            case 6:
            case 7:
                return FrameRate.FPS_DEFAULT.fps;
            default:
                return FrameRate.FPS_INVALID.fps;
        }
    }

    public void setGameState(String packageName, GameState gameState, int userId) {
        if (!isPackageGame(packageName, userId)) {
            return;
        }
        Message msg = this.mHandler.obtainMessage(4);
        Bundle data = new Bundle();
        data.putString("packageName", packageName);
        data.putInt("userId", userId);
        msg.setData(data);
        msg.obj = gameState;
        this.mHandler.sendMessage(msg);
    }

    /* loaded from: classes.dex */
    public class GamePackageConfiguration {
        private static final String GAME_MODE_CONFIG_NODE_NAME = "game-mode-config";
        public static final String METADATA_ANGLE_ALLOW_ANGLE = "com.android.graphics.intervention.angle.allowAngle";
        public static final String METADATA_BATTERY_MODE_ENABLE = "com.android.app.gamemode.battery.enabled";
        public static final String METADATA_GAME_MODE_CONFIG = "android.game_mode_config";
        public static final String METADATA_PERFORMANCE_MODE_ENABLE = "com.android.app.gamemode.performance.enabled";
        public static final String METADATA_WM_ALLOW_DOWNSCALE = "com.android.graphics.intervention.wm.allowDownscale";
        public static final String TAG = "GameManagerService_GamePackageConfiguration";
        private boolean mAllowAngle;
        private boolean mAllowDownscale;
        private boolean mAllowFpsOverride;
        private boolean mBatteryModeOptedIn;
        private final ArrayMap<Integer, GameModeConfiguration> mModeConfigs = new ArrayMap<>();
        private final String mPackageName;
        private boolean mPerfModeOptedIn;

        GamePackageConfiguration(String packageName, int userId) {
            this.mPackageName = packageName;
            try {
                ApplicationInfo ai = GameManagerService.this.mPackageManager.getApplicationInfoAsUser(packageName, 128, userId);
                if (!parseInterventionFromXml(ai, packageName)) {
                    if (ai.metaData != null) {
                        this.mPerfModeOptedIn = ai.metaData.getBoolean(METADATA_PERFORMANCE_MODE_ENABLE);
                        this.mBatteryModeOptedIn = ai.metaData.getBoolean(METADATA_BATTERY_MODE_ENABLE);
                        this.mAllowDownscale = ai.metaData.getBoolean(METADATA_WM_ALLOW_DOWNSCALE, true);
                        this.mAllowAngle = ai.metaData.getBoolean(METADATA_ANGLE_ALLOW_ANGLE, true);
                    } else {
                        this.mPerfModeOptedIn = false;
                        this.mBatteryModeOptedIn = false;
                        this.mAllowDownscale = true;
                        this.mAllowAngle = true;
                        this.mAllowFpsOverride = true;
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                Slog.v(TAG, "Failed to get package metadata");
            }
            String configString = DeviceConfig.getProperty("game_overlay", packageName);
            if (configString != null) {
                String[] gameModeConfigStrings = configString.split(":");
                for (String gameModeConfigString : gameModeConfigStrings) {
                    try {
                        KeyValueListParser parser = new KeyValueListParser(',');
                        parser.setString(gameModeConfigString);
                        addModeConfig(new GameModeConfiguration(parser));
                    } catch (IllegalArgumentException e2) {
                        Slog.e(TAG, "Invalid config string");
                    }
                }
            }
        }

        private boolean parseInterventionFromXml(ApplicationInfo ai, String packageName) {
            boolean xmlFound = false;
            try {
                XmlResourceParser parser = ai.loadXmlMetaData(GameManagerService.this.mPackageManager, METADATA_GAME_MODE_CONFIG);
                if (parser == null) {
                    Slog.v(TAG, "No android.game_mode_config meta-data found for package " + this.mPackageName);
                } else {
                    xmlFound = true;
                    Resources resources = GameManagerService.this.mPackageManager.getResourcesForApplication(packageName);
                    AttributeSet attributeSet = Xml.asAttributeSet(parser);
                    while (true) {
                        int type = parser.next();
                        if (type == 1 || type == 2) {
                            break;
                        }
                    }
                    boolean isStartingTagGameModeConfig = GAME_MODE_CONFIG_NODE_NAME.equals(parser.getName());
                    if (!isStartingTagGameModeConfig) {
                        Slog.w(TAG, "Meta-data does not start with game-mode-config tag");
                    } else {
                        TypedArray array = resources.obtainAttributes(attributeSet, R.styleable.GameModeConfig);
                        this.mPerfModeOptedIn = array.getBoolean(1, false);
                        this.mBatteryModeOptedIn = array.getBoolean(0, false);
                        this.mAllowDownscale = array.getBoolean(3, true);
                        this.mAllowAngle = array.getBoolean(2, true);
                        this.mAllowFpsOverride = array.getBoolean(4, true);
                        array.recycle();
                    }
                }
                if (parser != null) {
                    parser.close();
                }
            } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e) {
                Slog.e(TAG, "Error while parsing XML meta-data for android.game_mode_config");
            }
            return xmlFound;
        }

        /* loaded from: classes.dex */
        public class GameModeConfiguration {
            public static final String ANGLE_KEY = "useAngle";
            public static final String DEFAULT_FPS = "";
            public static final String DEFAULT_SCALING = "1.0";
            public static final String FPS_KEY = "fps";
            public static final String LOADING_BOOST_KEY = "loadingBoost";
            public static final String MODE_KEY = "mode";
            public static final String SCALING_KEY = "downscaleFactor";
            public static final String TAG = "GameManagerService_GameModeConfiguration";
            private String mFps;
            private final int mGameMode;
            private final int mLoadingBoostDuration;
            private String mScaling;
            private final boolean mUseAngle;

            GameModeConfiguration(KeyValueListParser parser) {
                boolean z = false;
                int i = parser.getInt(MODE_KEY, 0);
                this.mGameMode = i;
                boolean z2 = GamePackageConfiguration.this.mAllowDownscale;
                String str = DEFAULT_SCALING;
                if (z2 && !GamePackageConfiguration.this.willGamePerformOptimizations(i)) {
                    str = parser.getString(SCALING_KEY, DEFAULT_SCALING);
                }
                this.mScaling = str;
                String str2 = "";
                if (GamePackageConfiguration.this.mAllowFpsOverride && !GamePackageConfiguration.this.willGamePerformOptimizations(i)) {
                    str2 = parser.getString(FPS_KEY, "");
                }
                this.mFps = str2;
                if (GamePackageConfiguration.this.mAllowAngle && !GamePackageConfiguration.this.willGamePerformOptimizations(i) && parser.getBoolean(ANGLE_KEY, false)) {
                    z = true;
                }
                this.mUseAngle = z;
                this.mLoadingBoostDuration = GamePackageConfiguration.this.willGamePerformOptimizations(i) ? -1 : parser.getInt(LOADING_BOOST_KEY, -1);
            }

            public int getGameMode() {
                return this.mGameMode;
            }

            public String getScaling() {
                return this.mScaling;
            }

            public int getFps() {
                return GameManagerService.getFpsInt(this.mFps);
            }

            public boolean getUseAngle() {
                return this.mUseAngle;
            }

            public int getLoadingBoostDuration() {
                return this.mLoadingBoostDuration;
            }

            public void setScaling(String scaling) {
                this.mScaling = scaling;
            }

            public void setFpsStr(String fpsStr) {
                this.mFps = fpsStr;
            }

            public boolean isValid() {
                int i = this.mGameMode;
                return (i == 1 || i == 2 || i == 3) && !GamePackageConfiguration.this.willGamePerformOptimizations(i);
            }

            public String toString() {
                return "[Game Mode:" + this.mGameMode + ",Scaling:" + this.mScaling + ",Use Angle:" + this.mUseAngle + ",Fps:" + this.mFps + ",Loading Boost Duration:" + this.mLoadingBoostDuration + "]";
            }

            public long getCompatChangeId() {
                return GameManagerService.getCompatChangeId(this.mScaling);
            }
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public boolean willGamePerformOptimizations(int gameMode) {
            return (this.mBatteryModeOptedIn && gameMode == 3) || (this.mPerfModeOptedIn && gameMode == 2);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getAvailableGameModesBitfield() {
            int field = 0;
            for (Integer num : this.mModeConfigs.keySet()) {
                int mode = num.intValue();
                field |= GameManagerService.this.modeToBitmask(mode);
            }
            if (this.mBatteryModeOptedIn) {
                field |= GameManagerService.this.modeToBitmask(3);
            }
            if (this.mPerfModeOptedIn) {
                field |= GameManagerService.this.modeToBitmask(2);
            }
            if (field > 1) {
                return field | GameManagerService.this.modeToBitmask(1);
            }
            return field | GameManagerService.this.modeToBitmask(0);
        }

        public int[] getAvailableGameModes() {
            int modesBitfield = getAvailableGameModesBitfield();
            int[] modes = new int[Integer.bitCount(modesBitfield)];
            int i = 0;
            int gameModeInHighestBit = Integer.numberOfTrailingZeros(Integer.highestOneBit(modesBitfield));
            for (int mode = 0; mode <= gameModeInHighestBit; mode++) {
                if (((modesBitfield >> mode) & 1) != 0) {
                    modes[i] = mode;
                    i++;
                }
            }
            return modes;
        }

        public GameModeConfiguration getGameModeConfiguration(int gameMode) {
            return this.mModeConfigs.get(Integer.valueOf(gameMode));
        }

        public void addModeConfig(GameModeConfiguration config) {
            if (config.isValid()) {
                this.mModeConfigs.put(Integer.valueOf(config.getGameMode()), config);
            } else {
                Slog.w(TAG, "Invalid game mode config for " + this.mPackageName + ":" + config.toString());
            }
        }

        public boolean isValid() {
            return this.mModeConfigs.size() > 0 || this.mBatteryModeOptedIn || this.mPerfModeOptedIn;
        }

        public String toString() {
            return "[Name:" + this.mPackageName + " Modes: " + this.mModeConfigs.toString() + "]";
        }
    }

    /* loaded from: classes.dex */
    public static class Lifecycle extends SystemService {
        private GameManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.server.app.GameManagerService$Lifecycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r1v0, types: [com.android.server.app.GameManagerService, android.os.IBinder] */
        @Override // com.android.server.SystemService
        public void onStart() {
            Context context = getContext();
            ?? gameManagerService = new GameManagerService(context);
            this.mService = gameManagerService;
            publishBinderService("game", gameManagerService);
            this.mService.registerDeviceConfigListener();
            this.mService.registerPackageReceiver();
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (phase == 1000) {
                this.mService.onBootCompleted();
            }
        }

        @Override // com.android.server.SystemService
        public void onUserStarting(SystemService.TargetUser user) {
            this.mService.onUserStarting(user);
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser user) {
            this.mService.onUserUnlocking(user);
        }

        @Override // com.android.server.SystemService
        public void onUserStopping(SystemService.TargetUser user) {
            this.mService.onUserStopping(user);
        }

        @Override // com.android.server.SystemService
        public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
            this.mService.onUserSwitching(from, to);
        }
    }

    private boolean isValidPackageName(String packageName, int userId) {
        try {
            return this.mPackageManager.getPackageUidAsUser(packageName, userId) == Binder.getCallingUid();
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void checkPermission(String permission) throws SecurityException {
        if (this.mContext.checkCallingOrSelfPermission(permission) != 0) {
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid() + ", must have permission " + permission);
        }
    }

    private int[] getAvailableGameModesUnchecked(String packageName) {
        GamePackageConfiguration config;
        synchronized (this.mOverrideConfigLock) {
            config = this.mOverrideConfigs.get(packageName);
        }
        if (config == null) {
            synchronized (this.mDeviceConfigLock) {
                config = this.mConfigs.get(packageName);
            }
        }
        if (config == null) {
            return new int[0];
        }
        return config.getAvailableGameModes();
    }

    private boolean isPackageGame(String packageName, int userId) {
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfoAsUser(packageName, 131072, userId);
            return applicationInfo.category == 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public int[] getAvailableGameModes(String packageName) throws SecurityException {
        checkPermission("android.permission.MANAGE_GAME_MODE");
        return getAvailableGameModesUnchecked(packageName);
    }

    private int getGameModeFromSettings(String packageName, int userId) {
        synchronized (this.mLock) {
            if (!this.mSettings.containsKey(Integer.valueOf(userId))) {
                Slog.w(TAG, "User ID '" + userId + "' does not have a Game Mode selected for package: '" + packageName + "'");
                return 0;
            }
            return this.mSettings.get(Integer.valueOf(userId)).getGameModeLocked(packageName);
        }
    }

    public int getGameMode(String packageName, int userId) throws SecurityException {
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "getGameMode", "com.android.server.app.GameManagerService");
        if (!isPackageGame(packageName, userId2)) {
            return 0;
        }
        if (isValidPackageName(packageName, userId2)) {
            return getGameModeFromSettings(packageName, userId2);
        }
        checkPermission("android.permission.MANAGE_GAME_MODE");
        return getGameModeFromSettings(packageName, userId2);
    }

    public GameModeInfo getGameModeInfo(String packageName, int userId) {
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "getGameModeInfo", "com.android.server.app.GameManagerService");
        checkPermission("android.permission.MANAGE_GAME_MODE");
        if (!isPackageGame(packageName, userId2)) {
            return null;
        }
        int activeGameMode = getGameModeFromSettings(packageName, userId2);
        int[] availableGameModes = getAvailableGameModesUnchecked(packageName);
        return new GameModeInfo(activeGameMode, availableGameModes);
    }

    public void setGameMode(String packageName, int gameMode, int userId) throws SecurityException {
        checkPermission("android.permission.MANAGE_GAME_MODE");
        if (!isPackageGame(packageName, userId)) {
            return;
        }
        synchronized (this.mLock) {
            int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "setGameMode", "com.android.server.app.GameManagerService");
            if (this.mSettings.containsKey(Integer.valueOf(userId2))) {
                GameManagerSettings userSettings = this.mSettings.get(Integer.valueOf(userId2));
                userSettings.setGameModeLocked(packageName, gameMode);
                Message msg = this.mHandler.obtainMessage(1);
                msg.obj = Integer.valueOf(userId2);
                if (!this.mHandler.hasEqualMessages(1, Integer.valueOf(userId2))) {
                    this.mHandler.sendMessageDelayed(msg, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                }
                updateInterventions(packageName, gameMode, userId2);
            }
        }
    }

    public boolean isAngleEnabled(String packageName, int userId) throws SecurityException {
        int gameMode = getGameMode(packageName, userId);
        if (gameMode == 0) {
            return false;
        }
        synchronized (this.mDeviceConfigLock) {
            GamePackageConfiguration config = this.mConfigs.get(packageName);
            if (config == null) {
                return false;
            }
            GamePackageConfiguration.GameModeConfiguration gameModeConfiguration = config.getGameModeConfiguration(gameMode);
            if (gameModeConfiguration == null) {
                return false;
            }
            return gameModeConfiguration.getUseAngle();
        }
    }

    public int getLoadingBoostDuration(String packageName, int userId) throws SecurityException {
        int gameMode = getGameMode(packageName, userId);
        if (gameMode == 0) {
            return -1;
        }
        synchronized (this.mDeviceConfigLock) {
            GamePackageConfiguration config = this.mConfigs.get(packageName);
            if (config == null) {
                return -1;
            }
            GamePackageConfiguration.GameModeConfiguration gameModeConfiguration = config.getGameModeConfiguration(gameMode);
            if (gameModeConfiguration == null) {
                return -1;
            }
            return gameModeConfiguration.getLoadingBoostDuration();
        }
    }

    public void notifyGraphicsEnvironmentSetup(String packageName, int userId) throws SecurityException {
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "notifyGraphicsEnvironmentSetup", "com.android.server.app.GameManagerService");
        if (!isPackageGame(packageName, userId2) || !isValidPackageName(packageName, userId2)) {
            return;
        }
        int gameMode = getGameMode(packageName, userId2);
        if (gameMode != 0 && (loadingBoostDuration = getLoadingBoostDuration(packageName, userId2)) != -1) {
            int loadingBoostDuration = (loadingBoostDuration == 0 || loadingBoostDuration > 5000) ? 5000 : 5000;
            if (this.mHandler.hasMessages(5)) {
                this.mHandler.removeMessages(5);
            } else {
                this.mPowerManagerInternal.setPowerMode(16, true);
            }
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(5), loadingBoostDuration);
        }
    }

    public void setGameServiceProvider(String packageName) throws SecurityException {
        checkPermission("android.permission.SET_GAME_SERVICE");
        GameServiceController gameServiceController = this.mGameServiceController;
        if (gameServiceController == null) {
            return;
        }
        gameServiceController.setGameServiceProvider(packageName);
    }

    void onBootCompleted() {
        Slog.d(TAG, "onBootCompleted");
        GameServiceController gameServiceController = this.mGameServiceController;
        if (gameServiceController != null) {
            gameServiceController.onBootComplete();
        }
    }

    void onUserStarting(SystemService.TargetUser user) {
        int userId = user.getUserIdentifier();
        synchronized (this.mLock) {
            if (!this.mSettings.containsKey(Integer.valueOf(userId))) {
                GameManagerSettings userSettings = new GameManagerSettings(Environment.getDataSystemDeDirectory(userId));
                this.mSettings.put(Integer.valueOf(userId), userSettings);
                userSettings.readPersistentDataLocked();
            }
        }
        Message msg = this.mHandler.obtainMessage(3);
        msg.obj = Integer.valueOf(userId);
        this.mHandler.sendMessage(msg);
        GameServiceController gameServiceController = this.mGameServiceController;
        if (gameServiceController != null) {
            gameServiceController.notifyUserStarted(user);
        }
    }

    void onUserUnlocking(SystemService.TargetUser user) {
        GameServiceController gameServiceController = this.mGameServiceController;
        if (gameServiceController != null) {
            gameServiceController.notifyUserUnlocking(user);
        }
    }

    void onUserStopping(SystemService.TargetUser user) {
        int userId = user.getUserIdentifier();
        synchronized (this.mLock) {
            if (this.mSettings.containsKey(Integer.valueOf(userId))) {
                Message msg = this.mHandler.obtainMessage(2);
                msg.obj = Integer.valueOf(userId);
                this.mHandler.sendMessage(msg);
                GameServiceController gameServiceController = this.mGameServiceController;
                if (gameServiceController != null) {
                    gameServiceController.notifyUserStopped(user);
                }
            }
        }
    }

    void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        int toUserId = to.getUserIdentifier();
        if (from != null) {
            synchronized (this.mLock) {
                int fromUserId = from.getUserIdentifier();
                if (this.mSettings.containsKey(Integer.valueOf(fromUserId))) {
                    Message msg = this.mHandler.obtainMessage(2);
                    msg.obj = Integer.valueOf(fromUserId);
                    this.mHandler.sendMessage(msg);
                }
            }
        }
        Message msg2 = this.mHandler.obtainMessage(3);
        msg2.obj = Integer.valueOf(toUserId);
        this.mHandler.sendMessage(msg2);
        GameServiceController gameServiceController = this.mGameServiceController;
        if (gameServiceController != null) {
            gameServiceController.notifyNewForegroundUser(to);
        }
    }

    public void disableCompatScale(String packageName) {
        long uid = Binder.clearCallingIdentity();
        try {
            Slog.i(TAG, "Disabling downscale for " + packageName);
            ArrayMap<Long, PackageOverride> overrides = new ArrayMap<>();
            overrides.put(Long.valueOf((long) CompatModePackages.DOWNSCALED), COMPAT_DISABLED);
            CompatibilityOverrideConfig changeConfig = new CompatibilityOverrideConfig(overrides);
            try {
                this.mPlatformCompat.putOverridesOnReleaseBuilds(changeConfig, packageName);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to call IPlatformCompat#putOverridesOnReleaseBuilds", e);
            }
        } finally {
            Binder.restoreCallingIdentity(uid);
        }
    }

    private void resetFps(String packageName, int userId) {
        try {
            int uid = this.mPackageManager.getPackageUidAsUser(packageName, userId);
            nativeSetOverrideFrameRate(uid, 0.0f);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    private void enableCompatScale(String packageName, long scaleId) {
        long uid = Binder.clearCallingIdentity();
        try {
            Slog.i(TAG, "Enabling downscale: " + scaleId + " for " + packageName);
            ArrayMap<Long, PackageOverride> overrides = new ArrayMap<>();
            Long valueOf = Long.valueOf((long) CompatModePackages.DOWNSCALED);
            PackageOverride packageOverride = COMPAT_ENABLED;
            overrides.put(valueOf, packageOverride);
            Long valueOf2 = Long.valueOf((long) CompatModePackages.DOWNSCALE_30);
            PackageOverride packageOverride2 = COMPAT_DISABLED;
            overrides.put(valueOf2, packageOverride2);
            overrides.put(Long.valueOf((long) CompatModePackages.DOWNSCALE_35), packageOverride2);
            overrides.put(Long.valueOf((long) CompatModePackages.DOWNSCALE_40), packageOverride2);
            overrides.put(Long.valueOf((long) CompatModePackages.DOWNSCALE_45), packageOverride2);
            overrides.put(Long.valueOf((long) CompatModePackages.DOWNSCALE_50), packageOverride2);
            overrides.put(Long.valueOf((long) CompatModePackages.DOWNSCALE_55), packageOverride2);
            overrides.put(Long.valueOf((long) CompatModePackages.DOWNSCALE_60), packageOverride2);
            overrides.put(Long.valueOf((long) CompatModePackages.DOWNSCALE_65), packageOverride2);
            overrides.put(Long.valueOf((long) CompatModePackages.DOWNSCALE_70), packageOverride2);
            overrides.put(Long.valueOf((long) CompatModePackages.DOWNSCALE_75), packageOverride2);
            overrides.put(Long.valueOf((long) CompatModePackages.DOWNSCALE_80), packageOverride2);
            overrides.put(Long.valueOf((long) CompatModePackages.DOWNSCALE_85), packageOverride2);
            overrides.put(Long.valueOf((long) CompatModePackages.DOWNSCALE_90), packageOverride2);
            overrides.put(Long.valueOf(scaleId), packageOverride);
            CompatibilityOverrideConfig changeConfig = new CompatibilityOverrideConfig(overrides);
            try {
                this.mPlatformCompat.putOverridesOnReleaseBuilds(changeConfig, packageName);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to call IPlatformCompat#putOverridesOnReleaseBuilds", e);
            }
        } finally {
            Binder.restoreCallingIdentity(uid);
        }
    }

    private void updateCompatModeDownscale(GamePackageConfiguration packageConfig, String packageName, int gameMode) {
        GamePackageConfiguration.GameModeConfiguration modeConfig = packageConfig.getGameModeConfiguration(gameMode);
        if (modeConfig == null) {
            Slog.i(TAG, "Game mode " + gameMode + " not found for " + packageName);
            return;
        }
        long scaleId = modeConfig.getCompatChangeId();
        if (scaleId == 0) {
            Slog.w(TAG, "Invalid downscaling change id " + scaleId + " for " + packageName);
        } else {
            enableCompatScale(packageName, scaleId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int modeToBitmask(int gameMode) {
        return 1 << gameMode;
    }

    private boolean bitFieldContainsModeBitmask(int bitField, int gameMode) {
        return (modeToBitmask(gameMode) & bitField) != 0;
    }

    private void updateUseAngle(String packageName, int gameMode) {
    }

    private void updateFps(GamePackageConfiguration packageConfig, String packageName, int gameMode, int userId) {
        GamePackageConfiguration.GameModeConfiguration modeConfig = packageConfig.getGameModeConfiguration(gameMode);
        if (modeConfig == null) {
            Slog.d(TAG, "Game mode " + gameMode + " not found for " + packageName);
            return;
        }
        try {
            float fps = modeConfig.getFps();
            int uid = this.mPackageManager.getPackageUidAsUser(packageName, userId);
            nativeSetOverrideFrameRate(uid, fps);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    private void updateInterventions(String packageName, int gameMode, int userId) {
        GamePackageConfiguration packageConfig;
        if (gameMode == 1 || gameMode == 0) {
            disableCompatScale(packageName);
            resetFps(packageName, userId);
            return;
        }
        synchronized (this.mOverrideConfigLock) {
            packageConfig = this.mOverrideConfigs.get(packageName);
        }
        if (packageConfig == null) {
            synchronized (this.mDeviceConfigLock) {
                packageConfig = this.mConfigs.get(packageName);
            }
        }
        if (packageConfig == null) {
            disableCompatScale(packageName);
            Slog.v(TAG, "Package configuration not found for " + packageName);
        } else if (packageConfig.willGamePerformOptimizations(gameMode)) {
        } else {
            updateCompatModeDownscale(packageConfig, packageName, gameMode);
            updateFps(packageConfig, packageName, gameMode, userId);
            updateUseAngle(packageName, gameMode);
        }
    }

    public void setGameModeConfigOverride(String packageName, int userId, int gameMode, String fpsStr, String scaling) throws SecurityException {
        checkPermission("android.permission.MANAGE_GAME_MODE");
        synchronized (this.mLock) {
            if (this.mSettings.containsKey(Integer.valueOf(userId))) {
                synchronized (this.mOverrideConfigLock) {
                    GamePackageConfiguration overrideConfig = this.mOverrideConfigs.get(packageName);
                    if (overrideConfig == null) {
                        overrideConfig = new GamePackageConfiguration(packageName, userId);
                        this.mOverrideConfigs.put(packageName, overrideConfig);
                    }
                    GamePackageConfiguration.GameModeConfiguration overrideModeConfig = overrideConfig.getGameModeConfiguration(gameMode);
                    if (fpsStr != null) {
                        overrideModeConfig.setFpsStr(fpsStr);
                    } else {
                        overrideModeConfig.setFpsStr("");
                    }
                    if (scaling != null) {
                        overrideModeConfig.setScaling(scaling);
                    } else {
                        overrideModeConfig.setScaling(GamePackageConfiguration.GameModeConfiguration.DEFAULT_SCALING);
                    }
                    Slog.i(TAG, "Package Name: " + packageName + " FPS: " + String.valueOf(overrideModeConfig.getFps()) + " Scaling: " + overrideModeConfig.getScaling());
                }
                setGameMode(packageName, gameMode, userId);
            }
        }
    }

    public void resetGameModeConfigOverride(String packageName, int userId, int gameModeToReset) throws SecurityException {
        GamePackageConfiguration config;
        GamePackageConfiguration overrideConfig;
        GamePackageConfiguration config2;
        checkPermission("android.permission.MANAGE_GAME_MODE");
        synchronized (this.mLock) {
            if (this.mSettings.containsKey(Integer.valueOf(userId))) {
                if (gameModeToReset != -1) {
                    synchronized (this.mOverrideConfigLock) {
                        overrideConfig = this.mOverrideConfigs.get(packageName);
                    }
                    synchronized (this.mDeviceConfigLock) {
                        config2 = this.mConfigs.get(packageName);
                    }
                    int[] modes = overrideConfig.getAvailableGameModes();
                    boolean isGameModeExist = false;
                    for (int mode : modes) {
                        if (gameModeToReset == mode) {
                            isGameModeExist = true;
                        }
                    }
                    if (!isGameModeExist) {
                        return;
                    }
                    if (modes.length <= 2) {
                        synchronized (this.mOverrideConfigLock) {
                            this.mOverrideConfigs.remove(packageName);
                        }
                    } else {
                        overrideConfig.addModeConfig(config2.getGameModeConfiguration(gameModeToReset));
                    }
                } else {
                    synchronized (this.mOverrideConfigLock) {
                        this.mOverrideConfigs.remove(packageName);
                    }
                }
                int gameMode = getGameMode(packageName, userId);
                synchronized (this.mOverrideConfigLock) {
                    config = this.mOverrideConfigs.get(packageName);
                }
                if (config == null) {
                    synchronized (this.mDeviceConfigLock) {
                        config = this.mConfigs.get(packageName);
                    }
                }
                int newGameMode = getNewGameMode(gameMode, config);
                if (gameMode != newGameMode) {
                    setGameMode(packageName, 1, userId);
                } else {
                    setGameMode(packageName, gameMode, userId);
                }
            }
        }
    }

    private int getNewGameMode(int gameMode, GamePackageConfiguration config) {
        if (config != null) {
            int modesBitfield = config.getAvailableGameModesBitfield() & (~modeToBitmask(0));
            if (bitFieldContainsModeBitmask(modesBitfield, gameMode)) {
                return gameMode;
            }
            if (bitFieldContainsModeBitmask(modesBitfield, 1)) {
                return 1;
            }
            return 0;
        } else if (gameMode == 0) {
            return gameMode;
        } else {
            return 0;
        }
    }

    public String getInterventionList(String packageName) {
        GamePackageConfiguration packageConfig;
        synchronized (this.mOverrideConfigLock) {
            packageConfig = this.mOverrideConfigs.get(packageName);
        }
        if (packageConfig == null) {
            synchronized (this.mDeviceConfigLock) {
                packageConfig = this.mConfigs.get(packageName);
            }
        }
        StringBuilder listStrSb = new StringBuilder();
        if (packageConfig == null) {
            listStrSb.append("\n No intervention found for package ").append(packageName);
            return listStrSb.toString();
        }
        listStrSb.append("\n").append(packageConfig.toString());
        return listStrSb.toString();
    }

    void updateConfigsForUser(int userId, String... packageNames) {
        int i;
        GamePackageConfiguration config;
        try {
            synchronized (this.mDeviceConfigLock) {
                for (String packageName : packageNames) {
                    GamePackageConfiguration config2 = new GamePackageConfiguration(packageName, userId);
                    if (config2.isValid()) {
                        this.mConfigs.put(packageName, config2);
                    } else {
                        Slog.w(TAG, "Invalid package config for " + config2.getPackageName() + ":" + config2.toString());
                        this.mConfigs.remove(packageName);
                    }
                }
            }
            synchronized (this.mLock) {
                if (this.mSettings.containsKey(Integer.valueOf(userId))) {
                    for (String packageName2 : packageNames) {
                        int gameMode = getGameMode(packageName2, userId);
                        synchronized (this.mDeviceConfigLock) {
                            config = this.mConfigs.get(packageName2);
                        }
                        int newGameMode = getNewGameMode(gameMode, config);
                        if (newGameMode != gameMode) {
                            setGameMode(packageName2, newGameMode, userId);
                        }
                        updateInterventions(packageName2, gameMode, userId);
                    }
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to update compat modes for user " + userId + ": " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String[] getInstalledGamePackageNames(int userId) {
        List<PackageInfo> packages = this.mPackageManager.getInstalledPackagesAsUser(0, userId);
        return (String[]) packages.stream().filter(new Predicate() { // from class: com.android.server.app.GameManagerService$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return GameManagerService.lambda$getInstalledGamePackageNames$0((PackageInfo) obj);
            }
        }).map(new Function() { // from class: com.android.server.app.GameManagerService$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                String str;
                str = ((PackageInfo) obj).packageName;
                return str;
            }
        }).toArray(new IntFunction() { // from class: com.android.server.app.GameManagerService$$ExternalSyntheticLambda2
            @Override // java.util.function.IntFunction
            public final Object apply(int i) {
                return GameManagerService.lambda$getInstalledGamePackageNames$2(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getInstalledGamePackageNames$0(PackageInfo e) {
        return e.applicationInfo != null && e.applicationInfo.category == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String[] lambda$getInstalledGamePackageNames$2(int x$0) {
        return new String[x$0];
    }

    public GamePackageConfiguration getConfig(String packageName) {
        GamePackageConfiguration packageConfig;
        synchronized (this.mOverrideConfigLock) {
            packageConfig = this.mOverrideConfigs.get(packageName);
        }
        if (packageConfig == null) {
            synchronized (this.mDeviceConfigLock) {
                packageConfig = this.mConfigs.get(packageName);
            }
        }
        return packageConfig;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerPackageReceiver() {
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addDataScheme("package");
        BroadcastReceiver packageReceiver = new BroadcastReceiver() { // from class: com.android.server.app.GameManagerService.1
            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                Uri data = intent.getData();
                try {
                    int userId = getSendingUserId();
                    if (userId != ActivityManager.getCurrentUser()) {
                        return;
                    }
                    String packageName = data.getSchemeSpecificPart();
                    try {
                        ApplicationInfo applicationInfo = GameManagerService.this.mPackageManager.getApplicationInfoAsUser(packageName, 131072, userId);
                        if (applicationInfo.category != 0) {
                            return;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                    String action = intent.getAction();
                    char c = 65535;
                    switch (action.hashCode()) {
                        case 525384130:
                            if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                                c = 1;
                                break;
                            }
                            break;
                        case 1544582882:
                            if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                                c = 0;
                                break;
                            }
                            break;
                    }
                    switch (c) {
                        case 0:
                            GameManagerService.this.updateConfigsForUser(userId, packageName);
                            return;
                        case 1:
                            GameManagerService.this.disableCompatScale(packageName);
                            if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                                synchronized (GameManagerService.this.mOverrideConfigLock) {
                                    GameManagerService.this.mOverrideConfigs.remove(packageName);
                                }
                                synchronized (GameManagerService.this.mDeviceConfigLock) {
                                    GameManagerService.this.mConfigs.remove(packageName);
                                }
                                synchronized (GameManagerService.this.mLock) {
                                    if (GameManagerService.this.mSettings.containsKey(Integer.valueOf(userId))) {
                                        ((GameManagerSettings) GameManagerService.this.mSettings.get(Integer.valueOf(userId))).removeGame(packageName);
                                    }
                                }
                                return;
                            }
                            return;
                        default:
                            return;
                    }
                } catch (NullPointerException e2) {
                    Slog.e(GameManagerService.TAG, "Failed to get package name for new package");
                }
            }
        };
        this.mContext.registerReceiverForAllUsers(packageReceiver, packageFilter, null, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerDeviceConfigListener() {
        this.mDeviceConfigListener = new DeviceConfigListener();
    }

    private String dumpDeviceConfigs() {
        StringBuilder out = new StringBuilder();
        for (String key : this.mConfigs.keySet()) {
            out.append("[\nName: ").append(key).append("\nConfig: ").append(this.mConfigs.get(key).toString()).append("\n]");
        }
        return out.toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int gameStateModeToStatsdGameState(int mode) {
        switch (mode) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            default:
                return 0;
        }
    }

    private static ServiceThread createServiceThread() {
        ServiceThread handlerThread = new ServiceThread(TAG, 10, true);
        handlerThread.start();
        return handlerThread;
    }

    public void setOverrideFrameRate(String packageName, float fps, int userId) {
        try {
            int uid = this.mPackageManager.getPackageUidAsUser(packageName, userId);
            nativeSetOverrideFrameRate(uid, fps);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }
}
