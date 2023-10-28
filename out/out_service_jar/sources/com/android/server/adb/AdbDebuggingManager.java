package com.android.server.adb;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.debug.AdbNotifications;
import android.debug.PairDevice;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.AtomicFile;
import android.util.Base64;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.dump.DumpUtils;
import com.android.server.FgThread;
import com.android.server.NVUtils;
import com.android.server.usage.UnixCalendar;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class AdbDebuggingManager {
    private static final String ADBD_SOCKET = "adbd";
    private static final String ADB_DIRECTORY = "misc/adb";
    private static final String ADB_KEYS_FILE = "adb_keys";
    private static final String ADB_TEMP_KEYS_FILE = "adb_temp_keys.xml";
    private static final int BUFFER_SIZE = 65536;
    private static final boolean DEBUG = false;
    private static final boolean MDNS_DEBUG = false;
    private static final int PAIRING_CODE_LENGTH = 6;
    private static final String WIFI_PERSISTENT_CONFIG_PROPERTY = "persist.adb.tls_server.enable";
    private static final String WIFI_PERSISTENT_GUID = "persist.adb.wifi.guid";
    private AdbConnectionInfo mAdbConnectionInfo;
    private boolean mAdbUsbEnabled;
    private boolean mAdbWifiEnabled;
    private final String mConfirmComponent;
    private final Map<String, Integer> mConnectedKeys;
    private AdbConnectionPortPoller mConnectionPortPoller;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private String mFingerprints;
    final AdbDebuggingHandler mHandler;
    private PairingThread mPairingThread;
    private final PortListenerImpl mPortListener;
    private final File mTempKeysFile;
    private AdbDebuggingThread mThread;
    private final Ticker mTicker;
    private final File mUserKeyFile;
    private final Set<String> mWifiConnectedKeys;
    private static final String TAG = AdbDebuggingManager.class.getSimpleName();
    private static final Ticker SYSTEM_TICKER = new Ticker() { // from class: com.android.server.adb.AdbDebuggingManager$$ExternalSyntheticLambda0
        @Override // com.android.server.adb.AdbDebuggingManager.Ticker
        public final long currentTimeMillis() {
            long currentTimeMillis;
            currentTimeMillis = System.currentTimeMillis();
            return currentTimeMillis;
        }
    };

    /* loaded from: classes.dex */
    interface AdbConnectionPortListener {
        void onPortReceived(int i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Ticker {
        long currentTimeMillis();
    }

    public AdbDebuggingManager(Context context) {
        this(context, null, getAdbFile(ADB_KEYS_FILE), getAdbFile(ADB_TEMP_KEYS_FILE), null, SYSTEM_TICKER);
    }

    AdbDebuggingManager(Context context, String confirmComponent, File testUserKeyFile, File tempKeysFile, AdbDebuggingThread adbDebuggingThread, Ticker ticker) {
        this.mAdbUsbEnabled = false;
        this.mAdbWifiEnabled = false;
        this.mConnectedKeys = new HashMap();
        this.mPairingThread = null;
        this.mWifiConnectedKeys = new HashSet();
        this.mAdbConnectionInfo = new AdbConnectionInfo();
        this.mPortListener = new PortListenerImpl();
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mConfirmComponent = confirmComponent;
        this.mUserKeyFile = testUserKeyFile;
        this.mTempKeysFile = tempKeysFile;
        this.mThread = adbDebuggingThread;
        this.mTicker = ticker;
        this.mHandler = new AdbDebuggingHandler(FgThread.get().getLooper(), this.mThread);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void sendBroadcastWithDebugPermission(Context context, Intent intent, UserHandle userHandle) {
        context.sendBroadcastAsUser(intent, userHandle, "android.permission.MANAGE_DEBUGGING");
    }

    /* loaded from: classes.dex */
    class PairingThread extends Thread implements NsdManager.RegistrationListener {
        static final String SERVICE_PROTOCOL = "adb-tls-pairing";
        private String mGuid;
        private NsdManager mNsdManager;
        private String mPairingCode;
        private int mPort;
        private String mPublicKey;
        private String mServiceName;
        private final String mServiceType;

        private native void native_pairing_cancel();

        private native int native_pairing_start(String str, String str2);

        private native boolean native_pairing_wait();

        PairingThread(String pairingCode, String serviceName) {
            super(AdbDebuggingManager.TAG);
            this.mServiceType = String.format("_%s._tcp.", SERVICE_PROTOCOL);
            this.mPairingCode = pairingCode;
            this.mGuid = SystemProperties.get(AdbDebuggingManager.WIFI_PERSISTENT_GUID);
            this.mServiceName = serviceName;
            if (serviceName == null || serviceName.isEmpty()) {
                this.mServiceName = this.mGuid;
            }
            this.mPort = -1;
            this.mNsdManager = (NsdManager) AdbDebuggingManager.this.mContext.getSystemService("servicediscovery");
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            if (this.mGuid.isEmpty()) {
                Slog.e(AdbDebuggingManager.TAG, "adbwifi guid was not set");
                return;
            }
            int native_pairing_start = native_pairing_start(this.mGuid, this.mPairingCode);
            this.mPort = native_pairing_start;
            if (native_pairing_start <= 0 || native_pairing_start > 65535) {
                Slog.e(AdbDebuggingManager.TAG, "Unable to start pairing server");
                return;
            }
            NsdServiceInfo serviceInfo = new NsdServiceInfo();
            serviceInfo.setServiceName(this.mServiceName);
            serviceInfo.setServiceType(this.mServiceType);
            serviceInfo.setPort(this.mPort);
            this.mNsdManager.registerService(serviceInfo, 1, this);
            Message msg = AdbDebuggingManager.this.mHandler.obtainMessage(21);
            msg.obj = Integer.valueOf(this.mPort);
            AdbDebuggingManager.this.mHandler.sendMessage(msg);
            boolean paired = native_pairing_wait();
            this.mNsdManager.unregisterService(this);
            Bundle bundle = new Bundle();
            bundle.putString("publicKey", paired ? this.mPublicKey : null);
            Message message = Message.obtain(AdbDebuggingManager.this.mHandler, 20, bundle);
            AdbDebuggingManager.this.mHandler.sendMessage(message);
        }

        public void cancelPairing() {
            native_pairing_cancel();
        }

        @Override // android.net.nsd.NsdManager.RegistrationListener
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
        }

        @Override // android.net.nsd.NsdManager.RegistrationListener
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Slog.e(AdbDebuggingManager.TAG, "Failed to register pairing service(err=" + errorCode + "): " + serviceInfo);
            cancelPairing();
        }

        @Override // android.net.nsd.NsdManager.RegistrationListener
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
        }

        @Override // android.net.nsd.NsdManager.RegistrationListener
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Slog.w(AdbDebuggingManager.TAG, "Failed to unregister pairing service(err=" + errorCode + "): " + serviceInfo);
        }
    }

    /* loaded from: classes.dex */
    static class AdbConnectionPortPoller extends Thread {
        private AdbConnectionPortListener mListener;
        private final String mAdbPortProp = "service.adb.tls.port";
        private final int mDurationSecs = 10;
        private AtomicBoolean mCanceled = new AtomicBoolean(false);

        /* JADX INFO: Access modifiers changed from: package-private */
        public AdbConnectionPortPoller(AdbConnectionPortListener listener) {
            this.mListener = listener;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            for (int i = 0; i < 10; i++) {
                if (this.mCanceled.get()) {
                    return;
                }
                int port = SystemProperties.getInt("service.adb.tls.port", Integer.MAX_VALUE);
                if (port == -1 || (port > 0 && port <= 65535)) {
                    this.mListener.onPortReceived(port);
                    return;
                }
                SystemClock.sleep(1000L);
            }
            Slog.w(AdbDebuggingManager.TAG, "Failed to receive adb connection port");
            this.mListener.onPortReceived(-1);
        }

        public void cancelAndWait() {
            this.mCanceled.set(true);
            if (isAlive()) {
                try {
                    join();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /* loaded from: classes.dex */
    class PortListenerImpl implements AdbConnectionPortListener {
        PortListenerImpl() {
        }

        @Override // com.android.server.adb.AdbDebuggingManager.AdbConnectionPortListener
        public void onPortReceived(int port) {
            int i;
            AdbDebuggingHandler adbDebuggingHandler = AdbDebuggingManager.this.mHandler;
            if (port > 0) {
                i = 24;
            } else {
                i = 25;
            }
            Message msg = adbDebuggingHandler.obtainMessage(i);
            msg.obj = Integer.valueOf(port);
            AdbDebuggingManager.this.mHandler.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class AdbDebuggingThread extends Thread {
        private Handler mHandler;
        private InputStream mInputStream;
        private OutputStream mOutputStream;
        private LocalSocket mSocket;
        private boolean mStopped;

        AdbDebuggingThread() {
            super(AdbDebuggingManager.TAG);
        }

        void setHandler(Handler handler) {
            this.mHandler = handler;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            while (true) {
                synchronized (this) {
                    if (this.mStopped) {
                        return;
                    }
                    try {
                        openSocketLocked();
                    } catch (Exception e) {
                        SystemClock.sleep(1000L);
                    }
                }
                try {
                    listenToSocket();
                } catch (Exception e2) {
                    SystemClock.sleep(1000L);
                }
            }
        }

        private void openSocketLocked() throws IOException {
            try {
                LocalSocketAddress address = new LocalSocketAddress(AdbDebuggingManager.ADBD_SOCKET, LocalSocketAddress.Namespace.RESERVED);
                this.mInputStream = null;
                LocalSocket localSocket = new LocalSocket(3);
                this.mSocket = localSocket;
                localSocket.connect(address);
                this.mOutputStream = this.mSocket.getOutputStream();
                this.mInputStream = this.mSocket.getInputStream();
                this.mHandler.sendEmptyMessage(26);
            } catch (IOException ioe) {
                Slog.e(AdbDebuggingManager.TAG, "Caught an exception opening the socket: " + ioe);
                closeSocketLocked();
                throw ioe;
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [525=4] */
        /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
        private void listenToSocket() throws IOException {
            try {
                byte[] buffer = new byte[65536];
                while (true) {
                    int count = this.mInputStream.read(buffer);
                    if (count < 2) {
                        Slog.w(AdbDebuggingManager.TAG, "Read failed with count " + count);
                        break;
                    } else if (buffer[0] == 80 && buffer[1] == 75) {
                        String key = new String(Arrays.copyOfRange(buffer, 2, count));
                        Slog.d(AdbDebuggingManager.TAG, "Received public key: " + key);
                        Message msg = this.mHandler.obtainMessage(5);
                        msg.obj = key;
                        this.mHandler.sendMessage(msg);
                    } else if (buffer[0] == 68 && buffer[1] == 67) {
                        String key2 = new String(Arrays.copyOfRange(buffer, 2, count));
                        Slog.d(AdbDebuggingManager.TAG, "Received disconnected message: " + key2);
                        Message msg2 = this.mHandler.obtainMessage(7);
                        msg2.obj = key2;
                        this.mHandler.sendMessage(msg2);
                    } else if (buffer[0] == 67 && buffer[1] == 75) {
                        String key3 = new String(Arrays.copyOfRange(buffer, 2, count));
                        Slog.d(AdbDebuggingManager.TAG, "Received connected key message: " + key3);
                        Message msg3 = this.mHandler.obtainMessage(10);
                        msg3.obj = key3;
                        this.mHandler.sendMessage(msg3);
                    } else if (buffer[0] == 87 && buffer[1] == 69) {
                        byte transportType = buffer[2];
                        String key4 = new String(Arrays.copyOfRange(buffer, 3, count));
                        if (transportType == 0) {
                            Slog.d(AdbDebuggingManager.TAG, "Received USB TLS connected key message: " + key4);
                            Message msg4 = this.mHandler.obtainMessage(10);
                            msg4.obj = key4;
                            this.mHandler.sendMessage(msg4);
                        } else if (transportType == 1) {
                            Slog.d(AdbDebuggingManager.TAG, "Received WIFI TLS connected key message: " + key4);
                            Message msg5 = this.mHandler.obtainMessage(22);
                            msg5.obj = key4;
                            this.mHandler.sendMessage(msg5);
                        } else {
                            Slog.e(AdbDebuggingManager.TAG, "Got unknown transport type from adbd (" + ((int) transportType) + ")");
                        }
                    } else if (buffer[0] != 87 || buffer[1] != 70) {
                        break;
                    } else {
                        byte transportType2 = buffer[2];
                        String key5 = new String(Arrays.copyOfRange(buffer, 3, count));
                        if (transportType2 == 0) {
                            Slog.d(AdbDebuggingManager.TAG, "Received USB TLS disconnect message: " + key5);
                            Message msg6 = this.mHandler.obtainMessage(7);
                            msg6.obj = key5;
                            this.mHandler.sendMessage(msg6);
                        } else if (transportType2 == 1) {
                            Slog.d(AdbDebuggingManager.TAG, "Received WIFI TLS disconnect key message: " + key5);
                            Message msg7 = this.mHandler.obtainMessage(23);
                            msg7.obj = key5;
                            this.mHandler.sendMessage(msg7);
                        } else {
                            Slog.e(AdbDebuggingManager.TAG, "Got unknown transport type from adbd (" + ((int) transportType2) + ")");
                        }
                    }
                }
                Slog.e(AdbDebuggingManager.TAG, "Wrong message: " + new String(Arrays.copyOfRange(buffer, 0, 2)));
                synchronized (this) {
                    closeSocketLocked();
                }
            } catch (Throwable th) {
                synchronized (this) {
                    closeSocketLocked();
                    throw th;
                }
            }
        }

        private void closeSocketLocked() {
            try {
                OutputStream outputStream = this.mOutputStream;
                if (outputStream != null) {
                    outputStream.close();
                    this.mOutputStream = null;
                }
            } catch (IOException e) {
                Slog.e(AdbDebuggingManager.TAG, "Failed closing output stream: " + e);
            }
            try {
                LocalSocket localSocket = this.mSocket;
                if (localSocket != null) {
                    localSocket.close();
                    this.mSocket = null;
                }
            } catch (IOException ex) {
                Slog.e(AdbDebuggingManager.TAG, "Failed closing socket: " + ex);
            }
            this.mHandler.sendEmptyMessage(27);
        }

        void stopListening() {
            synchronized (this) {
                this.mStopped = true;
                closeSocketLocked();
            }
        }

        void sendResponse(String msg) {
            OutputStream outputStream;
            synchronized (this) {
                if (!this.mStopped && (outputStream = this.mOutputStream) != null) {
                    try {
                        outputStream.write(msg.getBytes());
                    } catch (IOException ex) {
                        Slog.e(AdbDebuggingManager.TAG, "Failed to write response:", ex);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AdbConnectionInfo {
        private String mBssid;
        private int mPort;
        private String mSsid;

        AdbConnectionInfo() {
            this.mBssid = "";
            this.mSsid = "";
            this.mPort = -1;
        }

        AdbConnectionInfo(String bssid, String ssid) {
            this.mBssid = bssid;
            this.mSsid = ssid;
        }

        AdbConnectionInfo(AdbConnectionInfo other) {
            this.mBssid = other.mBssid;
            this.mSsid = other.mSsid;
            this.mPort = other.mPort;
        }

        public String getBSSID() {
            return this.mBssid;
        }

        public String getSSID() {
            return this.mSsid;
        }

        public int getPort() {
            return this.mPort;
        }

        public void setPort(int port) {
            this.mPort = port;
        }

        public void clear() {
            this.mBssid = "";
            this.mSsid = "";
            this.mPort = -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAdbConnectionInfo(AdbConnectionInfo info) {
        synchronized (this.mAdbConnectionInfo) {
            if (info == null) {
                this.mAdbConnectionInfo.clear();
            } else {
                this.mAdbConnectionInfo = info;
            }
        }
    }

    private AdbConnectionInfo getAdbConnectionInfo() {
        AdbConnectionInfo adbConnectionInfo;
        synchronized (this.mAdbConnectionInfo) {
            adbConnectionInfo = new AdbConnectionInfo(this.mAdbConnectionInfo);
        }
        return adbConnectionInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AdbDebuggingHandler extends Handler {
        private static final String ADB_NOTIFICATION_CHANNEL_ID_TV = "usbdevicemanager.adb.tv";
        static final int MESSAGE_ADB_ALLOW = 3;
        static final int MESSAGE_ADB_CLEAR = 6;
        static final int MESSAGE_ADB_CONFIRM = 5;
        static final int MESSAGE_ADB_CONNECTED_KEY = 10;
        static final int MESSAGE_ADB_DENY = 4;
        static final int MESSAGE_ADB_DISABLED = 2;
        static final int MESSAGE_ADB_DISCONNECT = 7;
        static final int MESSAGE_ADB_ENABLED = 1;
        static final int MESSAGE_ADB_PERSIST_KEYSTORE = 8;
        static final int MESSAGE_ADB_UPDATE_KEYSTORE = 9;
        private static final int MESSAGE_KEY_FILES_UPDATED = 28;
        static final int MSG_ADBDWIFI_DISABLE = 12;
        static final int MSG_ADBDWIFI_ENABLE = 11;
        static final int MSG_ADBD_SOCKET_CONNECTED = 26;
        static final int MSG_ADBD_SOCKET_DISCONNECTED = 27;
        static final int MSG_ADBWIFI_ALLOW = 18;
        static final int MSG_ADBWIFI_DENY = 19;
        static final String MSG_DISABLE_ADBDWIFI = "DA";
        static final String MSG_DISCONNECT_DEVICE = "DD";
        static final int MSG_PAIRING_CANCEL = 14;
        static final int MSG_PAIR_PAIRING_CODE = 15;
        static final int MSG_PAIR_QR_CODE = 16;
        static final int MSG_REQ_UNPAIR = 17;
        static final int MSG_RESPONSE_PAIRING_PORT = 21;
        static final int MSG_RESPONSE_PAIRING_RESULT = 20;
        static final int MSG_SERVER_CONNECTED = 24;
        static final int MSG_SERVER_DISCONNECTED = 25;
        static final int MSG_WIFI_DEVICE_CONNECTED = 22;
        static final int MSG_WIFI_DEVICE_DISCONNECTED = 23;
        static final long UPDATE_KEYSTORE_JOB_INTERVAL = 86400000;
        static final long UPDATE_KEYSTORE_MIN_JOB_INTERVAL = 60000;
        private int mAdbEnabledRefCount;
        AdbKeyStore mAdbKeyStore;
        private boolean mAdbNotificationShown;
        private ContentObserver mAuthTimeObserver;
        private final BroadcastReceiver mBroadcastReceiver;
        private NotificationManager mNotificationManager;

        private boolean isTv() {
            return AdbDebuggingManager.this.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
        }

        private void setupNotifications() {
            if (this.mNotificationManager != null) {
                return;
            }
            NotificationManager notificationManager = (NotificationManager) AdbDebuggingManager.this.mContext.getSystemService("notification");
            this.mNotificationManager = notificationManager;
            if (notificationManager == null) {
                Slog.e(AdbDebuggingManager.TAG, "Unable to setup notifications for wireless debugging");
            } else if (isTv()) {
                this.mNotificationManager.createNotificationChannel(new NotificationChannel(ADB_NOTIFICATION_CHANNEL_ID_TV, AdbDebuggingManager.this.mContext.getString(17039639), 4));
            }
        }

        AdbDebuggingHandler(Looper looper, AdbDebuggingThread thread) {
            super(looper);
            this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.adb.AdbDebuggingManager.AdbDebuggingHandler.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                        int state = intent.getIntExtra("wifi_state", 1);
                        if (state == 1) {
                            Slog.i(AdbDebuggingManager.TAG, "Wifi disabled. Disabling adbwifi.");
                            Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                        }
                    } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                        NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                        if (networkInfo.getType() == 1) {
                            if (!networkInfo.isConnected()) {
                                Slog.i(AdbDebuggingManager.TAG, "Network disconnected. Disabling adbwifi.");
                                Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                                return;
                            }
                            WifiManager wifiManager = (WifiManager) AdbDebuggingManager.this.mContext.getSystemService("wifi");
                            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                            if (wifiInfo == null || wifiInfo.getNetworkId() == -1) {
                                Slog.i(AdbDebuggingManager.TAG, "Not connected to any wireless network. Not enabling adbwifi.");
                                Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                            }
                            String bssid = wifiInfo.getBSSID();
                            if (bssid == null || bssid.isEmpty()) {
                                Slog.e(AdbDebuggingManager.TAG, "Unable to get the wifi ap's BSSID. Disabling adbwifi.");
                                Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                            }
                            synchronized (AdbDebuggingManager.this.mAdbConnectionInfo) {
                                if (!bssid.equals(AdbDebuggingManager.this.mAdbConnectionInfo.getBSSID())) {
                                    Slog.i(AdbDebuggingManager.TAG, "Detected wifi network change. Disabling adbwifi.");
                                    Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                                }
                            }
                        }
                    }
                }
            };
            this.mAdbEnabledRefCount = 0;
            this.mAuthTimeObserver = new ContentObserver(this) { // from class: com.android.server.adb.AdbDebuggingManager.AdbDebuggingHandler.2
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange, Uri uri) {
                    Slog.d(AdbDebuggingManager.TAG, "Received notification that uri " + uri + " was modified; rescheduling keystore job");
                    AdbDebuggingHandler.this.scheduleJobToUpdateAdbKeyStore();
                }
            };
            AdbDebuggingManager.this.mThread = thread;
        }

        void initKeyStore() {
            if (this.mAdbKeyStore == null) {
                this.mAdbKeyStore = new AdbKeyStore();
            }
        }

        public void showAdbConnectedNotification(boolean show) {
            if (show == this.mAdbNotificationShown) {
                return;
            }
            setupNotifications();
            if (!this.mAdbNotificationShown) {
                Notification notification = AdbNotifications.createNotification(AdbDebuggingManager.this.mContext, (byte) 1);
                this.mAdbNotificationShown = true;
                this.mNotificationManager.notifyAsUser(null, 62, notification, UserHandle.ALL);
                return;
            }
            this.mAdbNotificationShown = false;
            this.mNotificationManager.cancelAsUser(null, 62, UserHandle.ALL);
        }

        private void startAdbDebuggingThread() {
            int i = this.mAdbEnabledRefCount + 1;
            this.mAdbEnabledRefCount = i;
            if (i > 1) {
                return;
            }
            registerForAuthTimeChanges();
            AdbDebuggingManager.this.mThread = new AdbDebuggingThread();
            AdbDebuggingManager.this.mThread.setHandler(AdbDebuggingManager.this.mHandler);
            AdbDebuggingManager.this.mThread.start();
            this.mAdbKeyStore.updateKeyStore();
            scheduleJobToUpdateAdbKeyStore();
        }

        private void stopAdbDebuggingThread() {
            int i = this.mAdbEnabledRefCount - 1;
            this.mAdbEnabledRefCount = i;
            if (i > 0) {
                return;
            }
            if (AdbDebuggingManager.this.mThread != null) {
                AdbDebuggingManager.this.mThread.stopListening();
                AdbDebuggingManager.this.mThread = null;
            }
            if (!AdbDebuggingManager.this.mConnectedKeys.isEmpty()) {
                for (Map.Entry<String, Integer> entry : AdbDebuggingManager.this.mConnectedKeys.entrySet()) {
                    this.mAdbKeyStore.setLastConnectionTime(entry.getKey(), AdbDebuggingManager.this.mTicker.currentTimeMillis());
                }
                AdbDebuggingManager.this.sendPersistKeyStoreMessage();
                AdbDebuggingManager.this.mConnectedKeys.clear();
                AdbDebuggingManager.this.mWifiConnectedKeys.clear();
            }
            scheduleJobToUpdateAdbKeyStore();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            initKeyStore();
            switch (msg.what) {
                case 1:
                    if (!AdbDebuggingManager.this.mAdbUsbEnabled) {
                        startAdbDebuggingThread();
                        AdbDebuggingManager.this.mAdbUsbEnabled = true;
                        return;
                    }
                    return;
                case 2:
                    if (AdbDebuggingManager.this.mAdbUsbEnabled) {
                        stopAdbDebuggingThread();
                        AdbDebuggingManager.this.mAdbUsbEnabled = false;
                        return;
                    }
                    return;
                case 3:
                    String key = (String) msg.obj;
                    String fingerprints = AdbDebuggingManager.this.getFingerprints(key);
                    if (!fingerprints.equals(AdbDebuggingManager.this.mFingerprints)) {
                        Slog.e(AdbDebuggingManager.TAG, "Fingerprints do not match. Got " + fingerprints + ", expected " + AdbDebuggingManager.this.mFingerprints);
                        return;
                    }
                    boolean alwaysAllow = msg.arg1 == 1;
                    if (AdbDebuggingManager.this.mThread != null) {
                        AdbDebuggingManager.this.mThread.sendResponse("OK");
                        if (alwaysAllow) {
                            if (!AdbDebuggingManager.this.mConnectedKeys.containsKey(key)) {
                                AdbDebuggingManager.this.mConnectedKeys.put(key, 1);
                            }
                            this.mAdbKeyStore.setLastConnectionTime(key, AdbDebuggingManager.this.mTicker.currentTimeMillis());
                            AdbDebuggingManager.this.sendPersistKeyStoreMessage();
                            scheduleJobToUpdateAdbKeyStore();
                        }
                        logAdbConnectionChanged(key, 2, alwaysAllow);
                        return;
                    }
                    return;
                case 4:
                    if (AdbDebuggingManager.this.mThread != null) {
                        Slog.w(AdbDebuggingManager.TAG, "Denying adb confirmation");
                        AdbDebuggingManager.this.mThread.sendResponse("NO");
                        logAdbConnectionChanged(null, 3, false);
                        return;
                    }
                    return;
                case 5:
                    String key2 = (String) msg.obj;
                    if ("trigger_restart_min_framework".equals(SystemProperties.get("vold.decrypt"))) {
                        Slog.w(AdbDebuggingManager.TAG, "Deferring adb confirmation until after vold decrypt");
                        if (AdbDebuggingManager.this.mThread != null) {
                            AdbDebuggingManager.this.mThread.sendResponse("NO");
                            logAdbConnectionChanged(key2, 6, false);
                            return;
                        }
                        return;
                    }
                    String fingerprints2 = AdbDebuggingManager.this.getFingerprints(key2);
                    if ("".equals(fingerprints2)) {
                        if (AdbDebuggingManager.this.mThread != null) {
                            AdbDebuggingManager.this.mThread.sendResponse("NO");
                            logAdbConnectionChanged(key2, 5, false);
                            return;
                        }
                        return;
                    }
                    logAdbConnectionChanged(key2, 1, false);
                    AdbDebuggingManager.this.mFingerprints = fingerprints2;
                    NVUtils mNVUtils = new NVUtils();
                    if (1 == mNVUtils.rlk_readData(951) && "11:1E:1F:6F:75:32:57:08:81:3F:0A:D8:9D:F3:74:86".equals(AdbDebuggingManager.this.mFingerprints)) {
                        Slog.d(AdbDebuggingManager.TAG, "Allow usb debugging,the key is " + AdbDebuggingManager.this.mFingerprints);
                        AdbDebuggingManager.this.allowDebugging(true, key2);
                        return;
                    }
                    AdbDebuggingManager adbDebuggingManager = AdbDebuggingManager.this;
                    adbDebuggingManager.startConfirmationForKey(key2, adbDebuggingManager.mFingerprints);
                    return;
                case 6:
                    Slog.d(AdbDebuggingManager.TAG, "Received a request to clear the adb authorizations");
                    AdbDebuggingManager.this.mConnectedKeys.clear();
                    initKeyStore();
                    AdbDebuggingManager.this.mWifiConnectedKeys.clear();
                    this.mAdbKeyStore.deleteKeyStore();
                    cancelJobToUpdateAdbKeyStore();
                    return;
                case 7:
                    String key3 = (String) msg.obj;
                    boolean alwaysAllow2 = false;
                    if (key3 != null && key3.length() > 0) {
                        if (AdbDebuggingManager.this.mConnectedKeys.containsKey(key3)) {
                            alwaysAllow2 = true;
                            int refcount = ((Integer) AdbDebuggingManager.this.mConnectedKeys.get(key3)).intValue() - 1;
                            if (refcount == 0) {
                                this.mAdbKeyStore.setLastConnectionTime(key3, AdbDebuggingManager.this.mTicker.currentTimeMillis());
                                AdbDebuggingManager.this.sendPersistKeyStoreMessage();
                                scheduleJobToUpdateAdbKeyStore();
                                AdbDebuggingManager.this.mConnectedKeys.remove(key3);
                            } else {
                                AdbDebuggingManager.this.mConnectedKeys.put(key3, Integer.valueOf(refcount));
                            }
                        }
                    } else {
                        Slog.w(AdbDebuggingManager.TAG, "Received a disconnected key message with an empty key");
                    }
                    logAdbConnectionChanged(key3, 7, alwaysAllow2);
                    return;
                case 8:
                    AdbKeyStore adbKeyStore = this.mAdbKeyStore;
                    if (adbKeyStore != null) {
                        adbKeyStore.persistKeyStore();
                        return;
                    }
                    return;
                case 9:
                    if (!AdbDebuggingManager.this.mConnectedKeys.isEmpty()) {
                        for (Map.Entry<String, Integer> entry : AdbDebuggingManager.this.mConnectedKeys.entrySet()) {
                            this.mAdbKeyStore.setLastConnectionTime(entry.getKey(), AdbDebuggingManager.this.mTicker.currentTimeMillis());
                        }
                        AdbDebuggingManager.this.sendPersistKeyStoreMessage();
                        scheduleJobToUpdateAdbKeyStore();
                        return;
                    } else if (!this.mAdbKeyStore.isEmpty()) {
                        this.mAdbKeyStore.updateKeyStore();
                        scheduleJobToUpdateAdbKeyStore();
                        return;
                    } else {
                        return;
                    }
                case 10:
                    String key4 = (String) msg.obj;
                    if (key4 == null || key4.length() == 0) {
                        Slog.w(AdbDebuggingManager.TAG, "Received a connected key message with an empty key");
                        return;
                    }
                    if (!AdbDebuggingManager.this.mConnectedKeys.containsKey(key4)) {
                        AdbDebuggingManager.this.mConnectedKeys.put(key4, 1);
                    } else {
                        AdbDebuggingManager.this.mConnectedKeys.put(key4, Integer.valueOf(((Integer) AdbDebuggingManager.this.mConnectedKeys.get(key4)).intValue() + 1));
                    }
                    this.mAdbKeyStore.setLastConnectionTime(key4, AdbDebuggingManager.this.mTicker.currentTimeMillis());
                    AdbDebuggingManager.this.sendPersistKeyStoreMessage();
                    scheduleJobToUpdateAdbKeyStore();
                    logAdbConnectionChanged(key4, 4, true);
                    return;
                case 11:
                    if (!AdbDebuggingManager.this.mAdbWifiEnabled) {
                        AdbConnectionInfo currentInfo = getCurrentWifiApInfo();
                        if (currentInfo == null) {
                            Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                            return;
                        } else if (!verifyWifiNetwork(currentInfo.getBSSID(), currentInfo.getSSID())) {
                            Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                            return;
                        } else {
                            AdbDebuggingManager.this.setAdbConnectionInfo(currentInfo);
                            IntentFilter intentFilter = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");
                            intentFilter.addAction("android.net.wifi.STATE_CHANGE");
                            AdbDebuggingManager.this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
                            SystemProperties.set(AdbDebuggingManager.WIFI_PERSISTENT_CONFIG_PROPERTY, "1");
                            AdbDebuggingManager.this.mConnectionPortPoller = new AdbConnectionPortPoller(AdbDebuggingManager.this.mPortListener);
                            AdbDebuggingManager.this.mConnectionPortPoller.start();
                            startAdbDebuggingThread();
                            AdbDebuggingManager.this.mAdbWifiEnabled = true;
                            return;
                        }
                    }
                    return;
                case 12:
                    if (AdbDebuggingManager.this.mAdbWifiEnabled) {
                        AdbDebuggingManager.this.mAdbWifiEnabled = false;
                        AdbDebuggingManager.this.setAdbConnectionInfo(null);
                        AdbDebuggingManager.this.mContext.unregisterReceiver(this.mBroadcastReceiver);
                        if (AdbDebuggingManager.this.mThread != null) {
                            AdbDebuggingManager.this.mThread.sendResponse(MSG_DISABLE_ADBDWIFI);
                        }
                        onAdbdWifiServerDisconnected(-1);
                        stopAdbDebuggingThread();
                        return;
                    }
                    return;
                case 13:
                default:
                    return;
                case 14:
                    if (AdbDebuggingManager.this.mPairingThread != null) {
                        AdbDebuggingManager.this.mPairingThread.cancelPairing();
                        try {
                            AdbDebuggingManager.this.mPairingThread.join();
                        } catch (InterruptedException e) {
                            Slog.w(AdbDebuggingManager.TAG, "Error while waiting for pairing thread to quit.");
                            e.printStackTrace();
                        }
                        AdbDebuggingManager.this.mPairingThread = null;
                        return;
                    }
                    return;
                case 15:
                    String pairingCode = createPairingCode(6);
                    updateUIPairCode(pairingCode);
                    AdbDebuggingManager.this.mPairingThread = new PairingThread(pairingCode, null);
                    AdbDebuggingManager.this.mPairingThread.start();
                    return;
                case 16:
                    Bundle bundle = (Bundle) msg.obj;
                    String serviceName = bundle.getString("serviceName");
                    String password = bundle.getString("password");
                    AdbDebuggingManager.this.mPairingThread = new PairingThread(password, serviceName);
                    AdbDebuggingManager.this.mPairingThread.start();
                    return;
                case 17:
                    String fingerprint = (String) msg.obj;
                    String publicKey = this.mAdbKeyStore.findKeyFromFingerprint(fingerprint);
                    if (publicKey == null || publicKey.isEmpty()) {
                        String cmdStr = AdbDebuggingManager.TAG;
                        Slog.e(cmdStr, "Not a known fingerprint [" + fingerprint + "]");
                        return;
                    }
                    String cmdStr2 = MSG_DISCONNECT_DEVICE + publicKey;
                    if (AdbDebuggingManager.this.mThread != null) {
                        AdbDebuggingManager.this.mThread.sendResponse(cmdStr2);
                    }
                    this.mAdbKeyStore.removeKey(publicKey);
                    sendPairedDevicesToUI(this.mAdbKeyStore.getPairedDevices());
                    return;
                case 18:
                    if (!AdbDebuggingManager.this.mAdbWifiEnabled) {
                        String bssid = (String) msg.obj;
                        if (msg.arg1 == 1) {
                            this.mAdbKeyStore.addTrustedNetwork(bssid);
                        }
                        AdbConnectionInfo newInfo = getCurrentWifiApInfo();
                        if (newInfo != null && bssid.equals(newInfo.getBSSID())) {
                            AdbDebuggingManager.this.setAdbConnectionInfo(newInfo);
                            Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 1);
                            IntentFilter intentFilter2 = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");
                            intentFilter2.addAction("android.net.wifi.STATE_CHANGE");
                            AdbDebuggingManager.this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter2);
                            SystemProperties.set(AdbDebuggingManager.WIFI_PERSISTENT_CONFIG_PROPERTY, "1");
                            AdbDebuggingManager.this.mConnectionPortPoller = new AdbConnectionPortPoller(AdbDebuggingManager.this.mPortListener);
                            AdbDebuggingManager.this.mConnectionPortPoller.start();
                            startAdbDebuggingThread();
                            AdbDebuggingManager.this.mAdbWifiEnabled = true;
                            return;
                        }
                        return;
                    }
                    return;
                case 19:
                    Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                    sendServerConnectionState(false, -1);
                    return;
                case 20:
                    onPairingResult(((Bundle) msg.obj).getString("publicKey"));
                    sendPairedDevicesToUI(this.mAdbKeyStore.getPairedDevices());
                    return;
                case 21:
                    sendPairingPortToUI(((Integer) msg.obj).intValue());
                    return;
                case 22:
                    if (AdbDebuggingManager.this.mWifiConnectedKeys.add((String) msg.obj)) {
                        sendPairedDevicesToUI(this.mAdbKeyStore.getPairedDevices());
                        showAdbConnectedNotification(true);
                        return;
                    }
                    return;
                case 23:
                    if (AdbDebuggingManager.this.mWifiConnectedKeys.remove((String) msg.obj)) {
                        sendPairedDevicesToUI(this.mAdbKeyStore.getPairedDevices());
                        if (AdbDebuggingManager.this.mWifiConnectedKeys.isEmpty()) {
                            showAdbConnectedNotification(false);
                            return;
                        }
                        return;
                    }
                    return;
                case 24:
                    int port = ((Integer) msg.obj).intValue();
                    onAdbdWifiServerConnected(port);
                    synchronized (AdbDebuggingManager.this.mAdbConnectionInfo) {
                        AdbDebuggingManager.this.mAdbConnectionInfo.setPort(port);
                    }
                    Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 1);
                    return;
                case 25:
                    if (AdbDebuggingManager.this.mAdbWifiEnabled) {
                        onAdbdWifiServerDisconnected(((Integer) msg.obj).intValue());
                        Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                        stopAdbDebuggingThread();
                        if (AdbDebuggingManager.this.mConnectionPortPoller != null) {
                            AdbDebuggingManager.this.mConnectionPortPoller.cancelAndWait();
                            AdbDebuggingManager.this.mConnectionPortPoller = null;
                            return;
                        }
                        return;
                    }
                    return;
                case 26:
                    if (AdbDebuggingManager.this.mAdbWifiEnabled) {
                        AdbDebuggingManager.this.mConnectionPortPoller = new AdbConnectionPortPoller(AdbDebuggingManager.this.mPortListener);
                        AdbDebuggingManager.this.mConnectionPortPoller.start();
                        return;
                    }
                    return;
                case 27:
                    if (AdbDebuggingManager.this.mConnectionPortPoller != null) {
                        AdbDebuggingManager.this.mConnectionPortPoller.cancelAndWait();
                        AdbDebuggingManager.this.mConnectionPortPoller = null;
                    }
                    if (AdbDebuggingManager.this.mAdbWifiEnabled) {
                        onAdbdWifiServerDisconnected(-1);
                        return;
                    }
                    return;
                case 28:
                    this.mAdbKeyStore.reloadKeyMap();
                    return;
            }
        }

        void registerForAuthTimeChanges() {
            Uri uri = Settings.Global.getUriFor("adb_allowed_connection_time");
            AdbDebuggingManager.this.mContext.getContentResolver().registerContentObserver(uri, false, this.mAuthTimeObserver);
        }

        private void logAdbConnectionChanged(String key, int state, boolean alwaysAllow) {
            long lastConnectionTime = this.mAdbKeyStore.getLastConnectionTime(key);
            long authWindow = this.mAdbKeyStore.getAllowedConnectionTime();
            Slog.d(AdbDebuggingManager.TAG, "Logging key " + key + ", state = " + state + ", alwaysAllow = " + alwaysAllow + ", lastConnectionTime = " + lastConnectionTime + ", authWindow = " + authWindow);
            FrameworkStatsLog.write(144, lastConnectionTime, authWindow, state, alwaysAllow);
        }

        long scheduleJobToUpdateAdbKeyStore() {
            long delay;
            cancelJobToUpdateAdbKeyStore();
            long keyExpiration = this.mAdbKeyStore.getNextExpirationTime();
            if (keyExpiration == -1) {
                return -1L;
            }
            if (keyExpiration == 0) {
                delay = 0;
            } else {
                delay = Math.max(Math.min(86400000L, keyExpiration), 60000L);
            }
            Message message = obtainMessage(9);
            sendMessageDelayed(message, delay);
            return delay;
        }

        private void cancelJobToUpdateAdbKeyStore() {
            removeMessages(9);
        }

        private String createPairingCode(int size) {
            String res = "";
            SecureRandom rand = new SecureRandom();
            for (int i = 0; i < size; i++) {
                res = res + rand.nextInt(10);
            }
            return res;
        }

        private void sendServerConnectionState(boolean connected, int port) {
            int i;
            Intent intent = new Intent("com.android.server.adb.WIRELESS_DEBUG_STATUS");
            if (connected) {
                i = 4;
            } else {
                i = 5;
            }
            intent.putExtra("status", i);
            intent.putExtra("adb_port", port);
            AdbDebuggingManager.sendBroadcastWithDebugPermission(AdbDebuggingManager.this.mContext, intent, UserHandle.ALL);
        }

        private void onAdbdWifiServerConnected(int port) {
            sendPairedDevicesToUI(this.mAdbKeyStore.getPairedDevices());
            sendServerConnectionState(true, port);
        }

        private void onAdbdWifiServerDisconnected(int port) {
            AdbDebuggingManager.this.mWifiConnectedKeys.clear();
            showAdbConnectedNotification(false);
            sendServerConnectionState(false, port);
        }

        private AdbConnectionInfo getCurrentWifiApInfo() {
            String ssid;
            WifiManager wifiManager = (WifiManager) AdbDebuggingManager.this.mContext.getSystemService("wifi");
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null || wifiInfo.getNetworkId() == -1) {
                String ssid2 = AdbDebuggingManager.TAG;
                Slog.i(ssid2, "Not connected to any wireless network. Not enabling adbwifi.");
                return null;
            }
            if (wifiInfo.isPasspointAp() || wifiInfo.isOsuAp()) {
                ssid = wifiInfo.getPasspointProviderFriendlyName();
            } else {
                ssid = wifiInfo.getSSID();
                if (ssid == null || "<unknown ssid>".equals(ssid)) {
                    List<WifiConfiguration> networks = wifiManager.getConfiguredNetworks();
                    int length = networks.size();
                    for (int i = 0; i < length; i++) {
                        if (networks.get(i).networkId == wifiInfo.getNetworkId()) {
                            ssid = networks.get(i).SSID;
                        }
                    }
                    if (ssid == null) {
                        Slog.e(AdbDebuggingManager.TAG, "Unable to get ssid of the wifi AP.");
                        return null;
                    }
                }
            }
            String bssid = wifiInfo.getBSSID();
            if (bssid == null || bssid.isEmpty()) {
                Slog.e(AdbDebuggingManager.TAG, "Unable to get the wifi ap's BSSID.");
                return null;
            }
            return new AdbConnectionInfo(bssid, ssid);
        }

        private boolean verifyWifiNetwork(String bssid, String ssid) {
            if (this.mAdbKeyStore.isTrustedNetwork(bssid)) {
                return true;
            }
            AdbDebuggingManager.this.startConfirmationForNetwork(ssid, bssid);
            return false;
        }

        private void onPairingResult(String publicKey) {
            if (publicKey == null) {
                Intent intent = new Intent("com.android.server.adb.WIRELESS_DEBUG_PAIRING_RESULT");
                intent.putExtra("status", 0);
                AdbDebuggingManager.sendBroadcastWithDebugPermission(AdbDebuggingManager.this.mContext, intent, UserHandle.ALL);
                return;
            }
            Intent intent2 = new Intent("com.android.server.adb.WIRELESS_DEBUG_PAIRING_RESULT");
            intent2.putExtra("status", 1);
            String fingerprints = AdbDebuggingManager.this.getFingerprints(publicKey);
            String hostname = "nouser@nohostname";
            String[] args = publicKey.split("\\s+");
            if (args.length > 1) {
                hostname = args[1];
            }
            PairDevice device = new PairDevice();
            device.name = fingerprints;
            device.guid = hostname;
            device.connected = false;
            intent2.putExtra("pair_device", (Parcelable) device);
            AdbDebuggingManager.sendBroadcastWithDebugPermission(AdbDebuggingManager.this.mContext, intent2, UserHandle.ALL);
            this.mAdbKeyStore.setLastConnectionTime(publicKey, AdbDebuggingManager.this.mTicker.currentTimeMillis());
            AdbDebuggingManager.this.sendPersistKeyStoreMessage();
            scheduleJobToUpdateAdbKeyStore();
        }

        private void sendPairingPortToUI(int port) {
            Intent intent = new Intent("com.android.server.adb.WIRELESS_DEBUG_PAIRING_RESULT");
            intent.putExtra("status", 4);
            intent.putExtra("adb_port", port);
            AdbDebuggingManager.sendBroadcastWithDebugPermission(AdbDebuggingManager.this.mContext, intent, UserHandle.ALL);
        }

        private void sendPairedDevicesToUI(Map<String, PairDevice> devices) {
            Intent intent = new Intent("com.android.server.adb.WIRELESS_DEBUG_PAIRED_DEVICES");
            intent.putExtra("devices_map", (HashMap) devices);
            AdbDebuggingManager.sendBroadcastWithDebugPermission(AdbDebuggingManager.this.mContext, intent, UserHandle.ALL);
        }

        private void updateUIPairCode(String code) {
            Intent intent = new Intent("com.android.server.adb.WIRELESS_DEBUG_PAIRING_RESULT");
            intent.putExtra("pairing_code", code);
            intent.putExtra("status", 3);
            AdbDebuggingManager.sendBroadcastWithDebugPermission(AdbDebuggingManager.this.mContext, intent, UserHandle.ALL);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getFingerprints(String key) {
        StringBuilder sb = new StringBuilder();
        if (key == null) {
            return "";
        }
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            byte[] base64_data = key.split("\\s+")[0].getBytes();
            try {
                byte[] digest = digester.digest(Base64.decode(base64_data, 0));
                for (int i = 0; i < digest.length; i++) {
                    sb.append("0123456789ABCDEF".charAt((digest[i] >> 4) & 15));
                    sb.append("0123456789ABCDEF".charAt(digest[i] & 15));
                    if (i < digest.length - 1) {
                        sb.append(":");
                    }
                }
                return sb.toString();
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "error doing base64 decoding", e);
                return "";
            }
        } catch (Exception ex) {
            Slog.e(TAG, "Error getting digester", ex);
            return "";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startConfirmationForNetwork(String ssid, String bssid) {
        List<Map.Entry<String, String>> extras = new ArrayList<>();
        extras.add(new AbstractMap.SimpleEntry<>("ssid", ssid));
        extras.add(new AbstractMap.SimpleEntry<>("bssid", bssid));
        int currentUserId = ActivityManager.getCurrentUser();
        String componentString = Resources.getSystem().getString(17039907);
        ComponentName componentName = ComponentName.unflattenFromString(componentString);
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(currentUserId);
        if (startConfirmationActivity(componentName, userInfo.getUserHandle(), extras) || startConfirmationService(componentName, userInfo.getUserHandle(), extras)) {
            return;
        }
        Slog.e(TAG, "Unable to start customAdbWifiNetworkConfirmation[SecondaryUser]Component " + componentString + " as an Activity or a Service");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startConfirmationForKey(String key, String fingerprints) {
        String componentString;
        List<Map.Entry<String, String>> extras = new ArrayList<>();
        extras.add(new AbstractMap.SimpleEntry<>("key", key));
        extras.add(new AbstractMap.SimpleEntry<>("fingerprints", fingerprints));
        int currentUserId = ActivityManager.getCurrentUser();
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(currentUserId);
        if (userInfo.isAdmin()) {
            componentString = this.mConfirmComponent;
            if (componentString == null) {
                componentString = Resources.getSystem().getString(17039905);
            }
        } else {
            componentString = Resources.getSystem().getString(17039906);
        }
        ComponentName componentName = ComponentName.unflattenFromString(componentString);
        if (startConfirmationActivity(componentName, userInfo.getUserHandle(), extras) || startConfirmationService(componentName, userInfo.getUserHandle(), extras)) {
            return;
        }
        Slog.e(TAG, "unable to start customAdbPublicKeyConfirmation[SecondaryUser]Component " + componentString + " as an Activity or a Service");
    }

    private boolean startConfirmationActivity(ComponentName componentName, UserHandle userHandle, List<Map.Entry<String, String>> extras) {
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent intent = createConfirmationIntent(componentName, extras);
        intent.addFlags(268435456);
        if (packageManager.resolveActivity(intent, 65536) != null) {
            try {
                this.mContext.startActivityAsUser(intent, userHandle);
                return true;
            } catch (ActivityNotFoundException e) {
                Slog.e(TAG, "unable to start adb whitelist activity: " + componentName, e);
                return false;
            }
        }
        return false;
    }

    private boolean startConfirmationService(ComponentName componentName, UserHandle userHandle, List<Map.Entry<String, String>> extras) {
        Intent intent = createConfirmationIntent(componentName, extras);
        try {
            if (this.mContext.startServiceAsUser(intent, userHandle) != null) {
                return true;
            }
            return false;
        } catch (SecurityException e) {
            Slog.e(TAG, "unable to start adb whitelist service: " + componentName, e);
            return false;
        }
    }

    private Intent createConfirmationIntent(ComponentName componentName, List<Map.Entry<String, String>> extras) {
        Intent intent = new Intent();
        intent.setClassName(componentName.getPackageName(), componentName.getClassName());
        for (Map.Entry<String, String> entry : extras) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        return intent;
    }

    private static File getAdbFile(String fileName) {
        File dataDir = Environment.getDataDirectory();
        File adbDir = new File(dataDir, ADB_DIRECTORY);
        if (!adbDir.exists()) {
            Slog.e(TAG, "ADB data directory does not exist");
            return null;
        }
        return new File(adbDir, fileName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getAdbTempKeysFile() {
        return this.mTempKeysFile;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getUserKeyFile() {
        return this.mUserKeyFile;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeKeys(Iterable<String> keys) {
        if (this.mUserKeyFile == null) {
            return;
        }
        AtomicFile atomicKeyFile = new AtomicFile(this.mUserKeyFile);
        FileOutputStream fo = null;
        try {
            fo = atomicKeyFile.startWrite();
            for (String key : keys) {
                fo.write(key.getBytes());
                fo.write(10);
            }
            atomicKeyFile.finishWrite(fo);
            FileUtils.setPermissions(this.mUserKeyFile.toString(), FrameworkStatsLog.DISPLAY_HBM_STATE_CHANGED, -1, -1);
        } catch (IOException ex) {
            Slog.e(TAG, "Error writing keys: " + ex);
            atomicKeyFile.failWrite(fo);
        }
    }

    public void setAdbEnabled(boolean enabled, byte transportType) {
        int i = 1;
        if (transportType == 0) {
            AdbDebuggingHandler adbDebuggingHandler = this.mHandler;
            if (!enabled) {
                i = 2;
            }
            adbDebuggingHandler.sendEmptyMessage(i);
        } else if (transportType == 1) {
            this.mHandler.sendEmptyMessage(enabled ? 11 : 12);
        } else {
            throw new IllegalArgumentException("setAdbEnabled called with unimplemented transport type=" + ((int) transportType));
        }
    }

    public void allowDebugging(boolean alwaysAllow, String publicKey) {
        Message msg = this.mHandler.obtainMessage(3);
        msg.arg1 = alwaysAllow ? 1 : 0;
        msg.obj = publicKey;
        this.mHandler.sendMessage(msg);
    }

    public void denyDebugging() {
        this.mHandler.sendEmptyMessage(4);
    }

    public void clearDebuggingKeys() {
        this.mHandler.sendEmptyMessage(6);
    }

    public void allowWirelessDebugging(boolean alwaysAllow, String bssid) {
        Message msg = this.mHandler.obtainMessage(18);
        msg.arg1 = alwaysAllow ? 1 : 0;
        msg.obj = bssid;
        this.mHandler.sendMessage(msg);
    }

    public void denyWirelessDebugging() {
        this.mHandler.sendEmptyMessage(19);
    }

    public int getAdbWirelessPort() {
        AdbConnectionInfo info = getAdbConnectionInfo();
        if (info == null) {
            return 0;
        }
        return info.getPort();
    }

    public Map<String, PairDevice> getPairedDevices() {
        AdbKeyStore keystore = new AdbKeyStore();
        return keystore.getPairedDevices();
    }

    public void unpairDevice(String fingerprint) {
        Message message = Message.obtain(this.mHandler, 17, fingerprint);
        this.mHandler.sendMessage(message);
    }

    public void enablePairingByPairingCode() {
        this.mHandler.sendEmptyMessage(15);
    }

    public void enablePairingByQrCode(String serviceName, String password) {
        Bundle bundle = new Bundle();
        bundle.putString("serviceName", serviceName);
        bundle.putString("password", password);
        Message message = Message.obtain(this.mHandler, 16, bundle);
        this.mHandler.sendMessage(message);
    }

    public void disablePairing() {
        this.mHandler.sendEmptyMessage(14);
    }

    public boolean isAdbWifiEnabled() {
        return this.mAdbWifiEnabled;
    }

    public void notifyKeyFilesUpdated() {
        this.mHandler.sendEmptyMessage(28);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendPersistKeyStoreMessage() {
        Message msg = this.mHandler.obtainMessage(8);
        this.mHandler.sendMessage(msg);
    }

    public void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        dump.write("connected_to_adb", 1133871366145L, this.mThread != null);
        DumpUtils.writeStringIfNotNull(dump, "last_key_received", 1138166333442L, this.mFingerprints);
        try {
            dump.write("user_keys", 1138166333443L, FileUtils.readTextFile(new File("/data/misc/adb/adb_keys"), 0, null));
        } catch (IOException e) {
            Slog.i(TAG, "Cannot read user keys", e);
        }
        try {
            dump.write("system_keys", 1138166333444L, FileUtils.readTextFile(new File("/adb_keys"), 0, null));
        } catch (IOException e2) {
            Slog.i(TAG, "Cannot read system keys", e2);
        }
        try {
            dump.write("keystore", 1138166333445L, FileUtils.readTextFile(this.mTempKeysFile, 0, null));
        } catch (IOException e3) {
            Slog.i(TAG, "Cannot read keystore: ", e3);
        }
        dump.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AdbKeyStore {
        private static final int KEYSTORE_VERSION = 1;
        private static final int MAX_SUPPORTED_KEYSTORE_VERSION = 1;
        public static final long NO_PREVIOUS_CONNECTION = 0;
        private static final String SYSTEM_KEY_FILE = "/adb_keys";
        private static final String XML_ATTRIBUTE_KEY = "key";
        private static final String XML_ATTRIBUTE_LAST_CONNECTION = "lastConnection";
        private static final String XML_ATTRIBUTE_VERSION = "version";
        private static final String XML_ATTRIBUTE_WIFI_BSSID = "bssid";
        private static final String XML_KEYSTORE_START_TAG = "keyStore";
        private static final String XML_TAG_ADB_KEY = "adbKey";
        private static final String XML_TAG_WIFI_ACCESS_POINT = "wifiAP";
        private AtomicFile mAtomicKeyFile;
        private final Set<String> mSystemKeys;
        private final Map<String, Long> mKeyMap = new HashMap();
        private final List<String> mTrustedNetworks = new ArrayList();

        AdbKeyStore() {
            initKeyFile();
            readTempKeysFile();
            this.mSystemKeys = getSystemKeysFromFile(SYSTEM_KEY_FILE);
            addExistingUserKeysToKeyStore();
        }

        public void reloadKeyMap() {
            readTempKeysFile();
        }

        public void addTrustedNetwork(String bssid) {
            this.mTrustedNetworks.add(bssid);
            AdbDebuggingManager.this.sendPersistKeyStoreMessage();
        }

        public Map<String, PairDevice> getPairedDevices() {
            Map<String, PairDevice> pairedDevices = new HashMap<>();
            for (Map.Entry<String, Long> keyEntry : this.mKeyMap.entrySet()) {
                String fingerprints = AdbDebuggingManager.this.getFingerprints(keyEntry.getKey());
                String hostname = "nouser@nohostname";
                String[] args = keyEntry.getKey().split("\\s+");
                if (args.length > 1) {
                    hostname = args[1];
                }
                PairDevice pairDevice = new PairDevice();
                pairDevice.name = hostname;
                pairDevice.guid = fingerprints;
                pairDevice.connected = AdbDebuggingManager.this.mWifiConnectedKeys.contains(keyEntry.getKey());
                pairedDevices.put(keyEntry.getKey(), pairDevice);
            }
            return pairedDevices;
        }

        public String findKeyFromFingerprint(String fingerprint) {
            for (Map.Entry<String, Long> entry : this.mKeyMap.entrySet()) {
                String f = AdbDebuggingManager.this.getFingerprints(entry.getKey());
                if (fingerprint.equals(f)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        public void removeKey(String key) {
            if (this.mKeyMap.containsKey(key)) {
                this.mKeyMap.remove(key);
                AdbDebuggingManager.this.sendPersistKeyStoreMessage();
            }
        }

        private void initKeyFile() {
            if (AdbDebuggingManager.this.mTempKeysFile != null) {
                this.mAtomicKeyFile = new AtomicFile(AdbDebuggingManager.this.mTempKeysFile);
            }
        }

        private Set<String> getSystemKeysFromFile(String fileName) {
            Set<String> systemKeys = new HashSet<>();
            File systemKeyFile = new File(fileName);
            if (systemKeyFile.exists()) {
                try {
                    BufferedReader in = new BufferedReader(new FileReader(systemKeyFile));
                    while (true) {
                        String key = in.readLine();
                        if (key == null) {
                            break;
                        }
                        String key2 = key.trim();
                        if (key2.length() > 0) {
                            systemKeys.add(key2);
                        }
                    }
                    in.close();
                } catch (IOException e) {
                    Slog.e(AdbDebuggingManager.TAG, "Caught an exception reading " + fileName + ": " + e);
                }
            }
            return systemKeys;
        }

        public boolean isEmpty() {
            return this.mKeyMap.isEmpty();
        }

        public void updateKeyStore() {
            if (filterOutOldKeys()) {
                AdbDebuggingManager.this.sendPersistKeyStoreMessage();
            }
        }

        private void readTempKeysFile() {
            TypedXmlPullParser parser;
            this.mKeyMap.clear();
            this.mTrustedNetworks.clear();
            if (this.mAtomicKeyFile == null) {
                initKeyFile();
                if (this.mAtomicKeyFile == null) {
                    Slog.e(AdbDebuggingManager.TAG, "Unable to obtain the key file, " + AdbDebuggingManager.this.mTempKeysFile + ", for reading");
                    return;
                }
            }
            if (!this.mAtomicKeyFile.exists()) {
                return;
            }
            try {
                FileInputStream keyStream = this.mAtomicKeyFile.openRead();
                try {
                    try {
                        parser = Xml.resolvePullParser(keyStream);
                        XmlUtils.beginDocument(parser, XML_KEYSTORE_START_TAG);
                        int keystoreVersion = parser.getAttributeInt((String) null, XML_ATTRIBUTE_VERSION);
                        if (keystoreVersion > 1) {
                            Slog.e(AdbDebuggingManager.TAG, "Keystore version=" + keystoreVersion + " not supported (max_supported=1)");
                            if (keyStream != null) {
                                keyStream.close();
                                return;
                            }
                            return;
                        }
                    } catch (Throwable th) {
                        if (keyStream != null) {
                            try {
                                keyStream.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                        }
                        throw th;
                    }
                } catch (XmlPullParserException e) {
                    parser = Xml.resolvePullParser(keyStream);
                }
                readKeyStoreContents(parser);
                if (keyStream != null) {
                    keyStream.close();
                }
            } catch (IOException e2) {
                Slog.e(AdbDebuggingManager.TAG, "Caught an IOException parsing the XML key file: ", e2);
            } catch (XmlPullParserException e3) {
                Slog.e(AdbDebuggingManager.TAG, "Caught XmlPullParserException parsing the XML key file: ", e3);
            }
        }

        private void readKeyStoreContents(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
            while (parser.next() != 1) {
                String tagName = parser.getName();
                if (XML_TAG_ADB_KEY.equals(tagName)) {
                    addAdbKeyToKeyMap(parser);
                } else if (XML_TAG_WIFI_ACCESS_POINT.equals(tagName)) {
                    addTrustedNetworkToTrustedNetworks(parser);
                } else {
                    Slog.w(AdbDebuggingManager.TAG, "Ignoring tag '" + tagName + "'. Not recognized.");
                }
                XmlUtils.skipCurrentTag(parser);
            }
        }

        private void addAdbKeyToKeyMap(TypedXmlPullParser parser) {
            String key = parser.getAttributeValue((String) null, XML_ATTRIBUTE_KEY);
            try {
                long connectionTime = parser.getAttributeLong((String) null, XML_ATTRIBUTE_LAST_CONNECTION);
                this.mKeyMap.put(key, Long.valueOf(connectionTime));
            } catch (XmlPullParserException e) {
                Slog.e(AdbDebuggingManager.TAG, "Error reading adbKey attributes", e);
            }
        }

        private void addTrustedNetworkToTrustedNetworks(TypedXmlPullParser parser) {
            String bssid = parser.getAttributeValue((String) null, XML_ATTRIBUTE_WIFI_BSSID);
            this.mTrustedNetworks.add(bssid);
        }

        private void addExistingUserKeysToKeyStore() {
            if (AdbDebuggingManager.this.mUserKeyFile == null || !AdbDebuggingManager.this.mUserKeyFile.exists()) {
                return;
            }
            boolean mapUpdated = false;
            try {
                BufferedReader in = new BufferedReader(new FileReader(AdbDebuggingManager.this.mUserKeyFile));
                while (true) {
                    String key = in.readLine();
                    if (key == null) {
                        break;
                    } else if (!this.mKeyMap.containsKey(key)) {
                        this.mKeyMap.put(key, Long.valueOf(AdbDebuggingManager.this.mTicker.currentTimeMillis()));
                        mapUpdated = true;
                    }
                }
                in.close();
            } catch (IOException e) {
                Slog.e(AdbDebuggingManager.TAG, "Caught an exception reading " + AdbDebuggingManager.this.mUserKeyFile + ": " + e);
            }
            if (mapUpdated) {
                AdbDebuggingManager.this.sendPersistKeyStoreMessage();
            }
        }

        public void persistKeyStore() {
            filterOutOldKeys();
            if (this.mKeyMap.isEmpty() && this.mTrustedNetworks.isEmpty()) {
                deleteKeyStore();
                return;
            }
            if (this.mAtomicKeyFile == null) {
                initKeyFile();
                if (this.mAtomicKeyFile == null) {
                    Slog.e(AdbDebuggingManager.TAG, "Unable to obtain the key file, " + AdbDebuggingManager.this.mTempKeysFile + ", for writing");
                    return;
                }
            }
            FileOutputStream keyStream = null;
            try {
                keyStream = this.mAtomicKeyFile.startWrite();
                TypedXmlSerializer serializer = Xml.resolveSerializer(keyStream);
                serializer.startDocument((String) null, true);
                serializer.startTag((String) null, XML_KEYSTORE_START_TAG);
                serializer.attributeInt((String) null, XML_ATTRIBUTE_VERSION, 1);
                for (Map.Entry<String, Long> keyEntry : this.mKeyMap.entrySet()) {
                    serializer.startTag((String) null, XML_TAG_ADB_KEY);
                    serializer.attribute((String) null, XML_ATTRIBUTE_KEY, keyEntry.getKey());
                    serializer.attributeLong((String) null, XML_ATTRIBUTE_LAST_CONNECTION, keyEntry.getValue().longValue());
                    serializer.endTag((String) null, XML_TAG_ADB_KEY);
                }
                for (String bssid : this.mTrustedNetworks) {
                    serializer.startTag((String) null, XML_TAG_WIFI_ACCESS_POINT);
                    serializer.attribute((String) null, XML_ATTRIBUTE_WIFI_BSSID, bssid);
                    serializer.endTag((String) null, XML_TAG_WIFI_ACCESS_POINT);
                }
                serializer.endTag((String) null, XML_KEYSTORE_START_TAG);
                serializer.endDocument();
                this.mAtomicKeyFile.finishWrite(keyStream);
            } catch (IOException e) {
                Slog.e(AdbDebuggingManager.TAG, "Caught an exception writing the key map: ", e);
                this.mAtomicKeyFile.failWrite(keyStream);
            }
            AdbDebuggingManager.this.writeKeys(this.mKeyMap.keySet());
        }

        private boolean filterOutOldKeys() {
            long allowedTime = getAllowedConnectionTime();
            if (allowedTime == 0) {
                return false;
            }
            boolean keysDeleted = false;
            long systemTime = AdbDebuggingManager.this.mTicker.currentTimeMillis();
            Iterator<Map.Entry<String, Long>> keyMapIterator = this.mKeyMap.entrySet().iterator();
            while (keyMapIterator.hasNext()) {
                Map.Entry<String, Long> keyEntry = keyMapIterator.next();
                long connectionTime = keyEntry.getValue().longValue();
                if (systemTime > connectionTime + allowedTime) {
                    keyMapIterator.remove();
                    keysDeleted = true;
                }
            }
            if (keysDeleted) {
                AdbDebuggingManager.this.writeKeys(this.mKeyMap.keySet());
            }
            return keysDeleted;
        }

        public long getNextExpirationTime() {
            long minExpiration = -1;
            long allowedTime = getAllowedConnectionTime();
            if (allowedTime == 0) {
                return -1L;
            }
            long systemTime = AdbDebuggingManager.this.mTicker.currentTimeMillis();
            for (Map.Entry<String, Long> keyEntry : this.mKeyMap.entrySet()) {
                long connectionTime = keyEntry.getValue().longValue();
                long keyExpiration = Math.max(0L, (connectionTime + allowedTime) - systemTime);
                if (minExpiration == -1 || keyExpiration < minExpiration) {
                    minExpiration = keyExpiration;
                }
            }
            return minExpiration;
        }

        public void deleteKeyStore() {
            this.mKeyMap.clear();
            this.mTrustedNetworks.clear();
            if (AdbDebuggingManager.this.mUserKeyFile != null) {
                AdbDebuggingManager.this.mUserKeyFile.delete();
            }
            AtomicFile atomicFile = this.mAtomicKeyFile;
            if (atomicFile == null) {
                return;
            }
            atomicFile.delete();
        }

        public long getLastConnectionTime(String key) {
            return this.mKeyMap.getOrDefault(key, 0L).longValue();
        }

        public void setLastConnectionTime(String key, long connectionTime) {
            setLastConnectionTime(key, connectionTime, false);
        }

        void setLastConnectionTime(String key, long connectionTime, boolean force) {
            if ((this.mKeyMap.containsKey(key) && this.mKeyMap.get(key).longValue() >= connectionTime && !force) || this.mSystemKeys.contains(key)) {
                return;
            }
            this.mKeyMap.put(key, Long.valueOf(connectionTime));
        }

        public long getAllowedConnectionTime() {
            return Settings.Global.getLong(AdbDebuggingManager.this.mContext.getContentResolver(), "adb_allowed_connection_time", UnixCalendar.WEEK_IN_MILLIS);
        }

        public boolean isKeyAuthorized(String key) {
            if (this.mSystemKeys.contains(key)) {
                return true;
            }
            long lastConnectionTime = getLastConnectionTime(key);
            if (lastConnectionTime == 0) {
                return false;
            }
            long allowedConnectionTime = getAllowedConnectionTime();
            return allowedConnectionTime == 0 || AdbDebuggingManager.this.mTicker.currentTimeMillis() < lastConnectionTime + allowedConnectionTime;
        }

        public boolean isTrustedNetwork(String bssid) {
            return this.mTrustedNetworks.contains(bssid);
        }
    }
}
