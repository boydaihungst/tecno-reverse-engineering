package com.android.server.vcn;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.InetAddresses;
import android.net.IpPrefix;
import android.net.IpSecManager;
import android.net.IpSecTransform;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkAgent;
import android.net.NetworkAgentConfig;
import android.net.NetworkCapabilities;
import android.net.NetworkProvider;
import android.net.NetworkScore;
import android.net.RouteInfo;
import android.net.TelephonyNetworkSpecifier;
import android.net.Uri;
import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.ChildSessionConfiguration;
import android.net.ipsec.ike.ChildSessionParams;
import android.net.ipsec.ike.IkeSession;
import android.net.ipsec.ike.IkeSessionCallback;
import android.net.ipsec.ike.IkeSessionConfiguration;
import android.net.ipsec.ike.IkeSessionConnectionInfo;
import android.net.ipsec.ike.IkeSessionParams;
import android.net.ipsec.ike.IkeTunnelConnectionParams;
import android.net.ipsec.ike.exceptions.IkeException;
import android.net.ipsec.ike.exceptions.IkeInternalException;
import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.vcn.VcnGatewayConnectionConfig;
import android.net.vcn.VcnTransportInfo;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.VcnManagementService;
import com.android.server.vcn.TelephonySubscriptionTracker;
import com.android.server.vcn.Vcn;
import com.android.server.vcn.VcnGatewayConnection;
import com.android.server.vcn.routeselection.UnderlyingNetworkController;
import com.android.server.vcn.routeselection.UnderlyingNetworkRecord;
import com.android.server.vcn.util.LogUtils;
import com.android.server.vcn.util.MtuUtils;
import com.android.server.vcn.util.OneWayBoolean;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class VcnGatewayConnection extends StateMachine {
    private static final int ARG_NOT_PRESENT = Integer.MIN_VALUE;
    private static final String DISCONNECT_REASON_INTERNAL_ERROR = "Uncaught exception: ";
    private static final String DISCONNECT_REASON_NETWORK_AGENT_UNWANTED = "NetworkAgent was unwanted";
    private static final String DISCONNECT_REASON_TEARDOWN = "teardown() called on VcnTunnel";
    private static final String DISCONNECT_REASON_UNDERLYING_NETWORK_LOST = "Underlying Network lost";
    static final String DISCONNECT_REQUEST_ALARM;
    static final InetAddress DUMMY_ADDR;
    private static final int EVENT_DISCONNECT_REQUESTED = 7;
    private static final int EVENT_IKE_CONNECTION_INFO_CHANGED = 12;
    private static final int EVENT_MIGRATION_COMPLETED = 11;
    private static final int EVENT_RETRY_TIMEOUT_EXPIRED = 2;
    private static final int EVENT_SAFE_MODE_TIMEOUT_EXCEEDED = 10;
    private static final int EVENT_SESSION_CLOSED = 4;
    private static final int EVENT_SESSION_LOST = 3;
    private static final int EVENT_SETUP_COMPLETED = 6;
    private static final int EVENT_SUBSCRIPTIONS_CHANGED = 9;
    private static final int EVENT_TEARDOWN_TIMEOUT_EXPIRED = 8;
    private static final int EVENT_TRANSFORM_CREATED = 5;
    private static final int EVENT_UNDERLYING_NETWORK_CHANGED = 1;
    private static final int[] MERGED_CAPABILITIES;
    static final String NETWORK_INFO_EXTRA_INFO = "VCN";
    static final String NETWORK_INFO_NETWORK_TYPE_STRING = "MOBILE";
    static final int NETWORK_LOSS_DISCONNECT_TIMEOUT_SECONDS = 30;
    static final String RETRY_TIMEOUT_ALARM;
    static final String SAFEMODE_TIMEOUT_ALARM;
    static final int SAFEMODE_TIMEOUT_SECONDS = 30;
    private static final int SAFEMODE_TIMEOUT_SECONDS_TEST_MODE = 10;
    private static final String TAG;
    static final String TEARDOWN_TIMEOUT_ALARM;
    static final int TEARDOWN_TIMEOUT_SECONDS = 5;
    private static final int TOKEN_ALL = Integer.MIN_VALUE;
    private VcnChildSessionConfiguration mChildConfig;
    final ConnectedState mConnectedState;
    final ConnectingState mConnectingState;
    private final VcnGatewayConnectionConfig mConnectionConfig;
    private final ConnectivityManager mConnectivityManager;
    private int mCurrentToken;
    private final Dependencies mDeps;
    private WakeupMessage mDisconnectRequestAlarm;
    final DisconnectedState mDisconnectedState;
    final DisconnectingState mDisconnectingState;
    private int mFailedAttempts;
    private final Vcn.VcnGatewayStatusCallback mGatewayStatusCallback;
    private IkeSessionConnectionInfo mIkeConnectionInfo;
    private VcnIkeSession mIkeSession;
    private final IpSecManager mIpSecManager;
    private boolean mIsInSafeMode;
    private final boolean mIsMobileDataEnabled;
    private OneWayBoolean mIsQuitting;
    private TelephonySubscriptionTracker.TelephonySubscriptionSnapshot mLastSnapshot;
    private VcnNetworkAgent mNetworkAgent;
    private WakeupMessage mRetryTimeoutAlarm;
    final RetryTimeoutState mRetryTimeoutState;
    private WakeupMessage mSafeModeTimeoutAlarm;
    private final ParcelUuid mSubscriptionGroup;
    private WakeupMessage mTeardownTimeoutAlarm;
    private IpSecManager.IpSecTunnelInterface mTunnelIface;
    private UnderlyingNetworkRecord mUnderlying;
    private final UnderlyingNetworkController mUnderlyingNetworkController;
    private final VcnUnderlyingNetworkControllerCallback mUnderlyingNetworkControllerCallback;
    private final VcnContext mVcnContext;
    private final VcnWakeLock mWakeLock;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public interface EventInfo {
    }

    static {
        String simpleName = VcnGatewayConnection.class.getSimpleName();
        TAG = simpleName;
        DUMMY_ADDR = InetAddresses.parseNumericAddress("192.0.2.0");
        TEARDOWN_TIMEOUT_ALARM = simpleName + "_TEARDOWN_TIMEOUT_ALARM";
        DISCONNECT_REQUEST_ALARM = simpleName + "_DISCONNECT_REQUEST_ALARM";
        RETRY_TIMEOUT_ALARM = simpleName + "_RETRY_TIMEOUT_ALARM";
        SAFEMODE_TIMEOUT_ALARM = simpleName + "_SAFEMODE_TIMEOUT_ALARM";
        MERGED_CAPABILITIES = new int[]{11, 18};
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class EventUnderlyingNetworkChangedInfo implements EventInfo {
        public final UnderlyingNetworkRecord newUnderlying;

        EventUnderlyingNetworkChangedInfo(UnderlyingNetworkRecord newUnderlying) {
            this.newUnderlying = newUnderlying;
        }

        public int hashCode() {
            return Objects.hash(this.newUnderlying);
        }

        public boolean equals(Object other) {
            if (!(other instanceof EventUnderlyingNetworkChangedInfo)) {
                return false;
            }
            EventUnderlyingNetworkChangedInfo rhs = (EventUnderlyingNetworkChangedInfo) other;
            return Objects.equals(this.newUnderlying, rhs.newUnderlying);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class EventSessionLostInfo implements EventInfo {
        public final Exception exception;

        EventSessionLostInfo(Exception exception) {
            this.exception = exception;
        }

        public int hashCode() {
            return Objects.hash(this.exception);
        }

        public boolean equals(Object other) {
            if (!(other instanceof EventSessionLostInfo)) {
                return false;
            }
            EventSessionLostInfo rhs = (EventSessionLostInfo) other;
            return Objects.equals(this.exception, rhs.exception);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class EventTransformCreatedInfo implements EventInfo {
        public final int direction;
        public final IpSecTransform transform;

        EventTransformCreatedInfo(int direction, IpSecTransform transform) {
            this.direction = direction;
            this.transform = (IpSecTransform) Objects.requireNonNull(transform);
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.direction), this.transform);
        }

        public boolean equals(Object other) {
            if (other instanceof EventTransformCreatedInfo) {
                EventTransformCreatedInfo rhs = (EventTransformCreatedInfo) other;
                return this.direction == rhs.direction && Objects.equals(this.transform, rhs.transform);
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class EventSetupCompletedInfo implements EventInfo {
        public final VcnChildSessionConfiguration childSessionConfig;

        EventSetupCompletedInfo(VcnChildSessionConfiguration childSessionConfig) {
            this.childSessionConfig = (VcnChildSessionConfiguration) Objects.requireNonNull(childSessionConfig);
        }

        public int hashCode() {
            return Objects.hash(this.childSessionConfig);
        }

        public boolean equals(Object other) {
            if (!(other instanceof EventSetupCompletedInfo)) {
                return false;
            }
            EventSetupCompletedInfo rhs = (EventSetupCompletedInfo) other;
            return Objects.equals(this.childSessionConfig, rhs.childSessionConfig);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class EventDisconnectRequestedInfo implements EventInfo {
        public final String reason;
        public final boolean shouldQuit;

        EventDisconnectRequestedInfo(String reason, boolean shouldQuit) {
            this.reason = (String) Objects.requireNonNull(reason);
            this.shouldQuit = shouldQuit;
        }

        public int hashCode() {
            return Objects.hash(this.reason, Boolean.valueOf(this.shouldQuit));
        }

        public boolean equals(Object other) {
            if (other instanceof EventDisconnectRequestedInfo) {
                EventDisconnectRequestedInfo rhs = (EventDisconnectRequestedInfo) other;
                return this.reason.equals(rhs.reason) && this.shouldQuit == rhs.shouldQuit;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class EventMigrationCompletedInfo implements EventInfo {
        public final IpSecTransform inTransform;
        public final IpSecTransform outTransform;

        EventMigrationCompletedInfo(IpSecTransform inTransform, IpSecTransform outTransform) {
            this.inTransform = (IpSecTransform) Objects.requireNonNull(inTransform);
            this.outTransform = (IpSecTransform) Objects.requireNonNull(outTransform);
        }

        public int hashCode() {
            return Objects.hash(this.inTransform, this.outTransform);
        }

        public boolean equals(Object other) {
            if (other instanceof EventMigrationCompletedInfo) {
                EventMigrationCompletedInfo rhs = (EventMigrationCompletedInfo) other;
                return Objects.equals(this.inTransform, rhs.inTransform) && Objects.equals(this.outTransform, rhs.outTransform);
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class EventIkeConnectionInfoChangedInfo implements EventInfo {
        public final IkeSessionConnectionInfo ikeConnectionInfo;

        EventIkeConnectionInfoChangedInfo(IkeSessionConnectionInfo ikeConnectionInfo) {
            this.ikeConnectionInfo = ikeConnectionInfo;
        }

        public int hashCode() {
            return Objects.hash(this.ikeConnectionInfo);
        }

        public boolean equals(Object other) {
            if (!(other instanceof EventIkeConnectionInfoChangedInfo)) {
                return false;
            }
            EventIkeConnectionInfoChangedInfo rhs = (EventIkeConnectionInfoChangedInfo) other;
            return Objects.equals(this.ikeConnectionInfo, rhs.ikeConnectionInfo);
        }
    }

    public VcnGatewayConnection(VcnContext vcnContext, ParcelUuid subscriptionGroup, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, VcnGatewayConnectionConfig connectionConfig, Vcn.VcnGatewayStatusCallback gatewayStatusCallback, boolean isMobileDataEnabled) {
        this(vcnContext, subscriptionGroup, snapshot, connectionConfig, gatewayStatusCallback, isMobileDataEnabled, new Dependencies());
    }

    /* JADX WARN: Illegal instructions before constructor call */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    VcnGatewayConnection(VcnContext vcnContext, ParcelUuid subscriptionGroup, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, VcnGatewayConnectionConfig connectionConfig, Vcn.VcnGatewayStatusCallback gatewayStatusCallback, boolean isMobileDataEnabled, Dependencies deps) {
        super(r1, ((VcnContext) Objects.requireNonNull(vcnContext, "Missing vcnContext")).getLooper());
        String str = TAG;
        DisconnectedState disconnectedState = new DisconnectedState();
        this.mDisconnectedState = disconnectedState;
        DisconnectingState disconnectingState = new DisconnectingState();
        this.mDisconnectingState = disconnectingState;
        ConnectingState connectingState = new ConnectingState();
        this.mConnectingState = connectingState;
        ConnectedState connectedState = new ConnectedState();
        this.mConnectedState = connectedState;
        RetryTimeoutState retryTimeoutState = new RetryTimeoutState();
        this.mRetryTimeoutState = retryTimeoutState;
        this.mTunnelIface = null;
        this.mIsQuitting = new OneWayBoolean();
        this.mIsInSafeMode = false;
        this.mCurrentToken = -1;
        this.mFailedAttempts = 0;
        this.mVcnContext = vcnContext;
        this.mSubscriptionGroup = (ParcelUuid) Objects.requireNonNull(subscriptionGroup, "Missing subscriptionGroup");
        VcnGatewayConnectionConfig vcnGatewayConnectionConfig = (VcnGatewayConnectionConfig) Objects.requireNonNull(connectionConfig, "Missing connectionConfig");
        this.mConnectionConfig = vcnGatewayConnectionConfig;
        this.mGatewayStatusCallback = (Vcn.VcnGatewayStatusCallback) Objects.requireNonNull(gatewayStatusCallback, "Missing gatewayStatusCallback");
        this.mIsMobileDataEnabled = isMobileDataEnabled;
        Dependencies dependencies = (Dependencies) Objects.requireNonNull(deps, "Missing deps");
        this.mDeps = dependencies;
        this.mLastSnapshot = (TelephonySubscriptionTracker.TelephonySubscriptionSnapshot) Objects.requireNonNull(snapshot, "Missing snapshot");
        VcnUnderlyingNetworkControllerCallback vcnUnderlyingNetworkControllerCallback = new VcnUnderlyingNetworkControllerCallback();
        this.mUnderlyingNetworkControllerCallback = vcnUnderlyingNetworkControllerCallback;
        this.mWakeLock = dependencies.newWakeLock(vcnContext.getContext(), 1, str);
        this.mUnderlyingNetworkController = dependencies.newUnderlyingNetworkController(vcnContext, vcnGatewayConnectionConfig, subscriptionGroup, this.mLastSnapshot, vcnUnderlyingNetworkControllerCallback);
        this.mIpSecManager = (IpSecManager) vcnContext.getContext().getSystemService(IpSecManager.class);
        this.mConnectivityManager = (ConnectivityManager) vcnContext.getContext().getSystemService(ConnectivityManager.class);
        addState(disconnectedState);
        addState(disconnectingState);
        addState(connectingState);
        addState(connectedState);
        addState(retryTimeoutState);
        setInitialState(disconnectedState);
        setDbg(false);
        start();
    }

    public boolean isInSafeMode() {
        this.mVcnContext.ensureRunningOnLooperThread();
        return this.mIsInSafeMode;
    }

    public void teardownAsynchronously() {
        logDbg("Triggering async teardown");
        sendDisconnectRequestedAndAcquireWakelock(DISCONNECT_REASON_TEARDOWN, true);
    }

    protected void onQuitting() {
        logInfo("Quitting VcnGatewayConnection");
        if (this.mNetworkAgent != null) {
            logWtf("NetworkAgent was non-null in onQuitting");
            this.mNetworkAgent.unregister();
            this.mNetworkAgent = null;
        }
        if (this.mIkeSession != null) {
            logWtf("IkeSession was non-null in onQuitting");
            this.mIkeSession.kill();
            this.mIkeSession = null;
        }
        IpSecManager.IpSecTunnelInterface ipSecTunnelInterface = this.mTunnelIface;
        if (ipSecTunnelInterface != null) {
            ipSecTunnelInterface.close();
        }
        releaseWakeLock();
        cancelTeardownTimeoutAlarm();
        cancelDisconnectRequestAlarm();
        cancelRetryTimeoutAlarm();
        cancelSafeModeAlarm();
        this.mUnderlyingNetworkController.teardown();
        this.mGatewayStatusCallback.onQuit();
    }

    public void updateSubscriptionSnapshot(TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "Missing snapshot");
        this.mVcnContext.ensureRunningOnLooperThread();
        this.mLastSnapshot = snapshot;
        this.mUnderlyingNetworkController.updateSubscriptionSnapshot(snapshot);
        sendMessageAndAcquireWakeLock(9, Integer.MIN_VALUE);
    }

    /* loaded from: classes2.dex */
    private class VcnUnderlyingNetworkControllerCallback implements UnderlyingNetworkController.UnderlyingNetworkControllerCallback {
        private VcnUnderlyingNetworkControllerCallback() {
        }

        @Override // com.android.server.vcn.routeselection.UnderlyingNetworkController.UnderlyingNetworkControllerCallback
        public void onSelectedUnderlyingNetworkChanged(UnderlyingNetworkRecord underlying) {
            VcnGatewayConnection.this.mVcnContext.ensureRunningOnLooperThread();
            VcnGatewayConnection.this.logInfo("Selected underlying network changed: " + (underlying == null ? null : underlying.network));
            if (underlying == null) {
                if (VcnGatewayConnection.this.mDeps.isAirplaneModeOn(VcnGatewayConnection.this.mVcnContext)) {
                    VcnGatewayConnection.this.sendMessageAndAcquireWakeLock(1, Integer.MIN_VALUE, new EventUnderlyingNetworkChangedInfo(null));
                    VcnGatewayConnection.this.sendDisconnectRequestedAndAcquireWakelock(VcnGatewayConnection.DISCONNECT_REASON_UNDERLYING_NETWORK_LOST, false);
                    return;
                }
                VcnGatewayConnection.this.setDisconnectRequestAlarm();
            } else {
                VcnGatewayConnection.this.cancelDisconnectRequestAlarm();
            }
            VcnGatewayConnection.this.sendMessageAndAcquireWakeLock(1, Integer.MIN_VALUE, new EventUnderlyingNetworkChangedInfo(underlying));
        }
    }

    private void acquireWakeLock() {
        this.mVcnContext.ensureRunningOnLooperThread();
        if (!this.mIsQuitting.getValue()) {
            this.mWakeLock.acquire();
            logVdbg("Wakelock acquired: " + this.mWakeLock);
        }
    }

    private void releaseWakeLock() {
        this.mVcnContext.ensureRunningOnLooperThread();
        this.mWakeLock.release();
        logVdbg("Wakelock released: " + this.mWakeLock);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeReleaseWakeLock() {
        Handler handler = getHandler();
        if (handler == null || !handler.hasMessagesOrCallbacks()) {
            releaseWakeLock();
        }
    }

    public void sendMessage(int what) {
        logWtf("sendMessage should not be used in VcnGatewayConnection. See sendMessageAndAcquireWakeLock()");
        super.sendMessage(what);
    }

    public void sendMessage(int what, Object obj) {
        logWtf("sendMessage should not be used in VcnGatewayConnection. See sendMessageAndAcquireWakeLock()");
        super.sendMessage(what, obj);
    }

    public void sendMessage(int what, int arg1) {
        logWtf("sendMessage should not be used in VcnGatewayConnection. See sendMessageAndAcquireWakeLock()");
        super.sendMessage(what, arg1);
    }

    public void sendMessage(int what, int arg1, int arg2) {
        logWtf("sendMessage should not be used in VcnGatewayConnection. See sendMessageAndAcquireWakeLock()");
        super.sendMessage(what, arg1, arg2);
    }

    public void sendMessage(int what, int arg1, int arg2, Object obj) {
        logWtf("sendMessage should not be used in VcnGatewayConnection. See sendMessageAndAcquireWakeLock()");
        super.sendMessage(what, arg1, arg2, obj);
    }

    public void sendMessage(Message msg) {
        logWtf("sendMessage should not be used in VcnGatewayConnection. See sendMessageAndAcquireWakeLock()");
        super.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendMessageAndAcquireWakeLock(int what, int token) {
        acquireWakeLock();
        super.sendMessage(what, token);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendMessageAndAcquireWakeLock(int what, int token, EventInfo data) {
        acquireWakeLock();
        super.sendMessage(what, token, Integer.MIN_VALUE, data);
    }

    private void sendMessageAndAcquireWakeLock(int what, int token, int arg2, EventInfo data) {
        acquireWakeLock();
        super.sendMessage(what, token, arg2, data);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: sendMessageAndAcquireWakeLock */
    public void m7505x45ae7932(Message msg) {
        acquireWakeLock();
        super.sendMessage(msg);
    }

    private void removeEqualMessages(int what) {
        removeEqualMessages(what, null);
    }

    private void removeEqualMessages(int what, Object obj) {
        Handler handler = getHandler();
        if (handler != null) {
            handler.removeEqualMessages(what, obj);
        }
        maybeReleaseWakeLock();
    }

    private WakeupMessage createScheduledAlarm(String cmdName, final Message delayedMessage, long delay) {
        Handler handler = getHandler();
        if (handler == null) {
            logWarn("Attempted to schedule alarm after StateMachine has quit", new IllegalStateException());
            return null;
        }
        WakeupMessage alarm = this.mDeps.newWakeupMessage(this.mVcnContext, handler, cmdName, new Runnable() { // from class: com.android.server.vcn.VcnGatewayConnection$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                VcnGatewayConnection.this.m7505x45ae7932(delayedMessage);
            }
        });
        alarm.schedule(this.mDeps.getElapsedRealTime() + delay);
        return alarm;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setTeardownTimeoutAlarm() {
        logVdbg("Setting teardown timeout alarm; mCurrentToken: " + this.mCurrentToken);
        if (this.mTeardownTimeoutAlarm != null) {
            logWtf("mTeardownTimeoutAlarm should be null before being set; mCurrentToken: " + this.mCurrentToken);
        }
        Message delayedMessage = obtainMessage(8, this.mCurrentToken);
        this.mTeardownTimeoutAlarm = createScheduledAlarm(TEARDOWN_TIMEOUT_ALARM, delayedMessage, TimeUnit.SECONDS.toMillis(5L));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelTeardownTimeoutAlarm() {
        logVdbg("Cancelling teardown timeout alarm; mCurrentToken: " + this.mCurrentToken);
        WakeupMessage wakeupMessage = this.mTeardownTimeoutAlarm;
        if (wakeupMessage != null) {
            wakeupMessage.cancel();
            this.mTeardownTimeoutAlarm = null;
        }
        removeEqualMessages(8);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDisconnectRequestAlarm() {
        logVdbg("Setting alarm to disconnect due to underlying network loss; mCurrentToken: " + this.mCurrentToken);
        if (this.mDisconnectRequestAlarm != null) {
            return;
        }
        Message delayedMessage = obtainMessage(7, Integer.MIN_VALUE, 0, new EventDisconnectRequestedInfo(DISCONNECT_REASON_UNDERLYING_NETWORK_LOST, false));
        this.mDisconnectRequestAlarm = createScheduledAlarm(DISCONNECT_REQUEST_ALARM, delayedMessage, TimeUnit.SECONDS.toMillis(30L));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelDisconnectRequestAlarm() {
        logVdbg("Cancelling alarm to disconnect due to underlying network loss; mCurrentToken: " + this.mCurrentToken);
        WakeupMessage wakeupMessage = this.mDisconnectRequestAlarm;
        if (wakeupMessage != null) {
            wakeupMessage.cancel();
            this.mDisconnectRequestAlarm = null;
        }
        removeEqualMessages(7, new EventDisconnectRequestedInfo(DISCONNECT_REASON_UNDERLYING_NETWORK_LOST, false));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setRetryTimeoutAlarm(long delay) {
        logVdbg("Setting retry alarm; mCurrentToken: " + this.mCurrentToken);
        if (this.mRetryTimeoutAlarm != null) {
            logWtf("mRetryTimeoutAlarm should be null before being set; mCurrentToken: " + this.mCurrentToken);
        }
        Message delayedMessage = obtainMessage(2, this.mCurrentToken);
        this.mRetryTimeoutAlarm = createScheduledAlarm(RETRY_TIMEOUT_ALARM, delayedMessage, delay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelRetryTimeoutAlarm() {
        logVdbg("Cancel retry alarm; mCurrentToken: " + this.mCurrentToken);
        WakeupMessage wakeupMessage = this.mRetryTimeoutAlarm;
        if (wakeupMessage != null) {
            wakeupMessage.cancel();
            this.mRetryTimeoutAlarm = null;
        }
        removeEqualMessages(2);
    }

    void setSafeModeAlarm() {
        long millis;
        logVdbg("Setting safe mode alarm; mCurrentToken: " + this.mCurrentToken);
        if (this.mSafeModeTimeoutAlarm != null) {
            return;
        }
        Message delayedMessage = obtainMessage(10, Integer.MIN_VALUE);
        String str = SAFEMODE_TIMEOUT_ALARM;
        if (this.mVcnContext.isInTestMode()) {
            millis = TimeUnit.SECONDS.toMillis(10L);
        } else {
            millis = TimeUnit.SECONDS.toMillis(30L);
        }
        this.mSafeModeTimeoutAlarm = createScheduledAlarm(str, delayedMessage, millis);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelSafeModeAlarm() {
        logVdbg("Cancel safe mode alarm; mCurrentToken: " + this.mCurrentToken);
        WakeupMessage wakeupMessage = this.mSafeModeTimeoutAlarm;
        if (wakeupMessage != null) {
            wakeupMessage.cancel();
            this.mSafeModeTimeoutAlarm = null;
        }
        removeEqualMessages(10);
    }

    private void sessionLostWithoutCallback(int token, Exception exception) {
        sendMessageAndAcquireWakeLock(3, token, new EventSessionLostInfo(exception));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sessionLost(int token, Exception exception) {
        if (exception != null) {
            this.mGatewayStatusCallback.onGatewayConnectionError(this.mConnectionConfig.getGatewayConnectionName(), 0, RuntimeException.class.getName(), "Received " + exception.getClass().getSimpleName() + " with message: " + exception.getMessage());
        }
        sessionLostWithoutCallback(token, exception);
    }

    private static boolean isIkeAuthFailure(Exception exception) {
        return (exception instanceof IkeProtocolException) && ((IkeProtocolException) exception).getErrorType() == 24;
    }

    private void notifyStatusCallbackForSessionClosed(Exception exception) {
        int errorCode;
        String exceptionClass;
        String exceptionMessage;
        if (isIkeAuthFailure(exception)) {
            errorCode = 1;
            exceptionClass = exception.getClass().getName();
            exceptionMessage = exception.getMessage();
        } else if ((exception instanceof IkeInternalException) && (exception.getCause() instanceof IOException)) {
            errorCode = 2;
            exceptionClass = IOException.class.getName();
            exceptionMessage = exception.getCause().getMessage();
        } else {
            errorCode = 0;
            exceptionClass = RuntimeException.class.getName();
            exceptionMessage = "Received " + exception.getClass().getSimpleName() + " with message: " + exception.getMessage();
        }
        logDbg("Encountered error; code=" + errorCode + ", exceptionClass=" + exceptionClass + ", exceptionMessage=" + exceptionMessage);
        this.mGatewayStatusCallback.onGatewayConnectionError(this.mConnectionConfig.getGatewayConnectionName(), errorCode, exceptionClass, exceptionMessage);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void ikeConnectionInfoChanged(int token, IkeSessionConnectionInfo ikeConnectionInfo) {
        sendMessageAndAcquireWakeLock(12, token, new EventIkeConnectionInfoChangedInfo(ikeConnectionInfo));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sessionClosed(int token, Exception exception) {
        if (exception != null) {
            notifyStatusCallbackForSessionClosed(exception);
        }
        sessionLostWithoutCallback(token, exception);
        sendMessageAndAcquireWakeLock(4, token);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void migrationCompleted(int token, IpSecTransform inTransform, IpSecTransform outTransform) {
        sendMessageAndAcquireWakeLock(11, token, new EventMigrationCompletedInfo(inTransform, outTransform));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void childTransformCreated(int token, IpSecTransform transform, int direction) {
        sendMessageAndAcquireWakeLock(5, token, new EventTransformCreatedInfo(direction, transform));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void childOpened(int token, VcnChildSessionConfiguration childConfig) {
        sendMessageAndAcquireWakeLock(6, token, new EventSetupCompletedInfo(childConfig));
    }

    /* loaded from: classes2.dex */
    private abstract class BaseState extends State {
        protected abstract void processStateMsg(Message message) throws Exception;

        private BaseState() {
        }

        public void enter() {
            try {
                enterState();
            } catch (Exception e) {
                VcnGatewayConnection.this.logWtf("Uncaught exception", e);
                VcnGatewayConnection.this.sendDisconnectRequestedAndAcquireWakelock(VcnGatewayConnection.DISCONNECT_REASON_INTERNAL_ERROR + e.toString(), true);
            }
        }

        protected void enterState() throws Exception {
        }

        protected boolean isValidToken(int token) {
            return true;
        }

        public final boolean processMessage(Message msg) {
            int token = msg.arg1;
            if (!isValidToken(token)) {
                VcnGatewayConnection.this.logDbg("Message called with obsolete token: " + token + "; what: " + msg.what);
                return true;
            }
            try {
                processStateMsg(msg);
            } catch (Exception e) {
                VcnGatewayConnection.this.logWtf("Uncaught exception", e);
                VcnGatewayConnection.this.sendDisconnectRequestedAndAcquireWakelock(VcnGatewayConnection.DISCONNECT_REASON_INTERNAL_ERROR + e.toString(), true);
            }
            VcnGatewayConnection.this.maybeReleaseWakeLock();
            return true;
        }

        public void exit() {
            try {
                exitState();
            } catch (Exception e) {
                VcnGatewayConnection.this.logWtf("Uncaught exception", e);
                VcnGatewayConnection.this.sendDisconnectRequestedAndAcquireWakelock(VcnGatewayConnection.DISCONNECT_REASON_INTERNAL_ERROR + e.toString(), true);
            }
        }

        protected void exitState() throws Exception {
        }

        protected void logUnhandledMessage(Message msg) {
            switch (msg.what) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                    logUnexpectedEvent(msg.what);
                    return;
                default:
                    logWtfUnknownEvent(msg.what);
                    return;
            }
        }

        protected void teardownNetwork() {
            if (VcnGatewayConnection.this.mNetworkAgent != null) {
                VcnGatewayConnection.this.mNetworkAgent.unregister();
                VcnGatewayConnection.this.mNetworkAgent = null;
            }
        }

        protected void handleDisconnectRequested(EventDisconnectRequestedInfo info) {
            VcnGatewayConnection.this.logInfo("Tearing down. Cause: " + info.reason + "; quitting = " + info.shouldQuit);
            if (info.shouldQuit) {
                VcnGatewayConnection.this.mIsQuitting.setTrue();
            }
            teardownNetwork();
            if (VcnGatewayConnection.this.mIkeSession == null) {
                VcnGatewayConnection vcnGatewayConnection = VcnGatewayConnection.this;
                vcnGatewayConnection.transitionTo(vcnGatewayConnection.mDisconnectedState);
                return;
            }
            VcnGatewayConnection vcnGatewayConnection2 = VcnGatewayConnection.this;
            vcnGatewayConnection2.transitionTo(vcnGatewayConnection2.mDisconnectingState);
        }

        protected void handleSafeModeTimeoutExceeded() {
            VcnGatewayConnection.this.mSafeModeTimeoutAlarm = null;
            VcnGatewayConnection.this.logInfo("Entering safe mode after timeout exceeded");
            teardownNetwork();
            VcnGatewayConnection.this.mIsInSafeMode = true;
            VcnGatewayConnection.this.mGatewayStatusCallback.onSafeModeStatusChanged();
        }

        protected void logUnexpectedEvent(int what) {
            VcnGatewayConnection.this.logVdbg("Unexpected event code " + what + " in state " + getClass().getSimpleName());
        }

        protected void logWtfUnknownEvent(int what) {
            VcnGatewayConnection.this.logWtf("Unknown event code " + what + " in state " + getClass().getSimpleName());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DisconnectedState extends BaseState {
        private DisconnectedState() {
            super();
        }

        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected void enterState() {
            if (VcnGatewayConnection.this.mIsQuitting.getValue()) {
                VcnGatewayConnection.this.quitNow();
            }
            if (VcnGatewayConnection.this.mIkeSession != null || VcnGatewayConnection.this.mNetworkAgent != null) {
                VcnGatewayConnection.this.logWtf("Active IKE Session or NetworkAgent in DisconnectedState");
            }
            VcnGatewayConnection.this.cancelSafeModeAlarm();
        }

        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected void processStateMsg(Message msg) {
            switch (msg.what) {
                case 1:
                    VcnGatewayConnection.this.mUnderlying = ((EventUnderlyingNetworkChangedInfo) msg.obj).newUnderlying;
                    if (VcnGatewayConnection.this.mUnderlying != null) {
                        VcnGatewayConnection vcnGatewayConnection = VcnGatewayConnection.this;
                        vcnGatewayConnection.transitionTo(vcnGatewayConnection.mConnectingState);
                        return;
                    }
                    return;
                case 7:
                    if (((EventDisconnectRequestedInfo) msg.obj).shouldQuit) {
                        VcnGatewayConnection.this.mIsQuitting.setTrue();
                        VcnGatewayConnection.this.quitNow();
                        return;
                    }
                    return;
                default:
                    logUnhandledMessage(msg);
                    return;
            }
        }

        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected void exitState() {
            VcnGatewayConnection.this.setSafeModeAlarm();
        }
    }

    /* loaded from: classes2.dex */
    private abstract class ActiveBaseState extends BaseState {
        private ActiveBaseState() {
            super();
        }

        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected boolean isValidToken(int token) {
            return token == Integer.MIN_VALUE || token == VcnGatewayConnection.this.mCurrentToken;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DisconnectingState extends ActiveBaseState {
        private boolean mSkipRetryTimeout;

        private DisconnectingState() {
            super();
            this.mSkipRetryTimeout = false;
        }

        public void setSkipRetryTimeout(boolean shouldSkip) {
            this.mSkipRetryTimeout = shouldSkip;
        }

        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected void enterState() throws Exception {
            if (VcnGatewayConnection.this.mIkeSession == null) {
                VcnGatewayConnection.this.logWtf("IKE session was already closed when entering Disconnecting state.");
                VcnGatewayConnection vcnGatewayConnection = VcnGatewayConnection.this;
                vcnGatewayConnection.sendMessageAndAcquireWakeLock(4, vcnGatewayConnection.mCurrentToken);
            } else if (VcnGatewayConnection.this.mUnderlying == null) {
                VcnGatewayConnection.this.mIkeSession.kill();
            } else {
                VcnGatewayConnection.this.mIkeSession.close();
                VcnGatewayConnection.this.setTeardownTimeoutAlarm();
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected void processStateMsg(Message msg) {
            switch (msg.what) {
                case 1:
                    VcnGatewayConnection.this.mUnderlying = ((EventUnderlyingNetworkChangedInfo) msg.obj).newUnderlying;
                    if (VcnGatewayConnection.this.mUnderlying != null) {
                        return;
                    }
                    break;
                case 4:
                    VcnGatewayConnection.this.mIkeSession = null;
                    if (!VcnGatewayConnection.this.mIsQuitting.getValue() && VcnGatewayConnection.this.mUnderlying != null) {
                        VcnGatewayConnection vcnGatewayConnection = VcnGatewayConnection.this;
                        vcnGatewayConnection.transitionTo(this.mSkipRetryTimeout ? vcnGatewayConnection.mConnectingState : vcnGatewayConnection.mRetryTimeoutState);
                        return;
                    }
                    teardownNetwork();
                    VcnGatewayConnection vcnGatewayConnection2 = VcnGatewayConnection.this;
                    vcnGatewayConnection2.transitionTo(vcnGatewayConnection2.mDisconnectedState);
                    return;
                case 7:
                    EventDisconnectRequestedInfo info = (EventDisconnectRequestedInfo) msg.obj;
                    if (info.shouldQuit) {
                        VcnGatewayConnection.this.mIsQuitting.setTrue();
                    }
                    teardownNetwork();
                    if (info.reason.equals(VcnGatewayConnection.DISCONNECT_REASON_UNDERLYING_NETWORK_LOST)) {
                        VcnGatewayConnection.this.mIkeSession.kill();
                        return;
                    }
                    return;
                case 8:
                    break;
                case 10:
                    handleSafeModeTimeoutExceeded();
                    return;
                default:
                    logUnhandledMessage(msg);
                    return;
            }
            VcnGatewayConnection.this.mIkeSession.kill();
        }

        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected void exitState() throws Exception {
            this.mSkipRetryTimeout = false;
            VcnGatewayConnection.this.cancelTeardownTimeoutAlarm();
        }
    }

    /* loaded from: classes2.dex */
    private class ConnectingState extends ActiveBaseState {
        private ConnectingState() {
            super();
        }

        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected void enterState() {
            if (VcnGatewayConnection.this.mIkeSession != null) {
                VcnGatewayConnection.this.logWtf("ConnectingState entered with active session");
                VcnGatewayConnection.this.mIkeSession.kill();
                VcnGatewayConnection.this.mIkeSession = null;
            }
            VcnGatewayConnection vcnGatewayConnection = VcnGatewayConnection.this;
            vcnGatewayConnection.mIkeSession = vcnGatewayConnection.buildIkeSession(vcnGatewayConnection.mUnderlying.network);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected void processStateMsg(Message msg) {
            switch (msg.what) {
                case 1:
                    UnderlyingNetworkRecord oldUnderlying = VcnGatewayConnection.this.mUnderlying;
                    VcnGatewayConnection.this.mUnderlying = ((EventUnderlyingNetworkChangedInfo) msg.obj).newUnderlying;
                    if (oldUnderlying == null) {
                        VcnGatewayConnection.this.logWtf("Old underlying network was null in connected state. Bug?");
                    }
                    if (VcnGatewayConnection.this.mUnderlying == null) {
                        VcnGatewayConnection vcnGatewayConnection = VcnGatewayConnection.this;
                        vcnGatewayConnection.transitionTo(vcnGatewayConnection.mDisconnectingState);
                        return;
                    } else if (oldUnderlying == null || !VcnGatewayConnection.this.mUnderlying.network.equals(oldUnderlying.network)) {
                        VcnGatewayConnection.this.mDisconnectingState.setSkipRetryTimeout(true);
                        break;
                    } else {
                        return;
                    }
                    break;
                case 2:
                case 8:
                case 9:
                case 11:
                default:
                    logUnhandledMessage(msg);
                    return;
                case 3:
                    break;
                case 4:
                    VcnGatewayConnection.this.deferMessage(msg);
                    VcnGatewayConnection vcnGatewayConnection2 = VcnGatewayConnection.this;
                    vcnGatewayConnection2.transitionTo(vcnGatewayConnection2.mDisconnectingState);
                    return;
                case 5:
                case 6:
                case 12:
                    VcnGatewayConnection.this.deferMessage(msg);
                    VcnGatewayConnection vcnGatewayConnection3 = VcnGatewayConnection.this;
                    vcnGatewayConnection3.transitionTo(vcnGatewayConnection3.mConnectedState);
                    return;
                case 7:
                    handleDisconnectRequested((EventDisconnectRequestedInfo) msg.obj);
                    return;
                case 10:
                    handleSafeModeTimeoutExceeded();
                    return;
            }
            VcnGatewayConnection vcnGatewayConnection4 = VcnGatewayConnection.this;
            vcnGatewayConnection4.transitionTo(vcnGatewayConnection4.mDisconnectingState);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public abstract class ConnectedStateBase extends ActiveBaseState {
        private ConnectedStateBase() {
            super();
        }

        protected void updateNetworkAgent(IpSecManager.IpSecTunnelInterface tunnelIface, VcnNetworkAgent agent, VcnChildSessionConfiguration childConfig, IkeSessionConnectionInfo ikeConnectionInfo) {
            NetworkCapabilities caps = VcnGatewayConnection.buildNetworkCapabilities(VcnGatewayConnection.this.mConnectionConfig, VcnGatewayConnection.this.mUnderlying, VcnGatewayConnection.this.mIsMobileDataEnabled);
            VcnGatewayConnection vcnGatewayConnection = VcnGatewayConnection.this;
            LinkProperties lp = vcnGatewayConnection.buildConnectedLinkProperties(vcnGatewayConnection.mConnectionConfig, tunnelIface, childConfig, VcnGatewayConnection.this.mUnderlying, ikeConnectionInfo);
            agent.sendNetworkCapabilities(caps);
            agent.sendLinkProperties(lp);
            agent.setUnderlyingNetworks(VcnGatewayConnection.this.mUnderlying == null ? null : Collections.singletonList(VcnGatewayConnection.this.mUnderlying.network));
        }

        protected VcnNetworkAgent buildNetworkAgent(IpSecManager.IpSecTunnelInterface tunnelIface, VcnChildSessionConfiguration childConfig, IkeSessionConnectionInfo ikeConnectionInfo) {
            NetworkCapabilities caps = VcnGatewayConnection.buildNetworkCapabilities(VcnGatewayConnection.this.mConnectionConfig, VcnGatewayConnection.this.mUnderlying, VcnGatewayConnection.this.mIsMobileDataEnabled);
            VcnGatewayConnection vcnGatewayConnection = VcnGatewayConnection.this;
            LinkProperties lp = vcnGatewayConnection.buildConnectedLinkProperties(vcnGatewayConnection.mConnectionConfig, tunnelIface, childConfig, VcnGatewayConnection.this.mUnderlying, ikeConnectionInfo);
            NetworkAgentConfig nac = new NetworkAgentConfig.Builder().setLegacyType(0).setLegacyTypeName(VcnGatewayConnection.NETWORK_INFO_NETWORK_TYPE_STRING).setLegacySubType(0).setLegacySubTypeName(TelephonyManager.getNetworkTypeName(0)).setLegacyExtraInfo(VcnGatewayConnection.NETWORK_INFO_EXTRA_INFO).build();
            VcnNetworkAgent agent = VcnGatewayConnection.this.mDeps.newNetworkAgent(VcnGatewayConnection.this.mVcnContext, VcnGatewayConnection.TAG, caps, lp, Vcn.getNetworkScore(), nac, VcnGatewayConnection.this.mVcnContext.getVcnNetworkProvider(), new Consumer() { // from class: com.android.server.vcn.VcnGatewayConnection$ConnectedStateBase$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    VcnGatewayConnection.ConnectedStateBase.this.m7506xce6c3b40((VcnGatewayConnection.VcnNetworkAgent) obj);
                }
            }, new Consumer() { // from class: com.android.server.vcn.VcnGatewayConnection$ConnectedStateBase$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    VcnGatewayConnection.ConnectedStateBase.this.m7507x4ccd3f1f((Integer) obj);
                }
            });
            agent.register();
            agent.markConnected();
            return agent;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$buildNetworkAgent$0$com-android-server-vcn-VcnGatewayConnection$ConnectedStateBase  reason: not valid java name */
        public /* synthetic */ void m7506xce6c3b40(VcnNetworkAgent agentRef) {
            if (VcnGatewayConnection.this.mNetworkAgent != agentRef) {
                VcnGatewayConnection.this.logDbg("unwanted() called on stale NetworkAgent");
                return;
            }
            VcnGatewayConnection.this.logInfo(VcnGatewayConnection.DISCONNECT_REASON_NETWORK_AGENT_UNWANTED);
            VcnGatewayConnection.this.teardownAsynchronously();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$buildNetworkAgent$1$com-android-server-vcn-VcnGatewayConnection$ConnectedStateBase  reason: not valid java name */
        public /* synthetic */ void m7507x4ccd3f1f(Integer status) {
            if (VcnGatewayConnection.this.mIsQuitting.getValue()) {
                return;
            }
            switch (status.intValue()) {
                case 1:
                    clearFailedAttemptCounterAndSafeModeAlarm();
                    return;
                case 2:
                    if (VcnGatewayConnection.this.mUnderlying != null) {
                        VcnGatewayConnection.this.mConnectivityManager.reportNetworkConnectivity(VcnGatewayConnection.this.mUnderlying.network, false);
                    }
                    VcnGatewayConnection.this.setSafeModeAlarm();
                    return;
                default:
                    VcnGatewayConnection.this.logWtf("Unknown validation status " + status + "; ignoring");
                    return;
            }
        }

        protected void clearFailedAttemptCounterAndSafeModeAlarm() {
            VcnGatewayConnection.this.mVcnContext.ensureRunningOnLooperThread();
            VcnGatewayConnection.this.mFailedAttempts = 0;
            VcnGatewayConnection.this.cancelSafeModeAlarm();
            VcnGatewayConnection.this.mIsInSafeMode = false;
            VcnGatewayConnection.this.mGatewayStatusCallback.onSafeModeStatusChanged();
        }

        protected void applyTransform(int token, IpSecManager.IpSecTunnelInterface tunnelIface, Network underlyingNetwork, IpSecTransform transform, int direction) {
            if (direction != 0 && direction != 1) {
                VcnGatewayConnection.this.logWtf("Applying transform for unexpected direction: " + direction);
            }
            try {
                tunnelIface.setUnderlyingNetwork(underlyingNetwork);
                VcnGatewayConnection.this.mIpSecManager.applyTunnelModeTransform(tunnelIface, direction, transform);
                Set<Integer> exposedCaps = VcnGatewayConnection.this.mConnectionConfig.getAllExposedCapabilities();
                if (direction == 0 && exposedCaps.contains(2)) {
                    VcnGatewayConnection.this.mIpSecManager.applyTunnelModeTransform(tunnelIface, 2, transform);
                }
            } catch (IOException e) {
                VcnGatewayConnection.this.logInfo("Transform application failed for network " + token, e);
                VcnGatewayConnection.this.sessionLost(token, e);
            }
        }

        protected void setupInterface(int token, IpSecManager.IpSecTunnelInterface tunnelIface, VcnChildSessionConfiguration childConfig, VcnChildSessionConfiguration oldChildConfig) {
            try {
                Collection<? extends LinkAddress> arraySet = new ArraySet<>(childConfig.getInternalAddresses());
                ArraySet arraySet2 = new ArraySet();
                if (oldChildConfig != null) {
                    arraySet2.addAll(oldChildConfig.getInternalAddresses());
                }
                Set<LinkAddress> toAdd = new ArraySet<>();
                toAdd.addAll(arraySet);
                toAdd.removeAll(arraySet2);
                Set<LinkAddress> toRemove = new ArraySet<>();
                toRemove.addAll(arraySet2);
                toRemove.removeAll(arraySet);
                for (LinkAddress address : toAdd) {
                    tunnelIface.addAddress(address.getAddress(), address.getPrefixLength());
                }
                for (LinkAddress address2 : toRemove) {
                    tunnelIface.removeAddress(address2.getAddress(), address2.getPrefixLength());
                }
            } catch (IOException e) {
                VcnGatewayConnection.this.logInfo("Adding address to tunnel failed for token " + token, e);
                VcnGatewayConnection.this.sessionLost(token, e);
            }
        }
    }

    /* loaded from: classes2.dex */
    class ConnectedState extends ConnectedStateBase {
        ConnectedState() {
            super();
        }

        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected void enterState() throws Exception {
            if (VcnGatewayConnection.this.mTunnelIface == null) {
                try {
                    VcnGatewayConnection vcnGatewayConnection = VcnGatewayConnection.this;
                    vcnGatewayConnection.mTunnelIface = vcnGatewayConnection.mIpSecManager.createIpSecTunnelInterface(VcnGatewayConnection.DUMMY_ADDR, VcnGatewayConnection.DUMMY_ADDR, VcnGatewayConnection.this.mUnderlying.network);
                } catch (IpSecManager.ResourceUnavailableException | IOException e) {
                    VcnGatewayConnection.this.teardownAsynchronously();
                }
            }
        }

        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected void processStateMsg(Message msg) {
            switch (msg.what) {
                case 1:
                    handleUnderlyingNetworkChanged(msg);
                    return;
                case 2:
                case 8:
                case 9:
                default:
                    logUnhandledMessage(msg);
                    return;
                case 3:
                    VcnGatewayConnection vcnGatewayConnection = VcnGatewayConnection.this;
                    vcnGatewayConnection.transitionTo(vcnGatewayConnection.mDisconnectingState);
                    return;
                case 4:
                    VcnGatewayConnection.this.deferMessage(msg);
                    VcnGatewayConnection vcnGatewayConnection2 = VcnGatewayConnection.this;
                    vcnGatewayConnection2.transitionTo(vcnGatewayConnection2.mDisconnectingState);
                    return;
                case 5:
                    EventTransformCreatedInfo transformCreatedInfo = (EventTransformCreatedInfo) msg.obj;
                    applyTransform(VcnGatewayConnection.this.mCurrentToken, VcnGatewayConnection.this.mTunnelIface, VcnGatewayConnection.this.mUnderlying.network, transformCreatedInfo.transform, transformCreatedInfo.direction);
                    return;
                case 6:
                    VcnChildSessionConfiguration oldChildConfig = VcnGatewayConnection.this.mChildConfig;
                    VcnGatewayConnection.this.mChildConfig = ((EventSetupCompletedInfo) msg.obj).childSessionConfig;
                    setupInterfaceAndNetworkAgent(VcnGatewayConnection.this.mCurrentToken, VcnGatewayConnection.this.mTunnelIface, VcnGatewayConnection.this.mChildConfig, oldChildConfig, VcnGatewayConnection.this.mIkeConnectionInfo);
                    return;
                case 7:
                    handleDisconnectRequested((EventDisconnectRequestedInfo) msg.obj);
                    return;
                case 10:
                    handleSafeModeTimeoutExceeded();
                    return;
                case 11:
                    EventMigrationCompletedInfo migrationCompletedInfo = (EventMigrationCompletedInfo) msg.obj;
                    handleMigrationCompleted(migrationCompletedInfo);
                    return;
                case 12:
                    VcnGatewayConnection.this.mIkeConnectionInfo = ((EventIkeConnectionInfoChangedInfo) msg.obj).ikeConnectionInfo;
                    return;
            }
        }

        private void handleMigrationCompleted(EventMigrationCompletedInfo migrationCompletedInfo) {
            VcnGatewayConnection.this.logInfo("Migration completed: " + VcnGatewayConnection.this.mUnderlying.network);
            applyTransform(VcnGatewayConnection.this.mCurrentToken, VcnGatewayConnection.this.mTunnelIface, VcnGatewayConnection.this.mUnderlying.network, migrationCompletedInfo.inTransform, 0);
            applyTransform(VcnGatewayConnection.this.mCurrentToken, VcnGatewayConnection.this.mTunnelIface, VcnGatewayConnection.this.mUnderlying.network, migrationCompletedInfo.outTransform, 1);
            updateNetworkAgent(VcnGatewayConnection.this.mTunnelIface, VcnGatewayConnection.this.mNetworkAgent, VcnGatewayConnection.this.mChildConfig, VcnGatewayConnection.this.mIkeConnectionInfo);
            VcnGatewayConnection.this.mConnectivityManager.reportNetworkConnectivity(VcnGatewayConnection.this.mNetworkAgent.getNetwork(), false);
        }

        private void handleUnderlyingNetworkChanged(Message msg) {
            UnderlyingNetworkRecord oldUnderlying = VcnGatewayConnection.this.mUnderlying;
            VcnGatewayConnection.this.mUnderlying = ((EventUnderlyingNetworkChangedInfo) msg.obj).newUnderlying;
            if (VcnGatewayConnection.this.mUnderlying == null) {
                VcnGatewayConnection.this.logInfo("Underlying network lost");
            } else if (oldUnderlying == null || !oldUnderlying.network.equals(VcnGatewayConnection.this.mUnderlying.network)) {
                VcnGatewayConnection.this.logInfo("Migrating to new network: " + VcnGatewayConnection.this.mUnderlying.network);
                VcnGatewayConnection.this.mIkeSession.setNetwork(VcnGatewayConnection.this.mUnderlying.network);
            } else if (VcnGatewayConnection.this.mNetworkAgent != null && VcnGatewayConnection.this.mChildConfig != null) {
                updateNetworkAgent(VcnGatewayConnection.this.mTunnelIface, VcnGatewayConnection.this.mNetworkAgent, VcnGatewayConnection.this.mChildConfig, VcnGatewayConnection.this.mIkeConnectionInfo);
            }
        }

        protected void setupInterfaceAndNetworkAgent(int token, IpSecManager.IpSecTunnelInterface tunnelIface, VcnChildSessionConfiguration childConfig, VcnChildSessionConfiguration oldChildConfig, IkeSessionConnectionInfo ikeConnectionInfo) {
            setupInterface(token, tunnelIface, childConfig, oldChildConfig);
            if (VcnGatewayConnection.this.mNetworkAgent == null) {
                VcnGatewayConnection.this.mNetworkAgent = buildNetworkAgent(tunnelIface, childConfig, ikeConnectionInfo);
                return;
            }
            updateNetworkAgent(tunnelIface, VcnGatewayConnection.this.mNetworkAgent, childConfig, ikeConnectionInfo);
            clearFailedAttemptCounterAndSafeModeAlarm();
        }

        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected void exitState() {
            VcnGatewayConnection.this.setSafeModeAlarm();
        }
    }

    /* loaded from: classes2.dex */
    class RetryTimeoutState extends ActiveBaseState {
        RetryTimeoutState() {
            super();
        }

        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected void enterState() throws Exception {
            VcnGatewayConnection.this.mFailedAttempts++;
            if (VcnGatewayConnection.this.mUnderlying == null) {
                VcnGatewayConnection.this.logWtf("Underlying network was null in retry state");
                teardownNetwork();
                VcnGatewayConnection vcnGatewayConnection = VcnGatewayConnection.this;
                vcnGatewayConnection.transitionTo(vcnGatewayConnection.mDisconnectedState);
                return;
            }
            VcnGatewayConnection.this.setRetryTimeoutAlarm(getNextRetryIntervalsMs());
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        protected void processStateMsg(Message msg) {
            switch (msg.what) {
                case 1:
                    UnderlyingNetworkRecord oldUnderlying = VcnGatewayConnection.this.mUnderlying;
                    VcnGatewayConnection.this.mUnderlying = ((EventUnderlyingNetworkChangedInfo) msg.obj).newUnderlying;
                    if (VcnGatewayConnection.this.mUnderlying == null) {
                        teardownNetwork();
                        VcnGatewayConnection vcnGatewayConnection = VcnGatewayConnection.this;
                        vcnGatewayConnection.transitionTo(vcnGatewayConnection.mDisconnectedState);
                        return;
                    } else if (oldUnderlying != null && VcnGatewayConnection.this.mUnderlying.network.equals(oldUnderlying.network)) {
                        return;
                    }
                    break;
                case 2:
                    break;
                case 7:
                    handleDisconnectRequested((EventDisconnectRequestedInfo) msg.obj);
                    return;
                case 10:
                    handleSafeModeTimeoutExceeded();
                    return;
                default:
                    logUnhandledMessage(msg);
                    return;
            }
            VcnGatewayConnection vcnGatewayConnection2 = VcnGatewayConnection.this;
            vcnGatewayConnection2.transitionTo(vcnGatewayConnection2.mConnectingState);
        }

        @Override // com.android.server.vcn.VcnGatewayConnection.BaseState
        public void exitState() {
            VcnGatewayConnection.this.cancelRetryTimeoutAlarm();
        }

        private long getNextRetryIntervalsMs() {
            int retryDelayIndex = VcnGatewayConnection.this.mFailedAttempts - 1;
            long[] retryIntervalsMs = VcnGatewayConnection.this.mConnectionConfig.getRetryIntervalsMillis();
            if (retryDelayIndex >= retryIntervalsMs.length) {
                return retryIntervalsMs[retryIntervalsMs.length - 1];
            }
            return retryIntervalsMs[retryDelayIndex];
        }
    }

    static NetworkCapabilities buildNetworkCapabilities(VcnGatewayConnectionConfig gatewayConnectionConfig, UnderlyingNetworkRecord underlying, boolean isMobileDataEnabled) {
        int[] iArr;
        int[] adminUids;
        NetworkCapabilities.Builder builder = new NetworkCapabilities.Builder();
        builder.addTransportType(0);
        builder.addCapability(28);
        builder.addCapability(20);
        builder.addCapability(21);
        for (Integer num : gatewayConnectionConfig.getAllExposedCapabilities()) {
            int cap = num.intValue();
            if (isMobileDataEnabled || (cap != 12 && cap != 2)) {
                builder.addCapability(cap);
            }
        }
        if (underlying != null) {
            NetworkCapabilities underlyingCaps = underlying.networkCapabilities;
            for (int cap2 : MERGED_CAPABILITIES) {
                if (underlyingCaps.hasCapability(cap2)) {
                    builder.addCapability(cap2);
                }
            }
            int[] underlyingAdminUids = underlyingCaps.getAdministratorUids();
            Arrays.sort(underlyingAdminUids);
            if (underlyingCaps.getOwnerUid() > 0 && Arrays.binarySearch(underlyingAdminUids, underlyingCaps.getOwnerUid()) < 0) {
                adminUids = Arrays.copyOf(underlyingAdminUids, underlyingAdminUids.length + 1);
                adminUids[adminUids.length - 1] = underlyingCaps.getOwnerUid();
                Arrays.sort(adminUids);
            } else {
                adminUids = underlyingAdminUids;
            }
            builder.setOwnerUid(Process.myUid());
            int[] adminUids2 = Arrays.copyOf(adminUids, adminUids.length + 1);
            adminUids2[adminUids2.length - 1] = Process.myUid();
            builder.setAdministratorUids(adminUids2);
            builder.setLinkUpstreamBandwidthKbps(underlyingCaps.getLinkUpstreamBandwidthKbps());
            builder.setLinkDownstreamBandwidthKbps(underlyingCaps.getLinkDownstreamBandwidthKbps());
            if (!underlyingCaps.hasTransport(1) || !(underlyingCaps.getTransportInfo() instanceof WifiInfo)) {
                if (underlyingCaps.hasTransport(0) && (underlyingCaps.getNetworkSpecifier() instanceof TelephonyNetworkSpecifier)) {
                    TelephonyNetworkSpecifier telNetSpecifier = (TelephonyNetworkSpecifier) underlyingCaps.getNetworkSpecifier();
                    builder.setTransportInfo(new VcnTransportInfo(telNetSpecifier.getSubscriptionId()));
                } else {
                    Slog.wtf(TAG, "Unknown transport type or missing TransportInfo/NetworkSpecifier for non-null underlying network");
                }
            } else {
                WifiInfo wifiInfo = (WifiInfo) underlyingCaps.getTransportInfo();
                builder.setTransportInfo(new VcnTransportInfo(wifiInfo));
            }
            builder.setUnderlyingNetworks(List.of(underlying.network));
        } else {
            Slog.wtf(TAG, "No underlying network while building network capabilities", new IllegalStateException());
        }
        return builder.build();
    }

    LinkProperties buildConnectedLinkProperties(VcnGatewayConnectionConfig gatewayConnectionConfig, IpSecManager.IpSecTunnelInterface tunnelIface, VcnChildSessionConfiguration childConfig, UnderlyingNetworkRecord underlying, IkeSessionConnectionInfo ikeConnectionInfo) {
        IkeTunnelConnectionParams ikeTunnelParams = gatewayConnectionConfig.getTunnelConnectionParams();
        LinkProperties lp = new LinkProperties();
        lp.setInterfaceName(tunnelIface.getInterfaceName());
        for (LinkAddress addr : childConfig.getInternalAddresses()) {
            lp.addLinkAddress(addr);
        }
        for (InetAddress addr2 : childConfig.getInternalDnsServers()) {
            lp.addDnsServer(addr2);
        }
        lp.addRoute(new RouteInfo(new IpPrefix(Inet4Address.ANY, 0), null, null, 1));
        lp.addRoute(new RouteInfo(new IpPrefix(Inet6Address.ANY, 0), null, null, 1));
        int underlyingMtu = 0;
        if (underlying != null) {
            LinkProperties underlyingLp = underlying.linkProperties;
            lp.setTcpBufferSizes(underlyingLp.getTcpBufferSizes());
            underlyingMtu = underlyingLp.getMtu();
            if (underlyingMtu == 0 && underlyingLp.getInterfaceName() != null) {
                underlyingMtu = this.mDeps.getUnderlyingIfaceMtu(underlyingLp.getInterfaceName());
            }
        } else {
            Slog.wtf(TAG, "No underlying network while building link properties", new IllegalStateException());
        }
        lp.setMtu(MtuUtils.getMtu(ikeTunnelParams.getTunnelModeChildSessionParams().getSaProposals(), gatewayConnectionConfig.getMaxMtu(), underlyingMtu, ikeConnectionInfo.getLocalAddress() instanceof Inet4Address));
        return lp;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class IkeSessionCallbackImpl implements IkeSessionCallback {
        private final int mToken;

        IkeSessionCallbackImpl(int token) {
            this.mToken = token;
        }

        @Override // android.net.ipsec.ike.IkeSessionCallback
        public void onOpened(IkeSessionConfiguration ikeSessionConfig) {
            VcnGatewayConnection.this.logDbg("IkeOpened for token " + this.mToken);
            VcnGatewayConnection.this.ikeConnectionInfoChanged(this.mToken, ikeSessionConfig.getIkeSessionConnectionInfo());
        }

        @Override // android.net.ipsec.ike.IkeSessionCallback
        public void onClosed() {
            VcnGatewayConnection.this.logDbg("IkeClosed for token " + this.mToken);
            VcnGatewayConnection.this.sessionClosed(this.mToken, null);
        }

        public void onClosedExceptionally(IkeException exception) {
            VcnGatewayConnection.this.logInfo("IkeClosedExceptionally for token " + this.mToken, exception);
            VcnGatewayConnection.this.sessionClosed(this.mToken, exception);
        }

        public void onError(IkeProtocolException exception) {
            VcnGatewayConnection.this.logInfo("IkeError for token " + this.mToken, exception);
        }

        public void onIkeSessionConnectionInfoChanged(IkeSessionConnectionInfo connectionInfo) {
            VcnGatewayConnection.this.logDbg("onIkeSessionConnectionInfoChanged for token " + this.mToken);
            VcnGatewayConnection.this.ikeConnectionInfoChanged(this.mToken, connectionInfo);
        }
    }

    /* loaded from: classes2.dex */
    public class VcnChildSessionCallback implements ChildSessionCallback {
        private final int mToken;

        VcnChildSessionCallback(int token) {
            this.mToken = token;
        }

        void onOpened(VcnChildSessionConfiguration childConfig) {
            VcnGatewayConnection.this.logDbg("ChildOpened for token " + this.mToken);
            VcnGatewayConnection.this.childOpened(this.mToken, childConfig);
        }

        @Override // android.net.ipsec.ike.ChildSessionCallback
        public void onOpened(ChildSessionConfiguration childConfig) {
            onOpened(new VcnChildSessionConfiguration(childConfig));
        }

        @Override // android.net.ipsec.ike.ChildSessionCallback
        public void onClosed() {
            VcnGatewayConnection.this.logDbg("ChildClosed for token " + this.mToken);
            VcnGatewayConnection.this.sessionLost(this.mToken, null);
        }

        public void onClosedExceptionally(IkeException exception) {
            VcnGatewayConnection.this.logInfo("ChildClosedExceptionally for token " + this.mToken, exception);
            VcnGatewayConnection.this.sessionLost(this.mToken, exception);
        }

        @Override // android.net.ipsec.ike.ChildSessionCallback
        public void onIpSecTransformCreated(IpSecTransform transform, int direction) {
            VcnGatewayConnection.this.logDbg("ChildTransformCreated; Direction: " + direction + "; token " + this.mToken);
            VcnGatewayConnection.this.childTransformCreated(this.mToken, transform, direction);
        }

        public void onIpSecTransformsMigrated(IpSecTransform inIpSecTransform, IpSecTransform outIpSecTransform) {
            VcnGatewayConnection.this.logDbg("ChildTransformsMigrated; token " + this.mToken);
            VcnGatewayConnection.this.migrationCompleted(this.mToken, inIpSecTransform, outIpSecTransform);
        }

        @Override // android.net.ipsec.ike.ChildSessionCallback
        public void onIpSecTransformDeleted(IpSecTransform transform, int direction) {
            VcnGatewayConnection.this.logDbg("ChildTransformDeleted; Direction: " + direction + "; for token " + this.mToken);
        }
    }

    public String getLogPrefix() {
        return "(" + LogUtils.getHashedSubscriptionGroup(this.mSubscriptionGroup) + "-" + this.mConnectionConfig.getGatewayConnectionName() + "-" + System.identityHashCode(this) + ") ";
    }

    private String getTagLogPrefix() {
        return "[ " + TAG + " " + getLogPrefix() + "]";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logVdbg(String msg) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logDbg(String msg) {
        Slog.d(TAG, getLogPrefix() + msg);
    }

    private void logDbg(String msg, Throwable tr) {
        Slog.d(TAG, getLogPrefix() + msg, tr);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logInfo(String msg) {
        Slog.i(TAG, getLogPrefix() + msg);
        VcnManagementService.LOCAL_LOG.log("[INFO] " + getTagLogPrefix() + msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logInfo(String msg, Throwable tr) {
        Slog.i(TAG, getLogPrefix() + msg, tr);
        VcnManagementService.LOCAL_LOG.log("[INFO] " + getTagLogPrefix() + msg + tr);
    }

    private void logWarn(String msg) {
        Slog.w(TAG, getLogPrefix() + msg);
        VcnManagementService.LOCAL_LOG.log("[WARN] " + getTagLogPrefix() + msg);
    }

    private void logWarn(String msg, Throwable tr) {
        Slog.w(TAG, getLogPrefix() + msg, tr);
        VcnManagementService.LOCAL_LOG.log("[WARN] " + getTagLogPrefix() + msg + tr);
    }

    private void logErr(String msg) {
        Slog.e(TAG, getLogPrefix() + msg);
        VcnManagementService.LOCAL_LOG.log("[ERR ] " + getTagLogPrefix() + msg);
    }

    private void logErr(String msg, Throwable tr) {
        Slog.e(TAG, getLogPrefix() + msg, tr);
        VcnManagementService.LOCAL_LOG.log("[ERR ] " + getTagLogPrefix() + msg + tr);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logWtf(String msg) {
        Slog.wtf(TAG, getLogPrefix() + msg);
        VcnManagementService.LOCAL_LOG.log("[WTF ] " + msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logWtf(String msg, Throwable tr) {
        Slog.wtf(TAG, getLogPrefix() + msg, tr);
        VcnManagementService.LOCAL_LOG.log("[WTF ] " + msg + tr);
    }

    public void dump(IndentingPrintWriter pw) {
        String simpleName;
        pw.println("VcnGatewayConnection (" + this.mConnectionConfig.getGatewayConnectionName() + "):");
        pw.increaseIndent();
        StringBuilder append = new StringBuilder().append("Current state: ");
        if (getCurrentState() == null) {
            simpleName = null;
        } else {
            simpleName = getCurrentState().getClass().getSimpleName();
        }
        pw.println(append.append(simpleName).toString());
        pw.println("mIsQuitting: " + this.mIsQuitting.getValue());
        pw.println("mIsInSafeMode: " + this.mIsInSafeMode);
        pw.println("mCurrentToken: " + this.mCurrentToken);
        pw.println("mFailedAttempts: " + this.mFailedAttempts);
        StringBuilder append2 = new StringBuilder().append("mNetworkAgent.getNetwork(): ");
        VcnNetworkAgent vcnNetworkAgent = this.mNetworkAgent;
        pw.println(append2.append(vcnNetworkAgent != null ? vcnNetworkAgent.getNetwork() : null).toString());
        pw.println();
        this.mUnderlyingNetworkController.dump(pw);
        pw.println();
        pw.decreaseIndent();
    }

    void setTunnelInterface(IpSecManager.IpSecTunnelInterface tunnelIface) {
        this.mTunnelIface = tunnelIface;
    }

    UnderlyingNetworkController.UnderlyingNetworkControllerCallback getUnderlyingNetworkControllerCallback() {
        return this.mUnderlyingNetworkControllerCallback;
    }

    UnderlyingNetworkRecord getUnderlyingNetwork() {
        return this.mUnderlying;
    }

    void setUnderlyingNetwork(UnderlyingNetworkRecord record) {
        this.mUnderlying = record;
    }

    IkeSessionConnectionInfo getIkeConnectionInfo() {
        return this.mIkeConnectionInfo;
    }

    boolean isQuitting() {
        return this.mIsQuitting.getValue();
    }

    void setQuitting() {
        this.mIsQuitting.setTrue();
    }

    VcnIkeSession getIkeSession() {
        return this.mIkeSession;
    }

    void setIkeSession(VcnIkeSession session) {
        this.mIkeSession = session;
    }

    VcnNetworkAgent getNetworkAgent() {
        return this.mNetworkAgent;
    }

    void setNetworkAgent(VcnNetworkAgent networkAgent) {
        this.mNetworkAgent = networkAgent;
    }

    void sendDisconnectRequestedAndAcquireWakelock(String reason, boolean shouldQuit) {
        sendMessageAndAcquireWakeLock(7, Integer.MIN_VALUE, new EventDisconnectRequestedInfo(reason, shouldQuit));
    }

    private IkeSessionParams buildIkeParams(Network network) {
        IkeTunnelConnectionParams ikeTunnelConnectionParams = this.mConnectionConfig.getTunnelConnectionParams();
        IkeSessionParams.Builder builder = new IkeSessionParams.Builder(ikeTunnelConnectionParams.getIkeSessionParams());
        builder.setNetwork(network);
        return builder.build();
    }

    private ChildSessionParams buildChildParams() {
        return this.mConnectionConfig.getTunnelConnectionParams().getTunnelModeChildSessionParams();
    }

    VcnIkeSession buildIkeSession(Network network) {
        int token = this.mCurrentToken + 1;
        this.mCurrentToken = token;
        return this.mDeps.newIkeSession(this.mVcnContext, buildIkeParams(network), buildChildParams(), new IkeSessionCallbackImpl(token), new VcnChildSessionCallback(token));
    }

    /* loaded from: classes2.dex */
    public static class Dependencies {
        public UnderlyingNetworkController newUnderlyingNetworkController(VcnContext vcnContext, VcnGatewayConnectionConfig connectionConfig, ParcelUuid subscriptionGroup, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, UnderlyingNetworkController.UnderlyingNetworkControllerCallback callback) {
            return new UnderlyingNetworkController(vcnContext, connectionConfig, subscriptionGroup, snapshot, callback);
        }

        public VcnIkeSession newIkeSession(VcnContext vcnContext, IkeSessionParams ikeSessionParams, ChildSessionParams childSessionParams, IkeSessionCallback ikeSessionCallback, ChildSessionCallback childSessionCallback) {
            return new VcnIkeSession(vcnContext, ikeSessionParams, childSessionParams, ikeSessionCallback, childSessionCallback);
        }

        public VcnWakeLock newWakeLock(Context context, int wakeLockFlag, String wakeLockTag) {
            return new VcnWakeLock(context, wakeLockFlag, wakeLockTag);
        }

        public WakeupMessage newWakeupMessage(VcnContext vcnContext, Handler handler, String tag, Runnable runnable) {
            return new WakeupMessage(vcnContext.getContext(), handler, tag, runnable);
        }

        public VcnNetworkAgent newNetworkAgent(VcnContext vcnContext, String tag, NetworkCapabilities caps, LinkProperties lp, NetworkScore score, NetworkAgentConfig nac, NetworkProvider provider, Consumer<VcnNetworkAgent> networkUnwantedCallback, Consumer<Integer> validationStatusCallback) {
            return new VcnNetworkAgent(vcnContext, tag, caps, lp, score, nac, provider, networkUnwantedCallback, validationStatusCallback);
        }

        public boolean isAirplaneModeOn(VcnContext vcnContext) {
            return Settings.Global.getInt(vcnContext.getContext().getContentResolver(), "airplane_mode_on", 0) != 0;
        }

        public long getElapsedRealTime() {
            return SystemClock.elapsedRealtime();
        }

        public int getUnderlyingIfaceMtu(String ifaceName) {
            try {
                NetworkInterface underlyingIface = NetworkInterface.getByName(ifaceName);
                if (underlyingIface == null) {
                    return 0;
                }
                return underlyingIface.getMTU();
            } catch (IOException e) {
                Slog.d(VcnGatewayConnection.TAG, "Could not get MTU of underlying network", e);
                return 0;
            }
        }
    }

    /* loaded from: classes2.dex */
    public static class VcnChildSessionConfiguration {
        private final ChildSessionConfiguration mChildConfig;

        public VcnChildSessionConfiguration(ChildSessionConfiguration childConfig) {
            this.mChildConfig = childConfig;
        }

        public List<LinkAddress> getInternalAddresses() {
            return this.mChildConfig.getInternalAddresses();
        }

        public List<InetAddress> getInternalDnsServers() {
            return this.mChildConfig.getInternalDnsServers();
        }
    }

    /* loaded from: classes2.dex */
    public static class VcnIkeSession {
        private final IkeSession mImpl;

        public VcnIkeSession(VcnContext vcnContext, IkeSessionParams ikeSessionParams, ChildSessionParams childSessionParams, IkeSessionCallback ikeSessionCallback, ChildSessionCallback childSessionCallback) {
            this.mImpl = new IkeSession(vcnContext.getContext(), ikeSessionParams, childSessionParams, new HandlerExecutor(new Handler(vcnContext.getLooper())), ikeSessionCallback, childSessionCallback);
        }

        public void openChildSession(ChildSessionParams childSessionParams, ChildSessionCallback childSessionCallback) {
            this.mImpl.openChildSession(childSessionParams, childSessionCallback);
        }

        public void closeChildSession(ChildSessionCallback childSessionCallback) {
            this.mImpl.closeChildSession(childSessionCallback);
        }

        public void close() {
            this.mImpl.close();
        }

        public void kill() {
            this.mImpl.kill();
        }

        public void setNetwork(Network network) {
            this.mImpl.setNetwork(network);
        }
    }

    /* loaded from: classes2.dex */
    public static class VcnWakeLock {
        private final PowerManager.WakeLock mImpl;

        public VcnWakeLock(Context context, int flags, String tag) {
            PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
            PowerManager.WakeLock newWakeLock = powerManager.newWakeLock(flags, tag);
            this.mImpl = newWakeLock;
            newWakeLock.setReferenceCounted(false);
        }

        public synchronized void acquire() {
            this.mImpl.acquire();
        }

        public synchronized void release() {
            this.mImpl.release();
        }
    }

    /* loaded from: classes2.dex */
    public static class VcnNetworkAgent {
        private final NetworkAgent mImpl;

        public VcnNetworkAgent(VcnContext vcnContext, String tag, NetworkCapabilities caps, LinkProperties lp, NetworkScore score, NetworkAgentConfig nac, NetworkProvider provider, final Consumer<VcnNetworkAgent> networkUnwantedCallback, final Consumer<Integer> validationStatusCallback) {
            this.mImpl = new NetworkAgent(vcnContext.getContext(), vcnContext.getLooper(), tag, caps, lp, score, nac, provider) { // from class: com.android.server.vcn.VcnGatewayConnection.VcnNetworkAgent.1
                public void onNetworkUnwanted() {
                    networkUnwantedCallback.accept(VcnNetworkAgent.this);
                }

                public void onValidationStatus(int status, Uri redirectUri) {
                    validationStatusCallback.accept(Integer.valueOf(status));
                }
            };
        }

        public void register() {
            this.mImpl.register();
        }

        public void markConnected() {
            this.mImpl.markConnected();
        }

        public void unregister() {
            this.mImpl.unregister();
        }

        public void sendNetworkCapabilities(NetworkCapabilities caps) {
            this.mImpl.sendNetworkCapabilities(caps);
        }

        public void sendLinkProperties(LinkProperties lp) {
            this.mImpl.sendLinkProperties(lp);
        }

        public void setUnderlyingNetworks(List<Network> underlyingNetworks) {
            this.mImpl.setUnderlyingNetworks(underlyingNetworks);
        }

        public Network getNetwork() {
            return this.mImpl.getNetwork();
        }
    }
}
