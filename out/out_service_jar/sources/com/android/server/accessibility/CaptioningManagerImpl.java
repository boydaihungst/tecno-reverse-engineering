package com.android.server.accessibility;

import android.content.Context;
import android.os.Binder;
import android.provider.Settings;
import android.view.accessibility.CaptioningManager;
/* loaded from: classes.dex */
public class CaptioningManagerImpl implements CaptioningManager.SystemAudioCaptioningAccessing {
    private static final boolean SYSTEM_AUDIO_CAPTIONING_UI_DEFAULT_ENABLED = false;
    private final Context mContext;

    public CaptioningManagerImpl(Context context) {
        this.mContext = context;
    }

    public void setSystemAudioCaptioningEnabled(boolean isEnabled, int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "odi_captions_enabled", isEnabled ? 1 : 0, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean isSystemAudioCaptioningUiEnabled(int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "odi_captions_volume_ui_enabled", 0, userId) == 1;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void setSystemAudioCaptioningUiEnabled(boolean isEnabled, int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "odi_captions_volume_ui_enabled", isEnabled ? 1 : 0, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}
