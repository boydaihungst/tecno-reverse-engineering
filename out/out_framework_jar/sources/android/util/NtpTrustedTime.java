package android.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.SntpClient;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import com.android.internal.R;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
/* loaded from: classes3.dex */
public class NtpTrustedTime implements TrustedTime {
    private static final boolean LOGD = false;
    private static final String TAG = "NtpTrustedTime";
    private static NtpTrustedTime sSingleton;
    private final Supplier<ConnectivityManager> mConnectivityManagerSupplier = new Supplier<ConnectivityManager>() { // from class: android.util.NtpTrustedTime.1
        private ConnectivityManager mConnectivityManager;

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // java.util.function.Supplier
        public synchronized ConnectivityManager get() {
            if (this.mConnectivityManager == null) {
                this.mConnectivityManager = (ConnectivityManager) NtpTrustedTime.this.mContext.getSystemService(ConnectivityManager.class);
            }
            return this.mConnectivityManager;
        }
    };
    private final Context mContext;
    private String mHostnameForTests;
    private Integer mPortForTests;
    private volatile TimeResult mTimeResult;
    private java.time.Duration mTimeoutForTests;

    /* loaded from: classes3.dex */
    public static class TimeResult {
        private final long mCertaintyMillis;
        private final long mElapsedRealtimeMillis;
        private final long mTimeMillis;

        public TimeResult(long timeMillis, long elapsedRealtimeMillis, long certaintyMillis) {
            this.mTimeMillis = timeMillis;
            this.mElapsedRealtimeMillis = elapsedRealtimeMillis;
            this.mCertaintyMillis = certaintyMillis;
        }

        public long getTimeMillis() {
            return this.mTimeMillis;
        }

        public long getElapsedRealtimeMillis() {
            return this.mElapsedRealtimeMillis;
        }

        public long getCertaintyMillis() {
            return this.mCertaintyMillis;
        }

        public long currentTimeMillis() {
            return this.mTimeMillis + getAgeMillis();
        }

        public long getAgeMillis() {
            return getAgeMillis(SystemClock.elapsedRealtime());
        }

        public long getAgeMillis(long currentElapsedRealtimeMillis) {
            return currentElapsedRealtimeMillis - this.mElapsedRealtimeMillis;
        }

        public String toString() {
            return "TimeResult{mTimeMillis=" + Instant.ofEpochMilli(this.mTimeMillis) + ", mElapsedRealtimeMillis=" + java.time.Duration.ofMillis(this.mElapsedRealtimeMillis) + ", mCertaintyMillis=" + this.mCertaintyMillis + '}';
        }
    }

    private NtpTrustedTime(Context context) {
        this.mContext = (Context) Objects.requireNonNull(context);
    }

    public static synchronized NtpTrustedTime getInstance(Context context) {
        NtpTrustedTime ntpTrustedTime;
        synchronized (NtpTrustedTime.class) {
            if (sSingleton == null) {
                Context appContext = context.getApplicationContext();
                sSingleton = new NtpTrustedTime(appContext);
            }
            ntpTrustedTime = sSingleton;
        }
        return ntpTrustedTime;
    }

    public void setServerConfigForTests(String hostname, Integer port, java.time.Duration timeout) {
        synchronized (this) {
            this.mHostnameForTests = hostname;
            this.mPortForTests = port;
            this.mTimeoutForTests = timeout;
        }
    }

    @Override // android.util.TrustedTime
    public boolean forceRefresh() {
        synchronized (this) {
            NtpConnectionInfo connectionInfo = getNtpConnectionInfo();
            if (connectionInfo == null) {
                return false;
            }
            ConnectivityManager connectivityManager = this.mConnectivityManagerSupplier.get();
            if (connectivityManager == null) {
                return false;
            }
            Network network = connectivityManager.getActiveNetwork();
            NetworkInfo ni = connectivityManager.getNetworkInfo(network);
            if (ni != null && ni.isConnected()) {
                SntpClient client = new SntpClient();
                String serverName = connectionInfo.getServer();
                int port = connectionInfo.getPort();
                int timeoutMillis = connectionInfo.getTimeoutMillis();
                if (!client.requestTime(serverName, port, timeoutMillis, network)) {
                    return false;
                }
                long ntpCertainty = client.getRoundTripTime() / 2;
                this.mTimeResult = new TimeResult(client.getNtpTime(), client.getNtpTimeReference(), ntpCertainty);
                return true;
            }
            return false;
        }
    }

    @Override // android.util.TrustedTime
    @Deprecated
    public boolean hasCache() {
        return this.mTimeResult != null;
    }

    @Override // android.util.TrustedTime
    @Deprecated
    public long getCacheAge() {
        TimeResult timeResult = this.mTimeResult;
        if (timeResult != null) {
            return SystemClock.elapsedRealtime() - timeResult.getElapsedRealtimeMillis();
        }
        return Long.MAX_VALUE;
    }

    @Override // android.util.TrustedTime
    @Deprecated
    public long currentTimeMillis() {
        TimeResult timeResult = this.mTimeResult;
        if (timeResult == null) {
            throw new IllegalStateException("Missing authoritative time source");
        }
        return timeResult.currentTimeMillis();
    }

    @Deprecated
    public long getCachedNtpTime() {
        TimeResult timeResult = this.mTimeResult;
        if (timeResult == null) {
            return 0L;
        }
        return timeResult.getTimeMillis();
    }

    @Deprecated
    public long getCachedNtpTimeReference() {
        TimeResult timeResult = this.mTimeResult;
        if (timeResult == null) {
            return 0L;
        }
        return timeResult.getElapsedRealtimeMillis();
    }

    public TimeResult getCachedTimeResult() {
        return this.mTimeResult;
    }

    public void clearCachedTimeResult() {
        synchronized (this) {
            this.mTimeResult = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class NtpConnectionInfo {
        private final int mPort;
        private final String mServer;
        private final int mTimeoutMillis;

        NtpConnectionInfo(String server, int port, int timeoutMillis) {
            this.mServer = (String) Objects.requireNonNull(server);
            this.mPort = port;
            this.mTimeoutMillis = timeoutMillis;
        }

        public String getServer() {
            return this.mServer;
        }

        public int getPort() {
            return this.mPort;
        }

        int getTimeoutMillis() {
            return this.mTimeoutMillis;
        }

        public String toString() {
            return "NtpConnectionInfo{mServer='" + this.mServer + DateFormat.QUOTE + ", mPort='" + this.mPort + DateFormat.QUOTE + ", mTimeoutMillis=" + this.mTimeoutMillis + '}';
        }
    }

    private NtpConnectionInfo getNtpConnectionInfo() {
        String serverGlobalSetting;
        Integer port;
        int defaultTimeoutMillis;
        ContentResolver resolver = this.mContext.getContentResolver();
        Resources res = this.mContext.getResources();
        if (this.mHostnameForTests != null) {
            serverGlobalSetting = this.mHostnameForTests;
        } else {
            serverGlobalSetting = Settings.Global.getString(resolver, Settings.Global.NTP_SERVER);
            if (serverGlobalSetting == null) {
                serverGlobalSetting = res.getString(R.string.config_ntpServer);
            }
        }
        if (this.mPortForTests != null) {
            port = this.mPortForTests;
        } else {
            port = 123;
        }
        java.time.Duration duration = this.mTimeoutForTests;
        if (duration != null) {
            defaultTimeoutMillis = (int) duration.toMillis();
        } else {
            int defaultTimeoutMillis2 = res.getInteger(R.integer.config_ntpTimeout);
            defaultTimeoutMillis = Settings.Global.getInt(resolver, Settings.Global.NTP_TIMEOUT, defaultTimeoutMillis2);
        }
        if (TextUtils.isEmpty(serverGlobalSetting)) {
            return null;
        }
        return new NtpConnectionInfo(serverGlobalSetting, port.intValue(), defaultTimeoutMillis);
    }

    public void dump(PrintWriter pw) {
        synchronized (this) {
            pw.println("getNtpConnectionInfo()=" + getNtpConnectionInfo());
            pw.println("mTimeResult=" + this.mTimeResult);
            if (this.mTimeResult != null) {
                pw.println("mTimeResult.getAgeMillis()=" + java.time.Duration.ofMillis(this.mTimeResult.getAgeMillis()));
            }
        }
    }
}
