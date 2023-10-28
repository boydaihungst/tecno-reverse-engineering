package com.android.server.texttospeech;

import android.content.Context;
import android.os.UserHandle;
import android.speech.tts.ITextToSpeechManager;
import android.speech.tts.ITextToSpeechSessionCallback;
import com.android.server.infra.AbstractMasterSystemService;
import com.android.server.texttospeech.TextToSpeechManagerPerUserService;
/* loaded from: classes2.dex */
public final class TextToSpeechManagerService extends AbstractMasterSystemService<TextToSpeechManagerService, TextToSpeechManagerPerUserService> {
    private static final String TAG = TextToSpeechManagerService.class.getSimpleName();

    public TextToSpeechManagerService(Context context) {
        super(context, null, null);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("texttospeech", new TextToSpeechManagerServiceStub());
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public TextToSpeechManagerPerUserService newServiceLocked(int resolvedUserId, boolean disabled) {
        return new TextToSpeechManagerPerUserService(this, this.mLock, resolvedUserId);
    }

    /* loaded from: classes2.dex */
    private final class TextToSpeechManagerServiceStub extends ITextToSpeechManager.Stub {
        private TextToSpeechManagerServiceStub() {
        }

        public void createSession(String engine, final ITextToSpeechSessionCallback sessionCallback) {
            synchronized (TextToSpeechManagerService.this.mLock) {
                TextToSpeechManagerPerUserService perUserService = (TextToSpeechManagerPerUserService) TextToSpeechManagerService.this.getServiceForUserLocked(UserHandle.getCallingUserId());
                if (perUserService != null) {
                    perUserService.createSessionLocked(engine, sessionCallback);
                } else {
                    TextToSpeechManagerPerUserService.runSessionCallbackMethod(new TextToSpeechManagerPerUserService.ThrowingRunnable() { // from class: com.android.server.texttospeech.TextToSpeechManagerService$TextToSpeechManagerServiceStub$$ExternalSyntheticLambda0
                        @Override // com.android.server.texttospeech.TextToSpeechManagerPerUserService.ThrowingRunnable
                        public final void runOrThrow() {
                            sessionCallback.onError("Service is not available for user");
                        }
                    });
                }
            }
        }
    }
}
