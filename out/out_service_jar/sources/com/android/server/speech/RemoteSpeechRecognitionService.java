package com.android.server.speech;

import android.content.AttributionSource;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.speech.IRecognitionListener;
import android.speech.IRecognitionService;
import android.speech.IRecognitionSupportCallback;
import android.util.Log;
import android.util.Slog;
import com.android.internal.infra.ServiceConnector;
import com.android.server.speech.RemoteSpeechRecognitionService;
import java.util.function.Function;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class RemoteSpeechRecognitionService extends ServiceConnector.Impl<IRecognitionService> {
    private static final boolean DEBUG = false;
    private static final String TAG = RemoteSpeechRecognitionService.class.getSimpleName();
    private final int mCallingUid;
    private final ComponentName mComponentName;
    private boolean mConnected;
    private DelegatingListener mDelegatingListener;
    private IRecognitionListener mListener;
    private final Object mLock;
    private boolean mRecordingInProgress;
    private boolean mSessionInProgress;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteSpeechRecognitionService(Context context, ComponentName serviceName, int userId, int callingUid) {
        super(context, new Intent("android.speech.RecognitionService").setComponent(serviceName), 68161537, userId, new Function() { // from class: com.android.server.speech.RemoteSpeechRecognitionService$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return IRecognitionService.Stub.asInterface((IBinder) obj);
            }
        });
        this.mLock = new Object();
        this.mConnected = false;
        this.mSessionInProgress = false;
        this.mRecordingInProgress = false;
        this.mCallingUid = callingUid;
        this.mComponentName = serviceName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getServiceComponentName() {
        return this.mComponentName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startListening(final Intent recognizerIntent, IRecognitionListener listener, final AttributionSource attributionSource) {
        if (listener == null) {
            Log.w(TAG, "#startListening called with no preceding #setListening - ignoring");
        } else if (!this.mConnected) {
            tryRespondWithError(listener, 11);
        } else {
            synchronized (this.mLock) {
                if (this.mSessionInProgress) {
                    Slog.i(TAG, "#startListening called while listening is in progress.");
                    tryRespondWithError(listener, 8);
                    return;
                }
                this.mSessionInProgress = true;
                this.mRecordingInProgress = true;
                this.mListener = listener;
                final DelegatingListener listenerToStart = new DelegatingListener(listener, new Runnable() { // from class: com.android.server.speech.RemoteSpeechRecognitionService$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        RemoteSpeechRecognitionService.this.m6577x288aeb13();
                    }
                });
                this.mDelegatingListener = listenerToStart;
                run(new ServiceConnector.VoidJob() { // from class: com.android.server.speech.RemoteSpeechRecognitionService$$ExternalSyntheticLambda7
                    public final void runNoResult(Object obj) {
                        ((IRecognitionService) obj).startListening(recognizerIntent, listenerToStart, attributionSource);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startListening$0$com-android-server-speech-RemoteSpeechRecognitionService  reason: not valid java name */
    public /* synthetic */ void m6577x288aeb13() {
        synchronized (this.mLock) {
            resetStateLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopListening(IRecognitionListener listener) {
        if (!this.mConnected) {
            tryRespondWithError(listener, 11);
            return;
        }
        synchronized (this.mLock) {
            IRecognitionListener iRecognitionListener = this.mListener;
            if (iRecognitionListener == null) {
                Log.w(TAG, "#stopListening called with no preceding #startListening - ignoring");
                tryRespondWithError(listener, 5);
            } else if (iRecognitionListener.asBinder() != listener.asBinder()) {
                Log.w(TAG, "#stopListening called with an unexpected listener");
                tryRespondWithError(listener, 5);
            } else if (!this.mRecordingInProgress) {
                Slog.i(TAG, "#stopListening called while listening isn't in progress, ignoring.");
            } else {
                this.mRecordingInProgress = false;
                final DelegatingListener listenerToStop = this.mDelegatingListener;
                run(new ServiceConnector.VoidJob() { // from class: com.android.server.speech.RemoteSpeechRecognitionService$$ExternalSyntheticLambda0
                    public final void runNoResult(Object obj) {
                        ((IRecognitionService) obj).stopListening(RemoteSpeechRecognitionService.DelegatingListener.this);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancel(IRecognitionListener listener, final boolean isShutdown) {
        if (!this.mConnected) {
            tryRespondWithError(listener, 11);
        }
        synchronized (this.mLock) {
            IRecognitionListener iRecognitionListener = this.mListener;
            if (iRecognitionListener == null) {
                return;
            }
            if (iRecognitionListener.asBinder() != listener.asBinder()) {
                Log.w(TAG, "#cancel called with an unexpected listener");
                tryRespondWithError(listener, 5);
                return;
            }
            final DelegatingListener delegatingListener = this.mDelegatingListener;
            run(new ServiceConnector.VoidJob() { // from class: com.android.server.speech.RemoteSpeechRecognitionService$$ExternalSyntheticLambda2
                public final void runNoResult(Object obj) {
                    ((IRecognitionService) obj).cancel(delegatingListener, isShutdown);
                }
            });
            this.mRecordingInProgress = false;
            this.mSessionInProgress = false;
            this.mDelegatingListener = null;
            this.mListener = null;
            if (isShutdown) {
                run(new ServiceConnector.VoidJob() { // from class: com.android.server.speech.RemoteSpeechRecognitionService$$ExternalSyntheticLambda3
                    public final void runNoResult(Object obj) {
                        RemoteSpeechRecognitionService.this.m6576x5a9281ee((IRecognitionService) obj);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancel$4$com-android-server-speech-RemoteSpeechRecognitionService  reason: not valid java name */
    public /* synthetic */ void m6576x5a9281ee(IRecognitionService service) throws Exception {
        unbind();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkRecognitionSupport(final Intent recognizerIntent, final IRecognitionSupportCallback callback) {
        if (!this.mConnected) {
            try {
                callback.onError(11);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to report the connection broke to the caller.", e);
                e.printStackTrace();
                return;
            }
        }
        run(new ServiceConnector.VoidJob() { // from class: com.android.server.speech.RemoteSpeechRecognitionService$$ExternalSyntheticLambda5
            public final void runNoResult(Object obj) {
                ((IRecognitionService) obj).checkRecognitionSupport(recognizerIntent, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void triggerModelDownload(final Intent recognizerIntent) {
        if (!this.mConnected) {
            Slog.e(TAG, "#downloadModel failed due to connection.");
        } else {
            run(new ServiceConnector.VoidJob() { // from class: com.android.server.speech.RemoteSpeechRecognitionService$$ExternalSyntheticLambda4
                public final void runNoResult(Object obj) {
                    ((IRecognitionService) obj).triggerModelDownload(recognizerIntent);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void shutdown() {
        synchronized (this.mLock) {
            IRecognitionListener iRecognitionListener = this.mListener;
            if (iRecognitionListener == null) {
                return;
            }
            cancel(iRecognitionListener, true);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    public void onServiceConnectionStatusChanged(IRecognitionService service, boolean connected) {
        this.mConnected = connected;
        synchronized (this.mLock) {
            if (!connected) {
                IRecognitionListener iRecognitionListener = this.mListener;
                if (iRecognitionListener == null) {
                    Slog.i(TAG, "Connection to speech recognition service lost, but no #startListening has been invoked yet.");
                } else {
                    tryRespondWithError(iRecognitionListener, 11);
                    resetStateLocked();
                }
            }
        }
    }

    protected long getAutoDisconnectTimeoutMs() {
        return 0L;
    }

    private void resetStateLocked() {
        this.mListener = null;
        this.mDelegatingListener = null;
        this.mSessionInProgress = false;
        this.mRecordingInProgress = false;
    }

    private static void tryRespondWithError(IRecognitionListener listener, int errorCode) {
        if (listener != null) {
            try {
                listener.onError(errorCode);
            } catch (RemoteException e) {
                Slog.w(TAG, String.format("Failed to respond with an error %d to the client", Integer.valueOf(errorCode)), e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class DelegatingListener extends IRecognitionListener.Stub {
        private final Runnable mOnSessionComplete;
        private final IRecognitionListener mRemoteListener;

        DelegatingListener(IRecognitionListener listener, Runnable onSessionComplete) {
            this.mRemoteListener = listener;
            this.mOnSessionComplete = onSessionComplete;
        }

        public void onReadyForSpeech(Bundle params) throws RemoteException {
            this.mRemoteListener.onReadyForSpeech(params);
        }

        public void onBeginningOfSpeech() throws RemoteException {
            this.mRemoteListener.onBeginningOfSpeech();
        }

        public void onRmsChanged(float rmsdB) throws RemoteException {
            this.mRemoteListener.onRmsChanged(rmsdB);
        }

        public void onBufferReceived(byte[] buffer) throws RemoteException {
            this.mRemoteListener.onBufferReceived(buffer);
        }

        public void onEndOfSpeech() throws RemoteException {
            this.mRemoteListener.onEndOfSpeech();
        }

        public void onError(int error) throws RemoteException {
            this.mOnSessionComplete.run();
            this.mRemoteListener.onError(error);
        }

        public void onResults(Bundle results) throws RemoteException {
            this.mOnSessionComplete.run();
            this.mRemoteListener.onResults(results);
        }

        public void onPartialResults(Bundle results) throws RemoteException {
            this.mRemoteListener.onPartialResults(results);
        }

        public void onSegmentResults(Bundle results) throws RemoteException {
            this.mRemoteListener.onSegmentResults(results);
        }

        public void onEndOfSegmentedSession() throws RemoteException {
            this.mOnSessionComplete.run();
            this.mRemoteListener.onEndOfSegmentedSession();
        }

        public void onEvent(int eventType, Bundle params) throws RemoteException {
            this.mRemoteListener.onEvent(eventType, params);
        }
    }
}
