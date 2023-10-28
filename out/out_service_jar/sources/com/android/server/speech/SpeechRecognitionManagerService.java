package com.android.server.speech;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.UserHandle;
import android.speech.IRecognitionServiceManager;
import android.speech.IRecognitionServiceManagerCallback;
import android.util.Slog;
import com.android.server.infra.AbstractMasterSystemService;
import com.android.server.infra.FrameworkResourcesServiceNameResolver;
/* loaded from: classes2.dex */
public final class SpeechRecognitionManagerService extends AbstractMasterSystemService<SpeechRecognitionManagerService, SpeechRecognitionManagerServiceImpl> {
    private static final int MAX_TEMP_SERVICE_SUBSTITUTION_DURATION_MS = 60000;
    private static final String TAG = SpeechRecognitionManagerService.class.getSimpleName();

    public SpeechRecognitionManagerService(Context context) {
        super(context, new FrameworkResourcesServiceNameResolver(context, 17039936), null);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("speech_recognition", new SpeechRecognitionManagerServiceStub());
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void enforceCallingPermissionForManagement() {
        getContext().enforceCallingPermission("android.permission.MANAGE_SPEECH_RECOGNITION", TAG);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected int getMaximumTemporaryServiceDurationMs() {
        return 60000;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public SpeechRecognitionManagerServiceImpl newServiceLocked(int resolvedUserId, boolean disabled) {
        return new SpeechRecognitionManagerServiceImpl(this, this.mLock, resolvedUserId);
    }

    /* loaded from: classes2.dex */
    final class SpeechRecognitionManagerServiceStub extends IRecognitionServiceManager.Stub {
        SpeechRecognitionManagerServiceStub() {
        }

        public void createSession(ComponentName componentName, IBinder clientToken, boolean onDevice, IRecognitionServiceManagerCallback callback) {
            int userId = UserHandle.getCallingUserId();
            synchronized (SpeechRecognitionManagerService.this.mLock) {
                SpeechRecognitionManagerServiceImpl service = (SpeechRecognitionManagerServiceImpl) SpeechRecognitionManagerService.this.getServiceForUserLocked(userId);
                service.createSessionLocked(componentName, clientToken, onDevice, callback);
            }
        }

        public void setTemporaryComponent(ComponentName componentName) {
            int userId = UserHandle.getCallingUserId();
            if (componentName == null) {
                SpeechRecognitionManagerService.this.resetTemporaryService(userId);
                Slog.i(SpeechRecognitionManagerService.TAG, "Reset temporary service for user " + userId);
                return;
            }
            SpeechRecognitionManagerService.this.setTemporaryService(userId, componentName.flattenToString(), 60000);
            Slog.i(SpeechRecognitionManagerService.TAG, "SpeechRecognition temporarily set to " + componentName + " for 60000ms");
        }
    }
}
