package com.android.server.connectivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.IPacProxyInstalledListener;
import android.net.IPacProxyManager;
import android.net.Network;
import android.net.ProxyInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.webkit.URLUtil;
import com.android.net.IProxyCallback;
import com.android.net.IProxyPortListener;
import com.android.net.IProxyService;
import com.android.net.module.util.PermissionUtils;
import com.android.server.tare.AlarmManagerEconomicPolicy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
/* loaded from: classes.dex */
public class PacProxyService extends IPacProxyManager.Stub {
    private static final String ACTION_PAC_REFRESH = "android.net.proxy.PAC_REFRESH";
    private static final String DEFAULT_DELAYS = "8 32 120 14400 43200";
    private static final int DELAY_1 = 0;
    private static final int DELAY_4 = 3;
    private static final int DELAY_LONG = 4;
    private static final long MAX_PAC_SIZE = 20000000;
    private static final String PAC_PACKAGE = "com.android.pacprocessor";
    private static final String PAC_SERVICE = "com.android.pacprocessor.PacService";
    private static final String PAC_SERVICE_NAME = "com.android.net.IProxyService";
    private static final String PROXY_PACKAGE = "com.android.proxyhandler";
    private static final String PROXY_SERVICE = "com.android.proxyhandler.ProxyService";
    private static final String TAG = "PacProxyService";
    private AlarmManager mAlarmManager;
    private ServiceConnection mConnection;
    private Context mContext;
    private int mCurrentDelay;
    private String mCurrentPac;
    private volatile boolean mHasDownloaded;
    private volatile boolean mHasSentBroadcast;
    private final Handler mNetThreadHandler;
    private PendingIntent mPacRefreshIntent;
    private ServiceConnection mProxyConnection;
    private IProxyService mProxyService;
    private volatile Uri mPacUrl = Uri.EMPTY;
    private final RemoteCallbackList<IPacProxyInstalledListener> mCallbacks = new RemoteCallbackList<>();
    private final Object mProxyLock = new Object();
    private final Object mBroadcastStateLock = new Object();
    private Runnable mPacDownloader = new Runnable() { // from class: com.android.server.connectivity.PacProxyService.1
        @Override // java.lang.Runnable
        public void run() {
            String file;
            Uri pacUrl = PacProxyService.this.mPacUrl;
            if (Uri.EMPTY.equals(pacUrl)) {
                return;
            }
            int oldTag = TrafficStats.getAndSetThreadStatsTag(-187);
            try {
                try {
                    file = PacProxyService.get(pacUrl);
                } catch (IOException ioe) {
                    Log.w(PacProxyService.TAG, "Failed to load PAC file: " + ioe);
                    TrafficStats.setThreadStatsTag(oldTag);
                    file = null;
                }
                if (file != null) {
                    synchronized (PacProxyService.this.mProxyLock) {
                        if (!file.equals(PacProxyService.this.mCurrentPac)) {
                            PacProxyService.this.setCurrentProxyScript(file);
                        }
                    }
                    PacProxyService.this.mHasDownloaded = true;
                    PacProxyService.this.sendProxyIfNeeded();
                    PacProxyService.this.longSchedule();
                    return;
                }
                PacProxyService.this.reschedule();
            } finally {
                TrafficStats.setThreadStatsTag(oldTag);
            }
        }
    };
    private int mLastPort = -1;

    /* loaded from: classes.dex */
    class PacRefreshIntentReceiver extends BroadcastReceiver {
        PacRefreshIntentReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            PacProxyService.this.mNetThreadHandler.post(PacProxyService.this.mPacDownloader);
        }
    }

    public PacProxyService(Context context) {
        this.mContext = context;
        HandlerThread netThread = new HandlerThread("android.pacproxyservice", 0);
        netThread.start();
        this.mNetThreadHandler = new Handler(netThread.getLooper());
        this.mPacRefreshIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_PAC_REFRESH), 67108864);
        context.registerReceiver(new PacRefreshIntentReceiver(), new IntentFilter(ACTION_PAC_REFRESH));
    }

    private AlarmManager getAlarmManager() {
        if (this.mAlarmManager == null) {
            this.mAlarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        }
        return this.mAlarmManager;
    }

    public void addListener(IPacProxyInstalledListener listener) {
        PermissionUtils.enforceNetworkStackPermissionOr(this.mContext, new String[]{"android.permission.NETWORK_SETTINGS"});
        this.mCallbacks.register(listener);
    }

    public void removeListener(IPacProxyInstalledListener listener) {
        PermissionUtils.enforceNetworkStackPermissionOr(this.mContext, new String[]{"android.permission.NETWORK_SETTINGS"});
        this.mCallbacks.unregister(listener);
    }

    public void setCurrentProxyScriptUrl(ProxyInfo proxy) {
        PermissionUtils.enforceNetworkStackPermissionOr(this.mContext, new String[]{"android.permission.NETWORK_SETTINGS"});
        synchronized (this.mBroadcastStateLock) {
            if (proxy != null) {
                if (!Uri.EMPTY.equals(proxy.getPacFileUrl())) {
                    if (!proxy.getPacFileUrl().equals(this.mPacUrl) || proxy.getPort() <= 0) {
                        this.mPacUrl = proxy.getPacFileUrl();
                        this.mCurrentDelay = 0;
                        this.mHasSentBroadcast = false;
                        this.mHasDownloaded = false;
                        getAlarmManager().cancel(this.mPacRefreshIntent);
                        bind();
                    }
                    return;
                }
            }
            getAlarmManager().cancel(this.mPacRefreshIntent);
            synchronized (this.mProxyLock) {
                this.mPacUrl = Uri.EMPTY;
                this.mCurrentPac = null;
                if (this.mProxyService != null) {
                    unbind();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String get(Uri pacUri) throws IOException {
        if (!URLUtil.isValidUrl(pacUri.toString())) {
            throw new IOException("Malformed URL:" + pacUri);
        }
        URL url = new URL(pacUri.toString());
        try {
            URLConnection urlConnection = url.openConnection(Proxy.NO_PROXY);
            long contentLength = -1;
            try {
                contentLength = Long.parseLong(urlConnection.getHeaderField("Content-Length"));
            } catch (NumberFormatException e) {
            }
            if (contentLength > MAX_PAC_SIZE) {
                throw new IOException("PAC too big: " + contentLength + " bytes");
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            do {
                int count = urlConnection.getInputStream().read(buffer);
                if (count != -1) {
                    bytes.write(buffer, 0, count);
                } else {
                    return bytes.toString();
                }
            } while (bytes.size() <= MAX_PAC_SIZE);
            throw new IOException("PAC too big");
        } catch (IllegalArgumentException e2) {
            throw new IOException("Incorrect proxy type for " + pacUri);
        } catch (UnsupportedOperationException e3) {
            throw new IOException("Unsupported URL connection type for " + pacUri);
        }
    }

    private int getNextDelay(int currentDelay) {
        int currentDelay2 = currentDelay + 1;
        if (currentDelay2 > 3) {
            return 3;
        }
        return currentDelay2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void longSchedule() {
        this.mCurrentDelay = 0;
        setDownloadIn(4);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reschedule() {
        int nextDelay = getNextDelay(this.mCurrentDelay);
        this.mCurrentDelay = nextDelay;
        setDownloadIn(nextDelay);
    }

    private String getPacChangeDelay() {
        ContentResolver cr = this.mContext.getContentResolver();
        String defaultDelay = SystemProperties.get("conn.pac_change_delay", DEFAULT_DELAYS);
        String val = Settings.Global.getString(cr, "pac_change_delay");
        return val == null ? defaultDelay : val;
    }

    private long getDownloadDelay(int delayIndex) {
        String[] list = getPacChangeDelay().split(" ");
        if (delayIndex < list.length) {
            return Long.parseLong(list[delayIndex]);
        }
        return 0L;
    }

    private void setDownloadIn(int delayIndex) {
        long delay = getDownloadDelay(delayIndex);
        long timeTillTrigger = (1000 * delay) + SystemClock.elapsedRealtime();
        getAlarmManager().set(3, timeTillTrigger, this.mPacRefreshIntent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCurrentProxyScript(String script) {
        IProxyService iProxyService = this.mProxyService;
        if (iProxyService == null) {
            Log.e(TAG, "setCurrentProxyScript: no proxy service");
            return;
        }
        try {
            iProxyService.setPacFile(script);
            this.mCurrentPac = script;
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to set PAC file", e);
        }
    }

    private void bind() {
        if (this.mContext == null) {
            Log.e(TAG, "No context for binding");
            return;
        }
        Intent intent = new Intent();
        intent.setClassName(PAC_PACKAGE, PAC_SERVICE);
        if (this.mProxyConnection != null && this.mConnection != null) {
            this.mNetThreadHandler.post(this.mPacDownloader);
            return;
        }
        ServiceConnection serviceConnection = new ServiceConnection() { // from class: com.android.server.connectivity.PacProxyService.2
            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName component) {
                synchronized (PacProxyService.this.mProxyLock) {
                    PacProxyService.this.mProxyService = null;
                }
            }

            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName component, IBinder binder) {
                synchronized (PacProxyService.this.mProxyLock) {
                    try {
                        Log.d(PacProxyService.TAG, "Adding service com.android.net.IProxyService " + binder.getInterfaceDescriptor());
                    } catch (RemoteException e1) {
                        Log.e(PacProxyService.TAG, "Remote Exception", e1);
                    }
                    ServiceManager.addService(PacProxyService.PAC_SERVICE_NAME, binder);
                    PacProxyService.this.mProxyService = IProxyService.Stub.asInterface(binder);
                    if (PacProxyService.this.mProxyService == null) {
                        Log.e(PacProxyService.TAG, "No proxy service");
                    } else if (PacProxyService.this.mCurrentPac != null) {
                        PacProxyService pacProxyService = PacProxyService.this;
                        pacProxyService.setCurrentProxyScript(pacProxyService.mCurrentPac);
                    } else {
                        PacProxyService.this.mNetThreadHandler.post(PacProxyService.this.mPacDownloader);
                    }
                }
            }
        };
        this.mConnection = serviceConnection;
        this.mContext.bindServiceAsUser(intent, serviceConnection, AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_EXACT, UserHandle.SYSTEM);
        Intent intent2 = new Intent();
        intent2.setClassName(PROXY_PACKAGE, PROXY_SERVICE);
        ServiceConnection serviceConnection2 = new ServiceConnection() { // from class: com.android.server.connectivity.PacProxyService.3
            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName component) {
            }

            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName component, IBinder binder) {
                IProxyCallback callbackService = IProxyCallback.Stub.asInterface(binder);
                if (callbackService != null) {
                    try {
                        callbackService.getProxyPort(new IProxyPortListener.Stub() { // from class: com.android.server.connectivity.PacProxyService.3.1
                            public void setProxyPort(int port) {
                                if (PacProxyService.this.mLastPort != -1) {
                                    PacProxyService.this.mHasSentBroadcast = false;
                                }
                                PacProxyService.this.mLastPort = port;
                                if (port != -1) {
                                    Log.d(PacProxyService.TAG, "Local proxy is bound on " + port);
                                    PacProxyService.this.sendProxyIfNeeded();
                                    return;
                                }
                                Log.e(PacProxyService.TAG, "Received invalid port from Local Proxy, PAC will not be operational");
                            }
                        });
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        this.mProxyConnection = serviceConnection2;
        this.mContext.bindServiceAsUser(intent2, serviceConnection2, AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_EXACT, this.mNetThreadHandler, UserHandle.SYSTEM);
    }

    private void unbind() {
        ServiceConnection serviceConnection = this.mConnection;
        if (serviceConnection != null) {
            this.mContext.unbindService(serviceConnection);
            this.mConnection = null;
        }
        ServiceConnection serviceConnection2 = this.mProxyConnection;
        if (serviceConnection2 != null) {
            this.mContext.unbindService(serviceConnection2);
            this.mProxyConnection = null;
        }
        this.mProxyService = null;
        this.mLastPort = -1;
    }

    private void sendPacBroadcast(ProxyInfo proxy) {
        int length = this.mCallbacks.beginBroadcast();
        for (int i = 0; i < length; i++) {
            IPacProxyInstalledListener listener = this.mCallbacks.getBroadcastItem(i);
            if (listener != null) {
                try {
                    listener.onPacProxyInstalled((Network) null, proxy);
                } catch (RemoteException e) {
                }
            }
        }
        this.mCallbacks.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendProxyIfNeeded() {
        synchronized (this.mBroadcastStateLock) {
            if (this.mHasDownloaded && this.mLastPort != -1) {
                if (!this.mHasSentBroadcast) {
                    sendPacBroadcast(ProxyInfo.buildPacProxy(this.mPacUrl, this.mLastPort));
                    this.mHasSentBroadcast = true;
                }
            }
        }
    }
}
