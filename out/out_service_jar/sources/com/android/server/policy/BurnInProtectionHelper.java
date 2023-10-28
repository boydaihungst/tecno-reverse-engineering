package com.android.server.policy;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.os.SystemClock;
import android.view.Display;
import android.view.animation.LinearInterpolator;
import com.android.server.LocalServices;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
/* loaded from: classes2.dex */
public class BurnInProtectionHelper implements DisplayManager.DisplayListener, Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {
    private static final String ACTION_BURN_IN_PROTECTION = "android.internal.policy.action.BURN_IN_PROTECTION";
    public static final int BURN_IN_MAX_RADIUS_DEFAULT = -1;
    private static final int BURN_IN_SHIFT_STEP = 2;
    private static final long CENTERING_ANIMATION_DURATION_MS = 100;
    private static final boolean DEBUG = false;
    private static final String TAG = "BurnInProtection";
    private final AlarmManager mAlarmManager;
    private boolean mBurnInProtectionActive;
    private final PendingIntent mBurnInProtectionIntent;
    private final int mBurnInRadiusMaxSquared;
    private final ValueAnimator mCenteringAnimator;
    private final Display mDisplay;
    private final DisplayManagerInternal mDisplayManagerInternal;
    private boolean mFirstUpdate;
    private final int mMaxHorizontalBurnInOffset;
    private final int mMaxVerticalBurnInOffset;
    private final int mMinHorizontalBurnInOffset;
    private final int mMinVerticalBurnInOffset;
    private static final long BURNIN_PROTECTION_FIRST_WAKEUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long BURNIN_PROTECTION_SUBSEQUENT_WAKEUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(2);
    private static final long BURNIN_PROTECTION_MINIMAL_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10);
    private int mLastBurnInXOffset = 0;
    private int mXOffsetDirection = 1;
    private int mLastBurnInYOffset = 0;
    private int mYOffsetDirection = 1;
    private int mAppliedBurnInXOffset = 0;
    private int mAppliedBurnInYOffset = 0;
    private BroadcastReceiver mBurnInProtectionReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.BurnInProtectionHelper.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            BurnInProtectionHelper.this.updateBurnInProtection();
        }
    };

    public BurnInProtectionHelper(Context context, int minHorizontalOffset, int maxHorizontalOffset, int minVerticalOffset, int maxVerticalOffset, int maxOffsetRadius) {
        this.mMinHorizontalBurnInOffset = minHorizontalOffset;
        this.mMaxHorizontalBurnInOffset = maxHorizontalOffset;
        this.mMinVerticalBurnInOffset = minVerticalOffset;
        this.mMaxVerticalBurnInOffset = maxVerticalOffset;
        if (maxOffsetRadius != -1) {
            this.mBurnInRadiusMaxSquared = maxOffsetRadius * maxOffsetRadius;
        } else {
            this.mBurnInRadiusMaxSquared = -1;
        }
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        context.registerReceiver(this.mBurnInProtectionReceiver, new IntentFilter(ACTION_BURN_IN_PROTECTION));
        Intent intent = new Intent(ACTION_BURN_IN_PROTECTION);
        intent.setPackage(context.getPackageName());
        intent.setFlags(1073741824);
        this.mBurnInProtectionIntent = PendingIntent.getBroadcast(context, 0, intent, AudioFormat.E_AC3);
        DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
        this.mDisplay = displayManager.getDisplay(0);
        displayManager.registerDisplayListener(this, null);
        ValueAnimator ofFloat = ValueAnimator.ofFloat(1.0f, 0.0f);
        this.mCenteringAnimator = ofFloat;
        ofFloat.setDuration(CENTERING_ANIMATION_DURATION_MS);
        ofFloat.setInterpolator(new LinearInterpolator());
        ofFloat.addListener(this);
        ofFloat.addUpdateListener(this);
    }

    public void startBurnInProtection() {
        if (!this.mBurnInProtectionActive) {
            this.mBurnInProtectionActive = true;
            this.mFirstUpdate = true;
            this.mCenteringAnimator.cancel();
            updateBurnInProtection();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBurnInProtection() {
        long interval;
        if (this.mBurnInProtectionActive) {
            boolean z = this.mFirstUpdate;
            if (z) {
                interval = BURNIN_PROTECTION_FIRST_WAKEUP_INTERVAL_MS;
            } else {
                interval = BURNIN_PROTECTION_SUBSEQUENT_WAKEUP_INTERVAL_MS;
            }
            if (z) {
                this.mFirstUpdate = false;
            } else {
                adjustOffsets();
                this.mAppliedBurnInXOffset = this.mLastBurnInXOffset;
                this.mAppliedBurnInYOffset = this.mLastBurnInYOffset;
                this.mDisplayManagerInternal.setDisplayOffsets(this.mDisplay.getDisplayId(), this.mLastBurnInXOffset, this.mLastBurnInYOffset);
            }
            long nowWall = System.currentTimeMillis();
            long nowElapsed = SystemClock.elapsedRealtime();
            long nextWall = BURNIN_PROTECTION_MINIMAL_INTERVAL_MS + nowWall;
            long nextElapsed = (((nextWall - (nextWall % interval)) + interval) - nowWall) + nowElapsed;
            this.mAlarmManager.setExact(3, nextElapsed, this.mBurnInProtectionIntent);
            return;
        }
        this.mAlarmManager.cancel(this.mBurnInProtectionIntent);
        this.mCenteringAnimator.start();
    }

    public void cancelBurnInProtection() {
        if (this.mBurnInProtectionActive) {
            this.mBurnInProtectionActive = false;
            updateBurnInProtection();
        }
    }

    private void adjustOffsets() {
        int i;
        int i2;
        int i3;
        do {
            int i4 = this.mXOffsetDirection;
            int xChange = i4 * 2;
            int i5 = this.mLastBurnInXOffset + xChange;
            this.mLastBurnInXOffset = i5;
            if (i5 > this.mMaxHorizontalBurnInOffset || i5 < this.mMinHorizontalBurnInOffset) {
                this.mLastBurnInXOffset = i5 - xChange;
                this.mXOffsetDirection = i4 * (-1);
                int i6 = this.mYOffsetDirection;
                int yChange = i6 * 2;
                int i7 = this.mLastBurnInYOffset + yChange;
                this.mLastBurnInYOffset = i7;
                if (i7 > this.mMaxVerticalBurnInOffset || i7 < this.mMinVerticalBurnInOffset) {
                    this.mLastBurnInYOffset = i7 - yChange;
                    this.mYOffsetDirection = i6 * (-1);
                }
            }
            i = this.mBurnInRadiusMaxSquared;
            if (i == -1) {
                return;
            }
            i2 = this.mLastBurnInXOffset;
            i3 = this.mLastBurnInYOffset;
        } while ((i2 * i2) + (i3 * i3) > i);
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + TAG);
        String prefix2 = prefix + "  ";
        pw.println(prefix2 + "mBurnInProtectionActive=" + this.mBurnInProtectionActive);
        pw.println(prefix2 + "mHorizontalBurnInOffsetsBounds=(" + this.mMinHorizontalBurnInOffset + ", " + this.mMaxHorizontalBurnInOffset + ")");
        pw.println(prefix2 + "mVerticalBurnInOffsetsBounds=(" + this.mMinVerticalBurnInOffset + ", " + this.mMaxVerticalBurnInOffset + ")");
        pw.println(prefix2 + "mBurnInRadiusMaxSquared=" + this.mBurnInRadiusMaxSquared);
        pw.println(prefix2 + "mLastBurnInOffset=(" + this.mLastBurnInXOffset + ", " + this.mLastBurnInYOffset + ")");
        pw.println(prefix2 + "mOfsetChangeDirections=(" + this.mXOffsetDirection + ", " + this.mYOffsetDirection + ")");
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayAdded(int i) {
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayRemoved(int i) {
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayChanged(int displayId) {
        if (displayId == this.mDisplay.getDisplayId()) {
            if (this.mDisplay.getState() == 3 || this.mDisplay.getState() == 4 || this.mDisplay.getState() == 6) {
                startBurnInProtection();
            } else {
                cancelBurnInProtection();
            }
        }
    }

    @Override // android.animation.Animator.AnimatorListener
    public void onAnimationStart(Animator animator) {
    }

    @Override // android.animation.Animator.AnimatorListener
    public void onAnimationEnd(Animator animator) {
        if (animator == this.mCenteringAnimator && !this.mBurnInProtectionActive) {
            this.mAppliedBurnInXOffset = 0;
            this.mAppliedBurnInYOffset = 0;
            this.mDisplayManagerInternal.setDisplayOffsets(this.mDisplay.getDisplayId(), 0, 0);
        }
    }

    @Override // android.animation.Animator.AnimatorListener
    public void onAnimationCancel(Animator animator) {
    }

    @Override // android.animation.Animator.AnimatorListener
    public void onAnimationRepeat(Animator animator) {
    }

    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        if (!this.mBurnInProtectionActive) {
            float value = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            this.mDisplayManagerInternal.setDisplayOffsets(this.mDisplay.getDisplayId(), (int) (this.mAppliedBurnInXOffset * value), (int) (this.mAppliedBurnInYOffset * value));
        }
    }
}
