package com.android.server.inputmethod;

import android.os.IBinder;
import android.view.inputmethod.InputMethodInfo;
import com.android.internal.inputmethod.IAccessibilityInputMethodSession;
import com.android.internal.view.IInlineSuggestionsRequestCallback;
import com.android.internal.view.InlineSuggestionsRequestInfo;
import com.android.server.LocalServices;
import java.util.Collections;
import java.util.List;
/* loaded from: classes.dex */
public abstract class InputMethodManagerInternal {
    private static final InputMethodManagerInternal NOP = new InputMethodManagerInternal() { // from class: com.android.server.inputmethod.InputMethodManagerInternal.1
        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void setInteractive(boolean interactive) {
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void hideCurrentInputMethod(int reason) {
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public List<InputMethodInfo> getInputMethodListAsUser(int userId) {
            return Collections.emptyList();
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public List<InputMethodInfo> getEnabledInputMethodListAsUser(int userId) {
            return Collections.emptyList();
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void onCreateInlineSuggestionsRequest(int userId, InlineSuggestionsRequestInfo requestInfo, IInlineSuggestionsRequestCallback cb) {
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public boolean switchToInputMethod(String imeId, int userId) {
            return false;
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public boolean setInputMethodEnabled(String imeId, boolean enabled, int userId) {
            return false;
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void registerInputMethodListListener(InputMethodListListener listener) {
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public boolean transferTouchFocusToImeWindow(IBinder sourceInputToken, int displayId) {
            return false;
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void reportImeControl(IBinder windowToken) {
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void onImeParentChanged() {
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void removeImeSurface() {
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void updateImeWindowStatus(boolean disableImeIcon) {
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void onSessionForAccessibilityCreated(int accessibilityConnectionId, IAccessibilityInputMethodSession session) {
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void unbindAccessibilityFromCurrentClient(int accessibilityConnectionId) {
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void maybeFinishStylusHandwriting() {
        }
    };

    /* loaded from: classes.dex */
    public interface InputMethodListListener {
        void onInputMethodListUpdated(List<InputMethodInfo> list, int i);
    }

    public abstract List<InputMethodInfo> getEnabledInputMethodListAsUser(int i);

    public abstract List<InputMethodInfo> getInputMethodListAsUser(int i);

    public abstract void hideCurrentInputMethod(int i);

    public abstract void maybeFinishStylusHandwriting();

    public abstract void onCreateInlineSuggestionsRequest(int i, InlineSuggestionsRequestInfo inlineSuggestionsRequestInfo, IInlineSuggestionsRequestCallback iInlineSuggestionsRequestCallback);

    public abstract void onImeParentChanged();

    public abstract void onSessionForAccessibilityCreated(int i, IAccessibilityInputMethodSession iAccessibilityInputMethodSession);

    public abstract void registerInputMethodListListener(InputMethodListListener inputMethodListListener);

    public abstract void removeImeSurface();

    public abstract void reportImeControl(IBinder iBinder);

    public abstract boolean setInputMethodEnabled(String str, boolean z, int i);

    public abstract void setInteractive(boolean z);

    public abstract boolean switchToInputMethod(String str, int i);

    public abstract boolean transferTouchFocusToImeWindow(IBinder iBinder, int i);

    public abstract void unbindAccessibilityFromCurrentClient(int i);

    public abstract void updateImeWindowStatus(boolean z);

    public static InputMethodManagerInternal get() {
        InputMethodManagerInternal instance = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
        return instance != null ? instance : NOP;
    }
}
