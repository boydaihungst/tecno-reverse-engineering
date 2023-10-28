package android.hardware.display;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import java.time.LocalTime;
/* loaded from: classes.dex */
public class NightDisplayListener {
    private Callback mCallback;
    private final ContentObserver mContentObserver;
    private final Context mContext;
    private final Handler mHandler;
    private final ColorDisplayManager mManager;
    private final int mUserId;

    public NightDisplayListener(Context context) {
        this(context, ActivityManager.getCurrentUser(), new Handler(Looper.getMainLooper()));
    }

    public NightDisplayListener(Context context, Handler handler) {
        this(context, ActivityManager.getCurrentUser(), handler);
    }

    public NightDisplayListener(Context context, int userId, Handler handler) {
        Context applicationContext = context.getApplicationContext();
        this.mContext = applicationContext;
        this.mManager = (ColorDisplayManager) applicationContext.getSystemService(ColorDisplayManager.class);
        this.mUserId = userId;
        this.mHandler = handler;
        this.mContentObserver = new ContentObserver(handler) { // from class: android.hardware.display.NightDisplayListener.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                String setting = uri == null ? null : uri.getLastPathSegment();
                if (setting != null && NightDisplayListener.this.mCallback != null) {
                    char c = 65535;
                    switch (setting.hashCode()) {
                        case -2038150513:
                            if (setting.equals(Settings.Secure.NIGHT_DISPLAY_AUTO_MODE)) {
                                c = 1;
                                break;
                            }
                            break;
                        case -1761668069:
                            if (setting.equals(Settings.Secure.NIGHT_DISPLAY_CUSTOM_END_TIME)) {
                                c = 3;
                                break;
                            }
                            break;
                        case -969458956:
                            if (setting.equals(Settings.Secure.NIGHT_DISPLAY_COLOR_TEMPERATURE)) {
                                c = 4;
                                break;
                            }
                            break;
                        case 800115245:
                            if (setting.equals(Settings.Secure.NIGHT_DISPLAY_ACTIVATED)) {
                                c = 0;
                                break;
                            }
                            break;
                        case 1578271348:
                            if (setting.equals(Settings.Secure.NIGHT_DISPLAY_CUSTOM_START_TIME)) {
                                c = 2;
                                break;
                            }
                            break;
                    }
                    switch (c) {
                        case 0:
                            NightDisplayListener.this.mCallback.onActivated(NightDisplayListener.this.mManager.isNightDisplayActivated());
                            return;
                        case 1:
                            NightDisplayListener.this.mCallback.onAutoModeChanged(NightDisplayListener.this.mManager.getNightDisplayAutoMode());
                            return;
                        case 2:
                            NightDisplayListener.this.mCallback.onCustomStartTimeChanged(NightDisplayListener.this.mManager.getNightDisplayCustomStartTime());
                            return;
                        case 3:
                            NightDisplayListener.this.mCallback.onCustomEndTimeChanged(NightDisplayListener.this.mManager.getNightDisplayCustomEndTime());
                            return;
                        case 4:
                            NightDisplayListener.this.mCallback.onColorTemperatureChanged(NightDisplayListener.this.mManager.getNightDisplayColorTemperature());
                            return;
                        default:
                            return;
                    }
                }
            }
        };
    }

    public void setCallback(final Callback callback) {
        if (Looper.myLooper() != this.mHandler.getLooper()) {
            this.mHandler.post(new Runnable() { // from class: android.hardware.display.NightDisplayListener$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    NightDisplayListener.this.m1524x7e1990e2(callback);
                }
            });
        }
        m1524x7e1990e2(callback);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: setCallbackInternal */
    public void m1524x7e1990e2(Callback newCallback) {
        Callback oldCallback = this.mCallback;
        if (oldCallback != newCallback) {
            this.mCallback = newCallback;
            if (newCallback == null) {
                this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
            } else if (oldCallback == null) {
                ContentResolver cr = this.mContext.getContentResolver();
                try {
                    cr.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.NIGHT_DISPLAY_ACTIVATED), false, this.mContentObserver, this.mUserId);
                    cr.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.NIGHT_DISPLAY_AUTO_MODE), false, this.mContentObserver, this.mUserId);
                    cr.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.NIGHT_DISPLAY_CUSTOM_START_TIME), false, this.mContentObserver, this.mUserId);
                    cr.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.NIGHT_DISPLAY_CUSTOM_END_TIME), false, this.mContentObserver, this.mUserId);
                    cr.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.NIGHT_DISPLAY_COLOR_TEMPERATURE), false, this.mContentObserver, this.mUserId);
                } catch (SecurityException e) {
                    Log.d("NightDisplayListener", "SecurityException: " + e);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public interface Callback {
        default void onActivated(boolean activated) {
        }

        default void onAutoModeChanged(int autoMode) {
        }

        default void onCustomStartTimeChanged(LocalTime startTime) {
        }

        default void onCustomEndTimeChanged(LocalTime endTime) {
        }

        default void onColorTemperatureChanged(int colorTemperature) {
        }
    }
}
