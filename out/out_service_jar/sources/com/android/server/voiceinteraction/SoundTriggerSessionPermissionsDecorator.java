package com.android.server.voiceinteraction;

import android.content.Context;
import android.hardware.soundtrigger.SoundTrigger;
import android.media.permission.Identity;
import android.media.permission.PermissionUtil;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.app.IHotwordRecognitionStatusCallback;
import com.android.internal.app.IVoiceInteractionSoundTriggerSession;
/* loaded from: classes2.dex */
final class SoundTriggerSessionPermissionsDecorator implements IVoiceInteractionSoundTriggerSession {
    static final String TAG = "SoundTriggerSessionPermissionsDecorator";
    private static final int TEMPORARY_PERMISSION_DENIED = 3;
    private final Context mContext;
    private final IVoiceInteractionSoundTriggerSession mDelegate;
    private final Identity mOriginatorIdentity;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SoundTriggerSessionPermissionsDecorator(IVoiceInteractionSoundTriggerSession delegate, Context context, Identity originatorIdentity) {
        this.mDelegate = delegate;
        this.mContext = context;
        this.mOriginatorIdentity = originatorIdentity;
    }

    public SoundTrigger.ModuleProperties getDspModuleProperties() throws RemoteException {
        return this.mDelegate.getDspModuleProperties();
    }

    public int startRecognition(int i, String s, IHotwordRecognitionStatusCallback iHotwordRecognitionStatusCallback, SoundTrigger.RecognitionConfig recognitionConfig, boolean b) throws RemoteException {
        if (!isHoldingPermissions()) {
            return SoundTrigger.STATUS_PERMISSION_DENIED;
        }
        return this.mDelegate.startRecognition(i, s, iHotwordRecognitionStatusCallback, recognitionConfig, b);
    }

    public int stopRecognition(int i, IHotwordRecognitionStatusCallback iHotwordRecognitionStatusCallback) throws RemoteException {
        return this.mDelegate.stopRecognition(i, iHotwordRecognitionStatusCallback);
    }

    public int setParameter(int i, int i1, int i2) throws RemoteException {
        if (!isHoldingPermissions()) {
            return SoundTrigger.STATUS_PERMISSION_DENIED;
        }
        return this.mDelegate.setParameter(i, i1, i2);
    }

    public int getParameter(int i, int i1) throws RemoteException {
        return this.mDelegate.getParameter(i, i1);
    }

    public SoundTrigger.ModelParamRange queryParameter(int i, int i1) throws RemoteException {
        return this.mDelegate.queryParameter(i, i1);
    }

    public IBinder asBinder() {
        throw new UnsupportedOperationException("This object isn't intended to be used as a Binder.");
    }

    private boolean isHoldingPermissions() {
        try {
            enforcePermissionForPreflight(this.mContext, this.mOriginatorIdentity, "android.permission.RECORD_AUDIO");
            enforcePermissionForPreflight(this.mContext, this.mOriginatorIdentity, "android.permission.CAPTURE_AUDIO_HOTWORD");
            return true;
        } catch (SecurityException e) {
            Slog.e(TAG, e.toString());
            return false;
        }
    }

    static void enforcePermissionForPreflight(Context context, Identity identity, String permission) {
        int status = PermissionUtil.checkPermissionForPreflight(context, identity, permission);
        switch (status) {
            case 0:
            case 1:
                return;
            case 2:
                throw new SecurityException(TextUtils.formatSimple("Failed to obtain permission %s for identity %s", new Object[]{permission, toString(identity)}));
            default:
                throw new RuntimeException("Unexpected permission check result.");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String toString(Identity identity) {
        return "{uid=" + identity.uid + " pid=" + identity.pid + " packageName=" + identity.packageName + " attributionTag=" + identity.attributionTag + "}";
    }
}
