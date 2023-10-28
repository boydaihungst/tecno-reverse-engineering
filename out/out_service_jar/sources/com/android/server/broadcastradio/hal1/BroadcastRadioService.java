package com.android.server.broadcastradio.hal1;

import android.hardware.radio.ITuner;
import android.hardware.radio.ITunerCallback;
import android.hardware.radio.RadioManager;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
public class BroadcastRadioService {
    private final Object mLock;
    private final long mNativeContext = nativeInit();

    private native void nativeFinalize(long j);

    private native long nativeInit();

    private native List<RadioManager.ModuleProperties> nativeLoadModules(long j);

    private native Tuner nativeOpenTuner(long j, int i, RadioManager.BandConfig bandConfig, boolean z, ITunerCallback iTunerCallback);

    protected void finalize() throws Throwable {
        nativeFinalize(this.mNativeContext);
        super.finalize();
    }

    public BroadcastRadioService(Object lock) {
        this.mLock = lock;
    }

    public List<RadioManager.ModuleProperties> loadModules() {
        List<RadioManager.ModuleProperties> list;
        synchronized (this.mLock) {
            list = (List) Objects.requireNonNull(nativeLoadModules(this.mNativeContext));
        }
        return list;
    }

    public ITuner openTuner(int moduleId, RadioManager.BandConfig bandConfig, boolean withAudio, ITunerCallback callback) {
        Tuner nativeOpenTuner;
        synchronized (this.mLock) {
            nativeOpenTuner = nativeOpenTuner(this.mNativeContext, moduleId, bandConfig, withAudio, callback);
        }
        return nativeOpenTuner;
    }
}
