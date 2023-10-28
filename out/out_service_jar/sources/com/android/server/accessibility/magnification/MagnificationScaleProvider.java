package com.android.server.accessibility.magnification;

import android.content.Context;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.SparseArray;
import com.android.internal.os.BackgroundThread;
/* loaded from: classes.dex */
public class MagnificationScaleProvider {
    protected static final float DEFAULT_MAGNIFICATION_SCALE = 2.0f;
    public static final float MAX_SCALE = 8.0f;
    public static final float MIN_SCALE = 1.0f;
    private final Context mContext;
    private final SparseArray<SparseArray<Float>> mUsersScales = new SparseArray<>();
    private int mCurrentUserId = 0;
    private final Object mLock = new Object();

    public MagnificationScaleProvider(Context context) {
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void putScale(final float scale, int displayId) {
        if (displayId == 0) {
            BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.accessibility.magnification.MagnificationScaleProvider$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MagnificationScaleProvider.this.m738xad6a86a1(scale);
                }
            });
            return;
        }
        synchronized (this.mLock) {
            getScalesWithCurrentUser().put(displayId, Float.valueOf(scale));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$putScale$0$com-android-server-accessibility-magnification-MagnificationScaleProvider  reason: not valid java name */
    public /* synthetic */ void m738xad6a86a1(float scale) {
        Settings.Secure.putFloatForUser(this.mContext.getContentResolver(), "accessibility_display_magnification_scale", scale, this.mCurrentUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getScale(int displayId) {
        float floatValue;
        if (displayId == 0) {
            return Settings.Secure.getFloatForUser(this.mContext.getContentResolver(), "accessibility_display_magnification_scale", DEFAULT_MAGNIFICATION_SCALE, this.mCurrentUserId);
        }
        synchronized (this.mLock) {
            floatValue = getScalesWithCurrentUser().get(displayId, Float.valueOf((float) DEFAULT_MAGNIFICATION_SCALE)).floatValue();
        }
        return floatValue;
    }

    private SparseArray<Float> getScalesWithCurrentUser() {
        SparseArray<Float> scales = this.mUsersScales.get(this.mCurrentUserId);
        if (scales == null) {
            SparseArray<Float> scales2 = new SparseArray<>();
            this.mUsersScales.put(this.mCurrentUserId, scales2);
            return scales2;
        }
        return scales;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserChanged(int userId) {
        synchronized (this.mLock) {
            this.mCurrentUserId = userId;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserRemoved(int userId) {
        synchronized (this.mLock) {
            this.mUsersScales.remove(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplayRemoved(int displayId) {
        synchronized (this.mLock) {
            int userCounts = this.mUsersScales.size();
            for (int i = userCounts - 1; i >= 0; i--) {
                this.mUsersScales.get(i).remove(displayId);
            }
        }
    }

    public String toString() {
        String str;
        synchronized (this.mLock) {
            str = "MagnificationScaleProvider{mCurrentUserId=" + this.mCurrentUserId + "Scale on the default display=" + getScale(0) + "Scales on non-default displays=" + getScalesWithCurrentUser() + '}';
        }
        return str;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static float constrainScale(float scale) {
        return MathUtils.constrain(scale, 1.0f, 8.0f);
    }
}
