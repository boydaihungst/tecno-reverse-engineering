package com.android.server.soundtrigger_middleware;

import android.content.Context;
import android.media.permission.Identity;
import android.media.permission.IdentityContext;
import android.media.permission.PermissionUtil;
import android.media.soundtrigger.ModelParameterRange;
import android.media.soundtrigger.PhraseRecognitionEvent;
import android.media.soundtrigger.PhraseSoundModel;
import android.media.soundtrigger.RecognitionConfig;
import android.media.soundtrigger.RecognitionEvent;
import android.media.soundtrigger.SoundModel;
import android.media.soundtrigger_middleware.ISoundTriggerCallback;
import android.media.soundtrigger_middleware.ISoundTriggerModule;
import android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.LocalServices;
import com.android.server.pm.permission.LegacyPermissionManagerInternal;
import java.io.PrintWriter;
import java.util.Objects;
/* loaded from: classes2.dex */
public class SoundTriggerMiddlewarePermission implements ISoundTriggerMiddlewareInternal, Dumpable {
    private static final String TAG = "SoundTriggerMiddlewarePermission";
    private final Context mContext;
    private final ISoundTriggerMiddlewareInternal mDelegate;

    public SoundTriggerMiddlewarePermission(ISoundTriggerMiddlewareInternal delegate, Context context) {
        this.mDelegate = delegate;
        this.mContext = context;
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerMiddlewareInternal
    public SoundTriggerModuleDescriptor[] listModules() {
        Identity identity = getIdentity();
        enforcePermissionsForPreflight(identity);
        return this.mDelegate.listModules();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerMiddlewareInternal
    public ISoundTriggerModule attach(int handle, ISoundTriggerCallback callback) {
        Identity identity = getIdentity();
        enforcePermissionsForPreflight(identity);
        ModuleWrapper wrapper = new ModuleWrapper(identity, callback);
        return wrapper.attach(this.mDelegate.attach(handle, wrapper.getCallbackWrapper()));
    }

    public String toString() {
        return Objects.toString(this.mDelegate);
    }

    private static Identity getIdentity() {
        return IdentityContext.getNonNull();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforcePermissionsForPreflight(Identity identity) {
        enforcePermissionForPreflight(this.mContext, identity, "android.permission.RECORD_AUDIO");
        enforcePermissionForPreflight(this.mContext, identity, "android.permission.CAPTURE_AUDIO_HOTWORD");
    }

    void enforcePermissionsForDataDelivery(Identity identity, String reason) {
        enforceSoundTriggerRecordAudioPermissionForDataDelivery(identity, reason);
        enforcePermissionForDataDelivery(this.mContext, identity, "android.permission.CAPTURE_AUDIO_HOTWORD", reason);
    }

    private static void enforcePermissionForDataDelivery(Context context, Identity identity, String permission, String reason) {
        int status = PermissionUtil.checkPermissionForDataDelivery(context, identity, permission, reason);
        if (status != 0) {
            throw new SecurityException(String.format("Failed to obtain permission %s for identity %s", permission, ObjectPrinter.print(identity, 16)));
        }
    }

    private static void enforceSoundTriggerRecordAudioPermissionForDataDelivery(Identity identity, String reason) {
        LegacyPermissionManagerInternal lpmi = (LegacyPermissionManagerInternal) LocalServices.getService(LegacyPermissionManagerInternal.class);
        int status = lpmi.checkSoundTriggerRecordAudioPermissionForDataDelivery(identity.uid, identity.packageName, identity.attributionTag, reason);
        if (status != 0) {
            throw new SecurityException(String.format("Failed to obtain permission RECORD_AUDIO for identity %s", ObjectPrinter.print(identity, 16)));
        }
    }

    private static void enforcePermissionForPreflight(Context context, Identity identity, String permission) {
        int status = PermissionUtil.checkPermissionForPreflight(context, identity, permission);
        switch (status) {
            case 0:
            case 1:
                return;
            case 2:
                throw new SecurityException(String.format("Failed to obtain permission %s for identity %s", permission, ObjectPrinter.print(identity, 16)));
            default:
                throw new RuntimeException("Unexpected perimission check result.");
        }
    }

    @Override // com.android.server.soundtrigger_middleware.Dumpable
    public void dump(PrintWriter pw) {
        ISoundTriggerMiddlewareInternal iSoundTriggerMiddlewareInternal = this.mDelegate;
        if (iSoundTriggerMiddlewareInternal instanceof Dumpable) {
            ((Dumpable) iSoundTriggerMiddlewareInternal).dump(pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ModuleWrapper extends ISoundTriggerModule.Stub {
        private final CallbackWrapper mCallbackWrapper;
        private ISoundTriggerModule mDelegate;
        private final Identity mOriginatorIdentity;

        ModuleWrapper(Identity originatorIdentity, ISoundTriggerCallback callback) {
            this.mOriginatorIdentity = originatorIdentity;
            this.mCallbackWrapper = new CallbackWrapper(callback);
        }

        ModuleWrapper attach(ISoundTriggerModule delegate) {
            this.mDelegate = delegate;
            return this;
        }

        ISoundTriggerCallback getCallbackWrapper() {
            return this.mCallbackWrapper;
        }

        public int loadModel(SoundModel model) throws RemoteException {
            enforcePermissions();
            return this.mDelegate.loadModel(model);
        }

        public int loadPhraseModel(PhraseSoundModel model) throws RemoteException {
            enforcePermissions();
            return this.mDelegate.loadPhraseModel(model);
        }

        public void unloadModel(int modelHandle) throws RemoteException {
            this.mDelegate.unloadModel(modelHandle);
        }

        public void startRecognition(int modelHandle, RecognitionConfig config) throws RemoteException {
            enforcePermissions();
            this.mDelegate.startRecognition(modelHandle, config);
        }

        public void stopRecognition(int modelHandle) throws RemoteException {
            this.mDelegate.stopRecognition(modelHandle);
        }

        public void forceRecognitionEvent(int modelHandle) throws RemoteException {
            enforcePermissions();
            this.mDelegate.forceRecognitionEvent(modelHandle);
        }

        public void setModelParameter(int modelHandle, int modelParam, int value) throws RemoteException {
            enforcePermissions();
            this.mDelegate.setModelParameter(modelHandle, modelParam, value);
        }

        public int getModelParameter(int modelHandle, int modelParam) throws RemoteException {
            enforcePermissions();
            return this.mDelegate.getModelParameter(modelHandle, modelParam);
        }

        public ModelParameterRange queryModelParameterSupport(int modelHandle, int modelParam) throws RemoteException {
            enforcePermissions();
            return this.mDelegate.queryModelParameterSupport(modelHandle, modelParam);
        }

        public void detach() throws RemoteException {
            this.mDelegate.detach();
        }

        public String toString() {
            return Objects.toString(this.mDelegate);
        }

        private void enforcePermissions() {
            SoundTriggerMiddlewarePermission.this.enforcePermissionsForPreflight(this.mOriginatorIdentity);
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class CallbackWrapper implements ISoundTriggerCallback {
            private final ISoundTriggerCallback mDelegate;

            private CallbackWrapper(ISoundTriggerCallback delegate) {
                this.mDelegate = delegate;
            }

            public void onRecognition(int modelHandle, RecognitionEvent event, int captureSession) throws RemoteException {
                enforcePermissions("Sound trigger recognition.");
                this.mDelegate.onRecognition(modelHandle, event, captureSession);
            }

            public void onPhraseRecognition(int modelHandle, PhraseRecognitionEvent event, int captureSession) throws RemoteException {
                if (event != null && event.common != null && event.common.status != 1) {
                    enforcePermissions("Sound trigger phrase recognition.");
                } else {
                    Log.w(SoundTriggerMiddlewarePermission.TAG, "always notify ABORTED event to update model status");
                }
                this.mDelegate.onPhraseRecognition(modelHandle, event, captureSession);
            }

            public void onResourcesAvailable() throws RemoteException {
                this.mDelegate.onResourcesAvailable();
            }

            public void onModelUnloaded(int modelHandle) throws RemoteException {
                this.mDelegate.onModelUnloaded(modelHandle);
            }

            public void onModuleDied() throws RemoteException {
                this.mDelegate.onModuleDied();
            }

            public IBinder asBinder() {
                return this.mDelegate.asBinder();
            }

            public String toString() {
                return this.mDelegate.toString();
            }

            private void enforcePermissions(String reason) {
                SoundTriggerMiddlewarePermission.this.enforcePermissionsForDataDelivery(ModuleWrapper.this.mOriginatorIdentity, reason);
            }
        }
    }
}
