package android.view;
/* loaded from: classes3.dex */
public interface FallbackEventHandler {
    boolean dispatchKeyEvent(KeyEvent keyEvent);

    void preDispatchKeyEvent(KeyEvent keyEvent);

    void setView(View view);
}
