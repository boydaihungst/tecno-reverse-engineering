package android.app;

import android.annotation.SystemApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import android.os.RemoteException;
/* loaded from: classes.dex */
public class LocaleManager {
    private static final String TAG = "LocaleManager";
    private Context mContext;
    private ILocaleManager mService;

    public LocaleManager(Context context, ILocaleManager service) {
        this.mContext = context;
        this.mService = service;
    }

    public void setApplicationLocales(LocaleList locales) {
        setApplicationLocales(this.mContext.getPackageName(), locales);
    }

    @SystemApi
    public void setApplicationLocales(String appPackageName, LocaleList locales) {
        try {
            this.mService.setApplicationLocales(appPackageName, this.mContext.getUser().getIdentifier(), locales);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public LocaleList getApplicationLocales() {
        return getApplicationLocales(this.mContext.getPackageName());
    }

    public LocaleList getApplicationLocales(String appPackageName) {
        try {
            return this.mService.getApplicationLocales(appPackageName, this.mContext.getUser().getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public LocaleList getSystemLocales() {
        try {
            return this.mService.getSystemLocales();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setSystemLocales(LocaleList locales) {
        try {
            Configuration conf = ActivityManager.getService().getConfiguration();
            conf.setLocales(locales);
            ActivityManager.getService().updatePersistentConfiguration(conf);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
