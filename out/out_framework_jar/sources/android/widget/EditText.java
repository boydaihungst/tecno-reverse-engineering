package android.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.SystemProperties;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.internal.widget.commonphrase.CommonPhraseDialogController;
/* loaded from: classes4.dex */
public class EditText extends TextView {
    public static final int ID_COMMON_PHRASES = 101122054;
    public static final int ID_CREATE_COMMON_PHRASES = 101122055;
    private CommonPhraseDialogController mCommonPhraseController;
    private boolean mCommonPhraseSupported;

    public EditText(Context context) {
        this(context, null);
    }

    public EditText(Context context, AttributeSet attrs) {
        this(context, attrs, 16842862);
    }

    public EditText(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mCommonPhraseSupported = "1".equals(SystemProperties.get("ro.os_common_phrase_support"));
    }

    @Override // android.widget.TextView
    public boolean getFreezesText() {
        return true;
    }

    @Override // android.widget.TextView
    protected boolean getDefaultEditable() {
        return true;
    }

    @Override // android.widget.TextView
    protected MovementMethod getDefaultMovementMethod() {
        return ArrowKeyMovementMethod.getInstance();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.widget.TextView
    public Editable getText() {
        CharSequence text = super.getText();
        if (text == null) {
            return null;
        }
        if (text instanceof Editable) {
            return (Editable) text;
        }
        super.setText(text, TextView.BufferType.EDITABLE);
        return (Editable) super.getText();
    }

    @Override // android.widget.TextView
    public void setText(CharSequence text, TextView.BufferType type) {
        super.setText(text, TextView.BufferType.EDITABLE);
    }

    public void setSelection(int start, int stop) {
        Selection.setSelection(getText(), start, stop);
    }

    public void setSelection(int index) {
        Selection.setSelection(getText(), index);
    }

    public void selectAll() {
        Selection.selectAll(getText());
    }

    public void extendSelection(int index) {
        Selection.extendSelection(getText(), index);
    }

    @Override // android.widget.TextView
    public void setEllipsize(TextUtils.TruncateAt ellipsis) {
        if (ellipsis == TextUtils.TruncateAt.MARQUEE) {
            throw new IllegalArgumentException("EditText cannot use the ellipsize mode TextUtils.TruncateAt.MARQUEE");
        }
        super.setEllipsize(ellipsis);
    }

    @Override // android.widget.TextView, android.view.View
    public CharSequence getAccessibilityClassName() {
        return EditText.class.getName();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.View
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setHandwritingArea(new Rect(0, 0, w, h));
    }

    @Override // android.widget.TextView
    protected boolean supportsAutoSizeText() {
        return false;
    }

    @Override // android.widget.TextView
    public boolean onTextContextMenuItem(int id) {
        if (this.mCommonPhraseSupported && (id == 101122055 || id == 101122054)) {
            if (this.mCommonPhraseController == null) {
                this.mCommonPhraseController = new CommonPhraseDialogController(this, this.mContext);
            }
            this.mCommonPhraseController.refreshOffset(viewportToContentHorizontalOffset(), viewportToContentVerticalOffset());
            this.mCommonPhraseController.show(id == 101122055);
            stopTextActionMode();
            return true;
        }
        return super.onTextContextMenuItem(id);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.widget.TextView, android.view.View
    public void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        CommonPhraseDialogController commonPhraseDialogController = this.mCommonPhraseController;
        if (commonPhraseDialogController != null) {
            commonPhraseDialogController.onFocusChanged(focused);
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override // android.widget.TextView, android.view.View
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        CommonPhraseDialogController commonPhraseDialogController = this.mCommonPhraseController;
        if (commonPhraseDialogController != null) {
            commonPhraseDialogController.onFocusChanged(hasWindowFocus);
        }
        super.onWindowFocusChanged(hasWindowFocus);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.widget.TextView, android.view.View
    public void onVisibilityChanged(View changedView, int visibility) {
        CommonPhraseDialogController commonPhraseDialogController = this.mCommonPhraseController;
        if (commonPhraseDialogController != null) {
            commonPhraseDialogController.onVisibilityChanged(visibility);
        }
        super.onVisibilityChanged(changedView, visibility);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.View
    public void onDetachedFromWindow() {
        CommonPhraseDialogController commonPhraseDialogController = this.mCommonPhraseController;
        if (commonPhraseDialogController != null) {
            commonPhraseDialogController.onDetachedFromWindow();
        }
        this.mCommonPhraseController = null;
        super.onDetachedFromWindow();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.View
    public void onWindowVisibilityChanged(int visibility) {
        CommonPhraseDialogController commonPhraseDialogController = this.mCommonPhraseController;
        if (commonPhraseDialogController != null) {
            commonPhraseDialogController.onVisibilityChanged(visibility);
        }
        super.onWindowVisibilityChanged(visibility);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.widget.TextView
    public boolean supportCommonPhrase() {
        int inputTypeClass = getInputType() & 15;
        return (!this.mCommonPhraseSupported || hasSelection() || hasPasswordTransformationMethod() || isDisplayLandscape() || inputTypeClass == 2 || inputTypeClass == 3 || inputTypeClass == 4) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // android.widget.TextView
    public void onLocaleChanged() {
        super.onLocaleChanged();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.widget.TextView, android.view.View
    public void onConfigurationChanged(Configuration newConfig) {
        CommonPhraseDialogController commonPhraseDialogController = this.mCommonPhraseController;
        if (commonPhraseDialogController != null) {
            commonPhraseDialogController.onConfigurationChanged(newConfig);
        }
        super.onConfigurationChanged(newConfig);
    }

    public void setSupportCommonPhrase(boolean supported) {
        this.mCommonPhraseSupported = supported;
    }

    private boolean isDisplayLandscape() {
        return this.mContext.getResources().getConfiguration().orientation == 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // android.widget.TextView
    public boolean needCreatePhrase() {
        if (this.mCommonPhraseController == null) {
            this.mCommonPhraseController = new CommonPhraseDialogController(this, this.mContext);
        }
        return !this.mCommonPhraseController.existPhrase();
    }
}
