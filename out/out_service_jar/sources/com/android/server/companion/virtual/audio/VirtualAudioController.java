package com.android.server.companion.virtual.audio;

import android.companion.virtual.audio.IAudioConfigChangedCallback;
import android.companion.virtual.audio.IAudioRoutingCallback;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRecordingConfiguration;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.companion.virtual.GenericWindowPolicyController;
import com.android.server.companion.virtual.audio.AudioPlaybackDetector;
import com.android.server.companion.virtual.audio.AudioRecordingDetector;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public final class VirtualAudioController implements AudioPlaybackDetector.AudioPlaybackCallback, AudioRecordingDetector.AudioRecordingCallback, GenericWindowPolicyController.RunningAppsChangedListener {
    private static final String TAG = "VirtualAudioController";
    private static final int UPDATE_REROUTING_APPS_DELAY_MS = 2000;
    private final AudioPlaybackDetector mAudioPlaybackDetector;
    private final AudioRecordingDetector mAudioRecordingDetector;
    private IAudioConfigChangedCallback mConfigChangedCallback;
    private final Context mContext;
    private GenericWindowPolicyController mGenericWindowPolicyController;
    private IAudioRoutingCallback mRoutingCallback;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mUpdateAudioRoutingRunnable = new Runnable() { // from class: com.android.server.companion.virtual.audio.VirtualAudioController$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            VirtualAudioController.this.notifyAppsNeedingAudioRoutingChanged();
        }
    };
    private final Object mLock = new Object();
    private final ArraySet<Integer> mRunningAppUids = new ArraySet<>();
    private ArraySet<Integer> mPlayingAppUids = new ArraySet<>();
    private final Object mCallbackLock = new Object();

    public VirtualAudioController(Context context) {
        this.mContext = context;
        this.mAudioPlaybackDetector = new AudioPlaybackDetector(context);
        this.mAudioRecordingDetector = new AudioRecordingDetector(context);
    }

    public void startListening(GenericWindowPolicyController genericWindowPolicyController, IAudioRoutingCallback routingCallback, IAudioConfigChangedCallback configChangedCallback) {
        this.mGenericWindowPolicyController = genericWindowPolicyController;
        genericWindowPolicyController.registerRunningAppsChangedListener(this);
        synchronized (this.mCallbackLock) {
            this.mRoutingCallback = routingCallback;
            this.mConfigChangedCallback = configChangedCallback;
        }
        synchronized (this.mLock) {
            this.mRunningAppUids.clear();
            this.mPlayingAppUids.clear();
        }
        if (configChangedCallback != null) {
            this.mAudioPlaybackDetector.register(this);
            this.mAudioRecordingDetector.register(this);
        }
    }

    public void stopListening() {
        if (this.mHandler.hasCallbacks(this.mUpdateAudioRoutingRunnable)) {
            this.mHandler.removeCallbacks(this.mUpdateAudioRoutingRunnable);
        }
        this.mAudioPlaybackDetector.unregister();
        this.mAudioRecordingDetector.unregister();
        GenericWindowPolicyController genericWindowPolicyController = this.mGenericWindowPolicyController;
        if (genericWindowPolicyController != null) {
            genericWindowPolicyController.unregisterRunningAppsChangedListener(this);
            this.mGenericWindowPolicyController = null;
        }
        synchronized (this.mCallbackLock) {
            this.mRoutingCallback = null;
            this.mConfigChangedCallback = null;
        }
    }

    @Override // com.android.server.companion.virtual.GenericWindowPolicyController.RunningAppsChangedListener
    public void onRunningAppsChanged(ArraySet<Integer> runningUids) {
        synchronized (this.mLock) {
            if (this.mRunningAppUids.equals(runningUids)) {
                return;
            }
            this.mRunningAppUids.clear();
            this.mRunningAppUids.addAll((ArraySet<? extends Integer>) runningUids);
            ArraySet<Integer> oldPlayingAppUids = this.mPlayingAppUids;
            AudioManager audioManager = (AudioManager) this.mContext.getSystemService(AudioManager.class);
            List<AudioPlaybackConfiguration> configs = audioManager.getActivePlaybackConfigurations();
            ArraySet<Integer> findPlayingAppUids = findPlayingAppUids(configs, this.mRunningAppUids);
            this.mPlayingAppUids = findPlayingAppUids;
            if (!findPlayingAppUids.isEmpty()) {
                Slog.i(TAG, "Audio is playing, do not change rerouted apps");
            } else if (!oldPlayingAppUids.isEmpty()) {
                Slog.i(TAG, "The last playing app removed, delay change rerouted apps");
                if (this.mHandler.hasCallbacks(this.mUpdateAudioRoutingRunnable)) {
                    this.mHandler.removeCallbacks(this.mUpdateAudioRoutingRunnable);
                }
                this.mHandler.postDelayed(this.mUpdateAudioRoutingRunnable, 2000L);
            } else {
                notifyAppsNeedingAudioRoutingChanged();
            }
        }
    }

    @Override // com.android.server.companion.virtual.audio.AudioPlaybackDetector.AudioPlaybackCallback
    public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
        List<AudioPlaybackConfiguration> audioPlaybackConfigurations;
        updatePlayingApplications(configs);
        synchronized (this.mLock) {
            audioPlaybackConfigurations = findPlaybackConfigurations(configs, this.mRunningAppUids);
        }
        synchronized (this.mCallbackLock) {
            IAudioConfigChangedCallback iAudioConfigChangedCallback = this.mConfigChangedCallback;
            if (iAudioConfigChangedCallback != null) {
                try {
                    iAudioConfigChangedCallback.onPlaybackConfigChanged(audioPlaybackConfigurations);
                } catch (RemoteException e) {
                    Slog.e(TAG, "RemoteException when calling onPlaybackConfigChanged", e);
                }
            }
        }
    }

    @Override // com.android.server.companion.virtual.audio.AudioRecordingDetector.AudioRecordingCallback
    public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
        List<AudioRecordingConfiguration> audioRecordingConfigurations;
        synchronized (this.mLock) {
            audioRecordingConfigurations = findRecordingConfigurations(configs, this.mRunningAppUids);
        }
        synchronized (this.mCallbackLock) {
            IAudioConfigChangedCallback iAudioConfigChangedCallback = this.mConfigChangedCallback;
            if (iAudioConfigChangedCallback != null) {
                try {
                    iAudioConfigChangedCallback.onRecordingConfigChanged(audioRecordingConfigurations);
                } catch (RemoteException e) {
                    Slog.e(TAG, "RemoteException when calling onRecordingConfigChanged", e);
                }
            }
        }
    }

    private void updatePlayingApplications(List<AudioPlaybackConfiguration> configs) {
        synchronized (this.mLock) {
            ArraySet<Integer> playingAppUids = findPlayingAppUids(configs, this.mRunningAppUids);
            if (this.mPlayingAppUids.equals(playingAppUids)) {
                return;
            }
            this.mPlayingAppUids = playingAppUids;
            notifyAppsNeedingAudioRoutingChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyAppsNeedingAudioRoutingChanged() {
        int[] runningUids;
        if (this.mHandler.hasCallbacks(this.mUpdateAudioRoutingRunnable)) {
            this.mHandler.removeCallbacks(this.mUpdateAudioRoutingRunnable);
        }
        synchronized (this.mLock) {
            runningUids = new int[this.mRunningAppUids.size()];
            for (int i = 0; i < this.mRunningAppUids.size(); i++) {
                runningUids[i] = this.mRunningAppUids.valueAt(i).intValue();
            }
        }
        synchronized (this.mCallbackLock) {
            IAudioRoutingCallback iAudioRoutingCallback = this.mRoutingCallback;
            if (iAudioRoutingCallback != null) {
                try {
                    iAudioRoutingCallback.onAppsNeedingAudioRoutingChanged(runningUids);
                } catch (RemoteException e) {
                    Slog.e(TAG, "RemoteException when calling updateReroutingApps", e);
                }
            }
        }
    }

    private static ArraySet<Integer> findPlayingAppUids(List<AudioPlaybackConfiguration> configs, ArraySet<Integer> runningAppUids) {
        ArraySet<Integer> playingAppUids = new ArraySet<>();
        for (AudioPlaybackConfiguration config : configs) {
            if (runningAppUids.contains(Integer.valueOf(config.getClientUid())) && config.getPlayerState() == 2) {
                playingAppUids.add(Integer.valueOf(config.getClientUid()));
            }
        }
        return playingAppUids;
    }

    private static List<AudioPlaybackConfiguration> findPlaybackConfigurations(List<AudioPlaybackConfiguration> configs, ArraySet<Integer> runningAppUids) {
        List<AudioPlaybackConfiguration> runningConfigs = new ArrayList<>();
        for (AudioPlaybackConfiguration config : configs) {
            if (runningAppUids.contains(Integer.valueOf(config.getClientUid()))) {
                runningConfigs.add(config);
            }
        }
        return runningConfigs;
    }

    private static List<AudioRecordingConfiguration> findRecordingConfigurations(List<AudioRecordingConfiguration> configs, ArraySet<Integer> runningAppUids) {
        List<AudioRecordingConfiguration> runningConfigs = new ArrayList<>();
        for (AudioRecordingConfiguration config : configs) {
            if (runningAppUids.contains(Integer.valueOf(config.getClientUid()))) {
                runningConfigs.add(config);
            }
        }
        return runningConfigs;
    }

    boolean hasPendingRunnable() {
        return this.mHandler.hasCallbacks(this.mUpdateAudioRoutingRunnable);
    }

    void addPlayingAppsForTesting(int appUid) {
        synchronized (this.mLock) {
            this.mPlayingAppUids.add(Integer.valueOf(appUid));
        }
    }
}
