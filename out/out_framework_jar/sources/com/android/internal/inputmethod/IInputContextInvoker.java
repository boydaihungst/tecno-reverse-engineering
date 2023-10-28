package com.android.internal.inputmethod;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.SurroundingText;
import android.view.inputmethod.TextAttribute;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.view.IInputContext;
import java.util.Objects;
/* loaded from: classes4.dex */
public final class IInputContextInvoker {
    private final IInputContext mIInputContext;
    private final int mSessionId;

    private IInputContextInvoker(IInputContext inputContext, int sessionId) {
        this.mIInputContext = inputContext;
        this.mSessionId = sessionId;
    }

    public static IInputContextInvoker create(IInputContext inputContext) {
        Objects.requireNonNull(inputContext);
        return new IInputContextInvoker(inputContext, 0);
    }

    public IInputContextInvoker cloneWithSessionId(int sessionId) {
        return new IInputContextInvoker(this.mIInputContext, sessionId);
    }

    public boolean isSameConnection(IInputContext inputContext) {
        return inputContext != null && this.mIInputContext.asBinder() == inputContext.asBinder();
    }

    InputConnectionCommandHeader createHeader() {
        return new InputConnectionCommandHeader(this.mSessionId);
    }

    public AndroidFuture<CharSequence> getTextAfterCursor(int length, int flags) {
        AndroidFuture<CharSequence> future = new AndroidFuture<>();
        try {
            this.mIInputContext.getTextAfterCursor(createHeader(), length, flags, future);
        } catch (RemoteException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public AndroidFuture<CharSequence> getTextBeforeCursor(int length, int flags) {
        AndroidFuture<CharSequence> future = new AndroidFuture<>();
        try {
            this.mIInputContext.getTextBeforeCursor(createHeader(), length, flags, future);
        } catch (RemoteException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public AndroidFuture<CharSequence> getSelectedText(int flags) {
        AndroidFuture<CharSequence> future = new AndroidFuture<>();
        try {
            this.mIInputContext.getSelectedText(createHeader(), flags, future);
        } catch (RemoteException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public AndroidFuture<SurroundingText> getSurroundingText(int beforeLength, int afterLength, int flags) {
        AndroidFuture<SurroundingText> future = new AndroidFuture<>();
        try {
            this.mIInputContext.getSurroundingText(createHeader(), beforeLength, afterLength, flags, future);
        } catch (RemoteException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public AndroidFuture<Integer> getCursorCapsMode(int reqModes) {
        AndroidFuture<Integer> future = new AndroidFuture<>();
        try {
            this.mIInputContext.getCursorCapsMode(createHeader(), reqModes, future);
        } catch (RemoteException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public AndroidFuture<ExtractedText> getExtractedText(ExtractedTextRequest request, int flags) {
        AndroidFuture<ExtractedText> future = new AndroidFuture<>();
        try {
            this.mIInputContext.getExtractedText(createHeader(), request, flags, future);
        } catch (RemoteException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public boolean commitText(CharSequence text, int newCursorPosition) {
        try {
            this.mIInputContext.commitText(createHeader(), text, newCursorPosition);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean commitText(CharSequence text, int newCursorPosition, TextAttribute textAttribute) {
        try {
            this.mIInputContext.commitTextWithTextAttribute(createHeader(), text, newCursorPosition, textAttribute);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean commitCompletion(CompletionInfo text) {
        try {
            this.mIInputContext.commitCompletion(createHeader(), text);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        try {
            this.mIInputContext.commitCorrection(createHeader(), correctionInfo);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean setSelection(int start, int end) {
        try {
            this.mIInputContext.setSelection(createHeader(), start, end);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean performEditorAction(int actionCode) {
        try {
            this.mIInputContext.performEditorAction(createHeader(), actionCode);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean performContextMenuAction(int id) {
        try {
            this.mIInputContext.performContextMenuAction(createHeader(), id);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean setComposingRegion(int start, int end) {
        try {
            this.mIInputContext.setComposingRegion(createHeader(), start, end);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean setComposingRegion(int start, int end, TextAttribute textAttribute) {
        try {
            this.mIInputContext.setComposingRegionWithTextAttribute(createHeader(), start, end, textAttribute);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        try {
            this.mIInputContext.setComposingText(createHeader(), text, newCursorPosition);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean setComposingText(CharSequence text, int newCursorPosition, TextAttribute textAttribute) {
        try {
            this.mIInputContext.setComposingTextWithTextAttribute(createHeader(), text, newCursorPosition, textAttribute);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean finishComposingText() {
        try {
            this.mIInputContext.finishComposingText(createHeader());
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean beginBatchEdit() {
        try {
            this.mIInputContext.beginBatchEdit(createHeader());
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean endBatchEdit() {
        try {
            this.mIInputContext.endBatchEdit(createHeader());
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean sendKeyEvent(KeyEvent event) {
        try {
            this.mIInputContext.sendKeyEvent(createHeader(), event);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean clearMetaKeyStates(int states) {
        try {
            this.mIInputContext.clearMetaKeyStates(createHeader(), states);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        try {
            this.mIInputContext.deleteSurroundingText(createHeader(), beforeLength, afterLength);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        try {
            this.mIInputContext.deleteSurroundingTextInCodePoints(createHeader(), beforeLength, afterLength);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean performSpellCheck() {
        try {
            this.mIInputContext.performSpellCheck(createHeader());
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean performPrivateCommand(String action, Bundle data) {
        try {
            this.mIInputContext.performPrivateCommand(createHeader(), action, data);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public AndroidFuture<Boolean> requestCursorUpdates(int cursorUpdateMode, int imeDisplayId) {
        AndroidFuture<Boolean> future = new AndroidFuture<>();
        try {
            this.mIInputContext.requestCursorUpdates(createHeader(), cursorUpdateMode, imeDisplayId, future);
        } catch (RemoteException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public AndroidFuture<Boolean> requestCursorUpdates(int cursorUpdateMode, int cursorUpdateFilter, int imeDisplayId) {
        AndroidFuture<Boolean> future = new AndroidFuture<>();
        try {
            this.mIInputContext.requestCursorUpdatesWithFilter(createHeader(), cursorUpdateMode, cursorUpdateFilter, imeDisplayId, future);
        } catch (RemoteException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public AndroidFuture<Boolean> commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts) {
        AndroidFuture<Boolean> future = new AndroidFuture<>();
        try {
            this.mIInputContext.commitContent(createHeader(), inputContentInfo, flags, opts, future);
        } catch (RemoteException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public boolean setImeConsumesInput(boolean imeConsumesInput) {
        try {
            this.mIInputContext.setImeConsumesInput(createHeader(), imeConsumesInput);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }
}
