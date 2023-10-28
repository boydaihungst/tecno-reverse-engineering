package android.view.inputmethod;

import android.os.RemoteException;
import android.util.Log;
import com.android.internal.inputmethod.IAccessibilityInputMethodSession;
import com.android.internal.inputmethod.IRemoteAccessibilityInputConnection;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes3.dex */
public final class IAccessibilityInputMethodSessionInvoker {
    private static final String TAG = "IAccessibilityInputMethodSessionInvoker";
    private final IAccessibilityInputMethodSession mSession;

    private IAccessibilityInputMethodSessionInvoker(IAccessibilityInputMethodSession session) {
        this.mSession = session;
    }

    public static IAccessibilityInputMethodSessionInvoker createOrNull(IAccessibilityInputMethodSession session) {
        if (session == null) {
            return null;
        }
        return new IAccessibilityInputMethodSessionInvoker(session);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishInput() {
        try {
            this.mSession.finishInput();
        } catch (RemoteException e) {
            Log.w(TAG, "A11yIME died", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSelection(int oldSelStart, int oldSelEnd, int selStart, int selEnd, int candidatesStart, int candidatesEnd) {
        try {
            this.mSession.updateSelection(oldSelStart, oldSelEnd, selStart, selEnd, candidatesStart, candidatesEnd);
        } catch (RemoteException e) {
            Log.w(TAG, "A11yIME died", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void invalidateInput(EditorInfo editorInfo, IRemoteAccessibilityInputConnection connection, int sessionId) {
        try {
            this.mSession.invalidateInput(editorInfo, connection, sessionId);
        } catch (RemoteException e) {
            Log.w(TAG, "A11yIME died", e);
        }
    }
}
