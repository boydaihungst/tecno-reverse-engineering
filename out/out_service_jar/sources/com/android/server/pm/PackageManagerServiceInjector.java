package com.android.server.pm;

import android.app.ActivityManagerInternal;
import android.app.backup.IBackupManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.incremental.IncrementalManager;
import android.util.DisplayMetrics;
import com.android.server.SystemConfig;
import com.android.server.compat.PlatformCompat;
import com.android.server.pm.dex.ArtManagerService;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.ViewCompiler;
import com.android.server.pm.parsing.PackageParser2;
import com.android.server.pm.permission.LegacyPermissionManagerInternal;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.pm.resolution.ComponentResolver;
import com.android.server.pm.verify.domain.DomainVerificationManagerInternal;
import java.util.List;
import java.util.concurrent.Executor;
/* loaded from: classes2.dex */
public class PackageManagerServiceInjector {
    private final PackageAbiHelper mAbiHelper;
    private final Singleton<ApexManager> mApexManagerProducer;
    private final Singleton<AppsFilterImpl> mAppsFilterProducer;
    private final Singleton<ArtManagerService> mArtManagerServiceProducer;
    private final Singleton<BackgroundDexOptService> mBackgroundDexOptService;
    private final Executor mBackgroundExecutor;
    private final Handler mBackgroundHandler;
    private final Singleton<ComponentResolver> mComponentResolverProducer;
    private final Context mContext;
    private final Singleton<DefaultAppProvider> mDefaultAppProviderProducer;
    private final Singleton<DexManager> mDexManagerProducer;
    private final Singleton<DisplayMetrics> mDisplayMetricsProducer;
    private final Singleton<DomainVerificationManagerInternal> mDomainVerificationManagerInternalProducer;
    private final ServiceProducer mGetLocalServiceProducer;
    private final ServiceProducer mGetSystemServiceProducer;
    private final Singleton<Handler> mHandlerProducer;
    private final Singleton<IBackupManager> mIBackupManager;
    private final Singleton<IncrementalManager> mIncrementalManagerProducer;
    private final Object mInstallLock;
    private final Installer mInstaller;
    private final ProducerWithArgument<InstantAppResolverConnection, ComponentName> mInstantAppResolverConnectionProducer;
    private final Singleton<LegacyPermissionManagerInternal> mLegacyPermissionManagerInternalProducer;
    private final PackageManagerTracedLock mLock;
    private final Singleton<ModuleInfoProvider> mModuleInfoProviderProducer;
    private final Singleton<PackageDexOptimizer> mPackageDexOptimizerProducer;
    private final Singleton<PackageInstallerService> mPackageInstallerServiceProducer;
    private PackageManagerService mPackageManager;
    private final Singleton<PermissionManagerServiceInternal> mPermissionManagerServiceProducer;
    private final Singleton<PlatformCompat> mPlatformCompatProducer;
    private final Producer<PackageParser2> mPreparingPackageParserProducer;
    private final Producer<PackageParser2> mScanningCachingPackageParserProducer;
    private final Producer<PackageParser2> mScanningPackageParserProducer;
    private final Singleton<Settings> mSettingsProducer;
    private final Singleton<SharedLibrariesImpl> mSharedLibrariesProducer;
    private final Singleton<SystemConfig> mSystemConfigProducer;
    private final List<ScanPartition> mSystemPartitions;
    private final SystemWrapper mSystemWrapper;
    private final Singleton<UserManagerService> mUserManagerProducer;
    private final Singleton<ViewCompiler> mViewCompilerProducer;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface Producer<T> {
        T produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface ProducerWithArgument<T, R> {
        T produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService, R r);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface ServiceProducer {
        <T> T produce(Class<T> cls);
    }

    /* loaded from: classes2.dex */
    public interface SystemWrapper {
        void disablePackageCaches();

        void enablePackageCaches();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Singleton<T> {
        private volatile T mInstance = null;
        private final Producer<T> mProducer;

        Singleton(Producer<T> producer) {
            this.mProducer = producer;
        }

        T get(PackageManagerServiceInjector injector, PackageManagerService packageManagerService) {
            if (this.mInstance == null) {
                this.mInstance = this.mProducer.produce(injector, packageManagerService);
            }
            return this.mInstance;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageManagerServiceInjector(Context context, PackageManagerTracedLock lock, Installer installer, Object installLock, PackageAbiHelper abiHelper, Handler backgroundHandler, List<ScanPartition> systemPartitions, Producer<ComponentResolver> componentResolverProducer, Producer<PermissionManagerServiceInternal> permissionManagerServiceProducer, Producer<UserManagerService> userManagerProducer, Producer<Settings> settingsProducer, Producer<AppsFilterImpl> appsFilterProducer, Producer<PlatformCompat> platformCompatProducer, Producer<SystemConfig> systemConfigProducer, Producer<PackageDexOptimizer> packageDexOptimizerProducer, Producer<DexManager> dexManagerProducer, Producer<ArtManagerService> artManagerServiceProducer, Producer<ApexManager> apexManagerProducer, Producer<ViewCompiler> viewCompilerProducer, Producer<IncrementalManager> incrementalManagerProducer, Producer<DefaultAppProvider> defaultAppProviderProducer, Producer<DisplayMetrics> displayMetricsProducer, Producer<PackageParser2> scanningCachingPackageParserProducer, Producer<PackageParser2> scanningPackageParserProducer, Producer<PackageParser2> preparingPackageParserProducer, Producer<PackageInstallerService> packageInstallerServiceProducer, ProducerWithArgument<InstantAppResolverConnection, ComponentName> instantAppResolverConnectionProducer, Producer<ModuleInfoProvider> moduleInfoProviderProducer, Producer<LegacyPermissionManagerInternal> legacyPermissionManagerInternalProducer, Producer<DomainVerificationManagerInternal> domainVerificationManagerInternalProducer, Producer<Handler> handlerProducer, SystemWrapper systemWrapper, ServiceProducer getLocalServiceProducer, ServiceProducer getSystemServiceProducer, Producer<BackgroundDexOptService> backgroundDexOptService, Producer<IBackupManager> iBackupManager, Producer<SharedLibrariesImpl> sharedLibrariesProducer) {
        this.mContext = context;
        this.mLock = lock;
        this.mInstaller = installer;
        this.mAbiHelper = abiHelper;
        this.mInstallLock = installLock;
        this.mBackgroundHandler = backgroundHandler;
        this.mBackgroundExecutor = new HandlerExecutor(backgroundHandler);
        this.mSystemPartitions = systemPartitions;
        this.mComponentResolverProducer = new Singleton<>(componentResolverProducer);
        this.mPermissionManagerServiceProducer = new Singleton<>(permissionManagerServiceProducer);
        this.mUserManagerProducer = new Singleton<>(userManagerProducer);
        this.mSettingsProducer = new Singleton<>(settingsProducer);
        this.mAppsFilterProducer = new Singleton<>(appsFilterProducer);
        this.mPlatformCompatProducer = new Singleton<>(platformCompatProducer);
        this.mSystemConfigProducer = new Singleton<>(systemConfigProducer);
        this.mPackageDexOptimizerProducer = new Singleton<>(packageDexOptimizerProducer);
        this.mDexManagerProducer = new Singleton<>(dexManagerProducer);
        this.mArtManagerServiceProducer = new Singleton<>(artManagerServiceProducer);
        this.mApexManagerProducer = new Singleton<>(apexManagerProducer);
        this.mViewCompilerProducer = new Singleton<>(viewCompilerProducer);
        this.mIncrementalManagerProducer = new Singleton<>(incrementalManagerProducer);
        this.mDefaultAppProviderProducer = new Singleton<>(defaultAppProviderProducer);
        this.mDisplayMetricsProducer = new Singleton<>(displayMetricsProducer);
        this.mScanningCachingPackageParserProducer = scanningCachingPackageParserProducer;
        this.mScanningPackageParserProducer = scanningPackageParserProducer;
        this.mPreparingPackageParserProducer = preparingPackageParserProducer;
        this.mPackageInstallerServiceProducer = new Singleton<>(packageInstallerServiceProducer);
        this.mInstantAppResolverConnectionProducer = instantAppResolverConnectionProducer;
        this.mModuleInfoProviderProducer = new Singleton<>(moduleInfoProviderProducer);
        this.mLegacyPermissionManagerInternalProducer = new Singleton<>(legacyPermissionManagerInternalProducer);
        this.mSystemWrapper = systemWrapper;
        this.mGetLocalServiceProducer = getLocalServiceProducer;
        this.mGetSystemServiceProducer = getSystemServiceProducer;
        this.mDomainVerificationManagerInternalProducer = new Singleton<>(domainVerificationManagerInternalProducer);
        this.mHandlerProducer = new Singleton<>(handlerProducer);
        this.mBackgroundDexOptService = new Singleton<>(backgroundDexOptService);
        this.mIBackupManager = new Singleton<>(iBackupManager);
        this.mSharedLibrariesProducer = new Singleton<>(sharedLibrariesProducer);
    }

    public void bootstrap(PackageManagerService pm) {
        this.mPackageManager = pm;
    }

    public UserManagerInternal getUserManagerInternal() {
        return getUserManagerService().getInternalForInjectorOnly();
    }

    public PackageAbiHelper getAbiHelper() {
        return this.mAbiHelper;
    }

    public Object getInstallLock() {
        return this.mInstallLock;
    }

    public List<ScanPartition> getSystemPartitions() {
        return this.mSystemPartitions;
    }

    public UserManagerService getUserManagerService() {
        return this.mUserManagerProducer.get(this, this.mPackageManager);
    }

    public PackageManagerTracedLock getLock() {
        return this.mLock;
    }

    public Installer getInstaller() {
        return this.mInstaller;
    }

    public ComponentResolver getComponentResolver() {
        return this.mComponentResolverProducer.get(this, this.mPackageManager);
    }

    public PermissionManagerServiceInternal getPermissionManagerServiceInternal() {
        return this.mPermissionManagerServiceProducer.get(this, this.mPackageManager);
    }

    public Context getContext() {
        return this.mContext;
    }

    public Settings getSettings() {
        return this.mSettingsProducer.get(this, this.mPackageManager);
    }

    public AppsFilterImpl getAppsFilter() {
        return this.mAppsFilterProducer.get(this, this.mPackageManager);
    }

    public PlatformCompat getCompatibility() {
        return this.mPlatformCompatProducer.get(this, this.mPackageManager);
    }

    public SystemConfig getSystemConfig() {
        return this.mSystemConfigProducer.get(this, this.mPackageManager);
    }

    public PackageDexOptimizer getPackageDexOptimizer() {
        return this.mPackageDexOptimizerProducer.get(this, this.mPackageManager);
    }

    public DexManager getDexManager() {
        return this.mDexManagerProducer.get(this, this.mPackageManager);
    }

    public ArtManagerService getArtManagerService() {
        return this.mArtManagerServiceProducer.get(this, this.mPackageManager);
    }

    public ApexManager getApexManager() {
        return this.mApexManagerProducer.get(this, this.mPackageManager);
    }

    public ViewCompiler getViewCompiler() {
        return this.mViewCompilerProducer.get(this, this.mPackageManager);
    }

    public Handler getBackgroundHandler() {
        return this.mBackgroundHandler;
    }

    public Executor getBackgroundExecutor() {
        return this.mBackgroundExecutor;
    }

    public DisplayMetrics getDisplayMetrics() {
        return this.mDisplayMetricsProducer.get(this, this.mPackageManager);
    }

    public <T> T getLocalService(Class<T> c) {
        return (T) this.mGetLocalServiceProducer.produce(c);
    }

    public <T> T getSystemService(Class<T> c) {
        return (T) this.mGetSystemServiceProducer.produce(c);
    }

    public SystemWrapper getSystemWrapper() {
        return this.mSystemWrapper;
    }

    public IncrementalManager getIncrementalManager() {
        return this.mIncrementalManagerProducer.get(this, this.mPackageManager);
    }

    public DefaultAppProvider getDefaultAppProvider() {
        return this.mDefaultAppProviderProducer.get(this, this.mPackageManager);
    }

    public PackageParser2 getScanningCachingPackageParser() {
        return this.mScanningCachingPackageParserProducer.produce(this, this.mPackageManager);
    }

    public PackageParser2 getScanningPackageParser() {
        return this.mScanningPackageParserProducer.produce(this, this.mPackageManager);
    }

    public PackageParser2 getPreparingPackageParser() {
        return this.mPreparingPackageParserProducer.produce(this, this.mPackageManager);
    }

    public PackageInstallerService getPackageInstallerService() {
        return this.mPackageInstallerServiceProducer.get(this, this.mPackageManager);
    }

    public InstantAppResolverConnection getInstantAppResolverConnection(ComponentName instantAppResolverComponent) {
        return this.mInstantAppResolverConnectionProducer.produce(this, this.mPackageManager, instantAppResolverComponent);
    }

    public ModuleInfoProvider getModuleInfoProvider() {
        return this.mModuleInfoProviderProducer.get(this, this.mPackageManager);
    }

    public LegacyPermissionManagerInternal getLegacyPermissionManagerInternal() {
        return this.mLegacyPermissionManagerInternalProducer.get(this, this.mPackageManager);
    }

    public DomainVerificationManagerInternal getDomainVerificationManagerInternal() {
        return this.mDomainVerificationManagerInternalProducer.get(this, this.mPackageManager);
    }

    public Handler getHandler() {
        return this.mHandlerProducer.get(this, this.mPackageManager);
    }

    public ActivityManagerInternal getActivityManagerInternal() {
        return (ActivityManagerInternal) getLocalService(ActivityManagerInternal.class);
    }

    public BackgroundDexOptService getBackgroundDexOptService() {
        return this.mBackgroundDexOptService.get(this, this.mPackageManager);
    }

    public IBackupManager getIBackupManager() {
        return this.mIBackupManager.get(this, this.mPackageManager);
    }

    public SharedLibrariesImpl getSharedLibrariesImpl() {
        return this.mSharedLibrariesProducer.get(this, this.mPackageManager);
    }
}
