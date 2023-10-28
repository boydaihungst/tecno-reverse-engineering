package com.mediatek.server.display;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Slog;
import android.widget.Toast;
import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;
import com.android.server.display.WifiDisplayController;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.powerhal.PowerHalManager;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
/* loaded from: classes2.dex */
public class MtkWifiDisplayController {
    private static final int CONNECTION_TIMEOUT_SECONDS = 30;
    private static final int RECONNECT_RETRY_DELAY_MILLIS = 1000;
    private static final String TAG = "MtkWifiDisplayController";
    private static final long WIFI_SCAN_TIMER = 100000;
    private int WFDCONTROLLER_DISPLAY_POWER_SAVING_DELAY;
    private int WFDCONTROLLER_DISPLAY_POWER_SAVING_OPTION;
    private AlarmManager mAlarmManager;
    private final Context mContext;
    private WifiDisplayController mController;
    private final Handler mHandler;
    private WifiP2pDevice mReConnectDevice;
    private int mReConnection_Timeout_Remain_Seconds;
    private final BroadcastReceiver mWifiReceiver;
    private static boolean DEBUG = true;
    private static final String goIntent = SystemProperties.get("wfd.source.go_intent", String.valueOf(14));
    private PowerHalManager mPowerHalManager = MtkSystemServiceFactory.getInstance().makePowerHalManager();
    private PathClassLoader mWifiPathClassLoader = null;
    private final Runnable mReConnect = new Runnable() { // from class: com.mediatek.server.display.MtkWifiDisplayController.1
        @Override // java.lang.Runnable
        public void run() {
            Iterator<WifiP2pDevice> it = MtkWifiDisplayController.this.mController.mAvailableWifiDisplayPeers.iterator();
            while (it.hasNext()) {
                WifiP2pDevice device = it.next();
                if (MtkWifiDisplayController.DEBUG) {
                    Slog.d(MtkWifiDisplayController.TAG, "\t" + MtkWifiDisplayController.describeWifiP2pDevice(device));
                }
                if (device.deviceAddress.equals(MtkWifiDisplayController.this.mReConnectDevice.deviceAddress)) {
                    Slog.i(MtkWifiDisplayController.TAG, "connect() in mReConnect. Set mReConnecting as true");
                    MtkWifiDisplayController.this.mReConnectDevice = null;
                    MtkWifiDisplayController.this.mController.requestConnect(device.deviceAddress);
                    return;
                }
            }
            MtkWifiDisplayController mtkWifiDisplayController = MtkWifiDisplayController.this;
            mtkWifiDisplayController.mReConnection_Timeout_Remain_Seconds--;
            if (MtkWifiDisplayController.this.mReConnection_Timeout_Remain_Seconds > 0) {
                Slog.i(MtkWifiDisplayController.TAG, "post mReconnect, s:" + MtkWifiDisplayController.this.mReConnection_Timeout_Remain_Seconds);
                MtkWifiDisplayController.this.mHandler.postDelayed(MtkWifiDisplayController.this.mReConnect, 1000L);
                return;
            }
            Slog.e(MtkWifiDisplayController.TAG, "reconnect timeout!");
            Toast.makeText(MtkWifiDisplayController.this.mContext, MtkWifiDisplayController.this.getMtkStringResourceId("wifi_display_disconnected"), 0).show();
            MtkWifiDisplayController.this.mReConnectDevice = null;
            MtkWifiDisplayController.this.mReConnection_Timeout_Remain_Seconds = 0;
            MtkWifiDisplayController.this.mHandler.removeCallbacks(MtkWifiDisplayController.this.mReConnect);
        }
    };
    private final AlarmManager.OnAlarmListener mWifiScanTimerListener = new AlarmManager.OnAlarmListener() { // from class: com.mediatek.server.display.MtkWifiDisplayController.2
        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            Slog.i(MtkWifiDisplayController.TAG, "Stop WiFi scan/reconnect due to scan timer timeout");
            MtkWifiDisplayController.this.stopWifiScan(true);
        }
    };
    public boolean mStopWifiScan = false;

    public MtkWifiDisplayController(Context context, Handler handler, WifiDisplayController controller) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.mediatek.server.display.MtkWifiDisplayController.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (MtkWifiDisplayController.this.mController.mConnectedDevice != null) {
                        NetworkInfo.State state = info.getState();
                        if (state == NetworkInfo.State.DISCONNECTED && MtkWifiDisplayController.this.mStopWifiScan) {
                            Slog.i(MtkWifiDisplayController.TAG, "Resume WiFi scan/reconnect if WiFi is disconnected");
                            MtkWifiDisplayController.this.stopWifiScan(false);
                            MtkWifiDisplayController.this.mAlarmManager.cancel(MtkWifiDisplayController.this.mWifiScanTimerListener);
                            MtkWifiDisplayController.this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + MtkWifiDisplayController.WIFI_SCAN_TIMER, "Set WiFi scan timer", MtkWifiDisplayController.this.mWifiScanTimerListener, MtkWifiDisplayController.this.mHandler);
                        } else if (state == NetworkInfo.State.CONNECTED && !MtkWifiDisplayController.this.mStopWifiScan) {
                            Slog.i(MtkWifiDisplayController.TAG, "Stop WiFi scan/reconnect if WiFi is connected");
                            MtkWifiDisplayController.this.mAlarmManager.cancel(MtkWifiDisplayController.this.mWifiScanTimerListener);
                            MtkWifiDisplayController.this.stopWifiScan(true);
                        }
                    }
                }
            }
        };
        this.mWifiReceiver = broadcastReceiver;
        this.mContext = context;
        this.mHandler = handler;
        this.mController = controller;
        registerEMObserver();
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        context.registerReceiver(broadcastReceiver, intentFilter, null, handler);
        getWifiPathClassLoader();
    }

    private void getWifiPathClassLoader() {
        try {
            Class ssmClz = Class.forName(SystemServiceManager.class.getName());
            Field loadedPathsField = ssmClz.getDeclaredField("mLoadedPaths");
            loadedPathsField.setAccessible(true);
            SystemServiceManager ssm = (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
            ArrayMap<String, PathClassLoader> loadedPaths = (ArrayMap) loadedPathsField.get(ssm);
            PathClassLoader pathClassLoader = loadedPaths.get("/apex/com.android.wifi/javalib/service-wifi.jar");
            this.mWifiPathClassLoader = pathClassLoader;
            if (pathClassLoader == null) {
                Slog.e(TAG, "mWifiPathClassLoader is null!");
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public WifiP2pConfig overWriteConfig(WifiP2pConfig oldConfig) {
        WifiP2pConfig config = new WifiP2pConfig(oldConfig);
        Slog.i(TAG, "oldConfig:" + oldConfig);
        config.groupOwnerIntent = Integer.valueOf(goIntent).intValue();
        Slog.i(TAG, "config:" + config);
        stopWifiScan(true);
        return config;
    }

    public void setWFD(boolean enable) {
        Slog.d(TAG, "setWFD(), enable: " + enable);
        this.mPowerHalManager.setWFD(enable);
        stopWifiScan(enable);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String describeWifiP2pDevice(WifiP2pDevice device) {
        return device != null ? device.toString().replace('\n', ',') : "null";
    }

    public void checkReConnect() {
        if (this.mReConnectDevice != null) {
            Slog.i(TAG, "requestStartScan() for resolution change.");
            this.mController.requestStartScan();
            this.mReConnection_Timeout_Remain_Seconds = 30;
            this.mHandler.postDelayed(this.mReConnect, 1000L);
        }
    }

    private void registerEMObserver() {
        this.WFDCONTROLLER_DISPLAY_POWER_SAVING_OPTION = this.mContext.getResources().getInteger(getMtkIntegerResourceId("wfd_display_power_saving_option", 1));
        this.WFDCONTROLLER_DISPLAY_POWER_SAVING_DELAY = this.mContext.getResources().getInteger(getMtkIntegerResourceId("wfd_display_power_saving_delay", 10));
        Slog.d(TAG, "registerObserver() ps:" + this.WFDCONTROLLER_DISPLAY_POWER_SAVING_OPTION + ",psd:" + this.WFDCONTROLLER_DISPLAY_POWER_SAVING_DELAY);
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_POWER_SAVING_OPTION"), this.WFDCONTROLLER_DISPLAY_POWER_SAVING_OPTION);
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_POWER_SAVING_DELAY"), this.WFDCONTROLLER_DISPLAY_POWER_SAVING_DELAY);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getMtkStringResourceId(String name) {
        try {
            Class<?> rCls = Class.forName("com.mediatek.internal.R$string", false, ClassLoader.getSystemClassLoader());
            Field field = rCls.getField(name);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (Exception e) {
            Slog.e(TAG, "Cannot get MTK resource - " + e);
            return 0;
        }
    }

    private String getMtkSettingsExtGlobalSetting(String name) {
        try {
            Class<?> rCls = Class.forName("com.mediatek.provider.MtkSettingsExt$Global", false, ClassLoader.getSystemClassLoader());
            Field field = rCls.getField(name);
            field.setAccessible(true);
            return (String) field.get(rCls);
        } catch (Exception e) {
            Slog.e(TAG, "Cannot get MTK settings - " + e);
            return "";
        }
    }

    private int getMtkIntegerResourceId(String name, int defaultVal) {
        try {
            Class<?> rCls = Class.forName("com.mediatek.internal.R$integer", false, ClassLoader.getSystemClassLoader());
            Field field = rCls.getField(name);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (Exception e) {
            Slog.e(TAG, "Cannot get MTK resource - " + e);
            return defaultVal;
        }
    }

    public void stopWifiScan(final boolean ifStop) {
        if (!ifStop) {
            this.mAlarmManager.cancel(this.mWifiScanTimerListener);
        }
        if (this.mStopWifiScan != ifStop) {
            Slog.i(TAG, "stopWifiScan()," + ifStop);
            try {
                WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
                wifiManager.allowAutojoinGlobal(!ifStop);
                Class wifiInjectorClz = Class.forName("com.android.server.wifi.WifiInjector", false, this.mWifiPathClassLoader);
                Method instanceMethod = wifiInjectorClz.getMethod("getInstance", new Class[0]);
                Object wifiInjector = instanceMethod.invoke(null, new Object[0]);
                Method runnerMethod = wifiInjectorClz.getMethod("getWifiThreadRunner", new Class[0]);
                Object runner = runnerMethod.invoke(wifiInjector, new Object[0]);
                Field handlerField = runner.getClass().getDeclaredField("mHandler");
                handlerField.setAccessible(true);
                Handler wifiHandler = (Handler) handlerField.get(runner);
                if (!ifStop && wifiHandler != null) {
                    Method wcmMethod = wifiInjectorClz.getMethod("getWifiConnectivityManager", new Class[0]);
                    final Object wcm = wcmMethod.invoke(wifiInjector, new Object[0]);
                    final Method scanMethod = wcm.getClass().getDeclaredMethod("startPeriodicScan", Boolean.TYPE);
                    scanMethod.setAccessible(true);
                    wifiHandler.post(new Runnable() { // from class: com.mediatek.server.display.MtkWifiDisplayController$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            MtkWifiDisplayController.lambda$stopWifiScan$0(scanMethod, wcm, ifStop);
                        }
                    });
                }
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
            this.mStopWifiScan = ifStop;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$stopWifiScan$0(Method scanMethod, Object wcm, boolean ifStop) {
        boolean z = true;
        try {
            Object[] objArr = new Object[1];
            if (ifStop) {
                z = false;
            }
            objArr[0] = Boolean.valueOf(z);
            scanMethod.invoke(wcm, objArr);
            Slog.i(TAG, "reflection to startPeriodicScan");
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}
