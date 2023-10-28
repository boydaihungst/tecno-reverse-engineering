package com.android.server.app;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.R;
import com.android.server.SystemService;
import com.android.server.app.GameServiceConfiguration;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
final class GameServiceProviderSelectorImpl implements GameServiceProviderSelector {
    private static final boolean DEBUG = false;
    private static final String GAME_SERVICE_NODE_NAME = "game-service";
    private static final String TAG = "GameServiceProviderSelector";
    private final PackageManager mPackageManager;
    private final Resources mResources;

    /* JADX INFO: Access modifiers changed from: package-private */
    public GameServiceProviderSelectorImpl(Resources resources, PackageManager packageManager) {
        this.mResources = resources;
        this.mPackageManager = packageManager;
    }

    @Override // com.android.server.app.GameServiceProviderSelector
    public GameServiceConfiguration get(SystemService.TargetUser user, String packageNameOverride) {
        int resolveInfoQueryFlags;
        String gameServicePackage;
        ServiceInfo gameServiceServiceInfo;
        ComponentName gameSessionServiceComponentName;
        if (user == null) {
            return null;
        }
        boolean isUserSupported = user.isFull() && !user.isManagedProfile();
        if (!isUserSupported) {
            Slog.i(TAG, "Game Service not supported for user: " + user.getUserIdentifier());
            return null;
        }
        if (!TextUtils.isEmpty(packageNameOverride)) {
            resolveInfoQueryFlags = 0;
            gameServicePackage = packageNameOverride;
        } else {
            resolveInfoQueryFlags = 1048576;
            gameServicePackage = this.mResources.getString(17040042);
        }
        if (TextUtils.isEmpty(gameServicePackage)) {
            Slog.w(TAG, "No game service package defined");
            return null;
        }
        int userId = user.getUserIdentifier();
        List<ResolveInfo> gameServiceResolveInfos = this.mPackageManager.queryIntentServicesAsUser(new Intent("android.service.games.action.GAME_SERVICE").setPackage(gameServicePackage), resolveInfoQueryFlags | 128, userId);
        if (gameServiceResolveInfos == null || gameServiceResolveInfos.isEmpty()) {
            Slog.w(TAG, "No available game service found for user id: " + userId);
            return new GameServiceConfiguration(gameServicePackage, null);
        }
        GameServiceConfiguration selectedProvider = null;
        Iterator<ResolveInfo> it = gameServiceResolveInfos.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            ResolveInfo resolveInfo = it.next();
            if (resolveInfo.serviceInfo != null && (gameSessionServiceComponentName = determineGameSessionServiceFromGameService((gameServiceServiceInfo = resolveInfo.serviceInfo))) != null) {
                selectedProvider = new GameServiceConfiguration(gameServicePackage, new GameServiceConfiguration.GameServiceComponentConfiguration(new UserHandle(userId), gameServiceServiceInfo.getComponentName(), gameSessionServiceComponentName));
                break;
            }
        }
        if (selectedProvider == null) {
            Slog.w(TAG, "No valid game service found for user id: " + userId);
            return new GameServiceConfiguration(gameServicePackage, null);
        }
        return selectedProvider;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [169=4] */
    private ComponentName determineGameSessionServiceFromGameService(ServiceInfo gameServiceServiceInfo) {
        try {
            XmlResourceParser parser = gameServiceServiceInfo.loadXmlMetaData(this.mPackageManager, "android.game_service");
            if (parser == null) {
                Slog.w(TAG, "No android.game_service meta-data found for " + gameServiceServiceInfo.getComponentName());
                if (parser != null) {
                    parser.close();
                }
                return null;
            }
            Resources resources = this.mPackageManager.getResourcesForApplication(gameServiceServiceInfo.packageName);
            AttributeSet attributeSet = Xml.asAttributeSet(parser);
            while (true) {
                int type = parser.next();
                if (type == 1 || type == 2) {
                    break;
                }
            }
            boolean isStartingTagGameService = GAME_SERVICE_NODE_NAME.equals(parser.getName());
            if (!isStartingTagGameService) {
                Slog.w(TAG, "Meta-data does not start with game-service tag");
                if (parser != null) {
                    parser.close();
                }
                return null;
            }
            TypedArray array = resources.obtainAttributes(attributeSet, R.styleable.GameService);
            String gameSessionService = array.getString(0);
            array.recycle();
            if (parser != null) {
                parser.close();
            }
            if (TextUtils.isEmpty(gameSessionService)) {
                Slog.w(TAG, "No gameSessionService specified");
                return null;
            }
            ComponentName componentName = new ComponentName(gameServiceServiceInfo.packageName, gameSessionService);
            try {
                this.mPackageManager.getServiceInfo(componentName, 0);
                return componentName;
            } catch (PackageManager.NameNotFoundException e) {
                Slog.w(TAG, "GameSessionService does not exist: " + componentName);
                return null;
            }
        } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException ex) {
            Slog.w("Error while parsing meta-data for " + gameServiceServiceInfo.getComponentName(), ex);
            return null;
        }
    }
}
