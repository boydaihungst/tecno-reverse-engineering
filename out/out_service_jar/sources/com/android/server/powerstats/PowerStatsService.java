package com.android.server.powerstats;

import android.content.Context;
import android.hardware.power.stats.Channel;
import android.hardware.power.stats.EnergyConsumer;
import android.hardware.power.stats.EnergyConsumerResult;
import android.hardware.power.stats.EnergyMeasurement;
import android.hardware.power.stats.PowerEntity;
import android.hardware.power.stats.StateResidencyResult;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.power.PowerStatsInternal;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.SystemService;
import com.android.server.powerstats.PowerStatsHALWrapper;
import com.android.server.powerstats.ProtoStreamUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
/* loaded from: classes2.dex */
public class PowerStatsService extends SystemService {
    private static final String DATA_STORAGE_SUBDIR = "powerstats";
    private static final int DATA_STORAGE_VERSION = 0;
    private static final boolean DEBUG = false;
    private static final String METER_CACHE_FILENAME = "meterCache";
    private static final String METER_FILENAME = "log.powerstats.meter.0";
    private static final String MODEL_CACHE_FILENAME = "modelCache";
    private static final String MODEL_FILENAME = "log.powerstats.model.0";
    private static final String RESIDENCY_CACHE_FILENAME = "residencyCache";
    private static final String RESIDENCY_FILENAME = "log.powerstats.residency.0";
    private static final String TAG = PowerStatsService.class.getSimpleName();
    private BatteryTrigger mBatteryTrigger;
    private Context mContext;
    private File mDataStoragePath;
    private final Injector mInjector;
    private Looper mLooper;
    private PowerStatsInternal mPowerStatsInternal;
    private PowerStatsLogger mPowerStatsLogger;
    private StatsPullAtomCallbackImpl mPullAtomCallback;
    private TimerTrigger mTimerTrigger;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Injector {
        private PowerStatsHALWrapper.IPowerStatsHALWrapper mPowerStatsHALWrapper;

        Injector() {
        }

        File createDataStoragePath() {
            return new File(Environment.getDataSystemDeDirectory(0), PowerStatsService.DATA_STORAGE_SUBDIR);
        }

        String createMeterFilename() {
            return PowerStatsService.METER_FILENAME;
        }

        String createModelFilename() {
            return PowerStatsService.MODEL_FILENAME;
        }

        String createResidencyFilename() {
            return PowerStatsService.RESIDENCY_FILENAME;
        }

        String createMeterCacheFilename() {
            return PowerStatsService.METER_CACHE_FILENAME;
        }

        String createModelCacheFilename() {
            return PowerStatsService.MODEL_CACHE_FILENAME;
        }

        String createResidencyCacheFilename() {
            return PowerStatsService.RESIDENCY_CACHE_FILENAME;
        }

        PowerStatsHALWrapper.IPowerStatsHALWrapper createPowerStatsHALWrapperImpl() {
            return PowerStatsHALWrapper.getPowerStatsHalImpl();
        }

        PowerStatsHALWrapper.IPowerStatsHALWrapper getPowerStatsHALWrapperImpl() {
            PowerStatsHALWrapper.IPowerStatsHALWrapper iPowerStatsHALWrapper;
            synchronized (this) {
                if (this.mPowerStatsHALWrapper == null) {
                    this.mPowerStatsHALWrapper = PowerStatsHALWrapper.getPowerStatsHalImpl();
                }
                iPowerStatsHALWrapper = this.mPowerStatsHALWrapper;
            }
            return iPowerStatsHALWrapper;
        }

        PowerStatsLogger createPowerStatsLogger(Context context, Looper looper, File dataStoragePath, String meterFilename, String meterCacheFilename, String modelFilename, String modelCacheFilename, String residencyFilename, String residencyCacheFilename, PowerStatsHALWrapper.IPowerStatsHALWrapper powerStatsHALWrapper) {
            return new PowerStatsLogger(context, looper, dataStoragePath, meterFilename, meterCacheFilename, modelFilename, modelCacheFilename, residencyFilename, residencyCacheFilename, powerStatsHALWrapper);
        }

        BatteryTrigger createBatteryTrigger(Context context, PowerStatsLogger powerStatsLogger) {
            return new BatteryTrigger(context, powerStatsLogger, true);
        }

        TimerTrigger createTimerTrigger(Context context, PowerStatsLogger powerStatsLogger) {
            return new TimerTrigger(context, powerStatsLogger, true);
        }

        StatsPullAtomCallbackImpl createStatsPullerImpl(Context context, PowerStatsInternal powerStatsInternal) {
            return new StatsPullAtomCallbackImpl(context, powerStatsInternal);
        }
    }

    /* loaded from: classes2.dex */
    private final class BinderService extends Binder {
        private BinderService() {
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(PowerStatsService.this.mContext, PowerStatsService.TAG, pw)) {
                if (PowerStatsService.this.mPowerStatsLogger == null) {
                    Slog.e(PowerStatsService.TAG, "PowerStats HAL is not initialized.  No data available.");
                } else if (args.length > 0 && "--proto".equals(args[0])) {
                    if ("model".equals(args[1])) {
                        PowerStatsService.this.mPowerStatsLogger.writeModelDataToFile(fd);
                    } else if ("meter".equals(args[1])) {
                        PowerStatsService.this.mPowerStatsLogger.writeMeterDataToFile(fd);
                    } else if ("residency".equals(args[1])) {
                        PowerStatsService.this.mPowerStatsLogger.writeResidencyDataToFile(fd);
                    }
                } else if (args.length == 0) {
                    pw.println("PowerStatsService dumpsys: available PowerEntities");
                    PowerEntity[] powerEntity = PowerStatsService.this.getPowerStatsHal().getPowerEntityInfo();
                    ProtoStreamUtils.PowerEntityUtils.dumpsys(powerEntity, pw);
                    pw.println("PowerStatsService dumpsys: available Channels");
                    Channel[] channel = PowerStatsService.this.getPowerStatsHal().getEnergyMeterInfo();
                    ProtoStreamUtils.ChannelUtils.dumpsys(channel, pw);
                    pw.println("PowerStatsService dumpsys: available EnergyConsumers");
                    EnergyConsumer[] energyConsumer = PowerStatsService.this.getPowerStatsHal().getEnergyConsumerInfo();
                    ProtoStreamUtils.EnergyConsumerUtils.dumpsys(energyConsumer, pw);
                }
            }
        }
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            onSystemServicesReady();
        } else if (phase == 1000) {
            onBootCompleted();
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        if (getPowerStatsHal().isInitialized()) {
            LocalService localService = new LocalService();
            this.mPowerStatsInternal = localService;
            publishLocalService(PowerStatsInternal.class, localService);
        }
        publishBinderService(DATA_STORAGE_SUBDIR, new BinderService());
    }

    private void onSystemServicesReady() {
        this.mPullAtomCallback = this.mInjector.createStatsPullerImpl(this.mContext, this.mPowerStatsInternal);
    }

    public boolean getDeleteMeterDataOnBoot() {
        return this.mPowerStatsLogger.getDeleteMeterDataOnBoot();
    }

    public boolean getDeleteModelDataOnBoot() {
        return this.mPowerStatsLogger.getDeleteModelDataOnBoot();
    }

    public boolean getDeleteResidencyDataOnBoot() {
        return this.mPowerStatsLogger.getDeleteResidencyDataOnBoot();
    }

    private void onBootCompleted() {
        if (getPowerStatsHal().isInitialized()) {
            this.mDataStoragePath = this.mInjector.createDataStoragePath();
            PowerStatsLogger createPowerStatsLogger = this.mInjector.createPowerStatsLogger(this.mContext, getLooper(), this.mDataStoragePath, this.mInjector.createMeterFilename(), this.mInjector.createMeterCacheFilename(), this.mInjector.createModelFilename(), this.mInjector.createModelCacheFilename(), this.mInjector.createResidencyFilename(), this.mInjector.createResidencyCacheFilename(), getPowerStatsHal());
            this.mPowerStatsLogger = createPowerStatsLogger;
            this.mBatteryTrigger = this.mInjector.createBatteryTrigger(this.mContext, createPowerStatsLogger);
            this.mTimerTrigger = this.mInjector.createTimerTrigger(this.mContext, this.mPowerStatsLogger);
            return;
        }
        Slog.e(TAG, "Failed to start PowerStatsService loggers");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public PowerStatsHALWrapper.IPowerStatsHALWrapper getPowerStatsHal() {
        return this.mInjector.getPowerStatsHALWrapperImpl();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Looper getLooper() {
        synchronized (this) {
            Looper looper = this.mLooper;
            if (looper == null) {
                HandlerThread thread = new HandlerThread(TAG);
                thread.start();
                return thread.getLooper();
            }
            return looper;
        }
    }

    public PowerStatsService(Context context) {
        this(context, new Injector());
    }

    public PowerStatsService(Context context, Injector injector) {
        super(context);
        this.mContext = context;
        this.mInjector = injector;
    }

    /* loaded from: classes2.dex */
    private final class LocalService extends PowerStatsInternal {
        private final Handler mHandler;

        LocalService() {
            this.mHandler = new Handler(PowerStatsService.this.getLooper());
        }

        @Override // android.power.PowerStatsInternal
        public EnergyConsumer[] getEnergyConsumerInfo() {
            return PowerStatsService.this.getPowerStatsHal().getEnergyConsumerInfo();
        }

        @Override // android.power.PowerStatsInternal
        public CompletableFuture<EnergyConsumerResult[]> getEnergyConsumedAsync(int[] energyConsumerIds) {
            CompletableFuture<EnergyConsumerResult[]> future = new CompletableFuture<>();
            Handler handler = this.mHandler;
            final PowerStatsService powerStatsService = PowerStatsService.this;
            handler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.powerstats.PowerStatsService$LocalService$$ExternalSyntheticLambda0
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    PowerStatsService.this.getEnergyConsumedAsync((CompletableFuture) obj, (int[]) obj2);
                }
            }, future, energyConsumerIds));
            return future;
        }

        @Override // android.power.PowerStatsInternal
        public PowerEntity[] getPowerEntityInfo() {
            return PowerStatsService.this.getPowerStatsHal().getPowerEntityInfo();
        }

        @Override // android.power.PowerStatsInternal
        public CompletableFuture<StateResidencyResult[]> getStateResidencyAsync(int[] powerEntityIds) {
            CompletableFuture<StateResidencyResult[]> future = new CompletableFuture<>();
            Handler handler = this.mHandler;
            final PowerStatsService powerStatsService = PowerStatsService.this;
            handler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.powerstats.PowerStatsService$LocalService$$ExternalSyntheticLambda1
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    PowerStatsService.this.getStateResidencyAsync((CompletableFuture) obj, (int[]) obj2);
                }
            }, future, powerEntityIds));
            return future;
        }

        @Override // android.power.PowerStatsInternal
        public Channel[] getEnergyMeterInfo() {
            return PowerStatsService.this.getPowerStatsHal().getEnergyMeterInfo();
        }

        @Override // android.power.PowerStatsInternal
        public CompletableFuture<EnergyMeasurement[]> readEnergyMeterAsync(int[] channelIds) {
            CompletableFuture<EnergyMeasurement[]> future = new CompletableFuture<>();
            Handler handler = this.mHandler;
            final PowerStatsService powerStatsService = PowerStatsService.this;
            handler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.powerstats.PowerStatsService$LocalService$$ExternalSyntheticLambda2
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    PowerStatsService.this.readEnergyMeterAsync((CompletableFuture) obj, (int[]) obj2);
                }
            }, future, channelIds));
            return future;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getEnergyConsumedAsync(CompletableFuture<EnergyConsumerResult[]> future, int[] energyConsumerIds) {
        future.complete(getPowerStatsHal().getEnergyConsumed(energyConsumerIds));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getStateResidencyAsync(CompletableFuture<StateResidencyResult[]> future, int[] powerEntityIds) {
        future.complete(getPowerStatsHal().getStateResidency(powerEntityIds));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readEnergyMeterAsync(CompletableFuture<EnergyMeasurement[]> future, int[] channelIds) {
        future.complete(getPowerStatsHal().readEnergyMeter(channelIds));
    }
}
