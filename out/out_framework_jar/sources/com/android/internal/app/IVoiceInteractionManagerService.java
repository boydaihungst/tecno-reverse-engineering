package com.android.internal.app;

import android.content.ComponentName;
import android.content.Intent;
import android.hardware.soundtrigger.KeyphraseMetadata;
import android.hardware.soundtrigger.SoundTrigger;
import android.media.AudioFormat;
import android.media.permission.Identity;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.SharedMemory;
import android.service.voice.IMicrophoneHotwordDetectionVoiceInteractionCallback;
import android.service.voice.IVoiceInteractionSession;
import com.android.internal.app.IHotwordRecognitionStatusCallback;
import com.android.internal.app.IVoiceActionCheckCallback;
import com.android.internal.app.IVoiceInteractionSessionListener;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractionSoundTriggerSession;
import com.android.internal.app.IVoiceInteractor;
import java.util.List;
/* loaded from: classes4.dex */
public interface IVoiceInteractionManagerService extends IInterface {
    boolean activeServiceSupportsAssist() throws RemoteException;

    boolean activeServiceSupportsLaunchFromKeyguard() throws RemoteException;

    void closeSystemDialogs(IBinder iBinder) throws RemoteException;

    IVoiceInteractionSoundTriggerSession createSoundTriggerSessionAsOriginator(Identity identity, IBinder iBinder) throws RemoteException;

    int deleteKeyphraseSoundModel(int i, String str) throws RemoteException;

    boolean deliverNewSession(IBinder iBinder, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor) throws RemoteException;

    void finish(IBinder iBinder) throws RemoteException;

    ComponentName getActiveServiceComponentName() throws RemoteException;

    void getActiveServiceSupportedActions(List<String> list, IVoiceActionCheckCallback iVoiceActionCheckCallback) throws RemoteException;

    int getDisabledShowContext() throws RemoteException;

    KeyphraseMetadata getEnrolledKeyphraseMetadata(String str, String str2) throws RemoteException;

    SoundTrigger.KeyphraseSoundModel getKeyphraseSoundModel(int i, String str) throws RemoteException;

    int getUserDisabledShowContext() throws RemoteException;

    void hideCurrentSession() throws RemoteException;

    boolean hideSessionFromSession(IBinder iBinder) throws RemoteException;

    boolean isEnrolledForKeyphrase(int i, String str) throws RemoteException;

    boolean isSessionRunning() throws RemoteException;

    void launchVoiceAssistFromKeyguard() throws RemoteException;

    void onLockscreenShown() throws RemoteException;

    void performDirectAction(IBinder iBinder, String str, Bundle bundle, int i, IBinder iBinder2, RemoteCallback remoteCallback, RemoteCallback remoteCallback2) throws RemoteException;

    void registerVoiceInteractionSessionListener(IVoiceInteractionSessionListener iVoiceInteractionSessionListener) throws RemoteException;

    void requestDirectActions(IBinder iBinder, int i, IBinder iBinder2, RemoteCallback remoteCallback, RemoteCallback remoteCallback2) throws RemoteException;

    void setDisabled(boolean z) throws RemoteException;

    void setDisabledShowContext(int i) throws RemoteException;

    void setKeepAwake(IBinder iBinder, boolean z) throws RemoteException;

    void setSessionWindowVisible(IBinder iBinder, boolean z) throws RemoteException;

    void setUiHints(Bundle bundle) throws RemoteException;

    void showSession(Bundle bundle, int i) throws RemoteException;

    boolean showSessionForActiveService(Bundle bundle, int i, IVoiceInteractionSessionShowCallback iVoiceInteractionSessionShowCallback, IBinder iBinder) throws RemoteException;

    boolean showSessionFromSession(IBinder iBinder, Bundle bundle, int i) throws RemoteException;

    void shutdownHotwordDetectionService() throws RemoteException;

    int startAssistantActivity(IBinder iBinder, Intent intent, String str, String str2) throws RemoteException;

    void startListeningFromExternalSource(ParcelFileDescriptor parcelFileDescriptor, AudioFormat audioFormat, PersistableBundle persistableBundle, IMicrophoneHotwordDetectionVoiceInteractionCallback iMicrophoneHotwordDetectionVoiceInteractionCallback) throws RemoteException;

    void startListeningFromMic(AudioFormat audioFormat, IMicrophoneHotwordDetectionVoiceInteractionCallback iMicrophoneHotwordDetectionVoiceInteractionCallback) throws RemoteException;

    void startListeningVisibleActivityChanged(IBinder iBinder) throws RemoteException;

    int startVoiceActivity(IBinder iBinder, Intent intent, String str, String str2) throws RemoteException;

    void stopListeningFromMic() throws RemoteException;

    void stopListeningVisibleActivityChanged(IBinder iBinder) throws RemoteException;

    void triggerHardwareRecognitionEventForTest(SoundTrigger.KeyphraseRecognitionEvent keyphraseRecognitionEvent, IHotwordRecognitionStatusCallback iHotwordRecognitionStatusCallback) throws RemoteException;

    int updateKeyphraseSoundModel(SoundTrigger.KeyphraseSoundModel keyphraseSoundModel) throws RemoteException;

    void updateState(Identity identity, PersistableBundle persistableBundle, SharedMemory sharedMemory, IHotwordRecognitionStatusCallback iHotwordRecognitionStatusCallback, int i) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements IVoiceInteractionManagerService {
        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void showSession(Bundle sessionArgs, int flags) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public boolean deliverNewSession(IBinder token, IVoiceInteractionSession session, IVoiceInteractor interactor) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public boolean showSessionFromSession(IBinder token, Bundle sessionArgs, int flags) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public boolean hideSessionFromSession(IBinder token) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public int startVoiceActivity(IBinder token, Intent intent, String resolvedType, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public int startAssistantActivity(IBinder token, Intent intent, String resolvedType, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void setKeepAwake(IBinder token, boolean keepAwake) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void closeSystemDialogs(IBinder token) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void finish(IBinder token) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void setDisabledShowContext(int flags) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public int getDisabledShowContext() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public int getUserDisabledShowContext() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public SoundTrigger.KeyphraseSoundModel getKeyphraseSoundModel(int keyphraseId, String bcp47Locale) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public int updateKeyphraseSoundModel(SoundTrigger.KeyphraseSoundModel model) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public int deleteKeyphraseSoundModel(int keyphraseId, String bcp47Locale) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public boolean isEnrolledForKeyphrase(int keyphraseId, String bcp47Locale) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public KeyphraseMetadata getEnrolledKeyphraseMetadata(String keyphrase, String bcp47Locale) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public ComponentName getActiveServiceComponentName() throws RemoteException {
            return null;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public boolean showSessionForActiveService(Bundle args, int sourceFlags, IVoiceInteractionSessionShowCallback showCallback, IBinder activityToken) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void hideCurrentSession() throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void launchVoiceAssistFromKeyguard() throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public boolean isSessionRunning() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public boolean activeServiceSupportsAssist() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public boolean activeServiceSupportsLaunchFromKeyguard() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void onLockscreenShown() throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void registerVoiceInteractionSessionListener(IVoiceInteractionSessionListener listener) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void getActiveServiceSupportedActions(List<String> voiceActions, IVoiceActionCheckCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void setUiHints(Bundle hints) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void requestDirectActions(IBinder token, int taskId, IBinder assistToken, RemoteCallback cancellationCallback, RemoteCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void performDirectAction(IBinder token, String actionId, Bundle arguments, int taskId, IBinder assistToken, RemoteCallback cancellationCallback, RemoteCallback resultCallback) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void setDisabled(boolean disabled) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public IVoiceInteractionSoundTriggerSession createSoundTriggerSessionAsOriginator(Identity originatorIdentity, IBinder client) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void updateState(Identity originatorIdentity, PersistableBundle options, SharedMemory sharedMemory, IHotwordRecognitionStatusCallback callback, int detectorType) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void shutdownHotwordDetectionService() throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void startListeningFromMic(AudioFormat audioFormat, IMicrophoneHotwordDetectionVoiceInteractionCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void stopListeningFromMic() throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void startListeningFromExternalSource(ParcelFileDescriptor audioStream, AudioFormat audioFormat, PersistableBundle options, IMicrophoneHotwordDetectionVoiceInteractionCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void triggerHardwareRecognitionEventForTest(SoundTrigger.KeyphraseRecognitionEvent event, IHotwordRecognitionStatusCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void startListeningVisibleActivityChanged(IBinder token) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void stopListeningVisibleActivityChanged(IBinder token) throws RemoteException {
        }

        @Override // com.android.internal.app.IVoiceInteractionManagerService
        public void setSessionWindowVisible(IBinder token, boolean visible) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements IVoiceInteractionManagerService {
        public static final String DESCRIPTOR = "com.android.internal.app.IVoiceInteractionManagerService";
        static final int TRANSACTION_activeServiceSupportsAssist = 23;
        static final int TRANSACTION_activeServiceSupportsLaunchFromKeyguard = 24;
        static final int TRANSACTION_closeSystemDialogs = 8;
        static final int TRANSACTION_createSoundTriggerSessionAsOriginator = 32;
        static final int TRANSACTION_deleteKeyphraseSoundModel = 15;
        static final int TRANSACTION_deliverNewSession = 2;
        static final int TRANSACTION_finish = 9;
        static final int TRANSACTION_getActiveServiceComponentName = 18;
        static final int TRANSACTION_getActiveServiceSupportedActions = 27;
        static final int TRANSACTION_getDisabledShowContext = 11;
        static final int TRANSACTION_getEnrolledKeyphraseMetadata = 17;
        static final int TRANSACTION_getKeyphraseSoundModel = 13;
        static final int TRANSACTION_getUserDisabledShowContext = 12;
        static final int TRANSACTION_hideCurrentSession = 20;
        static final int TRANSACTION_hideSessionFromSession = 4;
        static final int TRANSACTION_isEnrolledForKeyphrase = 16;
        static final int TRANSACTION_isSessionRunning = 22;
        static final int TRANSACTION_launchVoiceAssistFromKeyguard = 21;
        static final int TRANSACTION_onLockscreenShown = 25;
        static final int TRANSACTION_performDirectAction = 30;
        static final int TRANSACTION_registerVoiceInteractionSessionListener = 26;
        static final int TRANSACTION_requestDirectActions = 29;
        static final int TRANSACTION_setDisabled = 31;
        static final int TRANSACTION_setDisabledShowContext = 10;
        static final int TRANSACTION_setKeepAwake = 7;
        static final int TRANSACTION_setSessionWindowVisible = 41;
        static final int TRANSACTION_setUiHints = 28;
        static final int TRANSACTION_showSession = 1;
        static final int TRANSACTION_showSessionForActiveService = 19;
        static final int TRANSACTION_showSessionFromSession = 3;
        static final int TRANSACTION_shutdownHotwordDetectionService = 34;
        static final int TRANSACTION_startAssistantActivity = 6;
        static final int TRANSACTION_startListeningFromExternalSource = 37;
        static final int TRANSACTION_startListeningFromMic = 35;
        static final int TRANSACTION_startListeningVisibleActivityChanged = 39;
        static final int TRANSACTION_startVoiceActivity = 5;
        static final int TRANSACTION_stopListeningFromMic = 36;
        static final int TRANSACTION_stopListeningVisibleActivityChanged = 40;
        static final int TRANSACTION_triggerHardwareRecognitionEventForTest = 38;
        static final int TRANSACTION_updateKeyphraseSoundModel = 14;
        static final int TRANSACTION_updateState = 33;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IVoiceInteractionManagerService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IVoiceInteractionManagerService)) {
                return (IVoiceInteractionManagerService) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public static String getDefaultTransactionName(int transactionCode) {
            switch (transactionCode) {
                case 1:
                    return "showSession";
                case 2:
                    return "deliverNewSession";
                case 3:
                    return "showSessionFromSession";
                case 4:
                    return "hideSessionFromSession";
                case 5:
                    return "startVoiceActivity";
                case 6:
                    return "startAssistantActivity";
                case 7:
                    return "setKeepAwake";
                case 8:
                    return "closeSystemDialogs";
                case 9:
                    return "finish";
                case 10:
                    return "setDisabledShowContext";
                case 11:
                    return "getDisabledShowContext";
                case 12:
                    return "getUserDisabledShowContext";
                case 13:
                    return "getKeyphraseSoundModel";
                case 14:
                    return "updateKeyphraseSoundModel";
                case 15:
                    return "deleteKeyphraseSoundModel";
                case 16:
                    return "isEnrolledForKeyphrase";
                case 17:
                    return "getEnrolledKeyphraseMetadata";
                case 18:
                    return "getActiveServiceComponentName";
                case 19:
                    return "showSessionForActiveService";
                case 20:
                    return "hideCurrentSession";
                case 21:
                    return "launchVoiceAssistFromKeyguard";
                case 22:
                    return "isSessionRunning";
                case 23:
                    return "activeServiceSupportsAssist";
                case 24:
                    return "activeServiceSupportsLaunchFromKeyguard";
                case 25:
                    return "onLockscreenShown";
                case 26:
                    return "registerVoiceInteractionSessionListener";
                case 27:
                    return "getActiveServiceSupportedActions";
                case 28:
                    return "setUiHints";
                case 29:
                    return "requestDirectActions";
                case 30:
                    return "performDirectAction";
                case 31:
                    return "setDisabled";
                case 32:
                    return "createSoundTriggerSessionAsOriginator";
                case 33:
                    return "updateState";
                case 34:
                    return "shutdownHotwordDetectionService";
                case 35:
                    return "startListeningFromMic";
                case 36:
                    return "stopListeningFromMic";
                case 37:
                    return "startListeningFromExternalSource";
                case 38:
                    return "triggerHardwareRecognitionEventForTest";
                case 39:
                    return "startListeningVisibleActivityChanged";
                case 40:
                    return "stopListeningVisibleActivityChanged";
                case 41:
                    return "setSessionWindowVisible";
                default:
                    return null;
            }
        }

        @Override // android.os.Binder
        public String getTransactionName(int transactionCode) {
            return getDefaultTransactionName(transactionCode);
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            Bundle _arg0 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            showSession(_arg0, _arg1);
                            reply.writeNoException();
                            break;
                        case 2:
                            IBinder _arg02 = data.readStrongBinder();
                            IVoiceInteractionSession _arg12 = IVoiceInteractionSession.Stub.asInterface(data.readStrongBinder());
                            IVoiceInteractor _arg2 = IVoiceInteractor.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result = deliverNewSession(_arg02, _arg12, _arg2);
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            break;
                        case 3:
                            IBinder _arg03 = data.readStrongBinder();
                            Bundle _arg13 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg22 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result2 = showSessionFromSession(_arg03, _arg13, _arg22);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 4:
                            IBinder _arg04 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            boolean _result3 = hideSessionFromSession(_arg04);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 5:
                            IBinder _arg05 = data.readStrongBinder();
                            Intent _arg14 = (Intent) data.readTypedObject(Intent.CREATOR);
                            String _arg23 = data.readString();
                            String _arg3 = data.readString();
                            data.enforceNoDataAvail();
                            int _result4 = startVoiceActivity(_arg05, _arg14, _arg23, _arg3);
                            reply.writeNoException();
                            reply.writeInt(_result4);
                            break;
                        case 6:
                            IBinder _arg06 = data.readStrongBinder();
                            Intent _arg15 = (Intent) data.readTypedObject(Intent.CREATOR);
                            String _arg24 = data.readString();
                            String _arg32 = data.readString();
                            data.enforceNoDataAvail();
                            int _result5 = startAssistantActivity(_arg06, _arg15, _arg24, _arg32);
                            reply.writeNoException();
                            reply.writeInt(_result5);
                            break;
                        case 7:
                            IBinder _arg07 = data.readStrongBinder();
                            boolean _arg16 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setKeepAwake(_arg07, _arg16);
                            reply.writeNoException();
                            break;
                        case 8:
                            IBinder _arg08 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            closeSystemDialogs(_arg08);
                            reply.writeNoException();
                            break;
                        case 9:
                            IBinder _arg09 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            finish(_arg09);
                            reply.writeNoException();
                            break;
                        case 10:
                            int _arg010 = data.readInt();
                            data.enforceNoDataAvail();
                            setDisabledShowContext(_arg010);
                            reply.writeNoException();
                            break;
                        case 11:
                            int _result6 = getDisabledShowContext();
                            reply.writeNoException();
                            reply.writeInt(_result6);
                            break;
                        case 12:
                            int _result7 = getUserDisabledShowContext();
                            reply.writeNoException();
                            reply.writeInt(_result7);
                            break;
                        case 13:
                            int _arg011 = data.readInt();
                            String _arg17 = data.readString();
                            data.enforceNoDataAvail();
                            SoundTrigger.KeyphraseSoundModel _result8 = getKeyphraseSoundModel(_arg011, _arg17);
                            reply.writeNoException();
                            reply.writeTypedObject(_result8, 1);
                            break;
                        case 14:
                            SoundTrigger.KeyphraseSoundModel _arg012 = (SoundTrigger.KeyphraseSoundModel) data.readTypedObject(SoundTrigger.KeyphraseSoundModel.CREATOR);
                            data.enforceNoDataAvail();
                            int _result9 = updateKeyphraseSoundModel(_arg012);
                            reply.writeNoException();
                            reply.writeInt(_result9);
                            break;
                        case 15:
                            int _arg013 = data.readInt();
                            String _arg18 = data.readString();
                            data.enforceNoDataAvail();
                            int _result10 = deleteKeyphraseSoundModel(_arg013, _arg18);
                            reply.writeNoException();
                            reply.writeInt(_result10);
                            break;
                        case 16:
                            int _arg014 = data.readInt();
                            String _arg19 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result11 = isEnrolledForKeyphrase(_arg014, _arg19);
                            reply.writeNoException();
                            reply.writeBoolean(_result11);
                            break;
                        case 17:
                            String _arg015 = data.readString();
                            String _arg110 = data.readString();
                            data.enforceNoDataAvail();
                            KeyphraseMetadata _result12 = getEnrolledKeyphraseMetadata(_arg015, _arg110);
                            reply.writeNoException();
                            reply.writeTypedObject(_result12, 1);
                            break;
                        case 18:
                            ComponentName _result13 = getActiveServiceComponentName();
                            reply.writeNoException();
                            reply.writeTypedObject(_result13, 1);
                            break;
                        case 19:
                            Bundle _arg016 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg111 = data.readInt();
                            IVoiceInteractionSessionShowCallback _arg25 = IVoiceInteractionSessionShowCallback.Stub.asInterface(data.readStrongBinder());
                            IBinder _arg33 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            boolean _result14 = showSessionForActiveService(_arg016, _arg111, _arg25, _arg33);
                            reply.writeNoException();
                            reply.writeBoolean(_result14);
                            break;
                        case 20:
                            hideCurrentSession();
                            reply.writeNoException();
                            break;
                        case 21:
                            launchVoiceAssistFromKeyguard();
                            reply.writeNoException();
                            break;
                        case 22:
                            boolean _result15 = isSessionRunning();
                            reply.writeNoException();
                            reply.writeBoolean(_result15);
                            break;
                        case 23:
                            boolean _result16 = activeServiceSupportsAssist();
                            reply.writeNoException();
                            reply.writeBoolean(_result16);
                            break;
                        case 24:
                            boolean _result17 = activeServiceSupportsLaunchFromKeyguard();
                            reply.writeNoException();
                            reply.writeBoolean(_result17);
                            break;
                        case 25:
                            onLockscreenShown();
                            reply.writeNoException();
                            break;
                        case 26:
                            IVoiceInteractionSessionListener _arg017 = IVoiceInteractionSessionListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerVoiceInteractionSessionListener(_arg017);
                            reply.writeNoException();
                            break;
                        case 27:
                            List<String> _arg018 = data.createStringArrayList();
                            IVoiceActionCheckCallback _arg112 = IVoiceActionCheckCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            getActiveServiceSupportedActions(_arg018, _arg112);
                            reply.writeNoException();
                            break;
                        case 28:
                            Bundle _arg019 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            setUiHints(_arg019);
                            reply.writeNoException();
                            break;
                        case 29:
                            IBinder _arg020 = data.readStrongBinder();
                            int _arg113 = data.readInt();
                            IBinder _arg26 = data.readStrongBinder();
                            RemoteCallback _arg34 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
                            RemoteCallback _arg4 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
                            data.enforceNoDataAvail();
                            requestDirectActions(_arg020, _arg113, _arg26, _arg34, _arg4);
                            reply.writeNoException();
                            break;
                        case 30:
                            IBinder _arg021 = data.readStrongBinder();
                            String _arg114 = data.readString();
                            Bundle _arg27 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg35 = data.readInt();
                            IBinder _arg42 = data.readStrongBinder();
                            RemoteCallback _arg5 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
                            RemoteCallback _arg6 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
                            data.enforceNoDataAvail();
                            performDirectAction(_arg021, _arg114, _arg27, _arg35, _arg42, _arg5, _arg6);
                            reply.writeNoException();
                            break;
                        case 31:
                            boolean _arg022 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setDisabled(_arg022);
                            reply.writeNoException();
                            break;
                        case 32:
                            Identity _arg023 = (Identity) data.readTypedObject(Identity.CREATOR);
                            IBinder _arg115 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            IVoiceInteractionSoundTriggerSession _result18 = createSoundTriggerSessionAsOriginator(_arg023, _arg115);
                            reply.writeNoException();
                            reply.writeStrongInterface(_result18);
                            break;
                        case 33:
                            Identity _arg024 = (Identity) data.readTypedObject(Identity.CREATOR);
                            PersistableBundle _arg116 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
                            SharedMemory _arg28 = (SharedMemory) data.readTypedObject(SharedMemory.CREATOR);
                            IHotwordRecognitionStatusCallback _arg36 = IHotwordRecognitionStatusCallback.Stub.asInterface(data.readStrongBinder());
                            int _arg43 = data.readInt();
                            data.enforceNoDataAvail();
                            updateState(_arg024, _arg116, _arg28, _arg36, _arg43);
                            reply.writeNoException();
                            break;
                        case 34:
                            shutdownHotwordDetectionService();
                            reply.writeNoException();
                            break;
                        case 35:
                            AudioFormat _arg025 = (AudioFormat) data.readTypedObject(AudioFormat.CREATOR);
                            IMicrophoneHotwordDetectionVoiceInteractionCallback _arg117 = IMicrophoneHotwordDetectionVoiceInteractionCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            startListeningFromMic(_arg025, _arg117);
                            reply.writeNoException();
                            break;
                        case 36:
                            stopListeningFromMic();
                            reply.writeNoException();
                            break;
                        case 37:
                            ParcelFileDescriptor _arg026 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            AudioFormat _arg118 = (AudioFormat) data.readTypedObject(AudioFormat.CREATOR);
                            PersistableBundle _arg29 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
                            IMicrophoneHotwordDetectionVoiceInteractionCallback _arg37 = IMicrophoneHotwordDetectionVoiceInteractionCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            startListeningFromExternalSource(_arg026, _arg118, _arg29, _arg37);
                            reply.writeNoException();
                            break;
                        case 38:
                            SoundTrigger.KeyphraseRecognitionEvent _arg027 = (SoundTrigger.KeyphraseRecognitionEvent) data.readTypedObject(SoundTrigger.KeyphraseRecognitionEvent.CREATOR);
                            IHotwordRecognitionStatusCallback _arg119 = IHotwordRecognitionStatusCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            triggerHardwareRecognitionEventForTest(_arg027, _arg119);
                            reply.writeNoException();
                            break;
                        case 39:
                            IBinder _arg028 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            startListeningVisibleActivityChanged(_arg028);
                            reply.writeNoException();
                            break;
                        case 40:
                            IBinder _arg029 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            stopListeningVisibleActivityChanged(_arg029);
                            reply.writeNoException();
                            break;
                        case 41:
                            IBinder _arg030 = data.readStrongBinder();
                            boolean _arg120 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setSessionWindowVisible(_arg030, _arg120);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes4.dex */
        private static class Proxy implements IVoiceInteractionManagerService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void showSession(Bundle sessionArgs, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(sessionArgs, 0);
                    _data.writeInt(flags);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public boolean deliverNewSession(IBinder token, IVoiceInteractionSession session, IVoiceInteractor interactor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeStrongInterface(session);
                    _data.writeStrongInterface(interactor);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public boolean showSessionFromSession(IBinder token, Bundle sessionArgs, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(sessionArgs, 0);
                    _data.writeInt(flags);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public boolean hideSessionFromSession(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public int startVoiceActivity(IBinder token, Intent intent, String resolvedType, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(intent, 0);
                    _data.writeString(resolvedType);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public int startAssistantActivity(IBinder token, Intent intent, String resolvedType, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(intent, 0);
                    _data.writeString(resolvedType);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void setKeepAwake(IBinder token, boolean keepAwake) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(keepAwake);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void closeSystemDialogs(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void finish(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void setDisabledShowContext(int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public int getDisabledShowContext() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public int getUserDisabledShowContext() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public SoundTrigger.KeyphraseSoundModel getKeyphraseSoundModel(int keyphraseId, String bcp47Locale) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(keyphraseId);
                    _data.writeString(bcp47Locale);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    SoundTrigger.KeyphraseSoundModel _result = (SoundTrigger.KeyphraseSoundModel) _reply.readTypedObject(SoundTrigger.KeyphraseSoundModel.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public int updateKeyphraseSoundModel(SoundTrigger.KeyphraseSoundModel model) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(model, 0);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public int deleteKeyphraseSoundModel(int keyphraseId, String bcp47Locale) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(keyphraseId);
                    _data.writeString(bcp47Locale);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public boolean isEnrolledForKeyphrase(int keyphraseId, String bcp47Locale) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(keyphraseId);
                    _data.writeString(bcp47Locale);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public KeyphraseMetadata getEnrolledKeyphraseMetadata(String keyphrase, String bcp47Locale) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(keyphrase);
                    _data.writeString(bcp47Locale);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    KeyphraseMetadata _result = (KeyphraseMetadata) _reply.readTypedObject(KeyphraseMetadata.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public ComponentName getActiveServiceComponentName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    ComponentName _result = (ComponentName) _reply.readTypedObject(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public boolean showSessionForActiveService(Bundle args, int sourceFlags, IVoiceInteractionSessionShowCallback showCallback, IBinder activityToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(args, 0);
                    _data.writeInt(sourceFlags);
                    _data.writeStrongInterface(showCallback);
                    _data.writeStrongBinder(activityToken);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void hideCurrentSession() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void launchVoiceAssistFromKeyguard() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public boolean isSessionRunning() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public boolean activeServiceSupportsAssist() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public boolean activeServiceSupportsLaunchFromKeyguard() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void onLockscreenShown() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void registerVoiceInteractionSessionListener(IVoiceInteractionSessionListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void getActiveServiceSupportedActions(List<String> voiceActions, IVoiceActionCheckCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(voiceActions);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void setUiHints(Bundle hints) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(hints, 0);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void requestDirectActions(IBinder token, int taskId, IBinder assistToken, RemoteCallback cancellationCallback, RemoteCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(taskId);
                    _data.writeStrongBinder(assistToken);
                    _data.writeTypedObject(cancellationCallback, 0);
                    _data.writeTypedObject(callback, 0);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void performDirectAction(IBinder token, String actionId, Bundle arguments, int taskId, IBinder assistToken, RemoteCallback cancellationCallback, RemoteCallback resultCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(actionId);
                    _data.writeTypedObject(arguments, 0);
                    _data.writeInt(taskId);
                    _data.writeStrongBinder(assistToken);
                    _data.writeTypedObject(cancellationCallback, 0);
                    _data.writeTypedObject(resultCallback, 0);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void setDisabled(boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(disabled);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public IVoiceInteractionSoundTriggerSession createSoundTriggerSessionAsOriginator(Identity originatorIdentity, IBinder client) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(originatorIdentity, 0);
                    _data.writeStrongBinder(client);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    IVoiceInteractionSoundTriggerSession _result = IVoiceInteractionSoundTriggerSession.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void updateState(Identity originatorIdentity, PersistableBundle options, SharedMemory sharedMemory, IHotwordRecognitionStatusCallback callback, int detectorType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(originatorIdentity, 0);
                    _data.writeTypedObject(options, 0);
                    _data.writeTypedObject(sharedMemory, 0);
                    _data.writeStrongInterface(callback);
                    _data.writeInt(detectorType);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void shutdownHotwordDetectionService() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void startListeningFromMic(AudioFormat audioFormat, IMicrophoneHotwordDetectionVoiceInteractionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(audioFormat, 0);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void stopListeningFromMic() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void startListeningFromExternalSource(ParcelFileDescriptor audioStream, AudioFormat audioFormat, PersistableBundle options, IMicrophoneHotwordDetectionVoiceInteractionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(audioStream, 0);
                    _data.writeTypedObject(audioFormat, 0);
                    _data.writeTypedObject(options, 0);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void triggerHardwareRecognitionEventForTest(SoundTrigger.KeyphraseRecognitionEvent event, IHotwordRecognitionStatusCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(event, 0);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void startListeningVisibleActivityChanged(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void stopListeningVisibleActivityChanged(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.app.IVoiceInteractionManagerService
            public void setSessionWindowVisible(IBinder token, boolean visible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(visible);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 40;
        }
    }
}
