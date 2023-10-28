package com.android.server.texttospeech;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.speech.tts.ITextToSpeechService;
import android.speech.tts.ITextToSpeechSession;
import android.speech.tts.ITextToSpeechSessionCallback;
import android.util.Slog;
import com.android.internal.infra.ServiceConnector;
import com.android.server.infra.AbstractPerUserSystemService;
import com.android.server.texttospeech.TextToSpeechManagerPerUserService;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class TextToSpeechManagerPerUserService extends AbstractPerUserSystemService<TextToSpeechManagerPerUserService, TextToSpeechManagerService> {
    private static final String TAG = TextToSpeechManagerPerUserService.class.getSimpleName();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface ThrowingRunnable {
        void runOrThrow() throws RemoteException;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TextToSpeechManagerPerUserService(TextToSpeechManagerService master, Object lock, int userId) {
        super(master, lock, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void createSessionLocked(String engine, ITextToSpeechSessionCallback sessionCallback) {
        TextToSpeechSessionConnection.start(getContext(), this.mUserId, engine, sessionCallback);
    }

    @Override // com.android.server.infra.AbstractPerUserSystemService
    protected ServiceInfo newServiceInfoLocked(ComponentName serviceComponent) throws PackageManager.NameNotFoundException {
        try {
            return AppGlobals.getPackageManager().getServiceInfo(serviceComponent, 128L, this.mUserId);
        } catch (RemoteException e) {
            throw new PackageManager.NameNotFoundException("Could not get service for " + serviceComponent);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class TextToSpeechSessionConnection extends ServiceConnector.Impl<ITextToSpeechService> {
        private final ITextToSpeechSessionCallback mCallback;
        private final String mEngine;
        private final IBinder.DeathRecipient mUnbindOnDeathHandler;

        static void start(Context context, int userId, String engine, ITextToSpeechSessionCallback callback) {
            new TextToSpeechSessionConnection(context, userId, engine, callback).start();
        }

        private TextToSpeechSessionConnection(Context context, int userId, String engine, ITextToSpeechSessionCallback callback) {
            super(context, new Intent("android.intent.action.TTS_SERVICE").setPackage(engine), 1, userId, new Function() { // from class: com.android.server.texttospeech.TextToSpeechManagerPerUserService$TextToSpeechSessionConnection$$ExternalSyntheticLambda1
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return ITextToSpeechService.Stub.asInterface((IBinder) obj);
                }
            });
            this.mEngine = engine;
            this.mCallback = callback;
            this.mUnbindOnDeathHandler = new IBinder.DeathRecipient() { // from class: com.android.server.texttospeech.TextToSpeechManagerPerUserService$TextToSpeechSessionConnection$$ExternalSyntheticLambda2
                @Override // android.os.IBinder.DeathRecipient
                public final void binderDied() {
                    TextToSpeechManagerPerUserService.TextToSpeechSessionConnection.this.m6847xd53b13fe();
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$0$com-android-server-texttospeech-TextToSpeechManagerPerUserService$TextToSpeechSessionConnection  reason: not valid java name */
        public /* synthetic */ void m6847xd53b13fe() {
            unbindEngine("client process death is reported");
        }

        private void start() {
            Slog.d(TextToSpeechManagerPerUserService.TAG, "Trying to start connection to TTS engine: " + this.mEngine);
            connect().thenAccept(new Consumer() { // from class: com.android.server.texttospeech.TextToSpeechManagerPerUserService$TextToSpeechSessionConnection$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    TextToSpeechManagerPerUserService.TextToSpeechSessionConnection.this.m6849x5560a982((ITextToSpeechService) obj);
                }
            }).exceptionally(new Function() { // from class: com.android.server.texttospeech.TextToSpeechManagerPerUserService$TextToSpeechSessionConnection$$ExternalSyntheticLambda5
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return TextToSpeechManagerPerUserService.TextToSpeechSessionConnection.this.m6851xb4cf1184((Throwable) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$start$2$com-android-server-texttospeech-TextToSpeechManagerPerUserService$TextToSpeechSessionConnection  reason: not valid java name */
        public /* synthetic */ void m6849x5560a982(ITextToSpeechService serviceBinder) {
            if (serviceBinder != null) {
                Slog.d(TextToSpeechManagerPerUserService.TAG, "Connected successfully to TTS engine: " + this.mEngine);
                try {
                    this.mCallback.onConnected(new ITextToSpeechSession.Stub() { // from class: com.android.server.texttospeech.TextToSpeechManagerPerUserService.TextToSpeechSessionConnection.1
                        public void disconnect() {
                            TextToSpeechSessionConnection.this.unbindEngine("client disconnection request");
                        }
                    }, serviceBinder.asBinder());
                    this.mCallback.asBinder().linkToDeath(this.mUnbindOnDeathHandler, 0);
                    return;
                } catch (RemoteException ex) {
                    Slog.w(TextToSpeechManagerPerUserService.TAG, "Error notifying the client on connection", ex);
                    unbindEngine("failed communicating with the client - process is dead");
                    return;
                }
            }
            Slog.w(TextToSpeechManagerPerUserService.TAG, "Failed to obtain TTS engine binder");
            TextToSpeechManagerPerUserService.runSessionCallbackMethod(new ThrowingRunnable() { // from class: com.android.server.texttospeech.TextToSpeechManagerPerUserService$TextToSpeechSessionConnection$$ExternalSyntheticLambda0
                @Override // com.android.server.texttospeech.TextToSpeechManagerPerUserService.ThrowingRunnable
                public final void runOrThrow() {
                    TextToSpeechManagerPerUserService.TextToSpeechSessionConnection.this.m6848x25a97581();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$start$1$com-android-server-texttospeech-TextToSpeechManagerPerUserService$TextToSpeechSessionConnection  reason: not valid java name */
        public /* synthetic */ void m6848x25a97581() throws RemoteException {
            this.mCallback.onError("Failed creating TTS session");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$start$4$com-android-server-texttospeech-TextToSpeechManagerPerUserService$TextToSpeechSessionConnection  reason: not valid java name */
        public /* synthetic */ Void m6851xb4cf1184(final Throwable ex) {
            Slog.w(TextToSpeechManagerPerUserService.TAG, "TTS engine binding error", ex);
            TextToSpeechManagerPerUserService.runSessionCallbackMethod(new ThrowingRunnable() { // from class: com.android.server.texttospeech.TextToSpeechManagerPerUserService$TextToSpeechSessionConnection$$ExternalSyntheticLambda6
                @Override // com.android.server.texttospeech.TextToSpeechManagerPerUserService.ThrowingRunnable
                public final void runOrThrow() {
                    TextToSpeechManagerPerUserService.TextToSpeechSessionConnection.this.m6850x8517dd83(ex);
                }
            });
            return null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$start$3$com-android-server-texttospeech-TextToSpeechManagerPerUserService$TextToSpeechSessionConnection  reason: not valid java name */
        public /* synthetic */ void m6850x8517dd83(Throwable ex) throws RemoteException {
            this.mCallback.onError("Failed creating TTS session: " + ex.getCause());
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        public void onServiceConnectionStatusChanged(ITextToSpeechService service, boolean connected) {
            if (!connected) {
                Slog.w(TextToSpeechManagerPerUserService.TAG, "Disconnected from TTS engine");
                final ITextToSpeechSessionCallback iTextToSpeechSessionCallback = this.mCallback;
                Objects.requireNonNull(iTextToSpeechSessionCallback);
                TextToSpeechManagerPerUserService.runSessionCallbackMethod(new ThrowingRunnable() { // from class: com.android.server.texttospeech.TextToSpeechManagerPerUserService$TextToSpeechSessionConnection$$ExternalSyntheticLambda3
                    @Override // com.android.server.texttospeech.TextToSpeechManagerPerUserService.ThrowingRunnable
                    public final void runOrThrow() {
                        iTextToSpeechSessionCallback.onDisconnected();
                    }
                });
                try {
                    this.mCallback.asBinder().unlinkToDeath(this.mUnbindOnDeathHandler, 0);
                } catch (NoSuchElementException e) {
                    Slog.d(TextToSpeechManagerPerUserService.TAG, "The death recipient was not linked.");
                }
            }
        }

        protected long getAutoDisconnectTimeoutMs() {
            return 0L;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void unbindEngine(String reason) {
            Slog.d(TextToSpeechManagerPerUserService.TAG, "Unbinding TTS engine: " + this.mEngine + ". Reason: " + reason);
            unbind();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void runSessionCallbackMethod(ThrowingRunnable callbackRunnable) {
        try {
            callbackRunnable.runOrThrow();
        } catch (RemoteException ex) {
            Slog.i(TAG, "Failed running callback method: " + ex);
        }
    }
}
