package com.android.internal.inputmethod;

import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.method.KeyListener;
import android.util.proto.ProtoOutputStream;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.DumpableInputConnection;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.widget.TextView;
/* loaded from: classes4.dex */
public final class EditableInputConnection extends BaseInputConnection implements DumpableInputConnection {
    private static final boolean DEBUG = false;
    private static final String TAG = "EditableInputConnection";
    private int mBatchEditNesting;
    private final TextView mTextView;

    public EditableInputConnection(TextView textview) {
        super((View) textview, true);
        this.mTextView = textview;
    }

    @Override // android.view.inputmethod.BaseInputConnection
    public Editable getEditable() {
        TextView tv = this.mTextView;
        if (tv != null) {
            return tv.getEditableText();
        }
        return null;
    }

    @Override // android.view.inputmethod.BaseInputConnection, android.view.inputmethod.InputConnection
    public boolean beginBatchEdit() {
        synchronized (this) {
            if (this.mBatchEditNesting >= 0) {
                this.mTextView.beginBatchEdit();
                this.mBatchEditNesting++;
                return true;
            }
            return false;
        }
    }

    @Override // android.view.inputmethod.BaseInputConnection, android.view.inputmethod.InputConnection
    public boolean endBatchEdit() {
        synchronized (this) {
            if (this.mBatchEditNesting > 0) {
                this.mTextView.endBatchEdit();
                int i = this.mBatchEditNesting - 1;
                this.mBatchEditNesting = i;
                return i > 0;
            }
            return false;
        }
    }

    @Override // android.view.inputmethod.BaseInputConnection
    public void endComposingRegionEditInternal() {
        this.mTextView.notifyContentCaptureTextChanged();
    }

    @Override // android.view.inputmethod.BaseInputConnection, android.view.inputmethod.InputConnection
    public void closeConnection() {
        super.closeConnection();
        synchronized (this) {
            while (this.mBatchEditNesting > 0) {
                endBatchEdit();
            }
            this.mBatchEditNesting = -1;
        }
    }

    @Override // android.view.inputmethod.BaseInputConnection, android.view.inputmethod.InputConnection
    public boolean clearMetaKeyStates(int states) {
        Editable content = getEditable();
        if (content == null) {
            return false;
        }
        KeyListener kl = this.mTextView.getKeyListener();
        if (kl != null) {
            try {
                kl.clearMetaKeyState(this.mTextView, content, states);
                return true;
            } catch (AbstractMethodError e) {
                return true;
            }
        }
        return true;
    }

    @Override // android.view.inputmethod.BaseInputConnection, android.view.inputmethod.InputConnection
    public boolean commitCompletion(CompletionInfo text) {
        this.mTextView.beginBatchEdit();
        this.mTextView.onCommitCompletion(text);
        this.mTextView.endBatchEdit();
        return true;
    }

    @Override // android.view.inputmethod.BaseInputConnection, android.view.inputmethod.InputConnection
    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        this.mTextView.beginBatchEdit();
        this.mTextView.onCommitCorrection(correctionInfo);
        this.mTextView.endBatchEdit();
        return true;
    }

    @Override // android.view.inputmethod.BaseInputConnection, android.view.inputmethod.InputConnection
    public boolean performEditorAction(int actionCode) {
        this.mTextView.onEditorAction(actionCode);
        return true;
    }

    @Override // android.view.inputmethod.BaseInputConnection, android.view.inputmethod.InputConnection
    public boolean performContextMenuAction(int id) {
        this.mTextView.beginBatchEdit();
        this.mTextView.onTextContextMenuItem(id);
        this.mTextView.endBatchEdit();
        return true;
    }

    @Override // android.view.inputmethod.BaseInputConnection, android.view.inputmethod.InputConnection
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
        if (this.mTextView != null) {
            ExtractedText et = new ExtractedText();
            if (this.mTextView.extractText(request, et)) {
                if ((flags & 1) != 0) {
                    this.mTextView.setExtracting(request);
                }
                return et;
            }
            return null;
        }
        return null;
    }

    @Override // android.view.inputmethod.InputConnection
    public boolean performSpellCheck() {
        this.mTextView.onPerformSpellCheck();
        return true;
    }

    @Override // android.view.inputmethod.BaseInputConnection, android.view.inputmethod.InputConnection
    public boolean performPrivateCommand(String action, Bundle data) {
        this.mTextView.onPrivateIMECommand(action, data);
        return true;
    }

    @Override // android.view.inputmethod.BaseInputConnection, android.view.inputmethod.InputConnection
    public boolean commitText(CharSequence text, int newCursorPosition) {
        TextView textView = this.mTextView;
        if (textView == null) {
            return super.commitText(text, newCursorPosition);
        }
        textView.resetErrorChangedFlag();
        boolean success = super.commitText(text, newCursorPosition);
        this.mTextView.hideErrorIfUnchanged();
        return success;
    }

    @Override // android.view.inputmethod.InputConnection
    public boolean requestCursorUpdates(int cursorUpdateMode, int cursorUpdateFilter) {
        return requestCursorUpdates(cursorUpdateMode | cursorUpdateFilter);
    }

    @Override // android.view.inputmethod.BaseInputConnection, android.view.inputmethod.InputConnection
    public boolean requestCursorUpdates(int cursorUpdateMode) {
        TextView textView;
        int unknownFlags = cursorUpdateMode & (-32);
        if (unknownFlags != 0 || this.mIMM == null) {
            return false;
        }
        this.mIMM.setUpdateCursorAnchorInfoMode(cursorUpdateMode);
        if ((cursorUpdateMode & 1) != 0 && (textView = this.mTextView) != null && !textView.isInLayout()) {
            this.mTextView.requestLayout();
            return true;
        }
        return true;
    }

    @Override // android.view.inputmethod.InputConnection
    public boolean setImeConsumesInput(boolean imeConsumesInput) {
        TextView textView = this.mTextView;
        if (textView == null) {
            return super.setImeConsumesInput(imeConsumesInput);
        }
        textView.setImeConsumesInput(imeConsumesInput);
        return true;
    }

    @Override // android.view.inputmethod.DumpableInputConnection
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        this.mTextView.getText();
        getSelectedText(0);
        Editable content = getEditable();
        if (content != null) {
            int start = Selection.getSelectionStart(content);
            int end = Selection.getSelectionEnd(content);
            proto.write(1120986464259L, start);
            proto.write(1120986464260L, end);
        }
        proto.write(1120986464261L, getCursorCapsMode(0));
        proto.end(token);
    }
}
