package com.android.server.soundtrigger_middleware;

import android.media.soundtrigger.ModelParameterRange;
import android.media.soundtrigger.PhraseSoundModel;
import android.media.soundtrigger.Properties;
import android.media.soundtrigger.RecognitionConfig;
import android.media.soundtrigger.SoundModel;
import android.os.IBinder;
import android.util.Log;
import com.android.server.soundtrigger_middleware.ISoundTriggerHal;
import com.android.server.soundtrigger_middleware.SoundTriggerHalWatchdog;
import com.android.server.soundtrigger_middleware.UptimeTimer;
import java.util.Objects;
/* loaded from: classes2.dex */
public class SoundTriggerHalWatchdog implements ISoundTriggerHal {
    private static final String TAG = "SoundTriggerHalWatchdog";
    private static final long TIMEOUT_MS = 3000;
    private final UptimeTimer mTimer = new UptimeTimer(TAG);
    private final ISoundTriggerHal mUnderlying;

    public SoundTriggerHalWatchdog(ISoundTriggerHal underlying) {
        this.mUnderlying = (ISoundTriggerHal) Objects.requireNonNull(underlying);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public Properties getProperties() {
        Watchdog ignore = new Watchdog();
        try {
            Properties properties = this.mUnderlying.getProperties();
            ignore.close();
            return properties;
        } catch (Throwable th) {
            try {
                ignore.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void registerCallback(ISoundTriggerHal.GlobalCallback callback) {
        Watchdog ignore = new Watchdog();
        try {
            this.mUnderlying.registerCallback(callback);
            ignore.close();
        } catch (Throwable th) {
            try {
                ignore.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int loadSoundModel(SoundModel soundModel, ISoundTriggerHal.ModelCallback callback) {
        Watchdog ignore = new Watchdog();
        try {
            int loadSoundModel = this.mUnderlying.loadSoundModel(soundModel, callback);
            ignore.close();
            return loadSoundModel;
        } catch (Throwable th) {
            try {
                ignore.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int loadPhraseSoundModel(PhraseSoundModel soundModel, ISoundTriggerHal.ModelCallback callback) {
        Watchdog ignore = new Watchdog();
        try {
            int loadPhraseSoundModel = this.mUnderlying.loadPhraseSoundModel(soundModel, callback);
            ignore.close();
            return loadPhraseSoundModel;
        } catch (Throwable th) {
            try {
                ignore.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void unloadSoundModel(int modelHandle) {
        Watchdog ignore = new Watchdog();
        try {
            this.mUnderlying.unloadSoundModel(modelHandle);
            ignore.close();
        } catch (Throwable th) {
            try {
                ignore.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void stopRecognition(int modelHandle) {
        Watchdog ignore = new Watchdog();
        try {
            this.mUnderlying.stopRecognition(modelHandle);
            ignore.close();
        } catch (Throwable th) {
            try {
                ignore.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void startRecognition(int modelHandle, int deviceHandle, int ioHandle, RecognitionConfig config) {
        Watchdog ignore = new Watchdog();
        try {
            this.mUnderlying.startRecognition(modelHandle, deviceHandle, ioHandle, config);
            ignore.close();
        } catch (Throwable th) {
            try {
                ignore.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void forceRecognitionEvent(int modelHandle) {
        Watchdog ignore = new Watchdog();
        try {
            this.mUnderlying.forceRecognitionEvent(modelHandle);
            ignore.close();
        } catch (Throwable th) {
            try {
                ignore.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int getModelParameter(int modelHandle, int param) {
        Watchdog ignore = new Watchdog();
        try {
            int modelParameter = this.mUnderlying.getModelParameter(modelHandle, param);
            ignore.close();
            return modelParameter;
        } catch (Throwable th) {
            try {
                ignore.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void setModelParameter(int modelHandle, int param, int value) {
        Watchdog ignore = new Watchdog();
        try {
            this.mUnderlying.setModelParameter(modelHandle, param, value);
            ignore.close();
        } catch (Throwable th) {
            try {
                ignore.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public ModelParameterRange queryParameter(int modelHandle, int param) {
        Watchdog ignore = new Watchdog();
        try {
            ModelParameterRange queryParameter = this.mUnderlying.queryParameter(modelHandle, param);
            ignore.close();
            return queryParameter;
        } catch (Throwable th) {
            try {
                ignore.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void linkToDeath(IBinder.DeathRecipient recipient) {
        this.mUnderlying.linkToDeath(recipient);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void unlinkToDeath(IBinder.DeathRecipient recipient) {
        this.mUnderlying.unlinkToDeath(recipient);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public String interfaceDescriptor() {
        return this.mUnderlying.interfaceDescriptor();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void flushCallbacks() {
        this.mUnderlying.flushCallbacks();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void reboot() {
        this.mUnderlying.reboot();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void detach() {
        Log.i(TAG, "[detach]");
        this.mUnderlying.detach();
        this.mTimer.onDetach();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Watchdog implements AutoCloseable {
        private final Exception mException = new Exception();
        private final UptimeTimer.Task mTask;

        Watchdog() {
            this.mTask = SoundTriggerHalWatchdog.this.mTimer.createTask(new Runnable() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHalWatchdog$Watchdog$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SoundTriggerHalWatchdog.Watchdog.this.m6544x413b9cea();
                }
            }, 3000L);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$0$com-android-server-soundtrigger_middleware-SoundTriggerHalWatchdog$Watchdog  reason: not valid java name */
        public /* synthetic */ void m6544x413b9cea() {
            Log.e(SoundTriggerHalWatchdog.TAG, "HAL deadline expired. Rebooting.", this.mException);
            SoundTriggerHalWatchdog.this.reboot();
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            this.mTask.cancel();
        }
    }
}
