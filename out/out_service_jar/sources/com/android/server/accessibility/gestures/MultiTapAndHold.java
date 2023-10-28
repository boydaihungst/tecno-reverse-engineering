package com.android.server.accessibility.gestures;

import android.content.Context;
import android.view.MotionEvent;
import com.android.server.accessibility.gestures.GestureMatcher;
/* loaded from: classes.dex */
public class MultiTapAndHold extends MultiTap {
    public MultiTapAndHold(Context context, int taps, int gesture, GestureMatcher.StateChangeListener listener) {
        super(context, taps, gesture, listener);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.accessibility.gestures.MultiTap, com.android.server.accessibility.gestures.GestureMatcher
    public void onDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        super.onDown(event, rawEvent, policyFlags);
        if (this.mCurrentTaps + 1 == this.mTargetTaps) {
            completeAfterLongPressTimeout(event, rawEvent, policyFlags);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.accessibility.gestures.MultiTap, com.android.server.accessibility.gestures.GestureMatcher
    public void onUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        super.onUp(event, rawEvent, policyFlags);
        cancelAfterDoubleTapTimeout(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.MultiTap, com.android.server.accessibility.gestures.GestureMatcher
    public String getGestureName() {
        switch (this.mTargetTaps) {
            case 2:
                return "Double Tap and Hold";
            case 3:
                return "Triple Tap and Hold";
            default:
                return Integer.toString(this.mTargetTaps) + " Taps and Hold";
        }
    }
}
