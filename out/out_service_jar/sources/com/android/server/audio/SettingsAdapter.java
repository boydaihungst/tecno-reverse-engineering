package com.android.server.audio;

import android.content.ContentResolver;
import android.provider.Settings;
/* loaded from: classes.dex */
public class SettingsAdapter {
    public static SettingsAdapter getDefaultAdapter() {
        return new SettingsAdapter();
    }

    public int getGlobalInt(ContentResolver cr, String name, int def) {
        return Settings.Global.getInt(cr, name, def);
    }

    public String getGlobalString(ContentResolver resolver, String name) {
        return Settings.Global.getString(resolver, name);
    }

    public boolean putGlobalInt(ContentResolver cr, String name, int value) {
        return Settings.Global.putInt(cr, name, value);
    }

    public boolean putGlobalString(ContentResolver resolver, String name, String value) {
        return Settings.Global.putString(resolver, name, value);
    }

    public int getSystemIntForUser(ContentResolver cr, String name, int def, int userHandle) {
        return Settings.System.getIntForUser(cr, name, def, userHandle);
    }

    public boolean putSystemIntForUser(ContentResolver cr, String name, int value, int userHandle) {
        return Settings.System.putIntForUser(cr, name, value, userHandle);
    }

    public int getSecureIntForUser(ContentResolver cr, String name, int def, int userHandle) {
        return Settings.Secure.getIntForUser(cr, name, def, userHandle);
    }

    public String getSecureStringForUser(ContentResolver resolver, String name, int userHandle) {
        return Settings.Secure.getStringForUser(resolver, name, userHandle);
    }

    public boolean putSecureIntForUser(ContentResolver cr, String name, int value, int userHandle) {
        return Settings.Secure.putIntForUser(cr, name, value, userHandle);
    }

    public boolean putSecureStringForUser(ContentResolver cr, String name, String value, int userHandle) {
        return Settings.Secure.putStringForUser(cr, name, value, userHandle);
    }
}
