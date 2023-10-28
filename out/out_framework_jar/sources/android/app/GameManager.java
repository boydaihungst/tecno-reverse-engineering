package android.app;

import android.annotation.SystemApi;
import android.app.IGameManagerService;
import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes.dex */
public final class GameManager {
    public static final int GAME_MODE_BATTERY = 3;
    public static final int GAME_MODE_PERFORMANCE = 2;
    public static final int GAME_MODE_STANDARD = 1;
    public static final int GAME_MODE_UNSUPPORTED = 0;
    private static final String TAG = "GameManager";
    private final Context mContext;
    private final IGameManagerService mService = IGameManagerService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.GAME_SERVICE));

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface GameMode {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GameManager(Context context, Handler handler) throws ServiceManager.ServiceNotFoundException {
        this.mContext = context;
    }

    public int getGameMode() {
        try {
            return this.mService.getGameMode(this.mContext.getPackageName(), this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getGameMode(String packageName) {
        try {
            return this.mService.getGameMode(packageName, this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public GameModeInfo getGameModeInfo(String packageName) {
        try {
            return this.mService.getGameModeInfo(packageName, this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setGameMode(String packageName, int gameMode) {
        try {
            this.mService.setGameMode(packageName, gameMode, this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int[] getAvailableGameModes(String packageName) {
        try {
            return this.mService.getAvailableGameModes(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isAngleEnabled(String packageName) {
        try {
            return this.mService.isAngleEnabled(packageName, this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void notifyGraphicsEnvironmentSetup() {
        try {
            this.mService.notifyGraphicsEnvironmentSetup(this.mContext.getPackageName(), this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setGameState(GameState gameState) {
        try {
            this.mService.setGameState(this.mContext.getPackageName(), gameState, this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setGameServiceProvider(String packageName) {
        try {
            this.mService.setGameServiceProvider(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
