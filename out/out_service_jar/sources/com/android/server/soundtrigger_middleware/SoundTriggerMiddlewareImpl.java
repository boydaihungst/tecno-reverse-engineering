package com.android.server.soundtrigger_middleware;

import android.media.soundtrigger_middleware.ISoundTriggerCallback;
import android.media.soundtrigger_middleware.ISoundTriggerModule;
import android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public class SoundTriggerMiddlewareImpl implements ISoundTriggerMiddlewareInternal {
    private static final String TAG = "SoundTriggerMiddlewareImpl";
    private final SoundTriggerModule[] mModules;

    /* loaded from: classes2.dex */
    public static abstract class AudioSessionProvider {
        public abstract AudioSession acquireSession();

        public abstract void releaseSession(int i);

        /* loaded from: classes2.dex */
        public static final class AudioSession {
            final int mDeviceHandle;
            final int mIoHandle;
            final int mSessionHandle;

            AudioSession(int sessionHandle, int ioHandle, int deviceHandle) {
                this.mSessionHandle = sessionHandle;
                this.mIoHandle = ioHandle;
                this.mDeviceHandle = deviceHandle;
            }
        }
    }

    public SoundTriggerMiddlewareImpl(HalFactory[] halFactories, AudioSessionProvider audioSessionProvider) {
        List<SoundTriggerModule> modules = new ArrayList<>(halFactories.length);
        for (HalFactory halFactory : halFactories) {
            try {
                modules.add(new SoundTriggerModule(halFactory, audioSessionProvider));
            } catch (Exception e) {
                Log.e(TAG, "Failed to add a SoundTriggerModule instance", e);
            }
        }
        this.mModules = (SoundTriggerModule[]) modules.toArray(new SoundTriggerModule[0]);
    }

    public SoundTriggerMiddlewareImpl(HalFactory factory, AudioSessionProvider audioSessionProvider) {
        this(new HalFactory[]{factory}, audioSessionProvider);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerMiddlewareInternal
    public SoundTriggerModuleDescriptor[] listModules() {
        SoundTriggerModuleDescriptor[] result = new SoundTriggerModuleDescriptor[this.mModules.length];
        for (int i = 0; i < this.mModules.length; i++) {
            SoundTriggerModuleDescriptor desc = new SoundTriggerModuleDescriptor();
            desc.handle = i;
            desc.properties = this.mModules[i].getProperties();
            result[i] = desc;
        }
        return result;
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerMiddlewareInternal
    public ISoundTriggerModule attach(int handle, ISoundTriggerCallback callback) {
        return this.mModules[handle].attach(callback);
    }
}
