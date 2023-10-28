package com.android.internal.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import com.android.internal.R;
import com.android.internal.config.sysui.SystemUiDeviceConfigFlags;
import java.util.concurrent.Executor;
/* loaded from: classes4.dex */
public class GestureNavigationSettingsObserver extends ContentObserver {
    private Context mContext;
    private Handler mMainHandler;
    private Runnable mOnChangeRunnable;
    private final DeviceConfig.OnPropertiesChangedListener mOnPropertiesChangedListener;

    public GestureNavigationSettingsObserver(Handler handler, Context context, Runnable onChangeRunnable) {
        super(handler);
        this.mOnPropertiesChangedListener = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.internal.policy.GestureNavigationSettingsObserver.1
            @Override // android.provider.DeviceConfig.OnPropertiesChangedListener
            public void onPropertiesChanged(DeviceConfig.Properties properties) {
                if (DeviceConfig.NAMESPACE_SYSTEMUI.equals(properties.getNamespace()) && GestureNavigationSettingsObserver.this.mOnChangeRunnable != null) {
                    GestureNavigationSettingsObserver.this.mOnChangeRunnable.run();
                }
            }
        };
        this.mMainHandler = handler;
        this.mContext = context;
        this.mOnChangeRunnable = onChangeRunnable;
    }

    public void register() {
        ContentResolver r = this.mContext.getContentResolver();
        r.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.BACK_GESTURE_INSET_SCALE_LEFT), false, this, -1);
        r.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.BACK_GESTURE_INSET_SCALE_RIGHT), false, this, -1);
        r.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.USER_SETUP_COMPLETE), false, this, -1);
        DeviceConfig.addOnPropertiesChangedListener(DeviceConfig.NAMESPACE_SYSTEMUI, new Executor() { // from class: com.android.internal.policy.GestureNavigationSettingsObserver$$ExternalSyntheticLambda0
            @Override // java.util.concurrent.Executor
            public final void execute(Runnable runnable) {
                GestureNavigationSettingsObserver.this.m6885x21607929(runnable);
            }
        }, this.mOnPropertiesChangedListener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$register$0$com-android-internal-policy-GestureNavigationSettingsObserver  reason: not valid java name */
    public /* synthetic */ void m6885x21607929(Runnable runnable) {
        this.mMainHandler.post(runnable);
    }

    public void unregister() {
        this.mContext.getContentResolver().unregisterContentObserver(this);
        DeviceConfig.removeOnPropertiesChangedListener(this.mOnPropertiesChangedListener);
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        Runnable runnable = this.mOnChangeRunnable;
        if (runnable != null) {
            runnable.run();
        }
    }

    public int getLeftSensitivity(Resources userRes) {
        return getSensitivity(userRes, Settings.Secure.BACK_GESTURE_INSET_SCALE_LEFT);
    }

    public int getRightSensitivity(Resources userRes) {
        return getSensitivity(userRes, Settings.Secure.BACK_GESTURE_INSET_SCALE_RIGHT);
    }

    public boolean areNavigationButtonForcedVisible() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 0, -2) == 0;
    }

    private int getSensitivity(Resources userRes, String side) {
        float backGestureInset;
        DisplayMetrics dm = userRes.getDisplayMetrics();
        float defaultInset = userRes.getDimension(R.dimen.config_backGestureInset) / dm.density;
        if (defaultInset > 0.0f) {
            backGestureInset = DeviceConfig.getFloat(DeviceConfig.NAMESPACE_SYSTEMUI, SystemUiDeviceConfigFlags.BACK_GESTURE_EDGE_WIDTH, defaultInset);
        } else {
            backGestureInset = defaultInset;
        }
        float inset = TypedValue.applyDimension(1, backGestureInset, dm);
        float scale = Settings.Secure.getFloatForUser(this.mContext.getContentResolver(), side, 1.0f, -2);
        return (int) (inset * scale);
    }
}
