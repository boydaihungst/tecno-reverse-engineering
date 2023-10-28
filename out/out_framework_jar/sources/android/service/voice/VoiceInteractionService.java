package android.service.voice;

import android.annotation.SystemApi;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.soundtrigger.KeyphraseEnrollmentInfo;
import android.media.voice.KeyphraseModelManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SharedMemory;
import android.provider.Settings;
import android.service.voice.AlwaysOnHotwordDetector;
import android.service.voice.HotwordDetector;
import android.service.voice.IVoiceInteractionService;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.app.IVoiceActionCheckCallback;
import com.android.internal.app.IVoiceInteractionManagerService;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
/* loaded from: classes3.dex */
public class VoiceInteractionService extends Service {
    public static final String SERVICE_INTERFACE = "android.service.voice.VoiceInteractionService";
    public static final String SERVICE_META_DATA = "android.voice_interaction";
    static final String TAG = VoiceInteractionService.class.getSimpleName();
    private AlwaysOnHotwordDetector mHotwordDetector;
    private KeyphraseEnrollmentInfo mKeyphraseEnrollmentInfo;
    private SoftwareHotwordDetector mSoftwareHotwordDetector;
    IVoiceInteractionManagerService mSystemService;
    IVoiceInteractionService mInterface = new AnonymousClass1();
    private final Object mLock = new Object();
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() { // from class: android.service.voice.VoiceInteractionService$$ExternalSyntheticLambda1
        @Override // android.os.IBinder.DeathRecipient
        public final void binderDied() {
            VoiceInteractionService.this.m3718lambda$new$0$androidservicevoiceVoiceInteractionService();
        }
    };

    /* renamed from: android.service.voice.VoiceInteractionService$1  reason: invalid class name */
    /* loaded from: classes3.dex */
    class AnonymousClass1 extends IVoiceInteractionService.Stub {
        AnonymousClass1() {
        }

        @Override // android.service.voice.IVoiceInteractionService
        public void ready() {
            Handler.getMain().executeOrSendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: android.service.voice.VoiceInteractionService$1$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((VoiceInteractionService) obj).onReady();
                }
            }, VoiceInteractionService.this));
        }

        @Override // android.service.voice.IVoiceInteractionService
        public void shutdown() {
            Handler.getMain().executeOrSendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: android.service.voice.VoiceInteractionService$1$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((VoiceInteractionService) obj).onShutdownInternal();
                }
            }, VoiceInteractionService.this));
        }

        @Override // android.service.voice.IVoiceInteractionService
        public void soundModelsChanged() {
            Handler.getMain().executeOrSendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: android.service.voice.VoiceInteractionService$1$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((VoiceInteractionService) obj).onSoundModelsChangedInternal();
                }
            }, VoiceInteractionService.this));
        }

        @Override // android.service.voice.IVoiceInteractionService
        public void launchVoiceAssistFromKeyguard() {
            Handler.getMain().executeOrSendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: android.service.voice.VoiceInteractionService$1$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((VoiceInteractionService) obj).onLaunchVoiceAssistFromKeyguard();
                }
            }, VoiceInteractionService.this));
        }

        @Override // android.service.voice.IVoiceInteractionService
        public void getActiveServiceSupportedActions(List<String> voiceActions, IVoiceActionCheckCallback callback) {
            Handler.getMain().executeOrSendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: android.service.voice.VoiceInteractionService$1$$ExternalSyntheticLambda0
                @Override // com.android.internal.util.function.TriConsumer
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((VoiceInteractionService) obj).onHandleVoiceActionCheck((List) obj2, (IVoiceActionCheckCallback) obj3);
                }
            }, VoiceInteractionService.this, voiceActions, callback));
        }
    }

    public void onLaunchVoiceAssistFromKeyguard() {
    }

    public static boolean isActiveService(Context context, ComponentName service) {
        ComponentName curComp;
        String cur = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.VOICE_INTERACTION_SERVICE);
        if (cur == null || cur.isEmpty() || (curComp = ComponentName.unflattenFromString(cur)) == null) {
            return false;
        }
        return curComp.equals(service);
    }

    public void setDisabledShowContext(int flags) {
        try {
            this.mSystemService.setDisabledShowContext(flags);
        } catch (RemoteException e) {
        }
    }

    public int getDisabledShowContext() {
        try {
            return this.mSystemService.getDisabledShowContext();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public void showSession(Bundle args, int flags) {
        IVoiceInteractionManagerService iVoiceInteractionManagerService = this.mSystemService;
        if (iVoiceInteractionManagerService == null) {
            throw new IllegalStateException("Not available until onReady() is called");
        }
        try {
            iVoiceInteractionManagerService.showSession(args, flags);
        } catch (RemoteException e) {
        }
    }

    public Set<String> onGetSupportedVoiceActions(Set<String> voiceActions) {
        return Collections.emptySet();
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return this.mInterface.asBinder();
        }
        return null;
    }

    public void onReady() {
        IVoiceInteractionManagerService asInterface = IVoiceInteractionManagerService.Stub.asInterface(ServiceManager.getService(Context.VOICE_INTERACTION_MANAGER_SERVICE));
        this.mSystemService = asInterface;
        Objects.requireNonNull(asInterface);
        try {
            this.mSystemService.asBinder().linkToDeath(this.mDeathRecipient, 0);
        } catch (RemoteException e) {
            Log.wtf(TAG, "unable to link to death with system service");
        }
        this.mKeyphraseEnrollmentInfo = new KeyphraseEnrollmentInfo(getPackageManager());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$android-service-voice-VoiceInteractionService  reason: not valid java name */
    public /* synthetic */ void m3718lambda$new$0$androidservicevoiceVoiceInteractionService() {
        Log.e(TAG, "system service binder died shutting down");
        Handler.getMain().executeOrSendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: android.service.voice.VoiceInteractionService$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((VoiceInteractionService) obj).onShutdownInternal();
            }
        }, this));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onShutdownInternal() {
        onShutdown();
        safelyShutdownAllHotwordDetectors();
    }

    public void onShutdown() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSoundModelsChangedInternal() {
        synchronized (this) {
            AlwaysOnHotwordDetector alwaysOnHotwordDetector = this.mHotwordDetector;
            if (alwaysOnHotwordDetector != null) {
                alwaysOnHotwordDetector.onSoundModelsChanged();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onHandleVoiceActionCheck(List<String> voiceActions, IVoiceActionCheckCallback callback) {
        if (callback != null) {
            try {
                Set<String> voiceActionsSet = new ArraySet<>(voiceActions);
                Set<String> resultSet = onGetSupportedVoiceActions(voiceActionsSet);
                callback.onComplete(new ArrayList(resultSet));
            } catch (RemoteException e) {
            }
        }
    }

    @SystemApi
    public final AlwaysOnHotwordDetector createAlwaysOnHotwordDetector(String keyphrase, Locale locale, AlwaysOnHotwordDetector.Callback callback) {
        return createAlwaysOnHotwordDetectorInternal(keyphrase, locale, false, null, null, callback);
    }

    @SystemApi
    public final AlwaysOnHotwordDetector createAlwaysOnHotwordDetector(String keyphrase, Locale locale, PersistableBundle options, SharedMemory sharedMemory, AlwaysOnHotwordDetector.Callback callback) {
        return createAlwaysOnHotwordDetectorInternal(keyphrase, locale, true, options, sharedMemory, callback);
    }

    private AlwaysOnHotwordDetector createAlwaysOnHotwordDetectorInternal(String keyphrase, Locale locale, boolean supportHotwordDetectionService, PersistableBundle options, SharedMemory sharedMemory, AlwaysOnHotwordDetector.Callback callback) {
        if (this.mSystemService == null) {
            throw new IllegalStateException("Not available until onReady() is called");
        }
        synchronized (this.mLock) {
            safelyShutdownAllHotwordDetectors();
            AlwaysOnHotwordDetector alwaysOnHotwordDetector = new AlwaysOnHotwordDetector(keyphrase, locale, callback, this.mKeyphraseEnrollmentInfo, this.mSystemService, getApplicationContext().getApplicationInfo().targetSdkVersion, supportHotwordDetectionService, options, sharedMemory);
            this.mHotwordDetector = alwaysOnHotwordDetector;
            alwaysOnHotwordDetector.registerOnDestroyListener(new Consumer() { // from class: android.service.voice.VoiceInteractionService$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    VoiceInteractionService.this.m3716x48cac540((AbstractHotwordDetector) obj);
                }
            });
        }
        return this.mHotwordDetector;
    }

    @SystemApi
    public final HotwordDetector createHotwordDetector(PersistableBundle options, SharedMemory sharedMemory, HotwordDetector.Callback callback) {
        if (this.mSystemService == null) {
            throw new IllegalStateException("Not available until onReady() is called");
        }
        synchronized (this.mLock) {
            safelyShutdownAllHotwordDetectors();
            SoftwareHotwordDetector softwareHotwordDetector = new SoftwareHotwordDetector(this.mSystemService, null, options, sharedMemory, callback);
            this.mSoftwareHotwordDetector = softwareHotwordDetector;
            softwareHotwordDetector.registerOnDestroyListener(new Consumer() { // from class: android.service.voice.VoiceInteractionService$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    VoiceInteractionService.this.m3717xbc5a69d2((AbstractHotwordDetector) obj);
                }
            });
        }
        return this.mSoftwareHotwordDetector;
    }

    @SystemApi
    public final KeyphraseModelManager createKeyphraseModelManager() {
        KeyphraseModelManager keyphraseModelManager;
        if (this.mSystemService == null) {
            throw new IllegalStateException("Not available until onReady() is called");
        }
        synchronized (this.mLock) {
            keyphraseModelManager = new KeyphraseModelManager(this.mSystemService);
        }
        return keyphraseModelManager;
    }

    protected final KeyphraseEnrollmentInfo getKeyphraseEnrollmentInfo() {
        return this.mKeyphraseEnrollmentInfo;
    }

    public final boolean isKeyphraseAndLocaleSupportedForHotword(String keyphrase, Locale locale) {
        KeyphraseEnrollmentInfo keyphraseEnrollmentInfo = this.mKeyphraseEnrollmentInfo;
        return (keyphraseEnrollmentInfo == null || keyphraseEnrollmentInfo.getKeyphraseMetadata(keyphrase, locale) == null) ? false : true;
    }

    private void safelyShutdownAllHotwordDetectors() {
        synchronized (this.mLock) {
            AlwaysOnHotwordDetector alwaysOnHotwordDetector = this.mHotwordDetector;
            if (alwaysOnHotwordDetector != null) {
                try {
                    alwaysOnHotwordDetector.destroy();
                } catch (Exception ex) {
                    Log.i(TAG, "exception destroying AlwaysOnHotwordDetector", ex);
                }
            }
            SoftwareHotwordDetector softwareHotwordDetector = this.mSoftwareHotwordDetector;
            if (softwareHotwordDetector != null) {
                try {
                    softwareHotwordDetector.destroy();
                } catch (Exception ex2) {
                    Log.i(TAG, "exception destroying SoftwareHotwordDetector", ex2);
                }
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onDspHotwordDetectorDestroyed */
    public void m3716x48cac540(AlwaysOnHotwordDetector detector) {
        synchronized (this.mLock) {
            this.mHotwordDetector = null;
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onMicrophoneHotwordDetectorDestroyed */
    public void m3717xbc5a69d2(SoftwareHotwordDetector detector) {
        synchronized (this.mLock) {
            this.mSoftwareHotwordDetector = null;
        }
    }

    public final void setUiHints(Bundle hints) {
        if (hints == null) {
            throw new IllegalArgumentException("Hints must be non-null");
        }
        try {
            this.mSystemService.setUiHints(hints);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.app.Service
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("VOICE INTERACTION");
        synchronized (this.mLock) {
            pw.println("  AlwaysOnHotwordDetector");
            AlwaysOnHotwordDetector alwaysOnHotwordDetector = this.mHotwordDetector;
            if (alwaysOnHotwordDetector == null) {
                pw.println("    NULL");
            } else {
                alwaysOnHotwordDetector.dump("    ", pw);
            }
            pw.println("  MicrophoneHotwordDetector");
            SoftwareHotwordDetector softwareHotwordDetector = this.mSoftwareHotwordDetector;
            if (softwareHotwordDetector == null) {
                pw.println("    NULL");
            } else {
                softwareHotwordDetector.dump("    ", pw);
            }
        }
    }
}
