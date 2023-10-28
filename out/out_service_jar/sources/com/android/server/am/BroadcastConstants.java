package com.android.server.am;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Slog;
import android.util.TimeUtils;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes.dex */
public class BroadcastConstants {
    private static final float DEFAULT_DEFERRAL_DECAY_FACTOR = 0.75f;
    private static final long DEFAULT_DEFERRAL_FLOOR = 0;
    public static final int DEFER_BOOT_COMPLETED_BROADCAST_ALL = 1;
    public static final int DEFER_BOOT_COMPLETED_BROADCAST_BACKGROUND_RESTRICTED_ONLY = 2;
    static final long DEFER_BOOT_COMPLETED_BROADCAST_CHANGE_ID = 203704822;
    public static final int DEFER_BOOT_COMPLETED_BROADCAST_NONE = 0;
    public static final int DEFER_BOOT_COMPLETED_BROADCAST_TARGET_T_ONLY = 4;
    static final String KEY_ALLOW_BG_ACTIVITY_START_TIMEOUT = "bcast_allow_bg_activity_start_timeout";
    static final String KEY_DEFERRAL = "bcast_deferral";
    static final String KEY_DEFERRAL_DECAY_FACTOR = "bcast_deferral_decay_factor";
    static final String KEY_DEFERRAL_FLOOR = "bcast_deferral_floor";
    static final String KEY_SLOW_TIME = "bcast_slow_time";
    static final String KEY_TIMEOUT = "bcast_timeout";
    private static final String TAG = "BroadcastConstants";
    private ContentResolver mResolver;
    private String mSettingsKey;
    private SettingsObserver mSettingsObserver;
    private static final long DEFAULT_TIMEOUT = Build.HW_TIMEOUT_MULTIPLIER * 10000;
    private static final long DEFAULT_SLOW_TIME = Build.HW_TIMEOUT_MULTIPLIER * 5000;
    private static final long DEFAULT_DEFERRAL = Build.HW_TIMEOUT_MULTIPLIER * 5000;
    private static final long DEFAULT_ALLOW_BG_ACTIVITY_START_TIMEOUT = Build.HW_TIMEOUT_MULTIPLIER * 10000;
    public long TIMEOUT = DEFAULT_TIMEOUT;
    public long SLOW_TIME = DEFAULT_SLOW_TIME;
    public long DEFERRAL = DEFAULT_DEFERRAL;
    public float DEFERRAL_DECAY_FACTOR = DEFAULT_DEFERRAL_DECAY_FACTOR;
    public long DEFERRAL_FLOOR = 0;
    public long ALLOW_BG_ACTIVITY_START_TIMEOUT = DEFAULT_ALLOW_BG_ACTIVITY_START_TIMEOUT;
    private final KeyValueListParser mParser = new KeyValueListParser(',');

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface DeferBootCompletedBroadcastType {
    }

    /* loaded from: classes.dex */
    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            BroadcastConstants.this.updateConstants();
        }
    }

    public BroadcastConstants(String settingsKey) {
        this.mSettingsKey = settingsKey;
    }

    public void startObserving(Handler handler, ContentResolver resolver) {
        this.mResolver = resolver;
        this.mSettingsObserver = new SettingsObserver(handler);
        this.mResolver.registerContentObserver(Settings.Global.getUriFor(this.mSettingsKey), false, this.mSettingsObserver);
        updateConstants();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateConstants() {
        synchronized (this.mParser) {
            try {
                try {
                    this.mParser.setString(Settings.Global.getString(this.mResolver, this.mSettingsKey));
                    this.TIMEOUT = this.mParser.getLong(KEY_TIMEOUT, this.TIMEOUT);
                    this.SLOW_TIME = this.mParser.getLong(KEY_SLOW_TIME, this.SLOW_TIME);
                    this.DEFERRAL = this.mParser.getLong(KEY_DEFERRAL, this.DEFERRAL);
                    this.DEFERRAL_DECAY_FACTOR = this.mParser.getFloat(KEY_DEFERRAL_DECAY_FACTOR, this.DEFERRAL_DECAY_FACTOR);
                    this.DEFERRAL_FLOOR = this.mParser.getLong(KEY_DEFERRAL_FLOOR, this.DEFERRAL_FLOOR);
                    this.ALLOW_BG_ACTIVITY_START_TIMEOUT = this.mParser.getLong(KEY_ALLOW_BG_ACTIVITY_START_TIMEOUT, this.ALLOW_BG_ACTIVITY_START_TIMEOUT);
                } catch (IllegalArgumentException e) {
                    Slog.e(TAG, "Bad broadcast settings in key '" + this.mSettingsKey + "'", e);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void dump(PrintWriter pw) {
        synchronized (this.mParser) {
            pw.println();
            pw.print("  Broadcast parameters (key=");
            pw.print(this.mSettingsKey);
            pw.print(", observing=");
            pw.print(this.mSettingsObserver != null);
            pw.println("):");
            pw.print("    ");
            pw.print(KEY_TIMEOUT);
            pw.print(" = ");
            TimeUtils.formatDuration(this.TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_SLOW_TIME);
            pw.print(" = ");
            TimeUtils.formatDuration(this.SLOW_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_DEFERRAL);
            pw.print(" = ");
            TimeUtils.formatDuration(this.DEFERRAL, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_DEFERRAL_DECAY_FACTOR);
            pw.print(" = ");
            pw.println(this.DEFERRAL_DECAY_FACTOR);
            pw.print("    ");
            pw.print(KEY_DEFERRAL_FLOOR);
            pw.print(" = ");
            TimeUtils.formatDuration(this.DEFERRAL_FLOOR, pw);
            pw.print("    ");
            pw.print(KEY_ALLOW_BG_ACTIVITY_START_TIMEOUT);
            pw.print(" = ");
            TimeUtils.formatDuration(this.ALLOW_BG_ACTIVITY_START_TIMEOUT, pw);
            pw.println();
        }
    }
}
