package com.android.internal.widget.commonphrase;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.text.Editable;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import com.android.internal.widget.commonphrase.CommonPhraseDialog;
/* loaded from: classes4.dex */
public class CommonPhraseDialogController {
    private static final String ACTION_ADD_PHRASE = "android.settings.ADD_COMMON_PHRASE";
    private static final String ACTION_PHRASE_SETTINGS = "android.settings.COMMON_PHRASE_SETTINGS";
    private static final String NEW_ACTION_ADD_PHRASE = "android.settings.NEW_ADD_COMMON_PHRASE";
    private static final String NEW_ACTION_PHRASE_SETTINGS = "android.settings.NEW_COMMON_PHRASE_SETTINGS";
    private static final String TAG = "CommonPhraseController";
    Context mContext;
    CommonPhraseDialog mPhraseDialog;
    TextView mTargetView;
    int mContentXOffset = 0;
    int mContentYOffset = 0;
    private Rect mWindowRect = new Rect();
    private CommonPhraseDialog.OnClickListener mOnSpinnerClickListener = new CommonPhraseDialog.OnClickListener() { // from class: com.android.internal.widget.commonphrase.CommonPhraseDialogController.1
        @Override // com.android.internal.widget.commonphrase.CommonPhraseDialog.OnClickListener
        public void onTileClick() {
            Intent intent = new Intent();
            intent.setAction(CommonPhraseDialogController.NEW_ACTION_PHRASE_SETTINGS);
            intent.setFlags(268435456);
            try {
                CommonPhraseDialogController.this.mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(CommonPhraseDialogController.TAG, "onTileClick: start activity filed, action =" + intent.getAction());
            }
            CommonPhraseDialogController.this.mPhraseDialog.dismiss();
        }

        @Override // com.android.internal.widget.commonphrase.CommonPhraseDialog.OnClickListener
        public void onItemClick(String str) {
            Editable editor = CommonPhraseDialogController.this.mTargetView.getEditableText();
            if (editor != null) {
                editor.insert(CommonPhraseDialogController.this.mTargetView.getSelectionEnd(), str);
            }
            CommonPhraseDialogController.this.mPhraseDialog.dismiss();
        }
    };
    private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.android.internal.widget.commonphrase.CommonPhraseDialogController$$ExternalSyntheticLambda0
        @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
        public final void onGlobalLayout() {
            CommonPhraseDialogController.this.m7191x7f0a5059();
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-internal-widget-commonphrase-CommonPhraseDialogController  reason: not valid java name */
    public /* synthetic */ void m7191x7f0a5059() {
        Rect tempRect = new Rect();
        this.mTargetView.getWindowVisibleDisplayFrame(tempRect);
        if (!tempRect.equals(this.mWindowRect)) {
            this.mWindowRect = tempRect;
        }
    }

    public CommonPhraseDialogController(TextView targetView, Context context) {
        this.mTargetView = targetView;
        this.mContext = context;
        CommonPhraseDialog commonPhraseDialog = new CommonPhraseDialog(targetView, context);
        this.mPhraseDialog = commonPhraseDialog;
        commonPhraseDialog.setClickListener(this.mOnSpinnerClickListener);
        this.mTargetView.getWindowVisibleDisplayFrame(this.mWindowRect);
        this.mTargetView.getViewTreeObserver().addOnGlobalLayoutListener(this.mOnGlobalLayoutListener);
    }

    public void show(boolean needCreate) {
        if (needCreate) {
            InputMethodManager imm = (InputMethodManager) this.mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.mTargetView.getWindowToken(), 0);
            Intent intent = new Intent();
            intent.setAction(NEW_ACTION_ADD_PHRASE);
            intent.setFlags(268435456);
            try {
                this.mContext.startActivity(intent);
                return;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "show: start activity filed, action =" + intent.getAction());
                return;
            }
        }
        this.mPhraseDialog.addPhrases(CommonPhraseRepository.queryWords(this.mContext));
        this.mPhraseDialog.show();
    }

    public void refreshOffset(int x, int y) {
        this.mContentXOffset = x;
        this.mContentYOffset = y;
        Rect tempRect = new Rect();
        this.mTargetView.getWindowVisibleDisplayFrame(tempRect);
        if (!tempRect.equals(this.mWindowRect)) {
            this.mWindowRect = tempRect;
            if (this.mPhraseDialog.isShowing()) {
                this.mPhraseDialog.dismiss();
                this.mPhraseDialog.show();
            }
        }
    }

    public void onFocusChanged(boolean focused) {
    }

    public void onVisibilityChanged(int visibility) {
        if (visibility != 0) {
            this.mPhraseDialog.dismiss();
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        this.mPhraseDialog.dismiss();
    }

    public void dismiss() {
        this.mPhraseDialog.dismiss();
    }

    public void onDetachedFromWindow() {
        this.mTargetView.getViewTreeObserver().removeOnGlobalLayoutListener(this.mOnGlobalLayoutListener);
    }

    public boolean existPhrase() {
        return CommonPhraseRepository.queryWords(this.mContext).size() != 0;
    }
}
