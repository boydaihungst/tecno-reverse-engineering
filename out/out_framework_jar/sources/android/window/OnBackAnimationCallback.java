package android.window;
/* loaded from: classes4.dex */
public interface OnBackAnimationCallback extends OnBackInvokedCallback {
    default void onBackStarted() {
    }

    default void onBackProgressed(BackEvent backEvent) {
    }

    default void onBackCancelled() {
    }
}
