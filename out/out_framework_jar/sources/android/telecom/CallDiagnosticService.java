package android.telecom;

import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.IBinder;
import android.os.RemoteException;
import android.telecom.Call;
import android.telecom.CallDiagnosticService;
import android.telecom.CallDiagnostics;
import android.telephony.CallQuality;
import android.util.ArrayMap;
import com.android.internal.telecom.ICallDiagnosticService;
import com.android.internal.telecom.ICallDiagnosticServiceAdapter;
import java.util.Map;
import java.util.concurrent.Executor;
@SystemApi
/* loaded from: classes3.dex */
public abstract class CallDiagnosticService extends Service {
    public static final String SERVICE_INTERFACE = "android.telecom.CallDiagnosticService";
    private ICallDiagnosticServiceAdapter mAdapter;
    private CallDiagnostics.Listener mDiagnosticCallListener = new CallDiagnostics.Listener() { // from class: android.telecom.CallDiagnosticService.1
        @Override // android.telecom.CallDiagnostics.Listener
        public void onSendDeviceToDeviceMessage(CallDiagnostics callDiagnostics, int message, int value) {
            CallDiagnosticService.this.handleSendDeviceToDeviceMessage(callDiagnostics, message, value);
        }

        @Override // android.telecom.CallDiagnostics.Listener
        public void onDisplayDiagnosticMessage(CallDiagnostics callDiagnostics, int messageId, CharSequence message) {
            CallDiagnosticService.this.handleDisplayDiagnosticMessage(callDiagnostics, messageId, message);
        }

        @Override // android.telecom.CallDiagnostics.Listener
        public void onClearDiagnosticMessage(CallDiagnostics callDiagnostics, int messageId) {
            CallDiagnosticService.this.handleClearDiagnosticMessage(callDiagnostics, messageId);
        }
    };
    private final Map<String, Call.Details> mCallByTelecomCallId = new ArrayMap();
    private final Map<String, CallDiagnostics> mDiagnosticCallByTelecomCallId = new ArrayMap();
    private final Object mLock = new Object();

    /* JADX DEBUG: Method merged with bridge method */
    /* renamed from: onBluetoothCallQualityReportReceived */
    public abstract void m3841x2bf98454(BluetoothCallQualityReport bluetoothCallQualityReport);

    public abstract void onCallAudioStateChanged(CallAudioState callAudioState);

    public abstract CallDiagnostics onInitializeCallDiagnostics(Call.Details details);

    /* JADX DEBUG: Method merged with bridge method */
    /* renamed from: onRemoveCallDiagnostics */
    public abstract void m3843lambda$handleCallRemoved$2$androidtelecomCallDiagnosticService(CallDiagnostics callDiagnostics);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public final class CallDiagnosticServiceBinder extends ICallDiagnosticService.Stub {
        private CallDiagnosticServiceBinder() {
        }

        @Override // com.android.internal.telecom.ICallDiagnosticService
        public void setAdapter(ICallDiagnosticServiceAdapter adapter) throws RemoteException {
            CallDiagnosticService.this.handleSetAdapter(adapter);
        }

        @Override // com.android.internal.telecom.ICallDiagnosticService
        public void initializeDiagnosticCall(ParcelableCall call) throws RemoteException {
            CallDiagnosticService.this.handleCallAdded(call);
        }

        @Override // com.android.internal.telecom.ICallDiagnosticService
        public void updateCall(ParcelableCall call) throws RemoteException {
            CallDiagnosticService.this.handleCallUpdated(call);
        }

        @Override // com.android.internal.telecom.ICallDiagnosticService
        public void removeDiagnosticCall(String callId) throws RemoteException {
            CallDiagnosticService.this.handleCallRemoved(callId);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateCallAudioState$0$android-telecom-CallDiagnosticService$CallDiagnosticServiceBinder  reason: not valid java name */
        public /* synthetic */ void m3844xa668a299(CallAudioState callAudioState) {
            CallDiagnosticService.this.onCallAudioStateChanged(callAudioState);
        }

        @Override // com.android.internal.telecom.ICallDiagnosticService
        public void updateCallAudioState(final CallAudioState callAudioState) throws RemoteException {
            CallDiagnosticService.this.getExecutor().execute(new Runnable() { // from class: android.telecom.CallDiagnosticService$CallDiagnosticServiceBinder$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    CallDiagnosticService.CallDiagnosticServiceBinder.this.m3844xa668a299(callAudioState);
                }
            });
        }

        @Override // com.android.internal.telecom.ICallDiagnosticService
        public void receiveDeviceToDeviceMessage(String callId, int message, int value) {
            CallDiagnosticService.this.handleReceivedD2DMessage(callId, message, value);
        }

        @Override // com.android.internal.telecom.ICallDiagnosticService
        public void receiveBluetoothCallQualityReport(BluetoothCallQualityReport qualityReport) throws RemoteException {
            CallDiagnosticService.this.handleBluetoothCallQualityReport(qualityReport);
        }

        @Override // com.android.internal.telecom.ICallDiagnosticService
        public void notifyCallDisconnected(String callId, DisconnectCause disconnectCause) throws RemoteException {
            CallDiagnosticService.this.handleCallDisconnected(callId, disconnectCause);
        }

        @Override // com.android.internal.telecom.ICallDiagnosticService
        public void callQualityChanged(String callId, CallQuality callQuality) throws RemoteException {
            CallDiagnosticService.this.handleCallQualityChanged(callId, callQuality);
        }
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        Log.i(this, "onBind!", new Object[0]);
        return new CallDiagnosticServiceBinder();
    }

    public Executor getExecutor() {
        return new HandlerExecutor(Handler.createAsync(getMainLooper()));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSetAdapter(ICallDiagnosticServiceAdapter adapter) {
        this.mAdapter = adapter;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCallAdded(ParcelableCall parcelableCall) {
        final String telecomCallId = parcelableCall.getId();
        Log.i(this, "handleCallAdded: callId=%s - added", telecomCallId);
        final Call.Details newCallDetails = Call.Details.createFromParcelableCall(parcelableCall);
        synchronized (this.mLock) {
            this.mCallByTelecomCallId.put(telecomCallId, newCallDetails);
        }
        getExecutor().execute(new Runnable() { // from class: android.telecom.CallDiagnosticService$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                CallDiagnosticService.this.m3842lambda$handleCallAdded$0$androidtelecomCallDiagnosticService(newCallDetails, telecomCallId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleCallAdded$0$android-telecom-CallDiagnosticService  reason: not valid java name */
    public /* synthetic */ void m3842lambda$handleCallAdded$0$androidtelecomCallDiagnosticService(Call.Details newCallDetails, String telecomCallId) {
        CallDiagnostics callDiagnostics = onInitializeCallDiagnostics(newCallDetails);
        if (callDiagnostics == null) {
            throw new IllegalArgumentException("A valid DiagnosticCall instance was not provided.");
        }
        synchronized (this.mLock) {
            callDiagnostics.setListener(this.mDiagnosticCallListener);
            callDiagnostics.setCallId(telecomCallId);
            this.mDiagnosticCallByTelecomCallId.put(telecomCallId, callDiagnostics);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCallUpdated(ParcelableCall parcelableCall) {
        String telecomCallId = parcelableCall.getId();
        Log.i(this, "handleCallUpdated: callId=%s - updated", telecomCallId);
        final Call.Details newCallDetails = Call.Details.createFromParcelableCall(parcelableCall);
        synchronized (this.mLock) {
            final CallDiagnostics callDiagnostics = this.mDiagnosticCallByTelecomCallId.get(telecomCallId);
            if (callDiagnostics == null) {
                return;
            }
            this.mCallByTelecomCallId.put(telecomCallId, newCallDetails);
            getExecutor().execute(new Runnable() { // from class: android.telecom.CallDiagnosticService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    CallDiagnostics.this.handleCallUpdated(newCallDetails);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCallRemoved(String telecomCallId) {
        final CallDiagnostics callDiagnostics;
        Log.i(this, "handleCallRemoved: callId=%s - removed", telecomCallId);
        synchronized (this.mLock) {
            if (this.mCallByTelecomCallId.containsKey(telecomCallId)) {
                this.mCallByTelecomCallId.remove(telecomCallId);
            }
            if (this.mDiagnosticCallByTelecomCallId.containsKey(telecomCallId)) {
                callDiagnostics = this.mDiagnosticCallByTelecomCallId.remove(telecomCallId);
            } else {
                callDiagnostics = null;
            }
        }
        if (callDiagnostics != null) {
            getExecutor().execute(new Runnable() { // from class: android.telecom.CallDiagnosticService$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    CallDiagnosticService.this.m3843lambda$handleCallRemoved$2$androidtelecomCallDiagnosticService(callDiagnostics);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleReceivedD2DMessage(String callId, final int message, final int value) {
        final CallDiagnostics callDiagnostics;
        Log.i(this, "handleReceivedD2DMessage: callId=%s, msg=%d/%d", callId, Integer.valueOf(message), Integer.valueOf(value));
        synchronized (this.mLock) {
            callDiagnostics = this.mDiagnosticCallByTelecomCallId.get(callId);
        }
        if (callDiagnostics != null) {
            getExecutor().execute(new Runnable() { // from class: android.telecom.CallDiagnosticService$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    CallDiagnostics.this.onReceiveDeviceToDeviceMessage(message, value);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCallDisconnected(String callId, DisconnectCause disconnectCause) {
        CallDiagnostics callDiagnostics;
        CharSequence message;
        Log.i(this, "handleCallDisconnected: call=%s; cause=%s", callId, disconnectCause);
        synchronized (this.mLock) {
            callDiagnostics = this.mDiagnosticCallByTelecomCallId.get(callId);
        }
        if (disconnectCause.getImsReasonInfo() != null) {
            message = callDiagnostics.onCallDisconnected(disconnectCause.getImsReasonInfo());
        } else {
            message = callDiagnostics.onCallDisconnected(disconnectCause.getTelephonyDisconnectCause(), disconnectCause.getTelephonyPreciseDisconnectCause());
        }
        try {
            this.mAdapter.overrideDisconnectMessage(callId, message);
        } catch (RemoteException e) {
            Log.w(this, "handleCallDisconnected: call=%s; cause=%s; %s", callId, disconnectCause, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleBluetoothCallQualityReport(final BluetoothCallQualityReport qualityReport) {
        Log.i(this, "handleBluetoothCallQualityReport; report=%s", qualityReport);
        getExecutor().execute(new Runnable() { // from class: android.telecom.CallDiagnosticService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                CallDiagnosticService.this.m3841x2bf98454(qualityReport);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCallQualityChanged(String callId, CallQuality callQuality) {
        CallDiagnostics callDiagnostics;
        Log.i(this, "handleCallQualityChanged; call=%s, cq=%s", callId, callQuality);
        synchronized (this.mLock) {
            callDiagnostics = this.mDiagnosticCallByTelecomCallId.get(callId);
        }
        if (callDiagnostics != null) {
            callDiagnostics.onCallQualityReceived(callQuality);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSendDeviceToDeviceMessage(CallDiagnostics callDiagnostics, int message, int value) {
        String callId = callDiagnostics.getCallId();
        try {
            this.mAdapter.sendDeviceToDeviceMessage(callId, message, value);
            Log.i(this, "handleSendDeviceToDeviceMessage: call=%s; msg=%d/%d", callId, Integer.valueOf(message), Integer.valueOf(value));
        } catch (RemoteException e) {
            Log.w(this, "handleSendDeviceToDeviceMessage: call=%s; msg=%d/%d failed %s", callId, Integer.valueOf(message), Integer.valueOf(value), e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDisplayDiagnosticMessage(CallDiagnostics callDiagnostics, int messageId, CharSequence message) {
        String callId = callDiagnostics.getCallId();
        try {
            this.mAdapter.displayDiagnosticMessage(callId, messageId, message);
            Log.i(this, "handleDisplayDiagnosticMessage: call=%s; msg=%d/%s", callId, Integer.valueOf(messageId), message);
        } catch (RemoteException e) {
            Log.w(this, "handleDisplayDiagnosticMessage: call=%s; msg=%d/%s failed %s", callId, Integer.valueOf(messageId), message, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleClearDiagnosticMessage(CallDiagnostics callDiagnostics, int messageId) {
        String callId = callDiagnostics.getCallId();
        try {
            this.mAdapter.clearDiagnosticMessage(callId, messageId);
            Log.i(this, "handleClearDiagnosticMessage: call=%s; msg=%d", callId, Integer.valueOf(messageId));
        } catch (RemoteException e) {
            Log.w(this, "handleClearDiagnosticMessage: call=%s; msg=%d failed %s", callId, Integer.valueOf(messageId), e);
        }
    }
}
