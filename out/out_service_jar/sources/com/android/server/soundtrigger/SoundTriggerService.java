package com.android.server.soundtrigger;

import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger;
import android.hardware.soundtrigger.SoundTriggerModule;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.permission.ClearCallingIdentityContext;
import android.media.permission.Identity;
import android.media.permission.IdentityContext;
import android.media.permission.PermissionUtil;
import android.media.permission.SafeCloseable;
import android.media.soundtrigger.ISoundTriggerDetectionService;
import android.media.soundtrigger.ISoundTriggerDetectionServiceClient;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.app.ISoundTriggerService;
import com.android.internal.app.ISoundTriggerSession;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.soundtrigger.SoundTriggerHelper;
import com.android.server.soundtrigger.SoundTriggerInternal;
import com.android.server.soundtrigger.SoundTriggerLogger;
import com.android.server.soundtrigger.SoundTriggerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class SoundTriggerService extends SystemService {
    private static final boolean DEBUG = true;
    private static final String TAG = "SoundTriggerService";
    private static final SoundTriggerLogger sEventLogger = new SoundTriggerLogger(200, "SoundTrigger activity");
    final Context mContext;
    private SoundTriggerDbHelper mDbHelper;
    private final LocalSoundTriggerService mLocalSoundTriggerService;
    private Object mLock;
    private final ArrayMap<String, NumOps> mNumOpsPerPackage;
    private final SoundTriggerServiceStub mServiceStub;
    private final SoundModelStatTracker mSoundModelStatTracker;

    /* loaded from: classes2.dex */
    class SoundModelStatTracker {
        private final TreeMap<UUID, SoundModelStat> mModelStats = new TreeMap<>();

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class SoundModelStat {
            long mStartCount = 0;
            long mTotalTimeMsec = 0;
            long mLastStartTimestampMsec = 0;
            long mLastStopTimestampMsec = 0;
            boolean mIsStarted = false;

            SoundModelStat() {
            }
        }

        SoundModelStatTracker() {
        }

        public synchronized void onStart(UUID id) {
            SoundModelStat stat = this.mModelStats.get(id);
            if (stat == null) {
                stat = new SoundModelStat();
                this.mModelStats.put(id, stat);
            }
            if (stat.mIsStarted) {
                Slog.w(SoundTriggerService.TAG, "error onStart(): Model " + id + " already started");
                return;
            }
            stat.mStartCount++;
            stat.mLastStartTimestampMsec = SystemClock.elapsedRealtime();
            stat.mIsStarted = true;
        }

        public synchronized void onStop(UUID id) {
            SoundModelStat stat = this.mModelStats.get(id);
            if (stat == null) {
                Slog.w(SoundTriggerService.TAG, "error onStop(): Model " + id + " has no stats available");
            } else if (!stat.mIsStarted) {
                Slog.w(SoundTriggerService.TAG, "error onStop(): Model " + id + " already stopped");
            } else {
                stat.mLastStopTimestampMsec = SystemClock.elapsedRealtime();
                stat.mTotalTimeMsec += stat.mLastStopTimestampMsec - stat.mLastStartTimestampMsec;
                stat.mIsStarted = false;
            }
        }

        public synchronized void dump(PrintWriter pw) {
            long curTime = SystemClock.elapsedRealtime();
            pw.println("Model Stats:");
            for (Map.Entry<UUID, SoundModelStat> entry : this.mModelStats.entrySet()) {
                UUID uuid = entry.getKey();
                SoundModelStat stat = entry.getValue();
                long totalTimeMsec = stat.mTotalTimeMsec;
                if (stat.mIsStarted) {
                    totalTimeMsec += curTime - stat.mLastStartTimestampMsec;
                }
                pw.println(uuid + ", total_time(msec)=" + totalTimeMsec + ", total_count=" + stat.mStartCount + ", last_start=" + stat.mLastStartTimestampMsec + ", last_stop=" + stat.mLastStopTimestampMsec);
            }
        }
    }

    public SoundTriggerService(Context context) {
        super(context);
        this.mNumOpsPerPackage = new ArrayMap<>();
        this.mContext = context;
        this.mServiceStub = new SoundTriggerServiceStub();
        this.mLocalSoundTriggerService = new LocalSoundTriggerService(context);
        this.mLock = new Object();
        this.mSoundModelStatTracker = new SoundModelStatTracker();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("soundtrigger", this.mServiceStub);
        publishLocalService(SoundTriggerInternal.class, this.mLocalSoundTriggerService);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        Slog.d(TAG, "onBootPhase: " + phase + " : " + isSafeMode());
        if (600 == phase) {
            this.mDbHelper = new SoundTriggerDbHelper(this.mContext);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public SoundTriggerHelper newSoundTriggerHelper() {
        final Identity middlemanIdentity = new Identity();
        middlemanIdentity.packageName = ActivityThread.currentOpPackageName();
        final Identity originatorIdentity = IdentityContext.getNonNull();
        return new SoundTriggerHelper(this.mContext, new SoundTriggerHelper.SoundTriggerModuleProvider() { // from class: com.android.server.soundtrigger.SoundTriggerService.1
            @Override // com.android.server.soundtrigger.SoundTriggerHelper.SoundTriggerModuleProvider
            public int listModuleProperties(ArrayList<SoundTrigger.ModuleProperties> modules) {
                return SoundTrigger.listModulesAsMiddleman(modules, middlemanIdentity, originatorIdentity);
            }

            @Override // com.android.server.soundtrigger.SoundTriggerHelper.SoundTriggerModuleProvider
            public SoundTriggerModule getModule(int moduleId, SoundTrigger.StatusListener statusListener) {
                return SoundTrigger.attachModuleAsMiddleman(moduleId, statusListener, (Handler) null, middlemanIdentity, originatorIdentity);
            }
        });
    }

    /* loaded from: classes2.dex */
    class SoundTriggerServiceStub extends ISoundTriggerService.Stub {
        SoundTriggerServiceStub() {
        }

        public ISoundTriggerSession attachAsOriginator(Identity originatorIdentity, IBinder client) {
            SafeCloseable ignored = PermissionUtil.establishIdentityDirect(originatorIdentity);
            try {
                SoundTriggerSessionStub soundTriggerSessionStub = new SoundTriggerSessionStub(client);
                if (ignored != null) {
                    ignored.close();
                }
                return soundTriggerSessionStub;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public ISoundTriggerSession attachAsMiddleman(Identity originatorIdentity, Identity middlemanIdentity, IBinder client) {
            SafeCloseable ignored = PermissionUtil.establishIdentityIndirect(SoundTriggerService.this.mContext, "android.permission.SOUNDTRIGGER_DELEGATE_IDENTITY", middlemanIdentity, originatorIdentity);
            try {
                SoundTriggerSessionStub soundTriggerSessionStub = new SoundTriggerSessionStub(client);
                if (ignored != null) {
                    ignored.close();
                }
                return soundTriggerSessionStub;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class SoundTriggerSessionStub extends ISoundTriggerSession.Stub {
        private final IBinder mClient;
        private final SoundTriggerHelper mSoundTriggerHelper;
        private final TreeMap<UUID, SoundTrigger.SoundModel> mLoadedModels = new TreeMap<>();
        private final Object mCallbacksLock = new Object();
        private final TreeMap<UUID, IRecognitionStatusCallback> mCallbacks = new TreeMap<>();
        private final Identity mOriginatorIdentity = IdentityContext.getNonNull();

        SoundTriggerSessionStub(IBinder client) {
            this.mSoundTriggerHelper = SoundTriggerService.this.newSoundTriggerHelper();
            this.mClient = client;
            try {
                client.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.soundtrigger.SoundTriggerService$SoundTriggerSessionStub$$ExternalSyntheticLambda0
                    @Override // android.os.IBinder.DeathRecipient
                    public final void binderDied() {
                        SoundTriggerService.SoundTriggerSessionStub.this.m6522xe20a666a();
                    }
                }, 0);
            } catch (RemoteException e) {
                Slog.e(SoundTriggerService.TAG, "Failed to register death listener.", e);
            }
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (RuntimeException e) {
                if (!(e instanceof SecurityException)) {
                    Slog.wtf(SoundTriggerService.TAG, "SoundTriggerService Crash", e);
                }
                throw e;
            }
        }

        public int startRecognition(ParcelUuid parcelUuid, IRecognitionStatusCallback callback, SoundTrigger.RecognitionConfig config, boolean runInBatterySaverMode) {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                if (runInBatterySaverMode) {
                    enforceCallingPermission("android.permission.SOUND_TRIGGER_RUN_IN_BATTERY_SAVER");
                }
                Slog.i(SoundTriggerService.TAG, "startRecognition(): Uuid : " + parcelUuid);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("startRecognition(): Uuid : " + parcelUuid));
                SoundTrigger.GenericSoundModel model = getSoundModel(parcelUuid);
                if (model == null) {
                    Slog.w(SoundTriggerService.TAG, "Null model in database for id: " + parcelUuid);
                    SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("startRecognition(): Null model in database for id: " + parcelUuid));
                    if (ignored != null) {
                        ignored.close();
                    }
                    return Integer.MIN_VALUE;
                }
                int ret = this.mSoundTriggerHelper.startGenericRecognition(parcelUuid.getUuid(), model, callback, config, runInBatterySaverMode);
                if (ret == 0) {
                    SoundTriggerService.this.mSoundModelStatTracker.onStart(parcelUuid.getUuid());
                }
                if (ignored != null) {
                    ignored.close();
                }
                return ret;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public int stopRecognition(ParcelUuid parcelUuid, IRecognitionStatusCallback callback) {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                Slog.i(SoundTriggerService.TAG, "stopRecognition(): Uuid : " + parcelUuid);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("stopRecognition(): Uuid : " + parcelUuid));
                int ret = this.mSoundTriggerHelper.stopGenericRecognition(parcelUuid.getUuid(), callback);
                if (ret == 0) {
                    SoundTriggerService.this.mSoundModelStatTracker.onStop(parcelUuid.getUuid());
                }
                if (ignored != null) {
                    ignored.close();
                }
                return ret;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public SoundTrigger.GenericSoundModel getSoundModel(ParcelUuid soundModelId) {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                Slog.i(SoundTriggerService.TAG, "getSoundModel(): id = " + soundModelId);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("getSoundModel(): id = " + soundModelId));
                SoundTrigger.GenericSoundModel model = SoundTriggerService.this.mDbHelper.getGenericSoundModel(soundModelId.getUuid());
                if (ignored != null) {
                    ignored.close();
                }
                return model;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public void updateSoundModel(SoundTrigger.GenericSoundModel soundModel) {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                Slog.i(SoundTriggerService.TAG, "updateSoundModel(): model = " + soundModel);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("updateSoundModel(): model = " + soundModel));
                SoundTriggerService.this.mDbHelper.updateGenericSoundModel(soundModel);
                if (ignored != null) {
                    ignored.close();
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public void deleteSoundModel(ParcelUuid soundModelId) {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                Slog.i(SoundTriggerService.TAG, "deleteSoundModel(): id = " + soundModelId);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("deleteSoundModel(): id = " + soundModelId));
                this.mSoundTriggerHelper.unloadGenericSoundModel(soundModelId.getUuid());
                SoundTriggerService.this.mSoundModelStatTracker.onStop(soundModelId.getUuid());
                SoundTriggerService.this.mDbHelper.deleteGenericSoundModel(soundModelId.getUuid());
                if (ignored != null) {
                    ignored.close();
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public int loadGenericSoundModel(SoundTrigger.GenericSoundModel soundModel) {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                if (soundModel != null && soundModel.getUuid() != null) {
                    Slog.i(SoundTriggerService.TAG, "loadGenericSoundModel(): id = " + soundModel.getUuid());
                    SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("loadGenericSoundModel(): id = " + soundModel.getUuid()));
                    synchronized (SoundTriggerService.this.mLock) {
                        SoundTrigger.SoundModel oldModel = this.mLoadedModels.get(soundModel.getUuid());
                        if (oldModel != null && !oldModel.equals(soundModel)) {
                            this.mSoundTriggerHelper.unloadGenericSoundModel(soundModel.getUuid());
                            synchronized (this.mCallbacksLock) {
                                this.mCallbacks.remove(soundModel.getUuid());
                            }
                        }
                        this.mLoadedModels.put(soundModel.getUuid(), soundModel);
                    }
                    if (ignored != null) {
                        ignored.close();
                    }
                    return 0;
                }
                Slog.w(SoundTriggerService.TAG, "Invalid sound model");
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("loadGenericSoundModel(): Invalid sound model"));
                if (ignored != null) {
                    ignored.close();
                }
                return Integer.MIN_VALUE;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public int loadKeyphraseSoundModel(SoundTrigger.KeyphraseSoundModel soundModel) {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                if (soundModel != null && soundModel.getUuid() != null) {
                    if (soundModel.getKeyphrases() != null && soundModel.getKeyphrases().length == 1) {
                        Slog.i(SoundTriggerService.TAG, "loadKeyphraseSoundModel(): id = " + soundModel.getUuid());
                        SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("loadKeyphraseSoundModel(): id = " + soundModel.getUuid()));
                        synchronized (SoundTriggerService.this.mLock) {
                            SoundTrigger.SoundModel oldModel = this.mLoadedModels.get(soundModel.getUuid());
                            if (oldModel != null && !oldModel.equals(soundModel)) {
                                this.mSoundTriggerHelper.unloadKeyphraseSoundModel(soundModel.getKeyphrases()[0].getId());
                                synchronized (this.mCallbacksLock) {
                                    this.mCallbacks.remove(soundModel.getUuid());
                                }
                            }
                            this.mLoadedModels.put(soundModel.getUuid(), soundModel);
                        }
                        if (ignored != null) {
                            ignored.close();
                        }
                        return 0;
                    }
                    Slog.w(SoundTriggerService.TAG, "Only one keyphrase per model is currently supported.");
                    SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("loadKeyphraseSoundModel(): Only one keyphrase per model is currently supported."));
                    if (ignored != null) {
                        ignored.close();
                    }
                    return Integer.MIN_VALUE;
                }
                Slog.w(SoundTriggerService.TAG, "Invalid sound model");
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("loadKeyphraseSoundModel(): Invalid sound model"));
                if (ignored != null) {
                    ignored.close();
                }
                return Integer.MIN_VALUE;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [566=5] */
        public int startRecognitionForService(ParcelUuid soundModelId, Bundle params, ComponentName detectionService, SoundTrigger.RecognitionConfig config) {
            IRecognitionStatusCallback existingCallback;
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                Objects.requireNonNull(soundModelId);
                Objects.requireNonNull(detectionService);
                Objects.requireNonNull(config);
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            } catch (Throwable th) {
                th = th;
            }
            try {
                enforceDetectionPermissions(detectionService);
                Slog.i(SoundTriggerService.TAG, "startRecognition(): id = " + soundModelId);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("startRecognitionForService(): id = " + soundModelId));
                IRecognitionStatusCallback callback = new RemoteSoundTriggerDetectionService(soundModelId.getUuid(), params, detectionService, Binder.getCallingUserHandle(), config);
                synchronized (SoundTriggerService.this.mLock) {
                    SoundTrigger.GenericSoundModel genericSoundModel = (SoundTrigger.SoundModel) this.mLoadedModels.get(soundModelId.getUuid());
                    if (genericSoundModel == null) {
                        Slog.w(SoundTriggerService.TAG, soundModelId + " is not loaded");
                        SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("startRecognitionForService():" + soundModelId + " is not loaded"));
                        if (ignored != null) {
                            ignored.close();
                        }
                        return Integer.MIN_VALUE;
                    }
                    synchronized (this.mCallbacksLock) {
                        existingCallback = this.mCallbacks.get(soundModelId.getUuid());
                    }
                    if (existingCallback != null) {
                        Slog.w(SoundTriggerService.TAG, soundModelId + " is already running");
                        SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("startRecognitionForService():" + soundModelId + " is already running"));
                        if (ignored != null) {
                            ignored.close();
                        }
                        return Integer.MIN_VALUE;
                    }
                    switch (genericSoundModel.getType()) {
                        case 1:
                            int ret = this.mSoundTriggerHelper.startGenericRecognition(genericSoundModel.getUuid(), genericSoundModel, callback, config, false);
                            if (ret != 0) {
                                Slog.e(SoundTriggerService.TAG, "Failed to start model: " + ret);
                                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("startRecognitionForService(): Failed to start model:"));
                                if (ignored != null) {
                                    ignored.close();
                                }
                                return ret;
                            }
                            synchronized (this.mCallbacksLock) {
                                this.mCallbacks.put(soundModelId.getUuid(), callback);
                            }
                            SoundTriggerService.this.mSoundModelStatTracker.onStart(soundModelId.getUuid());
                            if (ignored != null) {
                                ignored.close();
                            }
                            return 0;
                        default:
                            Slog.e(SoundTriggerService.TAG, "Unknown model type");
                            SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("startRecognitionForService(): Unknown model type"));
                            if (ignored != null) {
                                ignored.close();
                            }
                            return Integer.MIN_VALUE;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                Throwable th3 = th;
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th4) {
                        th3.addSuppressed(th4);
                    }
                }
                throw th3;
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [634=5] */
        public int stopRecognitionForService(ParcelUuid soundModelId) {
            IRecognitionStatusCallback callback;
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                Slog.i(SoundTriggerService.TAG, "stopRecognition(): id = " + soundModelId);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("stopRecognitionForService(): id = " + soundModelId));
                synchronized (SoundTriggerService.this.mLock) {
                    SoundTrigger.SoundModel soundModel = this.mLoadedModels.get(soundModelId.getUuid());
                    if (soundModel == null) {
                        Slog.w(SoundTriggerService.TAG, soundModelId + " is not loaded");
                        SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("stopRecognitionForService(): " + soundModelId + " is not loaded"));
                        if (ignored != null) {
                            ignored.close();
                        }
                        return Integer.MIN_VALUE;
                    }
                    synchronized (this.mCallbacksLock) {
                        callback = this.mCallbacks.get(soundModelId.getUuid());
                    }
                    if (callback == null) {
                        Slog.w(SoundTriggerService.TAG, soundModelId + " is not running");
                        SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("stopRecognitionForService(): " + soundModelId + " is not running"));
                        if (ignored != null) {
                            ignored.close();
                        }
                        return Integer.MIN_VALUE;
                    }
                    switch (soundModel.getType()) {
                        case 1:
                            int ret = this.mSoundTriggerHelper.stopGenericRecognition(soundModel.getUuid(), callback);
                            if (ret != 0) {
                                Slog.e(SoundTriggerService.TAG, "Failed to stop model: " + ret);
                                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("stopRecognitionForService(): Failed to stop model: " + ret));
                                if (ignored != null) {
                                    ignored.close();
                                }
                                return ret;
                            }
                            synchronized (this.mCallbacksLock) {
                                this.mCallbacks.remove(soundModelId.getUuid());
                            }
                            SoundTriggerService.this.mSoundModelStatTracker.onStop(soundModelId.getUuid());
                            if (ignored != null) {
                                ignored.close();
                            }
                            return 0;
                        default:
                            Slog.e(SoundTriggerService.TAG, "Unknown model type");
                            SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("stopRecognitionForService(): Unknown model type"));
                            if (ignored != null) {
                                ignored.close();
                            }
                            return Integer.MIN_VALUE;
                    }
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [686=4] */
        public int unloadSoundModel(ParcelUuid soundModelId) {
            int ret;
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                Slog.i(SoundTriggerService.TAG, "unloadSoundModel(): id = " + soundModelId);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("unloadSoundModel(): id = " + soundModelId));
                synchronized (SoundTriggerService.this.mLock) {
                    SoundTrigger.KeyphraseSoundModel keyphraseSoundModel = (SoundTrigger.SoundModel) this.mLoadedModels.get(soundModelId.getUuid());
                    if (keyphraseSoundModel == null) {
                        Slog.w(SoundTriggerService.TAG, soundModelId + " is not loaded");
                        SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("unloadSoundModel(): " + soundModelId + " is not loaded"));
                        if (ignored != null) {
                            ignored.close();
                        }
                        return Integer.MIN_VALUE;
                    }
                    switch (keyphraseSoundModel.getType()) {
                        case 0:
                            ret = this.mSoundTriggerHelper.unloadKeyphraseSoundModel(keyphraseSoundModel.getKeyphrases()[0].getId());
                            break;
                        case 1:
                            ret = this.mSoundTriggerHelper.unloadGenericSoundModel(keyphraseSoundModel.getUuid());
                            break;
                        default:
                            Slog.e(SoundTriggerService.TAG, "Unknown model type");
                            SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("unloadSoundModel(): Unknown model type"));
                            if (ignored != null) {
                                ignored.close();
                            }
                            return Integer.MIN_VALUE;
                    }
                    if (ret == 0) {
                        this.mLoadedModels.remove(soundModelId.getUuid());
                        if (ignored != null) {
                            ignored.close();
                        }
                        return 0;
                    }
                    Slog.e(SoundTriggerService.TAG, "Failed to unload model");
                    SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("unloadSoundModel(): Failed to unload model"));
                    if (ignored != null) {
                        ignored.close();
                    }
                    return ret;
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public boolean isRecognitionActive(ParcelUuid parcelUuid) {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                synchronized (this.mCallbacksLock) {
                    IRecognitionStatusCallback callback = this.mCallbacks.get(parcelUuid.getUuid());
                    if (callback != null) {
                        boolean isRecognitionRequested = this.mSoundTriggerHelper.isRecognitionRequested(parcelUuid.getUuid());
                        if (ignored != null) {
                            ignored.close();
                        }
                        return isRecognitionRequested;
                    }
                    if (ignored != null) {
                        ignored.close();
                    }
                    return false;
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public int getModelState(ParcelUuid soundModelId) {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                int ret = Integer.MIN_VALUE;
                Slog.i(SoundTriggerService.TAG, "getModelState(): id = " + soundModelId);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("getModelState(): id = " + soundModelId));
                synchronized (SoundTriggerService.this.mLock) {
                    SoundTrigger.SoundModel soundModel = this.mLoadedModels.get(soundModelId.getUuid());
                    if (soundModel == null) {
                        Slog.w(SoundTriggerService.TAG, soundModelId + " is not loaded");
                        SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("getModelState(): " + soundModelId + " is not loaded"));
                        if (ignored != null) {
                            ignored.close();
                        }
                        return Integer.MIN_VALUE;
                    }
                    switch (soundModel.getType()) {
                        case 1:
                            ret = this.mSoundTriggerHelper.getGenericModelState(soundModel.getUuid());
                            break;
                        default:
                            Slog.e(SoundTriggerService.TAG, "Unsupported model type, " + soundModel.getType());
                            SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("getModelState(): Unsupported model type, " + soundModel.getType()));
                            break;
                    }
                    if (ignored != null) {
                        ignored.close();
                    }
                    return ret;
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public SoundTrigger.ModuleProperties getModuleProperties() {
            SoundTrigger.ModuleProperties properties;
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                Slog.i(SoundTriggerService.TAG, "getModuleProperties()");
                synchronized (SoundTriggerService.this.mLock) {
                    properties = this.mSoundTriggerHelper.getModuleProperties();
                    SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("getModuleProperties(): " + properties));
                }
                if (ignored != null) {
                    ignored.close();
                }
                return properties;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public int setParameter(ParcelUuid soundModelId, int modelParam, int value) {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                Slog.d(SoundTriggerService.TAG, "setParameter(): id=" + soundModelId + ", param=" + modelParam + ", value=" + value);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("setParameter(): id=" + soundModelId + ", param=" + modelParam + ", value=" + value));
                synchronized (SoundTriggerService.this.mLock) {
                    SoundTrigger.SoundModel soundModel = this.mLoadedModels.get(soundModelId.getUuid());
                    if (soundModel == null) {
                        Slog.w(SoundTriggerService.TAG, soundModelId + " is not loaded. Loaded models: " + this.mLoadedModels.toString());
                        SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("setParameter(): " + soundModelId + " is not loaded"));
                        int i = SoundTrigger.STATUS_BAD_VALUE;
                        if (ignored != null) {
                            ignored.close();
                        }
                        return i;
                    }
                    int parameter = this.mSoundTriggerHelper.setParameter(soundModel.getUuid(), modelParam, value);
                    if (ignored != null) {
                        ignored.close();
                    }
                    return parameter;
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public int getParameter(ParcelUuid soundModelId, int modelParam) throws UnsupportedOperationException, IllegalArgumentException {
            int parameter;
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                Slog.d(SoundTriggerService.TAG, "getParameter(): id=" + soundModelId + ", param=" + modelParam);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("getParameter(): id=" + soundModelId + ", param=" + modelParam));
                synchronized (SoundTriggerService.this.mLock) {
                    SoundTrigger.SoundModel soundModel = this.mLoadedModels.get(soundModelId.getUuid());
                    if (soundModel == null) {
                        Slog.w(SoundTriggerService.TAG, soundModelId + " is not loaded");
                        SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("getParameter(): " + soundModelId + " is not loaded"));
                        throw new IllegalArgumentException("sound model is not loaded");
                    }
                    parameter = this.mSoundTriggerHelper.getParameter(soundModel.getUuid(), modelParam);
                }
                if (ignored != null) {
                    ignored.close();
                }
                return parameter;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public SoundTrigger.ModelParamRange queryParameter(ParcelUuid soundModelId, int modelParam) {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
                Slog.d(SoundTriggerService.TAG, "queryParameter(): id=" + soundModelId + ", param=" + modelParam);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("queryParameter(): id=" + soundModelId + ", param=" + modelParam));
                synchronized (SoundTriggerService.this.mLock) {
                    SoundTrigger.SoundModel soundModel = this.mLoadedModels.get(soundModelId.getUuid());
                    if (soundModel == null) {
                        Slog.w(SoundTriggerService.TAG, soundModelId + " is not loaded");
                        SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("queryParameter(): " + soundModelId + " is not loaded"));
                        if (ignored != null) {
                            ignored.close();
                        }
                        return null;
                    }
                    SoundTrigger.ModelParamRange queryParameter = this.mSoundTriggerHelper.queryParameter(soundModel.getUuid(), modelParam);
                    if (ignored != null) {
                        ignored.close();
                    }
                    return queryParameter;
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: clientDied */
        public void m6522xe20a666a() {
            Slog.w(SoundTriggerService.TAG, "Client died, cleaning up session.");
            SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("Client died, cleaning up session."));
            this.mSoundTriggerHelper.detach();
        }

        private void enforceCallingPermission(String permission) {
            if (PermissionUtil.checkPermissionForPreflight(SoundTriggerService.this.mContext, this.mOriginatorIdentity, permission) != 0) {
                throw new SecurityException("Identity " + this.mOriginatorIdentity + " does not have permission " + permission);
            }
        }

        private void enforceDetectionPermissions(ComponentName detectionService) {
            PackageManager packageManager = SoundTriggerService.this.mContext.getPackageManager();
            String packageName = detectionService.getPackageName();
            if (packageManager.checkPermission("android.permission.CAPTURE_AUDIO_HOTWORD", packageName) != 0) {
                throw new SecurityException(detectionService.getPackageName() + " does not have permission android.permission.CAPTURE_AUDIO_HOTWORD");
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class RemoteSoundTriggerDetectionService extends IRecognitionStatusCallback.Stub implements ServiceConnection {
            private static final int MSG_STOP_ALL_PENDING_OPERATIONS = 1;
            private final ISoundTriggerDetectionServiceClient mClient;
            private boolean mDestroyOnceRunningOpsDone;
            private boolean mIsBound;
            private boolean mIsDestroyed;
            private final NumOps mNumOps;
            private int mNumTotalOpsPerformed;
            private final Bundle mParams;
            private final ParcelUuid mPuuid;
            private final SoundTrigger.RecognitionConfig mRecognitionConfig;
            private final PowerManager.WakeLock mRemoteServiceWakeLock;
            private ISoundTriggerDetectionService mService;
            private final ComponentName mServiceName;
            private final UserHandle mUser;
            private final Object mRemoteServiceLock = new Object();
            private final ArrayList<Operation> mPendingOps = new ArrayList<>();
            private final ArraySet<Integer> mRunningOpIds = new ArraySet<>();
            private final Handler mHandler = new Handler(Looper.getMainLooper());

            public RemoteSoundTriggerDetectionService(UUID modelUuid, Bundle params, ComponentName serviceName, UserHandle user, SoundTrigger.RecognitionConfig config) {
                this.mPuuid = new ParcelUuid(modelUuid);
                this.mParams = params;
                this.mServiceName = serviceName;
                this.mUser = user;
                this.mRecognitionConfig = config;
                PowerManager pm = (PowerManager) SoundTriggerService.this.mContext.getSystemService("power");
                this.mRemoteServiceWakeLock = pm.newWakeLock(1, "RemoteSoundTriggerDetectionService " + serviceName.getPackageName() + ":" + serviceName.getClassName());
                synchronized (SoundTriggerService.this.mLock) {
                    NumOps numOps = (NumOps) SoundTriggerService.this.mNumOpsPerPackage.get(serviceName.getPackageName());
                    if (numOps == null) {
                        numOps = new NumOps();
                        SoundTriggerService.this.mNumOpsPerPackage.put(serviceName.getPackageName(), numOps);
                    }
                    this.mNumOps = numOps;
                }
                this.mClient = new ISoundTriggerDetectionServiceClient.Stub() { // from class: com.android.server.soundtrigger.SoundTriggerService.SoundTriggerSessionStub.RemoteSoundTriggerDetectionService.1
                    public void onOpFinished(int opId) {
                        long token = Binder.clearCallingIdentity();
                        try {
                            synchronized (RemoteSoundTriggerDetectionService.this.mRemoteServiceLock) {
                                RemoteSoundTriggerDetectionService.this.mRunningOpIds.remove(Integer.valueOf(opId));
                                if (RemoteSoundTriggerDetectionService.this.mRunningOpIds.isEmpty() && RemoteSoundTriggerDetectionService.this.mPendingOps.isEmpty()) {
                                    if (RemoteSoundTriggerDetectionService.this.mDestroyOnceRunningOpsDone) {
                                        RemoteSoundTriggerDetectionService.this.destroy();
                                    } else {
                                        RemoteSoundTriggerDetectionService.this.disconnectLocked();
                                    }
                                }
                            }
                        } finally {
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                };
            }

            public boolean pingBinder() {
                return (this.mIsDestroyed || this.mDestroyOnceRunningOpsDone) ? false : true;
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void disconnectLocked() {
                ISoundTriggerDetectionService iSoundTriggerDetectionService = this.mService;
                if (iSoundTriggerDetectionService != null) {
                    try {
                        iSoundTriggerDetectionService.removeClient(this.mPuuid);
                    } catch (Exception e) {
                        Slog.e(SoundTriggerService.TAG, this.mPuuid + ": Cannot remove client", e);
                        SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ": Cannot remove client"));
                    }
                    this.mService = null;
                }
                if (this.mIsBound) {
                    SoundTriggerService.this.mContext.unbindService(this);
                    this.mIsBound = false;
                    synchronized (SoundTriggerSessionStub.this.mCallbacksLock) {
                        this.mRemoteServiceWakeLock.release();
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void destroy() {
                Slog.v(SoundTriggerService.TAG, this.mPuuid + ": destroy");
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ": destroy"));
                synchronized (this.mRemoteServiceLock) {
                    disconnectLocked();
                    this.mIsDestroyed = true;
                }
                if (!this.mDestroyOnceRunningOpsDone) {
                    synchronized (SoundTriggerSessionStub.this.mCallbacksLock) {
                        SoundTriggerSessionStub.this.mCallbacks.remove(this.mPuuid.getUuid());
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void stopAllPendingOperations() {
                synchronized (this.mRemoteServiceLock) {
                    if (this.mIsDestroyed) {
                        return;
                    }
                    if (this.mService != null) {
                        int numOps = this.mRunningOpIds.size();
                        for (int i = 0; i < numOps; i++) {
                            try {
                                this.mService.onStopOperation(this.mPuuid, this.mRunningOpIds.valueAt(i).intValue());
                            } catch (Exception e) {
                                Slog.e(SoundTriggerService.TAG, this.mPuuid + ": Could not stop operation " + this.mRunningOpIds.valueAt(i), e);
                                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ": Could not stop operation " + this.mRunningOpIds.valueAt(i)));
                            }
                        }
                        this.mRunningOpIds.clear();
                    }
                    disconnectLocked();
                }
            }

            /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1151=4] */
            private void bind() {
                long token = Binder.clearCallingIdentity();
                try {
                    Intent i = new Intent();
                    i.setComponent(this.mServiceName);
                    ResolveInfo ri = SoundTriggerService.this.mContext.getPackageManager().resolveServiceAsUser(i, 268435588, this.mUser.getIdentifier());
                    if (ri == null) {
                        Slog.w(SoundTriggerService.TAG, this.mPuuid + ": " + this.mServiceName + " not found");
                        SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ": " + this.mServiceName + " not found"));
                    } else if (!"android.permission.BIND_SOUND_TRIGGER_DETECTION_SERVICE".equals(ri.serviceInfo.permission)) {
                        Slog.w(SoundTriggerService.TAG, this.mPuuid + ": " + this.mServiceName + " does not require android.permission.BIND_SOUND_TRIGGER_DETECTION_SERVICE");
                        SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ": " + this.mServiceName + " does not require android.permission.BIND_SOUND_TRIGGER_DETECTION_SERVICE"));
                    } else {
                        boolean bindServiceAsUser = SoundTriggerService.this.mContext.bindServiceAsUser(i, this, 67112961, this.mUser);
                        this.mIsBound = bindServiceAsUser;
                        if (bindServiceAsUser) {
                            this.mRemoteServiceWakeLock.acquire();
                        } else {
                            Slog.w(SoundTriggerService.TAG, this.mPuuid + ": Could not bind to " + this.mServiceName);
                            SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ": Could not bind to " + this.mServiceName));
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            private void runOrAddOperation(Operation op) {
                synchronized (this.mRemoteServiceLock) {
                    if (!this.mIsDestroyed && !this.mDestroyOnceRunningOpsDone) {
                        if (this.mService == null) {
                            this.mPendingOps.add(op);
                            if (!this.mIsBound) {
                                bind();
                            }
                        } else {
                            long currentTime = System.nanoTime();
                            this.mNumOps.clearOldOps(currentTime);
                            Settings.Global.getInt(SoundTriggerService.this.mContext.getContentResolver(), "max_sound_trigger_detection_service_ops_per_day", Integer.MAX_VALUE);
                            this.mNumOps.getOpsAdded();
                            this.mNumOps.addOp(currentTime);
                            int opId = this.mNumTotalOpsPerformed;
                            do {
                                this.mNumTotalOpsPerformed++;
                            } while (this.mRunningOpIds.contains(Integer.valueOf(opId)));
                            try {
                                Slog.v(SoundTriggerService.TAG, this.mPuuid + ": runOp " + opId);
                                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ": runOp " + opId));
                                op.run(opId, this.mService);
                                this.mRunningOpIds.add(Integer.valueOf(opId));
                            } catch (Exception e) {
                                Slog.e(SoundTriggerService.TAG, this.mPuuid + ": Could not run operation " + opId, e);
                                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ": Could not run operation " + opId));
                            }
                            if (this.mPendingOps.isEmpty() && this.mRunningOpIds.isEmpty()) {
                                if (this.mDestroyOnceRunningOpsDone) {
                                    destroy();
                                } else {
                                    disconnectLocked();
                                }
                            } else {
                                this.mHandler.removeMessages(1);
                                this.mHandler.sendMessageDelayed(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.soundtrigger.SoundTriggerService$SoundTriggerSessionStub$RemoteSoundTriggerDetectionService$$ExternalSyntheticLambda0
                                    @Override // java.util.function.Consumer
                                    public final void accept(Object obj) {
                                        ((SoundTriggerService.SoundTriggerSessionStub.RemoteSoundTriggerDetectionService) obj).stopAllPendingOperations();
                                    }
                                }, this).setWhat(1), Settings.Global.getLong(SoundTriggerService.this.mContext.getContentResolver(), "sound_trigger_detection_service_op_timeout", JobStatus.NO_LATEST_RUNTIME));
                            }
                        }
                        return;
                    }
                    Slog.w(SoundTriggerService.TAG, this.mPuuid + ": Dropped operation as already destroyed or marked for destruction");
                    SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ":Dropped operation as already destroyed or marked for destruction"));
                    op.drop();
                }
            }

            public void onKeyphraseDetected(SoundTrigger.KeyphraseRecognitionEvent event) {
                Slog.w(SoundTriggerService.TAG, this.mPuuid + "->" + this.mServiceName + ": IGNORED onKeyphraseDetected(" + event + ")");
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + "->" + this.mServiceName + ": IGNORED onKeyphraseDetected(" + event + ")"));
            }

            private AudioRecord createAudioRecordForEvent(SoundTrigger.GenericRecognitionEvent event) throws IllegalArgumentException, UnsupportedOperationException {
                AudioAttributes.Builder attributesBuilder = new AudioAttributes.Builder();
                attributesBuilder.setInternalCapturePreset(1999);
                AudioAttributes attributes = attributesBuilder.build();
                AudioFormat originalFormat = event.getCaptureFormat();
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("createAudioRecordForEvent"));
                return new AudioRecord.Builder().setAudioAttributes(attributes).setAudioFormat(new AudioFormat.Builder().setChannelMask(originalFormat.getChannelMask()).setEncoding(originalFormat.getEncoding()).setSampleRate(originalFormat.getSampleRate()).build()).setSessionId(event.getCaptureSession()).build();
            }

            public void onGenericSoundTriggerDetected(final SoundTrigger.GenericRecognitionEvent event) {
                Slog.v(SoundTriggerService.TAG, this.mPuuid + ": Generic sound trigger event: " + event);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ": Generic sound trigger event: " + event));
                runOrAddOperation(new Operation(new Runnable() { // from class: com.android.server.soundtrigger.SoundTriggerService$SoundTriggerSessionStub$RemoteSoundTriggerDetectionService$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        SoundTriggerService.SoundTriggerSessionStub.RemoteSoundTriggerDetectionService.this.m6531x3613f796();
                    }
                }, new Operation.ExecuteOp() { // from class: com.android.server.soundtrigger.SoundTriggerService$SoundTriggerSessionStub$RemoteSoundTriggerDetectionService$$ExternalSyntheticLambda2
                    @Override // com.android.server.soundtrigger.SoundTriggerService.Operation.ExecuteOp
                    public final void run(int i, ISoundTriggerDetectionService iSoundTriggerDetectionService) {
                        SoundTriggerService.SoundTriggerSessionStub.RemoteSoundTriggerDetectionService.this.m6532x44202175(event, i, iSoundTriggerDetectionService);
                    }
                }, new Runnable() { // from class: com.android.server.soundtrigger.SoundTriggerService$SoundTriggerSessionStub$RemoteSoundTriggerDetectionService$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        SoundTriggerService.SoundTriggerSessionStub.RemoteSoundTriggerDetectionService.this.m6533x522c4b54(event);
                    }
                }));
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$onGenericSoundTriggerDetected$0$com-android-server-soundtrigger-SoundTriggerService$SoundTriggerSessionStub$RemoteSoundTriggerDetectionService  reason: not valid java name */
            public /* synthetic */ void m6531x3613f796() {
                if (!this.mRecognitionConfig.allowMultipleTriggers) {
                    synchronized (SoundTriggerSessionStub.this.mCallbacksLock) {
                        SoundTriggerSessionStub.this.mCallbacks.remove(this.mPuuid.getUuid());
                    }
                    this.mDestroyOnceRunningOpsDone = true;
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$onGenericSoundTriggerDetected$1$com-android-server-soundtrigger-SoundTriggerService$SoundTriggerSessionStub$RemoteSoundTriggerDetectionService  reason: not valid java name */
            public /* synthetic */ void m6532x44202175(SoundTrigger.GenericRecognitionEvent event, int opId, ISoundTriggerDetectionService service) throws RemoteException {
                service.onGenericRecognitionEvent(this.mPuuid, opId, event);
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$onGenericSoundTriggerDetected$2$com-android-server-soundtrigger-SoundTriggerService$SoundTriggerSessionStub$RemoteSoundTriggerDetectionService  reason: not valid java name */
            public /* synthetic */ void m6533x522c4b54(SoundTrigger.GenericRecognitionEvent event) {
                if (event.isCaptureAvailable()) {
                    try {
                        AudioRecord capturedData = createAudioRecordForEvent(event);
                        capturedData.startRecording();
                        capturedData.release();
                    } catch (IllegalArgumentException | UnsupportedOperationException e) {
                        Slog.w(SoundTriggerService.TAG, this.mPuuid + ": createAudioRecordForEvent(" + event + "), failed to create AudioRecord");
                    }
                }
            }

            public void onError(final int status) {
                Slog.v(SoundTriggerService.TAG, this.mPuuid + ": onError: " + status);
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ": onError: " + status));
                runOrAddOperation(new Operation(new Runnable() { // from class: com.android.server.soundtrigger.SoundTriggerService$SoundTriggerSessionStub$RemoteSoundTriggerDetectionService$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        SoundTriggerService.SoundTriggerSessionStub.RemoteSoundTriggerDetectionService.this.m6529xb32bf8ad();
                    }
                }, new Operation.ExecuteOp() { // from class: com.android.server.soundtrigger.SoundTriggerService$SoundTriggerSessionStub$RemoteSoundTriggerDetectionService$$ExternalSyntheticLambda5
                    @Override // com.android.server.soundtrigger.SoundTriggerService.Operation.ExecuteOp
                    public final void run(int i, ISoundTriggerDetectionService iSoundTriggerDetectionService) {
                        SoundTriggerService.SoundTriggerSessionStub.RemoteSoundTriggerDetectionService.this.m6530xc138228c(status, i, iSoundTriggerDetectionService);
                    }
                }, null));
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$onError$3$com-android-server-soundtrigger-SoundTriggerService$SoundTriggerSessionStub$RemoteSoundTriggerDetectionService  reason: not valid java name */
            public /* synthetic */ void m6529xb32bf8ad() {
                synchronized (SoundTriggerSessionStub.this.mCallbacksLock) {
                    SoundTriggerSessionStub.this.mCallbacks.remove(this.mPuuid.getUuid());
                }
                this.mDestroyOnceRunningOpsDone = true;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$onError$4$com-android-server-soundtrigger-SoundTriggerService$SoundTriggerSessionStub$RemoteSoundTriggerDetectionService  reason: not valid java name */
            public /* synthetic */ void m6530xc138228c(int status, int opId, ISoundTriggerDetectionService service) throws RemoteException {
                service.onError(this.mPuuid, opId, status);
            }

            public void onRecognitionPaused() {
                Slog.i(SoundTriggerService.TAG, this.mPuuid + "->" + this.mServiceName + ": IGNORED onRecognitionPaused");
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + "->" + this.mServiceName + ": IGNORED onRecognitionPaused"));
            }

            public void onRecognitionResumed() {
                Slog.i(SoundTriggerService.TAG, this.mPuuid + "->" + this.mServiceName + ": IGNORED onRecognitionResumed");
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + "->" + this.mServiceName + ": IGNORED onRecognitionResumed"));
            }

            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                Slog.v(SoundTriggerService.TAG, this.mPuuid + ": onServiceConnected(" + service + ")");
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ": onServiceConnected(" + service + ")"));
                synchronized (this.mRemoteServiceLock) {
                    ISoundTriggerDetectionService asInterface = ISoundTriggerDetectionService.Stub.asInterface(service);
                    this.mService = asInterface;
                    try {
                        asInterface.setClient(this.mPuuid, this.mParams, this.mClient);
                        while (!this.mPendingOps.isEmpty()) {
                            runOrAddOperation(this.mPendingOps.remove(0));
                        }
                    } catch (Exception e) {
                        Slog.e(SoundTriggerService.TAG, this.mPuuid + ": Could not init " + this.mServiceName, e);
                    }
                }
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                Slog.v(SoundTriggerService.TAG, this.mPuuid + ": onServiceDisconnected");
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ": onServiceDisconnected"));
                synchronized (this.mRemoteServiceLock) {
                    this.mService = null;
                }
            }

            @Override // android.content.ServiceConnection
            public void onBindingDied(ComponentName name) {
                Slog.v(SoundTriggerService.TAG, this.mPuuid + ": onBindingDied");
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(this.mPuuid + ": onBindingDied"));
                synchronized (this.mRemoteServiceLock) {
                    destroy();
                }
            }

            @Override // android.content.ServiceConnection
            public void onNullBinding(ComponentName name) {
                Slog.w(SoundTriggerService.TAG, name + " for model " + this.mPuuid + " returned a null binding");
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent(name + " for model " + this.mPuuid + " returned a null binding"));
                synchronized (this.mRemoteServiceLock) {
                    disconnectLocked();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class NumOps {
        private long mLastOpsHourSinceBoot;
        private final Object mLock;
        private int[] mNumOps;

        private NumOps() {
            this.mLock = new Object();
            this.mNumOps = new int[24];
        }

        void clearOldOps(long currentTime) {
            synchronized (this.mLock) {
                long numHoursSinceBoot = TimeUnit.HOURS.convert(currentTime, TimeUnit.NANOSECONDS);
                long j = this.mLastOpsHourSinceBoot;
                if (j != 0) {
                    for (long hour = j + 1; hour <= numHoursSinceBoot; hour++) {
                        this.mNumOps[(int) (hour % 24)] = 0;
                    }
                }
            }
        }

        void addOp(long currentTime) {
            synchronized (this.mLock) {
                long numHoursSinceBoot = TimeUnit.HOURS.convert(currentTime, TimeUnit.NANOSECONDS);
                int[] iArr = this.mNumOps;
                int i = (int) (numHoursSinceBoot % 24);
                iArr[i] = iArr[i] + 1;
                this.mLastOpsHourSinceBoot = numHoursSinceBoot;
            }
        }

        int getOpsAdded() {
            int totalOperationsInLastDay;
            synchronized (this.mLock) {
                totalOperationsInLastDay = 0;
                for (int i = 0; i < 24; i++) {
                    totalOperationsInLastDay += this.mNumOps[i];
                }
            }
            return totalOperationsInLastDay;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class Operation {
        private final Runnable mDropOp;
        private final ExecuteOp mExecuteOp;
        private final Runnable mSetupOp;

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public interface ExecuteOp {
            void run(int i, ISoundTriggerDetectionService iSoundTriggerDetectionService) throws RemoteException;
        }

        private Operation(Runnable setupOp, ExecuteOp executeOp, Runnable cancelOp) {
            this.mSetupOp = setupOp;
            this.mExecuteOp = executeOp;
            this.mDropOp = cancelOp;
        }

        private void setup() {
            Runnable runnable = this.mSetupOp;
            if (runnable != null) {
                runnable.run();
            }
        }

        void run(int opId, ISoundTriggerDetectionService service) throws RemoteException {
            setup();
            this.mExecuteOp.run(opId, service);
        }

        void drop() {
            setup();
            Runnable runnable = this.mDropOp;
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    /* loaded from: classes2.dex */
    public final class LocalSoundTriggerService implements SoundTriggerInternal {
        private final Context mContext;

        LocalSoundTriggerService(Context context) {
            this.mContext = context;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class SessionImpl implements SoundTriggerInternal.Session {
            private final IBinder mClient;
            private final SoundTriggerHelper mSoundTriggerHelper;

            private SessionImpl(SoundTriggerHelper soundTriggerHelper, IBinder client) {
                this.mSoundTriggerHelper = soundTriggerHelper;
                this.mClient = client;
                try {
                    client.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.soundtrigger.SoundTriggerService$LocalSoundTriggerService$SessionImpl$$ExternalSyntheticLambda0
                        @Override // android.os.IBinder.DeathRecipient
                        public final void binderDied() {
                            SoundTriggerService.LocalSoundTriggerService.SessionImpl.this.m6519xe7f291e4();
                        }
                    }, 0);
                } catch (RemoteException e) {
                    Slog.e(SoundTriggerService.TAG, "Failed to register death listener.", e);
                }
            }

            @Override // com.android.server.soundtrigger.SoundTriggerInternal.Session
            public int startRecognition(int keyphraseId, SoundTrigger.KeyphraseSoundModel soundModel, IRecognitionStatusCallback listener, SoundTrigger.RecognitionConfig recognitionConfig, boolean runInBatterySaverMode) {
                return this.mSoundTriggerHelper.startKeyphraseRecognition(keyphraseId, soundModel, listener, recognitionConfig, runInBatterySaverMode);
            }

            @Override // com.android.server.soundtrigger.SoundTriggerInternal.Session
            public synchronized int stopRecognition(int keyphraseId, IRecognitionStatusCallback listener) {
                return this.mSoundTriggerHelper.stopKeyphraseRecognition(keyphraseId, listener);
            }

            @Override // com.android.server.soundtrigger.SoundTriggerInternal.Session
            public SoundTrigger.ModuleProperties getModuleProperties() {
                return this.mSoundTriggerHelper.getModuleProperties();
            }

            @Override // com.android.server.soundtrigger.SoundTriggerInternal.Session
            public int setParameter(int keyphraseId, int modelParam, int value) {
                return this.mSoundTriggerHelper.setKeyphraseParameter(keyphraseId, modelParam, value);
            }

            @Override // com.android.server.soundtrigger.SoundTriggerInternal.Session
            public int getParameter(int keyphraseId, int modelParam) {
                return this.mSoundTriggerHelper.getKeyphraseParameter(keyphraseId, modelParam);
            }

            @Override // com.android.server.soundtrigger.SoundTriggerInternal.Session
            public SoundTrigger.ModelParamRange queryParameter(int keyphraseId, int modelParam) {
                return this.mSoundTriggerHelper.queryKeyphraseParameter(keyphraseId, modelParam);
            }

            @Override // com.android.server.soundtrigger.SoundTriggerInternal.Session
            public int unloadKeyphraseModel(int keyphraseId) {
                return this.mSoundTriggerHelper.unloadKeyphraseSoundModel(keyphraseId);
            }

            @Override // com.android.server.soundtrigger.SoundTriggerInternal.Session
            public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                this.mSoundTriggerHelper.dump(fd, pw, args);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX INFO: Access modifiers changed from: private */
            /* renamed from: clientDied */
            public void m6519xe7f291e4() {
                Slog.w(SoundTriggerService.TAG, "Client died, cleaning up session.");
                SoundTriggerService.sEventLogger.log(new SoundTriggerLogger.StringEvent("Client died, cleaning up session."));
                this.mSoundTriggerHelper.detach();
            }
        }

        @Override // com.android.server.soundtrigger.SoundTriggerInternal
        public SoundTriggerInternal.Session attach(IBinder client) {
            return new SessionImpl(SoundTriggerService.this.newSoundTriggerHelper(), client);
        }

        @Override // com.android.server.soundtrigger.SoundTriggerInternal
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            SoundTriggerService.sEventLogger.dump(pw);
            SoundTriggerService.this.mDbHelper.dump(pw);
            SoundTriggerService.this.mSoundModelStatTracker.dump(pw);
        }
    }
}
