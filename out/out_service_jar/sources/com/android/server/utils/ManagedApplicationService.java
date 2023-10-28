package com.android.server.utils;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.utils.ManagedApplicationService;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
/* loaded from: classes2.dex */
public class ManagedApplicationService {
    private static final int MAX_RETRY_COUNT = 4;
    private static final long MAX_RETRY_DURATION_MS = 16000;
    private static final long MIN_RETRY_DURATION_MS = 2000;
    public static final int RETRY_BEST_EFFORT = 3;
    public static final int RETRY_FOREVER = 1;
    public static final int RETRY_NEVER = 2;
    private static final long RETRY_RESET_TIME_MS = 64000;
    private IInterface mBoundInterface;
    private final BinderChecker mChecker;
    private final int mClientLabel;
    private final ComponentName mComponent;
    private ServiceConnection mConnection;
    private final Context mContext;
    private final EventCallback mEventCb;
    private final Handler mHandler;
    private final boolean mIsImportant;
    private long mLastRetryTimeMs;
    private PendingEvent mPendingEvent;
    private int mRetryCount;
    private final int mRetryType;
    private boolean mRetrying;
    private final String mSettingsAction;
    private final int mUserId;
    private final String TAG = getClass().getSimpleName();
    private final Runnable mRetryRunnable = new Runnable() { // from class: com.android.server.utils.ManagedApplicationService$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            ManagedApplicationService.this.doRetry();
        }
    };
    private final Object mLock = new Object();
    private long mNextRetryDurationMs = MIN_RETRY_DURATION_MS;

    /* loaded from: classes2.dex */
    public interface BinderChecker {
        IInterface asInterface(IBinder iBinder);

        boolean checkType(IInterface iInterface);
    }

    /* loaded from: classes2.dex */
    public interface EventCallback {
        void onServiceEvent(LogEvent logEvent);
    }

    /* loaded from: classes2.dex */
    public interface LogFormattable {
        String toLogString(SimpleDateFormat simpleDateFormat);
    }

    /* loaded from: classes2.dex */
    public interface PendingEvent {
        void runEvent(IInterface iInterface) throws RemoteException;
    }

    /* loaded from: classes2.dex */
    public static class LogEvent implements LogFormattable {
        public static final int EVENT_BINDING_DIED = 3;
        public static final int EVENT_CONNECTED = 1;
        public static final int EVENT_DISCONNECTED = 2;
        public static final int EVENT_STOPPED_PERMANENTLY = 4;
        public final ComponentName component;
        public final int event;
        public final long timestamp;

        public LogEvent(long timestamp, ComponentName component, int event) {
            this.timestamp = timestamp;
            this.component = component;
            this.event = event;
        }

        @Override // com.android.server.utils.ManagedApplicationService.LogFormattable
        public String toLogString(SimpleDateFormat dateFormat) {
            StringBuilder append = new StringBuilder().append(dateFormat.format(new Date(this.timestamp))).append("   ").append(eventToString(this.event)).append(" Managed Service: ");
            ComponentName componentName = this.component;
            return append.append(componentName == null ? "None" : componentName.flattenToString()).toString();
        }

        public static String eventToString(int event) {
            switch (event) {
                case 1:
                    return "Connected";
                case 2:
                    return "Disconnected";
                case 3:
                    return "Binding Died For";
                case 4:
                    return "Permanently Stopped";
                default:
                    return "Unknown Event Occurred";
            }
        }
    }

    private ManagedApplicationService(Context context, ComponentName component, int userId, int clientLabel, String settingsAction, BinderChecker binderChecker, boolean isImportant, int retryType, Handler handler, EventCallback eventCallback) {
        this.mContext = context;
        this.mComponent = component;
        this.mUserId = userId;
        this.mClientLabel = clientLabel;
        this.mSettingsAction = settingsAction;
        this.mChecker = binderChecker;
        this.mIsImportant = isImportant;
        this.mRetryType = retryType;
        this.mHandler = handler;
        this.mEventCb = eventCallback;
    }

    public static ManagedApplicationService build(Context context, ComponentName component, int userId, int clientLabel, String settingsAction, BinderChecker binderChecker, boolean isImportant, int retryType, Handler handler, EventCallback eventCallback) {
        return new ManagedApplicationService(context, component, userId, clientLabel, settingsAction, binderChecker, isImportant, retryType, handler, eventCallback);
    }

    public int getUserId() {
        return this.mUserId;
    }

    public ComponentName getComponent() {
        return this.mComponent;
    }

    public boolean disconnectIfNotMatching(ComponentName componentName, int userId) {
        if (matches(componentName, userId)) {
            return false;
        }
        disconnect();
        return true;
    }

    public void sendEvent(PendingEvent event) {
        IInterface iface;
        synchronized (this.mLock) {
            iface = this.mBoundInterface;
            if (iface == null) {
                this.mPendingEvent = event;
            }
        }
        if (iface != null) {
            try {
                event.runEvent(iface);
            } catch (RemoteException | RuntimeException ex) {
                Slog.e(this.TAG, "Received exception from user service: ", ex);
            }
        }
    }

    public void disconnect() {
        synchronized (this.mLock) {
            ServiceConnection serviceConnection = this.mConnection;
            if (serviceConnection == null) {
                return;
            }
            this.mContext.unbindService(serviceConnection);
            this.mConnection = null;
            this.mBoundInterface = null;
        }
    }

    public void connect() {
        synchronized (this.mLock) {
            if (this.mConnection != null) {
                return;
            }
            Intent intent = new Intent().setComponent(this.mComponent);
            int i = this.mClientLabel;
            if (i != 0) {
                intent.putExtra("android.intent.extra.client_label", i);
            }
            if (this.mSettingsAction != null) {
                intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivity(this.mContext, 0, new Intent(this.mSettingsAction), 67108864));
            }
            AnonymousClass1 anonymousClass1 = new AnonymousClass1();
            this.mConnection = anonymousClass1;
            int flags = AudioFormat.AAC_MAIN;
            if (this.mIsImportant) {
                flags = 67108865 | 64;
            }
            try {
                if (!this.mContext.bindServiceAsUser(intent, anonymousClass1, flags, new UserHandle(this.mUserId))) {
                    Slog.w(this.TAG, "Unable to bind service: " + intent);
                    startRetriesLocked();
                }
            } catch (SecurityException e) {
                Slog.w(this.TAG, "Unable to bind service: " + intent, e);
                startRetriesLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.utils.ManagedApplicationService$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass1 implements ServiceConnection {
        AnonymousClass1() {
        }

        @Override // android.content.ServiceConnection
        public void onBindingDied(ComponentName componentName) {
            final long timestamp = System.currentTimeMillis();
            Slog.w(ManagedApplicationService.this.TAG, "Service binding died: " + componentName);
            synchronized (ManagedApplicationService.this.mLock) {
                if (ManagedApplicationService.this.mConnection != this) {
                    return;
                }
                ManagedApplicationService.this.mHandler.post(new Runnable() { // from class: com.android.server.utils.ManagedApplicationService$1$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        ManagedApplicationService.AnonymousClass1.this.m7433xe4b449e0(timestamp);
                    }
                });
                ManagedApplicationService.this.mBoundInterface = null;
                ManagedApplicationService.this.startRetriesLocked();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onBindingDied$0$com-android-server-utils-ManagedApplicationService$1  reason: not valid java name */
        public /* synthetic */ void m7433xe4b449e0(long timestamp) {
            ManagedApplicationService.this.mEventCb.onServiceEvent(new LogEvent(timestamp, ManagedApplicationService.this.mComponent, 3));
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            final long timestamp = System.currentTimeMillis();
            Slog.i(ManagedApplicationService.this.TAG, "Service connected: " + componentName);
            IInterface iface = null;
            PendingEvent pendingEvent = null;
            synchronized (ManagedApplicationService.this.mLock) {
                if (ManagedApplicationService.this.mConnection != this) {
                    return;
                }
                ManagedApplicationService.this.mHandler.post(new Runnable() { // from class: com.android.server.utils.ManagedApplicationService$1$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        ManagedApplicationService.AnonymousClass1.this.m7434x35ec5fdc(timestamp);
                    }
                });
                ManagedApplicationService.this.stopRetriesLocked();
                ManagedApplicationService.this.mBoundInterface = null;
                if (ManagedApplicationService.this.mChecker != null) {
                    ManagedApplicationService managedApplicationService = ManagedApplicationService.this;
                    managedApplicationService.mBoundInterface = managedApplicationService.mChecker.asInterface(iBinder);
                    if (!ManagedApplicationService.this.mChecker.checkType(ManagedApplicationService.this.mBoundInterface)) {
                        ManagedApplicationService.this.mBoundInterface = null;
                        Slog.w(ManagedApplicationService.this.TAG, "Invalid binder from " + componentName);
                        ManagedApplicationService.this.startRetriesLocked();
                        return;
                    }
                    iface = ManagedApplicationService.this.mBoundInterface;
                    pendingEvent = ManagedApplicationService.this.mPendingEvent;
                    ManagedApplicationService.this.mPendingEvent = null;
                }
                if (iface != null && pendingEvent != null) {
                    try {
                        pendingEvent.runEvent(iface);
                    } catch (RemoteException | RuntimeException ex) {
                        Slog.e(ManagedApplicationService.this.TAG, "Received exception from user service: ", ex);
                        ManagedApplicationService.this.startRetriesLocked();
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onServiceConnected$1$com-android-server-utils-ManagedApplicationService$1  reason: not valid java name */
        public /* synthetic */ void m7434x35ec5fdc(long timestamp) {
            ManagedApplicationService.this.mEventCb.onServiceEvent(new LogEvent(timestamp, ManagedApplicationService.this.mComponent, 1));
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            final long timestamp = System.currentTimeMillis();
            Slog.w(ManagedApplicationService.this.TAG, "Service disconnected: " + componentName);
            synchronized (ManagedApplicationService.this.mLock) {
                if (ManagedApplicationService.this.mConnection != this) {
                    return;
                }
                ManagedApplicationService.this.mHandler.post(new Runnable() { // from class: com.android.server.utils.ManagedApplicationService$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ManagedApplicationService.AnonymousClass1.this.m7435xf7ea1b7(timestamp);
                    }
                });
                ManagedApplicationService.this.mBoundInterface = null;
                ManagedApplicationService.this.startRetriesLocked();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onServiceDisconnected$2$com-android-server-utils-ManagedApplicationService$1  reason: not valid java name */
        public /* synthetic */ void m7435xf7ea1b7(long timestamp) {
            ManagedApplicationService.this.mEventCb.onServiceEvent(new LogEvent(timestamp, ManagedApplicationService.this.mComponent, 2));
        }
    }

    private boolean matches(ComponentName component, int userId) {
        return Objects.equals(this.mComponent, component) && this.mUserId == userId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startRetriesLocked() {
        if (checkAndDeliverServiceDiedCbLocked()) {
            disconnect();
        } else if (this.mRetrying) {
        } else {
            this.mRetrying = true;
            queueRetryLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopRetriesLocked() {
        this.mRetrying = false;
        this.mHandler.removeCallbacks(this.mRetryRunnable);
    }

    private void queueRetryLocked() {
        long now = SystemClock.uptimeMillis();
        if (now - this.mLastRetryTimeMs > RETRY_RESET_TIME_MS) {
            this.mNextRetryDurationMs = MIN_RETRY_DURATION_MS;
            this.mRetryCount = 0;
        }
        this.mLastRetryTimeMs = now;
        this.mHandler.postDelayed(this.mRetryRunnable, this.mNextRetryDurationMs);
        this.mNextRetryDurationMs = Math.min(this.mNextRetryDurationMs * 2, (long) MAX_RETRY_DURATION_MS);
        this.mRetryCount++;
    }

    private boolean checkAndDeliverServiceDiedCbLocked() {
        int i = this.mRetryType;
        if (i == 2 || (i == 3 && this.mRetryCount >= 4)) {
            Slog.e(this.TAG, "Service " + this.mComponent + " has died too much, not retrying.");
            if (this.mEventCb != null) {
                final long timestamp = System.currentTimeMillis();
                this.mHandler.post(new Runnable() { // from class: com.android.server.utils.ManagedApplicationService$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        ManagedApplicationService.this.m7432x28a226d7(timestamp);
                    }
                });
                return true;
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$checkAndDeliverServiceDiedCbLocked$0$com-android-server-utils-ManagedApplicationService  reason: not valid java name */
    public /* synthetic */ void m7432x28a226d7(long timestamp) {
        this.mEventCb.onServiceEvent(new LogEvent(timestamp, this.mComponent, 4));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doRetry() {
        synchronized (this.mLock) {
            if (this.mConnection == null) {
                return;
            }
            if (this.mRetrying) {
                Slog.i(this.TAG, "Attempting to reconnect " + this.mComponent + "...");
                disconnect();
                if (checkAndDeliverServiceDiedCbLocked()) {
                    return;
                }
                queueRetryLocked();
                connect();
            }
        }
    }
}
