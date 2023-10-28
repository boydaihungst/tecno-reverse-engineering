package android.view.inputmethod;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.InputChannel;
import android.view.MotionEvent;
import android.window.ImeOnBackInvokedDispatcher;
import com.android.internal.inputmethod.IInputMethodPrivilegedOperations;
import com.android.internal.view.IInlineSuggestionsRequestCallback;
import com.android.internal.view.InlineSuggestionsRequestInfo;
import java.util.List;
/* loaded from: classes3.dex */
public interface InputMethod {
    public static final String SERVICE_INTERFACE = "android.view.InputMethod";
    public static final String SERVICE_META_DATA = "android.view.im";
    public static final int SHOW_EXPLICIT = 1;
    public static final int SHOW_FORCED = 2;
    public static final String TAG = "InputMethod";

    /* loaded from: classes3.dex */
    public interface SessionCallback {
        void sessionCreated(InputMethodSession inputMethodSession);
    }

    void attachToken(IBinder iBinder);

    void bindInput(InputBinding inputBinding);

    void changeInputMethodSubtype(InputMethodSubtype inputMethodSubtype);

    void createSession(SessionCallback sessionCallback);

    void hideSoftInput(int i, ResultReceiver resultReceiver);

    void hideSoftInputWithToken(int i, ResultReceiver resultReceiver, IBinder iBinder);

    void restartInput(InputConnection inputConnection, EditorInfo editorInfo);

    void revokeSession(InputMethodSession inputMethodSession);

    void setSessionEnabled(InputMethodSession inputMethodSession, boolean z);

    void showSoftInput(int i, ResultReceiver resultReceiver);

    void startInput(InputConnection inputConnection, EditorInfo editorInfo);

    void unbindInput();

    default void initializeInternal(IBinder token, IInputMethodPrivilegedOperations privilegedOperations, int configChanges, boolean stylusHwSupported, int navButtonFlags) {
        attachToken(token);
    }

    default void onCreateInlineSuggestionsRequest(InlineSuggestionsRequestInfo requestInfo, IInlineSuggestionsRequestCallback cb) {
        try {
            cb.onInlineSuggestionsUnsupported();
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call onInlineSuggestionsUnsupported.", e);
        }
    }

    default void dispatchStartInputWithToken(InputConnection inputConnection, EditorInfo editorInfo, boolean restarting, IBinder startInputToken, int navButtonFlags, ImeOnBackInvokedDispatcher imeDispatcher) {
        if (restarting) {
            restartInput(inputConnection, editorInfo);
        } else {
            startInput(inputConnection, editorInfo);
        }
    }

    default void onNavButtonFlagsChanged(int navButtonFlags) {
    }

    default void showSoftInputWithToken(int flags, ResultReceiver resultReceiver, IBinder showInputToken) {
        showSoftInput(flags, resultReceiver);
    }

    default void canStartStylusHandwriting(int requestId) {
    }

    default void startStylusHandwriting(int requestId, InputChannel channel, List<MotionEvent> events) {
    }

    default void initInkWindow() {
    }

    default void finishStylusHandwriting() {
    }
}
