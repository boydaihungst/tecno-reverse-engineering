package com.android.server.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.SystemService;
import com.android.server.app.GameServiceConfiguration;
import java.util.Objects;
import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public final class GameServiceController {
    private static final String TAG = "GameServiceController";
    private volatile GameServiceConfiguration.GameServiceComponentConfiguration mActiveGameServiceComponentConfiguration;
    private volatile String mActiveGameServiceProviderPackage;
    private final Executor mBackgroundExecutor;
    private final Context mContext;
    private volatile SystemService.TargetUser mCurrentForegroundUser;
    private BroadcastReceiver mGameServicePackageChangedReceiver;
    private volatile GameServiceProviderInstance mGameServiceProviderInstance;
    private final GameServiceProviderInstanceFactory mGameServiceProviderInstanceFactory;
    private volatile String mGameServiceProviderOverride;
    private final GameServiceProviderSelector mGameServiceProviderSelector;
    private volatile boolean mHasBootCompleted;
    private final Object mLock = new Object();

    public GameServiceController(Context context, Executor backgroundExecutor, GameServiceProviderSelector gameServiceProviderSelector, GameServiceProviderInstanceFactory gameServiceProviderInstanceFactory) {
        this.mContext = context;
        this.mGameServiceProviderInstanceFactory = gameServiceProviderInstanceFactory;
        this.mBackgroundExecutor = backgroundExecutor;
        this.mGameServiceProviderSelector = gameServiceProviderSelector;
    }

    public void onBootComplete() {
        if (this.mHasBootCompleted) {
            return;
        }
        this.mHasBootCompleted = true;
        this.mBackgroundExecutor.execute(new GameServiceController$$ExternalSyntheticLambda0(this));
    }

    public void notifyUserStarted(SystemService.TargetUser user) {
        if (this.mCurrentForegroundUser != null) {
            return;
        }
        setCurrentForegroundUserAndEvaluateProvider(user);
    }

    public void notifyNewForegroundUser(SystemService.TargetUser user) {
        setCurrentForegroundUserAndEvaluateProvider(user);
    }

    public void notifyUserUnlocking(SystemService.TargetUser user) {
        boolean isSameAsForegroundUser = this.mCurrentForegroundUser != null && this.mCurrentForegroundUser.getUserIdentifier() == user.getUserIdentifier();
        if (!isSameAsForegroundUser) {
            return;
        }
        this.mBackgroundExecutor.execute(new GameServiceController$$ExternalSyntheticLambda0(this));
    }

    public void notifyUserStopped(SystemService.TargetUser user) {
        boolean isSameAsForegroundUser = this.mCurrentForegroundUser != null && this.mCurrentForegroundUser.getUserIdentifier() == user.getUserIdentifier();
        if (!isSameAsForegroundUser) {
            return;
        }
        setCurrentForegroundUserAndEvaluateProvider(null);
    }

    public void setGameServiceProvider(String packageName) {
        boolean hasPackageChanged = !Objects.equals(this.mGameServiceProviderOverride, packageName);
        if (!hasPackageChanged) {
            return;
        }
        this.mGameServiceProviderOverride = packageName;
        this.mBackgroundExecutor.execute(new GameServiceController$$ExternalSyntheticLambda0(this));
    }

    private void setCurrentForegroundUserAndEvaluateProvider(SystemService.TargetUser user) {
        boolean hasUserChanged = !Objects.equals(this.mCurrentForegroundUser, user);
        if (!hasUserChanged) {
            return;
        }
        this.mCurrentForegroundUser = user;
        this.mBackgroundExecutor.execute(new GameServiceController$$ExternalSyntheticLambda0(this));
    }

    public void evaluateActiveGameServiceProvider() {
        String gameServicePackage;
        GameServiceConfiguration.GameServiceComponentConfiguration gameServiceComponentConfiguration;
        if (!this.mHasBootCompleted) {
            return;
        }
        synchronized (this.mLock) {
            GameServiceConfiguration selectedGameServiceConfiguration = this.mGameServiceProviderSelector.get(this.mCurrentForegroundUser, this.mGameServiceProviderOverride);
            if (selectedGameServiceConfiguration == null) {
                gameServicePackage = null;
            } else {
                gameServicePackage = selectedGameServiceConfiguration.getPackageName();
            }
            if (selectedGameServiceConfiguration == null) {
                gameServiceComponentConfiguration = null;
            } else {
                gameServiceComponentConfiguration = selectedGameServiceConfiguration.getGameServiceComponentConfiguration();
            }
            evaluateGameServiceProviderPackageChangedListenerLocked(gameServicePackage);
            boolean didActiveGameServiceProviderChange = !Objects.equals(gameServiceComponentConfiguration, this.mActiveGameServiceComponentConfiguration);
            if (didActiveGameServiceProviderChange) {
                if (this.mGameServiceProviderInstance != null) {
                    Slog.i(TAG, "Stopping Game Service provider: " + this.mActiveGameServiceComponentConfiguration);
                    this.mGameServiceProviderInstance.stop();
                    this.mGameServiceProviderInstance = null;
                }
                this.mActiveGameServiceComponentConfiguration = gameServiceComponentConfiguration;
                if (this.mActiveGameServiceComponentConfiguration == null) {
                    return;
                }
                Slog.i(TAG, "Starting Game Service provider: " + this.mActiveGameServiceComponentConfiguration);
                this.mGameServiceProviderInstance = this.mGameServiceProviderInstanceFactory.create(this.mActiveGameServiceComponentConfiguration);
                this.mGameServiceProviderInstance.start();
            }
        }
    }

    private void evaluateGameServiceProviderPackageChangedListenerLocked(String gameServicePackage) {
        if (TextUtils.equals(this.mActiveGameServiceProviderPackage, gameServicePackage)) {
            return;
        }
        BroadcastReceiver broadcastReceiver = this.mGameServicePackageChangedReceiver;
        if (broadcastReceiver != null) {
            this.mContext.unregisterReceiver(broadcastReceiver);
            this.mGameServicePackageChangedReceiver = null;
        }
        this.mActiveGameServiceProviderPackage = gameServicePackage;
        if (TextUtils.isEmpty(this.mActiveGameServiceProviderPackage)) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        intentFilter.addDataSchemeSpecificPart(gameServicePackage, 0);
        PackageChangedBroadcastReceiver packageChangedBroadcastReceiver = new PackageChangedBroadcastReceiver(gameServicePackage);
        this.mGameServicePackageChangedReceiver = packageChangedBroadcastReceiver;
        this.mContext.registerReceiver(packageChangedBroadcastReceiver, intentFilter);
    }

    /* loaded from: classes.dex */
    public final class PackageChangedBroadcastReceiver extends BroadcastReceiver {
        private final String mPackageName;

        PackageChangedBroadcastReceiver(String packageName) {
            GameServiceController.this = r1;
            this.mPackageName = packageName;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (!TextUtils.equals(intent.getData().getSchemeSpecificPart(), this.mPackageName)) {
                return;
            }
            Executor executor = GameServiceController.this.mBackgroundExecutor;
            final GameServiceController gameServiceController = GameServiceController.this;
            executor.execute(new Runnable() { // from class: com.android.server.app.GameServiceController$PackageChangedBroadcastReceiver$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GameServiceController.this.evaluateActiveGameServiceProvider();
                }
            });
        }
    }
}
