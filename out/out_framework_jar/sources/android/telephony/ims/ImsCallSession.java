package android.telephony.ims;

import android.app.PendingIntent$$ExternalSyntheticLambda1;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.CallQuality;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.aidl.IImsCallSessionListener;
import android.util.ArraySet;
import android.util.Log;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsVideoCallProvider;
import com.android.internal.telephony.util.TelephonyUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
/* loaded from: classes3.dex */
public class ImsCallSession {
    private static final String TAG = "ImsCallSession";
    protected boolean mClosed;
    protected Listener mListener;
    private Executor mListenerExecutor;
    protected IImsCallSession miSession;

    /* loaded from: classes3.dex */
    public static class State {
        public static final int ESTABLISHED = 4;
        public static final int ESTABLISHING = 3;
        public static final int IDLE = 0;
        public static final int INITIATED = 1;
        public static final int INVALID = -1;
        public static final int NEGOTIATING = 2;
        public static final int REESTABLISHING = 6;
        public static final int RENEGOTIATING = 5;
        public static final int TERMINATED = 8;
        public static final int TERMINATING = 7;

        public static String toString(int state) {
            switch (state) {
                case 0:
                    return "IDLE";
                case 1:
                    return "INITIATED";
                case 2:
                    return "NEGOTIATING";
                case 3:
                    return "ESTABLISHING";
                case 4:
                    return "ESTABLISHED";
                case 5:
                    return "RENEGOTIATING";
                case 6:
                    return "REESTABLISHING";
                case 7:
                    return "TERMINATING";
                case 8:
                    return "TERMINATED";
                default:
                    return "UNKNOWN";
            }
        }

        private State() {
        }
    }

    /* loaded from: classes3.dex */
    public static class Listener {
        public void callSessionInitiating(ImsCallSession session, ImsCallProfile profile) {
        }

        public void callSessionInitiatingFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
        }

        public void callSessionProgressing(ImsCallSession session, ImsStreamMediaProfile profile) {
        }

        public void callSessionStarted(ImsCallSession session, ImsCallProfile profile) {
        }

        public void callSessionStartFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
        }

        public void callSessionTerminated(ImsCallSession session, ImsReasonInfo reasonInfo) {
        }

        public void callSessionHeld(ImsCallSession session, ImsCallProfile profile) {
        }

        public void callSessionHoldFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
        }

        public void callSessionHoldReceived(ImsCallSession session, ImsCallProfile profile) {
        }

        public void callSessionResumed(ImsCallSession session, ImsCallProfile profile) {
        }

        public void callSessionResumeFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
        }

        public void callSessionResumeReceived(ImsCallSession session, ImsCallProfile profile) {
        }

        public void callSessionMergeStarted(ImsCallSession session, ImsCallSession newSession, ImsCallProfile profile) {
        }

        public void callSessionMergeComplete(ImsCallSession session) {
        }

        public void callSessionMergeFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
        }

        public void callSessionUpdated(ImsCallSession session, ImsCallProfile profile) {
        }

        public void callSessionUpdateFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
        }

        public void callSessionUpdateReceived(ImsCallSession session, ImsCallProfile profile) {
        }

        public void callSessionConferenceExtended(ImsCallSession session, ImsCallSession newSession, ImsCallProfile profile) {
        }

        public void callSessionConferenceExtendFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
        }

        public void callSessionConferenceExtendReceived(ImsCallSession session, ImsCallSession newSession, ImsCallProfile profile) {
        }

        public void callSessionInviteParticipantsRequestDelivered(ImsCallSession session) {
        }

        public void callSessionInviteParticipantsRequestFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
        }

        public void callSessionRemoveParticipantsRequestDelivered(ImsCallSession session) {
        }

        public void callSessionRemoveParticipantsRequestFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
        }

        public void callSessionConferenceStateUpdated(ImsCallSession session, ImsConferenceState state) {
        }

        public void callSessionUssdMessageReceived(ImsCallSession session, int mode, String ussdMessage) {
        }

        public void callSessionMayHandover(ImsCallSession session, int srcNetworkType, int targetNetworkType) {
        }

        public void callSessionHandover(ImsCallSession session, int srcNetworkType, int targetNetworkType, ImsReasonInfo reasonInfo) {
        }

        public void callSessionHandoverFailed(ImsCallSession session, int srcNetworkType, int targetNetworkType, ImsReasonInfo reasonInfo) {
        }

        public void callSessionTtyModeReceived(ImsCallSession session, int mode) {
        }

        public void callSessionMultipartyStateChanged(ImsCallSession session, boolean isMultiParty) {
        }

        public void callSessionSuppServiceReceived(ImsCallSession session, ImsSuppServiceNotification suppServiceInfo) {
        }

        public void callSessionRttModifyRequestReceived(ImsCallSession session, ImsCallProfile callProfile) {
        }

        public void callSessionRttModifyResponseReceived(int status) {
        }

        public void callSessionRttMessageReceived(String rttMessage) {
        }

        public void callSessionRttAudioIndicatorChanged(ImsStreamMediaProfile profile) {
        }

        public void callSessionTransferred(ImsCallSession session) {
        }

        public void callSessionTransferFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
        }

        public void callSessionDtmfReceived(char digit) {
        }

        public void callQualityChanged(CallQuality callQuality) {
        }

        public void callSessionRtpHeaderExtensionsReceived(Set<RtpHeaderExtension> extensions) {
        }

        public void callSessionDeviceSwitched(ImsCallSession session) {
        }

        public void callSessionDeviceSwitchFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
        }

        public void callSessionTextCapabilityChanged(ImsCallSession session, int localCapability, int remoteCapability, int localTextStatus, int realRemoteCapability) {
        }

        public void callSessionRttEventReceived(ImsCallSession session, int event) {
        }

        public void callSessionRedialEcc(ImsCallSession session, boolean isNeedUserConfirm) {
        }

        public void callSessionRinging(ImsCallSession session, ImsCallProfile profile) {
        }

        public void callSessionBusy(ImsCallSession session) {
        }

        public void callSessionCalling(ImsCallSession session) {
        }

        public void callSessionVideoRingtoneEventReceived(ImsCallSession session, int eventType, String event) {
        }

        public void callSessionNotificationRingtoneReceived(ImsCallSession session, int causeNum, String causeText) {
        }
    }

    protected ImsCallSession() {
        this.mClosed = false;
        this.mListenerExecutor = new PendingIntent$$ExternalSyntheticLambda1();
        this.miSession = null;
    }

    public ImsCallSession(IImsCallSession iSession) {
        this.mClosed = false;
        this.mListenerExecutor = new PendingIntent$$ExternalSyntheticLambda1();
        this.miSession = iSession;
        if (iSession != null) {
            try {
                iSession.setListener(new IImsCallSessionListenerProxy());
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        this.mClosed = true;
    }

    public ImsCallSession(IImsCallSession iSession, Listener listener, Executor executor) {
        this(iSession);
        setListener(listener, executor);
    }

    public void close() {
        synchronized (this) {
            if (this.mClosed) {
                return;
            }
            try {
                this.miSession.close();
                this.mClosed = true;
            } catch (RemoteException e) {
            }
        }
    }

    public String getCallId() {
        if (this.mClosed) {
            return null;
        }
        try {
            return this.miSession.getCallId();
        } catch (RemoteException e) {
            return null;
        }
    }

    public ImsCallProfile getCallProfile() {
        if (this.mClosed) {
            return null;
        }
        try {
            return this.miSession.getCallProfile();
        } catch (RemoteException e) {
            return null;
        }
    }

    public ImsCallProfile getLocalCallProfile() {
        if (this.mClosed) {
            return null;
        }
        try {
            return this.miSession.getLocalCallProfile();
        } catch (RemoteException e) {
            return null;
        }
    }

    public ImsCallProfile getRemoteCallProfile() {
        if (this.mClosed) {
            return null;
        }
        try {
            return this.miSession.getRemoteCallProfile();
        } catch (RemoteException e) {
            return null;
        }
    }

    public IImsVideoCallProvider getVideoCallProvider() {
        if (this.mClosed) {
            return null;
        }
        try {
            return this.miSession.getVideoCallProvider();
        } catch (RemoteException e) {
            return null;
        }
    }

    public String getProperty(String name) {
        if (this.mClosed) {
            return null;
        }
        try {
            return this.miSession.getProperty(name);
        } catch (RemoteException e) {
            return null;
        }
    }

    public int getState() {
        if (this.mClosed) {
            return -1;
        }
        try {
            return this.miSession.getState();
        } catch (RemoteException e) {
            return -1;
        }
    }

    public boolean isAlive() {
        if (this.mClosed) {
            return false;
        }
        int state = getState();
        switch (state) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                return true;
            default:
                return false;
        }
    }

    public IImsCallSession getSession() {
        return this.miSession;
    }

    public boolean isInCall() {
        if (this.mClosed) {
            return false;
        }
        try {
            return this.miSession.isInCall();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setListener(Listener listener, Executor executor) {
        this.mListener = listener;
        if (executor != null) {
            this.mListenerExecutor = executor;
        }
    }

    public void setMute(boolean muted) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.setMute(muted);
        } catch (RemoteException e) {
        }
    }

    public void start(String callee, ImsCallProfile profile) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.start(callee, profile);
        } catch (RemoteException e) {
        }
    }

    public void start(String[] participants, ImsCallProfile profile) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.startConference(participants, profile);
        } catch (RemoteException e) {
        }
    }

    public void accept(int callType, ImsStreamMediaProfile profile) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.accept(callType, profile);
        } catch (RemoteException e) {
        }
    }

    public void deflect(String number) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.deflect(number);
        } catch (RemoteException e) {
        }
    }

    public void reject(int reason) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.reject(reason);
        } catch (RemoteException e) {
        }
    }

    public void transfer(String number, boolean isConfirmationRequired) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.transfer(number, isConfirmationRequired);
        } catch (RemoteException e) {
        }
    }

    public void transfer(ImsCallSession transferToSession) {
        if (!this.mClosed && transferToSession != null) {
            try {
                this.miSession.consultativeTransfer(transferToSession.getSession());
            } catch (RemoteException e) {
            }
        }
    }

    public void terminate(int reason) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.terminate(reason);
        } catch (RemoteException e) {
        }
    }

    public void hold(ImsStreamMediaProfile profile) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.hold(profile);
        } catch (RemoteException e) {
        }
    }

    public void resume(ImsStreamMediaProfile profile) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.resume(profile);
        } catch (RemoteException e) {
        }
    }

    public void merge() {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.merge();
        } catch (RemoteException e) {
        }
    }

    public void update(int callType, ImsStreamMediaProfile profile) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.update(callType, profile);
        } catch (RemoteException e) {
        }
    }

    public void extendToConference(String[] participants) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.extendToConference(participants);
        } catch (RemoteException e) {
        }
    }

    public void inviteParticipants(String[] participants) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.inviteParticipants(participants);
        } catch (RemoteException e) {
        }
    }

    public void removeParticipants(String[] participants) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.removeParticipants(participants);
        } catch (RemoteException e) {
        }
    }

    public void sendDtmf(char c, Message result) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.sendDtmf(c, result);
        } catch (RemoteException e) {
        }
    }

    public void startDtmf(char c) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.startDtmf(c);
        } catch (RemoteException e) {
        }
    }

    public void stopDtmf() {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.stopDtmf();
        } catch (RemoteException e) {
        }
    }

    public void sendUssd(String ussdMessage) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.sendUssd(ussdMessage);
        } catch (RemoteException e) {
        }
    }

    public boolean isMultiparty() {
        if (this.mClosed) {
            return false;
        }
        try {
            return this.miSession.isMultiparty();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void sendRttMessage(String rttMessage) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.sendRttMessage(rttMessage);
        } catch (RemoteException e) {
        }
    }

    public void sendRttModifyRequest(ImsCallProfile to) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.sendRttModifyRequest(to);
        } catch (RemoteException e) {
        }
    }

    public void sendRttModifyResponse(boolean response) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.sendRttModifyResponse(response);
        } catch (RemoteException e) {
        }
    }

    public void sendRtpHeaderExtensions(Set<RtpHeaderExtension> rtpHeaderExtensions) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.sendRtpHeaderExtensions(new ArrayList(rtpHeaderExtensions));
        } catch (RemoteException e) {
        }
    }

    /* loaded from: classes3.dex */
    public class IImsCallSessionListenerProxy extends IImsCallSessionListener.Stub {
        public IImsCallSessionListenerProxy() {
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionInitiating(final ImsCallProfile profile) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda40
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4245x81b87a6b(profile);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionInitiating$0$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4245x81b87a6b(ImsCallProfile profile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionInitiating(ImsCallSession.this, profile);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionProgressing(final ImsStreamMediaProfile profile) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4253x875ec3d5(profile);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionProgressing$1$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4253x875ec3d5(ImsStreamMediaProfile profile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionProgressing(ImsCallSession.this, profile);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionInitiated(final ImsCallProfile profile) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda16
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4243x3905c82c(profile);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionInitiated$2$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4243x3905c82c(ImsCallProfile profile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionStarted(ImsCallSession.this, profile);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionInitiatingFailed(final ImsReasonInfo reasonInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda25
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4246x4df416cb(reasonInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionInitiatingFailed$3$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4246x4df416cb(ImsReasonInfo reasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionStartFailed(ImsCallSession.this, reasonInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionInitiatedFailed(final ImsReasonInfo reasonInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda14
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4244x68ffd8b(reasonInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionInitiatedFailed$4$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4244x68ffd8b(ImsReasonInfo reasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionStartFailed(ImsCallSession.this, reasonInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionTerminated(final ImsReasonInfo reasonInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda24
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4265x20714fbd(reasonInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionTerminated$5$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4265x20714fbd(ImsReasonInfo reasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionTerminated(ImsCallSession.this, reasonInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionHeld(final ImsCallProfile profile) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda30
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4240x81a863d0(profile);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionHeld$6$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4240x81a863d0(ImsCallProfile profile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionHeld(ImsCallSession.this, profile);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionHoldFailed(final ImsReasonInfo reasonInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4241x96361678(reasonInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionHoldFailed$7$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4241x96361678(ImsReasonInfo reasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionHoldFailed(ImsCallSession.this, reasonInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionHoldReceived(final ImsCallProfile profile) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda33
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4242x680d3f5d(profile);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionHoldReceived$8$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4242x680d3f5d(ImsCallProfile profile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionHoldReceived(ImsCallSession.this, profile);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionResumed(final ImsCallProfile profile) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda17
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4258x13cede9f(profile);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionResumed$9$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4258x13cede9f(ImsCallProfile profile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionResumed(ImsCallSession.this, profile);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionResumeFailed(final ImsReasonInfo reasonInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda22
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4256x3ba4352c(reasonInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionResumeFailed$10$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4256x3ba4352c(ImsReasonInfo reasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionResumeFailed(ImsCallSession.this, reasonInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionResumeReceived(final ImsCallProfile profile) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda21
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4257xbea04bc9(profile);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionResumeReceived$11$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4257xbea04bc9(ImsCallProfile profile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionResumeReceived(ImsCallSession.this, profile);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionMergeStarted(IImsCallSession newSession, ImsCallProfile profile) {
            Log.d(ImsCallSession.TAG, "callSessionMergeStarted");
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionMergeComplete(final IImsCallSession newSession) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4250xd579a25d(newSession);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionMergeComplete$12$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4250xd579a25d(IImsCallSession newSession) {
            if (ImsCallSession.this.mListener != null) {
                if (newSession != null) {
                    ImsCallSession.this.mListener.callSessionMergeComplete(new ImsCallSession(newSession));
                } else {
                    ImsCallSession.this.mListener.callSessionMergeComplete(null);
                }
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionMergeFailed(final ImsReasonInfo reasonInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda19
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4251x261b025a(reasonInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionMergeFailed$13$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4251x261b025a(ImsReasonInfo reasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionMergeFailed(ImsCallSession.this, reasonInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionUpdated(final ImsCallProfile profile) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda35
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4271xa1209535(profile);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionUpdated$14$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4271xa1209535(ImsCallProfile profile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionUpdated(ImsCallSession.this, profile);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionUpdateFailed(final ImsReasonInfo reasonInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda20
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4269x5772d5d5(reasonInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionUpdateFailed$15$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4269x5772d5d5(ImsReasonInfo reasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionUpdateFailed(ImsCallSession.this, reasonInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionUpdateReceived(final ImsCallProfile profile) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda23
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4270xb5790372(profile);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionUpdateReceived$16$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4270xb5790372(ImsCallProfile profile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionUpdateReceived(ImsCallSession.this, profile);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionConferenceExtended(final IImsCallSession newSession, final ImsCallProfile profile) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4235x3fe8f2e8(newSession, profile);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionConferenceExtended$17$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4235x3fe8f2e8(IImsCallSession newSession, ImsCallProfile profile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionConferenceExtended(ImsCallSession.this, new ImsCallSession(newSession), profile);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionConferenceExtendFailed(final ImsReasonInfo reasonInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda36
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4233xa55ac22b(reasonInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionConferenceExtendFailed$18$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4233xa55ac22b(ImsReasonInfo reasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionConferenceExtendFailed(ImsCallSession.this, reasonInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionConferenceExtendReceived(final IImsCallSession newSession, final ImsCallProfile profile) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda27
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4234xe866f708(newSession, profile);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionConferenceExtendReceived$19$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4234xe866f708(IImsCallSession newSession, ImsCallProfile profile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionConferenceExtendReceived(ImsCallSession.this, new ImsCallSession(newSession), profile);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionInviteParticipantsRequestDelivered() {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda28
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4247xc04f00f7();
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionInviteParticipantsRequestDelivered$20$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4247xc04f00f7() {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionInviteParticipantsRequestDelivered(ImsCallSession.this);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionInviteParticipantsRequestFailed(final ImsReasonInfo reasonInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4248x98179229(reasonInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionInviteParticipantsRequestFailed$21$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4248x98179229(ImsReasonInfo reasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionInviteParticipantsRequestFailed(ImsCallSession.this, reasonInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionRemoveParticipantsRequestDelivered() {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda26
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4254x3a77e25e();
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionRemoveParticipantsRequestDelivered$22$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4254x3a77e25e() {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionRemoveParticipantsRequestDelivered(ImsCallSession.this);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionRemoveParticipantsRequestFailed(final ImsReasonInfo reasonInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4255x96a671e6(reasonInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionRemoveParticipantsRequestFailed$23$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4255x96a671e6(ImsReasonInfo reasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionRemoveParticipantsRequestFailed(ImsCallSession.this, reasonInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionConferenceStateUpdated(final ImsConferenceState state) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda18
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4236x4de12c73(state);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionConferenceStateUpdated$24$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4236x4de12c73(ImsConferenceState state) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionConferenceStateUpdated(ImsCallSession.this, state);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionUssdMessageReceived(final int mode, final String ussdMessage) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda31
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4272xf74c72b7(mode, ussdMessage);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionUssdMessageReceived$25$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4272xf74c72b7(int mode, String ussdMessage) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionUssdMessageReceived(ImsCallSession.this, mode, ussdMessage);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionMayHandover(final int srcNetworkType, final int targetNetworkType) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda32
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4249x6d67a29(srcNetworkType, targetNetworkType);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionMayHandover$26$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4249x6d67a29(int srcNetworkType, int targetNetworkType) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionMayHandover(ImsCallSession.this, srcNetworkType, targetNetworkType);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionHandover(final int srcNetworkType, final int targetNetworkType, final ImsReasonInfo reasonInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4238xab063ef9(srcNetworkType, targetNetworkType, reasonInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionHandover$27$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4238xab063ef9(int srcNetworkType, int targetNetworkType, ImsReasonInfo reasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionHandover(ImsCallSession.this, srcNetworkType, targetNetworkType, reasonInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionHandoverFailed(final int srcNetworkType, final int targetNetworkType, final ImsReasonInfo reasonInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4239xbeb322fd(srcNetworkType, targetNetworkType, reasonInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionHandoverFailed$28$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4239xbeb322fd(int srcNetworkType, int targetNetworkType, ImsReasonInfo reasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionHandoverFailed(ImsCallSession.this, srcNetworkType, targetNetworkType, reasonInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionTtyModeReceived(final int mode) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda29
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4268xa4628617(mode);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionTtyModeReceived$29$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4268xa4628617(int mode) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionTtyModeReceived(ImsCallSession.this, mode);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionMultipartyStateChanged(final boolean isMultiParty) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda39
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4252x3728c764(isMultiParty);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionMultipartyStateChanged$30$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4252x3728c764(boolean isMultiParty) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionMultipartyStateChanged(ImsCallSession.this, isMultiParty);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionSuppServiceReceived(final ImsSuppServiceNotification suppServiceInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4264xec4317(suppServiceInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionSuppServiceReceived$31$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4264xec4317(ImsSuppServiceNotification suppServiceInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionSuppServiceReceived(ImsCallSession.this, suppServiceInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionRttModifyRequestReceived(final ImsCallProfile callProfile) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda34
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4262xb7fbcad2(callProfile);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionRttModifyRequestReceived$32$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4262xb7fbcad2(ImsCallProfile callProfile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionRttModifyRequestReceived(ImsCallSession.this, callProfile);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionRttModifyResponseReceived(final int status) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda37
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4263x806d8adf(status);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionRttModifyResponseReceived$33$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4263x806d8adf(int status) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionRttModifyResponseReceived(status);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionRttMessageReceived(final String rttMessage) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4261x49ff0942(rttMessage);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionRttMessageReceived$34$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4261x49ff0942(String rttMessage) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionRttMessageReceived(rttMessage);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionRttAudioIndicatorChanged(final ImsStreamMediaProfile profile) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda38
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4260x5168d910(profile);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionRttAudioIndicatorChanged$35$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4260x5168d910(ImsStreamMediaProfile profile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionRttAudioIndicatorChanged(profile);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionTransferred() {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4267x477ed2ca();
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionTransferred$36$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4267x477ed2ca() {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionTransferred(ImsCallSession.this);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionTransferFailed(final ImsReasonInfo reasonInfo) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4266x44dcf073(reasonInfo);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionTransferFailed$37$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4266x44dcf073(ImsReasonInfo reasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionTransferFailed(ImsCallSession.this, reasonInfo);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionDtmfReceived(final char dtmf) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4237xb0c5af92(dtmf);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionDtmfReceived$38$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4237xb0c5af92(char dtmf) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionDtmfReceived(dtmf);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callQualityChanged(final CallQuality callQuality) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4232x58033348(callQuality);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callQualityChanged$39$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4232x58033348(CallQuality callQuality) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callQualityChanged(callQuality);
            }
        }

        @Override // android.telephony.ims.aidl.IImsCallSessionListener
        public void callSessionRtpHeaderExtensionsReceived(final List<RtpHeaderExtension> extensions) {
            TelephonyUtils.runWithCleanCallingIdentity(new Runnable() { // from class: android.telephony.ims.ImsCallSession$IImsCallSessionListenerProxy$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    ImsCallSession.IImsCallSessionListenerProxy.this.m4259x48d94599(extensions);
                }
            }, ImsCallSession.this.mListenerExecutor);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$callSessionRtpHeaderExtensionsReceived$40$android-telephony-ims-ImsCallSession$IImsCallSessionListenerProxy  reason: not valid java name */
        public /* synthetic */ void m4259x48d94599(List extensions) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionRtpHeaderExtensionsReceived(new ArraySet(extensions));
            }
        }
    }

    public String toString() {
        return "[ImsCallSession objId:" + System.identityHashCode(this) + " state:" + State.toString(getState()) + " callId:" + getCallId() + NavigationBarInflaterView.SIZE_MOD_END;
    }
}
