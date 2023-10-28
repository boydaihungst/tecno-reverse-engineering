package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
/* loaded from: classes4.dex */
public class ImeAwareEditText extends EditText {
    private boolean mHasPendingShowSoftInputRequest;
    final Runnable mRunShowSoftInputIfNecessary;

    public ImeAwareEditText(Context context) {
        super(context, null);
        this.mRunShowSoftInputIfNecessary = new Runnable() { // from class: android.widget.ImeAwareEditText$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ImeAwareEditText.this.m5752lambda$new$0$androidwidgetImeAwareEditText();
            }
        };
    }

    public ImeAwareEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRunShowSoftInputIfNecessary = new Runnable() { // from class: android.widget.ImeAwareEditText$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ImeAwareEditText.this.m5752lambda$new$0$androidwidgetImeAwareEditText();
            }
        };
    }

    public ImeAwareEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mRunShowSoftInputIfNecessary = new Runnable() { // from class: android.widget.ImeAwareEditText$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ImeAwareEditText.this.m5752lambda$new$0$androidwidgetImeAwareEditText();
            }
        };
    }

    public ImeAwareEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mRunShowSoftInputIfNecessary = new Runnable() { // from class: android.widget.ImeAwareEditText$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ImeAwareEditText.this.m5752lambda$new$0$androidwidgetImeAwareEditText();
            }
        };
    }

    @Override // android.widget.TextView, android.view.View
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        InputConnection ic = super.onCreateInputConnection(editorInfo);
        if (this.mHasPendingShowSoftInputRequest) {
            removeCallbacks(this.mRunShowSoftInputIfNecessary);
            post(this.mRunShowSoftInputIfNecessary);
        }
        return ic;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: showSoftInputIfNecessary */
    public void m5752lambda$new$0$androidwidgetImeAwareEditText() {
        if (this.mHasPendingShowSoftInputRequest) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(InputMethodManager.class);
            imm.showSoftInput(this, 0);
            this.mHasPendingShowSoftInputRequest = false;
        }
    }

    public void scheduleShowSoftInput() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(InputMethodManager.class);
        if (imm.hasActiveInputConnection(this)) {
            this.mHasPendingShowSoftInputRequest = false;
            removeCallbacks(this.mRunShowSoftInputIfNecessary);
            imm.showSoftInput(this, 0);
            return;
        }
        this.mHasPendingShowSoftInputRequest = true;
    }
}
