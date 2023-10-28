package com.android.internal.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.app.IVoiceInteractionManagerService;
import java.util.ArrayList;
import java.util.Set;
/* loaded from: classes4.dex */
public class AssistUtils {
    public static final int INVOCATION_TYPE_GESTURE = 1;
    public static final int INVOCATION_TYPE_HOME_BUTTON_LONG_PRESS = 5;
    public static final String INVOCATION_TYPE_KEY = "invocation_type";
    public static final int INVOCATION_TYPE_PHYSICAL_GESTURE = 2;
    public static final int INVOCATION_TYPE_POWER_BUTTON_LONG_PRESS = 6;
    public static final int INVOCATION_TYPE_QUICK_SEARCH_BAR = 4;
    public static final int INVOCATION_TYPE_UNKNOWN = 0;
    public static final int INVOCATION_TYPE_VOICE = 3;
    private static final String TAG = "AssistUtils";
    private final Context mContext;
    private final IVoiceInteractionManagerService mVoiceInteractionManagerService = IVoiceInteractionManagerService.Stub.asInterface(ServiceManager.getService(Context.VOICE_INTERACTION_MANAGER_SERVICE));

    public AssistUtils(Context context) {
        this.mContext = context;
    }

    public boolean showSessionForActiveService(Bundle args, int sourceFlags, IVoiceInteractionSessionShowCallback showCallback, IBinder activityToken) {
        try {
            IVoiceInteractionManagerService iVoiceInteractionManagerService = this.mVoiceInteractionManagerService;
            if (iVoiceInteractionManagerService != null) {
                return iVoiceInteractionManagerService.showSessionForActiveService(args, sourceFlags, showCallback, activityToken);
            }
            return false;
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call showSessionForActiveService", e);
            return false;
        }
    }

    public void getActiveServiceSupportedActions(Set<String> voiceActions, IVoiceActionCheckCallback callback) {
        try {
            IVoiceInteractionManagerService iVoiceInteractionManagerService = this.mVoiceInteractionManagerService;
            if (iVoiceInteractionManagerService != null) {
                iVoiceInteractionManagerService.getActiveServiceSupportedActions(new ArrayList(voiceActions), callback);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call activeServiceSupportedActions", e);
            try {
                callback.onComplete(null);
            } catch (RemoteException e2) {
            }
        }
    }

    public void launchVoiceAssistFromKeyguard() {
        try {
            IVoiceInteractionManagerService iVoiceInteractionManagerService = this.mVoiceInteractionManagerService;
            if (iVoiceInteractionManagerService != null) {
                iVoiceInteractionManagerService.launchVoiceAssistFromKeyguard();
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call launchVoiceAssistFromKeyguard", e);
        }
    }

    public boolean activeServiceSupportsAssistGesture() {
        try {
            IVoiceInteractionManagerService iVoiceInteractionManagerService = this.mVoiceInteractionManagerService;
            if (iVoiceInteractionManagerService != null) {
                return iVoiceInteractionManagerService.activeServiceSupportsAssist();
            }
            return false;
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call activeServiceSupportsAssistGesture", e);
            return false;
        }
    }

    public boolean activeServiceSupportsLaunchFromKeyguard() {
        try {
            IVoiceInteractionManagerService iVoiceInteractionManagerService = this.mVoiceInteractionManagerService;
            if (iVoiceInteractionManagerService != null) {
                return iVoiceInteractionManagerService.activeServiceSupportsLaunchFromKeyguard();
            }
            return false;
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call activeServiceSupportsLaunchFromKeyguard", e);
            return false;
        }
    }

    public ComponentName getActiveServiceComponentName() {
        try {
            IVoiceInteractionManagerService iVoiceInteractionManagerService = this.mVoiceInteractionManagerService;
            if (iVoiceInteractionManagerService == null) {
                return null;
            }
            return iVoiceInteractionManagerService.getActiveServiceComponentName();
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call getActiveServiceComponentName", e);
            return null;
        }
    }

    public boolean isSessionRunning() {
        try {
            IVoiceInteractionManagerService iVoiceInteractionManagerService = this.mVoiceInteractionManagerService;
            if (iVoiceInteractionManagerService != null) {
                return iVoiceInteractionManagerService.isSessionRunning();
            }
            return false;
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call isSessionRunning", e);
            return false;
        }
    }

    public void hideCurrentSession() {
        try {
            IVoiceInteractionManagerService iVoiceInteractionManagerService = this.mVoiceInteractionManagerService;
            if (iVoiceInteractionManagerService != null) {
                iVoiceInteractionManagerService.hideCurrentSession();
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call hideCurrentSession", e);
        }
    }

    public void onLockscreenShown() {
        try {
            IVoiceInteractionManagerService iVoiceInteractionManagerService = this.mVoiceInteractionManagerService;
            if (iVoiceInteractionManagerService != null) {
                iVoiceInteractionManagerService.onLockscreenShown();
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call onLockscreenShown", e);
        }
    }

    public void registerVoiceInteractionSessionListener(IVoiceInteractionSessionListener listener) {
        try {
            IVoiceInteractionManagerService iVoiceInteractionManagerService = this.mVoiceInteractionManagerService;
            if (iVoiceInteractionManagerService != null) {
                iVoiceInteractionManagerService.registerVoiceInteractionSessionListener(listener);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to register voice interaction listener", e);
        }
    }

    public ComponentName getAssistComponentForUser(int userId) {
        String setting = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), Settings.Secure.ASSISTANT, userId);
        if (setting != null) {
            return ComponentName.unflattenFromString(setting);
        }
        return null;
    }

    public static boolean isPreinstalledAssistant(Context context, ComponentName assistant) {
        if (assistant == null) {
            return false;
        }
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(assistant.getPackageName(), 0);
            return applicationInfo.isSystemApp() || applicationInfo.isUpdatedSystemApp();
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isDisclosureEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ASSIST_DISCLOSURE_ENABLED, 0) != 0;
    }

    public static boolean shouldDisclose(Context context, ComponentName assistant) {
        return (allowDisablingAssistDisclosure(context) && !isDisclosureEnabled(context) && isPreinstalledAssistant(context, assistant)) ? false : true;
    }

    public static boolean allowDisablingAssistDisclosure(Context context) {
        return context.getResources().getBoolean(R.bool.config_allowDisablingAssistDisclosure);
    }
}
