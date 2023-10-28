package com.android.server;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.timedetector.NetworkTimeSuggestion;
import android.app.timedetector.TimeDetector;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.TimestampedValue;
import android.provider.Settings;
import android.util.LocalLog;
import android.util.Log;
import android.util.NtpTrustedTime;
import com.android.internal.util.DumpUtils;
import com.transsion.hubcore.server.net.ITranNetworkTimeUpdateService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.Duration;
/* loaded from: classes.dex */
public class NetworkTimeUpdateService extends Binder {
    private static final String ACTION_POLL = "com.android.server.NetworkTimeUpdateService.action.POLL";
    private static final boolean DBG = false;
    private static final int EVENT_AUTO_TIME_ENABLED = 1;
    private static final int EVENT_NETWORK_CHANGED = 3;
    private static final int EVENT_POLL_NETWORK_TIME = 2;
    private static final int POLL_REQUEST = 0;
    private static final String TAG = "NetworkTimeUpdateService";
    private final AlarmManager mAlarmManager;
    private AutoTimeSettingObserver mAutoTimeSettingObserver;
    private final ConnectivityManager mCM;
    private final Context mContext;
    private Handler mHandler;
    private NetworkTimeUpdateCallback mNetworkTimeUpdateCallback;
    private final PendingIntent mPendingPollIntent;
    private final long mPollingIntervalMs;
    private final long mPollingIntervalShorterMs;
    private final NtpTrustedTime mTime;
    private final TimeDetector mTimeDetector;
    private final int mTryAgainTimesMax;
    private final PowerManager.WakeLock mWakeLock;
    private Network mDefaultNetwork = null;
    private int mTryAgainCounter = 0;
    private final LocalLog mLocalLog = new LocalLog(30, false);

    public NetworkTimeUpdateService(Context context) {
        this.mContext = context;
        this.mTime = NtpTrustedTime.getInstance(context);
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        this.mTimeDetector = (TimeDetector) context.getSystemService(TimeDetector.class);
        this.mCM = (ConnectivityManager) context.getSystemService(ConnectivityManager.class);
        Intent pollIntent = new Intent(ACTION_POLL, (Uri) null);
        this.mPendingPollIntent = PendingIntent.getBroadcast(context, 0, pollIntent, 67108864);
        this.mPollingIntervalMs = context.getResources().getInteger(17694904);
        this.mPollingIntervalShorterMs = context.getResources().getInteger(17694905);
        this.mTryAgainTimesMax = context.getResources().getInteger(17694906);
        this.mWakeLock = ((PowerManager) context.getSystemService(PowerManager.class)).newWakeLock(1, TAG);
    }

    public void systemRunning() {
        registerForAlarms();
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mHandler = new MyHandler(thread.getLooper());
        NetworkTimeUpdateCallback networkTimeUpdateCallback = new NetworkTimeUpdateCallback();
        this.mNetworkTimeUpdateCallback = networkTimeUpdateCallback;
        this.mCM.registerDefaultNetworkCallback(networkTimeUpdateCallback, this.mHandler);
        AutoTimeSettingObserver autoTimeSettingObserver = new AutoTimeSettingObserver(this.mContext, this.mHandler, 1);
        this.mAutoTimeSettingObserver = autoTimeSettingObserver;
        autoTimeSettingObserver.observe();
    }

    private void registerForAlarms() {
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.NetworkTimeUpdateService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                NetworkTimeUpdateService.this.mHandler.obtainMessage(2).sendToTarget();
            }
        }, new IntentFilter(ACTION_POLL));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearTimeForTests() {
        this.mContext.enforceCallingPermission("android.permission.SET_TIME", "clear latest network time");
        this.mTime.clearCachedTimeResult();
        this.mLocalLog.log("clearTimeForTests");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean forceRefreshForTests() {
        this.mContext.enforceCallingPermission("android.permission.SET_TIME", "force network time refresh");
        boolean success = this.mTime.forceRefresh();
        this.mLocalLog.log("forceRefreshForTests: success=" + success);
        if (success) {
            makeNetworkTimeSuggestion(this.mTime.getCachedTimeResult(), "Origin: NetworkTimeUpdateService: forceRefreshForTests");
        }
        return success;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setServerConfigForTests(String hostname, Integer port, Duration timeout) {
        this.mContext.enforceCallingPermission("android.permission.SET_TIME", "set NTP server config for tests");
        this.mLocalLog.log("Setting server config for tests: hostname=" + hostname + ", port=" + port + ", timeout=" + timeout);
        this.mTime.setServerConfigForTests(hostname, port, timeout);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPollNetworkTime(int event) {
        if (this.mDefaultNetwork == null) {
            return;
        }
        this.mWakeLock.acquire();
        try {
            onPollNetworkTimeUnderWakeLock(event);
        } finally {
            this.mWakeLock.release();
        }
    }

    private void onPollNetworkTimeUnderWakeLock(int event) {
        long currentElapsedRealtimeMillis = SystemClock.elapsedRealtime();
        NtpTrustedTime.TimeResult cachedNtpResult = this.mTime.getCachedTimeResult();
        if (cachedNtpResult == null || cachedNtpResult.getAgeMillis(currentElapsedRealtimeMillis) >= this.mPollingIntervalMs) {
            boolean isSuccessful = ITranNetworkTimeUpdateService.Instance().forceRefresh(this.mContext, this.mTime, this.mTryAgainCounter);
            if (isSuccessful) {
                this.mTryAgainCounter = 0;
            } else {
                String logMsg = "forceRefresh() returned false: cachedNtpResult=" + cachedNtpResult + ", currentElapsedRealtimeMillis=" + currentElapsedRealtimeMillis;
                this.mLocalLog.log(logMsg);
            }
            cachedNtpResult = this.mTime.getCachedTimeResult();
        }
        if (cachedNtpResult != null) {
            long ageMillis = cachedNtpResult.getAgeMillis(currentElapsedRealtimeMillis);
            long j = this.mPollingIntervalMs;
            if (ageMillis < j) {
                resetAlarm(j - cachedNtpResult.getAgeMillis(currentElapsedRealtimeMillis));
                makeNetworkTimeSuggestion(cachedNtpResult, "Origin: NetworkTimeUpdateService. event=" + event);
                return;
            }
        }
        int i = this.mTryAgainCounter + 1;
        this.mTryAgainCounter = i;
        int i2 = this.mTryAgainTimesMax;
        if (i2 < 0 || i <= i2) {
            resetAlarm(this.mPollingIntervalShorterMs);
            return;
        }
        String logMsg2 = "mTryAgainTimesMax exceeded, cachedNtpResult=" + cachedNtpResult;
        this.mLocalLog.log(logMsg2);
        this.mTryAgainCounter = 0;
        resetAlarm(this.mPollingIntervalMs);
    }

    private void makeNetworkTimeSuggestion(NtpTrustedTime.TimeResult ntpResult, String debugInfo) {
        TimestampedValue<Long> timeSignal = new TimestampedValue<>(ntpResult.getElapsedRealtimeMillis(), Long.valueOf(ntpResult.getTimeMillis()));
        NetworkTimeSuggestion timeSuggestion = new NetworkTimeSuggestion(timeSignal);
        timeSuggestion.addDebugInfo(new String[]{debugInfo});
        this.mTimeDetector.suggestNetworkTime(timeSuggestion);
    }

    private void resetAlarm(long interval) {
        this.mAlarmManager.cancel(this.mPendingPollIntent);
        long now = SystemClock.elapsedRealtime();
        long next = now + interval;
        this.mAlarmManager.set(3, next, this.mPendingPollIntent);
    }

    /* loaded from: classes.dex */
    private class MyHandler extends Handler {
        MyHandler(Looper l) {
            super(l);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                case 2:
                case 3:
                    NetworkTimeUpdateService.this.onPollNetworkTime(msg.what);
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    private class NetworkTimeUpdateCallback extends ConnectivityManager.NetworkCallback {
        private NetworkTimeUpdateCallback() {
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onAvailable(Network network) {
            Log.d(NetworkTimeUpdateService.TAG, String.format("New default network %s; checking time.", network));
            NetworkTimeUpdateService.this.mDefaultNetwork = network;
            NetworkTimeUpdateService.this.onPollNetworkTime(3);
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            if (network.equals(NetworkTimeUpdateService.this.mDefaultNetwork)) {
                NetworkTimeUpdateService.this.mDefaultNetwork = null;
            }
        }
    }

    /* loaded from: classes.dex */
    private static class AutoTimeSettingObserver extends ContentObserver {
        private final Context mContext;
        private final Handler mHandler;
        private final int mMsg;

        AutoTimeSettingObserver(Context context, Handler handler, int msg) {
            super(handler);
            this.mContext = context;
            this.mHandler = handler;
            this.mMsg = msg;
        }

        void observe() {
            ContentResolver resolver = this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Global.getUriFor("auto_time"), false, this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            if (isAutomaticTimeEnabled()) {
                this.mHandler.obtainMessage(this.mMsg).sendToTarget();
            }
        }

        private boolean isAutomaticTimeEnabled() {
            ContentResolver resolver = this.mContext.getContentResolver();
            return Settings.Global.getInt(resolver, "auto_time", 0) != 0;
        }
    }

    @Override // android.os.Binder
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("mPollingIntervalMs=" + Duration.ofMillis(this.mPollingIntervalMs));
            pw.println("mPollingIntervalShorterMs=" + Duration.ofMillis(this.mPollingIntervalShorterMs));
            pw.println("mTryAgainTimesMax=" + this.mTryAgainTimesMax);
            pw.println("mTryAgainCounter=" + this.mTryAgainCounter);
            pw.println();
            pw.println("NtpTrustedTime:");
            this.mTime.dump(pw);
            pw.println();
            pw.println("Local logs:");
            this.mLocalLog.dump(fd, pw, args);
            pw.println();
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new NetworkTimeUpdateServiceShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }
}
