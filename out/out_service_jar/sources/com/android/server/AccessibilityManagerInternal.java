package com.android.server;

import android.util.ArraySet;
import android.util.SparseArray;
import android.view.inputmethod.EditorInfo;
import com.android.internal.inputmethod.IAccessibilityInputMethodSession;
import com.android.internal.inputmethod.IRemoteAccessibilityInputConnection;
/* loaded from: classes.dex */
public abstract class AccessibilityManagerInternal {
    private static final AccessibilityManagerInternal NOP = new AccessibilityManagerInternal() { // from class: com.android.server.AccessibilityManagerInternal.1
        @Override // com.android.server.AccessibilityManagerInternal
        public void setImeSessionEnabled(SparseArray<IAccessibilityInputMethodSession> sessions, boolean enabled) {
        }

        @Override // com.android.server.AccessibilityManagerInternal
        public void unbindInput() {
        }

        @Override // com.android.server.AccessibilityManagerInternal
        public void bindInput() {
        }

        @Override // com.android.server.AccessibilityManagerInternal
        public void createImeSession(ArraySet<Integer> ignoreSet) {
        }

        @Override // com.android.server.AccessibilityManagerInternal
        public void startInput(IRemoteAccessibilityInputConnection remoteAccessibility, EditorInfo editorInfo, boolean restarting) {
        }
    };

    public abstract void bindInput();

    public abstract void createImeSession(ArraySet<Integer> arraySet);

    public abstract void setImeSessionEnabled(SparseArray<IAccessibilityInputMethodSession> sparseArray, boolean z);

    public abstract void startInput(IRemoteAccessibilityInputConnection iRemoteAccessibilityInputConnection, EditorInfo editorInfo, boolean z);

    public abstract void unbindInput();

    public static AccessibilityManagerInternal get() {
        AccessibilityManagerInternal instance = (AccessibilityManagerInternal) LocalServices.getService(AccessibilityManagerInternal.class);
        return instance != null ? instance : NOP;
    }
}
