package com.android.server.trust;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.service.trust.GrantTrustResult;
import android.service.trust.ITrustAgentService;
import android.service.trust.ITrustAgentServiceCallback;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.infra.AndroidFuture;
import java.util.Collections;
import java.util.List;
/* loaded from: classes2.dex */
public class TrustAgentWrapper {
    private static final String DATA_DURATION = "duration";
    private static final String DATA_ESCROW_TOKEN = "escrow_token";
    private static final String DATA_HANDLE = "handle";
    private static final String DATA_MESSAGE = "message";
    private static final String DATA_USER_ID = "user_id";
    private static final boolean DEBUG = TrustManagerService.DEBUG;
    private static final String EXTRA_COMPONENT_NAME = "componentName";
    private static final int MSG_ADD_ESCROW_TOKEN = 7;
    private static final int MSG_ESCROW_TOKEN_STATE = 9;
    private static final int MSG_GRANT_TRUST = 1;
    private static final int MSG_LOCK_USER = 12;
    private static final int MSG_MANAGING_TRUST = 6;
    private static final int MSG_REMOVE_ESCROW_TOKEN = 8;
    private static final int MSG_RESTART_TIMEOUT = 4;
    private static final int MSG_REVOKE_TRUST = 2;
    private static final int MSG_SET_TRUST_AGENT_FEATURES_COMPLETED = 5;
    private static final int MSG_SHOW_KEYGUARD_ERROR_MESSAGE = 11;
    private static final int MSG_TRUST_TIMEOUT = 3;
    private static final int MSG_UNLOCK_USER = 10;
    private static final String PERMISSION = "android.permission.PROVIDE_TRUST_AGENT";
    private static final long RESTART_TIMEOUT_MILLIS = 300000;
    private static final String TAG = "TrustAgentWrapper";
    private static final String TRUST_EXPIRED_ACTION = "android.server.trust.TRUST_EXPIRED_ACTION";
    private final Intent mAlarmIntent;
    private AlarmManager mAlarmManager;
    private PendingIntent mAlarmPendingIntent;
    private boolean mBound;
    private final BroadcastReceiver mBroadcastReceiver;
    private ITrustAgentServiceCallback mCallback;
    private final ServiceConnection mConnection;
    private final Context mContext;
    private boolean mDisplayTrustGrantedMessage;
    private final Handler mHandler;
    private boolean mManagingTrust;
    private long mMaximumTimeToLock;
    private CharSequence mMessage;
    private final ComponentName mName;
    private long mScheduledRestartUptimeMillis;
    private IBinder mSetTrustAgentFeaturesToken;
    private ITrustAgentService mTrustAgentService;
    private boolean mTrustDisabledByDpm;
    private final TrustManagerService mTrustManagerService;
    private boolean mTrustable;
    private final BroadcastReceiver mTrustableDowngradeReceiver;
    private boolean mTrusted;
    private final int mUserId;
    private boolean mPendingSuccessfulUnlock = false;
    private boolean mWaitingForTrustableDowngrade = false;

    public TrustAgentWrapper(Context context, TrustManagerService trustManagerService, Intent intent, UserHandle user) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.trust.TrustAgentWrapper.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent2) {
                if (TrustManagerService.ENABLE_ACTIVE_UNLOCK_FLAG && "android.intent.action.SCREEN_OFF".equals(intent2.getAction())) {
                    TrustAgentWrapper.this.downgradeToTrustable();
                }
            }
        };
        this.mTrustableDowngradeReceiver = broadcastReceiver;
        BroadcastReceiver broadcastReceiver2 = new BroadcastReceiver() { // from class: com.android.server.trust.TrustAgentWrapper.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent2) {
                ComponentName component = (ComponentName) intent2.getParcelableExtra(TrustAgentWrapper.EXTRA_COMPONENT_NAME);
                if (TrustAgentWrapper.TRUST_EXPIRED_ACTION.equals(intent2.getAction()) && TrustAgentWrapper.this.mName.equals(component)) {
                    TrustAgentWrapper.this.mHandler.removeMessages(3);
                    TrustAgentWrapper.this.mHandler.sendEmptyMessage(3);
                }
            }
        };
        this.mBroadcastReceiver = broadcastReceiver2;
        this.mHandler = new Handler() { // from class: com.android.server.trust.TrustAgentWrapper.3
            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                long duration;
                int i = 1;
                switch (msg.what) {
                    case 1:
                        if (!TrustAgentWrapper.this.isConnected()) {
                            Log.w(TrustAgentWrapper.TAG, "Agent is not connected, cannot grant trust: " + TrustAgentWrapper.this.mName.flattenToShortString());
                            return;
                        }
                        TrustAgentWrapper.this.mTrusted = true;
                        TrustAgentWrapper.this.mTrustable = false;
                        Pair<CharSequence, AndroidFuture<GrantTrustResult>> pair = (Pair) msg.obj;
                        TrustAgentWrapper.this.mMessage = (CharSequence) pair.first;
                        AndroidFuture<GrantTrustResult> resultCallback = (AndroidFuture) pair.second;
                        int flags = msg.arg1;
                        TrustAgentWrapper.this.mDisplayTrustGrantedMessage = (flags & 8) != 0;
                        if ((flags & 4) != 0) {
                            TrustAgentWrapper.this.mWaitingForTrustableDowngrade = true;
                        } else {
                            TrustAgentWrapper.this.mWaitingForTrustableDowngrade = false;
                        }
                        long durationMs = msg.getData().getLong(TrustAgentWrapper.DATA_DURATION);
                        if (durationMs > 0) {
                            if (TrustAgentWrapper.this.mMaximumTimeToLock != 0) {
                                duration = Math.min(durationMs, TrustAgentWrapper.this.mMaximumTimeToLock);
                                if (TrustAgentWrapper.DEBUG) {
                                    Slog.d(TrustAgentWrapper.TAG, "DPM lock timeout in effect. Timeout adjusted from " + durationMs + " to " + duration);
                                }
                            } else {
                                duration = durationMs;
                            }
                            long expiration = SystemClock.elapsedRealtime() + duration;
                            TrustAgentWrapper trustAgentWrapper = TrustAgentWrapper.this;
                            trustAgentWrapper.mAlarmPendingIntent = PendingIntent.getBroadcast(trustAgentWrapper.mContext, 0, TrustAgentWrapper.this.mAlarmIntent, AudioFormat.EVRCWB);
                            TrustAgentWrapper.this.mAlarmManager.set(2, expiration, TrustAgentWrapper.this.mAlarmPendingIntent);
                        }
                        TrustAgentWrapper.this.mTrustManagerService.mArchive.logGrantTrust(TrustAgentWrapper.this.mUserId, TrustAgentWrapper.this.mName, TrustAgentWrapper.this.mMessage != null ? TrustAgentWrapper.this.mMessage.toString() : null, durationMs, flags);
                        TrustAgentWrapper.this.mTrustManagerService.updateTrust(TrustAgentWrapper.this.mUserId, flags, resultCallback);
                        return;
                    case 2:
                        break;
                    case 3:
                        if (TrustAgentWrapper.DEBUG) {
                            Slog.d(TrustAgentWrapper.TAG, "Trust timed out : " + TrustAgentWrapper.this.mName.flattenToShortString());
                        }
                        TrustAgentWrapper.this.mTrustManagerService.mArchive.logTrustTimeout(TrustAgentWrapper.this.mUserId, TrustAgentWrapper.this.mName);
                        TrustAgentWrapper.this.onTrustTimeout();
                        break;
                    case 4:
                        Slog.w(TrustAgentWrapper.TAG, "Connection attempt to agent " + TrustAgentWrapper.this.mName.flattenToShortString() + " timed out, rebinding");
                        TrustAgentWrapper.this.destroy();
                        TrustAgentWrapper.this.mTrustManagerService.resetAgent(TrustAgentWrapper.this.mName, TrustAgentWrapper.this.mUserId);
                        return;
                    case 5:
                        IBinder token = (IBinder) msg.obj;
                        boolean result = msg.arg1 != 0;
                        if (TrustAgentWrapper.this.mSetTrustAgentFeaturesToken == token) {
                            TrustAgentWrapper.this.mSetTrustAgentFeaturesToken = null;
                            if (TrustAgentWrapper.this.mTrustDisabledByDpm && result) {
                                if (TrustAgentWrapper.DEBUG) {
                                    Slog.d(TrustAgentWrapper.TAG, "Re-enabling agent because it acknowledged enabled features: " + TrustAgentWrapper.this.mName.flattenToShortString());
                                }
                                TrustAgentWrapper.this.mTrustDisabledByDpm = false;
                                TrustAgentWrapper.this.mTrustManagerService.updateTrust(TrustAgentWrapper.this.mUserId, 0);
                                return;
                            }
                            return;
                        } else if (TrustAgentWrapper.DEBUG) {
                            Slog.w(TrustAgentWrapper.TAG, "Ignoring MSG_SET_TRUST_AGENT_FEATURES_COMPLETED with obsolete token: " + TrustAgentWrapper.this.mName.flattenToShortString());
                            return;
                        } else {
                            return;
                        }
                    case 6:
                        TrustAgentWrapper.this.mManagingTrust = msg.arg1 != 0;
                        if (!TrustAgentWrapper.this.mManagingTrust) {
                            TrustAgentWrapper.this.mTrusted = false;
                            TrustAgentWrapper.this.mDisplayTrustGrantedMessage = false;
                            TrustAgentWrapper.this.mMessage = null;
                        }
                        TrustAgentWrapper.this.mTrustManagerService.mArchive.logManagingTrust(TrustAgentWrapper.this.mUserId, TrustAgentWrapper.this.mName, TrustAgentWrapper.this.mManagingTrust);
                        TrustAgentWrapper.this.mTrustManagerService.updateTrust(TrustAgentWrapper.this.mUserId, 0);
                        return;
                    case 7:
                        byte[] eToken = msg.getData().getByteArray(TrustAgentWrapper.DATA_ESCROW_TOKEN);
                        int userId = msg.getData().getInt(TrustAgentWrapper.DATA_USER_ID);
                        long handle = TrustAgentWrapper.this.mTrustManagerService.addEscrowToken(eToken, userId);
                        boolean resultDeliverred = false;
                        try {
                            if (TrustAgentWrapper.this.mTrustAgentService != null) {
                                TrustAgentWrapper.this.mTrustAgentService.onEscrowTokenAdded(eToken, handle, UserHandle.of(userId));
                                resultDeliverred = true;
                            }
                        } catch (RemoteException e) {
                            TrustAgentWrapper.this.onError(e);
                        }
                        if (!resultDeliverred) {
                            TrustAgentWrapper.this.mTrustManagerService.removeEscrowToken(handle, userId);
                            return;
                        }
                        return;
                    case 8:
                        long handle2 = msg.getData().getLong(TrustAgentWrapper.DATA_HANDLE);
                        boolean success = TrustAgentWrapper.this.mTrustManagerService.removeEscrowToken(handle2, msg.getData().getInt(TrustAgentWrapper.DATA_USER_ID));
                        try {
                            if (TrustAgentWrapper.this.mTrustAgentService != null) {
                                TrustAgentWrapper.this.mTrustAgentService.onEscrowTokenRemoved(handle2, success);
                                return;
                            }
                            return;
                        } catch (RemoteException e2) {
                            TrustAgentWrapper.this.onError(e2);
                            return;
                        }
                    case 9:
                        long handle3 = msg.getData().getLong(TrustAgentWrapper.DATA_HANDLE);
                        boolean active = TrustAgentWrapper.this.mTrustManagerService.isEscrowTokenActive(handle3, msg.getData().getInt(TrustAgentWrapper.DATA_USER_ID));
                        try {
                            if (TrustAgentWrapper.this.mTrustAgentService != null) {
                                ITrustAgentService iTrustAgentService = TrustAgentWrapper.this.mTrustAgentService;
                                if (!active) {
                                    i = 0;
                                }
                                iTrustAgentService.onTokenStateReceived(handle3, i);
                                return;
                            }
                            return;
                        } catch (RemoteException e3) {
                            TrustAgentWrapper.this.onError(e3);
                            return;
                        }
                    case 10:
                        TrustAgentWrapper.this.mTrustManagerService.unlockUserWithToken(msg.getData().getLong(TrustAgentWrapper.DATA_HANDLE), msg.getData().getByteArray(TrustAgentWrapper.DATA_ESCROW_TOKEN), msg.getData().getInt(TrustAgentWrapper.DATA_USER_ID));
                        return;
                    case 11:
                        CharSequence message = msg.getData().getCharSequence(TrustAgentWrapper.DATA_MESSAGE);
                        TrustAgentWrapper.this.mTrustManagerService.showKeyguardErrorMessage(message);
                        return;
                    case 12:
                        TrustAgentWrapper.this.mTrusted = false;
                        TrustAgentWrapper.this.mTrustable = false;
                        TrustAgentWrapper.this.mTrustManagerService.updateTrust(TrustAgentWrapper.this.mUserId, 0);
                        TrustAgentWrapper.this.mTrustManagerService.lockUser(TrustAgentWrapper.this.mUserId);
                        return;
                    default:
                        return;
                }
                TrustAgentWrapper.this.mTrusted = false;
                TrustAgentWrapper.this.mTrustable = false;
                TrustAgentWrapper.this.mWaitingForTrustableDowngrade = false;
                TrustAgentWrapper.this.mDisplayTrustGrantedMessage = false;
                TrustAgentWrapper.this.mMessage = null;
                TrustAgentWrapper.this.mHandler.removeMessages(3);
                if (msg.what == 2) {
                    TrustAgentWrapper.this.mTrustManagerService.mArchive.logRevokeTrust(TrustAgentWrapper.this.mUserId, TrustAgentWrapper.this.mName);
                }
                TrustAgentWrapper.this.mTrustManagerService.updateTrust(TrustAgentWrapper.this.mUserId, 0);
            }
        };
        this.mCallback = new ITrustAgentServiceCallback.Stub() { // from class: com.android.server.trust.TrustAgentWrapper.4
            public void grantTrust(CharSequence message, long durationMs, int flags, AndroidFuture resultCallback) {
                if (TrustAgentWrapper.DEBUG) {
                    Slog.d(TrustAgentWrapper.TAG, "enableTrust(" + ((Object) message) + ", durationMs = " + durationMs + ", flags = " + flags + ")");
                }
                Message msg = TrustAgentWrapper.this.mHandler.obtainMessage(1, flags, 0, Pair.create(message, resultCallback));
                msg.getData().putLong(TrustAgentWrapper.DATA_DURATION, durationMs);
                msg.sendToTarget();
            }

            public void revokeTrust() {
                if (TrustAgentWrapper.DEBUG) {
                    Slog.d(TrustAgentWrapper.TAG, "revokeTrust()");
                }
                TrustAgentWrapper.this.mHandler.sendEmptyMessage(2);
            }

            public void lockUser() {
                TrustAgentWrapper.this.mHandler.sendEmptyMessage(12);
            }

            public void setManagingTrust(boolean managingTrust) {
                if (TrustAgentWrapper.DEBUG) {
                    Slog.d(TrustAgentWrapper.TAG, "managingTrust()");
                }
                TrustAgentWrapper.this.mHandler.obtainMessage(6, managingTrust ? 1 : 0, 0).sendToTarget();
            }

            public void onConfigureCompleted(boolean result, IBinder token) {
                if (TrustAgentWrapper.DEBUG) {
                    Slog.d(TrustAgentWrapper.TAG, "onSetTrustAgentFeaturesEnabledCompleted(result=" + result);
                }
                TrustAgentWrapper.this.mHandler.obtainMessage(5, result ? 1 : 0, 0, token).sendToTarget();
            }

            public void addEscrowToken(byte[] token, int userId) {
                if (TrustAgentWrapper.this.mContext.getResources().getBoolean(17891349)) {
                    throw new SecurityException("Escrow token API is not allowed.");
                }
                if (TrustAgentWrapper.DEBUG) {
                    Slog.d(TrustAgentWrapper.TAG, "adding escrow token for user " + userId);
                }
                Message msg = TrustAgentWrapper.this.mHandler.obtainMessage(7);
                msg.getData().putByteArray(TrustAgentWrapper.DATA_ESCROW_TOKEN, token);
                msg.getData().putInt(TrustAgentWrapper.DATA_USER_ID, userId);
                msg.sendToTarget();
            }

            public void isEscrowTokenActive(long handle, int userId) {
                if (TrustAgentWrapper.this.mContext.getResources().getBoolean(17891349)) {
                    throw new SecurityException("Escrow token API is not allowed.");
                }
                if (TrustAgentWrapper.DEBUG) {
                    Slog.d(TrustAgentWrapper.TAG, "checking the state of escrow token on user " + userId);
                }
                Message msg = TrustAgentWrapper.this.mHandler.obtainMessage(9);
                msg.getData().putLong(TrustAgentWrapper.DATA_HANDLE, handle);
                msg.getData().putInt(TrustAgentWrapper.DATA_USER_ID, userId);
                msg.sendToTarget();
            }

            public void removeEscrowToken(long handle, int userId) {
                if (TrustAgentWrapper.this.mContext.getResources().getBoolean(17891349)) {
                    throw new SecurityException("Escrow token API is not allowed.");
                }
                if (TrustAgentWrapper.DEBUG) {
                    Slog.d(TrustAgentWrapper.TAG, "removing escrow token on user " + userId);
                }
                Message msg = TrustAgentWrapper.this.mHandler.obtainMessage(8);
                msg.getData().putLong(TrustAgentWrapper.DATA_HANDLE, handle);
                msg.getData().putInt(TrustAgentWrapper.DATA_USER_ID, userId);
                msg.sendToTarget();
            }

            public void unlockUserWithToken(long handle, byte[] token, int userId) {
                if (TrustAgentWrapper.this.mContext.getResources().getBoolean(17891349)) {
                    throw new SecurityException("Escrow token API is not allowed.");
                }
                if (TrustAgentWrapper.DEBUG) {
                    Slog.d(TrustAgentWrapper.TAG, "unlocking user " + userId);
                }
                Message msg = TrustAgentWrapper.this.mHandler.obtainMessage(10);
                msg.getData().putInt(TrustAgentWrapper.DATA_USER_ID, userId);
                msg.getData().putLong(TrustAgentWrapper.DATA_HANDLE, handle);
                msg.getData().putByteArray(TrustAgentWrapper.DATA_ESCROW_TOKEN, token);
                msg.sendToTarget();
            }

            public void showKeyguardErrorMessage(CharSequence message) {
                if (TrustAgentWrapper.DEBUG) {
                    Slog.d(TrustAgentWrapper.TAG, "Showing keyguard error message: " + ((Object) message));
                }
                Message msg = TrustAgentWrapper.this.mHandler.obtainMessage(11);
                msg.getData().putCharSequence(TrustAgentWrapper.DATA_MESSAGE, message);
                msg.sendToTarget();
            }
        };
        ServiceConnection serviceConnection = new ServiceConnection() { // from class: com.android.server.trust.TrustAgentWrapper.5
            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (TrustAgentWrapper.DEBUG) {
                    Slog.d(TrustAgentWrapper.TAG, "TrustAgent started : " + name.flattenToString());
                }
                TrustAgentWrapper.this.mHandler.removeMessages(4);
                TrustAgentWrapper.this.mTrustAgentService = ITrustAgentService.Stub.asInterface(service);
                TrustAgentWrapper.this.mTrustManagerService.mArchive.logAgentConnected(TrustAgentWrapper.this.mUserId, name);
                TrustAgentWrapper trustAgentWrapper = TrustAgentWrapper.this;
                trustAgentWrapper.setCallback(trustAgentWrapper.mCallback);
                TrustAgentWrapper.this.updateDevicePolicyFeatures();
                if (TrustAgentWrapper.this.mPendingSuccessfulUnlock) {
                    TrustAgentWrapper.this.onUnlockAttempt(true);
                    TrustAgentWrapper.this.mPendingSuccessfulUnlock = false;
                }
                if (TrustAgentWrapper.this.mTrustManagerService.isDeviceLockedInner(TrustAgentWrapper.this.mUserId)) {
                    TrustAgentWrapper.this.onDeviceLocked();
                } else {
                    TrustAgentWrapper.this.onDeviceUnlocked();
                }
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                if (TrustAgentWrapper.DEBUG) {
                    Slog.d(TrustAgentWrapper.TAG, "TrustAgent disconnected : " + name.flattenToShortString());
                }
                TrustAgentWrapper.this.mTrustAgentService = null;
                TrustAgentWrapper.this.mManagingTrust = false;
                TrustAgentWrapper.this.mSetTrustAgentFeaturesToken = null;
                TrustAgentWrapper.this.mTrustManagerService.mArchive.logAgentDied(TrustAgentWrapper.this.mUserId, name);
                TrustAgentWrapper.this.mHandler.sendEmptyMessage(2);
                if (TrustAgentWrapper.this.mBound) {
                    TrustAgentWrapper.this.scheduleRestart();
                }
            }
        };
        this.mConnection = serviceConnection;
        this.mContext = context;
        this.mTrustManagerService = trustManagerService;
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mUserId = user.getIdentifier();
        ComponentName component = intent.getComponent();
        this.mName = component;
        Intent putExtra = new Intent(TRUST_EXPIRED_ACTION).putExtra(EXTRA_COMPONENT_NAME, component);
        this.mAlarmIntent = putExtra;
        putExtra.setData(Uri.parse(putExtra.toUri(1)));
        putExtra.setPackage(context.getPackageName());
        IntentFilter alarmFilter = new IntentFilter(TRUST_EXPIRED_ACTION);
        alarmFilter.addDataScheme(putExtra.getScheme());
        String pathUri = putExtra.toUri(1);
        alarmFilter.addDataPath(pathUri, 0);
        IntentFilter trustableFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
        scheduleRestart();
        boolean bindServiceAsUser = context.bindServiceAsUser(intent, serviceConnection, AudioFormat.AAC_MAIN, user);
        this.mBound = bindServiceAsUser;
        if (bindServiceAsUser) {
            context.registerReceiver(broadcastReceiver2, alarmFilter, PERMISSION, null, 2);
            context.registerReceiver(broadcastReceiver, trustableFilter);
            return;
        }
        Log.e(TAG, "Can't bind to TrustAgent " + component.flattenToShortString());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onError(Exception e) {
        Slog.w(TAG, "Exception ", e);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTrustTimeout() {
        try {
            ITrustAgentService iTrustAgentService = this.mTrustAgentService;
            if (iTrustAgentService != null) {
                iTrustAgentService.onTrustTimeout();
            }
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void onUnlockAttempt(boolean successful) {
        try {
            ITrustAgentService iTrustAgentService = this.mTrustAgentService;
            if (iTrustAgentService != null) {
                iTrustAgentService.onUnlockAttempt(successful);
            } else {
                this.mPendingSuccessfulUnlock = successful;
            }
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void onUserRequestedUnlock(boolean dismissKeyguard) {
        try {
            ITrustAgentService iTrustAgentService = this.mTrustAgentService;
            if (iTrustAgentService != null) {
                iTrustAgentService.onUserRequestedUnlock(dismissKeyguard);
            }
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void onUserMayRequestUnlock() {
        try {
            ITrustAgentService iTrustAgentService = this.mTrustAgentService;
            if (iTrustAgentService != null) {
                iTrustAgentService.onUserMayRequestUnlock();
            }
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void onUnlockLockout(int timeoutMs) {
        try {
            ITrustAgentService iTrustAgentService = this.mTrustAgentService;
            if (iTrustAgentService != null) {
                iTrustAgentService.onUnlockLockout(timeoutMs);
            }
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void onDeviceLocked() {
        try {
            ITrustAgentService iTrustAgentService = this.mTrustAgentService;
            if (iTrustAgentService != null) {
                iTrustAgentService.onDeviceLocked();
            }
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void onDeviceUnlocked() {
        try {
            ITrustAgentService iTrustAgentService = this.mTrustAgentService;
            if (iTrustAgentService != null) {
                iTrustAgentService.onDeviceUnlocked();
            }
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void onEscrowTokenActivated(long handle, int userId) {
        if (DEBUG) {
            Slog.d(TAG, "onEscrowTokenActivated: " + handle + " user: " + userId);
        }
        ITrustAgentService iTrustAgentService = this.mTrustAgentService;
        if (iTrustAgentService != null) {
            try {
                iTrustAgentService.onTokenStateReceived(handle, 1);
            } catch (RemoteException e) {
                onError(e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCallback(ITrustAgentServiceCallback callback) {
        try {
            ITrustAgentService iTrustAgentService = this.mTrustAgentService;
            if (iTrustAgentService != null) {
                iTrustAgentService.setCallback(callback);
            }
        } catch (RemoteException e) {
            onError(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateDevicePolicyFeatures() {
        boolean trustDisabled = false;
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, "updateDevicePolicyFeatures(" + this.mName + ")");
        }
        try {
            if (this.mTrustAgentService != null) {
                DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
                if ((dpm.getKeyguardDisabledFeatures(null, this.mUserId) & 16) != 0) {
                    List<PersistableBundle> config = dpm.getTrustAgentConfiguration(null, this.mName, this.mUserId);
                    trustDisabled = true;
                    if (z) {
                        Slog.d(TAG, "Detected trust agents disabled. Config = " + config);
                    }
                    if (config != null && config.size() > 0) {
                        if (z) {
                            Slog.d(TAG, "TrustAgent " + this.mName.flattenToShortString() + " disabled until it acknowledges " + config);
                        }
                        Binder binder = new Binder();
                        this.mSetTrustAgentFeaturesToken = binder;
                        this.mTrustAgentService.onConfigure(config, binder);
                    }
                } else {
                    this.mTrustAgentService.onConfigure(Collections.EMPTY_LIST, (IBinder) null);
                }
                long maxTimeToLock = dpm.getMaximumTimeToLock(null, this.mUserId);
                if (maxTimeToLock != this.mMaximumTimeToLock) {
                    this.mMaximumTimeToLock = maxTimeToLock;
                    PendingIntent pendingIntent = this.mAlarmPendingIntent;
                    if (pendingIntent != null) {
                        this.mAlarmManager.cancel(pendingIntent);
                        this.mAlarmPendingIntent = null;
                        this.mHandler.sendEmptyMessage(3);
                    }
                }
            }
        } catch (RemoteException e) {
            onError(e);
        }
        if (this.mTrustDisabledByDpm != trustDisabled) {
            this.mTrustDisabledByDpm = trustDisabled;
            this.mTrustManagerService.updateTrust(this.mUserId, 0);
        }
        return trustDisabled;
    }

    public boolean isTrusted() {
        return this.mTrusted && this.mManagingTrust && !this.mTrustDisabledByDpm;
    }

    public boolean isTrustable() {
        return this.mTrustable && this.mManagingTrust && !this.mTrustDisabledByDpm;
    }

    public void setUntrustable() {
        this.mTrustable = false;
    }

    public void downgradeToTrustable() {
        if (this.mWaitingForTrustableDowngrade) {
            this.mWaitingForTrustableDowngrade = false;
            this.mTrusted = false;
            this.mTrustable = true;
            this.mTrustManagerService.updateTrust(this.mUserId, 0);
        }
    }

    public boolean isManagingTrust() {
        return this.mManagingTrust && !this.mTrustDisabledByDpm;
    }

    public CharSequence getMessage() {
        return this.mMessage;
    }

    public boolean shouldDisplayTrustGrantedMessage() {
        return this.mDisplayTrustGrantedMessage;
    }

    public void destroy() {
        this.mHandler.removeMessages(4);
        if (!this.mBound) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "TrustAgent unbound : " + this.mName.flattenToShortString());
        }
        this.mTrustManagerService.mArchive.logAgentStopped(this.mUserId, this.mName);
        this.mContext.unbindService(this.mConnection);
        this.mBound = false;
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        this.mContext.unregisterReceiver(this.mTrustableDowngradeReceiver);
        this.mTrustAgentService = null;
        this.mSetTrustAgentFeaturesToken = null;
        this.mHandler.sendEmptyMessage(2);
    }

    public boolean isConnected() {
        return this.mTrustAgentService != null;
    }

    public boolean isBound() {
        return this.mBound;
    }

    public long getScheduledRestartUptimeMillis() {
        return this.mScheduledRestartUptimeMillis;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleRestart() {
        this.mHandler.removeMessages(4);
        long uptimeMillis = SystemClock.uptimeMillis() + 300000;
        this.mScheduledRestartUptimeMillis = uptimeMillis;
        this.mHandler.sendEmptyMessageAtTime(4, uptimeMillis);
    }
}
