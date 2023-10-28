package android.net.wifi.nl80211;

import android.annotation.SystemApi;
import android.app.AlarmManager;
import android.content.Context;
import android.net.wifi.nl80211.IApInterfaceEventCallback;
import android.net.wifi.nl80211.IPnoScanEvent;
import android.net.wifi.nl80211.IScanEvent;
import android.net.wifi.nl80211.ISendMgmtFrameEvent;
import android.net.wifi.nl80211.IWificond;
import android.net.wifi.nl80211.IWificondEventCallback;
import android.net.wifi.nl80211.WifiNl80211Manager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
@SystemApi
/* loaded from: classes2.dex */
public class WifiNl80211Manager {
    private static final int EID_WAPI = 68;
    public static final String SCANNING_PARAM_ENABLE_6GHZ_RNR = "android.net.wifi.nl80211.SCANNING_PARAM_ENABLE_6GHZ_RNR";
    public static final int SCAN_TYPE_PNO_SCAN = 1;
    public static final int SCAN_TYPE_SINGLE_SCAN = 0;
    public static final int SEND_MGMT_FRAME_ERROR_ALREADY_STARTED = 5;
    public static final int SEND_MGMT_FRAME_ERROR_MCS_UNSUPPORTED = 2;
    public static final int SEND_MGMT_FRAME_ERROR_NO_ACK = 3;
    public static final int SEND_MGMT_FRAME_ERROR_TIMEOUT = 4;
    public static final int SEND_MGMT_FRAME_ERROR_UNKNOWN = 1;
    private static final int SEND_MGMT_FRAME_TIMEOUT_MS = 1000;
    private static final String TAG = "WifiNl80211Manager";
    private static final String TIMEOUT_ALARM_TAG = "WifiNl80211Manager Send Management Frame Timeout";
    private static final int WAPI_AKM_CERT = 24253440;
    private static final int WAPI_AKM_PSK = 41030656;
    private static final int WAPI_CIPHER_SMS4 = 24253440;
    private static final short WAPI_VERSION = 1;
    private AlarmManager mAlarmManager;
    private HashMap<String, IApInterfaceEventCallback> mApInterfaceListeners;
    private HashMap<String, IApInterface> mApInterfaces;
    private HashMap<String, IClientInterface> mClientInterfaces;
    private Runnable mDeathEventHandler;
    private Handler mEventHandler;
    private HashMap<String, IPnoScanEvent> mPnoScanEventHandlers;
    private HashMap<String, IScanEvent> mScanEventHandlers;
    private AtomicBoolean mSendMgmtFrameInProgress;
    private boolean mVerboseLoggingEnabled;
    private IWificond mWificond;
    private WificondEventHandler mWificondEventHandler;
    private HashMap<String, IWifiScannerImpl> mWificondScanners;

    /* loaded from: classes2.dex */
    public interface CountryCodeChangedListener {
        void onCountryCodeChanged(String str);
    }

    /* loaded from: classes2.dex */
    public interface PnoScanRequestCallback {
        void onPnoRequestFailed();

        void onPnoRequestSucceeded();
    }

    /* loaded from: classes2.dex */
    public interface ScanEventCallback {
        void onScanFailed();

        void onScanResultReady();
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ScanResultType {
    }

    /* loaded from: classes2.dex */
    public interface SendMgmtFrameCallback {
        void onAck(int i);

        void onFailure(int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface SendMgmtFrameError {
    }

    @Deprecated
    /* loaded from: classes2.dex */
    public interface SoftApCallback {
        void onConnectedClientsChanged(NativeWifiClient nativeWifiClient, boolean z);

        void onFailure();

        void onSoftApChannelSwitched(int i, int i2);
    }

    /* loaded from: classes2.dex */
    public class WificondEventHandler extends IWificondEventCallback.Stub {
        private Map<CountryCodeChangedListener, Executor> mCountryCodeChangedListenerHolder = new HashMap();

        public WificondEventHandler() {
        }

        public void registerCountryCodeChangedListener(Executor executor, CountryCodeChangedListener listener) {
            this.mCountryCodeChangedListenerHolder.put(listener, executor);
        }

        public void unregisterCountryCodeChangedListener(CountryCodeChangedListener listener) {
            this.mCountryCodeChangedListenerHolder.remove(listener);
        }

        @Override // android.net.wifi.nl80211.IWificondEventCallback
        public void OnRegDomainChanged(final String countryCode) {
            Log.d(WifiNl80211Manager.TAG, "OnRegDomainChanged " + countryCode);
            long token = Binder.clearCallingIdentity();
            try {
                this.mCountryCodeChangedListenerHolder.forEach(new BiConsumer() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$WificondEventHandler$$ExternalSyntheticLambda0
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        Executor executor = (Executor) obj2;
                        executor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$WificondEventHandler$$ExternalSyntheticLambda1
                            @Override // java.lang.Runnable
                            public final void run() {
                                WifiNl80211Manager.CountryCodeChangedListener.this.onCountryCodeChanged(r2);
                            }
                        });
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ScanEventHandler extends IScanEvent.Stub {
        private ScanEventCallback mCallback;
        private Executor mExecutor;

        ScanEventHandler(Executor executor, ScanEventCallback callback) {
            this.mExecutor = executor;
            this.mCallback = callback;
        }

        @Override // android.net.wifi.nl80211.IScanEvent
        public void OnScanResultReady() {
            Log.d(WifiNl80211Manager.TAG, "Scan result ready event");
            long token = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$ScanEventHandler$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        WifiNl80211Manager.ScanEventHandler.this.m2863xa4fe4f9b();
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$OnScanResultReady$0$android-net-wifi-nl80211-WifiNl80211Manager$ScanEventHandler  reason: not valid java name */
        public /* synthetic */ void m2863xa4fe4f9b() {
            this.mCallback.onScanResultReady();
        }

        @Override // android.net.wifi.nl80211.IScanEvent
        public void OnScanFailed() {
            Log.d(WifiNl80211Manager.TAG, "Scan failed event");
            long token = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$ScanEventHandler$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        WifiNl80211Manager.ScanEventHandler.this.m2862xeb2fa111();
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$OnScanFailed$1$android-net-wifi-nl80211-WifiNl80211Manager$ScanEventHandler  reason: not valid java name */
        public /* synthetic */ void m2862xeb2fa111() {
            this.mCallback.onScanFailed();
        }
    }

    /* loaded from: classes2.dex */
    public static class SignalPollResult {
        public final int associationFrequencyMHz;
        public final int currentRssiDbm;
        public final int rxBitrateMbps;
        public final int txBitrateMbps;

        public SignalPollResult(int currentRssiDbm, int txBitrateMbps, int rxBitrateMbps, int associationFrequencyMHz) {
            this.currentRssiDbm = currentRssiDbm;
            this.txBitrateMbps = txBitrateMbps;
            this.rxBitrateMbps = rxBitrateMbps;
            this.associationFrequencyMHz = associationFrequencyMHz;
        }
    }

    /* loaded from: classes2.dex */
    public static class TxPacketCounters {
        public final int txPacketFailed;
        public final int txPacketSucceeded;

        public TxPacketCounters(int txPacketSucceeded, int txPacketFailed) {
            this.txPacketSucceeded = txPacketSucceeded;
            this.txPacketFailed = txPacketFailed;
        }
    }

    public WifiNl80211Manager(Context context) {
        this.mVerboseLoggingEnabled = false;
        this.mWificondEventHandler = new WificondEventHandler();
        this.mClientInterfaces = new HashMap<>();
        this.mApInterfaces = new HashMap<>();
        this.mWificondScanners = new HashMap<>();
        this.mScanEventHandlers = new HashMap<>();
        this.mPnoScanEventHandlers = new HashMap<>();
        this.mApInterfaceListeners = new HashMap<>();
        this.mSendMgmtFrameInProgress = new AtomicBoolean(false);
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        this.mEventHandler = new Handler(context.getMainLooper());
    }

    public WifiNl80211Manager(Context context, IWificond wificond) {
        this(context);
        this.mWificond = wificond;
    }

    public WificondEventHandler getWificondEventHandler() {
        return this.mWificondEventHandler;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class PnoScanEventHandler extends IPnoScanEvent.Stub {
        private ScanEventCallback mCallback;
        private Executor mExecutor;

        PnoScanEventHandler(Executor executor, ScanEventCallback callback) {
            this.mExecutor = executor;
            this.mCallback = callback;
        }

        @Override // android.net.wifi.nl80211.IPnoScanEvent
        public void OnPnoNetworkFound() {
            Log.d(WifiNl80211Manager.TAG, "Pno scan result event");
            long token = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$PnoScanEventHandler$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        WifiNl80211Manager.PnoScanEventHandler.this.m2860x1ab5b5cc();
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$OnPnoNetworkFound$0$android-net-wifi-nl80211-WifiNl80211Manager$PnoScanEventHandler  reason: not valid java name */
        public /* synthetic */ void m2860x1ab5b5cc() {
            this.mCallback.onScanResultReady();
        }

        @Override // android.net.wifi.nl80211.IPnoScanEvent
        public void OnPnoScanFailed() {
            Log.d(WifiNl80211Manager.TAG, "Pno Scan failed event");
            long token = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$PnoScanEventHandler$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        WifiNl80211Manager.PnoScanEventHandler.this.m2861x408b80b3();
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$OnPnoScanFailed$1$android-net-wifi-nl80211-WifiNl80211Manager$PnoScanEventHandler  reason: not valid java name */
        public /* synthetic */ void m2861x408b80b3() {
            this.mCallback.onScanFailed();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ApInterfaceEventCallback extends IApInterfaceEventCallback.Stub {
        private Executor mExecutor;
        private SoftApCallback mSoftApListener;

        ApInterfaceEventCallback(Executor executor, SoftApCallback listener) {
            this.mExecutor = executor;
            this.mSoftApListener = listener;
        }

        @Override // android.net.wifi.nl80211.IApInterfaceEventCallback
        public void onConnectedClientsChanged(final NativeWifiClient client, final boolean isConnected) {
            if (WifiNl80211Manager.this.mVerboseLoggingEnabled) {
                Log.d(WifiNl80211Manager.TAG, "onConnectedClientsChanged called with " + client.getMacAddress() + " isConnected: " + isConnected);
            }
            long token = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$ApInterfaceEventCallback$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        WifiNl80211Manager.ApInterfaceEventCallback.this.m2858x21eb4ab7(client, isConnected);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onConnectedClientsChanged$0$android-net-wifi-nl80211-WifiNl80211Manager$ApInterfaceEventCallback  reason: not valid java name */
        public /* synthetic */ void m2858x21eb4ab7(NativeWifiClient client, boolean isConnected) {
            this.mSoftApListener.onConnectedClientsChanged(client, isConnected);
        }

        @Override // android.net.wifi.nl80211.IApInterfaceEventCallback
        public void onSoftApChannelSwitched(final int frequency, final int bandwidth) {
            long token = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$ApInterfaceEventCallback$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        WifiNl80211Manager.ApInterfaceEventCallback.this.m2859xe4dbde8e(frequency, bandwidth);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSoftApChannelSwitched$1$android-net-wifi-nl80211-WifiNl80211Manager$ApInterfaceEventCallback  reason: not valid java name */
        public /* synthetic */ void m2859xe4dbde8e(int frequency, int bandwidth) {
            this.mSoftApListener.onSoftApChannelSwitched(frequency, toFrameworkBandwidth(bandwidth));
        }

        private int toFrameworkBandwidth(int bandwidth) {
            switch (bandwidth) {
                case 0:
                    return 0;
                case 1:
                    return 1;
                case 2:
                    return 2;
                case 3:
                    return 3;
                case 4:
                    return 4;
                case 5:
                    return 5;
                case 6:
                    return 6;
                case 7:
                    return 11;
                default:
                    return 0;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class SendMgmtFrameEvent extends ISendMgmtFrameEvent.Stub {
        private SendMgmtFrameCallback mCallback;
        private Executor mExecutor;
        private AlarmManager.OnAlarmListener mTimeoutCallback = new AlarmManager.OnAlarmListener() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$SendMgmtFrameEvent$$ExternalSyntheticLambda5
            @Override // android.app.AlarmManager.OnAlarmListener
            public final void onAlarm() {
                WifiNl80211Manager.SendMgmtFrameEvent.this.m2872x7dd84c8c();
            }
        };
        private boolean mWasCalled = false;

        private void runIfFirstCall(Runnable r) {
            if (this.mWasCalled) {
                return;
            }
            this.mWasCalled = true;
            WifiNl80211Manager.this.mSendMgmtFrameInProgress.set(false);
            r.run();
        }

        SendMgmtFrameEvent(Executor executor, SendMgmtFrameCallback callback) {
            this.mExecutor = executor;
            this.mCallback = callback;
            WifiNl80211Manager.this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + 1000, WifiNl80211Manager.TIMEOUT_ALARM_TAG, this.mTimeoutCallback, WifiNl80211Manager.this.mEventHandler);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$2$android-net-wifi-nl80211-WifiNl80211Manager$SendMgmtFrameEvent  reason: not valid java name */
        public /* synthetic */ void m2872x7dd84c8c() {
            runIfFirstCall(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$SendMgmtFrameEvent$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    WifiNl80211Manager.SendMgmtFrameEvent.this.m2871xff7748ad();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$1$android-net-wifi-nl80211-WifiNl80211Manager$SendMgmtFrameEvent  reason: not valid java name */
        public /* synthetic */ void m2871xff7748ad() {
            if (WifiNl80211Manager.this.mVerboseLoggingEnabled) {
                Log.e(WifiNl80211Manager.TAG, "Timed out waiting for ACK");
            }
            long token = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$SendMgmtFrameEvent$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        WifiNl80211Manager.SendMgmtFrameEvent.this.m2870x811644ce();
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$0$android-net-wifi-nl80211-WifiNl80211Manager$SendMgmtFrameEvent  reason: not valid java name */
        public /* synthetic */ void m2870x811644ce() {
            this.mCallback.onFailure(4);
        }

        @Override // android.net.wifi.nl80211.ISendMgmtFrameEvent
        public void OnAck(final int elapsedTimeMs) {
            WifiNl80211Manager.this.mEventHandler.post(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$SendMgmtFrameEvent$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    WifiNl80211Manager.SendMgmtFrameEvent.this.m2866xed1761bf(elapsedTimeMs);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$OnAck$5$android-net-wifi-nl80211-WifiNl80211Manager$SendMgmtFrameEvent  reason: not valid java name */
        public /* synthetic */ void m2866xed1761bf(final int elapsedTimeMs) {
            runIfFirstCall(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$SendMgmtFrameEvent$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    WifiNl80211Manager.SendMgmtFrameEvent.this.m2865x6eb65de0(elapsedTimeMs);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$OnAck$4$android-net-wifi-nl80211-WifiNl80211Manager$SendMgmtFrameEvent  reason: not valid java name */
        public /* synthetic */ void m2865x6eb65de0(final int elapsedTimeMs) {
            WifiNl80211Manager.this.mAlarmManager.cancel(this.mTimeoutCallback);
            long token = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$SendMgmtFrameEvent$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        WifiNl80211Manager.SendMgmtFrameEvent.this.m2864xf0555a01(elapsedTimeMs);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$OnAck$3$android-net-wifi-nl80211-WifiNl80211Manager$SendMgmtFrameEvent  reason: not valid java name */
        public /* synthetic */ void m2864xf0555a01(int elapsedTimeMs) {
            this.mCallback.onAck(elapsedTimeMs);
        }

        @Override // android.net.wifi.nl80211.ISendMgmtFrameEvent
        public void OnFailure(final int reason) {
            WifiNl80211Manager.this.mEventHandler.post(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$SendMgmtFrameEvent$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    WifiNl80211Manager.SendMgmtFrameEvent.this.m2869x51c3ee7b(reason);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$OnFailure$8$android-net-wifi-nl80211-WifiNl80211Manager$SendMgmtFrameEvent  reason: not valid java name */
        public /* synthetic */ void m2869x51c3ee7b(final int reason) {
            runIfFirstCall(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$SendMgmtFrameEvent$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    WifiNl80211Manager.SendMgmtFrameEvent.this.m2868xd362ea9c(reason);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$OnFailure$7$android-net-wifi-nl80211-WifiNl80211Manager$SendMgmtFrameEvent  reason: not valid java name */
        public /* synthetic */ void m2868xd362ea9c(final int reason) {
            WifiNl80211Manager.this.mAlarmManager.cancel(this.mTimeoutCallback);
            long token = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$SendMgmtFrameEvent$$ExternalSyntheticLambda7
                    @Override // java.lang.Runnable
                    public final void run() {
                        WifiNl80211Manager.SendMgmtFrameEvent.this.m2867x5501e6bd(reason);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$OnFailure$6$android-net-wifi-nl80211-WifiNl80211Manager$SendMgmtFrameEvent  reason: not valid java name */
        public /* synthetic */ void m2867x5501e6bd(int reason) {
            this.mCallback.onFailure(reason);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* renamed from: binderDied */
    public void m2857x449b629f() {
        this.mEventHandler.post(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                WifiNl80211Manager.this.m2856lambda$binderDied$0$androidnetwifinl80211WifiNl80211Manager();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$binderDied$0$android-net-wifi-nl80211-WifiNl80211Manager  reason: not valid java name */
    public /* synthetic */ void m2856lambda$binderDied$0$androidnetwifinl80211WifiNl80211Manager() {
        Log.e(TAG, "Wificond died!");
        clearState();
        this.mWificond = null;
        Runnable runnable = this.mDeathEventHandler;
        if (runnable != null) {
            runnable.run();
        }
    }

    public void enableVerboseLogging(boolean enable) {
        this.mVerboseLoggingEnabled = enable;
    }

    public void setOnServiceDeadCallback(Runnable deathEventHandler) {
        if (this.mDeathEventHandler != null) {
            Log.e(TAG, "Death handler already present");
        }
        this.mDeathEventHandler = deathEventHandler;
    }

    private boolean retrieveWificondAndRegisterForDeath() {
        if (this.mWificond != null) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Wificond handle already retrieved");
            }
            return true;
        }
        IBinder binder = ServiceManager.getService(Context.WIFI_NL80211_SERVICE);
        IWificond asInterface = IWificond.Stub.asInterface(binder);
        this.mWificond = asInterface;
        if (asInterface != null) {
            try {
                asInterface.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$$ExternalSyntheticLambda6
                    @Override // android.os.IBinder.DeathRecipient
                    public final void binderDied() {
                        WifiNl80211Manager.this.m2857x449b629f();
                    }
                }, 0);
                this.mWificond.registerWificondEventCallback(this.mWificondEventHandler);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to register death notification for wificond");
                return false;
            }
        }
        Log.e(TAG, "Failed to get reference to wificond");
        return false;
    }

    public boolean setupInterfaceForClientMode(String ifaceName, Executor executor, ScanEventCallback scanCallback, ScanEventCallback pnoScanCallback) {
        Log.d(TAG, "Setting up interface for client mode: " + ifaceName);
        if (retrieveWificondAndRegisterForDeath()) {
            if (scanCallback == null || pnoScanCallback == null || executor == null) {
                Log.e(TAG, "setupInterfaceForClientMode invoked with null callbacks");
                return false;
            }
            try {
                IClientInterface clientInterface = this.mWificond.createClientInterface(ifaceName);
                if (clientInterface == null) {
                    Log.e(TAG, "Could not get IClientInterface instance from wificond");
                    return false;
                }
                Binder.allowBlocking(clientInterface.asBinder());
                this.mClientInterfaces.put(ifaceName, clientInterface);
                try {
                    IWifiScannerImpl wificondScanner = clientInterface.getWifiScannerImpl();
                    if (wificondScanner == null) {
                        Log.e(TAG, "Failed to get WificondScannerImpl");
                        return false;
                    }
                    this.mWificondScanners.put(ifaceName, wificondScanner);
                    Binder.allowBlocking(wificondScanner.asBinder());
                    ScanEventHandler scanEventHandler = new ScanEventHandler(executor, scanCallback);
                    this.mScanEventHandlers.put(ifaceName, scanEventHandler);
                    wificondScanner.subscribeScanEvents(scanEventHandler);
                    PnoScanEventHandler pnoScanEventHandler = new PnoScanEventHandler(executor, pnoScanCallback);
                    this.mPnoScanEventHandlers.put(ifaceName, pnoScanEventHandler);
                    wificondScanner.subscribePnoScanEvents(pnoScanEventHandler);
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to refresh wificond scanner due to remote exception");
                    return true;
                }
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to get IClientInterface due to remote exception");
                return false;
            }
        }
        return false;
    }

    public boolean tearDownClientInterface(String ifaceName) {
        if (getClientInterface(ifaceName) == null) {
            Log.e(TAG, "No valid wificond client interface handler for iface=" + ifaceName);
            return false;
        }
        try {
            IWifiScannerImpl scannerImpl = this.mWificondScanners.get(ifaceName);
            if (scannerImpl != null) {
                scannerImpl.unsubscribeScanEvents();
                scannerImpl.unsubscribePnoScanEvents();
            }
            IWificond iWificond = this.mWificond;
            if (iWificond == null) {
                Log.e(TAG, "tearDownClientInterface: mWificond binder is null! Did wificond die?");
                return false;
            }
            try {
                boolean success = iWificond.tearDownClientInterface(ifaceName);
                if (!success) {
                    Log.e(TAG, "Failed to teardown client interface");
                    return false;
                }
                this.mClientInterfaces.remove(ifaceName);
                this.mWificondScanners.remove(ifaceName);
                this.mScanEventHandlers.remove(ifaceName);
                this.mPnoScanEventHandlers.remove(ifaceName);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to teardown client interface due to remote exception");
                return false;
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "Failed to unsubscribe wificond scanner due to remote exception");
            return false;
        }
    }

    public boolean setupInterfaceForSoftApMode(String ifaceName) {
        Log.d(TAG, "Setting up interface for soft ap mode for iface=" + ifaceName);
        if (retrieveWificondAndRegisterForDeath()) {
            try {
                IApInterface apInterface = this.mWificond.createApInterface(ifaceName);
                if (apInterface == null) {
                    Log.e(TAG, "Could not get IApInterface instance from wificond");
                    return false;
                }
                Binder.allowBlocking(apInterface.asBinder());
                this.mApInterfaces.put(ifaceName, apInterface);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to get IApInterface due to remote exception");
                return false;
            }
        }
        return false;
    }

    public boolean tearDownSoftApInterface(String ifaceName) {
        if (getApInterface(ifaceName) == null) {
            Log.e(TAG, "No valid wificond ap interface handler for iface=" + ifaceName);
            return false;
        }
        IWificond iWificond = this.mWificond;
        if (iWificond == null) {
            Log.e(TAG, "tearDownSoftApInterface: mWificond binder is null! Did wificond die?");
            return false;
        }
        try {
            boolean success = iWificond.tearDownApInterface(ifaceName);
            if (!success) {
                Log.e(TAG, "Failed to teardown AP interface");
                return false;
            }
            this.mApInterfaces.remove(ifaceName);
            this.mApInterfaceListeners.remove(ifaceName);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to teardown AP interface due to remote exception");
            return false;
        }
    }

    public boolean tearDownInterfaces() {
        Log.d(TAG, "tearing down interfaces in wificond");
        if (retrieveWificondAndRegisterForDeath()) {
            try {
                for (Map.Entry<String, IWifiScannerImpl> entry : this.mWificondScanners.entrySet()) {
                    entry.getValue().unsubscribeScanEvents();
                    entry.getValue().unsubscribePnoScanEvents();
                }
                this.mWificond.tearDownInterfaces();
                clearState();
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to tear down interfaces due to remote exception");
                return false;
            }
        }
        return false;
    }

    private IClientInterface getClientInterface(String ifaceName) {
        return this.mClientInterfaces.get(ifaceName);
    }

    public SignalPollResult signalPoll(String ifaceName) {
        IClientInterface iface = getClientInterface(ifaceName);
        if (iface == null) {
            Log.e(TAG, "No valid wificond client interface handler for iface=" + ifaceName);
            return null;
        }
        try {
            int[] resultArray = iface.signalPoll();
            if (resultArray == null || resultArray.length != 4) {
                Log.e(TAG, "Invalid signal poll result from wificond");
                return null;
            }
            return new SignalPollResult(resultArray[0], resultArray[1], resultArray[3], resultArray[2]);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to do signal polling due to remote exception");
            return null;
        }
    }

    public TxPacketCounters getTxPacketCounters(String ifaceName) {
        IClientInterface iface = getClientInterface(ifaceName);
        if (iface == null) {
            Log.e(TAG, "No valid wificond client interface handler for iface=" + ifaceName);
            return null;
        }
        try {
            int[] resultArray = iface.getPacketCounters();
            if (resultArray == null || resultArray.length != 2) {
                Log.e(TAG, "Invalid signal poll result from wificond");
                return null;
            }
            return new TxPacketCounters(resultArray[0], resultArray[1]);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to do signal polling due to remote exception");
            return null;
        }
    }

    private IWifiScannerImpl getScannerImpl(String ifaceName) {
        return this.mWificondScanners.get(ifaceName);
    }

    public List<NativeScanResult> getScanResults(String ifaceName, int scanType) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler for iface=" + ifaceName);
            return new ArrayList();
        }
        List<NativeScanResult> results = null;
        try {
            if (scanType == 0) {
                results = Arrays.asList(scannerImpl.getScanResults());
            } else {
                results = Arrays.asList(scannerImpl.getPnoScanResults());
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to create ScanDetail ArrayList");
        }
        if (results == null) {
            results = new ArrayList<>();
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "get " + results.size() + " scan results from wificond");
        }
        return results;
    }

    public int getMaxSsidsPerScan(String ifaceName) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler for iface=" + ifaceName);
            return 0;
        }
        try {
            return scannerImpl.getMaxSsidsPerScan();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to getMaxSsidsPerScan");
            return 0;
        }
    }

    private static int getScanType(int scanType) {
        switch (scanType) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                throw new IllegalArgumentException("Invalid scan type " + scanType);
        }
    }

    @Deprecated
    public boolean startScan(String ifaceName, int scanType, Set<Integer> freqs, List<byte[]> hiddenNetworkSSIDs) {
        return startScan(ifaceName, scanType, freqs, hiddenNetworkSSIDs, null);
    }

    public boolean startScan(String ifaceName, int scanType, Set<Integer> freqs, List<byte[]> hiddenNetworkSSIDs, Bundle extraScanningParams) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler for iface=" + ifaceName);
            return false;
        }
        SingleScanSettings settings = new SingleScanSettings();
        try {
            settings.scanType = getScanType(scanType);
            settings.channelSettings = new ArrayList<>();
            settings.hiddenNetworks = new ArrayList<>();
            if (extraScanningParams != null) {
                settings.enable6GhzRnr = extraScanningParams.getBoolean(SCANNING_PARAM_ENABLE_6GHZ_RNR);
            }
            if (freqs != null) {
                for (Integer freq : freqs) {
                    ChannelSettings channel = new ChannelSettings();
                    channel.frequency = freq.intValue();
                    settings.channelSettings.add(channel);
                }
            }
            if (hiddenNetworkSSIDs != null) {
                for (byte[] ssid : hiddenNetworkSSIDs) {
                    HiddenNetwork network = new HiddenNetwork();
                    network.ssid = ssid;
                    if (!settings.hiddenNetworks.contains(network)) {
                        settings.hiddenNetworks.add(network);
                    }
                }
            }
            try {
                return scannerImpl.scan(settings);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to request scan due to remote exception");
                return false;
            }
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "Invalid scan type ", e2);
            return false;
        }
    }

    public boolean startPnoScan(String ifaceName, PnoSettings pnoSettings, Executor executor, final PnoScanRequestCallback callback) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler for iface=" + ifaceName);
            return false;
        } else if (callback == null || executor == null) {
            Log.e(TAG, "startPnoScan called with a null callback");
            return false;
        } else {
            try {
                boolean success = scannerImpl.startPnoScan(pnoSettings);
                if (success) {
                    Objects.requireNonNull(callback);
                    executor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$$ExternalSyntheticLambda4
                        @Override // java.lang.Runnable
                        public final void run() {
                            WifiNl80211Manager.PnoScanRequestCallback.this.onPnoRequestSucceeded();
                        }
                    });
                } else {
                    Objects.requireNonNull(callback);
                    executor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$$ExternalSyntheticLambda5
                        @Override // java.lang.Runnable
                        public final void run() {
                            WifiNl80211Manager.PnoScanRequestCallback.this.onPnoRequestFailed();
                        }
                    });
                }
                return success;
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to start pno scan due to remote exception");
                return false;
            }
        }
    }

    public boolean stopPnoScan(String ifaceName) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler for iface=" + ifaceName);
            return false;
        }
        try {
            return scannerImpl.stopPnoScan();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to stop pno scan due to remote exception");
            return false;
        }
    }

    public void abortScan(String ifaceName) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler for iface=" + ifaceName);
            return;
        }
        try {
            scannerImpl.abortScan();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to request abortScan due to remote exception");
        }
    }

    public int[] getChannelsMhzForBand(int band) {
        IWificond iWificond = this.mWificond;
        if (iWificond == null) {
            Log.e(TAG, "getChannelsMhzForBand: mWificond binder is null! Did wificond die?");
            return new int[0];
        }
        int[] result = null;
        try {
            switch (band) {
                case 1:
                    int[] result2 = iWificond.getAvailable2gChannels();
                    result = result2;
                    break;
                case 2:
                    int[] result3 = iWificond.getAvailable5gNonDFSChannels();
                    result = result3;
                    break;
                case 4:
                    int[] result4 = iWificond.getAvailableDFSChannels();
                    result = result4;
                    break;
                case 8:
                    int[] result5 = iWificond.getAvailable6gChannels();
                    result = result5;
                    break;
                case 16:
                    int[] result6 = iWificond.getAvailable60gChannels();
                    result = result6;
                    break;
                default:
                    throw new IllegalArgumentException("unsupported band " + band);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to request getChannelsForBand due to remote exception");
        }
        if (result == null) {
            int[] result7 = new int[0];
            return result7;
        }
        return result;
    }

    private IApInterface getApInterface(String ifaceName) {
        return this.mApInterfaces.get(ifaceName);
    }

    public DeviceWiphyCapabilities getDeviceWiphyCapabilities(String ifaceName) {
        IWificond iWificond = this.mWificond;
        if (iWificond == null) {
            Log.e(TAG, "getDeviceWiphyCapabilities: mWificond binder is null! Did wificond die?");
            return null;
        }
        try {
            return iWificond.getDeviceWiphyCapabilities(ifaceName);
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean registerCountryCodeChangedListener(Executor executor, CountryCodeChangedListener listener) {
        if (!retrieveWificondAndRegisterForDeath()) {
            return false;
        }
        Log.d(TAG, "registerCountryCodeEventListener called");
        this.mWificondEventHandler.registerCountryCodeChangedListener(executor, listener);
        return true;
    }

    public void unregisterCountryCodeChangedListener(CountryCodeChangedListener listener) {
        Log.d(TAG, "unregisterCountryCodeEventListener called");
        this.mWificondEventHandler.unregisterCountryCodeChangedListener(listener);
    }

    public void notifyCountryCodeChanged(String newCountryCode) {
        if (this.mWificond == null) {
            new RemoteException("Wificond service doesn't exist!").rethrowFromSystemServer();
        }
        try {
            this.mWificond.notifyCountryCodeChanged();
            Log.i(TAG, "Receive country code change to " + newCountryCode);
        } catch (RemoteException re) {
            re.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean registerApCallback(String ifaceName, Executor executor, SoftApCallback callback) {
        IApInterface iface = getApInterface(ifaceName);
        if (iface == null) {
            Log.e(TAG, "No valid ap interface handler for iface=" + ifaceName);
            return false;
        } else if (callback == null || executor == null) {
            Log.e(TAG, "registerApCallback called with a null callback");
            return false;
        } else {
            try {
                IApInterfaceEventCallback wificondCallback = new ApInterfaceEventCallback(executor, callback);
                this.mApInterfaceListeners.put(ifaceName, wificondCallback);
                boolean success = iface.registerCallback(wificondCallback);
                if (!success) {
                    Log.e(TAG, "Failed to register ap callback.");
                    return false;
                }
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Exception in registering AP callback: " + e);
                return false;
            }
        }
    }

    public void sendMgmtFrame(String ifaceName, byte[] frame, int mcs, Executor executor, final SendMgmtFrameCallback callback) {
        if (callback == null || executor == null) {
            Log.e(TAG, "callback cannot be null!");
        } else if (frame == null) {
            Log.e(TAG, "frame cannot be null!");
            executor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    WifiNl80211Manager.SendMgmtFrameCallback.this.onFailure(1);
                }
            });
        } else {
            IClientInterface clientInterface = getClientInterface(ifaceName);
            if (clientInterface == null) {
                Log.e(TAG, "No valid wificond client interface handler for iface=" + ifaceName);
                executor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        WifiNl80211Manager.SendMgmtFrameCallback.this.onFailure(1);
                    }
                });
            } else if (!this.mSendMgmtFrameInProgress.compareAndSet(false, true)) {
                Log.e(TAG, "An existing management frame transmission is in progress!");
                executor.execute(new Runnable() { // from class: android.net.wifi.nl80211.WifiNl80211Manager$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        WifiNl80211Manager.SendMgmtFrameCallback.this.onFailure(5);
                    }
                });
            } else {
                SendMgmtFrameEvent sendMgmtFrameEvent = new SendMgmtFrameEvent(executor, callback);
                try {
                    clientInterface.SendMgmtFrame(frame, sendMgmtFrameEvent, mcs);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception while starting link probe: " + e);
                    sendMgmtFrameEvent.OnFailure(1);
                }
            }
        }
    }

    private void clearState() {
        this.mClientInterfaces.clear();
        this.mWificondScanners.clear();
        this.mPnoScanEventHandlers.clear();
        this.mScanEventHandlers.clear();
        this.mApInterfaces.clear();
        this.mApInterfaceListeners.clear();
        this.mSendMgmtFrameInProgress.set(false);
    }

    private static OemSecurityType parseWapiElement(byte[] bytes) {
        if (bytes.length == 4) {
            return null;
        }
        List<Integer> wapiKeyManagement = new ArrayList<>();
        List<Integer> wapiPairwiseCipher = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        try {
            short version = buf.getShort();
            if (version != 1) {
                return null;
            }
            short akmCount = buf.getShort();
            for (int i = 0; i < akmCount; i++) {
                int akm = buf.getInt();
                switch (akm) {
                    case 24253440:
                        wapiKeyManagement.add(14);
                        break;
                    case WAPI_AKM_PSK /* 41030656 */:
                        wapiKeyManagement.add(13);
                        break;
                }
            }
            if (wapiKeyManagement.isEmpty()) {
                Log.d(TAG, "parseWapiElement() set Default AKM to KEY_MGMT_WAPI_PSK");
                wapiKeyManagement.add(13);
            }
            short cipherCount = buf.getShort();
            for (int i2 = 0; i2 < cipherCount; i2++) {
                int cipher = buf.getInt();
                wapiPairwiseCipher.add(Integer.valueOf(parseWapiCipher(cipher)));
            }
            int cipher2 = buf.getInt();
            int groupCipher = parseWapiCipher(cipher2);
            return new OemSecurityType(4, wapiKeyManagement, wapiPairwiseCipher, groupCipher);
        } catch (BufferUnderflowException e) {
            Log.d("IE_Capabilities", "Couldn't parse WAPI IE, buffer underflow");
            return null;
        }
    }

    private static int parseWapiCipher(int cipher) {
        switch (cipher) {
            case 24253440:
                return 5;
            default:
                Log.d("IE_Capabilities", "Unknown WAPI cipher suite: " + Integer.toHexString(cipher));
                return 0;
        }
    }

    /* loaded from: classes2.dex */
    public static class OemSecurityType {
        public final int groupCipher;
        public final List<Integer> keyManagement;
        public final List<Integer> pairwiseCipher;
        public final int protocol;

        public OemSecurityType(int protocol, List<Integer> keyManagement, List<Integer> pairwiseCipher, int groupCipher) {
            this.protocol = protocol;
            this.keyManagement = keyManagement != null ? keyManagement : new ArrayList<>();
            this.pairwiseCipher = pairwiseCipher != null ? pairwiseCipher : new ArrayList<>();
            this.groupCipher = groupCipher;
        }
    }

    public static OemSecurityType parseOemSecurityTypeElement(int id, int idExt, byte[] bytes) {
        switch (id) {
            case 68:
                return parseWapiElement(bytes);
            default:
                return null;
        }
    }
}
